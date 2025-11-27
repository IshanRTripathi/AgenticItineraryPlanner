package com.tripplanner.dto.tools;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request DTO for completeness checking tool
 */
public class CompletenessCheckRequest {
    
    @JsonProperty("itineraryId")
    private String itineraryId;
    
    @JsonProperty("checkMeals")
    private Boolean checkMeals = true;
    
    @JsonProperty("checkActivities")
    private Boolean checkActivities = true;
    
    @JsonProperty("checkTransport")
    private Boolean checkTransport = true;
    
    @JsonProperty("checkAccommodation")
    private Boolean checkAccommodation = false;
    
    public CompletenessCheckRequest() {}
    
    public CompletenessCheckRequest(String itineraryId) {
        this.itineraryId = itineraryId;
    }
    
    // Getters and Setters
    
    public String getItineraryId() {
        return itineraryId;
    }
    
    public void setItineraryId(String itineraryId) {
        this.itineraryId = itineraryId;
    }
    
    public Boolean getCheckMeals() {
        return checkMeals;
    }
    
    public void setCheckMeals(Boolean checkMeals) {
        this.checkMeals = checkMeals;
    }
    
    public Boolean getCheckActivities() {
        return checkActivities;
    }
    
    public void setCheckActivities(Boolean checkActivities) {
        this.checkActivities = checkActivities;
    }
    
    public Boolean getCheckTransport() {
        return checkTransport;
    }
    
    public void setCheckTransport(Boolean checkTransport) {
        this.checkTransport = checkTransport;
    }
    
    public Boolean getCheckAccommodation() {
        return checkAccommodation;
    }
    
    public void setCheckAccommodation(Boolean checkAccommodation) {
        this.checkAccommodation = checkAccommodation;
    }
}
