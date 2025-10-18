package com.tripplanner.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import java.util.Map;
import java.util.HashMap;

/**
 * Booking request DTO for hotel, flight, and activity bookings.
 */
public class BookingRequest {
    
    @JsonProperty("bookingType")
    private String bookingType; // "hotel", "flight", "activity"
    
    @JsonProperty("location")
    private String location;
    
    @JsonProperty("checkInDate")
    private LocalDate checkInDate;
    
    @JsonProperty("checkOutDate")
    private LocalDate checkOutDate;
    
    @JsonProperty("guests")
    private Integer guests;
    
    @JsonProperty("rooms")
    private Integer rooms;
    
    @JsonProperty("origin")
    private String origin; // For flights
    
    @JsonProperty("destination")
    private String destination; // For flights
    
    @JsonProperty("departureDate")
    private LocalDate departureDate;
    
    @JsonProperty("returnDate")
    private LocalDate returnDate;
    
    @JsonProperty("passengers")
    private Integer passengers;
    
    @JsonProperty("activityType")
    private String activityType;
    
    @JsonProperty("interests")
    private String interests;
    
    @JsonProperty("budget")
    private Double budget;
    
    @JsonProperty("currency")
    private String currency;
    
    @JsonProperty("preferences")
    private Map<String, Object> preferences;
    
    @JsonProperty("userId")
    private String userId;
    
    @JsonProperty("itineraryId")
    private String itineraryId;
    
    public BookingRequest() {
        this.preferences = new HashMap<>();
        this.guests = 1;
        this.rooms = 1;
        this.passengers = 1;
        this.currency = "USD";
    }
    
    // Getters and Setters
    public String getBookingType() {
        return bookingType;
    }
    
    public void setBookingType(String bookingType) {
        this.bookingType = bookingType;
    }
    
    public String getLocation() {
        return location;
    }
    
    public void setLocation(String location) {
        this.location = location;
    }
    
    public LocalDate getCheckInDate() {
        return checkInDate;
    }
    
    public void setCheckInDate(LocalDate checkInDate) {
        this.checkInDate = checkInDate;
    }
    
    public LocalDate getCheckOutDate() {
        return checkOutDate;
    }
    
    public void setCheckOutDate(LocalDate checkOutDate) {
        this.checkOutDate = checkOutDate;
    }
    
    public Integer getGuests() {
        return guests;
    }
    
    public void setGuests(Integer guests) {
        this.guests = guests;
    }
    
    public Integer getRooms() {
        return rooms;
    }
    
    public void setRooms(Integer rooms) {
        this.rooms = rooms;
    }
    
    public String getOrigin() {
        return origin;
    }
    
    public void setOrigin(String origin) {
        this.origin = origin;
    }
    
    public String getDestination() {
        return destination;
    }
    
    public void setDestination(String destination) {
        this.destination = destination;
    }
    
    public LocalDate getDepartureDate() {
        return departureDate;
    }
    
    public void setDepartureDate(LocalDate departureDate) {
        this.departureDate = departureDate;
    }
    
    public LocalDate getReturnDate() {
        return returnDate;
    }
    
    public void setReturnDate(LocalDate returnDate) {
        this.returnDate = returnDate;
    }
    
    public Integer getPassengers() {
        return passengers;
    }
    
    public void setPassengers(Integer passengers) {
        this.passengers = passengers;
    }
    
    public String getActivityType() {
        return activityType;
    }
    
    public void setActivityType(String activityType) {
        this.activityType = activityType;
    }
    
    public String getInterests() {
        return interests;
    }
    
    public void setInterests(String interests) {
        this.interests = interests;
    }
    
    public Double getBudget() {
        return budget;
    }
    
    public void setBudget(Double budget) {
        this.budget = budget;
    }
    
    public String getCurrency() {
        return currency;
    }
    
    public void setCurrency(String currency) {
        this.currency = currency;
    }
    
    public Map<String, Object> getPreferences() {
        return preferences;
    }
    
    public void setPreferences(Map<String, Object> preferences) {
        this.preferences = preferences;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getItineraryId() {
        return itineraryId;
    }
    
    public void setItineraryId(String itineraryId) {
        this.itineraryId = itineraryId;
    }
    
    /**
     * Add a preference to the booking request.
     */
    public void addPreference(String key, Object value) {
        if (preferences == null) {
            preferences = new HashMap<>();
        }
        preferences.put(key, value);
    }
    
    @Override
    public String toString() {
        return "BookingRequest{" +
                "bookingType='" + bookingType + '\'' +
                ", location='" + location + '\'' +
                ", checkInDate=" + checkInDate +
                ", checkOutDate=" + checkOutDate +
                ", guests=" + guests +
                ", rooms=" + rooms +
                ", origin='" + origin + '\'' +
                ", destination='" + destination + '\'' +
                ", departureDate=" + departureDate +
                ", returnDate=" + returnDate +
                ", passengers=" + passengers +
                ", activityType='" + activityType + '\'' +
                ", interests='" + interests + '\'' +
                ", budget=" + budget +
                ", currency='" + currency + '\'' +
                ", preferences=" + preferences +
                ", userId='" + userId + '\'' +
                ", itineraryId='" + itineraryId + '\'' +
                '}';
    }
}