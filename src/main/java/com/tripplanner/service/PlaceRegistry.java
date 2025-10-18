package com.tripplanner.service;

import com.tripplanner.dto.CanonicalPlace;
import com.tripplanner.dto.Coordinates;
import com.tripplanner.dto.PlaceCandidate;
import com.tripplanner.service.PlaceMatcher.PlaceMatchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Central registry for canonical places that handles deduplication and merging
 * of place information from multiple sources.
 */
@Service
public class PlaceRegistry {
    
    private static final Logger logger = LoggerFactory.getLogger(PlaceRegistry.class);
    
    private final PlaceMatcher placeMatcher;
    private final UserDataService userDataService;
    
    // In-memory cache for frequently accessed places
    private final Map<String, CanonicalPlace> placeCache = new ConcurrentHashMap<>();
    private final Map<String, Long> cacheTimestamps = new ConcurrentHashMap<>();
    private static final long CACHE_TTL_MS = 30 * 60 * 1000; // 30 minutes
    
    public PlaceRegistry(PlaceMatcher placeMatcher, UserDataService userDataService) {
        this.placeMatcher = placeMatcher;
        this.userDataService = userDataService;
    }
    
    /**
     * Process a place candidate and either merge it with an existing canonical place
     * or create a new canonical place.
     * 
     * @param candidate The place candidate to process
     * @return The canonical place ID (either existing or newly created)
     */
    public String processPlaceCandidate(PlaceCandidate candidate) {
        logger.debug("Processing place candidate: {}", candidate);
        
        if (!candidate.isValid()) {
            throw new IllegalArgumentException("Invalid place candidate: " + candidate);
        }
        
        try {
            // Get existing canonical places in the area for matching
            List<CanonicalPlace> nearbyPlaces = findNearbyCanonicalPlaces(
                candidate.getCoordinates(), 200.0); // 200m search radius
            
            // Try to find a match
            PlaceMatchResult bestMatch = placeMatcher.findBestMatch(candidate, nearbyPlaces);
            
            if (bestMatch != null && bestMatch.isStrongMatch()) {
                // Merge with existing canonical place
                logger.info("Merging candidate {} with existing canonical place {}", 
                           candidate.getName(), bestMatch.getCanonicalPlace().getName());
                return mergeWithCanonicalPlace(candidate, bestMatch.getCanonicalPlace());
            } else {
                // Create new canonical place
                logger.info("Creating new canonical place for candidate: {}", candidate.getName());
                return createNewCanonicalPlace(candidate);
            }
            
        } catch (Exception e) {
            logger.error("Failed to process place candidate: {}", candidate, e);
            throw new RuntimeException("Failed to process place candidate", e);
        }
    }
    
    /**
     * Get a canonical place by its ID.
     */
    public Optional<CanonicalPlace> getCanonicalPlace(String placeId) {
        if (placeId == null || placeId.trim().isEmpty()) {
            return Optional.empty();
        }
        
        // Check cache first
        CanonicalPlace cached = getCachedPlace(placeId);
        if (cached != null) {
            return Optional.of(cached);
        }
        
        try {
            // Load from persistent storage
            Optional<CanonicalPlace> place = loadCanonicalPlaceFromStorage(placeId);
            
            // Cache if found
            place.ifPresent(p -> cachePlace(placeId, p));
            
            return place;
            
        } catch (Exception e) {
            logger.error("Failed to get canonical place: {}", placeId, e);
            return Optional.empty();
        }
    }
    
    /**
     * Find canonical places near the given coordinates.
     */
    public List<CanonicalPlace> findNearbyCanonicalPlaces(Coordinates coordinates, double radiusMeters) {
        if (coordinates == null || coordinates.getLat() == null || coordinates.getLng() == null) {
            return new ArrayList<>();
        }
        
        try {
            // For now, we'll implement a simple approach
            // In a production system, this would use spatial indexing (e.g., geohash, R-tree)
            return loadAllCanonicalPlaces().stream()
                    .filter(place -> place.getCoordinates() != null)
                    .filter(place -> calculateDistance(coordinates, place.getCoordinates()) <= radiusMeters)
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            logger.error("Failed to find nearby canonical places", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Search for canonical places by name.
     */
    public List<CanonicalPlace> searchPlacesByName(String query, int limit) {
        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        String normalizedQuery = query.toLowerCase().trim();
        
        try {
            return loadAllCanonicalPlaces().stream()
                    .filter(place -> matchesNameQuery(place, normalizedQuery))
                    .sorted((p1, p2) -> Double.compare(p2.getConfidence(), p1.getConfidence()))
                    .limit(limit)
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            logger.error("Failed to search places by name: {}", query, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Get statistics about the place registry.
     */
    public PlaceRegistryStats getStats() {
        try {
            List<CanonicalPlace> allPlaces = loadAllCanonicalPlaces();
            
            int totalPlaces = allPlaces.size();
            int placesWithMultipleSources = (int) allPlaces.stream()
                    .filter(place -> place.getSources().size() > 1)
                    .count();
            
            Map<String, Integer> sourceTypeCounts = new HashMap<>();
            allPlaces.forEach(place -> 
                place.getSources().forEach(source -> 
                    sourceTypeCounts.merge(source.getSourceType(), 1, Integer::sum)));
            
            double averageConfidence = allPlaces.stream()
                    .mapToDouble(CanonicalPlace::getConfidence)
                    .average()
                    .orElse(0.0);
            
            return new PlaceRegistryStats(totalPlaces, placesWithMultipleSources, 
                                        sourceTypeCounts, averageConfidence, placeCache.size());
                                        
        } catch (Exception e) {
            logger.error("Failed to get place registry stats", e);
            return new PlaceRegistryStats(0, 0, new HashMap<>(), 0.0, placeCache.size());
        }
    }
    
    /**
     * Merge a place candidate with an existing canonical place.
     */
    private String mergeWithCanonicalPlace(PlaceCandidate candidate, CanonicalPlace canonicalPlace) {
        // Add the candidate as a new source
        CanonicalPlace.PlaceSource newSource = new CanonicalPlace.PlaceSource(
            candidate.getSourceType(), candidate.getSourceId(), candidate.getAuthority());
        canonicalPlace.addSource(newSource);
        
        // Update alternative names
        if (candidate.getName() != null && !candidate.getName().equals(canonicalPlace.getName())) {
            canonicalPlace.addAlternativeName(candidate.getName());
        }
        
        // Update alternative addresses
        if (candidate.getAddress() != null && !candidate.getAddress().equals(canonicalPlace.getAddress())) {
            canonicalPlace.addAlternativeAddress(candidate.getAddress());
        }
        
        // Update coordinates with weighted average
        if (candidate.hasValidCoordinates()) {
            double weight = Math.min(candidate.getAuthority(), 0.3); // Max 30% influence
            canonicalPlace.updateCoordinates(candidate.getCoordinates(), weight);
        }
        
        // Update rating with weighted average
        if (candidate.getRating() != null) {
            double weight = Math.min(candidate.getAuthority(), 0.3); // Max 30% influence
            canonicalPlace.updateRating(candidate.getRating(), weight);
        }
        
        // Update types
        if (candidate.getTypes() != null) {
            candidate.getTypes().forEach(canonicalPlace::addType);
        }
        
        // Update price level (use higher authority source)
        if (candidate.getPriceLevel() != null && 
            (canonicalPlace.getPriceLevel() == null || candidate.getAuthority() > 0.7)) {
            canonicalPlace.setPriceLevel(candidate.getPriceLevel());
        }
        
        // Update confidence based on number of sources and their authority
        updateCanonicalPlaceConfidence(canonicalPlace);
        
        // Save updated canonical place
        saveCanonicalPlace(canonicalPlace);
        
        // Update cache
        cachePlace(canonicalPlace.getPlaceId(), canonicalPlace);
        
        return canonicalPlace.getPlaceId();
    }
    
    /**
     * Create a new canonical place from a candidate.
     */
    private String createNewCanonicalPlace(PlaceCandidate candidate) {
        // Generate stable canonical place ID
        String canonicalPlaceId = generateCanonicalPlaceId(candidate);
        
        // Create canonical place
        CanonicalPlace canonicalPlace = new CanonicalPlace(
            canonicalPlaceId, candidate.getName(), 
            candidate.getCoordinates(), candidate.getAddress());
        
        // Set initial properties
        canonicalPlace.setCategory(candidate.getCategory());
        canonicalPlace.setRating(candidate.getRating());
        canonicalPlace.setPriceLevel(candidate.getPriceLevel());
        canonicalPlace.setConfidence(candidate.getConfidence());
        
        // Add types
        if (candidate.getTypes() != null) {
            candidate.getTypes().forEach(canonicalPlace::addType);
        }
        
        // Add source
        CanonicalPlace.PlaceSource source = new CanonicalPlace.PlaceSource(
            candidate.getSourceType(), candidate.getSourceId(), candidate.getAuthority());
        canonicalPlace.addSource(source);
        
        // Save canonical place
        saveCanonicalPlace(canonicalPlace);
        
        // Cache the new place
        cachePlace(canonicalPlaceId, canonicalPlace);
        
        logger.info("Created new canonical place: {} with ID: {}", 
                   canonicalPlace.getName(), canonicalPlaceId);
        
        return canonicalPlaceId;
    }
    
    /**
     * Generate a stable canonical place ID based on normalized location data.
     */
    private String generateCanonicalPlaceId(PlaceCandidate candidate) {
        try {
            // Create a stable hash based on normalized coordinates and name
            String normalizedName = candidate.getName().toLowerCase().replaceAll("[^a-z0-9]", "");
            String coordString = String.format("%.4f,%.4f", 
                Math.round(candidate.getCoordinates().getLat() * 10000.0) / 10000.0,
                Math.round(candidate.getCoordinates().getLng() * 10000.0) / 10000.0);
            
            String hashInput = normalizedName + ":" + coordString;
            
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(hashInput.getBytes());
            
            // Convert to hex and take first 12 characters
            StringBuilder hexString = new StringBuilder();
            for (int i = 0; i < Math.min(6, hash.length); i++) {
                String hex = Integer.toHexString(0xff & hash[i]);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return "cp_" + hexString.toString();
            
        } catch (NoSuchAlgorithmException e) {
            logger.error("Failed to generate canonical place ID", e);
            // Fallback to UUID
            return "cp_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        }
    }
    
    /**
     * Update confidence score for a canonical place based on its sources.
     */
    private void updateCanonicalPlaceConfidence(CanonicalPlace canonicalPlace) {
        List<CanonicalPlace.PlaceSource> sources = canonicalPlace.getSources();
        
        if (sources.isEmpty()) {
            canonicalPlace.setConfidence(0.5);
            return;
        }
        
        // Base confidence from highest authority source
        double baseConfidence = sources.stream()
                .mapToDouble(CanonicalPlace.PlaceSource::getAuthority)
                .max()
                .orElse(0.5);
        
        // Bonus for multiple sources
        double sourceBonus = Math.min(0.2, (sources.size() - 1) * 0.05);
        
        // Bonus for source diversity
        Set<String> uniqueSourceTypes = sources.stream()
                .map(CanonicalPlace.PlaceSource::getSourceType)
                .collect(Collectors.toSet());
        double diversityBonus = Math.min(0.1, (uniqueSourceTypes.size() - 1) * 0.03);
        
        double finalConfidence = Math.min(1.0, baseConfidence + sourceBonus + diversityBonus);
        canonicalPlace.setConfidence(finalConfidence);
    }
    
    /**
     * Calculate distance between two coordinates in meters.
     */
    private double calculateDistance(Coordinates coord1, Coordinates coord2) {
        final double EARTH_RADIUS_METERS = 6371000.0;
        
        double lat1Rad = Math.toRadians(coord1.getLat());
        double lat2Rad = Math.toRadians(coord2.getLat());
        double deltaLatRad = Math.toRadians(coord2.getLat() - coord1.getLat());
        double deltaLngRad = Math.toRadians(coord2.getLng() - coord1.getLng());
        
        double a = Math.sin(deltaLatRad / 2) * Math.sin(deltaLatRad / 2) +
                   Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                   Math.sin(deltaLngRad / 2) * Math.sin(deltaLngRad / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return EARTH_RADIUS_METERS * c;
    }
    
    /**
     * Check if a place matches a name query.
     */
    private boolean matchesNameQuery(CanonicalPlace place, String normalizedQuery) {
        // Check primary name
        if (place.getName() != null && 
            place.getName().toLowerCase().contains(normalizedQuery)) {
            return true;
        }
        
        // Check alternative names
        return place.getAlternativeNames().stream()
                .anyMatch(name -> name.toLowerCase().contains(normalizedQuery));
    }
    
    /**
     * Get place from cache if valid.
     */
    private CanonicalPlace getCachedPlace(String placeId) {
        Long timestamp = cacheTimestamps.get(placeId);
        if (timestamp != null && (System.currentTimeMillis() - timestamp) < CACHE_TTL_MS) {
            return placeCache.get(placeId);
        }
        
        // Remove expired cache entry
        placeCache.remove(placeId);
        cacheTimestamps.remove(placeId);
        return null;
    }
    
    /**
     * Cache a place.
     */
    private void cachePlace(String placeId, CanonicalPlace place) {
        placeCache.put(placeId, place);
        cacheTimestamps.put(placeId, System.currentTimeMillis());
    }
    
    /**
     * Save canonical place to persistent storage.
     * For now, we'll use a simple approach. In production, this would use a proper database.
     */
    private void saveCanonicalPlace(CanonicalPlace canonicalPlace) {
        try {
            // Save to user data service (this is a simplified approach)
            // In production, this would be a dedicated canonical places collection
            userDataService.saveCanonicalPlace(canonicalPlace);
            
        } catch (Exception e) {
            logger.error("Failed to save canonical place: {}", canonicalPlace.getPlaceId(), e);
            throw new RuntimeException("Failed to save canonical place", e);
        }
    }
    
    /**
     * Load canonical place from storage.
     */
    private Optional<CanonicalPlace> loadCanonicalPlaceFromStorage(String placeId) {
        try {
            return userDataService.getCanonicalPlace(placeId);
            
        } catch (Exception e) {
            logger.error("Failed to load canonical place: {}", placeId, e);
            return Optional.empty();
        }
    }
    
    /**
     * Load all canonical places from storage.
     * This is inefficient for large datasets - in production, use pagination/streaming.
     */
    private List<CanonicalPlace> loadAllCanonicalPlaces() {
        try {
            return userDataService.getAllCanonicalPlaces();
            
        } catch (Exception e) {
            logger.error("Failed to load all canonical places", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Statistics about the place registry.
     */
    public static class PlaceRegistryStats {
        private final int totalPlaces;
        private final int placesWithMultipleSources;
        private final Map<String, Integer> sourceTypeCounts;
        private final double averageConfidence;
        private final int cachedPlaces;
        
        public PlaceRegistryStats(int totalPlaces, int placesWithMultipleSources,
                                Map<String, Integer> sourceTypeCounts, double averageConfidence,
                                int cachedPlaces) {
            this.totalPlaces = totalPlaces;
            this.placesWithMultipleSources = placesWithMultipleSources;
            this.sourceTypeCounts = sourceTypeCounts;
            this.averageConfidence = averageConfidence;
            this.cachedPlaces = cachedPlaces;
        }
        
        public int getTotalPlaces() { return totalPlaces; }
        public int getPlacesWithMultipleSources() { return placesWithMultipleSources; }
        public Map<String, Integer> getSourceTypeCounts() { return sourceTypeCounts; }
        public double getAverageConfidence() { return averageConfidence; }
        public int getCachedPlaces() { return cachedPlaces; }
        
        @Override
        public String toString() {
            return "PlaceRegistryStats{" +
                    "totalPlaces=" + totalPlaces +
                    ", placesWithMultipleSources=" + placesWithMultipleSources +
                    ", sourceTypeCounts=" + sourceTypeCounts +
                    ", averageConfidence=" + String.format("%.2f", averageConfidence) +
                    ", cachedPlaces=" + cachedPlaces +
                    '}';
        }
    }
}