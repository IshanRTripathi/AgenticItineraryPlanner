package com.tripplanner.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.ArrayList;

/**
 * Activity search response from Expedia API.
 */
public class ActivitySearchResponse {
    
    @JsonProperty("activities")
    private List<Activity> activities;
    
    @JsonProperty("totalResults")
    private Integer totalResults;
    
    @JsonProperty("searchId")
    private String searchId;
    
    @JsonProperty("status")
    private String status;
    
    @JsonProperty("errorMessage")
    private String errorMessage;
    
    @JsonProperty("currency")
    private String currency;
    
    @JsonProperty("location")
    private String location;
    
    @JsonProperty("date")
    private String date;
    
    @JsonProperty("participants")
    private Integer participants;
    
    @JsonProperty("activityType")
    private String activityType;
    
    public ActivitySearchResponse() {
        this.activities = new ArrayList<>();
    }
    
    // Getters and Setters
    public List<Activity> getActivities() {
        return activities;
    }
    
    public void setActivities(List<Activity> activities) {
        this.activities = activities;
    }
    
    public Integer getTotalResults() {
        return totalResults;
    }
    
    public void setTotalResults(Integer totalResults) {
        this.totalResults = totalResults;
    }
    
    public String getSearchId() {
        return searchId;
    }
    
    public void setSearchId(String searchId) {
        this.searchId = searchId;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public String getCurrency() {
        return currency;
    }
    
    public void setCurrency(String currency) {
        this.currency = currency;
    }
    
    public String getLocation() {
        return location;
    }
    
    public void setLocation(String location) {
        this.location = location;
    }
    
    public String getDate() {
        return date;
    }
    
    public void setDate(String date) {
        this.date = date;
    }
    
    public Integer getParticipants() {
        return participants;
    }
    
    public void setParticipants(Integer participants) {
        this.participants = participants;
    }
    
    public String getActivityType() {
        return activityType;
    }
    
    public void setActivityType(String activityType) {
        this.activityType = activityType;
    }
    
    /**
     * Check if the search was successful.
     */
    public boolean isSuccessful() {
        return "OK".equals(status) || "SUCCESS".equals(status);
    }
    
    /**
     * Check if there are activities in the response.
     */
    public boolean hasActivities() {
        return activities != null && !activities.isEmpty();
    }
    
    /**
     * Get activities filtered by type.
     */
    public List<Activity> getActivitiesByType(String type) {
        if (activities == null || type == null) {
            return new ArrayList<>();
        }
        return activities.stream()
            .filter(activity -> type.equalsIgnoreCase(activity.getType()))
            .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Get activities within price range.
     */
    public List<Activity> getActivitiesInPriceRange(Double minPrice, Double maxPrice) {
        if (activities == null) {
            return new ArrayList<>();
        }
        return activities.stream()
            .filter(activity -> {
                Double price = activity.getPrice();
                if (price == null) return false;
                if (minPrice != null && price < minPrice) return false;
                if (maxPrice != null && price > maxPrice) return false;
                return true;
            })
            .collect(java.util.stream.Collectors.toList());
    }
    
    @Override
    public String toString() {
        return "ActivitySearchResponse{" +
                "activities=" + (activities != null ? activities.size() : 0) + " activities" +
                ", totalResults=" + totalResults +
                ", searchId='" + searchId + '\'' +
                ", status='" + status + '\'' +
                ", errorMessage='" + errorMessage + '\'' +
                ", currency='" + currency + '\'' +
                ", location='" + location + '\'' +
                ", date='" + date + '\'' +
                ", participants=" + participants +
                ", activityType='" + activityType + '\'' +
                '}';
    }
}