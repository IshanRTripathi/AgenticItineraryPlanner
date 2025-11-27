package com.tripplanner.dto.tools;

public class ValidateCompletenessRequest {
    private String itineraryId;
    
    public ValidateCompletenessRequest() {
    }
    
    public ValidateCompletenessRequest(String itineraryId) {
        this.itineraryId = itineraryId;
    }
    
    public String getItineraryId() {
        return itineraryId;
    }
    
    public void setItineraryId(String itineraryId) {
        this.itineraryId = itineraryId;
    }
}
