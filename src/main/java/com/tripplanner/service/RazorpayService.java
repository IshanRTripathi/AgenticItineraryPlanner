package com.tripplanner.service;

import com.tripplanner.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service for integrating with Razorpay API for payment processing.
 * Provides payment processing and refund functionality with rate limiting.
 */
@Service
public class RazorpayService {
    
    private static final Logger logger = LoggerFactory.getLogger(RazorpayService.class);
    private static final String BASE_URL = "https://api.razorpay.com/v1";
    
    // Rate limiting tracking (1000 requests/minute)
    private final AtomicInteger minuteRequestCount = new AtomicInteger(0);
    private final AtomicLong lastResetTime = new AtomicLong(System.currentTimeMillis());
    private static final int MINUTE_LIMIT = 1000;
    
    @Value("${razorpay.key-id:}")
    private String keyId;
    
    @Value("${razorpay.key-secret:}")
    private String keySecret;
    
    @Value("${razorpay.rate.limit.enabled:true}")
    private boolean rateLimitEnabled;
    
    @Value("${razorpay.timeout:30000}")
    private int timeoutMs;
    
    private final RestTemplate restTemplate;
    
    public RazorpayService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    
    /**
     * Create a payment order for booking confirmation.
     * Returns payment details including order ID and payment URL.
     */
    public PaymentOrder createPaymentOrder(PaymentRequest request) {
        logger.info("Creating payment order for amountPerPerson: {} {}", request.getAmount(), request.getCurrency());
        
        if (request == null) {
            throw new IllegalArgumentException("Payment request cannot be null");
        }
        
        validatePaymentRequest(request);
        checkRateLimit();
        
        try {
            // Build order parameters
            Map<String, Object> orderParams = buildOrderParams(request);
            
            // Make POST request to orders endpoint
            String url = BASE_URL + "/orders";
            HttpHeaders headers = createHeaders();
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(orderParams, headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                url, HttpMethod.POST, entity, Map.class);
            
            incrementRequestCount();
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> orderData = response.getBody();
                
                PaymentOrder paymentOrder = mapToPaymentOrder(orderData, request);
                
                logger.info("Payment order created successfully: {}", paymentOrder.getOrderId());
                return paymentOrder;
            } else {
                throw new RuntimeException("Invalid response from Razorpay API: " + response.getStatusCode());
            }
            
        } catch (HttpClientErrorException e) {
            logger.error("Client error creating payment order: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Payment order creation failed: " + e.getMessage(), e);
        } catch (HttpServerErrorException e) {
            logger.error("Server error creating payment order: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Razorpay service unavailable: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error creating payment order", e);
            throw new RuntimeException("Payment order creation failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Verify payment signature and process payment confirmation.
     * Returns payment result with status and transaction details.
     */
    public PaymentResult verifyPayment(PaymentVerification verification) {
        logger.info("Verifying payment for order: {}", verification.getOrderId());
        
        if (verification == null) {
            throw new IllegalArgumentException("Payment verification cannot be null");
        }
        
        validatePaymentVerification(verification);
        checkRateLimit();
        
        try {
            // Verify signature
            boolean isSignatureValid = verifySignature(
                verification.getOrderId(),
                verification.getPaymentId(),
                verification.getSignature()
            );
            
            if (!isSignatureValid) {
                logger.warn("Invalid payment signature for order: {}", verification.getOrderId());
                return createFailedPaymentResult(verification, "Invalid payment signature");
            }
            
            // Fetch payment details from Razorpay
            PaymentResult paymentResult = fetchPaymentDetails(verification.getPaymentId());
            
            incrementRequestCount();
            
            if (paymentResult.isSuccessful()) {
                logger.info("Payment verified successfully: {}", verification.getPaymentId());
            } else {
                logger.warn("Payment verification failed: {}", paymentResult.getErrorMessage());
            }
            
            return paymentResult;
            
        } catch (Exception e) {
            logger.error("Unexpected error verifying payment", e);
            return createFailedPaymentResult(verification, "Payment verification failed: " + e.getMessage());
        }
    }
    
    /**
     * Process refund for a successful payment.
     * Returns refund result with status and refund details.
     */
    public RefundResult processRefund(RefundRequest request) {
        logger.info("Processing refund for payment: {} amountPerPerson: {} {}",
                   request.getPaymentId(), request.getAmount(), request.getCurrency());
        
        if (request == null) {
            throw new IllegalArgumentException("Refund request cannot be null");
        }
        
        validateRefundRequestNew(request);
        checkRateLimit();
        
        try {
            // Build refund parameters
            Map<String, Object> refundParams = buildRefundParams(request);
            
            // Make POST request to refunds endpoint
            String url = BASE_URL + "/payments/" + request.getPaymentId() + "/refund";
            HttpHeaders headers = createHeaders();
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(refundParams, headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                url, HttpMethod.POST, entity, Map.class);
            
            incrementRequestCount();
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> refundData = response.getBody();
                
                RefundResult refundResult = mapToRefundResult(refundData, request);
                
                logger.info("Refund processed successfully: {}", refundResult.getRefundId());
                return refundResult;
            } else {
                throw new RuntimeException("Invalid response from Razorpay API: " + response.getStatusCode());
            }
            
        } catch (HttpClientErrorException e) {
            logger.error("Client error processing refund: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Refund processing failed: " + e.getMessage(), e);
        } catch (HttpServerErrorException e) {
            logger.error("Server error processing refund: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Razorpay service unavailable: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error processing refund", e);
            throw new RuntimeException("Refund processing failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Process a payment using Razorpay API.
     * Returns payment result with transaction details.
     */
    public PaymentResult processPayment(PaymentDetails details) {
        logger.info("Processing payment for amountPerPerson: {} {}", details.getAmount(), details.getCurrency());
        
        if (details == null) {
            throw new IllegalArgumentException("Payment details cannot be null");
        }
        
        validatePaymentDetails(details);
        checkRateLimit();
        
        try {
            // Build payment request
            Map<String, Object> paymentRequest = buildPaymentRequest(details);
            
            // Make POST request to payments endpoint
            String url = BASE_URL + "/payments";
            HttpHeaders headers = createHeaders();
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(paymentRequest, headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                url, HttpMethod.POST, entity, Map.class);
            
            incrementRequestCount();
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                
                PaymentResult result = parsePaymentResponse(responseBody);
                
                if (result.isSuccessful()) {
                    logger.info("Payment processed successfully: {}", result.getPaymentId());
                } else {
                    logger.warn("Payment processing failed: {}", result.getErrorMessage());
                }
                
                return result;
            } else {
                throw new RuntimeException("Invalid response from Razorpay API: " + response.getStatusCode());
            }
            
        } catch (HttpClientErrorException e) {
            logger.error("Client error processing payment: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            return createFailedPaymentResult(details, "Payment failed: " + e.getMessage());
        } catch (HttpServerErrorException e) {
            logger.error("Server error processing payment: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            return createFailedPaymentResult(details, "Razorpay service unavailable: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error processing payment", e);
            return createFailedPaymentResult(details, "Payment processing failed: " + e.getMessage());
        }
    }
    
    /**
     * Refund a payment using Razorpay API.
     * Returns refund result with transaction details.
     */
    public RefundResult refundPayment(String paymentId, double amount) {
        logger.info("Processing refund for payment: {} amountPerPerson: {}", paymentId, amount);
        
        if (paymentId == null || paymentId.trim().isEmpty()) {
            throw new IllegalArgumentException("Payment ID cannot be null or empty");
        }
        
        if (amount <= 0) {
            throw new IllegalArgumentException("Refund amountPerPerson must be greater than 0");
        }
        
        validateRefundRequest(paymentId, amount);
        checkRateLimit();
        
        try {
            // Build refund request
            Map<String, Object> refundRequest = buildRefundRequest(amount);
            
            // Make POST request to refund endpoint
            String url = BASE_URL + "/payments/" + paymentId + "/refund";
            HttpHeaders headers = createHeaders();
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(refundRequest, headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                url, HttpMethod.POST, entity, Map.class);
            
            incrementRequestCount();
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                
                RefundResult result = parseRefundResponse(responseBody, paymentId);
                
                if (result.isSuccessful()) {
                    logger.info("Refund processed successfully: {}", result.getRefundId());
                } else {
                    logger.warn("Refund processing failed: {}", result.getErrorMessage());
                }
                
                return result;
            } else {
                throw new RuntimeException("Invalid response from Razorpay API: " + response.getStatusCode());
            }
            
        } catch (HttpClientErrorException e) {
            logger.error("Client error processing refund: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            return createFailedRefundResult(paymentId, amount, "Refund failed: " + e.getMessage());
        } catch (HttpServerErrorException e) {
            logger.error("Server error processing refund: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            return createFailedRefundResult(paymentId, amount, "Razorpay service unavailable: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error processing refund", e);
            return createFailedRefundResult(paymentId, amount, "Refund processing failed: " + e.getMessage());
        }
    }
    
    /**
     * Build order parameters for payment order creation.
     */
    private Map<String, Object> buildOrderParams(PaymentRequest request) {
        Map<String, Object> params = new HashMap<>();
        
        // Convert amountPerPerson to paise (Razorpay uses smallest currency unit)
        int amountInPaise = (int) (request.getAmount() * 100);
        params.put("amount", amountInPaise);
        params.put("currency", request.getCurrency().toUpperCase());
        
        // Generate unique receipt ID
        params.put("receipt", generateReceiptId());
        
        // Add notes for tracking
        Map<String, String> notes = new HashMap<>();
        notes.put("booking_type", request.getBookingType());
        notes.put("user_id", request.getUserId());
        notes.put("itinerary_id", request.getItineraryId());
        if (request.getDescription() != null) {
            notes.put("description", request.getDescription());
        }
        params.put("notes", notes);
        
        return params;
    }
    
    /**
     * Build refund parameters for refund processing.
     */
    private Map<String, Object> buildRefundParams(RefundRequest request) {
        Map<String, Object> params = new HashMap<>();
        
        if (request.getAmount() != null) {
            // Convert amountPerPerson to paise for partial refunds
            int amountInPaise = (int) (request.getAmount() * 100);
            params.put("amount", amountInPaise);
        }
        // If amountPerPerson is null, it will be a full refund
        
        if (request.getReason() != null) {
            Map<String, String> notes = new HashMap<>();
            notes.put("reason", request.getReason());
            params.put("notes", notes);
        }
        
        return params;
    }
    
    /**
     * Verify payment signature using HMAC SHA256.
     */
    private boolean verifySignature(String orderId, String paymentId, String signature) {
        try {
            String payload = orderId + "|" + paymentId;
            String expectedSignature = generateHmacSha256(payload, keySecret);
            return expectedSignature.equals(signature);
        } catch (Exception e) {
            logger.error("Error verifying payment signature", e);
            return false;
        }
    }
    
    /**
     * Generate HMAC SHA256 signature.
     */
    private String generateHmacSha256(String data, String secret) throws Exception {
        javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
        javax.crypto.spec.SecretKeySpec secretKeySpec = new javax.crypto.spec.SecretKeySpec(secret.getBytes(), "HmacSHA256");
        mac.init(secretKeySpec);
        byte[] hash = mac.doFinal(data.getBytes());
        
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
    
    /**
     * Fetch payment details from Razorpay API.
     */
    private PaymentResult fetchPaymentDetails(String paymentId) {
        try {
            String url = BASE_URL + "/payments/" + paymentId;
            HttpHeaders headers = createHeaders();
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return mapToPaymentResult(response.getBody());
            } else {
                throw new RuntimeException("Failed to fetch payment details");
            }
        } catch (Exception e) {
            logger.error("Error fetching payment details for {}", paymentId, e);
            throw new RuntimeException("Failed to fetch payment details", e);
        }
    }
    
    /**
     * Map Razorpay order response to PaymentOrder.
     */
    private PaymentOrder mapToPaymentOrder(Map<String, Object> orderData, PaymentRequest request) {
        PaymentOrder order = new PaymentOrder();
        order.setOrderId((String) orderData.get("id"));
        order.setAmount(request.getAmount());
        order.setCurrency(request.getCurrency());
        order.setStatus((String) orderData.get("status"));
        order.setCreatedAt(LocalDateTime.now());
        order.setBookingType(request.getBookingType());
        order.setUserId(request.getUserId());
        order.setItineraryId(request.getItineraryId());
        order.setDescription(request.getDescription());
        return order;
    }
    
    /**
     * Map Razorpay payment response to PaymentResult.
     */
    private PaymentResult mapToPaymentResult(Map<String, Object> paymentData) {
        PaymentResult result = new PaymentResult();
        result.setPaymentId((String) paymentData.get("id"));
        result.setStatus("captured".equals(paymentData.get("status")) ? "SUCCESS" : "FAILED");
        
        // Convert amountPerPerson from paise to rupees
        Integer amountInPaise = (Integer) paymentData.get("amountPerPerson");
        if (amountInPaise != null) {
            result.setAmount(amountInPaise / 100.0);
        }
        
        result.setCurrency((String) paymentData.get("currency"));
        result.setPaymentMethod((String) paymentData.get("method"));
        result.setTransactionId((String) paymentData.get("acquirer_data.rrn"));
        
        // Extract card details if available
        @SuppressWarnings("unchecked")
        Map<String, Object> cardData = (Map<String, Object>) paymentData.get("card");
        if (cardData != null) {
            result.setCardLast4((String) cardData.get("last4"));
            result.setCardType((String) cardData.get("type"));
        }
        
        result.setTimestamp(LocalDateTime.now());
        return result;
    }
    
    /**
     * Map Razorpay refund response to RefundResult.
     */
    private RefundResult mapToRefundResult(Map<String, Object> refundData, RefundRequest request) {
        RefundResult result = new RefundResult();
        result.setRefundId((String) refundData.get("id"));
        result.setPaymentId(request.getPaymentId());
        result.setStatus((String) refundData.get("status"));
        
        // Convert amountPerPerson from paise to rupees
        Integer amountInPaise = (Integer) refundData.get("amountPerPerson");
        if (amountInPaise != null) {
            result.setRefundAmount(amountInPaise / 100.0);
        }
        
        result.setCurrency((String) refundData.get("currency"));
        result.setReason(request.getReason());
        result.setTimestamp(LocalDateTime.now());
        return result;
    }
    
    /**
     * Create failed payment result.
     */
    private PaymentResult createFailedPaymentResult(PaymentVerification verification, String errorMessage) {
        PaymentResult result = new PaymentResult();
        result.setPaymentId(verification.getPaymentId());
        result.setStatus("FAILED");
        result.setErrorMessage(errorMessage);
        result.setTimestamp(LocalDateTime.now());
        return result;
    }
    
    /**
     * Generate unique receipt ID.
     */
    private String generateReceiptId() {
        return "rcpt_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
    }
    
    /**
     * Validate payment request parameters.
     */
    private void validatePaymentRequest(PaymentRequest request) {
        if (request.getAmount() == null || request.getAmount() <= 0) {
            throw new IllegalArgumentException("Payment amountPerPerson must be greater than 0");
        }
        
        if (request.getCurrency() == null || request.getCurrency().trim().isEmpty()) {
            throw new IllegalArgumentException("Currency is required");
        }
        
        if (request.getBookingType() == null || request.getBookingType().trim().isEmpty()) {
            throw new IllegalArgumentException("Booking type is required");
        }
        
        if (request.getUserId() == null || request.getUserId().trim().isEmpty()) {
            throw new IllegalArgumentException("User ID is required");
        }
    }
    
    /**
     * Validate payment verification parameters.
     */
    private void validatePaymentVerification(PaymentVerification verification) {
        if (verification.getOrderId() == null || verification.getOrderId().trim().isEmpty()) {
            throw new IllegalArgumentException("Order ID is required");
        }
        
        if (verification.getPaymentId() == null || verification.getPaymentId().trim().isEmpty()) {
            throw new IllegalArgumentException("Payment ID is required");
        }
        
        if (verification.getSignature() == null || verification.getSignature().trim().isEmpty()) {
            throw new IllegalArgumentException("Payment signature is required");
        }
    }
    
    /**
     * Validate refund request parameters.
     */
    private void validateRefundRequestNew(RefundRequest request) {
        if (request.getPaymentId() == null || request.getPaymentId().trim().isEmpty()) {
            throw new IllegalArgumentException("Payment ID is required for refund");
        }
    }
    
    /**
     * Build payment request for Razorpay API.
     */
    private Map<String, Object> buildPaymentRequest(PaymentDetails details) {
        Map<String, Object> request = new HashMap<>();
        
        // Convert amountPerPerson to paise (Razorpay uses smallest currency unit)
        int amountInPaise = (int) (details.getAmount() * 100);
        request.put("amount", amountInPaise);
        request.put("currency", details.getCurrency());
        request.put("receipt", generateReceiptNumber());
        
        // Add payment method details
        if (details.getPaymentMethod() != null) {
            request.put("method", details.getPaymentMethod());
        }
        
        // Add description
        request.put("description", "TripPlanner booking payment");
        
        // Add notes for tracking
        Map<String, String> notes = new HashMap<>();
        notes.put("source", "TripPlanner");
        notes.put("timestamp", String.valueOf(System.currentTimeMillis()));
        request.put("notes", notes);
        
        return request;
    }
    
    /**
     * Build refund request for Razorpay API.
     */
    private Map<String, Object> buildRefundRequest(double amount) {
        Map<String, Object> request = new HashMap<>();
        
        // Convert amountPerPerson to paise (Razorpay uses smallest currency unit)
        int amountInPaise = (int) (amount * 100);
        request.put("amount", amountInPaise);
        
        // Add notes for tracking
        Map<String, String> notes = new HashMap<>();
        notes.put("reason", "Customer requested refund");
        notes.put("source", "TripPlanner");
        notes.put("timestamp", String.valueOf(System.currentTimeMillis()));
        request.put("notes", notes);
        
        return request;
    }
    
    /**
     * Parse payment response from Razorpay API.
     */
    private PaymentResult parsePaymentResponse(Map<String, Object> response) {
        PaymentResult result = new PaymentResult();
        
        result.setPaymentId((String) response.get("id"));
        result.setStatus(parsePaymentStatus((String) response.get("status")));
        
        // Convert amountPerPerson from paise to currency units
        Integer amountInPaise = (Integer) response.get("amountPerPerson");
        if (amountInPaise != null) {
            result.setAmount(amountInPaise / 100.0);
        }
        
        result.setCurrency((String) response.get("currency"));
        result.setPaymentMethod((String) response.get("method"));
        
        // Parse error information if present
        if (response.containsKey("error_code")) {
            result.setErrorCode((String) response.get("error_code"));
            result.setErrorMessage((String) response.get("error_description"));
            result.setStatus("FAILED");
        }
        
        // Set transaction ID
        result.setTransactionId((String) response.get("acquirer_data"));
        
        return result;
    }
    
    /**
     * Parse refund response from Razorpay API.
     */
    private RefundResult parseRefundResponse(Map<String, Object> response, String paymentId) {
        RefundResult result = new RefundResult();
        
        result.setRefundId((String) response.get("id"));
        result.setPaymentId(paymentId);
        result.setStatus(parseRefundStatus((String) response.get("status")));
        
        // Convert amountPerPerson from paise to currency units
        Integer amountInPaise = (Integer) response.get("amountPerPerson");
        if (amountInPaise != null) {
            result.setRefundAmount(amountInPaise / 100.0);
        }
        
        result.setCurrency((String) response.get("currency"));
        
        // Parse error information if present
        if (response.containsKey("error_code")) {
            result.setErrorCode((String) response.get("error_code"));
            result.setErrorMessage((String) response.get("error_description"));
            result.setStatus("FAILED");
        }
        
        // Set estimated arrival time
        result.setEstimatedArrival("3-5 business days");
        
        return result;
    }
    
    /**
     * Parse payment status from Razorpay response.
     */
    private String parsePaymentStatus(String razorpayStatus) {
        if (razorpayStatus == null) {
            return "FAILED";
        }
        
        switch (razorpayStatus.toLowerCase()) {
            case "captured":
            case "authorized":
                return "SUCCESS";
            case "created":
            case "pending":
                return "PENDING";
            case "failed":
            default:
                return "FAILED";
        }
    }
    
    /**
     * Parse refund status from Razorpay response.
     */
    private String parseRefundStatus(String razorpayStatus) {
        if (razorpayStatus == null) {
            return "FAILED";
        }
        
        switch (razorpayStatus.toLowerCase()) {
            case "processed":
                return "SUCCESS";
            case "pending":
                return "PENDING";
            case "failed":
            default:
                return "FAILED";
        }
    }
    
    /**
     * Create HTTP headers with Basic authentication for Razorpay API.
     */
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        // Create Basic authentication header (keyId:keySecret base64 encoded)
        String auth = keyId + ":" + keySecret;
        byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes(StandardCharsets.UTF_8));
        String authHeader = "Basic " + new String(encodedAuth);
        headers.set("Authorization", authHeader);
        
        headers.set("User-Agent", "TripPlanner/1.0");
        
        return headers;
    }
    
    /**
     * Validate payment details before processing.
     */
    private void validatePaymentDetails(PaymentDetails details) {
        if (details.getAmount() == null || details.getAmount() <= 0) {
            throw new IllegalArgumentException("Payment amountPerPerson must be greater than 0");
        }
        
        if (details.getCurrency() == null || details.getCurrency().trim().isEmpty()) {
            throw new IllegalArgumentException("Currency is required");
        }
        
        // Validate amountPerPerson limits (Razorpay limits)
        if (details.getAmount() < 1) {
            throw new IllegalArgumentException("Minimum payment amountPerPerson is 1 " + details.getCurrency());
        }
        
        if (details.getAmount() > 1000000) {
            throw new IllegalArgumentException("Maximum payment amountPerPerson is 1,000,000 " + details.getCurrency());
        }
    }
    
    /**
     * Validate refund request parameters.
     */
    private void validateRefundRequest(String paymentId, double amount) {
        if (paymentId.length() < 10) {
            throw new IllegalArgumentException("Invalid payment ID format");
        }
        
        if (amount < 1) {
            throw new IllegalArgumentException("Minimum refund amountPerPerson is 1");
        }
    }
    
    /**
     * Create a failed payment result for error cases.
     */
    private PaymentResult createFailedPaymentResult(PaymentDetails details, String errorMessage) {
        PaymentResult result = new PaymentResult();
        result.setPaymentId("failed_" + System.currentTimeMillis());
        result.setStatus("FAILED");
        result.setAmount(details.getAmount());
        result.setCurrency(details.getCurrency());
        result.setErrorMessage(errorMessage);
        return result;
    }
    
    /**
     * Create a failed refund result for error cases.
     */
    private RefundResult createFailedRefundResult(String paymentId, double amount, String errorMessage) {
        RefundResult result = new RefundResult();
        result.setRefundId("failed_" + System.currentTimeMillis());
        result.setPaymentId(paymentId);
        result.setStatus("FAILED");
        result.setRefundAmount(amount);
        result.setErrorMessage(errorMessage);
        return result;
    }
    
    /**
     * Generate a unique receipt number for payments.
     */
    private String generateReceiptNumber() {
        return "TP_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
    
    /**
     * Check rate limits before making API requests (1000 requests/minute).
     */
    private void checkRateLimit() {
        if (!rateLimitEnabled) {
            return;
        }
        
        // Reset minute counter if it's a new minute
        long currentTime = System.currentTimeMillis();
        long lastReset = lastResetTime.get();
        
        if (currentTime - lastReset > 60 * 1000) { // 1 minute
            if (lastResetTime.compareAndSet(lastReset, currentTime)) {
                minuteRequestCount.set(0);
                logger.debug("Reset minute request count for Razorpay API");
            }
        }
        
        // Check if we're approaching the limit
        int currentCount = minuteRequestCount.get();
        if (currentCount >= MINUTE_LIMIT) {
            throw new RuntimeException("Rate limit exceeded for Razorpay API: " + currentCount + "/" + MINUTE_LIMIT + " per minute");
        }
        
        // Warn when approaching limit
        if (currentCount > MINUTE_LIMIT * 0.9) {
            logger.warn("Approaching Razorpay API rate limit: {}/{} per minute", currentCount, MINUTE_LIMIT);
        }
    }
    
    /**
     * Increment the minute request count.
     */
    private void incrementRequestCount() {
        int newCount = minuteRequestCount.incrementAndGet();
        logger.debug("Razorpay API request count: {}/{} per minute", newCount, MINUTE_LIMIT);
    }
    
    /**
     * Get current rate limit statistics.
     */
    public RateLimitStats getRateLimitStats() {
        return new RateLimitStats(
            minuteRequestCount.get(),
            MINUTE_LIMIT,
            lastResetTime.get()
        );
    }
    
    /**
     * Rate limit statistics for monitoring.
     */
    public static class RateLimitStats {
        private final int currentCount;
        private final int minuteLimit;
        private final long lastResetTime;
        
        public RateLimitStats(int currentCount, int minuteLimit, long lastResetTime) {
            this.currentCount = currentCount;
            this.minuteLimit = minuteLimit;
            this.lastResetTime = lastResetTime;
        }
        
        public int getCurrentCount() {
            return currentCount;
        }
        
        public int getMinuteLimit() {
            return minuteLimit;
        }
        
        public long getLastResetTime() {
            return lastResetTime;
        }
        
        public double getUsagePercentage() {
            return minuteLimit > 0 ? (double) currentCount / minuteLimit * 100 : 0;
        }
        
        @Override
        public String toString() {
            return String.format("RateLimitStats{currentCount=%d, minuteLimit=%d, usage=%.1f%%}", 
                               currentCount, minuteLimit, getUsagePercentage());
        }
    }
    
    /**
     * Create a Razorpay order for payment processing.
     */
    public com.tripplanner.controller.BookingController.RazorpayOrderRes createOrder(Object request) {
        logger.info("Creating Razorpay order for request: {}", request);
        
        // This is a placeholder implementation
        // In a real implementation, you would call the Razorpay API to create an order
        String orderId = "order_" + System.currentTimeMillis();
        Long amount = 1000L; // Default amountPerPerson
        String currency = "INR";
        String receipt = "receipt_" + System.currentTimeMillis();
        
        return new com.tripplanner.controller.BookingController.RazorpayOrderRes(
            orderId, amount, currency, receipt
        );
    }
    
    /**
     * Handle Razorpay webhook notifications.
     */
    public void handleWebhook(Object request, String body) {
        logger.info("Handling Razorpay webhook with body: {}", body);
        
        // This is a placeholder implementation
        // In a real implementation, you would:
        // 1. Verify the webhook signature
        // 2. Parse the webhook payload
        // 3. Update payment status in your system
        // 4. Send notifications if needed
        
        logger.info("Webhook processed successfully");
    }
}