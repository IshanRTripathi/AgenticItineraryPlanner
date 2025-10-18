package com.tripplanner.testing.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripplanner.dto.*;
import com.tripplanner.service.*;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Utility class for common service test setup and verification patterns.
 * Provides reusable mock configurations and assertion helpers.
 */
public class ServiceTestUtilities {
    
    private static final Logger logger = LoggerFactory.getLogger(ServiceTestUtilities.class);
    
    /**
     * Setup common mocks for ItineraryJsonService.
     */
    public static void setupItineraryJsonServiceMocks(ItineraryJsonService mockService, 
                                                     NormalizedItinerary testItinerary) {
        logger.debug("Setting up ItineraryJsonService mocks");
        
        // Mock getItinerary
        when(mockService.getItinerary(testItinerary.getItineraryId()))
                .thenReturn(Optional.of(testItinerary));
        
        // Mock updateItinerary
        when(mockService.updateItinerary(any(NormalizedItinerary.class)))
                .thenAnswer(invocation -> {
                    NormalizedItinerary updated = invocation.getArgument(0);
                    return createMockFirestoreItinerary(updated);
                });
        
        // Mock createItinerary
        when(mockService.createItinerary(any(NormalizedItinerary.class)))
                .thenAnswer(invocation -> {
                    NormalizedItinerary created = invocation.getArgument(0);
                    return createMockFirestoreItinerary(created);
                });
        
        // Mock agent data operations
        when(mockService.hasAgentData(anyString(), anyString())).thenReturn(false);
        when(mockService.getAgentData(anyString(), anyString())).thenReturn(Optional.empty());
    }
    
    /**
     * Setup common mocks for LLMService.
     */
    public static void setupLLMServiceMocks(LLMService mockService) {
        logger.debug("Setting up LLMService mocks");
        
        // Mock generateResponse
        when(mockService.generateResponse(anyString(), anyString(), any()))
                .thenReturn("Mock LLM response");
        
        // Mock classifyIntent
        IntentResult mockIntentResult = new IntentResult();
        mockIntentResult.setIntent("EDIT");
        mockIntentResult.setTaskType("edit");
        mockIntentResult.setConfidence(0.9);
        when(mockService.classifyIntent(anyString(), anyString()))
                .thenReturn(mockIntentResult);
        
        // Mock generateChangeSet
        ChangeSet mockChangeSet = new ChangeSet();
        mockChangeSet.setScope("day");
        mockChangeSet.setDay(1);
        mockChangeSet.setOps(Arrays.asList());
        when(mockService.generateChangeSet(anyString(), anyString()))
                .thenReturn(mockChangeSet);
        
        // Mock provider availability
        when(mockService.getAvailableProviders()).thenReturn(Arrays.asList("gemini", "qwen2.5-7b"));
        when(mockService.isModelSupported(anyString())).thenReturn(true);
    }
    
    /**
     * Setup common mocks for GooglePlacesService.
     */
    public static void setupGooglePlacesServiceMocks(GooglePlacesService mockService) {
        logger.debug("Setting up GooglePlacesService mocks");
        
        PlaceDetails mockPlaceDetails = createMockPlaceDetails();
        
        // Mock getPlaceDetails
        when(mockService.getPlaceDetails(anyString())).thenReturn(mockPlaceDetails);
        
        // Mock getPlacePhotos
        when(mockService.getPlacePhotos(anyString())).thenReturn(mockPlaceDetails.getPhotos());
        
        // Mock getPlaceReviews
        when(mockService.getPlaceReviews(anyString())).thenReturn(mockPlaceDetails.getReviews());
    }
    
    /**
     * Setup common mocks for AgentCoordinator.
     */
    public static void setupAgentCoordinatorMocks(AgentCoordinator mockCoordinator) {
        logger.debug("Setting up AgentCoordinator mocks");
        
        // Mock routeTask - return null by default (no agent found)
        when(mockCoordinator.routeTask(anyString(), any())).thenReturn(null);
        
        // Mock getCapableAgents
        when(mockCoordinator.getCapableAgents(anyString())).thenReturn(Arrays.asList());
        
        // Mock analyzeAgentConflicts
        when(mockCoordinator.analyzeAgentConflicts(anyString())).thenReturn(null);
    }
    
    /**
     * Setup common mocks for ChangeEngine.
     */
    public static void setupChangeEngineMocks(ChangeEngine mockEngine) {
        logger.debug("Setting up ChangeEngine mocks");
        
        // Mock propose
        ChangeEngine.ProposeResult mockProposeResult = mock(ChangeEngine.ProposeResult.class);
        when(mockProposeResult.getPreviewVersion()).thenReturn(2);
        when(mockProposeResult.getDiff()).thenReturn(new ItineraryDiff());
        try {
            when(mockEngine.propose(anyString(), any(ChangeSet.class))).thenReturn(mockProposeResult);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        
        // Mock apply
        ChangeEngine.ApplyResult mockApplyResult = new ChangeEngine.ApplyResult(2, new ItineraryDiff());
        when(mockEngine.apply(anyString(), any(ChangeSet.class))).thenReturn(mockApplyResult);
        
        // Mock undo
        ChangeEngine.UndoResult mockUndoResult = new ChangeEngine.UndoResult(1, new ItineraryDiff());
        when(mockEngine.undo(anyString(), anyInt())).thenReturn(mockUndoResult);
    }
    
    /**
     * Setup common mocks for BookingService.
     */
    public static void setupBookingServiceMocks(BookingService mockService) {
        logger.debug("Setting up BookingService mocks");
        
        // Mock createRazorpayOrder
        when(mockService.createRazorpayOrder(any())).thenAnswer(invocation -> {
            var request = invocation.getArgument(0);
            return createMockRazorpayOrderResponse();
        });
        
        // Mock executeProviderBooking
        when(mockService.executeProviderBooking(anyString(), anyString(), anyString(), any()))
                .thenReturn(createMockBookingResponse());
        
        // Mock getBooking
        when(mockService.getBooking(anyString(), anyString()))
                .thenReturn(createMockBookingResponse());
        
        // Mock getUserBookings
        when(mockService.getUserBookings(anyInt(), anyInt()))
                .thenReturn(Arrays.asList(createMockBookingResponse()));
        
        // Mock handleRazorpayWebhook
        doNothing().when(mockService).handleRazorpayWebhook(any(), anyString());
    }
    
    /**
     * Create argument matcher for NormalizedItinerary with specific version.
     */
    public static ArgumentMatcher<NormalizedItinerary> hasVersion(int expectedVersion) {
        return itinerary -> itinerary != null && itinerary.getVersion() == expectedVersion;
    }
    
    /**
     * Create argument matcher for ChangeSet with specific scope.
     */
    public static ArgumentMatcher<ChangeSet> hasScope(String expectedScope) {
        return changeSet -> changeSet != null && expectedScope.equals(changeSet.getScope());
    }
    
    /**
     * Create argument matcher for EnrichmentRequest with specific type.
     */
    public static ArgumentMatcher<EnrichmentRequest> hasRequestType(EnrichmentRequest.EnrichmentType expectedType) {
        return request -> request != null && expectedType.equals(request.getRequestType());
    }
    
    /**
     * Create argument matcher for Map containing specific key-value pair.
     */
    public static ArgumentMatcher<Map<String, Object>> containsEntry(String key, Object value) {
        return map -> map != null && value.equals(map.get(key));
    }
    
    /**
     * Create argument matcher for List with specific size.
     */
    public static ArgumentMatcher<List<?>> hasSize(int expectedSize) {
        return list -> list != null && list.size() == expectedSize;
    }
    
    /**
     * Verify that no unexpected interactions occurred with service mocks.
     */
    public static void verifyNoUnexpectedInteractions(Object... mocks) {
        for (Object mock : mocks) {
            verifyNoMoreInteractions(mock);
        }
        logger.debug("Verified no unexpected interactions with {} mocks", mocks.length);
    }
    
    /**
     * Create a predicate for filtering test data based on field values.
     */
    public static <T> Predicate<T> fieldEquals(String fieldName, Object expectedValue) {
        return item -> {
            try {
                var field = item.getClass().getDeclaredField(fieldName);
                field.setAccessible(true);
                Object actualValue = field.get(item);
                return expectedValue.equals(actualValue);
            } catch (Exception e) {
                logger.warn("Failed to access field {} on {}", fieldName, item.getClass().getSimpleName());
                return false;
            }
        };
    }
    
    /**
     * Assert that an itinerary has the expected structure and field values.
     */
    public static void assertItineraryStructure(NormalizedItinerary itinerary, 
                                               String expectedId, 
                                               int expectedVersion,
                                               int expectedDayCount) {
        assert itinerary != null : "Itinerary should not be null";
        assert expectedId.equals(itinerary.getItineraryId()) : 
            "Expected itinerary ID " + expectedId + " but got " + itinerary.getItineraryId();
        assert itinerary.getVersion() == expectedVersion : 
            "Expected version " + expectedVersion + " but got " + itinerary.getVersion();
        assert itinerary.getDays() != null : "Days should not be null";
        assert itinerary.getDays().size() == expectedDayCount : 
            "Expected " + expectedDayCount + " days but got " + itinerary.getDays().size();
        
        logger.debug("Itinerary structure assertion passed for {}", expectedId);
    }
    
    /**
     * Assert that a change set has the expected structure.
     */
    public static void assertChangeSetStructure(ChangeSet changeSet, 
                                              String expectedScope, 
                                              int expectedOpCount) {
        assert changeSet != null : "ChangeSet should not be null";
        assert expectedScope.equals(changeSet.getScope()) : 
            "Expected scope " + expectedScope + " but got " + changeSet.getScope();
        assert changeSet.getOps() != null : "Operations should not be null";
        assert changeSet.getOps().size() == expectedOpCount : 
            "Expected " + expectedOpCount + " operations but got " + changeSet.getOps().size();
        
        logger.debug("ChangeSet structure assertion passed");
    }
    
    /**
     * Assert that an ENRICHMENT response has the expected structure.
     */
    public static void assertEnrichmentResponseStructure(EnrichmentResponse response,
                                                       boolean expectedSuccess,
                                                       String expectedTraceId) {
        assert response != null : "EnrichmentResponse should not be null";
        assert response.isSuccessful() == expectedSuccess : 
            "Expected success " + expectedSuccess + " but got " + response.isSuccessful();
        assert expectedTraceId.equals(response.getTraceId()) : 
            "Expected trace ID " + expectedTraceId + " but got " + response.getTraceId();
        
        if (expectedSuccess) {
            assert response.getEnrichedData() != null : "Enriched data should not be null for successful response";
            assert response.getConfidence() > 0 : "Confidence should be positive for successful response";
        } else {
            assert response.getErrors() != null && !response.getErrors().isEmpty() : 
                "Errors should be present for failed response";
        }
        
        logger.debug("EnrichmentResponse structure assertion passed");
    }
    
    // Helper methods to create mock objects
    
    private static com.tripplanner.data.entity.FirestoreItinerary createMockFirestoreItinerary(NormalizedItinerary itinerary) {
        return new com.tripplanner.data.entity.FirestoreItinerary(
            itinerary.getItineraryId(),
            itinerary.getVersion(),
            "mock-json-data"
        );
    }
    
    private static PlaceDetails createMockPlaceDetails() {
        PlaceDetails placeDetails = new PlaceDetails();
        placeDetails.setName("Mock Place");
        placeDetails.setRating(4.5);
        placeDetails.setPlaceId("mock-place-id");
        
        Photo photo = new Photo();
        photo.setPhotoReference("mock-photo-ref");
        photo.setWidth(1024);
        photo.setHeight(768);
        placeDetails.setPhotos(Arrays.asList(photo));
        
        Review review = new Review();
        review.setAuthorName("Mock Reviewer");
        review.setRating(5);
        review.setText("Great place!");
        placeDetails.setReviews(Arrays.asList(review));
        
        return placeDetails;
    }
    
    private static com.tripplanner.controller.BookingController.RazorpayOrderRes createMockRazorpayOrderResponse() {
        return new com.tripplanner.controller.BookingController.RazorpayOrderRes(
            "order_mock123",
            50000L,
            "INR",
            "receipt_mock"
        );
    }
    
    private static com.tripplanner.controller.BookingController.BookingRes createMockBookingResponse() {
        return new com.tripplanner.controller.BookingController.BookingRes(
            "booking_123",
            "CONFIRMED",
            "CONF-MOCK123",
            "it_mock_001",
            java.time.Instant.now(),
            Map.of("provider", "mock-provider")
        );
    }
}