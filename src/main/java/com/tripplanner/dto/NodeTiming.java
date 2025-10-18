package com.tripplanner.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

/**
 * Node timing information.
 * Uses Long (milliseconds since epoch) for Firestore storage.
 */
public class NodeTiming {
    
    @JsonProperty("startTime")
    private Long startTime; // milliseconds since epoch
    
    @JsonProperty("endTime")
    private Long endTime; // milliseconds since epoch
    
    @JsonProperty("durationMin")
    private Integer durationMin;
    
    public NodeTiming() {}
    
    public NodeTiming(Long startTime, Long endTime, Integer durationMin) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.durationMin = durationMin;
    }
    
    // Getters and Setters
    public Long getStartTime() {
        return startTime;
    }
    
    public void setStartTime(Long startTime) {
        this.startTime = startTime;
    }
    
    public Long getEndTime() {
        return endTime;
    }
    
    public void setEndTime(Long endTime) {
        this.endTime = endTime;
    }
    
    // Helper methods to get Instant objects
    @JsonIgnore
    public Instant getStartTimeAsInstant() {
        return startTime != null ? Instant.ofEpochMilli(startTime) : null;
    }
    
    @JsonIgnore
    public Instant getEndTimeAsInstant() {
        return endTime != null ? Instant.ofEpochMilli(endTime) : null;
    }
    
    public Integer getDurationMin() {
        return durationMin;
    }
    
    public void setDurationMin(Integer durationMin) {
        this.durationMin = durationMin;
    }
    
    @Override
    public String toString() {
        return "NodeTiming{" +
                "startTime=" + startTime +
                ", endTime=" + endTime +
                ", durationMin=" + durationMin +
                '}';
    }
}
