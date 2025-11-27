package com.tripplanner.dto.tools;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * Response DTO for timing feasibility checking tool
 */
public class TimingFeasibilityResult {
    
    @JsonProperty("success")
    private boolean success = true;
    
    @JsonProperty("isFeasible")
    private boolean isFeasible = true;
    
    @JsonProperty("issues")
    private List<TimingIssue> issues = new ArrayList<>();
    
    @JsonProperty("recommendations")
    private List<String> recommendations = new ArrayList<>();
    
    @JsonProperty("error")
    private String error;
    
    public TimingFeasibilityResult() {}
    
    public void addIssue(TimingIssue issue) {
        this.issues.add(issue);
        this.isFeasible = false;
    }
    
    public void addRecommendation(String recommendation) {
        this.recommendations.add(recommendation);
    }
    
    public static TimingFeasibilityResult error(String error) {
        TimingFeasibilityResult result = new TimingFeasibilityResult();
        result.success = false;
        result.error = error;
        return result;
    }
    
    // Getters and Setters
    
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public boolean isFeasible() {
        return isFeasible;
    }
    
    public void setFeasible(boolean feasible) {
        isFeasible = feasible;
    }
    
    public List<TimingIssue> getIssues() {
        return issues;
    }
    
    public void setIssues(List<TimingIssue> issues) {
        this.issues = issues;
    }
    
    public List<String> getRecommendations() {
        return recommendations;
    }
    
    public void setRecommendations(List<String> recommendations) {
        this.recommendations = recommendations;
    }
    
    public String getError() {
        return error;
    }
    
    public void setError(String error) {
        this.error = error;
    }
    
    /**
     * Inner class for timing issues
     */
    public static class TimingIssue {
        @JsonProperty("type")
        private String type; // UNREALISTIC_DURATION, TIGHT_SCHEDULE, CLOSED_VENUE
        
        @JsonProperty("nodeId")
        private String nodeId;
        
        @JsonProperty("dayNumber")
        private Integer dayNumber;
        
        @JsonProperty("message")
        private String message;
        
        @JsonProperty("recommendation")
        private String recommendation;
        
        public TimingIssue() {}
        
        public TimingIssue(String type, String nodeId, Integer dayNumber, String message, String recommendation) {
            this.type = type;
            this.nodeId = nodeId;
            this.dayNumber = dayNumber;
            this.message = message;
            this.recommendation = recommendation;
        }
        
        // Getters and Setters
        
        public String getType() {
            return type;
        }
        
        public void setType(String type) {
            this.type = type;
        }
        
        public String getNodeId() {
            return nodeId;
        }
        
        public void setNodeId(String nodeId) {
            this.nodeId = nodeId;
        }
        
        public Integer getDayNumber() {
            return dayNumber;
        }
        
        public void setDayNumber(Integer dayNumber) {
            this.dayNumber = dayNumber;
        }
        
        public String getMessage() {
            return message;
        }
        
        public void setMessage(String message) {
            this.message = message;
        }
        
        public String getRecommendation() {
            return recommendation;
        }
        
        public void setRecommendation(String recommendation) {
            this.recommendation = recommendation;
        }
    }
}
