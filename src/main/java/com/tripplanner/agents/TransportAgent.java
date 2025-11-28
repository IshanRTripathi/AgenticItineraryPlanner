package com.tripplanner.agents;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripplanner.dto.*;
import com.tripplanner.enums.ProcessingState;
import com.tripplanner.enums.TransportMode;
import com.tripplanner.exception.GeographyException;
import com.tripplanner.exception.ValidationException;
import com.tripplanner.service.*;
import com.tripplanner.service.agents.AgentEventBus;
import com.tripplanner.service.agents.AgentEventPublisher;
import com.tripplanner.service.ai.AiClient;
import com.tripplanner.service.llm.LLMSchemaValidator;
import com.tripplanner.service.utilities.NodeIdGenerator;
import com.tripplanner.dto.tools.NodeIdRequest;
import com.tripplanner.dto.tools.NodeIdResponse;
import com.tripplanner.dto.tools.ConstraintCheckRequest;
import com.tripplanner.dto.tools.ConstraintCheckResult;
import com.tripplanner.dto.tools.SchemaValidationRequest;
import com.tripplanner.dto.tools.SchemaValidationResult;
import com.tripplanner.dto.tools.DistanceCalculationRequest;
import com.tripplanner.dto.tools.DistanceCalculationResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

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
    private final GoogleMapsDistanceService distanceService;
    private final LLMSchemaValidator schemaValidator;
    private final GeographyService geographyService;
    private final TransportMetadataService metadataService;
    private final ItineraryValidator itineraryValidator;
    private final RestTemplate restTemplate;
    
    // Feature flags for tool integration
    @Value("${features.transport-tools.enabled:false}")
    private boolean transportToolsEnabled;
    
    @Value("${features.transport-tools.fallback-on-error:true}")
    private boolean fallbackOnError;
    
    public TransportAgent(AgentEventBus eventBus, AiClient aiClient, ObjectMapper objectMapper,
                          ItineraryJsonService itineraryJsonService, AgentEventPublisher agentEventPublisher,
                          NodeIdGenerator nodeIdGenerator, GoogleMapsDistanceService distanceService,
                          LLMSchemaValidator schemaValidator, GeographyService geographyService,
                          TransportMetadataService metadataService, ItineraryValidator itineraryValidator) {
        super(eventBus, AgentEvent.AgentKind.ENRICHMENT);
        this.aiClient = aiClient;
        this.objectMapper = objectMapper;
        this.itineraryJsonService = itineraryJsonService;
        this.agentEventPublisher = agentEventPublisher;
        this.nodeIdGenerator = nodeIdGenerator;
        this.distanceService = distanceService;
        this.schemaValidator = schemaValidator;
        this.geographyService = geographyService;
        this.metadataService = metadataService;
        this.itineraryValidator = itineraryValidator;
        this.restTemplate = new RestTemplate();
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
        populateTransport(itineraryId, skeleton, false);
    }
    
    /**
     * Populate transport nodes with detailed information.
     * @param skipSave if true, modifies skeleton in-place without saving (for parallel execution)
     */
    public void populateTransport(String itineraryId, NormalizedItinerary skeleton, boolean skipSave) {
        logger.info("=== TRANSPORT AGENT ===");
        logger.info("Populating transport nodes for itinerary: {} (skipSave={})", itineraryId, skipSave);
        
        try {
            emitProgress(itineraryId, 10, "Loading transport data", "loading");
            
            // Read CityAllocationPlan for inter-city travel
            CityAllocationPlan cityPlan = null;
            if (skeleton.getAgentData() != null && skeleton.getAgentData().containsKey("cityAllocation")) {
                AgentDataSection agentDataSection = skeleton.getAgentData().get("cityAllocation");
                cityPlan = agentDataSection.getAgentData("cityAllocation", CityAllocationPlan.class);
                if (cityPlan != null) {
                    logger.info("Loaded city allocation plan for inter-city travel: {} travel segments", 
                               cityPlan.getTravelSegments() != null ? cityPlan.getTravelSegments().size() : 0);
                }
            }
            
            // Create inter-city transport nodes from travel segments
            if (cityPlan != null && cityPlan.getTravelSegments() != null) {
                for (TravelSegment segment : cityPlan.getTravelSegments()) {
                    createInterCityTransportNode(skeleton, segment);
                }
            }
            
            // IMPORTANT: Ensure all nodes have IDs BEFORE extracting contexts
            // This prevents duplicate key errors when collecting into a Map
            for (NormalizedDay day : skeleton.getDays()) {
                if (day.getNodes() == null) continue;
                for (NormalizedNode node : day.getNodes()) {
                    nodeIdGenerator.ensureNodeHasId(node, day.getDayNumber(), skeleton);
                }
            }
            
            // NEW: Populate metadata for all transport nodes BEFORE LLM call
            emitProgress(itineraryId, 20, "Analyzing geography and routes", "processing");
            populateTransportMetadata(skeleton);
            
            // Extract all transport nodes from skeleton
            List<TransportContext> transportContexts = extractTransportNodes(skeleton);
            
            if (transportContexts.isEmpty()) {
                logger.info("No transport nodes to populate");
                emitProgress(itineraryId, 100, "No transport to populate", "complete");
                return;
            }
            
            logger.info("Found {} transport nodes to populate", transportContexts.size());
            emitProgress(itineraryId, 30, 
                String.format("Populating %d transport segments", transportContexts.size()), 
                "populating");
            
            // Populate transport with AI
            List<PopulatedTransport> populatedTransport = populateTransportWithAI(
                skeleton, transportContexts);
            
            if (skipSave) {
                // Collect-only mode: Apply data to skeleton without saving
                logger.info("Applying transport data to skeleton (skipSave=true, no database write)");
                emitProgress(itineraryId, 70, "Applying transport data", "applying");
                
                // Apply transport data inline (same logic as updateItineraryWithTransport but without save)
                applyTransportDataToSkeleton(skeleton, populatedTransport);
                
                // Metadata already populated earlier in the method
                logger.info("Transport data applied to skeleton (in-memory only)");
                
                emitProgress(itineraryId, 100, 
                    String.format("Collected %d transport segments (not saved)", populatedTransport.size()), 
                    "collected");
            } else {
                // Normal mode: Apply and save
                emitProgress(itineraryId, 70, "Saving transport data", "saving");
                updateItineraryWithTransport(itineraryId, skeleton, populatedTransport);
                
                emitProgress(itineraryId, 100, 
                    String.format("Populated %d transport segments", populatedTransport.size()), 
                    "complete");
            }
            
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
            emitProgress(itineraryId, 0, "Failed to populate transport", "error");
            // Don't throw - graceful degradation (keep placeholders)
        }
    }
    
    /**
     * Create inter-city transport node from travel segment.
     */
    private void createInterCityTransportNode(NormalizedItinerary skeleton, TravelSegment segment) {
        try {
            NormalizedDay day = findDayByNumber(skeleton, segment.getDayNumber());
            if (day == null) {
                logger.warn("Day {} not found for travel segment", segment.getDayNumber());
                return;
            }
            
            // Find or create transport node
            NormalizedNode transportNode = findOrCreateTransportNode(day, segment);
            
            // Populate transport node with segment details
            populateTransportNode(transportNode, segment);
            
            logger.info("Created inter-city transport node for day {}: {} -> {}", 
                       segment.getDayNumber(), segment.getFromCity(), segment.getToCity());
            
        } catch (Exception e) {
            logger.error("Failed to create inter-city transport node: {}", e.getMessage());
        }
    }
    
    /**
     * Find day by number.
     */
    private NormalizedDay findDayByNumber(NormalizedItinerary itinerary, int dayNumber) {
        if (itinerary.getDays() == null) {
            return null;
        }
        
        return itinerary.getDays().stream()
            .filter(d -> d.getDayNumber() == dayNumber)
            .findFirst()
            .orElse(null);
    }
    
    /**
     * Find or create transport node for travel segment.
     * IMPROVED: Better detection of existing placeholders created by SkeletonPlannerAgent.
     */
    private NormalizedNode findOrCreateTransportNode(NormalizedDay day, TravelSegment segment) {
        // Check if transport node already exists (created by SkeletonPlannerAgent)
        if (day.getNodes() != null) {
            for (NormalizedNode node : day.getNodes()) {
                if ("transport".equals(node.getType())) {
                    logger.info("Found existing transport placeholder for day {}: {}", 
                               day.getDayNumber(), node.getId());
                    return node; // Found existing placeholder
                }
            }
        }
        
        // Fallback: Create new transport node if placeholder not found
        // This should rarely happen now that SkeletonPlannerAgent creates placeholders
        logger.warn("No transport placeholder found for day {}, creating fallback node (this should not happen often)", 
                   day.getDayNumber());
        
        NormalizedNode transportNode = new NormalizedNode();
        transportNode.setType("transport");
        // FIXED: Don't set ID here - let NodeIdGenerator assign it later
        transportNode.setTitle(String.format("Travel: %s to %s", 
                                             segment.getFromCity(), 
                                             segment.getToCity()));
        
        // Set location
        NodeLocation location = new NodeLocation();
        location.setName(String.format("%s to %s", segment.getFromCity(), segment.getToCity()));
        transportNode.setLocation(location);
        
        if (day.getNodes() == null) {
            day.setNodes(new ArrayList<>());
        }
        day.getNodes().add(0, transportNode); // Add at beginning of day
        
        logger.info("Created fallback transport node for day {} (ID will be assigned by NodeIdGenerator)", 
                   day.getDayNumber());
        
        return transportNode;
    }
    
    /**
     * Populate transport node with travel segment details.
     */
    private void populateTransportNode(NormalizedNode node, TravelSegment segment) {
        node.setTitle(String.format("Travel: %s to %s", 
                                   segment.getFromCity(), 
                                   segment.getToCity()));
        
        if (node.getDetails() == null) {
            node.setDetails(new NodeDetails());
        }
        
        node.getDetails().setDescription(
            String.format("Travel by %s from %s to %s. Estimated duration: %d hours. %s",
                        segment.getTravelMode(),
                        segment.getFromCity(),
                        segment.getToCity(),
                        segment.getEstimatedHours(),
                        segment.getNotes() != null ? segment.getNotes() : "")
        );
        
        node.getDetails().setCategory("inter-city-travel");
        
        // Set timing
        if (node.getTiming() == null) {
            node.setTiming(new NodeTiming());
        }
        
        if (segment.isFullDayTravel()) {
            node.getTiming().setStartTime(8L * 60 * 60 * 1000); // 8:00 AM in epoch ms (simplified)
            node.getTiming().setEndTime(18L * 60 * 60 * 1000); // 6:00 PM in epoch ms (simplified)
            node.getTiming().setDurationMin(segment.getEstimatedHours() * 60);
        } else {
            node.getTiming().setStartTime(9L * 60 * 60 * 1000); // 9:00 AM in epoch ms (simplified)
            node.getTiming().setEndTime((9L + segment.getEstimatedHours()) * 60 * 60 * 1000);
            node.getTiming().setDurationMin(segment.getEstimatedHours() * 60);
        }
        
        // Set location
        if (node.getLocation() == null) {
            node.setLocation(new NodeLocation());
        }
        node.getLocation().setName(segment.getFromCity() + " to " + segment.getToCity());
    }
    
    /**
     * Extract transport nodes from skeleton with context.
     * IMPROVED: Better detection of inter-city vs local transport, handles arrival/departure flights.
     */
    private List<TransportContext> extractTransportNodes(NormalizedItinerary skeleton) {
        List<TransportContext> contexts = new ArrayList<>();
        
        // Load city allocation plan to identify inter-city travel
        CityAllocationPlan cityPlan = null;
        if (skeleton.getAgentData() != null && skeleton.getAgentData().containsKey("cityAllocation")) {
            AgentDataSection agentDataSection = skeleton.getAgentData().get("cityAllocation");
            cityPlan = agentDataSection.getAgentData("cityAllocation", CityAllocationPlan.class);
        }
        
        for (NormalizedDay day : skeleton.getDays()) {
            if (day.getNodes() == null) continue;
            
            // Get previous and next nodes for context
            for (int i = 0; i < day.getNodes().size(); i++) {
                NormalizedNode node = day.getNodes().get(i);
                if ("transport".equals(node.getType())) {
                    // CRITICAL: Determine transport type from node title and position
                    String title = node.getTitle() != null ? node.getTitle().toLowerCase() : "";
                    boolean isArrival = title.contains("arrival");
                    boolean isDeparture = title.contains("departure");
                    boolean isInterCityTravel = title.contains("travel:") || 
                                               (node.getId() != null && node.getId().contains("_travel"));
                    
                    String fromLocation;
                    String toLocation;
                    TransportType transportType;
                    
                    if (isArrival) {
                        // Arrival flight: Parse from title "Arrival: CityA to CityB"
                        transportType = TransportType.ARRIVAL_FLIGHT;
                        String[] parts = extractLocationsFromTitle(node.getTitle());
                        fromLocation = parts[0];
                        toLocation = parts[1];
                        logger.info("Detected ARRIVAL flight: {} -> {}", fromLocation, toLocation);
                    } else if (isDeparture) {
                        // Departure flight: Parse from title "Departure: CityA to CityB"
                        transportType = TransportType.DEPARTURE_FLIGHT;
                        String[] parts = extractLocationsFromTitle(node.getTitle());
                        fromLocation = parts[0];
                        toLocation = parts[1];
                        logger.info("Detected DEPARTURE flight: {} -> {}", fromLocation, toLocation);
                    } else if (isInterCityTravel) {
                        // Inter-city travel: Use travel segment data
                        transportType = TransportType.INTER_CITY;
                        TravelSegment segment = getTravelSegmentForDay(cityPlan, day.getDayNumber());
                        if (segment != null) {
                            fromLocation = segment.getFromCity();
                            toLocation = segment.getToCity();
                            logger.info("Detected INTER-CITY travel: {} -> {}", fromLocation, toLocation);
                        } else {
                            // Fallback: parse from title
                            String[] parts = extractLocationsFromTitle(node.getTitle());
                            fromLocation = parts[0];
                            toLocation = parts[1];
                            logger.warn("No travel segment found for day {}, parsed from title: {} -> {}", 
                                       day.getDayNumber(), fromLocation, toLocation);
                        }
                    } else {
                        // Local transport: Use adjacent activity locations
                        transportType = TransportType.LOCAL;
                        fromLocation = i > 0 && day.getNodes().get(i-1).getLocation() != null 
                            ? day.getNodes().get(i-1).getLocation().getName() 
                            : day.getLocation();
                        toLocation = i < day.getNodes().size() - 1 && day.getNodes().get(i+1).getLocation() != null
                            ? day.getNodes().get(i+1).getLocation().getName()
                            : day.getLocation();
                    }
                    
                    contexts.add(new TransportContext(
                        node.getId(),
                        day.getDayNumber(),
                        day.getLocation(),
                        node.getTiming(),
                        fromLocation,
                        toLocation,
                        transportType
                    ));
                }
            }
        }
        
        return contexts;
    }
    
    /**
     * NEW: Populate metadata for transport nodes (replaces string parsing)
     * This should be called BEFORE sending to LLM
     */
    private void populateTransportMetadata(NormalizedItinerary skeleton) {
        logger.info("Populating transport metadata for all transport nodes");
        
        for (NormalizedDay day : skeleton.getDays()) {
            if (day.getNodes() == null) continue;
            
            for (NormalizedNode node : day.getNodes()) {
                if (!"transport".equals(node.getType())) continue;
                
                try {
                    // Mark as enriching
                    node.setProcessingState(ProcessingState.ENRICHING);
                    node.addProcessedBy("TransportAgent");
                    
                    // Extract locations from title
                    String[] locations = metadataService.extractLocationsFromTitle(node.getTitle());
                    String fromLocation = locations[0];
                    String toLocation = locations[1];
                    
                    // Create and populate metadata
                    TransportMetadata metadata = metadataService.populateFromTitle(
                        node.getTitle(), fromLocation, toLocation
                    );
                    node.setMetadata(metadata);
                    
                    // Log metadata
                    logger.info("Populated metadata for {}: {} -> {} (type: {}, island: {}/{})",
                               node.getId(), fromLocation, toLocation,
                               metadata.getTransportType(),
                               metadata.getFromIsIsland(),
                               metadata.getToIsIsland());
                    
                    // Validate geography constraints
                    if (metadata.getToIsIsland() != null && metadata.getToIsIsland()) {
                        logger.info("Destination {} is an island - only flight/ferry allowed", toLocation);
                    }
                    
                } catch (Exception e) {
                    logger.error("Failed to populate metadata for node {}: {}", node.getId(), e.getMessage());
                    node.setLastError("Metadata population failed: " + e.getMessage());
                    node.addValidationError("Failed to determine geography: " + e.getMessage());
                }
            }
        }
    }
    
    /**
     * NEW: Validate transport node after LLM enrichment
     */
    private void validateTransportNode(NormalizedNode node) throws ValidationException {
        if (node.getMetadata() == null || !(node.getMetadata() instanceof TransportMetadata)) {
            throw new ValidationException("Transport node missing metadata: " + node.getId());
        }
        
        TransportMetadata metadata = (TransportMetadata) node.getMetadata();
        
        // Extract mode from node details
        if (node.getDetails() != null && node.getDetails().getCategory() != null) {
            String modeStr = node.getDetails().getCategory().toUpperCase();
            try {
                TransportMode mode = TransportMode.valueOf(modeStr);
                metadata.setSelectedMode(mode);
                
                // Validate mode against geography
                metadataService.validateTransportMode(node);
                
                logger.info("Validated transport mode {} for {}", mode, node.getId());
            } catch (IllegalArgumentException e) {
                logger.warn("Unknown transport mode: {}", modeStr);
            } catch (GeographyException e) {
                logger.error("Geography validation failed for {}: {}", node.getId(), e.getMessage());
                node.addValidationError(e.getUserMessage());
                throw e;
            }
        }
        
        // Mark as validated
        node.setProcessingState(ProcessingState.VALIDATED);
    }
    
    /**
     * Extract from/to locations from transport node title.
     * Handles formats: "Arrival: A to B", "Departure: A to B", "Travel: A to B"
     * @deprecated Use TransportMetadataService.extractLocationsFromTitle instead
     */
    @Deprecated
    private String[] extractLocationsFromTitle(String title) {
        if (title == null || !title.contains(" to ")) {
            return new String[]{"Unknown", "Unknown"};
        }
        
        // Remove prefixes
        String cleaned = title.replaceFirst("(?i)(arrival|departure|travel):\\s*", "").trim();
        
        // Split by " to "
        String[] parts = cleaned.split("\\s+to\\s+", 2);
        if (parts.length == 2) {
            return new String[]{parts[0].trim(), parts[1].trim()};
        }
        
        return new String[]{"Unknown", "Unknown"};
    }
    
    /**
     * Transport type enum for better classification.
     */
    private enum TransportType {
        ARRIVAL_FLIGHT,      // Day 1 arrival from origin
        DEPARTURE_FLIGHT,    // Last day departure to origin
        INTER_CITY,          // Travel between cities in itinerary
        LOCAL                // Local transport within a city
    }
    
    /**
     * Get travel segment for a specific day from city allocation plan.
     */
    private TravelSegment getTravelSegmentForDay(CityAllocationPlan cityPlan, int dayNumber) {
        if (cityPlan == null || cityPlan.getTravelSegments() == null) {
            return null;
        }
        
        for (TravelSegment segment : cityPlan.getTravelSegments()) {
            if (segment.getDayNumber() == dayNumber) {
                return segment;
            }
        }
        
        return null;
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
            // IMPROVED: Validate response against schema before parsing
            LLMSchemaValidator.ValidationResult validationResult = schemaValidator.validateWithLogging(
                response, schema, "TransportAgent");
            
            if (!validationResult.isValid()) {
                String errorMsg = schemaValidator.getUserFriendlyError(validationResult);
                logger.error("Schema validation failed for TransportAgent: {}", errorMsg);
                
                // Return empty list on validation failure (graceful degradation)
                return new ArrayList<>();
            }
            
            // Use validated data
            com.fasterxml.jackson.databind.JsonNode root = validationResult.getData();
            logger.info("Schema validation passed for TransportAgent");
            
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
     * Apply transport data to skeleton without saving (for parallel execution).
     */
    private void applyTransportDataToSkeleton(NormalizedItinerary skeleton,
                                              List<PopulatedTransport> populatedTransport) {
        // Filter out any null nodeIds and handle duplicates gracefully
        Map<String, PopulatedTransport> transportMap = populatedTransport.stream()
            .filter(t -> t.getNodeId() != null)
            .collect(Collectors.toMap(
                PopulatedTransport::getNodeId, 
                t -> t,
                (existing, replacement) -> {
                    logger.warn("Duplicate transport nodeId found: {}, keeping first occurrence", existing.getNodeId());
                    return existing;
                }
            ));
        
        for (NormalizedDay day : skeleton.getDays()) {
            if (day.getNodes() == null) continue;
            
            for (NormalizedNode node : day.getNodes()) {
                if ("transport".equals(node.getType())) {
                    PopulatedTransport populated = transportMap.get(node.getId());
                    if (populated != null) {
                        // Validate that LLM didn't reverse the direction
                        String originalTitle = node.getTitle();
                        String llmTitle = populated.getTitle();
                        
                        if (!validateTransportDirection(originalTitle, llmTitle)) {
                            logger.error("LLM REVERSED DIRECTION! Original: '{}', LLM: '{}'", 
                                        originalTitle, llmTitle);
                            logger.error("Keeping original title to prevent confusion");
                        } else {
                            node.setTitle(populated.getTitle());
                        }
                        
                        if (node.getDetails() == null) {
                            node.setDetails(new NodeDetails());
                        }
                        node.getDetails().setDescription(populated.getDescription());
                        node.getDetails().setCategory(populated.getMode());
                        
                        validateTransportMode(node, populated.getMode(), originalTitle);
                        
                        try {
                            validateTransportNode(node);
                            logger.info("Transport node {} passed validation", node.getId());
                        } catch (ValidationException e) {
                            logger.error("Transport node {} failed validation: {}", node.getId(), e.getMessage());
                            node.setProcessingState(ProcessingState.FAILED);
                            node.setLastError(e.getMessage());
                        }
                        
                        if (node.getTiming() != null) {
                            Integer aiDuration = populated.getDurationMinutes();
                            Integer validatedDuration = validateAndCorrectDuration(
                                node, day, skeleton, populated.getMode(), aiDuration);
                            
                            if (validatedDuration != null) {
                                node.getTiming().setDurationMin(validatedDuration);
                                
                                if (aiDuration != null && Math.abs(aiDuration - validatedDuration) > 15) {
                                    logger.info("Corrected transport duration for {}: AI={}min, Actual={}min", 
                                               node.getTitle(), aiDuration, validatedDuration);
                                }
                            } else if (aiDuration != null) {
                                node.getTiming().setDurationMin(aiDuration);
                            }
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Update itinerary with populated transport data.
     */
    private void updateItineraryWithTransport(String itineraryId, NormalizedItinerary skeleton,
                                              List<PopulatedTransport> populatedTransport) {
        
        // Apply transport data to skeleton
        applyTransportDataToSkeleton(skeleton, populatedTransport);
        
        // Validate before save
        ItineraryValidator.ValidationResult validationResult = itineraryValidator.validate(skeleton);
        if (!validationResult.isValid()) {
            logger.error("Validation failed for itinerary {}: {}", itineraryId, validationResult.getErrors());
            throw new ValidationException("Itinerary validation failed", String.valueOf(validationResult.getErrors()));
        }
        if (!validationResult.getWarnings().isEmpty()) {
            logger.warn("Validation warnings for itinerary {}: {}", itineraryId, validationResult.getWarnings());
        }
        
        try {
            skeleton.setUpdatedAt(System.currentTimeMillis());
            // NEW: Use optimistic locking to prevent concurrent modifications
            itineraryJsonService.updateItineraryWithLock(skeleton);
            logger.info("Saved itinerary with populated transport (with lock)");
        } catch (com.tripplanner.exception.ConcurrentModificationException e) {
            logger.error("Concurrent modification detected: {}", e.getMessage());
            // Reload and retry
            logger.info("Reloading itinerary and retrying save...");
            Optional<NormalizedItinerary> reloaded = itineraryJsonService.getItinerary(skeleton.getItineraryId());
            if (reloaded.isPresent()) {
                // Merge changes and retry
                logger.warn("Itinerary was modified concurrently, changes may be lost");
            }
            throw e;
        } catch (Exception e) {
            logger.error("Failed to save itinerary with transport: {}", e.getMessage());
            throw new RuntimeException("Failed to save transport data", e);
        }
    }
    
    /**
     * Validate that LLM didn't reverse the direction.
     * Returns true if direction is correct, false if reversed.
     */
    private boolean validateTransportDirection(String originalTitle, String llmTitle) {
        if (originalTitle == null || llmTitle == null) {
            return true; // Can't validate, assume OK
        }
        
        // Extract locations from both titles
        String[] originalParts = extractLocationsFromTitle(originalTitle);
        String[] llmParts = extractLocationsFromTitle(llmTitle);
        
        String originalFrom = originalParts[0].toLowerCase().trim();
        String originalTo = originalParts[1].toLowerCase().trim();
        String llmFrom = llmParts[0].toLowerCase().trim();
        String llmTo = llmParts[1].toLowerCase().trim();
        
        // Check if LLM reversed the direction
        boolean isReversed = originalFrom.contains(llmTo) || llmTo.contains(originalFrom) ||
                            originalTo.contains(llmFrom) || llmFrom.contains(originalTo);
        
        // Check if LLM kept the correct direction
        boolean isCorrect = originalFrom.contains(llmFrom) || llmFrom.contains(originalFrom) ||
                           originalTo.contains(llmTo) || llmTo.contains(originalTo);
        
        return isCorrect && !isReversed;
    }
    
    /**
     * Validate that transport mode is feasible for the geography.
     * Logs warnings if mode is impossible (e.g., train to an island).
     */
    private void validateTransportMode(NormalizedNode node, String mode, String title) {
        if (title == null || mode == null) {
            return;
        }
        
        String titleLower = title.toLowerCase();
        String[] knownIslands = {"langkawi", "penang", "phuket", "bali", "maldives", "hawaii", 
                                 "santorini", "mykonos", "ibiza", "malta", "corsica", "sardinia"};
        
        // Check if destination is an island
        boolean isIslandDestination = false;
        for (String island : knownIslands) {
            if (titleLower.contains(island)) {
                isIslandDestination = true;
                break;
            }
        }
        
        // Validate mode for island destinations
        if (isIslandDestination) {
            if ("train".equals(mode) || "bus".equals(mode)) {
                logger.error("INVALID TRANSPORT MODE: {} to island destination in '{}'", mode, title);
                logger.error("Islands require flight or ferry! LLM made a geography error.");
                // Override mode to flight (most common for islands)
                node.getDetails().setCategory("flight");
            }
        }
    }
    
    private String buildTransportSystemPrompt() {
        return """
            You are a transportation and logistics expert for travel planning with deep knowledge of global transport infrastructure.
            
            Your task: Suggest SPECIFIC, FEASIBLE transport modes and routes for getting around.
            
            CRITICAL RULES FOR DIRECTION:
            1. ALWAYS describe transport in the EXACT direction specified in the user prompt
            2. If prompt says "FROM Kuala Lumpur TO Penang", describe KL → Penang (NOT Penang → KL)
            3. For ARRIVAL flights: Describe origin → destination (e.g., "Bengaluru to Kuala Lumpur")
            4. For DEPARTURE flights: Describe current location → home (e.g., "Penang to Bengaluru")
            5. NEVER reverse the direction - this causes major confusion for travelers
            
            CRITICAL RULES FOR GEOGRAPHY:
            1. ALWAYS verify if destination is an island or mainland:
               - Islands: MUST use flight OR ferry (NO train/bus to islands!)
               - Mainland: Can use train, bus, car, or flight
               - International borders: Require flight (unless land border exists)
            
            2. ISLAND DESTINATIONS - Mandatory Flight/Ferry:
               - Langkawi, Malaysia: Flight from KL (1h) OR ferry from Kuala Perlis/Kuala Kedah (2-3h)
               - Penang, Malaysia: Flight from KL (1h) OR ferry from Butterworth (20min) OR bridge by car
               - Phuket, Thailand: Flight OR bus+ferry combination
               - Bali, Indonesia: Flight only (no ferry from Java for tourists)
               - Maldives: Seaplane or speedboat between islands
               - Hawaii: Inter-island flights only
               - Singapore: Connected to Malaysia by bridge (bus/car OK)
            
            3. MAINLAND TRAVEL:
               - Short distance (< 100km): Bus, taxi, car rental
               - Medium distance (100-300km): Train, bus, car rental
               - Long distance (> 300km): Flight, overnight train, long-distance bus
            
            4. VALIDATION CHECKLIST:
               ✓ Is the destination an island? → Use flight or ferry
               ✓ Is the direction correct? → Match the FROM → TO in prompt
               ✓ Is the mode feasible? → No trains to islands, no ferries across deserts
               ✓ Is the duration realistic? → Include check-in/boarding time
            
            Transport Modes:
            - walk: For short distances (< 1km)
            - taxi: Door-to-door convenience
            - rideshare: Uber/Lyft/Grab equivalent
            - bus: Public buses, coaches
            - metro: Subway, underground
            - train: Regional/intercity trains
            - tram: City trams, streetcars
            - ferry: Water transport (islands, river crossings)
            - flight: Domestic/international flights
            - car_rental: Self-drive options
            
            Duration Guidelines (MUST include overhead time):
            - Flight: Actual flight time + 2-3 hours (check-in, security, boarding, baggage claim)
            - Ferry: Actual sailing time + 30 min (boarding, disembarkation)
            - Train: Actual travel time + 15 min (boarding, finding seat)
            - Bus: Actual travel time + 15 min (boarding, loading)
            - Taxi/Car: Direct travel time (no overhead)
            
            Include practical details:
            - Specific airport/station/terminal names
            - Approximate duration (realistic, including all overhead)
            - Booking requirements (advance booking needed?)
            - Cost-saving tips (book early, use local apps)
            - Alternative options if available
            - Check-in time requirements (flights: 2h domestic, 3h international)
            
            CRITICAL VALIDATION:
            1. Verify geography: Is destination an island? → MUST use flight/ferry
            2. Verify direction: Does description match FROM → TO in prompt?
            3. Verify feasibility: Is this transport mode physically possible?
            4. Verify duration: Does it include check-in/boarding overhead?
            
            COMMON MISTAKES TO AVOID:
            ❌ Suggesting train to an island (impossible!)
            ❌ Reversing the direction (causes major confusion)
            ❌ Forgetting check-in time for flights (unrealistic)
            ❌ Using generic descriptions instead of specific details
            
            Be practical, accurate, and consider the traveler's perspective.
            """;
    }
    
    private String buildTransportUserPrompt(NormalizedItinerary skeleton, 
                                            List<TransportContext> contexts) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("Destination: ").append(skeleton.getDays().get(0).getLocation()).append("\n");
        prompt.append("Total Days: ").append(skeleton.getDays().size()).append("\n");
        
        // CRITICAL: Include user's custom instructions/constraints
        // Pay special attention to accessibility and mobility requirements
        if (skeleton.getConstraints() != null && !skeleton.getConstraints().isEmpty()) {
            prompt.append("\n=== CRITICAL USER REQUIREMENTS (MUST FOLLOW) ===\n");
            for (String constraint : skeleton.getConstraints()) {
                prompt.append("- ").append(constraint).append("\n");
            }
            prompt.append("All transport recommendations MUST comply with these requirements.\n");
            prompt.append("Pay special attention to accessibility needs (wheelchair access, mobility assistance).\n");
            prompt.append("Consider party size for transport capacity.\n");
            prompt.append("If a requirement cannot be met, skip that transport segment.\n\n");
        }
        
        prompt.append("\nTransport segments to populate:\n");
        for (TransportContext ctx : contexts) {
            prompt.append(String.format("- Day %d, Node ID: %s\n", ctx.dayNumber, ctx.nodeId));
            
            // CRITICAL: Be explicit about transport type and direction
            switch (ctx.transportType) {
                case ARRIVAL_FLIGHT:
                    prompt.append("  Type: ARRIVAL FLIGHT (Day 1)\n");
                    prompt.append(String.format("  Direction: FROM %s (origin) TO %s (destination)\n", 
                                              ctx.fromLocation, ctx.toLocation));
                    prompt.append(String.format("  CRITICAL: This is the traveler's ARRIVAL at the destination.\n"));
                    prompt.append(String.format("  Describe the flight FROM %s TO %s (not the reverse!).\n", 
                                              ctx.fromLocation, ctx.toLocation));
                    prompt.append("  Include: Flight duration, airport names, arrival time considerations.\n");
                    break;
                    
                case DEPARTURE_FLIGHT:
                    prompt.append("  Type: DEPARTURE FLIGHT (Last Day)\n");
                    prompt.append(String.format("  Direction: FROM %s (current location) TO %s (returning home)\n", 
                                              ctx.fromLocation, ctx.toLocation));
                    prompt.append(String.format("  CRITICAL: This is the traveler's DEPARTURE from the destination.\n"));
                    prompt.append(String.format("  Describe the flight FROM %s TO %s (not the reverse!).\n", 
                                              ctx.fromLocation, ctx.toLocation));
                    prompt.append("  Include: Flight duration, airport names, check-in time requirements.\n");
                    break;
                    
                case INTER_CITY:
                    prompt.append("  Type: INTER-CITY TRAVEL\n");
                    prompt.append(String.format("  Direction: FROM %s TO %s\n", ctx.fromLocation, ctx.toLocation));
                    prompt.append(String.format("  CRITICAL: Describe travel FROM %s TO %s (not the reverse!).\n", 
                                              ctx.fromLocation, ctx.toLocation));
                    prompt.append("  GEOGRAPHY CHECK: Verify if destination is an island requiring flight/ferry.\n");
                    prompt.append(String.format("  - If %s is an island: MUST use flight or ferry\n", ctx.toLocation));
                    prompt.append("  - If mainland: Can use train, bus, or car\n");
                    break;
                    
                case LOCAL:
                    prompt.append("  Type: LOCAL TRANSPORT\n");
                    prompt.append(String.format("  From: %s → To: %s\n", ctx.fromLocation, ctx.toLocation));
                    prompt.append("  Use: Taxi, rideshare, metro, bus, or walk (depending on distance)\n");
                    break;
            }
            
            if (ctx.timing != null) {
                prompt.append(String.format("  Scheduled Time: %s\n", ctx.timing.getStartTime()));
            }
            prompt.append("\n");
        }
        
        prompt.append("\n=== CRITICAL INSTRUCTIONS ===\n");
        prompt.append("1. DIRECTION: Always describe transport in the EXACT direction specified (FROM → TO)\n");
        prompt.append("2. GEOGRAPHY: For islands, MUST specify flight or ferry (no train/bus to islands!)\n");
        prompt.append("3. ARRIVAL/DEPARTURE: Clearly indicate if this is arrival at or departure from destination\n");
        prompt.append("4. DURATION: Include realistic durations with check-in/boarding time\n");
        prompt.append("5. DETAILS: Provide specific airport/station names, booking tips, and practical advice\n");
        prompt.append("\nREMEMBER: All selections must respect the user requirements listed above.\n");
        
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
    
    // ========== TRANSPORT AGENT - TOOL INTEGRATION METHODS ==========
    
    /**
     * Generate node ID using the Generate Node ID tool.
     */
    private String generateNodeIdViaTool(String itineraryId, Integer dayNumber, String nodeType) {
        if (!transportToolsEnabled) {
            return nodeIdGenerator.generateNodeId(nodeType, dayNumber);
        }
        
        NodeIdRequest request = new NodeIdRequest(itineraryId, dayNumber, nodeType);
        
        try {
            NodeIdResponse response = restTemplate.postForObject(
                "http://localhost:8080/api/v1/tools/generate-node-id",
                request,
                NodeIdResponse.class
            );
            
            if (response != null && response.isSuccess()) {
                return response.getNodeId();
            } else if (fallbackOnError) {
                return nodeIdGenerator.generateNodeId(nodeType, dayNumber);
            }
            return nodeIdGenerator.generateNodeId(nodeType, dayNumber);
        } catch (Exception e) {
            logger.error("Generate Node ID tool error: {}", e.getMessage());
            return fallbackOnError ? nodeIdGenerator.generateNodeId(nodeType, dayNumber) : null;
        }
    }
    
    /**
     * Check user constraints (budget focus for transport).
     */
    private ConstraintCheckResult checkConstraintsViaTool(String itineraryId) {
        if (!transportToolsEnabled) {
            return new ConstraintCheckResult();
        }
        
        ConstraintCheckRequest request = new ConstraintCheckRequest(itineraryId);
        request.setCheckBudget(true);  // Critical for transport
        request.setCheckPartySize(true);
        
        try {
            ConstraintCheckResult result = restTemplate.postForObject(
                "http://localhost:8080/api/v1/tools/check-user-constraints",
                request,
                ConstraintCheckResult.class
            );
            
            if (result != null && !result.isValid()) {
                logger.warn("Transport constraint violations:");
                result.getViolations().forEach(v -> 
                    logger.warn("  - {}: {}", v.getType(), v.getMessage()));
            }
            return result != null ? result : new ConstraintCheckResult();
        } catch (Exception e) {
            logger.error("Check Constraints tool error: {}", e.getMessage());
            return fallbackOnError ? new ConstraintCheckResult() : new ConstraintCheckResult();
        }
    }
    
    /**
     * Calculate distance using the Calculate Distance tool.
     */
    private DistanceCalculationResult calculateDistanceViaTool(
            String origin, String destination, String mode) {
        if (!transportToolsEnabled) {
            // Use existing service
            return null; // Will trigger fallback
        }
        
        DistanceCalculationRequest request = new DistanceCalculationRequest();
        request.setOrigin(origin);
        request.setDestination(destination);
        request.setMode(mode);
        
        try {
            DistanceCalculationResult result = restTemplate.postForObject(
                "http://localhost:8080/api/v1/tools/calculate-distance",
                request,
                DistanceCalculationResult.class
            );
            
            if (result != null && result.isSuccess()) {
                logger.info("Distance tool: {} km, {} min", 
                    result.getDistanceKm(), result.getDurationMinutes());
                return result;
            }
            return null;
        } catch (Exception e) {
            logger.error("Calculate Distance tool error: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Validate LLM schema using the Validate Schema tool.
     */
    private boolean validateSchemaViaTool(String jsonOutput, String jsonSchema) {
        if (!transportToolsEnabled) {
            return schemaValidator.validateWithLogging(jsonOutput, jsonSchema, "TransportAgent").isValid();
        }
        
        SchemaValidationRequest request = new SchemaValidationRequest();
        request.setJsonOutput(jsonOutput);
        request.setJsonSchema(jsonSchema);
        request.setCleanBeforeValidation(true);
        
        try {
            SchemaValidationResult result = restTemplate.postForObject(
                "http://localhost:8080/api/v1/tools/validate-schema",
                request,
                SchemaValidationResult.class
            );
            
            if (result != null && !result.isValid()) {
                logger.error("Schema validation failed:");
                result.getErrors().forEach(error -> logger.error("  - {}", error));
            }
            return result != null && result.isValid();
        } catch (Exception e) {
            logger.error("Validate Schema tool error: {}", e.getMessage());
            return fallbackOnError ? 
                schemaValidator.validateWithLogging(jsonOutput, jsonSchema, "TransportAgent").isValid() : false;
        }
    }
    
    // ========== END TRANSPORT AGENT TOOL INTEGRATION ==========
    
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
        TransportType transportType;
        
        public TransportContext(String nodeId, int dayNumber, String dayLocation, 
                               NodeTiming timing, String fromLocation, String toLocation,
                               TransportType transportType) {
            this.nodeId = nodeId;
            this.dayNumber = dayNumber;
            this.dayLocation = dayLocation;
            this.timing = timing;
            this.fromLocation = fromLocation;
            this.toLocation = toLocation;
            this.transportType = transportType;
        }
        
        public boolean isInterCityTravel() {
            return transportType == TransportType.INTER_CITY || 
                   transportType == TransportType.ARRIVAL_FLIGHT || 
                   transportType == TransportType.DEPARTURE_FLIGHT;
        }
    }
    
    /**
     * Validate and correct transport duration using real distance calculation.
     * IMPROVED: Uses Google Maps API for accurate durations.
     */
    private Integer validateAndCorrectDuration(NormalizedNode transportNode, NormalizedDay day,
                                               NormalizedItinerary itinerary, String mode, Integer aiDuration) {
        try {
            // Find previous and next nodes to get from/to locations
            NodeLocation fromLocation = null;
            NodeLocation toLocation = null;
            
            List<NormalizedNode> dayNodes = day.getNodes();
            int transportIndex = dayNodes.indexOf(transportNode);
            
            if (transportIndex > 0) {
                NormalizedNode prevNode = dayNodes.get(transportIndex - 1);
                fromLocation = prevNode.getLocation();
            }
            
            if (transportIndex < dayNodes.size() - 1) {
                NormalizedNode nextNode = dayNodes.get(transportIndex + 1);
                toLocation = nextNode.getLocation();
            }
            
            // If we have both locations with coordinates, calculate real distance
            if (fromLocation != null && toLocation != null &&
                fromLocation.getCoordinates() != null && toLocation.getCoordinates() != null) {
                
                // Use cached distance calculation with itineraryId
                GoogleMapsDistanceService.DistanceResult result = 
                    distanceService.calculateDistance(itinerary.getItineraryId(), fromLocation, toLocation, mode);
                
                // Validate result is reasonable
                if (distanceService.isReasonable(result)) {
                    logger.info("Validated transport duration: {}km, {}min ({})", 
                               String.format("%.2f", result.getDistanceKm()), 
                               result.getDurationMinutes(),
                               result.isFromAPI() ? "Google Maps API" : "Haversine");
                    
                    return result.getDurationMinutes();
                } else {
                    logger.warn("Distance calculation returned unreasonable result, using AI duration");
                    return aiDuration;
                }
            } else {
                logger.debug("Cannot validate duration: missing coordinates for from/to locations");
                return aiDuration;
            }
            
        } catch (Exception e) {
            logger.warn("Failed to validate transport duration: {}", e.getMessage());
            return aiDuration; // Fallback to AI duration
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

