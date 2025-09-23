package com.tripplanner.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Time slot for opening hours.
 */
public class TimeSlot {
    
    @JsonProperty("open")
    private String open; // "09:00"
    
    @JsonProperty("close")
    private String close; // "18:00"
    
    public TimeSlot() {}
    
    public TimeSlot(String open, String close) {
        this.open = open;
        this.close = close;
    }
    
    // Getters and Setters
    public String getOpen() {
        return open;
    }
    
    public void setOpen(String open) {
        this.open = open;
    }
    
    public String getClose() {
        return close;
    }
    
    public void setClose(String close) {
        this.close = close;
    }
    
    @Override
    public String toString() {
        return "TimeSlot{" +
                "open='" + open + '\'' +
                ", close='" + close + '\'' +
                '}';
    }
}
