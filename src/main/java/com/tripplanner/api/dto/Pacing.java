package com.tripplanner.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Day pacing information.
 */
public class Pacing {
    
    @JsonProperty("score")
    private String score; // "relaxed", "balanced", "intense"
    
    @JsonProperty("transitMin")
    private Integer transitMin = 0;
    
    @JsonProperty("stops")
    private Integer stops = 0;
    
    public Pacing() {}
    
    public Pacing(String score, Integer transitMin, Integer stops) {
        this.score = score;
        this.transitMin = transitMin;
        this.stops = stops;
    }
    
    // Getters and Setters
    public String getScore() {
        return score;
    }
    
    public void setScore(String score) {
        this.score = score;
    }
    
    public Integer getTransitMin() {
        return transitMin;
    }
    
    public void setTransitMin(Integer transitMin) {
        this.transitMin = transitMin;
    }
    
    public Integer getStops() {
        return stops;
    }
    
    public void setStops(Integer stops) {
        this.stops = stops;
    }
    
    @Override
    public String toString() {
        return "Pacing{" +
                "score='" + score + '\'' +
                ", transitMin=" + transitMin +
                ", stops=" + stops +
                '}';
    }
}
