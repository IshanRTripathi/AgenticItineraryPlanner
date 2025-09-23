package com.tripplanner.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

/**
 * Agent status information.
 */
public class AgentStatus {
    
    @JsonProperty("lastRunAt")
    private Instant lastRunAt;
    
    @JsonProperty("status")
    private String status = "idle"; // "idle", "running", "completed", "failed"
    
    public AgentStatus() {}
    
    public AgentStatus(Instant lastRunAt, String status) {
        this.lastRunAt = lastRunAt;
        this.status = status;
    }
    
    // Getters and Setters
    public Instant getLastRunAt() {
        return lastRunAt;
    }
    
    public void setLastRunAt(Instant lastRunAt) {
        this.lastRunAt = lastRunAt;
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
