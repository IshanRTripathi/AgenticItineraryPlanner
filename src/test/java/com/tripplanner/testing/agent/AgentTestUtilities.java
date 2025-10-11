package com.tripplanner.testing.agent;

import com.tripplanner.dto.*;
import com.tripplanner.service.LLMResponseHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Utility class for agent testing with mock LLM response builders and validation helpers.
 */
public class AgentTestUtilities {
    
    private static final Logger logger = LoggerFactory.getLogger(AgentTestUtilities.class);
    
    /**
     * Create mock LLM response for itinerary planning.
     */
    public static String createMockItineraryPlanningResponse() {
        return """
            {
                "itineraryId": "test-itinerary-001",
                "title": "Bali Cultural Adventure",
                "destination": "Bali, Indonesia",
                "duration": 7,
                "days": [
                    {
                        "dayNumber": 1,
                        "date": "2024-03-15",
                        "title": "Arrival in Ubud",
                        "nodes": [
                            {
                                "id": "node-001",
                                "type": "accommodation",
                                "name": "Luxury Resort Ubud",
                                "location": {
                                    "latitude": -8.7467,
                                    "longitude": 115.1671,
                                    "address": "Ubud, Bali, Indonesia"
                                }
                            }
                        ]
                    }
                ]
            }
            """;
    }
    
    /**
     * Create mock LLM response for place ENRICHMENT.
     */
    public static String createMockPlaceEnrichmentResponse() {
        return """
            {
                "placeId": "ChIJoQ8Q6NNB0i0RkOYkS7EPkSQ",
                "name": "Ubud Traditional Market",
                "rating": 4.2,
                "photos": ["photo1.jpg", "photo2.jpg"],
                "openingHours": "06:00-18:00",
                "priceLevel": 2,
                "types": ["market", "tourist_attraction"]
            }
            """;
    }
    
    /**
     * Create mock LLM response for booking operations.
     */
    public static String createMockBookingResponse() {
        return """
            {
                "bookingId": "booking-001",
                "status": "confirmed",
                "confirmationNumber": "CONF-12345",
                "totalAmount": 150.00,
                "currency": "USD"
            }
            """;
    }
    
    /**
     * Create mock LLM response for editing operations.
     */
    public static String createMockEditingResponse() {
        return """
            {
                "changeSet": {
                    "type": "modify_node",
                    "nodeId": "node-001",
                    "changes": {
                        "name": "Updated Activity Name",
                        "description": "Updated description"
                    }
                },
                "summary": "Updated activity details"
            }
            """;
    }
    
    /**
     * Create mock processed response for LLMResponseHandler.
     */
    public static LLMResponseHandler.ProcessedResponse createMockProcessedResponse(boolean success) {
        LLMResponseHandler.ProcessedResponse response = mock(LLMResponseHandler.ProcessedResponse.class);
        when(response.isSuccess()).thenReturn(success);
        
        if (success) {
            when(response.getErrors()).thenReturn(Arrays.asList());
            when(response.getData()).thenReturn(mock(com.fasterxml.jackson.databind.JsonNode.class));
        } else {
            when(response.getErrors()).thenReturn(Arrays.asList("Invalid JSON format", "Missing required fields"));
            when(response.needsContinuation()).thenReturn(false);
        }
        
        return response;
    }
    
    /**
     * Create mock agent capabilities for testing.
     */
    public static AgentCapabilities createMockAgentCapabilities(String agentType) {
        AgentCapabilities capabilities = new AgentCapabilities();
        
        switch (agentType.toLowerCase()) {
            case "PLANNER":
                capabilities.addSupportedTask("plan");
                capabilities.addSupportedTask("create");
                capabilities.addSupportedTask("edit");
                capabilities.addSupportedDataSection("itinerary");
                capabilities.addSupportedDataSection("activities");
                capabilities.setPriority(10);
                capabilities.setConfigurationValue("requiresLLM", true);
                capabilities.setConfigurationValue("canCreateItinerary", true);
                break;
                
            case "booking":
                capabilities.addSupportedTask("book");
                capabilities.addSupportedTask("reserve");
                capabilities.addSupportedTask("payment");
                capabilities.addSupportedDataSection("bookings");
                capabilities.addSupportedDataSection("payments");
                capabilities.setPriority(40);
                capabilities.setConfigurationValue("requiresPayment", true);
                capabilities.setConfigurationValue("handlesReservations", true);
                break;
                
            case "ENRICHMENT":
                capabilities.addSupportedTask("enrich");
                capabilities.addSupportedTask("validate");
                capabilities.addSupportedTask("enhance");
                capabilities.addSupportedDataSection("places");
                capabilities.addSupportedDataSection("details");
                capabilities.setPriority(20);
                capabilities.setConfigurationValue("requiresExternalAPI", true);
                capabilities.setConfigurationValue("providesValidation", true);
                break;
                
            case "places":
                capabilities.addSupportedTask("places");
                capabilities.addSupportedTask("discover");
                capabilities.addSupportedTask("analyze");
                capabilities.addSupportedDataSection("areas");
                capabilities.addSupportedDataSection("insights");
                capabilities.setPriority(30);
                capabilities.setConfigurationValue("requiresLLM", true);
                capabilities.setConfigurationValue("providesInsights", true);
                break;
                
            case "editor":
                capabilities.addSupportedTask("edit");
                capabilities.addSupportedTask("modify");
                capabilities.addSupportedTask("update");
                capabilities.addSupportedDataSection("content");
                capabilities.addSupportedDataSection("text");
                capabilities.setPriority(50);
                capabilities.setConfigurationValue("handlesUserRequests", true);
                break;
                
            default:
                logger.warn("Unknown agent type: {}", agentType);
        }
        
        return capabilities;
    }
    
    /**
     * Validate agent capability matching.
     */
    public static boolean validateCapabilityMatching(AgentCapabilities capabilities, String taskType) {
        return capabilities.getSupportedTasks().contains(taskType);
    }
    
    /**
     * Detect capability conflicts between agents.
     */
    public static List<String> detectCapabilityConflicts(AgentCapabilities caps1, AgentCapabilities caps2) {
        return caps1.getSupportedTasks().stream()
                .filter(task -> caps2.getSupportedTasks().contains(task))
                .toList();
    }
    
    /**
     * Create agent data assertion map for testing.
     */
    public static Map<String, Object> createAgentDataAssertion(String agentType) {
        Map<String, Object> agentData = new HashMap<>();
        
        switch (agentType.toLowerCase()) {
            case "PLANNER":
                agentData.put("preferences", Map.of("budget", "medium", "style", "adventure"));
                agentData.put("constraints", Map.of("maxDuration", 7, "accessibility", true));
                agentData.put("generated", true);
                break;
                
            case "booking":
                agentData.put("bookingStatus", "confirmed");
                agentData.put("paymentMethod", "credit_card");
                agentData.put("confirmationNumber", "CONF-12345");
                break;
                
            case "ENRICHMENT":
                agentData.put("enrichmentLevel", "detailed");
                agentData.put("validationStatus", "passed");
                agentData.put("lastEnriched", "2024-01-15T10:00:00Z");
                break;
                
            case "places":
                agentData.put("discoveryRadius", 5.0);
                agentData.put("analysisDepth", "comprehensive");
                agentData.put("insights", Arrays.asList("popular", "local_favorite"));
                break;
                
            case "editor":
                agentData.put("editHistory", Arrays.asList("initial_creation", "user_modification"));
                agentData.put("lastModified", "2024-01-15T10:00:00Z");
                agentData.put("modificationCount", 2);
                break;
                
            default:
                logger.warn("Unknown agent type for data assertion: {}", agentType);
        }
        
        return agentData;
    }
    
    /**
     * Validate agent data structure.
     */
    public static boolean validateAgentDataStructure(Map<String, Object> agentData, String expectedAgentType) {
        if (agentData == null || agentData.isEmpty()) {
            return false;
        }
        
        // Basic validation - ensure data contains expected keys for agent type
        switch (expectedAgentType.toLowerCase()) {
            case "PLANNER":
                return agentData.containsKey("preferences") || agentData.containsKey("constraints");
            case "booking":
                return agentData.containsKey("bookingStatus") || agentData.containsKey("paymentMethod");
            case "ENRICHMENT":
                return agentData.containsKey("enrichmentLevel") || agentData.containsKey("validationStatus");
            case "places":
                return agentData.containsKey("discoveryRadius") || agentData.containsKey("analysisDepth");
            case "editor":
                return agentData.containsKey("editHistory") || agentData.containsKey("lastModified");
            default:
                return true; // Allow unknown agent types
        }
    }
    
    /**
     * Create agent request context for testing.
     */
    public static Map<String, Object> createAgentRequestContext(String contextType) {
        Map<String, Object> context = new HashMap<>();
        
        switch (contextType.toLowerCase()) {
            case "itinerary_creation":
                context.put("destination", "Bali, Indonesia");
                context.put("duration", 7);
                context.put("budget", "medium");
                context.put("travelers", 2);
                break;
                
            case "booking_request":
                context.put("itemType", "hotel");
                context.put("checkIn", "2024-03-15");
                context.put("checkOut", "2024-03-17");
                context.put("guests", 2);
                break;
                
            case "enrichment_request":
                context.put("placeId", "ChIJoQ8Q6NNB0i0RkOYkS7EPkSQ");
                context.put("enrichmentType", "details");
                context.put("includePhotos", true);
                break;
                
            case "editing_request":
                context.put("changeType", "modify_node");
                context.put("nodeId", "node-001");
                context.put("userRequest", "Make this activity longer");
                break;
                
            default:
                context.put("type", contextType);
        }
        
        return context;
    }
}