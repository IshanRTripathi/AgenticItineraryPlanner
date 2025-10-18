package com.tripplanner.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * Workflow edge for connecting nodes in the workflow view.
 * Contains connection information and styling for edges between nodes.
 */
public class WorkflowEdge {
    
    @JsonProperty("id")
    private String id;
    
    @JsonProperty("source")
    private String source; // Source node id
    
    @JsonProperty("target")
    private String target; // Target node id
    
    @JsonProperty("sourceHandle")
    private String sourceHandle;
    
    @JsonProperty("targetHandle")
    private String targetHandle;
    
    @JsonProperty("type")
    private String type; // "default", "straight", "step", "smoothstep", "bezier"
    
    @JsonProperty("animated")
    private Boolean animated = false;
    
    @JsonProperty("style")
    private Map<String, Object> style;
    
    @JsonProperty("label")
    private String label;
    
    @JsonProperty("labelStyle")
    private Map<String, Object> labelStyle;
    
    @JsonProperty("data")
    private Map<String, Object> data; // Additional edge data
    
    public WorkflowEdge() {}
    
    public WorkflowEdge(String id, String source, String target) {
        this.id = id;
        this.source = source;
        this.target = target;
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getSource() {
        return source;
    }
    
    public void setSource(String source) {
        this.source = source;
    }
    
    public String getTarget() {
        return target;
    }
    
    public void setTarget(String target) {
        this.target = target;
    }
    
    public String getSourceHandle() {
        return sourceHandle;
    }
    
    public void setSourceHandle(String sourceHandle) {
        this.sourceHandle = sourceHandle;
    }
    
    public String getTargetHandle() {
        return targetHandle;
    }
    
    public void setTargetHandle(String targetHandle) {
        this.targetHandle = targetHandle;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public Boolean getAnimated() {
        return animated;
    }
    
    public void setAnimated(Boolean animated) {
        this.animated = animated;
    }
    
    public Map<String, Object> getStyle() {
        return style;
    }
    
    public void setStyle(Map<String, Object> style) {
        this.style = style;
    }
    
    public String getLabel() {
        return label;
    }
    
    public void setLabel(String label) {
        this.label = label;
    }
    
    public Map<String, Object> getLabelStyle() {
        return labelStyle;
    }
    
    public void setLabelStyle(Map<String, Object> labelStyle) {
        this.labelStyle = labelStyle;
    }
    
    public Map<String, Object> getData() {
        return data;
    }
    
    public void setData(Map<String, Object> data) {
        this.data = data;
    }
    
    @Override
    public String toString() {
        return "WorkflowEdge{" +
                "id='" + id + '\'' +
                ", source='" + source + '\'' +
                ", target='" + target + '\'' +
                ", sourceHandle='" + sourceHandle + '\'' +
                ", targetHandle='" + targetHandle + '\'' +
                ", type='" + type + '\'' +
                ", animated=" + animated +
                ", style=" + style +
                ", label='" + label + '\'' +
                ", labelStyle=" + labelStyle +
                ", data=" + data +
                '}';
    }
}