package com.tripplanner.testing.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripplanner.dto.NodeCost;
import com.tripplanner.testing.DTOFieldValidator;
import com.tripplanner.testing.FieldValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Atomic tests for NodeCost DTO with complete field validation.
 */
class NodeCostTest {
    
    private static final Logger logger = LoggerFactory.getLogger(NodeCostTest.class);
    
    private DTOFieldValidator validator;
    private ObjectMapper objectMapper;
    
    @BeforeEach
    void setUp() {
        validator = new DTOFieldValidator();
        objectMapper = new ObjectMapper();
        logger.debug("Setting up NodeCost DTO tests");
    }
    
    @Test
    @DisplayName("Should validate both fields with correct types")
    void shouldValidateBothFieldsWithCorrectTypes() {
        // Given - Complete NodeCost with both fields populated
        NodeCost cost = new NodeCost(45.00, "USD");
        
        // When
        FieldValidationResult result = validator.validateNodeCost(cost);
        
        // Then
        assertThat(result.isValid()).isTrue();
        assertThat(result.getValidatedFieldCount()).isEqualTo(2); // Both fields
        assertThat(result.getSuccessCount()).isEqualTo(2);
        assertThat(result.getErrorCount()).isEqualTo(0);
        
        logger.info("Complete field validation test passed: {} fields validated successfully", result.getSuccessCount());
    }
    
    @Test
    @DisplayName("Should handle null cost object")
    void shouldHandleNullCostObject() {
        // Given - null cost
        NodeCost cost = null;
        
        // When
        FieldValidationResult result = validator.validateNodeCost(cost);
        
        // Then
        assertThat(result.isValid()).isTrue(); // NodeCost can be null (no @NotNull annotation)
        assertThat(result.getValidatedFieldCount()).isEqualTo(1);
        
        logger.info("Null object validation test passed");
    }
    
    @Test
    @DisplayName("Should serialize and deserialize correctly with Jackson")
    void shouldSerializeAndDeserializeCorrectlyWithJackson() throws Exception {
        // Given - Complete NodeCost
        NodeCost original = new NodeCost(850.00, "USD");
        
        // When - Serialize to JSON and back
        String json = objectMapper.writeValueAsString(original);
        NodeCost deserialized = objectMapper.readValue(json, original.getClass());
        
        // Then - Validate deserialized object has all fields
        FieldValidationResult result = validator.validateNodeCost(deserialized);
        
        assertThat(result.isValid()).isTrue();
        assertThat(deserialized.getAmountPerPerson()).isEqualTo(original.getAmountPerPerson());
        assertThat(deserialized.getCurrency()).isEqualTo(original.getCurrency());
        
        logger.info("JSON serialization/deserialization test passed");
    }
    
    @Test
    @DisplayName("Should validate constructor with parameters")
    void shouldValidateConstructorWithParameters() {
        // Given - NodeCost created with parameterized constructor
        NodeCost cost = new NodeCost(320.50, "EUR");
        
        // When
        FieldValidationResult result = validator.validateNodeCost(cost);
        
        // Then - Should have initialized fields correctly
        assertThat(cost.getAmountPerPerson()).isEqualTo(320.50);
        assertThat(cost.getCurrency()).isEqualTo("EUR");
        assertThat(result.isValid()).isTrue();
        
        logger.info("Parameterized constructor validation test passed");
    }
    
    @Test
    @DisplayName("Should validate default constructor")
    void shouldValidateDefaultConstructor() {
        // Given - NodeCost created with default constructor
        NodeCost cost = new NodeCost();
        
        // When
        FieldValidationResult result = validator.validateNodeCost(cost);
        
        // Then - Should pass validation with null fields (no required annotations)
        assertThat(result.isValid()).isTrue();
        assertThat(cost.getAmountPerPerson()).isNull();
        assertThat(cost.getCurrency()).isNull();
        
        logger.info("Default constructor validation test passed");
    }
    
    @Test
    @DisplayName("Should validate amountPerPerson field accepts Double type")
    void shouldValidateAmountPerPersonFieldAcceptsDoubleType() {
        // Given - NodeCost with various Double values
        NodeCost cost1 = new NodeCost();
        cost1.setAmountPerPerson(0.0);
        
        NodeCost cost2 = new NodeCost();
        cost2.setAmountPerPerson(999.99);
        
        NodeCost cost3 = new NodeCost();
        cost3.setAmountPerPerson(45.5);
        
        // When
        FieldValidationResult result1 = validator.validateNodeCost(cost1);
        FieldValidationResult result2 = validator.validateNodeCost(cost2);
        FieldValidationResult result3 = validator.validateNodeCost(cost3);
        
        // Then - All should pass validation
        assertThat(result1.isValid()).isTrue();
        assertThat(result2.isValid()).isTrue();
        assertThat(result3.isValid()).isTrue();
        
        assertThat(result1.getErrors().stream()
                .anyMatch(error -> error.getFieldName().equals("amountPerPerson"))).isFalse();
        assertThat(result2.getErrors().stream()
                .anyMatch(error -> error.getFieldName().equals("amountPerPerson"))).isFalse();
        assertThat(result3.getErrors().stream()
                .anyMatch(error -> error.getFieldName().equals("amountPerPerson"))).isFalse();
        
        logger.info("AmountPerPerson Double field validation test passed");
    }
    
    @Test
    @DisplayName("Should validate currency field accepts String type")
    void shouldValidateCurrencyFieldAcceptsStringType() {
        // Given - NodeCost with various currency values
        NodeCost cost1 = new NodeCost();
        cost1.setCurrency("USD");
        
        NodeCost cost2 = new NodeCost();
        cost2.setCurrency("EUR");
        
        NodeCost cost3 = new NodeCost();
        cost3.setCurrency("IDR");
        
        // When
        FieldValidationResult result1 = validator.validateNodeCost(cost1);
        FieldValidationResult result2 = validator.validateNodeCost(cost2);
        FieldValidationResult result3 = validator.validateNodeCost(cost3);
        
        // Then - All should pass validation
        assertThat(result1.isValid()).isTrue();
        assertThat(result2.isValid()).isTrue();
        assertThat(result3.isValid()).isTrue();
        
        assertThat(result1.getErrors().stream()
                .anyMatch(error -> error.getFieldName().equals("currency"))).isFalse();
        assertThat(result2.getErrors().stream()
                .anyMatch(error -> error.getFieldName().equals("currency"))).isFalse();
        assertThat(result3.getErrors().stream()
                .anyMatch(error -> error.getFieldName().equals("currency"))).isFalse();
        
        logger.info("Currency String field validation test passed");
    }
    
    @Test
    @DisplayName("Should validate toString method")
    void shouldValidateToStringMethod() {
        // Given - NodeCost with values
        NodeCost cost = new NodeCost(45.00, "USD");
        
        // When
        String toString = cost.toString();
        
        // Then - Should contain field information
        assertThat(toString).contains("NodeCost");
        assertThat(toString).contains("amountPerPerson=45.0");
        assertThat(toString).contains("currency='USD'");
        
        logger.info("ToString method validation test passed");
    }
    
    @Test
    @DisplayName("Should handle null field values correctly")
    void shouldHandleNullFieldValuesCorrectly() {
        // Given - NodeCost with null values
        NodeCost cost = new NodeCost();
        cost.setAmountPerPerson(null);
        cost.setCurrency(null);
        
        // When
        FieldValidationResult result = validator.validateNodeCost(cost);
        
        // Then - Should pass validation (no @NotNull annotations)
        assertThat(result.isValid()).isTrue();
        assertThat(cost.getAmountPerPerson()).isNull();
        assertThat(cost.getCurrency()).isNull();
        
        logger.info("Null field values validation test passed");
    }
}