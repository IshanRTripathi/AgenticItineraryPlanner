# Pipeline Optimization Implementation Plan

**Status:** 🚀 READY TO START  
**Date:** November 28, 2025  
**Target:** Reduce generation time from 150s → 55s (63% reduction)

---

## 🚨 CRITICAL BUG DISCOVERED: Population Lock Contention

**Date:** November 28, 2025  
**Status:** ❌ BLOCKING PERFORMANCE  
**Impact:** 26s wasted per itinerary (48% slower than expected)

### Problem Summary

The population phase claims to run in "TRUE PARALLEL" but actually runs **SEQUENTIALLY** due to lock contention:

```
Log says: "Running population agents in TRUE PARALLEL"
Reality:  ActivityAgent (28.7s) → MealAgent (20.3s) → TransportAgent (5.7s)
Total:    54.7s (sequential) instead of 28.7s (parallel)
Loss:     26s wasted
```

### Root Cause

All three agents (Activity, Meal, Transport) use the **same ReentrantLock** via `AgentCoordinator.executeWithLock(itineraryId, ...)`:

```java
// From AgentCoordinator.java line 27
private final Map<String, ReentrantLock> itineraryLocks = new ConcurrentHashMap<>();

public <T> T executeWithLock(String itineraryId, String agentName, Supplier<T> operation) {
    ReentrantLock lock = itineraryLocks.computeIfAbsent(itineraryId, k -> new ReentrantLock());
    lock.lock();  // ← ALL agents wait for this SAME lock
    try {
        return operation.get();
    } finally {
        lock.unlock();
    }
}
```

**Result:** Even though agents are launched in parallel threads, they serialize at the lock.

### Solution Options

#### Option A: Remove Locks Entirely (RECOMMENDED)
**Rationale:** Agents write to different node types - no conflict possible

```java
// In PipelineOrchestrator.java - Remove agentCoordinator.executeWithLock()
CompletableFuture<Void> activityFuture = CompletableFuture.runAsync(() -> {
    activityAgent.populateAttractions(itineraryId, skeleton);  // No lock!
}, pipelineExecutor);

CompletableFuture<Void> mealFuture = CompletableFuture.runAsync(() -> {
    mealAgent.populateMeals(itineraryId, skeleton);  // No lock!
}, pipelineExecutor);

CompletableFuture<Void> transportFuture = CompletableFuture.runAsync(() -> {
    transportAgent.populateTransport(itineraryId, skeleton);  // No lock!
}, pipelineExecutor);
```

**Why Safe:**
- ActivityAgent modifies `node.type = "activity"` nodes
- MealAgent modifies `node.type = "meal"` nodes  
- TransportAgent modifies `node.type = "transport"` nodes
- No overlap = no conflict

**Expected Impact:** 54.7s → 28.7s (26s savings, 48% faster)

#### Option B: Finer-Grained Locks
**Rationale:** Lock per node type instead of per itinerary

```java
// In AgentCoordinator.java
private final Map<String, Map<String, ReentrantLock>> typedLocks = new ConcurrentHashMap<>();

public <T> T executeWithTypedLock(String itineraryId, String nodeType, Supplier<T> operation) {
    Map<String, ReentrantLock> locks = typedLocks.computeIfAbsent(itineraryId, k -> new ConcurrentHashMap<>());
    ReentrantLock lock = locks.computeIfAbsent(nodeType, k -> new ReentrantLock());
    // ... rest same
}
```

**Why Safe:** Different node types can be modified in parallel

**Expected Impact:** Same as Option A (26s savings)

#### Option C: Collect-Then-Save Pattern (FUTURE)
**Rationale:** Agents collect data without modifying itinerary, then save once

```java
// Agents return data instead of modifying itinerary
PopulationResult activityResult = activityAgent.collectAttractions(skeleton);
PopulationResult mealResult = mealAgent.collectMeals(skeleton);
PopulationResult transportResult = transportAgent.collectTransport(skeleton);

// Apply all results in single-threaded context
applyPopulationResults(itinerary, activityResult, mealResult, transportResult);
itineraryJsonService.updateItineraryWithLock(itinerary);  // Single save
```

**Why Safe:** No concurrent modifications at all

**Expected Impact:** Same as Option A + cleaner architecture

### ✅ IMPLEMENTED - Collect-Then-Save Pattern (Completed)

**Status:** ✅ COMPLETE  
**Date Completed:** November 28, 2025  
**Implementation:** Proper collect-then-save pattern with skipSave parameter

**What Was Implemented:**

1. **Added `skipSave` parameter to all population agents** ✅
   - `ActivityAgent.populateAttractions(id, skeleton, skipSave)`
   - `MealAgent.populateMeals(id, skeleton, skipSave)`
   - `TransportAgent.populateTransport(id, skeleton, skipSave)`
   - Backward compatible (default skipSave=false)

2. **Created helper methods for in-memory data application** ✅
   - `ActivityAgent.applyAttractionsWithMetadata()` - applies attractions + metadata without saving
   - `MealAgent.applyMealsWithMetadata()` - applies meals + metadata without saving
   - `TransportAgent.applyTransportDataToSkeleton()` - applies transport data without saving

3. **Implemented collect-then-save in PipelineOrchestrator** ✅
   - All three agents run in parallel with `skipSave=true`
   - Agents modify skeleton in-memory (no database writes during collection)
   - After all agents complete, save once with `updateItineraryWithLock()`
   - Publish WebSocket events with `publishAgentComplete()` for each agent

4. **Proper architecture - no patches** ✅
   - Follows existing code patterns
   - Reuses existing helper methods
   - Proper separation of concerns
   - Budget tier calculation follows existing logic

**Files Modified:**
- `src/main/java/com/tripplanner/agents/ActivityAgent.java`
- `src/main/java/com/tripplanner/agents/MealAgent.java`
- `src/main/java/com/tripplanner/agents/TransportAgent.java`
- `src/main/java/com/tripplanner/service/PipelineOrchestrator.java`

**Expected Impact:**
- Population phase: 54.7s → 28.7s (26s savings, 48% faster)
- Total pipeline: 121.3s → 95s (22% improvement)

**Testing Required:**
- [ ] Run Switzerland 5-day trip test
- [ ] Verify population phase < 30s
- [ ] Verify all agents start simultaneously in logs
- [ ] Check for data integrity (no duplicate/missing nodes)
- [ ] Monitor WebSocket events reach frontend

---

## PHASE 1: CORE PARALLELIZATION (Week 1)

### Target: 150s → 82s (45% reduction) → **NOW: 150s → 56s (63% reduction with lock fix)**

### Task 1.1: Add City Grouping Method ✅ READY
**File:** `src/main/java/com/tripplanner/service/PipelineOrchestrator.java`

**Implementation:**
```java
/**
 * Group days by city/region for parallel processing.
 * Days in the same city must be sequential (to preserve context),
 * but different cities can be processed in parallel.
 */
private Map<String, List<Integer>> groupDaysByCity(CityAllocationPlan cityPlan, int totalDays) {
    Map<String, List<Integer>> cityGroups = new LinkedHashMap<>();
    
    for (int day = 1; day <= totalDays; day++) {
        CityAllocation cityForDay = getCityForDay(cityPlan, day);
        String cityName = cityForDay != null ? cityForDay.getCityName() : "Unknown";
        
        cityGroups.computeIfAbsent(cityName, k -> new ArrayList<>()).add(day);
    }
    
    logger.info("Grouped {} days into {} cities", totalDays, cityGroups.size());
    for (Map.Entry<String, List<Integer>> entry : cityGroups.entrySet()) {
        logger.info("  City '{}': Days {}", entry.getKey(), entry.getValue());
    }
    
    return cityGroups;
}

private CityAllocation getCityForDay(CityAllocationPlan cityPlan, int dayNumber) {
    return cityPlan.getAllocations().stream()
        .filter(a -> a.getStartDay() <= dayNumber && a.getEndDay() >= dayNumber)
        .findFirst()
        .orElse(null);
}
```

### Task 1.2: Add Feature Flag
**File:** `src/main/resources/application.properties`

```properties
# Enable city-grouped parallel skeleton generation
itinerary.generation.pipeline.parallel-cities:false

# Enable parallel day enrichment
itinerary.generation.pipeline.parallel-enrichment:true
```

### Task 1.3: Refactor Skeleton Phase (Collect-Then-Save)
**File:** `src/main/java/com/tripplanner/service/PipelineOrchestrator.java`

**Replace:** `executeSkeletonPhase()` method

**New Implementation:**
- Group days by city
- Process city groups in parallel
- Within each city: sequential (preserve context)
- Collect all days in memory
- Save once (no lock contention)

### Task 1.4: Add SkeletonPlannerAgent Method
**File:** `src/main/java/com/tripplanner/agents/SkeletonPlannerAgent.java`

**New Method:**
```java
/**
 * Generate day skeleton with explicit previous days context (NO SAVE).
 * Caller will collect all days and save once.
 */
public NormalizedDay generateDayWithContext(
        String itineraryId, 
        int dayNumber, 
        CreateItineraryReq request,
        List<NormalizedDay> previousDaysInCity) {
    // Generate skeleton with previous days context
    // Return day WITHOUT saving
}
```

### Task 1.5: Refactor Enrichment Phase (Parallel by Day)
**File:** `src/main/java/com/tripplanner/service/PipelineOrchestrator.java`

**Implementation:**
- Use existing `EnrichmentAgent.collectEnrichments(itinerary, day)`
- Process all days in parallel
- Collect enrichment results
- Apply enrichments in-memory
- Save once

### Task 1.6: Replace Cost Estimation with Tool
**File:** `src/main/java/com/tripplanner/service/PipelineOrchestrator.java`

**New Method:**
```java
private CostCalculationResult calculateCostWithTool(String itineraryId) {
    // Use calculate-cost tool
    // Fallback to LLM if tool fails
}
```

---

## IMPLEMENTATION ORDER

### ✅ Step 0: FIX POPULATION LOCK CONTENTION - COMPLETE

**Status:** ✅ IMPLEMENTED  
**Date Completed:** November 28, 2025  
**Time Taken:** ~3 hours (including proper refactoring)  
**Impact:** 26s savings (48% faster population phase)  
**Risk:** LOW

#### ✅ Task 0.1: Implemented skipSave Parameter (COMPLETE)

**Implementation:** Added `skipSave` parameter to all three population agents

**Changes Made:**
1. **ActivityAgent:**
   - Added `populateAttractions(id, skeleton, skipSave)` method
   - Created `applyAttractionsWithMetadata()` helper method
   - Refactored `updateItineraryWithAttractions()` to use helper

2. **MealAgent:**
   - Added `populateMeals(id, skeleton, skipSave)` method
   - Created `applyMealsWithMetadata()` helper method
   - Refactored `updateItineraryWithMeals()` to use helper

3. **TransportAgent:**
   - Added `populateTransport(id, skeleton, skipSave)` method
   - Created `applyTransportDataToSkeleton()` helper method
   - Refactored `updateItineraryWithTransport()` to use helper

**PipelineOrchestrator Changes:**
```java
// All agents run in parallel with skipSave=true
activityAgent.populateAttractions(itineraryId, skeleton, true);
mealAgent.populateMeals(itineraryId, skeleton, true);
transportAgent.populateTransport(itineraryId, skeleton, true);

// Wait for all to complete
CompletableFuture.allOf(activityFuture, mealFuture, transportFuture).get();

// Save once with all collected data
itineraryJsonService.updateItineraryWithLock(skeleton);

// Publish WebSocket events
agentEventPublisher.publishAgentComplete(itineraryId, execId, "ActivityAgent", activityCount);
agentEventPublisher.publishAgentComplete(itineraryId, execId, "MealAgent", mealCount);
agentEventPublisher.publishAgentComplete(itineraryId, execId, "TransportAgent", transportCount);
```

#### ⏭️ Task 0.2: Add Validation Test (TODO)

**Status:** Not yet implemented  
**Priority:** P2 - Add after initial testing confirms functionality

#### ⏭️ Task 0.3: Performance Test (TODO)

**Status:** Ready for testing  
**Next Step:** Run Switzerland 5-day trip to verify performance

**Expected Results:**
```
Population phase: ~28.7s (was 54.7s)
  - ActivityAgent: 28.7s  }
  - MealAgent: 20.3s      } All run in parallel
  - TransportAgent: 5.7s  }
  - Save: <1s
```

#### ⏭️ Task 0.4: Deploy & Monitor (TODO)

**Status:** Ready for deployment after testing  
**Build Status:** ✅ BUILD SUCCESSFUL

---

### Step 1: Add Helper Methods (30 min)
- [ ] Add `groupDaysByCity()` method
- [ ] Add `getCityForDay()` method
- [ ] Add `applyEnrichmentsToItinerary()` method
- [ ] Add `calculateCostWithTool()` method

### Step 2: Add Feature Flags (5 min)
- [ ] Add `parallel-cities` flag to application.properties
- [ ] Add field to PipelineOrchestrator

### Step 3: Refactor Skeleton Phase (2 hours)
- [ ] Create new `executeSkeletonPhaseParallel()` method
- [ ] Implement city grouping logic
- [ ] Implement parallel city processing
- [ ] Implement collect-then-save pattern
- [ ] Add feature flag check

### Step 4: Refactor Enrichment Phase (1 hour)
- [ ] Create new `executeEnrichmentPhaseParallel()` method
- [ ] Use existing `collectEnrichments()` method
- [ ] Implement parallel day processing
- [ ] Implement collect-then-save pattern

### Step 5: Add Cost Tool Integration (30 min)
- [ ] Implement `calculateCostWithTool()` method
- [ ] Add fallback to LLM
- [ ] Update cost estimation phase

### Step 6: Testing (2 hours)
- [ ] Test with 2-city itinerary
- [ ] Test with 3-city itinerary
- [ ] Test with single-city itinerary
- [ ] Verify context preservation
- [ ] Verify no race conditions
- [ ] Load test with 10 concurrent requests

---

## ROLLBACK STRATEGY

If issues occur:
1. Set `parallel-cities=false` in application.properties
2. Restart service
3. System reverts to sequential execution

---

## SUCCESS CRITERIA

- [ ] Generation time: 150s → 82s (45% reduction)
- [ ] Error rate: < 2%
- [ ] Memory usage: < 1GB
- [ ] No activity duplication
- [ ] Context preserved within cities

---

## NEXT STEPS

After Phase 1 complete:
- Monitor for 48 hours
- Analyze metrics
- Proceed to Phase 2 (Tool Integration)

