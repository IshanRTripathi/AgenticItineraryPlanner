package com.tripplanner.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.ArrayList;

/**
 * Hotel DTO for booking operations.
 */
public class Hotel {
    
    @JsonProperty("hotelId")
    private String hotelId;
    
    @JsonProperty("name")
    private String name;
    
    @JsonProperty("address")
    private String address;
    
    @JsonProperty("city")
    private String city;
    
    @JsonProperty("country")
    private String country;
    
    @JsonProperty("coordinates")
    private Coordinates coordinates;
    
    @JsonProperty("rating")
    private Double rating;
    
    @JsonProperty("starRating")
    private Integer starRating;
    
    @JsonProperty("price")
    private Double price;
    
    @JsonProperty("currency")
    private String currency;
    
    @JsonProperty("pricePerNight")
    private Double pricePerNight;
    
    @JsonProperty("totalPrice")
    private Double totalPrice;
    
    @JsonProperty("description")
    private String description;
    
    @JsonProperty("amenities")
    private List<String> amenities;
    
    @JsonProperty("photos")
    private List<Photo> photos;
    
    @JsonProperty("reviews")
    private List<Review> reviews;
    
    @JsonProperty("reviewCount")
    private Integer reviewCount;
    
    @JsonProperty("availability")
    private String availability;
    
    @JsonProperty("cancellationPolicy")
    private String cancellationPolicy;
    
    @JsonProperty("bookingUrl")
    private String bookingUrl;
    
    @JsonProperty("provider")
    private String provider;
    
    public Hotel() {
        this.amenities = new ArrayList<>();
        this.photos = new ArrayList<>();
        this.reviews = new ArrayList<>();
    }
    
    public Hotel(String hotelId, String name, Double price, String currency) {
        this();
        this.hotelId = hotelId;
        this.name = name;
        this.price = price;
        this.currency = currency;
    }
    
    // Getters and Setters
    public String getHotelId() {
        return hotelId;
    }
    
    public void setHotelId(String hotelId) {
        this.hotelId = hotelId;
    }
    
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
    
    public String getCity() {
        return city;
    }
    
    public void setCity(String city) {
        this.city = city;
    }
    
    public String getCountry() {
        return country;
    }
    
    public void setCountry(String country) {
        this.country = country;
    }
    
    public Coordinates getCoordinates() {
        return coordinates;
    }
    
    public void setCoordinates(Coordinates coordinates) {
        this.coordinates = coordinates;
    }
    
    public Double getRating() {
        return rating;
    }
    
    public void setRating(Double rating) {
        this.rating = rating;
    }
    
    public Integer getStarRating() {
        return starRating;
    }
    
    public void setStarRating(Integer starRating) {
        this.starRating = starRating;
    }
    
    public Double getPrice() {
        return price;
    }
    
    public void setPrice(Double price) {
        this.price = price;
    }
    
    public String getCurrency() {
        return currency;
    }
    
    public void setCurrency(String currency) {
        this.currency = currency;
    }
    
    public Double getPricePerNight() {
        return pricePerNight;
    }
    
    public void setPricePerNight(Double pricePerNight) {
        this.pricePerNight = pricePerNight;
    }
    
    public Double getTotalPrice() {
        return totalPrice;
    }
    
    public void setTotalPrice(Double totalPrice) {
        this.totalPrice = totalPrice;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public List<String> getAmenities() {
        return amenities;
    }
    
    public void setAmenities(List<String> amenities) {
        this.amenities = amenities;
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
    
    public Integer getReviewCount() {
        return reviewCount;
    }
    
    public void setReviewCount(Integer reviewCount) {
        this.reviewCount = reviewCount;
    }
    
    public String getAvailability() {
        return availability;
    }
    
    public void setAvailability(String availability) {
        this.availability = availability;
    }
    
    public String getCancellationPolicy() {
        return cancellationPolicy;
    }
    
    public void setCancellationPolicy(String cancellationPolicy) {
        this.cancellationPolicy = cancellationPolicy;
    }
    
    public String getBookingUrl() {
        return bookingUrl;
    }
    
    public void setBookingUrl(String bookingUrl) {
        this.bookingUrl = bookingUrl;
    }
    
    public String getProvider() {
        return provider;
    }
    
    public void setProvider(String provider) {
        this.provider = provider;
    }
    
    @Override
    public String toString() {
        return "Hotel{" +
                "hotelId='" + hotelId + '\'' +
                ", name='" + name + '\'' +
                ", address='" + address + '\'' +
                ", city='" + city + '\'' +
                ", country='" + country + '\'' +
                ", coordinates=" + coordinates +
                ", rating=" + rating +
                ", starRating=" + starRating +
                ", price=" + price +
                ", currency='" + currency + '\'' +
                ", pricePerNight=" + pricePerNight +
                ", totalPrice=" + totalPrice +
                ", description='" + description + '\'' +
                ", amenities=" + amenities +
                ", photos=" + (photos != null ? photos.size() : 0) + " photos" +
                ", reviews=" + (reviews != null ? reviews.size() : 0) + " reviews" +
                ", reviewCount=" + reviewCount +
                ", availability='" + availability + '\'' +
                ", cancellationPolicy='" + cancellationPolicy + '\'' +
                ", bookingUrl='" + bookingUrl + '\'' +
                ", provider='" + provider + '\'' +
                '}';
    }
}