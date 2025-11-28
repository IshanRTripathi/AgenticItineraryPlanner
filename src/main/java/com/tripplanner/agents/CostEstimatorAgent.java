package com.tripplanner.agents;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripplanner.dto.*;
import com.tripplanner.enums.ProcessingState;
import com.tripplanner.exception.ValidationException;
import com.tripplanner.service.agents.AgentEventBus;
import com.tripplanner.service.BudgetTracker;
import com.tripplanner.service.CurrencyConversionService;
import com.tripplanner.service.ItineraryJsonService;
import com.tripplanner.service.ItineraryValidator;
import com.tripplanner.service.ai.AiClient;
import com.tripplanner.service.analytics.ItineraryMetricsTracker;
import com.tripplanner.dto.tools.CostCalculationRequest;
import com.tripplanner.dto.tools.CostCalculationResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * CostEstimatorAgent - Adds realistic, location-specific cost estimates to all
 * nodes using LLM.
 * 
 * This agent uses AI to generate accurate cost estimates based on:
 * - Actual destination and local cost of living
 * - Specific activity/venue type
 * - Budget tier preferences
 * - Current market rates
 * 
 * IMPORTANT: ALL costs are PER PERSON. The amountPerPerson field represents the
 * cost
 * for one individual, not the total group cost.
 * 
 * Responsibilities:
 * - Generate realistic cost estimates for all nodes (per person)
 * - Use destination-specific pricing in local currency
 * - Adjust for budget tier (budget, medium, luxury)
 * - Provide accurate estimates based on current market rates
 * 
 * Processing Time: 10-15 seconds (AI-powered for accuracy)
 */
@Component
@ConditionalOnBean(AiClient.class)
public class CostEstimatorAgent extends BaseAgent {

    private final ItineraryJsonService itineraryJsonService;
    private final AiClient aiClient;
    private final ObjectMapper objectMapper;
    private final BudgetTracker budgetTracker;
    private final CurrencyConversionService currencyConversionService;
    private final ItineraryValidator itineraryValidator;
    private final ItineraryMetricsTracker metricsTracker;
    private final RestTemplate restTemplate;
    private final com.tripplanner.service.CostEstimationRules costEstimationRules;
    
    // Feature flags for tool integration
    @Value("${features.cost-tools.enabled:false}")
    private boolean costToolsEnabled;
    
    @Value("${features.cost-tools.fallback-on-error:true}")
    private boolean fallbackOnError;
    
    @Value("${features.cost-estimation.rule-based:false}")
    private boolean useRuleBasedEstimation;

    public CostEstimatorAgent(AgentEventBus eventBus,
            ItineraryJsonService itineraryJsonService,
            AiClient aiClient,
            ObjectMapper objectMapper,
            BudgetTracker budgetTracker,
            CurrencyConversionService currencyConversionService,
            ItineraryValidator itineraryValidator,
            ItineraryMetricsTracker metricsTracker,
            com.tripplanner.service.CostEstimationRules costEstimationRules) {
        super(eventBus, AgentEvent.AgentKind.ENRICHMENT);
        this.itineraryJsonService = itineraryJsonService;
        this.aiClient = aiClient;
        this.objectMapper = objectMapper;
        this.budgetTracker = budgetTracker;
        this.currencyConversionService = currencyConversionService;
        this.costEstimationRules = costEstimationRules;
        this.itineraryValidator = itineraryValidator;
        this.metricsTracker = metricsTracker;
        this.restTemplate = new RestTemplate();
    }

    @Override
    public AgentCapabilities getCapabilities() {
        AgentCapabilities capabilities = new AgentCapabilities();

        // Pipeline-only task: estimate costs
        capabilities.addSupportedTask("estimate_costs");

        capabilities.setPriority(50);
        capabilities.setChatEnabled(false); // Pipeline-only, not for chat
        capabilities.setConfigurationValue("fastExecution", true);

        return capabilities;
    }

    /**
     * Estimate costs for all nodes in the itinerary.
     * Uses Google Places priceLevel when available, falls back to AI for missing
     * data.
     */
    public void estimateCosts(String itineraryId, NormalizedItinerary itinerary, String budgetTier) {
        logger.info("=== COST ESTIMATOR AGENT ===");
        logger.info("Estimating costs for itinerary: {} (hybrid: Google Places + AI)", itineraryId);

        try {
            emitProgress(itineraryId, 10, "Preparing cost estimation", "loading");

            String destination = itinerary.getDays().isEmpty() ? "Unknown" : itinerary.getDays().get(0).getLocation();

            // IMPROVED: Detect destination currency automatically
            String destinationCurrency = currencyConversionService.detectDestinationCurrency(destination);
            String currency = itinerary.getCurrency() != null ? itinerary.getCurrency() : destinationCurrency;
            
            // Validate currency is supported, fallback to USD if not
            if (!currencyConversionService.isCurrencySupported(currency)) {
                logger.warn("Unsupported currency: {}, falling back to USD", currency);
                currency = "USD";
            }

            // Get user's budget parameters
            Double budgetMin = itinerary.getBudgetMin();
            Double budgetMax = itinerary.getBudgetMax();
            
            // FIXED: Get user currency from itinerary or detect from destination
            // Don't hardcode INR - this was causing incorrect budget conversions
            String userCurrency = itinerary.getCurrency();
            if (userCurrency == null || userCurrency.isEmpty()) {
                userCurrency = destinationCurrency; // Use destination currency if not specified
                logger.info("User currency not specified, using destination currency: {}", userCurrency);
            }

            logger.info("Cost estimation: destination={}, currency={}, userCurrency={}, budget={}-{}",
                    destination, currency, userCurrency, budgetMin, budgetMax);
            
            // Track currency detection for analytics
            if (!userCurrency.equals(destinationCurrency)) {
                logger.info("Currency conversion needed: {} → {}", userCurrency, destinationCurrency);
            }

            int totalDays = itinerary.getDays().size();
            int processedDays = 0;

            emitProgress(itineraryId, 20,
                    String.format("Estimating costs for %d days in %s", totalDays, destination),
                    "estimating");

            // Calculate inter-city travel costs
            double interCityTravelCost = 0.0;
            CityAllocationPlan cityPlan = null;
            if (itinerary.getAgentData() != null && itinerary.getAgentData().containsKey("cityAllocation")) {
                AgentDataSection agentDataSection = itinerary.getAgentData().get("cityAllocation");
                cityPlan = agentDataSection.getAgentData("cityAllocation", CityAllocationPlan.class);
                if (cityPlan != null && cityPlan.getTravelSegments() != null) {
                    logger.info("Calculating inter-city travel costs for {} segments",
                            cityPlan.getTravelSegments().size());
                    interCityTravelCost = calculateInterCityTravelCosts(cityPlan, budgetTier);
                    logger.info("Total inter-city travel cost: {} {}", interCityTravelCost, currency);
                }
            }

            // Process each day
            int dayNumber = 0;
            for (NormalizedDay day : itinerary.getDays()) {
                dayNumber++;
                if (day.getNodes() == null || day.getNodes().isEmpty())
                    continue;

                // Separate nodes into those with priceLevel and those without
                List<NormalizedNode> nodesWithPriceLevel = new ArrayList<>();
                List<NormalizedNode> nodesNeedingAI = new ArrayList<>();

                for (NormalizedNode node : day.getNodes()) {
                    // Mark node as being processed for cost estimation
                    node.setProcessingState(ProcessingState.ENRICHING);
                    node.addProcessedBy("CostEstimatorAgent");

                    if (node.getCost() != null && node.getCost().getAmountPerPerson() != null) {
                        // Already has cost, mark as enriched and skip
                        node.setProcessingState(ProcessingState.ENRICHED);
                        continue;
                    }

                    if (node.getLocation() != null && node.getLocation().getPriceLevel() != null) {
                        nodesWithPriceLevel.add(node);
                    } else {
                        nodesNeedingAI.add(node);
                    }
                }

                // Estimate costs for nodes with priceLevel (fast, no AI needed)
                for (NormalizedNode node : nodesWithPriceLevel) {
                    try {
                        estimateCostFromPriceLevel(node, destination, budgetTier, currency, itineraryId, dayNumber);
                        // Mark node as successfully processed
                        node.setProcessingState(ProcessingState.ENRICHED);
                    } catch (Exception e) {
                        logger.error("Failed to estimate cost for node {}: {}", node.getId(), e.getMessage());
                        node.setProcessingState(ProcessingState.FAILED);
                        node.setLastError("Cost estimation failed: " + e.getMessage());
                    }
                }

                // Estimate costs for nodes without priceLevel
                if (!nodesNeedingAI.isEmpty()) {
                    try {
                        if (useRuleBasedEstimation) {
                            // OPTIMIZED: Use rule-based estimation (fast, < 10ms per node)
                            logger.info("Using rule-based cost estimation for {} nodes (feature flag enabled)", 
                                       nodesNeedingAI.size());
                            estimateCostsWithRules(nodesNeedingAI, destination, budgetTier, currency);
                        } else {
                            // LEGACY: Use AI estimation (slower but accurate)
                            logger.info("Using AI cost estimation for {} nodes", nodesNeedingAI.size());
                            estimateCostsWithAI(nodesNeedingAI, destination, budgetTier, currency,
                                    budgetMin, budgetMax, userCurrency, itineraryId, dayNumber);
                        }
                        
                        // Mark all estimated nodes as successfully processed
                        for (NormalizedNode node : nodesNeedingAI) {
                            if (node.getCost() != null && node.getCost().getAmountPerPerson() != null) {
                                node.setProcessingState(ProcessingState.ENRICHED);
                            } else {
                                node.setProcessingState(ProcessingState.FAILED);
                                node.setLastError("Cost estimation returned no cost");
                            }
                        }
                    } catch (Exception e) {
                        logger.error("Failed to estimate costs: {}", e.getMessage());
                        for (NormalizedNode node : nodesNeedingAI) {
                            node.setProcessingState(ProcessingState.FAILED);
                            node.setLastError("AI cost estimation failed: " + e.getMessage());
                        }
                    }
                }

                // Calculate day total
                double dayCost = day.getNodes().stream()
                        .filter(n -> n.getCost() != null && n.getCost().getAmountPerPerson() != null)
                        .mapToDouble(n -> n.getCost().getAmountPerPerson())
                        .sum();
                day.setTotalCost(dayCost);

                // Send progress update
                processedDays++;
                int progress = 20 + (int) ((processedDays / (double) totalDays) * 60);
                emitProgress(itineraryId, progress,
                        String.format("Estimated costs for day %d/%d", processedDays, totalDays),
                        "estimating");
            }

            emitProgress(itineraryId, 85, "Saving cost data", "saving");

            // Calculate total cost (including inter-city travel)
            double totalCost = itinerary.getDays().stream()
                    .mapToDouble(NormalizedDay::getTotalCost)
                    .sum();
            totalCost += interCityTravelCost;

            // Calculate per-day average
            double perDayAverage = totalDays > 0 ? totalCost / totalDays : 0;

            // IMPROVED: Calculate budget summary and warnings
            emitProgress(itineraryId, 90, "Calculating budget", "budget_check");

            Integer partySize = itinerary.getPartySize() != null ? itinerary.getPartySize() : 1;

            // CRITICAL: Budget parameters from CityAllocationAgent are per-day, not total
            // trip
            // We need to convert to total trip budget for comparison
            Double totalBudgetMin = budgetMin != null ? budgetMin * totalDays : null;
            Double totalBudgetMax = budgetMax != null ? budgetMax * totalDays : null;

            logger.info("Budget tracking: perDay={}-{}, total={}-{}, partySize={}",
                    budgetMin, budgetMax, totalBudgetMin, totalBudgetMax, partySize);

            BudgetTracker.BudgetSummary budgetSummary;
            
            // FEATURE FLAG: Use Calculate Cost tool if enabled
            if (costToolsEnabled) {
                logger.info("Using Calculate Cost tool (feature flag enabled)");
                CostCalculationResult toolResult = calculateCostViaTool(itineraryId, partySize);
                
                if (toolResult != null && toolResult.isSuccess()) {
                    // Convert tool result to BudgetSummary
                    budgetSummary = convertToolResultToBudgetSummary(toolResult, totalBudgetMin, totalBudgetMax);
                    logger.info("Calculate Cost tool succeeded");
                } else if (fallbackOnError) {
                    logger.warn("Calculate Cost tool failed, falling back to BudgetTracker service");
                    budgetSummary = calculateCostWithFallback(itinerary, totalBudgetMin, totalBudgetMax, partySize);
                } else {
                    logger.error("Calculate Cost tool failed and fallback disabled");
                    budgetSummary = calculateCostWithFallback(itinerary, totalBudgetMin, totalBudgetMax, partySize);
                }
            } else {
                logger.info("Using BudgetTracker service (cost tools feature flag disabled)");
                budgetSummary = calculateCostWithFallback(itinerary, totalBudgetMin, totalBudgetMax, partySize);
            }

            // Log budget warnings
            if (!budgetSummary.getWarnings().isEmpty()) {
                logger.warn("=== BUDGET WARNINGS ===");
                for (String warning : budgetSummary.getWarnings()) {
                    logger.warn(warning);
                }

                // Get recommendations if over budget
                if (budgetSummary.isOverBudget()) {
                    List<String> recommendations = budgetTracker.getRecommendations(budgetSummary);
                    logger.warn("=== BUDGET RECOMMENDATIONS ===");
                    for (String rec : recommendations) {
                        logger.warn("- {}", rec);
                    }
                }
            }

            // Validate before save
            ItineraryValidator.ValidationResult validationResult = itineraryValidator.validate(itinerary);
            if (!validationResult.isValid()) {
                logger.error("Validation failed for itinerary {}: {}", itineraryId, validationResult.getErrors());
                throw new ValidationException("Itinerary validation failed",
                        String.valueOf(validationResult.getErrors()));
            }
            if (!validationResult.getWarnings().isEmpty()) {
                logger.warn("Validation warnings for itinerary {}: {}", itineraryId, validationResult.getWarnings());
            }

            // Save updated itinerary with optimistic locking and retry
            int maxRetries = 3;
            int retryCount = 0;
            boolean saved = false;

            while (!saved && retryCount < maxRetries) {
                try {
                    itinerary.setUpdatedAt(System.currentTimeMillis());
                    itineraryJsonService.updateItineraryWithLock(itinerary);
                    logger.info("Saved itinerary with cost estimates (with lock)");
                    saved = true;
                } catch (com.tripplanner.exception.ConcurrentModificationException e) {
                    retryCount++;
                    logger.error("Concurrent modification detected (attempt {}/{}): {}",
                            retryCount, maxRetries, e.getMessage());

                    if (retryCount < maxRetries) {
                        logger.info("Reloading itinerary and retrying save...");
                        Optional<NormalizedItinerary> reloaded = itineraryJsonService.getItinerary(itineraryId);
                        if (reloaded.isPresent()) {
                            itinerary = reloaded.get();
                            // Re-apply cost estimates to reloaded itinerary
                            // Note: Cost calculation is idempotent, will recalculate on retry
                            logger.info("Reloaded itinerary, will recalculate costs");
                        } else {
                            logger.error("Failed to reload itinerary for retry");
                            throw e;
                        }
                    } else {
                        logger.error("Max retries ({}) exceeded, giving up", maxRetries);
                        throw e;
                    }
                }
            }

            int totalNodes = itinerary.getDays().stream()
                    .mapToInt(d -> d.getNodes() != null ? d.getNodes().size() : 0)
                    .sum();

            emitProgress(itineraryId, 100,
                    String.format("Estimated costs for %d nodes", totalNodes),
                    "complete");

            logger.info("=== COST ESTIMATOR COMPLETE ===");
            logger.info("Estimated costs for {} nodes across {} days", totalNodes, totalDays);
            logger.info("Total estimated cost: {} {} per person (avg {} per day)",
                    currency, String.format("%.0f", totalCost), String.format("%.0f", perDayAverage));
            logger.info("Budget status: {}% of budget used",
                    String.format("%.1f", budgetSummary.getPercentageOfBudget()));
            logger.info("Per-day budget: {}-{} {}, Actual per-day: {} {}",
                    budgetMin != null ? String.format("%.0f", budgetMin) : "N/A",
                    budgetMax != null ? String.format("%.0f", budgetMax) : "N/A",
                    currency, String.format("%.0f", perDayAverage), currency);

        } catch (Exception e) {
            logger.error("Failed to estimate costs for itinerary: {}", itineraryId, e);
            emitProgress(itineraryId, 0, "Failed to estimate costs", "error");
            // Don't throw - costs are optional
        }
    }

    // ========== COST ESTIMATOR AGENT - TOOL INTEGRATION METHODS ==========
    
    /**
     * Calculate cost using the Calculate Cost tool.
     * This provides budget analysis with warnings and recommendations.
     */
    private CostCalculationResult calculateCostViaTool(String itineraryId, Integer partySize) {
        CostCalculationRequest request = new CostCalculationRequest(itineraryId, partySize);
        request.setIncludeBudgetAnalysis(true);
        
        try {
            logger.debug("Calling Calculate Cost tool for itinerary: {}", itineraryId);
            
            CostCalculationResult result = restTemplate.postForObject(
                "http://localhost:8080/api/v1/tools/calculate-cost",
                request,
                CostCalculationResult.class
            );
            
            if (result != null && result.isSuccess()) {
                logger.info("Calculate Cost tool succeeded: {} {} per person", 
                           result.getTotalCostPerPerson(), result.getCurrency());
                
                // Log budget analysis
                if (result.getBudgetAnalysis() != null) {
                    String status = result.getBudgetAnalysis().getBudgetStatus();
                    logger.info("Budget status: {}", status);
                    
                    if ("OVER".equals(status)) {
                        logger.warn("BUDGET EXCEEDED by {} {}", 
                                   result.getBudgetAnalysis().getOverage(),
                                   result.getCurrency());
                    }
                    
                    // Log recommendations
                    if (result.getBudgetAnalysis().getRecommendations() != null) {
                        for (String rec : result.getBudgetAnalysis().getRecommendations()) {
                            logger.info("Budget recommendation: {}", rec);
                        }
                    }
                }
                
                return result;
            } else {
                logger.warn("Calculate Cost tool returned unsuccessful result");
                return null;
            }
            
        } catch (Exception e) {
            logger.error("Calculate Cost tool failed: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Fallback to direct BudgetTracker service if tool fails.
     */
    private BudgetTracker.BudgetSummary calculateCostWithFallback(
            NormalizedItinerary itinerary, 
            Double totalBudgetMin, 
            Double totalBudgetMax, 
            Integer partySize) {
        
        logger.info("Using fallback BudgetTracker service for cost calculation");
        return budgetTracker.calculateBudget(itinerary, totalBudgetMin, totalBudgetMax, partySize);
    }
    
    /**
     * Convert CostCalculationResult from tool to BudgetSummary for compatibility.
     */
    private BudgetTracker.BudgetSummary convertToolResultToBudgetSummary(
            CostCalculationResult toolResult,
            Double totalBudgetMin,
            Double totalBudgetMax) {
        
        BudgetTracker.BudgetSummary summary = new BudgetTracker.BudgetSummary();
        summary.setTotalCostPerPerson(toolResult.getTotalCostPerPerson());
        summary.setTotalCostForParty(toolResult.getTotalCostForParty());
        summary.setCurrency(toolResult.getCurrency());
        summary.setPartySize(toolResult.getPartySize());
        
        // Set budget min/max
        if (totalBudgetMin != null) {
            summary.setBudgetMin(totalBudgetMin);
        }
        if (totalBudgetMax != null) {
            summary.setBudgetMax(totalBudgetMax);
        }
        
        // Convert breakdown to CategoryBudget
        if (toolResult.getBreakdown() != null) {
            BudgetTracker.CategoryBudget categoryBudget = new BudgetTracker.CategoryBudget();
            categoryBudget.setAttractions(toolResult.getBreakdown().getOrDefault("activities", 0.0));
            categoryBudget.setMeals(toolResult.getBreakdown().getOrDefault("meals", 0.0));
            categoryBudget.setTransport(toolResult.getBreakdown().getOrDefault("transport", 0.0));
            categoryBudget.setAccommodation(toolResult.getBreakdown().getOrDefault("accommodation", 0.0));
            categoryBudget.setOther(toolResult.getBreakdown().getOrDefault("other", 0.0));
            summary.setCategoryBreakdown(categoryBudget);
        }
        
        // Convert budget analysis to warnings
        if (toolResult.getBudgetAnalysis() != null) {
            List<String> warnings = new ArrayList<>();
            
            String status = toolResult.getBudgetAnalysis().getBudgetStatus();
            if ("OVER".equals(status)) {
                warnings.add(String.format("Budget exceeded by %s %s", 
                    toolResult.getBudgetAnalysis().getOverage(),
                    toolResult.getCurrency()));
            } else if ("MODERATE".equals(status)) {
                warnings.add(String.format("Using %.1f%% of budget", 
                    toolResult.getBudgetAnalysis().getBudgetUsedPercentage()));
            }
            
            // Add recommendations as warnings
            if (toolResult.getBudgetAnalysis().getRecommendations() != null) {
                warnings.addAll(toolResult.getBudgetAnalysis().getRecommendations());
            }
            
            summary.setWarnings(warnings);
        }
        
        return summary;
    }
    
    // ========== END COST ESTIMATOR AGENT TOOL INTEGRATION ==========

    /**
     * Calculate inter-city travel costs from city allocation plan.
     * IMPROVED: Now uses proper USD base with budget tier multipliers
     */
    private double calculateInterCityTravelCosts(CityAllocationPlan cityPlan, String budgetTier) {
        double totalCost = 0.0;

        if (cityPlan.getTravelSegments() == null || cityPlan.getTravelSegments().isEmpty()) {
            return 0.0;
        }

        for (TravelSegment segment : cityPlan.getTravelSegments()) {
            double cost = estimateTravelCost(segment, budgetTier);
            totalCost += cost;

            logger.info("Travel cost {} to {}: ${} USD",
                    segment.getFromCity(),
                    segment.getToCity(),
                    String.format("%.2f", cost));
        }

        return totalCost;
    }

    /**
     * Estimate travel cost for a single segment.
     * IMPROVED: Simplified with cleaner budget tier handling
     */
    private double estimateTravelCost(TravelSegment segment, String budgetTier) {
        String mode = segment.getTravelMode();
        int hours = segment.getEstimatedHours();

        // Base costs per hour by mode (in USD, medium tier)
        Map<String, Double> baseCostPerHour = Map.of(
                "flight", 35.0,
                "train", 6.0,
                "bus", 2.5,
                "car", 8.0,
                "ferry", 4.0);

        // Budget tier multipliers
        double tierMultiplier = switch (budgetTier != null ? budgetTier.toLowerCase() : "medium") {
            case "budget", "low" -> 0.6;
            case "luxury", "high" -> 1.8;
            default -> 1.0; // medium
        };

        // Handle combined modes (e.g., "car+ferry")
        if (mode.contains("+")) {
            String[] modes = mode.split("\\+");
            double totalCost = 0.0;
            for (String m : modes) {
                double baseRate = baseCostPerHour.getOrDefault(m.trim(), 5.0);
                totalCost += baseRate * tierMultiplier;
            }
            return (totalCost / modes.length) * hours;
        }

        // Single mode
        double baseRate = baseCostPerHour.getOrDefault(mode, 5.0);
        return baseRate * tierMultiplier * hours;
    }

    /**
     * Estimate cost from Google Places priceLevel (0-4 scale).
     * This is fast and doesn't require AI calls.
     * IMPROVED: Now uses proper USD base conversion without redundant budget tier adjustment
     */
    private void estimateCostFromPriceLevel(NormalizedNode node, String destination,
            String budgetTier, String currency, String itineraryId, int dayNumber) {
        Integer priceLevel = node.getLocation().getPriceLevel();
        String nodeType = node.getType() != null ? node.getType().toLowerCase() : "activity";

        logger.debug("Estimating cost from priceLevel {} for node: {} (type: {})",
                priceLevel, node.getTitle(), nodeType);

        // Convert priceLevel (0-4) to actual cost (already handles currency conversion)
        double cost = convertPriceLevelToCost(priceLevel, nodeType, destination, currency);

        // Set cost
        if (node.getCost() == null) {
            node.setCost(new NodeCost());
        }
        node.getCost().setAmountPerPerson(cost);
        node.getCost().setCurrency(currency);

        logger.debug("Set cost for {} from priceLevel: {} {} per person",
                node.getTitle(), cost, currency);

        // Track cost estimation
        metricsTracker.trackCostEstimated(
                itineraryId,
                node.getId(),
                nodeType,
                cost,
                currency,
                dayNumber,
                node.getTitle());
    }

    /**
     * Convert Google Places priceLevel (0-4) to actual cost estimate.
     * Price levels: 0=Free, 1=Inexpensive, 2=Moderate, 3=Expensive, 4=Very Expensive
     * 
     * FIXED: Now uses USD as base currency with proper conversion to destination currency
     */
    private double convertPriceLevelToCost(Integer priceLevel, String nodeType,
            String destination, String currency) {
        if (priceLevel == null)
            priceLevel = 2; // Default to moderate

        // Base multipliers for different node types
        double typeMultiplier = switch (nodeType) {
            case "meal", "restaurant" -> 1.0;
            case "attraction", "activity" -> 0.8;
            case "transport", "transportation" -> 0.5;
            case "accommodation", "hotel" -> 0.0; // EXCLUDED: Not estimated
            default -> 1.0;
        };

        // Base costs in USD (global average that works across destinations)
        double baseCostUSD = switch (priceLevel) {
            case 0 -> 0.0;    // Free
            case 1 -> 10.0;   // Inexpensive
            case 2 -> 30.0;   // Moderate
            case 3 -> 60.0;   // Expensive
            case 4 -> 120.0;  // Very Expensive
            default -> 30.0;
        };
        
        // Apply type multiplier
        double adjustedUSD = baseCostUSD * typeMultiplier;
        
        // Convert to destination currency
        double costInDestCurrency = currencyConversionService.convert(adjustedUSD, "USD", currency);
        
        logger.debug("PriceLevel {} for {} -> {} USD -> {} {}", 
                    priceLevel, nodeType, adjustedUSD, costInDestCurrency, currency);
        
        return costInDestCurrency;
    }

    /**
     * Apply budget tier adjustment to cost.
     * REMOVED: This is now handled in convertPriceLevelToCost via USD base conversion
     * Keeping method for backward compatibility but it's a no-op
     */
    @Deprecated
    private double applyBudgetTierAdjustment(double baseCost, String budgetTier) {
        // Budget tier adjustment now happens through proper currency conversion
        // This method is deprecated and will be removed
        return baseCost;
    }

    /**
     * Estimate costs for nodes without priceLevel using AI.
     * IMPROVED: Now passes budget and currency conversion context.
     */
    private void estimateCostsWithAI(List<NormalizedNode> nodes, String destination,
            String budgetTier, String currency,
            Double budgetMin, Double budgetMax, String userCurrency,
            String itineraryId, int dayNumber) {
        if (nodes.isEmpty())
            return;

        try {
            String systemPrompt = buildCostEstimationSystemPrompt();
            String userPrompt = buildCostEstimationUserPrompt(nodes, destination, budgetTier, currency,
                    budgetMin, budgetMax, userCurrency);
            String schema = buildCostEstimationSchema(nodes.size());

            logger.debug("Requesting AI cost estimates for {} nodes", nodes.size());

            String response = aiClient.generateStructuredContent(userPrompt, schema, systemPrompt);

            // Parse response and update node costs
            parseCostEstimates(response, nodes, currency, itineraryId, dayNumber);

        } catch (Exception e) {
            logger.error("Failed to estimate costs with AI: {}", e.getMessage());
            // Set minimal fallback costs
            for (NormalizedNode node : nodes) {
                setMinimalFallbackCost(node, currency);
            }
        }
    }

    /**
     * Build system prompt for cost estimation with concrete price anchoring.
     */
    private String buildCostEstimationSystemPrompt() {
        return """
                You are a travel cost estimation expert with deep knowledge of pricing across different destinations worldwide.

                Your task is to provide REALISTIC, ACCURATE cost estimates based on actual market rates.

                CRITICAL RULES:
                1. ALL costs must be PER PERSON (not total group cost)
                2. Use the destination's local currency and actual market rates
                3. "Luxury" means PREMIUM, not ULTRA-EXCLUSIVE:
                   - Luxury meal = Fine dining restaurant (NOT Michelin 3-star)
                   - Luxury tour = Guided tour with good guide (NOT private helicopter)
                   - Luxury transport = First-class train/comfortable taxi (NOT private chauffeur)
                4. Your estimates MUST be within realistic market ranges
                5. If you're unsure, estimate LOWER rather than higher
                6. Most activities should cost LESS than you think

                FREE ATTRACTIONS:
                - Public parks, gardens, beaches = $0 (FREE)
                - Walking tours (self-guided) = $0 (FREE)
                - Viewpoints, scenic spots = $0 (FREE)
                - Religious sites (temples, churches, mosques) = Usually FREE or small donation
                - Street markets, shopping districts = FREE to browse
                - Only charge for PAID attractions (museums, theme parks, guided tours)

                VALIDATION RULES (in USD equivalent):
                - Walking tours (guided): $20-50 (NOT $500+)
                - Museum entry: $15-30 (NOT $100+)
                - Fine dining: $50-100 (NOT $300+)
                - Mountain excursions: $80-150 (NOT $1000+)
                - Taxi rides: $15-50 (NOT $200+)
                - Budget meals: $5-15
                - Mid-range meals: $15-30
                - Public transport: $2-8
                
                If your estimate exceeds 2x these ranges, you are probably overestimating.
                Provide estimates as whole numbers (no decimals) in the local currency.
                """;
    }

    /**
     * Build user prompt for cost estimation.
     * IMPROVED: Now includes currency conversion context for accurate estimates.
     */
    private String buildCostEstimationUserPrompt(List<NormalizedNode> nodes, String destination,
            String budgetTier, String currency,
            Double budgetMin, Double budgetMax, String userCurrency) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("Estimate costs for the following activities in ").append(destination).append(":\n\n");
        prompt.append("Budget tier: ").append(budgetTier).append("\n");
        prompt.append("Currency: ").append(currency).append("\n");

        // Add currency conversion context if user's currency is different
        if (userCurrency != null && !userCurrency.equalsIgnoreCase(currency)) {
            String currencyContext = currencyConversionService.buildCurrencyContextForLLM(
                    budgetMin, budgetMax, userCurrency, currency);
            prompt.append(currencyContext);
        } else if (budgetMin != null || budgetMax != null) {
            prompt.append("Budget range: ");
            if (budgetMin != null) {
                prompt.append(currencyConversionService.formatAmount(budgetMin, currency));
            }
            if (budgetMax != null) {
                if (budgetMin != null) {
                    prompt.append(" - ");
                }
                prompt.append(currencyConversionService.formatAmount(budgetMax, currency));
            }
            prompt.append(" per person per day\n");
        }

        prompt.append("\nActivities to estimate (these don't have Google Places pricing data):\n");
        for (int i = 0; i < nodes.size(); i++) {
            NormalizedNode node = nodes.get(i);
            prompt.append(i + 1).append(". ");
            prompt.append("Type: ").append(node.getType());
            prompt.append(", Title: ").append(node.getTitle() != null ? node.getTitle() : "Unknown");

            if (node.getLocation() != null && node.getLocation().getName() != null) {
                prompt.append(", Location: ").append(node.getLocation().getName());
            }

            if (node.getDetails() != null && node.getDetails().getCategory() != null) {
                prompt.append(", Category: ").append(node.getDetails().getCategory());
            }

            prompt.append("\n");
        }

        prompt.append("\nProvide realistic per-person cost estimates in ").append(currency);
        prompt.append(" for each activity based on current market rates in ").append(destination).append(".");
        prompt.append("\nIMPORTANT: All costs must be in ").append(currency).append(" (local currency).");

        return prompt.toString();
    }

    /**
     * Build JSON schema for cost estimation response.
     */
    private String buildCostEstimationSchema(int nodeCount) {
        return """
                {
                  "type": "object",
                  "properties": {
                    "estimates": {
                      "type": "array",
                      "minItems": %d,
                      "maxItems": %d,
                      "items": {
                        "type": "object",
                        "properties": {
                          "index": { "type": "integer" },
                          "costPerPerson": { "type": "number" },
                          "notes": { "type": "string" }
                        },
                        "required": ["index", "costPerPerson"]
                      }
                    }
                  },
                  "required": ["estimates"]
                }
                """.formatted(nodeCount, nodeCount);
    }

    /**
     * Parse cost estimates from AI response and update nodes with validation.
     */
    private void parseCostEstimates(String response, List<NormalizedNode> nodes, String currency,
            String itineraryId, int dayNumber) {
        try {
            String cleanedResponse = cleanJsonResponse(response);
            JsonNode root = objectMapper.readTree(cleanedResponse);
            JsonNode estimates = root.get("estimates");

            if (estimates == null || !estimates.isArray()) {
                logger.warn("Invalid cost estimation response format");
                for (NormalizedNode node : nodes) {
                    setMinimalFallbackCost(node, currency);
                }
                return;
            }

            for (JsonNode estimate : estimates) {
                int index = estimate.get("index").asInt() - 1; // Convert to 0-based
                double costPerPerson = estimate.get("costPerPerson").asDouble();

                if (index >= 0 && index < nodes.size()) {
                    NormalizedNode node = nodes.get(index);
                    
                    // CRITICAL: Validate cost is reasonable before saving
                    double validatedCost = validateAndCapCost(costPerPerson, node, currency);
                    
                    if (validatedCost != costPerPerson) {
                        logger.warn("Cost validation: {} adjusted from {} to {} {} for {}",
                                   node.getType(), costPerPerson, validatedCost, currency, node.getTitle());
                    }
                    
                    if (node.getCost() == null) {
                        node.setCost(new NodeCost());
                    }
                    node.getCost().setAmountPerPerson(validatedCost);
                    node.getCost().setCurrency(currency);

                    logger.debug("Set AI-estimated cost for node {}: {} {} per person",
                            node.getTitle(), validatedCost, currency);

                    // Track cost estimation
                    metricsTracker.trackCostEstimated(
                            itineraryId,
                            node.getId(),
                            node.getType() != null ? node.getType() : "activity",
                            validatedCost,
                            currency,
                            dayNumber,
                            node.getTitle());
                }
            }

        } catch (JsonProcessingException e) {
            logger.error("Failed to parse cost estimates: {}", e.getMessage());
            for (NormalizedNode node : nodes) {
                setMinimalFallbackCost(node, currency);
            }
        }
    }
    
    /**
     * Validate and cap cost to prevent unrealistic estimates.
     * Returns adjusted cost if original exceeds reasonable maximum.
     */
    private double validateAndCapCost(double cost, NormalizedNode node, String currency) {
        String nodeType = node.getType() != null ? node.getType().toLowerCase() : "activity";
        
        // Get maximum reasonable cost in USD (global baseline)
        double maxUSD = switch (nodeType) {
            case "meal", "restaurant" -> 150.0;  // Max $150 for luxury meal
            case "attraction", "activity" -> 300.0;  // Max $300 for premium experience
            case "transport", "transportation" -> 200.0;  // Max $200 for long transport
            default -> 200.0;
        };
        
        // Convert to destination currency for comparison
        double maxInCurrency = currencyConversionService.convert(maxUSD, "USD", currency);
        
        // Check minimum (prevent zero/negative)
        double minUSD = switch (nodeType) {
            case "meal", "restaurant" -> 5.0;
            case "transport", "transportation" -> 2.0;
            default -> 5.0;
        };
        double minInCurrency = currencyConversionService.convert(minUSD, "USD", currency);
        
        // Cap at maximum
        if (cost > maxInCurrency) {
            logger.warn("Cost {} {} exceeds maximum {} {} for {}, capping",
                       cost, currency, maxInCurrency, currency, nodeType);
            return maxInCurrency;
        }
        
        // Raise to minimum
        if (cost < minInCurrency && cost > 0) {
            logger.warn("Cost {} {} below minimum {} {}, adjusting",
                       cost, currency, minInCurrency, currency);
            return minInCurrency;
        }
        
        return cost;
    }

    /**
     * Set minimal fallback cost for a single node if all estimation methods fail.
     * FIXED: Now uses USD as base with proper currency conversion
     */
    private void setMinimalFallbackCost(NormalizedNode node, String currency) {
        if (node.getCost() != null && node.getCost().getAmountPerPerson() != null) {
            return; // Already has cost
        }

        logger.warn("Using minimal fallback cost for node: {}", node.getTitle());

        if (node.getCost() == null) {
            node.setCost(new NodeCost());
        }

        // Set fallback cost in USD (global baseline)
        double fallbackUSD = switch (node.getType() != null ? node.getType().toLowerCase() : "activity") {
            case "meal", "restaurant" -> 25.0;  // $25 average meal
            case "transport", "transportation" -> 15.0;  // $15 average transport
            case "attraction", "activity" -> 20.0;  // $20 average attraction
            case "accommodation", "hotel" -> 0.0; // EXCLUDED: Not estimated
            default -> 20.0;
        };
        
        // Convert to destination currency
        double fallbackCost = currencyConversionService.convert(fallbackUSD, "USD", currency);

        node.getCost().setAmountPerPerson(fallbackCost);
        node.getCost().setCurrency(currency);
        
        logger.debug("Set fallback cost: {} USD -> {} {}", fallbackUSD, fallbackCost, currency);
    }

    /**
     * Clean JSON response by removing markdown formatting.
     */
    private String cleanJsonResponse(String response) {
        if (response == null)
            return "{}";

        String cleaned = response.replaceAll("```json\\s*", "").replaceAll("```\\s*", "");
        int startIndex = cleaned.indexOf('{');
        int endIndex = cleaned.lastIndexOf('}');

        if (startIndex >= 0 && endIndex > startIndex) {
            cleaned = cleaned.substring(startIndex, endIndex + 1);
        }

        return cleaned.trim();
    }

    /**
     * Estimate costs using rule-based logic (FAST - < 10ms per node).
     * No LLM calls needed.
     */
    private void estimateCostsWithRules(List<NormalizedNode> nodes, String destination, 
                                       String budgetTier, String currency) {
        logger.info("═══════════════════════════════════════════════════════");
        logger.info("🚀 [RULE-BASED COST ESTIMATION] Starting for {} nodes", nodes.size());
        logger.info("   Destination: {}", destination);
        logger.info("   Budget Tier: {}", budgetTier);
        logger.info("   Currency: {}", currency);
        logger.info("═══════════════════════════════════════════════════════");
        
        long startTime = System.currentTimeMillis();
        int successCount = 0;
        int failureCount = 0;
        
        for (int i = 0; i < nodes.size(); i++) {
            NormalizedNode node = nodes.get(i);
            try {
                logger.info("📍 [Node {}/{}] Processing: {}", i + 1, nodes.size(), node.getId());
                
                // Get node details
                String nodeType = node.getType() != null ? node.getType() : "attraction";
                String category = extractCategory(node);
                String title = node.getTitle() != null ? node.getTitle() : "";
                
                logger.info("   Title: '{}'", title);
                logger.info("   Type: {}, Category: {}", nodeType, category);
                
                // Estimate cost using rules
                double estimatedCost = costEstimationRules.estimateCostWithTitle(
                    nodeType, category, title, budgetTier, currency, destination);
                
                // Set cost on node
                if (node.getCost() == null) {
                    node.setCost(new NodeCost());
                }
                node.getCost().setAmountPerPerson(estimatedCost);
                node.getCost().setCurrency(currency);
                
                logger.info("✅ [Node {}/{}] Cost estimated: {} {}", 
                           i + 1, nodes.size(), Math.round(estimatedCost), currency);
                successCount++;
                
            } catch (Exception e) {
                logger.error("❌ [Node {}/{}] Failed to estimate cost for node {}: {}", 
                           i + 1, nodes.size(), node.getId(), e.getMessage(), e);
                // Set fallback cost
                if (node.getCost() == null) {
                    node.setCost(new NodeCost());
                }
                node.getCost().setAmountPerPerson(0.0);
                node.getCost().setCurrency(currency);
                failureCount++;
            }
        }
        
        long duration = System.currentTimeMillis() - startTime;
        
        logger.info("═══════════════════════════════════════════════════════");
        logger.info("✅ [RULE-BASED COST ESTIMATION] Complete");
        logger.info("   Total nodes: {}", nodes.size());
        logger.info("   Successful: {}", successCount);
        logger.info("   Failed: {}", failureCount);
        logger.info("   Duration: {} ms", duration);
        logger.info("   Avg per node: {} ms", nodes.size() > 0 ? duration / nodes.size() : 0);
        logger.info("═══════════════════════════════════════════════════════");
    }
    
    /**
     * Extract category from node for cost estimation.
     */
    private String extractCategory(NormalizedNode node) {
        logger.debug("   [extractCategory] Extracting category for node: {}", node.getId());
        
        // Try to get category from node.details
        if (node.getDetails() != null && node.getDetails().getCategory() != null && 
            !node.getDetails().getCategory().isEmpty()) {
            logger.debug("   [extractCategory] Found in node.details.category: {}", node.getDetails().getCategory());
            return node.getDetails().getCategory();
        }
        
        // Try from labels
        if (node.getLabels() != null && !node.getLabels().isEmpty()) {
            String label = node.getLabels().get(0);
            logger.debug("   [extractCategory] Found in labels: {}", label);
            return label;
        }
        
        // Try from agent data
        if (node.getAgentData() != null) {
            Object categoryObj = node.getAgentData().get("category");
            if (categoryObj != null) {
                logger.debug("   [extractCategory] Found in agentData: {}", categoryObj);
                return categoryObj.toString();
            }
        }
        
        // Fallback to type
        String fallback = node.getType() != null ? node.getType() : "general";
        logger.debug("   [extractCategory] Using fallback: {}", fallback);
        return fallback;
    }

    @Override
    protected <T> T executeInternal(String itineraryId, AgentRequest<T> request) {
        // Not used - estimateCosts is called directly
        return null;
    }

    @Override
    protected String getAgentName() {
        return "Cost Estimator Agent";
    }
}
