/**
 * API Client for communicating with the backend
 */

import { DataTransformer } from './dataTransformer';
import { NormalizedDataTransformer } from './normalizedDataTransformer';
import { authService } from './authService';
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
import { logger } from '../utils/logger';

const API_BASE_URL = (import.meta as any).env?.VITE_API_BASE_URL || 'http://localhost:8080/api/v1';

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
  private pendingRequests: Map<string, Promise<any>> = new Map();

  constructor(baseUrl: string = API_BASE_URL) {
    this.baseUrl = baseUrl;
  }

  setAuthToken(token: string) {
    this.authToken = token;
  }

  clearAuthToken() {
    this.authToken = null;
  }

  /**
   * Refresh the auth token and update the API client
   */
  private async refreshAuthToken(): Promise<boolean> {
    try {
      const newToken = await authService.getIdTokenForceRefresh();
      if (newToken) {
        this.authToken = newToken;
        logger.info('Auth token refreshed successfully', { component: 'ApiClient', action: 'token_refresh' });
        return true;
      }
    } catch (error) {
      logger.error('Failed to refresh auth token', { component: 'ApiClient', action: 'token_refresh_failed' }, error);
    }
    return false;
  }

  /**
   * Ensure the auth token is valid before making a request
   * Proactively refreshes token if it's expiring soon (within 5 minutes)
   */
  private async ensureValidToken(): Promise<boolean> {
    if (!this.authToken) {
      return false;
    }

    try {
      // Decode JWT to check expiry (without verification)
      const tokenParts = this.authToken.split('.');
      if (tokenParts.length !== 3) {
        logger.warn('Invalid token format', { component: 'ApiClient', action: 'token_validation' });
        return false;
      }

      const payload = JSON.parse(atob(tokenParts[1]));
      const expiryTime = payload.exp * 1000; // Convert to milliseconds
      const now = Date.now();
      const fiveMinutes = 5 * 60 * 1000;

      // If token expires within 5 minutes, refresh it proactively
      if (expiryTime - now < fiveMinutes) {
        logger.info('Token expiring soon, refreshing proactively', { 
          component: 'ApiClient', 
          action: 'token_proactive_refresh',
          expiresIn: expiryTime - now
        });
        const refreshed = await this.refreshAuthToken();
        if (!refreshed) {
          logger.warn('Token refresh failed, continuing with current token', { 
            component: 'ApiClient', 
            action: 'token_refresh_fallback' 
          });
          // Don't clear token - let the request try with current token
          // If it fails with 401, the retry logic will handle it
        }
        return refreshed;
      }

      return true;
    } catch (error) {
      logger.error('Error checking token expiry', { component: 'ApiClient', action: 'token_expiry_check' }, error);
      // If we can't decode the token, assume it's valid and let the server reject it if needed
      return true;
    }
  }

  private async request<T>(
    endpoint: string,
    options: RequestInit = {},
    retryOptions: { maxRetries?: number; retryDelay?: number } = {}
  ): Promise<T> {
    const { maxRetries = 3, retryDelay = 1000 } = retryOptions;
    const url = `${this.baseUrl}${endpoint}`;

    // Ensure token is valid before making request
    await this.ensureValidToken();

    // Create a unique key for this request to enable deduplication
    const requestKey = `${options.method || 'GET'}:${endpoint}`;

    // Check if there's already a pending request for this endpoint
    if (this.pendingRequests.has(requestKey)) {
      logger.debug('Deduplicating request', { 
        component: 'ApiClient', 
        action: 'request_deduplication',
        requestKey 
      });
      return this.pendingRequests.get(requestKey)!;
    }

    logger.debug('API Request starting', {
      component: 'ApiClient',
      action: 'api_request_start',
      method: options.method || 'GET',
      endpoint,
      maxRetries
    });

    // Create the request promise and store it for deduplication
    const requestPromise = this.executeRequest<T>(url, options, maxRetries, retryDelay);
    this.pendingRequests.set(requestKey, requestPromise);

    try {
      const result = await requestPromise;
      return result;
    } finally {
      // Clean up the pending request
      this.pendingRequests.delete(requestKey);
    }
  }

  private async executeRequest<T>(
    url: string,
    options: RequestInit,
    maxRetries: number,
    retryDelay: number
  ): Promise<T> {
    let lastError: Error;

    for (let attempt = 0; attempt <= maxRetries; attempt++) {
      try {
        // Recreate headers on each attempt to ensure fresh token
        const headers: HeadersInit = {
          'Content-Type': 'application/json',
          ...options.headers,
        };

        // Add Authorization header if auth token is available
        if (this.authToken) {
          headers['Authorization'] = `Bearer ${this.authToken}`;
        }

        const config: RequestInit = {
          ...options,
          headers,
          mode: 'cors',
          credentials: 'include', // Include credentials for authentication
        };

        // Create abort controller for timeout (150 seconds to match backend)
        const controller = new AbortController();
        const timeoutId = setTimeout(() => controller.abort(), 150000);
        
        const response = await fetch(url, {
          ...config,
          signal: controller.signal
        });
        
        clearTimeout(timeoutId);

        logger.debug('API Response received', {
          component: 'ApiClient',
          action: 'api_response',
          status: response.status,
          statusText: response.statusText,
          attempt: attempt + 1
        });

        if (!response.ok) {
          const errorText = await response.text();
          logger.error('API Error Response', {
            component: 'ApiClient',
            action: 'api_error_response',
            status: response.status,
            url
          }, { errorText });

          let errorData: ApiError;
          try {
            errorData = JSON.parse(errorText);
          } catch {
            errorData = {
              code: 'UNKNOWN_ERROR',
              message: `HTTP ${response.status}: ${response.statusText}`,
              timestamp: new Date().toISOString(),
              path: url
            };
          }

          const error = new Error(`API Error: ${errorData.message} (${response.status})`);

          // Handle token expiration (401 Unauthorized)
          if (response.status === 401 && this.authToken) {
            logger.warn('Token expired, attempting to refresh', {
              component: 'ApiClient',
              action: 'token_expired_401',
              attempt: attempt + 1
            });
            const tokenRefreshed = await this.refreshAuthToken();
            if (tokenRefreshed) {
              logger.info('Token refreshed successfully, retrying request', {
                component: 'ApiClient',
                action: 'token_refresh_retry'
              });
              continue; // Retry the request with the new token (headers will be recreated)
            } else {
              logger.error('Failed to refresh token on 401', {
                component: 'ApiClient',
                action: 'token_refresh_failed_401'
              });
              // DON'T clear token - keep trying with current token
              // User may need to re-authenticate, but don't force it immediately
              if (attempt === maxRetries) {
                throw new Error('Authentication failed - please sign in again');
              }
              // Continue to retry logic below
            }
          }

          // Don't retry on client errors (4xx) except 401 (handled above), 408, 429
          if (response.status >= 400 && response.status < 500 &&
            response.status !== 401 && response.status !== 408 && response.status !== 429) {
            throw error;
          }

          // Don't retry on last attempt
          if (attempt === maxRetries) {
            throw error;
          }

          lastError = error;
          logger.info('Retrying request', {
            component: 'ApiClient',
            action: 'api_retry',
            retryDelay,
            attempt: attempt + 1,
            maxRetries: maxRetries + 1
          });
          await this.delay(retryDelay * Math.pow(2, attempt)); // Exponential backoff
          continue;
        }

        // Handle empty responses (like 204 No Content)
        if (response.status === 204) {
          return {} as T;
        }

        const responseData = await response.json();
        logger.debug('API Response Data received', {
          component: 'ApiClient',
          action: 'api_response_data',
          dataSize: JSON.stringify(responseData).length
        });

        return responseData;
      } catch (error) {
        logger.error('API request failed', {
          component: 'ApiClient',
          action: 'api_request_failed',
          url,
          attempt: attempt + 1,
          errorMessage: error.message
        }, error);

        // Don't retry on last attempt
        if (attempt === maxRetries) {
          throw error;
        }

        // Don't retry on network errors that are likely permanent
        if (error.name === 'TypeError' && error.message.includes('Failed to fetch')) {
          // This is likely a network error, retry with exponential backoff
          lastError = error;
          logger.warn('Network error, retrying', {
            component: 'ApiClient',
            action: 'network_error_retry',
            retryDelay,
            attempt: attempt + 1,
            maxRetries: maxRetries + 1
          });
          await this.delay(retryDelay * Math.pow(2, attempt));
          continue;
        }

        // For other errors, don't retry
        throw error;
      }
    }

    throw lastError!;
  }

  private delay(ms: number): Promise<void> {
    return new Promise(resolve => setTimeout(resolve, ms));
  }

  // Itinerary endpoints
  async createItinerary(data: CreateItineraryRequest, retryOptions?: { maxRetries?: number; retryDelay?: number }): Promise<ItineraryCreationResponse> {
    const response = await this.request<ItineraryCreationResponse>('/itineraries', {
      method: 'POST',
      body: JSON.stringify(data),
    }, retryOptions);
    
    // Validate response
    if (!response.itinerary) {
      throw new Error('Invalid response: missing itinerary data');
    }
    
    logger.info('Itinerary creation response received', {
      component: 'ApiClient',
      action: 'itinerary_created',
      itineraryId: response.itinerary.id,
      executionId: response.executionId,
      status: response.status
    });
    
    return response;
  }

  async getItinerary(id: string, retryOptions?: { maxRetries?: number; retryDelay?: number }): Promise<TripData> {
    const timer = logger.startTimer('getItinerary', { component: 'ApiClient', itineraryId: id });
    
    try {
      logger.debug('Getting itinerary', { component: 'ApiClient', action: 'get_itinerary_start', itineraryId: id });

      const response = await this.request<NormalizedItinerary>(`/itineraries/${id}/json`, {}, retryOptions);
      
      logger.debug('Itinerary data received', {
        component: 'ApiClient',
        action: 'get_itinerary_received',
        itineraryId: id,
        daysCount: response.days?.length || 0
      });

      logger.debug('Starting data transformation', { component: 'ApiClient', action: 'transform_start', itineraryId: id });
      const transformedData = NormalizedDataTransformer.transformNormalizedItineraryToTripData(response);
      
      logger.info('Itinerary retrieved successfully', {
        component: 'ApiClient',
        action: 'get_itinerary_success',
        itineraryId: id,
        daysCount: transformedData.itinerary?.days?.length || 0
      });
      
      timer();
      return transformedData;

    } catch (error) {
      logger.error('Failed to get itinerary', {
        component: 'ApiClient',
        action: 'get_itinerary_error',
        itineraryId: id,
        errorMessage: error.message,
        is404: error.message.includes('404')
      }, error);

      // If it's a 404 error, it might be that the itinerary is still being generated
      // Just throw the error - let the calling code handle it appropriately
      if (error.message.includes('404')) {
        logger.debug('404 error - itinerary might still be generating', {
          component: 'ApiClient',
          action: 'get_itinerary_404',
          itineraryId: id
        });
        throw error;
      }

      timer();
      throw error;
    }
  }


  async getPublicItinerary(id: string): Promise<ItineraryResponse> {
    return this.request<ItineraryResponse>(`/itineraries/${id}/public`);
  }

  async getUserItineraries(page = 0, size = 20): Promise<ItineraryResponse[]> {
    return this.request<ItineraryResponse[]>(`/itineraries?page=${page}&size=${size}`);
  }

  async getAllItineraries(retryOptions?: { maxRetries?: number; retryDelay?: number }): Promise<TripData[]> {
    const responses = await this.request<ItineraryResponse[]>('/itineraries', {}, retryOptions);
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

  async deleteItinerary(id: string, retryOptions?: { maxRetries?: number; retryDelay?: number }): Promise<void> {
    return this.request<void>(`/itineraries/${id}`, {
      method: 'DELETE',
    }, retryOptions);
  }

  // Agent SSE stream
  createAgentEventStream(itineraryId: string): EventSource {
    let url = `${this.baseUrl}/agents/events/${itineraryId}`;

    // Add auth token as query parameter since EventSource doesn't support headers
    if (this.authToken) {
      url += `?token=${encodeURIComponent(this.authToken)}`;
    }

    logger.info('Creating SSE connection', {
      component: 'ApiClient',
      action: 'sse_create',
      itineraryId,
      hasToken: !!this.authToken
    });

    const eventSource = new EventSource(url);

    eventSource.onerror = (error) => {
      logger.error('SSE connection error', {
        component: 'ApiClient',
        action: 'sse_error',
        itineraryId
      }, error);
      // Note: EventSource doesn't support token refresh. If token expires during SSE,
      // the connection will fail and need to be recreated with a fresh token.
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

  async undoChanges(id: string, request?: UndoRequest): Promise<UndoResponse> {
    return this.request<UndoResponse>(`/itineraries/${id}:undo`, {
      method: 'POST',
      body: request ? JSON.stringify(request) : JSON.stringify({}),
    });
  }

  async redoChanges(id: string): Promise<UndoResponse> {
    return this.request<UndoResponse>(`/itineraries/${id}:redo`, {
      method: 'POST',
      body: JSON.stringify({}),
    });
  }

  async getRevisions(id: string): Promise<{ revisions: any[] }> {
    return this.request<{ revisions: any[] }>(`/itineraries/${id}/revisions`, {
      method: 'GET',
    });
  }

  async rollbackToVersion(id: string, version: number): Promise<any> {
    return this.request(`/itineraries/${id}/revisions/${version}/rollback`, {
      method: 'POST',
      body: JSON.stringify({}),
    });
  }

  async getRevisionDetail(id: string, revisionId: string): Promise<any> {
    return this.request(`/itineraries/${id}/revisions/${revisionId}`, {
      method: 'GET',
    });
  }

  // Workflow sync endpoints
  async updateWorkflowPositions(itineraryId: string, positions: Array<{ nodeId: string; x: number; y: number }>): Promise<any> {
    try {
      return await this.request(`/itineraries/${itineraryId}/workflow`, {
        method: 'PUT',
        body: JSON.stringify({ positions }),
      });
    } catch (error: any) {
      logger.error('Failed to update workflow positions', {
        component: 'ApiClient',
        action: 'update_workflow_positions_failed',
        itineraryId,
        positionsCount: positions.length
      }, error);
      if (error.message?.includes('401') || error.message?.includes('Unauthorized')) {
        throw new Error('Authentication failed. Please sign in again.');
      }
      throw error;
    }
  }

  async updateNodeData(itineraryId: string, nodeId: string, data: any): Promise<any> {
    try {
      return await this.request(`/itineraries/${itineraryId}/nodes/${nodeId}`, {
        method: 'PUT',
        body: JSON.stringify(data),
      });
    } catch (error: any) {
      logger.error('Failed to update node data', {
        component: 'ApiClient',
        action: 'update_node_data_failed',
        itineraryId,
        nodeId
      }, error);
      if (error.message?.includes('401') || error.message?.includes('Unauthorized')) {
        throw new Error('Authentication failed. Please sign in again.');
      }
      throw error;
    }
  }

  // Lock endpoints
  async toggleNodeLock(itineraryId: string, nodeId: string, locked: boolean): Promise<{ success: boolean; message?: string }> {
    return this.request(`/itineraries/${itineraryId}/nodes/${nodeId}/lock`, {
      method: 'PUT',
      body: JSON.stringify({ locked }),
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
  createPatchesEventStream(itineraryId: string, executionId?: string): EventSource {
    let url = `${this.baseUrl}/itineraries/patches?itineraryId=${itineraryId}`;
    if (executionId) {
      url += `&executionId=${executionId}`;
    }
    
    // Add auth token as query parameter since EventSource doesn't support headers
    if (this.authToken) {
      url += `&token=${encodeURIComponent(this.authToken)}`;
    }
    
    logger.info('Creating patches SSE connection', {
      component: 'ApiClient',
      action: 'patches_sse_create',
      itineraryId,
      executionId,
      hasToken: !!this.authToken
    });
    
    const eventSource = new EventSource(url);

    eventSource.onerror = (error) => {
      logger.error('Patches SSE connection error', {
        component: 'ApiClient',
        action: 'patches_sse_error',
        itineraryId,
        executionId
      }, error);
      // Note: EventSource doesn't support token refresh. If token expires during SSE,
      // the connection will fail and need to be recreated with a fresh token.
    };

    return eventSource;
  }

  async getLockStates(itineraryId: string): Promise<Record<string, boolean>> {
    return this.request<Record<string, boolean>>(`/itineraries/${itineraryId}/lock-states`);
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
  interests: string[];
  constraints: string[];
  language: string;
}

export interface ItineraryCreationResponse {
  itinerary: ItineraryResponse;
  executionId?: string;
  sseEndpoint?: string;
  estimatedCompletion?: string;
  status?: 'generating' | 'complete' | 'failed';
  stages?: Array<{
    name: string;
    status: string;
    progress: number;
  }>;
  errorMessage?: string;
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
  status: 'queued' | 'running' | 'completed' | 'failed';
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

