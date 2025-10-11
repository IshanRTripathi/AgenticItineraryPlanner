package com.tripplanner.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

/**
 * Standardized response for place ENRICHMENT operations.
 * Provides status, confidence scoring, and enriched data.
 */
public class EnrichmentResponse {
    
    @JsonProperty("traceId")
    private String traceId; // Correlation with request
    
    @JsonProperty("idempotencyKey")
    private String idempotencyKey; // For caching and deduplication
    
    @JsonProperty("status")
    private EnrichmentStatus status; // Response status
    
    @JsonProperty("confidence")
    private Double confidence; // Confidence in ENRICHMENT quality (0.0-1.0)
    
    @JsonProperty("enrichedData")
    private Map<String, Object> enrichedData; // The actual enriched data
    
    @JsonProperty("partialResults")
    private List<PartialResult> partialResults; // Partial ENRICHMENT results
    
    @JsonProperty("errors")
    private List<EnrichmentError> errors; // Any errors encountered
    
    @JsonProperty("warnings")
    private List<String> warnings; // Non-fatal warnings
    
    @JsonProperty("processingTimeMs")
    private Long processingTimeMs; // Time taken to process
    
    @JsonProperty("cacheHit")
    private Boolean cacheHit; // Whether result was cached
    
    @JsonProperty("dataSource")
    private String dataSource; // Source of ENRICHMENT data
    
    @JsonProperty("completedAt")
    private Long completedAt; // Response completion timestamp
    
    @JsonProperty("metadata")
    private Map<String, Object> metadata; // Additional response metadata
    
    public EnrichmentResponse() {
        this.enrichedData = new HashMap<>();
        this.partialResults = new ArrayList<>();
        this.errors = new ArrayList<>();
        this.warnings = new ArrayList<>();
        this.metadata = new HashMap<>();
        this.confidence = 1.0;
        this.cacheHit = false;
        this.completedAt = System.currentTimeMillis();
    }
    
    public EnrichmentResponse(String traceId, String idempotencyKey, EnrichmentStatus status) {
        this();
        this.traceId = traceId;
        this.idempotencyKey = idempotencyKey;
        this.status = status;
    }
    
    /**
     * Create a successful ENRICHMENT response.
     */
    public static EnrichmentResponse success(String traceId, String idempotencyKey,
                                           Map<String, Object> enrichedData, double confidence) {
        EnrichmentResponse response = new EnrichmentResponse(traceId, idempotencyKey, EnrichmentStatus.SUCCESS);
        response.setEnrichedData(enrichedData);
        response.setConfidence(confidence);
        return response;
    }
    
    /**
     * Create a partial success ENRICHMENT response.
     */
    public static EnrichmentResponse partialSuccess(String traceId, String idempotencyKey,
                                                   List<PartialResult> partialResults, double confidence) {
        EnrichmentResponse response = new EnrichmentResponse(traceId, idempotencyKey, EnrichmentStatus.PARTIAL_SUCCESS);
        response.setPartialResults(partialResults);
        response.setConfidence(confidence);
        return response;
    }
    
    /**
     * Create a failed ENRICHMENT response.
     */
    public static EnrichmentResponse failure(String traceId, String idempotencyKey,
                                           List<EnrichmentError> errors) {
        EnrichmentResponse response = new EnrichmentResponse(traceId, idempotencyKey, EnrichmentStatus.FAILED);
        response.setErrors(errors);
        response.setConfidence(0.0);
        return response;
    }
    
    /**
     * Create a cached ENRICHMENT response.
     */
    public static EnrichmentResponse cached(String traceId, String idempotencyKey,
                                          Map<String, Object> cachedData, double confidence) {
        EnrichmentResponse response = success(traceId, idempotencyKey, cachedData, confidence);
        response.setCacheHit(true);
        return response;
    }
    
    /**
     * Add enriched data field.
     */
    public void addEnrichedData(String field, Object value) {
        if (field != null && value != null) {
            enrichedData.put(field, value);
        }
    }
    
    /**
     * Add partial result.
     */
    public void addPartialResult(String field, Object value, double confidence, String source) {
        partialResults.add(new PartialResult(field, value, confidence, source));
    }
    
    /**
     * Add error.
     */
    public void addError(String code, String message, String field) {
        errors.add(new EnrichmentError(code, message, field));
    }
    
    /**
     * Add warning.
     */
    public void addWarning(String warning) {
        if (warning != null && !warnings.contains(warning)) {
            warnings.add(warning);
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
     * Check if response is successful.
     */
    public boolean isSuccessful() {
        return status == EnrichmentStatus.SUCCESS || status == EnrichmentStatus.PARTIAL_SUCCESS;
    }
    
    /**
     * Check if response has errors.
     */
    public boolean hasErrors() {
        return !errors.isEmpty();
    }
    
    /**
     * Check if response has warnings.
     */
    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }
    
    /**
     * Get overall quality score (combines confidence and completeness).
     */
    public double getQualityScore() {
        if (status == EnrichmentStatus.FAILED) {
            return 0.0;
        }
        
        double completeness = 1.0;
        if (status == EnrichmentStatus.PARTIAL_SUCCESS) {
            // Reduce score based on partial results
            completeness = Math.max(0.5, (double) partialResults.size() / 
                                        (partialResults.size() + errors.size()));
        }
        
        return confidence * completeness;
    }
    
    /**
     * Set processing time from start time.
     */
    public void setProcessingTime(long startTimeMs) {
        this.processingTimeMs = System.currentTimeMillis() - startTimeMs;
    }
    
    // Getters and Setters
    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }
    
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    
    public EnrichmentStatus getStatus() { return status; }
    public void setStatus(EnrichmentStatus status) { this.status = status; }
    
    public Double getConfidence() { return confidence; }
    public void setConfidence(Double confidence) { this.confidence = confidence; }
    
    public Map<String, Object> getEnrichedData() { return enrichedData; }
    public void setEnrichedData(Map<String, Object> enrichedData) { 
        this.enrichedData = enrichedData != null ? enrichedData : new HashMap<>(); 
    }
    
    public List<PartialResult> getPartialResults() { return partialResults; }
    public void setPartialResults(List<PartialResult> partialResults) { 
        this.partialResults = partialResults != null ? partialResults : new ArrayList<>(); 
    }
    
    public List<EnrichmentError> getErrors() { return errors; }
    public void setErrors(List<EnrichmentError> errors) { 
        this.errors = errors != null ? errors : new ArrayList<>(); 
    }
    
    public List<String> getWarnings() { return warnings; }
    public void setWarnings(List<String> warnings) { 
        this.warnings = warnings != null ? warnings : new ArrayList<>(); 
    }
    
    public Long getProcessingTimeMs() { return processingTimeMs; }
    public void setProcessingTimeMs(Long processingTimeMs) { this.processingTimeMs = processingTimeMs; }
    
    public Boolean getCacheHit() { return cacheHit; }
    public void setCacheHit(Boolean cacheHit) { this.cacheHit = cacheHit; }
    
    public String getDataSource() { return dataSource; }
    public void setDataSource(String dataSource) { this.dataSource = dataSource; }
    
    public Long getCompletedAt() { return completedAt; }
    public void setCompletedAt(Long completedAt) { this.completedAt = completedAt; }
    
    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { 
        this.metadata = metadata != null ? metadata : new HashMap<>(); 
    }
    
    @Override
    public String toString() {
        return "EnrichmentResponse{" +
                "traceId='" + traceId + '\'' +
                ", status=" + status +
                ", confidence=" + confidence +
                ", enrichedFields=" + enrichedData.size() +
                ", partialResults=" + partialResults.size() +
                ", errors=" + errors.size() +
                ", processingTimeMs=" + processingTimeMs +
                '}';
    }
    
    /**
     * Status of ENRICHMENT operation.
     */
    public enum EnrichmentStatus {
        SUCCESS,         // All requested ENRICHMENT completed successfully
        PARTIAL_SUCCESS, // Some ENRICHMENT completed, some failed
        FAILED,          // Enrichment failed completely
        TIMEOUT,         // Enrichment timed out
        CACHED           // Result returned from cache
    }
    
    /**
     * Represents a partial ENRICHMENT result.
     */
    public static class PartialResult {
        @JsonProperty("field")
        private String field;
        
        @JsonProperty("value")
        private Object value;
        
        @JsonProperty("confidence")
        private Double confidence;
        
        @JsonProperty("source")
        private String source;
        
        public PartialResult() {}
        
        public PartialResult(String field, Object value, Double confidence, String source) {
            this.field = field;
            this.value = value;
            this.confidence = confidence;
            this.source = source;
        }
        
        // Getters and Setters
        public String getField() { return field; }
        public void setField(String field) { this.field = field; }
        public Object getValue() { return value; }
        public void setValue(Object value) { this.value = value; }
        public Double getConfidence() { return confidence; }
        public void setConfidence(Double confidence) { this.confidence = confidence; }
        public String getSource() { return source; }
        public void setSource(String source) { this.source = source; }
        
        @Override
        public String toString() {
            return "PartialResult{" +
                    "field='" + field + '\'' +
                    ", confidence=" + confidence +
                    ", source='" + source + '\'' +
                    '}';
        }
    }
    
    /**
     * Represents an ENRICHMENT error.
     */
    public static class EnrichmentError {
        @JsonProperty("code")
        private String code;
        
        @JsonProperty("message")
        private String message;
        
        @JsonProperty("field")
        private String field;
        
        @JsonProperty("retryable")
        private Boolean retryable;
        
        public EnrichmentError() {}
        
        public EnrichmentError(String code, String message, String field) {
            this.code = code;
            this.message = message;
            this.field = field;
            this.retryable = determineRetryable(code);
        }
        
        private Boolean determineRetryable(String code) {
            if (code == null) return false;
            
            // Network and temporary errors are retryable
            return code.contains("TIMEOUT") || 
                   code.contains("NETWORK") || 
                   code.contains("RATE_LIMIT") ||
                   code.contains("TEMPORARY");
        }
        
        // Getters and Setters
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public String getField() { return field; }
        public void setField(String field) { this.field = field; }
        public Boolean getRetryable() { return retryable; }
        public void setRetryable(Boolean retryable) { this.retryable = retryable; }
        
        @Override
        public String toString() {
            return "EnrichmentError{" +
                    "code='" + code + '\'' +
                    ", message='" + message + '\'' +
                    ", field='" + field + '\'' +
                    ", retryable=" + retryable +
                    '}';
        }
    }
}