package com.tripplanner.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Revision record for tracking changes to itineraries.
 * Contains information about what changed, when, and by whom.
 */
public class RevisionRecord {
    
    @NotBlank
    @JsonProperty("revisionId")
    private String revisionId;
    
    @NotNull
    @JsonProperty("timestamp")
    private Long timestamp;
    
    @JsonProperty("agent")
    private String agent;
    
    @Valid
    @JsonProperty("changes")
    private List<ChangeDetail> changes;
    
    @JsonProperty("reason")
    private String reason;
    
    @JsonProperty("userId")
    private String userId;
    
    public RevisionRecord() {}
    
    public RevisionRecord(String revisionId, Long timestamp, String agent, 
                         List<ChangeDetail> changes, String reason, String userId) {
        this.revisionId = revisionId;
        this.timestamp = timestamp;
        this.agent = agent;
        this.changes = changes;
        this.reason = reason;
        this.userId = userId;
    }
    
    // Getters and Setters
    public String getRevisionId() {
        return revisionId;
    }
    
    public void setRevisionId(String revisionId) {
        this.revisionId = revisionId;
    }
    
    public Long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getAgent() {
        return agent;
    }
    
    public void setAgent(String agent) {
        this.agent = agent;
    }
    
    public List<ChangeDetail> getChanges() {
        return changes;
    }
    
    public void setChanges(List<ChangeDetail> changes) {
        this.changes = changes;
    }
    
    public String getReason() {
        return reason;
    }
    
    public void setReason(String reason) {
        this.reason = reason;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    @Override
    public String toString() {
        return "RevisionRecord{" +
                "revisionId='" + revisionId + '\'' +
                ", timestamp=" + timestamp +
                ", agent='" + agent + '\'' +
                ", changes=" + changes +
                ", reason='" + reason + '\'' +
                ", userId='" + userId + '\'' +
                '}';
    }
}