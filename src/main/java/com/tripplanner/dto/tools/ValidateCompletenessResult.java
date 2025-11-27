package com.tripplanner.dto.tools;

import java.util.ArrayList;
import java.util.List;

public class ValidateCompletenessResult {
    private boolean success;
    private boolean complete;
    private List<String> missingComponents;
    private List<String> recommendations;
    private String error;
    
    public ValidateCompletenessResult() {
        this.missingComponents = new ArrayList<>();
        this.recommendations = new ArrayList<>();
    }
    
    public static ValidateCompletenessResult error(String error) {
        ValidateCompletenessResult result = new ValidateCompletenessResult();
        result.success = false;
        result.error = error;
        return result;
    }
    
    public void addMissingComponent(String component) {
        this.missingComponents.add(component);
    }
    
    public void addRecommendation(String recommendation) {
        this.recommendations.add(recommendation);
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public boolean isComplete() {
        return complete;
    }
    
    public void setComplete(boolean complete) {
        this.complete = complete;
    }
    
    public List<String> getMissingComponents() {
        return missingComponents;
    }
    
    public void setMissingComponents(List<String> missingComponents) {
        this.missingComponents = missingComponents;
    }
    
    public List<String> getRecommendations() {
        return recommendations;
    }
    
    public void setRecommendations(List<String> recommendations) {
        this.recommendations = recommendations;
    }
    
    public String getError() {
        return error;
    }
    
    public void setError(String error) {
        this.error = error;
    }
}
