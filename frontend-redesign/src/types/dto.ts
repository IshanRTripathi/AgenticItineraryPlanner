/**
 * Data Transfer Objects (DTOs)
 * Type definitions matching backend Spring Boot DTOs
 */

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

export interface NormalizedNode {
  id: string;
  type: 'attraction' | 'meal' | 'hotel' | 'transit' | 'activity';
  title: string;
  description?: string;
  location?: Location;
  timing?: Timing;
  cost?: Cost;
  bookingRef?: string;
  bookingStatus?: 'pending' | 'confirmed' | 'cancelled';
  provider?: string;
  metadata?: Record<string, any>;
  locked?: boolean;
}

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
