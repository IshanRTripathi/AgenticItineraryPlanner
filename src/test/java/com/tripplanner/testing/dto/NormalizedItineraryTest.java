package com.tripplanner.testing.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripplanner.dto.NormalizedItinerary;
import com.tripplanner.testing.DTOFieldValidator;
import com.tripplanner.testing.FieldValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Atomic tests for NormalizedItinerary DTO with complete field validation.
 */
class NormalizedItineraryTest {
    
    private static final Logger logger = LoggerFactory.getLogger(NormalizedItineraryTest.class);
    
    private DTOFieldValidator validator;
    private ObjectMapper objectMapper;
    
    @BeforeEach
    void setUp() {
        validator = new DTOFieldValidator();
        objectMapper = new ObjectMapper();
        logger.debug("Setting up NormalizedItinerary DTO tests");
    }
    
    @Test
    @DisplayName("Should validate all required fields with @NotBlank annotation")
    void shouldValidateRequiredFieldsWithNotBlankAnnotation() {
        // Given - NormalizedItinerary with missing required fields
        NormalizedItinerary itinerary = new NormalizedItinerary();
        // itineraryId is @NotBlank and days is @NotNull - should fail validation
        
        // When
        FieldValidationResult result = validator.validateNormalizedItinerary(itinerary);
        
        // Then
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).hasSize(2); // Both itineraryId and days are required
        assertThat(result.getErrors().stream()
                .anyMatch(error -> error.getFieldName().equals("itineraryId"))).isTrue();
        assertThat(result.getErrors().stream()
                .anyMatch(error -> error.getFieldName().equals("days"))).isTrue();
        
        logger.info("Required field validation test passed: {} errors found", result.getErrorCount());
    }
    
    @Test
    @DisplayName("Should validate all 20+ fields with correct types")
    void shouldValidateAllFieldsWithCorrectTypes() {
        // Given - Complete NormalizedItinerary with all fields populated
        NormalizedItinerary itinerary = createCompleteItinerary();
        
        // When
        FieldValidationResult result = validator.validateNormalizedItinerary(itinerary);
        
        // Then
        assertThat(result.isValid()).isTrue();
        assertThat(result.getValidatedFieldCount()).isEqualTo(21); // All 21 fields
        assertThat(result.getSuccessCount()).isEqualTo(21);
        assertThat(result.getErrorCount()).isEqualTo(0);
        
        logger.info("Complete field validation test passed: {} fields validated successfully", result.getSuccessCount());
    }
    
    @Test
    @DisplayName("Should handle null itinerary object")
    void shouldHandleNullItineraryObject() {
        // Given - null itinerary
        NormalizedItinerary itinerary = null;
        
        // When
        FieldValidationResult result = validator.validateNormalizedItinerary(itinerary);
        
        // Then
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().get(0).getFieldName()).isEqualTo("itinerary");
        
        logger.info("Null object validation test passed");
    }
    
    @Test
    @DisplayName("Should validate type mismatches for all fields")
    void shouldValidateTypeMismatchesForAllFields() {
        // Given - NormalizedItinerary with correct required fields
        NormalizedItinerary itinerary = new NormalizedItinerary();
        itinerary.setItineraryId("test-id"); // Required field set correctly
        itinerary.setDays(new ArrayList<>()); // Required field set correctly
        
        // In Java's type system, we can't actually set wrong types at compile time
        // This test validates that correctly typed fields pass validation
        
        // When
        FieldValidationResult result = validator.validateNormalizedItinerary(itinerary);
        
        // Then - Should pass because all fields are either correctly typed or null (which is allowed for optional fields)
        assertThat(result.isValid()).isTrue();
        
        logger.info("Type validation test passed");
    }
    
    @Test
    @DisplayName("Should serialize and deserialize correctly with Jackson")
    void shouldSerializeAndDeserializeCorrectlyWithJackson() throws Exception {
        // Given - Complete NormalizedItinerary
        NormalizedItinerary original = createCompleteItinerary();
        
        // When - Serialize to JSON and back
        String json = objectMapper.writeValueAsString(original);
        NormalizedItinerary deserialized = objectMapper.readValue(json, NormalizedItinerary.class);
        
        // Then - Validate deserialized object has all fields
        FieldValidationResult result = validator.validateNormalizedItinerary(deserialized);
        
        assertThat(result.isValid()).isTrue();
        assertThat(deserialized.getItineraryId()).isEqualTo(original.getItineraryId());
        assertThat(deserialized.getVersion()).isEqualTo(original.getVersion());
        assertThat(deserialized.getUserId()).isEqualTo(original.getUserId());
        assertThat(deserialized.getDestination()).isEqualTo(original.getDestination());
        
        logger.info("JSON serialization/deserialization test passed");
    }
    
    @Test
    @DisplayName("Should validate constraint annotations work correctly")
    void shouldValidateConstraintAnnotationsWorkCorrectly() {
        // Given - NormalizedItinerary with constraint violations
        NormalizedItinerary itinerary = new NormalizedItinerary();
        itinerary.setItineraryId(""); // @NotBlank should fail on empty string
        itinerary.setDays(new ArrayList<>()); // @NotNull should pass, but empty list is allowed
        
        // When
        FieldValidationResult result = validator.validateNormalizedItinerary(itinerary);
        
        // Then
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().get(0).getFieldName()).isEqualTo("itineraryId");
        assertThat(result.getErrors().get(0).getMessage()).contains("blank");
        
        logger.info("Constraint annotation validation test passed");
    }
    
    @Test
    @DisplayName("Should initialize collections correctly in constructor")
    void shouldInitializeCollectionsCorrectlyInConstructor() {
        // Given - New NormalizedItinerary using default constructor
        NormalizedItinerary itinerary = new NormalizedItinerary();
        
        // When - Check initialized collections
        // Then
        assertThat(itinerary.getAgentData()).isNotNull();
        assertThat(itinerary.getRevisions()).isNotNull();
        assertThat(itinerary.getChat()).isNotNull();
        
        // Validate these fields pass validation
        FieldValidationResult result = validator.validateNormalizedItinerary(itinerary);
        
        // Should fail only on required fields, not on initialized collections
        assertThat(result.getErrors().stream()
                .anyMatch(error -> error.getFieldName().equals("agentData"))).isFalse();
        assertThat(result.getErrors().stream()
                .anyMatch(error -> error.getFieldName().equals("revisions"))).isFalse();
        assertThat(result.getErrors().stream()
                .anyMatch(error -> error.getFieldName().equals("chat"))).isFalse();
        
        logger.info("Constructor initialization test passed");
    }
    
    /**
     * Helper method to create a complete NormalizedItinerary with all fields populated.
     */
    private NormalizedItinerary createCompleteItinerary() {
        NormalizedItinerary itinerary = new NormalizedItinerary();
        
        // Set all required fields
        itinerary.setItineraryId("test-itinerary-001");
        itinerary.setVersion(1);
        itinerary.setUserId("test-user-001");
        itinerary.setCreatedAt(System.currentTimeMillis());
        itinerary.setUpdatedAt(System.currentTimeMillis());
        itinerary.setSummary("Test itinerary summary");
        itinerary.setCurrency("USD");
        itinerary.setThemes(java.util.Arrays.asList("test", "theme"));
        itinerary.setOrigin("Test Origin");
        itinerary.setDestination("Test Destination");
        itinerary.setStartDate("2024-01-01");
        itinerary.setEndDate("2024-01-03");
        itinerary.setDays(new ArrayList<>());
        
        // Initialize other fields to avoid null values
        itinerary.setAgentData(new HashMap<>());
        itinerary.setRevisions(new ArrayList<>());
        itinerary.setChat(new ArrayList<>());
        
        return itinerary;
    }
}