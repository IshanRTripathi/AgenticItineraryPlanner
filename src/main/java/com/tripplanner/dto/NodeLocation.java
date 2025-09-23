package com.tripplanner.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Node location information.
 */
public class NodeLocation {
    
    @JsonProperty("name")
    private String name;
    
    @JsonProperty("address")
    private String address;
    
    @JsonProperty("coordinates")
    private Coordinates coordinates;
    
    public NodeLocation() {}
    
    public NodeLocation(String name, String address, Coordinates coordinates) {
        this.name = name;
        this.address = address;
        this.coordinates = coordinates;
    }
    
    // Getters and Setters
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getAddress() {
        return address;
    }
    
    public void setAddress(String address) {
        this.address = address;
    }
    
    public Coordinates getCoordinates() {
        return coordinates;
    }
    
    public void setCoordinates(Coordinates coordinates) {
        this.coordinates = coordinates;
    }
    
    @Override
    public String toString() {
        return "NodeLocation{" +
                "name='" + name + '\'' +
                ", address='" + address + '\'' +
                ", coordinates=" + coordinates +
                '}';
    }
}
