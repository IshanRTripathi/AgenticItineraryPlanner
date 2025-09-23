package com.tripplanner.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

/**
 * Edge between nodes in a day.
 */
public class Edge {
    
    @NotBlank
    @JsonProperty("from")
    private String from; // node ID
    
    @NotBlank
    @JsonProperty("to")
    private String to; // node ID
    
    public Edge() {}
    
    public Edge(String from, String to) {
        this.from = from;
        this.to = to;
    }
    
    // Getters and Setters
    public String getFrom() {
        return from;
    }
    
    public void setFrom(String from) {
        this.from = from;
    }
    
    public String getTo() {
        return to;
    }
    
    public void setTo(String to) {
        this.to = to;
    }
    
    @Override
    public String toString() {
        return "Edge{" +
                "from='" + from + '\'' +
                ", to='" + to + '\'' +
                '}';
    }
}
