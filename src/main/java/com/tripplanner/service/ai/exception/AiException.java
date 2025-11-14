package com.tripplanner.service.ai.exception;

/**
 * Base exception for all AI-related errors.
 * Provides common fields for tracking provider and timing information.
 */
public abstract class AiException extends RuntimeException {
    
    private final String provider;
    private final long timestamp;
    
    /**
     * Create a new AI exception.
     * 
     * @param message Error message
     * @param provider Name of the AI provider that failed
     */
    public AiException(String message, String provider) {
        super(message);
        this.provider = provider;
        this.timestamp = System.currentTimeMillis();
    }
    
    /**
     * Create a new AI exception with a cause.
     * 
     * @param message Error message
     * @param provider Name of the AI provider that failed
     * @param cause The underlying cause
     */
    public AiException(String message, String provider, Throwable cause) {
        super(message, cause);
        this.provider = provider;
        this.timestamp = System.currentTimeMillis();
    }
    
    /**
     * Get the name of the provider that failed.
     * 
     * @return Provider name (e.g., "GeminiClient", "OpenRouterClient")
     */
    public String getProvider() {
        return provider;
    }
    
    /**
     * Get the timestamp when the error occurred.
     * 
     * @return Timestamp in milliseconds since epoch
     */
    public long getTimestamp() {
        return timestamp;
    }
    
    /**
     * Check if this error is transient (may succeed on retry).
     * 
     * @return true if transient, false if permanent
     */
    public abstract boolean isTransient();
}
