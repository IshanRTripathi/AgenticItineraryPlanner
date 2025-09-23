package com.tripplanner.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.List;

/**
 * Normalized node structure for all types (attraction, meal, hotel, transit).
 * Single schema for all node types as per MVP contract.
 */
public class NormalizedNode {
    
    @NotBlank
    @JsonProperty("id")
    private String id;
    
    @NotBlank
    @JsonProperty("type")
    private String type; // "attraction", "meal", "hotel", "transit"
    
    @NotBlank
    @JsonProperty("title")
    private String title;
    
    @Valid
    @JsonProperty("location")
    private NodeLocation location;
    
    @Valid
    @JsonProperty("timing")
    private NodeTiming timing;
    
    @Valid
    @JsonProperty("cost")
    private NodeCost cost;
    
    @Valid
    @JsonProperty("details")
    private NodeDetails details;
    
    @JsonProperty("labels")
    private List<String> labels;
    
    @Valid
    @JsonProperty("tips")
    private NodeTips tips;
    
    @Valid
    @JsonProperty("links")
    private NodeLinks links;
    
    @Valid
    @JsonProperty("transit")
    private TransitInfo transit;
    
    @JsonProperty("locked")
    private Boolean locked = false;
    
    @JsonProperty("bookingRef")
    private String bookingRef;
    
    @JsonProperty("status")
    private String status = "planned"; // "planned", "in_progress", "skipped", "cancelled", "completed"
    
    @JsonProperty("updatedBy")
    private String updatedBy; // "agent" or "user"
    
    @JsonProperty("updatedAt")
    private Instant updatedAt;
    
    public NormalizedNode() {
        this.updatedAt = Instant.now();
    }
    
    public NormalizedNode(String id, String type, String title) {
        this();
        this.id = id;
        this.type = type;
        this.title = title;
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public NodeLocation getLocation() {
        return location;
    }
    
    public void setLocation(NodeLocation location) {
        this.location = location;
    }
    
    public NodeTiming getTiming() {
        return timing;
    }
    
    public void setTiming(NodeTiming timing) {
        this.timing = timing;
    }
    
    public NodeCost getCost() {
        return cost;
    }
    
    public void setCost(NodeCost cost) {
        this.cost = cost;
    }
    
    public NodeDetails getDetails() {
        return details;
    }
    
    public void setDetails(NodeDetails details) {
        this.details = details;
    }
    
    public List<String> getLabels() {
        return labels;
    }
    
    public void setLabels(List<String> labels) {
        this.labels = labels;
    }
    
    public NodeTips getTips() {
        return tips;
    }
    
    public void setTips(NodeTips tips) {
        this.tips = tips;
    }
    
    public NodeLinks getLinks() {
        return links;
    }
    
    public void setLinks(NodeLinks links) {
        this.links = links;
    }
    
    public TransitInfo getTransit() {
        return transit;
    }
    
    public void setTransit(TransitInfo transit) {
        this.transit = transit;
    }
    
    public Boolean getLocked() {
        return locked;
    }
    
    public void setLocked(Boolean locked) {
        this.locked = locked;
    }
    
    public String getBookingRef() {
        return bookingRef;
    }
    
    public void setBookingRef(String bookingRef) {
        this.bookingRef = bookingRef;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getUpdatedBy() {
        return updatedBy;
    }
    
    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }
    
    public Instant getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    // Helper methods for status management
    public boolean isPlanned() {
        return "planned".equals(status);
    }
    
    public boolean isInProgress() {
        return "in_progress".equals(status);
    }
    
    public boolean isSkipped() {
        return "skipped".equals(status);
    }
    
    public boolean isCancelled() {
        return "cancelled".equals(status);
    }
    
    public boolean isCompleted() {
        return "completed".equals(status);
    }
    
    public boolean isBooked() {
        return bookingRef != null && !bookingRef.trim().isEmpty();
    }
    
    public void markAsUpdated(String updatedBy) {
        this.updatedBy = updatedBy;
        this.updatedAt = Instant.now();
    }
    
    public boolean canTransitionTo(String newStatus) {
        if (status == null || newStatus == null) return false;
        
        // Define valid status transitions
        return switch (status) {
            case "planned" -> List.of("in_progress", "skipped", "cancelled").contains(newStatus);
            case "in_progress" -> List.of("completed", "skipped", "cancelled").contains(newStatus);
            case "skipped", "cancelled" -> List.of("planned", "in_progress").contains(newStatus);
            case "completed" -> List.of("planned", "in_progress").contains(newStatus);
            default -> false;
        };
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NormalizedNode that = (NormalizedNode) o;
        return id != null ? id.equals(that.id) : that.id == null;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "NormalizedNode{" +
                "id='" + id + '\'' +
                ", type='" + type + '\'' +
                ", title='" + title + '\'' +
                ", location=" + location +
                ", timing=" + timing +
                ", cost=" + cost +
                ", details=" + details +
                ", labels=" + labels +
                ", tips=" + tips +
                ", links=" + links +
                ", transit=" + transit +
                ", locked=" + locked +
                ", bookingRef='" + bookingRef + '\'' +
                ", status='" + status + '\'' +
                ", updatedBy='" + updatedBy + '\'' +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
