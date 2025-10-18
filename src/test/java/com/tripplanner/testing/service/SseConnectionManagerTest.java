package com.tripplanner.testing.service;

import com.tripplanner.dto.ItineraryUpdateEvent;
import com.tripplanner.service.SseConnectionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SseConnectionManager.
 */
public class SseConnectionManagerTest {
    
    private SseConnectionManager sseConnectionManager;
    
    @Mock
    private SseEmitter mockEmitter1;
    
    @Mock
    private SseEmitter mockEmitter2;
    
    private static final String TEST_ITINERARY_ID = "test-itinerary-123";
    private static final String TEST_EXECUTION_ID = "exec-456";
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        sseConnectionManager = new SseConnectionManager();
    }
    
    @Test
    @DisplayName("Should register connection successfully")
    void shouldRegisterConnectionSuccessfully() {
        // When
        sseConnectionManager.registerConnection(TEST_ITINERARY_ID, TEST_EXECUTION_ID, mockEmitter1);
        
        // Then
        assertTrue(sseConnectionManager.hasActiveConnections(TEST_ITINERARY_ID));
        assertEquals(1, sseConnectionManager.getConnectionCount(TEST_ITINERARY_ID));
        
        // Verify emitter handlers were set up
        verify(mockEmitter1).onCompletion(any(Runnable.class));
        verify(mockEmitter1).onTimeout(any(Runnable.class));
        verify(mockEmitter1).onError(any());
    }
    
    @Test
    @DisplayName("Should register multiple connections for same itinerary")
    void shouldRegisterMultipleConnectionsForSameItinerary() {
        // When
        sseConnectionManager.registerConnection(TEST_ITINERARY_ID, TEST_EXECUTION_ID, mockEmitter1);
        sseConnectionManager.registerConnection(TEST_ITINERARY_ID, TEST_EXECUTION_ID, mockEmitter2);
        
        // Then
        assertTrue(sseConnectionManager.hasActiveConnections(TEST_ITINERARY_ID));
        assertEquals(2, sseConnectionManager.getConnectionCount(TEST_ITINERARY_ID));
    }
    
    @Test
    @DisplayName("Should broadcast update to all connections")
    void shouldBroadcastUpdateToAllConnections() throws IOException {
        // Given
        sseConnectionManager.registerConnection(TEST_ITINERARY_ID, TEST_EXECUTION_ID, mockEmitter1);
        sseConnectionManager.registerConnection(TEST_ITINERARY_ID, TEST_EXECUTION_ID, mockEmitter2);
        
        ItineraryUpdateEvent event = ItineraryUpdateEvent.dayCompleted(
            TEST_ITINERARY_ID, TEST_EXECUTION_ID, 1, null, 25, "Day 1 completed"
        );
        
        // When
        sseConnectionManager.broadcastUpdate(TEST_ITINERARY_ID, event);
        
        // Then - Each emitter should be called twice: once for connection confirmation, once for broadcast
        verify(mockEmitter1, times(2)).send(any(SseEmitter.SseEventBuilder.class));
        verify(mockEmitter2, times(2)).send(any(SseEmitter.SseEventBuilder.class));
    }
    
    @Test
    @DisplayName("Should handle dead connections during broadcast")
    void shouldHandleDeadConnectionsDuringBroadcast() throws IOException {
        // Given
        sseConnectionManager.registerConnection(TEST_ITINERARY_ID, TEST_EXECUTION_ID, mockEmitter1);
        sseConnectionManager.registerConnection(TEST_ITINERARY_ID, TEST_EXECUTION_ID, mockEmitter2);
        
        // Reset mocks to clear the connection confirmation calls
        reset(mockEmitter1, mockEmitter2);
        
        // Mock one emitter to throw IOException (dead connection)
        doThrow(new IOException("Connection closed")).when(mockEmitter1).send(any(SseEmitter.SseEventBuilder.class));
        
        ItineraryUpdateEvent event = ItineraryUpdateEvent.progressUpdate(
            TEST_ITINERARY_ID, TEST_EXECUTION_ID, 50, "Progress update", "PLANNER"
        );
        
        // When
        sseConnectionManager.broadcastUpdate(TEST_ITINERARY_ID, event);
        
        // Then - Both emitters should be called once for the broadcast
        verify(mockEmitter1, times(1)).send(any(SseEmitter.SseEventBuilder.class));
        verify(mockEmitter2, times(1)).send(any(SseEmitter.SseEventBuilder.class));
        
        // Dead connection should be cleaned up
        assertEquals(1, sseConnectionManager.getConnectionCount(TEST_ITINERARY_ID));
    }
    
    @Test
    @DisplayName("Should store events for missed event recovery")
    void shouldStoreEventsForMissedEventRecovery() throws IOException {
        // Given
        sseConnectionManager.registerConnection(TEST_ITINERARY_ID, TEST_EXECUTION_ID, mockEmitter1);
        
        // When - broadcast multiple events
        for (int i = 1; i <= 5; i++) {
            ItineraryUpdateEvent event = ItineraryUpdateEvent.progressUpdate(
                TEST_ITINERARY_ID, TEST_EXECUTION_ID, i * 20, "Progress " + i, "PLANNER"
            );
            sseConnectionManager.broadcastUpdate(TEST_ITINERARY_ID, event);
        }
        
        // Then - register new connection and verify missed events are sent
        SseEmitter newEmitter = mock(SseEmitter.class);
        sseConnectionManager.registerConnection(TEST_ITINERARY_ID, TEST_EXECUTION_ID, newEmitter);
        
        // Should send connection confirmation + missed events
        verify(newEmitter, atLeast(6)).send(any(SseEmitter.SseEventBuilder.class));
    }
    
    @Test
    @DisplayName("Should limit stored events to maximum")
    void shouldLimitStoredEventsToMaximum() throws IOException {
        // Given
        sseConnectionManager.registerConnection(TEST_ITINERARY_ID, TEST_EXECUTION_ID, mockEmitter1);
        
        // When - broadcast more than MAX_STORED_EVENTS (10) events
        for (int i = 1; i <= 15; i++) {
            ItineraryUpdateEvent event = ItineraryUpdateEvent.progressUpdate(
                TEST_ITINERARY_ID, TEST_EXECUTION_ID, i * 5, "Progress " + i, "PLANNER"
            );
            sseConnectionManager.broadcastUpdate(TEST_ITINERARY_ID, event);
        }
        
        // Then - register new connection
        SseEmitter newEmitter = mock(SseEmitter.class);
        sseConnectionManager.registerConnection(TEST_ITINERARY_ID, TEST_EXECUTION_ID, newEmitter);
        
        // Should send connection confirmation + max 10 missed events = 11 total
        verify(newEmitter, atMost(11)).send(any(SseEmitter.SseEventBuilder.class));
    }
    
    @Test
    @DisplayName("Should provide connection statistics")
    void shouldProvideConnectionStatistics() {
        // Given
        sseConnectionManager.registerConnection(TEST_ITINERARY_ID, TEST_EXECUTION_ID, mockEmitter1);
        sseConnectionManager.registerConnection("itinerary-2", "exec-2", mockEmitter2);
        
        // When
        Map<String, Object> stats = sseConnectionManager.getConnectionStats();
        
        // Then
        assertNotNull(stats);
        assertEquals(2, stats.get("totalItineraries"));
        assertEquals(2, stats.get("totalConnections"));
        assertTrue(stats.containsKey("connectionsPerItinerary"));
        assertTrue(stats.containsKey("connectionDurations"));
    }
    
    @Test
    @DisplayName("Should close all connections for itinerary")
    void shouldCloseAllConnectionsForItinerary() {
        // Given
        sseConnectionManager.registerConnection(TEST_ITINERARY_ID, TEST_EXECUTION_ID, mockEmitter1);
        sseConnectionManager.registerConnection(TEST_ITINERARY_ID, TEST_EXECUTION_ID, mockEmitter2);
        
        assertTrue(sseConnectionManager.hasActiveConnections(TEST_ITINERARY_ID));
        
        // When
        sseConnectionManager.closeConnectionsForItinerary(TEST_ITINERARY_ID);
        
        // Then
        assertFalse(sseConnectionManager.hasActiveConnections(TEST_ITINERARY_ID));
        assertEquals(0, sseConnectionManager.getConnectionCount(TEST_ITINERARY_ID));
        
        verify(mockEmitter1).complete();
        verify(mockEmitter2).complete();
    }
    
    @Test
    @DisplayName("Should handle no connections gracefully")
    void shouldHandleNoConnectionsGracefully() {
        // Given - no connections registered
        ItineraryUpdateEvent event = ItineraryUpdateEvent.progressUpdate(
            TEST_ITINERARY_ID, TEST_EXECUTION_ID, 50, "Progress update", "PLANNER"
        );
        
        // When - broadcast to non-existent connections
        assertDoesNotThrow(() -> {
            sseConnectionManager.broadcastUpdate(TEST_ITINERARY_ID, event);
        });
        
        // Then
        assertFalse(sseConnectionManager.hasActiveConnections(TEST_ITINERARY_ID));
        assertEquals(0, sseConnectionManager.getConnectionCount(TEST_ITINERARY_ID));
    }
    
    @Test
    @DisplayName("Should perform maintenance cleanup")
    void shouldPerformMaintenanceCleanup() {
        // Given
        sseConnectionManager.registerConnection(TEST_ITINERARY_ID, TEST_EXECUTION_ID, mockEmitter1);
        
        // Broadcast some events
        ItineraryUpdateEvent event = ItineraryUpdateEvent.progressUpdate(
            TEST_ITINERARY_ID, TEST_EXECUTION_ID, 50, "Progress update", "PLANNER"
        );
        sseConnectionManager.broadcastUpdate(TEST_ITINERARY_ID, event);
        
        // When
        assertDoesNotThrow(() -> {
            sseConnectionManager.performMaintenance();
        });
        
        // Then - should not throw any exceptions
        Map<String, Object> stats = sseConnectionManager.getConnectionStats();
        assertNotNull(stats);
    }
    
    @Test
    @DisplayName("Should send connection confirmation on registration")
    void shouldSendConnectionConfirmationOnRegistration() throws IOException {
        // When
        sseConnectionManager.registerConnection(TEST_ITINERARY_ID, TEST_EXECUTION_ID, mockEmitter1);
        
        // Then - should send connection confirmation event
        verify(mockEmitter1, atLeastOnce()).send(any(SseEmitter.SseEventBuilder.class));
    }
    
    @Test
    @DisplayName("Should filter missed events by execution ID")
    void shouldFilterMissedEventsByExecutionId() throws IOException {
        // Given
        String otherExecutionId = "other-exec-789";
        sseConnectionManager.registerConnection(TEST_ITINERARY_ID, TEST_EXECUTION_ID, mockEmitter1);
        
        // Broadcast events with different execution IDs
        ItineraryUpdateEvent event1 = ItineraryUpdateEvent.progressUpdate(
            TEST_ITINERARY_ID, TEST_EXECUTION_ID, 25, "Progress 1", "PLANNER"
        );
        ItineraryUpdateEvent event2 = ItineraryUpdateEvent.progressUpdate(
            TEST_ITINERARY_ID, otherExecutionId, 50, "Progress 2", "PLANNER"
        );
        
        sseConnectionManager.broadcastUpdate(TEST_ITINERARY_ID, event1);
        sseConnectionManager.broadcastUpdate(TEST_ITINERARY_ID, event2);
        
        // When - register new connection with specific execution ID
        SseEmitter newEmitter = mock(SseEmitter.class);
        sseConnectionManager.registerConnection(TEST_ITINERARY_ID, TEST_EXECUTION_ID, newEmitter);
        
        // Then - should only receive events for matching execution ID + connection confirmation
        verify(newEmitter, atLeast(2)).send(any(SseEmitter.SseEventBuilder.class)); // confirmation + 1 matching event
        verify(newEmitter, atMost(3)).send(any(SseEmitter.SseEventBuilder.class)); // should not get the other execution's event
    }
}