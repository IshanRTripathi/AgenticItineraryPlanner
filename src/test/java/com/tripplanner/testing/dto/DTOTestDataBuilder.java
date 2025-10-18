package com.tripplanner.testing.dto;

import com.tripplanner.dto.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

/**
 * Simple factory methods for creating test DTO objects.
 * Uses straightforward factory pattern without over-engineering.
 */
public class DTOTestDataBuilder {
    
    /**
     * Create a complete NormalizedItinerary with all required fields.
     */
    public static NormalizedItinerary createCompleteItinerary() {
        NormalizedItinerary itinerary = new NormalizedItinerary();
        itinerary.setItineraryId("test-itinerary-001");
        itinerary.setVersion(1);
        itinerary.setUserId("test-user-001");
        itinerary.setCreatedAt(System.currentTimeMillis());
        itinerary.setUpdatedAt(System.currentTimeMillis());
        itinerary.setSummary("Test itinerary summary");
        itinerary.setCurrency("USD");
        itinerary.setThemes(Arrays.asList("test", "theme"));
        itinerary.setOrigin("Test Origin");
        itinerary.setDestination("Test Destination");
        itinerary.setStartDate("2024-01-01");
        itinerary.setEndDate("2024-01-03");
        itinerary.setDays(new ArrayList<>());
        itinerary.setAgentData(new HashMap<>());
        itinerary.setRevisions(new ArrayList<>());
        itinerary.setChat(new ArrayList<>());
        return itinerary;
    }
    
    /**
     * Create a minimal NormalizedItinerary with only required fields.
     */
    public static NormalizedItinerary createMinimalItinerary() {
        NormalizedItinerary itinerary = new NormalizedItinerary();
        itinerary.setItineraryId("minimal-test-001");
        itinerary.setDays(new ArrayList<>());
        return itinerary;
    }
    
    /**
     * Create a complete NormalizedDay with all fields.
     */
    public static NormalizedDay createCompleteDay() {
        NormalizedDay day = new NormalizedDay();
        day.setDayNumber(1);
        day.setDate("2024-01-01");
        day.setLocation("Test Location");
        day.setWarnings(Arrays.asList("Test warning"));
        day.setNotes("Test notes");
        day.setPace("relaxed");
        day.setTotalDistance(25.0);
        day.setTotalCost(450.0);
        day.setTotalDuration(8.0);
        day.setTimeWindowStart("09:00");
        day.setTimeWindowEnd("21:00");
        day.setTimeZone("UTC");
        day.setNodes(new ArrayList<>());
        day.setEdges(new ArrayList<>());
        return day;
    }
    
    /**
     * Create a minimal NormalizedDay with only required fields.
     */
    public static NormalizedDay createMinimalDay() {
        NormalizedDay day = new NormalizedDay();
        day.setNodes(new ArrayList<>());
        return day;
    }
    
    /**
     * Create a complete NormalizedNode with all fields.
     */
    public static NormalizedNode createCompleteNode() {
        NormalizedNode node = new NormalizedNode("test-node-001", "attraction", "Test Node");
        node.setLabels(Arrays.asList("test", "label"));
        node.setLocked(false);
        node.setBookingRef("booking-123");
        node.setStatus("planned");
        node.setUpdatedBy("agent");
        node.setUpdatedAt(System.currentTimeMillis());
        node.setAgentData(new HashMap<>());
        return node;
    }
    
    /**
     * Create a complete NodeLocation with all fields.
     */
    public static NodeLocation createCompleteLocation() {
        Coordinates coords = new Coordinates(-8.7467, 115.1671);
        return new NodeLocation(
            "Test Location",
            "Test Address",
            coords,
            "place-id-123",
            "https://maps.google.com/?cid=123",
            4.5,
            "09:00",
            "18:00"
        );
    }
    
    /**
     * Create a complete NodeCost.
     */
    public static NodeCost createCompleteCost() {
        return new NodeCost(45.00, "USD");
    }
    
    /**
     * Create a complete NodeLinks with BookingInfo.
     */
    public static NodeLinks createCompleteLinks() {
        NodeLinks.BookingInfo bookingInfo = new NodeLinks.BookingInfo(
            "booking-ref-123",
            "BOOKED",
            "Booking details"
        );
        return new NodeLinks(bookingInfo);
    }
    
    /**
     * Create a complete AgentDataSection with sample data.
     */
    public static AgentDataSection createCompleteAgentData() {
        AgentDataSection agentData = new AgentDataSection();
        agentData.setAgentData("ENRICHMENT", "test-ENRICHMENT-data");
        agentData.setAgentData("booking", new HashMap<String, Object>());
        agentData.setAgentData("PLANNER", 42);
        return agentData;
    }
}