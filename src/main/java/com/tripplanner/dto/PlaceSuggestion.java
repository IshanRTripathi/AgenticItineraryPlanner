package com.tripplanner.dto;

import java.util.List;

/**
 * Place suggestion with photos, ratings, and details.
 * Used for chat-based place search with rich data display.
 * 
 * This DTO is returned by the place search tool and displayed
 * in the chat UI as suggestion cards with photos and ratings.
 */
public class PlaceSuggestion {
    private String placeId;
    private String name;
    private String address;
    private Double rating;
    private Integer userRatingsTotal;
    private Integer priceLevel; // 0-4 (0=Free, 1=$, 2=$$, 3=$$$, 4=$$$$)
    private List<Photo> photos;
    private List<String> types;
    private Geometry geometry;
    private String openingHours; // e.g., "9am - 6pm (Closed Tuesdays)"
    private Double estimatedCost; // Estimated cost per person in local currency
    private Integer estimatedDuration; // Estimated duration in minutes
    private Double distanceKm; // Distance from destination in kilometers
    private String website; // Place website URL
    private String googleMapsUrl; // Google Maps link for the place
    private Integer day; // Day number this suggestion is for (1-based)
    
    // Constructors
    public PlaceSuggestion() {}
    
    // Builder pattern for easy construction
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private PlaceSuggestion suggestion = new PlaceSuggestion();
        
        public Builder placeId(String placeId) {
            suggestion.placeId = placeId;
            return this;
        }
        
        public Builder name(String name) {
            suggestion.name = name;
            return this;
        }
        
        public Builder address(String address) {
            suggestion.address = address;
            return this;
        }
        
        public Builder rating(Double rating) {
            suggestion.rating = rating;
            return this;
        }
        
        public Builder userRatingsTotal(Integer userRatingsTotal) {
            suggestion.userRatingsTotal = userRatingsTotal;
            return this;
        }
        
        public Builder priceLevel(Integer priceLevel) {
            suggestion.priceLevel = priceLevel;
            return this;
        }
        
        public Builder photos(List<Photo> photos) {
            suggestion.photos = photos;
            return this;
        }
        
        public Builder types(List<String> types) {
            suggestion.types = types;
            return this;
        }
        
        public Builder geometry(Geometry geometry) {
            suggestion.geometry = geometry;
            return this;
        }
        
        public Builder openingHours(String openingHours) {
            suggestion.openingHours = openingHours;
            return this;
        }
        
        public Builder estimatedCost(Double estimatedCost) {
            suggestion.estimatedCost = estimatedCost;
            return this;
        }
        
        public Builder estimatedDuration(Integer estimatedDuration) {
            suggestion.estimatedDuration = estimatedDuration;
            return this;
        }
        
        public Builder distanceKm(Double distanceKm) {
            suggestion.distanceKm = distanceKm;
            return this;
        }
        
        public Builder website(String website) {
            suggestion.website = website;
            return this;
        }
        
        public Builder googleMapsUrl(String googleMapsUrl) {
            suggestion.googleMapsUrl = googleMapsUrl;
            return this;
        }
        
        public Builder day(Integer day) {
            suggestion.day = day;
            return this;
        }
        
        public PlaceSuggestion build() {
            return suggestion;
        }
    }
    
    // Getters and Setters
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
    
    public Integer getUserRatingsTotal() {
        return userRatingsTotal;
    }
    
    public void setUserRatingsTotal(Integer userRatingsTotal) {
        this.userRatingsTotal = userRatingsTotal;
    }
    
    public Integer getPriceLevel() {
        return priceLevel;
    }
    
    public void setPriceLevel(Integer priceLevel) {
        this.priceLevel = priceLevel;
    }
    
    public List<Photo> getPhotos() {
        return photos;
    }
    
    public void setPhotos(List<Photo> photos) {
        this.photos = photos;
    }
    
    public List<String> getTypes() {
        return types;
    }
    
    public void setTypes(List<String> types) {
        this.types = types;
    }
    
    public Geometry getGeometry() {
        return geometry;
    }
    
    public void setGeometry(Geometry geometry) {
        this.geometry = geometry;
    }
    
    public String getOpeningHours() {
        return openingHours;
    }
    
    public void setOpeningHours(String openingHours) {
        this.openingHours = openingHours;
    }
    
    public Double getEstimatedCost() {
        return estimatedCost;
    }
    
    public void setEstimatedCost(Double estimatedCost) {
        this.estimatedCost = estimatedCost;
    }
    
    public Integer getEstimatedDuration() {
        return estimatedDuration;
    }
    
    public void setEstimatedDuration(Integer estimatedDuration) {
        this.estimatedDuration = estimatedDuration;
    }
    
    public Double getDistanceKm() {
        return distanceKm;
    }
    
    public void setDistanceKm(Double distanceKm) {
        this.distanceKm = distanceKm;
    }
    
    public String getWebsite() {
        return website;
    }
    
    public void setWebsite(String website) {
        this.website = website;
    }
    
    public String getGoogleMapsUrl() {
        return googleMapsUrl;
    }
    
    public void setGoogleMapsUrl(String googleMapsUrl) {
        this.googleMapsUrl = googleMapsUrl;
    }
    
    public Integer getDay() {
        return day;
    }
    
    public void setDay(Integer day) {
        this.day = day;
    }
    
    @Override
    public String toString() {
        return "PlaceSuggestion{" +
                "placeId='" + placeId + '\'' +
                ", name='" + name + '\'' +
                ", rating=" + rating +
                ", userRatingsTotal=" + userRatingsTotal +
                ", priceLevel=" + priceLevel +
                ", address='" + address + '\'' +
                ", estimatedCost=" + estimatedCost +
                ", estimatedDuration=" + estimatedDuration +
                ", distanceKm=" + distanceKm +
                ", website='" + website + '\'' +
                '}';
    }
}
