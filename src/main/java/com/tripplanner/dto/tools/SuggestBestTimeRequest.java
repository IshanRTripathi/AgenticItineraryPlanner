package com.tripplanner.dto.tools;

/**
 * Request for suggest-best-time tool
 * Finds optimal times for activities based on weather and activity type
 */
public class SuggestBestTimeRequest {

    private String activityName;
    private String activityType; // outdoor_park, scenic_spot, beach_water, indoor_museum, etc.
    private String location;
    private String date; // YYYY-MM-DD format
    private Integer duration; // minutes

    private String itineraryId;

    // Constructors
    public SuggestBestTimeRequest() {
    }

    public SuggestBestTimeRequest(String activityName, String activityType, String location, String date,
            Integer duration, String itineraryId) {
        this.activityName = activityName;
        this.activityType = activityType;
        this.location = location;
        this.date = date;
        this.duration = duration;
        this.itineraryId = itineraryId;
    }

    // Getters and setters
    public String getActivityName() {
        return activityName;
    }

    public void setActivityName(String activityName) {
        this.activityName = activityName;
    }

    public String getActivityType() {
        return activityType;
    }

    public void setActivityType(String activityType) {
        this.activityType = activityType;
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

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public String getItineraryId() {
        return itineraryId;
    }

    public void setItineraryId(String itineraryId) {
        this.itineraryId = itineraryId;
    }
}
