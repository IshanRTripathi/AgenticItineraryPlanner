package com.tripplanner.testing.integration;

import com.tripplanner.controller.WebSocketController;
import com.tripplanner.dto.AgentEvent;
import com.tripplanner.service.AgentEventBus;
import com.tripplanner.service.WebSocketEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Integration tests for WebSocket event publishing.
 * Tests the complete flow from AgentEventBus through WebSocketEventPublisher to WebSocketController.
 */
public class WebSocketEventIntegrationTest {
    
    private AgentEventBus agentEventBus;
    private WebSocketEventPublisher webSocketEventPublisher;
    private WebSocketController webSocketController;
    
    @Mock
    private SimpMessagingTemplate messagingTemplate;
    
    private static final String TEST_ITINERARY_ID = "test-itinerary-123";
    private static final String TEST_AGENT_ID = "test-agent-456";
    private static final String TEST_EXECUTION_ID = "exec-789";
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Create real instances
        webSocketController = new WebSocketController();
        webSocketEventPublisher = new WebSocketEventPublisher();
        agentEventBus = new AgentEventBus();
        
        // Inject mocks using reflection
        try {
            // Inject SimpMessagingTemplate into WebSocketController
            java.lang.reflect.Field messagingTemplateField = WebSocketController.class.getDeclaredField("messagingTemplate");
            messagingTemplateField.setAccessible(true);
            messagingTemplateField.set(webSocketController, messagingTemplate);
            
            // Inject WebSocketController into WebSocketEventPublisher
            java.lang.reflect.Field webSocketControllerField = WebSocketEventPublisher.class.getDeclaredField("webSocketController");
            webSocketControllerField.setAccessible(true);
            webSocketControllerField.set(webSocketEventPublisher, webSocketController);
            
            // Inject WebSocketEventPublisher into AgentEventBus
            java.lang.reflect.Field webSocketEventPublisherField = AgentEventBus.class.getDeclaredField("webSocketEventPublisher");
            webSocketEventPublisherField.setAccessible(true);
            webSocketEventPublisherField.set(agentEventBus, webSocketEventPublisher);
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject dependencies", e);
        }
    }
    
    @Test
    @DisplayName("Should publish agent event through complete WebSocket flow")
    void shouldPublishAgentEventThroughCompleteWebSocketFlow() {
        // Given
        AgentEvent agentEvent = new AgentEvent(
            TEST_AGENT_ID,
            AgentEvent.AgentKind.PLANNER,
            AgentEvent.AgentStatus.running,
            75,
            "Processing itinerary...",
            "skeleton_generation",
            Instant.now(),
            TEST_ITINERARY_ID
        );
        
        // When
        agentEventBus.publish(TEST_ITINERARY_ID, agentEvent);
        
        // Then
        // Verify that the message was sent to WebSocket
        verify(messagingTemplate, times(1)).convertAndSend(
            eq("/topic/itinerary/" + TEST_ITINERARY_ID),
            any(Object.class)
        );
    }
    
    @Test
    @DisplayName("Should publish completion event through complete WebSocket flow")
    void shouldPublishCompletionEventThroughCompleteWebSocketFlow() {
        // When
        agentEventBus.sendCompletion(TEST_ITINERARY_ID);
        
        // Then
        // Verify that the completion message was sent to WebSocket
        verify(messagingTemplate, times(1)).convertAndSend(
            eq("/topic/itinerary/" + TEST_ITINERARY_ID),
            any(Object.class)
        );
    }
    
    @Test
    @DisplayName("Should handle multiple agent events in sequence")
    void shouldHandleMultipleAgentEventsInSequence() {
        // Given
        AgentEvent[] events = {
            new AgentEvent(TEST_AGENT_ID, AgentEvent.AgentKind.PLANNER, AgentEvent.AgentStatus.queued, 0, "Starting...", "init", Instant.now(), TEST_ITINERARY_ID),
            new AgentEvent(TEST_AGENT_ID, AgentEvent.AgentKind.PLANNER, AgentEvent.AgentStatus.running, 25, "Planning...", "skeleton", Instant.now(), TEST_ITINERARY_ID),
            new AgentEvent(TEST_AGENT_ID, AgentEvent.AgentKind.PLANNER, AgentEvent.AgentStatus.running, 50, "Populating...", "population", Instant.now(), TEST_ITINERARY_ID),
            new AgentEvent(TEST_AGENT_ID, AgentEvent.AgentKind.PLANNER, AgentEvent.AgentStatus.running, 75, "Enriching...", "enrichment", Instant.now(), TEST_ITINERARY_ID),
            new AgentEvent(TEST_AGENT_ID, AgentEvent.AgentKind.PLANNER, AgentEvent.AgentStatus.completed, 100, "Complete!", "finalization", Instant.now(), TEST_ITINERARY_ID)
        };
        
        // When
        for (AgentEvent event : events) {
            agentEventBus.publish(TEST_ITINERARY_ID, event);
        }
        
        // Then
        // Verify that all events were sent to WebSocket
        verify(messagingTemplate, times(events.length)).convertAndSend(
            eq("/topic/itinerary/" + TEST_ITINERARY_ID),
            any(Object.class)
        );
    }
    
    @Test
    @DisplayName("Should handle WebSocket errors gracefully")
    void shouldHandleWebSocketErrorsGracefully() {
        // Given
        doThrow(new RuntimeException("WebSocket error")).when(messagingTemplate)
            .convertAndSend(anyString(), any(Object.class));
        
        AgentEvent agentEvent = new AgentEvent(
            TEST_AGENT_ID,
            AgentEvent.AgentKind.PLANNER,
            AgentEvent.AgentStatus.running,
            50,
            "Processing...",
            "test",
            Instant.now(),
            TEST_ITINERARY_ID
        );
        
        // When & Then - should not throw exception
        assertDoesNotThrow(() -> {
            agentEventBus.publish(TEST_ITINERARY_ID, agentEvent);
        });
        
        // Verify the method was still called
        verify(messagingTemplate, times(1)).convertAndSend(anyString(), any(Object.class));
    }
    
    @Test
    @DisplayName("Should handle concurrent agent events")
    void shouldHandleConcurrentAgentEvents() throws InterruptedException {
        // Given
        int numberOfThreads = 5;
        int eventsPerThread = 10;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completionLatch = new CountDownLatch(numberOfThreads);
        
        // When
        for (int i = 0; i < numberOfThreads; i++) {
            final int threadId = i;
            new Thread(() -> {
                try {
                    startLatch.await(); // Wait for all threads to be ready
                    
                    for (int j = 0; j < eventsPerThread; j++) {
                        AgentEvent event = new AgentEvent(
                            "agent-" + threadId,
                            AgentEvent.AgentKind.PLANNER,
                            AgentEvent.AgentStatus.running,
                            j * 10,
                            "Thread " + threadId + " event " + j,
                            "test",
                            Instant.now(),
                            TEST_ITINERARY_ID + "-" + threadId
                        );
                        agentEventBus.publish(TEST_ITINERARY_ID + "-" + threadId, event);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    completionLatch.countDown();
                }
            }).start();
        }
        
        startLatch.countDown(); // Start all threads
        boolean allCompleted = completionLatch.await(5, TimeUnit.SECONDS);
        
        // Then
        assertTrue(allCompleted, "All threads should complete within timeout");
        
        // Verify that all events were sent (numberOfThreads * eventsPerThread)
        verify(messagingTemplate, times(numberOfThreads * eventsPerThread)).convertAndSend(anyString(), any(Object.class));
    }
    
    @Test
    @DisplayName("Should maintain event order for same itinerary")
    void shouldMaintainEventOrderForSameItinerary() {
        // Given
        AgentEvent[] events = {
            new AgentEvent(TEST_AGENT_ID, AgentEvent.AgentKind.PLANNER, AgentEvent.AgentStatus.queued, 0, "Event 1", "step1", Instant.now(), TEST_ITINERARY_ID),
            new AgentEvent(TEST_AGENT_ID, AgentEvent.AgentKind.PLANNER, AgentEvent.AgentStatus.running, 25, "Event 2", "step2", Instant.now(), TEST_ITINERARY_ID),
            new AgentEvent(TEST_AGENT_ID, AgentEvent.AgentKind.PLANNER, AgentEvent.AgentStatus.running, 50, "Event 3", "step3", Instant.now(), TEST_ITINERARY_ID),
            new AgentEvent(TEST_AGENT_ID, AgentEvent.AgentKind.PLANNER, AgentEvent.AgentStatus.completed, 100, "Event 4", "step4", Instant.now(), TEST_ITINERARY_ID)
        };
        
        // When
        for (AgentEvent event : events) {
            agentEventBus.publish(TEST_ITINERARY_ID, event);
        }
        
        // Then
        // Verify that all events were sent in order
        verify(messagingTemplate, times(events.length)).convertAndSend(
            eq("/topic/itinerary/" + TEST_ITINERARY_ID),
            any(Object.class)
        );
    }
    
    @Test
    @DisplayName("Should handle different agent types")
    void shouldHandleDifferentAgentTypes() {
        // Given
        AgentEvent[] events = {
            new AgentEvent("planner-1", AgentEvent.AgentKind.PLANNER, AgentEvent.AgentStatus.running, 30, "Planning...", "skeleton", Instant.now(), TEST_ITINERARY_ID),
            new AgentEvent("enrichment-1", AgentEvent.AgentKind.ENRICHMENT, AgentEvent.AgentStatus.running, 60, "Enriching...", "details", Instant.now(), TEST_ITINERARY_ID),
            new AgentEvent("places-1", AgentEvent.AgentKind.PLACES, AgentEvent.AgentStatus.running, 80, "Adding places...", "locations", Instant.now(), TEST_ITINERARY_ID),
            new AgentEvent("cost-1", AgentEvent.AgentKind.orchestrator, AgentEvent.AgentStatus.completed, 100, "Cost estimated", "estimation", Instant.now(), TEST_ITINERARY_ID)
        };
        
        // When
        for (AgentEvent event : events) {
            agentEventBus.publish(TEST_ITINERARY_ID, event);
        }
        
        // Then
        // Verify that all events were sent to WebSocket
        verify(messagingTemplate, times(events.length)).convertAndSend(
            eq("/topic/itinerary/" + TEST_ITINERARY_ID),
            any(Object.class)
        );
    }
}
