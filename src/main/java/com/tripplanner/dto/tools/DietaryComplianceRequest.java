package com.tripplanner.dto.tools;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Request DTO for dietary compliance checking tool
 */
public class DietaryComplianceRequest {
    
    @JsonProperty("itineraryId")
    private String itineraryId;
    
    @JsonProperty("restrictions")
    private List<String> restrictions;
    
    @JsonProperty("strictness")
    private String strictness = "STRICT"; // STRICT, MODERATE, FLEXIBLE
    
    public DietaryComplianceRequest() {}
    
    public DietaryComplianceRequest(String itineraryId) {
        this.itineraryId = itineraryId;
    }
    
    // Getters and Setters
    
    public String getItineraryId() {
        return itineraryId;
    }
    
    public void setItineraryId(String itineraryId) {
        this.itineraryId = itineraryId;
    }
    
    public List<String> getRestrictions() {
        return restrictions;
    }
    
    public void setRestrictions(List<String> restrictions) {
        this.restrictions = restrictions;
    }
    
    public String getStrictness() {
        return strictness;
    }
    
    public void setStrictness(String strictness) {
        this.strictness = strictness;
    }
}
