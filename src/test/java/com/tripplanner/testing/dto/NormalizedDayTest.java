package com.tripplanner.testing.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripplanner.dto.NormalizedDay;
import com.tripplanner.testing.DTOFieldValidator;
import com.tripplanner.testing.FieldValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Atomic tests for NormalizedDay DTO with complete field validation.
 */
class NormalizedDayTest {
    
    private static final Logger logger = LoggerFactory.getLogger(NormalizedDayTest.class);
    
    private DTOFieldValidator validator;
    private ObjectMapper objectMapper;
    
    @BeforeEach
    void setUp() {
        validator = new DTOFieldValidator();
        objectMapper = new ObjectMapper();
        logger.debug("Setting up NormalizedDay DTO tests");
    }
    
    @Test
    @DisplayName("Should validate all 14 fields with correct types")
    void shouldValidateAllFieldsWithCorrectTypes() {
        // Given - Complete NormalizedDay with all fields populated
        NormalizedDay day = createCompleteDay();
        
        // When
        FieldValidationResult result = validator.validateNormalizedDay(day);
        
        // Then
        assertThat(result.isValid()).isTrue();
        assertThat(result.getValidatedFieldCount()).isEqualTo(14); // All 14 fields
        assertThat(result.getSuccessCount()).isEqualTo(14);
        assertThat(result.getErrorCount()).isEqualTo(0);
        
        logger.info("Complete field validation test passed: {} fields validated successfully", result.getSuccessCount());
    }
    
    @Test
    @DisplayName("Should validate required @NotNull nodes field")
    void shouldValidateRequiredNotNullNodesField() {
        // Given - NormalizedDay with null nodes (violates @NotNull)
        NormalizedDay day = new NormalizedDay();
        day.setNodes(null); // This should fail validation
        
        // When
        FieldValidationResult result = validator.validateNormalizedDay(day);
        
        // Then
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().get(0).getFieldName()).isEqualTo("nodes");
        assertThat(result.getErrors().get(0).getMessage()).contains("@NotNull");
        
        logger.info("Required @NotNull field validation test passed");
    }
    
    @Test
    @DisplayName("Should handle null day object")
    void shouldHandleNullDayObject() {
        // Given - null day
        NormalizedDay day = null;
        
        // When
        FieldValidationResult result = validator.validateNormalizedDay(day);
        
        // Then
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().get(0).getFieldName()).isEqualTo("day");
        
        logger.info("Null object validation test passed");
    }
    
    @Test
    @DisplayName("Should validate nested collections (nodes, edges)")
    void shouldValidateNestedCollections() {
        // Given - NormalizedDay with empty collections
        NormalizedDay day = new NormalizedDay();
        day.setNodes(new ArrayList<>()); // Empty list should be valid
        day.setEdges(new ArrayList<>()); // Empty list should be valid
        
        // When
        FieldValidationResult result = validator.validateNormalizedDay(day);
        
        // Then - Should pass validation for collection types
        assertThat(result.getErrors().stream()
                .anyMatch(error -> error.getFieldName().equals("nodes"))).isFalse();
        assertThat(result.getErrors().stream()
                .anyMatch(error -> error.getFieldName().equals("edges"))).isFalse();
        
        logger.info("Nested collections validation test passed");
    }
    
    @Test
    @DisplayName("Should serialize and deserialize correctly with Jackson")
    void shouldSerializeAndDeserializeCorrectlyWithJackson() throws Exception {
        // Given - Complete NormalizedDay
        NormalizedDay original = createCompleteDay();
        
        // When - Serialize to JSON and back
        String json = objectMapper.writeValueAsString(original);
        NormalizedDay deserialized = objectMapper.readValue(json, NormalizedDay.class);
        
        // Then - Validate deserialized object has all fields
        FieldValidationResult result = validator.validateNormalizedDay(deserialized);
        
        assertThat(result.isValid()).isTrue();
        assertThat(deserialized.getDayNumber()).isEqualTo(original.getDayNumber());
        assertThat(deserialized.getDate()).isEqualTo(original.getDate());
        assertThat(deserialized.getLocation()).isEqualTo(original.getLocation());
        assertThat(deserialized.getPace()).isEqualTo(original.getPace());
        assertThat(deserialized.getTotalDistance()).isEqualTo(original.getTotalDistance());
        assertThat(deserialized.getTotalCost()).isEqualTo(original.getTotalCost());
        assertThat(deserialized.getTotalDuration()).isEqualTo(original.getTotalDuration());
        assertThat(deserialized.getTimeZone()).isEqualTo(original.getTimeZone());
        
        logger.info("JSON serialization/deserialization test passed");
    }
    
    @Test
    @DisplayName("Should validate constructor with parameters")
    void shouldValidateConstructorWithParameters() {
        // Given - NormalizedDay created with parameterized constructor
        NormalizedDay day = new NormalizedDay(1, "2024-01-01", "Test Location");
        
        // When
        FieldValidationResult result = validator.validateNormalizedDay(day);
        
        // Then - Should have initialized fields correctly
        assertThat(day.getDayNumber()).isEqualTo(1);
        assertThat(day.getDate()).isEqualTo("2024-01-01");
        assertThat(day.getLocation()).isEqualTo("Test Location");
        assertThat(day.getTotalDistance()).isEqualTo(0.0);
        assertThat(day.getTotalCost()).isEqualTo(0.0);
        assertThat(day.getTotalDuration()).isEqualTo(0.0);
        
        // Validation should fail only on required nodes field
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().get(0).getFieldName()).isEqualTo("nodes");
        
        logger.info("Parameterized constructor validation test passed");
    }
    
    @Test
    @DisplayName("Should validate all numeric fields accept correct types")
    void shouldValidateAllNumericFieldsAcceptCorrectTypes() {
        // Given - NormalizedDay with all numeric fields set
        NormalizedDay day = new NormalizedDay();
        day.setDayNumber(1); // Integer
        day.setTotalDistance(25.5); // Double
        day.setTotalCost(150.75); // Double
        day.setTotalDuration(8.5); // Double
        day.setNodes(new ArrayList<>()); // Required field
        
        // When
        FieldValidationResult result = validator.validateNormalizedDay(day);
        
        // Then - Numeric fields should pass validation
        assertThat(result.getErrors().stream()
                .anyMatch(error -> error.getFieldName().equals("dayNumber"))).isFalse();
        assertThat(result.getErrors().stream()
                .anyMatch(error -> error.getFieldName().equals("totalDistance"))).isFalse();
        assertThat(result.getErrors().stream()
                .anyMatch(error -> error.getFieldName().equals("totalCost"))).isFalse();
        assertThat(result.getErrors().stream()
                .anyMatch(error -> error.getFieldName().equals("totalDuration"))).isFalse();
        
        logger.info("Numeric fields validation test passed");
    }
    
    @Test
    @DisplayName("Should validate string fields accept correct values")
    void shouldValidateStringFieldsAcceptCorrectValues() {
        // Given - NormalizedDay with all string fields set
        NormalizedDay day = new NormalizedDay();
        day.setDate("2024-01-01");
        day.setLocation("Bali, Indonesia");
        day.setNotes("Test notes");
        day.setPace("relaxed");
        day.setTimeWindowStart("09:00");
        day.setTimeWindowEnd("18:00");
        day.setTimeZone("Asia/Makassar");
        day.setNodes(new ArrayList<>()); // Required field
        
        // When
        FieldValidationResult result = validator.validateNormalizedDay(day);
        
        // Then - String fields should pass validation
        assertThat(result.getErrors().stream()
                .anyMatch(error -> error.getFieldName().equals("date"))).isFalse();
        assertThat(result.getErrors().stream()
                .anyMatch(error -> error.getFieldName().equals("location"))).isFalse();
        assertThat(result.getErrors().stream()
                .anyMatch(error -> error.getFieldName().equals("notes"))).isFalse();
        assertThat(result.getErrors().stream()
                .anyMatch(error -> error.getFieldName().equals("pace"))).isFalse();
        assertThat(result.getErrors().stream()
                .anyMatch(error -> error.getFieldName().equals("timeWindowStart"))).isFalse();
        assertThat(result.getErrors().stream()
                .anyMatch(error -> error.getFieldName().equals("timeWindowEnd"))).isFalse();
        assertThat(result.getErrors().stream()
                .anyMatch(error -> error.getFieldName().equals("timeZone"))).isFalse();
        
        logger.info("String fields validation test passed");
    }
    
    /**
     * Helper method to create a complete NormalizedDay with all fields populated.
     */
    private NormalizedDay createCompleteDay() {
        NormalizedDay day = new NormalizedDay();
        
        // Set all fields
        day.setDayNumber(1);
        day.setDate("2024-01-01");
        day.setLocation("Bali, Indonesia");
        day.setWarnings(Arrays.asList("Test warning"));
        day.setNotes("Test notes for the day");
        day.setPace("relaxed");
        day.setTotalDistance(25.0);
        day.setTotalCost(450.0);
        day.setTotalDuration(8.0);
        day.setTimeWindowStart("09:00");
        day.setTimeWindowEnd("21:00");
        day.setTimeZone("Asia/Makassar");
        day.setNodes(new ArrayList<>()); // Required field
        day.setEdges(new ArrayList<>());
        
        return day;
    }
}