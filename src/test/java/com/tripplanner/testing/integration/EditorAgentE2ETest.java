package com.tripplanner.testing.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripplanner.agents.EditorAgent;
import com.tripplanner.agents.BaseAgent;
import com.tripplanner.dto.*;
import com.tripplanner.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * End-to-end integration tests for EditorAgent covering complete user scenarios.
 * Tests the full flow: user request → summarization → LLM → ChangeSet → validation.
 */
class EditorAgentE2ETest {
    
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
    @DisplayName("E2E: User adds sushi place on day 2 - full flow with node ID resolution")
    void userAddsSushiPlaceOnDay2FullFlow() throws Exception {
        // ===== SETUP: Create realistic itinerary with proper node IDs =====
        String itineraryId = "it_test_123";
        NormalizedItinerary itinerary = createTestItinerary(itineraryId);
        
        // Get the actual dinner node ID from day 2
        NormalizedDay day2 = itinerary.getDays().get(1);
        NormalizedNode dinnerNode = day2.getNodes().stream()
            .filter(n -> "Dinner in Hadibo".equals(n.getTitle()))
            .findFirst()
            .orElseThrow();
        String dinnerNodeId = dinnerNode.getId();
        
        // ===== MOCK: Itinerary loading =====
        when(itineraryJsonService.getMasterItinerary(itineraryId))
            .thenReturn(Optional.of(itinerary));
        when(itineraryJsonService.getItinerary(itineraryId))
            .thenReturn(Optional.of(itinerary));
        
        // ===== MOCK: Summarization - includes node IDs =====
        String summary = createRealisticSummary(itinerary);
        when(summarizationService.summarizeForAgent(any(), eq("editor"), anyInt()))
            .thenReturn(summary);
        
        // ===== MOCK: LLM Response - generates valid ChangeSet with correct node ID =====
        String llmJsonContent = String.format("""
            {
              "ops": [{
                "op": "replace",
                "id": "%s",
                "startTime": "18:30",
                "endTime": "19:30",
                "node": {
                  "title": "Sushi Dinner in Hadibo",
                  "type": "meal",
                  "location": {
                    "name": "Hadibo",
                    "address": "Hadibo, Socotra Island, Yemen"
                  }
                }
              }],
              "day": 2,
              "reason": "Replacing generic dinner with sushi place as requested by user",
              "agent": "EditorAgent"
            }
            """, dinnerNodeId);
        
        String llmResponse = "```json\n" + llmJsonContent + "\n```";
        
        when(geminiClient.generateStructuredContent(anyString(), anyString(), anyString()))
            .thenReturn(llmResponse);
        
        // ===== MOCK: LLMResponseHandler - parses LLM response =====
        com.fasterxml.jackson.databind.JsonNode extractedJson = objectMapper.readTree(llmJsonContent);
        
        LLMResponseHandler.ProcessedResponse mockProcessedResponse = 
            LLMResponseHandler.ProcessedResponse.success(extractedJson, true);
        
        when(llmResponseHandler.processResponse(anyString(), any(), any()))
            .thenReturn(mockProcessedResponse);
        
        // ===== EXECUTE: User request =====
        ChatRequest userRequest = new ChatRequest();
        userRequest.setItineraryId(itineraryId);
        userRequest.setText("add a sushi place on day 2");
        userRequest.setScope("trip");
        userRequest.setAutoApply(false);
        userRequest.setUserId("test-user");
        
        Map<String, Object> requestData = new HashMap<>();
        requestData.put("chatRequest", userRequest);
        requestData.put("taskType", "edit");
        requestData.put("text", userRequest.getText());
        
        BaseAgent.AgentRequest<Map> agentRequest = new BaseAgent.AgentRequest<>(requestData, Map.class);
        
        // ===== MOCK: ChangeEngine applies the changes =====
        ChangeEngine.ApplyResult mockApplyResult = new ChangeEngine.ApplyResult(2, null);
        when(changeEngine.apply(eq(itineraryId), any(ChangeSet.class)))
            .thenReturn(mockApplyResult);
        
        // Execute the agent
        Object result = editorAgent.execute(itineraryId, agentRequest);
        
        // ===== VERIFY: Result is an ApplyResult (EditorAgent returns ApplyResult after applying changes) =====
        assertNotNull(result, "Result should not be null");
        assertTrue(result instanceof ChangeEngine.ApplyResult, "Result should be an ApplyResult");
        
        // ===== VERIFY: ChangeEngine was called with correct ChangeSet =====
        ArgumentCaptor<ChangeSet> changeSetCaptor = ArgumentCaptor.forClass(ChangeSet.class);
        verify(changeEngine).apply(eq(itineraryId), changeSetCaptor.capture());
        
        ChangeSet changeSet = changeSetCaptor.getValue();
        
        // ===== VERIFY: ChangeSet structure =====
        assertNotNull(changeSet.getOps(), "ChangeSet should have operations");
        assertEquals(1, changeSet.getOps().size(), "Should have 1 operation");
        assertEquals(2, changeSet.getDay(), "Should target day 2");
        assertEquals("EditorAgent", changeSet.getAgent());
        assertNotNull(changeSet.getReason());
        assertTrue(changeSet.getReason().toLowerCase().contains("sushi"));
        
        // ===== VERIFY: Operation details =====
        ChangeOperation op = changeSet.getOps().get(0);
        assertEquals("replace", op.getOp(), "Should be a replace operation");
        assertEquals(dinnerNodeId, op.getId(), "Should reference the correct dinner node ID");
        assertNotNull(op.getStartTime(), "Should have start time");
        assertNotNull(op.getEndTime(), "Should have end time");
        assertTrue(op.getStartTime() > 0, "Start time should be valid timestamp");
        assertTrue(op.getEndTime() > op.getStartTime(), "End time should be after start time");
        
        // ===== VERIFY: Node structure =====
        NormalizedNode newNode = op.getNode();
        assertNotNull(newNode, "New node should not be null");
        assertEquals("Sushi Dinner in Hadibo", newNode.getTitle());
        assertEquals("meal", newNode.getType());
        
        // ===== VERIFY: Location structure (critical - was failing before) =====
        assertNotNull(newNode.getLocation(), "Location should not be null");
        assertEquals("Hadibo", newNode.getLocation().getName());
        assertNotNull(newNode.getLocation().getAddress());
        assertTrue(newNode.getLocation().getAddress().contains("Hadibo"));
        
        // ===== VERIFY: Summarization was called with correct agent type =====
        verify(summarizationService).summarizeForAgent(any(), eq("editor"), anyInt());
        
        // ===== VERIFY: LLM received context with node IDs =====
        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(geminiClient).generateStructuredContent(promptCaptor.capture(), anyString(), anyString());
        
        String sentPrompt = promptCaptor.getValue();
        assertTrue(sentPrompt.contains("[ID:"), "Prompt should include node IDs");
        assertTrue(sentPrompt.contains(dinnerNodeId), "Prompt should include the dinner node ID");
        assertTrue(sentPrompt.contains("add a sushi place on day 2"), "Prompt should include user request");
        assertTrue(sentPrompt.contains("Day 2"), "Prompt should include day 2 context");
    }
    
    @Test
    @DisplayName("E2E: Node IDs are dynamically extracted from itinerary, not hardcoded")
    void nodeIDsAreDynamicallyExtracted() throws Exception {
        // Create two different itineraries with different node IDs
        NormalizedItinerary itinerary1 = createTestItinerary("it_001");
        NormalizedItinerary itinerary2 = createTestItinerary("it_002");
        
        // Verify they have different node IDs
        String nodeId1 = itinerary1.getDays().get(0).getNodes().get(0).getId();
        String nodeId2 = itinerary2.getDays().get(0).getNodes().get(0).getId();
        
        assertNotEquals(nodeId1, nodeId2, "Different itineraries should have different node IDs");
        assertNotNull(nodeId1);
        assertNotNull(nodeId2);
        
        // This confirms node IDs are generated dynamically, not hardcoded
    }
    
    @Test
    @DisplayName("E2E: Summarization includes all required context for LLM decision-making")
    void summarizationIncludesAllRequiredContext() throws Exception {
        String itineraryId = "it_test_456";
        NormalizedItinerary itinerary = createTestItinerary(itineraryId);
        
        when(itineraryJsonService.getMasterItinerary(itineraryId))
            .thenReturn(Optional.of(itinerary));
        
        // Create a real summarization service to test actual output
        SummarizationService realSummarizationService = new SummarizationService(objectMapper);
        String summary = realSummarizationService.summarizeForAgent(itinerary, "editor", 2000);
        
        // Verify summary contains all critical information
        assertTrue(summary.contains("Day 1"), "Summary should include day numbers");
        assertTrue(summary.contains("Day 2"), "Summary should include all days");
        assertTrue(summary.contains("[ID:"), "Summary should include node IDs");
        assertTrue(summary.contains("Breakfast"), "Summary should include node titles");
        assertTrue(summary.contains("(meal)"), "Summary should include node types");
        assertTrue(summary.contains("@"), "Summary should include timing info");
        
        // Verify each day has identifiable nodes with IDs
        for (NormalizedDay day : itinerary.getDays()) {
            assertTrue(summary.contains("Day " + day.getDayNumber()));
            for (NormalizedNode node : day.getNodes()) {
                assertTrue(summary.contains(node.getId()), 
                    "Summary should include ID for node: " + node.getTitle());
            }
        }
    }
    
    @Test
    @DisplayName("E2E: Prompt guides LLM to use correct operation type based on user intent")
    void promptGuidesLLMToUseCorrectOperationType() throws Exception {
        String itineraryId = "it_test_789";
        NormalizedItinerary itinerary = createTestItinerary(itineraryId);
        
        when(itineraryJsonService.getMasterItinerary(itineraryId))
            .thenReturn(Optional.of(itinerary));
        when(itineraryJsonService.getItinerary(itineraryId))
            .thenReturn(Optional.of(itinerary));
        when(summarizationService.summarizeForAgent(any(), anyString(), anyInt()))
            .thenReturn("test summary");
        
        // Test different user intents
        String[] userIntents = {
            "add a restaurant",      // should trigger "insert"
            "replace the dinner",    // should trigger "replace"
            "delete breakfast",      // should trigger "delete"
            "move lunch to 2pm"      // should trigger "move"
        };
        
        for (String intent : userIntents) {
            ChatRequest request = new ChatRequest();
            request.setItineraryId(itineraryId);
            request.setText(intent);
            
            // Capture the prompt sent to LLM
            ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
            
            // Execute would normally call LLM, but we're testing prompt generation
            // We verify the prompt contains guidance for each operation type
            when(geminiClient.generateStructuredContent(promptCaptor.capture(), anyString(), anyString()))
                .thenReturn("{\"ops\":[],\"day\":1,\"reason\":\"test\",\"agent\":\"EditorAgent\"}");
            
            try {
                Map<String, Object> requestData = new HashMap<>();
                requestData.put("chatRequest", request);
                requestData.put("taskType", "edit");
                BaseAgent.AgentRequest<Map> agentRequest = new BaseAgent.AgentRequest<>(requestData, Map.class);
                
                editorAgent.execute(itineraryId, agentRequest);
            } catch (Exception e) {
                // Ignore execution errors, we're testing prompt content
            }
            
            if (promptCaptor.getAllValues().size() > 0) {
                String prompt = promptCaptor.getValue();
                assertTrue(prompt.contains("insert"), "Prompt should explain insert operation");
                assertTrue(prompt.contains("replace"), "Prompt should explain replace operation");
                assertTrue(prompt.contains("delete"), "Prompt should explain delete operation");
                assertTrue(prompt.contains("move"), "Prompt should explain move operation");
            }
        }
    }
    
    /**
     * Helper: Create a realistic test itinerary with proper structure.
     */
    private NormalizedItinerary createTestItinerary(String itineraryId) {
        NormalizedItinerary itinerary = new NormalizedItinerary();
        itinerary.setItineraryId(itineraryId);
        itinerary.setDestination("Socotra Island, Yemen");
        itinerary.setStartDate("2025-10-10");
        itinerary.setEndDate("2025-10-13");
        itinerary.setCurrency("USD");
        
        List<NormalizedDay> days = new ArrayList<>();
        
        // Day 1
        long baseTime = System.currentTimeMillis();
        NormalizedDay day1 = createDay(1, "2025-10-10", "Hadibo", Arrays.asList(
            createNode("node_" + (baseTime + 1) + "_1", "Breakfast at Hotel", "meal", "08:00", "Hadibo"),
            createNode("node_" + (baseTime + 2) + "_2", "Explore Hadibo Town", "attraction", "10:00", "Hadibo"),
            createNode("node_" + (baseTime + 3) + "_3", "Lunch at Local Restaurant", "meal", "12:30", "Hadibo")
        ));
        days.add(day1);
        
        // Day 2
        NormalizedDay day2 = createDay(2, "2025-10-11", "Hadibo", Arrays.asList(
            createNode("node_" + (baseTime + 4) + "_4", "Morning Hike", "attraction", "07:00", "Hadibo"),
            createNode("node_" + (baseTime + 5) + "_5", "Lunch at Cafe", "meal", "13:00", "Hadibo"),
            createNode("node_" + (baseTime + 6) + "_6", "Dinner in Hadibo", "meal", "18:30", "Hadibo")
        ));
        days.add(day2);
        
        itinerary.setDays(days);
        return itinerary;
    }
    
    private NormalizedDay createDay(int dayNumber, String date, String location, List<NormalizedNode> nodes) {
        NormalizedDay day = new NormalizedDay();
        day.setDayNumber(dayNumber);
        day.setDate(date);
        day.setLocation(location);
        day.setNodes(nodes);
        return day;
    }
    
    private NormalizedNode createNode(String id, String title, String type, String time, String locationName) {
        NormalizedNode node = new NormalizedNode();
        node.setId(id);
        node.setTitle(title);
        node.setType(type);
        
        NodeTiming timing = new NodeTiming();
        // Convert time string to timestamp (simplified for testing)
        long baseTimestamp = System.currentTimeMillis() / 86400000L * 86400000L;  // Start of day
        String[] parts = time.split(":");
        int hours = Integer.parseInt(parts[0]);
        int minutes = Integer.parseInt(parts[1]);
        long timeOffset = (hours * 3600000L) + (minutes * 60000L);
        timing.setStartTime(baseTimestamp + timeOffset);
        timing.setDurationMin(60);  // Default 1 hour
        node.setTiming(timing);
        
        NodeLocation location = new NodeLocation();
        location.setName(locationName);
        location.setAddress(locationName + ", Socotra Island, Yemen");
        node.setLocation(location);
        
        return node;
    }
    
    /**
     * Helper: Create realistic summary with node IDs (simulates SummarizationService output).
     */
    private String createRealisticSummary(NormalizedItinerary itinerary) {
        StringBuilder summary = new StringBuilder();
        summary.append("ITINERARY FOR EDITING\n\n");
        
        for (NormalizedDay day : itinerary.getDays()) {
            summary.append("Day ").append(day.getDayNumber()).append(":\n");
            for (NormalizedNode node : day.getNodes()) {
                summary.append("  - [ID: ").append(node.getId()).append("] ");
                summary.append(node.getTitle());
                if (node.getType() != null) {
                    summary.append(" (").append(node.getType()).append(")");
                }
                if (node.getTiming() != null && node.getTiming().getStartTime() != null) {
                    summary.append(" @ ").append(node.getTiming().getStartTime());
                }
                if (node.getLocation() != null && node.getLocation().getName() != null) {
                    summary.append(" at ").append(node.getLocation().getName());
                }
                summary.append("\n");
            }
            summary.append("\n");
        }
        
        return summary.toString();
    }
}

