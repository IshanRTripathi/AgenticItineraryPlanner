package com.tripplanner.controller;

import com.tripplanner.dto.*;
import com.tripplanner.service.ChangeEngine;
import com.tripplanner.service.ItineraryJsonService;
import com.tripplanner.service.ItineraryService;
import com.tripplanner.service.UserDataService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * REST controller for itinerary operations.
 */
@RestController
@RequestMapping("/api/v1/itineraries")
public class ItinerariesController {
    
    private static final Logger logger = LoggerFactory.getLogger(ItinerariesController.class);
    
    private final ItineraryService itineraryService;
    private final ItineraryJsonService itineraryJsonService;
    private final ChangeEngine changeEngine;
    private final UserDataService userDataService;
    
    // SSE emitters for real-time updates
    private final Map<String, SseEmitter> sseEmitters = new ConcurrentHashMap<>();
    
    public ItinerariesController(ItineraryService itineraryService, 
                               ItineraryJsonService itineraryJsonService,
                               ChangeEngine changeEngine,
                               UserDataService userDataService) {
        this.itineraryService = itineraryService;
        this.itineraryJsonService = itineraryJsonService;
        this.changeEngine = changeEngine;
        this.userDataService = userDataService;
    }
    
    /**
     * Create a new itinerary.
     */
    @PostMapping
    public ResponseEntity<ItineraryDto> create(@Valid @RequestBody CreateItineraryReq request, 
                                             HttpServletRequest httpRequest) {
        logger.info("Creating itinerary");
        logger.info("Request: {}", request);
        
        try {
            // Extract userId from request attributes (set by FirebaseAuthConfig)
            String userId = (String) httpRequest.getAttribute("userId");
            if (userId == null) {
                logger.error("User ID not found in request");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            ItineraryDto itinerary = itineraryService.create(request, userId);
            
            logger.info("Itinerary created successfully: {} for user: {}", itinerary.getId(), userId);
            return ResponseEntity.ok(itinerary);
            
        } catch (Exception e) {
            logger.error("Failed to create itinerary", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get all itineraries for the authenticated user.
     */
    @GetMapping
    public ResponseEntity<List<ItineraryDto>> getAll(HttpServletRequest httpRequest) {
        logger.info("Getting all itineraries");
        
        try {
            // Extract userId from request attributes (set by FirebaseAuthConfig)
            String userId = (String) httpRequest.getAttribute("userId");
            if (userId == null) {
                logger.error("User ID not found in request");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            List<ItineraryDto> itineraries = itineraryService.getUserItineraries(userId, 0, 10);
            
            logger.info("Found {} itineraries for user: {}", itineraries.size(), userId);
            return ResponseEntity.ok(itineraries);
            
        } catch (Exception e) {
            logger.error("Failed to get itineraries", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get itinerary by ID for the authenticated user.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ItineraryDto> getById(@PathVariable String id, HttpServletRequest httpRequest) {
        logger.info("Getting itinerary: {}", id);
        
        // Extract userId from request attributes (set by FirebaseAuthConfig)
        String userId = (String) httpRequest.getAttribute("userId");
        if (userId == null) {
            logger.error("User ID not found in request");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        try {
            ItineraryDto itinerary = itineraryService.get(id, userId);
            logger.info("Itinerary found: {} for user: {}", itinerary.getId(), userId);
            return ResponseEntity.ok(itinerary);
        } catch (org.springframework.web.server.ResponseStatusException rse) {
            if (rse.getStatusCode().value() == 404) {
                logger.warn("Itinerary not found: {} for user: {}", id, userId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            logger.error("Failed to get itinerary (RSE): {} for user: {}", id, userId, rse);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (Exception e) {
            logger.error("Failed to get itinerary: {} for user: {}", id, userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Delete itinerary for the authenticated user.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id, HttpServletRequest httpRequest) {
        logger.info("Deleting itinerary: {}", id);
        
        // Extract userId from request attributes (set by FirebaseAuthConfig)
        String userId = (String) httpRequest.getAttribute("userId");
        if (userId == null) {
            logger.error("User ID not found in request");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        try {
            itineraryService.delete(id, userId);
            
            logger.info("Itinerary deleted successfully: {} for user: {}", id, userId);
            return ResponseEntity.noContent().build();
            
        } catch (Exception e) {
            logger.error("Failed to delete itinerary: {} for user: {}", id, userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    // ===== NEW MVP CONTRACT ENDPOINTS =====
    
    /**
     * Get itinerary by ID (returns master JSON).
     * GET /itineraries/{id} → 200 → returns master JSON
     * This endpoint is public (no authentication required) for backward compatibility
     */
    @GetMapping("/{id}/json")
    public ResponseEntity<NormalizedItinerary> getItineraryJson(@PathVariable String id, HttpServletRequest httpRequest) {
        logger.info("Getting normalized itinerary JSON: {}", id);
        
        // Extract userId from request attributes (set by FirebaseAuthConfig) - optional
        String userId = (String) httpRequest.getAttribute("userId");
        
        try {
            // If user is authenticated, try user-specific storage first
            if (userId != null) {
                logger.info("User authenticated, checking user-specific storage: {}", userId);
                var itinerary = userDataService.getUserItinerary(userId, id);
                
                if (itinerary.isPresent()) {
                    logger.info("Normalized itinerary found in user storage: {}", id);
                    return ResponseEntity.ok(itinerary.get());
                } else {
                    logger.info("Not found in user storage, trying legacy storage: {}", id);
                }
            } else {
                logger.info("No user authentication, checking legacy storage: {}", id);
            }
            
            // Fallback to legacy storage system
            var legacyItinerary = itineraryJsonService.getItinerary(id);
            
            if (legacyItinerary.isPresent()) {
                logger.info("Normalized itinerary found in legacy storage: {}", id);
                return ResponseEntity.ok(legacyItinerary.get());
            } else {
                logger.warn("Normalized itinerary not found: {}", id);
                return ResponseEntity.notFound().build();
            }
            
        } catch (Exception e) {
            logger.error("Failed to get normalized itinerary: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Propose changes (preview without writing to DB).
     * POST /itineraries/{id}:propose → 200 → body: ChangeSet → {proposed, diff, previewVersion}
     */
    @PostMapping("/{id}:propose")
    public ResponseEntity<ProposeResponse> proposeChanges(@PathVariable String id, 
                                                         @Valid @RequestBody ChangeSet changeSet) {
        logger.info("Proposing changes for itinerary: {}", id);
        
        try {
            ChangeEngine.ProposeResult result = changeEngine.propose(id, changeSet);
            
            ProposeResponse response = new ProposeResponse(
                result.getProposed(),
                result.getDiff(),
                result.getPreviewVersion()
            );
            
            logger.info("Changes proposed successfully for itinerary: {}", id);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to propose changes for itinerary: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Apply changes (writes to DB and increments version).
     * POST /itineraries/{id}:apply → 200 → body: {changeSetId | changeSet} → {toVersion, diff}
     */
    @PostMapping("/{id}:apply")
    public ResponseEntity<ApplyResponse> applyChanges(@PathVariable String id, 
                                                     @Valid @RequestBody ApplyRequest request) {
        logger.info("Applying changes for itinerary: {}", id);
        
        try {
            ChangeSet changeSet = request.getChangeSet();
            if (changeSet == null) {
                return ResponseEntity.badRequest().build();
            }
            
            ChangeEngine.ApplyResult result = changeEngine.apply(id, changeSet);
            
            ApplyResponse response = new ApplyResponse(
                result.getToVersion(),
                result.getDiff()
            );
            
            // Send SSE event
            sendPatchEvent(id, result.getToVersion() - 1, result.getToVersion(), result.getDiff(), "user");
            
            logger.info("Changes applied successfully for itinerary: {}", id);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to apply changes for itinerary: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Undo changes (restores from revision).
     * POST /itineraries/{id}:undo → 200 → body: {toVersion?} → {toVersion, diff}
     */
    @PostMapping("/{id}:undo")
    public ResponseEntity<UndoResponse> undoChanges(@PathVariable String id, 
                                                   @Valid @RequestBody UndoRequest request) {
        logger.info("Undoing changes for itinerary: {} to version: {}", id, request.getToVersion());
        
        try {
            ChangeEngine.UndoResult result = changeEngine.undo(id, request.getToVersion());
            
            UndoResponse response = new UndoResponse(
                result.getToVersion(),
                result.getDiff()
            );
            
            // Send SSE event
            sendPatchEvent(id, request.getToVersion() + 1, result.getToVersion(), result.getDiff(), "user");
            
            logger.info("Changes undone successfully for itinerary: {}", id);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to undo changes for itinerary: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * SSE endpoint for real-time updates.
     * GET /itineraries/patches?itineraryId=… → 200 text/event-stream → emits PatchEvent
     */
    @GetMapping(value = "/patches", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter getPatches(@RequestParam String itineraryId) {
        logger.info("Opening SSE connection for itinerary: {}", itineraryId);
        
        SseEmitter emitter = new SseEmitter(30000L); // 30 second timeout
        sseEmitters.put(itineraryId, emitter);
        
        emitter.onCompletion(() -> {
            logger.info("SSE connection completed for itinerary: {}", itineraryId);
            sseEmitters.remove(itineraryId);
        });
        
        emitter.onTimeout(() -> {
            logger.info("SSE connection timeout for itinerary: {}", itineraryId);
            sseEmitters.remove(itineraryId);
        });
        
        emitter.onError((ex) -> {
            logger.error("SSE connection error for itinerary: {}", itineraryId, ex);
            sseEmitters.remove(itineraryId);
        });
        
        return emitter;
    }
    
    /**
     * Send patch event to SSE subscribers.
     */
    private void sendPatchEvent(String itineraryId, Integer fromVersion, Integer toVersion,
                                ItineraryDiff diff, String updatedBy) {
        try {
            PatchEvent patchEvent = new PatchEvent(
                itineraryId, fromVersion, toVersion, diff, 
                "Changes applied", updatedBy
            );
            
            SseEmitter emitter = sseEmitters.get(itineraryId);
            if (emitter != null) {
                emitter.send(patchEvent);
                logger.info("Patch event sent for itinerary: {}", itineraryId);
            }
        } catch (Exception e) {
            logger.error("Failed to send patch event for itinerary: {}", itineraryId, e);
        }
    }
    
    // Response DTOs
    public static class ProposeResponse {
        private final NormalizedItinerary proposed;
        private final ItineraryDiff diff;
        private final Integer previewVersion;
        
        public ProposeResponse(NormalizedItinerary proposed, ItineraryDiff diff, Integer previewVersion) {
            this.proposed = proposed;
            this.diff = diff;
            this.previewVersion = previewVersion;
        }
        
        public NormalizedItinerary getProposed() { return proposed; }
        public ItineraryDiff getDiff() { return diff; }
        public Integer getPreviewVersion() { return previewVersion; }
    }
    
    public static class ApplyRequest {
        private String changeSetId;
        private ChangeSet changeSet;
        
        public String getChangeSetId() { return changeSetId; }
        public void setChangeSetId(String changeSetId) { this.changeSetId = changeSetId; }
        public ChangeSet getChangeSet() { return changeSet; }
        public void setChangeSet(ChangeSet changeSet) { this.changeSet = changeSet; }
    }
    
    public static class ApplyResponse {
        private final Integer toVersion;
        private final ItineraryDiff diff;
        
        public ApplyResponse(Integer toVersion, ItineraryDiff diff) {
            this.toVersion = toVersion;
            this.diff = diff;
        }
        
        public Integer getToVersion() { return toVersion; }
        public ItineraryDiff getDiff() { return diff; }
    }
    
    public static class UndoRequest {
        private Integer toVersion;
        
        public Integer getToVersion() { return toVersion; }
        public void setToVersion(Integer toVersion) { this.toVersion = toVersion; }
    }
    
    public static class UndoResponse {
        private final Integer toVersion;
        private final ItineraryDiff diff;
        
        public UndoResponse(Integer toVersion, ItineraryDiff diff) {
            this.toVersion = toVersion;
            this.diff = diff;
        }
        
        public Integer getToVersion() { return toVersion; }
        public ItineraryDiff getDiff() { return diff; }
    }
}
