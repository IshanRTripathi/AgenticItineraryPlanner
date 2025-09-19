package com.tripplanner.service.agents;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripplanner.api.dto.*;
import com.tripplanner.service.AgentEventBus;
import com.tripplanner.service.GeminiClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Planner Agent - Main orchestrator for itinerary generation using Gemini.
 */
@Component
@ConditionalOnBean(GeminiClient.class)
public class PlannerAgent extends BaseAgent {
    
    private final GeminiClient geminiClient;
    private final ObjectMapper objectMapper;
    
    public PlannerAgent(AgentEventBus eventBus, GeminiClient geminiClient, ObjectMapper objectMapper) {
        super(eventBus, AgentEvent.AgentKind.planner);
        this.geminiClient = geminiClient;
        this.objectMapper = objectMapper;
    }
    
    @Override
    protected <T> T executeInternal(String itineraryId, AgentRequest<T> request) {
        CreateItineraryReq itineraryReq = request.getData(CreateItineraryReq.class);
        
        if (itineraryReq == null) {
            throw new IllegalArgumentException("PlannerAgent requires non-null CreateItineraryReq");
        }
        
        logger.info("=== PLANNER AGENT PROCESSING ===");
        logger.info("Destination: {}", itineraryReq.getDestination());
        logger.info("Start Date: {}", itineraryReq.getStartDate());
        logger.info("End Date: {}", itineraryReq.getEndDate());
        logger.info("Duration: {} days", itineraryReq.getDurationDays());
        logger.info("Party: {} adults", itineraryReq.getParty() != null ? itineraryReq.getParty().getAdults() : 0);
        logger.info("Budget Tier: {}", itineraryReq.getBudgetTier());
        logger.info("Interests: {}", itineraryReq.getInterests());
        logger.info("Constraints: {}", itineraryReq.getConstraints());
        logger.info("Language: {}", itineraryReq.getLanguage());
        logger.info("=================================");
        
        emitProgress(itineraryId, 10, "Analyzing trip requirements", "requirement_analysis");
        
        // Build the system prompt for itinerary planning
        String systemPrompt = buildSystemPrompt();
        
        emitProgress(itineraryId, 30, "Generating itinerary structure", "structure_generation");
        
        // Build the user prompt with trip details
        String userPrompt = buildUserPrompt(itineraryReq);
        
        logger.info("=== PLANNER AGENT PROMPTS ===");
        logger.info("System Prompt Length: {} chars", systemPrompt.length());
        logger.info("User Prompt: {}", userPrompt);
        logger.info("=============================");
        
        emitProgress(itineraryId, 50, "Processing with AI model", "ai_processing");
        
        // Use the JSON file approach instead of calling Gemini API
        String response = loadLatestGeminiResponse();
        
        logger.info("=== PLANNER AGENT RESPONSE ===");
        logger.info("Using JSON file response");
        logger.info("Response Length: {} chars", response.length());
        logger.info("Response Preview: {}", response.length() > 200 ? response.substring(0, 200) + "..." : response);
        logger.info("===============================");
        
        emitProgress(itineraryId, 80, "Parsing generated itinerary", "parsing");
        
        // Parse and validate the response
        try {
            // Clean the response by removing markdown code blocks if present
            String cleanedResponse = cleanJsonResponse(response);
            ItineraryGenerationResponse itineraryResponse = objectMapper.readValue(cleanedResponse, ItineraryGenerationResponse.class);
            
            logger.info("=== PLANNER AGENT RESULT ===");
            logger.info("Parsed Response Type: {}", itineraryResponse.getClass().getSimpleName());
            logger.info("Days Generated: {}", itineraryResponse.getDays() != null ? itineraryResponse.getDays().length : 0);
            logger.info("Result: {}", itineraryResponse);
            logger.info("============================");
            
            emitProgress(itineraryId, 90, "Finalizing itinerary", "finalization");
            
            // Convert to the expected response type
            if (request.getResponseType() == ItineraryDto.class) {
                ItineraryDto result = convertToItineraryDto(itineraryResponse, itineraryReq);
                
                logger.info("=== PLANNER AGENT FINAL RESULT ===");
                logger.info("Converted to ItineraryDto: {}", result.getId());
                logger.info("Final Result: {}", result);
                logger.info("==================================");
                
                return (T) result;
            }
            
            return (T) itineraryResponse;
            
        } catch (JsonProcessingException e) {
            logger.error("Failed to parse Gemini response for itinerary: {}", itineraryId, e);
            logger.error("Raw response that failed to parse: {}", response);
            throw new RuntimeException("Failed to parse generated itinerary: " + e.getMessage(), e);
        }
    }
    
    @Override
    protected String getAgentName() {
        return "Planner Agent";
    }
    
    /**
     * Clean JSON response by removing markdown code blocks if present.
     */
    private String cleanJsonResponse(String response) {
        if (response == null || response.trim().isEmpty()) {
            return response;
        }
        
        String cleaned = response.trim();
        
        // Remove markdown code blocks (```json ... ```)
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7); // Remove ```json
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3); // Remove ```
        }
        
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3); // Remove trailing ```
        }
        
        return cleaned.trim();
    }
    
    private String buildSystemPrompt() {
        return """
            You are an expert travel planner AI that creates detailed, personalized itineraries.
            
            Your responsibilities:
            1. Create day-by-day itineraries based on traveler preferences
            2. Include specific activities, restaurants, accommodations, and transportation
            3. Consider budget constraints and optimize for the best experience
            4. Provide practical details like timing, costs, and tips
            5. Ensure realistic travel times and logical flow between activities
            
            Guidelines:
            - Activities should be appropriate for the group size and preferences
            - Include a good mix of must-see attractions and local experiences
            - Consider opening hours, weather, and seasonal factors
            - Provide alternative options for flexibility
            - Include practical information like booking requirements and costs
            
            Always respond with valid JSON matching the exact schema provided.
            """;
    }
    
    private String buildUserPrompt(CreateItineraryReq request) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("Create a detailed itinerary for the following trip:\n\n");
        
        // Basic trip info
        prompt.append("Destination: ").append(request.getDestination()).append("\n");
        prompt.append("Start Date: ").append(request.getStartDate()).append("\n");
        prompt.append("End Date: ").append(request.getEndDate()).append("\n");
        prompt.append("Duration: ").append(request.getDurationDays()).append(" days\n");
        
        // Party information
        if (request.getParty() != null) {
            prompt.append("Party: ").append(request.getParty().getAdults()).append(" adults");
            if (request.getParty().getChildren() > 0) {
                prompt.append(", ").append(request.getParty().getChildren()).append(" children");
            }
            if (request.getParty().getInfants() > 0) {
                prompt.append(", ").append(request.getParty().getInfants()).append(" infants");
            }
            prompt.append("\n");
        }
        
        // Budget and preferences
        prompt.append("Budget Tier: ").append(request.getBudgetTier()).append("\n");
        prompt.append("Language: ").append(request.getLanguage()).append("\n");
        
        // Interests
        if (request.getInterests() != null && !request.getInterests().isEmpty()) {
            prompt.append("Interests: ").append(String.join(", ", request.getInterests())).append("\n");
        }
        
        // Constraints
        if (request.getConstraints() != null && !request.getConstraints().isEmpty()) {
            prompt.append("Constraints: ").append(String.join(", ", request.getConstraints())).append("\n");
        }
        
        prompt.append("\nPlease create a comprehensive itinerary that maximizes the travel experience while staying within the specified parameters.");
        
        return prompt.toString();
    }
    
    private String buildItineraryJsonSchema() {
        return """
            {
              "type": "object",
              "properties": {
                "summary": {
                  "type": "string",
                  "description": "Brief overview of the itinerary"
                },
                "highlights": {
                  "type": "array",
                  "items": { "type": "string" },
                  "description": "Key highlights of the trip"
                },
                "totalEstimatedCost": {
                  "type": "number",
                  "description": "Total estimated cost for the trip"
                },
                "currency": {
                  "type": "string",
                  "description": "Currency code (e.g., USD, EUR)"
                },
                "days": {
                  "type": "array",
                  "items": {
                    "type": "object",
                    "properties": {
                      "dayNumber": { "type": "integer" },
                      "date": { "type": "string", "format": "date" },
                      "theme": { "type": "string" },
                      "location": { "type": "string" },
                      "activities": {
                        "type": "array",
                        "items": {
                          "type": "object",
                          "properties": {
                            "name": { "type": "string" },
                            "description": { "type": "string" },
                            "startTime": { "type": "string" },
                            "endTime": { "type": "string" },
                            "duration": { "type": "string" },
                            "category": { "type": "string" },
                            "estimatedCost": { "type": "number" },
                            "location": {
                              "type": "object",
                              "properties": {
                                "name": { "type": "string" },
                                "address": { "type": "string" },
                                "lat": { "type": "number" },
                                "lng": { "type": "number" }
                              }
                            },
                            "tips": { "type": "string" },
                            "bookingRequired": { "type": "boolean" }
                          }
                        }
                      },
                      "meals": {
                        "type": "array",
                        "items": {
                          "type": "object",
                          "properties": {
                            "type": { "type": "string", "enum": ["breakfast", "lunch", "dinner", "snack"] },
                            "name": { "type": "string" },
                            "restaurant": { "type": "string" },
                            "cuisine": { "type": "string" },
                            "estimatedCost": { "type": "number" },
                            "location": {
                              "type": "object",
                              "properties": {
                                "name": { "type": "string" },
                                "address": { "type": "string" },
                                "lat": { "type": "number" },
                                "lng": { "type": "number" }
                              }
                            }
                          }
                        }
                      },
                      "accommodation": {
                        "type": "object",
                        "properties": {
                          "name": { "type": "string" },
                          "type": { "type": "string" },
                          "estimatedCost": { "type": "number" },
                          "rating": { "type": "number" },
                          "location": {
                            "type": "object",
                            "properties": {
                              "name": { "type": "string" },
                              "address": { "type": "string" },
                              "lat": { "type": "number" },
                              "lng": { "type": "number" }
                            }
                          }
                        }
                      },
                      "transportation": {
                        "type": "array",
                        "items": {
                          "type": "object",
                          "properties": {
                            "mode": { "type": "string" },
                            "from": { "type": "string" },
                            "to": { "type": "string" },
                            "departureTime": { "type": "string" },
                            "arrivalTime": { "type": "string" },
                            "estimatedCost": { "type": "number" },
                            "notes": { "type": "string" }
                          }
                        }
                      },
                      "notes": { "type": "string" }
                    }
                  }
                }
              },
              "required": ["summary", "days"]
            }
            """;
    }
    
    private ItineraryDto convertToItineraryDto(ItineraryGenerationResponse response, CreateItineraryReq request) {
        logger.info("=== CONVERTING TO ITINERARY DTO ===");
        logger.info("Response summary: {}", response.getSummary());
        logger.info("Days count: {}", response.getDays() != null ? response.getDays().length : 0);
        logger.info("Total cost: {} {}", response.getTotalEstimatedCost(), response.getCurrency());
        
        // Convert days from AI response to ItineraryDayDto
        List<ItineraryDayDto> days = new ArrayList<>();
        if (response.getDays() != null) {
            for (ItineraryGenerationResponse.DayPlan dayPlan : response.getDays()) {
                ItineraryDayDto dayDto = convertDayPlanToDto(dayPlan);
                days.add(dayDto);
            }
        }
        
        // Create agent results map with highlights
        Map<String, Object> agentResults = new HashMap<>();
        if (response.getHighlights() != null) {
            agentResults.put("highlights", response.getHighlights());
        }
        agentResults.put("totalEstimatedCost", response.getTotalEstimatedCost());
        agentResults.put("currency", response.getCurrency());
        
        // Build the ItineraryDto
        ItineraryDto itineraryDto = ItineraryDto.builder()
                .id(request.getDestination().hashCode() + "_" + System.currentTimeMillis()) // Generate a temporary ID
                .destination(request.getDestination())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .party(request.getParty())
                .budgetTier(request.getBudgetTier())
                .interests(request.getInterests())
                .constraints(request.getConstraints())
                .language(request.getLanguage())
                .summary(response.getSummary())
                .map(null) // Map data not implemented yet
                .days(days)
                .agentResults(agentResults)
                .status("completed")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .isPublic(false)
                .shareToken(null)
                .build();
        
        logger.info("=== CONVERSION COMPLETED ===");
        logger.info("Generated itinerary with {} days", days.size());
        logger.info("Itinerary ID: {}", itineraryDto.getId());
        
        return itineraryDto;
    }
    
    private ItineraryDayDto convertDayPlanToDto(ItineraryGenerationResponse.DayPlan dayPlan) {
        // Convert activities
        List<ActivityDto> activities = new ArrayList<>();
        if (dayPlan.getActivities() != null) {
            for (ItineraryGenerationResponse.Activity activity : dayPlan.getActivities()) {
                ActivityDto activityDto = new ActivityDto(
                        activity.getName(),
                        activity.getDescription(),
                        convertLocationToDto(activity.getLocation()),
                        activity.getStartTime(),
                        activity.getEndTime(),
                        activity.getDuration(),
                        activity.getCategory(),
                        new PriceDto(activity.getEstimatedCost(), "EUR", "person"), // Default to EUR for now
                        activity.isBookingRequired(),
                        null, // bookingUrl not provided by AI
                        activity.getTips()
                );
                activities.add(activityDto);
            }
        }
        
        // Convert accommodation
        AccommodationDto accommodation = null;
        if (dayPlan.getAccommodation() != null) {
            ItineraryGenerationResponse.Accommodation acc = dayPlan.getAccommodation();
            accommodation = new AccommodationDto(
                    acc.getName(),
                    acc.getType(),
                    convertLocationToDto(acc.getLocation()),
                    null, // checkIn not provided by AI
                    null, // checkOut not provided by AI
                    new PriceDto(acc.getEstimatedCost(), "EUR", "night"), // Default to EUR for now
                    acc.getRating(),
                    null, // amenities not provided by AI
                    null  // bookingUrl not provided by AI
            );
        }
        
        // Convert transportation
        List<TransportationDto> transportation = new ArrayList<>();
        if (dayPlan.getTransportation() != null) {
            for (ItineraryGenerationResponse.Transportation trans : dayPlan.getTransportation()) {
                TransportationDto transDto = new TransportationDto(
                        trans.getMode(),
                        createLocationFromString(trans.getFrom()),
                        createLocationFromString(trans.getTo()),
                        trans.getDepartureTime(),
                        trans.getArrivalTime(),
                        null, // duration not provided by AI
                        new PriceDto(trans.getEstimatedCost(), "EUR", "trip"), // Default to EUR for now
                        null, // provider not provided by AI
                        null, // bookingUrl not provided by AI
                        trans.getNotes()
                );
                transportation.add(transDto);
            }
        }
        
        // Convert meals
        List<MealDto> meals = new ArrayList<>();
        if (dayPlan.getMeals() != null) {
            for (ItineraryGenerationResponse.Meal meal : dayPlan.getMeals()) {
                MealDto mealDto = new MealDto(
                        meal.getType(),
                        meal.getName(),
                        meal.getRestaurant(),
                        convertLocationToDto(meal.getLocation()),
                        null, // time not provided by AI
                        new PriceDto(meal.getEstimatedCost(), "EUR", "person"), // Default to EUR for now
                        meal.getCuisine(),
                        null  // notes not provided by AI
                );
                meals.add(mealDto);
            }
        }
        
        return ItineraryDayDto.builder()
                .day(dayPlan.getDayNumber())
                .date(parseDate(dayPlan.getDate()))
                .location(dayPlan.getLocation())
                .activities(activities)
                .accommodation(accommodation)
                .transportation(transportation)
                .meals(meals)
                .notes(dayPlan.getNotes())
                .build();
    }
    
    private LocationDto convertLocationToDto(ItineraryGenerationResponse.Location location) {
        if (location == null) {
            return null;
        }
        
        return new LocationDto(
                location.getName(),
                location.getAddress(),
                location.getLat(),
                location.getLng(),
                null // placeId not provided by AI
        );
    }
    
    private LocationDto createLocationFromString(String locationString) {
        if (locationString == null || locationString.trim().isEmpty()) {
            return null;
        }
        
        // Create a simple location with just the name
        return new LocationDto(
                locationString.trim(),
                null, // address not provided
                0.0,  // lat not provided
                0.0,  // lng not provided
                null  // placeId not provided
        );
    }
    
    private LocalDate parseDate(String dateString) {
        if (dateString == null || dateString.trim().isEmpty()) {
            return null;
        }
        
        try {
            // Try to parse the date string - assuming it's in YYYY-MM-DD format
            return LocalDate.parse(dateString.trim());
        } catch (Exception e) {
            logger.warn("Failed to parse date: {}", dateString, e);
            return null;
        }
    }
    
    /**
     * Response structure from Gemini for itinerary generation.
     */
    public static class ItineraryGenerationResponse {
        private String summary;
        private String[] highlights;
        private double totalEstimatedCost;
        private String currency;
        private DayPlan[] days;
        
        // Getters and setters
        public String getSummary() { return summary; }
        public void setSummary(String summary) { this.summary = summary; }
        public String[] getHighlights() { return highlights; }
        public void setHighlights(String[] highlights) { this.highlights = highlights; }
        public double getTotalEstimatedCost() { return totalEstimatedCost; }
        public void setTotalEstimatedCost(double totalEstimatedCost) { this.totalEstimatedCost = totalEstimatedCost; }
        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }
        public DayPlan[] getDays() { return days; }
        public void setDays(DayPlan[] days) { this.days = days; }
        
        public static class DayPlan {
            private int dayNumber;
            private String date;
            private String theme;
            private String location;
            private Activity[] activities;
            private Meal[] meals;
            private Accommodation accommodation;
            private Transportation[] transportation;
            private String notes;
            
            // Getters and setters
            public int getDayNumber() { return dayNumber; }
            public void setDayNumber(int dayNumber) { this.dayNumber = dayNumber; }
            public String getDate() { return date; }
            public void setDate(String date) { this.date = date; }
            public String getTheme() { return theme; }
            public void setTheme(String theme) { this.theme = theme; }
            public String getLocation() { return location; }
            public void setLocation(String location) { this.location = location; }
            public Activity[] getActivities() { return activities; }
            public void setActivities(Activity[] activities) { this.activities = activities; }
            public Meal[] getMeals() { return meals; }
            public void setMeals(Meal[] meals) { this.meals = meals; }
            public Accommodation getAccommodation() { return accommodation; }
            public void setAccommodation(Accommodation accommodation) { this.accommodation = accommodation; }
            public Transportation[] getTransportation() { return transportation; }
            public void setTransportation(Transportation[] transportation) { this.transportation = transportation; }
            public String getNotes() { return notes; }
            public void setNotes(String notes) { this.notes = notes; }
        }
        
        public static class Activity {
            private String name;
            private String description;
            private String startTime;
            private String endTime;
            private String duration;
            private String category;
            private double estimatedCost;
            private Location location;
            private String tips;
            private boolean bookingRequired;
            
            // Getters and setters
            public String getName() { return name; }
            public void setName(String name) { this.name = name; }
            public String getDescription() { return description; }
            public void setDescription(String description) { this.description = description; }
            public String getStartTime() { return startTime; }
            public void setStartTime(String startTime) { this.startTime = startTime; }
            public String getEndTime() { return endTime; }
            public void setEndTime(String endTime) { this.endTime = endTime; }
            public String getDuration() { return duration; }
            public void setDuration(String duration) { this.duration = duration; }
            public String getCategory() { return category; }
            public void setCategory(String category) { this.category = category; }
            public double getEstimatedCost() { return estimatedCost; }
            public void setEstimatedCost(double estimatedCost) { this.estimatedCost = estimatedCost; }
            public Location getLocation() { return location; }
            public void setLocation(Location location) { this.location = location; }
            public String getTips() { return tips; }
            public void setTips(String tips) { this.tips = tips; }
            public boolean isBookingRequired() { return bookingRequired; }
            public void setBookingRequired(boolean bookingRequired) { this.bookingRequired = bookingRequired; }
        }
        
        public static class Meal {
            private String type;
            private String name;
            private String restaurant;
            private String cuisine;
            private double estimatedCost;
            private Location location;
            
            // Getters and setters
            public String getType() { return type; }
            public void setType(String type) { this.type = type; }
            public String getName() { return name; }
            public void setName(String name) { this.name = name; }
            public String getRestaurant() { return restaurant; }
            public void setRestaurant(String restaurant) { this.restaurant = restaurant; }
            public String getCuisine() { return cuisine; }
            public void setCuisine(String cuisine) { this.cuisine = cuisine; }
            public double getEstimatedCost() { return estimatedCost; }
            public void setEstimatedCost(double estimatedCost) { this.estimatedCost = estimatedCost; }
            public Location getLocation() { return location; }
            public void setLocation(Location location) { this.location = location; }
        }
        
        public static class Accommodation {
            private String name;
            private String type;
            private double estimatedCost;
            private double rating;
            private Location location;
            
            // Getters and setters
            public String getName() { return name; }
            public void setName(String name) { this.name = name; }
            public String getType() { return type; }
            public void setType(String type) { this.type = type; }
            public double getEstimatedCost() { return estimatedCost; }
            public void setEstimatedCost(double estimatedCost) { this.estimatedCost = estimatedCost; }
            public double getRating() { return rating; }
            public void setRating(double rating) { this.rating = rating; }
            public Location getLocation() { return location; }
            public void setLocation(Location location) { this.location = location; }
        }
        
        public static class Transportation {
            private String mode;
            private String from;
            private String to;
            private String departureTime;
            private String arrivalTime;
            private double estimatedCost;
            private String notes;
            
            // Getters and setters
            public String getMode() { return mode; }
            public void setMode(String mode) { this.mode = mode; }
            public String getFrom() { return from; }
            public void setFrom(String from) { this.from = from; }
            public String getTo() { return to; }
            public void setTo(String to) { this.to = to; }
            public String getDepartureTime() { return departureTime; }
            public void setDepartureTime(String departureTime) { this.departureTime = departureTime; }
            public String getArrivalTime() { return arrivalTime; }
            public void setArrivalTime(String arrivalTime) { this.arrivalTime = arrivalTime; }
            public double getEstimatedCost() { return estimatedCost; }
            public void setEstimatedCost(double estimatedCost) { this.estimatedCost = estimatedCost; }
            public String getNotes() { return notes; }
            public void setNotes(String notes) { this.notes = notes; }
        }
        
        public static class Location {
            private String name;
            private String address;
            private double lat;
            private double lng;
            
            // Getters and setters
            public String getName() { return name; }
            public void setName(String name) { this.name = name; }
            public String getAddress() { return address; }
            public void setAddress(String address) { this.address = address; }
            public double getLat() { return lat; }
            public void setLat(double lat) { this.lat = lat; }
            public double getLng() { return lng; }
            public void setLng(double lng) { this.lng = lng; }
        }
    }
    
    /**
     * Load the latest Gemini response from the JSON file
     */
    private String loadLatestGeminiResponse() {
        try {
            java.nio.file.Path jsonPath = java.nio.file.Paths.get("logs/gemini-responses/barcelona_3day_family.json");
            return java.nio.file.Files.readString(jsonPath);
        } catch (Exception e) {
            logger.error("Failed to load Gemini response from file: {}", e.getMessage());
            throw new RuntimeException("Failed to load Gemini response", e);
        }
    }
}

