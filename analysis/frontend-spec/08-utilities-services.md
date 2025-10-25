# 8. Shared Utilities & Services

**Last Updated:** January 25, 2025  
**Purpose:** Document all API services, custom hooks, utility functions, and configuration

---

## 8.1 API Services

### 8.1.1 apiClient

**File:** `frontend/src/services/apiClient.ts`

**Purpose:** Centralized HTTP client with retry logic, token management, and error handling.

**Key Features:**
- Automatic JWT token refresh (5 minutes before expiry)
- Retry logic with exponential backoff (max 3 retries)
- Request deduplication
- Bearer token authentication
- Comprehensive error handling

**Core Methods:**

| Method | Endpoint | Purpose |
|--------|----------|---------|
| `createItinerary(request)` | POST /itineraries | Create new itinerary |
| `getItinerary(id)` | GET /itineraries/{id}/json | Fetch itinerary |
| `proposeChanges(id, changeSet)` | POST /itineraries/{id}:propose | Propose changes |
| `applyChanges(id, changeSet)` | POST /itineraries/{id}:apply | Apply changes |
| `undoChanges(id, toVersion)` | POST /itineraries/{id}:undo | Undo to version |
| `deleteItinerary(id)` | DELETE /itineraries/{id} | Delete itinerary |
| `shareItinerary(id)` | POST /itineraries/{id}:share | Generate share link |
| `exportPdf(id)` | GET /itineraries/{id}/pdf | Export to PDF |
| `sendEmail(request)` | POST /email/send | Send email |
| `processAgentRequest(request)` | POST /agents/process-request | Chat AI request |
| `createPaymentOrder(request)` | POST /payments/razorpay/order | Create payment |
| `bookService(vertical, provider, request)` | POST /providers/{vertical}/{provider}:book | Book service |

**Token Management:**
```typescript
// Proactive token refresh
if (tokenExpiresIn < 5 * 60 * 1000) { // 5 minutes
  await refreshToken();
}

// Automatic retry on 401
if (response.status === 401) {
  await refreshToken();
  return retry(request);
}
```

**Retry Logic:**
```typescript
const maxRetries = 3;
const retryDelay = (attempt) => Math.min(1000 * Math.pow(2, attempt), 10000);

// Retryable errors: network errors, 5xx, 429
if (shouldRetry(error) && attempt < maxRetries) {
  await delay(retryDelay(attempt));
  return retry(request);
}
```

### 8.1.2 websocket

**File:** `frontend/src/services/websocket.ts`

**Purpose:** WebSocket service using STOMP protocol over SockJS for real-time updates.

**Key Features:**
- STOMP protocol over SockJS
- Automatic reconnection with exponential backoff
- Connection deduplication (single connection per app)
- Message routing to multiple subscribers
- Graceful error handling

**Topics:**
- `/topic/itinerary/{id}` - Itinerary updates
- `/topic/agent/{id}` - Agent progress
- `/topic/chat/{id}` - Chat responses

**Usage:**
```typescript
// Subscribe to topic
const subscription = webSocketService.subscribe(
  `/topic/agent/${itineraryId}`,
  (message) => {
    const data = JSON.parse(message.body);
    handleAgentProgress(data);
  }
);

// Unsubscribe
subscription.unsubscribe();

// Disconnect
webSocketService.disconnect();
```

### 8.1.3 authService

**File:** `frontend/src/services/authService.ts`

**Purpose:** Firebase authentication wrapper.

**Methods:**
- `signInWithGoogle()` - Google OAuth sign-in
- `signOut()` - Sign out user
- `getIdToken()` - Get current ID token
- `refreshToken()` - Refresh ID token
- `onAuthStateChanged(callback)` - Listen to auth changes

### 8.1.4 Other Services

| Service | File | Purpose |
|---------|------|---------|
| **chatService** | `chatService.ts` | Chat message handling |
| **chatStorageService** | `chatStorageService.ts` | Local storage for chat history |
| **agentService** | `agentService.ts` | Agent execution tracking |
| **geocodingService** | `geocodingService.ts` | Google Maps geocoding |
| **weatherService** | `weatherService.ts` | Weather data fetching |
| **workflowSyncService** | `workflowSyncService.ts` | Workflow position sync |
| **userChangeTracker** | `userChangeTracker.ts` | User change analytics |
| **firebaseService** | `firebaseService.ts` | Firebase initialization |

---

## 8.2 Custom Hooks

**Directory:** `frontend/src/hooks/`

### 8.2.1 Data Fetching Hooks

#### useItinerary
**File:** `frontend/src/state/query/hooks.ts`

**Purpose:** Fetch and cache itinerary data with React Query.

```typescript
const { data, isLoading, error, refetch } = useItinerary(itineraryId);
```

**Features:**
- Automatic caching
- Background refetching
- Optimistic updates
- Error handling

#### useGenerationStatus
**File:** `frontend/src/hooks/useGenerationStatus.ts`

**Purpose:** Poll agent generation status as fallback when WebSocket fails.

```typescript
const { status, isPolling } = useGenerationStatus(
  itineraryId,
  initialStatus,
  {
    pollingInterval: 5000,
    enabled: !isCompleted,
    onComplete: () => { /* ... */ }
  }
);
```

### 8.2.2 State Management Hooks

#### useNormalizedItinerary
**File:** `frontend/src/hooks/useNormalizedItinerary.ts`

**Purpose:** Manage normalized itinerary state with change tracking.

```typescript
const {
  itinerary,
  selectedDay,
  selectedNode,
  selectDay,
  selectNode,
  applyChanges,
  undo,
  redo,
  canUndo,
  canRedo
} = useNormalizedItinerary(itineraryId);
```

#### useMapState
**File:** `frontend/src/hooks/useMapState.ts`

**Purpose:** Manage map state (center, zoom, selected markers).

```typescript
const {
  mapCenter,
  mapZoom,
  selectedNodeId,
  highlightedMarkers,
  setMapCenter,
  setMapZoom,
  setSelectedNode,
  addHighlightedMarker,
  clearHighlightedMarkers
} = useMapState();
```

### 8.2.3 UI Interaction Hooks

#### useDebounce
**File:** `frontend/src/hooks/useDebounce.ts`

**Purpose:** Debounce value updates.

```typescript
const debouncedValue = useDebounce(value, 500);
```

#### useKeyboardShortcut
**File:** `frontend/src/hooks/useKeyboardShortcut.ts`

**Purpose:** Register keyboard shortcuts.

```typescript
useKeyboardShortcut('ctrl+s', () => {
  saveItinerary();
});
```

#### useSwipeGesture
**File:** `frontend/src/hooks/useSwipeGesture.ts`

**Purpose:** Detect swipe gestures on mobile.

```typescript
const { onTouchStart, onTouchMove, onTouchEnd } = useSwipeGesture({
  onSwipeLeft: () => { /* next */ },
  onSwipeRight: () => { /* previous */ }
});
```

### 8.2.4 Performance Hooks

#### useSmoothProgress
**File:** `frontend/src/hooks/useSmoothProgress.ts`

**Purpose:** Smooth progress bar animation with stages.

```typescript
const { progress, stage } = useProgressWithStages(
  isActive,
  actualProgress
);
```

**Stages:**
- 0-30%: Initial planning
- 30-60%: Enrichment
- 60-90%: Place details
- 90-100%: Finalization

#### useVirtualScroll
**File:** `frontend/src/hooks/useVirtualScroll.ts`

**Purpose:** Virtual scrolling for large lists.

```typescript
const { visibleItems, scrollProps } = useVirtualScroll({
  items: allItems,
  itemHeight: 100,
  containerHeight: 600
});
```

#### useLazyLoad
**File:** `frontend/src/hooks/useLazyLoad.ts`

**Purpose:** Lazy load images and components.

```typescript
const { ref, isVisible } = useLazyLoad({
  threshold: 0.1,
  rootMargin: '50px'
});
```

### 8.2.5 Device Detection Hooks

#### useDeviceDetection
**File:** `frontend/src/hooks/useDeviceDetection.ts`

**Purpose:** Detect device type and capabilities.

```typescript
const { 
  isMobile, 
  isTablet, 
  isDesktop,
  isTouchDevice,
  screenSize 
} = useDeviceDetection();
```

#### useScrollDetection
**File:** `frontend/src/hooks/useScrollDetection.ts`

**Purpose:** Track scroll position and direction.

```typescript
const { 
  scrollY, 
  scrollDirection,
  isScrollingDown,
  isAtTop,
  isAtBottom 
} = useScrollDetection();
```

### 8.2.6 Complete Hook List

| Hook | Purpose | Key Features |
|------|---------|--------------|
| `useGenerationStatus` | Poll agent status | Fallback polling, completion detection |
| `useNormalizedItinerary` | Itinerary state | Change tracking, undo/redo |
| `useSseConnection` | SSE connection | Auto-reconnect, event parsing |
| `useGoogleMaps` | Google Maps API | API loading, initialization |
| `useMapState` | Map state | Center, zoom, markers |
| `useChatHistory` | Chat history | Local storage, persistence |
| `useChangePreview` | Change preview | Diff calculation, preview |
| `useDebounce` | Debounce values | Configurable delay |
| `useLocalStorage` | Local storage | Type-safe storage |
| `useKeyboardShortcut` | Keyboard shortcuts | Global shortcuts |
| `useFormSubmission` | Form handling | Validation, submission |
| `useAutoRefresh` | Auto-refresh | Configurable interval |
| `useSmoothProgress` | Progress animation | Staged animation |
| `useDeviceDetection` | Device detection | Mobile/tablet/desktop |
| `useScrollDetection` | Scroll tracking | Position, direction |
| `useMobileScroll` | Mobile scroll | Touch-optimized |
| `useSwipeGesture` | Swipe detection | Left/right/up/down |
| `useVirtualScroll` | Virtual scrolling | Performance optimization |
| `useLazyLoad` | Lazy loading | Intersection observer |
| `useWorkflowSync` | Workflow sync | Position synchronization |

---

## 8.3 Utility Functions

**Directory:** `frontend/src/utils/`

### 8.3.1 Error Handling

#### errorHandler
**File:** `frontend/src/utils/errorHandler.ts`

**Purpose:** Centralized error handling and logging.

```typescript
class ErrorHandler {
  static handle(error: Error, context?: ErrorContext): AppError;
  static classify(error: unknown): ErrorType;
  static shouldRetry(error: unknown, key: string): boolean;
  static getRetryDelay(attemptIndex: number): number;
  static getUserMessage(error: unknown): string;
  static incrementRetryCount(key: string): void;
  static resetRetryCount(key: string): void;
}
```

**Error Types:**
- `NETWORK_ERROR` - Network failures
- `AUTH_FAILED` - Authentication errors
- `VALIDATION_ERROR` - Input validation
- `NOT_FOUND` - Resource not found
- `SERVER_ERROR` - 5xx errors
- `UNKNOWN_ERROR` - Unclassified

#### errorMessages
**File:** `frontend/src/utils/errorMessages.ts`

**Purpose:** User-friendly error message mapping.

```typescript
const ERROR_MESSAGES = {
  NETWORK_ERROR: 'Unable to connect. Please check your internet connection.',
  AUTH_FAILED: 'Authentication failed. Please sign in again.',
  VALIDATION_ERROR: 'Please check your input and try again.',
  NOT_FOUND: 'The requested resource was not found.',
  SERVER_ERROR: 'Server error. Please try again later.',
};
```

### 8.3.2 Formatting

#### formatters
**File:** `frontend/src/utils/formatters.ts`

**Functions:**
```typescript
// Date formatting
formatDate(date: Date | string, format: string): string;
formatRelativeTime(date: Date | string): string;
formatDateRange(start: Date, end: Date): string;

// Time formatting
formatTime(time: Date | string, format: '12h' | '24h'): string;
formatDuration(minutes: number): string;

// Currency formatting
formatCurrency(amount: number, currency: string): string;
formatPrice(amount: number, currency: string, options?: FormatOptions): string;

// Number formatting
formatNumber(value: number, decimals?: number): string;
formatPercentage(value: number): string;
formatDistance(km: number, unit: 'km' | 'mi'): string;
```

### 8.3.3 Validation

#### validators
**File:** `frontend/src/utils/validators.ts`

**Functions:**
```typescript
// Form validation
validateEmail(email: string): boolean;
validatePhone(phone: string): boolean;
validateUrl(url: string): boolean;
validateDate(date: string): boolean;
validateDateRange(start: string, end: string): boolean;

// Data validation
validateItinerary(itinerary: any): ValidationResult;
validateNode(node: any): ValidationResult;
validateChangeSet(changeSet: any): ValidationResult;

// Input sanitization
sanitizeInput(input: string): string;
sanitizeHtml(html: string): string;
```

### 8.3.4 Data Manipulation

#### itineraryUtils
**File:** `frontend/src/utils/itineraryUtils.ts`

**Functions:**
```typescript
// Node operations
findNodeById(itinerary: NormalizedItinerary, nodeId: string): NormalizedNode | null;
findNodesByDay(itinerary: NormalizedItinerary, dayNumber: number): NormalizedNode[];
findNodesByType(itinerary: NormalizedItinerary, type: string): NormalizedNode[];

// Day operations
getDayByNumber(itinerary: NormalizedItinerary, dayNumber: number): NormalizedDay | null;
calculateDayTotals(day: NormalizedDay): DayTotals;
sortNodesByTime(nodes: NormalizedNode[]): NormalizedNode[];

// Itinerary calculations
calculateTotalCost(itinerary: NormalizedItinerary): number;
calculateTotalDistance(itinerary: NormalizedItinerary): number;
calculateTotalDuration(itinerary: NormalizedItinerary): number;
```

#### diffUtils
**File:** `frontend/src/utils/diffUtils.ts`

**Purpose:** Calculate differences between itinerary versions.

```typescript
function calculateDiff(
  oldItinerary: NormalizedItinerary,
  newItinerary: NormalizedItinerary
): ItineraryDiff;

function applyDiff(
  itinerary: NormalizedItinerary,
  diff: ItineraryDiff
): NormalizedItinerary;
```

### 8.3.5 Map Utilities

#### mapUtils
**File:** `frontend/src/utils/mapUtils.ts`

**Functions:**
```typescript
// Bounds calculation
calculateBounds(coordinates: Coordinates[]): MapBounds;
fitBounds(map: any, bounds: MapBounds, padding?: number): void;

// Distance calculation
calculateDistance(from: Coordinates, to: Coordinates): number;
calculateRoute(waypoints: Coordinates[]): RouteInfo;

// Marker utilities
createMarker(node: NormalizedNode): MapMarker;
clusterMarkers(markers: MapMarker[], zoom: number): MarkerCluster[];
```

#### googleMapsLoader
**File:** `frontend/src/utils/googleMapsLoader.ts`

**Purpose:** Async Google Maps API loader.

```typescript
async function loadGoogleMaps(apiKey: string): Promise<void>;
function isGoogleMapsLoaded(): boolean;
```

### 8.3.6 Other Utilities

| Utility | File | Purpose |
|---------|------|---------|
| **logger** | `logger.ts` | Structured logging |
| **analytics** | `analytics.ts` | Event tracking |
| **cache** | `cache.ts` | Client-side caching |
| **encodingUtils** | `encodingUtils.ts` | URL encoding/decoding |
| **mobileTesting** | `mobileTesting.ts` | Mobile device testing |

---

## 8.4 Configuration

### 8.4.1 Firebase Configuration

**File:** `frontend/src/config/firebase.ts`

```typescript
const firebaseConfig = {
  apiKey: import.meta.env.VITE_FIREBASE_API_KEY,
  authDomain: import.meta.env.VITE_FIREBASE_AUTH_DOMAIN,
  projectId: import.meta.env.VITE_FIREBASE_PROJECT_ID,
  storageBucket: import.meta.env.VITE_FIREBASE_STORAGE_BUCKET,
  messagingSenderId: import.meta.env.VITE_FIREBASE_MESSAGING_SENDER_ID,
  appId: import.meta.env.VITE_FIREBASE_APP_ID
};

export const app = initializeApp(firebaseConfig);
export const auth = getAuth(app);
```

### 8.4.2 React Query Configuration

**File:** `frontend/src/state/query/client.ts`

```typescript
export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      refetchOnWindowFocus: false,
      retry: (failureCount, error) => {
        return ErrorHandler.shouldRetry(error, `query_${failureCount}`);
      },
      retryDelay: (attemptIndex) => {
        return ErrorHandler.getRetryDelay(attemptIndex);
      }
    },
    mutations: {
      retry: (failureCount, error) => {
        return ErrorHandler.shouldRetry(error, `mutation_${failureCount}`);
      },
      retryDelay: (attemptIndex) => {
        return ErrorHandler.getRetryDelay(attemptIndex);
      }
    }
  }
});
```

### 8.4.3 Environment Variables

**File:** `frontend/.env.local`

```bash
# API Configuration
VITE_API_BASE_URL=http://localhost:8080

# Google Maps
VITE_GOOGLE_MAPS_BROWSER_KEY=your_api_key_here

# Firebase
VITE_FIREBASE_API_KEY=your_api_key_here
VITE_FIREBASE_AUTH_DOMAIN=your_domain_here
VITE_FIREBASE_PROJECT_ID=your_project_id_here
VITE_FIREBASE_STORAGE_BUCKET=your_bucket_here
VITE_FIREBASE_MESSAGING_SENDER_ID=your_sender_id_here
VITE_FIREBASE_APP_ID=your_app_id_here

# Debug
VITE_DEBUG=false
```

### 8.4.4 Static Data

**File:** `frontend/src/data/destinations.ts`

**Purpose:** Popular destinations data for autocomplete and suggestions.

```typescript
export const popularDestinations = [
  {
    id: 'paris',
    name: 'Paris',
    country: 'France',
    coordinates: { lat: 48.8566, lng: 2.3522 },
    image: '/images/destinations/paris.jpg',
    description: 'The City of Light...'
  },
  // ... more destinations
];
```

---

**[← Back to Main Specification](../FRONTEND_UI_REDESIGN_SPECIFICATION.md)** | **[Next: UI/UX Patterns →](09-uiux-patterns.md)**
