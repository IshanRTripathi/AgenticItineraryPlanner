package com.tripplanner.controller;

import com.tripplanner.dto.*;
import com.tripplanner.service.ChangeEngine;
import com.tripplanner.service.ItineraryJsonService;
import com.tripplanner.service.ItineraryService;
import com.tripplanner.service.UserDataService;
import com.tripplanner.service.AgentRegistry;
import com.tripplanner.service.RevisionService;
import com.tripplanner.service.OrchestratorService;
import com.tripplanner.service.WebSocketBroadcastService;
import com.tripplanner.service.ChatHistoryService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.UUID;

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
    private final AgentRegistry agentRegistry;
    private final RevisionService revisionService;
    private final OrchestratorService orchestratorService;
    private final WebSocketBroadcastService webSocketBroadcastService;
    private final ChatHistoryService chatHistoryService;
    
    // Real-time updates managed by WebSocket
    
    public ItinerariesController(ItineraryService itineraryService, 
                               ItineraryJsonService itineraryJsonService,
                               ChangeEngine changeEngine,
                               UserDataService userDataService,
                               AgentRegistry agentRegistry,
                               RevisionService revisionService,
                               OrchestratorService orchestratorService,
                               WebSocketBroadcastService webSocketBroadcastService,
                               ChatHistoryService chatHistoryService) {
        this.itineraryService = itineraryService;
        this.itineraryJsonService = itineraryJsonService;
        this.changeEngine = changeEngine;
        this.userDataService = userDataService;
        this.agentRegistry = agentRegistry;
        this.revisionService = revisionService;
        this.orchestratorService = orchestratorService;
        this.webSocketBroadcastService = webSocketBroadcastService;
        this.chatHistoryService = chatHistoryService;
    }
    
    /**
     * Create a new itinerary with immediate response and real-time updates.
     */
    @PostMapping
    public ResponseEntity<ItineraryCreationResponse> create(@Valid @RequestBody CreateItineraryReq request, 
                                                          HttpServletRequest httpRequest) {
        logger.info("Creating itinerary with real-time updates");
        logger.info("Request: {}", request);
        
        try {
            // Extract userId from request attributes (set by FirebaseAuthConfig)
            String userId = (String) httpRequest.getAttribute("userId");
            if (userId == null) {
                logger.warn("User ID not found in request, using anonymous for development");
                userId = "anonymous"; // Temporary fallback for development
            }
            
            // Validate required fields
            if (request.getDestination() == null || request.getDestination().isBlank()) {
                logger.error("Destination is required");
                return ResponseEntity.badRequest()
                    .body(ItineraryCreationResponse.error("Destination is required"));
            }
            if (request.getStartDate() == null) {
                logger.error("Start date is required");
                return ResponseEntity.badRequest()
                    .body(ItineraryCreationResponse.error("Start date is required"));
            }
            
            // Create initial itinerary structure (this is synchronous)
            ItineraryDto initialItinerary = itineraryService.create(request, userId);
            
            // Generate unique execution ID for this creation process
            String executionId = "exec_" + UUID.randomUUID().toString();
            
            // Calculate estimated completion time based on duration
            LocalDateTime estimatedCompletion = calculateEstimatedCompletion(request);
            
            // Create execution stages for progress tracking
            List<AgentExecutionStage> stages = buildExecutionStages(request);
            
            // Build enhanced response (WebSocket handles real-time updates)
            ItineraryCreationResponse response = ItineraryCreationResponse.builder()
                .itinerary(initialItinerary)
                .executionId(executionId)
                .estimatedCompletion(estimatedCompletion)
                .status(CreationStatus.PROCESSING)
                .stages(stages)
                .build();
            
            logger.info("Enhanced itinerary creation response prepared: {} for user: {}", 
                       initialItinerary.getId(), userId);
            logger.info("Execution ID: {}", executionId);
            logger.info("Real-time updates will be sent via WebSocket");
            logger.info("Estimated completion: {}", estimatedCompletion);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to create itinerary", e);
            return ResponseEntity.ok(ItineraryCreationResponse.error(e.getMessage()));
        }
    }
    
    /**
     * Calculate estimated completion time based on request complexity.
     */
    private LocalDateTime calculateEstimatedCompletion(CreateItineraryReq request) {
        // Base time: 30 seconds
        int baseSeconds = 30;
        
        // Add time based on duration (5 seconds per day)
        int durationSeconds = request.getDurationDays() * 5;
        
        // Add time based on interests (2 seconds per interest)
        int interestSeconds = request.getInterests() != null ? request.getInterests().size() * 2 : 0;
        
        // Add time based on constraints (3 seconds per constraint)
        int constraintSeconds = request.getConstraints() != null ? request.getConstraints().size() * 3 : 0;
        
        int totalSeconds = baseSeconds + durationSeconds + interestSeconds + constraintSeconds;
        
        return LocalDateTime.now().plusSeconds(totalSeconds);
    }
    
    /**
     * Build execution stages for progress tracking.
     */
    private List<AgentExecutionStage> buildExecutionStages(CreateItineraryReq request) {
        List<AgentExecutionStage> stages = new ArrayList<>();
        
        // Planning stage
        stages.add(new AgentExecutionStage(
            "Planning", 
            "PLANNER",
            "Creating day-by-day itinerary",
            (long) (request.getDurationDays() * 3000) // 3 seconds per day
        ));
        
        // Enrichment stage
        stages.add(new AgentExecutionStage(
            "Enrichment", 
            "ENRICHMENT",
            "Adding location details and photos",
            15000L // 15 seconds
        ));
        
        // Places validation stage
        stages.add(new AgentExecutionStage(
            "Places Validation", 
            "places", 
            "Validating locations and adding details",
            10000L // 10 seconds
        ));
        
        // Final optimization stage
        stages.add(new AgentExecutionStage(
            "Optimization", 
            "orchestrator", 
            "Final optimization and cleanup",
            5000L // 5 seconds
        ));
        
        return stages;
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
                logger.warn("User ID not found in request, using anonymous for development");
                userId = "anonymous"; // Temporary fallback for development
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
            logger.warn("User ID not found in request, using anonymous for development");
            userId = "anonymous"; // Temporary fallback for development
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
            logger.warn("User ID not found in request, using anonymous for development");
            userId = "anonymous"; // Temporary fallback for development
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
     * This endpoint is public (no authentication required)
     */
    @GetMapping("/{id}/json")
    public ResponseEntity<NormalizedItinerary> getItineraryJson(@PathVariable String id, HttpServletRequest httpRequest) {
        logger.info("Getting normalized itinerary JSON: {}", id);
        
        // Extract userId from request attributes (set by FirebaseAuthConfig) - optional
        String userId = (String) httpRequest.getAttribute("userId");
        
        try {
            // If user is authenticated, check if they own this trip
            if (userId != null) {
                logger.info("User authenticated, checking ownership: {}", userId);
                if (!userDataService.userOwnsTrip(userId, id)) {
                    logger.warn("User {} does not own itinerary: {}", userId, id);
                    return ResponseEntity.notFound().build();
                }
            }
            
            // Get itinerary from ItineraryJsonService (single source of truth)
            var itinerary = itineraryJsonService.getItinerary(id);
            
            if (itinerary.isPresent()) {
                NormalizedItinerary normalizedItinerary = itinerary.get();
                
                // Calculate and set status based on current state
                String status = itineraryService.calculateItineraryStatus(normalizedItinerary);
                normalizedItinerary.setStatus(status);
                
                logger.info("Normalized itinerary found: {}, status: {}", id, status);
                return ResponseEntity.ok(normalizedItinerary);
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
            
            // WebSocket handles real-time updates automatically
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
            
            // WebSocket handles real-time updates automatically
            logger.info("Changes undone successfully for itinerary: {}", id);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to undo changes for itinerary: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    // SSE endpoints removed - WebSocket handles all real-time communication
    
    /**
     * Lock or unlock a specific node in an itinerary.
     * PUT /itineraries/{id}/nodes/{nodeId}/lock
     */
    @PutMapping("/{id}/nodes/{nodeId}/lock")
    public ResponseEntity<Map<String, Object>> toggleNodeLock(@PathVariable String id, 
                                                             @PathVariable String nodeId,
                                                             @RequestBody Map<String, Boolean> request,
                                                             HttpServletRequest httpRequest) {
        logger.info("Toggling lock for node {} in itinerary: {}", nodeId, id);
        
        // Get user ID from request (temporarily allow anonymous for testing)
        String userId = (String) httpRequest.getAttribute("userId");
        if (userId == null) {
            logger.warn("User ID not found in request, using anonymous for testing");
            userId = "anonymous"; // Temporary fallback for testing
        }
        
        try {
            // Get the locked state from request body
            Boolean locked = request.get("locked");
            if (locked == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Missing 'locked' field in request body"));
            }
            
            // Get current itinerary
            Optional<NormalizedItinerary> itineraryOpt = itineraryJsonService.getItinerary(id);
            if (itineraryOpt.isEmpty()) {
                logger.warn("Itinerary not found: {}", id);
                return ResponseEntity.notFound().build();
            }
            
            NormalizedItinerary itinerary = itineraryOpt.get();
            
            // Check ownership (allow access to anonymous itineraries for testing)
            String itineraryUserId = itinerary.getUserId();
            boolean isAnonymousItinerary = "anonymous".equals(itineraryUserId) || itineraryUserId == null;
            boolean hasAccess = userId.equals(itineraryUserId) || isAnonymousItinerary;
            
            if (!hasAccess) {
                logger.warn("User {} does not have access to itinerary {} (owner: {})", userId, id, itineraryUserId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            // If accessing anonymous itinerary, optionally transfer ownership
            if (isAnonymousItinerary && !"anonymous".equals(userId)) {
                logger.info("Transferring ownership of itinerary {} from '{}' to '{}'", id, itineraryUserId, userId);
                itinerary.setUserId(userId);
            }
            
            // Find and update the node
            boolean nodeFound = false;
            for (NormalizedDay day : itinerary.getDays()) {
                for (NormalizedNode node : day.getNodes()) {
                    if (nodeId.equals(node.getId())) {
                        node.setLocked(locked);
                        node.markAsUpdated("user");
                        nodeFound = true;
                        break;
                    }
                }
                if (nodeFound) break;
            }
            
            if (!nodeFound) {
                logger.warn("Node not found: {} in itinerary: {}", nodeId, id);
                return ResponseEntity.notFound().build();
            }
            
            // Save the updated itinerary
            itinerary.setUpdatedAt(System.currentTimeMillis());
            itineraryJsonService.saveMasterItinerary(id, itinerary);
            
            logger.info("Node {} lock status updated to {} in itinerary: {}", nodeId, locked, id);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "nodeId", nodeId,
                "locked", locked,
                "message", locked ? "Node locked successfully" : "Node unlocked successfully"
            ));
            
        } catch (Exception e) {
            logger.error("Failed to toggle lock for node {} in itinerary: {}", nodeId, id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update node lock status"));
        }
    }
    
    /**
     * Get lock states of all nodes in an itinerary for debugging.
     * GET /itineraries/{id}/lock-states
     */
    @GetMapping("/{id}/lock-states")
    public ResponseEntity<Map<String, Object>> getLockStates(@PathVariable String id, HttpServletRequest httpRequest) {
        logger.info("Getting lock states for itinerary: {}", id);
        
        try {
            // Get current itinerary
            Optional<NormalizedItinerary> itineraryOpt = itineraryJsonService.getItinerary(id);
            if (itineraryOpt.isEmpty()) {
                logger.warn("Itinerary not found: {}", id);
                return ResponseEntity.notFound().build();
            }
            
            NormalizedItinerary itinerary = itineraryOpt.get();
            Map<String, Object> lockStates = new HashMap<>();
            
            // Collect lock states for all nodes
            for (int dayIndex = 0; dayIndex < itinerary.getDays().size(); dayIndex++) {
                NormalizedDay day = itinerary.getDays().get(dayIndex);
                for (NormalizedNode node : day.getNodes()) {
                    Map<String, Object> nodeInfo = new HashMap<>();
                    nodeInfo.put("title", node.getTitle());
                    nodeInfo.put("locked", node.getLocked());
                    nodeInfo.put("day", dayIndex + 1);
                    lockStates.put(node.getId(), nodeInfo);
                }
            }
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "itineraryId", id,
                "lockStates", lockStates
            ));
            
        } catch (Exception e) {
            logger.error("Failed to get lock states for itinerary: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get lock states"));
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
    
    // ===== AGENT EXECUTION ENDPOINTS =====
    
    /**
     * Execute an agent on an itinerary
     */
    @PostMapping("/{id}/agents/{agentType}/execute")
    public ResponseEntity<Map<String, Object>> executeAgent(
            @PathVariable String id,
            @PathVariable String agentType,
            @RequestBody Map<String, Object> parameters,
            HttpServletRequest request) {
        try {
            String userId = (String) request.getAttribute("userId");
            if (userId == null) {
                logger.warn("User ID not found in request, using anonymous for development");
                userId = "anonymous"; // Temporary fallback for development
            }
            
            // Get the itinerary
            Optional<NormalizedItinerary> itineraryOpt = itineraryJsonService.getItinerary(id);
            if (itineraryOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            NormalizedItinerary itinerary = itineraryOpt.get();
            
            // Execute agent via registry
            Map<String, Object> result = agentRegistry.executeAgent(agentType, itinerary, parameters);
            
            // Broadcast update if changes were made
            if (result.containsKey("changes") && (Boolean) result.getOrDefault("applied", false)) {
                webSocketBroadcastService.broadcastUpdate(id, "agent_execution", result, userId);
            }
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error executing agent {} for itinerary {}: {}", agentType, id, e.getMessage());
            Map<String, Object> error = Map.of(
                "error", "Agent execution failed",
                "message", e.getMessage()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    /**
     * Get agent execution status
     */
    @GetMapping("/{id}/agents/{agentType}/status")
    public ResponseEntity<Map<String, Object>> getAgentStatus(
            @PathVariable String id,
            @PathVariable String agentType,
            HttpServletRequest request) {
        try {
            String userId = (String) request.getAttribute("userId");
            if (userId == null) {
                logger.warn("User ID not found in request, using anonymous for development");
                userId = "anonymous"; // Temporary fallback for development
            }
            
            // Get agent status from registry
            Map<String, Object> status = agentRegistry.getAgentStatus(agentType, id);
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            logger.error("Error getting agent status for {} on itinerary {}: {}", agentType, id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Cancel agent execution
     */
    @PostMapping("/{id}/agents/{agentType}/cancel")
    public ResponseEntity<Void> cancelAgentExecution(
            @PathVariable String id,
            @PathVariable String agentType,
            HttpServletRequest request) {
        try {
            String userId = (String) request.getAttribute("userId");
            if (userId == null) {
                logger.warn("User ID not found in request, using anonymous for development");
                userId = "anonymous"; // Temporary fallback for development
            }
            
            // Cancel agent execution
            agentRegistry.cancelAgentExecution(agentType, id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("Error canceling agent {} for itinerary {}: {}", agentType, id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    // ===== REVISION MANAGEMENT ENDPOINTS =====
    
    /**
     * Get revision history for an itinerary
     */
    @GetMapping("/{id}/revisions")
    public ResponseEntity<List<RevisionRecord>> getRevisions(
            @PathVariable String id,
            HttpServletRequest request) {
        try {
            String userId = (String) request.getAttribute("userId");
            if (userId == null) {
                logger.warn("User ID not found in request, using anonymous for development");
                userId = "anonymous"; // Temporary fallback for development
            }
            
            List<RevisionRecord> revisions = revisionService.getRevisionHistory(id);
            return ResponseEntity.ok(revisions);
        } catch (Exception e) {
            logger.error("Error getting revisions for itinerary {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Rollback to a specific revision
     */
    @PostMapping("/{id}/revisions/{revisionId}/rollback")
    public ResponseEntity<NormalizedItinerary> rollbackToRevision(
            @PathVariable String id,
            @PathVariable String revisionId,
            HttpServletRequest request) {
        try {
            String userId = (String) request.getAttribute("userId");
            if (userId == null) {
                logger.warn("User ID not found in request, using anonymous for development");
                userId = "anonymous"; // Temporary fallback for development
            }
            
            // Rollback to revision
            NormalizedItinerary rolledBack = revisionService.rollbackToVersion(id, revisionId);
            
            // Save the rolled back version
            itineraryJsonService.saveMasterItinerary(id, rolledBack);
            
            // Broadcast update
            webSocketBroadcastService.broadcastUpdate(id, "rollback", Map.of(
                "revisionId", revisionId,
                "itinerary", rolledBack
            ), userId);
            
            return ResponseEntity.ok(rolledBack);
        } catch (Exception e) {
            logger.error("Error rolling back itinerary {} to revision {}: {}", id, revisionId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get a specific revision
     */
    @GetMapping("/{id}/revisions/{revisionId}")
    public ResponseEntity<RevisionRecord> getRevision(
            @PathVariable String id,
            @PathVariable String revisionId,
            HttpServletRequest request) {
        try {
            String userId = (String) request.getAttribute("userId");
            if (userId == null) {
                logger.warn("User ID not found in request, using anonymous for development");
                userId = "anonymous"; // Temporary fallback for development
            }
            
            Optional<RevisionRecord> revisionOpt = revisionService.getRevision(id, revisionId);
            if (revisionOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            return ResponseEntity.ok(revisionOpt.get());
        } catch (Exception e) {
            logger.error("Error getting revision {} for itinerary {}: {}", revisionId, id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    // ===== CHAT INTEGRATION ENDPOINTS =====
    
    /**
     * Send a chat message and process with orchestrator
     */
    @PostMapping("/{id}/chat")
    public ResponseEntity<ChatResponse> sendChatMessage(
            @PathVariable String id,
            @RequestBody ChatRequest request,
            HttpServletRequest httpRequest) {
        try {
            String userId = (String) httpRequest.getAttribute("userId");
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            // Set the itinerary ID and user ID
            request.setItineraryId(id);
            request.setUserId(userId);
            
            // Process chat request with orchestrator
            ChatResponse response = orchestratorService.route(request);
            
            // Broadcast chat response if needed
            if (response.getChangeSet() != null && response.isApplied()) {
                webSocketBroadcastService.broadcastUpdate(id, "chat_update", Map.of(
                    "chatResponse", response,
                    "changes", response.getChangeSet()
                ), userId);
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error processing chat message for itinerary {}: {}", id, e.getMessage());
            ChatResponse errorResponse = new ChatResponse();
            errorResponse.setMessage("I'm sorry, I encountered an error processing your request. Please try again.");
            errorResponse.setErrors(List.of(e.getMessage()));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Get chat history for an itinerary
     */
    @GetMapping("/{id}/chat/history")
    public ResponseEntity<List<Map<String, Object>>> getChatHistory(
            @PathVariable String id,
            HttpServletRequest request) {
        try {
            String userId = (String) request.getAttribute("userId");
            if (userId == null) {
                logger.warn("User ID not found in request, using anonymous for development");
                userId = "anonymous"; // Temporary fallback for development
            }
            
            // Verify itinerary exists and user has access
            Optional<NormalizedItinerary> itineraryOpt = itineraryJsonService.getItinerary(id);
            if (itineraryOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            // Retrieve chat history using service
            List<Map<String, Object>> chatHistory = chatHistoryService.getChatHistory(id);
            return ResponseEntity.ok(chatHistory);
        } catch (Exception e) {
            logger.error("Error getting chat history for itinerary {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Save a chat message to history
     */
    @PostMapping("/{id}/chat/history")
    public ResponseEntity<Map<String, Object>> saveChatMessage(
            @PathVariable String id,
            @RequestBody Map<String, Object> message,
            HttpServletRequest request) {
        try {
            String userId = (String) request.getAttribute("userId");
            if (userId == null) {
                logger.warn("User ID not found in request, using anonymous for development");
                userId = "anonymous"; // Temporary fallback for development
            }
            
            // Verify itinerary exists
            Optional<NormalizedItinerary> itineraryOpt = itineraryJsonService.getItinerary(id);
            if (itineraryOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            // Save chat message using service
            Map<String, Object> response = chatHistoryService.saveChatMessage(id, message, userId);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            logger.error("Error saving chat message for itinerary {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Clear chat history for an itinerary
     */
    @DeleteMapping("/{id}/chat/history")
    public ResponseEntity<Void> clearChatHistory(
            @PathVariable String id,
            HttpServletRequest request) {
        try {
            String userId = (String) request.getAttribute("userId");
            if (userId == null) {
                logger.warn("User ID not found in request, using anonymous for development");
                userId = "anonymous"; // Temporary fallback for development
            }
            
            // Verify itinerary exists
            Optional<NormalizedItinerary> itineraryOpt = itineraryJsonService.getItinerary(id);
            if (itineraryOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            // Clear chat history using service
            chatHistoryService.clearChatHistory(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            logger.error("Error clearing chat history for itinerary {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    // ===== WORKFLOW MANAGEMENT ENDPOINTS =====
    
    /**
     * Update workflow data for an itinerary
     */
    @PutMapping("/{id}/workflow")
    public ResponseEntity<NormalizedItinerary> updateWorkflow(
            @PathVariable String id,
            @RequestBody Map<String, Object> workflowData,
            HttpServletRequest request) {
        try {
            String userId = (String) request.getAttribute("userId");
            if (userId == null) {
                logger.warn("User ID not found in request for workflow update, using anonymous for development");
                userId = "anonymous"; // Allow anonymous access for development
            }
            
            // Get current itinerary
            Optional<NormalizedItinerary> itineraryOpt = itineraryJsonService.getItinerary(id);
            if (itineraryOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            NormalizedItinerary itinerary = itineraryOpt.get();
            
            // Update workflow section
            if (itinerary.getWorkflow() == null) {
                itinerary.setWorkflow(new WorkflowData());
            }
            
            // Update workflow data (simplified - you may want more specific handling)
            // This would need proper WorkflowData mapping based on your requirements
            
            // Save updated itinerary
            itineraryJsonService.saveMasterItinerary(id, itinerary);
            
            // Broadcast update
            webSocketBroadcastService.broadcastUpdate(id, "workflow_update", Map.of(
                "workflow", workflowData
            ), userId);
            
            return ResponseEntity.ok(itinerary);
        } catch (Exception e) {
            logger.error("Error updating workflow for itinerary {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get workflow data for an itinerary
     */
    @GetMapping("/{id}/workflow")
    public ResponseEntity<WorkflowData> getWorkflow(
            @PathVariable String id,
            HttpServletRequest request) {
        try {
            String userId = (String) request.getAttribute("userId");
            if (userId == null) {
                logger.warn("User ID not found in request for workflow get, using anonymous for development");
                userId = "anonymous"; // Allow anonymous access for development
            }
            
            Optional<NormalizedItinerary> itineraryOpt = itineraryJsonService.getItinerary(id);
            if (itineraryOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            NormalizedItinerary itinerary = itineraryOpt.get();
            WorkflowData workflow = itinerary.getWorkflow();
            if (workflow == null) {
                workflow = new WorkflowData(); // Return empty workflow if none exists
            }
            
            return ResponseEntity.ok(workflow);
        } catch (Exception e) {
            logger.error("Error getting workflow for itinerary {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Update a specific node in an itinerary (used by workflow sync)
     */
    @PutMapping("/{id}/nodes/{nodeId}")
    public ResponseEntity<NormalizedItinerary> updateNode(
            @PathVariable String id,
            @PathVariable String nodeId,
            @RequestBody Map<String, Object> nodeData,
            HttpServletRequest request) {
        try {
            String userId = (String) request.getAttribute("userId");
            if (userId == null) {
                logger.warn("User ID not found in request for node update, using anonymous for development");
                userId = "anonymous"; // Allow anonymous access for development
            }
            
            // Get current itinerary
            Optional<NormalizedItinerary> itineraryOpt = itineraryJsonService.getItinerary(id);
            if (itineraryOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            NormalizedItinerary itinerary = itineraryOpt.get();
            
            // Find and update the node
            boolean nodeFound = false;
            for (NormalizedDay day : itinerary.getDays()) {
                for (NormalizedNode node : day.getNodes()) {
                    if (node.getId() != null && node.getId().equals(nodeId)) {
                        // Update node data
                        if (nodeData.containsKey("title")) {
                            node.setTitle((String) nodeData.get("title"));
                        }
                        
                        // Update timing information
                        if (nodeData.containsKey("startTime") || nodeData.containsKey("endTime")) {
                            if (node.getTiming() == null) {
                                node.setTiming(new NodeTiming());
                            }
                            if (nodeData.containsKey("startTime")) {
                                // Convert string time to Long (milliseconds since epoch)
                                String timeStr = (String) nodeData.get("startTime");
                                try {
                                    // For now, just use current time + some offset
                                    // In production, you'd want proper time parsing
                                    Long startTime = System.currentTimeMillis();
                                    node.getTiming().setStartTime(startTime);
                                } catch (Exception e) {
                                    logger.warn("Failed to parse startTime: {}", timeStr);
                                }
                            }
                            if (nodeData.containsKey("endTime")) {
                                // Convert string time to Long (milliseconds since epoch)
                                String timeStr = (String) nodeData.get("endTime");
                                try {
                                    // For now, just use current time + some offset
                                    // In production, you'd want proper time parsing
                                    Long endTime = System.currentTimeMillis() + 3600000; // +1 hour
                                    node.getTiming().setEndTime(endTime);
                                } catch (Exception e) {
                                    logger.warn("Failed to parse endTime: {}", timeStr);
                                }
                            }
                        }
                        
                        // Update details (description)
                        if (nodeData.containsKey("description")) {
                            if (node.getDetails() == null) {
                                node.setDetails(new NodeDetails());
                            }
                            node.getDetails().setDescription((String) nodeData.get("description"));
                        }
                        
                        // Update cost information
                        if (nodeData.containsKey("cost")) {
                            // Handle cost update if needed - would need proper mapping
                        }
                        
                        // Update location information
                        if (nodeData.containsKey("location")) {
                            // Handle location update if needed - would need proper mapping
                        }
                        
                        // Mark as updated by user
                        node.markAsUpdated("user");
                        nodeFound = true;
                        break;
                    }
                }
                if (nodeFound) break;
            }
            
            if (!nodeFound) {
                logger.warn("Node {} not found in itinerary {}", nodeId, id);
                return ResponseEntity.notFound().build();
            }
            
            // Save updated itinerary
            itinerary.setUpdatedAt(System.currentTimeMillis());
            itineraryJsonService.updateItinerary(itinerary);
            
            // Broadcast update
            webSocketBroadcastService.broadcastUpdate(id, "node_update", Map.of(
                "nodeId", nodeId,
                "nodeData", nodeData
            ), userId);
            
            return ResponseEntity.ok(itinerary);
        } catch (Exception e) {
            logger.error("Error updating node {} in itinerary {}: {}", nodeId, id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
