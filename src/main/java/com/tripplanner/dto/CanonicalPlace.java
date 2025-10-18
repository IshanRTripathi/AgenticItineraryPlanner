package com.tripplanner.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;

/**
 * Canonical place representation that consolidates information from multiple sources.
 * Each canonical place has a stable placeId and merges data from various place candidates.
 */
public class CanonicalPlace {
    
    @JsonProperty("placeId")
    private String placeId; // Stable canonical place ID (format: cp_<hash>)
    
    @JsonProperty("name")
    private String name; // Primary name (most authoritative source)
    
    @JsonProperty("alternativeNames")
    private Set<String> alternativeNames; // All known names for this place
    
    @JsonProperty("coordinates")
    private Coordinates coordinates; // Canonical coordinates (averaged from sources)
    
    @JsonProperty("address")
    private String address; // Primary address
    
    @JsonProperty("alternativeAddresses")
    private Set<String> alternativeAddresses; // All known addresses
    
    @JsonProperty("sources")
    private List<PlaceSource> sources; // All source place IDs and their metadata
    
    @JsonProperty("category")
    private String category; // Primary category
    
    @JsonProperty("types")
    private Set<String> types; // All place types from various sources
    
    @JsonProperty("rating")
    private Double rating; // Aggregated rating
    
    @JsonProperty("priceLevel")
    private Integer priceLevel; // Consensus price level
    
    @JsonProperty("confidence")
    private Double confidence; // Confidence score for the canonical place (0.0-1.0)
    
    @JsonProperty("lastUpdated")
    private Long lastUpdated; // Timestamp of last update
    
    @JsonProperty("createdAt")
    private Long createdAt; // Timestamp of creation
    
    public CanonicalPlace() {
        this.alternativeNames = new HashSet<>();
        this.alternativeAddresses = new HashSet<>();
        this.sources = new ArrayList<>();
        this.types = new HashSet<>();
        this.confidence = 1.0;
        this.createdAt = System.currentTimeMillis();
        this.lastUpdated = System.currentTimeMillis();
    }
    
    public CanonicalPlace(String placeId, String name, Coordinates coordinates, String address) {
        this();
        this.placeId = placeId;
        this.name = name;
        this.coordinates = coordinates;
        this.address = address;
    }
    
    /**
     * Add a place source to this canonical place.
     */
    public void addSource(PlaceSource source) {
        if (source != null) {
            this.sources.add(source);
            this.lastUpdated = System.currentTimeMillis();
        }
    }
    
    /**
     * Add an alternative name.
     */
    public void addAlternativeName(String name) {
        if (name != null && !name.trim().isEmpty() && !name.equals(this.name)) {
            this.alternativeNames.add(name.trim());
            this.lastUpdated = System.currentTimeMillis();
        }
    }
    
    /**
     * Add an alternative address.
     */
    public void addAlternativeAddress(String address) {
        if (address != null && !address.trim().isEmpty() && !address.equals(this.address)) {
            this.alternativeAddresses.add(address.trim());
            this.lastUpdated = System.currentTimeMillis();
        }
    }
    
    /**
     * Add a place type.
     */
    public void addType(String type) {
        if (type != null && !type.trim().isEmpty()) {
            this.types.add(type.trim());
            this.lastUpdated = System.currentTimeMillis();
        }
    }
    
    /**
     * Update coordinates with weighted average.
     */
    public void updateCoordinates(Coordinates newCoords, double weight) {
        if (newCoords != null && newCoords.getLat() != null && newCoords.getLng() != null) {
            if (this.coordinates == null) {
                this.coordinates = new Coordinates(newCoords.getLat(), newCoords.getLng());
            } else {
                // Weighted average of coordinates
                double currentWeight = 1.0 - weight;
                double newLat = (this.coordinates.getLat() * currentWeight) + (newCoords.getLat() * weight);
                double newLng = (this.coordinates.getLng() * currentWeight) + (newCoords.getLng() * weight);
                this.coordinates = new Coordinates(newLat, newLng);
            }
            this.lastUpdated = System.currentTimeMillis();
        }
    }
    
    /**
     * Update rating with weighted average.
     */
    public void updateRating(Double newRating, double weight) {
        if (newRating != null && newRating >= 0.0 && newRating <= 5.0) {
            if (this.rating == null) {
                this.rating = newRating;
            } else {
                // Weighted average of ratings
                double currentWeight = 1.0 - weight;
                this.rating = (this.rating * currentWeight) + (newRating * weight);
            }
            this.lastUpdated = System.currentTimeMillis();
        }
    }
    
    /**
     * Check if this canonical place has a specific source.
     */
    public boolean hasSource(String sourceType, String sourceId) {
        return sources.stream()
                .anyMatch(source -> sourceType.equals(source.getSourceType()) && 
                                  sourceId.equals(source.getSourceId()));
    }
    
    /**
     * Get the primary source (most authoritative).
     */
    public PlaceSource getPrimarySource() {
        return sources.stream()
                .max((s1, s2) -> Double.compare(s1.getAuthority(), s2.getAuthority()))
                .orElse(null);
    }
    
    // Getters and Setters
    public String getPlaceId() {
        return placeId;
    }
    
    public void setPlaceId(String placeId) {
        this.placeId = placeId;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
        this.lastUpdated = System.currentTimeMillis();
    }
    
    public Set<String> getAlternativeNames() {
        return alternativeNames;
    }
    
    public void setAlternativeNames(Set<String> alternativeNames) {
        this.alternativeNames = alternativeNames != null ? alternativeNames : new HashSet<>();
        this.lastUpdated = System.currentTimeMillis();
    }
    
    public Coordinates getCoordinates() {
        return coordinates;
    }
    
    public void setCoordinates(Coordinates coordinates) {
        this.coordinates = coordinates;
        this.lastUpdated = System.currentTimeMillis();
    }
    
    public String getAddress() {
        return address;
    }
    
    public void setAddress(String address) {
        this.address = address;
        this.lastUpdated = System.currentTimeMillis();
    }
    
    public Set<String> getAlternativeAddresses() {
        return alternativeAddresses;
    }
    
    public void setAlternativeAddresses(Set<String> alternativeAddresses) {
        this.alternativeAddresses = alternativeAddresses != null ? alternativeAddresses : new HashSet<>();
        this.lastUpdated = System.currentTimeMillis();
    }
    
    public List<PlaceSource> getSources() {
        return sources;
    }
    
    public void setSources(List<PlaceSource> sources) {
        this.sources = sources != null ? sources : new ArrayList<>();
        this.lastUpdated = System.currentTimeMillis();
    }
    
    public String getCategory() {
        return category;
    }
    
    public void setCategory(String category) {
        this.category = category;
        this.lastUpdated = System.currentTimeMillis();
    }
    
    public Set<String> getTypes() {
        return types;
    }
    
    public void setTypes(Set<String> types) {
        this.types = types != null ? types : new HashSet<>();
        this.lastUpdated = System.currentTimeMillis();
    }
    
    public Double getRating() {
        return rating;
    }
    
    public void setRating(Double rating) {
        this.rating = rating;
        this.lastUpdated = System.currentTimeMillis();
    }
    
    public Integer getPriceLevel() {
        return priceLevel;
    }
    
    public void setPriceLevel(Integer priceLevel) {
        this.priceLevel = priceLevel;
        this.lastUpdated = System.currentTimeMillis();
    }
    
    public Double getConfidence() {
        return confidence;
    }
    
    public void setConfidence(Double confidence) {
        this.confidence = confidence;
        this.lastUpdated = System.currentTimeMillis();
    }
    
    public Long getLastUpdated() {
        return lastUpdated;
    }
    
    public void setLastUpdated(Long lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
    
    public Long getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }
    
    @Override
    public String toString() {
        return "CanonicalPlace{" +
                "placeId='" + placeId + '\'' +
                ", name='" + name + '\'' +
                ", coordinates=" + coordinates +
                ", address='" + address + '\'' +
                ", sources=" + sources.size() + " sources" +
                ", confidence=" + confidence +
                '}';
    }
    
    /**
     * Represents a source of place information.
     */
    public static class PlaceSource {
        @JsonProperty("sourceType")
        private String sourceType; // "google", "foursquare", "user", etc.
        
        @JsonProperty("sourceId")
        private String sourceId; // Original place ID from the source
        
        @JsonProperty("authority")
        private Double authority; // Authority score (0.0-1.0, higher = more authoritative)
        
        @JsonProperty("addedAt")
        private Long addedAt;
        
        public PlaceSource() {
            this.addedAt = System.currentTimeMillis();
        }
        
        public PlaceSource(String sourceType, String sourceId, Double authority) {
            this();
            this.sourceType = sourceType;
            this.sourceId = sourceId;
            this.authority = authority;
        }
        
        // Getters and Setters
        public String getSourceType() {
            return sourceType;
        }
        
        public void setSourceType(String sourceType) {
            this.sourceType = sourceType;
        }
        
        public String getSourceId() {
            return sourceId;
        }
        
        public void setSourceId(String sourceId) {
            this.sourceId = sourceId;
        }
        
        public Double getAuthority() {
            return authority;
        }
        
        public void setAuthority(Double authority) {
            this.authority = authority;
        }
        
        public Long getAddedAt() {
            return addedAt;
        }
        
        public void setAddedAt(Long addedAt) {
            this.addedAt = addedAt;
        }
        
        @Override
        public String toString() {
            return "PlaceSource{" +
                    "sourceType='" + sourceType + '\'' +
                    ", sourceId='" + sourceId + '\'' +
                    ", authority=" + authority +
                    '}';
        }
    }
}