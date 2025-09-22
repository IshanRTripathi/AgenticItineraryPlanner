package com.tripplanner.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

/**
 * DiffItem DTO for individual items in an ItineraryDiff.
 */
public class DiffItem {
    
    @NotBlank
    @JsonProperty("id")
    private String id; // Node ID
    
    @JsonProperty("day")
    private Integer day; // Day number
    
    @JsonProperty("fields")
    private List<String> fields; // Fields that were updated
    
    public DiffItem() {}
    
    public DiffItem(String id, Integer day) {
        this.id = id;
        this.day = day;
    }
    
    public DiffItem(String id, Integer day, List<String> fields) {
        this.id = id;
        this.day = day;
        this.fields = fields;
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
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
    
    @Override
    public String toString() {
        return "DiffItem{" +
                "id='" + id + '\'' +
                ", day=" + day +
                ", fields=" + fields +
                '}';
    }
}
