package com.tripplanner.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

/**
 * Flexible agent data storage that can accommodate any agent type and structure.
 * Stores agent data as generic objects to prevent blocking on schema changes.
 * 
 * Benefits:
 * - Agents can store any JSON structure without backend changes
 * - No blocking if agents return unexpected fields
 * - Schema evolution without code updates
 * - Direct JSON storage with no serialization overhead
 */
public class AgentDataSection {
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    @JsonProperty("data")
    private Map<String, Object> data;
    
    public AgentDataSection() {
        this.data = new HashMap<>();
    }
    
    /**
     * Get raw agent data for any agent type.
     */
    public Object getAgentData(String agentName) {
        return data != null ? data.get(agentName) : null;
    }
    
    /**
     * Set agent data for any agent type.
     * Accepts any object - will be stored as-is.
     */
    public void setAgentData(String agentName, Object agentData) {
        if (data == null) {
            data = new HashMap<>();
        }
        data.put(agentName, agentData);
    }
    
    /**
     * Get typed agent data with automatic conversion.
     * Returns null if agent data doesn't exist or conversion fails.
     */
    @JsonIgnore
    public <T> T getAgentData(String agentName, Class<T> targetType) {
        Object rawData = getAgentData(agentName);
        if (rawData == null) {
            return null;
        }
        
        try {
            if (targetType.isInstance(rawData)) {
                return targetType.cast(rawData);
            }
            
            // Convert using ObjectMapper for complex objects
            return objectMapper.convertValue(rawData, targetType);
        } catch (Exception e) {
            // Log warning but don't fail - return null for graceful degradation
            System.err.println("Warning: Failed to convert agent data for " + agentName + " to " + targetType.getSimpleName() + ": " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Check if agent data exists for a specific agent.
     */
    @JsonIgnore
    public boolean hasAgentData(String agentName) {
        return data != null && data.containsKey(agentName);
    }
    
    /**
     * Remove agent data for a specific agent.
     */
    @JsonIgnore
    public void removeAgentData(String agentName) {
        if (data != null) {
            data.remove(agentName);
        }
    }
    
    /**
     * Get all agent names that have data.
     */
    @JsonIgnore
    public java.util.Set<String> getAgentNames() {
        return data != null ? data.keySet() : java.util.Collections.emptySet();
    }
    
    /**
     * Get the raw data map (for serialization).
     */
    public Map<String, Object> getData() {
        return data;
    }
    
    /**
     * Set the raw data map (for deserialization).
     */
    public void setData(Map<String, Object> data) {
        this.data = data;
    }
    

    
    @Override
    public String toString() {
        return "AgentDataSection{" +
                "data=" + data +
                '}';
    }
}