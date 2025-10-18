package com.tripplanner.service;

import com.tripplanner.dto.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

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
     * Generates IDs in format: node_{type}_{day}_{timestamp}_{uuid}
     * 
     * @param nodeType The type of the node (e.g., "attraction", "meal", "transport")
     * @param dayNumber The day number (can be null for non-day-specific nodes)
     * @return A unique node ID
     */
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
     * Format: day{number}_{type}_{index}
     * 
     * @param dayNumber The day number
     * @param nodeIndex The index of the node within the day
     * @param nodeType The type of the node
     * @return A predictable skeleton node ID
     */
    public String generateSkeletonNodeId(int dayNumber, int nodeIndex, String nodeType) {
        String typePrefix = sanitizeType(nodeType);
        return String.format("day%d_%s_%d", dayNumber, typePrefix, nodeIndex);
    }
    
    /**
     * Ensure a node has a valid ID, generating one if needed.
     * This method is idempotent - it won't overwrite existing valid IDs.
     * 
     * @param node The node to ensure has an ID
     * @param dayNumber The day number for context (can be null)
     */
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
        
        // Check for skeleton format: day{number}_{type}_{index}
        if (id.matches("day\\d+_\\w+_\\d+")) {
            return true;
        }
        
        // Check for general format with day: node_{type}_day{n}_{timestamp}_{uuid}
        if (id.matches("node_\\w+_day\\d+_\\d+_[a-z0-9]{8}")) {
            return true;
        }
        
        // Check for general format without day: node_{type}_{timestamp}_{uuid}
        if (id.matches("node_\\w+_\\d+_[a-z0-9]{8}")) {
            return true;
        }
        
        return false;
    }
}
