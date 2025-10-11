package com.tripplanner.service;

import com.tripplanner.controller.WebSocketController;
import com.tripplanner.dto.ItineraryUpdateMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service for broadcasting real-time updates via WebSocket
 * Provides a clean interface for other services to send updates
 */
@Service
public class WebSocketBroadcastService {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketBroadcastService.class);

    @Autowired
    private WebSocketController webSocketController;

    /**
     * Broadcast agent progress update
     */
    public void broadcastAgentProgress(String itineraryId, String agentId, int progress, String status) {
        logger.debug("Broadcasting agent progress: {} - {}% - {}", agentId, progress, status);
        
        try {
            webSocketController.broadcastAgentProgress(itineraryId, agentId, progress, status);
        } catch (Exception e) {
            logger.error("Failed to broadcast agent progress: {}", e.getMessage(), e);
        }
    }

    /**
     * Broadcast itinerary update
     */
    public void broadcastItineraryUpdate(String itineraryId, Object itineraryData, String userId) {
        logger.debug("Broadcasting itinerary update for: {}", itineraryId);
        
        try {
            ItineraryUpdateMessage message = ItineraryUpdateMessage.createItineraryUpdated(itineraryId, itineraryData, userId);
            webSocketController.broadcastToItinerary(itineraryId, message);
        } catch (Exception e) {
            logger.error("Failed to broadcast itinerary update: {}", e.getMessage(), e);
        }
    }

    /**
     * Broadcast node update
     */
    public void broadcastNodeUpdate(String itineraryId, String nodeId, Object nodeData, String userId) {
        logger.debug("Broadcasting node update: {} in itinerary {}", nodeId, itineraryId);
        
        try {
            ItineraryUpdateMessage message = ItineraryUpdateMessage.createNodeUpdate(itineraryId, nodeId, nodeData, userId);
            webSocketController.broadcastToItinerary(itineraryId, message);
        } catch (Exception e) {
            logger.error("Failed to broadcast node update: {}", e.getMessage(), e);
        }
    }

    /**
     * Broadcast revision created
     */
    public void broadcastRevisionCreated(String itineraryId, String revisionId, String description, String userId) {
        logger.debug("Broadcasting revision created: {} for itinerary {}", revisionId, itineraryId);
        
        try {
            ItineraryUpdateMessage message = ItineraryUpdateMessage.createRevisionCreated(itineraryId, revisionId, description, userId);
            webSocketController.broadcastToItinerary(itineraryId, message);
        } catch (Exception e) {
            logger.error("Failed to broadcast revision created: {}", e.getMessage(), e);
        }
    }

    /**
     * Broadcast chat message
     */
    public void broadcastChatMessage(String itineraryId, String messageId, String content, String sender) {
        logger.debug("Broadcasting chat message: {} in itinerary {}", messageId, itineraryId);
        
        try {
            ItineraryUpdateMessage message = ItineraryUpdateMessage.createChatMessage(itineraryId, messageId, content, sender);
            webSocketController.broadcastToItinerary(itineraryId, message);
        } catch (Exception e) {
            logger.error("Failed to broadcast chat message: {}", e.getMessage(), e);
        }
    }

    /**
     * Broadcast error message
     */
    public void broadcastError(String itineraryId, String errorMessage, String userId) {
        logger.debug("Broadcasting error for itinerary {}: {}", itineraryId, errorMessage);
        
        try {
            ItineraryUpdateMessage message = ItineraryUpdateMessage.createError(itineraryId, errorMessage, userId);
            webSocketController.broadcastToItinerary(itineraryId, message);
        } catch (Exception e) {
            logger.error("Failed to broadcast error message: {}", e.getMessage(), e);
        }
    }

    /**
     * Broadcast connection status
     */
    public void broadcastConnectionStatus(String itineraryId, String status) {
        logger.debug("Broadcasting connection status for itinerary {}: {}", itineraryId, status);
        
        try {
            ItineraryUpdateMessage message = ItineraryUpdateMessage.createConnectionStatus(itineraryId, status);
            webSocketController.broadcastToItinerary(itineraryId, message);
        } catch (Exception e) {
            logger.error("Failed to broadcast connection status: {}", e.getMessage(), e);
        }
    }

    /**
     * Broadcast generic update
     */
    public void broadcastUpdate(String itineraryId, String updateType, Object data, String userId) {
        logger.debug("Broadcasting generic update: {} for itinerary {}", updateType, itineraryId);
        
        try {
            webSocketController.broadcastItineraryUpdate(itineraryId, updateType, data);
        } catch (Exception e) {
            logger.error("Failed to broadcast generic update: {}", e.getMessage(), e);
        }
    }

    /**
     * Get connection statistics
     */
    public Object getConnectionStats() {
        try {
            return webSocketController.getConnectionStats();
        } catch (Exception e) {
            logger.error("Failed to get connection stats: {}", e.getMessage(), e);
            return java.util.Map.of("error", "Failed to get stats");
        }
    }
}