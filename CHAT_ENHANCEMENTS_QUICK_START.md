# Chat Enhancements - Quick Start Guide

## 🎯 What We're Building

4 critical features to make chat 10x better:

1. **Real-time Typing Indicators** - "🤔 Understanding your request..."
2. **Cost Impact Preview** - Show cost changes BEFORE applying
3. **Conflict Detection** - Warn about time overlaps, travel issues
4. **Place Suggestions** - Show museums/restaurants with photos & ratings

## ✅ Current State

**What Works:**
- Basic chat (send message, get response)
- Change preview (shows what will change)
- Disambiguation (handles ambiguous requests)

**What's Missing:**
- No real-time progress updates
- No cost preview before applying
- No conflict warnings in UI
- No place search with rich data
- No multi-turn conversations

## 🔧 Available Tools (Not Integrated Yet)

These tools exist but aren't being used:
- `/api/v1/tools/calculate-cost` ✅
- `/api/v1/tools/check-conflicts` ✅
- `/api/v1/tools/generate-node-id` ✅
- `/api/v1/tools/validate-schema` ✅

**Missing Tool:**
- `/api/v1/tools/search-places` ❌ (need to create)

## 📋 Implementation Order

### **Week 1: Foundation (2 days)**
1. Add SSE for real-time updates
2. Integrate cost preview tool
3. Build basic UI components

**Files to Create:**
- `ChatProgressService.java` (SSE manager)
- `ChatProgressEvent.java` (progress DTO)
- `useChatProgress.ts` (React hook)
- `CostImpactDisplay.tsx` (UI component)

**Files to Modify:**
- `ChatController.java` (add SSE endpoint)
- `OrchestratorService.java` (send progress updates)
- `EditorAgent.java` (call cost tool)
- `ChatMessage.tsx` (show progress & cost)

### **Week 2: Conflict & Search (2-3 days)**
1. Integrate conflict detection
2. Add place search to GooglePlacesService
3. Create PlaceSearchAgent
4. Build place suggestion UI

**Files to Create:**
- `PlaceSuggestion.java` (DTO)
- `PlaceSearchAgent.java` (new agent)
- `ConflictDisplay.tsx` (UI)
- `PlaceSuggestionCard.tsx` (UI)

**Files to Modify:**
- `GooglePlacesService.java` (add searchPlaces)
- `ToolsController.java` (add search endpoint)
- `EditorAgent.java` (call conflict tool)
- `ChatMessage.tsx` (show conflicts & places)

### **Week 3: Multi-turn (2 days)**
1. Add conversation state management
2. Handle clarification questions
3. Merge context across turns

**Files to Create:**
- `ConversationManager.java` (state manager)
- `ConversationContext.java` (DTO)

**Files to Modify:**
- `OrchestratorService.java` (use conversation context)
- `ChatRequest.java` (add conversationId)
- `UnifiedItineraryContext.tsx` (track conversation)

## 🚀 Quick Implementation Steps

### **Step 1: Enable Real-time Updates (4 hours)**

```java
// 1. Create ChatProgressService.java
@Service
public class ChatProgressService {
    private Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();
    
    public SseEmitter createEmitter(String sessionId) {
        SseEmitter emitter = new SseEmitter(30000L);
        emitters.put(sessionId, emitter);
        return emitter;
    }
    
    public void sendProgress(String sessionId, String message, int progress) {
        // Send SSE event
    }
}

// 2. Add endpoint to ChatController.java
@GetMapping("/progress/{sessionId}")
public SseEmitter streamProgress(@PathVariable String sessionId) {
    return chatProgressService.createEmitter(sessionId);
}

// 3. Send updates in OrchestratorService.java
chatProgressService.sendProgress(sessionId, "🤔 Understanding...", 10);
// ... do work
chatProgressService.sendProgress(sessionId, "✨ Generating...", 50);
```

```typescript
// 4. Create useChatProgress.ts hook
export function useChatProgress(sessionId: string) {
  const [progress, setProgress] = useState(null);
  
  useEffect(() => {
    const eventSource = new EventSource(`/api/v1/chat/progress/${sessionId}`);
    eventSource.onmessage = (e) => setProgress(JSON.parse(e.data));
    return () => eventSource.close();
  }, [sessionId]);
  
  return progress;
}

// 5. Use in ChatMessage.tsx
const progress = useChatProgress(sessionId);
{progress && <div>{progress.message} ({progress.progress}%)</div>}
```

### **Step 2: Add Cost Preview (3 hours)**

```java
// 1. In EditorAgent.java - BEFORE applying changes
if (editorToolsEnabled) {
    // Calculate current cost
    CostCalculationResult currentCost = calculateCostViaTool(itineraryId, partySize);
    
    // Apply to copy and calculate new cost
    NormalizedItinerary copy = deepCopy(itinerary);
    applyChangesToCopy(copy, changeSet);
    CostCalculationResult newCost = calculateCostOnCopy(copy, partySize);
    
    // Add to response
    response.setCostImpact(new CostImpact(
        currentCost.getTotalCostPerPerson(),
        newCost.getTotalCostPerPerson(),
        newCost.getTotalCostPerPerson() - currentCost.getTotalCostPerPerson()
    ));
}
```

```typescript
// 2. Create CostImpactDisplay.tsx
export function CostImpactDisplay({ costImpact }) {
  return (
    <div className="border rounded p-3">
      <div>Current: ${costImpact.currentCost}</div>
      <div>After: ${costImpact.newCost} 
        ({costImpact.difference > 0 ? '+' : ''}${costImpact.difference})
      </div>
      {costImpact.exceedsBudget && <div>⚠️ Exceeds budget!</div>}
    </div>
  );
}

// 3. Use in ChatMessage.tsx
{m.costImpact && <CostImpactDisplay costImpact={m.costImpact} />}
```

### **Step 3: Add Conflict Detection (3 hours)**

```java
// 1. In EditorAgent.java - BEFORE applying
if (editorToolsEnabled) {
    ConflictCheckResult conflicts = checkConflictsViaTool(itineraryId, changeSet);
    
    if (conflicts.isHasConflicts()) {
        response.setConflicts(conflicts.getConflicts());
        response.setAutoFixSuggestions(conflicts.getAutoFixSuggestions());
        
        // Don't auto-apply if errors
        if (hasErrorLevelConflicts(conflicts) && request.isAutoApply()) {
            response.setApplied(false);
        }
    }
}
```

```typescript
// 2. Create ConflictDisplay.tsx
export function ConflictDisplay({ conflicts, onAutoFix }) {
  return (
    <div className="border-yellow-300 bg-yellow-50 rounded p-3">
      <div>⚠️ Conflicts Detected</div>
      {conflicts.map(c => <div key={c.id}>{c.message}</div>)}
      <Button onClick={onAutoFix}>Auto-Fix</Button>
    </div>
  );
}

// 3. Use in ChatMessage.tsx
{m.conflicts && <ConflictDisplay conflicts={m.conflicts} />}
```

### **Step 4: Add Place Search (6 hours)**

```java
// 1. Add to GooglePlacesService.java
public List<PlaceSuggestion> searchPlaces(String query, String location, int maxResults) {
    String url = BASE_URL + "/textsearch/json?query=" + query + " in " + location;
    PlaceSearchResponse response = restTemplate.getForObject(url, PlaceSearchResponse.class);
    return response.getResults().stream()
        .limit(maxResults)
        .map(this::toPlaceSuggestion)
        .collect(Collectors.toList());
}

// 2. Add endpoint to ToolsController.java
@PostMapping("/search-places")
public ResponseEntity<PlaceSearchResult> searchPlaces(@RequestBody PlaceSearchRequest req) {
    List<PlaceSuggestion> suggestions = placesService.searchPlaces(
        req.getQuery(), req.getLocation(), 3
    );
    return ResponseEntity.ok(PlaceSearchResult.success(suggestions));
}

// 3. Create PlaceSearchAgent.java
@Component
public class PlaceSearchAgent extends BaseAgent {
    @Override
    protected <T> T executeInternal(String itineraryId, AgentRequest<T> request) {
        // Call search tool
        PlaceSearchResult result = restTemplate.postForObject(
            "http://localhost:8080/api/v1/tools/search-places",
            searchRequest,
            PlaceSearchResult.class
        );
        
        // Return suggestions
        ChatResponse response = new ChatResponse();
        response.setPlaceSuggestions(result.getSuggestions());
        return (T) response;
    }
}
```

```typescript
// 4. Create PlaceSuggestionCard.tsx
export function PlaceSuggestionCard({ suggestion, onSelect }) {
  return (
    <div className="border rounded p-3">
      <img src={suggestion.photos[0]} className="h-20 w-full object-cover" />
      <h4>{suggestion.name}</h4>
      <div>⭐ {suggestion.rating} ({suggestion.userRatingsTotal} reviews)</div>
      <div>💰 ${suggestion.estimatedCost}</div>
      <Button onClick={() => onSelect(suggestion)}>Select</Button>
    </div>
  );
}

// 5. Use in ChatMessage.tsx
{m.placeSuggestions?.map(s => 
  <PlaceSuggestionCard suggestion={s} onSelect={onSelectPlace} />
)}
```

## 📊 Effort Summary

- **Week 1:** 2 days (Real-time + Cost)
- **Week 2:** 2-3 days (Conflict + Search)
- **Week 3:** 2 days (Multi-turn)
- **Total:** 6-7 days

## 🎯 Priority Order

1. **Real-time indicators** (biggest UX improvement)
2. **Cost preview** (prevents budget surprises)
3. **Conflict detection** (prevents broken itineraries)
4. **Place suggestions** (better decisions)
5. **Multi-turn** (natural conversations)

## 🚀 Start Here

Begin with **Step 1 & 2** (Real-time + Cost Preview) - they give you the biggest bang for your buck and can be done in 1 day.

See `CHAT_ENHANCEMENTS_ROADMAP.md` for complete details.
