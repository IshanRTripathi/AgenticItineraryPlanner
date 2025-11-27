package com.tripplanner.dto.tools;

import java.util.ArrayList;
import java.util.List;

/**
 * Result DTO for get itinerary summary tool.
 * P2-6: Returns lightweight summary of itinerary.
 */
public class ItinerarySummaryResult {
    
    private boolean success;
    private String destination;
    private String startDate; // ISO format
    private String endDate; // ISO format
    private int dayCount;
    private Integer partySize;
    private Double budgetMax;
    private String currency;
    private List<String> constraints;
    private int totalNodes;
    private String state; // DRAFT, SKELETON, ENRICHED, etc.
    private String error;
    
    public ItinerarySummaryResult() {
        this.constraints = new ArrayList<>();
    }
    
    public static ItinerarySummaryResult error(String error) {
        ItinerarySummaryResult result = new ItinerarySummaryResult();
        result.success = false;
        result.error = error;
        return result;
    }
    
    // Getters and setters
    
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public String getDestination() {
        return destination;
    }
    
    public void setDestination(String destination) {
        this.destination = destination;
    }
    
    public String getStartDate() {
        return startDate;
    }
    
    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }
    
    public String getEndDate() {
        return endDate;
    }
    
    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }
    
    public int getDayCount() {
        return dayCount;
    }
    
    public void setDayCount(int dayCount) {
        this.dayCount = dayCount;
    }
    
    public Integer getPartySize() {
        return partySize;
    }
    
    public void setPartySize(Integer partySize) {
        this.partySize = partySize;
    }
    
    public Double getBudgetMax() {
        return budgetMax;
    }
    
    public void setBudgetMax(Double budgetMax) {
        this.budgetMax = budgetMax;
    }
    
    public String getCurrency() {
        return currency;
    }
    
    public void setCurrency(String currency) {
        this.currency = currency;
    }
    
    public List<String> getConstraints() {
        return constraints;
    }
    
    public void setConstraints(List<String> constraints) {
        this.constraints = constraints;
    }
    
    public int getTotalNodes() {
        return totalNodes;
    }
    
    public void setTotalNodes(int totalNodes) {
        this.totalNodes = totalNodes;
    }
    
    public String getState() {
        return state;
    }
    
    public void setState(String state) {
        this.state = state;
    }
    
    public String getError() {
        return error;
    }
    
    public void setError(String error) {
        this.error = error;
    }
}
