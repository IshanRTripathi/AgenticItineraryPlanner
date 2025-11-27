package com.tripplanner.util;

import com.tripplanner.dto.Coordinates;

/**
 * Generates deterministic cache keys for tool results.
 * 
 * Cache keys must be:
 * - Deterministic: Same input always produces same key
 * - Unique: Different inputs produce different keys
 * - Collision-free: No two different inputs produce same key
 * 
 * Keys are normalized to handle variations in input (case, whitespace, etc.)
 */
public class ToolCacheKeyGenerator {
    
    /**
     * Generate cache key for weather timing tool
     */
    public static String forWeatherTiming(String activity, String location, String date, int duration) {
        return String.format("weather:timing:%s:%s:%s:%d",
            normalize(activity),
            normalize(location),
            date,
            duration
        );
    }
    
    /**
     * Generate cache key for weather tool
     */
    public static String forWeather(String location, String date) {
        return String.format("weather:%s:%s",
            normalize(location),
            date
        );
    }
    
    /**
     * Generate cache key for geocoding
     */
    public static String forGeocode(String address) {
        return String.format("geocode:%s", normalize(address));
    }
    
    /**
     * Generate cache key for opening hours
     */
    public static String forOpeningHours(String placeId, String date) {
        return String.format("hours:%s:%s", placeId, date);
    }
    
    /**
     * Generate cache key for distance calculation
     */
    public static String forDistance(String origin, String destination, String mode) {
        return String.format("distance:%s:%s:%s",
            normalize(origin),
            normalize(destination),
            normalize(mode)
        );
    }
    
    /**
     * Generate cache key for distance calculation with coordinates
     */
    public static String forDistanceCoords(Coordinates origin, Coordinates destination, String mode) {
        return String.format("distance:%.4f,%.4f:%.4f,%.4f:%s",
            origin.getLat(),
            origin.getLng(),
            destination.getLat(),
            destination.getLng(),
            normalize(mode)
        );
    }
    
    /**
     * Generate cache key for restaurant suggestions
     */
    public static String forRestaurants(String location, String cuisine, String dietary, int priceLevel) {
        return String.format("restaurants:%s:%s:%s:%d",
            normalize(location),
            normalize(cuisine),
            normalize(dietary),
            priceLevel
        );
    }
    
    /**
     * Generate cache key for transport options
     */
    public static String forTransportOptions(String origin, String destination) {
        return String.format("transport:%s:%s",
            normalize(origin),
            normalize(destination)
        );
    }
    
    /**
     * Generate cache key for route optimization
     */
    public static String forRouteOptimization(String itineraryId, int dayNumber) {
        return String.format("route:%s:%d", itineraryId, dayNumber);
    }
    
    /**
     * Generate cache key for capacity check
     */
    public static String forCapacity(String placeId, int partySize) {
        return String.format("capacity:%s:%d", placeId, partySize);
    }
    
    /**
     * Generate cache key for similar activities
     */
    public static String forSimilarActivities(String activityName, String location) {
        return String.format("similar:%s:%s",
            normalize(activityName),
            normalize(location)
        );
    }
    
    /**
     * Generate cache key for timing validation
     */
    public static String forTimingValidation(String itineraryId, int dayNumber) {
        return String.format("timing-validation:%s:%d", itineraryId, dayNumber);
    }
    
    /**
     * Generate cache key for completeness validation
     */
    public static String forCompletenessValidation(String itineraryId) {
        return String.format("completeness:%s", itineraryId);
    }
    
    /**
     * Generate cache key for location validation
     */
    public static String forLocationValidation(String location) {
        return String.format("location:%s", normalize(location));
    }
    
    /**
     * Generate a generic cache key from request object
     * 
     * This is a fallback for tools without specific key generators.
     * Uses the request object's hashCode.
     */
    public static String forGenericRequest(String toolType, Object request) {
        if (request == null) {
            return String.format("%s:null", toolType);
        }
        
        // Use hashCode as fallback
        return String.format("%s:%d", toolType, request.hashCode());
    }
    
    /**
     * Normalize a string for use in cache keys
     * 
     * - Convert to lowercase
     * - Trim whitespace
     * - Replace non-alphanumeric with hyphens
     * - Remove duplicate hyphens
     * - Remove leading/trailing hyphens
     */
    private static String normalize(String input) {
        if (input == null) {
            return "null";
        }
        
        return input.toLowerCase()
            .trim()
            .replaceAll("[^a-z0-9]", "-")
            .replaceAll("-+", "-")
            .replaceAll("^-|-$", "");
    }
}
