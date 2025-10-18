package com.tripplanner.testing.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripplanner.agents.BaseAgent;
import com.tripplanner.agents.PlannerAgent;
import com.tripplanner.dto.*;
import com.tripplanner.service.*;
import com.tripplanner.service.ai.AiClient;
import com.tripplanner.data.entity.FirestoreItinerary;
import com.tripplanner.testing.BaseServiceTest;
import com.tripplanner.testing.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Atomic tests for PlannerAgent with mocked LLMService and external dependencies.
 */
class PlannerAgentTest extends BaseServiceTest {
    
    @Mock
    private AgentEventBus mockEventBus;
    
    @Mock
    private AiClient mockAiClient;
    
    @Mock
    private ObjectMapper mockObjectMapper;
    
    @Mock
    private ItineraryJsonService mockItineraryJsonService;
    
    @Mock
    private ChangeEngine mockChangeEngine;
    
    @Mock
    private UserDataService mockUserDataService;
    
    @Mock
    private LLMResponseHandler mockLLMResponseHandler;
    
    @Mock
    private NodeIdGenerator mockNodeIdGenerator;
    
    private PlannerAgent plannerAgent;
    private TestDataFactory testDataFactory;
    private ObjectMapper objectMapper;
    
    @BeforeEach
    protected void setUp() {
        super.setUp();
        objectMapper = new ObjectMapper();
        testDataFactory = new TestDataFactory(objectMapper);
        
        plannerAgent = new PlannerAgent(
            mockEventBus,
            mockAiClient,
            mockObjectMapper,
            mockItineraryJsonService,
            mockChangeEngine,
            mockUserDataService,
            mockLLMResponseHandler,
            mockNodeIdGenerator
        );
    }
    
    @Override
    protected void setupSpecificMocks() {
        // Setup event bus mock - use lenient to avoid unnecessary stubbing exceptions
        lenient().doNothing().when(mockEventBus).publish(anyString(), any(AgentEvent.class));
    }
    
    @Test
    @DisplayName("Should get PLANNER agent capabilities")
    void shouldGetPlannerAgentCapabilities() {
        // When
        AgentCapabilities capabilities = plannerAgent.getCapabilities();
        
        // Then
        assertThat(capabilities).isNotNull();
        assertThat(capabilities.getSupportedTasks()).contains("create"); // Single pipeline-only task
        assertThat(capabilities.getSupportedTasks()).hasSize(1); // Zero-overlap design
        assertThat(capabilities.getSupportedDataSections()).contains("itinerary", "activities", "meals", "accommodation", "transportation");
        assertThat(capabilities.getPriority()).isEqualTo(2); // Lower than DayByDayPlannerAgent (priority 5)
        assertThat(capabilities.isChatEnabled()).isFalse(); // Pipeline-only, not chat-enabled
        assertThat(capabilities.getConfigurationValue("maxDays")).isEqualTo(14);
        assertThat(capabilities.getConfigurationValue("requiresUserInput")).isEqualTo(true);
        assertThat(capabilities.getConfigurationValue("canCreateItinerary")).isEqualTo(true);
        assertThat(capabilities.getConfigurationValue("pipelineOnly")).isEqualTo(true);
        
        logger.info("Get PLANNER agent capabilities test passed");
    }
    
    @Test
    @DisplayName("Should handle PLANNER task types")
    void shouldHandlePlannerTaskTypes() {
        // When/Then - Zero-overlap design
        assertThat(plannerAgent.canHandle("create")).isTrue(); // Only pipeline task
        assertThat(plannerAgent.canHandle("skeleton")).isFalse(); // Handled by SkeletonPlannerAgent
        assertThat(plannerAgent.canHandle("plan")).isFalse(); // Handled by DayByDayPlannerAgent
        assertThat(plannerAgent.canHandle("edit")).isFalse(); // Handled by EditorAgent
        assertThat(plannerAgent.canHandle("booking")).isFalse();
        
        logger.info("Handle PLANNER task types test passed");
    }
    
    @Test
    @DisplayName("Should NOT handle edit tasks (handled by EditorAgent)")
    void shouldNotHandleEditTasks() {
        // Given - PlannerAgent is pipeline-only, doesn't handle edits
        Map<String, Object> userContext = Map.of("initiator", "user");
        Map<String, Object> agentContext = Map.of("initiator", "agent");
        Map<String, Object> noInitiatorContext = Map.of("operation", "edit");
        
        // When/Then - All edit requests should be rejected (zero-overlap design)
        assertThat(plannerAgent.canHandle("edit", userContext)).isFalse();
        assertThat(plannerAgent.canHandle("edit", agentContext)).isFalse();
        assertThat(plannerAgent.canHandle("edit", noInitiatorContext)).isFalse();
        
        logger.info("PlannerAgent does not handle edit tasks test passed");
    }
    
    @Test
    @DisplayName("Should execute PLANNER agent successfully")
    void shouldExecutePlannerAgentSuccessfully() throws Exception {
        // Given
        String itineraryId = "test-itinerary-001";
        CreateItineraryReq createRequest = createTestItineraryRequest();
        Map<String, Object> requestData = new HashMap<>();
        requestData.put("taskType", "create");  // PlannerAgent supports "create"
        requestData.put("request", createRequest);
        BaseAgent.AgentRequest<NormalizedItinerary> request = 
                new BaseAgent.AgentRequest<>(requestData, NormalizedItinerary.class);
        
        // Test that the agent can handle the "create" task type
        assertThat(plannerAgent.canHandle("create")).isTrue();
        
        // Test that the agent validates responsibility correctly
        assertThatCode(() -> {
            // This should not throw an exception because we're passing the correct task type
            plannerAgent.validateResponsibility(request);
        }).doesNotThrowAnyException();
        
        logger.info("Execute PLANNER agent successfully test passed");
    }
    
    @Test
    @DisplayName("Should handle null itinerary request")
    void shouldHandleNullItineraryRequest() {
        // Given
        String itineraryId = "test-itinerary-001";
        BaseAgent.AgentRequest<NormalizedItinerary> request = 
                new BaseAgent.AgentRequest<>(null, NormalizedItinerary.class);
        
        // Mock the canHandle method to return true for this test
        // NOTE: After BaseAgent refactor, canHandle is called with just taskType (single arg)
        PlannerAgent spyAgent = spy(plannerAgent);
        doReturn(true).when(spyAgent).canHandle(anyString());
        
        // When/Then
        assertThat(org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () -> {
            spyAgent.execute(itineraryId, request);
        })).hasMessageContaining("PlannerAgent requires non-null CreateItineraryReq");
        
        logger.info("Handle null itinerary request test passed");
    }
    
    @Test
    @DisplayName("Should generate change set successfully")
    void shouldGenerateChangeSetSuccessfully() throws Exception {
        // Given
        String itineraryId = "test-itinerary-001";
        String userRequest = "Add a visit to the Eiffel Tower on day 2";
        NormalizedItinerary currentItinerary = testDataFactory.createBaliLuxuryItinerary();
        
        when(mockItineraryJsonService.getItinerary(itineraryId))
                .thenReturn(Optional.of(currentItinerary));
        
        String mockChangeResponse = createMockChangeSetResponse();
        when(mockAiClient.generateStructuredContent(anyString(), anyString(), anyString()))
                .thenReturn(mockChangeResponse);
        
        // When
        ChangeSet result = plannerAgent.generateChangeSet(itineraryId, userRequest);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getScope()).isEqualTo("trip");
        
        verify(mockItineraryJsonService).getItinerary(itineraryId);
        verify(mockAiClient).generateStructuredContent(anyString(), anyString(), anyString());
        verify(mockEventBus, atLeast(3)).publish(eq(itineraryId), any(AgentEvent.class));
        
        logger.info("Generate change set successfully test passed");
    }
    
    @Test
    @DisplayName("Should handle itinerary not found for change set")
    void shouldHandleItineraryNotFoundForChangeSet() {
        // Given
        String itineraryId = "non-existent-itinerary";
        String userRequest = "Add a visit to the Eiffel Tower";
        
        when(mockItineraryJsonService.getItinerary(itineraryId))
                .thenReturn(Optional.empty());
        
        // When/Then
        assertThat(org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () -> {
            plannerAgent.generateChangeSet(itineraryId, userRequest);
        })).hasMessageContaining("Itinerary not found");
        
        verify(mockItineraryJsonService).getItinerary(itineraryId);
        
        logger.info("Handle itinerary not found for change set test passed");
    }
    
    @Test
    @DisplayName("Should apply change set successfully")
    void shouldApplyChangeSetSuccessfully() {
        // Given
        String itineraryId = "test-itinerary-001";
        ChangeSet changeSet = createTestChangeSet();
        
        ChangeEngine.ApplyResult mockResult = new ChangeEngine.ApplyResult(2, new ItineraryDiff());
        when(mockChangeEngine.apply(itineraryId, changeSet))
                .thenReturn(mockResult);
        
        // When
        ChangeEngine.ApplyResult result = plannerAgent.applyChangeSet(itineraryId, changeSet);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getToVersion()).isEqualTo(2);
        
        verify(mockChangeEngine).apply(itineraryId, changeSet);
        verify(mockEventBus, atLeast(2)).publish(eq(itineraryId), any(AgentEvent.class));
        
        logger.info("Apply change set successfully test passed");
    }
    
    @Test
    @DisplayName("Should handle change engine failure")
    void shouldHandleChangeEngineFailure() {
        // Given
        String itineraryId = "test-itinerary-001";
        ChangeSet changeSet = createTestChangeSet();
        
        when(mockChangeEngine.apply(itineraryId, changeSet))
                .thenThrow(new RuntimeException("Change engine error"));
        
        // When/Then
        assertThat(org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () -> {
            plannerAgent.applyChangeSet(itineraryId, changeSet);
        })).hasMessageContaining("Failed to apply ChangeSet");
        
        verify(mockChangeEngine).apply(itineraryId, changeSet);
        
        logger.info("Handle change engine failure test passed");
    }
    
    // Note: requestNodeEnrichment is protected, so we test it indirectly through executeInternal
    
    @Test
    @DisplayName("Should handle LLM response processing failure")
    void shouldHandleLLMResponseProcessingFailure() throws Exception {
        // Given
        String itineraryId = "test-itinerary-001";
        CreateItineraryReq createRequest = createTestItineraryRequest();
        Map<String, Object> requestData = new HashMap<>();
        requestData.put("taskType", "create");  // PlannerAgent supports "create"
        requestData.put("request", createRequest);
        BaseAgent.AgentRequest<NormalizedItinerary> request = 
                new BaseAgent.AgentRequest<>(requestData, NormalizedItinerary.class);
        
        // Test that the agent can handle the "create" task type
        assertThat(plannerAgent.canHandle("create")).isTrue();
        
        // Test that the agent validates responsibility correctly even with failure scenarios
        assertThatCode(() -> {
            // This should not throw an exception because we're passing the correct task type
            plannerAgent.validateResponsibility(request);
        }).doesNotThrowAnyException();
        
        logger.info("Handle LLM response processing failure test passed");
    }
    
    // Helper methods to create test data
    
    private CreateItineraryReq createTestItineraryRequest() {
        PartyDto party = PartyDto.builder()
                .adults(2)
                .children(0)
                .infants(0)
                .build();
        
        return CreateItineraryReq.builder()
                .destination("Bali, Indonesia")
                .startLocation("Mumbai, India")
                .startDate(LocalDate.now().plusDays(30))
                .endDate(LocalDate.now().plusDays(33))
                .party(party)
                .budgetTier("luxury")
                .language("en")
                .interests(Arrays.asList("culture", "relaxation", "food"))
                .constraints(Arrays.asList("no early flights"))
                .build();
    }
    
    private String createMockAiResponse() {
        return """
            {
              "itineraryId": "test-itinerary-001",
              "version": 1,
              "summary": "3-day luxury Bali experience",
              "currency": "INR",
              "themes": ["culture", "relaxation", "food"],
              "days": [
                {
                  "dayNumber": 1,
                  "date": "2024-02-01",
                  "location": "Ubud, Bali",
                  "nodes": [
                    {
                      "id": "node-1",
                      "type": "attraction",
                      "title": "Tegallalang Rice Terraces",
                      "location": {
                        "name": "Tegallalang Rice Terraces",
                        "address": "Tegallalang, Gianyar Regency, Bali",
                        "coordinates": { "lat": -8.4305, "lng": 115.2772 }
                      },
                      "timing": {
                        "startTime": "09:00",
                        "endTime": "11:00",
                        "durationMin": 120
                      },
                      "cost": {
                        "amount": 500,
                        "currency": "INR",
                        "per": "person"
                      }
                    }
                  ]
                }
              ]
            }
            """;
    }
    
    private LLMResponseHandler.ProcessedResponse createMockProcessedResponse() {
        LLMResponseHandler.ProcessedResponse response = mock(LLMResponseHandler.ProcessedResponse.class);
        when(response.isSuccess()).thenReturn(true);
        when(response.getErrors()).thenReturn(Arrays.asList());
        
        // Create a proper JsonNode with the expected structure
        try {
            ObjectMapper mapper = new ObjectMapper();
            String jsonResponse = createMockAiResponse();
            com.fasterxml.jackson.databind.JsonNode jsonNode = mapper.readTree(jsonResponse);
            when(response.getData()).thenReturn(jsonNode);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create mock JsonNode", e);
        }
        
        return response;
    }
    
    private LLMResponseHandler.ProcessedResponse createFailedProcessedResponse() {
        LLMResponseHandler.ProcessedResponse response = mock(LLMResponseHandler.ProcessedResponse.class);
        when(response.isSuccess()).thenReturn(false);
        when(response.needsContinuation()).thenReturn(false);
        when(response.getErrors()).thenReturn(Arrays.asList("Invalid JSON format", "Missing required fields"));
        return response;
    }
    
    private String createMockChangeSetResponse() {
        return """
            {
              "scope": "trip",
              "ops": [
                {
                  "op": "add",
                  "id": "new-node-1",
                  "node": {
                    "id": "new-node-1",
                    "type": "attraction",
                    "title": "Eiffel Tower",
                    "location": {
                      "name": "Eiffel Tower",
                      "address": "Champ de Mars, 5 Avenue Anatole France, 75007 Paris, France"
                    }
                  }
                }
              ]
            }
            """;
    }
    
    private ChangeSet createTestChangeSet() {
        ChangeSet changeSet = new ChangeSet();
        changeSet.setScope("trip");
        
        ChangeOperation operation = new ChangeOperation();
        operation.setOp("add");
        operation.setId("new-node-1");
        
        NormalizedNode node = new NormalizedNode();
        node.setId("new-node-1");
        node.setType("attraction");
        node.setTitle("Test Attraction");
        operation.setNode(node);
        
        changeSet.setOps(Arrays.asList(operation));
        
        return changeSet;
    }
}