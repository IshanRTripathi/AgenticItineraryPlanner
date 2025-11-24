package com.tripplanner.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Meal-specific metadata
 */
public class MealMetadata extends NodeMetadata {
    
    @JsonProperty("cuisineType")
    private String cuisineType;
    
    @JsonProperty("mealType")
    private String mealType; // breakfast, lunch, dinner
    
    @JsonProperty("dietaryOptions")
    private List<String> dietaryOptions;
    
    @JsonProperty("priceRange")
    private String priceRange;
    
    @JsonProperty("requiresReservation")
    private Boolean requiresReservation;

    public MealMetadata() {
        super("meal");
    }

    public String getCuisineType() {
        return cuisineType;
    }

    public void setCuisineType(String cuisineType) {
        this.cuisineType = cuisineType;
    }

    public String getMealType() {
        return mealType;
    }

    public void setMealType(String mealType) {
        this.mealType = mealType;
    }

    public List<String> getDietaryOptions() {
        return dietaryOptions;
    }

    public void setDietaryOptions(List<String> dietaryOptions) {
        this.dietaryOptions = dietaryOptions;
    }

    public String getPriceRange() {
        return priceRange;
    }

    public void setPriceRange(String priceRange) {
        this.priceRange = priceRange;
    }

    public Boolean getRequiresReservation() {
        return requiresReservation;
    }

    public void setRequiresReservation(Boolean requiresReservation) {
        this.requiresReservation = requiresReservation;
    }
}
