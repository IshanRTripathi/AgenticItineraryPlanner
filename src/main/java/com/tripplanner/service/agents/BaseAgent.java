package com.tripplanner.service.agents;

import com.tripplanner.api.dto.AgentEvent;
import com.tripplanner.service.AgentEventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.UUID;

/**
 * Base class for all LLM agents.
 */
public abstract class BaseAgent {
    
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    protected final AgentEventBus eventBus;
    
    private final String agentId;
    private final AgentEvent.AgentKind agentKind;
    
    protected BaseAgent(AgentEventBus eventBus, AgentEvent.AgentKind agentKind) {
        this.eventBus = eventBus;
        this.agentKind = agentKind;
        this.agentId = UUID.randomUUID().toString();
    }
    
    /**
     * Execute the agent's task.
     */
    public final <T> T execute(String itineraryId, AgentRequest<T> request) {
        logger.info("Starting agent execution: {} for itinerary: {}", agentKind, itineraryId);
        
        // Emit queued event
        emitEvent(itineraryId, AgentEvent.AgentStatus.queued, 0, "Agent queued for execution", null);
        
        try {
            // Emit running event
            emitEvent(itineraryId, AgentEvent.AgentStatus.running, 0, "Agent started", null);
            
            // Execute the agent-specific logic
            T result = executeInternal(itineraryId, request);
            
            // Emit success event
            emitEvent(itineraryId, AgentEvent.AgentStatus.succeeded, 100, "Agent completed successfully", null);
            
            logger.info("Agent execution completed: {} for itinerary: {}", agentKind, itineraryId);
            return result;
            
        } catch (Exception e) {
            logger.error("Agent execution failed: {} for itinerary: {}", agentKind, itineraryId, e);
            
            // Emit failure event
            emitEvent(itineraryId, AgentEvent.AgentStatus.failed, 0, 
                     "Agent failed: " + e.getMessage(), null);
            
            throw new RuntimeException("Agent execution failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Execute the agent's specific logic. To be implemented by subclasses.
     */
    protected abstract <T> T executeInternal(String itineraryId, AgentRequest<T> request);
    
    /**
     * Get the agent's name for logging and events.
     */
    protected abstract String getAgentName();
    
    /**
     * Emit progress update during execution.
     */
    protected void emitProgress(String itineraryId, int progress, String message, String step) {
        emitEvent(itineraryId, AgentEvent.AgentStatus.running, progress, message, step);
    }
    
    /**
     * Emit an agent event.
     */
    private void emitEvent(String itineraryId, AgentEvent.AgentStatus status, 
                          Integer progress, String message, String step) {
        AgentEvent event = new AgentEvent(
                agentId,
                agentKind,
                status,
                progress,
                message,
                step,
                Instant.now(),
                itineraryId
        );
        
        eventBus.publish(itineraryId, event);
    }
    
    /**
     * Get the agent ID.
     */
    public String getAgentId() {
        return agentId;
    }
    
    /**
     * Get the agent kind.
     */
    public AgentEvent.AgentKind getAgentKind() {
        return agentKind;
    }
    
    /**
     * Request object for agent execution.
     */
    public static class AgentRequest<T> {
        private final Object data;
        private final Class<T> responseType;
        
        public AgentRequest(Object data, Class<T> responseType) {
            this.data = data;
            this.responseType = responseType;
        }
        
        @SuppressWarnings("unchecked")
        public <D> D getData() {
            return (D) data;
        }
        
        public Class<T> getResponseType() {
            return responseType;
        }
    }
}
