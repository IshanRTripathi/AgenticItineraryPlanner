package com.tripplanner.dto.tools;

import java.util.ArrayList;
import java.util.List;

/**
 * Result DTO for restaurant suggestion tool.
 * P1-4: Returns list of restaurants matching criteria.
 */
public class RestaurantSuggestionResult {
    
    private boolean success;
    private List<RestaurantSuggestion> restaurants;
    private String error;
    private List<String> warnings;
    private int totalResults;
    
    public RestaurantSuggestionResult() {
        this.restaurants = new ArrayList<>();
        this.warnings = new ArrayList<>();
    }
    
    public static RestaurantSuggestionResult error(String error) {
        RestaurantSuggestionResult result = new RestaurantSuggestionResult();
        result.success = false;
        result.error = error;
        return result;
    }
    
    public void addWarning(String warning) {
        this.warnings.add(warning);
    }
    
    public void addRestaurant(RestaurantSuggestion restaurant) {
        this.restaurants.add(restaurant);
    }
    
    // Getters and setters
    
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public List<RestaurantSuggestion> getRestaurants() {
        return restaurants;
    }
    
    public void setRestaurants(List<RestaurantSuggestion> restaurants) {
        this.restaurants = restaurants;
    }
    
    public String getError() {
        return error;
    }
    
    public void setError(String error) {
        this.error = error;
    }
    
    public List<String> getWarnings() {
        return warnings;
    }
    
    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }
    
    public int getTotalResults() {
        return totalResults;
    }
    
    public void setTotalResults(int totalResults) {
        this.totalResults = totalResults;
    }
    
    /**
     * Individual restaurant suggestion.
     */
    public static class RestaurantSuggestion {
        private String placeId;
        private String name;
        private String address;
        private Double rating;
        private Integer priceLevel;
        private Integer userRatingsTotal;
        private List<String> cuisineTypes;
        private Boolean openNow;
        private String photoReference;
        private Double distanceKm;
        private Boolean matchesDietaryRestrictions;
        
        public RestaurantSuggestion() {
            this.cuisineTypes = new ArrayList<>();
        }
        
        // Getters and setters
        
        public String getPlaceId() {
            return placeId;
        }
        
        public void setPlaceId(String placeId) {
            this.placeId = placeId;
        }
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public String getAddress() {
            return address;
        }
        
        public void setAddress(String address) {
            this.address = address;
        }
        
        public Double getRating() {
            return rating;
        }
        
        public void setRating(Double rating) {
            this.rating = rating;
        }
        
        public Integer getPriceLevel() {
            return priceLevel;
        }
        
        public void setPriceLevel(Integer priceLevel) {
            this.priceLevel = priceLevel;
        }
        
        public Integer getUserRatingsTotal() {
            return userRatingsTotal;
        }
        
        public void setUserRatingsTotal(Integer userRatingsTotal) {
            this.userRatingsTotal = userRatingsTotal;
        }
        
        public List<String> getCuisineTypes() {
            return cuisineTypes;
        }
        
        public void setCuisineTypes(List<String> cuisineTypes) {
            this.cuisineTypes = cuisineTypes;
        }
        
        public Boolean getOpenNow() {
            return openNow;
        }
        
        public void setOpenNow(Boolean openNow) {
            this.openNow = openNow;
        }
        
        public String getPhotoReference() {
            return photoReference;
        }
        
        public void setPhotoReference(String photoReference) {
            this.photoReference = photoReference;
        }
        
        public Double getDistanceKm() {
            return distanceKm;
        }
        
        public void setDistanceKm(Double distanceKm) {
            this.distanceKm = distanceKm;
        }
        
        public Boolean getMatchesDietaryRestrictions() {
            return matchesDietaryRestrictions;
        }
        
        public void setMatchesDietaryRestrictions(Boolean matchesDietaryRestrictions) {
            this.matchesDietaryRestrictions = matchesDietaryRestrictions;
        }
    }
}
