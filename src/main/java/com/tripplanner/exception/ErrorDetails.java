package com.tripplanner.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

/**
 * Additional error details for structured error responses.
 * Contains context-specific information about the error.
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorDetails {
    
    /**
     * Whether the operation can be retried
     */
    private Boolean retryable;
    
    /**
     * Suggested action for the user
     */
    private String suggestedAction;
    
    /**
     * AI provider that failed (for AI service errors)
     */
    private String provider;
    
    /**
     * Operation that was being performed
     */
    private String operation;
    
    /**
     * User ID (for ownership/access errors)
     */
    private String userId;
    
    /**
     * Itinerary ID (for itinerary-related errors)
     */
    private String itineraryId;
    
    /**
     * Field that failed validation (for validation errors)
     */
    private String field;
    
    /**
     * Value that failed validation (for validation errors)
     */
    private String value;
    
    /**
     * Error type for categorization
     */
    private String errorType;
    
    /**
     * Whether this is a generation-in-progress scenario
     */
    private Boolean generationInProgress;
    
    /**
     * Retry delay in seconds (for rate limiting)
     */
    private Integer retryAfterSeconds;
    
    /**
     * Additional context information
     */
    private String context;
}