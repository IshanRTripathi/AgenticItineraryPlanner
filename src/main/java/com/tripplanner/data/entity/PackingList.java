package com.tripplanner.data.entity;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.firestore.annotation.PropertyName;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Packing list entity for travel itineraries.
 * Stored in Firestore collection: packingLists/{id}
 */
public class PackingList {
    
    @DocumentId
    private String id;
    
    @NotBlank
    @PropertyName("itineraryId")
    private String itineraryId;
    
    @NotBlank
    @PropertyName("userId")
    private String userId;
    
    @PropertyName("destination")
    private String destination;
    
    @PropertyName("climate")
    private String climate;
    
    @PropertyName("season")
    private String season;
    
    @PropertyName("activities")
    private List<String> activities;
    
    @PropertyName("duration")
    private int duration; // days
    
    @PropertyName("items")
    private List<PackingItem> items;
    
    @PropertyName("createdAt")
    private Instant createdAt;
    
    @PropertyName("updatedAt")
    private Instant updatedAt;
    
    @PropertyName("expiresAt")
    private Instant expiresAt; // TTL for cleanup
    
    public PackingList() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        // Set expiry to 30 days from creation
        this.expiresAt = Instant.now().plusSeconds(30 * 24 * 60 * 60);
    }
    
    public void updateTimestamp() {
        this.updatedAt = Instant.now();
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getItineraryId() {
        return itineraryId;
    }
    
    public void setItineraryId(String itineraryId) {
        this.itineraryId = itineraryId;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getDestination() {
        return destination;
    }
    
    public void setDestination(String destination) {
        this.destination = destination;
    }
    
    public String getClimate() {
        return climate;
    }
    
    public void setClimate(String climate) {
        this.climate = climate;
    }
    
    public String getSeason() {
        return season;
    }
    
    public void setSeason(String season) {
        this.season = season;
    }
    
    public List<String> getActivities() {
        return activities;
    }
    
    public void setActivities(List<String> activities) {
        this.activities = activities;
    }
    
    public int getDuration() {
        return duration;
    }
    
    public void setDuration(int duration) {
        this.duration = duration;
    }
    
    public List<PackingItem> getItems() {
        return items;
    }
    
    public void setItems(List<PackingItem> items) {
        this.items = items;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
    
    public Instant getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public Instant getExpiresAt() {
        return expiresAt;
    }
    
    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PackingList that = (PackingList) o;
        return Objects.equals(id, that.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "PackingList{" +
                "id='" + id + '\'' +
                ", itineraryId='" + itineraryId + '\'' +
                ", destination='" + destination + '\'' +
                ", duration=" + duration +
                '}';
    }
    
    // Nested class
    public static class PackingItem {
        @PropertyName("name")
        private String name;
        
        @PropertyName("quantity")
        private int quantity = 1;
        
        @PropertyName("group")
        private String group; // clothing, electronics, documents, toiletries, etc.
        
        @PropertyName("essential")
        private boolean essential = false;
        
        @PropertyName("notes")
        private String notes;
        
        @PropertyName("packed")
        private boolean packed = false;
        
        public PackingItem() {}
        
        public PackingItem(String name, int quantity, String group) {
            this.name = name;
            this.quantity = quantity;
            this.group = group;
        }
        
        // Getters and Setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
        public String getGroup() { return group; }
        public void setGroup(String group) { this.group = group; }
        public boolean isEssential() { return essential; }
        public void setEssential(boolean essential) { this.essential = essential; }
        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }
        public boolean isPacked() { return packed; }
        public void setPacked(boolean packed) { this.packed = packed; }
    }
}

