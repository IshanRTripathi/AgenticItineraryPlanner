package com.tripplanner.service;

import com.tripplanner.dto.NodeCandidate;
import com.tripplanner.dto.NormalizedItinerary;
import com.tripplanner.dto.NormalizedDay;
import com.tripplanner.dto.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import java.text.Normalizer;

/**
 * Service for resolving ambiguous node references in user text.
 * Uses fuzzy matching to find the best candidates for node selection.
 */
@Service
public class NodeResolutionService {
    
    private static final Logger logger = LoggerFactory.getLogger(NodeResolutionService.class);
    
    private final ItineraryJsonService itineraryJsonService;
    
    public NodeResolutionService(ItineraryJsonService itineraryJsonService) {
        this.itineraryJsonService = itineraryJsonService;
    }
    
    /**
     * Find node candidates based on text search across the itinerary.
     */
    public List<NodeCandidate> findNodeCandidates(String text, String itineraryId, Integer day) {
        logger.debug("Finding node candidates for text: '{}', itineraryId: {}, day: {}", text, itineraryId, day);
        
        try {
            // Load the itinerary using the flexible ID lookup
            var itineraryOpt = itineraryJsonService.getItinerary(itineraryId);
            if (itineraryOpt.isEmpty()) {
                logger.warn("Itinerary not found: {}", itineraryId);
                return List.of();
            }
            
            NormalizedItinerary itinerary = itineraryOpt.get();
            List<NodeCandidate> allCandidates = new ArrayList<>();
            
            // Search across all days or specific day
            List<NormalizedDay> daysToSearch = day != null ? 
                itinerary.getDays().stream()
                    .filter(d -> d.getDayNumber().equals(day))
                    .collect(Collectors.toList()) :
                itinerary.getDays();
            
            // Search by different criteria
            allCandidates.addAll(searchByTitle(text, daysToSearch));
            allCandidates.addAll(searchByLocation(text, daysToSearch));
            allCandidates.addAll(searchByType(text, daysToSearch));
            
            // Remove duplicates and rank by confidence
            List<NodeCandidate> uniqueCandidates = removeDuplicates(allCandidates);
            List<NodeCandidate> rankedCandidates = rankCandidates(uniqueCandidates, text);
            
            // Return top 3 candidates
            return rankedCandidates.stream()
                    .limit(3)
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            logger.error("Error finding node candidates", e);
            return List.of();
        }
    }
    
    /**
     * Search for nodes by title similarity.
     */
    private List<NodeCandidate> searchByTitle(String text, List<NormalizedDay> days) {
        List<NodeCandidate> candidates = new ArrayList<>();
        String searchText = text.toLowerCase().trim();
        
        for (NormalizedDay day : days) {
            for (NormalizedNode node : day.getNodes()) {
                if (node.getTitle() != null) {
                    String nodeTitle = node.getTitle().toLowerCase();
                    
                    // Check for exact substring match first (more flexible)
                    if (searchText.contains(nodeTitle) || nodeTitle.contains(searchText)) {
                        candidates.add(NodeCandidate.of(
                            node.getId(),
                            node.getTitle(),
                            day.getDayNumber(),
                            node.getType(),
                            node.getLocation() != null ? node.getLocation().getName() : null,
                            0.9 // High confidence for substring match
                        ));
                    } else if (isSimilarAfterNormalization(searchText, nodeTitle)) {
                        // Check normalized similarity for special characters
                        candidates.add(NodeCandidate.of(
                            node.getId(),
                            node.getTitle(),
                            day.getDayNumber(),
                            node.getType(),
                            node.getLocation() != null ? node.getLocation().getName() : null,
                            0.8 // High confidence for normalized match
                        ));
                    } else {
                        // Fall back to similarity calculation
                        double similarity = calculateSimilarity(searchText, nodeTitle);
                        if (similarity > 0.2) { // Lower threshold for more flexibility
                            candidates.add(NodeCandidate.of(
                                node.getId(),
                                node.getTitle(),
                                day.getDayNumber(),
                                node.getType(),
                                node.getLocation() != null ? node.getLocation().getName() : null,
                                similarity
                            ));
                        }
                    }
                }
            }
        }
        
        return candidates;
    }
    
    /**
     * Search for nodes by location similarity.
     */
    private List<NodeCandidate> searchByLocation(String text, List<NormalizedDay> days) {
        List<NodeCandidate> candidates = new ArrayList<>();
        String searchText = text.toLowerCase().trim();
        
        for (NormalizedDay day : days) {
            for (NormalizedNode node : day.getNodes()) {
                if (node.getLocation() != null && node.getLocation().getName() != null) {
                    String locationName = node.getLocation().getName().toLowerCase();
                    
                    // Check for exact substring match first
                    if (searchText.contains(locationName) || locationName.contains(searchText)) {
                        candidates.add(NodeCandidate.of(
                            node.getId(),
                            node.getTitle(),
                            day.getDayNumber(),
                            node.getType(),
                            node.getLocation().getName(),
                            0.8 // High confidence for location match
                        ));
                    } else if (isSimilarAfterNormalization(searchText, locationName)) {
                        // Check normalized similarity for special characters
                        candidates.add(NodeCandidate.of(
                            node.getId(),
                            node.getTitle(),
                            day.getDayNumber(),
                            node.getType(),
                            node.getLocation().getName(),
                            0.7 // High confidence for normalized location match
                        ));
                    } else {
                        // Fall back to similarity calculation
                        double similarity = calculateSimilarity(searchText, locationName);
                        if (similarity > 0.2) { // Lower threshold
                            candidates.add(NodeCandidate.of(
                                node.getId(),
                                node.getTitle(),
                                day.getDayNumber(),
                                node.getType(),
                                node.getLocation().getName(),
                                similarity * 0.8 // Slightly lower weight for location matches
                            ));
                        }
                    }
                }
            }
        }
        
        return candidates;
    }
    
    /**
     * Search for nodes by type similarity.
     */
    private List<NodeCandidate> searchByType(String text, List<NormalizedDay> days) {
        List<NodeCandidate> candidates = new ArrayList<>();
        String searchText = text.toLowerCase().trim();
        
        // Map common words to node types
        Map<String, String> typeMapping = new HashMap<>();
        typeMapping.put("restaurant", "meal");
        typeMapping.put("eat", "meal");
        typeMapping.put("lunch", "meal");
        typeMapping.put("dinner", "meal");
        typeMapping.put("food", "meal");
        typeMapping.put("museum", "attraction");
        typeMapping.put("attraction", "attraction");
        typeMapping.put("sight", "attraction");
        typeMapping.put("visit", "attraction");
        typeMapping.put("hotel", "accommodation");
        typeMapping.put("stay", "accommodation");
        typeMapping.put("accommodation", "accommodation");
        typeMapping.put("transport", "transit");
        typeMapping.put("taxi", "transit");
        typeMapping.put("bus", "transit");
        typeMapping.put("train", "transit");
        
        String targetType = null;
        for (Map.Entry<String, String> entry : typeMapping.entrySet()) {
            if (searchText.contains(entry.getKey())) {
                targetType = entry.getValue();
                break;
            }
        }
        
        if (targetType != null) {
            for (NormalizedDay day : days) {
                for (NormalizedNode node : day.getNodes()) {
                    if (targetType.equals(node.getType())) {
                        candidates.add(NodeCandidate.of(
                            node.getId(),
                            node.getTitle(),
                            day.getDayNumber(),
                            node.getType(),
                            node.getLocation() != null ? node.getLocation().getName() : null,
                            0.5 // Medium confidence for type matches
                        ));
                    }
                }
            }
        }
        
        return candidates;
    }
    
    /**
     * Calculate similarity between two strings using simple algorithms.
     */
    private double calculateSimilarity(String text1, String text2) {
        if (text1 == null || text2 == null) return 0.0;
        if (text1.equals(text2)) return 1.0;
        
        // Check for exact substring match
        if (text1.contains(text2) || text2.contains(text1)) {
            return 0.8;
        }
        
        // Check for word overlap
        String[] words1 = text1.split("\\s+");
        String[] words2 = text2.split("\\s+");
        
        int commonWords = 0;
        for (String word1 : words1) {
            for (String word2 : words2) {
                if (word1.equals(word2) || word1.contains(word2) || word2.contains(word1)) {
                    commonWords++;
                    break;
                }
            }
        }
        
        if (commonWords > 0) {
            return (double) commonWords / Math.max(words1.length, words2.length);
        }
        
        // Check for character similarity (simple Levenshtein-like)
        return calculateCharacterSimilarity(text1, text2);
    }
    
    /**
     * Calculate character-level similarity.
     */
    private double calculateCharacterSimilarity(String text1, String text2) {
        int maxLength = Math.max(text1.length(), text2.length());
        if (maxLength == 0) return 1.0;
        
        int commonChars = 0;
        int minLength = Math.min(text1.length(), text2.length());
        
        for (int i = 0; i < minLength; i++) {
            if (text1.charAt(i) == text2.charAt(i)) {
                commonChars++;
            }
        }
        
        return (double) commonChars / maxLength;
    }
    
    /**
     * Remove duplicate candidates based on node ID.
     */
    private List<NodeCandidate> removeDuplicates(List<NodeCandidate> candidates) {
        Map<String, NodeCandidate> uniqueCandidates = new HashMap<>();
        
        for (NodeCandidate candidate : candidates) {
            NodeCandidate existing = uniqueCandidates.get(candidate.getId());
            if (existing == null || candidate.getConfidence() > existing.getConfidence()) {
                uniqueCandidates.put(candidate.getId(), candidate);
            }
        }
        
        return new ArrayList<>(uniqueCandidates.values());
    }
    
    /**
     * Rank candidates by confidence and other factors.
     */
    private List<NodeCandidate> rankCandidates(List<NodeCandidate> candidates, String originalText) {
        return candidates.stream()
                .sorted((c1, c2) -> {
                    // Primary sort by confidence
                    int confidenceCompare = Double.compare(c2.getConfidence(), c1.getConfidence());
                    if (confidenceCompare != 0) return confidenceCompare;
                    
                    // Secondary sort by day (earlier days first)
                    if (c1.getDay() != null && c2.getDay() != null) {
                        return Integer.compare(c1.getDay(), c2.getDay());
                    }
                    
                    // Tertiary sort by title length (shorter titles first)
                    return Integer.compare(c1.getTitle().length(), c2.getTitle().length());
                })
                .collect(Collectors.toList());
    }
    
    /**
     * Normalize text for comparison by removing accents and special characters
     */
    private String normalizeText(String text) {
        if (text == null) return null;
        
        // Normalize Unicode characters (remove accents)
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD);
        
        // Remove diacritical marks (accents)
        normalized = normalized.replaceAll("\\p{M}", "");
        
        // Convert to lowercase for case-insensitive comparison
        normalized = normalized.toLowerCase();
        
        // Remove extra whitespace
        normalized = normalized.trim().replaceAll("\\s+", " ");
        
        return normalized;
    }
    
    /**
     * Check if two strings are similar after normalization
     */
    private boolean isSimilarAfterNormalization(String text1, String text2) {
        if (text1 == null || text2 == null) return false;
        
        String norm1 = normalizeText(text1);
        String norm2 = normalizeText(text2);
        
        return norm1.equals(norm2) || 
               norm1.contains(norm2) || 
               norm2.contains(norm1);
    }
}
