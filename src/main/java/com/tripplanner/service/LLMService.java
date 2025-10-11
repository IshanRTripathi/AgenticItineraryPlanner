package com.tripplanner.service;

import com.tripplanner.dto.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

/**
 * Service for LLM operations with provider pattern support.
 * Manages multiple LLM providers and routes requests based on model type.
 */
@Service
public class LLMService {
    
    private static final Logger logger = LoggerFactory.getLogger(LLMService.class);
    
    private final Map<String, LLMProvider> providerMap;
    private final ObjectMapper objectMapper;
    
    // Default models for different tasks
    private static final String DEFAULT_INTENT_MODEL = "gemini";
    private static final String DEFAULT_CHANGESET_MODEL = "gemini";
    private static final String DEFAULT_GENERAL_MODEL = "gemini";
    
    public LLMService(List<LLMProvider> providers, ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.providerMap = new HashMap<>();
        
        // Build provider map from injected providers
        if (providers != null) {
            for (LLMProvider provider : providers) {
                String providerName = provider.getProviderName();
                providerMap.put(providerName, provider);
                logger.info("Registered LLM provider: {}", providerName);
            }
        }
        
        logger.info("LLMService initialized with {} providers", providerMap.size());
    }
    
    /**
     * Generate response using specified model type and parameters.
     */
    public String generateResponse(String prompt, String modelType, Map<String, Object> parameters) {
        logger.debug("Generating response with model: {}", modelType);
        
        if (prompt == null || prompt.trim().isEmpty()) {
            throw new IllegalArgumentException("Prompt cannot be null or empty");
        }
        
        if (modelType == null || modelType.trim().isEmpty()) {
            modelType = DEFAULT_GENERAL_MODEL;
            logger.debug("Using default model: {}", modelType);
        }
        
        // Get provider for model type
        LLMProvider provider = getProviderForModel(modelType);
        if (provider == null) {
            throw new RuntimeException("No provider found for model type: " + modelType);
        }
        
        try {
            // Ensure parameters is not null
            if (parameters == null) {
                parameters = new HashMap<>();
            }
            
            // Add default parameters if not specified
            addDefaultParameters(parameters, modelType);
            
            // Generate response
            String response = provider.generate(prompt, parameters);
            
            if (response == null || response.trim().isEmpty()) {
                throw new RuntimeException("Provider returned empty response");
            }
            
            logger.debug("Generated response length: {} characters", response.length());
            return response;
            
        } catch (Exception e) {
            logger.error("Error generating response with model {}: {}", modelType, e.getMessage(), e);
            throw new RuntimeException("Failed to generate response: " + e.getMessage(), e);
        }
    }
    
    /**
     * Classify intent using LLM with context.
     */
    public IntentResult classifyIntent(String text, String context) {
        logger.debug("Classifying intent for text: {}", text);
        
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("Text for intent classification cannot be null or empty");
        }
        
        try {
            // Build intent classification prompt
            String prompt = buildIntentPrompt(text, context);
            
            // Use intent classification model
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("temperature", 0.1); // Low temperature for consistent classification
            parameters.put("max_tokens", 500);
            
            String response = generateResponse(prompt, DEFAULT_INTENT_MODEL, parameters);
            
            // Parse response to IntentResult
            IntentResult result = parseIntentFromResponse(response);
            
            if (result == null) {
                logger.warn("Failed to parse intent from LLM response");
                return createFallbackIntent(text);
            }
            
            logger.debug("Classified intent: {}", result.getIntent());
            return result;
            
        } catch (Exception e) {
            logger.error("Error classifying intent", e);
            return createFallbackIntent(text);
        }
    }
    
    /**
     * Generate change set using LLM with context.
     */
    public ChangeSet generateChangeSet(String request, String context) {
        logger.debug("Generating change set for request: {}", request);
        
        if (request == null || request.trim().isEmpty()) {
            throw new IllegalArgumentException("Request for change set generation cannot be null or empty");
        }
        
        try {
            // Build change set prompt
            String prompt = buildChangeSetPrompt(request, context);
            
            // Use change set generation model
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("temperature", 0.3); // Moderate temperature for creativity
            parameters.put("max_tokens", 1000);
            
            String response = generateResponse(prompt, DEFAULT_CHANGESET_MODEL, parameters);
            
            // Parse response to ChangeSet
            ChangeSet changeSet = parseChangeSetFromResponse(response);
            
            if (changeSet == null) {
                logger.warn("Failed to parse change set from LLM response");
                return createFallbackChangeSet(request);
            }
            
            // Validate change set structure
            if (!validateChangeSet(changeSet)) {
                logger.warn("Generated change set failed validation");
                return createFallbackChangeSet(request);
            }
            
            logger.debug("Generated change set with {} operations", 
                        changeSet.getOps() != null ? changeSet.getOps().size() : 0);
            return changeSet;
            
        } catch (Exception e) {
            logger.error("Error generating change set", e);
            return createFallbackChangeSet(request);
        }
    }
    
    /**
     * Get provider for specified model type.
     */
    private LLMProvider getProviderForModel(String modelType) {
        // Try exact match first
        LLMProvider provider = providerMap.get(modelType);
        if (provider != null) {
            return provider;
        }
        
        // Try to find provider that supports the model
        for (LLMProvider p : providerMap.values()) {
            if (p.supportsModel(modelType)) {
                return p;
            }
        }
        
        // Fallback to any available provider
        if (!providerMap.isEmpty()) {
            LLMProvider fallbackProvider = providerMap.values().iterator().next();
            logger.warn("No specific provider for model {}, using fallback: {}", 
                       modelType, fallbackProvider.getProviderName());
            return fallbackProvider;
        }
        
        return null;
    }
    
    /**
     * Add default parameters for model type.
     */
    private void addDefaultParameters(Map<String, Object> parameters, String modelType) {
        // Add temperature if not specified
        if (!parameters.containsKey("temperature")) {
            parameters.put("temperature", 0.7);
        }
        
        // Add max_tokens if not specified
        if (!parameters.containsKey("max_tokens")) {
            parameters.put("max_tokens", 1000);
        }
        
        // Add model-specific defaults
        switch (modelType.toLowerCase()) {
            case "gemini":
                parameters.putIfAbsent("top_p", 0.9);
                parameters.putIfAbsent("top_k", 40);
                break;
            case "qwen2.5-7b":
                parameters.putIfAbsent("top_p", 0.8);
                parameters.putIfAbsent("repetition_penalty", 1.1);
                break;
        }
    }
    
    /**
     * Build intent classification prompt.
     */
    private String buildIntentPrompt(String text, String context) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("You are an AI assistant that classifies user intents for travel itinerary management.\n\n");
        
        if (context != null && !context.trim().isEmpty()) {
            prompt.append("Context:\n").append(context).append("\n\n");
        }
        
        prompt.append("User request: \"").append(text).append("\"\n\n");
        
        prompt.append("=== SUPPORTED TASK TYPES ===\n");
        prompt.append("You MUST respond with EXACTLY ONE of these task types:\n\n");
        
        prompt.append("1. TASK TYPE: edit\n");
        prompt.append("   Use for: Modifying itinerary, changing times, adding/removing/replacing activities\n");
        prompt.append("   Examples:\n");
        prompt.append("   - \"Move lunch to 2pm\" → edit\n");
        prompt.append("   - \"Add a museum\" → edit\n");
        prompt.append("   - \"Remove the beach activity\" → edit\n");
        prompt.append("   - \"Change the restaurant\" → edit\n");
        prompt.append("   - \"Replace dinner with something else\" → edit\n\n");
        
        prompt.append("2. TASK TYPE: plan\n");
        prompt.append("   Use for: Creating NEW itineraries from scratch\n");
        prompt.append("   Examples:\n");
        prompt.append("   - \"Create a 3-day trip to Paris\" → plan\n");
        prompt.append("   - \"Plan my vacation\" → plan\n");
        prompt.append("   - \"Generate an itinerary\" → plan\n\n");
        
        prompt.append("3. TASK TYPE: explain\n");
        prompt.append("   Use for: Answering questions, providing information, or explaining existing itinerary\n");
        prompt.append("   Examples:\n");
        prompt.append("   - \"What's my plan for today?\" → explain\n");
        prompt.append("   - \"Tell me about day 2\" → explain\n");
        prompt.append("   - \"Summarize my trip\" → explain\n");
        prompt.append("   - \"What am I doing tomorrow?\" → explain\n");
        prompt.append("   - \"Why this restaurant?\" → explain\n");
        prompt.append("   - \"Explain the itinerary\" → explain\n\n");
        
        prompt.append("4. TASK TYPE: book\n");
        prompt.append("   Use for: Making reservations or bookings\n");
        prompt.append("   Examples:\n");
        prompt.append("   - \"Book this hotel\" → book\n");
        prompt.append("   - \"Reserve a flight\" → book\n");
        prompt.append("   - \"Purchase tickets\" → book\n\n");
        
        prompt.append("5. TASK TYPE: enrich\n");
        prompt.append("   Use for: Adding photos, reviews, or additional details\n");
        prompt.append("   Examples:\n");
        prompt.append("   - \"Add photos to this place\" → enrich\n");
        prompt.append("   - \"Show me reviews\" → enrich\n");
        prompt.append("   - \"Get more details about this hotel\" → enrich\n\n");
        
        prompt.append("=== CRITICAL RULES ===\n");
        prompt.append("1. Questions/explanations about EXISTING itinerary → USE 'explain'\n");
        prompt.append("2. Creating NEW itinerary from scratch → USE 'plan'\n");
        prompt.append("3. Modifying itinerary (add/remove/change) → USE 'edit'\n");
        prompt.append("4. Booking/reservations → USE 'book'\n");
        prompt.append("5. Photos/reviews/details → USE 'enrich'\n");
        prompt.append("6. You MUST choose from: edit, plan, explain, book, enrich\n\n");
        
        prompt.append("Respond in JSON format:\n");
        prompt.append("{\n");
        prompt.append("  \"intent\": \"DESCRIPTIVE_INTENT_NAME\",\n");
        prompt.append("  \"taskType\": \"edit OR plan OR explain OR book OR enrich\",\n");
        prompt.append("  \"entities\": {},\n");
        prompt.append("  \"confidence\": 0.95\n");
        prompt.append("}\n");
        
        return prompt.toString();
    }
    
    /**
     * Build change set generation prompt.
     */
    private String buildChangeSetPrompt(String request, String context) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("You are an AI assistant that generates change sets for travel itinerary modifications.\n\n");
        
        if (context != null && !context.trim().isEmpty()) {
            prompt.append("Current itinerary context:\n").append(context).append("\n\n");
        }
        
        prompt.append("User request: \"").append(request).append("\"\n\n");
        
        prompt.append("Generate a change set with operations to fulfill this request.\\n");
        prompt.append("Available operations: insert, update, delete, replace, move\\n\\n");
        
        prompt.append("Respond in JSON format:\\n");
        prompt.append("{\n");
        prompt.append("  \"scope\": \"trip|day|node\",\n");
        prompt.append("  \"day\": 1,\n");
        prompt.append("  \"ops\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"op\": \"insert|update|delete|replace|move\",\n");
        prompt.append("      \"id\": \"node_id\",\n");
        prompt.append("      \"node\": { /* node data */ },\n");
        prompt.append("      \"position\": 0\n");
        prompt.append("    }\n");
        prompt.append("  ],\n");
        prompt.append("  \"preferences\": {\n");
        prompt.append("    \"userFirst\": true,\n");
        prompt.append("    \"autoApply\": false\n");
        prompt.append("  }\n");
        prompt.append("}");
        
        return prompt.toString();
    }
    
    /**
     * Parse intent result from LLM response.
     */
    private IntentResult parseIntentFromResponse(String response) {
        try {
            // Clean response (remove markdown formatting if present)
            String cleanResponse = cleanJsonResponse(response);
            
            // Parse JSON
            @SuppressWarnings("unchecked")
            Map<String, Object> jsonMap = objectMapper.readValue(cleanResponse, Map.class);
            
            String intent = (String) jsonMap.get("intent");
            String taskType = (String) jsonMap.get("taskType");
            
            @SuppressWarnings("unchecked")
            Map<String, Object> entities = (Map<String, Object>) jsonMap.get("entities");
            
            if (intent == null) {
                logger.warn("No intent found in LLM response");
                return null;
            }
            
            // Create IntentResult
            IntentResult result = new IntentResult();
            result.setIntent(intent);
            result.setTaskType(taskType != null ? taskType : mapIntentToTaskType(intent));
            result.setEntities(entities != null ? entities : new HashMap<>());
            
            // Set confidence if available
            Object confidence = jsonMap.get("confidence");
            if (confidence instanceof Number) {
                result.setConfidence(((Number) confidence).doubleValue());
            }
            
            return result;
            
        } catch (Exception e) {
            logger.error("Error parsing intent from response: {}", response, e);
            return null;
        }
    }
    
    /**
     * Parse change set from LLM response.
     */
    private ChangeSet parseChangeSetFromResponse(String response) {
        try {
            // Clean response
            String cleanResponse = cleanJsonResponse(response);
            
            // Parse JSON to ChangeSet
            ChangeSet changeSet = objectMapper.readValue(cleanResponse, ChangeSet.class);
            
            return changeSet;
            
        } catch (Exception e) {
            logger.error("Error parsing change set from response: {}", response, e);
            return null;
        }
    }
    
    /**
     * Clean JSON response by removing markdown formatting.
     */
    private String cleanJsonResponse(String response) {
        if (response == null) {
            return "{}";
        }
        
        // Remove markdown code blocks
        String cleaned = response.replaceAll("```json\\s*", "").replaceAll("```\\s*", "");
        
        // Find JSON object boundaries
        int startIndex = cleaned.indexOf('{');
        int endIndex = cleaned.lastIndexOf('}');
        
        if (startIndex >= 0 && endIndex > startIndex) {
            cleaned = cleaned.substring(startIndex, endIndex + 1);
        }
        
        return cleaned.trim();
    }
    
    /**
     * Map intent type to task type for agent routing.
     * Maps to actual task types that agents support.
     */
    private String mapIntentToTaskType(String intent) {
        switch (intent.toUpperCase()) {
            case "EDIT":
            case "MOVE_TIME":
            case "INSERT_PLACE":
            case "DELETE_NODE":
            case "REPLACE_NODE":
                return "edit"; // All editing operations go to EditorAgent
            case "BOOK":
                return "book"; // Booking operations go to BookingAgent
            case "ENRICH":
                return "enrich"; // Enrichment operations go to EnrichmentAgent
            case "EXPLAIN":
                return "explain"; // Information requests use explanation agent
            default:
                return "explain"; // Default to explanation for unknown intents
        }
    }
    
    /**
     * Create fallback intent when LLM classification fails.
     */
    private IntentResult createFallbackIntent(String text) {
        IntentResult fallback = new IntentResult();
        fallback.setIntent("EXPLAIN");
        fallback.setTaskType("explanation");
        fallback.setEntities(new HashMap<>());
        fallback.setConfidence(0.1); // Low confidence for fallback
        
        logger.debug("Created fallback intent for text: {}", text);
        return fallback;
    }
    
    /**
     * Create fallback change set when LLM generation fails.
     */
    private ChangeSet createFallbackChangeSet(String request) {
        ChangeSet fallback = new ChangeSet();
        fallback.setScope("trip");
        fallback.setOps(List.of()); // Empty operations
        
        ChangePreferences preferences = new ChangePreferences();
        preferences.setUserFirst(true);
        preferences.setAutoApply(false);
        fallback.setPreferences(preferences);
        
        logger.debug("Created fallback change set for request: {}", request);
        return fallback;
    }
    
    /**
     * Validate change set structure.
     */
    private boolean validateChangeSet(ChangeSet changeSet) {
        if (changeSet == null) {
            return false;
        }
        
        if (changeSet.getScope() == null) {
            logger.warn("Change set missing scope");
            return false;
        }
        
        if (changeSet.getOps() == null) {
            logger.warn("Change set missing operations");
            return false;
        }
        
        // Validate operations
        for (ChangeOperation op : changeSet.getOps()) {
            if (op.getOp() == null || op.getOp().trim().isEmpty()) {
                logger.warn("Change operation missing op type");
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Get available providers for debugging/monitoring.
     */
    public List<String> getAvailableProviders() {
        return providerMap.keySet().stream().sorted().collect(Collectors.toList());
    }
    
    /**
     * Check if a specific model is supported.
     */
    public boolean isModelSupported(String modelType) {
        return getProviderForModel(modelType) != null;
    }
}