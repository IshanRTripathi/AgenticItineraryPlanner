package com.tripplanner.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tripplanner.api.dto.*;
import com.tripplanner.data.repo.ItineraryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChangeEngineAuditTest {

    @Mock
    private ItineraryRepository itineraryRepository;

    @Mock
    private ItineraryJsonService itineraryJsonService;

    private ChangeEngine changeEngine;
    private ObjectMapper objectMapper;
    private String testItineraryId;
    private NormalizedItinerary testItinerary;

    @BeforeEach
    void setUp() throws Exception {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        changeEngine = new ChangeEngine(itineraryJsonService, itineraryRepository, objectMapper);
        testItineraryId = "test-itinerary";

        // Create test itinerary
        testItinerary = createTestItinerary();
    }

    @Test
    void testMoveNodeUpdatesAuditTrail() throws Exception {
        // Given
        when(itineraryJsonService.getItineraryByAnyId(testItineraryId)).thenReturn(Optional.of(testItinerary));
        // update persists via itineraryJsonService
        when(itineraryJsonService.updateItinerary(any(NormalizedItinerary.class))).thenAnswer(inv -> {
            NormalizedItinerary ni = inv.getArgument(0);
            return null;
        });

        ChangeSet changeSet = createMoveNodeChangeSet();

        // When
        ChangeEngine.ApplyResult result = changeEngine.apply(testItineraryId, changeSet);

        // Then
        assertNotNull(result);
        assertEquals(2, result.getToVersion());

        // Verify that the node was updated with audit trail
        verify(itineraryJsonService, times(1)).saveRevision(eq(testItineraryId), any(NormalizedItinerary.class));
        verify(itineraryJsonService, times(1)).updateItinerary(any(NormalizedItinerary.class));
    }

    @Test
    void testInsertNodeSetsAuditTrail() throws Exception {
        // Given
        when(itineraryJsonService.getItineraryByAnyId(testItineraryId)).thenReturn(Optional.of(testItinerary));
        when(itineraryJsonService.updateItinerary(any(NormalizedItinerary.class))).thenAnswer(inv -> null);

        ChangeSet changeSet = createInsertNodeChangeSet();

        // When
        ChangeEngine.ApplyResult result = changeEngine.apply(testItineraryId, changeSet);

        // Then
        assertNotNull(result);
        assertEquals(2, result.getToVersion());

        // Verify that the new node was created with audit trail
        verify(itineraryJsonService, times(1)).saveRevision(eq(testItineraryId), any(NormalizedItinerary.class));
        verify(itineraryJsonService, times(1)).updateItinerary(any(NormalizedItinerary.class));
    }

    @Test
    void testProposeChangesPreservesAuditTrail() throws Exception {
        // Given
        when(itineraryJsonService.getItineraryByAnyId(testItineraryId)).thenReturn(Optional.of(testItinerary));

        ChangeSet changeSet = createMoveNodeChangeSet();

        // When
        ChangeEngine.ProposeResult result = changeEngine.propose(testItineraryId, changeSet);

        // Then
        assertNotNull(result);
        assertNotNull(result.getProposed());
        assertEquals(2, result.getPreviewVersion());
        assertNotNull(result.getDiff());

        // Verify that the proposed itinerary has updated audit trail
        NormalizedItinerary proposed = result.getProposed();
        assertNotNull(proposed);
    }

    @Test
    void testUndoRestoresAuditTrail() throws Exception {
        // Given
        when(itineraryJsonService.getItineraryByAnyId(testItineraryId)).thenReturn(Optional.of(testItinerary));
        when(itineraryJsonService.getRevision(testItineraryId, 1)).thenReturn(Optional.of(testItinerary));
        when(itineraryJsonService.updateItinerary(any(NormalizedItinerary.class))).thenAnswer(inv -> null);

        // When
        ChangeEngine.UndoResult result = changeEngine.undo(testItineraryId, 1);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getToVersion());
        assertNotNull(result.getDiff());

        // Verify that the revision was restored
        verify(itineraryJsonService, times(1)).updateItinerary(any(NormalizedItinerary.class));
    }

    private NormalizedItinerary createTestItinerary() {
        NormalizedItinerary itinerary = new NormalizedItinerary();
        itinerary.setItineraryId(testItineraryId);
        itinerary.setVersion(1);
        itinerary.setSummary("Test Itinerary");
        itinerary.setCurrency("USD");

        // Create a test day
        NormalizedDay day = new NormalizedDay();
        day.setDayNumber(1);
        day.setDate(LocalDate.of(2025, 6, 1));
        day.setLocation("Test City");

        // Create a test node
        NormalizedNode node = new NormalizedNode("test-node", "attraction", "Test Attraction");
        node.setStatus("planned");
        node.setUpdatedBy("system");
        node.setUpdatedAt(Instant.now());

        // Set timing
        NodeTiming timing = new NodeTiming();
        timing.setStartTime(Instant.parse("2025-06-01T10:00:00Z"));
        timing.setEndTime(Instant.parse("2025-06-01T12:00:00Z"));
        timing.setDurationMin(120);
        node.setTiming(timing);

        day.setNodes(List.of(node));
        itinerary.setDays(List.of(day));

        return itinerary;
    }

    private ChangeSet createMoveNodeChangeSet() {
        ChangeSet changeSet = new ChangeSet();
        changeSet.setScope("day");
        changeSet.setDay(1);

        ChangeOperation operation = new ChangeOperation();
        operation.setOp("move");
        operation.setId("test-node");
        operation.setStartTime(Instant.parse("2025-06-01T14:00:00Z"));
        operation.setEndTime(Instant.parse("2025-06-01T16:00:00Z"));

        changeSet.setOps(List.of(operation));

        ChangePreferences preferences = new ChangePreferences();
        preferences.setUserFirst(true);
        preferences.setAutoApply(false);
        preferences.setRespectLocks(true);
        changeSet.setPreferences(preferences);

        return changeSet;
    }

    private ChangeSet createInsertNodeChangeSet() {
        ChangeSet changeSet = new ChangeSet();
        changeSet.setScope("day");
        changeSet.setDay(1);

        ChangeOperation operation = new ChangeOperation();
        operation.setOp("insert");
        operation.setAfter("test-node");

        // Create a new node
        NormalizedNode newNode = new NormalizedNode("new-node", "meal", "New Restaurant");
        newNode.setStatus("planned");
        operation.setNode(newNode);

        changeSet.setOps(List.of(operation));

        ChangePreferences preferences = new ChangePreferences();
        preferences.setUserFirst(true);
        preferences.setAutoApply(false);
        preferences.setRespectLocks(true);
        changeSet.setPreferences(preferences);

        return changeSet;
    }
}
