package com.tripplanner.api;

import com.tripplanner.security.GoogleUserPrincipal;
import com.tripplanner.service.BookingService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for booking and payment operations.
 */
@RestController
@RequestMapping("/api/v1")
public class BookingController {
    
    private static final Logger logger = LoggerFactory.getLogger(BookingController.class);
    
    private final BookingService bookingService;
    
    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }
    
    /**
     * Create Razorpay order for payment.
     */
    @PostMapping("/payments/razorpay/order")
    public ResponseEntity<RazorpayOrderRes> createRazorpayOrder(
            @Valid @RequestBody RazorpayOrderReq request,
            @AuthenticationPrincipal GoogleUserPrincipal user) {
        
        logger.info("Creating Razorpay order for user: {}, amount: {} {}", 
                   user.getUserId(), request.amount(), request.currency());
        
        RazorpayOrderRes response = bookingService.createRazorpayOrder(request, user);
        
        logger.info("Razorpay order created: {}", response.orderId());
        return ResponseEntity.ok(response);
    }
    
    /**
     * Handle Razorpay webhook for payment notifications.
     */
    @PostMapping("/payments/razorpay/webhook")
    public ResponseEntity<Void> handleRazorpayWebhook(
            HttpServletRequest request,
            @RequestBody String body) {
        
        logger.info("Received Razorpay webhook");
        
        bookingService.handleRazorpayWebhook(request, body);
        
        logger.info("Razorpay webhook processed successfully");
        return ResponseEntity.ok().build();
    }
    
    /**
     * Execute booking with provider after payment confirmation.
     */
    @PostMapping("/providers/{vertical}/{provider}:book")
    public ResponseEntity<BookingRes> executeProviderBooking(
            @PathVariable String vertical,
            @PathVariable String provider,
            @Valid @RequestBody ProviderBookReq request,
            @AuthenticationPrincipal GoogleUserPrincipal user) {
        
        logger.info("Executing {} booking with provider {} for user: {}", 
                   vertical, provider, user.getUserId());
        
        BookingRes response = bookingService.executeProviderBooking(vertical, provider, request, user);
        
        logger.info("Provider booking executed: {}", response.bookingId());
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get booking by ID.
     */
    @GetMapping("/bookings/{bookingId}")
    public ResponseEntity<BookingRes> getBooking(
            @PathVariable String bookingId,
            @AuthenticationPrincipal GoogleUserPrincipal user) {
        
        logger.debug("Getting booking: {} for user: {}", bookingId, user.getUserId());
        
        BookingRes booking = bookingService.getBooking(bookingId, user);
        return ResponseEntity.ok(booking);
    }
    
    /**
     * Get user's bookings.
     */
    @GetMapping("/bookings")
    public ResponseEntity<java.util.List<BookingRes>> getUserBookings(
            @AuthenticationPrincipal GoogleUserPrincipal user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        logger.debug("Getting bookings for user: {}, page: {}, size: {}", 
                    user.getUserId(), page, size);
        
        java.util.List<BookingRes> bookings = bookingService.getUserBookings(user, page, size);
        return ResponseEntity.ok(bookings);
    }
    
    /**
     * Cancel a booking.
     */
    @PostMapping("/bookings/{bookingId}:cancel")
    public ResponseEntity<Void> cancelBooking(
            @PathVariable String bookingId,
            @Valid @RequestBody CancelBookingReq request,
            @AuthenticationPrincipal GoogleUserPrincipal user) {
        
        logger.info("Canceling booking: {} for user: {}", bookingId, user.getUserId());
        
        bookingService.cancelBooking(bookingId, request, user);
        
        logger.info("Booking canceled: {}", bookingId);
        return ResponseEntity.ok().build();
    }
    
    // Request/Response DTOs
    
    /**
     * Request DTO for creating Razorpay order.
     */
    public record RazorpayOrderReq(
            @NotBlank(message = "Item type is required")
            String itemType,
            
            @NotBlank(message = "Itinerary ID is required")
            String itineraryId,
            
            @NotNull(message = "Amount is required")
            @Positive(message = "Amount must be positive")
            Long amount, // Amount in paise for Razorpay
            
            @NotBlank(message = "Currency is required")
            String currency,
            
            Map<String, Object> meta
    ) {}
    
    /**
     * Response DTO for Razorpay order creation.
     */
    public record RazorpayOrderRes(
            String orderId,
            Long amount,
            String currency,
            String receipt
    ) {}
    
    /**
     * Request DTO for provider booking.
     */
    public record ProviderBookReq(
            @NotNull(message = "Payment details are required")
            PaymentDetails payment,
            
            @NotNull(message = "Item details are required")
            ItemDetails item,
            
            @NotBlank(message = "Itinerary ID is required")
            String itineraryId
    ) {}
    
    /**
     * Payment details for booking.
     */
    public record PaymentDetails(
            @NotBlank(message = "Order ID is required")
            String orderId,
            
            @NotBlank(message = "Payment ID is required")
            String paymentId,
            
            @NotBlank(message = "Signature is required")
            String signature
    ) {}
    
    /**
     * Item details for booking.
     */
    public record ItemDetails(
            @NotBlank(message = "Token is required")
            String token,
            
            Map<String, Object> details
    ) {}
    
    /**
     * Response DTO for booking operations.
     */
    public record BookingRes(
            String bookingId,
            String status,
            String providerConfirmationId,
            String itineraryId,
            java.time.Instant createdAt,
            Map<String, Object> details
    ) {}
    
    /**
     * Request DTO for booking cancellation.
     */
    public record CancelBookingReq(
            String reason,
            Map<String, Object> meta
    ) {}
}

