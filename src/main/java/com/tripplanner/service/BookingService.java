package com.tripplanner.service;

import com.tripplanner.api.BookingController;
import com.tripplanner.data.repo.BookingRepository;
import com.tripplanner.security.GoogleUserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Service for booking and payment operations.
 */
@Service
@org.springframework.boot.autoconfigure.condition.ConditionalOnBean(BookingRepository.class)
public class BookingService {
    
    private static final Logger logger = LoggerFactory.getLogger(BookingService.class);
    
    private final RazorpayService razorpayService;
    private final BookingRepository bookingRepository;
    
    public BookingService(RazorpayService razorpayService, BookingRepository bookingRepository) {
        this.razorpayService = razorpayService;
        this.bookingRepository = bookingRepository;
    }
    
    /**
     * Create Razorpay order.
     */
    public BookingController.RazorpayOrderRes createRazorpayOrder(BookingController.RazorpayOrderReq request, GoogleUserPrincipal user) {
        logger.info("=== CREATE RAZORPAY ORDER REQUEST ===");
        logger.info("User ID: {}", user.getUserId());
        logger.info("Item Type: {}", request.itemType());
        logger.info("Itinerary ID: {}", request.itineraryId());
        logger.info("Amount: {} {}", request.amount(), request.currency());
        logger.info("Meta: {}", request.meta());
        
        try {
            BookingController.RazorpayOrderRes result = razorpayService.createOrder(request, user);
            
            logger.info("=== CREATE RAZORPAY ORDER RESPONSE ===");
            logger.info("Order ID: {}", result.orderId());
            logger.info("Amount: {} {}", result.amount(), result.currency());
            logger.info("Receipt: {}", result.receipt());
            logger.info("=====================================");
            
            return result;
        } catch (Exception e) {
            logger.error("=== CREATE RAZORPAY ORDER FAILED ===");
            logger.error("User: {}", user.getUserId());
            logger.error("Request: {}", request);
            logger.error("Error: {}", e.getMessage(), e);
            logger.error("===================================");
            throw e;
        }
    }
    
    /**
     * Handle Razorpay webhook.
     */
    public void handleRazorpayWebhook(HttpServletRequest request, String body) {
        logger.info("=== RAZORPAY WEBHOOK REQUEST ===");
        logger.info("Request URI: {}", request.getRequestURI());
        logger.info("Content Type: {}", request.getContentType());
        logger.info("Content Length: {}", request.getContentLength());
        logger.info("Headers:");
        java.util.Collections.list(request.getHeaderNames()).forEach(headerName -> 
            logger.info("  {}: {}", headerName, request.getHeader(headerName)));
        logger.info("Body length: {}", body != null ? body.length() : 0);
        
        try {
            razorpayService.handleWebhook(request, body);
            logger.info("=== RAZORPAY WEBHOOK PROCESSED ===");
            logger.info("Webhook processed successfully");
            logger.info("=================================");
        } catch (Exception e) {
            logger.error("=== RAZORPAY WEBHOOK FAILED ===");
            logger.error("Error: {}", e.getMessage(), e);
            logger.error("==============================");
            throw e;
        }
    }
    
    /**
     * Execute provider booking.
     */
    public BookingController.BookingRes executeProviderBooking(String vertical, String provider, BookingController.ProviderBookReq request, GoogleUserPrincipal user) {
        logger.info("Executing {} provider booking with {} for user: {}", vertical, provider, user.getUserId());
        
        try {
            // Find booking by payment details
            com.tripplanner.data.entity.Booking booking = bookingRepository.findByRazorpayOrderId(request.payment().orderId())
                    .orElseThrow(() -> new RuntimeException("Booking not found for order: " + request.payment().orderId()));
            
            // Verify ownership
            if (!booking.getUserId().equals(user.getUserId())) {
                throw new RuntimeException("Access denied to booking");
            }
            
            // Update booking status
            booking.setStatus(com.tripplanner.data.entity.Booking.BookingStatus.BOOKING_IN_PROGRESS);
            
            // Simulate provider booking (in real implementation, this would call the actual provider)
            com.tripplanner.data.entity.Booking.ProviderDetails providerDetails = new com.tripplanner.data.entity.Booking.ProviderDetails();
            providerDetails.setConfirmationId("CONF-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            providerDetails.setStatus("confirmed");
            providerDetails.setBookingReference("REF-" + System.currentTimeMillis());
            providerDetails.setContactInfo(Map.of(
                "phone", "+1-555-PROVIDER",
                "email", "support@provider.com"
            ));
            
            booking.setProvider(providerDetails);
            booking.setStatus(com.tripplanner.data.entity.Booking.BookingStatus.CONFIRMED);
            
            bookingRepository.save(booking);
            
            logger.info("Provider booking completed: {}", booking.getId());
            
            return new BookingController.BookingRes(
                    booking.getId(),
                    booking.getStatus().toString(),
                    providerDetails.getConfirmationId(),
                    booking.getItineraryId(),
                    booking.getCreatedAt(),
                    Map.of("provider", provider, "vertical", vertical)
            );
            
        } catch (Exception e) {
            logger.error("Provider booking failed", e);
            throw new RuntimeException("Provider booking failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get booking by ID.
     */
    public BookingController.BookingRes getBooking(String bookingId, GoogleUserPrincipal user) {
        logger.debug("Getting booking: {} for user: {}", bookingId, user.getUserId());
        
        try {
            com.tripplanner.data.entity.Booking booking = bookingRepository.findById(bookingId)
                    .orElseThrow(() -> new RuntimeException("Booking not found: " + bookingId));
            
            // Verify ownership
            if (!booking.getUserId().equals(user.getUserId())) {
                throw new RuntimeException("Access denied to booking: " + bookingId);
            }
            
            return new BookingController.BookingRes(
                    booking.getId(),
                    booking.getStatus().toString(),
                    booking.getProvider() != null ? booking.getProvider().getConfirmationId() : null,
                    booking.getItineraryId(),
                    booking.getCreatedAt(),
                    booking.getMetadata()
            );
            
        } catch (Exception e) {
            logger.error("Failed to get booking: " + bookingId, e);
            throw new RuntimeException("Failed to get booking", e);
        }
    }
    
    /**
     * Get user bookings.
     */
    public List<BookingController.BookingRes> getUserBookings(GoogleUserPrincipal user, int page, int size) {
        logger.debug("Getting bookings for user: {}", user.getUserId());
        
        try {
            List<com.tripplanner.data.entity.Booking> bookings = bookingRepository.findByUserId(user.getUserId(), size);
            
            return bookings.stream()
                    .map(booking -> new BookingController.BookingRes(
                            booking.getId(),
                            booking.getStatus().toString(),
                            booking.getProvider() != null ? booking.getProvider().getConfirmationId() : null,
                            booking.getItineraryId(),
                            booking.getCreatedAt(),
                            booking.getMetadata()
                    ))
                    .toList();
            
        } catch (Exception e) {
            logger.error("Failed to get user bookings", e);
            throw new RuntimeException("Failed to get user bookings", e);
        }
    }
    
    /**
     * Cancel booking.
     */
    public void cancelBooking(String bookingId, BookingController.CancelBookingReq request, GoogleUserPrincipal user) {
        logger.info("Canceling booking: {} for user: {}", bookingId, user.getUserId());
        
        try {
            com.tripplanner.data.entity.Booking booking = bookingRepository.findById(bookingId)
                    .orElseThrow(() -> new RuntimeException("Booking not found: " + bookingId));
            
            // Verify ownership
            if (!booking.getUserId().equals(user.getUserId())) {
                throw new RuntimeException("Access denied to booking: " + bookingId);
            }
            
            // Update booking status
            booking.setStatus(com.tripplanner.data.entity.Booking.BookingStatus.CANCELLED);
            bookingRepository.save(booking);
            
            // TODO: Call provider cancellation API if needed
            
            logger.info("Booking canceled: {}", bookingId);
            
        } catch (Exception e) {
            logger.error("Failed to cancel booking: " + bookingId, e);
            throw new RuntimeException("Failed to cancel booking", e);
        }
    }
}
