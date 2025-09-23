package com.tripplanner.service;

import com.tripplanner.dto.IntentResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Service for classifying user intents from natural language text.
 * Uses deterministic pre-router with regex patterns for common intents,
 * with fallback to LLM classification for ambiguous cases.
 */
@Service
public class IntentClassificationService {
    
    private static final Logger logger = LoggerFactory.getLogger(IntentClassificationService.class);
    
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
        "(?i).*\\b(what|how|why|explain|tell me|describe|show me)\\b.*", 
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
     */
    public IntentResult classifyIntent(String text, String selectedNodeId, Integer day) {
        logger.debug("Classifying intent for text: '{}', selectedNodeId: {}, day: {}", text, selectedNodeId, day);
        
        // Try pre-router classification first
        IntentResult result = preRouterClassification(text, selectedNodeId, day);
        
        if (result != null) {
            logger.debug("Pre-router classified intent as: {}", result.getIntent());
            return result;
        }
        
        // Fallback to LLM classification for ambiguous cases
        logger.debug("Pre-router could not classify, falling back to LLM");
        return llmClassification(text, selectedNodeId, day);
    }
    
    /**
     * Deterministic classification using regex patterns and keywords.
     */
    private IntentResult preRouterClassification(String text, String selectedNodeId, Integer day) {
        
        // Check for explicit intents in order of specificity
        if (isReplanToday(text)) {
            Map<String, String> entities = extractEntities(text);
            return IntentResult.replanToday(day, entities);
        }
        
        if (isBookNode(text)) {
            List<String> nodeIds = selectedNodeId != null ? List.of(selectedNodeId) : List.of();
            return IntentResult.bookNode(nodeIds);
        }
        
        if (isUndo(text)) {
            return IntentResult.undo(null); // Version will be determined later
        }
        
        if (isDeleteNode(text)) {
            List<String> nodeIds = selectedNodeId != null ? List.of(selectedNodeId) : List.of();
            return IntentResult.deleteNode(nodeIds);
        }
        
        if (isReplaceNode(text)) {
            List<String> nodeIds = selectedNodeId != null ? List.of(selectedNodeId) : List.of();
            Map<String, String> entities = extractEntities(text);
            return IntentResult.replaceNode(nodeIds, entities);
        }
        
        if (isMoveTime(text)) {
            List<String> nodeIds = selectedNodeId != null ? List.of(selectedNodeId) : List.of();
            Map<String, String> entities = extractEntities(text);
            return IntentResult.moveTime(nodeIds, entities);
        }
        
        if (isInsertPlace(text)) {
            Map<String, String> entities = extractEntities(text);
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
     * Extract entities (time, location, etc.) from text.
     */
    private Map<String, String> extractEntities(String text) {
        Map<String, String> entities = new HashMap<>();
        
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
        } else if (text.toLowerCase().contains("museum") || text.toLowerCase().contains("attraction") || 
                   text.toLowerCase().contains("sight") || text.toLowerCase().contains("visit")) {
            entities.put("category", "attraction");
        } else if (text.toLowerCase().contains("hotel") || text.toLowerCase().contains("stay") || 
                   text.toLowerCase().contains("accommodation")) {
            entities.put("category", "accommodation");
        }
        
        return entities;
    }
}
