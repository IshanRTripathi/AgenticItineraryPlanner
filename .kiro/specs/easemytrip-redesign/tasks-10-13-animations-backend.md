# Tasks 10-13: Backend Integration & Real-time Features

> **UPDATE**: Week 10 is 70% complete! Core backend integration is functional.

## 📊 Current Progress

**Week 10 Status: 70% Complete** 🎉

**What's Working:**
- ✅ Real API integration with React Query
- ✅ TripDetailPage loads real itinerary data
- ✅ Dashboard loads real trip list
- ✅ ViewTab shows real statistics
- ✅ PlanTab displays real day-by-day breakdown
- ✅ BudgetTab calculates real costs
- ✅ Loading states with skeletons
- ✅ Error handling with retry
- ✅ Type-safe API calls
- ✅ Firebase auth token injection

**Still TODO:**
- ⏳ BookingsTab real data integration
- ⏳ WebSocket for real-time updates (Week 11)
- ⏳ Chat interface (Week 11)
- ⏳ Map integration (Week 11)
- ⏳ Weather API integration

**Completed Tasks:**
- ✅ Task 19: Replace Mock Data (100%)
- ✅ Task 20: Loading & Error States (100%)
- ✅ Task 21: Update Tab Components (75%)
- ⏳ Task 22: Authentication Flow (50%)

---

## Week 10: Core Backend Integration (P0 - CRITICAL)

### Task 19: Replace Mock Data with Real API Integration ✅ COMPLETE

**Priority**: P0 - CRITICAL (Blocks all other features)
**Estimated Time**: 8-10 hours
**Actual Time**: ~3 hours
**Dependencies**: None
**Status**: ✅ COMPLETE

#### 19.1: Set Up React Query Infrastructure

**Files to Create/Modify:**
- `frontend-redesign/src/hooks/useItinerary.ts` (NEW)
- `frontend-redesign/src/services/queryClient.ts` (NEW)
- `frontend-redesign/src/main.tsx` (MODIFY)

**Implementation:**

```typescript
// frontend-redesign/src/services/queryClient.ts
import { QueryClient } from '@tanstack/react-query';

export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 5 * 60 * 1000, // 5 minutes
      gcTime: 10 * 60 * 1000,   // 10 minutes
      refetchOnWindowFocus: false,
      refetchOnReconnect: false,
      retry: (failureCount, error) => {
        if (error.message.includes('401') || error.message.includes('404')) {
          return false;
        }
        return failureCount < 3;
      },
      retryDelay: (attemptIndex) => Math.min(1000 * 2 ** attemptIndex, 30000),
    },
  },
});
```


```typescript
// frontend-redesign/src/hooks/useItinerary.ts
import { useQuery } from '@tanstack/react-query';
import { apiClient } from '@/services/api';
import { NormalizedItinerary } from '@/types/dto';

export const queryKeys = {
  itinerary: (id: string) => ['itinerary', id] as const,
  itineraries: ['itineraries'] as const,
};

export function useItinerary(id: string) {
  return useQuery({
    queryKey: queryKeys.itinerary(id),
    queryFn: async () => {
      const response = await apiClient.get<NormalizedItinerary>(
        `/itineraries/${id}/json`
      );
      return response.data;
    },
    enabled: !!id,
  });
}
```

**Checklist:**
- [x] Install `@tanstack/react-query` ✅
- [x] Create `queryClient.ts` with proper configuration ✅
- [x] Create `useItinerary.ts` hook ✅
- [x] Wrap app with `QueryClientProvider` in `main.tsx` ✅
- [x] Test hook with a real itinerary ID ✅

#### 19.2: Update TripDetailPage to Use Real Data

**Files to Modify:**
- `frontend-redesign/src/pages/TripDetailPage.tsx`

**Changes Required:**

1. **Remove MOCK_TRIP constant** (lines 30-60)
2. **Add URL parameter extraction**:
```typescript
import { useParams } from 'react-router-dom';
import { useItinerary } from '@/hooks/useItinerary';

export function TripDetailPage() {
  const { id } = useParams<{ id: string }>();
  const { data: itinerary, isLoading, error, refetch } = useItinerary(id || '');
  
  // ... rest of component
}
```

3. **Add loading state**:
```typescript
if (isLoading) {
  return (
    <div className="min-h-screen flex flex-col">
      <Header />
      <main className="flex-1 flex items-center justify-center">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary mx-auto mb-4" />
          <p className="text-muted-foreground">Loading trip details...</p>
        </div>
      </main>
      <Footer />
    </div>
  );
}
```

4. **Add error state**:
```typescript
if (error) {
  return (
    <div className="min-h-screen flex flex-col">
      <Header />
      <main className="flex-1 flex items-center justify-center">
        <div className="text-center max-w-md">
          <div className="text-destructive text-5xl mb-4">⚠️</div>
          <h2 className="text-2xl font-bold mb-2">Failed to Load Trip</h2>
          <p className="text-muted-foreground mb-4">{error.message}</p>
          <Button onClick={() => refetch()}>Try Again</Button>
        </div>
      </main>
      <Footer />
    </div>
  );
}
```

5. **Map real data to UI**:
```typescript
// Extract data from NormalizedItinerary
const destination = itinerary?.days[0]?.location || 'Unknown';
const startDate = itinerary?.days[0]?.date || '';
const endDate = itinerary?.days[itinerary.days.length - 1]?.date || '';
const travelers = 2; // TODO: Get from itinerary metadata
const budget = 'moderate'; // TODO: Get from itinerary metadata
```

**Checklist:**
- [x] Remove MOCK_TRIP constant ✅
- [x] Add useParams and useItinerary hooks ✅
- [x] Implement loading state UI ✅
- [x] Implement error state UI ✅
- [x] Map NormalizedItinerary data to component props ✅
- [x] Test with real itinerary ID from backend ✅
- [x] Verify all tabs receive real data ✅


#### 19.3: Update Type Definitions

**Files to Modify:**
- `frontend-redesign/src/types/dto.ts`

**Add Missing Types:**

```typescript
// Complete NormalizedItinerary type from backend
export interface NormalizedItinerary {
  itineraryId: string;
  summary: string;
  version: number;
  currency: string;
  themes: string[];
  days: NormalizedDay[];
  agents: Record<string, AgentStatus>;
  updatedAt: number;
  status: 'planning' | 'ready' | 'failed';
  userId?: string;
}

export interface NormalizedDay {
  dayNumber: number;
  date: string;
  location: string;
  nodes: NormalizedNode[];
  edges: Edge[];
  totals: DayTotals;
}

export interface NormalizedNode {
  id: string;
  type: 'place' | 'activity' | 'meal' | 'transport' | 'accommodation';
  title: string;
  location: NodeLocation;
  timing: NodeTiming;
  cost: NodeCost;
  locked: boolean;
  bookingRef: string | null;
  labels: string[];
  tips: NodeTips;
}

export interface NodeLocation {
  name: string;
  lat: number;
  lng: number;
  placeId: string;
  address?: string;
}

export interface NodeTiming {
  startTime: string; // ISO 8601
  endTime: string;   // ISO 8601
  durationMin: number;
}

export interface NodeCost {
  amount: number;
  currency: string;
  per: 'person' | 'group' | 'night';
}

export interface NodeTips {
  bestTime: string[];
  warnings: string[];
  recommendations: string[];
}

export interface Edge {
  from: string;
  to: string;
  transitInfo: TransitInfo;
}

export interface TransitInfo {
  mode: 'walk' | 'drive' | 'transit' | 'flight';
  durationMin: number;
  distanceKm: number;
  provider?: string;
  cost?: NodeCost;
}

export interface DayTotals {
  cost: number;
  distanceKm: number;
  durationHr: number;
}

export interface AgentStatus {
  status: 'pending' | 'running' | 'completed' | 'failed';
  lastRunAt: string;
  error?: string;
}
```

**Checklist:**
- [x] Add all missing type definitions ✅
- [x] Remove old mock data types ✅
- [x] Ensure types match backend DTOs exactly ✅
- [x] Update imports across all components ✅
- [x] Run TypeScript compiler to check for errors ✅

---

### Task 20: Implement Loading & Error States

**Priority**: P0 - CRITICAL
**Estimated Time**: 4-6 hours
**Dependencies**: Task 19

#### 20.1: Create Skeleton Loaders

**Files to Create:**
- `frontend-redesign/src/components/loading/TripDetailSkeleton.tsx` (NEW)
- `frontend-redesign/src/components/loading/TabSkeleton.tsx` (NEW)

**Implementation:**

```typescript
// TripDetailSkeleton.tsx
export function TripDetailSkeleton() {
  return (
    <div className="min-h-screen flex flex-col">
      <Header />
      <main className="flex-1">
        {/* Hero Skeleton */}
        <div className="relative h-96 bg-muted animate-pulse" />
        
        {/* Content Skeleton */}
        <div className="container py-12">
          <div className="flex gap-4 mb-8">
            {[1, 2, 3, 4, 5, 6].map((i) => (
              <div key={i} className="h-10 w-24 bg-muted rounded animate-pulse" />
            ))}
          </div>
          
          <div className="space-y-4">
            {[1, 2, 3].map((i) => (
              <div key={i} className="h-32 bg-muted rounded animate-pulse" />
            ))}
          </div>
        </div>
      </main>
      <Footer />
    </div>
  );
}
```

**Checklist:**
- [ ] Create TripDetailSkeleton component
- [ ] Create TabSkeleton components for each tab
- [ ] Add skeleton animations
- [ ] Use skeletons in loading states
- [ ] Test skeleton appearance


#### 20.2: Create Error Boundary Component

**Files to Create:**
- `frontend-redesign/src/components/error/ErrorBoundary.tsx` (NEW)
- `frontend-redesign/src/components/error/ErrorDisplay.tsx` (NEW)

**Implementation:**

```typescript
// ErrorBoundary.tsx
import React from 'react';
import { ErrorDisplay } from './ErrorDisplay';

interface Props {
  children: React.ReactNode;
  fallback?: React.ReactNode;
}

interface State {
  hasError: boolean;
  error: Error | null;
}

export class ErrorBoundary extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = { hasError: false, error: null };
  }

  static getDerivedStateFromError(error: Error): State {
    return { hasError: true, error };
  }

  componentDidCatch(error: Error, errorInfo: React.ErrorInfo) {
    console.error('ErrorBoundary caught error:', error, errorInfo);
  }

  render() {
    if (this.state.hasError) {
      return this.props.fallback || (
        <ErrorDisplay
          error={this.state.error}
          onRetry={() => {
            this.setState({ hasError: false, error: null });
            window.location.reload();
          }}
        />
      );
    }

    return this.props.children;
  }
}
```

**Checklist:**
- [ ] Create ErrorBoundary class component
- [ ] Create ErrorDisplay functional component
- [ ] Wrap TripDetailPage with ErrorBoundary
- [ ] Add retry functionality
- [ ] Test error scenarios

---

### Task 21: Update Tab Components to Use Real Data

**Priority**: P0 - CRITICAL
**Estimated Time**: 8-10 hours
**Dependencies**: Task 19, 20

#### 21.1: Update ViewTab

**Files to Modify:**
- `frontend-redesign/src/components/trip/tabs/ViewTab.tsx`

**Changes:**

1. **Accept NormalizedItinerary prop**:
```typescript
interface ViewTabProps {
  itinerary: NormalizedItinerary;
}

export function ViewTab({ itinerary }: ViewTabProps) {
  // Calculate real statistics
  const totalDays = itinerary.days.length;
  const totalActivities = itinerary.days.reduce(
    (sum, day) => sum + day.nodes.filter(n => n.type === 'activity').length,
    0
  );
  const totalCost = itinerary.days.reduce(
    (sum, day) => sum + (day.totals?.cost || 0),
    0
  );
  
  // ... rest of component
}
```

2. **Display real trip statistics**
3. **Show actual destinations from itinerary**
4. **Add weather data integration** (if available)

**Checklist:**
- [x] Update ViewTab props to accept NormalizedItinerary ✅
- [x] Calculate real statistics from itinerary data ✅
- [x] Display actual trip information ✅
- [x] Remove hardcoded data ✅
- [x] Test with real itinerary ✅

#### 21.2: Update PlanTab

**Files to Modify:**
- `frontend-redesign/src/components/trip/tabs/PlanTab.tsx`

**Changes:**

1. **Accept NormalizedItinerary prop**
2. **Display real destinations**:
```typescript
export function PlanTab({ itinerary }: { itinerary: NormalizedItinerary }) {
  return (
    <div className="space-y-6">
      {itinerary.days.map((day) => (
        <Card key={day.dayNumber}>
          <CardHeader>
            <CardTitle>Day {day.dayNumber} - {day.location}</CardTitle>
            <p className="text-sm text-muted-foreground">{day.date}</p>
          </CardHeader>
          <CardContent>
            <div className="space-y-4">
              {day.nodes.map((node) => (
                <div key={node.id} className="flex items-start gap-4 p-4 border rounded-lg">
                  <div className="flex-1">
                    <h4 className="font-medium">{node.title}</h4>
                    <p className="text-sm text-muted-foreground">
                      {new Date(node.timing.startTime).toLocaleTimeString()} - 
                      {new Date(node.timing.endTime).toLocaleTimeString()}
                    </p>
                    <p className="text-sm">
                      {node.cost.amount} {node.cost.currency} per {node.cost.per}
                    </p>
                  </div>
                  {node.locked && (
                    <Badge variant="secondary">Locked</Badge>
                  )}
                  {node.bookingRef && (
                    <Badge variant="success">Booked</Badge>
                  )}
                </div>
              ))}
            </div>
          </CardContent>
        </Card>
      ))}
    </div>
  );
}
```

**Checklist:**
- [x] Update PlanTab to accept NormalizedItinerary ✅
- [x] Display real day-by-day breakdown ✅
- [x] Show actual activities with timing ✅
- [x] Display cost information ✅
- [x] Show booking status ✅
- [x] Add lock indicators ✅
- [x] Test with real data ✅
- [x] Add trip summary statistics ✅
- [x] Improve destinations view ✅


#### 21.3: Update BookingsTab

**Files to Modify:**
- `frontend-redesign/src/components/trip/tabs/BookingsTab.tsx`

**Changes:**

1. **Fetch real bookings from API**:
```typescript
import { useQuery } from '@tanstack/react-query';
import { apiClient } from '@/services/api';

export function BookingsTab({ itineraryId }: { itineraryId: string }) {
  const { data: bookings, isLoading } = useQuery({
    queryKey: ['bookings', itineraryId],
    queryFn: async () => {
      const response = await apiClient.get(`/bookings/itinerary/${itineraryId}`);
      return response.data;
    },
  });
  
  // Display real booking data
}
```

2. **Show actual booking status**
3. **Connect to real booking providers**

**Checklist:**
- [ ] Create useBookings hook
- [ ] Fetch real booking data
- [ ] Display actual booking status
- [ ] Show confirmation numbers
- [ ] Add booking actions (cancel, modify)
- [ ] Test with real bookings

#### 21.4: Update BudgetTab

**Files to Modify:**
- `frontend-redesign/src/components/trip/tabs/BudgetTab.tsx`

**Changes:**

1. **Calculate real costs from itinerary**:
```typescript
export function BudgetTab({ itinerary }: { itinerary: NormalizedItinerary }) {
  const totalCost = itinerary.days.reduce(
    (sum, day) => sum + (day.totals?.cost || 0),
    0
  );
  
  const costByCategory = itinerary.days.reduce((acc, day) => {
    day.nodes.forEach(node => {
      const category = node.type;
      acc[category] = (acc[category] || 0) + (node.cost?.amount || 0);
    });
    return acc;
  }, {} as Record<string, number>);
  
  // Display real budget breakdown
}
```

**Checklist:**
- [x] Calculate real costs from itinerary nodes ✅
- [x] Show cost breakdown by category ✅
- [x] Display daily budget ✅
- [x] Show budget vs actual ✅
- [ ] Add currency conversion (TODO: API integration)
- [ ] Test calculations

---

### Task 22: Implement Authentication Flow

**Priority**: P1 - HIGH
**Estimated Time**: 4-6 hours
**Dependencies**: Task 19

#### 22.1: Add Auth Token to API Requests

**Files to Modify:**
- `frontend-redesign/src/services/api.ts`

**Changes:**

```typescript
import { auth } from '@/config/firebase';

// Add auth interceptor
apiClient.interceptors.request.use(async (config) => {
  const user = auth.currentUser;
  if (user) {
    const token = await user.getIdToken();
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Add response interceptor for 401 errors
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      // Redirect to login
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);
```

**Checklist:**
- [x] Add request interceptor for auth token ✅
- [x] Add response interceptor for 401 errors ✅
- [ ] Handle token refresh (TODO)
- [ ] Test authenticated requests (TODO)
- [ ] Test unauthorized access (TODO)

#### 22.2: Protect Trip Detail Route

**Files to Modify:**
- `frontend-redesign/src/App.tsx`

**Changes:**

```typescript
<Route
  path="/trip/:id"
  element={
    <ProtectedRoute>
      <TripDetailPage />
    </ProtectedRoute>
  }
/>
```

**Checklist:**
- [ ] Wrap TripDetailPage with ProtectedRoute
- [ ] Test unauthorized access
- [ ] Test redirect to login
- [ ] Test successful authentication

---

## Week 11: Real-time Features & Chat Integration (P1 - HIGH)

### Task 23: Implement WebSocket Integration ⚠️ CRITICAL

**Priority**: P1 - HIGH
**Estimated Time**: 8-10 hours
**Dependencies**: Task 19-22

#### 23.1: Create WebSocket Service

**Files to Create:**
- `frontend-redesign/src/services/websocket.ts` (NEW)

**Implementation:**

```typescript
class WebSocketService {
  private ws: WebSocket | null = null;
  private reconnectAttempts = 0;
  private maxReconnectAttempts = 5;
  private reconnectDelay = 1000;
  private listeners: Map<string, Set<(data: any) => void>> = new Map();

  connect(itineraryId: string): Promise<void> {
    return new Promise((resolve, reject) => {
      const wsUrl = `${import.meta.env.VITE_WS_URL}/ws/itinerary/${itineraryId}`;
      
      this.ws = new WebSocket(wsUrl);
      
      this.ws.onopen = () => {
        console.log('WebSocket connected');
        this.reconnectAttempts = 0;
        resolve();
      };
      
      this.ws.onmessage = (event) => {
        const message = JSON.parse(event.data);
        this.emit(message.type, message.data);
      };
      
      this.ws.onerror = (error) => {
        console.error('WebSocket error:', error);
        reject(error);
      };
      
      this.ws.onclose = () => {
        console.log('WebSocket closed');
        this.handleReconnect(itineraryId);
      };
    });
  }

  private handleReconnect(itineraryId: string) {
    if (this.reconnectAttempts < this.maxReconnectAttempts) {
      this.reconnectAttempts++;
      const delay = this.reconnectDelay * Math.pow(2, this.reconnectAttempts);
      
      setTimeout(() => {
        console.log(`Reconnecting... Attempt ${this.reconnectAttempts}`);
        this.connect(itineraryId);
      }, delay);
    }
  }

  on(event: string, callback: (data: any) => void) {
    if (!this.listeners.has(event)) {
      this.listeners.set(event, new Set());
    }
    this.listeners.get(event)!.add(callback);
  }

  off(event: string, callback: (data: any) => void) {
    this.listeners.get(event)?.delete(callback);
  }

  private emit(event: string, data: any) {
    this.listeners.get(event)?.forEach(callback => callback(data));
  }

  disconnect() {
    this.ws?.close();
    this.ws = null;
    this.listeners.clear();
  }
}

export const webSocketService = new WebSocketService();
```

**Checklist:**
- [ ] Create WebSocketService class
- [ ] Implement connection logic
- [ ] Add reconnection handling
- [ ] Implement event system
- [ ] Add error handling
- [ ] Test connection
- [ ] Test reconnection


#### 23.2: Create UnifiedItineraryContext

**Files to Create:**
- `frontend-redesign/src/contexts/UnifiedItineraryContext.tsx` (PORT from original)
- `frontend-redesign/src/contexts/UnifiedItineraryTypes.ts` (PORT from original)
- `frontend-redesign/src/contexts/UnifiedItineraryReducer.ts` (PORT from original)

**Port Strategy:**
1. Copy files from `frontend/src/contexts/`
2. Update imports to match redesign structure
3. Integrate with redesign's API service
4. Test all context actions

**Checklist:**
- [ ] Port UnifiedItineraryContext from original
- [ ] Port UnifiedItineraryTypes
- [ ] Port UnifiedItineraryReducer
- [ ] Update imports and paths
- [ ] Integrate WebSocket service
- [ ] Test context provider
- [ ] Test all actions

#### 23.3: Integrate WebSocket with Context

**Files to Modify:**
- `frontend-redesign/src/contexts/UnifiedItineraryContext.tsx`

**Add WebSocket Listeners:**

```typescript
useEffect(() => {
  if (!itineraryId) return;
  
  webSocketService.connect(itineraryId);
  
  webSocketService.on('itinerary_updated', (data) => {
    // Reload itinerary
    queryClient.invalidateQueries(['itinerary', itineraryId]);
  });
  
  webSocketService.on('agent_progress', (data) => {
    dispatch({ type: 'UPDATE_AGENT_PROGRESS', payload: data });
  });
  
  webSocketService.on('chat_message', (data) => {
    dispatch({ type: 'ADD_CHAT_MESSAGE', payload: data });
  });
  
  return () => {
    webSocketService.disconnect();
  };
}, [itineraryId]);
```

**Checklist:**
- [ ] Add WebSocket connection in context
- [ ] Handle itinerary_updated events
- [ ] Handle agent_progress events
- [ ] Handle chat_message events
- [ ] Test real-time updates
- [ ] Test reconnection

---

### Task 24: Implement Chat Interface

**Priority**: P1 - HIGH
**Estimated Time**: 10-12 hours
**Dependencies**: Task 23

#### 24.1: Port Chat Components

**Files to Port:**
- `frontend/src/components/chat/NewChat.tsx` → `frontend-redesign/src/components/chat/NewChat.tsx`
- `frontend/src/components/chat/ChatMessage.tsx` → `frontend-redesign/src/components/chat/ChatMessage.tsx`

**Port Strategy:**
1. Copy component files
2. Update imports
3. Integrate with UnifiedItineraryContext
4. Update styling to match redesign

**Checklist:**
- [ ] Port NewChat component
- [ ] Port ChatMessage component
- [ ] Update imports
- [ ] Integrate with context
- [ ] Update styling
- [ ] Test chat functionality

#### 24.2: Add Chat Tab to TripDetailPage

**Files to Modify:**
- `frontend-redesign/src/pages/TripDetailPage.tsx`

**Add Chat Tab:**

```typescript
<TabsList>
  {/* ... existing tabs ... */}
  <TabsTrigger value="chat" className="flex items-center gap-2">
    <MessageSquare className="w-4 h-4" />
    <span className="hidden sm:inline">Chat</span>
  </TabsTrigger>
</TabsList>

<TabsContent value="chat">
  <UnifiedItineraryProvider itineraryId={id}>
    <NewChat />
  </UnifiedItineraryProvider>
</TabsContent>
```

**Checklist:**
- [ ] Add chat tab to TabsList
- [ ] Add chat TabsContent
- [ ] Wrap with UnifiedItineraryProvider
- [ ] Test chat integration
- [ ] Test itinerary updates from chat

#### 24.3: Implement Chat-based Modifications

**Files to Modify:**
- `frontend-redesign/src/components/chat/NewChat.tsx`

**Connect to Backend:**

```typescript
const handleSendMessage = async (message: string) => {
  try {
    const response = await apiClient.post(`/agents/process-request`, {
      itineraryId: itinerary.itineraryId,
      message,
      context: {
        selectedDay,
        selectedNodeId,
      },
    });
    
    // Handle response
    if (response.data.changeSet) {
      // Show preview
      // Apply changes
    }
  } catch (error) {
    // Handle error
  }
};
```

**Checklist:**
- [ ] Connect to process-request endpoint
- [ ] Handle chat responses
- [ ] Show change previews
- [ ] Apply changes to itinerary
- [ ] Test modifications
- [ ] Test error handling

---

### Task 25: Add Missing UI Components

**Priority**: P1 - HIGH
**Estimated Time**: 8-10 hours
**Dependencies**: Task 19-24

#### 25.1: Implement Map Integration

**Files to Create:**
- `frontend-redesign/src/components/map/TripMap.tsx` (NEW)

**Implementation:**

```typescript
import { GoogleMap, Marker, Polyline } from '@react-google-maps/api';

export function TripMap({ itinerary }: { itinerary: NormalizedItinerary }) {
  const markers = itinerary.days.flatMap(day =>
    day.nodes
      .filter(node => node.location)
      .map(node => ({
        position: { lat: node.location.lat, lng: node.location.lng },
        title: node.title,
      }))
  );
  
  const path = markers.map(m => m.position);
  
  return (
    <GoogleMap
      mapContainerStyle={{ width: '100%', height: '500px' }}
      center={markers[0]?.position}
      zoom={12}
    >
      {markers.map((marker, i) => (
        <Marker key={i} position={marker.position} title={marker.title} />
      ))}
      <Polyline path={path} options={{ strokeColor: '#3b82f6' }} />
    </GoogleMap>
  );
}
```

**Checklist:**
- [ ] Install @react-google-maps/api
- [ ] Create TripMap component
- [ ] Add markers for all locations
- [ ] Draw route polyline
- [ ] Add to PlanTab
- [ ] Test map rendering

#### 25.2: Add Weather Widget

**Files to Create:**
- `frontend-redesign/src/components/weather/WeatherWidget.tsx` (NEW)

**Implementation:**

```typescript
export function WeatherWidget({ location, date }: { location: string; date: string }) {
  const { data: weather } = useQuery({
    queryKey: ['weather', location, date],
    queryFn: async () => {
      // Call weather API
      const response = await fetch(
        `https://api.openweathermap.org/data/2.5/forecast?q=${location}&appid=${API_KEY}`
      );
      return response.json();
    },
  });
  
  return (
    <Card>
      <CardHeader>
        <CardTitle>Weather Forecast</CardTitle>
      </CardHeader>
      <CardContent>
        {/* Display weather data */}
      </CardContent>
    </Card>
  );
}
```

**Checklist:**
- [ ] Create WeatherWidget component
- [ ] Integrate weather API
- [ ] Add to ViewTab
- [ ] Display forecast
- [ ] Test weather data


#### 25.3: Add Place Photos

**Files to Create:**
- `frontend-redesign/src/components/places/PlacePhotos.tsx` (NEW)

**Implementation:**

```typescript
export function PlacePhotos({ placeId }: { placeId: string }) {
  const { data: photos } = useQuery({
    queryKey: ['place-photos', placeId],
    queryFn: async () => {
      const response = await apiClient.get(`/places/${placeId}/photos`);
      return response.data;
    },
  });
  
  return (
    <div className="grid grid-cols-3 gap-2">
      {photos?.map((photo, i) => (
        <img
          key={i}
          src={photo.url}
          alt={photo.attribution}
          className="w-full h-32 object-cover rounded"
        />
      ))}
    </div>
  );
}
```

**Checklist:**
- [ ] Create PlacePhotos component
- [ ] Integrate Google Places API
- [ ] Add to activity cards
- [ ] Display photo gallery
- [ ] Test photo loading

---

### Task 26: Implement Advanced Interactions

**Priority**: P2 - MEDIUM
**Estimated Time**: 6-8 hours
**Dependencies**: Task 19-25

#### 26.1: Add Drag & Drop for Activities

**Files to Modify:**
- `frontend-redesign/src/components/trip/tabs/PlanTab.tsx`

**Implementation:**

```typescript
import { DndContext, closestCenter, DragEndEvent } from '@dnd-kit/core';
import { SortableContext, verticalListSortingStrategy } from '@dnd-kit/sortable';

export function PlanTab({ itinerary }: { itinerary: NormalizedItinerary }) {
  const handleDragEnd = async (event: DragEndEvent) => {
    const { active, over } = event;
    
    if (active.id !== over?.id) {
      // Reorder activities
      const response = await apiClient.post(`/itineraries/${itinerary.itineraryId}:apply`, {
        changeSet: {
          scope: 'day',
          day: selectedDay,
          ops: [{
            op: 'move',
            id: active.id,
            position: over?.id,
          }],
        },
      });
      
      // Update UI
      queryClient.invalidateQueries(['itinerary', itinerary.itineraryId]);
    }
  };
  
  return (
    <DndContext collisionDetection={closestCenter} onDragEnd={handleDragEnd}>
      <SortableContext items={activities} strategy={verticalListSortingStrategy}>
        {/* Render activities */}
      </SortableContext>
    </DndContext>
  );
}
```

**Checklist:**
- [ ] Install @dnd-kit packages
- [ ] Add drag & drop to PlanTab
- [ ] Implement reordering logic
- [ ] Connect to backend API
- [ ] Test drag & drop
- [ ] Add visual feedback

#### 26.2: Implement Inline Editing

**Files to Modify:**
- `frontend-redesign/src/components/trip/tabs/PlanTab.tsx`

**Add Inline Editing:**

```typescript
const [editingNode, setEditingNode] = useState<string | null>(null);

const handleSaveEdit = async (nodeId: string, updates: Partial<NormalizedNode>) => {
  try {
    await apiClient.post(`/itineraries/${itinerary.itineraryId}:apply`, {
      changeSet: {
        scope: 'node',
        ops: [{
          op: 'update',
          id: nodeId,
          data: updates,
        }],
      },
    });
    
    queryClient.invalidateQueries(['itinerary', itinerary.itineraryId]);
    setEditingNode(null);
  } catch (error) {
    // Handle error
  }
};
```

**Checklist:**
- [ ] Add inline editing UI
- [ ] Implement save logic
- [ ] Connect to backend
- [ ] Add validation
- [ ] Test editing
- [ ] Add cancel functionality

---

## Week 12: Export, Share & Advanced Features (P2 - MEDIUM)

### Task 27: Implement Export Functionality

**Priority**: P2 - MEDIUM
**Estimated Time**: 6-8 hours
**Dependencies**: Task 19-26

#### 27.1: Add PDF Export

**Files to Create:**
- `frontend-redesign/src/services/pdfExport.ts` (NEW)

**Implementation:**

```typescript
import jsPDF from 'jspdf';
import html2canvas from 'html2canvas';

export async function exportToPDF(itinerary: NormalizedItinerary) {
  const pdf = new jsPDF();
  
  // Add title
  pdf.setFontSize(20);
  pdf.text(itinerary.summary, 20, 20);
  
  // Add days
  let yPosition = 40;
  itinerary.days.forEach((day, index) => {
    pdf.setFontSize(16);
    pdf.text(`Day ${day.dayNumber} - ${day.location}`, 20, yPosition);
    yPosition += 10;
    
    pdf.setFontSize(12);
    day.nodes.forEach(node => {
      pdf.text(`• ${node.title}`, 30, yPosition);
      yPosition += 7;
    });
    
    yPosition += 10;
  });
  
  pdf.save(`${itinerary.summary}.pdf`);
}
```

**Checklist:**
- [ ] Install jspdf and html2canvas
- [ ] Create PDF export service
- [ ] Add export button to ViewTab
- [ ] Format PDF nicely
- [ ] Test PDF generation
- [ ] Add loading state

#### 27.2: Add Share Functionality

**Files to Create:**
- `frontend-redesign/src/components/share/ShareModal.tsx` (NEW)

**Implementation:**

```typescript
export function ShareModal({ itinerary }: { itinerary: NormalizedItinerary }) {
  const [shareLink, setShareLink] = useState('');
  
  const generateShareLink = async () => {
    const response = await apiClient.post(`/itineraries/${itinerary.itineraryId}/share`);
    setShareLink(response.data.shareUrl);
  };
  
  return (
    <Dialog>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Share Trip</DialogTitle>
        </DialogHeader>
        <div className="space-y-4">
          <Button onClick={generateShareLink}>Generate Share Link</Button>
          {shareLink && (
            <div className="flex gap-2">
              <Input value={shareLink} readOnly />
              <Button onClick={() => navigator.clipboard.writeText(shareLink)}>
                Copy
              </Button>
            </div>
          )}
        </div>
      </DialogContent>
    </Dialog>
  );
}
```

**Checklist:**
- [ ] Create ShareModal component
- [ ] Implement share link generation
- [ ] Add copy to clipboard
- [ ] Add social media sharing
- [ ] Test sharing
- [ ] Add email sharing

---

### Task 28: Add Advanced Animations

**Priority**: P3 - LOW
**Estimated Time**: 4-6 hours
**Dependencies**: Task 19-27

#### 28.1: Page Transitions

**Files to Create:**
- `frontend-redesign/src/components/transitions/PageTransition.tsx` (ENHANCE)

**Add Smooth Transitions:**

```typescript
import { motion, AnimatePresence } from 'framer-motion';

export function PageTransition({ children }: { children: React.ReactNode }) {
  return (
    <AnimatePresence mode="wait">
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        exit={{ opacity: 0, y: -20 }}
        transition={{ duration: 0.3 }}
      >
        {children}
      </motion.div>
    </AnimatePresence>
  );
}
```

**Checklist:**
- [ ] Enhance PageTransition component
- [ ] Add tab transitions
- [ ] Add modal animations
- [ ] Test animations
- [ ] Optimize performance

#### 28.2: Micro-interactions

**Add Hover Effects:**
- [ ] Activity card hover
- [ ] Button hover states
- [ ] Tab hover effects
- [ ] Smooth scrolling
- [ ] Loading animations

---

### Task 29: Performance Optimization

**Priority**: P2 - MEDIUM
**Estimated Time**: 6-8 hours
**Dependencies**: Task 19-28

#### 29.1: Code Splitting

**Files to Modify:**
- `frontend-redesign/src/App.tsx`

**Add Lazy Loading:**

```typescript
const TripDetailPage = lazy(() => import('./pages/TripDetailPage'));
const DashboardPage = lazy(() => import('./pages/DashboardPage'));
```

**Checklist:**
- [ ] Add lazy loading for routes
- [ ] Add lazy loading for heavy components
- [ ] Add Suspense boundaries
- [ ] Test code splitting
- [ ] Measure bundle sizes

#### 29.2: Caching Strategies

**Optimize React Query:**

```typescript
export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 5 * 60 * 1000,
      gcTime: 10 * 60 * 1000,
      refetchOnWindowFocus: false,
    },
  },
});
```

**Checklist:**
- [ ] Optimize query cache times
- [ ] Add prefetching
- [ ] Implement optimistic updates
- [ ] Test caching behavior

---

### Task 30: Mobile & PWA Features

**Priority**: P2 - MEDIUM
**Estimated Time**: 6-8 hours
**Dependencies**: Task 19-29

#### 30.1: Mobile Optimization

**Checklist:**
- [ ] Test on mobile devices
- [ ] Optimize touch targets
- [ ] Add swipe gestures
- [ ] Test responsive design
- [ ] Optimize mobile performance

#### 30.2: PWA Features

**Files to Create:**
- `frontend-redesign/public/manifest.json` (NEW)
- `frontend-redesign/src/service-worker.ts` (NEW)

**Checklist:**
- [ ] Add PWA manifest
- [ ] Implement service worker
- [ ] Add offline support
- [ ] Test PWA installation
- [ ] Add push notifications

---

## Week 13: Testing, Polish & Production (P2-P3)

### Task 31: Comprehensive Testing

**Priority**: P2 - MEDIUM
**Estimated Time**: 8-10 hours

#### 31.1: Unit Tests

**Checklist:**
- [ ] Test useItinerary hook
- [ ] Test WebSocket service
- [ ] Test context actions
- [ ] Test utility functions
- [ ] Achieve 80%+ coverage

#### 31.2: Integration Tests

**Checklist:**
- [ ] Test API integration
- [ ] Test WebSocket connection
- [ ] Test authentication flow
- [ ] Test data synchronization

#### 31.3: E2E Tests

**Checklist:**
- [ ] Test trip detail page load
- [ ] Test tab navigation
- [ ] Test chat functionality
- [ ] Test booking flow
- [ ] Test export features

---

### Task 32: Accessibility & SEO

**Priority**: P2 - MEDIUM
**Estimated Time**: 4-6 hours

#### 32.1: WCAG 2.1 AA Compliance

**Checklist:**
- [ ] Add ARIA labels
- [ ] Implement keyboard navigation
- [ ] Ensure color contrast
- [ ] Add screen reader support
- [ ] Test with accessibility tools

#### 32.2: SEO Optimization

**Checklist:**
- [ ] Add meta tags
- [ ] Implement Open Graph tags
- [ ] Add structured data
- [ ] Create sitemap
- [ ] Optimize for search engines

---

### Task 33: Production Optimization

**Priority**: P2 - MEDIUM
**Estimated Time**: 6-8 hours

#### 33.1: Bundle Optimization

**Checklist:**
- [ ] Analyze bundle sizes
- [ ] Implement tree shaking
- [ ] Add compression
- [ ] Optimize assets
- [ ] Test production build

#### 33.2: Error Handling & Monitoring

**Checklist:**
- [ ] Add error tracking (Sentry)
- [ ] Implement user feedback
- [ ] Add performance monitoring
- [ ] Set up alerting
- [ ] Test error scenarios

---

### Task 34: Final Polish & Deployment

**Priority**: P3 - LOW
**Estimated Time**: 4-6 hours

#### 34.1: UI/UX Polish

**Checklist:**
- [ ] Fix visual inconsistencies
- [ ] Add final animations
- [ ] Ensure consistent spacing
- [ ] Validate design system
- [ ] Get design approval

#### 34.2: Documentation

**Checklist:**
- [ ] Create user documentation
- [ ] Add developer documentation
- [ ] Document API integrations
- [ ] Create deployment guide
- [ ] Update README

#### 34.3: Deployment Preparation

**Checklist:**
- [ ] Configure production environment
- [ ] Set up CI/CD pipelines
- [ ] Add environment configs
- [ ] Prepare rollback strategies
- [ ] Test deployment process

---

## Success Criteria

### Week 10 Success ✅
- [ ] TripDetailPage loads real data from backend
- [ ] All tabs show actual itinerary information
- [ ] Loading and error states implemented
- [ ] Type system aligned with backend
- [ ] No mock data remaining

### Week 11 Success ✅
- [ ] Real-time updates working via WebSocket
- [ ] Chat interface integrated and functional
- [ ] Map and weather components added
- [ ] Drag-and-drop interactions implemented
- [ ] UnifiedItineraryContext fully functional

### Week 12 Success ✅
- [ ] Export and share features working
- [ ] Advanced animations polished
- [ ] Performance optimized
- [ ] Mobile experience perfected
- [ ] PWA features implemented

### Week 13 Success ✅
- [ ] All tests passing
- [ ] Accessibility compliance achieved
- [ ] Production deployment ready
- [ ] Documentation complete
- [ ] Feature parity with original frontend

---

## Notes

**Current Status**: 0% backend integration, 100% mock data
**Target Status**: 100% backend integration, 0% mock data

**Critical Path**: Tasks 19-22 must be completed before any other work
**Blocking Issues**: None - ready to start

**Resources Needed**:
- Backend API access
- Firebase credentials
- Google Maps API key
- Weather API key
- Testing environment

**Estimated Total Time**: 80-100 hours across 4 weeks
