package com.tripplanner.service.memory;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.tripplanner.dto.memory.Memory;
import com.tripplanner.dto.memory.MemoryQuery;
import com.tripplanner.dto.memory.UserProfile;
import com.tripplanner.enums.MemoryCategory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Service for managing user memories in Firestore
 * Storage path: itineraries/{itineraryId}/memory/{memoryId}
 */
@Service
public class MemoryService {
    
    private static final Logger logger = LoggerFactory.getLogger(MemoryService.class);
    
    private final Firestore firestore;
    private final MemoryConfidenceService confidenceService;
    
    public MemoryService(Firestore firestore, 
                        MemoryConfidenceService confidenceService) {
        this.firestore = firestore;
        this.confidenceService = confidenceService;
    }
    
    /**
     * Store memory asynchronously
     */
    public CompletableFuture<Memory> storeAsync(String itineraryId, Memory memory) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Set metadata
                if (memory.getId() == null || memory.getId().isEmpty()) {
                    memory.setId(UUID.randomUUID().toString());
                }
                memory.setItineraryId(itineraryId);
                memory.setCreatedAt(System.currentTimeMillis());
                memory.setLastUpdated(System.currentTimeMillis());
                
                // Mark personal data
                if (memory.getIsPersonal() == null) {
                    memory.setIsPersonal(isPersonalCategory(memory.getCategory()));
                }
                
                // Save to Firestore
                DocumentReference docRef = firestore
                    .collection("itineraries")
                    .document(itineraryId)
                    .collection("memory")
                    .document(memory.getId());
                docRef.set(memory).get();
                
                logger.info("Stored {} memory for itinerary {}", memory.getCategory(), itineraryId);
                return memory;
                
            } catch (Exception e) {
                logger.error("Failed to store memory: {}", e.getMessage(), e);
                throw new RuntimeException("Failed to store memory", e);
            }
        });
    }
    
    /**
     * Get all memories for itinerary
     */
    public List<Memory> getAll(String itineraryId) {
        try {
            QuerySnapshot snapshot = firestore
                .collection("itineraries")
                .document(itineraryId)
                .collection("memory")
                .get()
                .get();
            
            List<Memory> memories = new ArrayList<>();
            for (DocumentSnapshot doc : snapshot.getDocuments()) {
                Memory memory = doc.toObject(Memory.class);
                if (memory != null) {
                    memories.add(memory);
                }
            }
            
            logger.debug("Retrieved {} memories for itinerary {}", memories.size(), itineraryId);
            return memories;
            
        } catch (Exception e) {
            logger.error("Failed to get memories: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Get memories by category
     */
    public List<Memory> getByCategory(String itineraryId, MemoryCategory category) {
        return getAll(itineraryId).stream()
            .filter(m -> m.getCategory() == category)
            .collect(Collectors.toList());
    }
    
    /**
     * Query memories with filters
     */
    public List<Memory> query(String itineraryId, MemoryQuery query) {
        List<Memory> all = getAll(itineraryId);
        
        return all.stream()
            .filter(m -> query.getCategory() == null || m.getCategory() == query.getCategory())
            .filter(m -> query.getType() == null || m.getType() == query.getType())
            .filter(m -> query.getSource() == null || query.getSource().equals(m.getSource()))
            .filter(m -> query.getIsPersonal() == null || query.getIsPersonal().equals(m.getIsPersonal()))
            .filter(m -> query.getMinConfidence() == null || 
                confidenceService.calculateConfidence(m) >= query.getMinConfidence())
            .collect(Collectors.toList());
    }
    
    /**
     * Update memory asynchronously
     */
    public CompletableFuture<Memory> updateAsync(String itineraryId, String memoryId, Memory updates) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                DocumentReference docRef = firestore
                    .collection("itineraries")
                    .document(itineraryId)
                    .collection("memory")
                    .document(memoryId);
                DocumentSnapshot doc = docRef.get().get();
                
                if (!doc.exists()) {
                    throw new RuntimeException("Memory not found: " + memoryId);
                }
                
                Memory existing = doc.toObject(Memory.class);
                
                // Apply updates
                if (updates.getData() != null) {
                    existing.setData(updates.getData());
                }
                if (updates.getConfidence() != null) {
                    existing.setConfidence(updates.getConfidence());
                }
                if (updates.getIsPersonal() != null) {
                    existing.setIsPersonal(updates.getIsPersonal());
                }
                
                existing.setLastUpdated(System.currentTimeMillis());
                
                // Save
                docRef.set(existing).get();
                
                logger.info("Updated memory {} for itinerary {}", memoryId, itineraryId);
                return existing;
                
            } catch (Exception e) {
                logger.error("Failed to update memory: {}", e.getMessage(), e);
                throw new RuntimeException("Failed to update memory", e);
            }
        });
    }
    
    /**
     * Delete memory asynchronously
     */
    public CompletableFuture<Void> deleteAsync(String itineraryId, String memoryId) {
        return CompletableFuture.runAsync(() -> {
            try {
                firestore
                    .collection("itineraries")
                    .document(itineraryId)
                    .collection("memory")
                    .document(memoryId)
                    .delete()
                    .get();
                
                logger.info("Deleted memory {} for itinerary {}", memoryId, itineraryId);
                
            } catch (Exception e) {
                logger.error("Failed to delete memory: {}", e.getMessage(), e);
                throw new RuntimeException("Failed to delete memory", e);
            }
        });
    }
    
    /**
     * Get user profile (aggregated memories)
     */
    public UserProfile getProfile(String itineraryId) {
        List<Memory> all = getAll(itineraryId);
        
        return UserProfile.builder()
            .itineraryId(itineraryId)
            .activityPreferences(extractActivityPrefs(all))
            .budgetBehavior(extractBudgetBehavior(all))
            .timingPatterns(extractTimingPatterns(all))
            .mealPreferences(extractMealPrefs(all))
            .dietaryRestrictions(extractDietary(all))
            .transportPreferences(extractTransportPrefs(all))
            .validationPreferences(extractValidationPrefs(all))
            .build();
    }
    
    /**
     * Check if category contains personal data
     */
    private boolean isPersonalCategory(MemoryCategory category) {
        return category == MemoryCategory.DIETARY;
    }
    
    // Extraction methods for profile aggregation
    
    private Map<String, Object> extractActivityPrefs(List<Memory> memories) {
        return memories.stream()
            .filter(m -> m.getCategory() == MemoryCategory.ACTIVITY)
            .findFirst()
            .map(Memory::getData)
            .orElse(new HashMap<>());
    }
    
    private Map<String, Object> extractBudgetBehavior(List<Memory> memories) {
        return memories.stream()
            .filter(m -> m.getCategory() == MemoryCategory.BUDGET)
            .findFirst()
            .map(Memory::getData)
            .orElse(new HashMap<>());
    }
    
    private Map<String, Object> extractTimingPatterns(List<Memory> memories) {
        return memories.stream()
            .filter(m -> m.getCategory() == MemoryCategory.TIMING)
            .findFirst()
            .map(Memory::getData)
            .orElse(new HashMap<>());
    }
    
    private Map<String, Object> extractMealPrefs(List<Memory> memories) {
        return memories.stream()
            .filter(m -> m.getCategory() == MemoryCategory.MEAL)
            .findFirst()
            .map(Memory::getData)
            .orElse(new HashMap<>());
    }
    
    private List<String> extractDietary(List<Memory> memories) {
        return memories.stream()
            .filter(m -> m.getCategory() == MemoryCategory.DIETARY)
            .flatMap(m -> {
                Object restrictions = m.getData().get("restrictions");
                if (restrictions instanceof List) {
                    return ((List<?>) restrictions).stream()
                        .filter(r -> r instanceof String)
                        .map(r -> (String) r);
                }
                return java.util.stream.Stream.empty();
            })
            .collect(Collectors.toList());
    }
    
    private Map<String, Object> extractTransportPrefs(List<Memory> memories) {
        return memories.stream()
            .filter(m -> m.getCategory() == MemoryCategory.TRANSPORT)
            .findFirst()
            .map(Memory::getData)
            .orElse(new HashMap<>());
    }
    
    private Map<String, Object> extractValidationPrefs(List<Memory> memories) {
        return memories.stream()
            .filter(m -> m.getCategory() == MemoryCategory.VALIDATION)
            .findFirst()
            .map(Memory::getData)
            .orElse(new HashMap<>());
    }
    
    /**
     * Consolidate memories (remove duplicates, merge similar)
     */
    public void consolidateMemories(String itineraryId) {
        // TODO: Implement memory consolidation logic
        logger.debug("Memory consolidation not yet implemented");
    }
    
    /**
     * Cleanup expired memories
     */
    public void cleanupExpired(String itineraryId) {
        // TODO: Implement cleanup logic
        logger.debug("Memory cleanup not yet implemented");
    }
}
