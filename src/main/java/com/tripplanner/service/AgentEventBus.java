package com.tripplanner.service;

import com.tripplanner.api.dto.AgentEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Event bus for distributing agent events to SSE clients.
 */
@Component
public class AgentEventBus {
    
    private static final Logger logger = LoggerFactory.getLogger(AgentEventBus.class);
    
    // Map of itinerary ID to list of SSE emitters
    private final ConcurrentHashMap<String, CopyOnWriteArrayList<SseEmitter>> emitters = new ConcurrentHashMap<>();
    
    /**
     * Register an SSE emitter for an itinerary.
     */
    public void register(String itineraryId, SseEmitter emitter) {
        logger.debug("Registering SSE emitter for itinerary: {}", itineraryId);
        
        emitters.computeIfAbsent(itineraryId, k -> new CopyOnWriteArrayList<>()).add(emitter);
        
        logger.debug("Total emitters for itinerary {}: {}", 
                    itineraryId, emitters.get(itineraryId).size());
    }
    
    /**
     * Unregister an SSE emitter for an itinerary.
     */
    public void unregister(String itineraryId, SseEmitter emitter) {
        logger.debug("Unregistering SSE emitter for itinerary: {}", itineraryId);
        
        CopyOnWriteArrayList<SseEmitter> itineraryEmitters = emitters.get(itineraryId);
        if (itineraryEmitters != null) {
            itineraryEmitters.remove(emitter);
            
            // Clean up empty lists
            if (itineraryEmitters.isEmpty()) {
                emitters.remove(itineraryId);
                logger.debug("Removed empty emitter list for itinerary: {}", itineraryId);
            } else {
                logger.debug("Remaining emitters for itinerary {}: {}", 
                            itineraryId, itineraryEmitters.size());
            }
        }
    }
    
    /**
     * Publish an event to all emitters for an itinerary.
     */
    public void publish(String itineraryId, AgentEvent event) {
        logger.debug("Publishing event for itinerary: {}, event: {}", itineraryId, event);
        
        CopyOnWriteArrayList<SseEmitter> itineraryEmitters = emitters.get(itineraryId);
        if (itineraryEmitters == null || itineraryEmitters.isEmpty()) {
            logger.debug("No emitters found for itinerary: {}", itineraryId);
            return;
        }
        
        // Send event to all registered emitters
        int successCount = 0;
        int failureCount = 0;
        
        for (SseEmitter emitter : itineraryEmitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("agent-event")
                        .data(event));
                successCount++;
                
            } catch (IOException e) {
                logger.warn("Failed to send event to SSE emitter for itinerary: {}", itineraryId, e);
                failureCount++;
                
                // Complete the failed emitter and remove it
                try {
                    emitter.complete();
                } catch (Exception completionError) {
                    logger.debug("Error completing failed emitter", completionError);
                }
                
                // Remove the failed emitter
                itineraryEmitters.remove(emitter);
            }
        }
        
        logger.debug("Event sent to {} emitters successfully, {} failed for itinerary: {}", 
                    successCount, failureCount, itineraryId);
    }
    
    /**
     * Publish an event to all emitters for an itinerary with custom event name.
     */
    public void publish(String itineraryId, String eventName, Object data) {
        logger.debug("Publishing custom event '{}' for itinerary: {}", eventName, itineraryId);
        
        CopyOnWriteArrayList<SseEmitter> itineraryEmitters = emitters.get(itineraryId);
        if (itineraryEmitters == null || itineraryEmitters.isEmpty()) {
            logger.debug("No emitters found for itinerary: {}", itineraryId);
            return;
        }
        
        // Send event to all registered emitters
        for (SseEmitter emitter : itineraryEmitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name(eventName)
                        .data(data));
                
            } catch (IOException e) {
                logger.warn("Failed to send custom event to SSE emitter for itinerary: {}", itineraryId, e);
                
                // Complete the failed emitter and remove it
                try {
                    emitter.complete();
                } catch (Exception completionError) {
                    logger.debug("Error completing failed emitter", completionError);
                }
                
                // Remove the failed emitter
                itineraryEmitters.remove(emitter);
            }
        }
    }
    
    /**
     * Get the number of active emitters for an itinerary.
     */
    public int getEmitterCount(String itineraryId) {
        CopyOnWriteArrayList<SseEmitter> itineraryEmitters = emitters.get(itineraryId);
        return itineraryEmitters != null ? itineraryEmitters.size() : 0;
    }
    
    /**
     * Get the total number of active emitters across all itineraries.
     */
    public int getTotalEmitterCount() {
        return emitters.values().stream()
                .mapToInt(CopyOnWriteArrayList::size)
                .sum();
    }
    
    /**
     * Get the number of itineraries with active emitters.
     */
    public int getActiveItineraryCount() {
        return emitters.size();
    }
    
    /**
     * Close all emitters for an itinerary.
     */
    public void closeAll(String itineraryId) {
        logger.info("Closing all emitters for itinerary: {}", itineraryId);
        
        CopyOnWriteArrayList<SseEmitter> itineraryEmitters = emitters.remove(itineraryId);
        if (itineraryEmitters != null) {
            for (SseEmitter emitter : itineraryEmitters) {
                try {
                    emitter.complete();
                } catch (Exception e) {
                    logger.debug("Error completing emitter during close", e);
                }
            }
            logger.info("Closed {} emitters for itinerary: {}", itineraryEmitters.size(), itineraryId);
        }
    }
    
    /**
     * Close all emitters across all itineraries.
     */
    public void closeAll() {
        logger.info("Closing all emitters across all itineraries");
        
        int totalClosed = 0;
        for (String itineraryId : emitters.keySet()) {
            CopyOnWriteArrayList<SseEmitter> itineraryEmitters = emitters.get(itineraryId);
            if (itineraryEmitters != null) {
                for (SseEmitter emitter : itineraryEmitters) {
                    try {
                        emitter.complete();
                        totalClosed++;
                    } catch (Exception e) {
                        logger.debug("Error completing emitter during shutdown", e);
                    }
                }
            }
        }
        
        emitters.clear();
        logger.info("Closed {} emitters total", totalClosed);
    }
}
