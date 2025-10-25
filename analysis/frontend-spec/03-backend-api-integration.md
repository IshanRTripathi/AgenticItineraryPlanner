# 3. Backend API Integration Inventory

**Last Updated:** January 25, 2025  
**Sources:** `frontend/src/services/apiClient.ts`, `src/main/resources/swagger-api-documentation.yaml`

---

## 3.1 REST API Endpoints

This section documents all REST API endpoints consumed by the frontend application, including request/response schemas, authentication requirements, and which components use each endpoint.

### 3.1.1 Itinerary Management Endpoints

| Endpoint | Method | Request Schema | Response Schema | Used By Components | Auth Required |
|----------|--------|----------------|-----------------|-------------------|---------------|
| `/itineraries` | POST | CreateItineraryRequest | ItineraryCreationResponse | SimplifiedTripWizard | Yes |
| `/itineraries` | GET | - | ItineraryResponse[] | TripDashboard, ItineraryOverview | Yes |
| `/itineraries/{id}` | GET | - | ItineraryResponse | TripViewLoader | Yes |
| `/itineraries/{id}/json` | GET | - | NormalizedItinerary | TravelPlanner, useItinerary hook | Yes |
| `/itineraries/{id}/public` | GET | - | ItineraryResponse | ShareView (public access) | No |
| `/itineraries/{id}` | DELETE | - | void (204) | TripDashboard, ItineraryOverview | Yes |
| `/itineraries/{id}:save` | POST | - | void | TravelPlanner (auto-save) | Yes |
| `/itineraries/{id}:share` | POST | - | ShareResponse | ShareModal | Yes |
| `/itineraries/{id}:revise` | POST | ReviseRequest | ReviseResponse | (Legacy - not actively used) | Yes |
| `/itineraries/{id}:extend` | POST | ExtendRequest | ItineraryResponse | (Legacy - not actively used) | Yes |

**CreateItineraryRequest Schema:**
```typescript
interface CreateItineraryRequest {
  destination: string;           // e.g., "Barcelona, Spain"
  startLocation: string;          // Starting point for the trip
  startDate: string;              // ISO date format: "2025-06-01"
  endDate: string;                // ISO date format: "2025-06-04"
  party: {
    adults: number;               // Minimum: 1
    children: number;             // Default: 0
    infants: number;              // Default: 0
    rooms: number;                // Default: 1
  };
  budgetTier: string;             // "economy" | "mid-range" | "luxury"
  interests: string[];            // e.g., ["culture", "architecture", "food"]
  constraints: string[];          // e.g., ["familyFriendly", "wheelchairAccessible"]
  language: string;               // ISO language code, default: "en"
}
```

**ItineraryCreationResponse Schema:**
```typescript
interface ItineraryCreationResponse {
  itinerary: ItineraryResponse;   // Basic itinerary info
  executionId?: string;           // Agent execution ID for tracking
  sseEndpoint?: string;           // SSE endpoint URL for progress tracking
  estimatedCompletion?: string;   // Estimated completion time
  status?: 'generating' | 'complete' | 'failed';
  stages?: Array<{
    name: string;
    status: string;
    progress: number;
  }>;
  errorMessage?: string;
}
```

**NormalizedItinerary Schema:**
```typescript
interface NormalizedItinerary {
  id: string;
  destination: string;
  startDate: string;
  endDate: string;
  party: PartyDto;
  budgetTier: string;
  interests: string[];
  constraints: string[];
  language: string;
  days: NormalizedDay[];
  settings?: ItinerarySettings;
  agentStatus?: AgentStatus;
  metadata?: Record<string, any>;
}
```

**ShareResponse Schema:**
```typescript
interface ShareResponse {
  shareToken: string;             // Token for public access
  publicUrl: string;              // Full public URL
}
```

---

### 3.1.2 Change Management Endpoints (Normalized JSON API)

| Endpoint | Method | Request Schema | Response Schema | Used By Components | Auth Required |
|----------|--------|----------------|-----------------|-------------------|---------------|
| `/itineraries/{id}:propose` | POST | ChangeSet | ProposeResponse | TravelPlanner, Chat components | Yes |
| `/itineraries/{id}:apply` | POST | ApplyRequest | ApplyResponse | TravelPlanner, Chat components | Yes |
| `/itineraries/{id}:undo` | POST | UndoRequest | UndoResponse | UndoRedoControls, TravelPlanner | Yes |
| `/itineraries/{id}:redo` | POST | - | UndoResponse | UndoRedoControls, TravelPlanner | Yes |

**ChangeSet Schema:**
```typescript
interface ChangeSet {
  operations: ChangeOperation[];
  description?: string;
  source?: 'user' | 'agent' | 'system';
}

interface ChangeOperation {
  op: 'add' | 'remove' | 'update' | 'move';
  path: string;                   // JSON Pointer path (e.g., "/days/0/nodes/1")
  value?: any;                    // New value for add/update operations
  from?: string;                  // Source path for move operations
}
```

**ProposeResponse Schema:**
```typescript
interface ProposeResponse {
  proposalId: string;
  changes: ChangeSet;
  preview: NormalizedItinerary;   // Preview of itinerary after changes
  conflicts?: string[];           // Any conflicts detected
  warnings?: string[];            // Warnings about the changes
}
```

**ApplyRequest Schema:**
```typescript
interface ApplyRequest {
  proposalId?: string;            // Optional proposal ID from propose endpoint
  changes: ChangeSet;             // Changes to apply
  force?: boolean;                // Force apply even if conflicts exist
}
```

**ApplyResponse Schema:**
```typescript
interface ApplyResponse {
  success: boolean;
  itinerary: NormalizedItinerary; // Updated itinerary
  appliedChanges: ChangeSet;      // Changes that were applied
  revisionId?: string;            // New revision ID
}
```

---

### 3.1.3 Revision History Endpoints

| Endpoint | Method | Request Schema | Response Schema | Used By Components | Auth Required |
|----------|--------|----------------|-----------------|-------------------|---------------|
| `/itineraries/{id}/revisions` | GET | - | { revisions: Revision[] } | RevisionHistoryButton, RevisionTimeline | Yes |
| `/itineraries/{id}/revisions/{revisionId}` | GET | - | RevisionDetail | RevisionDiffViewer | Yes |
| `/itineraries/{id}/revisions/{version}/rollback` | POST | - | NormalizedItinerary | RevisionCard, RevisionTimeline | Yes |

**Revision Schema:**
```typescript
interface Revision {
  id: string;
  version: number;
  timestamp: string;
  description: string;
  author?: string;
  changeCount: number;
  tags?: string[];
}
```

---

### 3.1.4 Workflow Sync Endpoints

| Endpoint | Method | Request Schema | Response Schema | Used By Components | Auth Required |
|----------|--------|----------------|-----------------|-------------------|---------------|
| `/itineraries/{id}/workflow` | PUT | { positions: NodePosition[] } | void | WorkflowBuilder, useWorkflowSync hook | Yes |
| `/itineraries/{id}/nodes/{nodeId}` | PUT | NodeData | void | WorkflowBuilder, NodeInspectorModal | Yes |

**NodePosition Schema:**
```typescript
interface NodePosition {
  nodeId: string;
  x: number;
  y: number;
}
```

---

### 3.1.5 Lock Management Endpoints

| Endpoint | Method | Request Schema | Response Schema | Used By Components | Auth Required |
|----------|--------|----------------|-----------------|-------------------|---------------|
| `/itineraries/{id}/nodes/{nodeId}/lock` | PUT | { locked: boolean } | { success: boolean; message?: string } | NodeLockToggle, LockedNodeIndicator | Yes |
| `/itineraries/{id}/lock-states` | GET | - | Record<string, boolean> | TravelPlanner, WorkflowBuilder | Yes |

---

### 3.1.6 Agent Endpoints

| Endpoint | Method | Request Schema | Response Schema | Used By Components | Auth Required |
|----------|--------|----------------|-----------------|-------------------|---------------|
| `/agents/process-request` | POST | ProcessRequestRequest | ProcessRequestResponse | NewChat, ChatMessage | Yes |
| `/agents/apply-with-enrichment` | POST | ApplyWithEnrichmentRequest | ApplyWithEnrichmentResponse | Chat components | Yes |
| `/agents/{itineraryId}/status` | GET | - | AgentStatusResponse | (Polling alternative to SSE) | Yes |

**ProcessRequestRequest Schema:**
```typescript
interface ProcessRequestRequest {
  itineraryId: string;
  userMessage: string;
  context?: {
    selectedNodes?: string[];
    currentDay?: number;
    focusArea?: string;
  };
  conversationHistory?: ChatMessage[];
}
```

**ProcessRequestResponse Schema:**
```typescript
interface ProcessRequestResponse {
  responseMessage: string;
  proposedChanges?: ChangeSet;
  disambiguationOptions?: DisambiguationOption[];
  requiresConfirmation: boolean;
  executionId?: string;
}
```

---

### 3.1.7 Tools Endpoints

| Endpoint | Method | Request Schema | Response Schema | Used By Components | Auth Required |
|----------|--------|----------------|-----------------|-------------------|---------------|
| `/packing-list` | POST | PackingListRequest | PackingListResponse | (Future feature) | Yes |
| `/photo-spots` | POST | PhotoSpotsRequest | PhotoSpotsResponse | (Future feature) | Yes |
| `/must-try-foods` | POST | MustTryFoodsRequest | MustTryFoodsResponse | (Future feature) | Yes |
| `/cost-estimator` | POST | CostEstimateRequest | CostEstimateResponse | CostAndCart, Checkout | Yes |
| `/tools/weather` | GET | Query params: destination, date | WeatherRes | (Future feature) | No |

**CostEstimateRequest Schema:**
```typescript
interface CostEstimateRequest {
  destination: string;
  startDate: string;
  endDate: string;
  budgetTier: string;
  partySize: number;
  interests: string[];
}
```

**CostEstimateResponse Schema:**
```typescript
interface CostEstimateResponse {
  currency: string;
  totals: CostBreakdown;
  perDay: CostBreakdown;
  perPerson: CostBreakdown;
  confidence: string;
  notes: string;
}

interface CostBreakdown {
  transport: number;
  lodging: number;
  food: number;
  activities: number;
  shopping: number;
  misc: number;
  total: number;
}
```

---

### 3.1.8 Booking & Payment Endpoints

| Endpoint | Method | Request Schema | Response Schema | Used By Components | Auth Required |
|----------|--------|----------------|-----------------|-------------------|---------------|
| `/payments/razorpay/order` | POST | RazorpayOrderReq | RazorpayOrderRes | Checkout, BookingModal | Yes |
| `/payments/razorpay/webhook` | POST | Raw webhook payload | void | (Backend webhook handler) | No |
| `/providers/{vertical}/{provider}:book` | POST | ProviderBookReq | BookingRes | Checkout, HotelBookingSystem | Yes |
| `/bookings` | GET | Query params: page, size | BookingRes[] | (Future: Booking history) | Yes |
| `/bookings/{bookingId}` | GET | - | BookingRes | BookingConfirmation | Yes |
| `/bookings/{bookingId}:cancel` | POST | CancelBookingReq | void | BookingCancellation | Yes |
| `/book` | POST | MockBookingRequest | MockBookingResponse | (Mock booking for testing) | Yes |

**RazorpayOrderReq Schema:**
```typescript
interface RazorpayOrderReq {
  itemType: string;               // "hotel" | "flight" | "activity"
  itineraryId: string;
  amount: number;                 // Amount in paise (smallest currency unit)
  currency: string;               // "INR"
  meta?: Record<string, any>;
}
```

**ProviderBookReq Schema:**
```typescript
interface ProviderBookReq {
  payment: {
    orderId: string;              // Razorpay order ID
    paymentId: string;            // Razorpay payment ID
    signature: string;            // Payment signature
  };
  item: {
    token: string;                // Item token
    details?: Record<string, any>;
  };
  itineraryId: string;
}
```

---

### 3.1.9 Export Endpoints

| Endpoint | Method | Request Schema | Response Schema | Used By Components | Auth Required |
|----------|--------|----------------|-----------------|-------------------|---------------|
| `/itineraries/{id}/pdf` | GET | - | Blob (PDF file) | PdfExportButton, ExportOptionsModal | Yes |
| `/email/send` | POST | EmailRequest | EmailResponse | EmailShareForm, ShareModal | Yes |

**EmailRequest Schema:**
```typescript
interface EmailRequest {
  to: string;                     // Recipient email
  subject: string;
  itineraryId: string;
  template?: string;
  personalMessage?: string;
  includePdf: boolean;
  templateData?: Record<string, any>;
}
```

---

### 3.1.10 Authentication Endpoints

| Endpoint | Method | Request Schema | Response Schema | Used By Components | Auth Required |
|----------|--------|----------------|-----------------|-------------------|---------------|
| `/auth/google` | POST | { idToken: string } | AuthResponse | GoogleSignIn, AuthContext | No |
| `/auth/me` | GET | - | UserInfo | AuthContext, UserProfile | Yes |

**AuthResponse Schema:**
```typescript
interface AuthResponse {
  session: string;
  user: UserInfo;
  expiresAt: string;
}

interface UserInfo {
  id: string;
  email: string;
  name: string;
  pictureUrl?: string;
}
```

---

### 3.1.11 Test & Health Endpoints

| Endpoint | Method | Request Schema | Response Schema | Used By Components | Auth Required |
|----------|--------|----------------|-----------------|-------------------|---------------|
| `/health` | GET | - | { status: string; timestamp: string; service: string; version: string } | (Health monitoring) | No |
| `/test` | GET | - | { message: string; timestamp: string } | (API connectivity test) | No |
| `/ping` | GET | - | { message: string; timestamp: string; status: string } | apiClient.ping() | No |
| `/echo` | POST | any | { echo: any; timestamp: string; message: string } | apiClient.echo() | No |

---

## 3.2 WebSocket Real-time Communication

The frontend uses WebSocket (STOMP protocol) for all real-time updates from the backend. WebSocket connections are managed by `webSocketService.ts`.

### 3.2.1 WebSocket Connection

**Endpoint:** `ws://localhost:8080/ws` (development) or `wss://api.domain.com/ws` (production)

**Protocol:** STOMP over SockJS

**Authentication:** Bearer token in STOMP CONNECT frame headers

**Connection Management:**
- Automatic reconnection with exponential backoff
- Connection deduplication (prevents multiple connections)
- Heartbeat support (30 second intervals)
- Connection cleanup on component unmount
- Managed by `webSocketService` singleton

### 3.2.2 WebSocket Topics

The frontend subscribes to these topics for real-time updates:

| Topic | Purpose | Message Types | Handled By |
|-------|---------|---------------|------------|
| `/topic/itinerary/{id}` | Itinerary updates | itinerary_updated, generation_complete, day_completed, phase_transition | UnifiedItineraryContext, SimplifiedAgentProgress |
| `/topic/agent/{id}` | Agent progress | agent_progress | SimplifiedAgentProgress, AgentProgressBar |
| `/topic/chat/{id}` | Chat responses | chat_response | Chat components |

### 3.2.3 Message Formats

**Agent Progress Message:**
```typescript
interface AgentProgressMessage {
  type: 'agent_progress';
  agentId: string;
  progress: number;              // 0-100
  data: {
    status: string;              // "running" | "completed" | "failed"
    message?: string;
  };
  timestamp: string;
}
```

**Itinerary Update Message:**
```typescript
interface ItineraryUpdateMessage {
  type: 'itinerary_updated' | 'generation_complete' | 'day_completed';
  data: {
    itineraryId: string;
    progress?: number;
    message?: string;
    itinerary?: NormalizedItinerary;
    day?: NormalizedDay;
  };
  timestamp: string;
}
```

**Chat Response Message:**
```typescript
interface ChatResponseMessage {
  type: 'chat_response';
  data: {
    message: string;
    proposedChanges?: ChangeSet;
  };
  timestamp: string;
}
```

### 3.2.4 Used By Components

**SimplifiedAgentProgress:**
- Subscribes to `/topic/agent/{id}` and `/topic/itinerary/{id}`
- Handles agent progress updates
- Handles generation complete events
- Updates progress bar and status messages

**UnifiedItineraryContext:**
- Subscribes to `/topic/itinerary/{id}`
- Applies real-time itinerary updates to local state
- Handles patch events
- Manages WebSocket connection lifecycle

**Chat Components:**
- Subscribe to `/topic/chat/{id}`
- Receive AI responses
- Handle proposed changes

### 3.2.5 Connection Lifecycle

```typescript
// Connect
await webSocketService.connect(itineraryId);

// Subscribe to messages
webSocketService.on('message', handleMessage);
webSocketService.on('agent_progress', handleAgentProgress);
webSocketService.on('generation_complete', handleComplete);

// Send messages (for chat)
webSocketService.sendChatMessage(message, context);

// Cleanup
webSocketService.off('message', handleMessage);
// Don't disconnect - shared connection
```

### 3.2.6 Error Handling

**Connection Errors:**
- Automatic reconnection with exponential backoff
- Max reconnection attempts: 10
- Reconnection interval: 3 seconds (doubles on each failure)
- Connection timeout: 15 seconds

**Message Errors:**
- Failed messages logged but don't break connection
- Error events emitted for handling by components
- Graceful degradation (polling fallback available)

---

## 3.3 Authentication & Authorization

### 3.3.1 Authentication Flow

```
1. User clicks "Sign in with Google"
   ↓
2. GoogleSignIn component triggers Firebase Auth
   ↓
3. Firebase returns ID token
   ↓
4. Frontend sends ID token to /auth/google
   ↓
5. Backend validates token and returns session
   ↓
6. AuthContext stores user info and token
   ↓
7. apiClient.setAuthToken(token) called
   ↓
8. All subsequent API calls include Bearer token
```

### 3.3.2 Token Management

**Token Storage:**
- JWT token stored in apiClient instance (in-memory)
- Token automatically included in all API requests via Authorization header
- Token passed as query parameter for SSE connections (EventSource doesn't support headers)

**Token Refresh:**
- Proactive refresh: Token refreshed 5 minutes before expiry
- Reactive refresh: On 401 error, token is refreshed and request retried
- Token expiry checked before each request by decoding JWT payload

**Token Refresh Logic (from apiClient.ts):**
```typescript
private async ensureValidToken(): Promise<boolean> {
  if (!this.authToken) return false;
  
  // Decode JWT to check expiry
  const payload = JSON.parse(atob(this.authToken.split('.')[1]));
  const expiryTime = payload.exp * 1000;
  const now = Date.now();
  const fiveMinutes = 5 * 60 * 1000;
  
  // Refresh if expiring within 5 minutes
  if (expiryTime - now < fiveMinutes) {
    return await this.refreshAuthToken();
  }
  
  return true;
}
```

**401 Error Handling:**
- On 401 response, apiClient attempts token refresh
- If refresh succeeds, request is retried with new token
- If refresh fails after max retries, user must re-authenticate
- Token is NOT cleared on 401 to avoid forcing immediate re-auth

### 3.3.3 Protected Routes

**Frontend Route Protection:**
- ProtectedRoute component wraps authenticated routes
- Checks AuthContext.isAuthenticated
- Redirects to /login if not authenticated

**Backend Endpoint Protection:**
- Most endpoints require Bearer token authentication
- Public endpoints: /health, /test, /ping, /echo, /itineraries/{id}/public, /tools/weather
- SSE endpoints accept token via query parameter

---

## 3.4 API-Component Mapping

### 3.4.1 Component → API Endpoints

| Component | API Endpoints Used | SSE Connections |
|-----------|-------------------|-----------------|
| SimplifiedTripWizard | POST /itineraries | - |
| SimplifiedAgentProgress | - | /agents/events/{id} |
| TravelPlanner | GET /itineraries/{id}/json, POST :propose, POST :apply, POST :undo, POST :redo, PUT /workflow, PUT /nodes/{id}/lock | /itineraries/patches |
| TripDashboard | GET /itineraries, DELETE /itineraries/{id} | - |
| WorkflowBuilder | GET /itineraries/{id}/json, PUT /workflow, PUT /nodes/{id} | - |
| NewChat | POST /agents/process-request, POST /agents/apply-with-enrichment | - |
| ShareModal | POST /itineraries/{id}:share, POST /email/send | - |
| PdfExportButton | GET /itineraries/{id}/pdf | - |
| Checkout | POST /payments/razorpay/order, POST /providers/{vertical}/{provider}:book | - |
| BookingConfirmation | GET /bookings/{id} | - |
| BookingCancellation | POST /bookings/{id}:cancel | - |
| RevisionHistoryButton | GET /itineraries/{id}/revisions | - |
| RevisionTimeline | GET /itineraries/{id}/revisions, POST /revisions/{version}/rollback | - |
| GoogleSignIn | POST /auth/google | - |
| UserProfile | GET /auth/me | - |

### 3.4.2 API Endpoint → Components

| API Endpoint | Components Using It |
|--------------|---------------------|
| POST /itineraries | SimplifiedTripWizard |
| GET /itineraries | TripDashboard, ItineraryOverview |
| GET /itineraries/{id}/json | TravelPlanner, WorkflowBuilder, useItinerary hook |
| DELETE /itineraries/{id} | TripDashboard, ItineraryOverview |
| POST /itineraries/{id}:propose | TravelPlanner, Chat components |
| POST /itineraries/{id}:apply | TravelPlanner, Chat components |
| POST /itineraries/{id}:undo | UndoRedoControls, TravelPlanner |
| POST /itineraries/{id}:redo | UndoRedoControls, TravelPlanner |
| POST /itineraries/{id}:share | ShareModal |
| GET /itineraries/{id}/revisions | RevisionHistoryButton, RevisionTimeline |
| POST /revisions/{version}/rollback | RevisionCard, RevisionTimeline |
| PUT /itineraries/{id}/workflow | WorkflowBuilder, useWorkflowSync hook |
| PUT /itineraries/{id}/nodes/{nodeId} | WorkflowBuilder, NodeInspectorModal |
| PUT /itineraries/{id}/nodes/{nodeId}/lock | NodeLockToggle, LockedNodeIndicator |
| POST /agents/process-request | NewChat, ChatMessage |
| POST /agents/apply-with-enrichment | Chat components |
| POST /payments/razorpay/order | Checkout, BookingModal |
| POST /providers/{vertical}/{provider}:book | Checkout, HotelBookingSystem |
| GET /itineraries/{id}/pdf | PdfExportButton, ExportOptionsModal |
| POST /email/send | EmailShareForm, ShareModal |
| POST /auth/google | GoogleSignIn, AuthContext |
| GET /auth/me | AuthContext, UserProfile |
| SSE /agents/events/{id} | SimplifiedAgentProgress, EnhancedGenerationProgress, AgentProgressBar |
| SSE /itineraries/patches | UnifiedItineraryContext, TravelPlanner, WorkflowBuilder |

---

## 3.5 API Client Features

### 3.5.1 Retry Logic

**Configuration:**
- Max retries: 3 (configurable per request)
- Retry delay: 1000ms base with exponential backoff (1s, 2s, 4s)
- Retries on: 5xx errors, 408 (timeout), 429 (rate limit), network errors
- No retry on: 4xx errors (except 401, 408, 429)

**401 Handling:**
- Special case: Token refresh attempted on 401
- Request retried with new token if refresh succeeds
- No retry if refresh fails

### 3.5.2 Request Deduplication

**Purpose:** Prevent duplicate requests for the same endpoint

**Implementation:**
- Request key: `${method}:${endpoint}`
- Pending requests stored in Map
- Subsequent requests for same key return existing promise
- Request removed from Map after completion

### 3.5.3 Timeout Handling

**Configuration:**
- Request timeout: 150 seconds (matches backend timeout)
- Uses AbortController for timeout enforcement
- Timeout errors are retried with exponential backoff

### 3.5.4 Error Handling

**Error Response Format:**
```typescript
interface ApiError {
  code: string;
  message: string;
  hint?: string;
  timestamp: string;
  path: string;
  details?: Record<string, string>;
}
```

**Error Logging:**
- All errors logged via logger utility
- Includes: component, action, error message, status code
- Structured logging for easy debugging

---

## 3.6 Unused/Future Endpoints

The following endpoints are defined in the swagger documentation but not currently used by the frontend:

| Endpoint | Status | Notes |
|----------|--------|-------|
| POST /itineraries/{id}:revise | Legacy | Replaced by :propose/:apply pattern |
| POST /itineraries/{id}:extend | Legacy | Replaced by :propose/:apply pattern |
| GET /agents/{itineraryId}/status | Alternative | Polling alternative to SSE (not used) |
| POST /packing-list | Future | Tool not yet implemented in UI |
| POST /photo-spots | Future | Tool not yet implemented in UI |
| POST /must-try-foods | Future | Tool not yet implemented in UI |
| GET /tools/weather | Future | Weather feature not yet implemented |
| GET /bookings | Future | Booking history not yet implemented |
| POST /payments/razorpay/webhook | Backend | Webhook handler (not called by frontend) |

---

**[← Back to Main Specification](../FRONTEND_UI_REDESIGN_SPECIFICATION.md)** | **[Next: Frontend Architecture →](04-frontend-architecture.md)**

