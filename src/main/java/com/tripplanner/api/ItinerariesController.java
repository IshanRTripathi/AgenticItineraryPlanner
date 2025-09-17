package com.tripplanner.api;

import com.tripplanner.api.dto.*;
import com.tripplanner.security.GoogleUserPrincipal;
import com.tripplanner.service.ItineraryService;
import com.tripplanner.service.ReviseService;
import com.tripplanner.service.ExtendService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

/**
 * REST controller for itinerary operations.
 */
@RestController
@RequestMapping("/itineraries")
public class ItinerariesController {
    
    private static final Logger logger = LoggerFactory.getLogger(ItinerariesController.class);
    
    private final ItineraryService itineraryService;
    private final ReviseService reviseService;
    private final ExtendService extendService;
    
    public ItinerariesController(ItineraryService itineraryService,
                               ReviseService reviseService,
                               ExtendService extendService) {
        this.itineraryService = itineraryService;
        this.reviseService = reviseService;
        this.extendService = extendService;
    }
    
    /**
     * Create a new itinerary.
     */
    @PostMapping
    public ResponseEntity<ItineraryDto> create(@Valid @RequestBody CreateItineraryReq request,
                                             @AuthenticationPrincipal GoogleUserPrincipal user) {
        logger.info("Creating itinerary for user: {} to destination: {}", 
                   user.getUserId(), request.destination());
        
        ItineraryDto itinerary = itineraryService.create(request, user);
        
        logger.info("Itinerary created with ID: {}", itinerary.id());
        return ResponseEntity.ok(itinerary);
    }
    
    /**
     * Get an itinerary by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ItineraryDto> get(@PathVariable String id,
                                          @AuthenticationPrincipal GoogleUserPrincipal user) {
        logger.debug("Getting itinerary: {} for user: {}", id, user.getUserId());
        
        ItineraryDto itinerary = itineraryService.get(id, user);
        return ResponseEntity.ok(itinerary);
    }
    
    /**
     * Get public itinerary (no authentication required).
     */
    @GetMapping("/{id}/public")
    public ResponseEntity<ItineraryDto> getPublic(@PathVariable String id) {
        logger.debug("Getting public itinerary: {}", id);
        
        ItineraryDto itinerary = itineraryService.getPublic(id);
        return ResponseEntity.ok(itinerary);
    }
    
    /**
     * Revise an existing itinerary.
     */
    @PostMapping("/{id}:revise")
    public ResponseEntity<ReviseRes> revise(@PathVariable String id,
                                          @Valid @RequestBody ReviseReq request,
                                          @AuthenticationPrincipal GoogleUserPrincipal user) {
        logger.info("Revising itinerary: {} for user: {}", id, user.getUserId());
        
        ReviseRes result = reviseService.revise(id, request, user);
        
        logger.info("Itinerary revised: {}", id);
        return ResponseEntity.ok(result);
    }
    
    /**
     * Extend an existing itinerary.
     */
    @PostMapping("/{id}:extend")
    public ResponseEntity<ItineraryDto> extend(@PathVariable String id,
                                             @Valid @RequestBody ExtendReq request,
                                             @AuthenticationPrincipal GoogleUserPrincipal user) {
        logger.info("Extending itinerary: {} by {} days for user: {}", 
                   id, request.days(), user.getUserId());
        
        ItineraryDto itinerary = extendService.extend(id, request, user);
        
        logger.info("Itinerary extended: {}", id);
        return ResponseEntity.ok(itinerary);
    }
    
    /**
     * Save an itinerary (mark as saved/favorite).
     */
    @PostMapping("/{id}:save")
    public ResponseEntity<Void> save(@PathVariable String id,
                                   @AuthenticationPrincipal GoogleUserPrincipal user) {
        logger.info("Saving itinerary: {} for user: {}", id, user.getUserId());
        
        itineraryService.save(id, user);
        
        logger.info("Itinerary saved: {}", id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
    
    /**
     * Get user's itineraries.
     */
    @GetMapping
    public ResponseEntity<java.util.List<ItineraryDto>> getUserItineraries(
            @AuthenticationPrincipal GoogleUserPrincipal user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        logger.debug("Getting itineraries for user: {}, page: {}, size: {}", 
                    user.getUserId(), page, size);
        
        java.util.List<ItineraryDto> itineraries = itineraryService.getUserItineraries(user, page, size);
        
        return ResponseEntity.ok(itineraries);
    }
    
    /**
     * Delete an itinerary.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id,
                                     @AuthenticationPrincipal GoogleUserPrincipal user) {
        logger.info("Deleting itinerary: {} for user: {}", id, user.getUserId());
        
        itineraryService.delete(id, user);
        
        logger.info("Itinerary deleted: {}", id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
    
    /**
     * Share an itinerary (make it public).
     */
    @PostMapping("/{id}:share")
    public ResponseEntity<ShareResponse> share(@PathVariable String id,
                                             @AuthenticationPrincipal GoogleUserPrincipal user) {
        logger.info("Sharing itinerary: {} for user: {}", id, user.getUserId());
        
        ShareResponse shareResponse = itineraryService.share(id, user);
        
        logger.info("Itinerary shared: {} with token: {}", id, shareResponse.shareToken());
        return ResponseEntity.ok(shareResponse);
    }
    
    /**
     * Unshare an itinerary (make it private).
     */
    @PostMapping("/{id}:unshare")
    public ResponseEntity<Void> unshare(@PathVariable String id,
                                      @AuthenticationPrincipal GoogleUserPrincipal user) {
        logger.info("Unsharing itinerary: {} for user: {}", id, user.getUserId());
        
        itineraryService.unshare(id, user);
        
        logger.info("Itinerary unshared: {}", id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
    
    /**
     * Response DTO for share operation.
     */
    public record ShareResponse(String shareToken, String publicUrl) {}
}
