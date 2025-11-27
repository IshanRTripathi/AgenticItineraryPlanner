package com.tripplanner.dto.tools;

public class GetAvailableSlotsRequest {
    private String itineraryId;
    private Integer dayNumber;
    private Integer minDurationMinutes;
    
    public GetAvailableSlotsRequest() {
    }
    
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
    
    public Integer getMinDurationMinutes() {
        return minDurationMinutes;
    }
    
    public void setMinDurationMinutes(Integer minDurationMinutes) {
        this.minDurationMinutes = minDurationMinutes;
    }
}
