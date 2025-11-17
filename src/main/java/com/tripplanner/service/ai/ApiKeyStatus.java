package com.tripplanner.service.ai;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Tracks the status and health of an API key.
 * Manages failure counts, cooldown periods, and usage statistics.
 */
public class ApiKeyStatus {
    
    private final String key;
    private final String provider;
    private final AtomicInteger failureCount;
    private final AtomicLong lastUsedTimestamp;
    private final AtomicLong cooldownUntilTimestamp;
    private final AtomicInteger successCount;
    private volatile KeyState state;
    
    public enum KeyState {
        AVAILABLE,      // Key is ready to use
        IN_USE,         // Key is currently being used
        FAILED,         // Key has failed and is in cooldown
        PREFERRED       // Key is the preferred/working key
    }
    
    public ApiKeyStatus(String key, String provider) {
        this.key = key;
        this.provider = provider;
        this.failureCount = new AtomicInteger(0);
        this.successCount = new AtomicInteger(0);
        this.lastUsedTimestamp = new AtomicLong(0);
        this.cooldownUntilTimestamp = new AtomicLong(0);
        this.state = KeyState.AVAILABLE;
    }
    
    /**
     * Check if the key is available for use (not in cooldown).
     */
    public boolean isAvailable() {
        if (state == KeyState.FAILED) {
            long now = System.currentTimeMillis();
            long cooldownUntil = cooldownUntilTimestamp.get();
            
            if (now >= cooldownUntil) {
                // Cooldown period expired, make key available again
                state = KeyState.AVAILABLE;
                failureCount.set(0);
                return true;
            }
            return false;
        }
        
        return state == KeyState.AVAILABLE || state == KeyState.PREFERRED;
    }
    
    /**
     * Mark the key as in use.
     */
    public void markInUse() {
        lastUsedTimestamp.set(System.currentTimeMillis());
        if (state != KeyState.PREFERRED) {
            state = KeyState.IN_USE;
        }
    }
    
    /**
     * Record a successful API call with this key.
     */
    public void recordSuccess() {
        successCount.incrementAndGet();
        failureCount.set(0);
        lastUsedTimestamp.set(System.currentTimeMillis());
        
        // If key was in failed state and succeeded, restore it
        if (state == KeyState.FAILED) {
            state = KeyState.AVAILABLE;
            cooldownUntilTimestamp.set(0);
        }
    }
    
    /**
     * Record a failed API call with this key.
     * Puts the key into cooldown for 30 minutes.
     */
    public void recordFailure() {
        int failures = failureCount.incrementAndGet();
        lastUsedTimestamp.set(System.currentTimeMillis());
        
        // Put key into cooldown for 30 minutes
        long cooldownDuration = 30 * 60 * 1000; // 30 minutes in milliseconds
        cooldownUntilTimestamp.set(System.currentTimeMillis() + cooldownDuration);
        state = KeyState.FAILED;
    }
    
    /**
     * Mark this key as the preferred key for the provider.
     */
    public void markAsPreferred() {
        state = KeyState.PREFERRED;
    }
    
    /**
     * Remove preferred status from this key.
     */
    public void removePreferred() {
        if (state == KeyState.PREFERRED) {
            state = KeyState.AVAILABLE;
        }
    }
    
    /**
     * Get the masked key for logging (shows only first and last 4 characters).
     */
    public String getMaskedKey() {
        if (key == null || key.length() <= 8) {
            return "***";
        }
        return key.substring(0, 4) + "..." + key.substring(key.length() - 4);
    }
    
    /**
     * Get time remaining in cooldown (in milliseconds).
     */
    public long getCooldownRemaining() {
        if (state != KeyState.FAILED) {
            return 0;
        }
        
        long now = System.currentTimeMillis();
        long cooldownUntil = cooldownUntilTimestamp.get();
        return Math.max(0, cooldownUntil - now);
    }
    
    /**
     * Get success rate as a percentage.
     */
    public double getSuccessRate() {
        int total = successCount.get() + failureCount.get();
        if (total == 0) {
            return 100.0;
        }
        return (double) successCount.get() / total * 100.0;
    }
    
    // Getters
    
    public String getKey() {
        return key;
    }
    
    public String getProvider() {
        return provider;
    }
    
    public int getFailureCount() {
        return failureCount.get();
    }
    
    public int getSuccessCount() {
        return successCount.get();
    }
    
    public long getLastUsedTimestamp() {
        return lastUsedTimestamp.get();
    }
    
    public Instant getLastUsedInstant() {
        long timestamp = lastUsedTimestamp.get();
        return timestamp > 0 ? Instant.ofEpochMilli(timestamp) : null;
    }
    
    public KeyState getState() {
        return state;
    }
    
    public boolean isPreferred() {
        return state == KeyState.PREFERRED;
    }
    
    public boolean isFailed() {
        return state == KeyState.FAILED;
    }
    
    @Override
    public String toString() {
        return String.format("ApiKeyStatus{provider=%s, key=%s, state=%s, failures=%d, successes=%d, successRate=%.1f%%, cooldownRemaining=%dms}",
            provider, getMaskedKey(), state, failureCount.get(), successCount.get(), getSuccessRate(), getCooldownRemaining());
    }
}
