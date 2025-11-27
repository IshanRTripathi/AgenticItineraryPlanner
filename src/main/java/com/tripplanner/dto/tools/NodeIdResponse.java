package com.tripplanner.dto.tools;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response DTO for node ID generation tool
 */
public class NodeIdResponse {
    
    @JsonProperty("success")
    private boolean success;
    
    @JsonProperty("nodeId")
    private String nodeId;
    
    @JsonProperty("error")
    private String error;
    
    public NodeIdResponse() {}
    
    public NodeIdResponse(String nodeId) {
        this.success = true;
        this.nodeId = nodeId;
    }
    
    public static NodeIdResponse error(String error) {
        NodeIdResponse response = new NodeIdResponse();
        response.success = false;
        response.error = error;
        return response;
    }
    
    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public String getNodeId() {
        return nodeId;
    }
    
    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }
    
    public String getError() {
        return error;
    }
    
    public void setError(String error) {
        this.error = error;
    }
}
