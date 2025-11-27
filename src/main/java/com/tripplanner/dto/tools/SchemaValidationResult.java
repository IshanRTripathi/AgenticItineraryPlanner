package com.tripplanner.dto.tools;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;

/**
 * Response DTO for schema validation tool
 */
public class SchemaValidationResult {
    
    @JsonProperty("success")
    private boolean success = true;
    
    @JsonProperty("valid")
    private boolean valid;
    
    @JsonProperty("errors")
    private List<String> errors = new ArrayList<>();
    
    @JsonProperty("retryable")
    private boolean retryable;
    
    @JsonProperty("userFriendlyMessage")
    private String userFriendlyMessage;
    
    @JsonProperty("error")
    private String error;
    
    public SchemaValidationResult() {}
    
    public SchemaValidationResult(boolean valid, List<String> errors) {
        this.valid = valid;
        this.errors = errors != null ? errors : new ArrayList<>();
    }
    
    public static SchemaValidationResult error(String error) {
        SchemaValidationResult result = new SchemaValidationResult();
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
    
    public List<String> getErrors() {
        return errors;
    }
    
    public void setErrors(List<String> errors) {
        this.errors = errors;
    }
    
    public boolean isRetryable() {
        return retryable;
    }
    
    public void setRetryable(boolean retryable) {
        this.retryable = retryable;
    }
    
    public String getUserFriendlyMessage() {
        return userFriendlyMessage;
    }
    
    public void setUserFriendlyMessage(String userFriendlyMessage) {
        this.userFriendlyMessage = userFriendlyMessage;
    }
    
    public String getError() {
        return error;
    }
    
    public void setError(String error) {
        this.error = error;
    }
}
