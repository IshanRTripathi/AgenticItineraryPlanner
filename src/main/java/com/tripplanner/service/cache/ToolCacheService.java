package com.tripplanner.service.cache;

import com.tripplanner.dto.cache.CacheStats;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Service interface for caching tool results.
 * 
 * This interface abstracts the caching implementation, allowing easy migration
 * from Firestore to Redis or other backends.
 * 
 * Key features:
 * - Type-safe generic implementation
 * - TTL-based expiration
 * - Async preloading
 * - Deduplication of concurrent requests
 * - Statistics tracking
 */
public interface ToolCacheService {
    
    /**
     * Get cached result or compute if missing/expired.
     * 
     * This is the main method for tool caching. It:
     * 1. Checks if result is cached and valid
     * 2. If yes, returns cached result (CACHE HIT)
     * 3. If no, calls toolCall to compute result (CACHE MISS)
     * 4. Stores result in cache for future use
     * 5. Handles concurrent requests via deduplication
     * 
     * Type safety is guaranteed through:
     * - Generic method signature (compile-time)
     * - Type metadata validation (runtime)
     * - Jackson deserialization with type checking
     * 
     * @param itineraryId The itinerary ID
     * @param toolType The tool type (e.g., "suggest-best-time")
     * @param cacheKey The cache key (must be deterministic)
     * @param request The request object (for storage)
     * @param toolCall The function to call if cache miss
     * @param resultType The expected result type
     * @param <T> The result type
     * @return The result (from cache or fresh computation)
     */
    <T> T getOrCompute(
        String itineraryId,
        String toolType,
        String cacheKey,
        Object request,
        Supplier<T> toolCall,
        Class<T> resultType
    );
    
    /**
     * Preload cache for an itinerary (async).
     * 
     * This method loads all cached tool results for an itinerary from
     * persistent storage into the session cache.
     * 
     * Should be called when an itinerary is loaded, but don't wait for it
     * to complete (it's async). The cache will be available when agents
     * start making tool calls.
     * 
     * @param itineraryId The itinerary ID
     * @return CompletableFuture that completes when preload is done
     */
    CompletableFuture<Void> preloadCache(String itineraryId);
    
    /**
     * Invalidate a specific cache entry.
     * 
     * Use this when you know a specific cached result is no longer valid.
     * For example, if the user changes the trip dates, invalidate all
     * weather-related cache entries.
     * 
     * @param itineraryId The itinerary ID
     * @param cacheKey The cache key to invalidate
     */
    void invalidate(String itineraryId, String cacheKey);
    
    /**
     * Invalidate all cache entries matching a pattern.
     * 
     * Use this for bulk invalidation. For example:
     * - invalidateByPattern(itineraryId, "weather:*") - invalidate all weather
     * - invalidateByPattern(itineraryId, "*") - invalidate everything
     * 
     * @param itineraryId The itinerary ID
     * @param pattern The pattern (supports * wildcard)
     */
    void invalidateByPattern(String itineraryId, String pattern);
    
    /**
     * Invalidate all cache for an itinerary.
     * 
     * Use this when the itinerary is significantly modified and all
     * cached results should be recomputed.
     * 
     * @param itineraryId The itinerary ID
     */
    void invalidateAll(String itineraryId);
    
    /**
     * Get cache statistics for an itinerary.
     * 
     * Returns metrics like:
     * - Total cache entries
     * - Cache hits/misses
     * - Hit rate
     * - Last access time
     * 
     * Useful for monitoring and debugging.
     * 
     * @param itineraryId The itinerary ID
     * @return Cache statistics
     */
    CacheStats getStats(String itineraryId);
    
    /**
     * Cleanup expired entries for an itinerary.
     * 
     * Removes all expired cache entries to free up memory and storage.
     * This is called automatically by a scheduled job, but can also be
     * called manually.
     * 
     * @param itineraryId The itinerary ID
     * @return Number of entries removed
     */
    int cleanupExpired(String itineraryId);
    
    /**
     * Cleanup expired entries for all itineraries.
     * 
     * This is called by a scheduled job to keep cache size under control.
     * 
     * @return Total number of entries removed
     */
    int cleanupAllExpired();
    
    /**
     * Simple get method for retrieving cached values.
     * Returns Optional.empty() if not found or expired.
     * 
     * @param cacheKey The cache key
     * @param resultType The expected result type
     * @param <T> The result type
     * @return Optional containing the cached value, or empty if not found
     */
    <T> java.util.Optional<T> get(String cacheKey, Class<T> resultType);
    
    /**
     * Simple put method for storing values in cache.
     * 
     * @param cacheKey The cache key
     * @param value The value to cache
     * @param ttl The time-to-live duration
     * @param <T> The value type
     */
    <T> void put(String cacheKey, T value, java.time.Duration ttl);
}
