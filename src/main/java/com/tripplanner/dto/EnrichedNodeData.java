package com.tripplanner.dto;

import java.util.List;
import java.util.Map;

/**
 * Enrichment data for a single node.
 * Contains only the enriched fields, not the entire node.
 */
public class EnrichedNodeData {
    private String nodeId;
    private NodeLocation location;
    private Map<String, Object> agentData;
    
    public EnrichedNodeData(String nodeId) {
        this.nodeId = nodeId;
    }
    
    // Getters and setters
    public String getNodeId() {
        return nodeId;
    }
    
    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }
    
    public NodeLocation getLocation() {
        return location;
    }
    
    public void setLocation(NodeLocation location) {
        this.location = location;
    }
    
    public Map<String, Object> getAgentData() {
        return agentData;
    }
    
    public void setAgentData(Map<String, Object> agentData) {
        this.agentData = agentData;
    }
}
