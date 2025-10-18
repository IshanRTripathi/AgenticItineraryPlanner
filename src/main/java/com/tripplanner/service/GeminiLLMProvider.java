package com.tripplanner.service;

import com.tripplanner.service.ai.AiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * LLMProvider specifically for Gemini model requests.
 * Delegates to the AiClient infrastructure but registers under the "gemini" name
 * to support explicit model type requests.
 */
@Component
public class GeminiLLMProvider implements LLMProvider {
    
    private static final Logger logger = LoggerFactory.getLogger(GeminiLLMProvider.class);
    
    private final AiClient aiClient;
    
    @Autowired
    public GeminiLLMProvider(AiClient aiClient) {
        this.aiClient = aiClient;
        logger.info("Initialized GeminiLLMProvider with AI client: {}", 
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
                response = aiClient.generateStructuredContent(prompt, jsonSchema, systemPrompt);
            } else {
                response = aiClient.generateContent(prompt, systemPrompt);
            }
            
            if (response == null || response.trim().isEmpty()) {
                throw new RuntimeException("AI client returned empty response");
            }
            
            logger.debug("Generated {} characters from Gemini", response.length());
            return response;
            
        } catch (Exception e) {
            logger.error("Error generating Gemini response: {}", e.getMessage(), e);
            throw new RuntimeException("Gemini generation failed: " + e.getMessage(), e);
        }
    }
    
    @Override
    public boolean supportsModel(String modelName) {
        // Support various Gemini model names
        if (modelName == null) return false;
        String normalized = modelName.toLowerCase().trim();
        return normalized.equals("gemini") || 
               normalized.startsWith("gemini-") ||
               normalized.contains("gemini");
    }
    
    @Override
    public String getProviderName() {
        return "gemini";
    }
    
    @Override
    public String getDefaultModel() {
        return "gemini";
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
            "supportedModels", "gemini, gemini-pro, gemini-*",
            "clientInfo", aiClient != null ? aiClient.getModelInfo() : "Not initialized"
        );
    }
}






