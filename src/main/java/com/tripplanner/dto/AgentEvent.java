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
     * Agent types/kinds.
     */
    public enum AgentKind {
        PLANNER,
        ENRICHMENT,
        PLACES,
        orchestrator,
        BOOKING,
        EDITOR,
        EXPLAINER
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

