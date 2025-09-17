package com.tripplanner.security;

import com.google.cloud.firestore.Firestore;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * Filter to handle idempotency for POST requests.
 */
@Component
@org.springframework.boot.autoconfigure.condition.ConditionalOnBean(Firestore.class)
public class IdempotencyFilter extends OncePerRequestFilter {
    
    private static final Logger logger = LoggerFactory.getLogger(IdempotencyFilter.class);
    private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
    private static final String COLLECTION_NAME = "idempotency_cache";
    private static final int TTL_HOURS = 24;
    
    private final Firestore firestore;
    
    public IdempotencyFilter(Firestore firestore) {
        this.firestore = firestore;
    }
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                  HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        
        // Only process POST requests that should be idempotent
        if (!shouldProcessIdempotency(request)) {
            filterChain.doFilter(request, response);
            return;
        }
        
        String idempotencyKey = request.getHeader(IDEMPOTENCY_KEY_HEADER);
        
        if (idempotencyKey == null || idempotencyKey.trim().isEmpty()) {
            // No idempotency key provided - continue normally
            filterChain.doFilter(request, response);
            return;
        }
        
        try {
            // Generate cache key from idempotency key + request signature
            String cacheKey = generateCacheKey(idempotencyKey, request);
            
            // Check if request was already processed
            IdempotencyRecord existingRecord = getIdempotencyRecord(cacheKey);
            
            if (existingRecord != null && !isExpired(existingRecord)) {
                // Request already processed - return cached response
                logger.info("Returning cached response for idempotency key: {}", idempotencyKey);
                writeResponse(response, existingRecord);
                return;
            }
            
            // Wrap response to capture the result
            IdempotencyResponseWrapper responseWrapper = new IdempotencyResponseWrapper(response);
            
            // Continue with request processing
            filterChain.doFilter(request, responseWrapper);
            
            // Cache the response if successful
            if (responseWrapper.getStatus() >= 200 && responseWrapper.getStatus() < 300) {
                cacheResponse(cacheKey, responseWrapper);
            }
            
        } catch (Exception e) {
            logger.error("Error processing idempotency", e);
            // Continue without idempotency on error
            filterChain.doFilter(request, response);
        }
    }
    
    /**
     * Check if request should be processed for idempotency.
     */
    private boolean shouldProcessIdempotency(HttpServletRequest request) {
        if (!HttpMethod.POST.matches(request.getMethod())) {
            return false;
        }
        
        String path = request.getRequestURI();
        
        // Process idempotency for booking and payment related endpoints
        return path.contains("/payments/") ||
               path.contains("/providers/") ||
               path.contains(":book") ||
               path.contains("/bookings");
    }
    
    /**
     * Generate cache key from idempotency key and request signature.
     */
    private String generateCacheKey(String idempotencyKey, HttpServletRequest request) throws NoSuchAlgorithmException {
        // Create request signature from method, path, and user
        StringBuilder signature = new StringBuilder();
        signature.append(request.getMethod()).append(":");
        signature.append(request.getRequestURI()).append(":");
        
        // Add user context if available
        if (request.getUserPrincipal() != null) {
            signature.append(request.getUserPrincipal().getName());
        }
        
        // Hash the signature
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(signature.toString().getBytes(StandardCharsets.UTF_8));
        String hashString = bytesToHex(hash);
        
        return idempotencyKey + ":" + hashString;
    }
    
    /**
     * Get idempotency record from cache.
     */
    private IdempotencyRecord getIdempotencyRecord(String cacheKey) throws ExecutionException, InterruptedException {
        var document = firestore.collection(COLLECTION_NAME)
                .document(cacheKey)
                .get()
                .get();
        
        if (document.exists()) {
            return document.toObject(IdempotencyRecord.class);
        }
        
        return null;
    }
    
    /**
     * Check if idempotency record is expired.
     */
    private boolean isExpired(IdempotencyRecord record) {
        if (record.getExpiresAt() == null) {
            return true;
        }
        return Instant.now().isAfter(record.getExpiresAt());
    }
    
    /**
     * Cache the response.
     */
    private void cacheResponse(String cacheKey, IdempotencyResponseWrapper responseWrapper) {
        try {
            IdempotencyRecord record = new IdempotencyRecord();
            record.setCacheKey(cacheKey);
            record.setStatus(responseWrapper.getStatus());
            record.setHeaders(responseWrapper.getCapturedHeaders());
            record.setBody(responseWrapper.getCapturedBody());
            record.setCreatedAt(Instant.now());
            record.setExpiresAt(Instant.now().plus(TTL_HOURS, ChronoUnit.HOURS));
            
            firestore.collection(COLLECTION_NAME)
                    .document(cacheKey)
                    .set(record);
            
            logger.debug("Cached idempotent response for key: {}", cacheKey);
            
        } catch (Exception e) {
            logger.error("Failed to cache idempotent response", e);
            // Don't fail the request if caching fails
        }
    }
    
    /**
     * Write cached response.
     */
    private void writeResponse(HttpServletResponse response, IdempotencyRecord record) throws IOException {
        response.setStatus(record.getStatus());
        
        // Set cached headers
        if (record.getHeaders() != null) {
            for (Map.Entry<String, String> header : record.getHeaders().entrySet()) {
                response.setHeader(header.getKey(), header.getValue());
            }
        }
        
        // Set cached body
        if (record.getBody() != null) {
            response.getWriter().write(record.getBody());
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
     * Idempotency record for caching.
     */
    public static class IdempotencyRecord {
        private String cacheKey;
        private int status;
        private Map<String, String> headers = new HashMap<>();
        private String body;
        private Instant createdAt;
        private Instant expiresAt;
        
        // Getters and setters
        public String getCacheKey() { return cacheKey; }
        public void setCacheKey(String cacheKey) { this.cacheKey = cacheKey; }
        public int getStatus() { return status; }
        public void setStatus(int status) { this.status = status; }
        public Map<String, String> getHeaders() { return headers; }
        public void setHeaders(Map<String, String> headers) { this.headers = headers; }
        public String getBody() { return body; }
        public void setBody(String body) { this.body = body; }
        public Instant getCreatedAt() { return createdAt; }
        public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
        public Instant getExpiresAt() { return expiresAt; }
        public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    }
}
