package com.tripplanner.testing.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripplanner.dto.*;
import com.tripplanner.service.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * End-to-End Integration Test for Chat Flow.
 * 
 * Tests the complete flow:
 * User message → Intent classification → Agent selection → Agent execution → Response
 * 
 * Uses mocked LLM to avoid external API calls while testing real agent routing logic.
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Chat Flow E2E Integration Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ChatFlowE2ETest {

    @Autowired
    private OrchestratorService orchestratorService;

    @Autowired
    private AgentRegistry agentRegistry;

    @MockBean
    private LLMService llmService;

    @MockBean
    private ItineraryJsonService itineraryJsonService;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String TEST_ITINERARY_ID = "test-itin-e2e";
    private static final String TEST_USER_ID = "test-user-e2e";

    @BeforeEach
    void setUp() {
        // Mock a basic itinerary for context
        NormalizedItinerary mockItinerary = createMockItinerary();
        when(itineraryJsonService.getMasterItinerary(TEST_ITINERARY_ID))
            .thenReturn(Optional.of(mockItinerary));
    }

    @Test
    @Order(1)
    @DisplayName("E2E: User says 'hi' → Should handle conversational intent gracefully")
    void testGreetingMessage() {
        // Given: User sends a greeting
        String userMessage = "hi";
        
        // Mock LLM to classify as EXPLAIN with low confidence
        IntentResult intent = new IntentResult();
        intent.setIntent("EXPLAIN");
        intent.setTaskType("explain");
        intent.setConfidence(0.5);
        
        when(llmService.classifyIntent(eq(userMessage), anyString())).thenReturn(intent);
        
        // When: Process chat request
        ChatRequest request = new ChatRequest();
        request.setItineraryId(TEST_ITINERARY_ID);
        request.setUserId(TEST_USER_ID);
        request.setText(userMessage);
        request.setScope("trip");
        
        ChatResponse response = orchestratorService.route(request);
        
        // Then: Should route to PLANNER agent (handles explain)
        assertThat(response).isNotNull();
        assertThat(response.getIntent()).isEqualTo("EXPLAIN");
        
        // Verify LLM was called with itinerary context
        verify(llmService).classifyIntent(eq(userMessage), contains("Itinerary:"));
    }

    @Test
    @Order(2)
    @DisplayName("E2E: 'Move lunch to 2pm' → EditorAgent → ChangeSet response")
    void testEditIntent() {
        // Given: User wants to edit timing
        String userMessage = "Move lunch to 2pm";
        
        // Mock LLM classification
        IntentResult intent = new IntentResult();
        intent.setIntent("MOVE_TIME");
        intent.setTaskType("edit");
        intent.setConfidence(0.95);
        // Entities are optional - removed for simplicity
        
        when(llmService.classifyIntent(eq(userMessage), anyString())).thenReturn(intent);
        
        // When: Process chat request
        ChatRequest request = new ChatRequest();
        request.setItineraryId(TEST_ITINERARY_ID);
        request.setUserId(TEST_USER_ID);
        request.setText(userMessage);
        request.setScope("day");
        request.setDay(1);
        
        ChatResponse response = orchestratorService.route(request);
        
        // Then: Verify response structure
        assertThat(response).isNotNull();
        assertThat(response.getIntent()).isEqualTo("MOVE_TIME");
        assertThat(response.getMessage()).isNotNull();
        
        // Verify agent was selected correctly
        verify(llmService).classifyIntent(eq(userMessage), anyString());
        
        // Verify AgentRegistry was used to find EditorAgent
        List<com.tripplanner.agents.BaseAgent> agents = agentRegistry.getAgentsForTask("edit", true);
        assertThat(agents).hasSize(1);
        assertThat(agents.get(0)).isInstanceOf(com.tripplanner.agents.EditorAgent.class);
    }

    @Test
    @Order(3)
    @DisplayName("E2E: 'What is my plan for today?' → DayByDayPlannerAgent → Summary response")
    void testExplainIntent() {
        // Given: User asks for itinerary summary
        String userMessage = "What is my plan for today?";
        
        // Mock LLM classification
        IntentResult intent = new IntentResult();
        intent.setIntent("EXPLAIN");
        intent.setTaskType("explain");
        intent.setConfidence(0.92);
        
        when(llmService.classifyIntent(eq(userMessage), anyString())).thenReturn(intent);
        
        // When: Process chat request
        ChatRequest request = new ChatRequest();
        request.setItineraryId(TEST_ITINERARY_ID);
        request.setUserId(TEST_USER_ID);
        request.setText(userMessage);
        request.setScope("day");
        request.setDay(1);
        
        ChatResponse response = orchestratorService.route(request);
        
        // Then: Verify routing
        assertThat(response).isNotNull();
        assertThat(response.getIntent()).isEqualTo("EXPLAIN");
        
        // Verify correct agent selected
        List<com.tripplanner.agents.BaseAgent> agents = agentRegistry.getAgentsForTask("explain", true);
        assertThat(agents).hasSize(1);
        assertThat(agents.get(0)).isInstanceOf(com.tripplanner.agents.DayByDayPlannerAgent.class);
    }

    @Test
    @Order(4)
    @DisplayName("E2E: 'Book this hotel' → BookingAgent → Booking response")
    void testBookIntent() {
        // Given: User wants to book
        String userMessage = "Book this hotel for 2 nights";
        
        // Mock LLM classification
        IntentResult intent = new IntentResult();
        intent.setIntent("BOOK");
        intent.setTaskType("book");
        intent.setConfidence(0.96);
        // Entities are optional
        
        when(llmService.classifyIntent(eq(userMessage), anyString())).thenReturn(intent);
        
        // When: Process chat request
        ChatRequest request = new ChatRequest();
        request.setItineraryId(TEST_ITINERARY_ID);
        request.setUserId(TEST_USER_ID);
        request.setText(userMessage);
        request.setScope("day");
        request.setDay(1);
        request.setSelectedNodeId("node-hotel-123");
        
        ChatResponse response = orchestratorService.route(request);
        
        // Then: Verify routing
        assertThat(response).isNotNull();
        assertThat(response.getIntent()).isEqualTo("BOOK");
        
        // Verify correct agent selected
        List<com.tripplanner.agents.BaseAgent> agents = agentRegistry.getAgentsForTask("book", true);
        assertThat(agents).hasSize(1);
        assertThat(agents.get(0)).isInstanceOf(com.tripplanner.agents.BookingAgent.class);
    }

    @Test
    @Order(5)
    @DisplayName("E2E: 'Add photos to this place' → EnrichmentAgent → Enrichment response")
    void testEnrichIntent() {
        // Given: User wants to enrich
        String userMessage = "Add photos to this place";
        
        // Mock LLM classification
        IntentResult intent = new IntentResult();
        intent.setIntent("ENRICH");
        intent.setTaskType("enrich");
        intent.setConfidence(0.88);
        
        when(llmService.classifyIntent(eq(userMessage), anyString())).thenReturn(intent);
        
        // When: Process chat request
        ChatRequest request = new ChatRequest();
        request.setItineraryId(TEST_ITINERARY_ID);
        request.setUserId(TEST_USER_ID);
        request.setText(userMessage);
        request.setScope("day");
        request.setDay(1);
        request.setSelectedNodeId("node-attraction-123");
        
        ChatResponse response = orchestratorService.route(request);
        
        // Then: Verify routing
        assertThat(response).isNotNull();
        assertThat(response.getIntent()).isEqualTo("ENRICH");
        
        // Verify correct agent selected
        List<com.tripplanner.agents.BaseAgent> agents = agentRegistry.getAgentsForTask("enrich", true);
        assertThat(agents).hasSize(1);
        assertThat(agents.get(0)).isInstanceOf(com.tripplanner.agents.EnrichmentAgent.class);
    }

    @Test
    @Order(6)
    @DisplayName("E2E: Verify context is passed to LLM for classification")
    void testContextPassingToLLM() {
        // Given: User sends any message
        String userMessage = "Change the restaurant";
        
        IntentResult intent = new IntentResult();
        intent.setIntent("EDIT");
        intent.setTaskType("edit");
        intent.setConfidence(0.90);
        
        when(llmService.classifyIntent(anyString(), anyString())).thenReturn(intent);
        
        // When: Process chat request
        ChatRequest request = new ChatRequest();
        request.setItineraryId(TEST_ITINERARY_ID);
        request.setUserId(TEST_USER_ID);
        request.setText(userMessage);
        request.setScope("trip");
        
        orchestratorService.route(request);
        
        // Then: Verify LLM received itinerary context
        verify(llmService).classifyIntent(
            eq(userMessage), 
            argThat(context -> 
                context.contains("Itinerary:") &&
                context.contains("Days:") &&
                context.contains("Currency:") &&
                context.contains("Day 1")
            )
        );
    }

    @Test
    @Order(7)
    @DisplayName("E2E: Unknown task type → Error response")
    void testUnknownTaskType() {
        // Given: LLM returns unknown task type
        String userMessage = "Do something weird";
        
        IntentResult intent = new IntentResult();
        intent.setIntent("UNKNOWN");
        intent.setTaskType("unknown_task");
        intent.setConfidence(0.30);
        
        when(llmService.classifyIntent(eq(userMessage), anyString())).thenReturn(intent);
        
        // When: Process chat request
        ChatRequest request = new ChatRequest();
        request.setItineraryId(TEST_ITINERARY_ID);
        request.setUserId(TEST_USER_ID);
        request.setText(userMessage);
        request.setScope("trip");
        
        ChatResponse response = orchestratorService.route(request);
        
        // Then: Should return error response
        assertThat(response).isNotNull();
        assertThat(response.getErrors()).isNotEmpty();
        assertThat(response.getErrors().get(0)).contains("No suitable agents available");
    }

    @Test
    @Order(8)
    @DisplayName("E2E: Multi-step conversation flow")
    void testMultiStepConversation() {
        // Simulate a multi-turn conversation
        
        // Turn 1: User asks for plan
        IntentResult intent1 = new IntentResult();
        intent1.setIntent("EXPLAIN");
        intent1.setTaskType("explain");
        intent1.setConfidence(0.95);
        when(llmService.classifyIntent(eq("What's my plan?"), anyString())).thenReturn(intent1);
        
        ChatRequest req1 = new ChatRequest();
        req1.setItineraryId(TEST_ITINERARY_ID);
        req1.setUserId(TEST_USER_ID);
        req1.setText("What's my plan?");
        req1.setScope("trip");
        
        ChatResponse resp1 = orchestratorService.route(req1);
        assertThat(resp1.getIntent()).isEqualTo("EXPLAIN");
        
        // Turn 2: User modifies itinerary
        IntentResult intent2 = new IntentResult();
        intent2.setIntent("EDIT");
        intent2.setTaskType("edit");
        intent2.setConfidence(0.92);
        when(llmService.classifyIntent(eq("Move lunch to 3pm"), anyString())).thenReturn(intent2);
        
        ChatRequest req2 = new ChatRequest();
        req2.setItineraryId(TEST_ITINERARY_ID);
        req2.setUserId(TEST_USER_ID);
        req2.setText("Move lunch to 3pm");
        req2.setScope("day");
        req2.setDay(1);
        
        ChatResponse resp2 = orchestratorService.route(req2);
        assertThat(resp2.getIntent()).isEqualTo("EDIT");
        
        // Turn 3: User books
        IntentResult intent3 = new IntentResult();
        intent3.setIntent("BOOK");
        intent3.setTaskType("book");
        intent3.setConfidence(0.94);
        when(llmService.classifyIntent(eq("Book that restaurant"), anyString())).thenReturn(intent3);
        
        ChatRequest req3 = new ChatRequest();
        req3.setItineraryId(TEST_ITINERARY_ID);
        req3.setUserId(TEST_USER_ID);
        req3.setText("Book that restaurant");
        req3.setScope("day");
        req3.setDay(1);
        
        ChatResponse resp3 = orchestratorService.route(req3);
        assertThat(resp3.getIntent()).isEqualTo("BOOK");
        
        // Verify all three intents used different agents
        assertThat(resp1.getIntent()).isNotEqualTo(resp2.getIntent());
        assertThat(resp2.getIntent()).isNotEqualTo(resp3.getIntent());
    }

    // Helper method to create mock itinerary
    private NormalizedItinerary createMockItinerary() {
        NormalizedItinerary itinerary = new NormalizedItinerary();
        itinerary.setItineraryId(TEST_ITINERARY_ID);
        itinerary.setDestination("Paris, France");
        itinerary.setSummary("A 3-day trip to Paris");
        itinerary.setCurrency("EUR");
        itinerary.setVersion(1);
        
        // Add mock days
        NormalizedDay day1 = new NormalizedDay();
        day1.setDayNumber(1);
        day1.setLocation("Paris");
        day1.setDate("2024-06-01");
        
        // Add mock nodes
        NormalizedNode node1 = new NormalizedNode();
        node1.setId("node-1");
        node1.setTitle("Lunch at Le Bistro");
        node1.setType("meal");
        
        // Simplified - timing not required for test
        
        day1.setNodes(List.of(node1));
        itinerary.setDays(List.of(day1));
        
        return itinerary;
    }
}

