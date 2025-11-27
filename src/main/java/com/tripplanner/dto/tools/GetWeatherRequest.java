package com.tripplanner.dto.tools;

import com.tripplanner.dto.Coordinates;

public class GetWeatherRequest {
    private String itineraryId; // Optional: for caching
    private String location;
    private Coordinates coordinates;
    private String date;
    
    public GetWeatherRequest() {
    }
    
    public String getItineraryId() {
        return itineraryId;
    }
    
    public void setItineraryId(String itineraryId) {
        this.itineraryId = itineraryId;
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
    
    public String getDate() {
        return date;
    }
    
    public void setDate(String date) {
        this.date = date;
    }
}
