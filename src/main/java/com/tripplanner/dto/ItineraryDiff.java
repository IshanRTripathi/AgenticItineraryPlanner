package com.tripplanner.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;

import java.util.List;

/**
 * ItineraryDiff DTO for previewing changes before applying them.
 */
public class ItineraryDiff {
    
    @Valid
    @JsonProperty("added")
    private List<DiffItem> added;
    
    @Valid
    @JsonProperty("removed")
    private List<DiffItem> removed;
    
    @Valid
    @JsonProperty("updated")
    private List<DiffItem> updated;
    
    public ItineraryDiff() {}
    
    public ItineraryDiff(List<DiffItem> added, List<DiffItem> removed, List<DiffItem> updated) {
        this.added = added;
        this.removed = removed;
        this.updated = updated;
    }
    
    // Getters and Setters
    public List<DiffItem> getAdded() {
        return added;
    }
    
    public void setAdded(List<DiffItem> added) {
        this.added = added;
    }
    
    public List<DiffItem> getRemoved() {
        return removed;
    }
    
    public void setRemoved(List<DiffItem> removed) {
        this.removed = removed;
    }
    
    public List<DiffItem> getUpdated() {
        return updated;
    }
    
    public void setUpdated(List<DiffItem> updated) {
        this.updated = updated;
    }
    
    @Override
    public String toString() {
        return "ItineraryDiff{" +
                "added=" + added +
                ", removed=" + removed +
                ", updated=" + updated +
                '}';
    }
}
