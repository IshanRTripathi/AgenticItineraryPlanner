package com.tripplanner.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Activity-specific metadata
 */
public class ActivityMetadata extends NodeMetadata {
    
    @JsonProperty("category")
    private String category;
    
    @JsonProperty("estimatedDurationMinutes")
    private Integer estimatedDurationMinutes;
    
    @JsonProperty("difficultyLevel")
    private String difficultyLevel;
    
    @JsonProperty("requiresBooking")
    private Boolean requiresBooking;
    
    @JsonProperty("isIndoor")
    private Boolean isIndoor;

    public ActivityMetadata() {
        super("activity");
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Integer getEstimatedDurationMinutes() {
        return estimatedDurationMinutes;
    }

    public void setEstimatedDurationMinutes(Integer estimatedDurationMinutes) {
        this.estimatedDurationMinutes = estimatedDurationMinutes;
    }

    public String getDifficultyLevel() {
        return difficultyLevel;
    }

    public void setDifficultyLevel(String difficultyLevel) {
        this.difficultyLevel = difficultyLevel;
    }

    public Boolean getRequiresBooking() {
        return requiresBooking;
    }

    public void setRequiresBooking(Boolean requiresBooking) {
        this.requiresBooking = requiresBooking;
    }

    public Boolean getIsIndoor() {
        return isIndoor;
    }

    public void setIsIndoor(Boolean isIndoor) {
        this.isIndoor = isIndoor;
    }
}
