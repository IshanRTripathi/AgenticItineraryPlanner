package com.tripplanner.service;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.WriteResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

/**
 * Manages idempotency keys and operation result caching to prevent duplicate processing.
 * Ensures that operations with the same idempotency key return the same result without side effects.
 */
@Service
public class IdempotencyManager {
    
    private static final Logger logger = LoggerFactory.getLogger(IdempotencyManager.class);
    private static final String IDEMPOTENCY_COLLECTION = "idempotencyKeys";
    private static final long DEFAULT_TTL_HOURS = 24; // 24 hours default TTL
    
    @Autowired
    private Firestore firestore;
    
    /**
     * Check if an operation with the given idempotency key has already been processed.
     * 
     * @param idempotencyKey The unique key for the operation
     * @return Optional containing the cached result if found, empty otherwise
     */
    public Optional<IdempotencyRecord> getExistingOperation(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.trim().isEmpty()) {
            return Optional.empty();
        }
        
        try {
            DocumentReference docRef = firestore.collection(IDEMPOTENCY_COLLECTION)
                    .document(idempotencyKey);
            
            DocumentSnapshot snapshot = docRef.get().get();
            
            if (!snapshot.exists()) {
                return Optional.empty();
            }
            
            IdempotencyRecord record = snapshot.toObject(IdempotencyRecord.class);
            
            // Check if record has expired
            if (record != null && isExpired(record)) {
                logger.debug("Idempotency record expired for key: {}", idempotencyKey);
                // Clean up expired record asynchronously
                docRef.delete();
                return Optional.empty();
            }
            
            logger.debug("Found existing idempotency record for key: {}", idempotencyKey);
            return Optional.ofNullable(record);
            
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Failed to check idempotency key: {}", idempotencyKey, e);
            return Optional.empty();
        }
    }
    
    /**
     * Store the result of an operation with the given idempotency key.
     * 
     * @param idempotencyKey The unique key for the operation
     * @param result The result to cache
     * @param operationType The type of operation performed
     * @param ttlHours Time to live in hours (optional, defaults to 24 hours)
     */
    public void storeOperationResult(String idempotencyKey, Object result, 
                                   String operationType, Long ttlHours) {
        if (idempotencyKey == null || idempotencyKey.trim().isEmpty()) {
            logger.warn("Cannot store operation result with null or empty idempotency key");
            return;
        }
        
        try {
            long ttl = ttlHours != null ? ttlHours : DEFAULT_TTL_HOURS;
            Instant expiresAt = Instant.now().plusSeconds(ttl * 3600);
            
            IdempotencyRecord record = new IdempotencyRecord(
                idempotencyKey,
                result,
                operationType,
                Instant.now(),
                expiresAt
            );
            
            DocumentReference docRef = firestore.collection(IDEMPOTENCY_COLLECTION)
                    .document(idempotencyKey);
            
            WriteResult writeResult = docRef.set(record).get();
            logger.debug("Stored idempotency record for key: {} at {}", 
                        idempotencyKey, writeResult.getUpdateTime());
            
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Failed to store idempotency record for key: {}", idempotencyKey, e);
        }
    }
    
    /**
     * Store the result of an operation with default TTL.
     */
    public void storeOperationResult(String idempotencyKey, Object result, String operationType) {
        storeOperationResult(idempotencyKey, result, operationType, null);
    }
    
    /**
     * Validate that an idempotency key is properly formatted and not expired.
     * 
     * @param idempotencyKey The key to validate
     * @return true if valid, false otherwise
     */
    public boolean isValidIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.trim().isEmpty()) {
            return false;
        }
        
        // Check length constraints
        if (idempotencyKey.length() > 255) {
            logger.warn("Idempotency key too long: {} characters", idempotencyKey.length());
            return false;
        }
        
        // Check for valid characters (alphanumeric, underscore, hyphen, dot)
        if (!idempotencyKey.matches("^[a-zA-Z0-9_.-]+$")) {
            logger.warn("Idempotency key contains invalid characters: {}", idempotencyKey);
            return false;
        }
        
        return true;
    }
    
    /**
     * Generate a standard idempotency key for common operations.
     * 
     * @param operationType The type of operation
     * @param entityId The ID of the entity being operated on
     * @param additionalContext Additional context for uniqueness
     * @return A formatted idempotency key
     */
    public String generateIdempotencyKey(String operationType, String entityId, String additionalContext) {
        StringBuilder key = new StringBuilder();
        key.append(operationType);
        key.append("_");
        key.append(entityId);
        
        if (additionalContext != null && !additionalContext.trim().isEmpty()) {
            key.append("_");
            key.append(additionalContext);
        }
        
        // Add timestamp component for uniqueness within the same operation
        key.append("_");
        key.append(System.currentTimeMillis());
        
        return key.toString();
    }
    
    /**
     * Clean up expired idempotency records.
     * This method is called periodically by a scheduled task.
     */
    @Scheduled(fixedRate = 3600000) // Run every hour
    public void cleanupExpiredRecords() {
        try {
            Instant now = Instant.now();
            
            firestore.collection(IDEMPOTENCY_COLLECTION)
                    .whereLessThan("expiresAt", now)
                    .get()
                    .get()
                    .getDocuments()
                    .forEach(doc -> {
                        try {
                            doc.getReference().delete().get();
                            logger.debug("Cleaned up expired idempotency record: {}", doc.getId());
                        } catch (InterruptedException | ExecutionException e) {
                            logger.warn("Failed to delete expired idempotency record: {}", doc.getId(), e);
                        }
                    });
                    
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Failed to cleanup expired idempotency records", e);
        }
    }
    
    /**
     * Check if an idempotency record has expired.
     */
    private boolean isExpired(IdempotencyRecord record) {
        return record.getExpiresAt() != null && 
               Instant.now().isAfter(record.getExpiresAt());
    }
    
    /**
     * Represents a cached operation result with idempotency key.
     */
    public static class IdempotencyRecord {
        private String idempotencyKey;
        private Object result;
        private String operationType;
        private Instant createdAt;
        private Instant expiresAt;
        
        // Default constructor for Firestore
        public IdempotencyRecord() {}
        
        public IdempotencyRecord(String idempotencyKey, Object result, String operationType, 
                               Instant createdAt, Instant expiresAt) {
            this.idempotencyKey = idempotencyKey;
            this.result = result;
            this.operationType = operationType;
            this.createdAt = createdAt;
            this.expiresAt = expiresAt;
        }
        
        // Getters and setters
        public String getIdempotencyKey() { return idempotencyKey; }
        public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
        
        public Object getResult() { return result; }
        public void setResult(Object result) { this.result = result; }
        
        public String getOperationType() { return operationType; }
        public void setOperationType(String operationType) { this.operationType = operationType; }
        
        public Instant getCreatedAt() { return createdAt; }
        public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
        
        public Instant getExpiresAt() { return expiresAt; }
        public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    }
}