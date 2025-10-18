package com.tripplanner.dto;

/**
 * Status enum for itinerary creation process.
 */
public enum CreationStatus {
    /**
     * Itinerary creation is in progress.
     */
    PROCESSING,
    
    /**
     * Itinerary creation completed successfully.
     */
    COMPLETED,
    
    /**
     * Itinerary creation failed.
     */
    FAILED,
    
    /**
     * Itinerary creation partially completed with some failures.
     */
    PARTIAL_COMPLETION
}