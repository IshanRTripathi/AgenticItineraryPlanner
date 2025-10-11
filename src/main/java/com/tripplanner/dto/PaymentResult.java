package com.tripplanner.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

/**
 * Payment result DTO for payment processing responses.
 */
public class PaymentResult {
    
    @JsonProperty("paymentId")
    private String paymentId;
    
    @JsonProperty("status")
    private String status; // "SUCCESS", "FAILED", "PENDING"
    
    @JsonProperty("amountPerPerson")
    private Double amount;
    
    @JsonProperty("currency")
    private String currency;
    
    @JsonProperty("transactionId")
    private String transactionId;
    
    @JsonProperty("paymentMethod")
    private String paymentMethod;
    
    @JsonProperty("cardLast4")
    private String cardLast4;
    
    @JsonProperty("cardType")
    private String cardType;
    
    @JsonProperty("processingFee")
    private Double processingFee;
    
    @JsonProperty("netAmount")
    private Double netAmount;
    
    @JsonProperty("timestamp")
    private LocalDateTime timestamp;
    
    @JsonProperty("errorCode")
    private String errorCode;
    
    @JsonProperty("errorMessage")
    private String errorMessage;
    
    @JsonProperty("receiptUrl")
    private String receiptUrl;
    
    @JsonProperty("refundable")
    private Boolean refundable;
    
    public PaymentResult() {
        this.timestamp = LocalDateTime.now();
        this.refundable = true;
    }
    
    public PaymentResult(String paymentId, String status, Double amount, String currency) {
        this();
        this.paymentId = paymentId;
        this.status = status;
        this.amount = amount;
        this.currency = currency;
    }
    
    // Getters and Setters
    public String getPaymentId() {
        return paymentId;
    }
    
    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
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
    
    public String getTransactionId() {
        return transactionId;
    }
    
    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }
    
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
    
    public Double getProcessingFee() {
        return processingFee;
    }
    
    public void setProcessingFee(Double processingFee) {
        this.processingFee = processingFee;
    }
    
    public Double getNetAmount() {
        return netAmount;
    }
    
    public void setNetAmount(Double netAmount) {
        this.netAmount = netAmount;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public String getReceiptUrl() {
        return receiptUrl;
    }
    
    public void setReceiptUrl(String receiptUrl) {
        this.receiptUrl = receiptUrl;
    }
    
    public Boolean getRefundable() {
        return refundable;
    }
    
    public void setRefundable(Boolean refundable) {
        this.refundable = refundable;
    }
    
    /**
     * Check if the payment was successful.
     */
    public boolean isSuccessful() {
        return "SUCCESS".equals(status);
    }
    
    /**
     * Check if the payment failed.
     */
    public boolean isFailed() {
        return "FAILED".equals(status);
    }
    
    /**
     * Check if the payment is pending.
     */
    public boolean isPending() {
        return "PENDING".equals(status);
    }
    
    @Override
    public String toString() {
        return "PaymentResult{" +
                "paymentId='" + paymentId + '\'' +
                ", status='" + status + '\'' +
                ", amountPerPerson=" + amount +
                ", currency='" + currency + '\'' +
                ", transactionId='" + transactionId + '\'' +
                ", paymentMethod='" + paymentMethod + '\'' +
                ", cardLast4='" + cardLast4 + '\'' +
                ", cardType='" + cardType + '\'' +
                ", processingFee=" + processingFee +
                ", netAmount=" + netAmount +
                ", timestamp=" + timestamp +
                ", errorCode='" + errorCode + '\'' +
                ", errorMessage='" + errorMessage + '\'' +
                ", receiptUrl='" + receiptUrl + '\'' +
                ", refundable=" + refundable +
                '}';
    }
}