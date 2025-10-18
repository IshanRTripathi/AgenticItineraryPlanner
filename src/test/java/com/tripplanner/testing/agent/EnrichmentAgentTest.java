package com.tripplanner.testing.agent;

import com.tripplanner.agents.BaseAgent;
import com.tripplanner.agents.EnrichmentAgent;
import com.tripplanner.dto.*;
import com.tripplanner.service.*;
import com.tripplanner.testing.BaseServiceTest;
import com.tripplanner.testing.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Atomic tests for EnrichmentAgent with mocked external services.
 */
class EnrichmentAgentTest extends BaseServiceTest {
    
    @Mock
    private AgentEventBus mockEventBus;
    
    @Mock
    private ItineraryJsonService mockItineraryJsonService;
    
    @Mock
    private ChangeEngine mockChangeEngine;
    
    @Mock
    private GooglePlacesService mockGooglePlacesService;
    
    @Mock
    private EnrichmentProtocolHandler mockEnrichmentProtocolHandler;
    
    private EnrichmentAgent enrichmentAgent;
    private TestDataFactory testDataFactory;
    
    @BeforeEach
    protected void setUp() {
        super.setUp();
        testDataFactory = new TestDataFactory(new com.fasterxml.jackson.databind.ObjectMapper());
        
        enrichmentAgent = new EnrichmentAgent(
            mockEventBus,
            mockItineraryJsonService,
            mockChangeEngine,
            mockGooglePlacesService,
            mockEnrichmentProtocolHandler
        );
    }
    
    @Override
    protected void setupSpecificMocks() {
        // Setup event bus mock - use lenient to avoid unnecessary stubbing exceptions
        lenient().doNothing().when(mockEventBus).publish(anyString(), any(AgentEvent.class));
    }
    
    @Test
    @DisplayName("Should get ENRICHMENT agent capabilities")
    void shouldGetEnrichmentAgentCapabilities() {
        // When
        AgentCapabilities capabilities = enrichmentAgent.getCapabilities();
        
        // Then
        assertThat(capabilities).isNotNull();
        assertThat(capabilities.getSupportedTasks()).contains("enrich"); // Single task type
        assertThat(capabilities.getSupportedTasks()).hasSize(1); // Zero-overlap design
        assertThat(capabilities.getPriority()).isEqualTo(20); // Medium priority
        assertThat(capabilities.isChatEnabled()).isTrue(); // Chat-enabled
        assertThat(capabilities.getConfigurationValue("requiresExternalAPI")).isEqualTo(true);
        assertThat(capabilities.getConfigurationValue("canModifyPlaces")).isEqualTo(true);
        assertThat(capabilities.getConfigurationValue("canAddDetails")).isEqualTo(true);
        assertThat(capabilities.getConfigurationValue("respectsLocks")).isEqualTo(true);
        
        logger.info("Get ENRICHMENT agent capabilities test passed");
    }
    
    @Test
    @DisplayName("Should handle ENRICHMENT task types")
    void shouldHandleEnrichmentTaskTypes() {
        // When/Then
        assertThat(enrichmentAgent.canHandle("enrich")).isTrue(); // Only handles "enrich"
        assertThat(enrichmentAgent.canHandle("ENRICHMENT")).isFalse(); // No longer supported (zero-overlap, case-sensitive)
        assertThat(enrichmentAgent.canHandle("validate")).isFalse(); // No longer supported (zero-overlap)
        assertThat(enrichmentAgent.canHandle("enhance")).isFalse(); // No longer supported (zero-overlap)
        assertThat(enrichmentAgent.canHandle("booking")).isFalse();
        
        logger.info("Handle ENRICHMENT task types test passed");
    }
    
    @Test
    @DisplayName("Should handle ENRICHMENT task with place context")
    void shouldHandleEnrichmentTaskWithPlaceContext() {
        // Given - context is no longer used for validation in simplified canHandle
        Map<String, Object> placeContext = Map.of("operation", "enrich_place");
        Map<String, Object> nonPlaceContext = Map.of("operation", "book_hotel");
        
        // When/Then - canHandle only checks taskType now, not context
        assertThat(enrichmentAgent.canHandle("enrich", placeContext)).isTrue();
        assertThat(enrichmentAgent.canHandle("enrich", nonPlaceContext)).isTrue(); // Still true, only taskType matters
        assertThat(enrichmentAgent.canHandle("book", placeContext)).isFalse(); // Wrong taskType
        
        logger.info("Handle ENRICHMENT task with place context test passed");
    }
    
    @Test
    @DisplayName("Should process ENRICHMENT request via protocol")
    void shouldProcessEnrichmentRequestViaProtocol() {
        // Given
        EnrichmentRequest enrichmentRequest = createPlaceDetailsEnrichmentRequest();
        
        EnrichmentResponse expectedResponse = EnrichmentResponse.success(
            enrichmentRequest.getTraceId(),
            enrichmentRequest.getIdempotencyKey(),
            Map.of("rating", 4.5, "photos", Arrays.asList("photo1", "photo2")),
            0.9
        );
        
        when(mockEnrichmentProtocolHandler.processRequestSync(enrichmentRequest))
                .thenReturn(expectedResponse);
        
        // When
        EnrichmentResponse result = enrichmentAgent.processEnrichmentRequest(enrichmentRequest);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccessful()).isTrue();
        assertThat(result.getTraceId()).isEqualTo(enrichmentRequest.getTraceId());
        assertThat(result.getEnrichedData()).containsKey("rating");
        assertThat(result.getConfidence()).isEqualTo(0.9);
        
        verify(mockEnrichmentProtocolHandler).processRequestSync(enrichmentRequest);
        
        logger.info("Process ENRICHMENT request via protocol test passed");
    }
    
    @Test
    @DisplayName("Should handle unsupported ENRICHMENT request type")
    void shouldHandleUnsupportedEnrichmentRequestType() {
        // Given
        EnrichmentRequest unsupportedRequest = createUnsupportedEnrichmentRequest();
        
        // When
        EnrichmentResponse result = enrichmentAgent.processEnrichmentRequest(unsupportedRequest);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccessful()).isFalse();
        assertThat(result.getErrors()).isNotEmpty();
        assertThat(result.getErrors().get(0).getCode()).isEqualTo("AGENT_CANNOT_HANDLE");
        
        verifyNoInteractions(mockEnrichmentProtocolHandler);
        
        logger.info("Handle unsupported ENRICHMENT request type test passed");
    }
    
    @Test
    @DisplayName("Should execute ENRICHMENT agent successfully")
    void shouldExecuteEnrichmentAgentSuccessfully() {
        // Given
        String itineraryId = "test-itinerary-001";
        NormalizedItinerary testItinerary = testDataFactory.createBaliLuxuryItinerary();
        Map<String, Object> data = Map.of("taskType", "enrich", "operation", "enrich_places");
        BaseAgent.AgentRequest<ChangeEngine.ApplyResult> request = 
                new BaseAgent.AgentRequest<>(data, ChangeEngine.ApplyResult.class);
        
        // Setup mocks
        when(mockItineraryJsonService.getItinerary(itineraryId))
                .thenReturn(Optional.of(testItinerary));
        
        ChangeEngine.ApplyResult mockResult = new ChangeEngine.ApplyResult(2, new ItineraryDiff());
        when(mockChangeEngine.apply(eq(itineraryId), any(ChangeSet.class)))
                .thenReturn(mockResult);
        
        // When
        ChangeEngine.ApplyResult result = enrichmentAgent.execute(itineraryId, request);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getToVersion()).isEqualTo(2);
        
        verify(mockItineraryJsonService).getItinerary(itineraryId);
        verify(mockChangeEngine).apply(eq(itineraryId), any(ChangeSet.class));
        verify(mockEventBus, atLeast(3)).publish(eq(itineraryId), any(AgentEvent.class));
        
        logger.info("Execute ENRICHMENT agent successfully test passed");
    }
    
    @Test
    @DisplayName("Should handle itinerary not found")
    void shouldHandleItineraryNotFound() {
        // Given
        String itineraryId = "non-existent-itinerary";
        Map<String, Object> data = Map.of("taskType", "enrich");
        BaseAgent.AgentRequest<ChangeEngine.ApplyResult> request = 
                new BaseAgent.AgentRequest<>(data, ChangeEngine.ApplyResult.class);
        
        when(mockItineraryJsonService.getItinerary(itineraryId))
                .thenReturn(Optional.empty());
        
        // When/Then
        assertThat(org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () -> {
            enrichmentAgent.execute(itineraryId, request);
        })).hasMessageContaining("Itinerary not found");
        
        verify(mockItineraryJsonService).getItinerary(itineraryId);
        verifyNoInteractions(mockChangeEngine);
        
        logger.info("Handle itinerary not found test passed");
    }
    
    @Test
    @DisplayName("Should handle ENRICHMENT protocol failure")
    void shouldHandleEnrichmentProtocolFailure() {
        // Given
        EnrichmentRequest enrichmentRequest = createPlaceDetailsEnrichmentRequest();
        
        when(mockEnrichmentProtocolHandler.processRequestSync(enrichmentRequest))
                .thenThrow(new RuntimeException("Protocol handler error"));
        
        // When
        EnrichmentResponse result = enrichmentAgent.processEnrichmentRequest(enrichmentRequest);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccessful()).isFalse();
        assertThat(result.getErrors()).isNotEmpty();
        assertThat(result.getErrors().get(0).getCode()).isEqualTo("PROCESSING_ERROR");
        assertThat(result.getErrors().get(0).getMessage()).contains("Failed to process ENRICHMENT request");
        
        verify(mockEnrichmentProtocolHandler).processRequestSync(enrichmentRequest);
        
        logger.info("Handle ENRICHMENT protocol failure test passed");
    }
    
    @Test
    @DisplayName("Should execute with no enrichments needed")
    void shouldExecuteWithNoEnrichmentsNeeded() {
        // Given
        String itineraryId = "test-itinerary-001";
        NormalizedItinerary emptyItinerary = new NormalizedItinerary();
        emptyItinerary.setItineraryId(itineraryId);
        emptyItinerary.setDays(Arrays.asList()); // Empty days
        
        Map<String, Object> data = Map.of("taskType", "enrich");
        BaseAgent.AgentRequest<ChangeEngine.ApplyResult> request = 
                new BaseAgent.AgentRequest<>(data, ChangeEngine.ApplyResult.class);
        
        when(mockItineraryJsonService.getItinerary(itineraryId))
                .thenReturn(Optional.of(emptyItinerary));
        
        // When
        ChangeEngine.ApplyResult result = enrichmentAgent.execute(itineraryId, request);
        
        // Then
        assertThat(result).isNull(); // No enrichments applied
        
        verify(mockItineraryJsonService).getItinerary(itineraryId);
        verifyNoInteractions(mockChangeEngine); // No changes to apply
        
        logger.info("Execute with no enrichments needed test passed");
    }
    
    @Test
    @DisplayName("Should handle change engine failure")
    void shouldHandleChangeEngineFailure() {
        // Given
        String itineraryId = "test-itinerary-001";
        NormalizedItinerary testItinerary = testDataFactory.createBaliLuxuryItinerary();
        Map<String, Object> data = Map.of("taskType", "enrich");
        BaseAgent.AgentRequest<ChangeEngine.ApplyResult> request = 
                new BaseAgent.AgentRequest<>(data, ChangeEngine.ApplyResult.class);
        
        when(mockItineraryJsonService.getItinerary(itineraryId))
                .thenReturn(Optional.of(testItinerary));
        when(mockChangeEngine.apply(eq(itineraryId), any(ChangeSet.class)))
                .thenThrow(new RuntimeException("Change engine error"));
        
        // When/Then
        assertThat(org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () -> {
            enrichmentAgent.execute(itineraryId, request);
        })).hasMessageContaining("Failed to enrich itinerary");
        
        verify(mockItineraryJsonService).getItinerary(itineraryId);
        verify(mockChangeEngine).apply(eq(itineraryId), any(ChangeSet.class));
        
        logger.info("Handle change engine failure test passed");
    }
    
    // Helper methods to create test data
    
    private EnrichmentRequest createPlaceDetailsEnrichmentRequest() {
        EnrichmentRequest request = new EnrichmentRequest();
        request.setTraceId("trace-123");
        request.setIdempotencyKey("idempotent-key-456");
        request.setRequestType(EnrichmentRequest.EnrichmentType.PLACE_DETAILS);
        request.setTargetNodeId("node-123");
        request.setItineraryId("test-itinerary-001");
        request.setUserId("test-user-123");
        request.setRequestedFields(Arrays.asList("rating", "photos", "reviews"));
        request.setContext(Map.of(
            "placeId", "ChIJoQ8Q6NNB0i0RkOYkS7EPkSQ",
            "operation", "enrich_place_details"
        ));
        return request;
    }
    
    private EnrichmentRequest createUnsupportedEnrichmentRequest() {
        EnrichmentRequest request = new EnrichmentRequest();
        request.setTraceId("trace-456");
        request.setIdempotencyKey("idempotent-key-789");
        // Use a type that the agent cannot handle
        request.setRequestType(EnrichmentRequest.EnrichmentType.CANONICAL_MAPPING);
        request.setContext(Map.of("operation", "book_hotel")); // Non-place operation
        return request;
    }
}