package com.tripplanner.testing.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripplanner.agents.BaseAgent;
import com.tripplanner.agents.DayByDayPlannerAgent;
import com.tripplanner.dto.*;
import com.tripplanner.service.AgentEventBus;
import com.tripplanner.service.ItineraryJsonService;
import com.tripplanner.service.SummarizationService;
import com.tripplanner.service.ai.AiClient;
import com.tripplanner.testing.MockLLMProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test cases for DayByDayPlannerAgent focusing on token limit management
 * and location duplication prevention.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("DayByDayPlannerAgent Tests")
public class DayByDayPlannerAgentTest {
    
    @Mock
    private AgentEventBus mockEventBus;
    
    @Mock
    private ItineraryJsonService mockItineraryJsonService;
    
    @Mock
    private SummarizationService mockSummarizationService;
    
    @Mock
    private AiClient mockAiClient;
    
    private ObjectMapper objectMapper;
    private DayByDayPlannerAgent agent;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        objectMapper = new ObjectMapper();
        
        agent = new DayByDayPlannerAgent(
            mockEventBus, 
            mockAiClient, 
            objectMapper, 
            mockItineraryJsonService,
            mockSummarizationService,
            mock(com.tripplanner.service.AgentEventPublisher.class)
        );
    }
    
    @Test
    @DisplayName("Should have correct enum-based capabilities")
    void shouldHaveCorrectEnumBasedCapabilities() {
        AgentCapabilities capabilities = agent.getCapabilities();
        
        // Zero-overlap design: only "plan" task type
        assertNotNull(capabilities);
        assertEquals(1, capabilities.getSupportedTasks().size());
        assertTrue(capabilities.getSupportedTasks().contains("plan"));
        
        // Chat-enabled for user-facing planning
        assertTrue(capabilities.isChatEnabled());
        
        // High priority for planning
        assertEquals(5, capabilities.getPriority());
        
        // Should have proper configuration
        assertEquals(2, capabilities.getConfigurationValue("maxDaysPerBatch")); // Currently set to 2 for balance
        assertEquals(8000, capabilities.getConfigurationValue("tokenLimit"));
    }
    
    @Test
    @DisplayName("Should create itinerary in batches for long trips")
    void shouldCreateItineraryInBatchesForLongTrips() {
        // Setup: 7-day trip (should be processed in 3 batches: 3+3+1)
        String itineraryId = "test_long_trip";
        CreateItineraryReq request = createTestRequest("Tokyo, Japan", 7);
        
        // Mock summarization service
        when(mockSummarizationService.summarizeDays(any())).thenReturn("Summary of previous days");
        
        // Mock AI responses for each batch
        setupMockAiResponsesForBatches();
        
        // Execute
        BaseAgent.AgentRequest<NormalizedItinerary> agentRequest = 
            new BaseAgent.AgentRequest<>(request, NormalizedItinerary.class);
        
        NormalizedItinerary result = agent.execute(itineraryId, agentRequest);
        
        // Verify
        assertNotNull(result);
        assertTrue(result.getDays().size() >= 7, "Should have at least 7 days, got: " + result.getDays().size());
        
        // Verify summarization was called for context management
        verify(mockSummarizationService, atLeast(1)).summarizeDays(any());
        
        // Verify progress events were emitted
        verify(mockEventBus, atLeast(5)).publish(eq(itineraryId), any(AgentEvent.class));
    }
    
    @Test
    @DisplayName("Should avoid location duplication across days")
    void shouldAvoidLocationDuplicationAcrossDays() {
        String itineraryId = "test_no_duplication";
        CreateItineraryReq request = createTestRequest("Paris, France", 5);
        
        // Mock AI responses with potential duplicates
        setupMockAiResponsesWithDuplicates();
        
        BaseAgent.AgentRequest<NormalizedItinerary> agentRequest = 
            new BaseAgent.AgentRequest<>(request, NormalizedItinerary.class);
        
        NormalizedItinerary result = agent.execute(itineraryId, agentRequest);
        
        // Collect all location names
        Set<String> allLocations = new HashSet<>();
        Set<String> duplicateLocations = new HashSet<>();
        
        for (NormalizedDay day : result.getDays()) {
            for (NormalizedNode node : day.getNodes()) {
                String locationName = node.getLocation().getName().toLowerCase();
                if (allLocations.contains(locationName)) {
                    duplicateLocations.add(locationName);
                }
                allLocations.add(locationName);
            }
        }
        
        // Should have minimal duplicates (only accommodation might repeat)
        assertTrue(duplicateLocations.size() <= 1, 
            "Should have minimal location duplicates, found: " + duplicateLocations);
    }
    
    @Test
    @DisplayName("Should handle token limit constraints properly")
    void shouldHandleTokenLimitConstraintsProperly() {
        String itineraryId = "test_token_limits";
        CreateItineraryReq request = createTestRequest("India Golden Triangle", 10); // Long trip
        
        // Mock summarization to return concise summaries
        when(mockSummarizationService.summarizeDays(any()))
            .thenReturn("Day summary: visited 3 attractions, 2 restaurants, stayed at hotel");
        
        setupMockAiResponsesForBatches();
        
        BaseAgent.AgentRequest<NormalizedItinerary> agentRequest = 
            new BaseAgent.AgentRequest<>(request, NormalizedItinerary.class);
        
        NormalizedItinerary result = agent.execute(itineraryId, agentRequest);
        
        // Verify result
        assertNotNull(result);
        assertTrue(result.getDays().size() >= 10, "Should have at least 10 days, got: " + result.getDays().size());
        
        // Verify summarization was used to manage context
        verify(mockSummarizationService, atLeast(2)).summarizeDays(any());
        
        // Each day should have reasonable number of nodes
        for (NormalizedDay day : result.getDays()) {
            assertTrue(day.getNodes().size() >= 3, 
                "Day " + day.getDayNumber() + " should have at least 3 nodes");
            assertTrue(day.getNodes().size() <= 8, 
                "Day " + day.getDayNumber() + " should have at most 8 nodes");
        }
    }
    
    @Test
    @DisplayName("Should maintain context across day batches")
    void shouldMaintainContextAcrossDayBatches() {
        String itineraryId = "test_context_management";
        CreateItineraryReq request = createTestRequest("Thailand", 8);
        
        // Track summarization calls
        List<String> summaries = new ArrayList<>();
        when(mockSummarizationService.summarizeDays(any())).thenAnswer(invocation -> {
            String summary = "Summary " + (summaries.size() + 1);
            summaries.add(summary);
            return summary;
        });
        
        setupMockAiResponsesForBatches();
        
        BaseAgent.AgentRequest<NormalizedItinerary> agentRequest = 
            new BaseAgent.AgentRequest<>(request, NormalizedItinerary.class);
        
        NormalizedItinerary result = agent.execute(itineraryId, agentRequest);
        
        // Verify context management
        assertNotNull(result);
        assertTrue(result.getDays().size() >= 8, "Should have at least 8 days, got: " + result.getDays().size());
        
        // Should have created summaries for context management
        assertTrue(summaries.size() >= 1, "Should have created summaries for context");
        
        // Verify summarization service was called appropriately
        verify(mockSummarizationService, atLeast(1)).summarizeDays(any());
    }
    
    @Test
    @DisplayName("Should handle single day planning correctly")
    void shouldHandleSingleDayPlanningCorrectly() {
        String itineraryId = "test_single_day";
        CreateItineraryReq request = createTestRequest("Mumbai, India", 1);
        
        // Setup mock for single day
        when(mockAiClient.generateStructuredContent(anyString(), anyString(), anyString()))
            .thenReturn(createMockDayBatchResponse(1, 1));
        
        BaseAgent.AgentRequest<NormalizedItinerary> agentRequest = 
            new BaseAgent.AgentRequest<>(request, NormalizedItinerary.class);
        
        NormalizedItinerary result = agent.execute(itineraryId, agentRequest);
        
        // Verify single day handling
        assertNotNull(result);
        assertEquals(1, result.getDays().size());
        
        NormalizedDay day = result.getDays().get(0);
        assertEquals(1, day.getDayNumber());
        assertTrue(day.getNodes().size() >= 3, "Single day should have at least 3 nodes");
        
        // Should not call summarization for single day
        verify(mockSummarizationService, never()).summarizeDays(any());
    }
    
    @Test
    @DisplayName("Should emit progress events during execution")
    void shouldEmitProgressEventsDuringExecution() {
        String itineraryId = "test_progress_events";
        CreateItineraryReq request = createTestRequest("Bali, Indonesia", 4);
        
        setupMockAiResponsesForBatches();
        
        BaseAgent.AgentRequest<NormalizedItinerary> agentRequest = 
            new BaseAgent.AgentRequest<>(request, NormalizedItinerary.class);
        
        agent.execute(itineraryId, agentRequest);
        
        // Verify progress events were emitted
        verify(mockEventBus, atLeast(3)).publish(eq(itineraryId), any(AgentEvent.class));
        
        // Verify that some progress events were emitted (more flexible verification)
        verify(mockEventBus, atLeastOnce()).publish(eq(itineraryId), argThat(event -> 
            event.progress() != null && event.progress() > 0));
        verify(mockEventBus, atLeastOnce()).publish(eq(itineraryId), argThat(event -> 
            event.progress() != null && event.progress() == 100));
    }
    
    @Test
    @DisplayName("Should handle AI service failures gracefully")
    void shouldHandleAiServiceFailuresGracefully() {
        String itineraryId = "test_ai_failure";
        CreateItineraryReq request = createTestRequest("Sydney, Australia", 3);
        
        // Mock AI client to throw exception
        AiClient failingAiClient = mock(AiClient.class);
        when(failingAiClient.generateStructuredContent(any(), any(), any()))
            .thenThrow(new RuntimeException("AI service unavailable"));
        
        DayByDayPlannerAgent failingAgent = new DayByDayPlannerAgent(
            mockEventBus, failingAiClient, objectMapper, mockItineraryJsonService, mockSummarizationService,
            mock(com.tripplanner.service.AgentEventPublisher.class));
        
        BaseAgent.AgentRequest<NormalizedItinerary> agentRequest = 
            new BaseAgent.AgentRequest<>(request, NormalizedItinerary.class);
        
        // Should throw exception with meaningful message
        RuntimeException exception = assertThrows(RuntimeException.class, () -> 
            failingAgent.execute(itineraryId, agentRequest));
        
        assertTrue(exception.getMessage().contains("Failed to create itinerary"));
    }
    
    private CreateItineraryReq createTestRequest(String destination, int days) {
        PartyDto party = PartyDto.builder()
            .adults(2)
            .children(0)
            .infants(0)
            .rooms(1)
            .build();
        
        java.time.LocalDate startDate = java.time.LocalDate.parse("2025-06-01");
        java.time.LocalDate endDate = startDate.plusDays(days); // Add days to get the correct duration
        
        return CreateItineraryReq.builder()
            .destination(destination)
            .startDate(startDate)
            .endDate(endDate)
            .budgetTier("medium")
            .language("en")
            .interests(Arrays.asList("culture", "food", "sightseeing"))
            .constraints(new ArrayList<>())
            .party(party)
            .build();
    }
    
    private void setupMockAiResponsesForBatches() {
        // Mock AI responses for different batch sizes - return different responses for different calls
        when(mockAiClient.generateStructuredContent(anyString(), anyString(), anyString()))
            .thenReturn(createMockDayBatchResponse(1, 3))   // First call: days 1-3
            .thenReturn(createMockDayBatchResponse(4, 6))   // Second call: days 4-6  
            .thenReturn(createMockDayBatchResponse(7, 9))   // Third call: days 7-9
            .thenReturn(createMockDayBatchResponse(10, 10)); // Fourth call: day 10 (for very long trips)
    }
    
    private void setupMockAiResponsesWithDuplicates() {
        // Mock responses that might contain duplicate locations
        when(mockAiClient.generateStructuredContent(anyString(), anyString(), anyString()))
            .thenReturn(createMockResponseWithPotentialDuplicates(1, 3))
            .thenReturn(createMockResponseWithPotentialDuplicates(4, 5));
    }
    
    private String createMockDayBatchResponse() {
        return createMockDayBatchResponse(1, 1); // Default single day
    }
    
    private String createMockDayBatchResponse(int startDay, int endDay) {
        StringBuilder response = new StringBuilder("{\"days\": [");
        
        for (int day = startDay; day <= endDay; day++) {
            if (day > startDay) response.append(",");
            
            // Calculate timestamps for the specific day
            long baseTime = java.time.LocalDate.of(2025, 6, day)
                .atTime(9, 0)
                .atZone(java.time.ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli();
            
            response.append(String.format("""
                {
                  "dayNumber": %d,
                  "date": "2025-06-%02d",
                  "location": "Tokyo District %d",
                  "summary": "Day %d exploration",
                  "nodes": [
                    {
                      "id": "node_%d_1",
                      "type": "attraction",
                      "title": "Morning Activity Day %d",
                      "location": {
                        "name": "Attraction %d",
                        "address": "Tokyo District %d",
                        "coordinates": { "lat": 35.7188, "lng": 139.7753 }
                      },
                      "timing": {
                        "startTime": %d,
                        "endTime": %d,
                        "durationMin": 150
                      },
                      "cost": {
                        "amountPerPerson": 1000,
                        "currency": "INR"
                      }
                    },
                    {
                      "id": "node_%d_2",
                      "type": "meal",
                      "title": "Lunch Day %d",
                      "location": {
                        "name": "Restaurant %d",
                        "address": "Tokyo Food District %d"
                      },
                      "timing": {
                        "startTime": %d,
                        "endTime": %d,
                        "durationMin": 60
                      },
                      "cost": {
                        "amountPerPerson": 2500,
                        "currency": "INR"
                      }
                    },
                    {
                      "id": "node_%d_3",
                      "type": "attraction",
                      "title": "Evening Activity Day %d",
                      "location": {
                        "name": "Evening Spot %d",
                        "address": "Tokyo Evening District %d"
                      },
                      "timing": {
                        "startTime": %d,
                        "endTime": %d,
                        "durationMin": 90
                      },
                      "cost": {
                        "amountPerPerson": 500,
                        "currency": "INR"
                      }
                    }
                  ]
                }
                """, day, day, day, day, day, day, day, day, baseTime, baseTime + 9000000L, 
                day, day, day, day, baseTime + 10800000L, baseTime + 14400000L,
                day, day, day, day, baseTime + 18000000L, baseTime + 23400000L));
        }
        
        response.append("]}");
        return response.toString();
    }
    
    private String createMockResponseWithPotentialDuplicates() {
        return createMockResponseWithPotentialDuplicates(1, 2);
    }
    
    private String createMockResponseWithPotentialDuplicates(int startDay, int endDay) {
        StringBuilder response = new StringBuilder("{\"days\": [");
        
        // Locations that might be duplicated
        String[] locations = {"Eiffel Tower", "Louvre Museum", "Notre Dame", "Arc de Triomphe", "Champs Elysees"};
        String[] restaurants = {"Cafe de Flore", "Le Comptoir du Relais", "L'Ami Jean", "Bistrot Paul Bert"};
        
        for (int day = startDay; day <= endDay; day++) {
            if (day > startDay) response.append(",");
            
            // Intentionally reuse some locations to test duplication detection
            String attraction = locations[(day - 1) % locations.length];
            String restaurant = restaurants[(day - 1) % restaurants.length];
            
            response.append(String.format("""
                {
                  "dayNumber": %d,
                  "date": "2025-06-%02d",
                  "location": "Paris Center",
                  "nodes": [
                    {
                      "id": "node_%d_1",
                      "type": "attraction",
                      "title": "%s",
                      "location": { "name": "%s", "address": "Paris Address %d" }
                    },
                    {
                      "id": "node_%d_2",
                      "type": "meal",
                      "title": "%s",
                      "location": { "name": "%s", "address": "Paris Restaurant District %d" }
                    },
                    {
                      "id": "node_%d_3",
                      "type": "attraction",
                      "title": "Unique Activity Day %d",
                      "location": { "name": "Unique Location Day %d", "address": "Unique Address %d" }
                    }
                  ]
                }
                """, day, day, day, attraction, attraction, day, day, restaurant, restaurant, day, day, day, day, day));
        }
        
        response.append("]}");
        return response.toString();
    }
}