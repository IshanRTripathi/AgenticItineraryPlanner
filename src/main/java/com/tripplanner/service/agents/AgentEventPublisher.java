package com.tripplanner.service.agents;

import com.tripplanner.dto.*;
import com.tripplanner.service.WebSocketEventPublisher;
import com.tripplanner.util.ActivityCounter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Service for publishing agent events to real-time subscribers via WebSocket.
 * Provides a clean interface for agents to publish progress, completion, and error events.
 * 
 * Replaces the old SSE-based implementation.
 */
@Component
public class AgentEventPublisher {
    
    private static final Logger logger = LoggerFactory.getLogger(AgentEventPublisher.class);
    
    @Autowired
    @Lazy
    private WebSocketEventPublisher webSocketEventPublisher;
    
    /**
     * Publish day completed event.
     * IMPROVED: Only counts attractions and meals as activities (not transport/accommodation).
     */
    public void publishDayCompleted(String itineraryId, String executionId, NormalizedDay completedDay) {
        if (completedDay == null) {
            logger.warn("Cannot publish day completed event: day data is null for itinerary {}", itineraryId);
            return;
        }
        
        try {
            // CRITICAL: Count only attractions and meals as activities
            int activityCount = ActivityCounter.countActivities(completedDay);
            
            int progress = calculateDayProgress(completedDay.getDayNumber());
            String message = String.format("Day %d planning completed with %d %s", 
                                         completedDay.getDayNumber(), 
                                         activityCount,
                                         activityCount == 1 ? "activity" : "activities");
            
            webSocketEventPublisher.publishItineraryUpdate(itineraryId, "day_completed", 
                java.util.Map.of(
                    "type", "day_completed",
                    "dayNumber", completedDay.getDayNumber(),
                    "progress", progress,
                    "message", message,
                    "activities", activityCount,  // Send correct count
                    "day", completedDay
                ));
            
            logger.info("Published day completed event via WebSocket: itinerary={}, day={}, activities={} (attractions+meals only)", 
                       itineraryId, completedDay.getDayNumber(), activityCount);
                       
        } catch (Exception e) {
            logger.error("Failed to publish day completed event for itinerary: {}, day: {}", 
                        itineraryId, completedDay.getDayNumber(), e);
        }
    }
    
    /**
     * Publish progress update event.
     */
    public void publishProgress(String itineraryId, String executionId, int progress, 
                               String message, String agentType) {
        try {
            webSocketEventPublisher.publishAgentProgress(itineraryId, agentType, progress, message);
            
            logger.debug("Published progress update via WebSocket: itinerary={}, progress={}%, agent={}, message={}", 
                        itineraryId, progress, agentType, message);
                        
        } catch (Exception e) {
            logger.error("Failed to publish progress update for itinerary: {}", itineraryId, e);
        }
    }
    
    /**
     * Publish generation complete event.
     */
    public void publishGenerationComplete(String itineraryId, String executionId, 
                                        NormalizedItinerary finalItinerary) {
        if (finalItinerary == null) {
            logger.warn("Cannot publish generation complete event: itinerary data is null for {}", itineraryId);
            return;
        }
        
        try {
            String message = buildCompletionMessage(finalItinerary);
            
            webSocketEventPublisher.publishItineraryUpdate(itineraryId, "generation_complete", 
                java.util.Map.of(
                    "message", message,
                    "itinerary", finalItinerary,
                    "progress", 100,
                    "status", "completed",
                    "type", "generation_complete"
                ));
            
            logger.info("Published generation complete event via WebSocket: itinerary={}, days={}, total_nodes={}", 
                       itineraryId, 
                       finalItinerary.getDays() != null ? finalItinerary.getDays().size() : 0,
                       calculateTotalNodes(finalItinerary));
                       
        } catch (Exception e) {
            logger.error("Failed to publish generation complete event for itinerary: {}", itineraryId, e);
        }
    }
    
    /**
     * Publish phase transition event.
     */
    public void publishPhaseTransition(String itineraryId, String executionId, String fromPhase, 
                                      String toPhase, int overallProgress) {
        try {
            webSocketEventPublisher.publishItineraryUpdate(itineraryId, "phase_transition", 
                java.util.Map.of(
                    "type", "phase_transition",
                    "fromPhase", fromPhase,
                    "toPhase", toPhase,
                    "progress", overallProgress,
                    "message", String.format("Moving from %s to %s phase", fromPhase, toPhase)
                ));
            
            logger.info("Published phase transition event via WebSocket: itinerary={}, {}→{}, progress={}%", 
                       itineraryId, fromPhase, toPhase, overallProgress);
                       
        } catch (Exception e) {
            logger.error("Failed to publish phase transition event for itinerary: {}", itineraryId, e);
        }
    }
    
    /**
     * Publish batch progress event (for day-by-day planning).
     */
    public void publishBatchProgress(String itineraryId, String executionId, int currentDay, 
                                    int totalDays, String currentActivity) {
        try {
            int progress = (int) ((currentDay * 100.0) / totalDays);
            String message = String.format("Planning day %d of %d: %s", currentDay, totalDays, currentActivity);
            
            webSocketEventPublisher.publishAgentProgress(itineraryId, "PLANNER", progress, message);
            
            logger.debug("Published batch progress via WebSocket: itinerary={}, day={}/{}, progress={}%", 
                        itineraryId, currentDay, totalDays, progress);
                        
        } catch (Exception e) {
            logger.error("Failed to publish batch progress for itinerary: {}", itineraryId, e);
        }
    }
    
    /**
     * Publish warning event.
     */
    public void publishWarning(String itineraryId, String executionId, String errorCode, 
                              String message, String recoveryAction) {
        try {
            webSocketEventPublisher.publishItineraryUpdate(itineraryId, "warning", 
                java.util.Map.of(
                    "type", "warning",
                    "errorCode", errorCode,
                    "message", message,
                    "recoveryAction", recoveryAction,
                    "severity", "WARNING"
                ));
            
            logger.info("Published warning event via WebSocket: itinerary={}, code={}, message={}, recovery={}", 
                       itineraryId, errorCode, message, recoveryAction);
                       
        } catch (Exception e) {
            logger.error("Failed to publish warning event for itinerary: {}", itineraryId, e);
        }
    }
    
    /**
     * Publish error from exception.
     */
    public void publishErrorFromException(String itineraryId, String executionId, Exception exception, 
                                         String context, ErrorEvent.ErrorSeverity severity) {
        try {
            webSocketEventPublisher.publishItineraryUpdate(itineraryId, "error", 
                java.util.Map.of(
                    "type", "error",
                    "context", context,
                    "message", exception.getMessage(),
                    "severity", severity.name(),
                    "canRetry", isRetryableException(exception)
                ));
            
            logger.error("Published exception-based error event via WebSocket: itinerary={}, context={}, exception={}", 
                        itineraryId, context, exception.getClass().getSimpleName(), exception);
                        
        } catch (Exception e) {
            logger.error("Failed to publish exception-based error event for itinerary: {}", itineraryId, e);
        }
    }
    
    /**
     * Publish agent completion event.
     */
    public void publishAgentComplete(String itineraryId, String executionId, 
                                     String agentName, int itemsProcessed) {
        try {
            String message = String.format("%s completed: %d items processed", 
                                         agentName, itemsProcessed);
            
            webSocketEventPublisher.publishItineraryUpdate(itineraryId, "agent_complete", 
                java.util.Map.of(
                    "type", "agent_complete",
                    "agentName", agentName,
                    "itemsProcessed", itemsProcessed,
                    "message", message
                ));
            
            logger.info("Published agent completion event via WebSocket: itinerary={}, agent={}, items={}", 
                       itineraryId, agentName, itemsProcessed);
                       
        } catch (Exception e) {
            logger.error("Failed to publish agent completion event for itinerary: {}, agent: {}", 
                        itineraryId, agentName, e);
        }
    }
    
    /**
     * Publish city allocation insights (NEW!).
     * Shows destination analysis, city selection reasoning, and budget estimate to the user.
     */
    public void publishCityAllocationInsights(String itineraryId, String executionId, 
                                              CityAllocationPlan plan) {
        if (plan == null) {
            logger.warn("Cannot publish city allocation insights: plan is null for itinerary {}", itineraryId);
            return;
        }
        
        try {
            // Build insights data
            java.util.Map<String, Object> insights = new java.util.HashMap<>();
            insights.put("type", "city_allocation_insights");
            insights.put("destinationType", plan.getDestinationType());
            insights.put("primaryDestination", plan.getPrimaryDestination());
            insights.put("rationale", plan.getPlanningRationale());
            insights.put("cityCount", plan.getAllocations() != null ? plan.getAllocations().size() : 0);
            
            // Add city summary
            if (plan.getAllocations() != null && !plan.getAllocations().isEmpty()) {
                java.util.List<java.util.Map<String, Object>> cities = new java.util.ArrayList<>();
                for (CityAllocation city : plan.getAllocations()) {
                    cities.add(java.util.Map.of(
                        "name", city.getCityName(),
                        "days", city.getTotalDays(),
                        "type", city.getDestinationType(),
                        "reason", city.getReason(),
                        "highlights", city.getHighlights() != null ? city.getHighlights() : java.util.List.of()
                    ));
                }
                insights.put("cities", cities);
            }
            
            // Add budget estimate
            if (plan.getBudgetEstimate() != null) {
                BudgetEstimate budget = plan.getBudgetEstimate();
                insights.put("budget", java.util.Map.of(
                    "currency", budget.getCurrency(),
                    "minPerDay", budget.getMinPerPersonPerDay(),
                    "maxPerDay", budget.getMaxPerPersonPerDay(),
                    "rationale", budget.getRationale()
                ));
            }
            
            // Add travel summary
            if (plan.getTravelSegments() != null && !plan.getTravelSegments().isEmpty()) {
                insights.put("travelSegments", plan.getTravelSegments().size());
            }
            
            webSocketEventPublisher.publishItineraryUpdate(itineraryId, "city_allocation_insights", insights);
            
            logger.info("Published city allocation insights via WebSocket: itinerary={}, cities={}, budget={}", 
                       itineraryId, 
                       plan.getAllocations() != null ? plan.getAllocations().size() : 0,
                       plan.getBudgetEstimate() != null ? 
                           plan.getBudgetEstimate().getCurrency() + " " + 
                           plan.getBudgetEstimate().getMinPerPersonPerDay() + "-" + 
                           plan.getBudgetEstimate().getMaxPerPersonPerDay() : "N/A");
                       
        } catch (Exception e) {
            logger.error("Failed to publish city allocation insights for itinerary: {}", itineraryId, e);
        }
    }
    
    /**
     * Check if there are active connections for an itinerary.
     * Always returns true for WebSocket (connections are managed by WebSocket service).
     */
    public boolean hasActiveConnections(String itineraryId) {
        return true; // WebSocket manages connections internally
    }
    
    /**
     * Get connection count for an itinerary.
     * Returns 1 for WebSocket (actual count managed by WebSocket service).
     */
    public int getConnectionCount(String itineraryId) {
        return 1; // WebSocket manages connections internally
    }
    
    // Helper methods
    
    /**
     * Calculate progress percentage based on day number.
     */
    private int calculateDayProgress(int dayNumber) {
        return Math.min(20 + (dayNumber * 10), 60);
    }
    
    /**
     * Build completion message based on final itinerary.
     */
    private String buildCompletionMessage(NormalizedItinerary itinerary) {
        int totalDays = itinerary.getDays() != null ? itinerary.getDays().size() : 0;
        int totalNodes = calculateTotalNodes(itinerary);
        
        return String.format("Itinerary completed! %d days planned with %d activities total", 
                           totalDays, totalNodes);
    }
    
    /**
     * Calculate total number of nodes across all days.
     */
    private int calculateTotalNodes(NormalizedItinerary itinerary) {
        if (itinerary.getDays() == null) {
            return 0;
        }
        
        return itinerary.getDays().stream()
                       .mapToInt(day -> day.getNodes() != null ? day.getNodes().size() : 0)
                       .sum();
    }
    
    /**
     * Determine if an exception is retryable.
     */
    private boolean isRetryableException(Exception exception) {
        if (exception instanceof java.net.SocketTimeoutException ||
            exception instanceof java.net.ConnectException ||
            exception instanceof java.io.IOException) {
            return true;
        }
        
        if (exception.getMessage() != null) {
            String message = exception.getMessage().toLowerCase();
            if (message.contains("timeout") ||
                message.contains("connection") ||
                message.contains("service unavailable") ||
                message.contains("rate limit") ||
                message.contains("quota exceeded")) {
                return true;
            }
        }
        
        if (exception instanceof IllegalArgumentException ||
            exception instanceof com.fasterxml.jackson.core.JsonProcessingException ||
            exception instanceof NumberFormatException) {
            return false;
        }
        
        return true;
    }
}
