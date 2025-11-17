package com.tripplanner.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuration class for loading API keys from environment variables.
 * Supports both single keys (backward compatibility) and JSON arrays of multiple keys.
 */
@Configuration
public class ApiKeyConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(ApiKeyConfig.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // Gemini API Keys
    @Value("${google.ai.api-key:}")
    private String geminiSingleKey;
    
    @Value("${google.ai.api-keys:[]}")
    private String geminiKeysJson;
    
    // OpenRouter API Keys
    @Value("${openrouter.api-key:}")
    private String openRouterSingleKey;
    
    @Value("${openrouter.api-keys:[]}")
    private String openRouterKeysJson;
    
    // Parsed key maps
    private Map<String, List<String>> apiKeyMap = new HashMap<>();
    
    @PostConstruct
    public void initialize() {
        logger.info("=== API KEY CONFIGURATION INITIALIZATION ===");
        
        // Load Gemini keys
        List<String> geminiKeys = loadKeys("gemini", geminiKeysJson, geminiSingleKey);
        apiKeyMap.put("gemini", geminiKeys);
        logger.info("Loaded {} Gemini API key(s)", geminiKeys.size());
        
        // Load OpenRouter keys
        List<String> openRouterKeys = loadKeys("openrouter", openRouterKeysJson, openRouterSingleKey);
        apiKeyMap.put("openrouter", openRouterKeys);
        logger.info("Loaded {} OpenRouter API key(s)", openRouterKeys.size());
        
        // Log summary
        int totalKeys = geminiKeys.size() + openRouterKeys.size();
        logger.info("Total API keys configured: {}", totalKeys);
        
        if (totalKeys == 0) {
            logger.warn("⚠️ WARNING: No API keys configured! AI services will not work.");
        }
        
        logger.info("==========================================");
    }
    
    /**
     * Load API keys for a provider from JSON array or single key (backward compatibility).
     * 
     * @param provider Provider name (for logging)
     * @param keysJson JSON array string (e.g., ["key1","key2","key3"])
     * @param singleKey Single key string (backward compatibility)
     * @return List of API keys
     */
    private List<String> loadKeys(String provider, String keysJson, String singleKey) {
        List<String> keys = new ArrayList<>();
        
        // Try to parse JSON array first
        if (keysJson != null && !keysJson.trim().isEmpty() && !keysJson.equals("[]")) {
            try {
                List<String> parsedKeys = objectMapper.readValue(keysJson, new TypeReference<List<String>>() {});
                
                // Filter out empty keys
                for (String key : parsedKeys) {
                    if (key != null && !key.trim().isEmpty()) {
                        keys.add(key.trim());
                    }
                }
                
                if (!keys.isEmpty()) {
                    logger.info("Loaded {} {} key(s) from JSON array", keys.size(), provider);
                    return keys;
                }
            } catch (Exception e) {
                logger.warn("Failed to parse {} keys JSON array: {}. Falling back to single key.", provider, e.getMessage());
            }
        }
        
        // Fallback to single key (backward compatibility)
        if (singleKey != null && !singleKey.trim().isEmpty()) {
            keys.add(singleKey.trim());
            logger.info("Loaded 1 {} key from single key configuration (backward compatibility mode)", provider);
        }
        
        return keys;
    }
    
    /**
     * Get all API keys for a provider.
     * 
     * @param provider Provider name ("gemini", "openrouter", etc.)
     * @return List of API keys, or empty list if none configured
     */
    public List<String> getKeys(String provider) {
        return apiKeyMap.getOrDefault(provider.toLowerCase(), new ArrayList<>());
    }
    
    /**
     * Check if any keys are configured for a provider.
     * 
     * @param provider Provider name
     * @return true if at least one key is configured
     */
    public boolean hasKeys(String provider) {
        List<String> keys = getKeys(provider);
        return keys != null && !keys.isEmpty();
    }
    
    /**
     * Get the number of keys configured for a provider.
     * 
     * @param provider Provider name
     * @return Number of keys
     */
    public int getKeyCount(String provider) {
        return getKeys(provider).size();
    }
    
    /**
     * Get all configured providers.
     * 
     * @return List of provider names
     */
    public List<String> getConfiguredProviders() {
        return new ArrayList<>(apiKeyMap.keySet());
    }
}
