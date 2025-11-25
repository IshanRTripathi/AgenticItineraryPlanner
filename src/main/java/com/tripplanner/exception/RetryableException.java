package com.tripplanner.exception;

/**
 * Exception that indicates the operation can be retried
 */
public class RetryableException extends RuntimeException {
    
    private final int currentAttempt;
    private final int maxAttempts;
    
    public RetryableException(String message) {
        super(message);
        this.currentAttempt = 0;
        this.maxAttempts = 3;
    }
    
    public RetryableException(String message, Throwable cause) {
        super(message, cause);
        this.currentAttempt = 0;
        this.maxAttempts = 3;
    }
    
    public RetryableException(String message, int currentAttempt, int maxAttempts) {
        super(message);
        this.currentAttempt = currentAttempt;
        this.maxAttempts = maxAttempts;
    }
    
    public RetryableException(String message, Throwable cause, int currentAttempt, int maxAttempts) {
        super(message, cause);
        this.currentAttempt = currentAttempt;
        this.maxAttempts = maxAttempts;
    }
    
    public int getCurrentAttempt() {
        return currentAttempt;
    }
    
    public int getMaxAttempts() {
        return maxAttempts;
    }
    
    public boolean canRetry() {
        return currentAttempt < maxAttempts;
    }
}
