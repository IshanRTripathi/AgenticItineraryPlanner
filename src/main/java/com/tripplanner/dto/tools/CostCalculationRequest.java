package com.tripplanner.dto.tools;

/**
 * Request DTO for cost calculation tool.
 * P0-5: Calculate itinerary costs with budget tracking.
 */
public class CostCalculationRequest {
    
    private String itineraryId;
    private Integer partySize;
    private Boolean includeBudgetAnalysis;
    
    public CostCalculationRequest() {
    }
    
    public CostCalculationRequest(String itineraryId, Integer partySize) {
        this.itineraryId = itineraryId;
        this.partySize = partySize;
        this.includeBudgetAnalysis = true;
    }
    
    // Getters and setters
    
    public String getItineraryId() {
        return itineraryId;
    }
    
    public void setItineraryId(String itineraryId) {
        this.itineraryId = itineraryId;
    }
    
    public Integer getPartySize() {
        return partySize;
    }
    
    public void setPartySize(Integer partySize) {
        this.partySize = partySize;
    }
    
    public Boolean getIncludeBudgetAnalysis() {
        return includeBudgetAnalysis;
    }
    
    public void setIncludeBudgetAnalysis(Boolean includeBudgetAnalysis) {
        this.includeBudgetAnalysis = includeBudgetAnalysis;
    }
}
