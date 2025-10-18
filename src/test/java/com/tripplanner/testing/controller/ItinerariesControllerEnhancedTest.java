package com.tripplanner.testing.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripplanner.controller.ItinerariesController;
import com.tripplanner.dto.*;
import com.tripplanner.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for enhanced ItinerariesController endpoints.
 */
public class ItinerariesControllerEnhancedTest {
    
    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    
    @Mock
    private ItineraryService itineraryService;
    
    @Mock
    private ItineraryJsonService itineraryJsonService;
    
    @Mock
    private ChangeEngine changeEngine;
    
    @Mock
    private UserDataService userDataService;
    
    @Mock
    private AgentRegistry agentRegistry;
    
    @Mock
    private RevisionService revisionService;
    
    @Mock
    private OrchestratorService orchestratorService;
    
    @Mock
    private WebSocketBroadcastService webSocketBroadcastService;
    
    @Mock
    private SseConnectionManager sseConnectionManager;
    
    private ItinerariesController controller;
    
    private static final String TEST_USER_ID = "test-user-123";
    private static final String TEST_ITINERARY_ID = "it_test-456";
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        
        controller = new ItinerariesController(
            itineraryService,
            itineraryJsonService,
            changeEngine,
            userDataService,
            agentRegistry,
            revisionService,
            orchestratorService,
            webSocketBroadcastService,
            sseConnectionManager,
            mock(com.tripplanner.service.ChatHistoryService.class)
        );
        
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }
    
    @Test
    @DisplayName("Should return enhanced creation response with immediate structure")
    void shouldReturnEnhancedCreationResponseWithImmediateStructure() throws Exception {
        // Given
        CreateItineraryReq request = createTestRequest();
        ItineraryDto mockItinerary = createMockItinerary();
        
        when(itineraryService.create(any(CreateItineraryReq.class), eq(TEST_USER_ID)))
            .thenReturn(mockItinerary);
        
        // When & Then
        mockMvc.perform(post("/api/v1/itineraries")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .requestAttr("userId", TEST_USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itinerary").exists())
                .andExpect(jsonPath("$.itinerary.id").value(TEST_ITINERARY_ID))
                .andExpect(jsonPath("$.executionId").exists())
                .andExpect(jsonPath("$.sseEndpoint").exists())
                .andExpect(jsonPath("$.estimatedCompletion").exists())
                .andExpect(jsonPath("$.status").value("PROCESSING"))
                .andExpect(jsonPath("$.stages").isArray())
                .andExpect(jsonPath("$.stages").isNotEmpty());
        
        // Verify service was called
        verify(itineraryService).create(any(CreateItineraryReq.class), eq(TEST_USER_ID));
    }
    
    @Test
    @DisplayName("Should return error response when itinerary creation fails")
    void shouldReturnErrorResponseWhenItineraryCreationFails() throws Exception {
        // Given
        CreateItineraryReq request = createTestRequest();
        
        when(itineraryService.create(any(CreateItineraryReq.class), eq(TEST_USER_ID)))
            .thenThrow(new RuntimeException("Database connection failed"));
        
        // When & Then
        mockMvc.perform(post("/api/v1/itineraries")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .requestAttr("userId", TEST_USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.errorMessage").exists())
                .andExpect(jsonPath("$.itinerary").doesNotExist());
    }
    
    @Test
    @DisplayName("Should return unauthorized when user ID is missing")
    void shouldReturnUnauthorizedWhenUserIdIsMissing() throws Exception {
        // Given
        CreateItineraryReq request = createTestRequest();
        
        // When & Then
        mockMvc.perform(post("/api/v1/itineraries")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
        
        // Verify service was not called
        verify(itineraryService, never()).create(any(), any());
    }
    
    @Test
    @DisplayName("Should calculate estimated completion time based on request complexity")
    void shouldCalculateEstimatedCompletionTimeBasedOnRequestComplexity() throws Exception {
        // Given
        CreateItineraryReq simpleRequest = CreateItineraryReq.builder()
            .destination("Paris")
            .startDate(LocalDate.of(2024, 6, 1))
            .endDate(LocalDate.of(2024, 6, 3)) // 2 days
            .interests(List.of("museums")) // 1 interest
            .constraints(List.of()) // no constraints
            .build();
        
        ItineraryDto mockItinerary = createMockItinerary();
        when(itineraryService.create(any(CreateItineraryReq.class), eq(TEST_USER_ID)))
            .thenReturn(mockItinerary);
        
        LocalDateTime beforeRequest = LocalDateTime.now();
        
        // When & Then
        String responseJson = mockMvc.perform(post("/api/v1/itineraries")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(simpleRequest))
                .requestAttr("userId", TEST_USER_ID))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        
        ItineraryCreationResponse response = objectMapper.readValue(responseJson, ItineraryCreationResponse.class);
        
        // Verify estimated completion is reasonable (should be 30 + 2*5 + 1*2 = 42 seconds from now)
        LocalDateTime afterRequest = LocalDateTime.now();
        assertTrue(response.getEstimatedCompletion().isAfter(beforeRequest.plusSeconds(35)));
        assertTrue(response.getEstimatedCompletion().isBefore(afterRequest.plusSeconds(50)));
    }
    
    @Test
    @DisplayName("Should create execution stages based on request")
    void shouldCreateExecutionStagesBasedOnRequest() throws Exception {
        // Given
        CreateItineraryReq request = createTestRequest();
        ItineraryDto mockItinerary = createMockItinerary();
        
        when(itineraryService.create(any(CreateItineraryReq.class), eq(TEST_USER_ID)))
            .thenReturn(mockItinerary);
        
        // When & Then
        String responseJson = mockMvc.perform(post("/api/v1/itineraries")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .requestAttr("userId", TEST_USER_ID))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        
        ItineraryCreationResponse response = objectMapper.readValue(responseJson, ItineraryCreationResponse.class);
        
        // Verify stages
        assertNotNull(response.getStages());
        assertEquals(4, response.getStages().size());
        
        // Verify stage names and types
        assertEquals("Planning", response.getStages().get(0).getStageName());
        assertEquals("PLANNER", response.getStages().get(0).getAgentType());
        
        assertEquals("Enrichment", response.getStages().get(1).getStageName());
        assertEquals("ENRICHMENT", response.getStages().get(1).getAgentType());
        
        assertEquals("Places Validation", response.getStages().get(2).getStageName());
        assertEquals("places", response.getStages().get(2).getAgentType());
        
        assertEquals("Optimization", response.getStages().get(3).getStageName());
        assertEquals("orchestrator", response.getStages().get(3).getAgentType());
    }
    
    @Test
    @DisplayName("Should establish SSE connection with execution ID")
    void shouldEstablishSseConnectionWithExecutionId() throws Exception {
        // Given
        String itineraryId = "test-itinerary-123";
        String executionId = "exec-456";
        
        // When
        mockMvc.perform(get("/api/v1/itineraries/patches")
                .param("itineraryId", itineraryId)
                .param("executionId", executionId))
                .andExpect(status().isOk());
        
        // Then - Verify connection manager was called (MockMvc doesn't support SSE headers properly)
        verify(sseConnectionManager).registerConnection(eq(itineraryId), eq(executionId), any(SseEmitter.class));
    }
    
    @Test
    @DisplayName("Should establish SSE connection without execution ID")
    void shouldEstablishSseConnectionWithoutExecutionId() throws Exception {
        // Given
        String itineraryId = "test-itinerary-123";
        
        // When
        mockMvc.perform(get("/api/v1/itineraries/patches")
                .param("itineraryId", itineraryId))
                .andExpect(status().isOk());
        
        // Then - Verify connection manager was called with null execution ID
        verify(sseConnectionManager).registerConnection(eq(itineraryId), isNull(), any(SseEmitter.class));
    }
    
    @Test
    @DisplayName("Should return SSE connection statistics")
    void shouldReturnSseConnectionStatistics() throws Exception {
        // Given
        when(sseConnectionManager.getConnectionStats())
            .thenReturn(java.util.Map.of(
                "totalConnections", 5,
                "totalItineraries", 3,
                "connectionsPerItinerary", java.util.Map.of("it_123", 2, "it_456", 3)
            ));
        
        // When & Then
        mockMvc.perform(get("/api/v1/itineraries/patches/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalConnections").value(5))
                .andExpect(jsonPath("$.totalItineraries").value(3))
                .andExpect(jsonPath("$.connectionsPerItinerary").exists());
        
        verify(sseConnectionManager).getConnectionStats();
    }
    
    @Test
    @DisplayName("Should handle SSE stats error gracefully")
    void shouldHandleSseStatsErrorGracefully() throws Exception {
        // Given
        when(sseConnectionManager.getConnectionStats())
            .thenThrow(new RuntimeException("Connection manager error"));
        
        // When & Then
        mockMvc.perform(get("/api/v1/itineraries/patches/stats"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").exists());
    }
    
    @Test
    @DisplayName("Should send update events via connection manager")
    void shouldSendUpdateEventsViaConnectionManager() {
        // Given
        String itineraryId = "test-itinerary-123";
        ItineraryUpdateEvent event = ItineraryUpdateEvent.dayCompleted(
            itineraryId, "exec-456", 1, null, 25, "Day 1 completed"
        );
        
        // When
        controller.sendUpdateEvent(itineraryId, event);
        
        // Then
        verify(sseConnectionManager).broadcastUpdate(itineraryId, event);
    }
    
    @Test
    @DisplayName("Should handle update event sending errors gracefully")
    void shouldHandleUpdateEventSendingErrorsGracefully() {
        // Given
        String itineraryId = "test-itinerary-123";
        ItineraryUpdateEvent event = ItineraryUpdateEvent.progressUpdate(
            itineraryId, "exec-456", 50, "Progress update", "PLANNER"
        );
        
        doThrow(new RuntimeException("Connection error"))
            .when(sseConnectionManager).broadcastUpdate(itineraryId, event);
        
        // When & Then - should not throw exception
        assertDoesNotThrow(() -> controller.sendUpdateEvent(itineraryId, event));
        
        verify(sseConnectionManager).broadcastUpdate(itineraryId, event);
    }
    
    @Test
    @DisplayName("Should validate request parameters")
    void shouldValidateRequestParameters() throws Exception {
        // Given - invalid request with missing required fields
        CreateItineraryReq invalidRequest = CreateItineraryReq.builder()
            .destination("") // empty destination
            .startDate(null) // invalid date
            .build();
        
        // When & Then
        mockMvc.perform(post("/api/v1/itineraries")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest))
                .requestAttr("userId", TEST_USER_ID))
                .andExpect(status().isBadRequest());
        
        // Verify service was not called
        verify(itineraryService, never()).create(any(), any());
    }
    
    // Helper methods
    
    private CreateItineraryReq createTestRequest() {
        return CreateItineraryReq.builder()
            .destination("Tokyo")
            .startDate(LocalDate.of(2024, 6, 1))
            .endDate(LocalDate.of(2024, 6, 5))
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
            .endDate(LocalDate.of(2024, 6, 5))
            .budgetTier("mid")
            .interests(List.of("culture", "food", "temples"))
            .constraints(List.of("no early mornings"))
            .language("en")
            .summary("4-day trip to Tokyo")
            .status("generating")
            .build();
    }
}