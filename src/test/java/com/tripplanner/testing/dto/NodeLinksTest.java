package com.tripplanner.testing.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripplanner.dto.NodeLinks;
import com.tripplanner.testing.DTOFieldValidator;
import com.tripplanner.testing.FieldValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Atomic tests for NodeLinks DTO with nested BookingInfo validation.
 */
class NodeLinksTest {
    
    private static final Logger logger = LoggerFactory.getLogger(NodeLinksTest.class);
    
    private DTOFieldValidator validator;
    private ObjectMapper objectMapper;
    
    @BeforeEach
    void setUp() {
        validator = new DTOFieldValidator();
        objectMapper = new ObjectMapper();
        logger.debug("Setting up NodeLinks DTO tests");
    }
    
    @Test
    @DisplayName("Should validate main booking field and nested BookingInfo fields")
    void shouldValidateMainBookingFieldAndNestedBookingInfoFields() {
        // Given - Complete NodeLinks with nested BookingInfo
        NodeLinks links = createCompleteNodeLinks();
        
        // When
        FieldValidationResult result = validator.validateNodeLinks(links);
        
        // Then
        assertThat(result.isValid()).isTrue();
        assertThat(result.getValidatedFieldCount()).isEqualTo(4); // booking + 3 nested fields
        assertThat(result.getSuccessCount()).isEqualTo(4);
        assertThat(result.getErrorCount()).isEqualTo(0);
        
        logger.info("Complete field validation test passed: {} fields validated successfully", result.getSuccessCount());
    }
    
    @Test
    @DisplayName("Should handle null links object")
    void shouldHandleNullLinksObject() {
        // Given - null links
        NodeLinks links = null;
        
        // When
        FieldValidationResult result = validator.validateNodeLinks(links);
        
        // Then
        assertThat(result.isValid()).isTrue(); // NodeLinks can be null (no @NotNull annotation)
        assertThat(result.getValidatedFieldCount()).isEqualTo(1);
        
        logger.info("Null object validation test passed");
    }
    
    @Test
    @DisplayName("Should handle null booking field")
    void shouldHandleNullBookingField() {
        // Given - NodeLinks with null booking
        NodeLinks links = new NodeLinks();
        links.setBooking(null);
        
        // When
        FieldValidationResult result = validator.validateNodeLinks(links);
        
        // Then
        assertThat(result.isValid()).isTrue(); // booking can be null
        assertThat(result.getValidatedFieldCount()).isEqualTo(1); // Only booking field validated
        
        logger.info("Null booking field validation test passed");
    }
    
    @Test
    @DisplayName("Should serialize and deserialize correctly with Jackson")
    void shouldSerializeAndDeserializeCorrectlyWithJackson() throws Exception {
        // Given - Complete NodeLinks
        NodeLinks original = createCompleteNodeLinks();
        
        // When - Serialize to JSON and back
        String json = objectMapper.writeValueAsString(original);
        NodeLinks deserialized = objectMapper.readValue(json, NodeLinks.class);
        
        // Then - Validate deserialized object has all fields
        FieldValidationResult result = validator.validateNodeLinks(deserialized);
        
        assertThat(result.isValid()).isTrue();
        assertThat(deserialized.getBooking()).isNotNull();
        assertThat(deserialized.getBooking().getRefNumber()).isEqualTo(original.getBooking().getRefNumber());
        assertThat(deserialized.getBooking().getStatus()).isEqualTo(original.getBooking().getStatus());
        assertThat(deserialized.getBooking().getDetails()).isEqualTo(original.getBooking().getDetails());
        
        logger.info("JSON serialization/deserialization test passed");
    }
    
    @Test
    @DisplayName("Should validate constructor with BookingInfo parameter")
    void shouldValidateConstructorWithBookingInfoParameter() {
        // Given - NodeLinks created with BookingInfo constructor
        NodeLinks.BookingInfo bookingInfo = new NodeLinks.BookingInfo("ref-123", "BOOKED", "Booking details");
        NodeLinks links = new NodeLinks(bookingInfo);
        
        // When
        FieldValidationResult result = validator.validateNodeLinks(links);
        
        // Then - Should have initialized booking field correctly
        assertThat(links.getBooking()).isEqualTo(bookingInfo);
        assertThat(result.isValid()).isTrue();
        
        logger.info("Constructor with BookingInfo validation test passed");
    }
    
    @Test
    @DisplayName("Should validate nested BookingInfo class fields")
    void shouldValidateNestedBookingInfoClassFields() {
        // Given - BookingInfo with all fields
        NodeLinks.BookingInfo bookingInfo = new NodeLinks.BookingInfo();
        bookingInfo.setRefNumber("FS-SAYAN-001");
        bookingInfo.setStatus("BOOKED");
        bookingInfo.setDetails("https://www.fourseasons.com/sayan/");
        
        NodeLinks links = new NodeLinks(bookingInfo);
        
        // When
        FieldValidationResult result = validator.validateNodeLinks(links);
        
        // Then - All nested fields should pass validation
        assertThat(result.isValid()).isTrue();
        assertThat(result.getErrors().stream()
                .anyMatch(error -> error.getFieldName().equals("booking.refNumber"))).isFalse();
        assertThat(result.getErrors().stream()
                .anyMatch(error -> error.getFieldName().equals("booking.status"))).isFalse();
        assertThat(result.getErrors().stream()
                .anyMatch(error -> error.getFieldName().equals("booking.details"))).isFalse();
        
        logger.info("Nested BookingInfo fields validation test passed");
    }
    
    @Test
    @DisplayName("Should validate BookingInfo constructor with all parameters")
    void shouldValidateBookingInfoConstructorWithAllParameters() {
        // Given - BookingInfo created with parameterized constructor
        NodeLinks.BookingInfo bookingInfo = new NodeLinks.BookingInfo("ref-456", "REQUIRED", "Booking required");
        
        // When
        NodeLinks links = new NodeLinks(bookingInfo);
        FieldValidationResult result = validator.validateNodeLinks(links);
        
        // Then - Should have initialized all fields correctly
        assertThat(bookingInfo.getRefNumber()).isEqualTo("ref-456");
        assertThat(bookingInfo.getStatus()).isEqualTo("REQUIRED");
        assertThat(bookingInfo.getDetails()).isEqualTo("Booking required");
        assertThat(result.isValid()).isTrue();
        
        logger.info("BookingInfo parameterized constructor validation test passed");
    }
    
    @Test
    @DisplayName("Should validate BookingInfo toString method")
    void shouldValidateBookingInfoToStringMethod() {
        // Given - BookingInfo with values
        NodeLinks.BookingInfo bookingInfo = new NodeLinks.BookingInfo("ref-789", "NOT_REQUIRED", null);
        
        // When
        String toString = bookingInfo.toString();
        
        // Then - Should contain field information
        assertThat(toString).contains("BookingInfo");
        assertThat(toString).contains("refNumber='ref-789'");
        assertThat(toString).contains("status='NOT_REQUIRED'");
        assertThat(toString).contains("details='null'");
        
        logger.info("BookingInfo toString method validation test passed");
    }
    
    @Test
    @DisplayName("Should validate NodeLinks toString method")
    void shouldValidateNodeLinksToStringMethod() {
        // Given - NodeLinks with BookingInfo
        NodeLinks links = createCompleteNodeLinks();
        
        // When
        String toString = links.toString();
        
        // Then - Should contain field information
        assertThat(toString).contains("NodeLinks");
        assertThat(toString).contains("booking=");
        
        logger.info("NodeLinks toString method validation test passed");
    }
    
    @Test
    @DisplayName("Should validate different booking status values")
    void shouldValidateDifferentBookingStatusValues() {
        // Given - BookingInfo with different status values
        String[] statusValues = {"NOT_REQUIRED", "REQUIRED", "BOOKED"};
        
        for (String status : statusValues) {
            NodeLinks.BookingInfo bookingInfo = new NodeLinks.BookingInfo("ref-" + status, status, "Details for " + status);
            NodeLinks links = new NodeLinks(bookingInfo);
            
            // When
            FieldValidationResult result = validator.validateNodeLinks(links);
            
            // Then - Should pass validation for all status values
            assertThat(result.isValid()).isTrue();
            assertThat(bookingInfo.getStatus()).isEqualTo(status);
        }
        
        logger.info("Different booking status values validation test passed");
    }
    
    @Test
    @DisplayName("Should handle null values in BookingInfo fields")
    void shouldHandleNullValuesInBookingInfoFields() {
        // Given - BookingInfo with null values
        NodeLinks.BookingInfo bookingInfo = new NodeLinks.BookingInfo();
        bookingInfo.setRefNumber(null);
        bookingInfo.setStatus("REQUIRED");
        bookingInfo.setDetails(null);
        
        NodeLinks links = new NodeLinks(bookingInfo);
        
        // When
        FieldValidationResult result = validator.validateNodeLinks(links);
        
        // Then - Should pass validation (no @NotNull annotations on BookingInfo fields)
        assertThat(result.isValid()).isTrue();
        assertThat(bookingInfo.getRefNumber()).isNull();
        assertThat(bookingInfo.getDetails()).isNull();
        
        logger.info("Null values in BookingInfo fields validation test passed");
    }
    
    /**
     * Helper method to create a complete NodeLinks with nested BookingInfo.
     */
    private NodeLinks createCompleteNodeLinks() {
        NodeLinks.BookingInfo bookingInfo = new NodeLinks.BookingInfo(
            "FS-SAYAN-001",
            "BOOKED", 
            "https://www.fourseasons.com/sayan/"
        );
        
        return new NodeLinks(bookingInfo);
    }
}