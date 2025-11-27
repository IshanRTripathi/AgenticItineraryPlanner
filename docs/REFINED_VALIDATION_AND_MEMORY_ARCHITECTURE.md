# Refined Validation & Memory Architecture

**Date:** 2025-11-27  
**Status:** Final Design  
**Philosophy:** Single Responsibility + Centralized Memory

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                     System Architecture                      │
├─────────────────────────────────────────────────────────────┤
│                                                               │
│  ┌──────────────────┐              ┌──────────────────┐     │
│  │ ValidationAdvisor│◄────────────►│  MemoryAgent     │     │
│  │     Agent        │  Uses Memory │  (Central Hub)   │     │
│  │ (Orchestrator)   │              │                  │     │
│  └────────┬─────────┘              └────────┬─────────┘     │
│           │                                  │               │
│           │ Calls                            │ Stores/       │
│           │                                  │ Retrieves     │
│           ▼                                  ▼               │
│  ┌──────────────────┐              ┌──────────────────┐     │
│  │ Validation Tools │              │  Memory Store    │     │
│  │  (5 Stateless)   │              │  (Firestore)     │     │
│  └──────────────────┘              └──────────────────┘     │
│                                                               │
└─────────────────────────────────────────────────────────────┘
```

---

## Part 1: MemoryAgent (Refined)

### Single Responsibility
**Store, retrieve, and learn from ALL user data across the entire system.**

### Memory Categories

#### 1. User Profile (Core Identity)
```json
{
  "userId": "user_123",
  "category": "PROFILE",
  "type": "TRAVEL_STYLE",
  "data": {
    "pace": "relaxed",
    "budgetFlexibility": "moderate",
    "planningStyle": "detailed",
    "riskTolerance": "low"
  },
  "confidence": 0.85,
  "learnedFrom": ["itin_1", "itin_2", "itin_3"],
  "lastUpdated": "2025-11-27T10:00:00Z"
}
```

**When to Update:**
- After each completed itinerary
- When user explicitly states preferences
- When patterns reach high confidence (>0.8)

**Used By:** All planning agents (Skeleton, DayByDay, Activity, Meal, Transport)

---

#### 2. Dietary & Health (Critical Constraints)
```json
{
  "userId": "user_123",
  "category": "DIETARY",
  "type": "RESTRICTION",
  "data": {
    "restrictions": ["vegetarian", "gluten-free"],
    "allergies": ["peanuts"],
    "preferences": ["local_cuisine", "street_food"],
    "strictness": "strict"
  },
  "source": "USER_STATED",
  "createdAt": "2025-01-15T10:00:00Z",
  "sticky": true,
  "neverExpires": true
}
```

**When to Update:**
- User explicitly states dietary needs
- User corrects dietary violations
- NEVER auto-update (too critical)

**Used By:** MealAgent, ValidationAdvisor, ActivityAgent (food tours)

---

#### 3. Validation Preferences (What User Cares About)
```json
{
  "userId": "user_123",
  "category": "VALIDATION",
  "type": "DISMISSAL",
  "data": {
    "issueType": "MISSING_BREAKFAST",
    "reason": "Hotel includes breakfast",
    "applyToFuture": true,
    "scope": "ALL_ITINERARIES",
    "neverShowAgain": true
  },
  "createdAt": "2025-11-27T10:00:00Z",
  "expiresAt": "2026-11-27T10:00:00Z"
}
```

**When to Update:**
- User dismisses validation issue
- User clicks "Don't show again"
- User applies auto-fix (implies acceptance)

**Used By:** ValidationAdvisor

---

#### 4. Activity Preferences (What User Enjoys)
```json
{
  "userId": "user_123",
  "category": "ACTIVITY",
  "type": "PREFERENCE",
  "data": {
    "preferredTypes": ["cultural", "historical", "nature"],
    "avoidTypes": ["extreme_sports", "nightlife"],
    "pacePreference": "2-3_per_day",
    "durationPreference": "60-90_minutes"
  },
  "confidence": 0.78,
  "learnedFrom": ["itin_1", "itin_2", "itin_3", "itin_4"],
  "lastUpdated": "2025-11-27T10:00:00Z"
}
```

**When to Update:**
- After each itinerary (analyze activity choices)
- User adds/removes activities (immediate signal)
- User rates activities (if rating system exists)

**Used By:** ActivityAgent, SkeletonPlannerAgent

---

#### 5. Budget Behavior (Spending Patterns)
```json
{
  "userId": "user_123",
  "category": "BUDGET",
  "type": "BEHAVIOR",
  "data": {
    "typicalBudgetPerDay": 150,
    "flexibilityLevel": "moderate",
    "splurgeCategories": ["meals", "unique_experiences"],
    "savingCategories": ["transport", "accommodation"],
    "warningThreshold": 0.85
  },
  "confidence": 0.82,
  "basedOnItineraries": 5,
  "lastUpdated": "2025-11-27T10:00:00Z"
}
```

**When to Update:**
- After each itinerary (analyze spending)
- User adjusts budget warnings
- User makes budget-related edits

**Used By:** CostEstimatorAgent, ValidationAdvisor, all planning agents

---

#### 6. Timing Patterns (Schedule Preferences)
```json
{
  "userId": "user_123",
  "category": "TIMING",
  "type": "PATTERN",
  "data": {
    "morningPerson": true,
    "preferredStartTime": "08:00",
    "preferredEndTime": "20:00",
    "bufferPreference": "tight",
    "breakPreference": "short_frequent"
  },
  "confidence": 0.91,
  "evidence": {
    "morningActivities": 18,
    "afternoonActivities": 12,
    "eveningActivities": 4
  },
  "lastUpdated": "2025-11-27T10:00:00Z"
}
```

**When to Update:**
- After each itinerary (analyze timing choices)
- User adjusts activity times
- User dismisses timing warnings

**Used By:** DayByDayPlannerAgent, ActivityAgent, ValidationAdvisor

---

#### 7. Transport Preferences (How User Travels)
```json
{
  "userId": "user_123",
  "category": "TRANSPORT",
  "type": "PREFERENCE",
  "data": {
    "preferredModes": ["train", "walking"],
    "avoidModes": ["bus"],
    "comfortLevel": "standard",
    "willingToWalk": "up_to_20_min",
    "priceVsSpeed": "balanced"
  },
  "confidence": 0.75,
  "learnedFrom": ["itin_1", "itin_2", "itin_3"],
  "lastUpdated": "2025-11-27T10:00:00Z"
}
```

**When to Update:**
- After each itinerary (analyze transport choices)
- User changes transport modes
- User adds/removes transport nodes

**Used By:** TransportAgent, CityAllocationAgent

---

#### 8. Meal Preferences (Dining Style)
```json
{
  "userId": "user_123",
  "category": "MEAL",
  "type": "PREFERENCE",
  "data": {
    "cuisinePreferences": ["local", "asian", "mediterranean"],
    "mealImportance": {
      "breakfast": "low",
      "lunch": "medium",
      "dinner": "high"
    },
    "diningStyle": "casual",
    "priceRange": "mid_range",
    "skipBreakfast": true,
    "skipBreakfastReason": "hotel_includes"
  },
  "confidence": 0.88,
  "lastUpdated": "2025-11-27T10:00:00Z"
}
```

**When to Update:**
- After each itinerary (analyze meal choices)
- User adds/removes meals
- User dismisses meal-related warnings

**Used By:** MealAgent, ValidationAdvisor

---

#### 9. Explicit Instructions (User Commands)
```json
{
  "userId": "user_123",
  "category": "INSTRUCTION",
  "type": "EXPLICIT",
  "data": {
    "context": "MEAL_PLANNING",
    "instruction": "Always include at least one street food experience",
    "priority": "HIGH",
    "applyToFuture": true
  },
  "source": "CHAT",
  "createdAt": "2025-11-27T10:00:00Z",
  "sticky": true
}
```

**When to Update:**
- User states explicit requirement in chat
- User adds note to itinerary
- User creates custom rule

**Used By:** All agents (highest priority memory)

---

#### 10. Past Itinerary Metadata (Learning Data)
```json
{
  "userId": "user_123",
  "category": "HISTORY",
  "type": "ITINERARY_SUMMARY",
  "data": {
    "itineraryId": "itin_5",
    "destination": "Japan",
    "duration": 7,
    "totalCost": 2500,
    "satisfaction": "high",
    "completedActivities": 18,
    "skippedActivities": 2,
    "editsCount": 5,
    "validationDismissals": 3
  },
  "createdAt": "2025-11-20T10:00:00Z",
  "usedForLearning": true
}
```

**When to Update:**
- After itinerary completion
- User provides feedback
- System analyzes behavior

**Used By:** MemoryAgent (for pattern learning), all agents (for context)

---

### MemoryAgent API

#### Store Memory
```java
public void store(String userId, Memory memory) {
    // Validate memory
    validateMemory(memory);
    
    // Check for conflicts with existing memories
    resolveConflicts(userId, memory);
    
    // Store in Firestore
    memoryStore.save(userId, memory);
    
    // Trigger pattern learning if threshold reached
    if (shouldLearnPatterns(userId)) {
        learnPatternsAsync(userId);
    }
    
    logger.info("Stored {} memory for user {}", 
        memory.getCategory(), userId);
}
```

#### Retrieve Memory
```java
public List<Memory> retrieve(String userId, MemoryQuery query) {
    // Get from cache first
    List<Memory> cached = memoryCache.get(userId, query);
    if (cached != null) return cached;
    
    // Query Firestore
    List<Memory> memories = memoryStore.query(userId, query);
    
    // Filter expired
    memories = filterExpired(memories);
    
    // Sort by relevance
    memories = sortByRelevance(memories, query);
    
    // Cache result
    memoryCache.put(userId, query, memories);
    
    return memories;
}
```

#### Learn Patterns
```java
public void learnPatterns(String userId) {
    // Get all historical data
    List<Memory> history = retrieve(userId, 
        MemoryQuery.ofCategory("HISTORY"));
    
    // Extract patterns
    List<Pattern> patterns = patternExtractor.extract(history);
    
    // Store high-confidence patterns
    patterns.stream()
        .filter(p -> p.getConfidence() > 0.7)
        .forEach(p -> store(userId, Memory.fromPattern(p)));
    
    logger.info("Learned {} patterns for user {}", 
        patterns.size(), userId);
}
```

#### Get User Profile
```java
public UserProfile getProfile(String userId) {
    // Aggregate all relevant memories
    List<Memory> profile = retrieve(userId, 
        MemoryQuery.ofCategory("PROFILE"));
    List<Memory> dietary = retrieve(userId, 
        MemoryQuery.ofCategory("DIETARY"));
    List<Memory> preferences = retrieve(userId, 
        MemoryQuery.ofType("PREFERENCE"));
    List<Memory> patterns = retrieve(userId, 
        MemoryQuery.ofType("PATTERN"));
    
    // Build comprehensive profile
    return UserProfile.builder()
        .userId(userId)
        .travelStyle(extractTravelStyle(profile))
        .dietary(extractDietary(dietary))
        .activityPreferences(extractActivityPrefs(preferences))
        .budgetBehavior(extractBudgetBehavior(preferences))
        .timingPatterns(extractTimingPatterns(patterns))
        .transportPreferences(extractTransportPrefs(preferences))
        .mealPreferences(extractMealPrefs(preferences))
        .build();
}
```

---

## Part 2: ValidationAdvisor Agent (Refined)

### Single Responsibility
**Orchestrate validation tools, filter by user preferences, generate actionable advice.**

### Agent Structure

```java
@Component
public class ValidationAdvisorAgent extends BaseAgent {
    
    private final RestTemplate restTemplate;
    private final MemoryAgent memoryAgent;
    private final AiClient aiClient;
    
    @Override
    public AgentCapabilities getCapabilities() {
        AgentCapabilities capabilities = new AgentCapabilities();
        capabilities.addSupportedTask("validate_and_advise");
        capabilities.setChatEnabled(true);
        capabilities.setPriority(15);
        return capabilities;
    }
    
    /**
     * Main validation orchestration method
     */
    public ValidationAdvice validateAndAdvise(String itineraryId, 
                                              ValidationLevel level) {
        String userId = getItineraryUserId(itineraryId);
        
        // 1. Get user's validation preferences from memory
        List<Memory> validationPrefs = memoryAgent.retrieve(userId,
            MemoryQuery.builder()
                .category("VALIDATION")
                .build());
        
        // 2. Run validation tools in parallel
        Map<String, CompletableFuture<ValidationResult>> validations = 
            runValidationsAsync(itineraryId, level);
        
        // 3. Wait for all results
        CompletableFuture.allOf(validations.values().toArray(
            new CompletableFuture[0])).join();
        
        // 4. Collect all issues
        List<ValidationIssue> allIssues = collectIssues(validations);
        
        // 5. Filter based on user's past dismissals
        List<ValidationIssue> filteredIssues = filterByMemory(
            allIssues, validationPrefs);
        
        // 6. Generate smart recommendations using LLM
        ValidationAdvice advice = generateAdviceWithLLM(
            itineraryId, filteredIssues, validationPrefs);
        
        // 7. Store validation event in memory (for learning)
        storeValidationEvent(userId, itineraryId, advice);
        
        return advice;
    }
    
    /**
     * Run all validation tools in parallel
     */
    private Map<String, CompletableFuture<ValidationResult>> 
            runValidationsAsync(String itineraryId, ValidationLevel level) {
        
        Map<String, CompletableFuture<ValidationResult>> futures = 
            new HashMap<>();
        
        // Always run these
        futures.put("conflicts", CompletableFuture.supplyAsync(() -> 
            checkConflicts(itineraryId)));
        futures.put("completeness", CompletableFuture.supplyAsync(() -> 
            checkCompleteness(itineraryId)));
        
        // Conditional based on level
        if (level.includesBudget()) {
            futures.put("budget", CompletableFuture.supplyAsync(() -> 
                checkBudgetHealth(itineraryId)));
        }
        
        if (level.includesDietary()) {
            futures.put("dietary", CompletableFuture.supplyAsync(() -> 
                checkDietaryCompliance(itineraryId)));
        }
        
        if (level.includesTiming()) {
            futures.put("timing", CompletableFuture.supplyAsync(() -> 
                checkTimingFeasibility(itineraryId)));
        }
        
        return futures;
    }
    
    /**
     * Filter issues based on user's memory
     */
    private List<ValidationIssue> filterByMemory(
            List<ValidationIssue> issues, 
            List<Memory> validationPrefs) {
        
        return issues.stream()
            .filter(issue -> !isDismissed(issue, validationPrefs))
            .filter(issue -> !isAcceptedAsIs(issue, validationPrefs))
            .collect(Collectors.toList());
    }
    
    /**
     * Generate advice using LLM with context
     */
    private ValidationAdvice generateAdviceWithLLM(
            String itineraryId,
            List<ValidationIssue> issues,
            List<Memory> validationPrefs) {
        
        // Build context for LLM
        String context = buildValidationContext(issues, validationPrefs);
        
        // Generate advice
        String prompt = String.format(
            "User validation preferences: %s\n\n" +
            "Current issues: %s\n\n" +
            "Generate friendly, actionable advice...",
            summarizePreferences(validationPrefs),
            summarizeIssues(issues));
        
        String advice = aiClient.generate(prompt);
        
        return parseAdvice(advice, issues);
    }
    
    /**
     * Store validation event for learning
     */
    private void storeValidationEvent(String userId, String itineraryId, 
                                      ValidationAdvice advice) {
        Memory event = Memory.builder()
            .category("HISTORY")
            .type("VALIDATION_EVENT")
            .data(Map.of(
                "itineraryId", itineraryId,
                "issuesFound", advice.getTotalIssues(),
                "issuesShown", advice.getShownIssues(),
                "issuesFiltered", advice.getFilteredIssues(),
                "timestamp", System.currentTimeMillis()
            ))
            .build();
        
        memoryAgent.store(userId, event);
    }
}
```

---

### Validation Tools (Called by ValidationAdvisor)

#### 1. Check Conflicts Tool
```java
private ConflictCheckResult checkConflicts(String itineraryId) {
    ConflictCheckRequest request = new ConflictCheckRequest();
    request.setItineraryId(itineraryId);
    request.setCheckTimeConflicts(true);
    request.setCheckBudgetConflicts(true);
    request.setCheckLocationConflicts(true);
    
    return restTemplate.postForObject(
        "http://localhost:8080/api/v1/tools/check-conflicts",
        request,
        ConflictCheckResult.class);
}
```

#### 2. Check Completeness Tool
```java
private CompletenessResult checkCompleteness(String itineraryId) {
    CompletenessRequest request = new CompletenessRequest();
    request.setItineraryId(itineraryId);
    request.setCheckMeals(true);
    request.setCheckActivities(true);
    request.setCheckTransport(true);
    
    return restTemplate.postForObject(
        "http://localhost:8080/api/v1/tools/check-completeness",
        request,
        CompletenessResult.class);
}
```

#### 3. Check Budget Health Tool
```java
private BudgetHealthResult checkBudgetHealth(String itineraryId) {
    BudgetHealthRequest request = new BudgetHealthRequest();
    request.setItineraryId(itineraryId);
    request.setIncludeProjections(true);
    
    return restTemplate.postForObject(
        "http://localhost:8080/api/v1/tools/check-budget-health",
        request,
        BudgetHealthResult.class);
}
```

#### 4. Check Dietary Compliance Tool
```java
private DietaryComplianceResult checkDietaryCompliance(String itineraryId) {
    // Get user's dietary restrictions from memory
    String userId = getItineraryUserId(itineraryId);
    List<Memory> dietary = memoryAgent.retrieve(userId,
        MemoryQuery.builder()
            .category("DIETARY")
            .type("RESTRICTION")
            .build());
    
    DietaryComplianceRequest request = new DietaryComplianceRequest();
    request.setItineraryId(itineraryId);
    request.setRestrictions(extractRestrictions(dietary));
    
    return restTemplate.postForObject(
        "http://localhost:8080/api/v1/tools/check-dietary-compliance",
        request,
        DietaryComplianceResult.class);
}
```

#### 5. Check Timing Feasibility Tool
```java
private TimingFeasibilityResult checkTimingFeasibility(String itineraryId) {
    TimingFeasibilityRequest request = new TimingFeasibilityRequest();
    request.setItineraryId(itineraryId);
    request.setIncludeTravel(true);
    
    return restTemplate.postForObject(
        "http://localhost:8080/api/v1/tools/check-timing-feasibility",
        request,
        TimingFeasibilityResult.class);
}
```

---

## Integration Flow

### Flow 1: Pipeline Completion

```
PipelineOrchestrator
    ↓
Finalization complete
    ↓
ValidationAdvisorAgent.validateAndAdvise(itineraryId, COMPREHENSIVE)
    ↓
    ├─ Get user validation preferences from MemoryAgent
    ├─ Run 5 validation tools in parallel
    ├─ Filter issues based on user memory
    ├─ Generate LLM-powered advice
    ├─ Store validation event in MemoryAgent
    ↓
Return ValidationAdvice (non-blocking)
    ↓
Frontend displays advice
```

### Flow 2: User Dismisses Issue

```
User clicks "Dismiss" on validation issue
    ↓
Frontend sends dismissal request
    ↓
MemoryAgent.store(userId, dismissalMemory)
    ↓
    ├─ Store dismissal with reason
    ├─ Mark "apply to future" if selected
    ├─ Set expiration (1 year default)
    ↓
ValidationAdvisorAgent.validateAndAdvise() (re-run)
    ↓
    ├─ Retrieve updated memory
    ├─ Filter out dismissed issue
    ├─ Generate updated advice
    ↓
Frontend updates display (issue gone)
```

### Flow 3: Pattern Learning (Background)

```
After itinerary completion
    ↓
MemoryAgent.storeItinerarySummary(userId, itinerary)
    ↓
Check if learning threshold reached (e.g., 5 itineraries)
    ↓
MemoryAgent.learnPatterns(userId)
    ↓
    ├─ Analyze all historical data
    ├─ Extract timing patterns
    ├─ Extract activity preferences
    ├─ Extract budget behavior
    ├─ Extract transport preferences
    ↓
Store high-confidence patterns (>0.7)
    ↓
Next itinerary uses learned patterns
```

---

## When Memory Updates

### Automatic Updates (System-triggered)

1. **After Itinerary Completion**
   - Store itinerary summary
   - Trigger pattern learning
   - Update confidence scores

2. **After User Edit**
   - Update relevant preferences
   - Increment edit counters
   - Adjust confidence scores

3. **After Validation Dismissal**
   - Store dismissal immediately
   - Update validation preferences
   - Mark for future filtering

4. **Pattern Learning Threshold**
   - Every 5 itineraries
   - When confidence changes significantly
   - Weekly background job

### Manual Updates (User-triggered)

1. **User States Preference**
   - Store as explicit instruction
   - Mark as sticky (high priority)
   - Never auto-expire

2. **User Corrects System**
   - Update relevant memory
   - Increase confidence
   - Log correction event

3. **User Manages Memory**
   - User views/edits memories
   - User deletes memories
   - User exports memories (GDPR)

---

## Summary

### ValidationAdvisor Agent
**Single Responsibility:** Orchestrate validation, filter by memory, generate advice

**Does:**
- ✅ Call validation tools
- ✅ Filter by user memory
- ✅ Generate LLM advice
- ✅ Store validation events

**Doesn't:**
- ❌ Store user preferences (MemoryAgent does)
- ❌ Learn patterns (MemoryAgent does)
- ❌ Manage memory lifecycle (MemoryAgent does)

### MemoryAgent
**Single Responsibility:** Store, retrieve, learn from ALL user data

**Does:**
- ✅ Store all memory types
- ✅ Retrieve relevant memories
- ✅ Learn patterns from history
- ✅ Manage memory lifecycle
- ✅ Provide user profiles

**Doesn't:**
- ❌ Validate itineraries (ValidationAdvisor does)
- ❌ Generate advice (ValidationAdvisor does)
- ❌ Call validation tools (ValidationAdvisor does)

### Result
Clean separation of concerns, centralized memory, smart validation! 🎯

