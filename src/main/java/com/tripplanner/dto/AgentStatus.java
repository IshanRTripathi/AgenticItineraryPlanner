package com.tripplanner.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

/**
 * Agent status information.
 * Uses Long (milliseconds since epoch) for Firestore compatibility.
 */
public class AgentStatus {
    
    @JsonProperty("lastRunAt")
    private Long lastRunAt; // milliseconds since epoch
    
    @JsonProperty("status")
    private String status = "idle"; // "idle", "running", "completed", "failed"
    
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
    
    @Override
    public String toString() {
        return "AgentStatus{" +
                "lastRunAt=" + lastRunAt +
                ", status='" + status + '\'' +
                '}';
    }
}
