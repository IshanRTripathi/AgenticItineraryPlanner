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
 * TransportAgent - Specialized agent for populating transport nodes.
 * 
 * This agent focuses solely on adding detailed information to transport-type nodes
 * that were created as placeholders by SkeletonPlannerAgent.
 * 
 * Responsibilities:
 * - Add specific transport modes (taxi, bus, train, flight, walk)
 * - Add route information
 * - Add duration estimates
 * - Add booking/platform information
 * - Add practical travel tips
 * 
 * Does NOT handle: Activities, Meals, Costs, Exact coordinates
 * (Those are handled by specialized agents)
 * 
 * Processing Time: 8-12 seconds for a typical itinerary
 */
@Component
@ConditionalOnBean(AiClient.class)
public class TransportAgent extends BaseAgent {
    
    private final AiClient aiClient;
    private final ObjectMapper objectMapper;
    private final ItineraryJsonService itineraryJsonService;
    private final AgentEventPublisher agentEventPublisher;
    private final NodeIdGenerator nodeIdGenerator;
    
    public TransportAgent(AgentEventBus eventBus, AiClient aiClient, ObjectMapper objectMapper,
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
        
        // Pipeline-only task: populate transport nodes
        capabilities.addSupportedTask("populate_transport");
        
        capabilities.setPriority(10);
        capabilities.setChatEnabled(false); // Pipeline-only, not for chat
        capabilities.setConfigurationValue("nodeType", "transport");
        capabilities.setConfigurationValue("parallel", true);
        
        return capabilities;
    }
    
    /**
     * Populate transport nodes with detailed information.
     */
    public void populateTransport(String itineraryId, NormalizedItinerary skeleton) {
        logger.info("=== TRANSPORT AGENT ===");
        logger.info("Populating transport nodes for itinerary: {}", itineraryId);
        
        try {
            // Extract all transport nodes from skeleton
            List<TransportContext> transportContexts = extractTransportNodes(skeleton);
            
            if (transportContexts.isEmpty()) {
                logger.info("No transport nodes to populate");
                return;
            }
            
            logger.info("Found {} transport nodes to populate", transportContexts.size());
            
            // Populate transport with AI
            List<PopulatedTransport> populatedTransport = populateTransportWithAI(
                skeleton, transportContexts);
            
            // Update the itinerary with populated data
            updateItineraryWithTransport(itineraryId, skeleton, populatedTransport);
            
            logger.info("=== TRANSPORT AGENT COMPLETE ===");
            logger.info("Populated {} transport segments", populatedTransport.size());
            
            // Publish agent completion event via WebSocket
            if (agentEventPublisher.hasActiveConnections(itineraryId)) {
                String execId = "agent_" + System.currentTimeMillis();
                agentEventPublisher.publishAgentComplete(itineraryId, execId, 
                    "TransportAgent", populatedTransport.size());
            }
            
        } catch (Exception e) {
            logger.error("Failed to populate transport for itinerary: {}", itineraryId, e);
            // Don't throw - graceful degradation (keep placeholders)
        }
    }
    
    /**
     * Extract transport nodes from skeleton with context.
     */
    private List<TransportContext> extractTransportNodes(NormalizedItinerary skeleton) {
        List<TransportContext> contexts = new ArrayList<>();
        
        for (NormalizedDay day : skeleton.getDays()) {
            if (day.getNodes() == null) continue;
            
            // Get previous and next nodes for context
            for (int i = 0; i < day.getNodes().size(); i++) {
                NormalizedNode node = day.getNodes().get(i);
                if ("transport".equals(node.getType())) {
                    String fromLocation = i > 0 && day.getNodes().get(i-1).getLocation() != null 
                        ? day.getNodes().get(i-1).getLocation().getName() 
                        : day.getLocation();
                    String toLocation = i < day.getNodes().size() - 1 && day.getNodes().get(i+1).getLocation() != null
                        ? day.getNodes().get(i+1).getLocation().getName()
                        : day.getLocation();
                    
                    contexts.add(new TransportContext(
                        node.getId(),
                        day.getDayNumber(),
                        day.getLocation(),
                        node.getTiming(),
                        fromLocation,
                        toLocation
                    ));
                }
            }
        }
        
        return contexts;
    }
    
    /**
     * Populate transport using AI.
     */
    private List<PopulatedTransport> populateTransportWithAI(NormalizedItinerary skeleton,
                                                              List<TransportContext> contexts) {
        
        String systemPrompt = buildTransportSystemPrompt();
        String userPrompt = buildTransportUserPrompt(skeleton, contexts);
        String schema = buildTransportJsonSchema();
        
        logger.info("Calling AI to populate {} transport segments", contexts.size());
        
        String response = aiClient.generateStructuredContent(userPrompt, schema, systemPrompt);
        
        // Log full LLM response for analysis
        logger.info("=== TRANSPORT AGENT - FULL LLM RESPONSE ===");
        logger.info("Contexts to populate: {}", contexts.size());
        logger.info("Raw Response: {}", response);
        logger.info("=== END TRANSPORT AGENT RESPONSE ===");
        
        try {
            // Clean response by removing markdown formatting
            String cleanedResponse = cleanJsonResponse(response);
            logger.info("Cleaned Response: {}", cleanedResponse);
            com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(cleanedResponse);
            List<PopulatedTransport> transports = new ArrayList<>();
            
            if (root.has("transports") && root.get("transports").isArray()) {
                for (com.fasterxml.jackson.databind.JsonNode node : root.get("transports")) {
                    PopulatedTransport transport = objectMapper.treeToValue(node, PopulatedTransport.class);
                    transports.add(transport);
                }
            }
            
            return transports;
            
        } catch (Exception e) {
            logger.error("Failed to parse transport response", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Update itinerary with populated transport data.
     */
    private void updateItineraryWithTransport(String itineraryId, NormalizedItinerary skeleton,
                                              List<PopulatedTransport> populatedTransport) {
        
        Map<String, PopulatedTransport> transportMap = populatedTransport.stream()
            .collect(Collectors.toMap(PopulatedTransport::getNodeId, t -> t));
        
        for (NormalizedDay day : skeleton.getDays()) {
            if (day.getNodes() == null) continue;
            
            for (NormalizedNode node : day.getNodes()) {
                // Ensure node has ID
                nodeIdGenerator.ensureNodeHasId(node, day.getDayNumber(), skeleton);
                logger.debug("Ensuring node {} has ID for day {}", node.getTitle(), day.getDayNumber());
                
                if ("transport".equals(node.getType())) {
                    PopulatedTransport populated = transportMap.get(node.getId());
                    if (populated != null) {
                        node.setTitle(populated.getTitle());
                        
                        if (node.getDetails() == null) {
                            node.setDetails(new NodeDetails());
                        }
                        node.getDetails().setDescription(populated.getDescription());
                        node.getDetails().setCategory(populated.getMode());
                        
                        if (node.getTiming() != null && populated.getDurationMinutes() != null) {
                            node.getTiming().setDurationMin(populated.getDurationMinutes());
                        }
                    }
                }
            }
        }
        
        try {
            skeleton.setUpdatedAt(System.currentTimeMillis());
            itineraryJsonService.updateItinerary(skeleton);
            logger.info("Saved itinerary with populated transport");
        } catch (Exception e) {
            logger.error("Failed to save itinerary with transport: {}", e.getMessage());
        }
    }
    
    private String buildTransportSystemPrompt() {
        return """
            You are a transportation and logistics expert for travel planning.
            
            Your task: Suggest SPECIFIC transport modes and routes for getting around.
            
            Guidelines:
            1. Suggest practical transport modes based on distance and context
            2. Provide realistic duration estimates
            3. Include helpful booking or platform information when relevant
            4. Consider local transportation options
            5. Balance convenience, cost, and experience
            
            Transport Modes:
            - walk: For short distances (< 1km)
            - taxi: Door-to-door convenience
            - rideshare: Uber/Lyft equivalent
            - bus: Public buses, coaches
            - metro: Subway, underground
            - train: Regional/intercity trains
            - tram: City trams, streetcars
            - ferry: Water transport
            - flight: Domestic/international flights
            - car_rental: Self-drive options
            
            Include practical details like:
            - Approximate duration
            - Where to catch it
            - Booking requirements
            - Cost-saving tips
            
            Be practical and consider the traveler's perspective.
            """;
    }
    
    private String buildTransportUserPrompt(NormalizedItinerary skeleton, 
                                            List<TransportContext> contexts) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("Destination: ").append(skeleton.getDays().get(0).getLocation()).append("\n");
        prompt.append("Total Days: ").append(skeleton.getDays().size()).append("\n");
        
        prompt.append("\nTransport segments to populate:\n");
        for (TransportContext ctx : contexts) {
            prompt.append(String.format("- Day %d, Node ID: %s\n", ctx.dayNumber, ctx.nodeId));
            prompt.append(String.format("  From: %s → To: %s\n", ctx.fromLocation, ctx.toLocation));
            if (ctx.timing != null) {
                prompt.append(String.format("  Time: %s\n", ctx.timing.getStartTime()));
            }
        }
        
        prompt.append("\nProvide practical transport recommendations for each segment.");
        prompt.append("\nConsider distance, timing, local options, and traveler convenience.");
        
        return prompt.toString();
    }
    
    private String buildTransportJsonSchema() {
        return """
            {
              "type": "object",
              "properties": {
                "transports": {
                  "type": "array",
                  "items": {
                    "type": "object",
                    "properties": {
                      "nodeId": { "type": "string" },
                      "title": { "type": "string" },
                      "description": { "type": "string" },
                      "mode": { 
                        "type": "string",
                        "enum": ["walk", "taxi", "rideshare", "bus", "metro", "train", 
                                "tram", "ferry", "flight", "car_rental"]
                      },
                      "durationMinutes": { "type": "integer" }
                    },
                    "required": ["nodeId", "title", "description", "mode"]
                  }
                }
              },
              "required": ["transports"]
            }
            """;
    }
    
    @Override
    protected <T> T executeInternal(String itineraryId, AgentRequest<T> request) {
        NormalizedItinerary skeleton = request.getData(NormalizedItinerary.class);
        populateTransport(itineraryId, skeleton);
        return (T) skeleton;
    }
    
    @Override
    protected String getAgentName() {
        return "Transport Agent";
    }
    
    // Helper classes
    
    private static class TransportContext {
        String nodeId;
        int dayNumber;
        String dayLocation;
        NodeTiming timing;
        String fromLocation;
        String toLocation;
        
        public TransportContext(String nodeId, int dayNumber, String dayLocation, 
                               NodeTiming timing, String fromLocation, String toLocation) {
            this.nodeId = nodeId;
            this.dayNumber = dayNumber;
            this.dayLocation = dayLocation;
            this.timing = timing;
            this.fromLocation = fromLocation;
            this.toLocation = toLocation;
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
    
    public static class PopulatedTransport {
        private String nodeId;
        private String title;
        private String description;
        private String mode;
        private Integer durationMinutes;
        
        // Getters and setters
        public String getNodeId() { return nodeId; }
        public void setNodeId(String nodeId) { this.nodeId = nodeId; }
        
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public String getMode() { return mode; }
        public void setMode(String mode) { this.mode = mode; }
        
        public Integer getDurationMinutes() { return durationMinutes; }
        public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }
    }
}

