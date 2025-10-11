package com.tripplanner.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Payment request DTO for creating payment orders.
 */
public class PaymentRequest {
    
    @JsonProperty("amountPerPerson")
    private Double amount;
    
    @JsonProperty("currency")
    private String currency;
    
    @JsonProperty("bookingType")
    private String bookingType;
    
    @JsonProperty("userId")
    private String userId;
    
    @JsonProperty("itineraryId")
    private String itineraryId;
    
    @JsonProperty("description")
    private String description;
    
    @JsonProperty("customerEmail")
    private String customerEmail;
    
    @JsonProperty("customerPhone")
    private String customerPhone;
    
    @JsonProperty("customerName")
    private String customerName;
    
    @JsonProperty("notes")
    private java.util.Map<String, String> notes;
    
    public PaymentRequest() {
        this.notes = new java.util.HashMap<>();
        this.currency = "INR"; // Default currency
    }
    
    public PaymentRequest(Double amount, String currency, String bookingType) {
        this();
        this.amount = amount;
        this.currency = currency;
        this.bookingType = bookingType;
    }
    
    // Getters and Setters
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
    
    public String getCustomerEmail() {
        return customerEmail;
    }
    
    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }
    
    public String getCustomerPhone() {
        return customerPhone;
    }
    
    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
    }
    
    public String getCustomerName() {
        return customerName;
    }
    
    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }
    
    public java.util.Map<String, String> getNotes() {
        return notes;
    }
    
    public void setNotes(java.util.Map<String, String> notes) {
        this.notes = notes;
    }
    
    /**
     * Add a note to the payment request.
     */
    public void addNote(String key, String value) {
        if (this.notes == null) {
            this.notes = new java.util.HashMap<>();
        }
        this.notes.put(key, value);
    }
    
    /**
     * Get amountPerPerson in smallest currency unit (paise for INR).
     */
    public Integer getAmountInSmallestUnit() {
        if (amount == null) {
            return null;
        }
        return (int) (amount * 100); // Convert to paise
    }
    
    @Override
    public String toString() {
        return "PaymentRequest{" +
                "amountPerPerson=" + amount +
                ", currency='" + currency + '\'' +
                ", bookingType='" + bookingType + '\'' +
                ", userId='" + userId + '\'' +
                ", itineraryId='" + itineraryId + '\'' +
                ", description='" + description + '\'' +
                ", customerEmail='" + customerEmail + '\'' +
                ", customerName='" + customerName + '\'' +
                '}';
    }
}