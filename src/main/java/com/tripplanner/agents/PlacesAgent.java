package com.tripplanner.agents;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripplanner.dto.AgentEvent;
import com.tripplanner.service.AgentEventBus;
import com.tripplanner.service.GeminiClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

/**
 * Places Agent - Discovers and analyzes places, areas, and local insights.
 */
@Component
@ConditionalOnBean(GeminiClient.class)
public class PlacesAgent extends BaseAgent {
    
    private final GeminiClient geminiClient;
    private final ObjectMapper objectMapper;
    
    public PlacesAgent(AgentEventBus eventBus, GeminiClient geminiClient, ObjectMapper objectMapper) {
        super(eventBus, AgentEvent.AgentKind.places);
        this.geminiClient = geminiClient;
        this.objectMapper = objectMapper;
    }
    
    @Override
    protected <T> T executeInternal(String itineraryId, AgentRequest<T> request) {
        PlacesRequest placesRequest = (PlacesRequest) request.getData();
        
        logger.info("=== PLACES AGENT PROCESSING ===");
        logger.info("Destination: {}", placesRequest.getDestination());
        logger.info("Duration: {} days", placesRequest.getDuration());
        logger.info("Interests: {}", (Object) placesRequest.getInterests());
        logger.info("Budget Tier: {}", placesRequest.getBudgetTier());
        logger.info("===============================");
        
        emitProgress(itineraryId, 10, "Analyzing destination", "destination_analysis");
        
        String systemPrompt = buildSystemPrompt();
        String userPrompt = buildUserPrompt(placesRequest);
        String jsonSchema = buildPlacesJsonSchema();
        
        logger.info("=== PLACES AGENT PROMPTS ===");
        logger.info("System Prompt Length: {} chars", systemPrompt.length());
        logger.info("User Prompt: {}", userPrompt);
        logger.info("JSON Schema Length: {} chars", jsonSchema.length());
        logger.info("============================");
        
        emitProgress(itineraryId, 50, "Discovering places and areas", "place_discovery");
        
        String response = geminiClient.generateStructuredContent(userPrompt, jsonSchema, systemPrompt);
        
        logger.info("=== PLACES AGENT RESPONSE ===");
        logger.info("Response Length: {} chars", response.length());
        logger.info("Response Preview: {}", response.length() > 200 ? response.substring(0, 200) + "..." : response);
        logger.info("=============================");
        
        emitProgress(itineraryId, 80, "Processing place information", "processing");
        
        try {
            PlacesResponse placesResponse = objectMapper.readValue(response, PlacesResponse.class);
            
            logger.info("=== PLACES AGENT RESULT ===");
            logger.info("Parsed Response Type: {}", placesResponse.getClass().getSimpleName());
            logger.info("Key Areas Found: {}", placesResponse.getKeyAreas() != null ? placesResponse.getKeyAreas().length : 0);
            logger.info("Result: {}", placesResponse);
            logger.info("===========================");
            
            return (T) placesResponse;
        } catch (JsonProcessingException e) {
            logger.error("Failed to parse Places agent response for itinerary: {}", itineraryId, e);
            logger.error("Raw response that failed to parse: {}", response);
            throw new RuntimeException("Failed to parse places data: " + e.getMessage(), e);
        }
    }
    
    @Override
    protected String getAgentName() {
        return "Places Agent";
    }
    
    private String buildSystemPrompt() {
        return """
            You are a local area expert that provides comprehensive information about destinations.
            
            Your responsibilities:
            1. Identify key areas, neighborhoods, and districts in the destination
            2. Provide crowd patterns and best times to visit popular spots
            3. Analyze opening hours and seasonal considerations
            4. Create heatmaps of tourist activity and local hotspots
            5. Provide insider knowledge about hidden gems and local favorites
            
            Focus on practical, actionable information that helps travelers make informed decisions.
            """;
    }
    
    private String buildUserPrompt(PlacesRequest request) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("Analyze the following destination and provide comprehensive place information:\n\n");
        prompt.append("Destination: ").append(request.getDestination()).append("\n");
        prompt.append("Duration: ").append(request.getDuration()).append(" days\n");
        prompt.append("Interests: ").append(String.join(", ", request.getInterests())).append("\n");
        prompt.append("Budget Tier: ").append(request.getBudgetTier()).append("\n");
        
        prompt.append("\nProvide detailed information about key areas, crowd patterns, and local insights.");
        
        return prompt.toString();
    }
    
    private String buildPlacesJsonSchema() {
        return """
            {
              "type": "object",
              "properties": {
                "destination": { "type": "string" },
                "keyAreas": {
                  "type": "array",
                  "items": {
                    "type": "object",
                    "properties": {
                      "name": { "type": "string" },
                      "description": { "type": "string" },
                      "type": { "type": "string", "enum": ["historic", "modern", "cultural", "entertainment", "shopping", "residential", "business"] },
                      "coordinates": {
                        "type": "object",
                        "properties": {
                          "lat": { "type": "number" },
                          "lng": { "type": "number" }
                        }
                      },
                      "crowdPattern": {
                        "type": "object",
                        "properties": {
                          "peakHours": { "type": "array", "items": { "type": "string" } },
                          "quietHours": { "type": "array", "items": { "type": "string" } },
                          "bestDays": { "type": "array", "items": { "type": "string" } }
                        }
                      },
                      "highlights": { "type": "array", "items": { "type": "string" } },
                      "tips": { "type": "string" }
                    }
                  }
                },
                "heatmap": {
                  "type": "object",
                  "properties": {
                    "touristHotspots": { "type": "array", "items": { "type": "string" } },
                    "localFavorites": { "type": "array", "items": { "type": "string" } },
                    "hiddenGems": { "type": "array", "items": { "type": "string" } }
                  }
                },
                "seasonalInfo": {
                  "type": "object",
                  "properties": {
                    "bestTimeToVisit": { "type": "string" },
                    "weatherConsiderations": { "type": "string" },
                    "seasonalEvents": { "type": "array", "items": { "type": "string" } }
                  }
                },
                "practicalInfo": {
                  "type": "object",
                  "properties": {
                    "generalOpeningHours": { "type": "string" },
                    "transportHubs": { "type": "array", "items": { "type": "string" } },
                    "safetyTips": { "type": "array", "items": { "type": "string" } }
                  }
                }
              },
              "required": ["destination", "keyAreas", "heatmap"]
            }
            """;
    }
    
    /**
     * Request for Places Agent.
     */
    public static class PlacesRequest {
        private String destination;
        private int duration;
        private String[] interests;
        private String budgetTier;
        
        public PlacesRequest(String destination, int duration, String[] interests, String budgetTier) {
            this.destination = destination;
            this.duration = duration;
            this.interests = interests;
            this.budgetTier = budgetTier;
        }
        
        // Getters
        public String getDestination() { return destination; }
        public int getDuration() { return duration; }
        public String[] getInterests() { return interests; }
        public String getBudgetTier() { return budgetTier; }
    }
    
    /**
     * Response from Places Agent.
     */
    public static class PlacesResponse {
        private String destination;
        private KeyArea[] keyAreas;
        private Heatmap heatmap;
        private SeasonalInfo seasonalInfo;
        private PracticalInfo practicalInfo;
        
        // Getters and setters
        public String getDestination() { return destination; }
        public void setDestination(String destination) { this.destination = destination; }
        public KeyArea[] getKeyAreas() { return keyAreas; }
        public void setKeyAreas(KeyArea[] keyAreas) { this.keyAreas = keyAreas; }
        public Heatmap getHeatmap() { return heatmap; }
        public void setHeatmap(Heatmap heatmap) { this.heatmap = heatmap; }
        public SeasonalInfo getSeasonalInfo() { return seasonalInfo; }
        public void setSeasonalInfo(SeasonalInfo seasonalInfo) { this.seasonalInfo = seasonalInfo; }
        public PracticalInfo getPracticalInfo() { return practicalInfo; }
        public void setPracticalInfo(PracticalInfo practicalInfo) { this.practicalInfo = practicalInfo; }
        
        public static class KeyArea {
            private String name;
            private String description;
            private String type;
            private Coordinates coordinates;
            private CrowdPattern crowdPattern;
            private String[] highlights;
            private String tips;
            
            // Getters and setters
            public String getName() { return name; }
            public void setName(String name) { this.name = name; }
            public String getDescription() { return description; }
            public void setDescription(String description) { this.description = description; }
            public String getType() { return type; }
            public void setType(String type) { this.type = type; }
            public Coordinates getCoordinates() { return coordinates; }
            public void setCoordinates(Coordinates coordinates) { this.coordinates = coordinates; }
            public CrowdPattern getCrowdPattern() { return crowdPattern; }
            public void setCrowdPattern(CrowdPattern crowdPattern) { this.crowdPattern = crowdPattern; }
            public String[] getHighlights() { return highlights; }
            public void setHighlights(String[] highlights) { this.highlights = highlights; }
            public String getTips() { return tips; }
            public void setTips(String tips) { this.tips = tips; }
        }
        
        public static class Coordinates {
            private double lat;
            private double lng;
            
            // Getters and setters
            public double getLat() { return lat; }
            public void setLat(double lat) { this.lat = lat; }
            public double getLng() { return lng; }
            public void setLng(double lng) { this.lng = lng; }
        }
        
        public static class CrowdPattern {
            private String[] peakHours;
            private String[] quietHours;
            private String[] bestDays;
            
            // Getters and setters
            public String[] getPeakHours() { return peakHours; }
            public void setPeakHours(String[] peakHours) { this.peakHours = peakHours; }
            public String[] getQuietHours() { return quietHours; }
            public void setQuietHours(String[] quietHours) { this.quietHours = quietHours; }
            public String[] getBestDays() { return bestDays; }
            public void setBestDays(String[] bestDays) { this.bestDays = bestDays; }
        }
        
        public static class Heatmap {
            private String[] touristHotspots;
            private String[] localFavorites;
            private String[] hiddenGems;
            
            // Getters and setters
            public String[] getTouristHotspots() { return touristHotspots; }
            public void setTouristHotspots(String[] touristHotspots) { this.touristHotspots = touristHotspots; }
            public String[] getLocalFavorites() { return localFavorites; }
            public void setLocalFavorites(String[] localFavorites) { this.localFavorites = localFavorites; }
            public String[] getHiddenGems() { return hiddenGems; }
            public void setHiddenGems(String[] hiddenGems) { this.hiddenGems = hiddenGems; }
        }
        
        public static class SeasonalInfo {
            private String bestTimeToVisit;
            private String weatherConsiderations;
            private String[] seasonalEvents;
            
            // Getters and setters
            public String getBestTimeToVisit() { return bestTimeToVisit; }
            public void setBestTimeToVisit(String bestTimeToVisit) { this.bestTimeToVisit = bestTimeToVisit; }
            public String getWeatherConsiderations() { return weatherConsiderations; }
            public void setWeatherConsiderations(String weatherConsiderations) { this.weatherConsiderations = weatherConsiderations; }
            public String[] getSeasonalEvents() { return seasonalEvents; }
            public void setSeasonalEvents(String[] seasonalEvents) { this.seasonalEvents = seasonalEvents; }
        }
        
        public static class PracticalInfo {
            private String generalOpeningHours;
            private String[] transportHubs;
            private String[] safetyTips;
            
            // Getters and setters
            public String getGeneralOpeningHours() { return generalOpeningHours; }
            public void setGeneralOpeningHours(String generalOpeningHours) { this.generalOpeningHours = generalOpeningHours; }
            public String[] getTransportHubs() { return transportHubs; }
            public void setTransportHubs(String[] transportHubs) { this.transportHubs = transportHubs; }
            public String[] getSafetyTips() { return safetyTips; }
            public void setSafetyTips(String[] safetyTips) { this.safetyTips = safetyTips; }
        }
    }
}

