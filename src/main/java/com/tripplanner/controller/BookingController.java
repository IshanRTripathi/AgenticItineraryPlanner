package com.tripplanner.controller;

import com.tripplanner.dto.*;
import com.tripplanner.service.BookingService;
import com.tripplanner.service.ChangeEngine;
import com.tripplanner.service.ItineraryJsonService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for booking and payment operations.
 */
@RestController
@ConditionalOnProperty(name = "razorpay.enabled", havingValue = "true")
@RequestMapping("/api/v1")
public class BookingController {
    
    private static final Logger logger = LoggerFactory.getLogger(BookingController.class);
    
    private final BookingService bookingService;
    private final ItineraryJsonService itineraryJsonService;
    private final ChangeEngine changeEngine;
    
    public BookingController(BookingService bookingService, 
                           ItineraryJsonService itineraryJsonService,
                           ChangeEngine changeEngine) {
        this.bookingService = bookingService;
        this.itineraryJsonService = itineraryJsonService;
        this.changeEngine = changeEngine;
    }
    
    /**
     * Create Razorpay order for payment.
     */
    @PostMapping("/payments/razorpay/order")
    public ResponseEntity<RazorpayOrderRes> createRazorpayOrder(@Valid @RequestBody RazorpayOrderReq request) {
        logger.info("Creating Razorpay order, amountPerPerson: {} {}", request.amount(), request.currency());
        
        RazorpayOrderRes response = bookingService.createRazorpayOrder(request);
        
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
            HttpServletRequest httpRequest) {
        String userId = (String) httpRequest.getAttribute("userId");
        logger.info("Executing {} booking with provider {} for user {}", vertical, provider, userId);
        
        BookingRes response = bookingService.executeProviderBooking(userId, vertical, provider, request);
        
        logger.info("Provider booking executed: {}", response.bookingId());
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get booking by ID.
     */
    @GetMapping("/bookings/{bookingId}")
    public ResponseEntity<BookingRes> getBooking(
            @PathVariable String bookingId,
            HttpServletRequest httpRequest) {
        String userId = (String) httpRequest.getAttribute("userId");
        logger.debug("Getting booking: {} for user: {}", bookingId, userId);
        
        BookingRes booking = bookingService.getBooking(userId, bookingId);
        return ResponseEntity.ok(booking);
    }
    
    /**
     * Get user's bookings.
     */
    @GetMapping("/bookings")
    public ResponseEntity<java.util.List<BookingRes>> getUserBookings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        logger.debug("Getting bookings for user: {}, page: {}, size: {}", 
                    "anonymous", page, size);
        
        java.util.List<BookingRes> bookings = bookingService.getUserBookings(page, size);
        return ResponseEntity.ok(bookings);
    }
    
    /**
     * Cancel a booking.
     */
    @PostMapping("/bookings/{bookingId}:cancel")
    public ResponseEntity<Void> cancelBooking(
            @PathVariable String bookingId,
            @Valid @RequestBody CancelBookingReq request,
            HttpServletRequest httpRequest) {
        String userId = (String) httpRequest.getAttribute("userId");
        logger.info("Canceling booking: {} for user: {}", bookingId, userId);
        
        bookingService.cancelBooking(userId, bookingId, request);
        
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
    
    // ===== NEW MVP CONTRACT ENDPOINT =====
    
    /**
     * Mock booking endpoint for the MVP contract.
     * POST /book → 200 → body: {itineraryId, nodeId} → {bookingRef, locked}
     */
    @PostMapping("/book")
    public ResponseEntity<MockBookingResponse> mockBook(@Valid @RequestBody MockBookingRequest request) {
        logger.info("=== BOOKING CONTROLLER: MOCK BOOKING ===");
        logger.info("Itinerary ID: {}", request.getItineraryId());
        logger.info("Node ID: {}", request.getNodeId());
        
        try {
            // Load the current normalized itinerary
            var currentItinerary = itineraryJsonService.getItinerary(request.getItineraryId());
            if (currentItinerary.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            // Create a ChangeSet to lock the node and add booking reference
            ChangeSet changeSet = createBookingChangeSet(request.getNodeId());
            
            // Apply the booking changes
            ChangeEngine.ApplyResult result = changeEngine.apply(request.getItineraryId(), changeSet);
            
            // Generate a mock booking reference
            String bookingRef = "BK" + System.currentTimeMillis();
            
            MockBookingResponse response = new MockBookingResponse(
                request.getItineraryId(),
                request.getNodeId(),
                bookingRef,
                true, // locked
                "Mock booking completed successfully"
            );
            
            logger.info("=== MOCK BOOKING COMPLETED ===");
            logger.info("Booking Reference: {}", bookingRef);
            logger.info("Node Locked: {}", response.locked());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to process mock booking", e);
            return ResponseEntity.status(500).body(new MockBookingResponse(
                request.getItineraryId(),
                request.getNodeId(),
                null,
                false,
                "Failed to process booking: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Create a ChangeSet for booking a node (locking it and adding booking reference).
     */
    private ChangeSet createBookingChangeSet(String nodeId) {
        ChangeSet changeSet = new ChangeSet();
        changeSet.setScope("trip");
        
        // Create a change operation to update the node
        ChangeOperation bookingOp = new ChangeOperation();
        bookingOp.setOp("update");
        bookingOp.setId(nodeId);
        
        // Create a node with booking information
        NormalizedNode bookedNode = new NormalizedNode();
        bookedNode.setId(nodeId);
        // DO NOT auto-lock booked nodes - user must manually lock if desired
        
        // Add booking reference to labels
        bookedNode.setLabels(java.util.List.of("Booked"));
        
        // Add booking reference to details (we'll use the category field for now)
        NodeDetails details = new NodeDetails();
        details.setCategory("Booked - Reference: BK" + System.currentTimeMillis());
        bookedNode.setDetails(details);
        
        bookingOp.setNode(bookedNode);
        changeSet.setOps(java.util.List.of(bookingOp));
        
        // Set preferences
        ChangePreferences preferences = new ChangePreferences();
        preferences.setUserFirst(false); // Booking takes precedence
        preferences.setRespectLocks(false); // Override locks for booking
        changeSet.setPreferences(preferences);
        
        return changeSet;
    }
    
    /**
     * Request DTO for mock booking.
     */
    public static class MockBookingRequest {
        @NotBlank
        private String itineraryId;
        
        @NotBlank
        private String nodeId;
        
        public MockBookingRequest() {}
        
        public MockBookingRequest(String itineraryId, String nodeId) {
            this.itineraryId = itineraryId;
            this.nodeId = nodeId;
        }
        
        public String getItineraryId() { return itineraryId; }
        public void setItineraryId(String itineraryId) { this.itineraryId = itineraryId; }
        public String getNodeId() { return nodeId; }
        public void setNodeId(String nodeId) { this.nodeId = nodeId; }
    }
    
    /**
     * Response DTO for mock booking.
     */
    public record MockBookingResponse(
            String itineraryId,
            String nodeId,
            String bookingRef,
            boolean locked,
            String message
    ) {}
    
    // ===== SIMPLIFIED BOOKING ENDPOINTS FOR FRONTEND =====
    
    /**
     * Create a booking record from provider confirmation.
     * POST /api/bookings/record
     */
    @PostMapping("/bookings/record")
    public ResponseEntity<BookingRecordResponse> createBookingRecord(
            @Valid @RequestBody com.tripplanner.dto.CreateBookingRecordRequest request,
            HttpServletRequest httpRequest) {
        String userId = (String) httpRequest.getAttribute("userId");
        if (userId == null) {
            userId = request.getUserId(); // Fallback to request userId
        }
        
        logger.info("=== CREATE BOOKING RECORD ===");
        logger.info("User ID: {}", userId);
        logger.info("Itinerary ID: {}", request.getItineraryId());
        logger.info("Node ID: {}", request.getNodeId());
        logger.info("Provider: {}", request.getProviderName());
        logger.info("Confirmation: {}", request.getConfirmationNumber());
        
        try {
            // Create booking entity
            com.tripplanner.data.entity.Booking booking = new com.tripplanner.data.entity.Booking();
            booking.setUserId(userId);
            booking.setItineraryId(request.getItineraryId());
            booking.setStatus(com.tripplanner.data.entity.Booking.BookingStatus.CONFIRMED);
            
            // Set item details
            com.tripplanner.data.entity.Booking.BookingItem item = new com.tripplanner.data.entity.Booking.BookingItem();
            item.setType(request.getBookingType() != null ? request.getBookingType() : "general");
            item.setProvider(request.getProviderName());
            item.setToken(request.getNodeId()); // Use nodeId as token
            // Store item name in details JSON
            item.setDetailsJson("{\"name\":\"" + (request.getItemName() != null ? request.getItemName() : "Booking") + "\"}");
            booking.setItem(item);
            
            // Set price details
            com.tripplanner.data.entity.Booking.BookingPrice price = new com.tripplanner.data.entity.Booking.BookingPrice();
            price.setAmount(request.getTotalAmount() != null ? request.getTotalAmount() : 0.0);
            price.setCurrency(request.getCurrency());
            booking.setPrice(price);
            
            // Set provider details
            com.tripplanner.data.entity.Booking.ProviderDetails provider = new com.tripplanner.data.entity.Booking.ProviderDetails();
            provider.setConfirmationId(request.getConfirmationNumber());
            provider.setStatus("confirmed");
            provider.setBookingReference(request.getConfirmationNumber());
            booking.setProvider(provider);
            
            // Save booking via service
            booking = bookingService.saveBookingRecord(booking);
            
            // If nodeId provided, update the node with booking reference
            if (request.getNodeId() != null && !request.getNodeId().isEmpty()) {
                try {
                    ChangeSet changeSet = createNodeBookingReferenceChangeSet(
                        request.getNodeId(), 
                        booking.getBookingId()
                    );
                    changeEngine.apply(request.getItineraryId(), changeSet);
                    logger.info("Updated node {} with booking reference", request.getNodeId());
                } catch (Exception e) {
                    logger.warn("Failed to update node with booking reference", e);
                    // Don't fail the booking creation if node update fails
                }
            }
            
            logger.info("=== BOOKING RECORD CREATED ===");
            logger.info("Booking ID: {}", booking.getBookingId());
            
            return ResponseEntity.ok(new BookingRecordResponse(
                booking.getBookingId(),
                booking.getStatus().toString(),
                request.getConfirmationNumber(),
                booking.getCreatedAt()
            ));
            
        } catch (Exception e) {
            logger.error("Failed to create booking record", e);
            return ResponseEntity.status(500).build();
        }
    }
    
    /**
     * Get bookings for an itinerary.
     * GET /api/bookings/itinerary/{itineraryId}
     */
    @GetMapping("/bookings/itinerary/{itineraryId}")
    public ResponseEntity<java.util.List<BookingRecordResponse>> getItineraryBookings(
            @PathVariable String itineraryId) {
        logger.debug("Getting bookings for itinerary: {}", itineraryId);
        
        try {
            java.util.List<com.tripplanner.data.entity.Booking> bookings = 
                this.bookingService.getBookingsByItineraryId(itineraryId);
            
            java.util.List<BookingRecordResponse> response = bookings.stream()
                .map(booking -> new BookingRecordResponse(
                    booking.getBookingId(),
                    booking.getStatus().toString(),
                    booking.getProvider() != null ? booking.getProvider().getConfirmationId() : null,
                    booking.getCreatedAt()
                ))
                .toList();
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to get itinerary bookings", e);
            return ResponseEntity.status(500).build();
        }
    }
    
    /**
     * Create a ChangeSet to add booking reference to a node.
     */
    private ChangeSet createNodeBookingReferenceChangeSet(String nodeId, String bookingRef) {
        ChangeSet changeSet = new ChangeSet();
        changeSet.setScope("trip");
        
        ChangeOperation op = new ChangeOperation();
        op.setOp("update");
        op.setId(nodeId);
        
        NormalizedNode node = new NormalizedNode();
        node.setId(nodeId);
        node.setBookingRef(bookingRef);
        
        op.setNode(node);
        changeSet.setOps(java.util.List.of(op));
        
        ChangePreferences preferences = new ChangePreferences();
        preferences.setUserFirst(false);
        preferences.setRespectLocks(false);
        changeSet.setPreferences(preferences);
        
        return changeSet;
    }
    
    /**
     * Response DTO for booking record operations.
     */
    public record BookingRecordResponse(
            String bookingId,
            String status,
            String confirmationNumber,
            java.time.Instant createdAt
    ) {}
}

