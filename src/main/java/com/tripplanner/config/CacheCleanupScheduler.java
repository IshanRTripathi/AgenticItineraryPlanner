package com.tripplanner.config;

import com.tripplanner.service.cache.ToolCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled job to cleanup expired cache entries.
 * 
 * Runs periodically to remove expired entries from both session cache
 * and Firestore, keeping memory usage under control.
 * 
 * Only active when cache is enabled.
 */
@Component
@ConditionalOnProperty(name = "cache.tool-results.enabled", havingValue = "true")
public class CacheCleanupScheduler {
    
    private static final Logger logger = LoggerFactory.getLogger(CacheCleanupScheduler.class);
    
    @Autowired(required = false)
    private ToolCacheService toolCacheService;
    
    /**
     * Cleanup expired cache entries once per day (configurable).
     * 
     * This prevents memory buildup and removes stale data from Firestore.
     * Runs at 2 AM daily by default.
     */
    @Scheduled(cron = "${cache.cleanup.cron:0 0 2 * * ?}") // 2 AM daily
    public void cleanupExpiredEntries() {
        if (toolCacheService == null) {
            return;
        }
        
        try {
            logger.info("Starting scheduled cache cleanup...");
            long startTime = System.currentTimeMillis();
            
            int removedCount = toolCacheService.cleanupAllExpired();
            
            long duration = System.currentTimeMillis() - startTime;
            logger.info("Cache cleanup complete: removed {} expired entries in {}ms", 
                removedCount, duration);
            
        } catch (Exception e) {
            logger.error("Failed to cleanup expired cache entries", e);
        }
    }
}
