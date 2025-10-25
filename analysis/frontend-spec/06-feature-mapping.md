# 6. Feature Mapping

**Last Updated:** January 25, 2025  
**Purpose:** Document how components work together to deliver complete user-facing features

---

## Overview

This document groups components by user-facing features and documents complete user journeys. Each feature section includes:
- Components involved
- User journey flow
- Backend API dependencies
- State management patterns
- Data flow diagrams

---

## 6.1 Trip Creation & Generation

### 6.1.1 Feature Overview

**Purpose:** Allow users to create a new trip by providing destination, dates, and preferences, then watch AI agents generate a personalized itinerary in real-time.

**Entry Points:**
- Landing page "Start Trip" button
- Dashboard "Create New Trip" button

### 6.1.2 Components Involved

**Primary Components:**
- `SimplifiedTripWizard` - Multi-step form for trip details
- `SimplifiedAgentProgress` - Real-time progress tracking
- `EnhancedGenerationProgress` - Enhanced progress UI (alternative)
- `GeneratingPlan` - Legacy progress component

**Supporting Components:**
- `GoogleSignIn` - Authentication (if not logged in)
- `ProtectedRoute` - Route protection
- `LoadingState` - Loading indicators
- `ErrorDisplay` - Error handling

**Shared Components:**
- `Button`, `Input`, `Select`, `Calendar` (ui)
- `Card`, `Progress`, `Badge` (ui)

### 6.1.3 User Journey

```
1. Landing Page / Dashboard
   ↓ Click "Start Trip" / "Create New Trip"
   
2. Authentication Check
   ↓ If not authenticated → GoogleSignIn
   ↓ If authenticated → Continue
   
3. SimplifiedTripWizard
   ├── Step 1: Destination Selection
   │   - Search/select destination
   │   - Add multiple destinations (optional)
   ├── Step 2: Date Selection
   │   - Start date
   │   - End date
   │   - Flexible dates option
   ├── Step 3: Party Details
   │   - Number of travelers
   │   - Traveler types (adults, children)
   ├── Step 4: Budget Selection
   │   - Budget range
   │   - Currency
   ├── Step 5: Interests & Preferences
   │   - Activity types
   │   - Pace (relaxed, moderate, packed)
   │   - Dietary restrictions
   └── Step 6: Additional Constraints
       - Accessibility needs
       - Special requests
   ↓ Click "Create Trip"
   
4. Trip Creation API Call
   ↓ POST /itineraries
   ↓ Receive itineraryId
   
5. SimplifiedAgentProgress
   ├── WebSocket Connection
   │   - Subscribe to /topic/agent/{itineraryId}
   │   - Receive real-time updates
   ├── Progress Display
   │   - Agent status (planner, enrichment, places)
   │   - Progress percentage
   │   - Current activity message
   │   - Estimated time remaining
   ├── Smooth Progress Animation
   │   - Stages: 0-30%, 30-60%, 60-90%, 90-100%
   │   - Fallback polling if WebSocket fails
   └── Error Handling
       - Connection failures
       - Agent errors
       - Retry mechanism
   ↓ On completion (100%)
   
6. Redirect to TravelPlanner
   ↓ Load completed itinerary
   ↓ Display in day-by-day view
```


### 6.1.4 Backend API Dependencies

| API Endpoint | Method | Purpose | Request Data | Response Data |
|--------------|--------|---------|--------------|---------------|
| `/itineraries` | POST | Create new itinerary | CreateItineraryRequest | ItineraryCreationResponse |
| `/topic/agent/{id}` | WebSocket | Real-time agent updates | - | AgentProgressMessage |
| `/agents/status/{id}` | GET | Fallback polling | - | AgentStatusResponse |

**CreateItineraryRequest:**
```typescript
{
  destination: string;
  startDate: string;
  endDate: string;
  travelers: Traveler[];
  budget: BudgetRange;
  interests: string[];
  preferences: TravelPreferences;
  constraints?: string[];
}
```

**AgentProgressMessage:**
```typescript
{
  type: 'agent_progress';
  agentId: string; // 'planner', 'enrichment', 'places'
  progress: number; // 0-100
  status: 'queued' | 'running' | 'completed' | 'failed';
  message?: string;
  timestamp: string;
}
```

### 6.1.5 State Management

**Zustand Store:**
- `currentTrip` - Stores created trip data
- `trips` - Adds new trip to list
- `currentScreen` - Updates to 'generating'

**React Query:**
- `createItinerary` mutation
- Cache invalidation on completion

**Local State (SimplifiedAgentProgress):**
- `agents` - Array of agent statuses
- `actualProgress` - Real progress from backend
- `smoothProgress` - Animated progress
- `isCompleted` - Completion flag
- `hasError` - Error state
- `errorMessage` - Error details

**WebSocket State:**
- Connection status
- Reconnection attempts
- Message queue

### 6.1.6 Data Flow Diagram

```
User Input (Wizard Form)
      ↓
Form Validation
      ↓
CreateItineraryRequest
      ↓
POST /itineraries
      ↓
Backend Creates Itinerary
      ↓
Returns itineraryId
      ↓
Store in Zustand (currentTrip)
      ↓
Navigate to /generating
      ↓
SimplifiedAgentProgress Mounts
      ↓
WebSocket Connection
      ↓
Subscribe to /topic/agent/{id}
      ↓
┌─────────────────────────┐
│  Real-time Updates Loop │
│  ┌──────────────────┐   │
│  │ Agent Progress   │   │
│  │ Messages         │   │
│  └────────┬─────────┘   │
│           ↓             │
│  Update Local State     │
│  (agents, progress)     │
│           ↓             │
│  Smooth Animation       │
│  (useProgressWithStages)│
│           ↓             │
│  UI Re-render           │
│  (Progress bars, msgs)  │
│           ↓             │
│  Check Completion       │
│  (progress >= 100%)     │
└─────────┬───────────────┘
          ↓
    Completion Detected
          ↓
Invalidate React Query Cache
          ↓
Fetch Completed Itinerary
          ↓
Update Zustand (currentTrip)
          ↓
Navigate to /planner
```

### 6.1.7 Error Scenarios

**Scenario 1: WebSocket Connection Failure**
- **Detection:** Connection timeout or error event
- **Recovery:** 
  1. Show error message
  2. Attempt reconnection (exponential backoff)
  3. Fall back to polling (every 5 seconds)
  4. Continue showing progress from polling

**Scenario 2: Agent Execution Failure**
- **Detection:** Agent status = 'failed' or error message
- **Recovery:**
  1. Display error message
  2. Show retry button
  3. Allow user to retry (up to 3 attempts)
  4. Provide option to cancel and return to dashboard

**Scenario 3: Timeout (> 5 minutes)**
- **Detection:** Timer exceeds threshold
- **Recovery:**
  1. Show timeout message
  2. Offer to continue waiting
  3. Provide option to cancel
  4. Background polling continues

---

## 6.2 Itinerary Viewing & Editing

### 6.2.1 Feature Overview

**Purpose:** View and edit generated itineraries with multiple view modes (day-by-day, map, workflow, chat).

**Entry Points:**
- Dashboard "View Trip" button
- Direct URL `/planner` or `/trip/:id`

### 6.2.2 Components Involved

**Primary Components:**
- `TravelPlanner` - Main container with view switching
- `DayByDayView` - Day-by-day itinerary view
- `TripMap` - Interactive map view
- `WorkflowBuilder` - Workflow/timeline view
- `NewChat` - AI chat for modifications
- `TripOverviewView` - Summary and statistics

**Layout Components:**
- `NavigationSidebar` - View navigation
- `TopNavigation` - Actions and controls
- `ResizablePanel` - Adjustable panels
- `MobileLayout` - Mobile-optimized interface

**Supporting Components:**
- `DayCard`, `ActivityCard`, `MealCard`, `AccommodationCard`, `TransportCard`
- `MarkerInfoWindow`, `TerrainControl`
- `WorkflowNode`, `NodeInspectorModal`
- `ChatMessage`, `DisambiguationPanel`

### 6.2.3 User Journey

```
1. Dashboard / Direct Link
   ↓ Click "View Trip" or navigate to URL
   
2. Load Itinerary
   ├── Fetch from React Query cache (if available)
   └── Or fetch from API: GET /itineraries/{id}/json
   ↓
   
3. TravelPlanner Renders
   ├── UnifiedItineraryContext wraps component
   ├── WebSocket connection established
   └── Default view: Overview or Day-by-Day
   
4. View Switching
   ├── Overview Tab
   │   ├── Trip statistics
   │   ├── Weather forecast
   │   ├── Cost breakdown
   │   └── Quick actions
   │
   ├── Day-by-Day Tab
   │   ├── Expandable day cards
   │   ├── Activity timeline
   │   ├── Add/edit/delete activities
   │   └── Drag-and-drop reordering
   │
   ├── Map Tab
   │   ├── Google Maps with markers
   │   ├── Marker clustering
   │   ├── Route visualization
   │   └── Click marker for details
   │
   ├── Workflow Tab
   │   ├── Node-based visualization
   │   ├── Drag-and-drop positioning
   │   ├── Connect nodes with edges
   │   └── Click node to inspect/edit
   │
   └── Chat Tab
       ├── Message history
       ├── Natural language input
       ├── AI responses with proposals
       └── Apply/reject changes
       
5. Editing Actions
   ├── Add Activity
   │   ├── Search places
   │   ├── Select from results
   │   └── Add to specific day/time
   │
   ├── Edit Activity
   │   ├── Click activity card
   │   ├── Modify details (time, duration, notes)
   │   └── Save changes
   │
   ├── Delete Activity
   │   ├── Click delete button
   │   ├── Confirm deletion
   │   └── Remove from itinerary
   │
   ├── Reorder Activities
   │   ├── Drag activity card
   │   ├── Drop in new position
   │   └── Auto-update timing
   │
   └── Chat Modifications
       ├── Type natural language request
       ├── AI proposes changes
       ├── Preview changes
       └── Apply or reject
       
6. Save & Sync
   ├── Auto-save on changes (debounced)
   ├── Optimistic UI updates
   ├── WebSocket broadcasts to other clients
   └── Conflict resolution if needed
```

### 6.2.4 Backend API Dependencies

| API Endpoint | Method | Purpose |
|--------------|--------|---------|
| `/itineraries/{id}/json` | GET | Fetch itinerary |
| `/itineraries/{id}:propose` | POST | Propose changes |
| `/itineraries/{id}:apply` | POST | Apply changes |
| `/itineraries/{id}:undo` | POST | Undo changes |
| `/itineraries/{id}/workflow` | PUT | Save workflow positions |
| `/itineraries/{id}/nodes/{nodeId}` | PUT | Update node |
| `/itineraries/{id}/nodes/{nodeId}/lock` | PUT | Lock/unlock node |
| `/agents/process-request` | POST | Chat AI request |
| `/topic/itinerary/{id}` | WebSocket | Real-time updates |

### 6.2.5 State Management

**UnifiedItineraryContext:**
```typescript
{
  // Itinerary data
  itinerary: NormalizedItinerary | null;
  isLoading: boolean;
  error: string | null;
  
  // Selection state
  selectedDay: number;
  selectedNodeId: string | null;
  
  // Change management
  changes: ChangeOperation[];
  canUndo: boolean;
  canRedo: boolean;
  
  // Chat state
  chatMessages: ChatMessage[];
  pendingProposal: ChangeSet | null;
  
  // Workflow state
  workflowNodes: WorkflowNode[];
  workflowEdges: WorkflowEdge[];
  
  // Actions
  loadItinerary: (id: string) => void;
  applyChanges: (changes: ChangeOperation[]) => void;
  undo: () => void;
  redo: () => void;
  selectDay: (day: number) => void;
  selectNode: (nodeId: string) => void;
  sendChatMessage: (message: string) => void;
}
```

**React Query:**
- `useItinerary(id)` - Fetch and cache itinerary
- `useProposeChanges()` - Mutation for proposing changes
- `useApplyChanges()` - Mutation for applying changes
- Optimistic updates for instant UI feedback

**Map State (useMapState):**
- `selectedNodeId` - Currently selected marker
- `highlightedMarkers` - Highlighted markers
- `mapCenter` - Map center coordinates
- `mapZoom` - Zoom level
- `viewMode` - Map view mode

### 6.2.6 Real-Time Collaboration

**WebSocket Integration:**
```
User A makes change
      ↓
Optimistic UI update (User A)
      ↓
POST /itineraries/{id}:apply
      ↓
Backend processes change
      ↓
WebSocket broadcast to all clients
      ↓
User B receives update
      ↓
Update User B's UI
      ↓
Conflict detection (if simultaneous edits)
      ↓
Conflict resolution modal (if needed)
```

---

## 6.3 Day-by-Day Planning

### 6.3.1 Feature Overview

**Purpose:** Detailed day-by-day view of itinerary with timeline, activity cards, and inline editing.

**Entry Point:** TravelPlanner → Day-by-Day tab

### 6.3.2 Components Involved

**Primary:**
- `DayByDayView` - Main container
- `DayCard` - Individual day display
- `ActivityCard` - Activity display
- `MealCard` - Meal display
- `AccommodationCard` - Accommodation display
- `TransportCard` - Transport display

**Supporting:**
- `DayHeader` - Day title and summary
- `DayTimeline` - Visual timeline
- `AddActivityButton` - Add new activity
- `ActivityDetailsModal` - Edit activity details

### 6.3.3 User Journey

```
1. Select Day-by-Day View
   ↓
2. Day List Renders
   ├── Each day as expandable card
   ├── Day summary (date, activities count, cost)
   └── Expand/collapse toggle
   ↓
3. Expand Day
   ├── Timeline visualization
   ├── Activity cards in chronological order
   ├── Time gaps highlighted
   └── Add activity button
   ↓
4. View Activity Details
   ├── Click activity card
   ├── Show full details
   ├── Edit button
   └── Delete button
   ↓
5. Edit Activity
   ├── Modify time
   ├── Change duration
   ├── Update notes
   ├── Add photos
   └── Save changes
   ↓
6. Reorder Activities
   ├── Drag activity card
   ├── Drop in new position
   ├── Auto-adjust times
   └── Update timeline
```

### 6.3.4 Data Flow

```
DayByDayView
      ↓
UnifiedItineraryContext.itinerary.days[]
      ↓
Map days to DayCard components
      ↓
Each DayCard renders:
  ├── DayHeader (date, summary)
  ├── DayTimeline (visual timeline)
  └── nodes[] mapped to:
      ├── ActivityCard (if type = 'activity')
      ├── MealCard (if type = 'meal')
      ├── AccommodationCard (if type = 'accommodation')
      └── TransportCard (if type = 'transport')
      
User edits activity
      ↓
Local state update (optimistic)
      ↓
POST /itineraries/{id}:propose
      ↓
Preview changes
      ↓
User confirms
      ↓
POST /itineraries/{id}:apply
      ↓
Backend updates itinerary
      ↓
WebSocket broadcast
      ↓
All clients update
```

---

## 6.4 Map Integration

### 6.4.1 Feature Overview

**Purpose:** Interactive map showing all itinerary locations with markers, clustering, and route visualization.

**Entry Point:** TravelPlanner → Map tab

### 6.4.2 Components Involved

**Primary:**
- `TripMap` - Google Maps integration
- `MarkerInfoWindow` - Marker details popup
- `TerrainControl` - Map type control
- `MapErrorBoundary` - Error handling

**Supporting:**
- `useMapState` - Map state management
- `useGoogleMaps` - Google Maps API hook
- `mapUtils` - Map utility functions
- `googleMapsLoader` - API loader

### 6.4.3 User Journey

```
1. Select Map View
   ↓
2. Load Google Maps
   ├── Check if API loaded
   ├── Load if needed
   └── Initialize map
   ↓
3. Render Markers
   ├── Extract locations from itinerary
   ├── Create markers for each location
   ├── Apply clustering (if many markers)
   └── Fit bounds to show all markers
   ↓
4. User Interactions
   ├── Click Marker
   │   ├── Show info window
   │   ├── Display location details
   │   ├── Show associated activities
   │   └── Highlight in day-by-day view
   │
   ├── Zoom/Pan
   │   ├── Update map state
   │   ├── Adjust clustering
   │   └── Load more details if needed
   │
   ├── Change Map Type
   │   ├── Roadmap
   │   ├── Satellite
   │   ├── Terrain
   │   └── Hybrid
   │
   └── Search Location
       ├── Geocoding search
       ├── Add marker
       └── Add to itinerary
```

### 6.4.4 Map State Synchronization

```
Map View ←→ Day-by-Day View
      ↓
Bidirectional Sync:
  
  User selects day in Day-by-Day
        ↓
  Map highlights day's markers
  Map centers on day's locations
  
  User clicks marker on Map
        ↓
  Day-by-Day scrolls to activity
  Activity card highlights
```

### 6.4.5 Backend Dependencies

| API Endpoint | Purpose |
|--------------|---------|
| Google Maps JavaScript API | Map rendering |
| Google Geocoding API | Address ↔ coordinates |
| `/itineraries/{id}/json` | Location data |

---

## 6.5 Chat & AI Assistant

### 6.5.1 Feature Overview

**Purpose:** Natural language interface for modifying itineraries through conversation with AI.

**Entry Point:** TravelPlanner → Chat tab

### 6.5.2 Components Involved

**Primary:**
- `NewChat` - Main chat interface
- `ChatMessage` - Individual messages
- `DisambiguationPanel` - Clarification UI

**Supporting:**
- `ChatInput` - Message input field
- `TypingIndicator` - AI typing animation
- `ChangeProposal` - Change preview
- `ChatActions` - Apply/reject buttons

### 6.5.3 User Journey

```
1. Open Chat Tab
   ↓
2. View Message History
   ├── Load from local storage
   ├── Display previous messages
   └── Scroll to bottom
   ↓
3. User Types Message
   ├── "Add a visit to the Eiffel Tower on day 2"
   ├── "Find a restaurant near the hotel"
   ├── "Move the museum visit to the afternoon"
   └── "What's the weather like?"
   ↓
4. Send Message
   ↓
5. AI Processing
   ├── Show typing indicator
   ├── POST /agents/process-request
   ├── WebSocket receives response
   └── Parse AI response
   ↓
6. AI Response Types
   │
   ├── Information Response
   │   ├── Display text answer
   │   └── No changes proposed
   │
   ├── Change Proposal
   │   ├── Display proposed changes
   │   ├── Show preview (diff view)
   │   ├── Apply button
   │   └── Reject button
   │
   └── Disambiguation Request
       ├── Display clarification question
       ├── Show multiple choice options
       ├── User selects option
       └── Continue conversation
       
7. Apply Changes
   ├── User clicks "Apply"
   ├── POST /itineraries/{id}:apply
   ├── Update itinerary
   ├── Show success message
   └── Update all views
```

### 6.5.4 Chat Message Types

**User Message:**
```typescript
{
  role: 'user';
  content: string;
  timestamp: string;
}
```

**AI Response:**
```typescript
{
  role: 'assistant';
  content: string;
  timestamp: string;
  proposedChanges?: ChangeSet;
  disambiguationOptions?: DisambiguationOption[];
}
```

**Change Proposal:**
```typescript
{
  operations: ChangeOperation[];
  preview: {
    added: Node[];
    removed: Node[];
    modified: Node[];
  };
  explanation: string;
}
```

### 6.5.5 State Management

**Chat State:**
- `chatMessages` - Message history
- `pendingProposal` - Current change proposal
- `isTyping` - AI typing indicator
- `disambiguationContext` - Clarification context

**Persistence:**
- Local storage for message history
- Session-based (cleared on logout)

---

## 6.6 Booking System

### 6.6.1 Feature Overview

**Purpose:** Book hotels, activities, and transportation with integrated payment processing.

**Entry Points:**
- Activity card "Book" button
- Accommodation card "Book" button
- Transport card "Book" button

### 6.6.2 Components Involved

**Primary:**
- `BookingModal` - Main booking interface
- `Checkout` - Payment flow
- `BookingConfirmation` - Success page
- `CostAndCart` - Shopping cart

**Supporting:**
- `HotelBookingSystem` - Hotel-specific booking
- `BookingErrorDisplay` - Error handling
- `BookedNodeIndicator` - Booking status badge

### 6.6.3 User Journey

```
1. Click "Book" on Activity/Hotel/Transport
   ↓
2. BookingModal Opens
   ├── Display item details
   ├── Show pricing
   ├── Check availability
   └── Provider options (if multiple)
   ↓
3. Select Options
   ├── Date/time (if flexible)
   ├── Quantity (tickets, rooms)
   ├── Add-ons (optional)
   └── Special requests
   ↓
4. Add to Cart
   ├── Update CostAndCart
   ├── Calculate total
   └── Continue shopping or checkout
   ↓
5. Proceed to Checkout
   ├── Review cart items
   ├── Apply promo code (optional)
   └── Click "Checkout"
   ↓
6. Payment Flow (Razorpay)
   ├── Create payment order
   │   └── POST /payments/razorpay/order
   ├── Open Razorpay modal
   ├── User enters payment details
   ├── Process payment
   └── Receive payment confirmation
   ↓
7. Confirm Booking
   ├── POST /providers/{vertical}/{provider}:book
   ├── Backend processes booking
   ├── Receive booking confirmation
   └── Send confirmation email
   ↓
8. BookingConfirmation Page
   ├── Display booking reference
   ├── Show booking details
   ├── Download confirmation PDF
   ├── Add to calendar
   └── Share booking
```

### 6.6.4 Backend Dependencies

| API Endpoint | Purpose |
|--------------|---------|
| `/payments/razorpay/order` | Create payment order |
| `/providers/{vertical}/{provider}:book` | Book service |
| `/bookings/{id}` | Get booking details |
| `/bookings/{id}/cancel` | Cancel booking |

### 6.6.5 Payment Flow

```
User clicks "Pay Now"
      ↓
Create Razorpay Order
  POST /payments/razorpay/order
  {
    amount: number;
    currency: string;
    items: BookingItem[];
  }
      ↓
Receive Order ID
      ↓
Open Razorpay Checkout Modal
  (Third-party Razorpay UI)
      ↓
User Completes Payment
      ↓
Razorpay Callback
  (Success/Failure)
      ↓
If Success:
  ├── Verify payment signature
  ├── POST /providers/{vertical}/{provider}:book
  ├── Create booking records
  ├── Send confirmation email
  └── Navigate to confirmation page
      
If Failure:
  ├── Show error message
  ├── Offer to retry
  └── Keep items in cart
```

---

## 6.7 Sharing & Export

### 6.7.1 Feature Overview

**Purpose:** Share itineraries publicly or via email, export to PDF.

**Entry Points:**
- TravelPlanner "Share" button
- TravelPlanner "Export PDF" button

### 6.7.2 Components Involved

**Primary:**
- `ShareModal` - Share options dialog
- `ShareView` - Public share page
- `EmailShareForm` - Email sharing form
- `PdfExportButton` - PDF export trigger
- `ExportOptionsModal` - Export configuration

### 6.7.3 User Journey - Public Sharing

```
1. Click "Share" Button
   ↓
2. ShareModal Opens
   ├── Generate share link option
   ├── Email share option
   └── Social media share option
   ↓
3. Generate Share Link
   ├── POST /itineraries/{id}:share
   ├── Backend creates public token
   ├── Returns share URL
   └── Display URL with copy button
   ↓
4. Share Link Usage
   ├── Anyone with link can view
   ├── Navigate to /share/{token}
   ├── ShareView component renders
   ├── Read-only itinerary display
   └── No editing allowed
```

### 6.7.4 User Journey - Email Sharing

```
1. Click "Share via Email"
   ↓
2. EmailShareForm Opens
   ├── Recipient email input
   ├── Personal message (optional)
   └── Include PDF attachment option
   ↓
3. Send Email
   ├── POST /email/send
   ├── Backend sends email
   ├── Include share link
   ├── Attach PDF (if selected)
   └── Show success message
```

### 6.7.5 User Journey - PDF Export

```
1. Click "Export PDF"
   ↓
2. ExportOptionsModal Opens
   ├── Select pages to include
   │   ├── Overview
   │   ├── Day-by-day details
   │   ├── Map
   │   └── Booking confirmations
   ├── Layout options
   │   ├── Portrait/Landscape
   │   └── Page size
   └── Branding options
       ├── Include logo
       └── Custom header/footer
   ↓
3. Generate PDF
   ├── GET /itineraries/{id}/pdf
   ├── Backend generates PDF
   ├── Stream PDF to browser
   └── Browser download dialog
   ↓
4. PDF Downloaded
   ├── Save to device
   ├── Print option
   └── Share PDF file
```

### 6.7.6 Backend Dependencies

| API Endpoint | Purpose |
|--------------|---------|
| `/itineraries/{id}:share` | Generate share token |
| `/itineraries/{id}/pdf` | Generate PDF |
| `/email/send` | Send email |
| `/share/{token}` | Access shared itinerary |

---

**[← Back to Main Specification](../FRONTEND_UI_REDESIGN_SPECIFICATION.md)** | **[Next: Data Models & Types →](07-data-models-types.md)**
