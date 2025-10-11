package com.tripplanner.testing.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripplanner.dto.Coordinates;
import com.tripplanner.dto.NodeLocation;
import com.tripplanner.testing.DTOFieldValidator;
import com.tripplanner.testing.FieldValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Atomic tests for NodeLocation DTO with complete field validation.
 */
class NodeLocationTest {
    
    private static final Logger logger = LoggerFactory.getLogger(NodeLocationTest.class);
    
    private DTOFieldValidator validator;
    private ObjectMapper objectMapper;
    
    @BeforeEach
    void setUp() {
        validator = new DTOFieldValidator();
        objectMapper = new ObjectMapper();
        logger.debug("Setting up NodeLocation DTO tests");
    }
    
    @Test
    @DisplayName("Should validate all 8 fields with correct types")
    void shouldValidateAllFieldsWithCorrectTypes() {
        // Given - Complete NodeLocation with all fields populated
        NodeLocation location = createCompleteLocation();
        
        // When
        FieldValidationResult result = validator.validateNodeLocation(location);
        
        // Then
        assertThat(result.isValid()).isTrue();
        assertThat(result.getValidatedFieldCount()).isEqualTo(8); // All 8 fields
        assertThat(result.getSuccessCount()).isEqualTo(8);
        assertThat(result.getErrorCount()).isEqualTo(0);
        
        logger.info("Complete field validation test passed: {} fields validated successfully", result.getSuccessCount());
    }
    
    @Test
    @DisplayName("Should handle null location object")
    void shouldHandleNullLocationObject() {
        // Given - null location
        NodeLocation location = null;
        
        // When
        FieldValidationResult result = validator.validateNodeLocation(location);
        
        // Then
        assertThat(result.isValid()).isTrue(); // NodeLocation can be null (no @NotNull annotation)
        assertThat(result.getValidatedFieldCount()).isEqualTo(1);
        
        logger.info("Null object validation test passed");
    }
    
    @Test
    @DisplayName("Should serialize and deserialize correctly with Jackson")
    void shouldSerializeAndDeserializeCorrectlyWithJackson() throws Exception {
        // Given - Complete NodeLocation
        NodeLocation original = createCompleteLocation();
        
        // When - Serialize to JSON and back
        String json = objectMapper.writeValueAsString(original);
        NodeLocation deserialized = objectMapper.readValue(json, NodeLocation.class);
        
        // Then - Validate deserialized object has all fields
        FieldValidationResult result = validator.validateNodeLocation(deserialized);
        
        assertThat(result.isValid()).isTrue();
        assertThat(deserialized.getName()).isEqualTo(original.getName());
        assertThat(deserialized.getAddress()).isEqualTo(original.getAddress());
        assertThat(deserialized.getPlaceId()).isEqualTo(original.getPlaceId());
        assertThat(deserialized.getGoogleMapsUri()).isEqualTo(original.getGoogleMapsUri());
        assertThat(deserialized.getRating()).isEqualTo(original.getRating());
        assertThat(deserialized.getOpeningHours()).isEqualTo(original.getOpeningHours());
        assertThat(deserialized.getClosingHours()).isEqualTo(original.getClosingHours());
        
        logger.info("JSON serialization/deserialization test passed");
    }
    
    @Test
    @DisplayName("Should validate constructor with basic parameters")
    void shouldValidateConstructorWithBasicParameters() {
        // Given - NodeLocation created with basic constructor
        Coordinates coords = new Coordinates(-8.7467, 115.1671);
        NodeLocation location = new NodeLocation("Test Location", "Test Address", coords);
        
        // When
        FieldValidationResult result = validator.validateNodeLocation(location);
        
        // Then - Should have initialized basic fields correctly
        assertThat(location.getName()).isEqualTo("Test Location");
        assertThat(location.getAddress()).isEqualTo("Test Address");
        assertThat(location.getCoordinates()).isEqualTo(coords);
        assertThat(result.isValid()).isTrue();
        
        logger.info("Basic constructor validation test passed");
    }
    
    @Test
    @DisplayName("Should validate constructor with placeId parameter")
    void shouldValidateConstructorWithPlaceIdParameter() {
        // Given - NodeLocation created with placeId constructor
        Coordinates coords = new Coordinates(-8.7467, 115.1671);
        NodeLocation location = new NodeLocation("Test Location", "Test Address", coords, "place-id-123");
        
        // When
        FieldValidationResult result = validator.validateNodeLocation(location);
        
        // Then - Should have initialized fields correctly
        assertThat(location.getName()).isEqualTo("Test Location");
        assertThat(location.getAddress()).isEqualTo("Test Address");
        assertThat(location.getCoordinates()).isEqualTo(coords);
        assertThat(location.getPlaceId()).isEqualTo("place-id-123");
        assertThat(result.isValid()).isTrue();
        
        logger.info("PlaceId constructor validation test passed");
    }
    
    @Test
    @DisplayName("Should validate constructor with all parameters")
    void shouldValidateConstructorWithAllParameters() {
        // Given - NodeLocation created with full constructor
        Coordinates coords = new Coordinates(-8.7467, 115.1671);
        NodeLocation location = new NodeLocation(
            "Test Location", 
            "Test Address", 
            coords, 
            "place-id-123",
            "https://maps.google.com/?cid=123456789",
            4.5,
            "09:00",
            "18:00"
        );
        
        // When
        FieldValidationResult result = validator.validateNodeLocation(location);
        
        // Then - Should have initialized all fields correctly
        assertThat(location.getName()).isEqualTo("Test Location");
        assertThat(location.getAddress()).isEqualTo("Test Address");
        assertThat(location.getCoordinates()).isEqualTo(coords);
        assertThat(location.getPlaceId()).isEqualTo("place-id-123");
        assertThat(location.getGoogleMapsUri()).isEqualTo("https://maps.google.com/?cid=123456789");
        assertThat(location.getRating()).isEqualTo(4.5);
        assertThat(location.getOpeningHours()).isEqualTo("09:00");
        assertThat(location.getClosingHours()).isEqualTo("18:00");
        assertThat(result.isValid()).isTrue();
        
        logger.info("Full constructor validation test passed");
    }
    
    @Test
    @DisplayName("Should validate all string fields accept correct values")
    void shouldValidateAllStringFieldsAcceptCorrectValues() {
        // Given - NodeLocation with all string fields set
        NodeLocation location = new NodeLocation();
        location.setName("Ngurah Rai International Airport");
        location.setAddress("Jl. Raya Gusti Ngurah Rai, Tuban, Badung, Bali 80119");
        location.setPlaceId("ChIJoQ8Q6NNB0i0RkOYkS7EPkSQ");
        location.setGoogleMapsUri("https://maps.google.com/?cid=123456789");
        location.setOpeningHours("00:00");
        location.setClosingHours("23:59");
        
        // When
        FieldValidationResult result = validator.validateNodeLocation(location);
        
        // Then - String fields should pass validation
        assertThat(result.getErrors().stream()
                .anyMatch(error -> error.getFieldName().equals("name"))).isFalse();
        assertThat(result.getErrors().stream()
                .anyMatch(error -> error.getFieldName().equals("address"))).isFalse();
        assertThat(result.getErrors().stream()
                .anyMatch(error -> error.getFieldName().equals("placeId"))).isFalse();
        assertThat(result.getErrors().stream()
                .anyMatch(error -> error.getFieldName().equals("googleMapsUri"))).isFalse();
        assertThat(result.getErrors().stream()
                .anyMatch(error -> error.getFieldName().equals("openingHours"))).isFalse();
        assertThat(result.getErrors().stream()
                .anyMatch(error -> error.getFieldName().equals("closingHours"))).isFalse();
        
        logger.info("String fields validation test passed");
    }
    
    @Test
    @DisplayName("Should validate numeric fields accept correct types")
    void shouldValidateNumericFieldsAcceptCorrectTypes() {
        // Given - NodeLocation with numeric fields set
        NodeLocation location = new NodeLocation();
        location.setRating(4.8); // Double
        location.setCoordinates(new Coordinates(-8.7467, 115.1671)); // Coordinates object
        
        // When
        FieldValidationResult result = validator.validateNodeLocation(location);
        
        // Then - Numeric fields should pass validation
        assertThat(result.getErrors().stream()
                .anyMatch(error -> error.getFieldName().equals("rating"))).isFalse();
        assertThat(result.getErrors().stream()
                .anyMatch(error -> error.getFieldName().equals("coordinates"))).isFalse();
        
        logger.info("Numeric fields validation test passed");
    }
    
    /**
     * Helper method to create a complete NodeLocation with all fields populated.
     */
    private NodeLocation createCompleteLocation() {
        Coordinates coordinates = new Coordinates(-8.7467, 115.1671);
        
        return new NodeLocation(
            "Ngurah Rai International Airport",
            "Jl. Raya Gusti Ngurah Rai, Tuban, Badung, Bali 80119",
            coordinates,
            "ChIJoQ8Q6NNB0i0RkOYkS7EPkSQ",
            "https://maps.google.com/?cid=123456789",
            4.2,
            "00:00",
            "23:59"
        );
    }
}