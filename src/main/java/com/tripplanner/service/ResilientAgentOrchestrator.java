package com.tripplanner.service;

import com.tripplanner.agents.*;
import com.tripplanner.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Resilient Agent Orchestrator that doesn't depend on orchestrator status detection.
 * Each agent runs independently and the system continues even if orchestrator fails.
 */
@Service
public class ResilientAgentOrchestrator {
    
    private static final Logger logger = LoggerFactory.getLogger(ResilientAgentOrchestrator.class);
    
    private final PlannerAgent plannerAgent;
    private final EnrichmentAgent enrichmentAgent;
    private final PlacesAgent placesAgent;
    private final BookingAgent bookingAgent;
    private final ItineraryJsonService itineraryJsonService;
    private final AgentEventBus agentEventBus;
    private final AgentEventPublisher agentEventPublisher;
    private final ExecutorService agentExecutor;
    
    // Resilience configuration
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MS = 2000;
    private static final long AGENT_TIMEOUT_MS = 300000; // 5 minutes per agent
    
    public ResilientAgentOrchestrator(PlannerAgent plannerAgent, EnrichmentAgent enrichmentAgent,
                                     PlacesAgent placesAgent, BookingAgent bookingAgent,
                                     ItineraryJsonService itineraryJsonService, AgentEventBus agentEventBus,
                                     AgentEventPublisher agentEventPublisher) {
        this.plannerAgent = plannerAgent;
        this.enrichmentAgent = enrichmentAgent;
        this.placesAgent = placesAgent;
        this.bookingAgent = bookingAgent;
        this.itineraryJsonService = itineraryJsonService;
        this.agentEventBus = agentEventBus;
        this.agentEventPublisher = agentEventPublisher;
        
        // Create dedicated thread pool for agent execution
        this.agentExecutor = Executors.newFixedThreadPool(4, r -> {
            Thread t = new Thread(r);
            t.setName("ResilientAgent-" + t.getId());
            t.setDaemon(true);
            return t;
        });
    }
    
    /**
     * Generate itinerary with resilient agent orchestration.
     * Each agent runs independently without waiting for orchestrator status detection.
     */
    @Async
    public CompletableFuture<NormalizedItinerary> generateItineraryResilient(
            String itineraryId, CreateItineraryReq request, String userId) {
        
        logger.info("=== RESILIENT ORCHESTRATOR: STARTING GENERATION ===");
        logger.info("Itinerary ID: {}", itineraryId);
        logger.info("Strategy: Independent agent execution with automatic progression");
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Phase 1: Core Planning (Critical - must succeed)
                NormalizedItinerary itinerary = executeCriticalPhase(itineraryId, request, userId);
                
                // Phase 2: Enhancement (Non-critical - continues even if fails)
                executeEnhancementPhase(itineraryId, itinerary);
                
                // Phase 3: Finalization
                return finalizationPhase(itineraryId);
                
            } catch (Exception e) {
                logger.error("Resilient orchestration failed for itinerary: {}", itineraryId, e);
                throw new RuntimeException("Itinerary generation failed: " + e.getMessage(), e);
            }
        }, agentExecutor);
    }
    
    /**
     * Phase 1: Critical planning phase that must succeed
     */
    private NormalizedItinerary executeCriticalPhase(String itineraryId, CreateItineraryReq request, String userId) {
        logger.info("=== PHASE 1: CRITICAL PLANNING ===");
        
        // Execute PlannerAgent with retries
        AgentExecutionResult plannerResult = executeAgentWithRetries(
            "PlannerAgent", 
            () -> {
                Map<String, Object> requestData = new HashMap<>();
                requestData.put("taskType", "create");  // PlannerAgent supports "create"
                requestData.put("request", request);
                BaseAgent.AgentRequest<ItineraryDto> plannerRequest = 
                    new BaseAgent.AgentRequest<>(requestData, ItineraryDto.class);
                return plannerAgent.execute(itineraryId, plannerRequest);
            }
        );
        
        if (!plannerResult.isSuccess()) {
            throw new RuntimeException("Critical planning phase failed: " + plannerResult.getError());
        }
        
        // Verify itinerary was created
        var itinerary = itineraryJsonService.getItinerary(itineraryId);
        if (itinerary.isEmpty()) {
            throw new RuntimeException("Itinerary not found after planning phase");
        }
        
        logger.info("Phase 1 completed: Itinerary created with {} days", 
                   itinerary.get().getDays() != null ? itinerary.get().getDays().size() : 0);
        
        return itinerary.get();
    }
    
    /**
     * Phase 2: Enhancement phase (non-critical)
     */
    private void executeEnhancementPhase(String itineraryId, NormalizedItinerary itinerary) {
        logger.info("=== PHASE 2: ENHANCEMENT (NON-CRITICAL) ===");
        
        // Run enhancement agents in parallel
        CompletableFuture<AgentExecutionResult> enrichmentFuture = CompletableFuture.supplyAsync(() -> 
            executeAgentWithRetries("EnrichmentAgent", () -> {
                Map<String, Object> enrichmentData = new HashMap<>();
                enrichmentData.put("taskType", "enrich");  // EnrichmentAgent supports "enrich"
                BaseAgent.AgentRequest<ChangeEngine.ApplyResult> enrichmentRequest = 
                    new BaseAgent.AgentRequest<>(enrichmentData, ChangeEngine.ApplyResult.class);
                return enrichmentAgent.execute(itineraryId, enrichmentRequest);
            }), agentExecutor);
        
        CompletableFuture<AgentExecutionResult> placesFuture = CompletableFuture.supplyAsync(() -> 
            executeAgentWithRetries("PlacesAgent", () -> {
                Map<String, Object> placesData = new HashMap<>();
                placesData.put("taskType", "search");  // PlacesAgent supports "search"
                placesData.put("itinerary", itinerary);
                BaseAgent.AgentRequest<Object> placesRequest = 
                    new BaseAgent.AgentRequest<>(placesData, Object.class);
                return placesAgent.execute(itineraryId, placesRequest);
            }), agentExecutor);
        
        // Wait for enhancement agents with timeout
        try {
            CompletableFuture.allOf(enrichmentFuture, placesFuture)
                .get(AGENT_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            
            logger.info("Phase 2 completed: Enhancement agents finished");
        } catch (TimeoutException e) {
            logger.warn("Enhancement phase timed out, continuing with basic itinerary");
        } catch (Exception e) {
            logger.warn("Enhancement phase failed, continuing with basic itinerary: {}", e.getMessage());
        }
    }
    
    /**
     * Phase 3: Finalization
     */
    private NormalizedItinerary finalizationPhase(String itineraryId) {
        logger.info("=== PHASE 3: FINALIZATION ===");
        
        // Get final itinerary
        var finalItinerary = itineraryJsonService.getItinerary(itineraryId);
        if (finalItinerary.isEmpty()) {
            throw new RuntimeException("Final itinerary not found");
        }
        
        // Mark as completed
        NormalizedItinerary itinerary = finalItinerary.get();
        itinerary.setUpdatedAt(System.currentTimeMillis());
        
        // Send completion event via new real-time system
        String executionId = "exec_" + System.currentTimeMillis(); // Generate execution ID
        agentEventPublisher.publishGenerationComplete(itineraryId, executionId, itinerary);
        
        logger.info("=== RESILIENT ORCHESTRATION COMPLETED ===");
        logger.info("Final itinerary has {} days", itinerary.getDays() != null ? itinerary.getDays().size() : 0);
        
        return itinerary;
    }
    
    /**
     * Execute an agent with retry logic and timeout
     */
    private AgentExecutionResult executeAgentWithRetries(String agentName, Callable<Object> agentExecution) {
        AtomicInteger attempts = new AtomicInteger(0);
        
        while (attempts.get() < MAX_RETRY_ATTEMPTS) {
            try {
                logger.info("Executing {} (attempt {}/{})", agentName, attempts.get() + 1, MAX_RETRY_ATTEMPTS);
                
                // Execute with timeout
                CompletableFuture<Object> future = CompletableFuture.supplyAsync(() -> {
                    try {
                        return agentExecution.call();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }, agentExecutor);
                
                Object result = future.get(AGENT_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                
                logger.info("{} completed successfully", agentName);
                return AgentExecutionResult.success(result);
                
            } catch (TimeoutException e) {
                logger.warn("{} timed out (attempt {})", agentName, attempts.get() + 1);
                attempts.incrementAndGet();
                
                if (attempts.get() >= MAX_RETRY_ATTEMPTS) {
                    return AgentExecutionResult.failure("Agent timed out after " + MAX_RETRY_ATTEMPTS + " attempts");
                }
                
            } catch (Exception e) {
                logger.warn("{} failed (attempt {}): {}", agentName, attempts.get() + 1, e.getMessage());
                attempts.incrementAndGet();
                
                if (attempts.get() >= MAX_RETRY_ATTEMPTS) {
                    return AgentExecutionResult.failure("Agent failed after " + MAX_RETRY_ATTEMPTS + " attempts: " + e.getMessage());
                }
            }
            
            // Wait before retry
            try {
                Thread.sleep(RETRY_DELAY_MS * attempts.get()); // Exponential backoff
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                return AgentExecutionResult.failure("Agent execution interrupted");
            }
        }
        
        return AgentExecutionResult.failure("Agent execution failed after all retries");
    }
    
    /**
     * Result of agent execution
     */
    private static class AgentExecutionResult {
        private final boolean success;
        private final Object result;
        private final String error;
        
        private AgentExecutionResult(boolean success, Object result, String error) {
            this.success = success;
            this.result = result;
            this.error = error;
        }
        
        public static AgentExecutionResult success(Object result) {
            return new AgentExecutionResult(true, result, null);
        }
        
        public static AgentExecutionResult failure(String error) {
            return new AgentExecutionResult(false, null, error);
        }
        
        public boolean isSuccess() { return success; }
        public Object getResult() { return result; }
        public String getError() { return error; }
    }
}