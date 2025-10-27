package com.tripplanner.controller;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

/**
 * REST controller for analytics and event tracking.
 * Receives events from frontend for analytics purposes.
 */
@RestController
@RequestMapping("/api/v1/analytics")
public class AnalyticsController {
    
    private static final Logger logger = LoggerFactory.getLogger(AnalyticsController.class);
    
    /**
     * Track an analytics event from the frontend.
     * POST /api/v1/analytics/events
     */
    @PostMapping("/events")
    public ResponseEntity<AnalyticsEventResponse> trackEvent(
            @Valid @RequestBody AnalyticsEventRequest request) {
        
        logger.info("=== ANALYTICS EVENT ===");
        logger.info("Event: {}", request.event());
        logger.info("User ID: {}", request.userId());
        logger.info("Timestamp: {}", request.timestamp());
        
        if (request.properties() != null) {
            logger.debug("Properties: {}", request.properties());
        }
        
        // In a real implementation, you would:
        // 1. Save to database (AnalyticsEvent entity)
        // 2. Send to analytics service (Google Analytics, Mixpanel, etc.)
        // 3. Process for real-time dashboards
        
        // For now, just log and acknowledge
        AnalyticsEventResponse response = new AnalyticsEventResponse(
            true,
            "Event tracked successfully",
            Instant.now()
        );
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Track multiple events in batch.
     * POST /api/v1/analytics/events/batch
     */
    @PostMapping("/events/batch")
    public ResponseEntity<BatchAnalyticsResponse> trackEventsBatch(
            @Valid @RequestBody BatchAnalyticsRequest request) {
        
        logger.info("=== BATCH ANALYTICS EVENTS ===");
        logger.info("Number of events: {}", request.events().size());
        
        int successCount = 0;
        int failureCount = 0;
        
        for (AnalyticsEventRequest event : request.events()) {
            try {
                logger.debug("Processing event: {}", event.event());
                // Process each event
                successCount++;
            } catch (Exception e) {
                logger.error("Failed to process event: {}", event.event(), e);
                failureCount++;
            }
        }
        
        BatchAnalyticsResponse response = new BatchAnalyticsResponse(
            true,
            successCount,
            failureCount,
            "Batch processed successfully"
        );
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get analytics summary for a user.
     * GET /api/v1/analytics/summary
     */
    @GetMapping("/summary")
    public ResponseEntity<AnalyticsSummaryResponse> getAnalyticsSummary(
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        
        logger.debug("Getting analytics summary for user: {}", userId);
        
        // In a real implementation, query the database for analytics data
        // For now, return mock summary
        AnalyticsSummaryResponse response = new AnalyticsSummaryResponse(
            userId,
            0L, // totalEvents
            0L, // totalPageViews
            0L, // totalBookings
            0L, // totalSearches
            Map.of() // eventCounts
        );
        
        return ResponseEntity.ok(response);
    }
    
    // DTOs
    
    /**
     * Request DTO for tracking an analytics event.
     */
    public record AnalyticsEventRequest(
            @NotBlank(message = "Event name is required")
            String event,
            
            String userId,
            
            Map<String, Object> properties,
            
            String timestamp,
            
            String userAgent,
            
            String url
    ) {}
    
    /**
     * Response DTO for event tracking.
     */
    public record AnalyticsEventResponse(
            boolean success,
            String message,
            Instant timestamp
    ) {}
    
    /**
     * Request DTO for batch event tracking.
     */
    public record BatchAnalyticsRequest(
            java.util.List<AnalyticsEventRequest> events
    ) {}
    
    /**
     * Response DTO for batch event tracking.
     */
    public record BatchAnalyticsResponse(
            boolean success,
            int successCount,
            int failureCount,
            String message
    ) {}
    
    /**
     * Response DTO for analytics summary.
     */
    public record AnalyticsSummaryResponse(
            String userId,
            Long totalEvents,
            Long totalPageViews,
            Long totalBookings,
            Long totalSearches,
            Map<String, Long> eventCounts
    ) {}
}
