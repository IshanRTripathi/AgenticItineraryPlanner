package com.tripplanner.dto.tools;

import java.util.ArrayList;
import java.util.List;

public class CheckCapacityResult {
    private boolean success;
    private boolean canAccommodate;
    private Integer maxCapacity;
    private String recommendation;
    private List<String> warnings;
    private String error;
    
    public CheckCapacityResult() {
        this.warnings = new ArrayList<>();
    }
    
    public static CheckCapacityResult error(String error) {
        CheckCapacityResult result = new CheckCapacityResult();
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
    
    public boolean isCanAccommodate() {
        return canAccommodate;
    }
    
    public void setCanAccommodate(boolean canAccommodate) {
        this.canAccommodate = canAccommodate;
    }
    
    public Integer getMaxCapacity() {
        return maxCapacity;
    }
    
    public void setMaxCapacity(Integer maxCapacity) {
        this.maxCapacity = maxCapacity;
    }
    
    public String getRecommendation() {
        return recommendation;
    }
    
    public void setRecommendation(String recommendation) {
        this.recommendation = recommendation;
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
