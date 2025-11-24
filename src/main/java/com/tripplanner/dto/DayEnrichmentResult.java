package com.tripplanner.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Result of enriching a single day.
 * Contains all the enriched nodes without modifying the itinerary.
 */
public class DayEnrichmentResult {
    private int dayNumber;
    private List<EnrichedNodeData> enrichedNodes;
    private boolean success;
    private String errorMessage;
    
    public DayEnrichmentResult(int dayNumber) {
        this.dayNumber = dayNumber;
        this.enrichedNodes = new ArrayList<>();
        this.success = true;
    }
    
    public static DayEnrichmentResult failure(int dayNumber, String errorMessage) {
        DayEnrichmentResult result = new DayEnrichmentResult(dayNumber);
        result.success = false;
        result.errorMessage = errorMessage;
        return result;
    }
    
    public void addEnrichedNode(EnrichedNodeData nodeData) {
        this.enrichedNodes.add(nodeData);
    }
    
    // Getters and setters
    public int getDayNumber() {
        return dayNumber;
    }
    
    public void setDayNumber(int dayNumber) {
        this.dayNumber = dayNumber;
    }
    
    public List<EnrichedNodeData> getEnrichedNodes() {
        return enrichedNodes;
    }
    
    public void setEnrichedNodes(List<EnrichedNodeData> enrichedNodes) {
        this.enrichedNodes = enrichedNodes;
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
