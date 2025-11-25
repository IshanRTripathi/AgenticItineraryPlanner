package com.tripplanner.service;

import com.tripplanner.agents.EnrichmentAgent;
import com.tripplanner.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * BatchEnrichmentService - Handles parallel enrichment with collect-then-save
 * pattern.
 * 
 * This service solves the race condition problem by:
 * 1. Loading itinerary once
 * 2. Collecting enrichments from all days in parallel
 * 3. Applying all enrichments to single itinerary object
 * 4. Saving once (no race condition)
 */
@Service
public class BatchEnrichmentService {

    private static final Logger logger = LoggerFactory.getLogger(BatchEnrichmentService.class);

    private final EnrichmentAgent enrichmentAgent;
    private final ItineraryJsonService itineraryJsonService;
    private final ExecutorService enrichmentExecutor;

    public BatchEnrichmentService(EnrichmentAgent enrichmentAgent,
            ItineraryJsonService itineraryJsonService) {
        this.enrichmentAgent = enrichmentAgent;
        this.itineraryJsonService = itineraryJsonService;

        // Dedicated thread pool for enrichment
        this.enrichmentExecutor = Executors.newFixedThreadPool(4, r -> {
            Thread t = new Thread(r);
            t.setName("Enrichment-" + t.getId());
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * Enrich a batch of days using collect-then-save pattern.
     * 
     * @param itineraryId The itinerary ID
     * @param batchDays   The days to enrich
     * @param batchNumber The batch number (for logging)
     * @param maxRetries  Maximum retry attempts per day
     * @param timeoutMs   Timeout per batch in milliseconds
     * @return Number of successfully enriched days
     */
    public int enrichBatch(String itineraryId, List<NormalizedDay> batchDays,
            int batchNumber, int maxRetries, long timeoutMs) {

        logger.info("  📦 [Batch {}] Starting collect-then-save enrichment for {} days",
                batchNumber, batchDays.size());

        // Step 1: Load itinerary ONCE
        Optional<NormalizedItinerary> itineraryOpt = itineraryJsonService.getItinerary(itineraryId);
        if (itineraryOpt.isEmpty()) {
            logger.error("  ❌ [Batch {}] Itinerary not found: {}", batchNumber, itineraryId);
            return 0;
        }

        NormalizedItinerary itinerary = itineraryOpt.get();
        logger.info("  📖 [Batch {}] Loaded itinerary version {}", batchNumber, itinerary.getVersion());

        // Step 2: Collect enrichments from all days in parallel (with retry)
        List<DayEnrichmentResult> allResults = collectEnrichmentsWithRetry(
                itinerary, batchDays, batchNumber, maxRetries, timeoutMs);

        // Step 3: Apply all enrichments to the itinerary object
        int enrichedNodeCount = applyEnrichmentsToItinerary(itinerary, allResults, batchNumber);

        // Step 4: Save itinerary ONCE with optimistic locking and retry
        if (enrichedNodeCount > 0) {
            int retryCount = 0;
            boolean saved = false;

            while (!saved && retryCount < maxRetries) {
                try {
                    itineraryJsonService.updateItineraryWithLock(itinerary);
                    logger.info("  💾 [Batch {}] Saved itinerary with {} enriched nodes (with lock)",
                            batchNumber, enrichedNodeCount);
                    saved = true;
                } catch (com.tripplanner.exception.ConcurrentModificationException e) {
                    retryCount++;
                    logger.error("  ❌ [Batch {}] Concurrent modification detected (attempt {}/{}): {}",
                            batchNumber, retryCount, maxRetries, e.getMessage());

                    if (retryCount < maxRetries) {
                        logger.info("  🔄 [Batch {}] Reloading itinerary and retrying save...", batchNumber);
                        Optional<NormalizedItinerary> reloaded = itineraryJsonService
                                .getItinerary(itinerary.getItineraryId());
                        if (reloaded.isPresent()) {
                            itinerary = reloaded.get();
                            // Re-apply enrichments to reloaded itinerary
                            enrichedNodeCount = applyEnrichmentsToItinerary(itinerary, allResults, batchNumber);
                            logger.info("  🔄 [Batch {}] Re-applied {} enrichments to reloaded itinerary",
                                    batchNumber, enrichedNodeCount);
                        } else {
                            logger.error("  ❌ [Batch {}] Failed to reload itinerary for retry", batchNumber);
                            return 0;
                        }
                    } else {
                        logger.error("  ❌ [Batch {}] Max retries ({}) exceeded, giving up", batchNumber, maxRetries);
                        return 0;
                    }
                } catch (Exception e) {
                    logger.error("  ❌ [Batch {}] Failed to save itinerary: {}",
                            batchNumber, e.getMessage());
                    return 0;
                }
            }
        } else {
            logger.info("  ℹ️ [Batch {}] No enrichments to save", batchNumber);
        }

        // Count successful days
        int successfulDays = (int) allResults.stream().filter(DayEnrichmentResult::isSuccess).count();
        logger.info("  ✅ [Batch {}] Complete: {}/{} days enriched, {} nodes total",
                batchNumber, successfulDays, batchDays.size(), enrichedNodeCount);

        return successfulDays;
    }

    /**
     * Collect enrichments from all days with retry logic.
     */
    private List<DayEnrichmentResult> collectEnrichmentsWithRetry(
            NormalizedItinerary itinerary, List<NormalizedDay> batchDays,
            int batchNumber, int maxRetries, long timeoutMs) {

        Map<Integer, DayEnrichmentResult> results = new HashMap<>();
        List<NormalizedDay> daysToEnrich = new ArrayList<>(batchDays);
        int attempt = 0;

        while (!daysToEnrich.isEmpty() && attempt <= maxRetries) {
            attempt++;

            if (attempt > 1) {
                logger.info("  🔄 [Batch {}] Retry attempt {} for {} failed days",
                        batchNumber, attempt - 1, daysToEnrich.size());
            }

            // Collect enrichments in parallel
            Map<Integer, CompletableFuture<DayEnrichmentResult>> futures = new HashMap<>();

            for (NormalizedDay day : daysToEnrich) {
                int finalAttempt = attempt;
                CompletableFuture<DayEnrichmentResult> future = CompletableFuture.supplyAsync(() -> {
                    try {
                        logger.info("  🔄 [Batch {}] Collecting enrichments for Day {} (attempt {})",
                                batchNumber, day.getDayNumber(), finalAttempt);

                        DayEnrichmentResult result = enrichmentAgent.collectEnrichments(itinerary, day);

                        logger.info("  ✅ [Batch {}] Day {} collected {} enrichments",
                                batchNumber, day.getDayNumber(), result.getEnrichedNodes().size());

                        return result;

                    } catch (Exception e) {
                        logger.error("  ❌ [Batch {}] Day {} failed (attempt {}): {}",
                                batchNumber, day.getDayNumber(), finalAttempt, e.getMessage());
                        return DayEnrichmentResult.failure(day.getDayNumber(), e.getMessage());
                    }
                }, enrichmentExecutor);

                futures.put(day.getDayNumber(), future);
            }

            // Wait for all with timeout
            try {
                CompletableFuture.allOf(futures.values().toArray(new CompletableFuture[0]))
                        .get(timeoutMs, TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                logger.warn("  ⏱️ [Batch {}] Timeout after {} ms (attempt {})",
                        batchNumber, timeoutMs, attempt);
            } catch (Exception e) {
                logger.warn("  ⚠️ [Batch {}] Error waiting for futures: {}",
                        batchNumber, e.getMessage());
            }

            // Collect results and identify failures
            List<NormalizedDay> failedDays = new ArrayList<>();
            for (NormalizedDay day : daysToEnrich) {
                try {
                    DayEnrichmentResult result = futures.get(day.getDayNumber()).getNow(
                            DayEnrichmentResult.failure(day.getDayNumber(), "Timeout or not completed"));

                    if (result.isSuccess()) {
                        results.put(day.getDayNumber(), result);
                    } else {
                        failedDays.add(day);
                    }
                } catch (Exception e) {
                    failedDays.add(day);
                }
            }

            // Check if we should retry
            if (failedDays.isEmpty()) {
                break;
            } else if (attempt <= maxRetries) {
                logger.warn("  ⚠️ [Batch {}] {} days failed, will retry",
                        batchNumber, failedDays.size());
                daysToEnrich = failedDays;

                // Small delay before retry
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            } else {
                logger.error("  ❌ [Batch {}] {} days failed after {} attempts",
                        batchNumber, failedDays.size(), maxRetries + 1);
            }
        }

        return new ArrayList<>(results.values());
    }

    /**
     * Apply all enrichments to the itinerary object.
     * This is single-threaded and safe.
     */
    private int applyEnrichmentsToItinerary(NormalizedItinerary itinerary,
            List<DayEnrichmentResult> results,
            int batchNumber) {

        int totalEnriched = 0;

        for (DayEnrichmentResult result : results) {
            if (!result.isSuccess()) {
                continue;
            }

            for (EnrichedNodeData enrichment : result.getEnrichedNodes()) {
                boolean applied = applyEnrichmentToNode(itinerary, enrichment);
                if (applied) {
                    totalEnriched++;
                }
            }
        }

        return totalEnriched;
    }

    /**
     * Apply a single enrichment to a node in the itinerary.
     * Finds the node by ID and merges the enrichment data.
     */
    private boolean applyEnrichmentToNode(NormalizedItinerary itinerary, EnrichedNodeData enrichment) {
        // Find the node by ID
        for (NormalizedDay day : itinerary.getDays()) {
            if (day.getNodes() == null)
                continue;

            for (NormalizedNode node : day.getNodes()) {
                if (node.getId().equals(enrichment.getNodeId())) {
                    // Found the node, apply enrichment
                    if (enrichment.getLocation() != null) {
                        node.setLocation(enrichment.getLocation());
                    }
                    if (enrichment.getAgentData() != null) {
                        node.setAgentData(enrichment.getAgentData());
                    }

                    logger.debug("  ✅ Applied enrichment to node: {}", node.getId());
                    return true;
                }
            }
        }

        logger.warn("  ⚠️ Node not found for enrichment: {}", enrichment.getNodeId());
        return false;
    }
}
