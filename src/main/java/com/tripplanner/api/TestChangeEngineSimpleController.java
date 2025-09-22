package com.tripplanner.api;

import com.tripplanner.api.dto.*;
import com.tripplanner.service.ChangeEngine;
import com.tripplanner.service.ItineraryJsonService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Simple test controller for ChangeEngine debugging.
 */
@RestController
@RequestMapping("/api/v1/test/change-simple")
public class TestChangeEngineSimpleController {
    
    private static final Logger logger = LoggerFactory.getLogger(TestChangeEngineSimpleController.class);
    
    private final ChangeEngine changeEngine;
    private final ItineraryJsonService itineraryJsonService;
    
    public TestChangeEngineSimpleController(ChangeEngine changeEngine, ItineraryJsonService itineraryJsonService) {
        this.changeEngine = changeEngine;
        this.itineraryJsonService = itineraryJsonService;
    }
    
    /**
     * Test basic ChangeEngine functionality.
     */
    @PostMapping("/test/{itineraryId}")
    public ResponseEntity<String> testBasic(@PathVariable String itineraryId) {
        logger.info("Testing basic ChangeEngine functionality for itinerary: {}", itineraryId);
        
        try {
            // First, let's check if the itinerary exists
            var itinerary = itineraryJsonService.getItinerary(itineraryId);
            if (itinerary.isEmpty()) {
                return ResponseEntity.badRequest().body("Itinerary not found: " + itineraryId);
            }
            
            logger.info("Itinerary found with {} days", itinerary.get().getDays().size());
            
            // Create a simple ChangeSet with no operations
            ChangeSet changeSet = new ChangeSet("day", 1, java.util.Collections.emptyList());
            ChangePreferences preferences = new ChangePreferences(true, false, true);
            changeSet.setPreferences(preferences);
            
            // Try to propose with empty operations
            ChangeEngine.ProposeResult result = changeEngine.propose(itineraryId, changeSet);
            
            return ResponseEntity.ok("Basic test successful! Version: " + result.getPreviewVersion());
        } catch (Exception e) {
            logger.error("Basic test failed", e);
            return ResponseEntity.badRequest().body("Basic test failed: " + e.getMessage());
        }
    }
}
