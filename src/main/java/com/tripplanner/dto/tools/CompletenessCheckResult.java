package com.tripplanner.dto.tools;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * Response DTO for completeness checking tool
 */
public class CompletenessCheckResult {
    
    @JsonProperty("success")
    private boolean success = true;
    
    @JsonProperty("isComplete")
    private boolean isComplete = true;
    
    @JsonProperty("missingItems")
    private List<CompletenessIssue> missingItems = new ArrayList<>();
    
    @JsonProperty("recommendations")
    private List<String> recommendations = new ArrayList<>();
    
    @JsonProperty("error")
    private String error;
    
    public CompletenessCheckResult() {}
    
    public void addMissingItem(CompletenessIssue issue) {
        this.missingItems.add(issue);
        this.isComplete = false;
    }
    
    public void addRecommendation(String recommendation) {
        this.recommendations.add(recommendation);
    }
    
    public static CompletenessCheckResult error(String error) {
        CompletenessCheckResult result = new CompletenessCheckResult();
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
    
    public boolean isComplete() {
        return isComplete;
    }
    
    public void setComplete(boolean complete) {
        isComplete = complete;
    }
    
    public List<CompletenessIssue> getMissingItems() {
        return missingItems;
    }
    
    public void setMissingItems(List<CompletenessIssue> missingItems) {
        this.missingItems = missingItems;
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
     * Inner class for completeness issues
     */
    public static class CompletenessIssue {
        @JsonProperty("type")
        private String type; // MISSING_BREAKFAST, MISSING_ACTIVITY, etc.
        
        @JsonProperty("dayNumber")
        private Integer dayNumber;
        
        @JsonProperty("message")
        private String message;
        
        @JsonProperty("recommendation")
        private String recommendation;
        
        public CompletenessIssue() {}
        
        public CompletenessIssue(String type, Integer dayNumber, String message, String recommendation) {
            this.type = type;
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
