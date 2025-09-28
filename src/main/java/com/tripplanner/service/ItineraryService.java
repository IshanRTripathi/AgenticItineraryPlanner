package com.tripplanner.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.tripplanner.agents.AgentOrchestrator;
import com.tripplanner.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Service for itinerary operations using Firestore-backed normalized JSON only.
 */
@Service
public class ItineraryService {
    
    private static final Logger logger = LoggerFactory.getLogger(ItineraryService.class);
    
    private final AgentOrchestrator agentOrchestrator;
    private final ItineraryJsonService itineraryJsonService;
    private final UserDataService userDataService;
    
    public ItineraryService(AgentOrchestrator agentOrchestrator,
                            ItineraryJsonService itineraryJsonService,
                            UserDataService userDataService) {
        this.agentOrchestrator = agentOrchestrator;
        this.itineraryJsonService = itineraryJsonService;
        this.userDataService = userDataService;
    }
    
    /**
     * Create a new itinerary for a specific user.
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
            
            // Generate the itinerary using AgentOrchestrator
            agentOrchestrator.generateNormalizedItinerary(itineraryId, request, userId);
            
            // Save the itinerary to user-specific storage
            // Note: The actual itinerary data will be saved by AgentOrchestrator
            // We just need to ensure it's associated with the user
            logger.info("Itinerary generation started for user: {} with ID: {}", userId, itineraryId);

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
                    .summary("Your personalized itinerary for " + request.getDestination())
                    .status("generating")
                    .build();

            logger.info("=== CREATE ITINERARY RESPONSE ===");
            logger.info("User ID: {}", userId);
            logger.info("Itinerary ID: {}", result.getId());
            logger.info("Status: {}", result.getStatus());
            logger.info("Orchestration started: {}", itineraryId);
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
            ItineraryDto result = ItineraryDto.builder()
                    .id(ni.getItineraryId())
                    .summary(ni.getSummary())
                    .status("completed")
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

                        return ItineraryDto.builder()
                                .id(metadata.getItineraryId())
                                .destination(destination)
                                .startDate(startDate)
                                .endDate(endDate)
                                .language(metadata.getLanguage() != null ? metadata.getLanguage() : "en")
                                .summary(metadata.getSummary())
                                .interests(metadata.getInterests())
                                .status("completed")
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
                "EUR",
                "person"
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
                "EUR",
                "night"
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
                "EUR",
                "person"
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
                "EUR",
                "trip"
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
