# Agentic Architecture Strategy: Manual Tool Calling with Java Backend

**Version:** 1.0  
**Last Updated:** 2025-11-25  
**Status:** Strategic Direction Document  
**Purpose:** Define the architectural strategy for implementing robust agentic patterns using manual tool calling with Java Spring backend

---

## Table of Contents

1. [Executive Summary](#executive-summary)
2. [Core Strategy: Agent-as-Client Model](#core-strategy-agent-as-client-model)
3. [Architectural Patterns](#architectural-patterns)
4. [Tool Endpoint Mapping](#tool-endpoint-mapping)
5. [ADK Integration Strategy](#adk-integration-strategy)
6. [Advanced Agentic Patterns](#advanced-agentic-patterns)
7. [Implementation Roadmap](#implementation-roadmap)

---

## 1. Executive Summary

### Strategic Decision

**We are adopting Manual Tool Calling using our Java Spring backend as the foundation for agentic capabilities.**

**Rationale:**
- Leverages existing, battle-tested Java Spring services
- Provides deterministic, reliable execution for critical business logic
- Enables gradual adoption of agentic patterns without full MCP dependency
- Maintains control over validation, security, and data integrity
- Allows hybrid architecture: LLM reasoning + Java reliability

### Key Principles

1. **LLM for Reasoning, Java for Execution:** LLMs determine WHAT to do, Java services execute HOW
2. **Tools as Guardrails:** Every critical operation must go through a validated tool endpoint
3. **Reflection Before Action:** Validate feasibility before applying changes
4. **Stateless Tools:** All tool endpoints must be idempotent and stateless
5. **Graceful Degradation:** System must handle tool failures without cascading

---

## 2. Core Strategy: Agent-as-Client Model

### The Manual Tool Calling Loop

```
┌─────────────────────────────────────────────────────────────┐
│                    AGENTIC REASONING LOOP                    │
└─────────────────────────────────────────────────────────────┘

1. THOUGHT (LLM Reasoning)
   ├─ LLM receives prompt with tool specifications
   ├─ Uses Chain-of-Thought or ReAct reasoning
   └─ Determines which tool(s) to call

2. ACTION (Structured Output)
   ├─ LLM generates JSON function call
   ├─ Specifies: tool name, endpoint, parameters
   └─ Example: {"tool": "calculate-cost", "params": {...}}

3. ORCHESTRATION (Client Execution)
   ├─ Java orchestrator intercepts JSON
   ├─ Validates request structure
   ├─ Makes HTTP/REST call to Java Spring service
   └─ Handles errors and retries

4. EXECUTION (Service Layer)
   ├─ Java Spring service executes business logic
   ├─ Enforces constraints and validation
   ├─ Accesses database and external APIs
   └─ Returns deterministic result

5. OBSERVATION (Result Integration)
   ├─ Result returned to orchestrator
   ├─ Fed back to LLM as observation
   └─ LLM continues reasoning or completes task

6. REFLECTION (Optional)
   ├─ LLM evaluates result quality
   ├─ Checks for conflicts or issues
   └─ Decides: proceed, retry, or abort
```

### Why This Works

**Separation of Concerns:**
- **LLM:** Complex reasoning, natural language understanding, planning
- **Java:** Data integrity, validation, external API integration, transactions

**Reliability:**
- Critical operations (budget tracking, conflict detection) are deterministic
- LLM hallucinations cannot corrupt data
- Validation happens in controlled Java environment

**Flexibility:**
- Can swap LLM providers without changing business logic
- Can add new tools incrementally
- Can test tools independently of LLM

---

## 3. Architectural Patterns

### 3.1 Tool Use Pattern

**Definition:** LLM generates structured function calls that are executed by external tools

**Implementation:**

```java
// Tool Specification (provided to LLM)
{
  "name": "calculate-cost",
  "description": "Calculate total trip cost with budget validation",
  "endpoint": "POST /api/v1/tools/calculate-cost",
  "parameters": {
    "itineraryId": "string (required)",
    "partySize": "integer (required)",
    "budgetTier": "string (enum: budget, medium, luxury)"
  },
  "returns": {
    "totalCost": "number",
    "perPersonCost": "number",
    "currency": "string",
    "budgetStatus": "string (enum: under, within, over)",
    "warnings": "array of strings"
  }
}

// Java Service Implementation
@RestController
@RequestMapping("/api/v1/tools")
public class CostToolsController {
    
    @PostMapping("/calculate-cost")
    public CostCalculationResult calculateCost(@RequestBody CostCalculationRequest request) {
        // 1. Validate request
        validateRequest(request);
        
        // 2. Load itinerary
        NormalizedItinerary itinerary = itineraryService.getItinerary(request.getItineraryId());
        
        // 3. Calculate costs (deterministic)
        CostCalculationResult result = costCalculator.calculate(itinerary, request);
        
        // 4. Check budget constraints
        result.setBudgetStatus(budgetTracker.checkBudget(result, itinerary));
        
        // 5. Generate warnings
        if (result.getBudgetStatus() == BudgetStatus.OVER) {
            result.addWarning("Total cost exceeds budget by " + result.getOverage());
        }
        
        return result;
    }
}
```

### 3.2 Reflection Pattern

**Definition:** Agent evaluates its own output before committing, using validation tools

**Implementation:**

```java
// EditorAgent with Reflection
public class EditorAgent extends BaseAgent {
    
    @Override
    protected <T> T executeInternal(String itineraryId, AgentRequest<T> request) {
        // 1. THOUGHT: Generate proposed changes
        ChangeSet proposedChanges = generateChanges(request);
        
        // 2. REFLECTION: Validate before applying
        ValidationResult validation = callValidationTool(itineraryId, proposedChanges);
        
        if (!validation.isValid()) {
            // 3. REFINEMENT: Fix issues
            logger.warn("Validation failed: {}", validation.getErrors());
            
            // Option A: Retry with refined prompt
            proposedChanges = refineChanges(proposedChanges, validation.getErrors());
            validation = callValidationTool(itineraryId, proposedChanges);
            
            // Option B: Fail gracefully
            if (!validation.isValid()) {
                return createErrorResponse(validation.getErrors());
            }
        }
        
        // 4. ACTION: Apply validated changes
        return applyChanges(itineraryId, proposedChanges);
    }
    
    private ValidationResult callValidationTool(String itineraryId, ChangeSet changes) {
        // Call Java validation tool
        RestTemplate restTemplate = new RestTemplate();
        ValidationRequest request = new ValidationRequest(itineraryId, changes);
        
        return restTemplate.postForObject(
            "http://localhost:8080/api/v1/tools/validate-changes",
            request,
            ValidationResult.class
        );
    }
}
```

### 3.3 Guardrails Pattern

**Definition:** Strict validation of LLM output before accepting it

**Implementation:**

```java
// Schema Validation Tool
@PostMapping("/validate-schema")
public SchemaValidationResult validateSchema(@RequestBody SchemaValidationRequest request) {
    SchemaValidationResult result = new SchemaValidationResult();
    
    try {
        // 1. Parse JSON
        JsonNode jsonNode = objectMapper.readTree(request.getJsonOutput());
        
        // 2. Validate against schema
        JsonSchema schema = schemaFactory.getSchema(request.getSchemaName());
        Set<ValidationMessage> errors = schema.validate(jsonNode);
        
        if (!errors.isEmpty()) {
            result.setValid(false);
            result.setErrors(errors.stream()
                .map(ValidationMessage::getMessage)
                .collect(Collectors.toList()));
            return result;
        }
        
        // 3. Additional business validation
        if ("itinerary".equals(request.getSchemaName())) {
            validateItineraryConstraints(jsonNode, result);
        }
        
        result.setValid(result.getErrors().isEmpty());
        return result;
        
    } catch (Exception e) {
        result.setValid(false);
        result.addError("Schema validation failed: " + e.getMessage());
        return result;
    }
}
```

### 3.4 Planning Pattern

**Definition:** LLM decomposes complex tasks into sequential steps with validation

**Implementation:**

```java
// Multi-City Trip Planning with Validation
public class SkeletonPlannerAgent extends BaseAgent {
    
    @Override
    protected <T> T executeInternal(String itineraryId, AgentRequest<T> request) {
        CreateItineraryReq itineraryReq = extractRequest(request);
        
        // 1. PLANNING: Decompose into steps
        TripPlan plan = generateTripPlan(itineraryReq);
        
        // 2. VALIDATION: Check plan feasibility
        PlanValidationResult validation = callPlanValidationTool(plan);
        
        if (!validation.isFeasible()) {
            // Refine plan based on validation feedback
            plan = refinePlan(plan, validation.getIssues());
        }
        
        // 3. EXECUTION: Generate skeleton based on validated plan
        NormalizedItinerary skeleton = generateSkeleton(itineraryId, plan);
        
        // 4. STRUCTURAL VALIDATION: Ensure structure is correct
        StructureValidationResult structureCheck = callStructureValidationTool(skeleton);
        
        if (!structureCheck.isValid()) {
            throw new ValidationException("Skeleton structure invalid", structureCheck.getErrors());
        }
        
        return (T) skeleton;
    }
    
    private PlanValidationResult callPlanValidationTool(TripPlan plan) {
        // Validate:
        // - Day allocations sum to total days
        // - Travel segments reference valid cities
        // - No impossible travel times
        // - Budget constraints feasible
        
        RestTemplate restTemplate = new RestTemplate();
        return restTemplate.postForObject(
            "http://localhost:8080/api/v1/tools/validate-plan",
            plan,
            PlanValidationResult.class
        );
    }
}
```

---

## 4. Tool Endpoint Mapping

### Critical Tools Required (Priority Order)

| Priority | Tool Name | Endpoint | Agent(s) Using | Critical Fix Addressed |
|----------|-----------|----------|----------------|------------------------|
| **P0** | `validate-itinerary` | `POST /api/v1/tools/validate-itinerary` | All agents | Data integrity, constraint propagation |
| **P0** | `generate-node-id` | `POST /api/v1/tools/generate-node-id` | All agents | Node ID conflicts |
| **P0** | `convert-currency` | `POST /api/v1/tools/convert-currency` | CostEstimator | Currency handling |
| **P0** | `calculate-cost` | `POST /api/v1/tools/calculate-cost` | CostEstimator | Budget tracking |
| **P0** | `check-user-constraints` | `POST /api/v1/tools/check-user-constraints` | All agents | Constraint propagation |
| **P1** | `check-conflicts` | `POST /api/v1/tools/check-conflicts` | Editor | Time conflicts |
| **P1** | `calculate-distance` | `POST /api/v1/tools/calculate-distance` | Transport | Distance calculation |
| **P1** | `transport-cost` | `POST /api/v1/tools/transport-cost` | Transport | Cost estimation |
| **P1** | `suggest-restaurants` | `POST /api/v1/tools/suggest-restaurants` | Meal | Dietary restrictions |
| **P1** | `validate-schema` | `POST /api/v1/tools/validate-schema` | Orchestrator | JSON validation |
| **P2** | `itinerary-summary` | `GET /api/v1/tools/itinerary-summary/{id}` | Router | Context retrieval |
| **P2** | `day-details` | `GET /api/v1/tools/day-details/{id}/{day}` | Router | Context retrieval |

### Tool Specification Template

```json
{
  "name": "tool-name",
  "version": "1.0",
  "description": "Clear description of what the tool does",
  "endpoint": "POST /api/v1/tools/tool-name",
  "authentication": "Bearer token required",
  "parameters": {
    "param1": {
      "type": "string",
      "required": true,
      "description": "Parameter description",
      "validation": "regex or constraints"
    }
  },
  "returns": {
    "field1": {
      "type": "string",
      "description": "Return field description"
    }
  },
  "errors": [
    {
      "code": "VALIDATION_ERROR",
      "message": "Description of error condition"
    }
  ],
  "examples": [
    {
      "request": {},
      "response": {}
    }
  ]
}
```

---

## 5. ADK Integration Strategy

### 5.1 Local ADK Agent Setup

**Architecture:**

```
┌─────────────────────────────────────────────────────────┐
│                    ADK Agent (Python)                    │
│  ┌────────────────────────────────────────────────┐    │
│  │  Agent Definition (ReAct/CoT)                  │    │
│  │  - Reasoning loop                              │    │
│  │  - Tool specifications                         │    │
│  │  - Prompt templates                            │    │
│  └────────────────────────────────────────────────┘    │
│                         │                               │
│                         ▼                               │
│  ┌────────────────────────────────────────────────┐    │
│  │  ADK HTTP Client                               │    │
│  │  - Makes REST calls to Java backend           │    │
│  │  - Handles authentication                      │    │
│  │  - Manages retries                             │    │
│  └────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────┘
                         │
                         │ HTTP/REST
                         ▼
┌─────────────────────────────────────────────────────────┐
│              Java Spring Backend (Port 8080)             │
│  ┌────────────────────────────────────────────────┐    │
│  │  Tool Endpoints (/api/v1/tools/*)              │    │
│  │  - calculate-cost                              │    │
│  │  - validate-itinerary                          │    │
│  │  - check-conflicts                             │    │
│  │  - etc.                                        │    │
│  └────────────────────────────────────────────────┘    │
│                         │                               │
│                         ▼                               │
│  ┌────────────────────────────────────────────────┐    │
│  │  Business Logic Services                       │    │
│  │  - CostEstimatorService                        │    │
│  │  - ValidationService                           │    │
│  │  - ConflictDetectionService                    │    │
│  └────────────────────────────────────────────────┘    │
│                         │                               │
│                         ▼                               │
│  ┌────────────────────────────────────────────────┐    │
│  │  Firestore Database                            │    │
│  └────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────┘
```

### 5.2 ADK Agent Configuration

**Python ADK Agent Example:**

```python
# adk_agent_config.py
from adk import Agent, Tool, HttpServerParameters

# Define tools pointing to Java backend
calculate_cost_tool = Tool(
    name="calculate_cost",
    description="Calculate total trip cost with budget validation",
    endpoint="http://localhost:8080/api/v1/tools/calculate-cost",
    method="POST",
    parameters={
        "itineraryId": {"type": "string", "required": True},
        "partySize": {"type": "integer", "required": True},
        "budgetTier": {"type": "string", "enum": ["budget", "medium", "luxury"]}
    }
)

validate_itinerary_tool = Tool(
    name="validate_itinerary",
    description="Validate itinerary structure and constraints",
    endpoint="http://localhost:8080/api/v1/tools/validate-itinerary",
    method="POST",
    parameters={
        "itineraryId": {"type": "string", "required": True}
    }
)

# Create ADK agent with tools
cost_estimator_agent = Agent(
    name="CostEstimatorAgent",
    description="Estimates costs for trip itineraries",
    tools=[calculate_cost_tool, validate_itinerary_tool],
    reasoning_strategy="react",  # ReAct pattern
    connection=HttpServerParameters(
        base_url="http://localhost:8080",
        auth_token="Bearer ${API_TOKEN}"
    )
)

# Agent execution
def estimate_costs(itinerary_id: str, party_size: int, budget_tier: str):
    """
    Agent will:
    1. Reason about what data it needs
    2. Call calculate_cost tool
    3. Evaluate result
    4. Call validate_itinerary tool if needed
    5. Return final result
    """
    result = cost_estimator_agent.run(
        task=f"Estimate costs for itinerary {itinerary_id} with {party_size} people at {budget_tier} tier",
        context={
            "itineraryId": itinerary_id,
            "partySize": party_size,
            "budgetTier": budget_tier
        }
    )
    return result
```

### 5.3 Hybrid Execution Model

**Option 1: Full ADK (Future State)**
- All agents run as ADK agents in Python
- Java backend provides only tool endpoints
- ADK handles reasoning, orchestration, state management

**Option 2: Hybrid (Current Recommended)**
- Critical agents (Editor, Planner) remain in Java
- New experimental agents use ADK
- Shared tool endpoints
- Gradual migration path

**Option 3: Java-Only with Manual Tool Calling (Current State)**
- All agents in Java
- Manual tool calling within Java agents
- No ADK dependency
- Full control and reliability

---

## 6. Advanced Agentic Patterns

### 6.1 Routing Pattern

**Purpose:** Efficiently direct requests to appropriate specialized agents

**Implementation:**

```java
@Service
public class RouterAgent {
    
    private final Map<String, BaseAgent> agentRegistry;
    private final LLMService llmService;
    
    public ChatResponse route(ChatRequest request) {
        // 1. Get context via tools
        String summary = callItinerarySummaryTool(request.getItineraryId());
        
        // 2. LLM-based routing decision
        RoutingDecision decision = llmService.classifyIntent(
            request.getText(),
            summary,
            getAvailableAgents()
        );
        
        // 3. Route to appropriate agent
        BaseAgent targetAgent = agentRegistry.get(decision.getAgentName());
        
        if (targetAgent == null) {
            return ChatResponse.error("No suitable agent found");
        }
        
        // 4. Execute agent
        return targetAgent.execute(request.getItineraryId(), 
            new AgentRequest<>(request, ChatResponse.class));
    }
    
    private String callItinerarySummaryTool(String itineraryId) {
        RestTemplate restTemplate = new RestTemplate();
        return restTemplate.getForObject(
            "http://localhost:8080/api/v1/tools/itinerary-summary/" + itineraryId,
            String.class
        );
    }
}
```

### 6.2 Context Engineering

**Purpose:** Provide agents with relevant, focused context to improve reasoning

**Implementation:**

```java
@RestController
@RequestMapping("/api/v1/tools")
public class ContextToolsController {
    
    @GetMapping("/itinerary-summary/{itineraryId}")
    public ItinerarySummary getItinerarySummary(@PathVariable String itineraryId) {
        NormalizedItinerary itinerary = itineraryService.getItinerary(itineraryId);
        
        return ItinerarySummary.builder()
            .destination(itinerary.getDestination())
            .duration(itinerary.getDays().size())
            .totalCost(calculateTotalCost(itinerary))
            .currency(itinerary.getCurrency())
            .dayCount(itinerary.getDays().size())
            .nodeCount(countNodes(itinerary))
            .constraints(itinerary.getConstraints())
            .budgetStatus(getBudgetStatus(itinerary))
            .build();
    }
    
    @GetMapping("/day-details/{itineraryId}/{dayNumber}")
    public DayDetails getDayDetails(
            @PathVariable String itineraryId,
            @PathVariable Integer dayNumber) {
        
        NormalizedDay day = itineraryService.getDay(itineraryId, dayNumber);
        
        return DayDetails.builder()
            .dayNumber(day.getDayNumber())
            .location(day.getLocation())
            .nodes(day.getNodes().stream()
                .map(this::summarizeNode)
                .collect(Collectors.toList()))
            .totalCost(day.getTotalCost())
            .startTime(getFirstNodeTime(day))
            .endTime(getLastNodeTime(day))
            .build();
    }
}
```

### 6.3 Goal Setting and Monitoring

**Purpose:** Define success metrics and track agent performance

**Implementation:**

```java
@Service
public class AgentMonitoringService {
    
    private final MetricsTracker metricsTracker;
    
    public void trackAgentExecution(String agentName, AgentExecutionContext context) {
        long startTime = System.currentTimeMillis();
        boolean success = false;
        String errorMessage = null;
        
        try {
            // Execute agent
            context.execute();
            success = true;
            
            // Validate result against goals
            ValidationResult validation = validateAgainstGoals(context);
            
            if (!validation.isValid()) {
                success = false;
                errorMessage = "Goal validation failed: " + validation.getErrors();
            }
            
        } catch (Exception e) {
            errorMessage = e.getMessage();
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            
            // Track metrics
            metricsTracker.trackAgentExecution(
                agentName,
                duration,
                success,
                errorMessage,
                context.getMetadata()
            );
        }
    }
    
    private ValidationResult validateAgainstGoals(AgentExecutionContext context) {
        ValidationResult result = new ValidationResult();
        
        // Goal 1: Constraint compliance
        if (!checkConstraintCompliance(context)) {
            result.addError("User constraints not satisfied");
        }
        
        // Goal 2: Budget adherence
        if (!checkBudgetAdherence(context)) {
            result.addError("Budget exceeded");
        }
        
        // Goal 3: Data integrity
        if (!checkDataIntegrity(context)) {
            result.addError("Data integrity violation");
        }
        
        return result;
    }
}
```

---

## 7. Implementation Roadmap

### Phase 1: Foundation (Weeks 1-2)

**Objective:** Implement critical P0 tools and fix data integrity issues

**Deliverables:**
1. ✅ Tool endpoint infrastructure (`/api/v1/tools/*`)
2. ✅ `generate-node-id` tool (P0)
3. ✅ `validate-itinerary` tool (P0)
4. ✅ `check-user-constraints` tool (P0)
5. ✅ Fix Unicode handling (P0)
6. ✅ Implement transaction rollback in ChangeEngine (P0)

**Success Criteria:**
- No node ID conflicts
- All user constraints propagated
- Unicode characters preserved
- Failed edits don't corrupt data

### Phase 2: Budget & Cost (Weeks 3-4)

**Objective:** Implement robust budget tracking and cost estimation

**Deliverables:**
1. ✅ `convert-currency` tool (P0)
2. ✅ `calculate-cost` tool (P0)
3. ✅ Budget warning system
4. ✅ Per-person vs total cost standardization
5. ✅ CostEstimatorAgent refactored to use tools

**Success Criteria:**
- Accurate currency conversion
- Budget warnings displayed
- No cost calculation errors
- All costs in per-person format

### Phase 3: Validation & Reflection (Weeks 5-6)

**Objective:** Implement reflection pattern and validation tools

**Deliverables:**
1. ✅ `check-conflicts` tool (P1)
2. ✅ `validate-schema` tool (P1)
3. ✅ EditorAgent with reflection
4. ✅ Conflict detection service
5. ✅ Schema validation service

**Success Criteria:**
- Time conflicts detected before applying
- Invalid JSON rejected
- EditorAgent validates before committing
- Graceful error messages

### Phase 4: Transport & Geography (Weeks 7-8)

**Objective:** Implement geography-aware transport planning

**Deliverables:**
1. ✅ `calculate-distance` tool (P1)
2. ✅ `transport-cost` tool (P1)
3. ✅ Google Maps Distance Matrix integration
4. ✅ TransportAgent refactored to use tools
5. ✅ Geography validation

**Success Criteria:**
- Accurate distance calculations
- Realistic transport costs
- Island geography handled correctly
- No impossible routes

### Phase 5: Dietary & Preferences (Weeks 9-10)

**Objective:** Implement constraint-aware meal planning

**Deliverables:**
1. ✅ `suggest-restaurants` tool (P1)
2. ✅ `user-preferences` tool
3. ✅ Dietary restriction filtering
4. ✅ MealAgent refactored to use tools
5. ✅ Restaurant existence validation

**Success Criteria:**
- Dietary restrictions respected
- No invalid restaurant suggestions
- Preferences propagated correctly
- Variety in meal suggestions

### Phase 6: ADK Integration (Weeks 11-12)

**Objective:** Set up ADK agents with tool calling

**Deliverables:**
1. ✅ ADK agent configuration
2. ✅ Tool specifications for ADK
3. ✅ Hybrid execution model
4. ✅ A/B testing framework
5. ✅ Performance comparison

**Success Criteria:**
- ADK agents can call Java tools
- Performance comparable to Java agents
- Successful A/B test results
- Migration path defined

### Phase 7: Advanced Patterns (Weeks 13-14)

**Objective:** Implement advanced agentic patterns

**Deliverables:**
1. ✅ Routing agent with context tools
2. ✅ Goal setting and monitoring
3. ✅ Circuit breakers for external APIs
4. ✅ Fallback mechanisms
5. ✅ Comprehensive metrics

**Success Criteria:**
- Efficient request routing
- Goal compliance tracked
- No cascading failures
- Graceful degradation working
- Metrics dashboard operational

---

## Conclusion

This strategy provides a clear path to implementing robust agentic capabilities while maintaining the reliability and control of our Java Spring backend. By treating Java services as tools that LLMs can call, we get the best of both worlds: sophisticated reasoning from LLMs and deterministic execution from Java.

The manual tool calling approach is not a limitation—it's a strategic advantage that gives us:
- **Control:** We decide what tools exist and how they behave
- **Reliability:** Critical operations are deterministic
- **Flexibility:** Can adopt ADK gradually without rewriting everything
- **Safety:** Guardrails prevent LLM hallucinations from corrupting data

This is the foundation for a production-grade agentic system.
