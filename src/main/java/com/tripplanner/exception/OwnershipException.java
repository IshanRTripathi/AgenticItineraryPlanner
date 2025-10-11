package com.tripplanner.exception;

/**
 * Exception thrown when ownership validation fails.
 * Provides context about the user and itinerary involved.
 */
public class OwnershipException extends RuntimeException {
    
    private final String userId;
    private final String itineraryId;
    private final String operation;
    
    public OwnershipException(String message, String userId, String itineraryId, String operation) {
        super(message);
        this.userId = userId;
        this.itineraryId = itineraryId;
        this.operation = operation;
    }
    
    public OwnershipException(String message, String userId, String itineraryId, String operation, Throwable cause) {
        super(message, cause);
        this.userId = userId;
        this.itineraryId = itineraryId;
        this.operation = operation;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public String getItineraryId() {
        return itineraryId;
    }
    
    public String getOperation() {
        return operation;
    }
}