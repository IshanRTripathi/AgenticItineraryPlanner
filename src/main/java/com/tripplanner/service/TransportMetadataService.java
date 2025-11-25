package com.tripplanner.service;

import com.tripplanner.dto.NormalizedNode;
import com.tripplanner.dto.TransportMetadata;
import com.tripplanner.enums.TransportMode;
import com.tripplanner.enums.TransportType;
import com.tripplanner.exception.GeographyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service to populate transport metadata, replacing string parsing
 */
@Service
public class TransportMetadataService {
    
    private static final Logger logger = LoggerFactory.getLogger(TransportMetadataService.class);
    
    private final GeographyService geographyService;
    
    public TransportMetadataService(GeographyService geographyService) {
        this.geographyService = geographyService;
    }
    
    /**
     * Populate transport metadata from node title (migration helper)
     * This replaces string parsing with explicit metadata
     */
    public TransportMetadata populateFromTitle(String title, String fromLocation, String toLocation) {
        TransportMetadata metadata = new TransportMetadata();
        
        // Determine transport type
        TransportType type = TransportType.fromTitle(title);
        metadata.setTransportType(type);
        
        // Set locations
        metadata.setFromLocationName(fromLocation);
        metadata.setToLocationName(toLocation);
        
        // Check geography
        boolean fromIsIsland = geographyService.isIsland(fromLocation);
        boolean toIsIsland = geographyService.isIsland(toLocation);
        metadata.setFromIsIsland(fromIsIsland);
        metadata.setToIsIsland(toIsIsland);
        
        // Get valid transport modes
        List<TransportMode> allowedModes = geographyService.getValidModes(fromLocation, toLocation);
        metadata.setAllowedModes(allowedModes);
        
        // Get geography info for coordinates
        GeographyService.GeographyInfo fromInfo = geographyService.getGeographyInfo(fromLocation);
        GeographyService.GeographyInfo toInfo = geographyService.getGeographyInfo(toLocation);
        
        if (fromInfo.getCoordinates() != null) {
            metadata.setFromCoordinates(fromInfo.getCoordinates());
        }
        if (toInfo.getCoordinates() != null) {
            metadata.setToCoordinates(toInfo.getCoordinates());
        }
        
        // Calculate distance if coordinates available
        if (metadata.getFromCoordinates() != null && metadata.getToCoordinates() != null) {
            double distance = geographyService.calculateDistance(
                metadata.getFromCoordinates(), 
                metadata.getToCoordinates()
            );
            metadata.setEstimatedDistanceKm(distance);
        }
        
        logger.info("Populated transport metadata: {} -> {} (type: {}, island: {}/{})", 
                   fromLocation, toLocation, type, fromIsIsland, toIsIsland);
        
        return metadata;
    }
    
    /**
     * Validate transport mode against geography
     */
    public void validateTransportMode(NormalizedNode node) throws GeographyException {
        if (!"transport".equals(node.getType())) {
            return;
        }
        
        TransportMetadata metadata = (TransportMetadata) node.getMetadata();
        if (metadata == null) {
            logger.warn("No metadata for transport node: {}", node.getId());
            return;
        }
        
        TransportMode selectedMode = metadata.getSelectedMode();
        if (selectedMode == null) {
            return; // No mode selected yet
        }
        
        // Check if mode is in allowed modes
        List<TransportMode> allowedModes = metadata.getAllowedModes();
        if (allowedModes != null && !allowedModes.isEmpty() && !allowedModes.contains(selectedMode)) {
            throw new GeographyException(
                "Invalid transport mode: " + selectedMode + " not allowed for this route",
                metadata.getFromLocationName(),
                metadata.getToLocationName(),
                "Mode " + selectedMode + " is not valid. Allowed modes: " + allowedModes
            );
        }
        
        // Check island constraints
        if (metadata.getToIsIsland() != null && metadata.getToIsIsland()) {
            if (selectedMode.requiresLandConnection()) {
                throw new GeographyException(
                    "Cannot use land transport to island destination",
                    metadata.getFromLocationName(),
                    metadata.getToLocationName(),
                    "Destination is an island. Use flight or ferry."
                );
            }
        }
        
        // Check distance constraints
        if (metadata.getEstimatedDistanceKm() != null) {
            double distance = metadata.getEstimatedDistanceKm();
            if (!selectedMode.isValidForDistance(distance)) {
                throw new GeographyException(
                    "Transport mode not suitable for distance",
                    metadata.getFromLocationName(),
                    metadata.getToLocationName(),
                    String.format("Distance %.0f km exceeds max for %s (%.0f km)", 
                                 distance, selectedMode, (double) selectedMode.getMaxReasonableDistanceKm())
                );
            }
        }
    }
    
    /**
     * Extract locations from title (migration helper)
     */
    public String[] extractLocationsFromTitle(String title) {
        if (title == null || !title.contains(" to ")) {
            return new String[]{"Unknown", "Unknown"};
        }
        
        // Remove prefixes
        String cleaned = title.replaceFirst("(?i)(arrival|departure|travel):\\s*", "").trim();
        
        // Split by " to "
        String[] parts = cleaned.split("\\s+to\\s+", 2);
        if (parts.length == 2) {
            return new String[]{parts[0].trim(), parts[1].trim()};
        }
        
        return new String[]{"Unknown", "Unknown"};
    }
}
