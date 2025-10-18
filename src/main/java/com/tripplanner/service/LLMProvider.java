package com.tripplanner.service;

import java.util.Map;

/**
 * Interface for LLM providers to support multiple AI models.
 * Implementations should handle specific model APIs and request/response formats.
 */
public interface LLMProvider {
    
    /**
     * Generate text response using the provider's model.
     * 
     * @param prompt The input prompt for text generation
     * @param parameters Model-specific parameters (temperature, max_tokens, etc.)
     * @return Generated text response
     * @throws RuntimeException if generation fails
     */
    String generate(String prompt, Map<String, Object> parameters);
    
    /**
     * Check if this provider supports the specified model.
     * 
     * @param modelName The model name to check
     * @return true if the model is supported by this provider
     */
    boolean supportsModel(String modelName);
    
    /**
     * Get the provider name for identification and logging.
     * 
     * @return Provider name (e.g., "gemini", "qwen", "openai")
     */
    String getProviderName();
    
    /**
     * Get the default model name for this provider.
     * 
     * @return Default model name
     */
    default String getDefaultModel() {
        return getProviderName();
    }
    
    /**
     * Check if the provider is currently available/healthy.
     * 
     * @return true if the provider can handle requests
     */
    default boolean isAvailable() {
        return true;
    }
    
    /**
     * Get provider-specific configuration or metadata.
     * 
     * @return Configuration map
     */
    default Map<String, Object> getConfiguration() {
        return Map.of(
            "provider", getProviderName(),
            "defaultModel", getDefaultModel(),
            "available", isAvailable()
        );
    }
}