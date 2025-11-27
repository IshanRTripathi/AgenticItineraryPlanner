# Tool Creation Rulebook for Trip Planner System

**Version:** 1.0  
**Last Updated:** 2025-11-25  
**Purpose:** Comprehensive guide for creating tools, agents, and services that integrate with the ADK (Agent Development Kit) and enable cross-agent communication

---

## Table of Contents

1. [System Overview](#system-overview)
2. [Architecture Patterns](#architecture-patterns)
3. [Agent Design Principles](#agent-design-principles)
4. [Tool Creation Guidelines](#tool-creation-guidelines)
5. [Data Flow & Communication](#data-flow--communication)
6. [Integration Patterns](#integration-patterns)
7. [Best Practices](#best-practices)

---

## 1. System Overview

### Current System Status

**Architecture:** Multi-agent pipeline with specialized agents for different aspects of itinerary generation

**Core Components:**
- **Agents:** 14 specialized agents (Skeleton, Activity, Meal, Transport, Enrichment, etc.)
- **Services:** 50+ service classes handling business logic
- **DTOs:** 100+ data transfer objects for structured communication
- **Pipeline:** Orchestrated multi-stage generation process

**Technology Stack:**
- **Backend:** Spring Boot (Java)
- **LLM Integration:** Gemini AI (via AiClient abstraction)
- **Database:** Firestore (JSON-based storage)
- **Real-time:** WebSocket for progress updates
- **Frontend:** React with TypeScript

### Agent Ecosystem

```
Pipeline Flow:
1. CityAllocationAgent → Determines cities and day allocation
2. SkeletonPlannerAgent → Creates day structure with placeholder nodes
3. Population Phase (Parallel):
   - ActivityAgent → Populates attraction nodes
   - MealAgent → Populates meal nodes
   - TransportAgent → Populates transport nodes
4. EnrichmentAgent → Adds Google Places data (photos, ratings, etc.)
5. CostEstimatorAgent → Adds cost estimates
6. Finalization → Validation and completion
```

**Chat-Enabled Agents:**
- EditorAgent (edit operations)
- ExplainAgent (Q&A)
- BookingAgent (reservations)

**Pipeline-Only Agents:**
- SkeletonPlannerAgent
- ActivityAgent
- MealAgent
- TransportAgent
- CostEstimatorAgent
- EnrichmentAgent

---

## 2. Architecture Patterns

### 2.1 Agent Architecture

**Base Agent Pattern:**
```java
public abstract class BaseAgent {
    // Core methods every agent must implement
    public abstract AgentCapabilities getCapabilities();
    protected abstract <T> T executeInternal(String itineraryId, AgentRequest<T> request);
    protected abstract String getAgentName();
    
    // Execution wrapper with validation and event emission
    public final <T> T execute(String itineraryId, AgentRequest<T> request);
}
```

**Key Principles:**
1. **Single Responsibility:** Each agent handles ONE specific task type
2. **Capability Declaration:** Agents declare what they can do via `getCapabilities()`
3. **Task-Based Routing:** Agents are selected based on task type, not hardcoded
4. **Event-Driven:** Agents emit progress events for real-time UI updates

### 2.2 Service Layer Pattern

**Service Responsibilities:**
- Business logic implementation
- External API integration (Google Places, etc.)
- Data transformation and validation
- Metadata population (reducing LLM dependency)

**Example Service Pattern:**
```java
@Service
public class MealMetadataService {
    // Populate metadata without LLM calls
    public MealMetadata populateMetadata(String title, String startTime, String budgetTier);
    
    // Apply metadata to nodes
    public void populateNodeMetadata(NormalizedNode node, String budgetTier);
}
```

### 2.3 Data Model

**Core Data Structures:**

```java
// Main itinerary structure
NormalizedItinerary {
    String itineraryId;
    Integer version;
    Long lockVersion;  // Optimistic locking
    List<NormalizedDay> days;
    Map<String, AgentDataSection> agentData;  // Agent-specific data
    WorkflowData workflow;
    List<RevisionRecord> revisions;
    List<ChatRecord> chat;
}

// Day structure
NormalizedDay {
    Integer dayNumber;
    String location;
    List<NormalizedNode> nodes;
    Double totalCost;
}

// Node structure (unified for all types)
NormalizedNode {
    String id;
    String type;  // "attraction", "meal", "transport", "accommodation"
    String title;
    NodeLocation location;
    NodeTiming timing;
    NodeCost cost;
    NodeDetails details;
    NodeMetadata metadata;  // Type-specific metadata
    ProcessingState processingState;
    List<String> processedBy;  // Agent tracking
}
```

---

## 3. Agent Design Principles

### 3.1 Agent Capabilities

**Every agent MUST declare capabilities:**

```java
@Override
public AgentCapabilities getCapabilities() {
    AgentCapabilities capabilities = new AgentCapabilities();
    
    // Task types this agent handles
    capabilities.addSupportedTask("populate_attractions");
    
    // Priority (lower = higher priority)
    capabilities.setPriority(10);
    
    // Chat enablement
    capabilities.setChatEnabled(false);  // Pipeline-only
    
    // Configuration
    capabilities.setConfigurationValue("nodeType", "attraction");
    capabilities.setConfigurationValue("parallel", true);
    
    return capabilities;
}
```

**Task Types:**
- `skeleton` - Create day structure
- `populate_attractions` - Fill attraction details
- `populate_meals` - Fill meal details
- `populate_transport` - Fill transport details
- `enrich` - Add external data
- `estimate_costs` - Add pricing
- `edit` - Modify itinerary
- `explain` - Answer questions
- `book` - Make reservations

### 3.2 Agent Execution Pattern

**Standard Execution Flow:**

```java
@Override
protected <T> T executeInternal(String itineraryId, AgentRequest<T> request) {
    logger.info("=== AGENT NAME ===");
    
    try {
        // 1. Load itinerary
        Optional<NormalizedItinerary> itineraryOpt = itineraryJsonService.getItinerary(itineraryId);
        NormalizedItinerary itinerary = itineraryOpt.orElseThrow();
        
        // 2. Extract relevant nodes
        List<NodeContext> contexts = extractNodes(itinerary);
        
        // 3. Process with LLM (if needed)
        List<PopulatedData> results = processWithAI(contexts);
        
        // 4. Update itinerary
        updateItinerary(itineraryId, itinerary, results);
        
        // 5. Return result
        return (T) itinerary;
        
    } catch (Exception e) {
        logger.error("Agent failed", e);
        throw new RuntimeException("Agent execution failed", e);
    }
}
```

### 3.3 Node Processing State Machine

**Processing States:**
```java
public enum ProcessingState {
    CREATED,      // Node created by skeleton
    ENRICHING,    // Being processed by agent
    ENRICHED,     // Successfully processed
    VALIDATED,    // Passed validation
    FAILED        // Processing failed
}
```

**State Tracking:**
```java
// Mark node as being processed
node.setProcessingState(ProcessingState.ENRICHING);
node.addProcessedBy("ActivityAgent");

// Mark as complete
node.setProcessingState(ProcessingState.ENRICHED);

// Track errors
node.setProcessingState(ProcessingState.FAILED);
node.setLastError("Error message");
```

---

## 4. Tool Creation Guidelines

### 4.1 Creating a New Agent

**Step 1: Define Agent Class**

```java
@Component
@ConditionalOnBean(AiClient.class)
public class MyNewAgent extends BaseAgent {
    
    private final AiClient aiClient;
    private final ItineraryJsonService itineraryJsonService;
    private final AgentEventPublisher agentEventPublisher;
    
    public MyNewAgent(AgentEventBus eventBus, AiClient aiClient, 
                      ItineraryJsonService itineraryJsonService,
                      AgentEventPublisher agentEventPublisher) {
        super(eventBus, AgentEvent.AgentKind.ENRICHMENT);
        this.aiClient = aiClient;
        this.itineraryJsonService = itineraryJsonService;
        this.agentEventPublisher = agentEventPublisher;
    }
    
    @Override
    public AgentCapabilities getCapabilities() {
        AgentCapabilities capabilities = new AgentCapabilities();
        capabilities.addSupportedTask("my_task");
        capabilities.setPriority(20);
        capabilities.setChatEnabled(false);
        return capabilities;
    }
    
    @Override
    protected <T> T executeInternal(String itineraryId, AgentRequest<T> request) {
        // Implementation
    }
    
    @Override
    protected String getAgentName() {
        return "My New Agent";
    }
}
```

**Step 2: Register with AgentRegistry**

The agent is automatically registered via Spring's component scanning. Ensure:
- Class is annotated with `@Component`
- Extends `BaseAgent`
- Implements required methods

**Step 3: Add to Pipeline (if needed)**

```java
// In PipelineOrchestrator
private void executeMyNewPhase(String itineraryId, NormalizedItinerary itinerary) {
    agentCoordinator.executeWithLock(itineraryId, "MyNewAgent", () -> {
        myNewAgent.execute(itineraryId, request);
    });
}
```

### 4.2 Creating a Metadata Service

**Purpose:** Reduce LLM dependency by providing pre-computed metadata

**Pattern:**

```java
@Service
public class MyMetadataService {
    
    private static final Logger logger = LoggerFactory.getLogger(MyMetadataService.class);
    
    /**
     * Populate metadata for a node
     */
    public void populateNodeMetadata(NormalizedNode node) {
        if (!"my_type".equals(node.getType())) {
            return;
        }
        
        MyMetadata metadata = new MyMetadata();
        
        // Infer from title
        metadata.setCategory(inferCategory(node.getTitle()));
        
        // Set defaults
        metadata.setDefaultValue(getDefault());
        
        // Apply to node
        node.setMetadata(metadata);
    }
    
    private String inferCategory(String title) {
        // Logic to infer from keywords
    }
}
```

### 4.3 Creating a DTO

**Naming Convention:**
- Request DTOs: `*Request` or `*Req`
- Response DTOs: `*Response` or `*Res`
- Metadata DTOs: `*Metadata`
- Data DTOs: `*Dto`

**Example:**

```java
@JsonIgnoreProperties(ignoreUnknown = true)
public class MyMetadata extends NodeMetadata {
    
    @JsonProperty("category")
    private String category;
    
    @JsonProperty("estimatedDuration")
    private Integer estimatedDuration;
    
    // Getters and setters
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
}
```

---

## 5. Data Flow & Communication

### 5.1 Agent-to-Agent Communication

**Pattern 1: Via Itinerary AgentData**

```java
// Agent A stores data
if (itinerary.getAgentData() == null) {
    itinerary.setAgentData(new HashMap<>());
}
AgentDataSection section = itinerary.getAgentData().computeIfAbsent(
    "myData", k -> new AgentDataSection()
);
section.setAgentData("myData", myDataObject);

// Agent B reads data
AgentDataSection section = itinerary.getAgentData().get("myData");
MyData data = section.getAgentData("myData", MyData.class);
```

**Pattern 2: Via Node Metadata**

```java
// Agent A adds metadata
MyMetadata metadata = new MyMetadata();
metadata.setInfo("value");
node.setMetadata(metadata);

// Agent B reads metadata
MyMetadata metadata = (MyMetadata) node.getMetadata();
String info = metadata.getInfo();
```

**Pattern 3: Via Processing State**

```java
// Check if node was processed by specific agent
if (node.getProcessedBy().contains("ActivityAgent")) {
    // Node has activity data
}

// Check processing state
if (node.getProcessingState() == ProcessingState.ENRICHED) {
    // Node is ready for next stage
}
```

### 5.2 LLM Integration

**Standard LLM Call Pattern:**

```java
// 1. Build prompts
String systemPrompt = buildSystemPrompt();
String userPrompt = buildUserPrompt(context);
String schema = buildJsonSchema();

// 2. Call LLM
String response = aiClient.generateStructuredContent(userPrompt, schema, systemPrompt);

// 3. Validate response
LLMSchemaValidator.ValidationResult validationResult = 
    schemaValidator.validateWithLogging(response, schema, "AgentName");

if (!validationResult.isValid()) {
    // Handle validation failure
}

// 4. Parse response
MyResponse parsed = objectMapper.treeToValue(validationResult.getData(), MyResponse.class);
```

**Prompt Engineering Guidelines:**

1. **System Prompt:** Define role and capabilities
2. **User Prompt:** Provide context and specific request
3. **JSON Schema:** Define expected output structure
4. **Examples:** Include 2-3 examples in prompt
5. **Constraints:** Clearly state rules and limitations

### 5.3 Real-Time Updates

**WebSocket Event Pattern:**

```java
// Emit progress
emitProgress(itineraryId, 50, "Processing nodes", "processing");

// Publish completion
if (agentEventPublisher.hasActiveConnections(itineraryId)) {
    agentEventPublisher.publishAgentComplete(itineraryId, execId, 
        "AgentName", processedCount);
}

// Publish day completion
agentEventPublisher.publishDayCompleted(itineraryId, execId, day);
```

---

## 6. Integration Patterns

### 6.1 ADK Integration (Future)

**Current Status:** REST API tools created for ADK integration

**Tool Endpoint Pattern:**

```java
@RestController
@RequestMapping("/api/tools")
public class ToolsController {
    
    @PostMapping("/calculate-cost")
    public CostEstimateRes calculateCost(@RequestBody CostEstimateReq request) {
        // Tool implementation
    }
}
```

**ADK Tool Specification:**

```json
{
  "name": "calculate-cost",
  "description": "Calculate estimated cost for a trip",
  "parameters": {
    "destination": "string",
    "duration": "number",
    "budgetTier": "string"
  },
  "endpoint": "/api/tools/calculate-cost"
}
```

### 6.2 External API Integration

**Google Places Integration:**

```java
@Service
public class GooglePlacesService {
    
    public PlaceDetails getPlaceDetails(String placeId) {
        // Call Google Places API
    }
    
    public List<PlaceSearchResult> searchPlaces(String query, String location) {
        // Search places
    }
}
```

**Pattern:**
1. Create service class for external API
2. Handle authentication and rate limiting
3. Transform external data to internal DTOs
4. Cache results when appropriate

### 6.3 Database Patterns

**Firestore Operations:**

```java
// Read
Optional<NormalizedItinerary> itinerary = itineraryJsonService.getItinerary(itineraryId);

// Write with optimistic locking
itinerary.setUpdatedAt(System.currentTimeMillis());
itineraryJsonService.updateItineraryWithLock(itinerary);

// Retry on concurrent modification
int maxRetries = 3;
for (int i = 0; i < maxRetries; i++) {
    try {
        itineraryJsonService.updateItineraryWithLock(itinerary);
        break;
    } catch (ConcurrentModificationException e) {
        // Reload and retry
        itinerary = itineraryJsonService.getItinerary(itineraryId).get();
    }
}
```

---

## 7. Best Practices

### 7.1 Error Handling

**Agent-Level:**
```java
try {
    // Agent logic
} catch (Exception e) {
    logger.error("Agent failed", e);
    node.setProcessingState(ProcessingState.FAILED);
    node.setLastError(e.getMessage());
    // Don't throw - allow graceful degradation
}
```

**Service-Level:**
```java
try {
    // Service logic
} catch (RetryableException e) {
    // Retry with backoff
} catch (PermanentException e) {
    // Log and fail
    throw new RuntimeException("Permanent failure", e);
}
```

### 7.2 Validation

**Itinerary Validation:**
```java
ItineraryValidator.ValidationResult result = itineraryValidator.validate(itinerary);

if (!result.isValid()) {
    logger.error("Validation failed: {}", result.getErrors());
    throw new ValidationException("Invalid itinerary", result.getErrors());
}

if (!result.getWarnings().isEmpty()) {
    logger.warn("Validation warnings: {}", result.getWarnings());
}
```

**Node ID Validation:**
```java
// Ensure all nodes have valid IDs
for (NormalizedDay day : itinerary.getDays()) {
    for (NormalizedNode node : day.getNodes()) {
        nodeIdGenerator.ensureNodeHasId(node, day.getDayNumber(), itinerary);
    }
}
```

### 7.3 Performance Optimization

**Parallel Processing:**
```java
// Run independent agents in parallel
CompletableFuture<Void> activityFuture = CompletableFuture.runAsync(() -> 
    activityAgent.populateAttractions(itineraryId, skeleton), executor);

CompletableFuture<Void> mealFuture = CompletableFuture.runAsync(() -> 
    mealAgent.populateMeals(itineraryId, skeleton), executor);

CompletableFuture.allOf(activityFuture, mealFuture).get();
```

**Batch Processing:**
```java
// Process nodes in batches
int batchSize = 10;
for (int i = 0; i < nodes.size(); i += batchSize) {
    List<NormalizedNode> batch = nodes.subList(i, Math.min(i + batchSize, nodes.size()));
    processBatch(batch);
}
```

### 7.4 Logging

**Structured Logging:**
```java
logger.info("=== AGENT NAME ===");
logger.info("Processing {} nodes for itinerary: {}", nodeCount, itineraryId);
logger.debug("Node details: {}", node);
logger.warn("Potential issue: {}", issue);
logger.error("Failed to process: {}", error, exception);
logger.info("=== AGENT COMPLETE ===");
```

### 7.5 Testing

**Agent Testing Pattern:**
```java
@Test
public void testAgentExecution() {
    // Arrange
    String itineraryId = "test-123";
    NormalizedItinerary itinerary = createTestItinerary();
    AgentRequest<NormalizedItinerary> request = new AgentRequest<>(itinerary, NormalizedItinerary.class);
    
    // Act
    NormalizedItinerary result = agent.execute(itineraryId, request);
    
    // Assert
    assertNotNull(result);
    assertEquals(ProcessingState.ENRICHED, result.getDays().get(0).getNodes().get(0).getProcessingState());
}
```

---

## Summary

This rulebook provides the foundation for creating tools and agents that integrate seamlessly with the Trip Planner system. Key takeaways:

1. **Follow the Agent Pattern:** Extend BaseAgent, declare capabilities, implement executeInternal
2. **Use Metadata Services:** Reduce LLM dependency with pre-computed metadata
3. **Communicate via Data Structures:** Use agentData, node metadata, and processing states
4. **Handle Errors Gracefully:** Allow degradation, don't fail the entire pipeline
5. **Emit Progress Events:** Keep UI updated in real-time
6. **Validate Everything:** Use ItineraryValidator and schema validation
7. **Optimize for Performance:** Use parallel processing and batching
8. **Log Comprehensively:** Structured logging for debugging

For ADK integration, follow the REST API tool pattern and ensure tools are stateless and idempotent.

---

## Related Documentation

**Strategic Planning:**
- **[AGENTIC_ARCHITECTURE_STRATEGY.md](./AGENTIC_ARCHITECTURE_STRATEGY.md)** - Comprehensive strategy for implementing manual tool calling with Java backend, including ADK integration and advanced agentic patterns

**Implementation Roadmap:**
- **[CRITICAL_FIXES_ROADMAP.md](./CRITICAL_FIXES_ROADMAP.md)** - Prioritized list of critical fixes (P0, P1, P2) with detailed implementation plans and effort estimates

**Tool Specifications:**
- **[AGENT_TOOLS_SPECIFICATION.md](./tools/AGENT_TOOLS_SPECIFICATION.md)** - Detailed specifications for all tool endpoints

**Analysis Documents:**
- **[AGENT_RESPONSE_ANALYSIS.md](../AGENT_RESPONSE_ANALYSIS.md)** - Analysis of agent response patterns and issues
- **[AGENT_METADATA_ANALYSIS.md](../AGENT_METADATA_ANALYSIS.md)** - Analysis of metadata usage across agents

These documents work together to provide a complete picture of the system architecture, current issues, and the path forward for implementing robust agentic capabilities.
