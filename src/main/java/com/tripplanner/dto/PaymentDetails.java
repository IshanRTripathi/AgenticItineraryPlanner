package com.tripplanner.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Payment details DTO for storing payment information
 */
public class PaymentDetails {
    
    @JsonProperty("paymentMethod")
    private String paymentMethod;
    
    @JsonProperty("cardLast4")
    private String cardLast4;
    
    @JsonProperty("cardType")
    private String cardType;
    
    @JsonProperty("paymentId")
    private String paymentId;
    
    @JsonProperty("transactionId")
    private String transactionId;
    
    @JsonProperty("paymentStatus")
    private String paymentStatus;
    
    @JsonProperty("paymentTimestamp")
    private Long paymentTimestamp;
    
    @JsonProperty("amountPerPerson")
    private Double amount;
    
    @JsonProperty("currency")
    private String currency;
    
    public PaymentDetails() {}
    
    public PaymentDetails(String paymentMethod, String paymentId, String paymentStatus) {
        this.paymentMethod = paymentMethod;
        this.paymentId = paymentId;
        this.paymentStatus = paymentStatus;
        this.paymentTimestamp = System.currentTimeMillis();
    }
    
    // Getters and Setters
    public String getPaymentMethod() {
        return paymentMethod;
    }
    
    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
    
    public String getCardLast4() {
        return cardLast4;
    }
    
    public void setCardLast4(String cardLast4) {
        this.cardLast4 = cardLast4;
    }
    
    public String getCardType() {
        return cardType;
    }
    
    public void setCardType(String cardType) {
        this.cardType = cardType;
    }
    
    public String getPaymentId() {
        return paymentId;
    }
    
    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }
    
    public String getTransactionId() {
        return transactionId;
    }
    
    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }
    
    public String getPaymentStatus() {
        return paymentStatus;
    }
    
    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }
    
    public Long getPaymentTimestamp() {
        return paymentTimestamp;
    }
    
    public void setPaymentTimestamp(Long paymentTimestamp) {
        this.paymentTimestamp = paymentTimestamp;
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
    
    @Override
    public String toString() {
        return "PaymentDetails{" +
                "paymentMethod='" + paymentMethod + '\'' +
                ", cardLast4='" + cardLast4 + '\'' +
                ", cardType='" + cardType + '\'' +
                ", paymentId='" + paymentId + '\'' +
                ", transactionId='" + transactionId + '\'' +
                ", paymentStatus='" + paymentStatus + '\'' +
                ", paymentTimestamp=" + paymentTimestamp +
                ", amountPerPerson=" + amount +
                ", currency='" + currency + '\'' +
                '}';
    }
}