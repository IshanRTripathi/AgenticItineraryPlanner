package com.tripplanner.api;

import com.tripplanner.api.dto.*;
import com.tripplanner.service.ChangeEngine;
import com.tripplanner.service.ItineraryJsonService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;

/**
 * Test controller for ChangeEngine functionality.
 * This will be removed once the main implementation is complete.
 */
@RestController
@RequestMapping("/api/v1/test/change")
public class TestChangeEngineController {
    
    private static final Logger logger = LoggerFactory.getLogger(TestChangeEngineController.class);
    
    private final ChangeEngine changeEngine;
    private final ItineraryJsonService itineraryJsonService;
    
    public TestChangeEngineController(ChangeEngine changeEngine, ItineraryJsonService itineraryJsonService) {
        this.changeEngine = changeEngine;
        this.itineraryJsonService = itineraryJsonService;
    }
    
    /**
     * Test propose operation.
     */
    @PostMapping("/propose/{itineraryId}")
    public ResponseEntity<ChangeEngine.ProposeResult> testPropose(@PathVariable String itineraryId) {
        logger.info("Testing propose operation for itinerary: {}", itineraryId);
        
        try {
            // Create a test ChangeSet
            ChangeSet changeSet = createTestChangeSet();
            
            // Propose changes
            ChangeEngine.ProposeResult result = changeEngine.propose(itineraryId, changeSet);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Propose test failed", e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Test apply operation.
     */
    @PostMapping("/apply/{itineraryId}")
    public ResponseEntity<ChangeEngine.ApplyResult> testApply(@PathVariable String itineraryId) {
        logger.info("Testing apply operation for itinerary: {}", itineraryId);
        
        try {
            // Create a test ChangeSet
            ChangeSet changeSet = createTestChangeSet();
            
            // Apply changes
            ChangeEngine.ApplyResult result = changeEngine.apply(itineraryId, changeSet);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Apply test failed", e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Test undo operation.
     */
    @PostMapping("/undo/{itineraryId}/{toVersion}")
    public ResponseEntity<ChangeEngine.UndoResult> testUndo(@PathVariable String itineraryId, 
                                                           @PathVariable Integer toVersion) {
        logger.info("Testing undo operation for itinerary: {} to version: {}", itineraryId, toVersion);
        
        try {
            // Undo changes
            ChangeEngine.UndoResult result = changeEngine.undo(itineraryId, toVersion);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Undo test failed", e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Test complete workflow: propose -> apply -> undo.
     */
    @PostMapping("/workflow/{itineraryId}")
    public ResponseEntity<String> testWorkflow(@PathVariable String itineraryId) {
        logger.info("Testing complete workflow for itinerary: {}", itineraryId);
        
        try {
            // Create a test ChangeSet
            ChangeSet changeSet = createTestChangeSet();
            
            // Step 1: Propose
            ChangeEngine.ProposeResult proposeResult = changeEngine.propose(itineraryId, changeSet);
            logger.info("Propose result: version {}", proposeResult.getPreviewVersion());
            
            // Step 2: Apply
            ChangeEngine.ApplyResult applyResult = changeEngine.apply(itineraryId, changeSet);
            logger.info("Apply result: version {}", applyResult.getToVersion());
            
            // Step 3: Undo (go back to previous version)
            ChangeEngine.UndoResult undoResult = changeEngine.undo(itineraryId, proposeResult.getPreviewVersion() - 1);
            logger.info("Undo result: version {}", undoResult.getToVersion());
            
            return ResponseEntity.ok("Workflow test successful! Final version: " + undoResult.getToVersion());
        } catch (Exception e) {
            logger.error("Workflow test failed", e);
            return ResponseEntity.badRequest().body("Workflow test failed: " + e.getMessage());
        }
    }
    
    /**
     * Create a test ChangeSet for testing.
     */
    private ChangeSet createTestChangeSet() {
        // Create a move operation
        ChangeOperation moveOp = new ChangeOperation(
                "move",
                "n_sagrada",
                Instant.now().plus(1, ChronoUnit.HOURS),
                Instant.now().plus(2, ChronoUnit.HOURS)
        );
        
        // Create preferences
        ChangePreferences preferences = new ChangePreferences(true, false, true);
        
        // Create ChangeSet
        ChangeSet changeSet = new ChangeSet("day", 1, Arrays.asList(moveOp));
        changeSet.setPreferences(preferences);
        
        return changeSet;
    }
}
