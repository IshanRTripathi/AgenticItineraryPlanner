package com.tripplanner.controller;

import com.tripplanner.dto.ChatRequest;
import com.tripplanner.dto.ChatResponse;
import com.tripplanner.service.OrchestratorService;
import com.tripplanner.service.ChatProgressService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * REST controller for chat-based itinerary interactions.
 * Provides natural language interface for modifying itineraries.
 */
@RestController
@RequestMapping("/api/v1/chat")
public class ChatController {
    
    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);
    
    private final OrchestratorService orchestratorService;
    private final ChatProgressService chatProgressService;

    public ChatController(OrchestratorService orchestratorService, ChatProgressService chatProgressService) {
        this.orchestratorService = orchestratorService;
        this.chatProgressService = chatProgressService;
    }
    
    /**
     * Main chat routing endpoint.
     * POST /api/v1/chat/route → 200 → body: ChatRequest → ChatResponse
     */
    @PostMapping("/route")
    public ResponseEntity<ChatResponse> route(@Valid @RequestBody ChatRequest request, HttpServletRequest httpRequest) {
        logger.info("=== CHAT CONTROLLER: ROUTING MESSAGE ===");
        logger.info("Itinerary ID: {}", request.getItineraryId());
        logger.info("Scope: {}", request.getScope());
        logger.info("Day: {}", request.getDay());
        logger.info("Selected Node ID: {}", request.getSelectedNodeId());
        logger.info("Text: {}", request.getText());
        logger.info("Auto Apply: {}", request.isAutoApply());
        
        // Extract userId from request attributes (optional for public endpoint)
        String userId = (String) httpRequest.getAttribute("userId");
        logger.info("User ID: {}", userId != null ? userId : "anonymous");
        
        try {
            // Validate request
            validateChatRequest(request);
            
            // Route the request through orchestrator
            ChatResponse response = orchestratorService.route(request);
            
            logger.info("=== CHAT ROUTING COMPLETE ===");
            logger.info("Intent: {}", response.getIntent());
            logger.info("Message: {}", response.getMessage());
            logger.info("Applied: {}", response.isApplied());
            logger.info("Needs Disambiguation: {}", response.isNeedsDisambiguation());
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid chat request: {}", e.getMessage());
            ChatResponse errorResponse = ChatResponse.error(
                "Invalid request: " + e.getMessage(),
                List.of(e.getMessage())
            );
            return ResponseEntity.badRequest().body(errorResponse);
            
        } catch (Exception e) {
            logger.error("Error routing chat request", e);
            ChatResponse errorResponse = ChatResponse.error(
                "An unexpected error occurred while processing your request",
                List.of(e.getMessage())
            );
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    /**
     * SSE endpoint for real-time progress updates.
     * Frontend connects to this to receive progress as chat request is processed.
     * 
     * GET /api/v1/chat/progress/{sessionId}
     */
    @GetMapping("/progress/{sessionId}")
    public SseEmitter streamProgress(@PathVariable String sessionId) {
        logger.info("=== SSE CONNECTION REQUEST ===");
        logger.info("Session ID: {}", sessionId);
        
        try {
            SseEmitter emitter = chatProgressService.createEmitter(sessionId);
            logger.info("SSE emitter created successfully for session: {}", sessionId);
            return emitter;
            
        } catch (Exception e) {
            logger.error("Failed to create SSE emitter for session: {}", sessionId, e);
            SseEmitter emitter = new SseEmitter();
            emitter.completeWithError(e);
            return emitter;
        }
    }
    
    /**
     * Validate the chat request.
     */
    private void validateChatRequest(ChatRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request cannot be null");
        }
        
        if (request.getItineraryId() == null || request.getItineraryId().trim().isEmpty()) {
            throw new IllegalArgumentException("Itinerary ID is required");
        }
        
        if (request.getScope() == null || request.getScope().trim().isEmpty()) {
            throw new IllegalArgumentException("Scope is required");
        }
        
        if (!"trip".equals(request.getScope()) && !"day".equals(request.getScope())) {
            throw new IllegalArgumentException("Scope must be 'trip' or 'day'");
        }
        
        if ("day".equals(request.getScope()) && request.getDay() == null) {
            throw new IllegalArgumentException("Day is required when scope is 'day'");
        }
        
        if (request.getText() == null || request.getText().trim().isEmpty()) {
            throw new IllegalArgumentException("Text is required");
        }
        
        if (request.getText().trim().length() > 1000) {
            throw new IllegalArgumentException("Text is too long (max 1000 characters)");
        }
    }
}
