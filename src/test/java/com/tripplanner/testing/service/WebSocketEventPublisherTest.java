package com.tripplanner.testing.service;

import com.tripplanner.controller.WebSocketController;
import com.tripplanner.service.WebSocketEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for WebSocketEventPublisher.
 * Tests the service that bridges backend events to WebSocket broadcasting.
 */
public class WebSocketEventPublisherTest {
    
    private WebSocketEventPublisher webSocketEventPublisher;
    
    @Mock
    private WebSocketController webSocketController;
    
    private static final String TEST_ITINERARY_ID = "test-itinerary-123";
    private static final String TEST_AGENT_ID = "test-agent-456";
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        webSocketEventPublisher = new WebSocketEventPublisher();
        // Use reflection to inject the mock controller
        try {
            java.lang.reflect.Field field = WebSocketEventPublisher.class.getDeclaredField("webSocketController");
            field.setAccessible(true);
            field.set(webSocketEventPublisher, webSocketController);
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject mock WebSocketController", e);
        }
    }
    
    @Test
    @DisplayName("Should publish agent progress update to WebSocket")
    void shouldPublishAgentProgressUpdateToWebSocket() {
        // Given
        int progress = 75;
        String status = "processing";
        
        // When
        webSocketEventPublisher.publishAgentProgress(TEST_ITINERARY_ID, TEST_AGENT_ID, progress, status);
        
        // Then
        verify(webSocketController, times(1)).broadcastAgentProgress(
            eq(TEST_ITINERARY_ID), 
            eq(TEST_AGENT_ID), 
            eq(progress), 
            eq(status)
        );
    }
    
    @Test
    @DisplayName("Should publish itinerary update to WebSocket")
    void shouldPublishItineraryUpdateToWebSocket() {
        // Given
        String updateType = "generation_complete";
        Object data = java.util.Map.of(
            "status", "completed",
            "days", 3,
            "totalNodes", 15
        );
        
        // When
        webSocketEventPublisher.publishItineraryUpdate(TEST_ITINERARY_ID, updateType, data);
        
        // Then
        verify(webSocketController, times(1)).broadcastItineraryUpdate(
            eq(TEST_ITINERARY_ID), 
            eq(updateType), 
            eq(data)
        );
    }
    
    @Test
    @DisplayName("Should handle WebSocket controller exceptions gracefully")
    void shouldHandleWebSocketControllerExceptionsGracefully() {
        // Given
        doThrow(new RuntimeException("WebSocket error")).when(webSocketController)
            .broadcastAgentProgress(anyString(), anyString(), anyInt(), anyString());
        
        // When & Then - should not throw exception
        assertDoesNotThrow(() -> {
            webSocketEventPublisher.publishAgentProgress(TEST_ITINERARY_ID, TEST_AGENT_ID, 50, "error_test");
        });
        
        // Verify the method was still called
        verify(webSocketController, times(1)).broadcastAgentProgress(
            eq(TEST_ITINERARY_ID), 
            eq(TEST_AGENT_ID), 
            eq(50), 
            eq("error_test")
        );
    }
    
    @Test
    @DisplayName("Should handle null parameters gracefully")
    void shouldHandleNullParametersGracefully() {
        // When & Then - should not throw exception with null parameters
        assertDoesNotThrow(() -> {
            webSocketEventPublisher.publishAgentProgress(null, null, 0, null);
            webSocketEventPublisher.publishItineraryUpdate(null, null, null);
        });
        
        // Verify methods were called with null parameters
        verify(webSocketController, times(1)).broadcastAgentProgress(null, null, 0, null);
        verify(webSocketController, times(1)).broadcastItineraryUpdate(null, null, null);
    }
    
    @Test
    @DisplayName("Should publish multiple events in sequence")
    void shouldPublishMultipleEventsInSequence() {
        // Given
        String[] agentIds = {"agent1", "agent2", "agent3"};
        int[] progressValues = {25, 50, 75};
        String[] statuses = {"started", "processing", "completed"};
        
        // When
        for (int i = 0; i < agentIds.length; i++) {
            webSocketEventPublisher.publishAgentProgress(
                TEST_ITINERARY_ID, 
                agentIds[i], 
                progressValues[i], 
                statuses[i]
            );
        }
        
        // Then
        verify(webSocketController, times(3)).broadcastAgentProgress(anyString(), anyString(), anyInt(), anyString());
        
        // Verify specific calls
        verify(webSocketController).broadcastAgentProgress(TEST_ITINERARY_ID, "agent1", 25, "started");
        verify(webSocketController).broadcastAgentProgress(TEST_ITINERARY_ID, "agent2", 50, "processing");
        verify(webSocketController).broadcastAgentProgress(TEST_ITINERARY_ID, "agent3", 75, "completed");
    }
    
    @Test
    @DisplayName("Should publish different types of itinerary updates")
    void shouldPublishDifferentTypesOfItineraryUpdates() {
        // Given
        String[] updateTypes = {
            "generation_complete",
            "day_completed", 
            "node_enhanced",
            "error_occurred"
        };
        
        // When
        for (String updateType : updateTypes) {
            Object data = java.util.Map.of("type", updateType, "timestamp", System.currentTimeMillis());
            webSocketEventPublisher.publishItineraryUpdate(TEST_ITINERARY_ID, updateType, data);
        }
        
        // Then
        verify(webSocketController, times(4)).broadcastItineraryUpdate(anyString(), anyString(), any());
        
        // Verify each update type was called
        for (String updateType : updateTypes) {
            verify(webSocketController).broadcastItineraryUpdate(eq(TEST_ITINERARY_ID), eq(updateType), any());
        }
    }
}


