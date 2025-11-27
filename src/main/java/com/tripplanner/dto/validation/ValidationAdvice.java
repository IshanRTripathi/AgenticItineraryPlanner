package com.tripplanner.dto.validation;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Complete validation advice from ValidationAdvisor
 */
public class ValidationAdvice {
    
    @JsonProperty("success")
    private boolean success = true;
    
    @JsonProperty("itineraryId")
    private String itineraryId;
    
    @JsonProperty("level")
    private ValidationLevel level;
    
    @JsonProperty("summary")
    private ValidationSummary summary = new ValidationSummary();
    
    @JsonProperty("issuesByCategory")
    private Map<String, List<ValidationIssue>> issuesByCategory = new HashMap<>();
    
    @JsonProperty("recommendations")
    private List<Recommendation> recommendations = new ArrayList<>();
    
    @JsonProperty("metadata")
    private ValidationMetadata metadata = new ValidationMetadata();
    
    @JsonProperty("error")
    private String error;
    
    public ValidationAdvice() {}
    
    public static ValidationAdvice skipped(String reason) {
        ValidationAdvice advice = new ValidationAdvice();
        advice.success = true;
        advice.metadata.setSkipped(true);
        advice.metadata.setSkipReason(reason);
        return advice;
    }
    
    public static ValidationAdvice error(String error) {
        ValidationAdvice advice = new ValidationAdvice();
        advice.success = false;
        advice.error = error;
        return advice;
    }
    
    // Getters and Setters
    
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public String getItineraryId() {
        return itineraryId;
    }
    
    public void setItineraryId(String itineraryId) {
        this.itineraryId = itineraryId;
    }
    
    public ValidationLevel getLevel() {
        return level;
    }
    
    public void setLevel(ValidationLevel level) {
        this.level = level;
    }
    
    public ValidationSummary getSummary() {
        return summary;
    }
    
    public void setSummary(ValidationSummary summary) {
        this.summary = summary;
    }
    
    public Map<String, List<ValidationIssue>> getIssuesByCategory() {
        return issuesByCategory;
    }
    
    public void setIssuesByCategory(Map<String, List<ValidationIssue>> issuesByCategory) {
        this.issuesByCategory = issuesByCategory;
    }
    
    public List<Recommendation> getRecommendations() {
        return recommendations;
    }
    
    public void setRecommendations(List<Recommendation> recommendations) {
        this.recommendations = recommendations;
    }
    
    public ValidationMetadata getMetadata() {
        return metadata;
    }
    
    public void setMetadata(ValidationMetadata metadata) {
        this.metadata = metadata;
    }
    
    public String getError() {
        return error;
    }
    
    public void setError(String error) {
        this.error = error;
    }
    
    /**
     * Validation summary
     */
    public static class ValidationSummary {
        @JsonProperty("totalErrors")
        private int totalErrors;
        
        @JsonProperty("totalWarnings")
        private int totalWarnings;
        
        @JsonProperty("criticalIssues")
        private int criticalIssues;
        
        @JsonProperty("filteredIssues")
        private int filteredIssues;
        
        @JsonProperty("score")
        private double score; // 0-100
        
        public int getTotalErrors() {
            return totalErrors;
        }
        
        public void setTotalErrors(int totalErrors) {
            this.totalErrors = totalErrors;
        }
        
        public int getTotalWarnings() {
            return totalWarnings;
        }
        
        public void setTotalWarnings(int totalWarnings) {
            this.totalWarnings = totalWarnings;
        }
        
        public int getCriticalIssues() {
            return criticalIssues;
        }
        
        public void setCriticalIssues(int criticalIssues) {
            this.criticalIssues = criticalIssues;
        }
        
        public int getFilteredIssues() {
            return filteredIssues;
        }
        
        public void setFilteredIssues(int filteredIssues) {
            this.filteredIssues = filteredIssues;
        }
        
        public double getScore() {
            return score;
        }
        
        public void setScore(double score) {
            this.score = score;
        }
    }
    
    /**
     * Recommendation
     */
    public static class Recommendation {
        @JsonProperty("priority")
        private String priority; // HIGH, MEDIUM, LOW
        
        @JsonProperty("category")
        private String category;
        
        @JsonProperty("action")
        private String action;
        
        @JsonProperty("impact")
        private String impact;
        
        @JsonProperty("autoFixable")
        private boolean autoFixable;
        
        public String getPriority() {
            return priority;
        }
        
        public void setPriority(String priority) {
            this.priority = priority;
        }
        
        public String getCategory() {
            return category;
        }
        
        public void setCategory(String category) {
            this.category = category;
        }
        
        public String getAction() {
            return action;
        }
        
        public void setAction(String action) {
            this.action = action;
        }
        
        public String getImpact() {
            return impact;
        }
        
        public void setImpact(String impact) {
            this.impact = impact;
        }
        
        public boolean isAutoFixable() {
            return autoFixable;
        }
        
        public void setAutoFixable(boolean autoFixable) {
            this.autoFixable = autoFixable;
        }
    }
    
    /**
     * Validation metadata
     */
    public static class ValidationMetadata {
        @JsonProperty("validatedAt")
        private Long validatedAt;
        
        @JsonProperty("validationDuration")
        private Long validationDuration;
        
        @JsonProperty("skipped")
        private Boolean skipped;
        
        @JsonProperty("skipReason")
        private String skipReason;
        
        public Long getValidatedAt() {
            return validatedAt;
        }
        
        public void setValidatedAt(Long validatedAt) {
            this.validatedAt = validatedAt;
        }
        
        public Long getValidationDuration() {
            return validationDuration;
        }
        
        public void setValidationDuration(Long validationDuration) {
            this.validationDuration = validationDuration;
        }
        
        public Boolean getSkipped() {
            return skipped;
        }
        
        public void setSkipped(Boolean skipped) {
            this.skipped = skipped;
        }
        
        public String getSkipReason() {
            return skipReason;
        }
        
        public void setSkipReason(String skipReason) {
            this.skipReason = skipReason;
        }
    }
}
