package com.tripplanner.agents;

import com.tripplanner.dto.AgentCapabilities;
import com.tripplanner.dto.AgentEvent;
import com.tripplanner.dto.ChangeSet;
import com.tripplanner.dto.NormalizedItinerary;
import com.tripplanner.dto.memory.Memory;
import com.tripplanner.dto.memory.MemoryQuery;
import com.tripplanner.dto.memory.UserProfile;
import com.tripplanner.enums.MemoryCategory;
import com.tripplanner.service.agents.AgentEventBus;
import com.tripplanner.service.memory.MemoryConfidenceService;
import com.tripplanner.service.memory.MemoryService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * MemoryAgent - Central hub for storing, retrieving, and learning from user data
 * 
 * Single Responsibility: Store, retrieve, and learn from ALL user data across the entire system
 * 
 * Responsibilities:
 * - Store user preferences and patterns
 * - Retrieve relevant memories for agents
 * - Manage confidence scores with time decay
 * - Learn patterns from user behavior
 * - Provide aggregated user profiles
 * 
 * Does NOT handle: Validation, itinerary generation, or business logic
 * (Those are handled by specialized agents)
 * 
 * Storage: itineraries/{itineraryId}/memory/{memoryId}
 */
@Component
public class MemoryAgent extends BaseAgent {
    
    private final MemoryService memoryService;
    private final MemoryConfidenceService confidenceService;
    
    public MemoryAgent(AgentEventBus eventBus,
                      MemoryService memoryService,
                      MemoryConfidenceService confidenceService) {
        super(eventBus, AgentEvent.AgentKind.ENRICHMENT); // Using ENRICHMENT kind for memory operations
        this.memoryService = memoryService;
        this.confidenceService = confidenceService;
    }
    
    @Override
    public AgentCapabilities getCapabilities() {
        AgentCapabilities capabilities = new AgentCapabilities();
        capabilities.addSupportedTask("memory_management");
        capabilities.setPriority(5); // High priority for memory operations
        capabilities.setChatEnabled(false); // Not a chat agent
        capabilities.setConfigurationValue("requiresLLM", false);
        capabilities.setConfigurationValue("handlesUserData", true);
        return capabilities;
    }
    
    @Override
    protected <T> T executeInternal(String itineraryId, AgentRequest<T> request) {
        logger.info("=== MEMORY AGENT ===");
        logger.info("Itinerary ID: {}", itineraryId);
        
        // MemoryAgent doesn't use the standard execute pattern
        // It provides direct methods for memory operations
        // This method is here for BaseAgent compliance
        
        logger.info("=== MEMORY AGENT COMPLETE ===");
        return null;
    }
    
    @Override
    protected String getAgentName() {
        return "MemoryAgent";
    }
    
    // ========== CORE MEMORY OPERATIONS ==========
    
    /**
     * Store memory asynchronously
     */
    public CompletableFuture<Memory> store(String itineraryId, Memory memory) {
        logger.debug("Storing {} memory for itinerary {}", memory.getCategory(), itineraryId);
        return memoryService.storeAsync(itineraryId, memory);
    }
    
    /**
     * Retrieve memories with query
     */
    public List<Memory> retrieve(String itineraryId, MemoryQuery query) {
        logger.debug("Retrieving memories for itinerary {} with query", itineraryId);
        return memoryService.query(itineraryId, query);
    }
    
    /**
     * Get all memories for itinerary
     */
    public List<Memory> getAll(String itineraryId) {
        logger.debug("Retrieving all memories for itinerary {}", itineraryId);
        return memoryService.getAll(itineraryId);
    }
    
    /**
     * Get memories by category
     */
    public List<Memory> getByCategory(String itineraryId, MemoryCategory category) {
        logger.debug("Retrieving {} memories for itinerary {}", category, itineraryId);
        return memoryService.getByCategory(itineraryId, category);
    }
    
    /**
     * Update memory asynchronously
     */
    public CompletableFuture<Memory> update(String itineraryId, String memoryId, Memory updates) {
        logger.debug("Updating memory {} for itinerary {}", memoryId, itineraryId);
        return memoryService.updateAsync(itineraryId, memoryId, updates);
    }
    
    /**
     * Delete memory asynchronously
     */
    public CompletableFuture<Void> delete(String itineraryId, String memoryId) {
        logger.debug("Deleting memory {} for itinerary {}", memoryId, itineraryId);
        return memoryService.deleteAsync(itineraryId, memoryId);
    }
    
    /**
     * Get aggregated user profile
     */
    public UserProfile getProfile(String itineraryId) {
        logger.debug("Getting user profile for itinerary {}", itineraryId);
        return memoryService.getProfile(itineraryId);
    }
    
    // ========== LIFECYCLE EVENT HANDLERS ==========
    
    /**
     * Handle itinerary completion - store summary and learn patterns
     */
    public void onItineraryComplete(String itineraryId, NormalizedItinerary itinerary) {
        logger.info("Handling itinerary completion for: {}", itineraryId);
        
        // TODO: Store itinerary summary as memory
        // TODO: Trigger pattern learning
        
        logger.debug("Itinerary completion handling not yet implemented");
    }
    
    /**
     * Handle user edit - update relevant memories
     */
    public void onUserEdit(String itineraryId, ChangeSet changeSet) {
        logger.info("Handling user edit for itinerary: {}", itineraryId);
        
        // TODO: Analyze edit and update relevant memories
        // TODO: Boost confidence for confirmed preferences
        
        logger.debug("User edit handling not yet implemented");
    }
    
    /**
     * Handle validation dismissal - store dismissal memory
     */
    public void onValidationDismissal(String itineraryId, String issueType, String reason) {
        logger.info("Handling validation dismissal for itinerary: {}", itineraryId);
        
        // TODO: Store dismissal as memory
        // TODO: Mark for future filtering
        
        logger.debug("Validation dismissal handling not yet implemented");
    }
    
    // ========== PATTERN LEARNING ==========
    
    /**
     * Learn patterns from historical data
     */
    public void learnPatterns(String itineraryId) {
        logger.info("Learning patterns for itinerary: {}", itineraryId);
        
        // TODO: Implement pattern learning
        // TODO: Extract activity preferences
        // TODO: Extract timing patterns
        // TODO: Extract budget behavior
        
        logger.debug("Pattern learning not yet implemented");
    }
    
    // ========== CONFIDENCE MANAGEMENT ==========
    
    /**
     * Boost confidence for a memory
     */
    public void boostConfidence(Memory memory) {
        confidenceService.boostConfidence(memory);
    }
    
    /**
     * Update confidence based on behavior
     */
    public void updateFromBehavior(Memory memory, boolean behaviorMatches) {
        confidenceService.updateFromBehavior(memory, behaviorMatches);
    }
    
    /**
     * Calculate current confidence with decay
     */
    public double calculateConfidence(Memory memory) {
        return confidenceService.calculateConfidence(memory);
    }
}
