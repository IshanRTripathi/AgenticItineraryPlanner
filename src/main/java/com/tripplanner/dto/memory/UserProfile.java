package com.tripplanner.dto.memory;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * Aggregated user profile from memories
 */
public class UserProfile {
    
    @JsonProperty("userId")
    private String userId;
    
    @JsonProperty("itineraryId")
    private String itineraryId;
    
    @JsonProperty("activityPreferences")
    private Map<String, Object> activityPreferences;
    
    @JsonProperty("budgetBehavior")
    private Map<String, Object> budgetBehavior;
    
    @JsonProperty("timingPatterns")
    private Map<String, Object> timingPatterns;
    
    @JsonProperty("mealPreferences")
    private Map<String, Object> mealPreferences;
    
    @JsonProperty("dietaryRestrictions")
    private List<String> dietaryRestrictions;
    
    @JsonProperty("transportPreferences")
    private Map<String, Object> transportPreferences;
    
    @JsonProperty("validationPreferences")
    private Map<String, Object> validationPreferences;
    
    // Getters and Setters
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getItineraryId() {
        return itineraryId;
    }
    
    public void setItineraryId(String itineraryId) {
        this.itineraryId = itineraryId;
    }
    
    public Map<String, Object> getActivityPreferences() {
        return activityPreferences;
    }
    
    public void setActivityPreferences(Map<String, Object> activityPreferences) {
        this.activityPreferences = activityPreferences;
    }
    
    public Map<String, Object> getBudgetBehavior() {
        return budgetBehavior;
    }
    
    public void setBudgetBehavior(Map<String, Object> budgetBehavior) {
        this.budgetBehavior = budgetBehavior;
    }
    
    public Map<String, Object> getTimingPatterns() {
        return timingPatterns;
    }
    
    public void setTimingPatterns(Map<String, Object> timingPatterns) {
        this.timingPatterns = timingPatterns;
    }
    
    public Map<String, Object> getMealPreferences() {
        return mealPreferences;
    }
    
    public void setMealPreferences(Map<String, Object> mealPreferences) {
        this.mealPreferences = mealPreferences;
    }
    
    public List<String> getDietaryRestrictions() {
        return dietaryRestrictions;
    }
    
    public void setDietaryRestrictions(List<String> dietaryRestrictions) {
        this.dietaryRestrictions = dietaryRestrictions;
    }
    
    public Map<String, Object> getTransportPreferences() {
        return transportPreferences;
    }
    
    public void setTransportPreferences(Map<String, Object> transportPreferences) {
        this.transportPreferences = transportPreferences;
    }
    
    public Map<String, Object> getValidationPreferences() {
        return validationPreferences;
    }
    
    public void setValidationPreferences(Map<String, Object> validationPreferences) {
        this.validationPreferences = validationPreferences;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private UserProfile profile = new UserProfile();
        
        public Builder userId(String userId) {
            profile.userId = userId;
            return this;
        }
        
        public Builder itineraryId(String itineraryId) {
            profile.itineraryId = itineraryId;
            return this;
        }
        
        public Builder activityPreferences(Map<String, Object> activityPreferences) {
            profile.activityPreferences = activityPreferences;
            return this;
        }
        
        public Builder budgetBehavior(Map<String, Object> budgetBehavior) {
            profile.budgetBehavior = budgetBehavior;
            return this;
        }
        
        public Builder timingPatterns(Map<String, Object> timingPatterns) {
            profile.timingPatterns = timingPatterns;
            return this;
        }
        
        public Builder mealPreferences(Map<String, Object> mealPreferences) {
            profile.mealPreferences = mealPreferences;
            return this;
        }
        
        public Builder dietaryRestrictions(List<String> dietaryRestrictions) {
            profile.dietaryRestrictions = dietaryRestrictions;
            return this;
        }
        
        public Builder transportPreferences(Map<String, Object> transportPreferences) {
            profile.transportPreferences = transportPreferences;
            return this;
        }
        
        public Builder validationPreferences(Map<String, Object> validationPreferences) {
            profile.validationPreferences = validationPreferences;
            return this;
        }
        
        public UserProfile build() {
            return profile;
        }
    }
}
