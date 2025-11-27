package com.tripplanner.dto.tools;

/**
 * Request DTO for geocode tool.
 * P2-1: Convert address to coordinates.
 */
public class GeocodeRequest {
    
    private String itineraryId; // Optional: for caching
    private String address;
    private String country; // Optional hint
    
    public GeocodeRequest() {
    }
    
    public GeocodeRequest(String address) {
        this.address = address;
    }
    
    public GeocodeRequest(String address, String country) {
        this.address = address;
        this.country = country;
    }
    
    public GeocodeRequest(String itineraryId, String address, String country) {
        this.itineraryId = itineraryId;
        this.address = address;
        this.country = country;
    }
    
    // Getters and setters
    
    public String getItineraryId() {
        return itineraryId;
    }
    
    public void setItineraryId(String itineraryId) {
        this.itineraryId = itineraryId;
    }
    
    public String getAddress() {
        return address;
    }
    
    public void setAddress(String address) {
        this.address = address;
    }
    
    public String getCountry() {
        return country;
    }
    
    public void setCountry(String country) {
        this.country = country;
    }
}
