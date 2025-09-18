package com.tripplanner.service.agents;

import com.tripplanner.api.dto.AgentEvent;
import com.tripplanner.api.dto.CreateItineraryReq;
import com.tripplanner.api.dto.ItineraryDto;
import com.tripplanner.data.entity.Itinerary;
import com.tripplanner.data.repo.ItineraryRepository;
import com.tripplanner.service.AgentEventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Orchestrates the execution of multiple agents to generate complete itineraries.
 */
@Service
@org.springframework.boot.autoconfigure.condition.ConditionalOnBean(ItineraryRepository.class)
public class AgentOrchestrator {
    
    private static final Logger logger = LoggerFactory.getLogger(AgentOrchestrator.class);
    
    private final AgentEventBus eventBus;
    private final ItineraryRepository itineraryRepository;
    private final PlannerAgent plannerAgent;
    private final PlacesAgent placesAgent;
    private final ExecutorService executorService;
    
    public AgentOrchestrator(AgentEventBus eventBus,
                           ItineraryRepository itineraryRepository,
                           PlannerAgent plannerAgent,
                           PlacesAgent placesAgent) {
        this.eventBus = eventBus;
        this.itineraryRepository = itineraryRepository;
        this.plannerAgent = plannerAgent;
        this.placesAgent = placesAgent;
        this.executorService = Executors.newFixedThreadPool(6); // For parallel agent execution
    }
    
    /**
     * Orchestrate the generation of a complete itinerary using multiple agents.
     */
    @Async
    public CompletableFuture<ItineraryDto> generateItinerary(String itineraryId, CreateItineraryReq request) {
        logger.info("=== AGENT ORCHESTRATION START ===");
        logger.info("Itinerary ID: {}", itineraryId);
        logger.info("Destination: {}", request.destination());
        logger.info("Duration: {} days", request.getDurationDays());
        logger.info("Budget Tier: {}", request.budgetTier());
        logger.info("Interests: {}", request.interests());
        logger.info("Language: {}", request.language());
        logger.info("================================");
        
        try {
            // Update itinerary status to generating
            updateItineraryStatus(itineraryId, "generating");
            
            // Emit orchestrator start event
            emitOrchestratorEvent(itineraryId, AgentEvent.AgentStatus.running, 0, 
                               "Starting itinerary generation", "orchestration_start");
            
            // Phase 1: Run independent agents in parallel
            CompletableFuture<Void> phase1 = runPhase1Agents(itineraryId, request);
            
            // Phase 2: Run dependent agents after phase 1 completes
            CompletableFuture<ItineraryDto> result = phase1.thenCompose(v -> runPhase2Agents(itineraryId, request));
            
            // Handle completion
            return result.thenApply(itinerary -> {
                updateItineraryStatus(itineraryId, "completed");
                emitOrchestratorEvent(itineraryId, AgentEvent.AgentStatus.succeeded, 100, 
                                   "Itinerary generation completed", "orchestration_complete");
                
                logger.info("=== AGENT ORCHESTRATION COMPLETED ===");
                logger.info("Itinerary ID: {}", itineraryId);
                logger.info("Final Status: completed");
                logger.info("Result: {}", itinerary.id());
                logger.info("====================================");
                
                return itinerary;
            }).exceptionally(throwable -> {
                updateItineraryStatus(itineraryId, "failed");
                emitOrchestratorEvent(itineraryId, AgentEvent.AgentStatus.failed, 0, 
                                   "Itinerary generation failed: " + throwable.getMessage(), "orchestration_error");
                
                logger.error("=== AGENT ORCHESTRATION FAILED ===");
                logger.error("Itinerary ID: {}", itineraryId);
                logger.error("Error: {}", throwable.getMessage(), throwable);
                logger.error("=================================");
                
                throw new RuntimeException("Itinerary generation failed", throwable);
            });
            
        } catch (Exception e) {
            logger.error("Failed to start itinerary generation for: {}", itineraryId, e);
            updateItineraryStatus(itineraryId, "failed");
            return CompletableFuture.failedFuture(e);
        }
    }
    
    /**
     * Phase 1: Run independent agents in parallel.
     */
    private CompletableFuture<Void> runPhase1Agents(String itineraryId, CreateItineraryReq request) {
        emitOrchestratorEvent(itineraryId, AgentEvent.AgentStatus.running, 20, 
                           "Starting Phase 1: Independent agents", "phase1_start");
        
        // Create agent requests
        PlacesAgent.PlacesRequest placesRequest = new PlacesAgent.PlacesRequest(
                request.destination(),
                request.getDurationDays(),
                request.interests() != null ? request.interests().toArray(new String[0]) : new String[0],
                request.budgetTier()
        );
        
        // Run independent agents in parallel
        CompletableFuture<PlacesAgent.PlacesResponse> placesTask = CompletableFuture.supplyAsync(() -> 
                placesAgent.execute(itineraryId, new BaseAgent.AgentRequest<>(placesRequest, PlacesAgent.PlacesResponse.class)),
                executorService
        );
        
        // Add more independent agents here (flights, restaurants, transit)
        CompletableFuture<Void> flightsTask = simulateAgentExecution(itineraryId, AgentEvent.AgentKind.flights, "Fares & hold eligibility");
        CompletableFuture<Void> restaurantsTask = simulateAgentExecution(itineraryId, AgentEvent.AgentKind.food, "Ratings, cost, cuisine near heatmap zones");
        CompletableFuture<Void> transitTask = simulateAgentExecution(itineraryId, AgentEvent.AgentKind.pt, "Passes, transfers, travel times");
        
        return CompletableFuture.allOf(placesTask, flightsTask, restaurantsTask, transitTask)
                .thenRun(() -> {
                    emitOrchestratorEvent(itineraryId, AgentEvent.AgentStatus.running, 60, 
                                       "Phase 1 completed", "phase1_complete");
                });
    }
    
    /**
     * Phase 2: Run dependent agents after phase 1 completes.
     */
    private CompletableFuture<ItineraryDto> runPhase2Agents(String itineraryId, CreateItineraryReq request) {
        emitOrchestratorEvent(itineraryId, AgentEvent.AgentStatus.running, 70, 
                           "Starting Phase 2: Dependent agents", "phase2_start");
        
        // Run hotels agent (depends on places)
        CompletableFuture<Void> hotelsTask = simulateAgentExecution(itineraryId, AgentEvent.AgentKind.hotels, 
                "Hotels ≥★4.2 near key areas; Standard Room from $120/night");
        
        // Run planner agent (depends on places)
        CompletableFuture<ItineraryDto> plannerTask = CompletableFuture.supplyAsync(() -> {
            try {
                // For now, simulate planner execution and return a basic ItineraryDto
                simulateAgentExecution(itineraryId, AgentEvent.AgentKind.planner, 
                        "Drafted itinerary with day-by-day activities").join();
                
                // Create a basic ItineraryDto response
                return createBasicItineraryDto(itineraryId, request);
            } catch (Exception e) {
                throw new RuntimeException("Planner agent failed", e);
            }
        }, executorService);
        
        return CompletableFuture.allOf(hotelsTask).thenCompose(v -> plannerTask);
    }
    
    /**
     * Simulate agent execution for agents not yet fully implemented.
     */
    private CompletableFuture<Void> simulateAgentExecution(String itineraryId, AgentEvent.AgentKind kind, String completionMessage) {
        return CompletableFuture.runAsync(() -> {
            String agentId = java.util.UUID.randomUUID().toString();
            
            // Emit running event
            emitAgentEvent(itineraryId, agentId, kind, AgentEvent.AgentStatus.running, 0, "Agent started", null);
            
            try {
                // Simulate work with progress updates
                for (int progress = 20; progress <= 100; progress += 20) {
                    Thread.sleep(500); // Simulate work
                    emitAgentEvent(itineraryId, agentId, kind, AgentEvent.AgentStatus.running, progress, 
                                 "Processing...", null);
                }
                
                // Emit completion
                emitAgentEvent(itineraryId, agentId, kind, AgentEvent.AgentStatus.succeeded, 100, 
                             completionMessage, null);
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                emitAgentEvent(itineraryId, agentId, kind, AgentEvent.AgentStatus.failed, 0, 
                             "Agent interrupted", null);
                throw new RuntimeException("Agent execution interrupted", e);
            }
        }, executorService);
    }
    
    /**
     * Create a basic ItineraryDto for testing purposes.
     */
    private ItineraryDto createBasicItineraryDto(String itineraryId, CreateItineraryReq request) {
        // This is a placeholder - in a real implementation, this would be built from agent results
        return new ItineraryDto(
                itineraryId,
                request.destination(),
                request.startDate(),
                request.endDate(),
                request.party(),
                request.budgetTier(),
                request.interests(),
                request.constraints(),
                request.language(),
                "Your personalized itinerary for " + request.destination(),
                null, // map
                null, // days - would be populated by agents
                "completed",
                java.time.Instant.now(),
                java.time.Instant.now(),
                false,
                null
        );
    }
    
    /**
     * Update itinerary status in database.
     */
    private void updateItineraryStatus(String itineraryId, String status) {
        try {
            itineraryRepository.updateStatus(itineraryId, status);
        } catch (Exception e) {
            logger.error("Failed to update itinerary status for: {}", itineraryId, e);
        }
    }
    
    /**
     * Emit orchestrator event.
     */
    private void emitOrchestratorEvent(String itineraryId, AgentEvent.AgentStatus status, 
                                     int progress, String message, String step) {
        emitAgentEvent(itineraryId, "orchestrator", AgentEvent.AgentKind.orchestrator, 
                      status, progress, message, step);
    }
    
    /**
     * Emit agent event.
     */
    private void emitAgentEvent(String itineraryId, String agentId, AgentEvent.AgentKind kind,
                              AgentEvent.AgentStatus status, int progress, String message, String step) {
        AgentEvent event = new AgentEvent(agentId, kind, status, progress, message, step, 
                                        java.time.Instant.now(), itineraryId);
        eventBus.publish(itineraryId, event);
    }
}

