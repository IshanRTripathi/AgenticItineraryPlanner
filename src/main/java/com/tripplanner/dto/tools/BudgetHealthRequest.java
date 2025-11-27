package com.tripplanner.dto.tools;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request DTO for budget health checking tool
 */
public class BudgetHealthRequest {
    
    @JsonProperty("itineraryId")
    private String itineraryId;
    
    @JsonProperty("includeProjections")
    private Boolean includeProjections = true;
    
    @JsonProperty("includeBreakdown")
    private Boolean includeBreakdown = true;
    
    public BudgetHealthRequest() {}
    
    public BudgetHealthRequest(String itineraryId) {
        this.itineraryId = itineraryId;
    }
    
    // Getters and Setters
    
    public String getItineraryId() {
        return itineraryId;
    }
    
    public void setItineraryId(String itineraryId) {
        this.itineraryId = itineraryId;
    }
    
    public Boolean getIncludeProjections() {
        return includeProjections;
    }
    
    public void setIncludeProjections(Boolean includeProjections) {
        this.includeProjections = includeProjections;
    }
    
    public Boolean getIncludeBreakdown() {
        return includeBreakdown;
    }
    
    public void setIncludeBreakdown(Boolean includeBreakdown) {
        this.includeBreakdown = includeBreakdown;
    }
}
