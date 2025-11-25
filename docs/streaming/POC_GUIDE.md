# ADK + MCP POC Implementation Guide
## Step-by-Step Instructions (3 Hours)

---

## ⚡ Quick Start

**Goal**: Expose `CostEstimatorService` as MCP tool, call it from ADK agent, stream thoughts to UI.

**Time**: 3 hours  
**Risk**: Low (reversible, no production changes)

---

## Step 1: Add Dependencies (10 minutes)

### Update `pom.xml`

```xml
<dependencies>
    <!-- Existing dependencies... -->
    
    <!-- Google ADK (Agent Development Kit) -->
    <dependency>
        <groupId>com.google.cloud.ai</groupId>
        <artifactId>google-cloud-ai-adk</artifactId>
        <version>0.1.0</version>
    </dependency>
    
    <!-- MCP Server (Model Context Protocol) -->
    <dependency>
        <groupId>io.modelcontextprotocol</groupId>
        <artifactId>mcp-server-java</artifactId>
        <version>1.0.0</version>
    </dependency>
    
    <!-- JSON Schema validation -->
    <dependency>
        <groupId>com.networknt</groupId>
        <artifactId>json-schema-validator</artifactId>
        <version>1.5.1</version>
    </dependency>
</dependencies>
```

### Run Maven

```bash
cd c:\Users\user\IdeaProjects\AgenticItineraryPlanner
mvn clean install
```

---

## Step 2: Create MCP Tool (45 minutes)

### 2.1 Create Package Structure

```
src/main/java/com/tripplanner/
├── mcp/
│   ├── config/
│   │   └── MCPServerConfig.java
│   ├── tools/
│   │   └── CostEstimatorMCPTool.java
│   └── dto/
│       └── ToolInput.java
```

### 2.2 Create MCP Tool

**File**: `src/main/java/com/tripplanner/mcp/tools/CostEstimatorMCPTool.java`

```java
package com.tripplanner.mcp.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripplanner.dto.BudgetSummary;
import com.tripplanner.dto.ItineraryDto;
import com.tripplanner.service.CostEstimatorService;
import io.modelcontextprotocol.server.Tool;
import io.modelcontextprotocol.server.ToolHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class CostEstimatorMCPTool implements Tool {
    
    private static final Logger logger = LoggerFactory.getLogger(CostEstimatorMCPTool.class);
    
    private final CostEstimatorService costEstimatorService;
    private final ObjectMapper objectMapper;
    
    public CostEstimatorMCPTool(CostEstimatorService costEstimatorService, ObjectMapper objectMapper) {
        this.costEstimatorService = costEstimatorService;
        this.objectMapper = objectMapper;
    }
    
    @Override
    public String getName() {
        return "calculate_cost";
    }
    
    @Override
    public String getDescription() {
        return "Calculate the total estimated cost of an itinerary including accommodations, meals, activities, and transport";
    }
    
    @Override
    public Map<String, Object> getInputSchema() {
        // JSON Schema for tool input
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "itinerary", Map.of(
                    "type", "object",
                    "description", "The complete itinerary data"
                )
            ),
            "required", new String[]{"itinerary"}
        );
    }
    
    @Override
    public Object execute(Map<String, Object> input) {
        logger.info("=== MCP Tool: calculate_cost called ===");
        logger.info("Input: {}", input);
        
        try {
            // Convert input to ItineraryDto
            Object itineraryData = input.get("itinerary");
            ItineraryDto itinerary = objectMapper.convertValue(itineraryData, ItineraryDto.class);
            
            // Call existing service
            BudgetSummary cost = costEstimatorService.calculateTotalCost(itinerary);
            
            logger.info("Cost calculated: {}", cost);
            
            return Map.of(
                "success", true,
                "totalCost", cost.getTotal(),
                "currency", cost.getCurrency(),
                "breakdown", cost
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

import com.tripplanner.mcp.tools.CostEstimatorMCPTool;
import io.modelcontextprotocol.server.MCPServer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MCPServerConfig {
    
    private final CostEstimatorMCPTool costEstimatorTool;
    
    public MCPServerConfig(CostEstimatorMCPTool costEstimatorTool) {
        this.costEstimatorTool = costEstimatorTool;
    }
    
    @Bean
    public MCPServer mcpServer() {
        return MCPServer.builder()
            .name("TripPlanner MCP Server")
            .version("1.0.0")
            .addTool(costEstimatorTool)
            .build();
    }
}
```

### 2.4 Create MCP Endpoint

**File**: `src/main/java/com/tripplanner/controller/MCPController.java`

```java
package com.tripplanner.controller;

import io.modelcontextprotocol.server.MCPServer;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/mcp")
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
    public Object executeTool(@PathVariable String toolName, @RequestBody Map<String, Object> input) {
        return mcpServer.executeTool(toolName, input);
    }
}
```

### 2.5 Test MCP Endpoint

```bash
# Test tool listing
curl http://localhost:8080/mcp/tools

# Expected: {"tools": [{"name": "calculate_cost", "description": "..."}]}

# Test tool execution
curl -X POST http://localhost:8080/mcp/tools/calculate_cost \
  -H "Content-Type: application/json" \
  -d '{
    "itinerary": {
      "days": [],
      "currency": "USD"
    }
  }'

# Expected: {"success": true, "totalCost": 0, ...}
```

---

## Step 3: Create ADK Agent (45 minutes)

### 3.1 Configure Gemini API Key

**File**: `src/main/resources/application.yml`

```yaml
google:
  ai:
    api-key: ${GEMINI_API_KEY}
    model: gemini-2.5-flash
    
adk:
  enabled: true
  mcp-server-url: http://localhost:8080/mcp
```

### 3.2 Create ADK Agent

**File**: `src/main/java/com/tripplanner/adk/ADKCoordinatorAgent.java`

```java
package com.tripplanner.adk;

import com.google.cloud.ai.adk.Agent;
import com.google.cloud.ai.adk.GenerateContentRequest;
import com.google.cloud.ai.adk.toolsets.MCPToolset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ADKCoordinatorAgent {
    
    private static final Logger logger = LoggerFactory.getLogger(ADKCoordinatorAgent.class);
    
    private final Agent agent;
    
    public ADKCoordinatorAgent(
        @Value("${google.ai.api-key}") String apiKey,
        @Value("${adk.mcp-server-url}") String mcpServerUrl
    ) {
        logger.info("Initializing ADK Coordinator Agent...");
        
        // Create MCP toolset
        MCPToolset toolset = MCPToolset.builder()
            .serverUrl(mcpServerUrl)
            .build();
        
        // Create ADK agent
        this.agent = Agent.builder()
            .apiKey(apiKey)
            .model("gemini-2.5-flash")
            .instructions("""
                You are a travel planning coordinator assistant.
                You have access to a tool called 'calculate_cost' that can estimate the total cost of an itinerary.
                When asked about costs, use this tool to provide accurate estimates.
                Always explain your reasoning before taking actions.
            """)
            .addToolset(toolset)
            .enableThoughts(true)  // Enable reasoning display
            .build();
        
        logger.info("ADK Agent initialized successfully");
    }
    
    public String execute(String userRequest) {
        logger.info("=== ADK Agent: Processing request ===");
        logger.info("Request: {}", userRequest);
        
        return agent.generateContent(
            GenerateContentRequest.builder()
                .prompt(userRequest)
                .includeThoughts(true)
                .build()
        );
    }
    
    public void executeWithStreaming(String userRequest, StreamingCallback callback) {
        logger.info("=== ADK Agent: Processing request (streaming) ===");
        
        agent.generateContentStream(
            GenerateContentRequest.builder()
                .prompt(userRequest)
                .includeThoughts(true)
                .build()
        )
        .forEach(chunk -> {
            if (chunk.hasThought()) {
                callback.onThought(chunk.getThought());
            }
            if (chunk.hasContent()) {
                callback.onToken(chunk.getContent());
            }
        });
    }
}
```

### 3.3 Test ADK Agent

**File**: `src/test/java/com/tripplanner/adk/ADKCoordinatorAgentTest.java`

```java
@SpringBootTest
class ADKCoordinatorAgentTest {
    
    @Autowired
    private ADKCoordinatorAgent agent;
    
    @Test
    void testSimpleQuery() {
        String response = agent.execute("What can you help me with?");
        assertNotNull(response);
        assertTrue(response.contains("travel") || response.contains("cost"));
    }
    
    @Test
    void testToolCall() {
        String response = agent.execute(
            "Calculate the cost of a simple itinerary with 3 hotel nights at $100 each"
        );
        assertNotNull(response);
        // Agent should have called the calculate_cost tool
        assertTrue(response.contains("$300") || response.contains("300"));
    }
}
```

---

## Step 4: Add Streaming Controller (30 minutes)

**File**: `src/main/java/com/tripplanner/controller/ADKStreamingController.java`

```java
package com.tripplanner.controller;

import com.tripplanner.adk.ADKCoordinatorAgent;
import com.tripplanner.dto.ChatRequest;
import com.tripplanner.service.ai.StreamingCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/v1/adk/stream")
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
                    new StreamingCallback() {
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
                                    .data(fullResponse));
                                emitter.complete();
                            } catch (IOException e) {
                                emitter.completeWithError(e);
                            }
                        }
                        
                        @Override
                        public void onError(Exception error) {
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

## Step 5: Frontend Testing (30 minutes)

### 5.1 Update API Endpoint

**File**: `frontend/src/services/chatApi.ts`

```typescript
// Add ADK streaming endpoint
export async function streamADKChat(message: string): Promise<EventSource> {
  const eventSource = new EventSource(
    `/api/v1/adk/stream?text=${encodeURIComponent(message)}`
  );
  return eventSource;
}
```

### 5.2 Test in Browser Console

```javascript
// Open browser console on your app
const es = new EventSource('/api/v1/adk/stream?text=' + encodeURIComponent('Calculate cost for a 3-day trip'));

es.addEventListener('thought', (e) => {
  console.log('THOUGHT:', e.data);
});

es.addEventListener('token', (e) => {
  console.log('TOKEN:', e.data);
});

es.addEventListener('done', (e) => {
  console.log('DONE:', e.data);
  es.close();
});
```

---

## Success Checklist

After completing all steps, verify:

- [ ] ✅ MCP endpoint lists `calculate_cost` tool
- [ ] ✅ Direct tool call via curl works
- [ ] ✅ ADK agent responds to basic queries
- [ ] ✅ ADK agent calls MCP tool automatically
- [ ] ✅ Streaming endpoint returns SSE events
- [ ] ✅ Browser console shows thoughts + tokens
- [ ] ✅ Existing `AgentStreamingCard` displays output

---

## Troubleshooting

### Issue: Dependencies not found

```bash
# Maven might need to update repositories
mvn clean install -U
```

### Issue: Gemini API quota exceeded

- Use smaller model: `gemini-2.5-flash` instead of `gemini-3`
- Enable free tier quota in Google Cloud Console

### Issue: MCP tool not discovered

- Check `mcpServerUrl` in application.yml
- Verify `/mcp/tools` endpoint returns tool list
- Check agent logs for connection errors

---

## What You'll See

**When working correctly**:

```
User: "Calculate cost for a 7-day Barcelona trip"

Thought: "I need to use the calculate_cost tool to estimate the cost"
Token: "Let"
Token: " me"
Token: " calculate"
Token: " the"
Token: " cost"
Token: "..."
Thought: "The tool returned $1,850 total"
Token: "Based"
Token: " on"
Token: " the"
Token: " calculation"
Token: ","
Token: " your"
Token: " 7"
Token: "-day"
Token: " trip"
Token: " will"
Token: " cost"
Token: " approximately"
Token: " $1,850"

DONE
```

---

## Next Steps After POC

1. ✅ **POC Successful** → Migrate remaining agents
2. ✅ Add more MCP tools (TransportAgent, ActivityAgent)
3. ✅ Deploy to staging
4. ✅ Full multi-agent streaming dashboard

**Estimated time to full production**: 8-13 more hours

---

**Ready to start? Begin with Step 1!** 🚀
