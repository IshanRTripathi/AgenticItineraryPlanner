package com.tripplanner.dto.tools;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request DTO for node ID generation tool
 */
public class NodeIdRequest {
    
    @JsonProperty("itineraryId")
    private String itineraryId;
    
    @JsonProperty("dayNumber")
    private Integer dayNumber;
    
    @JsonProperty("nodeType")
    private String nodeType; // "attraction", "meal", "transport", "accommodation"
    
    @JsonProperty("sequenceHint")
    private Integer sequenceHint; // Optional: suggested sequence number
    
    public NodeIdRequest() {}
    
    public NodeIdRequest(String itineraryId, Integer dayNumber, String nodeType) {
        this.itineraryId = itineraryId;
        this.dayNumber = dayNumber;
        this.nodeType = nodeType;
    }
    
    // Getters and Setters
    public String getItineraryId() {
        return itineraryId;
    }
    
    public void setItineraryId(String itineraryId) {
        this.itineraryId = itineraryId;
    }
    
    public Integer getDayNumber() {
        return dayNumber;
    }
    
    public void setDayNumber(Integer dayNumber) {
        this.dayNumber = dayNumber;
    }
    
    public String getNodeType() {
        return nodeType;
    }
    
    public void setNodeType(String nodeType) {
        this.nodeType = nodeType;
    }
    
    public Integer getSequenceHint() {
        return sequenceHint;
    }
    
    public void setSequenceHint(Integer sequenceHint) {
        this.sequenceHint = sequenceHint;
    }
}
