package com.tripplanner.service;

import com.google.cloud.firestore.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

/**
 * Manages distributed locks for itinerary nodes and operations with TTL-based expiration.
 * Prevents concurrent modifications and provides metadata about lock ownership.
 */
@Service
public class LockManager {
    
    private static final Logger logger = LoggerFactory.getLogger(LockManager.class);
    
    @Autowired
    private Firestore firestore;
    
    // In-memory cache for frequently accessed locks
    private final Map<String, NodeLock> lockCache = new ConcurrentHashMap<>();
    private final Map<String, Long> cacheTimestamps = new ConcurrentHashMap<>();
    
    // Configuration
    private static final String LOCKS_COLLECTION = "node_locks";
    private static final long DEFAULT_LOCK_TTL_MS = 300000; // 5 minutes
    private static final long CACHE_TTL_MS = 30000; // 30 seconds
    private static final long CLEANUP_INTERVAL_MS = 60000; // 1 minute
    
    /**
     * Acquire a lock on a specific node.
     * 
     * @param nodeId The node to lock
     * @param lockType The type of lock (READ, WRITE, EXCLUSIVE)
     * @param ownerId The ID of the lock owner (user, agent, etc.)
     * @param ttlMs Time-to-live for the lock in milliseconds
     * @return LockResult indicating success or failure
     */
    public LockResult acquireLock(String nodeId, LockType lockType, String ownerId, Long ttlMs) {
        logger.debug("Attempting to acquire {} lock on node {} for owner {}", 
                    lockType, nodeId, ownerId);
        
        if (nodeId == null || nodeId.trim().isEmpty()) {
            return new LockResult(false, "Node ID cannot be null or empty", null);
        }
        
        if (ownerId == null || ownerId.trim().isEmpty()) {
            return new LockResult(false, "Owner ID cannot be null or empty", null);
        }
        
        long lockTtl = ttlMs != null ? ttlMs : DEFAULT_LOCK_TTL_MS;
        long expiresAt = System.currentTimeMillis() + lockTtl;
        
        try {
            // Check existing locks
            Optional<NodeLock> existingLock = getLock(nodeId);
            
            if (existingLock.isPresent()) {
                NodeLock existing = existingLock.get();
                
                // Check if lock is expired
                if (existing.isExpired()) {
                    logger.debug("Existing lock is expired, removing: {}", nodeId);
                    releaseLock(nodeId, existing.getOwnerId());
                } else {
                    // Check lock type compatibility
                    if (!isLockCompatible(existing, lockType, ownerId)) {
                        logger.debug("Lock conflict detected for node {}: existing {} by {}, requested {} by {}", 
                                   nodeId, existing.getLockType(), existing.getOwnerId(), lockType, ownerId);
                        return new LockResult(false, "Lock conflict with existing lock", existing);
                    }
                    
                    // Same owner can upgrade/extend lock
                    if (existing.getOwnerId().equals(ownerId)) {
                        logger.debug("Extending/upgrading lock for same owner: {}", ownerId);
                        existing.setLockType(lockType);
                        existing.setExpiresAt(expiresAt);
                        existing.setUpdatedAt(System.currentTimeMillis());
                        saveLock(existing);
                        cacheTimestamps.put(nodeId, System.currentTimeMillis());
                        return new LockResult(true, "Lock extended/upgraded", existing);
                    }
                }
            }
            
            // Create new lock
            NodeLock newLock = new NodeLock(nodeId, lockType, ownerId, expiresAt);
            saveLock(newLock);
            
            // Cache the lock
            lockCache.put(nodeId, newLock);
            cacheTimestamps.put(nodeId, System.currentTimeMillis());
            
            logger.info("Successfully acquired {} lock on node {} for owner {}", 
                       lockType, nodeId, ownerId);
            
            return new LockResult(true, "Lock acquired successfully", newLock);
            
        } catch (Exception e) {
            logger.error("Failed to acquire lock on node {} for owner {}", nodeId, ownerId, e);
            return new LockResult(false, "Failed to acquire lock: " + e.getMessage(), null);
        }
    }
    
    /**
     * Release a lock on a specific node.
     * 
     * @param nodeId The node to unlock
     * @param ownerId The ID of the lock owner
     * @return true if lock was released, false otherwise
     */
    public boolean releaseLock(String nodeId, String ownerId) {
        logger.debug("Attempting to release lock on node {} for owner {}", nodeId, ownerId);
        
        try {
            Optional<NodeLock> existingLock = getLock(nodeId);
            
            if (existingLock.isEmpty()) {
                logger.debug("No lock found for node: {}", nodeId);
                return true; // No lock to release
            }
            
            NodeLock lock = existingLock.get();
            
            // Verify ownership
            if (!lock.getOwnerId().equals(ownerId)) {
                logger.warn("Lock release denied: owner mismatch for node {} (expected: {}, actual: {})", 
                           nodeId, ownerId, lock.getOwnerId());
                return false;
            }
            
            // Remove from Firestore
            firestore.collection(LOCKS_COLLECTION)
                    .document(nodeId)
                    .delete()
                    .get();
            
            // Remove from cache
            lockCache.remove(nodeId);
            cacheTimestamps.remove(nodeId);
            
            logger.info("Successfully released lock on node {} for owner {}", nodeId, ownerId);
            return true;
            
        } catch (Exception e) {
            logger.error("Failed to release lock on node {} for owner {}", nodeId, ownerId, e);
            return false;
        }
    }
    
    /**
     * Check if a node is currently locked.
     * 
     * @param nodeId The node to check
     * @return true if the node is locked, false otherwise
     */
    public boolean isLocked(String nodeId) {
        try {
            Optional<NodeLock> lock = getLock(nodeId);
            return lock.isPresent() && !lock.get().isExpired();
        } catch (Exception e) {
            logger.error("Failed to check lock status for node: {}", nodeId, e);
            return false; // Assume not locked on error
        }
    }
    
    /**
     * Get lock information for a specific node.
     * 
     * @param nodeId The node to check
     * @return Optional containing lock information if present
     */
    public Optional<NodeLock> getLock(String nodeId) {
        try {
            // Check cache first
            NodeLock cached = getCachedLock(nodeId);
            if (cached != null) {
                return Optional.of(cached);
            }
            
            // Load from Firestore
            DocumentSnapshot doc = firestore.collection(LOCKS_COLLECTION)
                    .document(nodeId)
                    .get()
                    .get();
            
            if (doc.exists()) {
                NodeLock lock = doc.toObject(NodeLock.class);
                if (lock != null) {
                    // Cache the lock
                    lockCache.put(nodeId, lock);
                    cacheTimestamps.put(nodeId, System.currentTimeMillis());
                    return Optional.of(lock);
                }
            }
            
            return Optional.empty();
            
        } catch (Exception e) {
            logger.error("Failed to get lock for node: {}", nodeId, e);
            return Optional.empty();
        }
    }
    
    /**
     * Get all locks owned by a specific owner.
     * 
     * @param ownerId The owner to check
     * @return List of locks owned by the specified owner
     */
    public List<NodeLock> getLocksByOwner(String ownerId) {
        try {
            QuerySnapshot querySnapshot = firestore.collection(LOCKS_COLLECTION)
                    .whereEqualTo("ownerId", ownerId)
                    .get()
                    .get();
            
            List<NodeLock> locks = new ArrayList<>();
            for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                NodeLock lock = doc.toObject(NodeLock.class);
                if (lock != null && !lock.isExpired()) {
                    locks.add(lock);
                }
            }
            
            return locks;
            
        } catch (Exception e) {
            logger.error("Failed to get locks for owner: {}", ownerId, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Release all locks owned by a specific owner.
     * 
     * @param ownerId The owner whose locks should be released
     * @return Number of locks released
     */
    public int releaseAllLocks(String ownerId) {
        logger.info("Releasing all locks for owner: {}", ownerId);
        
        List<NodeLock> locks = getLocksByOwner(ownerId);
        int releasedCount = 0;
        
        for (NodeLock lock : locks) {
            if (releaseLock(lock.getNodeId(), ownerId)) {
                releasedCount++;
            }
        }
        
        logger.info("Released {} locks for owner: {}", releasedCount, ownerId);
        return releasedCount;
    }
    
    /**
     * Extend the TTL of an existing lock.
     * 
     * @param nodeId The node whose lock should be extended
     * @param ownerId The owner of the lock
     * @param additionalTtlMs Additional time to add to the lock
     * @return true if lock was extended, false otherwise
     */
    public boolean extendLock(String nodeId, String ownerId, long additionalTtlMs) {
        logger.debug("Extending lock on node {} for owner {} by {}ms", 
                    nodeId, ownerId, additionalTtlMs);
        
        try {
            Optional<NodeLock> existingLock = getLock(nodeId);
            
            if (existingLock.isEmpty()) {
                logger.warn("Cannot extend non-existent lock for node: {}", nodeId);
                return false;
            }
            
            NodeLock lock = existingLock.get();
            
            // Verify ownership
            if (!lock.getOwnerId().equals(ownerId)) {
                logger.warn("Lock extension denied: owner mismatch for node {}", nodeId);
                return false;
            }
            
            // Extend the lock
            lock.setExpiresAt(lock.getExpiresAt() + additionalTtlMs);
            lock.setUpdatedAt(System.currentTimeMillis());
            saveLock(lock);
            
            // Update cache
            lockCache.put(nodeId, lock);
            cacheTimestamps.put(nodeId, System.currentTimeMillis());
            
            logger.debug("Successfully extended lock on node {} for owner {}", nodeId, ownerId);
            return true;
            
        } catch (Exception e) {
            logger.error("Failed to extend lock on node {} for owner {}", nodeId, ownerId, e);
            return false;
        }
    }
    
    /**
     * Get lock statistics for monitoring.
     */
    public LockStatistics getStatistics() {
        try {
            QuerySnapshot allLocks = firestore.collection(LOCKS_COLLECTION)
                    .get()
                    .get();
            
            int totalLocks = 0;
            int expiredLocks = 0;
            Map<LockType, Integer> lockTypeCount = new HashMap<>();
            Map<String, Integer> ownerCount = new HashMap<>();
            
            for (DocumentSnapshot doc : allLocks.getDocuments()) {
                NodeLock lock = doc.toObject(NodeLock.class);
                if (lock != null) {
                    totalLocks++;
                    
                    if (lock.isExpired()) {
                        expiredLocks++;
                    }
                    
                    lockTypeCount.merge(lock.getLockType(), 1, Integer::sum);
                    ownerCount.merge(lock.getOwnerId(), 1, Integer::sum);
                }
            }
            
            return new LockStatistics(totalLocks, expiredLocks, lockTypeCount, 
                                    ownerCount, lockCache.size());
            
        } catch (Exception e) {
            logger.error("Failed to get lock statistics", e);
            return new LockStatistics(0, 0, new HashMap<>(), new HashMap<>(), lockCache.size());
        }
    }
    
    /**
     * Scheduled cleanup of expired locks.
     */
    @Scheduled(fixedDelay = CLEANUP_INTERVAL_MS)
    public void cleanupExpiredLocks() {
        logger.debug("Running scheduled cleanup of expired locks");
        
        try {
            long currentTime = System.currentTimeMillis();
            
            QuerySnapshot expiredLocks = firestore.collection(LOCKS_COLLECTION)
                    .whereLessThan("expiresAt", currentTime)
                    .get()
                    .get();
            
            int cleanedCount = 0;
            for (DocumentSnapshot doc : expiredLocks.getDocuments()) {
                doc.getReference().delete();
                lockCache.remove(doc.getId());
                cacheTimestamps.remove(doc.getId());
                cleanedCount++;
            }
            
            if (cleanedCount > 0) {
                logger.info("Cleaned up {} expired locks", cleanedCount);
            }
            
            // Clean up expired cache entries
            cleanupExpiredCacheEntries();
            
        } catch (Exception e) {
            logger.error("Failed to cleanup expired locks", e);
        }
    }
    
    /**
     * Check if a new lock is compatible with an existing lock.
     */
    private boolean isLockCompatible(NodeLock existingLock, LockType requestedType, String requestedOwner) {
        // Same owner can always acquire compatible locks
        if (existingLock.getOwnerId().equals(requestedOwner)) {
            return true;
        }
        
        // Check lock type compatibility
        switch (existingLock.getLockType()) {
            case READ:
                return requestedType == LockType.READ; // Multiple readers allowed
            case WRITE:
            case EXCLUSIVE:
                return false; // No other locks allowed
            default:
                return false;
        }
    }
    
    /**
     * Save lock to Firestore.
     */
    private void saveLock(NodeLock lock) throws ExecutionException, InterruptedException {
        firestore.collection(LOCKS_COLLECTION)
                .document(lock.getNodeId())
                .set(lock)
                .get();
    }
    
    /**
     * Get lock from cache if valid.
     */
    private NodeLock getCachedLock(String nodeId) {
        Long timestamp = cacheTimestamps.get(nodeId);
        if (timestamp != null && (System.currentTimeMillis() - timestamp) < CACHE_TTL_MS) {
            return lockCache.get(nodeId);
        }
        
        // Remove expired cache entry
        lockCache.remove(nodeId);
        cacheTimestamps.remove(nodeId);
        return null;
    }
    
    /**
     * Clean up expired cache entries.
     */
    private void cleanupExpiredCacheEntries() {
        long currentTime = System.currentTimeMillis();
        
        cacheTimestamps.entrySet().removeIf(entry -> {
            if (currentTime - entry.getValue() > CACHE_TTL_MS) {
                lockCache.remove(entry.getKey());
                return true;
            }
            return false;
        });
    }
    
    // Enums and Data Classes
    
    public enum LockType {
        READ,       // Allows multiple readers
        WRITE,      // Exclusive write access
        EXCLUSIVE   // Completely exclusive access
    }
    
    public static class NodeLock {
        private String nodeId;
        private LockType lockType;
        private String ownerId;
        private long createdAt;
        private long updatedAt;
        private long expiresAt;
        private Map<String, Object> metadata;
        
        public NodeLock() {
            this.metadata = new HashMap<>();
        }
        
        public NodeLock(String nodeId, LockType lockType, String ownerId, long expiresAt) {
            this();
            this.nodeId = nodeId;
            this.lockType = lockType;
            this.ownerId = ownerId;
            this.createdAt = System.currentTimeMillis();
            this.updatedAt = this.createdAt;
            this.expiresAt = expiresAt;
        }
        
        public boolean isExpired() {
            return System.currentTimeMillis() > expiresAt;
        }
        
        public long getRemainingTtlMs() {
            return Math.max(0, expiresAt - System.currentTimeMillis());
        }
        
        // Getters and Setters
        public String getNodeId() { return nodeId; }
        public void setNodeId(String nodeId) { this.nodeId = nodeId; }
        public LockType getLockType() { return lockType; }
        public void setLockType(LockType lockType) { this.lockType = lockType; }
        public String getOwnerId() { return ownerId; }
        public void setOwnerId(String ownerId) { this.ownerId = ownerId; }
        public long getCreatedAt() { return createdAt; }
        public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
        public long getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
        public long getExpiresAt() { return expiresAt; }
        public void setExpiresAt(long expiresAt) { this.expiresAt = expiresAt; }
        public Map<String, Object> getMetadata() { return metadata; }
        public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
        
        @Override
        public String toString() {
            return "NodeLock{" +
                    "nodeId='" + nodeId + '\'' +
                    ", lockType=" + lockType +
                    ", ownerId='" + ownerId + '\'' +
                    ", expiresAt=" + expiresAt +
                    ", expired=" + isExpired() +
                    '}';
        }
    }
    
    public static class LockResult {
        private final boolean success;
        private final String message;
        private final NodeLock lock;
        
        public LockResult(boolean success, String message, NodeLock lock) {
            this.success = success;
            this.message = message;
            this.lock = lock;
        }
        
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public NodeLock getLock() { return lock; }
        
        @Override
        public String toString() {
            return "LockResult{" +
                    "success=" + success +
                    ", message='" + message + '\'' +
                    ", lock=" + lock +
                    '}';
        }
    }
    
    public static class LockStatistics {
        private final int totalLocks;
        private final int expiredLocks;
        private final Map<LockType, Integer> lockTypeCount;
        private final Map<String, Integer> ownerCount;
        private final int cachedLocks;
        
        public LockStatistics(int totalLocks, int expiredLocks, Map<LockType, Integer> lockTypeCount,
                            Map<String, Integer> ownerCount, int cachedLocks) {
            this.totalLocks = totalLocks;
            this.expiredLocks = expiredLocks;
            this.lockTypeCount = lockTypeCount;
            this.ownerCount = ownerCount;
            this.cachedLocks = cachedLocks;
        }
        
        public int getTotalLocks() { return totalLocks; }
        public int getExpiredLocks() { return expiredLocks; }
        public int getActiveLocks() { return totalLocks - expiredLocks; }
        public Map<LockType, Integer> getLockTypeCount() { return lockTypeCount; }
        public Map<String, Integer> getOwnerCount() { return ownerCount; }
        public int getCachedLocks() { return cachedLocks; }
        
        @Override
        public String toString() {
            return "LockStatistics{" +
                    "totalLocks=" + totalLocks +
                    ", activeLocks=" + getActiveLocks() +
                    ", expiredLocks=" + expiredLocks +
                    ", cachedLocks=" + cachedLocks +
                    ", lockTypes=" + lockTypeCount +
                    '}';
        }
    }
}