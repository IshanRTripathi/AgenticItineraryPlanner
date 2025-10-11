package com.tripplanner.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

/**
 * Standardized request for place ENRICHMENT operations.
 * Provides traceability, idempotency, and context for ENRICHMENT tasks.
 */
public class EnrichmentRequest {
    
    @JsonProperty("traceId")
    private String traceId; // For request tracing and correlation
    
    @JsonProperty("idempotencyKey")
    private String idempotencyKey; // For duplicate detection and caching
    
    @JsonProperty("requestType")
    private EnrichmentType requestType; // Type of ENRICHMENT requested
    
    @JsonProperty("targetNodeId")
    private String targetNodeId; // Node to be enriched
    
    @JsonProperty("itineraryId")
    private String itineraryId; // Associated itinerary
    
    @JsonProperty("userId")
    private String userId; // User making the request
    
    @JsonProperty("requestedFields")
    private List<String> requestedFields; // Specific fields to enrich
    
    @JsonProperty("context")
    private Map<String, Object> context; // Additional context for ENRICHMENT
    
    @JsonProperty("priority")
    private EnrichmentPriority priority; // Request priority
    
    @JsonProperty("timeout")
    private Long timeoutMs; // Request timeout in milliseconds
    
    @JsonProperty("createdAt")
    private Long createdAt; // Request creation timestamp
    
    @JsonProperty("metadata")
    private Map<String, Object> metadata; // Additional metadata
    
    public EnrichmentRequest() {
        this.requestedFields = new ArrayList<>();
        this.context = new HashMap<>();
        this.metadata = new HashMap<>();
        this.priority = EnrichmentPriority.MEDIUM;
        this.timeoutMs = 30000L; // Default 30 seconds
        this.createdAt = System.currentTimeMillis();
    }
    
    public EnrichmentRequest(String traceId, String idempotencyKey, EnrichmentType requestType,
                           String targetNodeId, String itineraryId, String userId) {
        this();
        this.traceId = traceId;
        this.idempotencyKey = idempotencyKey;
        this.requestType = requestType;
        this.targetNodeId = targetNodeId;
        this.itineraryId = itineraryId;
        this.userId = userId;
    }
    
    /**
     * Create a place details ENRICHMENT request.
     */
    public static EnrichmentRequest forPlaceDetails(String traceId, String nodeId, 
                                                   String itineraryId, String userId) {
        EnrichmentRequest request = new EnrichmentRequest(
            traceId, generateIdempotencyKey("details", nodeId), 
            EnrichmentType.PLACE_DETAILS, nodeId, itineraryId, userId);
        
        request.addRequestedField("photos");
        request.addRequestedField("reviews");
        request.addRequestedField("rating");
        request.addRequestedField("openingHours");
        
        return request;
    }
    
    /**
     * Create a place validation ENRICHMENT request.
     */
    public static EnrichmentRequest forPlaceValidation(String traceId, String nodeId,
                                                      String itineraryId, String userId) {
        EnrichmentRequest request = new EnrichmentRequest(
            traceId, generateIdempotencyKey("validation", nodeId),
            EnrichmentType.PLACE_VALIDATION, nodeId, itineraryId, userId);
        
        request.addRequestedField("exists");
        request.addRequestedField("coordinates");
        request.addRequestedField("address");
        request.setPriority(EnrichmentPriority.HIGH);
        
        return request;
    }
    
    /**
     * Create a timing optimization ENRICHMENT request.
     */
    public static EnrichmentRequest forTimingOptimization(String traceId, String nodeId,
                                                         String itineraryId, String userId) {
        EnrichmentRequest request = new EnrichmentRequest(
            traceId, generateIdempotencyKey("timing", nodeId),
            EnrichmentType.TIMING_OPTIMIZATION, nodeId, itineraryId, userId);
        
        request.addRequestedField("optimalTiming");
        request.addRequestedField("crowdPatterns");
        request.addRequestedField("waitTimes");
        
        return request;
    }
    
    /**
     * Add a requested field for ENRICHMENT.
     */
    public void addRequestedField(String field) {
        if (field != null && !requestedFields.contains(field)) {
            requestedFields.add(field);
        }
    }
    
    /**
     * Add context information.
     */
    public void addContext(String key, Object value) {
        if (key != null && value != null) {
            context.put(key, value);
        }
    }
    
    /**
     * Add metadata.
     */
    public void addMetadata(String key, Object value) {
        if (key != null && value != null) {
            metadata.put(key, value);
        }
    }
    
    /**
     * Check if request has timed out.
     */
    public boolean hasTimedOut() {
        return System.currentTimeMillis() - createdAt > timeoutMs;
    }
    
    /**
     * Get remaining timeout in milliseconds.
     */
    public long getRemainingTimeoutMs() {
        return Math.max(0, timeoutMs - (System.currentTimeMillis() - createdAt));
    }
    
    /**
     * Generate idempotency key for request type and target.
     */
    private static String generateIdempotencyKey(String operation, String nodeId) {
        return String.format("enrich_%s_%s_%d", operation, nodeId, System.currentTimeMillis() / 60000);
    }
    
    // Getters and Setters
    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }
    
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    
    public EnrichmentType getRequestType() { return requestType; }
    public void setRequestType(EnrichmentType requestType) { this.requestType = requestType; }
    
    public String getTargetNodeId() { return targetNodeId; }
    public void setTargetNodeId(String targetNodeId) { this.targetNodeId = targetNodeId; }
    
    public String getItineraryId() { return itineraryId; }
    public void setItineraryId(String itineraryId) { this.itineraryId = itineraryId; }
    
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public List<String> getRequestedFields() { return requestedFields; }
    public void setRequestedFields(List<String> requestedFields) { 
        this.requestedFields = requestedFields != null ? requestedFields : new ArrayList<>(); 
    }
    
    public Map<String, Object> getContext() { return context; }
    public void setContext(Map<String, Object> context) { 
        this.context = context != null ? context : new HashMap<>(); 
    }
    
    public EnrichmentPriority getPriority() { return priority; }
    public void setPriority(EnrichmentPriority priority) { this.priority = priority; }
    
    public Long getTimeoutMs() { return timeoutMs; }
    public void setTimeoutMs(Long timeoutMs) { this.timeoutMs = timeoutMs; }
    
    public Long getCreatedAt() { return createdAt; }
    public void setCreatedAt(Long createdAt) { this.createdAt = createdAt; }
    
    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { 
        this.metadata = metadata != null ? metadata : new HashMap<>(); 
    }
    
    @Override
    public String toString() {
        return "EnrichmentRequest{" +
                "traceId='" + traceId + '\'' +
                ", requestType=" + requestType +
                ", targetNodeId='" + targetNodeId + '\'' +
                ", priority=" + priority +
                ", requestedFields=" + requestedFields.size() +
                '}';
    }
    
    /**
     * Types of ENRICHMENT operations.
     */
    public enum EnrichmentType {
        PLACE_DETAILS,      // Enrich with photos, reviews, ratings
        PLACE_VALIDATION,   // Validate place exists and details are correct
        TIMING_OPTIMIZATION, // Optimize timing based on crowd patterns
        CANONICAL_MAPPING,  // Map to canonical place ID
        CONTENT_ENHANCEMENT // Enhance descriptions and content
    }
    
    /**
     * Priority levels for ENRICHMENT requests.
     */
    public enum EnrichmentPriority {
        LOW(1),
        MEDIUM(2),
        HIGH(3),
        URGENT(4);
        
        private final int level;
        
        EnrichmentPriority(int level) {
            this.level = level;
        }
        
        public int getLevel() { return level; }
        
        public boolean isHigherThan(EnrichmentPriority other) {
            return this.level > other.level;
        }
    }
}