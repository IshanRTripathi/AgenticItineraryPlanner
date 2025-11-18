package com.tripplanner.controller;

import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Thin analytics ingestion controller.
 * Publishes events to Pub/Sub for async processing.
 * 
 * This is intentionally simple - no complex logic, no blocking operations.
 * Events are validated minimally and published immediately.
 */
@RestController
@RequestMapping("/api/v1/analytics")
public class AnalyticsIngestController {
    
    private static final Logger logger = LoggerFactory.getLogger(AnalyticsIngestController.class);
    
    private final PubSubTemplate pubSubTemplate;
    private final Gson gson;
    
    @Value("${analytics.pubsub.topic:analytics-events}")
    private String analyticsTopic;
    
    @Value("${analytics.enabled:true}")
    private boolean analyticsEnabled;
    
    public AnalyticsIngestController(PubSubTemplate pubSubTemplate) {
        this.pubSubTemplate = pubSubTemplate;
        this.gson = new Gson();
    }
    
    /**
     * Ingest a single analytics event.
     * POST /api/v1/analytics/events
     * 
     * Expected payload:
     * {
     *   "eventName": "trip_creation_completed",
     *   "timestamp": 1700000000000,
     *   "userId": "user_123",
     *   "sessionId": "session_abc",
     *   "platform": "web",
     *   "properties": { ... }
     * }
     */
    @PostMapping("/events")
    public ResponseEntity<Map<String, Object>> trackEvent(@RequestBody Map<String, Object> event) {
        if (!analyticsEnabled) {
            return ResponseEntity.ok(Map.of("success", false, "message", "Analytics disabled"));
        }
        
        // Minimal validation - only check required fields
        if (!isValidEvent(event)) {
            logger.warn("Invalid analytics event received: {}", event);
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "message", "Missing required fields: eventName, timestamp"));
        }
        
        try {
            // Publish to Pub/Sub asynchronously (non-blocking)
            String eventJson = gson.toJson(event);
            
            CompletableFuture<String> future = pubSubTemplate.publish(analyticsTopic, eventJson);
            
            // Don't wait for publish to complete - return immediately
            future.whenComplete((messageId, error) -> {
                if (error != null) {
                    logger.error("Failed to publish analytics event: {}", event.get("eventName"), error);
                } else {
                    logger.debug("Published analytics event: {} (messageId: {})", 
                               event.get("eventName"), messageId);
                }
            });
            
            return ResponseEntity.ok(Map.of("success", true));
            
        } catch (Exception e) {
            logger.error("Error publishing analytics event", e);
            // Don't fail the request - analytics should never break user experience
            return ResponseEntity.ok(Map.of("success", false, "message", "Internal error"));
        }
    }
    
    /**
     * Ingest multiple analytics events in batch.
     * POST /api/v1/analytics/events/batch
     * 
     * Expected payload:
     * {
     *   "events": [ {...}, {...}, ... ]
     * }
     */
    @PostMapping("/events/batch")
    public ResponseEntity<Map<String, Object>> trackEventsBatch(@RequestBody Map<String, Object> request) {
        if (!analyticsEnabled) {
            return ResponseEntity.ok(Map.of("success", false, "message", "Analytics disabled"));
        }
        
        @SuppressWarnings("unchecked")
        var events = (java.util.List<Map<String, Object>>) request.get("events");
        
        if (events == null || events.isEmpty()) {
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "message", "No events provided"));
        }
        
        int successCount = 0;
        int failureCount = 0;
        
        for (Map<String, Object> event : events) {
            if (!isValidEvent(event)) {
                failureCount++;
                continue;
            }
            
            try {
                String eventJson = gson.toJson(event);
                pubSubTemplate.publish(analyticsTopic, eventJson);
                successCount++;
            } catch (Exception e) {
                logger.error("Failed to publish event in batch: {}", event.get("eventName"), e);
                failureCount++;
            }
        }
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "successCount", successCount,
            "failureCount", failureCount
        ));
    }
    
    /**
     * Health check endpoint for analytics ingestion.
     * GET /api/v1/analytics/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
            "status", analyticsEnabled ? "enabled" : "disabled",
            "topic", analyticsTopic
        ));
    }
    
    /**
     * Validate that an event has required fields.
     */
    private boolean isValidEvent(Map<String, Object> event) {
        return event != null 
            && event.containsKey("eventName") 
            && event.containsKey("timestamp")
            && event.get("eventName") != null
            && event.get("timestamp") != null;
    }
}
