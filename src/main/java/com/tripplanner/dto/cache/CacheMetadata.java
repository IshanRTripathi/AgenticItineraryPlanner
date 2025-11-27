package com.tripplanner.dto.cache;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Metadata about a cached tool execution.
 * 
 * Tracks information about when and how the tool was executed,
 * useful for debugging and monitoring.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CacheMetadata {
    
    @JsonProperty("agentCalled")
    private String agentCalled;  // Which agent made the call
    
    @JsonProperty("executionTimeMs")
    private Long executionTimeMs;  // How long the tool took to execute
    
    @JsonProperty("apiCallsMade")
    private Integer apiCallsMade;  // Number of external API calls made
    
    @JsonProperty("toolVersion")
    private String toolVersion;  // Tool version (for invalidation)
    
    @JsonProperty("cacheHit")
    private Boolean cacheHit;  // Was this a cache hit or miss
    
    // Constructors
    
    public CacheMetadata() {
    }
    
    public CacheMetadata(String agentCalled, Long executionTimeMs) {
        this.agentCalled = agentCalled;
        this.executionTimeMs = executionTimeMs;
        this.cacheHit = false;
    }
    
    // Getters and Setters
    
    public String getAgentCalled() {
        return agentCalled;
    }
    
    public void setAgentCalled(String agentCalled) {
        this.agentCalled = agentCalled;
    }
    
    public Long getExecutionTimeMs() {
        return executionTimeMs;
    }
    
    public void setExecutionTimeMs(Long executionTimeMs) {
        this.executionTimeMs = executionTimeMs;
    }
    
    public Integer getApiCallsMade() {
        return apiCallsMade;
    }
    
    public void setApiCallsMade(Integer apiCallsMade) {
        this.apiCallsMade = apiCallsMade;
    }
    
    public String getToolVersion() {
        return toolVersion;
    }
    
    public void setToolVersion(String toolVersion) {
        this.toolVersion = toolVersion;
    }
    
    public Boolean getCacheHit() {
        return cacheHit;
    }
    
    public void setCacheHit(Boolean cacheHit) {
        this.cacheHit = cacheHit;
    }
}
