package com.tripplanner.testing.dto;

import com.tripplanner.dto.AgentCapabilities;
import com.tripplanner.testing.BaseServiceTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Atomic tests for AgentCapabilities with comprehensive validation.
 */
class AgentCapabilitiesTest extends BaseServiceTest {
    
    private AgentCapabilities capabilities;
    
    @BeforeEach
    protected void setUp() {
        super.setUp();
        capabilities = new AgentCapabilities();
    }
    
    @Override
    protected void setupSpecificMocks() {
        // No mocks needed for DTO tests
    }
    
    @Test
    @DisplayName("Should validate agent capabilities structure")
    void shouldValidateAgentCapabilitiesStructure() {
        // Given
        capabilities.addSupportedTask("plan");
        capabilities.addSupportedTask("book");
        capabilities.addSupportedDataSection("itinerary");
        capabilities.addSupportedDataSection("bookings");
        capabilities.setPriority(10);
        capabilities.setConfigurationValue("requiresLLM", true);
        capabilities.setConfigurationValue("maxDays", 14);
        
        // When/Then
        assertThat(capabilities.getSupportedTasks()).contains("plan", "book");
        assertThat(capabilities.getSupportedDataSections()).contains("itinerary", "bookings");
        assertThat(capabilities.getPriority()).isEqualTo(10);
        assertThat(capabilities.getConfigurationValue("requiresLLM")).isEqualTo(true);
        assertThat(capabilities.getConfigurationValue("maxDays")).isEqualTo(14);
        
        logger.info("Validate agent capabilities structure test passed");
    }
    
    @Test
    @DisplayName("Should handle capability matching and conflict detection")
    void shouldHandleCapabilityMatchingAndConflictDetection() {
        // Given - Create two capabilities with overlapping tasks
        AgentCapabilities capabilities1 = new AgentCapabilities();
        capabilities1.addSupportedTask("plan");
        capabilities1.addSupportedTask("edit");
        capabilities1.setPriority(10);
        
        AgentCapabilities capabilities2 = new AgentCapabilities();
        capabilities2.addSupportedTask("plan"); // Overlapping task
        capabilities2.addSupportedTask("book");
        capabilities2.setPriority(20);
        
        // When/Then - Test capability matching
        assertThat(capabilities1.getSupportedTasks()).contains("plan");
        assertThat(capabilities2.getSupportedTasks()).contains("plan");
        
        // Test conflict detection (both support "plan")
        List<String> overlappingTasks = Arrays.asList("plan");
        assertThat(capabilities1.getSupportedTasks()).containsAnyElementsOf(overlappingTasks);
        assertThat(capabilities2.getSupportedTasks()).containsAnyElementsOf(overlappingTasks);
        
        // Test priority-based resolution (lower priority wins)
        assertThat(capabilities1.getPriority()).isLessThan(capabilities2.getPriority());
        
        logger.info("Handle capability matching and conflict detection test passed");
    }
    
    @Test
    @DisplayName("Should verify agent responsibility validation works correctly")
    void shouldVerifyAgentResponsibilityValidationWorksCorrectly() {
        // Given
        capabilities.addSupportedTask("plan");
        capabilities.addSupportedTask("create");
        capabilities.addSupportedDataSection("itinerary");
        capabilities.addSupportedDataSection("activities");
        capabilities.setPriority(10);
        capabilities.setConfigurationValue("canCreateItinerary", true);
        capabilities.setConfigurationValue("canModifyItinerary", true);
        
        // When/Then - Test task responsibility
        assertThat(capabilities.getSupportedTasks()).contains("plan", "create");
        assertThat(capabilities.getSupportedTasks()).doesNotContain("book", "payment");
        
        // Test data section responsibility
        assertThat(capabilities.getSupportedDataSections()).contains("itinerary", "activities");
        assertThat(capabilities.getSupportedDataSections()).doesNotContain("payments", "bookings");
        
        // Test configuration-based responsibility
        assertThat(capabilities.getConfigurationValue("canCreateItinerary")).isEqualTo(true);
        assertThat(capabilities.getConfigurationValue("canModifyItinerary")).isEqualTo(true);
        assertThat(capabilities.getConfigurationValue("requiresPayment")).isNull();
        
        logger.info("Verify agent responsibility validation test passed");
    }
    
    @Test
    @DisplayName("Should handle empty capabilities gracefully")
    void shouldHandleEmptyCapabilitiesGracefully() {
        // Given - Empty capabilities
        AgentCapabilities emptyCapabilities = new AgentCapabilities();
        
        // When/Then
        assertThat(emptyCapabilities.getSupportedTasks()).isEmpty();
        assertThat(emptyCapabilities.getSupportedDataSections()).isEmpty();
        assertThat(emptyCapabilities.getPriority()).isEqualTo(100); // Default priority
        assertThat(emptyCapabilities.getConfigurationValue("nonexistent")).isNull();
        
        logger.info("Handle empty capabilities gracefully test passed");
    }
    
    @Test
    @DisplayName("Should validate priority ordering")
    void shouldValidatePriorityOrdering() {
        // Given - Different priority levels
        AgentCapabilities highPriority = new AgentCapabilities();
        highPriority.setPriority(10); // Lower number = higher priority
        
        AgentCapabilities mediumPriority = new AgentCapabilities();
        mediumPriority.setPriority(30);
        
        AgentCapabilities lowPriority = new AgentCapabilities();
        lowPriority.setPriority(50);
        
        // When/Then - Verify priority ordering
        assertThat(highPriority.getPriority()).isLessThan(mediumPriority.getPriority());
        assertThat(mediumPriority.getPriority()).isLessThan(lowPriority.getPriority());
        
        // Test priority-based selection (lower number wins)
        List<AgentCapabilities> capabilitiesList = Arrays.asList(lowPriority, highPriority, mediumPriority);
        AgentCapabilities selectedCapabilities = capabilitiesList.stream()
                .min((c1, c2) -> Integer.compare(c1.getPriority(), c2.getPriority()))
                .orElse(null);
        
        assertThat(selectedCapabilities).isEqualTo(highPriority);
        
        logger.info("Validate priority ordering test passed");
    }
    
    @Test
    @DisplayName("Should handle configuration values correctly")
    void shouldHandleConfigurationValuesCorrectly() {
        // Given
        capabilities.setConfigurationValue("stringValue", "test");
        capabilities.setConfigurationValue("intValue", 42);
        capabilities.setConfigurationValue("boolValue", true);
        capabilities.setConfigurationValue("nullValue", null);
        
        // When/Then
        assertThat(capabilities.getConfigurationValue("stringValue")).isEqualTo("test");
        assertThat(capabilities.getConfigurationValue("intValue")).isEqualTo(42);
        assertThat(capabilities.getConfigurationValue("boolValue")).isEqualTo(true);
        assertThat(capabilities.getConfigurationValue("nullValue")).isNull();
        assertThat(capabilities.getConfigurationValue("nonexistent")).isNull();
        
        logger.info("Handle configuration values correctly test passed");
    }
}