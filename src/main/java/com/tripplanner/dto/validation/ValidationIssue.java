package com.tripplanner.dto.validation;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Individual validation issue
 */
public class ValidationIssue {
    
    @JsonProperty("id")
    private String id;
    
    @JsonProperty("type")
    private String type;
    
    @JsonProperty("category")
    private String category;
    
    @JsonProperty("severity")
    private String severity; // ERROR, WARNING
    
    @JsonProperty("nodeId")
    private String nodeId;
    
    @JsonProperty("dayNumber")
    private Integer dayNumber;
    
    @JsonProperty("message")
    private String message;
    
    @JsonProperty("recommendation")
    private String recommendation;
    
    public ValidationIssue() {}
    
    // Getters and Setters
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public String getCategory() {
        return category;
    }
    
    public void setCategory(String category) {
        this.category = category;
    }
    
    public String getSeverity() {
        return severity;
    }
    
    public void setSeverity(String severity) {
        this.severity = severity;
    }
    
    public String getNodeId() {
        return nodeId;
    }
    
    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }
    
    public Integer getDayNumber() {
        return dayNumber;
    }
    
    public void setDayNumber(Integer dayNumber) {
        this.dayNumber = dayNumber;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public String getRecommendation() {
        return recommendation;
    }
    
    public void setRecommendation(String recommendation) {
        this.recommendation = recommendation;
    }
}
