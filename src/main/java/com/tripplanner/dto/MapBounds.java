package com.tripplanner.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Map bounds for defining the geographic area of an itinerary.
 * Used for initial map camera positioning and bounds calculation.
 */
public class MapBounds {
    
    @JsonProperty("south")
    private Double south;
    
    @JsonProperty("west")
    private Double west;
    
    @JsonProperty("north")
    private Double north;
    
    @JsonProperty("east")
    private Double east;
    
    public MapBounds() {}
    
    public MapBounds(Double south, Double west, Double north, Double east) {
        this.south = south;
        this.west = west;
        this.north = north;
        this.east = east;
    }
    
    // Getters and Setters
    public Double getSouth() {
        return south;
    }
    
    public void setSouth(Double south) {
        this.south = south;
    }
    
    public Double getWest() {
        return west;
    }
    
    public void setWest(Double west) {
        this.west = west;
    }
    
    public Double getNorth() {
        return north;
    }
    
    public void setNorth(Double north) {
        this.north = north;
    }
    
    public Double getEast() {
        return east;
    }
    
    public void setEast(Double east) {
        this.east = east;
    }
    
    /**
     * Check if the bounds are valid (all coordinates are non-null and logically correct).
     */
    public boolean isValid() {
        return south != null && west != null && north != null && east != null
                && south <= north && west <= east;
    }
    
    /**
     * Get the center point of the bounds.
     */
    public Coordinates getCenter() {
        if (!isValid()) {
            return null;
        }
        double centerLat = (south + north) / 2.0;
        double centerLng = (west + east) / 2.0;
        return new Coordinates(centerLat, centerLng);
    }
    
    /**
     * Get the span of the bounds (width and height).
     */
    public Coordinates getSpan() {
        if (!isValid()) {
            return null;
        }
        double latSpan = north - south;
        double lngSpan = east - west;
        return new Coordinates(latSpan, lngSpan);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MapBounds mapBounds = (MapBounds) o;
        return java.util.Objects.equals(south, mapBounds.south) &&
                java.util.Objects.equals(west, mapBounds.west) &&
                java.util.Objects.equals(north, mapBounds.north) &&
                java.util.Objects.equals(east, mapBounds.east);
    }
    
    @Override
    public int hashCode() {
        return java.util.Objects.hash(south, west, north, east);
    }
    
    @Override
    public String toString() {
        return "MapBounds{" +
                "south=" + south +
                ", west=" + west +
                ", north=" + north +
                ", east=" + east +
                '}';
    }
}
