package com.tripplanner.service.ai.exception;

/**
 * Exception for permanent AI errors that will not succeed on retry.
 * 
 * Examples:
 * - 401 Unauthorized (invalid API key)
 * - 403 Forbidden (insufficient permissions)
 * - 400 Bad Request (invalid request format)
 * - 404 Not Found (invalid endpoint)
 * 
 * These errors indicate configuration or request problems that
 * won't be resolved by retrying. The provider should be skipped
 * and the next provider should be tried instead.
 */
public class PermanentAiException extends AiException {
    
    private final int statusCode;
    
    /**
     * Create a new permanent AI exception.
     * 
     * @param message Error message
     * @param provider Name of the AI provider that failed
     * @param statusCode HTTP status code
     */
    public PermanentAiException(String message, String provider, int statusCode) {
        super(message, provider);
        this.statusCode = statusCode;
    }
    
    /**
     * Create a new permanent AI exception with a cause.
     * 
     * @param message Error message
     * @param provider Name of the AI provider that failed
     * @param statusCode HTTP status code
     * @param cause The underlying cause
     */
    public PermanentAiException(String message, String provider, int statusCode, Throwable cause) {
        super(message, provider, cause);
        this.statusCode = statusCode;
    }
    
    /**
     * Get the HTTP status code that caused this error.
     * 
     * @return HTTP status code
     */
    public int getStatusCode() {
        return statusCode;
    }
    
    @Override
    public boolean isTransient() {
        return false;
    }
    
    @Override
    public String toString() {
        return String.format("PermanentAiException[provider=%s, statusCode=%d, message=%s, timestamp=%d]",
                getProvider(), statusCode, getMessage(), getTimestamp());
    }
}
