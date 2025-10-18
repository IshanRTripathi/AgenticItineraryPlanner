package com.tripplanner.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

/**
 * Refund result DTO for payment refund responses.
 */
public class RefundResult {
    
    @JsonProperty("refundId")
    private String refundId;
    
    @JsonProperty("paymentId")
    private String paymentId;
    
    @JsonProperty("status")
    private String status; // "SUCCESS", "FAILED", "PENDING"
    
    @JsonProperty("amountPerPerson")
    private Double amount;
    
    @JsonProperty("currency")
    private String currency;
    
    @JsonProperty("refundAmount")
    private Double refundAmount;
    
    @JsonProperty("processingFee")
    private Double processingFee;
    
    @JsonProperty("netRefund")
    private Double netRefund;
    
    @JsonProperty("reason")
    private String reason;
    
    @JsonProperty("timestamp")
    private LocalDateTime timestamp;
    
    @JsonProperty("estimatedArrival")
    private String estimatedArrival; // e.g., "3-5 business days"
    
    @JsonProperty("errorCode")
    private String errorCode;
    
    @JsonProperty("errorMessage")
    private String errorMessage;
    
    @JsonProperty("receiptUrl")
    private String receiptUrl;
    
    public RefundResult() {
        this.timestamp = LocalDateTime.now();
    }
    
    public RefundResult(String refundId, String paymentId, String status, Double amount, String currency) {
        this();
        this.refundId = refundId;
        this.paymentId = paymentId;
        this.status = status;
        this.amount = amount;
        this.currency = currency;
    }
    
    // Getters and Setters
    public String getRefundId() {
        return refundId;
    }
    
    public void setRefundId(String refundId) {
        this.refundId = refundId;
    }
    
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
    
    public Double getRefundAmount() {
        return refundAmount;
    }
    
    public void setRefundAmount(Double refundAmount) {
        this.refundAmount = refundAmount;
    }
    
    public Double getProcessingFee() {
        return processingFee;
    }
    
    public void setProcessingFee(Double processingFee) {
        this.processingFee = processingFee;
    }
    
    public Double getNetRefund() {
        return netRefund;
    }
    
    public void setNetRefund(Double netRefund) {
        this.netRefund = netRefund;
    }
    
    public String getReason() {
        return reason;
    }
    
    public void setReason(String reason) {
        this.reason = reason;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getEstimatedArrival() {
        return estimatedArrival;
    }
    
    public void setEstimatedArrival(String estimatedArrival) {
        this.estimatedArrival = estimatedArrival;
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
    
    /**
     * Check if the refund was successful.
     */
    public boolean isSuccessful() {
        return "SUCCESS".equals(status);
    }
    
    /**
     * Check if the refund failed.
     */
    public boolean isFailed() {
        return "FAILED".equals(status);
    }
    
    /**
     * Check if the refund is pending.
     */
    public boolean isPending() {
        return "PENDING".equals(status);
    }
    
    @Override
    public String toString() {
        return "RefundResult{" +
                "refundId='" + refundId + '\'' +
                ", paymentId='" + paymentId + '\'' +
                ", status='" + status + '\'' +
                ", amountPerPerson=" + amount +
                ", currency='" + currency + '\'' +
                ", refundAmount=" + refundAmount +
                ", processingFee=" + processingFee +
                ", netRefund=" + netRefund +
                ", reason='" + reason + '\'' +
                ", timestamp=" + timestamp +
                ", estimatedArrival='" + estimatedArrival + '\'' +
                ", errorCode='" + errorCode + '\'' +
                ", errorMessage='" + errorMessage + '\'' +
                ", receiptUrl='" + receiptUrl + '\'' +
                '}';
    }
}