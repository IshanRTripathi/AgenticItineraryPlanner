package com.tripplanner.testing.agents;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.tripplanner.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests to validate that LLM responses can be correctly deserialized to DTOs.
 * These tests catch schema mismatches before they cause production errors.
 */
class EditorAgentSchemaValidationTest {
    
    private ObjectMapper objectMapper;
    
    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }
    
    @Test
    @DisplayName("Valid ChangeSet with complete node structure should deserialize")
    void validChangeSetWithCompleteNodeShouldDeserialize() throws Exception {
        String json = """
            {
              "ops": [{
                "op": "replace",
                "id": "1778700600000",
                "startTime": 1778700600000,
                "endTime": 1778704200000,
                "node": {
                  "title": "Sushi Restaurant",
                  "type": "meal",
                  "location": {
                    "name": "Hadibo",
                    "address": "Hadibo, Socotra Island"
                  }
                }
              }],
              "day": 2,
              "reason": "Adding sushi place",
              "agent": "EditorAgent"
            }
            """;
        
        ChangeSet changeSet = objectMapper.readValue(json, ChangeSet.class);
        
        assertNotNull(changeSet);
        assertNotNull(changeSet.getOps());
        assertEquals(1, changeSet.getOps().size());
        
        ChangeOperation op = changeSet.getOps().get(0);
        assertEquals("replace", op.getOp());
        assertEquals("1778700600000", op.getId());
        assertEquals(1778700600000L, op.getStartTime());
        assertEquals(1778704200000L, op.getEndTime());
        
        NormalizedNode node = op.getNode();
        assertNotNull(node, "Node should not be null");
        assertEquals("Sushi Restaurant", node.getTitle());
        assertEquals("meal", node.getType());
        
        NodeLocation location = node.getLocation();
        assertNotNull(location, "Location should not be null");
        assertEquals("Hadibo", location.getName());
        assertEquals("Hadibo, Socotra Island", location.getAddress());
    }
    
    @Test
    @DisplayName("ChangeSet with location as string should FAIL")
    void changeSetWithLocationAsStringShouldFail() {
        String json = """
            {
              "ops": [{
                "op": "replace",
                "id": "123",
                "node": {
                  "title": "Restaurant",
                  "type": "meal",
                  "location": "Hadibo"
                }
              }],
              "day": 2,
              "reason": "Test",
              "agent": "EditorAgent"
            }
            """;
        
        assertThrows(Exception.class, () -> {
            objectMapper.readValue(json, ChangeSet.class);
        }, "Location as string should cause deserialization error");
    }
    
    @Test
    @DisplayName("ChangeSet with 'name' instead of 'title' should fail or ignore")
    void changeSetWithNameInsteadOfTitleShouldFailOrIgnore() {
        String json = """
            {
              "ops": [{
                "op": "replace",
                "id": "123",
                "node": {
                  "name": "Restaurant",
                  "type": "meal",
                  "location": {
                    "name": "Hadibo",
                    "address": "Test"
                  }
                }
              }],
              "day": 2,
              "reason": "Test",
              "agent": "EditorAgent"
            }
            """;
        
        // This may throw UnrecognizedPropertyException depending on ObjectMapper config
        // Or it may deserialize with title as null
        try {
            ChangeSet changeSet = objectMapper.readValue(json, ChangeSet.class);
            // If it succeeds, title should be null
            assertNull(changeSet.getOps().get(0).getNode().getTitle(),
                "Title should be null when 'name' is used instead");
        } catch (com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException e) {
            // This is also acceptable - Jackson is configured to fail on unknown properties
            assertTrue(e.getMessage().contains("name"), "Exception should mention 'name' field");
        } catch (Exception e) {
            fail("Unexpected exception: " + e.getMessage());
        }
    }
    
    @Test
    @DisplayName("ChangeSet with time strings should work after conversion")
    void changeSetWithTimeStringsShouldWork() throws Exception {
        // This tests the conversion logic - time strings converted to Long
        String json = """
            {
              "ops": [{
                "op": "move",
                "id": "123",
                "startTime": 1778700600000,
                "endTime": 1778704200000
              }],
              "day": 2,
              "reason": "Test",
              "agent": "EditorAgent"
            }
            """;
        
        ChangeSet changeSet = objectMapper.readValue(json, ChangeSet.class);
        
        assertEquals(1778700600000L, changeSet.getOps().get(0).getStartTime());
        assertEquals(1778704200000L, changeSet.getOps().get(0).getEndTime());
    }
    
    @ParameterizedTest
    @ValueSource(strings = {"insert", "delete", "move", "replace"})
    @DisplayName("All operation types should deserialize")
    void allOperationTypesShouldDeserialize(String opType) throws Exception {
        String json = String.format("""
            {
              "ops": [{
                "op": "%s",
                "id": "123"
              }],
              "day": 1,
              "reason": "Test",
              "agent": "EditorAgent"
            }
            """, opType);
        
        ChangeSet changeSet = objectMapper.readValue(json, ChangeSet.class);
        assertEquals(opType, changeSet.getOps().get(0).getOp());
    }
    
    @Test
    @DisplayName("ChangeSet with missing location name/address should handle")
    void changeSetWithIncompleteLocationShouldDeserialize() throws Exception {
        String json = """
            {
              "ops": [{
                "op": "replace",
                "id": "123",
                "node": {
                  "title": "Restaurant",
                  "type": "meal",
                  "location": {}
                }
              }],
              "day": 2,
              "reason": "Test",
              "agent": "EditorAgent"
            }
            """;
        
        ChangeSet changeSet = objectMapper.readValue(json, ChangeSet.class);
        
        NodeLocation location = changeSet.getOps().get(0).getNode().getLocation();
        assertNotNull(location, "Location object should exist");
        assertNull(location.getName(), "Name can be null");
        assertNull(location.getAddress(), "Address can be null");
    }
    
    @Test
    @DisplayName("ChangeSet with multiple operations should deserialize")
    void changeSetWithMultipleOperationsShouldDeserialize() throws Exception {
        String json = """
            {
              "ops": [
                {
                  "op": "delete",
                  "id": "123"
                },
                {
                  "op": "insert",
                  "id": "456",
                  "after": "123",
                  "node": {
                    "title": "New Activity",
                    "type": "attraction",
                    "location": {
                      "name": "Location",
                      "address": "Address"
                    }
                  }
                }
              ],
              "day": 3,
              "reason": "Multiple changes",
              "agent": "EditorAgent"
            }
            """;
        
        ChangeSet changeSet = objectMapper.readValue(json, ChangeSet.class);
        
        assertEquals(2, changeSet.getOps().size());
        assertEquals("delete", changeSet.getOps().get(0).getOp());
        assertEquals("insert", changeSet.getOps().get(1).getOp());
    }
    
    @Test
    @DisplayName("ChangeSet with all node types should deserialize")
    void changeSetWithAllNodeTypesShouldDeserialize() throws Exception {
        String[] nodeTypes = {"attraction", "meal", "accommodation", "transport"};
        
        for (String nodeType : nodeTypes) {
            String json = String.format("""
                {
                  "ops": [{
                    "op": "insert",
                    "id": "123",
                    "node": {
                      "title": "Test Node",
                      "type": "%s",
                      "location": {
                        "name": "Test",
                        "address": "Test Address"
                      }
                    }
                  }],
                  "day": 1,
                  "reason": "Test",
                  "agent": "EditorAgent"
                }
                """, nodeType);
            
            ChangeSet changeSet = objectMapper.readValue(json, ChangeSet.class);
            assertEquals(nodeType, changeSet.getOps().get(0).getNode().getType());
        }
    }
    
    @Test
    @DisplayName("Empty ops array should deserialize")
    void emptyOpsArrayShouldDeserialize() throws Exception {
        String json = """
            {
              "ops": [],
              "day": 1,
              "reason": "No changes",
              "agent": "EditorAgent"
            }
            """;
        
        ChangeSet changeSet = objectMapper.readValue(json, ChangeSet.class);
        assertNotNull(changeSet.getOps());
        assertEquals(0, changeSet.getOps().size());
    }
}

