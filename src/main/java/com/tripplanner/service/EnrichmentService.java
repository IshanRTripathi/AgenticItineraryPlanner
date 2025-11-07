package com.tripplanner.service;

import com.tripplanner.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for handling asynchronous enrichment of itinerary nodes.
 * Provides non-blocking enrichment that doesn't affect response times.
 * 
 * This service directly enriches nodes using GooglePlacesService without
 * going through EnrichmentAgent to avoid circular dependencies.
 */
@Service
public class EnrichmentService {
    
    private static final Logger logger = LoggerFactory.getLogger(EnrichmentService.class);
    
    private final ItineraryJsonService itineraryJsonService;
    private final GooglePlacesService googlePlacesService;
    
    @Value("${enrichment.auto-enrich.enabled:true}")
    private boolean autoEnrichEnabled;
    
    public EnrichmentService(ItineraryJsonService itineraryJsonService,
                           GooglePlacesService googlePlacesService) {
        this.itineraryJsonService = itineraryJsonService;
        this.googlePlacesService = googlePlacesService;
    }
    
    /**
     * Enrich specific nodes asynchronously.
     * This method returns immediately and enrichment happens in the background.
     */
    @Async
    public void enrichNodesAsync(String itineraryId, List<String> nodeIds) {
        if (!autoEnrichEnabled) {
            logger.debug("Auto-enrichment is disabled, skipping enrichment for itinerary {}", itineraryId);
            return;
        }
        
        long startTime = System.currentTimeMillis();
        int enrichedCount = 0;
        
        try {
            logger.info("Starting async enrichment for {} nodes in itinerary {}", 
                nodeIds.size(), itineraryId);
            
            // Load current itinerary
            var itineraryOpt = itineraryJsonService.getItinerary(itineraryId);
            if (itineraryOpt.isEmpty()) {
                logger.warn("Itinerary {} not found for enrichment", itineraryId);
                return;
            }
            
            NormalizedItinerary itinerary = itineraryOpt.get();
            String destination = itinerary.getDestination() != null ? itinerary.getDestination() : "";
            
            // Filter nodes that need enrichment (skip nodes with coordinates)
            List<NormalizedNode> nodesToEnrich = filterNodesNeedingEnrichment(itinerary, nodeIds);
            
            if (nodesToEnrich.isEmpty()) {
                logger.info("All {} nodes already have coordinates, skipping enrichment", nodeIds.size());
                return;
            }
            
            logger.info("Enriching {} out of {} nodes (others already have coordinates)", 
                nodesToEnrich.size(), nodeIds.size());
            
            // Enrich each node directly using GooglePlacesService
            boolean hasChanges = false;
            for (NormalizedNode node : nodesToEnrich) {
                try {
                    if (enrichNode(node, destination)) {
                        enrichedCount++;
                        hasChanges = true;
                    }
                } catch (Exception e) {
                    logger.warn("Failed to enrich node {}: {}", node.getId(), e.getMessage());
                }
            }
            
            // Save the enriched itinerary if there were changes
            if (hasChanges) {
                itineraryJsonService.updateItinerary(itinerary);
            }
            
            long duration = System.currentTimeMillis() - startTime;
            logger.info("Successfully enriched {} out of {} nodes in itinerary {} (took {}ms)", 
                enrichedCount, nodesToEnrich.size(), itineraryId, duration);
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            logger.error("Error during async enrichment for itinerary {} (took {}ms): {}", 
                itineraryId, duration, e.getMessage(), e);
            // Don't rethrow - this is async and shouldn't affect the main flow
        }
    }
    
    /**
     * Enrich entire itinerary asynchronously.
     * Useful for bulk enrichment operations.
     */
    @Async
    public void enrichItineraryAsync(String itineraryId) {
        if (!autoEnrichEnabled) {
            logger.debug("Auto-enrichment is disabled, skipping enrichment for itinerary {}", itineraryId);
            return;
        }
        
        long startTime = System.currentTimeMillis();
        int enrichedCount = 0;
        
        try {
            logger.info("Starting async enrichment for entire itinerary {}", itineraryId);
            
            // Load current itinerary
            var itineraryOpt = itineraryJsonService.getItinerary(itineraryId);
            if (itineraryOpt.isEmpty()) {
                logger.warn("Itinerary {} not found for enrichment", itineraryId);
                return;
            }
            
            NormalizedItinerary itinerary = itineraryOpt.get();
            String destination = itinerary.getDestination() != null ? itinerary.getDestination() : "";
            
            // Collect all node IDs
            List<String> allNodeIds = itinerary.getDays().stream()
                .flatMap(day -> day.getNodes().stream())
                .map(NormalizedNode::getId)
                .collect(Collectors.toList());
            
            // Filter and enrich nodes
            List<NormalizedNode> nodesToEnrich = filterNodesNeedingEnrichment(itinerary, allNodeIds);
            
            if (nodesToEnrich.isEmpty()) {
                logger.info("All nodes in itinerary {} already have coordinates", itineraryId);
                return;
            }
            
            logger.info("Enriching {} nodes in itinerary {}", nodesToEnrich.size(), itineraryId);
            
            boolean hasChanges = false;
            for (NormalizedNode node : nodesToEnrich) {
                try {
                    if (enrichNode(node, destination)) {
                        enrichedCount++;
                        hasChanges = true;
                    }
                } catch (Exception e) {
                    logger.warn("Failed to enrich node {}: {}", node.getId(), e.getMessage());
                }
            }
            
            // Save the enriched itinerary if there were changes
            if (hasChanges) {
                itineraryJsonService.updateItinerary(itinerary);
            }
            
            long duration = System.currentTimeMillis() - startTime;
            logger.info("Successfully enriched {} out of {} nodes in entire itinerary {} (took {}ms)", 
                enrichedCount, nodesToEnrich.size(), itineraryId, duration);
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            logger.error("Error during async enrichment for itinerary {} (took {}ms): {}", 
                itineraryId, duration, e.getMessage(), e);
        }
    }
    
    /**
     * Filter nodes that need enrichment (skip nodes that already have coordinates).
     * Returns the actual node objects, not just IDs.
     */
    private List<NormalizedNode> filterNodesNeedingEnrichment(NormalizedItinerary itinerary, List<String> nodeIds) {
        return nodeIds.stream()
            .map(nodeId -> findNodeById(itinerary, nodeId))
            .filter(node -> {
                if (node == null) {
                    return false;
                }
                
                // Only enrich meal and attraction nodes (skip all other types)
                if (!"meal".equals(node.getType()) && !"attraction".equals(node.getType())) {
                    logger.debug("Skipping {} node {} (only meal and attraction nodes are enriched)", 
                        node.getType(), node.getId());
                    return false;
                }
                
                // Check if node already has coordinates
                boolean hasCoordinates = node.getLocation() != null 
                    && node.getLocation().getCoordinates() != null
                    && node.getLocation().getCoordinates().getLat() != null
                    && node.getLocation().getCoordinates().getLng() != null;
                
                if (hasCoordinates) {
                    logger.debug("Node {} already has coordinates, skipping enrichment", node.getId());
                    return false;
                }
                
                return true;
            })
            .collect(Collectors.toList());
    }
    
    /**
     * Find a node by ID across all days.
     */
    private NormalizedNode findNodeById(NormalizedItinerary itinerary, String nodeId) {
        return itinerary.getDays().stream()
                .flatMap(day -> day.getNodes().stream())
                .filter(node -> node.getId() != null && node.getId().equals(nodeId))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * Enrich a single node with Google Places data.
     * Returns true if the node was enriched, false otherwise.
     */
    private boolean enrichNode(NormalizedNode node, String destination) {
        // Get location name for search
        String locationName = null;
        if (node.getLocation() != null && node.getLocation().getName() != null) {
            locationName = node.getLocation().getName();
        } else if (node.getTitle() != null) {
            locationName = node.getTitle();
        }
        
        if (locationName == null || locationName.trim().isEmpty()) {
            logger.warn("Node {} has no location name for enrichment", node.getId());
            return false;
        }
        
        try {
            // Search for place using Google Places API
            PlaceSearchResult searchResult = googlePlacesService.searchPlace(locationName, destination);
            
            if (searchResult == null || searchResult.getGeometry() == null || 
                searchResult.getGeometry().getLocation() == null) {
                logger.warn("No place found for node {} ({})", node.getId(), locationName);
                return false;
            }
            
            // Initialize location if needed
            if (node.getLocation() == null) {
                node.setLocation(new NodeLocation());
            }
            
            // Set coordinates
            if (node.getLocation().getCoordinates() == null) {
                node.getLocation().setCoordinates(new Coordinates());
            }
            node.getLocation().getCoordinates().setLat(
                searchResult.getGeometry().getLocation().getLatitude());
            node.getLocation().getCoordinates().setLng(
                searchResult.getGeometry().getLocation().getLongitude());
            
            // Set place details
            node.getLocation().setName(searchResult.getName());
            if (searchResult.getFormattedAddress() != null) {
                node.getLocation().setAddress(searchResult.getFormattedAddress());
            }
            if (searchResult.getPlaceId() != null) {
                node.getLocation().setPlaceId(searchResult.getPlaceId());
            }
            
            // Set rating if available
            if (searchResult.getRating() != null) {
                node.getLocation().setRating(searchResult.getRating());
            }
            
            logger.info("Enriched node {} ({}) with coordinates ({}, {})", 
                node.getId(), locationName,
                searchResult.getGeometry().getLocation().getLatitude(),
                searchResult.getGeometry().getLocation().getLongitude());
            
            return true;
            
        } catch (Exception e) {
            logger.error("Failed to enrich node {} ({}): {}", node.getId(), locationName, e.getMessage());
            return false;
        }
    }
}
