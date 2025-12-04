package com.tripplanner.agents;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripplanner.dto.*;
import com.tripplanner.enums.ProcessingState;
import com.tripplanner.exception.ValidationException;
import com.tripplanner.service.*;
import com.tripplanner.service.agents.AgentEventBus;
import com.tripplanner.service.agents.AgentEventPublisher;
import com.tripplanner.service.ai.AiClient;
import com.tripplanner.service.ai.ResilientAiClient;
import com.tripplanner.service.ai.RetryStrategy;
import com.tripplanner.service.llm.LLMSchemaValidator;
import com.tripplanner.service.utilities.NodeIdGenerator;
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
    private final LLMSchemaValidator schemaValidator;
    private final NodeIdValidator nodeIdValidator;
    private final ItineraryValidator itineraryValidator;
    private final RestTemplate restTemplate;

    // Configuration
    private static final int DAYS_PER_BATCH = 1; // Generate 1 day at a time for maximum reliability

    // Feature flags for tool integration
    @Value("${features.skeleton-tools.enabled:false}")
    private boolean skeletonToolsEnabled;

    @Value("${features.skeleton-tools.fallback-on-error:true}")
    private boolean fallbackOnError;

    public SkeletonPlannerAgent(AgentEventBus eventBus, AiClient aiClient, ObjectMapper objectMapper,
                                ItineraryJsonService itineraryJsonService, AgentEventPublisher agentEventPublisher,
                                NodeIdGenerator nodeIdGenerator, LLMSchemaValidator schemaValidator,
                                NodeIdValidator nodeIdValidator, ItineraryValidator itineraryValidator) {
        super(eventBus, AgentEvent.AgentKind.PLANNER);
        this.aiClient = aiClient;
        this.objectMapper = objectMapper;
        this.itineraryJsonService = itineraryJsonService;
        this.agentEventPublisher = agentEventPublisher;
        this.nodeIdGenerator = nodeIdGenerator;
        this.schemaValidator = schemaValidator;
        this.nodeIdValidator = nodeIdValidator;
        this.itineraryValidator = itineraryValidator;
        this.restTemplate = new RestTemplate();
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
            // Load the existing itinerary (which was created by ItineraryInitializationService with userId)
            Optional<NormalizedItinerary> existingOpt = itineraryJsonService.getItinerary(itineraryId);
            NormalizedItinerary itinerary;

            if (existingOpt.isPresent()) {
                // Use existing itinerary to preserve userId and other metadata
                itinerary = existingOpt.get();
                logger.info("Loaded existing itinerary with userId: {}", itinerary.getUserId());
            } else {
                // Fallback: create new itinerary (shouldn't happen in normal flow)
                logger.warn("Itinerary {} not found, creating new one (userId will be missing)", itineraryId);
                itinerary = createInitialItinerary(itineraryId, request);
            }

            // Read CityAllocationPlan from agentData
            CityAllocationPlan cityPlan = null;
            if (itinerary.getAgentData() != null && itinerary.getAgentData().containsKey("cityAllocation")) {
                AgentDataSection agentDataSection = itinerary.getAgentData().get("cityAllocation");
                cityPlan = agentDataSection.getAgentData("cityAllocation", CityAllocationPlan.class);
                if (cityPlan != null) {
                    logger.info("Loaded city allocation plan: {} cities, {} travel segments",
                            cityPlan.getAllocations().size(),
                            cityPlan.getTravelSegments() != null ? cityPlan.getTravelSegments().size() : 0);
                }
            }

            if (cityPlan == null) {
                logger.warn("No city allocation plan found, generating without city context");
            }

            // Generate days in small batches
            int totalDays = request.getDurationDays();
            int processedDays = 0;
            List<NormalizedDay> previousDays = new ArrayList<>();

            while (processedDays < totalDays) {
                int remainingDays = totalDays - processedDays;
                int batchSize = Math.min(DAYS_PER_BATCH, remainingDays);

                emitProgress(itineraryId,
                        (int) (20 + (processedDays * 60.0 / totalDays)),
                        String.format("Creating day %d structure", processedDays + 1),
                        "skeleton_generation");

                int dayNumber = processedDays + 1;

                // Find city allocation for this day
                CityAllocation cityForThisDay = getCityForDay(cityPlan, dayNumber);

                // Find travel segment for this day
                TravelSegment travelSegment = getTravelSegmentForDay(cityPlan, dayNumber);

                // IMPROVED: Create infrastructure nodes BEFORE LLM call
                // This allows LLM to see transport/accommodation in context and plan around them
                List<NormalizedNode> preCreatedNodes = new ArrayList<>();

                // Create a temporary itinerary for ID generation
                NormalizedItinerary tempItinerary = new NormalizedItinerary();
                tempItinerary.setDays(new ArrayList<>(previousDays));

                // Add arrival travel for Day 1
                if (dayNumber == 1 && shouldAddArrivalTravel(request, cityForThisDay)) {
                    NormalizedNode arrivalNode = createArrivalTravelNode(dayNumber, request, cityForThisDay);
                    // CRITICAL FIX: Assign ID immediately using NodeIdGenerator
                    arrivalNode.setId(nodeIdGenerator.generateNodeId("transport", dayNumber, tempItinerary));
                    preCreatedNodes.add(arrivalNode);
                    logger.info("Pre-created arrival node for Day 1 with ID: {}", arrivalNode.getId());
                }

                // Add inter-city travel
                if (travelSegment != null) {
                    NormalizedNode travelNode = createTravelNode(dayNumber, travelSegment);
                    // CRITICAL FIX: Assign ID immediately using NodeIdGenerator
                    travelNode.setId(nodeIdGenerator.generateNodeId("transport", dayNumber, tempItinerary));
                    preCreatedNodes.add(travelNode);
                    logger.info("Pre-created travel node for Day {} with ID: {} ({} -> {})",
                            dayNumber, travelNode.getId(), travelSegment.getFromCity(), travelSegment.getToCity());
                }

                // Add departure travel for last day
                if (dayNumber == totalDays && shouldAddDepartureTravel(request, cityForThisDay)) {
                    NormalizedNode departureNode = createDepartureTravelNode(dayNumber, request, cityForThisDay);
                    // CRITICAL FIX: Assign ID immediately using NodeIdGenerator
                    departureNode.setId(nodeIdGenerator.generateNodeId("transport", dayNumber, tempItinerary));
                    preCreatedNodes.add(departureNode);
                    logger.info("Pre-created departure node for last day with ID: {}", departureNode.getId());
                }

                // REMOVED: Accommodation nodes no longer created
                // Users will book accommodation separately via "Book Stay" button in UI

                // Generate skeleton for this day with pre-created infrastructure nodes
                // LLM will see these nodes and plan activities around them
                NormalizedDay day = generateDaySkeleton(request, dayNumber, cityForThisDay,
                        travelSegment, previousDays, preCreatedNodes);

                itinerary.getDays().add(day);
                previousDays.add(day);

                // Validate before save
                ItineraryValidator.ValidationResult validationResult = itineraryValidator.validate(itinerary);
                if (!validationResult.isValid()) {
                    logger.error("Validation failed for itinerary {}: {}", itineraryId, validationResult.getErrors());
                    throw new ValidationException("Itinerary validation failed", String.valueOf(validationResult.getErrors()));
                }
                if (!validationResult.getWarnings().isEmpty()) {
                    logger.warn("Validation warnings for itinerary {}: {}", itineraryId, validationResult.getWarnings());
                }

                // Save immediately for real-time access with optimistic locking and retry
                int maxRetries = 3;
                int retryCount = 0;
                boolean saved = false;

                while (!saved && retryCount < maxRetries) {
                    try {
                        itinerary.setUpdatedAt(System.currentTimeMillis());
                        itineraryJsonService.updateItineraryWithLock(itinerary);

                        if (agentEventPublisher.hasActiveConnections(itineraryId)) {
                            agentEventPublisher.publishDayCompleted(itineraryId,
                                    "exec_" + System.currentTimeMillis(), day);
                        }

                        logger.info("Skeleton day {} created and saved (city: {}, travel: {})",
                                day.getDayNumber(),
                                cityForThisDay != null ? cityForThisDay.getCityName() : "N/A",
                                travelSegment != null ? "Yes" : "No");
                        saved = true;
                    } catch (com.tripplanner.exception.ConcurrentModificationException e) {
                        retryCount++;
                        logger.error("Concurrent modification on day {} (attempt {}/{}): {}",
                                day.getDayNumber(), retryCount, maxRetries, e.getMessage());

                        if (retryCount < maxRetries) {
                            logger.info("Reloading itinerary and retrying save...");
                            Optional<NormalizedItinerary> reloaded = itineraryJsonService.getItinerary(itineraryId);
                            if (reloaded.isPresent()) {
                                itinerary = reloaded.get();
                                // Re-add the day to reloaded itinerary
                                itinerary.getDays().add(day);
                                logger.info("Re-added day {} to reloaded itinerary", day.getDayNumber());
                            } else {
                                logger.error("Failed to reload itinerary for retry");
                                throw e;
                            }
                        } else {
                            logger.error("Max retries ({}) exceeded for day {}, giving up", maxRetries, day.getDayNumber());
                            throw e;
                        }
                    } catch (Exception e) {
                        logger.warn("Failed to save skeleton day {}: {}", day.getDayNumber(), e.getMessage());
                        break; // Don't retry on other exceptions
                    }
                }

                processedDays += batchSize;
            }

            emitProgress(itineraryId, 80, "Skeleton complete", "skeleton_complete");

            // Validate before final save
            ItineraryValidator.ValidationResult validationResult = itineraryValidator.validate(itinerary);
            if (!validationResult.isValid()) {
                logger.error("Validation failed for itinerary {}: {}", itineraryId, validationResult.getErrors());
                throw new ValidationException("Itinerary validation failed", String.valueOf(validationResult.getErrors()));
            }
            if (!validationResult.getWarnings().isEmpty()) {
                logger.warn("Validation warnings for itinerary {}: {}", itineraryId, validationResult.getWarnings());
            }

            // Final save with optimistic locking and retry
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
                    logger.error("Concurrent modification on final save (attempt {}/{}): {}",
                            retryCount, maxRetries, e.getMessage());

                    if (retryCount < maxRetries) {
                        logger.info("Reloading itinerary and retrying final save...");
                        Optional<NormalizedItinerary> reloaded = itineraryJsonService.getItinerary(itineraryId);
                        if (reloaded.isPresent()) {
                            itinerary = reloaded.get();
                            logger.info("Reloaded itinerary for final save retry");
                        } else {
                            logger.error("Failed to reload itinerary for retry");
                            throw e;
                        }
                    } else {
                        logger.error("Max retries ({}) exceeded on final save, giving up", maxRetries);
                        throw e;
                    }
                }
            }

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
     * Get city allocation for a specific day.
     */
    private CityAllocation getCityForDay(CityAllocationPlan cityPlan, int dayNumber) {
        if (cityPlan == null || cityPlan.getAllocations() == null) {
            return null;
        }

        for (CityAllocation allocation : cityPlan.getAllocations()) {
            if (dayNumber >= allocation.getStartDay() && dayNumber <= allocation.getEndDay()) {
                return allocation;
            }
        }

        return null;
    }

    /**
     * Get travel segment for a specific day.
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
     * Determine node count based on city type and travel.
     * NOTE: This is for LLM-generated nodes only (activities + meals).
     * Transport nodes are added programmatically and not counted here.
     */
    private int determineNodeCount(CityAllocation city, TravelSegment travel) {
        // Travel day with >6 hours travel
        if (travel != null && travel.isFullDayTravel()) {
            return 2; // 1 activity + 1 meal (travel node added separately)
        }

        // Travel day with 3-6 hours travel
        if (travel != null && travel.getEstimatedHours() >= 3) {
            return 3; // 2 activities + 1 meal (travel node added separately)
        }

        // No city context, use default
        if (city == null) {
            return 4; // Default: 2 activities + 2 meals
        }

        // Nature/adventure destinations
        if ("nature".equals(city.getDestinationType())) {
            return 3; // Safari/trek + 2 meals
        }

        // Gateway cities (arrival/departure)
        if ("gateway".equals(city.getDestinationType())) {
            return 3; // 1-2 activities + 2 meals
        }

        // Cultural/urban cities
        return 4; // 2-3 activities + 2 meals
    }

    /**
     * Check if arrival travel node should be added for Day 1.
     * Returns true if startLocation is provided and different from first city.
     */
    private boolean shouldAddArrivalTravel(CreateItineraryReq request, CityAllocation firstCity) {
        if (request.getStartLocation() == null || request.getStartLocation().isEmpty()) {
            return false;
        }

        // If no city context, assume arrival is needed
        if (firstCity == null) {
            return true;
        }

        // Check if startLocation is different from first city
        String startLoc = request.getStartLocation().toLowerCase().trim();
        String firstCityName = firstCity.getCityName().toLowerCase().trim();

        // Simple check: if startLocation doesn't contain first city name, it's different
        return !startLoc.contains(firstCityName) && !firstCityName.contains(startLoc);
    }

    /**
     * Check if departure travel node should be added for last day.
     * Returns true if startLocation is provided (assuming round trip).
     */
    private boolean shouldAddDepartureTravel(CreateItineraryReq request, CityAllocation lastCity) {
        // If startLocation provided, assume round trip (return to origin)
        return request.getStartLocation() != null && !request.getStartLocation().isEmpty();
    }

    /**
     * Create arrival travel node for Day 1 (returns node, doesn't add to day).
     * Example: "Flight: Bengaluru to Kuala Lumpur"
     * FIXED: Don't set ID manually - let NodeIdGenerator handle it.
     */
    private NormalizedNode createArrivalTravelNode(int dayNumber, CreateItineraryReq request, CityAllocation firstCity) {
        String arrivalCity = firstCity != null ? firstCity.getCityName() : request.getDestination();

        NormalizedNode arrivalNode = new NormalizedNode();
        arrivalNode.setType("transport");
        // Mark node as created by SkeletonPlannerAgent
        arrivalNode.setProcessingState(ProcessingState.CREATED);
        arrivalNode.addProcessedBy("SkeletonPlannerAgent");
        // FIXED: Don't set ID here - let NodeIdGenerator assign it
        arrivalNode.setTitle(String.format("Arrival: %s to %s",
                request.getStartLocation(),
                arrivalCity));

        // Set morning arrival timing (assume 9 AM arrival)
        NodeTiming timing = new NodeTiming();
        timing.setStartTime(6L * 60 * 60 * 1000);  // 6:00 AM (departure from origin)
        timing.setEndTime(9L * 60 * 60 * 1000);    // 9:00 AM (arrival at destination)
        timing.setDurationMin(180); // 3 hours (placeholder, will be refined by TransportAgent)
        arrivalNode.setTiming(timing);

        // Set location
        NodeLocation location = new NodeLocation();
        location.setName(String.format("%s to %s", request.getStartLocation(), arrivalCity));
        arrivalNode.setLocation(location);

        return arrivalNode;
    }

    /**
     * DEPRECATED: Old method that added node directly to day.
     * Kept for backward compatibility but not used in new flow.
     */
    @Deprecated
    private void addArrivalTravelNode(NormalizedDay day, CreateItineraryReq request, CityAllocation firstCity) {
        try {
            NormalizedNode arrivalNode = createArrivalTravelNode(day.getDayNumber(), request, firstCity);

            // Add to beginning of day (arrival happens first)
            if (day.getNodes() == null) {
                day.setNodes(new ArrayList<>());
            }
            day.getNodes().add(0, arrivalNode);

            logger.info("Added arrival travel node for Day 1: {} to {}",
                    request.getStartLocation(),
                    firstCity != null ? firstCity.getCityName() : request.getDestination());

        } catch (Exception e) {
            logger.warn("Failed to add arrival travel node: {}", e.getMessage());
        }
    }

    /**
     * Create departure travel node for last day (returns node, doesn't add to day).
     * Example: "Flight: Kuala Lumpur to Bengaluru"
     * FIXED: Don't set ID manually - let NodeIdGenerator handle it.
     */
    private NormalizedNode createDepartureTravelNode(int dayNumber, CreateItineraryReq request, CityAllocation lastCity) {
        String departureCity = lastCity != null ? lastCity.getCityName() : request.getDestination();

        NormalizedNode departureNode = new NormalizedNode();
        departureNode.setType("transport");
        // Mark node as created by SkeletonPlannerAgent
        departureNode.setProcessingState(ProcessingState.CREATED);
        departureNode.addProcessedBy("SkeletonPlannerAgent");
        // FIXED: Don't set ID here - let NodeIdGenerator assign it
        departureNode.setTitle(String.format("Departure: %s to %s",
                departureCity,
                request.getStartLocation()));

        // Set evening departure timing (assume 6 PM departure)
        NodeTiming timing = new NodeTiming();
        timing.setStartTime(18L * 60 * 60 * 1000); // 6:00 PM (departure)
        timing.setEndTime(21L * 60 * 60 * 1000);   // 9:00 PM (arrival at origin)
        timing.setDurationMin(180); // 3 hours (placeholder, will be refined by TransportAgent)
        departureNode.setTiming(timing);

        // Set location
        NodeLocation location = new NodeLocation();
        location.setName(String.format("%s to %s", departureCity, request.getStartLocation()));
        departureNode.setLocation(location);

        return departureNode;
    }

    /**
     * DEPRECATED: Old method that added node directly to day.
     */
    @Deprecated
    private void addDepartureTravelNode(NormalizedDay day, CreateItineraryReq request, CityAllocation lastCity) {
        try {
            NormalizedNode departureNode = createDepartureTravelNode(day.getDayNumber(), request, lastCity);

            // Add to end of day (departure happens last)
            if (day.getNodes() == null) {
                day.setNodes(new ArrayList<>());
            }
            day.getNodes().add(departureNode);

            logger.info("Added departure travel node for last day: {} to {}",
                    lastCity != null ? lastCity.getCityName() : request.getDestination(),
                    request.getStartLocation());

        } catch (Exception e) {
            logger.warn("Failed to add departure travel node: {}", e.getMessage());
        }
    }

    /**
     * Create travel node for inter-city travel (returns node, doesn't add to day).
     * CRITICAL: This ensures travel nodes are ALWAYS created programmatically.
     * FIXED: Don't set ID manually - let NodeIdGenerator handle it.
     */
    private NormalizedNode createTravelNode(int dayNumber, TravelSegment travelSegment) {
        // Create travel node placeholder
        NormalizedNode travelNode = new NormalizedNode();
        travelNode.setType("transport");
        // Mark node as created by SkeletonPlannerAgent
        travelNode.setProcessingState(ProcessingState.CREATED);
        travelNode.addProcessedBy("SkeletonPlannerAgent");
        // FIXED: Don't set ID here - let NodeIdGenerator assign it
        travelNode.setTitle(String.format("Travel: %s to %s",
                travelSegment.getFromCity(),
                travelSegment.getToCity()));

        // Set basic timing based on travel duration
        NodeTiming timing = new NodeTiming();
        if (travelSegment.isFullDayTravel()) {
            // Full day travel: 8 AM - 6 PM (simplified epoch ms)
            timing.setStartTime(8L * 60 * 60 * 1000); // 8:00 AM
            timing.setEndTime(18L * 60 * 60 * 1000);  // 6:00 PM
            timing.setDurationMin(travelSegment.getEstimatedHours() * 60);
        } else {
            // Partial travel: morning departure
            timing.setStartTime(9L * 60 * 60 * 1000); // 9:00 AM
            timing.setEndTime((9L + travelSegment.getEstimatedHours()) * 60 * 60 * 1000);
            timing.setDurationMin(travelSegment.getEstimatedHours() * 60);
        }
        travelNode.setTiming(timing);

        // Set location
        NodeLocation location = new NodeLocation();
        location.setName(String.format("%s to %s", travelSegment.getFromCity(), travelSegment.getToCity()));
        travelNode.setLocation(location);

        return travelNode;
    }

    /**
     * DEPRECATED: Old method that added node directly to day.
     * This method is no longer used in the main flow.
     */
    @Deprecated
    private void addTravelNodePlaceholder(NormalizedDay day, TravelSegment travelSegment) {
        try {
            NormalizedNode travelNode = createTravelNode(day.getDayNumber(), travelSegment);

            // Add to beginning of day (travel happens first)
            if (day.getNodes() == null) {
                day.setNodes(new ArrayList<>());
            }
            day.getNodes().add(0, travelNode);

            logger.info("Added travel placeholder for day {}: {} to {} ({} hours, full-day: {})",
                    day.getDayNumber(),
                    travelSegment.getFromCity(),
                    travelSegment.getToCity(),
                    travelSegment.getEstimatedHours(),
                    travelSegment.isFullDayTravel());

            // Add accommodation placeholder if overnight travel or late arrival
            if (shouldAddAccommodationPlaceholder(travelSegment)) {
                // Create temp itinerary for ID generation
                NormalizedItinerary tempItinerary = new NormalizedItinerary();
                tempItinerary.setDays(new ArrayList<>());
                tempItinerary.getDays().add(day);
                
                addAccommodationPlaceholder(day, travelSegment, tempItinerary);
            }

        } catch (Exception e) {
            logger.warn("Failed to add travel placeholder for day {}: {}",
                    day.getDayNumber(), e.getMessage());
        }
    }

    /**
     * Determine if accommodation placeholder is needed.
     * Add accommodation if:
     * 1. Full day travel (likely arriving late)
     * 2. Travel duration > 8 hours (overnight journey)
     */
    private boolean shouldAddAccommodationPlaceholder(TravelSegment travelSegment) {
        return travelSegment.isFullDayTravel() || travelSegment.getEstimatedHours() > 8;
    }

    /**
     * Add accommodation placeholder for overnight stays.
     * CRITICAL: Multi-city trips need hotel bookings.
     * CRITICAL FIX: Now accepts tempItinerary parameter to generate ID immediately.
     * 
     * NOTE: This method is only called from deprecated addTravelNodePlaceholder.
     * In the main flow, IDs are assigned in the post-processing step (line ~930).
     */
    private void addAccommodationPlaceholder(NormalizedDay day, TravelSegment travelSegment, 
                                            NormalizedItinerary tempItinerary) {
        try {
            NormalizedNode accommodationNode = new NormalizedNode();
            accommodationNode.setType("accommodation");
            // Mark node as created by SkeletonPlannerAgent
            accommodationNode.setProcessingState(ProcessingState.CREATED);
            accommodationNode.addProcessedBy("SkeletonPlannerAgent");
            
            // CRITICAL FIX: Generate ID immediately if tempItinerary is provided
            if (tempItinerary != null) {
                accommodationNode.setId(nodeIdGenerator.generateNodeId("accommodation", 
                                                                       day.getDayNumber(), 
                                                                       tempItinerary));
                logger.debug("Generated ID for accommodation node: {}", accommodationNode.getId());
            } else {
                // Fallback: ID will be generated in post-processing step
                logger.debug("Accommodation node created without ID (will be assigned in post-processing)");
            }
            
            accommodationNode.setTitle(String.format("Hotel in %s", travelSegment.getToCity()));

            // Set evening timing
            NodeTiming timing = new NodeTiming();
            timing.setStartTime(20L * 60 * 60 * 1000); // 8:00 PM
            timing.setEndTime(22L * 60 * 60 * 1000);   // 10:00 PM
            timing.setDurationMin(120); // 2 hours for check-in and settling
            accommodationNode.setTiming(timing);

            // Set location
            NodeLocation location = new NodeLocation();
            location.setName(travelSegment.getToCity());
            accommodationNode.setLocation(location);

            day.getNodes().add(accommodationNode);

            logger.info("Added accommodation placeholder for day {} in {} (ID: {})",
                    day.getDayNumber(), travelSegment.getToCity(), 
                    accommodationNode.getId() != null ? accommodationNode.getId() : "pending");

        } catch (Exception e) {
            logger.warn("Failed to add accommodation placeholder: {}", e.getMessage());
        }
    }

    /**
     * Create accommodation node for a night (returns node, doesn't add to day).
     * FIXED: Use NodeIdGenerator for consistent ID format instead of manual generation.
     *
     * @param dayNumber      The day number
     * @param cityAllocation The city allocation for this day
     */
    private NormalizedNode createAccommodationNode(int dayNumber, CityAllocation cityAllocation) {
        NormalizedNode accommodationNode = new NormalizedNode();
        accommodationNode.setType("accommodation");
        // Mark node as created by SkeletonPlannerAgent
        accommodationNode.setProcessingState(ProcessingState.CREATED);
        accommodationNode.addProcessedBy("SkeletonPlannerAgent");
        // FIXED: Don't set ID here - let it be generated later by NodeIdGenerator
        // This ensures consistent format (day{N}_node{M}) and avoids validation errors
        accommodationNode.setTitle(String.format("Hotel in %s", cityAllocation.getCityName()));

        // Set evening timing (check-in time)
        NodeTiming timing = new NodeTiming();
        timing.setStartTime(20L * 60 * 60 * 1000); // 8:00 PM
        timing.setEndTime(22L * 60 * 60 * 1000);   // 10:00 PM
        timing.setDurationMin(120); // 2 hours for check-in and settling
        accommodationNode.setTiming(timing);

        // Set location
        NodeLocation location = new NodeLocation();
        location.setName(cityAllocation.getCityName());
        accommodationNode.setLocation(location);

        return accommodationNode;
    }

    /**
     * DEPRECATED: Old method that added node directly to day.
     */
    @Deprecated
    private void addAccommodationPlaceholderForNight(NormalizedDay day, CityAllocation cityAllocation) {
        try {
            // Check if accommodation already exists (from travel day logic)
            if (day.getNodes() != null) {
                for (NormalizedNode node : day.getNodes()) {
                    if ("accommodation".equals(node.getType())) {
                        logger.debug("Accommodation already exists for day {}, skipping", day.getDayNumber());
                        return; // Already has accommodation
                    }
                }
            }

            NormalizedNode accommodationNode = createAccommodationNode(day.getDayNumber(), cityAllocation);

            if (day.getNodes() == null) {
                day.setNodes(new ArrayList<>());
            }
            day.getNodes().add(accommodationNode);

            logger.info("Added accommodation placeholder for night of day {} in {}",
                    day.getDayNumber(), cityAllocation.getCityName());

        } catch (Exception e) {
            logger.warn("Failed to add accommodation placeholder for night: {}", e.getMessage());
        }
    }

    /**
     * Merge pre-created infrastructure nodes with LLM-generated nodes and position them intelligently.
     * <p>
     * Positioning logic:
     * - Transport nodes: Position based on timing (morning = start, afternoon = middle, evening = end)
     * - Accommodation nodes: Always at end of day
     * - Activity/meal nodes: Fill in between infrastructure nodes
     *
     * @param llmNodes        Nodes generated by LLM (activities, meals)
     * @param preCreatedNodes Infrastructure nodes (transport, accommodation)
     * @return Merged and sorted list of nodes
     */
    private List<NormalizedNode> mergeAndPositionNodes(List<NormalizedNode> llmNodes,
                                                       List<NormalizedNode> preCreatedNodes) {
        List<NormalizedNode> allNodes = new ArrayList<>();

        // Add all nodes to a single list
        if (llmNodes != null) {
            allNodes.addAll(llmNodes);
        }
        if (preCreatedNodes != null) {
            allNodes.addAll(preCreatedNodes);
        }

        // Sort by start time (chronological order)
        allNodes.sort((n1, n2) -> {
            Long time1 = n1.getTiming() != null ? n1.getTiming().getStartTime() : 0L;
            Long time2 = n2.getTiming() != null ? n2.getTiming().getStartTime() : 0L;

            // Handle null times
            if (time1 == null) time1 = 0L;
            if (time2 == null) time2 = 0L;

            // Primary sort: by time
            int timeCompare = time1.compareTo(time2);
            if (timeCompare != 0) {
                return timeCompare;
            }

            // Secondary sort: infrastructure nodes (transport, accommodation) come before activities
            int priority1 = getNodePriority(n1.getType());
            int priority2 = getNodePriority(n2.getType());
            return Integer.compare(priority1, priority2);
        });

        logger.debug("Merged and positioned {} nodes in chronological order", allNodes.size());

        return allNodes;
    }

    /**
     * Get node priority for sorting (lower = earlier in day).
     */
    private int getNodePriority(String nodeType) {
        return switch (nodeType != null ? nodeType.toLowerCase() : "activity") {
            case "transport" -> 1;      // Transport first
            case "meal" -> 2;           // Meals second
            case "attraction", "activity" -> 3;  // Activities third
            case "accommodation" -> 4;  // Accommodation last
            default -> 3;
        };
    }

    /**
     * Format time in milliseconds to HH:mm string for display.
     */
    private String formatTime(Long timeMs) {
        if (timeMs == null) {
            return "??:??";
        }

        // Use centralized formatter - handles both full timestamps and time-of-day
        String formatted = com.tripplanner.util.TimeFormatter.formatFor24HourDisplay(timeMs);
        return "N/A".equals(formatted) ? "??:??" : formatted;
    }

    /**
     * Build context string from previous days to avoid repetition.
     */
    private String buildPreviousDaysContext(List<NormalizedDay> previousDays) {
        if (previousDays == null || previousDays.isEmpty()) {
            return "No previous days.";
        }

        StringBuilder context = new StringBuilder();
        context.append("Previous days in this trip:\n");

        for (NormalizedDay day : previousDays) {
            context.append("Day ").append(day.getDayNumber()).append(" (").append(day.getLocation()).append("): ");
            if (day.getNodes() != null && !day.getNodes().isEmpty()) {
                List<String> activities = new ArrayList<>();
                for (NormalizedNode node : day.getNodes()) {
                    if ("attraction".equals(node.getType()) || "activity".equals(node.getType())) {
                        activities.add(node.getTitle());
                    }
                }
                context.append(String.join(", ", activities));
            }
            context.append("\n");
        }

        context.append("\nIMPORTANT: Avoid repeating similar activities from previous days.\n");

        return context.toString();
    }

    /**
     * Generate skeleton for a single day.
     * Uses FAST_FAIL strategy for immediate failover without retries.
     * <p>
     * IMPROVED: Now receives pre-created infrastructure nodes (transport, accommodation)
     * so LLM can see them in context and plan activities around them.
     */
    private NormalizedDay generateDaySkeleton(CreateItineraryReq request, int dayNumber,
                                              CityAllocation cityForThisDay,
                                              TravelSegment travelSegment,
                                              List<NormalizedDay> previousDays,
                                              List<NormalizedNode> preCreatedNodes) {
        String systemPrompt = buildSkeletonSystemPrompt();
        String userPrompt = buildSkeletonUserPrompt(request, dayNumber, cityForThisDay,
                travelSegment, previousDays, preCreatedNodes);

        // IMPROVED: Schema now expects only activity/meal nodes since infrastructure is pre-created
        // This eliminates the schema mismatch issue
        int minNodes = determineMinNodesForSchema(travelSegment, dayNumber, request);
        String schema = buildSkeletonJsonSchema(minNodes);

        logger.info("Generating skeleton for day {} with FAST_FAIL strategy (no retries, immediate failover)", dayNumber);

        // Use FAST_FAIL strategy for critical path (trip creation)
        // This ensures immediate failover to backup provider without retry delays
        String response;
        if (aiClient instanceof ResilientAiClient) {
            ResilientAiClient resilientClient = (ResilientAiClient) aiClient;
            response = resilientClient.generateStructuredContent(userPrompt, schema, systemPrompt, RetryStrategy.FAST_FAIL);
        } else {
            // Fallback for non-resilient clients
            response = aiClient.generateStructuredContent(userPrompt, schema, systemPrompt);
        }

        // Log full LLM response for analysis
        logger.info("=== SKELETON PLANNER AGENT - FULL LLM RESPONSE ===");
        logger.info("Day: {}", dayNumber);
        logger.info("Raw Response: {}", response);
        logger.info("=== END SKELETON PLANNER RESPONSE ===");

        try {
            // IMPROVED: Validate response against schema before parsing
            LLMSchemaValidator.ValidationResult validationResult = schemaValidator.validateWithLogging(
                    response, schema, "SkeletonPlanner Day " + dayNumber);

            if (!validationResult.isValid()) {
                String errorMsg = schemaValidator.getUserFriendlyError(validationResult);
                logger.error("Schema validation failed for day {}: {}", dayNumber, errorMsg);

                // Check if retryable
                if (schemaValidator.isRetryable(validationResult)) {
                    throw new RuntimeException("LLM response validation failed (retryable): " + errorMsg);
                } else {
                    throw new RuntimeException("LLM response validation failed (non-retryable): " + errorMsg);
                }
            }

            // Use validated data
            com.fasterxml.jackson.databind.JsonNode root = validationResult.getData();
            logger.info("Schema validation passed for day {}", dayNumber);

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

            // Set day location from city context if available
            if (cityForThisDay != null && (day.getLocation() == null || day.getLocation().isEmpty())) {
                day.setLocation(cityForThisDay.getCityName());
                logger.debug("Set day {} location to: {}", dayNumber, cityForThisDay.getCityName());
            }

            // IMPROVED: Merge pre-created infrastructure nodes with LLM-generated nodes
            // Position them intelligently based on timing
            if (preCreatedNodes != null && !preCreatedNodes.isEmpty()) {
                day.setNodes(mergeAndPositionNodes(day.getNodes(), preCreatedNodes));
                logger.info("Merged {} pre-created nodes with {} LLM-generated nodes for day {}",
                        preCreatedNodes.size(),
                        day.getNodes().size() - preCreatedNodes.size(),
                        dayNumber);
            }

            // Generate IDs for nodes if missing (LLM no longer generates IDs)
            if (day.getNodes() != null) {
                // Create a temporary itinerary for ID generation (includes pre-created nodes)
                NormalizedItinerary tempItinerary = new NormalizedItinerary();
                tempItinerary.setDays(new ArrayList<>());
                tempItinerary.getDays().add(day);

                for (int i = 0; i < day.getNodes().size(); i++) {
                    NormalizedNode node = day.getNodes().get(i);
                    if (node.getId() == null || node.getId().isEmpty()) {
                        // Use generateNodeId which checks for uniqueness and avoids conflicts
                        String nodeId = nodeIdGenerator.generateNodeId(node.getType(), dayNumber, tempItinerary);
                        node.setId(nodeId);
                        logger.debug("Assigned unique ID {} to node: {}", nodeId, node.getTitle());
                    }

                    // Set placeholder values with more descriptive titles
                    if (node.getTitle() == null || node.getTitle().isEmpty()) {
                        String placeholderTitle = generatePlaceholderTitle(node.getType(), dayNumber, i + 1);
                        node.setTitle(placeholderTitle);
                        logger.debug("Set placeholder title '{}' for node {}", placeholderTitle, node.getId());
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

    /**
     * Generate a descriptive placeholder title for a node based on its type and position.
     */
    private String generatePlaceholderTitle(String nodeType, int dayNumber, int sequenceNumber) {
        String timeOfDay = getTimeOfDay(sequenceNumber);

        return switch (nodeType != null ? nodeType.toLowerCase() : "activity") {
            case "meal", "restaurant" -> String.format("%s Meal (Day %d)", timeOfDay, dayNumber);
            case "attraction", "activity" -> String.format("%s Activity (Day %d)", timeOfDay, dayNumber);
            case "transport", "transportation" -> String.format("Transport (Day %d)", dayNumber);
            case "accommodation", "hotel" -> String.format("Accommodation (Day %d)", dayNumber);
            default -> String.format("%s Activity (Day %d)", timeOfDay, dayNumber);
        };
    }

    /**
     * Determine time of day based on sequence number.
     */
    private String getTimeOfDay(int sequenceNumber) {
        if (sequenceNumber <= 2) return "Morning";
        if (sequenceNumber <= 4) return "Afternoon";
        if (sequenceNumber <= 6) return "Evening";
        return "Late";
    }

    private NormalizedItinerary createInitialItinerary(String itineraryId, CreateItineraryReq request) {
        NormalizedItinerary itinerary = new NormalizedItinerary();
        itinerary.setItineraryId(itineraryId);
        itinerary.setVersion(1);
        itinerary.setSummary(String.format("%d-day trip to %s (skeleton)",
                request.getDurationDays(), request.getDestination()));

        // NOTE: Currency should be set by CityAllocationAgent which runs before this.
        // This fallback path should rarely be hit. Leave currency as null if not set.
        if (itinerary.getCurrency() == null) {
            logger.warn("Fallback: Currency not set by CityAllocationAgent for {}. Leaving as null.",
                    request.getDestination());
        }

        itinerary.setThemes(request.getInterests() != null ? request.getInterests() : new ArrayList<>());

        // CRITICAL: Propagate user constraints to itinerary for downstream agents
        itinerary.setConstraints(request.getConstraints() != null ? request.getConstraints() : new ArrayList<>());

        // CRITICAL: Propagate budget parameters to itinerary for budget tracking
        itinerary.setBudgetMin(request.getBudgetMin());
        itinerary.setBudgetMax(request.getBudgetMax());
        itinerary.setPartySize(request.getParty() != null ? request.getParty().getTotalGuests() : 1);

        itinerary.setDays(new ArrayList<>());
        itinerary.setCreatedAt(System.currentTimeMillis());
        itinerary.setUpdatedAt(System.currentTimeMillis());

        logger.info("Created initial itinerary with {} constraints, budget: {}-{}, party size: {}",
                itinerary.getConstraints() != null ? itinerary.getConstraints().size() : 0,
                itinerary.getBudgetMin(), itinerary.getBudgetMax(), itinerary.getPartySize());

        return itinerary;
    }

    private String buildSkeletonSystemPrompt() {
        return """
                You are a travel planning assistant creating a DAY STRUCTURE SKELETON.
                
                IMPORTANT: Generate ONLY activities and meals. Infrastructure (transport, accommodation) is pre-scheduled.
                
                Your job:
                1. Review pre-scheduled infrastructure nodes and timings (transport, accommodation) if provided
                2. Plan activities and meals that fit AROUND the pre-scheduled items
                3. Ensure timing doesn't conflict with transport 
                4. CRITICAL: Include travel time buffers between activities based on rough time taken to travel (default to 30 mins)
                5. Use DESCRIPTIVE placeholder titles that indicate the type of activity
                6. Consider party size when planning activities (group-friendly vs individual)
                7. Respect budget tier and ensure activities match the budget level
                8. Use city context to plan appropriate activities for the location
                9. Avoid repeating activities from previous days
                10. CRITICAL: Set the day-level "location" field to the SPECIFIC CITY NAME (e.g., "Zurich", "Interlaken"), NOT the country or destination
                11. DO NOT generate node IDs - they will be assigned automatically
                
                Title Guidelines:
                - Use DESCRIPTIVE placeholders that indicate activity type
                - Examples: "Morning Cultural Exploration", "Lunch at Local Restaurant", "Afternoon Shopping District"
                - NOT generic like "Morning Activity" or "Lunch Spot"
                - Include time of day and activity category
                - Make titles informative enough to understand the plan flow
                
                Location Guidelines:
                - For location.name, use SPECIFIC area/district names within the destination (e.g., "Shibuya, Tokyo" not just "Tokyo")
                - If the activity is in a specific neighborhood or district, include that in location.name
                - Examples: "Chowk, Lucknow", "Hazratganj, Lucknow", "Shibuya, Tokyo", "Asakusa, Tokyo"
                - Do NOT use generic names like "Breakfast Spot" or "Morning Activity Area"
                - The enrichment agent will add exact place details later, but specific area names help with accurate place search
                
                Budget & Pricing Guidelines:
                - ALL cost estimates must be PER PERSON
                - Consider the destination's local currency and cost of living
                - Budget tiers should reflect realistic local prices:
                  * Low: Budget-friendly options (hostels, street food, free attractions)
                  * Medium: Mid-range options (3-star hotels, casual dining, paid attractions)
                  * High: Premium options (4-5 star hotels, fine dining, exclusive experiences)
                - When party size is provided, plan activities that work well for groups
                
                Do NOT include:
                - Specific place names or addresses
                - Detailed descriptions, Exact costs, coordinates, Reviews or ratings (other agents will calculate these)
                
                Node Count Guidelines Based on Available Time:
                - Full day available (no travel): 4-6 nodes (2-3 activities + 2-3 meals) - MINIMUM 4
                - Half day available (morning/afternoon travel): 3-4 nodes (2 activities + 1-2 meals) - MINIMUM 3
                - Limited time (full-day travel): 2-3 nodes (1 activity + 1-2 meals) - MINIMUM 2
                - Check pre-scheduled transport timing to determine available time
                
                You MUST generate a relaistic number of nodes based on activity and duration it may take.
                
                Activity Duration Awareness:
                - Safari/Trek: 4-8 hours → Plan only 1-2 other activities
                - Museum/Temple: 1-2 hours → Can plan 3-4 activities
                - Shopping/Market: 2-3 hours → Can plan 2-3 activities
                - Full-day tour: 8-10 hours → No other activities
                
                CRITICAL RULES:
                1. DO NOT create "transport" or "accommodation" nodes - they are pre-created
                2. Focus ONLY on "attraction" and "meal" nodes
                3. Plan activities in time slots NOT occupied by pre-scheduled infrastructure
                4. If transport is scheduled 8 AM - 2 PM, dont plan activities for that time.
                
                Keep it relaistic, fast and well thought - other agents will add specific details later.
                """;
    }

    private String buildSkeletonUserPrompt(CreateItineraryReq request, int dayNumber,
                                           CityAllocation cityForThisDay,
                                           TravelSegment travelSegment,
                                           List<NormalizedDay> previousDays,
                                           List<NormalizedNode> preCreatedNodes) {
        StringBuilder prompt = new StringBuilder();

        LocalDate startDate = LocalDate.parse(request.getStartDate().toString());
        LocalDate dayDate = startDate.plusDays(dayNumber - 1);

        prompt.append("Create skeleton structure for:\n");
        prompt.append("Day ").append(dayNumber).append(" of ").append(request.getDurationDays()).append("\n");
        prompt.append("Date: ").append(dayDate.format(DateTimeFormatter.ISO_LOCAL_DATE)).append("\n");
        prompt.append("Destination: ").append(request.getDestination()).append("\n");

        // Add city context if available
        if (cityForThisDay != null) {
            prompt.append("\n=== CITY CONTEXT FOR THIS DAY ===\n");
            prompt.append("City: ").append(cityForThisDay.getCityName()).append("\n");
            prompt.append("CRITICAL: Set the 'location' field to EXACTLY: ").append(cityForThisDay.getCityName()).append("\n");
            prompt.append("DO NOT use country name or destination name - use ONLY the city name above.\n");
            prompt.append("City Type: ").append(cityForThisDay.getDestinationType()).append(" (gateway/nature/cultural/urban)\n");
            prompt.append("Day ").append(dayNumber - cityForThisDay.getStartDay() + 1)
                    .append(" of ").append(cityForThisDay.getTotalDays())
                    .append(" in this city\n");
            if (cityForThisDay.getHighlights() != null && !cityForThisDay.getHighlights().isEmpty()) {
                prompt.append("City Highlights: ").append(String.join(", ", cityForThisDay.getHighlights())).append("\n");
            }
        }

        // IMPROVED: Add pre-created infrastructure nodes context
        if (preCreatedNodes != null && !preCreatedNodes.isEmpty()) {
            prompt.append("\n=== PRE-SCHEDULED INFRASTRUCTURE ===\n");
            prompt.append("The following nodes are already scheduled for this day:\n");
            for (NormalizedNode node : preCreatedNodes) {
                prompt.append("- ").append(node.getType().toUpperCase()).append(": ");
                prompt.append(node.getTitle());
                if (node.getTiming() != null) {
                    prompt.append(" (").append(formatTime(node.getTiming().getStartTime()));
                    prompt.append(" - ").append(formatTime(node.getTiming().getEndTime())).append(")");
                }
                prompt.append("\n");
            }
            prompt.append("\nIMPORTANT: Plan activities around these pre-scheduled items.\n");
            prompt.append("Do NOT create duplicate transport or accommodation nodes.\n");
            prompt.append("Focus on attractions and meals that fit in the available time slots.\n");
        }

        // Add travel context if available (for additional context)
        if (travelSegment != null) {
            prompt.append("\n=== TRAVEL DETAILS ===\n");
            prompt.append("Travel: ").append(travelSegment.getFromCity())
                    .append(" → ").append(travelSegment.getToCity()).append("\n");
            prompt.append("Mode: ").append(travelSegment.getTravelMode()).append("\n");
            prompt.append("Duration: ").append(travelSegment.getEstimatedHours()).append(" hours\n");
            prompt.append("Full Day Travel: ").append(travelSegment.isFullDayTravel() ? "Yes" : "No").append("\n");
            if (travelSegment.getNotes() != null && !travelSegment.getNotes().isEmpty()) {
                prompt.append("Notes: ").append(travelSegment.getNotes()).append("\n");
            }
            prompt.append("NOTE: Transport node is already created above. Plan activities for remaining time.\n");
        }

        // Add previous days context
        if (previousDays != null && !previousDays.isEmpty()) {
            prompt.append("\n=== PREVIOUS DAYS CONTEXT ===\n");
            prompt.append(buildPreviousDaysContext(previousDays));
        }

        // Add start location if provided
        if (request.getStartLocation() != null && !request.getStartLocation().isEmpty()) {
            prompt.append("Starting from: ").append(request.getStartLocation()).append("\n");
        }

        // Add party details
        if (request.getParty() != null) {
            int totalGuests = request.getParty().getTotalGuests();
            prompt.append("Party size: ").append(totalGuests).append(" people");
            prompt.append(" (").append(request.getParty().getAdults()).append(" adults");
            if (request.getParty().getChildren() > 0) {
                prompt.append(", ").append(request.getParty().getChildren()).append(" children");
            }
            if (request.getParty().getInfants() > 0) {
                prompt.append(", ").append(request.getParty().getInfants()).append(" infants");
            }
            prompt.append(")\n");

            // Add note about group-appropriate activities
            if (totalGuests > 1) {
                prompt.append("NOTE: Plan activities suitable for groups of ").append(totalGuests).append(" people.\n");
            }
        }

        // Enhanced budget information with currency context
        String budgetTier = request.getBudgetTier() != null ? request.getBudgetTier() : "medium";
        prompt.append("\nBudget preference: ").append(budgetTier).append("\n");
        prompt.append("IMPORTANT: Interpret budget tier relative to ").append(request.getDestination()).append(":\n");

        // Provide context-aware budget guidance
        switch (budgetTier.toLowerCase()) {
            case "budget":
                prompt.append("- 'budget' means cost-conscious choices appropriate for this destination\n");
                prompt.append("- Select affordable but quality accommodations, local restaurants, public transport\n");
                prompt.append("- Focus on free/low-cost activities, local experiences\n");
                break;
            case "medium":
                prompt.append("- 'medium' means comfortable mid-range options for this destination\n");
                prompt.append("- Balance between cost and comfort with good value\n");
                prompt.append("- Mix of popular attractions, decent restaurants, efficient transport\n");
                break;
            case "luxury":
                prompt.append("- 'luxury' means premium experiences appropriate for this destination\n");
                prompt.append("- High-end accommodations, fine dining, private transport\n");
                prompt.append("- Exclusive activities, VIP experiences, premium services\n");
                break;
            default:
                prompt.append("- Select options that provide good value for this destination\n");
        }

        prompt.append("All cost estimates should be PER PERSON in local currency.\n");
        prompt.append("Adjust recommendations based on local cost of living and tourism standards.\n");

        // Add interests
        if (request.getInterests() != null && !request.getInterests().isEmpty()) {
            prompt.append("\nInterests: ").append(String.join(", ", request.getInterests())).append("\n");
        }

        // CRITICAL: Include user's custom instructions/constraints
        if (request.getConstraints() != null && !request.getConstraints().isEmpty()) {
            prompt.append("\nIMPORTANT - User Requirements:\n");
            for (String constraint : request.getConstraints()) {
                prompt.append("- ").append(constraint).append("\n");
            }
            prompt.append("Please ensure the itinerary structure accommodates these requirements.\n");
        }


        // Determine expected node count and minimum requirement
        int expectedNodeCount = determineNodeCount(cityForThisDay, travelSegment);
        int minNodeCount = determineMinNodesForSchema(travelSegment, dayNumber, request);

        prompt.append("\n=== NODE COUNT REQUIREMENTS ===\n");
        prompt.append("MINIMUM nodes required: ").append(minNodeCount).append(" (CRITICAL - schema will reject if less)\n");
        prompt.append("Expected nodes for this day: ").append(expectedNodeCount).append("\n");
        if (cityForThisDay != null) {
            prompt.append("Reason: ");
            if (travelSegment != null && travelSegment.isFullDayTravel()) {
                prompt.append("Full-day travel (travel + 1 meal)\n");
            } else if (travelSegment != null) {
                prompt.append("Partial travel day (travel + limited activities)\n");
            } else if ("nature".equals(cityForThisDay.getDestinationType())) {
                prompt.append("Nature destination (safaris/treks take 4-8 hours)\n");
            } else if ("gateway".equals(cityForThisDay.getDestinationType())) {
                prompt.append("Gateway city (moderate pace)\n");
            } else {
                prompt.append("Cultural/urban city (normal pace)\n");
            }
        }

        prompt.append("\nCRITICAL: You MUST generate at least ").append(minNodeCount).append(" nodes.\n");
        prompt.append("If you generate fewer nodes, the response will be REJECTED.\n");

        prompt.append("\nGenerate time slots with node type placeholders.\n");
        prompt.append("Use descriptive titles like 'Morning Cultural Exploration', 'Lunch at Local Restaurant', etc.\n");
        prompt.append("Focus on timing and logical flow, not specific places.\n");
        prompt.append("Consider the party size when planning activities and meal venues.\n");
        if (cityForThisDay == null) {
            prompt.append("Consider multiple cities if destination is a general location like a state or country\n");
        }

        // Add context for Day 1 and last day (arrival/departure handled programmatically)
        if (dayNumber == 1 && request.getStartLocation() != null && !request.getStartLocation().isEmpty()) {
            prompt.append("NOTE: This is Day 1 (arrival day). Arrival travel is handled separately. Plan activities for afternoon/evening only.\n");
        }
        if (dayNumber == request.getDurationDays() && request.getStartLocation() != null && !request.getStartLocation().isEmpty()) {
            prompt.append("NOTE: This is the last day (departure day). Departure travel is handled separately. Plan activities for morning/afternoon only.\n");
        }

        prompt.append("IMPORTANT: Do NOT include 'id' field in nodes - IDs will be assigned automatically.\n");

        return prompt.toString();
    }

    /**
     * Determine minimum nodes for schema validation.
     * CRITICAL: This accounts for programmatically-added nodes (transport, accommodation).
     *
     * @param travelSegment Travel segment for this day (null if no travel)
     * @param dayNumber     Current day number
     * @param request       Original request
     * @return Minimum number of LLM-generated nodes required
     */
    private int determineMinNodesForSchema(TravelSegment travelSegment, int dayNumber, CreateItineraryReq request) {
        // Full-day travel: 2 nodes (1 meal + 1 activity)
        // Transport and accommodation will be added programmatically
        if (travelSegment != null && travelSegment.isFullDayTravel()) {
            return 2;
        }

        // Partial travel day: 3 nodes (2 activities + 1 meal)
        // Transport will be added programmatically
        if (travelSegment != null) {
            return 3;
        }

        // Day 1 with arrival: 3 nodes (arrival transport added programmatically)
        if (dayNumber == 1 && shouldAddArrivalTravel(request, null)) {
            return 3;
        }

        // Last day with departure: 3 nodes (departure transport added programmatically)
        if (dayNumber == request.getDurationDays() && shouldAddDepartureTravel(request, null)) {
            return 3;
        }

        // Regular day: 4 nodes minimum (2-3 activities + 2 meals)
        return 4;
    }

    private String buildSkeletonJsonSchema(int minNodes) {
        return String.format("""
                {
                  "type": "object",
                  "properties": {
                    "dayNumber": { "type": "integer" },
                    "date": { "type": "string", "format": "date" },
                    "location": { "type": "string" },
                    "summary": { "type": "string" },
                    "nodes": {
                      "type": "array",
                      "minItems": %d,
                      "maxItems": 7,
                      "items": {
                        "type": "object",
                        "properties": {
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
                """, minNodes);
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

    // ========== SKELETON PLANNER AGENT - TOOL INTEGRATION METHODS ==========

    /**
     * Generate node ID using the Generate Node ID tool.
     */
    private String generateNodeIdViaTool(String itineraryId, Integer dayNumber, String nodeType) {
        if (!skeletonToolsEnabled) {
            // FIXED: Use proper method with itinerary context
            Optional<NormalizedItinerary> itineraryOpt = itineraryJsonService.getItinerary(itineraryId);
            if (itineraryOpt.isPresent()) {
                return nodeIdGenerator.generateNodeId(nodeType, dayNumber, itineraryOpt.get());
            }
            logger.warn("Itinerary not found for ID generation, using deprecated method");
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
            }
            if (fallbackOnError) {
                Optional<NormalizedItinerary> itineraryOpt = itineraryJsonService.getItinerary(itineraryId);
                if (itineraryOpt.isPresent()) {
                    return nodeIdGenerator.generateNodeId(nodeType, dayNumber, itineraryOpt.get());
                }
                return nodeIdGenerator.generateNodeId(nodeType, dayNumber);
            }
            return null;
        } catch (Exception e) {
            logger.error("Generate Node ID tool error: {}", e.getMessage());
            if (fallbackOnError) {
                Optional<NormalizedItinerary> itineraryOpt = itineraryJsonService.getItinerary(itineraryId);
                if (itineraryOpt.isPresent()) {
                    return nodeIdGenerator.generateNodeId(nodeType, dayNumber, itineraryOpt.get());
                }
                return nodeIdGenerator.generateNodeId(nodeType, dayNumber);
            }
            return null;
        }
    }

    /**
     * Check user constraints before skeleton generation.
     */
    private ConstraintCheckResult checkConstraintsViaTool(String itineraryId) {
        if (!skeletonToolsEnabled) {
            return new ConstraintCheckResult();
        }

        ConstraintCheckRequest request = new ConstraintCheckRequest(itineraryId);
        request.setCheckBudget(true);
        request.setCheckPartySize(true);

        try {
            ConstraintCheckResult result = restTemplate.postForObject(
                    "http://localhost:8080/api/v1/tools/check-user-constraints",
                    request,
                    ConstraintCheckResult.class
            );

            if (result != null && !result.isValid()) {
                logger.warn("Skeleton constraint violations:");
                result.getViolations().forEach(v ->
                        logger.warn("  - {}: {}", v.getType(), v.getMessage()));
            }
            return result != null ? result : new ConstraintCheckResult();
        } catch (Exception e) {
            logger.error("Check Constraints tool error: {}", e.getMessage());
            return new ConstraintCheckResult();
        }
    }

    /**
     * Validate LLM schema using the Validate Schema tool.
     */
    private boolean validateSchemaViaTool(String jsonOutput, String jsonSchema) {
        if (!skeletonToolsEnabled) {
            return schemaValidator.validateWithLogging(jsonOutput, jsonSchema, "SkeletonPlannerAgent").isValid();
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
                    schemaValidator.validateWithLogging(jsonOutput, jsonSchema, "SkeletonPlannerAgent").isValid() : false;
        }
    }

    // ========== END SKELETON PLANNER AGENT TOOL INTEGRATION ==========

    @Override
    protected String getAgentName() {
        return "Skeleton Planner Agent";
    }

    /**
     * Clean JSON response by removing markdown formatting and corrupted content.
     * CRITICAL: Preserves Unicode characters for international destinations.
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

        // Remove control characters that actually break JSON
        // Keep all printable Unicode characters (letters, numbers, punctuation)
        cleaned = cleaned.replaceAll("[\\x00-\\x1F\\x7F]", "");

        // Remove any text that appears between JSON properties (malformed content)
        // These patterns handle cases where LLM adds explanatory text between properties
        cleaned = cleaned.replaceAll("}\\s*[^\\s\"{}\\[\\],:]+\\s*,\\s*\"id\"", "},\"id\"");
        cleaned = cleaned.replaceAll("}\\s*[^\\s\"{}\\[\\],:]+\\s*,\\s*\"type\"", "},\"type\"");
        cleaned = cleaned.replaceAll("}\\s*[^\\s\"{}\\[\\],:]+\\s*,\\s*\"title\"", "},\"title\"");
        cleaned = cleaned.replaceAll("}\\s*[^\\s\"{}\\[\\],:]+\\s*,\\s*\"location\"", "},\"location\"");
        cleaned = cleaned.replaceAll("}\\s*[^\\s\"{}\\[\\],:]+\\s*,\\s*\"timing\"", "},\"timing\"");

        // Clean up any remaining malformed content
        cleaned = cleaned.replaceAll("}\\s*[^\\s\"{}\\[\\],:]+\\s*,", "},");

        logger.debug("Cleaned JSON response (Unicode preserved): {} chars", cleaned.length());

        return cleaned.trim();
    }

    /**
     * Normalize time fields in the JSON tree.
     * Converts time strings like "14:00" to milliseconds since epoch.
     * IMPROVED: Now uses destination timezone instead of system timezone.
     */
    private void normalizeTimeFields(com.fasterxml.jackson.databind.JsonNode root, String startDate, int dayNumber) {
        if (root == null || !root.has("nodes") || !root.get("nodes").isArray()) {
            return;
        }

        // Calculate the date for this day
        LocalDate baseDate = LocalDate.parse(startDate);
        LocalDate dayDate = baseDate.plusDays(dayNumber - 1);
        String dayDateStr = dayDate.format(DateTimeFormatter.ISO_LOCAL_DATE);

        // TODO: Get destination timezone from request/itinerary
        // For now, use UTC as a consistent baseline (better than system timezone)
        // Future improvement: Pass destination timezone through the call chain
        java.time.ZoneId timezone = java.time.ZoneId.of("UTC");

        logger.debug("Normalizing time fields for day {} using timezone: {}", dayNumber, timezone);

        // Process each node's timing
        com.fasterxml.jackson.databind.node.ArrayNode nodesArray = (com.fasterxml.jackson.databind.node.ArrayNode) root.get("nodes");
        for (com.fasterxml.jackson.databind.JsonNode node : nodesArray) {
            if (!node.has("timing")) continue;

            com.fasterxml.jackson.databind.node.ObjectNode timing = (com.fasterxml.jackson.databind.node.ObjectNode) node.get("timing");
            if (timing == null) continue;

            // Normalize startTime and endTime with timezone awareness
            normalizeTimeField(timing, "startTime", dayDateStr, timezone);
            normalizeTimeField(timing, "endTime", dayDateStr, timezone);
        }
    }

    /**
     * Normalize a single time field from HH:mm string to milliseconds since epoch.
     * IMPROVED: Now timezone-aware.
     *
     * @param timing   The timing object to update
     * @param field    The field name (startTime or endTime)
     * @param dayDate  The date string in ISO format (YYYY-MM-DD)
     * @param timezone The timezone to use for conversion
     */
    private void normalizeTimeField(com.fasterxml.jackson.databind.node.ObjectNode timing, String field,
                                    String dayDate, java.time.ZoneId timezone) {
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
                // Parse as LocalDateTime in the destination timezone
                java.time.LocalDateTime localDateTime = java.time.LocalDateTime.parse(
                        dayDate + "T" + value + ":00"
                );

                // Convert to ZonedDateTime using destination timezone
                java.time.ZonedDateTime zonedDateTime = localDateTime.atZone(timezone);

                // Convert to epoch milliseconds
                long milliseconds = zonedDateTime.toInstant().toEpochMilli();

                timing.put(field, milliseconds);
                logger.debug("Converted {} '{}' to timestamp {} (timezone: {})",
                        field, value, milliseconds, timezone);
                return;
            }

        } catch (Exception e) {
            logger.warn("Failed to normalize time field {} with value {}: {}", field, value, e.getMessage());
            // If parsing fails, set to null
            timing.putNull(field);
        }
    }

    /**
     * Generate skeleton days for a specific city group (for parallel processing).
     * This method generates days sequentially within the same city to preserve context.
     *
     * @param itineraryId The itinerary ID
     * @param request     The itinerary request
     * @param cityPlan    The city allocation plan
     * @param cityName    The city name for this group
     * @param dayNumbers  The day numbers to generate for this city
     * @return List of generated days
     */
    public List<NormalizedDay> generateCityGroupDays(
            String itineraryId,
            CreateItineraryReq request,
            CityAllocationPlan cityPlan,
            String cityName,
            List<Integer> dayNumbers) {

        logger.info("Generating {} days for city: {}", dayNumbers.size(), cityName);

        List<NormalizedDay> generatedDays = new ArrayList<>();
        List<NormalizedDay> previousDaysInCity = new ArrayList<>();

        for (int dayNumber : dayNumbers) {
            // Find city allocation and travel segment
            CityAllocation cityForThisDay = getCityForDay(cityPlan, dayNumber);
            TravelSegment travelSegment = getTravelSegmentForDay(cityPlan, dayNumber);

            // Pre-create infrastructure nodes
            List<NormalizedNode> preCreatedNodes = createPreCreatedNodes(
                    dayNumber, request, cityForThisDay, travelSegment, previousDaysInCity);

            // Generate skeleton with previous days context (within same city)
            NormalizedDay day = generateDaySkeleton(request, dayNumber, cityForThisDay,
                    travelSegment, previousDaysInCity, preCreatedNodes);

            generatedDays.add(day);
            previousDaysInCity.add(day); // Context for next day in same city

            logger.info("Generated Day {} for {} (nodes: {})",
                    dayNumber, cityName, day.getNodes() != null ? day.getNodes().size() : 0);
        }

        return generatedDays;
    }

    /**
     * Helper method to create pre-created infrastructure nodes.
     */
    private List<NormalizedNode> createPreCreatedNodes(
            int dayNumber,
            CreateItineraryReq request,
            CityAllocation cityForThisDay,
            TravelSegment travelSegment,
            List<NormalizedDay> previousDays) {

        List<NormalizedNode> preCreatedNodes = new ArrayList<>();

        // Create temporary itinerary for ID generation
        NormalizedItinerary tempItinerary = new NormalizedItinerary();
        tempItinerary.setDays(new ArrayList<>(previousDays));

        // Add arrival travel for Day 1
        if (dayNumber == 1 && shouldAddArrivalTravel(request, cityForThisDay)) {
            NormalizedNode arrivalNode = createArrivalTravelNode(dayNumber, request, cityForThisDay);
            arrivalNode.setId(nodeIdGenerator.generateNodeId("transport", dayNumber, tempItinerary));
            preCreatedNodes.add(arrivalNode);
        }

        // Add inter-city travel
        if (travelSegment != null) {
            NormalizedNode travelNode = createTravelNode(dayNumber, travelSegment);
            travelNode.setId(nodeIdGenerator.generateNodeId("transport", dayNumber, tempItinerary));
            preCreatedNodes.add(travelNode);
        }

        // Add departure travel for last day
        int totalDays = request.getDurationDays();
        if (dayNumber == totalDays && shouldAddDepartureTravel(request, cityForThisDay)) {
            NormalizedNode departureNode = createDepartureTravelNode(dayNumber, request, cityForThisDay);
            departureNode.setId(nodeIdGenerator.generateNodeId("transport", dayNumber, tempItinerary));
            preCreatedNodes.add(departureNode);
        }

        return preCreatedNodes;
    }
}
