package com.tripplanner.service.ai;

import com.tripplanner.config.ApiKeyConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Service for managing API key rotation across multiple providers.
 * 
 * Features:
 * - Manages multiple API keys per provider
 * - Tracks key health and failure rates
 * - Implements 30-minute cooldown for failed keys
 * - Remembers working keys and prefers them for future requests
 * - Thread-safe for concurrent access
 */
@Service
public class ApiKeyRotationService {
    
    private static final Logger logger = LoggerFactory.getLogger(ApiKeyRotationService.class);
    
    private final ApiKeyConfig apiKeyConfig;
    
    // Map: provider -> list of key statuses
    private final Map<String, List<ApiKeyStatus>> keyStatusMap = new ConcurrentHashMap<>();
    
    // Map: provider -> preferred key
    private final Map<String, String> preferredKeyMap = new ConcurrentHashMap<>();
    
    public ApiKeyRotationService(ApiKeyConfig apiKeyConfig) {
        this.apiKeyConfig = apiKeyConfig;
    }
    
    @PostConstruct
    public void initialize() {
        logger.info("=== API KEY ROTATION SERVICE INITIALIZATION ===");
        
        // Initialize key statuses for each provider
        for (String provider : apiKeyConfig.getConfiguredProviders()) {
            List<String> keys = apiKeyConfig.getKeys(provider);
            
            if (keys.isEmpty()) {
                logger.warn("No keys configured for provider: {}", provider);
                continue;
            }
            
            List<ApiKeyStatus> statuses = keys.stream()
                .map(key -> new ApiKeyStatus(key, provider))
                .collect(Collectors.toList());
            
            keyStatusMap.put(provider, statuses);
            
            logger.info("Initialized {} key(s) for provider: {}", statuses.size(), provider);
            for (int i = 0; i < statuses.size(); i++) {
                logger.info("  Key {}: {}", i + 1, statuses.get(i).getMaskedKey());
            }
        }
        
        int totalKeys = keyStatusMap.values().stream()
            .mapToInt(List::size)
            .sum();
        
        logger.info("Total keys initialized: {}", totalKeys);
        logger.info("===============================================");
    }
    
    /**
     * Get a working API key for the specified provider.
     * 
     * Strategy:
     * 1. Try preferred key first (if exists and available)
     * 2. Try other available keys in order
     * 3. Throw exception if all keys are in cooldown
     * 
     * @param provider Provider name ("gemini", "openrouter", etc.)
     * @return Working API key
     * @throws RuntimeException if no keys are available
     */
    public synchronized String getWorkingKey(String provider) {
        List<ApiKeyStatus> statuses = keyStatusMap.get(provider.toLowerCase());
        
        if (statuses == null || statuses.isEmpty()) {
            throw new RuntimeException("No API keys configured for provider: " + provider);
        }
        
        // Strategy 1: Try preferred key first
        String preferredKey = preferredKeyMap.get(provider.toLowerCase());
        if (preferredKey != null) {
            ApiKeyStatus preferredStatus = findKeyStatus(statuses, preferredKey);
            if (preferredStatus != null && preferredStatus.isAvailable()) {
                preferredStatus.markInUse();
                logger.debug("Using preferred key for {}: {}", provider, preferredStatus.getMaskedKey());
                return preferredKey;
            } else if (preferredStatus != null && !preferredStatus.isAvailable()) {
                logger.warn("Preferred key for {} is not available (cooldown: {}ms), trying other keys",
                    provider, preferredStatus.getCooldownRemaining());
                // Remove preferred status since it's not available
                preferredKeyMap.remove(provider.toLowerCase());
                preferredStatus.removePreferred();
            }
        }
        
        // Strategy 2: Try other available keys in order
        for (ApiKeyStatus status : statuses) {
            if (status.isAvailable()) {
                status.markInUse();
                String key = status.getKey();
                logger.info("Using key for {}: {} (state: {}, success rate: {:.1f}%)",
                    provider, status.getMaskedKey(), status.getState(), status.getSuccessRate());
                return key;
            }
        }
        
        // Strategy 3: All keys are in cooldown
        long minCooldown = statuses.stream()
            .mapToLong(ApiKeyStatus::getCooldownRemaining)
            .min()
            .orElse(0);
        
        logger.error("All {} API keys are in cooldown! Minimum cooldown: {}ms", provider, minCooldown);
        
        // Log status of all keys
        logger.error("Key status for {}:", provider);
        for (int i = 0; i < statuses.size(); i++) {
            ApiKeyStatus status = statuses.get(i);
            logger.error("  Key {}: {} - State: {}, Cooldown: {}ms, Failures: {}, Successes: {}",
                i + 1, status.getMaskedKey(), status.getState(),
                status.getCooldownRemaining(), status.getFailureCount(), status.getSuccessCount());
        }
        
        throw new RuntimeException(String.format(
            "All %d API key(s) for provider '%s' are in cooldown. Minimum wait time: %d seconds",
            statuses.size(), provider, minCooldown / 1000));
    }
    
    /**
     * Report a successful API call with the given key.
     * Marks the key as preferred for future requests.
     * 
     * @param provider Provider name
     * @param key API key that succeeded
     */
    public synchronized void reportSuccess(String provider, String key) {
        List<ApiKeyStatus> statuses = keyStatusMap.get(provider.toLowerCase());
        if (statuses == null) {
            logger.warn("Cannot report success for unknown provider: {}", provider);
            return;
        }
        
        ApiKeyStatus status = findKeyStatus(statuses, key);
        if (status == null) {
            logger.warn("Cannot report success for unknown key: {}", maskKey(key));
            return;
        }
        
        status.recordSuccess();
        
        // Mark as preferred key
        String oldPreferred = preferredKeyMap.get(provider.toLowerCase());
        if (oldPreferred != null && !oldPreferred.equals(key)) {
            // Remove preferred status from old key
            ApiKeyStatus oldStatus = findKeyStatus(statuses, oldPreferred);
            if (oldStatus != null) {
                oldStatus.removePreferred();
            }
        }
        
        preferredKeyMap.put(provider.toLowerCase(), key);
        status.markAsPreferred();
        
        logger.info("✅ Key success for {}: {} (now preferred, success rate: {}%)",
            provider, status.getMaskedKey(), String.format("%.1f", status.getSuccessRate()));
    }
    
    /**
     * Report a failed API call with the given key.
     * Puts the key into 30-minute cooldown.
     * 
     * @param provider Provider name
     * @param key API key that failed
     */
    public synchronized void reportFailure(String provider, String key) {
        List<ApiKeyStatus> statuses = keyStatusMap.get(provider.toLowerCase());
        if (statuses == null) {
            logger.warn("Cannot report failure for unknown provider: {}", provider);
            return;
        }
        
        ApiKeyStatus status = findKeyStatus(statuses, key);
        if (status == null) {
            logger.warn("Cannot report failure for unknown key: {}", maskKey(key));
            return;
        }
        
        status.recordFailure();
        
        // Remove preferred status if this was the preferred key
        String preferredKey = preferredKeyMap.get(provider.toLowerCase());
        if (key.equals(preferredKey)) {
            preferredKeyMap.remove(provider.toLowerCase());
            logger.warn("Removed preferred status from failed key for {}", provider);
        }
        
        logger.warn("❌ Key failure for {}: {} (entering 30min cooldown, failures: {}, success rate: {}%)",
            provider, status.getMaskedKey(), status.getFailureCount(), String.format("%.1f", status.getSuccessRate()));
        
        // Check if all keys are now in cooldown
        long availableKeys = statuses.stream()
            .filter(ApiKeyStatus::isAvailable)
            .count();
        
        if (availableKeys == 0) {
            logger.error("⚠️ CRITICAL: All {} API keys for {} are now in cooldown!",
                statuses.size(), provider);
        } else {
            logger.info("{} key(s) still available for {}", availableKeys, provider);
        }
    }
    
    /**
     * Get statistics for all keys of a provider.
     * 
     * @param provider Provider name
     * @return Map of key statistics
     */
    public Map<String, Object> getProviderStats(String provider) {
        List<ApiKeyStatus> statuses = keyStatusMap.get(provider.toLowerCase());
        if (statuses == null || statuses.isEmpty()) {
            return Collections.emptyMap();
        }
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("provider", provider);
        stats.put("totalKeys", statuses.size());
        stats.put("availableKeys", statuses.stream().filter(ApiKeyStatus::isAvailable).count());
        stats.put("failedKeys", statuses.stream().filter(ApiKeyStatus::isFailed).count());
        stats.put("preferredKey", preferredKeyMap.get(provider.toLowerCase()));
        
        List<Map<String, Object>> keyStats = new ArrayList<>();
        for (int i = 0; i < statuses.size(); i++) {
            ApiKeyStatus status = statuses.get(i);
            Map<String, Object> keyStat = new HashMap<>();
            keyStat.put("index", i + 1);
            keyStat.put("maskedKey", status.getMaskedKey());
            keyStat.put("state", status.getState().toString());
            keyStat.put("isPreferred", status.isPreferred());
            keyStat.put("successCount", status.getSuccessCount());
            keyStat.put("failureCount", status.getFailureCount());
            keyStat.put("successRate", status.getSuccessRate());
            keyStat.put("cooldownRemaining", status.getCooldownRemaining());
            keyStat.put("lastUsed", status.getLastUsedInstant());
            keyStats.add(keyStat);
        }
        stats.put("keys", keyStats);
        
        return stats;
    }
    
    /**
     * Get statistics for all providers.
     * 
     * @return Map of all provider statistics
     */
    public Map<String, Map<String, Object>> getAllStats() {
        Map<String, Map<String, Object>> allStats = new HashMap<>();
        for (String provider : keyStatusMap.keySet()) {
            allStats.put(provider, getProviderStats(provider));
        }
        return allStats;
    }
    
    /**
     * Find key status by key value.
     */
    private ApiKeyStatus findKeyStatus(List<ApiKeyStatus> statuses, String key) {
        return statuses.stream()
            .filter(s -> s.getKey().equals(key))
            .findFirst()
            .orElse(null);
    }
    
    /**
     * Mask API key for logging.
     */
    private String maskKey(String key) {
        if (key == null || key.length() <= 8) {
            return "***";
        }
        return key.substring(0, 4) + "..." + key.substring(key.length() - 4);
    }
    
    /**
     * Check if any keys are available for a provider.
     * 
     * @param provider Provider name
     * @return true if at least one key is available
     */
    public boolean hasAvailableKeys(String provider) {
        List<ApiKeyStatus> statuses = keyStatusMap.get(provider.toLowerCase());
        if (statuses == null || statuses.isEmpty()) {
            return false;
        }
        
        return statuses.stream().anyMatch(ApiKeyStatus::isAvailable);
    }
    
    /**
     * Get the number of available keys for a provider.
     * 
     * @param provider Provider name
     * @return Number of available keys
     */
    public int getAvailableKeyCount(String provider) {
        List<ApiKeyStatus> statuses = keyStatusMap.get(provider.toLowerCase());
        if (statuses == null || statuses.isEmpty()) {
            return 0;
        }
        
        return (int) statuses.stream().filter(ApiKeyStatus::isAvailable).count();
    }
}
