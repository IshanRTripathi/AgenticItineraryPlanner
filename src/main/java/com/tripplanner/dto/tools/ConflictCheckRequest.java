package com.tripplanner.dto.tools;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tripplanner.dto.ChangeSet;

/**
 * Request DTO for conflict checking tool
 */
public class ConflictCheckRequest {
    
    @JsonProperty("itineraryId")
    private String itineraryId;
    
    @JsonProperty("proposedChanges")
    private ChangeSet proposedChanges;
    
    @JsonProperty("checkTimeConflicts")
    private Boolean checkTimeConflicts = true;
    
    @JsonProperty("checkBudgetConflicts")
    private Boolean checkBudgetConflicts = true;
    
    public ConflictCheckRequest() {}
    
    public ConflictCheckRequest(String itineraryId, ChangeSet proposedChanges) {
        this.itineraryId = itineraryId;
        this.proposedChanges = proposedChanges;
    }
    
    // Getters and Setters
    public String getItineraryId() {
        return itineraryId;
    }
    
    public void setItineraryId(String itineraryId) {
        this.itineraryId = itineraryId;
    }
    
    public ChangeSet getProposedChanges() {
        return proposedChanges;
    }
    
    public void setProposedChanges(ChangeSet proposedChanges) {
        this.proposedChanges = proposedChanges;
    }
    
    public Boolean getCheckTimeConflicts() {
        return checkTimeConflicts;
    }
    
    public void setCheckTimeConflicts(Boolean checkTimeConflicts) {
        this.checkTimeConflicts = checkTimeConflicts;
    }
    
    public Boolean getCheckBudgetConflicts() {
        return checkBudgetConflicts;
    }
    
    public void setCheckBudgetConflicts(Boolean checkBudgetConflicts) {
        this.checkBudgetConflicts = checkBudgetConflicts;
    }
}
