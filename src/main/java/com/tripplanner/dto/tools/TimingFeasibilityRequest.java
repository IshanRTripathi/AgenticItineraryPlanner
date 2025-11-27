package com.tripplanner.dto.tools;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request DTO for timing feasibility checking tool
 */
public class TimingFeasibilityRequest {
    
    @JsonProperty("itineraryId")
    private String itineraryId;
    
    @JsonProperty("includeTravel")
    private Boolean includeTravel = true;
    
    @JsonProperty("checkOpeningHours")
    private Boolean checkOpeningHours = false;
    
    public TimingFeasibilityRequest() {}
    
    public TimingFeasibilityRequest(String itineraryId) {
        this.itineraryId = itineraryId;
    }
    
    // Getters and Setters
    
    public String getItineraryId() {
        return itineraryId;
    }
    
    public void setItineraryId(String itineraryId) {
        this.itineraryId = itineraryId;
    }
    
    public Boolean getIncludeTravel() {
        return includeTravel;
    }
    
    public void setIncludeTravel(Boolean includeTravel) {
        this.includeTravel = includeTravel;
    }
    
    public Boolean getCheckOpeningHours() {
        return checkOpeningHours;
    }
    
    public void setCheckOpeningHours(Boolean checkOpeningHours) {
        this.checkOpeningHours = checkOpeningHours;
    }
}
