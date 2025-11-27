package com.tripplanner.enums;

/**
 * Enum for tool types used in caching and TTL configuration.
 * 
 * This provides type safety and prevents typos when referencing tool types.
 * Each tool type has an associated string value that matches the tool name
 * used in API endpoints and cache keys.
 */
public enum ToolType {
    // Weather tools
    GET_WEATHER("get-weather"),
    SUGGEST_BEST_TIME("suggest-best-time"),
    
    // Geocoding
    GEOCODE("geocode"),
    
    // Place data
    CHECK_OPENING_HOURS("check-opening-hours"),
    OPENING_HOURS("opening-hours"), // Alias for parsing
    CHECK_CAPACITY("check-capacity"),
    GET_SIMILAR_ACTIVITIES("get-similar-activities"),
    SIMILAR_ACTIVITIES("similar-activities"), // Alias for search
    
    // Distance
    CALCULATE_DISTANCE("calculate-distance"),
    DISTANCE("distance"), // Alias for service
    
    // Restaurants
    SUGGEST_RESTAURANTS("suggest-restaurants"),
    
    // Transport
    GET_TRANSPORT_OPTIONS("get-transport-options"),
    
    // Route optimization
    OPTIMIZE_ROUTE("optimize-route"),
    
    // Validation
    VALIDATE_TIMING("validate-timing"),
    VALIDATE_COMPLETENESS("validate-completeness"),
    VALIDATE_LOCATION("validate-location"),
    
    // Node operations (not cached)
    GENERATE_NODE_ID("generate-node-id"),
    UPDATE_NODE("update-node"),
    CHECK_CONFLICTS("check-conflicts"),
    VALIDATE_SCHEMA("validate-schema"),
    
    // Cost calculation
    CALCULATE_COST("calculate-cost"),
    
    // Constraints
    CHECK_USER_CONSTRAINTS("check-user-constraints"),
    
    // Currency
    CONVERT_CURRENCY("convert-currency");
    
    private final String value;
    
    ToolType(String value) {
        this.value = value;
    }
    
    /**
     * Get the string value of the tool type (e.g., "get-weather")
     */
    public String getValue() {
        return value;
    }
    
    /**
     * Get the string value (same as getValue())
     */
    @Override
    public String toString() {
        return value;
    }
    
    /**
     * Parse a string to a ToolType enum
     * 
     * @param value The string value (e.g., "get-weather")
     * @return The corresponding ToolType, or null if not found
     */
    public static ToolType fromString(String value) {
        if (value == null) {
            return null;
        }
        
        for (ToolType type : ToolType.values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        
        return null;
    }
    
    /**
     * Check if this tool type should be cached
     */
    public boolean shouldCache() {
        return switch (this) {
            case GENERATE_NODE_ID,
                 UPDATE_NODE,
                 CHECK_CONFLICTS,
                 VALIDATE_SCHEMA -> false;
            default -> true;
        };
    }
    
    /**
     * Get the TTL (Time To Live) in seconds for this tool type
     */
    public int getTTLSeconds() {
        return switch (this) {
            // Weather tools - 1 hour
            case GET_WEATHER, SUGGEST_BEST_TIME -> 3600;
            
            // Geocoding - 30 days (never changes)
            case GEOCODE -> 2592000;
            
            // Place data - 7 days
            case CHECK_OPENING_HOURS, OPENING_HOURS, CHECK_CAPACITY, GET_SIMILAR_ACTIVITIES, SIMILAR_ACTIVITIES -> 604800;
            
            // Distance - 30 days (static)
            case CALCULATE_DISTANCE, DISTANCE -> 2592000;
            
            // Restaurants - 1 day
            case SUGGEST_RESTAURANTS -> 86400;
            
            // Transport - 1 hour (pricing changes)
            case GET_TRANSPORT_OPTIONS -> 3600;
            
            // Route optimization - 30 minutes
            case OPTIMIZE_ROUTE -> 1800;
            
            // Validation - 15 minutes
            case VALIDATE_TIMING, VALIDATE_COMPLETENESS -> 900;
            
            // Location validation - 1 day
            case VALIDATE_LOCATION -> 86400;
            
            // Default - 15 minutes
            default -> 900;
        };
    }
}
