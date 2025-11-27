package com.tripplanner.dto.tools;

import java.util.ArrayList;
import java.util.List;

/**
 * Result DTO for timing validation tool.
 * P2-2: Returns feasibility analysis of schedule.
 */
public class TimingValidationResult {
    
    private boolean feasible;
    private List<TimingIssue> issues;
    private List<String> warnings;
    private String error;
    
    public TimingValidationResult() {
        this.issues = new ArrayList<>();
        this.warnings = new ArrayList<>();
        this.feasible = true;
    }
    
    public static TimingValidationResult error(String error) {
        TimingValidationResult result = new TimingValidationResult();
        result.feasible = false;
        result.error = error;
        return result;
    }
    
    public void addIssue(TimingIssue issue) {
        this.issues.add(issue);
        this.feasible = false;
    }
    
    public void addWarning(String warning) {
        this.warnings.add(warning);
    }
    
    // Getters and setters
    
    public boolean isFeasible() {
        return feasible;
    }
    
    public void setFeasible(boolean feasible) {
        this.feasible = feasible;
    }
    
    public List<TimingIssue> getIssues() {
        return issues;
    }
    
    public void setIssues(List<TimingIssue> issues) {
        this.issues = issues;
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
     * Individual timing issue.
     */
    public static class TimingIssue {
        private String type; // INSUFFICIENT_TRAVEL_TIME, ACTIVITY_TOO_LONG, etc.
        private String between; // Node IDs
        private Integer required; // minutes
        private Integer available; // minutes
        private String suggestion;
        
        public TimingIssue() {
        }
        
        public TimingIssue(String type, String between, Integer required, Integer available, String suggestion) {
            this.type = type;
            this.between = between;
            this.required = required;
            this.available = available;
            this.suggestion = suggestion;
        }
        
        // Getters and setters
        
        public String getType() {
            return type;
        }
        
        public void setType(String type) {
            this.type = type;
        }
        
        public String getBetween() {
            return between;
        }
        
        public void setBetween(String between) {
            this.between = between;
        }
        
        public Integer getRequired() {
            return required;
        }
        
        public void setRequired(Integer required) {
            this.required = required;
        }
        
        public Integer getAvailable() {
            return available;
        }
        
        public void setAvailable(Integer available) {
            this.available = available;
        }
        
        public String getSuggestion() {
            return suggestion;
        }
        
        public void setSuggestion(String suggestion) {
            this.suggestion = suggestion;
        }
    }
}
