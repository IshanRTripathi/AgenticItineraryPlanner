package com.tripplanner.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for Google Places Text Search API result.
 */
public class PlaceSearchResult {
    
    @JsonProperty("place_id")
    private String placeId;
    
    private String name;
    
    @JsonProperty("formatted_address")
    private String formattedAddress;
    
    private Geometry geometry;
    
    private Double rating;
    
    @JsonProperty("user_ratings_total")
    private Integer userRatingsTotal;
    
    private String[] types;
    
    @JsonProperty("price_level")
    private Integer priceLevel;
    
    @JsonProperty("business_status")
    private String businessStatus;
    
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
    
    public String getFormattedAddress() {
        return formattedAddress;
    }
    
    public void setFormattedAddress(String formattedAddress) {
        this.formattedAddress = formattedAddress;
    }
    
    public Geometry getGeometry() {
        return geometry;
    }
    
    public void setGeometry(Geometry geometry) {
        this.geometry = geometry;
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
    
    public String[] getTypes() {
        return types;
    }
    
    public void setTypes(String[] types) {
        this.types = types;
    }
    
    public Integer getPriceLevel() {
        return priceLevel;
    }
    
    public void setPriceLevel(Integer priceLevel) {
        this.priceLevel = priceLevel;
    }
    
    public String getBusinessStatus() {
        return businessStatus;
    }
    
    public void setBusinessStatus(String businessStatus) {
        this.businessStatus = businessStatus;
    }
}
