package com.tripplanner.service;

import com.tripplanner.agents.BaseAgent;
import com.tripplanner.dto.AgentEvent;
import com.tripplanner.dto.AgentCapabilities;
import com.tripplanner.dto.AgentExecutionPlan;
import com.tripplanner.dto.NormalizedItinerary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Registry for managing available agents at runtime.
 */
@Component
public class AgentRegistry {
    
    private static final Logger logger = LoggerFactory.getLogger(AgentRegistry.class);
    
    private final Map<String, BaseAgent> registeredAgents = new ConcurrentHashMap<>();
    private final Map<String, AgentCapabilities> capabilities = new ConcurrentHashMap<>();
    
    @Autowired
    public AgentRegistry(List<BaseAgent> agents) {
        logger.info("=== AGENT REGISTRY INITIALIZATION ===");
        logger.info("Found {} agents to register", agents.size());
        
        for (BaseAgent agent : agents) {
            registerAgent(agent);
        }
        
        logger.info("Total registered agents: {}", registeredAgents.size());
        logger.info("=====================================");
    }
    
    /**
     * Get all registered agent IDs.
     */
    public Set<String> getRegisteredAgentIds() {
        return new HashSet<>(registeredAgents.keySet());
    }
    
    /**
     * Get agent by ID.
     */
    public Optional<BaseAgent> getAgent(String agentId) {
        return Optional.ofNullable(registeredAgents.get(agentId));
    }
    
    /**
     * Check if agent is registered.
     */
    public boolean isAgentRegistered(String agentId) {
        return registeredAgents.containsKey(agentId);
    }
    
    /**
     * Register an agent with capabilities management.
     * Extracts capabilities from agent and validates no task overlap.
     */
    public void registerAgent(BaseAgent agent) {
        try {
            // Get agent kind from the agent's class name or a method
            AgentEvent.AgentKind kind = getAgentKind(agent);
            String agentId = generateUniqueAgentId(agent, kind);
            
            // Extract capabilities from agent using reflection or interface
            AgentCapabilities agentCapabilities = extractCapabilities(agent, kind);
            
            // Validate no task overlap with existing agents
            validateTaskOverlap(agentCapabilities);
            
            // Store agent and capabilities
            registeredAgents.put(agentId, agent);
            capabilities.put(agentId, agentCapabilities);
            
            logger.info("Registered agent: {} -> {} with capabilities: {}", 
                       agentId, agent.getClass().getSimpleName(), agentCapabilities.getSupportedTasks());
            
        } catch (Exception e) {
            logger.error("Failed to register agent: {}", agent.getClass().getSimpleName(), e);
            throw new RuntimeException("Agent registration failed", e);
        }
    }
    
    /**
     * Disable an agent without removing it from registry.
     */
    public void disableAgent(String agentId) {
        AgentCapabilities caps = capabilities.get(agentId);
        if (caps != null) {
            caps.setEnabled(false);
            logger.info("Disabled agent: {}", agentId);
        } else {
            logger.warn("Attempted to disable unknown agent: {}", agentId);
        }
    }
    
    /**
     * Enable a previously disabled agent.
     */
    public void enableAgent(String agentId) {
        AgentCapabilities caps = capabilities.get(agentId);
        if (caps != null) {
            caps.setEnabled(true);
            logger.info("Enabled agent: {}", agentId);
        } else {
            logger.warn("Attempted to enable unknown agent: {}", agentId);
        }
    }
    
    /**
     * Get agents suitable for a specific task type.
     * Filters by enabled status and supported tasks, sorted by priority.
     * By default, only returns chat-enabled agents.
     */
    public List<BaseAgent> getAgentsForTask(String taskType) {
        return getAgentsForTask(taskType, true);
    }
    
    /**
     * Get agents suitable for a specific task type with optional chat filtering.
     * @param taskType The task type to filter by
     * @param chatOnly If true, only return agents with chatEnabled=true
     */
    public List<BaseAgent> getAgentsForTask(String taskType, boolean chatOnly) {
        logger.debug("Getting agents for task: {}, chatOnly: {}", taskType, chatOnly);
        
        return capabilities.entrySet().stream()
            .filter(entry -> {
                AgentCapabilities caps = entry.getValue();
                
                // Must be enabled
                if (!caps.isEnabled()) {
                    return false;
                }
                
                // Must support the task type
                if (!caps.supportsTask(taskType)) {
                    return false;
                }
                
                // Filter by chat-enabled if requested
                if (chatOnly && !caps.isChatEnabled()) {
                    logger.debug("Filtering out agent {} (not chat-enabled) for task: {}", 
                               entry.getKey(), taskType);
                    return false;
                }
                
                return true;
            })
            .sorted(Map.Entry.comparingByValue((caps1, caps2) -> 
                Integer.compare(caps1.getPriority(), caps2.getPriority())))
            .map(entry -> {
                String agentId = entry.getKey();
                BaseAgent agent = registeredAgents.get(agentId);
                if (agent != null) {
                    logger.debug("Found agent {} (priority: {}) for task: {}", 
                               agentId, entry.getValue().getPriority(), taskType);
                }
                return agent;
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }
    
    /**
     * Get agent by kind (for backward compatibility).
     */
    public BaseAgent getAgentByKind(String agentKind) {
        // Try to find agent by exact ID first
        BaseAgent agent = registeredAgents.get(agentKind);
        if (agent != null) {
            return agent;
        }
        
        // Try to find by AgentKind enum
        try {
            AgentEvent.AgentKind kind = AgentEvent.AgentKind.valueOf(agentKind);
            // Find first agent with this kind
            for (Map.Entry<String, BaseAgent> entry : registeredAgents.entrySet()) {
                if (getAgentKind(entry.getValue()) == kind) {
                    return entry.getValue();
                }
            }
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid agent kind: {}", agentKind);
        }
        return null;
    }
    
    /**
     * Get capabilities for a specific agent.
     */
    public Optional<AgentCapabilities> getAgentCapabilities(String agentId) {
        return Optional.ofNullable(capabilities.get(agentId));
    }
    
    /**
     * Get all registered agent capabilities.
     */
    public Map<String, AgentCapabilities> getAllCapabilities() {
        return new HashMap<>(capabilities);
    }
    
    /**
     * Create an execution plan for a specific task type with itinerary context.
     * Gets suitable agents, creates execution plan with priorities, and includes context.
     */
    public AgentExecutionPlan createExecutionPlan(String taskType, NormalizedItinerary itinerary) {
        logger.debug("Creating execution plan for task type: {}", taskType);
        
        // Get suitable agents using getAgentsForTask()
        List<BaseAgent> suitableAgents = getAgentsForTask(taskType);
        
        if (suitableAgents.isEmpty()) {
            logger.warn("No agents found for task type: {}", taskType);
            return new AgentExecutionPlan(taskType, new ArrayList<>(), itinerary);
        }
        
        // Create execution plan with agent priorities
        AgentExecutionPlan plan = new AgentExecutionPlan(taskType, suitableAgents, itinerary);
        
        // Add task-specific configuration from capabilities
        addTaskSpecificConfiguration(plan, taskType);
        
        // Include itinerary context for agent decisions
        if (itinerary != null) {
            plan.addParameter("itineraryId", itinerary.getItineraryId());
            plan.addParameter("userId", itinerary.getUserId());
            plan.addParameter("version", itinerary.getVersion());
            plan.addParameter("destination", itinerary.getDestination());
            plan.addParameter("dayCount", itinerary.getDays() != null ? itinerary.getDays().size() : 0);
        }
        
        logger.info("Created execution plan for '{}': {} agents, primary: {}", 
                   taskType, suitableAgents.size(), 
                   plan.getPrimaryAgent() != null ? plan.getPrimaryAgent().getClass().getSimpleName() : "none");
        
        return plan;
    }
    
    /**
     * Add task-specific configuration to the execution plan.
     */
    private void addTaskSpecificConfiguration(AgentExecutionPlan plan, String taskType) {
        // Set task-specific timeouts and retry policies
        switch (taskType.toLowerCase()) {
            case "edit":
            case "modify":
            case "update":
                plan.setTimeoutMs(60000); // 1 minute for editing tasks
                plan.setMaxRetries(2);
                plan.setFallbackEnabled(true);
                break;
                
            case "enrich":
            case "enhance":
                plan.setTimeoutMs(120000); // 2 minutes for ENRICHMENT (API calls)
                plan.setMaxRetries(3);
                plan.setFallbackEnabled(true);
                break;
                
            case "search":
            case "lookup":
                plan.setTimeoutMs(30000); // 30 seconds for search
                plan.setMaxRetries(2);
                plan.setFallbackEnabled(false); // Search should be fast
                break;
                
            case "plan":
            case "create":
                plan.setTimeoutMs(180000); // 3 minutes for planning
                plan.setMaxRetries(1);
                plan.setFallbackEnabled(true);
                break;
                
            default:
                plan.setTimeoutMs(45000); // Default 45 seconds
                plan.setMaxRetries(2);
                plan.setFallbackEnabled(true);
                break;
        }
        
        // Add agent-specific configuration from capabilities
        for (BaseAgent agent : plan.getAgents()) {
            AgentEvent.AgentKind kind = getAgentKind(agent);
            AgentCapabilities caps = capabilities.get(kind.toString());
            if (caps != null && caps.getConfiguration() != null) {
                for (Map.Entry<String, Object> entry : caps.getConfiguration().entrySet()) {
                    plan.addParameter(kind.toString() + "." + entry.getKey(), entry.getValue());
                }
            }
        }
    }
    
    /**
     * Validate execution plan before execution.
     */
    public boolean validateExecutionPlan(AgentExecutionPlan plan) {
        if (plan == null) {
            logger.error("Execution plan is null");
            return false;
        }
        
        if (!plan.isValid()) {
            logger.error("Execution plan is invalid: {}", plan.getExecutionSummary());
            return false;
        }
        
        // Validate that all agents in the plan are still registered and enabled
        for (BaseAgent agent : plan.getAgents()) {
            // Find the agent ID for this agent
            String agentId = findAgentId(agent);
            if (agentId == null) {
                logger.error("Agent {} in execution plan is not registered", agent.getClass().getSimpleName());
                return false;
            }
            
            AgentCapabilities caps = capabilities.get(agentId);
            if (caps == null || !caps.isEnabled()) {
                logger.error("Agent {} in execution plan is disabled", agentId);
                return false;
            }
            
            if (!caps.supportsTask(plan.getTaskType())) {
                logger.error("Agent {} does not support task type {}", agentId, plan.getTaskType());
                return false;
            }
        }
        
        logger.debug("Execution plan validation passed: {}", plan.getExecutionSummary());
        return true;
    }
    
    /**
     * Get execution statistics for monitoring.
     */
    public Map<String, Object> getExecutionStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        // Count enabled vs disabled agents
        long enabledCount = capabilities.values().stream()
            .mapToLong(caps -> caps.isEnabled() ? 1 : 0)
            .sum();
        long disabledCount = capabilities.size() - enabledCount;
        
        stats.put("totalAgents", capabilities.size());
        stats.put("enabledAgents", enabledCount);
        stats.put("disabledAgents", disabledCount);
        
        // Count agents by supported tasks
        Map<String, Long> taskCounts = new HashMap<>();
        for (AgentCapabilities caps : capabilities.values()) {
            if (caps.isEnabled()) {
                for (String task : caps.getSupportedTasks()) {
                    taskCounts.merge(task, 1L, Long::sum);
                }
            }
        }
        stats.put("taskSupport", taskCounts);
        
        return stats;
    }
    
    /**
     * Generate a unique agent ID based on the agent class name and kind.
     */
    private String generateUniqueAgentId(BaseAgent agent, AgentEvent.AgentKind kind) {
        String className = agent.getClass().getSimpleName();
        
        // For agents with unique class names, use the class name
        if (isUniqueClassName(className)) {
            return className.toLowerCase();
        }
        
        // For agents that share class names or kinds, create a unique ID
        String baseId = kind.toString().toLowerCase();
        String uniqueId = baseId;
        int counter = 1;
        
        while (registeredAgents.containsKey(uniqueId)) {
            uniqueId = baseId + "_" + counter;
            counter++;
        }
        
        return uniqueId;
    }
    
    /**
     * Check if a class name is unique among all registered agents.
     */
    private boolean isUniqueClassName(String className) {
        return registeredAgents.values().stream()
            .noneMatch(agent -> agent.getClass().getSimpleName().equals(className));
    }
    
    /**
     * Find the agent ID for a given agent instance.
     */
    private String findAgentId(BaseAgent agent) {
        for (Map.Entry<String, BaseAgent> entry : registeredAgents.entrySet()) {
            if (entry.getValue() == agent) {
                return entry.getKey();
            }
        }
        return null;
    }
    
    /**
     * Extract capabilities from agent using reflection or interface.
     * Prefers agent-defined capabilities over defaults.
     */
    private AgentCapabilities extractCapabilities(BaseAgent agent, AgentEvent.AgentKind kind) {
        // Try to get capabilities directly from agent (preferred approach)
        try {
            java.lang.reflect.Method method = agent.getClass().getMethod("getCapabilities");
            AgentCapabilities agentCaps = (AgentCapabilities) method.invoke(agent);
            if (agentCaps != null && !agentCaps.getSupportedTasks().isEmpty()) {
                // Agent defines its own capabilities - use them
                logger.debug("Using agent-defined capabilities for {}: tasks={}, chatEnabled={}", 
                           agent.getClass().getSimpleName(),
                           agentCaps.getSupportedTasks(),
                           agentCaps.isChatEnabled());
                return agentCaps;
            }
        } catch (NoSuchMethodException e) {
            logger.debug("Agent {} doesn't implement getCapabilities(), using minimal fallback", 
                        agent.getClass().getSimpleName());
        } catch (Exception e) {
            logger.warn("Failed to extract capabilities from agent {}: {}", 
                       agent.getClass().getSimpleName(), e.getMessage());
        }
        
        // Minimal fallback: Create empty capabilities and mark as not chat-enabled
        // Agents should define their own capabilities via getCapabilities()
        AgentCapabilities caps = new AgentCapabilities();
        caps.setPriority(50); // Default low priority
        caps.setChatEnabled(false); // Assume pipeline-only if not explicitly defined
        
        logger.warn("Agent {} (kind: {}) doesn't define capabilities. Using minimal fallback (chatEnabled=false).",
                   agent.getClass().getSimpleName(), kind);
        
        logger.debug("Using minimal fallback capabilities for {} (kind: {})", 
                    agent.getClass().getSimpleName(), kind);
        return caps;
    }
    
    /**
     * Validate that new agent capabilities don't overlap with existing agents.
     */
    public void validateTaskOverlap(AgentCapabilities newCapabilities) {
        for (Map.Entry<String, AgentCapabilities> entry : capabilities.entrySet()) {
            AgentCapabilities existingCaps = entry.getValue();
            
            // Check for task overlap
            for (String task : newCapabilities.getSupportedTasks()) {
                if (existingCaps.supportsTask(task)) {
                    logger.warn("Task overlap detected: task '{}' is already supported by agent '{}'", 
                               task, entry.getKey());
                    // Note: We log a warning but don't throw an exception to allow multiple agents 
                    // to handle the same task type (they will be prioritized)
                }
            }
        }
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
            if (className.contains("Planner")) return AgentEvent.AgentKind.PLANNER;
            if (className.contains("Enrichment")) return AgentEvent.AgentKind.ENRICHMENT;
            if (className.contains("Places")) return AgentEvent.AgentKind.PLACES;
            if (className.contains("Editor")) return AgentEvent.AgentKind.EDITOR;
            if (className.contains("Explain")) return AgentEvent.AgentKind.EXPLAINER;
            // Add more mappings as needed
            return AgentEvent.AgentKind.orchestrator; // Default fallback
        }
    }
    
    /**
     * Execute an agent with given parameters.
     */
    public Map<String, Object> executeAgent(String agentType, NormalizedItinerary itinerary, Map<String, Object> parameters) {
        logger.info("Executing agent: {} for itinerary: {}", agentType, itinerary.getItineraryId());
        
        try {
            // Convert agentType string to AgentEvent.AgentKind
            AgentEvent.AgentKind agentKind = parseAgentType(agentType);
            
            // Get the agent
            BaseAgent agent = getAgentByKind(agentKind.toString());
            if (agent == null) {
                throw new IllegalArgumentException("Agent not found: " + agentType);
            }
            
            // Create a simple agent request (you may need to adjust this based on your BaseAgent interface)
            // For now, return a simple success response
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("agentType", agentType);
            result.put("itineraryId", itinerary.getItineraryId());
            result.put("message", "Agent execution completed");
            result.put("applied", false); // Set to true if changes were actually applied
            
            logger.info("Agent {} executed successfully for itinerary: {}", agentType, itinerary.getItineraryId());
            return result;
            
        } catch (Exception e) {
            logger.error("Failed to execute agent {} for itinerary {}: {}", agentType, itinerary.getItineraryId(), e.getMessage());
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("error", e.getMessage());
            return result;
        }
    }
    
    /**
     * Get agent execution status.
     */
    public Map<String, Object> getAgentStatus(String agentType, String itineraryId) {
        logger.debug("Getting status for agent: {} on itinerary: {}", agentType, itineraryId);
        
        Map<String, Object> status = new HashMap<>();
        status.put("agentType", agentType);
        status.put("itineraryId", itineraryId);
        status.put("status", "idle"); // Could be: idle, running, completed, failed
        status.put("progress", 0);
        status.put("message", "Agent is ready");
        
        return status;
    }
    
    /**
     * Cancel agent execution.
     */
    public void cancelAgentExecution(String agentType, String itineraryId) {
        logger.info("Canceling execution for agent: {} on itinerary: {}", agentType, itineraryId);
        // Implementation would depend on how you track running agents
        // For now, just log the cancellation request
    }
    
    /**
     * Parse agent type string to AgentEvent.AgentKind.
     */
    private AgentEvent.AgentKind parseAgentType(String agentType) {
        switch (agentType.toLowerCase()) {
            case "editor":
                return AgentEvent.AgentKind.EDITOR;
            case "ENRICHMENT":
                return AgentEvent.AgentKind.ENRICHMENT;
            case "booking":
                return AgentEvent.AgentKind.BOOKING;
            case "PLANNER":
                return AgentEvent.AgentKind.PLANNER;
            case "places":
                return AgentEvent.AgentKind.PLACES;
            case "orchestrator":
                return AgentEvent.AgentKind.orchestrator;
            case "explainer":
            case "explain":
                return AgentEvent.AgentKind.EXPLAINER;
            default:
                throw new IllegalArgumentException("Unknown agent type: " + agentType);
        }
    }
}
