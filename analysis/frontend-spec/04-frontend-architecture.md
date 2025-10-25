# 4. Frontend Architecture

**Last Updated:** January 25, 2025  
**Sources:** `frontend/src/` directory analysis

---

## 4.1 Project Structure and Organization

### 4.1.1 Directory Overview

```
frontend/src/
├── components/          # React components organized by feature
├── config/             # Configuration files (Firebase, Weather)
├── contexts/           # React Context providers
├── data/               # Static data and constants
├── guidelines/         # Development guidelines
├── hooks/              # Custom React hooks
├── i18n/               # Internationalization setup and translations
├── services/           # API clients and service layers
├── state/              # State management (Zustand + React Query)
├── styles/             # Global styles
├── test/               # Test setup and utilities
├── types/              # TypeScript type definitions
├── utils/              # Utility functions and helpers
├── App.tsx             # Root application component with routing
├── main.tsx            # Application entry point
└── index.css           # Global CSS imports
```

### 4.1.2 Component Organization

The `components/` directory is organized by feature and function:

**Feature-Based Components:**
- `agents/` - Agent progress tracking and control (13 components)
- `booking/` - Booking flow and payment (8 components)
- `chat/` - Chat interface components (3 components)
- `travel-planner/` - Main planner with subdirectories
- `trip-management/` - Trip dashboard and management (4 components)
- `trip-wizard/` - Trip creation wizard
- `workflow/` - Workflow visualization (3 components)
- `workflow-builder/` - Advanced workflow builder

**Functional Components:**
- `ui/` - Radix UI wrapper components (40+ primitives)
- `shared/` - Reusable shared components (14 components)
- `controls/` - UI controls (UndoRedoControls)
- `dialogs/` - Modal dialogs
- `export/` - Export functionality
- `help/` - Help and documentation
- `history/` - Change history
- `layout/` - Layout components
- `loading/` - Loading states
- `locks/` - Node locking
- `notifications/` - Notifications
- `preview/` - Change preview
- `revision/` - Revision history
- `settings/` - Settings and preferences
- `share/` - Sharing functionality
- `sync/` - Synchronization
- `toolbar/` - Toolbars

**Root-Level Components:**
- `App.tsx` - Root component with routing logic
- `TravelPlanner.tsx` - Main itinerary planner (lazy-loaded)
- `WorkflowBuilder.tsx` - Workflow visualization (lazy-loaded)
- `LandingPage.tsx` - Public landing page
- `LoginPage.tsx` - Authentication page
- `GoogleSignIn.tsx` - Google authentication
- `ProtectedRoute.tsx` - Route protection wrapper
- `UserProfile.tsx` - User profile component
- `ItineraryWithChat.tsx` - Itinerary with chat integration
- `NormalizedItineraryViewer.tsx` - Itinerary viewer
- `TripViewLoader.tsx` - Trip data loader

### 4.1.3 Code Splitting Strategy

**Lazy-Loaded Components:**
The application uses React.lazy() for code splitting of heavy components:

```typescript
// From App.tsx
const SimplifiedTripWizard = lazy(() => import('./components/trip-wizard/SimplifiedTripWizard'));
const SimplifiedAgentProgress = lazy(() => import('./components/agents/SimplifiedAgentProgress'));
const TravelPlanner = lazy(() => import('./components/TravelPlanner'));
const CostAndCart = lazy(() => import('./components/booking/CostAndCart'));
const Checkout = lazy(() => import('./components/booking/Checkout'));
const BookingConfirmation = lazy(() => import('./components/booking/BookingConfirmation'));
const ShareView = lazy(() => import('./components/trip-management/ShareView'));
const TripDashboard = lazy(() => import('./components/trip-management/TripDashboard'));
const ItineraryWithChat = lazy(() => import('./components/ItineraryWithChat'));
```

**Eager-Loaded Components:**
Critical components are loaded immediately:
- LandingPage
- LoginPage
- GoogleSignIn
- LoadingState (used as Suspense fallback)

**Benefits:**
- Reduced initial bundle size
- Faster time to interactive
- Better performance on slower connections
- Route-based code splitting

---

## 4.2 Data Flow Patterns

### 4.2.1 Data Flow Architecture

```
User Interaction
      ↓
Component Event Handler
      ↓
┌─────┴─────┐
│           │
Hook/Context  Direct API Call
│           │
└─────┬─────┘
      ↓
Service Layer (apiClient, websocket)
      ↓
Backend API
      ↓
Response
      ↓
┌─────┴─────┐
│           │
React Query  WebSocket Handler
Cache        │
│           │
└─────┬─────┘
      ↓
State Update (Zustand/Context/Local)
      ↓
Component Re-render
      ↓
UI Update
```

### 4.2.2 Data Flow Layers

**1. API Layer**
- **File:** `frontend/src/services/apiClient.ts`
- **Purpose:** Centralized HTTP client with retry logic and token management
- **Features:**
  - Automatic token refresh (5 minutes before expiry)
  - Retry logic with exponential backoff (max 3 retries)
  - Request deduplication
  - Error handling and logging
  - Bearer token authentication

**2. Server State Layer (React Query)**
- **Files:** `frontend/src/state/query/`
- **Purpose:** Manage server data with caching and synchronization
- **Features:**
  - Automatic caching with configurable stale time
  - Background refetching
  - Optimistic updates
  - Automatic retry on failure
  - Query invalidation on mutations
- **Configuration:** `frontend/src/state/query/client.ts`
  - Integrated error handling
  - Exponential backoff retry strategy
  - Toast notifications for errors

**3. Client State Layer (Zustand)**
- **File:** `frontend/src/state/store/useAppStore.ts`
- **Purpose:** Manage UI state and temporary data
- **Persisted State:**
  - `isAuthenticated` - Authentication status
  - `authToken` - JWT token
  - `currentScreen` - Current app screen
- **Non-Persisted State:**
  - `currentTrip` - Current trip data (from React Query)
  - `trips` - List of trips (from React Query)
  - `agentProgress` - Real-time agent progress
  - `overallProgress` - Overall generation progress

**4. Context State Layer**
- **AuthContext:** `frontend/src/contexts/AuthContext.tsx`
  - Firebase authentication state
  - User information
  - Token management methods
- **UnifiedItineraryContext:** `frontend/src/contexts/UnifiedItineraryContext.tsx`
  - Itinerary-specific state
  - Chat messages
  - Workflow nodes/edges
  - Agent data
  - Revisions
  - UI state

**5. Component State Layer**
- **Local useState:** Component-specific UI state
  - Form inputs
  - Modal visibility
  - Loading states
  - UI interactions

### 4.2.3 Real-Time Data Flow

**WebSocket Communication:**
```
Backend WebSocket Server
      ↓
frontend/src/services/websocket.ts
      ↓
STOMP Protocol Handler
      ↓
Topic Subscriptions:
  - /topic/itinerary/{id}
  - /topic/agent/{id}
  - /topic/chat/{id}
      ↓
Message Routing
      ↓
Context/Component State Updates
      ↓
UI Re-renders
```

**Recently Migrated:** SimplifiedAgentProgress migrated from SSE to WebSocket for better reliability and error recovery.

---

## 4.3 State Management Architecture

### 4.3.1 State Management Layers

```
┌─────────────────────────────────────┐
│     React Query (Server State)     │
│  - Itinerary data                   │
│  - User data                        │
│  - Booking data                     │
│  - API caching                      │
└─────────────────────────────────────┘
              ↓
┌─────────────────────────────────────┐
│      Zustand (Client State)         │
│  - UI preferences                   │
│  - View state                       │
│  - Temporary data                   │
│  - Authentication state             │
└─────────────────────────────────────┘
              ↓
┌─────────────────────────────────────┐
│    React Context (Shared State)     │
│  - AuthContext                      │
│  - UnifiedItineraryContext          │
└─────────────────────────────────────┘
              ↓
┌─────────────────────────────────────┐
│   Component State (Local State)     │
│  - Form inputs                      │
│  - Modal states                     │
│  - UI interactions                  │
└─────────────────────────────────────┘
```

### 4.3.2 Zustand Store Structure

**File:** `frontend/src/state/store/useAppStore.ts`

**Store Slices:**
```typescript
interface AppStore {
  // Auth Slice
  isAuthenticated: boolean;
  authToken?: string;
  setAuthenticated: (value: boolean) => void;
  setAuthToken: (token?: string) => void;
  clearAuth: () => void;
  
  // Trip Slice (not persisted - from React Query)
  currentTrip: TripData | null;
  trips: TripData[];
  setCurrentTrip: (trip: TripData | null) => void;
  addTrip: (trip: TripData) => void;
  updateCurrentTrip: (updates: Partial<TripData>) => void;
  
  // UI Slice
  currentScreen: AppScreen;
  setScreen: (screen: AppScreen) => void;
  
  // SSE Slice (legacy - being phased out)
  agentProgress: Record<string, AgentProgressData>;
  overallProgress: number;
  connectSse: (tripId: string) => void;
  disconnectSse: () => void;
  clearSse: () => void;
  
  // Debug utilities
  clearStore: () => void;
  debugSaveToStorage: () => void;
}
```

**Persistence Strategy:**
- Uses `zustand/middleware` persist
- Storage: localStorage
- Only persists UI state (not server data)
- Server data comes from React Query cache

### 4.3.3 React Query Configuration

**File:** `frontend/src/state/query/client.ts`

**Features:**
- Centralized error handling with ErrorHandler utility
- Automatic retry with exponential backoff
- User-friendly error messages via toast notifications
- Integrated logging

**Query Configuration:**
```typescript
{
  refetchOnWindowFocus: false,
  retry: (failureCount, error) => {
    // Uses ErrorHandler.shouldRetry()
    // Exponential backoff: 1s, 2s, 4s, 8s...
  },
  retryDelay: (attemptIndex) => {
    // ErrorHandler.getRetryDelay()
  }
}
```

**Mutation Configuration:**
```typescript
{
  retry: (failureCount, error) => {
    // Uses ErrorHandler.shouldRetry()
    // Shows toast for user-facing errors
  },
  retryDelay: (attemptIndex) => {
    // Exponential backoff
  }
}
```

### 4.3.4 Context Providers

**AuthContext:**
```typescript
interface AuthContextType {
  user: User | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  signIn: (idToken: string) => Promise<void>;
  signOut: () => void;
  getIdToken: () => Promise<string | null>;
}
```

**UnifiedItineraryContext:**
```typescript
interface UnifiedItineraryState {
  itinerary: NormalizedItinerary | null;
  isLoading: boolean;
  error: string | null;
  selectedDay: number;
  selectedNodeId: string | null;
  changes: ChangeOperation[];
  canUndo: boolean;
  canRedo: boolean;
  // ... more state
}

interface UnifiedItineraryActions {
  loadItinerary: (id: string) => void;
  applyChanges: (changes: ChangeOperation[]) => void;
  undo: () => void;
  redo: () => void;
  selectDay: (day: number) => void;
  selectNode: (nodeId: string) => void;
  sendChatMessage: (message: string) => void;
  // ... more actions
}
```

---

## 4.4 Routing and Navigation

### 4.4.1 Route Structure

**File:** `frontend/src/App.tsx`

**Public Routes:**
- `/` - LandingPage (public, shows login if not authenticated)
- `/login` - LoginPage (public)

**Protected Routes (require authentication):**
- `/wizard` - SimplifiedTripWizard
- `/dashboard` - TripDashboard
- `/generating` - SimplifiedAgentProgress (requires currentTrip)
- `/planner` - TravelPlanner (requires currentTrip)
- `/cost` - CostAndCart (requires currentTrip)
- `/checkout` - Checkout (requires currentTrip)
- `/confirmation` - BookingConfirmation (requires currentTrip)
- `/share` - ShareView (requires currentTrip)

**Dynamic Routes:**
- `/trip/:id` - TripRouteLoader (redirects to /planner)
- `/itinerary/:id` - ItineraryRouteLoader (loads and redirects)
- `/itinerary/:id/chat` - ItineraryWithChat

### 4.4.2 Route Protection

**ProtectedRoute Component:**
```typescript
function ProtectedRoute({ 
  children, 
  requireAuth = true 
}: { 
  children: React.ReactNode;
  requireAuth?: boolean;
}) {
  const { isAuthenticated, isLoading } = useAuth();
  
  if (isLoading) return <LoadingSpinner />;
  if (requireAuth && !isAuthenticated) return <Navigate to="/login" />;
  
  return <>{children}</>;
}
```

**RequireTrip Guard:**
```typescript
function RequireTrip({ tripExists }: { tripExists: boolean }) {
  if (!tripExists) {
    return <Navigate to="/" replace />;
  }
  return <Outlet />;
}
```

### 4.4.3 Navigation Patterns

**Programmatic Navigation:**
```typescript
const navigate = useNavigate();
const navigateToScreen = (path: string, tripData?: TripData) => {
  if (tripData) setCurrentTrip(tripData);
  navigate(path);
};
```

**Route Loaders:**
- `TripRouteLoader` - Loads trip from store and redirects
- `PlannerRouteLoader` - Validates currentTrip and loads TripViewLoader
- `ItineraryRouteLoader` - Fetches itinerary and redirects to planner
- `ItineraryChatRouteLoader` - Loads chat interface for itinerary

---

## 4.5 Real-Time Communication

### 4.5.1 WebSocket Integration

**File:** `frontend/src/services/websocket.ts`

**Protocol:** STOMP over SockJS

**Features:**
- Automatic reconnection with exponential backoff
- Connection deduplication (single connection per app)
- Message routing to multiple subscribers
- Graceful error handling

**Topics:**
- `/topic/itinerary/{id}` - Itinerary updates
- `/topic/agent/{id}` - Agent progress updates
- `/topic/chat/{id}` - Chat responses

**Integration Points:**
- UnifiedItineraryContext (primary consumer)
- SimplifiedAgentProgress (agent progress)
- Chat components (chat responses)

### 4.5.2 Message Flow

```
Backend WebSocket → WebSocket Service → Topic Handlers → State Updates → UI Re-renders
```

**Connection Lifecycle:**
1. Component mounts → Subscribe to topic
2. Receive messages → Parse and route
3. Update state → Trigger re-render
4. Component unmounts → Unsubscribe

**Error Handling:**
- Connection failures trigger automatic reconnection
- Exponential backoff prevents server overload
- User notifications for persistent failures
- Graceful degradation (polling fallback planned)

---

**[← Back to Main Specification](../FRONTEND_UI_REDESIGN_SPECIFICATION.md)** | **[Next: Component Catalog →](05-component-catalog.md)**
