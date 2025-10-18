package com.tripplanner.dto;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Result of intent classification containing structured information about the user's intent.
 */
public class IntentResult {
    
    private String intent; // "REPLAN_TODAY" | "MOVE_TIME" | "INSERT_PLACE" | "DELETE_NODE" | "REPLACE_NODE" | "BOOK_NODE" | "UNDO" | "EXPLAIN"
    private String taskType; // "editing" | "booking" | "ENRICHMENT" | "explanation"
    private Integer day;
    private List<String> nodeIds;
    private Map<String, Object> entities; // Changed from Map<String, String> to Map<String, Object>
    private Map<String, Object> constraints;
    private Double confidence;
    
    // Constructors
    public IntentResult() {}
    
    public IntentResult(String intent, Integer day, List<String> nodeIds, 
                       Map<String, Object> entities, Map<String, Object> constraints) {
        this.intent = intent;
        this.day = day;
        this.nodeIds = nodeIds;
        this.entities = entities;
        this.constraints = constraints;
        this.taskType = mapIntentToTaskType(intent);
    }
    
    // Static factory methods for common intents
    public static IntentResult replanToday(Integer day, Map<String, Object> entities) {
        return new IntentResult("REPLAN_TODAY", day, List.of(), entities, Map.of());
    }
    
    public static IntentResult moveTime(List<String> nodeIds, Map<String, Object> entities) {
        return new IntentResult("MOVE_TIME", null, nodeIds, entities, Map.of());
    }
    
    public static IntentResult insertPlace(Integer day, Map<String, Object> entities) {
        return new IntentResult("INSERT_PLACE", day, List.of(), entities, Map.of());
    }
    
    public static IntentResult deleteNode(List<String> nodeIds) {
        return new IntentResult("DELETE_NODE", null, nodeIds, Map.of(), Map.of());
    }
    
    public static IntentResult replaceNode(List<String> nodeIds, Map<String, Object> entities) {
        return new IntentResult("REPLACE_NODE", null, nodeIds, entities, Map.of());
    }
    
    public static IntentResult bookNode(List<String> nodeIds) {
        return new IntentResult("BOOK_NODE", null, nodeIds, Map.of(), Map.of());
    }
    
    public static IntentResult undo(Integer toVersion) {
        Map<String, Object> constraints = new HashMap<>();
        if (toVersion != null) {
            constraints.put("toVersion", toVersion);
        }
        return new IntentResult("UNDO", null, List.of(), Map.of(), constraints);
    }
    
    public static IntentResult explain() {
        return new IntentResult("EXPLAIN", null, List.of(), Map.of(), Map.of());
    }
    
    // Getters and Setters
    public String getIntent() {
        return intent;
    }
    
    public void setIntent(String intent) {
        this.intent = intent;
        this.taskType = mapIntentToTaskType(intent);
    }
    
    public String getTaskType() {
        return taskType;
    }
    
    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }
    
    public Integer getDay() {
        return day;
    }
    
    public void setDay(Integer day) {
        this.day = day;
    }
    
    public List<String> getNodeIds() {
        return nodeIds;
    }
    
    public void setNodeIds(List<String> nodeIds) {
        this.nodeIds = nodeIds;
    }
    
    public Map<String, Object> getEntities() {
        return entities;
    }
    
    public void setEntities(Map<String, Object> entities) {
        this.entities = entities;
    }
    
    public Map<String, Object> getConstraints() {
        return constraints;
    }
    
    public void setConstraints(Map<String, Object> constraints) {
        this.constraints = constraints;
    }
    
    public Double getConfidence() {
        return confidence;
    }
    
    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }
    
    // Helper methods
    public boolean isChangeIntent() {
        return intent != null && !intent.equals("EXPLAIN");
    }
    
    public boolean requiresNodeResolution() {
        return intent != null && (intent.equals("MOVE_TIME") || intent.equals("DELETE_NODE") || 
                                 intent.equals("REPLACE_NODE") || intent.equals("BOOK_NODE"));
    }
    
    public boolean requiresDayContext() {
        return intent != null && (intent.equals("REPLAN_TODAY") || intent.equals("INSERT_PLACE"));
    }
    
    /**
     * Map intent type to task type for agent routing.
     */
    private String mapIntentToTaskType(String intent) {
        if (intent == null) {
            return "general";
        }
        
        switch (intent.toUpperCase()) {
            case "EDIT":
            case "MOVE_TIME":
            case "INSERT_PLACE":
            case "DELETE_NODE":
            case "REPLACE_NODE":
            case "REPLAN_TODAY":
                return "editing";
            case "BOOK_NODE":
                return "booking";
            case "ENRICH":
                return "ENRICHMENT";
            case "EXPLAIN":
                return "explanation";
            default:
                return "general";
        }
    }
    
    @Override
    public String toString() {
        return "IntentResult{" +
                "intent='" + intent + '\'' +
                ", taskType='" + taskType + '\'' +
                ", day=" + day +
                ", nodeIds=" + nodeIds +
                ", entities=" + entities +
                ", constraints=" + constraints +
                ", confidence=" + confidence +
                '}';
    }
}
