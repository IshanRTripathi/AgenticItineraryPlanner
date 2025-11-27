package com.tripplanner.service;

import com.tripplanner.dto.Coordinates;
import com.tripplanner.dto.TransportMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service for validating location accuracy and preventing incorrect place matches.
 * Ensures that places are actually in the intended destination, not in a different country.
 */
@Service
public class LocationValidationService {
    
    private static final Logger logger = LoggerFactory.getLogger(LocationValidationService.class);
    
    // Distance thresholds in kilometers
    private static final double CITY_RADIUS_KM = 50.0;  // Within city
    private static final double REGION_RADIUS_KM = 200.0;  // Within region/state
    private static final double COUNTRY_RADIUS_KM = 1000.0;  // Within country (for large countries)
    private static final double MAX_ACCEPTABLE_DISTANCE_KM = 1500.0;  // Absolute max
    
    private final GeographyService geographyService;
    
    public LocationValidationService(GeographyService geographyService) {
        this.geographyService = geographyService;
    }
    
    /**
     * Convert com.tripplanner.dto.Coordinates to TransportMetadata.Coordinates
     */
    private TransportMetadata.Coordinates convertCoordinates(Coordinates coords) {
        if (coords == null) return null;
        TransportMetadata.Coordinates result = new TransportMetadata.Coordinates();
        result.setLatitude(coords.getLat());
        result.setLongitude(coords.getLng());
        return result;
    }
    
    /**
     * Validate that a place's coordinates are within acceptable distance from destination.
     * 
     * @param placeCoords Coordinates of the found place
     * @param destinationCoords Coordinates of the intended destination
     * @param placeName Name of the place (for logging)
     * @param destinationName Name of the destination (for logging)
     * @return ValidationResult with isValid flag and reason
     */
    public ValidationResult validatePlaceLocation(
            Coordinates placeCoords,
            Coordinates destinationCoords,
            String placeName,
            String destinationName) {
        
        if (placeCoords == null || destinationCoords == null) {
            return ValidationResult.invalid("Missing coordinates for validation");
        }
        
        // Convert to TransportMetadata.Coordinates for GeographyService
        TransportMetadata.Coordinates placeConverted = convertCoordinates(placeCoords);
        TransportMetadata.Coordinates destConverted = convertCoordinates(destinationCoords);
        
        double distanceKm = geographyService.calculateDistance(placeConverted, destConverted);
        
        logger.info("📏 [LocationValidation] Distance from '{}' to destination '{}': {:.2f} km",
                   placeName, destinationName, distanceKm);
        
        // Validate based on distance thresholds
        if (distanceKm <= CITY_RADIUS_KM) {
            logger.info("✅ [LocationValidation] Place is within city radius ({:.2f} km)", distanceKm);
            return ValidationResult.valid(distanceKm, "Within city");
        } else if (distanceKm <= REGION_RADIUS_KM) {
            logger.info("✅ [LocationValidation] Place is within region radius ({:.2f} km)", distanceKm);
            return ValidationResult.valid(distanceKm, "Within region");
        } else if (distanceKm <= COUNTRY_RADIUS_KM) {
            logger.warn("⚠️ [LocationValidation] Place is far from destination ({:.2f} km) but within country range", distanceKm);
            return ValidationResult.warning(distanceKm, "Far from destination but possibly valid");
        } else if (distanceKm <= MAX_ACCEPTABLE_DISTANCE_KM) {
            logger.error("❌ [LocationValidation] Place is very far from destination ({:.2f} km) - likely wrong location", distanceKm);
            return ValidationResult.invalid(String.format(
                "Place is %.0f km from destination - likely in different country/region", distanceKm));
        } else {
            logger.error("❌ [LocationValidation] Place is extremely far from destination ({:.2f} km) - definitely wrong", distanceKm);
            return ValidationResult.invalid(String.format(
                "Place is %.0f km from destination - definitely wrong location", distanceKm));
        }
    }
    
    /**
     * Validate that all places in an itinerary are within reasonable distance from each other.
     * Detects outliers that might be in wrong locations.
     * 
     * @param coordinates List of coordinates to validate
     * @param destinationCoords Reference destination coordinates
     * @return ValidationResult with outliers identified
     */
    public OutlierValidationResult detectOutliers(
            java.util.List<CoordinateWithName> coordinates,
            Coordinates destinationCoords) {
        
        if (coordinates == null || coordinates.isEmpty()) {
            return new OutlierValidationResult(true, java.util.Collections.emptyList());
        }
        
        java.util.List<String> outliers = new java.util.ArrayList<>();
        
        TransportMetadata.Coordinates destConverted = convertCoordinates(destinationCoords);
        
        for (CoordinateWithName coord : coordinates) {
            TransportMetadata.Coordinates coordConverted = convertCoordinates(coord.getCoordinates());
            double distance = geographyService.calculateDistance(coordConverted, destConverted);
            
            if (distance > COUNTRY_RADIUS_KM) {
                String message = String.format("%s is %.0f km from destination - possible outlier", 
                                              coord.getName(), distance);
                outliers.add(message);
                logger.warn("⚠️ [LocationValidation] Outlier detected: {}", message);
            }
        }
        
        boolean isValid = outliers.isEmpty();
        if (!isValid) {
            logger.error("❌ [LocationValidation] Found {} outliers in itinerary", outliers.size());
        }
        
        return new OutlierValidationResult(isValid, outliers);
    }
    
    /**
     * Get recommended search radius based on destination type.
     * Larger cities/countries need larger search radius.
     */
    public double getRecommendedSearchRadius(String destination) {
        if (destination == null) {
            return CITY_RADIUS_KM * 1000; // 50km in meters
        }
        
        String destLower = destination.toLowerCase();
        
        // Large countries need larger radius
        if (destLower.contains("india") || destLower.contains("china") || 
            destLower.contains("usa") || destLower.contains("russia") ||
            destLower.contains("brazil") || destLower.contains("australia")) {
            return REGION_RADIUS_KM * 1000; // 200km
        }
        
        // Default city radius
        return CITY_RADIUS_KM * 1000; // 50km
    }
    
    // Result classes
    
    public static class ValidationResult {
        private final boolean valid;
        private final boolean warning;
        private final double distanceKm;
        private final String reason;
        
        private ValidationResult(boolean valid, boolean warning, double distanceKm, String reason) {
            this.valid = valid;
            this.warning = warning;
            this.distanceKm = distanceKm;
            this.reason = reason;
        }
        
        public static ValidationResult valid(double distanceKm, String reason) {
            return new ValidationResult(true, false, distanceKm, reason);
        }
        
        public static ValidationResult warning(double distanceKm, String reason) {
            return new ValidationResult(true, true, distanceKm, reason);
        }
        
        public static ValidationResult invalid(String reason) {
            return new ValidationResult(false, false, -1, reason);
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public boolean isWarning() {
            return warning;
        }
        
        public double getDistanceKm() {
            return distanceKm;
        }
        
        public String getReason() {
            return reason;
        }
    }
    
    public static class OutlierValidationResult {
        private final boolean valid;
        private final java.util.List<String> outliers;
        
        public OutlierValidationResult(boolean valid, java.util.List<String> outliers) {
            this.valid = valid;
            this.outliers = outliers;
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public java.util.List<String> getOutliers() {
            return outliers;
        }
    }
    
    public static class CoordinateWithName {
        private final Coordinates coordinates;
        private final String name;
        
        public CoordinateWithName(Coordinates coordinates, String name) {
            this.coordinates = coordinates;
            this.name = name;
        }
        
        public Coordinates getCoordinates() {
            return coordinates;
        }
        
        public String getName() {
            return name;
        }
    }
}
