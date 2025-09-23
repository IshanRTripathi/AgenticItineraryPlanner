package com.tripplanner.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Master data structure that serves as the central hub for all trip information.
 * This is populated by different agents and used by the UI for rendering.
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MasterTripData {
    
    // ===== BASIC TRIP INFORMATION =====
    private String id;
    private String destination;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status; // draft, generating, completed, failed
    
    // ===== TRAVELER INFORMATION =====
    private PartyDto party;
    private List<TravelerProfile> travelers;
    private TravelerProfile leadTraveler;
    
    // ===== BUDGET & PREFERENCES =====
    private String budgetTier; // economy, mid-range, luxury
    private BudgetBreakdown budget;
    private TravelPreferences preferences;
    private List<String> interests;
    private List<String> constraints;
    private String language;
    
    // ===== DESTINATION ANALYSIS (Places Agent) =====
    private DestinationAnalysis destinationAnalysis;
    
    // ===== ACCOMMODATION OPTIONS (Hotels Agent) =====
    private List<AccommodationOption> accommodationOptions;
    private AccommodationOption selectedAccommodation;
    
    // ===== FLIGHT OPTIONS (Flights Agent) =====
    private List<FlightOption> flightOptions;
    private FlightOption selectedFlight;
    
    // ===== RESTAURANT OPTIONS (Food Agent) =====
    private List<RestaurantOption> restaurantOptions;
    private List<RestaurantOption> selectedRestaurants;
    
    // ===== TRANSPORTATION OPTIONS (Transport Agent) =====
    private List<TransportOption> transportOptions;
    private List<TransportOption> selectedTransport;
    
    // ===== DAILY ITINERARY (Planner Agent) =====
    private List<DayPlan> dailyItinerary;
    
    // ===== ADDITIONAL DATA =====
    private String summary;
    private Map<String, Object> map;
    private boolean isPublic;
    private String shareToken;
    private String createdAt;
    private String updatedAt;
    
    // ===== AGENT PROGRESS TRACKING =====
    private Map<String, AgentProgress> agentProgress;
    
    // ===== NESTED DATA STRUCTURES =====
    
    @Data
    @Builder
    public static class TravelerProfile {
        private String id;
        private String name;
        private int age;
        private String type; // adult, child, infant
        private List<String> dietaryRestrictions;
        private List<String> allergies;
        private String mobilityLevel; // high, medium, low
        private List<String> interests;
        private String pace; // slow, medium, fast
        private String accommodationPreference; // hotel, airbnb, hostel, resort
        private String transportPreference; // walking, public, private, mixed
    }
    
    @Data
    @Builder
    public static class BudgetBreakdown {
        private double total;
        private String currency;
        private double accommodation;
        private double food;
        private double transport;
        private double activities;
        private double shopping;
        private double emergency;
        private double perDay;
        private double perPerson;
    }
    
    @Data
    @Builder
    public static class TravelPreferences {
        private String pace; // slow, medium, fast
        private String accommodationType; // hotel, airbnb, hostel, resort
        private String transportMode; // walking, public, private, mixed
        private List<String> themes; // adventure, relaxation, culture, nightlife, etc.
        private String walkingTolerance; // 1-5 scale
        private String crowdTolerance; // 1-5 scale
        private String budgetFlexibility; // strict, moderate, flexible
        private List<String> mustSee;
        private List<String> avoid;
        private String timeOfDayPreference; // morning, afternoon, evening, mixed
        private String seasonPreference; // spring, summer, fall, winter
    }
    
    @Data
    @Builder
    public static class DestinationAnalysis {
        private String destination;
        private List<KeyArea> keyAreas;
        private List<String> touristHotspots;
        private List<String> hiddenGems;
        private List<String> localInsights;
        private Map<String, Object> weatherInfo;
        private Map<String, Object> culturalInfo;
        private List<String> safetyTips;
        private List<String> bestTimesToVisit;
        private Map<String, Object> costOfLiving;
    }
    
    @Data
    @Builder
    public static class KeyArea {
        private String name;
        private String description;
        private String type; // historic, modern, cultural, entertainment, shopping, residential, business
        private Coordinates coordinates;
        private CrowdPattern crowdPattern;
        private List<String> highlights;
        private String tips;
        private String bestTimeToVisit;
        private String accessibility;
    }
    
    @Data
    @Builder
    public static class Coordinates {
        private double lat;
        private double lng;
    }
    
    @Data
    @Builder
    public static class CrowdPattern {
        private List<String> peakHours;
        private List<String> quietHours;
        private List<String> bestDays;
        private String crowdLevel; // low, medium, high
    }
    
    @Data
    @Builder
    public static class AccommodationOption {
        private String id;
        private String name;
        private String type; // hotel, airbnb, hostel, resort
        private String description;
        private Coordinates location;
        private double rating;
        private PriceDto price;
        private List<String> amenities;
        private List<String> features;
        private String checkIn;
        private String checkOut;
        private String bookingUrl;
        private boolean isSelected;
        private String selectionReason;
    }
    
    @Data
    @Builder
    public static class FlightOption {
        private String id;
        private String airline;
        private String flightNumber;
        private FlightDetails departure;
        private FlightDetails arrival;
        private String duration;
        private int stops;
        private PriceDto price;
        private String category; // cost-effective, fastest, optimal
        private List<String> features;
        private String bookingUrl;
        private boolean holdAvailable;
        private BaggageInfo baggage;
        private boolean isSelected;
        private String selectionReason;
    }
    
    @Data
    @Builder
    public static class FlightDetails {
        private String airport;
        private String time;
        private String terminal;
        private String gate;
    }
    
    @Data
    @Builder
    public static class BaggageInfo {
        private boolean included;
        private String weight;
        private String dimensions;
    }
    
    @Data
    @Builder
    public static class RestaurantOption {
        private String id;
        private String name;
        private String description;
        private String cuisine;
        private Coordinates location;
        private double rating;
        private PriceDto price;
        private List<String> dietaryOptions;
        private List<String> features;
        private String openingHours;
        private String priceRange;
        private String bookingUrl;
        private boolean isSelected;
        private String selectionReason;
        private String recommendedFor; // breakfast, lunch, dinner, snack
    }
    
    @Data
    @Builder
    public static class TransportOption {
        private String id;
        private String name;
        private String type; // walking, public, private, mixed
        private String description;
        private Coordinates fromLocation;
        private Coordinates toLocation;
        private String duration;
        private PriceDto price;
        private List<String> features;
        private String bookingUrl;
        private boolean isSelected;
        private String selectionReason;
    }
    
    @Data
    @Builder
    public static class DayPlan {
        private int dayNumber;
        private String date;
        private String theme;
        private String location;
        private List<Activity> activities;
        private List<Meal> meals;
        private AccommodationOption accommodation;
        private List<TransportOption> transportation;
        private String notes;
        private String weather;
        private String tips;
    }
    
    @Data
    @Builder
    public static class Activity {
        private String id;
        private String name;
        private String description;
        private String category;
        private Coordinates location;
        private String startTime;
        private String endTime;
        private String duration;
        private PriceDto price;
        private boolean bookingRequired;
        private String bookingUrl;
        private String tips;
        private String bestTimeToVisit;
        private String accessibility;
        private List<String> features;
    }
    
    @Data
    @Builder
    public static class Meal {
        private String id;
        private String name;
        private String type; // breakfast, lunch, dinner, snack
        private String cuisine;
        private Coordinates location;
        private String time;
        private PriceDto price;
        private List<String> dietaryOptions;
        private String bookingUrl;
        private String notes;
    }
    
    @Data
    @Builder
    public static class AgentProgress {
        private String status; // queued, running, completed, failed
        private int progress; // 0-100
        private String message;
        private String startTime;
        private String endTime;
        private String errorMessage;
    }
}
