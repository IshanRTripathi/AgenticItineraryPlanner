package com.tripplanner.service;

import com.tripplanner.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Itinerary Service Tests")
class ItineraryServiceTest {

    @Mock
    private ItineraryInitializationService initService;

    @Mock
    private ItineraryJsonService itineraryJsonService;

    @Mock
    private UserDataService userDataService;

    @Mock
    private PipelineOrchestrator pipelineOrchestrator;

    @Mock
    private AgentEventPublisher agentEventPublisher;

    private ItineraryService itineraryService;

    @BeforeEach
    void setUp() {
        itineraryService = new ItineraryService(
            initService,
            itineraryJsonService,
            userDataService,
            pipelineOrchestrator,
            agentEventPublisher
        );
    }

    @Test
    @DisplayName("Should create itinerary using pipeline flow only")
    void testCreate_UsesPipelineFlow() {
        // Given
        String userId = "user_test_123";
        CreateItineraryReq request = CreateItineraryReq.builder()
            .destination("Barcelona, Spain")
            .startLocation("Mumbai, India")
            .startDate(LocalDate.of(2025, 11, 1))
            .endDate(LocalDate.of(2025, 11, 3))
            .party(PartyDto.builder().adults(2).children(1).infants(0).rooms(1).build())
            .budgetTier("medium")
            .interests(Arrays.asList("culture", "food"))
            .language("en")
            .build();

        NormalizedItinerary mockInitialItinerary = createMockItinerary("it_test", userId);
        when(initService.createInitialItinerary(anyString(), eq(request), eq(userId)))
            .thenReturn(mockInitialItinerary);

        CompletableFuture<NormalizedItinerary> mockFuture = CompletableFuture.completedFuture(mockInitialItinerary);
        when(pipelineOrchestrator.generateItinerary(anyString(), eq(request), eq(userId)))
            .thenReturn(mockFuture);

        // When
        ItineraryDto result = itineraryService.create(request, userId);

        // Then
        assertNotNull(result);
        assertEquals("generating", result.getStatus());
        assertEquals("Barcelona, Spain", result.getDestination());

        // Verify initService was called first
        verify(initService, times(1)).createInitialItinerary(anyString(), eq(request), eq(userId));

        // Verify pipelineOrchestrator was called (always pipeline flow)
        verify(pipelineOrchestrator, times(1)).generateItinerary(anyString(), eq(request), eq(userId));
    }

    @Test
    @DisplayName("Should establish ownership before starting async generation")
    void testCreate_EstablishesOwnershipFirst() {
        // Given
        String userId = "user_ownership_test";
        CreateItineraryReq request = CreateItineraryReq.builder()
            .destination("Paris, France")
            .startDate(LocalDate.of(2025, 12, 1))
            .endDate(LocalDate.of(2025, 12, 3))
            .build();

        NormalizedItinerary mockInitialItinerary = createMockItinerary("it_ownership", userId);
        when(initService.createInitialItinerary(anyString(), eq(request), eq(userId)))
            .thenReturn(mockInitialItinerary);

        CompletableFuture<NormalizedItinerary> mockFuture = CompletableFuture.completedFuture(mockInitialItinerary);
        when(pipelineOrchestrator.generateItinerary(anyString(), eq(request), eq(userId)))
            .thenReturn(mockFuture);

        // When
        ItineraryDto result = itineraryService.create(request, userId);

        // Then
        assertNotNull(result);

        // Verify order of operations: initService BEFORE pipelineOrchestrator
        var inOrder = inOrder(initService, pipelineOrchestrator);
        inOrder.verify(initService).createInitialItinerary(anyString(), eq(request), eq(userId));
        inOrder.verify(pipelineOrchestrator).generateItinerary(anyString(), eq(request), eq(userId));
    }

    @Test
    @DisplayName("Should publish error event when pipeline generation fails")
    void testCreate_PublishesErrorOnFailure() throws Exception {
        // Given
        String userId = "user_error_test";
        CreateItineraryReq request = CreateItineraryReq.builder()
            .destination("London, UK")
            .startDate(LocalDate.of(2026, 1, 1))
            .endDate(LocalDate.of(2026, 1, 3))
            .build();

        NormalizedItinerary mockInitialItinerary = createMockItinerary("it_error", userId);
        when(initService.createInitialItinerary(anyString(), eq(request), eq(userId)))
            .thenReturn(mockInitialItinerary);

        // Create a failed future
        CompletableFuture<NormalizedItinerary> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Pipeline generation failed"));
        when(pipelineOrchestrator.generateItinerary(anyString(), eq(request), eq(userId)))
            .thenReturn(failedFuture);

        // When
        ItineraryDto result = itineraryService.create(request, userId);

        // Then
        assertNotNull(result);
        assertEquals("generating", result.getStatus()); // API returns 200 OK even if async fails

        // Wait for async callback to execute
        Thread.sleep(100);

        // Verify error event was published
        verify(agentEventPublisher, times(1)).publishErrorFromException(
            anyString(),
            anyString(),
            any(Exception.class),
            eq("itinerary generation"),
            eq(ErrorEvent.ErrorSeverity.ERROR)
        );
    }

    @Test
    @DisplayName("Should handle initialization failure")
    void testCreate_InitializationFailure() {
        // Given
        String userId = "user_init_fail";
        CreateItineraryReq request = CreateItineraryReq.builder()
            .destination("Rome, Italy")
            .startDate(LocalDate.of(2026, 2, 1))
            .endDate(LocalDate.of(2026, 2, 3))
            .build();

        when(initService.createInitialItinerary(anyString(), eq(request), eq(userId)))
            .thenThrow(new RuntimeException("Initialization failed"));

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            itineraryService.create(request, userId);
        });

        // Verify pipelineOrchestrator was NOT called (initialization failed first)
        verify(pipelineOrchestrator, never()).generateItinerary(anyString(), any(), anyString());
    }

    @Test
    @DisplayName("Should get itinerary for authorized user")
    void testGet_AuthorizedUser() {
        // Given
        String itineraryId = "it_get_test";
        String userId = "user_get_test";

        when(userDataService.userOwnsTrip(userId, itineraryId)).thenReturn(true);

        NormalizedItinerary mockItinerary = createMockItinerary(itineraryId, userId);
        when(itineraryJsonService.getItinerary(itineraryId)).thenReturn(Optional.of(mockItinerary));

        // When
        ItineraryDto result = itineraryService.get(itineraryId, userId);

        // Then
        assertNotNull(result);
        assertEquals(itineraryId, result.getId());
        assertEquals("completed", result.getStatus());

        verify(userDataService, times(1)).userOwnsTrip(userId, itineraryId);
        verify(itineraryJsonService, times(1)).getItinerary(itineraryId);
    }

    @Test
    @DisplayName("Should throw exception for unauthorized user")
    void testGet_UnauthorizedUser() {
        // Given
        String itineraryId = "it_unauthorized";
        String userId = "user_unauthorized";

        when(userDataService.userOwnsTrip(userId, itineraryId)).thenReturn(false);

        // When & Then
        assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> {
            itineraryService.get(itineraryId, userId);
        });

        // Verify itinerary was NOT retrieved (authorization failed first)
        verify(itineraryJsonService, never()).getItinerary(anyString());
    }

    @Test
    @DisplayName("Should delete itinerary for authorized user")
    void testDelete_Success() {
        // Given
        String itineraryId = "it_delete_test";
        String userId = "user_delete_test";

        doNothing().when(userDataService).deleteUserTripMetadata(userId, itineraryId);
        doNothing().when(itineraryJsonService).deleteItinerary(itineraryId);

        // When
        itineraryService.delete(itineraryId, userId);

        // Then
        verify(userDataService, times(1)).deleteUserTripMetadata(userId, itineraryId);
        verify(itineraryJsonService, times(1)).deleteItinerary(itineraryId);
    }

    @Test
    @DisplayName("Should get user itineraries")
    void testGetUserItineraries() {
        // Given
        String userId = "user_list_test";

        TripMetadata metadata1 = new TripMetadata();
        metadata1.setItineraryId("it_1");
        metadata1.setDestination("Barcelona, Spain");
        metadata1.setStartDate("2025-11-01");
        metadata1.setEndDate("2025-11-03");
        metadata1.setLanguage("en");
        metadata1.setSummary("3-day trip to Barcelona");
        metadata1.setInterests(Arrays.asList("culture", "food"));

        TripMetadata metadata2 = new TripMetadata();
        metadata2.setItineraryId("it_2");
        metadata2.setDestination("Paris, France");
        metadata2.setStartDate("2025-12-01");
        metadata2.setEndDate("2025-12-05");
        metadata2.setLanguage("en");
        metadata2.setSummary("5-day trip to Paris");
        metadata2.setInterests(Arrays.asList("art", "history"));

        when(userDataService.getUserTripMetadata(userId))
            .thenReturn(Arrays.asList(metadata1, metadata2));

        // When
        var result = itineraryService.getUserItineraries(userId, 0, 10);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("it_1", result.get(0).getId());
        assertEquals("Barcelona, Spain", result.get(0).getDestination());
        assertEquals("it_2", result.get(1).getId());
        assertEquals("Paris, France", result.get(1).getDestination());

        verify(userDataService, times(1)).getUserTripMetadata(userId);
    }

    @Test
    @DisplayName("Should always use pipeline orchestrator (never monolithic)")
    void testCreate_AlwaysUsesPipeline() {
        // Given
        String userId = "user_pipeline_only";
        CreateItineraryReq request = CreateItineraryReq.builder()
            .destination("Tokyo, Japan")
            .startDate(LocalDate.of(2025, 6, 1))
            .endDate(LocalDate.of(2025, 6, 3))
            .build();

        NormalizedItinerary mockInitialItinerary = createMockItinerary("it_pipeline", userId);
        when(initService.createInitialItinerary(anyString(), eq(request), eq(userId)))
            .thenReturn(mockInitialItinerary);

        CompletableFuture<NormalizedItinerary> mockFuture = CompletableFuture.completedFuture(mockInitialItinerary);
        when(pipelineOrchestrator.generateItinerary(anyString(), eq(request), eq(userId)))
            .thenReturn(mockFuture);

        // When
        itineraryService.create(request, userId);

        // Then - Verify ONLY pipelineOrchestrator was called
        verify(pipelineOrchestrator, times(1)).generateItinerary(anyString(), eq(request), eq(userId));
        
        // Note: We can't verify AgentOrchestrator was never called because it's been removed
        // This test documents that the service now ONLY uses pipeline flow
    }

    // Helper methods

    private NormalizedItinerary createMockItinerary(String id, String userId) {
        NormalizedItinerary itinerary = new NormalizedItinerary();
        itinerary.setItineraryId(id);
        itinerary.setUserId(userId);
        itinerary.setVersion(1);
        itinerary.setCreatedAt(System.currentTimeMillis());
        itinerary.setUpdatedAt(System.currentTimeMillis());
        itinerary.setSummary("Test itinerary");
        itinerary.setCurrency("INR");
        return itinerary;
    }
}
