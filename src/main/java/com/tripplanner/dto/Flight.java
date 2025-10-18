package com.tripplanner.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

/**
 * Flight DTO for flight booking operations.
 */
public class Flight {
    
    @JsonProperty("flightId")
    private String flightId;
    
    @JsonProperty("airline")
    private String airline;
    
    @JsonProperty("flightNumber")
    private String flightNumber;
    
    @JsonProperty("origin")
    private String origin;
    
    @JsonProperty("destination")
    private String destination;
    
    @JsonProperty("originAirport")
    private String originAirport;
    
    @JsonProperty("destinationAirport")
    private String destinationAirport;
    
    @JsonProperty("departureTime")
    private LocalDateTime departureTime;
    
    @JsonProperty("arrivalTime")
    private LocalDateTime arrivalTime;
    
    @JsonProperty("duration")
    private Integer duration; // in minutes
    
    @JsonProperty("price")
    private Double price;
    
    @JsonProperty("currency")
    private String currency;
    
    @JsonProperty("cabinClass")
    private String cabinClass; // "economy", "business", "first"
    
    @JsonProperty("stops")
    private Integer stops;
    
    @JsonProperty("layovers")
    private List<String> layovers;
    
    @JsonProperty("aircraft")
    private String aircraft;
    
    @JsonProperty("baggage")
    private String baggage;
    
    @JsonProperty("cancellable")
    private Boolean cancellable;
    
    @JsonProperty("refundable")
    private Boolean refundable;
    
    @JsonProperty("bookingUrl")
    private String bookingUrl;
    
    @JsonProperty("provider")
    private String provider;
    
    @JsonProperty("availability")
    private Integer availability; // seats available
    
    public Flight() {
        this.layovers = new ArrayList<>();
        this.stops = 0;
        this.cancellable = false;
        this.refundable = false;
    }
    
    public Flight(String flightId, String airline, String flightNumber, String origin, String destination) {
        this();
        this.flightId = flightId;
        this.airline = airline;
        this.flightNumber = flightNumber;
        this.origin = origin;
        this.destination = destination;
    }
    
    // Getters and Setters
    public String getFlightId() {
        return flightId;
    }
    
    public void setFlightId(String flightId) {
        this.flightId = flightId;
    }
    
    public String getAirline() {
        return airline;
    }
    
    public void setAirline(String airline) {
        this.airline = airline;
    }
    
    public String getFlightNumber() {
        return flightNumber;
    }
    
    public void setFlightNumber(String flightNumber) {
        this.flightNumber = flightNumber;
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
    
    public String getOriginAirport() {
        return originAirport;
    }
    
    public void setOriginAirport(String originAirport) {
        this.originAirport = originAirport;
    }
    
    public String getDestinationAirport() {
        return destinationAirport;
    }
    
    public void setDestinationAirport(String destinationAirport) {
        this.destinationAirport = destinationAirport;
    }
    
    public LocalDateTime getDepartureTime() {
        return departureTime;
    }
    
    public void setDepartureTime(LocalDateTime departureTime) {
        this.departureTime = departureTime;
    }
    
    public LocalDateTime getArrivalTime() {
        return arrivalTime;
    }
    
    public void setArrivalTime(LocalDateTime arrivalTime) {
        this.arrivalTime = arrivalTime;
    }
    
    public Integer getDuration() {
        return duration;
    }
    
    public void setDuration(Integer duration) {
        this.duration = duration;
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
    
    public String getCabinClass() {
        return cabinClass;
    }
    
    public void setCabinClass(String cabinClass) {
        this.cabinClass = cabinClass;
    }
    
    public Integer getStops() {
        return stops;
    }
    
    public void setStops(Integer stops) {
        this.stops = stops;
    }
    
    public List<String> getLayovers() {
        return layovers;
    }
    
    public void setLayovers(List<String> layovers) {
        this.layovers = layovers;
    }
    
    public String getAircraft() {
        return aircraft;
    }
    
    public void setAircraft(String aircraft) {
        this.aircraft = aircraft;
    }
    
    public String getBaggage() {
        return baggage;
    }
    
    public void setBaggage(String baggage) {
        this.baggage = baggage;
    }
    
    public Boolean getCancellable() {
        return cancellable;
    }
    
    public void setCancellable(Boolean cancellable) {
        this.cancellable = cancellable;
    }
    
    public Boolean getRefundable() {
        return refundable;
    }
    
    public void setRefundable(Boolean refundable) {
        this.refundable = refundable;
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
    
    public Integer getAvailability() {
        return availability;
    }
    
    public void setAvailability(Integer availability) {
        this.availability = availability;
    }
    
    /**
     * Check if this is a direct flight (no stops).
     */
    public boolean isDirect() {
        return stops == null || stops == 0;
    }
    
    /**
     * Get flight duration in hours and minutes format.
     */
    public String getFormattedDuration() {
        if (duration == null) {
            return "Unknown";
        }
        int hours = duration / 60;
        int minutes = duration % 60;
        return String.format("%dh %02dm", hours, minutes);
    }
    
    /**
     * Get total price (alias for price field).
     */
    public Double getTotalPrice() {
        return price;
    }
    
    /**
     * Set total price (alias for price field).
     */
    public void setTotalPrice(Double totalPrice) {
        this.price = totalPrice;
    }
    
    @Override
    public String toString() {
        return "Flight{" +
                "flightId='" + flightId + '\'' +
                ", airline='" + airline + '\'' +
                ", flightNumber='" + flightNumber + '\'' +
                ", origin='" + origin + '\'' +
                ", destination='" + destination + '\'' +
                ", originAirport='" + originAirport + '\'' +
                ", destinationAirport='" + destinationAirport + '\'' +
                ", departureTime=" + departureTime +
                ", arrivalTime=" + arrivalTime +
                ", duration=" + duration +
                ", price=" + price +
                ", currency='" + currency + '\'' +
                ", cabinClass='" + cabinClass + '\'' +
                ", stops=" + stops +
                ", layovers=" + layovers +
                ", aircraft='" + aircraft + '\'' +
                ", baggage='" + baggage + '\'' +
                ", cancellable=" + cancellable +
                ", refundable=" + refundable +
                ", bookingUrl='" + bookingUrl + '\'' +
                ", provider='" + provider + '\'' +
                ", availability=" + availability +
                '}';
    }
}