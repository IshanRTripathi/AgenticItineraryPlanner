package com.tripplanner.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Itinerary settings configuration.
 */
public class ItinerarySettings {
    
    @JsonProperty("autoApply")
    private Boolean autoApply = false;
    
    @JsonProperty("defaultScope")
    private String defaultScope = "trip"; // "trip" or "day"
    
    public ItinerarySettings() {}
    
    public ItinerarySettings(Boolean autoApply, String defaultScope) {
        this.autoApply = autoApply;
        this.defaultScope = defaultScope;
    }
    
    // Getters and Setters
    public Boolean getAutoApply() {
        return autoApply;
    }
    
    public void setAutoApply(Boolean autoApply) {
        this.autoApply = autoApply;
    }
    
    public String getDefaultScope() {
        return defaultScope;
    }
    
    public void setDefaultScope(String defaultScope) {
        this.defaultScope = defaultScope;
    }
    
    @Override
    public String toString() {
        return "ItinerarySettings{" +
                "autoApply=" + autoApply +
                ", defaultScope='" + defaultScope + '\'' +
                '}';
    }
}
