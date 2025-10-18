package com.tripplanner.service;

import com.tripplanner.agents.BaseAgent;
import com.tripplanner.dto.AgentCapabilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Coordinates agent selection and task routing based on agent capabilities and responsibilities.
 * Ensures that tasks are routed to the most appropriate agent while respecting boundaries.
 */
@Service
public class AgentCoordinator {
    
    private static final Logger logger = LoggerFactory.getLogger(AgentCoordinator.class);
    
    private final AgentRegistry agentRegistry;
    private final ItineraryJsonService itineraryJsonService;
    
    public AgentCoordinator(AgentRegistry agentRegistry, ItineraryJsonService itineraryJsonService) {
        this.agentRegistry = agentRegistry;
        this.itineraryJsonService = itineraryJsonService;
    }
    
    /**
     * Route a task to the most appropriate agent based on capabilities and context.
     * 
     * @param taskType The type of task to be performed
     * @param taskContext Additional context for task routing decisions
     * @return The best agent for the task, or null if no suitable agent found
     */
    public BaseAgent routeTask(String taskType, Object taskContext) {
        logger.debug("Routing task type: {} with context: {}", taskType, taskContext);
        
        // Get all agents that can handle this task type
        List<BaseAgent> candidateAgents = agentRegistry.getAgentsForTask(taskType);
        
        if (candidateAgents.isEmpty()) {
            logger.warn("No agents found for task type: {}", taskType);
            return null;
        }
        
        // Filter agents based on context-specific capabilities
        List<AgentCandidate> suitableAgents = candidateAgents.stream()
                .filter(agent -> agent.canHandle(taskType, taskContext))
                .map(agent -> new AgentCandidate(agent, calculateAgentScore(agent, taskType, taskContext)))
                .collect(Collectors.toList());
        
        if (suitableAgents.isEmpty()) {
            logger.warn("No suitable agents found for task type: {} with context", taskType);
            return null;
        }
        
        // Sort by score (highest first) and return the best agent
        suitableAgents.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
        
        BaseAgent selectedAgent = suitableAgents.get(0).getAgent();
        logger.info("Selected agent {} for task type: {} (score: {:.2f})", 
                   selectedAgent.getClass().getSimpleName(), taskType, suitableAgents.get(0).getScore());
        
        return selectedAgent;
    }
    
    /**
     * Get all agents that can handle a specific task type.
     * 
     * @param taskType The task type to check
     * @return List of agents that can handle the task
     */
    public List<BaseAgent> getCapableAgents(String taskType) {
        return agentRegistry.getAgentsForTask(taskType);
    }
    
    /**
     * Check if there are any conflicts between agents for a task type.
     * 
     * @param taskType The task type to check
     * @return ConflictAnalysis containing information about potential conflicts
     */
    public ConflictAnalysis analyzeAgentConflicts(String taskType) {
        List<BaseAgent> agents = getCapableAgents(taskType);
        
        if (agents.size() <= 1) {
            return new ConflictAnalysis(false, agents, new ArrayList<>());
        }
        
        List<String> conflicts = new ArrayList<>();
        
        // Check for overlapping responsibilities
        for (int i = 0; i < agents.size(); i++) {
            for (int j = i + 1; j < agents.size(); j++) {
                BaseAgent agent1 = agents.get(i);
                BaseAgent agent2 = agents.get(j);
                
                AgentCapabilities caps1 = agent1.getCapabilities();
                AgentCapabilities caps2 = agent2.getCapabilities();
                
                // Check for overlapping supported tasks
                Set<String> tasks1 = new HashSet<>(caps1.getSupportedTasks());
                Set<String> tasks2 = new HashSet<>(caps2.getSupportedTasks());
                tasks1.retainAll(tasks2);
                
                if (!tasks1.isEmpty()) {
                    conflicts.add(String.format("Agents %s and %s both support tasks: %s",
                                               agent1.getClass().getSimpleName(),
                                               agent2.getClass().getSimpleName(),
                                               tasks1));
                }
            }
        }
        
        return new ConflictAnalysis(!conflicts.isEmpty(), agents, conflicts);
    }
    
    /**
     * Validate that an agent can handle a specific task with context.
     * 
     * @param agent The agent to validate
     * @param taskType The task type
     * @param taskContext The task context
     * @return ValidationResult containing validation outcome and details
     */
    public ValidationResult validateAgentForTask(BaseAgent agent, String taskType, Object taskContext) {
        try {
            // Check basic capability
            if (!agent.canHandle(taskType)) {
                return new ValidationResult(false, 
                    String.format("Agent %s does not support task type: %s", 
                                agent.getClass().getSimpleName(), taskType));
            }
            
            // Check context-specific capability
            if (!agent.canHandle(taskType, taskContext)) {
                return new ValidationResult(false, 
                    String.format("Agent %s cannot handle task type %s with given context", 
                                agent.getClass().getSimpleName(), taskType));
            }
            
            // Check if agent is enabled
            AgentCapabilities capabilities = agent.getCapabilities();
            if (!capabilities.isEnabled()) {
                return new ValidationResult(false, 
                    String.format("Agent %s is currently disabled", 
                                agent.getClass().getSimpleName()));
            }
            
            return new ValidationResult(true, "Agent is suitable for the task");
            
        } catch (Exception e) {
            logger.error("Error validating agent {} for task {}", 
                        agent.getClass().getSimpleName(), taskType, e);
            return new ValidationResult(false, "Validation failed: " + e.getMessage());
        }
    }
    
    /**
     * Store agent result data flexibly for any agent type.
     */
    public void storeAgentResult(String itineraryId, String agentName, Object result) {
        logger.debug("Storing agent result for itinerary: {}, agent: {}", itineraryId, agentName);
        
        try {
            // This would typically interact with ItineraryJsonService
            // For now, log the operation
            logger.info("Agent {} stored result for itinerary {}: {}", 
                       agentName, itineraryId, result.getClass().getSimpleName());
            
            // Store via ItineraryJsonService
            itineraryJsonService.updateAgentData(itineraryId, agentName, result);
            
        } catch (Exception e) {
            logger.error("Failed to store agent result for itinerary: {}, agent: {}", 
                        itineraryId, agentName, e);
            throw new RuntimeException("Failed to store agent result", e);
        }
    }
    
    /**
     * Retrieve agent data with optional type conversion.
     */
    public <T> Optional<T> getAgentData(String itineraryId, String agentName, Class<T> type) {
        logger.debug("Retrieving agent data for itinerary: {}, agent: {}, type: {}", 
                    itineraryId, agentName, type.getSimpleName());
        
        try {
            // Retrieve via ItineraryJsonService
            return itineraryJsonService.getAgentData(itineraryId, agentName, type);
            
        } catch (Exception e) {
            logger.error("Failed to retrieve agent data for itinerary: {}, agent: {}", 
                        itineraryId, agentName, e);
            return Optional.empty();
        }
    }
    
    /**
     * Check if agent data exists for a specific agent.
     */
    public boolean hasAgentData(String itineraryId, String agentName) {
        logger.debug("Checking agent data existence for itinerary: {}, agent: {}", itineraryId, agentName);
        
        try {
            // Check via ItineraryJsonService
            return itineraryJsonService.hasAgentData(itineraryId, agentName);
            
        } catch (Exception e) {
            logger.error("Failed to check agent data existence for itinerary: {}, agent: {}", 
                        itineraryId, agentName, e);
            return false;
        }
    }
    
    /**
     * Get agent coordination statistics.
     */
    public CoordinationStats getStats() {
        Map<String, AgentCapabilities> allCapabilities = agentRegistry.getAllCapabilities();
        
        int totalAgents = allCapabilities.size();
        int enabledAgents = (int) allCapabilities.values().stream()
                .filter(AgentCapabilities::isEnabled)
                .count();
        
        // Count task type coverage
        Set<String> allSupportedTasks = allCapabilities.values().stream()
                .flatMap(caps -> caps.getSupportedTasks().stream())
                .collect(Collectors.toSet());
        
        // Find task types with multiple agents
        Map<String, Integer> taskCoverage = new HashMap<>();
        for (String taskType : allSupportedTasks) {
            int agentCount = (int) allCapabilities.values().stream()
                    .filter(caps -> caps.supportsTask(taskType))
                    .count();
            taskCoverage.put(taskType, agentCount);
        }
        
        return new CoordinationStats(totalAgents, enabledAgents, allSupportedTasks.size(), taskCoverage);
    }
    
    /**
     * Calculate a score for how well an agent fits a task.
     */
    private double calculateAgentScore(BaseAgent agent, String taskType, Object taskContext) {
        AgentCapabilities capabilities = agent.getCapabilities();
        double score = 0.0;
        
        // Base score from priority (lower priority = higher score)
        score += Math.max(0, 100 - capabilities.getPriority());
        
        // Bonus for exact task match
        if (capabilities.getSupportedTasks().contains(taskType)) {
            score += 50;
        }
        
        // Context-specific scoring
        if (taskContext instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> context = (Map<String, Object>) taskContext;
            
            // Bonus for matching initiator preference
            String initiator = (String) context.get("initiator");
            if ("user".equals(initiator)) {
                // Prefer agents that handle user requests
                Object handlesUserRequests = capabilities.getConfigurationValue("handlesUserRequests");
                if (Boolean.TRUE.equals(handlesUserRequests)) {
                    score += 30;
                }
            }
            
            // Bonus for matching operation type
            String operation = (String) context.get("operation");
            if (operation != null) {
                for (String supportedTask : capabilities.getSupportedTasks()) {
                    if (operation.contains(supportedTask)) {
                        score += 20;
                        break;
                    }
                }
            }
        }
        
        // Penalty for disabled agents
        if (!capabilities.isEnabled()) {
            score -= 1000;
        }
        
        return score;
    }
    
    // Result classes
    
    private static class AgentCandidate {
        private final BaseAgent agent;
        private final double score;
        
        public AgentCandidate(BaseAgent agent, double score) {
            this.agent = agent;
            this.score = score;
        }
        
        public BaseAgent getAgent() { return agent; }
        public double getScore() { return score; }
    }
    
    public static class ConflictAnalysis {
        private final boolean hasConflicts;
        private final List<BaseAgent> agents;
        private final List<String> conflicts;
        
        public ConflictAnalysis(boolean hasConflicts, List<BaseAgent> agents, List<String> conflicts) {
            this.hasConflicts = hasConflicts;
            this.agents = agents;
            this.conflicts = conflicts;
        }
        
        public boolean hasConflicts() { return hasConflicts; }
        public List<BaseAgent> getAgents() { return agents; }
        public List<String> getConflicts() { return conflicts; }
        
        @Override
        public String toString() {
            return "ConflictAnalysis{" +
                    "hasConflicts=" + hasConflicts +
                    ", agents=" + agents.size() +
                    ", conflicts=" + conflicts.size() +
                    '}';
        }
    }
    
    public static class ValidationResult {
        private final boolean valid;
        private final String message;
        
        public ValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }
        
        public boolean isValid() { return valid; }
        public String getMessage() { return message; }
        
        @Override
        public String toString() {
            return "ValidationResult{" +
                    "valid=" + valid +
                    ", message='" + message + '\'' +
                    '}';
        }
    }
    
    public static class CoordinationStats {
        private final int totalAgents;
        private final int enabledAgents;
        private final int supportedTaskTypes;
        private final Map<String, Integer> taskCoverage;
        
        public CoordinationStats(int totalAgents, int enabledAgents, int supportedTaskTypes,
                               Map<String, Integer> taskCoverage) {
            this.totalAgents = totalAgents;
            this.enabledAgents = enabledAgents;
            this.supportedTaskTypes = supportedTaskTypes;
            this.taskCoverage = taskCoverage;
        }
        
        public int getTotalAgents() { return totalAgents; }
        public int getEnabledAgents() { return enabledAgents; }
        public int getSupportedTaskTypes() { return supportedTaskTypes; }
        public Map<String, Integer> getTaskCoverage() { return taskCoverage; }
        
        public List<String> getOverlappingTasks() {
            return taskCoverage.entrySet().stream()
                    .filter(entry -> entry.getValue() > 1)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());
        }
        
        @Override
        public String toString() {
            return "CoordinationStats{" +
                    "totalAgents=" + totalAgents +
                    ", enabledAgents=" + enabledAgents +
                    ", supportedTaskTypes=" + supportedTaskTypes +
                    ", overlappingTasks=" + getOverlappingTasks().size() +
                    '}';
        }
    }
}