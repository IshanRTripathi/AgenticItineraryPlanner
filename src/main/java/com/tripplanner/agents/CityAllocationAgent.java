package com.tripplanner.agents;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripplanner.dto.*;
import com.tripplanner.service.*;
import com.tripplanner.service.agents.AgentEventBus;
import com.tripplanner.service.agents.AgentEventPublisher;
import com.tripplanner.service.ai.AiClient;
import com.tripplanner.service.ai.ResilientAiClient;
import com.tripplanner.service.ai.RetryStrategy;
import com.tripplanner.service.llm.LLMSchemaValidator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * CityAllocationAgent - Intelligently distributes days across cities for multi-city destinations.
 * 
 * This agent runs BEFORE SkeletonPlannerAgent to determine which cities to visit and how many days
 * to spend in each city. It considers:
 * - Destination type (city/state/country/region)
 * - User requirements (MUST respect if specified)
 * - City importance and recommended stay duration
 * - Travel time between cities
 * - User interests
 * 
 * Responsibilities:
 * - Determine destination type
 * - Identify top cities to visit
 * - Allocate days to each city
 * - Plan travel segments between cities
 * - Return structured CityAllocationPlan
 * 
 * Output: CityAllocationPlan stored in itinerary.agentData
 * Processing Time: 10-30 seconds
 */
@Component
@ConditionalOnBean(AiClient.class)
public class CityAllocationAgent extends BaseAgent {
    
    private final AiClient aiClient;
    private final ObjectMapper objectMapper;
    private final ItineraryJsonService itineraryJsonService;
    private final AgentEventPublisher agentEventPublisher;
    private final LLMSchemaValidator schemaValidator;
    
    public CityAllocationAgent(AgentEventBus eventBus, AiClient aiClient, ObjectMapper objectMapper,
                               ItineraryJsonService itineraryJsonService, AgentEventPublisher agentEventPublisher,
                               LLMSchemaValidator schemaValidator) {
        super(eventBus, AgentEvent.AgentKind.PLANNER);
        this.aiClient = aiClient;
        this.objectMapper = objectMapper;
        this.itineraryJsonService = itineraryJsonService;
        this.agentEventPublisher = agentEventPublisher;
        this.schemaValidator = schemaValidator;
    }
    
    @Override
    public AgentCapabilities getCapabilities() {
        AgentCapabilities capabilities = new AgentCapabilities();
        capabilities.addSupportedTask("city_allocation");
        capabilities.setPriority(0); // Highest priority - runs first before skeleton
        capabilities.setChatEnabled(false); // Pipeline-only
        capabilities.setConfigurationValue("lightweight", true);
        capabilities.setConfigurationValue("fastGeneration", true);
        return capabilities;
    }
    
    /**
     * Allocate cities for the itinerary based on destination and user requirements.
     */
    public CityAllocationPlan allocateCities(CreateItineraryReq request, NormalizedItinerary itinerary) {
        logger.info("=== CITY ALLOCATION AGENT ===");
        logger.info("Destination: {}, Duration: {} days", request.getDestination(), request.getDurationDays());
        
        try {
            // Check if trip is too short for multi-city (1-2 days)
            if (request.getDurationDays() <= 2) {
                logger.info("Trip too short for multi-city (1-2 days), creating single city plan");
                return createSingleCityPlan(request);
            }
            
            // ALWAYS use LLM to generate city allocation plan
            // LLM will intelligently determine if destination is single-city or multi-city
            // and return appropriate plan (1 city for Tokyo, multiple cities for Malaysia, etc.)
            logger.info("Calling LLM to analyze destination and create city allocation plan");
            CityAllocationPlan plan = generateCityPlan(request);
            
            // Validate and optimize
            validateAndOptimizePlan(plan, request);
            
            // Store in itinerary agentData
            if (itinerary.getAgentData() == null) {
                itinerary.setAgentData(new HashMap<>());
            }
            
            AgentDataSection agentDataSection = itinerary.getAgentData().computeIfAbsent(
                "cityAllocation", k -> new AgentDataSection()
            );
            agentDataSection.setAgentData("cityAllocation", plan);
            
            // CRITICAL: Update itinerary with destination-specific budget from LLM
            if (plan.getBudgetEstimate() != null) {
                BudgetEstimate budget = plan.getBudgetEstimate();
                itinerary.setBudgetMin(budget.getMinPerPersonPerDay());
                itinerary.setBudgetMax(budget.getMaxPerPersonPerDay());
                itinerary.setCurrency(budget.getCurrency());
                
                logger.info("Updated itinerary budget: {} {}-{} per person per day", 
                           budget.getCurrency(),
                           budget.getMinPerPersonPerDay(),
                           budget.getMaxPerPersonPerDay());
            }
            
            // CRITICAL: Publish city allocation insights to UI via WebSocket
            // This shows destination analysis, city selection, and budget estimate to the user
            // BEFORE skeleton generation starts, giving them context about the plan
            if (agentEventPublisher.hasActiveConnections(itinerary.getItineraryId())) {
                String execId = "city_allocation_" + System.currentTimeMillis();
                agentEventPublisher.publishCityAllocationInsights(itinerary.getItineraryId(), execId, plan);
                logger.info("Published city allocation insights to UI");
            }
            
            logger.info("=== CITY ALLOCATION COMPLETE ===");
            logger.info("Destination Type: {}", plan.getDestinationType());
            logger.info("Plan: {} cities, {} travel segments", 
                       plan.getAllocations().size(), 
                       plan.getTravelSegments().size());
            
            return plan;
            
        } catch (Exception e) {
            logger.error("City allocation failed, using fallback single-city plan: {}", e.getMessage(), e);
            return createFallbackSingleCityPlan(request);
        }
    }
    
    /**
     * Generate city allocation plan using LLM.
     */
    private CityAllocationPlan generateCityPlan(CreateItineraryReq request) {
        String systemPrompt = buildSystemPrompt();
        String userPrompt = buildUserPrompt(request);
        String schema = buildJsonSchema();
        
        logger.info("Generating city plan with FAST_FAIL strategy");
        
        // Use FAST_FAIL strategy for critical path
        String response;
        if (aiClient instanceof ResilientAiClient) {
            ResilientAiClient resilientClient = (ResilientAiClient) aiClient;
            response = resilientClient.generateStructuredContent(userPrompt, schema, systemPrompt, RetryStrategy.FAST_FAIL);
        } else {
            response = aiClient.generateStructuredContent(userPrompt, schema, systemPrompt);
        }
        
        logger.info("=== CITY ALLOCATION AGENT - FULL LLM RESPONSE ===");
        logger.info("Raw Response: {}", response);
        logger.info("=== END CITY ALLOCATION RESPONSE ===");
        
        try {
            // Validate response against schema
            LLMSchemaValidator.ValidationResult validationResult = schemaValidator.validateWithLogging(
                response, schema, "CityAllocation");
            
            if (!validationResult.isValid()) {
                String errorMsg = schemaValidator.getUserFriendlyError(validationResult);
                logger.error("Schema validation failed: {}", errorMsg);
                throw new RuntimeException("LLM response validation failed: " + errorMsg);
            }
            
            // Parse validated data
            CityAllocationPlan plan = objectMapper.treeToValue(validationResult.getData(), CityAllocationPlan.class);
            
            logger.info("City plan parsed successfully: {} cities", plan.getAllocations().size());
            
            // Log budget estimate
            if (plan.getBudgetEstimate() != null) {
                BudgetEstimate budget = plan.getBudgetEstimate();
                logger.info("Budget estimate: {} {}-{} per person per day ({})", 
                           budget.getCurrency(),
                           budget.getMinPerPersonPerDay(),
                           budget.getMaxPerPersonPerDay(),
                           budget.getRationale());
            } else {
                logger.warn("No budget estimate provided by LLM");
            }
            
            return plan;
            
        } catch (JsonProcessingException e) {
            logger.error("Failed to parse city allocation response", e);
            throw new RuntimeException("Failed to parse city allocation: " + e.getMessage(), e);
        }
    }
    
    /**
     * Validate and optimize the city allocation plan.
     */
    private void validateAndOptimizePlan(CityAllocationPlan plan, CreateItineraryReq request) {
        if (plan.getAllocations() == null || plan.getAllocations().isEmpty()) {
            throw new RuntimeException("City allocation plan has no cities");
        }
        
        // Ensure day allocations are valid
        int totalDays = 0;
        for (CityAllocation allocation : plan.getAllocations()) {
            if (allocation.getStartDay() < 1 || allocation.getEndDay() < allocation.getStartDay()) {
                logger.warn("Invalid day allocation for {}: start={}, end={}", 
                           allocation.getCityName(), allocation.getStartDay(), allocation.getEndDay());
            }
            totalDays += allocation.getTotalDays();
        }
        
        if (totalDays != request.getDurationDays()) {
            logger.warn("Total allocated days ({}) doesn't match request ({} days)", 
                       totalDays, request.getDurationDays());
        }
        
        // Ensure travel segments reference valid cities
        if (plan.getTravelSegments() != null) {
            Set<String> cityNames = new HashSet<>();
            for (CityAllocation allocation : plan.getAllocations()) {
                cityNames.add(allocation.getCityName());
            }
            
            for (TravelSegment segment : plan.getTravelSegments()) {
                if (!cityNames.contains(segment.getFromCity()) || !cityNames.contains(segment.getToCity())) {
                    logger.warn("Travel segment references unknown city: {} -> {}", 
                               segment.getFromCity(), segment.getToCity());
                }
            }
        }
        
        logger.info("City allocation plan validated successfully");
    }
    
    /**
     * Create a simple single-city plan.
     */
    private CityAllocationPlan createSingleCityPlan(CreateItineraryReq request) {
        CityAllocationPlan plan = new CityAllocationPlan();
        plan.setDestinationType("city");
        plan.setPrimaryDestination(request.getDestination());
        plan.setPlanningRationale("Single city destination");
        
        CityAllocation allocation = new CityAllocation();
        allocation.setCityName(request.getDestination());
        allocation.setRegion(request.getDestination());
        allocation.setStartDay(1);
        allocation.setEndDay(request.getDurationDays());
        allocation.setTotalDays(request.getDurationDays());
        allocation.setPriority("must-visit");
        allocation.setReason("Primary destination");
        allocation.setHighlights(new ArrayList<>());
        allocation.setDestinationType("city");
        
        plan.setAllocations(List.of(allocation));
        plan.setTravelSegments(new ArrayList<>());
        
        return plan;
    }
    
    /**
     * Create fallback single-city plan in case of errors.
     */
    private CityAllocationPlan createFallbackSingleCityPlan(CreateItineraryReq request) {
        CityAllocationPlan plan = createSingleCityPlan(request);
        plan.setPlanningRationale("Fallback plan due to error in city allocation");
        logger.warn("Using fallback single-city plan for: {}", request.getDestination());
        return plan;
    }
    
    /**
     * Build system prompt for city allocation.
     */
    private String buildSystemPrompt() {
        return """
            You are a travel planning expert specializing in multi-city itinerary design.
            
            Your task: Analyze the destination and create a realistic city allocation plan WITH budget estimation.
            
            STEP 1: DETERMINE DESTINATION TYPE
            - Is it a CITY? (e.g., Tokyo, Paris, New York) → Return 1 city
            - Is it a STATE/PROVINCE? (e.g., Assam, California, Bavaria) → Return 2-4 cities
            - Is it a COUNTRY? (e.g., Malaysia, Japan, Italy) → Return 2-5 cities based on duration
            - Is it a REGION? (e.g., Southeast Asia, Scandinavia) → Return 3-6 cities
            
            STEP 2: ALLOCATE CITIES (if multi-city destination)
            
            CRITICAL RULES:
            1. ALWAYS respect user-specified cities if provided in constraints
            2. ALWAYS respect user-specified city count if provided in constraints
            3. Allocate minimum 1 day per city, maximum based on city importance
            4. Consider travel time between cities (don't plan impossible schedules)
            5. Prioritize cities based on:
               - Tourist importance
               - Unique attractions
               - Accessibility
               - User interests
            6. Plan logical travel order (minimize backtracking)
            
            CRITICAL DAY ALLOCATION RULES:
            - Each day belongs to EXACTLY ONE city
            - NO OVERLAP: If City A ends on Day 2, City B must start on Day 3 (not Day 2)
            - Travel day belongs to the DESTINATION city (where you're going TO)
            - Example for 5-day trip with 2 cities:
              * City A: startDay=1, endDay=2 (Days 1, 2)
              * City B: startDay=3, endDay=5 (Days 3, 4, 5)
              * Travel on Day 3 from City A to City B
            - WRONG: City A endDay=3, City B startDay=3 (Day 3 belongs to both!)
            - RIGHT: City A endDay=2, City B startDay=3 (no overlap)
            
            City Allocation Guidelines:
            - Gateway cities: 1-2 days (arrival/departure hub)
            - Major attractions: 2-4 days (main destinations)
            - Secondary cities: 1-2 days (if time permits)
            - Nature destinations: 2-3 days (safaris, treks need time)
            - Cultural cities: 1-2 days (temples, museums)
            
            Travel Time Considerations:
            - Flight: 2-4 hours + airport time = half day lost
            - Train: 4-8 hours = half to full day lost
            - Bus/Car: 3-6 hours = half day lost
            - If travel > 6 hours, mark as full-day travel (no activities)
            
            STEP 3: ESTIMATE REALISTIC BUDGET (NEW!)
            
            Based on the destination and budget tier, provide realistic per-person per-day budget ranges in LOCAL CURRENCY.
            
            Budget Tier Guidelines:
            - "budget": Hostels, street food, free/cheap attractions, public transport more than others
            - "medium": 3-star hotels, casual dining, standard attractions, mix of transport
            - "luxury": 4-5 star hotels, fine dining, exclusive experiences, private transport + flights when it saves time compared to other modes 
            
            Consider destination's cost of living:
            - Expensive destinations (Tokyo, Paris, Dubai): Higher ranges
            - Moderate destinations (Malaysia, Thailand, Mexico): Mid ranges
            - Budget destinations (India, Vietnam, Nepal): Lower ranges
            
            IMPORTANT: Budget should be realistic for the destination, not generic!
            
            Output Format: JSON with cities, day allocations, travel segments, AND budget estimate
            """;
    }
    
    /**
     * Build user prompt for city allocation.
     * UPDATED: Now includes budget tier for destination-specific budget estimation.
     */
    private String buildUserPrompt(CreateItineraryReq request) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("ANALYZE THIS DESTINATION:\n");
        prompt.append("Destination: ").append(request.getDestination()).append("\n");
        prompt.append("Total Days: ").append(request.getDurationDays()).append("\n");
        
        // CRITICAL: Include budget tier for destination-specific budget estimation
        String budgetTier = request.getBudgetTier() != null ? request.getBudgetTier() : "medium";
        prompt.append("Budget Tier: ").append(budgetTier).append("\n");
        prompt.append("IMPORTANT: Provide realistic budget estimate for ").append(request.getDestination());
        prompt.append(" based on '").append(budgetTier).append("' tier in LOCAL CURRENCY.\n");
        
        if (request.getInterests() != null && !request.getInterests().isEmpty()) {
            prompt.append("User Interests: ").append(String.join(", ", request.getInterests())).append("\n");
        }
        
        prompt.append("\nUSER REQUIREMENTS (MUST FOLLOW):\n");
        if (request.getConstraints() != null && !request.getConstraints().isEmpty()) {
            for (String constraint : request.getConstraints()) {
                prompt.append("- ").append(constraint).append("\n");
            }
        } else {
            prompt.append("- No specific constraints\n");
        }
        
        prompt.append("\n=== INSTRUCTIONS ===\n");
        prompt.append("1. First, determine: Is '").append(request.getDestination()).append("' a city, state, country, or region?\n");
        prompt.append("2. Set destinationType accordingly: \"city\", \"state\", \"country\", or \"region\"\n");
        prompt.append("3. CITY ALLOCATION RULES:\n");
        prompt.append("   - Small/Medium Cities (e.g., Guwahati, Bruges): 1 city allocation\n");
        prompt.append("   - Large Metropolises with distinct districts (e.g., Hong Kong, Tokyo, NYC):\n");
        prompt.append("     * If duration ≤ 3 days: 1 city allocation\n");
        prompt.append("     * If duration ≥ 4 days: Consider splitting into districts/regions\n");
        prompt.append("       Examples: Hong Kong → 'Hong Kong Island', 'Kowloon', 'Lantau Island'\n");
        prompt.append("                 Tokyo → 'Central Tokyo', 'Western Tokyo', 'Yokohama'\n");
        prompt.append("4. If it's a STATE/COUNTRY/REGION: Return multiple cities based on duration\n");
        prompt.append("5. ESTIMATE REALISTIC BUDGET for this destination based on '").append(budgetTier).append("' tier\n");
        prompt.append("\nCRITICAL: If user specifies cities or city count in requirements, RESPECT IT EXACTLY.\n");
        prompt.append("\nExamples:\n");
        prompt.append("- \"Tokyo\" → destinationType: \"city\", 1 city (Tokyo), budget: ¥25,000-40,000/day (luxury)\n");
        prompt.append("- \"Malaysia\" → destinationType: \"country\", 2-3 cities (Kuala Lumpur, Penang, Malacca), budget: RM 300-500/day (luxury)\n");
        prompt.append("- \"Assam\" → destinationType: \"state\", 2-3 cities (Guwahati, Kaziranga, Majuli), budget: ₹3,000-6,000/day (medium)\n");
        prompt.append("\nProvide:\n");
        prompt.append("1. destinationType (city/state/country/region)\n");
        prompt.append("2. budgetEstimate (currency, min/max per person per day, rationale)\n");
        prompt.append("3. List of cities with day allocations\n");
        prompt.append("4. Reason for each city selection\n");
        prompt.append("5. Travel segments between cities (if multi-city)\n");
        prompt.append("6. Highlights for each city\n");
        
        return prompt.toString();
    }
    
    /**
     * Build JSON schema for city allocation response.
     * UPDATED: Now includes budgetEstimate field.
     */
    private String buildJsonSchema() {
        return """
            {
              "type": "object",
              "properties": {
                "destinationType": {
                  "type": "string",
                  "enum": ["city", "state", "country", "region"]
                },
                "primaryDestination": { "type": "string" },
                "planningRationale": { "type": "string" },
                "budgetEstimate": {
                  "type": "object",
                  "properties": {
                    "currency": { "type": "string" },
                    "minPerPersonPerDay": { "type": "number", "minimum": 0 },
                    "maxPerPersonPerDay": { "type": "number", "minimum": 0 },
                    "rationale": { "type": "string" }
                  },
                  "required": ["currency", "minPerPersonPerDay", "maxPerPersonPerDay", "rationale"]
                },
                "allocations": {
                  "type": "array",
                  "minItems": 1,
                  "items": {
                    "type": "object",
                    "properties": {
                      "cityName": { "type": "string" },
                      "region": { "type": "string" },
                      "startDay": { "type": "integer", "minimum": 1 },
                      "endDay": { "type": "integer", "minimum": 1 },
                      "totalDays": { "type": "integer", "minimum": 1 },
                      "priority": {
                        "type": "string",
                        "enum": ["must-visit", "recommended", "optional"]
                      },
                      "reason": { "type": "string" },
                      "highlights": {
                        "type": "array",
                        "items": { "type": "string" },
                        "minItems": 3,
                        "maxItems": 5
                      },
                      "destinationType": {
                        "type": "string",
                        "enum": ["gateway", "main-attraction", "cultural", "nature"]
                      }
                    },
                    "required": ["cityName", "region", "startDay", "endDay", "totalDays", "priority", "reason", "highlights", "destinationType"]
                  }
                },
                "travelSegments": {
                  "type": "array",
                  "items": {
                    "type": "object",
                    "properties": {
                      "dayNumber": { "type": "integer", "minimum": 1 },
                      "fromCity": { "type": "string" },
                      "toCity": { "type": "string" },
                      "travelMode": {
                        "type": "string",
                        "enum": ["flight", "train", "bus", "car", "ferry", "car+ferry"]
                      },
                      "estimatedHours": { "type": "integer", "minimum": 1 },
                      "isFullDayTravel": { "type": "boolean" },
                      "notes": { "type": "string" }
                    },
                    "required": ["dayNumber", "fromCity", "toCity", "travelMode", "estimatedHours", "isFullDayTravel"]
                  }
                }
              },
              "required": ["destinationType", "primaryDestination", "planningRationale", "budgetEstimate", "allocations", "travelSegments"]
            }
            """;
    }
    
    @Override
    protected <T> T executeInternal(String itineraryId, AgentRequest<T> request) {
        // Extract CreateItineraryReq from Map or use directly
        CreateItineraryReq itineraryReq;
        Object data = request.getData();
        if (data instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> dataMap = (Map<String, Object>) data;
            itineraryReq = (CreateItineraryReq) dataMap.get("request");
        } else if (data instanceof CreateItineraryReq) {
            itineraryReq = (CreateItineraryReq) data;
        } else {
            itineraryReq = null;
        }
        
        if (itineraryReq == null) {
            throw new IllegalArgumentException("CityAllocationAgent requires CreateItineraryReq");
        }
        
        // Load itinerary
        Optional<NormalizedItinerary> itineraryOpt = itineraryJsonService.getItinerary(itineraryId);
        if (itineraryOpt.isEmpty()) {
            throw new RuntimeException("Itinerary not found: " + itineraryId);
        }
        
        CityAllocationPlan plan = allocateCities(itineraryReq, itineraryOpt.get());
        
        return (T) plan;
    }
    
    @Override
    protected String getAgentName() {
        return "City Allocation Agent";
    }
}
