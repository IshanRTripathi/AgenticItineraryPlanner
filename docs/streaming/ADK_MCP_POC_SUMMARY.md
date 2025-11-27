# ADK + MCP POC - Implementation Summary

**Date**: November 25, 2025  
**Status**: ✅ Ready for Testing  
**Time Invested**: ~4 hours

---

## 📁 Files Involved

### 1. Dependencies Configuration
**File**: [`build.gradle`](file:///c:/Users/user/IdeaProjects/AgenticItineraryPlanner/build.gradle#L61-L66)
```gradle
// Google ADK (Agent Development Kit) + MCP (Model Context Protocol) - POC
implementation("com.google.adk:google-adk:0.3.0")
// Spring AI will bring in compatible MCP SDK version automatically
// implementation("org.springframework.ai:spring-ai-mcp-client-webflux-spring-boot-starter:1.0.0-M6")  // DISABLED - not needed
implementation("org.springframework.ai:spring-ai-mcp-server-webflux-spring-boot-starter:1.0.0-M6")
implementation("org.springframework.ai:spring-ai-mcp-annotations:1.1.0")
```

**Key Decisions**:
- ✅ Using Spring AI MCP **server-only** (client disabled)
- ✅ Server version: `1.0.0-M6` (compatible with MCP SDK 0.7.0)
- ✅ Annotations version: `1.1.0` (for `@McpTool`)
- ✅ Removed explicit `mcp:0.16.0` dependency to avoid version conflicts

---

### 2. MCP Tool Implementation
**File**: [`TripPlannerMcpTools.java`](file:///c:/Users/user/IdeaProjects/AgenticItineraryPlanner/src/main/java/com/tripplanner/mcp/tools/TripPlannerMcpTools.java)

```java
@Component
public class TripPlannerMcpTools {
    
    @McpTool(name = "calculate_itinerary_cost", 
             description = "Calculate total estimated cost for an itinerary")
    public Map<String, Object> calculateCost(
        @McpToolParam(description = "Itinerary ID", required = true) 
        String itineraryId,
        
        @McpToolParam(description = "Min budget per day", required = false) 
        Double budgetMin,
        
        @McpToolParam(description = "Max budget per day", required = false) 
        Double budgetMax,
        
        @McpToolParam(description = "Party size", required = false) 
        Integer partySize
    ) {
        // Wraps existing BudgetTracker service
        BudgetSummary budget = budgetTracker.calculateBudget(...);
        CategoryBudget breakdown = budget.getCategoryBreakdown();
        
        return Map.of(
            "success", true,
            "totalCost", budget.getTotalCostPerPerson(),
            "totalCostForParty", budget.getTotalCostForParty(),
            "currency", budget.getCurrency(),
            "partySize", budget.getPartySize(),
            "breakdown", Map.of(...),
            "budgetStatus", Map.of(...)
        );
    }
}
```

**Key Features**:
- ✅ Uses `@McpTool` annotation from `org.springaicommunity.mcp.annotation`
- ✅ Wraps existing `BudgetTracker` service
- ✅ Returns comprehensive cost data with breakdown
- ✅ Handles errors gracefully

---

### 3. Application Configuration
**File**: [`application.yml`](file:///c:/Users/user/IdeaProjects/AgenticItineraryPlanner/src/main/resources/application.yml#L109-L114)

```yaml
# ADK (Agent Development Kit) + MCP Configuration - POC
adk:
  enabled: ${ADK_ENABLED:true}
  mcp:
    enabled: ${ADK_MCP_ENABLED:true}
    server-url: ${ADK_MCP_SERVER_URL:http://localhost:8080/api/v1/mcp}
```

**Configuration**:
- ✅ ADK enabled by default
- ✅ MCP server enabled
- ✅ Server URL configured (for future ADK agent)

---

### 4. Task Tracking
**File**: [`task.md`](file:///C:/Users/user/.gemini/antigravity/brain/a6f98bde-d815-46bf-841d-216971b2dd42/task.md)

Current progress:
```markdown
## Phase 2: Single MCP Tool ✅
- [x] Create TripPlannerMcpTools.java (Spring AI Community annotations)
- [x] Fix Spring AI MCP dependencies (1.0.0-M6 + annotations)
- [x] Fix BudgetSummary API usage
- [x] ✅ Compilation successful!
- [ ] Test application startup (bootRun)
- [ ] Verify MCP tool registration

## Phase 3: Simple ADK Agent (NEXT)
- [ ] Create ADKCoordinatorAgent.java
- [ ] Connect to MCP toolset
- [ ] Test with simple query
```

---

## 🎯 What We Accomplished

### ✅ Completed

1. **Dependency Hell Resolved**
   - Found correct Spring AI MCP dependencies
   - Resolved version conflicts (0.16.0 vs 0.7.0)
   - Disabled unnecessary MCP client

2. **MCP Tool Created**
   - Annotation-based tool: `calculate_itinerary_cost`
   - Wraps existing `BudgetTracker` service
   - Returns JSON with cost breakdown

3. **Build Success**
   - ✅ Gradle compilation successful
   - ✅ All dependencies resolved
   - ✅ No import errors

---

## 🔍 Key Technical Decisions

### 1. Spring AI vs Raw MCP SDK

**Decision**: Use Spring AI MCP annotations  
**Reason**: 
- Auto-discovery via `@Component` scan
- Automatic JSON schema generation
- No manual server configuration
- Spring Boot integration

### 2. Version 1.0.0-M6 vs 1.1.0

**Decision**: Server `1.0.0-M6`, Annotations `1.1.0`  
**Reason**:
- M6 stable for server auto-configuration
- 1.1.0 has latest annotation features
- Compatible MCP SDK version (0.7.0)

### 3. Client Disabled

**Decision**: Commented out client dependency  
**Reason**:
- Only exposing tools (server-side)
- Client caused auto-configuration errors
- ADK agent will use HTTP to call tools

---

## 📊 Current Architecture

```
┌─────────────────────────────────────┐
│  Frontend (Future)                  │
│  - Calls ADK streaming endpoint     │
└──────────────┬──────────────────────┘
               │ HTTP/SSE
               │
┌──────────────▼──────────────────────┐
│  ADK Streaming Agent (Phase 3)      │  ← TO BE CREATED
│  - Uses Runner.runLive()            │
│  - Streams thoughts + tokens        │
└──────────────┬──────────────────────┘
               │ HTTP (MCP Protocol)
               │
┌──────────────▼──────────────────────┐
│  Spring AI MCP Server ✅            │
│  - Auto-discovers @McpTool          │
│  - Exposes: calculate_itinerary_cost│
└──────────────┬──────────────────────┘
               │ Java Method Call
               │
┌──────────────▼──────────────────────┐
│  TripPlannerMcpTools ✅             │
│  - Wraps BudgetTracker              │
│  - Returns cost breakdown           │
└─────────────────────────────────────┘
```

---

## ⏭️ Next Steps

### Immediate: Test MCP Server

```bash
./gradlew bootRun
```

**Expected Logs**:
```
...Spring AI MCP server auto-configuration...
...Registered MCP tool: calculate_itinerary_cost...
...Started App in X seconds...
```

### Phase 3: ADK Streaming Agent

**File to Create**: `src/main/java/com/tripplanner/service/adk/ADKCoordinatorAgent.java`

```java
@Service
public class ADKCoordinatorAgent {
    
    private final Runner<Agent> runner;
    
    public Flowable<Event> streamResponse(String query) {
        LiveRequestQueue queue = new LiveRequestQueue();
        
        // Configure agent to use MCP tools
        Agent agent = Agent.builder()
            .tools(mcpToolRegistry.getTools())
            .build();
        
        return runner.runLive(queue, agent);
    }
}
```

### Phase 4: SSE Controller

**File to Create**: `src/main/java/com/tripplanner/controller/ADKStreamingController.java`

```java
@RestController
@RequestMapping("/api/v1/adk/stream")
public class ADKStreamingController {
    
    @PostMapping
    public Flux<ServerSentEvent<String>> stream(@RequestBody ChatRequest request) {
        return Flux.from(adkAgent.streamResponse(request.getText()))
            .map(event -> ServerSentEvent.<String>builder()
                .event(event.getType())
                .data(event.getData())
                .build());
    }
}
```

---

## 🐛 Issues Encountered & Solved

| Issue | Solution |
|-------|----------|
| MCP SDK 0.16.0 incompatible | Removed explicit dependency, let Spring AI manage |
| `ClientMcpTransport` not found | Same as above - version mismatch |
| `@McpTool` not found | Added `spring-ai-mcp-annotations:1.1.0` |
| Wrong package for annotations | Used `org.springaicommunity.mcp.annotation` |
| `BudgetSummary.getTotal()` missing | Fixed to use `getTotalCostPerPerson()` |
| MCP client auto-config error | Disabled client dependency |

---

## 📝 Lessons Learned

1. **Spring AI != Spring Boot versioning**  
   - Spring AI has its own release cycle
   - Milestone (M) releases may be more stable than GA for new features

2. **MCP SDK rapidly evolving**  
   - 0.7.0 → 0.16.0 has breaking changes
   - Let dependency management handle transitive versions

3. **Community annotations**  
   - Package `org.springaicommunity.*` vs `org.springframework.ai.*`
   - Indicates separate community-maintained module

4. **Server vs Client confusion**  
   - MCP has bidirectional capabilities
   - For tool exposure, only server needed

---

## 🎉 Success Metrics

- ✅ **Compilation**: Success
- ✅ **Dependencies**: All resolved
- ✅ **MCP Tool**: Created and compiles
- ⏸️ **Runtime**: Pending test
- ⏸️ **ADK Agent**: Phase 3
- ⏸️ **Streaming**: Phase 4

---

**Ready for**: Runtime testing → ADK agent creation → End-to-end streaming
