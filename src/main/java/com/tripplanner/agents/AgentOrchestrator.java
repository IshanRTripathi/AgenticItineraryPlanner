package com.tripplanner.agents;

import com.tripplanner.dto.*;
import com.tripplanner.service.ChangeEngine;
import com.tripplanner.service.ItineraryJsonService;
import com.tripplanner.service.UserDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Orchestrates the execution of multiple agents to generate complete itineraries.
 */
@Service
public class AgentOrchestrator {
    
    private static final Logger logger = LoggerFactory.getLogger(AgentOrchestrator.class);
    
    private final PlannerAgent plannerAgent;
    private final EnrichmentAgent enrichmentAgent;
    private final ItineraryJsonService itineraryJsonService;
    private final ChangeEngine changeEngine;
    private final UserDataService userDataService;
    
    public AgentOrchestrator(PlannerAgent plannerAgent, EnrichmentAgent enrichmentAgent,
                           ItineraryJsonService itineraryJsonService,
                           ChangeEngine changeEngine, UserDataService userDataService) {
        this.plannerAgent = plannerAgent;
        this.enrichmentAgent = enrichmentAgent;
        this.itineraryJsonService = itineraryJsonService;
        this.changeEngine = changeEngine;
        this.userDataService = userDataService;
    }
    
    // Old generateItinerary method removed - use generateNormalizedItinerary instead
    
    // ===== NEW MVP CONTRACT METHODS =====
    
    /**
     * Generate a complete itinerary using the new agent sequence: PlannerAgent → EnrichmentAgent.
     * This method works with normalized JSON and uses the ChangeEngine.
     */
    @Async
    public CompletableFuture<NormalizedItinerary> generateNormalizedItinerary(String itineraryId, CreateItineraryReq request, String userId) {
        logger.info("=== AGENT ORCHESTRATOR: NORMALIZED ITINERARY GENERATION ===");
        logger.info("Itinerary ID: {}", itineraryId);
        logger.info("Destination: {}", request.getDestination());
        
        try {
            // Step 1: Create initial normalized itinerary structure
            logger.info("Step 1: Creating initial itinerary structure");
            NormalizedItinerary initialItinerary = createInitialNormalizedItinerary(itineraryId, request, userId);
            
            // Save initial itinerary to user-specific storage
            userDataService.saveUserItinerary(userId, initialItinerary);
            
            // Add a small delay to allow frontend to establish SSE connection
            logger.info("Waiting 3 seconds for frontend to establish SSE connection...");
            Thread.sleep(3000);
            
            // Step 2: Run PlannerAgent to generate the main itinerary
            logger.info("Step 2: Running PlannerAgent");
            BaseAgent.AgentRequest<ItineraryDto> plannerRequest = new BaseAgent.AgentRequest<>(request, ItineraryDto.class);
            ItineraryDto plannerResult = plannerAgent.execute(itineraryId, plannerRequest);
            
            // PlannerAgent already updates the database with NormalizedItinerary, no need to convert
            logger.info("PlannerAgent completed, database already updated");
            
            // Step 3: Run EnrichmentAgent to add validation and pacing
            logger.info("Step 3: Running EnrichmentAgent");
            BaseAgent.AgentRequest<ChangeEngine.ApplyResult> enrichmentRequest = new BaseAgent.AgentRequest<>(null, ChangeEngine.ApplyResult.class);
            enrichmentAgent.execute(itineraryId, enrichmentRequest);
            
            // Get the final enriched itinerary
            var finalItinerary = itineraryJsonService.getItinerary(itineraryId);
            if (finalItinerary.isEmpty()) {
                throw new RuntimeException("Failed to retrieve final itinerary");
            }
            
            logger.info("=== NORMALIZED ITINERARY GENERATION COMPLETE ===");
            logger.info("Final itinerary has {} days", finalItinerary.get().getDays() != null ? finalItinerary.get().getDays().size() : 0);
            
            return CompletableFuture.completedFuture(finalItinerary.get());
            
        } catch (Exception e) {
            logger.error("Failed to generate normalized itinerary for ID: {}", itineraryId, e);
            throw new RuntimeException("Normalized itinerary generation failed", e);
        }
    }
    
    /**
     * Process a user request to modify an existing itinerary using PlannerAgent.
     */
    @Async
    public CompletableFuture<ChangeSet> processUserRequest(String itineraryId, String userRequest) {
        logger.info("=== AGENT ORCHESTRATOR: PROCESSING USER REQUEST ===");
        logger.info("Itinerary ID: {}", itineraryId);
        logger.info("User Request: {}", userRequest);
        
        try {
            // Use PlannerAgent to generate ChangeSet from user request
            ChangeSet changeSet = plannerAgent.generateChangeSet(itineraryId, userRequest);
            
            logger.info("=== USER REQUEST PROCESSED ===");
            logger.info("Generated ChangeSet with {} operations", changeSet.getOps() != null ? changeSet.getOps().size() : 0);
            
            return CompletableFuture.completedFuture(changeSet);
            
        } catch (Exception e) {
            logger.error("Failed to process user request for itinerary: {}", itineraryId, e);
            throw new RuntimeException("User request processing failed", e);
        }
    }
    
    /**
     * Apply a ChangeSet and run EnrichmentAgent for validation.
     */
    @Async
    public CompletableFuture<ChangeEngine.ApplyResult> applyChangeSetWithEnrichment(String itineraryId, ChangeSet changeSet) {
        logger.info("=== AGENT ORCHESTRATOR: APPLYING CHANGESET WITH ENRICHMENT ===");
        logger.info("Itinerary ID: {}", itineraryId);
        logger.info("ChangeSet: {}", changeSet);
        
        try {
            // Step 1: Apply the ChangeSet using PlannerAgent
            logger.info("Step 1: Applying ChangeSet");
            ChangeEngine.ApplyResult applyResult = plannerAgent.applyChangeSet(itineraryId, changeSet);
            
            // Step 2: Run EnrichmentAgent to add validation and pacing
            logger.info("Step 2: Running EnrichmentAgent for validation");
            BaseAgent.AgentRequest<ChangeEngine.ApplyResult> enrichmentRequest = new BaseAgent.AgentRequest<>(null, ChangeEngine.ApplyResult.class);
            enrichmentAgent.execute(itineraryId, enrichmentRequest);
            
            logger.info("=== CHANGESET APPLIED WITH ENRICHMENT ===");
            logger.info("Final version: {}", applyResult.getToVersion());
            
            return CompletableFuture.completedFuture(applyResult);
            
        } catch (Exception e) {
            logger.error("Failed to apply ChangeSet with enrichment for itinerary: {}", itineraryId, e);
            throw new RuntimeException("ChangeSet application with enrichment failed", e);
        }
    }
    
    /**
     * Create initial normalized itinerary structure.
     */
    private NormalizedItinerary createInitialNormalizedItinerary(String itineraryId, CreateItineraryReq request, String userId) {
        NormalizedItinerary itinerary = new NormalizedItinerary();
        itinerary.setItineraryId(itineraryId);
        itinerary.setVersion(1);
        itinerary.setUserId(userId);
        itinerary.setCreatedAt(System.currentTimeMillis());
        itinerary.setUpdatedAt(System.currentTimeMillis());
        itinerary.setSummary("Your personalized itinerary for " + request.getDestination());
        itinerary.setCurrency("EUR");
        itinerary.setThemes(request.getInterests() != null ? request.getInterests() : new ArrayList<>());
        itinerary.setDays(new ArrayList<>());

        // Set explicit trip meta
        itinerary.setDestination(request.getDestination());
        if (request.getStartDate() != null) {
            itinerary.setStartDate(request.getStartDate().toString());
        }
        if (request.getEndDate() != null) {
            itinerary.setEndDate(request.getEndDate().toString());
        }
        
        // Set settings
        ItinerarySettings settings = new ItinerarySettings();
        settings.setAutoApply(false);
        settings.setDefaultScope("trip");
        itinerary.setSettings(settings);
        
        // Set agent status
        Map<String, AgentStatus> agents = new HashMap<>();
        agents.put("planner", new AgentStatus());
        agents.put("enrichment", new AgentStatus());
        itinerary.setAgents(agents);
        
        return itinerary;
    }
    
    /**
     * Convert ItineraryDto to NormalizedItinerary.
     */
    private NormalizedItinerary convertToNormalizedItinerary(String itineraryId, ItineraryDto dto, CreateItineraryReq request) {
        NormalizedItinerary normalized = new NormalizedItinerary();
        normalized.setItineraryId(itineraryId);
        normalized.setVersion(1);
        normalized.setSummary(dto.getSummary());
        normalized.setCurrency("EUR");
        normalized.setThemes(request.getInterests() != null ? request.getInterests() : new ArrayList<>());
        
        // Convert days
        if (dto.getDays() != null) {
            List<NormalizedDay> normalizedDays = new ArrayList<>();
            for (ItineraryDayDto dayDto : dto.getDays()) {
                NormalizedDay normalizedDay = convertDayToNormalized(dayDto);
                normalizedDays.add(normalizedDay);
            }
            normalized.setDays(normalizedDays);
        }
        
        // Set settings
        ItinerarySettings settings = new ItinerarySettings();
        settings.setAutoApply(false);
        settings.setDefaultScope("trip");
        normalized.setSettings(settings);
        
        // Set agent status
        Map<String, AgentStatus> agents = new HashMap<>();
        agents.put("planner", new AgentStatus());
        agents.put("enrichment", new AgentStatus());
        normalized.setAgents(agents);
        
        return normalized;
    }
    
    /**
     * Convert ItineraryDayDto to NormalizedDay.
     */
    private NormalizedDay convertDayToNormalized(ItineraryDayDto dayDto) {
        NormalizedDay normalizedDay = new NormalizedDay();
        normalizedDay.setDayNumber(dayDto.getDay());
        normalizedDay.setDate(dayDto.getDate().toString());
        normalizedDay.setNodes(new ArrayList<>());
        normalizedDay.setEdges(new ArrayList<>());
        
        // Convert activities to nodes
        if (dayDto.getActivities() != null) {
            for (ActivityDto activity : dayDto.getActivities()) {
                NormalizedNode node = convertActivityToNode(activity);
                normalizedDay.getNodes().add(node);
            }
        }
        
        // Convert meals to nodes
        if (dayDto.getMeals() != null) {
            for (MealDto meal : dayDto.getMeals()) {
                NormalizedNode node = convertMealToNode(meal);
                normalizedDay.getNodes().add(node);
            }
        }
        
        // Convert accommodation to node
        if (dayDto.getAccommodation() != null) {
            NormalizedNode node = convertAccommodationToNode(dayDto.getAccommodation());
            normalizedDay.getNodes().add(node);
        }
        
        // Create edges from transportation
        if (dayDto.getTransportation() != null) {
            for (TransportationDto transport : dayDto.getTransportation()) {
                Edge edge = convertTransportationToEdge(transport);
                normalizedDay.getEdges().add(edge);
            }
        }
        
        return normalizedDay;
    }
    
    /**
     * Convert ActivityDto to NormalizedNode.
     */
    private NormalizedNode convertActivityToNode(ActivityDto activity) {
        NormalizedNode node = new NormalizedNode();
        node.setId("n_" + activity.name().toLowerCase().replaceAll("\\s+", "_"));
        node.setType("activity");
        node.setTitle(activity.name());
        node.setDetails(createNodeDetails(activity.description()));
        node.setLocked(false);
        
        // Set timing
        NodeTiming timing = new NodeTiming();
        timing.setStartTime(java.time.Instant.parse(activity.startTime()));
        timing.setEndTime(java.time.Instant.parse(activity.endTime()));
        timing.setDurationMin(parseDuration(activity.duration()));
        node.setTiming(timing);
        
        // Set location
        if (activity.location() != null) {
            NodeLocation location = new NodeLocation();
            location.setName(activity.location().name());
            location.setAddress(activity.location().address());
            Coordinates coords = new Coordinates();
            coords.setLat(activity.location().lat());
            coords.setLng(activity.location().lng());
            location.setCoordinates(coords);
            node.setLocation(location);
        }
        
        // Set cost
        if (activity.price() != null) {
            NodeCost cost = new NodeCost();
            cost.setAmount(activity.price().amount());
            cost.setCurrency(activity.price().currency());
            node.setCost(cost);
        }
        
        return node;
    }
    
    /**
     * Convert MealDto to NormalizedNode.
     */
    private NormalizedNode convertMealToNode(MealDto meal) {
        NormalizedNode node = new NormalizedNode();
        node.setId("n_" + meal.name().toLowerCase().replaceAll("\\s+", "_"));
        node.setType("meal");
        node.setTitle(meal.name());
        node.setDetails(createNodeDetails(meal.restaurant() + " - " + meal.cuisine()));
        node.setLocked(false);
        
        // Set timing (meals don't have specific times in the DTO)
        NodeTiming timing = new NodeTiming();
        timing.setStartTime(java.time.Instant.parse("2025-10-04T12:00:00Z")); // Default lunch time
        timing.setEndTime(java.time.Instant.parse("2025-10-04T13:00:00Z"));
        timing.setDurationMin(60);
        node.setTiming(timing);
        
        // Set location
        if (meal.location() != null) {
            NodeLocation location = new NodeLocation();
            location.setName(meal.location().name());
            location.setAddress(meal.location().address());
            Coordinates coords = new Coordinates();
            coords.setLat(meal.location().lat());
            coords.setLng(meal.location().lng());
            location.setCoordinates(coords);
            node.setLocation(location);
        }
        
        // Set cost
        if (meal.price() != null) {
            NodeCost cost = new NodeCost();
            cost.setAmount(meal.price().amount());
            cost.setCurrency(meal.price().currency());
            node.setCost(cost);
        }
        
        return node;
    }
    
    /**
     * Convert AccommodationDto to NormalizedNode.
     */
    private NormalizedNode convertAccommodationToNode(AccommodationDto accommodation) {
        NormalizedNode node = new NormalizedNode();
        node.setId("n_" + accommodation.name().toLowerCase().replaceAll("\\s+", "_"));
        node.setType("accommodation");
        node.setTitle(accommodation.name());
        node.setDetails(createNodeDetails(accommodation.type()));
        node.setLocked(false);
        
        // Set timing (accommodation is typically overnight)
        NodeTiming timing = new NodeTiming();
        timing.setStartTime(java.time.Instant.parse("2025-10-04T15:00:00Z")); // Check-in time
        timing.setEndTime(java.time.Instant.parse("2025-10-05T11:00:00Z")); // Check-out time (next day)
        timing.setDurationMin(20 * 60); // 20 hours
        node.setTiming(timing);
        
        // Set location
        if (accommodation.location() != null) {
            NodeLocation location = new NodeLocation();
            location.setName(accommodation.location().name());
            location.setAddress(accommodation.location().address());
            Coordinates coords = new Coordinates();
            coords.setLat(accommodation.location().lat());
            coords.setLng(accommodation.location().lng());
            location.setCoordinates(coords);
            node.setLocation(location);
        }
        
        // Set cost
        if (accommodation.price() != null) {
            NodeCost cost = new NodeCost();
            cost.setAmount(accommodation.price().amount());
            cost.setCurrency(accommodation.price().currency());
            node.setCost(cost);
        }
        
        return node;
    }
    
    /**
     * Convert TransportationDto to Edge.
     */
    private Edge convertTransportationToEdge(TransportationDto transport) {
        Edge edge = new Edge();
        edge.setFrom("n_" + transport.from().name().toLowerCase().replaceAll("\\s+", "_"));
        edge.setTo("n_" + transport.to().name().toLowerCase().replaceAll("\\s+", "_"));
        // Note: Edge doesn't have mode/duration/transit methods in current implementation
        // This is a simplified conversion for now
        
        return edge;
    }
    
    private int parseDuration(String duration) {
        if (duration == null || duration.trim().isEmpty()) {
            return 0;
        }
        
        try {
            return Integer.parseInt(duration.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
    
    /**
     * Create NodeDetails with category (using description as category for now).
     */
    private NodeDetails createNodeDetails(String description) {
        NodeDetails details = new NodeDetails();
        details.setCategory(description);
        return details;
    }
}