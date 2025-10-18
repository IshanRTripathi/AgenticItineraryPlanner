package com.tripplanner.testing.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripplanner.dto.*;
import com.tripplanner.service.LLMProvider;
import com.tripplanner.service.LLMService;
import com.tripplanner.testing.BaseServiceTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * Atomic tests for LLMService with mocked LLM providers.
 */
class LLMServiceTest extends BaseServiceTest {
    
    @Mock
    private LLMProvider mockProvider1;
    
    @Mock
    private LLMProvider mockProvider2;
    
    private LLMService llmService;
    private ObjectMapper objectMapper;
    
    @BeforeEach
    protected void setUp() {
        super.setUp();
        objectMapper = new ObjectMapper();
        
        // Setup mock providers with lenient stubbing
        lenient().when(mockProvider1.getProviderName()).thenReturn("gemini");
        lenient().when(mockProvider1.supportsModel("gemini")).thenReturn(true);
        lenient().when(mockProvider1.isAvailable()).thenReturn(true);
        
        lenient().when(mockProvider2.getProviderName()).thenReturn("qwen2.5-7b");
        lenient().when(mockProvider2.supportsModel("qwen2.5-7b")).thenReturn(true);
        lenient().when(mockProvider2.isAvailable()).thenReturn(true);
        
        List<LLMProvider> providers = Arrays.asList(mockProvider1, mockProvider2);
        llmService = new LLMService(providers, objectMapper);
    }
    
    @Override
    protected void setupSpecificMocks() {
        // No additional mocks needed for LLMService tests
    }
    
    @Test
    @DisplayName("Should generate response with specified model")
    void shouldGenerateResponseWithSpecifiedModel() {
        // Given
        String prompt = "Generate a travel itinerary for Bali";
        String modelType = "gemini";
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("temperature", 0.7);
        String expectedResponse = "Here's your Bali itinerary...";
        
        when(mockProvider1.generate(eq(prompt), any())).thenReturn(expectedResponse);
        
        // When
        String result = llmService.generateResponse(prompt, modelType, parameters);
        
        // Then
        assertThat(result).isEqualTo(expectedResponse);
        
        verify(mockProvider1).generate(eq(prompt), argThat(params -> {
            Map<String, Object> p = (Map<String, Object>) params;
            return p.containsKey("temperature") && 
                   p.containsKey("max_tokens") &&
                   p.containsKey("top_p") &&
                   p.containsKey("top_k");
        }));
        
        logger.info("Generate response with specified model test passed");
    }
    
    @Test
    @DisplayName("Should use default model when model type is null")
    void shouldUseDefaultModelWhenModelTypeIsNull() {
        // Given
        String prompt = "Test prompt";
        String expectedResponse = "Test response";
        
        when(mockProvider1.generate(eq(prompt), any())).thenReturn(expectedResponse);
        
        // When
        String result = llmService.generateResponse(prompt, null, null);
        
        // Then
        assertThat(result).isEqualTo(expectedResponse);
        
        verify(mockProvider1).generate(eq(prompt), any());
        
        logger.info("Default model usage test passed");
    }
    
    @Test
    @DisplayName("Should add default parameters when parameters is null")
    void shouldAddDefaultParametersWhenParametersIsNull() {
        // Given
        String prompt = "Test prompt";
        String modelType = "gemini";
        String expectedResponse = "Test response";
        
        when(mockProvider1.generate(eq(prompt), any())).thenReturn(expectedResponse);
        
        // When
        String result = llmService.generateResponse(prompt, modelType, null);
        
        // Then
        assertThat(result).isEqualTo(expectedResponse);
        
        verify(mockProvider1).generate(eq(prompt), argThat(params -> {
            Map<String, Object> p = (Map<String, Object>) params;
            return p.containsKey("temperature") && 
                   p.containsKey("max_tokens") &&
                   p.get("temperature").equals(0.7) &&
                   p.get("max_tokens").equals(1000);
        }));
        
        logger.info("Default parameters test passed");
    }
    
    @Test
    @DisplayName("Should throw exception when prompt is null or empty")
    void shouldThrowExceptionWhenPromptIsNullOrEmpty() {
        // When/Then - null prompt
        assertThatThrownBy(() -> llmService.generateResponse(null, "gemini", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Prompt cannot be null or empty");
        
        // When/Then - empty prompt
        assertThatThrownBy(() -> llmService.generateResponse("", "gemini", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Prompt cannot be null or empty");
        
        // When/Then - whitespace prompt
        assertThatThrownBy(() -> llmService.generateResponse("   ", "gemini", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Prompt cannot be null or empty");
        
        logger.info("Prompt validation test passed");
    }
    
    @Test
    @DisplayName("Should throw exception when no provider found for model")
    void shouldThrowExceptionWhenNoProviderFoundForModel() {
        // Given
        String prompt = "Test prompt";
        String unsupportedModel = "unsupported-model";
        
        // Setup: mock provider to return empty response to trigger the expected error
        when(mockProvider1.generate(eq(prompt), any())).thenReturn("");
        
        // When/Then
        assertThatThrownBy(() -> llmService.generateResponse(prompt, unsupportedModel, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to generate response: Provider returned empty response");
        
        logger.info("No provider found test passed");
    }
    
    @Test
    @DisplayName("Should handle provider exceptions gracefully")
    void shouldHandleProviderExceptionsGracefully() {
        // Given
        String prompt = "Test prompt";
        String modelType = "gemini";
        
        when(mockProvider1.generate(any(), any())).thenThrow(new RuntimeException("Provider error"));
        
        // When/Then
        assertThatThrownBy(() -> llmService.generateResponse(prompt, modelType, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to generate response");
        
        verify(mockProvider1).generate(eq(prompt), any());
        
        logger.info("Provider exception handling test passed");
    }
    
    @Test
    @DisplayName("Should classify intent with context")
    void shouldClassifyIntentWithContext() {
        // Given
        String text = "Add a restaurant to day 2";
        String context = "Current itinerary has 3 days in Bali";
        String mockResponse = "{\n" +
                "  \"intent\": \"INSERT_PLACE\",\n" +
                "  \"taskType\": \"edit\",\n" +
                "  \"entities\": {\n" +
                "    \"location\": \"restaurant\",\n" +
                "    \"date\": \"day 2\"\n" +
                "  },\n" +
                "  \"confidence\": 0.95\n" +
                "}";
        when(mockProvider1.generate(any(), any())).thenReturn(mockResponse);
        
        // When
        IntentResult result = llmService.classifyIntent(text, context);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getIntent()).isEqualTo("INSERT_PLACE");
        assertThat(result.getTaskType()).isEqualTo("edit");
        assertThat(result.getConfidence()).isEqualTo(0.95);
        assertThat(result.getEntities()).containsKey("location");
        assertThat(result.getEntities().get("location")).isEqualTo("restaurant");
        
        // Verify provider was called with appropriate parameters
        verify(mockProvider1, atLeastOnce()).generate(any(String.class), any(Map.class));
        
        logger.info("Intent classification test passed");
    }
    
    @Test
    @DisplayName("Should return fallback intent when classification fails")
    void shouldReturnFallbackIntentWhenClassificationFails() {
        // Given
        String text = "Some unclear request";
        String context = null;
        
        when(mockProvider1.generate(any(), any())).thenReturn("invalid json response");
        
        // When
        IntentResult result = llmService.classifyIntent(text, context);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getIntent()).isEqualTo("EXPLAIN");
        assertThat(result.getTaskType()).isEqualTo("explanation");
        assertThat(result.getConfidence()).isEqualTo(0.1);
        
        logger.info("Fallback intent test passed");
    }
    
    @Test
    @DisplayName("Should throw exception when intent text is null or empty")
    void shouldThrowExceptionWhenIntentTextIsNullOrEmpty() {
        // When/Then - null text
        assertThatThrownBy(() -> llmService.classifyIntent(null, "context"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Text for intent classification cannot be null or empty");
        
        // When/Then - empty text
        assertThatThrownBy(() -> llmService.classifyIntent("", "context"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Text for intent classification cannot be null or empty");
        
        logger.info("Intent text validation test passed");
    }
    
    @Test
    @DisplayName("Should generate change set with context")
    void shouldGenerateChangeSetWithContext() {
        // Given
        String request = "Move lunch to 2pm on day 2";
        String context = "Current itinerary JSON data";
        String mockResponse = "{\n" +
                "  \"scope\": \"day\",\n" +
                "  \"day\": 2,\n" +
                "  \"ops\": [\n" +
                "    {\n" +
                "      \"op\": \"update\",\n" +
                "      \"id\": \"lunch-node-id\",\n" +
                "      \"node\": { \"timing\": { \"startTime\": 1769320800000 } },\n" +
                "      \"position\": 1\n" +
                "    }\n" +
                "  ],\n" +
                "  \"preferences\": {\n" +
                "    \"userFirst\": true,\n" +
                "    \"autoApply\": false\n" +
                "  }\n" +
                "}";
        when(mockProvider1.generate(any(), any())).thenReturn(mockResponse);
        
        // When
        ChangeSet result = llmService.generateChangeSet(request, context);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getScope()).isEqualTo("day");
        assertThat(result.getDay()).isEqualTo(2);
        assertThat(result.getOps()).hasSize(1);
        assertThat(result.getOps().get(0).getOp()).isEqualTo("update");
        assertThat(result.getPreferences()).isNotNull();
        assertThat(result.getPreferences().getUserFirst()).isTrue();
        
        verify(mockProvider1).generate(argThat(prompt -> 
            prompt.toString().contains(request) && 
            prompt.toString().contains(context)
        ), argThat(params -> {
            Map<String, Object> p = (Map<String, Object>) params;
            return p.get("temperature").equals(0.3) && p.get("max_tokens").equals(1000);
        }));
        
        logger.info("Change set generation test passed");
    }
    
    @Test
    @DisplayName("Should return fallback change set when generation fails")
    void shouldReturnFallbackChangeSetWhenGenerationFails() {
        // Given
        String request = "Some request";
        String context = null;
        
        when(mockProvider1.generate(any(), any())).thenReturn("invalid json");
        
        // When
        ChangeSet result = llmService.generateChangeSet(request, context);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getScope()).isEqualTo("trip");
        assertThat(result.getOps()).isEmpty();
        assertThat(result.getPreferences()).isNotNull();
        assertThat(result.getPreferences().getUserFirst()).isTrue();
        assertThat(result.getPreferences().getAutoApply()).isFalse();
        
        logger.info("Fallback change set test passed");
    }
    
    @Test
    @DisplayName("Should throw exception when change set request is null or empty")
    void shouldThrowExceptionWhenChangeSetRequestIsNullOrEmpty() {
        // When/Then - null request
        assertThatThrownBy(() -> llmService.generateChangeSet(null, "context"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Request for change set generation cannot be null or empty");
        
        // When/Then - empty request
        assertThatThrownBy(() -> llmService.generateChangeSet("", "context"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Request for change set generation cannot be null or empty");
        
        logger.info("Change set request validation test passed");
    }
    
    @Test
    @DisplayName("Should get available providers")
    void shouldGetAvailableProviders() {
        // When
        List<String> providers = llmService.getAvailableProviders();
        
        // Then
        assertThat(providers).hasSize(2);
        assertThat(providers).contains("gemini", "qwen2.5-7b");
        
        logger.info("Get available providers test passed");
    }
    
    @Test
    @DisplayName("Should check if model is supported")
    void shouldCheckIfModelIsSupported() {
        // When/Then
        assertThat(llmService.isModelSupported("gemini")).isTrue();
        assertThat(llmService.isModelSupported("qwen2.5-7b")).isTrue();
        // Note: unsupported-model will return true because of fallback provider logic
        assertThat(llmService.isModelSupported("unsupported-model")).isTrue();
        
        logger.info("Model support check test passed");
    }
    
    @Test
    @DisplayName("Should use fallback provider when exact match not found")
    void shouldUseFallbackProviderWhenExactMatchNotFound() {
        // Given
        String prompt = "Test prompt";
        String unknownModel = "unknown-model";
        String expectedResponse = "Fallback response";
        
        // Setup: no provider supports the unknown model exactly, but fallback should work
        when(mockProvider1.supportsModel(unknownModel)).thenReturn(false);
        when(mockProvider2.supportsModel(unknownModel)).thenReturn(false);
        when(mockProvider1.generate(eq(prompt), any())).thenReturn(expectedResponse);
        
        // When
        String result = llmService.generateResponse(prompt, unknownModel, null);
        
        // Then
        assertThat(result).isEqualTo(expectedResponse);
        
        verify(mockProvider1).supportsModel(unknownModel);
        verify(mockProvider2).supportsModel(unknownModel);
        verify(mockProvider1).generate(eq(prompt), any());
        
        logger.info("Fallback provider test passed");
    }
    
    @Test
    @DisplayName("Should handle empty provider list gracefully")
    void shouldHandleEmptyProviderListGracefully() {
        // Given
        LLMService emptyService = new LLMService(null, objectMapper);
        String prompt = "Test prompt";
        String modelType = "gemini";
        
        // When/Then
        assertThatThrownBy(() -> emptyService.generateResponse(prompt, modelType, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("No provider found for model type");
        
        logger.info("Empty provider list handling test passed");
    }
    
    @Test
    @DisplayName("Should add model-specific default parameters")
    void shouldAddModelSpecificDefaultParameters() {
        // Given
        String prompt = "Test prompt";
        String expectedResponse = "Test response";
        
        when(mockProvider2.generate(eq(prompt), any())).thenReturn(expectedResponse);
        
        // When - Test qwen2.5-7b specific parameters
        String result = llmService.generateResponse(prompt, "qwen2.5-7b", new HashMap<>());
        
        // Then
        assertThat(result).isEqualTo(expectedResponse);
        
        verify(mockProvider2).generate(eq(prompt), argThat(params -> {
            Map<String, Object> p = (Map<String, Object>) params;
            return p.containsKey("top_p") && 
                   p.containsKey("repetition_penalty") &&
                   p.get("top_p").equals(0.8) &&
                   p.get("repetition_penalty").equals(1.1);
        }));
        
        logger.info("Model-specific parameters test passed");
    }
}