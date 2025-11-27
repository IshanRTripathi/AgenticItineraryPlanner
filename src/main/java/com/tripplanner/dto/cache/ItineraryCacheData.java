package com.tripplanner.dto.cache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory cache data for a single itinerary.
 * 
 * This class holds all cached tool results for one itinerary in the current session.
 * It's stored in a ConcurrentHashMap in FirestoreToolCacheService for fast access.
 * 
 * Thread-safe for concurrent access by multiple agents.
 */
public class ItineraryCacheData {
    
    private final String itineraryId;
    private final Map<String, CachedToolResult> toolResults;
    
    // Statistics
    private final AtomicInteger totalHits;
    private final AtomicInteger totalMisses;
    private final AtomicLong lastAccessTime;
    
    public ItineraryCacheData(String itineraryId) {
        this.itineraryId = itineraryId;
        this.toolResults = new ConcurrentHashMap<>();
        this.totalHits = new AtomicInteger(0);
        this.totalMisses = new AtomicInteger(0);
        this.lastAccessTime = new AtomicLong(System.currentTimeMillis());
    }
    
    /**
     * Get a cached tool result by cache key
     */
    public CachedToolResult getToolResult(String cacheKey) {
        lastAccessTime.set(System.currentTimeMillis());
        return toolResults.get(cacheKey);
    }
    
    /**
     * Store a tool result in cache
     */
    public void putToolResult(String cacheKey, CachedToolResult result) {
        lastAccessTime.set(System.currentTimeMillis());
        toolResults.put(cacheKey, result);
    }
    
    /**
     * Remove a tool result from cache
     */
    public void removeToolResult(String cacheKey) {
        toolResults.remove(cacheKey);
    }
    
    /**
     * Clear all cached results
     */
    public void clear() {
        toolResults.clear();
    }
    
    /**
     * Get number of cached entries
     */
    public int size() {
        return toolResults.size();
    }
    
    /**
     * Check if cache is empty
     */
    public boolean isEmpty() {
        return toolResults.isEmpty();
    }
    
    /**
     * Record a cache hit
     */
    public void recordHit() {
        totalHits.incrementAndGet();
        lastAccessTime.set(System.currentTimeMillis());
    }
    
    /**
     * Record a cache miss
     */
    public void recordMiss() {
        totalMisses.incrementAndGet();
        lastAccessTime.set(System.currentTimeMillis());
    }
    
    /**
     * Get cache hit rate (0.0 to 1.0)
     */
    public double getHitRate() {
        int hits = totalHits.get();
        int misses = totalMisses.get();
        int total = hits + misses;
        
        if (total == 0) {
            return 0.0;
        }
        
        return (double) hits / total;
    }
    
    /**
     * Get all tool results (for persistence)
     */
    public Map<String, CachedToolResult> getAllToolResults() {
        return new ConcurrentHashMap<>(toolResults);
    }
    
    /**
     * Load tool results from persistence
     */
    public void loadToolResults(Map<String, CachedToolResult> results) {
        toolResults.clear();
        if (results != null) {
            toolResults.putAll(results);
        }
    }
    
    // Getters
    
    public String getItineraryId() {
        return itineraryId;
    }
    
    public int getTotalHits() {
        return totalHits.get();
    }
    
    public int getTotalMisses() {
        return totalMisses.get();
    }
    
    public long getLastAccessTime() {
        return lastAccessTime.get();
    }
}
