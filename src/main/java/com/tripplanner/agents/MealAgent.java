package com.tripplanner.agents;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripplanner.dto.*;
import com.tripplanner.enums.ProcessingState;
import com.tripplanner.exception.ValidationException;
import com.tripplanner.service.agents.AgentEventBus;
import com.tripplanner.service.agents.AgentEventPublisher;
import com.tripplanner.service.ItineraryJsonService;
import com.tripplanner.service.ItineraryValidator;
import com.tripplanner.service.ItineraryValidator.ValidationResult;
import com.tripplanner.service.llm.LLMSchemaValidator;
import com.tripplanner.service.MealMetadataService;
import com.tripplanner.service.utilities.NodeIdGenerator;
import com.tripplanner.service.ai.AiClient;
import com.tripplanner.dto.tools.NodeIdRequest;
import com.tripplanner.dto.tools.NodeIdResponse;
import com.tripplanner.dto.tools.ConstraintCheckRequest;
import com.tripplanner.dto.tools.ConstraintCheckResult;
import com.tripplanner.dto.tools.SchemaValidationRequest;
import com.tripplanner.dto.tools.SchemaValidationResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

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
    private final LLMSchemaValidator schemaValidator;
    private final ItineraryValidator itineraryValidator;
    private final MealMetadataService mealMetadataService;
    private final RestTemplate restTemplate;
    
    // Feature flags for tool integration
    @Value("${features.meal-tools.enabled:false}")
    private boolean mealToolsEnabled;
    
    @Value("${features.meal-tools.fallback-on-error:true}")
    private boolean fallbackOnError;
    
    public MealAgent(AgentEventBus eventBus, AiClient aiClient, ObjectMapper objectMapper,
                    ItineraryJsonService itineraryJsonService, AgentEventPublisher agentEventPublisher,
                    NodeIdGenerator nodeIdGenerator, LLMSchemaValidator schemaValidator,
                    ItineraryValidator itineraryValidator, MealMetadataService mealMetadataService) {
        super(eventBus, AgentEvent.AgentKind.ENRICHMENT);
        this.aiClient = aiClient;
        this.objectMapper = objectMapper;
        this.itineraryJsonService = itineraryJsonService;
        this.agentEventPublisher = agentEventPublisher;
        this.nodeIdGenerator = nodeIdGenerator;
        this.schemaValidator = schemaValidator;
        this.itineraryValidator = itineraryValidator;
        this.mealMetadataService = mealMetadataService;
        this.restTemplate = new RestTemplate();
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
            emitProgress(itineraryId, 10, "Loading meal data", "loading");
            
            // Extract all meal nodes from skeleton
            List<MealContext> mealContexts = extractMealNodes(skeleton);
            
            if (mealContexts.isEmpty()) {
                logger.info("No meal nodes to populate");
                emitProgress(itineraryId, 100, "No meals to populate", "complete");
                return;
            }
            
            logger.info("Found {} meal nodes to populate", mealContexts.size());
            emitProgress(itineraryId, 30, 
                String.format("Populating %d meals", mealContexts.size()), 
                "populating");
            
            // Populate meals with AI
            List<PopulatedMeal> populatedMeals = populateMealsWithAI(skeleton, mealContexts);
            
            emitProgress(itineraryId, 70, "Saving meal data", "saving");
            
            // Update the itinerary with populated data
            updateItineraryWithMeals(itineraryId, skeleton, populatedMeals);
            
            emitProgress(itineraryId, 100, 
                String.format("Populated %d meals", populatedMeals.size()), 
                "complete");
            
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
            emitProgress(itineraryId, 0, "Failed to populate meals", "error");
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
            
            for (int i = 0; i < day.getNodes().size(); i++) {
                NormalizedNode node = day.getNodes().get(i);
                
                if ("meal".equals(node.getType())) {
                    String mealType = determineMealType(node.getTiming());
                    
                    // Find nearby activities
                    NormalizedNode previousActivity = findPreviousActivity(day, i);
                    NormalizedNode nextActivity = findNextActivity(day, i);
                    
                    contexts.add(new MealContext(
                        node.getId(),
                        day.getDayNumber(),
                        day.getLocation(),
                        node.getTiming(),
                        mealType,
                        previousActivity,  // NEW
                        nextActivity       // NEW
                    ));
                }
            }
        }
        
        return contexts;
    }
    
    /**
     * Find the previous activity node before a meal.
     */
    private NormalizedNode findPreviousActivity(NormalizedDay day, int mealIndex) {
        try {
            // Look backwards for the nearest activity node
            for (int i = mealIndex - 1; i >= 0; i--) {
                NormalizedNode node = day.getNodes().get(i);
                if ("attraction".equals(node.getType()) || "activity".equals(node.getType())) {
                    return node;
                }
            }
        } catch (Exception e) {
            logger.warn("Error finding previous activity: {}", e.getMessage());
        }
        return null;
    }
    
    /**
     * Find the next activity node after a meal.
     */
    private NormalizedNode findNextActivity(NormalizedDay day, int mealIndex) {
        try {
            // Look forwards for the nearest activity node
            for (int i = mealIndex + 1; i < day.getNodes().size(); i++) {
                NormalizedNode node = day.getNodes().get(i);
                if ("attraction".equals(node.getType()) || "activity".equals(node.getType())) {
                    return node;
                }
            }
        } catch (Exception e) {
            logger.warn("Error finding next activity: {}", e.getMessage());
        }
        return null;
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
            // IMPROVED: Validate response against schema before parsing
            LLMSchemaValidator.ValidationResult validationResult = schemaValidator.validateWithLogging(
                response, schema, "MealAgent");
            
            if (!validationResult.isValid()) {
                String errorMsg = schemaValidator.getUserFriendlyError(validationResult);
                logger.error("Schema validation failed for MealAgent: {}", errorMsg);
                
                // Return empty list on validation failure (graceful degradation)
                return new ArrayList<>();
            }
            
            // Use validated data
            com.fasterxml.jackson.databind.JsonNode root = validationResult.getData();
            logger.info("Schema validation passed for MealAgent");
            
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
     * Apply meal data to skeleton nodes (extracted for retry logic).
     */
    private void applyMealsToSkeleton(NormalizedItinerary skeleton,
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
                    // Mark node as being processed
                    node.setProcessingState(ProcessingState.ENRICHING);
                    node.addProcessedBy("MealAgent");
                    
                    PopulatedMeal populated = mealMap.get(node.getId());
                    if (populated != null) {
                        node.setTitle(populated.getTitle());
                        
                        if (node.getDetails() == null) {
                            node.setDetails(new NodeDetails());
                        }
                        node.getDetails().setDescription(populated.getDescription());
                        node.getDetails().setCategory(populated.getCuisineType());
                        
                        // Update location name - use locationName if provided, otherwise use title as fallback
                        if (node.getLocation() != null) {
                            String locationName = populated.getLocationName();
                            if (locationName == null || locationName.trim().isEmpty()) {
                                // Fallback to title if locationName not provided
                                locationName = populated.getTitle();
                                logger.debug("Using title as location name for node {}: {}", node.getId(), locationName);
                            }
                            node.getLocation().setName(locationName);
                        }
                        
                        // Mark node as successfully processed
                        node.setProcessingState(ProcessingState.ENRICHED);
                    } else {
                        // Mark node as failed if no populated data found
                        node.setProcessingState(ProcessingState.FAILED);
                        node.setLastError("No meal data found for node");
                    }
                }
            }
        }
    }
    
    /**
     * Update itinerary with populated meal data.
     */
    private void updateItineraryWithMeals(String itineraryId, NormalizedItinerary skeleton,
                                          List<PopulatedMeal> populatedMeals) {
        
        // Apply meals to skeleton
        applyMealsToSkeleton(skeleton, populatedMeals);
        
        // Populate metadata for all meal nodes
        // Determine budget tier from budget range
        String budgetTier = "medium";
        if (skeleton.getBudgetMax() != null) {
            if (skeleton.getBudgetMax() < 1000) {
                budgetTier = "budget";
            } else if (skeleton.getBudgetMax() > 3000) {
                budgetTier = "luxury";
            }
        }
        
        for (NormalizedDay day : skeleton.getDays()) {
            if (day.getNodes() != null) {
                for (NormalizedNode node : day.getNodes()) {
                    if ("meal".equals(node.getType())) {
                        mealMetadataService.populateNodeMetadata(node, budgetTier);
                    }
                }
            }
        }
        logger.info("Populated metadata for all meal nodes");
        
        // Validate before save
        ValidationResult validationResult = itineraryValidator.validate(skeleton);
        if (!validationResult.isValid()) {
            logger.error("Validation failed for itinerary {}: {}", itineraryId, validationResult.getErrors());
            List<String> errorMessages = validationResult.getErrors().stream()
                .map(error -> error.getCategory() + ": " + error.getMessage())
                .collect(java.util.stream.Collectors.toList());
            throw new ValidationException(errorMessages, "Itinerary validation failed");
        }
        if (!validationResult.getWarnings().isEmpty()) {
            logger.warn("Validation warnings for itinerary {}: {}", itineraryId, validationResult.getWarnings());
        }
        
        // Save with optimistic locking and retry
        int maxRetries = 3;
        int retryCount = 0;
        boolean saved = false;
        
        while (!saved && retryCount < maxRetries) {
            try {
                skeleton.setUpdatedAt(System.currentTimeMillis());
                itineraryJsonService.updateItineraryWithLock(skeleton);
                logger.info("Saved itinerary with populated meals (with lock)");
                saved = true;
            } catch (com.tripplanner.exception.ConcurrentModificationException e) {
                retryCount++;
                logger.error("Concurrent modification detected (attempt {}/{}): {}", 
                           retryCount, maxRetries, e.getMessage());
                
                if (retryCount < maxRetries) {
                    logger.info("Reloading itinerary and retrying save...");
                    Optional<NormalizedItinerary> reloaded = itineraryJsonService.getItinerary(skeleton.getItineraryId());
                    if (reloaded.isPresent()) {
                        skeleton = reloaded.get();
                        // Re-apply meal changes to reloaded skeleton
                        applyMealsToSkeleton(skeleton, populatedMeals);
                        logger.info("Re-applied meal changes to reloaded itinerary");
                    } else {
                        logger.error("Failed to reload itinerary for retry");
                        throw e;
                    }
                } else {
                    logger.error("Max retries ({}) exceeded, giving up", maxRetries);
                    throw e;
                }
            } catch (Exception e) {
                logger.error("Failed to save itinerary with meals: {}", e.getMessage());
                throw new RuntimeException("Failed to save meal data", e);
            }
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
            8. IMPORTANT: For locationName, provide the SPECIFIC restaurant/place name, NOT just the district
               - GOOD: "Sushi Zanmai Tsukiji", "Ichiran Ramen Shibuya", "Gonpachi Nishi-Azabu"
               - BAD: "Tsukiji", "Shibuya", "Nishi-Azabu"
            
            Location Awareness (CRITICAL):
            - ALWAYS suggest restaurants IN THE SAME CITY as the day's location
            - NEVER suggest restaurants from other cities (e.g., no Zermatt restaurants on Interlaken days)
            - ALWAYS suggest restaurants near the previous or next activity
            - Minimize travel time between activities and meals
            - If previous activity is in Asakusa, suggest restaurants in Asakusa
            - If next activity is in Shibuya, suggest restaurants in Shibuya or nearby
            - DO NOT suggest restaurants in distant areas (>30 minutes travel)
            - DO NOT suggest restaurants in different cities (>2 hours travel)
            
            Example 1 (Same City):
            Day Location: Tokyo
            Previous activity: Senso-ji Temple (Asakusa)
            Next activity: Tokyo Skytree (Sumida)
            → Suggest: Restaurants in Asakusa or Sumida, NOT in Shibuya or Shinjuku
            
            Example 2 (Wrong City - DO NOT DO THIS):
            Day Location: Interlaken
            ❌ WRONG: Restaurant Spycher (Zermatt) - This is in a DIFFERENT CITY
            ✅ CORRECT: Restaurant Goldener Anker (Interlaken) - Same city
            
            If no activity context available, suggest restaurants in the day's main location.
            
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
            The locationName field should be specific enough to find the exact restaurant on Google Maps.
            """;
    }
    
    private String buildMealUserPrompt(NormalizedItinerary skeleton, List<MealContext> contexts) {
        StringBuilder prompt = new StringBuilder();
        
        // IMPROVED: Group contexts by day to show city-specific context
        Map<Integer, List<MealContext>> contextsByDay = new HashMap<>();
        for (MealContext ctx : contexts) {
            contextsByDay.computeIfAbsent(ctx.dayNumber, k -> new ArrayList<>()).add(ctx);
        }
        
        prompt.append("Trip Overview:\n");
        prompt.append("Total Days: ").append(skeleton.getDays().size()).append("\n");
        
        if (skeleton.getThemes() != null && skeleton.getThemes().contains("food")) {
            prompt.append("Traveler is interested in food experiences\n");
        }
        
        // CRITICAL: Include user's custom instructions/constraints
        // Pay special attention to dietary restrictions
        if (skeleton.getConstraints() != null && !skeleton.getConstraints().isEmpty()) {
            prompt.append("\n=== CRITICAL USER REQUIREMENTS (MUST FOLLOW) ===\n");
            for (String constraint : skeleton.getConstraints()) {
                prompt.append("- ").append(constraint).append("\n");
            }
            prompt.append("All meal recommendations MUST comply with these requirements.\n");
            prompt.append("Pay special attention to dietary restrictions (vegetarian, vegan, halal, kosher, allergies).\n");
            prompt.append("If a requirement cannot be met, skip that meal slot.\n\n");
        }
        
        prompt.append("\n=== MEAL SLOTS TO POPULATE (BY DAY) ===\n");
        
        // CRITICAL: Show each day's location explicitly to prevent wrong-city restaurants
        for (Map.Entry<Integer, List<MealContext>> entry : contextsByDay.entrySet()) {
            int dayNum = entry.getKey();
            List<MealContext> dayContexts = entry.getValue();
            
            // Get the day's location
            String dayLocation = dayContexts.get(0).dayLocation;
            
            prompt.append(String.format("\n** DAY %d - LOCATION: %s **\n", dayNum, dayLocation));
            prompt.append(String.format("CRITICAL: All restaurants for Day %d MUST be in %s, NOT in any other city!\n", 
                                       dayNum, dayLocation));
            
            for (MealContext ctx : dayContexts) {
                prompt.append(String.format("  - Node ID: %s, Type: %s, Time: %s\n",
                    ctx.nodeId, ctx.mealType,
                    ctx.timing != null ? ctx.timing.getStartTime() : "TBD"));
                
                // Add activity context for location awareness
                if (ctx.previousActivity != null) {
                    prompt.append("    Previous activity: ")
                          .append(ctx.previousActivity.getTitle());
                    if (ctx.previousActivity.getLocation() != null && ctx.previousActivity.getLocation().getName() != null) {
                        prompt.append(" at ").append(ctx.previousActivity.getLocation().getName());
                    }
                    prompt.append("\n");
                }
                
                if (ctx.nextActivity != null) {
                    prompt.append("    Next activity: ")
                          .append(ctx.nextActivity.getTitle());
                    if (ctx.nextActivity.getLocation() != null && ctx.nextActivity.getLocation().getName() != null) {
                        prompt.append(" at ").append(ctx.nextActivity.getLocation().getName());
                    }
                    prompt.append("\n");
                }
                
                if (ctx.previousActivity != null || ctx.nextActivity != null) {
                    prompt.append("    IMPORTANT: Suggest restaurant near these activity locations in ").append(dayLocation).append("\n");
                }
            }
        }
        
        prompt.append("\n=== CRITICAL RULES ===\n");
        prompt.append("1. Use the EXACT node IDs listed above. Do NOT generate your own node IDs.\n");
        prompt.append("2. Each restaurant MUST be in the CORRECT CITY for that day.\n");
        prompt.append("3. Do NOT suggest restaurants from other cities (e.g., no Kuala Lumpur restaurants on Penang days).\n");
        prompt.append("4. Provide specific restaurant names that are searchable on Google Maps.\n");
        prompt.append("5. Ensure variety and include both local specialties and familiar options.\n");
        prompt.append("6. All selections must respect the user requirements listed above, especially dietary restrictions.\n");
        
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
                      "locationName": { 
                        "type": "string",
                        "description": "SPECIFIC restaurant name (e.g., 'Sushi Zanmai Tsukiji' not 'Tsukiji', 'Ichiran Ramen Shibuya' not 'Shibuya'). Must be searchable on Google Maps."
                      }
                    },
                    "required": ["nodeId", "title", "description", "cuisineType", "mealType", "locationName"]
                  }
                }
              },
              "required": ["meals"]
            }
            """;
    }
    
    // ========== MEAL AGENT - TOOL INTEGRATION METHODS ==========
    
    /**
     * Generate node ID using the Generate Node ID tool.
     */
    private String generateNodeIdViaTool(String itineraryId, Integer dayNumber, String nodeType) {
        if (!mealToolsEnabled) {
            return nodeIdGenerator.generateNodeId(nodeType, dayNumber);
        }
        
        NodeIdRequest request = new NodeIdRequest(itineraryId, dayNumber, nodeType);
        
        try {
            logger.debug("Calling Generate Node ID tool");
            
            NodeIdResponse response = restTemplate.postForObject(
                "http://localhost:8080/api/v1/tools/generate-node-id",
                request,
                NodeIdResponse.class
            );
            
            if (response != null && response.isSuccess()) {
                logger.debug("Generate Node ID tool succeeded: {}", response.getNodeId());
                return response.getNodeId();
            } else if (fallbackOnError) {
                logger.warn("Generate Node ID tool failed, using fallback");
                return nodeIdGenerator.generateNodeId(nodeType, dayNumber);
            } else {
                logger.error("Generate Node ID tool failed and fallback disabled");
                return nodeIdGenerator.generateNodeId(nodeType, dayNumber);
            }
            
        } catch (Exception e) {
            logger.error("Generate Node ID tool error: {}", e.getMessage());
            if (fallbackOnError) {
                return nodeIdGenerator.generateNodeId(nodeType, dayNumber);
            }
            throw e;
        }
    }
    
    /**
     * Check user constraints using the Check User Constraints tool.
     * Focus on dietary restrictions and budget for meals.
     */
    private ConstraintCheckResult checkConstraintsViaTool(String itineraryId) {
        if (!mealToolsEnabled) {
            return new ConstraintCheckResult(); // Skip if disabled
        }
        
        ConstraintCheckRequest request = new ConstraintCheckRequest(itineraryId);
        request.setCheckDietary(true);  // Critical for meals
        request.setCheckBudget(true);
        request.setCheckPartySize(true);
        
        try {
            logger.debug("Calling Check User Constraints tool");
            
            ConstraintCheckResult result = restTemplate.postForObject(
                "http://localhost:8080/api/v1/tools/check-user-constraints",
                request,
                ConstraintCheckResult.class
            );
            
            if (result != null) {
                if (!result.isValid()) {
                    logger.warn("Constraint violations detected:");
                    for (var violation : result.getViolations()) {
                        logger.warn("  - {}: {}", violation.getType(), violation.getMessage());
                    }
                }
                if (!result.getWarnings().isEmpty()) {
                    logger.info("Constraint warnings:");
                    for (String warning : result.getWarnings()) {
                        logger.info("  - {}", warning);
                    }
                }
                return result;
            } else if (fallbackOnError) {
                logger.warn("Check User Constraints tool returned null, continuing");
                return new ConstraintCheckResult();
            } else {
                return new ConstraintCheckResult();
            }
            
        } catch (Exception e) {
            logger.error("Check User Constraints tool error: {}", e.getMessage());
            if (fallbackOnError) {
                return new ConstraintCheckResult();
            }
            throw e;
        }
    }
    
    /**
     * Validate LLM schema using the Validate Schema tool.
     */
    private boolean validateSchemaViaTool(String jsonOutput, String jsonSchema) {
        if (!mealToolsEnabled) {
            // Use existing validator
            return schemaValidator.validateWithLogging(jsonOutput, jsonSchema, "MealAgent").isValid();
        }
        
        SchemaValidationRequest request = new SchemaValidationRequest();
        request.setJsonOutput(jsonOutput);
        request.setJsonSchema(jsonSchema);
        request.setCleanBeforeValidation(true);
        
        try {
            logger.debug("Calling Validate Schema tool");
            
            SchemaValidationResult result = restTemplate.postForObject(
                "http://localhost:8080/api/v1/tools/validate-schema",
                request,
                SchemaValidationResult.class
            );
            
            if (result != null) {
                if (!result.isValid()) {
                    logger.error("Schema validation failed:");
                    for (String error : result.getErrors()) {
                        logger.error("  - {}", error);
                    }
                }
                return result.isValid();
            } else if (fallbackOnError) {
                logger.warn("Validate Schema tool returned null, using fallback");
                return schemaValidator.validateWithLogging(jsonOutput, jsonSchema, "MealAgent").isValid();
            } else {
                return false;
            }
            
        } catch (Exception e) {
            logger.error("Validate Schema tool error: {}", e.getMessage());
            if (fallbackOnError) {
                return schemaValidator.validateWithLogging(jsonOutput, jsonSchema, "MealAgent").isValid();
            }
            return false;
        }
    }
    
    // ========== END MEAL AGENT TOOL INTEGRATION ==========
    
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
        NormalizedNode previousActivity;  // NEW
        NormalizedNode nextActivity;      // NEW
        
        public MealContext(String nodeId, int dayNumber, String dayLocation, 
                          NodeTiming timing, String mealType) {
            this.nodeId = nodeId;
            this.dayNumber = dayNumber;
            this.dayLocation = dayLocation;
            this.timing = timing;
            this.mealType = mealType;
        }
        
        public MealContext(String nodeId, int dayNumber, String dayLocation, 
                          NodeTiming timing, String mealType,
                          NormalizedNode previousActivity, NormalizedNode nextActivity) {
            this.nodeId = nodeId;
            this.dayNumber = dayNumber;
            this.dayLocation = dayLocation;
            this.timing = timing;
            this.mealType = mealType;
            this.previousActivity = previousActivity;
            this.nextActivity = nextActivity;
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

