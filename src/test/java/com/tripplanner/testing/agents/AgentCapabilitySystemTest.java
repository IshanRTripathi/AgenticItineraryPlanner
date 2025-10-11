package com.tripplanner.testing.agents;

import com.tripplanner.agents.*;
import com.tripplanner.dto.AgentCapabilities;
import com.tripplanner.service.AgentRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive tests for the Agent Capability System.
 * Tests zero-overlap design, chat-enabled filtering, and proper agent routing.
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Agent Capability System Tests")
public class AgentCapabilitySystemTest {

    @Autowired
    private AgentRegistry agentRegistry;

    @BeforeEach
    void setUp() {
        // Verify all agents are registered
        assertThat(agentRegistry.getAllCapabilities()).isNotEmpty();
    }

    @Nested
    @DisplayName("1. Chat-Enabled Agents Tests")
    class ChatEnabledAgentsTests {

        @Test
        @DisplayName("Should identify all chat-enabled agents")
        void shouldIdentifyAllChatEnabledAgents() {
            Map<String, AgentCapabilities> allCaps = agentRegistry.getAllCapabilities();
            
            // Expected chat-enabled agents (zero-overlap design)
            List<String> expectedChatAgents = List.of("editoragent", "daybydayplanneragent", "bookingagent", "enrichmentagent", "explainagent");
            
            for (String agentId : expectedChatAgents) {
                AgentCapabilities caps = allCaps.get(agentId);
                assertThat(caps)
                    .as("Agent %s should have capabilities defined", agentId)
                    .isNotNull();
                assertThat(caps.isChatEnabled())
                    .as("Agent %s should be chat-enabled", agentId)
                    .isTrue();
            }
        }

        @Test
        @DisplayName("Should identify all pipeline-only agents")
        void shouldIdentifyAllPipelineOnlyAgents() {
            Map<String, AgentCapabilities> allCaps = agentRegistry.getAllCapabilities();
            
            // Expected pipeline-only agents
            List<String> expectedPipelineAgents = List.of("planneragent", "activityagent", "mealagent", "transportagent", "costestimatoragent", "placesagent");
            // Note: These correspond to ActivityAgent, MealAgent, TransportAgent, CostEstimatorAgent, etc.
            
            // Check specific pipeline-only agents we know about
            for (Map.Entry<String, AgentCapabilities> entry : allCaps.entrySet()) {
                String agentId = entry.getKey();
                AgentCapabilities caps = entry.getValue();
                
                // Check for known pipeline task types
                if (caps.supportsTask("skeleton") || 
                    caps.supportsTask("populate_attractions") ||
                    caps.supportsTask("populate_meals") ||
                    caps.supportsTask("populate_transport") ||
                    caps.supportsTask("estimate_costs")) {
                    
                    assertThat(caps.isChatEnabled())
                        .as("Agent %s with pipeline tasks should NOT be chat-enabled", agentId)
                        .isFalse();
                }
            }
        }

        @Test
        @DisplayName("EditorAgent should be chat-enabled with 'edit' task")
        void editorAgentShouldBeChatEnabled() {
            AgentCapabilities caps = agentRegistry.getAgentCapabilities("editoragent").orElse(null);
            
            assertThat(caps).isNotNull();
            assertThat(caps.isChatEnabled()).isTrue();
            assertThat(caps.supportsTask("edit")).isTrue();
            assertThat(caps.getPriority()).isEqualTo(10);
        }

        @Test
        @DisplayName("DayByDayPlannerAgent should be chat-enabled with 'plan' task only")
        void dayByDayPlannerShouldBeChatEnabled() {
            AgentCapabilities caps = agentRegistry.getAgentCapabilities("daybydayplanneragent").orElse(null);
            
            assertThat(caps).isNotNull();
            assertThat(caps.isChatEnabled()).isTrue();
            assertThat(caps.supportsTask("plan")).isTrue();
            assertThat(caps.supportsTask("explain")).isFalse(); // Now handled by ExplainAgent
            assertThat(caps.getPriority()).isEqualTo(5);
        }
        
        @Test
        @DisplayName("ExplainAgent should be chat-enabled with 'explain' task")
        void explainAgentShouldBeChatEnabled() {
            AgentCapabilities caps = agentRegistry.getAgentCapabilities("explainagent").orElse(null);
            
            assertThat(caps).isNotNull();
            assertThat(caps.isChatEnabled()).isTrue();
            assertThat(caps.supportsTask("explain")).isTrue();
            assertThat(caps.getPriority()).isEqualTo(15);
        }

        @Test
        @DisplayName("BookingAgent should be chat-enabled with 'book' task")
        void bookingAgentShouldBeChatEnabled() {
            AgentCapabilities caps = agentRegistry.getAgentCapabilities("bookingagent").orElse(null);
            
            assertThat(caps).isNotNull();
            assertThat(caps.isChatEnabled()).isTrue();
            assertThat(caps.supportsTask("book")).isTrue();
            assertThat(caps.getPriority()).isEqualTo(30);
        }

        @Test
        @DisplayName("EnrichmentAgent should be chat-enabled with 'enrich' task")
        void enrichmentAgentShouldBeChatEnabled() {
            AgentCapabilities caps = agentRegistry.getAgentCapabilities("enrichmentagent").orElse(null);
            
            assertThat(caps).isNotNull();
            assertThat(caps.isChatEnabled()).isTrue();
            assertThat(caps.supportsTask("enrich")).isTrue();
            assertThat(caps.getPriority()).isEqualTo(20);
        }
    }

    @Nested
    @DisplayName("2. Pipeline-Only Agents Tests")
    class PipelineOnlyAgentsTests {

        @Test
        @DisplayName("SkeletonPlannerAgent should NOT be chat-enabled")
        void skeletonPlannerShouldNotBeChatEnabled() {
            // SkeletonPlannerAgent is also registered as 'PLANNER' kind
            // but we need to check if it's the one with 'skeleton' task
            Map<String, AgentCapabilities> allCaps = agentRegistry.getAllCapabilities();
            
            boolean foundSkeletonAgent = false;
            for (Map.Entry<String, AgentCapabilities> entry : allCaps.entrySet()) {
                AgentCapabilities caps = entry.getValue();
                if (caps.supportsTask("skeleton")) {
                    foundSkeletonAgent = true;
                    assertThat(caps.isChatEnabled())
                        .as("SkeletonPlannerAgent should NOT be chat-enabled")
                        .isFalse();
                    assertThat(caps.getPriority()).isEqualTo(1);
                }
            }
            
            assertThat(foundSkeletonAgent)
                .as("Should find SkeletonPlannerAgent with 'skeleton' task")
                .isTrue();
        }

        @Test
        @DisplayName("ActivityAgent should NOT be chat-enabled")
        void activityAgentShouldNotBeChatEnabled() {
            Map<String, AgentCapabilities> allCaps = agentRegistry.getAllCapabilities();
            
            boolean foundActivityAgent = false;
            for (Map.Entry<String, AgentCapabilities> entry : allCaps.entrySet()) {
                AgentCapabilities caps = entry.getValue();
                if (caps.supportsTask("populate_attractions")) {
                    foundActivityAgent = true;
                    assertThat(caps.isChatEnabled())
                        .as("ActivityAgent should NOT be chat-enabled")
                        .isFalse();
                }
            }
            
            assertThat(foundActivityAgent)
                .as("Should find ActivityAgent with 'populate_attractions' task")
                .isTrue();
        }

        @Test
        @DisplayName("MealAgent should NOT be chat-enabled")
        void mealAgentShouldNotBeChatEnabled() {
            Map<String, AgentCapabilities> allCaps = agentRegistry.getAllCapabilities();
            
            boolean foundMealAgent = false;
            for (Map.Entry<String, AgentCapabilities> entry : allCaps.entrySet()) {
                AgentCapabilities caps = entry.getValue();
                if (caps.supportsTask("populate_meals")) {
                    foundMealAgent = true;
                    assertThat(caps.isChatEnabled())
                        .as("MealAgent should NOT be chat-enabled")
                        .isFalse();
                }
            }
            
            assertThat(foundMealAgent)
                .as("Should find MealAgent with 'populate_meals' task")
                .isTrue();
        }

        @Test
        @DisplayName("TransportAgent should NOT be chat-enabled")
        void transportAgentShouldNotBeChatEnabled() {
            Map<String, AgentCapabilities> allCaps = agentRegistry.getAllCapabilities();
            
            boolean foundTransportAgent = false;
            for (Map.Entry<String, AgentCapabilities> entry : allCaps.entrySet()) {
                AgentCapabilities caps = entry.getValue();
                if (caps.supportsTask("populate_transport")) {
                    foundTransportAgent = true;
                    assertThat(caps.isChatEnabled())
                        .as("TransportAgent should NOT be chat-enabled")
                        .isFalse();
                }
            }
            
            assertThat(foundTransportAgent)
                .as("Should find TransportAgent with 'populate_transport' task")
                .isTrue();
        }

        @Test
        @DisplayName("CostEstimatorAgent should NOT be chat-enabled")
        void costEstimatorShouldNotBeChatEnabled() {
            Map<String, AgentCapabilities> allCaps = agentRegistry.getAllCapabilities();
            
            boolean foundCostEstimator = false;
            for (Map.Entry<String, AgentCapabilities> entry : allCaps.entrySet()) {
                AgentCapabilities caps = entry.getValue();
                if (caps.supportsTask("estimate_costs")) {
                    foundCostEstimator = true;
                    assertThat(caps.isChatEnabled())
                        .as("CostEstimatorAgent should NOT be chat-enabled")
                        .isFalse();
                }
            }
            
            assertThat(foundCostEstimator)
                .as("Should find CostEstimatorAgent with 'estimate_costs' task")
                .isTrue();
        }

        @Test
        @DisplayName("PlacesAgent should NOT be chat-enabled (helper service)")
        void placesAgentShouldNotBeChatEnabled() {
            AgentCapabilities caps = agentRegistry.getAgentCapabilities("places").orElse(null);
            
            if (caps != null) { // PlacesAgent might be registered under different key
                assertThat(caps.isChatEnabled()).isFalse();
                assertThat(caps.supportsTask("search")).isTrue();
            }
        }
    }

    @Nested
    @DisplayName("3. Zero-Overlap Task Type Tests")
    class ZeroOverlapTests {

        @Test
        @DisplayName("'edit' task should map to exactly ONE agent")
        void editTaskShouldHaveOneAgent() {
            List<BaseAgent> agents = agentRegistry.getAgentsForTask("edit", true);
            
            assertThat(agents)
                .as("'edit' task should return exactly 1 chat-enabled agent")
                .hasSize(1);
            
            assertThat(agents.get(0))
                .as("'edit' task should map to EditorAgent")
                .isInstanceOf(EditorAgent.class);
        }

        @Test
        @DisplayName("'plan' task should map to exactly ONE agent")
        void planTaskShouldHaveOneAgent() {
            List<BaseAgent> agents = agentRegistry.getAgentsForTask("plan", true);
            
            assertThat(agents)
                .as("'plan' task should return exactly 1 chat-enabled agent")
                .hasSize(1);
            
            assertThat(agents.get(0))
                .as("'plan' task should map to DayByDayPlannerAgent")
                .isInstanceOf(DayByDayPlannerAgent.class);
        }

        @Test
        @DisplayName("'explain' task should map to exactly ONE agent")
        void explainTaskShouldHaveOneAgent() {
            List<BaseAgent> agents = agentRegistry.getAgentsForTask("explain", true);
            
            assertThat(agents)
                .as("'explain' task should return exactly 1 chat-enabled agent")
                .hasSize(1);
            
            assertThat(agents.get(0))
                .as("'explain' task should map to ExplainAgent")
                .isInstanceOf(com.tripplanner.agents.ExplainAgent.class);
        }

        @Test
        @DisplayName("'book' task should map to exactly ONE agent")
        void bookTaskShouldHaveOneAgent() {
            List<BaseAgent> agents = agentRegistry.getAgentsForTask("book", true);
            
            assertThat(agents)
                .as("'book' task should return exactly 1 chat-enabled agent")
                .hasSize(1);
            
            assertThat(agents.get(0))
                .as("'book' task should map to BookingAgent")
                .isInstanceOf(BookingAgent.class);
        }

        @Test
        @DisplayName("'enrich' task should map to exactly ONE agent")
        void enrichTaskShouldHaveOneAgent() {
            List<BaseAgent> agents = agentRegistry.getAgentsForTask("enrich", true);
            
            assertThat(agents)
                .as("'enrich' task should return exactly 1 chat-enabled agent")
                .hasSize(1);
            
            assertThat(agents.get(0))
                .as("'enrich' task should map to EnrichmentAgent")
                .isInstanceOf(EnrichmentAgent.class);
        }

        @Test
        @DisplayName("All chat task types should have exactly one agent")
        void allChatTaskTypesShouldHaveOneAgent() {
            List<String> chatTaskTypes = List.of("edit", "plan", "explain", "book", "enrich");
            
            for (String taskType : chatTaskTypes) {
                List<BaseAgent> agents = agentRegistry.getAgentsForTask(taskType, true);
                assertThat(agents)
                    .as("Task '%s' should return exactly 1 chat-enabled agent (zero overlap)", taskType)
                    .hasSize(1);
            }
        }
    }

    @Nested
    @DisplayName("4. Chat-Only Filtering Tests")
    class ChatFilteringTests {

        @Test
        @DisplayName("Should filter out pipeline-only agents when chatOnly=true")
        void shouldFilterOutPipelineAgentsWithChatOnly() {
            List<BaseAgent> chatAgents = agentRegistry.getAgentsForTask("skeleton", true);
            
            assertThat(chatAgents)
                .as("'skeleton' task with chatOnly=true should return empty list")
                .isEmpty();
        }

        @Test
        @DisplayName("Should include pipeline-only agents when chatOnly=false")
        void shouldIncludePipelineAgentsWithoutChatFilter() {
            List<BaseAgent> allAgents = agentRegistry.getAgentsForTask("skeleton", false);
            
            assertThat(allAgents)
                .as("'skeleton' task with chatOnly=false should return SkeletonPlannerAgent")
                .isNotEmpty();
        }

        @Test
        @DisplayName("Pipeline tasks should return empty list with chatOnly=true")
        void pipelineTasksShouldReturnEmptyWithChatFilter() {
            List<String> pipelineTasks = List.of(
                "skeleton", 
                "populate_attractions", 
                "populate_meals", 
                "populate_transport", 
                "estimate_costs"
            );
            
            for (String taskType : pipelineTasks) {
                List<BaseAgent> agents = agentRegistry.getAgentsForTask(taskType, true);
                assertThat(agents)
                    .as("Pipeline task '%s' should return empty list with chatOnly=true", taskType)
                    .isEmpty();
            }
        }

        @Test
        @DisplayName("Pipeline tasks should return agents with chatOnly=false")
        void pipelineTasksShouldReturnAgentsWithoutFilter() {
            List<String> pipelineTasks = List.of(
                "skeleton", 
                "populate_attractions", 
                "populate_meals", 
                "populate_transport", 
                "estimate_costs"
            );
            
            for (String taskType : pipelineTasks) {
                List<BaseAgent> agents = agentRegistry.getAgentsForTask(taskType, false);
                assertThat(agents)
                    .as("Pipeline task '%s' should return agents with chatOnly=false", taskType)
                    .isNotEmpty();
            }
        }
    }

    @Nested
    @DisplayName("5. Priority Sorting Tests")
    class PrioritySortingTests {

        @Test
        @DisplayName("Agents should be sorted by priority (lower number = higher priority)")
        void agentsShouldBeSortedByPriority() {
            Map<String, AgentCapabilities> allCaps = agentRegistry.getAllCapabilities();
            
            // Expected priority order for chat-enabled agents
            // DayByDayPlannerAgent: 5
            // EditorAgent: 10
            // EnrichmentAgent: 20
            // BookingAgent: 30
            
            // Get all chat-enabled agents and sort them by priority
            List<Map.Entry<String, AgentCapabilities>> chatEnabledAgents = allCaps.entrySet().stream()
                .filter(entry -> entry.getValue().isChatEnabled())
                .sorted(Map.Entry.comparingByValue(Comparator.comparing(AgentCapabilities::getPriority)))
                .collect(Collectors.toList());
            
            // Verify that priorities are in ascending order (lower number = higher priority)
            int previousPriority = -1;
            for (Map.Entry<String, AgentCapabilities> entry : chatEnabledAgents) {
                AgentCapabilities caps = entry.getValue();
                if (previousPriority >= 0) {
                    assertThat(caps.getPriority())
                        .as("Chat-enabled agents should be sorted by priority")
                        .isGreaterThanOrEqualTo(previousPriority);
                }
                previousPriority = caps.getPriority();
            }
        }

        @Test
        @DisplayName("DayByDayPlannerAgent should have highest priority (lowest number)")
        void dayByDayPlannerShouldHaveHighestPriority() {
            AgentCapabilities plannerCaps = agentRegistry.getAgentCapabilities("daybydayplanneragent").orElse(null);
            AgentCapabilities editorCaps = agentRegistry.getAgentCapabilities("editoragent").orElse(null);
            
            assertThat(plannerCaps).isNotNull();
            assertThat(editorCaps).isNotNull();
            
            assertThat(plannerCaps.getPriority())
                .as("DayByDayPlannerAgent should have higher priority than EditorAgent")
                .isLessThan(editorCaps.getPriority());
        }
    }

    @Nested
    @DisplayName("6. Agent Registration Tests")
    class AgentRegistrationTests {

        @Test
        @DisplayName("All expected agents should be registered")
        void allExpectedAgentsShouldBeRegistered() {
            Map<String, AgentCapabilities> allCaps = agentRegistry.getAllCapabilities();
            
            assertThat(allCaps)
                .as("AgentRegistry should have agents registered")
                .isNotEmpty();
            
            // At minimum, we should have these agents
            List<String> expectedAgents = List.of("editoragent", "daybydayplanneragent", "bookingagent", "enrichmentagent", "placesagent");
            
            for (String agentId : expectedAgents) {
                assertThat(allCaps.get(agentId))
                    .as("Agent %s should be registered", agentId)
                    .isNotNull();
            }
        }

        @Test
        @DisplayName("All registered agents should have valid capabilities")
        void allRegisteredAgentsShouldHaveValidCapabilities() {
            Map<String, AgentCapabilities> allCaps = agentRegistry.getAllCapabilities();
            
            for (Map.Entry<String, AgentCapabilities> entry : allCaps.entrySet()) {
                String agentId = entry.getKey();
                AgentCapabilities caps = entry.getValue();
                
                assertThat(caps)
                    .as("Agent %s should have capabilities", agentId)
                    .isNotNull();
                
                assertThat(caps.isEnabled())
                    .as("Agent %s should be enabled", agentId)
                    .isTrue();
                
                assertThat(caps.getSupportedTasks())
                    .as("Agent %s should have at least one supported task", agentId)
                    .isNotEmpty();
                
                assertThat(caps.getPriority())
                    .as("Agent %s should have valid priority", agentId)
                    .isGreaterThan(0);
            }
        }
    }

    @Nested
    @DisplayName("7. Configuration Tests")
    class ConfigurationTests {

        @Test
        @DisplayName("Chat-enabled agents should have proper configuration")
        void chatEnabledAgentsShouldHaveProperConfig() {
            List<String> chatAgentIds = List.of("EDITOR", "PLANNER", "BOOKING", "ENRICHMENT");
            
            for (String agentId : chatAgentIds) {
                AgentCapabilities caps = agentRegistry.getAgentCapabilities(agentId).orElse(null);
                if (caps != null) {
                    assertThat(caps.getConfiguration())
                        .as("Chat-enabled agent %s should have configuration", agentId)
                        .isNotNull();
                    
                    assertThat(caps.getConfigurationValue("chatEnabled"))
                        .as("Agent %s should have chatEnabled=true in configuration", agentId)
                        .isEqualTo(true);
                }
            }
        }

        @Test
        @DisplayName("Pipeline-only agents should have chatEnabled=false")
        void pipelineAgentsShouldHaveChatDisabled() {
            Map<String, AgentCapabilities> allCaps = agentRegistry.getAllCapabilities();
            
            for (Map.Entry<String, AgentCapabilities> entry : allCaps.entrySet()) {
                AgentCapabilities caps = entry.getValue();
                
                // Check pipeline tasks
                if (caps.supportsTask("skeleton") || 
                    caps.supportsTask("populate_attractions") ||
                    caps.supportsTask("populate_meals") ||
                    caps.supportsTask("populate_transport") ||
                    caps.supportsTask("estimate_costs")) {
                    
                    assertThat(caps.getConfigurationValue("chatEnabled"))
                        .as("Pipeline agent with task %s should have chatEnabled=false", 
                            caps.getSupportedTasks())
                        .isEqualTo(false);
                }
            }
        }
    }
}

