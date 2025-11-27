package com.tripplanner.dto.tools;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;

/**
 * Response DTO for constraint validation tool
 */
public class ConstraintCheckResult {
    
    @JsonProperty("success")
    private boolean success = true;
    
    @JsonProperty("valid")
    private boolean valid = true;
    
    @JsonProperty("violations")
    private List<ConstraintViolation> violations = new ArrayList<>();
    
    @JsonProperty("warnings")
    private List<String> warnings = new ArrayList<>();
    
    @JsonProperty("error")
    private String error;
    
    public ConstraintCheckResult() {}
    
    public void addViolation(String type, String message) {
        this.violations.add(new ConstraintViolation(type, message));
        this.valid = false;
    }
    
    public void addWarning(String warning) {
        this.warnings.add(warning);
    }
    
    public static ConstraintCheckResult error(String error) {
        ConstraintCheckResult result = new ConstraintCheckResult();
        result.success = false;
        result.valid = false;
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
    
    public boolean isValid() {
        return valid;
    }
    
    public void setValid(boolean valid) {
        this.valid = valid;
    }
    
    public List<ConstraintViolation> getViolations() {
        return violations;
    }
    
    public void setViolations(List<ConstraintViolation> violations) {
        this.violations = violations;
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
     * Inner class for constraint violations
     */
    public static class ConstraintViolation {
        @JsonProperty("type")
        private String type;
        
        @JsonProperty("message")
        private String message;
        
        public ConstraintViolation() {}
        
        public ConstraintViolation(String type, String message) {
            this.type = type;
            this.message = message;
        }
        
        public String getType() {
            return type;
        }
        
        public void setType(String type) {
            this.type = type;
        }
        
        public String getMessage() {
            return message;
        }
        
        public void setMessage(String message) {
            this.message = message;
        }
    }
}
