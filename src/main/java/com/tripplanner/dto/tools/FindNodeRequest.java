package com.tripplanner.dto.tools;

public class FindNodeRequest {
    private String itineraryId;
    private String nodeId;
    
    public FindNodeRequest() {
    }
    
    public FindNodeRequest(String itineraryId, String nodeId) {
        this.itineraryId = itineraryId;
        this.nodeId = nodeId;
    }
    
    public String getItineraryId() {
        return itineraryId;
    }
    
    public void setItineraryId(String itineraryId) {
        this.itineraryId = itineraryId;
    }
    
    public String getNodeId() {
        return nodeId;
    }
    
    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }
}
