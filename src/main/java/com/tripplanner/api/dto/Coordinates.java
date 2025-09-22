package com.tripplanner.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Geographic coordinates.
 */
public class Coordinates {
    
    @JsonProperty("lat")
    private Double lat;
    
    @JsonProperty("lng")
    private Double lng;
    
    public Coordinates() {}
    
    public Coordinates(Double lat, Double lng) {
        this.lat = lat;
        this.lng = lng;
    }
    
    // Getters and Setters
    public Double getLat() {
        return lat;
    }
    
    public void setLat(Double lat) {
        this.lat = lat;
    }
    
    public Double getLng() {
        return lng;
    }
    
    public void setLng(Double lng) {
        this.lng = lng;
    }
    
    @Override
    public String toString() {
        return "Coordinates{" +
                "lat=" + lat +
                ", lng=" + lng +
                '}';
    }
}
