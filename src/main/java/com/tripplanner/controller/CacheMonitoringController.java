package com.tripplanner.controller;

import com.tripplanner.dto.cache.CacheStats;
import com.tripplanner.service.cache.ToolCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * REST API endpoints for monitoring tool cache performance.
 * 
 * Provides visibility into cache effectiveness, hit rates, and statistics.
 * Only available when cache is enabled.
 */
@RestController
@RequestMapping("/api/v1/cache")
@ConditionalOnProperty(name = "cache.tool-results.enabled", havingValue = "true")
public class CacheMonitoringController {
    
    private static final Logger logger = LoggerFactory.getLogger(CacheMonitoringController.class);
    
    @Autowired(required = false)
    private ToolCacheService toolCacheService;
    
    /**
     * Get cache statistics for a specific itinerary.
     * 
     * Returns metrics like hit rate, total entries, hits/misses.
     * 
     * @param itineraryId The itinerary ID
     * @return Cache statistics
     */
    @GetMapping("/stats/{itineraryId}")
    public ResponseEntity<CacheStats> getCacheStats(@PathVariable String itineraryId) {
        try {
            if (toolCacheService == null) {
                return ResponseEntity.status(503).build();
            }
            
            CacheStats stats = toolCacheService.getStats(itineraryId);
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            logger.error("Failed to get cache stats for itinerary: {}", itineraryId, e);
            return ResponseEntity.status(500).build();
        }
    }
    
    /**
     * Invalidate cache for a specific itinerary.
     * 
     * Useful for testing or when itinerary is significantly modified.
     * 
     * @param itineraryId The itinerary ID
     * @return Success response
     */
    @DeleteMapping("/invalidate/{itineraryId}")
    public ResponseEntity<Map<String, String>> invalidateCache(@PathVariable String itineraryId) {
        try {
            if (toolCacheService == null) {
                return ResponseEntity.status(503).build();
            }
            
            toolCacheService.invalidateAll(itineraryId);
            
            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Cache invalidated for itinerary: " + itineraryId);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to invalidate cache for itinerary: {}", itineraryId, e);
            return ResponseEntity.status(500).build();
        }
    }
    
    /**
     * Manually trigger cache cleanup.
     * 
     * Removes all expired entries across all itineraries.
     * 
     * @return Number of entries removed
     */
    @PostMapping("/cleanup")
    public ResponseEntity<Map<String, Object>> cleanupCache() {
        try {
            if (toolCacheService == null) {
                return ResponseEntity.status(503).build();
            }
            
            int removedCount = toolCacheService.cleanupAllExpired();
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("removedCount", removedCount);
            response.put("message", "Removed " + removedCount + " expired cache entries");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to cleanup cache", e);
            return ResponseEntity.status(500).build();
        }
    }
    
    /**
     * Health check endpoint for cache service.
     * 
     * @return Cache service status
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        Map<String, String> response = new HashMap<>();
        
        if (toolCacheService == null) {
            response.put("status", "disabled");
            response.put("message", "Cache service is not enabled");
            return ResponseEntity.ok(response);
        }
        
        response.put("status", "enabled");
        response.put("message", "Cache service is running");
        return ResponseEntity.ok(response);
    }
}
