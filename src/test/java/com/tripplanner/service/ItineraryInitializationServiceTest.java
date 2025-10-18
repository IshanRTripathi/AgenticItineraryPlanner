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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Itinerary Initialization Service Tests")
class ItineraryInitializationServiceTest {

    @Mock
    private ItineraryJsonService itineraryJsonService;

    @Mock
    private UserDataService userDataService;

    private ItineraryInitializationService initService;

    @BeforeEach
    void setUp() {
        initService = new ItineraryInitializationService(
            itineraryJsonService,
            userDataService
        );
    }

    @Test
    @DisplayName("Should create initial itinerary successfully")
    void testCreateInitialItinerary_Success() {
        // Given
        String itineraryId = "it_test_123";
        String userId = "user_test_456";
        CreateItineraryReq request = CreateItineraryReq.builder()
            .destination("Barcelona, Spain")
            .startLocation("Mumbai, India")
            .startDate(LocalDate.of(2025, 11, 1))
            .endDate(LocalDate.of(2025, 11, 3))
            .party(PartyDto.builder().adults(2).children(1).infants(0).rooms(1).build())
            .budgetTier("medium")
            .interests(Arrays.asList("culture", "food", "family-friendly"))
            .constraints(Arrays.asList("no-spicy-food"))
            .language("en")
            .build();

        doNothing().when(itineraryJsonService).createItinerary(any(NormalizedItinerary.class));
        doNothing().when(userDataService).saveUserTripMetadata(anyString(), any(TripMetadata.class));

        // When
        NormalizedItinerary result = initService.createInitialItinerary(itineraryId, request, userId);

        // Then
        assertNotNull(result);
        assertEquals(itineraryId, result.getItineraryId());
        assertEquals(userId, result.getUserId());
        assertEquals(1, result.getVersion());
        assertEquals("INR", result.getCurrency());
        assertEquals("Barcelona, Spain", result.getDestination());
        assertEquals("Mumbai, India", result.getOrigin());
        assertEquals("2025-11-01", result.getStartDate());
        assertEquals("2025-11-03", result.getEndDate());
        assertEquals(3, result.getThemes().size());
        assertTrue(result.getThemes().contains("culture"));
        assertTrue(result.getThemes().contains("food"));
        assertTrue(result.getThemes().contains("family-friendly"));
        assertNotNull(result.getDays());
        assertTrue(result.getDays().isEmpty()); // Initial itinerary has no days yet
        assertNotNull(result.getSettings());
        assertFalse(result.getSettings().isAutoApply());
        assertEquals("trip", result.getSettings().getDefaultScope());
        assertNotNull(result.getAgents());
        assertTrue(result.getAgents().containsKey("planner"));
        assertTrue(result.getAgents().containsKey("enrichment"));
        assertTrue(result.getSummary().contains("Barcelona, Spain"));

        // Verify itinerary was saved to Firestore
        ArgumentCaptor<NormalizedItinerary> itineraryCaptor = ArgumentCaptor.forClass(NormalizedItinerary.class);
        verify(itineraryJsonService, times(1)).createItinerary(itineraryCaptor.capture());
        NormalizedItinerary savedItinerary = itineraryCaptor.getValue();
        assertEquals(itineraryId, savedItinerary.getItineraryId());

        // Verify ownership was established
        ArgumentCaptor<TripMetadata> metadataCaptor = ArgumentCaptor.forClass(TripMetadata.class);
        verify(userDataService, times(1)).saveUserTripMetadata(eq(userId), metadataCaptor.capture());
        TripMetadata savedMetadata = metadataCaptor.getValue();
        assertEquals(itineraryId, savedMetadata.getItineraryId());
        assertEquals("Barcelona, Spain", savedMetadata.getDestination());
    }

    @Test
    @DisplayName("Should handle Firestore save failure")
    void testCreateInitialItinerary_FirestoreFailure() {
        // Given
        String itineraryId = "it_test_fail_firestore";
        String userId = "user_test_fail";
        CreateItineraryReq request = CreateItineraryReq.builder()
            .destination("Paris, France")
            .startDate(LocalDate.of(2025, 12, 1))
            .endDate(LocalDate.of(2025, 12, 3))
            .build();

        doThrow(new RuntimeException("Firestore connection failed"))
            .when(itineraryJsonService).createItinerary(any(NormalizedItinerary.class));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            initService.createInitialItinerary(itineraryId, request, userId);
        });

        assertTrue(exception.getMessage().contains("Initial itinerary creation failed"));
        assertTrue(exception.getCause().getMessage().contains("Firestore connection failed"));

        // Verify ownership was NOT established (transaction-like behavior)
        verify(userDataService, never()).saveUserTripMetadata(anyString(), any(TripMetadata.class));
    }

    @Test
    @DisplayName("Should handle UserDataService failure")
    void testCreateInitialItinerary_UserDataServiceFailure() {
        // Given
        String itineraryId = "it_test_fail_userdata";
        String userId = "user_test_fail_userdata";
        CreateItineraryReq request = CreateItineraryReq.builder()
            .destination("London, UK")
            .startDate(LocalDate.of(2026, 1, 1))
            .endDate(LocalDate.of(2026, 1, 3))
            .build();

        doNothing().when(itineraryJsonService).createItinerary(any(NormalizedItinerary.class));
        doThrow(new RuntimeException("User data save failed"))
            .when(userDataService).saveUserTripMetadata(anyString(), any(TripMetadata.class));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            initService.createInitialItinerary(itineraryId, request, userId);
        });

        assertTrue(exception.getMessage().contains("Initial itinerary creation failed"));
        assertTrue(exception.getCause().getMessage().contains("User data save failed"));

        // Verify itinerary WAS saved to Firestore (failure happened after)
        verify(itineraryJsonService, times(1)).createItinerary(any(NormalizedItinerary.class));
    }

    @Test
    @DisplayName("Should create initial structure with various request parameters")
    void testCreateInitialStructure_VariousParameters() {
        // Test Case 1: Minimal request
        CreateItineraryReq minimalRequest = CreateItineraryReq.builder()
            .destination("Tokyo, Japan")
            .startDate(LocalDate.of(2025, 6, 1))
            .endDate(LocalDate.of(2025, 6, 3))
            .build();

        doNothing().when(itineraryJsonService).createItinerary(any(NormalizedItinerary.class));
        doNothing().when(userDataService).saveUserTripMetadata(anyString(), any(TripMetadata.class));

        NormalizedItinerary result1 = initService.createInitialItinerary("it_minimal", minimalRequest, "user_minimal");
        assertNotNull(result1);
        assertEquals("Tokyo, Japan", result1.getDestination());
        assertNotNull(result1.getThemes());
        assertTrue(result1.getThemes().isEmpty()); // No interests provided

        // Test Case 2: Request with null interests
        CreateItineraryReq nullInterestsRequest = CreateItineraryReq.builder()
            .destination("Rome, Italy")
            .startDate(LocalDate.of(2025, 7, 1))
            .endDate(LocalDate.of(2025, 7, 3))
            .interests(null)
            .build();

        NormalizedItinerary result2 = initService.createInitialItinerary("it_null_interests", nullInterestsRequest, "user_null");
        assertNotNull(result2);
        assertNotNull(result2.getThemes());
        assertTrue(result2.getThemes().isEmpty());

        // Test Case 3: Request with multiple interests
        CreateItineraryReq multiInterestsRequest = CreateItineraryReq.builder()
            .destination("New York, USA")
            .startDate(LocalDate.of(2025, 8, 1))
            .endDate(LocalDate.of(2025, 8, 5))
            .interests(Arrays.asList("art", "museums", "theater", "shopping", "nightlife"))
            .build();

        NormalizedItinerary result3 = initService.createInitialItinerary("it_multi", multiInterestsRequest, "user_multi");
        assertNotNull(result3);
        assertEquals(5, result3.getThemes().size());
        assertTrue(result3.getThemes().contains("art"));
        assertTrue(result3.getThemes().contains("nightlife"));

        // Test Case 4: Request with party information
        CreateItineraryReq partyRequest = CreateItineraryReq.builder()
            .destination("Dubai, UAE")
            .startDate(LocalDate.of(2025, 9, 1))
            .endDate(LocalDate.of(2025, 9, 7))
            .party(PartyDto.builder().adults(4).children(2).infants(1).rooms(2).build())
            .build();

        NormalizedItinerary result4 = initService.createInitialItinerary("it_party", partyRequest, "user_party");
        assertNotNull(result4);
        assertEquals("Dubai, UAE", result4.getDestination());
    }

    @Test
    @DisplayName("Should set correct timestamps")
    void testTimestamps() {
        // Given
        String itineraryId = "it_timestamps";
        String userId = "user_timestamps";
        CreateItineraryReq request = CreateItineraryReq.builder()
            .destination("Singapore")
            .startDate(LocalDate.of(2025, 10, 1))
            .endDate(LocalDate.of(2025, 10, 3))
            .build();

        doNothing().when(itineraryJsonService).createItinerary(any(NormalizedItinerary.class));
        doNothing().when(userDataService).saveUserTripMetadata(anyString(), any(TripMetadata.class));

        long beforeCreation = System.currentTimeMillis();

        // When
        NormalizedItinerary result = initService.createInitialItinerary(itineraryId, request, userId);

        long afterCreation = System.currentTimeMillis();

        // Then
        assertNotNull(result.getCreatedAt());
        assertNotNull(result.getUpdatedAt());
        assertTrue(result.getCreatedAt() >= beforeCreation);
        assertTrue(result.getCreatedAt() <= afterCreation);
        assertEquals(result.getCreatedAt(), result.getUpdatedAt()); // Should be same for initial creation
    }

    @Test
    @DisplayName("Should handle null start location")
    void testNullStartLocation() {
        // Given
        String itineraryId = "it_null_start";
        String userId = "user_null_start";
        CreateItineraryReq request = CreateItineraryReq.builder()
            .destination("Amsterdam, Netherlands")
            .startLocation(null) // Null start location
            .startDate(LocalDate.of(2025, 11, 1))
            .endDate(LocalDate.of(2025, 11, 3))
            .build();

        doNothing().when(itineraryJsonService).createItinerary(any(NormalizedItinerary.class));
        doNothing().when(userDataService).saveUserTripMetadata(anyString(), any(TripMetadata.class));

        // When
        NormalizedItinerary result = initService.createInitialItinerary(itineraryId, request, userId);

        // Then
        assertNotNull(result);
        assertNull(result.getOrigin()); // Should be null if not provided
        assertEquals("Amsterdam, Netherlands", result.getDestination());
    }

    @Test
    @DisplayName("Should create TripMetadata correctly")
    void testTripMetadataCreation() {
        // Given
        String itineraryId = "it_metadata";
        String userId = "user_metadata";
        CreateItineraryReq request = CreateItineraryReq.builder()
            .destination("Sydney, Australia")
            .startDate(LocalDate.of(2025, 12, 1))
            .endDate(LocalDate.of(2025, 12, 10))
            .interests(Arrays.asList("beaches", "wildlife", "adventure"))
            .language("en")
            .build();

        doNothing().when(itineraryJsonService).createItinerary(any(NormalizedItinerary.class));
        doNothing().when(userDataService).saveUserTripMetadata(anyString(), any(TripMetadata.class));

        // When
        initService.createInitialItinerary(itineraryId, request, userId);

        // Then
        ArgumentCaptor<TripMetadata> metadataCaptor = ArgumentCaptor.forClass(TripMetadata.class);
        verify(userDataService, times(1)).saveUserTripMetadata(eq(userId), metadataCaptor.capture());
        
        TripMetadata metadata = metadataCaptor.getValue();
        assertEquals(itineraryId, metadata.getItineraryId());
        assertEquals("Sydney, Australia", metadata.getDestination());
        assertEquals("2025-12-01", metadata.getStartDate());
        assertEquals("2025-12-10", metadata.getEndDate());
        assertEquals("en", metadata.getLanguage());
        assertNotNull(metadata.getInterests());
        assertEquals(3, metadata.getInterests().size());
        assertTrue(metadata.getInterests().contains("beaches"));
        assertTrue(metadata.getSummary().contains("Sydney, Australia"));
    }

    @Test
    @DisplayName("Should include descriptive error messages")
    void testDescriptiveErrorMessages() {
        // Given
        String itineraryId = "it_error_msg";
        String userId = "user_error_msg";
        CreateItineraryReq request = CreateItineraryReq.builder()
            .destination("Berlin, Germany")
            .startDate(LocalDate.of(2026, 1, 1))
            .endDate(LocalDate.of(2026, 1, 3))
            .build();

        String detailedError = "Database connection timeout after 30 seconds";
        doThrow(new RuntimeException(detailedError))
            .when(itineraryJsonService).createItinerary(any(NormalizedItinerary.class));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            initService.createInitialItinerary(itineraryId, request, userId);
        });

        // Verify error message includes context
        String errorMessage = exception.getMessage();
        assertTrue(errorMessage.contains("Initial itinerary creation failed"));
        assertTrue(errorMessage.contains(detailedError));
        
        // Verify cause is preserved
        assertNotNull(exception.getCause());
        assertEquals(detailedError, exception.getCause().getMessage());
    }

    @Test
    @DisplayName("Should set agent status correctly")
    void testAgentStatusSetup() {
        // Given
        String itineraryId = "it_agent_status";
        String userId = "user_agent_status";
        CreateItineraryReq request = CreateItineraryReq.builder()
            .destination("Vienna, Austria")
            .startDate(LocalDate.of(2026, 2, 1))
            .endDate(LocalDate.of(2026, 2, 3))
            .build();

        doNothing().when(itineraryJsonService).createItinerary(any(NormalizedItinerary.class));
        doNothing().when(userDataService).saveUserTripMetadata(anyString(), any(TripMetadata.class));

        // When
        NormalizedItinerary result = initService.createInitialItinerary(itineraryId, request, userId);

        // Then
        assertNotNull(result.getAgents());
        assertEquals(2, result.getAgents().size());
        assertTrue(result.getAgents().containsKey("planner"));
        assertTrue(result.getAgents().containsKey("enrichment"));
        assertNotNull(result.getAgents().get("planner"));
        assertNotNull(result.getAgents().get("enrichment"));
    }
}
