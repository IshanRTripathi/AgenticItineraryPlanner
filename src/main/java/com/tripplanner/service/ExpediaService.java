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

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Service for integrating with Expedia API for flights and activities.
 * Provides flight search and activity search functionality with rate limiting.
 */
@Service
public class ExpediaService {
    
    private static final Logger logger = LoggerFactory.getLogger(ExpediaService.class);
    private static final String BASE_URL = "https://rapidapi.com/apidojo/api/expedia";
    
    // Rate limiting tracking (1000 requests/day)
    private final AtomicInteger dailyRequestCount = new AtomicInteger(0);
    private final AtomicLong lastResetTime = new AtomicLong(System.currentTimeMillis());
    private static final int DAILY_LIMIT = 1000;
    
    @Value("${expedia.api.key:}")
    private String apiKey;
    
    @Value("${expedia.rate.limit.enabled:true}")
    private boolean rateLimitEnabled;
    
    @Value("${expedia.timeout:30000}")
    private int timeoutMs;
    
    private final RestTemplate restTemplate;
    
    public ExpediaService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    
    /**
     * Search for flights based on booking request parameters.
     * Returns a list of flights sorted by price and duration.
     */
    public FlightSearchResponse searchFlights(BookingRequest request) {
        logger.info("Searching flights from {} to {} on {}", 
                   request.getOrigin(), request.getDestination(), request.getDepartureDate());
        
        if (request == null) {
            throw new IllegalArgumentException("Booking request cannot be null");
        }
        
        validateFlightSearchRequest(request);
        checkRateLimit();
        
        try {
            // Build flight search parameters
            Map<String, String> searchParams = buildFlightSearchParams(request);
            
            // Make GET request to flights endpoint
            String url = BASE_URL + "/flights/search";
            HttpHeaders headers = createHeaders();
            
            // Add query parameters to URL
            StringBuilder urlBuilder = new StringBuilder(url);
            urlBuilder.append("?");
            for (Map.Entry<String, String> param : searchParams.entrySet()) {
                urlBuilder.append(param.getKey()).append("=").append(param.getValue()).append("&");
            }
            String finalUrl = urlBuilder.toString().replaceAll("&$", "");
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<FlightSearchResponse> response = restTemplate.exchange(
                finalUrl, HttpMethod.GET, entity, FlightSearchResponse.class);
            
            incrementRequestCount();
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                FlightSearchResponse searchResponse = response.getBody();
                
                if (searchResponse.isSuccessful() && searchResponse.hasFlights()) {
                    // Sort flights by price and duration
                    List<Flight> sortedFlights = sortFlightsByPriceAndDuration(searchResponse.getFlights());
                    searchResponse.setFlights(sortedFlights);
                    
                    logger.info("Found {} flights from {} to {}", 
                               sortedFlights.size(), request.getOrigin(), request.getDestination());
                    return searchResponse;
                } else {
                    logger.warn("Flight search returned no results for {} to {}", 
                               request.getOrigin(), request.getDestination());
                    return searchResponse;
                }
            } else {
                throw new RuntimeException("Invalid response from Expedia API: " + response.getStatusCode());
            }
            
        } catch (HttpClientErrorException e) {
            logger.error("Client error searching flights: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Flight search failed: " + e.getMessage(), e);
        } catch (HttpServerErrorException e) {
            logger.error("Server error searching flights: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Expedia service unavailable: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error searching flights", e);
            throw new RuntimeException("Flight search failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Search for activities based on booking request parameters.
     * Returns a list of activities sorted by rating and price.
     */
    public ActivitySearchResponse searchActivities(BookingRequest request) {
        logger.info("Searching activities in {} for type: {}", request.getLocation(), request.getActivityType());
        
        if (request == null) {
            throw new IllegalArgumentException("Booking request cannot be null");
        }
        
        validateActivitySearchRequest(request);
        checkRateLimit();
        
        try {
            // Build activity search parameters
            Map<String, Object> searchParams = buildActivitySearchParams(request);
            
            // Make GET request to activities endpoint
            String url = BASE_URL + "/activities/search";
            HttpHeaders headers = createHeaders();
            
            // Add query parameters to URL
            StringBuilder urlBuilder = new StringBuilder(url);
            urlBuilder.append("?");
            for (Map.Entry<String, Object> param : searchParams.entrySet()) {
                urlBuilder.append(param.getKey()).append("=").append(param.getValue()).append("&");
            }
            String finalUrl = urlBuilder.toString().replaceAll("&$", "");
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<ActivitySearchResponse> response = restTemplate.exchange(
                finalUrl, HttpMethod.GET, entity, ActivitySearchResponse.class);
            
            incrementRequestCount();
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                ActivitySearchResponse searchResponse = response.getBody();
                
                if (searchResponse.isSuccessful() && searchResponse.hasActivities()) {
                    // Sort activities by rating and price
                    List<Activity> sortedActivities = sortActivitiesByRatingAndPrice(searchResponse.getActivities());
                    searchResponse.setActivities(sortedActivities);
                    
                    logger.info("Found {} activities in {} for type: {}", 
                               sortedActivities.size(), request.getLocation(), request.getActivityType());
                    return searchResponse;
                } else {
                    logger.warn("Activity search returned no results for {} type: {}", 
                               request.getLocation(), request.getActivityType());
                    return searchResponse;
                }
            } else {
                throw new RuntimeException("Invalid response from Expedia API: " + response.getStatusCode());
            }
            
        } catch (HttpClientErrorException e) {
            logger.error("Client error searching activities: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Activity search failed: " + e.getMessage(), e);
        } catch (HttpServerErrorException e) {
            logger.error("Server error searching activities: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Expedia service unavailable: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error searching activities", e);
            throw new RuntimeException("Activity search failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Sort flights by price and duration (price priority, then duration).
     */
    private List<Flight> sortFlightsByPriceAndDuration(List<Flight> flights) {
        if (flights == null || flights.isEmpty()) {
            return new ArrayList<>();
        }
        
        return flights.stream()
            .sorted((f1, f2) -> {
                // Primary sort: price (ascending)
                if (f1.getPrice() != null && f2.getPrice() != null) {
                    int priceComparison = Double.compare(f1.getPrice(), f2.getPrice());
                    if (priceComparison != 0) {
                        return priceComparison;
                    }
                }
                
                // Secondary sort: duration (ascending)
                if (f1.getDuration() != null && f2.getDuration() != null) {
                    return Integer.compare(f1.getDuration(), f2.getDuration());
                }
                
                // Tertiary sort: direct flights first
                if (f1.isDirect() && !f2.isDirect()) {
                    return -1;
                } else if (!f1.isDirect() && f2.isDirect()) {
                    return 1;
                }
                
                return 0;
            })
            .collect(Collectors.toList());
    }
    
    /**
     * Sort activities by rating and price (rating priority, then price).
     */
    private List<Activity> sortActivitiesByRatingAndPrice(List<Activity> activities) {
        if (activities == null || activities.isEmpty()) {
            return new ArrayList<>();
        }
        
        return activities.stream()
            .sorted((a1, a2) -> {
                // Primary sort: rating (descending)
                if (a1.getRating() != null && a2.getRating() != null) {
                    int ratingComparison = Double.compare(a2.getRating(), a1.getRating());
                    if (ratingComparison != 0) {
                        return ratingComparison;
                    }
                }
                
                // Secondary sort: price (ascending)
                if (a1.getPrice() != null && a2.getPrice() != null) {
                    return Double.compare(a1.getPrice(), a2.getPrice());
                }
                
                return 0;
            })
            .collect(Collectors.toList());
    }
    
    /**
     * Build flight search parameters for API call.
     */
    private Map<String, String> buildFlightSearchParams(BookingRequest request) {
        Map<String, String> params = new HashMap<>();
        
        params.put("origin", request.getOrigin());
        params.put("destination", request.getDestination());
        params.put("departure_date", request.getDepartureDate().format(DateTimeFormatter.ISO_LOCAL_DATE));
        
        if (request.getReturnDate() != null) {
            params.put("return_date", request.getReturnDate().format(DateTimeFormatter.ISO_LOCAL_DATE));
        }
        
        params.put("passengers", String.valueOf(request.getPassengers()));
        params.put("currency", request.getCurrency());
        
        // Add cabin class preference if specified
        if (request.getPreferences() != null && request.getPreferences().containsKey("cabinClass")) {
            params.put("cabin_class", String.valueOf(request.getPreferences().get("cabinClass")));
        } else {
            params.put("cabin_class", "economy");
        }
        
        // Add budget constraint if specified
        if (request.getBudget() != null) {
            params.put("max_price", String.valueOf(request.getBudget()));
        }
        
        params.put("limit", "50"); // Limit results
        
        return params;
    }
    
    /**
     * Build activity search parameters for API call.
     */
    private Map<String, Object> buildActivitySearchParams(BookingRequest request) {
        Map<String, Object> params = new HashMap<>();
        
        params.put("location", request.getLocation());
        params.put("currency", request.getCurrency());
        
        if (request.getActivityType() != null) {
            params.put("activity_type", request.getActivityType());
        }
        
        if (request.getInterests() != null) {
            params.put("interests", request.getInterests());
        }
        
        if (request.getCheckInDate() != null) {
            params.put("start_date", request.getCheckInDate().format(DateTimeFormatter.ISO_LOCAL_DATE));
        }
        
        if (request.getCheckOutDate() != null) {
            params.put("end_date", request.getCheckOutDate().format(DateTimeFormatter.ISO_LOCAL_DATE));
        }
        
        // Add budget constraint if specified
        if (request.getBudget() != null) {
            params.put("max_price", String.valueOf(request.getBudget()));
        }
        
        params.put("limit", "50"); // Limit results
        
        return params;
    }
    
    /**
     * Create HTTP headers for API requests with RapidAPI authentication.
     */
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-RapidAPI-Key", apiKey);
        headers.set("X-RapidAPI-Host", "expedia.p.rapidapi.com");
        headers.set("User-Agent", "TripPlanner/1.0");
        return headers;
    }
    
    /**
     * Validate flight search request parameters.
     */
    private void validateFlightSearchRequest(BookingRequest request) {
        if (request.getOrigin() == null || request.getOrigin().trim().isEmpty()) {
            throw new IllegalArgumentException("Origin is required for flight search");
        }
        
        if (request.getDestination() == null || request.getDestination().trim().isEmpty()) {
            throw new IllegalArgumentException("Destination is required for flight search");
        }
        
        if (request.getDepartureDate() == null) {
            throw new IllegalArgumentException("Departure date is required for flight search");
        }
        
        if (request.getPassengers() == null || request.getPassengers() < 1) {
            throw new IllegalArgumentException("Number of passengers must be at least 1");
        }
        
        if (request.getPassengers() > 9) {
            throw new IllegalArgumentException("Number of passengers cannot exceed 9");
        }
    }
    

    
    /**
     * Confirm a flight booking with payment details.
     * Returns booking confirmation with reference number.
     */
    public BookingConfirmation confirmFlightBooking(Flight flight, PaymentResult payment) {
        logger.info("Confirming flight booking for {} with payment: {}", 
                   flight.getFlightNumber(), payment.getPaymentId());
        
        if (flight == null) {
            throw new IllegalArgumentException("Flight cannot be null");
        }
        if (payment == null) {
            throw new IllegalArgumentException("Payment result cannot be null");
        }
        
        validatePaymentResult(payment);
        checkRateLimit();
        
        try {
            // Build booking parameters
            Map<String, Object> bookingParams = buildFlightBookingParams(flight, payment);
            
            // Make POST request to bookings endpoint
            String url = BASE_URL + "/flights/bookings";
            HttpHeaders headers = createHeaders();
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(bookingParams, headers);
            
            ResponseEntity<BookingConfirmation> response = restTemplate.exchange(
                url, HttpMethod.POST, entity, BookingConfirmation.class);
            
            incrementRequestCount();
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                BookingConfirmation confirmation = response.getBody();
                
                if (confirmation.isSuccessful()) {
                    logger.info("Flight booking confirmed successfully: {}", confirmation.getBookingReference());
                    return confirmation;
                } else if (confirmation.isFailed()) {
                    throw new RuntimeException("Flight booking failed: " + confirmation.getErrorMessage());
                } else {
                    logger.warn("Flight booking is pending: {}", confirmation.getBookingReference());
                    return confirmation;
                }
            } else {
                throw new RuntimeException("Invalid response from Expedia API: " + response.getStatusCode());
            }
            
        } catch (HttpClientErrorException e) {
            logger.error("Client error confirming flight booking: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Flight booking confirmation failed: " + e.getMessage(), e);
        } catch (HttpServerErrorException e) {
            logger.error("Server error confirming flight booking: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Expedia service unavailable: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error confirming flight booking", e);
            throw new RuntimeException("Flight booking confirmation failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Build booking parameters for flight booking confirmation API call.
     */
    private Map<String, Object> buildFlightBookingParams(Flight flight, PaymentResult payment) {
        Map<String, Object> params = new HashMap<>();
        
        params.put("flightId", flight.getFlightId());
        params.put("paymentId", payment.getPaymentId());
        params.put("amount", payment.getAmount());
        params.put("currency", payment.getCurrency());
        
        // Passenger information (would typically come from user profile)
        List<Map<String, Object>> passengers = new ArrayList<>();
        Map<String, Object> passenger = new HashMap<>();
        passenger.put("firstName", "John"); // This should come from user data
        passenger.put("lastName", "Doe"); // This should come from user data
        passenger.put("email", "john.doe@example.com"); // This should come from user data
        passenger.put("phone", "+1234567890"); // This should come from user data
        passengers.add(passenger);
        params.put("passengers", passengers);
        
        // Booking details
        params.put("bookingReference", generateFlightBookingReference());
        
        return params;
    }
    
    /**
     * Validate payment result before booking confirmation.
     */
    private void validatePaymentResult(PaymentResult payment) {
        if (payment.getPaymentId() == null || payment.getPaymentId().trim().isEmpty()) {
            throw new IllegalArgumentException("Payment ID is required");
        }
        
        if (!"SUCCESS".equals(payment.getStatus())) {
            throw new IllegalArgumentException("Payment must be successful before booking confirmation");
        }
        
        if (payment.getAmount() == null || payment.getAmount() <= 0) {
            throw new IllegalArgumentException("Payment amountPerPerson must be greater than 0");
        }
    }
    
    /**
     * Generate a unique flight booking reference.
     */
    private String generateFlightBookingReference() {
        return "FL" + System.currentTimeMillis() + "_" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
    
    /**
     * Validate activity search request parameters.
     */
    private void validateActivitySearchRequest(BookingRequest request) {
        if (request.getLocation() == null || request.getLocation().trim().isEmpty()) {
            throw new IllegalArgumentException("Location is required for activity search");
        }
        
        if (request.getDepartureDate() == null) {
            throw new IllegalArgumentException("Date is required for activity search");
        }
        
        if (request.getDepartureDate().isBefore(java.time.LocalDate.now())) {
            throw new IllegalArgumentException("Activity date cannot be in the past");
        }
        
        if (request.getPassengers() == null || request.getPassengers() < 1) {
            throw new IllegalArgumentException("Number of participants must be at least 1");
        }
        
        if (request.getPassengers() > 20) {
            throw new IllegalArgumentException("Number of participants cannot exceed 20");
        }
    }
    
    /**
     * Check rate limits before making API requests.
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
                logger.info("Reset daily request count for Expedia API");
            }
        }
        
        // Check if we're approaching the limit
        int currentCount = dailyRequestCount.get();
        if (currentCount >= DAILY_LIMIT) {
            throw new RuntimeException("Daily rate limit exceeded for Expedia API: " + currentCount + "/" + DAILY_LIMIT);
        }
        
        // Warn when approaching limit
        if (currentCount > DAILY_LIMIT * 0.9) {
            logger.warn("Approaching Expedia API daily limit: {}/{}", currentCount, DAILY_LIMIT);
        }
    }
    
    /**
     * Increment the daily request count.
     */
    private void incrementRequestCount() {
        int newCount = dailyRequestCount.incrementAndGet();
        logger.debug("Expedia API request count: {}/{}", newCount, DAILY_LIMIT);
    }
    
    /**
     * Get current rate limit statistics.
     */
    public RateLimitStats getRateLimitStats() {
        return new RateLimitStats(
            dailyRequestCount.get(),
            DAILY_LIMIT,
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
}