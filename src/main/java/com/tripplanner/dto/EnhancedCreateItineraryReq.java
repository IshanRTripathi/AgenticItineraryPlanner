package com.tripplanner.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * Enhanced request DTO for creating itineraries with detailed user preferences.
 * This provides much more information for agents to work with.
 */
@Data
@Builder
public class EnhancedCreateItineraryReq {
    
    // ===== BASIC TRIP INFORMATION =====
    private String destination;
    private LocalDate startDate;
    private LocalDate endDate;
    private String budgetTier; // economy, mid-range, luxury
    private String language;
    
    // ===== PARTY INFORMATION =====
    private PartyDto party;
    private List<TravelerProfile> travelers;
    
    // ===== TRAVEL PREFERENCES =====
    private TravelPreferences preferences;
    private List<String> interests;
    private List<String> constraints;
    
    // ===== ACCOMMODATION PREFERENCES =====
    private AccommodationPreferences accommodationPreferences;
    
    // ===== TRANSPORTATION PREFERENCES =====
    private TransportationPreferences transportationPreferences;
    
    // ===== DINING PREFERENCES =====
    private DiningPreferences diningPreferences;
    
    // ===== ACTIVITY PREFERENCES =====
    private ActivityPreferences activityPreferences;
    
    // ===== NESTED DATA STRUCTURES =====
    
    @Data
    @Builder
    public static class TravelerProfile {
        private String name;
        private int age;
        private String type; // adult, child, infant
        private List<String> dietaryRestrictions;
        private List<String> allergies;
        private String mobilityLevel; // high, medium, low
        private List<String> personalInterests;
        private String pace; // slow, medium, fast
    }
    
    @Data
    @Builder
    public static class TravelPreferences {
        private String pace; // slow, medium, fast
        private String accommodationType; // hotel, airbnb, hostel, resort
        private String transportMode; // walking, public, private, mixed
        private List<String> themes; // adventure, relaxation, culture, nightlife, etc.
        private int walkingTolerance; // 1-5 scale
        private int crowdTolerance; // 1-5 scale
        private String budgetFlexibility; // strict, moderate, flexible
        private List<String> mustSee;
        private List<String> avoid;
        private String timeOfDayPreference; // morning, afternoon, evening, mixed
        private String seasonPreference; // spring, summer, fall, winter
    }
    
    @Data
    @Builder
    public static class AccommodationPreferences {
        private String type; // hotel, airbnb, hostel, resort
        private String location; // city-center, near-attractions, quiet-area, etc.
        private List<String> amenities; // wifi, pool, gym, spa, etc.
        private String priceRange; // budget, mid-range, luxury
        private String roomType; // single, double, suite, etc.
        private boolean breakfastIncluded;
        private boolean parkingRequired;
        private String accessibility; // wheelchair-accessible, etc.
    }
    
    @Data
    @Builder
    public static class TransportationPreferences {
        private String primaryMode; // walking, public, private, mixed
        private boolean airportTransfer;
        private boolean intercityTransport;
        private String comfortLevel; // basic, standard, premium
        private boolean scenicRoutes;
        private boolean localTransport;
        private String budgetAllocation; // low, medium, high
    }
    
    @Data
    @Builder
    public static class DiningPreferences {
        private List<String> cuisineTypes; // italian, asian, local, etc.
        private List<String> dietaryRestrictions; // vegetarian, vegan, gluten-free, etc.
        private String priceRange; // budget, mid-range, fine-dining
        private String diningStyle; // casual, formal, mixed
        private boolean localRecommendations;
        private boolean foodTours;
        private String mealFrequency; // light, normal, heavy
    }
    
    @Data
    @Builder
    public static class ActivityPreferences {
        private List<String> activityTypes; // sightseeing, adventure, culture, nightlife, etc.
        private String intensity; // low, medium, high
        private boolean guidedTours;
        private boolean freeTime;
        private String groupSize; // solo, couple, small-group, large-group
        private boolean photography;
        private boolean shopping;
        private boolean relaxation;
        private String timeAllocation; // packed, balanced, relaxed
    }
    
    /**
     * Convert from basic CreateItineraryReq to enhanced version.
     */
    public static EnhancedCreateItineraryReq fromBasic(CreateItineraryReq basic) {
        return EnhancedCreateItineraryReq.builder()
                .destination(basic.getDestination())
                .startDate(basic.getStartDate())
                .endDate(basic.getEndDate())
                .budgetTier(basic.getBudgetTier())
                .language(basic.getLanguage())
                .party(basic.getParty())
                .interests(basic.getInterests())
                .constraints(basic.getConstraints())
                .build();
    }
    
    /**
     * Get trip duration in days.
     */
    public int getDurationDays() {
        if (startDate == null || endDate == null) {
            return 0;
        }
        return (int) java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate);
    }
    
    /**
     * Get total party size.
     */
    public int getTotalPartySize() {
        return party != null ? party.getTotalGuests() : 0;
    }
}
