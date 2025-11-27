package com.tripplanner.dto.tools;

/**
 * Request DTO for get day details tool.
 * P2-7: Get detailed information for a specific day.
 */
public class GetDayDetailsRequest {
    
    private String itineraryId;
    private Integer dayNumber;
    
    public GetDayDetailsRequest() {
    }
    
    public GetDayDetailsRequest(String itineraryId, Integer dayNumber) {
        this.itineraryId = itineraryId;
        this.dayNumber = dayNumber;
    }
    
    // Getters and setters
    
    public String getItineraryId() {
        return itineraryId;
    }
    
    public void setItineraryId(String itineraryId) {
        this.itineraryId = itineraryId;
    }
    
    public Integer getDayNumber() {
        return dayNumber;
    }
    
    public void setDayNumber(Integer dayNumber) {
        this.dayNumber = dayNumber;
    }
}
