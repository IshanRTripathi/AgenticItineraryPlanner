package com.tripplanner.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

/**
 * Review DTO for Google Places API reviews.
 */
public class Review {
    
    @JsonProperty("author_name")
    private String authorName;
    
    @JsonProperty("author_url")
    private String authorUrl;
    
    @JsonProperty("language")
    private String language;
    
    @JsonProperty("profile_photo_url")
    private String profilePhotoUrl;
    
    @JsonProperty("rating")
    private Integer rating;
    
    @JsonProperty("relative_time_description")
    private String relativeTimeDescription;
    
    @JsonProperty("text")
    private String text;
    
    @JsonProperty("time")
    private Long time; // Unix timestamp
    
    // Computed fields
    private LocalDateTime reviewDate;
    private String sentiment; // positive, negative, neutral
    
    public Review() {}
    
    public Review(String authorName, Integer rating, String text) {
        this.authorName = authorName;
        this.rating = rating;
        this.text = text;
    }
    
    // Getters and Setters
    public String getAuthorName() {
        return authorName;
    }
    
    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }
    
    public String getAuthorUrl() {
        return authorUrl;
    }
    
    public void setAuthorUrl(String authorUrl) {
        this.authorUrl = authorUrl;
    }
    
    public String getLanguage() {
        return language;
    }
    
    public void setLanguage(String language) {
        this.language = language;
    }
    
    public String getProfilePhotoUrl() {
        return profilePhotoUrl;
    }
    
    public void setProfilePhotoUrl(String profilePhotoUrl) {
        this.profilePhotoUrl = profilePhotoUrl;
    }
    
    public Integer getRating() {
        return rating;
    }
    
    public void setRating(Integer rating) {
        this.rating = rating;
    }
    
    public String getRelativeTimeDescription() {
        return relativeTimeDescription;
    }
    
    public void setRelativeTimeDescription(String relativeTimeDescription) {
        this.relativeTimeDescription = relativeTimeDescription;
    }
    
    public String getText() {
        return text;
    }
    
    public void setText(String text) {
        this.text = text;
    }
    
    public Long getTime() {
        return time;
    }
    
    public void setTime(Long time) {
        this.time = time;
        // Convert Unix timestamp to LocalDateTime
        if (time != null) {
            this.reviewDate = LocalDateTime.ofEpochSecond(time, 0, java.time.ZoneOffset.UTC);
        }
    }
    
    public LocalDateTime getReviewDate() {
        return reviewDate;
    }
    
    public void setReviewDate(LocalDateTime reviewDate) {
        this.reviewDate = reviewDate;
    }
    
    public String getSentiment() {
        return sentiment;
    }
    
    public void setSentiment(String sentiment) {
        this.sentiment = sentiment;
    }
    
    /**
     * Check if this is a positive review (rating >= 4).
     */
    public boolean isPositive() {
        return rating != null && rating >= 4;
    }
    
    /**
     * Check if this is a negative review (rating <= 2).
     */
    public boolean isNegative() {
        return rating != null && rating <= 2;
    }
    
    /**
     * Check if this is a neutral review (rating == 3).
     */
    public boolean isNeutral() {
        return rating != null && rating == 3;
    }
    
    /**
     * Get review length category.
     */
    public String getReviewLength() {
        if (text == null) {
            return "none";
        }
        
        int length = text.length();
        if (length < 50) {
            return "short";
        } else if (length < 200) {
            return "medium";
        } else {
            return "long";
        }
    }
    
    /**
     * Check if review has text content.
     */
    public boolean hasText() {
        return text != null && !text.trim().isEmpty();
    }
    
    /**
     * Get truncated text for display purposes.
     */
    public String getTruncatedText(int maxLength) {
        if (text == null) {
            return "";
        }
        
        if (text.length() <= maxLength) {
            return text;
        }
        
        return text.substring(0, maxLength - 3) + "...";
    }
    
    @Override
    public String toString() {
        return "Review{" +
                "authorName='" + authorName + '\'' +
                ", rating=" + rating +
                ", text='" + (text != null ? getTruncatedText(50) : null) + '\'' +
                ", relativeTimeDescription='" + relativeTimeDescription + '\'' +
                ", language='" + language + '\'' +
                '}';
    }
}