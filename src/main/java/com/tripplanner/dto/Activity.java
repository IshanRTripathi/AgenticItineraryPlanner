package com.tripplanner.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.ArrayList;

/**
 * Activity DTO for storing activity information
 */
public class Activity {
    
    @JsonProperty("activityId")
    private String activityId;
    
    @JsonProperty("name")
    private String name;
    
    @JsonProperty("description")
    private String description;
    
    @JsonProperty("type")
    private String type; // "outdoor", "cultural", "adventure", "relaxation", etc.
    
    @JsonProperty("location")
    private String location;
    
    @JsonProperty("coordinates")
    private Coordinates coordinates;
    
    @JsonProperty("duration")
    private Integer duration; // in minutes
    
    @JsonProperty("difficulty")
    private String difficulty; // "easy", "moderate", "hard"
    
    @JsonProperty("rating")
    private Double rating;
    
    @JsonProperty("price")
    private Double price;
    
    @JsonProperty("currency")
    private String currency;
    
    @JsonProperty("ageRestriction")
    private String ageRestriction;
    
    @JsonProperty("groupSize")
    private String groupSize;
    
    @JsonProperty("equipment")
    private List<String> equipment;
    
    @JsonProperty("includes")
    private List<String> includes;
    
    @JsonProperty("excludes")
    private List<String> excludes;
    
    @JsonProperty("bookingRequired")
    private Boolean bookingRequired;
    
    @JsonProperty("bookingUrl")
    private String bookingUrl;
    
    @JsonProperty("provider")
    private String provider;
    
    @JsonProperty("photos")
    private List<Photo> photos;
    
    @JsonProperty("tags")
    private List<String> tags;
    
    public Activity() {
        this.equipment = new ArrayList<>();
        this.includes = new ArrayList<>();
        this.excludes = new ArrayList<>();
        this.photos = new ArrayList<>();
        this.tags = new ArrayList<>();
    }
    
    public Activity(String activityId, String name, String type, Integer duration) {
        this();
        this.activityId = activityId;
        this.name = name;
        this.type = type;
        this.duration = duration;
    }
    
    // Getters and Setters
    public String getActivityId() {
        return activityId;
    }
    
    public void setActivityId(String activityId) {
        this.activityId = activityId;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public String getLocation() {
        return location;
    }
    
    public void setLocation(String location) {
        this.location = location;
    }
    
    public Coordinates getCoordinates() {
        return coordinates;
    }
    
    public void setCoordinates(Coordinates coordinates) {
        this.coordinates = coordinates;
    }
    
    public Integer getDuration() {
        return duration;
    }
    
    public void setDuration(Integer duration) {
        this.duration = duration;
    }
    
    public String getDifficulty() {
        return difficulty;
    }
    
    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }
    
    public Double getRating() {
        return rating;
    }
    
    public void setRating(Double rating) {
        this.rating = rating;
    }
    
    public Double getPrice() {
        return price;
    }
    
    public void setPrice(Double price) {
        this.price = price;
    }
    
    public String getCurrency() {
        return currency;
    }
    
    public void setCurrency(String currency) {
        this.currency = currency;
    }
    
    public String getAgeRestriction() {
        return ageRestriction;
    }
    
    public void setAgeRestriction(String ageRestriction) {
        this.ageRestriction = ageRestriction;
    }
    
    public String getGroupSize() {
        return groupSize;
    }
    
    public void setGroupSize(String groupSize) {
        this.groupSize = groupSize;
    }
    
    public List<String> getEquipment() {
        return equipment;
    }
    
    public void setEquipment(List<String> equipment) {
        this.equipment = equipment;
    }
    
    public List<String> getIncludes() {
        return includes;
    }
    
    public void setIncludes(List<String> includes) {
        this.includes = includes;
    }
    
    public List<String> getExcludes() {
        return excludes;
    }
    
    public void setExcludes(List<String> excludes) {
        this.excludes = excludes;
    }
    
    public Boolean getBookingRequired() {
        return bookingRequired;
    }
    
    public void setBookingRequired(Boolean bookingRequired) {
        this.bookingRequired = bookingRequired;
    }
    
    public String getBookingUrl() {
        return bookingUrl;
    }
    
    public void setBookingUrl(String bookingUrl) {
        this.bookingUrl = bookingUrl;
    }
    
    public String getProvider() {
        return provider;
    }
    
    public void setProvider(String provider) {
        this.provider = provider;
    }
    
    public List<Photo> getPhotos() {
        return photos;
    }
    
    public void setPhotos(List<Photo> photos) {
        this.photos = photos;
    }
    
    public List<String> getTags() {
        return tags;
    }
    
    public void setTags(List<String> tags) {
        this.tags = tags;
    }
    
    @Override
    public String toString() {
        return "Activity{" +
                "activityId='" + activityId + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", type='" + type + '\'' +
                ", location='" + location + '\'' +
                ", coordinates=" + coordinates +
                ", duration=" + duration +
                ", difficulty='" + difficulty + '\'' +
                ", rating=" + rating +
                ", price=" + price +
                ", currency='" + currency + '\'' +
                ", ageRestriction='" + ageRestriction + '\'' +
                ", groupSize='" + groupSize + '\'' +
                ", equipment=" + equipment +
                ", includes=" + includes +
                ", excludes=" + excludes +
                ", bookingRequired=" + bookingRequired +
                ", bookingUrl='" + bookingUrl + '\'' +
                ", provider='" + provider + '\'' +
                ", photos=" + photos +
                ", tags=" + tags +
                '}';
    }
}