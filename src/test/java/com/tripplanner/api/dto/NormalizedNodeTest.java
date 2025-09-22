package com.tripplanner.api.dto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class NormalizedNodeTest {

    private NormalizedNode node;

    @BeforeEach
    void setUp() {
        node = new NormalizedNode("test-node", "attraction", "Test Attraction");
    }

    @Test
    void testDefaultValues() {
        assertEquals("test-node", node.getId());
        assertEquals("attraction", node.getType());
        assertEquals("Test Attraction", node.getTitle());
        assertEquals("planned", node.getStatus());
        assertFalse(node.getLocked());
        assertNull(node.getBookingRef());
        assertNotNull(node.getUpdatedAt());
        assertNull(node.getUpdatedBy());
    }

    @Test
    void testStatusHelpers() {
        // Test planned status
        assertTrue(node.isPlanned());
        assertFalse(node.isInProgress());
        assertFalse(node.isSkipped());
        assertFalse(node.isCancelled());
        assertFalse(node.isCompleted());

        // Test status transitions
        node.setStatus("in_progress");
        assertTrue(node.isInProgress());
        assertFalse(node.isPlanned());

        node.setStatus("completed");
        assertTrue(node.isCompleted());
        assertFalse(node.isInProgress());
    }

    @Test
    void testBookingStatus() {
        assertFalse(node.isBooked());

        node.setBookingRef("BOOK123");
        assertTrue(node.isBooked());

        node.setBookingRef("");
        assertFalse(node.isBooked());

        node.setBookingRef(null);
        assertFalse(node.isBooked());
    }

    @Test
    void testMarkAsUpdated() {
        Instant beforeUpdate = node.getUpdatedAt();
        
        // Wait a small amount to ensure timestamp difference
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        node.markAsUpdated("user");
        
        assertEquals("user", node.getUpdatedBy());
        assertTrue(node.getUpdatedAt().isAfter(beforeUpdate));
    }

    @Test
    void testStatusTransitions() {
        // Test valid transitions from planned
        assertTrue(node.canTransitionTo("in_progress"));
        assertTrue(node.canTransitionTo("skipped"));
        assertTrue(node.canTransitionTo("cancelled"));
        assertFalse(node.canTransitionTo("completed"));

        // Test valid transitions from in_progress
        node.setStatus("in_progress");
        assertTrue(node.canTransitionTo("completed"));
        assertTrue(node.canTransitionTo("skipped"));
        assertTrue(node.canTransitionTo("cancelled"));
        assertFalse(node.canTransitionTo("planned"));

        // Test valid transitions from completed
        node.setStatus("completed");
        assertTrue(node.canTransitionTo("planned"));
        assertTrue(node.canTransitionTo("in_progress"));
        assertFalse(node.canTransitionTo("skipped"));
        assertFalse(node.canTransitionTo("cancelled"));

        // Test valid transitions from skipped
        node.setStatus("skipped");
        assertTrue(node.canTransitionTo("planned"));
        assertTrue(node.canTransitionTo("in_progress"));
        assertFalse(node.canTransitionTo("completed"));
        assertFalse(node.canTransitionTo("cancelled"));
    }

    @Test
    void testInvalidStatusTransitions() {
        // Test null status
        assertFalse(node.canTransitionTo(null));
        
        // Test invalid status
        assertFalse(node.canTransitionTo("invalid_status"));
        
        // Test invalid transition from planned to completed
        assertFalse(node.canTransitionTo("completed"));
    }

    @Test
    void testLocationAccess() {
        // Test without location
        assertNull(node.getLocation());

        // Test with location
        NodeLocation location = new NodeLocation();
        location.setName("Barcelona");
        node.setLocation(location);
        assertNotNull(node.getLocation());
        assertEquals("Barcelona", node.getLocation().getName());
    }

    @Test
    void testToString() {
        String toString = node.toString();
        assertTrue(toString.contains("test-node"));
        assertTrue(toString.contains("attraction"));
        assertTrue(toString.contains("Test Attraction"));
        assertTrue(toString.contains("planned"));
    }

    @Test
    void testEqualsAndHashCode() {
        NormalizedNode node1 = new NormalizedNode("same-id", "attraction", "Test");
        NormalizedNode node2 = new NormalizedNode("same-id", "meal", "Different");
        NormalizedNode node3 = new NormalizedNode("different-id", "attraction", "Test");

        // Same ID should be equal
        assertEquals(node1, node2);
        assertEquals(node1.hashCode(), node2.hashCode());

        // Different ID should not be equal
        assertNotEquals(node1, node3);
        assertNotEquals(node1.hashCode(), node3.hashCode());
        
        // Test null ID
        NormalizedNode node4 = new NormalizedNode();
        NormalizedNode node5 = new NormalizedNode();
        assertEquals(node4, node5);
        assertEquals(node4.hashCode(), node5.hashCode());
    }
}
