package com.tripplanner.agents;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripplanner.dto.*;
import com.tripplanner.enums.ProcessingState;
import com.tripplanner.enums.ToolType;
import com.tripplanner.exception.ValidationException;
import com.tripplanner.service.metadata.ActivityMetadataService;
import com.tripplanner.service.agents.AgentEventBus;
import com.tripplanner.service.agents.AgentEventPublisher;
import com.tripplanner.service.ItineraryJsonService;
import com.tripplanner.service.ItineraryValidator;
import com.tripplanner.service.ItineraryValidator.ValidationResult;
import com.tripplanner.service.llm.LLMSchemaValidator;
import com.tripplanner.service.utilities.NodeIdGenerator;
import com.tripplanner.service.ai.AiClient;
import com.tripplanner.service.WeatherService;
import com.tripplanner.service.ActivitySuitabilityService;
import com.tripplanner.service.cache.ToolCacheService;
import com.tripplanner.util.ToolCacheKeyGenerator;
import com.tripplanner.dto.tools.SuggestBestTimeRequest;
import com.tripplanner.dto.tools.SuggestBestTimeResult;
import com.tripplanner.dto.tools.SuggestBestTimeResult.TimeSlot;
import com.tripplanner.dto.tools.NodeIdRequest;
import com.tripplanner.dto.tools.NodeIdResponse;
import com.tripplanner.dto.tools.ConstraintCheckRequest;
import com.tripplanner.dto.tools.ConstraintCheckResult;
import com.tripplanner.dto.tools.SchemaValidationRequest;
import com.tripplanner.dto.tools.SchemaValidationResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ActivityAgent - Specialized agent for populating attraction and activity nodes.
 * 
 * This agent focuses solely on adding detailed information to attraction-type nodes
 * that were created as placeholders by SkeletonPlannerAgent.
 * 
 * Responsibilities:
 * - Add specific place names (e.g., "Senso-ji Temple" not "Morning Activity")
 * - Add detailed descriptions
 * - Add categories (museum, landmark, park, etc.)
 * - Add duration estimates
 * - Add opening hours
 * - Add difficulty/accessibility info
 * 
 * Does NOT handle: Meals, Transport, Accommodation, Costs, Coordinates
 * (Those are handled by specialized agents)
 * 
 * Processing Time: 10-15 seconds for a typical itinerary
 */
@Component
@ConditionalOnBean(AiClient.class)
public class ActivityAgent extends BaseAgent {
    
    private final AiClient aiClient;
    private final ObjectMapper objectMapper;
    private final ItineraryJsonService itineraryJsonService;
    private final AgentEventPublisher agentEventPublisher;
    private final NodeIdGenerator nodeIdGenerator;
    private final LLMSchemaValidator schemaValidator;
    private final ItineraryValidator itineraryValidator;
    private final ActivityMetadataService activityMetadataService;
    private final WeatherService weatherService;
    private final ActivitySuitabilityService activitySuitabilityService;
    private final RestTemplate restTemplate;
    
    // Optional: Tool cache service (only injected if enabled)
    @Autowired(required = false)
    private ToolCacheService toolCacheService;
    
    // Feature flags
    @Value("${features.weather-tools.enabled:false}")
    private boolean weatherToolsEnabled;
    
    @Value("${features.weather-tools.fallback-on-error:true}")
    private boolean fallbackOnError;
    
    @Value("${features.weather-tools.log-comparisons:true}")
    private boolean logComparisons;
    
    public ActivityAgent(AgentEventBus eventBus, AiClient aiClient, ObjectMapper objectMapper,
                        ItineraryJsonService itineraryJsonService, AgentEventPublisher agentEventPublisher,
                        NodeIdGenerator nodeIdGenerator, LLMSchemaValidator schemaValidator,
                        ItineraryValidator itineraryValidator, ActivityMetadataService activityMetadataService,
                        WeatherService weatherService, ActivitySuitabilityService activitySuitabilityService) {
        super(eventBus, AgentEvent.AgentKind.ENRICHMENT);
        this.aiClient = aiClient;
        this.objectMapper = objectMapper;
        this.itineraryJsonService = itineraryJsonService;
        this.agentEventPublisher = agentEventPublisher;
        this.nodeIdGenerator = nodeIdGenerator;
        this.schemaValidator = schemaValidator;
        this.itineraryValidator = itineraryValidator;
        this.activityMetadataService = activityMetadataService;
        this.weatherService = weatherService;
        this.activitySuitabilityService = activitySuitabilityService;
        this.restTemplate = new RestTemplate();
    }
    
    @Override
    public AgentCapabilities getCapabilities() {
        AgentCapabilities capabilities = new AgentCapabilities();
        
        // Pipeline-only task: populate attraction nodes
        capabilities.addSupportedTask("populate_attractions");
        
        capabilities.setPriority(10);
        capabilities.setChatEnabled(false); // Pipeline-only, not for chat
        capabilities.setConfigurationValue("nodeType", "attraction");
        capabilities.setConfigurationValue("parallel", true);
        
        return capabilities;
    }
    
    /**
     * Populate attraction nodes with detailed information.
     */
    public void populateAttractions(String itineraryId, NormalizedItinerary skeleton) {
        logger.info("=== ACTIVITY AGENT ===");
        logger.info("Populating attraction nodes for itinerary: {}", itineraryId);
        
        try {
            emitProgress(itineraryId, 10, "Loading attraction data", "loading");
            
            // Extract all attraction nodes from skeleton
            List<AttractionContext> attractionContexts = extractAttractionNodes(skeleton);
            
            if (attractionContexts.isEmpty()) {
                logger.info("No attraction nodes to populate");
                emitProgress(itineraryId, 100, "No attractions to populate", "complete");
                return;
            }
            
            logger.info("Found {} attraction nodes to populate", attractionContexts.size());
            emitProgress(itineraryId, 30, 
                String.format("Populating %d attractions", attractionContexts.size()), 
                "populating");
            
            // Populate attractions with AI
            List<PopulatedAttraction> populatedAttractions = populateAttractionsWithAI(
                skeleton, attractionContexts);
            
            // Validate activity durations
            emitProgress(itineraryId, 60, "Validating activity durations", "validating");
            List<PopulatedAttraction> validatedAttractions = validateActivityDurations(
                populatedAttractions, skeleton);
            
            // FEATURE FLAG: Weather-aware scheduling
            if (weatherToolsEnabled) {
                emitProgress(itineraryId, 65, "Optimizing activity timing based on weather", "weather-optimization");
                validatedAttractions = optimizeActivityTimingWithWeather(
                    itineraryId, validatedAttractions, skeleton);
            }
            
            emitProgress(itineraryId, 70, "Saving attraction data", "saving");
            
            // Update the itinerary with populated data
            updateItineraryWithAttractions(itineraryId, skeleton, validatedAttractions);
            
            emitProgress(itineraryId, 100, 
                String.format("Populated %d attractions", populatedAttractions.size()), 
                "complete");
            
            logger.info("=== ACTIVITY AGENT COMPLETE ===");
            logger.info("Populated {} attractions", populatedAttractions.size());
            
            // Publish agent completion event via WebSocket
            if (agentEventPublisher.hasActiveConnections(itineraryId)) {
                String execId = "agent_" + System.currentTimeMillis();
                agentEventPublisher.publishAgentComplete(itineraryId, execId, 
                    "ActivityAgent", populatedAttractions.size());
            }
            
        } catch (Exception e) {
            logger.error("Failed to populate attractions for itinerary: {}", itineraryId, e);
            emitProgress(itineraryId, 0, "Failed to populate attractions", "error");
            // Don't throw - graceful degradation (keep placeholders)
        }
    }
    
    /**
     * Extract attraction nodes from skeleton with context.
     */
    private List<AttractionContext> extractAttractionNodes(NormalizedItinerary skeleton) {
        List<AttractionContext> contexts = new ArrayList<>();
        
        for (NormalizedDay day : skeleton.getDays()) {
            if (day.getNodes() == null) continue;
            
            for (NormalizedNode node : day.getNodes()) {
                if ("attraction".equals(node.getType())) {
                    contexts.add(new AttractionContext(
                        node.getId(),
                        day.getDayNumber(),
                        day.getLocation(),
                        node.getTiming()
                    ));
                }
            }
        }
        
        return contexts;
    }
    
    /**
     * Populate attractions using AI.
     */
    private List<PopulatedAttraction> populateAttractionsWithAI(
            NormalizedItinerary skeleton, List<AttractionContext> contexts) {
        
        String systemPrompt = buildActivitySystemPrompt();
        String userPrompt = buildActivityUserPrompt(skeleton, contexts);
        String schema = buildActivityJsonSchema();
        
        logger.info("Calling AI to populate {} attractions", contexts.size());
        
        String response = aiClient.generateStructuredContent(userPrompt, schema, systemPrompt);
        
        // Log full LLM response for analysis
        logger.info("=== ACTIVITY AGENT - FULL LLM RESPONSE ===");
        logger.info("Contexts to populate: {}", contexts.size());
        logger.info("Raw Response: {}", response);
        logger.info("=== END ACTIVITY AGENT RESPONSE ===");
        
        try {
            // IMPROVED: Validate response against schema before parsing
            LLMSchemaValidator.ValidationResult validationResult = schemaValidator.validateWithLogging(
                response, schema, "ActivityAgent");
            
            if (!validationResult.isValid()) {
                String errorMsg = schemaValidator.getUserFriendlyError(validationResult);
                logger.error("Schema validation failed for ActivityAgent: {}", errorMsg);
                
                // Return empty list on validation failure (graceful degradation)
                return new ArrayList<>();
            }
            
            // Use validated data
            com.fasterxml.jackson.databind.JsonNode root = validationResult.getData();
            logger.info("Schema validation passed for ActivityAgent");
            
            List<PopulatedAttraction> attractions = new ArrayList<>();
            
            if (root.has("attractions") && root.get("attractions").isArray()) {
                for (com.fasterxml.jackson.databind.JsonNode node : root.get("attractions")) {
                    PopulatedAttraction attraction = objectMapper.treeToValue(node, PopulatedAttraction.class);
                    attractions.add(attraction);
                }
            }
            
            return attractions;
            
        } catch (Exception e) {
            logger.error("Failed to parse activity response", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Validate activity durations to ensure feasible schedules.
     */
    private List<PopulatedAttraction> validateActivityDurations(
            List<PopulatedAttraction> attractions,
            NormalizedItinerary skeleton) {
        
        try {
            // Group by day
            Map<Integer, List<PopulatedAttraction>> byDay = new HashMap<>();
            for (PopulatedAttraction attraction : attractions) {
                // Find which day this attraction belongs to
                int dayNumber = findDayNumberForNode(skeleton, attraction.getNodeId());
                if (dayNumber > 0) {
                    byDay.computeIfAbsent(dayNumber, k -> new ArrayList<>()).add(attraction);
                }
            }
            
            List<PopulatedAttraction> validated = new ArrayList<>();
            
            for (Map.Entry<Integer, List<PopulatedAttraction>> entry : byDay.entrySet()) {
                int dayNumber = entry.getKey();
                List<PopulatedAttraction> dayAttractions = entry.getValue();
                
                // Calculate total duration
                int totalMinutes = dayAttractions.stream()
                    .mapToInt(a -> a.getDurationMinutes() != null ? a.getDurationMinutes() : 60)
                    .sum();
                
                // Add travel time between activities (estimate 30 min per transition)
                int travelTime = Math.max(0, (dayAttractions.size() - 1) * 30);
                totalMinutes += travelTime;
                
                // Available time: 10 hours (600 minutes) for activities
                int availableMinutes = 600;
                
                if (totalMinutes <= availableMinutes) {
                    // All activities fit
                    validated.addAll(dayAttractions);
                    logger.debug("Day {} activities fit: {} minutes (limit: {})", 
                               dayNumber, totalMinutes, availableMinutes);
                } else {
                    // Too many activities, filter by priority
                    logger.warn("Day {} has {} minutes of activities, max is {}. Filtering...",
                               dayNumber, totalMinutes, availableMinutes);
                    
                    List<PopulatedAttraction> filtered = filterByPriority(
                        dayAttractions, 
                        availableMinutes
                    );
                    validated.addAll(filtered);
                    
                    logger.info("Day {} reduced from {} to {} activities",
                               dayNumber, dayAttractions.size(), filtered.size());
                }
            }
            
            return validated;
            
        } catch (Exception e) {
            logger.error("Error in duration validation: {}", e.getMessage());
            // Fallback: Return all activities (no filtering)
            logger.warn("Skipping duration validation, returning all activities");
            return attractions;
        }
    }
    
    /**
     * Filter activities by priority to fit within time limit.
     */
    private List<PopulatedAttraction> filterByPriority(
            List<PopulatedAttraction> attractions,
            int maxMinutes) {
        
        // Sort by priority (must-see > recommended > optional)
        List<PopulatedAttraction> sorted = new ArrayList<>(attractions);
        sorted.sort((a, b) -> {
            int priorityA = getPriorityScore(a.getCategory());
            int priorityB = getPriorityScore(b.getCategory());
            return Integer.compare(priorityB, priorityA);
        });
        
        List<PopulatedAttraction> filtered = new ArrayList<>();
        int totalMinutes = 0;
        
        for (PopulatedAttraction attraction : sorted) {
            int duration = attraction.getDurationMinutes() != null ? attraction.getDurationMinutes() : 60;
            if (totalMinutes + duration <= maxMinutes) {
                filtered.add(attraction);
                totalMinutes += duration;
            } else {
                logger.info("Skipping {} (would exceed time limit)", attraction.getTitle());
            }
        }
        
        return filtered;
    }
    
    /**
     * Get priority score for a category.
     */
    private int getPriorityScore(String category) {
        if (category == null) return 1;
        
        return switch (category.toLowerCase()) {
            case "landmark", "unesco-site", "temple_shrine" -> 3; // Must-see
            case "museum", "park", "nature" -> 2; // Recommended
            default -> 1; // Optional
        };
    }
    
    /**
     * Find which day a node belongs to.
     */
    private int findDayNumberForNode(NormalizedItinerary skeleton, String nodeId) {
        if (skeleton.getDays() == null || nodeId == null) {
            return -1;
        }
        
        for (NormalizedDay day : skeleton.getDays()) {
            if (day.getNodes() == null) continue;
            
            for (NormalizedNode node : day.getNodes()) {
                if (nodeId.equals(node.getId())) {
                    return day.getDayNumber();
                }
            }
        }
        
        return -1;
    }
    
    /**
     * Apply attraction data to skeleton nodes (extracted for retry logic).
     */
    private void applyAttractionsToSkeleton(NormalizedItinerary skeleton,
                                           List<PopulatedAttraction> populatedAttractions) {
        // Create a map for quick lookup
        Map<String, PopulatedAttraction> attractionMap = populatedAttractions.stream()
            .collect(Collectors.toMap(PopulatedAttraction::getNodeId, a -> a));
        
        // Update nodes in skeleton
        for (NormalizedDay day : skeleton.getDays()) {
            if (day.getNodes() == null) continue;
            
            for (NormalizedNode node : day.getNodes()) {
                // Ensure node has ID
                nodeIdGenerator.ensureNodeHasId(node, day.getDayNumber(), skeleton);
                logger.debug("Ensuring node {} has ID for day {}", node.getTitle(), day.getDayNumber());
                
                if ("attraction".equals(node.getType())) {
                    // Mark node as being processed
                    node.setProcessingState(ProcessingState.ENRICHING);
                    node.addProcessedBy("ActivityAgent");
                    
                    PopulatedAttraction populated = attractionMap.get(node.getId());
                    if (populated != null) {
                        // Update node with populated data
                        node.setTitle(populated.getTitle());
                        
                        if (node.getDetails() == null) {
                            node.setDetails(new NodeDetails());
                        }
                        node.getDetails().setDescription(populated.getDescription());
                        node.getDetails().setCategory(populated.getCategory());
                        
                        if (node.getTiming() != null && populated.getDurationMinutes() != null) {
                            node.getTiming().setDurationMin(populated.getDurationMinutes());
                        }
                        
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
                        node.setLastError("No attraction data found for node");
                    }
                }
            }
        }
    }
    
    /**
     * Optimize activity timing based on weather conditions.
     * Uses weather tools to suggest best times for outdoor activities.
     */
    private List<PopulatedAttraction> optimizeActivityTimingWithWeather(
            String itineraryId,
            List<PopulatedAttraction> attractions,
            NormalizedItinerary skeleton) {
        
        logger.info("=== WEATHER-AWARE ACTIVITY OPTIMIZATION ===");
        logger.info("Feature flag enabled: {}", weatherToolsEnabled);
        logger.info("Processing {} attractions", attractions.size());
        
        try {
            for (PopulatedAttraction attraction : attractions) {
                // Find the day and node for this attraction
                int dayNumber = findDayNumberForNode(skeleton, attraction.getNodeId());
                if (dayNumber <= 0) continue;
                
                NormalizedDay day = skeleton.getDays().stream()
                    .filter(d -> d.getDayNumber() == dayNumber)
                    .findFirst()
                    .orElse(null);
                
                if (day == null || day.getDate() == null) continue;
                
                // Get the node
                NormalizedNode node = day.getNodes().stream()
                    .filter(n -> attraction.getNodeId().equals(n.getId()))
                    .findFirst()
                    .orElse(null);
                
                if (node == null) continue;
                
                // Only optimize outdoor activities
                if (!isOutdoorActivity(attraction.getCategory())) {
                    logger.debug("Skipping indoor activity: {}", attraction.getTitle());
                    continue;
                }
                
                // Get weather-based timing suggestions
                try {
                    SuggestBestTimeResult timing = getWeatherBasedTiming(
                        itineraryId,
                        attraction.getTitle(),
                        attraction.getCategory(),
                        day.getLocation(),
                        day.getDate(),
                        attraction.getDurationMinutes() != null ? attraction.getDurationMinutes() : 120
                    );
                    
                    if (timing != null && timing.isSuccess() && !timing.getRecommendedTimeSlots().isEmpty()) {
                        // Use the best recommended time slot
                        TimeSlot bestSlot = timing.getRecommendedTimeSlots().get(0);
                        
                        if (node.getTiming() == null) {
                            node.setTiming(new NodeTiming());
                        }
                        
                        // Update timing (convert time strings to appropriate format)
                        String oldTime = node.getTiming().getStartTime() != null ? 
                            String.valueOf(node.getTiming().getStartTime()) : "unscheduled";
                        
                        // Note: Timing update would need proper time format conversion
                        // For now, log the recommendation
                        logger.info("Weather tool recommends: {} at {} (score: {})",
                            attraction.getTitle(), bestSlot.getStartTime(), bestSlot.getSuitabilityScore());
                        
                        if (logComparisons) {
                            logger.info("Weather optimization for {}: recommended time {} (score: {}, reason: {})",
                                attraction.getTitle(),
                                bestSlot.getStartTime(),
                                bestSlot.getSuitabilityScore(),
                                bestSlot.getReason());
                        }
                        
                        // Add weather warnings to metadata if extreme conditions
                        if (timing.getExtremeWeatherWarning() != null && timing.getExtremeWeatherWarning().isExtreme()) {
                            addWeatherWarningToNode(node, timing.getExtremeWeatherWarning());
                        }
                        
                        // Add seasonal context to description
                        if (timing.getWeatherContext() != null && timing.getWeatherContext().getSeasonalNotes() != null) {
                            String seasonalNote = timing.getWeatherContext().getSeasonalNotes();
                            if (node.getDetails() != null && seasonalNote != null && !seasonalNote.isEmpty()) {
                                String currentDesc = node.getDetails().getDescription();
                                if (currentDesc != null && !currentDesc.contains(seasonalNote.substring(0, Math.min(20, seasonalNote.length())))) {
                                    node.getDetails().setDescription(currentDesc + "\n\nWeather Note: " + seasonalNote);
                                }
                            }
                        }
                    }
                    
                } catch (Exception e) {
                    logger.warn("Failed to get weather timing for {}: {}", attraction.getTitle(), e.getMessage());
                    if (!fallbackOnError) {
                        throw e;
                    }
                    // Continue with original timing (fallback)
                }
            }
            
            logger.info("=== WEATHER OPTIMIZATION COMPLETE ===");
            return attractions;
            
        } catch (Exception e) {
            logger.error("Weather optimization failed: {}", e.getMessage(), e);
            if (fallbackOnError) {
                logger.warn("Falling back to original timing (weather tools disabled for this request)");
                return attractions;
            }
            throw new RuntimeException("Weather optimization failed", e);
        }
    }
    
    /**
     * Check if activity category is outdoor.
     */
    private boolean isOutdoorActivity(String category) {
        if (category == null) return false;
        
        return switch (category.toLowerCase()) {
            case "park", "nature", "landmark", "temple_shrine", "experience" -> true;
            case "museum", "shopping", "entertainment" -> false;
            default -> false; // Conservative: assume indoor if unknown
        };
    }
    
    /**
     * Get weather-based timing suggestions for an activity.
     * Uses cache if available to avoid duplicate API calls.
     */
    private SuggestBestTimeResult getWeatherBasedTiming(String itineraryId, String activityName, 
                                                        String category, String location, 
                                                        String date, int durationMinutes) {
        try {
            // Map category to activity type
            String activityType = mapCategoryToActivityType(category);
            
            SuggestBestTimeRequest request = new SuggestBestTimeRequest();
            request.setActivityName(activityName);
            request.setActivityType(activityType);
            request.setLocation(location);
            request.setDate(date);
            request.setDuration(durationMinutes);
            request.setItineraryId(itineraryId);  // ✅ FIX: Set itineraryId for caching
            
            // Use cache if available
            if (toolCacheService != null) {
                logger.debug("Using cached weather tool for: {} at {} on {}", activityName, location, date);
                
                // Generate cache key
                String cacheKey = ToolCacheKeyGenerator.forWeatherTiming(
                    activityName, location, date, durationMinutes);
                
                // Get or compute with cache
                return toolCacheService.getOrCompute(
                    itineraryId,
                    ToolType.SUGGEST_BEST_TIME.getValue(),
                    cacheKey,
                    request,
                    () -> callWeatherToolDirect(request),
                    SuggestBestTimeResult.class
                );
            } else {
                // No cache available, call directly
                logger.debug("Calling weather tool (no cache) for: {} at {} on {}", 
                    activityName, location, date);
                return callWeatherToolDirect(request);
            }
            
        } catch (Exception e) {
            logger.error("Failed to call weather tool: {}", e.getMessage());
            if (!fallbackOnError) {
                throw e;
            }
            return null;
        }
    }
    
    /**
     * Call weather tool directly via REST API with agent name header.
     */
    private SuggestBestTimeResult callWeatherToolDirect(SuggestBestTimeRequest request) {
        // Create headers with agent name
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.set("X-Agent-Name", "ActivityAgent");
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        
        // Create HTTP entity with headers
        org.springframework.http.HttpEntity<SuggestBestTimeRequest> entity = 
            new org.springframework.http.HttpEntity<>(request, headers);
        
        // Make POST request with headers
        return restTemplate.postForEntity(
            "http://localhost:8080/api/v1/tools/suggest-best-time",
            entity,
            SuggestBestTimeResult.class
        ).getBody();
    }
    
    // ========== ACTIVITY AGENT - ADDITIONAL TOOL INTEGRATION METHODS ==========
    
    /**
     * Generate node ID for new attraction nodes using the Generate Node ID tool.
     */
    private String generateNodeIdViaTool(String itineraryId, Integer dayNumber, String nodeType) {
        if (!weatherToolsEnabled) {
            return nodeIdGenerator.generateNodeId(nodeType, dayNumber, null);
        }
        
        com.tripplanner.dto.tools.NodeIdRequest request = 
            new com.tripplanner.dto.tools.NodeIdRequest(itineraryId, dayNumber, nodeType);
        
        try {
            com.tripplanner.dto.tools.NodeIdResponse response = restTemplate.postForObject(
                "http://localhost:8080/api/v1/tools/generate-node-id",
                request,
                com.tripplanner.dto.tools.NodeIdResponse.class
            );
            
            if (response != null && response.isSuccess()) {
                logger.debug("Generated node ID via tool: {}", response.getNodeId());
                return response.getNodeId();
            }
            return fallbackOnError ? nodeIdGenerator.generateNodeId(nodeType, dayNumber, null) : null;
        } catch (Exception e) {
            logger.error("Generate Node ID tool error: {}", e.getMessage());
            return fallbackOnError ? nodeIdGenerator.generateNodeId(nodeType, dayNumber, null) : null;
        }
    }
    
    /**
     * Check user constraints (budget, party size) using the Check Constraints tool.
     */
    private com.tripplanner.dto.tools.ConstraintCheckResult checkConstraintsViaTool(String itineraryId) {
        if (!weatherToolsEnabled) {
            return null;
        }
        
        com.tripplanner.dto.tools.ConstraintCheckRequest request = 
            new com.tripplanner.dto.tools.ConstraintCheckRequest(itineraryId);
        
        try {
            com.tripplanner.dto.tools.ConstraintCheckResult result = restTemplate.postForObject(
                "http://localhost:8080/api/v1/tools/check-user-constraints",
                request,
                com.tripplanner.dto.tools.ConstraintCheckResult.class
            );
            
            if (result != null && !result.isValid()) {
                logger.warn("Constraint violations detected:");
                result.getViolations().forEach(v -> 
                    logger.warn("  - {}: {}", v.getType(), v.getMessage()));
            }
            return result;
        } catch (Exception e) {
            logger.error("Check Constraints tool error: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Validate LLM schema using the Validate Schema tool.
     */
    private boolean validateSchemaViaTool(String jsonOutput, String jsonSchema) {
        if (!weatherToolsEnabled) {
            return true; // Use existing validator
        }
        
        com.tripplanner.dto.tools.SchemaValidationRequest request = 
            new com.tripplanner.dto.tools.SchemaValidationRequest();
        request.setJsonOutput(jsonOutput);
        request.setJsonSchema(jsonSchema);
        request.setCleanBeforeValidation(true);
        
        try {
            com.tripplanner.dto.tools.SchemaValidationResult result = restTemplate.postForObject(
                "http://localhost:8080/api/v1/tools/validate-schema",
                request,
                com.tripplanner.dto.tools.SchemaValidationResult.class
            );
            
            if (result != null && !result.isValid()) {
                logger.error("Schema validation failed:");
                result.getErrors().forEach(error -> logger.error("  - {}", error));
            }
            return result != null && result.isValid();
        } catch (Exception e) {
            logger.error("Validate Schema tool error: {}", e.getMessage());
            return fallbackOnError;
        }
    }
    
    // ========== END ACTIVITY AGENT TOOL INTEGRATION ==========
    
    /**
     * Map activity category to activity type for weather tool.
     */
    private String mapCategoryToActivityType(String category) {
        if (category == null) return "flexible";
        
        return switch (category.toLowerCase()) {
            case "park", "nature" -> "outdoor_park";
            case "landmark" -> "outdoor_monument";
            case "temple_shrine" -> "outdoor_monument";
            case "experience" -> "outdoor_adventure";
            case "museum" -> "indoor_museum";
            case "shopping" -> "indoor_mall";
            case "entertainment" -> "indoor_museum";
            default -> "flexible";
        };
    }
    
    /**
     * Add weather warning to node description.
     */
    private void addWeatherWarningToNode(NormalizedNode node, SuggestBestTimeResult.ExtremeWeatherWarning warning) {
        if (node.getDetails() == null) {
            node.setDetails(new NodeDetails());
        }
        
        // Add weather warning to description
        StringBuilder warningText = new StringBuilder();
        warningText.append("\n\n⚠️ WEATHER ALERT (").append(warning.getSeverity().toUpperCase()).append("):\n");
        
        for (String w : warning.getWarnings()) {
            warningText.append("• ").append(w).append("\n");
        }
        
        if (!warning.getGearRequirements().isEmpty()) {
            warningText.append("\nRequired Gear:\n");
            for (String gear : warning.getGearRequirements()) {
                warningText.append("• ").append(gear).append("\n");
            }
        }
        
        String currentDesc = node.getDetails().getDescription();
        if (currentDesc != null) {
            node.getDetails().setDescription(currentDesc + warningText.toString());
        } else {
            node.getDetails().setDescription(warningText.toString());
        }
        
        logger.info("Added {} weather warning to {}", warning.getSeverity(), node.getTitle());
    }
    
    /**
     * Update itinerary with populated attraction data.
     */
    private void updateItineraryWithAttractions(String itineraryId, NormalizedItinerary skeleton,
                                                List<PopulatedAttraction> populatedAttractions) {
        
        // Apply attractions to skeleton
        applyAttractionsToSkeleton(skeleton, populatedAttractions);
        
        // Populate metadata for all attraction nodes
        for (NormalizedDay day : skeleton.getDays()) {
            if (day.getNodes() != null) {
                for (NormalizedNode node : day.getNodes()) {
                    if ("attraction".equals(node.getType())) {
                        activityMetadataService.populateNodeMetadata(node);
                    }
                }
            }
        }
        logger.info("Populated metadata for all attraction nodes");
        
        // Validate before save
        ValidationResult validationResult = itineraryValidator.validate(skeleton);
        if (!validationResult.isValid()) {
            logger.error("Validation failed for itinerary {}: {}", itineraryId, validationResult.getErrors());
            throw new ValidationException("Itinerary validation failed", String.valueOf(validationResult.getErrors()));
        }
        if (!validationResult.getWarnings().isEmpty()) {
            logger.warn("Validation warnings for itinerary {}: {}", itineraryId, validationResult.getWarnings());
        }
        
        // Save updated itinerary with optimistic locking and retry
        int maxRetries = 3;
        int retryCount = 0;
        boolean saved = false;
        
        while (!saved && retryCount < maxRetries) {
            try {
                skeleton.setUpdatedAt(System.currentTimeMillis());
                itineraryJsonService.updateItineraryWithLock(skeleton);
                logger.info("Saved itinerary with populated attractions (with lock)");
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
                        // Re-apply attraction changes to reloaded skeleton
                        applyAttractionsToSkeleton(skeleton, populatedAttractions);
                        logger.info("Re-applied attraction changes to reloaded itinerary");
                    } else {
                        logger.error("Failed to reload itinerary for retry");
                        throw e;
                    }
                } else {
                    logger.error("Max retries ({}) exceeded, giving up", maxRetries);
                    throw e;
                }
            } catch (Exception e) {
                logger.error("Failed to save itinerary with attractions: {}", e.getMessage());
                throw new RuntimeException("Failed to save activity data", e);
            }
        }
    }
    
    private String buildActivitySystemPrompt() {
        return """
            You are a travel expert specializing in attractions and activities.
            
            Your task: Suggest SPECIFIC attractions for placeholder activity nodes.
            
            CRITICAL: Use the EXACT node IDs provided in the user prompt. Do NOT generate your own node IDs.
            
            Guidelines:
            1. Use the EXACT nodeId provided for each attraction slot
            2. Provide real, specific place names (e.g., "Tokyo National Museum" not "Museum")
            3. Write engaging descriptions (2-3 sentences)
            4. Assign appropriate categories
            5. Estimate realistic visit durations (CRITICAL - see duration guidelines below)
            6. Consider the time slot and day context
            7. Ensure variety across the day and trip
            8. IMPORTANT: For locationName, provide the SPECIFIC place name, NOT just the district/area
               - GOOD: "Shibuya Crossing", "Senso-ji Temple", "Tsukiji Outer Market"
               - BAD: "Shibuya", "Asakusa", "Tsukiji"
            
            Activity Duration Guidelines (in minutes):
            - Landmark/Monument: 60-90 minutes
            - Museum/Gallery: 90-120 minutes
            - Temple/Shrine: 45-60 minutes
            - Park/Garden: 60-90 minutes
            - Safari/Trek: 240-480 minutes (4-8 hours)
            - Theme Park: 360-480 minutes (6-8 hours)
            - Shopping District: 90-120 minutes
            - Cultural Experience: 120-180 minutes
            
            CRITICAL: Include durationMinutes for each attraction.
            If a day has a long-duration activity (>4 hours), other activities should be minimal.
            
            Categories:
            - museum: Museums, galleries, exhibitions
            - landmark: Monuments, historical sites, viewpoints
            - park: Parks, gardens, outdoor spaces
            - temple_shrine: Religious sites, spiritual places
            - entertainment: Shows, performances, theme parks
            - shopping: Markets, malls, shopping districts
            - experience: Cultural experiences, workshops
            - nature: Beaches, mountains, natural attractions
            
            Be specific, practical, and ensure attractions match the destination and timing.
            The locationName field should be specific enough to find the exact place on Google Maps.
            """;
    }
    
    private String buildActivityUserPrompt(NormalizedItinerary skeleton, 
                                           List<AttractionContext> contexts) {
        StringBuilder prompt = new StringBuilder();
        
        // IMPROVED: Group contexts by day to show city-specific context
        Map<Integer, List<AttractionContext>> contextsByDay = new HashMap<>();
        for (AttractionContext ctx : contexts) {
            contextsByDay.computeIfAbsent(ctx.dayNumber, k -> new ArrayList<>()).add(ctx);
        }
        
        prompt.append("Trip Overview:\n");
        prompt.append("Total Days: ").append(skeleton.getDays().size()).append("\n");
        
        if (skeleton.getThemes() != null && !skeleton.getThemes().isEmpty()) {
            prompt.append("Interests: ").append(String.join(", ", skeleton.getThemes())).append("\n");
        }
        
        // CRITICAL: Include user's custom instructions/constraints
        // These constraints MUST be respected in all attraction selections
        if (skeleton.getConstraints() != null && !skeleton.getConstraints().isEmpty()) {
            prompt.append("\n=== CRITICAL USER REQUIREMENTS (MUST FOLLOW) ===\n");
            for (String constraint : skeleton.getConstraints()) {
                prompt.append("- ").append(constraint).append("\n");
            }
            prompt.append("All attractions MUST comply with these requirements.\n");
            prompt.append("If a requirement cannot be met, skip that attraction slot.\n\n");
        }
        
        prompt.append("\n=== ATTRACTION SLOTS TO POPULATE (BY DAY) ===\n");
        
        // CRITICAL: Show each day's location explicitly to prevent wrong-city attractions
        for (Map.Entry<Integer, List<AttractionContext>> entry : contextsByDay.entrySet()) {
            int dayNum = entry.getKey();
            List<AttractionContext> dayContexts = entry.getValue();
            
            // Get the day's location
            String dayLocation = dayContexts.get(0).dayLocation;
            
            prompt.append(String.format("\n** DAY %d - LOCATION: %s **\n", dayNum, dayLocation));
            prompt.append(String.format("CRITICAL: All attractions for Day %d MUST be in %s, NOT in any other city!\n", 
                                       dayNum, dayLocation));
            
            for (AttractionContext ctx : dayContexts) {
                prompt.append(String.format("  - Node ID: %s, Time: %s\n",
                    ctx.nodeId, 
                    ctx.timing != null ? ctx.timing.getStartTime() : "TBD"));
            }
        }
        
        prompt.append("\n=== CRITICAL RULES ===\n");
        prompt.append("1. Use the EXACT node IDs listed above. Do NOT generate your own node IDs.\n");
        prompt.append("2. Each attraction MUST be in the CORRECT CITY for that day.\n");
        prompt.append("3. Do NOT suggest attractions from other cities (e.g., no Kuala Lumpur attractions on Penang days).\n");
        prompt.append("4. Provide specific attraction names that are searchable on Google Maps.\n");
        prompt.append("5. Ensure variety and avoid repetition across days.\n");
        prompt.append("6. All selections must respect the user requirements listed above.\n");
        
        return prompt.toString();
    }
    
    private String buildActivityJsonSchema() {
        return """
            {
              "type": "object",
              "properties": {
                "attractions": {
                  "type": "array",
                  "items": {
                    "type": "object",
                    "properties": {
                      "nodeId": { "type": "string" },
                      "title": { "type": "string" },
                      "description": { "type": "string" },
                      "category": { 
                        "type": "string",
                        "enum": ["museum", "landmark", "park", "temple_shrine", 
                                "entertainment", "shopping", "experience", "nature"]
                      },
                      "durationMinutes": { "type": "integer" },
                      "locationName": { 
                        "type": "string",
                        "description": "SPECIFIC place name (e.g., 'Shibuya Crossing' not 'Shibuya', 'Senso-ji Temple' not 'Asakusa'). Must be searchable on Google Maps."
                      }
                    },
                    "required": ["nodeId", "title", "description", "category", "locationName"]
                  }
                }
              },
              "required": ["attractions"]
            }
            """;
    }
    
    @Override
    protected <T> T executeInternal(String itineraryId, AgentRequest<T> request) {
        NormalizedItinerary skeleton = request.getData(NormalizedItinerary.class);
        populateAttractions(itineraryId, skeleton);
        return (T) skeleton;
    }
    
    @Override
    protected String getAgentName() {
        return "Activity Agent";
    }
    
    // Helper classes
    
    private static class AttractionContext {
        String nodeId;
        int dayNumber;
        String dayLocation;
        NodeTiming timing;
        
        public AttractionContext(String nodeId, int dayNumber, String dayLocation, NodeTiming timing) {
            this.nodeId = nodeId;
            this.dayNumber = dayNumber;
            this.dayLocation = dayLocation;
            this.timing = timing;
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
    
    public static class PopulatedAttraction {
        private String nodeId;
        private String title;
        private String description;
        private String category;
        private Integer durationMinutes;
        private String locationName;
        
        // Getters and setters
        public String getNodeId() { return nodeId; }
        public void setNodeId(String nodeId) { this.nodeId = nodeId; }
        
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        
        public Integer getDurationMinutes() { return durationMinutes; }
        public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }
        
        public String getLocationName() { return locationName; }
        public void setLocationName(String locationName) { this.locationName = locationName; }
    }
}

