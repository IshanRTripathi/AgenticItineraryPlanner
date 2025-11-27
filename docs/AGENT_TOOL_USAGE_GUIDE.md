# Agent Tool Usage Guide

**Complete Reference for Integrating All Tools into Agents**

**Last Updated:** 2025-11-26  
**Status:** 16 tools implemented + 6 enhanced (88% complete)  
**Coverage:** P0 + P1 + P2 tools

---

## Overview

This guide provides comprehensive information for integrating all implemented tools into the agent pipeline.

**Core Tools (P0/P1):**
1. ✅ Generate Node ID
2. ✅ Check User Constraints
3. ✅ Convert Currency
4. ✅ Calculate Cost
5. ✅ Check Conflicts
6. ✅ Validate Schema
7. ✅ Calculate Distance
8. ✅ Suggest Restaurants (ENHANCED with dietary verification)

**New/Enhanced Tools (P2):**
9. ✅ Suggest Best Time (NEW - Weather-based scheduling)
10. ✅ Check Opening Hours (ENHANCED - Full weekday parsing)
11. ✅ Check Capacity (ENHANCED - Google Places data)
12. ✅ Get Transport Options (ENHANCED - Distance-based pricing)
13. ✅ Get Weather (Fully functional - schema fixed)
14. ✅ Validate Timing
15. ✅ Optimize Route
16. ✅ Get Itinerary Summary

---

## Tool 1: Generate Node ID

**When to use:** BEFORE creating any new node

**Purpose:** Prevent node ID conflicts by using centralized ID generation

### Code Pattern

```java
import org.springframework.web.client.RestTemplate;
import com.tripplanner.dto.tools.NodeIdRequest;
import com.tripplanner.dto.tools.NodeIdResponse;

public class MyAgent extends BaseAgent {
    
    private final RestTemplate restTemplate = new RestTemplate();
    
    private String generateNodeId(String itineraryId, Integer dayNumber, String nodeType) {
        NodeIdRequest request = new NodeIdRequest(itineraryId, dayNumber, nodeType);
        
        try {
            NodeIdResponse response = restTemplate.postForObject(
                "http://localhost:8080/api/v1/tools/generate-node-id",
                request,
                NodeIdResponse.class
            );
            
            if (response != null && response.isSuccess()) {
                return response.getNodeId();
            } else {
                logger.error("Failed to generate node ID: {}", 
                           response != null ? response.getError() : "null response");
                // Fallback to legacy method
                return nodeIdGenerator.generateNodeId(nodeType, dayNumber);
            }
        } catch (Exception e) {
            logger.error("Error calling node ID generation tool", e);
            // Fallback to legacy method
            return nodeIdGenerator.generateNodeId(nodeType, dayNumber);
        }
    }
    
    @Override
    protected <T> T executeInternal(String itineraryId, AgentRequest<T> request) {
        // Generate ID before creating node
        String nodeId = generateNodeId(itineraryId, 1, "attraction");
        
        NormalizedNode node = new NormalizedNode();
        node.setId(nodeId);  // Use generated ID
        node.setType("attraction");
        // ... populate rest of node
        
        return (T) itinerary;
    }
}
```

### Alternative: Direct Service Call

If you prefer to use the service directly (no HTTP overhead):

```java
@Component
public class MyAgent extends BaseAgent {
    
    private final NodeIdGenerator nodeIdGenerator;
    
    public MyAgent(NodeIdGenerator nodeIdGenerator, ...) {
        this.nodeIdGenerator = nodeIdGenerator;
    }
    
    @Override
    protected <T> T executeInternal(String itineraryId, AgentRequest<T> request) {
        // Load itinerary
        NormalizedItinerary itinerary = itineraryJsonService.getItinerary(itineraryId).get();
        
        // Generate ID using service
        String nodeId = nodeIdGenerator.generateNodeId("attraction", 1, itinerary);
        
        NormalizedNode node = new NormalizedNode();
        node.setId(nodeId);
        // ... populate node
        
        return (T) itinerary;
    }
}
```

---

## Tool 2: Check User Constraints

**When to use:** BEFORE generating nodes, especially for meals and activities

**Purpose:** Ensure generated content respects user budget, dietary restrictions, and party size

### Code Pattern

```java
import com.tripplanner.dto.tools.ConstraintCheckRequest;
import com.tripplanner.dto.tools.ConstraintCheckResult;

public class MyAgent extends BaseAgent {
    
    private final RestTemplate restTemplate = new RestTemplate();
    
    private ConstraintCheckResult checkConstraints(String itineraryId) {
        ConstraintCheckRequest request = new ConstraintCheckRequest(itineraryId);
        request.setCheckBudget(true);
        request.setCheckDietary(true);
        request.setCheckPartySize(true);
        
        try {
            ConstraintCheckResult result = restTemplate.postForObject(
                "http://localhost:8080/api/v1/tools/check-user-constraints",
                request,
                ConstraintCheckResult.class
            );
            
            return result;
        } catch (Exception e) {
            logger.error("Error calling constraint check tool", e);
            // Return empty result on error
            return new ConstraintCheckResult();
        }
    }
    
    @Override
    protected <T> T executeInternal(String itineraryId, AgentRequest<T> request) {
        // Check constraints before generating
        ConstraintCheckResult constraints = checkConstraints(itineraryId);
        
        if (!constraints.isValid()) {
            logger.warn("Constraint violations detected:");
            for (var violation : constraints.getViolations()) {
                logger.warn("  - {}: {}", violation.getType(), violation.getMessage());
            }
            
            // Option 1: Adjust generation strategy
            // Option 2: Fail gracefully
            // Option 3: Continue with warnings
        }
        
        if (!constraints.getWarnings().isEmpty()) {
            logger.info("Constraint warnings:");
            for (String warning : constraints.getWarnings()) {
                logger.info("  - {}", warning);
            }
        }
        
        // Continue with generation...
        return (T) itinerary;
    }
}
```

### Checking Specific Nodes

You can also validate a specific proposed node:

```java
private boolean validateProposedNode(String itineraryId, NormalizedNode proposedNode) {
    ConstraintCheckRequest request = new ConstraintCheckRequest(itineraryId);
    request.setProposedNode(proposedNode);
    request.setCheckDietary(true);  // Only check dietary for meals
    
    ConstraintCheckResult result = restTemplate.postForObject(
        "http://localhost:8080/api/v1/tools/check-user-constraints",
        request,
        ConstraintCheckResult.class
    );
    
    return result != null && result.isValid();
}
```

---

## Complete Agent Example

Here's a complete example showing both tools in action:

```java
@Component
@ConditionalOnBean(AiClient.class)
public class ActivityAgent extends BaseAgent {
    
    private final AiClient aiClient;
    private final ItineraryJsonService itineraryJsonService;
    private final RestTemplate restTemplate;
    
    public ActivityAgent(AgentEventBus eventBus, 
                        AiClient aiClient,
                        ItineraryJsonService itineraryJsonService) {
        super(eventBus, AgentEvent.AgentKind.ENRICHMENT);
        this.aiClient = aiClient;
        this.itineraryJsonService = itineraryJsonService;
        this.restTemplate = new RestTemplate();
    }
    
    @Override
    protected <T> T executeInternal(String itineraryId, AgentRequest<T> request) {
        logger.info("=== ACTIVITY AGENT ===");
        
        // STEP 1: Check user constraints
        ConstraintCheckResult constraints = checkConstraints(itineraryId);
        if (!constraints.isValid()) {
            logger.warn("Constraint violations: {}", constraints.getViolations());
        }
        
        // STEP 2: Load itinerary
        NormalizedItinerary itinerary = itineraryJsonService.getItinerary(itineraryId)
            .orElseThrow(() -> new RuntimeException("Itinerary not found"));
        
        // STEP 3: Process each day
        for (NormalizedDay day : itinerary.getDays()) {
            for (NormalizedNode node : day.getNodes()) {
                if (!"attraction".equals(node.getType())) {
                    continue;
                }
                
                // STEP 4: Generate unique ID if needed
                if (node.getId() == null || node.getId().isEmpty()) {
                    String nodeId = generateNodeId(itineraryId, day.getDayNumber(), "attraction");
                    node.setId(nodeId);
                }
                
                // STEP 5: Populate node with LLM
                populateAttractionNode(node, itinerary, constraints);
            }
        }
        
        // STEP 6: Save updated itinerary
        itineraryJsonService.updateItineraryWithLock(itinerary);
        
        logger.info("=== ACTIVITY AGENT COMPLETE ===");
        return (T) itinerary;
    }
    
    private String generateNodeId(String itineraryId, Integer dayNumber, String nodeType) {
        NodeIdRequest request = new NodeIdRequest(itineraryId, dayNumber, nodeType);
        
        try {
            NodeIdResponse response = restTemplate.postForObject(
                "http://localhost:8080/api/v1/tools/generate-node-id",
                request,
                NodeIdResponse.class
            );
            
            if (response != null && response.isSuccess()) {
                return response.getNodeId();
            }
        } catch (Exception e) {
            logger.error("Error generating node ID", e);
        }
        
        // Fallback
        return "day" + dayNumber + "_node_" + System.currentTimeMillis();
    }
    
    private ConstraintCheckResult checkConstraints(String itineraryId) {
        ConstraintCheckRequest request = new ConstraintCheckRequest(itineraryId);
        request.setCheckBudget(true);
        
        try {
            return restTemplate.postForObject(
                "http://localhost:8080/api/v1/tools/check-user-constraints",
                request,
                ConstraintCheckResult.class
            );
        } catch (Exception e) {
            logger.error("Error checking constraints", e);
            return new ConstraintCheckResult();
        }
    }
    
    private void populateAttractionNode(NormalizedNode node, 
                                       NormalizedItinerary itinerary,
                                       ConstraintCheckResult constraints) {
        // Build prompt with constraint awareness
        String prompt = buildPrompt(node, itinerary, constraints);
        
        // Call LLM
        String response = aiClient.generateStructuredContent(prompt, schema, systemPrompt);
        
        // Parse and apply
        // ... existing logic
    }
}
```

---

## Best Practices

### 1. Always Generate IDs First

```java
// ✅ GOOD: Generate ID before creating node
String nodeId = generateNodeId(itineraryId, dayNumber, nodeType);
NormalizedNode node = new NormalizedNode();
node.setId(nodeId);

// ❌ BAD: Create node without ID
NormalizedNode node = new NormalizedNode();
// ID might conflict with existing nodes
```

### 2. Check Constraints Early

```java
// ✅ GOOD: Check constraints before generating
ConstraintCheckResult constraints = checkConstraints(itineraryId);
if (!constraints.isValid()) {
    // Adjust strategy or fail gracefully
}
// Generate nodes...

// ❌ BAD: Generate first, check later
// Generate nodes...
ConstraintCheckResult constraints = checkConstraints(itineraryId);
// Too late - nodes already created
```

### 3. Handle Tool Failures Gracefully

```java
// ✅ GOOD: Fallback on tool failure
try {
    String nodeId = generateNodeId(itineraryId, dayNumber, nodeType);
} catch (Exception e) {
    logger.error("Tool failed, using fallback", e);
    nodeId = nodeIdGenerator.generateNodeId(nodeType, dayNumber);
}

// ❌ BAD: Let tool failure crash agent
String nodeId = generateNodeId(itineraryId, dayNumber, nodeType);
// If tool fails, entire agent fails
```

### 4. Log Constraint Violations

```java
// ✅ GOOD: Log violations for debugging
if (!constraints.isValid()) {
    logger.warn("Constraint violations detected:");
    constraints.getViolations().forEach(v -> 
        logger.warn("  - {}: {}", v.getType(), v.getMessage())
    );
}

// ❌ BAD: Silently ignore violations
if (!constraints.isValid()) {
    // No logging - hard to debug
}
```

---

## Migration Checklist

For each agent, complete these steps:

- [ ] Add RestTemplate or inject NodeIdGenerator service
- [ ] Add `generateNodeId()` helper method
- [ ] Add `checkConstraints()` helper method
- [ ] Update `executeInternal()` to call tools
- [ ] Add error handling and fallbacks
- [ ] Test with real itinerary data
- [ ] Update agent documentation

---

## Testing Your Integration

### Unit Test Example

```java
@Test
public void testAgentUsesNodeIdTool() {
    // Arrange
    String itineraryId = "test-123";
    NormalizedItinerary itinerary = createTestItinerary();
    
    // Mock tool response
    NodeIdResponse mockResponse = new NodeIdResponse("day1_node1");
    when(restTemplate.postForObject(anyString(), any(), eq(NodeIdResponse.class)))
        .thenReturn(mockResponse);
    
    // Act
    agent.execute(itineraryId, new AgentRequest<>(itinerary, NormalizedItinerary.class));
    
    // Assert
    verify(restTemplate).postForObject(
        contains("/generate-node-id"),
        any(NodeIdRequest.class),
        eq(NodeIdResponse.class)
    );
}
```

### Integration Test Example

```java
@SpringBootTest
@AutoConfigureMockMvc
public class ActivityAgentIntegrationTest {
    
    @Autowired
    private ActivityAgent activityAgent;
    
    @Autowired
    private ItineraryJsonService itineraryJsonService;
    
    @Test
    public void testAgentGeneratesUniqueIds() {
        // Create test itinerary
        String itineraryId = createTestItinerary();
        
        // Execute agent
        activityAgent.populateAttractions(itineraryId, skeleton);
        
        // Verify all nodes have unique IDs
        NormalizedItinerary result = itineraryJsonService.getItinerary(itineraryId).get();
        Set<String> nodeIds = new HashSet<>();
        
        for (NormalizedDay day : result.getDays()) {
            for (NormalizedNode node : day.getNodes()) {
                assertNotNull(node.getId());
                assertTrue(nodeIds.add(node.getId()), "Duplicate node ID: " + node.getId());
            }
        }
    }
}
```

---

## Troubleshooting

### Issue: Tool returns null

**Cause:** Network error or service not running

**Solution:** Add null checks and fallback logic

```java
NodeIdResponse response = restTemplate.postForObject(...);
if (response == null || !response.isSuccess()) {
    // Use fallback
    return nodeIdGenerator.generateNodeId(nodeType, dayNumber);
}
```

### Issue: Constraint check always passes

**Cause:** Itinerary doesn't have constraints set

**Solution:** Check if constraints exist before validating

```java
if (itinerary.getConstraints() == null || itinerary.getConstraints().isEmpty()) {
    logger.info("No constraints to check");
    return new ConstraintCheckResult(); // Valid by default
}
```

### Issue: Node IDs still conflicting

**Cause:** Not using tool consistently

**Solution:** Ensure ALL agents use the tool

```bash
# Search for agents not using tool
grep -r "node.setId" src/main/java/com/tripplanner/agents/
# Update any agents that set IDs manually
```

---

---

## NEW TOOLS - Quick Reference

### Tool 9: Suggest Best Time (Weather-Based Scheduling)

**Agents:** DayByDayPlannerAgent, ActivityAgent  
**When:** When scheduling activities to find optimal times  
**Endpoint:** `POST /api/v1/tools/suggest-best-time`

```java
SuggestBestTimeRequest request = new SuggestBestTimeRequest(
    "Pondicherry White Town",  // activityName
    "scenic_spot",              // activityType (auto-classified if null)
    "Pondicherry",              // location
    "2024-12-15",               // date
    120                         // duration in minutes
);

SuggestBestTimeResult result = restTemplate.postForObject(
    "http://localhost:8080/api/v1/tools/suggest-best-time",
    request,
    SuggestBestTimeResult.class
);

// Use top recommended slot
if (!result.getRecommendedTimeSlots().isEmpty()) {
    TimeSlot bestSlot = result.getRecommendedTimeSlots().get(0);
    node.getTiming().setStartTime(bestSlot.getStartTime());
    logger.info("Scheduled at {} (score: {})", bestSlot.getStartTime(), bestSlot.getSuitabilityScore());
}
```

**Activity Types:** outdoor_park, outdoor_monument, scenic_spot, beach_water, indoor_museum, indoor_mall, indoor_temple, night_activity, flexible

---

### Tool 10: Check Opening Hours (Enhanced)

**Agents:** ActivityAgent, MealAgent  
**When:** Before scheduling visits to verify place is open  
**Endpoint:** `POST /api/v1/tools/check-opening-hours`

```java
CheckOpeningHoursRequest request = new CheckOpeningHoursRequest();
request.setPlaceId("ChIJ...");
request.setDate("2024-12-15");
request.setTime("14:00");  // Optional: check if open at specific time

CheckOpeningHoursResult result = restTemplate.postForObject(
    "http://localhost:8080/api/v1/tools/check-opening-hours",
    request,
    CheckOpeningHoursResult.class
);

// Check results
if (result.getOpenAtRequestedTime() != null && !result.getOpenAtRequestedTime()) {
    logger.warn("Place closed at requested time: {}", request.getTime());
}

// Get full week schedule
List<String> weekdayText = result.getWeekdayText();
// ["Monday: 9:00 AM – 5:00 PM", "Tuesday: 9:00 AM – 5:00 PM", ...]
```

---

### Tool 11: Check Capacity (Enhanced)

**Agents:** ActivityAgent, MealAgent  
**When:** For large parties to verify venue can accommodate  
**Endpoint:** `POST /api/v1/tools/check-capacity`

```java
CheckCapacityRequest request = new CheckCapacityRequest();
request.setPlaceId("ChIJ...");  // Optional: for better estimation
request.setPlaceName("Restaurant Name");
request.setPartySize(15);

CheckCapacityResult result = restTemplate.postForObject(
    "http://localhost:8080/api/v1/tools/check-capacity",
    request,
    CheckCapacityResult.class
);

if (!result.isCanAccommodate()) {
    logger.warn("Venue may not accommodate party of {}: {}", 
               request.getPartySize(), result.getRecommendation());
}
```

---

### Tool 12: Get Transport Options (Enhanced)

**Agents:** TransportAgent  
**When:** Planning transport between cities  
**Endpoint:** `POST /api/v1/tools/get-transport-options`

```java
GetTransportOptionsRequest request = new GetTransportOptionsRequest();
request.setOrigin("Delhi");
request.setDestination("Mumbai");

GetTransportOptionsResult result = restTemplate.postForObject(
    "http://localhost:8080/api/v1/tools/get-transport-options",
    request,
    GetTransportOptionsResult.class
);

// Enhanced cost estimation based on distance
for (TransportOption option : result.getOptions()) {
    logger.info("{}: {} km, {} min, ${} ({})", 
               option.getMode(), 
               option.getDistanceKm(),
               option.getDurationMinutes(),
               option.getEstimatedCost(),
               option.getNotes());
}
```

---

## Agent-Tool Matrix (Updated)

| Agent | Suggest Best Time | Opening Hours | Capacity | Dietary Verification | Transport Options |
|-------|-------------------|---------------|----------|---------------------|-------------------|
| **DayByDayPlannerAgent** | ✅ Primary | ✅ Yes | ✅ Yes | ❌ No | ✅ Yes |
| **ActivityAgent** | ✅ Yes | ✅ Primary | ✅ Yes | ❌ No | ❌ No |
| **MealAgent** | ❌ No | ✅ Yes | ✅ Yes | ✅ Primary | ❌ No |
| **TransportAgent** | ❌ No | ❌ No | ❌ No | ❌ No | ✅ Primary |
| **EnrichmentAgent** | ❌ No | ✅ Yes | ❌ No | ❌ No | ❌ No |

---

## Summary

**Key Takeaways:**

1. **Always** generate node IDs using the tool before creating nodes
2. **Always** check constraints before generating content
3. **Use** suggest-best-time for intelligent activity scheduling
4. **Use** enhanced opening hours to avoid closed venues
5. **Use** dietary verification for restaurant confidence scoring
6. **Always** handle tool failures gracefully with fallbacks

**Benefits:**

- ✅ Zero node ID conflicts
- ✅ User constraints respected
- ✅ Weather-optimized scheduling
- ✅ Verified opening hours
- ✅ Dietary restriction confidence
- ✅ Better cost estimates
- ✅ Easier debugging

---

For more details, see:
- [TOOLS_IMPLEMENTATION_FINAL_STATUS.md](./TOOLS_IMPLEMENTATION_FINAL_STATUS.md)
- [WEATHER_BASED_SCHEDULING_GUIDE.md](./WEATHER_BASED_SCHEDULING_GUIDE.md)
- [TOOL_CREATION_RULEBOOK.md](./TOOL_CREATION_RULEBOOK.md)
