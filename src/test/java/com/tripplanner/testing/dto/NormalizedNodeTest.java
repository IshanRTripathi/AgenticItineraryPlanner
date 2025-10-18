package com.tripplanner.testing.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripplanner.dto.NormalizedNode;
import com.tripplanner.testing.DTOFieldValidator;
import com.tripplanner.testing.FieldValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Atomic tests for NormalizedNode DTO with complete field validation.
 */
class NormalizedNodeTest {
    
    private static final Logger logger = LoggerFactory.getLogger(NormalizedNodeTest.class);
    
    private DTOFieldValidator validator;
    private ObjectMapper objectMapper;
    
    @BeforeEach
    void setUp() {
        validator = new DTOFieldValidator();
        objectMapper = new ObjectMapper();
        logger.debug("Setting up NormalizedNode DTO tests");
    }
    
    @Test
    @DisplayName("Should validate all 17 fields with correct types")
    void shouldValidateAllFieldsWithCorrectTypes() {
        // Given - Complete NormalizedNode with all fields populated
        NormalizedNode node = createCompleteNode();
        
        // When
        FieldValidationResult result = validator.validateNormalizedNode(node);
        
        // Then
        assertThat(result.isValid()).isTrue();
        assertThat(result.getValidatedFieldCount()).isEqualTo(17); // All 17 fields
        assertThat(result.getSuccessCount()).isEqualTo(17);
        assertThat(result.getErrorCount()).isEqualTo(0);
        
        logger.info("Complete field validation test passed: {} fields validated successfully", result.getSuccessCount());
    }
    
    @Test
    @DisplayName("Should validate required @NotBlank fields")
    void shouldValidateRequiredNotBlankFields() {
        // Given - NormalizedNode with missing required @NotBlank fields
        NormalizedNode node = new NormalizedNode();
        // id, type, title are @NotBlank - should fail validation
        
        // When
        FieldValidationResult result = validator.validateNormalizedNode(node);
        
        // Then
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).hasSize(3); // id, type, title
        
        // Check each required field error
        assertThat(result.getErrors().stream()
                .anyMatch(error -> error.getFieldName().equals("id") && error.getMessage().contains("@NotNull"))).isTrue();
        assertThat(result.getErrors().stream()
                .anyMatch(error -> error.getFieldName().equals("type") && error.getMessage().contains("@NotNull"))).isTrue();
        assertThat(result.getErrors().stream()
                .anyMatch(error -> error.getFieldName().equals("title") && error.getMessage().contains("@NotNull"))).isTrue();
        
        logger.info("Required @NotBlank fields validation test passed: {} errors found", result.getErrorCount());
    }
    
    @Test
    @DisplayName("Should handle null node object")
    void shouldHandleNullNodeObject() {
        // Given - null node
        NormalizedNode node = null;
        
        // When
        FieldValidationResult result = validator.validateNormalizedNode(node);
        
        // Then
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().get(0).getFieldName()).isEqualTo("node");
        
        logger.info("Null object validation test passed");
    }
    
    @Test
    @DisplayName("Should validate flexible agentData Map<String, Object> field")
    void shouldValidateFlexibleAgentDataMapField() {
        // Given - NormalizedNode with agentData Map
        NormalizedNode node = new NormalizedNode("test-id", "attraction", "Test Title");
        HashMap<String, Object> agentData = new HashMap<>();
        agentData.put("enrichment", "test-data");
        agentData.put("booking", 123);
        agentData.put("nested", new HashMap<String, String>());
        node.setAgentData(agentData);
        
        // When
        FieldValidationResult result = validator.validateNormalizedNode(node);
        
        // Then - agentData field should pass validation as Map instance
        assertThat(result.getErrors().stream()
                .anyMatch(error -> error.getFieldName().equals("agentData"))).isFalse();
        
        logger.info("Flexible agentData Map validation test passed");
    }
    
    @Test
    @DisplayName("Should serialize and deserialize correctly with Jackson")
    void shouldSerializeAndDeserializeCorrectlyWithJackson() throws Exception {
        // Given - Complete NormalizedNode
        NormalizedNode original = createCompleteNode();
        
        // When - Serialize to JSON and back
        String json = objectMapper.writeValueAsString(original);
        NormalizedNode deserialized = objectMapper.readValue(json, NormalizedNode.class);
        
        // Then - Validate deserialized object has all fields
        FieldValidationResult result = validator.validateNormalizedNode(deserialized);
        
        assertThat(result.isValid()).isTrue();
        assertThat(deserialized.getId()).isEqualTo(original.getId());
        assertThat(deserialized.getType()).isEqualTo(original.getType());
        assertThat(deserialized.getTitle()).isEqualTo(original.getTitle());
        assertThat(deserialized.getStatus()).isEqualTo(original.getStatus());
        assertThat(deserialized.getUpdatedBy()).isEqualTo(original.getUpdatedBy());
        assertThat(deserialized.getLocked()).isEqualTo(original.getLocked());
        
        logger.info("JSON serialization/deserialization test passed");
    }
    
    @Test
    @DisplayName("Should validate status transition methods and helper methods")
    void shouldValidateStatusTransitionMethodsAndHelperMethods() {
        // Given - NormalizedNode with different statuses
        NormalizedNode node = new NormalizedNode("test-id", "attraction", "Test Title");
        
        // Test initial status
        node.setStatus("planned");
        assertThat(node.isPlanned()).isTrue();
        assertThat(node.isInProgress()).isFalse();
        assertThat(node.isCompleted()).isFalse();
        
        // Test status transitions
        assertThat(node.canTransitionTo("in_progress")).isTrue();
        assertThat(node.canTransitionTo("completed")).isFalse(); // Can't go directly from planned to completed
        
        // Test booking status
        node.setBookingRef("booking-123");
        assertThat(node.isBooked()).isTrue();
        
        // Test update tracking
        String originalUpdatedBy = node.getUpdatedBy();
        Long originalUpdatedAt = node.getUpdatedAt();
        
        // Wait a bit to ensure timestamp difference
        try {
            Thread.sleep(1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        node.markAsUpdated("user");
        assertThat(node.getUpdatedBy()).isEqualTo("user");
        assertThat(node.getUpdatedAt()).isGreaterThanOrEqualTo(originalUpdatedAt);
        
        logger.info("Status transition and helper methods validation test passed");
    }
    
    @Test
    @DisplayName("Should validate constructor with parameters")
    void shouldValidateConstructorWithParameters() {
        // Given - NormalizedNode created with parameterized constructor
        NormalizedNode node = new NormalizedNode("test-id", "hotel", "Test Hotel");
        
        // When
        FieldValidationResult result = validator.validateNormalizedNode(node);
        
        // Then - Should have initialized required fields correctly
        assertThat(node.getId()).isEqualTo("test-id");
        assertThat(node.getType()).isEqualTo("hotel");
        assertThat(node.getTitle()).isEqualTo("Test Hotel");
        assertThat(node.getUpdatedAt()).isNotNull();
        assertThat(node.getAgentData()).isNotNull();
        
        // Should pass validation for required fields
        assertThat(result.isValid()).isTrue();
        
        logger.info("Parameterized constructor validation test passed");
    }
    
    @Test
    @DisplayName("Should validate equals and hashCode methods")
    void shouldValidateEqualsAndHashCodeMethods() {
        // Given - Two nodes with same ID
        NormalizedNode node1 = new NormalizedNode("same-id", "attraction", "Title 1");
        NormalizedNode node2 = new NormalizedNode("same-id", "hotel", "Title 2");
        NormalizedNode node3 = new NormalizedNode("different-id", "attraction", "Title 3");
        
        // When/Then - Test equals and hashCode
        assertThat(node1.equals(node2)).isTrue(); // Same ID
        assertThat(node1.equals(node3)).isFalse(); // Different ID
        assertThat(node1.hashCode()).isEqualTo(node2.hashCode()); // Same ID should have same hash
        
        logger.info("Equals and hashCode validation test passed");
    }
    
    @Test
    @DisplayName("Should validate default constructor initialization")
    void shouldValidateDefaultConstructorInitialization() {
        // Given - NormalizedNode created with default constructor
        NormalizedNode node = new NormalizedNode();
        
        // When/Then - Check default initialization
        assertThat(node.getUpdatedAt()).isNotNull();
        assertThat(node.getAgentData()).isNotNull();
        assertThat(node.getAgentData()).isEmpty();
        
        logger.info("Default constructor initialization validation test passed");
    }
    
    /**
     * Helper method to create a complete NormalizedNode with all fields populated.
     */
    private NormalizedNode createCompleteNode() {
        NormalizedNode node = new NormalizedNode("test-node-001", "attraction", "Test Attraction");
        
        // Set all optional fields
        node.setLabels(Arrays.asList("cultural", "popular"));
        node.setLocked(false);
        node.setBookingRef("booking-ref-123");
        node.setStatus("planned");
        node.setUpdatedBy("agent");
        node.setUpdatedAt(System.currentTimeMillis());
        
        // Set agentData Map
        HashMap<String, Object> agentData = new HashMap<>();
        agentData.put("enrichment", "enriched-data");
        agentData.put("booking", "booking-data");
        node.setAgentData(agentData);
        
        return node;
    }
}