package com.tripplanner.dto.tools;

import java.util.ArrayList;
import java.util.List;

/**
 * Result DTO for check opening hours tool.
 * P2-5: Returns whether place is open and provides recommendations.
 */
public class CheckOpeningHoursResult {
    
    private boolean success;
    private boolean open;
    private String opensAt; // HH:mm format
    private String closesAt; // HH:mm format
    private boolean specialHours; // Holiday or special event hours
    private String recommendation; // e.g., "Visit earlier" or "Place is closed on this day"
    private List<String> weekdayText; // Full week schedule (e.g., ["Monday: 9:00 AM – 5:00 PM", ...])
    private String todayHours; // Today's hours in readable format
    private Boolean openAtRequestedTime; // If time was provided in request
    private String error;
    private List<String> warnings;
    
    public CheckOpeningHoursResult() {
        this.warnings = new ArrayList<>();
        this.weekdayText = new ArrayList<>();
    }
    
    public static CheckOpeningHoursResult error(String error) {
        CheckOpeningHoursResult result = new CheckOpeningHoursResult();
        result.success = false;
        result.error = error;
        return result;
    }
    
    public void addWarning(String warning) {
        this.warnings.add(warning);
    }
    
    // Getters and setters
    
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public boolean isOpen() {
        return open;
    }
    
    public void setOpen(boolean open) {
        this.open = open;
    }
    
    public String getOpensAt() {
        return opensAt;
    }
    
    public void setOpensAt(String opensAt) {
        this.opensAt = opensAt;
    }
    
    public String getClosesAt() {
        return closesAt;
    }
    
    public void setClosesAt(String closesAt) {
        this.closesAt = closesAt;
    }
    
    public boolean isSpecialHours() {
        return specialHours;
    }
    
    public void setSpecialHours(boolean specialHours) {
        this.specialHours = specialHours;
    }
    
    public String getRecommendation() {
        return recommendation;
    }
    
    public void setRecommendation(String recommendation) {
        this.recommendation = recommendation;
    }
    
    public String getError() {
        return error;
    }
    
    public void setError(String error) {
        this.error = error;
    }
    
    public List<String> getWarnings() {
        return warnings;
    }
    
    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }
    
    public List<String> getWeekdayText() {
        return weekdayText;
    }
    
    public void setWeekdayText(List<String> weekdayText) {
        this.weekdayText = weekdayText;
    }
    
    public String getTodayHours() {
        return todayHours;
    }
    
    public void setTodayHours(String todayHours) {
        this.todayHours = todayHours;
    }
    
    public Boolean getOpenAtRequestedTime() {
        return openAtRequestedTime;
    }
    
    public void setOpenAtRequestedTime(Boolean openAtRequestedTime) {
        this.openAtRequestedTime = openAtRequestedTime;
    }
}
