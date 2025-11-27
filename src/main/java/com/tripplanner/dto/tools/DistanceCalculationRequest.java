package com.tripplanner.dto.tools;

import com.tripplanner.dto.Coordinates;

/**
 * Request DTO for distance calculation tool.
 * P1-3: Calculate distance and travel time between two locations.
 */
public class DistanceCalculationRequest {
    
    private String origin;
    private String destination;
    private Coordinates originCoordinates;
    private Coordinates destinationCoordinates;
    private String mode; // driving, walking, transit
    
    public DistanceCalculationRequest() {
    }
    
    public DistanceCalculationRequest(String origin, String destination, String mode) {
        this.origin = origin;
        this.destination = destination;
        this.mode = mode;
    }
    
    // Getters and setters
    
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
    
    public Coordinates getOriginCoordinates() {
        return originCoordinates;
    }
    
    public void setOriginCoordinates(Coordinates originCoordinates) {
        this.originCoordinates = originCoordinates;
    }
    
    public Coordinates getDestinationCoordinates() {
        return destinationCoordinates;
    }
    
    public void setDestinationCoordinates(Coordinates destinationCoordinates) {
        this.destinationCoordinates = destinationCoordinates;
    }
    
    public String getMode() {
        return mode;
    }
    
    public void setMode(String mode) {
        this.mode = mode;
    }
}
