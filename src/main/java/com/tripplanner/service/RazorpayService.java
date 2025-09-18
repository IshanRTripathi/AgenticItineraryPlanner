package com.tripplanner.service;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import com.tripplanner.api.BookingController;
import com.tripplanner.data.entity.Booking;
import com.tripplanner.data.repo.BookingRepository;
import com.tripplanner.security.GoogleUserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.concurrent.ExecutionException;

/**
 * Service for Razorpay payment integration.
 */
@Service
public class RazorpayService {
    
    private static final Logger logger = LoggerFactory.getLogger(RazorpayService.class);
    
    @Value("${razorpay.key-id}")
    private String keyId;
    
    @Value("${razorpay.key-secret}")
    private String keySecret;
    
    @Value("${razorpay.webhook-secret}")
    private String webhookSecret;
    
    @Value("${razorpay.environment:test}")
    private String environment;
    
    private final BookingRepository bookingRepository;
    private RazorpayClient razorpayClient;
    
    public RazorpayService(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }
    
    /**
     * Initialize Razorpay client after properties are injected.
     */
    @jakarta.annotation.PostConstruct
    public void initializeClient() throws RazorpayException {
        logger.info("Initializing Razorpay client for environment: {}", environment);
        this.razorpayClient = new RazorpayClient(keyId, keySecret);
        logger.info("Razorpay client initialized successfully");
    }
    
    /**
     * Create a Razorpay order.
     */
    public BookingController.RazorpayOrderRes createOrder(BookingController.RazorpayOrderReq request, GoogleUserPrincipal user) {
        logger.info("Creating Razorpay order for user: {}, amount: {}", user.getUserId(), request.amount());
        
        try {
            // Create order with Razorpay
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", request.amount()); // Amount in paise
            orderRequest.put("currency", request.currency());
            orderRequest.put("receipt", "rcpt_" + System.currentTimeMillis());
            
            if (request.meta() != null) {
                JSONObject notes = new JSONObject();
                request.meta().forEach((key, value) -> notes.put(key, value.toString()));
                orderRequest.put("notes", notes);
            }
            
            Order order = razorpayClient.orders.create(orderRequest);
            
            // Create booking record
            Booking booking = new Booking();
            booking.setUserId(user.getUserId());
            booking.setItineraryId(request.itineraryId());
            
            // Set booking item
            Booking.BookingItem item = new Booking.BookingItem();
            item.setType(request.itemType());
            item.setProvider("razorpay");
            booking.setItem(item);
            
            // Set booking price
            Booking.BookingPrice price = new Booking.BookingPrice();
            price.setAmount(request.amount() / 100.0); // Convert from paise to currency
            price.setCurrency(request.currency());
            booking.setPrice(price);
            
            // Set Razorpay details
            Booking.RazorpayDetails razorpayDetails = new Booking.RazorpayDetails();
            razorpayDetails.setOrderId(order.get("id"));
            razorpayDetails.setAmount(request.amount());
            razorpayDetails.setCurrency(request.currency());
            razorpayDetails.setReceipt(order.get("receipt"));
            razorpayDetails.setStatus("created");
            razorpayDetails.setCreatedAt(Instant.now());
            booking.setRazorpay(razorpayDetails);
            
            booking.setStatus(Booking.BookingStatus.PAYMENT_ORDERED);
            
            // Save booking
            booking = bookingRepository.save(booking);
            
            logger.info("Razorpay order created: {}, booking ID: {}", order.get("id"), booking.getId());
            
            return new BookingController.RazorpayOrderRes(
                    order.get("id"),
                    request.amount(),
                    request.currency(),
                    order.get("receipt")
            );
            
        } catch (RazorpayException | ExecutionException | InterruptedException e) {
            logger.error("Failed to create Razorpay order", e);
            throw new RuntimeException("Failed to create payment order: " + e.getMessage(), e);
        }
    }
    
    /**
     * Handle Razorpay webhook for payment notifications.
     */
    public void handleWebhook(HttpServletRequest request, String body) {
        logger.info("Processing Razorpay webhook");
        
        try {
            // Verify webhook signature
            String signature = request.getHeader("X-Razorpay-Signature");
            if (!verifyWebhookSignature(body, signature)) {
                logger.warn("Invalid Razorpay webhook signature");
                throw new RuntimeException("Invalid webhook signature");
            }
            
            // Parse webhook payload
            JSONObject payload = new JSONObject(body);
            String event = payload.getString("event");
            JSONObject paymentEntity = payload.getJSONObject("payload").getJSONObject("payment").getJSONObject("entity");
            
            logger.info("Processing Razorpay webhook event: {}", event);
            
            if ("payment.captured".equals(event)) {
                handlePaymentCaptured(paymentEntity);
            } else if ("payment.failed".equals(event)) {
                handlePaymentFailed(paymentEntity);
            } else {
                logger.info("Unhandled webhook event: {}", event);
            }
            
        } catch (Exception e) {
            logger.error("Failed to process Razorpay webhook", e);
            throw new RuntimeException("Failed to process webhook: " + e.getMessage(), e);
        }
    }
    
    /**
     * Handle successful payment capture.
     */
    private void handlePaymentCaptured(JSONObject payment) throws ExecutionException, InterruptedException {
        String paymentId = payment.getString("id");
        String orderId = payment.getString("order_id");
        
        logger.info("Payment captured: {}, order: {}", paymentId, orderId);
        
        // Find booking by order ID
        Booking booking = bookingRepository.findByRazorpayOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Booking not found for order: " + orderId));
        
        // Update Razorpay details
        Booking.RazorpayDetails razorpayDetails = booking.getRazorpay();
        if (razorpayDetails == null) {
            razorpayDetails = new Booking.RazorpayDetails();
        }
        
        razorpayDetails.setPaymentId(paymentId);
        razorpayDetails.setStatus("captured");
        razorpayDetails.setMethod(payment.optString("method"));
        
        booking.setRazorpay(razorpayDetails);
        booking.setStatus(Booking.BookingStatus.PAYMENT_CONFIRMED);
        
        bookingRepository.save(booking);
        
        logger.info("Booking updated for successful payment: {}", booking.getId());
        
        // TODO: Trigger provider booking process
        // This would call the appropriate provider to complete the actual booking
    }
    
    /**
     * Handle failed payment.
     */
    private void handlePaymentFailed(JSONObject payment) throws ExecutionException, InterruptedException {
        String paymentId = payment.getString("id");
        String orderId = payment.getString("order_id");
        
        logger.info("Payment failed: {}, order: {}", paymentId, orderId);
        
        // Find booking by order ID
        Booking booking = bookingRepository.findByRazorpayOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Booking not found for order: " + orderId));
        
        // Update status to failed
        booking.setStatus(Booking.BookingStatus.FAILED);
        bookingRepository.save(booking);
        
        logger.info("Booking marked as failed: {}", booking.getId());
    }
    
    /**
     * Verify webhook signature to ensure authenticity.
     */
    private boolean verifyWebhookSignature(String payload, String signature) {
        if (signature == null || webhookSecret == null) {
            return false;
        }
        
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            
            byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String expectedSignature = bytesToHex(digest);
            
            return signature.equals(expectedSignature);
            
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            logger.error("Error verifying webhook signature", e);
            return false;
        }
    }
    
    /**
     * Convert bytes to hex string.
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
    
    /**
     * Check if Razorpay is properly configured.
     */
    public boolean isConfigured() {
        return keyId != null && !keyId.trim().isEmpty() &&
               keySecret != null && !keySecret.trim().isEmpty();
    }
    
    /**
     * Get Razorpay configuration info (without sensitive data).
     */
    public String getConfigInfo() {
        return String.format("Environment: %s, Key ID: %s***", 
                           environment, 
                           keyId != null && keyId.length() > 4 ? keyId.substring(0, 4) : "****");
    }
}

