package com.tripplanner.service.agents;

import com.tripplanner.dto.AgentEvent;
import com.tripplanner.service.WebSocketEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Event bus for distributing agent events via WebSocket.
 * Replaces the old SSE-based implementation.
 * 
 * Now includes progress coordination to prevent jittery progress updates
 * when multiple agents run in parallel.
 */
@Component
public class AgentEventBus {
    
    private static final Logger logger = LoggerFactory.getLogger(AgentEventBus.class);
    
    @Autowired
    @Lazy
    private WebSocketEventPublisher webSocketEventPublisher;
    
    @Autowired
    private PipelineProgressCoordinator progressCoordinator;
    
    /**
     * Publish an event to all WebSocket subscribers for an itinerary.
     * Coordinates progress across multiple agents to prevent jittery updates.
     */
    public void publish(String itineraryId, AgentEvent event) {
        logger.info("=== PUBLISHING AGENT EVENT VIA WEBSOCKET ===");
        logger.info("Itinerary ID: {}", itineraryId);
        logger.info("Agent ID: {}", event.agentId());
        logger.info("Agent Kind: {}", event.kind());
        logger.info("Status: {}", event.status());
        logger.info("Progress (agent): {}", event.progress());
        logger.info("Message: {}", event.message());
        logger.info("Step: {}", event.step());
        logger.info("Timestamp: {}", event.updatedAt());
        
        try {
            // Coordinate progress across pipeline stages
            int agentProgress = event.progress() != null ? event.progress() : 0;
            int overallProgress;
            
            // Special handling for final completion
            if (event.status() == AgentEvent.AgentStatus.completed && 
                agentProgress == 100 && 
                "FINALIZATION".equals(event.kind().name())) {
                // Only finalization agent at 100% triggers true completion
                progressCoordinator.markComplete(itineraryId);
                overallProgress = 100;
                logger.info("🎉 Pipeline complete for {}: 100%", itineraryId);
            } else {
                overallProgress = progressCoordinator.calculateOverallProgress(
                    itineraryId, 
                    event.kind().name(), 
                    agentProgress
                );
            }
            
            logger.info("Progress (overall): {} (mapped from {}% in {})", 
                overallProgress, agentProgress, event.kind().name());
            
            // Build complete event data including message and step
            java.util.Map<String, Object> eventData = new java.util.HashMap<>();
            // Use agent kind (e.g., "PLANNER", "ENRICHMENT") as agentId instead of UUID
            // This makes it human-readable in the UI
            eventData.put("agentId", event.kind().name());
            eventData.put("kind", event.kind().name());
            eventData.put("status", event.status().name());
            eventData.put("progress", overallProgress);  // Use coordinated progress
            eventData.put("message", event.message() != null ? event.message() : "");
            eventData.put("step", event.step() != null ? event.step() : "");
            eventData.put("timestamp", event.updatedAt().toString());
            
            // Send complete event data via WebSocket
            webSocketEventPublisher.publishItineraryUpdate(
                itineraryId,
                "agent_progress",
                eventData
            );
            
            logger.debug("Agent event published successfully via WebSocket with coordinated progress");
        } catch (Exception e) {
            logger.error("Failed to publish agent event via WebSocket for itinerary: {}", itineraryId, e);
        }
        
        logger.info("==========================================");
    }
}
