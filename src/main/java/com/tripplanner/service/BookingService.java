package com.tripplanner.service;

import com.tripplanner.controller.BookingController;
import com.tripplanner.data.entity.Booking;
import com.tripplanner.data.repo.BookingRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Service for booking and payment operations.
 */
@Service
@ConditionalOnProperty(name = "razorpay.enabled", havingValue = "true")
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
    public BookingController.RazorpayOrderRes createRazorpayOrder(BookingController.RazorpayOrderReq request) {
        logger.info("=== CREATE RAZORPAY ORDER REQUEST ===");
        logger.info("Creating Razorpay order");
        logger.info("Item Type: {}", request.itemType());
        logger.info("Itinerary ID: {}", request.itineraryId());
        logger.info("Amount: {} {}", request.amount(), request.currency());
        logger.info("Meta: {}", request.meta());
        
        try {
            BookingController.RazorpayOrderRes result = razorpayService.createOrder(request);
            
            logger.info("=== CREATE RAZORPAY ORDER RESPONSE ===");
            logger.info("Order ID: {}", result.orderId());
            logger.info("Amount: {} {}", result.amount(), result.currency());
            logger.info("Receipt: {}", result.receipt());
            logger.info("=====================================");
            
            return result;
        } catch (Exception e) {
            logger.error("=== CREATE RAZORPAY ORDER FAILED ===");
            logger.error("Failed to create Razorpay order");
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
    public BookingController.BookingRes executeProviderBooking(String userId, String vertical, String provider, BookingController.ProviderBookReq request) {
        logger.info("Executing {} provider booking with {} for user: {}", vertical, provider, userId);
        
        try {
            // Find booking by payment details
            com.tripplanner.data.entity.Booking booking = bookingRepository.findByRazorpayOrderId(request.payment().orderId())
                    .orElseThrow(() -> new RuntimeException("Booking not found for order: " + request.payment().orderId()));
            
            // Validate user ownership
            if (!booking.getUserId().equals(userId)) {
                throw new RuntimeException("User " + userId + " does not own booking " + booking.getId());
            }
            
            // Update booking status to confirmed (simplified for current implementation)
            booking.setStatus(Booking.BookingStatus.CONFIRMED);
            
            // Simulate provider booking (in real implementation, this would call the actual provider)
            Booking.ProviderDetails providerDetails = new Booking.ProviderDetails();
            providerDetails.setConfirmationId("CONF-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            providerDetails.setStatus("confirmed");
            providerDetails.setBookingReference("REF-" + System.currentTimeMillis());
                    providerDetails.setContactInfoJson("{\"phone\":\"+1-555-PROVIDER\",\"email\":\"support@provider.com\"}");
            booking.setProvider(providerDetails);
            
            bookingRepository.save(booking);
            
            logger.info("Provider booking completed: {}", booking.getId());
            
            return new BookingController.BookingRes(
                    booking.getBookingId(),
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
    public BookingController.BookingRes getBooking(String userId, String bookingId) {
        logger.debug("Getting booking: {} for user: {}", bookingId, userId);
        
        try {
            com.tripplanner.data.entity.Booking booking = bookingRepository.findByBookingId(bookingId)
                    .orElseThrow(() -> new RuntimeException("Booking not found: " + bookingId));
            
            // Validate user ownership
            if (!booking.getUserId().equals(userId)) {
                throw new RuntimeException("User " + userId + " does not own booking " + bookingId);
            }
            
            return new BookingController.BookingRes(
                    booking.getBookingId(),
                    booking.getStatus().toString(),
                    booking.getProvider() != null ? booking.getProvider().getConfirmationId() : null,
                    booking.getItineraryId(),
                    booking.getCreatedAt(),
                    null // TODO: Parse JSON to Map if needed - currently returning null as JSON parsing is not implemented
            );
            
        } catch (Exception e) {
            logger.error("Failed to get booking: " + bookingId, e);
            throw new RuntimeException("Failed to get booking", e);
        }
    }
    
    /**
     * Get user bookings.
     */
    public List<BookingController.BookingRes> getUserBookings(int page, int size) {
        logger.debug("Getting bookings");
        
        try {
            // Get all bookings for anonymous user (current implementation)
            List<com.tripplanner.data.entity.Booking> bookings = bookingRepository.findAll();
            
            return bookings.stream()
                    .map(booking -> new BookingController.BookingRes(
                            booking.getBookingId(),
                            booking.getStatus().toString(),
                            booking.getProvider() != null ? booking.getProvider().getConfirmationId() : null,
                            booking.getItineraryId(),
                            booking.getCreatedAt(),
                            null // TODO: Parse JSON to Map if needed - currently returning null as JSON parsing is not implemented
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
    public void cancelBooking(String userId, String bookingId, BookingController.CancelBookingReq request) {
        logger.info("Canceling booking: {} for user: {}", bookingId, userId);
        
        try {
            com.tripplanner.data.entity.Booking booking = bookingRepository.findByBookingId(bookingId)
                    .orElseThrow(() -> new RuntimeException("Booking not found: " + bookingId));
            
            // Validate user ownership
            if (!booking.getUserId().equals(userId)) {
                throw new RuntimeException("User " + userId + " does not own booking " + bookingId);
            }
            
            // Update booking status
            booking.setStatus(Booking.BookingStatus.CANCELLED);
            bookingRepository.save(booking);
            
            // TODO: Call provider cancellation API if needed - currently not implemented
            
            logger.info("Booking canceled: {}", bookingId);
            
        } catch (Exception e) {
            logger.error("Failed to cancel booking: " + bookingId, e);
            throw new RuntimeException("Failed to cancel booking", e);
        }
    }
    
    /**
     * Save booking record.
     */
    public com.tripplanner.data.entity.Booking saveBookingRecord(com.tripplanner.data.entity.Booking booking) {
        logger.info("Saving booking record: {}", booking.getBookingId());
        return bookingRepository.save(booking);
    }
    
    /**
     * Get bookings by itinerary ID.
     */
    public List<com.tripplanner.data.entity.Booking> getBookingsByItineraryId(String itineraryId) {
        logger.debug("Getting bookings for itinerary: {}", itineraryId);
        return bookingRepository.findByItineraryId(itineraryId);
    }
}
