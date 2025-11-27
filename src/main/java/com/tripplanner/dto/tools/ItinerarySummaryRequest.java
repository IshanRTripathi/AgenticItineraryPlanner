package com.tripplanner.dto.tools;

/**
 * Request DTO for get itinerary summary tool.
 * P2-6: Get lightweight summary without loading full itinerary data.
 */
public class ItinerarySummaryRequest {
    
    private String itineraryId;
    
    public ItinerarySummaryRequest() {
    }
    
    public ItinerarySummaryRequest(String itineraryId) {
        this.itineraryId = itineraryId;
    }
    
    // Getters and setters
    
    public String getItineraryId() {
        return itineraryId;
    }
    
    public void setItineraryId(String itineraryId) {
        this.itineraryId = itineraryId;
    }
}
