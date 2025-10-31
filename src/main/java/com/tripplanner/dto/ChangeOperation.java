package com.tripplanner.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;

/**
 * ChangeOperation DTO for individual operations in a ChangeSet.
 * Supports move, insert, and delete operations.
 */
public class ChangeOperation {
    
    @NotBlank
    @JsonProperty("op")
    private String op; // "move", "insert", "delete"
    
    @JsonProperty("id")
    private String id; // Node ID for move/delete operations
    
    @JsonProperty("startTime")
    private Long startTime; // For move operations (milliseconds since epoch)
    
    @JsonProperty("endTime")
    private Long endTime; // For move operations (milliseconds since epoch)
    
    @JsonProperty("after")
    private String after; // Node ID to insert after
    
    @JsonProperty("node")
    private NormalizedNode node; // Node to insert
    
    @JsonProperty("position")
    private Integer position; // Position for insert operations
    
    @JsonProperty("nodeIds")
    private java.util.List<String> nodeIds; // List of node IDs for reorder operations
    
    public ChangeOperation() {}
    
    public ChangeOperation(String op, String id) {
        this.op = op;
        this.id = id;
    }
    
    public ChangeOperation(String op, String id, Long startTime, Long endTime) {
        this.op = op;
        this.id = id;
        this.startTime = startTime;
        this.endTime = endTime;
    }
    
    public ChangeOperation(String op, String id, Instant startTime, Instant endTime) {
        this.op = op;
        this.id = id;
        this.startTime = startTime != null ? startTime.toEpochMilli() : null;
        this.endTime = endTime != null ? endTime.toEpochMilli() : null;
    }
    
    public ChangeOperation(String op, String after, NormalizedNode node) {
        this.op = op;
        this.after = after;
        this.node = node;
    }
    
    // Getters and Setters
    public String getOp() {
        return op;
    }
    
    public void setOp(String op) {
        this.op = op;
    }
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public Long getStartTime() {
        return startTime;
    }
    
    public void setStartTime(Long startTime) {
        this.startTime = startTime;
    }
    
    public Long getEndTime() {
        return endTime;
    }
    
    public void setEndTime(Long endTime) {
        this.endTime = endTime;
    }
    
    // Helper methods to get Instant objects
    @JsonIgnore
    public Instant getStartTimeAsInstant() {
        return startTime != null ? Instant.ofEpochMilli(startTime) : null;
    }
    
    @JsonIgnore
    public Instant getEndTimeAsInstant() {
        return endTime != null ? Instant.ofEpochMilli(endTime) : null;
    }
    
    public String getAfter() {
        return after;
    }
    
    public void setAfter(String after) {
        this.after = after;
    }
    
    public NormalizedNode getNode() {
        return node;
    }
    
    public void setNode(NormalizedNode node) {
        this.node = node;
    }
    
    public Integer getPosition() {
        return position;
    }
    
    public void setPosition(Integer position) {
        this.position = position;
    }
    
    public java.util.List<String> getNodeIds() {
        return nodeIds;
    }
    
    public void setNodeIds(java.util.List<String> nodeIds) {
        this.nodeIds = nodeIds;
    }
    
    @Override
    public String toString() {
        return "ChangeOperation{" +
                "op='" + op + '\'' +
                ", id='" + id + '\'' +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", after='" + after + '\'' +
                ", node=" + node +
                ", position=" + position +
                ", nodeIds=" + nodeIds +
                '}';
    }
}
