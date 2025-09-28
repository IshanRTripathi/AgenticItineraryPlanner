package com.tripplanner.service;

import com.tripplanner.agents.BaseAgent;
import com.tripplanner.dto.AgentEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for managing available agents at runtime.
 */
@Component
public class AgentRegistry {
    
    private static final Logger logger = LoggerFactory.getLogger(AgentRegistry.class);
    
    private final Map<AgentEvent.AgentKind, BaseAgent> registeredAgents = new ConcurrentHashMap<>();
    
    @Autowired
    public AgentRegistry(List<BaseAgent> agents) {
        logger.info("=== AGENT REGISTRY INITIALIZATION ===");
        logger.info("Found {} agents to register", agents.size());
        
        for (BaseAgent agent : agents) {
            // Get agent kind from the agent's class name or a method
            AgentEvent.AgentKind kind = getAgentKind(agent);
            registeredAgents.put(kind, agent);
            logger.info("Registered agent: {} -> {}", kind, agent.getClass().getSimpleName());
        }
        
        logger.info("Total registered agents: {}", registeredAgents.size());
        logger.info("=====================================");
    }
    
    /**
     * Get all registered agent kinds.
     */
    public Set<AgentEvent.AgentKind> getRegisteredAgentKinds() {
        return new HashSet<>(registeredAgents.keySet());
    }
    
    /**
     * Get agent by kind.
     */
    public Optional<BaseAgent> getAgent(AgentEvent.AgentKind kind) {
        return Optional.ofNullable(registeredAgents.get(kind));
    }
    
    /**
     * Check if agent is registered.
     */
    public boolean isAgentRegistered(AgentEvent.AgentKind kind) {
        return registeredAgents.containsKey(kind);
    }
    
    /**
     * Get agent kind from agent instance.
     * This uses reflection to get the agent kind from the BaseAgent.
     */
    private AgentEvent.AgentKind getAgentKind(BaseAgent agent) {
        // Use reflection to access the agentKind field from BaseAgent
        try {
            java.lang.reflect.Field field = BaseAgent.class.getDeclaredField("agentKind");
            field.setAccessible(true);
            return (AgentEvent.AgentKind) field.get(agent);
        } catch (Exception e) {
            logger.error("Failed to get agent kind for agent: {}", agent.getClass().getSimpleName(), e);
            // Fallback: try to determine from class name
            String className = agent.getClass().getSimpleName();
            if (className.contains("Planner")) return AgentEvent.AgentKind.planner;
            if (className.contains("Enrichment")) return AgentEvent.AgentKind.enrichment;
            if (className.contains("Places")) return AgentEvent.AgentKind.places;
            // Add more mappings as needed
            return AgentEvent.AgentKind.orchestrator; // Default fallback
        }
    }
}
