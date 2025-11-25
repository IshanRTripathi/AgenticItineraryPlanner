package com.tripplanner.service.agents;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Service for tracking AI agent execution metrics.
 * Publishes agent lifecycle events to Pub/Sub for analytics.
 */
@Service
public class AgentTracker {
    
    private static final Logger logger = LoggerFactory.getLogger(AgentTracker.class);
    
    @Autowired(required = false)
    private PubSubTemplate pubSubTemplate;
    
    @Value("${analytics.pubsub.topic:analytics-events}")
    private String analyticsTopic;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Track agent execution with automatic success/failure tracking.
     * 
     * @param agentType Type of agent (e.g., "trip_planner", "activity_search")
     * @param execution The code to execute
     * @return Result of the execution
     */
    public <T> T trackAgentExecution(String agentType, Supplier<T> execution) {
        return trackAgentExecution(agentType, null, execution);
    }
    
    /**
     * Track agent execution with context.
     * 
     * @param agentType Type of agent
     * @param context Additional context (e.g., itineraryId, userId)
     * @param execution The code to execute
     * @return Result of the execution
     */
    public <T> T trackAgentExecution(String agentType, Map<String, Object> context, Supplier<T> execution) {
        long startTime = System.currentTimeMillis();
        
        // Set agent name in thread context for LLM tracking
        String previousAgent = com.tripplanner.util.UserContext.getAgentName();
        com.tripplanner.util.UserContext.setAgentName(agentType);
        
        // Track agent started
        publishAgentEvent("agent_started", agentType, context, null, 0);
        
        try {
            T result = execution.get();
            long duration = System.currentTimeMillis() - startTime;
            
            // Track agent completed
            publishAgentEvent("agent_completed", agentType, context, null, duration);
            
            return result;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            
            // Track agent failed
            Map<String, Object> errorContext = new HashMap<>(context != null ? context : new HashMap<>());
            errorContext.put("error", e.getMessage());
            errorContext.put("errorType", e.getClass().getSimpleName());
            
            publishAgentEvent("agent_failed", agentType, errorContext, e.getMessage(), duration);
            
            throw e;
        } finally {
            // Restore previous agent name
            com.tripplanner.util.UserContext.setAgentName(previousAgent);
        }
    }
    
    /**
     * Publish agent event to Pub/Sub.
     */
    private void publishAgentEvent(String eventName, String agentType, Map<String, Object> context, String error, long duration) {
        if (pubSubTemplate == null) {
            logger.debug("PubSubTemplate not available, skipping agent event: {}", eventName);
            return;
        }
        
        try {
            Map<String, Object> properties = new HashMap<>();
            properties.put("agentType", agentType);
            
            if (context != null) {
                properties.putAll(context);
            }
            
            if (duration > 0) {
                properties.put("duration", duration);
            }
            
            if (error != null) {
                properties.put("error", error);
            }
            
            Map<String, Object> event = new HashMap<>();
            event.put("eventName", eventName);
            event.put("timestamp", System.currentTimeMillis());
            event.put("userId", com.tripplanner.util.UserContext.getUserId());
            event.put("sessionId", null);
            event.put("platform", "backend");
            event.put("properties", properties);
            
            String eventJson = objectMapper.writeValueAsString(event);
            pubSubTemplate.publish(analyticsTopic, eventJson);
            
            logger.debug("Published agent event: {} for agent: {}", eventName, agentType);
        } catch (Exception e) {
            logger.error("Failed to publish agent event: {}", eventName, e);
        }
    }
}
