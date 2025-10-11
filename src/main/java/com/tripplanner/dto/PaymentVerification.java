package com.tripplanner.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Payment verification DTO for verifying payment signatures.
 */
public class PaymentVerification {
    
    @JsonProperty("orderId")
    private String orderId;
    
    @JsonProperty("paymentId")
    private String paymentId;
    
    @JsonProperty("signature")
    private String signature;
    
    @JsonProperty("status")
    private String status;
    
    @JsonProperty("errorMessage")
    private String errorMessage;
    
    public PaymentVerification() {}
    
    public PaymentVerification(String orderId, String paymentId, String signature) {
        this.orderId = orderId;
        this.paymentId = paymentId;
        this.signature = signature;
    }
    
    // Getters and Setters
    public String getOrderId() {
        return orderId;
    }
    
    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }
    
    public String getPaymentId() {
        return paymentId;
    }
    
    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }
    
    public String getSignature() {
        return signature;
    }
    
    public void setSignature(String signature) {
        this.signature = signature;
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
    
    /**
     * Check if verification is successful.
     */
    public boolean isVerified() {
        return "SUCCESS".equals(status);
    }
    
    /**
     * Check if verification failed.
     */
    public boolean isFailed() {
        return "FAILED".equals(status);
    }
    
    @Override
    public String toString() {
        return "PaymentVerification{" +
                "orderId='" + orderId + '\'' +
                ", paymentId='" + paymentId + '\'' +
                ", signature='" + signature + '\'' +
                ", status='" + status + '\'' +
                ", errorMessage='" + errorMessage + '\'' +
                '}';
    }
}