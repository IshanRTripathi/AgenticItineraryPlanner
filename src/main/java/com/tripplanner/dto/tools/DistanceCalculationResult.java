package com.tripplanner.dto.tools;

import java.util.ArrayList;
import java.util.List;

/**
 * Result DTO for distance calculation tool.
 * P1-3: Returns distance, duration, and metadata.
 */
public class DistanceCalculationResult {
    
    private boolean success;
    private double distanceKm;
    private int durationMinutes;
    private String mode;
    private boolean fromAPI;
    private String origin;
    private String destination;
    private String error;
    private List<String> warnings;
    
    public DistanceCalculationResult() {
        this.warnings = new ArrayList<>();
    }
    
    public DistanceCalculationResult(double distanceKm, int durationMinutes, String mode, boolean fromAPI) {
        this.success = true;
        this.distanceKm = distanceKm;
        this.durationMinutes = durationMinutes;
        this.mode = mode;
        this.fromAPI = fromAPI;
        this.warnings = new ArrayList<>();
    }
    
    public static DistanceCalculationResult error(String error) {
        DistanceCalculationResult result = new DistanceCalculationResult();
        result.success = false;
        result.error = error;
        return result;
    }
    
    public void addWarning(String warning) {
        this.warnings.add(warning);
    }
    
    // Getters and setters
    
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public double getDistanceKm() {
        return distanceKm;
    }
    
    public void setDistanceKm(double distanceKm) {
        this.distanceKm = distanceKm;
    }
    
    public int getDurationMinutes() {
        return durationMinutes;
    }
    
    public void setDurationMinutes(int durationMinutes) {
        this.durationMinutes = durationMinutes;
    }
    
    public String getMode() {
        return mode;
    }
    
    public void setMode(String mode) {
        this.mode = mode;
    }
    
    public boolean isFromAPI() {
        return fromAPI;
    }
    
    public void setFromAPI(boolean fromAPI) {
        this.fromAPI = fromAPI;
    }
    
    public String getOrigin() {
        return origin;
    }
    
    public void setOrigin(String origin) {
        this.origin = origin;
    }
    
    public String getDestination() {
        return destination;
    }
    
    public void setDestination(String destination) {
        this.destination = destination;
    }
    
    public String getError() {
        return error;
    }
    
    public void setError(String error) {
        this.error = error;
    }
    
    public List<String> getWarnings() {
        return warnings;
    }
    
    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }
}
