package com.tripplanner.service;

import com.tripplanner.dto.IntentResult;
import com.tripplanner.service.cache.ToolCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Service for classifying user intents from natural language text.
 * Uses deterministic pre-router with regex patterns for common intents,
 * with fallback to LLM classification for ambiguous cases.
 * 
 * OPTIMIZATION: Caches intent classification results to avoid repeated LLM calls.
 */
@Service
public class IntentClassificationService {
    
    private static final Logger logger = LoggerFactory.getLogger(IntentClassificationService.class);
    
    @Autowired(required = false)
    private ToolCacheService toolCacheService;
    
    @Value("${features.editor-tools.enabled:false}")
    private boolean cachingEnabled;
    
    // Regex patterns for intent classification
    private static final Pattern REPLAN_TODAY_PATTERN = Pattern.compile(
        "(?i).*\\b(replan|reschedule|start over|from now|today|current day)\\b.*", 
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern MOVE_TIME_PATTERN = Pattern.compile(
        "(?i).*\\b(move|shift|reschedule|change time|at \\d{1,2}:?\\d{0,2}|\\d{1,2}:?\\d{0,2})\\b.*", 
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern INSERT_PLACE_PATTERN = Pattern.compile(
        "(?i).*\\b(add|insert|include|visit|go to|see|check out)\\b.*", 
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern DELETE_NODE_PATTERN = Pattern.compile(
        "(?i).*\\b(remove|delete|skip|cancel|avoid)\\b.*", 
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern REPLACE_NODE_PATTERN = Pattern.compile(
        "(?i).*\\b(replace|swap|instead of|alternative|substitute|don't want|dont want|don't like|dont like|something else|different)\\b.*", 
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern BOOK_NODE_PATTERN = Pattern.compile(
        "(?i).*\\b(book|reserve|buy|purchase|ticket|booking)\\b.*", 
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern UNDO_PATTERN = Pattern.compile(
        "(?i).*\\b(undo|revert|back|previous|last change|go back)\\b.*", 
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern EXPLAIN_PATTERN = Pattern.compile(
        "(?i).*\\b(what|how|why|explain|tell me|describe)\\b.*", 
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern SEARCH_PLACE_PATTERN = Pattern.compile(
        "(?i).*\\b(show me more|find|suggest|search for|discover|what other|more options|more places|other attractions|other museums|other restaurants)\\b.*\\b(places?|museums?|restaurants?|attractions?|activities?|things to do|options?)\\b.*", 
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern ADD_DAY_PATTERN = Pattern.compile(
        "(?i).*\\b(add|extend|extra|more|another|one more)\\b.*\\b(day|days)\\b.*", 
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern REMOVE_DAY_PATTERN = Pattern.compile(
        "(?i).*\\b(remove|delete|shorten|less|fewer|drop)\\b.*\\b(day|days)\\b.*", 
        Pattern.CASE_INSENSITIVE
    );
    
    // Time extraction patterns
    private static final Pattern TIME_PATTERN = Pattern.compile(
        "\\b(\\d{1,2}):?(\\d{0,2})\\s*(am|pm|a\\.m\\.|p\\.m\\.)?\\b", 
        Pattern.CASE_INSENSITIVE
    );
    
    // Location extraction patterns
    private static final Pattern LOCATION_PATTERN = Pattern.compile(
        "\\b(near|at|in|around|close to|by)\\s+([^,]+)", 
        Pattern.CASE_INSENSITIVE
    );
    
    /**
     * Classify the intent from user text using deterministic pre-router.
     * Falls back to LLM classification if no clear intent is detected.
     * 
     * OPTIMIZATION: Caches intent results to avoid repeated classification.
     */
    public IntentResult classifyIntent(String text, String selectedNodeId, Integer day) {
        logger.debug("Classifying intent for text: '{}', selectedNodeId: {}, day: {}", text, selectedNodeId, day);
        
        // ========== TOOL INTEGRATION: Check intent cache first ==========
        if (cachingEnabled && toolCacheService != null) {
            String textHash = Integer.toHexString(text.hashCode());
            String contextHash = Integer.toHexString((selectedNodeId + "_" + day).hashCode());
            String cacheKey = "intent:" + textHash + ":" + contextHash;
            
            Optional<IntentResult> cached = toolCacheService.get(cacheKey, IntentResult.class);
            if (cached.isPresent()) {
                logger.info("📊 CACHE HIT: Intent classification (saved ~1s)");
                return cached.get();
            }
            
            logger.debug("📊 CACHE MISS: Intent classification");
        }
        
        // Try pre-router classification first
        IntentResult result = preRouterClassification(text, selectedNodeId, day);
        
        if (result != null) {
            logger.debug("Pre-router classified intent as: {}", result.getIntent());
            
            // Cache the result
            if (cachingEnabled && toolCacheService != null) {
                String textHash = Integer.toHexString(text.hashCode());
                String contextHash = Integer.toHexString((selectedNodeId + "_" + day).hashCode());
                String cacheKey = "intent:" + textHash + ":" + contextHash;
                toolCacheService.put(cacheKey, result, Duration.ofHours(1));
                logger.debug("✅ Cached intent result for: {}", text);
            }
            
            return result;
        }
        
        // Fallback to LLM classification for ambiguous cases
        logger.debug("Pre-router could not classify, falling back to LLM");
        IntentResult llmResult = llmClassification(text, selectedNodeId, day);
        
        // Cache LLM result as well
        if (cachingEnabled && toolCacheService != null && llmResult != null) {
            String textHash = Integer.toHexString(text.hashCode());
            String contextHash = Integer.toHexString((selectedNodeId + "_" + day).hashCode());
            String cacheKey = "intent:" + textHash + ":" + contextHash;
            toolCacheService.put(cacheKey, llmResult, Duration.ofHours(1));
            logger.debug("✅ Cached LLM intent result for: {}", text);
        }
        
        return llmResult;
    }
    
    /**
     * Deterministic classification using regex patterns and keywords.
     */
    private IntentResult preRouterClassification(String text, String selectedNodeId, Integer day) {
        
        // Check for explicit intents in order of specificity
        // IMPORTANT: Check search BEFORE explain, as "show me more" should be search, not explain
        if (isSearchPlace(text)) {
            Map<String, Object> entities = extractEntities(text);
            entities.put("query", text); // Store full query for context
            return IntentResult.searchPlace(day, entities);
        }
        
        if (isReplanToday(text)) {
            Map<String, Object> entities = extractEntities(text);
            return IntentResult.replanToday(day, entities);
        }
        
        if (isBookNode(text)) {
            List<String> nodeIds = selectedNodeId != null ? List.of(selectedNodeId) : List.of();
            return IntentResult.bookNode(nodeIds);
        }
        
        if (isUndo(text)) {
            return IntentResult.undo(null); // Version will be determined later
        }
        
        if (isAddDay(text)) {
            Map<String, Object> entities = extractEntities(text);
            return IntentResult.addDay(entities);
        }
        
        if (isRemoveDay(text)) {
            Map<String, Object> entities = extractEntities(text);
            return IntentResult.removeDay(entities);
        }
        
        if (isDeleteNode(text)) {
            List<String> nodeIds = selectedNodeId != null ? List.of(selectedNodeId) : List.of();
            return IntentResult.deleteNode(nodeIds);
        }
        
        if (isReplaceNode(text)) {
            List<String> nodeIds = selectedNodeId != null ? List.of(selectedNodeId) : List.of();
            Map<String, Object> entities = extractEntities(text);
            return IntentResult.replaceNode(nodeIds, entities);
        }
        
        if (isMoveTime(text)) {
            List<String> nodeIds = selectedNodeId != null ? List.of(selectedNodeId) : List.of();
            Map<String, Object> entities = extractEntities(text);
            return IntentResult.moveTime(nodeIds, entities);
        }
        
        if (isInsertPlace(text)) {
            Map<String, Object> entities = extractEntities(text);
            return IntentResult.insertPlace(day, entities);
        }
        
        if (isExplain(text)) {
            return IntentResult.explain();
        }
        
        return null; // No clear intent detected
    }
    
    /**
     * LLM-based classification for ambiguous cases.
     * For now, returns a default intent based on context.
     */
    private IntentResult llmClassification(String text, String selectedNodeId, Integer day) {
        logger.debug("LLM classification not implemented, using fallback logic");
        
        // Simple fallback logic based on context
        if (selectedNodeId != null) {
            // If a node is selected, assume it's a modification
            return IntentResult.moveTime(List.of(selectedNodeId), extractEntities(text));
        }
        
        if (day != null) {
            // If a day is specified, assume it's adding something
            return IntentResult.insertPlace(day, extractEntities(text));
        }
        
        // Default to explain if no clear context
        return IntentResult.explain();
    }
    
    /**
     * Check if text indicates replanning from today.
     */
    private boolean isReplanToday(String text) {
        return REPLAN_TODAY_PATTERN.matcher(text).matches();
    }
    
    /**
     * Check if text indicates moving or rescheduling time.
     */
    private boolean isMoveTime(String text) {
        return MOVE_TIME_PATTERN.matcher(text).matches();
    }
    
    /**
     * Check if text indicates adding a new place.
     */
    private boolean isInsertPlace(String text) {
        return INSERT_PLACE_PATTERN.matcher(text).matches();
    }
    
    /**
     * Check if text indicates deleting/removing a node.
     */
    private boolean isDeleteNode(String text) {
        return DELETE_NODE_PATTERN.matcher(text).matches();
    }
    
    /**
     * Check if text indicates replacing a node.
     */
    private boolean isReplaceNode(String text) {
        return REPLACE_NODE_PATTERN.matcher(text).matches();
    }
    
    /**
     * Check if text indicates booking a node.
     */
    private boolean isBookNode(String text) {
        return BOOK_NODE_PATTERN.matcher(text).matches();
    }
    
    /**
     * Check if text indicates undoing a change.
     */
    private boolean isUndo(String text) {
        return UNDO_PATTERN.matcher(text).matches();
    }
    
    /**
     * Check if text indicates asking for explanation.
     */
    private boolean isExplain(String text) {
        return EXPLAIN_PATTERN.matcher(text).matches();
    }
    
    /**
     * Check if text indicates searching for new places.
     */
    private boolean isSearchPlace(String text) {
        return SEARCH_PLACE_PATTERN.matcher(text).matches();
    }
    
    /**
     * Check if text indicates adding a day.
     */
    private boolean isAddDay(String text) {
        return ADD_DAY_PATTERN.matcher(text).matches();
    }
    
    /**
     * Check if text indicates removing a day.
     */
    private boolean isRemoveDay(String text) {
        return REMOVE_DAY_PATTERN.matcher(text).matches();
    }
    
    /**
     * Extract entities (time, location, etc.) from text.
     */
    private Map<String, Object> extractEntities(String text) {
        Map<String, Object> entities = new HashMap<>();
        
        // Extract time
        var timeMatcher = TIME_PATTERN.matcher(text);
        if (timeMatcher.find()) {
            String time = timeMatcher.group();
            entities.put("time", time);
        }
        
        // Extract location
        var locationMatcher = LOCATION_PATTERN.matcher(text);
        if (locationMatcher.find()) {
            String location = locationMatcher.group(2).trim();
            entities.put("location", location);
        }
        
        // Extract category/type hints
        if (text.toLowerCase().contains("restaurant") || text.toLowerCase().contains("eat") || 
            text.toLowerCase().contains("lunch") || text.toLowerCase().contains("dinner")) {
            entities.put("category", "meal");
            entities.put("placeType", "restaurant");
        } else if (text.toLowerCase().contains("museum")) {
            entities.put("category", "attraction");
            entities.put("placeType", "museum");
        } else if (text.toLowerCase().contains("attraction") || text.toLowerCase().contains("tourist") ||
                   text.toLowerCase().contains("sight") || text.toLowerCase().contains("visit")) {
            entities.put("category", "attraction");
            entities.put("placeType", "tourist_attraction");
        } else if (text.toLowerCase().contains("hotel") || text.toLowerCase().contains("stay") || 
                   text.toLowerCase().contains("accommodation")) {
            entities.put("category", "accommodation");
            entities.put("placeType", "lodging");
        } else if (text.toLowerCase().contains("cafe") || text.toLowerCase().contains("coffee")) {
            entities.put("category", "meal");
            entities.put("placeType", "cafe");
        } else if (text.toLowerCase().contains("bar") || text.toLowerCase().contains("pub")) {
            entities.put("category", "meal");
            entities.put("placeType", "bar");
        } else if (text.toLowerCase().contains("park") || text.toLowerCase().contains("garden")) {
            entities.put("category", "attraction");
            entities.put("placeType", "park");
        }
        
        return entities;
    }
}
