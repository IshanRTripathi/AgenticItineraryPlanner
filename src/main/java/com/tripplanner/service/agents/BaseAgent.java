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
        logger.info("=== AGENT EXECUTION START ===");
        logger.info("Agent Kind: {}", agentKind);
        logger.info("Agent ID: {}", agentId);
        logger.info("Itinerary ID: {}", itineraryId);
        logger.info("Request Type: {}", request.getResponseType().getSimpleName());
        Object requestData = request.getData();
        logger.info("Request Data: {}", requestData != null ? requestData.toString() : "null");
        logger.info("Request Details: {}", requestData != null ? requestData.toString() : "null");
        logger.info("=============================");
        
        // Emit queued event
        emitEvent(itineraryId, AgentEvent.AgentStatus.queued, 0, "Agent queued for execution", null);
        
        try {
            // Emit running event
            emitEvent(itineraryId, AgentEvent.AgentStatus.running, 0, "Agent started", null);
            
            logger.info("Executing agent-specific logic for: {}", agentKind);
            
            // Execute the agent-specific logic
            T result = executeInternal(itineraryId, request);
            
            // Emit success event
            emitEvent(itineraryId, AgentEvent.AgentStatus.succeeded, 100, "Agent completed successfully", null);
            
            logger.info("=== AGENT EXECUTION COMPLETED ===");
            logger.info("Agent Kind: {}", agentKind);
            logger.info("Itinerary ID: {}", itineraryId);
            logger.info("Result Type: {}", result != null ? result.getClass().getSimpleName() : "null");
            logger.info("Result: {}", result != null ? result.toString() : "null");
            logger.info("=================================");
            
            return result;
            
        } catch (Exception e) {
            logger.error("=== AGENT EXECUTION FAILED ===");
            logger.error("Agent Kind: {}", agentKind);
            logger.error("Itinerary ID: {}", itineraryId);
            logger.error("Error: {}", e.getMessage(), e);
            logger.error("==============================");
            
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
        
        @SuppressWarnings("unchecked")
        public <D> D getData(Class<D> expectedType) {
            if (data == null) {
                return null;
            }
            if (!expectedType.isAssignableFrom(data.getClass())) {
                throw new ClassCastException("Cannot cast " + data.getClass().getSimpleName() + 
                    " to " + expectedType.getSimpleName());
            }
            return (D) data;
        }
        
        public Class<T> getResponseType() {
            return responseType;
        }
    }
}
