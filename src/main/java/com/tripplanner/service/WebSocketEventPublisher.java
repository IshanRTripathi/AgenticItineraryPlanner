package com.tripplanner.service;

import com.tripplanner.controller.WebSocketController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Service for publishing events to WebSocket connections.
 * This service is separate from AgentEventBus to avoid circular dependencies.
 */
@Component
public class WebSocketEventPublisher {
    
    private static final Logger logger = LoggerFactory.getLogger(WebSocketEventPublisher.class);
    
    @Autowired
    @Lazy
    private WebSocketController webSocketController;
    
    /**
     * Publish agent progress update to WebSocket.
     */
    public void publishAgentProgress(String itineraryId, String agentId, int progress, String status) {
        try {
            webSocketController.broadcastAgentProgress(itineraryId, agentId, progress, status);
            logger.debug("Agent progress sent to WebSocket: itinerary={}, agent={}, progress={}%", 
                        itineraryId, agentId, progress);
        } catch (Exception e) {
            logger.error("Failed to send agent progress to WebSocket for itinerary: {}", itineraryId, e);
        }
    }
    
    /**
     * Publish itinerary update to WebSocket.
     */
    public void publishItineraryUpdate(String itineraryId, String updateType, Object data) {
        try {
            webSocketController.broadcastItineraryUpdate(itineraryId, updateType, data);
            logger.debug("Itinerary update sent to WebSocket: itinerary={}, type={}", 
                        itineraryId, updateType);
        } catch (Exception e) {
            logger.error("Failed to send itinerary update to WebSocket for itinerary: {}", itineraryId, e);
        }
    }
}
