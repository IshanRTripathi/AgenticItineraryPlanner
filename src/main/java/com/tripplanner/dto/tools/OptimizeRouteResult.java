package com.tripplanner.dto.tools;

import java.util.ArrayList;
import java.util.List;

/**
 * Result DTO for optimize route tool.
 * P2-4: Returns optimized order with distance and time savings.
 */
public class OptimizeRouteResult {
    
    private boolean success;
    private List<String> optimizedOrder; // Node IDs in optimized order
    private double totalDistanceKm;
    private int totalTimeMinutes;
    private double originalDistanceKm;
    private int originalTimeMinutes;
    private double distanceSavingsKm;
    private int timeSavingsMinutes;
    private String error;
    private List<String> warnings;
    
    public OptimizeRouteResult() {
        this.optimizedOrder = new ArrayList<>();
        this.warnings = new ArrayList<>();
    }
    
    public static OptimizeRouteResult error(String error) {
        OptimizeRouteResult result = new OptimizeRouteResult();
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
    
    public List<String> getOptimizedOrder() {
        return optimizedOrder;
    }
    
    public void setOptimizedOrder(List<String> optimizedOrder) {
        this.optimizedOrder = optimizedOrder;
    }
    
    public double getTotalDistanceKm() {
        return totalDistanceKm;
    }
    
    public void setTotalDistanceKm(double totalDistanceKm) {
        this.totalDistanceKm = totalDistanceKm;
    }
    
    public int getTotalTimeMinutes() {
        return totalTimeMinutes;
    }
    
    public void setTotalTimeMinutes(int totalTimeMinutes) {
        this.totalTimeMinutes = totalTimeMinutes;
    }
    
    public double getOriginalDistanceKm() {
        return originalDistanceKm;
    }
    
    public void setOriginalDistanceKm(double originalDistanceKm) {
        this.originalDistanceKm = originalDistanceKm;
    }
    
    public int getOriginalTimeMinutes() {
        return originalTimeMinutes;
    }
    
    public void setOriginalTimeMinutes(int originalTimeMinutes) {
        this.originalTimeMinutes = originalTimeMinutes;
    }
    
    public double getDistanceSavingsKm() {
        return distanceSavingsKm;
    }
    
    public void setDistanceSavingsKm(double distanceSavingsKm) {
        this.distanceSavingsKm = distanceSavingsKm;
    }
    
    public int getTimeSavingsMinutes() {
        return timeSavingsMinutes;
    }
    
    public void setTimeSavingsMinutes(int timeSavingsMinutes) {
        this.timeSavingsMinutes = timeSavingsMinutes;
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
