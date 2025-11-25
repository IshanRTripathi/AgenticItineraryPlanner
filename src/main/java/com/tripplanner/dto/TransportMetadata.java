package com.tripplanner.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tripplanner.enums.TransportMode;
import com.tripplanner.enums.TransportType;

import java.util.List;

/**
 * Explicit transport metadata to replace string parsing
 */
public class TransportMetadata extends NodeMetadata {
    
    @JsonProperty("transportType")
    private TransportType transportType;
    
    @JsonProperty("fromLocationId")
    private String fromLocationId;
    
    @JsonProperty("toLocationId")
    private String toLocationId;
    
    @JsonProperty("fromLocationName")
    private String fromLocationName;
    
    @JsonProperty("toLocationName")
    private String toLocationName;
    
    @JsonProperty("fromCoordinates")
    private Coordinates fromCoordinates;
    
    @JsonProperty("toCoordinates")
    private Coordinates toCoordinates;
    
    @JsonProperty("fromIsIsland")
    private Boolean fromIsIsland;
    
    @JsonProperty("toIsIsland")
    private Boolean toIsIsland;
    
    @JsonProperty("allowedModes")
    private List<TransportMode> allowedModes;
    
    @JsonProperty("selectedMode")
    private TransportMode selectedMode;
    
    @JsonProperty("estimatedDurationMinutes")
    private Integer estimatedDurationMinutes;
    
    @JsonProperty("estimatedDistanceKm")
    private Double estimatedDistanceKm;
    
    @JsonProperty("bookingUrl")
    private String bookingUrl;
    
    @JsonProperty("bookingPlatform")
    private String bookingPlatform;

    public TransportMetadata() {
        super("transport");
    }

    // Getters and setters
    public TransportType getTransportType() {
        return transportType;
    }

    public void setTransportType(TransportType transportType) {
        this.transportType = transportType;
    }

    public String getFromLocationId() {
        return fromLocationId;
    }

    public void setFromLocationId(String fromLocationId) {
        this.fromLocationId = fromLocationId;
    }

    public String getToLocationId() {
        return toLocationId;
    }

    public void setToLocationId(String toLocationId) {
        this.toLocationId = toLocationId;
    }

    public String getFromLocationName() {
        return fromLocationName;
    }

    public void setFromLocationName(String fromLocationName) {
        this.fromLocationName = fromLocationName;
    }

    public String getToLocationName() {
        return toLocationName;
    }

    public void setToLocationName(String toLocationName) {
        this.toLocationName = toLocationName;
    }

    public Coordinates getFromCoordinates() {
        return fromCoordinates;
    }

    public void setFromCoordinates(Coordinates fromCoordinates) {
        this.fromCoordinates = fromCoordinates;
    }

    public Coordinates getToCoordinates() {
        return toCoordinates;
    }

    public void setToCoordinates(Coordinates toCoordinates) {
        this.toCoordinates = toCoordinates;
    }

    public Boolean getFromIsIsland() {
        return fromIsIsland;
    }

    public void setFromIsIsland(Boolean fromIsIsland) {
        this.fromIsIsland = fromIsIsland;
    }

    public Boolean getToIsIsland() {
        return toIsIsland;
    }

    public void setToIsIsland(Boolean toIsIsland) {
        this.toIsIsland = toIsIsland;
    }

    public List<TransportMode> getAllowedModes() {
        return allowedModes;
    }

    public void setAllowedModes(List<TransportMode> allowedModes) {
        this.allowedModes = allowedModes;
    }

    public TransportMode getSelectedMode() {
        return selectedMode;
    }

    public void setSelectedMode(TransportMode selectedMode) {
        this.selectedMode = selectedMode;
    }

    public Integer getEstimatedDurationMinutes() {
        return estimatedDurationMinutes;
    }

    public void setEstimatedDurationMinutes(Integer estimatedDurationMinutes) {
        this.estimatedDurationMinutes = estimatedDurationMinutes;
    }

    public Double getEstimatedDistanceKm() {
        return estimatedDistanceKm;
    }

    public void setEstimatedDistanceKm(Double estimatedDistanceKm) {
        this.estimatedDistanceKm = estimatedDistanceKm;
    }

    public String getBookingUrl() {
        return bookingUrl;
    }

    public void setBookingUrl(String bookingUrl) {
        this.bookingUrl = bookingUrl;
    }

    public String getBookingPlatform() {
        return bookingPlatform;
    }

    public void setBookingPlatform(String bookingPlatform) {
        this.bookingPlatform = bookingPlatform;
    }

    public static class Coordinates {
        @JsonProperty("latitude")
        private Double latitude;
        
        @JsonProperty("longitude")
        private Double longitude;

        public Coordinates() {}

        public Coordinates(Double latitude, Double longitude) {
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
    }
}
