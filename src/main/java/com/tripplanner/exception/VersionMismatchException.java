package com.tripplanner.exception;

import com.tripplanner.dto.ItineraryDiff;

/**
 * Exception thrown when a ChangeSet is applied to an itinerary with a different version
 * than expected, indicating a potential conflict.
 */
public class VersionMismatchException extends RuntimeException {
    
    private final String itineraryId;
    private final Integer expectedVersion;
    private final Integer actualVersion;
    private final ItineraryDiff conflictDetails;
    
    public VersionMismatchException(String itineraryId, Integer expectedVersion, Integer actualVersion) {
        super(String.format("Version mismatch for itinerary %s: expected %d, actual %d", 
                           itineraryId, expectedVersion, actualVersion));
        this.itineraryId = itineraryId;
        this.expectedVersion = expectedVersion;
        this.actualVersion = actualVersion;
        this.conflictDetails = null;
    }
    
    public VersionMismatchException(String itineraryId, Integer expectedVersion, Integer actualVersion, 
                                  ItineraryDiff conflictDetails) {
        super(String.format("Version mismatch for itinerary %s: expected %d, actual %d. Conflicts detected.", 
                           itineraryId, expectedVersion, actualVersion));
        this.itineraryId = itineraryId;
        this.expectedVersion = expectedVersion;
        this.actualVersion = actualVersion;
        this.conflictDetails = conflictDetails;
    }
    
    public VersionMismatchException(String itineraryId, Integer expectedVersion, Integer actualVersion, 
                                  String message) {
        super(message);
        this.itineraryId = itineraryId;
        this.expectedVersion = expectedVersion;
        this.actualVersion = actualVersion;
        this.conflictDetails = null;
    }
    
    public VersionMismatchException(String itineraryId, Integer expectedVersion, Integer actualVersion, 
                                  String message, Throwable cause) {
        super(message, cause);
        this.itineraryId = itineraryId;
        this.expectedVersion = expectedVersion;
        this.actualVersion = actualVersion;
        this.conflictDetails = null;
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
    
    public ItineraryDiff getConflictDetails() {
        return conflictDetails;
    }
    
    public boolean hasConflictDetails() {
        return conflictDetails != null;
    }
    
    /**
     * Get a detailed error message including conflict information.
     */
    public String getDetailedMessage() {
        StringBuilder message = new StringBuilder(getMessage());
        
        if (conflictDetails != null) {
            message.append("\nConflict details:");
            
            if (conflictDetails.getAdded() != null && !conflictDetails.getAdded().isEmpty()) {
                message.append("\n  Added items: ").append(conflictDetails.getAdded().size());
            }
            
            if (conflictDetails.getRemoved() != null && !conflictDetails.getRemoved().isEmpty()) {
                message.append("\n  Removed items: ").append(conflictDetails.getRemoved().size());
            }
            
            if (conflictDetails.getUpdated() != null && !conflictDetails.getUpdated().isEmpty()) {
                message.append("\n  Updated items: ").append(conflictDetails.getUpdated().size());
            }
        }
        
        return message.toString();
    }
}