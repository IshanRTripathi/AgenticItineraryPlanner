package com.tripplanner.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.ArrayList;

/**
 * Place details from Google Places API.
 * Contains comprehensive information about a place including photos, reviews, and metadata.
 */
public class PlaceDetails {
    
    @JsonProperty("place_id")
    private String placeId;
    
    @JsonProperty("name")
    private String name;
    
    @JsonProperty("formatted_address")
    private String formattedAddress;
    
    @JsonProperty("rating")
    private Double rating;
    
    @JsonProperty("price_level")
    private Integer priceLevel;
    
    @JsonProperty("photos")
    private List<Photo> photos;
    
    @JsonProperty("reviews")
    private List<Review> reviews;
    
    @JsonProperty("opening_hours")
    private OpeningHours openingHours;
    
    @JsonProperty("geometry")
    private Geometry geometry;
    
    @JsonProperty("types")
    private List<String> types;
    
    @JsonProperty("website")
    private String website;
    
    @JsonProperty("formatted_phone_number")
    private String formattedPhoneNumber;
    
    @JsonProperty("international_phone_number")
    private String internationalPhoneNumber;
    
    public PlaceDetails() {
        this.photos = new ArrayList<>();
        this.reviews = new ArrayList<>();
        this.types = new ArrayList<>();
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
    }
    
    public String getFormattedAddress() {
        return formattedAddress;
    }
    
    public void setFormattedAddress(String formattedAddress) {
        this.formattedAddress = formattedAddress;
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
    
    public List<Photo> getPhotos() {
        return photos;
    }
    
    public void setPhotos(List<Photo> photos) {
        this.photos = photos;
    }
    
    public List<Review> getReviews() {
        return reviews;
    }
    
    public void setReviews(List<Review> reviews) {
        this.reviews = reviews;
    }
    
    public OpeningHours getOpeningHours() {
        return openingHours;
    }
    
    public void setOpeningHours(OpeningHours openingHours) {
        this.openingHours = openingHours;
    }
    
    public Geometry getGeometry() {
        return geometry;
    }
    
    public void setGeometry(Geometry geometry) {
        this.geometry = geometry;
    }
    
    public List<String> getTypes() {
        return types;
    }
    
    public void setTypes(List<String> types) {
        this.types = types;
    }
    
    public String getWebsite() {
        return website;
    }
    
    public void setWebsite(String website) {
        this.website = website;
    }
    
    public String getFormattedPhoneNumber() {
        return formattedPhoneNumber;
    }
    
    public void setFormattedPhoneNumber(String formattedPhoneNumber) {
        this.formattedPhoneNumber = formattedPhoneNumber;
    }
    
    public String getInternationalPhoneNumber() {
        return internationalPhoneNumber;
    }
    
    public void setInternationalPhoneNumber(String internationalPhoneNumber) {
        this.internationalPhoneNumber = internationalPhoneNumber;
    }
    
    @Override
    public String toString() {
        return "PlaceDetails{" +
                "placeId='" + placeId + '\'' +
                ", name='" + name + '\'' +
                ", formattedAddress='" + formattedAddress + '\'' +
                ", rating=" + rating +
                ", priceLevel=" + priceLevel +
                ", photos=" + (photos != null ? photos.size() : 0) + " photos" +
                ", reviews=" + (reviews != null ? reviews.size() : 0) + " reviews" +
                ", openingHours=" + openingHours +
                ", geometry=" + geometry +
                ", types=" + types +
                ", website='" + website + '\'' +
                ", formattedPhoneNumber='" + formattedPhoneNumber + '\'' +
                ", internationalPhoneNumber='" + internationalPhoneNumber + '\'' +
                '}';
    }
    
    /**
     * Opening hours information from Google Places API.
     */
    public static class OpeningHours {
        @JsonProperty("open_now")
        private Boolean openNow;
        
        @JsonProperty("weekday_text")
        private List<String> weekdayText;
        
        public OpeningHours() {
            this.weekdayText = new ArrayList<>();
        }
        
        public Boolean getOpenNow() {
            return openNow;
        }
        
        public void setOpenNow(Boolean openNow) {
            this.openNow = openNow;
        }
        
        public List<String> getWeekdayText() {
            return weekdayText;
        }
        
        public void setWeekdayText(List<String> weekdayText) {
            this.weekdayText = weekdayText;
        }
        
        @Override
        public String toString() {
            return "OpeningHours{" +
                    "openNow=" + openNow +
                    ", weekdayText=" + weekdayText +
                    '}';
        }
    }
    
    /**
     * Geometry information from Google Places API.
     */
    public static class Geometry {
        @JsonProperty("location")
        private Location location;
        
        public Location getLocation() {
            return location;
        }
        
        public void setLocation(Location location) {
            this.location = location;
        }
        
        @Override
        public String toString() {
            return "Geometry{" +
                    "location=" + location +
                    '}';
        }
        
        public static class Location {
            @JsonProperty("lat")
            private Double lat;
            
            @JsonProperty("lng")
            private Double lng;
            
            public Double getLat() {
                return lat;
            }
            
            public void setLat(Double lat) {
                this.lat = lat;
            }
            
            public Double getLng() {
                return lng;
            }
            
            public void setLng(Double lng) {
                this.lng = lng;
            }
            
            @Override
            public String toString() {
                return "Location{" +
                        "lat=" + lat +
                        ", lng=" + lng +
                        '}';
            }
        }
    }
}