package com.tripplanner.service;

import com.tripplanner.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service for resolving location context from itineraries.
 * Provides a single source of truth for location resolution using CityAllocationPlan.
 * 
 * This service centralizes the logic for building "City, Country" format locations
 * that are used for accurate geocoding in Google Places API.
 */
@Service
public class LocationResolutionService {
    
    private static final Logger logger = LoggerFactory.getLogger(LocationResolutionService.class);
    
    /**
     * Resolve the location for a specific node.
     * Returns "City, Country" format for accurate geocoding.
     * 
     * @param itinerary The itinerary containing the node
     * @param nodeId The node ID to resolve location for
     * @return Location in "City, Country" format, or null if not found
     */
    public String resolveNodeLocation(NormalizedItinerary itinerary, String nodeId) {
        Integer dayNumber = findDayNumberForNode(itinerary, nodeId);
        if (dayNumber == null) {
            logger.warn("Could not find day number for node {}", nodeId);
            return null;
        }
        
        return resolveDayLocation(itinerary, dayNumber);
    }
    
    /**
     * Resolve the location for a specific day.
     * Returns "City, Country" format for accurate geocoding.
     * 
     * Strategy:
     * 1. Get city from CityAllocationPlan (source of truth)
     * 2. Extract country from itinerary destination
     * 3. Build "City, Country" format (e.g., "Zurich, Switzerland")
     * 
     * @param itinerary The itinerary
     * @param dayNumber The day number to resolve location for
     * @return Location in "City, Country" format
     */
    public String resolveDayLocation(NormalizedItinerary itinerary, Integer dayNumber) {
        if (dayNumber == null) {
            logger.warn("Day number is null, cannot resolve location");
            return itinerary.getDestination(); // Fallback to destination
        }
        
        // Get city from CityAllocationPlan (source of truth)
        String city = getCityFromAllocationPlan(itinerary, dayNumber);
        
        // Extract country from destination
        String country = extractCountryName(itinerary.getDestination());
        
        // Build "City, Country" format
        return buildCityCountryFormat(city, country);
    }
    
    /**
     * Get the city name from CityAllocationPlan for a specific day.
     * This is the SOURCE OF TRUTH for city names.
     * 
     * @param itinerary The itinerary containing the CityAllocationPlan
     * @param dayNumber The day number to find the city for
     * @return The city name or null if not found
     */
    public String getCityFromAllocationPlan(NormalizedItinerary itinerary, Integer dayNumber) {
        if (dayNumber == null) {
            logger.warn("⚠️ [getCityFromAllocationPlan] dayNumber is null");
            return null;
        }
        
        try {
            logger.debug("🔍 [getCityFromAllocationPlan] Looking for city for day {} in CityAllocationPlan", dayNumber);
            
            if (itinerary.getAgentData() == null || !itinerary.getAgentData().containsKey("cityAllocation")) {
                logger.warn("⚠️ [getCityFromAllocationPlan] No agentData or cityAllocation section found");
                return null;
            }
            
            AgentDataSection agentDataSection = itinerary.getAgentData().get("cityAllocation");
            CityAllocationPlan cityPlan = agentDataSection.getAgentData("cityAllocation", CityAllocationPlan.class);
            
            if (cityPlan == null || cityPlan.getAllocations() == null) {
                logger.warn("⚠️ [getCityFromAllocationPlan] CityAllocationPlan is null or has no allocations");
                return null;
            }
            
            logger.debug("📋 [getCityFromAllocationPlan] Found {} city allocations", cityPlan.getAllocations().size());
            
            // Find the city allocation for this day
            for (CityAllocation allocation : cityPlan.getAllocations()) {
                if (dayNumber >= allocation.getStartDay() && dayNumber <= allocation.getEndDay()) {
                    logger.debug("✅ [getCityFromAllocationPlan] Found city '{}' for day {}", 
                               allocation.getCityName(), dayNumber);
                    return allocation.getCityName();
                }
            }
            
            logger.warn("⚠️ [getCityFromAllocationPlan] No city allocation found for day {}", dayNumber);
            return null;
        } catch (Exception e) {
            logger.error("❌ [getCityFromAllocationPlan] Error getting city from CityAllocationPlan: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Extract country name from destination string.
     * Examples:
     * - "Switzerland" → "Switzerland"
     * - "Zurich, Switzerland" → "Switzerland"
     * - "Malaysia" → "Malaysia"
     * 
     * @param destination The destination string
     * @return The country name
     */
    public String extractCountryName(String destination) {
        if (destination == null || destination.trim().isEmpty()) {
            return "";
        }
        
        // If destination contains comma, take the last part (country)
        if (destination.contains(",")) {
            String[] parts = destination.split(",");
            return parts[parts.length - 1].trim();
        }
        
        // Otherwise, assume the whole destination is the country
        return destination.trim();
    }
    
    /**
     * Build "City, Country" format for accurate geocoding.
     * This format gives Google Geocoding API the best chance of finding the correct location.
     * 
     * @param city The city name from CityAllocationPlan
     * @param country The country name from destination
     * @return "City, Country" format or fallback to country if city is null
     */
    public String buildCityCountryFormat(String city, String country) {
        if (city != null && !city.trim().isEmpty()) {
            String result = city.trim() + ", " + country.trim();
            logger.debug("✅ Built location format: '{}'", result);
            return result;
        }
        
        logger.warn("⚠️ City is null/empty, falling back to country: '{}'", country);
        return country;
    }
    
    /**
     * Find the day number for a given node.
     * 
     * @param itinerary The itinerary containing the node
     * @param nodeId The node ID to find
     * @return The day number or null if not found
     */
    public Integer findDayNumberForNode(NormalizedItinerary itinerary, String nodeId) {
        if (itinerary.getDays() == null) {
            return null;
        }
        
        for (NormalizedDay day : itinerary.getDays()) {
            if (day.getNodes() != null) {
                for (NormalizedNode node : day.getNodes()) {
                    if (node.getId() != null && node.getId().equals(nodeId)) {
                        return day.getDayNumber();
                    }
                }
            }
        }
        return null;
    }
}
