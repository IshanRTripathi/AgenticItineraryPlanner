package com.tripplanner.controller;

import com.tripplanner.dto.AgentEvent;
import com.tripplanner.service.AgentEventBus;
import com.tripplanner.service.AgentRegistry;
import com.google.firebase.auth.FirebaseAuth;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static java.time.LocalTime.now;

/**
 * REST controller for agent events via Server-Sent Events (SSE).
 * 
 * This controller provides SSE endpoints for real-time agent progress updates.
 * The main itinerary creation flow is handled by ItineraryController.
 */
@RestController
@RequestMapping("/api/v1/agents")
public class AgentController {
    
    private static final Logger logger = LoggerFactory.getLogger(AgentController.class);
    
    private final AgentEventBus agentEventBus;
    private final AgentRegistry agentRegistry;
    
    @Autowired
    private FirebaseAuth firebaseAuth;
    
    public AgentController(AgentEventBus agentEventBus, AgentRegistry agentRegistry) {
        this.agentEventBus = agentEventBus;
        this.agentRegistry = agentRegistry;
    }
    
    /**
     * Stream agent events for an itinerary via SSE.
     * Token validation is handled by FirebaseSseAuthFilter.
     */
    @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestParam String itineraryId,
                           HttpServletRequest request) {
        // User ID is set by FirebaseSseAuthFilter if token is valid
        String userId = (String) request.getAttribute("userId");
        
        logger.info("=== SSE STREAM REQUEST ===");
        logger.info("Itinerary ID: {}", itineraryId);
        logger.info("User ID: {}", userId != null ? userId : "anonymous");
        logger.info("Request Time: {}", now());
        logger.info("Client requesting SSE connection");
        
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
     * Token validation is handled by FirebaseSseAuthFilter.
     */
    @GetMapping(path = "/events/{itineraryId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamEvents(@PathVariable String itineraryId,
                                  HttpServletRequest request) {
        // User ID is set by FirebaseSseAuthFilter if token is valid
        String userId = (String) request.getAttribute("userId");
        
        logger.info("=== SSE EVENTS REQUEST ===");
        logger.info("Itinerary ID: {}", itineraryId);
        logger.info("User ID: {}", userId != null ? userId : "anonymous");
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
     * 
     * Note: The main itinerary creation flow is handled by ItineraryController.
     * This endpoint provides status information for monitoring purposes.
     */
    @GetMapping("/{itineraryId}/status")
    public AgentStatusResponse getStatus(@PathVariable String itineraryId) {
        logger.debug("Getting agent status for itinerary: {}", itineraryId);
        
        // This would return current agent statuses
        // For now, return a placeholder
        return new AgentStatusResponse(itineraryId, "active", "Processing itinerary...");
    }
    
    /**
     * Response DTO for agent status.
     */
    public record AgentStatusResponse(
            String itineraryId,
            String status,
            String message
    ) {}
}

