package com.tripplanner.dto.tools;

import java.util.ArrayList;
import java.util.List;

public class ValidateLocationResult {
    private boolean success;
    private boolean valid;
    private boolean warning;
    private double distanceKm;
    private String reason;
    private List<String> recommendations;
    private String error;
    
    public ValidateLocationResult() {
        this.recommendations = new ArrayList<>();
    }
    
    public static ValidateLocationResult error(String error) {
        ValidateLocationResult result = new ValidateLocationResult();
        result.success = false;
        result.error = error;
        return result;
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
    
    public boolean isValid() {
        return valid;
    }
    
    public void setValid(boolean valid) {
        this.valid = valid;
    }
    
    public boolean isWarning() {
        return warning;
    }
    
    public void setWarning(boolean warning) {
        this.warning = warning;
    }
    
    public double getDistanceKm() {
        return distanceKm;
    }
    
    public void setDistanceKm(double distanceKm) {
        this.distanceKm = distanceKm;
    }
    
    public String getReason() {
        return reason;
    }
    
    public void setReason(String reason) {
        this.reason = reason;
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
