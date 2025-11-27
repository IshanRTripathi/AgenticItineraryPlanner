package com.tripplanner.dto.tools;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * Response DTO for dietary compliance checking tool
 */
public class DietaryComplianceResult {
    
    @JsonProperty("success")
    private boolean success = true;
    
    @JsonProperty("isCompliant")
    private boolean isCompliant = true;
    
    @JsonProperty("violations")
    private List<DietaryViolation> violations = new ArrayList<>();
    
    @JsonProperty("warnings")
    private List<String> warnings = new ArrayList<>();
    
    @JsonProperty("error")
    private String error;
    
    public DietaryComplianceResult() {}
    
    public void addViolation(DietaryViolation violation) {
        this.violations.add(violation);
        this.isCompliant = false;
    }
    
    public void addWarning(String warning) {
        this.warnings.add(warning);
    }
    
    public static DietaryComplianceResult error(String error) {
        DietaryComplianceResult result = new DietaryComplianceResult();
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
    
    public boolean isCompliant() {
        return isCompliant;
    }
    
    public void setCompliant(boolean compliant) {
        isCompliant = compliant;
    }
    
    public List<DietaryViolation> getViolations() {
        return violations;
    }
    
    public void setViolations(List<DietaryViolation> violations) {
        this.violations = violations;
    }
    
    public List<String> getWarnings() {
        return warnings;
    }
    
    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }
    
    public String getError() {
        return error;
    }
    
    public void setError(String error) {
        this.error = error;
    }
    
    /**
     * Inner class for dietary violations
     */
    public static class DietaryViolation {
        @JsonProperty("nodeId")
        private String nodeId;
        
        @JsonProperty("dayNumber")
        private Integer dayNumber;
        
        @JsonProperty("mealTitle")
        private String mealTitle;
        
        @JsonProperty("restriction")
        private String restriction;
        
        @JsonProperty("reason")
        private String reason;
        
        @JsonProperty("recommendation")
        private String recommendation;
        
        public DietaryViolation() {}
        
        public DietaryViolation(String nodeId, Integer dayNumber, String mealTitle, 
                               String restriction, String reason, String recommendation) {
            this.nodeId = nodeId;
            this.dayNumber = dayNumber;
            this.mealTitle = mealTitle;
            this.restriction = restriction;
            this.reason = reason;
            this.recommendation = recommendation;
        }
        
        // Getters and Setters
        
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
        
        public String getMealTitle() {
            return mealTitle;
        }
        
        public void setMealTitle(String mealTitle) {
            this.mealTitle = mealTitle;
        }
        
        public String getRestriction() {
            return restriction;
        }
        
        public void setRestriction(String restriction) {
            this.restriction = restriction;
        }
        
        public String getReason() {
            return reason;
        }
        
        public void setReason(String reason) {
            this.reason = reason;
        }
        
        public String getRecommendation() {
            return recommendation;
        }
        
        public void setRecommendation(String recommendation) {
            this.recommendation = recommendation;
        }
    }
}
