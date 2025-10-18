package com.tripplanner.testing.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripplanner.dto.AgentDataSection;
import com.tripplanner.testing.DTOFieldValidator;
import com.tripplanner.testing.FieldValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Atomic tests for AgentDataSection DTO with flexible Map<String, Object> operations.
 */
class AgentDataSectionTest {
    
    private static final Logger logger = LoggerFactory.getLogger(AgentDataSectionTest.class);
    
    private DTOFieldValidator validator;
    private ObjectMapper objectMapper;
    
    @BeforeEach
    void setUp() {
        validator = new DTOFieldValidator();
        objectMapper = new ObjectMapper();
        logger.debug("Setting up AgentDataSection DTO tests");
    }
    
    @Test
    @DisplayName("Should validate flexible Map<String, Object> data field")
    void shouldValidateFlexibleMapDataField() {
        // Given - AgentDataSection with Map data
        AgentDataSection agentData = new AgentDataSection();
        Map<String, Object> data = new HashMap<>();
        data.put("enrichment", "enriched-data");
        data.put("booking", 123);
        data.put("nested", new HashMap<String, String>());
        agentData.setData(data);
        
        // When
        FieldValidationResult result = validator.validateAgentDataSection(agentData);
        
        // Then
        assertThat(result.isValid()).isTrue();
        assertThat(result.getValidatedFieldCount()).isEqualTo(1); // data field
        assertThat(result.getSuccessCount()).isEqualTo(1);
        assertThat(result.getErrorCount()).isEqualTo(0);
        
        logger.info("Flexible Map data field validation test passed");
    }
    
    @Test
    @DisplayName("Should handle null agentData object")
    void shouldHandleNullAgentDataObject() {
        // Given - null agentData
        AgentDataSection agentData = null;
        
        // When
        FieldValidationResult result = validator.validateAgentDataSection(agentData);
        
        // Then
        assertThat(result.isValid()).isTrue(); // AgentDataSection can be null
        assertThat(result.getValidatedFieldCount()).isEqualTo(1);
        
        logger.info("Null object validation test passed");
    }
    
    @Test
    @DisplayName("Should serialize and deserialize correctly with Jackson")
    void shouldSerializeAndDeserializeCorrectlyWithJackson() throws Exception {
        // Given - Complete AgentDataSection
        AgentDataSection original = createCompleteAgentDataSection();
        
        // When - Serialize to JSON and back
        String json = objectMapper.writeValueAsString(original);
        AgentDataSection deserialized = objectMapper.readValue(json, AgentDataSection.class);
        
        // Then - Validate deserialized object has all fields
        FieldValidationResult result = validator.validateAgentDataSection(deserialized);
        
        assertThat(result.isValid()).isTrue();
        assertThat(deserialized.getData()).isNotNull();
        assertThat(deserialized.getData().size()).isEqualTo(original.getData().size());
        
        logger.info("JSON serialization/deserialization test passed");
    }
    
    @Test
    @DisplayName("Should validate getAgentData and setAgentData operations")
    void shouldValidateGetAgentDataAndSetAgentDataOperations() {
        // Given - AgentDataSection
        AgentDataSection agentData = new AgentDataSection();
        
        // When - Set agent data for different agents
        agentData.setAgentData("ENRICHMENT", "ENRICHMENT-data");
        agentData.setAgentData("booking", Map.of("ref", "booking-123"));
        agentData.setAgentData("PLANNER", 42);
        
        // Then - Should retrieve correct data
        assertThat(agentData.getAgentData("ENRICHMENT")).isEqualTo("ENRICHMENT-data");
        assertThat(agentData.getAgentData("booking")).isInstanceOf(Map.class);
        assertThat(agentData.getAgentData("PLANNER")).isEqualTo(42);
        assertThat(agentData.getAgentData("nonexistent")).isNull();
        
        logger.info("Agent data get/set operations validation test passed");
    }
    
    @Test
    @DisplayName("Should validate typed getAgentData with automatic conversion")
    void shouldValidateTypedGetAgentDataWithAutomaticConversion() {
        // Given - AgentDataSection with various data types
        AgentDataSection agentData = new AgentDataSection();
        agentData.setAgentData("string", "test-string");
        agentData.setAgentData("number", 123);
        agentData.setAgentData("map", Map.of("key", "value"));
        
        // When - Get typed data
        String stringData = agentData.getAgentData("string", String.class);
        Integer numberData = agentData.getAgentData("number", Integer.class);
        Map<?, ?> mapData = agentData.getAgentData("map", Map.class);
        String nonExistent = agentData.getAgentData("nonexistent", String.class);
        
        // Then - Should return correct types or null
        assertThat(stringData).isEqualTo("test-string");
        assertThat(numberData).isEqualTo(123);
        assertThat(mapData).isNotNull();
        assertThat(nonExistent).isNull();
        
        logger.info("Typed agent data retrieval validation test passed");
    }
    
    @Test
    @DisplayName("Should validate hasAgentData operation")
    void shouldValidateHasAgentDataOperation() {
        // Given - AgentDataSection with some data
        AgentDataSection agentData = new AgentDataSection();
        agentData.setAgentData("existing", "data");
        
        // When/Then - Check existence
        assertThat(agentData.hasAgentData("existing")).isTrue();
        assertThat(agentData.hasAgentData("nonexistent")).isFalse();
        
        logger.info("Has agent data operation validation test passed");
    }
    
    @Test
    @DisplayName("Should validate removeAgentData operation")
    void shouldValidateRemoveAgentDataOperation() {
        // Given - AgentDataSection with data
        AgentDataSection agentData = new AgentDataSection();
        agentData.setAgentData("toRemove", "data");
        agentData.setAgentData("toKeep", "data");
        
        // When - Remove one agent's data
        agentData.removeAgentData("toRemove");
        
        // Then - Should remove only specified agent data
        assertThat(agentData.hasAgentData("toRemove")).isFalse();
        assertThat(agentData.hasAgentData("toKeep")).isTrue();
        
        logger.info("Remove agent data operation validation test passed");
    }
    
    @Test
    @DisplayName("Should validate getAgentNames operation")
    void shouldValidateGetAgentNamesOperation() {
        // Given - AgentDataSection with multiple agents
        AgentDataSection agentData = new AgentDataSection();
        agentData.setAgentData("agent1", "data1");
        agentData.setAgentData("agent2", "data2");
        agentData.setAgentData("agent3", "data3");
        
        // When - Get agent names
        var agentNames = agentData.getAgentNames();
        
        // Then - Should return all agent names
        assertThat(agentNames).hasSize(3);
        assertThat(agentNames).contains("agent1", "agent2", "agent3");
        
        logger.info("Get agent names operation validation test passed");
    }
    
    @Test
    @DisplayName("Should validate default constructor initialization")
    void shouldValidateDefaultConstructorInitialization() {
        // Given - AgentDataSection created with default constructor
        AgentDataSection agentData = new AgentDataSection();
        
        // When/Then - Check initialization
        assertThat(agentData.getData()).isNotNull();
        assertThat(agentData.getData()).isEmpty();
        assertThat(agentData.getAgentNames()).isEmpty();
        
        logger.info("Default constructor initialization validation test passed");
    }
    
    @Test
    @DisplayName("Should handle null data map gracefully")
    void shouldHandleNullDataMapGracefully() {
        // Given - AgentDataSection with null data
        AgentDataSection agentData = new AgentDataSection();
        agentData.setData(null);
        
        // When/Then - Should handle null gracefully
        assertThat(agentData.getAgentData("any")).isNull();
        assertThat(agentData.hasAgentData("any")).isFalse();
        assertThat(agentData.getAgentNames()).isEmpty();
        
        // removeAgentData should not throw exception
        agentData.removeAgentData("any");
        
        logger.info("Null data map handling validation test passed");
    }
    
    @Test
    @DisplayName("Should validate toString method")
    void shouldValidateToStringMethod() {
        // Given - AgentDataSection with data
        AgentDataSection agentData = createCompleteAgentDataSection();
        
        // When
        String toString = agentData.toString();
        
        // Then - Should contain field information
        assertThat(toString).contains("AgentDataSection");
        assertThat(toString).contains("data=");
        
        logger.info("ToString method validation test passed");
    }
    
    @Test
    @DisplayName("Should handle type conversion failures gracefully")
    void shouldHandleTypeConversionFailuresGracefully() {
        // Given - AgentDataSection with incompatible data
        AgentDataSection agentData = new AgentDataSection();
        agentData.setAgentData("string", "not-a-number");
        
        // When - Try to get as wrong type
        Integer result = agentData.getAgentData("string", Integer.class);
        
        // Then - Should return null for failed conversion
        assertThat(result).isNull();
        
        logger.info("Type conversion failure handling validation test passed");
    }
    
    /**
     * Helper method to create a complete AgentDataSection with various data types.
     */
    private AgentDataSection createCompleteAgentDataSection() {
        AgentDataSection agentData = new AgentDataSection();
        
        // Add various types of agent data
        agentData.setAgentData("ENRICHMENT", "enriched-place-data");
        agentData.setAgentData("booking", Map.of("ref", "booking-123", "status", "confirmed"));
        agentData.setAgentData("PLANNER", 42);
        agentData.setAgentData("nested", Map.of("level1", Map.of("level2", "deep-data")));
        
        return agentData;
    }
}