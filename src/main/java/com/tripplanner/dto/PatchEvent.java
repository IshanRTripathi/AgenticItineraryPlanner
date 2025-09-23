package com.tripplanner.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * PatchEvent DTO for Server-Sent Events (SSE) updates.
 */
public class PatchEvent {
    
    @NotBlank
    @JsonProperty("type")
    private String type = "PatchEvent";
    
    @NotBlank
    @JsonProperty("itineraryId")
    private String itineraryId;
    
    @NotNull
    @JsonProperty("fromVersion")
    private Integer fromVersion;
    
    @NotNull
    @JsonProperty("toVersion")
    private Integer toVersion;
    
    @JsonProperty("diff")
    private ItineraryDiff diff;
    
    @JsonProperty("summary")
    private String summary;
    
    @NotBlank
    @JsonProperty("updatedBy")
    private String updatedBy; // "agent" or "user"
    
    public PatchEvent() {}
    
    public PatchEvent(String itineraryId, Integer fromVersion, Integer toVersion, 
                     ItineraryDiff diff, String summary, String updatedBy) {
        this.itineraryId = itineraryId;
        this.fromVersion = fromVersion;
        this.toVersion = toVersion;
        this.diff = diff;
        this.summary = summary;
        this.updatedBy = updatedBy;
    }
    
    // Getters and Setters
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public String getItineraryId() {
        return itineraryId;
    }
    
    public void setItineraryId(String itineraryId) {
        this.itineraryId = itineraryId;
    }
    
    public Integer getFromVersion() {
        return fromVersion;
    }
    
    public void setFromVersion(Integer fromVersion) {
        this.fromVersion = fromVersion;
    }
    
    public Integer getToVersion() {
        return toVersion;
    }
    
    public void setToVersion(Integer toVersion) {
        this.toVersion = toVersion;
    }
    
    public ItineraryDiff getDiff() {
        return diff;
    }
    
    public void setDiff(ItineraryDiff diff) {
        this.diff = diff;
    }
    
    public String getSummary() {
        return summary;
    }
    
    public void setSummary(String summary) {
        this.summary = summary;
    }
    
    public String getUpdatedBy() {
        return updatedBy;
    }
    
    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }
    
    @Override
    public String toString() {
        return "PatchEvent{" +
                "type='" + type + '\'' +
                ", itineraryId='" + itineraryId + '\'' +
                ", fromVersion=" + fromVersion +
                ", toVersion=" + toVersion +
                ", diff=" + diff +
                ", summary='" + summary + '\'' +
                ", updatedBy='" + updatedBy + '\'' +
                '}';
    }
}
