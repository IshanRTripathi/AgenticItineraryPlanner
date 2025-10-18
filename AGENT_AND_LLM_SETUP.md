# Agent & LLM Provider Setup Guide

## 🎯 Overview

This document explains how the agent system, LLM providers, and chat orchestration work together in the travel planner.

## 🔧 Architecture

### 1. **LLM Provider Layer** (NEW)

The system now has a bridge between the existing `AiClient` infrastructure and the `LLMService`:

```
Chat Request → OrchestratorService → LLMService → LLMProvider → AiClient → (Gemini/OpenRouter)
```

#### LLMProvider Implementations

**`GeminiLLMProvider`** (`src/main/java/com/tripplanner/service/GeminiLLMProvider.java`)
- Registers under the name `"gemini"`
- Supports model names: `gemini`, `gemini-pro`, `gemini-*`
- Delegates to the resilient `AiClient` infrastructure
- **This fixes the "No provider found for model type: gemini" error**

**`AiClientLLMProvider`** (`src/main/java/com/tripplanner/service/AiClientLLMProvider.java`)
- Registers under the name `"ai-client"`
- Acts as a fallback for any model type
- Universal adapter for the AI client system

#### How It Works

1. **LLMService** requests a model (e.g., `"gemini"`)
2. Looks up provider in `providerMap`:
   - Exact match: `providerMap.get("gemini")` → `GeminiLLMProvider`
   - Model support check: Calls `provider.supportsModel("gemini")`
   - Fallback: Uses first available provider
3. Provider delegates to `AiClient` which routes to:
   - **Gemini** (primary) if configured
   - **OpenRouter** (fallback) if Gemini unavailable
   - **NoopAiClient** (dev/test) if no keys

### 2. **Agent Registry**

Manages all available agents and their capabilities.

#### Registered Agents

| Agent Kind | Tasks | Data Sections | Priority | Implementation |
|-----------|-------|---------------|----------|---------------|
| `planner` | plan, create | itinerary, schedule | 5 (highest) | `PlannerAgent.java` |
| `EDITOR` | edit, modify, update | nodes, timing | 10 | `EditorAgent.java` |
| `enrichment` | enrich, enhance | location, photos, reviews | 20 | `EnrichmentAgent.java` |
| `places` | search, lookup | location, places | 30 | `PlacesAgent.java` |
| `booking` | book, reserve | booking | 40 | `BookingAgent.java` |
| `orchestrator` | orchestrate, coordinate | - | 1 (highest) | `OrchestratorService.java` |

#### Agent Capabilities

Each agent has:
- **Supported Tasks**: What the agent can do (e.g., `["edit", "modify", "update"]`)
- **Data Sections**: What data the agent operates on (e.g., `["nodes", "timing"]`)
- **Priority**: Lower number = higher priority (1-100)
- **Enabled Status**: Can be disabled without removing
- **Configuration**: Custom settings per agent

#### Agent Selection Flow

```java
// In OrchestratorService
1. Classify user intent → "modify_activity"
2. Convert to task type → "edit"
3. AgentRegistry.getAgentsForTask("edit")
   - Filters by enabled=true
   - Filters by supportedTasks.contains("edit")
   - Sorts by priority (ascending)
4. Returns: [EditorAgent (priority 10)]
5. Execute: EditorAgent.execute(AgentEvent)
```

### 3. **Chat Flow**

#### Step-by-Step Execution

**1. User sends message**: `"Move lunch to 2pm"`

**2. OrchestratorService.route()**
```java
// Classify intent using LLM
IntentResult intent = llmService.classifyIntent(text, context);
// Result: { intent: "modify_activity", confidence: 0.95 }
```

**3. Resolve target nodes**
```java
// Find what "lunch" refers to in the itinerary
List<NodeCandidate> candidates = resolveTargetNode(intent, itinerary);
// If multiple matches → needs disambiguation
// If one match → proceed
```

**4. Route to agent**
```java
// Convert intent to task type
String taskType = mapIntentToTaskType(intent); // "modify_activity" → "edit"

// Get capable agents
List<BaseAgent> agents = agentRegistry.getAgentsForTask(taskType);
// Returns: [EditorAgent]

// Build execution plan
AgentExecutionPlan plan = agentRegistry.buildExecutionPlan(taskType, intent, itinerary);
```

**5. Execute agent**
```java
AgentEvent event = new AgentEvent();
event.setKind(AgentEvent.AgentKind.EDITOR);
event.setNormalizedItinerary(itinerary);
event.setOriginalPrompt("Move lunch to 2pm");
event.setIntent(intent);

AgentEvent result = editorAgent.execute(event);
```

**6. Generate change set**
```java
ChangeSet changeSet = result.getChangeSet();
// Contains: operations to modify the lunch activity timing
```

**7. Create diff preview**
```java
ItineraryDiff diff = changeEngine.createDiff(itinerary, changeSet);
// Shows before/after comparison
```

**8. Return to user**
```java
ChatResponse response = new ChatResponse();
response.setIntent(intent.getIntent());
response.setMessage("I'll move lunch to 2:00 PM. Here's a preview:");
response.setChangeSet(changeSet);
response.setDiff(diff);
response.setApplied(false); // Not auto-applied
return response;
```

## 🚀 How to Use Different Agents

### Example 1: Modify Activity (EditorAgent)

**User**: `"Change dinner time to 7pm"`

**Flow**:
1. Intent: `modify_activity`
2. Task: `edit`
3. Agent: `EditorAgent`
4. Action: Updates timing of dinner node
5. Result: ChangeSet with timing modification

### Example 2: Add Place (PlacesAgent + EditorAgent)

**User**: `"Add a museum visit on day 2"`

**Flow**:
1. Intent: `add_activity`
2. Task: `search` (for museum) → `create` (new node)
3. Agents: 
   - `PlacesAgent` (search for museums)
   - `EditorAgent` (create new activity node)
4. Action: 
   - Search Google Places API
   - Create node with place details
   - Insert into day 2 schedule
5. Result: ChangeSet with new node addition

### Example 3: Enrich Existing Place (EnrichmentAgent)

**User**: `"Get more details about the Eiffel Tower visit"`

**Flow**:
1. Intent: `enrich_activity`
2. Task: `enrich`
3. Agent: `EnrichmentAgent`
4. Action:
   - Fetch place details from Google Places
   - Add photos, reviews, opening hours
   - Enhance location data
5. Result: ChangeSet with enriched metadata

### Example 4: Plan New Trip (PlannerAgent)

**User**: `"Plan a 3-day trip to Bali"`

**Flow**:
1. Intent: `create_itinerary`
2. Task: `plan`
3. Agent: `PlannerAgent`
4. Action:
   - Generate complete itinerary structure
   - Populate with activities per day
   - Set timings and logistics
5. Result: Complete NormalizedItinerary

## 🔧 Configuration

### LLM Provider Setup

**Environment Variables** (set these in your environment or `.env`):
```bash
# For Gemini (Google AI)
GOOGLE_AI_API_KEY=your_gemini_key_here

# For OpenRouter (fallback)
OPENROUTER_API_KEY=your_openrouter_key_here
```

**Application Properties** (`application.yml`):
```yaml
ai:
  provider: gemini  # Options: gemini, openrouter, auto
  model: gemini-pro # Default model name
```

**Spring Configuration** (`AiClientConfig.java`):
- Creates `ResilientAiClient` with fallback chain
- Primary: Configured provider (Gemini or OpenRouter)
- Fallback: Other available provider
- Last resort: `NoopAiClient` (for dev/testing)

### Agent Configuration

**Enable/Disable Agents**:
```java
// In code or via API
agentRegistry.disableAgent("booking"); // Disable booking agent
agentRegistry.enableAgent("booking");  // Re-enable
```

**Agent Priority** (in `AgentRegistry.extractCapabilities()`):
```java
case EDITOR:
    caps.setPriority(10); // High priority for editing
    break;
case enrichment:
    caps.setPriority(20); // Medium priority
    break;
```

Lower number = executed first when multiple agents support same task.

## 📊 Monitoring & Debugging

### LLM Provider Logs

**Startup**:
```
INFO LLMService - LLMService initialized with 2 providers
INFO LLMService - Registered LLM provider: gemini
INFO LLMService - Registered LLM provider: ai-client
```

**Request**:
```
DEBUG LLMService - Generating response with model: gemini
DEBUG GeminiLLMProvider - Generated 523 characters from Gemini
```

**Errors**:
```
ERROR LLMService - Error classifying intent
java.lang.RuntimeException: No provider found for model type: gemini
```
**Fix**: Ensure `GeminiLLMProvider` is registered (now auto-registered as `@Component`)

### Agent Registry Logs

**Startup**:
```
INFO AgentRegistry - === AGENT REGISTRY INITIALIZATION ===
INFO AgentRegistry - Found 5 agents to register
INFO AgentRegistry - Registered agent: EDITOR -> EditorAgent with capabilities: [edit, modify, update]
INFO AgentRegistry - Registered agent: planner -> PlannerAgent with capabilities: [plan, create]
INFO AgentRegistry - Total registered agents: 5
```

**Execution**:
```
INFO OrchestratorService - Routing chat request: text='Move lunch to 2pm'
DEBUG OrchestratorService - Classified intent: modify_activity (confidence: 0.95)
DEBUG AgentRegistry - Getting agents for task: edit
DEBUG AgentRegistry - Found 1 agents for task 'edit': [EditorAgent]
INFO EditorAgent - Executing edit task for activity modification
```

## 🐛 Troubleshooting

### Problem: "No provider found for model type: gemini"

**Cause**: `GeminiLLMProvider` not registered in Spring context

**Solutions**:
1. ✅ Ensure `GeminiLLMProvider.java` has `@Component` annotation (fixed)
2. ✅ Ensure `AiClientLLMProvider.java` has `@Component` annotation (fixed)
3. Restart backend to load new components
4. Check logs for "Registered LLM provider: gemini"

### Problem: "No suitable agents available for task type: X"

**Cause**: No agent registered with capability for task type `X`

**Solutions**:
1. Check `AgentRegistry` logs for registered agents
2. Verify agent capabilities in `extractCapabilities()`
3. Add new agent or extend existing agent's capabilities
4. Check agent is enabled: `agentRegistry.isAgentRegistered(kind)`

### Problem: AI client returns errors

**Cause**: Missing API keys or rate limits

**Solutions**:
1. Set `GOOGLE_AI_API_KEY` or `OPENROUTER_API_KEY`
2. Check AI client availability: `aiClient.isAvailable()`
3. Monitor fallback chain in `ResilientAiClient`
4. Check API quota/limits for your provider

### Problem: Chat doesn't classify intent correctly

**Cause**: LLM prompt or response format issues

**Solutions**:
1. Check `LLMService.buildIntentPrompt()` prompt template
2. Verify LLM response is valid JSON
3. Add more context to chat request
4. Tune LLM parameters (temperature, max_tokens)

## 📝 Adding New Agents

### Step 1: Create Agent Class

```java
@Component
public class MyCustomAgent extends BaseAgent {
    
    @Override
    public AgentEvent execute(AgentEvent event) {
        // Your agent logic here
        return event;
    }
}
```

### Step 2: Register in AgentRegistry

Add to `extractCapabilities()`:
```java
case MY_CUSTOM:
    caps.addSupportedTask("my_task");
    caps.addSupportedDataSection("my_data");
    caps.setPriority(25);
    break;
```

### Step 3: Add Intent Mapping

In `OrchestratorService.mapIntentToTaskType()`:
```java
case "my_intent":
    return "my_task";
```

### Step 4: Test

```java
ChatRequest request = new ChatRequest();
request.setText("Trigger my custom intent");

ChatResponse response = orchestratorService.route(request);
// Should route to MyCustomAgent
```

## 🎯 Current Status

### ✅ Working
- LLM provider bridge (`GeminiLLMProvider`, `AiClientLLMProvider`)
- Agent registry with capabilities
- Intent classification
- Chat routing to agents
- Change set generation
- Diff preview

### ⚠️ Needs Configuration
- API keys for Gemini/OpenRouter
- Specific agent implementations (some may be placeholders)
- Fine-tuned intent prompts
- Agent-specific LLM parameters

### 🚧 Next Steps
1. Test with real API keys
2. Implement missing agent execute() methods
3. Add more intent types and agent capabilities
4. Create agent-specific LLM prompts
5. Add agent execution telemetry

## 📚 Key Files

### LLM Layer
- `LLMService.java` - LLM orchestration
- `LLMProvider.java` - Provider interface
- `GeminiLLMProvider.java` - Gemini provider (NEW)
- `AiClientLLMProvider.java` - Universal provider (NEW)
- `AiClient.java` - AI client interface
- `AiClientConfig.java` - AI client configuration

### Agent Layer
- `AgentRegistry.java` - Agent management
- `AgentCapabilities.java` - Capability model
- `AgentExecutionPlan.java` - Execution planning
- `BaseAgent.java` - Agent base class
- `*Agent.java` - Specific agent implementations

### Orchestration
- `OrchestratorService.java` - Chat routing
- `ChangeEngine.java` - Change application
- `ItinerariesController.java` - REST endpoints

## 🔗 Related Documentation
- `CHAT_FIX_SUMMARY.md` - Chat 404 fix
- `CHAT_E2E_TEST_PLAN.md` - Testing guide
- `COMPREHENSIVE_REMAINING_TASKS.md` - Task tracking






