package com.tripplanner.service.metadata;

import com.tripplanner.dto.ActivityMetadata;
import com.tripplanner.dto.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Service to populate activity metadata, reducing LLM dependency
 * Provides pre-computed metadata for common activity types
 */
@Service
public class ActivityMetadataService {
    
    private static final Logger logger = LoggerFactory.getLogger(ActivityMetadataService.class);
    
    private final Map<String, ActivityMetadata> categoryDefaults;
    
    public ActivityMetadataService() {
        this.categoryDefaults = initializeCategoryDefaults();
    }
    
    /**
     * Populate activity metadata based on title and category
     */
    public ActivityMetadata populateMetadata(String title, String category) {
        ActivityMetadata metadata = new ActivityMetadata();
        
        // Determine category from title if not provided
        if (category == null || category.isEmpty()) {
            category = inferCategoryFromTitle(title);
        }
        metadata.setCategory(category);
        
        // Get defaults for this category
        ActivityMetadata defaults = categoryDefaults.get(category.toLowerCase());
        if (defaults != null) {
            metadata.setEstimatedDurationMinutes(defaults.getEstimatedDurationMinutes());
            metadata.setDifficultyLevel(defaults.getDifficultyLevel());
            metadata.setRequiresBooking(defaults.getRequiresBooking());
            metadata.setIsIndoor(defaults.getIsIndoor());
        } else {
            // Generic defaults
            metadata.setEstimatedDurationMinutes(120); // 2 hours
            metadata.setDifficultyLevel("moderate");
            metadata.setRequiresBooking(false);
            metadata.setIsIndoor(false);
        }
        
        // Override based on title keywords
        applyTitleBasedOverrides(metadata, title);
        
        logger.debug("Populated activity metadata for '{}': category={}, duration={}min, difficulty={}", 
                    title, metadata.getCategory(), metadata.getEstimatedDurationMinutes(), 
                    metadata.getDifficultyLevel());
        
        return metadata;
    }
    
    /**
     * Populate metadata for a node
     */
    public void populateNodeMetadata(NormalizedNode node) {
        if (!"attraction".equals(node.getType())) {
            return;
        }
        
        String title = node.getTitle();
        String category = node.getDetails() != null ? node.getDetails().getCategory() : null;
        
        ActivityMetadata metadata = populateMetadata(title, category);
        node.setMetadata(metadata);
        
        // Also update node timing if not set
        if (node.getTiming() != null && node.getTiming().getDurationMin() == null) {
            node.getTiming().setDurationMin(metadata.getEstimatedDurationMinutes());
        }
    }
    
    /**
     * Infer category from title keywords
     */
    private String inferCategoryFromTitle(String title) {
        if (title == null) {
            return "attraction";
        }
        
        String lower = title.toLowerCase();
        
        // Museums and cultural
        if (lower.contains("museum") || lower.contains("gallery") || lower.contains("exhibition")) {
            return "museum";
        }
        
        // Religious sites
        if (lower.contains("temple") || lower.contains("shrine") || lower.contains("church") || 
            lower.contains("mosque") || lower.contains("cathedral")) {
            return "religious_site";
        }
        
        // Historical
        if (lower.contains("palace") || lower.contains("fort") || lower.contains("castle") || 
            lower.contains("monument") || lower.contains("ruins")) {
            return "historical_site";
        }
        
        // Nature
        if (lower.contains("park") || lower.contains("garden") || lower.contains("beach") || 
            lower.contains("lake") || lower.contains("mountain") || lower.contains("waterfall")) {
            return "nature";
        }
        
        // Shopping
        if (lower.contains("market") || lower.contains("bazaar") || lower.contains("shopping") || 
            lower.contains("mall")) {
            return "shopping";
        }
        
        // Entertainment
        if (lower.contains("show") || lower.contains("performance") || lower.contains("theater") || 
            lower.contains("cinema")) {
            return "entertainment";
        }
        
        // Adventure
        if (lower.contains("trek") || lower.contains("hike") || lower.contains("climb") || 
            lower.contains("safari") || lower.contains("dive")) {
            return "adventure";
        }
        
        return "attraction";
    }
    
    /**
     * Apply title-based overrides to metadata
     */
    private void applyTitleBasedOverrides(ActivityMetadata metadata, String title) {
        if (title == null) {
            return;
        }
        
        String lower = title.toLowerCase();
        
        // Duration overrides
        if (lower.contains("quick") || lower.contains("brief")) {
            metadata.setEstimatedDurationMinutes(60);
        } else if (lower.contains("full day") || lower.contains("all day")) {
            metadata.setEstimatedDurationMinutes(480); // 8 hours
        } else if (lower.contains("half day")) {
            metadata.setEstimatedDurationMinutes(240); // 4 hours
        }
        
        // Booking overrides
        if (lower.contains("ticket") || lower.contains("reservation") || lower.contains("booking")) {
            metadata.setRequiresBooking(true);
        }
        
        // Indoor/outdoor overrides
        if (lower.contains("indoor") || lower.contains("museum") || lower.contains("gallery")) {
            metadata.setIsIndoor(true);
        } else if (lower.contains("outdoor") || lower.contains("beach") || lower.contains("park")) {
            metadata.setIsIndoor(false);
        }
        
        // Difficulty overrides
        if (lower.contains("easy") || lower.contains("relaxed")) {
            metadata.setDifficultyLevel("easy");
        } else if (lower.contains("challenging") || lower.contains("difficult") || lower.contains("strenuous")) {
            metadata.setDifficultyLevel("hard");
        }
    }
    
    /**
     * Initialize default metadata for common categories
     */
    private Map<String, ActivityMetadata> initializeCategoryDefaults() {
        Map<String, ActivityMetadata> defaults = new HashMap<>();
        
        // Museum
        ActivityMetadata museum = new ActivityMetadata();
        museum.setCategory("museum");
        museum.setEstimatedDurationMinutes(120);
        museum.setDifficultyLevel("easy");
        museum.setRequiresBooking(true);
        museum.setIsIndoor(true);
        defaults.put("museum", museum);
        
        // Religious site
        ActivityMetadata religious = new ActivityMetadata();
        religious.setCategory("religious_site");
        religious.setEstimatedDurationMinutes(60);
        religious.setDifficultyLevel("easy");
        religious.setRequiresBooking(false);
        religious.setIsIndoor(false);
        defaults.put("religious_site", religious);
        
        // Historical site
        ActivityMetadata historical = new ActivityMetadata();
        historical.setCategory("historical_site");
        historical.setEstimatedDurationMinutes(90);
        historical.setDifficultyLevel("easy");
        historical.setRequiresBooking(true);
        historical.setIsIndoor(false);
        defaults.put("historical_site", historical);
        
        // Nature
        ActivityMetadata nature = new ActivityMetadata();
        nature.setCategory("nature");
        nature.setEstimatedDurationMinutes(180);
        nature.setDifficultyLevel("moderate");
        nature.setRequiresBooking(false);
        nature.setIsIndoor(false);
        defaults.put("nature", nature);
        
        // Shopping
        ActivityMetadata shopping = new ActivityMetadata();
        shopping.setCategory("shopping");
        shopping.setEstimatedDurationMinutes(120);
        shopping.setDifficultyLevel("easy");
        shopping.setRequiresBooking(false);
        shopping.setIsIndoor(true);
        defaults.put("shopping", shopping);
        
        // Entertainment
        ActivityMetadata entertainment = new ActivityMetadata();
        entertainment.setCategory("entertainment");
        entertainment.setEstimatedDurationMinutes(150);
        entertainment.setDifficultyLevel("easy");
        entertainment.setRequiresBooking(true);
        entertainment.setIsIndoor(true);
        defaults.put("entertainment", entertainment);
        
        // Adventure
        ActivityMetadata adventure = new ActivityMetadata();
        adventure.setCategory("adventure");
        adventure.setEstimatedDurationMinutes(240);
        adventure.setDifficultyLevel("hard");
        adventure.setRequiresBooking(true);
        adventure.setIsIndoor(false);
        defaults.put("adventure", adventure);
        
        return defaults;
    }
}
