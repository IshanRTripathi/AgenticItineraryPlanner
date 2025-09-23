package com.tripplanner.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tripplanner.data.entity.Itinerary;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

/**
 * DTO for location information.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record LocationDto(
        @Size(max = 200, message = "Location name must not exceed 200 characters")
        String name,
        
        @Size(max = 500, message = "Address must not exceed 500 characters")
        String address,
        
        @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
        @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
        double lat,
        
        @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
        @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
        double lng,
        
        @Size(max = 100, message = "Place ID must not exceed 100 characters")
        String placeId
) {
    
    /**
     * Create DTO from entity.
     */
    public static LocationDto fromEntity(Itinerary.Location entity) {
        if (entity == null) {
            return null;
        }
        
        return new LocationDto(
                entity.getName(),
                entity.getAddress(),
                entity.getLat(),
                entity.getLng(),
                entity.getPlaceId()
        );
    }
    
    /**
     * Convert to entity.
     */
    public Itinerary.Location toEntity() {
        Itinerary.Location entity = new Itinerary.Location();
        entity.setName(name);
        entity.setAddress(address);
        entity.setLat(lat);
        entity.setLng(lng);
        entity.setPlaceId(placeId);
        return entity;
    }
    
    /**
     * Create a simple location with just name and coordinates.
     */
    public static LocationDto of(String name, double lat, double lng) {
        return new LocationDto(name, null, lat, lng, null);
    }
    
    /**
     * Get formatted coordinates string.
     */
    public String getCoordinatesString() {
        return String.format("%.6f,%.6f", lat, lng);
    }
    
    /**
     * Calculate distance to another location in kilometers (using Haversine formula).
     */
    public double distanceTo(LocationDto other) {
        if (other == null) {
            return Double.MAX_VALUE;
        }
        
        final int R = 6371; // Radius of the earth in km
        
        double latDistance = Math.toRadians(other.lat - this.lat);
        double lonDistance = Math.toRadians(other.lng - this.lng);
        
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(this.lat)) * Math.cos(Math.toRadians(other.lat))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return R * c; // Distance in km
    }
    
    /**
     * Check if coordinates are valid.
     */
    public boolean hasValidCoordinates() {
        return lat >= -90 && lat <= 90 && lng >= -180 && lng <= 180;
    }
}

