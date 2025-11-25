package com.tripplanner.service;

import com.tripplanner.dto.NormalizedDay;
import com.tripplanner.dto.NormalizedItinerary;
import com.tripplanner.dto.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Service for migrating itineraries from old node ID patterns to new standardized pattern.
 * Automatically detects and migrates itineraries when they are loaded.
 */
@Service
public class ItineraryMigrationService {
    
    private static final Logger logger = LoggerFactory.getLogger(ItineraryMigrationService.class);
    
    private final ItineraryJsonService itineraryJsonService;
    
    public ItineraryMigrationService(ItineraryJsonService itineraryJsonService) {
        this.itineraryJsonService = itineraryJsonService;
    }
    
    /**
     * Migrate itinerary node IDs to new standardized pattern if needed.
     * Called automatically when itinerary is loaded.
     * 
     * @param itinerary The itinerary to check and migrate
     * @return The migrated itinerary, or original if no migration needed
     */
    public NormalizedItinerary migrateIfNeeded(NormalizedItinerary itinerary) {
        if (itinerary == null) {
            logger.warn("Cannot migrate null itinerary");
            return itinerary;
        }
        
        if (!needsMigration(itinerary)) {
            logger.debug("Itinerary {} does not need migration", itinerary.getItineraryId());
            return itinerary;
        }
        
        logger.info("Migrating node IDs for itinerary: {}", itinerary.getItineraryId());
        
        try {
            NormalizedItinerary migrated = performMigration(itinerary);
            
            // Save migrated version with optimistic locking and retry
            int maxRetries = 3;
            int retryCount = 0;
            boolean saved = false;
            
            while (!saved && retryCount < maxRetries) {
                try {
                    itineraryJsonService.updateItineraryWithLock(migrated);
                    saved = true;
                } catch (com.tripplanner.exception.ConcurrentModificationException e) {
                    retryCount++;
                    logger.error("Concurrent modification during migration (attempt {}/{}): {}", 
                               retryCount, maxRetries, e.getMessage());
                    
                    if (retryCount < maxRetries) {
                        logger.info("Reloading itinerary and retrying migration save...");
                        Optional<NormalizedItinerary> reloaded = itineraryJsonService.getItinerary(itinerary.getItineraryId());
                        if (reloaded.isPresent()) {
                            migrated = reloaded.get();
                            // Re-apply migration to reloaded itinerary
                            migrated = performMigration(migrated);
                            logger.info("Re-applied migration to reloaded itinerary");
                        } else {
                            logger.error("Failed to reload itinerary for retry");
                            throw new RuntimeException("Migration conflict", e);
                        }
                    } else {
                        logger.error("Max retries ({}) exceeded, giving up", maxRetries);
                        throw new RuntimeException("Migration conflict", e);
                    }
                }
            }
            
            int totalNodes = migrated.getDays().stream()
                    .mapToInt(d -> d.getNodes() != null ? d.getNodes().size() : 0)
                    .sum();
            
            logger.info("Successfully migrated itinerary {} with {} days and {} total nodes", 
                       migrated.getItineraryId(),
                       migrated.getDays().size(), 
                       totalNodes);
            
            return migrated;
            
        } catch (Exception e) {
            logger.error("Failed to migrate itinerary: {}", itinerary.getItineraryId(), e);
            // Return original itinerary if migration fails (graceful degradation)
            return itinerary;
        }
    }
    
    /**
     * Check if an itinerary needs migration.
     * Returns true if any node has an old ID pattern.
     * 
     * @param itinerary The itinerary to check
     * @return true if migration is needed
     */
    private boolean needsMigration(NormalizedItinerary itinerary) {
        if (itinerary.getDays() == null) {
            return false;
        }
        
        return itinerary.getDays().stream()
                .filter(day -> day.getNodes() != null)
                .flatMap(day -> day.getNodes().stream())
                .anyMatch(node -> !isNewIdPattern(node.getId()));
    }
    
    /**
     * Check if a node ID follows the new standardized pattern.
     * New pattern: day{N}_node{M}
     * 
     * @param nodeId The node ID to check
     * @return true if the ID follows the new pattern
     */
    private boolean isNewIdPattern(String nodeId) {
        if (nodeId == null || nodeId.trim().isEmpty()) {
            return false;
        }
        
        // Check for new standardized format: day{number}_node{number}
        return nodeId.matches("day\\d+_node\\d+");
    }
    
    /**
     * Perform the actual migration of node IDs.
     * Converts all node IDs to the new standardized pattern: day{N}_node{M}
     * 
     * @param itinerary The itinerary to migrate
     * @return The migrated itinerary
     */
    private NormalizedItinerary performMigration(NormalizedItinerary itinerary) {
        if (itinerary.getDays() == null) {
            return itinerary;
        }
        
        for (NormalizedDay day : itinerary.getDays()) {
            if (day.getNodes() == null) {
                continue;
            }
            
            // LOG: Before migration
            logger.debug("Day {} before migration: {} nodes with IDs: {}", 
                        day.getDayNumber(),
                        day.getNodes().size(),
                        day.getNodes().stream().map(NormalizedNode::getId).collect(java.util.stream.Collectors.toList()));
            
            int nodeCounter = 1;
            for (NormalizedNode node : day.getNodes()) {
                String oldId = node.getId();
                String newId = String.format("day%d_node%d", day.getDayNumber(), nodeCounter++);
                
                if (!newId.equals(oldId)) {
                    logger.debug("Migrating node ID: {} -> {}", oldId, newId);
                    node.setId(newId);
                }
            }
            
            // LOG: After migration
            logger.info("Day {} after migration: {} nodes with IDs: {}", 
                       day.getDayNumber(),
                       day.getNodes().size(),
                       day.getNodes().stream().map(NormalizedNode::getId).collect(java.util.stream.Collectors.toList()));
        }
        
        // Update version to indicate migration
        itinerary.setVersion(itinerary.getVersion() + 1);
        itinerary.setUpdatedAt(System.currentTimeMillis());
        
        return itinerary;
    }
}
