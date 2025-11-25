package com.tripplanner.util;

import com.tripplanner.dto.NormalizedDay;
import com.tripplanner.dto.NormalizedItinerary;
import com.tripplanner.dto.NormalizedNode;

import java.util.List;

/**
 * Utility for counting activities in itineraries.
 * 
 * CRITICAL: Only attractions and meals count as "activities" for user-facing displays.
 * Transport and accommodation are infrastructure, not activities.
 * 
 * This ensures consistent activity counting across:
 * - WebSocket progress events
 * - UI displays
 * - Cost estimation
 * - Analytics
 */
public class ActivityCounter {
    
    /**
     * Count activities in a single day.
     * Only counts attractions and meals (not transport or accommodation).
     * 
     * @param day The day to count activities for
     * @return Number of activities (attractions + meals)
     */
    public static int countActivities(NormalizedDay day) {
        if (day == null || day.getNodes() == null) {
            return 0;
        }
        
        return (int) day.getNodes().stream()
            .filter(ActivityCounter::isActivity)
            .count();
    }
    
    /**
     * Count activities across entire itinerary.
     * 
     * @param itinerary The itinerary to count activities for
     * @return Total number of activities across all days
     */
    public static int countActivities(NormalizedItinerary itinerary) {
        if (itinerary == null || itinerary.getDays() == null) {
            return 0;
        }
        
        return itinerary.getDays().stream()
            .mapToInt(ActivityCounter::countActivities)
            .sum();
    }
    
    /**
     * Count activities in a list of nodes.
     * 
     * @param nodes List of nodes to count
     * @return Number of activities
     */
    public static int countActivities(List<NormalizedNode> nodes) {
        if (nodes == null) {
            return 0;
        }
        
        return (int) nodes.stream()
            .filter(ActivityCounter::isActivity)
            .count();
    }
    
    /**
     * Check if a node is an activity (attraction or meal).
     * 
     * @param node The node to check
     * @return true if node is an attraction or meal, false otherwise
     */
    public static boolean isActivity(NormalizedNode node) {
        if (node == null || node.getType() == null) {
            return false;
        }
        
        String type = node.getType().toLowerCase();
        return "attraction".equals(type) || 
               "activity".equals(type) || 
               "meal".equals(type) ||
               "restaurant".equals(type);
    }
    
    /**
     * Check if a node is infrastructure (transport or accommodation).
     * 
     * @param node The node to check
     * @return true if node is transport or accommodation, false otherwise
     */
    public static boolean isInfrastructure(NormalizedNode node) {
        if (node == null || node.getType() == null) {
            return false;
        }
        
        String type = node.getType().toLowerCase();
        return "transport".equals(type) || 
               "transportation".equals(type) ||
               "accommodation".equals(type) ||
               "hotel".equals(type);
    }
}
