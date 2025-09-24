package com.tripplanner.service;

import com.tripplanner.dto.Coordinates;
import com.tripplanner.dto.MapBounds;
import com.tripplanner.dto.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Service for calculating map bounds and centroids from itinerary nodes.
 * Provides utilities for determining the geographic area covered by an itinerary.
 */
@Service
public class MapBoundsCalculator {
    
    private static final Logger logger = LoggerFactory.getLogger(MapBoundsCalculator.class);
    
    // Default bounds for world view when no valid coordinates are found
    private static final MapBounds WORLD_BOUNDS = new MapBounds(-90.0, -180.0, 90.0, 180.0);
    
    // Default centroid for world view
    private static final Coordinates WORLD_CENTROID = new Coordinates(0.0, 0.0);
    
    /**
     * Calculate map bounds from a list of nodes with coordinates.
     * 
     * @param nodes List of normalized nodes
     * @return MapBounds object, or WORLD_BOUNDS if no valid coordinates found
     */
    public MapBounds calculateBounds(List<NormalizedNode> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            logger.debug("No nodes provided for bounds calculation, returning world bounds");
            return WORLD_BOUNDS;
        }
        
        List<Coordinates> validCoordinates = extractValidCoordinates(nodes);
        
        if (validCoordinates.isEmpty()) {
            logger.debug("No valid coordinates found in nodes, returning world bounds");
            return WORLD_BOUNDS;
        }
        
        if (validCoordinates.size() == 1) {
            // Single point - create small bounds around it
            Coordinates coord = validCoordinates.get(0);
            double padding = 0.01; // ~1km padding
            return new MapBounds(
                coord.getLat() - padding,
                coord.getLng() - padding,
                coord.getLat() + padding,
                coord.getLng() + padding
            );
        }
        
        // Calculate bounds from multiple coordinates
        double minLat = validCoordinates.stream().mapToDouble(Coordinates::getLat).min().orElse(0.0);
        double maxLat = validCoordinates.stream().mapToDouble(Coordinates::getLat).max().orElse(0.0);
        double minLng = validCoordinates.stream().mapToDouble(Coordinates::getLng).min().orElse(0.0);
        double maxLng = validCoordinates.stream().mapToDouble(Coordinates::getLng).max().orElse(0.0);
        
        // Add padding to bounds (5% of the span or minimum 0.01 degrees)
        double latSpan = maxLat - minLat;
        double lngSpan = maxLng - minLng;
        double latPadding = Math.max(latSpan * 0.05, 0.01);
        double lngPadding = Math.max(lngSpan * 0.05, 0.01);
        
        MapBounds bounds = new MapBounds(
            minLat - latPadding,
            minLng - lngPadding,
            maxLat + latPadding,
            maxLng + lngPadding
        );
        
        logger.debug("Calculated bounds for {} nodes: {}", validCoordinates.size(), bounds);
        return bounds;
    }
    
    /**
     * Calculate the centroid (center point) from a list of nodes with coordinates.
     * 
     * @param nodes List of normalized nodes
     * @return Coordinates object representing the centroid, or WORLD_CENTROID if no valid coordinates found
     */
    public Coordinates calculateCentroid(List<NormalizedNode> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            logger.debug("No nodes provided for centroid calculation, returning world centroid");
            return WORLD_CENTROID;
        }
        
        List<Coordinates> validCoordinates = extractValidCoordinates(nodes);
        
        if (validCoordinates.isEmpty()) {
            logger.debug("No valid coordinates found in nodes, returning world centroid");
            return WORLD_CENTROID;
        }
        
        if (validCoordinates.size() == 1) {
            return validCoordinates.get(0);
        }
        
        // Calculate average coordinates
        double avgLat = validCoordinates.stream().mapToDouble(Coordinates::getLat).average().orElse(0.0);
        double avgLng = validCoordinates.stream().mapToDouble(Coordinates::getLng).average().orElse(0.0);
        
        Coordinates centroid = new Coordinates(avgLat, avgLng);
        logger.debug("Calculated centroid for {} nodes: {}", validCoordinates.size(), centroid);
        return centroid;
    }
    
    /**
     * Validate if coordinates are within valid geographic ranges.
     * 
     * @param coordinates Coordinates to validate
     * @return true if coordinates are valid, false otherwise
     */
    public boolean validateCoordinates(Coordinates coordinates) {
        if (coordinates == null) {
            return false;
        }
        
        Double lat = coordinates.getLat();
        Double lng = coordinates.getLng();
        
        if (lat == null || lng == null) {
            return false;
        }
        
        // Check if coordinates are within valid ranges
        boolean validLat = lat >= -90.0 && lat <= 90.0;
        boolean validLng = lng >= -180.0 && lng <= 180.0;
        
        return validLat && validLng;
    }
    
    /**
     * Extract valid coordinates from a list of nodes.
     * 
     * @param nodes List of nodes to extract coordinates from
     * @return List of valid coordinates
     */
    private List<Coordinates> extractValidCoordinates(List<NormalizedNode> nodes) {
        return nodes.stream()
                .map(NormalizedNode::getLocation)
                .filter(Objects::nonNull)
                .map(location -> location.getCoordinates())
                .filter(Objects::nonNull)
                .filter(this::validateCoordinates)
                .collect(Collectors.toList());
    }
    
    /**
     * Check if a node has valid coordinates.
     * 
     * @param node Node to check
     * @return true if node has valid coordinates, false otherwise
     */
    public boolean hasValidCoordinates(NormalizedNode node) {
        if (node == null || node.getLocation() == null) {
            return false;
        }
        
        Coordinates coordinates = node.getLocation().getCoordinates();
        return validateCoordinates(coordinates);
    }
    
    /**
     * Get the number of nodes with valid coordinates.
     * 
     * @param nodes List of nodes to check
     * @return Number of nodes with valid coordinates
     */
    public int countNodesWithValidCoordinates(List<NormalizedNode> nodes) {
        if (nodes == null) {
            return 0;
        }
        
        return (int) nodes.stream()
                .filter(this::hasValidCoordinates)
                .count();
    }
    
    /**
     * Get default world bounds for fallback scenarios.
     * 
     * @return World bounds
     */
    public MapBounds getWorldBounds() {
        return WORLD_BOUNDS;
    }
    
    /**
     * Get default world centroid for fallback scenarios.
     * 
     * @return World centroid
     */
    public Coordinates getWorldCentroid() {
        return WORLD_CENTROID;
    }
}
