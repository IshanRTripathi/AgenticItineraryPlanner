package com.tripplanner.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Error event DTO that extends ItineraryUpdateEvent for error-specific information.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorEvent extends ItineraryUpdateEvent {
    
    @JsonProperty("severity")
    private ErrorSeverity severity;
    
    @JsonProperty("errorCode")
    private String errorCode;
    
    @JsonProperty("recoveryAction")
    private String recoveryAction;
    
    @JsonProperty("canRetry")
    private Boolean canRetry;
    
    @JsonProperty("stackTrace")
    private String stackTrace; // Only for debugging, not sent to frontend
    
    public ErrorEvent() {
        super();
        setEventType("error");
    }
    
    public ErrorEvent(String itineraryId, String executionId, ErrorSeverity severity, 
                     String errorCode, String message) {
        super("error", itineraryId, executionId);
        this.severity = severity;
        this.errorCode = errorCode;
        setMessage(message);
    }
    
    /**
     * Create a warning error event.
     */
    public static ErrorEvent warning(String itineraryId, String executionId, String errorCode, 
                                   String message, String recoveryAction) {
        ErrorEvent event = new ErrorEvent(itineraryId, executionId, ErrorSeverity.WARNING, errorCode, message);
        event.setRecoveryAction(recoveryAction);
        event.setCanRetry(true);
        return event;
    }
    
    /**
     * Create an error event.
     */
    public static ErrorEvent error(String itineraryId, String executionId, String errorCode, 
                                 String message, boolean canRetry) {
        ErrorEvent event = new ErrorEvent(itineraryId, executionId, ErrorSeverity.ERROR, errorCode, message);
        event.setCanRetry(canRetry);
        return event;
    }
    
    /**
     * Create a critical error event.
     */
    public static ErrorEvent critical(String itineraryId, String executionId, String errorCode, 
                                    String message, String recoveryAction) {
        ErrorEvent event = new ErrorEvent(itineraryId, executionId, ErrorSeverity.CRITICAL, errorCode, message);
        event.setRecoveryAction(recoveryAction);
        event.setCanRetry(false);
        return event;
    }
    
    /**
     * Create error event from exception.
     */
    public static ErrorEvent fromException(String itineraryId, String executionId, 
                                         Exception exception, ErrorSeverity severity) {
        ErrorEvent event = new ErrorEvent(itineraryId, executionId, severity, 
                                        exception.getClass().getSimpleName(), exception.getMessage());
        
        // Add stack trace for debugging (not sent to frontend)
        if (exception.getStackTrace() != null && exception.getStackTrace().length > 0) {
            StringBuilder stackTrace = new StringBuilder();
            for (int i = 0; i < Math.min(5, exception.getStackTrace().length); i++) {
                stackTrace.append(exception.getStackTrace()[i].toString()).append("\n");
            }
            event.setStackTrace(stackTrace.toString());
        }
        
        return event;
    }
    
    /**
     * Check if this is a recoverable error.
     */
    public boolean isRecoverable() {
        return Boolean.TRUE.equals(canRetry) && severity != ErrorSeverity.CRITICAL;
    }
    
    /**
     * Check if this is a critical error that should stop processing.
     */
    public boolean isCritical() {
        return severity == ErrorSeverity.CRITICAL;
    }
    
    // Getters and Setters
    public ErrorSeverity getSeverity() {
        return severity;
    }
    
    public void setSeverity(ErrorSeverity severity) {
        this.severity = severity;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }
    
    public String getRecoveryAction() {
        return recoveryAction;
    }
    
    public void setRecoveryAction(String recoveryAction) {
        this.recoveryAction = recoveryAction;
    }
    
    public Boolean getCanRetry() {
        return canRetry;
    }
    
    public void setCanRetry(Boolean canRetry) {
        this.canRetry = canRetry;
    }
    
    public String getStackTrace() {
        return stackTrace;
    }
    
    public void setStackTrace(String stackTrace) {
        this.stackTrace = stackTrace;
    }
    
    /**
     * Error severity levels.
     */
    public enum ErrorSeverity {
        /**
         * Warning - processing can continue with degraded functionality.
         */
        WARNING,
        
        /**
         * Error - processing failed but can be retried or recovered.
         */
        ERROR,
        
        /**
         * Critical - processing must stop, manual intervention required.
         */
        CRITICAL
    }
    
    @Override
    public String toString() {
        return "ErrorEvent{" +
                "severity=" + severity +
                ", errorCode='" + errorCode + '\'' +
                ", message='" + getMessage() + '\'' +
                ", recoveryAction='" + recoveryAction + '\'' +
                ", canRetry=" + canRetry +
                ", itineraryId='" + getItineraryId() + '\'' +
                ", executionId='" + getExecutionId() + '\'' +
                '}';
    }
}