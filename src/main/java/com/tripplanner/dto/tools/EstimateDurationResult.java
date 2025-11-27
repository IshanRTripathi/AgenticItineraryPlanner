package com.tripplanner.dto.tools;

public class EstimateDurationResult {
    private boolean success;
    private int estimatedDurationMinutes;
    private int minDurationMinutes;
    private int maxDurationMinutes;
    private String reasoning;
    private String error;
    
    public EstimateDurationResult() {
    }
    
    public static EstimateDurationResult error(String error) {
        EstimateDurationResult result = new EstimateDurationResult();
        result.success = false;
        result.error = error;
        return result;
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public int getEstimatedDurationMinutes() {
        return estimatedDurationMinutes;
    }
    
    public void setEstimatedDurationMinutes(int estimatedDurationMinutes) {
        this.estimatedDurationMinutes = estimatedDurationMinutes;
    }
    
    public int getMinDurationMinutes() {
        return minDurationMinutes;
    }
    
    public void setMinDurationMinutes(int minDurationMinutes) {
        this.minDurationMinutes = minDurationMinutes;
    }
    
    public int getMaxDurationMinutes() {
        return maxDurationMinutes;
    }
    
    public void setMaxDurationMinutes(int maxDurationMinutes) {
        this.maxDurationMinutes = maxDurationMinutes;
    }
    
    public String getReasoning() {
        return reasoning;
    }
    
    public void setReasoning(String reasoning) {
        this.reasoning = reasoning;
    }
    
    public String getError() {
        return error;
    }
    
    public void setError(String error) {
        this.error = error;
    }
}
