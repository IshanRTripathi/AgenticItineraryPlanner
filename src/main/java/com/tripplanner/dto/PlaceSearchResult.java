package com.tripplanner.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * DTO for Google Places Text Search API result.
 * Matches the exact structure from Google Places API response.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PlaceSearchResult {
    
    @JsonProperty("place_id")
    private String placeId;
    
    @JsonProperty("name")
    private String name;
    
    @JsonProperty("formatted_address")
    private String formattedAddress;
    
    @JsonProperty("geometry")
    private Geometry geometry;
    
    @JsonProperty("icon")
    private String icon;
    
    @JsonProperty("icon_background_color")
    private String iconBackgroundColor;
    
    @JsonProperty("icon_mask_base_uri")
    private String iconMaskBaseUri;
    
    @JsonProperty("photos")
    private List<Photo> photos;
    
    @JsonProperty("reference")
    private String reference;
    
    @JsonProperty("types")
    private List<String> types;
    
    @JsonProperty("rating")
    private Double rating;
    
    @JsonProperty("user_ratings_total")
    private Integer userRatingsTotal;
    
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
    
    public String getIcon() {
        return icon;
    }
    
    public void setIcon(String icon) {
        this.icon = icon;
    }
    
    public String getIconBackgroundColor() {
        return iconBackgroundColor;
    }
    
    public void setIconBackgroundColor(String iconBackgroundColor) {
        this.iconBackgroundColor = iconBackgroundColor;
    }
    
    public String getIconMaskBaseUri() {
        return iconMaskBaseUri;
    }
    
    public void setIconMaskBaseUri(String iconMaskBaseUri) {
        this.iconMaskBaseUri = iconMaskBaseUri;
    }
    
    public List<Photo> getPhotos() {
        return photos;
    }
    
    public void setPhotos(List<Photo> photos) {
        this.photos = photos;
    }
    
    public String getReference() {
        return reference;
    }
    
    public void setReference(String reference) {
        this.reference = reference;
    }
    
    public List<String> getTypes() {
        return types;
    }
    
    public void setTypes(List<String> types) {
        this.types = types;
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
    
    public String getBusinessStatus() {
        return businessStatus;
    }
    
    public void setBusinessStatus(String businessStatus) {
        this.businessStatus = businessStatus;
    }
    
    @Override
    public String toString() {
        return "PlaceSearchResult{" +
                "placeId='" + placeId + '\'' +
                ", name='" + name + '\'' +
                ", formattedAddress='" + formattedAddress + '\'' +
                ", geometry=" + geometry +
                '}';
    }
}
