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
    
    public ActivityAgent(AgentEventBus eventBus, AiClient aiClient, ObjectMapper objectMapper,
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
            // Extract all attraction nodes from skeleton
            List<AttractionContext> attractionContexts = extractAttractionNodes(skeleton);
            
            if (attractionContexts.isEmpty()) {
                logger.info("No attraction nodes to populate");
                return;
            }
            
            logger.info("Found {} attraction nodes to populate", attractionContexts.size());
            
            // Populate attractions with AI
            List<PopulatedAttraction> populatedAttractions = populateAttractionsWithAI(
                skeleton, attractionContexts);
            
            // Update the itinerary with populated data
            updateItineraryWithAttractions(itineraryId, skeleton, populatedAttractions);
            
            logger.info("=== ACTIVITY AGENT COMPLETE ===");
            logger.info("Populated {} attractions", populatedAttractions.size());
            
        } catch (Exception e) {
            logger.error("Failed to populate attractions for itinerary: {}", itineraryId, e);
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
            // Clean response by removing markdown formatting
            String cleanedResponse = cleanJsonResponse(response);
            logger.info("Cleaned Response: {}", cleanedResponse);
            com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(cleanedResponse);
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
     * Update itinerary with populated attraction data.
     */
    private void updateItineraryWithAttractions(String itineraryId, NormalizedItinerary skeleton,
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
                        
                        // Update location name if available
                        if (node.getLocation() != null && populated.getLocationName() != null) {
                            node.getLocation().setName(populated.getLocationName());
                        }
                    }
                }
            }
        }
        
        // Save updated itinerary
        try {
            skeleton.setUpdatedAt(System.currentTimeMillis());
            itineraryJsonService.updateItinerary(skeleton);
            logger.info("Saved itinerary with populated attractions");
        } catch (Exception e) {
            logger.error("Failed to save itinerary with attractions: {}", e.getMessage());
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
            5. Estimate realistic visit durations
            6. Consider the time slot and day context
            7. Ensure variety across the day and trip
            
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
            """;
    }
    
    private String buildActivityUserPrompt(NormalizedItinerary skeleton, 
                                           List<AttractionContext> contexts) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("Destination: ").append(skeleton.getDays().get(0).getLocation()).append("\n");
        prompt.append("Total Days: ").append(skeleton.getDays().size()).append("\n");
        
        if (skeleton.getThemes() != null && !skeleton.getThemes().isEmpty()) {
            prompt.append("Interests: ").append(String.join(", ", skeleton.getThemes())).append("\n");
        }
        
        prompt.append("\nAttraction slots to populate:\n");
        for (AttractionContext ctx : contexts) {
            prompt.append(String.format("- Day %d, Node ID: %s, Time: %s\n",
                ctx.dayNumber, ctx.nodeId, 
                ctx.timing != null ? ctx.timing.getStartTime() : "TBD"));
        }
        
        prompt.append("\nCRITICAL: Use the EXACT node IDs listed above. Do NOT generate your own node IDs.");
        prompt.append("\nProvide specific attraction names, descriptions, and details for each slot.");
        prompt.append("\nEnsure variety and avoid repetition across days.");
        
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
                      "locationName": { "type": "string" }
                    },
                    "required": ["nodeId", "title", "description", "category"]
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

