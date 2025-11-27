package com.tripplanner.dto.tools;

import java.util.HashMap;
import java.util.Map;

public class UpdateNodeRequest {
    private String itineraryId;
    private String nodeId;
    private Map<String, Object> updates;
    
    public UpdateNodeRequest() {
        this.updates = new HashMap<>();
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
    
    public Map<String, Object> getUpdates() {
        return updates;
    }
    
    public void setUpdates(Map<String, Object> updates) {
        this.updates = updates;
    }
}
