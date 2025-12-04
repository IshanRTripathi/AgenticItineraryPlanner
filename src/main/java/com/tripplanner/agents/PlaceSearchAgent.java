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
        
        // Get existing places context to avoid duplicates
        // Pass the full query text to extract city name if needed
        String userQuery = (String) params.get("text");
        if (userQuery == null || userQuery.isEmpty()) {
            userQuery = query; // Fallback to processed query
        }
        
        String existingPlacesContext = getExistingPlacesContext(itineraryId, location, placeType, userQuery);
        if (!existingPlacesContext.isEmpty()) {
            logger.info("📋 Existing places context: {}", existingPlacesContext);
        }
        
        // Get existing place identifiers for filtering
        java.util.List<PlaceIdentifier> existingPlaceIdentifiers = getExistingPlaceIdentifiers(itineraryId, location, placeType, userQuery);
        
        // Search places using GooglePlacesService
        // Request more than needed since we'll filter out duplicates
        List<PlaceSuggestion> allSuggestions = placesService.searchPlaces(
            itineraryId,
            query,
            location,
            placeType,
            10  // Request 10 to have enough after filtering
        );
        
        // Filter out places that are already in the itinerary
        List<PlaceSuggestion> filteredSuggestions = new java.util.ArrayList<>();
        for (PlaceSuggestion suggestion : allSuggestions) {
            if (!isPlaceAlreadyCovered(suggestion, existingPlaceIdentifiers)) {
                filteredSuggestions.add(suggestion);
                if (filteredSuggestions.size() >= 3) {
                    break; // Stop once we have 3 unique suggestions
                }
            }
        }
        
        // If we still don't have enough after filtering, add remaining suggestions
        if (filteredSuggestions.size() < 3) {
            logger.warn("Only found {} unique suggestions after filtering, adding {} duplicates", 
                filteredSuggestions.size(), Math.min(3 - filteredSuggestions.size(), allSuggestions.size() - filteredSuggestions.size()));
            for (PlaceSuggestion suggestion : allSuggestions) {
                if (!filteredSuggestions.contains(suggestion) && filteredSuggestions.size() < 3) {
                    filteredSuggestions.add(suggestion);
                }
            }
        }
        
        List<PlaceSuggestion> suggestions = filteredSuggestions;
        
        // Set the day number on each suggestion so frontend knows which day to add to
        if (day != null) {
            for (PlaceSuggestion suggestion : suggestions) {
                suggestion.setDay(day);
            }
        }
        
        logger.info("Found {} place suggestions for day {} ({} filtered out as duplicates)", 
            suggestions.size(), day, allSuggestions.size() - suggestions.size());
        
        // Build chat response with suggestions
        ChatResponse response = new ChatResponse();
        response.setIntent("SEARCH_PLACE");
        response.setPlaceSuggestions(suggestions);
        response.setNeedsDisambiguation(true); // User needs to select one
        
        // Build friendly message with context about existing places
        String placeTypeLabel = placeType != null ? placeType : "place";
        String dayLabel = day != null ? " for Day " + day : "";
        
        if (suggestions.isEmpty()) {
            response.setMessage("I couldn't find any " + placeTypeLabel + "s" + dayLabel + " in " + location + ". Try a different search?");
        } else {
            String message = "Here are " + suggestions.size() + " " + placeTypeLabel + " suggestions" + dayLabel + ":";
            if (!existingPlacesContext.isEmpty()) {
                message += "\n\n" + existingPlacesContext;
            }
            response.setMessage(message);
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
    
    /**
     * Get context about existing places in the itinerary to avoid duplicates.
     * Filters by city (from location parameter or user query) and category.
     * 
     * @param itineraryId The itinerary ID
     * @param location The city/location to filter by (can be null)
     * @param placeType The type of place (hotel, restaurant, attraction)
     * @param userQuery The original user query to extract city name if location is null
     * @return Formatted context string with existing places
     */
    private String getExistingPlacesContext(String itineraryId, String location, String placeType, String userQuery) {
        try {
            java.util.Optional<NormalizedItinerary> itinerary = itineraryJsonService.getItinerary(itineraryId);
            if (itinerary.isEmpty()) {
                return "";
            }
            
            NormalizedItinerary itin = itinerary.get();
            
            // Extract city name from location or user query
            String targetCity = extractCityName(location, userQuery, itin);
            if (targetCity == null || targetCity.isEmpty()) {
                logger.warn("Could not determine target city for place search");
                return "";
            }
            
            logger.info("🏙️ Filtering existing places for city: {}", targetCity);
            
            // Determine category to filter by (default to attraction)
            String category = determineCategoryFromType(placeType);
            logger.info("📂 Filtering by category: {}", category);
            
            // Collect existing places filtered by city and category
            java.util.List<String> existingPlaces = new java.util.ArrayList<>();
            
            for (NormalizedDay dayData : itin.getDays()) {
                if (dayData.getNodes() == null) continue;
                
                // Check if this day belongs to the target city
                String dayLocation = dayData.getLocation();
                if (dayLocation == null || !cityMatches(dayLocation, targetCity)) {
                    continue; // Skip days not in target city
                }
                
                logger.debug("✓ Day {} matches city {}", dayData.getDayNumber(), targetCity);
                
                for (NormalizedNode node : dayData.getNodes()) {
                    // Filter by category
                    if (!matchesCategory(node, category)) {
                        continue;
                    }
                    
                    // Add place name with location
                    if (node.getTitle() != null && !node.getTitle().isEmpty()) {
                        String placeInfo = node.getTitle();
                        if (node.getLocation() != null && node.getLocation().getAddress() != null) {
                            placeInfo += " (" + node.getLocation().getAddress() + ")";
                        }
                        existingPlaces.add(placeInfo);
                    }
                }
            }
            
            // Build context string
            if (!existingPlaces.isEmpty()) {
                StringBuilder context = new StringBuilder();
                context.append("These places in ").append(targetCity).append(" are already covered: ");
                context.append(String.join(", ", existingPlaces));
                context.append(". Please suggest different ").append(category).append("s not in this list.");
                
                logger.info("📋 Found {} existing places in {} for category {}", 
                    existingPlaces.size(), targetCity, category);
                
                return context.toString();
            }
            
            logger.info("ℹ️ No existing places found in {} for category {}", targetCity, category);
            return "";
            
        } catch (Exception e) {
            logger.error("Failed to get existing places context", e);
            return "";
        }
    }
    
    /**
     * Extract city name from location parameter, user query, or itinerary.
     */
    private String extractCityName(String location, String userQuery, NormalizedItinerary itinerary) {
        // Priority 1: Use provided location
        if (location != null && !location.isEmpty()) {
            return normalizeCityName(location);
        }
        
        // Priority 2: Extract from user query (e.g., "show me museums in Lucerne")
        if (userQuery != null && !userQuery.isEmpty()) {
            String extracted = extractCityFromQuery(userQuery);
            if (extracted != null) {
                return extracted;
            }
        }
        
        // Priority 3: Use itinerary destination as fallback
        if (itinerary.getDestination() != null) {
            return normalizeCityName(itinerary.getDestination());
        }
        
        return null;
    }
    
    /**
     * Extract city name from user query using common patterns.
     */
    private String extractCityFromQuery(String query) {
        String lowerQuery = query.toLowerCase();
        
        // Pattern: "in [city]" or "for [city]"
        String[] patterns = {" in ", " for ", " at ", " near "};
        for (String pattern : patterns) {
            int index = lowerQuery.indexOf(pattern);
            if (index != -1) {
                String afterPattern = query.substring(index + pattern.length()).trim();
                // Extract first word/phrase (up to next space or end)
                String[] words = afterPattern.split("\\s+");
                if (words.length > 0) {
                    return normalizeCityName(words[0]);
                }
            }
        }
        
        return null;
    }
    
    /**
     * Normalize city name by removing common suffixes and trimming.
     */
    private String normalizeCityName(String city) {
        if (city == null) return null;
        
        // Remove country names and common suffixes
        city = city.replaceAll(",.*", "").trim(); // Remove ", Country"
        city = city.replaceAll("\\s+(India|Switzerland|USA|UK|France|Italy|Spain)$", "").trim();
        
        return city;
    }
    
    /**
     * Check if day location matches target city (case-insensitive, partial match).
     */
    private boolean cityMatches(String dayLocation, String targetCity) {
        if (dayLocation == null || targetCity == null) return false;
        
        String normalizedDay = normalizeCityName(dayLocation).toLowerCase();
        String normalizedTarget = targetCity.toLowerCase();
        
        return normalizedDay.contains(normalizedTarget) || normalizedTarget.contains(normalizedDay);
    }
    
    /**
     * Determine category from place type.
     * Maps user-friendly types to internal categories.
     */
    private String determineCategoryFromType(String placeType) {
        if (placeType == null || placeType.isEmpty()) {
            return "attraction"; // Default
        }
        
        String lower = placeType.toLowerCase();
        
        // Hotel/Accommodation
        if (lower.contains("hotel") || lower.contains("accommodation") || 
            lower.contains("stay") || lower.contains("lodging")) {
            return "hotel";
        }
        
        // Restaurant/Food
        if (lower.contains("restaurant") || lower.contains("food") || 
            lower.contains("dining") || lower.contains("cafe") || 
            lower.contains("meal") || lower.contains("eat")) {
            return "restaurant";
        }
        
        // Everything else is attraction (museums, landmarks, activities, etc.)
        return "attraction";
    }
    
    /**
     * Data class to hold place identification info for duplicate detection.
     */
    private static class PlaceIdentifier {
        String placeId;
        Coordinates coordinates;
        String name;
        
        PlaceIdentifier(String placeId, Coordinates coordinates, String name) {
            this.placeId = placeId;
            this.coordinates = coordinates;
            this.name = name;
        }
    }
    
    /**
     * Get set of existing place identifiers for filtering duplicates.
     * Uses placeId and coordinates for accurate matching.
     */
    private java.util.List<PlaceIdentifier> getExistingPlaceIdentifiers(String itineraryId, String location, String placeType, String userQuery) {
        java.util.List<PlaceIdentifier> identifiers = new java.util.ArrayList<>();
        
        try {
            java.util.Optional<NormalizedItinerary> itinerary = itineraryJsonService.getItinerary(itineraryId);
            if (itinerary.isEmpty()) {
                return identifiers;
            }
            
            NormalizedItinerary itin = itinerary.get();
            String targetCity = extractCityName(location, userQuery, itin);
            if (targetCity == null) {
                return identifiers;
            }
            
            String category = determineCategoryFromType(placeType);
            
            for (NormalizedDay dayData : itin.getDays()) {
                if (dayData.getNodes() == null) continue;
                
                String dayLocation = dayData.getLocation();
                if (dayLocation == null || !cityMatches(dayLocation, targetCity)) {
                    continue;
                }
                
                for (NormalizedNode node : dayData.getNodes()) {
                    if (!matchesCategory(node, category)) {
                        continue;
                    }
                    
                    // Extract place identification data
                    String placeId = null;
                    Coordinates coords = null;
                    
                    if (node.getLocation() != null) {
                        placeId = node.getLocation().getPlaceId();
                        coords = node.getLocation().getCoordinates();
                    }
                    
                    identifiers.add(new PlaceIdentifier(placeId, coords, node.getTitle()));
                }
            }
        } catch (Exception e) {
            logger.error("Failed to get existing place identifiers", e);
        }
        
        return identifiers;
    }
    
    /**
     * Check if a place suggestion is already covered in the itinerary.
     * Uses hybrid approach: placeId (100% accurate) and coordinates (99% accurate).
     */
    private boolean isPlaceAlreadyCovered(PlaceSuggestion suggestion, java.util.List<PlaceIdentifier> existingPlaces) {
        if (suggestion == null || existingPlaces.isEmpty()) {
            return false;
        }
        
        String suggestionPlaceId = suggestion.getPlaceId();
        Coordinates suggestionCoords = null;
        if (suggestion.getGeometry() != null && suggestion.getGeometry().getLocation() != null) {
            suggestionCoords = suggestion.getGeometry().getLocation();
        }
        
        for (PlaceIdentifier existing : existingPlaces) {
            // Priority 1: Check placeId (most reliable)
            if (suggestionPlaceId != null && existing.placeId != null) {
                if (suggestionPlaceId.equals(existing.placeId)) {
                    logger.debug("🚫 Filtering duplicate (placeId match): {} == {}", 
                        suggestion.getName(), existing.name);
                    return true;
                }
            }
            
            // Priority 2: Check coordinates (within 100 meters = same place)
            if (suggestionCoords != null && existing.coordinates != null) {
                double distance = calculateDistance(suggestionCoords, existing.coordinates);
                if (distance < 0.1) { // Less than 100 meters
                    logger.debug("🚫 Filtering duplicate (coordinate match): {} ≈ {} ({}m apart)", 
                        suggestion.getName(), existing.name, Math.round(distance * 1000));
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Calculate distance between two coordinates in kilometers using Haversine formula.
     */
    private double calculateDistance(Coordinates coord1, Coordinates coord2) {
        if (coord1 == null || coord2 == null) {
            return Double.MAX_VALUE;
        }
        
        double lat1 = coord1.getLat();
        double lon1 = coord1.getLng();
        double lat2 = coord2.getLat();
        double lon2 = coord2.getLng();
        
        final int R = 6371; // Radius of the earth in km
        
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return R * c; // Distance in km
    }
    
    /**
     * Check if node matches the specified category.
     */
    private boolean matchesCategory(NormalizedNode node, String category) {
        String nodeType = node.getType() != null ? node.getType().toLowerCase() : "";
        String nodeCategory = "";
        
        // Get category from details if available
        if (node.getDetails() != null && node.getDetails().getCategory() != null) {
            nodeCategory = node.getDetails().getCategory().toLowerCase();
        }
        
        // Also check labels for category information
        String nodeLabels = "";
        if (node.getLabels() != null && !node.getLabels().isEmpty()) {
            nodeLabels = String.join(" ", node.getLabels()).toLowerCase();
        }
        
        switch (category) {
            case "hotel":
                return nodeType.contains("accommodation") || nodeType.contains("hotel") ||
                       nodeCategory.contains("accommodation") || nodeCategory.contains("hotel") ||
                       nodeLabels.contains("hotel") || nodeLabels.contains("accommodation");
                       
            case "restaurant":
                return nodeType.contains("meal") || nodeType.contains("restaurant") || 
                       nodeType.contains("food") || nodeType.contains("dining") ||
                       nodeCategory.contains("meal") || nodeCategory.contains("restaurant") ||
                       nodeCategory.contains("food") || nodeCategory.contains("dining") ||
                       nodeLabels.contains("restaurant") || nodeLabels.contains("food") ||
                       nodeLabels.contains("dining");
                       
            case "attraction":
                // Attraction is anything that's not hotel or restaurant
                return !matchesCategory(node, "hotel") && !matchesCategory(node, "restaurant");
                
            default:
                return true; // Include everything if category unknown
        }
    }
}
