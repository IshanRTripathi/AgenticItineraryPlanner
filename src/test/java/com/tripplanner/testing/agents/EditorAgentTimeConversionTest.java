package com.tripplanner.testing.agents;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tripplanner.agents.EditorAgent;
import com.tripplanner.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for time string to timestamp conversion logic in EditorAgent.
 */
class EditorAgentTimeConversionTest {
    
    @Mock
    private SummarizationService summarizationService;
    
    @Mock
    private ChangeEngine changeEngine;
    
    @Mock
    private GeminiClient geminiClient;
    
    @Mock
    private ItineraryJsonService itineraryJsonService;
    
    @Mock
    private LLMResponseHandler llmResponseHandler;
    
    @Mock
    private AgentEventBus eventBus;
    
    private ObjectMapper objectMapper;
    private EditorAgent editorAgent;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        objectMapper = new ObjectMapper();
        editorAgent = new EditorAgent(
            eventBus,
            summarizationService,
            changeEngine,
            geminiClient,
            itineraryJsonService,
            objectMapper,
            llmResponseHandler
        );
    }
    
    @ParameterizedTest
    @CsvSource({
        "00:00",
        "09:00",
        "12:30",
        "14:45",
        "18:30",
        "23:59"
    })
    @DisplayName("Valid 24-hour time strings should convert to timestamps")
    void validTimeStringsShouldConvert(String timeStr) throws Exception {
        Long timestamp = convertTimeStringViaReflection(timeStr);
        
        assertNotNull(timestamp, "Timestamp should not be null for: " + timeStr);
        assertTrue(timestamp > 0, "Timestamp should be positive for: " + timeStr);
    }
    
    @ParameterizedTest
    @CsvSource({
        "24:00",  // Invalid hour
        "12:60",  // Invalid minute
        "9:00",   // Missing leading zero
        "12:5",   // Missing leading zero
        "1200",   // No colon
        "12:",    // Missing minute
        ":30",    // Missing hour
        "12:30:00" // Too many parts
    })
    @DisplayName("Invalid time strings should return null")
    void invalidTimeStringsShouldReturnNull(String timeStr) throws Exception {
        Long timestamp = convertTimeStringViaReflection(timeStr);
        assertNull(timestamp, "Invalid time should return null: " + timeStr);
    }
    
    @Test
    @DisplayName("Null time string should return null")
    void nullTimeStringShouldReturnNull() throws Exception {
        Long timestamp = convertTimeStringViaReflection(null);
        assertNull(timestamp);
    }
    
    @Test
    @DisplayName("Time conversion should handle edge cases")
    void timeConversionShouldHandleEdgeCases() throws Exception {
        // Midnight
        Long midnight = convertTimeStringViaReflection("00:00");
        assertNotNull(midnight);
        
        // Just before midnight
        Long almostMidnight = convertTimeStringViaReflection("23:59");
        assertNotNull(almostMidnight);
        
        // Midnight should be less than almost midnight
        assertTrue(midnight < almostMidnight);
    }
    
    @Test
    @DisplayName("convertTimeStringsToTimestamps should convert all time fields in ops")
    void convertTimeStringsToTimestampsShouldConvertAllFields() throws Exception {
        String json = """
            {
              "ops": [
                {
                  "op": "move",
                  "id": "123",
                  "startTime": "14:30",
                  "endTime": "15:30"
                },
                {
                  "op": "move",
                  "id": "456",
                  "startTime": "18:00",
                  "endTime": "19:00"
                }
              ]
            }
            """;
        
        JsonNode jsonNode = objectMapper.readTree(json);
        JsonNode converted = convertTimeStringsToTimestampsViaReflection(jsonNode);
        
        // Check first op
        JsonNode firstOp = converted.get("ops").get(0);
        assertTrue(firstOp.get("startTime").isNumber(), "startTime should be converted to number");
        assertTrue(firstOp.get("endTime").isNumber(), "endTime should be converted to number");
        
        // Check second op
        JsonNode secondOp = converted.get("ops").get(1);
        assertTrue(secondOp.get("startTime").isNumber(), "startTime should be converted to number");
        assertTrue(secondOp.get("endTime").isNumber(), "endTime should be converted to number");
    }
    
    @Test
    @DisplayName("convertTimeStringsToTimestamps should handle ops without times")
    void convertTimeStringsToTimestampsShouldHandleOpsWithoutTimes() throws Exception {
        String json = """
            {
              "ops": [
                {
                  "op": "delete",
                  "id": "123"
                }
              ]
            }
            """;
        
        JsonNode jsonNode = objectMapper.readTree(json);
        JsonNode converted = convertTimeStringsToTimestampsViaReflection(jsonNode);
        
        JsonNode op = converted.get("ops").get(0);
        assertFalse(op.has("startTime"), "Op without startTime should remain without it");
        assertFalse(op.has("endTime"), "Op without endTime should remain without it");
    }
    
    @Test
    @DisplayName("convertTimeStringsToTimestamps should handle already-converted timestamps")
    void convertTimeStringsToTimestampsShouldHandleAlreadyConvertedTimestamps() throws Exception {
        String json = """
            {
              "ops": [
                {
                  "op": "move",
                  "id": "123",
                  "startTime": 1778700600000,
                  "endTime": 1778704200000
                }
              ]
            }
            """;
        
        JsonNode jsonNode = objectMapper.readTree(json);
        JsonNode converted = convertTimeStringsToTimestampsViaReflection(jsonNode);
        
        JsonNode op = converted.get("ops").get(0);
        assertEquals(1778700600000L, op.get("startTime").asLong());
        assertEquals(1778704200000L, op.get("endTime").asLong());
    }
    
    @Test
    @DisplayName("convertTimeStringsToTimestamps should handle invalid JSON gracefully")
    void convertTimeStringsToTimestampsShouldHandleInvalidJson() throws Exception {
        JsonNode nullNode = null;
        JsonNode result = convertTimeStringsToTimestampsViaReflection(nullNode);
        assertNull(result);
        
        JsonNode stringNode = objectMapper.readTree("\"test\"");
        JsonNode result2 = convertTimeStringsToTimestampsViaReflection(stringNode);
        assertEquals(stringNode, result2);
    }
    
    /**
     * Use reflection to access private convertTimeStringToTimestamp method.
     */
    private Long convertTimeStringViaReflection(String timeStr) throws Exception {
        Method method = EditorAgent.class.getDeclaredMethod("convertTimeStringToTimestamp", String.class);
        method.setAccessible(true);
        return (Long) method.invoke(editorAgent, timeStr);
    }
    
    /**
     * Use reflection to access private convertTimeStringsToTimestamps method.
     */
    private JsonNode convertTimeStringsToTimestampsViaReflection(JsonNode jsonNode) throws Exception {
        Method method = EditorAgent.class.getDeclaredMethod("convertTimeStringsToTimestamps", JsonNode.class);
        method.setAccessible(true);
        return (JsonNode) method.invoke(editorAgent, jsonNode);
    }
}





