package com.tripplanner.dto;

import java.util.HashMap;
import java.util.Map;

/**
 * DTO representing the cost impact of proposed changes.
 * Shows current cost vs new cost before changes are applied.
 */
public class CostImpact {
    
    private double currentCost;
    private double newCost;
    private double difference;
    private String currency;
    private boolean exceedsBudget;
    private Double budgetLimit;
    private Map<String, Double> breakdown; // category -> cost change
    
    public CostImpact() {
        this.breakdown = new HashMap<>();
    }
    
    public CostImpact(double currentCost, double newCost, double difference, String currency) {
        this.currentCost = currentCost;
        this.newCost = newCost;
        this.difference = difference;
        this.currency = currency;
        this.breakdown = new HashMap<>();
    }
    
    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private final CostImpact costImpact = new CostImpact();
        
        public Builder currentCost(double currentCost) {
            costImpact.currentCost = currentCost;
            return this;
        }
        
        public Builder newCost(double newCost) {
            costImpact.newCost = newCost;
            return this;
        }
        
        public Builder difference(double difference) {
            costImpact.difference = difference;
            return this;
        }
        
        public Builder currency(String currency) {
            costImpact.currency = currency;
            return this;
        }
        
        public Builder exceedsBudget(boolean exceedsBudget) {
            costImpact.exceedsBudget = exceedsBudget;
            return this;
        }
        
        public Builder budgetLimit(Double budgetLimit) {
            costImpact.budgetLimit = budgetLimit;
            return this;
        }
        
        public Builder breakdown(Map<String, Double> breakdown) {
            costImpact.breakdown = breakdown;
            return this;
        }
        
        public Builder addBreakdownItem(String category, double change) {
            costImpact.breakdown.put(category, change);
            return this;
        }
        
        public CostImpact build() {
            return costImpact;
        }
    }
    
    // Getters and Setters
    public double getCurrentCost() {
        return currentCost;
    }
    
    public void setCurrentCost(double currentCost) {
        this.currentCost = currentCost;
    }
    
    public double getNewCost() {
        return newCost;
    }
    
    public void setNewCost(double newCost) {
        this.newCost = newCost;
    }
    
    public double getDifference() {
        return difference;
    }
    
    public void setDifference(double difference) {
        this.difference = difference;
    }
    
    public String getCurrency() {
        return currency;
    }
    
    public void setCurrency(String currency) {
        this.currency = currency;
    }
    
    public boolean isExceedsBudget() {
        return exceedsBudget;
    }
    
    public void setExceedsBudget(boolean exceedsBudget) {
        this.exceedsBudget = exceedsBudget;
    }
    
    public Double getBudgetLimit() {
        return budgetLimit;
    }
    
    public void setBudgetLimit(Double budgetLimit) {
        this.budgetLimit = budgetLimit;
    }
    
    public Map<String, Double> getBreakdown() {
        return breakdown;
    }
    
    public void setBreakdown(Map<String, Double> breakdown) {
        this.breakdown = breakdown;
    }
    
    @Override
    public String toString() {
        return "CostImpact{" +
                "currentCost=" + currentCost +
                ", newCost=" + newCost +
                ", difference=" + difference +
                ", currency='" + currency + '\'' +
                ", exceedsBudget=" + exceedsBudget +
                ", budgetLimit=" + budgetLimit +
                ", breakdown=" + breakdown +
                '}';
    }
}
