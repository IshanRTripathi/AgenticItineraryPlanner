package com.tripplanner.testing.dto;

import com.tripplanner.dto.AgentCapability;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test cases for AgentCapability enum to ensure proper capability management.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("AgentCapability Tests")
public class AgentCapabilityTest {
    
    @Test
    @DisplayName("Should parse capability from string with case insensitivity")
    void shouldParseCapabilityFromStringWithCaseInsensitivity() {
        // Test various case formats
        assertEquals(AgentCapability.ITINERARY_GENERATION, AgentCapability.fromString("ITINERARY_GENERATION"));
        assertEquals(AgentCapability.ITINERARY_GENERATION, AgentCapability.fromString("itinerary_generation"));
        assertEquals(AgentCapability.ITINERARY_GENERATION, AgentCapability.fromString("Itinerary_Generation"));
        
        // Test with spaces (should be converted to underscores)
        assertEquals(AgentCapability.PLACE_DISCOVERY, AgentCapability.fromString("PLACE DISCOVERY"));
        assertEquals(AgentCapability.NATURAL_LANGUAGE_PROCESSING, AgentCapability.fromString("natural language processing"));
    }
    
    @Test
    @DisplayName("Should throw exception for invalid capability name")
    void shouldThrowExceptionForInvalidCapabilityName() {
        assertThrows(IllegalArgumentException.class, () -> 
            AgentCapability.fromString("INVALID_CAPABILITY"));
        
        assertThrows(IllegalArgumentException.class, () -> 
            AgentCapability.fromString(""));
        
        assertThrows(IllegalArgumentException.class, () -> 
            AgentCapability.fromString(null));
    }
    
    @Test
    @DisplayName("Should correctly identify core capabilities")
    void shouldCorrectlyIdentifyCoreCapabilities() {
        assertTrue(AgentCapability.ITINERARY_GENERATION.isCoreCapability());
        assertTrue(AgentCapability.DAY_PLANNING.isCoreCapability());
        assertTrue(AgentCapability.ACTIVITY_SCHEDULING.isCoreCapability());
        assertTrue(AgentCapability.PLACE_DISCOVERY.isCoreCapability());
        
        assertFalse(AgentCapability.PHOTO_RECOMMENDATIONS.isCoreCapability());
        assertFalse(AgentCapability.BUDGET_ANALYSIS.isCoreCapability());
    }
    
    @Test
    @DisplayName("Should correctly identify capabilities requiring external APIs")
    void shouldCorrectlyIdentifyCapabilitiesRequiringExternalApis() {
        assertTrue(AgentCapability.PLACE_DISCOVERY.requiresExternalApi());
        assertTrue(AgentCapability.PLACE_ENRICHMENT.requiresExternalApi());
        assertTrue(AgentCapability.ACCOMMODATION_BOOKING.requiresExternalApi());
        assertTrue(AgentCapability.WEATHER_INTEGRATION.requiresExternalApi());
        
        assertFalse(AgentCapability.ITINERARY_GENERATION.requiresExternalApi());
        assertFalse(AgentCapability.CONTENT_SUMMARIZATION.requiresExternalApi());
    }
    
    @Test
    @DisplayName("Should return correct capability categories")
    void shouldReturnCorrectCapabilityCategories() {
        AgentCapability[] planningCapabilities = AgentCapability.getPlanningCapabilities();
        assertEquals(4, planningCapabilities.length);
        assertTrue(Arrays.asList(planningCapabilities).contains(AgentCapability.ITINERARY_GENERATION));
        assertTrue(Arrays.asList(planningCapabilities).contains(AgentCapability.DAY_PLANNING));
        
        AgentCapability[] bookingCapabilities = AgentCapability.getBookingCapabilities();
        assertEquals(4, bookingCapabilities.length);
        assertTrue(Arrays.asList(bookingCapabilities).contains(AgentCapability.ACCOMMODATION_BOOKING));
        assertTrue(Arrays.asList(bookingCapabilities).contains(AgentCapability.ACTIVITY_BOOKING));
        
        AgentCapability[] contentCapabilities = AgentCapability.getContentCapabilities();
        assertEquals(4, contentCapabilities.length);
        assertTrue(Arrays.asList(contentCapabilities).contains(AgentCapability.CONTENT_ENRICHMENT));
        assertTrue(Arrays.asList(contentCapabilities).contains(AgentCapability.PHOTO_RECOMMENDATIONS));
    }
    
    @Test
    @DisplayName("Should have meaningful descriptions for all capabilities")
    void shouldHaveMeaningfulDescriptionsForAllCapabilities() {
        for (AgentCapability capability : AgentCapability.values()) {
            assertNotNull(capability.getDescription(), 
                "Capability " + capability.name() + " should have a description");
            assertFalse(capability.getDescription().trim().isEmpty(), 
                "Capability " + capability.name() + " should have a non-empty description");
            assertTrue(capability.getDescription().length() > 10, 
                "Capability " + capability.name() + " should have a meaningful description");
        }
    }
}