package com.tripplanner.api.dto;

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
    private String date; // ISO date string for frontend compatibility
    
    @JsonProperty("location")
    private String location;
    
    @Valid
    @JsonProperty("pacing")
    private Pacing pacing;
    
    @JsonProperty("warnings")
    private List<String> warnings;
    
    @JsonProperty("notes")
    private String notes;
    
    @Valid
    @JsonProperty("totals")
    private DayTotals totals;
    
    @Valid
    @JsonProperty("timeWindow")
    private TimeWindow timeWindow;
    
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
    
    public Pacing getPacing() {
        return pacing;
    }
    
    public void setPacing(Pacing pacing) {
        this.pacing = pacing;
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
    
    public DayTotals getTotals() {
        return totals;
    }
    
    public void setTotals(DayTotals totals) {
        this.totals = totals;
    }
    
    public TimeWindow getTimeWindow() {
        return timeWindow;
    }
    
    public void setTimeWindow(TimeWindow timeWindow) {
        this.timeWindow = timeWindow;
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
                ", pacing=" + pacing +
                ", warnings=" + warnings +
                ", notes='" + notes + '\'' +
                ", totals=" + totals +
                ", timeWindow=" + timeWindow +
                ", nodes=" + nodes +
                ", edges=" + edges +
                '}';
    }
}
