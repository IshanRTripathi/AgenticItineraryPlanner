package com.tripplanner.service.ai;

import java.util.Random;

/**
 * Utility for calculating retry delays with exponential backoff and jitter.
 * 
 * Jitter prevents the "thundering herd" problem where multiple concurrent
 * requests all retry at the same time, overwhelming the recovering service.
 */
public class RetryDelayCalculator {
    
    private static final int BASE_DELAY_MS = 2000;      // 2 seconds
    private static final int MIN_DELAY_MS = 1000;       // 1 second minimum
    private static final int MAX_DELAY_MS = 10000;      // 10 seconds maximum
    private static final double JITTER_FACTOR = 0.25;   // ±25% jitter
    
    private final Random random;
    
    /**
     * Create a new retry delay calculator with a random seed.
     */
    public RetryDelayCalculator() {
        this.random = new Random();
    }
    
    /**
     * Create a new retry delay calculator with a specific seed (for testing).
     * 
     * @param seed Random seed for reproducible jitter
     */
    public RetryDelayCalculator(long seed) {
        this.random = new Random(seed);
    }
    
    /**
     * Calculate retry delay with exponential backoff and jitter.
     * 
     * Formula:
     * 1. Base delay: BASE_DELAY_MS * 2^attemptNumber
     * 2. Cap at MAX_DELAY_MS
     * 3. Add jitter: delay * (0.75 to 1.25)
     * 4. Ensure minimum: max(result, MIN_DELAY_MS)
     * 
     * Examples:
     * - Attempt 0: 2000ms * 2^0 = 2000ms → 1500-2500ms with jitter
     * - Attempt 1: 2000ms * 2^1 = 4000ms → 3000-5000ms with jitter
     * - Attempt 2: 2000ms * 2^2 = 8000ms → 6000-10000ms with jitter
     * - Attempt 3: 2000ms * 2^3 = 16000ms → capped at 10000ms → 7500-10000ms with jitter
     * 
     * @param attemptNumber Zero-based attempt number (0 = first retry)
     * @return Delay in milliseconds
     */
    public int calculateDelay(int attemptNumber) {
        if (attemptNumber < 0) {
            throw new IllegalArgumentException("Attempt number must be non-negative");
        }
        
        // Step 1: Exponential backoff
        int exponentialDelay = BASE_DELAY_MS * (int) Math.pow(2, attemptNumber);
        
        // Step 2: Cap at maximum
        int cappedDelay = Math.min(exponentialDelay, MAX_DELAY_MS);
        
        // Step 3: Add jitter (±25%)
        // Random value between 0.75 and 1.25
        double jitterMultiplier = 1.0 - JITTER_FACTOR + (random.nextDouble() * 2 * JITTER_FACTOR);
        int delayWithJitter = (int) (cappedDelay * jitterMultiplier);
        
        // Step 4: Ensure minimum
        return Math.max(delayWithJitter, MIN_DELAY_MS);
    }
    
    /**
     * Calculate total time for all retries.
     * 
     * @param maxRetries Maximum number of retries
     * @return Approximate total time in milliseconds (without jitter)
     */
    public int calculateTotalTime(int maxRetries) {
        int total = 0;
        for (int i = 0; i < maxRetries; i++) {
            // Use average delay (no jitter) for estimation
            int exponentialDelay = BASE_DELAY_MS * (int) Math.pow(2, i);
            total += Math.min(exponentialDelay, MAX_DELAY_MS);
        }
        return total;
    }
    
    /**
     * Get the base delay in milliseconds.
     * 
     * @return Base delay (2000ms)
     */
    public static int getBaseDelayMs() {
        return BASE_DELAY_MS;
    }
    
    /**
     * Get the minimum delay in milliseconds.
     * 
     * @return Minimum delay (1000ms)
     */
    public static int getMinDelayMs() {
        return MIN_DELAY_MS;
    }
    
    /**
     * Get the maximum delay in milliseconds.
     * 
     * @return Maximum delay (10000ms)
     */
    public static int getMaxDelayMs() {
        return MAX_DELAY_MS;
    }
    
    /**
     * Get the jitter factor.
     * 
     * @return Jitter factor (0.25 = ±25%)
     */
    public static double getJitterFactor() {
        return JITTER_FACTOR;
    }
}
