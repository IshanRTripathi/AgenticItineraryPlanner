package com.tripplanner.testing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripplanner.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Factory for creating test data objects with complete field accuracy.
 * Provides realistic test data for all scenarios including Bali luxury itinerary.
 */
public class TestDataFactory {
    
    private static final Logger logger = LoggerFactory.getLogger(TestDataFactory.class);
    
    private final ObjectMapper objectMapper;
    
    public TestDataFactory(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        logger.debug("TestDataFactory initialized with ObjectMapper");
    }
    
    /**
     * Create complete Bali luxury itinerary with all required fields from test data.
     */
    public NormalizedItinerary createBaliLuxuryItinerary() {
        try {
            String json = loadResourceFile("mock-data/destinations/bali/3-day-luxury-relaxation.json");
            return objectMapper.readValue(json, NormalizedItinerary.class);
        } catch (Exception e) {
            logger.warn("Could not load Bali itinerary from file, creating programmatically: {}", e.getMessage());
            return createBaliItineraryProgrammatically();
        }
    }
    
    /**
     * Create Tokyo business itinerary for testing different scenarios.
     */
    public NormalizedItinerary createTokyoBusinessItinerary() {
        NormalizedItinerary itinerary = new NormalizedItinerary();
        itinerary.setItineraryId("it_tokyo_business_5d_001");
        itinerary.setVersion(1);
        itinerary.setUserId("user_business_traveler_001");
        itinerary.setCreatedAt(System.currentTimeMillis());
        itinerary.setUpdatedAt(System.currentTimeMillis());
        itinerary.setSummary("A 5-day business trip to Tokyo with meetings and cultural experiences");
        itinerary.setCurrency("JPY");
        itinerary.setThemes(Arrays.asList("business", "culture", "technology", "networking"));
        itinerary.setOrigin("San Francisco, CA");
        itinerary.setDestination("Tokyo, Japan");
        itinerary.setStartDate("2024-03-15");
        itinerary.setEndDate("2024-03-20");
        itinerary.setDays(createTokyoDays());
        itinerary.setAgentData(new HashMap<>());
        itinerary.setRevisions(new ArrayList<>());
        itinerary.setChat(new ArrayList<>());
        return itinerary;
    }
    
    /**
     * Create budget backpacker itinerary for testing different price ranges.
     */
    public NormalizedItinerary createBudgetBackpackerItinerary() {
        NormalizedItinerary itinerary = new NormalizedItinerary();
        itinerary.setItineraryId("it_europe_budget_14d_001");
        itinerary.setVersion(1);
        itinerary.setUserId("user_backpacker_001");
        itinerary.setCreatedAt(System.currentTimeMillis());
        itinerary.setUpdatedAt(System.currentTimeMillis());
        itinerary.setSummary("A 14-day budget backpacking trip through Europe");
        itinerary.setCurrency("EUR");
        itinerary.setThemes(Arrays.asList("budget", "adventure", "culture", "hostels"));
        itinerary.setOrigin("New York, NY");
        itinerary.setDestination("Europe");
        itinerary.setStartDate("2024-06-01");
        itinerary.setEndDate("2024-06-15");
        itinerary.setDays(createBudgetDays());
        itinerary.setAgentData(new HashMap<>());
        itinerary.setRevisions(new ArrayList<>());
        itinerary.setChat(new ArrayList<>());
        return itinerary;
    }
    
    /**
     * Create family vacation itinerary for testing family-specific features.
     */
    public NormalizedItinerary createFamilyVacationItinerary() {
        NormalizedItinerary itinerary = new NormalizedItinerary();
        itinerary.setItineraryId("it_orlando_family_7d_001");
        itinerary.setVersion(1);
        itinerary.setUserId("user_family_001");
        itinerary.setCreatedAt(System.currentTimeMillis());
        itinerary.setUpdatedAt(System.currentTimeMillis());
        itinerary.setSummary("A 7-day family vacation to Orlando with theme parks and attractions");
        itinerary.setCurrency("USD");
        itinerary.setThemes(Arrays.asList("family", "theme-parks", "kids", "entertainment"));
        itinerary.setOrigin("Chicago, IL");
        itinerary.setDestination("Orlando, FL");
        itinerary.setStartDate("2024-07-10");
        itinerary.setEndDate("2024-07-17");
        itinerary.setDays(createFamilyDays());
        itinerary.setAgentData(new HashMap<>());
        itinerary.setRevisions(new ArrayList<>());
        itinerary.setChat(new ArrayList<>());
        return itinerary;
    }
    
    /**
     * Create test booking request for payment testing.
     */
    public BookingRequest createTestBookingRequest() {
        BookingRequest request = new BookingRequest();
        request.setItineraryId("test-itinerary-001");
        request.setUserId("test-user-001");
        request.setBookingType("hotel");
        request.setLocation("Bali, Indonesia");
        request.setGuests(2);
        return request;
    }
    
    /**
     * Create test payment request for payment service testing.
     */
    public PaymentRequest createTestPaymentRequest() {
        PaymentRequest request = new PaymentRequest();
        request.setAmount(850.00);
        request.setCurrency("USD");
        request.setDescription("Hotel booking payment");
        request.setCustomerEmail("test@example.com");
        return request;
    }
    
    /**
     * Create test chat request for chat service testing.
     */
    public ChatRequest createTestChatRequest() {
        ChatRequest request = new ChatRequest();
        request.setItineraryId("test-itinerary-001");
        request.setUserId("test-user-001");
        request.setText("Add a cooking class activity on day 2");
        return request;
    }
    
    /**
     * Create test create itinerary request.
     */
    public CreateItineraryReq createTestCreateItineraryRequest() {
        PartyDto party = new PartyDto();
        party.setAdults(2);
        party.setChildren(0);
        
        return CreateItineraryReq.builder()
                .destination("Bali, Indonesia")
                .startDate(LocalDate.parse("2024-01-24"))
                .endDate(LocalDate.parse("2024-01-27"))
                .party(party)
                .budgetTier("luxury")
                .interests(Arrays.asList("relaxation", "luxury", "culture"))
                .build();
    }
    
    /**
     * Load resource file from classpath.
     */
    private String loadResourceFile(String resourcePath) throws IOException {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new IOException("Resource not found: " + resourcePath);
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
    
    /**
     * Create Bali itinerary programmatically if file loading fails.
     */
    private NormalizedItinerary createBaliItineraryProgrammatically() {
        NormalizedItinerary itinerary = new NormalizedItinerary();
        itinerary.setItineraryId("it_bali_luxury_3d_001");
        itinerary.setVersion(1);
        itinerary.setUserId("user_luxury_traveler_001");
        itinerary.setCreatedAt(1769184000000L);
        itinerary.setUpdatedAt(1769184000000L);
        itinerary.setSummary("A luxurious 3-day escape to Bali featuring world-class spas, fine dining, and cultural experiences");
        itinerary.setCurrency("USD");
        itinerary.setThemes(Arrays.asList("relaxation", "luxury", "culture", "spa"));
        itinerary.setOrigin(null);
        itinerary.setDestination("Bali, Indonesia");
        itinerary.setStartDate("2026-01-24");
        itinerary.setEndDate("2026-01-27");
        itinerary.setDays(createBaliDays());
        itinerary.setAgentData(new HashMap<>());
        itinerary.setRevisions(new ArrayList<>());
        itinerary.setChat(new ArrayList<>());
        return itinerary;
    }
    
    /**
     * Create Bali days with complete node structure.
     */
    private java.util.List<NormalizedDay> createBaliDays() {
        java.util.List<NormalizedDay> days = new ArrayList<>();
        
        // Day 1
        NormalizedDay day1 = new NormalizedDay();
        day1.setDayNumber(1);
        day1.setDate("2026-01-24");
        day1.setLocation("Ubud, Bali");
        day1.setPace("relaxed");
        day1.setTotalDistance(25.0);
        day1.setTotalCost(450.0);
        day1.setTotalDuration(8.0);
        day1.setTimeWindowStart("09:00");
        day1.setTimeWindowEnd("21:00");
        day1.setTimeZone("Asia/Makassar");
        day1.setNodes(createBaliDay1Nodes());
        day1.setEdges(new ArrayList<>());
        days.add(day1);
        
        // Day 2
        NormalizedDay day2 = new NormalizedDay();
        day2.setDayNumber(2);
        day2.setDate("2026-01-25");
        day2.setLocation("Ubud Cultural District");
        day2.setPace("moderate");
        day2.setTotalDistance(15.0);
        day2.setTotalCost(320.0);
        day2.setTotalDuration(9.0);
        day2.setTimeWindowStart("08:00");
        day2.setTimeWindowEnd("20:00");
        day2.setTimeZone("Asia/Makassar");
        day2.setNodes(createBaliDay2Nodes());
        day2.setEdges(new ArrayList<>());
        days.add(day2);
        
        // Day 3
        NormalizedDay day3 = new NormalizedDay();
        day3.setDayNumber(3);
        day3.setDate("2026-01-27");
        day3.setLocation("Ubud to Airport");
        day3.setPace("relaxed");
        day3.setTotalDistance(25.0);
        day3.setTotalCost(180.0);
        day3.setTotalDuration(6.0);
        day3.setTimeWindowStart("09:00");
        day3.setTimeWindowEnd("15:00");
        day3.setTimeZone("Asia/Makassar");
        day3.setNodes(createBaliDay3Nodes());
        day3.setEdges(new ArrayList<>());
        days.add(day3);
        
        return days;
    }
    
    /**
     * Create Day 1 nodes for Bali itinerary.
     */
    private java.util.List<NormalizedNode> createBaliDay1Nodes() {
        java.util.List<NormalizedNode> nodes = new ArrayList<>();
        
        // Airport transfer
        NormalizedNode transfer = new NormalizedNode("arrival_001", "transport", "Private Airport Transfer to Ubud");
        NodeLocation transferLocation = new NodeLocation(
            "Ngurah Rai International Airport",
            "Jl. Raya Gusti Ngurah Rai, Tuban, Badung, Bali 80119",
            new Coordinates(-8.7467, 115.1671),
            "ChIJoQ8Q6NNB0i0RkOYkS7EPkSQ",
            "https://maps.google.com/?cid=123456789",
            4.2,
            "00:00",
            "23:59"
        );
        transfer.setLocation(transferLocation);
        transfer.setCost(new NodeCost(45.0, "USD"));
        transfer.setStatus("planned");
        transfer.setUpdatedBy("agent");
        transfer.setUpdatedAt(1769184000000L);
        nodes.add(transfer);
        
        // Hotel check-in
        NormalizedNode hotel = new NormalizedNode("hotel_001", "hotel", "Four Seasons Resort Bali at Sayan - Check-in");
        NodeLocation hotelLocation = new NodeLocation(
            "Four Seasons Resort Bali at Sayan",
            "Sayan, Ubud, Gianyar, Bali 80571",
            new Coordinates(-8.5069, 115.2625),
            "ChIJXYZ123ABC_SAYAN",
            "https://maps.google.com/?cid=987654321",
            4.8,
            "00:00",
            "23:59"
        );
        hotel.setLocation(hotelLocation);
        hotel.setCost(new NodeCost(850.0, "USD"));
        hotel.setStatus("planned");
        hotel.setUpdatedBy("agent");
        hotel.setUpdatedAt(1769184000000L);
        nodes.add(hotel);
        
        return nodes;
    }
    
    /**
     * Create Day 2 nodes for Bali itinerary.
     */
    private java.util.List<NormalizedNode> createBaliDay2Nodes() {
        java.util.List<NormalizedNode> nodes = new ArrayList<>();
        
        // Breakfast
        NormalizedNode breakfast = new NormalizedNode("breakfast_002", "meal", "Breakfast at Ayung Terrace");
        breakfast.setCost(new NodeCost(45.0, "USD"));
        breakfast.setStatus("planned");
        breakfast.setUpdatedBy("agent");
        breakfast.setUpdatedAt(1769184000000L);
        nodes.add(breakfast);
        
        return nodes;
    }
    
    /**
     * Create Day 3 nodes for Bali itinerary.
     */
    private java.util.List<NormalizedNode> createBaliDay3Nodes() {
        java.util.List<NormalizedNode> nodes = new ArrayList<>();
        
        // Departure transfer
        NormalizedNode departure = new NormalizedNode("departure_001", "transport", "Private Transfer to Airport");
        departure.setCost(new NodeCost(45.0, "USD"));
        departure.setStatus("planned");
        departure.setUpdatedBy("agent");
        departure.setUpdatedAt(1769184000000L);
        nodes.add(departure);
        
        return nodes;
    }
    
    /**
     * Create Tokyo days for business itinerary.
     */
    private java.util.List<NormalizedDay> createTokyoDays() {
        java.util.List<NormalizedDay> days = new ArrayList<>();
        
        // Create 5 days with business-focused activities
        for (int i = 1; i <= 5; i++) {
            NormalizedDay day = new NormalizedDay();
            day.setDayNumber(i);
            day.setDate("2024-03-" + (14 + i));
            day.setLocation("Tokyo, Japan");
            day.setPace("busy");
            day.setTotalDistance(20.0);
            day.setTotalCost(300.0);
            day.setTotalDuration(10.0);
            day.setTimeWindowStart("08:00");
            day.setTimeWindowEnd("22:00");
            day.setTimeZone("Asia/Tokyo");
            day.setNodes(new ArrayList<>());
            day.setEdges(new ArrayList<>());
            days.add(day);
        }
        
        return days;
    }
    
    /**
     * Create budget days for backpacker itinerary.
     */
    private java.util.List<NormalizedDay> createBudgetDays() {
        java.util.List<NormalizedDay> days = new ArrayList<>();
        
        // Create 14 days with budget-focused activities
        for (int i = 1; i <= 14; i++) {
            NormalizedDay day = new NormalizedDay();
            day.setDayNumber(i);
            day.setDate("2024-06-" + String.format("%02d", i));
            day.setLocation("Europe");
            day.setPace("moderate");
            day.setTotalDistance(30.0);
            day.setTotalCost(50.0);
            day.setTotalDuration(12.0);
            day.setTimeWindowStart("07:00");
            day.setTimeWindowEnd("23:00");
            day.setTimeZone("CET");
            day.setNodes(new ArrayList<>());
            day.setEdges(new ArrayList<>());
            days.add(day);
        }
        
        return days;
    }
    
    /**
     * Create family days for vacation itinerary.
     */
    private java.util.List<NormalizedDay> createFamilyDays() {
        java.util.List<NormalizedDay> days = new ArrayList<>();
        
        // Create 7 days with family-focused activities
        for (int i = 1; i <= 7; i++) {
            NormalizedDay day = new NormalizedDay();
            day.setDayNumber(i);
            day.setDate("2024-07-" + (9 + i));
            day.setLocation("Orlando, FL");
            day.setPace("family-friendly");
            day.setTotalDistance(15.0);
            day.setTotalCost(400.0);
            day.setTotalDuration(10.0);
            day.setTimeWindowStart("09:00");
            day.setTimeWindowEnd("21:00");
            day.setTimeZone("EST");
            day.setNodes(new ArrayList<>());
            day.setEdges(new ArrayList<>());
            days.add(day);
        }
        
        return days;
    }
}