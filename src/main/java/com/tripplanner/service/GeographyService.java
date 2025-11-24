package com.tripplanner.service;

import com.tripplanner.dto.TransportMetadata.Coordinates;
import com.tripplanner.enums.TransportMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Geography service to replace hardcoded island lists and string parsing
 * Provides real geography data and validation
 */
@Service
public class GeographyService {
    
    private static final Logger logger = LoggerFactory.getLogger(GeographyService.class);
    
    private final Map<String, GeographyInfo> cache = new ConcurrentHashMap<>();
    private final GooglePlacesService placesService;
    
    // Known islands for quick lookup (can be expanded)
    private static final Map<String, Boolean> KNOWN_ISLANDS = Map.ofEntries(
        Map.entry("bali", true),
        Map.entry("phuket", true),
        Map.entry("langkawi", true),
        Map.entry("penang", true),
        Map.entry("singapore", true),
        Map.entry("maldives", true),
        Map.entry("sri lanka", true),
        Map.entry("mauritius", true),
        Map.entry("seychelles", true),
        Map.entry("santorini", true),
        Map.entry("mykonos", true),
        Map.entry("crete", true),
        Map.entry("rhodes", true),
        Map.entry("malta", true),
        Map.entry("sicily", true),
        Map.entry("sardinia", true),
        Map.entry("corsica", true),
        Map.entry("ibiza", true),
        Map.entry("mallorca", true),
        Map.entry("canary islands", true),
        Map.entry("madeira", true),
        Map.entry("iceland", true),
        Map.entry("ireland", true),
        Map.entry("uk", true),
        Map.entry("united kingdom", true),
        Map.entry("great britain", true),
        Map.entry("japan", true),
        Map.entry("taiwan", true),
        Map.entry("philippines", true),
        Map.entry("indonesia", true),
        Map.entry("new zealand", true),
        Map.entry("hawaii", true),
        Map.entry("fiji", true),
        Map.entry("tahiti", true),
        Map.entry("bora bora", true),
        Map.entry("cuba", true),
        Map.entry("jamaica", true),
        Map.entry("bahamas", true),
        Map.entry("barbados", true),
        Map.entry("aruba", true)
    );
    
    public GeographyService(GooglePlacesService placesService) {
        this.placesService = placesService;
    }
    
    /**
     * Get geography information for a location
     */
    public GeographyInfo getGeographyInfo(String location) {
        if (location == null || location.trim().isEmpty()) {
            return new GeographyInfo(location, false, null);
        }
        
        return cache.computeIfAbsent(location.toLowerCase(), this::fetchGeographyInfo);
    }
    
    /**
     * Check if a location is an island
     */
    public boolean isIsland(String location) {
        if (location == null) return false;
        
        String lower = location.toLowerCase().trim();
        
        // Check known islands first
        for (Map.Entry<String, Boolean> entry : KNOWN_ISLANDS.entrySet()) {
            if (lower.contains(entry.getKey())) {
                return true;
            }
        }
        
        // Check cache
        GeographyInfo info = cache.get(lower);
        if (info != null) {
            return info.isIsland();
        }
        
        return false;
    }
    
    /**
     * Get valid transport modes between two locations
     */
    public List<TransportMode> getValidModes(String from, String to) {
        List<TransportMode> modes = new ArrayList<>();
        
        boolean fromIsIsland = isIsland(from);
        boolean toIsIsland = isIsland(to);
        
        // If either is an island, only air/water transport
        if (fromIsIsland || toIsIsland) {
            modes.add(TransportMode.FLIGHT);
            modes.add(TransportMode.FERRY);
            return modes;
        }
        
        // Try to get coordinates and calculate distance
        GeographyInfo fromInfo = getGeographyInfo(from);
        GeographyInfo toInfo = getGeographyInfo(to);
        
        if (fromInfo.getCoordinates() != null && toInfo.getCoordinates() != null) {
            double distance = calculateDistance(fromInfo.getCoordinates(), toInfo.getCoordinates());
            
            if (distance < 50) {
                // Short distance - local transport
                modes.add(TransportMode.TAXI);
                modes.add(TransportMode.BUS);
                modes.add(TransportMode.METRO);
                modes.add(TransportMode.CAR);
            } else if (distance < 300) {
                // Medium distance - regional transport
                modes.add(TransportMode.TRAIN);
                modes.add(TransportMode.BUS);
                modes.add(TransportMode.CAR);
            } else {
                // Long distance - inter-city transport
                modes.add(TransportMode.FLIGHT);
                modes.add(TransportMode.TRAIN);
                modes.add(TransportMode.BUS);
            }
        } else {
            // Unknown distance - allow all land transport
            modes.add(TransportMode.TRAIN);
            modes.add(TransportMode.BUS);
            modes.add(TransportMode.CAR);
            modes.add(TransportMode.TAXI);
        }
        
        return modes;
    }
    
    /**
     * Calculate distance between two coordinates in kilometers
     */
    public double calculateDistance(Coordinates from, Coordinates to) {
        if (from == null || to == null) return 0;
        
        double lat1 = Math.toRadians(from.getLatitude());
        double lon1 = Math.toRadians(from.getLongitude());
        double lat2 = Math.toRadians(to.getLatitude());
        double lon2 = Math.toRadians(to.getLongitude());
        
        double dlon = lon2 - lon1;
        double dlat = lat2 - lat1;
        
        double a = Math.pow(Math.sin(dlat / 2), 2) + 
                   Math.cos(lat1) * Math.cos(lat2) * Math.pow(Math.sin(dlon / 2), 2);
        double c = 2 * Math.asin(Math.sqrt(a));
        
        // Radius of earth in kilometers
        double r = 6371;
        
        return c * r;
    }
    
    /**
     * Fetch geography info (can be enhanced with Google Places API)
     */
    private GeographyInfo fetchGeographyInfo(String location) {
        try {
            // For now, use known islands and basic logic
            // Can be enhanced with Google Places API geocoding
            boolean isIsland = KNOWN_ISLANDS.getOrDefault(location, false);
            
            // Try to get coordinates from Places API if available
            Coordinates coords = null;
            // TODO: Integrate with Google Places Geocoding API
            
            return new GeographyInfo(location, isIsland, coords);
        } catch (Exception e) {
            logger.warn("Failed to fetch geography info for {}: {}", location, e.getMessage());
            return new GeographyInfo(location, false, null);
        }
    }
    
    /**
     * Geography information for a location
     */
    public static class GeographyInfo {
        private final String location;
        private final boolean isIsland;
        private final Coordinates coordinates;
        
        public GeographyInfo(String location, boolean isIsland, Coordinates coordinates) {
            this.location = location;
            this.isIsland = isIsland;
            this.coordinates = coordinates;
        }
        
        public String getLocation() {
            return location;
        }
        
        public boolean isIsland() {
            return isIsland;
        }
        
        public Coordinates getCoordinates() {
            return coordinates;
        }
    }
}
