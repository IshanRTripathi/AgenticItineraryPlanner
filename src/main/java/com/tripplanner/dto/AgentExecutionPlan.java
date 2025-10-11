package com.tripplanner.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tripplanner.agents.BaseAgent;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Agent execution plan for coordinating multiple agents on a task.
 * Contains the task type, ordered list of agents, and execution context.
 */
public class AgentExecutionPlan {
    
    @JsonProperty("taskType")
    private String taskType;
    
    @JsonProperty("agents")
    private List<BaseAgent> agents;
    
    @JsonProperty("context")
    private NormalizedItinerary context;
    
    @JsonProperty("parameters")
    private Map<String, Object> parameters;
    
    @JsonProperty("executionOrder")
    private List<String> executionOrder;
    
    @JsonProperty("fallbackEnabled")
    private boolean fallbackEnabled;
    
    @JsonProperty("maxRetries")
    private int maxRetries;
    
    @JsonProperty("timeoutMs")
    private long timeoutMs;
    
    public AgentExecutionPlan() {
        this.agents = new ArrayList<>();
        this.parameters = new HashMap<>();
        this.executionOrder = new ArrayList<>();
        this.fallbackEnabled = true;
        this.maxRetries = 3;
        this.timeoutMs = 30000; // 30 seconds default
    }
    
    public AgentExecutionPlan(String taskType, List<BaseAgent> agents, NormalizedItinerary context) {
        this();
        this.taskType = taskType;
        this.agents = agents != null ? agents : new ArrayList<>();
        this.context = context;
        
        // Build execution order from agent priorities
        buildExecutionOrder();
    }
    
    // Getters and Setters
    public String getTaskType() {
        return taskType;
    }
    
    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }
    
    public List<BaseAgent> getAgents() {
        return agents;
    }
    
    public void setAgents(List<BaseAgent> agents) {
        this.agents = agents;
        buildExecutionOrder();
    }
    
    public NormalizedItinerary getContext() {
        return context;
    }
    
    public void setContext(NormalizedItinerary context) {
        this.context = context;
    }
    
    public Map<String, Object> getParameters() {
        return parameters;
    }
    
    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }
    
    public List<String> getExecutionOrder() {
        return executionOrder;
    }
    
    public void setExecutionOrder(List<String> executionOrder) {
        this.executionOrder = executionOrder;
    }
    
    public boolean isFallbackEnabled() {
        return fallbackEnabled;
    }
    
    public void setFallbackEnabled(boolean fallbackEnabled) {
        this.fallbackEnabled = fallbackEnabled;
    }
    
    public int getMaxRetries() {
        return maxRetries;
    }
    
    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }
    
    public long getTimeoutMs() {
        return timeoutMs;
    }
    
    public void setTimeoutMs(long timeoutMs) {
        this.timeoutMs = timeoutMs;
    }
    
    /**
     * Add a parameter to the execution plan.
     */
    public void addParameter(String key, Object value) {
        if (parameters == null) {
            parameters = new HashMap<>();
        }
        parameters.put(key, value);
    }
    
    /**
     * Get a parameter value.
     */
    public Object getParameter(String key) {
        return parameters != null ? parameters.get(key) : null;
    }
    
    /**
     * Get a parameter value with type casting.
     */
    @SuppressWarnings("unchecked")
    public <T> T getParameter(String key, Class<T> type) {
        Object value = getParameter(key);
        if (value != null && type.isAssignableFrom(value.getClass())) {
            return (T) value;
        }
        return null;
    }
    
    /**
     * Check if the plan has any agents.
     */
    public boolean hasAgents() {
        return agents != null && !agents.isEmpty();
    }
    
    /**
     * Get the primary (first) agent in the execution plan.
     */
    public BaseAgent getPrimaryAgent() {
        return hasAgents() ? agents.get(0) : null;
    }
    
    /**
     * Get fallback agents (all except the first).
     */
    public List<BaseAgent> getFallbackAgents() {
        if (!hasAgents() || agents.size() <= 1) {
            return new ArrayList<>();
        }
        return new ArrayList<>(agents.subList(1, agents.size()));
    }
    
    /**
     * Build execution order based on agent priorities and types.
     */
    private void buildExecutionOrder() {
        executionOrder.clear();
        if (agents != null) {
            for (BaseAgent agent : agents) {
                executionOrder.add(agent.getClass().getSimpleName());
            }
        }
    }
    
    /**
     * Validate the execution plan.
     */
    public boolean isValid() {
        return taskType != null && !taskType.trim().isEmpty() && hasAgents();
    }
    
    /**
     * Get execution summary for logging.
     */
    public String getExecutionSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("Task: ").append(taskType);
        summary.append(", Agents: ").append(agents != null ? agents.size() : 0);
        summary.append(", Fallback: ").append(fallbackEnabled);
        summary.append(", MaxRetries: ").append(maxRetries);
        summary.append(", Timeout: ").append(timeoutMs).append("ms");
        return summary.toString();
    }
    
    @Override
    public String toString() {
        return "AgentExecutionPlan{" +
                "taskType='" + taskType + '\'' +
                ", agents=" + (agents != null ? agents.size() : 0) + " agents" +
                ", context=" + (context != null ? context.getItineraryId() : "null") +
                ", parameters=" + (parameters != null ? parameters.size() : 0) + " params" +
                ", executionOrder=" + executionOrder +
                ", fallbackEnabled=" + fallbackEnabled +
                ", maxRetries=" + maxRetries +
                ", timeoutMs=" + timeoutMs +
                '}';
    }
}