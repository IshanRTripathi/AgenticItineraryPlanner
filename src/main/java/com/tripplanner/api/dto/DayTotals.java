package com.tripplanner.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Day totals for distance, cost, and duration.
 */
public class DayTotals {
    
    @JsonProperty("distanceKm")
    private Double distanceKm = 0.0;
    
    @JsonProperty("cost")
    private Double cost = 0.0;
    
    @JsonProperty("durationHr")
    private Double durationHr = 0.0;
    
    public DayTotals() {}
    
    public DayTotals(Double distanceKm, Double cost, Double durationHr) {
        this.distanceKm = distanceKm;
        this.cost = cost;
        this.durationHr = durationHr;
    }
    
    // Getters and Setters
    public Double getDistanceKm() {
        return distanceKm;
    }
    
    public void setDistanceKm(Double distanceKm) {
        this.distanceKm = distanceKm;
    }
    
    public Double getCost() {
        return cost;
    }
    
    public void setCost(Double cost) {
        this.cost = cost;
    }
    
    public Double getDurationHr() {
        return durationHr;
    }
    
    public void setDurationHr(Double durationHr) {
        this.durationHr = durationHr;
    }
    
    @Override
    public String toString() {
        return "DayTotals{" +
                "distanceKm=" + distanceKm +
                ", cost=" + cost +
                ", durationHr=" + durationHr +
                '}';
    }
}
