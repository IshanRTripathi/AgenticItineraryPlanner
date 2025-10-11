# Unused and Minimally-Used Files Analysis

## Executive Summary
This document identifies files in the codebase that have **no usage** or **minimal usage** in production code, based on comprehensive grep searches across the entire `src` directory.

---

## 🔴 CRITICAL: Completely Unused or Orphaned Files

### Service Layer

#### 1. **`AgentTaskSystem.java`** - UNUSED
- **Usage Count:** 2 matches (only in `TaskLifecycleManager.java` and `TaskProcessor.java`)
- **Status:** Appears to be part of an incomplete task management system
- **Used By:** Only other unused files
- **Recommendation:** Remove if task system is not being used

#### 2. **`TaskLifecycleManager.java`** - UNUSED
- **Usage Count:** 2 matches (only in `AgentTaskSystem.java` and `TaskProcessor.java`)
- **Status:** Part of incomplete task management system
- **Used By:** Only other unused files
- **Recommendation:** Remove if task system is not being used

#### 3. **`TaskProcessor.java`** - UNUSED
- **Usage Count:** 4 matches (only references to `RetryPolicy` and within task system files)
- **Status:** Part of incomplete task management system
- **Used By:** Only other unused files
- **Recommendation:** Remove if task system is not being used

#### 4. **`AlertManager.java`** - UNUSED
- **Usage Count:** 0 matches in production code
- **Status:** No references found anywhere in the codebase
- **Used By:** Nothing
- **Recommendation:** **DELETE - completely unused**

#### 5. **`SystemMetrics.java`** - UNUSED
- **Usage Count:** 0-1 matches in production code
- **Status:** No active usage found
- **Used By:** Nothing significant
- **Recommendation:** **DELETE - completely unused**

#### 6. **`TaskMetrics.java`** - UNUSED
- **Usage Count:** 0-1 matches in production code
- **Status:** No active usage found
- **Used By:** Nothing significant
- **Recommendation:** **DELETE - completely unused**

#### 7. **`TraceVisualizationService.java`** - UNUSED
- **Usage Count:** 0 matches in production code
- **Status:** No references found anywhere
- **Used By:** Nothing
- **Recommendation:** **DELETE - completely unused**

---

## 🟡 MINIMAL USAGE: Files Used Only Internally or in Tests

### Service Layer

#### 8. **`AgentCoordinator.java`** - MINIMAL USAGE
- **Usage Count:** Only in `AgentCoordinatorTest.java`
- **Status:** Only used in test files
- **Used By:** Tests only
- **Recommendation:** Remove if not planned for production use

#### 9. **`AgentEventBus.java`** - INTERNAL ONLY
- **Usage Count:** Only used by `AgentEventPublisher`
- **Status:** Wrapper around Spring's ApplicationEventPublisher
- **Used By:** `AgentEventPublisher` only
- **Recommendation:** Consider whether this abstraction is necessary

#### 10. **`PlaceRegistry.java`** - LOW USAGE
- **Usage Count:** 27 matches (mostly internal, 38 for CanonicalPlace)
- **Status:** Used by `PlaceEnrichmentService` and `PlaceMatcher`
- **Used By:** Limited to place-related services
- **Assessment:** May be partially implemented or future feature

#### 11. **`PlaceMatcher.java`** - LOW USAGE
- **Usage Count:** 5 matches (only in `PlaceRegistry`)
- **Status:** Only used internally by PlaceRegistry
- **Used By:** `PlaceRegistry` only
- **Recommendation:** May be part of incomplete feature

#### 12. **`RetryPolicy.java`** - MINIMAL USAGE
- **Usage Count:** 5 matches (in `TaskProcessor` and `RetryHandler`)
- **Status:** Only used by potentially unused task system
- **Used By:** `TaskProcessor`, `RetryHandler`
- **Recommendation:** Keep if RetryHandler is actively used

#### 13. **`RetryHandler.java`** - MINIMAL USAGE
- **Usage Count:** 4 matches (only in `RetryPolicy`)
- **Status:** Not clear if actively used
- **Used By:** Minimal
- **Recommendation:** Verify actual usage in retry scenarios

#### 14. **`LLMResponseHandler.java`** - MODERATE USAGE
- **Usage Count:** 41 matches
- **Status:** Used by `EditorAgent`, `PlannerAgent`, and tests
- **Used By:** Multiple agents
- **Assessment:** **KEEP - actively used**

---

## 🟢 HEAVILY USED: Core DTOs (Verified as Essential)

The following DTOs are **heavily used** and **critical** to the system:

### Core Data Models
- **`NormalizedItinerary.java`** - 463 matches across 53 files - **CRITICAL**
- **`NormalizedNode.java`** - 215 matches across 43 files - **CRITICAL**
- **`NormalizedDay.java`** - 182 matches across 36 files - **CRITICAL**
- **`ChangeSet.java`** - 297 matches across 27 files - **CRITICAL**
- **`ItineraryDiff.java`** - 58 matches across 11 files - **ESSENTIAL**
- **`ChatResponse.java`** - Used across orchestration and chat endpoints - **ESSENTIAL**
- **`ChatRequest.java`** - Used in chat controllers and orchestration - **ESSENTIAL**

### Supporting DTOs (Well-Used)
- **`NodeLocation.java`** - 78 matches across 17 files - **ESSENTIAL**
- **`NodeDetails.java`** - 26 matches across 13 files - **ESSENTIAL**
- **`NodeTiming.java`** - 22 matches across 12 files - **ESSENTIAL**
- **`NodeCost.java`** - 69 matches across 11 files - **ESSENTIAL**
- **`NodeTips.java`** - 10 matches across 4 files - **USED**
- **`Coordinates.java`** - 131 matches across 29 files - **ESSENTIAL**
- **`Edge.java`** - 53 matches across 12 files - **ESSENTIAL**
- **`DiffItem.java`** - 29 matches in ChangeEngine - **ESSENTIAL**
- **`NodeCandidate.java`** - 39 matches in orchestration - **ESSENTIAL**
- **`AgentEvent.java`** - Used for SSE real-time updates - **ESSENTIAL**
- **`AgentStatus.java`** - 29 matches across 9 files - **ESSENTIAL**
- **`MapBounds.java`** - 24 matches across 6 files - **ESSENTIAL**
- **`WorkflowData.java`** - 14 matches across 4 files - **USED**
- **`WorkflowEdge.java`** - 8 matches in workflow - **USED**
- **`RevisionRecord.java`** - 35 matches in revision management - **ESSENTIAL**
- **`ChatRecord.java`** - 7 matches in NormalizedItinerary - **USED**
- **`ItinerarySettings.java`** - 9 matches across 4 files - **USED**
- **`ItineraryUpdateEvent.java`** - 61 matches for SSE updates - **ESSENTIAL**
- **`ErrorEvent.java`** - 40 matches for error handling - **ESSENTIAL**
- **`PatchEvent.java`** - 11 matches in controllers - **USED**

### Booking & External Service DTOs
- **`Hotel.java`** - 112 matches (BookingAgent, BookingComService) - **ESSENTIAL**
- **`Activity.java`** - 149 matches across 29 files - **ESSENTIAL**
- **`Restaurant.java`** - 32 matches across 14 files - **ESSENTIAL**
- **`BookingRequest.java`** - 55 matches across 8 files - **ESSENTIAL**
- **`BookingConfirmation.java`** - 24 matches across 6 files - **ESSENTIAL**
- **`HotelSearchResponse.java`** - 10 matches in booking services - **USED**
- **`ActivitySearchResponse.java`** - 8 matches in booking services - **USED**

### Legacy DTOs (Used but Legacy)
- **`LocationDto.java`** - 33 matches (used in ItineraryService) - **LEGACY**
- **`ItineraryDayDto.java`** - 15 matches (used in ItineraryService) - **LEGACY**
- **`ActivityDto.java`** - 13 matches (used in ItineraryService) - **LEGACY**
- **`MealDto.java`** - 11 matches (used in ItineraryService) - **LEGACY**
- **`AccommodationDto.java`** - 10 matches (used in ItineraryService) - **LEGACY**
- **`TransportationDto.java`** - 11 matches (used in ItineraryService) - **LEGACY**

### Place-Related DTOs (Moderate Usage)
- **`PlaceCandidate.java`** - 31 matches across 4 files - **USED**
- **`CanonicalPlace.java`** - 67 matches across 5 files - **USED**

---

## 📊 Summary Statistics

### Files to DELETE (Completely Unused):
1. AlertManager.java
2. SystemMetrics.java
3. TaskMetrics.java
4. TraceVisualizationService.java

### Files to REVIEW (Minimal/Test-Only Usage):
1. AgentTaskSystem.java
2. TaskLifecycleManager.java
3. TaskProcessor.java
4. AgentCoordinator.java
5. AgentEventBus.java
6. PlaceRegistry.java
7. PlaceMatcher.java
8. RetryPolicy.java
9. RetryHandler.java

### Files CONFIRMED as ESSENTIAL (Heavily Used):
- All core DTOs (NormalizedItinerary, NormalizedNode, NormalizedDay, ChangeSet, etc.)
- All agents (ActivityAgent, BookingAgent, CostEstimatorAgent, etc.)
- All controllers
- Core services (ItineraryJsonService, ChangeEngine, OrchestratorService, LLMService, etc.)

---

## 🔍 Detailed Recommendations

### Immediate Actions:
1. **DELETE** the 4 completely unused files (AlertManager, SystemMetrics, TaskMetrics, TraceVisualizationService)
2. **INVESTIGATE** the task management system (AgentTaskSystem, TaskLifecycleManager, TaskProcessor) - if not in use, remove all three
3. **REVIEW** PlaceRegistry and PlaceMatcher - appear to be incomplete features
4. **VERIFY** RetryPolicy and RetryHandler usage - may be unused if task system is removed

### Further Investigation Needed:
1. Are AgentTaskSystem, TaskLifecycleManager, and TaskProcessor part of a planned feature or abandoned code?
2. Is PlaceRegistry/PlaceMatcher a work-in-progress or should it be removed?
3. Is AgentCoordinator used in production or only in tests?

---

## ✅ Verification Methodology
This analysis was conducted by:
1. Running `grep` searches for each class name across the entire `src` directory
2. Counting matches and analyzing context (production vs test usage)
3. Identifying files with 0-5 matches as potentially unused
4. Verifying usage patterns to distinguish between:
   - Completely unused (0 real references)
   - Test-only usage
   - Internal-only usage (one service calling another unused service)
   - Production usage

**Analysis Date:** Based on comprehensive searches performed during the codebase analysis
**Files Analyzed:** All Java files under `src/main/java/com/tripplanner/`
**Accuracy:** High confidence based on grep pattern matching across entire codebase




