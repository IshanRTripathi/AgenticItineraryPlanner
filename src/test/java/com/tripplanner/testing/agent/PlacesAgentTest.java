package com.tripplanner.testing.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripplanner.agents.PlacesAgent;
import com.tripplanner.dto.AgentCapabilities;
import com.tripplanner.dto.AgentEvent;
import com.tripplanner.service.AgentEventBus;
import com.tripplanner.service.ai.AiClient;
import com.tripplanner.testing.BaseServiceTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * Atomic tests for PlacesAgent with mocked dependencies.
 */
class PlacesAgentTest extends BaseServiceTest {
    
    @Mock
    private AgentEventBus mockEventBus;
    
    @Mock
    private AiClient mockAiClient;
    
    @Mock
    private ObjectMapper mockObjectMapper;
    
    private PlacesAgent placesAgent;
    
    @BeforeEach
    protected void setUp() {
        super.setUp();
        placesAgent = new PlacesAgent(mockEventBus, mockAiClient, mockObjectMapper);
    }
    
    @Override
    protected void setupSpecificMocks() {
        // Setup event bus mock with lenient stubbing
        lenient().doNothing().when(mockEventBus).publish(anyString(), any(AgentEvent.class));
        
        // Setup AI client mock with lenient stubbing
        lenient().when(mockAiClient.generateStructuredContent(anyString(), anyString(), anyString()))
                .thenReturn("{}");
    }
    
    @Test
    @DisplayName("Should get places agent capabilities")
    void shouldGetPlacesAgentCapabilities() {
        // When
        AgentCapabilities capabilities = placesAgent.getCapabilities();
        
        // Then
        assertThat(capabilities).isNotNull();
        assertThat(capabilities.getSupportedTasks()).contains("search"); // Single task type
        assertThat(capabilities.getSupportedTasks()).hasSize(1); // Zero-overlap design
        assertThat(capabilities.getPriority()).isEqualTo(40); // Lower priority (helper service)
        assertThat(capabilities.isChatEnabled()).isFalse(); // Not for direct chat, used by other agents
        assertThat(capabilities.getConfigurationValue("helperService")).isEqualTo(true);
        assertThat(capabilities.getConfigurationValue("requiresLLM")).isEqualTo(true);
        assertThat(capabilities.getConfigurationValue("providesInsights")).isEqualTo(true);
        assertThat(capabilities.getConfigurationValue("scopeType")).isEqualTo("location_discovery");
        
        logger.info("Get places agent capabilities test passed");
    }
    
    @Test
    @DisplayName("Should handle places task types")
    void shouldHandlePlacesTaskTypes() {
        // When/Then
        assertThat(placesAgent.canHandle("search")).isTrue(); // Only handles "search"
        assertThat(placesAgent.canHandle("places")).isFalse(); // No longer supported (zero-overlap)
        assertThat(placesAgent.canHandle("discover")).isFalse(); // No longer supported (zero-overlap)
        assertThat(placesAgent.canHandle("analyze")).isFalse(); // No longer supported (zero-overlap)
        assertThat(placesAgent.canHandle("explore")).isFalse(); // No longer supported (zero-overlap)
        assertThat(placesAgent.canHandle("booking")).isFalse();
        assertThat(placesAgent.canHandle("payment")).isFalse();
        
        logger.info("Handle places task types test passed");
    }
    
    @Test
    @DisplayName("Should handle places task with context")
    void shouldHandlePlacesTaskWithContext() {
        // Given
        String taskType = "search"; // Correct task type
        Object context = new Object();
        
        // When
        boolean canHandle = placesAgent.canHandle(taskType, context);
        
        // Then
        assertThat(canHandle).isTrue();
        
        logger.info("Handle places task with context test passed");
    }
    
    @Test
    @DisplayName("Should not handle unsupported task types")
    void shouldNotHandleUnsupportedTaskTypes() {
        // When/Then
        assertThat(placesAgent.canHandle("unsupported")).isFalse();
        assertThat(placesAgent.canHandle("random")).isFalse();
        assertThat(placesAgent.canHandle("")).isFalse();
        assertThat(placesAgent.canHandle(null)).isFalse();
        
        logger.info("Not handle unsupported task types test passed");
    }
}