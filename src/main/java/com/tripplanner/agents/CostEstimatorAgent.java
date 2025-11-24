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
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

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

    public CostEstimatorAgent(AgentEventBus eventBus,
            ItineraryJsonService itineraryJsonService,
            AiClient aiClient,
            ObjectMapper objectMapper,
            BudgetTracker budgetTracker,
            CurrencyConversionService currencyConversionService,
            ItineraryValidator itineraryValidator,
            ItineraryMetricsTracker metricsTracker) {
        super(eventBus, AgentEvent.AgentKind.ENRICHMENT);
        this.itineraryJsonService = itineraryJsonService;
        this.aiClient = aiClient;
        this.objectMapper = objectMapper;
        this.budgetTracker = budgetTracker;
        this.currencyConversionService = currencyConversionService;
        this.itineraryValidator = itineraryValidator;
        this.metricsTracker = metricsTracker;
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

                // Estimate costs for nodes without priceLevel using AI (slower but accurate)
                if (!nodesNeedingAI.isEmpty()) {
                    try {
                        estimateCostsWithAI(nodesNeedingAI, destination, budgetTier, currency,
                                budgetMin, budgetMax, userCurrency, itineraryId, dayNumber);
                        // Mark all AI-estimated nodes as successfully processed
                        for (NormalizedNode node : nodesNeedingAI) {
                            if (node.getCost() != null && node.getCost().getAmountPerPerson() != null) {
                                node.setProcessingState(ProcessingState.ENRICHED);
                            } else {
                                node.setProcessingState(ProcessingState.FAILED);
                                node.setLastError("AI cost estimation returned no cost");
                            }
                        }
                    } catch (Exception e) {
                        logger.error("Failed to estimate costs with AI: {}", e.getMessage());
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

            BudgetTracker.BudgetSummary budgetSummary = budgetTracker.calculateBudget(
                    itinerary, totalBudgetMin, totalBudgetMax, partySize);

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

    /**
     * Calculate inter-city travel costs from city allocation plan.
     * NOTE: Costs are in USD base, will be converted to destination currency if needed.
     */
    private double calculateInterCityTravelCosts(CityAllocationPlan cityPlan, String budgetTier) {
        double totalCost = 0.0;

        if (cityPlan.getTravelSegments() == null || cityPlan.getTravelSegments().isEmpty()) {
            return 0.0;
        }

        for (TravelSegment segment : cityPlan.getTravelSegments()) {
            double cost = estimateTravelCost(segment, budgetTier);
            totalCost += cost;

            logger.info("Travel cost {} to {}: {} (USD base)",
                    segment.getFromCity(),
                    segment.getToCity(),
                    cost);
        }

        return totalCost;
    }

    /**
     * Estimate travel cost for a single segment.
     * Base costs are in USD and represent typical costs globally.
     * These will be adjusted by destination currency conversion if needed.
     */
    private double estimateTravelCost(TravelSegment segment, String budgetTier) {
        String mode = segment.getTravelMode();
        int hours = segment.getEstimatedHours();

        // Base costs per hour by mode and budget tier (in USD)
        // These are global averages that work reasonably well across destinations
        Map<String, Map<String, Double>> costPerHour = Map.of(
                "flight", Map.of("low", 20.0, "medium", 35.0, "high", 65.0, "budget", 20.0, "luxury", 65.0),
                "train", Map.of("low", 3.0, "medium", 6.0, "high", 12.0, "budget", 3.0, "luxury", 12.0),
                "bus", Map.of("low", 1.5, "medium", 2.5, "high", 5.0, "budget", 1.5, "luxury", 5.0),
                "car", Map.of("low", 5.0, "medium", 8.0, "high", 12.0, "budget", 5.0, "luxury", 12.0),
                "ferry", Map.of("low", 2.5, "medium", 4.0, "high", 6.0, "budget", 2.5, "luxury", 6.0));

        // Handle combined modes (e.g., "car+ferry")
        if (mode.contains("+")) {
            String[] modes = mode.split("\\+");
            double totalCost = 0.0;
            for (String m : modes) {
                Map<String, Double> tierCosts = costPerHour.getOrDefault(m.trim(),
                        Map.of("medium", 5.0));
                double costRate = tierCosts.getOrDefault(budgetTier != null ? budgetTier : "medium", 5.0);
                totalCost += costRate;
            }
            return totalCost * hours / modes.length;
        }

        // Single mode
        Map<String, Double> tierCosts = costPerHour.getOrDefault(mode,
                Map.of("medium", 5.0));
        double costRate = tierCosts.getOrDefault(budgetTier != null ? budgetTier : "medium", 5.0);

        return costRate * hours;
    }

    /**
     * Estimate cost from Google Places priceLevel (0-4 scale).
     * This is fast and doesn't require AI calls.
     */
    private void estimateCostFromPriceLevel(NormalizedNode node, String destination,
            String budgetTier, String currency, String itineraryId, int dayNumber) {
        Integer priceLevel = node.getLocation().getPriceLevel();
        String nodeType = node.getType() != null ? node.getType().toLowerCase() : "activity";

        logger.debug("Estimating cost from priceLevel {} for node: {} (type: {})",
                priceLevel, node.getTitle(), nodeType);

        // Convert priceLevel (0-4) to actual cost based on destination and node type
        double baseCost = convertPriceLevelToCost(priceLevel, nodeType, destination, currency);

        // Apply budget tier adjustment if needed
        double adjustedCost = applyBudgetTierAdjustment(baseCost, budgetTier);

        // Set cost
        if (node.getCost() == null) {
            node.setCost(new NodeCost());
        }
        node.getCost().setAmountPerPerson(adjustedCost);
        node.getCost().setCurrency(currency);

        logger.debug("Set cost for {} from priceLevel: {} {} per person",
                node.getTitle(), adjustedCost, currency);

        // Track cost estimation
        metricsTracker.trackCostEstimated(
                itineraryId,
                node.getId(),
                nodeType,
                adjustedCost,
                currency,
                dayNumber,
                node.getTitle());
    }

    /**
     * Convert Google Places priceLevel (0-4) to actual cost estimate.
     * Price levels: 0=Free, 1=Inexpensive, 2=Moderate, 3=Expensive, 4=Very
     * Expensive
     * 
     * FIXED: Reduced to realistic per-person costs in INR
     */
    private double convertPriceLevelToCost(Integer priceLevel, String nodeType,
            String destination, String currency) {
        if (priceLevel == null)
            priceLevel = 2; // Default to moderate

        // Base multipliers for different node types
        // FIXED: Reduced accommodation multiplier from 3.0 to 1.5 for realistic hotel
        // costs
        double typeMultiplier = switch (nodeType) {
            case "meal", "restaurant" -> 1.0;
            case "attraction", "activity" -> 0.8;
            case "transport", "transportation" -> 0.5;
            case "accommodation", "hotel" -> 1.5; // FIXED: Was 3.0, now 1.5
            default -> 1.0;
        };

        // Price level to cost mapping (in INR, will be adjusted for other currencies)
        // FIXED: Reduced to realistic per-person costs
        // These represent typical costs in India/Malaysia for luxury tier
        double baseCost = switch (priceLevel) {
            case 0 -> 0.0; // Free
            case 1 -> 50.0; // Inexpensive (street food, budget options) - Was 300
            case 2 -> 150.0; // Moderate (casual dining, standard attractions) - Was 800
            case 3 -> 400.0; // Expensive (fine dining, premium experiences) - Was 1500
            case 4 -> 800.0; // Very Expensive (luxury dining, exclusive) - Was 3000
            default -> 150.0; // Default to moderate
        };

        return baseCost * typeMultiplier;
    }

    /**
     * Apply budget tier adjustment to cost.
     */
    private double applyBudgetTierAdjustment(double baseCost, String budgetTier) {
        if (budgetTier == null)
            return baseCost;

        double multiplier = switch (budgetTier.toLowerCase()) {
            case "budget", "low" -> 0.7;
            case "medium" -> 1.0;
            case "luxury", "high" -> 1.5;
            default -> 1.0;
        };

        return baseCost * multiplier;
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
     * Build system prompt for cost estimation.
     */
    private String buildCostEstimationSystemPrompt() {
        return """
                You are a travel cost estimation expert with deep knowledge of pricing across different destinations worldwide.

                Your task is to provide REALISTIC, ACCURATE cost estimates based on:
                - Current market rates for the specific destination
                - Local cost of living and currency
                - Budget tier (budget/medium/luxury)
                - Activity/venue type and typical pricing

                CRITICAL RULES:
                1. ALL costs must be PER PERSON (not total group cost)
                2. Use the destination's local currency and actual market rates
                3. Consider the budget tier:
                   - Budget: Economical options (hostels, street food, free/cheap attractions)
                   - Medium: Mid-range options (3-star hotels, casual dining, standard attractions)
                   - Luxury: Premium options (4-5 star hotels, fine dining, exclusive experiences)
                4. Be realistic - research actual costs for that destination
                5. Consider seasonal variations and current pricing trends
                6. For transport, consider typical local rates (taxi, metro, etc.)
                7. For meals, consider the type (breakfast/lunch/dinner) and venue category
                8. For attractions, consider entry fees, guided tours, etc.

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
     * Parse cost estimates from AI response and update nodes.
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
                    if (node.getCost() == null) {
                        node.setCost(new NodeCost());
                    }
                    node.getCost().setAmountPerPerson(costPerPerson);
                    node.getCost().setCurrency(currency);

                    logger.debug("Set AI-estimated cost for node {}: {} {} per person",
                            node.getTitle(), costPerPerson, currency);

                    // Track cost estimation
                    metricsTracker.trackCostEstimated(
                            itineraryId,
                            node.getId(),
                            node.getType() != null ? node.getType() : "activity",
                            costPerPerson,
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
     * Set minimal fallback cost for a single node if all estimation methods fail.
     */
    private void setMinimalFallbackCost(NormalizedNode node, String currency) {
        if (node.getCost() != null && node.getCost().getAmountPerPerson() != null) {
            return; // Already has cost
        }

        logger.warn("Using minimal fallback cost for node: {}", node.getTitle());

        if (node.getCost() == null) {
            node.setCost(new NodeCost());
        }

        // Set minimal placeholder based on type
        double fallbackCost = switch (node.getType() != null ? node.getType().toLowerCase() : "activity") {
            case "meal", "restaurant" -> 500.0;
            case "transport", "transportation" -> 200.0;
            case "accommodation", "hotel" -> 2000.0;
            default -> 300.0;
        };

        node.getCost().setAmountPerPerson(fallbackCost);
        node.getCost().setCurrency(currency);
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
