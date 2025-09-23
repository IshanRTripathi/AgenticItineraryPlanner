package com.tripplanner.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * ChangeSet DTO for managing itinerary changes.
 * Contains operations to be applied to an itinerary.
 */
public class ChangeSet {
    
    @NotBlank
    @JsonProperty("scope")
    private String scope; // "trip" or "day"
    
    @JsonProperty("day")
    private Integer day; // Required when scope is "day"
    
    @Valid
    @NotNull
    @JsonProperty("ops")
    private List<ChangeOperation> ops;
    
    @Valid
    @JsonProperty("preferences")
    private ChangePreferences preferences;
    
    public ChangeSet() {}
    
    public ChangeSet(String scope, Integer day, List<ChangeOperation> ops) {
        this.scope = scope;
        this.day = day;
        this.ops = ops;
    }
    
    // Getters and Setters
    public String getScope() {
        return scope;
    }
    
    public void setScope(String scope) {
        this.scope = scope;
    }
    
    public Integer getDay() {
        return day;
    }
    
    public void setDay(Integer day) {
        this.day = day;
    }
    
    public List<ChangeOperation> getOps() {
        return ops;
    }
    
    public void setOps(List<ChangeOperation> ops) {
        this.ops = ops;
    }
    
    public ChangePreferences getPreferences() {
        return preferences;
    }
    
    public void setPreferences(ChangePreferences preferences) {
        this.preferences = preferences;
    }
    
    @Override
    public String toString() {
        return "ChangeSet{" +
                "scope='" + scope + '\'' +
                ", day=" + day +
                ", ops=" + ops +
                ", preferences=" + preferences +
                '}';
    }
}
