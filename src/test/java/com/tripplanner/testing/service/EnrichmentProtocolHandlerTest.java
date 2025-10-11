package com.tripplanner.testing.service;

import com.tripplanner.dto.*;
import com.tripplanner.service.*;
import com.tripplanner.testing.BaseServiceTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * Atomic tests for EnrichmentProtocolHandler with mocked services.
 */
class EnrichmentProtocolHandlerTest extends BaseServiceTest {
    
    @Mock
    private PlaceEnrichmentService mockPlaceEnrichmentService;
    
    @Mock
    private GooglePlacesService mockGooglePlacesService;
    
    @Mock
    private PlaceRegistry mockPlaceRegistry;
    
    @Mock
    private IdempotencyManager mockIdempotencyManager;
    
    private EnrichmentProtocolHandler enrichmentProtocolHandler;
    
    @BeforeEach
    protected void setUp() {
        super.setUp();
        enrichmentProtocolHandler = new EnrichmentProtocolHandler(
            mockPlaceEnrichmentService,
            mockGooglePlacesService,
            mockPlaceRegistry,
            mockIdempotencyManager
        );
    }
    
    @Override
    protected void setupSpecificMocks() {
        // Setup specific mocks for EnrichmentProtocolHandler tests with lenient stubbing
        lenient().when(mockIdempotencyManager.getExistingOperation(anyString())).thenReturn(Optional.empty());
        lenient().doNothing().when(mockIdempotencyManager).storeOperationResult(anyString(), any(), anyString(), anyLong());
        // Note: PlaceRegistry methods will be mocked in individual tests as needed
    }
    
    @Test
    @DisplayName("Should process place details request successfully")
    void shouldProcessPlaceDetailsRequestSuccessfully() throws Exception {
        // Given
        EnrichmentRequest request = createPlaceDetailsRequest();
        PlaceDetails mockPlaceDetails = createMockPlaceDetails();
        
        when(mockIdempotencyManager.getExistingOperation(request.getIdempotencyKey()))
                .thenReturn(Optional.empty());
        when(mockGooglePlacesService.getPlaceDetails(anyString()))
                .thenReturn(mockPlaceDetails);
        doNothing().when(mockIdempotencyManager).storeOperationResult(anyString(), any(), anyString(), anyLong());
        
        // When
        CompletableFuture<EnrichmentResponse> future = enrichmentProtocolHandler.processRequest(request);
        EnrichmentResponse result = future.get();
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccessful()).isTrue();
        assertThat(result.getTraceId()).isEqualTo(request.getTraceId());
        assertThat(result.getIdempotencyKey()).isEqualTo(request.getIdempotencyKey());
        assertThat(result.getEnrichedData()).isNotEmpty();
        assertThat(result.getEnrichedData()).containsKey("rating");
        assertThat(result.getEnrichedData()).containsKey("photos");
        assertThat(result.getConfidence()).isGreaterThan(0.8);
        
        verify(mockGooglePlacesService).getPlaceDetails(anyString());
        
        logger.info("Process place details request test passed");
    }
    
    @Test
    @DisplayName("Should return cached response when available")
    void shouldReturnCachedResponseWhenAvailable() throws Exception {
        // Given
        EnrichmentRequest request = createPlaceDetailsRequest();
        EnrichmentResponse cachedResponse = EnrichmentResponse.success(
            request.getTraceId(),
            request.getIdempotencyKey(),
            Map.of("rating", 4.5, "cached", true),
            0.95
        );
        
        IdempotencyManager.IdempotencyRecord cachedRecord = 
                new IdempotencyManager.IdempotencyRecord(
                    request.getIdempotencyKey(), 
                    cachedResponse, 
                    "enrichment_request",
                    java.time.Instant.now(),
                    java.time.Instant.now().plusSeconds(3600)
                );
        
        when(mockIdempotencyManager.getExistingOperation(request.getIdempotencyKey()))
                .thenReturn(Optional.of(cachedRecord));
        
        // When
        CompletableFuture<EnrichmentResponse> future = enrichmentProtocolHandler.processRequest(request);
        EnrichmentResponse result = future.get();
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccessful()).isTrue();
        assertThat(result.getCacheHit()).isTrue();
        assertThat(result.getEnrichedData()).containsEntry("cached", true);
        
        verifyNoInteractions(mockGooglePlacesService);
        
        logger.info("Cached response test passed");
    }
    
    @Test
    @DisplayName("Should handle invalid request gracefully")
    void shouldHandleInvalidRequestGracefully() throws Exception {
        // Given
        EnrichmentRequest invalidRequest = new EnrichmentRequest();
        // Missing required fields like traceId, idempotencyKey, etc.
        
        // When
        CompletableFuture<EnrichmentResponse> future = enrichmentProtocolHandler.processRequest(invalidRequest);
        EnrichmentResponse result = future.get();
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccessful()).isFalse();
        assertThat(result.getErrors()).isNotEmpty();
        assertThat(result.getErrors().get(0).getCode()).isEqualTo("INVALID_REQUEST");
        assertThat(result.getErrors().get(0).getMessage()).contains("Request validation failed");
        
        verifyNoInteractions(mockGooglePlacesService);
        
        logger.info("Invalid request handling test passed");
    }
    
    @Test
    @DisplayName("Should handle Google Places service failure")
    void shouldHandleGooglePlacesServiceFailure() throws Exception {
        // Given
        EnrichmentRequest request = createPlaceDetailsRequest();
        
        when(mockIdempotencyManager.getExistingOperation(request.getIdempotencyKey()))
                .thenReturn(Optional.empty());
        when(mockGooglePlacesService.getPlaceDetails(anyString()))
                .thenThrow(new RuntimeException("Google Places API error"));
        
        // When
        CompletableFuture<EnrichmentResponse> future = enrichmentProtocolHandler.processRequest(request);
        EnrichmentResponse result = future.get();
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccessful()).isFalse();
        assertThat(result.getErrors()).isNotEmpty();
        assertThat(result.getErrors().get(0).getCode()).isEqualTo("PROCESSING_ERROR");
        assertThat(result.getErrors().get(0).getMessage()).contains("Failed to get place details");
        
        verify(mockGooglePlacesService).getPlaceDetails(anyString());
        
        logger.info("Google Places service failure test passed");
    }
    
    @Test
    @DisplayName("Should process request synchronously")
    void shouldProcessRequestSynchronously() {
        // Given
        EnrichmentRequest request = createPlaceDetailsRequest();
        PlaceDetails mockPlaceDetails = createMockPlaceDetails();
        
        when(mockIdempotencyManager.getExistingOperation(request.getIdempotencyKey()))
                .thenReturn(Optional.empty());
        when(mockGooglePlacesService.getPlaceDetails(anyString()))
                .thenReturn(mockPlaceDetails);
        
        // When
        EnrichmentResponse result = enrichmentProtocolHandler.processRequestSync(request);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccessful()).isTrue();
        assertThat(result.getTraceId()).isEqualTo(request.getTraceId());
        assertThat(result.getEnrichedData()).isNotEmpty();
        
        verify(mockGooglePlacesService).getPlaceDetails(anyString());
        
        logger.info("Synchronous processing test passed");
    }
    
    @Test
    @DisplayName("Should handle synchronous processing failure")
    void shouldHandleSynchronousProcessingFailure() {
        // Given
        EnrichmentRequest request = createPlaceDetailsRequest();
        
        when(mockIdempotencyManager.getExistingOperation(request.getIdempotencyKey()))
                .thenReturn(Optional.empty());
        when(mockGooglePlacesService.getPlaceDetails(anyString()))
                .thenThrow(new RuntimeException("Service unavailable"));
        
        // When
        EnrichmentResponse result = enrichmentProtocolHandler.processRequestSync(request);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccessful()).isFalse();
        assertThat(result.getErrors()).isNotEmpty();
        assertThat(result.getErrors().get(0).getCode()).isEqualTo("PROCESSING_ERROR");
        
        logger.info("Synchronous processing failure test passed");
    }
    
    @Test
    @DisplayName("Should get cached response directly")
    void shouldGetCachedResponseDirectly() {
        // Given
        String idempotencyKey = "test-idempotency-key";
        EnrichmentResponse cachedResponse = EnrichmentResponse.success(
            "trace-123",
            idempotencyKey,
            Map.of("rating", 4.2),
            0.9
        );
        
        IdempotencyManager.IdempotencyRecord cachedRecord = 
                new IdempotencyManager.IdempotencyRecord(
                    idempotencyKey, 
                    cachedResponse, 
                    "enrichment_request",
                    java.time.Instant.now(),
                    java.time.Instant.now().plusSeconds(3600)
                );
        
        when(mockIdempotencyManager.getExistingOperation(idempotencyKey))
                .thenReturn(Optional.of(cachedRecord));
        
        // When
        EnrichmentResponse result = enrichmentProtocolHandler.getCachedResponse(idempotencyKey);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getCacheHit()).isTrue();
        assertThat(result.getIdempotencyKey()).isEqualTo(idempotencyKey);
        assertThat(result.getEnrichedData()).containsEntry("rating", 4.2);
        
        verify(mockIdempotencyManager).getExistingOperation(idempotencyKey);
        
        logger.info("Get cached response directly test passed");
    }
    
    @Test
    @DisplayName("Should return null for non-existent cached response")
    void shouldReturnNullForNonExistentCachedResponse() {
        // Given
        String idempotencyKey = "non-existent-key";
        
        when(mockIdempotencyManager.getExistingOperation(idempotencyKey))
                .thenReturn(Optional.empty());
        
        // When
        EnrichmentResponse result = enrichmentProtocolHandler.getCachedResponse(idempotencyKey);
        
        // Then
        assertThat(result).isNull();
        
        verify(mockIdempotencyManager).getExistingOperation(idempotencyKey);
        
        logger.info("Non-existent cached response test passed");
    }
    
    @Test
    @DisplayName("Should return null for null idempotency key")
    void shouldReturnNullForNullIdempotencyKey() {
        // When
        EnrichmentResponse result = enrichmentProtocolHandler.getCachedResponse(null);
        
        // Then
        assertThat(result).isNull();
        
        verifyNoInteractions(mockIdempotencyManager);
        
        logger.info("Null idempotency key test passed");
    }
    
    @Test
    @DisplayName("Should get ENRICHMENT protocol stats")
    void shouldGetEnrichmentProtocolStats() {
        // When
        EnrichmentProtocolHandler.EnrichmentProtocolStats stats = enrichmentProtocolHandler.getStats();
        
        // Then
        assertThat(stats).isNotNull();
        assertThat(stats.getActiveRequests()).isEqualTo(0);
        assertThat(stats.getCachedResponses()).isEqualTo(0);
        assertThat(stats.getTotalCacheEntries()).isEqualTo(0);
        
        logger.info("Get ENRICHMENT protocol stats test passed");
    }
    
    @Test
    @DisplayName("Should cleanup cache through idempotency manager")
    void shouldCleanupCacheThroughIdempotencyManager() {
        // Given
        doNothing().when(mockIdempotencyManager).cleanupExpiredRecords();
        
        // When
        enrichmentProtocolHandler.cleanupCache();
        
        // Then
        verify(mockIdempotencyManager).cleanupExpiredRecords();
        
        logger.info("Cache cleanup test passed");
    }
    
    @Test
    @DisplayName("Should handle unsupported ENRICHMENT type")
    void shouldHandleUnsupportedEnrichmentType() throws Exception {
        // Given
        EnrichmentRequest request = createUnsupportedTypeRequest();
        
        // When
        CompletableFuture<EnrichmentResponse> future = enrichmentProtocolHandler.processRequest(request);
        EnrichmentResponse result = future.get();
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccessful()).isFalse();
        assertThat(result.getErrors()).isNotEmpty();
        assertThat(result.getErrors().get(0).getCode()).isEqualTo("INVALID_REQUEST");
        assertThat(result.getErrors().get(0).getMessage()).contains("Request validation failed");
        
        verifyNoInteractions(mockGooglePlacesService);
        
        logger.info("Unsupported ENRICHMENT type test passed");
    }
    
    @Test
    @DisplayName("Should handle partial success with missing data")
    void shouldHandlePartialSuccessWithMissingData() throws Exception {
        // Given
        EnrichmentRequest request = createPlaceDetailsRequest();
        PlaceDetails mockPlaceDetails = createMockPlaceDetailsWithMissingData();
        
        when(mockIdempotencyManager.getExistingOperation(request.getIdempotencyKey()))
                .thenReturn(Optional.empty());
        when(mockGooglePlacesService.getPlaceDetails(anyString()))
                .thenReturn(mockPlaceDetails);
        
        // When
        CompletableFuture<EnrichmentResponse> future = enrichmentProtocolHandler.processRequest(request);
        EnrichmentResponse result = future.get();
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccessful()).isTrue(); // Still successful but partial
        assertThat(result.getEnrichedData()).containsKey("rating");
        assertThat(result.getPartialResults()).isNotEmpty(); // Should have partial results for missing data
        assertThat(result.getConfidence()).isLessThan(0.9); // Lower confidence due to partial data
        
        verify(mockGooglePlacesService).getPlaceDetails(anyString());
        
        logger.info("Partial success test passed");
    }
    
    // Helper methods to create test data
    
    private EnrichmentRequest createPlaceDetailsRequest() {
        EnrichmentRequest request = new EnrichmentRequest();
        request.setTraceId("trace-123");
        request.setIdempotencyKey("idempotent-key-456");
        request.setRequestType(EnrichmentRequest.EnrichmentType.PLACE_DETAILS);
        request.setRequestedFields(Arrays.asList("rating", "photos", "reviews", "openingHours"));
        request.setContext(Map.of("placeId", "ChIJoQ8Q6NNB0i0RkOYkS7EPkSQ"));
        request.setTargetNodeId("node-123");
        request.setItineraryId("itinerary-456");
        request.setCreatedAt(System.currentTimeMillis());
        request.setTimeoutMs(30000L); // 30 seconds timeout
        return request;
    }
    
    private EnrichmentRequest createUnsupportedTypeRequest() {
        EnrichmentRequest request = new EnrichmentRequest();
        request.setTraceId("trace-456");
        request.setIdempotencyKey("idempotent-key-789");
        // Set an unsupported type by using reflection or creating a custom enum value
        // For testing purposes, we'll use null which should trigger unsupported handling
        request.setRequestType(null);
        request.setRequestedFields(Arrays.asList("unsupported_field"));
        request.setContext(Map.of());
        request.setTargetNodeId("node-456");
        request.setItineraryId("itinerary-789");
        request.setCreatedAt(System.currentTimeMillis());
        request.setTimeoutMs(30000L); // 30 seconds timeout
        return request;
    }
    
    private PlaceDetails createMockPlaceDetails() {
        PlaceDetails placeDetails = new PlaceDetails();
        placeDetails.setName("Test Place");
        placeDetails.setRating(4.5);
        placeDetails.setPlaceId("ChIJoQ8Q6NNB0i0RkOYkS7EPkSQ");
        
        // Add photos
        Photo photo1 = new Photo();
        photo1.setPhotoReference("photo-ref-1");
        photo1.setWidth(1024);
        photo1.setHeight(768);
        placeDetails.setPhotos(Arrays.asList(photo1));
        
        // Add reviews
        Review review1 = new Review();
        review1.setAuthorName("Test Reviewer");
        review1.setRating(5);
        review1.setText("Great place!");
        placeDetails.setReviews(Arrays.asList(review1));
        
        // Add opening hours
        PlaceDetails.OpeningHours openingHours = new PlaceDetails.OpeningHours();
        openingHours.setOpenNow(true);
        placeDetails.setOpeningHours(openingHours);
        
        return placeDetails;
    }
    
    private PlaceDetails createMockPlaceDetailsWithMissingData() {
        PlaceDetails placeDetails = new PlaceDetails();
        placeDetails.setName("Test Place");
        placeDetails.setRating(4.2);
        placeDetails.setPlaceId("ChIJoQ8Q6NNB0i0RkOYkS7EPkSQ");
        
        // Missing photos, reviews, and opening hours to trigger partial results
        placeDetails.setPhotos(null);
        placeDetails.setReviews(null);
        placeDetails.setOpeningHours(null);
        
        return placeDetails;
    }
}