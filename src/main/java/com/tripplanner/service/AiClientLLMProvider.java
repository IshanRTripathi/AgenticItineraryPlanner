package com.tripplanner.service;

import com.tripplanner.service.ai.AiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * LLMProvider implementation that bridges to the existing AiClient infrastructure.
 * This adapter allows LLMService to use the resilient AI client configuration
 * (Gemini/OpenRouter) without duplicating the provider logic.
 */
@Component
public class AiClientLLMProvider implements LLMProvider {
    
    private static final Logger logger = LoggerFactory.getLogger(AiClientLLMProvider.class);
    
    private final AiClient aiClient;
    
    @Autowired
    public AiClientLLMProvider(AiClient aiClient) {
        this.aiClient = aiClient;
        logger.info("Initialized AiClientLLMProvider with AI client: {}", 
                   aiClient != null ? aiClient.getModelInfo() : "null");
    }
    
    @Override
    public String generate(String prompt, Map<String, Object> parameters) {
        if (prompt == null || prompt.trim().isEmpty()) {
            throw new IllegalArgumentException("Prompt cannot be null or empty");
        }
        
        try {
            // Extract system prompt if provided in parameters
            String systemPrompt = (String) parameters.getOrDefault("system_prompt", null);
            
            // Check if we need structured JSON output
            String jsonSchema = (String) parameters.get("json_schema");
            
            String response;
            if (jsonSchema != null && !jsonSchema.trim().isEmpty()) {
                // Use structured content generation
                response = aiClient.generateStructuredContent(prompt, jsonSchema, systemPrompt);
            } else {
                // Use standard content generation
                response = aiClient.generateContent(prompt, systemPrompt);
            }
            
            if (response == null || response.trim().isEmpty()) {
                throw new RuntimeException("AI client returned empty response");
            }
            
            return response;
            
        } catch (Exception e) {
            logger.error("Error generating response from AI client: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate response: " + e.getMessage(), e);
        }
    }
    
    @Override
    public boolean supportsModel(String modelName) {
        // This provider acts as a facade to the resilient AI client
        // which supports multiple models (Gemini, OpenRouter)
        // We accept any model name and let the AI client handle routing
        return true;
    }
    
    @Override
    public String getProviderName() {
        // Use a generic name since this bridges to multiple providers
        return "ai-client";
    }
    
    @Override
    public String getDefaultModel() {
        return "default";
    }
    
    @Override
    public boolean isAvailable() {
        return aiClient != null && aiClient.isAvailable();
    }
    
    @Override
    public Map<String, Object> getConfiguration() {
        return Map.of(
            "provider", getProviderName(),
            "defaultModel", getDefaultModel(),
            "available", isAvailable(),
            "clientInfo", aiClient != null ? aiClient.getModelInfo() : "Not initialized"
        );
    }
}






