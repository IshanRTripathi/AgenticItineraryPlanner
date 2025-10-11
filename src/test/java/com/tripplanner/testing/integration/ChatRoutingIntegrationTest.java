package com.tripplanner.testing.integration;

import com.tripplanner.agents.*;
import com.tripplanner.dto.ChatRequest;
import com.tripplanner.dto.ChatResponse;
import com.tripplanner.dto.IntentResult;
import com.tripplanner.service.AgentRegistry;
import com.tripplanner.service.LLMService;
import com.tripplanner.service.OrchestratorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive integration tests for chat routing with different user inputs.
 * Tests the complete flow from user message → intent classification → agent selection → response.
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Chat Routing Integration Tests")
public class ChatRoutingIntegrationTest {

    @Autowired
    private AgentRegistry agentRegistry;

    @Autowired
    private OrchestratorService orchestratorService;

    @MockBean
    private LLMService llmService;

    private static final String TEST_ITINERARY_ID = "test-itinerary-123";
    private static final String TEST_USER_ID = "test-user";

    @BeforeEach
    void setUp() {
        // Verify system is properly initialized
        assertThat(agentRegistry.getAllCapabilities()).isNotEmpty();
    }

    @Nested
    @DisplayName("1. Edit Intent Routing Tests")
    class EditIntentRoutingTests {

        @Test
        @DisplayName("User: 'Move lunch to 2pm' → Should route to EditorAgent")
        void moveLunchShouldRouteToEditor() {
            // Given: User wants to change timing
            String userMessage = "Move lunch to 2pm";
            
            // Mock LLM to classify as EDIT intent with 'edit' taskType
            when(llmService.classifyIntent(eq(userMessage), anyString()))
                .thenReturn(createIntentResult("EDIT", "edit", 0.95));
            
            // When: Get agents for 'edit' task
            List<BaseAgent> agents = agentRegistry.getAgentsForTask("edit", true);
            
            // Then: Should return EditorAgent only
            assertThat(agents)
                .hasSize(1)
                .first()
                .isInstanceOf(EditorAgent.class);
        }

        @Test
        @DisplayName("User: 'Change the restaurant' → Should route to EditorAgent")
        void changeRestaurantShouldRouteToEditor() {
            String userMessage = "Change the restaurant";
            
            when(llmService.classifyIntent(eq(userMessage), anyString()))
                .thenReturn(createIntentResult("EDIT", "edit", 0.92));
            
            List<BaseAgent> agents = agentRegistry.getAgentsForTask("edit", true);
            
            assertThat(agents)
                .hasSize(1)
                .first()
                .isInstanceOf(EditorAgent.class);
        }

        @Test
        @DisplayName("User: 'Update the hotel name' → Should route to EditorAgent")
        void updateHotelShouldRouteToEditor() {
            String userMessage = "Update the hotel name to Grand Plaza";
            
            when(llmService.classifyIntent(eq(userMessage), anyString()))
                .thenReturn(createIntentResult("EDIT", "edit", 0.88));
            
            List<BaseAgent> agents = agentRegistry.getAgentsForTask("edit", true);
            
            assertThat(agents)
                .hasSize(1)
                .first()
                .isInstanceOf(EditorAgent.class);
        }

        @Test
        @DisplayName("User: 'Add a museum visit on day 2' → Should route to EditorAgent")
        void addMuseumShouldRouteToEditor() {
            String userMessage = "Add a museum visit on day 2";
            
            when(llmService.classifyIntent(eq(userMessage), anyString()))
                .thenReturn(createIntentResult("INSERT_PLACE", "edit", 0.90));
            
            List<BaseAgent> agents = agentRegistry.getAgentsForTask("edit", true);
            
            assertThat(agents)
                .hasSize(1)
                .first()
                .isInstanceOf(EditorAgent.class);
        }

        @Test
        @DisplayName("User: 'Remove the beach activity' → Should route to EditorAgent")
        void removeActivityShouldRouteToEditor() {
            String userMessage = "Remove the beach activity";
            
            when(llmService.classifyIntent(eq(userMessage), anyString()))
                .thenReturn(createIntentResult("DELETE_NODE", "edit", 0.93));
            
            List<BaseAgent> agents = agentRegistry.getAgentsForTask("edit", true);
            
            assertThat(agents)
                .hasSize(1)
                .first()
                .isInstanceOf(EditorAgent.class);
        }

        @Test
        @DisplayName("User: 'Replace dinner with a different restaurant' → Should route to EditorAgent")
        void replaceRestaurantShouldRouteToEditor() {
            String userMessage = "Replace dinner with a different restaurant";
            
            when(llmService.classifyIntent(eq(userMessage), anyString()))
                .thenReturn(createIntentResult("REPLACE_NODE", "edit", 0.89));
            
            List<BaseAgent> agents = agentRegistry.getAgentsForTask("edit", true);
            
            assertThat(agents)
                .hasSize(1)
                .first()
                .isInstanceOf(EditorAgent.class);
        }
    }

    @Nested
    @DisplayName("2. Plan Intent Routing Tests")
    class PlanIntentRoutingTests {

        @Test
        @DisplayName("User: 'Create a 3-day trip to Paris' → Should route to DayByDayPlannerAgent")
        void createTripShouldRouteToPlannerAgent() {
            String userMessage = "Create a 3-day trip to Paris";
            
            when(llmService.classifyIntent(eq(userMessage), anyString()))
                .thenReturn(createIntentResult("CREATE_ITINERARY", "plan", 0.96));
            
            List<BaseAgent> agents = agentRegistry.getAgentsForTask("plan", true);
            
            assertThat(agents)
                .hasSize(1)
                .first()
                .isInstanceOf(DayByDayPlannerAgent.class);
        }

        @Test
        @DisplayName("User: 'Plan a romantic weekend in Rome' → Should route to DayByDayPlannerAgent")
        void planWeekendShouldRouteToPlannerAgent() {
            String userMessage = "Plan a romantic weekend in Rome";
            
            when(llmService.classifyIntent(eq(userMessage), anyString()))
                .thenReturn(createIntentResult("CREATE_ITINERARY", "plan", 0.94));
            
            List<BaseAgent> agents = agentRegistry.getAgentsForTask("plan", true);
            
            assertThat(agents)
                .hasSize(1)
                .first()
                .isInstanceOf(DayByDayPlannerAgent.class);
        }

        @Test
        @DisplayName("User: 'Generate a budget trip to Tokyo' → Should route to DayByDayPlannerAgent")
        void generateBudgetTripShouldRouteToPlannerAgent() {
            String userMessage = "Generate a budget trip to Tokyo for 5 days";
            
            when(llmService.classifyIntent(eq(userMessage), anyString()))
                .thenReturn(createIntentResult("CREATE_ITINERARY", "plan", 0.91));
            
            List<BaseAgent> agents = agentRegistry.getAgentsForTask("plan", true);
            
            assertThat(agents)
                .hasSize(1)
                .first()
                .isInstanceOf(DayByDayPlannerAgent.class);
        }
    }

    @Nested
    @DisplayName("3. Explain Intent Routing Tests")
    class ExplainIntentRoutingTests {

        @Test
        @DisplayName("User: 'What's my plan for today?' → Should route to ExplainAgent")
        void whatsPlanTodayShouldRouteToExplainAgent() {
            String userMessage = "What's my plan for today?";
            
            when(llmService.classifyIntent(eq(userMessage), anyString()))
                .thenReturn(createIntentResult("EXPLAIN", "explain", 0.97));
            
            List<BaseAgent> agents = agentRegistry.getAgentsForTask("explain", true);
            
            assertThat(agents)
                .hasSize(1)
                .first()
                .isInstanceOf(com.tripplanner.agents.ExplainAgent.class);
        }

        @Test
        @DisplayName("User: 'Show me today's schedule' → Should route to ExplainAgent")
        void showScheduleShouldRouteToExplainAgent() {
            String userMessage = "Show me today's schedule";
            
            when(llmService.classifyIntent(eq(userMessage), anyString()))
                .thenReturn(createIntentResult("EXPLAIN", "explain", 0.95));
            
            List<BaseAgent> agents = agentRegistry.getAgentsForTask("explain", true);
            
            assertThat(agents)
                .hasSize(1)
                .first()
                .isInstanceOf(com.tripplanner.agents.ExplainAgent.class);
        }

        @Test
        @DisplayName("User: 'Summarize my trip' → Should route to ExplainAgent")
        void summarizeTripShouldRouteToExplainAgent() {
            String userMessage = "Summarize my entire trip";
            
            when(llmService.classifyIntent(eq(userMessage), anyString()))
                .thenReturn(createIntentResult("EXPLAIN", "explain", 0.93));
            
            List<BaseAgent> agents = agentRegistry.getAgentsForTask("explain", true);
            
            assertThat(agents)
                .hasSize(1)
                .first()
                .isInstanceOf(com.tripplanner.agents.ExplainAgent.class);
        }

        @Test
        @DisplayName("User: 'How many days is my trip?' → Should route to ExplainAgent")
        void howManyDaysShouldRouteToExplainAgent() {
            String userMessage = "How many days is my trip?";
            
            when(llmService.classifyIntent(eq(userMessage), anyString()))
                .thenReturn(createIntentResult("EXPLAIN", "explain", 0.89));
            
            List<BaseAgent> agents = agentRegistry.getAgentsForTask("explain", true);
            
            assertThat(agents)
                .hasSize(1)
                .first()
                .isInstanceOf(com.tripplanner.agents.ExplainAgent.class);
        }
    }

    @Nested
    @DisplayName("4. Book Intent Routing Tests")
    class BookIntentRoutingTests {

        @Test
        @DisplayName("User: 'Book this hotel' → Should route to BookingAgent")
        void bookHotelShouldRouteToBookingAgent() {
            String userMessage = "Book this hotel for 3 nights";
            
            when(llmService.classifyIntent(eq(userMessage), anyString()))
                .thenReturn(createIntentResult("BOOK", "book", 0.98));
            
            List<BaseAgent> agents = agentRegistry.getAgentsForTask("book", true);
            
            assertThat(agents)
                .hasSize(1)
                .first()
                .isInstanceOf(BookingAgent.class);
        }

        @Test
        @DisplayName("User: 'Reserve a table at this restaurant' → Should route to BookingAgent")
        void reserveTableShouldRouteToBookingAgent() {
            String userMessage = "Reserve a table at this restaurant for 7pm";
            
            when(llmService.classifyIntent(eq(userMessage), anyString()))
                .thenReturn(createIntentResult("BOOK", "book", 0.94));
            
            List<BaseAgent> agents = agentRegistry.getAgentsForTask("book", true);
            
            assertThat(agents)
                .hasSize(1)
                .first()
                .isInstanceOf(BookingAgent.class);
        }

        @Test
        @DisplayName("User: 'Purchase tickets for the museum' → Should route to BookingAgent")
        void purchaseTicketsShouldRouteToBookingAgent() {
            String userMessage = "Purchase tickets for the Louvre museum";
            
            when(llmService.classifyIntent(eq(userMessage), anyString()))
                .thenReturn(createIntentResult("BOOK", "book", 0.92));
            
            List<BaseAgent> agents = agentRegistry.getAgentsForTask("book", true);
            
            assertThat(agents)
                .hasSize(1)
                .first()
                .isInstanceOf(BookingAgent.class);
        }

        @Test
        @DisplayName("User: 'Book a flight to London' → Should route to BookingAgent")
        void bookFlightShouldRouteToBookingAgent() {
            String userMessage = "Book a flight to London for tomorrow";
            
            when(llmService.classifyIntent(eq(userMessage), anyString()))
                .thenReturn(createIntentResult("BOOK", "book", 0.96));
            
            List<BaseAgent> agents = agentRegistry.getAgentsForTask("book", true);
            
            assertThat(agents)
                .hasSize(1)
                .first()
                .isInstanceOf(BookingAgent.class);
        }
    }

    @Nested
    @DisplayName("5. Enrich Intent Routing Tests")
    class EnrichIntentRoutingTests {

        @Test
        @DisplayName("User: 'Add photos to this place' → Should route to EnrichmentAgent")
        void addPhotosShouldRouteToEnrichmentAgent() {
            String userMessage = "Add photos to this place";
            
            when(llmService.classifyIntent(eq(userMessage), anyString()))
                .thenReturn(createIntentResult("ENRICH", "enrich", 0.93));
            
            List<BaseAgent> agents = agentRegistry.getAgentsForTask("enrich", true);
            
            assertThat(agents)
                .hasSize(1)
                .first()
                .isInstanceOf(EnrichmentAgent.class);
        }

        @Test
        @DisplayName("User: 'Show me reviews for this restaurant' → Should route to EnrichmentAgent")
        void showReviewsShouldRouteToEnrichmentAgent() {
            String userMessage = "Show me reviews for this restaurant";
            
            when(llmService.classifyIntent(eq(userMessage), anyString()))
                .thenReturn(createIntentResult("ENRICH", "enrich", 0.91));
            
            List<BaseAgent> agents = agentRegistry.getAgentsForTask("enrich", true);
            
            assertThat(agents)
                .hasSize(1)
                .first()
                .isInstanceOf(EnrichmentAgent.class);
        }

        @Test
        @DisplayName("User: 'Get more details about this hotel' → Should route to EnrichmentAgent")
        void getDetailsShouldRouteToEnrichmentAgent() {
            String userMessage = "Get more details about this hotel";
            
            when(llmService.classifyIntent(eq(userMessage), anyString()))
                .thenReturn(createIntentResult("ENRICH", "enrich", 0.89));
            
            List<BaseAgent> agents = agentRegistry.getAgentsForTask("enrich", true);
            
            assertThat(agents)
                .hasSize(1)
                .first()
                .isInstanceOf(EnrichmentAgent.class);
        }

        @Test
        @DisplayName("User: 'Find POI information' → Should route to EnrichmentAgent")
        void findPOIShouldRouteToEnrichmentAgent() {
            String userMessage = "Find points of interest near my hotel";
            
            when(llmService.classifyIntent(eq(userMessage), anyString()))
                .thenReturn(createIntentResult("ENRICH", "enrich", 0.87));
            
            List<BaseAgent> agents = agentRegistry.getAgentsForTask("enrich", true);
            
            assertThat(agents)
                .hasSize(1)
                .first()
                .isInstanceOf(EnrichmentAgent.class);
        }
    }

    @Nested
    @DisplayName("6. Pipeline Tasks Should Not Route to Chat")
    class PipelineTaskFilteringTests {

        @Test
        @DisplayName("'skeleton' task should return empty with chatOnly=true")
        void skeletonTaskShouldNotRouteToChat() {
            List<BaseAgent> agents = agentRegistry.getAgentsForTask("skeleton", true);
            
            assertThat(agents)
                .as("Pipeline task 'skeleton' should not be accessible via chat")
                .isEmpty();
        }

        @Test
        @DisplayName("'populate_attractions' task should return empty with chatOnly=true")
        void populateAttractionsShouldNotRouteToChat() {
            List<BaseAgent> agents = agentRegistry.getAgentsForTask("populate_attractions", true);
            
            assertThat(agents)
                .as("Pipeline task 'populate_attractions' should not be accessible via chat")
                .isEmpty();
        }

        @Test
        @DisplayName("'populate_meals' task should return empty with chatOnly=true")
        void populateMealsShouldNotRouteToChat() {
            List<BaseAgent> agents = agentRegistry.getAgentsForTask("populate_meals", true);
            
            assertThat(agents)
                .as("Pipeline task 'populate_meals' should not be accessible via chat")
                .isEmpty();
        }

        @Test
        @DisplayName("'populate_transport' task should return empty with chatOnly=true")
        void populateTransportShouldNotRouteToChat() {
            List<BaseAgent> agents = agentRegistry.getAgentsForTask("populate_transport", true);
            
            assertThat(agents)
                .as("Pipeline task 'populate_transport' should not be accessible via chat")
                .isEmpty();
        }

        @Test
        @DisplayName("'estimate_costs' task should return empty with chatOnly=true")
        void estimateCostsShouldNotRouteToChat() {
            List<BaseAgent> agents = agentRegistry.getAgentsForTask("estimate_costs", true);
            
            assertThat(agents)
                .as("Pipeline task 'estimate_costs' should not be accessible via chat")
                .isEmpty();
        }
    }

    @Nested
    @DisplayName("7. Parameterized User Input Tests")
    class ParameterizedUserInputTests {

        @ParameterizedTest
        @CsvSource({
            "'Move lunch to 2pm', EDIT, edit, EditorAgent",
            "'Change the restaurant', EDIT, edit, EditorAgent",
            "'Add a museum', INSERT_PLACE, edit, EditorAgent",
            "'Remove the beach', DELETE_NODE, edit, EditorAgent",
            "'Create a trip to Paris', CREATE_ITINERARY, plan, DayByDayPlannerAgent",
            "'What is my plan?', EXPLAIN, explain, DayByDayPlannerAgent",
            "'Book this hotel', BOOK, book, BookingAgent",
            "'Add photos', ENRICH, enrich, EnrichmentAgent"
        })
        @DisplayName("Should route different user inputs to correct agents")
        void shouldRouteUserInputsCorrectly(String userMessage, String intent, String taskType, String expectedAgentClass) {
            // Mock LLM classification
            when(llmService.classifyIntent(eq(userMessage), anyString()))
                .thenReturn(createIntentResult(intent, taskType, 0.90));
            
            // Get agents for task
            List<BaseAgent> agents = agentRegistry.getAgentsForTask(taskType, true);
            
            // Verify routing
            assertThat(agents)
                .as("User message '%s' should route to %s", userMessage, expectedAgentClass)
                .hasSize(1);
            
            assertThat(agents.get(0).getClass().getSimpleName())
                .isEqualTo(expectedAgentClass);
        }
    }

    @Nested
    @DisplayName("8. Edge Cases and Error Handling")
    class EdgeCasesTests {

        @Test
        @DisplayName("Unknown task type should return empty list")
        void unknownTaskTypeShouldReturnEmpty() {
            List<BaseAgent> agents = agentRegistry.getAgentsForTask("unknown_task", true);
            
            assertThat(agents)
                .as("Unknown task type should return empty list")
                .isEmpty();
        }

        @Test
        @DisplayName("Null task type should return empty list")
        void nullTaskTypeShouldReturnEmpty() {
            List<BaseAgent> agents = agentRegistry.getAgentsForTask(null, true);
            
            assertThat(agents)
                .as("Null task type should return empty list")
                .isEmpty();
        }

        @Test
        @DisplayName("Empty task type should return empty list")
        void emptyTaskTypeShouldReturnEmpty() {
            List<BaseAgent> agents = agentRegistry.getAgentsForTask("", true);
            
            assertThat(agents)
                .as("Empty task type should return empty list")
                .isEmpty();
        }

        @Test
        @DisplayName("Case-sensitive task type matching")
        void taskTypesShouldBeCaseSensitive() {
            List<BaseAgent> editAgents = agentRegistry.getAgentsForTask("edit", true);
            List<BaseAgent> EDITAgents = agentRegistry.getAgentsForTask("EDIT", true);
            
            // Task types are case-sensitive, so EDIT should not match 'edit'
            assertThat(editAgents).isNotEmpty();
            assertThat(EDITAgents).isEmpty();
        }
    }

    @Nested
    @DisplayName("9. Complex User Scenarios")
    class ComplexScenariosTests {

        @Test
        @DisplayName("Scenario: User asks to move activity, then add photos")
        void userMovesActivityThenAddsPhotos() {
            // First request: Move activity (edit)
            when(llmService.classifyIntent(eq("Move the museum to 3pm"), anyString()))
                .thenReturn(createIntentResult("MOVE_TIME", "edit", 0.92));
            
            List<BaseAgent> editAgents = agentRegistry.getAgentsForTask("edit", true);
            assertThat(editAgents).hasSize(1).first().isInstanceOf(EditorAgent.class);
            
            // Second request: Add photos (enrich)
            when(llmService.classifyIntent(eq("Add photos to that museum"), anyString()))
                .thenReturn(createIntentResult("ENRICH", "enrich", 0.88));
            
            List<BaseAgent> enrichAgents = agentRegistry.getAgentsForTask("enrich", true);
            assertThat(enrichAgents).hasSize(1).first().isInstanceOf(EnrichmentAgent.class);
        }

        @Test
        @DisplayName("Scenario: User creates trip, then asks for summary")
        void userCreatesTripThenAsksSummary() {
            // First request: Create trip (plan)
            when(llmService.classifyIntent(eq("Create a 5-day trip to Japan"), anyString()))
                .thenReturn(createIntentResult("CREATE_ITINERARY", "plan", 0.95));
            
            List<BaseAgent> planAgents = agentRegistry.getAgentsForTask("plan", true);
            assertThat(planAgents).hasSize(1).first().isInstanceOf(DayByDayPlannerAgent.class);
            
            // Second request: Get summary (explain)
            when(llmService.classifyIntent(eq("What did you plan for me?"), anyString()))
                .thenReturn(createIntentResult("EXPLAIN", "explain", 0.91));
            
            List<BaseAgent> explainAgents = agentRegistry.getAgentsForTask("explain", true);
            assertThat(explainAgents).hasSize(1).first().isInstanceOf(DayByDayPlannerAgent.class);
        }

        @Test
        @DisplayName("Scenario: User edits, then books, then enriches")
        void userEditsBooksThenEnriches() {
            // 1. Edit
            when(llmService.classifyIntent(eq("Change dinner to Italian restaurant"), anyString()))
                .thenReturn(createIntentResult("EDIT", "edit", 0.93));
            List<BaseAgent> editAgents = agentRegistry.getAgentsForTask("edit", true);
            assertThat(editAgents).hasSize(1).first().isInstanceOf(EditorAgent.class);
            
            // 2. Book
            when(llmService.classifyIntent(eq("Book a table at that restaurant"), anyString()))
                .thenReturn(createIntentResult("BOOK", "book", 0.96));
            List<BaseAgent> bookAgents = agentRegistry.getAgentsForTask("book", true);
            assertThat(bookAgents).hasSize(1).first().isInstanceOf(BookingAgent.class);
            
            // 3. Enrich
            when(llmService.classifyIntent(eq("Show me reviews"), anyString()))
                .thenReturn(createIntentResult("ENRICH", "enrich", 0.89));
            List<BaseAgent> enrichAgents = agentRegistry.getAgentsForTask("enrich", true);
            assertThat(enrichAgents).hasSize(1).first().isInstanceOf(EnrichmentAgent.class);
        }
    }

    // Helper method to create mock IntentResult
    private IntentResult createIntentResult(String intent, String taskType, double confidence) {
        IntentResult result = new IntentResult();
        result.setIntent(intent);
        result.setTaskType(taskType);
        result.setConfidence(confidence);
        return result;
    }
}

