/**
 * API Service
 * Centralized API client for backend communication
 */

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';

interface ApiError {
  message: string;
  status: number;
}

class ApiClient {
  private baseUrl: string;

  constructor(baseUrl: string) {
    this.baseUrl = baseUrl;
  }

  private async request<T>(
    endpoint: string,
    options: RequestInit = {}
  ): Promise<T> {
    const url = `${this.baseUrl}${endpoint}`;
    
    const config: RequestInit = {
      ...options,
      headers: {
        'Content-Type': 'application/json',
        ...options.headers,
      },
    };

    try {
      const response = await fetch(url, config);

      if (!response.ok) {
        const error: ApiError = {
          message: `API Error: ${response.statusText}`,
          status: response.status,
        };
        throw error;
      }

      return await response.json();
    } catch (error) {
      console.error('API request failed:', error);
      throw error;
    }
  }

  // GET request
  async get<T>(endpoint: string): Promise<T> {
    return this.request<T>(endpoint, { method: 'GET' });
  }

  // POST request
  async post<T>(endpoint: string, data: any): Promise<T> {
    return this.request<T>(endpoint, {
      method: 'POST',
      body: JSON.stringify(data),
    });
  }

  // PUT request
  async put<T>(endpoint: string, data: any): Promise<T> {
    return this.request<T>(endpoint, {
      method: 'PUT',
      body: JSON.stringify(data),
    });
  }

  // DELETE request
  async delete<T>(endpoint: string): Promise<T> {
    return this.request<T>(endpoint, { method: 'DELETE' });
  }
}

export const api = new ApiClient(API_BASE_URL);

// API Endpoints (matching backend Spring Boot controllers)
export const endpoints = {
  // Itineraries (/api/v1/itineraries)
  createItinerary: '/v1/itineraries',
  listItineraries: '/v1/itineraries',
  getItinerary: (id: string) => `/v1/itineraries/${id}`,
  getItineraryJson: (id: string) => `/v1/itineraries/${id}/json`,
  updateItinerary: (id: string) => `/v1/itineraries/${id}`,
  deleteItinerary: (id: string) => `/v1/itineraries/${id}`,
  chatWithItinerary: (id: string) => `/v1/itineraries/${id}/chat`,

  // Bookings (/api/v1)
  createRazorpayOrder: '/v1/payments/razorpay/order',
  razorpayWebhook: '/v1/payments/razorpay/webhook',
  executeProviderBooking: (vertical: string, provider: string) => 
    `/v1/providers/${vertical}/${provider}:book`,

  // Export (/api/v1/export)
  exportPdf: (id: string) => `/v1/export/${id}/pdf`,

  // WebSocket
  websocketUrl: API_BASE_URL.replace(/^http/, 'ws').replace('/api', '') + '/ws',
  itineraryTopic: (executionId: string) => `/topic/itinerary/${executionId}`,

  // Health
  health: '/v1/health',
  ping: '/v1/ping',
};

// Import types from dto.ts
export type {
  CreateItineraryReq,
  ItineraryCreationResponse,
  ItineraryListItem,
  ItineraryJson,
  NormalizedNode,
  DayItinerary,
  ProviderBookReq,
  BookingRes,
  AgentProgressEvent,
  ChatRequest,
  ChatResponse,
} from '../types/dto';
