package com.tripplanner.dto.cache;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Statistics about cache performance for an itinerary.
 * 
 * Used for monitoring and debugging cache effectiveness.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CacheStats {
    
    @JsonProperty("itineraryId")
    private String itineraryId;
    
    @JsonProperty("totalEntries")
    private Integer totalEntries;
    
    @JsonProperty("totalHits")
    private Integer totalHits;
    
    @JsonProperty("totalMisses")
    private Integer totalMisses;
    
    @JsonProperty("hitRate")
    private Double hitRate;  // 0.0 to 1.0
    
    @JsonProperty("lastAccessTime")
    private Long lastAccessTime;
    
    @JsonProperty("cacheSize")
    private Long cacheSize;  // Estimated size in bytes
    
    // Constructors
    
    public CacheStats() {
    }
    
    public CacheStats(String itineraryId, int totalEntries, int totalHits, int totalMisses) {
        this.itineraryId = itineraryId;
        this.totalEntries = totalEntries;
        this.totalHits = totalHits;
        this.totalMisses = totalMisses;
        
        int total = totalHits + totalMisses;
        this.hitRate = total > 0 ? (double) totalHits / total : 0.0;
    }
    
    // Getters and Setters
    
    public String getItineraryId() {
        return itineraryId;
    }
    
    public void setItineraryId(String itineraryId) {
        this.itineraryId = itineraryId;
    }
    
    public Integer getTotalEntries() {
        return totalEntries;
    }
    
    public void setTotalEntries(Integer totalEntries) {
        this.totalEntries = totalEntries;
    }
    
    public Integer getTotalHits() {
        return totalHits;
    }
    
    public void setTotalHits(Integer totalHits) {
        this.totalHits = totalHits;
    }
    
    public Integer getTotalMisses() {
        return totalMisses;
    }
    
    public void setTotalMisses(Integer totalMisses) {
        this.totalMisses = totalMisses;
    }
    
    public Double getHitRate() {
        return hitRate;
    }
    
    public void setHitRate(Double hitRate) {
        this.hitRate = hitRate;
    }
    
    public Long getLastAccessTime() {
        return lastAccessTime;
    }
    
    public void setLastAccessTime(Long lastAccessTime) {
        this.lastAccessTime = lastAccessTime;
    }
    
    public Long getCacheSize() {
        return cacheSize;
    }
    
    public void setCacheSize(Long cacheSize) {
        this.cacheSize = cacheSize;
    }
}
