package com.tripplanner.service;

import com.tripplanner.api.dto.IntentResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class IntentClassificationServiceTest {

    private IntentClassificationService intentClassificationService;

    @BeforeEach
    void setUp() {
        intentClassificationService = new IntentClassificationService();
    }

    @Test
    void testPreRouterClassification() {
        // Test replan today
        IntentResult result = intentClassificationService.classifyIntent("replan from now", null, 1);
        assertEquals("REPLAN_TODAY", result.getIntent());
        assertEquals(1, result.getDay());

        // Test move time
        result = intentClassificationService.classifyIntent("move lunch to 2pm", "n_lunch", null);
        assertEquals("MOVE_TIME", result.getIntent());
        assertNotNull(result.getNodeIds());
        assertTrue(result.getNodeIds().contains("n_lunch"));

        // Test insert place
        result = intentClassificationService.classifyIntent("add a restaurant", null, 1);
        assertEquals("INSERT_PLACE", result.getIntent());
        assertEquals(1, result.getDay());

        // Test delete node
        result = intentClassificationService.classifyIntent("remove this place", "n_place", null);
        assertEquals("DELETE_NODE", result.getIntent());
        assertNotNull(result.getNodeIds());
        assertTrue(result.getNodeIds().contains("n_place"));

        // Test replace node
        result = intentClassificationService.classifyIntent("replace with museum", "n_place", null);
        assertEquals("REPLACE_NODE", result.getIntent());
        assertNotNull(result.getNodeIds());
        assertTrue(result.getNodeIds().contains("n_place"));

        // Test book node
        result = intentClassificationService.classifyIntent("book this place", "n_place", null);
        assertEquals("BOOK_NODE", result.getIntent());
        assertNotNull(result.getNodeIds());
        assertTrue(result.getNodeIds().contains("n_place"));

        // Test undo
        result = intentClassificationService.classifyIntent("undo last change", null, null);
        assertEquals("UNDO", result.getIntent());

        // Test explain
        result = intentClassificationService.classifyIntent("what's in my itinerary?", null, null);
        assertEquals("EXPLAIN", result.getIntent());
    }

    @Test
    void testLlmClassification() {
        // Test fallback for ambiguous text
        IntentResult result = intentClassificationService.classifyIntent("ambiguous text", null, null);
        assertNotNull(result);
        assertNotNull(result.getIntent());
    }

    @Test
    void testEntityExtraction() {
        // Test that entities are extracted from text
        IntentResult result = intentClassificationService.classifyIntent("move lunch to 2pm near Sagrada", "n_lunch", null);
        assertEquals("MOVE_TIME", result.getIntent());
        assertNotNull(result.getEntities());
        // The entities should contain time and location information
    }

    @Test
    void testContextAwareClassification() {
        // Test classification with different contexts
        IntentResult result1 = intentClassificationService.classifyIntent("move this", "n_lunch", null);
        assertEquals("MOVE_TIME", result1.getIntent());
        assertNotNull(result1.getNodeIds());
        assertTrue(result1.getNodeIds().contains("n_lunch"));

        IntentResult result2 = intentClassificationService.classifyIntent("add restaurant", null, 1);
        assertEquals("INSERT_PLACE", result2.getIntent());
        assertEquals(1, result2.getDay());
    }
}
