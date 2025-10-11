package com.tripplanner.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

/**
 * DiffItem DTO for individual items in an ItineraryDiff.
 */
public class DiffItem {
    
    @NotBlank
    @JsonProperty("nodeId")
    private String nodeId; // Node ID
    
    @JsonProperty("day")
    private Integer day; // Day number
    
    @JsonProperty("fields")
    private List<String> fields; // Fields that were updated
    
    @JsonProperty("title")
    private String title; // Node title for display
    
    public DiffItem() {}
    
    public DiffItem(String nodeId, Integer day) {
        this.nodeId = nodeId;
        this.day = day;
    }
    
    public DiffItem(String nodeId, Integer day, List<String> fields) {
        this.nodeId = nodeId;
        this.day = day;
        this.fields = fields;
    }
    
    public DiffItem(String nodeId, Integer day, List<String> fields, String title) {
        this.nodeId = nodeId;
        this.day = day;
        this.fields = fields;
        this.title = title;
    }
    
    // Getters and Setters
    public String getNodeId() {
        return nodeId;
    }
    
    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }
    
    public Integer getDay() {
        return day;
    }
    
    public void setDay(Integer day) {
        this.day = day;
    }
    
    public List<String> getFields() {
        return fields;
    }
    
    public void setFields(List<String> fields) {
        this.fields = fields;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    @Override
    public String toString() {
        return "DiffItem{" +
                "nodeId='" + nodeId + '\'' +
                ", day=" + day +
                ", fields=" + fields +
                ", title='" + title + '\'' +
                '}';
    }
}

