package com.tripplanner.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.tripplanner.dto.deserializers.BookingInfoDeserializer;

/**
 * Node booking information.
 */
public class NodeLinks {
    
    @JsonProperty("booking")
    @JsonDeserialize(using = BookingInfoDeserializer.class)
    private BookingInfo booking;
    
    public NodeLinks() {}
    
    public NodeLinks(BookingInfo booking) {
        this.booking = booking;
    }
    
    // Getters and Setters
    public BookingInfo getBooking() {
        return booking;
    }
    
    public void setBooking(BookingInfo booking) {
        this.booking = booking;
    }
    
    /**
     * Booking information nested class
     */
    public static class BookingInfo {
        @JsonProperty("refNumber")
        private String refNumber;
        
        @JsonProperty("status")
        private String status; // "NOT_REQUIRED", "REQUIRED", "BOOKED"
        
        @JsonProperty("details")
        private String details;
        
        public BookingInfo() {}
        
        public BookingInfo(String refNumber, String status, String details) {
            this.refNumber = refNumber;
            this.status = status;
            this.details = details;
        }
        
        public String getRefNumber() {
            return refNumber;
        }
        
        public void setRefNumber(String refNumber) {
            this.refNumber = refNumber;
        }
        
        public String getStatus() {
            return status;
        }
        
        public void setStatus(String status) {
            this.status = status;
        }
        
        public String getDetails() {
            return details;
        }
        
        public void setDetails(String details) {
            this.details = details;
        }
        
        @Override
        public String toString() {
            return "BookingInfo{" +
                    "refNumber='" + refNumber + '\'' +
                    ", status='" + status + '\'' +
                    ", details='" + details + '\'' +
                    '}';
        }
    }
    
    @Override
    public String toString() {
        return "NodeLinks{" +
                "booking=" + booking +
                '}';
    }
}
