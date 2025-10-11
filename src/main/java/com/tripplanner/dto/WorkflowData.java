package com.tripplanner.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;

import java.util.List;

/**
 * Workflow-specific data for storing workflow view layout and settings.
 * Contains nodes, edges, layout information, and workflow-specific settings.
 */
public class WorkflowData {
    
    @Valid
    @JsonProperty("nodes")
    private List<WorkflowNode> nodes;
    
    @Valid
    @JsonProperty("edges")
    private List<WorkflowEdge> edges;
    
    @Valid
    @JsonProperty("layout")
    private WorkflowLayout layout;
    
    @Valid
    @JsonProperty("settings")
    private WorkflowSettings settings;
    
    public WorkflowData() {}
    
    public WorkflowData(List<WorkflowNode> nodes, List<WorkflowEdge> edges, 
                       WorkflowLayout layout, WorkflowSettings settings) {
        this.nodes = nodes;
        this.edges = edges;
        this.layout = layout;
        this.settings = settings;
    }
    
    // Getters and Setters
    public List<WorkflowNode> getNodes() {
        return nodes;
    }
    
    public void setNodes(List<WorkflowNode> nodes) {
        this.nodes = nodes;
    }
    
    public List<WorkflowEdge> getEdges() {
        return edges;
    }
    
    public void setEdges(List<WorkflowEdge> edges) {
        this.edges = edges;
    }
    
    public WorkflowLayout getLayout() {
        return layout;
    }
    
    public void setLayout(WorkflowLayout layout) {
        this.layout = layout;
    }
    
    public WorkflowSettings getSettings() {
        return settings;
    }
    
    public void setSettings(WorkflowSettings settings) {
        this.settings = settings;
    }
    
    @Override
    public String toString() {
        return "WorkflowData{" +
                "nodes=" + nodes +
                ", edges=" + edges +
                ", layout=" + layout +
                ", settings=" + settings +
                '}';
    }
}