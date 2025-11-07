package com.tripplanner.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/**
 * Request DTO for creating a booking record.
 * Used by the frontend to record bookings made through provider iframes.
 */
public class CreateBookingRecordRequest {
    
    @NotBlank(message = "User ID is required")
    private String userId;
    
    @NotBlank(message = "Itinerary ID is required")
    private String itineraryId;
    
    private String nodeId; // Optional - for linking booking to specific node
    
    @NotBlank(message = "Provider name is required")
    private String providerName;
    
    @NotBlank(message = "Confirmation number is required")
    private String confirmationNumber;
    
    @Positive(message = "Total amount must be positive")
    private Double totalAmount;
    
    private String currency = "USD";
    
    private String bookingType; // "flight", "hotel", "activity", etc.
    
    private String itemName; // Name of the booked item
    
    private String notes; // Additional notes
    
    public CreateBookingRecordRequest() {}
    
    // Getters and Setters
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
    
    public String getNodeId() {
        return nodeId;
    }
    
    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }
    
    public String getProviderName() {
        return providerName;
    }
    
    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }
    
    public String getConfirmationNumber() {
        return confirmationNumber;
    }
    
    public void setConfirmationNumber(String confirmationNumber) {
        this.confirmationNumber = confirmationNumber;
    }
    
    public Double getTotalAmount() {
        return totalAmount;
    }
    
    public void setTotalAmount(Double totalAmount) {
        this.totalAmount = totalAmount;
    }
    
    public String getCurrency() {
        return currency;
    }
    
    public void setCurrency(String currency) {
        this.currency = currency;
    }
    
    public String getBookingType() {
        return bookingType;
    }
    
    public void setBookingType(String bookingType) {
        this.bookingType = bookingType;
    }
    
    public String getItemName() {
        return itemName;
    }
    
    public void setItemName(String itemName) {
        this.itemName = itemName;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
    }
    
    @Override
    public String toString() {
        return "CreateBookingRecordRequest{" +
                "userId='" + userId + '\'' +
                ", itineraryId='" + itineraryId + '\'' +
                ", nodeId='" + nodeId + '\'' +
                ", providerName='" + providerName + '\'' +
                ", confirmationNumber='" + confirmationNumber + '\'' +
                ", totalAmount=" + totalAmount +
                ", currency='" + currency + '\'' +
                ", bookingType='" + bookingType + '\'' +
                ", itemName='" + itemName + '\'' +
                '}';
    }
}
