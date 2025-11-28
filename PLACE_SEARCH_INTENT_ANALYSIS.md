# Place Search Intent Classification - Critical Analysis

**Date:** November 28, 2025  
**Status:** 🔴 CRITICAL ISSUE IDENTIFIED  
**User Request:** "show me more tourist places for day 3"

---

## 🎯 EXPECTED BEHAVIOR (Per Documentation)

According to `PHASE2_PLACE_SEARCH_IMPLEMENTATION.md`:

**Expected Flow:**
```
User: "show me more tourist places for day 3"
↓
IntentClassificationService → Detects "SEARCH_PLACE" intent
↓
PlaceSearchAgent → Executes place search
↓
GooglePlacesService.searchPlaces() → Returns 3 suggestions with photos
↓
Frontend → Displays PlaceSuggestionCard components
```

**Expected Response:**
- 3 place suggestion cards with photos
- Ratings, addresses, costs, opening hours
- "Select This Place" buttons
- Rich visual experience

---

## ❌ ACTUAL BEHAVIOR (From Backend Logs)

### **1. Intent Classification - WRONG TASK TYPE**

**Log Evidence:**
```json
{
  "intent": "QueryDayActivities",
  "taskType": "explain",  ← WRONG! Should be "search" or trigger place search
  "entities": {
    "day": "3",
    "activity_type": "tourist places"
  },
  "confidence": 0.95
}
```

**Problem:** Intent was classified as `explain` instead of triggering place search functionality.

### **2. Agent Routing - WRONG AGENT**

**Log Evidence:**
```
ExplainAgent - === AGENT EXECUTION START ===
Agent Kind: EXPLAINER
```

**Problem:** Request was routed to `ExplainAgent` instead of `PlaceSearchAgent`.

### **3. Response - EXISTING ITINERARY DATA ONLY**

**Actual Response:**
```
Certainly! On Day 3, your itinerary includes a couple of wonderful tourist places in Lucerne:

* Chapel Bridge (Kapellbrücke): CHF 28.00
* Old Town Lucerne (Altstadt) Exploration: CHF 28.00

Remember, Day 3 also involves a train journey...
```

**Problem:** 
- ❌ No place search performed
- ❌ No Google Places API call
- ❌ No new suggestions
- ❌ No photos, ratings, or rich data
- ✅ Only explained existing itinerary items

---

## 🔍 ROOT CAUSE ANALYSIS

### **Issue 1: Intent Classification Prompt Doesn't Include Place Search**

**Current Prompt (from logs):**
```
=== SUPPORTED TASK TYPES ===
1. TASK TYPE: edit
2. TASK TYPE: plan
3. TASK TYPE: explain  ← User request matched this
4. TASK TYPE: book
5. TASK TYPE: enrich
```

**Missing:** No `search` or `place_search` task type!

**Why It Failed:**
- User said "show me more tourist places"
- Prompt has no "search for new places" category
- AI defaulted to "explain" (closest match for "show me")
- Intent classification has no awareness of place search feature

### **Issue 2: No PlaceSearchAgent Integration**

**Evidence from logs:**
```
AgentRegistry - Created execution plan for 'explain': 1 agents, primary: ExplainAgent
```

**Missing:**
- PlaceSearchAgent not registered in AgentRegistry
- No routing logic for place search intent
- No integration between IntentClassificationService and place search

### **Issue 3: Semantic Ambiguity**

**User Request:** "show me more tourist places for day 3"

**Ambiguous Interpretation:**
1. ✅ "Show me what tourist places are ALREADY in day 3" → `explain` (current behavior)
2. ❌ "Show me MORE tourist places I could ADD to day 3" → `search` (expected behavior)

**Problem:** Without explicit "add", "find", or "search" keywords, the AI interprets as explanation request.

---

## 📊 COMPARISON: EXPECTED vs ACTUAL

| Aspect | Expected | Actual | Status |
|--------|----------|--------|--------|
| **Intent** | SEARCH_PLACE | QueryDayActivities | ❌ Wrong |
| **Task Type** | search | explain | ❌ Wrong |
| **Agent** | PlaceSearchAgent | ExplainAgent | ❌ Wrong |
| **API Call** | Google Places API | None | ❌ Missing |
| **Response** | 3 new suggestions with photos | Existing itinerary items | ❌ Wrong |
| **UI** | PlaceSuggestionCard components | Plain text | ❌ Wrong |
| **User Value** | Discover new places | See what's already planned | ❌ Low |

---

## 🚨 CRITICAL GAPS

### **Gap 1: Intent Classification System**
- ❌ No "search" task type in prompt
- ❌ No place search intent detection
- ❌ No pattern matching for "show me more", "find", "suggest"

### **Gap 2: Agent Integration**
- ❌ PlaceSearchAgent not registered
- ❌ No routing from OrchestratorService to PlaceSearchAgent
- ❌ No connection between intent and agent

### **Gap 3: Semantic Understanding**
- ❌ "show me more" interpreted as "explain existing"
- ❌ No context awareness (user already knows what's in itinerary)
- ❌ No disambiguation ("more" = additional, not existing)

---

## 🎯 WHAT NEEDS TO HAPPEN

### **Fix 1: Update Intent Classification Prompt**

**Add to IntentClassificationService prompt:**
```
6. TASK TYPE: search
   Use for: Finding new places, activities, or options to add
   Examples:
   - "Show me more tourist places" → search
   - "Find museums for day 2" → search
   - "Suggest restaurants in Lucerne" → search
   - "What other attractions are nearby?" → search
   - "I want to visit a museum" → search
```

### **Fix 2: Register PlaceSearchAgent**

**In AgentRegistry:**
```java
@Component
public class PlaceSearchAgent extends BaseAgent {
    @Override
    public AgentCapabilities getCapabilities() {
        AgentCapabilities capabilities = new AgentCapabilities();
        capabilities.addSupportedTask("search");  // ← Add this
        capabilities.setPriority(5);
        capabilities.setChatEnabled(true);
        return capabilities;
    }
}
```

### **Fix 3: Add Intent Detection Logic**

**In IntentClassificationService:**
```java
// Detect place search intent
if (text.matches(".*(?:show me more|find|suggest|search for).*(?:places|museums|restaurants|attractions).*")) {
    return IntentResult.builder()
        .intent("SEARCH_PLACE")
        .taskType("search")  // ← New task type
        .entities(Map.of(
            "placeType", extractPlaceType(text),
            "day", day
        ))
        .confidence(0.9)
        .build();
}
```

### **Fix 4: Update OrchestratorService Routing**

**Add routing logic:**
```java
if ("search".equals(taskType)) {
    // Route to PlaceSearchAgent
    AgentExecutionPlan plan = agentRegistry.createExecutionPlan("search", itineraryId);
    return executePlan(plan, request);
}
```

---

## 🔄 CORRECT FLOW (After Fixes)

```
User: "show me more tourist places for day 3"
↓
IntentClassificationService
  ├─ Detects "show me more" + "tourist places"
  ├─ Matches search pattern
  └─ Returns: {intent: "SEARCH_PLACE", taskType: "search"}
↓
OrchestratorService
  ├─ Routes to PlaceSearchAgent (taskType = "search")
  └─ Passes: {placeType: "tourist_attraction", day: 3, location: "Lucerne"}
↓
PlaceSearchAgent
  ├─ Calls /api/v1/tools/search-places
  └─ Receives 3 suggestions with photos
↓
ChatResponse
  ├─ Sets placeSuggestions field
  └─ Returns to frontend
↓
Frontend (ChatMessage.tsx)
  ├─ Detects placeSuggestions in response
  ├─ Renders 3 PlaceSuggestionCard components
  └─ Shows photos, ratings, "Select" buttons
```

---

## 📈 IMPACT ASSESSMENT

### **Current State:**
- ❌ Place search feature exists but is **NEVER TRIGGERED**
- ❌ Users cannot discover new places via chat
- ❌ All "show me" requests go to ExplainAgent
- ❌ No value from Phase 2 implementation

### **After Fixes:**
- ✅ Place search triggered correctly
- ✅ Users can discover new places with photos
- ✅ Rich UI experience with PlaceSuggestionCard
- ✅ Smart caching reduces API costs by 90%
- ✅ Phase 2 feature fully functional

---

## 🎯 PRIORITY ACTIONS

### **Immediate (Critical):**
1. ✅ Update IntentClassificationService prompt (add "search" task type)
2. ✅ Register PlaceSearchAgent in AgentRegistry
3. ✅ Add place search pattern detection
4. ✅ Update OrchestratorService routing

### **Testing:**
1. Test: "show me more tourist places for day 3"
2. Verify: Intent = "SEARCH_PLACE", taskType = "search"
3. Verify: PlaceSearchAgent executes
4. Verify: Google Places API called
5. Verify: 3 suggestions returned with photos
6. Verify: PlaceSuggestionCard components render

### **Documentation:**
1. Update PHASE2_PLACE_SEARCH_IMPLEMENTATION.md with intent patterns
2. Add troubleshooting guide for intent classification
3. Document semantic patterns for place search

---

## 💡 KEY INSIGHTS

### **Insight 1: Feature Implementation ≠ Feature Integration**
- Backend API works perfectly (`/api/v1/tools/search-places`)
- Frontend components work perfectly (`PlaceSuggestionCard.tsx`)
- **BUT:** No connection between user intent and feature execution

### **Insight 2: Intent Classification is the Gatekeeper**
- All chat requests go through IntentClassificationService
- If intent classification fails, feature is invisible
- Prompt engineering is critical for feature discoverability

### **Insight 3: Semantic Ambiguity Matters**
- "show me" can mean "explain existing" OR "find new"
- Context matters: user already knows what's in itinerary
- Need explicit patterns for "discovery" vs "explanation"

---

## 🎓 LESSONS LEARNED

1. **Test End-to-End:** Don't just test API endpoints, test user flows
2. **Intent Classification First:** Before building features, ensure they're discoverable
3. **Semantic Patterns:** Document exact phrases that trigger each feature
4. **Integration Testing:** Test OrchestratorService routing, not just individual agents

---

**Document End**
