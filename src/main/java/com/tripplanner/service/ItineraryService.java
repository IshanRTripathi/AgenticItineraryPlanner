package com.tripplanner.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.tripplanner.dto.*;
import com.tripplanner.dto.ErrorEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Service for itinerary operations using Firestore-backed normalized JSON only.
 * 
 * This service now uses only the pipeline flow for itinerary generation.
 * The monolithic flow has been removed as part of the dual-flow migration (2025-10-18).
 */
@Service
public class ItineraryService {
    
    private static final Logger logger = LoggerFactory.getLogger(ItineraryService.class);
    
    private final ItineraryInitializationService initService;
    private final ItineraryJsonService itineraryJsonService;
    private final UserDataService userDataService;
    private final PipelineOrchestrator pipelineOrchestrator;
    private final AgentEventPublisher agentEventPublisher;
    private final ItineraryMigrationService migrationService;
    private final WebSocketBroadcastService webSocketBroadcastService;
    
    public ItineraryService(ItineraryInitializationService initService,
                            ItineraryJsonService itineraryJsonService,
                            UserDataService userDataService,
                            PipelineOrchestrator pipelineOrchestrator,
                            AgentEventPublisher agentEventPublisher,
                            ItineraryMigrationService migrationService,
                            WebSocketBroadcastService webSocketBroadcastService) {
        this.initService = initService;
        this.itineraryJsonService = itineraryJsonService;
        this.userDataService = userDataService;
        this.pipelineOrchestrator = pipelineOrchestrator;
        this.agentEventPublisher = agentEventPublisher;
        this.migrationService = migrationService;
        this.webSocketBroadcastService = webSocketBroadcastService;
    }
    
    /**
     * Create a new itinerary for a specific user.
     * 
     * <p>This method creates an initial itinerary synchronously to establish ownership,
     * then starts async generation in the background. Progress events are published
     * via SSE to connected clients.</p>
     * 
     * <p>The method returns immediately with status="generating". Clients should
     * establish an SSE connection to receive real-time progress updates.</p>
     * 
     * <p>Error handling: If async generation fails, an error event is published
     * to SSE subscribers. The error is logged but does not affect the API response.</p>
     * 
     * @param request The itinerary creation request with destination, dates, preferences
     * @param userId The authenticated user ID from Firebase token
     * @return ItineraryDto with status="generating" and initial metadata
     * @throws RuntimeException if initial itinerary creation fails
     * @throws IllegalStateException if no orchestrator is available for the configured mode
     */
    public ItineraryDto create(CreateItineraryReq request, String userId) {
        logger.info("=== CREATE ITINERARY REQUEST ===");
        logger.info("Request Details:");
        logger.info("  Destination: {}", request.getDestination());
        logger.info("  Start Date: {}", request.getStartDate());
        logger.info("  End Date: {}", request.getEndDate());
        logger.info("  Duration: {} days", request.getDurationDays());
        logger.info("  Budget Tier: {}", request.getBudgetTier());
        logger.info("  Language: {}", request.getLanguage());
        if (request.getParty() != null) {
            logger.info("  Party: {} adults, {} children, {} infants, {} rooms", 
                       request.getParty().getAdults(), request.getParty().getChildren(), 
                       request.getParty().getInfants(), request.getParty().getRooms());
        }
        if (request.getInterests() != null && !request.getInterests().isEmpty()) {
            logger.info("  Interests: {}", String.join(", ", request.getInterests()));
        }
        if (request.getConstraints() != null && !request.getConstraints().isEmpty()) {
            logger.info("  Constraints: {}", String.join(", ", request.getConstraints()));
        }
        
        try {
            String itineraryId = "it_" + java.util.UUID.randomUUID();
            
            // SYNCHRONOUSLY create initial itinerary and establish ownership
            // This ensures the user can immediately access the itinerary endpoint
            logger.info("Creating initial itinerary and establishing ownership synchronously");
            NormalizedItinerary initialItinerary = initService.createInitialItinerary(itineraryId, request, userId);
            
            // Convert the initial itinerary to DTO for the API response
            ItineraryDto result = ItineraryDto.builder()
                    .id(itineraryId)
                    .destination(request.getDestination())
                    .startDate(request.getStartDate())
                    .endDate(request.getEndDate())
                    .party(request.getParty())
                    .budgetTier(request.getBudgetTier())
                    .interests(request.getInterests())
                    .constraints(request.getConstraints())
                    .language(request.getLanguage())
                    .summary(initialItinerary.getSummary())
                    .status("generating")
                    .build();
            
            // NOW start async agent processing (after ownership is established)
            // Add a small delay to allow frontend SSE connection to establish
            logger.info("Waiting 2 seconds for frontend SSE connection to establish...");
            try {
                Thread.sleep(2000); // 2 second delay
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("Sleep interrupted while waiting for SSE connection");
            }
            
            logger.info("Starting async itinerary generation for user: {} with ID: {}", userId, itineraryId);
            logger.info("Starting pipeline generation");
            
            // Generate unique execution ID for tracking this generation attempt
            String executionId = "exec_" + System.currentTimeMillis();
            logger.info("Generated executionId: {} for itinerary: {}", executionId, itineraryId);
            
            // Start pipeline generation (always use pipeline flow)
            CompletableFuture<NormalizedItinerary> future = 
                pipelineOrchestrator.generateItinerary(itineraryId, request, userId);
            
            // Attach completion callback for error handling and tracking
            future.whenComplete((pipelineResult, throwable) -> {
                if (throwable != null) {
                    // Log the error with full context
                    logger.error("Pipeline generation failed for itinerary: {}, executionId: {}", 
                        itineraryId, executionId, throwable);
                    
                    // Publish error event to SSE subscribers so UI can show error message
                    try {
                        agentEventPublisher.publishErrorFromException(
                            itineraryId,
                            executionId,
                            (Exception) throwable,
                            "itinerary generation",
                            ErrorEvent.ErrorSeverity.ERROR
                        );
                        logger.info("Error event published for itinerary: {}, executionId: {}", 
                            itineraryId, executionId);
                    } catch (Exception e) {
                        // Don't let event publishing failures affect the main flow
                        logger.error("Failed to publish error event for itinerary: {}, executionId: {}", 
                            itineraryId, executionId, e);
                    }
                } else {
                    // Log successful completion
                    logger.info("Pipeline generation completed successfully for itinerary: {}, executionId: {}", 
                        itineraryId, executionId);
                    // Note: Completion event is already published by PipelineOrchestrator.publishPipelineComplete()
                    
                    // Send WebSocket update to notify frontend of completion
                    try {
                        logger.info("Sending WebSocket update for completed itinerary: {}", itineraryId);
                        // Get the completed itinerary
                        var completedItinerary = itineraryJsonService.getItinerary(itineraryId);
                        if (completedItinerary.isPresent()) {
                            // Broadcast itinerary update via WebSocket
                            webSocketBroadcastService.broadcastUpdate(
                                itineraryId, 
                                "itinerary_updated", 
                                completedItinerary.get(),
                                userId
                            );
                            logger.info("WebSocket update sent successfully for itinerary: {}", itineraryId);
                        } else {
                            logger.warn("Could not find completed itinerary to broadcast: {}", itineraryId);
                        }
                    } catch (Exception e) {
                        logger.error("Failed to send WebSocket update for itinerary: {}", itineraryId, e);
                    }
                }
            });

            logger.info("=== CREATE ITINERARY RESPONSE ===");
            logger.info("User ID: {}", userId);
            logger.info("Itinerary ID: {}", result.getId());
            logger.info("ExecutionId: {}", executionId);
            logger.info("Status: {}", result.getStatus());
            logger.info("Ownership established: YES");
            logger.info("Async generation started: YES");
            logger.info("Async callbacks attached: YES");
            logger.info("Error handling enabled: YES");
            logger.info("User can now access /itineraries/{}/json", itineraryId);
            logger.info("=====================================");

            return result;

        } catch (Exception e) {
            logger.error("=== CREATE ITINERARY FAILED ===");
            logger.error("User: {}", userId);
            logger.error("Destination: {}", request.getDestination());
            logger.error("Error: {}", e.getMessage(), e);
            logger.error("===================================");
            throw new RuntimeException("Failed to create itinerary", e);
        }
    }
    
    /**
     * Get an itinerary by ID for a specific user.
     */
    public ItineraryDto get(String id, String userId) {
        logger.info("=== GET ITINERARY REQUEST ===");
        logger.info("Itinerary ID: {}", id);
        
        try {
            // First check if user owns this trip
            if (!userDataService.userOwnsTrip(userId, id)) {
                throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.NOT_FOUND,
                    "Itinerary not found for user: " + userId
                );
            }
            
            // Get itinerary from ItineraryJsonService (single source of truth)
            var niOpt = itineraryJsonService.getItinerary(id);
            if (niOpt.isEmpty()) {
                throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.NOT_FOUND,
                    "Itinerary not found: " + id
                );
            }
            NormalizedItinerary ni = niOpt.get();
            
            // Automatically migrate if needed
            ni = migrationService.migrateIfNeeded(ni);
            logger.debug("Loaded itinerary {} (version {})", id, ni.getVersion());
            
            // Calculate actual status instead of hardcoding "completed"
            String actualStatus = calculateItineraryStatus(ni);
            
            ItineraryDto result = ItineraryDto.builder()
                    .id(ni.getItineraryId())
                    .summary(ni.getSummary())
                    .status(actualStatus)
                    .build();

            logger.info("=== GET ITINERARY RESPONSE ===");
            logger.info("User ID: {}", userId);
            logger.info("Found itinerary: {}", id);
            logger.info("Summary: {}", result.getSummary());
            logger.info("==================================");

            return result;

        } catch (Exception e) {
            logger.error("=== GET ITINERARY FAILED ===");
            logger.error("Itinerary ID: {}", id);
            logger.error("User: {}", userId);
            logger.error("Error: {}", e.getMessage(), e);
            logger.error("===============================");
            throw new RuntimeException("Failed to get itinerary", e);
        }
    }
    
    /**
     * Get public itinerary (no authentication required).
     */
    public ItineraryDto getPublic(String id) {
        logger.debug("Getting public itinerary: {}", id);
        
        try {
            throw new UnsupportedOperationException("Public itineraries not supported in Firestore-only mode");
        } catch (Exception e) {
            logger.error("Failed to get public itinerary: " + id, e);
            throw new RuntimeException("Failed to get public itinerary", e);
        }
    }
    
    /**
     * Save an itinerary (mark as saved/favorite).
     */
    public void save(String id) {
        logger.info("Saving itinerary: {}", id);
        
        try {
            // No-op in Firestore-only mode
            logger.info("Itinerary saved (no-op): {}", id);
        } catch (Exception e) {
            logger.error("Failed to save itinerary: " + id, e);
            throw new RuntimeException("Failed to save itinerary", e);
        }
    }
    
    /**
     * Get user's itineraries.
     */
    public List<ItineraryDto> getUserItineraries(String userId, int page, int size) {
        logger.debug("Getting itineraries");
        
        try {
            // Get trip metadata from UserDataService
            List<TripMetadata> tripMetadataList = userDataService.getUserTripMetadata(userId);
            
            return tripMetadataList.stream()
                    .map(metadata -> {
                        String destination = metadata.getDestination();
                        if (destination == null || destination.isBlank()) {
                            destination = "Unknown Destination";
                        }

                        LocalDate startDate = null;
                        if (metadata.getStartDate() != null && !metadata.getStartDate().isBlank()) {
                            try { startDate = LocalDate.parse(metadata.getStartDate()); } catch (Exception ignored) {}
                        }

                        LocalDate endDate = null;
                        if (metadata.getEndDate() != null && !metadata.getEndDate().isBlank()) {
                            try { endDate = LocalDate.parse(metadata.getEndDate()); } catch (Exception ignored) {}
                        }

                        // Get actual status from the itinerary instead of hardcoding "completed"
                        // Edge case: Use metadata status if available, otherwise fetch from itinerary
                        String actualStatus = "completed"; // Safe default for list view
                        try {
                            // Check if metadata has status (optimization to avoid extra fetch)
                            if (metadata.getStatus() != null && !metadata.getStatus().isBlank()) {
                                actualStatus = metadata.getStatus();
                            } else {
                                // Fallback: fetch full itinerary to calculate status
                                Optional<NormalizedItinerary> itinOpt = itineraryJsonService.getItinerary(metadata.getItineraryId());
                                if (itinOpt.isPresent()) {
                                    actualStatus = calculateItineraryStatus(itinOpt.get());
                                }
                            }
                        } catch (Exception e) {
                            logger.warn("Failed to get status for itinerary {}: {}", metadata.getItineraryId(), e.getMessage());
                            // Keep default "completed" on error to avoid breaking UI
                        }

                        return ItineraryDto.builder()
                                .id(metadata.getItineraryId())
                                .destination(destination)
                                .startDate(startDate)
                                .endDate(endDate)
                                .language(metadata.getLanguage() != null ? metadata.getLanguage() : "en")
                                .summary(metadata.getSummary())
                                .interests(metadata.getInterests())
                                .status(actualStatus)
                                .build();
                    })
                    .toList();
        } catch (Exception e) {
            logger.error("Failed to get user itineraries", e);
            throw new RuntimeException("Failed to get user itineraries", e);
        }
    }
    
    /**
     * Delete an itinerary for a specific user.
     */
    public void delete(String id, String userId) {
        logger.info("Deleting itinerary: {} for user: {}", id, userId);
        
        try {
            // Delete from both services
            userDataService.deleteUserTripMetadata(userId, id);
            itineraryJsonService.deleteItinerary(id);
            logger.info("Itinerary deleted: {} for user: {}", id, userId);
        } catch (Exception e) {
            logger.error("Failed to delete itinerary: {} for user: {}", id, userId, e);
            throw new RuntimeException("Failed to delete itinerary", e);
        }
    }
    
    /**
     * Calculate the current status of an itinerary based on agent states and content.
     * 
     * Priority-based logic:
     * 1. If any agent is running → "generating"
     * 2. If days exist with components → "completed"
     * 3. If all agents are idle with no days → "planning"
     * 4. Default → "planning" (safe fallback)
     * 
     * @param itinerary The normalized itinerary to check
     * @return Status string: "planning", "generating", "completed", or "failed"
     */
    public String calculateItineraryStatus(NormalizedItinerary itinerary) {
        // Edge case: null itinerary
        if (itinerary == null) {
            logger.warn("calculateItineraryStatus called with null itinerary");
            return "planning";
        }
        
        // Edge case: Check explicit status field first (set by pipeline completion)
        if (itinerary.getStatus() != null && !itinerary.getStatus().isBlank()) {
            String explicitStatus = itinerary.getStatus().toLowerCase();
            // Trust explicit status if it's a valid value
            if (explicitStatus.equals("completed") || 
                explicitStatus.equals("generating") || 
                explicitStatus.equals("failed")) {
                return explicitStatus;
            }
        }
        
        // Priority 1: Check for running agents
        if (itinerary.getAgents() != null && !itinerary.getAgents().isEmpty()) {
            boolean anyRunning = itinerary.getAgents().values().stream()
                .filter(agent -> agent != null && agent.getStatus() != null)
                .anyMatch(agent -> "running".equals(agent.getStatus()));
            if (anyRunning) {
                return "generating";
            }
        }
        
        // Priority 2: Check for completed content
        if (itinerary.getDays() != null && !itinerary.getDays().isEmpty()) {
            boolean hasContent = itinerary.getDays().stream()
                .filter(day -> day != null)
                .anyMatch(day -> day.getNodes() != null && !day.getNodes().isEmpty());
            if (hasContent) {
                return "completed";
            }
        }
        
        // Priority 3: Check for all idle agents with no content
        if (itinerary.getAgents() != null && !itinerary.getAgents().isEmpty()) {
            boolean allIdle = itinerary.getAgents().values().stream()
                .filter(agent -> agent != null && agent.getStatus() != null)
                .allMatch(agent -> "idle".equals(agent.getStatus()));
            if (allIdle && (itinerary.getDays() == null || itinerary.getDays().isEmpty())) {
                return "planning";
            }
        }
        
        // Default fallback
        return "planning";
    }
    
    /**
     * Share an itinerary (make it public).
     */
    public ShareResponse share(String id) {
        logger.info("Sharing itinerary: {}", id);
        
        try {
            // Not supported in Firestore-only mode (no public sharing implemented)
            String shareToken = UUID.randomUUID().toString();
            String publicUrl = "/itineraries/" + id + "/json";
            logger.info("Itinerary shared (token only, no ACL): {} -> {}", id, shareToken);
            return new ShareResponse(shareToken, publicUrl);
        } catch (Exception e) {
            logger.error("Failed to share itinerary: " + id, e);
            throw new RuntimeException("Failed to share itinerary", e);
        }
    }
    
    /**
     * Unshare an itinerary (make it private).
     */
    public void unshare(String id) {
        logger.info("Unsharing itinerary: {}", id);
        
        try {
            // No-op in Firestore-only mode
            logger.info("Itinerary unshared (no-op): {}", id);
        } catch (Exception e) {
            logger.error("Failed to unshare itinerary: " + id, e);
            throw new RuntimeException("Failed to unshare itinerary", e);
        }
    }

    /**
     * Load the most recent Gemini response JSON.
     */
    private String loadLatestGeminiResponse() {
        try {
            // Use the Barcelona response as a template
            java.nio.file.Path responsePath = java.nio.file.Paths.get("logs/gemini-responses/barcelona_3day_family.json");
            if (java.nio.file.Files.exists(responsePath)) {
                return java.nio.file.Files.readString(responsePath);
            }
        } catch (Exception e) {
            logger.warn("Failed to load Gemini response: {}", e.getMessage());
        }
        return null;
    }
    
    /**
     * Create a complete itinerary from AI response JSON.
     */
    
    /**
     * Create a day DTO from AI response JSON.
     */
    private ItineraryDayDto createDayFromAIResponse(JsonNode dayNode) {
        // Create activities
        java.util.List<ActivityDto> activities = new java.util.ArrayList<>();
        if (dayNode.has("activities")) {
            for (JsonNode activityNode : dayNode.get("activities")) {
                ActivityDto activity = createActivityFromAIResponse(activityNode);
                activities.add(activity);
            }
        }
        
        // Create accommodation
        AccommodationDto accommodation = null;
        if (dayNode.has("accommodation")) {
            accommodation = createAccommodationFromAIResponse(dayNode.get("accommodation"));
        }
        
        // Create meals
        java.util.List<MealDto> meals = new java.util.ArrayList<>();
        if (dayNode.has("meals")) {
            for (JsonNode mealNode : dayNode.get("meals")) {
                MealDto meal = createMealFromAIResponse(mealNode);
                meals.add(meal);
            }
        }
        
        // Create transportation
        java.util.List<TransportationDto> transportation = new java.util.ArrayList<>();
        if (dayNode.has("transportation")) {
            for (JsonNode transportNode : dayNode.get("transportation")) {
                TransportationDto transport = createTransportationFromAIResponse(transportNode);
                transportation.add(transport);
            }
        }
        
        return ItineraryDayDto.builder()
                .day(dayNode.get("dayNumber").asInt())
                .date(java.time.LocalDate.parse(dayNode.get("date").asText()))
                .location(dayNode.get("location").asText())
                .notes(dayNode.has("notes") ? dayNode.get("notes").asText() : "")
                .activities(activities)
                .accommodation(accommodation)
                .meals(meals)
                .transportation(transportation)
                .build();
    }
    
    /**
     * Create activity DTO from AI response JSON.
     */
    private ActivityDto createActivityFromAIResponse(JsonNode activityNode) {
        LocationDto location = null;
        if (activityNode.has("location")) {
            JsonNode locNode = activityNode.get("location");
            location = new LocationDto(
                locNode.get("name").asText(),
                locNode.get("address").asText(),
                locNode.get("lat").asDouble(),
                locNode.get("lng").asDouble(),
                null // placeId
            );
        }
        
        PriceDto price = null;
        if (activityNode.has("estimatedCost")) {
            price = new PriceDto(
                activityNode.get("estimatedCost").asDouble(),
                "EUR"
            );
        }
        
        return new ActivityDto(
            activityNode.get("name").asText(),
            activityNode.get("description").asText(),
            location,
            activityNode.get("startTime").asText(),
            activityNode.get("endTime").asText(),
            activityNode.get("duration").asText(),
            activityNode.get("category").asText(),
            price,
            activityNode.has("bookingRequired") ? activityNode.get("bookingRequired").asBoolean() : false,
            null, // bookingUrl
            activityNode.has("tips") ? activityNode.get("tips").asText() : null
        );
    }
    
    /**
     * Create accommodation DTO from AI response JSON.
     */
    private AccommodationDto createAccommodationFromAIResponse(JsonNode accNode) {
        LocationDto location = null;
        if (accNode.has("location")) {
            JsonNode locNode = accNode.get("location");
            location = new LocationDto(
                locNode.get("name").asText(),
                locNode.get("address").asText(),
                locNode.get("lat").asDouble(),
                locNode.get("lng").asDouble(),
                null // placeId
            );
        }
        
        PriceDto price = null;
        if (accNode.has("estimatedCost")) {
            price = new PriceDto(
                accNode.get("estimatedCost").asDouble(),
                "EUR"
            );
        }
        
        return new AccommodationDto(
            accNode.get("name").asText(),
            accNode.get("type").asText(),
            location,
            null, // checkIn
            null, // checkOut
            price,
            accNode.has("rating") ? accNode.get("rating").asDouble() : 0.0,
            null, // amenities
            null  // bookingUrl
        );
    }
    
    /**
     * Create meal DTO from AI response JSON.
     */
    private MealDto createMealFromAIResponse(JsonNode mealNode) {
        LocationDto location = null;
        if (mealNode.has("location")) {
            JsonNode locNode = mealNode.get("location");
            location = new LocationDto(
                locNode.get("name").asText(),
                locNode.get("address").asText(),
                locNode.get("lat").asDouble(),
                locNode.get("lng").asDouble(),
                null // placeId
            );
        }
        
        PriceDto price = null;
        if (mealNode.has("estimatedCost")) {
            price = new PriceDto(
                mealNode.get("estimatedCost").asDouble(),
                "EUR"
            );
        }
        
        return new MealDto(
            mealNode.get("type").asText(),
            mealNode.get("name").asText(),
            mealNode.get("restaurant").asText(),
            location,
            null, // time
            price,
            mealNode.get("cuisine").asText(),
            null  // notes
        );
    }
    
    /**
     * Create transportation DTO from AI response JSON.
     */
    private TransportationDto createTransportationFromAIResponse(JsonNode transportNode) {
        LocationDto fromLocation = new LocationDto(
            transportNode.get("from").asText(),
            null, // address
            0.0,  // lat
            0.0,  // lng
            null  // placeId
        );
        
        LocationDto toLocation = new LocationDto(
            transportNode.get("to").asText(),
            null, // address
            0.0,  // lat
            0.0,  // lng
            null  // placeId
        );
        
        PriceDto price = null;
        if (transportNode.has("estimatedCost")) {
            price = new PriceDto(
                transportNode.get("estimatedCost").asDouble(),
                "EUR"
            );
        }
        
        return new TransportationDto(
            transportNode.get("mode").asText(),
            fromLocation,
            toLocation,
            transportNode.get("departureTime").asText(),
            transportNode.get("arrivalTime").asText(),
            null, // duration
            price,
            null, // provider
            null, // bookingUrl
            transportNode.has("notes") ? transportNode.get("notes").asText() : null
        );
    }
}
