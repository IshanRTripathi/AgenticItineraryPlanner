package com.tripplanner.service.ai.exception;

/**
 * Exception for transient AI errors that may succeed on retry.
 * 
 * Examples:
 * - 503 Service Unavailable (provider overloaded)
 * - 429 Too Many Requests (rate limit)
 * - 500 Internal Server Error (temporary issue)
 * - Network timeouts or connection errors
 * 
 * These errors indicate temporary problems that might resolve
 * if the request is retried or sent to a different provider.
 */
public class TransientAiException extends AiException {
    
    private final int statusCode;
    
    /**
     * Create a new transient AI exception.
     * 
     * @param message Error message
     * @param provider Name of the AI provider that failed
     * @param statusCode HTTP status code (0 for non-HTTP errors)
     */
    public TransientAiException(String message, String provider, int statusCode) {
        super(message, provider);
        this.statusCode = statusCode;
    }
    
    /**
     * Create a new transient AI exception with a cause.
     * 
     * @param message Error message
     * @param provider Name of the AI provider that failed
     * @param statusCode HTTP status code (0 for non-HTTP errors)
     * @param cause The underlying cause
     */
    public TransientAiException(String message, String provider, int statusCode, Throwable cause) {
        super(message, provider, cause);
        this.statusCode = statusCode;
    }
    
    /**
     * Get the HTTP status code that caused this error.
     * 
     * @return HTTP status code, or 0 for non-HTTP errors (e.g., network timeout)
     */
    public int getStatusCode() {
        return statusCode;
    }
    
    @Override
    public boolean isTransient() {
        return true;
    }
    
    @Override
    public String toString() {
        return String.format("TransientAiException[provider=%s, statusCode=%d, message=%s, timestamp=%d]",
                getProvider(), statusCode, getMessage(), getTimestamp());
    }
}
