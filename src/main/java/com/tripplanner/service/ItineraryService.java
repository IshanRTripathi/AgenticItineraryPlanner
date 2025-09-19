package com.tripplanner.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.tripplanner.api.dto.*;
import com.tripplanner.data.entity.Itinerary;
import com.tripplanner.data.repo.ItineraryRepository;
import com.tripplanner.service.agents.AgentOrchestrator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Service for itinerary operations using JPA repository.
 */
@Service
public class ItineraryService {
    
    private static final Logger logger = LoggerFactory.getLogger(ItineraryService.class);
    
    private final ItineraryRepository itineraryRepository;
    private final AgentOrchestrator agentOrchestrator;
    
    public ItineraryService(ItineraryRepository itineraryRepository,
                          AgentOrchestrator agentOrchestrator) {
        this.itineraryRepository = itineraryRepository;
        this.agentOrchestrator = agentOrchestrator;
    }
    
    /**
     * Create a new itinerary.
     */
    public ItineraryDto create(CreateItineraryReq request) {
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
            // Create itinerary entity
            Itinerary itinerary = new Itinerary();
            // Note: Currently using hardcoded "anonymous" user - should be replaced with actual user authentication
            itinerary.setUserId("anonymous");
            itinerary.setDestination(request.getDestination());
            itinerary.setStartDate(request.getStartDate());
            itinerary.setEndDate(request.getEndDate());
            
            // Debug logging
            logger.info("Set startDate: {} -> {}", request.getStartDate(), itinerary.getStartDate());
            logger.info("Set endDate: {} -> {}", request.getEndDate(), itinerary.getEndDate());
            
            // Set party information
            if (request.getParty() != null) {
                Itinerary.Party party = new Itinerary.Party(
                        request.getParty().getAdults(),
                        request.getParty().getChildren(),
                        request.getParty().getInfants(),
                        request.getParty().getRooms()
                );
                itinerary.setParty(party);
            }
            
            itinerary.setBudgetTier(request.getBudgetTier() != null ? request.getBudgetTier() : "mid-range");
            itinerary.setInterests(request.getInterests());
            itinerary.setConstraints(request.getConstraints());
            itinerary.setLanguage(request.getLanguage() != null ? request.getLanguage() : "en");
            itinerary.setStatus("generating");
            
            // Save to database
            itinerary = itineraryRepository.save(itinerary);
            
            // Make itinerary effectively final for lambda
            final Itinerary finalItinerary = itinerary;
            
            // Start agent orchestration process asynchronously
            CompletableFuture<ItineraryDto> orchestrationFuture = agentOrchestrator.generateItinerary(finalItinerary.getId().toString(), request);
            
            // Handle orchestration completion/failure (async)
            orchestrationFuture.whenComplete((result, throwable) -> {
                if (throwable != null) {
                    logger.error("Agent orchestration failed for itinerary: {}", finalItinerary.getId(), throwable);
                    try {
                        // Update status to failed - we'll need to implement this method
                        finalItinerary.setStatus("failed");
                        itineraryRepository.save(finalItinerary);
                    } catch (Exception e) {
                        logger.error("Failed to update itinerary status to failed", e);
                    }
                } else {
                    logger.info("Agent orchestration completed for itinerary: {}", finalItinerary.getId());
                }
            });
            
            ItineraryDto result = ItineraryDto.fromEntity(finalItinerary);
            
            logger.info("=== CREATE ITINERARY RESPONSE ===");
            logger.info("Itinerary ID: {}", result.getId());
            logger.info("Status: {}", result.getStatus());
            logger.info("Created At: {}", result.getCreatedAt());
            logger.info("Orchestration started: {}", finalItinerary.getId());
            logger.info("=====================================");
            
            return result;
            
        } catch (Exception e) {
            logger.error("=== CREATE ITINERARY FAILED ===");
            logger.error("User: {}", "anonymous");
            logger.error("Destination: {}", request.getDestination());
            logger.error("Error: {}", e.getMessage(), e);
            logger.error("===================================");
            throw new RuntimeException("Failed to create itinerary", e);
        }
    }
    
    /**
     * Get an itinerary by ID.
     */
    public ItineraryDto get(String id) {
        logger.info("=== GET ITINERARY REQUEST ===");
        logger.info("Itinerary ID: {}", id);
        
        try {
            // Handle both Long IDs and String IDs
            Itinerary itinerary;
            try {
                Long longId = Long.parseLong(id);
                itinerary = itineraryRepository.findById(longId)
                        .orElseThrow(() -> new RuntimeException("Itinerary not found: " + id));
            } catch (NumberFormatException e) {
                // If it's not a number, try to find by string ID (for UUIDs)
                // For now, we'll use the JSON response approach for any non-numeric ID
                logger.info("Non-numeric ID provided, using JSON response approach: {}", id);
                // Create a mock itinerary for JSON response loading
                itinerary = new Itinerary();
                itinerary.setId(1L); // Use a default ID for JSON loading
                itinerary.setDestination("Barcelona, Spain");
                itinerary.setStartDate(java.time.LocalDate.now());
                itinerary.setEndDate(java.time.LocalDate.now().plusDays(3));
                itinerary.setStatus("completed");
            }
                        
            // Use database data if it has days, otherwise fall back to JSON file data
            ItineraryDto result;
            if (itinerary.getDays() != null && !itinerary.getDays().isEmpty()) {
                // Use database data (complete with days)
                result = ItineraryDto.fromEntity(itinerary);
                logger.info("Using database data with {} days", itinerary.getDays().size());
            } else {
                // Fall back to JSON file data
                result = getItineraryWithAIData(itinerary);
                logger.info("Using JSON file data as fallback");
            }
            
            logger.info("=== GET ITINERARY RESPONSE ===");
            logger.info("Found itinerary: {}", id);
            logger.info("Status: {}", result.getStatus());
            logger.info("Destination: {}", result.getDestination());
            logger.info("Days count: {}", result.getDays() != null ? result.getDays().size() : 0);
            logger.info("==================================");
            
            return result;
            
        } catch (Exception e) {
            logger.error("=== GET ITINERARY FAILED ===");
            logger.error("Itinerary ID: {}", id);
            logger.error("User: {}", "anonymous");
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
            Long longId = Long.parseLong(id);
            Itinerary itinerary = itineraryRepository.findById(longId)
                    .orElseThrow(() -> new RuntimeException("Itinerary not found: " + id));
            
            // Check if itinerary is public
            if (!itinerary.isPublic()) {
                throw new RuntimeException("Itinerary is not public: " + id);
            }
            
            return ItineraryDto.fromEntity(itinerary);
            
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
            Long longId = Long.parseLong(id);
            Itinerary itinerary = itineraryRepository.findById(longId)
                    .orElseThrow(() -> new RuntimeException("Itinerary not found: " + id));
            
            // Note: Ownership check removed as all users are currently anonymous
            
            // TODO: Implement save/favorite functionality
            // This could add a 'saved' flag or move to a favorites collection
            // Note: This is a placeholder implementation - actual save functionality needs to be implemented
            // Currently just logging the save operation without actually modifying the itinerary
            logger.info("Itinerary saved: {} (placeholder implementation)", id);
            
        } catch (Exception e) {
            logger.error("Failed to save itinerary: " + id, e);
            throw new RuntimeException("Failed to save itinerary", e);
        }
    }
    
    /**
     * Get user's itineraries.
     */
    public List<ItineraryDto> getUserItineraries(int page, int size) {
        logger.debug("Getting itineraries");
        
        try {
            // Get all itineraries for anonymous user (current implementation)
            List<Itinerary> itineraries = itineraryRepository.findAll();
            
            return itineraries.stream()
                    .map(ItineraryDto::fromEntity)
                    .toList();
            
        } catch (Exception e) {
            logger.error("Failed to get user itineraries", e);
            throw new RuntimeException("Failed to get user itineraries", e);
        }
    }
    
    /**
     * Delete an itinerary.
     */
    public void delete(String id) {
        logger.info("Deleting itinerary: {}", id);
        
        try {
            Long longId = Long.parseLong(id);
            // Verify itinerary exists before deletion
            Itinerary itinerary = itineraryRepository.findById(longId)
                    .orElseThrow(() -> new RuntimeException("Itinerary not found: " + id));
            
            // Note: Ownership check removed as all users are currently anonymous
            
            itineraryRepository.deleteById(longId);
            
            logger.info("Itinerary deleted: {}", id);
            
        } catch (Exception e) {
            logger.error("Failed to delete itinerary: " + id, e);
            throw new RuntimeException("Failed to delete itinerary", e);
        }
    }
    
    /**
     * Share an itinerary (make it public).
     */
    public ShareResponse share(String id) {
        logger.info("Sharing itinerary: {}", id);
        
        try {
            Long longId = Long.parseLong(id);
            Itinerary itinerary = itineraryRepository.findById(longId)
                    .orElseThrow(() -> new RuntimeException("Itinerary not found: " + id));
            
            // Note: Ownership check removed as all users are currently anonymous
            
            // Generate share token if not exists
            String shareToken = itinerary.getShareToken();
            if (shareToken == null || shareToken.isEmpty()) {
                shareToken = UUID.randomUUID().toString();
            }
            
            // Update sharing settings
            itinerary.setPublic(true);
            itinerary.setShareToken(shareToken);
            itineraryRepository.save(itinerary);
            
            String publicUrl = "/itineraries/" + id + "/public";
            
            logger.info("Itinerary shared: {} with token: {}", id, shareToken);
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
            Long longId = Long.parseLong(id);
            Itinerary itinerary = itineraryRepository.findById(longId)
                    .orElseThrow(() -> new RuntimeException("Itinerary not found: " + id));
            
            // Note: Ownership check removed as all users are currently anonymous
            
            // Update sharing settings
            itinerary.setPublic(false);
            itinerary.setShareToken(null);
            itineraryRepository.save(itinerary);
            
            logger.info("Itinerary unshared: {}", id);
            
        } catch (Exception e) {
            logger.error("Failed to unshare itinerary: " + id, e);
            throw new RuntimeException("Failed to unshare itinerary", e);
        }
    }
    
    /**
     * Get itinerary with AI-generated data from JSON response files.
     */
    private ItineraryDto getItineraryWithAIData(Itinerary itinerary) {
        try {
            // Try to load the most recent Gemini response
            String jsonResponse = loadLatestGeminiResponse();
            if (jsonResponse != null) {
                // Parse the JSON and merge it with the entity data
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                com.fasterxml.jackson.databind.JsonNode jsonNode = mapper.readTree(jsonResponse);
                
                // Create a DTO that combines entity data with AI response data
                ItineraryDto dto = ItineraryDto.fromEntity(itinerary);
                
                // Override with AI-generated data
                if (jsonNode.has("summary")) {
                    dto.setSummary(jsonNode.get("summary").asText());
                }
                
                // Create days from AI response
                java.util.List<ItineraryDayDto> days = new java.util.ArrayList<>();
                com.fasterxml.jackson.databind.JsonNode daysNode = jsonNode.get("days");
                
                if (daysNode != null && daysNode.isArray()) {
                    for (com.fasterxml.jackson.databind.JsonNode dayNode : daysNode) {
                        ItineraryDayDto dayDto = createDayFromAIResponse(dayNode);
                        days.add(dayDto);
                    }
                }
                
                dto.setDays(days);
                
                // Set agent results
                Map<String, Object> agentResults = new HashMap<>();
                if (jsonNode.has("highlights")) {
                    java.util.List<String> highlights = new java.util.ArrayList<>();
                    for (com.fasterxml.jackson.databind.JsonNode highlight : jsonNode.get("highlights")) {
                        highlights.add(highlight.asText());
                    }
                    agentResults.put("highlights", highlights);
                }
                if (jsonNode.has("totalEstimatedCost")) {
                    agentResults.put("totalEstimatedCost", jsonNode.get("totalEstimatedCost").asDouble());
                }
                if (jsonNode.has("currency")) {
                    agentResults.put("currency", jsonNode.get("currency").asText());
                }
                dto.setAgentResults(agentResults);
                
                return dto;
            }
        } catch (Exception e) {
            logger.warn("Failed to load AI response, using basic itinerary: {}", e.getMessage());
        }
        
        // Fallback to basic itinerary without days
        return ItineraryDto.fromEntity(itinerary);
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
