package com.tripplanner.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

/**
 * Normalized day structure with nodes and edges.
 */
public class NormalizedDay {
    
    @Positive
    @JsonProperty("dayNumber")
    private Integer dayNumber;
    
    @JsonProperty("date")
    private String date; // ISO date string format
    
    @JsonProperty("location")
    private String location;
    
    @JsonProperty("warnings")
    private List<String> warnings;
    
    @JsonProperty("notes")
    private String notes;
    
    @JsonProperty("summary")
    private String summary;
    
    @JsonProperty("pace")
    private String pace; // "relaxed", "balanced", "intense"
    
    @JsonProperty("totalDistance")
    private Double totalDistance;
    
    @JsonProperty("totalCost")
    private Double totalCost;
    
    @JsonProperty("totalDuration")
    private Double totalDuration;
    
    @JsonProperty("timeWindowStart")
    private String timeWindowStart; // e.g., "09:00"
    
    @JsonProperty("timeWindowEnd")
    private String timeWindowEnd; // e.g., "18:00"
    
    @JsonProperty("timeZone")
    private String timeZone; // e.g., "UTC", "IST"
    
    @Valid
    @NotNull
    @JsonProperty("nodes")
    private List<NormalizedNode> nodes;
    
    @Valid
    @JsonProperty("edges")
    private List<Edge> edges;
    
    public NormalizedDay() {}
    
    public NormalizedDay(Integer dayNumber, String date, String location) {
        this.dayNumber = dayNumber;
        this.date = date;
        this.location = location;
        this.totalDistance = 0.0;
        this.totalCost = 0.0;
        this.totalDuration = 0.0;
    }
    
    // Getters and Setters
    public Integer getDayNumber() {
        return dayNumber;
    }
    
    public void setDayNumber(Integer dayNumber) {
        this.dayNumber = dayNumber;
    }
    
    public String getDate() {
        return date;
    }
    
    public void setDate(String date) {
        this.date = date;
    }
    
    public String getLocation() {
        return location;
    }
    
    public void setLocation(String location) {
        this.location = location;
    }
    
    public List<String> getWarnings() {
        return warnings;
    }
    
    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
    }
    
    public String getSummary() {
        return summary;
    }
    
    public void setSummary(String summary) {
        this.summary = summary;
    }
    
    public String getPace() {
        return pace;
    }
    
    public void setPace(String pace) {
        this.pace = pace;
    }
    
    public Double getTotalDistance() {
        return totalDistance;
    }
    
    public void setTotalDistance(Double totalDistance) {
        this.totalDistance = totalDistance;
    }
    
    public Double getTotalCost() {
        return totalCost;
    }
    
    public void setTotalCost(Double totalCost) {
        this.totalCost = totalCost;
    }
    
    public Double getTotalDuration() {
        return totalDuration;
    }
    
    public void setTotalDuration(Double totalDuration) {
        this.totalDuration = totalDuration;
    }
    
    public String getTimeWindowStart() {
        return timeWindowStart;
    }
    
    public void setTimeWindowStart(String timeWindowStart) {
        this.timeWindowStart = timeWindowStart;
    }
    
    public String getTimeWindowEnd() {
        return timeWindowEnd;
    }
    
    public void setTimeWindowEnd(String timeWindowEnd) {
        this.timeWindowEnd = timeWindowEnd;
    }
    
    public String getTimeZone() {
        return timeZone;
    }
    
    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }
    
    public List<NormalizedNode> getNodes() {
        return nodes;
    }
    
    public void setNodes(List<NormalizedNode> nodes) {
        this.nodes = nodes;
    }
    
    public List<Edge> getEdges() {
        return edges;
    }
    
    public void setEdges(List<Edge> edges) {
        this.edges = edges;
    }
    
    @Override
    public String toString() {
        return "NormalizedDay{" +
                "dayNumber=" + dayNumber +
                ", date=" + date +
                ", location='" + location + '\'' +
                ", warnings=" + warnings +
                ", notes='" + notes + '\'' +
                ", pace='" + pace + '\'' +
                ", totalDistance=" + totalDistance +
                ", totalCost=" + totalCost +
                ", totalDuration=" + totalDuration +
                ", timeWindowStart='" + timeWindowStart + '\'' +
                ", timeWindowEnd='" + timeWindowEnd + '\'' +
                ", timeZone='" + timeZone + '\'' +
                ", nodes=" + nodes +
                ", edges=" + edges +
                '}';
    }
}
