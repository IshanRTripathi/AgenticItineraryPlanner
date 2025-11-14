package com.tripplanner.controller;

import com.tripplanner.dto.ItineraryUpdateMessage;
import com.tripplanner.service.ItineraryJsonService;
import com.tripplanner.service.RevisionService;
import com.tripplanner.service.OrchestratorService;
import com.tripplanner.dto.ChatRequest;
import com.tripplanner.dto.ChatResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;


import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * WebSocket controller for real-time itinerary updates
 * Handles client connections and broadcasts updates to subscribed clients
 */
@Controller
public class WebSocketController {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketController.class);

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private ItineraryJsonService itineraryJsonService;

    @Autowired
    private RevisionService revisionService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrchestratorService orchestratorService;

    // Track connected clients per itinerary
    private final Map<String, Set<String>> itinerarySubscriptions = new ConcurrentHashMap<>();

    /**
     * Handle incoming WebSocket messages for specific itinerary updates
     */
    @MessageMapping("/itinerary/{itineraryId}")
    @SendTo("/topic/itinerary/{itineraryId}")
    public ItineraryUpdateMessage handleItineraryUpdate(
            @DestinationVariable String itineraryId,
            Map<String, Object> message) {
        
        logger.info("Received WebSocket message for itinerary: {}", itineraryId);
        
        try {
            // Extract message details
            String updateType = (String) message.get("type");
            Object data = message.get("data");
            String userId = (String) message.get("userId");
            
            // Validate permissions (basic check)
            if (!validateUserPermissions(userId, itineraryId)) {
                logger.warn("User {} does not have permission to update itinerary {}", userId, itineraryId);
                return createErrorMessage(itineraryId, "Permission denied");
            }
            
            // Process the update based on type
            ItineraryUpdateMessage updateMessage = processUpdate(itineraryId, updateType, data, userId);
            
            // Broadcast to all connected clients for this itinerary
            broadcastToItinerary(itineraryId, updateMessage);
            
            return updateMessage;
            
        } catch (Exception e) {
            logger.error("Error processing WebSocket message for itinerary {}: {}", itineraryId, e.getMessage(), e);
            return createErrorMessage(itineraryId, "Failed to process update: " + e.getMessage());
        }
    }

    /**
     * Handle client subscription to itinerary updates
     */
    @SubscribeMapping("/topic/itinerary/{itineraryId}")
    public ItineraryUpdateMessage handleSubscription(@DestinationVariable String itineraryId) {
        logger.info("Client subscribed to itinerary updates: {}", itineraryId);
        
        // Send initial connection confirmation
        return ItineraryUpdateMessage.builder()
                .type("connection_established")
                .itineraryId(itineraryId)
                .data(Map.of("status", "connected", "timestamp", Instant.now().toString()))
                .timestamp(Instant.now())
                .build();
    }

    /**
     * Handle incoming chat messages
     */
    @MessageMapping("/chat")
    public void handleChatMessage(Map<String, Object> message) {
        logger.info("Received chat message: {}", message);
        
        try {
            String itineraryId = (String) message.get("itineraryId");
            String messageText = (String) message.get("message");
            Map<String, Object> context = (Map<String, Object>) message.get("context");
            
            if (itineraryId == null || messageText == null) {
                logger.warn("Invalid chat message - missing required fields");
                return;
            }
            
            // Create ChatRequest for the orchestrator
            ChatRequest chatRequest = new ChatRequest();
            chatRequest.setItineraryId(itineraryId);
            chatRequest.setText(messageText);
            chatRequest.setUserId("websocket-user"); // Default user for WebSocket messages
            
            // Set context from WebSocket message
            if (context != null) {
                chatRequest.setScope((String) context.get("scope"));
                if (context.get("selectedDay") != null) {
                    chatRequest.setDay(((Number) context.get("selectedDay")).intValue());
                }
                chatRequest.setSelectedNodeId((String) context.get("selectedNodeId"));
                chatRequest.setAutoApply((Boolean) context.getOrDefault("autoApply", false));
            }
            
            // Process with OrchestratorService
            ChatResponse chatResponse = orchestratorService.route(chatRequest);
            
            // Build data map with null-safe values
            Map<String, Object> innerData = new HashMap<>();
            if (chatResponse.getChangeSet() != null) {
                innerData.put("changeSet", chatResponse.getChangeSet());
            }
            if (chatResponse.getDiff() != null) {
                innerData.put("diff", chatResponse.getDiff());
            }
            innerData.put("applied", chatResponse.isApplied());
            if (chatResponse.getErrors() != null && !chatResponse.getErrors().isEmpty()) {
                innerData.put("errors", chatResponse.getErrors());
            }
            if (chatResponse.getWarnings() != null && !chatResponse.getWarnings().isEmpty()) {
                innerData.put("warnings", chatResponse.getWarnings());
            }
            if (chatResponse.getIntent() != null) {
                innerData.put("intent", chatResponse.getIntent());
            }
            if (chatResponse.getCandidates() != null && !chatResponse.getCandidates().isEmpty()) {
                innerData.put("candidates", chatResponse.getCandidates());
            }
            
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("id", "msg_" + System.currentTimeMillis() + "_ws");
            responseData.put("text", chatResponse.getMessage() != null ? chatResponse.getMessage() : "");
            responseData.put("sender", "assistant");
            responseData.put("timestamp", Instant.now().toString());
            responseData.put("data", innerData);
            
            // Create WebSocket response message
            ItineraryUpdateMessage response = ItineraryUpdateMessage.builder()
                    .type("chat_response")
                    .itineraryId(itineraryId)
                    .data(responseData)
                    .timestamp(Instant.now())
                    .build();
            
            // Broadcast to chat topic
            messagingTemplate.convertAndSend("/topic/chat/" + itineraryId, response);
            
            // If changes were applied, also broadcast itinerary update
            if (chatResponse.getChangeSet() != null && chatResponse.isApplied()) {
                Map<String, Object> updateData = new HashMap<>();
                updateData.put("chatResponse", chatResponse);
                updateData.put("changes", chatResponse.getChangeSet());
                
                ItineraryUpdateMessage updateMessage = ItineraryUpdateMessage.builder()
                        .type("chat_update")
                        .itineraryId(itineraryId)
                        .data(updateData)
                        .timestamp(Instant.now())
                        .build();
                
                messagingTemplate.convertAndSend("/topic/itinerary/" + itineraryId, updateMessage);
            }
            
        } catch (Exception e) {
            logger.error("Error processing chat message: {}", e.getMessage(), e);
            
            // Send error response
            String itineraryId = (String) message.get("itineraryId");
            if (itineraryId != null) {
                Map<String, Object> errorData = new HashMap<>();
                errorData.put("id", "msg_" + System.currentTimeMillis() + "_error");
                errorData.put("text", "I'm sorry, I encountered an error processing your request. Please try again.");
                errorData.put("sender", "assistant");
                errorData.put("timestamp", Instant.now().toString());
                
                Map<String, Object> errorInnerData = new HashMap<>();
                errorInnerData.put("error", e.getMessage() != null ? e.getMessage() : "Unknown error");
                errorData.put("data", errorInnerData);
                
                ItineraryUpdateMessage errorResponse = ItineraryUpdateMessage.builder()
                        .type("chat_response")
                        .itineraryId(itineraryId)
                        .data(errorData)
                        .timestamp(Instant.now())
                        .build();
                
                messagingTemplate.convertAndSend("/topic/chat/" + itineraryId, errorResponse);
            }
        }
    }

    /**
     * Process different types of updates
     */
    private ItineraryUpdateMessage processUpdate(String itineraryId, String updateType, Object data, String userId) {
        logger.debug("Processing update type: {} for itinerary: {}", updateType, itineraryId);
        
        switch (updateType) {
            case "node_update":
                return processNodeUpdate(itineraryId, data, userId);
            case "agent_progress":
                return processAgentProgress(itineraryId, data, userId);
            case "revision_created":
                return processRevisionCreated(itineraryId, data, userId);
            case "chat_message":
                return processChatMessage(itineraryId, data, userId);
            default:
                logger.warn("Unknown update type: {}", updateType);
                return createGenericUpdate(itineraryId, updateType, data);
        }
    }

    /**
     * Process node update messages
     */
    private ItineraryUpdateMessage processNodeUpdate(String itineraryId, Object data, String userId) {
        try {
            // Here you would typically update the itinerary in the database
            // For now, we'll just broadcast the update
            
            return ItineraryUpdateMessage.builder()
                    .type("node_update")
                    .itineraryId(itineraryId)
                    .data(data)
                    .userId(userId)
                    .timestamp(Instant.now())
                    .build();
                    
        } catch (Exception e) {
            logger.error("Error processing node update: {}", e.getMessage(), e);
            return createErrorMessage(itineraryId, "Failed to process node update");
        }
    }

    /**
     * Process agent progress updates
     */
    private ItineraryUpdateMessage processAgentProgress(String itineraryId, Object data, String userId) {
        return ItineraryUpdateMessage.builder()
                .type("agent_progress")
                .itineraryId(itineraryId)
                .data(data)
                .userId(userId)
                .timestamp(Instant.now())
                .build();
    }

    /**
     * Process revision creation notifications
     */
    private ItineraryUpdateMessage processRevisionCreated(String itineraryId, Object data, String userId) {
        return ItineraryUpdateMessage.builder()
                .type("revision_created")
                .itineraryId(itineraryId)
                .data(data)
                .userId(userId)
                .timestamp(Instant.now())
                .build();
    }

    /**
     * Process chat messages
     */
    private ItineraryUpdateMessage processChatMessage(String itineraryId, Object data, String userId) {
        return ItineraryUpdateMessage.builder()
                .type("chat_message")
                .itineraryId(itineraryId)
                .data(data)
                .userId(userId)
                .timestamp(Instant.now())
                .build();
    }

    /**
     * Create a generic update message
     */
    private ItineraryUpdateMessage createGenericUpdate(String itineraryId, String updateType, Object data) {
        return ItineraryUpdateMessage.builder()
                .type(updateType)
                .itineraryId(itineraryId)
                .data(data)
                .timestamp(Instant.now())
                .build();
    }

    /**
     * Create an error message
     */
    private ItineraryUpdateMessage createErrorMessage(String itineraryId, String errorMessage) {
        return ItineraryUpdateMessage.builder()
                .type("error")
                .itineraryId(itineraryId)
                .data(Map.of("error", errorMessage))
                .timestamp(Instant.now())
                .build();
    }

    /**
     * Broadcast message to all clients subscribed to an itinerary
     */
    public void broadcastToItinerary(String itineraryId, ItineraryUpdateMessage message) {
        String destination = "/topic/itinerary/" + itineraryId;
        logger.debug("Broadcasting message to destination: {}", destination);
        
        try {
            messagingTemplate.convertAndSend(destination, message);
            logger.debug("Successfully broadcasted message to {} subscribers", 
                    itinerarySubscriptions.getOrDefault(itineraryId, Set.of()).size());
        } catch (Exception e) {
            logger.error("Error broadcasting message to itinerary {}: {}", itineraryId, e.getMessage(), e);
        }
    }

    /**
     * Broadcast agent progress update
     */
    public void broadcastAgentProgress(String itineraryId, String agentId, int progress, String status) {
        Map<String, Object> data = Map.of(
                "agentId", agentId,
                "progress", progress,
                "status", status
        );
        
        ItineraryUpdateMessage message = ItineraryUpdateMessage.builder()
                .type("agent_progress")
                .itineraryId(itineraryId)
                .data(data)
                .timestamp(Instant.now())
                .build();
        
        logger.info("🔵 WEBSOCKET SEND - agent_progress: itinerary={}, progress={}%, agentId={}, status={}, fullData={}", 
                    itineraryId, progress, agentId, status, data);
        
        broadcastToItinerary(itineraryId, message);
    }

    /**
     * Broadcast itinerary update
     */
    public void broadcastItineraryUpdate(String itineraryId, String updateType, Object data) {
        ItineraryUpdateMessage message = ItineraryUpdateMessage.builder()
                .type(updateType)
                .itineraryId(itineraryId)
                .data(data)
                .timestamp(Instant.now())
                .build();
        
        broadcastToItinerary(itineraryId, message);
    }



    /**
     * Basic permission validation (to be enhanced with proper security)
     */
    private boolean validateUserPermissions(String userId, String itineraryId) {
        // TODO: Implement proper permission checking
        // For now, allow all authenticated users
        return userId != null && !userId.trim().isEmpty();
    }

    /**
     * Get connection statistics
     */
    public Map<String, Object> getConnectionStats() {
        return Map.of(
                "itinerarySubscriptions", itinerarySubscriptions.size(),
                "subscriptionDetails", itinerarySubscriptions.entrySet().stream()
                        .collect(java.util.stream.Collectors.toMap(
                                Map.Entry::getKey,
                                entry -> entry.getValue().size()
                        ))
        );
    }
}