package com.tripplanner.exception;

/**
 * Exception thrown when AI service operations fail.
 * Provides context about which provider and operation failed.
 */
public class AiServiceException extends RuntimeException {
    
    private final String provider;
    private final String operation;
    
    public AiServiceException(String message, String provider, String operation) {
        super(message);
        this.provider = provider;
        this.operation = operation;
    }
    
    public AiServiceException(String message, String provider, String operation, Throwable cause) {
        super(message, cause);
        this.provider = provider;
        this.operation = operation;
    }
    
    public String getProvider() {
        return provider;
    }
    
    public String getOperation() {
        return operation;
    }
}