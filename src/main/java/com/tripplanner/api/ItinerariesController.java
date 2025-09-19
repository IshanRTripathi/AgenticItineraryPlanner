package com.tripplanner.api;

import com.tripplanner.api.dto.*;
import com.tripplanner.service.ItineraryService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for itinerary operations.
 */
@RestController
@RequestMapping("/api/v1/itineraries")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class ItinerariesController {
    
    private static final Logger logger = LoggerFactory.getLogger(ItinerariesController.class);
    
    private final ItineraryService itineraryService;
    
    public ItinerariesController(ItineraryService itineraryService) {
        this.itineraryService = itineraryService;
    }
    
    /**
     * Create a new itinerary.
     */
    @PostMapping
    public ResponseEntity<ItineraryDto> create(@Valid @RequestBody CreateItineraryReq request) {
        logger.info("Creating itinerary");
        logger.info("Request: {}", request);
        
        try {
            ItineraryDto itinerary = itineraryService.create(request);
            
            logger.info("Itinerary created successfully: {}", itinerary.getId());
            return ResponseEntity.ok(itinerary);
            
        } catch (Exception e) {
            logger.error("Failed to create itinerary", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get all itineraries.
     */
    @GetMapping
    public ResponseEntity<List<ItineraryDto>> getAll() {
        logger.info("Getting all itineraries");
        
        try {
            List<ItineraryDto> itineraries = itineraryService.getUserItineraries(0, 10);
            
            logger.info("Found {} itineraries", itineraries.size());
            return ResponseEntity.ok(itineraries);
            
        } catch (Exception e) {
            logger.error("Failed to get itineraries", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get itinerary by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ItineraryDto> getById(@PathVariable String id) {
        logger.info("Getting itinerary: {}", id);
        
        try {
            ItineraryDto itinerary = itineraryService.get(id);
            
            if (itinerary != null) {
                logger.info("Itinerary found: {}", itinerary.getId());
                return ResponseEntity.ok(itinerary);
            } else {
                logger.warn("Itinerary not found: {}", id);
                return ResponseEntity.notFound().build();
            }
            
        } catch (Exception e) {
            logger.error("Failed to get itinerary: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Delete itinerary.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        logger.info("Deleting itinerary: {}", id);
        
        try {
            itineraryService.delete(id);
            
            logger.info("Itinerary deleted successfully: {}", id);
            return ResponseEntity.noContent().build();
            
        } catch (Exception e) {
            logger.error("Failed to delete itinerary: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}