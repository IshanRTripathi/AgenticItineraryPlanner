package com.tripplanner.dto.tools;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tripplanner.dto.NormalizedNode;

/**
 * Request DTO for constraint validation tool
 */
public class ConstraintCheckRequest {
    
    @JsonProperty("itineraryId")
    private String itineraryId;
    
    @JsonProperty("proposedNode")
    private NormalizedNode proposedNode; // Optional: validate a specific node
    
    @JsonProperty("checkBudget")
    private Boolean checkBudget = true;
    
    @JsonProperty("checkDietary")
    private Boolean checkDietary = true;
    
    @JsonProperty("checkPartySize")
    private Boolean checkPartySize = true;
    
    public ConstraintCheckRequest() {}
    
    public ConstraintCheckRequest(String itineraryId) {
        this.itineraryId = itineraryId;
    }
    
    // Getters and Setters
    public String getItineraryId() {
        return itineraryId;
    }
    
    public void setItineraryId(String itineraryId) {
        this.itineraryId = itineraryId;
    }
    
    public NormalizedNode getProposedNode() {
        return proposedNode;
    }
    
    public void setProposedNode(NormalizedNode proposedNode) {
        this.proposedNode = proposedNode;
    }
    
    public Boolean getCheckBudget() {
        return checkBudget;
    }
    
    public void setCheckBudget(Boolean checkBudget) {
        this.checkBudget = checkBudget;
    }
    
    public Boolean getCheckDietary() {
        return checkDietary;
    }
    
    public void setCheckDietary(Boolean checkDietary) {
        this.checkDietary = checkDietary;
    }
    
    public Boolean getCheckPartySize() {
        return checkPartySize;
    }
    
    public void setCheckPartySize(Boolean checkPartySize) {
        this.checkPartySize = checkPartySize;
    }
}
