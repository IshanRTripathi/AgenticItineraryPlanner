package com.tripplanner.api;

import com.tripplanner.service.SampleDataGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Temporary controller for testing sample data generation.
 * This will be removed after Phase 5 completion.
 */
@RestController
@RequestMapping("/api/v1/test/sample-data")
public class TestSampleDataController {
    
    private static final Logger logger = LoggerFactory.getLogger(TestSampleDataController.class);
    
    private final SampleDataGenerator sampleDataGenerator;
    
    public TestSampleDataController(SampleDataGenerator sampleDataGenerator) {
        this.sampleDataGenerator = sampleDataGenerator;
    }
    
    /**
     * Generate all sample itineraries.
     * POST /test/sample-data/generate-all
     */
    @PostMapping("/generate-all")
    public ResponseEntity<Map<String, Object>> generateAllSamples() {
        logger.info("Generating all sample itineraries");
        
        try {
            sampleDataGenerator.generateAllSamples();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "All sample itineraries generated successfully");
            response.put("itineraries", new String[]{
                "it_barcelona_comprehensive",
                "it_paris_comprehensive", 
                "it_tokyo_comprehensive"
            });
            
            logger.info("All sample itineraries generated successfully");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to generate sample itineraries", e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    /**
     * Generate Barcelona sample itinerary.
     * POST /test/sample-data/generate-barcelona
     */
    @PostMapping("/generate-barcelona")
    public ResponseEntity<Map<String, Object>> generateBarcelona() {
        logger.info("Generating Barcelona sample itinerary");
        
        try {
            var barcelona = sampleDataGenerator.generateBarcelonaSample();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Barcelona sample itinerary generated");
            response.put("itineraryId", barcelona.getItineraryId());
            response.put("days", barcelona.getDays().size());
            response.put("totalNodes", barcelona.getDays().stream()
                .mapToInt(day -> day.getNodes().size())
                .sum());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to generate Barcelona sample", e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    /**
     * Generate Paris sample itinerary.
     * POST /test/sample-data/generate-paris
     */
    @PostMapping("/generate-paris")
    public ResponseEntity<Map<String, Object>> generateParis() {
        logger.info("Generating Paris sample itinerary");
        
        try {
            var paris = sampleDataGenerator.generateParisSample();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Paris sample itinerary generated");
            response.put("itineraryId", paris.getItineraryId());
            response.put("days", paris.getDays().size());
            response.put("totalNodes", paris.getDays().stream()
                .mapToInt(day -> day.getNodes().size())
                .sum());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to generate Paris sample", e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    /**
     * Generate Tokyo sample itinerary.
     * POST /test/sample-data/generate-tokyo
     */
    @PostMapping("/generate-tokyo")
    public ResponseEntity<Map<String, Object>> generateTokyo() {
        logger.info("Generating Tokyo sample itinerary");
        
        try {
            var tokyo = sampleDataGenerator.generateTokyoSample();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Tokyo sample itinerary generated");
            response.put("itineraryId", tokyo.getItineraryId());
            response.put("days", tokyo.getDays().size());
            response.put("totalNodes", tokyo.getDays().stream()
                .mapToInt(day -> day.getNodes().size())
                .sum());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to generate Tokyo sample", e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
}
