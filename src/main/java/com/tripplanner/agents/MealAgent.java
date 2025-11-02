package com.tripplanner.agents;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripplanner.dto.*;
import com.tripplanner.service.AgentEventBus;
import com.tripplanner.service.AgentEventPublisher;
import com.tripplanner.service.ItineraryJsonService;
import com.tripplanner.service.NodeIdGenerator;
import com.tripplanner.service.ai.AiClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * MealAgent - Specialized agent for populating meal and dining nodes.
 * 
 * This agent focuses solely on adding detailed information to meal-type nodes
 * that were created as placeholders by SkeletonPlannerAgent.
 * 
 * Responsibilities:
 * - Add specific restaurant/dining place names
 * - Add cuisine types
 * - Add ambiance descriptions
 * - Add meal types (breakfast, lunch, dinner)
 * - Add dietary options
 * - Add dress code / reservation requirements
 * 
 * Does NOT handle: Activities, Transport, Costs, Coordinates
 * (Those are handled by specialized agents)
 * 
 * Processing Time: 8-12 seconds for a typical itinerary
 */
@Component
@ConditionalOnBean(AiClient.class)
public class MealAgent extends BaseAgent {
    
    private final AiClient aiClient;
    private final ObjectMapper objectMapper;
    private final ItineraryJsonService itineraryJsonService;
    private final AgentEventPublisher agentEventPublisher;
    private final NodeIdGenerator nodeIdGenerator;
    
    public MealAgent(AgentEventBus eventBus, AiClient aiClient, ObjectMapper objectMapper,
                    ItineraryJsonService itineraryJsonService, AgentEventPublisher agentEventPublisher,
                    NodeIdGenerator nodeIdGenerator) {
        super(eventBus, AgentEvent.AgentKind.ENRICHMENT);
        this.aiClient = aiClient;
        this.objectMapper = objectMapper;
        this.itineraryJsonService = itineraryJsonService;
        this.agentEventPublisher = agentEventPublisher;
        this.nodeIdGenerator = nodeIdGenerator;
    }
    
    @Override
    public AgentCapabilities getCapabilities() {
        AgentCapabilities capabilities = new AgentCapabilities();
        
        // Pipeline-only task: populate meal nodes
        capabilities.addSupportedTask("populate_meals");
        
        capabilities.setPriority(10);
        capabilities.setChatEnabled(false); // Pipeline-only, not for chat
        capabilities.setConfigurationValue("nodeType", "meal");
        capabilities.setConfigurationValue("parallel", true);
        
        return capabilities;
    }
    
    /**
     * Populate meal nodes with detailed information.
     */
    public void populateMeals(String itineraryId, NormalizedItinerary skeleton) {
        logger.info("=== MEAL AGENT ===");
        logger.info("Populating meal nodes for itinerary: {}", itineraryId);
        
        try {
            // Extract all meal nodes from skeleton
            List<MealContext> mealContexts = extractMealNodes(skeleton);
            
            if (mealContexts.isEmpty()) {
                logger.info("No meal nodes to populate");
                return;
            }
            
            logger.info("Found {} meal nodes to populate", mealContexts.size());
            
            // Populate meals with AI
            List<PopulatedMeal> populatedMeals = populateMealsWithAI(skeleton, mealContexts);
            
            // Update the itinerary with populated data
            updateItineraryWithMeals(itineraryId, skeleton, populatedMeals);
            
            logger.info("=== MEAL AGENT COMPLETE ===");
            logger.info("Populated {} meals", populatedMeals.size());
            
            // Publish agent completion event via WebSocket
            if (agentEventPublisher.hasActiveConnections(itineraryId)) {
                String execId = "agent_" + System.currentTimeMillis();
                agentEventPublisher.publishAgentComplete(itineraryId, execId, 
                    "MealAgent", populatedMeals.size());
            }
            
        } catch (Exception e) {
            logger.error("Failed to populate meals for itinerary: {}", itineraryId, e);
            // Don't throw - graceful degradation (keep placeholders)
        }
    }
    
    /**
     * Extract meal nodes from skeleton with context.
     */
    private List<MealContext> extractMealNodes(NormalizedItinerary skeleton) {
        List<MealContext> contexts = new ArrayList<>();
        
        for (NormalizedDay day : skeleton.getDays()) {
            if (day.getNodes() == null) continue;
            
            for (NormalizedNode node : day.getNodes()) {
                if ("meal".equals(node.getType())) {
                    String mealType = determineMealType(node.getTiming());
                    contexts.add(new MealContext(
                        node.getId(),
                        day.getDayNumber(),
                        day.getLocation(),
                        node.getTiming(),
                        mealType
                    ));
                }
            }
        }
        
        return contexts;
    }
    
    /**
     * Determine meal type based on timing.
     */
    private String determineMealType(NodeTiming timing) {
        if (timing == null || timing.getStartTime() == null) {
            return "meal";
        }
        
        try {
            // Convert epoch milliseconds to hour
            long epochMs = timing.getStartTime();
            java.time.Instant instant = java.time.Instant.ofEpochMilli(epochMs);
            java.time.LocalDateTime dateTime = java.time.LocalDateTime.ofInstant(
                instant, java.time.ZoneId.systemDefault());
            int hour = dateTime.getHour();
            
            if (hour >= 6 && hour < 11) return "breakfast";
            if (hour >= 11 && hour < 15) return "lunch";
            if (hour >= 15 && hour < 18) return "snack";
            return "dinner";
        } catch (Exception e) {
            return "meal";
        }
    }
    
    /**
     * Populate meals using AI.
     */
    private List<PopulatedMeal> populateMealsWithAI(NormalizedItinerary skeleton, 
                                                     List<MealContext> contexts) {
        
        String systemPrompt = buildMealSystemPrompt();
        String userPrompt = buildMealUserPrompt(skeleton, contexts);
        String schema = buildMealJsonSchema();
        
        logger.info("Calling AI to populate {} meals", contexts.size());
        
        String response = aiClient.generateStructuredContent(userPrompt, schema, systemPrompt);
        
        // Log full LLM response for analysis
        logger.info("=== MEAL AGENT - FULL LLM RESPONSE ===");
        logger.info("Contexts to populate: {}", contexts.size());
        logger.info("Raw Response: {}", response);
        logger.info("=== END MEAL AGENT RESPONSE ===");
        
        try {
            // Clean response by removing markdown formatting
            String cleanedResponse = cleanJsonResponse(response);
            logger.info("Cleaned Response: {}", cleanedResponse);
            com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(cleanedResponse);
            List<PopulatedMeal> meals = new ArrayList<>();
            
            if (root.has("meals") && root.get("meals").isArray()) {
                for (com.fasterxml.jackson.databind.JsonNode node : root.get("meals")) {
                    PopulatedMeal meal = objectMapper.treeToValue(node, PopulatedMeal.class);
                    meals.add(meal);
                }
            }
            
            return meals;
            
        } catch (Exception e) {
            logger.error("Failed to parse meal response", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Update itinerary with populated meal data.
     */
    private void updateItineraryWithMeals(String itineraryId, NormalizedItinerary skeleton,
                                          List<PopulatedMeal> populatedMeals) {
        
        Map<String, PopulatedMeal> mealMap = populatedMeals.stream()
            .collect(Collectors.toMap(PopulatedMeal::getNodeId, m -> m));
        
        for (NormalizedDay day : skeleton.getDays()) {
            if (day.getNodes() == null) continue;
            
            for (NormalizedNode node : day.getNodes()) {
                // Ensure node has ID
                nodeIdGenerator.ensureNodeHasId(node, day.getDayNumber(), skeleton);
                logger.debug("Ensuring node {} has ID for day {}", node.getTitle(), day.getDayNumber());
                
                if ("meal".equals(node.getType())) {
                    PopulatedMeal populated = mealMap.get(node.getId());
                    if (populated != null) {
                        node.setTitle(populated.getTitle());
                        
                        if (node.getDetails() == null) {
                            node.setDetails(new NodeDetails());
                        }
                        node.getDetails().setDescription(populated.getDescription());
                        node.getDetails().setCategory(populated.getCuisineType());
                        
                        if (node.getLocation() != null && populated.getLocationName() != null) {
                            node.getLocation().setName(populated.getLocationName());
                        }
                    }
                }
            }
        }
        
        try {
            skeleton.setUpdatedAt(System.currentTimeMillis());
            itineraryJsonService.updateItinerary(skeleton);
            logger.info("Saved itinerary with populated meals");
        } catch (Exception e) {
            logger.error("Failed to save itinerary with meals: {}", e.getMessage());
        }
    }
    
    private String buildMealSystemPrompt() {
        return """
            You are a culinary travel expert specializing in dining recommendations.
            
            Your task: Suggest SPECIFIC restaurants and dining places for meal slots.
            
            CRITICAL: Use the EXACT node IDs provided in the user prompt. Do NOT generate your own node IDs.
            
            Guidelines:
            1. Use the EXACT nodeId provided for each meal slot
            2. Provide real restaurant names when possible, or specific dining types
            3. Write appetizing descriptions (2-3 sentences)
            4. Match cuisine to destination and meal type
            5. Consider timing (breakfast, lunch, dinner, snack)
            6. Ensure variety across meals and days
            7. Include local specialties and authentic experiences
            
            Cuisine Types:
            - local: Traditional local cuisine
            - japanese: Sushi, ramen, izakaya
            - italian: Pasta, pizza, trattorias
            - chinese: Dim sum, hotpot, regional dishes
            - indian: Curry, tandoor, street food
            - french: Bistro, patisserie, fine dining
            - street_food: Markets, food stalls, casual
            - cafe: Coffee, pastries, light meals
            - international: Fusion, multi-cuisine
            
            Meal Types: breakfast, lunch, dinner, snack
            
            Be specific, practical, and ensure dining matches the destination and timing.
            """;
    }
    
    private String buildMealUserPrompt(NormalizedItinerary skeleton, List<MealContext> contexts) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("Destination: ").append(skeleton.getDays().get(0).getLocation()).append("\n");
        prompt.append("Total Days: ").append(skeleton.getDays().size()).append("\n");
        
        if (skeleton.getThemes() != null && skeleton.getThemes().contains("food")) {
            prompt.append("Traveler is interested in food experiences\n");
        }
        
        prompt.append("\nMeal slots to populate:\n");
        for (MealContext ctx : contexts) {
            prompt.append(String.format("- Day %d, Node ID: %s, Type: %s, Time: %s\n",
                ctx.dayNumber, ctx.nodeId, ctx.mealType,
                ctx.timing != null ? ctx.timing.getStartTime() : "TBD"));
        }
        
        prompt.append("\nCRITICAL: Use the EXACT node IDs listed above. Do NOT generate your own node IDs.");
        prompt.append("\nProvide specific restaurant/dining recommendations for each slot.");
        prompt.append("\nEnsure variety and include both local specialties and familiar options.");
        
        return prompt.toString();
    }
    
    private String buildMealJsonSchema() {
        return """
            {
              "type": "object",
              "properties": {
                "meals": {
                  "type": "array",
                  "items": {
                    "type": "object",
                    "properties": {
                      "nodeId": { "type": "string" },
                      "title": { "type": "string" },
                      "description": { "type": "string" },
                      "cuisineType": { 
                        "type": "string",
                        "enum": ["local", "japanese", "italian", "chinese", "indian", 
                                "french", "street_food", "cafe", "international"]
                      },
                      "mealType": {
                        "type": "string",
                        "enum": ["breakfast", "lunch", "dinner", "snack"]
                      },
                      "locationName": { "type": "string" }
                    },
                    "required": ["nodeId", "title", "description", "cuisineType", "mealType"]
                  }
                }
              },
              "required": ["meals"]
            }
            """;
    }
    
    @Override
    protected <T> T executeInternal(String itineraryId, AgentRequest<T> request) {
        NormalizedItinerary skeleton = request.getData(NormalizedItinerary.class);
        populateMeals(itineraryId, skeleton);
        return (T) skeleton;
    }
    
    @Override
    protected String getAgentName() {
        return "Meal Agent";
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
    
    // Helper classes
    
    private static class MealContext {
        String nodeId;
        int dayNumber;
        String dayLocation;
        NodeTiming timing;
        String mealType;
        
        public MealContext(String nodeId, int dayNumber, String dayLocation, 
                          NodeTiming timing, String mealType) {
            this.nodeId = nodeId;
            this.dayNumber = dayNumber;
            this.dayLocation = dayLocation;
            this.timing = timing;
            this.mealType = mealType;
        }
    }
    
    public static class PopulatedMeal {
        private String nodeId;
        private String title;
        private String description;
        private String cuisineType;
        private String mealType;
        private String locationName;
        
        // Getters and setters
        public String getNodeId() { return nodeId; }
        public void setNodeId(String nodeId) { this.nodeId = nodeId; }
        
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public String getCuisineType() { return cuisineType; }
        public void setCuisineType(String cuisineType) { this.cuisineType = cuisineType; }
        
        public String getMealType() { return mealType; }
        public void setMealType(String mealType) { this.mealType = mealType; }
        
        public String getLocationName() { return locationName; }
        public void setLocationName(String locationName) { this.locationName = locationName; }
    }
}

