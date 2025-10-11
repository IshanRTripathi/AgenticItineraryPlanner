package com.tripplanner.service;

import com.tripplanner.dto.AgentTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

/**
 * Utility class for handling retry logic with exponential backoff and jitter.
 * Provides configurable retry strategies for different types of operations.
 */
@Component
public class RetryHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(RetryHandler.class);
    
    /**
     * Execute an operation with retry logic.
     * 
     * @param operation The operation to execute
     * @param retryConfig The retry configuration
     * @param operationName Name for logging purposes
     * @return The result of the operation
     * @throws Exception If all retry attempts fail
     */
    public <T> T executeWithRetry(Supplier<T> operation, AgentTask.RetryConfig retryConfig, 
                                 String operationName) throws Exception {
        Exception lastException = null;
        
        for (int attempt = 1; attempt <= retryConfig.getMaxAttempts(); attempt++) {
            try {
                logger.debug("Executing {} - attempt {}/{}", operationName, attempt, retryConfig.getMaxAttempts());
                
                T result = operation.get();
                
                if (attempt > 1) {
                    logger.info("Operation {} succeeded on attempt {}/{}", 
                               operationName, attempt, retryConfig.getMaxAttempts());
                }
                
                return result;
                
            } catch (Exception e) {
                lastException = e;
                
                if (attempt == retryConfig.getMaxAttempts()) {
                    logger.error("Operation {} failed after {} attempts", 
                                operationName, retryConfig.getMaxAttempts(), e);
                    break;
                }
                
                if (!isRetryableException(e)) {
                    logger.error("Operation {} failed with non-retryable exception", operationName, e);
                    break;
                }
                
                long delayMs = calculateDelay(attempt - 1, retryConfig);
                logger.warn("Operation {} failed on attempt {}/{}, retrying in {}ms: {}", 
                           operationName, attempt, retryConfig.getMaxAttempts(), delayMs, e.getMessage());
                
                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Retry interrupted", ie);
                }
            }
        }
        
        throw new RuntimeException("Operation failed after " + retryConfig.getMaxAttempts() + " attempts", lastException);
    }
    
    /**
     * Calculate delay for the next retry attempt with exponential backoff and jitter.
     * 
     * @param attemptNumber The attempt number (0-based)
     * @param retryConfig The retry configuration
     * @return Delay in milliseconds
     */
    public long calculateDelay(int attemptNumber, AgentTask.RetryConfig retryConfig) {
        long baseDelayMs = retryConfig.getBaseDelayMs();
        double multiplier = retryConfig.getBackoffMultiplier();
        long maxDelayMs = retryConfig.getMaxDelayMs();
        
        // Exponential backoff
        long delay = (long) (baseDelayMs * Math.pow(multiplier, attemptNumber));
        delay = Math.min(delay, maxDelayMs);
        
        // Add jitter (±25% of the delay)
        double jitterFactor = 0.25;
        long jitter = (long) (delay * jitterFactor * (ThreadLocalRandom.current().nextDouble() * 2 - 1));
        delay += jitter;
        
        // Ensure minimum delay
        return Math.max(delay, baseDelayMs);
    }
    
    /**
     * Determine if an exception is retryable.
     * 
     * @param exception The exception to check
     * @return true if the exception is retryable, false otherwise
     */
    public boolean isRetryableException(Exception exception) {
        if (exception == null) {
            return false;
        }
        
        // Network-related exceptions are generally retryable
        if (isNetworkException(exception)) {
            return true;
        }
        
        // Timeout exceptions are retryable
        if (isTimeoutException(exception)) {
            return true;
        }
        
        // Service unavailable exceptions are retryable
        if (isServiceUnavailableException(exception)) {
            return true;
        }
        
        // Rate limit exceptions are retryable
        if (isRateLimitException(exception)) {
            return true;
        }
        
        // Temporary failures are retryable
        if (isTemporaryFailure(exception)) {
            return true;
        }
        
        // Check exception message for retryable patterns
        String message = exception.getMessage();
        if (message != null) {
            String lowerMessage = message.toLowerCase();
            
            // Common retryable error patterns
            if (lowerMessage.contains("timeout") ||
                lowerMessage.contains("connection") ||
                lowerMessage.contains("network") ||
                lowerMessage.contains("unavailable") ||
                lowerMessage.contains("rate limit") ||
                lowerMessage.contains("too many requests") ||
                lowerMessage.contains("server error") ||
                lowerMessage.contains("internal error") ||
                lowerMessage.contains("temporary")) {
                return true;
            }
        }
        
        // Default to non-retryable for unknown exceptions
        logger.debug("Exception not considered retryable: {}", exception.getClass().getSimpleName());
        return false;
    }
    
    /**
     * Check if exception is network-related.
     */
    private boolean isNetworkException(Exception exception) {
        String className = exception.getClass().getSimpleName().toLowerCase();
        return className.contains("connect") ||
               className.contains("socket") ||
               className.contains("network") ||
               className.contains("io") ||
               className.contains("http");
    }
    
    /**
     * Check if exception is timeout-related.
     */
    private boolean isTimeoutException(Exception exception) {
        String className = exception.getClass().getSimpleName().toLowerCase();
        return className.contains("timeout") ||
               exception instanceof java.util.concurrent.TimeoutException ||
               exception instanceof java.net.SocketTimeoutException;
    }
    
    /**
     * Check if exception indicates service unavailability.
     */
    private boolean isServiceUnavailableException(Exception exception) {
        String className = exception.getClass().getSimpleName().toLowerCase();
        return className.contains("unavailable") ||
               className.contains("service");
    }
    
    /**
     * Check if exception indicates rate limiting.
     */
    private boolean isRateLimitException(Exception exception) {
        String message = exception.getMessage();
        if (message != null) {
            String lowerMessage = message.toLowerCase();
            return lowerMessage.contains("rate limit") ||
                   lowerMessage.contains("too many requests") ||
                   lowerMessage.contains("quota exceeded");
        }
        return false;
    }
    
    /**
     * Check if exception indicates a temporary failure.
     */
    private boolean isTemporaryFailure(Exception exception) {
        String message = exception.getMessage();
        if (message != null) {
            String lowerMessage = message.toLowerCase();
            return lowerMessage.contains("temporary") ||
                   lowerMessage.contains("transient") ||
                   lowerMessage.contains("retry");
        }
        return false;
    }
    
    /**
     * Create a default retry configuration for different operation types.
     */
    public static AgentTask.RetryConfig createDefaultRetryConfig(RetryStrategy strategy) {
        switch (strategy) {
            case AGGRESSIVE:
                return new AgentTask.RetryConfig(5, 500L, 30000L, 2.0);
            case CONSERVATIVE:
                return new AgentTask.RetryConfig(2, 2000L, 60000L, 1.5);
            case NETWORK:
                return new AgentTask.RetryConfig(3, 1000L, 10000L, 2.0);
            case DATABASE:
                return new AgentTask.RetryConfig(3, 100L, 5000L, 2.0);
            case EXTERNAL_API:
                return new AgentTask.RetryConfig(4, 2000L, 60000L, 2.5);
            default:
                return new AgentTask.RetryConfig(); // Default configuration
        }
    }
    
    /**
     * Retry strategies for different types of operations.
     */
    public enum RetryStrategy {
        AGGRESSIVE,    // Many attempts, short delays
        CONSERVATIVE,  // Few attempts, longer delays
        NETWORK,       // Optimized for network operations
        DATABASE,      // Optimized for database operations
        EXTERNAL_API,  // Optimized for external API calls
        DEFAULT        // Standard retry configuration
    }
    
    /**
     * Builder for creating custom retry configurations.
     */
    public static class RetryConfigBuilder {
        private Integer maxAttempts = 3;
        private Long baseDelayMs = 1000L;
        private Long maxDelayMs = 300000L;
        private Double backoffMultiplier = 2.0;
        
        public RetryConfigBuilder maxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
            return this;
        }
        
        public RetryConfigBuilder baseDelay(long baseDelayMs) {
            this.baseDelayMs = baseDelayMs;
            return this;
        }
        
        public RetryConfigBuilder maxDelay(long maxDelayMs) {
            this.maxDelayMs = maxDelayMs;
            return this;
        }
        
        public RetryConfigBuilder backoffMultiplier(double multiplier) {
            this.backoffMultiplier = multiplier;
            return this;
        }
        
        public AgentTask.RetryConfig build() {
            return new AgentTask.RetryConfig(maxAttempts, baseDelayMs, maxDelayMs, backoffMultiplier);
        }
    }
    
    /**
     * Create a retry configuration builder.
     */
    public static RetryConfigBuilder builder() {
        return new RetryConfigBuilder();
    }
}