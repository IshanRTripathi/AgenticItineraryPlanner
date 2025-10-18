package com.tripplanner.agents;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripplanner.dto.*;
import com.tripplanner.service.AgentEventBus;
import com.tripplanner.service.AgentEventPublisher;
import com.tripplanner.service.ItineraryJsonService;
import com.tripplanner.service.NodeIdGenerator;
import com.tripplanner.service.ai.AiClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * SkeletonPlannerAgent - Generates lightweight day structure with placeholder nodes.
 * 
 * This agent is the first step in the pipeline architecture, creating the basic
 * structure of the itinerary without detailed information. Other specialized agents
 * will populate the details later.
 * 
 * Responsibilities:
 * - Create day structure (day number, date, location)
 * - Create placeholder nodes (type, rough timing)
 * - Determine node sequence (logical flow)
 * - No detailed information (titles, descriptions, costs)
 * 
 * Output: Lightweight skeleton that other agents can populate
 * Processing Time: 15-20 seconds (vs 60-150s for complete generation)
 */
@Component
@ConditionalOnBean(AiClient.class)
public class SkeletonPlannerAgent extends BaseAgent {
    
    private final AiClient aiClient;
    private final ObjectMapper objectMapper;
    private final ItineraryJsonService itineraryJsonService;
    private final AgentEventPublisher agentEventPublisher;
    private final NodeIdGenerator nodeIdGenerator;
    
    // Configuration
    private static final int DAYS_PER_BATCH = 1; // Generate 1 day at a time for maximum reliability
    
    public SkeletonPlannerAgent(AgentEventBus eventBus, AiClient aiClient, ObjectMapper objectMapper,
                               ItineraryJsonService itineraryJsonService, AgentEventPublisher agentEventPublisher,
                               NodeIdGenerator nodeIdGenerator) {
        super(eventBus, AgentEvent.AgentKind.PLANNER);
        this.aiClient = aiClient;
        this.objectMapper = objectMapper;
        this.itineraryJsonService = itineraryJsonService;
        this.agentEventPublisher = agentEventPublisher;
        this.nodeIdGenerator = nodeIdGenerator;
    }
    
    @Override
    public AgentCapabilities getCapabilities() {
        AgentCapabilities capabilities = new AgentCapabilities();
        
        // Pipeline-only task: skeleton (creates itinerary structure)
        capabilities.addSupportedTask("skeleton");
        
        capabilities.setPriority(1); // Highest priority - runs first in pipeline
        capabilities.setChatEnabled(false); // Pipeline-only, not for chat
        capabilities.setConfigurationValue("daysPerBatch", DAYS_PER_BATCH);
        capabilities.setConfigurationValue("lightweight", true);
        capabilities.setConfigurationValue("fastGeneration", true);
        
        return capabilities;
    }
    
    /**
     * Generate skeleton itinerary structure.
     */
    public NormalizedItinerary generateSkeleton(String itineraryId, CreateItineraryReq request) {
        logger.info("=== SKELETON PLANNER AGENT ===");
        logger.info("Generating lightweight skeleton for {} days", request.getDurationDays());
        logger.info("Destination: {}", request.getDestination());
        
        try {
            // Create initial itinerary
            NormalizedItinerary itinerary = createInitialItinerary(itineraryId, request);
            
            // Generate days in small batches
            int totalDays = request.getDurationDays();
            int processedDays = 0;
            
            while (processedDays < totalDays) {
                int remainingDays = totalDays - processedDays;
                int batchSize = Math.min(DAYS_PER_BATCH, remainingDays);
                
                emitProgress(itineraryId, 
                    (int) (20 + (processedDays * 60.0 / totalDays)), 
                    String.format("Creating day %d structure", processedDays + 1),
                    "skeleton_generation");
                
                // Generate skeleton for this day
                NormalizedDay day = generateDaySkeleton(request, processedDays + 1);
                itinerary.getDays().add(day);
                
                // Save immediately for real-time access
                try {
                    itinerary.setUpdatedAt(System.currentTimeMillis());
                    itineraryJsonService.updateItinerary(itinerary);
                    
                    if (agentEventPublisher.hasActiveConnections(itineraryId)) {
                        agentEventPublisher.publishDayCompleted(itineraryId, 
                            "exec_" + System.currentTimeMillis(), day);
                    }
                    
                    logger.info("Skeleton day {} created and saved", day.getDayNumber());
                } catch (Exception e) {
                    logger.warn("Failed to save skeleton day {}: {}", day.getDayNumber(), e.getMessage());
                }
                
                processedDays += batchSize;
            }
            
            emitProgress(itineraryId, 80, "Skeleton complete", "skeleton_complete");
            
            // Final save
            itinerary.setUpdatedAt(System.currentTimeMillis());
            itineraryJsonService.updateItinerary(itinerary);
            
            logger.info("=== SKELETON COMPLETE ===");
            logger.info("Generated {} days with {} total node placeholders", 
                itinerary.getDays().size(),
                itinerary.getDays().stream().mapToInt(d -> d.getNodes() != null ? d.getNodes().size() : 0).sum());
            
            return itinerary;
            
        } catch (Exception e) {
            logger.error("Failed to generate skeleton for itinerary: {}", itineraryId, e);
            throw new RuntimeException("Skeleton generation failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Generate skeleton for a single day.
     */
    private NormalizedDay generateDaySkeleton(CreateItineraryReq request, int dayNumber) {
        String systemPrompt = buildSkeletonSystemPrompt();
        String userPrompt = buildSkeletonUserPrompt(request, dayNumber);
        String schema = buildSkeletonJsonSchema();
        
        logger.info("Generating skeleton for day {} with minimal prompt", dayNumber);
        
        String response = aiClient.generateStructuredContent(userPrompt, schema, systemPrompt);
        
        // Log full LLM response for analysis
        logger.info("=== SKELETON PLANNER AGENT - FULL LLM RESPONSE ===");
        logger.info("Day: {}", dayNumber);
        logger.info("Raw Response: {}", response);
        logger.info("=== END SKELETON PLANNER RESPONSE ===");
        
        try {
            // Clean response by removing markdown formatting
            String cleanedResponse = cleanJsonResponse(response);
            logger.info("Cleaned Response: {}", cleanedResponse);
            com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(cleanedResponse);
            
            // Normalize time fields (convert HH:mm strings to milliseconds)
            normalizeTimeFields(root, request.getStartDate().toString(), dayNumber);
            
            NormalizedDay day = objectMapper.treeToValue(root, NormalizedDay.class);
            
            // Ensure basic fields are set
            if (day.getDayNumber() == 0) {
                day.setDayNumber(dayNumber);
            }
            
            LocalDate startDate = LocalDate.parse(request.getStartDate().toString());
            LocalDate dayDate = startDate.plusDays(dayNumber - 1);
            day.setDate(dayDate.format(DateTimeFormatter.ISO_LOCAL_DATE));
            
            // Generate IDs for nodes if missing
            if (day.getNodes() != null) {
                for (int i = 0; i < day.getNodes().size(); i++) {
                    NormalizedNode node = day.getNodes().get(i);
                    if (node.getId() == null || node.getId().isEmpty()) {
                        String nodeId = nodeIdGenerator.generateSkeletonNodeId(dayNumber, i + 1, node.getType());
                        node.setId(nodeId);
                        logger.debug("Assigned ID {} to node: {}", nodeId, node.getTitle());
                    }
                    
                    // Set placeholder values
                    if (node.getTitle() == null || node.getTitle().isEmpty()) {
                        node.setTitle(String.format("%s placeholder", node.getType()));
                    }
                    
                    // Initialize location if null
                    if (node.getLocation() == null) {
                        node.setLocation(new NodeLocation());
                        node.getLocation().setName(request.getDestination());
                    }
                }
            }
            
            return day;
            
        } catch (JsonProcessingException e) {
            logger.error("Failed to parse skeleton response", e);
            throw new RuntimeException("Failed to parse skeleton: " + e.getMessage(), e);
        }
    }
    
    private NormalizedItinerary createInitialItinerary(String itineraryId, CreateItineraryReq request) {
        NormalizedItinerary itinerary = new NormalizedItinerary();
        itinerary.setItineraryId(itineraryId);
        itinerary.setVersion(1);
        itinerary.setSummary(String.format("%d-day trip to %s (skeleton)", 
            request.getDurationDays(), request.getDestination()));
        itinerary.setCurrency("INR");
        itinerary.setThemes(request.getInterests() != null ? request.getInterests() : new ArrayList<>());
        itinerary.setDays(new ArrayList<>());
        itinerary.setCreatedAt(System.currentTimeMillis());
        itinerary.setUpdatedAt(System.currentTimeMillis());
        
        return itinerary;
    }
    
    private String buildSkeletonSystemPrompt() {
        return """
            You are a travel planning assistant creating a DAY STRUCTURE SKELETON.
            
            IMPORTANT: Generate ONLY the basic structure, NOT detailed information.
            
            Your job:
            1. Create time slots for the day (morning to evening)
            2. Assign node types to each slot (attraction, meal, transport)
            3. Set rough timing (start/end times)
            4. Use placeholder titles
            5. CRITICAL: Use consistent node ID format: "day{dayNumber}_node{sequenceNumber}"
            
            Node ID Format Rules:
            - Day 1: "day1_node1", "day1_node2", "day1_node3", etc.
            - Day 2: "day2_node1", "day2_node2", "day2_node3", etc.
            - Day 3: "day3_node1", "day3_node2", "day3_node3", etc.
            - Always use underscore between day and node
            - Always use sequential numbering starting from 1
            
            Do NOT include:
            - Specific place names (use "Morning Activity", "Lunch Spot", etc.)
            - Detailed descriptions
            - Costs
            - Exact locations/addresses
            - Reviews or ratings
            
            Node Types:
            - "attraction": Morning/afternoon activity placeholder
            - "meal": Breakfast/lunch/dinner placeholder
            - "transport": If moving between areas
            - "accommodation": If overnight stay
            
            Each day should have 4-7 node placeholders:
            - 2-3 attraction placeholders
            - 2-3 meal placeholders
            - 0-2 transport placeholders (if needed)
            - 0-1 accommodation placeholder (if overnight)
            
            Keep it simple and fast - other agents will add details later.
            """;
    }
    
    private String buildSkeletonUserPrompt(CreateItineraryReq request, int dayNumber) {
        StringBuilder prompt = new StringBuilder();
        
        LocalDate startDate = LocalDate.parse(request.getStartDate().toString());
        LocalDate dayDate = startDate.plusDays(dayNumber - 1);
        
        prompt.append("Create skeleton structure for:\n");
        prompt.append("Day ").append(dayNumber).append(" of ").append(request.getDurationDays()).append("\n");
        prompt.append("Date: ").append(dayDate.format(DateTimeFormatter.ISO_LOCAL_DATE)).append("\n");
        prompt.append("Destination: ").append(request.getDestination()).append("\n");
        prompt.append("Budget: ").append(request.getBudgetTier()).append("\n");
        
        if (request.getInterests() != null && !request.getInterests().isEmpty()) {
            prompt.append("Interests: ").append(String.join(", ", request.getInterests())).append("\n");
        }
        
        prompt.append("\nGenerate time slots with node type placeholders.\n");
        prompt.append("Use generic titles like 'Morning Activity', 'Lunch Break', etc.\n");
        prompt.append("Focus on timing and logical flow, not specific places.\n");
        prompt.append("CRITICAL: Use node IDs in format 'day").append(dayNumber).append("_node1', 'day").append(dayNumber).append("_node2', etc.\n");
        
        return prompt.toString();
    }
    
    private String buildSkeletonJsonSchema() {
        return """
            {
              "type": "object",
              "properties": {
                "dayNumber": { "type": "integer" },
                "date": { "type": "string", "format": "date" },
                "location": { "type": "string" },
                "summary": { "type": "string" },
                "nodes": {
                  "type": "array",
                  "minItems": 4,
                  "maxItems": 7,
                  "items": {
                    "type": "object",
                    "properties": {
                      "id": { "type": "string" },
                      "type": { 
                        "type": "string", 
                        "enum": ["attraction", "meal", "accommodation", "transport"] 
                      },
                      "title": { "type": "string" },
                      "location": {
                        "type": "object",
                        "properties": {
                          "name": { "type": "string" }
                        }
                      },
                      "timing": {
                        "type": "object",
                        "properties": {
                          "startTime": { "type": "string" },
                          "endTime": { "type": "string" },
                          "durationMin": { "type": "integer" }
                        }
                      }
                    },
                    "required": ["type", "timing"]
                  }
                }
              },
              "required": ["dayNumber", "date", "location", "nodes"]
            }
            """;
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
            throw new IllegalArgumentException("SkeletonPlannerAgent requires CreateItineraryReq");
        }
        
        NormalizedItinerary skeleton = generateSkeleton(itineraryId, itineraryReq);
        
        return (T) skeleton;
    }
    
    @Override
    protected String getAgentName() {
        return "Skeleton Planner Agent";
    }
    
    /**
     * Clean JSON response by removing markdown formatting and corrupted content.
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
        
        // Remove any non-ASCII characters that might be breaking JSON
        // This handles cases where AI returns mixed language content
        cleaned = cleaned.replaceAll("[^\\x00-\\x7F]", "");
        
        // Remove any text that appears between JSON properties (like Japanese text)
        // Pattern: } followed by non-JSON characters, then "id":
        cleaned = cleaned.replaceAll("}\\s*[^\\s\"{}\\[\\],:]+\\s*,\\s*\"id\"", "},\"id\"");
        
        // Remove any text that appears between JSON properties
        // Pattern: } followed by non-JSON characters, then "type":
        cleaned = cleaned.replaceAll("}\\s*[^\\s\"{}\\[\\],:]+\\s*,\\s*\"type\"", "},\"type\"");
        
        // Remove any text that appears between JSON properties
        // Pattern: } followed by non-JSON characters, then "title":
        cleaned = cleaned.replaceAll("}\\s*[^\\s\"{}\\[\\],:]+\\s*,\\s*\"title\"", "},\"title\"");
        
        // Remove any text that appears between JSON properties
        // Pattern: } followed by non-JSON characters, then "location":
        cleaned = cleaned.replaceAll("}\\s*[^\\s\"{}\\[\\],:]+\\s*,\\s*\"location\"", "},\"location\"");
        
        // Remove any text that appears between JSON properties
        // Pattern: } followed by non-JSON characters, then "timing":
        cleaned = cleaned.replaceAll("}\\s*[^\\s\"{}\\[\\],:]+\\s*,\\s*\"timing\"", "},\"timing\"");
        
        // Clean up any remaining malformed content
        cleaned = cleaned.replaceAll("}\\s*[^\\s\"{}\\[\\],:]+\\s*,", "},");
        
        return cleaned.trim();
    }
    
    /**
     * Normalize time fields in the JSON tree.
     * Converts time strings like "14:00" to milliseconds since epoch.
     */
    private void normalizeTimeFields(com.fasterxml.jackson.databind.JsonNode root, String startDate, int dayNumber) {
        if (root == null || !root.has("nodes") || !root.get("nodes").isArray()) {
            return;
        }
        
        // Calculate the date for this day
        LocalDate baseDate = LocalDate.parse(startDate);
        LocalDate dayDate = baseDate.plusDays(dayNumber - 1);
        String dayDateStr = dayDate.format(DateTimeFormatter.ISO_LOCAL_DATE);
        
        // Process each node's timing
        com.fasterxml.jackson.databind.node.ArrayNode nodesArray = (com.fasterxml.jackson.databind.node.ArrayNode) root.get("nodes");
        for (com.fasterxml.jackson.databind.JsonNode node : nodesArray) {
            if (!node.has("timing")) continue;
            
            com.fasterxml.jackson.databind.node.ObjectNode timing = (com.fasterxml.jackson.databind.node.ObjectNode) node.get("timing");
            if (timing == null) continue;
            
            // Normalize startTime and endTime
            normalizeTimeField(timing, "startTime", dayDateStr);
            normalizeTimeField(timing, "endTime", dayDateStr);
        }
    }
    
    /**
     * Normalize a single time field from HH:mm string to milliseconds since epoch.
     */
    private void normalizeTimeField(com.fasterxml.jackson.databind.node.ObjectNode timing, String field, String dayDate) {
        if (!timing.has(field)) return;
        
        String value = timing.get(field).asText(null);
        if (value == null || value.isBlank()) return;
        
        try {
            // If already a number, keep it as is
            if (value.matches("^\\d+$")) {
                return; // Already a timestamp
            }
            
            // If time string HH:mm, combine with dayDate and convert to milliseconds
            if (value.matches("^\\d{2}:\\d{2}$")) {
                String iso = dayDate + "T" + value + ":00Z";
                long milliseconds = java.time.Instant.parse(iso).toEpochMilli();
                timing.put(field, milliseconds);
                logger.debug("Converted {} '{}' to timestamp {}", field, value, milliseconds);
                return;
            }
            
        } catch (Exception e) {
            logger.warn("Failed to normalize time field {} with value {}: {}", field, value, e.getMessage());
            // If parsing fails, set to null
            timing.putNull(field);
        }
    }
}

