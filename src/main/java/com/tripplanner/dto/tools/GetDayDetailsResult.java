package com.tripplanner.dto.tools;

import com.tripplanner.dto.NormalizedNode;
import java.util.ArrayList;
import java.util.List;

/**
 * Result DTO for get day details tool.
 * P2-7: Returns detailed information for a specific day.
 */
public class GetDayDetailsResult {
    
    private boolean success;
    private Integer dayNumber;
    private String date; // ISO format
    private List<NormalizedNode> nodes;
    private double totalCost;
    private String currency;
    private String startTime; // HH:mm format
    private String endTime; // HH:mm format
    private String error;
    
    public GetDayDetailsResult() {
        this.nodes = new ArrayList<>();
    }
    
    public static GetDayDetailsResult error(String error) {
        GetDayDetailsResult result = new GetDayDetailsResult();
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
    
    public Integer getDayNumber() {
        return dayNumber;
    }
    
    public void setDayNumber(Integer dayNumber) {
        this.dayNumber = dayNumber;
    }
    
    public String getDate() {
        return date;
    }
    
    public void setDate(String date) {
        this.date = date;
    }
    
    public List<NormalizedNode> getNodes() {
        return nodes;
    }
    
    public void setNodes(List<NormalizedNode> nodes) {
        this.nodes = nodes;
    }
    
    public double getTotalCost() {
        return totalCost;
    }
    
    public void setTotalCost(double totalCost) {
        this.totalCost = totalCost;
    }
    
    public String getCurrency() {
        return currency;
    }
    
    public void setCurrency(String currency) {
        this.currency = currency;
    }
    
    public String getStartTime() {
        return startTime;
    }
    
    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }
    
    public String getEndTime() {
        return endTime;
    }
    
    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }
    
    public String getError() {
        return error;
    }
    
    public void setError(String error) {
        this.error = error;
    }
}
