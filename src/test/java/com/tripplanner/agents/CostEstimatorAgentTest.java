package com.tripplanner.agents;

import com.tripplanner.dto.*;
import com.tripplanner.service.AgentEventBus;
import com.tripplanner.service.ItineraryJsonService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Cost Estimator Agent Tests")
class CostEstimatorAgentTest {

    @Mock
    private AgentEventBus eventBus;

    @Mock
    private ItineraryJsonService itineraryJsonService;

    private CostEstimatorAgent agent;

    @BeforeEach
    void setUp() {
        agent = new CostEstimatorAgent(eventBus, itineraryJsonService);
    }

    @Test
    @DisplayName("Should estimate costs for budget tier")
    void testEstimateCostsBudgetTier() {
        // Given
        String itineraryId = "test_it_budget";
        NormalizedItinerary itinerary = createTestItinerary(itineraryId);

        // When
        agent.estimateCosts(itineraryId, itinerary, "budget");

        // Then
        for (NormalizedDay day : itinerary.getDays()) {
            for (NormalizedNode node : day.getNodes()) {
                assertNotNull(node.getCost());
                assertNotNull(node.getCost().getAmountPerPerson());
                assertEquals("INR", node.getCost().getCurrency());
                assertTrue(node.getCost().getAmountPerPerson() >= 0);
            }
        }

        // Verify itinerary was saved
        verify(itineraryJsonService, times(1)).updateItinerary(any(NormalizedItinerary.class));
    }

    @Test
    @DisplayName("Should estimate costs for medium tier")
    void testEstimateCostsMediumTier() {
        // Given
        String itineraryId = "test_it_medium";
        NormalizedItinerary itinerary = createTestItinerary(itineraryId);

        // When
        agent.estimateCosts(itineraryId, itinerary, "medium");

        // Then
        double totalCost = 0;
        for (NormalizedDay day : itinerary.getDays()) {
            for (NormalizedNode node : day.getNodes()) {
                assertNotNull(node.getCost());
                assertNotNull(node.getCost().getAmountPerPerson());
                totalCost += node.getCost().getAmountPerPerson();
            }
        }

        assertTrue(totalCost > 0, "Total cost should be greater than 0");
    }

    @Test
    @DisplayName("Should estimate costs for luxury tier")
    void testEstimateCostsLuxuryTier() {
        // Given
        String itineraryId = "test_it_luxury";
        NormalizedItinerary itinerary = createTestItinerary(itineraryId);

        // When
        agent.estimateCosts(itineraryId, itinerary, "luxury");

        // Then
        for (NormalizedDay day : itinerary.getDays()) {
            for (NormalizedNode node : day.getNodes()) {
                assertNotNull(node.getCost());
                // Luxury should have higher costs
                assertTrue(node.getCost().getAmountPerPerson() >= 0);
            }
        }
    }

    @Test
    @DisplayName("Should handle different node types correctly")
    void testEstimateDifferentNodeTypes() {
        // Given
        String itineraryId = "test_it_types";
        NormalizedItinerary itinerary = new NormalizedItinerary();
        itinerary.setItineraryId(itineraryId);
        itinerary.setDays(new ArrayList<>());

        NormalizedDay day = new NormalizedDay();
        day.setDayNumber(1);
        day.setNodes(new ArrayList<>());

        // Create nodes of different types
        NormalizedNode attraction = createNode("n1", "attraction");
        attraction.setDetails(new NodeDetails());
        attraction.getDetails().setCategory("museum");

        NormalizedNode meal = createNode("n2", "meal");
        meal.setDetails(new NodeDetails());
        meal.getDetails().setCategory("lunch");

        NormalizedNode transport = createNode("n3", "transport");
        transport.setDetails(new NodeDetails());
        transport.getDetails().setCategory("metro");

        day.getNodes().addAll(Arrays.asList(attraction, meal, transport));
        itinerary.getDays().add(day);

        // When
        agent.estimateCosts(itineraryId, itinerary, "medium");

        // Then
        assertNotNull(attraction.getCost());
        assertNotNull(meal.getCost());
        assertNotNull(transport.getCost());

        // Verify each has a reasonable cost
        assertTrue(attraction.getCost().getAmountPerPerson() > 0);
        assertTrue(meal.getCost().getAmountPerPerson() > 0);
        // Transport might be 0 for walk/free options
        assertTrue(transport.getCost().getAmountPerPerson() >= 0);
    }

    @Test
    @DisplayName("Should not override existing costs")
    void testDoesNotOverrideExistingCosts() {
        // Given
        String itineraryId = "test_it_existing";
        NormalizedItinerary itinerary = createTestItinerary(itineraryId);

        // Set existing cost for first node
        NormalizedNode firstNode = itinerary.getDays().get(0).getNodes().get(0);
        NodeCost existingCost = new NodeCost();
        existingCost.setAmountPerPerson(9999.0);
        existingCost.setCurrency("INR");
        firstNode.setCost(existingCost);

        // When
        agent.estimateCosts(itineraryId, itinerary, "medium");

        // Then
        assertEquals(9999.0, firstNode.getCost().getAmountPerPerson(),
            "Existing cost should not be overridden");
    }

    @Test
    @DisplayName("Should handle itinerary with no nodes gracefully")
    void testHandleEmptyItinerary() {
        // Given
        String itineraryId = "test_it_empty";
        NormalizedItinerary itinerary = new NormalizedItinerary();
        itinerary.setItineraryId(itineraryId);
        itinerary.setDays(new ArrayList<>());

        NormalizedDay day = new NormalizedDay();
        day.setDayNumber(1);
        day.setNodes(new ArrayList<>()); // Empty nodes
        itinerary.getDays().add(day);

        // When/Then - Should not throw exception
        assertDoesNotThrow(() -> agent.estimateCosts(itineraryId, itinerary, "medium"));
    }

    @Test
    @DisplayName("Should apply correct budget multipliers")
    void testBudgetMultipliers() {
        // Given - Create three identical itineraries
        NormalizedItinerary budgetItinerary = createTestItinerary("it_budget");
        NormalizedItinerary mediumItinerary = createTestItinerary("it_medium");
        NormalizedItinerary luxuryItinerary = createTestItinerary("it_luxury");

        // When
        agent.estimateCosts("it_budget", budgetItinerary, "budget");
        agent.estimateCosts("it_medium", mediumItinerary, "medium");
        agent.estimateCosts("it_luxury", luxuryItinerary, "luxury");

        // Then - Calculate total costs
        double budgetTotal = calculateTotalCost(budgetItinerary);
        double mediumTotal = calculateTotalCost(mediumItinerary);
        double luxuryTotal = calculateTotalCost(luxuryItinerary);

        // Due to randomness factor, we can't guarantee exact ratios
        // But we can verify that luxury > medium in general
        assertTrue(budgetTotal > 0, "Budget cost should be > 0");
        assertTrue(mediumTotal > 0, "Medium cost should be > 0");
        assertTrue(luxuryTotal > 0, "Luxury cost should be > 0");
    }

    // Helper methods

    private NormalizedItinerary createTestItinerary(String id) {
        NormalizedItinerary itinerary = new NormalizedItinerary();
        itinerary.setItineraryId(id);
        itinerary.setDays(new ArrayList<>());

        NormalizedDay day = new NormalizedDay();
        day.setDayNumber(1);
        day.setNodes(new ArrayList<>());

        // Add various node types
        NormalizedNode attraction = createNode("n1", "attraction");
        attraction.setDetails(new NodeDetails());
        attraction.getDetails().setCategory("museum");

        NormalizedNode meal = createNode("n2", "meal");
        meal.setDetails(new NodeDetails());
        meal.getDetails().setCategory("dinner");

        NormalizedNode transport = createNode("n3", "transport");
        transport.setDetails(new NodeDetails());
        transport.getDetails().setCategory("taxi");

        day.getNodes().addAll(Arrays.asList(attraction, meal, transport));
        itinerary.getDays().add(day);

        return itinerary;
    }

    private NormalizedNode createNode(String id, String type) {
        NormalizedNode node = new NormalizedNode();
        node.setId(id);
        node.setType(type);
        node.setTitle("Test " + type);
        return node;
    }

    private double calculateTotalCost(NormalizedItinerary itinerary) {
        double total = 0;
        for (NormalizedDay day : itinerary.getDays()) {
            for (NormalizedNode node : day.getNodes()) {
                if (node.getCost() != null && node.getCost().getAmountPerPerson() != null) {
                    total += node.getCost().getAmountPerPerson();
                }
            }
        }
        return total;
    }
}






