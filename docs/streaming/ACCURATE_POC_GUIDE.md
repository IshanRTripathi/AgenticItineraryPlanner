# ADK + MCP POC Implementation Guide
## Tailored to YOUR Actual Codebase

**Based on Analysis of**:
- ✅ Gradle build system (Java 17)
- ✅ Existing  `BaseAgent` framework (14 agents)
- ✅ `AiClient` interface with `GeminiClient`/`OpenRouterClient`
- ✅ `PipelineOrchestrator` with `AgentCoordinator` locks
- ✅ WebSocket-based `AgentEventBus`

---

## Quick Facts About Your System

| Component | Current State |
|-----------|--------------|
| Build System | Gradle (not Maven!) |
| Java Version | 17 |
| Spring Boot | 3.5.5 |
| LLM Clients | `GeminiClient.java`, `OpenRouterClient.java` |
| Agent Base | `BaseAgent.java` (252 lines) |
| Agents | 14 total (Skeleton, Activity, Meal, Transport, Cost, etc.) |
| Orchestrator | `PipelineOrchestrator.java` (1098 lines) |
| Event System | `AgentEventBus` via WebSocket |

---

## Step 1: Add ADK Dependencies (15 min)

### 1.1 Update `build.gradle`

**File**: `build.gradle` (line 21-84)

Add these dependencies **after line 59** (after json-schema-validator):

```gradle
dependencies {
    // ... existing dependencies ...
    
    // JSON Schema Validation for LLM responses
    implementation 'com.networknt:json-schema-validator:1.0.87'

    // NEW: Google ADK + MCP
    implementation 'com.google.cloud.ai:google-cloud-ai-adk:0.2.0'
    implementation 'io.modelcontextprotocol:mcp-java:1.0.1'
    
    // Lombok for reducing boilerplate code
    compileOnly 'org.projectlombok:lombok'
    // ... rest of dependencies ...
}
```

### 1.2 Rebuild Project

```bash
cd c:\Users\user\IdeaProjects\AgenticItineraryPlanner
./gradlew clean build
```

**Expected**: Build succeeds, dependencies downloaded

---

## Step 2: Create MCP Tool (30 min)

### 2.1 Create Package Structure

```
src/main/java/com/tripplanner/
├── mcp/
│   ├── config/
│   │   └── MCPServerConfig.java
│   └── tools/
│       └── CostCalculatorMCPTool.java
```

### 2.2 Create MCP Tool (Wraps Existing Logic)

**File**: `src/main/java/com/tripplanner/mcp/tools/CostCalculatorMCPTool.java`

```java
package com.tripplanner.mcp.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripplanner.dto.BudgetSummary;
import com.tripplanner.dto.NormalizedItinerary;
import com.tripplanner.service.BudgetTracker;
import com.tripplanner.service.ItineraryJsonService;
import io.modelcontextprotocol.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

/**
 * MCP Tool that exposes cost calculation logic.
 * Wraps existing BudgetTracker service for ADK agents.
 */
@Component
public class CostCalculatorMCPTool implements Tool {
    
    private static final Logger logger = LoggerFactory.getLogger(CostCalculatorMCPTool.class);
    
    private final BudgetTracker budgetTracker;
    private final ItineraryJsonService itineraryJsonService;
    private final ObjectMapper objectMapper;
    
    public CostCalculatorMCPTool(
        BudgetTracker budgetTracker,
        ItineraryJsonService itineraryJsonService,
        ObjectMapper objectMapper
    ) {
        this.budgetTracker = budgetTracker;
        this.itineraryJsonService = itineraryJsonService;
        this.objectMapper = objectMapper;
    }
    
    @Override
    public String getName() {
        return "calculate_itinerary_cost";
    }
    
    @Override
    public String getDescription() {
        return "Calculate the total estimated cost and budget summary for an itinerary. " +
               "Returns total cost, budget breakdown, warnings, and recommendations.";
    }
    
    @Override
    public Map<String, Object> getInputSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "itineraryId", Map.of(
                    "type", "string",
                    "description", "The ID of the itinerary to calculate costs for"
                ),
                "budgetMin", Map.of(
                    "type", "number",
                    "description", "Minimum budget per day (optional)"
                ),
                "budgetMax", Map.of(
                    "type", "number",
                    "description", "Maximum budget per day (optional)"
                ),
                "partySize", Map.of(
                    "type", "integer",
                    "description", "Number of people in the party (default: 1)"
                )
            ),
            "required", new String[]{"itineraryId"}
        );
    }
    
    @Override
    public Object execute(Map<String, Object> input) {
        logger.info("=== MCP Tool: calculate_itinerary_cost called ===");
        logger.info("Input: {}", input);
        
        try {
            String itineraryId = (String) input.get("itineraryId");
            Double budgetMin = input.containsKey("budgetMin") ? 
                ((Number) input.get("budgetMin")).doubleValue() : null;
            Double budgetMax = input.containsKey("budgetMax") ? 
                ((Number) input.get("budgetMax")).doubleValue() : null;
            Integer partySize = input.containsKey("partySize") ? 
                ((Number) input.get("partySize")).intValue() : 1;
            
            // Fetch itinerary
            Optional<NormalizedItinerary> itineraryOpt = itineraryJsonService.getItinerary(itineraryId);
            if (itineraryOpt.isEmpty()) {
                return Map.of(
                    "success", false,
                    "error", "Itinerary not found: " + itineraryId
                );
            }
            
            NormalizedItinerary itinerary = itineraryOpt.get();
            
            // Calculate budget using existing service
            BudgetSummary budget = budgetTracker.calculateBudget(
                itinerary,
                budgetMin,
                budgetMax,
                partySize
            );
            
            logger.info("Cost calculated: Total = {}, Currency = {}", 
                budget.getTotal(), budget.getCurrency());
            
            return Map.of(
                "success", true,
                "totalCost", budget.getTotal(),
                "currency", budget.getCurrency(),
                "perPersonCost", budget.getTotalPerPerson(),
                "perDayCost", budget.getPerDayAverage(),
                "breakdown", Map.of(
                    "accommodation", budget.getAccommodationTotal(),
                    "activities", budget.getActivityTotal(),
                    "meals", budget.getMealTotal(),
                    "transport", budget.getTransportTotal()
                ),
                "budgetStatus", Map.of(
                    "percentageUsed", budget.getPercentage OfBudget(),
                    "isOverBudget", budget.isOverBudget(),
                    "warnings", budget.getWarnings()
                )
            );
            
        } catch (Exception e) {
            logger.error("Error calculating cost", e);
            return Map.of(
                "success", false,
                "error", e.getMessage()
            );
        }
    }
}
```

### 2.3 Create MCP Server Config

**File**: `src/main/java/com/tripplanner/mcp/config/MCPServerConfig.java`

```java
package com.tripplanner.mcp.config;

import com.tripplanner.mcp.tools.CostCalculatorMCPTool;
import io.modelcontextprotocol.MCPServer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "adk.mcp.enabled", havingValue = "true", matchIfMissing = true)
public class MCPServerConfig {
    
    @Bean
    public MCPServer mcpServer(CostCalculatorMCPTool costCalculatorTool) {
        return MCPServer.builder()
            .name("TripPlanner MCP Server")
            .version("1.0.0")
            .addTool(costCalculatorTool)
            .build();
    }
}
```

### 2.4 Create MCP REST Endpoint

**File**: `src/main/java/com/tripplanner/controller/MCPController.java`

```java
package com.tripplanner.controller;

import io.modelcontextprotocol.MCPServer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/mcp")
@ConditionalOnBean(MCPServer.class)
public class MCPController {
    
    private final MCPServer mcpServer;
    
    public MCPController(MCPServer mcpServer) {
        this.mcpServer = mcpServer;
    }
    
    @GetMapping("/tools")
    public Object listTools() {
        return mcpServer.listTools();
    }
    
    @PostMapping("/tools/{toolName}")
    public Object executeTool(
        @PathVariable String toolName,
        @RequestBody Map<String, Object> input
    ) {
        return mcpServer.executeTool(toolName, input);
    }
}
```

### 2.5 Update application.yml

**File**: `src/main/resources/application.yml` (add after line 100)

```yaml
# ADK + MCP Configuration
adk:
  mcp:
    enabled: ${ADK_MCP_ENABLED:true}
    server-url: ${ADK_MCP_SERVER_URL:http://localhost:8080/api/v1/mcp}
```

### 2.6 Test MCP Endpoint

Start your app, then:

```bash
# List available tools
curl http://localhost:8080/api/v1/mcp/tools

# Expected output:
# {"tools": [{"name": "calculate_itinerary_cost", "description": "..."}]}

# Test tool execution (replace with real itinerary ID)
curl -X POST http://localhost:8080/api/v1/mcp/tools/calculate_itinerary_cost \
  -H "Content-Type: application/json" \
  -d '{
    "itineraryId": "your-test-itinerary-id",
    "partySize": 2
  }'
```

---

## Step 3: Create ADK Agent (45 min)

### 3.1 Create ADK Coordinator Agent

**File**: `src/main/java/com/tripplanner/service/adk/ADKCoordinatorAgent.java`

```java
package com.tripplanner.service.adk;

import com.google.cloud.ai.adk.Agent;
import com.google.cloud.ai.adk.GenerateContentRequest;
import com.google.cloud.ai.adk.StreamingCallback;
import com.google.cloud.ai.adk.toolsets.MCPToolset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * ADK-powered coordinator agent that uses MCP tools.
 * This is a PROOF OF CONCEPT - shows streaming + tool use.
 */
@Service
@ConditionalOnProperty(name = "adk.enabled", havingValue = "true")
public class ADKCoordinatorAgent {
    
    private static final Logger logger = LoggerFactory.getLogger(ADKCoordinatorAgent.class);
    
    private final Agent agent;
    
    public ADKCoordinatorAgent(
        @Value("${google.ai.api-key}") String apiKey,
        @Value("${google.ai.model}") String model,
        @Value("${adk.mcp.server-url}") String mcpServerUrl
    ) {
        logger.info("Initializing ADK Coordinator Agent...");
        logger.info("Model: {}", model);
        logger.info("MCP Server: {}", mcpServerUrl);
        
        // Create MCP toolset (connects to our backend)
        MCPToolset toolset = MCPToolset.builder()
            .serverUrl(mcpServerUrl)
            .build();
        
        // Create ADK agent
        this.agent = Agent.builder()
            .apiKey(apiKey)
            .model(model)  // gemini-2.5-flash from your config
            .instructions("""
                You are a travel planning coordinator assistant.
                
                Your capabilities:
                - Calculate itinerary costs using the 'calculate_itinerary_cost' tool
                - Provide budget analysis and recommendations
                - Explain cost breakdowns
                
                Always:
                1. Show your reasoning process before using tools
                2. Explain what you're doing
                3. Provide clear cost summaries
                
                When asked about costs:
                - Use the calculate_itinerary_cost tool
                - Explain the breakdown (accommodation, meals, activities, transport)
                - Warn if over budget
                """)
            .addToolset(toolset)
            .enableThoughts(true)  // Enable reasoning visibility  
            .build();
        
        logger.info("✅ ADK Agent initialized successfully");
    }
    
    /**
     * Execute query with streaming (shows thoughts + tokens).
     */
    public void executeWithStreaming(String userQuery, StreamingCallback callback) {
        logger.info("=== ADK Agent: Processing query (streaming) ===");
        logger.info("Query: {}", userQuery);
        
        try {
            agent.generateContentStream(
                GenerateContentRequest.builder()
                    .prompt(userQuery)
                    .includeThoughts(true)
                    .build()
            )
            .forEach(chunk -> {
                // Thought tokens (reasoning)
                if (chunk.hasThought()) {
                    String thought = chunk.getThought();
                    logger.debug("THOUGHT: {}", thought);
                    callback.onThought(thought);
                }
                
                // Content tokens (response)
                if (chunk.hasContent()) {
                    String token = chunk.getContent();
                    callback.onToken(token);
                }
                
                // Tool calls (automatic - ADK handles this)
                if (chunk.hasToolCall()) {
                    logger.info("TOOL CALL: {}", chunk.getToolCall().getName());
                    // ADK automatically executes and gets result
                }
            });
            
            callback.onComplete("");
            logger.info("=== ADK Agent: Complete ===");
            
        } catch (Exception e) {
            logger.error("ADK agent failed", e);
            callback.onError(e);
        }
    }
    
    /**
     * Execute query synchronously (for testing).
     */
    public String execute(String userQuery) {
        logger.info("=== ADK Agent: Processing query (sync) ===");
        
        return agent.generateContent(
            GenerateContentRequest.builder()
                .prompt(userQuery)
                .includeThoughts(false)  // Don't include thoughts in final response
                .build()
        );
    }
}
```

### 3.2 Update application.yml

Add ADK configuration:

```yaml
# ADK Configuration
adk:
  enabled: ${ADK_ENABLED:true}
  mcp:
    enabled: ${ADK_MCP_ENABLED:true}
    server-url: ${ADK_MCP_SERVER_URL:http://localhost:8080/api/v1/mcp}
```

---

## Step 4: Add Streaming Controller (30 min)

### 4.1 Create Streaming Callback Interface

**File**: `src/main/java/com/tripplanner/service/adk/ADKStreamingCallback.java`

```java
package com.tripplanner.service.adk;

public interface ADKStreamingCallback {
    void onToken(String token);
    void onThought(String thought);
    void onComplete(String fullResponse);
    void onError(Exception error);
}
```

### 4.2 Create SSE Controller

**File**: `src/main/java/com/tripplanner/controller/ADKStreamingController.java`

```java
package com.tripplanner.controller;

import com.tripplanner.dto.ChatRequest;
import com.tripplanner.service.adk.ADKCoordinatorAgent;
import com.tripplanner.service.adk.ADKStreamingCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/v1/adk/stream")
@ConditionalOnBean(ADKCoordinatorAgent.class)
public class ADKStreamingController {
    
    private static final Logger logger = LoggerFactory.getLogger(ADKStreamingController.class);
    
    private final ADKCoordinatorAgent adkAgent;
    
    public ADKStreamingController(ADKCoordinatorAgent adkAgent) {
        this.adkAgent = adkAgent;
    }
    
    @PostMapping
    public SseEmitter streamResponse(@RequestBody ChatRequest request) {
        SseEmitter emitter = new SseEmitter(120_000L);
        
        logger.info("Starting ADK streaming for: {}", request.getText());
        
        CompletableFuture.runAsync(() -> {
            try {
                adkAgent.executeWithStreaming(
                    request.getText(),
                    new ADKStreamingCallback() {
                        @Override
                        public void onToken(String token) {
                            try {
                                emitter.send(SseEmitter.event()
                                    .name("token")
                                    .data(token));
                            } catch (IOException e) {
                                emitter.completeWithError(e);
                            }
                        }
                        
                        @Override
                        public void onThought(String thought) {
                            try {
                                emitter.send(SseEmitter.event()
                                    .name("thought")
                                    .data(thought));
                            } catch (IOException e) {
                                emitter.completeWithError(e);
                            }
                        }
                        
                        @Override
                        public void onComplete(String fullResponse) {
                            try {
                                emitter.send(SseEmitter.event()
                                    .name("done")
                                    .data("complete"));
                                emitter.complete();
                            } catch (IOException e) {
                                emitter.completeWithError(e);
                            }
                        }
                        
                        @Override
                        public void onError(Exception error) {
                            logger.error("Streaming error", error);
                            emitter.completeWithError(error);
                        }
                    }
                );
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });
        
        return emitter;
    }
}
```

---

## Step 5: Test End-to-End (15 min)

### 5.1 Start Application

```bash
./gradlew bootRun
```

### 5.2 Test MCP Tool Directly

```bash
# Verify tool is registered
curl http://localhost:8080/api/v1/mcp/tools

# Test tool execution
curl -X POST http://localhost:8080/api/v1/mcp/tools/calculate_itinerary_cost \
  -H "Content-Type: application/json" \
  -d '{"itineraryId": "test-id-123", "partySize": 2}'
```

### 5.3 Test ADK Agent (Sync)

Create a simple test endpoint:

```java
@GetMapping("/api/v1/adk/test")
public String testADK() {
    String result = adkAgent.execute("Calculate the cost for itinerary test-id-123");
    return result;
}
```

### 5.4 Test Streaming in Browser Console

```javascript
const es = new EventSource('/api/v1/adk/stream');

es.addEventListener('thought', (e) => {
  console.log('💭 THOUGHT:', e.data);
});

es.addEventListener('token', (e) => {
  console.log('📝 TOKEN:', e.data);
});

es.addEventListener('done', (e) => {
  console.log('✅ DONE');
  es.close();
});

// Send a message (use your actual POST method)
fetch('/api/v1/adk/stream', {
  method: 'POST',
  headers: {'Content-Type': 'application/json'},
  body: JSON.stringify({text: 'Calculate cost for itinerary abc123'})
});
```

---

## Success Checklist

After completing all steps:

- [ ] ✅ Gradle build succeeds with ADK/MCP dependencies
- [ ] ✅ `/api/v1/mcp/tools` returns tool list
- [ ] ✅ Direct tool call works (returns cost data)
- [ ] ✅ ADK agent initializes without errors
- [ ] ✅ Streaming endpoint returns SSE events
- [ ] ✅ Browser console shows thoughts + tokens
- [ ] ✅ Tool is automatically called by agent

---

## What You'll See

**When successful**:

```
User Query: "Calculate the cost for itinerary xyz-123"

💭 THOUGHT: "I need to use the calculate_itinerary_cost tool"
📝 TOKEN: "I'll"
📝 TOKEN: " calculate"
📝 TOKEN: " the"
📝 TOKEN: " cost"
📝 TOKEN: " for"
📝 TOKEN: " you..."
💭 THOUGHT: "Tool returned: total=$1,850, currency=USD"
📝 TOKEN: "The"
📝 TOKEN: " total"
📝 TOKEN: " cost"
📝 TOKEN: " is"
📝 TOKEN: " $1,850"
📝 TOKEN: " USD"
✅ DONE
```

---

## Next Steps After POC

1. ✅ **POC Works** → Migrate more tools:
   - Wrap `TransportAgent` as MCP tool
   - Wrap `ActivityAgent` as MCP tool
   - Wrap `MealAgent` as MCP tool

2. ✅ Integrate with `PipelineOrchestrator`:
   - Replace direct agent calls with ADK agent
   - Use ADK for orchestration logic

3. ✅ Frontend Integration:
   - Your existing `AgentStreamingCard` will work!
   - Just point to `/api/v1/adk/stream` endpoint

---

## Troubleshooting

### Issue: "ADK dependencies not found"

Check Gradle repositories:
```gradle
repositories {
    mavenCentral()
    maven { url 'https://maven.google.com' }
}
```

### Issue: "No Gemini API key"

Set environment variable:
```bash
export GEMINI_API_KEY="your-key-here"
```

Or update `application.yml` (already has default key).

### Issue: "MCP tool not discovered"

Check logs for:
```
✅ ADK Agent initialized successfully
```

If missing, verify `adk.enabled=true` in application.yml.

---

## Estimated Time

| Phase | Duration |
|-------|----------|
| Dependencies | 15 min |
| MCP Tool | 30 min |
| ADK Agent | 45 min |
| Streaming Controller | 30 min |
| Testing | 15 min |
| **Total** | **~2.5 hours** |

---

**Ready to start? Begin with Step 1 (add dependencies to `build.gradle`)!** 🚀
