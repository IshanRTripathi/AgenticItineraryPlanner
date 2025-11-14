package com.tripplanner.service.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.ArrayList;

/**
 * Resilient AI client that chains multiple AI providers with automatic fallback.
 * Provides reliability by trying multiple providers when one fails or returns empty responses.
 * 
 * Note: This class is not annotated with @Service because it's manually configured
 * in AiClientConfig to avoid bean definition conflicts.
 */
public class ResilientAiClient implements AiClient {
    
    private static final Logger logger = LoggerFactory.getLogger(ResilientAiClient.class);
    
    private final List<AiClient> providers;
    private final String modelInfo;
    
    /**
     * Create a resilient AI client with a chain of providers.
     * Providers are tried in order until one succeeds.
     */
    public ResilientAiClient(List<AiClient> providers) {
        this.providers = new ArrayList<>(providers);
        this.modelInfo = buildModelInfo();
        
        int availableCount = (int) providers.stream().filter(AiClient::isAvailable).count();
        
        logger.info("=== RESILIENT AI CLIENT INITIALIZATION ===");
        logger.info("Total providers configured: {}", providers.size());
        logger.info("Available providers: {}", availableCount);
        
        for (int i = 0; i < providers.size(); i++) {
            AiClient provider = providers.get(i);
            String status = provider.isAvailable() ? "✅ AVAILABLE" : "❌ NOT AVAILABLE";
            logger.info("  Provider {}: {} - {}", 
                i + 1, provider.getClass().getSimpleName(), status);
        }
        
        if (availableCount == 0) {
            logger.error("❌ CRITICAL: NO AI PROVIDERS AVAILABLE - Service will fail!");
        } else if (availableCount == 1) {
            logger.warn("⚠️ WARNING: Only 1 AI provider available - no redundancy!");
        } else {
            logger.info("✅ {} AI providers available - redundancy enabled", availableCount);
        }
        logger.info("==========================================");
    }
    
    @Override
    public String generateContent(String userPrompt, String systemPrompt) {
        logger.debug("Generating content with {} providers available", providers.size());
        
        for (int i = 0; i < providers.size(); i++) {
            AiClient provider = providers.get(i);
            String providerName = provider.getClass().getSimpleName();
            
            if (!provider.isAvailable()) {
                logger.debug("Provider {} ({}) not available, skipping", i + 1, providerName);
                continue;
            }
            
            try {
                logger.info("Attempting content generation with provider {} ({})", i + 1, providerName);
                
                String result = provider.generateContent(userPrompt, systemPrompt);
                
                if (isValidResponse(result)) {
                    logger.info("✅ Provider {} ({}) succeeded", i + 1, providerName);
                    return result;
                } else {
                    logger.warn("❌ Provider {} ({}) returned empty/invalid response", i + 1, providerName);
                }
                
            } catch (Exception e) {
                logger.error("❌ Provider {} ({}) failed with exception: {}", 
                    i + 1, providerName, e.getMessage(), e);
            }
        }
        
        int availableCount = (int) providers.stream().filter(AiClient::isAvailable).count();
        logger.error("🚨 All {} available providers failed for content generation (total configured: {})", 
            availableCount, providers.size());
        
        if (availableCount == 0) {
            throw new RuntimeException("No AI providers available - check configuration");
        } else {
            throw new RuntimeException(String.format("All %d AI provider(s) failed to generate content", availableCount));
        }
    }
    
    @Override
    public String generateStructuredContent(String userPrompt, String jsonSchema, String systemPrompt) {
        logger.debug("Generating structured content with {} providers available", providers.size());
        
        for (int i = 0; i < providers.size(); i++) {
            AiClient provider = providers.get(i);
            String providerName = provider.getClass().getSimpleName();
            
            if (!provider.isAvailable()) {
                logger.debug("Provider {} ({}) not available, skipping", i + 1, providerName);
                continue;
            }
            
            try {
                logger.info("Attempting structured content generation with provider {} ({})", i + 1, providerName);
                
                String result = provider.generateStructuredContent(userPrompt, jsonSchema, systemPrompt);
                
                if (isValidResponse(result)) {
                    logger.info("✅ Provider {} ({}) succeeded", i + 1, providerName);
                    return result;
                } else {
                    logger.warn("❌ Provider {} ({}) returned empty/invalid response", i + 1, providerName);
                }
                
            } catch (Exception e) {
                logger.error("❌ Provider {} ({}) failed with exception: {}", 
                    i + 1, providerName, e.getMessage(), e);
            }
        }
        
        int availableCount = (int) providers.stream().filter(AiClient::isAvailable).count();
        logger.error("🚨 All {} available providers failed for structured content generation (total configured: {})", 
            availableCount, providers.size());
        
        if (availableCount == 0) {
            throw new RuntimeException("No AI providers available - check configuration");
        } else {
            throw new RuntimeException(String.format("All %d AI provider(s) failed to generate structured content", availableCount));
        }
    }
    
    @Override
    public boolean isAvailable() {
        boolean anyAvailable = providers.stream().anyMatch(AiClient::isAvailable);
        logger.debug("ResilientAiClient availability: {} (providers available: {})", 
            anyAvailable, providers.stream().mapToInt(p -> p.isAvailable() ? 1 : 0).sum());
        return anyAvailable;
    }
    
    @Override
    public String getModelInfo() {
        return modelInfo;
    }
    
    /**
     * Check if the response is valid (not null, not empty, not just whitespace).
     */
    private boolean isValidResponse(String response) {
        return response != null && !response.trim().isEmpty();
    }
    
    /**
     * Build model info string from all available providers.
     */
    private String buildModelInfo() {
        if (providers.isEmpty()) {
            return "ResilientAiClient (no providers)";
        }
        
        StringBuilder info = new StringBuilder("ResilientAiClient [");
        for (int i = 0; i < providers.size(); i++) {
            if (i > 0) info.append(", ");
            info.append(providers.get(i).getModelInfo());
        }
        info.append("]");
        
        return info.toString();
    }
    
    /**
     * Get the list of providers for diagnostic purposes.
     */
    public List<AiClient> getProviders() {
        return new ArrayList<>(providers);
    }
    
    /**
     * Get the number of available providers.
     */
    public int getAvailableProviderCount() {
        return (int) providers.stream().mapToInt(p -> p.isAvailable() ? 1 : 0).sum();
    }
}