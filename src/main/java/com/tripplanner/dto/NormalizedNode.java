package com.tripplanner.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tripplanner.enums.ProcessingState;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.ArrayList;
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
    private Long updatedAt;
    
    // Agent data for unified structure
    @Valid
    @JsonProperty("agentData")
    private java.util.Map<String, Object> agentData;
    
    // NEW: Explicit metadata layer (replaces string parsing)
    @Valid
    @JsonProperty("metadata")
    private NodeMetadata metadata;
    
    // NEW: Processing state tracking
    @JsonProperty("processingState")
    private ProcessingState processingState;
    
    // NEW: Validation results
    @JsonProperty("validationErrors")
    private List<String> validationErrors;
    
    // NEW: Error tracking
    @JsonProperty("lastError")
    private String lastError;
    
    @JsonProperty("retryCount")
    private Integer retryCount = 0;
    
    // NEW: Processing history
    @JsonProperty("processedBy")
    private List<String> processedBy;
    
    public NormalizedNode() {
        this.updatedAt = System.currentTimeMillis();
        this.agentData = new java.util.HashMap<>();
        this.processingState = ProcessingState.CREATED;
        this.validationErrors = new ArrayList<>();
        this.processedBy = new ArrayList<>();
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
    
    public Long getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(Long updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public java.util.Map<String, Object> getAgentData() {
        return agentData;
    }
    
    public void setAgentData(java.util.Map<String, Object> agentData) {
        this.agentData = agentData;
    }
    
    public NodeMetadata getMetadata() {
        return metadata;
    }
    
    public void setMetadata(NodeMetadata metadata) {
        this.metadata = metadata;
    }
    
    public ProcessingState getProcessingState() {
        return processingState;
    }
    
    public void setProcessingState(ProcessingState processingState) {
        this.processingState = processingState;
    }
    
    public List<String> getValidationErrors() {
        return validationErrors;
    }
    
    public void setValidationErrors(List<String> validationErrors) {
        this.validationErrors = validationErrors;
    }
    
    public void addValidationError(String error) {
        if (this.validationErrors == null) {
            this.validationErrors = new ArrayList<>();
        }
        this.validationErrors.add(error);
    }
    
    public String getLastError() {
        return lastError;
    }
    
    public void setLastError(String lastError) {
        this.lastError = lastError;
    }
    
    public Integer getRetryCount() {
        return retryCount;
    }
    
    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }
    
    public void incrementRetryCount() {
        this.retryCount = (this.retryCount == null ? 0 : this.retryCount) + 1;
    }
    
    public List<String> getProcessedBy() {
        return processedBy;
    }
    
    public void setProcessedBy(List<String> processedBy) {
        this.processedBy = processedBy;
    }
    
    public void addProcessedBy(String agentName) {
        if (this.processedBy == null) {
            this.processedBy = new ArrayList<>();
        }
        if (!this.processedBy.contains(agentName)) {
            this.processedBy.add(agentName);
        }
    }
    
    // Helper method to get Instant object
    @JsonIgnore
    public Instant getUpdatedAtAsInstant() {
        return updatedAt != null ? Instant.ofEpochMilli(updatedAt) : null;
    }
    
    // Helper methods for status management
    @JsonIgnore
    public boolean isPlanned() {
        return "planned".equals(status);
    }
    
    @JsonIgnore
    public boolean isInProgress() {
        return "in_progress".equals(status);
    }
    
    @JsonIgnore
    public boolean isSkipped() {
        return "skipped".equals(status);
    }
    
    @JsonIgnore
    public boolean isCancelled() {
        return "cancelled".equals(status);
    }
    
    @JsonIgnore
    public boolean isCompleted() {
        return "completed".equals(status);
    }
    
    @JsonIgnore
    public boolean isBooked() {
        return bookingRef != null && !bookingRef.trim().isEmpty();
    }
    
    public void markAsUpdated(String updatedBy) {
        this.updatedBy = updatedBy;
        this.updatedAt = System.currentTimeMillis();
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
