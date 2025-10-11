package com.tripplanner.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.ArrayList;

/**
 * Represents a place candidate from any source that needs to be processed
 * by the canonical place registry system.
 */
public class PlaceCandidate {
    
    @JsonProperty("sourceType")
    private String sourceType; // "google", "foursquare", "user", "llm", etc.
    
    @JsonProperty("sourceId")
    private String sourceId; // Original ID from the source
    
    @JsonProperty("name")
    private String name;
    
    @JsonProperty("coordinates")
    private Coordinates coordinates;
    
    @JsonProperty("address")
    private String address;
    
    @JsonProperty("category")
    private String category;
    
    @JsonProperty("types")
    private List<String> types;
    
    @JsonProperty("rating")
    private Double rating;
    
    @JsonProperty("priceLevel")
    private Integer priceLevel;
    
    @JsonProperty("authority")
    private Double authority; // Authority score for this source (0.0-1.0)
    
    @JsonProperty("confidence")
    private Double confidence; // Confidence in the data quality (0.0-1.0)
    
    @JsonProperty("metadata")
    private PlaceCandidateMetadata metadata;
    
    public PlaceCandidate() {
        this.types = new ArrayList<>();
        this.authority = 0.5; // Default medium authority
        this.confidence = 0.8; // Default high confidence
    }
    
    public PlaceCandidate(String sourceType, String sourceId, String name, 
                         Coordinates coordinates, String address) {
        this();
        this.sourceType = sourceType;
        this.sourceId = sourceId;
        this.name = name;
        this.coordinates = coordinates;
        this.address = address;
    }
    
    /**
     * Create a place candidate from Google Places data.
     */
    public static PlaceCandidate fromGooglePlace(PlaceDetails placeDetails) {
        PlaceCandidate candidate = new PlaceCandidate();
        candidate.setSourceType("google");
        candidate.setSourceId(placeDetails.getPlaceId());
        candidate.setName(placeDetails.getName());
        candidate.setAddress(placeDetails.getFormattedAddress());
        candidate.setRating(placeDetails.getRating());
        candidate.setPriceLevel(placeDetails.getPriceLevel());
        candidate.setAuthority(0.9); // Google Places has high authority
        candidate.setConfidence(0.95); // High confidence in Google data
        
        // Extract coordinates from geometry
        if (placeDetails.getGeometry() != null && 
            placeDetails.getGeometry().getLocation() != null) {
            PlaceDetails.Geometry.Location location = placeDetails.getGeometry().getLocation();
            candidate.setCoordinates(new Coordinates(location.getLat(), location.getLng()));
        }
        
        // Set types
        if (placeDetails.getTypes() != null) {
            candidate.setTypes(new ArrayList<>(placeDetails.getTypes()));
            // Set primary category from first type
            if (!placeDetails.getTypes().isEmpty()) {
                candidate.setCategory(placeDetails.getTypes().get(0));
            }
        }
        
        // Set metadata
        PlaceCandidateMetadata metadata = new PlaceCandidateMetadata();
        metadata.setWebsite(placeDetails.getWebsite());
        metadata.setPhoneNumber(placeDetails.getFormattedPhoneNumber());
        metadata.setPhotosCount(placeDetails.getPhotos() != null ? placeDetails.getPhotos().size() : 0);
        metadata.setReviewsCount(placeDetails.getReviews() != null ? placeDetails.getReviews().size() : 0);
        candidate.setMetadata(metadata);
        
        return candidate;
    }
    
    /**
     * Create a place candidate from NodeLocation data.
     */
    public static PlaceCandidate fromNodeLocation(NodeLocation nodeLocation, String sourceType) {
        PlaceCandidate candidate = new PlaceCandidate();
        candidate.setSourceType(sourceType != null ? sourceType : "user");
        candidate.setSourceId(nodeLocation.getPlaceId());
        candidate.setName(nodeLocation.getName());
        candidate.setAddress(nodeLocation.getAddress());
        candidate.setCoordinates(nodeLocation.getCoordinates());
        
        // Set authority based on source type
        switch (candidate.getSourceType()) {
            case "google" -> candidate.setAuthority(0.9);
            case "foursquare" -> candidate.setAuthority(0.8);
            case "user" -> candidate.setAuthority(0.6);
            case "llm" -> candidate.setAuthority(0.4);
            default -> candidate.setAuthority(0.5);
        }
        
        return candidate;
    }
    
    /**
     * Check if this candidate has valid coordinates.
     */
    public boolean hasValidCoordinates() {
        return coordinates != null && 
               coordinates.getLat() != null && 
               coordinates.getLng() != null &&
               coordinates.getLat() >= -90.0 && coordinates.getLat() <= 90.0 &&
               coordinates.getLng() >= -180.0 && coordinates.getLng() <= 180.0;
    }
    
    /**
     * Check if this candidate has sufficient data for processing.
     */
    public boolean isValid() {
        return name != null && !name.trim().isEmpty() &&
               hasValidCoordinates() &&
               sourceType != null && !sourceType.trim().isEmpty();
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
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public Coordinates getCoordinates() {
        return coordinates;
    }
    
    public void setCoordinates(Coordinates coordinates) {
        this.coordinates = coordinates;
    }
    
    public String getAddress() {
        return address;
    }
    
    public void setAddress(String address) {
        this.address = address;
    }
    
    public String getCategory() {
        return category;
    }
    
    public void setCategory(String category) {
        this.category = category;
    }
    
    public List<String> getTypes() {
        return types;
    }
    
    public void setTypes(List<String> types) {
        this.types = types != null ? types : new ArrayList<>();
    }
    
    public Double getRating() {
        return rating;
    }
    
    public void setRating(Double rating) {
        this.rating = rating;
    }
    
    public Integer getPriceLevel() {
        return priceLevel;
    }
    
    public void setPriceLevel(Integer priceLevel) {
        this.priceLevel = priceLevel;
    }
    
    public Double getAuthority() {
        return authority;
    }
    
    public void setAuthority(Double authority) {
        this.authority = authority;
    }
    
    public Double getConfidence() {
        return confidence;
    }
    
    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }
    
    public PlaceCandidateMetadata getMetadata() {
        return metadata;
    }
    
    public void setMetadata(PlaceCandidateMetadata metadata) {
        this.metadata = metadata;
    }
    
    @Override
    public String toString() {
        return "PlaceCandidate{" +
                "sourceType='" + sourceType + '\'' +
                ", sourceId='" + sourceId + '\'' +
                ", name='" + name + '\'' +
                ", coordinates=" + coordinates +
                ", authority=" + authority +
                ", confidence=" + confidence +
                '}';
    }
    
    /**
     * Additional metadata for place candidates.
     */
    public static class PlaceCandidateMetadata {
        @JsonProperty("website")
        private String website;
        
        @JsonProperty("phoneNumber")
        private String phoneNumber;
        
        @JsonProperty("photosCount")
        private Integer photosCount;
        
        @JsonProperty("reviewsCount")
        private Integer reviewsCount;
        
        @JsonProperty("openingHours")
        private String openingHours;
        
        @JsonProperty("lastVerified")
        private Long lastVerified;
        
        public PlaceCandidateMetadata() {
            this.lastVerified = System.currentTimeMillis();
        }
        
        // Getters and Setters
        public String getWebsite() {
            return website;
        }
        
        public void setWebsite(String website) {
            this.website = website;
        }
        
        public String getPhoneNumber() {
            return phoneNumber;
        }
        
        public void setPhoneNumber(String phoneNumber) {
            this.phoneNumber = phoneNumber;
        }
        
        public Integer getPhotosCount() {
            return photosCount;
        }
        
        public void setPhotosCount(Integer photosCount) {
            this.photosCount = photosCount;
        }
        
        public Integer getReviewsCount() {
            return reviewsCount;
        }
        
        public void setReviewsCount(Integer reviewsCount) {
            this.reviewsCount = reviewsCount;
        }
        
        public String getOpeningHours() {
            return openingHours;
        }
        
        public void setOpeningHours(String openingHours) {
            this.openingHours = openingHours;
        }
        
        public Long getLastVerified() {
            return lastVerified;
        }
        
        public void setLastVerified(Long lastVerified) {
            this.lastVerified = lastVerified;
        }
        
        @Override
        public String toString() {
            return "PlaceCandidateMetadata{" +
                    "website='" + website + '\'' +
                    ", phoneNumber='" + phoneNumber + '\'' +
                    ", photosCount=" + photosCount +
                    ", reviewsCount=" + reviewsCount +
                    '}';
        }
    }
}