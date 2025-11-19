package com.tripplanner.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Service for tracking LLM token usage and costs.
 * Publishes detailed usage metrics to Pub/Sub for analytics.
 */
@Service
public class LLMUsageTracker {
    
    private static final Logger logger = LoggerFactory.getLogger(LLMUsageTracker.class);
    
    @Autowired(required = false)
    private PubSubTemplate pubSubTemplate;
    
    @Value("${analytics.pubsub.topic:analytics-events}")
    private String analyticsTopic;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // Pricing per 1M tokens (as of 2024)
    private static final Map<String, Double> PRICING = new HashMap<>();
    static {
        // Gemini pricing (per 1M tokens)
        PRICING.put("gemini-1.5-pro-input", 1.25);
        PRICING.put("gemini-1.5-pro-output", 5.00);
        PRICING.put("gemini-1.5-flash-input", 0.075);
        PRICING.put("gemini-1.5-flash-output", 0.30);
        PRICING.put("gemini-2.0-flash-input", 0.10);
        PRICING.put("gemini-2.0-flash-output", 0.40);
        PRICING.put("gemini-2.5-flash-input", 0.10);
        PRICING.put("gemini-2.5-flash-output", 0.40);
        
        // OpenAI pricing (per 1M tokens)
        PRICING.put("gpt-4o-input", 2.50);
        PRICING.put("gpt-4o-output", 10.00);
        PRICING.put("gpt-4o-mini-input", 0.15);
        PRICING.put("gpt-4o-mini-output", 0.60);
    }
    
    /**
     * Track LLM token usage.
     * 
     * @param provider Provider name (e.g., "gemini", "openai")
     * @param model Model name (e.g., "gemini-1.5-pro", "gpt-4o")
     * @param promptTokens Number of prompt tokens
     * @param completionTokens Number of completion tokens
     * @param agentType Type of agent making the request (optional)
     * @param itineraryId Related itinerary ID (optional)
     */
    public void trackUsage(String provider, String model, int promptTokens, int completionTokens, 
                          String agentType, String itineraryId) {
        if (pubSubTemplate == null) {
            logger.debug("PubSubTemplate not available, skipping LLM usage tracking");
            return;
        }
        
        try {
            int totalTokens = promptTokens + completionTokens;
            double cost = calculateCost(model, promptTokens, completionTokens);
            
            Map<String, Object> properties = new HashMap<>();
            properties.put("provider", provider);
            properties.put("model", model);
            properties.put("promptTokens", promptTokens);
            properties.put("completionTokens", completionTokens);
            properties.put("totalTokens", totalTokens);
            properties.put("llmCostUsd", cost);
            
            if (agentType != null) {
                properties.put("agentType", agentType);
            }
            
            if (itineraryId != null) {
                properties.put("itineraryId", itineraryId);
            }
            
            Map<String, Object> event = new HashMap<>();
            event.put("eventName", "llm_token_usage");
            event.put("timestamp", System.currentTimeMillis());
            event.put("platform", "backend");
            event.put("properties", properties);
            
            String eventJson = objectMapper.writeValueAsString(event);
            pubSubTemplate.publish(analyticsTopic, eventJson);
            
            logger.debug("Tracked LLM usage: {} tokens, ${} cost", totalTokens, String.format("%.4f", cost));
        } catch (Exception e) {
            logger.error("Failed to track LLM usage", e);
        }
    }
    
    /**
     * Calculate cost based on model and token usage.
     */
    private double calculateCost(String model, int promptTokens, int completionTokens) {
        String inputKey = model + "-input";
        String outputKey = model + "-output";
        
        double inputCostPer1M = PRICING.getOrDefault(inputKey, 0.0);
        double outputCostPer1M = PRICING.getOrDefault(outputKey, 0.0);
        
        double inputCost = (promptTokens / 1_000_000.0) * inputCostPer1M;
        double outputCost = (completionTokens / 1_000_000.0) * outputCostPer1M;
        
        return inputCost + outputCost;
    }
}
