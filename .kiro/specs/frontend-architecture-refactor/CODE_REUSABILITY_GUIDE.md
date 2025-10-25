# Code Reusability & Zero Duplication Guide

## 🎯 Core Principle: ZERO Code Duplication

**Every piece of logic, component, or utility should exist in exactly ONE place.**

---

## 📋 Reusability Patterns Established

### 1. State Management Hooks

**Pattern:** Extract state logic into custom hooks that can be reused across components.

**Examples:**
- `useTravelPlannerState()` - Manages all TravelPlanner state
- `useUnifiedItinerary()` - Provides itinerary context
- `useUnifiedItinerarySelector()` - Optimized state selection

**When to create a new hook:**
- State logic is used in 2+ components
- State logic is complex (>20 lines)
- State logic has side effects that need isolation

**Location:** `frontend/src/hooks/` or component-specific `*Hooks.ts` files

---

### 2. Effect Hooks (Side Effects)

**Pattern:** Extract useEffect logic into dedicated hooks with clear names.

**Examples:**
- `useDestinationsSync()` - Syncs destinations from trip data
- `useFreshItineraryCheck()` - Ensures fresh data
- `useMapViewModeSync()` - Syncs map view with UI state
- `useMapCenterSync()` - Centers map based on view
- `useAgentStatusesSync()` - Syncs agent progress

**When to create a new effect hook:**
- Effect logic is used in 2+ components
- Effect has complex dependencies
- Effect needs to be tested independently

**Location:** Component-specific `*Hooks.ts` files

---

### 3. Helper Functions & Handlers

**Pattern:** Extract event handlers and utility functions into factory functions.

**Examples:**
- `createDestinationHandlers()` - Returns update/add/remove handlers
- `createDaySelectHandler()` - Creates day selection handler
- `createItineraryUpdateHandler()` - Creates update handler
- `buildMapMarkers()` - Builds markers from trip data
- `createAddPlaceHandler()` - Creates place addition handler

**When to create a helper:**
- Logic is used in 2+ places
- Logic is complex (>10 lines)
- Logic can be tested independently

**Location:** Component-specific `*Helpers.ts` files or `frontend/src/utils/`

---

### 4. Type Definitions

**Pattern:** Define types once, export from a central location.

**Examples:**
- `UnifiedItineraryTypes.ts` - All context types
- `shared/types.ts` - Shared component types
- `MapTypes.ts` - Map-related types

**Rules:**
- ✅ Define types in dedicated `*Types.ts` files
- ✅ Export from a single source
- ✅ Import types, never duplicate
- ❌ Never copy-paste type definitions

**Location:** 
- Context types: `*Types.ts` next to context
- Shared types: `frontend/src/types/`
- Component types: `component-folder/shared/types.ts`

---

### 5. UI Components

**Pattern:** Create reusable components with clear props interfaces.

**Examples:**
- `LoadingState` - 4 variants (fullPage, inline, progress, minimal)
- `SkeletonLoader` - 5 types (dayCard, nodeCard, list, table, generic)
- `ErrorDisplay` - Consistent error UI
- `ErrorBoundary` - Error catching wrapper

**When to create a reusable component:**
- UI pattern is used in 2+ places
- Component has clear, single responsibility
- Component can be configured via props

**Location:** `frontend/src/components/shared/` or `frontend/src/components/ui/`

---

## 🔍 How to Check for Duplication

### Before Writing New Code:

1. **Search for similar logic:**
   ```bash
   # Search for similar function names
   grep -r "functionName" frontend/src
   
   # Search for similar patterns
   grep -r "useState.*destinations" frontend/src
   ```

2. **Check existing hooks:**
   - Look in `frontend/src/hooks/`
   - Look in component `*Hooks.ts` files
   - Check context providers

3. **Check existing helpers:**
   - Look in `frontend/src/utils/`
   - Look in component `*Helpers.ts` files

4. **Check existing types:**
   - Look in `frontend/src/types/`
   - Look in `*Types.ts` files

### During Code Review:

- ❌ **Red Flag:** Copy-pasted code blocks
- ❌ **Red Flag:** Similar function names in different files
- ❌ **Red Flag:** Duplicate type definitions
- ✅ **Green Flag:** Imports from shared modules
- ✅ **Green Flag:** Reused hooks and helpers
- ✅ **Green Flag:** Single source of truth for types

---

## 📐 Refactoring Guidelines

### When You Find Duplication:

1. **Identify the duplicated logic**
2. **Extract to appropriate location:**
   - State logic → Custom hook
   - Event handlers → Helper function
   - UI patterns → Reusable component
   - Types → Type definition file
3. **Update all usages to import from single source**
4. **Delete duplicated code**
5. **Verify with TypeScript (zero errors)**

### Example Refactoring:

**Before (Duplication):**
```typescript
// ComponentA.tsx
const [destinations, setDestinations] = useState<Destination[]>([]);
const updateDestination = (id: string, updates: Partial<Destination>) => {
  setDestinations(prev => prev.map(dest =>
    dest.id === id ? { ...dest, ...updates } : dest
  ));
};

// ComponentB.tsx
const [destinations, setDestinations] = useState<Destination[]>([]);
const updateDestination = (id: string, updates: Partial<Destination>) => {
  setDestinations(prev => prev.map(dest =>
    dest.id === id ? { ...dest, ...updates } : dest
  ));
};
```

**After (Reusable):**
```typescript
// shared/hooks/useDestinations.ts
export function useDestinations() {
  const [destinations, setDestinations] = useState<Destination[]>([]);
  
  const updateDestination = useCallback((id: string, updates: Partial<Destination>) => {
    setDestinations(prev => prev.map(dest =>
      dest.id === id ? { ...dest, ...updates } : dest
    ));
  }, []);
  
  return { destinations, setDestinations, updateDestination };
}

// ComponentA.tsx
const { destinations, updateDestination } = useDestinations();

// ComponentB.tsx
const { destinations, updateDestination } = useDestinations();
```

---

## 🎯 Reusability Checklist

### Before Committing Code:

- [ ] No copy-pasted code blocks
- [ ] All types imported from single source
- [ ] Complex logic extracted to hooks/helpers
- [ ] UI patterns use shared components
- [ ] Event handlers use factory functions
- [ ] Zero TypeScript errors
- [ ] All imports resolve correctly

### During Epic 2.3 (File Size Reduction):

- [ ] Extract state to custom hooks
- [ ] Extract effects to dedicated hooks
- [ ] Extract handlers to helper functions
- [ ] Extract types to `*Types.ts` files
- [ ] Verify no duplication across split files
- [ ] Update all imports to use extracted modules

---

## 📊 Current Reusable Modules

### Hooks (22 total)

**UnifiedItinerary Hooks:**
- `useUnifiedItinerary()` - Context hook
- `useUnifiedItinerarySelector()` - Optimized selector

**TravelPlanner Hooks:**
- `useTravelPlannerState()` - State management
- `useDestinationsSync()` - Destinations sync
- `useFreshItineraryCheck()` - Data freshness
- `useMapViewModeSync()` - Map view sync
- `useMapCenterSync()` - Map centering
- `useAgentStatusesSync()` - Agent progress sync

**WorkflowBuilder Hooks:**
- `useWorkflowBuilderState()` - State management
- `useUserChangesSubscription()` - User changes tracking
- `useTripDataSync()` - Trip data synchronization
- `useSavedPositionsSync()` - Position persistence
- `useActiveDaySync()` - Active day updates
- `useViewportZoomSync()` - Viewport zoom management
- `useWorkflowDaysSync()` - Workflow days updates
- `useMapViewModeSync()` - Map view synchronization
- `useNodePositionTracking()` - Node position tracking
- `useClearBadPositions()` - Position cleanup

**NormalizedItinerary Hooks (NEW - Phase 2):**
- `useNormalizedItinerary()` - Main hook with accessors and computed values
- `useNormalizedDay()` - Day-specific hook with filtered nodes
- `useItineraryStatistics()` - Statistics hook
- `useNodesByType()` - Node filtering hook

### Helpers (42 total)

**TravelPlanner Helpers:**
- `createDestinationHandlers()` - Destination CRUD
- `createDaySelectHandler()` - Day selection
- `createItineraryUpdateHandler()` - Itinerary updates
- `buildMapMarkers()` - Map marker building
- `createAddPlaceHandler()` - Place addition

**WorkflowBuilder Helpers:**
- `calculateOptimalZoom()` - Zoom calculation
- `calculateVerticalSeparation()` - Layout spacing
- `getGridPosition()` - Grid positioning
- `createWorkflowDaysFromTripData()` - Data transformation
- `createSeedData()` - Fallback data
- `createNodeHandlers()` - Node event handlers
- `createWorkflowActionHandlers()` - Workflow actions

**ItineraryAdapter Helpers (NEW - Phase 2):**
- `getId()` - Get itinerary ID
- `getDateRange()` - Get date range
- `getDuration()` - Get duration in days
- `getTotalCost()` - Calculate total cost
- `getDayCost()` - Get day cost
- `getTotalDistance()` - Calculate total distance
- `getDayDistance()` - Get day distance
- `getDayById()` - Get day by ID
- `getDayByNumber()` - Get day by number
- `getSortedDays()` - Get sorted days
- `getNodeById()` - Get node by ID
- `getNodesByType()` - Get nodes by type
- `getMealNodes()` - Get meal nodes
- `getAccommodationNode()` - Get accommodation node
- `getAttractionNodes()` - Get attraction nodes
- `getLockedNodes()` - Get locked nodes
- `getComponentType()` - Convert node type
- `getPriceRange()` - Get price range
- `millisecondsToTimeString()` - Convert time
- `timeStringToMilliseconds()` - Convert time
- `hasDays()` - Check if has days
- `hasNodes()` - Check if has nodes
- `hasValidCoordinates()` - Check coordinates
- `findNodeById()` - Find node across days
- `getAllNodes()` - Get all nodes
- `getAllNodesByType()` - Get all nodes by type
- `getUniqueLocations()` - Get unique locations
- `getStatistics()` - Get statistics

**TypeGuard Helpers (NEW - Phase 2):**
- `isNormalizedItinerary()` - Check format
- `isTripData()` - Check legacy format
- `getItineraryFormat()` - Get format type
- `isValidItineraryFormat()` - Validate format

### Components (5 total)
- `LoadingState` - Loading indicators (4 variants)
- `SkeletonLoader` - Skeleton screens (5 types)
- `ErrorDisplay` - Error messages
- `ErrorBoundary` - Error catching
- `AutoRefreshEmptyState` - Empty states

### Types (9 total)
- `UnifiedItineraryTypes.ts` - Context types
- `TravelPlannerView` - View types
- `PlanTab` - Tab types
- `Destination` - Destination type
- `AgentStatus` - Agent status type
- `MapMarker` - Map marker type
- `WorkflowBuilderTypes.ts` - Workflow types
- `WorkflowNodeData` - Node data type
- `WorkflowDay` - Day structure type

---

## 🚀 Next Steps for Zero Duplication

### Immediate Actions:

1. **Audit WorkflowBuilder.tsx** for duplication before splitting
2. **Check for similar patterns** across components
3. **Extract common logic** to shared modules
4. **Document reusable patterns** as they emerge

### Long-term Goals:

1. **Create component library** for common UI patterns
2. **Build hook library** for common state patterns
3. **Establish utility library** for common operations
4. **Maintain type library** for all shared types

---

## 📝 Documentation Standards

### When Creating Reusable Code:

1. **Add JSDoc comments:**
   ```typescript
   /**
    * Custom hook for managing destinations
    * @returns Destinations state and handlers
    * @example
    * const { destinations, updateDestination } = useDestinations();
    */
   ```

2. **Document parameters:**
   ```typescript
   /**
    * Creates destination handlers
    * @param setDestinations - State setter function
    * @returns Object with update, add, remove handlers
    */
   ```

3. **Add usage examples:**
   ```typescript
   /**
    * @example
    * const handlers = createDestinationHandlers(setDestinations);
    * handlers.updateDestination('id-1', { name: 'New Name' });
    */
   ```

---

## ✅ Success Metrics

- **Zero** copy-pasted code blocks
- **Single** source of truth for all types
- **Reusable** hooks for common patterns
- **Shared** components for UI patterns
- **Documented** reusable modules
- **Tested** independently testable units

---

*Last Updated: January 20, 2025*
*Applies to: All frontend refactoring work*
