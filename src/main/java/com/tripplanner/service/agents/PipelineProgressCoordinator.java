package com.tripplanner.service.agents;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Coordinates progress reporting across multiple agents in the pipeline.
 * 
 * Problem: When agents run in parallel, each emits 0-100% progress, causing
 * jittery UI updates as progress jumps between different agents' percentages.
 * 
 * Solution: Map each agent's progress to a weighted portion of overall pipeline progress,
 * with initial buffer and randomness to encourage users to wait for completion.
 * 
 * Pipeline stages and weights (with 10% initial buffer):
 * - Initial: 10-15% (immediate feedback)
 * - City Allocation: 15-35% (fast, simple)
 * - Skeleton Generation: 35-55% (critical, takes time)
 * - Node Population (parallel): 55-85% (multiple agents, most work)
 *   - Activity: 55-65%
 *   - Meal: 65-72%
 *   - Transport: 72-79%
 *   - Enrichment: 79-85%
 * - Cost Estimation: 85-93% (calculations)
 * - Finalization: 93-100% (validation, save)
 */
@Service
public class PipelineProgressCoordinator {
    
    private static final Logger logger = LoggerFactory.getLogger(PipelineProgressCoordinator.class);
    private static final Random random = new Random();
    
    // Progress ranges for each pipeline stage (with 10% initial buffer)
    private static final int INITIAL_START = 10;
    private static final int INITIAL_END = 15;
    
    private static final int CITY_ALLOCATION_START = 15;
    private static final int CITY_ALLOCATION_END = 35;
    
    private static final int SKELETON_START = 35;
    private static final int SKELETON_END = 55;
    
    private static final int ACTIVITY_START = 55;
    private static final int ACTIVITY_END = 65;
    
    private static final int MEAL_START = 65;
    private static final int MEAL_END = 72;
    
    private static final int TRANSPORT_START = 72;
    private static final int TRANSPORT_END = 79;
    
    private static final int ENRICHMENT_START = 79;
    private static final int ENRICHMENT_END = 85;
    
    private static final int COST_START = 85;
    private static final int COST_END = 93;
    
    private static final int FINALIZATION_START = 93;
    private static final int FINALIZATION_END = 100;
    
    // Track current progress for each itinerary
    private final Map<String, Integer> currentProgress = new ConcurrentHashMap<>();
    
    // Track if initial progress has been shown
    private final Map<String, Boolean> initialShown = new ConcurrentHashMap<>();
    
    /**
     * Calculate overall pipeline progress based on agent progress.
     * Adds initial buffer and randomness to encourage users to wait.
     * 
     * @param itineraryId The itinerary ID
     * @param agentKind The agent reporting progress
     * @param agentProgress The agent's progress (0-100)
     * @return Overall pipeline progress (10-100)
     */
    public int calculateOverallProgress(String itineraryId, String agentKind, int agentProgress) {
        // Show initial progress immediately (10-15% with randomness)
        if (!initialShown.getOrDefault(itineraryId, false)) {
            initialShown.put(itineraryId, true);
            int initialProgress = INITIAL_START + random.nextInt(INITIAL_END - INITIAL_START + 1);
            currentProgress.put(itineraryId, initialProgress);
            logger.info("Initial progress for {}: {}% (encouraging user to wait)", itineraryId, initialProgress);
            return initialProgress;
        }
        
        // Clamp agent progress to 0-100
        agentProgress = Math.max(0, Math.min(100, agentProgress));
        
        int overallProgress;
        int randomBoost = random.nextInt(3); // Add 0-2% randomness for natural feel
        
        switch (agentKind.toUpperCase()) {
            case "CITY_ALLOCATION":
                overallProgress = mapProgressWithRandomness(agentProgress, CITY_ALLOCATION_START, CITY_ALLOCATION_END, randomBoost);
                break;
                
            case "SKELETON":
            case "PLANNER":
                overallProgress = mapProgressWithRandomness(agentProgress, SKELETON_START, SKELETON_END, randomBoost);
                break;
                
            case "ACTIVITY":
                overallProgress = mapProgressWithRandomness(agentProgress, ACTIVITY_START, ACTIVITY_END, randomBoost);
                break;
                
            case "MEAL":
                overallProgress = mapProgressWithRandomness(agentProgress, MEAL_START, MEAL_END, randomBoost);
                break;
                
            case "TRANSPORT":
                overallProgress = mapProgressWithRandomness(agentProgress, TRANSPORT_START, TRANSPORT_END, randomBoost);
                break;
                
            case "ENRICHMENT":
                overallProgress = mapProgressWithRandomness(agentProgress, ENRICHMENT_START, ENRICHMENT_END, randomBoost);
                break;
                
            case "COST":
            case "COST_ESTIMATOR":
                overallProgress = mapProgressWithRandomness(agentProgress, COST_START, COST_END, randomBoost);
                break;
                
            case "FINALIZATION":
            case "VALIDATION":
                overallProgress = mapProgressWithRandomness(agentProgress, FINALIZATION_START, FINALIZATION_END, randomBoost);
                break;
                
            default:
                // Unknown agent - don't update progress
                logger.warn("Unknown agent kind for progress: {}", agentKind);
                return getCurrentProgress(itineraryId);
        }
        
        // Cap at 99% until explicitly marked complete (encourages waiting)
        overallProgress = Math.min(overallProgress, 99);
        
        // Only update if progress increases (prevent backwards movement)
        int current = getCurrentProgress(itineraryId);
        if (overallProgress > current) {
            currentProgress.put(itineraryId, overallProgress);
            logger.debug("Progress updated for {}: {} -> {} (agent: {}, agent progress: {}%)",
                itineraryId, current, overallProgress, agentKind, agentProgress);
            return overallProgress;
        }
        
        return current;
    }
    
    /**
     * Map agent progress (0-100) to overall pipeline range with randomness.
     */
    private int mapProgressWithRandomness(int agentProgress, int rangeStart, int rangeEnd, int randomBoost) {
        int rangeSize = rangeEnd - rangeStart;
        int baseProgress = rangeStart + (agentProgress * rangeSize / 100);
        return Math.min(baseProgress + randomBoost, rangeEnd);
    }
    
    /**
     * Get current overall progress for an itinerary.
     */
    public int getCurrentProgress(String itineraryId) {
        return currentProgress.getOrDefault(itineraryId, 0);
    }
    
    /**
     * Reset progress tracking for an itinerary (e.g., when starting new generation).
     */
    public void resetProgress(String itineraryId) {
        currentProgress.remove(itineraryId);
        initialShown.remove(itineraryId);
        logger.debug("Reset progress for itinerary: {}", itineraryId);
    }
    
    /**
     * Mark itinerary as complete (100%).
     * This is the only way to reach 100% - encourages users to wait.
     */
    public void markComplete(String itineraryId) {
        currentProgress.put(itineraryId, 100);
        initialShown.remove(itineraryId);
        logger.info("Marked itinerary complete: {} -> 100%", itineraryId);
    }
}
