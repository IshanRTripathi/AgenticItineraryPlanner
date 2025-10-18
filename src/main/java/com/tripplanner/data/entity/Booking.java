package com.tripplanner.data.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.Objects;

/**
 * Booking entity representing booking transactions.
 * Stored in Firestore via BookingRepository
 */
@Entity
@Table(name = "bookings")
public class Booking {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank
    @Column(name = "bookingId", unique = true)
    private String bookingId;
    
    @NotBlank
    @Column(name = "userId")
    private String userId;
    
    @NotBlank
    @Column(name = "itineraryId")
    private String itineraryId;
    
    @NotNull
    @Embedded
    private BookingItem item;
    
    @NotNull
    @Embedded
    private BookingPrice price;
    
    @Embedded
    private RazorpayDetails razorpay;
    
    @Embedded
    private ProviderDetails provider;
    
    @Column(name = "status")
    private BookingStatus status = BookingStatus.INIT;
    
    @Column(name = "createdAt")
    private Instant createdAt;
    
    @Column(name = "updatedAt")
    private Instant updatedAt;
    
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadataJson;
    
    public Booking() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        this.bookingId = generateBookingId();
    }
    
    /**
     * Generate a unique booking ID.
     */
    private String generateBookingId() {
        return "BK_" + System.currentTimeMillis() + "_" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
    
    public void updateTimestamp() {
        this.updatedAt = Instant.now();
    }
    
    public void updateStatus(BookingStatus newStatus) {
        this.status = newStatus;
        updateTimestamp();
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getBookingId() {
        return bookingId;
    }
    
    public void setBookingId(String bookingId) {
        this.bookingId = bookingId;
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
    
    public String getMetadataJson() {
        return metadataJson;
    }
    
    public void setMetadataJson(String metadataJson) {
        this.metadataJson = metadataJson;
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
    @Embeddable
    public static class BookingItem {
        @Column(name = "item_type")
        private String type; // hotel, flight, activity, transport
        
        @Column(name = "item_provider")
        private String provider;
        
        @Column(name = "item_token")
        private String token; // Provider-specific booking token
        
        @Column(name = "item_details", columnDefinition = "TEXT")
        private String detailsJson; // Store as JSON string instead of Map
        
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
        public String getDetailsJson() { return detailsJson; }
        public void setDetailsJson(String detailsJson) { this.detailsJson = detailsJson; }
    }
    
    @Embeddable
    public static class BookingPrice {
        @Column(name = "price_amount")
        private double amount;
        
        @Column(name = "price_currency")
        private String currency;
        
        @Column(name = "breakdown", columnDefinition = "TEXT")
        private String breakdownJson;
        
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
        public String getBreakdownJson() { return breakdownJson; }
        public void setBreakdownJson(String breakdownJson) { this.breakdownJson = breakdownJson; }
    }
    
    @Embeddable
    public static class RazorpayDetails {
        @Column(name = "orderId")
        private String orderId;
        
        @Column(name = "paymentId")
        private String paymentId;
        
        @Column(name = "signature")
        private String signature;
        
        @Column(name = "receipt")
        private String receipt;
        
        @Column(name = "razorpay_amount")
        private long amount; // Amount in paise
        
        @Column(name = "razorpay_currency")
        private String currency;
        
        @Column(name = "razorpay_status")
        private String status;
        
        @Column(name = "method")
        private String method;
        
        @Column(name = "razorpay_created_at")
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
    
    @Embeddable
    public static class ProviderDetails {
        @Column(name = "confirmationId")
        private String confirmationId;
        
        @Column(name = "provider_status")
        private String status;
        
        @Column(name = "bookingReference")
        private String bookingReference;
        
        @Column(name = "voucher")
        private String voucher;
        
        @Column(name = "cancellationPolicy")
        private String cancellationPolicy;
        
        @Column(name = "contact_info", columnDefinition = "TEXT")
        private String contactInfoJson;
        
        @Column(name = "additional_info", columnDefinition = "TEXT")
        private String additionalInfoJson;
        
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
        public String getContactInfoJson() { return contactInfoJson; }
        public void setContactInfoJson(String contactInfoJson) { this.contactInfoJson = contactInfoJson; }
        public String getAdditionalInfoJson() { return additionalInfoJson; }
        public void setAdditionalInfoJson(String additionalInfoJson) { this.additionalInfoJson = additionalInfoJson; }
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

