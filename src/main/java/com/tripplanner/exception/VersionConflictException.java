package com.tripplanner.exception;

/**
 * Exception thrown when there is a version conflict during concurrent updates.
 */
public class VersionConflictException extends RuntimeException {
    
    private final String itineraryId;
    private final Integer expectedVersion;
    private final Integer actualVersion;
    
    public VersionConflictException(String itineraryId, Integer expectedVersion, Integer actualVersion) {
        super(String.format("Version conflict for itinerary %s: expected %d, actual %d", 
              itineraryId, expectedVersion, actualVersion));
        this.itineraryId = itineraryId;
        this.expectedVersion = expectedVersion;
        this.actualVersion = actualVersion;
    }
    
    public VersionConflictException(String itineraryId, Integer expectedVersion, Integer actualVersion, String message) {
        super(message);
        this.itineraryId = itineraryId;
        this.expectedVersion = expectedVersion;
        this.actualVersion = actualVersion;
    }
    
    public VersionConflictException(String itineraryId, Integer expectedVersion, Integer actualVersion, String message, Throwable cause) {
        super(message, cause);
        this.itineraryId = itineraryId;
        this.expectedVersion = expectedVersion;
        this.actualVersion = actualVersion;
    }
    
    public String getItineraryId() {
        return itineraryId;
    }
    
    public Integer getExpectedVersion() {
        return expectedVersion;
    }
    
    public Integer getActualVersion() {
        return actualVersion;
    }
}