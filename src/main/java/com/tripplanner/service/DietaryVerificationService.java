package com.tripplanner.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for verifying dietary restriction support at restaurants.
 * Uses curated chain database and keyword matching for confidence scoring.
 */
@Service
public class DietaryVerificationService {
    
    private static final Logger logger = LoggerFactory.getLogger(DietaryVerificationService.class);
    
    // Curated database of restaurant chains with known dietary options
    private static final Map<String, List<String>> DIETARY_FRIENDLY_CHAINS = new HashMap<>();
    
    static {
        // Vegetarian-friendly chains
        DIETARY_FRIENDLY_CHAINS.put("saravana bhavan", Arrays.asList("vegetarian", "vegan"));
        DIETARY_FRIENDLY_CHAINS.put("haldiram", Arrays.asList("vegetarian"));
        DIETARY_FRIENDLY_CHAINS.put("haldiram's", Arrays.asList("vegetarian"));
        DIETARY_FRIENDLY_CHAINS.put("bikanervala", Arrays.asList("vegetarian"));
        DIETARY_FRIENDLY_CHAINS.put("moti mahal", Arrays.asList("vegetarian", "non-vegetarian"));
        
        // International chains with dietary options
        DIETARY_FRIENDLY_CHAINS.put("subway", Arrays.asList("vegetarian", "vegan", "gluten-free"));
        DIETARY_FRIENDLY_CHAINS.put("chipotle", Arrays.asList("vegetarian", "vegan", "gluten-free"));
        DIETARY_FRIENDLY_CHAINS.put("panera", Arrays.asList("vegetarian", "vegan", "gluten-free"));
        DIETARY_FRIENDLY_CHAINS.put("sweetgreen", Arrays.asList("vegetarian", "vegan", "gluten-free"));
        
        // Vegan-specific chains
        DIETARY_FRIENDLY_CHAINS.put("loving hut", Arrays.asList("vegan", "vegetarian"));
        DIETARY_FRIENDLY_CHAINS.put("veggie grill", Arrays.asList("vegan", "vegetarian"));
        
        // Halal chains
        DIETARY_FRIENDLY_CHAINS.put("nando's", Arrays.asList("halal"));
        DIETARY_FRIENDLY_CHAINS.put("kfc", Arrays.asList("halal")); // In some regions
        
        // Kosher chains
        DIETARY_FRIENDLY_CHAINS.put("mendy's", Arrays.asList("kosher"));
        DIETARY_FRIENDLY_CHAINS.put("prime grill", Arrays.asList("kosher"));
    }
    
    // Keywords that indicate dietary support
    private static final Map<String, List<String>> DIETARY_KEYWORDS = new HashMap<>();
    
    static {
        DIETARY_KEYWORDS.put("vegetarian", Arrays.asList(
            "vegetarian", "veg", "veggie", "plant-based", "meatless"
        ));
        
        DIETARY_KEYWORDS.put("vegan", Arrays.asList(
            "vegan", "plant-based", "dairy-free", "egg-free"
        ));
        
        DIETARY_KEYWORDS.put("gluten-free", Arrays.asList(
            "gluten-free", "gluten free", "celiac", "gf options"
        ));
        
        DIETARY_KEYWORDS.put("halal", Arrays.asList(
            "halal", "halal certified", "halal meat"
        ));
        
        DIETARY_KEYWORDS.put("kosher", Arrays.asList(
            "kosher", "kosher certified", "kosher kitchen"
        ));
        
        DIETARY_KEYWORDS.put("dairy-free", Arrays.asList(
            "dairy-free", "dairy free", "lactose-free", "no dairy"
        ));
        
        DIETARY_KEYWORDS.put("nut-free", Arrays.asList(
            "nut-free", "nut free", "no nuts", "nut allergy"
        ));
    }
    
    /**
     * Dietary match result with confidence score
     */
    public static class DietaryMatch {
        private String restriction;
        private boolean supported;
        private int confidenceScore; // 0-100
        private String reason;
        private List<String> matchedKeywords;
        
        public DietaryMatch(String restriction) {
            this.restriction = restriction;
            this.matchedKeywords = new ArrayList<>();
        }
        
        // Getters and setters
        public String getRestriction() { return restriction; }
        public void setRestriction(String restriction) { this.restriction = restriction; }
        public boolean isSupported() { return supported; }
        public void setSupported(boolean supported) { this.supported = supported; }
        public int getConfidenceScore() { return confidenceScore; }
        public void setConfidenceScore(int confidenceScore) { this.confidenceScore = confidenceScore; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
        public List<String> getMatchedKeywords() { return matchedKeywords; }
        public void setMatchedKeywords(List<String> matchedKeywords) { this.matchedKeywords = matchedKeywords; }
        public void addMatchedKeyword(String keyword) { this.matchedKeywords.add(keyword); }
    }
    
    /**
     * Verify dietary support for a restaurant
     * 
     * @param restaurantName Name of the restaurant
     * @param placeTypes Google Places types (e.g., ["restaurant", "vegetarian_restaurant"])
     * @param requiredRestrictions List of dietary restrictions to check
     * @return List of dietary matches with confidence scores
     */
    public List<DietaryMatch> verifyDietarySupport(
            String restaurantName,
            List<String> placeTypes,
            List<String> requiredRestrictions) {
        
        List<DietaryMatch> matches = new ArrayList<>();
        
        if (requiredRestrictions == null || requiredRestrictions.isEmpty()) {
            return matches;
        }
        
        for (String restriction : requiredRestrictions) {
            DietaryMatch match = verifyRestriction(
                restaurantName, 
                placeTypes, 
                restriction.toLowerCase()
            );
            matches.add(match);
        }
        
        return matches;
    }
    
    /**
     * Verify a single dietary restriction
     */
    private DietaryMatch verifyRestriction(
            String restaurantName, 
            List<String> placeTypes, 
            String restriction) {
        
        DietaryMatch match = new DietaryMatch(restriction);
        int score = 0;
        List<String> reasons = new ArrayList<>();
        
        String nameLower = restaurantName != null ? restaurantName.toLowerCase() : "";
        
        // Check curated chains (highest confidence)
        for (Map.Entry<String, List<String>> entry : DIETARY_FRIENDLY_CHAINS.entrySet()) {
            if (nameLower.contains(entry.getKey())) {
                if (entry.getValue().contains(restriction)) {
                    score += 80;
                    reasons.add("Known chain with " + restriction + " options");
                    match.addMatchedKeyword(entry.getKey());
                    logger.info("Matched curated chain: {} for {}", entry.getKey(), restriction);
                }
            }
        }
        
        // Check place types (high confidence)
        if (placeTypes != null) {
            for (String type : placeTypes) {
                String typeLower = type.toLowerCase();
                
                if (restriction.equals("vegetarian") && 
                    (typeLower.contains("vegetarian") || typeLower.equals("veg"))) {
                    score += 70;
                    reasons.add("Marked as vegetarian restaurant");
                    match.addMatchedKeyword("vegetarian_restaurant");
                }
                
                if (restriction.equals("vegan") && typeLower.contains("vegan")) {
                    score += 70;
                    reasons.add("Marked as vegan restaurant");
                    match.addMatchedKeyword("vegan_restaurant");
                }
            }
        }
        
        // Check keywords in restaurant name (medium confidence)
        List<String> keywords = DIETARY_KEYWORDS.get(restriction);
        if (keywords != null) {
            for (String keyword : keywords) {
                if (nameLower.contains(keyword.toLowerCase())) {
                    score += 40;
                    reasons.add("Name contains '" + keyword + "'");
                    match.addMatchedKeyword(keyword);
                }
            }
        }
        
        // Special case: Indian restaurants often have vegetarian options
        if (restriction.equals("vegetarian") && 
            (nameLower.contains("indian") || nameLower.contains("desi") || 
             nameLower.contains("punjabi") || nameLower.contains("south indian"))) {
            score += 30;
            reasons.add("Indian cuisine typically has vegetarian options");
        }
        
        // Special case: Mediterranean restaurants often have vegan/vegetarian
        if ((restriction.equals("vegetarian") || restriction.equals("vegan")) &&
            (nameLower.contains("mediterranean") || nameLower.contains("middle eastern") ||
             nameLower.contains("falafel") || nameLower.contains("hummus"))) {
            score += 30;
            reasons.add("Mediterranean cuisine typically has plant-based options");
        }
        
        // Cap score at 100
        score = Math.min(100, score);
        
        // Determine if supported based on score
        match.setSupported(score >= 50);
        match.setConfidenceScore(score);
        
        if (score >= 80) {
            match.setReason("High confidence: " + String.join(", ", reasons));
        } else if (score >= 50) {
            match.setReason("Moderate confidence: " + String.join(", ", reasons) + 
                          " - Verify with restaurant");
        } else if (score > 0) {
            match.setReason("Low confidence: " + String.join(", ", reasons) + 
                          " - Strongly recommend verifying");
        } else {
            match.setReason("No indication of " + restriction + " support - verify before visiting");
        }
        
        logger.info("Dietary verification for {}: {} (score: {}, supported: {})", 
                   restaurantName, restriction, score, match.isSupported());
        
        return match;
    }
    
    /**
     * Get overall dietary compatibility score for a restaurant
     * 
     * @param matches List of dietary matches
     * @return Average confidence score (0-100)
     */
    public int getOverallScore(List<DietaryMatch> matches) {
        if (matches == null || matches.isEmpty()) {
            return 100; // No restrictions = fully compatible
        }
        
        int totalScore = 0;
        for (DietaryMatch match : matches) {
            totalScore += match.getConfidenceScore();
        }
        
        return totalScore / matches.size();
    }
    
    /**
     * Check if all dietary restrictions are supported
     * 
     * @param matches List of dietary matches
     * @return true if all restrictions are supported with confidence >= 50
     */
    public boolean allRestrictionsSupported(List<DietaryMatch> matches) {
        if (matches == null || matches.isEmpty()) {
            return true;
        }
        
        return matches.stream().allMatch(DietaryMatch::isSupported);
    }
    
    /**
     * Generate warning message for unsupported restrictions
     * 
     * @param matches List of dietary matches
     * @return Warning message or null if all supported
     */
    public String generateWarning(List<DietaryMatch> matches) {
        if (matches == null || matches.isEmpty()) {
            return null;
        }
        
        List<String> unsupported = matches.stream()
            .filter(m -> !m.isSupported())
            .map(DietaryMatch::getRestriction)
            .collect(Collectors.toList());
        
        if (unsupported.isEmpty()) {
            return null;
        }
        
        return "⚠️ No clear indication of support for: " + String.join(", ", unsupported) + 
               ". Please verify with restaurant before visiting.";
    }
}
