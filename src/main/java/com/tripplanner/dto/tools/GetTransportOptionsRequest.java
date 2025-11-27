package com.tripplanner.dto.tools;

import com.tripplanner.dto.Coordinates;

/**
 * Request DTO for get transport options tool.
 * P2-3: Get available transport options between two locations.
 */
public class GetTransportOptionsRequest {
    
    private String origin;
    private String destination;
    private Coordinates originCoordinates;
    private Coordinates destinationCoordinates;
    private String date; // ISO format: YYYY-MM-DD
    private String time; // HH:mm format
    
    public GetTransportOptionsRequest() {
    }
    
    public GetTransportOptionsRequest(String origin, String destination) {
        this.origin = origin;
        this.destination = destination;
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
    
    public String getDate() {
        return date;
    }
    
    public void setDate(String date) {
        this.date = date;
    }
    
    public String getTime() {
        return time;
    }
    
    public void setTime(String time) {
        this.time = time;
    }
}
