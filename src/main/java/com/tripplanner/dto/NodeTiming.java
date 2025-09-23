package com.tripplanner.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

/**
 * Node timing information.
 */
public class NodeTiming {
    
    @JsonProperty("startTime")
    private Instant startTime;
    
    @JsonProperty("endTime")
    private Instant endTime;
    
    @JsonProperty("durationMin")
    private Integer durationMin;
    
    public NodeTiming() {}
    
    public NodeTiming(Instant startTime, Instant endTime, Integer durationMin) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.durationMin = durationMin;
    }
    
    // Getters and Setters
    public Instant getStartTime() {
        return startTime;
    }
    
    public void setStartTime(Instant startTime) {
        this.startTime = startTime;
    }
    
    public Instant getEndTime() {
        return endTime;
    }
    
    public void setEndTime(Instant endTime) {
        this.endTime = endTime;
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
