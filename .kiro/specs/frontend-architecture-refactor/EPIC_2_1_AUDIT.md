# Epic 2.1: Data Format Migration - Audit Report

**Date:** January 20, 2025  
**Status:** Planning Phase  
**Complexity:** VERY HIGH  
**Estimated Duration:** 2 weeks

---

## 📊 Scope Analysis

### Files Using TripData: 55 files

**Breakdown by Category:**

### 1. Agent Components (3 files)
- `components/agents/AgentProgressModal.tsx`
- `components/agents/GeneratingPlan.tsx`
- `components/agents/SimplifiedAgentProgress.tsx`

### 2. Booking Components (2 files)
- `components/booking/Checkout.tsx`
- `components/booking/CostAndCart.tsx`

### 3. Dialogs (1 file)
- `components/dialogs/AddDestinationDialog.tsx`

### 4. Travel Planner Components (13 files)
- `components/travel-planner/layout/TopNavigation.tsx`
- `components/travel-planner/mobile/MobileLayout.tsx`
- `components/travel-planner/mobile/MobileMapDetailView.tsx`
- `components/travel-planner/mobile/MobilePlanDetailView.tsx`
- `components/travel-planner/mobile/MobilePlanView.tsx`
- `components/travel-planner/shared/ToolsPanels.tsx`
- `components/travel-planner/shared/types.ts`
- `components/travel-planner/views/BudgetView.tsx`
- `components/travel-planner/views/CollectionView.tsx`
- `components/travel-planner/views/DayByDayView.tsx`
- `components/travel-planner/views/DocumentsView.tsx`
- `components/travel-planner/views/PackingListView.tsx`
- `components/travel-planner/views/TransportPlanner.tsx`
- `components/travel-planner/views/TripOverviewView.tsx`
- `components/travel-planner/TravelPlannerHelpers.ts`
- `components/travel-planner/TravelPlannerHooks.ts`

### 5. Trip Management (4 files)
- `components/trip-management/EditMode.tsx`
- `components/trip-management/ItineraryOverview.tsx`
- `components/trip-management/ShareView.tsx`
- `components/trip-management/TripDashboard.tsx`

### 6. Trip Wizard (1 file)
- `components/trip-wizard/SimplifiedTripWizard.tsx`

### 7. Workflow Builder (5 files) - RECENTLY SPLIT
- `components/workflow-builder/WorkflowBuilderHelpers.ts`
- `components/workflow-builder/WorkflowBuilderHooks.ts`
- `components/workflow-builder/WorkflowBuilderState.ts`
- `components/workflow-builder/WorkflowBuilderTypes.ts`
- `components/WorkflowBuilder.tsx`

### 8. Main Components (5 files)
- `components/ItineraryWithChat.tsx`
- `components/LandingPage.tsx`
- `components/NormalizedItineraryViewer.tsx`
- `components/TravelPlanner.tsx`
- `components/TripViewLoader.tsx`

### 9. Contexts (2 files)
- `contexts/UnifiedItineraryReducer.ts`
- `contexts/UnifiedItineraryTypes.ts`

### 10. Services (6 files)
- `services/api.ts`
- `services/apiClient.ts`
- `services/dataTransformer.ts`
- `services/normalizedDataTransformer.ts`
- `services/__tests__/api.test.ts`
- `services/__tests__/normalizedDataTransformer.test.ts`

### 11. State Management (2 files)
- `state/slices/types.ts`
- `state/store/useAppStore.ts`

### 12. Types (1 file)
- `types/TripData.ts` - SOURCE DEFINITION

### 13. Utils (3 files)
- `utils/addPlaceToItinerary.ts`
- `utils/dataTransformers.ts`
- `utils/itineraryUtils.ts`

### 14. Data (1 file)
- `data/destinations.ts`

### 15. Tests (2 files)
- `components/travel-planner/views/__tests__/TripOverviewView.test.tsx`
- `__tests__/e2e.test.ts`

### 16. Root (1 file)
- `App.tsx`

---

## 🎯 Migration Strategy

### Phase 1: Analysis & Planning (2 days)
1. ✅ Audit complete - 55 files identified
2. ⏳ Identify transformation layers
3. ⏳ Map TripData → NormalizedItinerary field mappings
4. ⏳ Identify breaking changes
5. ⏳ Create compatibility layer design

### Phase 2: Create Compatibility Layer (1 day)
1. Create `utils/legacyAdapter.ts`
2. Implement `useLegacyTripData` hook
3. Add feature flag `USE_NORMALIZED_FORMAT`
4. Test adapter with sample data

### Phase 3: Migrate Leaf Components (3 days)
**Priority Order:**
1. Agent components (3 files) - Low complexity
2. Booking components (2 files) - Medium complexity
3. Dialogs (1 file) - Low complexity
4. Trip management views (4 files) - Medium complexity

### Phase 4: Migrate Container Components (3 days)
**Priority Order:**
1. Travel planner views (13 files) - High complexity
2. Workflow builder (5 files) - Medium complexity (recently refactored)
3. Mobile components (4 files) - Medium complexity

### Phase 5: Migrate Core Components (2 days)
**Priority Order:**
1. Main components (5 files) - High complexity
2. Contexts (2 files) - Critical
3. Root App.tsx (1 file) - Critical

### Phase 6: Update Services & Utils (1 day)
1. Services (6 files) - Remove transformations
2. Utils (3 files) - Update helpers
3. State management (2 files) - Update types

### Phase 7: Cleanup (1 day)
1. Remove transformation layers
2. Delete TripData.ts
3. Remove compatibility layer
4. Remove feature flag
5. Update tests

---

## ⚠️ Risk Assessment

### HIGH RISK Areas:
1. **UnifiedItineraryContext** - Core state management
2. **TravelPlanner.tsx** - Main component
3. **apiClient.ts** - Data fetching layer
4. **Services layer** - Transformation logic

### MEDIUM RISK Areas:
1. Travel planner views (13 files)
2. Workflow builder (5 files)
3. Mobile components (4 files)

### LOW RISK Areas:
1. Agent components (3 files)
2. Booking components (2 files)
3. Dialogs (1 file)

---

## 📋 Prerequisites

### Before Starting Migration:
1. ✅ Epic 2.3 complete (File size reduction)
2. ⏳ Comprehensive test coverage for critical paths
3. ⏳ Feature flag system in place
4. ⏳ Rollback plan documented
5. ⏳ Staging environment ready

### Required Knowledge:
1. Current TripData structure (55 fields)
2. NormalizedItinerary structure
3. Transformation layer logic
4. Component data flow patterns

---

## 🔍 Next Steps

### Immediate Actions:
1. **Analyze transformation layers**
   - Read `services/dataTransformer.ts`
   - Read `services/normalizedDataTransformer.ts`
   - Document transformation logic

2. **Map data structures**
   - Create TripData → NormalizedItinerary mapping
   - Identify missing fields
   - Identify deprecated fields

3. **Design compatibility layer**
   - Define adapter interface
   - Plan feature flag implementation
   - Design migration hooks

4. **Create migration plan**
   - Detailed task breakdown
   - Component migration order
   - Testing strategy

---

## 📝 Notes

- This is a CRITICAL migration affecting 55 files
- Requires careful planning and incremental approach
- Must maintain backward compatibility during migration
- Comprehensive testing required at each step
- Feature flag allows gradual rollout
- Rollback plan essential

---

**Status:** Ready for detailed planning phase
**Next Session:** Analyze transformation layers and create field mapping

