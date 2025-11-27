package com.tripplanner.dto.tools;

import com.tripplanner.dto.NormalizedNode;
import java.util.ArrayList;
import java.util.List;

public class UpdateNodeResult {
    private boolean success;
    private NormalizedNode updatedNode;
    private List<String> warnings;
    private String error;
    
    public UpdateNodeResult() {
        this.warnings = new ArrayList<>();
    }
    
    public static UpdateNodeResult error(String error) {
        UpdateNodeResult result = new UpdateNodeResult();
        result.success = false;
        result.error = error;
        return result;
    }
    
    public void addWarning(String warning) {
        this.warnings.add(warning);
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public NormalizedNode getUpdatedNode() {
        return updatedNode;
    }
    
    public void setUpdatedNode(NormalizedNode updatedNode) {
        this.updatedNode = updatedNode;
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
}
