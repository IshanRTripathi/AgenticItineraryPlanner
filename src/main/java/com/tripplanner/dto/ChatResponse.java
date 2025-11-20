package com.tripplanner.dto;

import java.util.List;

/**
 * Response DTO for chat routing.
 */
public class ChatResponse {
    
    private String intent;
    private String message;
    private ChangeSet changeSet;
    private ItineraryDiff diff;
    private boolean applied;
    private Integer toVersion;
    private List<String> warnings;
    private boolean needsDisambiguation;
    private List<NodeCandidate> candidates;
    private List<String> errors; // List of error messages
    private ActionButton actionButton; // Optional action button for the response
    
    // Constructors
    public ChatResponse() {}
    
    public ChatResponse(String intent, String message, ChangeSet changeSet, ItineraryDiff diff, 
                       boolean applied, Integer toVersion, List<String> warnings, 
                       boolean needsDisambiguation, List<NodeCandidate> candidates) {
        this.intent = intent;
        this.message = message;
        this.changeSet = changeSet;
        this.diff = diff;
        this.applied = applied;
        this.toVersion = toVersion;
        this.warnings = warnings;
        this.needsDisambiguation = needsDisambiguation;
        this.candidates = candidates;
    }
    
    // Static factory methods for common response types
    public static ChatResponse success(String intent, String message, ChangeSet changeSet, 
                                     ItineraryDiff diff, boolean applied, Integer toVersion) {
        return new ChatResponse(intent, message, changeSet, diff, applied, toVersion, 
                               List.of(), false, null);
    }
    
    public static ChatResponse disambiguation(String intent, String message, List<NodeCandidate> candidates) {
        return new ChatResponse(intent, message, null, null, false, null, 
                               List.of(), true, candidates);
    }
    
    public static ChatResponse explain(String message) {
        return new ChatResponse("EXPLAIN", message, null, null, false, null, 
                               List.of(), false, null);
    }
    
    public static ChatResponse error(String message, List<String> warnings) {
        return new ChatResponse("ERROR", message, null, null, false, null, 
                               warnings, false, null);
    }
    
    // Getters and Setters
    public String getIntent() {
        return intent;
    }
    
    public void setIntent(String intent) {
        this.intent = intent;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public ChangeSet getChangeSet() {
        return changeSet;
    }
    
    public void setChangeSet(ChangeSet changeSet) {
        this.changeSet = changeSet;
    }
    
    public ItineraryDiff getDiff() {
        return diff;
    }
    
    public void setDiff(ItineraryDiff diff) {
        this.diff = diff;
    }
    
    public boolean isApplied() {
        return applied;
    }
    
    public void setApplied(boolean applied) {
        this.applied = applied;
    }
    
    public Integer getToVersion() {
        return toVersion;
    }
    
    public void setToVersion(Integer toVersion) {
        this.toVersion = toVersion;
    }
    
    public List<String> getWarnings() {
        return warnings;
    }
    
    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }
    
    public boolean isNeedsDisambiguation() {
        return needsDisambiguation;
    }
    
    public void setNeedsDisambiguation(boolean needsDisambiguation) {
        this.needsDisambiguation = needsDisambiguation;
    }
    
    public List<NodeCandidate> getCandidates() {
        return candidates;
    }
    
    public void setCandidates(List<NodeCandidate> candidates) {
        this.candidates = candidates;
    }
    
    public List<String> getErrors() {
        return errors;
    }
    
    public void setErrors(List<String> errors) {
        this.errors = errors;
    }
    
    public ActionButton getActionButton() {
        return actionButton;
    }
    
    public void setActionButton(ActionButton actionButton) {
        this.actionButton = actionButton;
    }
    
    /**
     * Check if this response represents an error.
     */
    public boolean isError() {
        return "ERROR".equals(intent) || (errors != null && !errors.isEmpty());
    }
    
    @Override
    public String toString() {
        return "ChatResponse{" +
                "intent='" + intent + '\'' +
                ", message='" + message + '\'' +
                ", changeSet=" + changeSet +
                ", diff=" + diff +
                ", applied=" + applied +
                ", toVersion=" + toVersion +
                ", warnings=" + warnings +
                ", needsDisambiguation=" + needsDisambiguation +
                ", candidates=" + candidates +
                ", errors=" + errors +
                ", actionButton=" + actionButton +
                '}';
    }
    
    /**
     * Action button for chat responses.
     */
    public static class ActionButton {
        private String label;
        private String action; // e.g., "VIEW_PLAN", "VIEW_DAY"
        private String target; // e.g., tab name or day number
        
        public ActionButton() {}
        
        public ActionButton(String label, String action, String target) {
            this.label = label;
            this.action = action;
            this.target = target;
        }
        
        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
        
        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }
        
        public String getTarget() { return target; }
        public void setTarget(String target) { this.target = target; }
        
        @Override
        public String toString() {
            return "ActionButton{label='" + label + "', action='" + action + "', target='" + target + "'}";
        }
    }
}
