package com.tripplanner.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for chat routing.
 */
public class ChatRequest {
    
    @NotBlank(message = "Itinerary ID is required")
    private String itineraryId;
    
    @NotNull(message = "Scope is required")
    private String scope; // "trip" | "day"
    
    private Integer day; // Required if scope is "day"
    
    private String selectedNodeId; // Optional but recommended for better accuracy
    
    @NotBlank(message = "Text is required")
    private String text;
    
    @NotNull(message = "Auto apply preference is required")
    private boolean autoApply;
    
    // Constructors
    public ChatRequest() {}
    
    public ChatRequest(String itineraryId, String scope, Integer day, String selectedNodeId, String text, boolean autoApply) {
        this.itineraryId = itineraryId;
        this.scope = scope;
        this.day = day;
        this.selectedNodeId = selectedNodeId;
        this.text = text;
        this.autoApply = autoApply;
    }
    
    // Getters and Setters
    public String getItineraryId() {
        return itineraryId;
    }
    
    public void setItineraryId(String itineraryId) {
        this.itineraryId = itineraryId;
    }
    
    public String getScope() {
        return scope;
    }
    
    public void setScope(String scope) {
        this.scope = scope;
    }
    
    public Integer getDay() {
        return day;
    }
    
    public void setDay(Integer day) {
        this.day = day;
    }
    
    public String getSelectedNodeId() {
        return selectedNodeId;
    }
    
    public void setSelectedNodeId(String selectedNodeId) {
        this.selectedNodeId = selectedNodeId;
    }
    
    public String getText() {
        return text;
    }
    
    public void setText(String text) {
        this.text = text;
    }
    
    public boolean isAutoApply() {
        return autoApply;
    }
    
    public void setAutoApply(boolean autoApply) {
        this.autoApply = autoApply;
    }
    
    @Override
    public String toString() {
        return "ChatRequest{" +
                "itineraryId='" + itineraryId + '\'' +
                ", scope='" + scope + '\'' +
                ", day=" + day +
                ", selectedNodeId='" + selectedNodeId + '\'' +
                ", text='" + text + '\'' +
                ", autoApply=" + autoApply +
                '}';
    }
}
