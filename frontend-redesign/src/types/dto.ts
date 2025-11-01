/**
 * Data Transfer Objects (DTOs)
 * Type definitions matching backend Spring Boot DTOs
 */

import type { NormalizedNode } from './NormalizedItinerary';

// ============================================================================
// Itinerary DTOs
// ============================================================================

export interface CreateItineraryReq {
  destination: string;
  startDate: string; // ISO date string
  endDate?: string; // ISO date string
  adults?: number;
  children?: number;
  infants?: number;
  budget?: string; // 'budget' | 'moderate' | 'luxury'
  pace?: string; // 'relaxed' | 'moderate' | 'fast'
  interests?: string[];
  preferences?: Record<string, any>;
}

export interface ItineraryCreationResponse {
  success: boolean;
  itineraryId?: string;
  executionId?: string;
  message?: string;
  error?: string;
}

export interface ItineraryListItem {
  id: string;
  destination: string;
  startDate: string;
  endDate: string;
  status: 'draft' | 'generating' | 'completed' | 'failed';
  createdAt: string;
  updatedAt: string;
  travelers: number;
  budget?: string;
  imageUrl?: string;
}

export interface ItineraryJson {
  id: string;
  userId: string;
  destination: string;
  startDate: string;
  endDate: string;
  status: string;
  createdAt: string;
  updatedAt: string;
  metadata?: ItineraryMetadata;
  days: DayItinerary[];
  nodes: Record<string, NormalizedNode>;
}

export interface ItineraryMetadata {
  adults?: number;
  children?: number;
  infants?: number;
  budget?: string;
  pace?: string;
  interests?: string[];
  totalCost?: number;
  currency?: string;
}

export interface DayItinerary {
  day: number;
  date: string;
  title?: string;
  nodeIds: string[];
}

// NormalizedNode is defined in NormalizedItinerary.ts and imported at the top

export interface Location {
  name?: string;
  address?: string;
  city?: string;
  country?: string;
  lat?: number;
  lng?: number;
  placeId?: string;
}

export interface Timing {
  startTime?: string;
  endTime?: string;
  duration?: number; // minutes
}

export interface Cost {
  amount: number;
  currency: string;
  perPerson?: boolean;
}

// ============================================================================
// Booking DTOs
// ============================================================================

export interface ProviderBookReq {
  itineraryId: string;
  nodeId: string;
  provider: string;
  vertical: string; // 'hotel' | 'flight' | 'activity'
  bookingDetails: Record<string, any>;
}

export interface BookingRes {
  success: boolean;
  bookingId?: string;
  confirmationNumber?: string;
  provider?: string;
  status?: string;
  error?: string;
}

export interface RazorpayOrderReq {
  amount: number;
  currency: string;
  itineraryId?: string;
  nodeId?: string;
}

export interface RazorpayOrderRes {
  orderId: string;
  amount: number;
  currency: string;
  key: string;
}

// ============================================================================
// WebSocket DTOs
// ============================================================================

export interface AgentProgressEvent {
  executionId: string;
  itineraryId?: string;
  status: 'started' | 'in_progress' | 'completed' | 'failed';
  progress: number; // 0-100
  currentStep?: string;
  message?: string;
  agentName?: string;
  timestamp: string;
}

export interface ChatMessage {
  id: string;
  role: 'user' | 'assistant';
  content: string;
  timestamp: string;
  diff?: ItineraryDiff; // Optional itinerary changes
}

export interface ItineraryDiff {
  added: DiffItem[];
  removed: DiffItem[];
  updated: DiffItem[];
}

export interface DiffItem {
  nodeId: string;
  day: number | null;
  fields?: string[];
  title?: string;
}

export interface ChatRequest {
  message: string;
  itineraryId: string;
}

export interface ChatResponse {
  success: boolean;
  message?: ChatMessage;
  error?: string;
}

// ============================================================================
// Export DTOs
// ============================================================================

export interface ExportPdfRequest {
  itineraryId: string;
  format?: 'pdf' | 'docx';
}

// ============================================================================
// API Response Wrapper
// ============================================================================

export interface ApiResponse<T> {
  success: boolean;
  data?: T;
  error?: string;
  message?: string;
}

// ============================================================================
// Complete NormalizedItinerary Types (Backend Contract)
// ============================================================================

export interface NormalizedItinerary {
  itineraryId: string;
  summary: string;
  version: number;
  currency: string;
  themes: string[];
  days: NormalizedDay[];
  agents: Record<string, AgentStatus>;
  updatedAt: number;
  status: 'planning' | 'generating' | 'ready' | 'completed' | 'failed';
  userId?: string;
  destination?: string;
}

export interface NormalizedDay {
  dayNumber: number;
  date: string;
  location: string;
  nodes: NormalizedNodeComplete[];
  edges: Edge[];
  totals: DayTotals;
}

export interface NormalizedNodeComplete {
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
  address?: string;
  coordinates: Coordinates;
  placeId: string;
  googleMapsUri?: string;
  rating?: number;
  openingHours?: string;
  closingHours?: string;
}

export interface Coordinates {
  lat: number;
  lng: number;
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
