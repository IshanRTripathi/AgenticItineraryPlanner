package com.tripplanner.service;

import com.tripplanner.dto.Coordinates;
import com.tripplanner.enums.ToolType;
import com.tripplanner.service.cache.ToolCacheService;
import com.tripplanner.util.ToolCacheKeyGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;

/**
 * GeocodingService - Converts addresses to coordinates using Google Geocoding API.
 * 
 * CACHING: Results cached for 30 days (coordinates never change)
 */
@Service
public class GeocodingService {
    
    private static final Logger logger = LoggerFactory.getLogger(GeocodingService.class);
    
    @Value("${google.maps.api.key:}")
    private String apiKey;
    
    private final RestTemplate restTemplate;
    
    @Autowired(required = false)
    private ToolCacheService toolCacheService;
    
    public GeocodingService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    
    public static class GeocodeResponse {
        private Coordinates coordinates;
        private String formattedAddress;
        private String placeId;
        
        public GeocodeResponse(Coordinates coordinates, String formattedAddress, String placeId) {
            this.coordinates = coordinates;
            this.formattedAddress = formattedAddress;
            this.placeId = placeId;
        }
        
        public Coordinates getCoordinates() {
            return coordinates;
        }
        
        public String getFormattedAddress() {
            return formattedAddress;
        }
        
        public String getPlaceId() {
            return placeId;
        }
    }
    
    /**
     * Geocode an address to coordinates.
     * 
     * @param itineraryId Itinerary ID for caching (optional)
     * @param address Address to geocode
     * @param countryHint Country hint for better results (optional)
     * @return Geocode response with coordinates
     */
    public GeocodeResponse geocode(String itineraryId, String address, String countryHint) {
        if (address == null || address.trim().isEmpty()) {
            throw new IllegalArgumentException("Address cannot be empty");
        }
        
        // Try cache first if itineraryId provided
        if (toolCacheService != null && itineraryId != null) {
            String cacheKey = ToolCacheKeyGenerator.forGeocode(address);
            
            return toolCacheService.getOrCompute(
                itineraryId,
                ToolType.GEOCODE.getValue(),
                cacheKey,
                Map.of("address", address, "countryHint", countryHint != null ? countryHint : ""),
                () -> geocodeInternal(address, countryHint),
                GeocodeResponse.class
            );
        }
        
        // Fallback to direct call
        return geocodeInternal(address, countryHint);
    }
    
    /**
     * Overload for backward compatibility (no itineraryId)
     */
    public GeocodeResponse geocode(String address, String countryHint) {
        return geocode(null, address, countryHint);
    }
    
    /**
     * Internal geocoding implementation (called by cache or directly)
     */
    private GeocodeResponse geocodeInternal(String address, String countryHint) {
        // Check if API key is configured
        if (apiKey == null || apiKey.trim().isEmpty()) {
            logger.warn("Google Maps API key not configured, cannot geocode");
            throw new RuntimeException("Geocoding service not configured");
        }
        
        try {
            // Build URL
            UriComponentsBuilder builder = UriComponentsBuilder
                    .fromHttpUrl("https://maps.googleapis.com/maps/api/geocode/json")
                    .queryParam("address", address)
                    .queryParam("key", apiKey);
            
            // Add country hint if provided
            if (countryHint != null && !countryHint.trim().isEmpty()) {
                builder.queryParam("components", "country:" + countryHint);
            }
            
            String url = builder.build().toUriString();
            
            logger.debug("Geocoding address: {}", address);
            
            // Call API
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            
            if (response == null || !"OK".equals(response.get("status"))) {
                String status = response != null ? (String) response.get("status") : "null";
                logger.error("Geocoding failed with status: {}", status);
                throw new RuntimeException("Geocoding failed: " + status);
            }
            
            // Parse response
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> results = (List<Map<String, Object>>) response.get("results");
            
            if (results == null || results.isEmpty()) {
                throw new RuntimeException("No results found for address");
            }
            
            Map<String, Object> firstResult = results.get(0);
            
            // Extract coordinates
            @SuppressWarnings("unchecked")
            Map<String, Object> geometry = (Map<String, Object>) firstResult.get("geometry");
            @SuppressWarnings("unchecked")
            Map<String, Object> location = (Map<String, Object>) geometry.get("location");
            
            double lat = ((Number) location.get("lat")).doubleValue();
            double lng = ((Number) location.get("lng")).doubleValue();
            
            Coordinates coordinates = new Coordinates(lat, lng);
            String formattedAddress = (String) firstResult.get("formatted_address");
            String placeId = (String) firstResult.get("place_id");
            
            logger.info("Geocoded '{}' to ({}, {})", address, lat, lng);
            
            return new GeocodeResponse(coordinates, formattedAddress, placeId);
            
        } catch (Exception e) {
            logger.error("Failed to geocode address: {}", address, e);
            throw new RuntimeException("Geocoding failed: " + e.getMessage(), e);
        }
    }
}
