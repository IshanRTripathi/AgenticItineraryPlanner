package com.tripplanner.testing.agent;

import com.tripplanner.agents.BaseAgent;
import com.tripplanner.dto.AgentCapabilities;
import com.tripplanner.dto.AgentEvent;
import com.tripplanner.service.AgentEventBus;
import com.tripplanner.testing.BaseServiceTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * Atomic tests for BaseAgent with mocked dependencies.
 */
class BaseAgentTest extends BaseServiceTest {
    
    @Mock
    private AgentEventBus mockEventBus;
    
    private TestAgent testAgent;
    
    @BeforeEach
    protected void setUp() {
        super.setUp();
        testAgent = new TestAgent(mockEventBus);
    }
    
    @Override
    protected void setupSpecificMocks() {
        // Setup event bus mock with lenient stubbing
        lenient().doNothing().when(mockEventBus).publish(anyString(), any(AgentEvent.class));
    }
    
    @Test
    @DisplayName("Should check if agent can handle supported task type")
    void shouldCheckIfAgentCanHandleSupportedTaskType() {
        // When
        boolean canHandle = testAgent.canHandle("test");
        
        // Then
        assertThat(canHandle).isTrue();
        
        logger.info("Agent can handle supported task type test passed");
    }
    
    @Test
    @DisplayName("Should check if agent cannot handle unsupported task type")
    void shouldCheckIfAgentCannotHandleUnsupportedTaskType() {
        // When
        boolean canHandle = testAgent.canHandle("unsupported");
        
        // Then
        assertThat(canHandle).isFalse();
        
        logger.info("Agent cannot handle unsupported task type test passed");
    }
    
    @Test
    @DisplayName("Should check if agent can handle task with context")
    void shouldCheckIfAgentCanHandleTaskWithContext() {
        // Given
        Map<String, Object> context = Map.of("operation", "test_operation");
        
        // When
        boolean canHandle = testAgent.canHandle("test", context);
        
        // Then
        assertThat(canHandle).isTrue();
        
        logger.info("Agent can handle task with context test passed");
    }
    
    @Test
    @DisplayName("Should get agent capabilities")
    void shouldGetAgentCapabilities() {
        // When
        AgentCapabilities capabilities = testAgent.getCapabilities();
        
        // Then
        assertThat(capabilities).isNotNull();
        assertThat(capabilities.getSupportedTasks()).contains("test");
        assertThat(capabilities.getPriority()).isEqualTo(1);
        
        logger.info("Get agent capabilities test passed");
    }
    
    @Test
    @DisplayName("Should validate responsibility for supported task")
    void shouldValidateResponsibilityForSupportedTask() {
        // Given
        Map<String, Object> data = Map.of("taskType", "test", "operation", "test_op");
        BaseAgent.AgentRequest<String> request = new BaseAgent.AgentRequest<>(data, String.class);
        
        // When/Then - Should not throw exception
        testAgent.validateResponsibility(request);
        
        logger.info("Validate responsibility for supported task test passed");
    }
    
    @Test
    @DisplayName("Should throw exception when validating responsibility for unsupported task")
    void shouldThrowExceptionWhenValidatingResponsibilityForUnsupportedTask() {
        // Given
        Map<String, Object> data = Map.of("taskType", "unsupported", "operation", "unsupported_op");
        BaseAgent.AgentRequest<String> request = new BaseAgent.AgentRequest<>(data, String.class);
        
        // When/Then
        assertThatThrownBy(() -> testAgent.validateResponsibility(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot handle task type: unsupported");
        
        logger.info("Validate responsibility exception test passed");
    }
    
    @Test
    @DisplayName("Should determine task type from request data")
    void shouldDetermineTaskTypeFromRequestData() {
        // Given
        Map<String, Object> data = Map.of("taskType", "custom_task", "operation", "test_op");
        BaseAgent.AgentRequest<String> request = new BaseAgent.AgentRequest<>(data, String.class);
        
        // When
        String taskType = testAgent.determineTaskType(request);
        
        // Then
        assertThat(taskType).isEqualTo("custom_task");
        
        logger.info("Determine task type from request test passed");
    }
    
    @Test
    @DisplayName("Should fallback to agent kind when no task type in data")
    void shouldFallbackToAgentKindWhenNoTaskTypeInData() {
        // Given
        Map<String, Object> data = Map.of("operation", "test_op");
        BaseAgent.AgentRequest<String> request = new BaseAgent.AgentRequest<>(data, String.class);
        
        // When
        String taskType = testAgent.determineTaskType(request);
        
        // Then - agent kind PLANNER normalizes to "plan" task type
        assertThat(taskType).isEqualTo("plan");
        
        logger.info("Fallback to agent kind test passed");
    }
    
    @Test
    @DisplayName("Should execute agent successfully")
    void shouldExecuteAgentSuccessfully() {
        // Given
        String itineraryId = "test-itinerary-001";
        Map<String, Object> data = Map.of("taskType", "test", "input", "test_input");
        BaseAgent.AgentRequest<String> request = new BaseAgent.AgentRequest<>(data, String.class);
        
        // When
        String result = testAgent.execute(itineraryId, request);
        
        // Then
        assertThat(result).isEqualTo("Test result for: test_input");
        
        // Verify events were published
        verify(mockEventBus, atLeast(3)).publish(eq(itineraryId), any(AgentEvent.class));
        
        logger.info("Execute agent successfully test passed");
    }
    
    @Test
    @DisplayName("Should handle agent execution failure")
    void shouldHandleAgentExecutionFailure() {
        // Given
        String itineraryId = "test-itinerary-001";
        Map<String, Object> data = Map.of("taskType", "test", "input", "error");
        BaseAgent.AgentRequest<String> request = new BaseAgent.AgentRequest<>(data, String.class);
        
        // When/Then
        assertThatThrownBy(() -> testAgent.execute(itineraryId, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Agent execution failed");
        
        // Verify failure event was published
        verify(mockEventBus, atLeast(2)).publish(eq(itineraryId), any(AgentEvent.class));
        
        logger.info("Handle agent execution failure test passed");
    }
    
    @Test
    @DisplayName("Should emit progress during execution")
    void shouldEmitProgressDuringExecution() {
        // Given
        String itineraryId = "test-itinerary-001";
        Map<String, Object> data = Map.of("taskType", "test", "input", "progress_test");
        BaseAgent.AgentRequest<String> request = new BaseAgent.AgentRequest<>(data, String.class);
        
        // When
        String result = testAgent.execute(itineraryId, request);
        
        // Then
        assertThat(result).isEqualTo("Test result for: progress_test");
        
        // Verify progress events were published (queued, running, progress, completed)
        verify(mockEventBus, atLeast(4)).publish(eq(itineraryId), any(AgentEvent.class));
        
        logger.info("Emit progress during execution test passed");
    }
    
    @Test
    @DisplayName("Should handle responsibility validation failure")
    void shouldHandleResponsibilityValidationFailure() {
        // Given
        String itineraryId = "test-itinerary-001";
        Map<String, Object> data = Map.of("taskType", "unsupported", "input", "test_input");
        BaseAgent.AgentRequest<String> request = new BaseAgent.AgentRequest<>(data, String.class);
        
        // When/Then
        assertThatThrownBy(() -> testAgent.execute(itineraryId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot handle task type");
        
        // Verify failure event was published
        verify(mockEventBus, atLeast(1)).publish(eq(itineraryId), any(AgentEvent.class));
        
        logger.info("Handle responsibility validation failure test passed");
    }
    
    @Test
    @DisplayName("Should handle agent request data casting")
    void shouldHandleAgentRequestDataCasting() {
        // Given
        Map<String, Object> data = Map.of("key", "value");
        BaseAgent.AgentRequest<String> request = new BaseAgent.AgentRequest<>(data, String.class);
        
        // When
        Map<String, Object> castData = request.getData();
        Map<String, Object> typedData = request.getData(Map.class);
        
        // Then
        assertThat(castData).isEqualTo(data);
        assertThat(typedData).isEqualTo(data);
        assertThat(request.getResponseType()).isEqualTo(String.class);
        
        logger.info("Agent request data casting test passed");
    }
    
    @Test
    @DisplayName("Should throw exception when casting to wrong type")
    void shouldThrowExceptionWhenCastingToWrongType() {
        // Given
        String data = "string_data";
        BaseAgent.AgentRequest<String> request = new BaseAgent.AgentRequest<>(data, String.class);
        
        // When/Then
        assertThatThrownBy(() -> request.getData(Map.class))
                .isInstanceOf(ClassCastException.class)
                .hasMessageContaining("Cannot cast String to Map");
        
        logger.info("Agent request casting exception test passed");
    }
    
    /**
     * Concrete test implementation of BaseAgent for testing.
     */
    private static class TestAgent extends BaseAgent {
        
        public TestAgent(AgentEventBus eventBus) {
            super(eventBus, AgentEvent.AgentKind.PLANNER);
        }
        
        @Override
        public AgentCapabilities getCapabilities() {
            AgentCapabilities capabilities = new AgentCapabilities();
            capabilities.setSupportedTasks(Arrays.asList("test"));
            capabilities.setPriority(1);
            return capabilities;
        }
        
        @Override
        protected String getAgentName() {
            return "TestAgent";
        }
        
        @Override
        protected <T> T executeInternal(String itineraryId, AgentRequest<T> request) {
            Map<String, Object> data = request.getData();
            String input = (String) data.get("input");
            
            // Simulate some work and emit progress
            emitProgress(itineraryId, 50, "Processing input", "processing");
            
            // Simulate error condition
            if ("error".equals(input)) {
                throw new RuntimeException("Simulated error");
            }
            
            // Return result
            @SuppressWarnings("unchecked")
            T result = (T) ("Test result for: " + input);
            return result;
        }
        
        // Make protected methods public for testing
        public void validateResponsibility(AgentRequest<?> request) {
            super.validateResponsibility(request);
        }
        
        public String determineTaskType(AgentRequest<?> request) {
            return super.determineTaskType(request);
        }
    }
}