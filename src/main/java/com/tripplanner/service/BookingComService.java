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
import java.util.stream.Collectors;

/**
 * Service for integrating with Booking.com API for hotel bookings.
 * Provides hotel search and booking confirmation functionality.
 */
@Service
public class BookingComService {
    
    private static final Logger logger = LoggerFactory.getLogger(BookingComService.class);
    private static final String BASE_URL = "https://distribution-xml.booking.com/2.5/json";
    
    @Value("${booking.com.api.key:}")
    private String apiKey;
    
    @Value("${booking.com.partner.id:}")
    private String partnerId;
    
    @Value("${booking.com.timeout:30000}")
    private int timeoutMs;
    
    private final RestTemplate restTemplate;
    
    public BookingComService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    
    /**
     * Search for hotels based on booking request parameters.
     * Returns a list of hotels ranked by reviews, ratings, and price.
     */
    public HotelSearchResponse searchHotels(BookingRequest request) {
        logger.info("Searching hotels for location: {}, dates: {} to {}", 
                   request.getLocation(), request.getCheckInDate(), request.getCheckOutDate());
        
        if (request == null) {
            throw new IllegalArgumentException("Booking request cannot be null");
        }
        
        validateHotelSearchRequest(request);
        
        try {
            // Build search parameters
            Map<String, Object> searchParams = buildHotelSearchParams(request);
            
            // Make POST request to hotels endpoint
            String url = BASE_URL + "/hotels";
            HttpHeaders headers = createHeaders();
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(searchParams, headers);
            
            ResponseEntity<HotelSearchResponse> response = restTemplate.exchange(
                url, HttpMethod.POST, entity, HotelSearchResponse.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                HotelSearchResponse searchResponse = response.getBody();
                
                if (searchResponse.isSuccessful() && searchResponse.hasHotels()) {
                    // Rank hotels by reviews, ratings, and price
                    List<Hotel> rankedHotels = rankHotels(searchResponse.getHotels());
                    searchResponse.setHotels(rankedHotels);
                    
                    logger.info("Found {} hotels for location: {}", rankedHotels.size(), request.getLocation());
                    return searchResponse;
                } else {
                    logger.warn("Hotel search returned no results for location: {}", request.getLocation());
                    return searchResponse;
                }
            } else {
                throw new RuntimeException("Invalid response from Booking.com API: " + response.getStatusCode());
            }
            
        } catch (HttpClientErrorException e) {
            logger.error("Client error searching hotels: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Hotel search failed: " + e.getMessage(), e);
        } catch (HttpServerErrorException e) {
            logger.error("Server error searching hotels: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Booking.com service unavailable: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error searching hotels", e);
            throw new RuntimeException("Hotel search failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Confirm a hotel booking with payment details.
     * Returns booking confirmation with reference number.
     */
    public BookingConfirmation confirmBooking(Hotel hotel, PaymentResult payment) {
        logger.info("Confirming booking for hotel: {} with payment: {}", hotel.getName(), payment.getPaymentId());
        
        if (hotel == null) {
            throw new IllegalArgumentException("Hotel cannot be null");
        }
        if (payment == null) {
            throw new IllegalArgumentException("Payment result cannot be null");
        }
        
        validatePaymentResult(payment);
        
        try {
            // Build booking parameters
            Map<String, Object> bookingParams = buildBookingParams(hotel, payment);
            
            // Make POST request to bookings endpoint
            String url = BASE_URL + "/bookings";
            HttpHeaders headers = createHeaders();
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(bookingParams, headers);
            
            ResponseEntity<BookingConfirmation> response = restTemplate.exchange(
                url, HttpMethod.POST, entity, BookingConfirmation.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                BookingConfirmation confirmation = response.getBody();
                
                if (confirmation.isSuccessful()) {
                    logger.info("Booking confirmed successfully: {}", confirmation.getBookingReference());
                    return confirmation;
                } else if (confirmation.isFailed()) {
                    throw new RuntimeException("Booking failed: " + confirmation.getErrorMessage());
                } else {
                    logger.warn("Booking is pending: {}", confirmation.getBookingReference());
                    return confirmation;
                }
            } else {
                throw new RuntimeException("Invalid response from Booking.com API: " + response.getStatusCode());
            }
            
        } catch (HttpClientErrorException e) {
            logger.error("Client error confirming booking: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Booking confirmation failed: " + e.getMessage(), e);
        } catch (HttpServerErrorException e) {
            logger.error("Server error confirming booking: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Booking.com service unavailable: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error confirming booking", e);
            throw new RuntimeException("Booking confirmation failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Rank hotels by reviews, ratings, and price.
     * Uses a weighted scoring algorithm to prioritize the best options.
     */
    public List<Hotel> rankHotels(List<Hotel> hotels) {
        if (hotels == null || hotels.isEmpty()) {
            return new ArrayList<>();
        }
        
        logger.debug("Ranking {} hotels by reviews, ratings, and price", hotels.size());
        
        return hotels.stream()
            .sorted((h1, h2) -> {
                double score1 = calculateHotelScore(h1);
                double score2 = calculateHotelScore(h2);
                return Double.compare(score2, score1); // Higher score first
            })
            .collect(Collectors.toList());
    }
    
    /**
     * Calculate a weighted score for hotel ranking.
     */
    private double calculateHotelScore(Hotel hotel) {
        double score = 0.0;
        
        // Rating weight: 40%
        if (hotel.getRating() != null) {
            score += (hotel.getRating() / 5.0) * 0.4;
        }
        
        // Review count weight: 20%
        if (hotel.getReviewCount() != null) {
            // Normalize review count (log scale to prevent outliers)
            double normalizedReviews = Math.min(Math.log(hotel.getReviewCount() + 1) / Math.log(1000), 1.0);
            score += normalizedReviews * 0.2;
        }
        
        // Star rating weight: 20%
        if (hotel.getStarRating() != null) {
            score += (hotel.getStarRating() / 5.0) * 0.2;
        }
        
        // Price weight: 20% (inverse - lower price is better)
        if (hotel.getPrice() != null && hotel.getPrice() > 0) {
            // Normalize price (assuming max reasonable price of 1000)
            double normalizedPrice = Math.min(hotel.getPrice() / 1000.0, 1.0);
            score += (1.0 - normalizedPrice) * 0.2;
        }
        
        return score;
    }
    
    /**
     * Build search parameters for hotel search API call.
     */
    private Map<String, Object> buildHotelSearchParams(BookingRequest request) {
        Map<String, Object> params = new HashMap<>();
        
        params.put("location", request.getLocation());
        params.put("checkin", request.getCheckInDate().format(DateTimeFormatter.ISO_LOCAL_DATE));
        params.put("checkout", request.getCheckOutDate().format(DateTimeFormatter.ISO_LOCAL_DATE));
        params.put("guests", request.getGuests());
        params.put("rooms", request.getRooms());
        params.put("currency", request.getCurrency());
        
        // Add optional parameters
        if (request.getBudget() != null) {
            params.put("max_price", request.getBudget());
        }
        
        // Add preferences
        if (request.getPreferences() != null) {
            for (Map.Entry<String, Object> pref : request.getPreferences().entrySet()) {
                params.put(pref.getKey(), pref.getValue());
            }
        }
        
        // API specific parameters
        params.put("partner_id", partnerId);
        params.put("limit", 50); // Limit results to top 50
        params.put("sort", "popularity"); // Sort by popularity initially
        
        return params;
    }
    
    /**
     * Build booking parameters for booking confirmation API call.
     */
    private Map<String, Object> buildBookingParams(Hotel hotel, PaymentResult payment) {
        Map<String, Object> params = new HashMap<>();
        
        params.put("hotel_id", hotel.getHotelId());
        params.put("payment_id", payment.getPaymentId());
        params.put("amount", payment.getAmount());
        params.put("currency", payment.getCurrency());
        
        // Guest information (would typically come from user profile)
        Map<String, Object> guestInfo = new HashMap<>();
        guestInfo.put("name", "Guest Name"); // This should come from user data
        guestInfo.put("email", "guest@example.com"); // This should come from user data
        guestInfo.put("phone", "+1234567890"); // This should come from user data
        params.put("guest_info", guestInfo);
        
        // Booking details
        params.put("partner_id", partnerId);
        params.put("booking_reference", generateBookingReference());
        
        return params;
    }
    
    /**
     * Create HTTP headers for API requests.
     */
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);
        headers.set("User-Agent", "TripPlanner/1.0");
        return headers;
    }
    
    /**
     * Validate hotel search request parameters.
     */
    private void validateHotelSearchRequest(BookingRequest request) {
        if (request.getLocation() == null || request.getLocation().trim().isEmpty()) {
            throw new IllegalArgumentException("Location is required for hotel search");
        }
        
        if (request.getCheckInDate() == null) {
            throw new IllegalArgumentException("Check-in date is required");
        }
        
        if (request.getCheckOutDate() == null) {
            throw new IllegalArgumentException("Check-out date is required");
        }
        
        if (request.getCheckInDate().isAfter(request.getCheckOutDate())) {
            throw new IllegalArgumentException("Check-in date must be before check-out date");
        }
        
        if (request.getGuests() == null || request.getGuests() < 1) {
            throw new IllegalArgumentException("Number of guests must be at least 1");
        }
        
        if (request.getRooms() == null || request.getRooms() < 1) {
            throw new IllegalArgumentException("Number of rooms must be at least 1");
        }
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
     * Generate a unique booking reference.
     */
    private String generateBookingReference() {
        return "BK" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}