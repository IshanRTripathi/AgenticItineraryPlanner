package com.tripplanner.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple WebSocket handler for testing - no SockJS, no STOMP
 * This is a minimal implementation to test basic WebSocket connectivity
 */
@Component
public class SimpleWebSocketController extends TextWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(SimpleWebSocketController.class);
    
    // Store active sessions
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String sessionId = session.getId();
        sessions.put(sessionId, session);
        
        logger.info("Simple WebSocket connection established: {}", sessionId);
        
        // Send welcome message
        session.sendMessage(new TextMessage("{\"type\":\"welcome\",\"message\":\"Connected to simple WebSocket\",\"timestamp\":\"" + System.currentTimeMillis() + "\"}"));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String sessionId = session.getId();
        String payload = message.getPayload();
        
        logger.info("Simple WebSocket message received from {}: {}", sessionId, payload);
        
        // Echo the message back
        String response = "{\"type\":\"echo\",\"originalMessage\":\"" + payload + "\",\"timestamp\":\"" + System.currentTimeMillis() + "\"}";
        session.sendMessage(new TextMessage(response));
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        String sessionId = session.getId();
        logger.error("Simple WebSocket transport error for session {}: {}", sessionId, exception.getMessage());
        sessions.remove(sessionId);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        String sessionId = session.getId();
        sessions.remove(sessionId);
        logger.info("Simple WebSocket connection closed: {} with status: {}", sessionId, closeStatus);
    }

    /**
     * Broadcast message to all connected sessions
     */
    public void broadcast(String message) {
        sessions.values().forEach(session -> {
            try {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(message));
                }
            } catch (IOException e) {
                logger.error("Error broadcasting message to session {}: {}", session.getId(), e.getMessage());
            }
        });
    }
}