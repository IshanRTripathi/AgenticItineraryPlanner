package com.tripplanner.service;

import com.tripplanner.dto.NormalizedDay;
import com.tripplanner.dto.NormalizedItinerary;
import com.tripplanner.dto.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Centralized service for generating unique node IDs across the application.
 * Provides consistent ID generation patterns and ensures all nodes have valid IDs.
 */
@Service
public class NodeIdGenerator {
    
    private static final Logger logger = LoggerFactory.getLogger(NodeIdGenerator.class);
    private static final String NODE_PREFIX = "node";
    
    /**
     * Primary ID generation method for general use.
     * Generates IDs in format: day{N}_node{M} where N is day number and M is sequential node number.
     * 
     * @param nodeType The type of the node (e.g., "attraction", "meal", "transport")
     * @param dayNumber The day number
     * @param itinerary The itinerary context to find next available node number
     * @return A unique sequential node ID
     */
    public synchronized String generateNodeId(String nodeType, Integer dayNumber, NormalizedItinerary itinerary) {
        if (dayNumber == null) {
            dayNumber = 1; // Default to day 1
        }
        
        int nextNodeNumber = findNextNodeNumber(itinerary, dayNumber);
        String nodeId = String.format("day%d_node%d", dayNumber, nextNodeNumber);
        
        logger.debug("Generated ID for node type '{}' on day {}: {}", nodeType, dayNumber, nodeId);
        return nodeId;
    }
    
    /**
     * Legacy method for backward compatibility.
     * Generates IDs in old format: node_{type}_{day}_{timestamp}_{uuid}
     * 
     * @deprecated Use generateNodeId(String, Integer, NormalizedItinerary) instead
     */
    @Deprecated
    public String generateNodeId(String nodeType, Integer dayNumber) {
        long timestamp = System.currentTimeMillis();
        String typePrefix = sanitizeType(nodeType);
        String dayPart = dayNumber != null ? "_day" + dayNumber : "";
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        return String.format("%s_%s%s_%d_%s", NODE_PREFIX, typePrefix, dayPart, timestamp % 10000, uuid);
    }
    
    /**
     * Generate predictable IDs for skeleton nodes.
     * Used during initial itinerary creation for sequential, predictable IDs.
     * Format: day{number}_node{index} (same as generateNodeId)
     * 
     * @param dayNumber The day number
     * @param nodeIndex The index of the node within the day
     * @param nodeType The type of the node (not used in new format, kept for compatibility)
     * @return A predictable skeleton node ID
     */
    public String generateSkeletonNodeId(int dayNumber, int nodeIndex, String nodeType) {
        String nodeId = String.format("day%d_node%d", dayNumber, nodeIndex);
        logger.debug("Generated skeleton ID for node type '{}' on day {}: {}", nodeType, dayNumber, nodeId);
        return nodeId;
    }
    
    /**
     * Ensure a node has a valid ID, generating one if needed.
     * This method is idempotent - it won't overwrite existing valid IDs.
     * 
     * @param node The node to ensure has an ID
     * @param dayNumber The day number for context
     * @param itinerary The itinerary context to find next available node number
     */
    public void ensureNodeHasId(NormalizedNode node, Integer dayNumber, NormalizedItinerary itinerary) {
        if (node == null) {
            logger.warn("Cannot ensure ID for null node");
            return;
        }
        
        if (node.getId() == null || node.getId().trim().isEmpty()) {
            String generatedId = generateNodeId(node.getType(), dayNumber, itinerary);
            node.setId(generatedId);
            logger.debug("Generated ID for node: {} -> {}", node.getTitle(), generatedId);
        }
    }
    
    /**
     * Legacy method for backward compatibility.
     * 
     * @deprecated Use ensureNodeHasId(NormalizedNode, Integer, NormalizedItinerary) instead
     */
    @Deprecated
    public void ensureNodeHasId(NormalizedNode node, Integer dayNumber) {
        if (node == null) {
            logger.warn("Cannot ensure ID for null node");
            return;
        }
        
        if (node.getId() == null || node.getId().trim().isEmpty()) {
            String generatedId = generateNodeId(node.getType(), dayNumber);
            node.setId(generatedId);
            logger.debug("Generated ID for node: {} -> {}", node.getTitle(), generatedId);
        }
    }
    
    /**
     * Find the next available node number for a given day.
     * Scans existing nodes in the day and returns max + 1.
     * 
     * @param itinerary The itinerary to search
     * @param dayNumber The day number
     * @return The next available sequential node number
     */
    private int findNextNodeNumber(NormalizedItinerary itinerary, int dayNumber) {
        if (itinerary == null || itinerary.getDays() == null) {
            return 1;
        }
        
        NormalizedDay targetDay = findDay(itinerary, dayNumber);
        if (targetDay == null || targetDay.getNodes() == null || targetDay.getNodes().isEmpty()) {
            return 1;
        }
        
        int maxNodeNumber = 0;
        for (NormalizedNode node : targetDay.getNodes()) {
            int nodeNumber = extractNodeNumber(node.getId());
            maxNodeNumber = Math.max(maxNodeNumber, nodeNumber);
        }
        
        return maxNodeNumber + 1;
    }
    
    /**
     * Extract node number from a node ID.
     * Supports both new format (day{N}_node{M}) and old formats.
     * 
     * @param nodeId The node ID to parse
     * @return The node number, or 0 if not found
     */
    private int extractNodeNumber(String nodeId) {
        if (nodeId == null || nodeId.trim().isEmpty()) {
            return 0;
        }
        
        // Try new format: day{N}_node{M}
        Pattern newPattern = Pattern.compile("day\\d+_node(\\d+)");
        Matcher newMatcher = newPattern.matcher(nodeId);
        if (newMatcher.find()) {
            return Integer.parseInt(newMatcher.group(1));
        }
        
        // Try old skeleton format: day{N}_{type}_{M}
        Pattern oldSkeletonPattern = Pattern.compile("day\\d+_\\w+_(\\d+)");
        Matcher oldSkeletonMatcher = oldSkeletonPattern.matcher(nodeId);
        if (oldSkeletonMatcher.find()) {
            return Integer.parseInt(oldSkeletonMatcher.group(1));
        }
        
        // Old format doesn't have sequential numbers, return 0
        return 0;
    }
    
    /**
     * Find a day by day number in the itinerary.
     * 
     * @param itinerary The itinerary to search
     * @param dayNumber The day number to find
     * @return The day, or null if not found
     */
    private NormalizedDay findDay(NormalizedItinerary itinerary, int dayNumber) {
        if (itinerary == null || itinerary.getDays() == null) {
            return null;
        }
        
        return itinerary.getDays().stream()
                .filter(day -> day.getDayNumber() != null && day.getDayNumber() == dayNumber)
                .findFirst()
                .orElse(null);
    }
    
    /**
     * Sanitize node type to create a consistent prefix.
     * Takes first 3 characters of the type, or "unk" if null/empty.
     * 
     * @param nodeType The node type to sanitize
     * @return A sanitized 3-character prefix
     */
    private String sanitizeType(String nodeType) {
        if (nodeType == null || nodeType.trim().isEmpty()) {
            return "unk";
        }
        return nodeType.trim().substring(0, Math.min(3, nodeType.trim().length()));
    }
    
    /**
     * Validate that an ID follows the expected format.
     * Useful for debugging and testing.
     * 
     * @param id The ID to validate
     * @return true if the ID appears to be valid
     */
    public boolean isValidNodeId(String id) {
        if (id == null || id.trim().isEmpty()) {
            return false;
        }
        
        // Check for new standardized format: day{number}_node{number}
        if (id.matches("day\\d+_node\\d+")) {
            return true;
        }
        
        // Check for old skeleton format: day{number}_{type}_{index}
        if (id.matches("day\\d+_\\w+_\\d+")) {
            return true;
        }
        
        // Check for old general format with day: node_{type}_day{n}_{timestamp}_{uuid}
        if (id.matches("node_\\w+_day\\d+_\\d+_[a-z0-9]{8}")) {
            return true;
        }
        
        // Check for old general format without day: node_{type}_{timestamp}_{uuid}
        if (id.matches("node_\\w+_\\d+_[a-z0-9]{8}")) {
            return true;
        }
        
        return false;
    }
}
