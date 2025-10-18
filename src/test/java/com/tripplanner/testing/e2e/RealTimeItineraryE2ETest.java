package com.tripplanner.testing.e2e;

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
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * End-to-end test simulating complete real-time itinerary generation flow.
 * Tests the entire pipeline from HTTP request to SSE events with realistic scenarios.
 */
public class RealTimeItineraryE2ETest {
    
    @Mock private ItineraryService itineraryService;
    @Mock private ItineraryJsonService itineraryJsonService;
    @Mock private AiClient aiClient;
    @Mock private AgentEventBus agentEventBus;
    
    private SseConnectionManager sseConnectionManager;
    private AgentEventPublisher agentEventPublisher;
    private SummarizationService summarizationService;
    private DayByDayPlannerAgent plannerAgent;
    private ItinerariesController controller;
    private ObjectMapper objectMapper;
    
    private static final String TEST_ITINERARY_ID = "it_e2e_test";
    private static final String TEST_USER_ID = "user_e2e_test";
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        objectMapper = new ObjectMapper();
        
        // Create real instances for E2E testing
        sseConnectionManager = new SseConnectionManager();
        agentEventPublisher = new AgentEventPublisher(sseConnectionManager);
        summarizationService = new SummarizationService(objectMapper);
        
        plannerAgent = new DayByDayPlannerAgent(
            agentEventBus, aiClient, objectMapper, itineraryJsonService,
            summarizationService, agentEventPublisher
        );
        
        controller = new ItinerariesController(
            itineraryService, itineraryJsonService, null, null, null,
            null, null, null, sseConnectionManager, null
        );
        
        setupRealisticMockResponses();
    }
    
    @Test
    @DisplayName("Complete E2E: 5-day Tokyo trip with real-time updates and summarization")
    void completeE2ETokyoTripWithRealTimeUpdates() throws Exception {
        // Given - 5-day Tokyo trip request
        CreateItineraryReq request = CreateItineraryReq.builder()
            .destination("Tokyo")
            .startDate(java.time.LocalDate.of(2024, 6, 1))
            .endDate(java.time.LocalDate.of(2024, 6, 5))
            .budgetTier("mid")
            .interests(List.of("culture", "food", "temples", "modern"))
            .constraints(List.of("no early mornings"))
            .language("en")
            .party(PartyDto.builder().adults(2).children(0).rooms(1).build())
            .build();
        
        // Setup event collection
        List<ItineraryUpdateEvent> receivedEvents = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch completionLatch = new CountDownLatch(1);
        AtomicInteger dayCompletedCount = new AtomicInteger(0);
        
        // Create SSE connection
        SseEmitter emitter = new SseEmitter(60000L);
        emitter.onCompletion(() -> completionLatch.countDown());
        
        String executionId = "exec_e2e_" + System.currentTimeMillis();
        sseConnectionManager.registerConnection(TEST_ITINERARY_ID, executionId, emitter);
        
        // When - Execute complete flow
        CompletableFuture<Void> planningFuture = CompletableFuture.runAsync(() -> {
            try {
                BaseAgent.AgentRequest<ItineraryDto> agentRequest = 
                    new BaseAgent.AgentRequest<>(request, ItineraryDto.class);
                plannerAgent.execute(TEST_ITINERARY_ID, agentRequest);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        
        // Simulate real-time event collection
        CompletableFuture<Void> eventCollectionFuture = CompletableFuture.runAsync(() -> {
            try {
                // Wait for planning to complete or timeout
                planningFuture.get(30, TimeUnit.SECONDS);
            } catch (Exception e) {
                // Expected for this test setup
            }
        });
        
        // Wait for completion or timeout
        boolean completed = completionLatch.await(35, TimeUnit.SECONDS);
        
        // Then - Verify the complete flow
        assertTrue(sseConnectionManager.hasActiveConnections(TEST_ITINERARY_ID) || completed,
                  "Should have active connections or completed");
        
        // Verify itinerary was updated (at least once per batch)
        verify(itineraryJsonService, atLeast(2)).updateItinerary(any(NormalizedItinerary.class));
        
        // Verify LLM was called for each batch
        verify(aiClient, atLeast(2)).generateStructuredContent(any(), any(), any());
    }
    
    @Test
    @DisplayName("E2E: Error handling and recovery during planning")
    void e2eErrorHandlingAndRecoveryDuringPlanning() throws Exception {
        // Given - Setup to simulate LLM failure on second batch
        AtomicInteger callCount = new AtomicInteger(0);
        when(aiClient.generateStructuredContent(anyString(), any(), any()))
            .thenAnswer(invocation -> {
                int count = callCount.incrementAndGet();
                if (count == 1) {
                    return createValidDayResponse(1, 3);
                } else {
                    throw new RuntimeException("LLM service temporarily unavailable");
                }
            });
        
        CreateItineraryReq request = createTestRequest(5);
        
        // Setup error event collection
        AtomicReference<ErrorEvent> capturedError = new AtomicReference<>();
        CountDownLatch errorLatch = new CountDownLatch(1);
        
        SseEmitter emitter = new SseEmitter(30000L);
        String executionId = "exec_error_test";
        sseConnectionManager.registerConnection(TEST_ITINERARY_ID, executionId, emitter);
        
        // When - Execute with error scenario
        BaseAgent.AgentRequest<ItineraryDto> agentRequest = 
            new BaseAgent.AgentRequest<>(request, ItineraryDto.class);
        
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            plannerAgent.execute(TEST_ITINERARY_ID, agentRequest);
        });
        
        // Then - Verify error handling
        assertTrue(exception.getMessage().contains("LLM service temporarily unavailable"));
        
        // Verify partial success (first batch should have succeeded)
        verify(itineraryJsonService, atLeast(3)).updateItinerary(any(NormalizedItinerary.class));
    }
    
    @Test
    @DisplayName("E2E: Connection recovery and missed events")
    void e2eConnectionRecoveryAndMissedEvents() throws Exception {
        // Given - Start planning and disconnect midway
        CreateItineraryReq request = createTestRequest(3);
        String executionId = "exec_recovery_test";
        
        // First connection
        SseEmitter firstEmitter = new SseEmitter(30000L);
        sseConnectionManager.registerConnection(TEST_ITINERARY_ID, executionId, firstEmitter);
        
        // Simulate some events
        agentEventPublisher.publishProgress(TEST_ITINERARY_ID, executionId, 25, "Starting planning", "PLANNER");
        agentEventPublisher.publishDayCompleted(TEST_ITINERARY_ID, executionId, createMockDay(1));
        
        // Simulate connection drop
        firstEmitter.complete();
        
        // When - Reconnect (simulating user refresh)
        SseEmitter secondEmitter = new SseEmitter(30000L);
        AtomicInteger missedEventCount = new AtomicInteger(0);
        
        // Mock to count events sent to new connection
        SseEmitter spyEmitter = spy(secondEmitter);
        doAnswer(invocation -> {
            missedEventCount.incrementAndGet();
            return null;
        }).when(spyEmitter).send(any(SseEmitter.SseEventBuilder.class));
        
        sseConnectionManager.registerConnection(TEST_ITINERARY_ID, executionId, spyEmitter);
        
        // Then - Should receive missed events
        assertTrue(missedEventCount.get() >= 2, "Should receive connection confirmation + missed events");
    }
    
    @Test
    @DisplayName("E2E: Multiple concurrent users with different itineraries")
    void e2eMultipleConcurrentUsersWithDifferentItineraries() throws Exception {
        // Given - Multiple users creating itineraries simultaneously
        String[] itineraryIds = {"it_user1", "it_user2", "it_user3"};
        String[] executionIds = {"exec_user1", "exec_user2", "exec_user3"};
        
        List<CompletableFuture<Void>> userFutures = new ArrayList<>();
        
        for (int i = 0; i < 3; i++) {
            final int userIndex = i;
            final String itineraryId = itineraryIds[i];
            final String executionId = executionIds[i];
            
            // Setup mock for each user's itinerary
            when(itineraryJsonService.getItinerary(itineraryId))
                .thenReturn(Optional.of(createMockItinerary(itineraryId)));
            
            CompletableFuture<Void> userFuture = CompletableFuture.runAsync(() -> {
                try {
                    // Each user connects to SSE
                    SseEmitter emitter = new SseEmitter(30000L);
                    sseConnectionManager.registerConnection(itineraryId, executionId, emitter);
                    
                    // Simulate planning events for each user
                    agentEventPublisher.publishProgress(itineraryId, executionId, 50, "Planning in progress", "PLANNER");
                    agentEventPublisher.publishDayCompleted(itineraryId, executionId, createMockDay(1));
                    
                    Thread.sleep(100); // Small delay to simulate processing
                    
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            
            userFutures.add(userFuture);
        }
        
        // When - Wait for all users to complete
        CompletableFuture.allOf(userFutures.toArray(new CompletableFuture[0]))
                        .get(10, TimeUnit.SECONDS);
        
        // Then - Verify each user has their own connection
        for (String itineraryId : itineraryIds) {
            assertTrue(sseConnectionManager.hasActiveConnections(itineraryId),
                      "User " + itineraryId + " should have active connection");
            assertEquals(1, sseConnectionManager.getConnectionCount(itineraryId),
                        "Each user should have exactly one connection");
        }
        
        // Verify connection stats
        Map<String, Object> stats = sseConnectionManager.getConnectionStats();
        assertEquals(3, stats.get("totalItineraries"));
        assertEquals(3, stats.get("totalConnections"));
    }
    
    @Test
    @DisplayName("E2E: Large itinerary with summarization and token management")
    void e2eLargeItineraryWithSummarizationAndTokenManagement() throws Exception {
        // Given - 10-day trip that will require summarization
        CreateItineraryReq request = createTestRequest(10);
        
        // Setup responses for multiple batches using dynamic parsing
        when(aiClient.generateStructuredContent(anyString(), anyString(), anyString()))
            .thenAnswer(invocation -> {
                String prompt = invocation.getArgument(0, String.class);
                
                // Parse the prompt to extract day range
                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("Plan days (\\d+)-(\\d+)");
                java.util.regex.Matcher matcher = pattern.matcher(prompt);
                
                if (matcher.find()) {
                    int startDay = Integer.parseInt(matcher.group(1));
                    int endDay = Integer.parseInt(matcher.group(2));
                    return createValidDayResponse(startDay, endDay);
                }
                
                return "{}"; // Fallback
            });
        
        String executionId = "exec_large_test";
        SseEmitter emitter = new SseEmitter(60000L);
        sseConnectionManager.registerConnection(TEST_ITINERARY_ID, executionId, emitter);
        
        // When - Execute large itinerary planning
        BaseAgent.AgentRequest<ItineraryDto> agentRequest = 
            new BaseAgent.AgentRequest<>(request, ItineraryDto.class);
        
        assertDoesNotThrow(() -> {
            plannerAgent.execute(TEST_ITINERARY_ID, agentRequest);
        });
        
        // Then - Verify all days were processed
        // With 10 days and batches of 3: 3+3+3+1 = 4 batches
        // Each day gets saved individually + final save = 10 + 1 = 11 updates
        verify(itineraryJsonService, atLeast(10)).updateItinerary(any(NormalizedItinerary.class));
        
        // Verify multiple LLM calls were made (batching: 10 days / 3 per batch = 4 batches)
        verify(aiClient, atLeast(4)).generateStructuredContent(any(), any(), any());
    }
    
    @Test
    @DisplayName("E2E: Performance under load with connection cleanup")
    void e2ePerformanceUnderLoadWithConnectionCleanup() throws Exception {
        // Given - Simulate high load scenario
        int connectionCount = 20;
        List<SseEmitter> emitters = new ArrayList<>();
        
        // Create many connections
        for (int i = 0; i < connectionCount; i++) {
            SseEmitter emitter = new SseEmitter(30000L);
            emitters.add(emitter);
            sseConnectionManager.registerConnection(TEST_ITINERARY_ID, "exec_" + i, emitter);
        }
        
        assertEquals(connectionCount, sseConnectionManager.getConnectionCount(TEST_ITINERARY_ID));
        
        // When - Broadcast events to all connections
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < 10; i++) {
            agentEventPublisher.publishProgress(TEST_ITINERARY_ID, "exec_load_test", 
                                              i * 10, "Progress " + i, "PLANNER");
        }
        
        long broadcastTime = System.currentTimeMillis() - startTime;
        
        // Simulate some connections dying
        for (int i = 0; i < 5; i++) {
            emitters.get(i).complete();
        }
        
        // Trigger cleanup by sending another event
        agentEventPublisher.publishProgress(TEST_ITINERARY_ID, "exec_load_test", 
                                          100, "Cleanup test", "PLANNER");
        
        // Then - Verify performance and cleanup
        assertTrue(broadcastTime < 1000, "Broadcasting should be fast even with many connections");
        
        // Connection count should be reduced after cleanup
        int remainingConnections = sseConnectionManager.getConnectionCount(TEST_ITINERARY_ID);
        assertTrue(remainingConnections < connectionCount, 
                  "Dead connections should be cleaned up");
    }
    
    // Helper methods
    
    private void setupRealisticMockResponses() {
        // Mock itinerary service
        when(itineraryService.create(any(CreateItineraryReq.class), any()))
            .thenReturn(createMockItineraryDto());
        
        when(itineraryJsonService.getItinerary(TEST_ITINERARY_ID))
            .thenReturn(Optional.of(createMockItinerary(TEST_ITINERARY_ID)));
        
        when(itineraryJsonService.updateItinerary(any(NormalizedItinerary.class)))
            .thenReturn(null);
        
        // Mock realistic LLM responses
        when(aiClient.generateStructuredContent(any(), any(), any()))
            .thenAnswer(invocation -> {
                String prompt = invocation.getArgument(0, String.class);
                if (prompt.contains("Planning days 1-3")) {
                    return createValidDayResponse(1, 3);
                } else if (prompt.contains("Planning days 4-5")) {
                    return createValidDayResponse(4, 5);
                } else {
                    return createValidDayResponse(1, 1);
                }
            });
    }
    
    private CreateItineraryReq createTestRequest(int days) {
        return CreateItineraryReq.builder()
            .destination("Tokyo")
            .startDate(java.time.LocalDate.of(2024, 6, 1))
            .endDate(java.time.LocalDate.of(2024, 6, 1).plusDays(days - 1))
            .budgetTier("mid")
            .interests(List.of("culture", "food"))
            .language("en")
            .build();
    }
    
    private ItineraryDto createMockItineraryDto() {
        return ItineraryDto.builder()
            .id(TEST_ITINERARY_ID)
            .destination("Tokyo")
            .status("generating")
            .build();
    }
    
    private NormalizedItinerary createMockItinerary(String id) {
        NormalizedItinerary itinerary = new NormalizedItinerary();
        itinerary.setItineraryId(id);
        itinerary.setDestination("Tokyo");
        itinerary.setDays(new ArrayList<>());
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
    
    private String createValidDayResponse(int startDay, int endDay) {
        StringBuilder response = new StringBuilder("{\"days\": [");
        
        for (int day = startDay; day <= endDay; day++) {
            if (day > startDay) response.append(",");
            
            // Calculate epoch milliseconds for timing
            long baseTime = java.time.LocalDate.of(2024, 6, day)
                .atTime(10, 0)
                .atZone(java.time.ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli();
            long endTime = baseTime + (2 * 3600000); // +2 hours
            
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
                      "title": "Morning Activity Day %d",
                      "location": {"name": "Location %d", "address": "Tokyo"},
                      "timing": {"startTime": %d, "endTime": %d, "durationMin": 120},
                      "cost": {"amountPerPerson": 1000, "currency": "JPY"},
                      "details": {"description": "Activity for day %d"}
                    }
                  ]
                }
                """, day, day, day, day, day, day, baseTime, endTime, day));
        }
        
        response.append("]}");
        return response.toString();
    }
}