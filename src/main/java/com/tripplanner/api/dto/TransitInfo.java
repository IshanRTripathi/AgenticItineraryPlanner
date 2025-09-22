package com.tripplanner.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Transit information for nodes.
 */
public class TransitInfo {
    
    @JsonProperty("mode")
    private String mode; // "taxi", "walk", "bus", "metro", etc.
    
    @JsonProperty("distanceKm")
    private Double distanceKm;
    
    @JsonProperty("timeMin")
    private Integer timeMin;
    
    public TransitInfo() {}
    
    public TransitInfo(String mode, Double distanceKm, Integer timeMin) {
        this.mode = mode;
        this.distanceKm = distanceKm;
        this.timeMin = timeMin;
    }
    
    // Getters and Setters
    public String getMode() {
        return mode;
    }
    
    public void setMode(String mode) {
        this.mode = mode;
    }
    
    public Double getDistanceKm() {
        return distanceKm;
    }
    
    public void setDistanceKm(Double distanceKm) {
        this.distanceKm = distanceKm;
    }
    
    public Integer getTimeMin() {
        return timeMin;
    }
    
    public void setTimeMin(Integer timeMin) {
        this.timeMin = timeMin;
    }
    
    @Override
    public String toString() {
        return "TransitInfo{" +
                "mode='" + mode + '\'' +
                ", distanceKm=" + distanceKm +
                ", timeMin=" + timeMin +
                '}';
    }
}
