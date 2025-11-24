package com.tripplanner.service;

import com.tripplanner.dto.MealMetadata;
import com.tripplanner.dto.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Service to populate meal metadata, reducing LLM dependency
 * Provides pre-computed metadata for meal types based on timing
 */
@Service
public class MealMetadataService {
    
    private static final Logger logger = LoggerFactory.getLogger(MealMetadataService.class);
    
    /**
     * Populate meal metadata based on title, timing, and budget tier
     */
    public MealMetadata populateMetadata(String title, String startTime, String budgetTier) {
        MealMetadata metadata = new MealMetadata();
        
        // Infer meal type from timing
        String mealType = inferMealTypeFromTime(startTime);
        metadata.setMealType(mealType);
        
        // Infer cuisine from title
        String cuisine = inferCuisineFromTitle(title);
        metadata.setCuisineType(cuisine);
        
        // Set price range based on budget tier
        String priceRange = mapBudgetToPriceRange(budgetTier);
        metadata.setPriceRange(priceRange);
        
        // Determine if reservation needed
        boolean needsReservation = determineReservationNeed(title, priceRange);
        metadata.setRequiresReservation(needsReservation);
        
        // Set dietary options (generic for now)
        metadata.setDietaryOptions(Arrays.asList("vegetarian", "non-vegetarian"));
        
        logger.debug("Populated meal metadata for '{}': type={}, cuisine={}, price={}", 
                    title, mealType, cuisine, priceRange);
        
        return metadata;
    }
    
    /**
     * Populate metadata for a node
     */
    public void populateNodeMetadata(NormalizedNode node, String budgetTier) {
        if (!"meal".equals(node.getType())) {
            return;
        }
        
        String title = node.getTitle();
        Long startTimeMs = node.getTiming() != null ? node.getTiming().getStartTime() : null;
        
        // Convert timestamp to time string (HH:mm format)
        String startTime = null;
        if (startTimeMs != null) {
            java.time.Instant instant = java.time.Instant.ofEpochMilli(startTimeMs);
            java.time.LocalTime time = java.time.LocalDateTime.ofInstant(instant, java.time.ZoneId.systemDefault()).toLocalTime();
            startTime = time.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
        }
        
        MealMetadata metadata = populateMetadata(title, startTime, budgetTier);
        node.setMetadata(metadata);
        
        // Also update node details category if not set
        if (node.getDetails() != null && node.getDetails().getCategory() == null) {
            node.getDetails().setCategory(metadata.getCuisineType());
        }
    }
    
    /**
     * Infer meal type from time of day
     */
    private String inferMealTypeFromTime(String startTime) {
        if (startTime == null || startTime.isEmpty()) {
            return "lunch"; // Default
        }
        
        try {
            LocalTime time = parseTime(startTime);
            
            // Breakfast: 6:00 AM - 10:30 AM
            if (time.isAfter(LocalTime.of(6, 0)) && time.isBefore(LocalTime.of(10, 30))) {
                return "breakfast";
            }
            
            // Brunch: 10:30 AM - 12:00 PM
            if (time.isAfter(LocalTime.of(10, 30)) && time.isBefore(LocalTime.of(12, 0))) {
                return "brunch";
            }
            
            // Lunch: 12:00 PM - 3:00 PM
            if (time.isAfter(LocalTime.of(12, 0)) && time.isBefore(LocalTime.of(15, 0))) {
                return "lunch";
            }
            
            // Snack/Tea: 3:00 PM - 5:00 PM
            if (time.isAfter(LocalTime.of(15, 0)) && time.isBefore(LocalTime.of(17, 0))) {
                return "snack";
            }
            
            // Dinner: 5:00 PM - 11:00 PM
            if (time.isAfter(LocalTime.of(17, 0)) && time.isBefore(LocalTime.of(23, 0))) {
                return "dinner";
            }
            
            // Late night: 11:00 PM - 6:00 AM
            return "snack";
            
        } catch (Exception e) {
            logger.warn("Failed to parse time '{}', defaulting to lunch", startTime);
            return "lunch";
        }
    }
    
    /**
     * Parse time string in various formats
     */
    private LocalTime parseTime(String timeStr) throws DateTimeParseException {
        // Try common formats
        String[] formats = {
            "HH:mm",
            "h:mm a",
            "h:mma",
            "HH:mm:ss"
        };
        
        for (String format : formats) {
            try {
                return LocalTime.parse(timeStr, DateTimeFormatter.ofPattern(format));
            } catch (DateTimeParseException e) {
                // Try next format
            }
        }
        
        // Try ISO format
        return LocalTime.parse(timeStr);
    }
    
    /**
     * Infer cuisine type from title keywords
     */
    private String inferCuisineFromTitle(String title) {
        if (title == null) {
            return "local";
        }
        
        String lower = title.toLowerCase();
        
        // Japanese
        if (lower.contains("sushi") || lower.contains("ramen") || lower.contains("japanese") || 
            lower.contains("izakaya") || lower.contains("tempura")) {
            return "japanese";
        }
        
        // Italian
        if (lower.contains("pizza") || lower.contains("pasta") || lower.contains("italian") || 
            lower.contains("trattoria") || lower.contains("risotto")) {
            return "italian";
        }
        
        // Chinese
        if (lower.contains("chinese") || lower.contains("dim sum") || lower.contains("hotpot") || 
            lower.contains("noodles") || lower.contains("dumpling")) {
            return "chinese";
        }
        
        // Indian
        if (lower.contains("indian") || lower.contains("curry") || lower.contains("tandoor") || 
            lower.contains("biryani") || lower.contains("masala")) {
            return "indian";
        }
        
        // French
        if (lower.contains("french") || lower.contains("bistro") || lower.contains("patisserie") || 
            lower.contains("brasserie")) {
            return "french";
        }
        
        // Thai
        if (lower.contains("thai") || lower.contains("pad thai") || lower.contains("tom yum")) {
            return "thai";
        }
        
        // Mexican
        if (lower.contains("mexican") || lower.contains("taco") || lower.contains("burrito")) {
            return "mexican";
        }
        
        // Street food
        if (lower.contains("street food") || lower.contains("food stall") || lower.contains("hawker")) {
            return "street_food";
        }
        
        // Cafe
        if (lower.contains("cafe") || lower.contains("coffee") || lower.contains("bakery")) {
            return "cafe";
        }
        
        // International/Fusion
        if (lower.contains("international") || lower.contains("fusion") || lower.contains("multi-cuisine")) {
            return "international";
        }
        
        // Default to local cuisine
        return "local";
    }
    
    /**
     * Map budget tier to price range
     */
    private String mapBudgetToPriceRange(String budgetTier) {
        if (budgetTier == null) {
            return "$$"; // Medium
        }
        
        switch (budgetTier.toLowerCase()) {
            case "budget":
            case "low":
                return "$";
            case "luxury":
            case "high":
                return "$$$$";
            case "premium":
                return "$$$";
            case "medium":
            case "moderate":
            default:
                return "$$";
        }
    }
    
    /**
     * Determine if reservation is needed based on title and price range
     */
    private boolean determineReservationNeed(String title, String priceRange) {
        if (title == null) {
            return false;
        }
        
        String lower = title.toLowerCase();
        
        // Always need reservation
        if (lower.contains("fine dining") || lower.contains("michelin") || 
            lower.contains("reservation required")) {
            return true;
        }
        
        // Never need reservation
        if (lower.contains("street food") || lower.contains("food stall") || 
            lower.contains("hawker") || lower.contains("food court") || 
            lower.contains("fast food")) {
            return false;
        }
        
        // Based on price range
        if ("$$$$".equals(priceRange) || "$$$".equals(priceRange)) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Get recommended meal types for a day
     */
    public List<String> getRecommendedMealTypes(int numberOfMeals) {
        List<String> mealTypes = new ArrayList<>();
        
        switch (numberOfMeals) {
            case 1:
                mealTypes.add("lunch");
                break;
            case 2:
                mealTypes.add("lunch");
                mealTypes.add("dinner");
                break;
            case 3:
                mealTypes.add("breakfast");
                mealTypes.add("lunch");
                mealTypes.add("dinner");
                break;
            case 4:
                mealTypes.add("breakfast");
                mealTypes.add("lunch");
                mealTypes.add("snack");
                mealTypes.add("dinner");
                break;
            default:
                mealTypes.add("lunch");
                mealTypes.add("dinner");
        }
        
        return mealTypes;
    }
}
