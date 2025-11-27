package com.tripplanner.dto.tools;

/**
 * Request DTO for check opening hours tool.
 * P2-5: Check if a place is open at a specific date/time.
 */
public class CheckOpeningHoursRequest {
    
    private String placeId; // Google Places ID
    private String placeName; // Fallback if placeId not available
    private String date; // ISO format: YYYY-MM-DD
    private String time; // HH:mm format
    
    public CheckOpeningHoursRequest() {
    }
    
    public CheckOpeningHoursRequest(String placeId, String date, String time) {
        this.placeId = placeId;
        this.date = date;
        this.time = time;
    }
    
    // Getters and setters
    
    public String getPlaceId() {
        return placeId;
    }
    
    public void setPlaceId(String placeId) {
        this.placeId = placeId;
    }
    
    public String getPlaceName() {
        return placeName;
    }
    
    public void setPlaceName(String placeName) {
        this.placeName = placeName;
    }
    
    public String getDate() {
        return date;
    }
    
    public void setDate(String date) {
        this.date = date;
    }
    
    public String getTime() {
        return time;
    }
    
    public void setTime(String time) {
        this.time = time;
    }
}
