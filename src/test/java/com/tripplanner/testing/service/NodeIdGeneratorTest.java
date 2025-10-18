package com.tripplanner.testing.service;

import com.tripplanner.dto.NormalizedNode;
import com.tripplanner.service.NodeIdGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive tests for NodeIdGenerator service.
 * Tests ID generation patterns, uniqueness, and edge cases.
 */
@DisplayName("NodeIdGenerator Tests")
class NodeIdGeneratorTest {

    private NodeIdGenerator nodeIdGenerator;

    @BeforeEach
    void setUp() {
        nodeIdGenerator = new NodeIdGenerator();
    }

    @Test
    @DisplayName("Should generate valid node IDs with all parameters")
    void testGenerateNodeId_WithAllParameters() {
        // Given
        String nodeType = "attraction";
        Integer dayNumber = 1;

        // When
        String id = nodeIdGenerator.generateNodeId(nodeType, dayNumber);

        // Then
        assertThat(id).isNotNull();
        assertThat(id).startsWith("node_");
        assertThat(id).contains("att_day1");
        assertThat(id).matches("node_att_day1_\\d+_[a-f0-9]{8}");
        assertThat(nodeIdGenerator.isValidNodeId(id)).isTrue();
    }

    @Test
    @DisplayName("Should generate valid node IDs without day number")
    void testGenerateNodeId_WithoutDayNumber() {
        // Given
        String nodeType = "meal";
        Integer dayNumber = null;

        // When
        String id = nodeIdGenerator.generateNodeId(nodeType, dayNumber);

        // Then
        assertThat(id).isNotNull();
        assertThat(id).startsWith("node_");
        assertThat(id).contains("mea");
        assertThat(id).doesNotContain("_day");
        assertThat(id).matches("node_mea_\\d+_[a-f0-9]{8}");
        assertThat(nodeIdGenerator.isValidNodeId(id)).isTrue();
    }

    @Test
    @DisplayName("Should generate unique IDs for multiple calls")
    void testGenerateNodeId_Uniqueness() {
        // Given
        String nodeType = "transport";
        Integer dayNumber = 2;

        // When
        String id1 = nodeIdGenerator.generateNodeId(nodeType, dayNumber);
        String id2 = nodeIdGenerator.generateNodeId(nodeType, dayNumber);

        // Then
        assertThat(id1).isNotEqualTo(id2);
        assertThat(nodeIdGenerator.isValidNodeId(id1)).isTrue();
        assertThat(nodeIdGenerator.isValidNodeId(id2)).isTrue();
    }

    @Test
    @DisplayName("Should generate predictable skeleton node IDs")
    void testGenerateSkeletonNodeId_Predictable() {
        // Given
        int dayNumber = 1;
        int nodeIndex = 1;
        String nodeType = "attraction";

        // When
        String id = nodeIdGenerator.generateSkeletonNodeId(dayNumber, nodeIndex, nodeType);

        // Then
        assertThat(id).isEqualTo("day1_att_1");
        assertThat(nodeIdGenerator.isValidNodeId(id)).isTrue();
    }

    @Test
    @DisplayName("Should generate different skeleton IDs for different parameters")
    void testGenerateSkeletonNodeId_DifferentParameters() {
        // When
        String id1 = nodeIdGenerator.generateSkeletonNodeId(1, 1, "attraction");
        String id2 = nodeIdGenerator.generateSkeletonNodeId(1, 2, "attraction");
        String id3 = nodeIdGenerator.generateSkeletonNodeId(2, 1, "attraction");
        String id4 = nodeIdGenerator.generateSkeletonNodeId(1, 1, "meal");

        // Then
        assertThat(id1).isEqualTo("day1_att_1");
        assertThat(id2).isEqualTo("day1_att_2");
        assertThat(id3).isEqualTo("day2_att_1");
        assertThat(id4).isEqualTo("day1_mea_1");
        
        assertThat(id1).isNotEqualTo(id2);
        assertThat(id1).isNotEqualTo(id3);
        assertThat(id1).isNotEqualTo(id4);
    }

    @Test
    @DisplayName("Should generate ID for node when null")
    void testEnsureNodeHasId_GeneratesWhenNull() {
        // Given
        NormalizedNode node = new NormalizedNode();
        node.setType("attraction");
        node.setTitle("Test Attraction");
        Integer dayNumber = 1;

        // When
        nodeIdGenerator.ensureNodeHasId(node, dayNumber);

        // Then
        assertThat(node.getId()).isNotNull();
        assertThat(node.getId()).startsWith("node_");
        assertThat(node.getId()).contains("att_day1");
        assertThat(nodeIdGenerator.isValidNodeId(node.getId())).isTrue();
    }

    @Test
    @DisplayName("Should generate ID for node when empty")
    void testEnsureNodeHasId_GeneratesWhenEmpty() {
        // Given
        NormalizedNode node = new NormalizedNode();
        node.setId("");
        node.setType("meal");
        node.setTitle("Test Meal");
        Integer dayNumber = 2;

        // When
        nodeIdGenerator.ensureNodeHasId(node, dayNumber);

        // Then
        assertThat(node.getId()).isNotNull();
        assertThat(node.getId()).isNotEmpty();
        assertThat(node.getId()).startsWith("node_");
        assertThat(node.getId()).contains("mea_day2");
        assertThat(nodeIdGenerator.isValidNodeId(node.getId())).isTrue();
    }

    @Test
    @DisplayName("Should preserve existing valid ID")
    void testEnsureNodeHasId_PreservesExisting() {
        // Given
        NormalizedNode node = new NormalizedNode();
        String existingId = "day1_att_1";
        node.setId(existingId);
        node.setType("attraction");
        node.setTitle("Test Attraction");
        Integer dayNumber = 1;

        // When
        nodeIdGenerator.ensureNodeHasId(node, dayNumber);

        // Then
        assertThat(node.getId()).isEqualTo(existingId);
    }

    @Test
    @DisplayName("Should handle null node gracefully")
    void testEnsureNodeHasId_HandlesNullNode() {
        // Given
        NormalizedNode node = null;
        Integer dayNumber = 1;

        // When/Then - Should not throw exception
        assertThatCode(() -> nodeIdGenerator.ensureNodeHasId(node, dayNumber))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should sanitize null node type")
    void testSanitizeType_HandlesNullType() {
        // Given
        String nodeType = null;
        Integer dayNumber = 1;

        // When
        String id = nodeIdGenerator.generateNodeId(nodeType, dayNumber);

        // Then
        assertThat(id).isNotNull();
        assertThat(id).startsWith("node_");
        assertThat(id).contains("unk_day1");
        assertThat(nodeIdGenerator.isValidNodeId(id)).isTrue();
    }

    @Test
    @DisplayName("Should sanitize empty node type")
    void testSanitizeType_HandlesEmptyType() {
        // Given
        String nodeType = "";
        Integer dayNumber = 1;

        // When
        String id = nodeIdGenerator.generateNodeId(nodeType, dayNumber);

        // Then
        assertThat(id).isNotNull();
        assertThat(id).startsWith("node_");
        assertThat(id).contains("unk_day1");
        assertThat(nodeIdGenerator.isValidNodeId(id)).isTrue();
    }

    @Test
    @DisplayName("Should sanitize short node types")
    void testSanitizeType_HandlesShortTypes() {
        // Given
        String shortType = "ab";
        Integer dayNumber = 1;

        // When
        String id = nodeIdGenerator.generateNodeId(shortType, dayNumber);

        // Then
        assertThat(id).isNotNull();
        assertThat(id).startsWith("node_");
        assertThat(id).contains("ab_day1");
        assertThat(nodeIdGenerator.isValidNodeId(id)).isTrue();
    }

    @Test
    @DisplayName("Should truncate long node types to 3 characters")
    void testSanitizeType_TruncatesLongTypes() {
        // Given
        String longType = "verylongnodetype";
        Integer dayNumber = 1;

        // When
        String id = nodeIdGenerator.generateNodeId(longType, dayNumber);

        // Then
        assertThat(id).isNotNull();
        assertThat(id).startsWith("node_");
        assertThat(id).contains("ver_day1");
        assertThat(id).doesNotContain("verylongnodetype");
        assertThat(nodeIdGenerator.isValidNodeId(id)).isTrue();
    }

    @Test
    @DisplayName("Should validate skeleton node IDs correctly")
    void testIsValidNodeId_SkeletonFormat() {
        // Valid skeleton IDs
        assertThat(nodeIdGenerator.isValidNodeId("day1_att_1")).isTrue();
        assertThat(nodeIdGenerator.isValidNodeId("day2_mea_3")).isTrue();
        assertThat(nodeIdGenerator.isValidNodeId("day10_tra_5")).isTrue();

        // Invalid skeleton IDs
        assertThat(nodeIdGenerator.isValidNodeId("day_att_1")).isFalse(); // Missing number
        assertThat(nodeIdGenerator.isValidNodeId("day1_att")).isFalse(); // Missing index
        assertThat(nodeIdGenerator.isValidNodeId("day1_att_")).isFalse(); // Empty index
    }

    @Test
    @DisplayName("Should validate general node IDs correctly")
    void testIsValidNodeId_GeneralFormat() {
        // Valid general IDs
        String id1 = "node_att_day1_1234_abcd1234";
        System.out.println("Testing: " + id1 + " -> " + nodeIdGenerator.isValidNodeId(id1));
        assertThat(nodeIdGenerator.isValidNodeId(id1)).isTrue();
        
        String id2 = "node_mea_5678_efgh5678";
        System.out.println("Testing: " + id2 + " -> " + nodeIdGenerator.isValidNodeId(id2));
        assertThat(nodeIdGenerator.isValidNodeId(id2)).isTrue();
        
        String id3 = "node_tra_day2_9999_ijkl9999";
        System.out.println("Testing: " + id3 + " -> " + nodeIdGenerator.isValidNodeId(id3));
        assertThat(nodeIdGenerator.isValidNodeId(id3)).isTrue();

        // Invalid general IDs
        assertThat(nodeIdGenerator.isValidNodeId("node_att_day1_1234_abcd")).isFalse(); // Short UUID
        assertThat(nodeIdGenerator.isValidNodeId("node_att_day1_1234_abcd12345")).isFalse(); // Long UUID
        assertThat(nodeIdGenerator.isValidNodeId("node_att_day1_abcd1234")).isFalse(); // Missing timestamp
        assertThat(nodeIdGenerator.isValidNodeId("nod_att_day1_1234_abcd1234")).isFalse(); // Wrong prefix
    }

    @Test
    @DisplayName("Should reject null and empty IDs")
    void testIsValidNodeId_NullAndEmpty() {
        assertThat(nodeIdGenerator.isValidNodeId(null)).isFalse();
        assertThat(nodeIdGenerator.isValidNodeId("")).isFalse();
        assertThat(nodeIdGenerator.isValidNodeId("   ")).isFalse();
    }

    @Test
    @DisplayName("Should handle whitespace in node types")
    void testSanitizeType_HandlesWhitespace() {
        // Given
        String nodeTypeWithSpaces = "  attraction  ";
        Integer dayNumber = 1;

        // When
        String id = nodeIdGenerator.generateNodeId(nodeTypeWithSpaces, dayNumber);

        // Then
        assertThat(id).isNotNull();
        assertThat(id).startsWith("node_");
        assertThat(id).contains("att_day1");
        assertThat(nodeIdGenerator.isValidNodeId(id)).isTrue();
    }

    @Test
    @DisplayName("Should generate consistent IDs for same parameters within short time")
    void testGenerateNodeId_Consistency() {
        // Given
        String nodeType = "attraction";
        Integer dayNumber = 1;

        // When - Generate multiple IDs quickly
        String id1 = nodeIdGenerator.generateNodeId(nodeType, dayNumber);
        String id2 = nodeIdGenerator.generateNodeId(nodeType, dayNumber);
        String id3 = nodeIdGenerator.generateNodeId(nodeType, dayNumber);

        // Then - All should be different due to UUID component
        assertThat(id1).isNotEqualTo(id2);
        assertThat(id2).isNotEqualTo(id3);
        assertThat(id1).isNotEqualTo(id3);

        // But all should follow the same pattern
        assertThat(id1).matches("node_att_day1_\\d+_[a-f0-9]{8}");
        assertThat(id2).matches("node_att_day1_\\d+_[a-f0-9]{8}");
        assertThat(id3).matches("node_att_day1_\\d+_[a-f0-9]{8}");
    }
}
