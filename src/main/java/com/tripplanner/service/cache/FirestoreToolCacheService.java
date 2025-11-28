package com.tripplanner.service.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.tripplanner.config.ToolCacheTTLConfig;
import com.tripplanner.dto.cache.CachedToolResult;
import com.tripplanner.dto.cache.CacheMetadata;
import com.tripplanner.dto.cache.CacheStats;
import com.tripplanner.dto.cache.ItineraryCacheData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Firestore-based implementation of ToolCacheService.
 * 
 * Architecture:
 * - Firestore subcollection: itineraries/{itineraryId}/toolCache
 * - One document per cache entry (document ID = cache key)
 * - Each document contains a CachedToolResult object
 * - Session cache (ConcurrentHashMap) for fast access
 * - Lazy loading: cache loaded on first access
 * - Deduplication: prevents concurrent duplicate tool calls
 * 
 * Storage Structure:
 * itineraries/{itineraryId}/toolCache/{cacheKey}
 * - Example: itineraries/abc123/toolCache/weather:zurich-switzerland:2025-06-15
 * 
 * Benefits of Subcollection Approach:
 * - No document size limits (1MB limit doesn't apply)
 * - Better organization and querying
 * - Separate lifecycle management
 * - Easier to clean up expired entries
 * 
 * Performance:
 * - Cache hit: <50ms (session cache lookup)
 * - Cache miss: tool execution time + ~100ms (Firestore write)
 * - Preload: ~200-500ms (Firestore read)
 * 
 * Thread Safety:
 * - All methods are thread-safe
 * - Uses ConcurrentHashMap for session cache
 * - Uses deduplication map for concurrent requests
 */
@Service
@ConditionalOnProperty(name = "cache.tool-results.enabled", havingValue = "true", matchIfMissing = false)
public class FirestoreToolCacheService implements ToolCacheService {
    
    private static final Logger logger = LoggerFactory.getLogger(FirestoreToolCacheService.class);
    
    private static final int MAX_SESSION_CACHE_SIZE = 100;  // Max itineraries in session cache
    private static final int DEDUPLICATION_TIMEOUT_SECONDS = 30;  // Max wait for concurrent request
    
    private final Firestore firestore;
    private final ObjectMapper objectMapper;
    
    // Session cache: itineraryId -> ItineraryCacheData
    private final ConcurrentHashMap<String, ItineraryCacheData> sessionCache;
    
    // Deduplication: requestKey -> CompletableFuture
    // Prevents multiple agents from calling same tool simultaneously
    private final ConcurrentHashMap<String, CompletableFuture<?>> pendingRequests;
    
    @Autowired
    public FirestoreToolCacheService(Firestore firestore, ObjectMapper objectMapper) {
        this.firestore = firestore;
        this.objectMapper = objectMapper;
        this.sessionCache = new ConcurrentHashMap<>();
        this.pendingRequests = new ConcurrentHashMap<>();
        
        logger.info("FirestoreToolCacheService initialized - storing cache in itinerary documents");
    }
    
    @Override
    public <T> T getOrCompute(
        String itineraryId,
        String toolType,
        String cacheKey,
        Object request,
        Supplier<T> toolCall,
        Class<T> resultType
    ) {
        // Check if this tool should be cached this is deprecated check service and replace with new
        if (!ToolCacheTTLConfig.shouldCache(toolType)) {
            logger.debug("Tool {} should not be cached, calling directly", toolType);
            return toolCall.get();
        }
        
        String requestKey = itineraryId + ":" + cacheKey;
        
        // Check if request already in flight (deduplication)
        @SuppressWarnings("unchecked")
        CompletableFuture<T> pending = (CompletableFuture<T>) pendingRequests.get(requestKey);
        if (pending != null) {
            try {
                logger.debug("Request in flight, waiting: {}", cacheKey);
                return pending.get(DEDUPLICATION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            } catch (Exception e) {
                logger.warn("Failed to wait for pending request: {}", e.getMessage());
                // Continue to execute ourselves
            }
        }
        
        // Create future for this request
        CompletableFuture<T> future = new CompletableFuture<>();
        CompletableFuture<?> existing = pendingRequests.putIfAbsent(requestKey, future);
        
        if (existing != null) {
            // Another thread just started, wait for it
            try {
                @SuppressWarnings("unchecked")
                CompletableFuture<T> existingTyped = (CompletableFuture<T>) existing;
                return existingTyped.get(DEDUPLICATION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            } catch (Exception e) {
                logger.warn("Failed to wait for concurrent request: {}", e.getMessage());
                // Continue to execute ourselves
            }
        }
        
        try {
            // 1. Check session cache first (fastest)
            ItineraryCacheData cacheData = ensureCacheLoaded(itineraryId);
            CachedToolResult cached = cacheData.getToolResult(cacheKey);
            
            if (cached != null && cached.isValid()) {
                logger.debug("Cache HIT (session): {}", cacheKey);
                cacheData.recordHit();
                
                // Type-safe deserialization
                T result = cached.getResultAs(resultType, objectMapper);
                future.complete(result);
                return result;
            }
            
            // 2. Cache MISS - call tool
            logger.debug("Cache MISS: {}", cacheKey);
            long startTime = System.currentTimeMillis();
            T result = toolCall.get();
            long executionTime = System.currentTimeMillis() - startTime;
            
            logger.debug("Tool executed in {}ms: {}", executionTime, toolType);
            
            // 3. Store in cache with type information
            CachedToolResult newEntry = new CachedToolResult();
            newEntry.setToolType(toolType);
            newEntry.setCacheKey(cacheKey);
            newEntry.setCachedAt(System.currentTimeMillis());
            newEntry.setTtlSeconds(ToolCacheTTLConfig.getTTL(toolType));
            newEntry.setExpiresAt(System.currentTimeMillis() + (newEntry.getTtlSeconds() * 1000L));
            
            // Store type information for safe deserialization
            newEntry.setRequestClass(request.getClass().getName());
            newEntry.setResultClass(resultType.getName());
            
            // Serialize to JSON strings
            try {
                newEntry.setRequestJson(objectMapper.writeValueAsString(request));
                newEntry.setResultJson(objectMapper.writeValueAsString(result));
            } catch (Exception e) {
                logger.error("Failed to serialize request/result for caching", e);
                // Don't cache if serialization fails, but return result
                future.complete(result);
                return result;
            }
            
            // Add metadata
            CacheMetadata metadata = new CacheMetadata();
            metadata.setExecutionTimeMs(executionTime);
            metadata.setToolVersion("1.0");
            metadata.setCacheHit(false);
            newEntry.setMetadata(metadata);
            
            // Save to Firestore (async)
            saveToFirestoreAsync(itineraryId, cacheKey, newEntry);
            
            // Update session cache
            cacheData.putToolResult(cacheKey, newEntry);
            cacheData.recordMiss();
            
            future.complete(result);
            return result;
            
        } catch (Exception e) {
            logger.error("Cache operation failed for {}: {}", cacheKey, e.getMessage(), e);
            future.completeExceptionally(e);
            throw new RuntimeException("Cache operation failed", e);
        } finally {
            pendingRequests.remove(requestKey);
        }
    }
    
    /**
     * Ensure cache is loaded for itinerary (lazy loading)
     */
    private ItineraryCacheData ensureCacheLoaded(String itineraryId) {
        return sessionCache.computeIfAbsent(itineraryId, id -> {
            try {
                // Check session cache size limit
                if (sessionCache.size() >= MAX_SESSION_CACHE_SIZE) {
                    evictOldestEntry();
                }
                
                return loadFromFirestoreSync(id);
            } catch (Exception e) {
                logger.warn("Failed to load cache for {}: {}", id, e.getMessage());
                return new ItineraryCacheData(id);
            }
        });
    }
    
    /**
     * Load cache from toolCache subcollection synchronously
     * Uses subcollection: itineraries/{itineraryId}/toolCache
     */
    private ItineraryCacheData loadFromFirestoreSync(String itineraryId) {
        try {
            ItineraryCacheData cacheData = new ItineraryCacheData(itineraryId);
            
            // Read from toolCache subcollection
            var cacheCollection = firestore.collection("itineraries")
                .document(itineraryId)
                .collection("toolCache");
            
            var querySnapshot = cacheCollection.get().get();
            
            if (!querySnapshot.isEmpty()) {
                for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                    try {
                        CachedToolResult cached = doc.toObject(CachedToolResult.class);
                        
                        if (cached != null) {
                            // Only load non-expired entries
                            if (cached.isValid()) {
                                cacheData.putToolResult(doc.getId(), cached);
                            } else {
                                // Delete expired entry
                                doc.getReference().delete();
                            }
                        }
                    } catch (Exception e) {
                        logger.warn("Failed to deserialize cache entry {}: {}", 
                            doc.getId(), e.getMessage());
                    }
                }
                
                logger.info("Loaded {} cache entries from itinerary {} toolCache subcollection", 
                    cacheData.size(), itineraryId);
            }
            
            return cacheData;
            
        } catch (Exception e) {
            logger.error("Failed to load cache from itinerary {}: {}", 
                itineraryId, e.getMessage());
            return new ItineraryCacheData(itineraryId);
        }
    }
    
    /**
     * Save cache entry to toolCache subcollection asynchronously
     * Uses subcollection: itineraries/{itineraryId}/toolCache/{cacheKey}
     */
    private void saveToFirestoreAsync(String itineraryId, String cacheKey, CachedToolResult entry) {
        CompletableFuture.runAsync(() -> {
            try {
                // Save to toolCache subcollection
                DocumentReference cacheDocRef = firestore.collection("itineraries")
                    .document(itineraryId)
                    .collection("toolCache")
                    .document(cacheKey);
                
                cacheDocRef.set(entry).get();
                
                logger.debug("Saved cache entry to toolCache subcollection: itineraries/{}/toolCache/{}", 
                    itineraryId, cacheKey);
                
            } catch (Exception e) {
                logger.error("Failed to save cache entry to toolCache subcollection: {}", e.getMessage());
            }
        });
    }
    
    /**
     * Evict oldest entry from session cache (LRU)
     */
    private void evictOldestEntry() {
        String oldestKey = null;
        long oldestTime = Long.MAX_VALUE;
        
        for (Map.Entry<String, ItineraryCacheData> entry : sessionCache.entrySet()) {
            long lastAccess = entry.getValue().getLastAccessTime();
            if (lastAccess < oldestTime) {
                oldestTime = lastAccess;
                oldestKey = entry.getKey();
            }
        }
        
        if (oldestKey != null) {
            sessionCache.remove(oldestKey);
            logger.debug("Evicted cache entry for itinerary: {}", oldestKey);
        }
    }
    
    @Override
    public CompletableFuture<Void> preloadCache(String itineraryId) {
        return CompletableFuture.runAsync(() -> {
            try {
                logger.info("Preloading cache for itinerary: {}", itineraryId);
                
                // Load from Firestore
                ItineraryCacheData cacheData = loadFromFirestoreSync(itineraryId);
                
                // Store in session cache
                sessionCache.put(itineraryId, cacheData);
                
                logger.info("Preloaded {} cache entries for itinerary {}", 
                    cacheData.size(), itineraryId);
                    
            } catch (Exception e) {
                logger.error("Failed to preload cache for itinerary {}: {}", 
                    itineraryId, e.getMessage());
            }
        });
    }
    
    @Override
    public void invalidate(String itineraryId, String cacheKey) {
        logger.info("Invalidating cache entry: {} for itinerary: {}", cacheKey, itineraryId);
        
        // Remove from session cache
        ItineraryCacheData cacheData = sessionCache.get(itineraryId);
        if (cacheData != null) {
            cacheData.removeToolResult(cacheKey);
        }
        
        // Remove from toolCache subcollection
        CompletableFuture.runAsync(() -> {
            try {
                DocumentReference cacheDocRef = firestore.collection("itineraries")
                    .document(itineraryId)
                    .collection("toolCache")
                    .document(cacheKey);
                
                cacheDocRef.delete().get();
                
                logger.debug("Removed cache entry from toolCache subcollection: {}", cacheKey);
                
            } catch (Exception e) {
                logger.error("Failed to remove cache entry from toolCache subcollection: {}", e.getMessage());
            }
        });
    }
    
    @Override
    public void invalidateByPattern(String itineraryId, String pattern) {
        logger.info("Invalidating cache entries matching pattern: {} for itinerary: {}", 
            pattern, itineraryId);
        
        ItineraryCacheData cacheData = sessionCache.get(itineraryId);
        if (cacheData == null) {
            return;
        }
        
        // Convert pattern to regex
        String regex = pattern.replace("*", ".*");
        
        // Find matching keys
        Map<String, CachedToolResult> allResults = cacheData.getAllToolResults();
        for (String key : allResults.keySet()) {
            if (key.matches(regex)) {
                invalidate(itineraryId, key);
            }
        }
    }
    
    @Override
    public void invalidateAll(String itineraryId) {
        logger.info("Invalidating all cache for itinerary: {}", itineraryId);
        
        // Remove from session cache
        sessionCache.remove(itineraryId);
        
        // Delete all documents in toolCache subcollection
        CompletableFuture.runAsync(() -> {
            try {
                var cacheCollection = firestore.collection("itineraries")
                    .document(itineraryId)
                    .collection("toolCache");
                
                var querySnapshot = cacheCollection.get().get();
                
                int deletedCount = 0;
                for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                    doc.getReference().delete().get();
                    deletedCount++;
                }
                
                logger.debug("Cleared {} cache entries from toolCache subcollection for itinerary: {}", 
                    deletedCount, itineraryId);
                
            } catch (Exception e) {
                logger.error("Failed to clear cache from toolCache subcollection: {}", e.getMessage());
            }
        });
    }
    
    @Override
    public CacheStats getStats(String itineraryId) {
        ItineraryCacheData cacheData = sessionCache.get(itineraryId);
        
        if (cacheData == null) {
            return new CacheStats(itineraryId, 0, 0, 0);
        }
        
        CacheStats stats = new CacheStats(
            itineraryId,
            cacheData.size(),
            cacheData.getTotalHits(),
            cacheData.getTotalMisses()
        );
        
        stats.setLastAccessTime(cacheData.getLastAccessTime());
        
        return stats;
    }
    
    @Override
    public int cleanupExpired(String itineraryId) {
        logger.info("Cleaning up expired cache entries for itinerary: {}", itineraryId);
        
        ItineraryCacheData cacheData = sessionCache.get(itineraryId);
        if (cacheData == null) {
            return 0;
        }
        
        int removedCount = 0;
        Map<String, CachedToolResult> allResults = cacheData.getAllToolResults();
        
        for (Map.Entry<String, CachedToolResult> entry : allResults.entrySet()) {
            if (entry.getValue().isExpired()) {
                invalidate(itineraryId, entry.getKey());
                removedCount++;
            }
        }
        
        logger.info("Removed {} expired cache entries for itinerary: {}", 
            removedCount, itineraryId);
        
        return removedCount;
    }
    
    @Override
    public int cleanupAllExpired() {
        logger.info("Cleaning up expired cache entries for all itineraries");
        
        int totalRemoved = 0;
        
        for (String itineraryId : sessionCache.keySet()) {
            totalRemoved += cleanupExpired(itineraryId);
        }
        
        logger.info("Removed {} expired cache entries total", totalRemoved);
        
        return totalRemoved;
    }
    
    /**
     * Simple get method for retrieving cached values.
     * Returns Optional.empty() if not found or expired.
     */
    @Override
    public <T> java.util.Optional<T> get(String cacheKey, Class<T> resultType) {
        try {
            // Extract itinerary ID from cache key (format: "intent:hash:hash" or similar)
            // For intent cache, we'll use a default itinerary ID
            String itineraryId = "global"; // Use global cache for intent classification
            
            ItineraryCacheData cacheData = ensureCacheLoaded(itineraryId);
            CachedToolResult cached = cacheData.getToolResult(cacheKey);
            
            if (cached != null && cached.isValid()) {
                try {
                    T result = objectMapper.readValue(cached.getResultJson(), resultType);
                    logger.debug("Cache hit for key: {}", cacheKey);
                    return java.util.Optional.of(result);
                } catch (Exception e) {
                    logger.warn("Failed to deserialize cached value for key {}: {}", 
                        cacheKey, e.getMessage());
                    return java.util.Optional.empty();
                }
            }
            
            logger.debug("Cache miss for key: {}", cacheKey);
            return java.util.Optional.empty();
            
        } catch (Exception e) {
            logger.error("Error retrieving from cache: {}", e.getMessage());
            return java.util.Optional.empty();
        }
    }
    
    /**
     * Simple put method for storing values in cache.
     */
    @Override
    public <T> void put(String cacheKey, T value, java.time.Duration ttl) {
        try {
            // Extract itinerary ID from cache key or use global
            String itineraryId = "global"; // Use global cache for intent classification
            
            // Create cached result
            CachedToolResult cached = new CachedToolResult();
            cached.setToolType("intent-classification"); // Default tool type
            cached.setCacheKey(cacheKey);
            
            // Serialize value to JSON
            String resultJson = objectMapper.writeValueAsString(value);
            cached.setResultJson(resultJson);
            cached.setResultClass(value.getClass().getName());
            
            // Set timestamps
            long now = System.currentTimeMillis();
            cached.setCachedAt(now);
            cached.setExpiresAt(now + ttl.toMillis());
            cached.setTtlSeconds((int) ttl.getSeconds());
            
            // Store in session cache
            ItineraryCacheData cacheData = ensureCacheLoaded(itineraryId);
            cacheData.putToolResult(cacheKey, cached);
            
            // Save to Firestore asynchronously
            saveToFirestoreAsync(itineraryId, cacheKey, cached);
            
            logger.debug("Cached value for key: {} with TTL: {}s", cacheKey, ttl.getSeconds());
            
        } catch (Exception e) {
            logger.error("Error storing in cache: {}", e.getMessage());
        }
    }
}
