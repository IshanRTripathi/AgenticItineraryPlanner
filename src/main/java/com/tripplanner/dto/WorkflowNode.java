package com.tripplanner.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * Workflow node with position and metadata for workflow view.
 * Contains positioning and visual information for nodes in the workflow.
 */
public class WorkflowNode {
    
    @JsonProperty("id")
    private String id; // Should match NormalizedNode id
    
    @JsonProperty("position")
    private Position position;
    
    @JsonProperty("type")
    private String type; // "default", "input", "output", "custom"
    
    @JsonProperty("data")
    private Map<String, Object> data; // Additional node data for workflow
    
    @JsonProperty("style")
    private Map<String, Object> style; // CSS-like styling properties
    
    @JsonProperty("className")
    private String className;
    
    @JsonProperty("draggable")
    private Boolean draggable = true;
    
    @JsonProperty("selectable")
    private Boolean selectable = true;
    
    public WorkflowNode() {}
    
    public WorkflowNode(String id, Position position, String type) {
        this.id = id;
        this.position = position;
        this.type = type;
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public Position getPosition() {
        return position;
    }
    
    public void setPosition(Position position) {
        this.position = position;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public Map<String, Object> getData() {
        return data;
    }
    
    public void setData(Map<String, Object> data) {
        this.data = data;
    }
    
    public Map<String, Object> getStyle() {
        return style;
    }
    
    public void setStyle(Map<String, Object> style) {
        this.style = style;
    }
    
    public String getClassName() {
        return className;
    }
    
    public void setClassName(String className) {
        this.className = className;
    }
    
    public Boolean getDraggable() {
        return draggable;
    }
    
    public void setDraggable(Boolean draggable) {
        this.draggable = draggable;
    }
    
    public Boolean getSelectable() {
        return selectable;
    }
    
    public void setSelectable(Boolean selectable) {
        this.selectable = selectable;
    }
    
    @Override
    public String toString() {
        return "WorkflowNode{" +
                "id='" + id + '\'' +
                ", position=" + position +
                ", type='" + type + '\'' +
                ", data=" + data +
                ", style=" + style +
                ", className='" + className + '\'' +
                ", draggable=" + draggable +
                ", selectable=" + selectable +
                '}';
    }
    
    /**
     * Position coordinates for workflow node.
     */
    public static class Position {
        @JsonProperty("x")
        private Double x;
        
        @JsonProperty("y")
        private Double y;
        
        public Position() {}
        
        public Position(Double x, Double y) {
            this.x = x;
            this.y = y;
        }
        
        // Getters and Setters
        public Double getX() {
            return x;
        }
        
        public void setX(Double x) {
            this.x = x;
        }
        
        public Double getY() {
            return y;
        }
        
        public void setY(Double y) {
            this.y = y;
        }
        
        @Override
        public String toString() {
            return "Position{" +
                    "x=" + x +
                    ", y=" + y +
                    '}';
        }
    }
}