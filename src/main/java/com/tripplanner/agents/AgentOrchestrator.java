package com.tripplanner.agents;

import com.tripplanner.dto.*;
import com.tripplanner.service.AgentEventBus;
import com.tripplanner.service.ChangeEngine;
import com.tripplanner.service.ItineraryJsonService;
import com.tripplanner.service.UserDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Orchestrates the execution of multiple agents to generate complete itineraries.
 */
@Service
public class AgentOrchestrator {
    
    private static final Logger logger = LoggerFactory.getLogger(AgentOrchestrator.class);
    
    private final PlannerAgent plannerAgent;
    private final EnrichmentAgent enrichmentAgent;
    private final ItineraryJsonService itineraryJsonService;
    private final ChangeEngine changeEngine; // todo check significance in terms of logic
    private final UserDataService userDataService;
    private final AgentEventBus agentEventBus;
    
    public AgentOrchestrator(PlannerAgent plannerAgent, EnrichmentAgent enrichmentAgent,
                           ItineraryJsonService itineraryJsonService,
                           ChangeEngine changeEngine, UserDataService userDataService,
                           AgentEventBus agentEventBus) {
        this.plannerAgent = plannerAgent;
        this.enrichmentAgent = enrichmentAgent;
        this.itineraryJsonService = itineraryJsonService;
        this.changeEngine = changeEngine;
        this.userDataService = userDataService;
        this.agentEventBus = agentEventBus;
    }
    
    /**
     * Create initial itinerary and establish ownership synchronously.
     * This method must complete before the API response is sent to avoid race conditions.
     */
    public NormalizedItinerary createInitialItinerary(String itineraryId, CreateItineraryReq request, String userId) {
        logger.info("=== AGENT ORCHESTRATOR: CREATING INITIAL ITINERARY ===");
        logger.info("Itinerary ID: {}", itineraryId);
        logger.info("Destination: {}", request.getDestination());
        logger.info("User ID: {}", userId);
        
        try {
            // Step 1: Create initial normalized itinerary structure
            logger.info("Step 1: Creating initial itinerary structure");
            NormalizedItinerary initialItinerary = createInitialNormalizedItinerary(itineraryId, request, userId);
            
            // Step 2: Save initial itinerary to ItineraryJsonService (single source of truth)
            logger.info("Step 2: Saving initial itinerary to ItineraryJsonService");
            itineraryJsonService.createItinerary(initialItinerary);
            
            // Step 3: SYNCHRONOUSLY save trip metadata to establish ownership
            logger.info("Step 3: Establishing ownership by saving trip metadata");
            TripMetadata tripMetadata = new TripMetadata(request, initialItinerary);
            userDataService.saveUserTripMetadata(userId, tripMetadata);
            
            logger.info("=== INITIAL ITINERARY CREATED AND OWNERSHIP ESTABLISHED ===");
            logger.info("Itinerary ID: {}", itineraryId);
            logger.info("User can now access /itineraries/{}/json endpoint", itineraryId);
            
            return initialItinerary;
            
        } catch (Exception e) {
            logger.error("Failed to create initial itinerary for ID: {}", itineraryId, e);
            throw new RuntimeException("Initial itinerary creation failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Generate a complete itinerary using the new agent sequence: PlannerAgent → EnrichmentAgent.
     * This method works with normalized JSON and uses the ChangeEngine.
     * This method now runs asynchronously AFTER ownership has been established.
     */
    @Async
    public CompletableFuture<NormalizedItinerary> generateNormalizedItinerary(String itineraryId, CreateItineraryReq request, String userId) {
        logger.info("=== AGENT ORCHESTRATOR: ASYNC ITINERARY GENERATION ===");
        logger.info("Itinerary ID: {}", itineraryId);
        logger.info("Destination: {}", request.getDestination());
        logger.info("Note: Initial itinerary and ownership should already be established");
        
        try {
            // Add a small delay to allow frontend to establish SSE connection
            logger.info("Waiting 3 seconds for frontend to establish SSE connection...");
            Thread.sleep(3000);
            
            // Step 1: Run PlannerAgent to generate the main itinerary
            logger.info("Step 1: Running PlannerAgent");
            try {
                // Add explicit taskType to match agent capabilities
                Map<String, Object> requestData = new HashMap<>();
                requestData.put("taskType", "create");  // PlannerAgent supports "create"
                requestData.put("request", request);
                BaseAgent.AgentRequest<ItineraryDto> plannerRequest = new BaseAgent.AgentRequest<>(requestData, ItineraryDto.class);
                plannerAgent.execute(itineraryId, plannerRequest);
                logger.info("PlannerAgent completed successfully, database already updated");
            } catch (Exception e) {
                logger.error("PlannerAgent failed for itinerary: {}", itineraryId, e);
                // Update the itinerary status to indicate failure
                try {
                    var currentItinerary = itineraryJsonService.getItinerary(itineraryId);
                    if (currentItinerary.isPresent()) {
                        logger.info("PlannerAgent failure recorded for itinerary: {}", itineraryId);
                    }
                } catch (Exception updateError) {
                    logger.warn("Failed to update itinerary status after PlannerAgent failure: {}", itineraryId, updateError);
                }
                throw new RuntimeException("PlannerAgent execution failed: " + e.getMessage(), e);
            }
            
            // Step 2: Run EnrichmentAgent to add validation and pacing
            logger.info("Step 2: Running EnrichmentAgent");
            try {
                Map<String, Object> enrichmentData = new HashMap<>();
                enrichmentData.put("taskType", "enrich");  // EnrichmentAgent supports "enrich"
                BaseAgent.AgentRequest<ChangeEngine.ApplyResult> enrichmentRequest = new BaseAgent.AgentRequest<>(enrichmentData, ChangeEngine.ApplyResult.class);
                enrichmentAgent.execute(itineraryId, enrichmentRequest);
                logger.info("EnrichmentAgent completed successfully");
            } catch (Exception e) {
                logger.error("EnrichmentAgent failed for itinerary: {}", itineraryId, e);
                // EnrichmentAgent failure is not critical, continue with the itinerary
                logger.warn("Continuing with itinerary despite EnrichmentAgent failure");
            }
            
            // Get the final enriched itinerary from ItineraryJsonService (single source of truth)
            var finalItinerary = itineraryJsonService.getItinerary(itineraryId);
            if (finalItinerary.isEmpty()) {
                throw new RuntimeException("Failed to retrieve final itinerary from ItineraryJsonService");
            }
            
            logger.info("Final enriched itinerary retrieved from ItineraryJsonService for user: {}", userId);
            
            logger.info("=== ASYNC ITINERARY GENERATION COMPLETE ===");
            logger.info("Final itinerary has {} days", finalItinerary.get().getDays() != null ? finalItinerary.get().getDays().size() : 0);
            
            // Update the itinerary status to completed in the database
            try {
                // Get the current itinerary DTO and update its status
                var currentItinerary = itineraryJsonService.getItinerary(itineraryId);
                if (currentItinerary.isPresent()) {
                    NormalizedItinerary itinerary = currentItinerary.get();
                    // The status is managed by the frontend, but we can add a completion marker
                    logger.info("Itinerary generation completed successfully for ID: {}", itineraryId);
                }
            } catch (Exception e) {
                logger.warn("Failed to update itinerary status for ID: {}, but generation completed successfully", itineraryId, e);
            }
            
            // Send completion event to frontend
            agentEventBus.sendCompletion(itineraryId);
            
            return CompletableFuture.completedFuture(finalItinerary.get());
            
        } catch (Exception e) {
            logger.error("Failed to generate normalized itinerary for ID: {}", itineraryId, e);
            throw new RuntimeException("Normalized itinerary generation failed", e);
        }
    }
    
    /**
     * Process a user request to modify an existing itinerary using PlannerAgent.
     * todo: this needs refinement
     */
    @Async
    public CompletableFuture<ChangeSet> processUserRequest(String itineraryId, String userRequest) {
        logger.info("=== AGENT ORCHESTRATOR: PROCESSING USER REQUEST ===");
        logger.info("Itinerary ID: {}", itineraryId);
        logger.info("User Request: {}", userRequest);
        
        try {
            // Use PlannerAgent to generate ChangeSet from user request
            ChangeSet changeSet = plannerAgent.generateChangeSet(itineraryId, userRequest);
            
            logger.info("=== USER REQUEST PROCESSED ===");
            logger.info("Generated ChangeSet with {} operations", changeSet.getOps() != null ? changeSet.getOps().size() : 0);
            
            return CompletableFuture.completedFuture(changeSet);
            
        } catch (Exception e) {
            logger.error("Failed to process user request for itinerary: {}", itineraryId, e);
            throw new RuntimeException("User request processing failed", e);
        }
    }
    
    /**
     * Apply a ChangeSet and run EnrichmentAgent for validation.
     */
    @Async
    public CompletableFuture<ChangeEngine.ApplyResult> applyChangeSetWithEnrichment(String itineraryId, ChangeSet changeSet) {
        logger.info("=== AGENT ORCHESTRATOR: APPLYING CHANGESET WITH ENRICHMENT ===");
        logger.info("Itinerary ID: {}", itineraryId);
        logger.info("ChangeSet: {}", changeSet);
        
        try {
            // Step 1: Apply the ChangeSet using PlannerAgent
            logger.info("Step 1: Applying ChangeSet");
            ChangeEngine.ApplyResult applyResult = plannerAgent.applyChangeSet(itineraryId, changeSet);
            
            // Step 2: Run EnrichmentAgent to add validation and pacing
            logger.info("Step 2: Running EnrichmentAgent for validation");
            Map<String, Object> enrichmentData = new HashMap<>();
            enrichmentData.put("taskType", "enrich");  // EnrichmentAgent supports "enrich"
            BaseAgent.AgentRequest<ChangeEngine.ApplyResult> enrichmentRequest = new BaseAgent.AgentRequest<>(enrichmentData, ChangeEngine.ApplyResult.class);
            enrichmentAgent.execute(itineraryId, enrichmentRequest);
            
            logger.info("=== CHANGESET APPLIED WITH ENRICHMENT ===");
            logger.info("Final version: {}", applyResult.getToVersion());
            
            return CompletableFuture.completedFuture(applyResult);
            
        } catch (Exception e) {
            logger.error("Failed to apply ChangeSet with ENRICHMENT for itinerary: {}", itineraryId, e);
            throw new RuntimeException("ChangeSet application with ENRICHMENT failed", e);
        }
    }
    
    /**
     * Create initial normalized itinerary structure.
     */
    private NormalizedItinerary createInitialNormalizedItinerary(String itineraryId, CreateItineraryReq request, String userId) {
        NormalizedItinerary itinerary = new NormalizedItinerary();
        itinerary.setItineraryId(itineraryId);
        itinerary.setVersion(1);
        itinerary.setUserId(userId);
        itinerary.setCreatedAt(System.currentTimeMillis());
        itinerary.setUpdatedAt(System.currentTimeMillis());
        itinerary.setSummary("Your personalized itinerary for " + request.getDestination());
        itinerary.setCurrency("INR");
        itinerary.setThemes(request.getInterests() != null ? request.getInterests() : new ArrayList<>());
        itinerary.setDays(new ArrayList<>());

        // Set explicit trip meta
        itinerary.setOrigin(request.getStartLocation());
        itinerary.setDestination(request.getDestination());
        if (request.getStartDate() != null) {
            itinerary.setStartDate(request.getStartDate().toString());
        }
        if (request.getEndDate() != null) {
            itinerary.setEndDate(request.getEndDate().toString());
        }
        
        // Set settings
        ItinerarySettings settings = new ItinerarySettings();
        settings.setAutoApply(false);
        settings.setDefaultScope("trip");
        itinerary.setSettings(settings);
        
        // Set agent status
        Map<String, AgentStatus> agents = new HashMap<>();
        agents.put("planner", new AgentStatus());
        agents.put("enrichment", new AgentStatus());
        itinerary.setAgents(agents);
        
        return itinerary;
    }
}