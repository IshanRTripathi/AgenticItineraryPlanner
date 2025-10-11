package com.tripplanner.exception;

/**
 * Exception thrown when an itinerary is not found.
 * Provides context about the itinerary and user involved.
 */
public class ItineraryNotFoundException extends RuntimeException {
    
    private final String itineraryId;
    private final String userId;
    
    public ItineraryNotFoundException(String message, String itineraryId, String userId) {
        super(message);
        this.itineraryId = itineraryId;
        this.userId = userId;
    }
    
    public ItineraryNotFoundException(String message, String itineraryId, String userId, Throwable cause) {
        super(message, cause);
        this.itineraryId = itineraryId;
        this.userId = userId;
    }
    
    public String getItineraryId() {
        return itineraryId;
    }
    
    public String getUserId() {
        return userId;
    }
}