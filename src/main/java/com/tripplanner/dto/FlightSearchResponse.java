package com.tripplanner.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.ArrayList;

/**
 * Flight search response from Expedia API.
 */
public class FlightSearchResponse {
    
    @JsonProperty("flights")
    private List<Flight> flights;
    
    @JsonProperty("totalResults")
    private Integer totalResults;
    
    @JsonProperty("searchId")
    private String searchId;
    
    @JsonProperty("status")
    private String status;
    
    @JsonProperty("errorMessage")
    private String errorMessage;
    
    @JsonProperty("currency")
    private String currency;
    
    @JsonProperty("origin")
    private String origin;
    
    @JsonProperty("destination")
    private String destination;
    
    @JsonProperty("departureDate")
    private String departureDate;
    
    @JsonProperty("returnDate")
    private String returnDate;
    
    @JsonProperty("passengers")
    private Integer passengers;
    
    @JsonProperty("cabinClass")
    private String cabinClass;
    
    public FlightSearchResponse() {
        this.flights = new ArrayList<>();
    }
    
    // Getters and Setters
    public List<Flight> getFlights() {
        return flights;
    }
    
    public void setFlights(List<Flight> flights) {
        this.flights = flights;
    }
    
    public Integer getTotalResults() {
        return totalResults;
    }
    
    public void setTotalResults(Integer totalResults) {
        this.totalResults = totalResults;
    }
    
    public String getSearchId() {
        return searchId;
    }
    
    public void setSearchId(String searchId) {
        this.searchId = searchId;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public String getCurrency() {
        return currency;
    }
    
    public void setCurrency(String currency) {
        this.currency = currency;
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
    
    public String getDepartureDate() {
        return departureDate;
    }
    
    public void setDepartureDate(String departureDate) {
        this.departureDate = departureDate;
    }
    
    public String getReturnDate() {
        return returnDate;
    }
    
    public void setReturnDate(String returnDate) {
        this.returnDate = returnDate;
    }
    
    public Integer getPassengers() {
        return passengers;
    }
    
    public void setPassengers(Integer passengers) {
        this.passengers = passengers;
    }
    
    public String getCabinClass() {
        return cabinClass;
    }
    
    public void setCabinClass(String cabinClass) {
        this.cabinClass = cabinClass;
    }
    
    /**
     * Check if the search was successful.
     */
    public boolean isSuccessful() {
        return "OK".equals(status) || "SUCCESS".equals(status);
    }
    
    /**
     * Check if there are flights in the response.
     */
    public boolean hasFlights() {
        return flights != null && !flights.isEmpty();
    }
    
    @Override
    public String toString() {
        return "FlightSearchResponse{" +
                "flights=" + (flights != null ? flights.size() : 0) + " flights" +
                ", totalResults=" + totalResults +
                ", searchId='" + searchId + '\'' +
                ", status='" + status + '\'' +
                ", errorMessage='" + errorMessage + '\'' +
                ", currency='" + currency + '\'' +
                ", origin='" + origin + '\'' +
                ", destination='" + destination + '\'' +
                ", departureDate='" + departureDate + '\'' +
                ", returnDate='" + returnDate + '\'' +
                ", passengers=" + passengers +
                ", cabinClass='" + cabinClass + '\'' +
                '}';
    }
}