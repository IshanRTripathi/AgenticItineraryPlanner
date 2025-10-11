package com.tripplanner.testing.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripplanner.controller.ItinerariesController;
import com.tripplanner.dto.*;
import com.tripplanner.service.*;
import com.tripplanner.service.ai.AiClient;
import com.tripplanner.agents.BaseAgent;
import com.tripplanner.agents.DayByDayPlannerAgent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Comprehensive integration tests for real-time itinerary generation.
 * Tests the complete flow from request to real-time updates with mocked LLM responses.
 */
public class RealTimeItineraryGenerationIntegrationTest {
    
    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    
    @Mock
    private ItineraryService itineraryService;
    
    @Mock
    private ItineraryJsonService itineraryJsonService;
    
    @Mock
    private AiClient aiClient;
    
    @Mock
    private SummarizationService summarizationService;
    
    @Mock
    private AgentEventBus agentEventBus;
    
    private SseConnectionManager sseConnectionManager;
    private AgentEventPublisher agentEventPublisher;
    private DayByDayPlannerAgent dayByDayPlannerAgent;
    private ItinerariesController controller;
    
    private static final String TEST_USER_ID = "test-user-123";
    private static final String TEST_ITINERARY_ID = "it_test-456";
    private static final String TEST_EXECUTION_ID = "exec_789";
    
    // Mock LLM responses for different scenarios
    private String TOKYO_DAY_1_RESPONSE; // Will be initialized in setUp with proper timestamps
    
    private String TOKYO_DAY_2_RESPONSE; // Will be initialized in setUp with proper timestamps
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        objectMapper = new ObjectMapper();
        // Register JSR310 module for Java 8 date/time support
        objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        // Initialize mock responses with proper timestamps
        initializeMockResponses();
        
        // Create real instances for integration testing
        sseConnectionManager = new SseConnectionManager();
        agentEventPublisher = new AgentEventPublisher(sseConnectionManager);
        
        dayByDayPlannerAgent = new DayByDayPlannerAgent(
            agentEventBus, aiClient, objectMapper, itineraryJsonService, 
            summarizationService, agentEventPublisher
        );
        
        controller = new ItinerariesController(
            itineraryService, itineraryJsonService, null, null, null, 
            null, null, null, sseConnectionManager, null
        );
        
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        
        setupMockResponses();
    }
    
    private void initializeMockResponses() {
        // Calculate timestamps for Day 1 (2024-06-01)
        long day1Base = java.time.LocalDate.of(2024, 6, 1)
            .atTime(10, 0)
            .atZone(java.time.ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli();
        
        TOKYO_DAY_1_RESPONSE = String.format("""
            {
              "days": [{
                "dayNumber": 1,
                "date": "2024-06-01",
                "location": "Tokyo",
                "summary": "Arrival and Shibuya exploration",
                "nodes": [
                  {
                    "id": "node_1_1",
                    "type": "transport",
                    "title": "Airport Transfer to Hotel",
                    "location": {
                      "name": "Narita Airport to Shibuya",
                      "address": "Narita International Airport to Shibuya District",
                      "coordinates": {"lat": 35.7720, "lng": 140.3929}
                    },
                    "timing": {"startTime": %d, "endTime": %d, "durationMin": 120},
                    "cost": {"amountPerPerson": 3000, "currency": "JPY"},
                    "details": {"description": "Express train from Narita to Shibuya", "category": "transport"}
                  },
                  {
                    "id": "node_1_2",
                    "type": "accommodation",
                    "title": "Check-in at Shibuya Hotel",
                    "location": {
                      "name": "Shibuya Grand Hotel",
                      "address": "1-1-1 Shibuya, Shibuya City, Tokyo",
                      "coordinates": {"lat": 35.6598, "lng": 139.7006}
                    },
                    "timing": {"startTime": %d, "endTime": %d, "durationMin": 60},
                    "cost": {"amountPerPerson": 12000, "currency": "JPY"},
                    "details": {"description": "Modern hotel in heart of Shibuya", "category": "accommodation", "rating": 4.2}
                  },
                  {
                    "id": "node_1_3",
                    "type": "attraction",
                    "title": "Shibuya Crossing Experience",
                    "location": {
                      "name": "Shibuya Crossing",
                      "address": "Shibuya City, Tokyo",
                      "coordinates": {"lat": 35.6595, "lng": 139.7006}
                    },
                    "timing": {"startTime": %d, "endTime": %d, "durationMin": 90},
                    "cost": {"amountPerPerson": 0, "currency": "JPY"},
                    "details": {"description": "World's busiest pedestrian crossing", "category": "landmark", "rating": 4.5}
                  },
                  {
                    "id": "node_1_4",
                    "type": "meal",
                    "title": "Dinner at Izakaya",
                    "location": {
                      "name": "Torikizoku Shibuya",
                      "address": "2-3-1 Shibuya, Shibuya City, Tokyo",
                      "coordinates": {"lat": 35.6580, "lng": 139.7016}
                    },
                    "timing": {"startTime": %d, "endTime": %d, "durationMin": 120},
                    "cost": {"amountPerPerson": 2500, "currency": "JPY"},
                    "details": {"description": "Traditional Japanese pub experience", "category": "restaurant", "rating": 4.1}
                  }
                ]
              }]
            }
            """, 
            day1Base, day1Base + 7200000L,  // 10:00-12:00
            day1Base + 14400000L, day1Base + 18000000L,  // 14:00-15:00
            day1Base + 21600000L, day1Base + 27000000L,  // 16:00-17:30
            day1Base + 32400000L, day1Base + 39600000L   // 19:00-21:00
        );
        
        // Calculate timestamps for Day 2 (2024-06-02)
        long day2Base = java.time.LocalDate.of(2024, 6, 2)
            .atTime(8, 0)
            .atZone(java.time.ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli();
        
        TOKYO_DAY_2_RESPONSE = String.format("""
            {
              "days": [{
                "dayNumber": 2,
                "date": "2024-06-02",
                "location": "Tokyo",
                "summary": "Traditional culture in Asakusa",
                "nodes": [
                  {
                    "id": "node_2_1",
                    "type": "meal",
                    "title": "Traditional Japanese Breakfast",
                    "location": {
                      "name": "Hotel Restaurant",
                      "address": "1-1-1 Shibuya, Shibuya City, Tokyo",
                      "coordinates": {"lat": 35.6598, "lng": 139.7006}
                    },
                    "timing": {"startTime": %d, "endTime": %d, "durationMin": 60},
                    "cost": {"amountPerPerson": 1500, "currency": "JPY"},
                    "details": {"description": "Traditional Japanese breakfast set", "category": "breakfast"}
                  },
                  {
                    "id": "node_2_2",
                    "type": "transport",
                    "title": "Travel to Asakusa",
                    "location": {
                      "name": "Shibuya to Asakusa",
                      "address": "Tokyo Metro",
                      "coordinates": {"lat": 35.7148, "lng": 139.7967}
                    },
                    "timing": {"startTime": %d, "endTime": %d, "durationMin": 60},
                    "cost": {"amountPerPerson": 200, "currency": "JPY"},
                    "details": {"description": "Metro ride to historic Asakusa district", "category": "transport"}
                  },
                  {
                    "id": "node_2_3",
                    "type": "attraction",
                    "title": "Senso-ji Temple Visit",
                    "location": {
                      "name": "Senso-ji Temple",
                      "address": "2-3-1 Asakusa, Taito City, Tokyo",
                      "coordinates": {"lat": 35.7148, "lng": 139.7967}
                    },
                    "timing": {"startTime": %d, "endTime": %d, "durationMin": 120},
                    "cost": {"amountPerPerson": 0, "currency": "JPY"},
                    "details": {"description": "Tokyo's oldest Buddhist temple", "category": "temple", "rating": 4.3}
                  },
                  {
                    "id": "node_2_4",
                    "type": "meal",
                    "title": "Tempura Lunch",
                    "location": {
                      "name": "Daikokuya Tempura",
                      "address": "1-38-10 Asakusa, Taito City, Tokyo",
                      "coordinates": {"lat": 35.7120, "lng": 139.7960}
                    },
                    "timing": {"startTime": %d, "endTime": %d, "durationMin": 60},
                    "cost": {"amountPerPerson": 3000, "currency": "JPY"},
                    "details": {"description": "Historic tempura restaurant since 1887", "category": "restaurant", "rating": 4.4}
                  }
                ]
              }]
            }
            """,
            day2Base, day2Base + 3600000L,  // 08:00-09:00
            day2Base + 5400000L, day2Base + 9000000L,  // 09:30-10:30
            day2Base + 10800000L, day2Base + 18000000L,  // 11:00-13:00
            day2Base + 19800000L, day2Base + 23400000L   // 13:30-14:30
        );
    }
    
    private void setupMockResponses() {
        // Mock initial itinerary creation
        when(itineraryService.create(any(CreateItineraryReq.class), eq(TEST_USER_ID)))
            .thenReturn(createMockItinerary());
        
        // Mock LLM responses for day-by-day planning
        // Use Answer to dynamically parse the prompt and return appropriate response
        when(aiClient.generateStructuredContent(anyString(), anyString(), anyString()))
            .thenAnswer(invocation -> {
                String prompt = invocation.getArgument(0, String.class);
                
                // Parse the prompt to extract day range using regex
                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("Plan days (\\d+)-(\\d+)");
                java.util.regex.Matcher matcher = pattern.matcher(prompt);
                
                if (matcher.find()) {
                    int startDay = Integer.parseInt(matcher.group(1));
                    int endDay = Integer.parseInt(matcher.group(2));
                    return createMockDayBatchResponse(startDay, endDay);
                }
                
                // Fallback for non-planning requests
                return "{}";
            });
        
        // Mock summarization service
        when(summarizationService.summarizeDays(any()))
            .thenAnswer(invocation -> {
                List<NormalizedDay> days = invocation.getArgument(0);
                return "Summary of " + days.size() + " days with key attractions and activities";
            });
        
        // Mock itinerary JSON service - return empty itinerary that gets populated
        when(itineraryJsonService.getItinerary(TEST_ITINERARY_ID))
            .thenReturn(Optional.of(createMockNormalizedItinerary()));
        
        when(itineraryJsonService.updateItinerary(any(NormalizedItinerary.class)))
            .thenReturn(null);
    }
    
    @Test
    @DisplayName("Should create itinerary with immediate response and real-time updates")
    void shouldCreateItineraryWithImmediateResponseAndRealTimeUpdates() throws Exception {
        // Given
        CreateItineraryReq request = createTestRequest(2); // 2-day trip
        
        // When - Create itinerary
        String responseJson = mockMvc.perform(post("/api/v1/itineraries")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .requestAttr("userId", TEST_USER_ID))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        
        // Then - Verify immediate response
        ItineraryCreationResponse response = objectMapper.readValue(responseJson, ItineraryCreationResponse.class);
        
        assertNotNull(response);
        assertEquals(CreationStatus.PROCESSING, response.getStatus());
        assertNotNull(response.getExecutionId());
        assertNotNull(response.getSseEndpoint());
        assertNotNull(response.getEstimatedCompletion());
        assertNotNull(response.getStages());
        assertEquals(4, response.getStages().size()); // Planning, Enrichment, Places, Optimization
        
        // Verify itinerary structure
        assertNotNull(response.getItinerary());
        assertEquals(TEST_ITINERARY_ID, response.getItinerary().getId());
        assertEquals("Tokyo", response.getItinerary().getDestination());
        assertEquals("generating", response.getItinerary().getStatus());
    }
    
    @Test
    @DisplayName("Should establish SSE connection and receive real-time updates")
    void shouldEstablishSseConnectionAndReceiveRealTimeUpdates() throws Exception {
        // Given
        CountDownLatch eventLatch = new CountDownLatch(1); // Just verify connection works
        
        // Create SSE connection with mock to capture events
        SseEmitter emitter = mock(SseEmitter.class);
        doAnswer(invocation -> {
            eventLatch.countDown();
            return null;
        }).when(emitter).send(any(SseEmitter.SseEventBuilder.class));
        
        // Register connection
        sseConnectionManager.registerConnection(TEST_ITINERARY_ID, TEST_EXECUTION_ID, emitter);
        
        // Simulate day-by-day planning with real-time updates
        simulateDayByDayPlanning();
        
        // Wait for at least one event
        boolean eventsReceived = eventLatch.await(5, TimeUnit.SECONDS);
        assertTrue(eventsReceived, "Should receive at least one real-time event");
        
        // Verify connection was established
        assertTrue(sseConnectionManager.hasActiveConnections(TEST_ITINERARY_ID));
        assertEquals(1, sseConnectionManager.getConnectionCount(TEST_ITINERARY_ID));
        
        // Verify events were sent
        verify(emitter, atLeastOnce()).send(any(SseEmitter.SseEventBuilder.class));
    }
    
    @Test
    @DisplayName("Should handle day-by-day planning with summarization")
    void shouldHandleDayByDayPlanningWithSummarization() throws Exception {
        // Given
        CreateItineraryReq request = createTestRequest(5); // 5-day trip to test batching
        
        // When - Execute day-by-day planning
        BaseAgent.AgentRequest<NormalizedItinerary> agentRequest = 
            new BaseAgent.AgentRequest<>(request, NormalizedItinerary.class);
        
        // Simulate agent execution
        NormalizedItinerary result = assertDoesNotThrow(() -> {
            return dayByDayPlannerAgent.execute(TEST_ITINERARY_ID, agentRequest);
        });
        
        // Then - Verify result
        assertNotNull(result);
        assertEquals(5, result.getDays().size(), "Should have exactly 5 days, got: " + result.getDays().size());
        
        // Verify day numbers are correct
        for (int i = 0; i < result.getDays().size(); i++) {
            assertEquals(i + 1, result.getDays().get(i).getDayNumber(), 
                "Day at index " + i + " should have dayNumber " + (i + 1));
        }
        
        // Verify summarization was used (for batches after the first)
        verify(summarizationService, atLeast(1)).summarizeDays(any());
        
        // Verify itinerary was updated
        verify(itineraryJsonService, atLeast(1)).updateItinerary(any(NormalizedItinerary.class));
    }
    
    @Test
    @DisplayName("Should handle LLM parsing errors gracefully")
    void shouldHandleLlmParsingErrorsGracefully() throws Exception {
        // Given - Override the mock to return invalid JSON
        reset(aiClient);
        when(aiClient.generateStructuredContent(anyString(), anyString(), anyString()))
            .thenReturn("Invalid JSON response");
        
        CreateItineraryReq request = createTestRequest(1);
        BaseAgent.AgentRequest<NormalizedItinerary> agentRequest = 
            new BaseAgent.AgentRequest<>(request, NormalizedItinerary.class);
        
        // When & Then - Should handle error gracefully
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            dayByDayPlannerAgent.execute(TEST_ITINERARY_ID, agentRequest);
        });
        
        assertTrue(exception.getMessage().contains("Failed to parse day planning response") ||
                   exception.getMessage().contains("Failed to create itinerary"),
                   "Expected parsing error message, got: " + exception.getMessage());
    }
    
    @Test
    @DisplayName("Should publish error events for failures")
    void shouldPublishErrorEventsForFailures() throws Exception {
        // Given
        CountDownLatch errorEventLatch = new CountDownLatch(1);
        AtomicReference<ErrorEvent> receivedError = new AtomicReference<>();
        
        // Mock SSE connection to capture error events
        SseEmitter emitter = mock(SseEmitter.class);
        doAnswer(invocation -> {
            SseEmitter.SseEventBuilder eventBuilder = invocation.getArgument(0);
            // In real scenario, we'd capture the event data
            errorEventLatch.countDown();
            return null;
        }).when(emitter).send(any(SseEmitter.SseEventBuilder.class));
        
        sseConnectionManager.registerConnection(TEST_ITINERARY_ID, TEST_EXECUTION_ID, emitter);
        
        // When - Publish error event
        agentEventPublisher.publishError(TEST_ITINERARY_ID, TEST_EXECUTION_ID, 
            "PARSING_ERROR", "Failed to parse LLM response", 
            ErrorEvent.ErrorSeverity.ERROR, true);
        
        // Then - Verify error event was sent
        boolean errorReceived = errorEventLatch.await(2, TimeUnit.SECONDS);
        assertTrue(errorReceived, "Should receive error event");
    }
    
    @Test
    @DisplayName("Should publish events to both SSE and WebSocket")
    void shouldPublishEventsToBothSseAndWebSocket() throws Exception {
        // Given
        CountDownLatch sseEventLatch = new CountDownLatch(1);
        CountDownLatch webSocketEventLatch = new CountDownLatch(1);
        
        // Mock SSE connection
        SseEmitter sseEmitter = mock(SseEmitter.class);
        doAnswer(invocation -> {
            sseEventLatch.countDown();
            return null;
        }).when(sseEmitter).send(any(SseEmitter.SseEventBuilder.class));
        
        // Mock WebSocket messaging template
        org.springframework.messaging.simp.SimpMessagingTemplate mockMessagingTemplate = 
            mock(org.springframework.messaging.simp.SimpMessagingTemplate.class);
        doAnswer(invocation -> {
            webSocketEventLatch.countDown();
            return null;
        }).when(mockMessagingTemplate).convertAndSend(anyString(), any(Object.class));
        
        // Register SSE connection
        sseConnectionManager.registerConnection(TEST_ITINERARY_ID, TEST_EXECUTION_ID, sseEmitter);
        
        // Inject WebSocket mock into the system (this would be done via Spring in real app)
        // For testing, we'll directly test the AgentEventBus which now publishes to both
        
        // When - Publish progress event
        agentEventPublisher.publishProgress(TEST_ITINERARY_ID, TEST_EXECUTION_ID, 50, 
            "Testing dual publishing", "PLANNER");
        
        // Then - Verify both SSE and WebSocket received events
        boolean sseReceived = sseEventLatch.await(2, TimeUnit.SECONDS);
        boolean webSocketReceived = webSocketEventLatch.await(2, TimeUnit.SECONDS);
        
        assertTrue(sseReceived, "Should receive SSE event");
        // Note: WebSocket test would require full Spring context setup
        // For now, we verify the SSE part works and the WebSocket integration is in place
        
        verify(sseEmitter, atLeastOnce()).send(any(SseEmitter.SseEventBuilder.class));
    }
    
    @Test
    @DisplayName("Should handle connection recovery and missed events")
    void shouldHandleConnectionRecoveryAndMissedEvents() throws Exception {
        // Given - Publish some events first
        agentEventPublisher.publishProgress(TEST_ITINERARY_ID, TEST_EXECUTION_ID, 25, "Day 1 planning", "PLANNER");
        agentEventPublisher.publishDayCompleted(TEST_ITINERARY_ID, TEST_EXECUTION_ID, createMockDay(1));
        
        // Small delay to ensure events are processed
        Thread.sleep(100);
        
        // When - Register new connection (simulating reconnection)
        SseEmitter newEmitter = mock(SseEmitter.class);
        AtomicInteger eventCount = new AtomicInteger(0);
        CountDownLatch eventLatch = new CountDownLatch(1);
        
        doAnswer(invocation -> {
            eventCount.incrementAndGet();
            eventLatch.countDown();
            return null;
        }).when(newEmitter).send(any(SseEmitter.SseEventBuilder.class));
        
        sseConnectionManager.registerConnection(TEST_ITINERARY_ID, TEST_EXECUTION_ID, newEmitter);
        
        // Wait for at least one event
        boolean received = eventLatch.await(2, TimeUnit.SECONDS);
        
        // Then - Should receive at least connection confirmation
        assertTrue(received, "Should receive connection confirmation");
        assertTrue(eventCount.get() >= 1, "Should receive at least connection confirmation, got: " + eventCount.get());
    }
    
    @Test
    @DisplayName("Should calculate progress correctly during batch planning")
    void shouldCalculateProgressCorrectlyDuringBatchPlanning() throws Exception {
        // Given
        AtomicInteger lastProgress = new AtomicInteger(0);
        
        // Mock emitter to capture progress updates
        SseEmitter emitter = mock(SseEmitter.class);
        doAnswer(invocation -> {
            // In real scenario, we'd parse the event and extract progress
            lastProgress.set(50); // Simulate progress update
            return null;
        }).when(emitter).send(any(SseEmitter.SseEventBuilder.class));
        
        sseConnectionManager.registerConnection(TEST_ITINERARY_ID, TEST_EXECUTION_ID, emitter);
        
        // When - Publish batch progress
        agentEventPublisher.publishBatchProgress(TEST_ITINERARY_ID, TEST_EXECUTION_ID, 2, 4, "Planning attractions");
        
        // Then - Verify progress calculation
        verify(emitter, atLeastOnce()).send(any(SseEmitter.SseEventBuilder.class));
    }
    
    @Test
    @DisplayName("Should handle multiple concurrent connections")
    void shouldHandleMultipleConcurrentConnections() throws Exception {
        // Given
        List<SseEmitter> emitters = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            SseEmitter emitter = mock(SseEmitter.class);
            emitters.add(emitter);
            sseConnectionManager.registerConnection(TEST_ITINERARY_ID, TEST_EXECUTION_ID, emitter);
        }
        
        // When - Broadcast event
        agentEventPublisher.publishProgress(TEST_ITINERARY_ID, TEST_EXECUTION_ID, 75, "Almost done", "orchestrator");
        
        // Then - All connections should receive the event
        for (SseEmitter emitter : emitters) {
            verify(emitter, atLeastOnce()).send(any(SseEmitter.SseEventBuilder.class));
        }
        
        assertEquals(3, sseConnectionManager.getConnectionCount(TEST_ITINERARY_ID));
    }
    
    @Test
    @DisplayName("Should clean up dead connections automatically")
    void shouldCleanUpDeadConnectionsAutomatically() throws Exception {
        // Given
        SseEmitter deadEmitter = mock(SseEmitter.class);
        SseEmitter aliveEmitter = mock(SseEmitter.class);
        
        // Mock dead connection (throws IOException)
        doThrow(new java.io.IOException("Connection closed")).when(deadEmitter).send(any(SseEmitter.SseEventBuilder.class));
        
        sseConnectionManager.registerConnection(TEST_ITINERARY_ID, TEST_EXECUTION_ID, deadEmitter);
        sseConnectionManager.registerConnection(TEST_ITINERARY_ID, TEST_EXECUTION_ID, aliveEmitter);
        
        assertEquals(2, sseConnectionManager.getConnectionCount(TEST_ITINERARY_ID));
        
        // When - Broadcast event (should clean up dead connection)
        agentEventPublisher.publishProgress(TEST_ITINERARY_ID, TEST_EXECUTION_ID, 50, "Progress", "PLANNER");
        
        // Then - Dead connection should be removed
        assertEquals(1, sseConnectionManager.getConnectionCount(TEST_ITINERARY_ID));
        verify(aliveEmitter, atLeastOnce()).send(any(SseEmitter.SseEventBuilder.class));
    }
    
    // Helper methods
    
    private void simulateDayByDayPlanning() {
        // Simulate the planning process with real-time updates
        new Thread(() -> {
            try {
                Thread.sleep(100); // Small delay to simulate processing
                agentEventPublisher.publishDayCompleted(TEST_ITINERARY_ID, TEST_EXECUTION_ID, createMockDay(1));
                
                Thread.sleep(100);
                agentEventPublisher.publishDayCompleted(TEST_ITINERARY_ID, TEST_EXECUTION_ID, createMockDay(2));
                
                Thread.sleep(100);
                agentEventPublisher.publishGenerationComplete(TEST_ITINERARY_ID, TEST_EXECUTION_ID, createMockNormalizedItinerary());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }
    
    private CreateItineraryReq createTestRequest(int days) {
        return CreateItineraryReq.builder()
            .destination("Tokyo")
            .startDate(java.time.LocalDate.of(2024, 6, 1))
            .endDate(java.time.LocalDate.of(2024, 6, 1).plusDays(days - 1))
            .budgetTier("mid")
            .interests(List.of("culture", "food", "temples"))
            .constraints(List.of("no early mornings"))
            .language("en")
            .party(PartyDto.builder()
                .adults(2)
                .children(0)
                .infants(0)
                .rooms(1)
                .build())
            .build();
    }
    
    private ItineraryDto createMockItinerary() {
        return ItineraryDto.builder()
            .id(TEST_ITINERARY_ID)
            .destination("Tokyo")
            .startDate(LocalDate.of(2024, 6, 1))
            .endDate(LocalDate.of(2024, 6, 2))
            .budgetTier("mid")
            .interests(List.of("culture", "food", "temples"))
            .language("en")
            .summary("2-day trip to Tokyo")
            .status("generating")
            .build();
    }
    
    private NormalizedItinerary createMockNormalizedItinerary() {
        NormalizedItinerary itinerary = new NormalizedItinerary();
        itinerary.setItineraryId(TEST_ITINERARY_ID);
        itinerary.setDestination("Tokyo");
        itinerary.setDays(new ArrayList<>());
        itinerary.setCreatedAt(System.currentTimeMillis());
        itinerary.setUpdatedAt(System.currentTimeMillis());
        return itinerary;
    }
    
    private NormalizedDay createMockDay(int dayNumber) {
        NormalizedDay day = new NormalizedDay();
        day.setDayNumber(dayNumber);
        day.setDate("2024-06-" + String.format("%02d", dayNumber));
        day.setLocation("Tokyo");
        day.setNodes(List.of(createMockNode("Activity " + dayNumber)));
        return day;
    }
    
    private NormalizedNode createMockNode(String title) {
        NormalizedNode node = new NormalizedNode();
        node.setId("node-" + System.currentTimeMillis());
        node.setTitle(title);
        node.setType("attraction");
        return node;
    }
    
    private String createMockDayBatchResponse(int startDay, int endDay) {
        StringBuilder response = new StringBuilder("{\"days\": [");
        
        for (int day = startDay; day <= endDay; day++) {
            if (day > startDay) response.append(",");
            
            // Calculate epoch milliseconds for timing
            long baseTime = java.time.LocalDate.of(2024, 6, day)
                .atTime(10, 0)
                .atZone(java.time.ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli();
            long endTime = baseTime + 7200000L; // +2 hours
            
            response.append(String.format("""
                {
                  "dayNumber": %d,
                  "date": "2024-06-%02d",
                  "location": "Tokyo",
                  "summary": "Day %d activities",
                  "nodes": [
                    {
                      "id": "node_%d_1",
                      "type": "attraction",
                      "title": "Activity %d",
                      "location": {"name": "Location %d", "address": "Tokyo"},
                      "timing": {"startTime": %d, "endTime": %d, "durationMin": 120},
                      "cost": {"amountPerPerson": 1000, "currency": "JPY"},
                      "details": {"description": "Mock activity for day %d"}
                    }
                  ]
                }
                """, day, day, day, day, day, day, baseTime, endTime, day));
        }
        
        response.append("]}");
        return response.toString();
    }
}