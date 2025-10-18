package com.tripplanner.service;

import com.tripplanner.dto.CanonicalPlace;
import com.tripplanner.dto.Coordinates;
import com.tripplanner.dto.PlaceCandidate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;

/**
 * Service for matching place candidates to existing canonical places.
 * Uses spatial proximity, name similarity, and other heuristics to determine matches.
 */
@Component
public class PlaceMatcher {
    
    private static final Logger logger = LoggerFactory.getLogger(PlaceMatcher.class);
    
    // Matching thresholds
    private static final double SPATIAL_MATCH_RADIUS_METERS = 100.0; // 100 meter radius
    private static final double NAME_SIMILARITY_THRESHOLD = 0.7; // 70% similarity
    private static final double STRONG_MATCH_THRESHOLD = 0.8; // 80% overall confidence
    private static final double WEAK_MATCH_THRESHOLD = 0.5; // 50% overall confidence
    
    /**
     * Find potential matches for a place candidate among existing canonical places.
     * Returns a list of match results sorted by confidence (highest first).
     */
    public List<PlaceMatchResult> findMatches(PlaceCandidate candidate, 
                                            List<CanonicalPlace> existingPlaces) {
        logger.debug("Finding matches for candidate: {} at {}", 
                    candidate.getName(), candidate.getCoordinates());
        
        if (!candidate.isValid()) {
            logger.warn("Invalid candidate provided for matching: {}", candidate);
            return new ArrayList<>();
        }
        
        List<PlaceMatchResult> matches = new ArrayList<>();
        
        for (CanonicalPlace existingPlace : existingPlaces) {
            PlaceMatchResult matchResult = calculateMatch(candidate, existingPlace);
            if (matchResult.getConfidence() >= WEAK_MATCH_THRESHOLD) {
                matches.add(matchResult);
            }
        }
        
        // Sort by confidence (highest first)
        matches.sort((m1, m2) -> Double.compare(m2.getConfidence(), m1.getConfidence()));
        
        logger.debug("Found {} potential matches for candidate {}", matches.size(), candidate.getName());
        return matches;
    }
    
    /**
     * Find the best match for a place candidate.
     * Returns null if no strong match is found.
     */
    public PlaceMatchResult findBestMatch(PlaceCandidate candidate, 
                                        List<CanonicalPlace> existingPlaces) {
        List<PlaceMatchResult> matches = findMatches(candidate, existingPlaces);
        
        if (matches.isEmpty()) {
            return null;
        }
        
        PlaceMatchResult bestMatch = matches.get(0);
        if (bestMatch.getConfidence() >= STRONG_MATCH_THRESHOLD) {
            logger.debug("Found strong match for {}: {} (confidence: {:.2f})", 
                        candidate.getName(), bestMatch.getCanonicalPlace().getName(), 
                        bestMatch.getConfidence());
            return bestMatch;
        }
        
        logger.debug("No strong match found for {} (best confidence: {:.2f})", 
                    candidate.getName(), bestMatch.getConfidence());
        return null;
    }
    
    /**
     * Calculate match confidence between a candidate and an existing canonical place.
     */
    private PlaceMatchResult calculateMatch(PlaceCandidate candidate, CanonicalPlace existingPlace) {
        double spatialScore = calculateSpatialSimilarity(candidate.getCoordinates(), 
                                                        existingPlace.getCoordinates());
        double nameScore = calculateNameSimilarity(candidate.getName(), existingPlace);
        double typeScore = calculateTypeSimilarity(candidate.getTypes(), existingPlace.getTypes());
        double sourceScore = calculateSourceSimilarity(candidate, existingPlace);
        
        // Weighted combination of scores
        double overallConfidence = (spatialScore * 0.4) + (nameScore * 0.3) + 
                                 (typeScore * 0.2) + (sourceScore * 0.1);
        
        PlaceMatchResult result = new PlaceMatchResult(existingPlace, overallConfidence);
        result.setSpatialScore(spatialScore);
        result.setNameScore(nameScore);
        result.setTypeScore(typeScore);
        result.setSourceScore(sourceScore);
        
        logger.debug("Match calculation for {} vs {}: spatial={:.2f}, name={:.2f}, type={:.2f}, source={:.2f}, overall={:.2f}",
                    candidate.getName(), existingPlace.getName(), 
                    spatialScore, nameScore, typeScore, sourceScore, overallConfidence);
        
        return result;
    }
    
    /**
     * Calculate spatial similarity based on distance between coordinates.
     */
    private double calculateSpatialSimilarity(Coordinates coord1, Coordinates coord2) {
        if (coord1 == null || coord2 == null || 
            coord1.getLat() == null || coord1.getLng() == null ||
            coord2.getLat() == null || coord2.getLng() == null) {
            return 0.0;
        }
        
        double distance = calculateDistance(coord1, coord2);
        
        if (distance <= SPATIAL_MATCH_RADIUS_METERS) {
            // Linear decay from 1.0 at distance 0 to 0.0 at SPATIAL_MATCH_RADIUS_METERS
            return Math.max(0.0, 1.0 - (distance / SPATIAL_MATCH_RADIUS_METERS));
        }
        
        return 0.0;
    }
    
    /**
     * Calculate name similarity using multiple techniques.
     */
    private double calculateNameSimilarity(String candidateName, CanonicalPlace existingPlace) {
        if (candidateName == null || candidateName.trim().isEmpty() ||
            existingPlace.getName() == null || existingPlace.getName().trim().isEmpty()) {
            return 0.0;
        }
        
        String normalizedCandidate = normalizeName(candidateName);
        String normalizedExisting = normalizeName(existingPlace.getName());
        
        // Check exact match first
        if (normalizedCandidate.equals(normalizedExisting)) {
            return 1.0;
        }
        
        // Check alternative names
        for (String altName : existingPlace.getAlternativeNames()) {
            if (normalizedCandidate.equals(normalizeName(altName))) {
                return 0.95; // Slightly lower than exact match
            }
        }
        
        // Calculate Levenshtein distance similarity
        double levenshteinSimilarity = calculateLevenshteinSimilarity(normalizedCandidate, normalizedExisting);
        
        // Check for substring matches
        double substringSimilarity = calculateSubstringSimilarity(normalizedCandidate, normalizedExisting);
        
        // Return the higher of the two similarities
        return Math.max(levenshteinSimilarity, substringSimilarity);
    }
    
    /**
     * Calculate type similarity between candidate and existing place.
     */
    private double calculateTypeSimilarity(List<String> candidateTypes, Set<String> existingTypes) {
        if ((candidateTypes == null || candidateTypes.isEmpty()) &&
            (existingTypes == null || existingTypes.isEmpty())) {
            return 1.0; // Both have no types
        }
        
        if (candidateTypes == null || candidateTypes.isEmpty() ||
            existingTypes == null || existingTypes.isEmpty()) {
            return 0.5; // One has types, other doesn't
        }
        
        Set<String> candidateSet = new HashSet<>(candidateTypes);
        Set<String> intersection = new HashSet<>(candidateSet);
        intersection.retainAll(existingTypes);
        
        Set<String> union = new HashSet<>(candidateSet);
        union.addAll(existingTypes);
        
        // Jaccard similarity
        return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
    }
    
    /**
     * Calculate source similarity (bonus for same source type).
     */
    private double calculateSourceSimilarity(PlaceCandidate candidate, CanonicalPlace existingPlace) {
        if (candidate.getSourceType() == null || existingPlace.getSources().isEmpty()) {
            return 0.5; // Neutral score
        }
        
        // Check if candidate source already exists in canonical place
        boolean hasMatchingSource = existingPlace.hasSource(candidate.getSourceType(), candidate.getSourceId());
        if (hasMatchingSource) {
            return 1.0; // Perfect match - same source
        }
        
        // Check if same source type exists
        boolean hasSameSourceType = existingPlace.getSources().stream()
                .anyMatch(source -> candidate.getSourceType().equals(source.getSourceType()));
        
        return hasSameSourceType ? 0.8 : 0.5;
    }
    
    /**
     * Calculate distance between two coordinates in meters using Haversine formula.
     */
    private double calculateDistance(Coordinates coord1, Coordinates coord2) {
        final double EARTH_RADIUS_METERS = 6371000.0; // Earth's radius in meters
        
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
     * Normalize place name for comparison.
     */
    private String normalizeName(String name) {
        if (name == null) {
            return "";
        }
        
        return name.toLowerCase()
                   .replaceAll("[^a-z0-9\\s]", "") // Remove special characters
                   .replaceAll("\\s+", " ") // Normalize whitespace
                   .trim();
    }
    
    /**
     * Calculate Levenshtein distance similarity.
     */
    private double calculateLevenshteinSimilarity(String s1, String s2) {
        int distance = calculateLevenshteinDistance(s1, s2);
        int maxLength = Math.max(s1.length(), s2.length());
        
        if (maxLength == 0) {
            return 1.0;
        }
        
        return 1.0 - ((double) distance / maxLength);
    }
    
    /**
     * Calculate Levenshtein distance between two strings.
     */
    private int calculateLevenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];
        
        for (int i = 0; i <= s1.length(); i++) {
            dp[i][0] = i;
        }
        
        for (int j = 0; j <= s2.length(); j++) {
            dp[0][j] = j;
        }
        
        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                int cost = (s1.charAt(i - 1) == s2.charAt(j - 1)) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1), dp[i - 1][j - 1] + cost);
            }
        }
        
        return dp[s1.length()][s2.length()];
    }
    
    /**
     * Calculate substring similarity.
     */
    private double calculateSubstringSimilarity(String s1, String s2) {
        if (s1.contains(s2) || s2.contains(s1)) {
            int minLength = Math.min(s1.length(), s2.length());
            int maxLength = Math.max(s1.length(), s2.length());
            return (double) minLength / maxLength;
        }
        
        return 0.0;
    }
    
    /**
     * Result of a place matching operation.
     */
    public static class PlaceMatchResult {
        private final CanonicalPlace canonicalPlace;
        private final double confidence;
        private double spatialScore;
        private double nameScore;
        private double typeScore;
        private double sourceScore;
        
        public PlaceMatchResult(CanonicalPlace canonicalPlace, double confidence) {
            this.canonicalPlace = canonicalPlace;
            this.confidence = confidence;
        }
        
        public CanonicalPlace getCanonicalPlace() {
            return canonicalPlace;
        }
        
        public double getConfidence() {
            return confidence;
        }
        
        public double getSpatialScore() {
            return spatialScore;
        }
        
        public void setSpatialScore(double spatialScore) {
            this.spatialScore = spatialScore;
        }
        
        public double getNameScore() {
            return nameScore;
        }
        
        public void setNameScore(double nameScore) {
            this.nameScore = nameScore;
        }
        
        public double getTypeScore() {
            return typeScore;
        }
        
        public void setTypeScore(double typeScore) {
            this.typeScore = typeScore;
        }
        
        public double getSourceScore() {
            return sourceScore;
        }
        
        public void setSourceScore(double sourceScore) {
            this.sourceScore = sourceScore;
        }
        
        public boolean isStrongMatch() {
            return confidence >= STRONG_MATCH_THRESHOLD;
        }
        
        public boolean isWeakMatch() {
            return confidence >= WEAK_MATCH_THRESHOLD;
        }
        
        @Override
        public String toString() {
            return "PlaceMatchResult{" +
                    "canonicalPlace=" + canonicalPlace.getName() +
                    ", confidence=" + String.format("%.2f", confidence) +
                    ", spatial=" + String.format("%.2f", spatialScore) +
                    ", name=" + String.format("%.2f", nameScore) +
                    ", type=" + String.format("%.2f", typeScore) +
                    ", source=" + String.format("%.2f", sourceScore) +
                    '}';
        }
    }
}