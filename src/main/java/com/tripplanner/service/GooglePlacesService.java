package com.tripplanner.service;

import com.tripplanner.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Service for integrating with Google Places API.
 * Provides place details, photos, and reviews with rate limiting and error handling.
 */
@Service
public class GooglePlacesService {
    
    private static final Logger logger = LoggerFactory.getLogger(GooglePlacesService.class);
    private static final String BASE_URL = "https://maps.googleapis.com/maps/api/place";
    
    // Rate limiting tracking
    private final AtomicInteger dailyRequestCount = new AtomicInteger(0);
    private final AtomicLong lastResetTime = new AtomicLong(System.currentTimeMillis());
    private static final int DAILY_LIMIT_FREE = 1000;
    private static final int DAILY_LIMIT_PAID = 100000;
    
    // Circuit breaker pattern
    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
    private final AtomicLong circuitBreakerOpenTime = new AtomicLong(0);
    private final AtomicBoolean circuitBreakerOpen = new AtomicBoolean(false);
    private static final int FAILURE_THRESHOLD = 5;
    private static final long CIRCUIT_BREAKER_TIMEOUT = 60000; // 1 minute
    
    @Value("${google.places.api.key:}")
    private String apiKey;
    
    @Value("${google.places.daily.limit:1000}")
    private int dailyLimit;
    
    @Value("${google.places.rate.limit.enabled:true}")
    private boolean rateLimitEnabled;

    private RestTemplate restTemplate;

    public GooglePlacesService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    
    /**
     * Get detailed information about a place by place ID.
     * Includes photos, reviews, opening hours, rating, and price level.
     */
    @Cacheable(value = "placeDetails", key = "#placeId")
    public PlaceDetails getPlaceDetails(String placeId) {
        logger.debug("Getting place details for placeId: {}", placeId);
        
        if (placeId == null || placeId.trim().isEmpty()) {
            throw new IllegalArgumentException("Place ID cannot be null or empty");
        }
        
        // Check rate limits
        checkRateLimit();
        
        // Check circuit breaker
        checkCircuitBreaker();
        
        try {
            // Build URL with parameters
            String url = UriComponentsBuilder.fromHttpUrl(BASE_URL + "/details/json")
                .queryParam("place_id", placeId)
                .queryParam("fields", "photos,reviews,opening_hours,price_level,rating,name,formatted_address,geometry,types,website,formatted_phone_number,international_phone_number")
                .queryParam("key", apiKey)
                .toUriString();
            
            // Make GET request with retry logic
            PlaceDetailsResponse response = makeRequestWithRetry(url, PlaceDetailsResponse.class);
            
            // Increment request count
            incrementRequestCount();
            
            // Handle API response
            if (response.isSuccessful() && response.getResult() != null) {
                logger.debug("Successfully retrieved place details for {}: {}", placeId, response.getResult().getName());
                recordSuccess(); // Reset circuit breaker on success
                return response.getResult();
            } else if (response.isRateLimited()) {
                recordFailure();
                throw new RuntimeException("Google Places API rate limit exceeded");
            } else if (response.isNotFound()) {
                // Not found is not considered a failure for circuit breaker
                throw new RuntimeException("Place not found: " + placeId);
            } else {
                recordFailure();
                throw new RuntimeException("Google Places API error: " + response.getStatus() + 
                    (response.getErrorMessage() != null ? " - " + response.getErrorMessage() : ""));
            }
            
        } catch (Exception e) {
            logger.error("Failed to get place details for {}: {}", placeId, e.getMessage(), e);
            recordFailure();
            throw new RuntimeException("Failed to get place details: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get photos for a place by place ID.
     * Returns a list of Photo objects with URLs and metadata.
     */
    @Cacheable(value = "placePhotos", key = "#placeId")
    public List<Photo> getPlacePhotos(String placeId) {
        logger.debug("Getting place photos for placeId: {}", placeId);
        
        try {
            PlaceDetails placeDetails = getPlaceDetails(placeId);
            
            if (placeDetails != null && placeDetails.getPhotos() != null) {
                logger.debug("Found {} photos for place {}", placeDetails.getPhotos().size(), placeId);
                return placeDetails.getPhotos();
            } else {
                logger.debug("No photos found for place {}", placeId);
                return new ArrayList<>();
            }
            
        } catch (Exception e) {
            logger.error("Failed to get place photos for {}: {}", placeId, e.getMessage(), e);
            return new ArrayList<>(); // Return empty list on error
        }
    }
    
    /**
     * Get reviews for a place by place ID.
     * Returns a list of Review objects with ratings and text.
     */
    @Cacheable(value = "placeReviews", key = "#placeId")
    public List<Review> getPlaceReviews(String placeId) {
        logger.debug("Getting place reviews for placeId: {}", placeId);
        
        try {
            PlaceDetails placeDetails = getPlaceDetails(placeId);
            
            if (placeDetails != null && placeDetails.getReviews() != null) {
                logger.debug("Found {} reviews for place {}", placeDetails.getReviews().size(), placeId);
                return placeDetails.getReviews();
            } else {
                logger.debug("No reviews found for place {}", placeId);
                return new ArrayList<>();
            }
            
        } catch (Exception e) {
            logger.error("Failed to get place reviews for {}: {}", placeId, e.getMessage(), e);
            return new ArrayList<>(); // Return empty list on error
        }
    }
    
    /**
     * Make HTTP request with exponential backoff retry mechanism.
     */
    private <T> T makeRequestWithRetry(String url, Class<T> responseType) {
        int maxRetries = 5;
        int retryDelay = 1000; // Start with 1 second
        
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                ResponseEntity<T> response = restTemplate.getForEntity(url, responseType);
                
                if (response.getStatusCode().is2xxSuccessful()) {
                    return response.getBody();
                } else {
                    throw new RuntimeException("HTTP " + response.getStatusCode() + " response from Google Places API");
                }
                
            } catch (HttpClientErrorException e) {
                if (e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                    logger.warn("Rate limit hit on attempt {} of {}, retrying in {}ms", attempt, maxRetries, retryDelay);
                    
                    if (attempt == maxRetries) {
                        throw new RuntimeException("Rate limit exceeded after " + maxRetries + " attempts", e);
                    }
                    
                    // Wait before retry
                    try {
                        Thread.sleep(retryDelay);
                        retryDelay = Math.min(retryDelay * 2, 8000); // Exponential backoff, max 8 seconds
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Request interrupted", ie);
                    }
                } else {
                    // Non-retryable client error
                    throw new RuntimeException("Google Places API client error: " + e.getStatusCode(), e);
                }
                
            } catch (HttpServerErrorException e) {
                logger.warn("Server error on attempt {} of {}, retrying in {}ms", attempt, maxRetries, retryDelay);
                
                if (attempt == maxRetries) {
                    throw new RuntimeException("Server error after " + maxRetries + " attempts", e);
                }
                
                // Wait before retry
                try {
                    Thread.sleep(retryDelay);
                    retryDelay = Math.min(retryDelay * 2, 8000); // Exponential backoff, max 8 seconds
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Request interrupted", ie);
                }
                
            } catch (Exception e) {
                if (attempt == maxRetries) {
                    throw new RuntimeException("Request failed after " + maxRetries + " attempts", e);
                }
                
                logger.warn("Request failed on attempt {} of {}: {}, retrying in {}ms", 
                           attempt, maxRetries, e.getMessage(), retryDelay);
                
                // Wait before retry
                try {
                    Thread.sleep(retryDelay);
                    retryDelay = Math.min(retryDelay * 2, 8000); // Exponential backoff, max 8 seconds
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Request interrupted", ie);
                }
            }
        }
        
        throw new RuntimeException("Unexpected error in retry logic");
    }
    
    /**
     * Check if we're within rate limits.
     */
    private void checkRateLimit() {
        if (!rateLimitEnabled) {
            return;
        }
        
        // Reset daily counter if it's a new day
        long currentTime = System.currentTimeMillis();
        long lastReset = lastResetTime.get();
        
        if (currentTime - lastReset > 24 * 60 * 60 * 1000) { // 24 hours
            if (lastResetTime.compareAndSet(lastReset, currentTime)) {
                dailyRequestCount.set(0);
                logger.info("Reset daily request count for Google Places API");
            }
        }
        
        // Check if we're approaching the limit
        int currentCount = dailyRequestCount.get();
        if (currentCount >= dailyLimit) {
            throw new RuntimeException("Daily rate limit exceeded for Google Places API: " + currentCount + "/" + dailyLimit);
        }
        
        // Warn when approaching limit
        if (currentCount > dailyLimit * 0.9) {
            logger.warn("Approaching Google Places API daily limit: {}/{}", currentCount, dailyLimit);
        }
    }
    
    /**
     * Increment the daily request count.
     */
    private void incrementRequestCount() {
        int newCount = dailyRequestCount.incrementAndGet();
        logger.debug("Google Places API request count: {}/{}", newCount, dailyLimit);
    }
    
    /**
     * Get current rate limit statistics.
     */
    public RateLimitStats getRateLimitStats() {
        return new RateLimitStats(
            dailyRequestCount.get(),
            dailyLimit,
            lastResetTime.get()
        );
    }
    
    /**
     * Rate limit statistics for monitoring.
     */
    public static class RateLimitStats {
        private final int currentCount;
        private final int dailyLimit;
        private final long lastResetTime;
        
        public RateLimitStats(int currentCount, int dailyLimit, long lastResetTime) {
            this.currentCount = currentCount;
            this.dailyLimit = dailyLimit;
            this.lastResetTime = lastResetTime;
        }
        
        public int getCurrentCount() {
            return currentCount;
        }
        
        public int getDailyLimit() {
            return dailyLimit;
        }
        
        public long getLastResetTime() {
            return lastResetTime;
        }
        
        public double getUsagePercentage() {
            return dailyLimit > 0 ? (double) currentCount / dailyLimit * 100 : 0;
        }
        
        @Override
        public String toString() {
            return String.format("RateLimitStats{currentCount=%d, dailyLimit=%d, usage=%.1f%%}", 
                               currentCount, dailyLimit, getUsagePercentage());
        }
    }
    
    /**
     * Check circuit breaker state before making requests.
     */
    private void checkCircuitBreaker() {
        if (!circuitBreakerOpen.get()) {
            return; // Circuit is closed, proceed normally
        }
        
        long currentTime = System.currentTimeMillis();
        long openTime = circuitBreakerOpenTime.get();
        
        if (currentTime - openTime > CIRCUIT_BREAKER_TIMEOUT) {
            // Try to close the circuit breaker (half-open state)
            logger.info("Circuit breaker timeout reached, attempting to close circuit breaker");
            circuitBreakerOpen.set(false);
            consecutiveFailures.set(0);
        } else {
            throw new RuntimeException("Google Places API circuit breaker is open. Service temporarily unavailable.");
        }
    }
    
    /**
     * Record a successful API call.
     */
    private void recordSuccess() {
        if (consecutiveFailures.get() > 0) {
            logger.info("Google Places API call succeeded, resetting failure count");
            consecutiveFailures.set(0);
        }
        
        if (circuitBreakerOpen.get()) {
            logger.info("Google Places API call succeeded, closing circuit breaker");
            circuitBreakerOpen.set(false);
        }
    }
    
    /**
     * Record a failed API call and potentially open circuit breaker.
     */
    private void recordFailure() {
        int failures = consecutiveFailures.incrementAndGet();
        logger.warn("Google Places API failure recorded. Consecutive failures: {}", failures);
        
        if (failures >= FAILURE_THRESHOLD && !circuitBreakerOpen.get()) {
            logger.error("Google Places API failure threshold reached ({}), opening circuit breaker", FAILURE_THRESHOLD);
            circuitBreakerOpen.set(true);
            circuitBreakerOpenTime.set(System.currentTimeMillis());
        }
    }
    
    /**
     * Get circuit breaker statistics for monitoring.
     */
    public CircuitBreakerStats getCircuitBreakerStats() {
        return new CircuitBreakerStats(
            circuitBreakerOpen.get(),
            consecutiveFailures.get(),
            circuitBreakerOpenTime.get()
        );
    }
    
    /**
     * Circuit breaker statistics for monitoring.
     */
    public static class CircuitBreakerStats {
        private final boolean isOpen;
        private final int consecutiveFailures;
        private final long openTime;
        
        public CircuitBreakerStats(boolean isOpen, int consecutiveFailures, long openTime) {
            this.isOpen = isOpen;
            this.consecutiveFailures = consecutiveFailures;
            this.openTime = openTime;
        }
        
        public boolean isOpen() {
            return isOpen;
        }
        
        public int getConsecutiveFailures() {
            return consecutiveFailures;
        }
        
        public long getOpenTime() {
            return openTime;
        }
        
        public long getTimeUntilRetry() {
            if (!isOpen) {
                return 0;
            }
            long elapsed = System.currentTimeMillis() - openTime;
            return Math.max(0, CIRCUIT_BREAKER_TIMEOUT - elapsed);
        }
        
        @Override
        public String toString() {
            return String.format("CircuitBreakerStats{isOpen=%s, consecutiveFailures=%d, timeUntilRetry=%dms}", 
                               isOpen, consecutiveFailures, getTimeUntilRetry());
        }
    }
    
    /**
     * Search for a place by name and location.
     * Returns the first matching place with coordinates.
     */
    @Cacheable(value = "placeSearch", key = "#query + '_' + #location")
    public PlaceSearchResult searchPlace(String query, String location) {
        logger.debug("Searching for place: {} near {}", query, location);
        
        if (query == null || query.trim().isEmpty()) {
            logger.warn("Empty query provided to searchPlace");
            return null;
        }
        
        // Check rate limits
        checkRateLimit();
        
        // Check circuit breaker
        checkCircuitBreaker();
        
        try {
            // Build search query
            String searchQuery = query;
            if (location != null && !location.trim().isEmpty()) {
                searchQuery = query + " " + location;
            }
            
            // Build URL with parameters for Text Search
            String url = UriComponentsBuilder.fromHttpUrl(BASE_URL + "/textsearch/json")
                .queryParam("query", searchQuery)
                .queryParam("key", apiKey)
                .toUriString();
            
            // Make GET request with retry logic
            PlaceSearchResponse response = makeRequestWithRetry(url, PlaceSearchResponse.class);
            
            // Increment request count
            incrementRequestCount();
            
            // Handle API response
            if (response != null && "OK".equals(response.getStatus()) && 
                response.getResults() != null && !response.getResults().isEmpty()) {
                
                PlaceSearchResult firstResult = response.getResults().get(0);
                logger.debug("Found place: {} at ({}, {})", 
                    firstResult.getName(), 
                    firstResult.getGeometry().getLocation().getLatitude(),
                    firstResult.getGeometry().getLocation().getLongitude());
                recordSuccess();
                return firstResult;
                
            } else if (response != null && "ZERO_RESULTS".equals(response.getStatus())) {
                logger.debug("No results found for query: {}", searchQuery);
                return null;
                
            } else {
                logger.warn("Google Places API returned status: {}", response != null ? response.getStatus() : "null");
                recordFailure();
                return null;
            }
            
        } catch (Exception e) {
            logger.error("Failed to search for place {}: {}", query, e.getMessage());
            recordFailure();
            return null; // Return null on error instead of throwing
        }
    }
}