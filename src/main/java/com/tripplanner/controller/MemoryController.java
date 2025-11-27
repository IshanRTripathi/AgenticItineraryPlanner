package com.tripplanner.controller;

import com.tripplanner.agents.MemoryAgent;
import com.tripplanner.dto.memory.Memory;
import com.tripplanner.dto.memory.MemoryQuery;
import com.tripplanner.dto.memory.UserProfile;
import com.tripplanner.enums.MemoryCategory;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * REST controller for memory operations
 * Path: /api/v1/itineraries/{itineraryId}/memory
 */
@RestController
@RequestMapping("/api/v1/itineraries/{itineraryId}/memory")
public class MemoryController {
    
    private static final Logger logger = LoggerFactory.getLogger(MemoryController.class);
    
    private final MemoryAgent memoryAgent;
    
    public MemoryController(MemoryAgent memoryAgent) {
        this.memoryAgent = memoryAgent;
    }
    
    /**
     * Get all memories for itinerary
     * GET /api/v1/itineraries/{itineraryId}/memory
     */
    @GetMapping
    public ResponseEntity<List<Memory>> getMemories(
            @PathVariable String itineraryId,
            @RequestParam(required = false) String category,
            HttpServletRequest httpRequest) {
        
        try {
            logger.info("Getting memories for itinerary: {}, category: {}", itineraryId, category);
            
            // Extract userId from request (set by FirebaseAuthConfig)
            String userId = (String) httpRequest.getAttribute("userId");
            if (userId == null) {
                logger.error("User ID not found in request");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            List<Memory> memories;
            if (category != null && !category.isEmpty()) {
                try {
                    MemoryCategory memoryCategory = MemoryCategory.valueOf(category.toUpperCase());
                    memories = memoryAgent.getByCategory(itineraryId, memoryCategory);
                } catch (IllegalArgumentException e) {
                    logger.error("Invalid category: {}", category);
                    return ResponseEntity.badRequest().build();
                }
            } else {
                memories = memoryAgent.getAll(itineraryId);
            }
            
            logger.info("Retrieved {} memories for itinerary: {}", memories.size(), itineraryId);
            return ResponseEntity.ok(memories);
            
        } catch (Exception e) {
            logger.error("Failed to get memories: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Create new memory
     * POST /api/v1/itineraries/{itineraryId}/memory
     */
    @PostMapping
    public ResponseEntity<Memory> createMemory(
            @PathVariable String itineraryId,
            @RequestBody Memory memory,
            HttpServletRequest httpRequest) {
        
        try {
            logger.info("Creating memory for itinerary: {}, category: {}", itineraryId, memory.getCategory());
            
            // Extract userId from request
            String userId = (String) httpRequest.getAttribute("userId");
            if (userId == null) {
                logger.error("User ID not found in request");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            // Set userId on memory
            memory.setUserId(userId);
            
            // Store memory asynchronously
            CompletableFuture<Memory> future = memoryAgent.store(itineraryId, memory);
            Memory created = future.get(); // Wait for completion
            
            logger.info("Created memory {} for itinerary: {}", created.getId(), itineraryId);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
            
        } catch (Exception e) {
            logger.error("Failed to create memory: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Update memory
     * PATCH /api/v1/itineraries/{itineraryId}/memory/{memoryId}
     */
    @PatchMapping("/{memoryId}")
    public ResponseEntity<Memory> updateMemory(
            @PathVariable String itineraryId,
            @PathVariable String memoryId,
            @RequestBody Map<String, Object> updates,
            HttpServletRequest httpRequest) {
        
        try {
            logger.info("Updating memory {} for itinerary: {}", memoryId, itineraryId);
            
            // Extract userId from request
            String userId = (String) httpRequest.getAttribute("userId");
            if (userId == null) {
                logger.error("User ID not found in request");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            // Create memory object with updates
            Memory memoryUpdates = new Memory();
            if (updates.containsKey("data")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) updates.get("data");
                memoryUpdates.setData(data);
            }
            if (updates.containsKey("confidence")) {
                memoryUpdates.setConfidence(((Number) updates.get("confidence")).doubleValue());
            }
            if (updates.containsKey("isPersonal")) {
                memoryUpdates.setIsPersonal((Boolean) updates.get("isPersonal"));
            }
            
            // Update memory asynchronously
            CompletableFuture<Memory> future = memoryAgent.update(itineraryId, memoryId, memoryUpdates);
            Memory updated = future.get(); // Wait for completion
            
            logger.info("Updated memory {} for itinerary: {}", memoryId, itineraryId);
            return ResponseEntity.ok(updated);
            
        } catch (Exception e) {
            logger.error("Failed to update memory: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Delete memory
     * DELETE /api/v1/itineraries/{itineraryId}/memory/{memoryId}
     */
    @DeleteMapping("/{memoryId}")
    public ResponseEntity<Void> deleteMemory(
            @PathVariable String itineraryId,
            @PathVariable String memoryId,
            HttpServletRequest httpRequest) {
        
        try {
            logger.info("Deleting memory {} for itinerary: {}", memoryId, itineraryId);
            
            // Extract userId from request
            String userId = (String) httpRequest.getAttribute("userId");
            if (userId == null) {
                logger.error("User ID not found in request");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            // Delete memory asynchronously
            CompletableFuture<Void> future = memoryAgent.delete(itineraryId, memoryId);
            future.get(); // Wait for completion
            
            logger.info("Deleted memory {} for itinerary: {}", memoryId, itineraryId);
            return ResponseEntity.noContent().build();
            
        } catch (Exception e) {
            logger.error("Failed to delete memory: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get aggregated user profile
     * GET /api/v1/itineraries/{itineraryId}/memory/profile
     */
    @GetMapping("/profile")
    public ResponseEntity<UserProfile> getProfile(
            @PathVariable String itineraryId,
            HttpServletRequest httpRequest) {
        
        try {
            logger.info("Getting user profile for itinerary: {}", itineraryId);
            
            // Extract userId from request
            String userId = (String) httpRequest.getAttribute("userId");
            if (userId == null) {
                logger.error("User ID not found in request");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            UserProfile profile = memoryAgent.getProfile(itineraryId);
            profile.setUserId(userId);
            
            logger.info("Retrieved user profile for itinerary: {}", itineraryId);
            return ResponseEntity.ok(profile);
            
        } catch (Exception e) {
            logger.error("Failed to get user profile: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
