package com.tripplanner.agents;

import com.tripplanner.dto.AgentCapabilities;
import com.tripplanner.dto.AgentEvent;
import com.tripplanner.service.AgentEventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Map;
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
     * Check if this agent can handle a specific task type.
     * Subclasses should override this to define their capabilities.
     */
    public boolean canHandle(String taskType) {
        return getCapabilities().supportsTask(taskType);
    }
    
    /**
     * Check if this agent can handle a specific task with given context.
     * Provides more granular control than simple task type checking.
     * todo: complete the implementation or remove it
     */
    public boolean canHandle(String taskType, Object taskContext) {
        return canHandle(taskType);
    }

    /**
     * Get the capabilities of this agent.
     * Subclasses should override this to define their specific capabilities.
     */
    public abstract AgentCapabilities getCapabilities();
    
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
        
        // Validate responsibility before execution
        try {
            validateResponsibility(request);
        } catch (IllegalArgumentException e) {
            logger.error("Responsibility validation failed for agent {}: {}", agentKind, e.getMessage());
            emitEvent(itineraryId, AgentEvent.AgentStatus.failed, 0, 
                     "Agent responsibility validation failed: " + e.getMessage(), null);
            throw e;
        }
        
        // Emit queued event
        emitEvent(itineraryId, AgentEvent.AgentStatus.queued, 0, "Agent queued for execution", null);
        
        try {
            // Emit running event
            emitEvent(itineraryId, AgentEvent.AgentStatus.running, 10, "Agent started", null);
            
            logger.info("Executing agent-specific logic for: {}", agentKind);
            
            // Execute the agent-specific logic
            T result = executeInternal(itineraryId, request);
            
            // Emit success event
            emitEvent(itineraryId, AgentEvent.AgentStatus.completed, 100, "Agent completed successfully", null);
            
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
     * Validate that this agent can handle the request's task type.
     */
    public void validateResponsibility(AgentRequest<?> request) {
        String taskType = determineTaskType(request);
        
        if (!canHandle(taskType)) {
            throw new IllegalArgumentException(
                String.format("Agent %s cannot handle task type: %s", agentKind, taskType)
            );
        }
    }
    
    /**
     * Determine the task type from the request.
     * Looks for "taskType" in the request data, or falls back to a normalized agent kind.
     */
    public String determineTaskType(AgentRequest<?> request) {
        Object data = request.getData();
        
        // If data is a Map, try to extract taskType
        if (data instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> dataMap = (Map<String, Object>) data;
            Object taskTypeObj = dataMap.get("taskType");
            if (taskTypeObj != null) {
                return taskTypeObj.toString().toLowerCase();
            }
        }
        
        // Fallback to normalized agent kind (map agent kind to task type)
        // BOOKING → book, EDITOR → edit, PLANNER → plan, EXPLAINER → explain, etc.
        return normalizeAgentKindToTaskType(agentKind);
    }
    
    /**
     * Normalize agent kind enum to lowercase task type.
     * This provides backward compatibility for tests and direct agent invocation.
     */
    private String normalizeAgentKindToTaskType(AgentEvent.AgentKind kind) {
        switch (kind) {
            case BOOKING:
                return "book";
            case EDITOR:
                return "edit";
            case PLANNER:
                return "plan";
            case EXPLAINER:
                return "explain";
            case ENRICHMENT:
                return "enrich";
            default:
                // For others, lowercase the kind name
                return kind.name().toLowerCase();
        }
    }
    
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
