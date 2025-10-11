package com.tripplanner.service;

import com.tripplanner.dto.ItineraryUpdateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Service for managing Server-Sent Events (SSE) connections for real-time itinerary updates.
 * Handles connection registration, cleanup, event broadcasting, and missed event recovery.
 */
@Service
public class SseConnectionManager {
    
    private static final Logger logger = LoggerFactory.getLogger(SseConnectionManager.class);
    
    // Connection management
    private final Map<String, Set<SseEmitter>> itineraryConnections = new ConcurrentHashMap<>();
    private final Map<SseEmitter, ConnectionInfo> connectionInfo = new ConcurrentHashMap<>();
    
    // Event storage for missed event recovery
    private final Map<String, List<ItineraryUpdateEvent>> lastEvents = new ConcurrentHashMap<>();
    private static final int MAX_STORED_EVENTS = 10;
    
    // Connection timeout (5 minutes)
    private static final long CONNECTION_TIMEOUT_MS = 300000L;
    
    /**
     * Register a new SSE connection for an itinerary.
     */
    public void registerConnection(String itineraryId, String executionId, SseEmitter emitter) {
        logger.info("Registering SSE connection for itinerary: {}, execution: {}", itineraryId, executionId);
        
        // Store connection info
        ConnectionInfo info = new ConnectionInfo(itineraryId, executionId, System.currentTimeMillis());
        connectionInfo.put(emitter, info);
        
        // Add to itinerary connections
        itineraryConnections.computeIfAbsent(itineraryId, k -> new CopyOnWriteArraySet<>())
                           .add(emitter);
        
        // Setup emitter handlers
        setupEmitterHandlers(emitter, itineraryId, executionId);
        
        // Send missed events if any
        sendMissedEvents(emitter, itineraryId, executionId);
        
        // Send connection confirmation
        sendConnectionConfirmation(emitter, itineraryId, executionId);
        
        logger.info("SSE connection registered successfully. Total connections for {}: {}", 
                   itineraryId, itineraryConnections.get(itineraryId).size());
    }
    
    /**
     * Broadcast an update event to all connections for an itinerary.
     */
    public void broadcastUpdate(String itineraryId, ItineraryUpdateEvent event) {
        Set<SseEmitter> emitters = itineraryConnections.get(itineraryId);
        if (emitters == null || emitters.isEmpty()) {
            logger.debug("No SSE connections found for itinerary: {}", itineraryId);
            return;
        }
        
        logger.debug("Broadcasting SSE update to {} connections for itinerary: {}", 
                    emitters.size(), itineraryId);
        
        List<SseEmitter> deadEmitters = new ArrayList<>();
        
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                    .name(event.getEventType())
                    .data(event)
                    .id(generateEventId(event)));
                    
                logger.debug("SSE event sent successfully: {} to connection", event.getEventType());
                
            } catch (IOException e) {
                logger.warn("Failed to send SSE event to connection, marking as dead: {}", e.getMessage());
                deadEmitters.add(emitter);
            } catch (Exception e) {
                logger.error("Unexpected error sending SSE event: {}", e.getMessage(), e);
                deadEmitters.add(emitter);
            }
        }
        
        // Clean up dead connections
        if (!deadEmitters.isEmpty()) {
            cleanupDeadConnections(itineraryId, deadEmitters);
        }
        
        // Store event for missed event recovery
        storeEventForRecovery(itineraryId, event);
        
        logger.debug("SSE broadcast completed for itinerary: {}", itineraryId);
    }
    
    /**
     * Send missed events to a newly connected client.
     */
    private void sendMissedEvents(SseEmitter emitter, String itineraryId, String executionId) {
        List<ItineraryUpdateEvent> events = lastEvents.get(itineraryId);
        if (events == null || events.isEmpty()) {
            logger.debug("No missed events to send for itinerary: {}", itineraryId);
            return;
        }
        
        logger.info("Sending {} missed events to new connection for itinerary: {}", 
                   events.size(), itineraryId);
        
        for (ItineraryUpdateEvent event : events) {
            // Only send events for the same execution or general events
            if (executionId == null || executionId.equals(event.getExecutionId()) || 
                event.getExecutionId() == null) {
                try {
                    emitter.send(SseEmitter.event()
                        .name("missed_" + event.getEventType())
                        .data(event)
                        .id(generateEventId(event)));
                        
                } catch (IOException e) {
                    logger.warn("Failed to send missed event, connection may be dead: {}", e.getMessage());
                    break; // Stop sending if connection is dead
                } catch (Exception e) {
                    logger.error("Error sending missed event: {}", e.getMessage(), e);
                }
            }
        }
    }
    
    /**
     * Send connection confirmation event.
     */
    private void sendConnectionConfirmation(SseEmitter emitter, String itineraryId, String executionId) {
        try {
            ItineraryUpdateEvent confirmationEvent = ItineraryUpdateEvent.builder()
                .eventType("connection_established")
                .itineraryId(itineraryId)
                .executionId(executionId)
                .message("SSE connection established successfully")
                .build();
                
            emitter.send(SseEmitter.event()
                .name("connection_established")
                .data(confirmationEvent)
                .id(generateEventId(confirmationEvent)));
                
            logger.debug("Connection confirmation sent for itinerary: {}", itineraryId);
            
        } catch (Exception e) {
            logger.warn("Failed to send connection confirmation: {}", e.getMessage());
        }
    }
    
    /**
     * Setup emitter event handlers for cleanup.
     */
    private void setupEmitterHandlers(SseEmitter emitter, String itineraryId, String executionId) {
        emitter.onCompletion(() -> {
            logger.info("SSE connection completed for itinerary: {}, execution: {}", itineraryId, executionId);
            cleanupConnection(emitter, itineraryId);
        });
        
        emitter.onTimeout(() -> {
            logger.info("SSE connection timeout for itinerary: {}, execution: {}", itineraryId, executionId);
            cleanupConnection(emitter, itineraryId);
        });
        
        emitter.onError((ex) -> {
            logger.warn("SSE connection error for itinerary: {}, execution: {}: {}", 
                       itineraryId, executionId, ex.getMessage());
            cleanupConnection(emitter, itineraryId);
        });
    }
    
    /**
     * Clean up a single connection.
     */
    private void cleanupConnection(SseEmitter emitter, String itineraryId) {
        // Remove from itinerary connections
        Set<SseEmitter> emitters = itineraryConnections.get(itineraryId);
        if (emitters != null) {
            emitters.remove(emitter);
            if (emitters.isEmpty()) {
                itineraryConnections.remove(itineraryId);
                logger.debug("Removed empty connection set for itinerary: {}", itineraryId);
            }
        }
        
        // Remove connection info
        ConnectionInfo info = connectionInfo.remove(emitter);
        if (info != null) {
            logger.debug("Cleaned up connection info for itinerary: {}, duration: {}ms", 
                        itineraryId, System.currentTimeMillis() - info.getConnectedAt());
        }
    }
    
    /**
     * Clean up multiple dead connections.
     */
    private void cleanupDeadConnections(String itineraryId, List<SseEmitter> deadEmitters) {
        logger.info("Cleaning up {} dead SSE connections for itinerary: {}", 
                   deadEmitters.size(), itineraryId);
        
        for (SseEmitter deadEmitter : deadEmitters) {
            cleanupConnection(deadEmitter, itineraryId);
        }
    }
    
    /**
     * Store event for missed event recovery.
     */
    private void storeEventForRecovery(String itineraryId, ItineraryUpdateEvent event) {
        List<ItineraryUpdateEvent> events = lastEvents.computeIfAbsent(itineraryId, k -> new ArrayList<>());
        
        synchronized (events) {
            events.add(event);
            
            // Keep only the last MAX_STORED_EVENTS events
            if (events.size() > MAX_STORED_EVENTS) {
                events.remove(0);
            }
        }
        
        logger.debug("Stored event for recovery: {} (total stored: {})", 
                    event.getEventType(), events.size());
    }
    
    /**
     * Generate unique event ID.
     */
    private String generateEventId(ItineraryUpdateEvent event) {
        return String.format("%s_%s_%d", 
                           event.getItineraryId(), 
                           event.getEventType(), 
                           System.currentTimeMillis());
    }
    
    /**
     * Get connection statistics.
     */
    public Map<String, Object> getConnectionStats() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("totalItineraries", itineraryConnections.size());
        stats.put("totalConnections", connectionInfo.size());
        stats.put("totalStoredEvents", lastEvents.values().stream()
                                                 .mapToInt(List::size)
                                                 .sum());
        
        // Connection details per itinerary
        Map<String, Integer> connectionsPerItinerary = new HashMap<>();
        itineraryConnections.forEach((itineraryId, emitters) -> 
            connectionsPerItinerary.put(itineraryId, emitters.size()));
        stats.put("connectionsPerItinerary", connectionsPerItinerary);
        
        // Connection durations
        List<Long> connectionDurations = new ArrayList<>();
        long currentTime = System.currentTimeMillis();
        connectionInfo.values().forEach(info -> 
            connectionDurations.add(currentTime - info.getConnectedAt()));
        stats.put("connectionDurations", connectionDurations);
        
        return stats;
    }
    
    /**
     * Clean up old events and connections (called periodically).
     */
    public void performMaintenance() {
        logger.debug("Performing SSE connection maintenance");
        
        // Clean up old stored events (older than 1 hour)
        long cutoffTime = System.currentTimeMillis() - (60 * 60 * 1000);
        
        lastEvents.entrySet().removeIf(entry -> {
            List<ItineraryUpdateEvent> events = entry.getValue();
            synchronized (events) {
                events.removeIf(event -> 
                    event.getTimestamp().isBefore(
                        java.time.LocalDateTime.now().minusHours(1)));
                return events.isEmpty();
            }
        });
        
        logger.debug("SSE maintenance completed. Active itineraries: {}, Total connections: {}", 
                    itineraryConnections.size(), connectionInfo.size());
    }
    
    /**
     * Force close all connections for an itinerary.
     */
    public void closeConnectionsForItinerary(String itineraryId) {
        Set<SseEmitter> emitters = itineraryConnections.get(itineraryId);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }
        
        logger.info("Force closing {} SSE connections for itinerary: {}", emitters.size(), itineraryId);
        
        for (SseEmitter emitter : new ArrayList<>(emitters)) {
            try {
                emitter.complete();
            } catch (Exception e) {
                logger.warn("Error closing SSE connection: {}", e.getMessage());
            }
        }
        
        // Clean up
        itineraryConnections.remove(itineraryId);
        lastEvents.remove(itineraryId);
    }
    
    /**
     * Check if there are active connections for an itinerary.
     */
    public boolean hasActiveConnections(String itineraryId) {
        Set<SseEmitter> emitters = itineraryConnections.get(itineraryId);
        return emitters != null && !emitters.isEmpty();
    }
    
    /**
     * Get number of active connections for an itinerary.
     */
    public int getConnectionCount(String itineraryId) {
        Set<SseEmitter> emitters = itineraryConnections.get(itineraryId);
        return emitters != null ? emitters.size() : 0;
    }
    
    /**
     * Connection information holder.
     */
    private static class ConnectionInfo {
        private final String itineraryId;
        private final String executionId;
        private final long connectedAt;
        
        public ConnectionInfo(String itineraryId, String executionId, long connectedAt) {
            this.itineraryId = itineraryId;
            this.executionId = executionId;
            this.connectedAt = connectedAt;
        }
        
        public String getItineraryId() { return itineraryId; }
        public String getExecutionId() { return executionId; }
        public long getConnectedAt() { return connectedAt; }
    }
}