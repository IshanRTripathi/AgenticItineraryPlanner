package com.tripplanner.service;

import com.tripplanner.dto.*;
import com.tripplanner.enums.ToolType;
import com.tripplanner.service.analytics.ItineraryMetricsTracker;
import com.tripplanner.service.cache.ToolCacheService;
import com.tripplanner.util.ToolCacheKeyGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Service for integrating with Google Places API.
 * Provides place details, photos, and reviews with rate limiting and error
 * handling.
 * 
 * CACHING: Place search results cached for 7 days
 */
@Service
public class GooglePlacesService {

    private static final Logger logger = LoggerFactory.getLogger(GooglePlacesService.class);
    private static final String BASE_URL = "https://maps.googleapis.com/maps/api/place";

    // Place Details API fields to request
    private static final String PLACE_DETAILS_FIELDS = "photos,reviews,opening_hours,price_level,rating,user_ratings_total,name,formatted_address,geometry,types,website,formatted_phone_number,international_phone_number";

    // Retry configuration
    private static final int MAX_RETRIES = 5;
    private static final int INITIAL_RETRY_DELAY_MS = 1000;
    private static final int MAX_RETRY_DELAY_MS = 8000;

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

    private final RestTemplate restTemplate;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;
    private final ItineraryMetricsTracker metricsTracker;
    
    @Autowired(required = false)
    private ToolCacheService toolCacheService;

    public GooglePlacesService(RestTemplate restTemplate, ItineraryMetricsTracker metricsTracker) {
        this.restTemplate = restTemplate;
        this.objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
        this.metricsTracker = metricsTracker;
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
            // Build URL using UriComponentsBuilder to handle encoding properly
            // This avoids double-encoding issues when RestTemplate makes the request
            String url = UriComponentsBuilder.fromHttpUrl(BASE_URL + "/details/json")
                    .queryParam("place_id", placeId)
                    .queryParam("fields", PLACE_DETAILS_FIELDS)
                    .queryParam("key", apiKey)
                    .build(false) // Don't encode - let RestTemplate handle it
                    .toUriString();

            // Track API call start time
            long startTime = System.currentTimeMillis();

            // Make GET request with retry logic
            PlaceDetailsResponse response = makeRequestWithRetry(url, PlaceDetailsResponse.class);

            // Calculate duration
            long durationMs = System.currentTimeMillis() - startTime;

            // Increment request count
            incrementRequestCount();

            // Handle API response
            if (response.isSuccessful() && response.getResult() != null) {
                PlaceDetails result = response.getResult();
                logger.info("✅ [GooglePlacesService] Successfully retrieved place details for placeId: {}", placeId);
                logger.info("   📍 Name: {}", result.getName());
                logger.info("   ⭐ Rating: {}", result.getRating());
                logger.info("   👥 User Ratings Total: {}", result.getUserRatingsTotal());
                logger.info("   💰 Price Level: {}", result.getPriceLevel());
                logger.info("   📸 Photos: {} photos", result.getPhotos() != null ? result.getPhotos().size() : 0);
                logger.info("   💬 Reviews: {} reviews", result.getReviews() != null ? result.getReviews().size() : 0);
                if (result.getPhotos() != null && !result.getPhotos().isEmpty()) {
                    logger.info("   📸 First photo reference: {}",
                            result.getPhotos().get(0).getPhotoReference().substring(0,
                                    Math.min(30, result.getPhotos().get(0).getPhotoReference().length())) + "...");
                }
                recordSuccess(); // Reset circuit breaker on success

                // Track successful API call
                String itineraryId = MDC.get("itineraryId");
                metricsTracker.trackAPICall(itineraryId, "GooglePlaces", "/place/details", durationMs, true, 200, null);

                return result;
            } else if (response.isRateLimited()) {
                recordFailure();
                // Track rate limited API call
                String itineraryId = MDC.get("itineraryId");
                metricsTracker.trackAPICall(itineraryId, "GooglePlaces", "/place/details", durationMs, false, 429,
                        "Rate limit exceeded");
                throw new RuntimeException("Google Places API rate limit exceeded");
            } else if (response.isNotFound()) {
                // Track not found API call
                String itineraryId = MDC.get("itineraryId");
                metricsTracker.trackAPICall(itineraryId, "GooglePlaces", "/place/details", durationMs, false, 404,
                        "Place not found");
                // Not found is not considered a failure for circuit breaker
                throw new RuntimeException("Place not found: " + placeId);
            } else {
                recordFailure();
                String errorMsg = "Google Places API error: " + response.getStatus() +
                        (response.getErrorMessage() != null ? " - " + response.getErrorMessage() : "");
                // Track failed API call
                String itineraryId = MDC.get("itineraryId");
                metricsTracker.trackAPICall(itineraryId, "GooglePlaces", "/place/details", durationMs, false, null,
                        errorMsg);
                throw new RuntimeException(errorMsg);
            }

        } catch (Exception e) {
            logger.error("Failed to get place details for {}: {}", placeId, e.getMessage(), e);
            recordFailure();
            // Track failed API call (exception case)
            String itineraryId = MDC.get("itineraryId");
            metricsTracker.trackAPICall(itineraryId, "GooglePlaces", "/place/details", 0, false, null, e.getMessage());
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
        int retryDelay = INITIAL_RETRY_DELAY_MS;

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                ResponseEntity<String> rawResponse = restTemplate.getForEntity(url, String.class);

                if (rawResponse.getStatusCode().is2xxSuccessful()) {
                    String rawBody = rawResponse.getBody();

                    // Parse response
                    try {
                        return objectMapper.readValue(rawBody, responseType);
                    } catch (Exception parseEx) {
                        logger.error("Failed to parse Google Places API response: {}", parseEx.getMessage());
                        if (logger.isDebugEnabled()) {
                            logger.debug("Raw response body: {}", rawBody);
                        }
                        throw new RuntimeException("Failed to parse API response: " + parseEx.getMessage(), parseEx);
                    }
                } else {
                    throw new RuntimeException("HTTP " + rawResponse.getStatusCode() + " from Google Places API");
                }

            } catch (HttpClientErrorException e) {
                if (e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS && attempt < MAX_RETRIES) {
                    logger.warn("Rate limit hit, retrying in {}ms (attempt {}/{})", retryDelay, attempt, MAX_RETRIES);
                    sleepWithInterruptHandling(retryDelay);
                    retryDelay = Math.min(retryDelay * 2, MAX_RETRY_DELAY_MS);
                } else if (attempt == MAX_RETRIES) {
                    throw new RuntimeException(
                            "Google Places API failed after " + MAX_RETRIES + " attempts: " + e.getStatusCode(), e);
                } else {
                    throw new RuntimeException("Google Places API error: " + e.getStatusCode(), e);
                }

            } catch (HttpServerErrorException e) {
                if (attempt < MAX_RETRIES) {
                    logger.warn("Server error, retrying in {}ms (attempt {}/{})", retryDelay, attempt, MAX_RETRIES);
                    sleepWithInterruptHandling(retryDelay);
                    retryDelay = Math.min(retryDelay * 2, MAX_RETRY_DELAY_MS);
                } else {
                    throw new RuntimeException("Google Places API server error after " + MAX_RETRIES + " attempts", e);
                }

            } catch (RuntimeException e) {
                throw e; // Re-throw runtime exceptions
            } catch (Exception e) {
                if (attempt < MAX_RETRIES) {
                    logger.warn("Request failed, retrying in {}ms (attempt {}/{}): {}", retryDelay, attempt,
                            MAX_RETRIES, e.getMessage());
                    sleepWithInterruptHandling(retryDelay);
                    retryDelay = Math.min(retryDelay * 2, MAX_RETRY_DELAY_MS);
                } else {
                    throw new RuntimeException("Request failed after " + MAX_RETRIES + " attempts: " + e.getMessage(),
                            e);
                }
            }
        }

        throw new RuntimeException("Unexpected error in retry logic");
    }

    /**
     * Sleep with proper interrupt handling.
     */
    private void sleepWithInterruptHandling(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Request interrupted during retry", ie);
        }
    }

    /**
     * Geocode a location string to get its coordinates using Google Geocoding API.
     * Results are cached to avoid repeated API calls for the same location.
     * 
     * @param location The location string to geocode (e.g., "Jammu Kashmir, India")
     * @return Coordinates object with lat/lng, or null if geocoding fails
     */
    @Cacheable(value = "geocoding", key = "#location.toLowerCase().trim()", unless = "#result == null")
    public com.tripplanner.dto.Coordinates geocodeLocation(String location) {
        if (location == null || location.trim().isEmpty()) {
            return null;
        }

        logger.info("🔍 [GooglePlacesService] Geocoding location: '{}'", location);

        try {
            // Build geocoding API URL
            String url = UriComponentsBuilder.fromHttpUrl("https://maps.googleapis.com/maps/api/geocode/json")
                    .queryParam("address", location)
                    .queryParam("key", apiKey)
                    .build(false)
                    .toUriString();

            logger.debug("Geocoding API URL: {}", url.replace(apiKey, "***KEY_HIDDEN***"));

            // Make request (no retry needed for geocoding, it's fast and reliable)
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                // Parse response
                com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(response.getBody());
                String status = root.path("status").asText();

                logger.debug("Geocoding API status: {}", status);

                if ("OK".equals(status)) {
                    com.fasterxml.jackson.databind.JsonNode results = root.path("results");
                    if (results.isArray() && results.size() > 0) {
                        com.fasterxml.jackson.databind.JsonNode firstResult = results.get(0);
                        String formattedAddress = firstResult.path("formatted_address").asText();
                        com.fasterxml.jackson.databind.JsonNode locationNode = firstResult
                                .path("geometry")
                                .path("location");

                        double lat = locationNode.path("lat").asDouble();
                        double lng = locationNode.path("lng").asDouble();

                        com.tripplanner.dto.Coordinates coords = new com.tripplanner.dto.Coordinates();
                        coords.setLat(lat);
                        coords.setLng(lng);

                        logger.info("✅ [GooglePlacesService] Geocoded '{}' to: '{}' at ({}, {})",
                                location, formattedAddress, lat, lng);
                        return coords;
                    }
                } else if ("ZERO_RESULTS".equals(status)) {
                    logger.warn("⚠️ [GooglePlacesService] No geocoding results found for location: '{}'", location);
                } else {
                    logger.warn("⚠️ [GooglePlacesService] Geocoding API returned status: {} for location: '{}'", status,
                            location);
                }
            }
        } catch (Exception e) {
            logger.error("❌ [GooglePlacesService] Failed to geocode location '{}': {}", location, e.getMessage(), e);
        }

        return null;
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
            throw new RuntimeException(
                    "Daily rate limit exceeded for Google Places API: " + currentCount + "/" + dailyLimit);
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
        return new RateLimitStats(dailyRequestCount.get(), dailyLimit, lastResetTime.get());
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
            logger.error("Google Places API failure threshold reached ({}), opening circuit breaker",
                    FAILURE_THRESHOLD);
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
                circuitBreakerOpenTime.get());
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
     * 
     * @param itineraryId Itinerary ID for caching (optional)
     * @param query Place name to search
     * @param location Location context for search
     * @return First matching place result
     */
    public PlaceSearchResult searchPlace(String itineraryId, String query, String location) {
        logger.info("🔍 [GooglePlacesService] Searching place: query='{}', location='{}'", query, location);

        if (query == null || query.trim().isEmpty()) {
            logger.warn("⚠️ [GooglePlacesService] Empty query provided to searchPlace");
            return null;
        }
        
        // Try cache first if itineraryId provided
        if (toolCacheService != null && itineraryId != null) {
            String cacheKey = ToolCacheKeyGenerator.forSimilarActivities(query, location);
            
            return toolCacheService.getOrCompute(
                itineraryId,
                ToolType.SIMILAR_ACTIVITIES.getValue(),
                cacheKey,
                Map.of("query", query, "location", location != null ? location : ""),
                () -> searchPlaceInternal(query, location),
                PlaceSearchResult.class
            );
        }
        
        // Fallback to direct call
        return searchPlaceInternal(query, location);
    }
    
    /**
     * Overload for backward compatibility (no itineraryId)
     * NOTE: Don't cache null results - they may succeed after validation logic changes
     */
    @Cacheable(value = "placeSearch", key = "#query + '_' + #location", unless = "#result == null")
    public PlaceSearchResult searchPlace(String query, String location) {
        return searchPlace(null, query, location);
    }
    
    /**
     * Internal place search implementation (called by cache or directly)
     */
    private PlaceSearchResult searchPlaceInternal(String query, String location) {
        // Check rate limits and circuit breaker
        checkRateLimit();
        checkCircuitBreaker();

        try {
            // Build search query - ALWAYS include location for better context
            // This ensures we search for "place name + city" which is more specific
            String searchQuery = query;
            if (location != null && !location.trim().isEmpty()) {
                // Check if query already contains the location to avoid duplication
                String queryLower = query.toLowerCase();
                String locationLower = location.toLowerCase();

                if (!queryLower.contains(locationLower)) {
                    searchQuery = query + ", " + location;
                    logger.info("📝 [GooglePlacesService] Combined search query: '{}'", searchQuery);
                } else {
                    logger.info("📝 [GooglePlacesService] Query already contains location, using as-is: '{}'",
                            searchQuery);
                }
            } else {
                logger.warn("⚠️ [GooglePlacesService] No location provided, searching with query only");
            }

            // Geocode destination first - we need this for both API call and validation
            com.tripplanner.dto.Coordinates destCoords = null;
            if (location != null && !location.trim().isEmpty()) {
                try {
                    logger.info("🌍 [GooglePlacesService] Geocoding destination: '{}'", location);
                    destCoords = geocodeLocation(location);
                    if (destCoords == null || destCoords.getLat() == null || destCoords.getLng() == null) {
                        logger.error("❌ [GooglePlacesService] Cannot search without valid destination coordinates");
                        return null; // Fail safely - better than returning wrong results
                    }
                    logger.info("✅ [GooglePlacesService] Destination coordinates: ({}, {})", 
                               destCoords.getLat(), destCoords.getLng());
                } catch (Exception e) {
                    logger.error("❌ [GooglePlacesService] Failed to geocode destination '{}': {}", 
                                location, e.getMessage(), e);
                    return null; // Fail safely
                }
            } else {
                logger.error("❌ [GooglePlacesService] No destination provided for search");
                return null;
            }

            // Build URL with HARD location filter (not soft bias)
            UriComponentsBuilder urlBuilder = UriComponentsBuilder.fromHttpUrl(BASE_URL + "/textsearch/json")
                    .queryParam("query", searchQuery)
                    .queryParam("location", destCoords.getLat() + "," + destCoords.getLng())
                    .queryParam("radius", "20000")  // HARD 20km limit (not soft bias)
                    .queryParam("key", apiKey);
            
            // Add country region bias for additional filtering
            String countryCode = extractCountryCode(location);
            if (countryCode != null) {
                urlBuilder.queryParam("region", countryCode);
                logger.info("🌍 [GooglePlacesService] Added region filter: {}", countryCode);
            }
            
            logger.info("📍 [GooglePlacesService] Using HARD radius filter: 20km from ({}, {})", 
                       destCoords.getLat(), destCoords.getLng());

            String url = urlBuilder.build(false).toUriString();

            logger.debug("Requesting Google Places API: {}", url.replace(apiKey, "***KEY_HIDDEN***"));

            // Make GET request with retry logic
            PlaceSearchResponse response = makeRequestWithRetry(url, PlaceSearchResponse.class);

            if (response != null && response.getStatus() != null &&
                    !response.getStatus().equals("OK") && !response.getStatus().equals("ZERO_RESULTS")) {
                logger.warn("Unexpected Google Places API status: {}", response.getStatus());
            }

            // Increment request count
            incrementRequestCount();

            // Handle API response with validation
            if (response != null && "OK".equals(response.getStatus()) &&
                    response.getResults() != null && !response.getResults().isEmpty()) {

                logger.info("📊 [GooglePlacesService] Found {} results from Google Places API", 
                           response.getResults().size());
                
                // Validate and pick best result (closest to destination within city)
                PlaceSearchResult bestResult = pickBestResult(response.getResults(), destCoords, location);
                
                if (bestResult != null) {
                    recordSuccess();
                    return bestResult;
                } else {
                    logger.warn("⚠️ [GooglePlacesService] No valid results after validation for query: '{}'", searchQuery);
                    
                    // Try fallback with larger radius
                    logger.info("🔄 [GooglePlacesService] Attempting fallback with 50km radius");
                    urlBuilder.queryParam("radius", "50000");  // Expand to 50km
                    PlaceSearchResponse fallbackResponse = makeRequestWithRetry(
                        urlBuilder.build(false).toUriString(), PlaceSearchResponse.class);
                    
                    if (fallbackResponse != null && "OK".equals(fallbackResponse.getStatus()) &&
                        fallbackResponse.getResults() != null && !fallbackResponse.getResults().isEmpty()) {
                        logger.info("📊 [GooglePlacesService] Fallback found {} results", 
                                   fallbackResponse.getResults().size());
                        bestResult = pickBestResult(fallbackResponse.getResults(), destCoords, location);
                        if (bestResult != null) {
                            recordSuccess();
                            return bestResult;
                        }
                    }
                    
                    return null;
                }

            } else if (response != null && "ZERO_RESULTS".equals(response.getStatus())) {
                logger.warn("⚠️ [GooglePlacesService] No results found within 20km for query: '{}'", searchQuery);
                
                // Try fallback with larger radius
                logger.info("🔄 [GooglePlacesService] Attempting fallback with 50km radius");
                urlBuilder.queryParam("radius", "50000");
                PlaceSearchResponse fallbackResponse = makeRequestWithRetry(
                    urlBuilder.build(false).toUriString(), PlaceSearchResponse.class);
                
                if (fallbackResponse != null && "OK".equals(fallbackResponse.getStatus()) &&
                    fallbackResponse.getResults() != null && !fallbackResponse.getResults().isEmpty()) {
                    logger.info("📊 [GooglePlacesService] Fallback found {} results", 
                               fallbackResponse.getResults().size());
                    PlaceSearchResult bestResult = pickBestResult(fallbackResponse.getResults(), destCoords, location);
                    if (bestResult != null) {
                        recordSuccess();
                        return bestResult;
                    }
                }
                
                return null;

            } else {
                String status = response != null ? response.getStatus() : "null";
                String errorMsg = response != null ? response.getErrorMessage() : "null";
                logger.error("Google Places API error - Status: {}, Message: {}", status, errorMsg);
                recordFailure();
                return null;
            }

        } catch (Exception e) {
            logger.error("Failed to search for place {}: {}", query, e.getMessage());
            recordFailure();
            return null; // Return null on error instead of throwing
        }
    }
    
    /**
     * Pick the best result from multiple candidates.
     * Validates distance and city name, picks closest valid result.
     */
    private PlaceSearchResult pickBestResult(List<PlaceSearchResult> results, 
                                            com.tripplanner.dto.Coordinates destCoords,
                                            String expectedLocation) {
        if (results == null || results.isEmpty()) {
            return null;
        }
        
        PlaceSearchResult bestResult = null;
        double minDistance = Double.MAX_VALUE;
        
        logger.info("🔍 [GooglePlacesService] Validating {} results:", results.size());
        
        for (int i = 0; i < Math.min(10, results.size()); i++) {
            PlaceSearchResult result = results.get(i);
            
            // Calculate distance from destination
            double distance = calculateDistance(
                destCoords.getLat(), destCoords.getLng(),
                result.getGeometry().getLocation().getLatitude(),
                result.getGeometry().getLocation().getLongitude()
            );
            
            logger.info("   [{}] '{}' - {}km away - {}", 
                       i + 1, result.getName(), String.format("%.1f", distance), 
                       result.getFormattedAddress());
            
            // Validate: Distance threshold depends on place type
            // Attractions: 60km (can be outside city for day trips)
            // Restaurants/Meals: 30km (should be within reasonable dining distance)
            double maxDistance = 60.0; // Default for attractions
            if (distance > maxDistance) {
                logger.warn("      ❌ Rejected: Too far (>{}km)", maxDistance);
                continue;
            }
            
            // NOTE: City name validation removed - distance validation is sufficient
            // Places in nearby towns/suburbs (e.g., Sundlauenen near Interlaken) are legitimate results
            // if they're within the distance threshold. The strict city name check was rejecting
            // valid tourist attractions that are part of the destination area.
            // 
            // Old validation (too strict):
            // if (!isInCorrectCity(result, expectedLocation)) {
            //     logger.warn("      ❌ Rejected: Wrong city in address");
            //     continue;
            // }
            
            // Pick closest valid result
            if (distance < minDistance) {
                minDistance = distance;
                bestResult = result;
                logger.info("      ✅ Current best candidate");
            }
        }
        
        if (bestResult != null) {
            logger.info("✅ [GooglePlacesService] Selected: '{}' at {}km from destination", 
                       bestResult.getName(), String.format("%.1f", minDistance));
            logger.info("   📍 Address: {}", bestResult.getFormattedAddress());
            logger.info("   🆔 Place ID: {}", bestResult.getPlaceId());
        } else {
            logger.error("❌ [GooglePlacesService] No valid results found after validation");
        }
        
        return bestResult;
    }
    
    /**
     * Calculate distance between two coordinates using Haversine formula.
     * Returns distance in kilometers.
     */
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Radius of the earth in km
        
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return R * c; // Distance in km
    }
    
    /**
     * Check if result is in the correct city by validating address.
     */
    private boolean isInCorrectCity(PlaceSearchResult result, String expectedLocation) {
        if (result.getFormattedAddress() == null || expectedLocation == null) {
            return true; // Can't validate, allow it
        }
        
        String address = result.getFormattedAddress().toLowerCase();
        String expectedCity = extractCityName(expectedLocation).toLowerCase();
        
        // Check if address contains the expected city name
        boolean matches = address.contains(expectedCity);
        
        if (!matches) {
            logger.debug("      City mismatch: expected '{}', address is '{}'", 
                        expectedCity, result.getFormattedAddress());
        }
        
        return matches;
    }
    
    /**
     * Extract city name from location string.
     * "Zurich, Switzerland" → "Zurich"
     * "Lucerne, Canton of Lucerne, Switzerland" → "Lucerne"
     */
    private String extractCityName(String location) {
        if (location == null || location.trim().isEmpty()) {
            return "";
        }
        
        // Split by comma and take first part
        String[] parts = location.split(",");
        return parts[0].trim();
    }
    
    /**
     * Extract country code from location string.
     * "Zurich, Switzerland" → "ch"
     * "Paris, France" → "fr"
     */
    private String extractCountryCode(String location) {
        if (location == null || location.trim().isEmpty()) {
            return null;
        }
        
        String locationLower = location.toLowerCase();
        
        // Map common country names to ISO codes
        if (locationLower.contains("switzerland") || locationLower.contains("schweiz")) {
            return "ch";
        } else if (locationLower.contains("germany") || locationLower.contains("deutschland")) {
            return "de";
        } else if (locationLower.contains("france")) {
            return "fr";
        } else if (locationLower.contains("italy") || locationLower.contains("italia")) {
            return "it";
        } else if (locationLower.contains("spain") || locationLower.contains("españa")) {
            return "es";
        } else if (locationLower.contains("austria") || locationLower.contains("österreich")) {
            return "at";
        } else if (locationLower.contains("netherlands") || locationLower.contains("nederland")) {
            return "nl";
        } else if (locationLower.contains("belgium") || locationLower.contains("belgique")) {
            return "be";
        } else if (locationLower.contains("united kingdom") || locationLower.contains("uk") || locationLower.contains("england")) {
            return "gb";
        } else if (locationLower.contains("united states") || locationLower.contains("usa")) {
            return "us";
        } else if (locationLower.contains("canada")) {
            return "ca";
        } else if (locationLower.contains("japan") || locationLower.contains("日本")) {
            return "jp";
        } else if (locationLower.contains("india") || locationLower.contains("भारत")) {
            return "in";
        } else if (locationLower.contains("china") || locationLower.contains("中国")) {
            return "cn";
        } else if (locationLower.contains("australia")) {
            return "au";
        }
        
        return null; // Unknown country
    }
    
    /**
     * Search for multiple places by query and location with smart caching.
     * Returns exactly 3 suggestions (or less if not available).
     * 
     * CACHING STRATEGY:
     * - Cache key: query + location + coordinates (normalized)
     * - Only cache results that pass distance validation (30km for restaurants, 60km for attractions)
     * - TTL: 7 days (place data doesn't change often)
     * - Reusable: Same query in same location returns cached results
     * 
     * @param itineraryId Itinerary ID for caching context
     * @param query Place type or name (e.g., "museum", "restaurant")
     * @param location Location context (e.g., "Interlaken, Switzerland")
     * @param type Optional place type filter (e.g., "museum", "restaurant")
     * @param maxResults Maximum number of results to return (default: 3, always 3 for chat)
     * @return List of exactly 3 place suggestions (or less if not available)
     */
    public List<PlaceSuggestion> searchPlaces(String itineraryId, String query, String location, String type, int maxResults) {
        logger.info("🔍 [GooglePlacesService] Searching places: query='{}', location='{}', type='{}', maxResults={}",
                query, location, type, maxResults);
        
        // Determine distance threshold based on type
        // Restaurants/meals: 30km (within city)
        // Attractions/activities: 60km (can be day trips outside city)
        double maxDistanceKm = isRestaurantType(type) ? 30.0 : 60.0;
        logger.info("📏 [GooglePlacesService] Using distance threshold: {}km for type '{}'", maxDistanceKm, type);

        if (query == null || query.trim().isEmpty()) {
            logger.warn("⚠️ [GooglePlacesService] Empty query provided to searchPlaces");
            return List.of();
        }

        // Geocode destination first (needed for cache key and validation)
        final com.tripplanner.dto.Coordinates destCoords;
        if (location != null && !location.trim().isEmpty()) {
            destCoords = geocodeLocation(location);
            if (destCoords == null) {
                logger.error("❌ [GooglePlacesService] Cannot search without valid destination coordinates");
                return List.of();
            }
        } else {
            logger.error("❌ [GooglePlacesService] Location is required for place search");
            return List.of();
        }

        // Try cache first if itineraryId and toolCacheService available
        if (toolCacheService != null && itineraryId != null) {
            // Generate cache key: query + location + coordinates (normalized)
            // This ensures same query in same location returns cached results
            String normalizedQuery = query.toLowerCase().trim();
            String normalizedLocation = location.toLowerCase().trim();
            String coordsKey = String.format("%.4f,%.4f", destCoords.getLat(), destCoords.getLng());
            String cacheKey = String.format("place-search:%s:%s:%s:%s", 
                normalizedQuery, normalizedLocation, coordsKey, type != null ? type : "any");
            
            logger.info("📦 [GooglePlacesService] Checking cache with key: {}", cacheKey);
            
            try {
                return toolCacheService.getOrCompute(
                    itineraryId,
                    "place-search",
                    cacheKey,
                    Map.of(
                        "query", query,
                        "location", location,
                        "type", type != null ? type : "",
                        "coordinates", coordsKey
                    ),
                    () -> searchPlacesInternal(query, location, type, maxResults, destCoords, maxDistanceKm),
                    (Class<List<PlaceSuggestion>>) (Class<?>) List.class
                );
            } catch (Exception e) {
                logger.warn("⚠️ [GooglePlacesService] Cache error, falling back to direct call: {}", e.getMessage());
                // Fallback to direct call if cache fails
            }
        }

        // Fallback to direct call (no caching)
        return searchPlacesInternal(query, location, type, maxResults, destCoords, maxDistanceKm);
    }
    
    /**
     * Helper method to determine if a type is restaurant/meal related.
     */
    private boolean isRestaurantType(String type) {
        if (type == null) return false;
        String typeLower = type.toLowerCase();
        return typeLower.contains("restaurant") || 
               typeLower.contains("meal") || 
               typeLower.contains("food") || 
               typeLower.contains("cafe") || 
               typeLower.contains("dining");
    }
    
    /**
     * Internal implementation of place search (called by cache or directly).
     * This method does the actual Google Places API call and validation.
     */
    private List<PlaceSuggestion> searchPlacesInternal(String query, String location, String type, 
                                                        int maxResults, com.tripplanner.dto.Coordinates destCoords,
                                                        double maxDistanceKm) {
        logger.info("🔍 [GooglePlacesService] Executing place search (cache miss or no cache) with {}km threshold", maxDistanceKm);

        // Check rate limits and circuit breaker
        checkRateLimit();
        checkCircuitBreaker();

        try {
            // Build search query
            String searchQuery = query;
            if (location != null && !location.trim().isEmpty()) {
                // Check if query already contains the location to avoid duplication
                String queryLower = query.toLowerCase();
                String locationLower = location.toLowerCase();

                if (!queryLower.contains(locationLower)) {
                    searchQuery = query + " in " + location;
                    logger.info("📝 [GooglePlacesService] Combined search query: '{}'", searchQuery);
                } else {
                    logger.info("📝 [GooglePlacesService] Query already contains location, using as-is: '{}'", searchQuery);
                }
            }

            // Build URL with location filter
            UriComponentsBuilder urlBuilder = UriComponentsBuilder.fromHttpUrl(BASE_URL + "/textsearch/json")
                    .queryParam("query", searchQuery)
                    .queryParam("location", destCoords.getLat() + "," + destCoords.getLng())
                    .queryParam("radius", "20000") // 20km radius
                    .queryParam("key", apiKey);

            // Add type filter if specified
            if (type != null && !type.trim().isEmpty()) {
                urlBuilder.queryParam("type", type);
            }

            // Add country region bias for additional filtering
            String countryCode = extractCountryCode(location);
            if (countryCode != null) {
                urlBuilder.queryParam("region", countryCode);
                logger.info("🌍 [GooglePlacesService] Added region filter: {}", countryCode);
            }

            String url = urlBuilder.build(false).toUriString();

            logger.debug("Requesting Google Places API: {}", url.replace(apiKey, "***KEY_HIDDEN***"));

            // Make GET request with retry logic
            PlaceSearchResponse response = makeRequestWithRetry(url, PlaceSearchResponse.class);

            // Increment request count
            incrementRequestCount();

            // Handle API response
            if (response != null && "OK".equals(response.getStatus()) &&
                    response.getResults() != null && !response.getResults().isEmpty()) {

                logger.info("📊 [GooglePlacesService] Found {} results from Google Places API",
                        response.getResults().size());

                // Validate results against distance filter (only cache valid results)
                List<PlaceSuggestion> validatedResults = new ArrayList<>();
                
                for (PlaceSearchResult result : response.getResults()) {
                    // Calculate distance from destination
                    double distance = calculateDistance(
                        destCoords.getLat(), destCoords.getLng(),
                        result.getGeometry().getLocation().getLatitude(),
                        result.getGeometry().getLocation().getLongitude()
                    );
                    
                    // Validate distance based on type-specific threshold
                    if (distance <= maxDistanceKm) {
                        PlaceSuggestion suggestion = toPlaceSuggestion(result, location, distance);
                        if (suggestion != null) {
                            validatedResults.add(suggestion);
                            logger.info("   ✅ [{}] '{}' - {}km away (threshold: {}km)", 
                                validatedResults.size(), suggestion.getName(), String.format("%.1f", distance), maxDistanceKm);
                        }
                    } else {
                        logger.debug("   ❌ Rejected: '{}' - {}km away (exceeds {}km threshold)", 
                            result.getName(), String.format("%.1f", distance), maxDistanceKm);
                    }
                    
                    // Stop when we have enough results
                    if (validatedResults.size() >= maxResults) {
                        break;
                    }
                }

                logger.info("✅ [GooglePlacesService] Returning {} validated place suggestions (out of {} total results)", 
                    validatedResults.size(), response.getResults().size());
                recordSuccess();
                
                // IMPORTANT: Always return exactly 3 suggestions for chat UI
                // If we have less, that's fine - UI will handle it
                return validatedResults;

            } else if (response != null && "ZERO_RESULTS".equals(response.getStatus())) {
                logger.warn("⚠️ [GooglePlacesService] No results found for query: '{}'", searchQuery);
                return List.of();
            } else {
                String status = response != null ? response.getStatus() : "null";
                logger.error("Google Places API error - Status: {}", status);
                recordFailure();
                return List.of();
            }

        } catch (Exception e) {
            logger.error("Failed to search places for query '{}': {}", query, e.getMessage(), e);
            recordFailure();
            return List.of();
        }
    }
    
    /**
     * Convert PlaceSearchResult to PlaceSuggestion with enriched data.
     * 
     * @param result The place search result from Google API
     * @param location The search location context
     * @param distance Distance from destination in km
     */
    private PlaceSuggestion toPlaceSuggestion(PlaceSearchResult result, String location, double distance) {
        try {
            PlaceSuggestion suggestion = PlaceSuggestion.builder()
                    .placeId(result.getPlaceId())
                    .name(result.getName())
                    .address(result.getFormattedAddress())
                    .rating(result.getRating())
                    .userRatingsTotal(result.getUserRatingsTotal())
                    .priceLevel(result.getPriceLevel())
                    .types(result.getTypes())
                    .geometry(result.getGeometry())
                    .build();

            // Add distance from destination
            suggestion.setDistanceKm(distance);
            
            // Add Google Maps URL
            suggestion.setGoogleMapsUrl("https://www.google.com/maps/place/?q=place_id:" + result.getPlaceId());

            // Get detailed information (photos, opening hours, website)
            try {
                PlaceDetails details = getPlaceDetails(result.getPlaceId());
                if (details != null) {
                    // Limit photos to 5 for performance
                    if (details.getPhotos() != null && !details.getPhotos().isEmpty()) {
                        List<Photo> limitedPhotos = details.getPhotos().stream()
                                .limit(5)
                                .collect(java.util.stream.Collectors.toList());
                        suggestion.setPhotos(limitedPhotos);
                    }
                    
                    // Format opening hours
                    if (details.getOpeningHours() != null) {
                        suggestion.setOpeningHours(formatOpeningHours(details.getOpeningHours()));
                    }
                    
                    // Add website if available
                    if (details.getWebsite() != null && !details.getWebsite().isEmpty()) {
                        suggestion.setWebsite(details.getWebsite());
                    }
                }
            } catch (Exception e) {
                logger.warn("Failed to get details for place {}: {}", result.getPlaceId(), e.getMessage());
                // Continue without details
            }

            // Estimate cost based on price level
            if (result.getPriceLevel() != null) {
                suggestion.setEstimatedCost(estimateCostFromPriceLevel(result.getPriceLevel()));
            }

            // Estimate duration based on place type
            suggestion.setEstimatedDuration(estimateDuration(result.getTypes()));

            return suggestion;

        } catch (Exception e) {
            logger.error("Failed to convert place result to suggestion: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Format opening hours for display.
     */
    private String formatOpeningHours(PlaceDetails.OpeningHours openingHours) {
        if (openingHours == null) {
            return null;
        }
        
        // If we have weekday text, use the current day
        if (openingHours.getWeekdayText() != null && !openingHours.getWeekdayText().isEmpty()) {
            int today = java.time.LocalDate.now().getDayOfWeek().getValue() % 7; // 0=Sunday
            if (today < openingHours.getWeekdayText().size()) {
                return openingHours.getWeekdayText().get(today);
            }
        }
        
        // Fallback to open now status
        Boolean openNow = openingHours.getOpenNow();
        if (openNow != null) {
            return openNow ? "Open now" : "Closed now";
        }
        
        return null;
    }
    
    /**
     * Estimate cost per person based on Google's price level (0-4).
     */
    private Double estimateCostFromPriceLevel(Integer priceLevel) {
        if (priceLevel == null) {
            return null;
        }
        
        // Rough estimates in USD
        switch (priceLevel) {
            case 0: return 0.0;      // Free
            case 1: return 10.0;     // $
            case 2: return 25.0;     // $$
            case 3: return 50.0;     // $$$
            case 4: return 100.0;    // $$$$
            default: return null;
        }
    }
    
    /**
     * Estimate duration based on place types.
     */
    private Integer estimateDuration(List<String> types) {
        if (types == null || types.isEmpty()) {
            return 120; // Default 2 hours
        }
        
        // Check for specific types
        for (String type : types) {
            if (type.contains("museum")) return 180;        // 3 hours
            if (type.contains("park")) return 120;          // 2 hours
            if (type.contains("restaurant")) return 90;     // 1.5 hours
            if (type.contains("cafe")) return 60;           // 1 hour
            if (type.contains("bar")) return 120;           // 2 hours
            if (type.contains("shopping")) return 120;      // 2 hours
            if (type.contains("church")) return 60;         // 1 hour
            if (type.contains("tourist_attraction")) return 120; // 2 hours
        }
        
        return 120; // Default 2 hours
    }
}
