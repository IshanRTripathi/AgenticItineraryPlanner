package com.tripplanner.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

/**
 * DTO for agent events sent via SSE.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AgentEvent(
        String agentId,
        AgentKind kind,
        AgentStatus status,
        Integer progress,
        String message,
        String step,
        Instant updatedAt,
        String itineraryId
) {
    
    /**
     * Create a new agent event.
     */
    public static AgentEvent create(String agentId, AgentKind kind, AgentStatus status, String itineraryId) {
        return new AgentEvent(agentId, kind, status, null, null, null, Instant.now(), itineraryId);
    }
    
    /**
     * Create an agent event with message.
     */
    public static AgentEvent withMessage(String agentId, AgentKind kind, AgentStatus status, String message, String itineraryId) {
        return new AgentEvent(agentId, kind, status, null, message, null, Instant.now(), itineraryId);
    }
    
    /**
     * Create an agent event with progress.
     */
    public static AgentEvent withProgress(String agentId, AgentKind kind, AgentStatus status, int progress, String message, String itineraryId) {
        return new AgentEvent(agentId, kind, status, progress, message, null, Instant.now(), itineraryId);
    }
    
    /**
     * Create an agent event with step information.
     */
    public static AgentEvent withStep(String agentId, AgentKind kind, AgentStatus status, String step, String message, String itineraryId) {
        return new AgentEvent(agentId, kind, status, null, message, step, Instant.now(), itineraryId);
    }
    
    /**
     * Agent types/kinds.
     */
    public enum AgentKind {
        planner,
        enrichment,
        places,
        route,
        hotels,
        flights,
        activities,
        bus,
        train,
        pt,
        food,
        photo,
        packing,
        cost,
        orchestrator
    }
    
    /**
     * Agent status states.
     */
    public enum AgentStatus {
        queued,
        running,
        completed,
        failed
    }
}

