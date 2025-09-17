package com.tripplanner.api;

import com.tripplanner.service.AgentEventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * REST controller for agent events via Server-Sent Events (SSE).
 */
@RestController
@RequestMapping("/agents")
public class AgentController {
    
    private static final Logger logger = LoggerFactory.getLogger(AgentController.class);
    
    private final AgentEventBus agentEventBus;
    
    public AgentController(AgentEventBus agentEventBus) {
        this.agentEventBus = agentEventBus;
    }
    
    /**
     * Stream agent events for an itinerary via SSE.
     */
    @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestParam String itineraryId) {
        logger.info("Starting SSE stream for itinerary: {}", itineraryId);
        
        // Create SSE emitter with no timeout (0L means no timeout)
        SseEmitter emitter = new SseEmitter(0L);
        
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
            logger.error("SSE stream error for itinerary: " + itineraryId, throwable);
            agentEventBus.unregister(itineraryId, emitter);
        });
        
        // Send initial connection event
        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data("Connected to agent stream for itinerary: " + itineraryId));
        } catch (Exception e) {
            logger.error("Failed to send initial SSE event", e);
        }
        
        logger.debug("SSE stream established for itinerary: {}", itineraryId);
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
    
    /**
     * Response DTO for agent status.
     */
    public record AgentStatusResponse(
            String itineraryId,
            String status,
            String message
    ) {}
}
