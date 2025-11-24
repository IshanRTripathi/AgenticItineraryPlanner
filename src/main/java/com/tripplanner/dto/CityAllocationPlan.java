package com.tripplanner.dto;

import java.util.List;

public class CityAllocationPlan {
    private String destinationType; // "city", "state", "country", "region"
    private String primaryDestination; // Original destination from request
    private List<CityAllocation> allocations;
    private List<TravelSegment> travelSegments;
    private String planningRationale; // Why this plan was chosen
    private BudgetEstimate budgetEstimate; // Destination-specific budget estimate based on tier

    public CityAllocationPlan() {
    }

    public String getDestinationType() {
        return destinationType;
    }

    public void setDestinationType(String destinationType) {
        this.destinationType = destinationType;
    }

    public String getPrimaryDestination() {
        return primaryDestination;
    }

    public void setPrimaryDestination(String primaryDestination) {
        this.primaryDestination = primaryDestination;
    }

    public List<CityAllocation> getAllocations() {
        return allocations;
    }

    public void setAllocations(List<CityAllocation> allocations) {
        this.allocations = allocations;
    }

    public List<TravelSegment> getTravelSegments() {
        return travelSegments;
    }

    public void setTravelSegments(List<TravelSegment> travelSegments) {
        this.travelSegments = travelSegments;
    }

    public String getPlanningRationale() {
        return planningRationale;
    }

    public void setPlanningRationale(String planningRationale) {
        this.planningRationale = planningRationale;
    }

    public BudgetEstimate getBudgetEstimate() {
        return budgetEstimate;
    }

    public void setBudgetEstimate(BudgetEstimate budgetEstimate) {
        this.budgetEstimate = budgetEstimate;
    }
}
