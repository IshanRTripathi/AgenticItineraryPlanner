package com.tripplanner.service;

import com.tripplanner.dto.NormalizedDay;
import com.tripplanner.dto.NormalizedItinerary;
import com.tripplanner.dto.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * NodeIdValidator - Validates node IDs for uniqueness, format, and existence.
 * 
 * CRITICAL: Prevents node ID conflicts that can corrupt itinerary data.
 * 
 * Validation Rules:
 * 1. Format: Must match pattern "day{X}_node{Y}" or "day{X}_{type}_{Y}"
 * 2. Uniqueness: No duplicate IDs within an itinerary
 * 3. Existence: Referenced IDs must exist before operations
 * 4. Day consistency: Day number in ID must match actual day
 */
@Service
public class NodeIdValidator {
    
    private static final Logger logger = LoggerFactory.getLogger(NodeIdValidator.class);
    
    // Valid node ID patterns
    private static final Pattern SKELETON_PATTERN = Pattern.compile("^day\\d+_node\\d+$");
    private static final Pattern TYPED_PATTERN = Pattern.compile("^day\\d+_(att|mea|tra|acc)_\\d+$");
    private static final Pattern LEGACY_PATTERN = Pattern.compile("^node_[a-z]{3}_day\\d+_\\d+_[a-f0-9]{8}$");
    
    /**
     * Validate that a node ID follows the correct format.
     */
    public boolean isValidFormat(String nodeId) {
        if (nodeId == null || nodeId.trim().isEmpty()) {
            return false;
        }
        
        return SKELETON_PATTERN.matcher(nodeId).matches() ||
               TYPED_PATTERN.matcher(nodeId).matches() ||
               LEGACY_PATTERN.matcher(nodeId).matches();
    }
    
    /**
     * Validate that a node ID is unique within the itinerary.
     */
    public boolean isUnique(String nodeId, NormalizedItinerary itinerary) {
        if (nodeId == null || itinerary == null || itinerary.getDays() == null) {
            return false;
        }
        
        Set<String> existingIds = collectAllNodeIds(itinerary);
        return !existingIds.contains(nodeId);
    }
    
    /**
     * Validate that a node ID exists in the itinerary.
     */
    public boolean exists(String nodeId, NormalizedItinerary itinerary) {
        if (nodeId == null || itinerary == null || itinerary.getDays() == null) {
            return false;
        }
        
        Set<String> existingIds = collectAllNodeIds(itinerary);
        return existingIds.contains(nodeId);
    }
    
    /**
     * Validate that the day number in the node ID matches the actual day.
     */
    public boolean isDayConsistent(String nodeId, int dayNumber) {
        if (nodeId == null) {
            return false;
        }
        
        try {
            // Extract day number from ID
            if (nodeId.startsWith("day")) {
                int underscoreIndex = nodeId.indexOf('_');
                if (underscoreIndex > 3) {
                    String dayPart = nodeId.substring(3, underscoreIndex);
                    int idDayNumber = Integer.parseInt(dayPart);
                    return idDayNumber == dayNumber;
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to extract day number from node ID: {}", nodeId);
        }
        
        return false;
    }
    
    /**
     * Collect all node IDs from an itinerary.
     */
    public Set<String> collectAllNodeIds(NormalizedItinerary itinerary) {
        Set<String> ids = new HashSet<>();
        
        if (itinerary == null || itinerary.getDays() == null) {
            return ids;
        }
        
        for (NormalizedDay day : itinerary.getDays()) {
            if (day.getNodes() == null) continue;
            
            for (NormalizedNode node : day.getNodes()) {
                if (node.getId() != null && !node.getId().trim().isEmpty()) {
                    ids.add(node.getId());
                }
            }
        }
        
        return ids;
    }
    
    /**
     * Find duplicate node IDs in an itinerary.
     */
    public Set<String> findDuplicates(NormalizedItinerary itinerary) {
        Set<String> seen = new HashSet<>();
        Set<String> duplicates = new HashSet<>();
        
        if (itinerary == null || itinerary.getDays() == null) {
            return duplicates;
        }
        
        for (NormalizedDay day : itinerary.getDays()) {
            if (day.getNodes() == null) continue;
            
            for (NormalizedNode node : day.getNodes()) {
                if (node.getId() != null && !node.getId().trim().isEmpty()) {
                    if (!seen.add(node.getId())) {
                        duplicates.add(node.getId());
                    }
                }
            }
        }
        
        return duplicates;
    }
    
    /**
     * Validate all node IDs in an itinerary.
     * Returns validation errors, or empty set if all valid.
     */
    public Set<String> validateItinerary(NormalizedItinerary itinerary) {
        Set<String> errors = new HashSet<>();
        
        if (itinerary == null || itinerary.getDays() == null) {
            errors.add("Itinerary or days is null");
            return errors;
        }
        
        // Check for duplicates
        Set<String> duplicates = findDuplicates(itinerary);
        if (!duplicates.isEmpty()) {
            errors.add("Duplicate node IDs found: " + String.join(", ", duplicates));
        }
        
        // Check format and day consistency
        for (NormalizedDay day : itinerary.getDays()) {
            if (day.getNodes() == null) continue;
            
            for (NormalizedNode node : day.getNodes()) {
                String nodeId = node.getId();
                
                if (nodeId == null || nodeId.trim().isEmpty()) {
                    errors.add("Node has null or empty ID in day " + day.getDayNumber());
                    continue;
                }
                
                if (!isValidFormat(nodeId)) {
                    errors.add("Invalid node ID format: " + nodeId);
                }
                
                if (!isDayConsistent(nodeId, day.getDayNumber())) {
                    errors.add("Node ID day mismatch: " + nodeId + " in day " + day.getDayNumber());
                }
            }
        }
        
        return errors;
    }
    
    /**
     * Validate a node ID before adding it to an itinerary.
     * Throws IllegalArgumentException if invalid.
     */
    public void validateBeforeAdd(String nodeId, int dayNumber, NormalizedItinerary itinerary) {
        if (!isValidFormat(nodeId)) {
            throw new IllegalArgumentException("Invalid node ID format: " + nodeId);
        }
        
        if (!isUnique(nodeId, itinerary)) {
            throw new IllegalArgumentException("Duplicate node ID: " + nodeId);
        }
        
        if (!isDayConsistent(nodeId, dayNumber)) {
            throw new IllegalArgumentException("Node ID day mismatch: " + nodeId + " for day " + dayNumber);
        }
        
        logger.debug("Node ID validated successfully: {}", nodeId);
    }
    
    /**
     * Validate a node ID before editing.
     * Throws IllegalArgumentException if invalid.
     */
    public void validateBeforeEdit(String nodeId, NormalizedItinerary itinerary) {
        if (!isValidFormat(nodeId)) {
            throw new IllegalArgumentException("Invalid node ID format: " + nodeId);
        }
        
        if (!exists(nodeId, itinerary)) {
            throw new IllegalArgumentException("Node ID does not exist: " + nodeId);
        }
        
        logger.debug("Node ID exists and is valid for editing: {}", nodeId);
    }
}
