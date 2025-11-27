package com.tripplanner.dto.cache;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Represents a cached tool result with type metadata for safe deserialization.
 * 
 * This class stores tool results in a type-safe manner by keeping:
 * - The actual request/result as JSON strings
 * - Type metadata (class names) for safe deserialization
 * - TTL and expiration information
 * 
 * Works for ALL 25 tools without special handling.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CachedToolResult {
    
    @JsonProperty("toolType")
    private String toolType;  // "suggest-best-time", "geocode", etc.
    
    @JsonProperty("cacheKey")
    private String cacheKey;  // Unique identifier for this cached result
    
    @JsonProperty("cachedAt")
    private Long cachedAt;  // Timestamp when cached (milliseconds)
    
    @JsonProperty("expiresAt")
    private Long expiresAt;  // Timestamp when expires (milliseconds)
    
    @JsonProperty("ttlSeconds")
    private Integer ttlSeconds;  // TTL in seconds
    
    // Type metadata for safe deserialization
    @JsonProperty("requestClass")
    private String requestClass;  // Full class name of request DTO
    
    @JsonProperty("resultClass")
    private String resultClass;  // Full class name of result DTO
    
    // Serialized data
    @JsonProperty("requestJson")
    private String requestJson;  // Request object as JSON string
    
    @JsonProperty("resultJson")
    private String resultJson;  // Result object as JSON string
    
    @JsonProperty("metadata")
    private CacheMetadata metadata;  // Execution metadata
    
    // Constructors
    
    public CachedToolResult() {
    }
    
    // Utility Methods
    
    /**
     * Check if this cached result has expired
     */
    public boolean isExpired() {
        if (expiresAt == null) {
            return true;
        }
        return System.currentTimeMillis() > expiresAt;
    }
    
    /**
     * Check if this cached result is valid (not expired and has result)
     */
    public boolean isValid() {
        return !isExpired() && resultJson != null && !resultJson.isEmpty();
    }
    
    /**
     * Deserialize the result to the specified type with type safety check
     * 
     * @param resultType The expected result type
     * @param objectMapper Jackson ObjectMapper for deserialization
     * @return The deserialized result
     * @throws IllegalArgumentException if type mismatch detected
     */
    public <T> T getResultAs(Class<T> resultType, ObjectMapper objectMapper) throws Exception {
        // Type safety check
        if (!resultClass.equals(resultType.getName())) {
            throw new IllegalArgumentException(
                String.format("Type mismatch: cached result is %s but requested %s",
                    resultClass, resultType.getName())
            );
        }
        
        // Deserialize
        return objectMapper.readValue(resultJson, resultType);
    }
    
    /**
     * Deserialize the request to the specified type with type safety check
     * 
     * @param requestType The expected request type
     * @param objectMapper Jackson ObjectMapper for deserialization
     * @return The deserialized request
     * @throws IllegalArgumentException if type mismatch detected
     */
    public <T> T getRequestAs(Class<T> requestType, ObjectMapper objectMapper) throws Exception {
        // Type safety check
        if (!requestClass.equals(requestType.getName())) {
            throw new IllegalArgumentException(
                String.format("Type mismatch: cached request is %s but requested %s",
                    requestClass, requestType.getName())
            );
        }
        
        // Deserialize
        return objectMapper.readValue(requestJson, requestType);
    }
    
    // Getters and Setters
    
    public String getToolType() {
        return toolType;
    }
    
    public void setToolType(String toolType) {
        this.toolType = toolType;
    }
    
    public String getCacheKey() {
        return cacheKey;
    }
    
    public void setCacheKey(String cacheKey) {
        this.cacheKey = cacheKey;
    }
    
    public Long getCachedAt() {
        return cachedAt;
    }
    
    public void setCachedAt(Long cachedAt) {
        this.cachedAt = cachedAt;
    }
    
    public Long getExpiresAt() {
        return expiresAt;
    }
    
    public void setExpiresAt(Long expiresAt) {
        this.expiresAt = expiresAt;
    }
    
    public Integer getTtlSeconds() {
        return ttlSeconds;
    }
    
    public void setTtlSeconds(Integer ttlSeconds) {
        this.ttlSeconds = ttlSeconds;
    }
    
    public String getRequestClass() {
        return requestClass;
    }
    
    public void setRequestClass(String requestClass) {
        this.requestClass = requestClass;
    }
    
    public String getResultClass() {
        return resultClass;
    }
    
    public void setResultClass(String resultClass) {
        this.resultClass = resultClass;
    }
    
    public String getRequestJson() {
        return requestJson;
    }
    
    public void setRequestJson(String requestJson) {
        this.requestJson = requestJson;
    }
    
    public String getResultJson() {
        return resultJson;
    }
    
    public void setResultJson(String resultJson) {
        this.resultJson = resultJson;
    }
    
    public CacheMetadata getMetadata() {
        return metadata;
    }
    
    public void setMetadata(CacheMetadata metadata) {
        this.metadata = metadata;
    }
}
