# Implementation Plan: Frontend Architecture Refactor

## Overview

This implementation plan breaks down the frontend refactoring into discrete, manageable coding tasks. Each task is designed to be completed independently while building incrementally toward the final architecture.

**Total Duration:** 11 weeks  
**Team Size:** 2-3 frontend developers  
**Priority:** CRITICAL

---

## PHASE 1: Critical Stabilization (Weeks 1-2)

### Epic 1.1: Centralized Logging System

- [ ] 1.1.1 Create logger service infrastructure
  - Create `frontend/src/services/logging/logger.ts`
  - Implement LogLevel enum (DEBUG, INFO, WARN, ERROR, NONE)
  - Implement Logger class with level-based filtering
  - Add timer functionality for performance tracking
  - _Requirements: 1.1_

- [ ] 1.1.2 Implement log transports
  - Create ConsoleTransport for development
  - Create SentryTransport for production errors
  - Create AnalyticsTransport for user actions
  - Add transport configuration system
  - _Requirements: 1.1_

- [ ] 1.1.3 Replace console.log in services layer
  - Update `apiClient.ts` (remove 10+ console.log)
  - Update `normalizedDataTransformer.ts` (remove 20+ console.log)
  - Update `websocket.ts` (remove 15+ console.log)
  - Update `sseManager.ts` (remove 5+ console.log)
  - _Requirements: 1.1_

- [ ] 1.1.4 Replace console.log in components
  - Update `TravelPlanner.tsx` (remove 15+ console.log)
  - Update `UnifiedItineraryContext.tsx` (remove 10+ console.log)
  - Update `DayByDayView.tsx` (remove 8+ console.log)
  - Update remaining components (scan and replace all)
  - _Requirements: 1.1_

- [ ] 1.1.5 Configure logging for environments
  - Set DEBUG level for development
  - Set ERROR level for production
  - Add environment variable controls
  - Test logging in both environments
  - _Requirements: 1.1_

### Epic 1.2: Centralized Error Handling

- [ ] 1.2.1 Create error handler service
  - Create `frontend/src/services/errorHandler.ts`
  - Implement error classification (NETWORK, AUTH, VALIDATION, etc.)
  - Implement getUserMessage() for user-friendly messages
  - Implement getRecoveryActions() for error recovery
  - _Requirements: 1.2_

- [ ] 1.2.2 Create error boundary component
  - Create `frontend/src/components/shared/ErrorBoundary.tsx`
  - Implement error catching and logging
  - Add reset functionality
  - Add error reporting integration
  - _Requirements: 1.2_

- [ ] 1.2.3 Create error display component
  - Create `frontend/src/components/shared/ErrorDisplay.tsx`
  - Design consistent error UI
  - Add recovery action buttons
  - Add error details (collapsible for debugging)
  - _Requirements: 1.2_

- [ ] 1.2.4 Integrate with React Query
  - Configure global error handlers in QueryClient
  - Add mutation error handling
  - Add query error handling
  - Test error scenarios
  - _Requirements: 1.2_

- [ ] 1.2.5 Wrap application with error boundary
  - Update `App.tsx` to wrap with ErrorBoundary
  - Test error boundary with intentional errors
  - Verify error logging and reporting
  - _Requirements: 1.2_

### Epic 1.3: Standardized Loading States

- [ ] 1.3.1 Create loading state component
  - Create `frontend/src/components/shared/LoadingState.tsx`
  - Implement fullPage variant
  - Implement inline variant
  - Implement progress variant
  - _Requirements: 1.3_

- [ ] 1.3.2 Create skeleton loader component
  - Create `frontend/src/components/shared/SkeletonLoader.tsx`
  - Implement dayCard skeleton
  - Implement nodeCard skeleton
  - Implement list skeleton
  - _Requirements: 1.3_

- [ ] 1.3.3 Replace loading indicators in TravelPlanner
  - Replace spinner with LoadingState
  - Replace empty state loading
  - Add skeleton loaders for lists
  - _Requirements: 1.3_

- [ ] 1.3.4 Replace loading indicators in other components
  - Update DayByDayView
  - Update WorkflowBuilder
  - Update all remaining components
  - _Requirements: 1.3_

- [ ] 1.3.5 Add loading state tests
  - Test all loading variants
  - Test transitions between states
  - Test cancellation functionality
  - _Requirements: 1.3_

---

## PHASE 2: Architecture Consolidation (Weeks 3-6)

### Epic 2.1: Data Format Migration

- [x] 2.1.1 Audit current TripData usage ✅ COMPLETE
  - Scanned codebase: 55 files identified using TripData
  - Documented all components by category (16 categories)
  - Identified transformation points (dataTransformer, normalizedDataTransformer)
  - Created comprehensive audit document (EPIC_2_1_AUDIT.md)
  - Created 7-phase migration strategy (13 days estimated)
  - Assessed risk levels (High/Medium/Low)
  - _Requirements: 2.1_

- [ ] 2.1.2 Create compatibility layer
  - Create `frontend/src/utils/legacyAdapter.ts`
  - Implement useLegacyTripData hook
  - Add feature flag USE_NORMALIZED_FORMAT
  - Test adapter with existing components
  - _Requirements: 2.1_

- [ ] 2.1.3 Migrate leaf components to NormalizedItinerary
  - Update DayCard to use NormalizedDay
  - Update NodeCard to use NormalizedNode
  - Update MapMarker to use NormalizedNode
  - Test components with new format
  - _Requirements: 2.1_

- [ ] 2.1.4 Migrate container components
  - Update DayByDayView
  - Update DestinationsManager
  - Update WorkflowBuilder
  - Test data flow
  - _Requirements: 2.1_

- [ ] 2.1.5 Migrate TravelPlanner (main component)
  - Update TravelPlanner.tsx
  - Remove TripData type usage
  - Update all prop types
  - Test entire application
  - _Requirements: 2.1_

- [ ] 2.1.6 Remove transformation layer
  - Delete `normalizedDataTransformer.ts`
  - Delete `dataTransformer.ts`
  - Remove compatibility layer
  - Delete TripData.ts
  - _Requirements: 2.1_

- [ ] 2.1.7 Update API client
  - Remove transformation in apiClient.ts
  - Return NormalizedItinerary directly
  - Update all API response types
  - Test API integration
  - _Requirements: 2.1_

### Epic 2.2: Context Consolidation

- [ ] 2.2.1 Create new AppContext
  - Create `frontend/src/contexts/AppContext.tsx` (<300 lines)
  - Implement authentication state
  - Implement user preferences
  - Implement theme management
  - _Requirements: 2.2_

- [ ] 2.2.2 Create new ItineraryContext
  - Create `frontend/src/contexts/ItineraryContext.tsx` (<300 lines)
  - Implement thin wrapper around React Query
  - Add currentItineraryId management
  - Add loading/error state
  - _Requirements: 2.2_

- [ ] 2.2.3 Create new UIContext
  - Create `frontend/src/contexts/UIContext.tsx` (<200 lines)
  - Implement sidebar state
  - Implement modal state
  - Implement selection state
  - Implement view mode state
  - _Requirements: 2.2_

- [ ] 2.2.4 Create custom hooks for new contexts
  - Create `frontend/src/hooks/useApp.ts`
  - Create `frontend/src/hooks/useItinerary.ts`
  - Create `frontend/src/hooks/useUI.ts`
  - Add TypeScript types
  - _Requirements: 2.2_

- [ ] 2.2.5 Add feature flag for new contexts
  - Add USE_NEW_CONTEXTS feature flag
  - Create context switcher utility
  - Test both old and new contexts
  - _Requirements: 2.2_

- [ ] 2.2.6 Migrate components to new contexts (batch 1)
  - Update TravelPlanner
  - Update TopNavigation
  - Update NavigationSidebar
  - Test functionality
  - _Requirements: 2.2_

- [ ] 2.2.7 Migrate components to new contexts (batch 2)
  - Update DayByDayView
  - Update WorkflowBuilder
  - Update NewChat
  - Test functionality
  - _Requirements: 2.2_

- [ ] 2.2.8 Migrate components to new contexts (batch 3)
  - Update all remaining components
  - Remove old context usage
  - Test entire application
  - _Requirements: 2.2_

- [ ] 2.2.9 Remove old contexts
  - Delete UnifiedItineraryContext.tsx
  - Delete PreviewSettingsContext.tsx
  - Merge MapContext into UIContext
  - Remove feature flag
  - _Requirements: 2.2_

### Epic 2.3: File Size Reduction

**CRITICAL: Zero Code Duplication Policy**
- Before creating new code, search for existing implementations
- Reuse hooks, helpers, and components wherever possible
- Extract common patterns to shared modules
- See: `CODE_REUSABILITY_GUIDE.md`

- [x] 2.3.1 Split UnifiedItineraryContext.tsx ✅ COMPLETE
  - Extracted types to UnifiedItineraryTypes.ts (198 lines)
  - Extracted reducer to UnifiedItineraryReducer.ts (275 lines)
  - Extracted actions to UnifiedItineraryActions.ts (658 lines)
  - Extracted hooks to UnifiedItineraryHooks.ts (68 lines)
  - Main context: 388 lines (72% reduction)
  - Zero TypeScript errors
  - Removed fallback modes for single flow
  - _Requirements: 2.3_

- [x] 2.3.2 Split TravelPlanner.tsx ✅ COMPLETE
  - Extracted state to TravelPlannerState.ts (50 lines)
  - Extracted effects to TravelPlannerHooks.ts (247 lines)
  - Extracted handlers to TravelPlannerHelpers.ts (136 lines)
  - Main component: 540 lines (30% reduction)
  - Zero TypeScript errors
  - All functionality preserved
  - _Requirements: 2.3_

- [x] 2.3.3 Split WorkflowBuilder.tsx ✅ COMPLETE
  - Extracted types to WorkflowBuilderTypes.ts (48 lines)
  - Extracted state to WorkflowBuilderState.ts (72 lines)
  - Extracted hooks to WorkflowBuilderHooks.ts (221 lines, 10 custom hooks)
  - Extracted helpers to WorkflowBuilderHelpers.ts (644 lines)
  - Main component: 376 lines (66% reduction)
  - Zero TypeScript errors
  - Zero code duplication (reused existing WorkflowUtils)
  - All functionality preserved
  - _Requirements: 2.3_

- [x] 2.3.4 Verify all files under size limits & zero duplication ✅ COMPLETE
  - All 3 target files split successfully
  - UnifiedItineraryContext: 388 lines (target met)
  - TravelPlanner: 540 lines (target met)
  - WorkflowBuilder: 376 lines (target met)
  - Zero code duplication verified across all splits
  - Reused existing utilities (WorkflowUtils, etc.)
  - 12 new reusable modules created and documented
  - _Requirements: 2.3_

### Epic 2.4: State Synchronization Fix

- [ ] 2.4.1 Configure React Query as single source of truth
  - Update QueryClient configuration
  - Set appropriate cache times
  - Configure stale time
  - Add optimistic updates
  - _Requirements: 2.4_

- [ ] 2.4.2 Remove duplicate state in components
  - Audit components for local state
  - Replace with React Query hooks
  - Remove useState for server data
  - Test data consistency
  - _Requirements: 2.4_

- [ ] 2.4.3 Implement optimistic updates
  - Add optimistic update for node changes
  - Add optimistic update for day changes
  - Add rollback on error
  - Test update scenarios
  - _Requirements: 2.4_

- [ ] 2.4.4 Remove LocalStorage for server data
  - Audit LocalStorage usage
  - Remove server data caching
  - Keep only UI preferences
  - Implement cleanup utility
  - _Requirements: 2.4_

- [ ] 2.4.5 Test state synchronization
  - Test multi-tab synchronization
  - Test real-time updates
  - Test offline/online transitions
  - Test conflict resolution
  - _Requirements: 2.4_

---

## PHASE 3: Real-time System Consolidation (Weeks 7-8)

### Epic 3.1: WebSocket-Only Implementation

- [ ] 3.1.1 Create new RealtimeService
  - Create `frontend/src/services/realtime/realtimeService.ts`
  - Implement WebSocket connection management
  - Add reconnection logic with exponential backoff
  - Add message queue for offline support
  - _Requirements: 3.1_

- [ ] 3.1.2 Implement token refresh for WebSocket
  - Add token refresh before expiry
  - Handle token refresh failures
  - Reconnect with new token
  - Test token expiration scenarios
  - _Requirements: 3.2_

- [ ] 3.1.3 Implement message handling
  - Add subscribe/unsubscribe methods
  - Add message type routing
  - Add error handling
  - Add message validation
  - _Requirements: 3.1_

- [ ] 3.1.4 Integrate with React Query
  - Update cache on itinerary_updated messages
  - Update cache on agent_progress messages
  - Update cache on chat_message messages
  - Test cache updates
  - _Requirements: 3.1_

- [ ] 3.1.5 Add feature flag for WebSocket-only
  - Add USE_WEBSOCKET_ONLY feature flag
  - Create service switcher
  - Test both SSE and WebSocket modes
  - _Requirements: 3.1_

- [ ] 3.1.6 Migrate agent progress to WebSocket
  - Update SimplifiedAgentProgress component
  - Remove SSE subscription
  - Add WebSocket subscription
  - Test agent progress updates
  - _Requirements: 3.1_

- [ ] 3.1.7 Migrate chat to WebSocket
  - Update NewChat component
  - Remove SSE subscription
  - Add WebSocket subscription
  - Test chat functionality
  - _Requirements: 3.1_

- [ ] 3.1.8 Migrate itinerary updates to WebSocket
  - Update all components listening to updates
  - Remove SSE subscriptions
  - Add WebSocket subscriptions
  - Test real-time updates
  - _Requirements: 3.1_

- [ ] 3.1.9 Remove SSE system
  - Delete `sseManager.ts`
  - Remove SSE endpoints from apiClient
  - Remove feature flag
  - Clean up unused code
  - _Requirements: 3.1_

- [ ] 3.1.10 Test real-time system
  - Test connection/disconnection
  - Test reconnection scenarios
  - Test message delivery
  - Test offline queue
  - _Requirements: 3.1, 3.2_

---

## PHASE 4: Performance Optimization (Weeks 9-10)

### Epic 4.1: Re-render Optimization

- [ ] 4.1.1 Audit expensive re-renders
  - Use React DevTools Profiler
  - Identify components with excessive renders
  - Document render causes
  - Create optimization plan
  - _Requirements: 4.1_

- [ ] 4.1.2 Fix JSON.stringify in dependencies
  - Find all useEffect with JSON.stringify
  - Replace with proper dependencies
  - Use useMemo for derived data
  - Test functionality
  - _Requirements: 4.1_

- [ ] 4.1.3 Memoize expensive computations
  - Add useMemo for data transformations
  - Add useMemo for filtered/sorted lists
  - Add useMemo for complex calculations
  - Measure performance improvement
  - _Requirements: 4.1_

- [ ] 4.1.4 Memoize callbacks
  - Add useCallback for event handlers
  - Add useCallback for callbacks passed to children
  - Ensure stable references
  - Test functionality
  - _Requirements: 4.1_

- [ ] 4.1.5 Memoize expensive components
  - Add React.memo to DayCard
  - Add React.memo to NodeCard
  - Add React.memo to list items
  - Add custom comparison functions
  - _Requirements: 4.1_

- [ ] 4.1.6 Optimize context updates
  - Split contexts by update frequency
  - Use context selectors
  - Minimize context value changes
  - Test render counts
  - _Requirements: 4.1_

### Epic 4.2: Code Splitting

- [ ] 4.2.1 Implement route-based splitting
  - Add lazy loading for TravelPlanner
  - Add lazy loading for WorkflowBuilder
  - Add lazy loading for Dashboard
  - Add Suspense boundaries
  - _Requirements: 4.2_

- [ ] 4.2.2 Implement feature-based splitting
  - Lazy load TripMap component
  - Lazy load NewChat component
  - Lazy load PDFExport component
  - Add loading states
  - _Requirements: 4.2_

- [ ] 4.2.3 Analyze bundle sizes
  - Run webpack-bundle-analyzer
  - Identify large dependencies
  - Document bundle composition
  - Create optimization plan
  - _Requirements: 4.2_

- [ ] 4.2.4 Optimize dependencies
  - Replace large libraries with smaller alternatives
  - Use tree-shaking for libraries
  - Remove unused dependencies
  - Measure size reduction
  - _Requirements: 4.2_

- [ ] 4.2.5 Verify bundle size targets
  - Initial bundle <500KB
  - Route chunks <200KB
  - Feature chunks <100KB
  - Document any exceptions
  - _Requirements: 4.2_

### Epic 4.3: Memory Leak Fixes

- [ ] 4.3.1 Audit useEffect cleanup
  - Scan all useEffect hooks
  - Verify cleanup functions exist
  - Add missing cleanup
  - Test cleanup execution
  - _Requirements: 4.3_

- [ ] 4.3.2 Fix WebSocket cleanup
  - Ensure disconnect on unmount
  - Remove all event listeners
  - Clear subscriptions
  - Test cleanup
  - _Requirements: 4.3_

- [ ] 4.3.3 Fix interval/timeout cleanup
  - Find all setInterval/setTimeout
  - Add clearInterval/clearTimeout
  - Test cleanup
  - _Requirements: 4.3_

- [ ] 4.3.4 Implement LocalStorage cleanup
  - Create StorageManager utility
  - Add timestamp to stored items
  - Implement periodic cleanup
  - Test cleanup
  - _Requirements: 4.3_

- [ ] 4.3.5 Memory profiling
  - Use Chrome DevTools Memory Profiler
  - Test long-running sessions
  - Verify no memory growth
  - Document findings
  - _Requirements: 4.3_

---

## PHASE 5: Testing & Documentation (Week 11)

### Epic 5.1: Unit Testing

- [ ] 5.1.1 Test logger service
  - Test log level filtering
  - Test transports
  - Test timer functionality
  - _Requirements: 5.1_

- [ ] 5.1.2 Test error handler
  - Test error classification
  - Test user messages
  - Test recovery actions
  - _Requirements: 5.1_

- [ ] 5.1.3 Test realtime service
  - Test connection/disconnection
  - Test message handling
  - Test reconnection
  - Test token refresh
  - _Requirements: 5.1_

- [ ] 5.1.4 Test custom hooks
  - Test useItinerary
  - Test useAuth
  - Test useUI
  - _Requirements: 5.1_

- [ ] 5.1.5 Test utility functions
  - Test data transformations
  - Test validation functions
  - Test helper functions
  - _Requirements: 5.1_

- [ ] 5.1.6 Achieve 70% coverage
  - Run coverage report
  - Identify gaps
  - Add missing tests
  - Verify coverage target
  - _Requirements: 5.1_

### Epic 5.2: Integration Testing

- [ ] 5.2.1 Test data flow
  - Test API to UI flow
  - Test optimistic updates
  - Test error handling
  - _Requirements: 5.1_

- [ ] 5.2.2 Test real-time updates
  - Test WebSocket message handling
  - Test cache updates
  - Test UI updates
  - _Requirements: 5.1_

- [ ] 5.2.3 Test state management
  - Test context updates
  - Test React Query integration
  - Test state synchronization
  - _Requirements: 5.1_

### Epic 5.3: E2E Testing

- [ ] 5.3.1 Test critical user flows
  - Test create itinerary flow
  - Test view itinerary flow
  - Test edit itinerary flow
  - _Requirements: 5.1_

- [ ] 5.3.2 Test real-time collaboration
  - Test multi-user updates
  - Test conflict resolution
  - Test lock/unlock
  - _Requirements: 5.1_

- [ ] 5.3.3 Test error recovery
  - Test network errors
  - Test auth errors
  - Test recovery actions
  - _Requirements: 5.1_

### Epic 5.4: Documentation

- [ ] 5.4.1 Write Architecture Decision Records
  - Document data format decision
  - Document context consolidation
  - Document real-time system choice
  - _Requirements: 5.2_

- [ ] 5.4.2 Document component patterns
  - Document loading states
  - Document error handling
  - Document data fetching
  - _Requirements: 5.2_

- [ ] 5.4.3 Create API documentation
  - Document service APIs
  - Document hook APIs
  - Document context APIs
  - _Requirements: 5.2_

- [ ] 5.4.4 Write onboarding guide
  - Document architecture overview
  - Document development setup
  - Document coding standards
  - _Requirements: 5.2_

### Epic 5.5: Performance Testing

- [ ] 5.5.1 Run Lighthouse audits
  - Test desktop performance
  - Test mobile performance
  - Document scores
  - _Requirements: NFR 1_

- [ ] 5.5.2 Load testing
  - Test with large itineraries
  - Test with many users
  - Document results
  - _Requirements: NFR 1_

- [ ] 5.5.3 Memory profiling
  - Test long sessions
  - Verify no leaks
  - Document findings
  - _Requirements: NFR 1_

---

*End of Implementation Plan*
