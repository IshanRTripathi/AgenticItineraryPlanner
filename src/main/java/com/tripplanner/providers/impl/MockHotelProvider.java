package com.tripplanner.providers.impl;

import com.tripplanner.providers.HotelSearchProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Mock implementation of HotelSearchProvider for testing and development.
 */
@Component
public class MockHotelProvider implements HotelSearchProvider {
    
    private static final Logger logger = LoggerFactory.getLogger(MockHotelProvider.class);
    
    @Override
    public List<HotelOption> search(HotelQuery query) {
        logger.info("Searching hotels for location: {}, {} from {} to {}", 
                   query.getLat(), query.getLng(), query.getCheckIn(), query.getCheckOut());
        
        // Simulate API delay
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Return mock hotel options
        HotelOption hotel1 = new HotelOption();
        hotel1.setProvider("mock-booking");
        hotel1.setName("Grand City Hotel");
        hotel1.setAddress("123 Main Street, City Center");
        hotel1.setLat(query.getLat() + 0.001);
        hotel1.setLng(query.getLng() + 0.001);
        hotel1.setCheckIn(query.getCheckIn());
        hotel1.setCheckOut(query.getCheckOut());
        hotel1.setPrice(getPriceForBudgetTier(query.getBudgetTier(), 150.0));
        hotel1.setCurrency("USD");
        hotel1.setToken(UUID.randomUUID().toString());
        hotel1.setRating(4.2);
        hotel1.setReviewCount(1247);
        hotel1.setAmenities(Arrays.asList("WiFi", "Pool", "Gym", "Restaurant", "Spa"));
        hotel1.setImageUrl("https://example.com/hotel1.jpg");
        hotel1.setDescription("Luxury hotel in the heart of the city with modern amenities");
        
        HotelOption hotel2 = new HotelOption();
        hotel2.setProvider("mock-booking");
        hotel2.setName("Boutique Garden Inn");
        hotel2.setAddress("456 Garden Avenue, Historic District");
        hotel2.setLat(query.getLat() + 0.002);
        hotel2.setLng(query.getLng() - 0.001);
        hotel2.setCheckIn(query.getCheckIn());
        hotel2.setCheckOut(query.getCheckOut());
        hotel2.setPrice(getPriceForBudgetTier(query.getBudgetTier(), 120.0));
        hotel2.setCurrency("USD");
        hotel2.setToken(UUID.randomUUID().toString());
        hotel2.setRating(4.5);
        hotel2.setReviewCount(856);
        hotel2.setAmenities(Arrays.asList("WiFi", "Garden", "Restaurant", "Concierge"));
        hotel2.setImageUrl("https://example.com/hotel2.jpg");
        hotel2.setDescription("Charming boutique hotel with beautiful garden views");
        
        HotelOption hotel3 = new HotelOption();
        hotel3.setProvider("mock-booking");
        hotel3.setName("Budget Comfort Lodge");
        hotel3.setAddress("789 Budget Street, Business District");
        hotel3.setLat(query.getLat() - 0.001);
        hotel3.setLng(query.getLng() + 0.002);
        hotel3.setCheckIn(query.getCheckIn());
        hotel3.setCheckOut(query.getCheckOut());
        hotel3.setPrice(getPriceForBudgetTier(query.getBudgetTier(), 80.0));
        hotel3.setCurrency("USD");
        hotel3.setToken(UUID.randomUUID().toString());
        hotel3.setRating(3.8);
        hotel3.setReviewCount(432);
        hotel3.setAmenities(Arrays.asList("WiFi", "24h Reception", "Breakfast"));
        hotel3.setImageUrl("https://example.com/hotel3.jpg");
        hotel3.setDescription("Comfortable and affordable accommodation with essential amenities");
        
        logger.info("Found {} hotel options", 3);
        return Arrays.asList(hotel1, hotel2, hotel3);
    }
    
    @Override
    public BookingResult book(BookingRequest request) {
        logger.info("Booking hotel with token: {}", request.getToken());
        
        // Simulate booking process
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Simulate 90% success rate
        boolean success = Math.random() < 0.9;
        
        BookingResult result = new BookingResult();
        result.setBookingId(UUID.randomUUID().toString());
        result.setStatus(success ? "confirmed" : "failed");
        result.setProviderConfirmationId("MOCK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        result.setCancellationPolicy("Free cancellation up to 24 hours before check-in");
        result.setContactInfo(Map.of(
            "phone", "+1-555-HOTEL",
            "email", "reservations@mockhotel.com"
        ));
        result.setVoucher("Present this confirmation at check-in");
        result.setAdditionalInfo(Map.of(
            "checkInTime", "15:00",
            "checkOutTime", "11:00",
            "specialInstructions", "Please bring valid ID for check-in"
        ));
        
        logger.info("Hotel booking {}: {}", success ? "successful" : "failed", result.getBookingId());
        return result;
    }
    
    @Override
    public String getProviderName() {
        return "Mock Hotel Provider";
    }
    
    @Override
    public boolean isAvailable() {
        return true;
    }
    
    /**
     * Adjust price based on budget tier.
     */
    private double getPriceForBudgetTier(String budgetTier, double basePrice) {
        return switch (budgetTier != null ? budgetTier.toLowerCase() : "mid-range") {
            case "economy" -> basePrice * 0.6;
            case "luxury" -> basePrice * 1.8;
            default -> basePrice; // mid-range
        };
    }
}
