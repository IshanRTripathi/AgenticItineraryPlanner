package com.tripplanner.service;

import com.tripplanner.api.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;

/**
 * Service for generating comprehensive sample data for testing.
 * Creates realistic itineraries with various node types and scenarios.
 */
@Service
public class SampleDataGenerator {
    
    private static final Logger logger = LoggerFactory.getLogger(SampleDataGenerator.class);
    
    private final ItineraryJsonService itineraryJsonService;
    
    public SampleDataGenerator(ItineraryJsonService itineraryJsonService) {
        this.itineraryJsonService = itineraryJsonService;
    }
    
    /**
     * Generate comprehensive Barcelona sample itinerary.
     */
    public NormalizedItinerary generateBarcelonaSample() {
        logger.info("Generating comprehensive Barcelona sample itinerary");
        
        String itineraryId = "it_barcelona_comprehensive";
        
        // Create Day 1 - Arrival and City Center
        NormalizedDay day1 = createBarcelonaDay1();
        
        // Create Day 2 - Gaudi Architecture
        NormalizedDay day2 = createBarcelonaDay2();
        
        // Create Day 3 - Beach and Departure
        NormalizedDay day3 = createBarcelonaDay3();
        
        // Create itinerary
        NormalizedItinerary itinerary = new NormalizedItinerary(itineraryId, 1);
        itinerary.setSummary("3-day Barcelona adventure: Gaudi architecture, local cuisine, and Mediterranean beaches");
        itinerary.setCurrency("EUR");
        itinerary.setThemes(Arrays.asList("culture", "architecture", "food", "beach", "family"));
        itinerary.setDays(Arrays.asList(day1, day2, day3));
        itinerary.setSettings(new ItinerarySettings(false, "trip"));
        
        // Set agent status
        Map<String, AgentStatus> agents = new HashMap<>();
        agents.put("planner", new AgentStatus(null, "idle"));
        agents.put("enrichment", new AgentStatus(null, "idle"));
        itinerary.setAgents(agents);
        
        return itinerary;
    }
    
    /**
     * Generate Paris sample itinerary.
     */
    public NormalizedItinerary generateParisSample() {
        logger.info("Generating comprehensive Paris sample itinerary");
        
        String itineraryId = "it_paris_comprehensive";
        
        // Create Day 1 - Arrival and Eiffel Tower
        NormalizedDay day1 = createParisDay1();
        
        // Create Day 2 - Museums and Art
        NormalizedDay day2 = createParisDay2();
        
        // Create Day 3 - Montmartre and Departure
        NormalizedDay day3 = createParisDay3();
        
        // Create itinerary
        NormalizedItinerary itinerary = new NormalizedItinerary(itineraryId, 1);
        itinerary.setSummary("3-day Paris romance: Eiffel Tower, Louvre, and Montmartre charm");
        itinerary.setCurrency("EUR");
        itinerary.setThemes(Arrays.asList("romance", "art", "culture", "food", "history"));
        itinerary.setDays(Arrays.asList(day1, day2, day3));
        itinerary.setSettings(new ItinerarySettings(false, "trip"));
        
        // Set agent status
        Map<String, AgentStatus> agents = new HashMap<>();
        agents.put("planner", new AgentStatus(null, "idle"));
        agents.put("enrichment", new AgentStatus(null, "idle"));
        itinerary.setAgents(agents);
        
        return itinerary;
    }
    
    /**
     * Generate Tokyo sample itinerary.
     */
    public NormalizedItinerary generateTokyoSample() {
        logger.info("Generating comprehensive Tokyo sample itinerary");
        
        String itineraryId = "it_tokyo_comprehensive";
        
        // Create Day 1 - Arrival and Shibuya
        NormalizedDay day1 = createTokyoDay1();
        
        // Create Day 2 - Traditional Tokyo
        NormalizedDay day2 = createTokyoDay2();
        
        // Create Day 3 - Modern Tokyo and Departure
        NormalizedDay day3 = createTokyoDay3();
        
        // Create itinerary
        NormalizedItinerary itinerary = new NormalizedItinerary(itineraryId, 1);
        itinerary.setSummary("3-day Tokyo experience: Shibuya crossing, temples, and modern innovation");
        itinerary.setCurrency("JPY");
        itinerary.setThemes(Arrays.asList("culture", "technology", "food", "tradition", "modern"));
        itinerary.setDays(Arrays.asList(day1, day2, day3));
        itinerary.setSettings(new ItinerarySettings(false, "trip"));
        
        // Set agent status
        Map<String, AgentStatus> agents = new HashMap<>();
        agents.put("planner", new AgentStatus(null, "idle"));
        agents.put("enrichment", new AgentStatus(null, "idle"));
        itinerary.setAgents(agents);
        
        return itinerary;
    }
    
    /**
     * Create Barcelona Day 1 - Arrival and City Center.
     */
    private NormalizedDay createBarcelonaDay1() {
        LocalDate date = LocalDate.now().plusDays(1);
        
        List<NormalizedNode> nodes = Arrays.asList(
            // Airport arrival
            createNode("n_airport_arrival", "transport", "Arrive at Barcelona Airport", 
                     41.2974, 2.0833, "10:00", "10:30", 30, 0.0, "EUR"),
            
            // Hotel check-in
            createNode("n_hotel_checkin", "accommodation", "Hotel Casa Fuster", 
                     41.3954, 2.1611, "12:00", "12:30", 30, 0.0, "EUR"),
            
            // Lunch
            createNode("n_lunch_la_boqueria", "meal", "Lunch at La Boqueria Market", 
                     41.3819, 2.1719, "13:00", "14:30", 90, 25.0, "EUR"),
            
            // Las Ramblas walk
            createNode("n_las_ramblas", "attraction", "Walk along Las Ramblas", 
                     41.3800, 2.1700, "15:00", "16:30", 90, 0.0, "EUR"),
            
            // Gothic Quarter
            createNode("n_gothic_quarter", "attraction", "Explore Gothic Quarter", 
                     41.3833, 2.1767, "17:00", "19:00", 120, 0.0, "EUR"),
            
            // Dinner
            createNode("n_dinner_tapas", "meal", "Tapas dinner at local restaurant", 
                     41.3833, 2.1767, "20:00", "22:00", 120, 45.0, "EUR")
        );
        
        List<Edge> edges = Arrays.asList(
            new Edge("n_airport_arrival", "n_hotel_checkin"),
            new Edge("n_hotel_checkin", "n_lunch_la_boqueria"),
            new Edge("n_lunch_la_boqueria", "n_las_ramblas"),
            new Edge("n_las_ramblas", "n_gothic_quarter"),
            new Edge("n_gothic_quarter", "n_dinner_tapas")
        );
        
        NormalizedDay day = new NormalizedDay(1, date.toString(), "Barcelona");
        day.setNodes(nodes);
        day.setEdges(edges);
        day.setPacing(new Pacing("balanced", 30, 6));
        day.setTimeWindow(new TimeWindow("10:00", "22:00"));
        day.setTotals(new DayTotals(15.5, 70.0, 8.0));
        
        return day;
    }
    
    /**
     * Create Barcelona Day 2 - Gaudi Architecture.
     */
    private NormalizedDay createBarcelonaDay2() {
        LocalDate date = LocalDate.now().plusDays(2);
        
        List<NormalizedNode> nodes = Arrays.asList(
            // Breakfast
            createNode("n_breakfast_cafe", "meal", "Breakfast at local café", 
                     41.3954, 2.1611, "08:00", "09:00", 60, 12.0, "EUR"),
            
            // Sagrada Familia
            createNode("n_sagrada_familia", "attraction", "Sagrada Familia", 
                     41.4036, 2.1744, "09:30", "12:00", 150, 26.0, "EUR"),
            
            // Lunch
            createNode("n_lunch_sagrada", "meal", "Lunch near Sagrada Familia", 
                     41.4036, 2.1744, "12:30", "13:30", 60, 20.0, "EUR"),
            
            // Park Güell
            createNode("n_park_guell", "attraction", "Park Güell", 
                     41.4145, 2.1527, "14:30", "17:00", 150, 10.0, "EUR"),
            
            // Casa Batlló
            createNode("n_casa_batllo", "attraction", "Casa Batlló", 
                     41.3917, 2.1649, "17:30", "19:00", 90, 35.0, "EUR"),
            
            // Dinner
            createNode("n_dinner_gaudi", "meal", "Dinner at Gaudi-inspired restaurant", 
                     41.3917, 2.1649, "20:00", "22:00", 120, 55.0, "EUR")
        );
        
        List<Edge> edges = Arrays.asList(
            new Edge("n_breakfast_cafe", "n_sagrada_familia"),
            new Edge("n_sagrada_familia", "n_lunch_sagrada"),
            new Edge("n_lunch_sagrada", "n_park_guell"),
            new Edge("n_park_guell", "n_casa_batllo"),
            new Edge("n_casa_batllo", "n_dinner_gaudi")
        );
        
        NormalizedDay day = new NormalizedDay(2, date.toString(), "Barcelona");
        day.setNodes(nodes);
        day.setEdges(edges);
        day.setPacing(new Pacing("balanced", 30, 6));
        day.setTimeWindow(new TimeWindow("08:00", "22:00"));
        day.setTotals(new DayTotals(12.3, 158.0, 10.5));
        
        return day;
    }
    
    /**
     * Create Barcelona Day 3 - Beach and Departure.
     */
    private NormalizedDay createBarcelonaDay3() {
        LocalDate date = LocalDate.now().plusDays(3);
        
        List<NormalizedNode> nodes = Arrays.asList(
            // Breakfast
            createNode("n_breakfast_beach", "meal", "Beachside breakfast", 
                     41.3954, 2.1611, "08:00", "09:00", 60, 15.0, "EUR"),
            
            // Barceloneta Beach
            createNode("n_barceloneta_beach", "attraction", "Barceloneta Beach", 
                     41.3789, 2.1925, "10:00", "13:00", 180, 0.0, "EUR"),
            
            // Lunch at beach
            createNode("n_lunch_beach", "meal", "Seafood lunch at beach restaurant", 
                     41.3789, 2.1925, "13:30", "15:00", 90, 35.0, "EUR"),
            
            // Hotel check-out
            createNode("n_hotel_checkout", "accommodation", "Hotel Casa Fuster - Check out", 
                     41.3954, 2.1611, "16:00", "16:30", 30, 0.0, "EUR"),
            
            // Airport departure
            createNode("n_airport_departure", "transport", "Depart from Barcelona Airport", 
                     41.2974, 2.0833, "18:00", "18:30", 30, 0.0, "EUR")
        );
        
        List<Edge> edges = Arrays.asList(
            new Edge("n_breakfast_beach", "n_barceloneta_beach"),
            new Edge("n_barceloneta_beach", "n_lunch_beach"),
            new Edge("n_lunch_beach", "n_hotel_checkout"),
            new Edge("n_hotel_checkout", "n_airport_departure")
        );
        
        NormalizedDay day = new NormalizedDay(3, date.toString(), "Barcelona");
        day.setNodes(nodes);
        day.setEdges(edges);
        day.setPacing(new Pacing("relaxed", 45, 5));
        day.setTimeWindow(new TimeWindow("08:00", "18:30"));
        day.setTotals(new DayTotals(8.7, 50.0, 6.5));
        
        return day;
    }
    
    /**
     * Create Paris Day 1 - Arrival and Eiffel Tower.
     */
    private NormalizedDay createParisDay1() {
        LocalDate date = LocalDate.now().plusDays(1);
        
        List<NormalizedNode> nodes = Arrays.asList(
            // Airport arrival
            createNode("n_cdg_arrival", "transport", "Arrive at Charles de Gaulle", 
                     49.0097, 2.5479, "11:00", "11:30", 30, 0.0, "EUR"),
            
            // Hotel check-in
            createNode("n_hotel_paris", "accommodation", "Hotel des Invalides", 
                     48.8566, 2.3522, "13:00", "13:30", 30, 0.0, "EUR"),
            
            // Lunch
            createNode("n_lunch_paris", "meal", "Lunch at local bistro", 
                     48.8566, 2.3522, "14:00", "15:30", 90, 28.0, "EUR"),
            
            // Eiffel Tower
            createNode("n_eiffel_tower", "attraction", "Eiffel Tower", 
                     48.8584, 2.2945, "16:00", "18:30", 150, 29.0, "EUR"),
            
            // Seine River cruise
            createNode("n_seine_cruise", "attraction", "Seine River Cruise", 
                     48.8566, 2.3522, "19:00", "20:30", 90, 15.0, "EUR"),
            
            // Dinner
            createNode("n_dinner_paris", "meal", "Dinner at traditional French restaurant", 
                     48.8566, 2.3522, "21:00", "23:00", 120, 65.0, "EUR")
        );
        
        List<Edge> edges = Arrays.asList(
            new Edge("n_cdg_arrival", "n_hotel_paris"),
            new Edge("n_hotel_paris", "n_lunch_paris"),
            new Edge("n_lunch_paris", "n_eiffel_tower"),
            new Edge("n_eiffel_tower", "n_seine_cruise"),
            new Edge("n_seine_cruise", "n_dinner_paris")
        );
        
        NormalizedDay day = new NormalizedDay(1, date.toString(), "Paris");
        day.setNodes(nodes);
        day.setEdges(edges);
        day.setPacing(new Pacing("balanced", 30, 6));
        day.setTimeWindow(new TimeWindow("11:00", "23:00"));
        day.setTotals(new DayTotals(18.2, 137.0, 8.5));
        
        return day;
    }
    
    /**
     * Create Paris Day 2 - Museums and Art.
     */
    private NormalizedDay createParisDay2() {
        LocalDate date = LocalDate.now().plusDays(2);
        
        List<NormalizedNode> nodes = Arrays.asList(
            // Breakfast
            createNode("n_breakfast_paris", "meal", "Breakfast at café", 
                     48.8566, 2.3522, "08:00", "09:00", 60, 12.0, "EUR"),
            
            // Louvre Museum
            createNode("n_louvre", "attraction", "Louvre Museum", 
                     48.8606, 2.3376, "09:30", "13:00", 210, 17.0, "EUR"),
            
            // Lunch
            createNode("n_lunch_louvre", "meal", "Lunch near Louvre", 
                     48.8606, 2.3376, "13:30", "14:30", 60, 22.0, "EUR"),
            
            // Notre-Dame
            createNode("n_notre_dame", "attraction", "Notre-Dame Cathedral", 
                     48.8530, 2.3499, "15:00", "16:30", 90, 0.0, "EUR"),
            
            // Musée d'Orsay
            createNode("n_orsay", "attraction", "Musée d'Orsay", 
                     48.8600, 2.3266, "17:00", "19:00", 120, 16.0, "EUR"),
            
            // Dinner
            createNode("n_dinner_art", "meal", "Dinner at art district restaurant", 
                     48.8600, 2.3266, "20:00", "22:00", 120, 48.0, "EUR")
        );
        
        List<Edge> edges = Arrays.asList(
            new Edge("n_breakfast_paris", "n_louvre"),
            new Edge("n_louvre", "n_lunch_louvre"),
            new Edge("n_lunch_louvre", "n_notre_dame"),
            new Edge("n_notre_dame", "n_orsay"),
            new Edge("n_orsay", "n_dinner_art")
        );
        
        NormalizedDay day = new NormalizedDay(2, date.toString(), "Paris");
        day.setNodes(nodes);
        day.setEdges(edges);
        day.setPacing(new Pacing("balanced", 30, 6));
        day.setTimeWindow(new TimeWindow("08:00", "22:00"));
        day.setTotals(new DayTotals(14.8, 115.0, 11.0));
        
        return day;
    }
    
    /**
     * Create Paris Day 3 - Montmartre and Departure.
     */
    private NormalizedDay createParisDay3() {
        LocalDate date = LocalDate.now().plusDays(3);
        
        List<NormalizedNode> nodes = Arrays.asList(
            // Breakfast
            createNode("n_breakfast_montmartre", "meal", "Breakfast in Montmartre", 
                     48.8566, 2.3522, "08:00", "09:00", 60, 14.0, "EUR"),
            
            // Sacré-Cœur
            createNode("n_sacre_coeur", "attraction", "Sacré-Cœur Basilica", 
                     48.8867, 2.3431, "09:30", "11:30", 120, 0.0, "EUR"),
            
            // Montmartre walk
            createNode("n_montmartre_walk", "attraction", "Walk through Montmartre", 
                     48.8867, 2.3431, "12:00", "13:30", 90, 0.0, "EUR"),
            
            // Lunch
            createNode("n_lunch_montmartre", "meal", "Lunch at Montmartre café", 
                     48.8867, 2.3431, "14:00", "15:00", 60, 25.0, "EUR"),
            
            // Hotel check-out
            createNode("n_hotel_checkout_paris", "accommodation", "Hotel des Invalides - Check out", 
                     48.8566, 2.3522, "16:00", "16:30", 30, 0.0, "EUR"),
            
            // Airport departure
            createNode("n_cdg_departure", "transport", "Depart from Charles de Gaulle", 
                     49.0097, 2.5479, "18:00", "18:30", 30, 0.0, "EUR")
        );
        
        List<Edge> edges = Arrays.asList(
            new Edge("n_breakfast_montmartre", "n_sacre_coeur"),
            new Edge("n_sacre_coeur", "n_montmartre_walk"),
            new Edge("n_montmartre_walk", "n_lunch_montmartre"),
            new Edge("n_lunch_montmartre", "n_hotel_checkout_paris"),
            new Edge("n_hotel_checkout_paris", "n_cdg_departure")
        );
        
        NormalizedDay day = new NormalizedDay(3, date.toString(), "Paris");
        day.setNodes(nodes);
        day.setEdges(edges);
        day.setPacing(new Pacing("relaxed", 45, 6));
        day.setTimeWindow(new TimeWindow("08:00", "18:30"));
        day.setTotals(new DayTotals(6.2, 39.0, 6.5));
        
        return day;
    }
    
    /**
     * Create Tokyo Day 1 - Arrival and Shibuya.
     */
    private NormalizedDay createTokyoDay1() {
        LocalDate date = LocalDate.now().plusDays(1);
        
        List<NormalizedNode> nodes = Arrays.asList(
            // Airport arrival
            createNode("n_narita_arrival", "transport", "Arrive at Narita Airport", 
                     35.7720, 140.3928, "14:00", "14:30", 30, 0.0, "JPY"),
            
            // Hotel check-in
            createNode("n_hotel_tokyo", "accommodation", "Hotel Shibuya Sky", 
                     35.6580, 139.7016, "16:00", "16:30", 30, 0.0, "JPY"),
            
            // Shibuya Crossing
            createNode("n_shibuya_crossing", "attraction", "Shibuya Crossing", 
                     35.6580, 139.7016, "17:00", "18:00", 60, 0.0, "JPY"),
            
            // Dinner
            createNode("n_dinner_shibuya", "meal", "Ramen dinner in Shibuya", 
                     35.6580, 139.7016, "19:00", "20:30", 90, 1200.0, "JPY"),
            
            // Tokyo Skytree
            createNode("n_skytree", "attraction", "Tokyo Skytree", 
                     35.7101, 139.8107, "21:00", "22:30", 90, 2100.0, "JPY")
        );
        
        List<Edge> edges = Arrays.asList(
            new Edge("n_narita_arrival", "n_hotel_tokyo"),
            new Edge("n_hotel_tokyo", "n_shibuya_crossing"),
            new Edge("n_shibuya_crossing", "n_dinner_shibuya"),
            new Edge("n_dinner_shibuya", "n_skytree")
        );
        
        NormalizedDay day = new NormalizedDay(1, date.toString(), "Tokyo");
        day.setNodes(nodes);
        day.setEdges(edges);
        day.setPacing(new Pacing("balanced", 30, 5));
        day.setTimeWindow(new TimeWindow("14:00", "22:30"));
        day.setTotals(new DayTotals(45.2, 3300.0, 5.0));
        
        return day;
    }
    
    /**
     * Create Tokyo Day 2 - Traditional Tokyo.
     */
    private NormalizedDay createTokyoDay2() {
        LocalDate date = LocalDate.now().plusDays(2);
        
        List<NormalizedNode> nodes = Arrays.asList(
            // Breakfast
            createNode("n_breakfast_tokyo", "meal", "Traditional Japanese breakfast", 
                     35.6580, 139.7016, "08:00", "09:00", 60, 800.0, "JPY"),
            
            // Senso-ji Temple
            createNode("n_sensoji", "attraction", "Senso-ji Temple", 
                     35.7148, 139.7967, "10:00", "12:00", 120, 0.0, "JPY"),
            
            // Lunch
            createNode("n_lunch_asakusa", "meal", "Lunch in Asakusa", 
                     35.7148, 139.7967, "12:30", "13:30", 60, 1500.0, "JPY"),
            
            // Imperial Palace
            createNode("n_imperial_palace", "attraction", "Imperial Palace", 
                     35.6852, 139.7528, "14:30", "16:30", 120, 0.0, "JPY"),
            
            // Ginza shopping
            createNode("n_ginza", "attraction", "Ginza shopping district", 
                     35.6719, 139.7653, "17:00", "19:00", 120, 0.0, "JPY"),
            
            // Dinner
            createNode("n_dinner_ginza", "meal", "Sushi dinner in Ginza", 
                     35.6719, 139.7653, "20:00", "22:00", 120, 8000.0, "JPY")
        );
        
        List<Edge> edges = Arrays.asList(
            new Edge("n_breakfast_tokyo", "n_sensoji"),
            new Edge("n_sensoji", "n_lunch_asakusa"),
            new Edge("n_lunch_asakusa", "n_imperial_palace"),
            new Edge("n_imperial_palace", "n_ginza"),
            new Edge("n_ginza", "n_dinner_ginza")
        );
        
        NormalizedDay day = new NormalizedDay(2, date.toString(), "Tokyo");
        day.setNodes(nodes);
        day.setEdges(edges);
        day.setPacing(new Pacing("balanced", 30, 6));
        day.setTimeWindow(new TimeWindow("08:00", "22:00"));
        day.setTotals(new DayTotals(28.5, 10300.0, 10.0));
        
        return day;
    }
    
    /**
     * Create Tokyo Day 3 - Modern Tokyo and Departure.
     */
    private NormalizedDay createTokyoDay3() {
        LocalDate date = LocalDate.now().plusDays(3);
        
        List<NormalizedNode> nodes = Arrays.asList(
            // Breakfast
            createNode("n_breakfast_modern", "meal", "Modern café breakfast", 
                     35.6580, 139.7016, "08:00", "09:00", 60, 1000.0, "JPY"),
            
            // Harajuku
            createNode("n_harajuku", "attraction", "Harajuku district", 
                     35.6702, 139.7026, "10:00", "12:00", 120, 0.0, "JPY"),
            
            // Lunch
            createNode("n_lunch_harajuku", "meal", "Lunch in Harajuku", 
                     35.6702, 139.7026, "12:30", "13:30", 60, 1200.0, "JPY"),
            
            // Hotel check-out
            createNode("n_hotel_checkout_tokyo", "accommodation", "Hotel Shibuya Sky - Check out", 
                     35.6580, 139.7016, "15:00", "15:30", 30, 0.0, "JPY"),
            
            // Airport departure
            createNode("n_narita_departure", "transport", "Depart from Narita Airport", 
                     35.7720, 140.3928, "17:00", "17:30", 30, 0.0, "JPY")
        );
        
        List<Edge> edges = Arrays.asList(
            new Edge("n_breakfast_modern", "n_harajuku"),
            new Edge("n_harajuku", "n_lunch_harajuku"),
            new Edge("n_lunch_harajuku", "n_hotel_checkout_tokyo"),
            new Edge("n_hotel_checkout_tokyo", "n_narita_departure")
        );
        
        NormalizedDay day = new NormalizedDay(3, date.toString(), "Tokyo");
        day.setNodes(nodes);
        day.setEdges(edges);
        day.setPacing(new Pacing("relaxed", 45, 5));
        day.setTimeWindow(new TimeWindow("08:00", "17:30"));
        day.setTotals(new DayTotals(12.8, 2200.0, 5.0));
        
        return day;
    }
    
    /**
     * Helper method to create a node.
     */
    private NormalizedNode createNode(String id, String type, String title, 
                                    double lat, double lng, String startTime, String endTime, 
                                    int durationMin, double cost, String currency) {
        NormalizedNode node = new NormalizedNode(id, type, title);
        
        // Set location
        Coordinates coords = new Coordinates(lat, lng);
        NodeLocation location = new NodeLocation(title, "Location", coords);
        node.setLocation(location);
        
        // Set timing
        Instant start = parseTimeToInstant(startTime);
        Instant end = parseTimeToInstant(endTime);
        NodeTiming timing = new NodeTiming(start, end, durationMin);
        node.setTiming(timing);
        
        // Set cost
        if (cost > 0) {
            NodeCost nodeCost = new NodeCost(cost, currency, "person");
            node.setCost(nodeCost);
        }
        
        // Set details
        NodeDetails details = new NodeDetails();
        details.setRating(4.0);
        details.setCategory(type);
        details.setTags(Arrays.asList(type));
        node.setDetails(details);
        
        // Set audit trail fields
        node.setStatus("planned");
        node.markAsUpdated("system");
        
        // Set labels based on type
        List<String> labels = new ArrayList<>();
        if (type.equals("attraction") && cost > 0) {
            labels.add("Booking Required");
        }
        if (type.equals("accommodation")) {
            labels.add("Booked");
        }
        node.setLabels(labels);
        
        // Set tips
        NodeTips tips = new NodeTips(
            Arrays.asList("Best time to visit"),
            Arrays.asList("Popular destination")
        );
        node.setTips(tips);
        
        return node;
    }
    
    /**
     * Parse time string to Instant.
     */
    private Instant parseTimeToInstant(String timeStr) {
        try {
            String[] parts = timeStr.split(":");
            int hour = Integer.parseInt(parts[0]);
            int minute = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
            return LocalDate.now().atTime(hour, minute).toInstant(ZoneOffset.UTC);
        } catch (Exception e) {
            logger.warn("Failed to parse time '{}', using current time", timeStr);
            return Instant.now();
        }
    }
    
    /**
     * Generate all sample itineraries and save them.
     */
    public void generateAllSamples() {
        logger.info("Generating all sample itineraries");
        
        try {
            // Generate Barcelona
            NormalizedItinerary barcelona = generateBarcelonaSample();
            itineraryJsonService.createItinerary(barcelona);
            logger.info("Generated Barcelona sample itinerary");
            
            // Generate Paris
            NormalizedItinerary paris = generateParisSample();
            itineraryJsonService.createItinerary(paris);
            logger.info("Generated Paris sample itinerary");
            
            // Generate Tokyo
            NormalizedItinerary tokyo = generateTokyoSample();
            itineraryJsonService.createItinerary(tokyo);
            logger.info("Generated Tokyo sample itinerary");
            
            logger.info("All sample itineraries generated successfully");
            
        } catch (Exception e) {
            logger.error("Failed to generate sample itineraries", e);
            throw new RuntimeException("Failed to generate sample itineraries", e);
        }
    }
}
