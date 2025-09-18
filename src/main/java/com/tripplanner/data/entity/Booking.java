package com.tripplanner.data.entity;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.firestore.annotation.PropertyName;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * Booking entity representing booking transactions.
 * Stored in Firestore collection: bookings/{bookingId}
 */
public class Booking {
    
    @DocumentId
    private String id;
    
    @NotBlank
    @PropertyName("userId")
    private String userId;
    
    @NotBlank
    @PropertyName("itineraryId")
    private String itineraryId;
    
    @NotNull
    @PropertyName("item")
    private BookingItem item;
    
    @NotNull
    @PropertyName("price")
    private BookingPrice price;
    
    @PropertyName("razorpay")
    private RazorpayDetails razorpay;
    
    @PropertyName("provider")
    private ProviderDetails provider;
    
    @PropertyName("status")
    private BookingStatus status = BookingStatus.INIT;
    
    @PropertyName("createdAt")
    private Instant createdAt;
    
    @PropertyName("updatedAt")
    private Instant updatedAt;
    
    @PropertyName("metadata")
    private Map<String, Object> metadata;
    
    public Booking() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }
    
    public void updateTimestamp() {
        this.updatedAt = Instant.now();
    }
    
    public void updateStatus(BookingStatus newStatus) {
        this.status = newStatus;
        updateTimestamp();
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
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
    
    public BookingItem getItem() {
        return item;
    }
    
    public void setItem(BookingItem item) {
        this.item = item;
    }
    
    public BookingPrice getPrice() {
        return price;
    }
    
    public void setPrice(BookingPrice price) {
        this.price = price;
    }
    
    public RazorpayDetails getRazorpay() {
        return razorpay;
    }
    
    public void setRazorpay(RazorpayDetails razorpay) {
        this.razorpay = razorpay;
    }
    
    public ProviderDetails getProvider() {
        return provider;
    }
    
    public void setProvider(ProviderDetails provider) {
        this.provider = provider;
    }
    
    public BookingStatus getStatus() {
        return status;
    }
    
    public void setStatus(BookingStatus status) {
        this.status = status;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
    
    public Instant getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Booking booking = (Booking) o;
        return Objects.equals(id, booking.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "Booking{" +
                "id='" + id + '\'' +
                ", userId='" + userId + '\'' +
                ", itineraryId='" + itineraryId + '\'' +
                ", status=" + status +
                ", createdAt=" + createdAt +
                '}';
    }
    
    // Nested classes
    public static class BookingItem {
        @PropertyName("type")
        private String type; // hotel, flight, activity, transport
        
        @PropertyName("provider")
        private String provider;
        
        @PropertyName("token")
        private String token; // Provider-specific booking token
        
        @PropertyName("details")
        private Map<String, Object> details;
        
        public BookingItem() {}
        
        public BookingItem(String type, String provider, String token) {
            this.type = type;
            this.provider = provider;
            this.token = token;
        }
        
        // Getters and Setters
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getProvider() { return provider; }
        public void setProvider(String provider) { this.provider = provider; }
        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
        public Map<String, Object> getDetails() { return details; }
        public void setDetails(Map<String, Object> details) { this.details = details; }
    }
    
    public static class BookingPrice {
        @PropertyName("amount")
        private double amount;
        
        @PropertyName("currency")
        private String currency;
        
        @PropertyName("breakdown")
        private Map<String, Double> breakdown;
        
        public BookingPrice() {}
        
        public BookingPrice(double amount, String currency) {
            this.amount = amount;
            this.currency = currency;
        }
        
        // Getters and Setters
        public double getAmount() { return amount; }
        public void setAmount(double amount) { this.amount = amount; }
        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }
        public Map<String, Double> getBreakdown() { return breakdown; }
        public void setBreakdown(Map<String, Double> breakdown) { this.breakdown = breakdown; }
    }
    
    public static class RazorpayDetails {
        @PropertyName("orderId")
        private String orderId;
        
        @PropertyName("paymentId")
        private String paymentId;
        
        @PropertyName("signature")
        private String signature;
        
        @PropertyName("receipt")
        private String receipt;
        
        @PropertyName("amount")
        private long amount; // Amount in paise
        
        @PropertyName("currency")
        private String currency;
        
        @PropertyName("status")
        private String status;
        
        @PropertyName("method")
        private String method;
        
        @PropertyName("createdAt")
        private Instant createdAt;
        
        public RazorpayDetails() {}
        
        // Getters and Setters
        public String getOrderId() { return orderId; }
        public void setOrderId(String orderId) { this.orderId = orderId; }
        public String getPaymentId() { return paymentId; }
        public void setPaymentId(String paymentId) { this.paymentId = paymentId; }
        public String getSignature() { return signature; }
        public void setSignature(String signature) { this.signature = signature; }
        public String getReceipt() { return receipt; }
        public void setReceipt(String receipt) { this.receipt = receipt; }
        public long getAmount() { return amount; }
        public void setAmount(long amount) { this.amount = amount; }
        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getMethod() { return method; }
        public void setMethod(String method) { this.method = method; }
        public Instant getCreatedAt() { return createdAt; }
        public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    }
    
    public static class ProviderDetails {
        @PropertyName("confirmationId")
        private String confirmationId;
        
        @PropertyName("status")
        private String status;
        
        @PropertyName("bookingReference")
        private String bookingReference;
        
        @PropertyName("voucher")
        private String voucher;
        
        @PropertyName("cancellationPolicy")
        private String cancellationPolicy;
        
        @PropertyName("contactInfo")
        private Map<String, String> contactInfo;
        
        @PropertyName("additionalInfo")
        private Map<String, Object> additionalInfo;
        
        public ProviderDetails() {}
        
        // Getters and Setters
        public String getConfirmationId() { return confirmationId; }
        public void setConfirmationId(String confirmationId) { this.confirmationId = confirmationId; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getBookingReference() { return bookingReference; }
        public void setBookingReference(String bookingReference) { this.bookingReference = bookingReference; }
        public String getVoucher() { return voucher; }
        public void setVoucher(String voucher) { this.voucher = voucher; }
        public String getCancellationPolicy() { return cancellationPolicy; }
        public void setCancellationPolicy(String cancellationPolicy) { this.cancellationPolicy = cancellationPolicy; }
        public Map<String, String> getContactInfo() { return contactInfo; }
        public void setContactInfo(Map<String, String> contactInfo) { this.contactInfo = contactInfo; }
        public Map<String, Object> getAdditionalInfo() { return additionalInfo; }
        public void setAdditionalInfo(Map<String, Object> additionalInfo) { this.additionalInfo = additionalInfo; }
    }
    
    public enum BookingStatus {
        INIT,
        PAYMENT_ORDERED,
        PAYMENT_CONFIRMED,
        BOOKING_IN_PROGRESS,
        CONFIRMED,
        FAILED,
        CANCELLED,
        REFUNDED
    }
}

