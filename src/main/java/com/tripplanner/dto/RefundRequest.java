package com.tripplanner.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Refund request DTO for processing payment refunds.
 */
public class RefundRequest {
    
    @JsonProperty("paymentId")
    private String paymentId;
    
    @JsonProperty("amountPerPerson")
    private Double amount; // null for full refund
    
    @JsonProperty("currency")
    private String currency;
    
    @JsonProperty("reason")
    private String reason;
    
    @JsonProperty("userId")
    private String userId;
    
    @JsonProperty("itineraryId")
    private String itineraryId;
    
    @JsonProperty("bookingReference")
    private String bookingReference;
    
    public RefundRequest() {}
    
    public RefundRequest(String paymentId, Double amount, String reason) {
        this.paymentId = paymentId;
        this.amount = amount;
        this.reason = reason;
    }
    
    // Getters and Setters
    public String getPaymentId() {
        return paymentId;
    }
    
    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
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
    
    public String getReason() {
        return reason;
    }
    
    public void setReason(String reason) {
        this.reason = reason;
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
    
    public String getBookingReference() {
        return bookingReference;
    }
    
    public void setBookingReference(String bookingReference) {
        this.bookingReference = bookingReference;
    }
    
    /**
     * Check if this is a full refund request.
     */
    public boolean isFullRefund() {
        return amount == null;
    }
    
    /**
     * Check if this is a partial refund request.
     */
    public boolean isPartialRefund() {
        return amount != null && amount > 0;
    }
    
    @Override
    public String toString() {
        return "RefundRequest{" +
                "paymentId='" + paymentId + '\'' +
                ", amountPerPerson=" + amount +
                ", currency='" + currency + '\'' +
                ", reason='" + reason + '\'' +
                ", userId='" + userId + '\'' +
                ", itineraryId='" + itineraryId + '\'' +
                ", bookingReference='" + bookingReference + '\'' +
                '}';
    }
}