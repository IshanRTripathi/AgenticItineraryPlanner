# 7. Data Models & Types

**Last Updated:** January 25, 2025  
**Purpose:** Document all TypeScript interfaces, data transformations, and type systems

---

## Overview

The application uses two primary data formats:
1. **TripData** - Legacy format with comprehensive metadata
2. **NormalizedItinerary** - New backend format (current standard)

Additional type systems exist for Chat, Maps, and UI state.

---

## 7.1 TripData Type System (Legacy Format)

**File:** `frontend/src/types/TripData.ts`

### 7.1.1 Core Types

#### TripData (Root Interface)
```typescript
interface TripData {
  id: string;
  userId: string;
  startLocation: TripLocation;
  endLocation: TripLocation;
  dates: {
    start: string; // ISO date
    end: string;
    flexible: boolean;
  };
  travelers: Traveler[];
  preferences: TravelPreferences;
  settings: TripSettings;
  budget: {
    total: number;
    currency: string;
    perDay: number;
    breakdown: {
      accommodation: number;
      food: number;
      activities: number;
      transport: number;
      shopping: number;
      other: number;
    };
  };
  itinerary?: TripItinerary;
  status: 'draft' | 'planning' | 'generating' | 'completed' | 'failed';
  createdAt: string;
  updatedAt: string;
  metadata: {
    version: number;
    source: string;
    generatedBy: string[];
  };
}
```

#### TripLocation
```typescript
interface TripLocation {
  id: string;
  name: string;
  country: string;
  city: string;
  coordinates: { lat: number; lng: number };
  timezone: string;
  currency: string;
  exchangeRate: number;
}
```

#### TravelPreferences
```typescript
interface TravelPreferences {
  heritage: number;      // 0-100 slider
  nightlife: number;
  adventure: number;
  relaxation: number;
  culture: number;
  nature: number;
  shopping: number;
  cuisine: number;
  photography: number;
  spiritual: number;
}
```

#### TripSettings
```typescript
interface TripSettings {
  womenFriendly: boolean;
  petFriendly: boolean;
  veganOnly: boolean;
  wheelchairAccessible: boolean;
  budgetFriendly: boolean;
  luxuryOnly: boolean;
  familyFriendly: boolean;
  soloTravelSafe: boolean;
}
```

#### Traveler
```typescript
interface Traveler {
  id: string;
  name: string;
  email: string;
  age: number;
  preferences?: {
    dietaryRestrictions: string[];
    mobilityNeeds: string[];
    interests: string[];
  };
}
```


### 7.1.2 Trip Component Types

#### TripComponent (Activity/Place)
```typescript
interface TripComponent {
  id: string;
  type: 'attraction' | 'hotel' | 'restaurant' | 'activity' | 'transport' | 'shopping' | 'entertainment';
  name: string;
  description: string;
  
  location: {
    name: string;
    address: string;
    coordinates: { lat: number | null; lng: number | null };
  };
  
  timing: {
    startTime: string;      // ISO string
    endTime: string;
    duration: number;       // minutes
    suggestedDuration: number;
  };
  
  cost: {
    pricePerPerson: number;
    currency: string;
    priceRange: 'budget' | 'mid-range' | 'luxury';
    includesWhat: string[];
    additionalCosts?: string[];
  };
  
  travel: {
    distanceFromPrevious: number;  // km
    travelTimeFromPrevious: number; // minutes
    transportMode: 'walking' | 'taxi' | 'bus' | 'train' | 'flight' | 'car';
    transportCost: number;
  };
  
  details: {
    rating: number;         // 1-5
    reviewCount: number;
    category: string;
    tags: string[];
    openingHours: Record<string, { open: string; close: string } | null>;
    contact: {
      phone?: string;
      website?: string;
      email?: string;
    };
    accessibility: {
      wheelchairAccessible: boolean;
      elevatorAccess: boolean;
      restrooms: boolean;
      parking: boolean;
    };
    amenities: string[];
  };
  
  booking: {
    required: boolean;
    bookingUrl?: string;
    phone?: string;
    notes?: string;
  };
  
  media: {
    images: string[];
    videos?: string[];
    virtualTour?: string;
  };
  
  tips: {
    bestTimeToVisit: string;
    whatToBring: string[];
    insider: string[];
    warnings: string[];
  };
  
  alternatives?: Array<{
    id: string;
    name: string;
    reason: string;
  }>;
  
  userNotes?: string;
  isCustom?: boolean;
  addedByUser?: boolean;
  priority: 'must-visit' | 'recommended' | 'optional' | 'backup';
  locked?: boolean;
}
```

#### DayPlan
```typescript
interface DayPlan {
  id: string;
  date: string;          // YYYY-MM-DD
  dayNumber: number;
  theme: string;
  location: string;      // main city/area
  components: TripComponent[];
  
  totalDistance: number;
  totalCost: number;
  totalDuration: number;
  startTime: string;
  endTime: string;
  
  meals: {
    breakfast?: TripComponent;
    lunch?: TripComponent;
    dinner?: TripComponent;
    snacks?: TripComponent[];
  };
  
  accommodation?: TripComponent;
  weather?: {
    condition: string;
    temperature: { min: number; max: number };
    precipitation: number;
  };
  
  notes?: string;
  warnings?: string[];
}
```

#### TripItinerary
```typescript
interface TripItinerary {
  days: DayPlan[];
  totalDistance: number;
  totalCost: number;
  totalDuration: number;
  highlights: string[];
  warnings: string[];
  packingList?: string[];
}
```

---

## 7.2 NormalizedItinerary Type System (Current Format)

**File:** `frontend/src/types/NormalizedItinerary.ts`

### 7.2.1 Core Types

#### NormalizedItinerary (Root Interface)
```typescript
interface NormalizedItinerary {
  itineraryId: string;
  version: number;
  userId?: string;
  createdAt?: number;    // milliseconds since epoch
  updatedAt?: number;
  status?: ItineraryStatus; // 'planning' | 'generating' | 'completed' | 'failed'
  
  summary: string;
  currency: string;
  themes: string[];
  destination?: string;
  startDate?: string;
  endDate?: string;
  
  days: NormalizedDay[];
  settings: ItinerarySettings;
  agents: Record<string, AgentStatus>;
  
  mapBounds?: MapBounds;
  countryCentroid?: Coordinates;
}
```

#### NormalizedDay
```typescript
interface NormalizedDay {
  dayNumber: number;
  date: string;          // ISO date
  location: string;
  nodes: NormalizedNode[];
  edges?: Edge[];
  pacing?: Pacing;
  timeWindow?: TimeWindow;
  totals?: DayTotals;
  warnings?: string[];
  notes?: string;
}
```

#### NormalizedNode
```typescript
interface NormalizedNode {
  id: string;
  type: 'attraction' | 'meal' | 'hotel' | 'transit' | 'transport' | 'accommodation';
  title: string;
  
  location?: NodeLocation;
  timing?: NodeTiming;
  cost?: NodeCost;
  details?: NodeDetails;
  
  labels?: string[];
  tips?: NodeTips;
  links?: NodeLinks;
  transit?: TransitInfo;
  
  locked?: boolean;
  bookingRef?: string;
  status?: 'planned' | 'in_progress' | 'skipped' | 'cancelled' | 'completed';
  updatedBy?: 'agent' | 'user';
  updatedAt?: number;    // milliseconds since epoch
}
```

### 7.2.2 Node Sub-Types

#### NodeLocation
```typescript
interface NodeLocation {
  name: string;
  address: string;
  coordinates: Coordinates;
}

interface Coordinates {
  lat: number;
  lng: number;
}
```

#### NodeTiming
```typescript
interface NodeTiming {
  startTime?: number;    // milliseconds since epoch
  endTime?: number;
  durationMin?: number;
}
```

#### NodeCost
```typescript
interface NodeCost {
  amount: number;
  currency: string;
  per: string;           // "person", "group", "night", etc.
}
```

#### NodeDetails
```typescript
interface NodeDetails {
  rating?: number;
  category?: string;
  tags?: string[];
  timeSlots?: TimeSlot[];
  googleMapsUri?: string;
}

interface TimeSlot {
  start: string;         // HH:MM
  end: string;
  available: boolean;
}
```

#### NodeTips
```typescript
interface NodeTips {
  bestTime?: string[];
  travel?: string[];
  warnings?: string[];
}
```

#### NodeLinks
```typescript
interface NodeLinks {
  booking?: string;
  website?: string;
  phone?: string;
}
```

### 7.2.3 Graph Types

#### Edge
```typescript
interface Edge {
  from: string;          // node ID
  to: string;            // node ID
  transitInfo?: TransitInfo;
}
```

#### TransitInfo
```typescript
interface TransitInfo {
  mode: string;          // "walking", "taxi", "bus", "train", etc.
  durationMin?: number;
  provider?: string;
  bookingUrl?: string;
}
```

### 7.2.4 Day Metadata Types

#### Pacing
```typescript
interface Pacing {
  style: string;         // "relaxed", "balanced", "intensive"
  avgDurationMin: number;
  maxNodesPerDay: number;
}
```

#### TimeWindow
```typescript
interface TimeWindow {
  start: string;         // HH:MM
  end: string;
}
```

#### DayTotals
```typescript
interface DayTotals {
  distanceKm: number;
  cost: number;
  durationHr: number;
}
```

### 7.2.5 Settings and Status Types

#### ItinerarySettings
```typescript
interface ItinerarySettings {
  autoApply: boolean;
  defaultScope: string;  // "trip" | "day"
}
```

#### AgentStatus
```typescript
interface AgentStatus {
  lastRunAt?: number;    // milliseconds since epoch
  status: string;        // "idle" | "running" | "completed" | "failed"
}
```

---

## 7.3 Change Management Types

### 7.3.1 ChangeSet

```typescript
interface ChangeSet {
  scope: 'trip' | 'day';
  day?: number;
  ops: ChangeOperation[];
  preferences?: ChangePreferences;
}
```

### 7.3.2 ChangeOperation

```typescript
interface ChangeOperation {
  op: 'move' | 'insert' | 'delete' | 'replace' | 'update' | 'update_edge';
  id?: string;
  after?: string;
  startTime?: string;
  endTime?: string;
  node?: NormalizedNode;
}
```

**Operation Types:**
- `move` - Move node to new position
- `insert` - Insert new node
- `delete` - Remove node
- `replace` - Replace node with another
- `update` - Update node properties
- `update_edge` - Update edge/transit info

### 7.3.3 ChangePreferences

```typescript
interface ChangePreferences {
  userFirst: boolean;      // Prioritize user changes
  autoApply: boolean;      // Auto-apply without confirmation
  respectLocks: boolean;   // Respect locked nodes
}
```

### 7.3.4 ItineraryDiff

```typescript
interface ItineraryDiff {
  added: DiffItem[];
  removed: DiffItem[];
  updated: DiffItem[];
  toVersion: number;
}

interface DiffItem {
  nodeId: string;
  day: number;
  fields?: string[];
  title?: string;
}
```

---

## 7.4 Chat Types

**File:** `frontend/src/types/ChatTypes.ts`

### 7.4.1 Request/Response Types

#### ChatRequest
```typescript
interface ChatRequest {
  itineraryId: string;
  scope: 'trip' | 'day';
  day?: number;
  selectedNodeId?: string;
  text: string;
  autoApply: boolean;
  userId?: string;
}
```

#### ChatResponse
```typescript
interface ChatResponse {
  intent: string;
  message: string;
  changeSet?: ChangeSet;
  diff?: ItineraryDiff;
  applied: boolean;
  toVersion?: number;
  warnings: string[];
  needsDisambiguation: boolean;
  candidates: NodeCandidate[];
  errors?: string[];
}
```

### 7.4.2 Disambiguation Types

#### NodeCandidate
```typescript
interface NodeCandidate {
  id: string;
  title: string;
  day: number;
  type: string;
  location: string;
  confidence?: number;
}
```

### 7.4.3 UI Types

#### ChatMessage
```typescript
interface ChatMessage {
  id: string;
  text: string;
  sender: 'user' | 'assistant';
  timestamp: Date | number | string;
  proposedChanges?: ChangeSet;
  applied?: boolean;
  error?: string;
}
```

---

## 7.5 Map Types

**File:** `frontend/src/types/MapTypes.ts`

### 7.5.1 Map Configuration

#### MapConfig
```typescript
interface MapConfig {
  center: Coordinates;
  zoom: number;
  mapTypeId: MapTypeId;  // 'roadmap' | 'satellite' | 'hybrid' | 'terrain'
  gestureHandling: 'greedy' | 'cooperative' | 'none' | 'auto';
  fullscreenControl: boolean;
  streetViewControl: boolean;
  mapTypeControl: boolean;
  zoomControl: boolean;
}
```

#### MapBounds
```typescript
interface MapBounds {
  south: number;
  west: number;
  north: number;
  east: number;
}
```

### 7.5.2 Marker Types

#### MapMarker
```typescript
interface MapMarker {
  id: string;
  position: Coordinates;
  title: string;
  type: 'attraction' | 'meal' | 'accommodation' | 'transport';
  status: 'planned' | 'in_progress' | 'completed' | 'skipped' | 'cancelled';
  locked: boolean;
  rating?: number;
  googleMapsUri?: string;
}
```

### 7.5.3 Camera Types

#### MapCameraOptions
```typescript
interface MapCameraOptions {
  center?: Coordinates;
  zoom?: number;
  bounds?: MapBounds;
  padding?: number;
}
```

#### MapAnimationOptions
```typescript
interface MapAnimationOptions {
  duration?: number;
  easing?: 'linear' | 'ease-in' | 'ease-out' | 'ease-in-out';
}
```

---

## 7.6 Data Transformations

### 7.6.1 Adapter Functions

**File:** `frontend/src/utils/itineraryAdapter.ts`

#### NormalizedItinerary → TripData
```typescript
function convertNormalizedToTripData(
  normalized: NormalizedItinerary
): TripData {
  // Converts new format to legacy format
  // Used for backward compatibility
}
```

**File:** `frontend/src/utils/normalizedToTripDataAdapter.ts`

#### TripData → NormalizedItinerary
```typescript
function convertTripDataToNormalized(
  tripData: TripData
): NormalizedItinerary {
  // Converts legacy format to new format
  // Used when migrating old data
}
```

### 7.6.2 Place Transformations

**File:** `frontend/src/utils/placeToWorkflowNode.ts`

#### Google Place → WorkflowNode
```typescript
function createWorkflowNodeFromPlace(
  place: GooglePlaceResult,
  dayNumber: number
): WorkflowNode {
  // Converts Google Places API result to workflow node
}
```

**File:** `frontend/src/utils/addPlaceToItinerary.ts`

#### Google Place → NormalizedNode
```typescript
function addPlaceToItineraryDay(
  itinerary: NormalizedItinerary,
  dayNumber: number,
  place: GooglePlaceResult
): NormalizedItinerary {
  // Adds Google Place to specific day in itinerary
}
```

### 7.6.3 Type Guards

**File:** `frontend/src/utils/typeGuards.ts`

```typescript
function isNormalizedItinerary(data: any): data is NormalizedItinerary {
  return data && typeof data.itineraryId === 'string' && Array.isArray(data.days);
}

function isTripData(data: any): data is TripData {
  return data && typeof data.id === 'string' && data.startLocation && data.endLocation;
}

function isNormalizedNode(node: any): node is NormalizedNode {
  return node && typeof node.id === 'string' && typeof node.type === 'string';
}
```

---

## 7.7 API Request/Response Types

**File:** `frontend/src/services/apiClient.ts`

### 7.7.1 Itinerary Operations

```typescript
interface CreateItineraryRequest {
  destination: string;
  startDate: string;
  endDate: string;
  travelers: Traveler[];
  budget: BudgetRange;
  interests: string[];
  preferences: TravelPreferences;
}

interface ItineraryCreationResponse {
  itineraryId: string;
  status: 'planning' | 'generating';
  estimatedCompletionTime: number;
}

interface ItineraryResponse {
  itinerary: NormalizedItinerary;
  version: number;
}
```

### 7.7.2 Change Operations

```typescript
interface ProposeRequest {
  changeSet: ChangeSet;
}

interface ProposeResponse {
  diff: ItineraryDiff;
  warnings: string[];
  conflicts: Conflict[];
}

interface ApplyRequest {
  changeSet: ChangeSet;
}

interface ApplyResponse {
  success: boolean;
  toVersion: number;
  applied: ChangeOperation[];
  rejected: ChangeOperation[];
  warnings: string[];
}

interface UndoRequest {
  toVersion: number;
}

interface UndoResponse {
  success: boolean;
  currentVersion: number;
}
```

---

**[← Back to Main Specification](../FRONTEND_UI_REDESIGN_SPECIFICATION.md)** | **[Next: Utilities & Services →](08-utilities-services.md)**
