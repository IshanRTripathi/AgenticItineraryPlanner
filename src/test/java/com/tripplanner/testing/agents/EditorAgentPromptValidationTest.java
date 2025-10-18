package com.tripplanner.testing.agents;

import com.tripplanner.agents.EditorAgent;
import com.tripplanner.dto.ChatRequest;
import com.tripplanner.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests to validate EditorAgent prompt generation and structure.
 * These tests ensure the prompt correctly guides the LLM.
 */
class EditorAgentPromptValidationTest {
    
    @Mock
    private SummarizationService summarizationService;
    
    @Mock
    private ChangeEngine changeEngine;
    
    @Mock
    private GeminiClient geminiClient;
    
    @Mock
    private ItineraryJsonService itineraryJsonService;
    
    @Mock
    private LLMResponseHandler llmResponseHandler;
    
    @Mock
    private AgentEventBus eventBus;
    
    private ObjectMapper objectMapper;
    private EditorAgent editorAgent;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        objectMapper = new ObjectMapper();
        editorAgent = new EditorAgent(
            eventBus,
            summarizationService,
            changeEngine,
            geminiClient,
            itineraryJsonService,
            objectMapper,
            llmResponseHandler
        );
    }
    
    @Test
    @DisplayName("Prompt must include 24-hour time format rules")
    void promptMustInclude24HourTimeRules() throws Exception {
        ChatRequest request = new ChatRequest();
        request.setText("Move lunch to 2pm");
        request.setItineraryId("test-123");
        
        String prompt = buildPromptViaReflection(request, "test context");
        
        assertTrue(prompt.contains("24-hour time format"), "Prompt should specify 24-hour format");
        assertTrue(prompt.contains("HH:mm"), "Prompt should show HH:mm pattern");
        assertTrue(prompt.contains("\"14:30\""), "Prompt should have example like 14:30");
        assertTrue(prompt.contains("leading zeros"), "Prompt should mention leading zeros");
    }
    
    @Test
    @DisplayName("Prompt must include complete node structure example")
    void promptMustIncludeCompleteNodeStructure() throws Exception {
        ChatRequest request = new ChatRequest();
        request.setText("Add a restaurant");
        request.setItineraryId("test-123");
        
        String prompt = buildPromptViaReflection(request, "test context");
        
        // Check for required node fields
        assertTrue(prompt.contains("\"title\":"), "Prompt should show 'title' field");
        assertTrue(prompt.contains("\"type\":"), "Prompt should show 'type' field");
        assertTrue(prompt.contains("\"location\": {"), "Prompt should show 'location' as object");
        assertTrue(prompt.contains("\"name\":"), "Prompt should show location 'name'");
        assertTrue(prompt.contains("\"address\":"), "Prompt should show location 'address'");
        
        // Check for warning
        assertTrue(prompt.contains("CRITICAL"), "Prompt should have critical warnings");
        assertTrue(prompt.contains("'title' (not 'name')"), "Prompt should clarify title vs name");
    }
    
    @Test
    @DisplayName("Prompt must specify startTime/endTime as strings")
    void promptMustSpecifyTimeAsStrings() throws Exception {
        ChatRequest request = new ChatRequest();
        request.setText("Change time");
        request.setItineraryId("test-123");
        
        String prompt = buildPromptViaReflection(request, "test context");
        
        assertTrue(prompt.contains("\"startTime\": \"18:30\""), "Example should show startTime as string");
        assertTrue(prompt.contains("\"endTime\": \"19:30\""), "Example should show endTime as string");
    }
    
    @Test
    @DisplayName("Prompt must include operation types")
    void promptMustIncludeOperationTypes() throws Exception {
        ChatRequest request = new ChatRequest();
        request.setText("Test request");
        request.setItineraryId("test-123");
        
        String prompt = buildPromptViaReflection(request, "test context");
        
        assertTrue(prompt.contains("insert"), "Prompt should mention insert operation");
        assertTrue(prompt.contains("delete"), "Prompt should mention delete operation");
        assertTrue(prompt.contains("move"), "Prompt should mention move operation");
        assertTrue(prompt.contains("replace"), "Prompt should mention replace operation");
    }
    
    @Test
    @DisplayName("Prompt must include user request and context")
    void promptMustIncludeUserRequestAndContext() throws Exception {
        ChatRequest request = new ChatRequest();
        request.setText("Add sushi restaurant on day 2");
        request.setItineraryId("test-123");
        
        String context = "Day 2: Lunch at 12:00, Dinner at 18:00";
        String prompt = buildPromptViaReflection(request, context);
        
        assertTrue(prompt.contains("Add sushi restaurant on day 2"), "Prompt should include user request");
        assertTrue(prompt.contains(context), "Prompt should include itinerary context");
    }
    
    @Test
    @DisplayName("Prompt must specify agent name")
    void promptMustSpecifyAgentName() throws Exception {
        ChatRequest request = new ChatRequest();
        request.setText("Test");
        request.setItineraryId("test-123");
        
        String prompt = buildPromptViaReflection(request, "test context");
        
        assertTrue(prompt.contains("EditorAgent"), "Prompt should mention EditorAgent");
    }
    
    /**
     * Use reflection to access private buildChangeSetPrompt method for testing.
     */
    private String buildPromptViaReflection(ChatRequest request, String context) throws Exception {
        var method = EditorAgent.class.getDeclaredMethod("buildChangeSetPrompt", ChatRequest.class, String.class);
        method.setAccessible(true);
        return (String) method.invoke(editorAgent, request, context);
    }
}





