package com.tripplanner.testing;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

import com.tripplanner.testing.service.SseConnectionManagerTest;
import com.tripplanner.testing.service.AgentEventPublisherTest;
import com.tripplanner.testing.service.SummarizationIntegrationTest;
import com.tripplanner.testing.controller.ItinerariesControllerEnhancedTest;
import com.tripplanner.testing.integration.RealTimeItineraryGenerationIntegrationTest;
import com.tripplanner.testing.e2e.RealTimeItineraryE2ETest;

/**
 * Comprehensive test suite for real-time itinerary generation system.
 * Runs all tests from unit level to end-to-end scenarios.
 */
@Suite
@SuiteDisplayName("Real-Time Itinerary Generation Test Suite")
@SelectClasses({
    // Unit Tests
    SseConnectionManagerTest.class,
    AgentEventPublisherTest.class,
    
    // Integration Tests
    SummarizationIntegrationTest.class,
    ItinerariesControllerEnhancedTest.class,
    RealTimeItineraryGenerationIntegrationTest.class,
    
    // End-to-End Tests
    RealTimeItineraryE2ETest.class
})
public class RealTimeTestSuite {
    
    /**
     * Test Coverage Summary:
     * 
     * UNIT TESTS (40+ test cases):
     * - SseConnectionManagerTest: Connection lifecycle, event broadcasting, cleanup
     * - AgentEventPublisherTest: Event publishing, error handling, progress tracking
     * 
     * INTEGRATION TESTS (30+ test cases):
     * - SummarizationIntegrationTest: Token management, context preservation, agent-specific summaries
     * - ItinerariesControllerEnhancedTest: Enhanced endpoints, immediate responses, SSE integration
     * - RealTimeItineraryGenerationIntegrationTest: Complete flow with mocked LLM responses
     * 
     * END-TO-END TESTS (15+ test cases):
     * - RealTimeItineraryE2ETest: Complete user scenarios, error recovery, performance under load
     * 
     * TOTAL: 85+ comprehensive test cases covering:
     * ✅ Real-time event publishing and SSE connections
     * ✅ Day-by-day planning with token limit management
     * ✅ Summarization logic with context preservation
     * ✅ Error handling and recovery scenarios
     * ✅ Connection management and cleanup
     * ✅ Multiple concurrent users
     * ✅ Performance under load
     * ✅ Mocked LLM response handling
     * ✅ Complete user journey simulation
     */
}