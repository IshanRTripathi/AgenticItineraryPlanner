package com.tripplanner.service;

import com.tripplanner.dto.AgentEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.*;
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
        logger.info("=== SSE EMITTER REGISTRATION ===");
        logger.info("Itinerary ID: {}", itineraryId);
        logger.info("Emitter: {}", emitter.toString());
        
        emitters.computeIfAbsent(itineraryId, k -> new CopyOnWriteArrayList<>()).add(emitter);
        
        int totalEmitters = emitters.get(itineraryId).size();
        logger.info("Total emitters for itinerary {}: {}", itineraryId, totalEmitters);
        logger.info("Total active itineraries: {}", emitters.size());
        logger.info("===============================");
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
        logger.info("=== PUBLISHING AGENT EVENT ===");
        logger.info("Itinerary ID: {}", itineraryId);
        logger.info("Agent ID: {}", event.agentId());
        logger.info("Agent Kind: {}", event.kind());
        logger.info("Status: {}", event.status());
        logger.info("Progress: {}", event.progress());
        logger.info("Message: {}", event.message());
        logger.info("Step: {}", event.step());
        logger.info("Timestamp: {}", event.updatedAt());
        
        CopyOnWriteArrayList<SseEmitter> itineraryEmitters = emitters.get(itineraryId);
        if (itineraryEmitters == null || itineraryEmitters.isEmpty()) {
            logger.warn("No emitters found for itinerary: {}", itineraryId);
            logger.info("==============================");
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
                logger.warn("Failed to send event to SSE emitter for itinerary: {} (client likely disconnected)", itineraryId);
                failureCount++;
                // Do not attempt to complete/flush on broken response; just remove the emitter
                itineraryEmitters.remove(emitter);
            }
        }
        
        logger.info("Event sent to {} emitters successfully, {} failed for itinerary: {}", 
                    successCount, failureCount, itineraryId);
        logger.info("==============================");
    }
    
    /**
     * Send agent list to all emitters for an itinerary.
     */
    public void sendAgentList(String itineraryId, Set<AgentEvent.AgentKind> agentKinds) {
        logger.info("=== SENDING AGENT LIST ===");
        logger.info("Itinerary ID: {}", itineraryId);
        logger.info("Agent Kinds: {}", agentKinds);
        
        CopyOnWriteArrayList<SseEmitter> itineraryEmitters = emitters.get(itineraryId);
        if (itineraryEmitters == null || itineraryEmitters.isEmpty()) {
            logger.warn("No emitters found for itinerary: {}", itineraryId);
            logger.info("==============================");
            return;
        }
        
        // Create agent list event
        Map<String, Object> agentListEvent = new HashMap<>();
        agentListEvent.put("type", "agent-list");
        agentListEvent.put("agents", agentKinds.stream().map(Enum::name).toArray());
        agentListEvent.put("timestamp", System.currentTimeMillis());
        
        // Send event to all registered emitters
        int successCount = 0;
        int failureCount = 0;
        
        for (SseEmitter emitter : itineraryEmitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("agent-list")
                        .data(agentListEvent));
                successCount++;
                
            } catch (IOException e) {
                logger.warn("Failed to send agent list to SSE emitter for itinerary: {} (client likely disconnected)", itineraryId);
                failureCount++;
                // Do not attempt to complete/flush on broken response; just remove the emitter
                itineraryEmitters.remove(emitter);
            }
        }
        
        logger.info("Agent list sent to {} emitters successfully, {} failed for itinerary: {}", 
                    successCount, failureCount, itineraryId);
        logger.info("==============================");
    }
    
    /**
     * Send completion event to all emitters for an itinerary.
     */
    public void sendCompletion(String itineraryId) {
        logger.info("=== SENDING COMPLETION EVENT ===");
        logger.info("Itinerary ID: {}", itineraryId);
        
        CopyOnWriteArrayList<SseEmitter> itineraryEmitters = emitters.get(itineraryId);
        if (itineraryEmitters == null || itineraryEmitters.isEmpty()) {
            logger.warn("No emitters found for itinerary: {}", itineraryId);
            logger.info("==============================");
            return;
        }
        
        // Create completion event
        Map<String, Object> completionEvent = new HashMap<>();
        completionEvent.put("type", "completion");
        completionEvent.put("status", "completed");
        completionEvent.put("timestamp", System.currentTimeMillis());
        
        // Send event to all registered emitters
        int successCount = 0;
        int failureCount = 0;
        
        for (SseEmitter emitter : itineraryEmitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("completion")
                        .data(completionEvent));
                successCount++;
                
            } catch (IOException e) {
                logger.warn("Failed to send completion event to SSE emitter for itinerary: {} (client likely disconnected)", itineraryId);
                failureCount++;
                // Do not attempt to complete/flush on broken response; just remove the emitter
                itineraryEmitters.remove(emitter);
            }
        }
        
        logger.info("Completion event sent to {} emitters successfully, {} failed for itinerary: {}", 
                    successCount, failureCount, itineraryId);
        logger.info("==============================");
    }
}

