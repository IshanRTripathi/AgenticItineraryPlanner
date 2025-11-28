package com.tripplanner.service;

import com.tripplanner.dto.ChatProgressEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for managing Server-Sent Events (SSE) connections for real-time chat progress updates.
 * Allows backend to push progress updates to frontend as chat requests are processed.
 */
@Service
public class ChatProgressService {
    
    private static final Logger logger = LoggerFactory.getLogger(ChatProgressService.class);
    private static final long SSE_TIMEOUT = 30000L; // 30 seconds
    
    // Map of sessionId -> SseEmitter
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();
    
    /**
     * Create a new SSE emitter for a session.
     * Frontend connects to this to receive progress updates.
     */
    public SseEmitter createEmitter(String sessionId) {
        logger.info("Creating SSE emitter for session: {}", sessionId);
        
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);
        emitters.put(sessionId, emitter);
        
        // Clean up on completion
        emitter.onCompletion(() -> {
            logger.debug("SSE emitter completed for session: {}", sessionId);
            emitters.remove(sessionId);
        });
        
        // Clean up on timeout
        emitter.onTimeout(() -> {
            logger.debug("SSE emitter timed out for session: {}", sessionId);
            emitters.remove(sessionId);
        });
        
        // Clean up on error
        emitter.onError((ex) -> {
            logger.warn("SSE emitter error for session {}: {}", sessionId, ex.getMessage());
            emitters.remove(sessionId);
        });
        
        // Send initial connection event
        try {
            emitter.send(SseEmitter.event()
                .name("connected")
                .data("Connected to chat progress stream"));
        } catch (IOException e) {
            logger.error("Failed to send initial SSE event", e);
            emitters.remove(sessionId);
        }
        
        return emitter;
    }
    
    /**
     * Send a progress event to a specific session.
     */
    public void sendProgress(String sessionId, ChatProgressEvent event) {
        SseEmitter emitter = emitters.get(sessionId);
        
        if (emitter == null) {
            logger.debug("No SSE emitter found for session: {}", sessionId);
            return;
        }
        
        try {
            logger.debug("Sending progress to session {}: {}", sessionId, event.getMessage());
            
            emitter.send(SseEmitter.event()
                .name("progress")
                .data(event));
                
        } catch (IOException e) {
            logger.warn("Failed to send progress event to session {}: {}", sessionId, e.getMessage());
            emitters.remove(sessionId);
        }
    }
    
    /**
     * Send a progress event with simple parameters.
     */
    public void sendProgress(String sessionId, String stage, String message, int progress) {
        sendProgress(sessionId, ChatProgressEvent.progress(stage, message, progress));
    }
    
    /**
     * Send a completion event.
     */
    public void sendComplete(String sessionId, String message) {
        sendProgress(sessionId, ChatProgressEvent.complete(message));
        
        // Complete and close the emitter
        SseEmitter emitter = emitters.get(sessionId);
        if (emitter != null) {
            emitter.complete();
            emitters.remove(sessionId);
        }
    }
    
    /**
     * Send an error event.
     */
    public void sendError(String sessionId, String message) {
        sendProgress(sessionId, ChatProgressEvent.error(message));
        
        // Complete the emitter with error
        SseEmitter emitter = emitters.get(sessionId);
        if (emitter != null) {
            emitter.completeWithError(new RuntimeException(message));
            emitters.remove(sessionId);
        }
    }
    
    /**
     * Check if a session has an active emitter.
     */
    public boolean hasActiveEmitter(String sessionId) {
        return emitters.containsKey(sessionId);
    }
    
    /**
     * Get count of active emitters.
     */
    public int getActiveEmitterCount() {
        return emitters.size();
    }
}
