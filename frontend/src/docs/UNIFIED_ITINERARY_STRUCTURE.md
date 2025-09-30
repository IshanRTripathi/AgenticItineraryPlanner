# Unified Itinerary Data Structure Design

## Overview

This document outlines a comprehensive, agent-friendly itinerary data structure that unifies day-by-day and workflow views while supporting efficient operations by specialized agents (location, photos, booking, etc.).

## Core Principles

1. **Single Source of Truth**: One data structure serves both day-by-day and workflow views
2. **Agent-Specific Sections**: Each agent has dedicated data sections they can efficiently modify
3. **Versioning Support**: Built-in versioning for tracking changes and rollbacks
4. **Bidirectional Sync**: Changes in either view automatically reflect in the other
5. **Extensibility**: Easy to add new agent types and data sections

## Main Data Structure

```typescript
interface UnifiedItinerary {
  // Core metadata
  id: string;
  version: number;
  userId: string;
  createdAt: string;
  updatedAt: string;
  
  // Trip overview
  trip: TripOverview;
  
  // Days array - main data container
  days: UnifiedDay[];
  
  // Global trip data
  global: GlobalTripData;
  
  // Versioning and change tracking
  changeHistory: ChangeRecord[];
}

interface TripOverview {
  destination: string;
  startDate: string;
  endDate: string;
  summary: string;
  currency: string;
  budgetTier: 'economy' | 'mid-range' | 'luxury';
  party: PartyInfo;
  interests: string[];
  constraints: string[];
  language: string;
}
```

## Unified Day Structure

```typescript
interface UnifiedDay {
  id: string;
  dayNumber: number;
  date: string;
  location: string;
  
  // Main activities/components
  components: UnifiedComponent[];
  
  // Day-level metadata
  metadata: DayMetadata;
  
  // Agent-specific data sections
  agentData: {
    location?: LocationAgentData;
    photos?: PhotosAgentData;
    booking?: BookingAgentData;
    weather?: WeatherAgentData;
    transport?: TransportAgentData;
    dining?: DiningAgentData;
    activities?: ActivitiesAgentData;
  };
  
  // Workflow-specific data
  workflow: WorkflowData;
  
  // Versioning
  version: number;
  lastModified: string;
  modifiedBy: string; // agent or user
}
```

## Unified Component Structure

```typescript
interface UnifiedComponent {
  id: string;
  type: ComponentType;
  title: string;
  description: string;
  
  // Core timing
  timing: TimingInfo;
  
  // Location data (Location Agent)
  location: LocationData;
  
  // Cost information
  cost: CostInfo;
  
  // Agent-specific data
  agentData: {
    photos?: PhotosData;
    booking?: BookingData;
    transport?: TransportData;
    dining?: DiningData;
    activities?: ActivitiesData;
  };
  
  // Workflow integration
  workflow: {
    nodeId: string;
    position: { x: number; y: number };
    connections: string[]; // connected node IDs
  };
  
  // Validation and status
  validation: ValidationInfo;
  
  // Versioning
  version: number;
  lastModified: string;
  modifiedBy: string;
}

type ComponentType = 
  | 'attraction' 
  | 'meal' 
  | 'transport' 
  | 'accommodation' 
  | 'free-time' 
  | 'shopping' 
  | 'entertainment';
```

## Agent-Specific Data Sections

### Location Agent Data
```typescript
interface LocationAgentData {
  // Map integration
  coordinates: {
    lat: number;
    lng: number;
  };
  address: string;
  placeId?: string;
  
  // Location metadata
  area: string;
  district: string;
  city: string;
  country: string;
  
  // Distance calculations
  distances: {
    fromPrevious?: number; // km
    toNext?: number; // km
    fromStart?: number; // km
  };
  
  // Map-specific data
  mapData: {
    zoomLevel?: number;
    markers?: MapMarker[];
    routes?: Route[];
  };
}

interface LocationData {
  name: string;
  address: string;
  coordinates: { lat: number; lng: number };
  placeId?: string;
  category: string;
  rating?: number;
  reviewCount?: number;
}
```

### Photos Agent Data
```typescript
interface PhotosAgentData {
  // Photo collections
  collections: PhotoCollection[];
  
  // Photo spots
  photoSpots: PhotoSpot[];
  
  // AI-generated photo suggestions
  suggestedPhotos: SuggestedPhoto[];
}

interface PhotosData {
  images: ImageInfo[];
  photoSpots: PhotoSpot[];
  instagramSpots: InstagramSpot[];
}

interface ImageInfo {
  id: string;
  url: string;
  alt: string;
  caption?: string;
  source: 'user' | 'ai-generated' | 'stock' | 'instagram';
  tags: string[];
  location?: { lat: number; lng: number };
  timestamp?: string;
}

interface PhotoSpot {
  id: string;
  name: string;
  description: string;
  coordinates: { lat: number; lng: number };
  bestTime: string;
  tips: string[];
  difficulty: 'easy' | 'medium' | 'hard';
}
```

### Booking Agent Data
```typescript
interface BookingAgentData {
  // Booking status tracking
  bookings: BookingRecord[];
  
  // Booking preferences
  preferences: BookingPreferences;
  
  // Integration data
  integrations: {
    hotels?: HotelIntegration;
    flights?: FlightIntegration;
    activities?: ActivityIntegration;
  };
}

interface BookingData {
  required: boolean;
  status: 'not-booked' | 'pending' | 'confirmed' | 'cancelled';
  bookingUrl?: string;
  confirmationNumber?: string;
  provider?: string;
  price?: number;
  currency?: string;
  notes?: string;
  deadline?: string; // booking deadline
}

interface BookingRecord {
  id: string;
  componentId: string;
  type: 'hotel' | 'flight' | 'activity' | 'restaurant' | 'transport';
  status: 'pending' | 'confirmed' | 'cancelled';
  provider: string;
  confirmationNumber: string;
  bookingDate: string;
  amount: number;
  currency: string;
}
```

### Transport Agent Data
```typescript
interface TransportAgentData {
  // Transport connections
  connections: TransportConnection[];
  
  // Route optimization
  routes: Route[];
  
  // Transport preferences
  preferences: TransportPreferences;
}

interface TransportData {
  mode: 'walk' | 'taxi' | 'bus' | 'train' | 'flight' | 'car' | 'bike';
  distance?: number; // km
  duration?: number; // minutes
  cost?: number;
  currency?: string;
  provider?: string;
  bookingRequired: boolean;
  bookingUrl?: string;
  notes?: string;
}

interface TransportConnection {
  id: string;
  fromComponentId: string;
  toComponentId: string;
  transport: TransportData;
  route?: Route;
  estimatedTime: number; // minutes
  cost: number;
  currency: string;
}
```

### Dining Agent Data
```typescript
interface DiningAgentData {
  // Restaurant recommendations
  recommendations: RestaurantRecommendation[];
  
  // Dietary preferences
  dietaryInfo: DietaryInfo;
  
  // Meal planning
  mealPlan: MealPlan;
}

interface DiningData {
  cuisine: string;
  priceRange: 'budget' | 'mid-range' | 'upscale' | 'fine-dining';
  dietaryOptions: string[]; // vegetarian, vegan, gluten-free, etc.
  specialties: string[];
  atmosphere: string;
  dressCode?: string;
  reservationRequired: boolean;
  reservationUrl?: string;
}
```

### Activities Agent Data
```typescript
interface ActivitiesAgentData {
  // Activity recommendations
  recommendations: ActivityRecommendation[];
  
  // Activity preferences
  preferences: ActivityPreferences;
  
  // Seasonal considerations
  seasonalInfo: SeasonalInfo;
}

interface ActivitiesData {
  category: string;
  difficulty: 'easy' | 'medium' | 'hard';
  duration: number; // minutes
  groupSize: 'solo' | 'couple' | 'small-group' | 'large-group';
  ageRestriction?: string;
  equipment?: string[];
  weatherDependent: boolean;
  indoorOutdoor: 'indoor' | 'outdoor' | 'both';
}
```

## Workflow Integration

```typescript
interface WorkflowData {
  // Node positions and connections
  nodes: WorkflowNode[];
  edges: WorkflowEdge[];
  
  // Workflow metadata
  layout: {
    zoom: number;
    pan: { x: number; y: number };
  };
  
  // Workflow-specific settings
  settings: {
    autoLayout: boolean;
    showConnections: boolean;
    groupByTime: boolean;
  };
}

interface WorkflowNode {
  id: string;
  componentId: string; // links to UnifiedComponent
  type: ComponentType;
  position: { x: number; y: number };
  data: {
    title: string;
    timing: TimingInfo;
    status: 'planned' | 'confirmed' | 'completed' | 'cancelled';
  };
}

interface WorkflowEdge {
  id: string;
  source: string; // node ID
  target: string; // node ID
  transport?: TransportData;
  type: 'transport' | 'walking' | 'timing';
}
```

## Versioning and Change Tracking

```typescript
interface ChangeRecord {
  id: string;
  timestamp: string;
  type: 'create' | 'update' | 'delete' | 'move';
  entityType: 'day' | 'component' | 'workflow';
  entityId: string;
  changes: ChangeDetail[];
  modifiedBy: string; // agent or user
  reason?: string;
}

interface ChangeDetail {
  field: string;
  oldValue: any;
  newValue: any;
  agent?: string; // which agent made the change
}

interface ValidationInfo {
  status: 'valid' | 'warning' | 'error';
  messages: ValidationMessage[];
  lastValidated: string;
  validatedBy: string;
}

interface ValidationMessage {
  type: 'error' | 'warning' | 'info';
  message: string;
  field?: string;
  suggestion?: string;
}
```

## Global Trip Data

```typescript
interface GlobalTripData {
  // Trip-wide settings
  settings: {
    timezone: string;
    currency: string;
    language: string;
    units: 'metric' | 'imperial';
  };
  
  // Global agent data
  agentData: {
    weather?: WeatherData;
    budget?: BudgetData;
    packing?: PackingData;
    documents?: DocumentsData;
  };
  
  // Trip statistics
  statistics: {
    totalCost: number;
    totalDistance: number;
    totalDuration: number;
    componentCount: number;
    bookingCount: number;
  };
}

interface WeatherData {
  forecast: WeatherForecast[];
  alerts: WeatherAlert[];
  recommendations: WeatherRecommendation[];
}

interface BudgetData {
  totalBudget: number;
  spent: number;
  remaining: number;
  breakdown: {
    accommodation: number;
    transport: number;
    dining: number;
    activities: number;
    shopping: number;
  };
}
```

## Agent Operations Interface

```typescript
// Base interface for all agents
interface Agent {
  id: string;
  name: string;
  version: string;
  capabilities: string[];
  
  // Core operations
  canProcess(component: UnifiedComponent): boolean;
  process(component: UnifiedComponent, context: AgentContext): Promise<UnifiedComponent>;
  validate(component: UnifiedComponent): ValidationInfo;
  
  // Data access
  getData(componentId: string, dataType: string): any;
  updateData(componentId: string, dataType: string, data: any): void;
}

interface AgentContext {
  trip: TripOverview;
  day: UnifiedDay;
  component: UnifiedComponent;
  userPreferences: UserPreferences;
  previousResults?: any;
}
```

## Benefits of This Structure

### 1. **Agent Efficiency**
- Each agent has dedicated data sections they can modify without affecting others
- Clear separation of concerns
- Easy to add new agent types

### 2. **Bidirectional Sync**
- Single data structure serves both views
- Changes in workflow automatically reflect in day-by-day view
- Changes in day-by-day view automatically reflect in workflow

### 3. **Versioning & Rollback**
- Complete change history
- Easy rollback to previous versions
- Audit trail for all modifications

### 4. **Extensibility**
- Easy to add new agent types
- New data sections can be added without breaking existing functionality
- Backward compatibility maintained

### 5. **Performance**
- Agents only process relevant data sections
- Efficient data access patterns
- Minimal data duplication

## Implementation Strategy

### Phase 1: Core Structure
1. Implement `UnifiedItinerary` and `UnifiedDay` structures
2. Create migration from current structures
3. Implement basic versioning

### Phase 2: Agent Integration
1. Implement agent-specific data sections
2. Create agent operation interfaces
3. Migrate existing agents to new structure

### Phase 3: Workflow Sync
1. Implement bidirectional sync between views
2. Add workflow-specific data structures
3. Create real-time update mechanisms

### Phase 4: Advanced Features
1. Implement advanced versioning
2. Add change tracking and rollback
3. Create agent marketplace/extensibility

## Example Usage

```typescript
// Location Agent updating coordinates
const locationAgent = new LocationAgent();
const updatedComponent = await locationAgent.process(component, {
  trip,
  day,
  component,
  userPreferences
});

// Photos Agent adding image URLs
const photosAgent = new PhotosAgent();
photosAgent.updateData(component.id, 'photos', {
  images: [
    {
      id: 'img1',
      url: 'https://example.com/photo1.jpg',
      alt: 'Beautiful sunset view',
      source: 'ai-generated',
      tags: ['sunset', 'landscape']
    }
  ]
});

// Booking Agent updating reservation status
const bookingAgent = new BookingAgent();
bookingAgent.updateData(component.id, 'booking', {
  status: 'confirmed',
  confirmationNumber: 'ABC123',
  provider: 'Booking.com'
});
```

This structure provides a solid foundation for efficient agent operations while maintaining perfect synchronization between all views and supporting comprehensive versioning.

