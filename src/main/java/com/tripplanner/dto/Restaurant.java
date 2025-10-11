package com.tripplanner.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.ArrayList;

/**
 * Restaurant DTO for storing restaurant information
 */
public class Restaurant {
    
    @JsonProperty("restaurantId")
    private String restaurantId;
    
    @JsonProperty("name")
    private String name;
    
    @JsonProperty("address")
    private String address;
    
    @JsonProperty("coordinates")
    private Coordinates coordinates;
    
    @JsonProperty("cuisineType")
    private String cuisineType;
    
    @JsonProperty("rating")
    private Double rating;
    
    @JsonProperty("priceLevel")
    private Integer priceLevel; // 1-4 scale
    
    @JsonProperty("phoneNumber")
    private String phoneNumber;
    
    @JsonProperty("website")
    private String website;
    
    @JsonProperty("openingHours")
    private List<String> openingHours;
    
    @JsonProperty("specialties")
    private List<String> specialties;
    
    @JsonProperty("dietaryOptions")
    private List<String> dietaryOptions;
    
    @JsonProperty("reservationRequired")
    private Boolean reservationRequired;
    
    @JsonProperty("reservationUrl")
    private String reservationUrl;
    
    @JsonProperty("photos")
    private List<Photo> photos;
    
    public Restaurant() {
        this.openingHours = new ArrayList<>();
        this.specialties = new ArrayList<>();
        this.dietaryOptions = new ArrayList<>();
        this.photos = new ArrayList<>();
    }
    
    public Restaurant(String restaurantId, String name, String cuisineType, Double rating) {
        this();
        this.restaurantId = restaurantId;
        this.name = name;
        this.cuisineType = cuisineType;
        this.rating = rating;
    }
    
    // Getters and Setters
    public String getRestaurantId() {
        return restaurantId;
    }
    
    public void setRestaurantId(String restaurantId) {
        this.restaurantId = restaurantId;
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
    
    public Coordinates getCoordinates() {
        return coordinates;
    }
    
    public void setCoordinates(Coordinates coordinates) {
        this.coordinates = coordinates;
    }
    
    public String getCuisineType() {
        return cuisineType;
    }
    
    public void setCuisineType(String cuisineType) {
        this.cuisineType = cuisineType;
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
    
    public String getPhoneNumber() {
        return phoneNumber;
    }
    
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
    
    public String getWebsite() {
        return website;
    }
    
    public void setWebsite(String website) {
        this.website = website;
    }
    
    public List<String> getOpeningHours() {
        return openingHours;
    }
    
    public void setOpeningHours(List<String> openingHours) {
        this.openingHours = openingHours;
    }
    
    public List<String> getSpecialties() {
        return specialties;
    }
    
    public void setSpecialties(List<String> specialties) {
        this.specialties = specialties;
    }
    
    public List<String> getDietaryOptions() {
        return dietaryOptions;
    }
    
    public void setDietaryOptions(List<String> dietaryOptions) {
        this.dietaryOptions = dietaryOptions;
    }
    
    public Boolean getReservationRequired() {
        return reservationRequired;
    }
    
    public void setReservationRequired(Boolean reservationRequired) {
        this.reservationRequired = reservationRequired;
    }
    
    public String getReservationUrl() {
        return reservationUrl;
    }
    
    public void setReservationUrl(String reservationUrl) {
        this.reservationUrl = reservationUrl;
    }
    
    public List<Photo> getPhotos() {
        return photos;
    }
    
    public void setPhotos(List<Photo> photos) {
        this.photos = photos;
    }
    
    @Override
    public String toString() {
        return "Restaurant{" +
                "restaurantId='" + restaurantId + '\'' +
                ", name='" + name + '\'' +
                ", address='" + address + '\'' +
                ", coordinates=" + coordinates +
                ", cuisineType='" + cuisineType + '\'' +
                ", rating=" + rating +
                ", priceLevel=" + priceLevel +
                ", phoneNumber='" + phoneNumber + '\'' +
                ", website='" + website + '\'' +
                ", openingHours=" + openingHours +
                ", specialties=" + specialties +
                ", dietaryOptions=" + dietaryOptions +
                ", reservationRequired=" + reservationRequired +
                ", reservationUrl='" + reservationUrl + '\'' +
                ", photos=" + photos +
                '}';
    }
}