package com.tripplanner.dto.tools;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Response DTO for budget health checking tool
 */
public class BudgetHealthResult {
    
    @JsonProperty("success")
    private boolean success = true;
    
    @JsonProperty("status")
    private String status; // UNDER, MODERATE, NEAR, OVER
    
    @JsonProperty("totalCost")
    private double totalCost;
    
    @JsonProperty("budgetMax")
    private double budgetMax;
    
    @JsonProperty("budgetRemaining")
    private double budgetRemaining;
    
    @JsonProperty("usedPercentage")
    private double usedPercentage;
    
    @JsonProperty("categoryBreakdown")
    private Map<String, Double> categoryBreakdown = new HashMap<>();
    
    @JsonProperty("warnings")
    private List<String> warnings = new ArrayList<>();
    
    @JsonProperty("recommendations")
    private List<String> recommendations = new ArrayList<>();
    
    @JsonProperty("error")
    private String error;
    
    public BudgetHealthResult() {}
    
    public void addWarning(String warning) {
        this.warnings.add(warning);
    }
    
    public void addRecommendation(String recommendation) {
        this.recommendations.add(recommendation);
    }
    
    public static BudgetHealthResult error(String error) {
        BudgetHealthResult result = new BudgetHealthResult();
        result.success = false;
        result.error = error;
        return result;
    }
    
    // Getters and Setters
    
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public double getTotalCost() {
        return totalCost;
    }
    
    public void setTotalCost(double totalCost) {
        this.totalCost = totalCost;
    }
    
    public double getBudgetMax() {
        return budgetMax;
    }
    
    public void setBudgetMax(double budgetMax) {
        this.budgetMax = budgetMax;
    }
    
    public double getBudgetRemaining() {
        return budgetRemaining;
    }
    
    public void setBudgetRemaining(double budgetRemaining) {
        this.budgetRemaining = budgetRemaining;
    }
    
    public double getUsedPercentage() {
        return usedPercentage;
    }
    
    public void setUsedPercentage(double usedPercentage) {
        this.usedPercentage = usedPercentage;
    }
    
    public Map<String, Double> getCategoryBreakdown() {
        return categoryBreakdown;
    }
    
    public void setCategoryBreakdown(Map<String, Double> categoryBreakdown) {
        this.categoryBreakdown = categoryBreakdown;
    }
    
    public List<String> getWarnings() {
        return warnings;
    }
    
    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }
    
    public List<String> getRecommendations() {
        return recommendations;
    }
    
    public void setRecommendations(List<String> recommendations) {
        this.recommendations = recommendations;
    }
    
    public String getError() {
        return error;
    }
    
    public void setError(String error) {
        this.error = error;
    }
}
