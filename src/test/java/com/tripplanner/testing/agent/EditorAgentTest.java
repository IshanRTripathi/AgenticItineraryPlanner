package com.tripplanner.testing.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripplanner.agents.EditorAgent;
import com.tripplanner.dto.AgentCapabilities;
import com.tripplanner.dto.AgentEvent;
import com.tripplanner.service.*;
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
 * Atomic tests for EditorAgent with mocked dependencies.
 */
class EditorAgentTest extends BaseServiceTest {
    
    @Mock
    private AgentEventBus mockEventBus;
    
    @Mock
    private SummarizationService mockSummarizationService;
    
    @Mock
    private ChangeEngine mockChangeEngine;
    
    @Mock
    private GeminiClient mockGeminiClient;
    
    @Mock
    private ItineraryJsonService mockItineraryJsonService;
    
    @Mock
    private ObjectMapper mockObjectMapper;
    
    @Mock
    private LLMResponseHandler mockLLMResponseHandler;
    
    private EditorAgent editorAgent;
    
    @BeforeEach
    protected void setUp() {
        super.setUp();
        editorAgent = new EditorAgent(
            mockEventBus,
            mockSummarizationService,
            mockChangeEngine,
            mockGeminiClient,
            mockItineraryJsonService,
            mockObjectMapper,
            mockLLMResponseHandler
        );
    }
    
    @Override
    protected void setupSpecificMocks() {
        // Setup event bus mock with lenient stubbing
        lenient().doNothing().when(mockEventBus).publish(anyString(), any(AgentEvent.class));
        
        // Setup other service mocks with lenient stubbing
        lenient().when(mockGeminiClient.generateContent(anyString())).thenReturn("{}");
        lenient().when(mockSummarizationService.summarizeItinerary(any(), anyInt())).thenReturn("Summary");
    }
    
    @Test
    @DisplayName("Should get editor agent capabilities")
    void shouldGetEditorAgentCapabilities() {
        // When
        AgentCapabilities capabilities = editorAgent.getCapabilities();
        
        // Then
        assertThat(capabilities).isNotNull();
        assertThat(capabilities.getSupportedTasks()).contains("edit"); // Single task type
        assertThat(capabilities.getSupportedTasks()).hasSize(1); // Zero-overlap design
        assertThat(capabilities.getPriority()).isEqualTo(10); // High priority for user modifications
        assertThat(capabilities.isChatEnabled()).isTrue(); // Chat-enabled
        assertThat(capabilities.getConfigurationValue("requiresLLM")).isEqualTo(true);
        assertThat(capabilities.getConfigurationValue("handlesUserRequests")).isEqualTo(true);
        assertThat(capabilities.getConfigurationValue("scopeType")).isEqualTo("user_modifications");
        
        logger.info("Get editor agent capabilities test passed");
    }
    
    @Test
    @DisplayName("Should handle editor task types")
    void shouldHandleEditorTaskTypes() {
        // When/Then
        assertThat(editorAgent.canHandle("edit")).isTrue(); // Only handles "edit"
        assertThat(editorAgent.canHandle("modify")).isFalse(); // No longer supported (zero-overlap)
        assertThat(editorAgent.canHandle("booking")).isFalse();
        assertThat(editorAgent.canHandle("payment")).isFalse();
        
        logger.info("Handle editor task types test passed");
    }
    
    @Test
    @DisplayName("Should handle editor task with context")
    void shouldHandleEditorTaskWithContext() {
        // Given
        String taskType = "edit";
        Object context = new Object();
        
        // When
        boolean canHandle = editorAgent.canHandle(taskType, context);
        
        // Then
        assertThat(canHandle).isTrue();
        
        logger.info("Handle editor task with context test passed");
    }
    
    @Test
    @DisplayName("Should not handle unsupported task types")
    void shouldNotHandleUnsupportedTaskTypes() {
        // When/Then
        assertThat(editorAgent.canHandle("unsupported")).isFalse();
        assertThat(editorAgent.canHandle("random")).isFalse();
        assertThat(editorAgent.canHandle("")).isFalse();
        assertThat(editorAgent.canHandle(null)).isFalse();
        
        logger.info("Not handle unsupported task types test passed");
    }
}