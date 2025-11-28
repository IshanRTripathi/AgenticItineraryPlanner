package com.tripplanner.agents;

import com.tripplanner.dto.*;
import com.tripplanner.service.GooglePlacesService;
import com.tripplanner.service.ItineraryJsonService;
import com.tripplanner.service.agents.AgentEventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Agent for searching places with photos, ratings, and details.
 * Handles user requests like "show me more tourist places" or "find museums".
 */
@Component
public class PlaceSearchAgent extends BaseAgent {
    
    private static final Logger logger = LoggerFactory.getLogger(PlaceSearchAgent.class);
    
    private final GooglePlacesService placesService;
    private final ItineraryJsonService itineraryJsonService;
    
    public PlaceSearchAgent(
            GooglePlacesService placesService,
            ItineraryJsonService itineraryJsonService,
            AgentEventBus eventBus) {
        super(eventBus, AgentEvent.AgentKind.EXPLAINER); // Using EXPLAINER for now, can add SEARCH later
        this.placesService = placesService;
        this.itineraryJsonService = itineraryJsonService;
    }
    
    @Override
    public AgentCapabilities getCapabilities() {
        AgentCapabilities capabilities = new AgentCapabilities();
        capabilities.addSupportedTask("search");
        capabilities.setPriority(5);
        capabilities.setChatEnabled(true);
        return capabilities;
    }
    
    @Override
    protected String getAgentName() {
        return "PlaceSearchAgent";
    }
    
    @Override
    protected <T> T executeInternal(String itineraryId, AgentRequest<T> request) {
        logger.info("=== PLACE SEARCH AGENT PROCESSING ===");
        
        // Extract parameters from request data
        Object data = request.getData();
        Map<String, Object> params = data instanceof Map ? (Map<String, Object>) data : Map.of();
        
        String query = (String) params.get("query");
        String location = (String) params.get("location");
        String placeType = (String) params.get("placeType");
        Integer day = (Integer) params.get("day");
        
        logger.info("Search parameters: query='{}', location='{}', type='{}', day={}", 
            query, location, placeType, day);
        
        // If location not provided, get from itinerary
        if (location == null || location.isEmpty()) {
            location = getLocationFromItinerary(itineraryId, day);
        }
        
        // If query not provided, use placeType
        if (query == null || query.isEmpty()) {
            query = placeType != null ? placeType : "tourist attraction";
        }
        
        // Search places using GooglePlacesService
        List<PlaceSuggestion> suggestions = placesService.searchPlaces(
            itineraryId,
            query,
            location,
            placeType,
            3  // Always return 3 suggestions for chat
        );
        
        // Set the day number on each suggestion so frontend knows which day to add to
        if (day != null) {
            for (PlaceSuggestion suggestion : suggestions) {
                suggestion.setDay(day);
            }
        }
        
        logger.info("Found {} place suggestions for day {}", suggestions.size(), day);
        
        // Build chat response with suggestions
        ChatResponse response = new ChatResponse();
        response.setIntent("SEARCH_PLACE");
        response.setPlaceSuggestions(suggestions);
        response.setNeedsDisambiguation(true); // User needs to select one
        
        // Build friendly message
        String placeTypeLabel = placeType != null ? placeType : "place";
        String dayLabel = day != null ? " for Day " + day : "";
        
        if (suggestions.isEmpty()) {
            response.setMessage("I couldn't find any " + placeTypeLabel + "s" + dayLabel + " in " + location + ". Try a different search?");
        } else {
            response.setMessage("Here are " + suggestions.size() + " " + placeTypeLabel + " suggestions" + dayLabel + ":");
        }
        
        return (T) response;
    }
    
    /**
     * Get location from itinerary for a specific day.
     */
    private String getLocationFromItinerary(String itineraryId, Integer day) {
        try {
            java.util.Optional<NormalizedItinerary> itinerary = itineraryJsonService.getItinerary(itineraryId);
            if (itinerary.isPresent()) {
                NormalizedItinerary itin = itinerary.get();
                
                // If day specified, try to get location for that day
                if (day != null && day > 0 && day <= itin.getDays().size()) {
                    NormalizedDay dayData = itin.getDays().get(day - 1);
                    if (dayData.getLocation() != null && !dayData.getLocation().isEmpty()) {
                        return dayData.getLocation();
                    }
                }
                
                // Fallback to destination
                return itin.getDestination();
            }
        } catch (Exception e) {
            logger.error("Failed to get location from itinerary", e);
        }
        
        return null;
    }
}
