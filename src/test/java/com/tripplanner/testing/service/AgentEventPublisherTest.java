package com.tripplanner.testing.service;

import com.tripplanner.dto.*;
import com.tripplanner.service.AgentEventPublisher;
import com.tripplanner.service.SseConnectionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AgentEventPublisher.
 */
public class AgentEventPublisherTest {
    
    private AgentEventPublisher agentEventPublisher;
    
    @Mock
    private SseConnectionManager sseConnectionManager;
    
    private static final String TEST_ITINERARY_ID = "test-itinerary-123";
    private static final String TEST_EXECUTION_ID = "exec-456";
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        agentEventPublisher = new AgentEventPublisher(sseConnectionManager);
    }
    
    @Test
    @DisplayName("Should publish day completed event with correct data")
    void shouldPublishDayCompletedEventWithCorrectData() {
        // Given
        NormalizedDay completedDay = createTestDay(1, 3);
        
        // When
        agentEventPublisher.publishDayCompleted(TEST_ITINERARY_ID, TEST_EXECUTION_ID, completedDay);
        
        // Then
        ArgumentCaptor<ItineraryUpdateEvent> eventCaptor = ArgumentCaptor.forClass(ItineraryUpdateEvent.class);
        verify(sseConnectionManager).broadcastUpdate(eq(TEST_ITINERARY_ID), eventCaptor.capture());
        
        ItineraryUpdateEvent event = eventCaptor.getValue();
        assertEquals("day_completed", event.getEventType());
        assertEquals(TEST_ITINERARY_ID, event.getItineraryId());
        assertEquals(TEST_EXECUTION_ID, event.getExecutionId());
        assertEquals(Integer.valueOf(1), event.getDayNumber());
        assertEquals(completedDay, event.getData());
        assertEquals("PLANNER", event.getAgentType());
        assertTrue(event.getMessage().contains("Day 1 planning completed"));
        assertTrue(event.getMessage().contains("3 activities"));
    }
    
    @Test
    @DisplayName("Should handle null day data gracefully")
    void shouldHandleNullDayDataGracefully() {
        // When
        agentEventPublisher.publishDayCompleted(TEST_ITINERARY_ID, TEST_EXECUTION_ID, null);
        
        // Then
        verify(sseConnectionManager, never()).broadcastUpdate(any(), any());
    }
    
    @Test
    @DisplayName("Should publish node enhanced event with correct data")
    void shouldPublishNodeEnhancedEventWithCorrectData() {
        // Given
        String nodeId = "node-123";
        NormalizedNode enhancedNode = createTestNode("Tokyo Tower");
        String enhancementType = "photos";
        
        // When
        agentEventPublisher.publishNodeEnhanced(TEST_ITINERARY_ID, TEST_EXECUTION_ID, 
                                               nodeId, enhancedNode, enhancementType);
        
        // Then
        ArgumentCaptor<ItineraryUpdateEvent> eventCaptor = ArgumentCaptor.forClass(ItineraryUpdateEvent.class);
        verify(sseConnectionManager).broadcastUpdate(eq(TEST_ITINERARY_ID), eventCaptor.capture());
        
        ItineraryUpdateEvent event = eventCaptor.getValue();
        assertEquals("node_enhanced", event.getEventType());
        assertEquals(TEST_ITINERARY_ID, event.getItineraryId());
        assertEquals(TEST_EXECUTION_ID, event.getExecutionId());
        assertEquals(nodeId, event.getNodeId());
        assertEquals(enhancedNode, event.getData());
        assertEquals("ENRICHMENT", event.getAgentType());
        assertTrue(event.getMessage().contains("Added photos to Tokyo Tower"));
    }
    
    @Test
    @DisplayName("Should determine correct agent type based on enhancement type")
    void shouldDetermineCorrectAgentTypeBasedOnEnhancementType() {
        // Given
        String nodeId = "node-123";
        NormalizedNode node = createTestNode("Test Location");
        
        // Test different enhancement types
        String[][] testCases = {
            {"photos", "ENRICHMENT"},
            {"details", "ENRICHMENT"},
            {"rating", "ENRICHMENT"},
            {"location", "places"},
            {"booking", "booking"},
            {"unknown", "ENRICHMENT"}
        };
        
        for (String[] testCase : testCases) {
            String enhancementType = testCase[0];
            String expectedAgentType = testCase[1];
            
            // When
            agentEventPublisher.publishNodeEnhanced(TEST_ITINERARY_ID, TEST_EXECUTION_ID, 
                                                   nodeId, node, enhancementType);
            
            // Then
            ArgumentCaptor<ItineraryUpdateEvent> eventCaptor = ArgumentCaptor.forClass(ItineraryUpdateEvent.class);
            verify(sseConnectionManager, atLeastOnce()).broadcastUpdate(eq(TEST_ITINERARY_ID), eventCaptor.capture());
            
            ItineraryUpdateEvent event = eventCaptor.getValue();
            assertEquals(expectedAgentType, event.getAgentType(), 
                        "Enhancement type '" + enhancementType + "' should map to agent type '" + expectedAgentType + "'");
        }
    }
    
    @Test
    @DisplayName("Should publish progress update event")
    void shouldPublishProgressUpdateEvent() {
        // Given
        int progress = 75;
        String message = "Processing day 3 of 4";
        String agentType = "PLANNER";
        
        // When
        agentEventPublisher.publishProgress(TEST_ITINERARY_ID, TEST_EXECUTION_ID, progress, message, agentType);
        
        // Then
        ArgumentCaptor<ItineraryUpdateEvent> eventCaptor = ArgumentCaptor.forClass(ItineraryUpdateEvent.class);
        verify(sseConnectionManager).broadcastUpdate(eq(TEST_ITINERARY_ID), eventCaptor.capture());
        
        ItineraryUpdateEvent event = eventCaptor.getValue();
        assertEquals("progress_update", event.getEventType());
        assertEquals(TEST_ITINERARY_ID, event.getItineraryId());
        assertEquals(TEST_EXECUTION_ID, event.getExecutionId());
        assertEquals(Integer.valueOf(progress), event.getProgress());
        assertEquals(message, event.getMessage());
        assertEquals(agentType, event.getAgentType());
    }
    
    @Test
    @DisplayName("Should publish generation complete event")
    void shouldPublishGenerationCompleteEvent() {
        // Given
        NormalizedItinerary finalItinerary = createTestItinerary(3, 2); // 3 days, 2 nodes per day
        
        // When
        agentEventPublisher.publishGenerationComplete(TEST_ITINERARY_ID, TEST_EXECUTION_ID, finalItinerary);
        
        // Then
        ArgumentCaptor<ItineraryUpdateEvent> eventCaptor = ArgumentCaptor.forClass(ItineraryUpdateEvent.class);
        verify(sseConnectionManager).broadcastUpdate(eq(TEST_ITINERARY_ID), eventCaptor.capture());
        
        ItineraryUpdateEvent event = eventCaptor.getValue();
        assertEquals("generation_complete", event.getEventType());
        assertEquals(TEST_ITINERARY_ID, event.getItineraryId());
        assertEquals(TEST_EXECUTION_ID, event.getExecutionId());
        assertEquals(finalItinerary, event.getData());
        assertEquals(Integer.valueOf(100), event.getProgress());
        assertEquals("orchestrator", event.getAgentType());
        assertTrue(event.getMessage().contains("3 days planned"));
        assertTrue(event.getMessage().contains("6 activities total"));
    }
    
    @Test
    @DisplayName("Should publish error event with correct severity")
    void shouldPublishErrorEventWithCorrectSeverity() {
        // Given
        String errorCode = "NETWORK_ERROR";
        String message = "Failed to connect to external service";
        ErrorEvent.ErrorSeverity severity = ErrorEvent.ErrorSeverity.ERROR;
        boolean canRetry = true;
        
        // When
        agentEventPublisher.publishError(TEST_ITINERARY_ID, TEST_EXECUTION_ID, errorCode, message, severity, canRetry);
        
        // Then
        ArgumentCaptor<ErrorEvent> eventCaptor = ArgumentCaptor.forClass(ErrorEvent.class);
        verify(sseConnectionManager).broadcastUpdate(eq(TEST_ITINERARY_ID), eventCaptor.capture());
        
        ErrorEvent event = eventCaptor.getValue();
        assertEquals("error", event.getEventType());
        assertEquals(TEST_ITINERARY_ID, event.getItineraryId());
        assertEquals(TEST_EXECUTION_ID, event.getExecutionId());
        assertEquals(errorCode, event.getErrorCode());
        assertEquals(message, event.getMessage());
        assertEquals(severity, event.getSeverity());
        assertEquals(canRetry, event.getCanRetry());
    }
    
    @Test
    @DisplayName("Should publish warning event")
    void shouldPublishWarningEvent() {
        // Given
        String errorCode = "RATE_LIMIT";
        String message = "API rate limit approaching";
        String recoveryAction = "Reducing request frequency";
        
        // When
        agentEventPublisher.publishWarning(TEST_ITINERARY_ID, TEST_EXECUTION_ID, errorCode, message, recoveryAction);
        
        // Then
        ArgumentCaptor<ErrorEvent> eventCaptor = ArgumentCaptor.forClass(ErrorEvent.class);
        verify(sseConnectionManager).broadcastUpdate(eq(TEST_ITINERARY_ID), eventCaptor.capture());
        
        ErrorEvent event = eventCaptor.getValue();
        assertEquals("error", event.getEventType());
        assertEquals(ErrorEvent.ErrorSeverity.WARNING, event.getSeverity());
        assertEquals(recoveryAction, event.getRecoveryAction());
        assertEquals(Boolean.TRUE, event.getCanRetry());
    }
    
    @Test
    @DisplayName("Should publish critical error event")
    void shouldPublishCriticalErrorEvent() {
        // Given
        String errorCode = "SYSTEM_FAILURE";
        String message = "Critical system component failed";
        String recoveryAction = "Manual intervention required";
        
        // When
        agentEventPublisher.publishCriticalError(TEST_ITINERARY_ID, TEST_EXECUTION_ID, errorCode, message, recoveryAction);
        
        // Then
        ArgumentCaptor<ErrorEvent> eventCaptor = ArgumentCaptor.forClass(ErrorEvent.class);
        verify(sseConnectionManager).broadcastUpdate(eq(TEST_ITINERARY_ID), eventCaptor.capture());
        
        ErrorEvent event = eventCaptor.getValue();
        assertEquals("error", event.getEventType());
        assertEquals(ErrorEvent.ErrorSeverity.CRITICAL, event.getSeverity());
        assertEquals(recoveryAction, event.getRecoveryAction());
        assertEquals(Boolean.FALSE, event.getCanRetry());
    }
    
    @Test
    @DisplayName("Should publish error from exception with correct retry determination")
    void shouldPublishErrorFromExceptionWithCorrectRetryDetermination() {
        // Test retryable exception
        SocketTimeoutException retryableException = new SocketTimeoutException("Connection timeout");
        
        // When
        agentEventPublisher.publishErrorFromException(TEST_ITINERARY_ID, TEST_EXECUTION_ID, 
                                                     retryableException, "API call", ErrorEvent.ErrorSeverity.ERROR);
        
        // Then
        ArgumentCaptor<ErrorEvent> eventCaptor = ArgumentCaptor.forClass(ErrorEvent.class);
        verify(sseConnectionManager).broadcastUpdate(eq(TEST_ITINERARY_ID), eventCaptor.capture());
        
        ErrorEvent event = eventCaptor.getValue();
        assertEquals(Boolean.TRUE, event.getCanRetry());
        assertTrue(event.getMessage().contains("API call"));
        assertTrue(event.getMessage().contains("Connection timeout"));
        assertEquals("Retrying operation automatically", event.getRecoveryAction());
        
        // Test non-retryable exception
        IllegalArgumentException nonRetryableException = new IllegalArgumentException("Invalid parameter");
        
        // When
        agentEventPublisher.publishErrorFromException(TEST_ITINERARY_ID, TEST_EXECUTION_ID, 
                                                     nonRetryableException, "validation", ErrorEvent.ErrorSeverity.ERROR);
        
        // Then
        verify(sseConnectionManager, times(2)).broadcastUpdate(eq(TEST_ITINERARY_ID), eventCaptor.capture());
        
        ErrorEvent secondEvent = eventCaptor.getValue();
        assertEquals(Boolean.FALSE, secondEvent.getCanRetry());
        assertEquals("Manual intervention may be required", secondEvent.getRecoveryAction());
    }
    
    @Test
    @DisplayName("Should publish agent started event")
    void shouldPublishAgentStartedEvent() {
        // Given
        String agentType = "PLANNER";
        String stageName = "Day Planning";
        String description = "Creating detailed day-by-day itinerary";
        
        // When
        agentEventPublisher.publishAgentStarted(TEST_ITINERARY_ID, TEST_EXECUTION_ID, agentType, stageName, description);
        
        // Then
        ArgumentCaptor<ItineraryUpdateEvent> eventCaptor = ArgumentCaptor.forClass(ItineraryUpdateEvent.class);
        verify(sseConnectionManager).broadcastUpdate(eq(TEST_ITINERARY_ID), eventCaptor.capture());
        
        ItineraryUpdateEvent event = eventCaptor.getValue();
        assertEquals("agent_started", event.getEventType());
        assertEquals(agentType, event.getAgentType());
        assertTrue(event.getMessage().contains("Started Day Planning"));
        assertTrue(event.getMessage().contains(description));
    }
    
    @Test
    @DisplayName("Should publish batch progress event")
    void shouldPublishBatchProgressEvent() {
        // Given
        int currentDay = 2;
        int totalDays = 5;
        String currentActivity = "Planning attractions";
        
        // When
        agentEventPublisher.publishBatchProgress(TEST_ITINERARY_ID, TEST_EXECUTION_ID, currentDay, totalDays, currentActivity);
        
        // Then
        ArgumentCaptor<ItineraryUpdateEvent> eventCaptor = ArgumentCaptor.forClass(ItineraryUpdateEvent.class);
        verify(sseConnectionManager).broadcastUpdate(eq(TEST_ITINERARY_ID), eventCaptor.capture());
        
        ItineraryUpdateEvent event = eventCaptor.getValue();
        assertEquals("progress_update", event.getEventType());
        assertEquals("PLANNER", event.getAgentType());
        assertEquals(Integer.valueOf(40), event.getProgress()); // 2/5 * 100 = 40%
        assertTrue(event.getMessage().contains("Planning day 2 of 5"));
        assertTrue(event.getMessage().contains(currentActivity));
    }
    
    @Test
    @DisplayName("Should check active connections")
    void shouldCheckActiveConnections() {
        // Given
        when(sseConnectionManager.hasActiveConnections(TEST_ITINERARY_ID)).thenReturn(true);
        when(sseConnectionManager.getConnectionCount(TEST_ITINERARY_ID)).thenReturn(3);
        
        // When & Then
        assertTrue(agentEventPublisher.hasActiveConnections(TEST_ITINERARY_ID));
        assertEquals(3, agentEventPublisher.getConnectionCount(TEST_ITINERARY_ID));
        
        verify(sseConnectionManager).hasActiveConnections(TEST_ITINERARY_ID);
        verify(sseConnectionManager).getConnectionCount(TEST_ITINERARY_ID);
    }
    
    @Test
    @DisplayName("Should handle broadcasting errors gracefully")
    void shouldHandleBroadcastingErrorsGracefully() {
        // Given
        NormalizedDay completedDay = createTestDay(1, 2);
        doThrow(new RuntimeException("Broadcasting failed")).when(sseConnectionManager).broadcastUpdate(any(), any());
        
        // When & Then - should not throw exception
        assertDoesNotThrow(() -> {
            agentEventPublisher.publishDayCompleted(TEST_ITINERARY_ID, TEST_EXECUTION_ID, completedDay);
        });
        
        verify(sseConnectionManager).broadcastUpdate(any(), any());
    }
    
    // Helper methods
    
    private NormalizedDay createTestDay(int dayNumber, int nodeCount) {
        NormalizedDay day = new NormalizedDay();
        day.setDayNumber(dayNumber);
        day.setDate("2024-06-0" + dayNumber);
        day.setLocation("Tokyo");
        
        List<NormalizedNode> nodes = new java.util.ArrayList<>();
        for (int i = 0; i < nodeCount; i++) {
            nodes.add(createTestNode("Activity " + (i + 1)));
        }
        day.setNodes(nodes);
        
        return day;
    }
    
    private NormalizedNode createTestNode(String title) {
        NormalizedNode node = new NormalizedNode();
        node.setId("node-" + System.currentTimeMillis());
        node.setTitle(title);
        node.setType("attraction");
        return node;
    }
    
    private NormalizedItinerary createTestItinerary(int dayCount, int nodesPerDay) {
        NormalizedItinerary itinerary = new NormalizedItinerary();
        itinerary.setItineraryId(TEST_ITINERARY_ID);
        itinerary.setDestination("Tokyo");
        
        List<NormalizedDay> days = new java.util.ArrayList<>();
        for (int i = 1; i <= dayCount; i++) {
            days.add(createTestDay(i, nodesPerDay));
        }
        itinerary.setDays(days);
        
        return itinerary;
    }
}