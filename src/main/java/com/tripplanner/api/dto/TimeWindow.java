package com.tripplanner.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Day time window.
 */
public class TimeWindow {
    
    @JsonProperty("start")
    private String start; // "09:00"
    
    @JsonProperty("end")
    private String end; // "18:00"
    
    public TimeWindow() {}
    
    public TimeWindow(String start, String end) {
        this.start = start;
        this.end = end;
    }
    
    // Getters and Setters
    public String getStart() {
        return start;
    }
    
    public void setStart(String start) {
        this.start = start;
    }
    
    public String getEnd() {
        return end;
    }
    
    public void setEnd(String end) {
        this.end = end;
    }
    
    @Override
    public String toString() {
        return "TimeWindow{" +
                "start='" + start + '\'' +
                ", end='" + end + '\'' +
                '}';
    }
}
