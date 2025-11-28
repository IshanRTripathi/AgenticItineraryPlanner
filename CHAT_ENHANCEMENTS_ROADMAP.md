# Chat Enhancements Roadmap
## Real-time Updates, Cost Preview, Conflict Detection & Multi-turn Conversations

**Date:** November 28, 2025  
**Status:** 🎯 READY TO IMPLEMENT  
**Priority Features:** 4 Critical Enhancements

---

## 📋 EXECUTIVE SUMMARY

**Current State Analysis:**
- ✅ Basic chat works (ChatController, OrchestratorService, EditorAgent)
- ✅ Disambiguation exists (NodeCandidate system)
- ✅ Change preview exists (ChangeSet, ItineraryDiff)
- ❌ No real-time typing indicators
- ❌ No cost impact preview before applying
- ❌ No conflict warnings in UI
- ❌ No place suggestions with photos/ratings
- ❌ No multi-turn conversation support

**What We're Building:**
1. Real-time typing indicators (WebSocket/SSE)
2. Cost impact preview (before applying changes)
3. Conflict detection with warnings
4. Place suggestions with rich data (photos, ratings)
5. Multi-turn conversation context

---

## 🔍 CURRENT IMPLEMENTATION REVIEW

### **Available Tools (ToolsController)**

✅ `/api/v1/tools/calculate-cost` - Calculate total cost with breakdown
✅ `/api/v1/tools/check-conflicts` - Detect time/budget conflicts
✅ `/api/v1/tools/generate-node-id` - Generate unique node IDs
✅ `/api/v1/tools/validate-schema` - Validate JSON schemas
✅ `/api/v1/tools/check-user-constraints` - Check user preferences
✅ `/api/v1/tools/convert-currency` - Currency conversion
✅ `/api/v1/tools/calculate-distance` - Distance between locations
✅ `/api/v1/tools/suggest-restaurants` - Restaurant suggestions
✅ `/api/v1/tools/geocode` - Geocode addresses
✅ `/api/v1/tools/validate-timing` - Validate timing constraints
✅ `/api/v1/tools/get-transport-options` - Transport options
✅ `/api/v1/tools/optimize-route` - Route optimization
✅ `/api/v1/tools/check-opening-hours` - Check opening hours
✅ `/api/v1/tools/get-itinerary-summary` - Get summary
✅ `/api/v1/tools/get-day-details` - Get day details

### **Available Services**
✅ `GooglePlacesService` - Get place details (photos, reviews, ratings)
✅ `ChangeEngine` - Apply changes with validation
✅ `BudgetTracker` - Calculate costs with breakdown
✅ `ConflictResolver` - Detect conflicts (assumed to exist)
✅ `ChatHistoryService` - Store/retrieve chat history
✅ `IntentClassificationService` - Classify user intent

### **Current Chat Flow**

```
User → ChatController.route()
     → OrchestratorService.route()
     → IntentClassificationService.classifyIntent()
     → EditorAgent.execute()
     → ChangeEngine.apply()
     → ChatResponse (with diff, warnings, candidates)
```

### **Frontend Chat Components**
✅ `ChatMessage.tsx` - Displays messages with markdown
✅ `ItineraryChangesDisplay.tsx` - Shows change preview
✅ `DetailedDiffView.tsx` - Detailed diff view
✅ `ChatTypes.ts` - TypeScript interfaces
✅ `chatApi.ts` - API client

### **What's Missing**
❌ Real-time progress updates (no WebSocket/SSE)
❌ Cost preview before applying (tool exists but not integrated)
❌ Conflict warnings in UI (tool exists but not integrated)
❌ Place search tool (GooglePlacesService only has getPlaceDetails)
❌ Multi-turn conversation state management
❌ Rich place suggestions (photos, ratings, reviews)

---

## 🎯 FEATURE 1: REAL-TIME TYPING INDICATORS

### **Goal**
Show users what the AI is doing in real-time:
- "🤔 Understanding your request..."
- "🔍 Searching for activities..."
- "✨ Generating suggestions..."
- "✅ Ready to apply changes"

### **Implementation Strategy: Server-Sent Events (SSE)**


**Why SSE over WebSocket:**
- Simpler implementation (HTTP-based)
- One-way communication (server → client)
- Auto-reconnect built-in
- Works with existing infrastructure

### **Backend Changes**

#### **1. Create Progress Event DTO**
**File:** `src/main/java/com/tripplanner/dto/ChatProgressEvent.java` (NEW)
```java
public class ChatProgressEvent {
    private String type; // "progress", "complete", "error"
    private String stage; // "intent", "generation", "validation", "applying"
    private String message; // User-friendly message
    private int progress; // 0-100
    private long timestamp;
    
    // Constructors, getters, setters
}
```

#### **2. Create SSE Manager Service**
**File:** `src/main/java/com/tripplanner/service/ChatProgressService.java` (NEW)
```java
@Service
public class ChatProgressService {
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();
    
    public SseEmitter createEmitter(String sessionId) {
        SseEmitter emitter = new SseEmitter(30000L); // 30 second timeout
        emitters.put(sessionId, emitter);
        
        emitter.onCompletion(() -> emitters.remove(sessionId));
        emitter.onTimeout(() -> emitters.remove(sessionId));
        
        return emitter;
    }
    
    public void sendProgress(String sessionId, ChatProgressEvent event) {
        SseEmitter emitter = emitters.get(sessionId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                    .name("progress")
                    .data(event));
            } catch (IOException e) {
                emitters.remove(sessionId);
            }
        }
    }
}
```


#### **3. Add SSE Endpoint to ChatController**
**File:** `src/main/java/com/tripplanner/controller/ChatController.java` (MODIFY)
```java
@GetMapping("/progress/{sessionId}")
public SseEmitter streamProgress(@PathVariable String sessionId) {
    return chatProgressService.createEmitter(sessionId);
}
```

#### **4. Integrate Progress Updates in OrchestratorService**
**File:** `src/main/java/com/tripplanner/service/OrchestratorService.java` (MODIFY)
```java
public ChatResponse route(ChatRequest request) {
    String sessionId = request.getSessionId(); // Add to ChatRequest
    
    // Stage 1: Intent classification
    chatProgressService.sendProgress(sessionId, 
        new ChatProgressEvent("progress", "intent", 
            "🤔 Understanding your request...", 10));
    
    IntentResult intent = intentClassificationService.classifyIntent(...);
    
    // Stage 2: Generation
    chatProgressService.sendProgress(sessionId,
        new ChatProgressEvent("progress", "generation",
            "✨ Generating suggestions...", 50));
    
    // ... rest of flow with progress updates
    
    // Stage 3: Complete
    chatProgressService.sendProgress(sessionId,
        new ChatProgressEvent("complete", "done",
            "✅ Ready!", 100));
    
    return response;
}
```

### **Frontend Changes**

#### **5. Add SSE Hook**
**File:** `frontend/src/hooks/useChatProgress.ts` (NEW)
```typescript
export function useChatProgress(sessionId: string) {
  const [progress, setProgress] = useState<ChatProgressEvent | null>(null);
  
  useEffect(() => {
    const eventSource = new EventSource(
      `${API_BASE_URL}/chat/progress/${sessionId}`
    );
    
    eventSource.addEventListener('progress', (e) => {
      const event = JSON.parse(e.data);
      setProgress(event);
    });
    
    return () => eventSource.close();
  }, [sessionId]);
  
  return progress;
}
```


#### **6. Update Chat UI to Show Progress**
**File:** `frontend/src/components/chat/ChatMessage.tsx` (MODIFY)
```typescript
// Add progress indicator component
{isLoading && progress && (
  <div className="flex items-center gap-2 text-sm text-gray-600">
    <Loader2 className="h-4 w-4 animate-spin" />
    <span>{progress.message}</span>
    <div className="flex-1 bg-gray-200 rounded-full h-2">
      <div 
        className="bg-primary h-2 rounded-full transition-all"
        style={{ width: `${progress.progress}%` }}
      />
    </div>
  </div>
)}
```

**Estimated Time:** 6-8 hours
**Files to Create:** 2 new files
**Files to Modify:** 3 files

---

## 💰 FEATURE 2: COST IMPACT PREVIEW

### **Goal**
Show cost changes BEFORE applying:
```
💰 Cost Impact:
Current: $1,500 per person
After changes: $1,650 per person (+$150)
⚠️ Exceeds budget by $150

Breakdown:
+ Louvre Museum: +$20
+ Fancy Restaurant: +$150
- Removed Cafe: -$20
```

### **Implementation Strategy: Tool Integration**

**Tool Already Exists:** ✅ `/api/v1/tools/calculate-cost`

### **Backend Changes**

#### **1. Add Cost Preview to ChatResponse**
**File:** `src/main/java/com/tripplanner/dto/ChatResponse.java` (MODIFY)
```java
public class ChatResponse {
    // ... existing fields
    private CostImpact costImpact; // NEW
    
    public static class CostImpact {
        private double currentCost;
        private double newCost;
        private double difference;
        private String currency;
        private boolean exceedsBudget;
        private Map<String, Double> breakdown; // category → cost change
    }
}
```


#### **2. Integrate Cost Calculation in EditorAgent**
**File:** `src/main/java/com/tripplanner/agents/EditorAgent.java` (MODIFY)
```java
@Override
protected <T> T executeInternal(String itineraryId, AgentRequest<T> request) {
    // ... existing code to generate ChangeSet
    
    // BEFORE applying changes, calculate cost impact
    if (editorToolsEnabled) {
        CostImpact costImpact = calculateCostImpact(itineraryId, changeSet);
        response.setCostImpact(costImpact);
    }
    
    // ... rest of execution
}

private CostImpact calculateCostImpact(String itineraryId, ChangeSet changeSet) {
    // 1. Get current cost
    CostCalculationResult currentCost = calculateCostViaTool(itineraryId, partySize);
    
    // 2. Apply changes to a COPY of itinerary (don't save)
    NormalizedItinerary copy = deepCopy(itinerary);
    applyChangesToCopy(copy, changeSet);
    
    // 3. Calculate new cost
    CostCalculationResult newCost = calculateCostOnCopy(copy, partySize);
    
    // 4. Build impact
    return CostImpact.builder()
        .currentCost(currentCost.getTotalCostPerPerson())
        .newCost(newCost.getTotalCostPerPerson())
        .difference(newCost.getTotalCostPerPerson() - currentCost.getTotalCostPerPerson())
        .currency(currentCost.getCurrency())
        .exceedsBudget(newCost.getTotalCostPerPerson() > budget)
        .breakdown(calculateBreakdown(currentCost, newCost))
        .build();
}
```

### **Frontend Changes**

#### **3. Add Cost Impact Display Component**
**File:** `frontend/src/components/chat/CostImpactDisplay.tsx` (NEW)
```typescript
interface CostImpactProps {
  costImpact: CostImpact;
}

export function CostImpactDisplay({ costImpact }: CostImpactProps) {
  const isIncrease = costImpact.difference > 0;
  const exceedsBudget = costImpact.exceedsBudget;
  
  return (
    <div className={`border rounded-lg p-3 ${exceedsBudget ? 'border-red-300 bg-red-50' : 'border-gray-200'}`}>
      <div className="flex items-center gap-2 mb-2">
        <DollarSign className="h-4 w-4" />
        <span className="font-semibold">Cost Impact</span>
      </div>
      
      <div className="space-y-1 text-sm">
        <div className="flex justify-between">
          <span className="text-gray-600">Current:</span>
          <span>{formatCurrency(costImpact.currentCost, costImpact.currency)}</span>
        </div>
        <div className="flex justify-between font-medium">
          <span className="text-gray-600">After changes:</span>
          <span className={isIncrease ? 'text-red-600' : 'text-green-600'}>
            {formatCurrency(costImpact.newCost, costImpact.currency)}
            <span className="ml-1">
              ({isIncrease ? '+' : ''}{formatCurrency(costImpact.difference, costImpact.currency)})
            </span>
          </span>
        </div>
        
        {exceedsBudget && (
          <div className="flex items-center gap-1 text-red-600 mt-2">
            <AlertCircle className="h-3 w-3" />
            <span className="text-xs">Exceeds budget by {formatCurrency(costImpact.difference, costImpact.currency)}</span>
          </div>
        )}
      </div>
    </div>
  );
}
```


#### **4. Integrate in ChatMessage Component**
**File:** `frontend/src/components/chat/ChatMessage.tsx` (MODIFY)
```typescript
// Show cost impact for proposed changes
{showPreview && m.costImpact && (
  <CostImpactDisplay costImpact={m.costImpact} />
)}
```

**Estimated Time:** 4-6 hours
**Files to Create:** 1 new file
**Files to Modify:** 2 files

---

## ⚠️ FEATURE 3: CONFLICT DETECTION & RESOLUTION

### **Goal**
Show conflicts BEFORE applying with auto-fix options:
```
⚠️ Conflicts Detected:
1. Time Overlap: Louvre (2pm-5pm) conflicts with Eiffel Tower (4pm-6pm)
2. Travel Time: Not enough time to travel (need 45 min, have 15 min)
3. Closed: Museum closed on Tuesdays

[Auto-Fix] [Adjust Manually] [Cancel]
```

### **Implementation Strategy: Tool Integration + UI**

**Tool Already Exists:** ✅ `/api/v1/tools/check-conflicts`

### **Backend Changes**

#### **1. Enhance ConflictCheckResult DTO**
**File:** `src/main/java/com/tripplanner/dto/tools/ConflictCheckResult.java` (MODIFY)
```java
public class ConflictCheckResult {
    private boolean hasConflicts;
    private List<Conflict> conflicts;
    private List<AutoFixSuggestion> autoFixSuggestions; // NEW
    
    public static class Conflict {
        private String type; // "TIME_OVERLAP", "TRAVEL_TIME", "CLOSED", "BUDGET"
        private String severity; // "ERROR", "WARNING", "INFO"
        private String message;
        private List<String> affectedNodeIds;
        private Map<String, Object> details;
    }
    
    public static class AutoFixSuggestion {
        private String description;
        private ChangeSet fixChangeSet; // Changes to fix the conflict
    }
}
```


#### **2. Integrate Conflict Check in EditorAgent**
**File:** `src/main/java/com/tripplanner/agents/EditorAgent.java` (MODIFY)
```java
@Override
protected <T> T executeInternal(String itineraryId, AgentRequest<T> request) {
    // ... generate ChangeSet
    
    // Check conflicts BEFORE applying
    if (editorToolsEnabled) {
        ConflictCheckResult conflicts = checkConflictsViaTool(itineraryId, changeSet);
        
        if (conflicts.isHasConflicts()) {
            // Add conflicts to response
            response.setConflicts(conflicts.getConflicts());
            response.setAutoFixSuggestions(conflicts.getAutoFixSuggestions());
            
            // Don't auto-apply if there are ERROR-level conflicts
            boolean hasErrors = conflicts.getConflicts().stream()
                .anyMatch(c -> "ERROR".equals(c.getSeverity()));
            
            if (hasErrors && request.isAutoApply()) {
                response.setApplied(false);
                response.setMessage("Cannot apply changes due to conflicts. Please review.");
            }
        }
    }
    
    // ... rest of execution
}
```

#### **3. Add Conflicts to ChatResponse**
**File:** `src/main/java/com/tripplanner/dto/ChatResponse.java` (MODIFY)
```java
public class ChatResponse {
    // ... existing fields
    private List<Conflict> conflicts; // NEW
    private List<AutoFixSuggestion> autoFixSuggestions; // NEW
}
```

### **Frontend Changes**

#### **4. Create Conflict Display Component**
**File:** `frontend/src/components/chat/ConflictDisplay.tsx` (NEW)
```typescript
interface ConflictDisplayProps {
  conflicts: Conflict[];
  autoFixSuggestions: AutoFixSuggestion[];
  onAutoFix: (suggestion: AutoFixSuggestion) => void;
  onManualFix: () => void;
}

export function ConflictDisplay({ conflicts, autoFixSuggestions, onAutoFix, onManualFix }: ConflictDisplayProps) {
  return (
    <div className="border border-yellow-300 bg-yellow-50 rounded-lg p-3">
      <div className="flex items-center gap-2 mb-2">
        <AlertCircle className="h-4 w-4 text-yellow-600" />
        <span className="font-semibold text-yellow-800">Conflicts Detected</span>
      </div>
      
      <div className="space-y-2 mb-3">
        {conflicts.map((conflict, idx) => (
          <div key={idx} className="flex items-start gap-2 text-sm">
            <span className={`font-medium ${
              conflict.severity === 'ERROR' ? 'text-red-600' : 'text-yellow-600'
            }`}>
              {idx + 1}.
            </span>
            <div className="flex-1">
              <div className="font-medium">{getConflictTitle(conflict.type)}</div>
              <div className="text-gray-700">{conflict.message}</div>
            </div>
          </div>
        ))}
      </div>
      
      {autoFixSuggestions.length > 0 && (
        <div className="flex gap-2">
          {autoFixSuggestions.map((suggestion, idx) => (
            <Button
              key={idx}
              size="sm"
              variant="outline"
              onClick={() => onAutoFix(suggestion)}
            >
              ✨ {suggestion.description}
            </Button>
          ))}
          <Button size="sm" variant="outline" onClick={onManualFix}>
            ✏️ Adjust Manually
          </Button>
        </div>
      )}
    </div>
  );
}
```


#### **5. Integrate in ChatMessage Component**
**File:** `frontend/src/components/chat/ChatMessage.tsx` (MODIFY)
```typescript
// Show conflicts for proposed changes
{showPreview && m.conflicts && m.conflicts.length > 0 && (
  <ConflictDisplay
    conflicts={m.conflicts}
    autoFixSuggestions={m.autoFixSuggestions || []}
    onAutoFix={(suggestion) => onApplyAutoFix(messageId, suggestion)}
    onManualFix={() => onManualFix(messageId)}
  />
)}
```

**Estimated Time:** 6-8 hours
**Files to Create:** 1 new file
**Files to Modify:** 3 files

---

## 🏛️ FEATURE 4: PLACE SUGGESTIONS WITH RICH DATA

### **Goal**
When user asks "I want to visit a museum on day 2", show:
```
🏛️ Museum Suggestions for Day 2:

1. Louvre Museum ⭐ 4.7 (150K reviews)
   📍 Rue de Rivoli, Paris
   💰 €17 per person
   ⏰ 9am - 6pm (Closed Tuesdays)
   [📸 3 photos] [Select]

2. Musée d'Orsay ⭐ 4.6 (85K reviews)
   📍 1 Rue de la Légion d'Honneur
   💰 €16 per person
   ⏰ 9:30am - 6pm
   [📸 5 photos] [Select]

3. Centre Pompidou ⭐ 4.5 (45K reviews)
   📍 Place Georges-Pompidou
   💰 €14 per person
   ⏰ 11am - 9pm
   [📸 4 photos] [Select]
```

### **Implementation Strategy: New Tool + Enhanced Response**

**Missing:** ❌ Place search tool (only getPlaceDetails exists)

### **Backend Changes**

#### **1. Add Place Search to GooglePlacesService**
**File:** `src/main/java/com/tripplanner/service/GooglePlacesService.java` (MODIFY)
```java
/**
 * Search for places by text query and location.
 * Returns top N results with photos, ratings, and details.
 */
public List<PlaceSuggestion> searchPlaces(String query, String location, String type, int maxResults) {
    logger.info("Searching places: query={}, location={}, type={}", query, location, type);
    
    // Build URL for Text Search API
    String url = UriComponentsBuilder.fromHttpUrl(BASE_URL + "/textsearch/json")
        .queryParam("query", query + " in " + location)
        .queryParam("type", type) // e.g., "museum", "restaurant"
        .queryParam("key", apiKey)
        .build(false)
        .toUriString();
    
    // Make request
    PlaceSearchResponse response = makeRequestWithRetry(url, PlaceSearchResponse.class);
    
    if (response.isSuccessful() && response.getResults() != null) {
        return response.getResults().stream()
            .limit(maxResults)
            .map(this::toPlaceSuggestion)
            .collect(Collectors.toList());
    }
    
    return List.of();
}

private PlaceSuggestion toPlaceSuggestion(PlaceSearchResult result) {
    return PlaceSuggestion.builder()
        .placeId(result.getPlaceId())
        .name(result.getName())
        .address(result.getFormattedAddress())
        .rating(result.getRating())
        .userRatingsTotal(result.getUserRatingsTotal())
        .priceLevel(result.getPriceLevel())
        .photos(result.getPhotos())
        .types(result.getTypes())
        .geometry(result.getGeometry())
        .build();
}
```


#### **2. Create Place Suggestion DTO**
**File:** `src/main/java/com/tripplanner/dto/PlaceSuggestion.java` (NEW)
```java
public class PlaceSuggestion {
    private String placeId;
    private String name;
    private String address;
    private Double rating;
    private Integer userRatingsTotal;
    private Integer priceLevel;
    private List<Photo> photos;
    private List<String> types;
    private Geometry geometry;
    private String openingHours;
    private Double estimatedCost;
    private Integer estimatedDuration; // minutes
    
    // Getters, setters, builder
}
```

#### **3. Add Place Search Tool Endpoint**
**File:** `src/main/java/com/tripplanner/controller/ToolsController.java` (MODIFY)
```java
@PostMapping("/search-places")
public ResponseEntity<PlaceSearchResult> searchPlaces(
        @RequestBody PlaceSearchRequest request,
        @RequestHeader(value = "X-Agent-Name", required = false) String agentName) {
    try {
        logToolUsage("search-places", agentName, request.getItineraryId());
        
        // Get location context from itinerary
        Optional<NormalizedItinerary> itinerary = itineraryJsonService.getItinerary(request.getItineraryId());
        String location = itinerary.map(i -> i.getDestination()).orElse(request.getLocation());
        
        // Search places
        List<PlaceSuggestion> suggestions = placesService.searchPlaces(
            request.getQuery(),
            location,
            request.getType(),
            request.getMaxResults() != null ? request.getMaxResults() : 3
        );
        
        // Enrich with cost estimates
        for (PlaceSuggestion suggestion : suggestions) {
            enrichWithCostEstimate(suggestion, request.getBudgetTier());
        }
        
        return ResponseEntity.ok(PlaceSearchResult.success(suggestions));
        
    } catch (Exception e) {
        logger.error("Place search failed", e);
        return ResponseEntity.status(500).body(PlaceSearchResult.error(e.getMessage()));
    }
}
```

#### **4. Integrate in IntentClassificationService**
**File:** `src/main/java/com/tripplanner/service/IntentClassificationService.java` (MODIFY)
```java
public IntentResult classifyIntent(String text, String itineraryId, Integer day) {
    // ... existing classification
    
    // Detect place search intent
    if (text.matches(".*(?:visit|see|go to|add).*(?:museum|restaurant|attraction|place).*")) {
        // Extract place type
        String placeType = extractPlaceType(text); // "museum", "restaurant", etc.
        
        // Mark as SEARCH_PLACE intent
        return IntentResult.builder()
            .intent("SEARCH_PLACE")
            .taskType("search")
            .entities(Map.of(
                "placeType", placeType,
                "day", day
            ))
            .confidence(0.9)
            .build();
    }
    
    // ... rest of classification
}
```


#### **5. Create PlaceSearchAgent**
**File:** `src/main/java/com/tripplanner/agents/PlaceSearchAgent.java` (NEW)
```java
@Component
public class PlaceSearchAgent extends BaseAgent {
    
    private final GooglePlacesService placesService;
    private final RestTemplate restTemplate;
    
    @Override
    public AgentCapabilities getCapabilities() {
        AgentCapabilities capabilities = new AgentCapabilities();
        capabilities.addSupportedTask("search");
        capabilities.setPriority(5);
        capabilities.setChatEnabled(true);
        return capabilities;
    }
    
    @Override
    protected <T> T executeInternal(String itineraryId, AgentRequest<T> request) {
        // Extract search parameters
        String placeType = (String) request.getParameters().get("placeType");
        Integer day = (Integer) request.getParameters().get("day");
        
        // Call search tool
        PlaceSearchRequest searchRequest = new PlaceSearchRequest();
        searchRequest.setItineraryId(itineraryId);
        searchRequest.setQuery(placeType);
        searchRequest.setType(placeType);
        searchRequest.setMaxResults(3);
        
        PlaceSearchResult result = restTemplate.postForObject(
            "http://localhost:8080/api/v1/tools/search-places",
            searchRequest,
            PlaceSearchResult.class
        );
        
        // Build response with suggestions
        ChatResponse response = new ChatResponse();
        response.setIntent("SEARCH_PLACE");
        response.setMessage("Here are some " + placeType + " suggestions for Day " + day + ":");
        response.setPlaceSuggestions(result.getSuggestions());
        response.setNeedsDisambiguation(true); // User needs to select
        
        return (T) response;
    }
}
```

#### **6. Add Place Suggestions to ChatResponse**
**File:** `src/main/java/com/tripplanner/dto/ChatResponse.java` (MODIFY)
```java
public class ChatResponse {
    // ... existing fields
    private List<PlaceSuggestion> placeSuggestions; // NEW
}
```

### **Frontend Changes**

#### **7. Create Place Suggestion Card Component**
**File:** `frontend/src/components/chat/PlaceSuggestionCard.tsx` (NEW)
```typescript
interface PlaceSuggestionCardProps {
  suggestion: PlaceSuggestion;
  onSelect: (suggestion: PlaceSuggestion) => void;
}

export function PlaceSuggestionCard({ suggestion, onSelect }: PlaceSuggestionCardProps) {
  return (
    <div className="border rounded-lg p-3 hover:border-primary transition-colors">
      {/* Photo Gallery */}
      {suggestion.photos && suggestion.photos.length > 0 && (
        <div className="flex gap-2 mb-2 overflow-x-auto">
          {suggestion.photos.slice(0, 3).map((photo, idx) => (
            <img
              key={idx}
              src={getPhotoUrl(photo.photoReference, 200)}
              alt={suggestion.name}
              className="h-20 w-20 object-cover rounded"
            />
          ))}
        </div>
      )}
      
      {/* Name and Rating */}
      <div className="flex items-start justify-between mb-1">
        <h4 className="font-semibold text-sm">{suggestion.name}</h4>
        {suggestion.rating && (
          <div className="flex items-center gap-1 text-xs">
            <Star className="h-3 w-3 fill-yellow-400 text-yellow-400" />
            <span>{suggestion.rating}</span>
            <span className="text-gray-500">({formatNumber(suggestion.userRatingsTotal)})</span>
          </div>
        )}
      </div>
      
      {/* Address */}
      <div className="flex items-center gap-1 text-xs text-gray-600 mb-2">
        <MapPin className="h-3 w-3" />
        <span>{suggestion.address}</span>
      </div>
      
      {/* Details */}
      <div className="flex items-center gap-3 text-xs mb-2">
        {suggestion.estimatedCost && (
          <div className="flex items-center gap-1">
            <DollarSign className="h-3 w-3" />
            <span>${suggestion.estimatedCost} per person</span>
          </div>
        )}
        {suggestion.openingHours && (
          <div className="flex items-center gap-1">
            <Clock className="h-3 w-3" />
            <span>{suggestion.openingHours}</span>
          </div>
        )}
      </div>
      
      {/* Select Button */}
      <Button
        size="sm"
        className="w-full"
        onClick={() => onSelect(suggestion)}
      >
        Select This Place
      </Button>
    </div>
  );
}
```


#### **8. Integrate in ChatMessage Component**
**File:** `frontend/src/components/chat/ChatMessage.tsx` (MODIFY)
```typescript
// Show place suggestions
{m.placeSuggestions && m.placeSuggestions.length > 0 && (
  <div className="mt-3 space-y-2">
    <div className="text-sm font-medium text-gray-700">
      Select a place to add:
    </div>
    {m.placeSuggestions.map((suggestion, idx) => (
      <PlaceSuggestionCard
        key={idx}
        suggestion={suggestion}
        onSelect={(s) => onSelectPlace(s, m.day)}
      />
    ))}
  </div>
)}
```

**Estimated Time:** 10-12 hours
**Files to Create:** 3 new files
**Files to Modify:** 5 files

---

## 🔄 FEATURE 5: MULTI-TURN CONVERSATIONS

### **Goal**
Support follow-up questions and context:
```
User: "Add a museum"
AI: "Which day would you like to add a museum?"
User: "Day 2"
AI: [Shows museum suggestions for Day 2]
User: "The Louvre"
AI: "✅ Added Louvre Museum to Day 2 at 2pm"
```

### **Implementation Strategy: Conversation State Management**

### **Backend Changes**

#### **1. Create Conversation Context DTO**
**File:** `src/main/java/com/tripplanner/dto/ConversationContext.java` (NEW)
```java
public class ConversationContext {
    private String conversationId;
    private String itineraryId;
    private String lastIntent;
    private Map<String, Object> pendingData; // Collected data from previous turns
    private List<String> missingFields; // What we still need to ask
    private long timestamp;
    
    // Example: User said "add museum" but didn't specify day
    // pendingData = {"placeType": "museum"}
    // missingFields = ["day"]
}
```


#### **2. Create Conversation Manager Service**
**File:** `src/main/java/com/tripplanner/service/ConversationManager.java` (NEW)
```java
@Service
public class ConversationManager {
    private final Map<String, ConversationContext> conversations = new ConcurrentHashMap<>();
    
    public ConversationContext getOrCreate(String conversationId, String itineraryId) {
        return conversations.computeIfAbsent(conversationId, 
            id -> new ConversationContext(id, itineraryId));
    }
    
    public void updateContext(String conversationId, String intent, Map<String, Object> data) {
        ConversationContext context = conversations.get(conversationId);
        if (context != null) {
            context.setLastIntent(intent);
            context.getPendingData().putAll(data);
            context.setTimestamp(System.currentTimeMillis());
        }
    }
    
    public boolean needsClarification(ConversationContext context, String intent) {
        // Check if we have all required fields for this intent
        return switch (intent) {
            case "INSERT_PLACE" -> !context.getPendingData().containsKey("day") 
                                || !context.getPendingData().containsKey("placeType");
            case "MOVE_TIME" -> !context.getPendingData().containsKey("nodeId") 
                             || !context.getPendingData().containsKey("time");
            default -> false;
        };
    }
    
    public String generateClarificationQuestion(ConversationContext context, String intent) {
        List<String> missing = context.getMissingFields();
        
        if (missing.contains("day")) {
            return "Which day would you like to add this to?";
        }
        if (missing.contains("placeType")) {
            return "What type of place are you looking for? (museum, restaurant, attraction, etc.)";
        }
        if (missing.contains("time")) {
            return "What time would you like to schedule this?";
        }
        
        return "Could you provide more details?";
    }
    
    public void clearContext(String conversationId) {
        conversations.remove(conversationId);
    }
}
```

#### **3. Integrate in OrchestratorService**
**File:** `src/main/java/com/tripplanner/service/OrchestratorService.java` (MODIFY)
```java
public ChatResponse route(ChatRequest request) {
    String conversationId = request.getConversationId(); // Add to ChatRequest
    
    // Get or create conversation context
    ConversationContext context = conversationManager.getOrCreate(conversationId, request.getItineraryId());
    
    // Classify intent with context
    IntentResult intent = intentClassificationService.classifyIntent(
        request.getText(), 
        context // Pass context for better classification
    );
    
    // Merge with pending data from previous turns
    Map<String, Object> allData = new HashMap<>(context.getPendingData());
    allData.putAll(intent.getEntities());
    
    // Check if we need clarification
    if (conversationManager.needsClarification(context, intent.getIntent())) {
        // Update context with what we have so far
        conversationManager.updateContext(conversationId, intent.getIntent(), allData);
        
        // Ask clarification question
        String question = conversationManager.generateClarificationQuestion(context, intent.getIntent());
        
        return ChatResponse.clarification(intent.getIntent(), question);
    }
    
    // We have all data - proceed with execution
    conversationManager.updateContext(conversationId, intent.getIntent(), allData);
    
    // ... rest of routing logic
    
    // Clear context after successful execution
    conversationManager.clearContext(conversationId);
    
    return response;
}
```

#### **4. Add Conversation ID to ChatRequest**
**File:** `src/main/java/com/tripplanner/dto/ChatRequest.java` (MODIFY)
```java
public class ChatRequest {
    // ... existing fields
    private String conversationId; // NEW - generated by frontend
    private String sessionId; // NEW - for SSE
}
```

### **Frontend Changes**

#### **5. Add Conversation State to Chat Context**
**File:** `frontend/src/contexts/UnifiedItineraryContext.tsx` (MODIFY)
```typescript
// Add to chat state
const [conversationId, setConversationId] = useState<string>(generateUUID());
const [sessionId, setSessionId] = useState<string>(generateUUID());

// Reset conversation on new topic
const resetConversation = () => {
  setConversationId(generateUUID());
};

// Include in chat requests
const sendMessage = async (text: string) => {
  const request: ChatRequest = {
    itineraryId,
    text,
    conversationId, // Include conversation ID
    sessionId, // Include session ID for SSE
    // ... other fields
  };
  
  // ... send request
};
```

**Estimated Time:** 8-10 hours
**Files to Create:** 2 new files
**Files to Modify:** 3 files

---

## 📊 IMPLEMENTATION SUMMARY

### **Total Effort Estimate**
- Feature 1 (Real-time Indicators): 6-8 hours
- Feature 2 (Cost Preview): 4-6 hours
- Feature 3 (Conflict Detection): 6-8 hours
- Feature 4 (Place Suggestions): 10-12 hours
- Feature 5 (Multi-turn): 8-10 hours

**Total: 34-44 hours (4-5 days)**

### **Files to Create (13 new files)**


**Backend (8 files):**
1. `src/main/java/com/tripplanner/dto/ChatProgressEvent.java`
2. `src/main/java/com/tripplanner/service/ChatProgressService.java`
3. `src/main/java/com/tripplanner/dto/PlaceSuggestion.java`
4. `src/main/java/com/tripplanner/agents/PlaceSearchAgent.java`
5. `src/main/java/com/tripplanner/dto/ConversationContext.java`
6. `src/main/java/com/tripplanner/service/ConversationManager.java`
7. `src/main/java/com/tripplanner/dto/tools/PlaceSearchRequest.java`
8. `src/main/java/com/tripplanner/dto/tools/PlaceSearchResult.java`

**Frontend (5 files):**
1. `frontend/src/hooks/useChatProgress.ts`
2. `frontend/src/components/chat/CostImpactDisplay.tsx`
3. `frontend/src/components/chat/ConflictDisplay.tsx`
4. `frontend/src/components/chat/PlaceSuggestionCard.tsx`
5. `frontend/src/types/ConversationTypes.ts`

### **Files to Modify (11 files)**

**Backend (7 files):**
1. `src/main/java/com/tripplanner/controller/ChatController.java` - Add SSE endpoint
2. `src/main/java/com/tripplanner/service/OrchestratorService.java` - Add progress updates, conversation management
3. `src/main/java/com/tripplanner/agents/EditorAgent.java` - Add cost preview, conflict check
4. `src/main/java/com/tripplanner/dto/ChatResponse.java` - Add costImpact, conflicts, placeSuggestions
5. `src/main/java/com/tripplanner/dto/ChatRequest.java` - Add conversationId, sessionId
6. `src/main/java/com/tripplanner/service/GooglePlacesService.java` - Add searchPlaces method
7. `src/main/java/com/tripplanner/controller/ToolsController.java` - Add search-places endpoint

**Frontend (4 files):**
1. `frontend/src/components/chat/ChatMessage.tsx` - Integrate all new displays
2. `frontend/src/contexts/UnifiedItineraryContext.tsx` - Add conversation state
3. `frontend/src/types/ChatTypes.ts` - Add new types
4. `frontend/src/services/chatApi.ts` - Add SSE support

---

## 🎯 IMPLEMENTATION PHASES

### **Phase 1: Foundation (Week 1)**
**Priority:** HIGH  
**Time:** 2 days

**Tasks:**
1. ✅ Add SSE infrastructure (ChatProgressService, endpoint)
2. ✅ Add cost preview integration (EditorAgent + UI)
3. ✅ Test basic real-time updates
4. ✅ Test cost preview display

**Deliverables:**
- Real-time typing indicators working
- Cost preview showing before apply
- Basic UI components

### **Phase 2: Conflict & Search (Week 2)**
**Priority:** HIGH  
**Time:** 2-3 days

**Tasks:**
1. ✅ Integrate conflict detection (EditorAgent + UI)
2. ✅ Add place search (GooglePlacesService + tool)
3. ✅ Create PlaceSearchAgent
4. ✅ Build place suggestion UI
5. ✅ Test conflict warnings
6. ✅ Test place search flow

**Deliverables:**
- Conflict warnings showing in chat
- Place suggestions with photos/ratings
- Auto-fix suggestions working

### **Phase 3: Multi-turn (Week 3)**
**Priority:** MEDIUM  
**Time:** 2 days

**Tasks:**
1. ✅ Add conversation management (ConversationManager)
2. ✅ Integrate in OrchestratorService
3. ✅ Add frontend conversation state
4. ✅ Test multi-turn flows
5. ✅ Handle edge cases

**Deliverables:**
- Multi-turn conversations working
- Context preserved across messages
- Clarification questions working

### **Phase 4: Polish & Testing (Week 4)**
**Priority:** MEDIUM  
**Time:** 1-2 days

**Tasks:**
1. ✅ End-to-end testing
2. ✅ Performance optimization
3. ✅ Error handling
4. ✅ UI polish
5. ✅ Documentation

**Deliverables:**
- All features tested and working
- Performance optimized
- User documentation

---

## 🔧 TOOLS NEEDED

### **Existing Tools (Ready to Use)**
✅ `/api/v1/tools/calculate-cost` - Cost calculation
✅ `/api/v1/tools/check-conflicts` - Conflict detection
✅ `GooglePlacesService.getPlaceDetails()` - Place details

### **New Tools (Need to Create)**
❌ `/api/v1/tools/search-places` - Place search
❌ `GooglePlacesService.searchPlaces()` - Text search API
❌ `ConversationManager` - Conversation state
❌ `ChatProgressService` - SSE progress updates

### **Tool Integration Status**
⚠️ Tools exist but NOT integrated in EditorAgent:
- `calculate-cost` - Available but not called
- `check-conflicts` - Available but not called
- `validate-schema` - Available but not called
- `generate-node-id` - Available but not called

**Action Required:** Implement tool integration in EditorAgent (see earlier analysis)

---

## 📝 TESTING CHECKLIST

### **Feature 1: Real-time Indicators**
- [ ] SSE connection established
- [ ] Progress updates received
- [ ] Progress bar animates
- [ ] Messages update in real-time
- [ ] Connection handles errors
- [ ] Auto-reconnect works

### **Feature 2: Cost Preview**
- [ ] Cost calculated before apply
- [ ] Shows current vs new cost
- [ ] Shows breakdown by category
- [ ] Warns if exceeds budget
- [ ] Updates when changes modified
- [ ] Handles currency conversion

### **Feature 3: Conflict Detection**
- [ ] Detects time overlaps
- [ ] Detects travel time issues
- [ ] Detects closed venues
- [ ] Detects budget violations
- [ ] Shows auto-fix suggestions
- [ ] Auto-fix applies correctly
- [ ] Manual fix opens editor

### **Feature 4: Place Suggestions**
- [ ] Search returns results
- [ ] Shows photos correctly
- [ ] Shows ratings and reviews
- [ ] Shows opening hours
- [ ] Shows estimated cost
- [ ] Select adds to itinerary
- [ ] Handles no results

### **Feature 5: Multi-turn**
- [ ] Context preserved across turns
- [ ] Clarification questions asked
- [ ] Answers merged with context
- [ ] Completes after all data collected
- [ ] Context cleared after completion
- [ ] Handles conversation timeout

---

## 🚀 DEPLOYMENT PLAN

### **Prerequisites**
1. ✅ Feature flags enabled
2. ✅ Google Places API key configured
3. ✅ Database migrations (if needed)
4. ✅ Frontend build updated

### **Rollout Strategy**
1. **Phase 1:** Deploy to staging
2. **Phase 2:** Test with internal users
3. **Phase 3:** Deploy to 10% of users
4. **Phase 4:** Monitor metrics
5. **Phase 5:** Deploy to 100%

### **Rollback Plan**
- Feature flags can disable each feature independently
- Database changes are backward compatible
- Frontend gracefully handles missing backend features

---

## 📊 SUCCESS METRICS

### **Performance**
- Chat response time: < 3s (with progress indicators)
- Cost preview calculation: < 500ms
- Conflict detection: < 200ms
- Place search: < 1s
- SSE latency: < 100ms

### **User Experience**
- Conflict detection accuracy: > 95%
- Place suggestion relevance: > 90%
- Multi-turn completion rate: > 80%
- User satisfaction: > 4.5/5

### **Technical**
- Error rate: < 2%
- SSE connection stability: > 99%
- Cache hit rate: > 70%
- API cost reduction: > 30%

---

## 🎉 CONCLUSION

This roadmap provides a complete implementation plan for 4 critical chat enhancements:

1. ✅ **Real-time typing indicators** - Better UX, user confidence
2. ✅ **Cost impact preview** - Budget awareness, fewer surprises
3. ✅ **Conflict detection** - Prevent broken itineraries, auto-fix
4. ✅ **Place suggestions** - Rich data, better decisions
5. ✅ **Multi-turn conversations** - Natural interaction, less typing

**Total Effort:** 4-5 days  
**Impact:** 10x better chat experience  
**Risk:** LOW (feature flags, incremental rollout)

**Next Step:** Start with Phase 1 (Real-time + Cost Preview) for immediate impact.

---

**Document End**
