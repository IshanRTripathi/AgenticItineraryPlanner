package com.tripplanner.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * Node details information.
 */
public class NodeDetails {
    
    @JsonProperty("rating")
    private Double rating;
    
    @JsonProperty("category")
    private String category;
    
    @JsonProperty("description")
    private String description;
    
    @JsonProperty("tags")
    private List<String> tags;
    
    @JsonProperty("openingHours")
    private Map<String, TimeSlot> openingHours;
    
    @JsonProperty("googleMapsUri")
    private String googleMapsUri;
    
    @JsonProperty("photos")
    private List<Photo> photos;
    
    @JsonProperty("reviews")
    private List<Review> reviews;
    
    public NodeDetails() {}
    
    public NodeDetails(Double rating, String category, List<String> tags, Map<String, TimeSlot> openingHours) {
        this.rating = rating;
        this.category = category;
        this.tags = tags;
        this.openingHours = openingHours;
    }
    
    // Getters and Setters
    public Double getRating() {
        return rating;
    }
    
    public void setRating(Double rating) {
        this.rating = rating;
    }
    
    public String getCategory() {
        return category;
    }
    
    public void setCategory(String category) {
        this.category = category;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public List<String> getTags() {
        return tags;
    }
    
    public void setTags(List<String> tags) {
        this.tags = tags;
    }
    
    public Map<String, TimeSlot> getOpeningHours() {
        return openingHours;
    }
    
    public void setOpeningHours(Map<String, TimeSlot> openingHours) {
        this.openingHours = openingHours;
    }
    
    public String getGoogleMapsUri() {
        return googleMapsUri;
    }
    
    public void setGoogleMapsUri(String googleMapsUri) {
        this.googleMapsUri = googleMapsUri;
    }
    
    public List<Photo> getPhotos() {
        return photos;
    }
    
    public void setPhotos(List<Photo> photos) {
        this.photos = photos;
    }
    
    public List<Review> getReviews() {
        return reviews;
    }
    
    public void setReviews(List<Review> reviews) {
        this.reviews = reviews;
    }
    
    @Override
    public String toString() {
        return "NodeDetails{" +
                "rating=" + rating +
                ", category='" + category + '\'' +
                ", tags=" + tags +
                ", openingHours=" + openingHours +
                ", googleMapsUri='" + googleMapsUri + '\'' +
                ", photos=" + photos +
                ", reviews=" + reviews +
                '}';
    }
}
