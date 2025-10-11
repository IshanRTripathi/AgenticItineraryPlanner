package com.tripplanner.service;

import com.tripplanner.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Service for publishing agent events to real-time subscribers via SSE.
 * Provides a clean interface for agents to publish progress, completion, and error events.
 */
@Component
public class AgentEventPublisher {
    
    private static final Logger logger = LoggerFactory.getLogger(AgentEventPublisher.class);
    
    private final SseConnectionManager sseConnectionManager;
    
    // WebSocketEventPublisher removed to break circular dependency
    
    public AgentEventPublisher(SseConnectionManager sseConnectionManager) {
        this.sseConnectionManager = sseConnectionManager;
    }
    
    /**
     * Publish day completed event.
     */
    public void publishDayCompleted(String itineraryId, String executionId, NormalizedDay completedDay) {
        if (completedDay == null) {
            logger.warn("Cannot publish day completed event: day data is null for itinerary {}", itineraryId);
            return;
        }
        
        try {
            // Calculate progress based on day number (assuming sequential planning)
            int progress = calculateDayProgress(completedDay.getDayNumber());
            
            ItineraryUpdateEvent event = ItineraryUpdateEvent.dayCompleted(
                itineraryId,
                executionId,
                completedDay.getDayNumber(),
                completedDay,
                progress,
                String.format("Day %d planning completed with %d activities", 
                             completedDay.getDayNumber(), 
                             completedDay.getNodes() != null ? completedDay.getNodes().size() : 0)
            );
            
            sseConnectionManager.broadcastUpdate(itineraryId, event);
            
            logger.info("Published day completed event: itinerary={}, day={}, activities={}", 
                       itineraryId, completedDay.getDayNumber(), 
                       completedDay.getNodes() != null ? completedDay.getNodes().size() : 0);
                       
        } catch (Exception e) {
            logger.error("Failed to publish day completed event for itinerary: {}, day: {}", 
                        itineraryId, completedDay.getDayNumber(), e);
        }
    }
    
    /**
     * Publish node enhanced event.
     */
    public void publishNodeEnhanced(String itineraryId, String executionId, String nodeId, 
                                   NormalizedNode enhancedNode, String enhancementType) {
        if (enhancedNode == null || nodeId == null) {
            logger.warn("Cannot publish node enhanced event: node data or ID is null for itinerary {}", itineraryId);
            return;
        }
        
        try {
            String message = buildEnhancementMessage(enhancedNode, enhancementType);
            
            ItineraryUpdateEvent event = ItineraryUpdateEvent.nodeEnhanced(
                itineraryId,
                executionId,
                nodeId,
                enhancedNode,
                message,
                determineAgentType(enhancementType)
            );
            
            sseConnectionManager.broadcastUpdate(itineraryId, event);
            
            logger.debug("Published node enhanced event: itinerary={}, node={}, type={}", 
                        itineraryId, nodeId, enhancementType);
                        
        } catch (Exception e) {
            logger.error("Failed to publish node enhanced event for itinerary: {}, node: {}", 
                        itineraryId, nodeId, e);
        }
    }
    
    /**
     * Publish progress update event.
     */
    public void publishProgress(String itineraryId, String executionId, int progress, 
                               String message, String agentType) {
        try {
            ItineraryUpdateEvent event = ItineraryUpdateEvent.progressUpdate(
                itineraryId,
                executionId,
                progress,
                message,
                agentType
            );
            
            // WebSocket publishing removed to break circular dependency
            
            // Also send to SSE (for backward compatibility)
            sseConnectionManager.broadcastUpdate(itineraryId, event);
            
            logger.debug("Published progress update: itinerary={}, progress={}%, agent={}, message={}", 
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
            
            ItineraryUpdateEvent event = ItineraryUpdateEvent.generationComplete(
                itineraryId,
                executionId,
                finalItinerary
            );
            event.setMessage(message);
            
            // WebSocket publishing removed to break circular dependency
            
            // Also send to SSE (for backward compatibility)
            sseConnectionManager.broadcastUpdate(itineraryId, event);
            
            logger.info("Published generation complete event: itinerary={}, days={}, total_nodes={}", 
                       itineraryId, 
                       finalItinerary.getDays() != null ? finalItinerary.getDays().size() : 0,
                       calculateTotalNodes(finalItinerary));
                       
        } catch (Exception e) {
            logger.error("Failed to publish generation complete event for itinerary: {}", itineraryId, e);
        }
    }
    
    /**
     * Publish agent started event.
     */
    public void publishAgentStarted(String itineraryId, String executionId, String agentType, 
                                   String stageName, String description) {
        try {
            ItineraryUpdateEvent event = ItineraryUpdateEvent.builder()
                .eventType("agent_started")
                .itineraryId(itineraryId)
                .executionId(executionId)
                .agentType(agentType)
                .message(String.format("Started %s: %s", stageName, description))
                .data(java.util.Map.of(
                    "stageName", stageName,
                    "description", description,
                    "agentType", agentType
                ))
                .build();
            
            sseConnectionManager.broadcastUpdate(itineraryId, event);
            
            logger.info("Published agent started event: itinerary={}, agent={}, stage={}", 
                       itineraryId, agentType, stageName);
                       
        } catch (Exception e) {
            logger.error("Failed to publish agent started event for itinerary: {}", itineraryId, e);
        }
    }
    
    /**
     * Publish agent completed event.
     */
    public void publishAgentCompleted(String itineraryId, String executionId, String agentType, 
                                     String stageName, Object result) {
        try {
            ItineraryUpdateEvent event = ItineraryUpdateEvent.builder()
                .eventType("agent_completed")
                .itineraryId(itineraryId)
                .executionId(executionId)
                .agentType(agentType)
                .message(String.format("Completed %s successfully", stageName))
                .data(java.util.Map.of(
                    "stageName", stageName,
                    "agentType", agentType,
                    "result", result != null ? result : "success"
                ))
                .build();
            
            sseConnectionManager.broadcastUpdate(itineraryId, event);
            
            logger.info("Published agent completed event: itinerary={}, agent={}, stage={}", 
                       itineraryId, agentType, stageName);
                       
        } catch (Exception e) {
            logger.error("Failed to publish agent completed event for itinerary: {}", itineraryId, e);
        }
    }
    
    /**
     * Publish phase transition event.
     */
    public void publishPhaseTransition(String itineraryId, String executionId, String fromPhase, 
                                      String toPhase, int overallProgress) {
        try {
            ItineraryUpdateEvent event = ItineraryUpdateEvent.builder()
                .eventType("phase_transition")
                .itineraryId(itineraryId)
                .executionId(executionId)
                .progress(overallProgress)
                .message(String.format("Moving from %s to %s phase", fromPhase, toPhase))
                .agentType("orchestrator")
                .data(java.util.Map.of(
                    "fromPhase", fromPhase,
                    "toPhase", toPhase,
                    "overallProgress", overallProgress
                ))
                .build();
            
            sseConnectionManager.broadcastUpdate(itineraryId, event);
            
            logger.info("Published phase transition event: itinerary={}, {}→{}, progress={}%", 
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
            
            ItineraryUpdateEvent event = ItineraryUpdateEvent.progressUpdate(
                itineraryId,
                executionId,
                progress,
                String.format("Planning day %d of %d: %s", currentDay, totalDays, currentActivity),
                "PLANNER"
            );
            
            event.setData(java.util.Map.of(
                "currentDay", currentDay,
                "totalDays", totalDays,
                "currentActivity", currentActivity,
                "batchProgress", true
            ));
            
            sseConnectionManager.broadcastUpdate(itineraryId, event);
            
            logger.debug("Published batch progress: itinerary={}, day={}/{}, progress={}%", 
                        itineraryId, currentDay, totalDays, progress);
                        
        } catch (Exception e) {
            logger.error("Failed to publish batch progress for itinerary: {}", itineraryId, e);
        }
    }
    
    /**
     * Publish error event.
     */
    public void publishError(String itineraryId, String executionId, String errorCode, 
                            String message, ErrorEvent.ErrorSeverity severity, boolean canRetry) {
        try {
            ErrorEvent errorEvent = ErrorEvent.error(itineraryId, executionId, errorCode, message, canRetry);
            errorEvent.setSeverity(severity);
            
            sseConnectionManager.broadcastUpdate(itineraryId, errorEvent);
            
            logger.warn("Published error event: itinerary={}, code={}, severity={}, message={}", 
                       itineraryId, errorCode, severity, message);
                       
        } catch (Exception e) {
            logger.error("Failed to publish error event for itinerary: {}", itineraryId, e);
        }
    }
    
    /**
     * Publish warning event.
     */
    public void publishWarning(String itineraryId, String executionId, String errorCode, 
                              String message, String recoveryAction) {
        try {
            ErrorEvent warningEvent = ErrorEvent.warning(itineraryId, executionId, errorCode, message, recoveryAction);
            
            sseConnectionManager.broadcastUpdate(itineraryId, warningEvent);
            
            logger.info("Published warning event: itinerary={}, code={}, message={}, recovery={}", 
                       itineraryId, errorCode, message, recoveryAction);
                       
        } catch (Exception e) {
            logger.error("Failed to publish warning event for itinerary: {}", itineraryId, e);
        }
    }
    
    /**
     * Publish critical error event.
     */
    public void publishCriticalError(String itineraryId, String executionId, String errorCode, 
                                    String message, String recoveryAction) {
        try {
            ErrorEvent criticalEvent = ErrorEvent.critical(itineraryId, executionId, errorCode, message, recoveryAction);
            
            sseConnectionManager.broadcastUpdate(itineraryId, criticalEvent);
            
            logger.error("Published critical error event: itinerary={}, code={}, message={}, recovery={}", 
                        itineraryId, errorCode, message, recoveryAction);
                        
        } catch (Exception e) {
            logger.error("Failed to publish critical error event for itinerary: {}", itineraryId, e);
        }
    }
    
    /**
     * Publish partial failure event.
     */
    public void publishPartialFailure(String itineraryId, String executionId, String component, 
                                     String failureReason, String continuationPlan) {
        try {
            ItineraryUpdateEvent event = ItineraryUpdateEvent.builder()
                .eventType("partial_failure")
                .itineraryId(itineraryId)
                .executionId(executionId)
                .message(String.format("Partial failure in %s: %s. Continuing with: %s", 
                                     component, failureReason, continuationPlan))
                .agentType("orchestrator")
                .data(java.util.Map.of(
                    "component", component,
                    "failureReason", failureReason,
                    "continuationPlan", continuationPlan,
                    "severity", "partial"
                ))
                .build();
            
            sseConnectionManager.broadcastUpdate(itineraryId, event);
            
            logger.warn("Published partial failure event: itinerary={}, component={}, reason={}", 
                       itineraryId, component, failureReason);
                       
        } catch (Exception e) {
            logger.error("Failed to publish partial failure event for itinerary: {}", itineraryId, e);
        }
    }
    
    /**
     * Publish error from exception.
     */
    public void publishErrorFromException(String itineraryId, String executionId, Exception exception, 
                                         String context, ErrorEvent.ErrorSeverity severity) {
        try {
            ErrorEvent errorEvent = ErrorEvent.fromException(itineraryId, executionId, exception, severity);
            errorEvent.setMessage(String.format("Error in %s: %s", context, exception.getMessage()));
            
            // Determine if error is retryable based on exception type
            boolean canRetry = isRetryableException(exception);
            errorEvent.setCanRetry(canRetry);
            
            if (canRetry) {
                errorEvent.setRecoveryAction("Retrying operation automatically");
            } else {
                errorEvent.setRecoveryAction("Manual intervention may be required");
            }
            
            sseConnectionManager.broadcastUpdate(itineraryId, errorEvent);
            
            logger.error("Published exception-based error event: itinerary={}, context={}, exception={}", 
                        itineraryId, context, exception.getClass().getSimpleName(), exception);
                        
        } catch (Exception e) {
            logger.error("Failed to publish exception-based error event for itinerary: {}", itineraryId, e);
        }
    }
    
    /**
     * Publish agent failure event.
     */
    public void publishAgentFailure(String itineraryId, String executionId, String agentType, 
                                   String stageName, String failureReason, boolean willRetry) {
        try {
            ItineraryUpdateEvent event = ItineraryUpdateEvent.builder()
                .eventType("agent_failed")
                .itineraryId(itineraryId)
                .executionId(executionId)
                .agentType(agentType)
                .message(String.format("Agent %s failed in %s: %s%s", 
                                     agentType, stageName, failureReason,
                                     willRetry ? " (will retry)" : ""))
                .data(java.util.Map.of(
                    "stageName", stageName,
                    "agentType", agentType,
                    "failureReason", failureReason,
                    "willRetry", willRetry
                ))
                .build();
            
            sseConnectionManager.broadcastUpdate(itineraryId, event);
            
            logger.warn("Published agent failure event: itinerary={}, agent={}, stage={}, retry={}", 
                       itineraryId, agentType, stageName, willRetry);
                       
        } catch (Exception e) {
            logger.error("Failed to publish agent failure event for itinerary: {}", itineraryId, e);
        }
    }
    
    /**
     * Check if there are active connections for an itinerary.
     */
    public boolean hasActiveConnections(String itineraryId) {
        return sseConnectionManager.hasActiveConnections(itineraryId);
    }
    
    /**
     * Get connection count for an itinerary.
     */
    public int getConnectionCount(String itineraryId) {
        return sseConnectionManager.getConnectionCount(itineraryId);
    }
    
    // Helper methods
    
    /**
     * Calculate progress percentage based on day number.
     * Assumes planning phase takes 60% of total time.
     */
    private int calculateDayProgress(int dayNumber) {
        // This is a simple calculation - in reality, you'd want to know total days
        // For now, assume each day represents incremental progress
        return Math.min(20 + (dayNumber * 10), 60); // Planning phase: 20-60%
    }
    
    /**
     * Build enhancement message based on node and enhancement type.
     */
    private String buildEnhancementMessage(NormalizedNode node, String enhancementType) {
        String nodeTitle = node.getTitle() != null ? node.getTitle() : "Activity";
        
        return switch (enhancementType.toLowerCase()) {
            case "photos" -> String.format("Added photos to %s", nodeTitle);
            case "details" -> String.format("Enhanced details for %s", nodeTitle);
            case "location" -> String.format("Validated location for %s", nodeTitle);
            case "booking" -> String.format("Added booking information for %s", nodeTitle);
            case "rating" -> String.format("Added rating and reviews for %s", nodeTitle);
            default -> String.format("Enhanced %s", nodeTitle);
        };
    }
    
    /**
     * Determine agent type based on enhancement type.
     */
    private String determineAgentType(String enhancementType) {
        return switch (enhancementType.toLowerCase()) {
            case "photos", "details", "rating" -> "ENRICHMENT";
            case "location" -> "places";
            case "booking" -> "booking";
            default -> "ENRICHMENT";
        };
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
        // Network-related exceptions are usually retryable
        if (exception instanceof java.net.SocketTimeoutException ||
            exception instanceof java.net.ConnectException ||
            exception instanceof java.io.IOException) {
            return true;
        }
        
        // Service unavailable exceptions are retryable
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
        
        // Validation and parsing errors are usually not retryable
        if (exception instanceof IllegalArgumentException ||
            exception instanceof com.fasterxml.jackson.core.JsonProcessingException ||
            exception instanceof NumberFormatException) {
            return false;
        }
        
        // Default to retryable for unknown exceptions
        return true;
    }
}