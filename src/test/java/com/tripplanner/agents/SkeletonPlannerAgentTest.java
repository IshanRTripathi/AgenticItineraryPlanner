package com.tripplanner.agents;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripplanner.dto.*;
import com.tripplanner.service.AgentEventBus;
import com.tripplanner.service.AgentEventPublisher;
import com.tripplanner.service.ItineraryJsonService;
import com.tripplanner.service.NodeIdGenerator;
import com.tripplanner.service.ai.AiClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Skeleton Planner Agent Tests")
class SkeletonPlannerAgentTest {

    @Mock
    private AgentEventBus eventBus;

    @Mock
    private AiClient aiClient;

    @Mock
    private ItineraryJsonService itineraryJsonService;

    @Mock
    private AgentEventPublisher agentEventPublisher;

    @Mock
    private NodeIdGenerator nodeIdGenerator;

    private ObjectMapper objectMapper;
    private SkeletonPlannerAgent agent;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        
        agent = new SkeletonPlannerAgent(
            eventBus, 
            aiClient, 
            objectMapper, 
            itineraryJsonService, 
            agentEventPublisher,
            nodeIdGenerator
        );
    }

    @Test
    @DisplayName("Should generate skeleton for 1-day trip")
    void testGenerateSkeleton1Day() throws Exception {
        // Given
        String itineraryId = "test_it_1day";
        CreateItineraryReq request = CreateItineraryReq.builder()
            .destination("Tokyo, Japan")
            .startDate(LocalDate.of(2025, 6, 1))
            .endDate(LocalDate.of(2025, 6, 1))
            .budgetTier("medium")
            .interests(Arrays.asList("culture", "food"))
            .build();

        // Mock AI response for 1 day (single day object, not array)
        String mockAiResponse = """
        {
          "dayNumber": 1,
          "date": "2025-06-01",
          "location": "Tokyo",
          "summary": "Day 1 in Tokyo",
          "nodes": [
            {"id": "d1_n1", "type": "attraction", "title": "Morning Activity"},
            {"id": "d1_n2", "type": "meal", "title": "Lunch"},
            {"id": "d1_n3", "type": "attraction", "title": "Afternoon Activity"},
            {"id": "d1_n4", "type": "meal", "title": "Dinner"}
          ]
        }
        """;

        when(aiClient.generateStructuredContent(anyString(), anyString(), anyString()))
            .thenReturn(mockAiResponse);
        when(agentEventPublisher.hasActiveConnections(anyString())).thenReturn(false);

        // When
        NormalizedItinerary result = agent.generateSkeleton(itineraryId, request);

        // Then
        assertNotNull(result);
        assertEquals(itineraryId, result.getItineraryId());
        assertEquals(1, result.getDays().size());
        assertEquals(4, result.getDays().get(0).getNodes().size());
        assertEquals("Tokyo", result.getDays().get(0).getLocation());
        
        // Verify itinerary was saved
        verify(itineraryJsonService, atLeastOnce()).updateItinerary(any(NormalizedItinerary.class));
    }

    @Test
    @DisplayName("Should generate skeleton for 3-day trip")
    void testGenerateSkeleton3Days() throws Exception {
        // Given
        String itineraryId = "test_it_3days";
        CreateItineraryReq request = CreateItineraryReq.builder()
            .destination("Paris, France")
            .startDate(LocalDate.of(2025, 7, 1))
            .endDate(LocalDate.of(2025, 7, 3))
            .budgetTier("luxury")
            .interests(Arrays.asList("art", "culture"))
            .build();

        // Mock AI response for multiple days (single day objects, no array wrapper)
        String mockDay1 = """
        {
          "dayNumber": 1,
          "date": "2025-07-01",
          "location": "Paris",
          "summary": "Day 1 in Paris",
          "nodes": [
            {"id": "d1_n1", "type": "attraction", "title": "Morning Activity"},
            {"id": "d1_n2", "type": "meal", "title": "Lunch"}
          ]
        }
        """;

        String mockDay2 = """
        {
          "dayNumber": 2,
          "date": "2025-07-02",
          "location": "Paris",
          "summary": "Day 2 in Paris",
          "nodes": [
            {"id": "d2_n1", "type": "attraction", "title": "Morning Activity"},
            {"id": "d2_n2", "type": "meal", "title": "Lunch"}
          ]
        }
        """;

        String mockDay3 = """
        {
          "dayNumber": 3,
          "date": "2025-07-03",
          "location": "Paris",
          "summary": "Day 3 in Paris",
          "nodes": [
            {"id": "d3_n1", "type": "attraction", "title": "Morning Activity"},
            {"id": "d3_n2", "type": "meal", "title": "Lunch"}
          ]
        }
        """;

        when(aiClient.generateStructuredContent(anyString(), anyString(), anyString()))
            .thenReturn(mockDay1, mockDay2, mockDay3);
        when(agentEventPublisher.hasActiveConnections(anyString())).thenReturn(false);

        // When
        NormalizedItinerary result = agent.generateSkeleton(itineraryId, request);

        // Then
        assertNotNull(result);
        assertEquals(3, result.getDays().size());
        assertEquals("Paris", result.getDays().get(0).getLocation());
        
        // Verify AI was called 3 times (batch size = 1)
        verify(aiClient, times(3)).generateStructuredContent(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Should handle AI failure gracefully")
    void testHandleAiFailure() {
        // Given
        String itineraryId = "test_it_fail";
        CreateItineraryReq request = CreateItineraryReq.builder()
            .destination("London, UK")
            .startDate(LocalDate.of(2025, 8, 1))
            .endDate(LocalDate.of(2025, 8, 1))
            .build();

        when(aiClient.generateStructuredContent(anyString(), anyString(), anyString()))
            .thenThrow(new RuntimeException("AI service unavailable"));

        // When/Then
        assertThrows(RuntimeException.class, () -> {
            agent.generateSkeleton(itineraryId, request);
        });
    }

    @Test
    @DisplayName("Should generate skeleton with correct structure")
    void testSkeletonStructure() throws Exception {
        // Given
        String itineraryId = "test_it_structure";
        CreateItineraryReq request = CreateItineraryReq.builder()
            .destination("Rome, Italy")
            .startDate(LocalDate.of(2025, 9, 1))
            .endDate(LocalDate.of(2025, 9, 1))
            .budgetTier("budget")
            .build();

        String mockAiResponse = """
        {
          "dayNumber": 1,
          "date": "2025-09-01",
          "location": "Rome",
          "summary": "Exploring Rome",
          "nodes": [
            {"id": "n1", "type": "attraction", "title": "Colosseum Visit"},
            {"id": "n2", "type": "meal", "title": "Lunch"},
            {"id": "n3", "type": "transport", "title": "Metro"}
          ]
        }
        """;

        when(aiClient.generateStructuredContent(anyString(), anyString(), anyString()))
            .thenReturn(mockAiResponse);
        when(agentEventPublisher.hasActiveConnections(anyString())).thenReturn(false);

        // When
        NormalizedItinerary result = agent.generateSkeleton(itineraryId, request);

        // Then
        assertNotNull(result);
        assertNotNull(result.getItineraryId());
        assertNotNull(result.getDays());
        assertNotNull(result.getCreatedAt());
        assertNotNull(result.getUpdatedAt());
        
        NormalizedDay day = result.getDays().get(0);
        assertEquals(1, day.getDayNumber());
        assertEquals("Rome", day.getLocation());
        assertEquals(3, day.getNodes().size());
    }
}

