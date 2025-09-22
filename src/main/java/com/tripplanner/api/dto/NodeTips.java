package com.tripplanner.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Node tips and warnings.
 */
public class NodeTips {
    
    @JsonProperty("travel")
    private List<String> travel;
    
    @JsonProperty("warnings")
    private List<String> warnings;
    
    public NodeTips() {}
    
    public NodeTips(List<String> travel, List<String> warnings) {
        this.travel = travel;
        this.warnings = warnings;
    }
    
    // Getters and Setters
    public List<String> getTravel() {
        return travel;
    }
    
    public void setTravel(List<String> travel) {
        this.travel = travel;
    }
    
    public List<String> getWarnings() {
        return warnings;
    }
    
    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }
    
    @Override
    public String toString() {
        return "NodeTips{" +
                "travel=" + travel +
                ", warnings=" + warnings +
                '}';
    }
}
