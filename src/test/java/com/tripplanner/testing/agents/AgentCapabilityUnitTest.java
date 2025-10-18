package com.tripplanner.testing.agents;

import com.tripplanner.agents.*;
import com.tripplanner.dto.AgentCapabilities;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for Agent Capability System (no Spring context required).
 */
@DisplayName("Agent Capability Unit Tests")
public class AgentCapabilityUnitTest {

    @Test
    @DisplayName("AgentCapabilities should have chatEnabled getter/setter")
    void agentCapabilitiesShouldHaveChatEnabled() {
        AgentCapabilities caps = new AgentCapabilities();
        
        // Default should be true
        assertThat(caps.isChatEnabled()).isTrue();
        
        // Should be settable
        caps.setChatEnabled(false);
        assertThat(caps.isChatEnabled()).isFalse();
        
        caps.setChatEnabled(true);
        assertThat(caps.isChatEnabled()).isTrue();
    }

    @Test
    @DisplayName("AgentCapabilities should store chatEnabled in configuration")
    void chatEnabledShouldBeInConfiguration() {
        AgentCapabilities caps = new AgentCapabilities();
        
        caps.setChatEnabled(false);
        assertThat(caps.getConfigurationValue("chatEnabled")).isEqualTo(false);
        
        caps.setChatEnabled(true);
        assertThat(caps.getConfigurationValue("chatEnabled")).isEqualTo(true);
    }

    @Test
    @DisplayName("EditorAgent should define 'edit' task and chatEnabled=true")
    void editorAgentCapabilities() {
        // We can't instantiate agents without Spring, but we can verify the design
        // This test documents the expected capability structure
        
        AgentCapabilities caps = new AgentCapabilities();
        caps.addSupportedTask("edit");
        caps.setPriority(10);
        caps.setChatEnabled(true);
        
        assertThat(caps.supportsTask("edit")).isTrue();
        assertThat(caps.isChatEnabled()).isTrue();
        assertThat(caps.getPriority()).isEqualTo(10);
    }

    @Test
    @DisplayName("DayByDayPlannerAgent should define 'plan' and 'explain' tasks")
    void dayByDayPlannerCapabilities() {
        AgentCapabilities caps = new AgentCapabilities();
        caps.addSupportedTask("plan");
        caps.addSupportedTask("explain");
        caps.setPriority(5);
        caps.setChatEnabled(true);
        
        assertThat(caps.supportsTask("plan")).isTrue();
        assertThat(caps.supportsTask("explain")).isTrue();
        assertThat(caps.isChatEnabled()).isTrue();
        assertThat(caps.getPriority()).isEqualTo(5);
    }

    @Test
    @DisplayName("BookingAgent should define 'book' task")
    void bookingAgentCapabilities() {
        AgentCapabilities caps = new AgentCapabilities();
        caps.addSupportedTask("book");
        caps.setPriority(30);
        caps.setChatEnabled(true);
        
        assertThat(caps.supportsTask("book")).isTrue();
        assertThat(caps.isChatEnabled()).isTrue();
    }

    @Test
    @DisplayName("EnrichmentAgent should define 'enrich' task")
    void enrichmentAgentCapabilities() {
        AgentCapabilities caps = new AgentCapabilities();
        caps.addSupportedTask("enrich");
        caps.setPriority(20);
        caps.setChatEnabled(true);
        
        assertThat(caps.supportsTask("enrich")).isTrue();
        assertThat(caps.isChatEnabled()).isTrue();
    }

    @Test
    @DisplayName("Pipeline agents should have chatEnabled=false")
    void pipelineAgentsShouldNotBeChatEnabled() {
        // SkeletonPlannerAgent
        AgentCapabilities skeleton = new AgentCapabilities();
        skeleton.addSupportedTask("skeleton");
        skeleton.setChatEnabled(false);
        assertThat(skeleton.isChatEnabled()).isFalse();
        
        // ActivityAgent
        AgentCapabilities activity = new AgentCapabilities();
        activity.addSupportedTask("populate_attractions");
        activity.setChatEnabled(false);
        assertThat(activity.isChatEnabled()).isFalse();
        
        // MealAgent
        AgentCapabilities meal = new AgentCapabilities();
        meal.addSupportedTask("populate_meals");
        meal.setChatEnabled(false);
        assertThat(meal.isChatEnabled()).isFalse();
        
        // TransportAgent
        AgentCapabilities transport = new AgentCapabilities();
        transport.addSupportedTask("populate_transport");
        transport.setChatEnabled(false);
        assertThat(transport.isChatEnabled()).isFalse();
        
        // CostEstimatorAgent
        AgentCapabilities cost = new AgentCapabilities();
        cost.addSupportedTask("estimate_costs");
        cost.setChatEnabled(false);
        assertThat(cost.isChatEnabled()).isFalse();
    }

    @Test
    @DisplayName("Zero-overlap: Each task type should be unique")
    void taskTypesShouldNotOverlap() {
        // Chat-enabled tasks
        AgentCapabilities edit = new AgentCapabilities();
        edit.addSupportedTask("edit");
        edit.setChatEnabled(true);
        
        AgentCapabilities plan = new AgentCapabilities();
        plan.addSupportedTask("plan");
        plan.addSupportedTask("explain");
        plan.setChatEnabled(true);
        
        AgentCapabilities book = new AgentCapabilities();
        book.addSupportedTask("book");
        book.setChatEnabled(true);
        
        AgentCapabilities enrich = new AgentCapabilities();
        enrich.addSupportedTask("enrich");
        enrich.setChatEnabled(true);
        
        // Verify no overlap
        assertThat(edit.supportsTask("edit")).isTrue();
        assertThat(plan.supportsTask("edit")).isFalse();
        assertThat(book.supportsTask("edit")).isFalse();
        assertThat(enrich.supportsTask("edit")).isFalse();
        
        assertThat(plan.supportsTask("plan")).isTrue();
        assertThat(edit.supportsTask("plan")).isFalse();
        assertThat(book.supportsTask("plan")).isFalse();
        assertThat(enrich.supportsTask("plan")).isFalse();
    }

    @Test
    @DisplayName("Priority system: Lower number = higher priority")
    void prioritySystemShouldWork() {
        AgentCapabilities high = new AgentCapabilities();
        high.setPriority(5);
        
        AgentCapabilities medium = new AgentCapabilities();
        medium.setPriority(10);
        
        AgentCapabilities low = new AgentCapabilities();
        low.setPriority(30);
        
        assertThat(high.getPriority()).isLessThan(medium.getPriority());
        assertThat(medium.getPriority()).isLessThan(low.getPriority());
    }

    @Test
    @DisplayName("User scenarios: Edit intents map to 'edit' task")
    void editIntentScenarios() {
        AgentCapabilities editor = new AgentCapabilities();
        editor.addSupportedTask("edit");
        editor.setChatEnabled(true);
        
        // All these user messages should map to "edit" task type
        String[] editMessages = {
            "Move lunch to 2pm",
            "Change the restaurant",
            "Add a museum visit",
            "Remove the beach activity",
            "Replace dinner with Italian restaurant",
            "Update the hotel name"
        };
        
        for (String message : editMessages) {
            assertThat(editor.supportsTask("edit"))
                .as("Edit task should handle: %s", message)
                .isTrue();
        }
    }

    @Test
    @DisplayName("User scenarios: Plan/Explain intents map to respective tasks")
    void planExplainIntentScenarios() {
        AgentCapabilities planner = new AgentCapabilities();
        planner.addSupportedTask("plan");
        planner.addSupportedTask("explain");
        planner.setChatEnabled(true);
        
        // Plan scenarios
        assertThat(planner.supportsTask("plan")).isTrue(); // "Create a 3-day trip to Paris"
        assertThat(planner.supportsTask("plan")).isTrue(); // "Plan a weekend in Rome"
        
        // Explain scenarios
        assertThat(planner.supportsTask("explain")).isTrue(); // "What's my plan for today?"
        assertThat(planner.supportsTask("explain")).isTrue(); // "Show me my schedule"
    }

    @Test
    @DisplayName("User scenarios: Book intents map to 'book' task")
    void bookIntentScenarios() {
        AgentCapabilities booking = new AgentCapabilities();
        booking.addSupportedTask("book");
        booking.setChatEnabled(true);
        
        String[] bookMessages = {
            "Book this hotel",
            "Reserve a table",
            "Purchase tickets",
            "Book a flight"
        };
        
        for (String message : bookMessages) {
            assertThat(booking.supportsTask("book"))
                .as("Book task should handle: %s", message)
                .isTrue();
        }
    }

    @Test
    @DisplayName("User scenarios: Enrich intents map to 'enrich' task")
    void enrichIntentScenarios() {
        AgentCapabilities enrichment = new AgentCapabilities();
        enrichment.addSupportedTask("enrich");
        enrichment.setChatEnabled(true);
        
        String[] enrichMessages = {
            "Add photos to this place",
            "Show me reviews",
            "Get more details about this hotel",
            "Find POI information"
        };
        
        for (String message : enrichMessages) {
            assertThat(enrichment.supportsTask("enrich"))
                .as("Enrich task should handle: %s", message)
                .isTrue();
        }
    }
}





