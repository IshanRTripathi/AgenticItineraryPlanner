package com.tripplanner.dto.tools;

import com.tripplanner.dto.Coordinates;
import java.util.List;

/**
 * Request DTO for restaurant suggestion tool.
 * P1-4: Suggest restaurants based on location, cuisine, and dietary restrictions.
 */
public class RestaurantSuggestionRequest {
    
    private String itineraryId;
    private Coordinates location;
    private String locationName;
    private String cuisineType;
    private Integer priceLevel; // 1-4 ($ to $$$$)
    private String mealType; // breakfast, lunch, dinner
    private List<String> dietaryRestrictions;
    private Integer radius; // meters
    
    public RestaurantSuggestionRequest() {
    }
    
    // Getters and setters
    
    public String getItineraryId() {
        return itineraryId;
    }
    
    public void setItineraryId(String itineraryId) {
        this.itineraryId = itineraryId;
    }
    
    public Coordinates getLocation() {
        return location;
    }
    
    public void setLocation(Coordinates location) {
        this.location = location;
    }
    
    public String getLocationName() {
        return locationName;
    }
    
    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }
    
    public String getCuisineType() {
        return cuisineType;
    }
    
    public void setCuisineType(String cuisineType) {
        this.cuisineType = cuisineType;
    }
    
    public Integer getPriceLevel() {
        return priceLevel;
    }
    
    public void setPriceLevel(Integer priceLevel) {
        this.priceLevel = priceLevel;
    }
    
    public String getMealType() {
        return mealType;
    }
    
    public void setMealType(String mealType) {
        this.mealType = mealType;
    }
    
    public List<String> getDietaryRestrictions() {
        return dietaryRestrictions;
    }
    
    public void setDietaryRestrictions(List<String> dietaryRestrictions) {
        this.dietaryRestrictions = dietaryRestrictions;
    }
    
    public Integer getRadius() {
        return radius;
    }
    
    public void setRadius(Integer radius) {
        this.radius = radius;
    }
}
