package com.tripplanner.service;

import com.tripplanner.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Service that integrates the canonical place registry with existing place-related functionality.
 * Provides a bridge between the new canonical place system and existing agents/services.
 */
@Service
public class PlaceEnrichmentService {
    
    private static final Logger logger = LoggerFactory.getLogger(PlaceEnrichmentService.class);
    
    private final PlaceRegistry placeRegistry;
    private final GooglePlacesService googlePlacesService;
    
    public PlaceEnrichmentService(PlaceRegistry placeRegistry, GooglePlacesService googlePlacesService) {
        this.placeRegistry = placeRegistry;
        this.googlePlacesService = googlePlacesService;
    }
    
    /**
     * Enrich a NodeLocation with canonical place information.
     * If the location doesn't have a placeId, tries to find or create one.
     * If it has a placeId, ensures it's canonical.
     */
    public NodeLocation enrichNodeLocation(NodeLocation nodeLocation) {
        logger.debug("Enriching node location: {}", nodeLocation);
        
        if (nodeLocation == null) {
            return null;
        }
        
        try {
            String canonicalPlaceId = null;
            
            // Check if we already have a canonical place ID
            if (nodeLocation.getPlaceId() != null && nodeLocation.getPlaceId().startsWith("cp_")) {
                canonicalPlaceId = nodeLocation.getPlaceId();
                logger.debug("Node location already has canonical place ID: {}", canonicalPlaceId);
            } else {
                // Try to find or create canonical place
                canonicalPlaceId = findOrCreateCanonicalPlace(nodeLocation);
            }
            
            if (canonicalPlaceId != null) {
                // Update the node location with canonical place ID
                NodeLocation enrichedLocation = new NodeLocation(
                    nodeLocation.getName(), 
                    nodeLocation.getAddress(), 
                    nodeLocation.getCoordinates(), 
                    canonicalPlaceId
                );
                
                logger.debug("Enriched node location with canonical place ID: {}", canonicalPlaceId);
                return enrichedLocation;
            } else {
                logger.warn("Could not find or create canonical place for node location: {}", nodeLocation);
                return nodeLocation; // Return original if ENRICHMENT fails
            }
            
        } catch (Exception e) {
            logger.error("Failed to enrich node location: {}", nodeLocation, e);
            return nodeLocation; // Return original if ENRICHMENT fails
        }
    }
    
    /**
     * Find or create a canonical place for a NodeLocation.
     */
    private String findOrCreateCanonicalPlace(NodeLocation nodeLocation) {
        try {
            // Create a place candidate from the node location
            PlaceCandidate candidate = createPlaceCandidateFromNodeLocation(nodeLocation);
            
            if (!candidate.isValid()) {
                logger.warn("Invalid place candidate created from node location: {}", nodeLocation);
                return null;
            }
            
            // Process the candidate through the place registry
            String canonicalPlaceId = placeRegistry.processPlaceCandidate(candidate);
            
            logger.debug("Created/found canonical place {} for node location: {}", 
                        canonicalPlaceId, nodeLocation.getName());
            
            return canonicalPlaceId;
            
        } catch (Exception e) {
            logger.error("Failed to find or create canonical place for node location: {}", nodeLocation, e);
            return null;
        }
    }
    
    /**
     * Create a place candidate from a NodeLocation.
     */
    private PlaceCandidate createPlaceCandidateFromNodeLocation(NodeLocation nodeLocation) {
        PlaceCandidate candidate = new PlaceCandidate();
        
        // Determine source type based on existing placeId
        String sourceType = "user"; // Default
        String sourceId = nodeLocation.getPlaceId();
        
        if (sourceId != null) {
            if (sourceId.startsWith("ChIJ")) {
                sourceType = "google";
                candidate.setAuthority(0.9); // High authority for Google Places
            } else if (sourceId.startsWith("4sq")) {
                sourceType = "foursquare";
                candidate.setAuthority(0.8); // High authority for Foursquare
            } else {
                sourceType = "user";
                candidate.setAuthority(0.6); // Medium authority for user data
            }
        } else {
            // Generate a temporary source ID for places without one
            sourceId = "temp_" + System.currentTimeMillis();
            candidate.setAuthority(0.5); // Lower authority for places without source ID
        }
        
        candidate.setSourceType(sourceType);
        candidate.setSourceId(sourceId);
        candidate.setName(nodeLocation.getName());
        candidate.setAddress(nodeLocation.getAddress());
        candidate.setCoordinates(nodeLocation.getCoordinates());
        
        return candidate;
    }
    
    /**
     * Enrich a place candidate with additional data from Google Places API.
     */
    public PlaceCandidate enrichPlaceCandidateWithGoogleData(PlaceCandidate candidate) {
        logger.debug("Enriching place candidate with Google data: {}", candidate);
        
        if (candidate == null || !"google".equals(candidate.getSourceType())) {
            return candidate; // Only enrich Google places
        }
        
        try {
            PlaceDetails placeDetails = googlePlacesService.getPlaceDetails(candidate.getSourceId());
            
            if (placeDetails != null) {
                // Create enriched candidate from Google data
                PlaceCandidate enrichedCandidate = PlaceCandidate.fromGooglePlace(placeDetails);
                
                // Preserve original source information
                enrichedCandidate.setSourceType(candidate.getSourceType());
                enrichedCandidate.setSourceId(candidate.getSourceId());
                
                logger.debug("Successfully enriched place candidate with Google data: {}", 
                           enrichedCandidate.getName());
                
                return enrichedCandidate;
            } else {
                logger.warn("Could not fetch Google Places data for: {}", candidate.getSourceId());
                return candidate;
            }
            
        } catch (Exception e) {
            logger.error("Failed to enrich place candidate with Google data: {}", candidate, e);
            return candidate; // Return original if ENRICHMENT fails
        }
    }
    
    /**
     * Get canonical place information for a place ID.
     */
    public Optional<CanonicalPlace> getCanonicalPlaceInfo(String placeId) {
        if (placeId == null || placeId.trim().isEmpty()) {
            return Optional.empty();
        }
        
        try {
            return placeRegistry.getCanonicalPlace(placeId);
        } catch (Exception e) {
            logger.error("Failed to get canonical place info for: {}", placeId, e);
            return Optional.empty();
        }
    }
    
    /**
     * Search for places by name using the canonical place registry.
     */
    public java.util.List<CanonicalPlace> searchPlaces(String query, int limit) {
        if (query == null || query.trim().isEmpty()) {
            return new java.util.ArrayList<>();
        }
        
        try {
            return placeRegistry.searchPlacesByName(query, limit);
        } catch (Exception e) {
            logger.error("Failed to search places for query: {}", query, e);
            return new java.util.ArrayList<>();
        }
    }
    
    /**
     * Get statistics about the place ENRICHMENT system.
     */
    public PlaceEnrichmentStats getStats() {
        try {
            PlaceRegistry.PlaceRegistryStats registryStats = placeRegistry.getStats();
            GooglePlacesService.RateLimitStats rateLimitStats = googlePlacesService.getRateLimitStats();
            
            return new PlaceEnrichmentStats(registryStats, rateLimitStats);
            
        } catch (Exception e) {
            logger.error("Failed to get place ENRICHMENT stats", e);
            return new PlaceEnrichmentStats(null, null);
        }
    }
    
    /**
     * Statistics about the place ENRICHMENT system.
     */
    public static class PlaceEnrichmentStats {
        private final PlaceRegistry.PlaceRegistryStats registryStats;
        private final GooglePlacesService.RateLimitStats rateLimitStats;
        
        public PlaceEnrichmentStats(PlaceRegistry.PlaceRegistryStats registryStats,
                                  GooglePlacesService.RateLimitStats rateLimitStats) {
            this.registryStats = registryStats;
            this.rateLimitStats = rateLimitStats;
        }
        
        public PlaceRegistry.PlaceRegistryStats getRegistryStats() {
            return registryStats;
        }
        
        public GooglePlacesService.RateLimitStats getRateLimitStats() {
            return rateLimitStats;
        }
        
        @Override
        public String toString() {
            return "PlaceEnrichmentStats{" +
                    "registryStats=" + registryStats +
                    ", rateLimitStats=" + rateLimitStats +
                    '}';
        }
    }
}