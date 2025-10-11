package com.tripplanner.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Agent capabilities for dynamic registration and task management.
 * Defines what tasks an agent can handle and its configuration.
 */
public class AgentCapabilities {
    
    @JsonProperty("supportedTasks")
    private List<String> supportedTasks;
    
    @JsonProperty("capabilities")
    private List<AgentCapability> capabilities;
    
    @JsonProperty("supportedDataSections")
    private List<String> supportedDataSections;
    
    @JsonProperty("priority")
    private int priority;
    
    @JsonProperty("enabled")
    private boolean enabled;
    
    @JsonProperty("configuration")
    private Map<String, Object> configuration;
    
    public AgentCapabilities() {
        this.supportedTasks = new ArrayList<>();
        this.supportedDataSections = new ArrayList<>();
        this.capabilities = new ArrayList<>();
        this.priority = 100; // Default priority (lower = higher priority)
        this.enabled = true; // Default enabled
        this.configuration = new HashMap<>();
    }
    
    public AgentCapabilities(List<String> supportedTasks, List<String> supportedDataSections, int priority) {
        this();
        this.supportedTasks = supportedTasks != null ? supportedTasks : new ArrayList<>();
        this.supportedDataSections = supportedDataSections != null ? supportedDataSections : new ArrayList<>();
        this.priority = priority;
    }
    
    // Getters and Setters
    public List<String> getSupportedTasks() {
        return supportedTasks;
    }
    
    public void setSupportedTasks(List<String> supportedTasks) {
        this.supportedTasks = supportedTasks;
    }
    
    public List<String> getSupportedDataSections() {
        return supportedDataSections;
    }
    
    public void setSupportedDataSections(List<String> supportedDataSections) {
        this.supportedDataSections = supportedDataSections;
    }
    
    public int getPriority() {
        return priority;
    }
    
    public void setPriority(int priority) {
        this.priority = priority;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public Map<String, Object> getConfiguration() {
        return configuration;
    }
    
    public void setConfiguration(Map<String, Object> configuration) {
        this.configuration = configuration;
    }
    
    /**
     * Check if this agent supports a specific task type.
     */
    public boolean supportsTask(String taskType) {
        return supportedTasks != null && supportedTasks.contains(taskType);
    }
    
    /**
     * Check if this agent supports a specific data section.
     */
    public boolean supportsDataSection(String dataSection) {
        return supportedDataSections != null && supportedDataSections.contains(dataSection);
    }
    
    /**
     * Add a supported task type.
     */
    public void addSupportedTask(String taskType) {
        if (supportedTasks == null) {
            supportedTasks = new ArrayList<>();
        }
        if (!supportedTasks.contains(taskType)) {
            supportedTasks.add(taskType);
        }
    }
    
    /**
     * Add a supported data section.
     */
    public void addSupportedDataSection(String dataSection) {
        if (supportedDataSections == null) {
            supportedDataSections = new ArrayList<>();
        }
        if (!supportedDataSections.contains(dataSection)) {
            supportedDataSections.add(dataSection);
        }
    }
    
    /**
     * Get agent capabilities using enum.
     */
    public List<AgentCapability> getCapabilities() {
        return capabilities;
    }
    
    /**
     * Set agent capabilities using enum.
     */
    public void setCapabilities(List<AgentCapability> capabilities) {
        this.capabilities = capabilities;
    }
    
    /**
     * Check if agent has a specific capability.
     */
    public boolean hasCapability(AgentCapability capability) {
        return capabilities != null && capabilities.contains(capability);
    }
    
    /**
     * Add a capability to this agent.
     */
    public void addCapability(AgentCapability capability) {
        if (capabilities == null) {
            capabilities = new ArrayList<>();
        }
        if (!capabilities.contains(capability)) {
            capabilities.add(capability);
        }
    }
    
    /**
     * Set a configuration value.
     */
    public void setConfigurationValue(String key, Object value) {
        if (configuration == null) {
            configuration = new HashMap<>();
        }
        configuration.put(key, value);
    }
    
    /**
     * Get a configuration value.
     */
    public Object getConfigurationValue(String key) {
        return configuration != null ? configuration.get(key) : null;
    }
    
    /**
     * Check if this agent is enabled for chat requests.
     * Default is true for backward compatibility.
     */
    public boolean isChatEnabled() {
        Object value = getConfigurationValue("chatEnabled");
        return value == null || (value instanceof Boolean && (Boolean) value);
    }
    
    /**
     * Set whether this agent handles chat requests.
     */
    public void setChatEnabled(boolean chatEnabled) {
        setConfigurationValue("chatEnabled", chatEnabled);
    }
    
    @Override
    public String toString() {
        return "AgentCapabilities{" +
                "supportedTasks=" + supportedTasks +
                ", supportedDataSections=" + supportedDataSections +
                ", priority=" + priority +
                ", enabled=" + enabled +
                ", chatEnabled=" + isChatEnabled() +
                ", configuration=" + configuration +
                '}';
    }
}