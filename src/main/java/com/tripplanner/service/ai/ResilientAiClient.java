package com.tripplanner.service.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.tripplanner.service.ai.exception.TransientAiException;
import com.tripplanner.service.ai.exception.PermanentAiException;

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
    private final RetryDelayCalculator retryDelayCalculator;
    
    /**
     * Create a resilient AI client with a chain of providers.
     * Providers are tried in order until one succeeds.
     */
    public ResilientAiClient(List<AiClient> providers) {
        this.providers = new ArrayList<>(providers);
        this.modelInfo = buildModelInfo();
        this.retryDelayCalculator = new RetryDelayCalculator();
        
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
        // Default to RETRY_WITH_BACKOFF for backward compatibility
        return generateContent(userPrompt, systemPrompt, RetryStrategy.RETRY_WITH_BACKOFF);
    }
    
    /**
     * Generate content with a specific retry strategy.
     * 
     * @param userPrompt User prompt
     * @param systemPrompt System prompt (optional)
     * @param strategy Retry strategy (FAST_FAIL or RETRY_WITH_BACKOFF)
     * @return Generated content
     */
    public String generateContent(String userPrompt, String systemPrompt, RetryStrategy strategy) {
        logger.info("Generating content with strategy: {} ({} providers available)", 
                   strategy, providers.size());
        
        List<Exception> failures = new ArrayList<>();
        
        for (int i = 0; i < providers.size(); i++) {
            AiClient provider = providers.get(i);
            String providerName = provider.getClass().getSimpleName();
            
            if (!provider.isAvailable()) {
                logger.debug("Provider {} ({}) not available, skipping", i + 1, providerName);
                continue;
            }
            
            try {
                logger.info("Attempting content generation with provider {} ({}) using strategy {}", 
                           i + 1, providerName, strategy);
                
                String result = attemptWithStrategy(provider, userPrompt, null, systemPrompt, strategy);
                
                if (isValidResponse(result)) {
                    logger.info("✅ Provider {} ({}) succeeded", i + 1, providerName);
                    return result;
                } else {
                    logger.warn("❌ Provider {} ({}) returned empty/invalid response", i + 1, providerName);
                    failures.add(new RuntimeException("Empty response from " + providerName));
                }
                
            } catch (TransientAiException e) {
                logger.warn("⚠️ Provider {} ({}) has transient error ({}), trying next provider", 
                           i + 1, providerName, e.getStatusCode());
                failures.add(e);
                
            } catch (PermanentAiException e) {
                logger.error("❌ Provider {} ({}) has permanent error, skipping: {}", 
                            i + 1, providerName, e.getMessage());
                failures.add(e);
                
            } catch (Exception e) {
                logger.error("❌ Provider {} ({}) failed with exception: {}", 
                            i + 1, providerName, e.getMessage());
                failures.add(e);
            }
        }
        
        // All providers failed
        String errorDetails = buildErrorDetails(failures);
        int availableCount = (int) providers.stream().filter(AiClient::isAvailable).count();
        logger.error("🚨 All {} available providers failed for content generation. Errors: {}", 
                    availableCount, errorDetails);
        
        if (availableCount == 0) {
            throw new RuntimeException("No AI providers available - check configuration");
        } else {
            throw new RuntimeException(
                String.format("All %d AI provider(s) failed to generate content. Errors: %s", 
                             availableCount, errorDetails));
        }
    }
    
    @Override
    public String generateStructuredContent(String userPrompt, String jsonSchema, String systemPrompt) {
        // Default to RETRY_WITH_BACKOFF for backward compatibility
        return generateStructuredContent(userPrompt, jsonSchema, systemPrompt, RetryStrategy.RETRY_WITH_BACKOFF);
    }
    
    /**
     * Generate structured content with a specific retry strategy.
     * 
     * @param userPrompt User prompt
     * @param jsonSchema JSON schema for structured output
     * @param systemPrompt System prompt (optional)
     * @param strategy Retry strategy (FAST_FAIL or RETRY_WITH_BACKOFF)
     * @return Generated structured content
     */
    public String generateStructuredContent(String userPrompt, String jsonSchema, String systemPrompt, RetryStrategy strategy) {
        logger.info("Generating structured content with strategy: {} ({} providers available)", 
                   strategy, providers.size());
        
        List<Exception> failures = new ArrayList<>();
        
        for (int i = 0; i < providers.size(); i++) {
            AiClient provider = providers.get(i);
            String providerName = provider.getClass().getSimpleName();
            
            if (!provider.isAvailable()) {
                logger.debug("Provider {} ({}) not available, skipping", i + 1, providerName);
                continue;
            }
            
            try {
                logger.info("Attempting structured content generation with provider {} ({}) using strategy {}", 
                           i + 1, providerName, strategy);
                
                String result = attemptWithStrategy(provider, userPrompt, jsonSchema, systemPrompt, strategy);
                
                if (isValidResponse(result)) {
                    logger.info("✅ Provider {} ({}) succeeded", i + 1, providerName);
                    return result;
                } else {
                    logger.warn("❌ Provider {} ({}) returned empty/invalid response", i + 1, providerName);
                    failures.add(new RuntimeException("Empty response from " + providerName));
                }
                
            } catch (TransientAiException e) {
                logger.warn("⚠️ Provider {} ({}) has transient error ({}), trying next provider", 
                           i + 1, providerName, e.getStatusCode());
                failures.add(e);
                
            } catch (PermanentAiException e) {
                logger.error("❌ Provider {} ({}) has permanent error, skipping: {}", 
                            i + 1, providerName, e.getMessage());
                failures.add(e);
                
            } catch (Exception e) {
                logger.error("❌ Provider {} ({}) failed with exception: {}", 
                            i + 1, providerName, e.getMessage());
                failures.add(e);
            }
        }
        
        // All providers failed
        String errorDetails = buildErrorDetails(failures);
        int availableCount = (int) providers.stream().filter(AiClient::isAvailable).count();
        logger.error("🚨 All {} available providers failed for structured content generation. Errors: {}", 
                    availableCount, errorDetails);
        
        if (availableCount == 0) {
            throw new RuntimeException("No AI providers available - check configuration");
        } else {
            throw new RuntimeException(
                String.format("All %d AI provider(s) failed to generate structured content. Errors: %s", 
                             availableCount, errorDetails));
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
    
    /**
     * Attempt to generate content with a provider using the specified strategy.
     * 
     * @param provider AI provider to use
     * @param userPrompt User prompt
     * @param jsonSchema JSON schema (null for unstructured content)
     * @param systemPrompt System prompt (optional)
     * @param strategy Retry strategy
     * @return Generated content
     */
    private String attemptWithStrategy(AiClient provider, String userPrompt, String jsonSchema, 
                                      String systemPrompt, RetryStrategy strategy) {
        if (strategy == RetryStrategy.FAST_FAIL) {
            // No retries, just attempt once
            if (jsonSchema != null) {
                return provider.generateStructuredContent(userPrompt, jsonSchema, systemPrompt);
            } else {
                return provider.generateContent(userPrompt, systemPrompt);
            }
        } else {
            // RETRY_WITH_BACKOFF: retry up to 3 times with exponential backoff + jitter
            return attemptWithRetry(provider, userPrompt, jsonSchema, systemPrompt);
        }
    }
    
    /**
     * Attempt to generate content with retries and exponential backoff.
     * 
     * @param provider AI provider to use
     * @param userPrompt User prompt
     * @param jsonSchema JSON schema (null for unstructured content)
     * @param systemPrompt System prompt (optional)
     * @return Generated content
     */
    private String attemptWithRetry(AiClient provider, String userPrompt, String jsonSchema, String systemPrompt) {
        int maxRetries = 3;
        String providerName = provider.getClass().getSimpleName();
        
        for (int attempt = 0; attempt < maxRetries; attempt++) {
            try {
                if (attempt > 0) {
                    logger.info("Retry attempt {} for provider {}", attempt, providerName);
                }
                
                if (jsonSchema != null) {
                    return provider.generateStructuredContent(userPrompt, jsonSchema, systemPrompt);
                } else {
                    return provider.generateContent(userPrompt, systemPrompt);
                }
                
            } catch (TransientAiException e) {
                if (attempt < maxRetries - 1) {
                    int delay = retryDelayCalculator.calculateDelay(attempt);
                    logger.warn("Provider {} failed with transient error ({}), retrying in {}ms (attempt {}/{})", 
                               providerName, e.getStatusCode(), delay, attempt + 1, maxRetries);
                    
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Retry interrupted", ie);
                    }
                } else {
                    logger.error("Provider {} failed after {} retries with transient error", 
                                providerName, maxRetries);
                    throw e;
                }
            } catch (PermanentAiException e) {
                // Don't retry permanent errors
                logger.error("Provider {} failed with permanent error, not retrying", providerName);
                throw e;
            }
        }
        
        // Should not reach here
        throw new RuntimeException("Unexpected state in retry logic");
    }
    
    /**
     * Build a detailed error message from all failures.
     * 
     * @param failures List of exceptions from failed providers
     * @return Formatted error details
     */
    private String buildErrorDetails(List<Exception> failures) {
        if (failures.isEmpty()) {
            return "No error details available";
        }
        
        StringBuilder details = new StringBuilder();
        for (int i = 0; i < failures.size(); i++) {
            if (i > 0) details.append("; ");
            
            Exception e = failures.get(i);
            if (e instanceof TransientAiException) {
                TransientAiException tae = (TransientAiException) e;
                details.append(String.format("%s[transient:%d]", 
                                            tae.getProvider(), tae.getStatusCode()));
            } else if (e instanceof PermanentAiException) {
                PermanentAiException pae = (PermanentAiException) e;
                details.append(String.format("%s[permanent:%d]", 
                                            pae.getProvider(), pae.getStatusCode()));
            } else {
                details.append(e.getClass().getSimpleName()).append(": ").append(e.getMessage());
            }
        }
        
        return details.toString();
    }
}