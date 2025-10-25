# 5. Component Catalog

**Last Updated:** January 25, 2025  
**Total Components:** 100+  
**Sources:** `frontend/src/components/` directory analysis

---

## 5.1 Page Components

Page components are top-level route components that represent complete application screens.

### 5.1.1 LandingPage

**File:** `frontend/src/components/LandingPage.tsx`  
**Type:** Page (Public Route)  
**Lazy Loaded:** No (eager-loaded for fast initial render)

**Purpose:** Public landing page showcasing the application features and providing entry points for authentication and trip creation.

**Props Interface:**
```typescript
interface LandingPageProps {
  isAuthenticated: boolean;
  onAuthenticate: () => void;
  onStartTrip: () => void;
  onViewTrips: () => void;
  trips: TripData[];
}
```

**State Management:**
- Local state: `showTripModal` (boolean)
- No context consumption
- No React Query hooks

**Backend Dependencies:** None (static content)

**Child Components:**
- GlobalNavigation
- UserProfileButton
- Button (ui)
- Card, CardContent (ui)
- Badge (ui)
- Dialog components (ui)

**User Interactions:**
- Login/Sign up buttons → triggers authentication
- "Start Trip" button → navigates to wizard
- "My Trips" button → navigates to dashboard
- Feature exploration (static content)

**UI Patterns:**
- Hero section with gradient background
- Feature cards with icons
- Responsive layout (mobile/desktop)
- Modal dialogs for additional information


### 5.1.2 LoginPage

**File:** `frontend/src/components/LoginPage.tsx`  
**Type:** Page (Public Route)  
**Lazy Loaded:** No

**Purpose:** Simple authentication page with Google Sign-in integration.

**Props Interface:** None (uses context)

**State Management:**
- Context: AuthContext (`isAuthenticated`, `loading`)
- Redirects to dashboard if already authenticated

**Backend Dependencies:**
- Firebase Authentication (via AuthContext)

**Child Components:**
- GoogleSignIn

**User Interactions:**
- Automatic redirect if authenticated
- Google Sign-in button

**UI Patterns:**
- Centered layout
- Loading spinner during auth check
- Automatic navigation on success

### 5.1.3 TravelPlanner

**File:** `frontend/src/components/TravelPlanner.tsx`  
**Type:** Page (Protected Route)  
**Lazy Loaded:** Yes

**Purpose:** Main itinerary planning interface with multiple views (day-by-day, map, workflow, chat).

**Props Interface:**
```typescript
interface TravelPlannerProps {
  itinerary: NormalizedItinerary;
  onSave: (updatedItinerary: NormalizedItinerary) => void;
  onBack: () => void;
  onShare: () => void;
  onExportPDF: () => void;
}
```

**State Management:**
- Context: UnifiedItineraryProvider (wraps entire component)
- Custom hooks: 
  - `useTravelPlannerState()` - View and tab state
  - `useDestinationsSync()` - Destination management
  - `useFreshItineraryCheck()` - Data freshness
  - `useMapViewModeSync()` - Map synchronization
  - `useMapCenterSync()` - Map center updates
  - `useAgentStatusesSync()` - Agent status tracking
  - `useMapState()` - Map state management
  - `useDeviceDetection()` - Mobile/tablet detection
- React Query: `useItinerary()` hook

**Backend Dependencies:**
- GET /itineraries/{id}/json
- POST /itineraries/{id}:propose
- POST /itineraries/{id}:apply
- WebSocket subscriptions for real-time updates

**Child Components:**
- **Desktop Layout:**
  - NavigationSidebar
  - TopNavigation
  - ResizablePanel
  - TripOverviewView
  - DayByDayView
  - DestinationsManager
  - WorkflowBuilder
  - TripMap (with MapErrorBoundary)
  - NewChat
  - PackingListView
  - BudgetView
  - CollectionView
  - DocumentsView
- **Mobile Layout:**
  - MobileLayout (custom mobile-optimized interface)

**User Interactions:**
- View switching (Overview, Day-by-Day, Map, Workflow, Chat)
- Tab switching within Plan view
- Day selection
- Node selection and editing
- Drag-and-drop (workflow)
- Map interactions (markers, bounds)
- Chat with AI assistant
- Save, share, export actions

**UI Patterns:**
- Responsive layout (desktop sidebar, mobile cards)
- Tabs for view switching
- Resizable panels
- Error boundaries for map and workflow
- Loading states with skeletons
- Real-time updates via WebSocket


### 5.1.4 WorkflowBuilder

**File:** `frontend/src/components/WorkflowBuilder.tsx`  
**Type:** Page (Embedded/Standalone)  
**Lazy Loaded:** Yes

**Purpose:** Visual workflow editor using ReactFlow for drag-and-drop itinerary planning with node-based interface.

**Props Interface:**
```typescript
interface WorkflowBuilderProps {
  tripData: TripData;
  onSave: (updatedTripData: TripData) => void;
  onCancel: () => void;
  embedded?: boolean; // Can be embedded in TravelPlanner
}
```

**State Management:**
- Custom hooks:
  - `useWorkflowBuilderState()` - Workflow state management
  - `useUserChangesSubscription()` - Track user changes
  - `useTripDataSync()` - Sync with trip data
  - `useSavedPositionsSync()` - Node position persistence
  - `useActiveDaySync()` - Active day tracking
  - `useViewportZoomSync()` - Zoom level management
  - `useWorkflowDaysSync()` - Day synchronization
  - `useMapViewModeSync()` - Map integration
  - `useNodePositionTracking()` - Position tracking
  - `useClearBadPositions()` - Position cleanup
  - `useResetToGridPattern()` - Grid layout reset
- ReactFlow: `useNodesState()`, `useEdgesState()`
- Map state: `useMapState()` for node highlighting

**Backend Dependencies:**
- PUT /itineraries/{id}/workflow - Save workflow positions
- PUT /itineraries/{id}/nodes/{nodeId} - Update node details

**Child Components:**
- ReactFlow (third-party)
- WorkflowNode (custom node type)
- NodeInspectorModal
- Controls, Background, MiniMap (ReactFlow)
- Button, Badge, Tabs (ui)

**User Interactions:**
- Drag-and-drop nodes
- Connect nodes with edges
- Click node to inspect/edit
- Zoom and pan viewport
- Reset to grid layout
- Save workflow
- Undo/redo actions

**UI Patterns:**
- Canvas-based interface
- Custom node rendering
- Modal for node inspection
- Toolbar with actions
- Mini-map for navigation
- Grid background

### 5.1.5 ProtectedRoute

**File:** `frontend/src/components/ProtectedRoute.tsx`  
**Type:** Utility Component (Route Guard)  
**Lazy Loaded:** No

**Purpose:** Guards routes based on authentication status, redirects unauthenticated users.

**Props Interface:**
```typescript
interface ProtectedRouteProps {
  children: ReactNode;
  requireAuth?: boolean; // Default: true
  redirectTo?: string;
}
```

**State Management:**
- Context: AuthContext (`user`, `loading`)

**Backend Dependencies:** None (uses auth context)

**User Interactions:**
- Automatic redirect to login if not authenticated
- Preserves attempted location for post-login redirect

**UI Patterns:**
- Loading state during auth check
- Transparent wrapper (renders children or redirects)

### 5.1.6 GoogleSignIn

**File:** `frontend/src/components/GoogleSignIn.tsx`  
**Type:** Feature Component  
**Lazy Loaded:** No

**Purpose:** Google authentication UI with Firebase integration.

**Props Interface:**
```typescript
interface GoogleSignInProps {
  className?: string;
  compact?: boolean; // Compact button vs full card
  onSuccess?: () => void;
}
```

**State Management:**
- Context: AuthContext (`signInWithGoogle`, `loading`)
- Local state: `isSigningIn` (boolean)

**Backend Dependencies:**
- Firebase Authentication
- POST /auth/google (backend token exchange)

**UI Patterns:**
- Two variants: compact button or full card
- Google branding (logo, colors)
- Loading spinner during sign-in
- Error handling

---

## 5.2 Feature Components

Feature components implement specific application features and are organized by domain.

### 5.2.1 Agent Components (13 components)

**Directory:** `frontend/src/components/agents/`

Components for tracking and controlling AI agent execution during itinerary generation.


#### SimplifiedAgentProgress

**File:** `frontend/src/components/agents/SimplifiedAgentProgress.tsx`  
**Purpose:** Main agent progress tracking component with WebSocket integration for real-time updates.

**Props:**
```typescript
interface SimplifiedAgentProgressProps {
  tripData: TripData;
  onComplete: () => void;
  onCancel: () => void;
  retryAttempt?: number;
  maxRetries?: number;
}
```

**Key Features:**
- WebSocket connection for real-time agent updates
- Smooth progress animation with stages
- Fallback polling if WebSocket fails
- Auto-completion at 100% progress
- Retry mechanism with countdown
- Error handling and recovery

**State Management:**
- Local state: agents, progress, completion status, error state
- Custom hooks: `useGenerationStatus()`, `useProgressWithStages()`
- React Query: Invalidates itinerary cache on completion

**Backend Dependencies:**
- WebSocket: `/topic/agent/{itineraryId}`
- Polling fallback: GET /agents/status/{itineraryId}

**Recently Updated:** Migrated from SSE to WebSocket for better reliability.

#### Agent Components Summary

| Component | Purpose | Key Features |
|-----------|---------|--------------|
| **AgentConfigModal** | Configure agent parameters | Modal dialog, form inputs, validation |
| **AgentControlPanel** | Control agent execution | Start/stop/pause controls, status display |
| **AgentErrorDisplay** | Display agent errors | Error messages, retry options, diagnostics |
| **AgentExecutionDetail** | Detailed execution view | Step-by-step progress, logs, timing |
| **AgentExecutionProgress** | Progress visualization | Progress bars, status indicators, ETA |
| **AgentHistoryPanel** | Execution history | Past runs, results, comparison |
| **AgentOrchestrator** | Orchestrate multiple agents | Parallel execution, dependencies, coordination |
| **AgentProgressBar** | Simple progress bar | Linear progress, percentage, status |
| **AgentProgressModal** | Modal progress display | Full-screen progress, cancellation |
| **AgentResultsPanel** | Display agent results | Results summary, data visualization |
| **EnhancedGenerationProgress** | Enhanced progress UI | Detailed stages, animations, messaging |
| **GeneratingPlan** | Legacy progress component | Older implementation, being phased out |

---

### 5.2.2 Booking Components (8 components)

**Directory:** `frontend/src/components/booking/`

Components for the booking flow including hotel, activity, and transportation booking with payment integration.


#### Booking Components Summary

| Component | Purpose | Key Features |
|-----------|---------|--------------|
| **BookingModal** | Main booking interface | Modal dialog, provider selection, pricing, availability |
| **Checkout** | Payment checkout flow | Razorpay integration, payment form, order summary |
| **BookingConfirmation** | Booking success page | Confirmation details, booking reference, next steps |
| **CostAndCart** | Shopping cart view | Cost breakdown, item management, checkout button |
| **HotelBookingSystem** | Hotel-specific booking | Room selection, dates, guest details, pricing |
| **BookingCancellation** | Cancel bookings | Cancellation form, refund policy, confirmation |
| **BookedNodeIndicator** | Visual booking indicator | Badge/icon showing node is booked |
| **BookingErrorDisplay** | Booking error handling | Error messages, retry options, support contact |

**Common Features:**
- Razorpay payment gateway integration
- Real-time pricing updates
- Booking status tracking
- Error handling and recovery
- Confirmation emails
- Booking history

**Backend Dependencies:**
- POST /payments/razorpay/order
- POST /providers/{vertical}/{provider}:book
- GET /bookings/{id}
- POST /bookings/{id}/cancel

---

### 5.2.3 Chat Components (3 components)

**Directory:** `frontend/src/components/chat/`

Components for AI chat interface allowing natural language itinerary modifications.

#### Chat Components Summary

| Component | Purpose | Key Features |
|-----------|---------|--------------|
| **NewChat** | Main chat interface | Message history, input field, real-time responses, typing indicator |
| **ChatMessage** | Individual message display | User/AI differentiation, timestamps, formatting, actions |
| **DisambiguationPanel** | Clarification UI | Multiple choice options, context clarification, selection handling |

**Key Features:**
- WebSocket integration for real-time responses
- Message history persistence (local storage)
- Typing indicators
- Change proposals with preview
- Context-aware suggestions
- Disambiguation for ambiguous requests

**Backend Dependencies:**
- POST /agents/process-request
- WebSocket: `/topic/chat/{itineraryId}`

---

### 5.2.4 Travel Planner Components

**Directory:** `frontend/src/components/travel-planner/`

Large collection of components for the main itinerary planning interface, organized into subdirectories.

#### Subdirectories:

**cards/** - Display cards for different node types
- ActivityCard
- MealCard
- AccommodationCard
- TransportCard
- DayCard
- NodeCard (generic)

**layout/** - Layout components
- NavigationSidebar
- TopNavigation
- ResizablePanel
- AppLayout

**mobile/** - Mobile-specific components
- MobileLayout
- MobilePlanView
- MobilePlanDetailView
- MobileMapDetailView
- MobileChatDetailView

**modals/** - Modal dialogs
- AddActivityModal
- EditNodeModal
- DeleteConfirmationModal
- ShareModal

**shared/** - Shared utilities
- ErrorBoundary
- types.ts (shared types)
- constants.ts

**views/** - Main view components
- TripOverviewView
- DayByDayView
- DestinationsManager
- PackingListView
- BudgetView
- CollectionView
- DocumentsView

#### Key Components Detail:

**TripOverviewView**
- Trip statistics and summary
- Weather information
- Cost breakdown
- Export options
- Quick actions

**DayByDayView**
- Expandable day cards
- Activity timeline
- Drag-and-drop reordering
- Add/edit/delete activities
- Real-time updates

**TripMap**
- Google Maps integration
- Activity markers with clustering
- Route visualization
- Bounds fitting
- Marker info windows
- Terrain control

**DestinationsManager**
- Add/remove destinations
- Transport planning between destinations
- Route optimization
- Multi-city itineraries

---

### 5.2.5 Trip Management Components (4 components)

**Directory:** `frontend/src/components/trip-management/`

| Component | Purpose | Key Features |
|-----------|---------|--------------|
| **TripDashboard** | List all user trips | Grid/list view, search, filter, sort, create new |
| **ItineraryOverview** | Trip summary card | Key details, thumbnail, status, quick actions |
| **EditMode** | Edit trip metadata | Title, dates, travelers, preferences |
| **ShareView** | Share trip publicly | Generate share link, email sharing, permissions |

---

### 5.2.6 Workflow Components (3 components)

**Directory:** `frontend/src/components/workflow/`

| Component | Purpose | Key Features |
|-----------|---------|--------------|
| **WorkflowNode** | Custom ReactFlow node | Node rendering, status indicators, actions |
| **NodeInspectorModal** | Node details modal | Full node information, edit form, validation |
| **WorkflowUtils** | Utility functions | Node validation, position calculation, layout |

---

## 5.3 Shared Components (14 components)

**Directory:** `frontend/src/components/shared/`

Reusable components used across multiple features.

| Component | Purpose | Usage |
|-----------|---------|-------|
| **GlobalHeader** | App header | Logo, navigation, user menu |
| **GlobalNavigation** | Main navigation | Sidebar/horizontal nav, active state |
| **GlobalErrorBoundary** | Error boundary | Catch React errors, fallback UI |
| **LoadingState** | Loading indicator | Spinner, skeleton, progress |
| **ErrorDisplay** | Error messages | User-friendly errors, retry |
| **KeyboardShortcuts** | Keyboard handler | Global shortcuts, help modal |
| **UserProfileButton** | User menu | Avatar, dropdown, logout |
| **LanguageSelector** | i18n switcher | Language dropdown, flags |
| **AutoRefreshEmptyState** | Empty state | No data message, refresh |
| **Breadcrumbs** | Navigation breadcrumbs | Path display, clickable |
| **SearchBar** | Search input | Debounced search, clear |
| **FilterPanel** | Filter controls | Multiple filters, reset |
| **SortDropdown** | Sort options | Sort by field, direction |
| **Pagination** | Page navigation | Page numbers, prev/next |

---

## 5.4 UI Primitives (40+ components)

**Directory:** `frontend/src/components/ui/`

Radix UI wrapper components with custom styling. These are low-level building blocks.

### 5.4.1 Form Controls

| Component | Radix Base | Purpose |
|-----------|------------|---------|
| **Button** | - | Buttons with variants |
| **Input** | - | Text inputs |
| **Textarea** | - | Multi-line text |
| **Select** | @radix-ui/react-select | Dropdown select |
| **Checkbox** | @radix-ui/react-checkbox | Checkboxes |
| **RadioGroup** | @radix-ui/react-radio-group | Radio buttons |
| **Switch** | @radix-ui/react-switch | Toggle switches |
| **Slider** | @radix-ui/react-slider | Range sliders |
| **Label** | @radix-ui/react-label | Form labels |

### 5.4.2 Overlays

| Component | Radix Base | Purpose |
|-----------|------------|---------|
| **Dialog** | @radix-ui/react-dialog | Modal dialogs |
| **AlertDialog** | @radix-ui/react-alert-dialog | Confirmation dialogs |
| **Sheet** | @radix-ui/react-dialog | Side panels |
| **Popover** | @radix-ui/react-popover | Popovers |
| **Tooltip** | @radix-ui/react-tooltip | Tooltips |
| **DropdownMenu** | @radix-ui/react-dropdown-menu | Dropdown menus |
| **ContextMenu** | @radix-ui/react-context-menu | Right-click menus |
| **HoverCard** | @radix-ui/react-hover-card | Hover cards |

### 5.4.3 Navigation

| Component | Radix Base | Purpose |
|-----------|------------|---------|
| **Tabs** | @radix-ui/react-tabs | Tab navigation |
| **Accordion** | @radix-ui/react-accordion | Collapsible sections |
| **NavigationMenu** | @radix-ui/react-navigation-menu | Navigation menus |
| **Menubar** | @radix-ui/react-menubar | Menu bars |

### 5.4.4 Feedback

| Component | Radix Base | Purpose |
|-----------|------------|---------|
| **Progress** | @radix-ui/react-progress | Progress bars |
| **Toast** | Sonner | Toast notifications |
| **Alert** | - | Alert messages |
| **Badge** | - | Status badges |
| **Skeleton** | - | Loading skeletons |

### 5.4.5 Layout

| Component | Radix Base | Purpose |
|-----------|------------|---------|
| **Card** | - | Content cards |
| **Separator** | @radix-ui/react-separator | Dividers |
| **ScrollArea** | @radix-ui/react-scroll-area | Custom scrollbars |
| **AspectRatio** | @radix-ui/react-aspect-ratio | Aspect ratio containers |
| **Collapsible** | @radix-ui/react-collapsible | Collapsible content |

### 5.4.6 Data Display

| Component | Radix Base | Purpose |
|-----------|------------|---------|
| **Table** | - | Data tables |
| **Avatar** | @radix-ui/react-avatar | User avatars |
| **Calendar** | - | Date picker |
| **Command** | cmdk | Command palette |

---

**[← Back to Main Specification](../FRONTEND_UI_REDESIGN_SPECIFICATION.md)** | **[Next: Feature Mapping →](06-feature-mapping.md)**
