# Complete Tool Reference for All Agents

**Last Updated:** 2025-11-26  
**Status:** All 8 tools implemented  
**Purpose:** Comprehensive guide for every agent and every tool

---

## Table of Contents

1. [Tool Overview](#tool-overview)
2. [Tool 1: Generate Node ID](#tool-1-generate-node-id)
3. [Tool 2: Check User Constraints](#tool-2-check-user-constraints)
4. [Tool 3: Convert Currency](#tool-3-convert-currency)
5. [Tool 4: Calculate Cost](#tool-4-calculate-cost)
6. [Tool 5: Check Conflicts](#tool-5-check-conflicts)
7. [Tool 6: Validate Schema](#tool-6-validate-schema)
8. [Tool 7: Calculate Distance](#tool-7-calculate-distance)
9. [Tool 8: Suggest Restaurants](#tool-8-suggest-restaurants)
10. [Agent Integration Matrix](#agent-integration-matrix)
11. [Complete Agent Examples](#complete-agent-examples)

---

## Tool Overview

| Tool | Endpoint | P0/P1 | Agents | Purpose |
|------|----------|-------|--------|---------|
| Generate Node ID | `/api/v1/tools/generate-node-id` | P0-1 | All | Prevent ID conflicts |
| Check Constraints | `/api/v1/tools/check-user-constraints` | P0-2 | All | Validate user requirements |
| Convert Currency | `/api/v1/tools/convert-currency` | P0-6 | Cost, Meal, Activity, Transport | Multi-currency support |
| Calculate Cost | `/api/v1/tools/calculate-cost` | P0-5 | Cost, Editor, Explain | Budget tracking |
| Check Conflicts | `/api/v1/tools/check-conflicts` | P1-1 | Editor, DayByDay | Time/budget conflicts |
| Validate Schema | `/api/v1/tools/validate-schema` | P1-2 | All | JSON validation |
| Calculate Distance | `/api/v1/tools/calculate-distance` | P1-3 | Transport, DayByDay | Travel time |
| Suggest Restaurants | `/api/v1/tools/suggest-restaurants` | P1-4 | Meal | Dietary filtering |

---


## Tool 1: Generate Node ID

**Endpoint:** `POST /api/v1/tools/generate-node-id`  
**Priority:** P0-1 (Critical)  
**Status:** ✅ Implemented

### Purpose
Generates unique node IDs to prevent conflicts across all agents.

### Request Format
```json
{
  "itineraryId": "string (required)",
  "dayNumber": "integer (required)",
  "nodeType": "string (required: attraction|meal|transport|accommodation)",
  "sequenceHint": "integer (optional)"
}
```

### Response Format
```json
{
  "success": true,
  "nodeId": "day1_attraction_001",
  "error": null
}
```

### Which Agents Use This
- ✅ **SkeletonPlannerAgent** - Generate IDs for skeleton nodes
- ✅ **ActivityAgent** - Generate IDs for attractions
- ✅ **MealAgent** - Generate IDs for meals
- ✅ **TransportAgent** - Generate IDs for transport
- ✅ **DayByDayPlannerAgent** - Generate IDs for all node types
- ✅ **EditorAgent** - Generate IDs for new nodes from edits

### Code Example
```java
private String generateNodeId(String itineraryId, Integer dayNumber, String nodeType) {
    NodeIdRequest request = new NodeIdRequest(itineraryId, dayNumber, nodeType);
    
    try {
        NodeIdResponse response = restTemplate.postForObject(
            "http://localhost:8080/api/v1/tools/generate-node-id",
            request,
            NodeIdResponse.class
        );
        
        if (response != null && response.isSuccess()) {
            logger.info("Generated node ID: {}", response.getNodeId());
            return response.getNodeId();
        }
    } catch (Exception e) {
        logger.error("Failed to generate node ID", e);
    }
    
    // Fallback
    return nodeIdGenerator.generateNodeId(nodeType, dayNumber, itinerary);
}
```

### When to Call
- **BEFORE** creating any new node
- **ALWAYS** for new nodes, never reuse IDs
- **FALLBACK** to service if HTTP call fails

---


## Tool 2: Check User Constraints

**Endpoint:** `POST /api/v1/tools/check-user-constraints`  
**Priority:** P0-2 (Critical)  
**Status:** ✅ Implemented

### Purpose
Validates that itinerary respects user constraints (budget, dietary, party size).

### Request Format
```json
{
  "itineraryId": "string (required)",
  "proposedNode": "object (optional)",
  "checkBudget": "boolean (default: true)",
  "checkDietary": "boolean (default: true)",
  "checkPartySize": "boolean (default: true)"
}
```

### Response Format
```json
{
  "valid": true,
  "violations": [],
  "warnings": ["Approaching budget limit: 90% used"],
  "error": null
}
```

### Which Agents Use This
- ✅ **ActivityAgent** - Check budget before adding attractions
- ✅ **MealAgent** - Check dietary restrictions and budget
- ✅ **TransportAgent** - Check budget for transport options
- ✅ **DayByDayPlannerAgent** - Check all constraints before planning
- ✅ **EditorAgent** - Validate edits don't violate constraints

### Code Example
```java
private ConstraintCheckResult checkConstraints(String itineraryId, NormalizedNode proposedNode) {
    ConstraintCheckRequest request = new ConstraintCheckRequest(itineraryId);
    request.setProposedNode(proposedNode);
    request.setCheckBudget(true);
    request.setCheckDietary(true);
    request.setCheckPartySize(true);
    
    try {
        ConstraintCheckResult result = restTemplate.postForObject(
            "http://localhost:8080/api/v1/tools/check-user-constraints",
            request,
            ConstraintCheckResult.class
        );
        
        if (result != null && !result.isValid()) {
            logger.warn("Constraint violations:");
            result.getViolations().forEach(v -> 
                logger.warn("  - {}: {}", v.getType(), v.getMessage())
            );
        }
        
        return result;
    } catch (Exception e) {
        logger.error("Failed to check constraints", e);
        return new ConstraintCheckResult(); // Valid by default on error
    }
}
```

### When to Call
- **BEFORE** generating nodes
- **AFTER** LLM generates content but before saving
- **DURING** editing to validate changes

---


## Tool 3: Convert Currency

**Endpoint:** `POST /api/v1/tools/convert-currency`  
**Priority:** P0-6 (Critical)  
**Status:** ✅ Implemented

### Purpose
Converts amounts between currencies for accurate cost estimation.

### Request Format
```json
{
  "amount": 100.0,
  "fromCurrency": "USD",
  "toCurrency": "INR"
}
```

### Response Format
```json
{
  "success": true,
  "fromCurrency": "USD",
  "toCurrency": "INR",
  "originalAmount": 100.0,
  "convertedAmount": 8300.0,
  "fromSymbol": "$",
  "toSymbol": "₹",
  "formattedOriginal": "$100.00",
  "formattedConverted": "₹8,300.00"
}
```

### Which Agents Use This
- ✅ **CostEstimatorAgent** - Convert all costs to destination currency
- ✅ **MealAgent** - Convert meal costs
- ✅ **ActivityAgent** - Convert activity costs
- ✅ **TransportAgent** - Convert transport costs
- ✅ **ExplainAgent** - Show costs in user's preferred currency

### Code Example
```java
private double convertCurrency(double amount, String fromCurrency, String toCurrency) {
    Map<String, Object> request = Map.of(
        "amount", amount,
        "fromCurrency", fromCurrency,
        "toCurrency", toCurrency
    );
    
    try {
        @SuppressWarnings("unchecked")
        Map<String, Object> response = restTemplate.postForObject(
            "http://localhost:8080/api/v1/tools/convert-currency",
            request,
            Map.class
        );
        
        if (response != null && Boolean.TRUE.equals(response.get("success"))) {
            return ((Number) response.get("convertedAmount")).doubleValue();
        }
    } catch (Exception e) {
        logger.error("Failed to convert currency", e);
    }
    
    // Fallback to service
    return currencyConversionService.convert(amount, fromCurrency, toCurrency);
}
```

### When to Call
- **BEFORE** displaying costs to user
- **WHEN** destination currency differs from cost currency
- **DURING** budget calculations

---


## Tool 4: Calculate Cost

**Endpoint:** `POST /api/v1/tools/calculate-cost`  
**Priority:** P0-5 (Critical - Enhanced)  
**Status:** ✅ Implemented with Budget Analysis

### Purpose
Calculates total itinerary cost with detailed budget analysis and recommendations.

### Request Format
```json
{
  "itineraryId": "string (required)",
  "partySize": "integer (optional)",
  "includeBudgetAnalysis": "boolean (default: true)"
}
```

### Response Format
```json
{
  "success": true,
  "totalCostPerPerson": 1250.00,
  "totalCostForParty": 2500.00,
  "currency": "USD",
  "partySize": 2,
  "breakdown": {
    "accommodation": 600.00,
    "activities": 300.00,
    "meals": 250.00,
    "transport": 100.00,
    "other": 0.00
  },
  "budgetAnalysis": {
    "budgetMax": 1500.00,
    "budgetRemaining": 250.00,
    "budgetUsedPercentage": 83.3,
    "budgetStatus": "MODERATE",
    "overage": null,
    "recommendations": [
      "MODERATE USAGE: Using 83% of budget (1250.00 / 1500.00 USD)"
    ]
  },
  "warnings": []
}
```

### Which Agents Use This
- ✅ **CostEstimatorAgent** - Primary user
- ✅ **EditorAgent** - Recalculate after edits
- ✅ **ExplainAgent** - Show cost breakdown to user
- ✅ **DayByDayPlannerAgent** - Check budget during planning

### Code Example
```java
private CostCalculationResult calculateCost(String itineraryId, Integer partySize) {
    CostCalculationRequest request = new CostCalculationRequest(itineraryId, partySize);
    request.setIncludeBudgetAnalysis(true);
    
    try {
        CostCalculationResult result = restTemplate.postForObject(
            "http://localhost:8080/api/v1/tools/calculate-cost",
            request,
            CostCalculationResult.class
        );
        
        if (result != null && result.isSuccess()) {
            logger.info("Total cost: {} {} per person", 
                       result.getTotalCostPerPerson(), result.getCurrency());
            
            if (result.getBudgetAnalysis() != null) {
                String status = result.getBudgetAnalysis().getBudgetStatus();
                logger.info("Budget status: {}", status);
                
                if ("OVER".equals(status)) {
                    logger.warn("BUDGET EXCEEDED!");
                }
            }
        }
        
        return result;
    } catch (Exception e) {
        logger.error("Failed to calculate cost", e);
        return CostCalculationResult.error(e.getMessage());
    }
}
```

### When to Call
- **AFTER** all nodes are populated
- **BEFORE** finalizing itinerary
- **DURING** editing to show updated costs
- **FOR** budget warnings and recommendations

---


## Tool 5: Check Conflicts

**Endpoint:** `POST /api/v1/tools/check-conflicts`  
**Priority:** P1-1 (High)  
**Status:** ✅ Implemented

### Purpose
Detects time overlaps and budget conflicts before applying changes.

### Request Format
```json
{
  "itineraryId": "string (required)",
  "proposedChanges": "ChangeSet (optional)",
  "checkTimeConflicts": "boolean (default: true)",
  "checkBudgetConflicts": "boolean (default: true)"
}
```

### Response Format
```json
{
  "hasConflicts": false,
  "conflicts": [],
  "warnings": [],
  "error": null
}
```

### Which Agents Use This
- ✅ **EditorAgent** - Validate edits before applying
- ✅ **DayByDayPlannerAgent** - Check schedule conflicts
- ✅ **TransportAgent** - Ensure transport times don't overlap

### Code Example
```java
private ConflictCheckResult checkConflicts(String itineraryId, ChangeSet proposedChanges) {
    ConflictCheckRequest request = new ConflictCheckRequest(itineraryId);
    request.setProposedChanges(proposedChanges);
    request.setCheckTimeConflicts(true);
    request.setCheckBudgetConflicts(true);
    
    try {
        ConflictCheckResult result = restTemplate.postForObject(
            "http://localhost:8080/api/v1/tools/check-conflicts",
            request,
            ConflictCheckResult.class
        );
        
        if (result != null && result.hasConflicts()) {
            logger.warn("Conflicts detected:");
            result.getConflicts().forEach(c -> 
                logger.warn("  - {}: {}", c.getType(), c.getMessage())
            );
        }
        
        return result;
    } catch (Exception e) {
        logger.error("Failed to check conflicts", e);
        return ConflictCheckResult.error(e.getMessage());
    }
}
```

### When to Call
- **BEFORE** applying ChangeSet
- **AFTER** LLM generates schedule changes
- **DURING** editing to prevent conflicts

---


## Tool 6: Validate Schema

**Endpoint:** `POST /api/v1/tools/validate-schema`  
**Priority:** P1-2 (High)  
**Status:** ✅ Implemented

### Purpose
Validates LLM JSON output against expected schema before processing.

### Request Format
```json
{
  "jsonOutput": "string (required)",
  "jsonSchema": "string (required)",
  "cleanBeforeValidation": "boolean (default: true)"
}
```

### Response Format
```json
{
  "valid": true,
  "errors": [],
  "retryable": false,
  "userFriendlyMessage": null
}
```

### Which Agents Use This
- ✅ **ALL AGENTS** - Validate LLM responses
- ✅ **ActivityAgent** - Validate attraction JSON
- ✅ **MealAgent** - Validate meal JSON
- ✅ **TransportAgent** - Validate transport JSON
- ✅ **EditorAgent** - Validate edit JSON

### Code Example
```java
private boolean validateLLMResponse(String jsonOutput, String jsonSchema) {
    SchemaValidationRequest request = new SchemaValidationRequest();
    request.setJsonOutput(jsonOutput);
    request.setJsonSchema(jsonSchema);
    request.setCleanBeforeValidation(true);
    
    try {
        SchemaValidationResult result = restTemplate.postForObject(
            "http://localhost:8080/api/v1/tools/validate-schema",
            request,
            SchemaValidationResult.class
        );
        
        if (result != null && !result.isValid()) {
            logger.error("Schema validation failed:");
            result.getErrors().forEach(error -> logger.error("  - {}", error));
            
            if (result.isRetryable()) {
                logger.info("Error is retryable, will retry LLM call");
                return false;
            }
        }
        
        return result != null && result.isValid();
    } catch (Exception e) {
        logger.error("Failed to validate schema", e);
        return false; // Fail safe
    }
}
```

### When to Call
- **IMMEDIATELY** after LLM response
- **BEFORE** parsing JSON
- **FOR** retry logic on validation failure

---


## Tool 7: Calculate Distance

**Endpoint:** `POST /api/v1/tools/calculate-distance`  
**Priority:** P1-3 (High)  
**Status:** ✅ Implemented

### Purpose
Calculates accurate distance and travel time between locations.

### Request Format
```json
{
  "origin": "string (optional)",
  "destination": "string (optional)",
  "originCoordinates": {"lat": 28.6139, "lng": 77.2090},
  "destinationCoordinates": {"lat": 28.5244, "lng": 77.1855},
  "mode": "driving|walking|transit|bicycling"
}
```

### Response Format
```json
{
  "success": true,
  "distanceKm": 10.21,
  "durationMinutes": 20,
  "mode": "driving",
  "fromAPI": false,
  "origin": "New Delhi",
  "destination": "Qutub Minar",
  "warnings": ["Using fallback calculation (Haversine formula)"]
}
```

### Which Agents Use This
- ✅ **TransportAgent** - Calculate actual travel times
- ✅ **DayByDayPlannerAgent** - Validate schedule feasibility
- ✅ **ActivityAgent** - Check if attractions are reachable

### Code Example
```java
private DistanceCalculationResult calculateDistance(
        Coordinates origin, Coordinates destination, String mode) {
    
    DistanceCalculationRequest request = new DistanceCalculationRequest();
    request.setOriginCoordinates(origin);
    request.setDestinationCoordinates(destination);
    request.setMode(mode);
    
    try {
        DistanceCalculationResult result = restTemplate.postForObject(
            "http://localhost:8080/api/v1/tools/calculate-distance",
            request,
            DistanceCalculationResult.class
        );
        
        if (result != null && result.isSuccess()) {
            logger.info("Distance: {} km, Duration: {} min", 
                       result.getDistanceKm(), result.getDurationMinutes());
            
            if (!result.isFromAPI()) {
                logger.warn("Using fallback calculation");
            }
        }
        
        return result;
    } catch (Exception e) {
        logger.error("Failed to calculate distance", e);
        return DistanceCalculationResult.error(e.getMessage());
    }
}
```

### When to Call
- **WHEN** creating transport nodes
- **BEFORE** scheduling activities
- **TO** validate travel time feasibility

---


## Tool 8: Suggest Restaurants

**Endpoint:** `POST /api/v1/tools/suggest-restaurants`  
**Priority:** P1-4 (High)  
**Status:** ✅ Implemented (Placeholder for Google Places API)

### Purpose
Suggests restaurants filtered by dietary restrictions and preferences.

### Request Format
```json
{
  "itineraryId": "string (optional)",
  "location": {"lat": 48.8566, "lng": 2.3522},
  "locationName": "Paris, France",
  "cuisineType": "French",
  "priceLevel": 2,
  "mealType": "dinner",
  "dietaryRestrictions": ["vegetarian", "gluten-free"],
  "radius": 5000
}
```

### Response Format
```json
{
  "success": true,
  "restaurants": [
    {
      "placeId": "ChIJ...",
      "name": "Le Potager du Marais",
      "address": "22 Rue Rambuteau, Paris",
      "rating": 4.5,
      "priceLevel": 2,
      "userRatingsTotal": 1234,
      "cuisineTypes": ["French", "Vegetarian"],
      "openNow": true,
      "photoReference": "CmRa...",
      "distanceKm": 1.2,
      "matchesDietaryRestrictions": true
    }
  ],
  "totalResults": 1,
  "warnings": []
}
```

### Which Agents Use This
- ✅ **MealAgent** - Primary user for restaurant suggestions
- ✅ **DayByDayPlannerAgent** - Suggest meals during planning

### Code Example
```java
private RestaurantSuggestionResult suggestRestaurants(
        String itineraryId, Coordinates location, List<String> dietaryRestrictions) {
    
    RestaurantSuggestionRequest request = new RestaurantSuggestionRequest();
    request.setItineraryId(itineraryId);
    request.setLocation(location);
    request.setDietaryRestrictions(dietaryRestrictions);
    request.setPriceLevel(2);
    request.setRadius(5000);
    
    try {
        RestaurantSuggestionResult result = restTemplate.postForObject(
            "http://localhost:8080/api/v1/tools/suggest-restaurants",
            request,
            RestaurantSuggestionResult.class
        );
        
        if (result != null && result.isSuccess()) {
            logger.info("Found {} restaurants", result.getTotalResults());
            
            result.getRestaurants().forEach(r -> 
                logger.info("  - {}: {} ({})", r.getName(), r.getRating(), r.getPriceLevel())
            );
        }
        
        return result;
    } catch (Exception e) {
        logger.error("Failed to suggest restaurants", e);
        return RestaurantSuggestionResult.error(e.getMessage());
    }
}
```

### When to Call
- **WHEN** creating meal nodes
- **BEFORE** LLM generates meal suggestions
- **TO** filter by dietary restrictions

---


## Agent Integration Matrix

This matrix shows which tools each agent should use:

| Agent | Node ID | Constraints | Currency | Cost | Conflicts | Schema | Distance | Restaurants |
|-------|---------|-------------|----------|------|-----------|--------|----------|-------------|
| **SkeletonPlannerAgent** | ✅ | ✅ | ❌ | ❌ | ❌ | ✅ | ❌ | ❌ |
| **ActivityAgent** | ✅ | ✅ | ✅ | ❌ | ❌ | ✅ | ✅ | ❌ |
| **MealAgent** | ✅ | ✅ | ✅ | ❌ | ❌ | ✅ | ❌ | ✅ |
| **TransportAgent** | ✅ | ✅ | ✅ | ❌ | ❌ | ✅ | ✅ | ❌ |
| **DayByDayPlannerAgent** | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| **EditorAgent** | ✅ | ✅ | ❌ | ✅ | ✅ | ✅ | ❌ | ❌ |
| **CostEstimatorAgent** | ❌ | ✅ | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ |
| **EnrichmentAgent** | ❌ | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ | ❌ |
| **ExplainAgent** | ❌ | ❌ | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ |
| **BookingAgent** | ❌ | ❌ | ✅ | ❌ | ❌ | ✅ | ❌ | ❌ |

### Priority Order for Implementation

**Phase 1: Critical (Week 1)**
1. All agents: Add Schema Validation
2. All node-creating agents: Add Node ID Generation
3. All agents: Add Constraint Checking

**Phase 2: Cost & Currency (Week 2)**
4. Cost/Activity/Meal/Transport: Add Currency Conversion
5. Cost/Editor/Explain: Add Cost Calculation

**Phase 3: Advanced (Week 3)**
6. Editor/DayByDay: Add Conflict Detection
7. Transport/DayByDay: Add Distance Calculation
8. Meal/DayByDay: Add Restaurant Suggestions

---


## Complete Agent Examples

### Example 1: ActivityAgent with All Tools

```java
@Component
public class ActivityAgent extends BaseAgent {
    
    private final RestTemplate restTemplate;
    private final AiClient aiClient;
    private final ItineraryJsonService itineraryJsonService;
    
    @Override
    protected <T> T executeInternal(String itineraryId, AgentRequest<T> request) {
        logger.info("=== ACTIVITY AGENT START ===");
        
        // STEP 1: Check user constraints
        ConstraintCheckResult constraints = checkConstraints(itineraryId);
        if (!constraints.isValid()) {
            logger.warn("Constraint violations detected");
            // Adjust strategy based on violations
        }
        
        // STEP 2: Load itinerary
        NormalizedItinerary itinerary = itineraryJsonService.getItinerary(itineraryId)
            .orElseThrow(() -> new RuntimeException("Itinerary not found"));
        
        // STEP 3: Process each day
        for (NormalizedDay day : itinerary.getDays()) {
            for (NormalizedNode node : day.getNodes()) {
                if (!"attraction".equals(node.getType())) continue;
                
                // STEP 4: Generate unique ID
                String nodeId = generateNodeId(itineraryId, day.getDayNumber(), "attraction");
                node.setId(nodeId);
                
                // STEP 5: Get LLM response
                String llmResponse = aiClient.generateStructuredContent(
                    buildPrompt(node, itinerary, constraints),
                    getSchema(),
                    getSystemPrompt()
                );
                
                // STEP 6: Validate schema
                if (!validateSchema(llmResponse, getSchema())) {
                    logger.error("Invalid LLM response, retrying...");
                    continue;
                }
                
                // STEP 7: Parse and populate node
                populateNode(node, llmResponse);
                
                // STEP 8: Convert currency if needed
                if (node.getCost() != null && !itinerary.getCurrency().equals(node.getCost().getCurrency())) {
                    double converted = convertCurrency(
                        node.getCost().getAmountPerPerson(),
                        node.getCost().getCurrency(),
                        itinerary.getCurrency()
                    );
                    node.getCost().setAmountPerPerson(converted);
                    node.getCost().setCurrency(itinerary.getCurrency());
                }
                
                // STEP 9: Calculate distance to next node (if applicable)
                NormalizedNode nextNode = getNextNode(day, node);
                if (nextNode != null && nextNode.getLocation() != null) {
                    DistanceCalculationResult distance = calculateDistance(
                        node.getLocation().getCoordinates(),
                        nextNode.getLocation().getCoordinates(),
                        "walking"
                    );
                    logger.info("Distance to next: {} km", distance.getDistanceKm());
                }
            }
        }
        
        // STEP 10: Save itinerary
        itineraryJsonService.updateItineraryWithLock(itinerary);
        
        logger.info("=== ACTIVITY AGENT COMPLETE ===");
        return (T) itinerary;
    }
    
    // Helper methods for each tool...
}
```

---


### Example 2: MealAgent with Restaurant Suggestions

```java
@Component
public class MealAgent extends BaseAgent {
    
    private final RestTemplate restTemplate;
    private final AiClient aiClient;
    
    @Override
    protected <T> T executeInternal(String itineraryId, AgentRequest<T> request) {
        logger.info("=== MEAL AGENT START ===");
        
        // Load itinerary
        NormalizedItinerary itinerary = itineraryJsonService.getItinerary(itineraryId).get();
        
        // Check dietary restrictions
        ConstraintCheckResult constraints = checkConstraints(itineraryId);
        List<String> dietaryRestrictions = extractDietaryRestrictions(constraints);
        
        for (NormalizedDay day : itinerary.getDays()) {
            for (NormalizedNode node : day.getNodes()) {
                if (!"meal".equals(node.getType())) continue;
                
                // Generate ID
                node.setId(generateNodeId(itineraryId, day.getDayNumber(), "meal"));
                
                // Get restaurant suggestions
                RestaurantSuggestionResult restaurants = suggestRestaurants(
                    itineraryId,
                    node.getLocation().getCoordinates(),
                    dietaryRestrictions
                );
                
                // Build prompt with restaurant suggestions
                String prompt = buildPromptWithRestaurants(node, restaurants, dietaryRestrictions);
                String llmResponse = aiClient.generateStructuredContent(prompt, getSchema(), getSystemPrompt());
                
                // Validate and populate
                if (validateSchema(llmResponse, getSchema())) {
                    populateNode(node, llmResponse);
                    
                    // Convert currency
                    if (node.getCost() != null) {
                        double converted = convertCurrency(
                            node.getCost().getAmountPerPerson(),
                            node.getCost().getCurrency(),
                            itinerary.getCurrency()
                        );
                        node.getCost().setAmountPerPerson(converted);
                    }
                }
            }
        }
        
        itineraryJsonService.updateItineraryWithLock(itinerary);
        logger.info("=== MEAL AGENT COMPLETE ===");
        return (T) itinerary;
    }
}
```

---


### Example 3: EditorAgent with Conflict Detection

```java
@Component
public class EditorAgent extends BaseAgent {
    
    private final RestTemplate restTemplate;
    private final ChangeEngine changeEngine;
    
    @Override
    protected <T> T executeInternal(String itineraryId, AgentRequest<T> request) {
        logger.info("=== EDITOR AGENT START ===");
        
        // Parse edit request
        String userEdit = request.getInput();
        
        // Generate ChangeSet from LLM
        String llmResponse = aiClient.generateStructuredContent(
            buildEditPrompt(userEdit),
            getChangeSetSchema(),
            getSystemPrompt()
        );
        
        // Validate schema
        if (!validateSchema(llmResponse, getChangeSetSchema())) {
            throw new RuntimeException("Invalid ChangeSet from LLM");
        }
        
        ChangeSet changeSet = parseChangeSet(llmResponse);
        
        // Check for conflicts BEFORE applying
        ConflictCheckResult conflicts = checkConflicts(itineraryId, changeSet);
        
        if (conflicts.hasConflicts()) {
            logger.error("Conflicts detected:");
            conflicts.getConflicts().forEach(c -> 
                logger.error("  - {}: {}", c.getType(), c.getMessage())
            );
            
            // Option 1: Reject changes
            throw new RuntimeException("Cannot apply changes due to conflicts");
            
            // Option 2: Ask LLM to resolve
            // String resolution = aiClient.resolveConflicts(conflicts);
        }
        
        // Apply changes
        ChangeEngine.ApplyResult result = changeEngine.apply(itineraryId, changeSet);
        
        // Recalculate cost
        CostCalculationResult cost = calculateCost(itineraryId, null);
        logger.info("Updated cost: {} {}", cost.getTotalCostPerPerson(), cost.getCurrency());
        
        if (cost.getBudgetAnalysis() != null && "OVER".equals(cost.getBudgetAnalysis().getBudgetStatus())) {
            logger.warn("BUDGET EXCEEDED after edit!");
        }
        
        logger.info("=== EDITOR AGENT COMPLETE ===");
        return (T) result;
    }
}
```

---


## Testing Strategy

### Unit Tests

```java
@Test
public void testActivityAgent_UsesNodeIdTool() {
    // Mock tool response
    NodeIdResponse mockResponse = new NodeIdResponse("day1_attraction_001");
    when(restTemplate.postForObject(anyString(), any(), eq(NodeIdResponse.class)))
        .thenReturn(mockResponse);
    
    // Execute agent
    NormalizedItinerary result = activityAgent.execute(itineraryId, request);
    
    // Verify tool was called
    verify(restTemplate).postForObject(
        contains("/generate-node-id"),
        any(NodeIdRequest.class),
        eq(NodeIdResponse.class)
    );
    
    // Verify node has generated ID
    assertEquals("day1_attraction_001", result.getDays().get(0).getNodes().get(0).getId());
}

@Test
public void testMealAgent_ChecksDietaryRestrictions() {
    // Setup itinerary with dietary restrictions
    itinerary.setConstraints(List.of("vegetarian", "gluten-free"));
    
    // Mock constraint check
    ConstraintCheckResult mockConstraints = new ConstraintCheckResult();
    mockConstraints.setValid(true);
    when(restTemplate.postForObject(anyString(), any(), eq(ConstraintCheckResult.class)))
        .thenReturn(mockConstraints);
    
    // Execute agent
    mealAgent.execute(itineraryId, request);
    
    // Verify constraints were checked
    verify(restTemplate).postForObject(
        contains("/check-user-constraints"),
        argThat(req -> ((ConstraintCheckRequest) req).getCheckDietary()),
        eq(ConstraintCheckResult.class)
    );
}
```

### Integration Tests

```java
@SpringBootTest
@AutoConfigureMockMvc
public class ToolIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    public void testGenerateNodeId_ReturnsUniqueId() throws Exception {
        String request = """
            {
                "itineraryId": "test-123",
                "dayNumber": 1,
                "nodeType": "attraction"
            }
            """;
        
        mockMvc.perform(post("/api/v1/tools/generate-node-id")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.nodeId").exists());
    }
    
    @Test
    public void testCalculateCost_ReturnsBudgetAnalysis() throws Exception {
        // Create test itinerary with budget
        String itineraryId = createTestItinerary(1500.0);
        
        String request = String.format("""
            {
                "itineraryId": "%s",
                "partySize": 2,
                "includeBudgetAnalysis": true
            }
            """, itineraryId);
        
        mockMvc.perform(post("/api/v1/tools/calculate-cost")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.budgetAnalysis").exists())
            .andExpect(jsonPath("$.budgetAnalysis.budgetStatus").exists());
    }
}
```

---


## Troubleshooting Guide

### Common Issues

#### Issue 1: Tool Returns Null
**Symptoms:** RestTemplate returns null response  
**Causes:**
- Service not running
- Network error
- Invalid request format

**Solutions:**
```java
// Add null check and fallback
NodeIdResponse response = restTemplate.postForObject(...);
if (response == null || !response.isSuccess()) {
    logger.error("Tool failed, using fallback");
    return nodeIdGenerator.generateNodeId(nodeType, dayNumber, itinerary);
}
```

#### Issue 2: Constraint Check Always Passes
**Symptoms:** No violations detected even when budget exceeded  
**Causes:**
- Itinerary doesn't have budgetMax set
- Costs not populated yet

**Solutions:**
```java
// Check if budget is set
if (itinerary.getBudgetMax() == null || itinerary.getBudgetMax() <= 0) {
    logger.warn("No budget set, skipping budget check");
    return new ConstraintCheckResult(); // Valid by default
}
```

#### Issue 3: Schema Validation Fails
**Symptoms:** LLM response marked as invalid  
**Causes:**
- LLM returned markdown-wrapped JSON
- Extra fields in response
- Missing required fields

**Solutions:**
```java
// Enable cleaning before validation
request.setCleanBeforeValidation(true);

// Log the actual response for debugging
logger.debug("LLM Response: {}", llmResponse);
logger.debug("Expected Schema: {}", schema);
```

#### Issue 4: Currency Conversion Returns Wrong Value
**Symptoms:** Converted amount doesn't match expected  
**Causes:**
- Exchange rate not configured
- Wrong currency code
- INR rate bug (fixed in P0-6)

**Solutions:**
```java
// Verify currency codes
logger.info("Converting {} {} to {}", amount, fromCurrency, toCurrency);

// Check result
if (result.getConvertedAmount() <= 0) {
    logger.error("Invalid conversion result");
    // Use fallback service
}
```

#### Issue 5: Distance Calculation Uses Fallback
**Symptoms:** fromAPI = false in response  
**Causes:**
- Google Maps API key not configured
- API quota exceeded
- Invalid coordinates

**Solutions:**
```java
// Check if using fallback
if (!result.isFromAPI()) {
    logger.warn("Using Haversine fallback - actual time may vary");
    // Add buffer to duration
    int bufferMinutes = (int) (result.getDurationMinutes() * 1.2);
}
```

---


## Best Practices Summary

### 1. Tool Call Order
```
1. Check Constraints (before generation)
2. Generate Node ID (before creating node)
3. Call LLM
4. Validate Schema (after LLM response)
5. Convert Currency (if needed)
6. Calculate Distance (if needed)
7. Check Conflicts (before saving)
8. Calculate Cost (after all nodes populated)
```

### 2. Error Handling Pattern
```java
try {
    Result result = callTool(request);
    if (result == null || !result.isSuccess()) {
        logger.error("Tool failed: {}", result != null ? result.getError() : "null");
        return useFallback();
    }
    return result;
} catch (Exception e) {
    logger.error("Tool exception", e);
    return useFallback();
}
```

### 3. Logging Pattern
```java
// Before tool call
logger.info("Calling {} tool with params: {}", toolName, params);

// After success
logger.info("{} tool succeeded: {}", toolName, summary);

// After failure
logger.error("{} tool failed: {}", toolName, error);

// Fallback
logger.warn("Using fallback for {}", toolName);
```

### 4. Validation Pattern
```java
// Validate request
if (request.getItineraryId() == null) {
    throw new IllegalArgumentException("itineraryId required");
}

// Validate response
if (response == null || !response.isSuccess()) {
    throw new RuntimeException("Tool failed: " + response.getError());
}

// Validate data
if (result.getData() == null || result.getData().isEmpty()) {
    logger.warn("Tool returned empty data");
}
```

---

## Quick Reference Card

### Tool Endpoints
```
POST /api/v1/tools/generate-node-id
POST /api/v1/tools/check-user-constraints
POST /api/v1/tools/convert-currency
POST /api/v1/tools/calculate-cost
POST /api/v1/tools/check-conflicts
POST /api/v1/tools/validate-schema
POST /api/v1/tools/calculate-distance
POST /api/v1/tools/suggest-restaurants

GET  /api/v1/tools/schema/{tool-name}
```

### Required DTOs
```
com.tripplanner.dto.tools.NodeIdRequest/Response
com.tripplanner.dto.tools.ConstraintCheckRequest/Result
com.tripplanner.dto.tools.CostCalculationRequest/Result
com.tripplanner.dto.tools.ConflictCheckRequest/Result
com.tripplanner.dto.tools.SchemaValidationRequest/Result
com.tripplanner.dto.tools.DistanceCalculationRequest/Result
com.tripplanner.dto.tools.RestaurantSuggestionRequest/Result
```

### Dependencies to Inject
```java
private final RestTemplate restTemplate;
private final NodeIdGenerator nodeIdGenerator; // fallback
private final CurrencyConversionService currencyService; // fallback
private final GoogleMapsDistanceService distanceService; // fallback
```

---

## Migration Checklist

For each agent, complete these tasks:

### Phase 1: Setup (Day 1)
- [ ] Add RestTemplate to constructor
- [ ] Add fallback services to constructor
- [ ] Create helper methods for each tool
- [ ] Add error handling and logging

### Phase 2: Integration (Day 2-3)
- [ ] Update executeInternal() to call tools
- [ ] Add schema validation after LLM calls
- [ ] Add constraint checking before generation
- [ ] Add node ID generation for new nodes

### Phase 3: Testing (Day 4)
- [ ] Write unit tests for tool integration
- [ ] Write integration tests with real endpoints
- [ ] Test error scenarios and fallbacks
- [ ] Verify no regressions

### Phase 4: Documentation (Day 5)
- [ ] Update agent documentation
- [ ] Add code comments for tool usage
- [ ] Document any agent-specific patterns
- [ ] Update system architecture docs

---

## Related Documentation

- [CRITICAL_FIXES_ROADMAP.md](./CRITICAL_FIXES_ROADMAP.md) - Implementation roadmap
- [AGENT_TOOL_USAGE_GUIDE.md](./AGENT_TOOL_USAGE_GUIDE.md) - Quick start guide
- [TOOL_CREATION_RULEBOOK.md](./TOOL_CREATION_RULEBOOK.md) - Tool creation patterns
- [tools/AGENT_TOOLS_SPECIFICATION.md](./tools/AGENT_TOOLS_SPECIFICATION.md) - API specs

---

**Status:** ✅ All 8 tools implemented and documented  
**Next Steps:** Begin agent integration following this guide  
**Support:** Check troubleshooting section for common issues

