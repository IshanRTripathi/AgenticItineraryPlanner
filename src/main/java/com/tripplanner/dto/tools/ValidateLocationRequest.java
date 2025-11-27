package com.tripplanner.dto.tools;

import com.tripplanner.dto.Coordinates;

public class ValidateLocationRequest {
    private Coordinates placeCoordinates;
    private Coordinates destinationCoordinates;
    private String placeName;
    private String destinationName;
    
    public ValidateLocationRequest() {
    }
    
    public Coordinates getPlaceCoordinates() {
        return placeCoordinates;
    }
    
    public void setPlaceCoordinates(Coordinates placeCoordinates) {
        this.placeCoordinates = placeCoordinates;
    }
    
    public Coordinates getDestinationCoordinates() {
        return destinationCoordinates;
    }
    
    public void setDestinationCoordinates(Coordinates destinationCoordinates) {
        this.destinationCoordinates = destinationCoordinates;
    }
    
    public String getPlaceName() {
        return placeName;
    }
    
    public void setPlaceName(String placeName) {
        this.placeName = placeName;
    }
    
    public String getDestinationName() {
        return destinationName;
    }
    
    public void setDestinationName(String destinationName) {
        this.destinationName = destinationName;
    }
}
