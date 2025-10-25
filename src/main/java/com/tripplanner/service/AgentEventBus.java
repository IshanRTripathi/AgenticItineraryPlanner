package com.tripplanner.service;

import com.tripplanner.dto.AgentEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Event bus for distributing agent events via WebSocket.
 * Replaces the old SSE-based implementation.
 */
@Component
public class AgentEventBus {
    
    private static final Logger logger = LoggerFactory.getLogger(AgentEventBus.class);
    
    @Autowired
    @Lazy
    private WebSocketEventPublisher webSocketEventPublisher;
    
    /**
     * Publish an event to all WebSocket subscribers for an itinerary.
     */
    public void publish(String itineraryId, AgentEvent event) {
        logger.info("=== PUBLISHING AGENT EVENT VIA WEBSOCKET ===");
        logger.info("Itinerary ID: {}", itineraryId);
        logger.info("Agent ID: {}", event.agentId());
        logger.info("Agent Kind: {}", event.kind());
        logger.info("Status: {}", event.status());
        logger.info("Progress: {}", event.progress());
        logger.info("Message: {}", event.message());
        logger.info("Step: {}", event.step());
        logger.info("Timestamp: {}", event.updatedAt());
        
        try {
            // Send via WebSocket
            webSocketEventPublisher.publishAgentProgress(
                itineraryId,
                event.agentId(),
                event.progress() != null ? event.progress() : 0,
                event.status().name()
            );
            
            logger.debug("Agent event published successfully via WebSocket");
        } catch (Exception e) {
            logger.error("Failed to publish agent event via WebSocket for itinerary: {}", itineraryId, e);
        }
        
        logger.info("==========================================");
    }
}
