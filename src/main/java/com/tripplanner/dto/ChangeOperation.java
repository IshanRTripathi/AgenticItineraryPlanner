package com.tripplanner.dto;

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
    private Instant startTime; // For move operations
    
    @JsonProperty("endTime")
    private Instant endTime; // For move operations
    
    @JsonProperty("after")
    private String after; // Node ID to insert after
    
    @JsonProperty("node")
    private NormalizedNode node; // Node to insert
    
    public ChangeOperation() {}
    
    public ChangeOperation(String op, String id) {
        this.op = op;
        this.id = id;
    }
    
    public ChangeOperation(String op, String id, Instant startTime, Instant endTime) {
        this.op = op;
        this.id = id;
        this.startTime = startTime;
        this.endTime = endTime;
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
    
    public Instant getStartTime() {
        return startTime;
    }
    
    public void setStartTime(Instant startTime) {
        this.startTime = startTime;
    }
    
    public Instant getEndTime() {
        return endTime;
    }
    
    public void setEndTime(Instant endTime) {
        this.endTime = endTime;
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
    
    @Override
    public String toString() {
        return "ChangeOperation{" +
                "op='" + op + '\'' +
                ", id='" + id + '\'' +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", after='" + after + '\'' +
                ", node=" + node +
                '}';
    }
}
