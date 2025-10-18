package com.tripplanner.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Geometry DTO for Google Places API location data.
 */
public class Geometry {
    
    @JsonProperty("location")
    private Location location;
    
    @JsonProperty("viewport")
    private Viewport viewport;
    
    public Geometry() {}
    
    public Geometry(Location location) {
        this.location = location;
    }
    
    // Getters and Setters
    public Location getLocation() {
        return location;
    }
    
    public void setLocation(Location location) {
        this.location = location;
    }
    
    public Viewport getViewport() {
        return viewport;
    }
    
    public void setViewport(Viewport viewport) {
        this.viewport = viewport;
    }
    
    @Override
    public String toString() {
        return "Geometry{" +
                "location=" + location +
                ", viewport=" + viewport +
                '}';
    }
    
    /**
     * Location DTO with latitude and longitude.
     */
    public static class Location {
        @JsonProperty("lat")
        private Double latitude;
        
        @JsonProperty("lng")
        private Double longitude;
        
        public Location() {}
        
        public Location(Double latitude, Double longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
        }
        
        public Double getLatitude() {
            return latitude;
        }
        
        public void setLatitude(Double latitude) {
            this.latitude = latitude;
        }
        
        public Double getLongitude() {
            return longitude;
        }
        
        public void setLongitude(Double longitude) {
            this.longitude = longitude;
        }
        
        /**
         * Calculate distance to another location in kilometers.
         */
        public double distanceTo(Location other) {
            if (other == null || latitude == null || longitude == null || 
                other.latitude == null || other.longitude == null) {
                return Double.MAX_VALUE;
            }
            
            double lat1Rad = Math.toRadians(latitude);
            double lat2Rad = Math.toRadians(other.latitude);
            double deltaLatRad = Math.toRadians(other.latitude - latitude);
            double deltaLngRad = Math.toRadians(other.longitude - longitude);
            
            double a = Math.sin(deltaLatRad / 2) * Math.sin(deltaLatRad / 2) +
                      Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                      Math.sin(deltaLngRad / 2) * Math.sin(deltaLngRad / 2);
            double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
            
            return 6371 * c; // Earth's radius in kilometers
        }
        
        /**
         * Check if coordinates are valid.
         */
        public boolean isValid() {
            return latitude != null && longitude != null &&
                   latitude >= -90 && latitude <= 90 &&
                   longitude >= -180 && longitude <= 180;
        }
        
        @Override
        public String toString() {
            return "Location{" +
                    "latitude=" + latitude +
                    ", longitude=" + longitude +
                    '}';
        }
    }
    
    /**
     * Viewport DTO for bounding box.
     */
    public static class Viewport {
        @JsonProperty("northeast")
        private Location northeast;
        
        @JsonProperty("southwest")
        private Location southwest;
        
        public Location getNortheast() {
            return northeast;
        }
        
        public void setNortheast(Location northeast) {
            this.northeast = northeast;
        }
        
        public Location getSouthwest() {
            return southwest;
        }
        
        public void setSouthwest(Location southwest) {
            this.southwest = southwest;
        }
        
        /**
         * Check if a location is within this viewport.
         */
        public boolean contains(Location location) {
            if (location == null || northeast == null || southwest == null) {
                return false;
            }
            
            return location.getLatitude() >= southwest.getLatitude() &&
                   location.getLatitude() <= northeast.getLatitude() &&
                   location.getLongitude() >= southwest.getLongitude() &&
                   location.getLongitude() <= northeast.getLongitude();
        }
        
        @Override
        public String toString() {
            return "Viewport{" +
                    "northeast=" + northeast +
                    ", southwest=" + southwest +
                    '}';
        }
    }
}