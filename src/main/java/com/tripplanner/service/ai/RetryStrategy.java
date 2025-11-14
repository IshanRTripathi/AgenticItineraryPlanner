package com.tripplanner.service.ai;

/**
 * Retry strategy for AI provider calls.
 * 
 * Different operations have different requirements:
 * - Critical path operations (trip creation) need fast results
 * - Non-critical operations (enrichment) can afford retry delays
 */
public enum RetryStrategy {
    
    /**
     * Fast-fail strategy: No retries, immediate failover to next provider.
     * 
     * Use for:
     * - Trip creation (SkeletonPlannerAgent)
     * - Any operation where user is actively waiting
     * 
     * Behavior:
     * - Try primary provider once
     * - On error, immediately try next provider
     * - No exponential backoff delays
     * - Total failover time: < 2 seconds
     * 
     * Example: User creates a new trip and is waiting for the skeleton structure.
     * We want to show results as fast as possible, so we don't retry the failing
     * provider - we immediately switch to the backup.
     */
    FAST_FAIL,
    
    /**
     * Retry with backoff strategy: Retry up to 3 times with exponential backoff and jitter.
     * 
     * Use for:
     * - Enrichment operations (EnrichmentAgent)
     * - Activity population (ActivityAgent)
     * - Meal population (MealAgent)
     * - Transport population (TransportAgent)
     * - Chat operations (ChatAgent)
     * - Any background operation where delays are acceptable
     * 
     * Behavior:
     * - Try provider up to 3 times
     * - Exponential backoff: 2s, 4s, 8s (with jitter)
     * - Add ±25% random jitter to prevent thundering herd
     * - If all retries fail, try next provider
     * - Total max time per provider: ~14 seconds
     * 
     * Example: Enriching an attraction node with photos and details. This happens
     * in the background after the trip is created, so we can afford to retry a few
     * times before giving up and trying the backup provider.
     */
    RETRY_WITH_BACKOFF;
    
    /**
     * Get a human-readable description of this strategy.
     * 
     * @return Description string
     */
    public String getDescription() {
        return switch (this) {
            case FAST_FAIL -> "No retries, immediate failover (< 2s)";
            case RETRY_WITH_BACKOFF -> "Up to 3 retries with exponential backoff (~14s max)";
        };
    }
    
    /**
     * Check if this strategy allows retries.
     * 
     * @return true if retries are allowed, false otherwise
     */
    public boolean allowsRetries() {
        return this == RETRY_WITH_BACKOFF;
    }
    
    /**
     * Get the maximum number of retry attempts for this strategy.
     * 
     * @return Number of retries (0 for FAST_FAIL, 3 for RETRY_WITH_BACKOFF)
     */
    public int getMaxRetries() {
        return switch (this) {
            case FAST_FAIL -> 0;
            case RETRY_WITH_BACKOFF -> 3;
        };
    }
}
