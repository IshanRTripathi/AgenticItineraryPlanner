package com.tripplanner.config;

import com.tripplanner.enums.ToolType;

/**
 * TTL (Time To Live) configuration for different tool types.
 * 
 * Different tools have different data freshness requirements:
 * - Weather data changes slowly (1 hour)
 * - Geocoding results never change (30 days)
 * - Opening hours change rarely (7 days)
 * - Transport pricing changes frequently (1 hour)
 * 
 * These values balance freshness with cache effectiveness.
 * 
 * @deprecated Use ToolType enum methods instead (getTTLSeconds(), shouldCache())
 */
@Deprecated
public class ToolCacheTTLConfig {
    
    // Weather tools - data changes slowly
    public static final int WEATHER_TTL_SECONDS = 3600;  // 1 hour
    public static final int SUGGEST_BEST_TIME_TTL_SECONDS = 3600;  // 1 hour
    
    // Geocoding - coordinates never change
    public static final int GEOCODE_TTL_SECONDS = 2592000;  // 30 days
    
    // Place data - changes rarely
    public static final int OPENING_HOURS_TTL_SECONDS = 604800;  // 7 days
    public static final int CAPACITY_TTL_SECONDS = 604800;  // 7 days
    public static final int SIMILAR_ACTIVITIES_TTL_SECONDS = 604800;  // 7 days
    
    // Distance calculations - static
    public static final int DISTANCE_TTL_SECONDS = 2592000;  // 30 days
    
    // Restaurant suggestions - depends on filters
    public static final int RESTAURANT_TTL_SECONDS = 86400;  // 1 day
    
    // Transport options - pricing changes
    public static final int TRANSPORT_TTL_SECONDS = 3600;  // 1 hour
    
    // Route optimization - context-dependent
    public static final int ROUTE_TTL_SECONDS = 1800;  // 30 minutes
    
    // Validation tools - short TTL
    public static final int TIMING_VALIDATION_TTL_SECONDS = 900;  // 15 minutes
    public static final int COMPLETENESS_VALIDATION_TTL_SECONDS = 900;  // 15 minutes
    public static final int LOCATION_VALIDATION_TTL_SECONDS = 86400;  // 1 day
    
    // Default for unknown tools
    public static final int DEFAULT_TTL_SECONDS = 900;  // 15 minutes
    
    /**
     * Get TTL for a specific tool type
     * 
     * @param toolType The tool type (e.g., "suggest-best-time", "geocode")
     * @return TTL in seconds
     * @deprecated Use ToolType.fromString(toolType).getTTLSeconds() instead
     */
    @Deprecated
    public static int getTTL(String toolType) {
        ToolType type = ToolType.fromString(toolType);
        if (type != null) {
            return type.getTTLSeconds();
        }
        return DEFAULT_TTL_SECONDS;
    }
    
    /**
     * Get TTL for a specific tool type (enum version)
     * 
     * @param toolType The tool type enum
     * @return TTL in seconds
     */
    public static int getTTL(ToolType toolType) {
        if (toolType == null) {
            return DEFAULT_TTL_SECONDS;
        }
        return toolType.getTTLSeconds();
    }
    
    /**
     * Check if a tool type should be cached
     * 
     * Some tools (like generate-node-id) should never be cached.
     * 
     * @param toolType The tool type
     * @return true if should be cached
     * @deprecated Use ToolType.fromString(toolType).shouldCache() instead
     */
    @Deprecated
    public static boolean shouldCache(String toolType) {
        ToolType type = ToolType.fromString(toolType);
        if (type != null) {
            return type.shouldCache();
        }
        return false;
    }
    
    /**
     * Check if a tool type should be cached (enum version)
     * 
     * @param toolType The tool type enum
     * @return true if should be cached
     */
    public static boolean shouldCache(ToolType toolType) {
        if (toolType == null) {
            return false;
        }
        return toolType.shouldCache();
    }
}
