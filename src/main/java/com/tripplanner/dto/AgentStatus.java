package com.tripplanner.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

/**
 * Agent status information.
 * Uses Long (milliseconds since epoch) for Firestore storage.
 */
public class AgentStatus {
    
    @JsonProperty(value = "lastRunAt", required = false)
    private Long lastRunAt; // milliseconds since epoch
    
    @JsonProperty(value = "status", required = false)
    private String status = "idle"; // "idle", "running", "completed", "failed"
    
    @JsonProperty(value = "triggeredTimestamps", required = false)
    private java.util.List<Long> triggeredTimestamps = new java.util.ArrayList<>(); // List of trigger timestamps
    
    public AgentStatus() {}
    
    public AgentStatus(Long lastRunAt, String status) {
        this.lastRunAt = lastRunAt;
        this.status = status;
    }
    
    public AgentStatus(Instant lastRunAt, String status) {
        this.lastRunAt = lastRunAt != null ? lastRunAt.toEpochMilli() : null;
        this.status = status;
    }
    
    // Getters and Setters
    public Long getLastRunAt() {
        return lastRunAt;
    }
    
    public void setLastRunAt(Long lastRunAt) {
        this.lastRunAt = lastRunAt;
    }
    
    // Helper method to get Instant object
    @JsonIgnore
    public Instant getLastRunAtAsInstant() {
        return lastRunAt != null ? Instant.ofEpochMilli(lastRunAt) : null;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public java.util.List<Long> getTriggeredTimestamps() {
        return triggeredTimestamps;
    }
    
    public void setTriggeredTimestamps(java.util.List<Long> triggeredTimestamps) {
        this.triggeredTimestamps = triggeredTimestamps != null ? triggeredTimestamps : new java.util.ArrayList<>();
    }
    
    // Helper method to add a trigger timestamp
    public void addTriggeredTimestamp(Long timestamp) {
        if (this.triggeredTimestamps == null) {
            this.triggeredTimestamps = new java.util.ArrayList<>();
        }
        this.triggeredTimestamps.add(timestamp);
    }
    
    @Override
    public String toString() {
        return "AgentStatus{" +
                "lastRunAt=" + lastRunAt +
                ", status='" + status + '\'' +
                ", triggeredTimestamps=" + triggeredTimestamps +
                '}';
    }
}
