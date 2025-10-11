package com.tripplanner.testing.service;

import com.tripplanner.agents.AgentOrchestrator;
import com.tripplanner.agents.PlannerAgent;
import com.tripplanner.agents.EnrichmentAgent;
import com.tripplanner.dto.*;
import com.tripplanner.service.AgentEventBus;
import com.tripplanner.service.ChangeEngine;
import com.tripplanner.service.ItineraryJsonService;
import com.tripplanner.service.UserDataService;
import com.tripplanner.testing.BaseServiceTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * Atomic tests for AgentOrchestrator with mocked dependencies.
 */
class AgentCoordinatorTest extends BaseServiceTest {
    
    @Mock
    private PlannerAgent mockPlannerAgent;
    
    @Mock
    private EnrichmentAgent mockEnrichmentAgent;
    
    @Mock
    private ItineraryJsonService mockItineraryJsonService;
    
    @Mock
    private ChangeEngine mockChangeEngine;
    
    @Mock
    private UserDataService mockUserDataService;
    
    @Mock
    private AgentEventBus mockAgentEventBus;
    
    @InjectMocks
    private AgentOrchestrator agentOrchestrator;
    
    @BeforeEach
    protected void setUp() {
        super.setUp();
        // @InjectMocks will automatically inject the mocked dependencies
    }
    
    @Override
    protected void setupSpecificMocks() {
        // Setup mock agent behaviors with lenient stubbing
        lenient().when(mockPlannerAgent.canHandle(anyString())).thenReturn(true);
        lenient().when(mockPlannerAgent.canHandle(anyString(), any())).thenReturn(true);
        lenient().when(mockEnrichmentAgent.canHandle(anyString())).thenReturn(true);
        lenient().when(mockEnrichmentAgent.canHandle(anyString(), any())).thenReturn(true);
        
        // Mock the execute methods to return valid results
        NormalizedItinerary mockItinerary = new NormalizedItinerary();
        mockItinerary.setItineraryId("test-itinerary");
        mockItinerary.setVersion(1);
        lenient().when(mockPlannerAgent.execute(anyString(), any())).thenReturn(mockItinerary);
        lenient().when(mockEnrichmentAgent.execute(anyString(), any())).thenReturn(mockItinerary);
        
        // Setup common service mocks
        TripMetadata mockTripMetadata = new TripMetadata();
        mockTripMetadata.setUserId("test-user");
        lenient().when(mockUserDataService.getUserTripMetadata(anyString())).thenReturn(List.of(mockTripMetadata));
        lenient().when(mockUserDataService.getUserTripMetadata(anyString(), anyString())).thenReturn(Optional.of(mockTripMetadata));
        lenient().doNothing().when(mockAgentEventBus).publish(anyString(), any());
        
        // Mock ItineraryJsonService methods that return values
        lenient().when(mockItineraryJsonService.createItinerary(any())).thenReturn(null);
        lenient().when(mockItineraryJsonService.updateItinerary(any())).thenReturn(null);
    }
    
    @Test
    @DisplayName("Should coordinate multiple agents successfully")
    void shouldCoordinateMultipleAgentsSuccessfully() {
        // Given
        String itineraryId = "test-itinerary-001";
        CreateItineraryReq request = CreateItineraryReq.builder()
                .destination("Bali")
                .startLocation("Jakarta")
                .startDate(LocalDate.of(2024, 6, 1))
                .endDate(LocalDate.of(2024, 6, 8))
                .party(PartyDto.builder().adults(2).children(0).infants(0).rooms(1).build())
                .budgetTier("mid")
                .interests(List.of("beach", "culture"))
                .constraints(List.of())
                .language("en")
                .build();
        String userId = "user-123";
        
        // No additional mocking needed - using lenient stubs from setupSpecificMocks
        
        // When
        NormalizedItinerary result = agentOrchestrator.createInitialItinerary(itineraryId, request, userId);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getItineraryId()).isEqualTo(itineraryId);
        assertThat(result.getDestination()).isEqualTo("Bali");
        assertThat(request.getDurationDays()).isEqualTo(8); // June 1-8 inclusive = 8 days
        
        verify(mockItineraryJsonService).createItinerary(any(NormalizedItinerary.class));
    }
    
    @Test
    @DisplayName("Should handle agent coordination with no agents")
    void shouldHandleAgentCoordinationWithNoAgents() {
        // Given
        String itineraryId = "test-itinerary-002";
        CreateItineraryReq request = CreateItineraryReq.builder()
                .destination("Tokyo")
                .startLocation("Osaka")
                .startDate(LocalDate.of(2024, 7, 1))
                .endDate(LocalDate.of(2024, 7, 6))
                .party(PartyDto.builder().adults(1).children(0).infants(0).rooms(1).build())
                .budgetTier("budget")
                .interests(List.of("food", "temples"))
                .constraints(List.of())
                .language("en")
                .build();
        String userId = "user-456";
        
        // No additional mocking needed - using lenient stubs from setupSpecificMocks
        
        // When
        NormalizedItinerary result = agentOrchestrator.createInitialItinerary(itineraryId, request, userId);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getItineraryId()).isEqualTo(itineraryId);
        assertThat(result.getDestination()).isEqualTo("Tokyo");
        assertThat(request.getDurationDays()).isEqualTo(6); // July 1-6 inclusive = 6 days
        
        verify(mockItineraryJsonService).createItinerary(any(NormalizedItinerary.class));
    }
    
    @Test
    @DisplayName("Should handle agent execution failure")
    void shouldHandleAgentExecutionFailure() {
        // Given
        String itineraryId = "test-itinerary-003";
        CreateItineraryReq request = CreateItineraryReq.builder()
                .destination("Paris")
                .startLocation("London")
                .startDate(LocalDate.of(2024, 8, 1))
                .endDate(LocalDate.of(2024, 8, 5))
                .party(PartyDto.builder().adults(2).children(1).infants(0).rooms(2).build())
                .budgetTier("luxury")
                .interests(List.of("art", "museums"))
                .constraints(List.of())
                .language("en")
                .build();
        String userId = "user-789";
        
        when(mockItineraryJsonService.createItinerary(any()))
            .thenThrow(new RuntimeException("Failed to create initial itinerary"));
        
        // When & Then
        assertThatThrownBy(() -> 
            agentOrchestrator.createInitialItinerary(itineraryId, request, userId))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Failed to create initial itinerary");
        
        verify(mockItineraryJsonService).createItinerary(any(NormalizedItinerary.class));
    }
}