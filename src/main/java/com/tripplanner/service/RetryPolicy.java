package com.tripplanner.service;

import com.tripplanner.dto.AgentTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages retry policies for different operation types and tracks retry attempts.
 * Provides configurable retry strategies with dead letter queue integration.
 */
@Component
public class RetryPolicy {
    
    private static final Logger logger = LoggerFactory.getLogger(RetryPolicy.class);
    
    // Retry attempt tracking
    private final Map<String, RetryAttempt> retryAttempts = new ConcurrentHashMap<>();
    
    // Default retry configurations for different operation types
    private final Map<String, AgentTask.RetryConfig> defaultConfigs = new HashMap<>();
    
    public RetryPolicy() {
        initializeDefaultConfigs();
    }
    
    /**
     * Initialize default retry configurations for different operation types.
     */
    private void initializeDefaultConfigs() {
        // Task processing operations
        defaultConfigs.put("task_processing", new AgentTask.RetryConfig(5, 1000L, 300000L, 2.0));
        
        // Change engine operations
        defaultConfigs.put("change_application", new AgentTask.RetryConfig(3, 500L, 30000L, 2.0));
        
        // Enrichment operations
        defaultConfigs.put("enrichment_request", new AgentTask.RetryConfig(4, 2000L, 60000L, 2.5));
        
        // Place registry operations
        defaultConfigs.put("place_registration", new AgentTask.RetryConfig(3, 1000L, 15000L, 2.0));
        
        // External API calls
        defaultConfigs.put("external_api", new AgentTask.RetryConfig(4, 2000L, 60000L, 2.5));
        
        // Database operations
        defaultConfigs.put("database_operation", new AgentTask.RetryConfig(3, 100L, 5000L, 2.0));
        
        // LLM operations
        defaultConfigs.put("llm_request", new AgentTask.RetryConfig(3, 3000L, 120000L, 2.0));
        
        // Lock operations
        defaultConfigs.put("lock_operation", new AgentTask.RetryConfig(5, 200L, 10000L, 1.5));
        
        // Default fallback
        defaultConfigs.put("default", new AgentTask.RetryConfig(3, 1000L, 30000L, 2.0));
    }
    
    /**
     * Get retry configuration for a specific operation type.
     * 
     * @param operationType The type of operation
     * @return The retry configuration for the operation type
     */
    public AgentTask.RetryConfig getRetryConfig(String operationType) {
        return defaultConfigs.getOrDefault(operationType, defaultConfigs.get("default"));
    }
    
    /**
     * Record a retry attempt for tracking purposes.
     * 
     * @param operationId Unique identifier for the operation
     * @param operationType Type of operation being retried
     * @param attemptNumber Current attempt number (1-based)
     * @param exception The exception that caused the retry
     */
    public void recordRetryAttempt(String operationId, String operationType, 
                                 int attemptNumber, Exception exception) {
        RetryAttempt attempt = retryAttempts.computeIfAbsent(operationId, 
            k -> new RetryAttempt(operationId, operationType));
        
        attempt.recordAttempt(attemptNumber, exception);
        
        logger.debug("Recorded retry attempt {}/{} for operation {} (type: {}): {}", 
                    attemptNumber, getRetryConfig(operationType).getMaxAttempts(),
                    operationId, operationType, exception.getMessage());
    }
    
    /**
     * Check if an operation should be retried based on the retry policy.
     * 
     * @param operationId Unique identifier for the operation
     * @param operationType Type of operation
     * @param currentAttempt Current attempt number (1-based)
     * @param exception The exception that occurred
     * @return true if the operation should be retried, false otherwise
     */
    public boolean shouldRetry(String operationId, String operationType, 
                              int currentAttempt, Exception exception) {
        AgentTask.RetryConfig config = getRetryConfig(operationType);
        
        // Check if we've exceeded maximum attempts
        if (currentAttempt >= config.getMaxAttempts()) {
            logger.info("Operation {} exceeded maximum retry attempts ({}/{})", 
                       operationId, currentAttempt, config.getMaxAttempts());
            return false;
        }
        
        // Check if the exception is retryable
        RetryHandler retryHandler = new RetryHandler();
        if (!retryHandler.isRetryableException(exception)) {
            logger.info("Operation {} failed with non-retryable exception: {}", 
                       operationId, exception.getClass().getSimpleName());
            return false;
        }
        
        return true;
    }
    
    /**
     * Calculate the delay before the next retry attempt.
     * 
     * @param operationId Unique identifier for the operation
     * @param operationType Type of operation
     * @param attemptNumber Current attempt number (0-based for calculation)
     * @return Delay in milliseconds
     */
    public long calculateRetryDelay(String operationId, String operationType, int attemptNumber) {
        AgentTask.RetryConfig config = getRetryConfig(operationType);
        RetryHandler retryHandler = new RetryHandler();
        
        long delay = retryHandler.calculateDelay(attemptNumber, config);
        
        logger.debug("Calculated retry delay for operation {} (attempt {}): {}ms", 
                    operationId, attemptNumber + 1, delay);
        
        return delay;
    }
    
    /**
     * Mark an operation as successful and clean up retry tracking.
     * 
     * @param operationId Unique identifier for the operation
     */
    public void markOperationSuccessful(String operationId) {
        RetryAttempt attempt = retryAttempts.remove(operationId);
        if (attempt != null) {
            logger.debug("Operation {} completed successfully after {} attempts", 
                        operationId, attempt.getAttemptCount());
        }
    }
    
    /**
     * Mark an operation as failed and prepare for dead letter queue.
     * 
     * @param operationId Unique identifier for the operation
     * @return RetryAttempt containing failure details
     */
    public RetryAttempt markOperationFailed(String operationId) {
        RetryAttempt attempt = retryAttempts.remove(operationId);
        if (attempt != null) {
            attempt.markFailed();
            logger.warn("Operation {} marked as failed after {} attempts", 
                       operationId, attempt.getAttemptCount());
        }
        return attempt;
    }
    
    /**
     * Get retry statistics for monitoring.
     * 
     * @return RetryStatistics containing current retry information
     */
    public RetryStatistics getRetryStatistics() {
        Map<String, Integer> attemptsByType = new HashMap<>();
        int totalActiveRetries = retryAttempts.size();
        int totalFailedOperations = 0;
        
        for (RetryAttempt attempt : retryAttempts.values()) {
            String type = attempt.getOperationType();
            attemptsByType.merge(type, 1, Integer::sum);
            
            if (attempt.isFailed()) {
                totalFailedOperations++;
            }
        }
        
        return new RetryStatistics(totalActiveRetries, totalFailedOperations, attemptsByType);
    }
    
    /**
     * Clean up old retry attempts to prevent memory leaks.
     * This method is called periodically by a scheduled task.
     */
    @Scheduled(fixedRate = 1800000) // Run every 30 minutes
    public void cleanupOldAttempts() {
        Instant cutoff = Instant.now().minusSeconds(3600); // 1 hour ago
        
        retryAttempts.entrySet().removeIf(entry -> {
            RetryAttempt attempt = entry.getValue();
            return attempt.getLastAttemptTime().isBefore(cutoff);
        });
    }
    
    /**
     * Represents a retry attempt for tracking purposes.
     */
    public static class RetryAttempt {
        private final String operationId;
        private final String operationType;
        private final Instant startTime;
        private Instant lastAttemptTime;
        private int attemptCount;
        private Exception lastException;
        private boolean failed;
        
        public RetryAttempt(String operationId, String operationType) {
            this.operationId = operationId;
            this.operationType = operationType;
            this.startTime = Instant.now();
            this.lastAttemptTime = startTime;
            this.attemptCount = 0;
            this.failed = false;
        }
        
        public void recordAttempt(int attemptNumber, Exception exception) {
            this.attemptCount = attemptNumber;
            this.lastException = exception;
            this.lastAttemptTime = Instant.now();
        }
        
        public void markFailed() {
            this.failed = true;
        }
        
        // Getters
        public String getOperationId() { return operationId; }
        public String getOperationType() { return operationType; }
        public Instant getStartTime() { return startTime; }
        public Instant getLastAttemptTime() { return lastAttemptTime; }
        public int getAttemptCount() { return attemptCount; }
        public Exception getLastException() { return lastException; }
        public boolean isFailed() { return failed; }
    }
    
    /**
     * Statistics about retry operations.
     */
    public static class RetryStatistics {
        private final int totalActiveRetries;
        private final int totalFailedOperations;
        private final Map<String, Integer> attemptsByType;
        
        public RetryStatistics(int totalActiveRetries, int totalFailedOperations, 
                             Map<String, Integer> attemptsByType) {
            this.totalActiveRetries = totalActiveRetries;
            this.totalFailedOperations = totalFailedOperations;
            this.attemptsByType = new HashMap<>(attemptsByType);
        }
        
        // Getters
        public int getTotalActiveRetries() { return totalActiveRetries; }
        public int getTotalFailedOperations() { return totalFailedOperations; }
        public Map<String, Integer> getAttemptsByType() { return attemptsByType; }
    }
}