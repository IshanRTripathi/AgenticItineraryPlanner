package com.tripplanner.service;

import com.tripplanner.dto.NormalizedDay;
import com.tripplanner.dto.NormalizedItinerary;
import com.tripplanner.dto.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * BudgetTracker - Tracks and validates budget across itinerary.
 * 
 * CRITICAL: Ensures users don't exceed their budget.
 * 
 * Features:
 * 1. Running total calculation (per person)
 * 2. Budget warnings when approaching limit
 * 3. Budget exceeded alerts
 * 4. Per-day budget breakdown
 * 5. Category-wise budget analysis
 */
@Service
public class BudgetTracker {
    
    private static final Logger logger = LoggerFactory.getLogger(BudgetTracker.class);
    
    // Warning thresholds
    private static final double WARNING_THRESHOLD = 0.8; // 80% of budget
    private static final double CRITICAL_THRESHOLD = 0.95; // 95% of budget
    
    /**
     * Budget summary for an itinerary.
     */
    public static class BudgetSummary {
        private double totalCostPerPerson;
        private double budgetMin;
        private double budgetMax;
        private String currency;
        private int partySize;
        private double totalCostForParty;
        private List<String> warnings;
        private List<DayBudget> dayBreakdown;
        private CategoryBudget categoryBreakdown;
        
        public BudgetSummary() {
            this.warnings = new ArrayList<>();
            this.dayBreakdown = new ArrayList<>();
        }
        
        // Getters and setters
        public double getTotalCostPerPerson() { return totalCostPerPerson; }
        public void setTotalCostPerPerson(double totalCostPerPerson) { this.totalCostPerPerson = totalCostPerPerson; }
        
        public double getBudgetMin() { return budgetMin; }
        public void setBudgetMin(double budgetMin) { this.budgetMin = budgetMin; }
        
        public double getBudgetMax() { return budgetMax; }
        public void setBudgetMax(double budgetMax) { this.budgetMax = budgetMax; }
        
        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }
        
        public int getPartySize() { return partySize; }
        public void setPartySize(int partySize) { this.partySize = partySize; }
        
        public double getTotalCostForParty() { return totalCostForParty; }
        public void setTotalCostForParty(double totalCostForParty) { this.totalCostForParty = totalCostForParty; }
        
        public List<String> getWarnings() { return warnings; }
        public void setWarnings(List<String> warnings) { this.warnings = warnings; }
        public void addWarning(String warning) { this.warnings.add(warning); }
        
        public List<DayBudget> getDayBreakdown() { return dayBreakdown; }
        public void setDayBreakdown(List<DayBudget> dayBreakdown) { this.dayBreakdown = dayBreakdown; }
        
        public CategoryBudget getCategoryBreakdown() { return categoryBreakdown; }
        public void setCategoryBreakdown(CategoryBudget categoryBreakdown) { this.categoryBreakdown = categoryBreakdown; }
        
        public boolean isOverBudget() {
            return budgetMax > 0 && totalCostPerPerson > budgetMax;
        }
        
        public boolean isNearBudget() {
            return budgetMax > 0 && totalCostPerPerson > (budgetMax * WARNING_THRESHOLD);
        }
        
        public double getPercentageOfBudget() {
            if (budgetMax <= 0) return 0;
            return (totalCostPerPerson / budgetMax) * 100;
        }
    }
    
    /**
     * Per-day budget breakdown.
     */
    public static class DayBudget {
        private int dayNumber;
        private double costPerPerson;
        private int nodeCount;
        
        public DayBudget(int dayNumber, double costPerPerson, int nodeCount) {
            this.dayNumber = dayNumber;
            this.costPerPerson = costPerPerson;
            this.nodeCount = nodeCount;
        }
        
        public int getDayNumber() { return dayNumber; }
        public double getCostPerPerson() { return costPerPerson; }
        public int getNodeCount() { return nodeCount; }
    }
    
    /**
     * Category-wise budget breakdown.
     */
    public static class CategoryBudget {
        private double attractions;
        private double meals;
        private double transport;
        private double accommodation;
        private double other;
        
        public double getAttractions() { return attractions; }
        public void setAttractions(double attractions) { this.attractions = attractions; }
        
        public double getMeals() { return meals; }
        public void setMeals(double meals) { this.meals = meals; }
        
        public double getTransport() { return transport; }
        public void setTransport(double transport) { this.transport = transport; }
        
        public double getAccommodation() { return accommodation; }
        public void setAccommodation(double accommodation) { this.accommodation = accommodation; }
        
        public double getOther() { return other; }
        public void setOther(double other) { this.other = other; }
        
        public double getTotal() {
            return attractions + meals + transport + accommodation + other;
        }
    }
    
    /**
     * Calculate budget summary for an itinerary.
     */
    public BudgetSummary calculateBudget(NormalizedItinerary itinerary, Double budgetMin, 
                                         Double budgetMax, Integer partySize) {
        BudgetSummary summary = new BudgetSummary();
        
        // Set budget parameters
        summary.setBudgetMin(budgetMin != null ? budgetMin : 0);
        summary.setBudgetMax(budgetMax != null ? budgetMax : 0);
        summary.setCurrency(itinerary.getCurrency() != null ? itinerary.getCurrency() : "USD");
        summary.setPartySize(partySize != null ? partySize : 1);
        
        // Calculate totals
        double totalCost = 0;
        CategoryBudget categoryBudget = new CategoryBudget();
        
        if (itinerary.getDays() != null) {
            for (NormalizedDay day : itinerary.getDays()) {
                double dayCost = 0;
                int nodeCount = 0;
                
                if (day.getNodes() != null) {
                    for (NormalizedNode node : day.getNodes()) {
                        if (node.getCost() != null && node.getCost().getAmountPerPerson() != null) {
                            double cost = node.getCost().getAmountPerPerson();
                            dayCost += cost;
                            totalCost += cost;
                            nodeCount++;
                            
                            // Category breakdown
                            String type = node.getType() != null ? node.getType().toLowerCase() : "other";
                            switch (type) {
                                case "attraction", "activity" -> categoryBudget.setAttractions(
                                    categoryBudget.getAttractions() + cost);
                                case "meal", "restaurant" -> categoryBudget.setMeals(
                                    categoryBudget.getMeals() + cost);
                                case "transport", "transportation" -> categoryBudget.setTransport(
                                    categoryBudget.getTransport() + cost);
                                case "accommodation", "hotel" -> categoryBudget.setAccommodation(
                                    categoryBudget.getAccommodation() + cost);
                                default -> categoryBudget.setOther(categoryBudget.getOther() + cost);
                            }
                        }
                    }
                }
                
                summary.getDayBreakdown().add(new DayBudget(day.getDayNumber(), dayCost, nodeCount));
            }
        }
        
        summary.setTotalCostPerPerson(totalCost);
        summary.setTotalCostForParty(totalCost * summary.getPartySize());
        summary.setCategoryBreakdown(categoryBudget);
        
        // Generate warnings
        generateWarnings(summary);
        
        logger.info("Budget calculated: {} {} per person ({} for party of {})", 
                   totalCost, summary.getCurrency(), summary.getTotalCostForParty(), summary.getPartySize());
        
        return summary;
    }
    
    /**
     * Generate budget warnings based on thresholds.
     */
    private void generateWarnings(BudgetSummary summary) {
        if (summary.getBudgetMax() <= 0) {
            // No budget set, no warnings
            return;
        }
        
        double percentage = summary.getPercentageOfBudget();
        
        if (summary.isOverBudget()) {
            double excess = summary.getTotalCostPerPerson() - summary.getBudgetMax();
            summary.addWarning(String.format(
                "⚠️ BUDGET EXCEEDED: Current cost %.0f %s is %.0f %s over budget (%.0f%% of budget)",
                summary.getTotalCostPerPerson(), summary.getCurrency(),
                excess, summary.getCurrency(), percentage
            ));
        } else if (percentage >= CRITICAL_THRESHOLD * 100) {
            double remaining = summary.getBudgetMax() - summary.getTotalCostPerPerson();
            summary.addWarning(String.format(
                "⚠️ CRITICAL: Only %.0f %s remaining (%.0f%% of budget used)",
                remaining, summary.getCurrency(), percentage
            ));
        } else if (percentage >= WARNING_THRESHOLD * 100) {
            double remaining = summary.getBudgetMax() - summary.getTotalCostPerPerson();
            summary.addWarning(String.format(
                "⚠️ WARNING: %.0f %s remaining (%.0f%% of budget used)",
                remaining, summary.getCurrency(), percentage
            ));
        }
        
        // Check if any single day exceeds reasonable daily budget
        if (summary.getBudgetMax() > 0 && !summary.getDayBreakdown().isEmpty()) {
            double avgDailyBudget = summary.getBudgetMax() / summary.getDayBreakdown().size();
            for (DayBudget dayBudget : summary.getDayBreakdown()) {
                if (dayBudget.getCostPerPerson() > avgDailyBudget * 1.5) {
                    summary.addWarning(String.format(
                        "Day %d cost (%.0f %s) is significantly higher than average daily budget (%.0f %s)",
                        dayBudget.getDayNumber(), dayBudget.getCostPerPerson(), 
                        summary.getCurrency(), avgDailyBudget, summary.getCurrency()
                    ));
                }
            }
        }
    }
    
    /**
     * Validate that adding a new node won't exceed budget.
     */
    public boolean canAffordNode(NormalizedItinerary itinerary, double nodeCost, 
                                  Double budgetMax) {
        if (budgetMax == null || budgetMax <= 0) {
            return true; // No budget limit
        }
        
        BudgetSummary current = calculateBudget(itinerary, null, budgetMax, null);
        double newTotal = current.getTotalCostPerPerson() + nodeCost;
        
        return newTotal <= budgetMax;
    }
    
    /**
     * Get budget recommendations for cost reduction.
     */
    public List<String> getRecommendations(BudgetSummary summary) {
        List<String> recommendations = new ArrayList<>();
        
        if (!summary.isOverBudget()) {
            return recommendations; // No recommendations needed
        }
        
        CategoryBudget cat = summary.getCategoryBreakdown();
        double total = cat.getTotal();
        
        // Identify highest cost categories
        if (cat.getAccommodation() / total > 0.4) {
            recommendations.add("Consider more budget-friendly accommodation options");
        }
        if (cat.getMeals() / total > 0.3) {
            recommendations.add("Mix high-end dining with local street food or casual restaurants");
        }
        if (cat.getAttractions() / total > 0.3) {
            recommendations.add("Include more free or low-cost attractions");
        }
        if (cat.getTransport() / total > 0.2) {
            recommendations.add("Use public transport instead of taxis where possible");
        }
        
        return recommendations;
    }
}
