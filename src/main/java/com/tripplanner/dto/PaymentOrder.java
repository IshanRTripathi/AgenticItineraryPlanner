package com.tripplanner.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

/**
 * Payment order DTO for payment processing.
 */
public class PaymentOrder {
    
    @JsonProperty("orderId")
    private String orderId;
    
    @JsonProperty("amountPerPerson")
    private Double amount;
    
    @JsonProperty("currency")
    private String currency;
    
    @JsonProperty("status")
    private String status;
    
    @JsonProperty("createdAt")
    private LocalDateTime createdAt;
    
    @JsonProperty("bookingType")
    private String bookingType;
    
    @JsonProperty("userId")
    private String userId;
    
    @JsonProperty("itineraryId")
    private String itineraryId;
    
    @JsonProperty("description")
    private String description;
    
    @JsonProperty("receipt")
    private String receipt;
    
    @JsonProperty("notes")
    private java.util.Map<String, String> notes;
    
    public PaymentOrder() {
        this.notes = new java.util.HashMap<>();
        this.createdAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public String getOrderId() {
        return orderId;
    }
    
    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }
    
    public Double getAmount() {
        return amount;
    }
    
    public void setAmount(Double amount) {
        this.amount = amount;
    }
    
    public String getCurrency() {
        return currency;
    }
    
    public void setCurrency(String currency) {
        this.currency = currency;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public String getBookingType() {
        return bookingType;
    }
    
    public void setBookingType(String bookingType) {
        this.bookingType = bookingType;
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
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getReceipt() {
        return receipt;
    }
    
    public void setReceipt(String receipt) {
        this.receipt = receipt;
    }
    
    public java.util.Map<String, String> getNotes() {
        return notes;
    }
    
    public void setNotes(java.util.Map<String, String> notes) {
        this.notes = notes;
    }
    
    /**
     * Check if the payment order is created successfully.
     */
    public boolean isCreated() {
        return "created".equals(status);
    }
    
    /**
     * Check if the payment order is paid.
     */
    public boolean isPaid() {
        return "paid".equals(status);
    }
    
    @Override
    public String toString() {
        return "PaymentOrder{" +
                "orderId='" + orderId + '\'' +
                ", amountPerPerson=" + amount +
                ", currency='" + currency + '\'' +
                ", status='" + status + '\'' +
                ", createdAt=" + createdAt +
                ", bookingType='" + bookingType + '\'' +
                ", userId='" + userId + '\'' +
                ", itineraryId='" + itineraryId + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}