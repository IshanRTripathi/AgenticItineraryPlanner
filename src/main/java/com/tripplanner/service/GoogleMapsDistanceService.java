package com.tripplanner.service;

import com.tripplanner.dto.Coordinates;
import com.tripplanner.dto.NodeLocation;
import com.tripplanner.service.analytics.ItineraryMetricsTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

/**
 * GoogleMapsDistanceService - Calculates real distances and durations using
 * Google Maps Distance Matrix API.
 * 
 * CRITICAL: Replaces hallucinated LLM transport durations with actual routing
 * data.
 * 
 * Features:
 * 1. Real distance calculation between coordinates
 * 2. Actual travel duration with traffic consideration
 * 3. Multiple transport modes (driving, walking, transit)
 * 4. Fallback to straight-line distance if API fails
 */
@Service
public class GoogleMapsDistanceService {

    private static final Logger logger = LoggerFactory.getLogger(GoogleMapsDistanceService.class);

    @Value("${google.maps.api.key:}")
    private String apiKey;

    private final RestTemplate restTemplate;

    @Autowired(required = false)
    private ItineraryMetricsTracker metricsTracker;

    // Fallback: average speeds for different modes (km/h)
    private static final double WALKING_SPEED_KMH = 5.0;
    private static final double DRIVING_SPEED_KMH = 40.0; // Urban average
    private static final double TRANSIT_SPEED_KMH = 30.0;

    // Earth radius for distance calculation (km)
    private static final double EARTH_RADIUS_KM = 6371.0;

    public GoogleMapsDistanceService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Distance and duration result.
     */
    public static class DistanceResult {
        private double distanceKm;
        private int durationMinutes;
        private String mode;
        private boolean fromAPI;

        public DistanceResult(double distanceKm, int durationMinutes, String mode, boolean fromAPI) {
            this.distanceKm = distanceKm;
            this.durationMinutes = durationMinutes;
            this.mode = mode;
            this.fromAPI = fromAPI;
        }

        public double getDistanceKm() {
            return distanceKm;
        }

        public int getDurationMinutes() {
            return durationMinutes;
        }

        public String getMode() {
            return mode;
        }

        public boolean isFromAPI() {
            return fromAPI;
        }
    }

    /**
     * Calculate distance and duration between two locations.
     * 
     * @param from Starting location
     * @param to   Ending location
     * @param mode Transport mode (driving, walking, transit)
     * @return DistanceResult with distance and duration
     */
    public DistanceResult calculateDistance(NodeLocation from, NodeLocation to, String mode) {
        // Validate inputs
        if (from == null || to == null ||
                from.getCoordinates() == null || to.getCoordinates() == null) {
            logger.warn("Invalid locations for distance calculation");
            return createFallbackResult(0, mode);
        }

        Coordinates fromCoords = from.getCoordinates();
        Coordinates toCoords = to.getCoordinates();

        // Try Google Maps API first
        if (apiKey != null && !apiKey.trim().isEmpty()) {
            try {
                return calculateWithAPI(fromCoords, toCoords, mode);
            } catch (Exception e) {
                logger.warn("Google Maps API failed, using fallback: {}", e.getMessage());
            }
        } else {
            logger.debug("Google Maps API key not configured, using fallback calculation");
        }

        // Fallback to haversine distance
        return calculateWithHaversine(fromCoords, toCoords, mode);
    }

    /**
     * Calculate distance using Google Maps Distance Matrix API.
     */
    private DistanceResult calculateWithAPI(Coordinates from, Coordinates to, String mode) {
        long startTime = System.currentTimeMillis();
        boolean success = false;
        Integer statusCode = null;
        String errorMessage = null;

        try {
            String url = UriComponentsBuilder
                    .fromHttpUrl("https://maps.googleapis.com/maps/api/distancematrix/json")
                    .queryParam("origins", from.getLat() + "," + from.getLng())
                    .queryParam("destinations", to.getLat() + "," + to.getLng())
                    .queryParam("mode", mapModeToGoogleMaps(mode))
                    .queryParam("key", apiKey)
                    .build()
                    .toUriString();

            logger.debug("Calling Google Maps Distance Matrix API: mode={}", mode);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response == null || !"OK".equals(response.get("status"))) {
                statusCode = 400;
                errorMessage = "Google Maps API returned error: " +
                        (response != null ? response.get("status") : "null response");
                throw new RuntimeException(errorMessage);
            }

            // Parse response
            @SuppressWarnings("unchecked")
            Map<String, Object>[] rows = (Map<String, Object>[]) response.get("rows");
            if (rows == null || rows.length == 0) {
                statusCode = 400;
                errorMessage = "No rows in Google Maps response";
                throw new RuntimeException(errorMessage);
            }

            @SuppressWarnings("unchecked")
            Map<String, Object>[] elements = (Map<String, Object>[]) rows[0].get("elements");
            if (elements == null || elements.length == 0) {
                statusCode = 400;
                errorMessage = "No elements in Google Maps response";
                throw new RuntimeException(errorMessage);
            }

            Map<String, Object> element = elements[0];
            if (!"OK".equals(element.get("status"))) {
                statusCode = 400;
                errorMessage = "Element status not OK: " + element.get("status");
                throw new RuntimeException(errorMessage);
            }

            // Extract distance and duration
            @SuppressWarnings("unchecked")
            Map<String, Object> distance = (Map<String, Object>) element.get("distance");
            @SuppressWarnings("unchecked")
            Map<String, Object> duration = (Map<String, Object>) element.get("duration");

            double distanceKm = ((Number) distance.get("value")).doubleValue() / 1000.0; // meters to km
            int durationMinutes = ((Number) duration.get("value")).intValue() / 60; // seconds to minutes

            logger.info("Google Maps API result: {}km, {}min (mode: {})",
                    String.format("%.2f", distanceKm), durationMinutes, mode);

            success = true;
            statusCode = 200;
            return new DistanceResult(distanceKm, durationMinutes, mode, true);

        } catch (Exception e) {
            if (errorMessage == null) {
                errorMessage = e.getMessage();
            }
            if (statusCode == null) {
                statusCode = 500;
            }
            throw e;

        } finally {
            // Track API call
            if (metricsTracker != null) {
                long duration = System.currentTimeMillis() - startTime;
                metricsTracker.trackAPICall(
                        null, // itineraryId - not available in this context
                        "google_maps",
                        "distance_matrix",
                        duration,
                        success,
                        statusCode,
                        errorMessage);
            }
        }
    }

    /**
     * Calculate distance using Haversine formula (straight-line distance).
     * Then estimate duration based on mode and average speeds.
     */
    private DistanceResult calculateWithHaversine(Coordinates from, Coordinates to, String mode) {
        double distanceKm = haversineDistance(
                from.getLat(), from.getLng(),
                to.getLat(), to.getLng());

        // Estimate duration based on mode
        // Add 30% to account for non-straight routes
        double adjustedDistance = distanceKm * 1.3;

        double speedKmh = switch (mode != null ? mode.toLowerCase() : "driving") {
            case "walking", "walk" -> WALKING_SPEED_KMH;
            case "transit", "bus", "metro", "train" -> TRANSIT_SPEED_KMH;
            default -> DRIVING_SPEED_KMH;
        };

        int durationMinutes = (int) Math.ceil((adjustedDistance / speedKmh) * 60);

        logger.debug("Haversine calculation: {}km, {}min (mode: {}, speed: {}km/h)",
                String.format("%.2f", distanceKm), durationMinutes, mode, speedKmh);

        return new DistanceResult(distanceKm, durationMinutes, mode, false);
    }

    /**
     * Calculate straight-line distance using Haversine formula.
     */
    private double haversineDistance(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_KM * c;
    }

    /**
     * Map our transport mode to Google Maps mode.
     */
    private String mapModeToGoogleMaps(String mode) {
        if (mode == null)
            return "driving";

        return switch (mode.toLowerCase()) {
            case "walk", "walking" -> "walking";
            case "transit", "bus", "metro", "train", "tram" -> "transit";
            case "bike", "bicycle", "cycling" -> "bicycling";
            default -> "driving";
        };
    }

    /**
     * Create fallback result for invalid inputs.
     */
    private DistanceResult createFallbackResult(double distanceKm, String mode) {
        int durationMinutes = 15; // Default 15 minutes
        return new DistanceResult(distanceKm, durationMinutes, mode, false);
    }

    /**
     * Validate distance result is reasonable.
     */
    public boolean isReasonable(DistanceResult result) {
        // Check for unreasonably long distances (> 500km suggests different cities)
        if (result.getDistanceKm() > 500) {
            logger.warn("Unreasonably long distance: {}km", result.getDistanceKm());
            return false;
        }

        // Check for unreasonably long durations (> 8 hours)
        if (result.getDurationMinutes() > 480) {
            logger.warn("Unreasonably long duration: {}min", result.getDurationMinutes());
            return false;
        }

        // Check for zero distance
        if (result.getDistanceKm() < 0.01) {
            logger.warn("Zero or near-zero distance: {}km", result.getDistanceKm());
            return false;
        }

        return true;
    }
}
