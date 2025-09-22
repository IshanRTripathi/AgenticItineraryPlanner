package com.tripplanner.api;

import com.tripplanner.api.dto.NormalizedItinerary;
import com.tripplanner.data.entity.FirestoreItinerary;
import com.tripplanner.service.ItineraryJsonService;
import com.tripplanner.service.SampleDataGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

/**
 * Test controller for JSON storage functionality.
 * This will be removed once the main implementation is complete.
 */
@RestController
@RequestMapping("/api/v1/test")
public class TestJsonController {
    
    private static final Logger logger = LoggerFactory.getLogger(TestJsonController.class);
    
    private final ItineraryJsonService itineraryJsonService;
    private final SampleDataGenerator sampleDataGenerator;
    
    public TestJsonController(ItineraryJsonService itineraryJsonService, SampleDataGenerator sampleDataGenerator) {
        this.itineraryJsonService = itineraryJsonService;
        this.sampleDataGenerator = sampleDataGenerator;
    }
    
    /**
     * Create sample Barcelona itinerary for testing.
     */
    @PostMapping("/create-sample")
    public ResponseEntity<NormalizedItinerary> createSample() {
        logger.info("Creating sample Barcelona itinerary");
        // Create a sample itinerary using SampleDataGenerator
        NormalizedItinerary sample = sampleDataGenerator.generateBarcelonaSample();
        FirestoreItinerary saved = itineraryJsonService.createItinerary(sample);
        logger.info("Sample itinerary created with ID: {}", saved.getId());
        return ResponseEntity.ok(sample);
    }
    
    /**
     * Get itinerary by ID.
     */
    @GetMapping("/itinerary/{id}")
    public ResponseEntity<NormalizedItinerary> getItinerary(@PathVariable String id) {
        logger.info("Getting itinerary with ID: {}", id);
        Optional<NormalizedItinerary> itinerary = itineraryJsonService.getItinerary(id);
        if (itinerary.isPresent()) {
            return ResponseEntity.ok(itinerary.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Test JSON serialization/deserialization.
     */
    @PostMapping("/test-json")
    public ResponseEntity<String> testJson() {
        logger.info("Testing JSON serialization/deserialization");
        try {
            NormalizedItinerary sample = sampleDataGenerator.generateBarcelonaSample();
            FirestoreItinerary saved = itineraryJsonService.createItinerary(sample);
            Optional<NormalizedItinerary> loaded = itineraryJsonService.getItinerary(saved.getId());
            
            if (loaded.isPresent()) {
                return ResponseEntity.ok("JSON test successful! Itinerary ID: " + saved.getId());
            } else {
                return ResponseEntity.badRequest().body("Failed to load itinerary");
            }
        } catch (Exception e) {
            logger.error("JSON test failed", e);
            return ResponseEntity.badRequest().body("JSON test failed: " + e.getMessage());
        }
    }
}
