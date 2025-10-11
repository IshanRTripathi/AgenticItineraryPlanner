package com.tripplanner.testing;

import com.tripplanner.service.ai.AiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Mock LLM Provider with context-aware response routing for testing.
 * Uses simple prompt analysis to return appropriate mock responses.
 */
public class MockLLMProvider implements AiClient {
    
    private static final Logger logger = LoggerFactory.getLogger(MockLLMProvider.class);
    
    private final Map<String, String> mockResponses = new HashMap<>();
    
    public MockLLMProvider() {
        setupMockResponses();
    }
    
    private void setupMockResponses() {
        // Itinerary creation responses
        mockResponses.put("create_itinerary_bali", loadBaliItineraryResponse());
        mockResponses.put("create_itinerary_tokyo", loadTokyoItineraryResponse());
        mockResponses.put("create_itinerary_default", loadDefaultItineraryResponse());
        
        // Intent classification responses
        mockResponses.put("intent_add_activity", "{\"intent\":\"ADD_ACTIVITY\",\"confidence\":0.95}");
        mockResponses.put("intent_change_hotel", "{\"intent\":\"CHANGE_ACCOMMODATION\",\"confidence\":0.92}");
        mockResponses.put("intent_default", "{\"intent\":\"GENERAL_INQUIRY\",\"confidence\":0.80}");
        
        // Agent-specific responses
        mockResponses.put("enrichment_place_details", loadPlaceDetailsResponse());
        mockResponses.put("booking_confirmation", loadBookingConfirmationResponse());
        mockResponses.put("planning_response", loadPlanningResponse());
        
        // Default responses
        mockResponses.put("default_response", "{\"response\":\"Mock LLM response\",\"status\":\"success\"}");
        
        logger.debug("Mock LLM Provider initialized with {} responses", mockResponses.size());
    }
    
    @Override
    public String generateStructuredContent(String userPrompt, String jsonSchema, String systemPrompt) {
        logger.debug("Mock LLM generateStructuredContent called with prompt length: {}", userPrompt.length());
        
        String mockKey = determineMockKey(userPrompt, null);
        String response = mockResponses.get(mockKey);
        
        if (response == null) {
            logger.warn("No mock response found for key: {}, using default", mockKey);
            response = mockResponses.get("default_response");
        }
        
        logger.debug("Returning mock response for key: {}", mockKey);
        return response;
    }
    
    @Override
    public String generateContent(String userPrompt, String systemPrompt) {
        return generateStructuredContent(userPrompt, null, systemPrompt);
    }
    
    @Override
    public boolean isAvailable() {
        return true;
    }
    
    @Override
    public String getModelInfo() {
        return "Mock LLM Provider v1.0 - Test Model";
    }
    
    /**
     * Determine which mock response to use based on prompt content and parameters.
     */
    private String determineMockKey(String prompt, Map<String, Object> parameters) {
        if (prompt == null) {
            return "default_response";
        }
        
        String lowerPrompt = prompt.toLowerCase();
        
        // Itinerary creation detection
        if (lowerPrompt.contains("itinerary") || lowerPrompt.contains("trip") || lowerPrompt.contains("travel")) {
            if (lowerPrompt.contains("bali")) {
                return "create_itinerary_bali";
            }
            if (lowerPrompt.contains("tokyo") || lowerPrompt.contains("japan")) {
                return "create_itinerary_tokyo";
            }
            return "create_itinerary_default";
        }
        
        // Intent classification detection
        if (lowerPrompt.contains("intent") || lowerPrompt.contains("classify")) {
            if (lowerPrompt.contains("activity") || lowerPrompt.contains("add")) {
                return "intent_add_activity";
            }
            if (lowerPrompt.contains("hotel") || lowerPrompt.contains("accommodation")) {
                return "intent_change_hotel";
            }
            return "intent_default";
        }
        
        // Agent-specific detection
        if (lowerPrompt.contains("enrich") || lowerPrompt.contains("place") || lowerPrompt.contains("details")) {
            return "enrichment_place_details";
        }
        if (lowerPrompt.contains("book") || lowerPrompt.contains("reservation")) {
            return "booking_confirmation";
        }
        if (lowerPrompt.contains("plan") || lowerPrompt.contains("schedule")) {
            return "planning_response";
        }
        
        return "default_response";
    }
    
    /**
     * Load Bali itinerary response from test resources.
     */
    private String loadBaliItineraryResponse() {
        try {
            return loadResourceFile("mock-data/destinations/bali/3-day-luxury-relaxation.json");
        } catch (Exception e) {
            logger.warn("Could not load Bali itinerary response: {}", e.getMessage());
            return createSimpleBaliResponse();
        }
    }
    
    /**
     * Load Tokyo itinerary response (simplified for testing).
     */
    private String loadTokyoItineraryResponse() {
        return createSimpleTokyoResponse();
    }
    
    /**
     * Load default itinerary response.
     */
    private String loadDefaultItineraryResponse() {
        return createSimpleDefaultResponse();
    }
    
    /**
     * Load place details response for ENRICHMENT.
     */
    private String loadPlaceDetailsResponse() {
        return "{\n" +
               "  \"placeId\": \"ChIJoQ8Q6NNB0i0RkOYkS7EPkSQ\",\n" +
               "  \"name\": \"Test Location\",\n" +
               "  \"rating\": 4.5,\n" +
               "  \"openingHours\": \"09:00\",\n" +
               "  \"closingHours\": \"18:00\",\n" +
               "  \"enriched\": true\n" +
               "}";
    }
    
    /**
     * Load booking confirmation response.
     */
    private String loadBookingConfirmationResponse() {
        return "{\n" +
               "  \"bookingReference\": \"MOCK-BOOKING-123\",\n" +
               "  \"status\": \"confirmed\",\n" +
               "  \"confirmationNumber\": \"CONF-456\"\n" +
               "}";
    }
    
    /**
     * Load planning response.
     */
    private String loadPlanningResponse() {
        return "{\n" +
               "  \"planningStatus\": \"completed\",\n" +
               "  \"recommendations\": [\"Visit temple\", \"Try local cuisine\"],\n" +
               "  \"optimized\": true\n" +
               "}";
    }
    
    /**
     * Load resource file from classpath.
     */
    private String loadResourceFile(String resourcePath) throws IOException {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new IOException("Resource not found: " + resourcePath);
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
    
    /**
     * Create simple Bali response if file loading fails.
     */
    private String createSimpleBaliResponse() {
        return "{\n" +
               "  \"itineraryId\": \"mock-bali-001\",\n" +
               "  \"destination\": \"Bali, Indonesia\",\n" +
               "  \"days\": [],\n" +
               "  \"currency\": \"USD\",\n" +
               "  \"themes\": [\"relaxation\", \"luxury\"]\n" +
               "}";
    }
    
    /**
     * Create simple Tokyo response.
     */
    private String createSimpleTokyoResponse() {
        return "{\n" +
               "  \"itineraryId\": \"mock-tokyo-001\",\n" +
               "  \"destination\": \"Tokyo, Japan\",\n" +
               "  \"days\": [],\n" +
               "  \"currency\": \"JPY\",\n" +
               "  \"themes\": [\"culture\", \"technology\"]\n" +
               "}";
    }
    
    /**
     * Create simple default response.
     */
    private String createSimpleDefaultResponse() {
        return "{\n" +
               "  \"itineraryId\": \"mock-default-001\",\n" +
               "  \"destination\": \"Test Destination\",\n" +
               "  \"days\": [],\n" +
               "  \"currency\": \"USD\",\n" +
               "  \"themes\": [\"test\"]\n" +
               "}";
    }
}