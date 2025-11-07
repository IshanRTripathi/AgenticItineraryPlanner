package com.tripplanner.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Node location information.
 */
public class NodeLocation {
    
    @JsonProperty("name")
    private String name;
    
    @JsonProperty("address")
    private String address;
    
    @JsonProperty("coordinates")
    private Coordinates coordinates;
    
    @JsonProperty("placeId")
    private String placeId;
    
    @JsonProperty("googleMapsUri")
    private String googleMapsUri;
    
    @JsonProperty("rating")
    private Double rating;
    
    @JsonProperty("openingHours")
    private String openingHours; // e.g., "09:00"
    
    @JsonProperty("closingHours")
    private String closingHours; // e.g., "18:00"
    
    @JsonProperty("photos")
    private java.util.List<String> photos; // Photo references from Google Places
    
    @JsonProperty("userRatingsTotal")
    private Integer userRatingsTotal; // Total number of reviews
    
    @JsonProperty("priceLevel")
    private Integer priceLevel; // Price level (0-4)
    
    public NodeLocation() {}
    
    public NodeLocation(String name, String address, Coordinates coordinates) {
        this.name = name;
        this.address = address;
        this.coordinates = coordinates;
    }
    
    public NodeLocation(String name, String address, Coordinates coordinates, String placeId) {
        this.name = name;
        this.address = address;
        this.coordinates = coordinates;
        this.placeId = placeId;
    }
    
    public NodeLocation(String name, String address, Coordinates coordinates, String placeId, 
                       String googleMapsUri, Double rating, String openingHours, String closingHours) {
        this.name = name;
        this.address = address;
        this.coordinates = coordinates;
        this.placeId = placeId;
        this.googleMapsUri = googleMapsUri;
        this.rating = rating;
        this.openingHours = openingHours;
        this.closingHours = closingHours;
    }
    
    // Getters and Setters
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
    
    public Coordinates getCoordinates() {
        return coordinates;
    }
    
    public void setCoordinates(Coordinates coordinates) {
        this.coordinates = coordinates;
    }
    
    public String getPlaceId() {
        return placeId;
    }
    
    public void setPlaceId(String placeId) {
        this.placeId = placeId;
    }
    
    public String getGoogleMapsUri() {
        return googleMapsUri;
    }
    
    public void setGoogleMapsUri(String googleMapsUri) {
        this.googleMapsUri = googleMapsUri;
    }
    
    public Double getRating() {
        return rating;
    }
    
    public void setRating(Double rating) {
        this.rating = rating;
    }
    
    public String getOpeningHours() {
        return openingHours;
    }
    
    public void setOpeningHours(String openingHours) {
        this.openingHours = openingHours;
    }
    
    public String getClosingHours() {
        return closingHours;
    }
    
    public void setClosingHours(String closingHours) {
        this.closingHours = closingHours;
    }
    
    public java.util.List<String> getPhotos() {
        return photos;
    }
    
    public void setPhotos(java.util.List<String> photos) {
        this.photos = photos;
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
    
    @Override
    public String toString() {
        return "NodeLocation{" +
                "name='" + name + '\'' +
                ", address='" + address + '\'' +
                ", coordinates=" + coordinates +
                ", placeId='" + placeId + '\'' +
                ", googleMapsUri='" + googleMapsUri + '\'' +
                ", rating=" + rating +
                ", openingHours='" + openingHours + '\'' +
                ", closingHours='" + closingHours + '\'' +
                ", photos=" + (photos != null ? photos.size() + " photos" : "null") +
                ", userRatingsTotal=" + userRatingsTotal +
                ", priceLevel=" + priceLevel +
                '}';
    }
}
