# Design Document: Frontend Architecture Refactor

## Overview

This document provides the technical design for refactoring the Travel Planner frontend application. The design addresses critical architectural issues while maintaining backward compatibility during migration.

**Design Principles:**
1. **Zero Code Duplication** - Every piece of logic exists in exactly ONE place
2. **Reusability First** - Extract common patterns to shared modules before writing new code
3. **Incremental Migration** - Changes are applied gradually with feature flags
4. **Single Source of Truth** - Data flows from one authoritative source
5. **Separation of Concerns** - Clear boundaries between layers
6. **Performance First** - Optimize for user experience
7. **Type Safety** - Leverage TypeScript for reliability

**Reusability Guidelines:**
- Before writing new code, search for existing implementations
- Extract state logic to custom hooks
- Extract event handlers to helper functions
- Extract UI patterns to shared components
- Extract types to dedicated type files
- See `CODE_REUSABILITY_GUIDE.md` for detailed patterns

---

## Architecture

### Current Architecture (Problematic)

```
┌─────────────────────────────────────────────────────────┐
│                    Component Layer                       │
│  (TravelPlanner, WorkflowBuilder, DayByDayView, etc.)  │
│         Multiple data formats, inconsistent patterns    │
└─────────────────────────────────────────────────────────┘
                          ↓ ↑
┌─────────────────────────────────────────────────────────┐
│                   Context Layer (Chaos)                  │
│  UnifiedItineraryContext (1,389 lines)                  │
│  AuthContext, MapContext, PreviewSettingsContext        │
│         Overlapping responsibilities, prop drilling     │
└─────────────────────────────────────────────────────────┘
                          ↓ ↑
┌─────────────────────────────────────────────────────────┐
│              Data Transformation Layer                   │
│  normalizedDataTransformer.ts ←→ dataTransformer.ts    │
│         Constant transformation overhead                 │
└─────────────────────────────────────────────────────────┘
                          ↓ ↑
┌─────────────────────────────────────────────────────────┐
│                   API/Service Layer                      │
│  apiClient.ts, websocket.ts, sseManager.ts             │
│         Dual real-time systems, inconsistent patterns   │
└─────────────────────────────────────────────────────────┘
                          ↓ ↑
┌─────────────────────────────────────────────────────────┐
│                      Backend API                         │
│              (NormalizedItinerary format)                │
└─────────────────────────────────────────────────────────┘
```

### Target Architecture (Clean)

```
┌─────────────────────────────────────────────────────────┐
│                    Component Layer                       │
│  Smart Components: TravelPlanner, WorkflowBuilder       │
│  Presentational: DayCard, NodeCard, MapMarker          │
│         Single data format, consistent patterns         │
└─────────────────────────────────────────────────────────┘
                          ↓ ↑
┌─────────────────────────────────────────────────────────┐
│                   Hook Layer (Custom)                    │
│  useItinerary, useAuth, useRealtime, useUI             │
│         Encapsulate business logic and state            │
└─────────────────────────────────────────────────────────┘
                          ↓ ↑
┌─────────────────────────────────────────────────────────┐
│              State Management Layer                      │
│  React Query Cache (single source of truth)             │
│  3 Contexts: AppContext, ItineraryContext, UIContext   │
│         Clear responsibilities, no overlap              │
└─────────────────────────────────────────────────────────┘
                          ↓ ↑
┌─────────────────────────────────────────────────────────┐
│                   API/Service Layer                      │
│  apiClient.ts (REST), realtimeService.ts (WebSocket)   │
│         Single real-time system, consistent patterns    │
└─────────────────────────────────────────────────────────┘
                          ↓ ↑
┌─────────────────────────────────────────────────────────┐
│                      Backend API                         │
│              (NormalizedItinerary format)                │
└─────────────────────────────────────────────────────────┘
```

---

## Component Architecture

### File Structure (Target)

```
frontend/src/
├── components/
│   ├── travel-planner/
│   │   ├── TravelPlanner.tsx              (<200 lines)
│   │   ├── TravelPlannerViews.tsx         (view logic)
│   │   ├── TravelPlannerState.ts          (state hooks)
│   │   ├── views/
│   │   │   ├── DayByDayView.tsx
│   │   │   ├── DestinationsView.tsx
│   │   │   └── OverviewView.tsx
│   │   └── cards/
│   │       ├── DayCard.tsx
│   │       └── NodeCard.tsx
│   ├── workflow/
│   │   ├── WorkflowBuilder.tsx            (<200 lines)
│   │   ├── WorkflowCanvas.tsx
│   │   ├── nodes/
│   │   └── edges/
│   └── shared/
│       ├── LoadingState.tsx               (standardized)
│       ├── ErrorBoundary.tsx
│       └── EmptyState.tsx
├── contexts/
│   ├── AppContext.tsx                     (<300 lines)
│   ├── ItineraryContext.tsx               (<300 lines)
│   └── UIContext.tsx                      (<300 lines)
├── hooks/
│   ├── useItinerary.ts
│   ├── useAuth.ts
│   ├── useRealtime.ts
│   └── useUI.ts
├── services/
│   ├── api/
│   │   ├── apiClient.ts
│   │   └── endpoints/
│   ├── realtime/
│   │   └── realtimeService.ts             (WebSocket only)
│   └── logging/
│       └── logger.ts                      (centralized)
├── types/
│   └── NormalizedItinerary.ts             (single format)
└── utils/
    ├── errorHandler.ts
    └── performance.ts
```


## Data Flow Design

### Current Data Flow (Problematic)

```
Backend (NormalizedItinerary)
    ↓
apiClient.getItinerary()
    ↓
NormalizedDataTransformer.transform()
    ↓
TripData (frontend format)
    ↓
UnifiedItineraryContext (1,389 lines)
    ↓
Multiple local states
    ↓
Components (inconsistent data)
```

**Problems:**
- Expensive transformation on every API call
- Multiple sources of truth
- Race conditions
- Stale data

### Target Data Flow (Clean)

```
Backend (NormalizedItinerary)
    ↓
apiClient.getItinerary()
    ↓
React Query Cache (single source of truth)
    ↓
Custom Hooks (useItinerary, etc.)
    ↓
Components (consistent data)
```

**Benefits:**
- No transformation overhead
- Single source of truth
- Automatic caching
- Optimistic updates
- Real-time sync

---

## State Management Design

### Context Consolidation

#### 1. AppContext (Authentication & User)

**Responsibility:** User authentication, profile, and app-level settings

```typescript
interface AppState {
  user: User | null;
  isAuthenticated: boolean;
  preferences: UserPreferences;
  theme: 'light' | 'dark';
}

interface AppContextType {
  state: AppState;
  login: () => Promise<void>;
  logout: () => Promise<void>;
  updatePreferences: (prefs: Partial<UserPreferences>) => void;
}
```

**File Size:** <300 lines

#### 2. ItineraryContext (Data & Operations)

**Responsibility:** Itinerary data access and mutations

```typescript
interface ItineraryState {
  currentItineraryId: string | null;
  isLoading: boolean;
  error: Error | null;
}

interface ItineraryContextType {
  state: ItineraryState;
  // Data access via React Query hooks
  useCurrentItinerary: () => UseQueryResult<NormalizedItinerary>;
  // Mutations
  updateNode: UseMutationResult<...>;
  addNode: UseMutationResult<...>;
  removeNode: UseMutationResult<...>;
}
```

**File Size:** <300 lines
**Note:** Actual data comes from React Query, context only manages IDs and UI state

#### 3. UIContext (UI State)

**Responsibility:** UI-specific state (modals, sidebars, selections)

```typescript
interface UIState {
  sidebarOpen: boolean;
  selectedDay: number | null;
  selectedNodes: string[];
  activeModal: string | null;
  viewMode: 'day-by-day' | 'workflow' | 'timeline';
}

interface UIContextType {
  state: UIState;
  openSidebar: () => void;
  closeSidebar: () => void;
  selectDay: (day: number) => void;
  selectNodes: (nodeIds: string[]) => void;
  openModal: (modalId: string) => void;
  closeModal: () => void;
}
```

**File Size:** <200 lines


---

## Real-time Communication Design

### Current System (Dual)

**SSE (sseManager.ts):**
- Used for agent progress updates
- Cannot refresh auth tokens
- Breaks after 1 hour
- Limited browser support

**WebSocket (websocket.ts):**
- Used for chat and itinerary updates
- Can refresh tokens
- More reliable
- Better browser support

**Problem:** Two systems doing similar things, causing conflicts

### Target System (WebSocket Only)

**Architecture:**

```typescript
class RealtimeService {
  private ws: WebSocket | null = null;
  private reconnectAttempts = 0;
  private messageQueue: Message[] = [];
  
  // Connection management
  connect(itineraryId: string): Promise<void>
  disconnect(): void
  reconnect(): Promise<void>
  
  // Message handling
  send(message: Message): void
  subscribe(topic: string, handler: MessageHandler): Unsubscribe
  
  // Token management
  refreshToken(): Promise<void>
  
  // Health monitoring
  ping(): void
  onConnectionChange(handler: (connected: boolean) => void): void
}
```

**Message Types:**

```typescript
type RealtimeMessage = 
  | { type: 'itinerary_updated'; data: NormalizedItinerary }
  | { type: 'agent_progress'; agentId: string; progress: number }
  | { type: 'chat_message'; message: ChatMessage }
  | { type: 'node_locked'; nodeId: string; userId: string }
  | { type: 'node_unlocked'; nodeId: string }
  | { type: 'error'; error: string };
```

**Integration with React Query:**

```typescript
// Automatic cache updates from WebSocket
realtimeService.subscribe('itinerary_updated', (message) => {
  queryClient.setQueryData(
    ['itinerary', message.data.itineraryId],
    message.data
  );
});
```

**Benefits:**
- Single connection
- Automatic token refresh
- Reliable reconnection
- Integrated with React Query
- Offline queue support

---

## Logging Design

### Current System (Problematic)

- 100+ console.log statements
- No log levels
- Sensitive data exposed
- Performance impact
- No centralized control

### Target System (Centralized Logger)

**Logger Service:**

```typescript
enum LogLevel {
  DEBUG = 0,
  INFO = 1,
  WARN = 2,
  ERROR = 3,
  NONE = 4
}

class Logger {
  private level: LogLevel;
  private transports: LogTransport[];
  
  debug(message: string, context?: object): void
  info(message: string, context?: object): void
  warn(message: string, context?: object): void
  error(message: string, error?: Error, context?: object): void
  
  // Performance tracking
  startTimer(label: string): () => void
  
  // User actions
  trackAction(action: string, metadata?: object): void
}
```

**Configuration:**

```typescript
// Development
logger.setLevel(LogLevel.DEBUG);
logger.addTransport(new ConsoleTransport());

// Production
logger.setLevel(LogLevel.ERROR);
logger.addTransport(new SentryTransport());
logger.addTransport(new AnalyticsTransport());
```

**Usage:**

```typescript
// Before (BAD)
console.log('=== TRAVEL PLANNER COMPONENT RENDER ===');
console.log('Trip Data Props:', tripData);

// After (GOOD)
logger.debug('TravelPlanner rendered', { 
  tripId: tripData.id,
  hasItinerary: !!tripData.itinerary 
});
```


---

## Error Handling Design

### Current System (Inconsistent)

- Try-catch with fallbacks
- Component error states
- React Query errors
- Global error boundary
- No consistent UX

### Target System (Centralized)

**Error Handler Service:**

```typescript
class ErrorHandler {
  // Error classification
  classify(error: Error): ErrorType
  
  // User-friendly messages
  getUserMessage(error: Error): string
  
  // Recovery actions
  getRecoveryActions(error: Error): RecoveryAction[]
  
  // Logging
  logError(error: Error, context?: object): void
  
  // Reporting
  reportError(error: Error): void
}

enum ErrorType {
  NETWORK = 'network',
  AUTH = 'auth',
  VALIDATION = 'validation',
  NOT_FOUND = 'not_found',
  SERVER = 'server',
  UNKNOWN = 'unknown'
}

interface RecoveryAction {
  label: string;
  action: () => void;
  primary?: boolean;
}
```

**Error Boundary Component:**

```typescript
<ErrorBoundary
  fallback={(error, reset) => (
    <ErrorDisplay
      error={error}
      message={errorHandler.getUserMessage(error)}
      actions={errorHandler.getRecoveryActions(error)}
      onReset={reset}
    />
  )}
  onError={(error, errorInfo) => {
    errorHandler.logError(error, errorInfo);
    errorHandler.reportError(error);
  }}
>
  <App />
</ErrorBoundary>
```

**React Query Integration:**

```typescript
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      onError: (error) => {
        errorHandler.logError(error);
        // Don't show UI for background refetches
      }
    },
    mutations: {
      onError: (error) => {
        errorHandler.logError(error);
        toast.error(errorHandler.getUserMessage(error));
      }
    }
  }
});
```

---

## Loading States Design

### Standardized Loading Components

#### 1. Full-Page Loading

**Use Case:** Initial app load, route transitions

```typescript
<LoadingState
  variant="fullPage"
  message="Loading your itinerary..."
  showProgress={false}
/>
```

#### 2. Inline Loading

**Use Case:** Component updates, data refetch

```typescript
<LoadingState
  variant="inline"
  size="small"
  message="Updating..."
/>
```

#### 3. Progress Loading

**Use Case:** Long operations (agent execution, file upload)

```typescript
<LoadingState
  variant="progress"
  progress={agentProgress}
  message="Generating itinerary..."
  estimatedTime="2-3 minutes"
  onCancel={() => cancelOperation()}
/>
```

#### 4. Skeleton Loading

**Use Case:** Content placeholders

```typescript
<SkeletonLoader
  type="dayCard"
  count={3}
/>
```

**Implementation:**

```typescript
interface LoadingStateProps {
  variant: 'fullPage' | 'inline' | 'progress' | 'skeleton';
  message?: string;
  progress?: number;
  estimatedTime?: string;
  size?: 'small' | 'medium' | 'large';
  onCancel?: () => void;
}
```


---

## Performance Optimization Design

### 1. Re-render Optimization

**Strategy:**

```typescript
// Before (BAD)
useEffect(() => {
  setDestinations(currentTripData.itinerary.days.map(...));
}, [JSON.stringify(currentTripData.itinerary?.days)]);

// After (GOOD)
const destinations = useMemo(() => {
  return currentTripData.itinerary?.days?.map(...) ?? [];
}, [currentTripData.itinerary?.days]);

// Memoized callbacks
const handleDaySelect = useCallback((dayNumber: number) => {
  setSelectedDay(dayNumber);
}, []);

// Memoized components
const DayCard = React.memo(({ day, onSelect }) => {
  // Component implementation
}, (prevProps, nextProps) => {
  // Custom comparison
  return prevProps.day.id === nextProps.day.id &&
         prevProps.day.updatedAt === nextProps.day.updatedAt;
});
```

### 2. Code Splitting Strategy

**Route-based Splitting:**

```typescript
const TravelPlanner = lazy(() => import('./components/TravelPlanner'));
const WorkflowBuilder = lazy(() => import('./components/WorkflowBuilder'));
const Dashboard = lazy(() => import('./components/Dashboard'));

<Suspense fallback={<LoadingState variant="fullPage" />}>
  <Routes>
    <Route path="/planner/:id" element={<TravelPlanner />} />
    <Route path="/workflow/:id" element={<WorkflowBuilder />} />
    <Route path="/dashboard" element={<Dashboard />} />
  </Routes>
</Suspense>
```

**Feature-based Splitting:**

```typescript
// Lazy load heavy features
const MapComponent = lazy(() => import('./components/TripMap'));
const ChatInterface = lazy(() => import('./components/chat/NewChat'));
const PDFExport = lazy(() => import('./components/PDFExport'));

// Use only when needed
{showMap && (
  <Suspense fallback={<LoadingState variant="inline" />}>
    <MapComponent />
  </Suspense>
)}
```

**Bundle Analysis:**

```bash
# Analyze bundle size
npm run build -- --analyze

# Target sizes:
# - Initial bundle: <500KB
# - Route chunks: <200KB
# - Feature chunks: <100KB
```

### 3. Memory Leak Prevention

**Cleanup Pattern:**

```typescript
useEffect(() => {
  // Setup
  const subscription = realtimeService.subscribe('updates', handler);
  const interval = setInterval(pollData, 5000);
  
  // Cleanup
  return () => {
    subscription.unsubscribe();
    clearInterval(interval);
  };
}, [dependencies]);
```

**LocalStorage Cleanup:**

```typescript
class StorageManager {
  private maxAge = 7 * 24 * 60 * 60 * 1000; // 7 days
  
  set(key: string, value: any): void {
    const item = {
      value,
      timestamp: Date.now()
    };
    localStorage.setItem(key, JSON.stringify(item));
  }
  
  get(key: string): any | null {
    const item = localStorage.getItem(key);
    if (!item) return null;
    
    const { value, timestamp } = JSON.parse(item);
    if (Date.now() - timestamp > this.maxAge) {
      localStorage.removeItem(key);
      return null;
    }
    
    return value;
  }
  
  cleanup(): void {
    // Remove old items
    for (let i = 0; i < localStorage.length; i++) {
      const key = localStorage.key(i);
      if (key) this.get(key); // Triggers cleanup
    }
  }
}
```


---

## Migration Strategy

### Phase 1: Stabilization (Weeks 1-2)

**Goal:** Remove technical debt without breaking functionality

**Steps:**

1. **Replace console.log with logger**
   - Create logger service
   - Replace all console.log statements
   - Configure for dev/prod environments

2. **Implement error boundary**
   - Create ErrorBoundary component
   - Create ErrorDisplay component
   - Wrap app with boundary

3. **Standardize loading states**
   - Create LoadingState component
   - Replace all loading indicators
   - Add skeleton loaders

**Feature Flag:** None (safe changes)

### Phase 2: Data Format Migration (Weeks 3-4)

**Goal:** Eliminate TripData, use NormalizedItinerary everywhere

**Steps:**

1. **Create compatibility layer**
   ```typescript
   // Temporary adapter
   function useLegacyTripData(itinerary: NormalizedItinerary): TripData {
     // Convert on-the-fly for old components
   }
   ```

2. **Migrate components one by one**
   - Start with leaf components (DayCard, NodeCard)
   - Move up to container components
   - Update TravelPlanner last

3. **Remove TripData type**
   - Delete TripData.ts
   - Delete transformation services
   - Remove compatibility layer

**Feature Flag:** `USE_NORMALIZED_FORMAT`

### Phase 3: Context Consolidation (Weeks 5-6)

**Goal:** Reduce contexts from 5 to 3

**Steps:**

1. **Create new contexts**
   - Implement AppContext
   - Implement ItineraryContext (thin wrapper around React Query)
   - Implement UIContext

2. **Migrate components**
   - Update components to use new contexts
   - Remove old context usage
   - Test thoroughly

3. **Remove old contexts**
   - Delete UnifiedItineraryContext
   - Delete PreviewSettingsContext
   - Merge MapContext into UIContext

**Feature Flag:** `USE_NEW_CONTEXTS`

### Phase 4: Real-time Consolidation (Weeks 7-8)

**Goal:** Single WebSocket system

**Steps:**

1. **Implement RealtimeService**
   - Create new WebSocket service
   - Add token refresh
   - Add reconnection logic

2. **Migrate subscriptions**
   - Move agent progress to WebSocket
   - Move chat to WebSocket
   - Move itinerary updates to WebSocket

3. **Remove SSE**
   - Delete sseManager.ts
   - Remove SSE endpoints
   - Update backend if needed

**Feature Flag:** `USE_WEBSOCKET_ONLY`

### Phase 5: Performance (Weeks 9-10)

**Goal:** Fast, responsive application

**Steps:**

1. **Optimize re-renders**
   - Add React.memo to expensive components
   - Add useMemo for expensive computations
   - Add useCallback for callbacks

2. **Implement code splitting**
   - Split routes
   - Split features
   - Analyze bundle sizes

3. **Fix memory leaks**
   - Audit all useEffect hooks
   - Add cleanup functions
   - Implement storage cleanup

**Feature Flag:** None (performance improvements)

### Phase 6: Testing & Documentation (Week 11)

**Goal:** Confidence and knowledge transfer

**Steps:**

1. **Write tests**
   - Unit tests for utilities
   - Integration tests for flows
   - E2E tests for critical paths

2. **Document architecture**
   - Write ADRs
   - Document patterns
   - Create onboarding guide

3. **Performance testing**
   - Lighthouse audits
   - Load testing
   - Memory profiling

**Feature Flag:** None

---

## Rollback Plan

Each phase has a rollback strategy:

### Phase 1-2: Simple Rollback
- Revert commits
- No data migration needed
- No user impact

### Phase 3-4: Feature Flag Rollback
- Disable feature flag
- Old code still present
- Gradual rollout possible

### Phase 5-6: Partial Rollback
- Keep performance improvements
- Revert only problematic changes
- Monitor metrics closely

---

## Testing Strategy

### Unit Tests

**Coverage Target:** 70%

**Focus Areas:**
- Utility functions
- Custom hooks
- Context providers
- Service classes

**Example:**

```typescript
describe('RealtimeService', () => {
  it('should connect to WebSocket', async () => {
    const service = new RealtimeService();
    await service.connect('itinerary-123');
    expect(service.isConnected()).toBe(true);
  });
  
  it('should reconnect on disconnect', async () => {
    const service = new RealtimeService();
    await service.connect('itinerary-123');
    service.disconnect();
    await service.reconnect();
    expect(service.isConnected()).toBe(true);
  });
});
```

### Integration Tests

**Focus Areas:**
- Data flow from API to UI
- Real-time updates
- Error handling
- State management

**Example:**

```typescript
describe('Itinerary Updates', () => {
  it('should update UI when itinerary changes', async () => {
    render(<TravelPlanner tripId="123" />);
    
    // Wait for initial load
    await waitFor(() => {
      expect(screen.getByText('Day 1')).toBeInTheDocument();
    });
    
    // Simulate real-time update
    realtimeService.emit('itinerary_updated', updatedItinerary);
    
    // Verify UI updated
    await waitFor(() => {
      expect(screen.getByText('Updated Day 1')).toBeInTheDocument();
    });
  });
});
```

### E2E Tests

**Critical Paths:**
1. Create itinerary
2. View itinerary
3. Edit itinerary
4. Real-time collaboration
5. Error recovery

**Example:**

```typescript
test('user can create and view itinerary', async ({ page }) => {
  await page.goto('/dashboard');
  await page.click('text=Create Itinerary');
  
  await page.fill('[name="destination"]', 'Paris');
  await page.fill('[name="startDate"]', '2025-06-01');
  await page.fill('[name="endDate"]', '2025-06-07');
  await page.click('text=Generate');
  
  await page.waitForSelector('text=Day 1');
  expect(await page.textContent('h1')).toContain('Paris');
});
```

---

## Monitoring & Metrics

### Performance Metrics

- **Initial Load Time:** <3s (desktop), <8s (mobile 3G)
- **Time to Interactive:** <5s (desktop), <12s (mobile 3G)
- **Bundle Size:** <500KB (initial), <200KB (routes)
- **Memory Usage:** <100MB (after 1 hour)

### Error Metrics

- **Error Rate:** <1% of requests
- **Error Recovery:** >90% successful
- **User-Reported Errors:** <5 per week

### User Experience Metrics

- **Task Completion Rate:** >95%
- **User Satisfaction:** >4.5/5
- **Support Tickets:** <10 per week

---

*End of Design Document*
