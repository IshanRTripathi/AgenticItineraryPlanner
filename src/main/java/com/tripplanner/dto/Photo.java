package com.tripplanner.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Photo DTO for Google Places API photos.
 */
public class Photo {
    
    @JsonProperty("photo_reference")
    private String photoReference;
    
    @JsonProperty("height")
    private Integer height;
    
    @JsonProperty("width")
    private Integer width;
    
    @JsonProperty("html_attributions")
    private List<String> htmlAttributions;
    
    // Computed fields
    private String photoUrl;
    private String thumbnailUrl;
    
    public Photo() {}
    
    public Photo(String photoReference, Integer width, Integer height) {
        this.photoReference = photoReference;
        this.width = width;
        this.height = height;
    }
    
    // Getters and Setters
    public String getPhotoReference() {
        return photoReference;
    }
    
    public void setPhotoReference(String photoReference) {
        this.photoReference = photoReference;
    }
    
    public Integer getHeight() {
        return height;
    }
    
    public void setHeight(Integer height) {
        this.height = height;
    }
    
    public Integer getWidth() {
        return width;
    }
    
    public void setWidth(Integer width) {
        this.width = width;
    }
    
    public List<String> getHtmlAttributions() {
        return htmlAttributions;
    }
    
    public void setHtmlAttributions(List<String> htmlAttributions) {
        this.htmlAttributions = htmlAttributions;
    }
    
    public String getPhotoUrl() {
        return photoUrl;
    }
    
    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }
    
    public String getThumbnailUrl() {
        return thumbnailUrl;
    }
    
    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }
    
    /**
     * Generate photo URL for Google Places API.
     */
    public String generatePhotoUrl(String apiKey, int maxWidth) {
        if (photoReference == null || apiKey == null) {
            return null;
        }
        return String.format("https://maps.googleapis.com/maps/api/place/photo?maxwidth=%d&photo_reference=%s&key=%s",
                           maxWidth, photoReference, apiKey);
    }
    
    /**
     * Check if this photo has valid dimensions.
     */
    public boolean hasValidDimensions() {
        return width != null && height != null && width > 0 && height > 0;
    }
    
    /**
     * Get aspect ratio of the photo.
     */
    public double getAspectRatio() {
        if (!hasValidDimensions()) {
            return 1.0; // Default square aspect ratio
        }
        return (double) width / height;
    }
    
    @Override
    public String toString() {
        return "Photo{" +
                "photoReference='" + photoReference + '\'' +
                ", height=" + height +
                ", width=" + width +
                ", htmlAttributions=" + htmlAttributions +
                ", photoUrl='" + photoUrl + '\'' +
                ", thumbnailUrl='" + thumbnailUrl + '\'' +
                '}';
    }
}