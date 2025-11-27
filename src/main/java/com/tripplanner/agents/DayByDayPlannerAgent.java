package com.tripplanner.agents;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripplanner.dto.*;
import com.tripplanner.service.agents.AgentEventBus;
import com.tripplanner.service.ItineraryJsonService;
import com.tripplanner.service.SummarizationService;
import com.tripplanner.service.agents.AgentEventPublisher;
import com.tripplanner.service.CurrencyConversionService;
import com.tripplanner.service.ai.AiClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Enhanced Planner Agent that creates itineraries day-by-day to avoid token limits
 * and prevent location duplication across days.
 * todo: this is not yet implemented in original flow but must be replaced with planner agent
 */
@Component
@ConditionalOnBean(AiClient.class)
public class DayByDayPlannerAgent extends BaseAgent {

    private final AiClient aiClient;
    private final ObjectMapper objectMapper;
    private final ItineraryJsonService itineraryJsonService;
    private final SummarizationService summarizationService;
    private final AgentEventPublisher agentEventPublisher;
    private final CurrencyConversionService currencyConversionService;
    
    // Token limits for different models
    private static final int MAX_CONTEXT_TOKENS = 8000; // Conservative limit
    private static final int ESTIMATED_TOKENS_PER_DAY = 1500; // Rough estimate
    // Configurable batch size: 2 days = faster but may timeout, 1 day = more reliable
    // If experiencing timeouts, reduce to 1. Currently set to 2 for balance.
    private static final int MAX_DAYS_PER_BATCH = 2; // Process 2 days at once (reduce to 1 if timeouts persist)
    
    public DayByDayPlannerAgent(AgentEventBus eventBus, AiClient aiClient, ObjectMapper objectMapper,
                               ItineraryJsonService itineraryJsonService, SummarizationService summarizationService,
                               AgentEventPublisher agentEventPublisher, CurrencyConversionService currencyConversionService) {
        super(eventBus, AgentEvent.AgentKind.PLANNER);
        this.aiClient = aiClient;
        this.objectMapper = objectMapper;
        this.itineraryJsonService = itineraryJsonService;
        this.summarizationService = summarizationService;
        this.agentEventPublisher = agentEventPublisher;
        this.currencyConversionService = currencyConversionService;
    }
    
    @Override
    public AgentCapabilities getCapabilities() {
        AgentCapabilities capabilities = new AgentCapabilities();
        
        // Chat-enabled task: plan (for creating NEW itineraries)
        capabilities.addSupportedTask("plan");
        
        capabilities.setPriority(5); // High priority for planning
        capabilities.setChatEnabled(true); // Handle chat requests
        capabilities.setConfigurationValue("maxDaysPerBatch", MAX_DAYS_PER_BATCH);
        capabilities.setConfigurationValue("tokenLimit", MAX_CONTEXT_TOKENS);
        
        return capabilities;
    }
    
    @Override
    protected <T> T executeInternal(String itineraryId, AgentRequest<T> request) {
        // Extract CreateItineraryReq from Map or use directly
        CreateItineraryReq itineraryReq;
        Object data = request.getData();
        if (data instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> dataMap = (Map<String, Object>) data;
            itineraryReq = (CreateItineraryReq) dataMap.get("request");
        } else if (data instanceof CreateItineraryReq) {
            itineraryReq = (CreateItineraryReq) data;
        } else {
            itineraryReq = null;
        }
        
        if (itineraryReq == null) {
            throw new IllegalArgumentException("DayByDayPlannerAgent requires non-null CreateItineraryReq");
        }
        
        logger.info("=== DAY-BY-DAY PLANNER AGENT PROCESSING ===");
        logger.info("Destination: {}", itineraryReq.getDestination());
        logger.info("Duration: {} days", itineraryReq.getDurationDays());
        logger.info("Processing in batches to avoid token limits");
        logger.info("==========================================");
        
        try {
            // Create initial itinerary structure
            NormalizedItinerary itinerary = createInitialItinerary(itineraryId, itineraryReq);
            
            // Plan days in batches to avoid token limits
            List<String> previousDaysSummaries = new ArrayList<>();
            Set<String> usedLocations = new HashSet<>();
            
            int totalDays = itineraryReq.getDurationDays();
            int processedDays = 0;
            
            while (processedDays < totalDays) {
                int remainingDays = totalDays - processedDays;
                int batchSize = Math.min(MAX_DAYS_PER_BATCH, remainingDays);
                
                emitProgress(itineraryId, 
                    (int) (20 + (processedDays * 60.0 / totalDays)), 
                    String.format("Planning days %d-%d", processedDays + 1, processedDays + batchSize),
                    "day_planning");
                
                // Plan this batch of days with real-time updates
                List<NormalizedDay> batchDays = planDaysBatch(
                    itineraryReq, 
                    processedDays + 1, 
                    batchSize, 
                    previousDaysSummaries, 
                    usedLocations,
                    itineraryId,
                    "exec_" + System.currentTimeMillis() // Generate execution ID if not available
                );
                
                // Add days to main itinerary and collect used locations
                for (NormalizedDay day : batchDays) {
                    // Check if day already exists (from real-time save in planDaysBatch)
                    boolean dayExists = false;
                    for (int i = 0; i < itinerary.getDays().size(); i++) {
                        if (itinerary.getDays().get(i).getDayNumber() == day.getDayNumber()) {
                            // Update existing day
                            itinerary.getDays().set(i, day);
                            dayExists = true;
                            break;
                        }
                    }
                    if (!dayExists) {
                        itinerary.getDays().add(day);
                    }
                    
                    // Collect used locations to avoid duplication
                    collectUsedLocations(day, usedLocations);
                }
                
                // Create summary of processed days for next batch
                if (processedDays + batchSize < totalDays) {
                    String batchSummary = summarizationService.summarizeDays(batchDays);
                    previousDaysSummaries.add(batchSummary);
                    
                    // Keep only last 3 summaries to manage context size
                    if (previousDaysSummaries.size() > 3) {
                        previousDaysSummaries.remove(0);
                    }
                }
                
                processedDays += batchSize;
            }
            
            emitProgress(itineraryId, 85, "Finalizing itinerary", "finalization");
            
            // Finalize and save itinerary with optimistic locking and retry
            finalizeItinerary(itinerary, itineraryReq);
            
            int maxRetries = 3;
            int retryCount = 0;
            boolean saved = false;
            
            while (!saved && retryCount < maxRetries) {
                try {
                    itineraryJsonService.updateItineraryWithLock(itinerary);
                    saved = true;
                } catch (com.tripplanner.exception.ConcurrentModificationException e) {
                    retryCount++;
                    logger.error("Concurrent modification during finalization (attempt {}/{}): {}", 
                               retryCount, maxRetries, e.getMessage());
                    
                    if (retryCount < maxRetries) {
                        logger.info("Reloading itinerary and retrying finalization save...");
                        Optional<NormalizedItinerary> reloaded = itineraryJsonService.getItinerary(itineraryId);
                        if (reloaded.isPresent()) {
                            itinerary = reloaded.get();
                            // Re-apply finalization
                            finalizeItinerary(itinerary, itineraryReq);
                            logger.info("Re-applied finalization to reloaded itinerary");
                        } else {
                            logger.error("Failed to reload itinerary for retry");
                            throw new RuntimeException("Finalization conflict", e);
                        }
                    } else {
                        logger.error("Max retries ({}) exceeded, giving up", maxRetries);
                        throw new RuntimeException("Finalization conflict", e);
                    }
                }
            }
            
            emitProgress(itineraryId, 100, "Itinerary completed", "complete");
            
            logger.info("=== DAY-BY-DAY PLANNING COMPLETED ===");
            logger.info("Total days planned: {}", itinerary.getDays().size());
            logger.info("Unique locations used: {}", usedLocations.size());
            logger.info("=====================================");
            
            // Convert to expected response type
            if (request.getResponseType() == ItineraryDto.class) {
                return (T) convertToItineraryDto(itinerary, itineraryReq);
            }
            
            return (T) itinerary;
            
        } catch (Exception e) {
            logger.error("Failed to create day-by-day itinerary: {}", itineraryId, e);
            throw new RuntimeException("Failed to create itinerary: " + e.getMessage(), e);
        }
    }
    
    /**
     * Plan a batch of days with context from previous days and real-time updates
     */
    private List<NormalizedDay> planDaysBatch(CreateItineraryReq request, int startDay, int batchSize,
                                             List<String> previousDaysSummaries, Set<String> usedLocations,
                                             String itineraryId, String executionId) {
        
        String systemPrompt = buildDayPlanningSystemPrompt();
        String userPrompt = buildDayPlanningUserPrompt(request, startDay, batchSize, 
                                                      previousDaysSummaries, usedLocations);
        
        logger.info("Planning days {}-{} with context from {} previous summaries", 
                   startDay, startDay + batchSize - 1, previousDaysSummaries.size());
        
        // Publish batch progress event
        if (agentEventPublisher.hasActiveConnections(itineraryId)) {
            agentEventPublisher.publishBatchProgress(itineraryId, executionId, startDay, 
                                                    request.getDurationDays(), 
                                                    String.format("Planning days %d-%d", startDay, startDay + batchSize - 1));
        }
        
        String schema = buildDayBatchJsonSchema();
        String response = aiClient.generateStructuredContent(userPrompt, schema, systemPrompt);
        
        try {
            // Parse response into list of days
            com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(response);
            List<NormalizedDay> days = new ArrayList<>();
            
            if (root.has("days") && root.get("days").isArray()) {
                for (com.fasterxml.jackson.databind.JsonNode dayNode : root.get("days")) {
                    NormalizedDay day = objectMapper.treeToValue(dayNode, NormalizedDay.class);
                    days.add(day);
                    
                    // Immediately update itinerary with new day for real-time access
                    try {
                        // Get current itinerary and add the day
                        var currentItinerary = itineraryJsonService.getItinerary(itineraryId);
                        if (currentItinerary.isPresent()) {
                            NormalizedItinerary itinerary = currentItinerary.get();
                            if (itinerary.getDays() == null) {
                                itinerary.setDays(new ArrayList<>());
                            }
                            // Add or update the day
                            boolean dayExists = false;
                            for (int i = 0; i < itinerary.getDays().size(); i++) {
                                if (itinerary.getDays().get(i).getDayNumber() == day.getDayNumber()) {
                                    itinerary.getDays().set(i, day);
                                    dayExists = true;
                                    break;
                                }
                            }
                            if (!dayExists) {
                                itinerary.getDays().add(day);
                            }
                            
                            // Save with optimistic locking and retry
                            int maxRetries = 3;
                            int retryCount = 0;
                            boolean saved = false;
                            
                            while (!saved && retryCount < maxRetries) {
                                try {
                                    itinerary.setUpdatedAt(System.currentTimeMillis());
                                    itineraryJsonService.updateItineraryWithLock(itinerary);
                                    saved = true;
                                } catch (com.tripplanner.exception.ConcurrentModificationException e) {
                                    retryCount++;
                                    logger.error("Concurrent modification during day save (attempt {}/{}): {}", 
                                               retryCount, maxRetries, e.getMessage());
                                    
                                    if (retryCount < maxRetries) {
                                        logger.info("Reloading itinerary and retrying day save...");
                                        Optional<NormalizedItinerary> reloaded = itineraryJsonService.getItinerary(itineraryId);
                                        if (reloaded.isPresent()) {
                                            itinerary = reloaded.get();
                                            // Re-add or update the day
                                            dayExists = false;
                                            for (int i = 0; i < itinerary.getDays().size(); i++) {
                                                if (itinerary.getDays().get(i).getDayNumber() == day.getDayNumber()) {
                                                    itinerary.getDays().set(i, day);
                                                    dayExists = true;
                                                    break;
                                                }
                                            }
                                            if (!dayExists) {
                                                itinerary.getDays().add(day);
                                            }
                                            logger.info("Re-applied day {} to reloaded itinerary", day.getDayNumber());
                                        } else {
                                            logger.error("Failed to reload itinerary for retry");
                                            throw new RuntimeException("Day save conflict", e);
                                        }
                                    } else {
                                        logger.error("Max retries ({}) exceeded, giving up", maxRetries);
                                        throw new RuntimeException("Day save conflict", e);
                                    }
                                }
                            }
                        }
                        
                        // Publish day completed event if there are active connections
                        if (agentEventPublisher.hasActiveConnections(itineraryId)) {
                            agentEventPublisher.publishDayCompleted(itineraryId, executionId, day);
                        }
                        
                        logger.info("Day {} planned and published in real-time for itinerary {}", 
                                   day.getDayNumber(), itineraryId);
                                   
                    } catch (Exception e) {
                        logger.warn("Failed to save day {} immediately, will be saved with batch: {}", 
                                   day.getDayNumber(), e.getMessage());
                        
                        // Publish error event but continue processing
                        if (agentEventPublisher.hasActiveConnections(itineraryId)) {
                            agentEventPublisher.publishWarning(itineraryId, executionId, "DAY_SAVE_FAILED",
                                                              String.format("Failed to save day %d immediately", day.getDayNumber()),
                                                              "Day will be saved with final batch");
                        }
                    }
                }
            }
            
            return days;
            
        } catch (JsonProcessingException e) {
            logger.error("Failed to parse day batch response", e);
            
            // Publish error event
            if (agentEventPublisher.hasActiveConnections(itineraryId)) {
                agentEventPublisher.publishErrorFromException(itineraryId, executionId, e, 
                                                             "day batch parsing", ErrorEvent.ErrorSeverity.ERROR);
            }
            
            throw new RuntimeException("Failed to parse day planning response: " + e.getMessage(), e);
        }
    }
    
    /**
     * Collect used locations from a day to avoid duplication
     */
    private void collectUsedLocations(NormalizedDay day, Set<String> usedLocations) {
        if (day.getNodes() != null) {
            for (NormalizedNode node : day.getNodes()) {
                if (node.getLocation() != null && node.getLocation().getName() != null) {
                    usedLocations.add(node.getLocation().getName().toLowerCase());
                }
                if (node.getLocation() != null && node.getLocation().getAddress() != null) {
                    usedLocations.add(node.getLocation().getAddress().toLowerCase());
                }
            }
        }
    }
    
    /**
     * Create initial itinerary structure
     */
    private NormalizedItinerary createInitialItinerary(String itineraryId, CreateItineraryReq request) {
        NormalizedItinerary itinerary = new NormalizedItinerary();
        itinerary.setItineraryId(itineraryId);
        itinerary.setVersion(1);
        itinerary.setSummary("Multi-day trip to " + request.getDestination());
        
        // Detect destination currency automatically
        String destinationCurrency = currencyConversionService.detectDestinationCurrency(request.getDestination());
        itinerary.setCurrency(destinationCurrency);
        logger.info("Detected currency for {}: {}", request.getDestination(), destinationCurrency);
        
        itinerary.setThemes(request.getInterests() != null ? request.getInterests() : new ArrayList<>());
        itinerary.setDays(new ArrayList<>());
        itinerary.setCreatedAt(System.currentTimeMillis());
        itinerary.setUpdatedAt(System.currentTimeMillis());
        
        return itinerary;
    }
    
    /**
     * Finalize itinerary with metadata and calculations
     */
    private void finalizeItinerary(NormalizedItinerary itinerary, CreateItineraryReq request) {
        // Calculate total costs, distances, etc.
        double totalCost = 0;
        int totalNodes = 0;
        
        for (NormalizedDay day : itinerary.getDays()) {
            if (day.getNodes() != null) {
                totalNodes += day.getNodes().size();
                for (NormalizedNode node : day.getNodes()) {
                    if (node.getCost() != null && node.getCost().getAmountPerPerson() != null) {
                        totalCost += node.getCost().getAmountPerPerson();
                    }
                }
            }
        }
        
        // Update summary with final details
        itinerary.setSummary(String.format(
            "%d-day trip to %s with %d activities and estimated cost of ₹%.0f per person",
            itinerary.getDays().size(), request.getDestination(), totalNodes, totalCost
        ));
        
        itinerary.setUpdatedAt(System.currentTimeMillis());
    }
    
    private String buildDayPlanningSystemPrompt() {
        return """
            You are an expert travel PLANNER creating detailed day-by-day itineraries.
            
            CRITICAL REQUIREMENTS:
            1. AVOID LOCATION DUPLICATION: Never suggest locations that have been used in previous days
            2. CREATE LOGICAL FLOW: Each day should have a coherent geographical and thematic flow
            3. INCLUDE ALL NODE TYPES: Each day needs attractions, meals, and transport as appropriate
            4. RESPECT TIMING: Consider opening hours, travel times, and realistic scheduling
            5. BUDGET AWARENESS: Stay within the specified budget tier
            6. SET CITY-LEVEL LOCATION: The "location" field for each day MUST be the specific city name (e.g., "Zurich", "Interlaken"), NOT the country or region
            
            Node Types:
            - "attraction": Museums, landmarks, experiences, activities
            - "meal": Restaurants, cafes, street food, food experiences  
            - "accommodation": Hotels, hostels, vacation rentals (for overnight stays)
            - "transport": Taxis, buses, trains, flights between locations
            
            Each day should have 4-8 nodes including:
            - 2-4 attractions/activities
            - 2-3 meals (breakfast, lunch, dinner)
            - 1-2 transport nodes if moving between areas
            - 1 accommodation node if staying overnight
            
            IMPORTANT: For the day-level "location" field, always specify the CITY name where activities take place.
            Examples:
            - CORRECT: "location": "Zurich"
            - CORRECT: "location": "Interlaken"
            - WRONG: "location": "Switzerland"
            - WRONG: "location": "Switzerland, Switzerland"
            
            Always provide realistic timing, costs, and practical details.
            """;
    }
    
    private String buildDayPlanningUserPrompt(CreateItineraryReq request, int startDay, int batchSize,
                                             List<String> previousDaysSummaries, Set<String> usedLocations) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("Plan days ").append(startDay).append("-").append(startDay + batchSize - 1)
              .append(" for a ").append(request.getDurationDays()).append("-day trip to ")
              .append(request.getDestination()).append("\n\n");
        
        // Trip details
        prompt.append("Trip Details:\n");
        prompt.append("- Destination: ").append(request.getDestination()).append("\n");
        prompt.append("- Start Date: ").append(request.getStartDate()).append("\n");
        prompt.append("- Budget Tier: ").append(request.getBudgetTier()).append("\n");
        prompt.append("- Party Size: ").append(request.getParty() != null ? request.getParty().getAdults() : 1).append(" adults\n");
        
        if (request.getInterests() != null && !request.getInterests().isEmpty()) {
            prompt.append("- Interests: ").append(String.join(", ", request.getInterests())).append("\n");
        }
        
        // Previous days context
        if (!previousDaysSummaries.isEmpty()) {
            prompt.append("\nPrevious Days Summary:\n");
            for (int i = 0; i < previousDaysSummaries.size(); i++) {
                prompt.append("Days ").append(i * MAX_DAYS_PER_BATCH + 1).append("-")
                      .append(Math.min((i + 1) * MAX_DAYS_PER_BATCH, startDay - 1)).append(": ")
                      .append(previousDaysSummaries.get(i)).append("\n");
            }
        }
        
        // Used locations to avoid
        if (!usedLocations.isEmpty()) {
            prompt.append("\nIMPORTANT - DO NOT use these locations (already visited):\n");
            List<String> locationList = new ArrayList<>(usedLocations);
            Collections.sort(locationList);
            for (String location : locationList.subList(0, Math.min(20, locationList.size()))) {
                prompt.append("- ").append(location).append("\n");
            }
            if (locationList.size() > 20) {
                prompt.append("... and ").append(locationList.size() - 20).append(" more locations\n");
            }
        }
        
        // Calculate dates for this batch
        LocalDate startDate = LocalDate.parse(request.getStartDate().toString());
        prompt.append("\nPlan the following days:\n");
        for (int i = 0; i < batchSize; i++) {
            LocalDate dayDate = startDate.plusDays(startDay - 1 + i);
            prompt.append("- Day ").append(startDay + i).append(" (").append(dayDate.format(DateTimeFormatter.ISO_LOCAL_DATE)).append(")\n");
        }
        
        prompt.append("\nCreate diverse, engaging days that build on the previous experience while exploring new areas and attractions.");
        
        return prompt.toString();
    }
    
    private String buildDayBatchJsonSchema() {
        return """
            {
              "type": "object",
              "properties": {
                "days": {
                  "type": "array",
                  "items": {
                    "type": "object",
                    "properties": {
                      "dayNumber": { "type": "integer" },
                      "date": { "type": "string", "format": "date" },
                      "location": { "type": "string", "description": "The specific city name where activities take place (e.g., 'Zurich', 'Interlaken'), NOT the country" },
                      "summary": { "type": "string" },
                      "nodes": {
                        "type": "array",
                        "minItems": 3,
                        "items": {
                          "type": "object",
                          "properties": {
                            "type": { "type": "string", "enum": ["attraction", "meal", "accommodation", "transport"] },
                            "title": { "type": "string" },
                            "location": {
                              "type": "object",
                              "properties": {
                                "name": { "type": "string" },
                                "address": { "type": "string" },
                                "coordinates": {
                                  "type": "object",
                                  "properties": {
                                    "lat": { "type": "number" },
                                    "lng": { "type": "number" }
                                  }
                                }
                              },
                              "required": ["name"]
                            },
                            "timing": {
                              "type": "object",
                              "properties": {
                                "startTime": { "type": "string" },
                                "endTime": { "type": "string" },
                                "durationMin": { "type": "integer" }
                              }
                            },
                            "cost": {
                              "type": "object",
                              "properties": {
                                "amountPerPerson": { "type": "number" },
                                "currency": { "type": "string", "default": "USD" }
                              }
                            },
                            "details": {
                              "type": "object",
                              "properties": {
                                "description": { "type": "string" },
                                "category": { "type": "string" },
                                "rating": { "type": "number" }
                              }
                            }
                          },
                          "required": ["type", "title", "location"]
                        }
                      }
                    },
                    "required": ["dayNumber", "date", "location", "nodes"]
                  }
                }
              },
              "required": ["days"]
            }
            """;
    }
    
    private ItineraryDto convertToItineraryDto(NormalizedItinerary normalized, CreateItineraryReq request) {
        return ItineraryDto.builder()
                .id(normalized.getItineraryId())
                .destination(request.getDestination())
                .startDate(LocalDate.parse(request.getStartDate().toString()))
                .endDate(LocalDate.parse(request.getEndDate().toString()))
                .party(request.getParty())
                .budgetTier(request.getBudgetTier())
                .interests(request.getInterests())
                .constraints(request.getConstraints())
                .language(request.getLanguage())
                .summary(normalized.getSummary())
                .status("completed")
                .createdAt(java.time.Instant.ofEpochMilli(normalized.getCreatedAt()))
                .updatedAt(java.time.Instant.ofEpochMilli(normalized.getUpdatedAt()))
                .build();
    }
    
    @Override
    protected String getAgentName() {
        return "Day-by-Day Planner Agent";
    }
    
    // ========== DAY BY DAY PLANNER AGENT - TOOL INTEGRATION METHODS ==========
    
    // Feature flags for tool integration
    @Value("${features.day-planner-tools.enabled:false}")
    private boolean dayPlannerToolsEnabled;
    
    @Value("${features.day-planner-tools.fallback-on-error:true}")
    private boolean fallbackOnError;
    
    private final RestTemplate restTemplate = new RestTemplate();
    
    /**
     * Generate node ID for new nodes using the Generate Node ID tool.
     */
    private String generateNodeIdViaTool(String itineraryId, Integer dayNumber, String nodeType) {
        if (!dayPlannerToolsEnabled) {
            return "day" + dayNumber + "_" + nodeType + "_" + System.currentTimeMillis();
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
            return fallbackOnError ? "day" + dayNumber + "_" + nodeType + "_" + System.currentTimeMillis() : null;
        } catch (Exception e) {
            logger.error("Generate Node ID tool error: {}", e.getMessage());
            return fallbackOnError ? "day" + dayNumber + "_" + nodeType + "_" + System.currentTimeMillis() : null;
        }
    }
    
    /**
     * Check user constraints (budget, dietary, party size) using the Check Constraints tool.
     */
    private com.tripplanner.dto.tools.ConstraintCheckResult checkConstraintsViaTool(String itineraryId) {
        if (!dayPlannerToolsEnabled) {
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
     * Check for conflicts (time, budget) using the Check Conflicts tool.
     */
    private com.tripplanner.dto.tools.ConflictCheckResult checkConflictsViaTool(
            String itineraryId, com.tripplanner.dto.ChangeSet proposedChanges) {
        if (!dayPlannerToolsEnabled) {
            return new com.tripplanner.dto.tools.ConflictCheckResult();
        }
        
        com.tripplanner.dto.tools.ConflictCheckRequest request = 
            new com.tripplanner.dto.tools.ConflictCheckRequest(itineraryId, proposedChanges);
        request.setCheckTimeConflicts(true);
        request.setCheckBudgetConflicts(true);
        
        try {
            com.tripplanner.dto.tools.ConflictCheckResult result = restTemplate.postForObject(
                "http://localhost:8080/api/v1/tools/check-conflicts",
                request,
                com.tripplanner.dto.tools.ConflictCheckResult.class
            );
            
            if (result != null && result.isHasConflicts()) {
                logger.warn("Conflicts detected in planning:");
                result.getConflicts().forEach(c -> 
                    logger.warn("  - {}: {}", c.getType(), c.getMessage()));
            }
            return result != null ? result : new com.tripplanner.dto.tools.ConflictCheckResult();
        } catch (Exception e) {
            logger.error("Check Conflicts tool error: {}", e.getMessage());
            return new com.tripplanner.dto.tools.ConflictCheckResult();
        }
    }
    
    /**
     * Validate timing feasibility using the Validate Timing tool.
     */
    private com.tripplanner.dto.tools.TimingValidationResult validateTimingViaTool(
            String itineraryId, Integer dayNumber) {
        if (!dayPlannerToolsEnabled) {
            return null;
        }
        
        com.tripplanner.dto.tools.TimingValidationRequest request = 
            new com.tripplanner.dto.tools.TimingValidationRequest(itineraryId, dayNumber);
        
        try {
            com.tripplanner.dto.tools.TimingValidationResult result = restTemplate.postForObject(
                "http://localhost:8080/api/v1/tools/validate-timing",
                request,
                com.tripplanner.dto.tools.TimingValidationResult.class
            );
            
            if (result != null && !result.isFeasible()) {
                logger.warn("Timing validation failed for day {}:", dayNumber);
                result.getIssues().forEach(issue -> 
                    logger.warn("  - {}", issue.getType()));
            }
            return result;
        } catch (Exception e) {
            logger.error("Validate Timing tool error: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Get weather-based timing suggestions using the Suggest Best Time tool.
     */
    private com.tripplanner.dto.tools.SuggestBestTimeResult suggestBestTimeViaTool(
            String itineraryId, String activityName, String location, String date, int durationMinutes) {
        if (!dayPlannerToolsEnabled) {
            return null;
        }
        
        com.tripplanner.dto.tools.SuggestBestTimeRequest request = 
            new com.tripplanner.dto.tools.SuggestBestTimeRequest();
        request.setActivityName(activityName);
        request.setActivityType("flexible");
        request.setLocation(location);
        request.setDate(date);
        request.setDuration(durationMinutes);
        request.setItineraryId(itineraryId);
        
        try {
            com.tripplanner.dto.tools.SuggestBestTimeResult result = restTemplate.postForObject(
                "http://localhost:8080/api/v1/tools/suggest-best-time",
                request,
                com.tripplanner.dto.tools.SuggestBestTimeResult.class
            );
            
            if (result != null && result.isSuccess()) {
                logger.debug("Weather-based timing suggested for {}: {}", 
                    activityName, result.getRecommendedTimeSlots().get(0).getStartTime());
            }
            return result;
        } catch (Exception e) {
            logger.error("Suggest Best Time tool error: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Validate LLM schema using the Validate Schema tool.
     */
    private boolean validateSchemaViaTool(String jsonOutput, String jsonSchema) {
        if (!dayPlannerToolsEnabled) {
            return true;
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
    
    // ========== END DAY BY DAY PLANNER AGENT TOOL INTEGRATION ==========
}