package com.tripplanner.dto;

import lombok.Data;

/**
 * Budget estimate for a destination based on budget tier.
 * Provides realistic min/max budget ranges in local currency.
 */
@Data
public class BudgetEstimate {
    private String destination;
    private String currency;
    private String budgetTier; // "budget", "medium", "luxury"
    private Double minPerPersonPerDay;
    private Double maxPerPersonPerDay;
    private String rationale; // Explanation of the budget range
    
    /**
     * Get total budget for the trip.
     * @param days Number of days
     * @param persons Number of persons
     * @return Total budget range as formatted string
     */
    public String getTotalBudgetRange(int days, int persons) {
        if (minPerPersonPerDay == null || maxPerPersonPerDay == null) {
            return "Not estimated";
        }
        
        double totalMin = minPerPersonPerDay * days * persons;
        double totalMax = maxPerPersonPerDay * days * persons;
        
        return String.format("%s %.0f - %.0f", currency, totalMin, totalMax);
    }
}
