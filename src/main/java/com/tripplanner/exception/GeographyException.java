package com.tripplanner.exception;

/**
 * Exception for geography-related errors (invalid routes, island detection, etc.)
 */
public class GeographyException extends ValidationException {
    
    private final String fromLocation;
    private final String toLocation;
    private final String reason;
    
    public GeographyException(String message, String fromLocation, String toLocation, String reason) {
        super(message, "Invalid route: " + reason);
        this.fromLocation = fromLocation;
        this.toLocation = toLocation;
        this.reason = reason;
    }
    
    public String getFromLocation() {
        return fromLocation;
    }
    
    public String getToLocation() {
        return toLocation;
    }
    
    public String getReason() {
        return reason;
    }
}
