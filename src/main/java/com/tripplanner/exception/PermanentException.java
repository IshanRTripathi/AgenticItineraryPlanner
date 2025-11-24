package com.tripplanner.exception;

/**
 * Exception that indicates the operation should not be retried
 */
public class PermanentException extends RuntimeException {
    
    private final String errorCode;
    private final String userMessage;
    
    public PermanentException(String message) {
        super(message);
        this.errorCode = "PERMANENT_ERROR";
        this.userMessage = "An error occurred that cannot be automatically resolved.";
    }
    
    public PermanentException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "PERMANENT_ERROR";
        this.userMessage = "An error occurred that cannot be automatically resolved.";
    }
    
    public PermanentException(String message, String errorCode, String userMessage) {
        super(message);
        this.errorCode = errorCode;
        this.userMessage = userMessage;
    }
    
    public PermanentException(String message, Throwable cause, String errorCode, String userMessage) {
        super(message, cause);
        this.errorCode = errorCode;
        this.userMessage = userMessage;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public String getUserMessage() {
        return userMessage;
    }
}
