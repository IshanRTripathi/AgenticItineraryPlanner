package com.tripplanner.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;

/**
 * Booking result DTO for booking agent responses.
 */
public class BookingResult {
    
    @JsonProperty("bookingId")
    private String bookingId;
    
    @JsonProperty("bookingType")
    private String bookingType; // "hotel", "flight", "activity"
    
    @JsonProperty("status")
    private String status; // "SUCCESS", "FAILED", "PENDING"
    
    @JsonProperty("confirmationNumber")
    private String confirmationNumber;
    
    @JsonProperty("bookingReference")
    private String bookingReference;
    
    @JsonProperty("paymentId")
    private String paymentId;
    
    @JsonProperty("totalAmount")
    private Double totalAmount;
    
    @JsonProperty("currency")
    private String currency;
    
    @JsonProperty("bookingDetails")
    private Map<String, Object> bookingDetails;
    
    @JsonProperty("timestamp")
    private LocalDateTime timestamp;
    
    @JsonProperty("errorMessage")
    private String errorMessage;
    
    @JsonProperty("errorCode")
    private String errorCode;
    
    @JsonProperty("provider")
    private String provider;
    
    @JsonProperty("cancellationPolicy")
    private String cancellationPolicy;
    
    @JsonProperty("contactInfo")
    private Map<String, String> contactInfo;
    
    public BookingResult() {
        this.bookingDetails = new HashMap<>();
        this.contactInfo = new HashMap<>();
        this.timestamp = LocalDateTime.now();
    }
    
    public BookingResult(String bookingType, String status) {
        this();
        this.bookingType = bookingType;
        this.status = status;
        this.bookingId = generateBookingId();
    }
    
    // Getters and Setters
    public String getBookingId() {
        return bookingId;
    }
    
    public void setBookingId(String bookingId) {
        this.bookingId = bookingId;
    }
    
    public String getBookingType() {
        return bookingType;
    }
    
    public void setBookingType(String bookingType) {
        this.bookingType = bookingType;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getConfirmationNumber() {
        return confirmationNumber;
    }
    
    public void setConfirmationNumber(String confirmationNumber) {
        this.confirmationNumber = confirmationNumber;
    }
    
    public String getBookingReference() {
        return bookingReference;
    }
    
    public void setBookingReference(String bookingReference) {
        this.bookingReference = bookingReference;
    }
    
    public String getPaymentId() {
        return paymentId;
    }
    
    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
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
    
    public Map<String, Object> getBookingDetails() {
        return bookingDetails;
    }
    
    public void setBookingDetails(Map<String, Object> bookingDetails) {
        this.bookingDetails = bookingDetails;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }
    
    public String getProvider() {
        return provider;
    }
    
    public void setProvider(String provider) {
        this.provider = provider;
    }
    
    public String getCancellationPolicy() {
        return cancellationPolicy;
    }
    
    public void setCancellationPolicy(String cancellationPolicy) {
        this.cancellationPolicy = cancellationPolicy;
    }
    
    public Map<String, String> getContactInfo() {
        return contactInfo;
    }
    
    public void setContactInfo(Map<String, String> contactInfo) {
        this.contactInfo = contactInfo;
    }
    
    /**
     * Add a booking detail.
     */
    public void addBookingDetail(String key, Object value) {
        if (bookingDetails == null) {
            bookingDetails = new HashMap<>();
        }
        bookingDetails.put(key, value);
    }
    
    /**
     * Add contact information.
     */
    public void addContactInfo(String key, String value) {
        if (contactInfo == null) {
            contactInfo = new HashMap<>();
        }
        contactInfo.put(key, value);
    }
    

    
    /**
     * Check if the booking was successful.
     */
    public boolean isSuccessful() {
        return "SUCCESS".equals(status);
    }
    
    /**
     * Check if the booking failed.
     */
    public boolean isFailed() {
        return "FAILED".equals(status);
    }
    
    /**
     * Check if the booking is pending.
     */
    public boolean isPending() {
        return "PENDING".equals(status);
    }
    
    /**
     * Get confirmation ID (alias for confirmation number).
     */
    public String getConfirmationId() {
        return confirmationNumber;
    }
    
    /**
     * Generate a unique booking ID.
     */
    private String generateBookingId() {
        return "BK_" + System.currentTimeMillis() + "_" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
    
    @Override
    public String toString() {
        return "BookingResult{" +
                "bookingId='" + bookingId + '\'' +
                ", bookingType='" + bookingType + '\'' +
                ", status='" + status + '\'' +
                ", confirmationNumber='" + confirmationNumber + '\'' +
                ", bookingReference='" + bookingReference + '\'' +
                ", paymentId='" + paymentId + '\'' +
                ", totalAmount=" + totalAmount +
                ", currency='" + currency + '\'' +
                ", bookingDetails=" + bookingDetails +
                ", timestamp=" + timestamp +
                ", errorMessage='" + errorMessage + '\'' +
                ", errorCode='" + errorCode + '\'' +
                ", provider='" + provider + '\'' +
                ", cancellationPolicy='" + cancellationPolicy + '\'' +
                ", contactInfo=" + contactInfo +
                '}';
    }
}