package com.tripplanner.dto.tools;

import com.tripplanner.dto.Coordinates;
import java.util.ArrayList;
import java.util.List;

/**
 * Result DTO for geocode tool.
 * P2-1: Returns coordinates and formatted address.
 */
public class GeocodeResult {
    
    private boolean success;
    private Coordinates coordinates;
    private String formattedAddress;
    private String placeId;
    private String error;
    private List<String> warnings;
    
    public GeocodeResult() {
        this.warnings = new ArrayList<>();
    }
    
    public GeocodeResult(Coordinates coordinates, String formattedAddress) {
        this.success = true;
        this.coordinates = coordinates;
        this.formattedAddress = formattedAddress;
        this.warnings = new ArrayList<>();
    }
    
    public static GeocodeResult error(String error) {
        GeocodeResult result = new GeocodeResult();
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
    
    public Coordinates getCoordinates() {
        return coordinates;
    }
    
    public void setCoordinates(Coordinates coordinates) {
        this.coordinates = coordinates;
    }
    
    public String getFormattedAddress() {
        return formattedAddress;
    }
    
    public void setFormattedAddress(String formattedAddress) {
        this.formattedAddress = formattedAddress;
    }
    
    public String getPlaceId() {
        return placeId;
    }
    
    public void setPlaceId(String placeId) {
        this.placeId = placeId;
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
