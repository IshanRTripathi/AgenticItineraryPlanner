package com.tripplanner.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Structured error response model for consistent API error handling.
 * Provides detailed error information including user-friendly messages and suggested actions.
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    
    /**
     * Timestamp when the error occurred
     */
    private LocalDateTime timestamp;
    
    /**
     * HTTP status code
     */
    private int status;
    
    /**
     * Error type/category
     */
    private String error;
    
    /**
     * User-friendly error message
     */
    private String message;
    
    /**
     * Request path that caused the error
     */
    private String path;
    
    /**
     * Additional error details
     */
    private ErrorDetails details;
    
    /**
     * Unique error ID for tracking (optional)
     */
    private String errorId;
}