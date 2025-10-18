package com.tripplanner.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

/**
 * Booking confirmation response from booking APIs.
 */
public class BookingConfirmation {
    
    @JsonProperty("bookingReference")
    private String bookingReference;
    
    @JsonProperty("confirmationNumber")
    private String confirmationNumber;
    
    @JsonProperty("status")
    private String status;
    
    @JsonProperty("bookingId")
    private String bookingId;
    
    @JsonProperty("hotelId")
    private String hotelId;
    
    @JsonProperty("hotelName")
    private String hotelName;
    
    @JsonProperty("guestName")
    private String guestName;
    
    @JsonProperty("checkInDate")
    private String checkInDate;
    
    @JsonProperty("checkOutDate")
    private String checkOutDate;
    
    @JsonProperty("rooms")
    private Integer rooms;
    
    @JsonProperty("guests")
    private Integer guests;
    
    @JsonProperty("totalAmount")
    private Double totalAmount;
    
    @JsonProperty("currency")
    private String currency;
    
    @JsonProperty("paymentStatus")
    private String paymentStatus;
    
    @JsonProperty("paymentId")
    private String paymentId;
    
    @JsonProperty("bookingTimestamp")
    private LocalDateTime bookingTimestamp;
    
    @JsonProperty("cancellationPolicy")
    private String cancellationPolicy;
    
    @JsonProperty("specialRequests")
    private String specialRequests;
    
    @JsonProperty("contactEmail")
    private String contactEmail;
    
    @JsonProperty("contactPhone")
    private String contactPhone;
    
    @JsonProperty("errorMessage")
    private String errorMessage;
    
    public BookingConfirmation() {}
    
    public BookingConfirmation(String bookingReference, String status) {
        this.bookingReference = bookingReference;
        this.status = status;
        this.bookingTimestamp = LocalDateTime.now();
    }
    
    // Getters and Setters
    public String getBookingReference() {
        return bookingReference;
    }
    
    public void setBookingReference(String bookingReference) {
        this.bookingReference = bookingReference;
    }
    
    public String getConfirmationNumber() {
        return confirmationNumber;
    }
    
    public void setConfirmationNumber(String confirmationNumber) {
        this.confirmationNumber = confirmationNumber;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getBookingId() {
        return bookingId;
    }
    
    public void setBookingId(String bookingId) {
        this.bookingId = bookingId;
    }
    
    public String getHotelId() {
        return hotelId;
    }
    
    public void setHotelId(String hotelId) {
        this.hotelId = hotelId;
    }
    
    public String getHotelName() {
        return hotelName;
    }
    
    public void setHotelName(String hotelName) {
        this.hotelName = hotelName;
    }
    
    public String getGuestName() {
        return guestName;
    }
    
    public void setGuestName(String guestName) {
        this.guestName = guestName;
    }
    
    public String getCheckInDate() {
        return checkInDate;
    }
    
    public void setCheckInDate(String checkInDate) {
        this.checkInDate = checkInDate;
    }
    
    public String getCheckOutDate() {
        return checkOutDate;
    }
    
    public void setCheckOutDate(String checkOutDate) {
        this.checkOutDate = checkOutDate;
    }
    
    public Integer getRooms() {
        return rooms;
    }
    
    public void setRooms(Integer rooms) {
        this.rooms = rooms;
    }
    
    public Integer getGuests() {
        return guests;
    }
    
    public void setGuests(Integer guests) {
        this.guests = guests;
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
    
    public String getPaymentStatus() {
        return paymentStatus;
    }
    
    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }
    
    public String getPaymentId() {
        return paymentId;
    }
    
    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }
    
    public LocalDateTime getBookingTimestamp() {
        return bookingTimestamp;
    }
    
    public void setBookingTimestamp(LocalDateTime bookingTimestamp) {
        this.bookingTimestamp = bookingTimestamp;
    }
    
    public String getCancellationPolicy() {
        return cancellationPolicy;
    }
    
    public void setCancellationPolicy(String cancellationPolicy) {
        this.cancellationPolicy = cancellationPolicy;
    }
    
    public String getSpecialRequests() {
        return specialRequests;
    }
    
    public void setSpecialRequests(String specialRequests) {
        this.specialRequests = specialRequests;
    }
    
    public String getContactEmail() {
        return contactEmail;
    }
    
    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }
    
    public String getContactPhone() {
        return contactPhone;
    }
    
    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    /**
     * Check if the booking was successful.
     */
    public boolean isSuccessful() {
        return "CONFIRMED".equals(status) || "SUCCESS".equals(status);
    }
    
    /**
     * Check if the booking failed.
     */
    public boolean isFailed() {
        return "FAILED".equals(status) || "ERROR".equals(status);
    }
    
    /**
     * Check if the booking is pending.
     */
    public boolean isPending() {
        return "PENDING".equals(status) || "PROCESSING".equals(status);
    }
    
    @Override
    public String toString() {
        return "BookingConfirmation{" +
                "bookingReference='" + bookingReference + '\'' +
                ", confirmationNumber='" + confirmationNumber + '\'' +
                ", status='" + status + '\'' +
                ", bookingId='" + bookingId + '\'' +
                ", hotelId='" + hotelId + '\'' +
                ", hotelName='" + hotelName + '\'' +
                ", guestName='" + guestName + '\'' +
                ", checkInDate='" + checkInDate + '\'' +
                ", checkOutDate='" + checkOutDate + '\'' +
                ", rooms=" + rooms +
                ", guests=" + guests +
                ", totalAmount=" + totalAmount +
                ", currency='" + currency + '\'' +
                ", paymentStatus='" + paymentStatus + '\'' +
                ", paymentId='" + paymentId + '\'' +
                ", bookingTimestamp=" + bookingTimestamp +
                ", cancellationPolicy='" + cancellationPolicy + '\'' +
                ", specialRequests='" + specialRequests + '\'' +
                ", contactEmail='" + contactEmail + '\'' +
                ", contactPhone='" + contactPhone + '\'' +
                ", errorMessage='" + errorMessage + '\'' +
                '}';
    }
}