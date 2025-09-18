package com.tripplanner.service;

import com.tripplanner.api.ItinerariesController;
import com.tripplanner.api.dto.CreateItineraryReq;
import com.tripplanner.api.dto.ItineraryDto;
import com.tripplanner.data.entity.Itinerary;
import com.tripplanner.data.repo.ItineraryRepository;
import com.tripplanner.security.GoogleUserPrincipal;
import com.tripplanner.service.agents.AgentOrchestrator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * Service for itinerary operations.
 */
@Service
@org.springframework.boot.autoconfigure.condition.ConditionalOnBean(ItineraryRepository.class)
public class ItineraryService {
    
    private static final Logger logger = LoggerFactory.getLogger(ItineraryService.class);
    
    private final ItineraryRepository itineraryRepository;
    private final AgentEventBus agentEventBus;
    private final AgentOrchestrator agentOrchestrator;
    
    public ItineraryService(ItineraryRepository itineraryRepository,
                          AgentEventBus agentEventBus,
                          AgentOrchestrator agentOrchestrator) {
        this.itineraryRepository = itineraryRepository;
        this.agentEventBus = agentEventBus;
        this.agentOrchestrator = agentOrchestrator;
    }
    
    /**
     * Create a new itinerary.
     */
    public ItineraryDto create(CreateItineraryReq request, GoogleUserPrincipal user) {
        logger.info("=== CREATE ITINERARY REQUEST ===");
        logger.info("User ID: {}", user.getUserId());
        logger.info("User Email: {}", user.getEmail());
        logger.info("Request Details:");
        logger.info("  Destination: {}", request.destination());
        logger.info("  Start Date: {}", request.startDate());
        logger.info("  End Date: {}", request.endDate());
        logger.info("  Duration: {} days", request.getDurationDays());
        logger.info("  Budget Tier: {}", request.budgetTier());
        logger.info("  Language: {}", request.language());
        if (request.party() != null) {
            logger.info("  Party: {} adults, {} children, {} infants, {} rooms", 
                       request.party().adults(), request.party().children(), 
                       request.party().infants(), request.party().rooms());
        }
        if (request.interests() != null && !request.interests().isEmpty()) {
            logger.info("  Interests: {}", String.join(", ", request.interests()));
        }
        if (request.constraints() != null && !request.constraints().isEmpty()) {
            logger.info("  Constraints: {}", String.join(", ", request.constraints()));
        }
        
        try {
            // Create itinerary entity
            Itinerary itinerary = new Itinerary();
            itinerary.setUserId(user.getUserId());
            itinerary.setDestination(request.destination());
            itinerary.setStartDate(request.startDate());
            itinerary.setEndDate(request.endDate());
            
            // Set party information
            if (request.party() != null) {
                Itinerary.Party party = new Itinerary.Party(
                        request.party().adults(),
                        request.party().children(),
                        request.party().infants(),
                        request.party().rooms()
                );
                itinerary.setParty(party);
            }
            
            itinerary.setBudgetTier(request.budgetTier());
            itinerary.setInterests(request.interests());
            itinerary.setConstraints(request.constraints());
            itinerary.setLanguage(request.language());
            itinerary.setStatus("generating");
            
            // Save to database
            itinerary = itineraryRepository.save(itinerary);
            
            // Make itinerary effectively final for lambda
            final Itinerary finalItinerary = itinerary;
            
            // Start agent orchestration process asynchronously
            CompletableFuture<ItineraryDto> orchestrationFuture = agentOrchestrator.generateItinerary(finalItinerary.getId(), request);
            
            // Handle orchestration completion/failure (async)
            orchestrationFuture.whenComplete((result, throwable) -> {
                if (throwable != null) {
                    logger.error("Agent orchestration failed for itinerary: {}", finalItinerary.getId(), throwable);
                    try {
                        itineraryRepository.updateStatus(finalItinerary.getId(), "failed");
                    } catch (Exception e) {
                        logger.error("Failed to update itinerary status to failed", e);
                    }
                } else {
                    logger.info("Agent orchestration completed for itinerary: {}", finalItinerary.getId());
                }
            });
            
            ItineraryDto result = ItineraryDto.fromEntity(finalItinerary);
            
            logger.info("=== CREATE ITINERARY RESPONSE ===");
            logger.info("Itinerary ID: {}", result.id());
            logger.info("Status: {}", result.status());
            logger.info("Created At: {}", result.createdAt());
            logger.info("Orchestration started: {}", finalItinerary.getId());
            logger.info("=====================================");
            
            return result;
            
        } catch (ExecutionException | InterruptedException e) {
            logger.error("=== CREATE ITINERARY FAILED ===");
            logger.error("User: {}", user.getUserId());
            logger.error("Destination: {}", request.destination());
            logger.error("Error: {}", e.getMessage(), e);
            logger.error("===================================");
            throw new RuntimeException("Failed to create itinerary", e);
        }
    }
    
    /**
     * Get an itinerary by ID.
     */
    public ItineraryDto get(String id, GoogleUserPrincipal user) {
        logger.info("=== GET ITINERARY REQUEST ===");
        logger.info("Itinerary ID: {}", id);
        logger.info("User ID: {}", user.getUserId());
        logger.info("User Email: {}", user.getEmail());
        
        try {
            Itinerary itinerary = itineraryRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Itinerary not found: " + id));
            
            // Check ownership
            if (!itinerary.getUserId().equals(user.getUserId())) {
                throw new RuntimeException("Access denied to itinerary: " + id);
            }
            
            ItineraryDto result = ItineraryDto.fromEntity(itinerary);
            
            logger.info("=== GET ITINERARY RESPONSE ===");
            logger.info("Found itinerary: {}", id);
            logger.info("Status: {}", result.status());
            logger.info("Destination: {}", result.destination());
            logger.info("Days count: {}", result.days() != null ? result.days().size() : 0);
            logger.info("==================================");
            
            return result;
            
        } catch (ExecutionException | InterruptedException e) {
            logger.error("=== GET ITINERARY FAILED ===");
            logger.error("Itinerary ID: {}", id);
            logger.error("User: {}", user.getUserId());
            logger.error("Error: {}", e.getMessage(), e);
            logger.error("===============================");
            throw new RuntimeException("Failed to get itinerary", e);
        }
    }
    
    /**
     * Get public itinerary (no authentication required).
     */
    public ItineraryDto getPublic(String id) {
        logger.debug("Getting public itinerary: {}", id);
        
        try {
            Itinerary itinerary = itineraryRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Itinerary not found: " + id));
            
            // Check if itinerary is public
            if (!itinerary.isPublic()) {
                throw new RuntimeException("Itinerary is not public: " + id);
            }
            
            return ItineraryDto.createPublicDto(itinerary);
            
        } catch (ExecutionException | InterruptedException e) {
            logger.error("Failed to get public itinerary: " + id, e);
            throw new RuntimeException("Failed to get public itinerary", e);
        }
    }
    
    /**
     * Save an itinerary (mark as saved/favorite).
     */
    public void save(String id, GoogleUserPrincipal user) {
        logger.info("Saving itinerary: {} for user: {}", id, user.getUserId());
        
        try {
            Itinerary itinerary = itineraryRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Itinerary not found: " + id));
            
            // Check ownership
            if (!itinerary.getUserId().equals(user.getUserId())) {
                throw new RuntimeException("Access denied to itinerary: " + id);
            }
            
            // TODO: Implement save/favorite functionality
            // This could add a 'saved' flag or move to a favorites collection
            
            logger.info("Itinerary saved: {}", id);
            
        } catch (ExecutionException | InterruptedException e) {
            logger.error("Failed to save itinerary: " + id, e);
            throw new RuntimeException("Failed to save itinerary", e);
        }
    }
    
    /**
     * Get user's itineraries.
     */
    public List<ItineraryDto> getUserItineraries(GoogleUserPrincipal user, int page, int size) {
        logger.debug("Getting itineraries for user: {}", user.getUserId());
        
        try {
            List<Itinerary> itineraries = itineraryRepository.findByUserId(user.getUserId(), size);
            
            return itineraries.stream()
                    .map(ItineraryDto::fromEntity)
                    .toList();
            
        } catch (ExecutionException | InterruptedException e) {
            logger.error("Failed to get user itineraries", e);
            throw new RuntimeException("Failed to get user itineraries", e);
        }
    }
    
    /**
     * Delete an itinerary.
     */
    public void delete(String id, GoogleUserPrincipal user) {
        logger.info("Deleting itinerary: {} for user: {}", id, user.getUserId());
        
        try {
            Itinerary itinerary = itineraryRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Itinerary not found: " + id));
            
            // Check ownership
            if (!itinerary.getUserId().equals(user.getUserId())) {
                throw new RuntimeException("Access denied to itinerary: " + id);
            }
            
            itineraryRepository.deleteById(id);
            
            logger.info("Itinerary deleted: {}", id);
            
        } catch (ExecutionException | InterruptedException e) {
            logger.error("Failed to delete itinerary: " + id, e);
            throw new RuntimeException("Failed to delete itinerary", e);
        }
    }
    
    /**
     * Share an itinerary (make it public).
     */
    public ItinerariesController.ShareResponse share(String id, GoogleUserPrincipal user) {
        logger.info("Sharing itinerary: {} for user: {}", id, user.getUserId());
        
        try {
            Itinerary itinerary = itineraryRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Itinerary not found: " + id));
            
            // Check ownership
            if (!itinerary.getUserId().equals(user.getUserId())) {
                throw new RuntimeException("Access denied to itinerary: " + id);
            }
            
            // Generate share token if not exists
            String shareToken = itinerary.getShareToken();
            if (shareToken == null || shareToken.isEmpty()) {
                shareToken = UUID.randomUUID().toString();
            }
            
            // Update sharing settings
            itineraryRepository.updateSharingSettings(id, true, shareToken);
            
            String publicUrl = "/itineraries/" + id + "/public";
            
            logger.info("Itinerary shared: {} with token: {}", id, shareToken);
            return new ItinerariesController.ShareResponse(shareToken, publicUrl);
            
        } catch (ExecutionException | InterruptedException e) {
            logger.error("Failed to share itinerary: " + id, e);
            throw new RuntimeException("Failed to share itinerary", e);
        }
    }
    
    /**
     * Unshare an itinerary (make it private).
     */
    public void unshare(String id, GoogleUserPrincipal user) {
        logger.info("Unsharing itinerary: {} for user: {}", id, user.getUserId());
        
        try {
            Itinerary itinerary = itineraryRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Itinerary not found: " + id));
            
            // Check ownership
            if (!itinerary.getUserId().equals(user.getUserId())) {
                throw new RuntimeException("Access denied to itinerary: " + id);
            }
            
            // Update sharing settings
            itineraryRepository.updateSharingSettings(id, false, null);
            
            logger.info("Itinerary unshared: {}", id);
            
        } catch (ExecutionException | InterruptedException e) {
            logger.error("Failed to unshare itinerary: " + id, e);
            throw new RuntimeException("Failed to unshare itinerary", e);
        }
    }
}
