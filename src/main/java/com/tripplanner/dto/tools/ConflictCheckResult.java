package com.tripplanner.dto.tools;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;

/**
 * Response DTO for conflict checking tool
 */
public class ConflictCheckResult {
    
    @JsonProperty("success")
    private boolean success = true;
    
    @JsonProperty("hasConflicts")
    private boolean hasConflicts = false;
    
    @JsonProperty("conflicts")
    private List<ConflictDetail> conflicts = new ArrayList<>();
    
    @JsonProperty("warnings")
    private List<String> warnings = new ArrayList<>();
    
    @JsonProperty("error")
    private String error;
    
    public ConflictCheckResult() {}
    
    public void addConflict(ConflictDetail conflict) {
        this.conflicts.add(conflict);
        this.hasConflicts = true;
    }
    
    public void addWarning(String warning) {
        this.warnings.add(warning);
    }
    
    public static ConflictCheckResult error(String error) {
        ConflictCheckResult result = new ConflictCheckResult();
        result.success = false;
        result.error = error;
        return result;
    }
    
    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public boolean isHasConflicts() {
        return hasConflicts;
    }
    
    public void setHasConflicts(boolean hasConflicts) {
        this.hasConflicts = hasConflicts;
    }
    
    public List<ConflictDetail> getConflicts() {
        return conflicts;
    }
    
    public void setConflicts(List<ConflictDetail> conflicts) {
        this.conflicts = conflicts;
    }
    
    public List<String> getWarnings() {
        return warnings;
    }
    
    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }
    
    public String getError() {
        return error;
    }
    
    public void setError(String error) {
        this.error = error;
    }
    
    /**
     * Inner class for conflict details
     */
    public static class ConflictDetail {
        @JsonProperty("type")
        private String type; // "TIME_OVERLAP", "BUDGET_EXCEEDED", etc.
        
        @JsonProperty("severity")
        private String severity; // "ERROR", "WARNING"
        
        @JsonProperty("dayNumber")
        private Integer dayNumber;
        
        @JsonProperty("nodeId1")
        private String nodeId1;
        
        @JsonProperty("nodeId2")
        private String nodeId2;
        
        @JsonProperty("message")
        private String message;
        
        public ConflictDetail() {}
        
        public ConflictDetail(String type, String severity, Integer dayNumber, 
                            String nodeId1, String nodeId2, String message) {
            this.type = type;
            this.severity = severity;
            this.dayNumber = dayNumber;
            this.nodeId1 = nodeId1;
            this.nodeId2 = nodeId2;
            this.message = message;
        }
        
        // Getters and Setters
        public String getType() {
            return type;
        }
        
        public void setType(String type) {
            this.type = type;
        }
        
        public String getSeverity() {
            return severity;
        }
        
        public void setSeverity(String severity) {
            this.severity = severity;
        }
        
        public Integer getDayNumber() {
            return dayNumber;
        }
        
        public void setDayNumber(Integer dayNumber) {
            this.dayNumber = dayNumber;
        }
        
        public String getNodeId1() {
            return nodeId1;
        }
        
        public void setNodeId1(String nodeId1) {
            this.nodeId1 = nodeId1;
        }
        
        public String getNodeId2() {
            return nodeId2;
        }
        
        public void setNodeId2(String nodeId2) {
            this.nodeId2 = nodeId2;
        }
        
        public String getMessage() {
            return message;
        }
        
        public void setMessage(String message) {
            this.message = message;
        }
    }
}
