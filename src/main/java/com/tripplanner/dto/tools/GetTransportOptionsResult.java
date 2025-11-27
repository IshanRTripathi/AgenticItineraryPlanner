package com.tripplanner.dto.tools;

import java.util.ArrayList;
import java.util.List;

/**
 * Result DTO for get transport options tool.
 * P2-3: Returns available transport options with mode, duration, and cost.
 */
public class GetTransportOptionsResult {
    
    private boolean success;
    private List<TransportOption> options;
    private String error;
    private List<String> warnings;
    
    public GetTransportOptionsResult() {
        this.options = new ArrayList<>();
        this.warnings = new ArrayList<>();
    }
    
    public static GetTransportOptionsResult error(String error) {
        GetTransportOptionsResult result = new GetTransportOptionsResult();
        result.success = false;
        result.error = error;
        return result;
    }
    
    public void addOption(TransportOption option) {
        this.options.add(option);
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
    
    public List<TransportOption> getOptions() {
        return options;
    }
    
    public void setOptions(List<TransportOption> options) {
        this.options = options;
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
    
    /**
     * Inner class representing a single transport option.
     */
    public static class TransportOption {
        private String mode; // flight, train, bus, car, ferry
        private int durationMinutes;
        private double estimatedCost;
        private String currency;
        private double distanceKm;
        private boolean available;
        private String provider; // e.g., "Air India", "Indian Railways"
        private String notes;
        
        public TransportOption() {
        }
        
        public TransportOption(String mode, int durationMinutes, double estimatedCost, String currency) {
            this.mode = mode;
            this.durationMinutes = durationMinutes;
            this.estimatedCost = estimatedCost;
            this.currency = currency;
            this.available = true;
        }
        
        // Getters and setters
        
        public String getMode() {
            return mode;
        }
        
        public void setMode(String mode) {
            this.mode = mode;
        }
        
        public int getDurationMinutes() {
            return durationMinutes;
        }
        
        public void setDurationMinutes(int durationMinutes) {
            this.durationMinutes = durationMinutes;
        }
        
        public double getEstimatedCost() {
            return estimatedCost;
        }
        
        public void setEstimatedCost(double estimatedCost) {
            this.estimatedCost = estimatedCost;
        }
        
        public String getCurrency() {
            return currency;
        }
        
        public void setCurrency(String currency) {
            this.currency = currency;
        }
        
        public double getDistanceKm() {
            return distanceKm;
        }
        
        public void setDistanceKm(double distanceKm) {
            this.distanceKm = distanceKm;
        }
        
        public boolean isAvailable() {
            return available;
        }
        
        public void setAvailable(boolean available) {
            this.available = available;
        }
        
        public String getProvider() {
            return provider;
        }
        
        public void setProvider(String provider) {
            this.provider = provider;
        }
        
        public String getNotes() {
            return notes;
        }
        
        public void setNotes(String notes) {
            this.notes = notes;
        }
    }
}
