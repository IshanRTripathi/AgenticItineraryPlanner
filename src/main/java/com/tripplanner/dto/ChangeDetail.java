package com.tripplanner.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * Change detail for tracking specific changes made to itinerary elements.
 * Contains information about what was changed, how it was changed, and the values.
 */
public class ChangeDetail {
    
    @JsonProperty("operationType")
    private String operationType; // "insert", "update", "delete", "move", "replace"
    
    @JsonProperty("elementType")
    private String elementType; // "node", "day", "edge", "setting"
    
    @JsonProperty("elementId")
    private String elementId; // ID of the changed element
    
    @JsonProperty("fieldPath")
    private String fieldPath; // Path to the changed field (e.g., "timing.startTime")
    
    @JsonProperty("oldValue")
    private Object oldValue; // Previous value
    
    @JsonProperty("newValue")
    private Object newValue; // New value
    
    @JsonProperty("affectedFields")
    private List<String> affectedFields; // List of fields that were changed
    
    @JsonProperty("metadata")
    private Map<String, Object> metadata; // Additional change metadata
    
    public ChangeDetail() {}
    
    public ChangeDetail(String operationType, String elementType, String elementId, 
                       String fieldPath, Object oldValue, Object newValue) {
        this.operationType = operationType;
        this.elementType = elementType;
        this.elementId = elementId;
        this.fieldPath = fieldPath;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }
    
    // Getters and Setters
    public String getOperationType() {
        return operationType;
    }
    
    public void setOperationType(String operationType) {
        this.operationType = operationType;
    }
    
    public String getElementType() {
        return elementType;
    }
    
    public void setElementType(String elementType) {
        this.elementType = elementType;
    }
    
    public String getElementId() {
        return elementId;
    }
    
    public void setElementId(String elementId) {
        this.elementId = elementId;
    }
    
    public String getFieldPath() {
        return fieldPath;
    }
    
    public void setFieldPath(String fieldPath) {
        this.fieldPath = fieldPath;
    }
    
    public Object getOldValue() {
        return oldValue;
    }
    
    public void setOldValue(Object oldValue) {
        this.oldValue = oldValue;
    }
    
    public Object getNewValue() {
        return newValue;
    }
    
    public void setNewValue(Object newValue) {
        this.newValue = newValue;
    }
    
    public List<String> getAffectedFields() {
        return affectedFields;
    }
    
    public void setAffectedFields(List<String> affectedFields) {
        this.affectedFields = affectedFields;
    }
    
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
    
    // Convenience methods for backward compatibility
    public void setField(String field) {
        this.fieldPath = field;
    }
    
    public void setOperation(String operation) {
        this.operationType = operation;
    }
    
    @Override
    public String toString() {
        return "ChangeDetail{" +
                "operationType='" + operationType + '\'' +
                ", elementType='" + elementType + '\'' +
                ", elementId='" + elementId + '\'' +
                ", fieldPath='" + fieldPath + '\'' +
                ", oldValue=" + oldValue +
                ", newValue=" + newValue +
                ", affectedFields=" + affectedFields +
                ", metadata=" + metadata +
                '}';
    }
}