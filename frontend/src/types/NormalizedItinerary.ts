// Types for the new normalized JSON structure from the backend

export type ItineraryStatus = 'planning' | 'generating' | 'completed' | 'failed';

export interface NormalizedItinerary {
  itineraryId: string;
  version: number;
  userId?: string;
  createdAt?: number; // milliseconds since epoch
  updatedAt?: number; // milliseconds since epoch
  status?: ItineraryStatus; // Generation status
  summary: string;
  currency: string;
  themes: string[];
  constraints?: string[]; // User's custom instructions/requirements
  destination?: string;
  startDate?: string;
  endDate?: string;
  budgetMin?: number; // Minimum budget per person
  budgetMax?: number; // Maximum budget per person
  partySize?: number; // Total number of people
  budgetSummary?: BudgetSummary; // Budget tracking information
  days: NormalizedDay[];
  settings: ItinerarySettings;
  agents: Record<string, AgentStatus>;
  mapBounds?: MapBounds;
  countryCentroid?: Coordinates;
}

export interface NormalizedDay {
  dayNumber: number;
  date: string; // ISO date
  location: string;
  summary?: string;
  pace?: string; // "relaxed", "balanced", "intense"
  totalDistance?: number;
  totalCost?: number;
  totalDuration?: number;
  timeWindowStart?: string; // e.g., "09:00"
  timeWindowEnd?: string; // e.g., "18:00"
  timeZone?: string; // e.g., "UTC", "IST"
  nodes: NormalizedNode[];
  edges?: Edge[];
  warnings?: string[];
  notes?: string;
}

export interface NormalizedNode {
  id: string;
  // Backend uses: "attraction", "meal", "hotel", "transit"
  // We also support "transport" and "accommodation" as aliases for backward compatibility
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
  enrichmentStatus?: 'pending' | 'enriching' | 'enriched' | 'failed'; // Google Maps enrichment status
  updatedBy?: 'agent' | 'user';
  updatedAt?: number; // milliseconds since epoch
  agentData?: Record<string, any>;
}

export interface NodeLocation {
  name: string;
  address: string;
  coordinates: Coordinates;
  placeId?: string; // Google Maps Place ID
  photos?: string[]; // Photo references from Google Maps
  rating?: number; // Google Maps rating
  userRatingsTotal?: number; // Number of reviews
  priceLevel?: number; // 1-4 price level from Google Maps
}

export interface Coordinates {
  lat: number;
  lng: number;
}

export interface NodeTiming {
  startTime?: number; // milliseconds since epoch
  endTime?: number; // milliseconds since epoch
  durationMin?: number;
}

export interface NodeCost {
  amountPerPerson: number; // CRITICAL: Must match backend field name
  currency: string;
}

export interface NodeDetails {
  rating?: number;
  category?: string;
  tags?: string[];
  timeSlots?: TimeSlot[];
  googleMapsUri?: string;
}

export interface TimeSlot {
  start: string; // HH:MM
  end: string; // HH:MM
  available: boolean;
}

export interface NodeTips {
  bestTime?: string[];
  travel?: string[];
  warnings?: string[];
}

export interface NodeLinks {
  booking?: string;
  website?: string;
  phone?: string;
}

export interface Edge {
  from: string; // node ID
  to: string; // node ID
  transitInfo?: TransitInfo;
}

export interface TransitInfo {
  mode: string; // "walking", "taxi", "bus", "train", etc.
  durationMin?: number;
  provider?: string;
  bookingUrl?: string;
}

// Removed obsolete interfaces - fields are now directly on NormalizedDay

export interface ItinerarySettings {
  autoApply: boolean;
  defaultScope: string; // "trip" | "day"
}

export interface AgentStatus {
  lastRunAt?: number; // milliseconds since epoch
  status: string; // "idle" | "running" | "completed" | "failed"
}

// ChangeSet types for the new API
export interface ChangeSet {
  scope: string; // "trip" | "day"
  day?: number;
  ops: ChangeOperation[];
  preferences?: ChangePreferences;
}

export interface ChangeOperation {
  op: string; // "move" | "insert" | "delete"
  id?: string; // node ID
  after?: string; // node ID to insert after
  node?: NormalizedNode; // for insert operations
  startTime?: string; // ISO string for move operations
  endTime?: string; // ISO string for move operations
}

export interface ChangePreferences {
  userFirst: boolean;
  autoApply: boolean;
  respectLocks: boolean;
}

export interface ItineraryDiff {
  added: DiffItem[];
  removed: DiffItem[];
  updated: DiffItem[];
}

export interface DiffItem {
  id: string;
  day: number;
  fields: Record<string, any>;
}

export interface PatchEvent {
  type: string;
  itineraryId: string;
  fromVersion: number;
  toVersion: number;
  diff: ItineraryDiff;
  summary: string;
  updatedBy: string;
}

// API Request/Response types
export interface ProposeResponse {
  proposed: NormalizedItinerary;
  diff: ItineraryDiff;
  previewVersion: number;
}

export interface ApplyRequest {
  changeSetId?: string;
  changeSet: ChangeSet;
}

export interface ApplyResponse {
  toVersion: number;
  diff: ItineraryDiff;
}

export interface UndoRequest {
  toVersion?: number;
}

export interface UndoResponse {
  toVersion: number;
  diff: ItineraryDiff;
}

// Agent API types
export interface AgentRunRequest {
  itineraryId: string;
  request: CreateItineraryRequest;
}

export interface AgentRunResponse {
  itineraryId: string;
  status: string;
  message: string;
}

export interface ProcessRequestRequest {
  itineraryId: string;
  request: string;
}

export interface ProcessRequestResponse {
  itineraryId: string;
  status: string;
  message: string;
}

export interface ApplyWithEnrichmentRequest {
  itineraryId: string;
  changeSet: ChangeSet;
}

export interface ApplyWithEnrichmentResponse {
  itineraryId: string;
  status: string;
  message: string;
}

// Booking API types
export interface MockBookingRequest {
  itineraryId: string;
  nodeId: string;
  bookingRef?: string;
}

export interface MockBookingResponse {
  itineraryId: string;
  nodeId: string;
  bookingRef: string;
  locked: boolean;
  message: string;
}

// Budget tracking types
export interface BudgetSummary {
  totalCostPerPerson: number;
  totalCostForParty: number;
  budgetMin?: number;
  budgetMax?: number;
  currency: string;
  partySize: number;
  warnings: string[];
  percentageUsed: number;
  dayBreakdown: DayBudget[];
  categoryBreakdown: CategoryBudget;
}

export interface DayBudget {
  dayNumber: number;
  costPerPerson: number;
  nodeCount: number;
}

export interface CategoryBudget {
  attractions: number;
  meals: number;
  transport: number;
  accommodation: number;
  other: number;
}

// Map-related types
export interface MapBounds {
  south: number;
  west: number;
  north: number;
  east: number;
}

// Legacy types for backward compatibility
export interface CreateItineraryRequest {
  destination: string;
  startLocation: string;
  startDate: string;
  endDate: string;
  party: {
    adults: number;
    children: number;
    infants: number;
    rooms: number;
  };
  budgetTier: string;
  budgetMin?: number; // Minimum budget per person
  budgetMax?: number; // Maximum budget per person
  interests: string[];
  constraints: string[];
  language: string;
}
