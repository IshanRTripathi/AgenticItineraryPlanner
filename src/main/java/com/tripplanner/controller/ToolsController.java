package com.tripplanner.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripplanner.dto.*;
import com.tripplanner.dto.tools.*;
import com.tripplanner.service.*;
import com.tripplanner.service.BudgetTracker.BudgetSummary;
import com.tripplanner.service.BudgetTracker.CategoryBudget;
import com.tripplanner.service.llm.LLMSchemaValidator;
import com.tripplanner.service.utilities.NodeIdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST API for ADK tools
 * Exposes itinerary operations as HTTP endpoints that can be called by Google
 * ADK agents
 */
@RestController
@RequestMapping("/api/v1/tools")
public class ToolsController {

    private static final Logger logger = LoggerFactory.getLogger(ToolsController.class);

    /**
     * Log tool usage with agent name for tracking and optimization
     */
    private void logToolUsage(String toolName, String agentName, String itineraryId) {
        logger.info("🔧 Tool: {}, Agent: {}, Itinerary: {}", 
            toolName, 
            agentName != null ? agentName : "unknown", 
            itineraryId != null ? itineraryId : "none");
    }

    private final BudgetTracker budgetTracker;
    private final ItineraryJsonService itineraryJsonService;
    private final NodeIdGenerator nodeIdGenerator;
    private final CurrencyConversionService currencyConversionService;
    private final ChangeEngine changeEngine;
    private final ObjectMapper objectMapper;
    private final LLMSchemaValidator schemaValidator;
    private final GoogleMapsDistanceService distanceService;
    private final GooglePlacesService placesService;
    private final GeocodingService geocodingService;
    private final TimingValidationService timingValidationService;
    private final WeatherService weatherService;
    private final LocationValidationService locationValidationService;
    private final ActivitySuitabilityService activitySuitabilityService;
    private final OpeningHoursService openingHoursService;
    private final DietaryVerificationService dietaryVerificationService;

    public ToolsController(BudgetTracker budgetTracker,
            ItineraryJsonService itineraryJsonService,
            NodeIdGenerator nodeIdGenerator,
            CurrencyConversionService currencyConversionService,
            ChangeEngine changeEngine,
            ObjectMapper objectMapper,
            LLMSchemaValidator schemaValidator,
            GoogleMapsDistanceService distanceService,
            GooglePlacesService placesService,
            GeocodingService geocodingService,
            TimingValidationService timingValidationService,
            WeatherService weatherService,
            LocationValidationService locationValidationService,
            ActivitySuitabilityService activitySuitabilityService,
            OpeningHoursService openingHoursService,
            DietaryVerificationService dietaryVerificationService) {
        this.budgetTracker = budgetTracker;
        this.itineraryJsonService = itineraryJsonService;
        this.nodeIdGenerator = nodeIdGenerator;
        this.currencyConversionService = currencyConversionService;
        this.changeEngine = changeEngine;
        this.objectMapper = objectMapper;
        this.schemaValidator = schemaValidator;
        this.distanceService = distanceService;
        this.placesService = placesService;
        this.geocodingService = geocodingService;
        this.timingValidationService = timingValidationService;
        this.weatherService = weatherService;
        this.locationValidationService = locationValidationService;
        this.activitySuitabilityService = activitySuitabilityService;
        this.openingHoursService = openingHoursService;
        this.dietaryVerificationService = dietaryVerificationService;
    }

    /**
     * P0-5: Calculate itinerary cost with enhanced budget tracking
     * 
     * This tool provides detailed cost breakdown with budget analysis,
     * warnings, and recommendations.
     */
    @PostMapping("/calculate-cost")
    public ResponseEntity<CostCalculationResult> calculateCost(
            @RequestBody CostCalculationRequest request,
            @RequestHeader(value = "X-Agent-Name", required = false) String agentName) {
        try {
            logToolUsage("calculate-cost", agentName, request.getItineraryId());
            logger.info("Calculating cost for itinerary: {}, partySize: {}",
                    request.getItineraryId(), request.getPartySize());

            // Validate request
            if (request.getItineraryId() == null || request.getItineraryId().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        CostCalculationResult.error("itineraryId is required"));
            }

            // Load itinerary
            Optional<NormalizedItinerary> itineraryOpt = itineraryJsonService.getItinerary(request.getItineraryId());
            if (itineraryOpt.isEmpty()) {
                return ResponseEntity.status(404).body(
                        CostCalculationResult.error("Itinerary not found: " + request.getItineraryId()));
            }

            NormalizedItinerary itinerary = itineraryOpt.get();

            // Use party size from request or itinerary
            int partySize = request.getPartySize() != null
                    ? request.getPartySize()
                    : (itinerary.getPartySize() != null ? itinerary.getPartySize() : 1);

            // Calculate budget
            BudgetSummary budget = budgetTracker.calculateBudget(itinerary, null, null, partySize);
            CategoryBudget breakdown = budget.getCategoryBreakdown();

            // Build result
            CostCalculationResult result = new CostCalculationResult();
            result.setSuccess(true);
            result.setTotalCostPerPerson(budget.getTotalCostPerPerson());
            result.setTotalCostForParty(budget.getTotalCostForParty());
            result.setCurrency(budget.getCurrency());
            result.setPartySize(partySize);

            // Add breakdown
            Map<String, Double> breakdownMap = new HashMap<>();
            if (breakdown != null) {
                breakdownMap.put("accommodation", breakdown.getAccommodation());
                breakdownMap.put("activities", breakdown.getAttractions());
                breakdownMap.put("meals", breakdown.getMeals());
                breakdownMap.put("transport", breakdown.getTransport());
                breakdownMap.put("other", breakdown.getOther());
            }
            result.setBreakdown(breakdownMap);

            // Add warnings from budget tracker
            if (budget.getWarnings() != null) {
                result.setWarnings(budget.getWarnings());
            }

            // Add budget analysis if requested
            if (Boolean.TRUE.equals(request.getIncludeBudgetAnalysis()) || request.getIncludeBudgetAnalysis() == null) {
                CostCalculationResult.BudgetAnalysis analysis = analyzeBudget(
                        itinerary,
                        budget.getTotalCostPerPerson(),
                        budget.getCurrency());
                result.setBudgetAnalysis(analysis);

                // Add budget warnings to main warnings list
                if (analysis != null && analysis.getRecommendations() != null) {
                    for (String recommendation : analysis.getRecommendations()) {
                        result.addWarning(recommendation);
                    }
                }
            }

            logger.info("Cost calculation complete: {} {} per person, {} {} total",
                    String.format("%.2f", result.getTotalCostPerPerson()), result.getCurrency(),
                    String.format("%.2f", result.getTotalCostForParty()), result.getCurrency());

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("Failed to calculate cost", e);
            return ResponseEntity.status(500).body(
                    CostCalculationResult.error("Failed to calculate cost: " + e.getMessage()));
        }
    }

    /**
     * Analyze budget and provide recommendations.
     */
    private CostCalculationResult.BudgetAnalysis analyzeBudget(
            NormalizedItinerary itinerary,
            double totalCostPerPerson,
            String currency) {

        if (itinerary.getBudgetMax() == null || itinerary.getBudgetMax() <= 0) {
            return null; // No budget set
        }

        CostCalculationResult.BudgetAnalysis analysis = new CostCalculationResult.BudgetAnalysis();
        double budgetMax = itinerary.getBudgetMax();

        analysis.setBudgetMax(budgetMax);
        analysis.setBudgetRemaining(budgetMax - totalCostPerPerson);

        double usedPercentage = (totalCostPerPerson / budgetMax) * 100;
        analysis.setBudgetUsedPercentage(usedPercentage);

        // Determine budget status
        if (totalCostPerPerson > budgetMax) {
            analysis.setBudgetStatus("OVER");
            double overage = totalCostPerPerson - budgetMax;
            analysis.setOverage(overage);
            analysis.addRecommendation(
                    String.format("BUDGET EXCEEDED: Cost (%.2f %s) exceeds budget (%.2f %s) by %.2f %s",
                            totalCostPerPerson, currency, budgetMax, currency, overage, currency));
            analysis.addRecommendation(
                    "Consider: reducing accommodation tier, fewer paid activities, or budget-friendly meals");
        } else if (usedPercentage >= 95) {
            analysis.setBudgetStatus("NEAR");
            analysis.addRecommendation(
                    String.format("APPROACHING LIMIT: Using %.0f%% of budget (%.2f / %.2f %s)",
                            usedPercentage, totalCostPerPerson, budgetMax, currency));
            analysis.addRecommendation("Very little budget remaining for unexpected expenses");
        } else if (usedPercentage >= 85) {
            analysis.setBudgetStatus("NEAR");
            analysis.addRecommendation(
                    String.format("HIGH USAGE: Using %.0f%% of budget (%.2f / %.2f %s)",
                            usedPercentage, totalCostPerPerson, budgetMax, currency));
            analysis.addRecommendation("Consider leaving buffer for unexpected costs");
        } else if (usedPercentage >= 70) {
            analysis.setBudgetStatus("MODERATE");
            analysis.addRecommendation(
                    String.format("MODERATE USAGE: Using %.0f%% of budget (%.2f / %.2f %s)",
                            usedPercentage, totalCostPerPerson, budgetMax, currency));
        } else {
            analysis.setBudgetStatus("UNDER");
            analysis.addRecommendation(
                    String.format("WELL WITHIN BUDGET: Using %.0f%% of budget (%.2f / %.2f %s)",
                            usedPercentage, totalCostPerPerson, budgetMax, currency));
            if (usedPercentage < 50) {
                analysis.addRecommendation(
                        "Consider: upgrading accommodation, adding premium activities, or extending trip");
            }
        }

        return analysis;
    }

    @GetMapping("/schema/calculate-cost")
    public ResponseEntity<Map<String, Object>> getCalculateCostSchema() {
        return ResponseEntity.ok(Map.of(
                "name", "calculate_itinerary_cost",
                "description", "Calculate total estimated cost with detailed budget analysis and recommendations",
                "parameters", Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "itineraryId", Map.of(
                                        "type", "string",
                                        "description", "ID of the itinerary to calculate costs for"),
                                "partySize", Map.of(
                                        "type", "integer",
                                        "description",
                                        "Number of people in the party (optional, uses itinerary default)"),
                                "includeBudgetAnalysis", Map.of(
                                        "type", "boolean",
                                        "description",
                                        "Include detailed budget analysis with recommendations (default: true)")),
                        "required", new String[] { "itineraryId" })));
    }

    // ========== P0-1: Node ID Generation Tool ==========

    /**
     * P0-1: Generate unique node ID to prevent conflicts
     * 
     * This tool provides centralized ID generation to ensure no duplicate node IDs.
     * All agents should call this tool BEFORE creating nodes.
     */
    @PostMapping("/generate-node-id")
    public ResponseEntity<NodeIdResponse> generateNodeId(
            @RequestBody NodeIdRequest request,
            @RequestHeader(value = "X-Agent-Name", required = false) String agentName) {
        try {
            logToolUsage("generate-node-id", agentName, request.getItineraryId());
            logger.info("Generating node ID for itinerary: {}, day: {}, type: {}",
                    request.getItineraryId(), request.getDayNumber(), request.getNodeType());

            // Validate request
            if (request.getItineraryId() == null || request.getItineraryId().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        NodeIdResponse.error("itineraryId is required"));
            }

            if (request.getDayNumber() == null) {
                return ResponseEntity.badRequest().body(
                        NodeIdResponse.error("dayNumber is required"));
            }

            if (request.getNodeType() == null || request.getNodeType().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        NodeIdResponse.error("nodeType is required"));
            }

            // Load itinerary to check existing IDs
            Optional<NormalizedItinerary> itineraryOpt = itineraryJsonService.getItinerary(request.getItineraryId());
            if (itineraryOpt.isEmpty()) {
                return ResponseEntity.status(404).body(
                        NodeIdResponse.error("Itinerary not found: " + request.getItineraryId()));
            }

            // Generate unique ID
            String nodeId = nodeIdGenerator.generateNodeId(
                    request.getNodeType(),
                    request.getDayNumber(),
                    itineraryOpt.get());

            logger.info("Generated node ID: {}", nodeId);
            return ResponseEntity.ok(new NodeIdResponse(nodeId));

        } catch (Exception e) {
            logger.error("Failed to generate node ID", e);
            return ResponseEntity.status(500).body(
                    NodeIdResponse.error("Failed to generate node ID: " + e.getMessage()));
        }
    }

    @GetMapping("/schema/generate-node-id")
    public ResponseEntity<Map<String, Object>> getGenerateNodeIdSchema() {
        return ResponseEntity.ok(Map.of(
                "name", "generate_node_id",
                "description", "Generate a unique node ID to prevent conflicts. Call this BEFORE creating any node.",
                "parameters", Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "itineraryId", Map.of(
                                        "type", "string",
                                        "description", "ID of the itinerary"),
                                "dayNumber", Map.of(
                                        "type", "integer",
                                        "description", "Day number (1-based)"),
                                "nodeType", Map.of(
                                        "type", "string",
                                        "description", "Type of node: attraction, meal, transport, or accommodation",
                                        "enum", new String[] { "attraction", "meal", "transport", "accommodation" }),
                                "sequenceHint", Map.of(
                                        "type", "integer",
                                        "description", "Optional: suggested sequence number")),
                        "required", new String[] { "itineraryId", "dayNumber", "nodeType" })));
    }

    // ========== P0-2: Constraint Propagation Tool ==========

    /**
     * P0-2: Check user constraints (budget, dietary restrictions, party size)
     * 
     * This tool validates that the itinerary respects all user constraints.
     * Agents should call this tool before generating nodes to ensure compliance.
     */
    @PostMapping("/check-user-constraints")
    public ResponseEntity<ConstraintCheckResult> checkUserConstraints(
            @RequestBody ConstraintCheckRequest request,
            @RequestHeader(value = "X-Agent-Name", required = false) String agentName) {
        try {
            logToolUsage("check-user-constraints", agentName, request.getItineraryId());
            logger.info("Checking constraints for itinerary: {}", request.getItineraryId());

            // Validate request
            if (request.getItineraryId() == null || request.getItineraryId().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        ConstraintCheckResult.error("itineraryId is required"));
            }

            // Load itinerary with constraints
            Optional<NormalizedItinerary> itineraryOpt = itineraryJsonService.getItinerary(request.getItineraryId());
            if (itineraryOpt.isEmpty()) {
                return ResponseEntity.status(404).body(
                        ConstraintCheckResult.error("Itinerary not found: " + request.getItineraryId()));
            }

            NormalizedItinerary itinerary = itineraryOpt.get();
            ConstraintCheckResult result = new ConstraintCheckResult();

            // Check budget constraints
            if (Boolean.TRUE.equals(request.getCheckBudget()) && itinerary.getBudgetMax() != null) {
                checkBudgetConstraints(itinerary, result);
            }

            // Check dietary restrictions
            if (Boolean.TRUE.equals(request.getCheckDietary()) && itinerary.getConstraints() != null) {
                checkDietaryConstraints(itinerary, request.getProposedNode(), result);
            }

            // Check party size constraints
            if (Boolean.TRUE.equals(request.getCheckPartySize()) && request.getProposedNode() != null) {
                checkPartySizeConstraints(itinerary, request.getProposedNode(), result);
            }

            logger.info("Constraint check complete. Valid: {}, Violations: {}",
                    result.isValid(), result.getViolations().size());

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("Failed to check constraints", e);
            return ResponseEntity.status(500).body(
                    ConstraintCheckResult.error("Failed to check constraints: " + e.getMessage()));
        }
    }

    @GetMapping("/schema/check-user-constraints")
    public ResponseEntity<Map<String, Object>> getCheckUserConstraintsSchema() {
        return ResponseEntity.ok(Map.of(
                "name", "check_user_constraints",
                "description", "Validate that itinerary respects user constraints (budget, dietary, party size)",
                "parameters", Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "itineraryId", Map.of(
                                        "type", "string",
                                        "description", "ID of the itinerary"),
                                "proposedNode", Map.of(
                                        "type", "object",
                                        "description", "Optional: specific node to validate"),
                                "checkBudget", Map.of(
                                        "type", "boolean",
                                        "description", "Check budget constraints (default: true)"),
                                "checkDietary", Map.of(
                                        "type", "boolean",
                                        "description", "Check dietary restrictions (default: true)"),
                                "checkPartySize", Map.of(
                                        "type", "boolean",
                                        "description", "Check party size constraints (default: true)")),
                        "required", new String[] { "itineraryId" })));
    }

    // ========== Helper Methods ==========

    private void checkBudgetConstraints(NormalizedItinerary itinerary, ConstraintCheckResult result) {
        try {
            int partySize = itinerary.getPartySize() != null ? itinerary.getPartySize() : 1;
            BudgetSummary budget = budgetTracker.calculateBudget(itinerary, null, null, partySize);

            double totalCostPerPerson = budget.getTotalCostPerPerson();
            double budgetMax = itinerary.getBudgetMax();

            if (totalCostPerPerson > budgetMax) {
                double overage = totalCostPerPerson - budgetMax;
                result.addViolation("BUDGET_EXCEEDED",
                        String.format("Total cost per person (%.2f %s) exceeds budget (%.2f %s) by %.2f",
                                totalCostPerPerson, budget.getCurrency(),
                                budgetMax, budget.getCurrency(), overage));
            } else if (totalCostPerPerson > budgetMax * 0.9) {
                result.addWarning(String.format("Approaching budget limit: %.0f%% used",
                        (totalCostPerPerson / budgetMax) * 100));
            }
        } catch (Exception e) {
            logger.warn("Failed to check budget constraints", e);
            result.addWarning("Could not validate budget: " + e.getMessage());
        }
    }

    private void checkDietaryConstraints(NormalizedItinerary itinerary, NormalizedNode proposedNode,
            ConstraintCheckResult result) {
        List<String> constraints = itinerary.getConstraints();
        if (constraints == null || constraints.isEmpty()) {
            return;
        }

        // Extract dietary restrictions
        for (String constraint : constraints) {
            String lowerConstraint = constraint.toLowerCase();

            if (lowerConstraint.contains("vegetarian") || lowerConstraint.contains("vegan") ||
                    lowerConstraint.contains("halal") || lowerConstraint.contains("kosher") ||
                    lowerConstraint.contains("gluten-free") || lowerConstraint.contains("dairy-free")) {

                // If checking a specific meal node
                if (proposedNode != null && "meal".equals(proposedNode.getType())) {
                    // Check if node metadata indicates dietary compliance
                    // This is a placeholder - actual implementation would check node details
                    result.addWarning("Ensure meal '" + proposedNode.getTitle() +
                            "' complies with dietary restriction: " + constraint);
                } else {
                    // General check - ensure all meal nodes comply
                    result.addWarning("Dietary restriction detected: " + constraint +
                            ". Ensure all meals comply.");
                }
            }
        }
    }

    private void checkPartySizeConstraints(NormalizedItinerary itinerary, NormalizedNode proposedNode,
            ConstraintCheckResult result) {
        Integer partySize = itinerary.getPartySize();
        if (partySize == null || partySize <= 0) {
            return;
        }

        // Check if proposed node has capacity constraints
        // This is a placeholder - actual implementation would check node metadata
        if (partySize > 10) {
            result.addWarning(
                    "Large party size (" + partySize + "). Ensure activities and restaurants can accommodate.");
        }
    }

    // ========== P0-6: Currency Conversion Tool ==========

    /**
     * P0-6: Convert currency amounts for accurate cost estimation
     * 
     * This tool provides currency conversion to help agents estimate costs
     * in the destination's local currency.
     */
    @PostMapping("/convert-currency")
    public ResponseEntity<Map<String, Object>> convertCurrency(
            @RequestBody Map<String, Object> request,
            @RequestHeader(value = "X-Agent-Name", required = false) String agentName) {
        try {
            logToolUsage("convert-currency", agentName, null);
            logger.info("Converting currency: {}", request);

            // Validate request
            if (!request.containsKey("amount") || !request.containsKey("fromCurrency") ||
                    !request.containsKey("toCurrency")) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "amount, fromCurrency, and toCurrency are required"));
            }

            double amount = ((Number) request.get("amount")).doubleValue();
            String fromCurrency = (String) request.get("fromCurrency");
            String toCurrency = (String) request.get("toCurrency");

            // Validate currencies
            if (fromCurrency == null || fromCurrency.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "fromCurrency cannot be empty"));
            }

            if (toCurrency == null || toCurrency.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "toCurrency cannot be empty"));
            }

            // Perform conversion
            double convertedAmount = currencyConversionService.convert(amount, fromCurrency, toCurrency);

            // Get currency symbols
            String fromSymbol = currencyConversionService.getCurrencySymbol(fromCurrency);
            String toSymbol = currencyConversionService.getCurrencySymbol(toCurrency);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "fromCurrency", fromCurrency,
                    "toCurrency", toCurrency,
                    "originalAmount", amount,
                    "convertedAmount", convertedAmount,
                    "fromSymbol", fromSymbol,
                    "toSymbol", toSymbol,
                    "formattedOriginal", currencyConversionService.formatAmount(amount, fromCurrency),
                    "formattedConverted", currencyConversionService.formatAmount(convertedAmount, toCurrency)));

        } catch (Exception e) {
            logger.error("Failed to convert currency", e);
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "error", e.getMessage()));
        }
    }

    @GetMapping("/schema/convert-currency")
    public ResponseEntity<Map<String, Object>> getConvertCurrencySchema() {
        return ResponseEntity.ok(Map.of(
                "name", "convert_currency",
                "description", "Convert amount from one currency to another for accurate cost estimation",
                "parameters", Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "amount", Map.of(
                                        "type", "number",
                                        "description", "Amount to convert"),
                                "fromCurrency", Map.of(
                                        "type", "string",
                                        "description", "Source currency code (e.g., INR, USD, EUR)"),
                                "toCurrency", Map.of(
                                        "type", "string",
                                        "description", "Target currency code (e.g., MYR, JPY, GBP)")),
                        "required", new String[] { "amount", "fromCurrency", "toCurrency" })));
    }

    // ========== P1-1: Time Conflict Detection Tool ==========

    /**
     * P1-1: Check for time conflicts and budget issues before applying changes
     * 
     * This tool validates proposed changes to detect scheduling conflicts
     * and budget violations before they are applied.
     */
    @PostMapping("/check-conflicts")
    public ResponseEntity<ConflictCheckResult> checkConflicts(
            @RequestBody ConflictCheckRequest request,
            @RequestHeader(value = "X-Agent-Name", required = false) String agentName) {
        try {
            logToolUsage("check-conflicts", agentName, request.getItineraryId());
            logger.info("Checking conflicts for itinerary: {}", request.getItineraryId());

            // Validate request
            if (request.getItineraryId() == null || request.getItineraryId().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        ConflictCheckResult.error("itineraryId is required"));
            }

            // Load current itinerary
            Optional<NormalizedItinerary> itineraryOpt = itineraryJsonService.getItinerary(request.getItineraryId());
            if (itineraryOpt.isEmpty()) {
                return ResponseEntity.status(404).body(
                        ConflictCheckResult.error("Itinerary not found: " + request.getItineraryId()));
            }

            NormalizedItinerary current = itineraryOpt.get();
            ConflictCheckResult result = new ConflictCheckResult();

            // If no proposed changes, check current state only
            NormalizedItinerary toCheck = current;

            // If proposed changes provided, apply them to a copy
            if (request.getProposedChanges() != null && request.getProposedChanges().getOps() != null) {
                try {
                    // Create a copy and apply proposed changes
                    String json = objectMapper.writeValueAsString(current);
                    toCheck = objectMapper.readValue(json, NormalizedItinerary.class);

                    // Apply changes using ChangeEngine's propose method
                    ChangeEngine.ProposeResult proposeResult = changeEngine.propose(
                            request.getItineraryId(),
                            request.getProposedChanges());
                    toCheck = proposeResult.getProposed();

                } catch (Exception e) {
                    logger.error("Failed to apply proposed changes", e);
                    result.addWarning("Could not simulate proposed changes: " + e.getMessage());
                    // Continue with current state
                }
            }

            // Check for time conflicts
            if (Boolean.TRUE.equals(request.getCheckTimeConflicts())) {
                checkTimeConflicts(toCheck, result);
            }

            // Check for budget conflicts
            if (Boolean.TRUE.equals(request.getCheckBudgetConflicts())) {
                checkBudgetConflicts(toCheck, result);
            }

            logger.info("Conflict check complete. Conflicts: {}, Warnings: {}",
                    result.getConflicts().size(), result.getWarnings().size());

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("Failed to check conflicts", e);
            return ResponseEntity.status(500).body(
                    ConflictCheckResult.error("Failed to check conflicts: " + e.getMessage()));
        }
    }

    @GetMapping("/schema/check-conflicts")
    public ResponseEntity<Map<String, Object>> getCheckConflictsSchema() {
        return ResponseEntity.ok(Map.of(
                "name", "check_conflicts",
                "description", "Check for time overlaps and budget conflicts before applying changes",
                "parameters", Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "itineraryId", Map.of(
                                        "type", "string",
                                        "description", "ID of the itinerary"),
                                "proposedChanges", Map.of(
                                        "type", "object",
                                        "description", "Optional: ChangeSet to validate before applying"),
                                "checkTimeConflicts", Map.of(
                                        "type", "boolean",
                                        "description", "Check for time overlaps (default: true)"),
                                "checkBudgetConflicts", Map.of(
                                        "type", "boolean",
                                        "description", "Check for budget violations (default: true)")),
                        "required", new String[] { "itineraryId" })));
    }

    // ========== Helper Methods for Conflict Detection ==========

    private void checkTimeConflicts(NormalizedItinerary itinerary, ConflictCheckResult result) {
        if (itinerary.getDays() == null) {
            return;
        }

        for (NormalizedDay day : itinerary.getDays()) {
            if (day.getNodes() == null || day.getNodes().size() < 2) {
                continue;
            }

            List<NormalizedNode> nodes = day.getNodes();

            // Check each pair of consecutive nodes for time overlap
            for (int i = 0; i < nodes.size() - 1; i++) {
                NormalizedNode current = nodes.get(i);
                NormalizedNode next = nodes.get(i + 1);

                if (hasTimeOverlap(current, next)) {
                    result.addConflict(new ConflictCheckResult.ConflictDetail(
                            "TIME_OVERLAP",
                            "ERROR",
                            day.getDayNumber(),
                            current.getId(),
                            next.getId(),
                            String.format("Time overlap detected between '%s' and '%s' on day %d",
                                    current.getTitle(), next.getTitle(), day.getDayNumber())));
                }
            }
        }
    }

    private boolean hasTimeOverlap(NormalizedNode node1, NormalizedNode node2) {
        // Check if both nodes have timing information
        if (node1.getTiming() == null || node2.getTiming() == null) {
            return false; // Can't determine overlap without timing
        }

        NodeTiming timing1 = node1.getTiming();
        NodeTiming timing2 = node2.getTiming();

        // Need both start and end times to check overlap
        if (timing1.getStartTime() == null || timing1.getEndTime() == null ||
                timing2.getStartTime() == null || timing2.getEndTime() == null) {
            return false;
        }

        // Check if node1 ends after node2 starts (overlap)
        // Overlap occurs if: node1.start < node2.end AND node2.start < node1.end
        return timing1.getStartTime() < timing2.getEndTime() &&
                timing2.getStartTime() < timing1.getEndTime();
    }

    private void checkBudgetConflicts(NormalizedItinerary itinerary, ConflictCheckResult result) {
        // Check if budget is set
        if (itinerary.getBudgetMax() == null || itinerary.getBudgetMax() <= 0) {
            return; // No budget to check against
        }

        try {
            int partySize = itinerary.getPartySize() != null ? itinerary.getPartySize() : 1;
            BudgetSummary budget = budgetTracker.calculateBudget(itinerary, null, null, partySize);

            double totalCostPerPerson = budget.getTotalCostPerPerson();
            double budgetMax = itinerary.getBudgetMax();

            if (totalCostPerPerson > budgetMax) {
                double overage = totalCostPerPerson - budgetMax;
                result.addConflict(new ConflictCheckResult.ConflictDetail(
                        "BUDGET_EXCEEDED",
                        "ERROR",
                        null,
                        null,
                        null,
                        String.format("Total cost per person (%.2f %s) exceeds budget (%.2f %s) by %.2f",
                                totalCostPerPerson, budget.getCurrency(),
                                budgetMax, budget.getCurrency(), overage)));
            } else if (totalCostPerPerson > budgetMax * 0.95) {
                result.addWarning(String.format("Approaching budget limit: %.0f%% used",
                        (totalCostPerPerson / budgetMax) * 100));
            }
        } catch (Exception e) {
            logger.warn("Failed to check budget conflicts", e);
            result.addWarning("Could not validate budget: " + e.getMessage());
        }
    }

    // ========== P1-2: JSON Schema Validation Tool ==========

    /**
     * P1-2: Validate LLM JSON output against a schema
     * 
     * This tool validates JSON responses from LLMs to ensure they match
     * the expected structure before processing.
     */
    @PostMapping("/validate-schema")
    public ResponseEntity<SchemaValidationResult> validateSchema(
            @RequestBody SchemaValidationRequest request,
            @RequestHeader(value = "X-Agent-Name", required = false) String agentName) {
        try {
            logToolUsage("validate-schema", agentName, null);
            logger.info("Validating JSON schema");

            // Validate request
            if (request.getJsonOutput() == null || request.getJsonOutput().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        SchemaValidationResult.error("jsonOutput is required"));
            }

            if (request.getJsonSchema() == null || request.getJsonSchema().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        SchemaValidationResult.error("jsonSchema is required"));
            }

            // Perform validation
            LLMSchemaValidator.ValidationResult validationResult;

            if (Boolean.TRUE.equals(request.getCleanBeforeValidation())) {
                validationResult = schemaValidator.cleanAndValidate(
                        request.getJsonOutput(),
                        request.getJsonSchema());
            } else {
                validationResult = schemaValidator.validate(
                        request.getJsonOutput(),
                        request.getJsonSchema());
            }

            // Build response
            SchemaValidationResult result = new SchemaValidationResult(
                    validationResult.isValid(),
                    validationResult.getErrors());

            result.setRetryable(schemaValidator.isRetryable(validationResult));
            result.setUserFriendlyMessage(schemaValidator.getUserFriendlyError(validationResult));

            logger.info("Schema validation complete. Valid: {}, Errors: {}",
                    result.isValid(), result.getErrors().size());

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("Failed to validate schema", e);
            return ResponseEntity.status(500).body(
                    SchemaValidationResult.error("Failed to validate schema: " + e.getMessage()));
        }
    }

    @GetMapping("/schema/validate-schema")
    public ResponseEntity<Map<String, Object>> getValidateSchemaSchema() {
        return ResponseEntity.ok(Map.of(
                "name", "validate_schema",
                "description", "Validate JSON output against a JSON schema to ensure correct structure",
                "parameters", Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "jsonOutput", Map.of(
                                        "type", "string",
                                        "description", "JSON string to validate"),
                                "jsonSchema", Map.of(
                                        "type", "string",
                                        "description", "JSON schema to validate against"),
                                "cleanBeforeValidation", Map.of(
                                        "type", "boolean",
                                        "description",
                                        "Clean markdown and extract JSON before validation (default: true)")),
                        "required", new String[] { "jsonOutput", "jsonSchema" })));
    }

    // ========== P1-3: Distance Calculation Tool ==========

    /**
     * P1-3: Calculate distance and travel time between locations
     * 
     * This tool provides accurate distance and duration calculations using
     * Google Maps API with fallback to Haversine formula.
     */
    @PostMapping("/calculate-distance")
    public ResponseEntity<DistanceCalculationResult> calculateDistance(
            @RequestBody DistanceCalculationRequest request,
            @RequestHeader(value = "X-Agent-Name", required = false) String agentName) {
        try {
            logToolUsage("calculate-distance", agentName, null);
            logger.info("Calculating distance from {} to {} (mode: {})",
                    request.getOrigin(), request.getDestination(), request.getMode());

            // Validate request
            if ((request.getOrigin() == null || request.getOrigin().isEmpty()) &&
                    request.getOriginCoordinates() == null) {
                return ResponseEntity.badRequest().body(
                        DistanceCalculationResult.error("origin or originCoordinates is required"));
            }

            if ((request.getDestination() == null || request.getDestination().isEmpty()) &&
                    request.getDestinationCoordinates() == null) {
                return ResponseEntity.badRequest().body(
                        DistanceCalculationResult.error("destination or destinationCoordinates is required"));
            }

            // Create NodeLocation objects for the service
            NodeLocation fromLocation = new NodeLocation();
            if (request.getOriginCoordinates() != null) {
                fromLocation.setCoordinates(request.getOriginCoordinates());
            } else {
                // If only address provided, we'd need geocoding here
                // For now, return error asking for coordinates
                return ResponseEntity.badRequest().body(
                        DistanceCalculationResult.error("originCoordinates required (geocoding not yet implemented)"));
            }

            NodeLocation toLocation = new NodeLocation();
            if (request.getDestinationCoordinates() != null) {
                toLocation.setCoordinates(request.getDestinationCoordinates());
            } else {
                return ResponseEntity.badRequest().body(
                        DistanceCalculationResult
                                .error("destinationCoordinates required (geocoding not yet implemented)"));
            }

            // Default mode to driving if not specified
            String mode = request.getMode() != null ? request.getMode() : "driving";

            // Calculate distance
            GoogleMapsDistanceService.DistanceResult distanceResult = distanceService.calculateDistance(fromLocation,
                    toLocation, mode);

            // Build response
            DistanceCalculationResult result = new DistanceCalculationResult(
                    distanceResult.getDistanceKm(),
                    distanceResult.getDurationMinutes(),
                    distanceResult.getMode(),
                    distanceResult.isFromAPI());

            result.setOrigin(request.getOrigin());
            result.setDestination(request.getDestination());

            // Add warnings if needed
            if (!distanceResult.isFromAPI()) {
                result.addWarning("Using fallback calculation (Haversine formula). " +
                        "Actual travel time may vary based on roads and traffic.");
            }

            if (!distanceService.isReasonable(distanceResult)) {
                result.addWarning("Distance calculation returned invalid data. Please verify locations.");
            }

            logger.info("Distance calculation complete: {}km, {}min",
                    String.format("%.2f", result.getDistanceKm()), result.getDurationMinutes());

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("Failed to calculate distance", e);
            return ResponseEntity.status(500).body(
                    DistanceCalculationResult.error("Failed to calculate distance: " + e.getMessage()));
        }
    }

    @GetMapping("/schema/calculate-distance")
    public ResponseEntity<Map<String, Object>> getCalculateDistanceSchema() {
        return ResponseEntity.ok(Map.of(
                "name", "calculate_distance",
                "description", "Calculate distance and travel time between two locations using Google Maps API",
                "parameters", Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "origin", Map.of(
                                        "type", "string",
                                        "description", "Origin location name (optional if originCoordinates provided)"),
                                "destination", Map.of(
                                        "type", "string",
                                        "description",
                                        "Destination location name (optional if destinationCoordinates provided)"),
                                "originCoordinates", Map.of(
                                        "type", "object",
                                        "description", "Origin coordinates {lat: number, lng: number}",
                                        "properties", Map.of(
                                                "lat", Map.of("type", "number"),
                                                "lng", Map.of("type", "number"))),
                                "destinationCoordinates", Map.of(
                                        "type", "object",
                                        "description", "Destination coordinates {lat: number, lng: number}",
                                        "properties", Map.of(
                                                "lat", Map.of("type", "number"),
                                                "lng", Map.of("type", "number"))),
                                "mode", Map.of(
                                        "type", "string",
                                        "description",
                                        "Transport mode: driving, walking, transit, bicycling (default: driving)",
                                        "enum", new String[] { "driving", "walking", "transit", "bicycling" })),
                        "required", new String[] { "originCoordinates", "destinationCoordinates" })));
    }

    // ========== P1-4: Restaurant Suggestion Tool ==========

    /**
     * P1-4: Suggest restaurants based on location and dietary restrictions
     * 
     * This tool provides restaurant recommendations filtered by dietary
     * restrictions, cuisine type, and price level.
     */
    @PostMapping("/suggest-restaurants")
    public ResponseEntity<RestaurantSuggestionResult> suggestRestaurants(
            @RequestBody RestaurantSuggestionRequest request,
            @RequestHeader(value = "X-Agent-Name", required = false) String agentName) {
        try {
            logToolUsage("suggest-restaurants", agentName, request.getItineraryId());
            logger.info("Suggesting restaurants for location: {}, cuisine: {}, dietary: {}",
                    request.getLocationName(), request.getCuisineType(), request.getDietaryRestrictions());

            // Validate request
            if (request.getLocation() == null &&
                    (request.getLocationName() == null || request.getLocationName().isEmpty())) {
                return ResponseEntity.badRequest().body(
                        RestaurantSuggestionResult.error("location or locationName is required"));
            }

            RestaurantSuggestionResult result = new RestaurantSuggestionResult();
            result.setSuccess(true);

            // Get dietary restrictions from itinerary if provided
            List<String> dietaryRestrictions = request.getDietaryRestrictions();
            if (request.getItineraryId() != null && !request.getItineraryId().isEmpty()) {
                Optional<NormalizedItinerary> itineraryOpt = itineraryJsonService
                        .getItinerary(request.getItineraryId());
                if (itineraryOpt.isPresent()) {
                    NormalizedItinerary itinerary = itineraryOpt.get();
                    if (itinerary.getConstraints() != null && !itinerary.getConstraints().isEmpty()) {
                        dietaryRestrictions = extractDietaryRestrictions(itinerary.getConstraints());
                        logger.info("✅ [P1-4] Extracted dietary restrictions from itinerary: {}", dietaryRestrictions);
                    }
                }
            }

            // P1-4 FIX: Actual Google Places API integration for restaurant suggestions
            logger.info("🍽️ [P1-4] Searching restaurants with Google Places API");
            logger.info("   Location: {}", request.getLocationName());
            logger.info("   Cuisine: {}", request.getCuisineType());
            logger.info("   Price Level: {}", request.getPriceLevel());
            logger.info("   Dietary Restrictions: {}", dietaryRestrictions);

            try {
                // Build search query
                StringBuilder searchQuery = new StringBuilder("restaurant");
                if (request.getCuisineType() != null && !request.getCuisineType().isEmpty()) {
                    searchQuery.append(" ").append(request.getCuisineType());
                }
                if (request.getMealType() != null && !request.getMealType().isEmpty()) {
                    searchQuery.append(" ").append(request.getMealType());
                }

                // Add dietary restrictions to search query for better filtering
                if (dietaryRestrictions != null && !dietaryRestrictions.isEmpty()) {
                    for (String restriction : dietaryRestrictions) {
                        searchQuery.append(" ").append(restriction);
                    }
                }

                logger.info("🔍 [P1-4] Search query: '{}'", searchQuery);

                // Search using Google Places API
                com.tripplanner.dto.PlaceSearchResult searchResult = placesService.searchPlace(
                        searchQuery.toString(),
                        request.getLocationName());

                if (searchResult != null) {
                    RestaurantSuggestionResult.RestaurantSuggestion restaurant = new RestaurantSuggestionResult.RestaurantSuggestion();

                    restaurant.setPlaceId(searchResult.getPlaceId());
                    restaurant.setName(searchResult.getName());
                    restaurant.setAddress(searchResult.getFormattedAddress());
                    restaurant.setRating(searchResult.getRating());
                    restaurant.setPriceLevel(searchResult.getPriceLevel());
                    restaurant.setUserRatingsTotal(searchResult.getUserRatingsTotal());

                    // Set cuisine types
                    if (searchResult.getTypes() != null && !searchResult.getTypes().isEmpty()) {
                        restaurant.setCuisineTypes(searchResult.getTypes());
                    }

                    // Note: Opening hours not available in search results
                    // Would need to call getPlaceDetails() for opening hours
                    restaurant.setOpenNow(null);

                    // Get photo reference
                    if (searchResult.getPhotos() != null && !searchResult.getPhotos().isEmpty()) {
                        restaurant.setPhotoReference(searchResult.getPhotos().get(0).getPhotoReference());
                    }

                    // Calculate distance if coordinates provided
                    if (request.getLocation() != null && searchResult.getGeometry() != null) {
                        double distance = calculateHaversineDistance(
                                request.getLocation(),
                                new com.tripplanner.dto.Coordinates(
                                        searchResult.getGeometry().getLocation().getLatitude(),
                                        searchResult.getGeometry().getLocation().getLongitude()));
                        restaurant.setDistanceKm(distance);
                        logger.info("📏 [P1-4] Restaurant distance: {:.2f} km", distance);
                    }

                    // Enhanced dietary verification with confidence scoring
                    if (dietaryRestrictions != null && !dietaryRestrictions.isEmpty()) {
                        List<DietaryVerificationService.DietaryMatch> dietaryMatches = dietaryVerificationService
                                .verifyDietarySupport(
                                        searchResult.getName(),
                                        searchResult.getTypes(),
                                        dietaryRestrictions);

                        boolean allSupported = dietaryVerificationService.allRestrictionsSupported(dietaryMatches);
                        int overallScore = dietaryVerificationService.getOverallScore(dietaryMatches);

                        restaurant.setMatchesDietaryRestrictions(allSupported);

                        // Add detailed warnings based on confidence
                        for (DietaryVerificationService.DietaryMatch match : dietaryMatches) {
                            if (match.getConfidenceScore() >= 80) {
                                logger.info("✅ High confidence for {}: {}", match.getRestriction(), match.getReason());
                            } else if (match.getConfidenceScore() >= 50) {
                                result.addWarning(String.format("⚠️ %s: %s",
                                        match.getRestriction(), match.getReason()));
                            } else {
                                result.addWarning(String.format("❌ %s: %s",
                                        match.getRestriction(), match.getReason()));
                            }
                        }

                        String warning = dietaryVerificationService.generateWarning(dietaryMatches);
                        if (warning != null) {
                            result.addWarning(warning);
                        }

                        logger.info("Dietary verification: overall score {}/100, all supported: {}",
                                overallScore, allSupported);
                    } else {
                        restaurant.setMatchesDietaryRestrictions(true); // No restrictions
                    }

                    result.addRestaurant(restaurant);
                    result.setTotalResults(1);

                    logger.info("✅ [P1-4] Found restaurant: {} (rating: {}, price: {})",
                            restaurant.getName(), restaurant.getRating(), restaurant.getPriceLevel());
                } else {
                    result.addWarning("❌ No restaurants found matching criteria");
                    logger.warn("❌ [P1-4] No restaurants found for query: {}", searchQuery);
                }

            } catch (Exception e) {
                logger.error("❌ [P1-4] Failed to search restaurants via Google Places API", e);
                result.addWarning("Failed to search restaurants: " + e.getMessage());
                result.addWarning("Falling back to manual restaurant selection");
            }

            logger.info("🍽️ [P1-4] Restaurant suggestion complete. Results: {}", result.getTotalResults());

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("Failed to suggest restaurants", e);
            return ResponseEntity.status(500).body(
                    RestaurantSuggestionResult.error("Failed to suggest restaurants: " + e.getMessage()));
        }
    }

    @GetMapping("/schema/suggest-restaurants")
    public ResponseEntity<Map<String, Object>> getSuggestRestaurantsSchema() {
        return ResponseEntity.ok(Map.of(
                "name", "suggest_restaurants",
                "description", "Get restaurant recommendations filtered by dietary restrictions, cuisine, and price",
                "parameters", Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "itineraryId", Map.of(
                                        "type", "string",
                                        "description", "Optional: Itinerary ID to extract dietary restrictions from"),
                                "location", Map.of(
                                        "type", "object",
                                        "description", "Location coordinates {lat: number, lng: number}",
                                        "properties", Map.of(
                                                "lat", Map.of("type", "number"),
                                                "lng", Map.of("type", "number"))),
                                "locationName", Map.of(
                                        "type", "string",
                                        "description", "Location name (required if location not provided)"),
                                "cuisineType", Map.of(
                                        "type", "string",
                                        "description", "Optional: Cuisine type (e.g., Italian, Indian, Chinese)"),
                                "priceLevel", Map.of(
                                        "type", "integer",
                                        "description", "Optional: Price level 1-4 ($ to $$$$)",
                                        "minimum", 1,
                                        "maximum", 4),
                                "mealType", Map.of(
                                        "type", "string",
                                        "description", "Optional: Meal type (breakfast, lunch, dinner)",
                                        "enum", new String[] { "breakfast", "lunch", "dinner" }),
                                "dietaryRestrictions", Map.of(
                                        "type", "array",
                                        "description",
                                        "Optional: Dietary restrictions (vegetarian, vegan, halal, kosher, gluten-free)",
                                        "items", Map.of("type", "string")),
                                "radius", Map.of(
                                        "type", "integer",
                                        "description", "Optional: Search radius in meters (default: 5000)")),
                        "required", new String[] {})));
    }

    // ========== Helper Methods ==========

    /**
     * Extract dietary restrictions from constraint list.
     */
    private List<String> extractDietaryRestrictions(List<String> constraints) {
        List<String> dietary = new ArrayList<>();
        if (constraints == null) {
            return dietary;
        }

        for (String constraint : constraints) {
            String lower = constraint.toLowerCase();
            if (lower.contains("vegetarian")) {
                dietary.add("vegetarian");
            }
            if (lower.contains("vegan")) {
                dietary.add("vegan");
            }
            if (lower.contains("halal")) {
                dietary.add("halal");
            }
            if (lower.contains("kosher")) {
                dietary.add("kosher");
            }
            if (lower.contains("gluten-free") || lower.contains("gluten free")) {
                dietary.add("gluten-free");
            }
            if (lower.contains("dairy-free") || lower.contains("dairy free") || lower.contains("lactose")) {
                dietary.add("dairy-free");
            }
            if (lower.contains("nut-free") || lower.contains("nut free") || lower.contains("nut allergy")) {
                dietary.add("nut-free");
            }
        }

        return dietary;
    }

    /**
     * Check if a restaurant matches dietary restrictions.
     * P1-4: Basic matching based on place types and name.
     * Note: Google Places API doesn't provide detailed menu information,
     * so this is a best-effort check based on available data.
     */
    private boolean checkDietaryMatch(com.tripplanner.dto.PlaceSearchResult place, List<String> dietaryRestrictions) {
        if (dietaryRestrictions == null || dietaryRestrictions.isEmpty()) {
            return true; // No restrictions to check
        }

        String placeName = place.getName() != null ? place.getName().toLowerCase() : "";
        List<String> placeTypes = place.getTypes() != null ? place.getTypes() : new ArrayList<>();

        // Check each dietary restriction
        for (String restriction : dietaryRestrictions) {
            String lower = restriction.toLowerCase();

            // Check if place name or types indicate support for restriction
            if (lower.contains("vegetarian") || lower.contains("vegan")) {
                // Look for vegetarian/vegan indicators
                if (placeName.contains("vegetarian") || placeName.contains("vegan") ||
                        placeName.contains("veggie") || placeTypes.contains("vegetarian_restaurant")) {
                    continue; // This restriction is likely supported
                }
                // If it's explicitly a steakhouse or meat-focused, it's not a match
                if (placeName.contains("steakhouse") || placeName.contains("bbq") ||
                        placeName.contains("grill") || placeName.contains("meat")) {
                    logger.warn("⚠️ [P1-4] Restaurant '{}' may not support {}", place.getName(), restriction);
                    return false;
                }
            }

            if (lower.contains("halal")) {
                if (placeName.contains("halal") || placeTypes.contains("halal_restaurant")) {
                    continue;
                }
            }

            if (lower.contains("kosher")) {
                if (placeName.contains("kosher") || placeTypes.contains("kosher_restaurant")) {
                    continue;
                }
            }

            // For gluten-free, dairy-free, nut-free - harder to determine from place data
            // We'll assume most restaurants can accommodate these with menu modifications
        }

        // If we got here, no explicit conflicts found
        // But we can't guarantee support, so this is a "maybe"
        return true;
    }

    // ========== P2-1: Geocode Address Tool ==========

    /**
     * P2-1: Convert address to coordinates
     * 
     * This tool provides geocoding to convert location names/addresses
     * to coordinates for distance calculations.
     */
    @PostMapping("/geocode")
    public ResponseEntity<GeocodeResult> geocode(
            @RequestBody GeocodeRequest request,
            @RequestHeader(value = "X-Agent-Name", required = false) String agentName) {
        try {
            logToolUsage("geocode", agentName, request.getItineraryId());
            logger.info("Geocoding address: {}", request.getAddress());

            // Validate request
            if (request.getAddress() == null || request.getAddress().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        GeocodeResult.error("address is required"));
            }

            // Geocode address (with caching if itineraryId provided)
            GeocodingService.GeocodeResponse geocodeResponse = geocodingService.geocode(
                    request.getItineraryId(),
                    request.getAddress(),
                    request.getCountry());

            com.tripplanner.dto.Coordinates coordinates = geocodeResponse.getCoordinates();

            // Build result
            GeocodeResult result = new GeocodeResult(
                    coordinates,
                    geocodeResponse.getFormattedAddress());
            result.setPlaceId(geocodeResponse.getPlaceId());

            // CRITICAL FIX: Add location validation if country hint is provided
            if (request.getCountry() != null && !request.getCountry().isEmpty()) {
                try {
                    // Geocode the country to get reference coordinates
                    GeocodingService.GeocodeResponse countryResponse = geocodingService.geocode(request.getCountry(),
                            null);
                    if (countryResponse != null && countryResponse.getCoordinates() != null) {
                        LocationValidationService.ValidationResult validation = locationValidationService
                                .validatePlaceLocation(
                                        coordinates,
                                        countryResponse.getCoordinates(),
                                        request.getAddress(),
                                        request.getCountry());

                        if (!validation.isValid()) {
                            result.addWarning("⚠️ Location may be in wrong country: " + validation.getReason() +
                                    String.format(" (%.0f km from %s)", validation.getDistanceKm(),
                                            request.getCountry()));
                            logger.error("Geocoded location validation FAILED: '{}' is {} km from '{}'",
                                    request.getAddress(), validation.getDistanceKm(), request.getCountry());
                        } else if (validation.isWarning()) {
                            result.addWarning("⚠️ Location is far from expected area: " + validation.getReason() +
                                    String.format(" (%.0f km from %s center)", validation.getDistanceKm(),
                                            request.getCountry()));
                            logger.warn("Geocoded location validation WARNING: '{}' is {} km from '{}'",
                                    request.getAddress(), validation.getDistanceKm(), request.getCountry());
                        } else {
                            logger.info("Geocoded location validation PASSED: '{}' is {} km from '{}'",
                                    request.getAddress(), validation.getDistanceKm(), request.getCountry());
                        }
                    }
                } catch (Exception e) {
                    logger.warn("Could not validate location against country '{}': {}", request.getCountry(),
                            e.getMessage());
                    result.addWarning("Could not validate location against country hint");
                }
            }

            logger.info("Geocoded '{}' to ({}, {})",
                    request.getAddress(),
                    result.getCoordinates().getLat(),
                    result.getCoordinates().getLng());

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("Failed to geocode address", e);
            return ResponseEntity.status(500).body(
                    GeocodeResult.error("Failed to geocode address: " + e.getMessage()));
        }
    }

    @GetMapping("/schema/geocode")
    public ResponseEntity<Map<String, Object>> getGeocodeSchema() {
        return ResponseEntity.ok(Map.of(
                "name", "geocode_address",
                "description", "Convert address or location name to coordinates for distance calculations",
                "parameters", Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "address", Map.of(
                                        "type", "string",
                                        "description",
                                        "Address or location name to geocode (e.g., 'Eiffel Tower, Paris')"),
                                "country", Map.of(
                                        "type", "string",
                                        "description",
                                        "Optional: Country code hint for better results (e.g., 'FR', 'US')")),
                        "required", new String[] { "address" })));
    }

    // ========== P2-2: Validate Timing Tool ==========

    /**
     * P2-2: Validate if proposed schedule is physically feasible
     * 
     * This tool checks for timing conflicts, insufficient travel time,
     * and unreasonably long days.
     */
    @PostMapping("/validate-timing")
    public ResponseEntity<TimingValidationResult> validateTiming(
            @RequestBody TimingValidationRequest request,
            @RequestHeader(value = "X-Agent-Name", required = false) String agentName) {
        try {
            logToolUsage("validate-timing", agentName, request.getItineraryId());
            logger.info("Validating timing for itinerary: {}, day: {}",
                    request.getItineraryId(), request.getDayNumber());

            // Validate request
            if (request.getItineraryId() == null || request.getItineraryId().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        TimingValidationResult.error("itineraryId is required"));
            }

            TimingValidationResult result;

            // If proposed schedule provided, validate it
            if (request.getProposedSchedule() != null && !request.getProposedSchedule().isEmpty()) {
                result = timingValidationService.validateSchedule(request.getProposedSchedule());
            }
            // Otherwise validate existing day
            else if (request.getDayNumber() != null) {
                Optional<NormalizedItinerary> itineraryOpt = itineraryJsonService
                        .getItinerary(request.getItineraryId());
                if (itineraryOpt.isEmpty()) {
                    return ResponseEntity.status(404).body(
                            TimingValidationResult.error("Itinerary not found: " + request.getItineraryId()));
                }

                result = timingValidationService.validateDay(itineraryOpt.get(), request.getDayNumber());
            } else {
                return ResponseEntity.badRequest().body(
                        TimingValidationResult.error("Either dayNumber or proposedSchedule is required"));
            }

            logger.info("Timing validation complete. Feasible: {}, Issues: {}",
                    result.isFeasible(), result.getIssues().size());

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("Failed to validate timing", e);
            return ResponseEntity.status(500).body(
                    TimingValidationResult.error("Failed to validate timing: " + e.getMessage()));
        }
    }

    @GetMapping("/schema/validate-timing")
    public ResponseEntity<Map<String, Object>> getValidateTimingSchema() {
        return ResponseEntity.ok(Map.of(
                "name", "validate_timing",
                "description",
                "Check if proposed schedule is physically feasible (enough time for travel, no overlaps)",
                "parameters", Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "itineraryId", Map.of(
                                        "type", "string",
                                        "description", "ID of the itinerary"),
                                "dayNumber", Map.of(
                                        "type", "integer",
                                        "description", "Day number to validate (if validating existing day)"),
                                "proposedSchedule", Map.of(
                                        "type", "array",
                                        "description", "Optional: Proposed schedule to validate before applying",
                                        "items", Map.of("type", "object"))),
                        "required", new String[] { "itineraryId" })));
    }

    // ========== P2-3: Get Transport Options Tool ==========

    /**
     * P2-3: Get available transport options between two locations
     * 
     * This tool provides actual transport options with modes, durations, and costs
     * instead of relying on LLM guesses.
     */
    @PostMapping("/get-transport-options")
    public ResponseEntity<GetTransportOptionsResult> getTransportOptions(
            @RequestBody GetTransportOptionsRequest request,
            @RequestHeader(value = "X-Agent-Name", required = false) String agentName) {
        try {
            logToolUsage("get-transport-options", agentName, null);
            logger.info("Getting transport options: {} -> {}", request.getOrigin(), request.getDestination());

            // Validate request
            if ((request.getOrigin() == null || request.getOrigin().isEmpty())
                    && request.getOriginCoordinates() == null) {
                return ResponseEntity.badRequest().body(
                        GetTransportOptionsResult.error("origin or originCoordinates is required"));
            }

            if ((request.getDestination() == null || request.getDestination().isEmpty())
                    && request.getDestinationCoordinates() == null) {
                return ResponseEntity.badRequest().body(
                        GetTransportOptionsResult.error("destination or destinationCoordinates is required"));
            }

            GetTransportOptionsResult result = new GetTransportOptionsResult();
            result.setSuccess(true);

            // Get coordinates if not provided
            com.tripplanner.dto.Coordinates originCoords = request.getOriginCoordinates();
            com.tripplanner.dto.Coordinates destCoords = request.getDestinationCoordinates();

            if (originCoords == null && request.getOrigin() != null) {
                GeocodingService.GeocodeResponse geocodeResponse = geocodingService.geocode(request.getOrigin(), null);
                if (geocodeResponse != null && geocodeResponse.getCoordinates() != null) {
                    originCoords = geocodeResponse.getCoordinates();
                } else {
                    result.addWarning("Could not geocode origin: " + request.getOrigin());
                }
            }

            if (destCoords == null && request.getDestination() != null) {
                GeocodingService.GeocodeResponse geocodeResponse = geocodingService.geocode(request.getDestination(),
                        null);
                if (geocodeResponse != null && geocodeResponse.getCoordinates() != null) {
                    destCoords = geocodeResponse.getCoordinates();
                } else {
                    result.addWarning("Could not geocode destination: " + request.getDestination());
                }
            }

            // Calculate distance using GeographyService
            double distanceKm = 0;
            if (originCoords != null && destCoords != null) {
                // Use simple haversine formula
                double lat1 = Math.toRadians(originCoords.getLat());
                double lat2 = Math.toRadians(destCoords.getLat());
                double lon1 = Math.toRadians(originCoords.getLng());
                double lon2 = Math.toRadians(destCoords.getLng());

                double dLat = lat2 - lat1;
                double dLon = lon2 - lon1;

                double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                        Math.cos(lat1) * Math.cos(lat2) *
                                Math.sin(dLon / 2) * Math.sin(dLon / 2);

                double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
                distanceKm = 6371 * c; // Earth radius in km
            }

            // Generate transport options based on distance and geography
            // This is a simplified implementation - in production, integrate with actual
            // transport APIs

            // Enhanced cost estimation based on distance ranges

            // Option 1: Flight (for distances > 300km)
            if (distanceKm > 300) {
                GetTransportOptionsResult.TransportOption flight = new GetTransportOptionsResult.TransportOption();
                flight.setMode("flight");
                flight.setDistanceKm(distanceKm);
                flight.setDurationMinutes((int) (distanceKm / 10) + 120); // ~600km/h + 2h for airport

                // Enhanced cost estimation
                double baseCost = 50; // Base airport/booking fee
                double perKmCost;
                if (distanceKm < 500) {
                    perKmCost = 0.20; // Short flights more expensive per km
                } else if (distanceKm < 1500) {
                    perKmCost = 0.12; // Medium distance
                } else {
                    perKmCost = 0.08; // Long distance cheaper per km
                }
                flight.setEstimatedCost(baseCost + (distanceKm * perKmCost));
                flight.setCurrency("USD");
                flight.setAvailable(true);
                flight.setNotes("Fastest option for long distances. Prices vary by season and booking time.");
                result.addOption(flight);
            }

            // Option 2: Train (for distances < 1000km)
            if (distanceKm < 1000) {
                GetTransportOptionsResult.TransportOption train = new GetTransportOptionsResult.TransportOption();
                train.setMode("train");
                train.setDistanceKm(distanceKm);
                train.setDurationMinutes((int) (distanceKm / 1.2)); // ~70km/h average

                // Enhanced cost estimation
                double baseCost = 10; // Base ticket fee
                double perKmCost;
                if (distanceKm < 100) {
                    perKmCost = 0.08; // Local trains
                } else if (distanceKm < 500) {
                    perKmCost = 0.05; // Regional trains
                } else {
                    perKmCost = 0.04; // Long-distance trains
                }
                train.setEstimatedCost(baseCost + (distanceKm * perKmCost));
                train.setCurrency("USD");
                train.setAvailable(true);
                train.setNotes("Comfortable and scenic. Good for medium distances.");
                result.addOption(train);
            }

            // Option 3: Bus (always available)
            GetTransportOptionsResult.TransportOption bus = new GetTransportOptionsResult.TransportOption();
            bus.setMode("bus");
            bus.setDistanceKm(distanceKm);
            bus.setDurationMinutes((int) (distanceKm / 0.8)); // ~50km/h average
            bus.setEstimatedCost(distanceKm * 0.03); // Cheapest option
            bus.setCurrency("USD");
            bus.setAvailable(true);
            bus.setNotes("Most economical option");
            result.addOption(bus);

            // Option 4: Car/Taxi (for distances < 500km)
            if (distanceKm < 500) {
                GetTransportOptionsResult.TransportOption car = new GetTransportOptionsResult.TransportOption();
                car.setMode("car");
                car.setDistanceKm(distanceKm);
                car.setDurationMinutes((int) (distanceKm / 1.0)); // ~60km/h average
                car.setEstimatedCost(distanceKm * 0.20); // Most expensive per km
                car.setCurrency("USD");
                car.setAvailable(true);
                car.setNotes("Most flexible, door-to-door");
                result.addOption(car);
            }

            logger.info("Found {} transport options for {} km distance", result.getOptions().size(), distanceKm);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("Failed to get transport options", e);
            return ResponseEntity.status(500).body(
                    GetTransportOptionsResult.error("Failed to get transport options: " + e.getMessage()));
        }
    }

    @GetMapping("/schema/get-transport-options")
    public ResponseEntity<Map<String, Object>> getTransportOptionsSchema() {
        return ResponseEntity.ok(Map.of(
                "name", "get_transport_options",
                "description", "Get available transport options between two locations with modes, durations, and costs",
                "parameters", Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "origin", Map.of(
                                        "type", "string",
                                        "description", "Origin location name"),
                                "destination", Map.of(
                                        "type", "string",
                                        "description", "Destination location name"),
                                "originCoordinates", Map.of(
                                        "type", "object",
                                        "description", "Optional: Origin coordinates {lat, lng}"),
                                "destinationCoordinates", Map.of(
                                        "type", "object",
                                        "description", "Optional: Destination coordinates {lat, lng}"),
                                "date", Map.of(
                                        "type", "string",
                                        "description", "Optional: Travel date (YYYY-MM-DD)"),
                                "time", Map.of(
                                        "type", "string",
                                        "description", "Optional: Travel time (HH:mm)")),
                        "required", new String[] {})));
    }

    // ========== P2-4: Optimize Route Tool ==========

    /**
     * P2-4: Optimize the order of activities to minimize travel time
     * 
     * Uses nearest-neighbor algorithm to reorder activities for minimal travel.
     */
    @PostMapping("/optimize-route")
    public ResponseEntity<OptimizeRouteResult> optimizeRoute(
            @RequestBody OptimizeRouteRequest request,
            @RequestHeader(value = "X-Agent-Name", required = false) String agentName) {
        try {
            logToolUsage("optimize-route", agentName, request.getItineraryId());
            logger.info("Optimizing route for itinerary: {}, day: {}", request.getItineraryId(),
                    request.getDayNumber());

            // Validate request
            if (request.getItineraryId() == null || request.getItineraryId().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        OptimizeRouteResult.error("itineraryId is required"));
            }

            if (request.getDayNumber() == null) {
                return ResponseEntity.badRequest().body(
                        OptimizeRouteResult.error("dayNumber is required"));
            }

            if (request.getNodeIds() == null || request.getNodeIds().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        OptimizeRouteResult.error("nodeIds is required"));
            }

            // Load itinerary
            Optional<NormalizedItinerary> itineraryOpt = itineraryJsonService.getItinerary(request.getItineraryId());
            if (itineraryOpt.isEmpty()) {
                return ResponseEntity.status(404).body(
                        OptimizeRouteResult.error("Itinerary not found: " + request.getItineraryId()));
            }

            NormalizedItinerary itinerary = itineraryOpt.get();

            // Find the day
            NormalizedDay day = itinerary.getDays().stream()
                    .filter(d -> d.getDayNumber().equals(request.getDayNumber()))
                    .findFirst()
                    .orElse(null);

            if (day == null) {
                return ResponseEntity.status(404).body(
                        OptimizeRouteResult.error("Day not found: " + request.getDayNumber()));
            }

            // Get nodes to optimize
            List<NormalizedNode> nodesToOptimize = new ArrayList<>();
            for (String nodeId : request.getNodeIds()) {
                NormalizedNode node = day.getNodes().stream()
                        .filter(n -> nodeId.equals(n.getId()))
                        .findFirst()
                        .orElse(null);

                if (node != null && node.getLocation() != null && node.getLocation().getCoordinates() != null) {
                    nodesToOptimize.add(node);
                } else {
                    logger.warn("Node {} not found or has no coordinates", nodeId);
                }
            }

            if (nodesToOptimize.size() < 2) {
                return ResponseEntity.badRequest().body(
                        OptimizeRouteResult.error("Need at least 2 nodes with coordinates to optimize"));
            }

            // Calculate original route distance
            double originalDistance = 0;
            int originalTime = 0;
            for (int i = 0; i < nodesToOptimize.size() - 1; i++) {
                com.tripplanner.dto.Coordinates from = nodesToOptimize.get(i).getLocation().getCoordinates();
                com.tripplanner.dto.Coordinates to = nodesToOptimize.get(i + 1).getLocation().getCoordinates();
                double dist = calculateHaversineDistance(from, to);
                originalDistance += dist;
                originalTime += (int) (dist / 0.5); // Assume 30km/h average in city
            }

            // Optimize using nearest-neighbor algorithm
            List<NormalizedNode> optimized = new ArrayList<>();
            List<NormalizedNode> remaining = new ArrayList<>(nodesToOptimize);

            // Start with first node (or start location if provided)
            NormalizedNode current = remaining.remove(0);
            optimized.add(current);

            // Greedily pick nearest neighbor
            while (!remaining.isEmpty()) {
                NormalizedNode nearest = null;
                double minDistance = Double.MAX_VALUE;

                com.tripplanner.dto.Coordinates currentCoords = current.getLocation().getCoordinates();

                for (NormalizedNode candidate : remaining) {
                    com.tripplanner.dto.Coordinates candidateCoords = candidate.getLocation().getCoordinates();
                    double dist = calculateHaversineDistance(currentCoords, candidateCoords);

                    if (dist < minDistance) {
                        minDistance = dist;
                        nearest = candidate;
                    }
                }

                if (nearest != null) {
                    optimized.add(nearest);
                    remaining.remove(nearest);
                    current = nearest;
                }
            }

            // Calculate optimized route distance
            double optimizedDistance = 0;
            int optimizedTime = 0;
            for (int i = 0; i < optimized.size() - 1; i++) {
                com.tripplanner.dto.Coordinates from = optimized.get(i).getLocation().getCoordinates();
                com.tripplanner.dto.Coordinates to = optimized.get(i + 1).getLocation().getCoordinates();
                double dist = calculateHaversineDistance(from, to);
                optimizedDistance += dist;
                optimizedTime += (int) (dist / 0.5);
            }

            // Build result
            OptimizeRouteResult result = new OptimizeRouteResult();
            result.setSuccess(true);
            result.setOptimizedOrder(
                    optimized.stream().map(NormalizedNode::getId).collect(java.util.stream.Collectors.toList()));
            result.setTotalDistanceKm(optimizedDistance);
            result.setTotalTimeMinutes(optimizedTime);
            result.setOriginalDistanceKm(originalDistance);
            result.setOriginalTimeMinutes(originalTime);
            result.setDistanceSavingsKm(originalDistance - optimizedDistance);
            result.setTimeSavingsMinutes(originalTime - optimizedTime);

            if (result.getDistanceSavingsKm() > 0) {
                logger.info("Route optimized: saved {} km and {} minutes",
                        String.format("%.2f", result.getDistanceSavingsKm()),
                        result.getTimeSavingsMinutes());
            } else {
                result.addWarning("Original route was already optimal or close to optimal");
            }

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("Failed to optimize route", e);
            return ResponseEntity.status(500).body(
                    OptimizeRouteResult.error("Failed to optimize route: " + e.getMessage()));
        }
    }

    @GetMapping("/schema/optimize-route")
    public ResponseEntity<Map<String, Object>> getOptimizeRouteSchema() {
        return ResponseEntity.ok(Map.of(
                "name", "optimize_route",
                "description",
                "Optimize the order of activities to minimize travel time using nearest-neighbor algorithm",
                "parameters", Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "itineraryId", Map.of(
                                        "type", "string",
                                        "description", "ID of the itinerary"),
                                "dayNumber", Map.of(
                                        "type", "integer",
                                        "description", "Day number to optimize"),
                                "nodeIds", Map.of(
                                        "type", "array",
                                        "description", "List of node IDs to optimize (in current order)",
                                        "items", Map.of("type", "string")),
                                "startLocation", Map.of(
                                        "type", "object",
                                        "description", "Optional: Fixed start location {lat, lng}"),
                                "endLocation", Map.of(
                                        "type", "object",
                                        "description", "Optional: Fixed end location {lat, lng}")),
                        "required", new String[] { "itineraryId", "dayNumber", "nodeIds" })));
    }

    // ========== P2-5: Check Opening Hours Tool ==========

    /**
     * P2-5: Check if a place is open at a specific date/time
     * 
     * Uses Google Places API to check opening hours and provide recommendations.
     */
    @PostMapping("/check-opening-hours")
    public ResponseEntity<CheckOpeningHoursResult> checkOpeningHours(
            @RequestBody CheckOpeningHoursRequest request,
            @RequestHeader(value = "X-Agent-Name", required = false) String agentName) {
        try {
            logToolUsage("check-opening-hours", agentName, null);
            logger.info("Checking opening hours for place: {}, date: {}, time: {}",
                    request.getPlaceId(), request.getDate(), request.getTime());

            // Validate request
            if (request.getPlaceId() == null || request.getPlaceId().isEmpty()) {
                if (request.getPlaceName() == null || request.getPlaceName().isEmpty()) {
                    return ResponseEntity.badRequest().body(
                            CheckOpeningHoursResult.error("placeId or placeName is required"));
                }
            }

            CheckOpeningHoursResult result = new CheckOpeningHoursResult();
            result.setSuccess(true);

            // Try to get place details from Google Places API
            try {
                if (request.getPlaceId() != null && !request.getPlaceId().isEmpty()) {
                    com.tripplanner.dto.PlaceDetails placeDetails = placesService.getPlaceDetails(request.getPlaceId());

                    if (placeDetails != null && placeDetails.getOpeningHours() != null) {
                        // Get basic open/closed status
                        boolean openNow = placeDetails.getOpeningHours().getOpenNow() != null &&
                                placeDetails.getOpeningHours().getOpenNow();
                        result.setOpen(openNow);

                        // Get weekday text (full week schedule)
                        List<String> weekdayText = placeDetails.getOpeningHours().getWeekdayText();
                        if (weekdayText != null && !weekdayText.isEmpty()) {
                            result.setWeekdayText(weekdayText);

                            // Parse hours for today (or requested date)
                            java.time.DayOfWeek dayOfWeek = request.getDate() != null
                                    ? java.time.LocalDate.parse(request.getDate()).getDayOfWeek()
                                    : java.time.LocalDate.now().getDayOfWeek();

                            OpeningHoursService.ParsedHours todayHours = openingHoursService
                                    .parseHoursForDay(weekdayText, dayOfWeek);

                            if (todayHours != null) {
                                result.setOpensAt(todayHours.getOpensAt());
                                result.setClosesAt(todayHours.getClosesAt());

                                // Get today's hours in readable format
                                String todayHoursText = openingHoursService.getTodayHoursText(weekdayText, dayOfWeek);
                                result.setTodayHours(todayHoursText);

                                // Check if open at requested time
                                if (request.getTime() != null && !request.getTime().isEmpty()) {
                                    boolean openAtTime = openingHoursService.isOpenAt(todayHours, request.getTime());
                                    result.setOpenAtRequestedTime(openAtTime);

                                    if (!openAtTime) {
                                        result.addWarning(String.format("Place is closed at %s", request.getTime()));
                                    }
                                }

                                // Generate recommendation
                                String recommendation = openingHoursService.generateRecommendation(
                                        todayHours,
                                        request.getTime());
                                result.setRecommendation(recommendation);

                                logger.info("Parsed opening hours: {} - {} (open now: {})",
                                        todayHours.getOpensAt(), todayHours.getClosesAt(), openNow);
                            } else {
                                result.setRecommendation(
                                        openNow ? "Place is currently open" : "Place is currently closed");
                            }
                        } else {
                            result.setRecommendation(openNow ? "Place is currently open (detailed hours not available)"
                                    : "Place is currently closed (detailed hours not available)");
                        }
                    } else {
                        result.addWarning("Opening hours not available for this place");
                        result.setOpen(true); // Assume open if no data
                        result.setRecommendation("Opening hours unknown - verify before visiting");
                    }
                } else {
                    // No place ID - cannot check opening hours
                    result.addWarning("Cannot check opening hours without place ID");
                    result.setOpen(true); // Assume open
                    result.setRecommendation("Opening hours unknown - verify before visiting");
                }
            } catch (Exception e) {
                logger.warn("Failed to get place details: {}", e.getMessage());
                result.addWarning("Could not retrieve opening hours: " + e.getMessage());
                result.setOpen(true); // Assume open on error
                result.setRecommendation("Opening hours could not be verified - check before visiting");
            }

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("Failed to check opening hours", e);
            return ResponseEntity.status(500).body(
                    CheckOpeningHoursResult.error("Failed to check opening hours: " + e.getMessage()));
        }
    }

    @GetMapping("/schema/check-opening-hours")
    public ResponseEntity<Map<String, Object>> getCheckOpeningHoursSchema() {
        return ResponseEntity.ok(Map.of(
                "name", "check_opening_hours",
                "description", "Check if a place is open at a specific date/time and get recommendations",
                "parameters", Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "placeId", Map.of(
                                        "type", "string",
                                        "description", "Google Places ID"),
                                "placeName", Map.of(
                                        "type", "string",
                                        "description", "Place name (fallback if placeId not available)"),
                                "date", Map.of(
                                        "type", "string",
                                        "description", "Date to check (YYYY-MM-DD)"),
                                "time", Map.of(
                                        "type", "string",
                                        "description", "Time to check (HH:mm)")),
                        "required", new String[] {})));
    }

    // ========== P2-6: Get Itinerary Summary Tool ==========

    /**
     * P2-6: Get lightweight itinerary summary without loading full data
     * 
     * Returns only high-level information for quick access.
     */
    @PostMapping("/get-itinerary-summary")
    public ResponseEntity<ItinerarySummaryResult> getItinerarySummary(
            @RequestBody ItinerarySummaryRequest request,
            @RequestHeader(value = "X-Agent-Name", required = false) String agentName) {
        try {
            logToolUsage("get-itinerary-summary", agentName, request.getItineraryId());
            logger.info("Getting itinerary summary for: {}", request.getItineraryId());

            // Validate request
            if (request.getItineraryId() == null || request.getItineraryId().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        ItinerarySummaryResult.error("itineraryId is required"));
            }

            // Load itinerary
            Optional<NormalizedItinerary> itineraryOpt = itineraryJsonService.getItinerary(request.getItineraryId());
            if (itineraryOpt.isEmpty()) {
                return ResponseEntity.status(404).body(
                        ItinerarySummaryResult.error("Itinerary not found: " + request.getItineraryId()));
            }

            NormalizedItinerary itinerary = itineraryOpt.get();

            // Build summary
            ItinerarySummaryResult result = new ItinerarySummaryResult();
            result.setSuccess(true);
            result.setDestination(itinerary.getDestination());
            result.setStartDate(itinerary.getStartDate());
            result.setEndDate(itinerary.getEndDate());
            result.setDayCount(itinerary.getDays() != null ? itinerary.getDays().size() : 0);
            result.setPartySize(itinerary.getPartySize());
            result.setBudgetMax(itinerary.getBudgetMax());
            result.setCurrency(itinerary.getCurrency());
            result.setConstraints(itinerary.getConstraints());
            result.setState(itinerary.getStatus() != null ? itinerary.getStatus() : "UNKNOWN");

            // Count total nodes
            int totalNodes = 0;
            if (itinerary.getDays() != null) {
                for (NormalizedDay day : itinerary.getDays()) {
                    if (day.getNodes() != null) {
                        totalNodes += day.getNodes().size();
                    }
                }
            }
            result.setTotalNodes(totalNodes);

            logger.info("Itinerary summary: {} days, {} nodes, state: {}",
                    result.getDayCount(), result.getTotalNodes(), result.getState());

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("Failed to get itinerary summary", e);
            return ResponseEntity.status(500).body(
                    ItinerarySummaryResult.error("Failed to get itinerary summary: " + e.getMessage()));
        }
    }

    @GetMapping("/schema/get-itinerary-summary")
    public ResponseEntity<Map<String, Object>> getItinerarySummarySchema() {
        return ResponseEntity.ok(Map.of(
                "name", "get_itinerary_summary",
                "description", "Get lightweight summary of itinerary without loading full data",
                "parameters", Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "itineraryId", Map.of(
                                        "type", "string",
                                        "description", "ID of the itinerary")),
                        "required", new String[] { "itineraryId" })));
    }

    // ========== P2-7: Get Day Details Tool ==========

    /**
     * P2-7: Get detailed information for a specific day
     * 
     * Returns all nodes and timing information for targeted day access.
     */
    @PostMapping("/get-day-details")
    public ResponseEntity<GetDayDetailsResult> getDayDetails(
            @RequestBody GetDayDetailsRequest request,
            @RequestHeader(value = "X-Agent-Name", required = false) String agentName) {
        try {
            logToolUsage("get-day-details", agentName, request.getItineraryId());
            logger.info("Getting day details for itinerary: {}, day: {}",
                    request.getItineraryId(), request.getDayNumber());

            // Validate request
            if (request.getItineraryId() == null || request.getItineraryId().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        GetDayDetailsResult.error("itineraryId is required"));
            }

            if (request.getDayNumber() == null) {
                return ResponseEntity.badRequest().body(
                        GetDayDetailsResult.error("dayNumber is required"));
            }

            // Load itinerary
            Optional<NormalizedItinerary> itineraryOpt = itineraryJsonService.getItinerary(request.getItineraryId());
            if (itineraryOpt.isEmpty()) {
                return ResponseEntity.status(404).body(
                        GetDayDetailsResult.error("Itinerary not found: " + request.getItineraryId()));
            }

            NormalizedItinerary itinerary = itineraryOpt.get();

            // Find the day
            NormalizedDay day = itinerary.getDays().stream()
                    .filter(d -> d.getDayNumber().equals(request.getDayNumber()))
                    .findFirst()
                    .orElse(null);

            if (day == null) {
                return ResponseEntity.status(404).body(
                        GetDayDetailsResult.error("Day not found: " + request.getDayNumber()));
            }

            // Build result
            GetDayDetailsResult result = new GetDayDetailsResult();
            result.setSuccess(true);
            result.setDayNumber(day.getDayNumber());
            result.setDate(day.getDate());
            result.setNodes(day.getNodes());
            result.setCurrency(itinerary.getCurrency());

            // Calculate total cost for the day
            double totalCost = 0;
            Long startTimeMs = null;
            Long endTimeMs = null;

            if (day.getNodes() != null) {
                for (NormalizedNode node : day.getNodes()) {
                    // Sum costs
                    if (node.getCost() != null && node.getCost().getAmountPerPerson() != null) {
                        totalCost += node.getCost().getAmountPerPerson();
                    }

                    // Track start and end times
                    if (node.getTiming() != null) {
                        if (startTimeMs == null || (node.getTiming().getStartTime() != null &&
                                node.getTiming().getStartTime() < startTimeMs)) {
                            startTimeMs = node.getTiming().getStartTime();
                        }
                        if (endTimeMs == null || (node.getTiming().getEndTime() != null &&
                                node.getTiming().getEndTime() > endTimeMs)) {
                            endTimeMs = node.getTiming().getEndTime();
                        }
                    }
                }
            }

            result.setTotalCost(totalCost);
            // Convert timestamps to HH:mm format
            if (startTimeMs != null) {
                result.setStartTime(formatTime(startTimeMs));
            }
            if (endTimeMs != null) {
                result.setEndTime(formatTime(endTimeMs));
            }

            logger.info("Day {} details: {} nodes, {} {} total cost",
                    day.getDayNumber(),
                    day.getNodes() != null ? day.getNodes().size() : 0,
                    String.format("%.2f", totalCost),
                    itinerary.getCurrency());

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("Failed to get day details", e);
            return ResponseEntity.status(500).body(
                    GetDayDetailsResult.error("Failed to get day details: " + e.getMessage()));
        }
    }

    @GetMapping("/schema/get-day-details")
    public ResponseEntity<Map<String, Object>> getDayDetailsSchema() {
        return ResponseEntity.ok(Map.of(
                "name", "get_day_details",
                "description", "Get detailed information for a specific day including all nodes and timing",
                "parameters", Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "itineraryId", Map.of(
                                        "type", "string",
                                        "description", "ID of the itinerary"),
                                "dayNumber", Map.of(
                                        "type", "integer",
                                        "description", "Day number to get details for")),
                        "required", new String[] { "itineraryId", "dayNumber" })));
    }

    // ========== Helper Methods ==========

    /**
     * Estimate venue capacity from Google Places data
     */
    private Integer estimateCapacityFromPlaceData(List<String> types, int userRatingsTotal) {
        if (types == null || types.isEmpty()) {
            return null;
        }

        int baseCapacity = 50;

        for (String type : types) {
            String typeLower = type.toLowerCase();

            if (typeLower.contains("cafe") || typeLower.contains("bar")) {
                baseCapacity = 30;
                break;
            }
            if (typeLower.contains("restaurant")) {
                baseCapacity = 80;
                break;
            }
            if (typeLower.contains("banquet") || typeLower.contains("convention") ||
                    typeLower.contains("stadium") || typeLower.contains("arena")) {
                baseCapacity = 500;
                break;
            }
            if (typeLower.contains("museum") || typeLower.contains("gallery") ||
                    typeLower.contains("tourist_attraction")) {
                baseCapacity = 200;
                break;
            }
            if (typeLower.contains("park")) {
                return null;
            }
        }

        if (userRatingsTotal > 5000) {
            baseCapacity = (int) (baseCapacity * 1.5);
        } else if (userRatingsTotal > 1000) {
            baseCapacity = (int) (baseCapacity * 1.2);
        } else if (userRatingsTotal < 100) {
            baseCapacity = (int) (baseCapacity * 0.7);
        }

        return baseCapacity;
    }

    /**
     * Estimate capacity from venue name (fallback)
     */
    private Integer estimateCapacityFromName(String name) {
        if (name.contains("cafe") || name.contains("café"))
            return 30;
        if (name.contains("restaurant"))
            return 80;
        if (name.contains("museum") || name.contains("gallery"))
            return 200;
        if (name.contains("tour"))
            return 30;
        if (name.contains("mall") || name.contains("market"))
            return null;
        if (name.contains("park") || name.contains("garden"))
            return null;
        return 50;
    }

    /**
     * Calculate haversine distance between two coordinates in kilometers.
     */
    private double calculateHaversineDistance(com.tripplanner.dto.Coordinates from,
            com.tripplanner.dto.Coordinates to) {
        if (from == null || to == null)
            return 0;

        double lat1 = Math.toRadians(from.getLat());
        double lat2 = Math.toRadians(to.getLat());
        double lon1 = Math.toRadians(from.getLng());
        double lon2 = Math.toRadians(to.getLng());

        double dLat = lat2 - lat1;
        double dLon = lon2 - lon1;

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(lat1) * Math.cos(lat2) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return 6371 * c; // Earth radius in km
    }

    /**
     * Format timestamp (milliseconds since epoch) to HH:mm format.
     */
    private String formatTime(Long timestampMs) {
        if (timestampMs == null)
            return null;

        java.time.Instant instant = java.time.Instant.ofEpochMilli(timestampMs);
        java.time.ZonedDateTime zdt = instant.atZone(java.time.ZoneId.systemDefault());
        return String.format("%02d:%02d", zdt.getHour(), zdt.getMinute());
    }

    // ========== P2-8: Find Node Tool ==========

    @PostMapping("/find-node")
    public ResponseEntity<FindNodeResult> findNode(
            @RequestBody FindNodeRequest request,
            @RequestHeader(value = "X-Agent-Name", required = false) String agentName) {
        try {
            logToolUsage("find-node", agentName, request.getItineraryId());
            logger.info("Finding node: {} in itinerary: {}", request.getNodeId(), request.getItineraryId());

            if (request.getItineraryId() == null || request.getItineraryId().isEmpty()) {
                return ResponseEntity.badRequest().body(FindNodeResult.error("itineraryId is required"));
            }

            if (request.getNodeId() == null || request.getNodeId().isEmpty()) {
                return ResponseEntity.badRequest().body(FindNodeResult.error("nodeId is required"));
            }

            Optional<NormalizedItinerary> itineraryOpt = itineraryJsonService.getItinerary(request.getItineraryId());
            if (itineraryOpt.isEmpty()) {
                return ResponseEntity.status(404).body(FindNodeResult.error("Itinerary not found"));
            }

            NormalizedItinerary itinerary = itineraryOpt.get();

            for (NormalizedDay day : itinerary.getDays()) {
                if (day.getNodes() != null) {
                    for (NormalizedNode node : day.getNodes()) {
                        if (request.getNodeId().equals(node.getId())) {
                            FindNodeResult result = new FindNodeResult();
                            result.setSuccess(true);
                            result.setNode(node);
                            result.setDayNumber(day.getDayNumber());
                            logger.info("Found node {} on day {}", node.getId(), day.getDayNumber());
                            return ResponseEntity.ok(result);
                        }
                    }
                }
            }

            return ResponseEntity.status(404).body(FindNodeResult.error("Node not found: " + request.getNodeId()));

        } catch (Exception e) {
            logger.error("Failed to find node", e);
            return ResponseEntity.status(500).body(FindNodeResult.error("Failed to find node: " + e.getMessage()));
        }
    }

    @GetMapping("/schema/find-node")
    public ResponseEntity<Map<String, Object>> getFindNodeSchema() {
        return ResponseEntity.ok(Map.of(
                "name", "find_node",
                "description", "Find a specific node by ID without loading entire itinerary",
                "parameters", Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "itineraryId", Map.of(
                                        "type", "string",
                                        "description", "ID of the itinerary"),
                                "nodeId", Map.of(
                                        "type", "string",
                                        "description", "ID of the node to find")),
                        "required", new String[] { "itineraryId", "nodeId" })));
    }

    // ========== P2-9: Get Nodes by Type Tool ==========

    @PostMapping("/get-nodes-by-type")
    public ResponseEntity<GetNodesByTypeResult> getNodesByType(
            @RequestBody GetNodesByTypeRequest request,
            @RequestHeader(value = "X-Agent-Name", required = false) String agentName) {
        try {
            logToolUsage("get-nodes-by-type", agentName, request.getItineraryId());
            logger.info("Getting nodes by type: {} for itinerary: {}", request.getNodeType(), request.getItineraryId());

            if (request.getItineraryId() == null || request.getItineraryId().isEmpty()) {
                return ResponseEntity.badRequest().body(GetNodesByTypeResult.error("itineraryId is required"));
            }

            if (request.getNodeType() == null || request.getNodeType().isEmpty()) {
                return ResponseEntity.badRequest().body(GetNodesByTypeResult.error("nodeType is required"));
            }

            Optional<NormalizedItinerary> itineraryOpt = itineraryJsonService.getItinerary(request.getItineraryId());
            if (itineraryOpt.isEmpty()) {
                return ResponseEntity.status(404).body(GetNodesByTypeResult.error("Itinerary not found"));
            }

            NormalizedItinerary itinerary = itineraryOpt.get();
            GetNodesByTypeResult result = new GetNodesByTypeResult();
            result.setSuccess(true);

            List<NormalizedNode> matchingNodes = new ArrayList<>();

            for (NormalizedDay day : itinerary.getDays()) {
                if (request.getDayNumber() != null && !request.getDayNumber().equals(day.getDayNumber())) {
                    continue;
                }

                if (day.getNodes() != null) {
                    for (NormalizedNode node : day.getNodes()) {
                        if (request.getNodeType().equals(node.getType())) {
                            matchingNodes.add(node);
                        }
                    }
                }
            }

            result.setNodes(matchingNodes);
            result.setTotalCount(matchingNodes.size());

            logger.info("Found {} nodes of type {}", matchingNodes.size(), request.getNodeType());
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("Failed to get nodes by type", e);
            return ResponseEntity.status(500)
                    .body(GetNodesByTypeResult.error("Failed to get nodes: " + e.getMessage()));
        }
    }

    @GetMapping("/schema/get-nodes-by-type")
    public ResponseEntity<Map<String, Object>> getNodesByTypeSchema() {
        return ResponseEntity.ok(Map.of(
                "name", "get_nodes_by_type",
                "description", "Get all nodes of a specific type (attraction, meal, transport, accommodation)",
                "parameters", Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "itineraryId", Map.of(
                                        "type", "string",
                                        "description", "ID of the itinerary"),
                                "nodeType", Map.of(
                                        "type", "string",
                                        "description", "Type of nodes to retrieve",
                                        "enum", new String[] { "attraction", "meal", "transport", "accommodation" }),
                                "dayNumber", Map.of(
                                        "type", "integer",
                                        "description", "Optional: Filter by specific day")),
                        "required", new String[] { "itineraryId", "nodeType" })));
    }

    // ========== P2-10: Validate Completeness Tool ==========

    @PostMapping("/validate-completeness")
    public ResponseEntity<ValidateCompletenessResult> validateCompleteness(
            @RequestBody ValidateCompletenessRequest request,
            @RequestHeader(value = "X-Agent-Name", required = false) String agentName) {
        try {
            logToolUsage("validate-completeness", agentName, request.getItineraryId());
            logger.info("Validating completeness for itinerary: {}", request.getItineraryId());

            if (request.getItineraryId() == null || request.getItineraryId().isEmpty()) {
                return ResponseEntity.badRequest().body(ValidateCompletenessResult.error("itineraryId is required"));
            }

            Optional<NormalizedItinerary> itineraryOpt = itineraryJsonService.getItinerary(request.getItineraryId());
            if (itineraryOpt.isEmpty()) {
                return ResponseEntity.status(404).body(ValidateCompletenessResult.error("Itinerary not found"));
            }

            NormalizedItinerary itinerary = itineraryOpt.get();
            ValidateCompletenessResult result = new ValidateCompletenessResult();
            result.setSuccess(true);

            boolean isComplete = true;

            for (NormalizedDay day : itinerary.getDays()) {
                int mealCount = 0;
                int activityCount = 0;
                int transportCount = 0;

                if (day.getNodes() != null) {
                    for (NormalizedNode node : day.getNodes()) {
                        if ("meal".equals(node.getType()))
                            mealCount++;
                        else if ("attraction".equals(node.getType()))
                            activityCount++;
                        else if ("transport".equals(node.getType()))
                            transportCount++;
                    }
                }

                if (mealCount == 0) {
                    result.addMissingComponent("Day " + day.getDayNumber() + " has no meals");
                    result.addRecommendation("Add at least one meal for Day " + day.getDayNumber());
                    isComplete = false;
                }

                if (activityCount == 0) {
                    result.addMissingComponent("Day " + day.getDayNumber() + " has no activities");
                    result.addRecommendation("Add at least one activity for Day " + day.getDayNumber());
                    isComplete = false;
                }
            }

            if (itinerary.getDays().size() > 1) {
                for (int i = 0; i < itinerary.getDays().size() - 1; i++) {
                    NormalizedDay currentDay = itinerary.getDays().get(i);
                    boolean hasTransportToNextDay = false;

                    if (currentDay.getNodes() != null) {
                        for (NormalizedNode node : currentDay.getNodes()) {
                            if ("transport".equals(node.getType())) {
                                hasTransportToNextDay = true;
                                break;
                            }
                        }
                    }

                    if (!hasTransportToNextDay) {
                        result.addMissingComponent("No transport between Day " + currentDay.getDayNumber() + " and Day "
                                + (currentDay.getDayNumber() + 1));
                        result.addRecommendation("Add transport for Day " + currentDay.getDayNumber());
                        isComplete = false;
                    }
                }
            }

            result.setComplete(isComplete);

            if (isComplete) {
                result.addRecommendation("Itinerary is complete!");
            }

            logger.info("Completeness validation: complete={}, missing={}", isComplete,
                    result.getMissingComponents().size());
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("Failed to validate completeness", e);
            return ResponseEntity.status(500)
                    .body(ValidateCompletenessResult.error("Failed to validate: " + e.getMessage()));
        }
    }

    @GetMapping("/schema/validate-completeness")
    public ResponseEntity<Map<String, Object>> getValidateCompletenessSchema() {
        return ResponseEntity.ok(Map.of(
                "name", "validate_completeness",
                "description", "Validate that itinerary has all required components (meals, activities, transport)",
                "parameters", Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "itineraryId", Map.of(
                                        "type", "string",
                                        "description", "ID of the itinerary to validate")),
                        "required", new String[] { "itineraryId" })));
    }

    // ========== P2-11: Get Available Slots Tool ==========

    @PostMapping("/get-available-slots")
    public ResponseEntity<GetAvailableSlotsResult> getAvailableSlots(
            @RequestBody GetAvailableSlotsRequest request,
            @RequestHeader(value = "X-Agent-Name", required = false) String agentName) {
        try {
            logToolUsage("get-available-slots", agentName, request.getItineraryId());
            logger.info("Getting available slots for itinerary: {}, day: {}", request.getItineraryId(),
                    request.getDayNumber());

            if (request.getItineraryId() == null) {
                return ResponseEntity.badRequest().body(GetAvailableSlotsResult.error("itineraryId is required"));
            }

            if (request.getDayNumber() == null) {
                return ResponseEntity.badRequest().body(GetAvailableSlotsResult.error("dayNumber is required"));
            }

            Optional<NormalizedItinerary> itineraryOpt = itineraryJsonService.getItinerary(request.getItineraryId());
            if (itineraryOpt.isEmpty()) {
                return ResponseEntity.status(404).body(GetAvailableSlotsResult.error("Itinerary not found"));
            }

            NormalizedItinerary itinerary = itineraryOpt.get();
            NormalizedDay day = itinerary.getDays().stream()
                    .filter(d -> d.getDayNumber().equals(request.getDayNumber()))
                    .findFirst()
                    .orElse(null);

            if (day == null) {
                return ResponseEntity.status(404).body(GetAvailableSlotsResult.error("Day not found"));
            }

            GetAvailableSlotsResult result = new GetAvailableSlotsResult();
            result.setSuccess(true);

            int minDuration = request.getMinDurationMinutes() != null ? request.getMinDurationMinutes() : 60;

            List<NormalizedNode> nodesWithTiming = new ArrayList<>();
            if (day.getNodes() != null) {
                for (NormalizedNode node : day.getNodes()) {
                    if (node.getTiming() != null && node.getTiming().getStartTime() != null
                            && node.getTiming().getEndTime() != null) {
                        nodesWithTiming.add(node);
                    }
                }
            }

            nodesWithTiming.sort((a, b) -> Long.compare(a.getTiming().getStartTime(), b.getTiming().getStartTime()));

            for (int i = 0; i < nodesWithTiming.size() - 1; i++) {
                Long gapStart = nodesWithTiming.get(i).getTiming().getEndTime();
                Long gapEnd = nodesWithTiming.get(i + 1).getTiming().getStartTime();

                long gapMinutes = (gapEnd - gapStart) / (60 * 1000);

                if (gapMinutes >= minDuration) {
                    GetAvailableSlotsResult.TimeSlot slot = new GetAvailableSlotsResult.TimeSlot(
                            gapStart, gapEnd, (int) gapMinutes);
                    result.getAvailableSlots().add(slot);
                }
            }

            logger.info("Found {} available slots", result.getAvailableSlots().size());
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("Failed to get available slots", e);
            return ResponseEntity.status(500).body(GetAvailableSlotsResult.error("Failed: " + e.getMessage()));
        }
    }

    @GetMapping("/schema/get-available-slots")
    public ResponseEntity<Map<String, Object>> getAvailableSlotsSchema() {
        return ResponseEntity.ok(Map.of(
                "name", "get_available_slots",
                "description", "Find free time slots in a day's schedule for adding activities",
                "parameters", Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "itineraryId", Map.of(
                                        "type", "string",
                                        "description", "ID of the itinerary"),
                                "dayNumber", Map.of(
                                        "type", "integer",
                                        "description", "Day number to check"),
                                "minDurationMinutes", Map.of(
                                        "type", "integer",
                                        "description", "Minimum slot duration needed (default: 60)")),
                        "required", new String[] { "itineraryId", "dayNumber" })));
    }

    // ========== P2-12: Estimate Duration Tool ==========

    @PostMapping("/estimate-duration")
    public ResponseEntity<EstimateDurationResult> estimateDuration(
            @RequestBody EstimateDurationRequest request,
            @RequestHeader(value = "X-Agent-Name", required = false) String agentName) {
        try {
            logToolUsage("estimate-duration", agentName, null);
            logger.info("Estimating duration for activity: {}, type: {}", request.getActivityName(),
                    request.getActivityType());

            EstimateDurationResult result = new EstimateDurationResult();
            result.setSuccess(true);

            String type = request.getActivityType() != null ? request.getActivityType().toLowerCase() : "";
            int partySize = request.getPartySize() != null ? request.getPartySize() : 2;

            int baseMin, baseMax;
            String reasoning;

            if (type.contains("museum") || type.contains("gallery")) {
                baseMin = 90;
                baseMax = 180;
                reasoning = "Museums typically require 1.5-3 hours for a thorough visit";
            } else if (type.contains("restaurant") || type.contains("meal") || type.contains("dining")) {
                baseMin = 60;
                baseMax = 120;
                reasoning = "Meals typically take 1-2 hours including ordering and service";
            } else if (type.contains("park") || type.contains("garden")) {
                baseMin = 60;
                baseMax = 120;
                reasoning = "Parks and gardens are best enjoyed with 1-2 hours";
            } else if (type.contains("shopping") || type.contains("market")) {
                baseMin = 90;
                baseMax = 180;
                reasoning = "Shopping areas typically require 1.5-3 hours";
            } else if (type.contains("tour") || type.contains("guided")) {
                baseMin = 120;
                baseMax = 240;
                reasoning = "Guided tours typically last 2-4 hours";
            } else if (type.contains("temple") || type.contains("church") || type.contains("mosque")) {
                baseMin = 30;
                baseMax = 90;
                reasoning = "Religious sites typically require 30-90 minutes";
            } else {
                baseMin = 60;
                baseMax = 120;
                reasoning = "General activities typically take 1-2 hours";
            }

            if (partySize > 4) {
                baseMin += 15;
                baseMax += 30;
                reasoning += ". Larger groups need extra time.";
            }

            result.setMinDurationMinutes(baseMin);
            result.setMaxDurationMinutes(baseMax);
            result.setEstimatedDurationMinutes((baseMin + baseMax) / 2);
            result.setReasoning(reasoning);

            logger.info("Estimated duration: {} minutes", result.getEstimatedDurationMinutes());
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("Failed to estimate duration", e);
            return ResponseEntity.status(500).body(EstimateDurationResult.error("Failed: " + e.getMessage()));
        }
    }

    @GetMapping("/schema/estimate-duration")
    public ResponseEntity<Map<String, Object>> getEstimateDurationSchema() {
        return ResponseEntity.ok(Map.of(
                "name", "estimate_duration",
                "description", "Estimate realistic duration for an activity based on type and party size",
                "parameters", Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "activityType", Map.of(
                                        "type", "string",
                                        "description",
                                        "Type of activity (museum, restaurant, park, shopping, tour, etc.)"),
                                "activityName", Map.of(
                                        "type", "string",
                                        "description", "Name of the activity"),
                                "partySize", Map.of(
                                        "type", "integer",
                                        "description", "Number of people (larger groups need more time)")),
                        "required", new String[] {})));
    }

    // ========== P2-13: Update Node Tool ==========

    @PostMapping("/update-node")
    public ResponseEntity<UpdateNodeResult> updateNode(
            @RequestBody UpdateNodeRequest request,
            @RequestHeader(value = "X-Agent-Name", required = false) String agentName) {
        try {
            logToolUsage("update-node", agentName, request.getItineraryId());
            logger.info("Updating node: {} in itinerary: {}", request.getNodeId(), request.getItineraryId());

            if (request.getItineraryId() == null) {
                return ResponseEntity.badRequest().body(UpdateNodeResult.error("itineraryId is required"));
            }
            if (request.getNodeId() == null) {
                return ResponseEntity.badRequest().body(UpdateNodeResult.error("nodeId is required"));
            }

            Optional<NormalizedItinerary> itineraryOpt = itineraryJsonService.getItinerary(request.getItineraryId());
            if (itineraryOpt.isEmpty()) {
                return ResponseEntity.status(404).body(UpdateNodeResult.error("Itinerary not found"));
            }

            NormalizedItinerary itinerary = itineraryOpt.get();
            NormalizedNode targetNode = null;

            for (NormalizedDay day : itinerary.getDays()) {
                if (day.getNodes() != null) {
                    for (NormalizedNode node : day.getNodes()) {
                        if (request.getNodeId().equals(node.getId())) {
                            targetNode = node;
                            break;
                        }
                    }
                }
                if (targetNode != null)
                    break;
            }

            if (targetNode == null) {
                return ResponseEntity.status(404).body(UpdateNodeResult.error("Node not found"));
            }

            UpdateNodeResult result = new UpdateNodeResult();
            result.setSuccess(true);

            for (Map.Entry<String, Object> entry : request.getUpdates().entrySet()) {
                String field = entry.getKey();
                Object value = entry.getValue();

                try {
                    switch (field) {
                        case "title":
                            if (value instanceof String) {
                                targetNode.setTitle((String) value);
                                logger.info("Updated title to: {}", value);
                            }
                            break;
                        case "type":
                            if (value instanceof String) {
                                targetNode.setType((String) value);
                                logger.info("Updated type to: {}", value);
                            }
                            break;
                        case "cost":
                            if (value instanceof Map) {
                                @SuppressWarnings("unchecked")
                                Map<String, Object> costMap = (Map<String, Object>) value;
                                if (targetNode.getCost() == null) {
                                    targetNode.setCost(new com.tripplanner.dto.NodeCost());
                                }
                                if (costMap.containsKey("amountPerPerson")) {
                                    targetNode.getCost().setAmountPerPerson(
                                            ((Number) costMap.get("amountPerPerson")).doubleValue());
                                }
                                if (costMap.containsKey("currency")) {
                                    targetNode.getCost().setCurrency((String) costMap.get("currency"));
                                }
                                logger.info("Updated cost");
                            }
                            break;
                        case "timing":
                            if (value instanceof Map) {
                                @SuppressWarnings("unchecked")
                                Map<String, Object> timingMap = (Map<String, Object>) value;
                                if (targetNode.getTiming() == null) {
                                    targetNode.setTiming(new com.tripplanner.dto.NodeTiming());
                                }
                                if (timingMap.containsKey("startTime")) {
                                    targetNode.getTiming().setStartTime(
                                            ((Number) timingMap.get("startTime")).longValue());
                                }
                                if (timingMap.containsKey("endTime")) {
                                    targetNode.getTiming().setEndTime(
                                            ((Number) timingMap.get("endTime")).longValue());
                                }
                                if (timingMap.containsKey("durationMin")) {
                                    targetNode.getTiming().setDurationMin(
                                            ((Number) timingMap.get("durationMin")).intValue());
                                }
                                logger.info("Updated timing");
                            }
                            break;
                        case "location":
                            if (value instanceof Map) {
                                @SuppressWarnings("unchecked")
                                Map<String, Object> locationMap = (Map<String, Object>) value;
                                if (targetNode.getLocation() == null) {
                                    targetNode.setLocation(new com.tripplanner.dto.NodeLocation());
                                }
                                if (locationMap.containsKey("name")) {
                                    targetNode.getLocation().setName((String) locationMap.get("name"));
                                }
                                if (locationMap.containsKey("address")) {
                                    targetNode.getLocation().setAddress((String) locationMap.get("address"));
                                }
                                if (locationMap.containsKey("coordinates")
                                        && locationMap.get("coordinates") instanceof Map) {
                                    @SuppressWarnings("unchecked")
                                    Map<String, Object> coordsMap = (Map<String, Object>) locationMap
                                            .get("coordinates");
                                    if (targetNode.getLocation().getCoordinates() == null) {
                                        targetNode.getLocation().setCoordinates(new com.tripplanner.dto.Coordinates());
                                    }
                                    if (coordsMap.containsKey("lat")) {
                                        targetNode.getLocation().getCoordinates().setLat(
                                                ((Number) coordsMap.get("lat")).doubleValue());
                                    }
                                    if (coordsMap.containsKey("lng")) {
                                        targetNode.getLocation().getCoordinates().setLng(
                                                ((Number) coordsMap.get("lng")).doubleValue());
                                    }
                                }
                                logger.info("Updated location");
                            }
                            break;
                        default:
                            result.addWarning("Field '" + field + "' is not supported for updates");
                    }
                } catch (Exception e) {
                    result.addWarning("Failed to update field '" + field + "': " + e.getMessage());
                    logger.error("Error updating field {}", field, e);
                }
            }

            itineraryJsonService.updateItineraryWithLock(itinerary);
            result.setUpdatedNode(targetNode);

            logger.info("Node updated successfully with {} fields", request.getUpdates().size());
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("Failed to update node", e);
            return ResponseEntity.status(500).body(UpdateNodeResult.error("Failed: " + e.getMessage()));
        }
    }

    @GetMapping("/schema/update-node")
    public ResponseEntity<Map<String, Object>> getUpdateNodeSchema() {
        return ResponseEntity.ok(Map.of(
                "name", "update_node",
                "description", "Update specific fields of a node without reloading entire itinerary",
                "parameters", Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "itineraryId", Map.of(
                                        "type", "string",
                                        "description", "ID of the itinerary"),
                                "nodeId", Map.of(
                                        "type", "string",
                                        "description", "ID of the node to update"),
                                "updates", Map.of(
                                        "type", "object",
                                        "description", "Map of field names to new values")),
                        "required", new String[] { "itineraryId", "nodeId", "updates" })));
    }

    // ========== P2-14: Check Capacity Tool ==========

    @PostMapping("/check-capacity")
    public ResponseEntity<CheckCapacityResult> checkCapacity(
            @RequestBody CheckCapacityRequest request,
            @RequestHeader(value = "X-Agent-Name", required = false) String agentName) {
        try {
            logToolUsage("check-capacity", agentName, null);
            logger.info("Checking capacity for place: {}, party size: {}", request.getPlaceName(),
                    request.getPartySize());

            CheckCapacityResult result = new CheckCapacityResult();
            result.setSuccess(true);

            int partySize = request.getPartySize() != null ? request.getPartySize() : 2;

            // Try to get place details for better capacity estimation
            Integer estimatedCapacity = null;
            List<String> placeTypes = new ArrayList<>();

            if (request.getPlaceId() != null && !request.getPlaceId().isEmpty()) {
                try {
                    com.tripplanner.dto.PlaceDetails placeDetails = placesService.getPlaceDetails(request.getPlaceId());
                    if (placeDetails != null) {
                        placeTypes = placeDetails.getTypes();
                        int userRatingsTotal = placeDetails.getUserRatingsTotal() != null
                                ? placeDetails.getUserRatingsTotal()
                                : 0;

                        // Estimate capacity based on place type and popularity
                        estimatedCapacity = estimateCapacityFromPlaceData(placeTypes, userRatingsTotal);

                        logger.info("Estimated capacity: {} for place with {} ratings",
                                estimatedCapacity, userRatingsTotal);
                    }
                } catch (Exception e) {
                    logger.warn("Could not get place details for capacity check: {}", e.getMessage());
                }
            }

            // Fallback to name-based heuristics if no place ID
            if (estimatedCapacity == null && request.getPlaceName() != null) {
                String name = request.getPlaceName().toLowerCase();
                estimatedCapacity = estimateCapacityFromName(name);
            }

            // Set result
            result.setMaxCapacity(estimatedCapacity);

            if (estimatedCapacity != null) {
                // Consider 70% of capacity as comfortable limit
                int comfortableLimit = (int) (estimatedCapacity * 0.7);
                result.setCanAccommodate(partySize <= comfortableLimit);

                if (partySize > estimatedCapacity) {
                    result.setRecommendation(String.format(
                            "Party size (%d) exceeds estimated capacity (%d). Contact venue for special arrangements.",
                            partySize, estimatedCapacity));
                } else if (partySize > comfortableLimit) {
                    result.setRecommendation(String.format(
                            "Large party (%d people). Strongly recommend calling ahead for reservation.",
                            partySize));
                } else if (partySize > 10) {
                    result.setRecommendation("Recommend calling ahead for groups over 10 people.");
                } else {
                    result.setRecommendation("Should accommodate your party comfortably.");
                }
            } else {
                result.setCanAccommodate(true);
                result.setRecommendation(
                        "Capacity unknown - recommend verifying with venue, especially for large groups.");
            }

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("Failed to check capacity", e);
            return ResponseEntity.status(500).body(CheckCapacityResult.error("Failed: " + e.getMessage()));
        }
    }

    @GetMapping("/schema/check-capacity")
    public ResponseEntity<Map<String, Object>> getCheckCapacitySchema() {
        return ResponseEntity.ok(Map.of(
                "name", "check_capacity",
                "description", "Check if a venue can accommodate the party size",
                "parameters", Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "placeId", Map.of(
                                        "type", "string",
                                        "description", "Google Places ID"),
                                "placeName", Map.of(
                                        "type", "string",
                                        "description", "Name of the place"),
                                "partySize", Map.of(
                                        "type", "integer",
                                        "description", "Number of people in party")),
                        "required", new String[] { "partySize" })));
    }

    // ========== P2-15: Get Weather Tool ==========

    @PostMapping("/get-weather")
    public ResponseEntity<GetWeatherResult> getWeather(
            @RequestBody GetWeatherRequest request,
            @RequestHeader(value = "X-Agent-Name", required = false) String agentName) {
        try {
            logToolUsage("get-weather", agentName, request.getItineraryId());
            logger.info("Getting weather for location: {}, date: {}", request.getLocation(), request.getDate());

            String location = request.getLocation();
            if (location == null || location.isEmpty()) {
                if (request.getCoordinates() != null) {
                    location = request.getCoordinates().getLat() + "," + request.getCoordinates().getLng();
                } else {
                    return ResponseEntity.badRequest().body(GetWeatherResult.error("location or coordinates required"));
                }
            }

            // Get weather with caching if itineraryId provided
            WeatherService.WeatherData weatherData = weatherService.getWeather(
                    request.getItineraryId(), 
                    location, 
                    request.getDate());

            GetWeatherResult result = new GetWeatherResult();
            result.setSuccess(true);
            result.setCondition(weatherData.getCondition());
            result.setTemperatureCelsius(weatherData.getTemperatureCelsius());
            result.setPrecipitationChance(weatherData.getPrecipitationChance());

            String condition = weatherData.getCondition().toLowerCase();
            if (condition.contains("rain") || condition.contains("storm")) {
                result.setRecommendation("Rainy weather - plan indoor activities or bring umbrella");
            } else if (condition.contains("cloud")) {
                result.setRecommendation("Cloudy weather - good for outdoor activities, no harsh sun");
            } else if (condition.contains("clear") || condition.contains("sun")) {
                result.setRecommendation("Clear weather - perfect for outdoor activities");
            } else {
                result.setRecommendation("Check weather conditions before outdoor activities");
            }

            logger.info("Weather: {} at {}°C", weatherData.getCondition(), weatherData.getTemperatureCelsius());
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("Failed to get weather", e);
            return ResponseEntity.status(500).body(GetWeatherResult.error("Failed: " + e.getMessage()));
        }
    }

    @GetMapping("/schema/get-weather")
    public ResponseEntity<Map<String, Object>> getWeatherSchema() {
        return ResponseEntity.ok(Map.of(
                "name", "get_weather",
                "description",
                "Get weather forecast for activity planning using OpenWeather API with temperature, conditions, and precipitation data",
                "parameters", Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "location", Map.of(
                                        "type", "string",
                                        "description", "Location name"),
                                "coordinates", Map.of(
                                        "type", "object",
                                        "description", "Coordinates {lat, lng}"),
                                "date", Map.of(
                                        "type", "string",
                                        "description", "Date for forecast (YYYY-MM-DD)")),
                        "required", new String[] {})));
    }

    // ========== NEW: Suggest Best Time Tool (Weather-Based Activity Scheduling)
    // ==========

    /**
     * Suggest optimal times for activities based on weather, temperature, and
     * activity type.
     * This is the intelligent weather-based scheduling feature that considers:
     * - Temperature comfort zones
     * - Weather conditions (rain, clear, cloudy)
     * - Activity type (outdoor, indoor, scenic, water, night)
     * - Golden hours for photography
     * - Peak heat hours to avoid
     */
    @PostMapping("/suggest-best-time")
    public ResponseEntity<SuggestBestTimeResult> suggestBestTime(
            @RequestBody SuggestBestTimeRequest request,
            @RequestHeader(value = "X-Agent-Name", required = false) String agentName) {
        try {
            logToolUsage("suggest-best-time", agentName, request.getItineraryId());
            logger.info("🔍 suggestBestTime called with itineraryId: {}", request.getItineraryId());
            logger.info("Suggesting best time for activity: {} (type: {}) at {} on {}",
                    request.getActivityName(), request.getActivityType(),
                    request.getLocation(), request.getDate());

            // Validate request
            if (request.getActivityName() == null || request.getActivityName().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        SuggestBestTimeResult.error("activityName is required"));
            }

            if (request.getLocation() == null || request.getLocation().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        SuggestBestTimeResult.error("location is required"));
            }

            if (request.getDate() == null || request.getDate().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        SuggestBestTimeResult.error("date is required (YYYY-MM-DD format)"));
            }

            // Parse activity type
            ActivitySuitabilityService.ActivityType activityType = ActivitySuitabilityService.ActivityType
                    .fromString(request.getActivityType());

            // If type not provided or invalid, try to classify from name
            if (activityType == ActivitySuitabilityService.ActivityType.FLEXIBLE &&
                    (request.getActivityType() == null || request.getActivityType().isEmpty())) {
                activityType = activitySuitabilityService.classifyActivity(
                        request.getActivityName(),
                        "");
                logger.info("Auto-classified activity as: {}", activityType);
            }

            // Get duration (default 120 minutes if not provided)
            int duration = request.getDuration() != null ? request.getDuration() : 120;

            // Get best times
            java.time.LocalDate date = java.time.LocalDate.parse(request.getDate());
            List<SuggestBestTimeResult.TimeSlot> allSlots = activitySuitabilityService.suggestBestTimes(
                    request.getActivityName(),
                    activityType,
                    request.getLocation(),
                    date,
                    duration,
                    request.getItineraryId());

            // Build result
            SuggestBestTimeResult result = new SuggestBestTimeResult();
            result.setSuccess(true);

            // Top 3 recommended slots
            int recommendedCount = Math.min(3, allSlots.size());
            result.setRecommendedTimeSlots(allSlots.subList(0, recommendedCount));

            // Bottom 2 slots to avoid (if score < 40)
            List<SuggestBestTimeResult.TimeSlot> avoidSlots = new ArrayList<>();
            for (int i = allSlots.size() - 1; i >= 0 && avoidSlots.size() < 2; i--) {
                if (allSlots.get(i).getSuitabilityScore() < 40) {
                    avoidSlots.add(allSlots.get(i));
                }
            }
            result.setAvoidTimeSlots(avoidSlots);

            // Add enhanced weather context with temperature ranges and seasonal notes
            WeatherService.WeatherData weather = weatherService.getWeather(
                    request.getItineraryId(),
                    request.getLocation(),
                    request.getDate());

            SuggestBestTimeResult.WeatherContext weatherContext = new SuggestBestTimeResult.WeatherContext(
                    weather.getTemperatureCelsius(),
                    weather.getCondition(),
                    "06:15", // Approximate sunrise
                    "18:30" // Approximate sunset
            );
            weatherContext.setTemperatureMin(weather.getTemperatureMin());
            weatherContext.setTemperatureMax(weather.getTemperatureMax());
            weatherContext.setHumidity(weather.getHumidity());
            weatherContext.setSeasonalNotes(weather.getSeasonalNotes());
            result.setWeatherContext(weatherContext);

            // Add balanced alternative activity suggestions based on weather
            // Uses new method that maintains variety even during adverse weather
            List<String> alternatives = activitySuitabilityService.getAlternativeActivities(
                    activityType,
                    weather);
            result.setAlternativeActivities(alternatives);

            // Analyze for extreme conditions and add warnings/gear requirements
            ActivitySuitabilityService.ExtremeConditions extremeConditions = activitySuitabilityService
                    .analyzeExtremeConditions(
                            weather,
                            activityType,
                            request.getActivityName(),
                            duration);

            if (extremeConditions.isExtreme()) {
                SuggestBestTimeResult.ExtremeWeatherWarning warning = new SuggestBestTimeResult.ExtremeWeatherWarning();
                warning.setExtreme(true);
                warning.setSeverity(extremeConditions.getSeverity());
                warning.setWarnings(extremeConditions.getWarnings());
                warning.setGearRequirements(extremeConditions.getGearRequirements());
                result.setExtremeWeatherWarning(warning);

                logger.warn("EXTREME CONDITIONS detected for {}: {} - {}",
                        request.getActivityName(),
                        extremeConditions.getSeverity(),
                        extremeConditions.getWarnings());
            }

            logger.info("Suggested {} time slots for {}", recommendedCount, request.getActivityName());

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("Failed to suggest best time", e);
            return ResponseEntity.status(500).body(
                    SuggestBestTimeResult.error("Failed to suggest best time: " + e.getMessage()));
        }
    }

    @GetMapping("/schema/suggest-best-time")
    public ResponseEntity<Map<String, Object>> getSuggestBestTimeSchema() {
        return ResponseEntity.ok(Map.of(
                "name", "suggest_best_time",
                "description",
                "Suggest optimal times for activities based on weather, temperature, and activity type. " +
                        "Considers golden hours for photography, peak heat hours, rain conditions, and activity-specific requirements.",
                "parameters", Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "activityName", Map.of(
                                        "type", "string",
                                        "description",
                                        "Name of the activity (e.g., 'Pondicherry White Town', 'Beach visit')"),
                                "activityType", Map.of(
                                        "type", "string",
                                        "description",
                                        "Type of activity: outdoor_park, outdoor_monument, scenic_spot, beach_water, " +
                                                "indoor_museum, indoor_mall, indoor_temple, night_activity, flexible. "
                                                +
                                                "If not provided, will auto-classify from activityName.",
                                        "enum", new String[] {
                                                "outdoor_park", "outdoor_monument", "scenic_spot", "beach_water",
                                                "indoor_museum", "indoor_mall", "indoor_temple", "night_activity",
                                                "flexible"
                                        }),
                                "location", Map.of(
                                        "type", "string",
                                        "description",
                                        "Location name for weather forecast (e.g., 'Pondicherry', 'Paris')"),
                                "date", Map.of(
                                        "type", "string",
                                        "description", "Date for the activity (YYYY-MM-DD format)"),
                                "duration", Map.of(
                                        "type", "integer",
                                        "description", "Expected duration in minutes (default: 120)"),
                                "itineraryId", Map.of(
                                        "type", "string",
                                        "description", "ID of the itinerary (required for caching)")),
                        "required", new String[] { "activityName", "location", "date" })));
    }

    // ========== P2-16: Get Similar Activities Tool ==========

    /**
     * P2-16: Get Similar Activities
     * 
     * Finds similar activities/attractions near a given location using Google Places API.
     * Useful for providing alternatives or expanding itinerary options.
     */
    @PostMapping("/get-similar-activities")
    public ResponseEntity<GetSimilarActivitiesResult> getSimilarActivities(
            @RequestBody GetSimilarActivitiesRequest request,
            @RequestHeader(value = "X-Agent-Name", required = false) String agentName) {
        try {
            logToolUsage("get-similar-activities", agentName, null);
            logger.info("Finding similar activities to: {} near {}", 
                    request.getActivityName(), request.getLocation());

            if (request.getActivityName() == null || request.getActivityName().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(GetSimilarActivitiesResult.error("activityName is required"));
            }

            GetSimilarActivitiesResult result = new GetSimilarActivitiesResult();
            result.setSuccess(true);

            // Determine activity type for search
            String searchType = determineActivitySearchType(request.getActivityName(), request.getActivityType());
            logger.info("Determined search type: {}", searchType);

            // Use Google Places text search to find similar activities
            int maxResults = request.getMaxResults() != null ? request.getMaxResults() : 10;
            
            try {
                // Build search query
                String searchQuery = request.getActivityType() != null ? 
                        request.getActivityType() + " near " + request.getLocation() :
                        request.getActivityName() + " near " + request.getLocation();
                
                logger.info("Searching for: {}", searchQuery);
                
                // Use existing searchPlace method
                com.tripplanner.dto.PlaceSearchResult searchResult = 
                        placesService.searchPlace(searchQuery, request.getLocation());

                if (searchResult != null && searchResult.getPlaceId() != null) {
                    GetSimilarActivitiesResult.SimilarActivity activity = 
                            new GetSimilarActivitiesResult.SimilarActivity();
                    
                    activity.setPlaceId(searchResult.getPlaceId());
                    activity.setName(searchResult.getName());
                    activity.setType(searchType);
                    activity.setRating(searchResult.getRating());
                    activity.setDistanceKm(0.0); // Distance not available from text search
                    
                    // Generate similarity reason
                    activity.setSimilarityReason(generateSimilarityReason(
                            request.getActivityName(), searchResult.getName(), searchType));
                    
                    result.getActivities().add(activity);
                }

                logger.info("Found {} similar activities", result.getActivities().size());

                if (result.getActivities().isEmpty()) {
                    result.addWarning("No similar activities found in the area. Try broadening your search.");
                }

            } catch (Exception e) {
                logger.error("Failed to search for similar activities", e);
                result.addWarning("Google Places API search failed: " + e.getMessage());
            }

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("Failed to get similar activities", e);
            return ResponseEntity.status(500)
                    .body(GetSimilarActivitiesResult.error(
                            "Failed to get similar activities: " + e.getMessage()));
        }
    }

    @GetMapping("/schema/get-similar-activities")
    public ResponseEntity<Map<String, Object>> getSimilarActivitiesSchema() {
        return ResponseEntity.ok(Map.of(
                "name", "get_similar_activities",
                "description",
                "Find similar activities or attractions near a location using Google Places API. " +
                        "Useful for providing alternatives or expanding itinerary options.",
                "parameters", Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "activityName", Map.of(
                                        "type", "string",
                                        "description",
                                        "Name of the activity to find similar options for (e.g., 'Eiffel Tower', 'Beach')"),
                                "activityType", Map.of(
                                        "type", "string",
                                        "description",
                                        "Type of activity (optional): museum, park, restaurant, beach, monument, etc."),
                                "location", Map.of(
                                        "type", "string",
                                        "description",
                                        "Location name to search near (e.g., 'Paris', 'Goa')"),
                                "coordinates", Map.of(
                                        "type", "object",
                                        "description",
                                        "Coordinates to search near (alternative to location)",
                                        "properties", Map.of(
                                                "lat", Map.of("type", "number"),
                                                "lng", Map.of("type", "number"))),
                                "maxResults", Map.of(
                                        "type", "integer",
                                        "description", "Maximum number of results to return (default: 10)")),
                        "required", new String[] { "activityName" })));
    }

    /**
     * Determine the Google Places type to search for based on activity name and type
     */
    private String determineActivitySearchType(String activityName, String activityType) {
        if (activityType != null && !activityType.isEmpty()) {
            return activityType;
        }

        String lowerName = activityName.toLowerCase();

        // Museums and cultural sites
        if (lowerName.contains("museum") || lowerName.contains("gallery") || 
            lowerName.contains("exhibition")) {
            return "museum";
        }

        // Parks and gardens
        if (lowerName.contains("park") || lowerName.contains("garden") || 
            lowerName.contains("botanical")) {
            return "park";
        }

        // Beaches and water activities
        if (lowerName.contains("beach") || lowerName.contains("shore") || 
            lowerName.contains("coast")) {
            return "beach";
        }

        // Restaurants and dining
        if (lowerName.contains("restaurant") || lowerName.contains("cafe") || 
            lowerName.contains("dining")) {
            return "restaurant";
        }

        // Shopping
        if (lowerName.contains("mall") || lowerName.contains("market") || 
            lowerName.contains("shopping")) {
            return "shopping_mall";
        }

        // Religious sites
        if (lowerName.contains("temple") || lowerName.contains("church") || 
            lowerName.contains("mosque") || lowerName.contains("shrine")) {
            return "place_of_worship";
        }

        // Monuments and landmarks
        if (lowerName.contains("tower") || lowerName.contains("monument") || 
            lowerName.contains("fort") || lowerName.contains("palace") || 
            lowerName.contains("castle")) {
            return "tourist_attraction";
        }

        // Default to tourist attraction
        return "tourist_attraction";
    }

    /**
     * Generate a human-readable reason for why this activity is similar
     */
    private String generateSimilarityReason(String originalActivity, String similarActivity, String type) {
        String typeDescription = switch (type) {
            case "museum" -> "cultural attraction";
            case "park" -> "outdoor space";
            case "beach" -> "waterfront location";
            case "restaurant" -> "dining venue";
            case "shopping_mall" -> "shopping destination";
            case "place_of_worship" -> "religious site";
            case "tourist_attraction" -> "landmark";
            default -> "attraction";
        };

        return String.format("Similar %s in the same area", typeDescription);
    }

    // ========== NEW VALIDATION TOOLS ==========

    /**
     * Check Completeness Tool - Validates itinerary has all required nodes
     */
    @PostMapping("/check-completeness")
    public ResponseEntity<CompletenessCheckResult> checkCompleteness(
            @RequestBody CompletenessCheckRequest request,
            @RequestHeader(value = "X-Agent-Name", required = false) String agentName) {
        try {
            logToolUsage("check-completeness", agentName, request.getItineraryId());
            
            Optional<NormalizedItinerary> itineraryOpt = itineraryJsonService.getItinerary(request.getItineraryId());
            if (itineraryOpt.isEmpty()) {
                return ResponseEntity.status(404).body(
                    CompletenessCheckResult.error("Itinerary not found"));
            }
            
            NormalizedItinerary itinerary = itineraryOpt.get();
            CompletenessCheckResult result = new CompletenessCheckResult();
            
            for (NormalizedDay day : itinerary.getDays()) {
                if (day.getNodes() == null) continue;
                
                // Check meals
                if (Boolean.TRUE.equals(request.getCheckMeals())) {
                    long mealCount = day.getNodes().stream()
                        .filter(n -> "meal".equals(n.getType())).count();
                    if (mealCount < 2) {
                        result.addMissingItem(new CompletenessCheckResult.CompletenessIssue(
                            "MISSING_MEALS", day.getDayNumber(),
                            String.format("Day %d has only %d meals", day.getDayNumber(), mealCount),
                            "Add breakfast, lunch, or dinner"));
                    }
                }
                
                // Check activities
                if (Boolean.TRUE.equals(request.getCheckActivities())) {
                    long activityCount = day.getNodes().stream()
                        .filter(n -> "attraction".equals(n.getType())).count();
                    if (activityCount == 0) {
                        result.addMissingItem(new CompletenessCheckResult.CompletenessIssue(
                            "MISSING_ACTIVITIES", day.getDayNumber(),
                            String.format("Day %d has no activities", day.getDayNumber()),
                            "Add at least one activity"));
                    }
                }
            }
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Failed to check completeness", e);
            return ResponseEntity.status(500).body(
                CompletenessCheckResult.error(e.getMessage()));
        }
    }

    /**
     * Check Budget Health Tool - Analyzes budget status
     */
    @PostMapping("/check-budget-health")
    public ResponseEntity<BudgetHealthResult> checkBudgetHealth(
            @RequestBody BudgetHealthRequest request,
            @RequestHeader(value = "X-Agent-Name", required = false) String agentName) {
        try {
            logToolUsage("check-budget-health", agentName, request.getItineraryId());
            
            Optional<NormalizedItinerary> itineraryOpt = itineraryJsonService.getItinerary(request.getItineraryId());
            if (itineraryOpt.isEmpty()) {
                return ResponseEntity.status(404).body(
                    BudgetHealthResult.error("Itinerary not found"));
            }
            
            NormalizedItinerary itinerary = itineraryOpt.get();
            BudgetHealthResult result = new BudgetHealthResult();
            
            if (itinerary.getBudgetMax() == null) {
                result.setStatus("NO_BUDGET");
                return ResponseEntity.ok(result);
            }
            
            int partySize = itinerary.getPartySize() != null ? itinerary.getPartySize() : 1;
            BudgetSummary budget = budgetTracker.calculateBudget(itinerary, null, null, partySize);
            
            double totalCost = budget.getTotalCostPerPerson();
            double budgetMax = itinerary.getBudgetMax();
            int tripDays = itinerary.getDays() != null ? itinerary.getDays().size() : 1;
            double totalBudgetMax = budgetMax * tripDays;
            
            result.setTotalCost(totalCost);
            result.setBudgetMax(totalBudgetMax);
            result.setBudgetRemaining(totalBudgetMax - totalCost);
            result.setUsedPercentage((totalCost / totalBudgetMax) * 100);
            
            // Determine status
            if (totalCost > totalBudgetMax) {
                result.setStatus("OVER");
                result.addWarning("Budget exceeded");
            } else if (result.getUsedPercentage() >= 95) {
                result.setStatus("NEAR");
            } else if (result.getUsedPercentage() >= 70) {
                result.setStatus("MODERATE");
            } else {
                result.setStatus("UNDER");
            }
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Failed to check budget health", e);
            return ResponseEntity.status(500).body(
                BudgetHealthResult.error(e.getMessage()));
        }
    }

    /**
     * Check Timing Feasibility Tool - Validates schedule timing
     */
    @PostMapping("/check-timing-feasibility")
    public ResponseEntity<TimingFeasibilityResult> checkTimingFeasibility(
            @RequestBody TimingFeasibilityRequest request,
            @RequestHeader(value = "X-Agent-Name", required = false) String agentName) {
        try {
            logToolUsage("check-timing-feasibility", agentName, request.getItineraryId());
            
            Optional<NormalizedItinerary> itineraryOpt = itineraryJsonService.getItinerary(request.getItineraryId());
            if (itineraryOpt.isEmpty()) {
                return ResponseEntity.status(404).body(
                    TimingFeasibilityResult.error("Itinerary not found"));
            }
            
            NormalizedItinerary itinerary = itineraryOpt.get();
            TimingFeasibilityResult result = new TimingFeasibilityResult();
            
            for (NormalizedDay day : itinerary.getDays()) {
                if (day.getNodes() == null) continue;
                
                for (NormalizedNode node : day.getNodes()) {
                    if (node.getTiming() == null) continue;
                    
                    // Check unrealistic durations
                    if (node.getTiming().getDurationMin() != null) {
                        int duration = node.getTiming().getDurationMin();
                        
                        if ("attraction".equals(node.getType()) && duration > 480) {
                            result.addIssue(new TimingFeasibilityResult.TimingIssue(
                                "UNREALISTIC_DURATION", node.getId(), day.getDayNumber(),
                                String.format("Activity duration %d min seems too long", duration),
                                "Consider splitting into multiple activities"));
                        }
                        
                        if ("meal".equals(node.getType()) && duration > 180) {
                            result.addIssue(new TimingFeasibilityResult.TimingIssue(
                                "UNREALISTIC_DURATION", node.getId(), day.getDayNumber(),
                                String.format("Meal duration %d min seems too long", duration),
                                "Reduce meal duration"));
                        }
                    }
                }
            }
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Failed to check timing feasibility", e);
            return ResponseEntity.status(500).body(
                TimingFeasibilityResult.error(e.getMessage()));
        }
    }

    /**
     * Check Dietary Compliance Tool - Validates dietary restrictions
     */
    @PostMapping("/check-dietary-compliance")
    public ResponseEntity<DietaryComplianceResult> checkDietaryCompliance(
            @RequestBody DietaryComplianceRequest request,
            @RequestHeader(value = "X-Agent-Name", required = false) String agentName) {
        try {
            logToolUsage("check-dietary-compliance", agentName, request.getItineraryId());
            
            Optional<NormalizedItinerary> itineraryOpt = itineraryJsonService.getItinerary(request.getItineraryId());
            if (itineraryOpt.isEmpty()) {
                return ResponseEntity.status(404).body(
                    DietaryComplianceResult.error("Itinerary not found"));
            }
            
            NormalizedItinerary itinerary = itineraryOpt.get();
            DietaryComplianceResult result = new DietaryComplianceResult();
            
            // Get restrictions from request or itinerary
            List<String> restrictions = request.getRestrictions();
            if (restrictions == null || restrictions.isEmpty()) {
                restrictions = itinerary.getConstraints();
            }
            
            if (restrictions == null || restrictions.isEmpty()) {
                result.addWarning("No dietary restrictions to check");
                return ResponseEntity.ok(result);
            }
            
            // Check each meal node
            for (NormalizedDay day : itinerary.getDays()) {
                if (day.getNodes() == null) continue;
                
                for (NormalizedNode node : day.getNodes()) {
                    if (!"meal".equals(node.getType())) continue;
                    
                    // Check if meal title/description suggests non-compliance
                    String title = node.getTitle() != null ? node.getTitle().toLowerCase() : "";
                    
                    for (String restriction : restrictions) {
                        String lowerRestriction = restriction.toLowerCase();
                        
                        // Simple keyword-based checking (can be enhanced with DietaryVerificationService)
                        if (lowerRestriction.contains("vegetarian") && 
                            (title.contains("meat") || title.contains("chicken") || title.contains("beef"))) {
                            result.addViolation(new DietaryComplianceResult.DietaryViolation(
                                node.getId(), day.getDayNumber(), node.getTitle(),
                                restriction, "Meal may contain meat",
                                "Choose vegetarian option"));
                        }
                    }
                }
            }
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Failed to check dietary compliance", e);
            return ResponseEntity.status(500).body(
                DietaryComplianceResult.error(e.getMessage()));
        }
    }
    
    // ========== Schema Endpoints for Validation Tools ==========
    
    @GetMapping("/schema/check-completeness")
    public ResponseEntity<Map<String, Object>> getCheckCompletenessSchema() {
        return ResponseEntity.ok(Map.of(
            "name", "check_completeness",
            "description", "Check if itinerary has all required components (meals, activities, transport)",
            "parameters", Map.of(
                "type", "object",
                "properties", Map.of(
                    "itineraryId", Map.of(
                        "type", "string",
                        "description", "ID of the itinerary to check"),
                    "checkMeals", Map.of(
                        "type", "boolean",
                        "description", "Check for missing meals (default: true)"),
                    "checkActivities", Map.of(
                        "type", "boolean",
                        "description", "Check for missing activities (default: true)"),
                    "checkTransport", Map.of(
                        "type", "boolean",
                        "description", "Check for missing transport (default: true)"),
                    "checkAccommodation", Map.of(
                        "type", "boolean",
                        "description", "Check for missing accommodation (default: false)")),
                "required", new String[] { "itineraryId" })));
    }
    
    @GetMapping("/schema/check-budget-health")
    public ResponseEntity<Map<String, Object>> getCheckBudgetHealthSchema() {
        return ResponseEntity.ok(Map.of(
            "name", "check_budget_health",
            "description", "Analyze budget health and spending patterns with warnings and recommendations",
            "parameters", Map.of(
                "type", "object",
                "properties", Map.of(
                    "itineraryId", Map.of(
                        "type", "string",
                        "description", "ID of the itinerary to check"),
                    "includeProjections", Map.of(
                        "type", "boolean",
                        "description", "Include spending projections for remaining days (default: true)"),
                    "includeBreakdown", Map.of(
                        "type", "boolean",
                        "description", "Include category-wise breakdown (default: true)")),
                "required", new String[] { "itineraryId" })));
    }
    
    @GetMapping("/schema/check-timing-feasibility")
    public ResponseEntity<Map<String, Object>> getCheckTimingFeasibilitySchema() {
        return ResponseEntity.ok(Map.of(
            "name", "check_timing_feasibility",
            "description", "Validate timing and schedule feasibility (durations, buffers, opening hours)",
            "parameters", Map.of(
                "type", "object",
                "properties", Map.of(
                    "itineraryId", Map.of(
                        "type", "string",
                        "description", "ID of the itinerary to check"),
                    "includeTravel", Map.of(
                        "type", "boolean",
                        "description", "Include travel time validation (default: true)"),
                    "checkOpeningHours", Map.of(
                        "type", "boolean",
                        "description", "Check venue opening hours (default: false)")),
                "required", new String[] { "itineraryId" })));
    }
    
    @GetMapping("/schema/check-dietary-compliance")
    public ResponseEntity<Map<String, Object>> getCheckDietaryComplianceSchema() {
        return ResponseEntity.ok(Map.of(
            "name", "check_dietary_compliance",
            "description", "Verify meals comply with dietary restrictions and preferences",
            "parameters", Map.of(
                "type", "object",
                "properties", Map.of(
                    "itineraryId", Map.of(
                        "type", "string",
                        "description", "ID of the itinerary to check"),
                    "restrictions", Map.of(
                        "type", "array",
                        "items", Map.of("type", "string"),
                        "description", "List of dietary restrictions (vegetarian, vegan, halal, kosher, gluten-free)"),
                    "strictness", Map.of(
                        "type", "string",
                        "enum", new String[] { "STRICT", "MODERATE", "FLEXIBLE" },
                        "description", "Strictness level for checking (default: STRICT)")),
                "required", new String[] { "itineraryId" })));
    }
    
    // ========== Place Search Tool ==========
    
    /**
     * Search for places with photos, ratings, and details.
     * Returns multiple suggestions (up to 3) for user to choose from.
     * 
     * This tool uses smart caching to avoid repeated API calls:
     * - Cache key: query + location + coordinates
     * - Only validated results (within 30km) are cached
     * - TTL: 7 days
     */
    @PostMapping("/search-places")
    public ResponseEntity<PlaceSearchToolResult> searchPlaces(
            @RequestBody PlaceSearchToolRequest request,
            @RequestHeader(value = "X-Agent-Name", required = false) String agentName) {
        try {
            logToolUsage("search-places", agentName, request.getItineraryId());
            logger.info("Searching places: query='{}', location='{}', type='{}'",
                    request.getQuery(), request.getLocation(), request.getType());

            // Validate request
            if (request.getQuery() == null || request.getQuery().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        PlaceSearchToolResult.error("query is required"));
            }

            // Get location context from itinerary if not provided
            String location = request.getLocation();
            if ((location == null || location.isEmpty()) && request.getItineraryId() != null) {
                Optional<NormalizedItinerary> itinerary = itineraryJsonService.getItinerary(request.getItineraryId());
                if (itinerary.isPresent()) {
                    location = itinerary.get().getDestination();
                    logger.info("Using itinerary destination: {}", location);
                }
            }

            if (location == null || location.isEmpty()) {
                return ResponseEntity.badRequest().body(
                        PlaceSearchToolResult.error("location is required (provide location or itineraryId)"));
            }

            // Search places (always return 3 suggestions for chat UI)
            int maxResults = request.getMaxResults() != null ? request.getMaxResults() : 3;
            List<PlaceSuggestion> suggestions = placesService.searchPlaces(
                    request.getItineraryId(),
                    request.getQuery(),
                    location,
                    request.getType(),
                    maxResults);

            logger.info("✅ Found {} place suggestions", suggestions.size());
            return ResponseEntity.ok(PlaceSearchToolResult.success(suggestions));

        } catch (Exception e) {
            logger.error("Place search failed", e);
            return ResponseEntity.status(500).body(
                    PlaceSearchToolResult.error("Failed to search places: " + e.getMessage()));
        }
    }

    @GetMapping("/schema/search-places")
    public ResponseEntity<Map<String, Object>> getSearchPlacesSchema() {
        return ResponseEntity.ok(Map.of(
                "name", "search_places",
                "description", "Search for places with photos, ratings, and details. Returns up to 3 suggestions with rich data.",
                "parameters", Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "itineraryId", Map.of(
                                        "type", "string",
                                        "description", "ID of the itinerary (for location context and caching)"),
                                "query", Map.of(
                                        "type", "string",
                                        "description", "Place name or type to search for (e.g., 'museum', 'Louvre', 'restaurant')"),
                                "location", Map.of(
                                        "type", "string",
                                        "description", "Location to search in (e.g., 'Paris, France', 'Interlaken, Switzerland'). Optional if itineraryId provided."),
                                "type", Map.of(
                                        "type", "string",
                                        "description", "Optional place type filter (e.g., 'museum', 'restaurant', 'tourist_attraction', 'park')"),
                                "maxResults", Map.of(
                                        "type", "integer",
                                        "description", "Maximum number of results to return (default: 3, max: 10)")),
                        "required", new String[] { "query" })));
    }
}
