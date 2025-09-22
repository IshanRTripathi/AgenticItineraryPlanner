package com.tripplanner.api.dto;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Result of intent classification containing structured information about the user's intent.
 */
public class IntentResult {
    
    private String intent; // "REPLAN_TODAY" | "MOVE_TIME" | "INSERT_PLACE" | "DELETE_NODE" | "REPLACE_NODE" | "BOOK_NODE" | "UNDO" | "EXPLAIN"
    private Integer day;
    private List<String> nodeIds;
    private Map<String, String> entities;
    private Map<String, Object> constraints;
    
    // Constructors
    public IntentResult() {}
    
    public IntentResult(String intent, Integer day, List<String> nodeIds, 
                       Map<String, String> entities, Map<String, Object> constraints) {
        this.intent = intent;
        this.day = day;
        this.nodeIds = nodeIds;
        this.entities = entities;
        this.constraints = constraints;
    }
    
    // Static factory methods for common intents
    public static IntentResult replanToday(Integer day, Map<String, String> entities) {
        return new IntentResult("REPLAN_TODAY", day, List.of(), entities, Map.of());
    }
    
    public static IntentResult moveTime(List<String> nodeIds, Map<String, String> entities) {
        return new IntentResult("MOVE_TIME", null, nodeIds, entities, Map.of());
    }
    
    public static IntentResult insertPlace(Integer day, Map<String, String> entities) {
        return new IntentResult("INSERT_PLACE", day, List.of(), entities, Map.of());
    }
    
    public static IntentResult deleteNode(List<String> nodeIds) {
        return new IntentResult("DELETE_NODE", null, nodeIds, Map.of(), Map.of());
    }
    
    public static IntentResult replaceNode(List<String> nodeIds, Map<String, String> entities) {
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
    
    public Map<String, String> getEntities() {
        return entities;
    }
    
    public void setEntities(Map<String, String> entities) {
        this.entities = entities;
    }
    
    public Map<String, Object> getConstraints() {
        return constraints;
    }
    
    public void setConstraints(Map<String, Object> constraints) {
        this.constraints = constraints;
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
    
    @Override
    public String toString() {
        return "IntentResult{" +
                "intent='" + intent + '\'' +
                ", day=" + day +
                ", nodeIds=" + nodeIds +
                ", entities=" + entities +
                ", constraints=" + constraints +
                '}';
    }
}
