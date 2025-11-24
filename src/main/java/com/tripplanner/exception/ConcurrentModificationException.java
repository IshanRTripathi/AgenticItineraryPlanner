package com.tripplanner.exception;

/**
 * Exception thrown when optimistic locking detects concurrent modification
 */
public class ConcurrentModificationException extends RuntimeException {
    
    private final String itineraryId;
    private final Long expectedVersion;
    private final Long actualVersion;
    
    public ConcurrentModificationException(String itineraryId, Long expectedVersion, Long actualVersion) {
        super(String.format("Itinerary %s was modified by another process. Expected version: %d, Actual version: %d",
                           itineraryId, expectedVersion, actualVersion));
        this.itineraryId = itineraryId;
        this.expectedVersion = expectedVersion;
        this.actualVersion = actualVersion;
    }
    
    public String getItineraryId() {
        return itineraryId;
    }
    
    public Long getExpectedVersion() {
        return expectedVersion;
    }
    
    public Long getActualVersion() {
        return actualVersion;
    }
}
