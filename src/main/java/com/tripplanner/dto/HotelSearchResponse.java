package com.tripplanner.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.ArrayList;

/**
 * Hotel search response from Booking.com API.
 */
public class HotelSearchResponse {
    
    @JsonProperty("hotels")
    private List<Hotel> hotels;
    
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
    
    @JsonProperty("location")
    private String location;
    
    @JsonProperty("checkInDate")
    private String checkInDate;
    
    @JsonProperty("checkOutDate")
    private String checkOutDate;
    
    public HotelSearchResponse() {
        this.hotels = new ArrayList<>();
    }
    
    // Getters and Setters
    public List<Hotel> getHotels() {
        return hotels;
    }
    
    public void setHotels(List<Hotel> hotels) {
        this.hotels = hotels;
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
    
    public String getLocation() {
        return location;
    }
    
    public void setLocation(String location) {
        this.location = location;
    }
    
    public String getCheckInDate() {
        return checkInDate;
    }
    
    public void setCheckInDate(String checkInDate) {
        this.checkInDate = checkInDate;
    }
    
    public String getCheckOutDate() {
        return checkOutDate;
    }
    
    public void setCheckOutDate(String checkOutDate) {
        this.checkOutDate = checkOutDate;
    }
    
    /**
     * Check if the search was successful.
     */
    public boolean isSuccessful() {
        return "OK".equals(status) || "SUCCESS".equals(status);
    }
    
    /**
     * Check if there are hotels in the response.
     */
    public boolean hasHotels() {
        return hotels != null && !hotels.isEmpty();
    }
    
    @Override
    public String toString() {
        return "HotelSearchResponse{" +
                "hotels=" + (hotels != null ? hotels.size() : 0) + " hotels" +
                ", totalResults=" + totalResults +
                ", searchId='" + searchId + '\'' +
                ", status='" + status + '\'' +
                ", errorMessage='" + errorMessage + '\'' +
                ", currency='" + currency + '\'' +
                ", location='" + location + '\'' +
                ", checkInDate='" + checkInDate + '\'' +
                ", checkOutDate='" + checkOutDate + '\'' +
                '}';
    }
}