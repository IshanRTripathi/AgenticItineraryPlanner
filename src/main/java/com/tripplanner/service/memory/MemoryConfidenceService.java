package com.tripplanner.service.memory;

import com.tripplanner.dto.memory.Memory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service for managing memory confidence scores with time decay
 */
@Service
public class MemoryConfidenceService {
    
    private static final Logger logger = LoggerFactory.getLogger(MemoryConfidenceService.class);
    
    // Decay constant: 6 months half-life
    private static final long HALF_LIFE_MS = 180L * 24 * 60 * 60 * 1000; // 6 months
    
    /**
     * Calculate confidence with time decay
     */
    public double calculateConfidence(Memory memory) {
        if (memory.getNeverExpires() != null && memory.getNeverExpires()) {
            return memory.getConfidence() != null ? memory.getConfidence() : 1.0;
        }
        
        double baseConfidence = memory.getConfidence() != null ? memory.getConfidence() : 0.5;
        long age = System.currentTimeMillis() - memory.getLastUpdated();
        
        // Exponential decay: confidence = base * 0.5^(age/halfLife)
        double decayFactor = Math.pow(0.5, (double) age / HALF_LIFE_MS);
        double currentConfidence = baseConfidence * decayFactor;
        
        // Floor at 0.1 (never completely lose confidence)
        return Math.max(0.1, currentConfidence);
    }
    
    /**
     * Boost confidence when memory is used/confirmed
     */
    public void boostConfidence(Memory memory) {
        double current = memory.getConfidence() != null ? memory.getConfidence() : 0.5;
        double boosted = Math.min(1.0, current + 0.1); // +10%, max 1.0
        
        memory.setConfidence(boosted);
        memory.setLastUsed(System.currentTimeMillis());
        memory.setLastUpdated(System.currentTimeMillis());
        
        logger.debug("Boosted confidence for memory {} from {} to {}", 
            memory.getId(), current, boosted);
    }
    
    /**
     * Update confidence based on new behavior
     */
    public void updateFromBehavior(Memory memory, boolean behaviorMatches) {
        if (behaviorMatches) {
            // Behavior confirms memory
            boostConfidence(memory);
        } else {
            // Behavior contradicts memory
            double current = memory.getConfidence() != null ? memory.getConfidence() : 0.5;
            double reduced = Math.max(0.1, current - 0.2); // -20%
            memory.setConfidence(reduced);
            memory.setLastUpdated(System.currentTimeMillis());
            
            logger.debug("Reduced confidence for memory {} from {} to {} (behavior mismatch)", 
                memory.getId(), current, reduced);
        }
    }
    
    /**
     * Apply recency bias to memory
     */
    public double applyRecencyBias(Memory memory) {
        return calculateConfidence(memory);
    }
}
