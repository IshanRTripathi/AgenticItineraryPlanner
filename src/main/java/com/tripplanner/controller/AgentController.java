package com.tripplanner.controller;

import com.tripplanner.dto.ChangeSet;
import com.tripplanner.dto.CreateItineraryReq;
import com.tripplanner.dto.NormalizedItinerary;
import com.tripplanner.service.AgentEventBus;
import com.tripplanner.service.AgentRegistry;
import com.tripplanner.service.ChangeEngine;
import com.tripplanner.agents.AgentOrchestrator;
import com.tripplanner.dto.AgentEvent;
import java.util.Set;
import java.util.Objects;
import java.util.stream.Collectors;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.CompletableFuture;

import static java.time.LocalTime.now;

/**
 * REST controller for agent events via Server-Sent Events (SSE).
 */
@RestController
@RequestMapping("/api/v1/agents")
public class AgentController {
    
    private static final Logger logger = LoggerFactory.getLogger(AgentController.class);
    
    private final AgentEventBus agentEventBus;
    private final AgentOrchestrator agentOrchestrator;
    private final AgentRegistry agentRegistry;
    
    @Autowired
    private FirebaseAuth firebaseAuth;
    
    public AgentController(AgentEventBus agentEventBus, AgentOrchestrator agentOrchestrator, AgentRegistry agentRegistry) {
        this.agentEventBus = agentEventBus;
        this.agentOrchestrator = agentOrchestrator;
        this.agentRegistry = agentRegistry;
    }
    
    /**
     * Stream agent events for an itinerary via SSE.
     */
    @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestParam String itineraryId, 
                           @RequestParam(required = false) String token,
                           HttpServletRequest request) {
        logger.info("=== SSE STREAM REQUEST ===");
        logger.info("Itinerary ID: {}", itineraryId);
        logger.info("Request Time: {}", now());
        logger.info("Client requesting SSE connection");
        
        // Validate token if provided
        String userId = null;
        if (token != null && !token.isEmpty()) {
            try {
                FirebaseToken decodedToken = firebaseAuth.verifyIdToken(token);
                userId = decodedToken.getUid();
                logger.info("Token validated for SSE connection, user: {}", userId);
            } catch (Exception e) {
                logger.error("Invalid token for SSE connection", e);
                throw new RuntimeException("Invalid authentication token");
            }
        } else {
            logger.warn("No token provided for SSE connection");
        }
        
        // Create SSE emitter with 30 minute timeout to prevent hanging connections
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);
        logger.info("Created SSE emitter: {}", emitter.toString());
        
        // Register emitter with event bus
        agentEventBus.register(itineraryId, emitter);
        
        // Set up cleanup handlers
        emitter.onCompletion(() -> {
            logger.debug("SSE stream completed for itinerary: {}", itineraryId);
            agentEventBus.unregister(itineraryId, emitter);
        });
        
        emitter.onTimeout(() -> {
            logger.debug("SSE stream timed out for itinerary: {}", itineraryId);
            agentEventBus.unregister(itineraryId, emitter);
        });
        
        emitter.onError((throwable) -> {
            // Client disconnects often manifest as IOExceptions; treat as normal
            logger.warn("SSE stream ended for itinerary {} due to: {}", itineraryId, throwable.toString());
            agentEventBus.unregister(itineraryId, emitter);
        });
        
        // Send initial connection event
        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data("Connected to agent stream for itinerary: " + itineraryId));
            logger.info("Initial SSE connection event sent successfully");
            
            // Send available agents list
            Set<String> availableAgentIds = agentRegistry.getRegisteredAgentIds();
            // Convert to AgentKind set for backward compatibility
            Set<AgentEvent.AgentKind> availableAgents = availableAgentIds.stream()
                .map(id -> {
                    try {
                        return AgentEvent.AgentKind.valueOf(id.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        // For agents with unique IDs, try to find their kind
                        return agentRegistry.getAgent(id)
                            .map(agent -> {
                                try {
                                    java.lang.reflect.Method method = agent.getClass().getMethod("getAgentKind");
                                    return (AgentEvent.AgentKind) method.invoke(agent);
                                } catch (Exception ex) {
                                    return null;
                                }
                            })
                            .orElse(null);
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
            agentEventBus.sendAgentList(itineraryId, availableAgents);
            
        } catch (Exception e) {
            logger.error("Failed to send initial SSE event", e);
        }
        
        logger.info("=== SSE STREAM ESTABLISHED ===");
        logger.info("Itinerary ID: {}", itineraryId);
        logger.info("Emitter registered and ready");
        logger.info("=============================");
        
        return emitter;
    }
    
    /**
     * Stream agent events for an itinerary via SSE (alternative endpoint).
     */
    @GetMapping(path = "/events/{itineraryId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamEvents(@PathVariable String itineraryId) {
        logger.info("=== SSE EVENTS REQUEST ===");
        logger.info("Itinerary ID: {}", itineraryId);
        logger.info("Request Time: {}", now());
        logger.info("Client requesting SSE events connection");
        
        // Create SSE emitter with 30 minute timeout to prevent hanging connections
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);
        logger.info("Created SSE emitter: {}", emitter.toString());
        
        // Register emitter with event bus
        agentEventBus.register(itineraryId, emitter);
        
        // Set up cleanup handlers
        emitter.onCompletion(() -> {
            logger.debug("SSE events stream completed for itinerary: {}", itineraryId);
            agentEventBus.unregister(itineraryId, emitter);
        });
        
        emitter.onTimeout(() -> {
            logger.debug("SSE events stream timed out for itinerary: {}", itineraryId);
            agentEventBus.unregister(itineraryId, emitter);
        });
        
        emitter.onError((throwable) -> {
            // Client disconnects often manifest as IOExceptions; treat as normal
            if (throwable instanceof java.io.IOException) {
                logger.debug("SSE events stream ended for itinerary {} due to client disconnect: {}", itineraryId, throwable.getMessage());
            } else {
                logger.warn("SSE events stream ended for itinerary {} due to: {}", itineraryId, throwable.toString());
            }
            agentEventBus.unregister(itineraryId, emitter);
        });
        
        // Send initial connection event
        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data("Connected to agent stream for itinerary: " + itineraryId));
            logger.info("Initial SSE events connection event sent successfully");
        } catch (Exception e) {
            logger.error("Failed to send initial SSE events event", e);
        }
        
        logger.info("=== SSE EVENTS STREAM ESTABLISHED ===");
        logger.info("Itinerary ID: {}", itineraryId);
        logger.info("Emitter registered and ready");
        logger.info("=============================");
        
        return emitter;
    }
    
    /**
     * Get agent status for an itinerary (alternative to SSE for polling).
     */
    @GetMapping("/{itineraryId}/status")
    public AgentStatusResponse getStatus(@PathVariable String itineraryId) {
        logger.debug("Getting agent status for itinerary: {}", itineraryId);
        
        // This would return current agent statuses
        // For now, return a placeholder
        return new AgentStatusResponse(itineraryId, "active", "Processing itinerary...");
    }
    
    // ===== NEW MVP CONTRACT ENDPOINTS =====
    
    /**
     * Execute agents to generate a normalized itinerary.
     * POST /agents/run → 200 → body: CreateItineraryReq → {itineraryId, status}
     */
    @PostMapping("/run")
    public ResponseEntity<AgentRunResponse> runAgents(@Valid @RequestBody CreateItineraryReq request, HttpServletRequest httpRequest) {
        logger.info("=== AGENT CONTROLLER: RUNNING AGENTS ===");
        logger.info("Destination: {}", request.getDestination());
        logger.info("Duration: {} days", request.getDurationDays());
        
        try {
            // Extract userId from request attributes (set by FirebaseAuthConfig)
            String userId = (String) httpRequest.getAttribute("userId");
            if (userId == null) {
                logger.error("User ID not found in request");
                return ResponseEntity.status(401).build();
            }
            
            // Generate a unique itinerary ID
            String itineraryId = "it_" + request.getDestination().toLowerCase().replaceAll("\\s+", "_") + "_" + System.currentTimeMillis();
            
            // Run the agent orchestration asynchronously
            CompletableFuture<NormalizedItinerary> future = agentOrchestrator.generateNormalizedItinerary(itineraryId, request, userId);
            
            // Return immediately with the itinerary ID
            AgentRunResponse response = new AgentRunResponse(
                itineraryId,
                "running",
                "Agents are processing your itinerary. Use SSE to track progress."
            );
            
            logger.info("=== AGENT EXECUTION STARTED ===");
            logger.info("Itinerary ID: {}", itineraryId);
            logger.info("Status: {}", response.status());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to run agents", e);
            return ResponseEntity.status(500).body(new AgentRunResponse(
                null,
                "failed",
                "Failed to start agent execution: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Process a user request to modify an existing itinerary.
     * POST /agents/process-request → 200 → body: {itineraryId, userRequest} → {changeSet, status}
     */
    @PostMapping("/process-request")
    public ResponseEntity<ProcessRequestResponse> processUserRequest(@Valid @RequestBody ProcessRequestRequest request) {
        logger.info("=== AGENT CONTROLLER: PROCESSING USER REQUEST ===");
        logger.info("Itinerary ID: {}", request.itineraryId());
        logger.info("User Request: {}", request.userRequest());
        
        try {
            // Process the user request asynchronously
            CompletableFuture<ChangeSet> future = agentOrchestrator.processUserRequest(request.itineraryId(), request.userRequest());
            
            // For now, we'll return a placeholder response
            // In a real implementation, you might want to wait for the result or return a job ID
            ProcessRequestResponse response = new ProcessRequestResponse(
                request.itineraryId(),
                "processing",
                "Processing your request. Use SSE to track progress.",
                null // ChangeSet will be available via SSE
            );
            
            logger.info("=== USER REQUEST PROCESSING STARTED ===");
            logger.info("Itinerary ID: {}", request.itineraryId());
            logger.info("Status: {}", response.status());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to process user request", e);
            return ResponseEntity.status(500).body(new ProcessRequestResponse(
                request.itineraryId(),
                "failed",
                "Failed to process request: " + e.getMessage(),
                null
            ));
        }
    }
    
    /**
     * Apply a ChangeSet with ENRICHMENT.
     * POST /agents/apply-with-ENRICHMENT → 200 → body: {itineraryId, changeSet} → {result, status}
     */
    @PostMapping("/apply-with-enrichment")
    public ResponseEntity<ApplyWithEnrichmentResponse> applyWithEnrichment(@Valid @RequestBody ApplyWithEnrichmentRequest request) {
        logger.info("=== AGENT CONTROLLER: APPLYING WITH ENRICHMENT ===");
        logger.info("Itinerary ID: {}", request.itineraryId());
        logger.info("ChangeSet: {}", request.changeSet());
        
        try {
            // Apply the ChangeSet with ENRICHMENT asynchronously
            CompletableFuture<ChangeEngine.ApplyResult> future = agentOrchestrator.applyChangeSetWithEnrichment(
                request.itineraryId(), request.changeSet());
            
            // Return immediately with status
            ApplyWithEnrichmentResponse response = new ApplyWithEnrichmentResponse(
                request.itineraryId(),
                "applying",
                "Applying changes with ENRICHMENT. Use SSE to track progress.",
                null // Result will be available via SSE
            );
            
            logger.info("=== APPLY WITH ENRICHMENT STARTED ===");
            logger.info("Itinerary ID: {}", request.itineraryId());
            logger.info("Status: {}", response.status());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to apply with ENRICHMENT", e);
            return ResponseEntity.status(500).body(new ApplyWithEnrichmentResponse(
                request.itineraryId(),
                "failed",
                "Failed to apply changes: " + e.getMessage(),
                null
            ));
        }
    }
    
    /**
     * Response DTO for agent status.
     */
    public record AgentStatusResponse(
            String itineraryId,
            String status,
            String message
    ) {}
    
    /**
     * Response DTO for agent run.
     */
    public record AgentRunResponse(
            String itineraryId,
            String status,
            String message
    ) {}
    
    /**
     * Request DTO for processing user request.
     */
    public record ProcessRequestRequest(
            String itineraryId,
            String userRequest
    ) {}
    
    /**
     * Response DTO for processing user request.
     */
    public record ProcessRequestResponse(
            String itineraryId,
            String status,
            String message,
            ChangeSet changeSet
    ) {}
    
    /**
     * Request DTO for applying with ENRICHMENT.
     */
    public record ApplyWithEnrichmentRequest(
            String itineraryId,
            ChangeSet changeSet
    ) {}
    
    /**
     * Response DTO for applying with ENRICHMENT.
     */
    public record ApplyWithEnrichmentResponse(
            String itineraryId,
            String status,
            String message,
            ChangeEngine.ApplyResult result
    ) {}
}

