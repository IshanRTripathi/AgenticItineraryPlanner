package com.tripplanner.service.agents;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripplanner.api.dto.AgentEvent;
import com.tripplanner.api.dto.CreateItineraryReq;
import com.tripplanner.api.dto.ItineraryDto;
import com.tripplanner.data.entity.Itinerary;
import com.tripplanner.service.AgentEventBus;
import com.tripplanner.service.GeminiClient;
import org.springframework.stereotype.Component;

/**
 * Planner Agent - Main orchestrator for itinerary generation using Gemini.
 */
@Component
@org.springframework.boot.autoconfigure.condition.ConditionalOnBean(GeminiClient.class)
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
        if (!(request.getData() instanceof CreateItineraryReq)) {
            throw new IllegalArgumentException("PlannerAgent expects CreateItineraryReq");
        }
        
        CreateItineraryReq itineraryReq = (CreateItineraryReq) request.getData();
        
        emitProgress(itineraryId, 10, "Analyzing trip requirements", "requirement_analysis");
        
        // Build the system prompt for itinerary planning
        String systemPrompt = buildSystemPrompt();
        
        emitProgress(itineraryId, 30, "Generating itinerary structure", "structure_generation");
        
        // Build the user prompt with trip details
        String userPrompt = buildUserPrompt(itineraryReq);
        
        emitProgress(itineraryId, 50, "Processing with AI model", "ai_processing");
        
        // Generate the itinerary using Gemini
        String jsonSchema = buildItineraryJsonSchema();
        String response = geminiClient.generateStructuredContent(userPrompt, jsonSchema, systemPrompt);
        
        emitProgress(itineraryId, 80, "Parsing generated itinerary", "parsing");
        
        // Parse and validate the response
        try {
            ItineraryGenerationResponse itineraryResponse = objectMapper.readValue(response, ItineraryGenerationResponse.class);
            
            emitProgress(itineraryId, 90, "Finalizing itinerary", "finalization");
            
            // Convert to the expected response type
            if (request.getResponseType() == ItineraryDto.class) {
                ItineraryDto result = convertToItineraryDto(itineraryResponse, itineraryReq);
                return (T) result;
            }
            
            return (T) itineraryResponse;
            
        } catch (JsonProcessingException e) {
            logger.error("Failed to parse Gemini response for itinerary: {}", itineraryId, e);
            throw new RuntimeException("Failed to parse generated itinerary: " + e.getMessage(), e);
        }
    }
    
    @Override
    protected String getAgentName() {
        return "Planner Agent";
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
        prompt.append("Destination: ").append(request.destination()).append("\n");
        prompt.append("Start Date: ").append(request.startDate()).append("\n");
        prompt.append("End Date: ").append(request.endDate()).append("\n");
        prompt.append("Duration: ").append(request.getDurationDays()).append(" days\n");
        
        // Party information
        if (request.party() != null) {
            prompt.append("Party: ").append(request.party().adults()).append(" adults");
            if (request.party().children() > 0) {
                prompt.append(", ").append(request.party().children()).append(" children");
            }
            if (request.party().infants() > 0) {
                prompt.append(", ").append(request.party().infants()).append(" infants");
            }
            prompt.append("\n");
        }
        
        // Budget and preferences
        prompt.append("Budget Tier: ").append(request.budgetTier()).append("\n");
        prompt.append("Language: ").append(request.language()).append("\n");
        
        // Interests
        if (request.interests() != null && !request.interests().isEmpty()) {
            prompt.append("Interests: ").append(String.join(", ", request.interests())).append("\n");
        }
        
        // Constraints
        if (request.constraints() != null && !request.constraints().isEmpty()) {
            prompt.append("Constraints: ").append(String.join(", ", request.constraints())).append("\n");
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
        // This would convert the AI response to the ItineraryDto format
        // For now, return a placeholder - this would need to be implemented based on the exact mapping needed
        throw new UnsupportedOperationException("Conversion to ItineraryDto not yet implemented");
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
}

