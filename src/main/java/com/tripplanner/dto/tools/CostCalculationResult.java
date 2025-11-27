package com.tripplanner.dto.tools;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Result DTO for cost calculation tool.
 * P0-5: Returns detailed cost breakdown with budget analysis.
 */
public class CostCalculationResult {
    
    private boolean success;
    private double totalCostPerPerson;
    private double totalCostForParty;
    private String currency;
    private int partySize;
    private Map<String, Double> breakdown;
    private BudgetAnalysis budgetAnalysis;
    private List<String> warnings;
    private String error;
    
    public CostCalculationResult() {
        this.breakdown = new HashMap<>();
        this.warnings = new ArrayList<>();
    }
    
    public static CostCalculationResult error(String error) {
        CostCalculationResult result = new CostCalculationResult();
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
    
    public double getTotalCostPerPerson() {
        return totalCostPerPerson;
    }
    
    public void setTotalCostPerPerson(double totalCostPerPerson) {
        this.totalCostPerPerson = totalCostPerPerson;
    }
    
    public double getTotalCostForParty() {
        return totalCostForParty;
    }
    
    public void setTotalCostForParty(double totalCostForParty) {
        this.totalCostForParty = totalCostForParty;
    }
    
    public String getCurrency() {
        return currency;
    }
    
    public void setCurrency(String currency) {
        this.currency = currency;
    }
    
    public int getPartySize() {
        return partySize;
    }
    
    public void setPartySize(int partySize) {
        this.partySize = partySize;
    }
    
    public Map<String, Double> getBreakdown() {
        return breakdown;
    }
    
    public void setBreakdown(Map<String, Double> breakdown) {
        this.breakdown = breakdown;
    }
    
    public BudgetAnalysis getBudgetAnalysis() {
        return budgetAnalysis;
    }
    
    public void setBudgetAnalysis(BudgetAnalysis budgetAnalysis) {
        this.budgetAnalysis = budgetAnalysis;
    }
    
    public List<String> getWarnings() {
        return warnings;
    }
    
    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }
    
    public String getError() {
        return error;
    }
    
    public void setError(String error) {
        this.error = error;
    }
    
    /**
     * Budget analysis details.
     */
    public static class BudgetAnalysis {
        private Double budgetMax;
        private Double budgetRemaining;
        private Double budgetUsedPercentage;
        private String budgetStatus; // UNDER, NEAR, OVER
        private Double overage;
        private List<String> recommendations;
        
        public BudgetAnalysis() {
            this.recommendations = new ArrayList<>();
        }
        
        public void addRecommendation(String recommendation) {
            this.recommendations.add(recommendation);
        }
        
        // Getters and setters
        
        public Double getBudgetMax() {
            return budgetMax;
        }
        
        public void setBudgetMax(Double budgetMax) {
            this.budgetMax = budgetMax;
        }
        
        public Double getBudgetRemaining() {
            return budgetRemaining;
        }
        
        public void setBudgetRemaining(Double budgetRemaining) {
            this.budgetRemaining = budgetRemaining;
        }
        
        public Double getBudgetUsedPercentage() {
            return budgetUsedPercentage;
        }
        
        public void setBudgetUsedPercentage(Double budgetUsedPercentage) {
            this.budgetUsedPercentage = budgetUsedPercentage;
        }
        
        public String getBudgetStatus() {
            return budgetStatus;
        }
        
        public void setBudgetStatus(String budgetStatus) {
            this.budgetStatus = budgetStatus;
        }
        
        public Double getOverage() {
            return overage;
        }
        
        public void setOverage(Double overage) {
            this.overage = overage;
        }
        
        public List<String> getRecommendations() {
            return recommendations;
        }
        
        public void setRecommendations(List<String> recommendations) {
            this.recommendations = recommendations;
        }
    }
}
