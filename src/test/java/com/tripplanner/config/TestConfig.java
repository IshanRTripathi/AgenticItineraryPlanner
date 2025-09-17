package com.tripplanner.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripplanner.service.AgentEventBus;
import com.tripplanner.service.GeminiClient;
import com.tripplanner.service.agents.AgentOrchestrator;
import com.tripplanner.service.agents.PlacesAgent;
import com.tripplanner.service.agents.PlannerAgent;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import static org.mockito.Mockito.mock;

/**
 * Test configuration to provide mock beans for testing.
 */
@TestConfiguration
public class TestConfig {
    
    @Bean
    @Primary
    public GeminiClient mockGeminiClient() {
        return mock(GeminiClient.class);
    }
    
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
    
    @Bean
    @Primary
    public AgentEventBus agentEventBus() {
        return new AgentEventBus();
    }
    
    @Bean
    @Primary
    public PlannerAgent mockPlannerAgent() {
        return mock(PlannerAgent.class);
    }
    
    @Bean
    @Primary
    public PlacesAgent mockPlacesAgent() {
        return mock(PlacesAgent.class);
    }
    
    @Bean
    @Primary
    public AgentOrchestrator mockAgentOrchestrator() {
        return mock(AgentOrchestrator.class);
    }
}
