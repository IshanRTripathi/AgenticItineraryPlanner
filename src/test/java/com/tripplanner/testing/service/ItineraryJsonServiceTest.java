package com.tripplanner.testing.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripplanner.data.entity.FirestoreItinerary;
import com.tripplanner.dto.NormalizedItinerary;
import com.tripplanner.service.DatabaseService;
import com.tripplanner.service.ItineraryJsonService;
import com.tripplanner.service.MapBoundsCalculator;
import com.tripplanner.testing.BaseServiceTest;
import com.tripplanner.testing.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Atomic tests for ItineraryJsonService with comprehensive mocking.
 */
class ItineraryJsonServiceTest extends BaseServiceTest {
    
    @Mock
    private MapBoundsCalculator mapBoundsCalculator;
    
    private ItineraryJsonService itineraryJsonService;
    
    private TestDataFactory testDataFactory;
    
    @BeforeEach
    protected void setUp() {
        super.setUp();
        testDataFactory = new TestDataFactory(new ObjectMapper());
        
        // Manually create service and inject dependencies
        itineraryJsonService = new ItineraryJsonService(objectMapper);
        // Use reflection to inject @Autowired fields
        try {
            java.lang.reflect.Field databaseField = ItineraryJsonService.class.getDeclaredField("databaseService");
            databaseField.setAccessible(true);
            databaseField.set(itineraryJsonService, databaseService);
            
            java.lang.reflect.Field mapBoundsField = ItineraryJsonService.class.getDeclaredField("mapBoundsCalculator");
            mapBoundsField.setAccessible(true);
            mapBoundsField.set(itineraryJsonService, mapBoundsCalculator);
        } catch (Exception e) {
            logger.error("Failed to inject dependencies", e);
        }
    }
    
    @Override
    protected void setupSpecificMocks() {
        // Setup specific mocks for ItineraryJsonService tests
    }
    
    @Test
    @DisplayName("Should create itinerary with complete field validation")
    void shouldCreateItineraryWithCompleteFieldValidation() throws JsonProcessingException {
        // Given
        NormalizedItinerary testItinerary = testDataFactory.createBaliLuxuryItinerary();
        String expectedJson = "serialized-json";
        FirestoreItinerary expectedEntity = new FirestoreItinerary("it_bali_luxury_3d_001", 1, expectedJson);
        
        when(objectMapper.writeValueAsString(testItinerary)).thenReturn(expectedJson);
        when(databaseService.save(any(FirestoreItinerary.class))).thenReturn(expectedEntity);
        
        // When
        FirestoreItinerary result = itineraryJsonService.createItinerary(testItinerary);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("it_bali_luxury_3d_001");
        assertThat(result.getVersion()).isEqualTo(1);
        assertThat(result.getJson()).isEqualTo(expectedJson);
        
        // Verify all interactions
        verify(objectMapper).writeValueAsString(testItinerary);
        verify(databaseService).save(any(FirestoreItinerary.class));
        verifyNoUnexpectedInteractions();
        
        logger.info("Create itinerary test passed with complete field validation");
    }
    
    @Test
    @DisplayName("Should update itinerary with version increment")
    void shouldUpdateItineraryWithVersionIncrement() throws JsonProcessingException {
        // Given
        NormalizedItinerary testItinerary = testDataFactory.createBaliLuxuryItinerary();
        testItinerary.setVersion(2); // Updated version
        String expectedJson = "updated-serialized-json";
        FirestoreItinerary expectedEntity = new FirestoreItinerary("it_bali_luxury_3d_001", 2, expectedJson);
        
        when(objectMapper.writeValueAsString(testItinerary)).thenReturn(expectedJson);
        when(databaseService.save(any(FirestoreItinerary.class))).thenReturn(expectedEntity);
        
        // When
        FirestoreItinerary result = itineraryJsonService.updateItinerary(testItinerary);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getVersion()).isEqualTo(2);
        assertThat(result.getJson()).isEqualTo(expectedJson);
        
        verify(objectMapper).writeValueAsString(testItinerary);
        verify(databaseService).save(any(FirestoreItinerary.class));
        
        logger.info("Update itinerary test passed with version increment");
    }
    
    @Test
    @DisplayName("Should get itinerary by ID with deserialization")
    void shouldGetItineraryByIdWithDeserialization() throws JsonProcessingException {
        // Given
        String itineraryId = "it_bali_luxury_3d_001";
        String jsonData = "{\"itineraryId\":\"it_bali_luxury_3d_001\"}";
        FirestoreItinerary firestoreEntity = new FirestoreItinerary(itineraryId, 1, jsonData);
        NormalizedItinerary expectedItinerary = testDataFactory.createBaliLuxuryItinerary();
        
        when(databaseService.findById(itineraryId)).thenReturn(Optional.of(firestoreEntity));
        when(objectMapper.readValue(jsonData, NormalizedItinerary.class)).thenReturn(expectedItinerary);
        
        // When
        Optional<NormalizedItinerary> result = itineraryJsonService.getItinerary(itineraryId);
        
        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getItineraryId()).isEqualTo("it_bali_luxury_3d_001");
        assertThat(result.get().getDestination()).isEqualTo("Bali, Indonesia");
        
        verify(databaseService).findById(itineraryId);
        verify(objectMapper).readValue(jsonData, NormalizedItinerary.class);
        
        logger.info("Get itinerary by ID test passed with deserialization");
    }
    
    @Test
    @DisplayName("Should return empty optional when itinerary not found")
    void shouldReturnEmptyOptionalWhenItineraryNotFound() {
        // Given
        String itineraryId = "non-existent-id";
        when(databaseService.findById(itineraryId)).thenReturn(Optional.empty());
        
        // When
        Optional<NormalizedItinerary> result = itineraryJsonService.getItinerary(itineraryId);
        
        // Then
        assertThat(result).isEmpty();
        
        verify(databaseService).findById(itineraryId);
        verifyNoInteractions(objectMapper);
        
        logger.info("Get non-existent itinerary test passed");
    }
    
    @Test
    @DisplayName("Should delete itinerary by ID")
    void shouldDeleteItineraryById() {
        // Given
        String itineraryId = "it_bali_luxury_3d_001";
        
        // When
        itineraryJsonService.deleteItinerary(itineraryId);
        
        // Then
        verify(databaseService).deleteById(itineraryId);
        
        logger.info("Delete itinerary test passed");
    }
    
    @Test
    @DisplayName("Should handle flexible agent data operations")
    void shouldHandleFlexibleAgentDataOperations() {
        // Given
        String itineraryId = "test-itinerary-001";
        String agentName = "ENRICHMENT";
        Object agentData = java.util.Map.of("enriched", true, "timestamp", System.currentTimeMillis());
        
        NormalizedItinerary testItinerary = testDataFactory.createBaliLuxuryItinerary();
        String jsonData = "{\"itineraryId\":\"test-itinerary-001\"}";
        FirestoreItinerary firestoreEntity = new FirestoreItinerary(itineraryId, 1, jsonData);
        
        when(databaseService.findById(itineraryId)).thenReturn(Optional.of(firestoreEntity));
        try {
            when(objectMapper.readValue(anyString(), eq(NormalizedItinerary.class))).thenReturn(testItinerary);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        
        // When - Check agent data operations (these methods may not exist, so we'll test what's available)
        Optional<Object> retrievedData = itineraryJsonService.getAgentData(itineraryId, agentName);
        assertThat(retrievedData).isEmpty(); // Should be empty initially
        
        // Verify has agent data
        boolean hasData = itineraryJsonService.hasAgentData(itineraryId, agentName);
        assertThat(hasData).isFalse(); // Should be false initially
        
        logger.info("Flexible agent data operations test passed");
    }
    
    @Test
    @DisplayName("Should handle JSON serialization errors gracefully")
    void shouldHandleJsonSerializationErrorsGracefully() throws JsonProcessingException {
        // Given
        NormalizedItinerary testItinerary = testDataFactory.createBaliLuxuryItinerary();
        when(objectMapper.writeValueAsString(testItinerary)).thenThrow(new JsonProcessingException("Serialization error") {});
        
        // When/Then
        assertThatThrownBy(() -> itineraryJsonService.createItinerary(testItinerary))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to create itinerary");
        
        verify(objectMapper).writeValueAsString(testItinerary);
        verifyNoInteractions(databaseService);
        
        logger.info("JSON serialization error handling test passed");
    }
    
    @Test
    @DisplayName("Should handle JSON deserialization errors gracefully")
    void shouldHandleJsonDeserializationErrorsGracefully() throws JsonProcessingException {
        // Given
        String itineraryId = "it_bali_luxury_3d_001";
        String invalidJsonData = "invalid-json";
        FirestoreItinerary firestoreEntity = new FirestoreItinerary(itineraryId, 1, invalidJsonData);
        
        when(databaseService.findById(itineraryId)).thenReturn(Optional.of(firestoreEntity));
        when(objectMapper.readValue(invalidJsonData, NormalizedItinerary.class))
                .thenThrow(new JsonProcessingException("Deserialization error") {});
        
        // When
        Optional<NormalizedItinerary> result = itineraryJsonService.getItinerary(itineraryId);
        
        // Then - Should return empty optional on deserialization error
        assertThat(result).isEmpty();
        
        verify(databaseService).findById(itineraryId);
        verify(objectMapper).readValue(invalidJsonData, NormalizedItinerary.class);
        
        logger.info("JSON deserialization error handling test passed");
    }
    
    @Test
    @DisplayName("Should validate all CRUD operations work correctly")
    void shouldValidateAllCrudOperationsWorkCorrectly() throws JsonProcessingException {
        // Given
        NormalizedItinerary testItinerary = testDataFactory.createBaliLuxuryItinerary();
        String itineraryId = testItinerary.getItineraryId();
        String jsonData = "test-json-data";
        FirestoreItinerary firestoreEntity = new FirestoreItinerary(itineraryId, 1, jsonData);
        
        // Setup mocks for all operations
        try {
            when(objectMapper.writeValueAsString(any())).thenReturn(jsonData);
            when(objectMapper.readValue(anyString(), eq(NormalizedItinerary.class))).thenReturn(testItinerary);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        when(databaseService.save(any(FirestoreItinerary.class))).thenReturn(firestoreEntity);
        when(databaseService.findById(itineraryId)).thenReturn(Optional.of(firestoreEntity));
        
        // When/Then - Test Create
        FirestoreItinerary created = itineraryJsonService.createItinerary(testItinerary);
        assertThat(created).isNotNull();
        
        // When/Then - Test Read
        Optional<NormalizedItinerary> retrieved = itineraryJsonService.getItinerary(itineraryId);
        assertThat(retrieved).isPresent();
        
        // When/Then - Test Update
        testItinerary.setVersion(2);
        FirestoreItinerary updated = itineraryJsonService.updateItinerary(testItinerary);
        assertThat(updated).isNotNull();
        
        // When/Then - Test Delete
        itineraryJsonService.deleteItinerary(itineraryId);
        
        // Verify all operations were called
        verify(databaseService, times(2)).save(any(FirestoreItinerary.class)); // Create + Update
        verify(databaseService).findById(itineraryId); // Read
        verify(databaseService).deleteById(itineraryId); // Delete
        
        logger.info("All CRUD operations validation test passed");
    }
}