/**
 * API Client for communicating with the backend
 */

import { DataTransformer } from './dataTransformer';
import { NormalizedDataTransformer } from './normalizedDataTransformer';
import { TripData } from '../types/TripData';
import { 
  NormalizedItinerary, 
  ChangeSet, 
  ProposeResponse, 
  ApplyRequest, 
  ApplyResponse, 
  UndoRequest, 
  UndoResponse,
  ProcessRequestRequest,
  ProcessRequestResponse,
  ApplyWithEnrichmentRequest,
  ApplyWithEnrichmentResponse,
  MockBookingRequest,
  MockBookingResponse
} from '../types/NormalizedItinerary';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/v1';

export interface ApiError {
  code: string;
  message: string;
  hint?: string;
  timestamp: string;
  path: string;
  details?: Record<string, string>;
}

class ApiClient {
  private baseUrl: string;
  private authToken: string | null = null;

  constructor(baseUrl: string = API_BASE_URL) {
    this.baseUrl = baseUrl;
  }

  setAuthToken(token: string) {
    this.authToken = token;
  }

  clearAuthToken() {
    this.authToken = null;
  }

  private async request<T>(
    endpoint: string,
    options: RequestInit = {}
  ): Promise<T> {
    const url = `${this.baseUrl}${endpoint}`;
    
    console.log('API Request:', {
      method: options.method || 'GET',
      url,
      baseUrl: this.baseUrl,
      endpoint
    });
    
    const headers: HeadersInit = {
      'Content-Type': 'application/json',
      ...options.headers,
    };

    const config: RequestInit = {
      ...options,
      headers,
      mode: 'cors',
      credentials: 'omit', // Don't send credentials for now
    };

    try {
      const response = await fetch(url, config);
      
      console.log('API Response:', {
        status: response.status,
        statusText: response.statusText,
        url: response.url
      });
      
      if (!response.ok) {
        const errorText = await response.text();
        console.error('API Error Response:', errorText);
        
        let errorData: ApiError;
        try {
          errorData = JSON.parse(errorText);
        } catch {
          errorData = {
            code: 'UNKNOWN_ERROR',
            message: `HTTP ${response.status}: ${response.statusText}`,
            timestamp: new Date().toISOString(),
            path: endpoint
          };
        }
        
        throw new Error(`API Error: ${errorData.message} (${response.status})`);
      }

      // Handle empty responses (like 204 No Content)
      if (response.status === 204) {
        return {} as T;
      }

      const responseData = await response.json();
      console.log('API Response Data:', responseData);
      
      return responseData;
    } catch (error) {
      console.error('API request failed:', {
        url,
        error: error.message,
        stack: error.stack
      });
      throw error;
    }
  }

  // Itinerary endpoints
  async createItinerary(data: CreateItineraryRequest): Promise<TripData> {
    const response = await this.request<ItineraryResponse>('/itineraries', {
      method: 'POST',
      body: JSON.stringify(data),
    });
    return DataTransformer.transformItineraryResponseToTripData(response);
  }

  async getItinerary(id: string): Promise<TripData> {
    const response = await this.request<NormalizedItinerary>(`/itineraries/${id}/json`);
    return NormalizedDataTransformer.transformNormalizedItineraryToTripData(response);
  }

  async getPublicItinerary(id: string): Promise<ItineraryResponse> {
    return this.request<ItineraryResponse>(`/itineraries/${id}/public`);
  }

  async getUserItineraries(page = 0, size = 20): Promise<ItineraryResponse[]> {
    return this.request<ItineraryResponse[]>(`/itineraries?page=${page}&size=${size}`);
  }

  async getAllItineraries(): Promise<TripData[]> {
    const responses = await this.request<ItineraryResponse[]>('/itineraries');
    return responses.map(response => DataTransformer.transformItineraryResponseToTripData(response));
  }

  async reviseItinerary(id: string, data: ReviseRequest): Promise<ReviseResponse> {
    return this.request<ReviseResponse>(`/itineraries/${id}:revise`, {
      method: 'POST',
      body: JSON.stringify(data),
    });
  }

  async extendItinerary(id: string, data: ExtendRequest): Promise<ItineraryResponse> {
    return this.request<ItineraryResponse>(`/itineraries/${id}:extend`, {
      method: 'POST',
      body: JSON.stringify(data),
    });
  }

  async saveItinerary(id: string): Promise<void> {
    return this.request<void>(`/itineraries/${id}:save`, {
      method: 'POST',
    });
  }

  async shareItinerary(id: string): Promise<ShareResponse> {
    return this.request<ShareResponse>(`/itineraries/${id}:share`, {
      method: 'POST',
    });
  }

  async deleteItinerary(id: string): Promise<void> {
    return this.request<void>(`/itineraries/${id}`, {
      method: 'DELETE',
    });
  }

  // Agent SSE stream
  createAgentEventStream(itineraryId: string): EventSource {
    const url = `${this.baseUrl}/agents/stream?itineraryId=${itineraryId}`;
    const eventSource = new EventSource(url);
    
    eventSource.onerror = (error) => {
      console.error('SSE connection error:', error);
    };

    return eventSource;
  }

  // Tools endpoints
  async generatePackingList(data: PackingListRequest): Promise<PackingListResponse> {
    return this.request<PackingListResponse>('/packing-list', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  }

  async getPhotoSpots(data: PhotoSpotsRequest): Promise<PhotoSpotsResponse> {
    return this.request<PhotoSpotsResponse>('/photo-spots', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  }

  async getMustTryFoods(data: MustTryFoodsRequest): Promise<MustTryFoodsResponse> {
    return this.request<MustTryFoodsResponse>('/must-try-foods', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  }

  async generateCostEstimate(data: CostEstimateRequest): Promise<CostEstimateResponse> {
    return this.request<CostEstimateResponse>('/cost-estimator', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  }

  // Export endpoints
  async generatePDF(itineraryId: string): Promise<Blob> {
    const response = await fetch(`${this.baseUrl}/itineraries/${itineraryId}/pdf`, {
      headers: this.authToken ? { Authorization: `Bearer ${this.authToken}` } : {},
      mode: 'cors',
      credentials: 'omit',
    });
    
    if (!response.ok) {
      throw new Error('Failed to generate PDF');
    }
    
    return response.blob();
  }

  async sendEmail(data: EmailRequest): Promise<EmailResponse> {
    return this.request<EmailResponse>('/email/send', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  }

  // Auth endpoints
  async authenticateWithGoogle(idToken: string): Promise<AuthResponse> {
    return this.request<AuthResponse>('/auth/google', {
      method: 'POST',
      body: JSON.stringify({ idToken }),
    });
  }

  async getCurrentUser(): Promise<UserInfo> {
    return this.request<UserInfo>('/auth/me');
  }

  // New normalized JSON endpoints
  async getItineraryJson(id: string): Promise<NormalizedItinerary> {
    return this.request<NormalizedItinerary>(`/itineraries/${id}/json`);
  }

  async proposeChanges(id: string, changeSet: ChangeSet): Promise<ProposeResponse> {
    return this.request<ProposeResponse>(`/itineraries/${id}:propose`, {
      method: 'POST',
      body: JSON.stringify(changeSet),
    });
  }

  async applyChanges(id: string, request: ApplyRequest): Promise<ApplyResponse> {
    return this.request<ApplyResponse>(`/itineraries/${id}:apply`, {
      method: 'POST',
      body: JSON.stringify(request),
    });
  }

  async undoChanges(id: string, request: UndoRequest): Promise<UndoResponse> {
    return this.request<UndoResponse>(`/itineraries/${id}:undo`, {
      method: 'POST',
      body: JSON.stringify(request),
    });
  }

  // Agent endpoints
  async processUserRequest(request: ProcessRequestRequest): Promise<ProcessRequestResponse> {
    return this.request<ProcessRequestResponse>('/agents/process-request', {
      method: 'POST',
      body: JSON.stringify(request),
    });
  }

  async applyWithEnrichment(request: ApplyWithEnrichmentRequest): Promise<ApplyWithEnrichmentResponse> {
    return this.request<ApplyWithEnrichmentResponse>('/agents/apply-with-enrichment', {
      method: 'POST',
      body: JSON.stringify(request),
    });
  }

  // Booking endpoints
  async mockBook(request: MockBookingRequest): Promise<MockBookingResponse> {
    return this.request<MockBookingResponse>('/book', {
      method: 'POST',
      body: JSON.stringify(request),
    });
  }

  // SSE for patches
  createPatchesEventStream(itineraryId: string): EventSource {
    const url = `${this.baseUrl}/itineraries/patches?itineraryId=${itineraryId}`;
    const eventSource = new EventSource(url);
    
    eventSource.onerror = (error) => {
      console.error('Patches SSE connection error:', error);
    };

    return eventSource;
  }

  // Test endpoints
  async ping(): Promise<unknown> {
    return this.request<unknown>('/ping');
  }

  async echo(data: unknown): Promise<unknown> {
    return this.request<unknown>('/echo', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  }
}

// Type definitions matching backend DTOs
export interface CreateItineraryRequest {
  destination: string;
  startDate: string;
  endDate: string;
  party: {
    adults: number;
    children: number;
    infants: number;
    rooms: number;
  };
  budgetTier: string;
  interests: string[];
  constraints: string[];
  language: string;
}

export interface ItineraryResponse {
  id: string;
  destination: string;
  startDate: string;
  endDate: string;
  party: {
    adults: number;
    children: number;
    infants: number;
    rooms: number;
  };
  budgetTier: string;
  interests: string[];
  constraints: string[];
  language: string;
  summary?: string;
  map?: unknown;
  days?: unknown[];
  agentResults?: {
    flights?: unknown[];
    hotels?: unknown[];
    restaurants?: unknown[];
    places?: unknown[];
    transport?: unknown[];
  };
  status: string;
  createdAt: string;
  updatedAt: string;
  isPublic: boolean;
  shareToken?: string;
}

export interface ReviseRequest {
  instructions: string;
  focusDay?: number;
}

export interface ReviseResponse {
  id: string;
  diff: {
    added: string[];
    removed: string[];
    updated: string[];
  };
  full: ItineraryResponse;
}

export interface ExtendRequest {
  days: number;
}

export interface ShareResponse {
  shareToken: string;
  publicUrl: string;
}

export interface AgentEvent {
  agentId: string;
  kind: string;
  status: 'queued' | 'running' | 'succeeded' | 'failed';
  progress?: number;
  message?: string;
  step?: string;
  updatedAt: string;
  itineraryId: string;
}

export interface PackingListRequest {
  destination: string;
  climate: string;
  season: string;
  startDate: string;
  endDate: string;
  activities: string[];
  partySize: number;
}

export interface PackingListResponse {
  items: Array<{
    name: string;
    quantity: number;
    group: string;
    essential: boolean;
    notes?: string;
  }>;
}

export interface PhotoSpotsRequest {
  destination: string;
  interests: string[];
  timeOfDay?: string;
  season?: string;
}

export interface PhotoSpotsResponse {
  spots: Array<{
    name: string;
    lat: number;
    lng: number;
    category: string;
    bestTime: string;
    tips: string;
    difficulty: string;
  }>;
}

export interface MustTryFoodsRequest {
  destination: string;
  dietaryPreferences: string[];
  budgetTier?: string;
  cuisineTypes?: string[];
}

export interface MustTryFoodsResponse {
  items: Array<{
    name: string;
    description: string;
    category: string;
    venues: string[];
    priceRange: string;
    tips: string;
  }>;
}

export interface CostEstimateRequest {
  destination: string;
  startDate: string;
  endDate: string;
  budgetTier: string;
  partySize: number;
  interests: string[];
}

export interface CostEstimateResponse {
  currency: string;
  totals: {
    transport: number;
    lodging: number;
    food: number;
    activities: number;
    shopping: number;
    misc: number;
    total: number;
  };
  perDay: {
    transport: number;
    lodging: number;
    food: number;
    activities: number;
    shopping: number;
    misc: number;
    total: number;
  };
  perPerson: {
    transport: number;
    lodging: number;
    food: number;
    activities: number;
    shopping: number;
    misc: number;
    total: number;
  };
  confidence: string;
  notes: string;
}

export interface EmailRequest {
  to: string;
  subject: string;
  itineraryId: string;
  template?: string;
  personalMessage?: string;
  includePdf: boolean;
  templateData?: Record<string, any>;
}

export interface EmailResponse {
  messageId: string;
  status: string;
  sentAt: string;
}

export interface AuthResponse {
  session: string;
  user: UserInfo;
  expiresAt: string;
}

export interface UserInfo {
  id: string;
  email: string;
  name: string;
  pictureUrl?: string;
}

// Create and export a singleton instance
export const apiClient = new ApiClient();
export default apiClient;

