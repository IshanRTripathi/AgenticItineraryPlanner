/**
 * API service for the unified itinerary system
 */

import { NormalizedItinerary } from '../types/NormalizedItinerary';
import { TripData } from '../types/TripData';
import { ChatRequest, ChatResponse } from '../types/ChatTypes';
import { logger, logInfo, logError, logWarn } from '../utils/logger';
import { convertNormalizedToTripData } from '../utils/normalizedToTripDataAdapter';
import { isNormalizedItinerary } from '../utils/typeGuards';

// Retry configuration
interface RetryConfig {
  maxRetries: number;
  baseDelay: number;
  maxDelay: number;
  backoffMultiplier: number;
  retryableStatusCodes: number[];
}

const DEFAULT_RETRY_CONFIG: RetryConfig = {
  maxRetries: 3,
  baseDelay: 1000, // 1 second
  maxDelay: 10000, // 10 seconds
  backoffMultiplier: 2,
  retryableStatusCodes: [404, 408, 429, 500, 502, 503, 504]
};

// User-friendly error messages
interface UserFriendlyError extends Error {
  userMessage: string;
  originalError: Error;
  isRetryable: boolean;
  suggestedAction?: string;
}

const ERROR_MESSAGES = {
  NETWORK_ERROR: 'Unable to connect to the server. Please check your internet connection and try again.',
  ITINERARY_NOT_FOUND: 'Your itinerary is still being generated. Please wait a moment and try again.',
  ITINERARY_GENERATION_IN_PROGRESS: 'Your itinerary is being created by our AI agents. This usually takes 30-60 seconds.',
  SERVER_ERROR: 'Our servers are experiencing issues. Please try again in a few moments.',
  TIMEOUT_ERROR: 'The request took too long to complete. Please try again.',
  PERMISSION_DENIED: 'You don\'t have permission to access this itinerary.',
  RATE_LIMITED: 'Too many requests. Please wait a moment before trying again.',
  VALIDATION_ERROR: 'The information provided is invalid. Please check your input and try again.',
  UNKNOWN_ERROR: 'Something went wrong. Please try again or contact support if the problem persists.'
};

// Define RevisionInfo locally for now
export interface RevisionInfo {
  id: string;
  itineraryId: string;
  version: string;
  description: string;
  createdAt: Date;
  createdBy: string;
  changes: any[];
}

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/v1';

class ApiService {
  private baseUrl: string;
  private defaultHeaders: HeadersInit;
  private retryConfig: RetryConfig;
  private authToken: string | null = null;

  constructor(baseUrl: string = API_BASE_URL, retryConfig: RetryConfig = DEFAULT_RETRY_CONFIG) {
    this.baseUrl = baseUrl;
    this.defaultHeaders = {
      'Content-Type': 'application/json',
    };
    this.retryConfig = retryConfig;
  }

  /**
   * Set authentication token for API requests
   */
  setAuthToken(token: string | null): void {
    this.authToken = token;
    logInfo(`Auth token ${token ? 'set' : 'cleared'} in ApiService`);
  }

  /**
   * Get current auth token
   */
  getAuthToken(): string | null {
    return this.authToken;
  }

  /**
   * Sleep for a specified number of milliseconds
   */
  private sleep(ms: number): Promise<void> {
    return new Promise(resolve => setTimeout(resolve, ms));
  }

  /**
   * Calculate exponential backoff delay
   */
  private calculateDelay(attempt: number): number {
    const delay = this.retryConfig.baseDelay * Math.pow(this.retryConfig.backoffMultiplier, attempt);
    return Math.min(delay, this.retryConfig.maxDelay);
  }

  /**
   * Check if an error is retryable
   */
  private isRetryableError(error: any): boolean {
    if (error instanceof TypeError && error.message.includes('fetch')) {
      // Network errors are retryable
      return true;
    }

    if (error.message && error.message.includes('HTTP ')) {
      const statusMatch = error.message.match(/HTTP (\d+):/);
      if (statusMatch) {
        const status = parseInt(statusMatch[1]);
        return this.retryConfig.retryableStatusCodes.includes(status);
      }
    }

    return false;
  }

  /**
   * Create user-friendly error from technical error
   */
  private createUserFriendlyError(error: Error, context?: string): UserFriendlyError {
    let userMessage = ERROR_MESSAGES.UNKNOWN_ERROR;
    let suggestedAction: string | undefined;
    let isRetryable = false;

    // Network errors
    if (error instanceof TypeError && error.message.includes('fetch')) {
      userMessage = ERROR_MESSAGES.NETWORK_ERROR;
      suggestedAction = 'Check your internet connection and refresh the page';
      isRetryable = true;
    }
    // HTTP errors
    else if (error.message.includes('HTTP ')) {
      const statusMatch = error.message.match(/HTTP (\d+):/);
      if (statusMatch) {
        const status = parseInt(statusMatch[1]);

        switch (status) {
          case 404:
            if (context === 'itinerary') {
              userMessage = ERROR_MESSAGES.ITINERARY_GENERATION_IN_PROGRESS;
              suggestedAction = 'Wait 30-60 seconds and refresh the page';
              isRetryable = true;
            } else {
              userMessage = ERROR_MESSAGES.ITINERARY_NOT_FOUND;
              suggestedAction = 'Try refreshing the page or check the URL';
              isRetryable = true;
            }
            break;
          case 403:
            userMessage = ERROR_MESSAGES.PERMISSION_DENIED;
            suggestedAction = 'Make sure you have access to this itinerary';
            break;
          case 408:
            userMessage = ERROR_MESSAGES.TIMEOUT_ERROR;
            suggestedAction = 'Try again with a stable internet connection';
            isRetryable = true;
            break;
          case 429:
            userMessage = ERROR_MESSAGES.RATE_LIMITED;
            suggestedAction = 'Wait a few seconds before trying again';
            isRetryable = true;
            break;
          case 400:
            userMessage = ERROR_MESSAGES.VALIDATION_ERROR;
            suggestedAction = 'Check your input and try again';
            break;
          case 500:
          case 502:
          case 503:
          case 504:
            userMessage = ERROR_MESSAGES.SERVER_ERROR;
            suggestedAction = 'Try again in a few minutes';
            isRetryable = true;
            break;
        }
      }
    }

    const userFriendlyError = new Error(userMessage) as UserFriendlyError;
    userFriendlyError.userMessage = userMessage;
    userFriendlyError.originalError = error;
    userFriendlyError.isRetryable = isRetryable;
    userFriendlyError.suggestedAction = suggestedAction;

    return userFriendlyError;
  }

  private async request<T>(
    endpoint: string,
    options: RequestInit = {}
  ): Promise<T> {
    return this.requestWithRetry<T>(endpoint, options);
  }

  /**
   * Make HTTP request with exponential backoff retry logic
   */
  private async requestWithRetry<T>(
    endpoint: string,
    options: RequestInit = {},
    attempt: number = 0
  ): Promise<T> {
    const url = `${this.baseUrl}${endpoint}`;

    // Build headers with auth token if available
    const headers: HeadersInit = { ...this.defaultHeaders, ...options.headers };
    if (this.authToken) {
      headers['Authorization'] = `Bearer ${this.authToken}`;
    }

    const config: RequestInit = {
      ...options,
      headers,
    };

    const startTime = performance.now();
    const apiLogger = logger.logApiCall(options.method || 'GET', url, {
      component: 'ApiService',
      endpoint,
      attempt: attempt + 1,
      maxRetries: this.retryConfig.maxRetries
    });

    try {
      logInfo(`API request starting: ${options.method || 'GET'} ${endpoint} (attempt ${attempt + 1}/${this.retryConfig.maxRetries + 1})`, {
        component: 'ApiService',
        action: 'api_request_start',
        method: options.method || 'GET',
        endpoint,
        url,
        attempt: attempt + 1,
        maxRetries: this.retryConfig.maxRetries + 1
      }, {
        headers: config.headers,
        bodySize: config.body ? JSON.stringify(config.body).length : 0
      });

      const response = await fetch(url, config);
      const duration = performance.now() - startTime;

      if (!response.ok) {
        const errorData = await response.json().catch(() => ({}));
        const technicalError = new Error(errorData.message || `HTTP ${response.status}: ${response.statusText}`);

        // Log detailed error information
        logError(`API request failed with status ${response.status}`, {
          component: 'ApiService',
          action: 'api_response_error',
          status: response.status,
          statusText: response.statusText,
          endpoint,
          method: options.method || 'GET',
          errorData,
          hasAuthToken: !!this.authToken,
          authTokenPreview: this.authToken ? `${this.authToken.substring(0, 20)}...` : 'none'
        }, technicalError);

        // Check if we should retry
        if (attempt < this.retryConfig.maxRetries && this.isRetryableError(technicalError)) {
          const delay = this.calculateDelay(attempt);

          logWarn(`API request failed, retrying in ${delay}ms: ${options.method || 'GET'} ${endpoint}`, {
            component: 'ApiService',
            action: 'api_request_retry',
            method: options.method || 'GET',
            endpoint,
            attempt: attempt + 1,
            maxRetries: this.retryConfig.maxRetries,
            delay,
            status: response.status,
            retryReason: 'retryable_error'
          }, technicalError);

          await this.sleep(delay);
          return this.requestWithRetry<T>(endpoint, options, attempt + 1);
        }

        // Create user-friendly error for final failure
        const userFriendlyError = this.createUserFriendlyError(technicalError);
        apiLogger.error(userFriendlyError, duration);
        throw userFriendlyError;
      }

      const contentType = response.headers.get('content-type');
      let result: T;

      if (contentType && contentType.includes('application/json')) {
        result = await response.json();
      } else {
        result = await response.text() as unknown as T;
      }

      apiLogger.success(response, duration);

      logInfo(`API request completed: ${options.method || 'GET'} ${endpoint} (attempt ${attempt + 1})`, {
        component: 'ApiService',
        action: 'api_request_success',
        method: options.method || 'GET',
        endpoint,
        duration,
        status: response.status,
        attempt: attempt + 1,
        totalAttempts: attempt + 1
      }, {
        responseSize: JSON.stringify(result).length,
        contentType
      });

      return result;
    } catch (error) {
      const duration = performance.now() - startTime;

      // Check if we should retry
      if (attempt < this.retryConfig.maxRetries && this.isRetryableError(error)) {
        const delay = this.calculateDelay(attempt);

        logWarn(`API request failed, retrying in ${delay}ms: ${options.method || 'GET'} ${endpoint}`, {
          component: 'ApiService',
          action: 'api_request_retry',
          method: options.method || 'GET',
          endpoint,
          attempt: attempt + 1,
          maxRetries: this.retryConfig.maxRetries,
          delay,
          retryReason: 'network_error'
        }, error);

        await this.sleep(delay);
        return this.requestWithRetry<T>(endpoint, options, attempt + 1);
      }

      // Create user-friendly error for final failure
      const userFriendlyError = this.createUserFriendlyError(error as Error);
      apiLogger.error(userFriendlyError, duration);

      logError(`API request failed after ${attempt + 1} attempts: ${options.method || 'GET'} ${endpoint}`, {
        component: 'ApiService',
        action: 'api_request_error',
        method: options.method || 'GET',
        endpoint,
        duration,
        errorType: error instanceof Error ? error.constructor.name : 'Unknown',
        totalAttempts: attempt + 1,
        maxRetries: this.retryConfig.maxRetries + 1
      }, userFriendlyError);

      throw userFriendlyError;
    }
  }

  async getItinerary(itineraryId: string, retryConfig?: Partial<RetryConfig>): Promise<NormalizedItinerary> {
    const config = { ...this.retryConfig, ...retryConfig };

    try {
      logInfo(`Fetching itinerary: ${itineraryId}`, {
        component: 'ApiService',
        action: 'get_itinerary_start',
        itineraryId
      });

      // Backend returns NormalizedItinerary, return it directly (no conversion needed)
      const normalized = await this.requestWithRetryForItinerary<NormalizedItinerary>(
        `/itineraries/${itineraryId}/json`,
        { method: 'GET' },
        config
      );

      logInfo(`Successfully fetched itinerary: ${itineraryId}`, {
        component: 'ApiService',
        action: 'get_itinerary_success',
        itineraryId,
        daysCount: normalized.days?.length || 0,
        totalNodes: normalized.days?.reduce((total, day) => total + (day.nodes?.length || 0), 0) || 0
      });

      return normalized;
    } catch (error) {
      logError(`Failed to fetch itinerary: ${itineraryId}`, {
        component: 'ApiService',
        action: 'get_itinerary_error',
        itineraryId,
        errorType: error instanceof Error ? error.constructor.name : 'Unknown'
      }, error);
      throw error;
    }
  }

  /**
   * Special retry logic for itinerary requests that handles generation scenarios
   */
  private async requestWithRetryForItinerary<T>(
    endpoint: string,
    options: RequestInit = {},
    config: RetryConfig,
    attempt: number = 0
  ): Promise<T> {
    const url = `${this.baseUrl}${endpoint}`;

    // Build headers with auth token if available
    const headers: HeadersInit = { ...this.defaultHeaders, ...options.headers };
    if (this.authToken) {
      headers['Authorization'] = `Bearer ${this.authToken}`;
    }

    const requestConfig: RequestInit = {
      ...options,
      headers,
    };

    try {
      const response = await fetch(url, requestConfig);

      if (!response.ok) {
        const errorData = await response.json().catch(() => ({}));
        const error = new Error(errorData.message || `HTTP ${response.status}: ${response.statusText}`);

        // Special handling for 404 errors during itinerary generation
        if (response.status === 404 && attempt < config.maxRetries) {
          const delay = this.calculateDelay(attempt);

          logWarn(`Itinerary not ready yet, retrying in ${delay}ms (attempt ${attempt + 1}/${config.maxRetries + 1})`, {
            component: 'ApiService',
            action: 'itinerary_generation_retry',
            endpoint,
            attempt: attempt + 1,
            delay,
            status: response.status,
            retryReason: 'itinerary_generation_in_progress'
          });

          await this.sleep(delay);
          return this.requestWithRetryForItinerary<T>(endpoint, options, config, attempt + 1);
        }

        // Create user-friendly error with itinerary context
        const userFriendlyError = this.createUserFriendlyError(error, 'itinerary');
        throw userFriendlyError;
      }

      const contentType = response.headers.get('content-type');
      let result: T;

      if (contentType && contentType.includes('application/json')) {
        result = await response.json();
      } else {
        result = await response.text() as unknown as T;
      }

      return result;
    } catch (error) {
      // Check if we should retry for network errors
      if (attempt < config.maxRetries && this.isRetryableError(error)) {
        const delay = this.calculateDelay(attempt);

        logWarn(`Network error, retrying in ${delay}ms (attempt ${attempt + 1}/${config.maxRetries + 1})`, {
          component: 'ApiService',
          action: 'itinerary_network_retry',
          endpoint,
          attempt: attempt + 1,
          delay,
          retryReason: 'network_error'
        }, error);

        await this.sleep(delay);
        return this.requestWithRetryForItinerary<T>(endpoint, options, config, attempt + 1);
      }

      // Create user-friendly error with itinerary context
      const userFriendlyError = this.createUserFriendlyError(error as Error, 'itinerary');
      throw userFriendlyError;
    }
  }

  async updateItinerary(itineraryId: string, itinerary: TripData): Promise<TripData> {
    try {
      // For now, just send the itinerary as-is
      // TODO: Add proper transformation when needed
      const updatedNormalized = await this.request<NormalizedItinerary>(`/itineraries/${itineraryId}/json`, {
        method: 'PUT',
        body: JSON.stringify(itinerary),
      });
      // Transform response back to TripData
      return convertNormalizedToTripData(updatedNormalized);
    } catch (error) {
      console.error('Error updating itinerary:', error);
      throw error;
    }
  }

  async createItinerary(itinerary: Omit<TripData, 'id' | 'createdAt' | 'updatedAt'>): Promise<TripData> {
    // For now, just send the itinerary as-is
    // TODO: Add proper transformation when needed
    const createdNormalized = await this.request<NormalizedItinerary>('/itineraries', {
      method: 'POST',
      body: JSON.stringify(itinerary),
    });

    // Convert back to TripData for frontend
    return convertNormalizedToTripData(createdNormalized);
  }

  async deleteItinerary(itineraryId: string): Promise<void> {
    return this.request<void>(`/itineraries/${itineraryId}`, {
      method: 'DELETE',
    });
  }

  async toggleNodeLock(itineraryId: string, nodeId: string, locked: boolean): Promise<{ success: boolean; message: string }> {
    logInfo(`Toggling lock for node ${nodeId} in itinerary ${itineraryId}: ${locked ? 'lock' : 'unlock'}`);
    return this.request<{ success: boolean; message: string }>(`/itineraries/${itineraryId}/nodes/${nodeId}/lock`, {
      method: 'PUT',
      body: JSON.stringify({ locked }),
    });
  }

  async getLockStates(itineraryId: string): Promise<{ success: boolean; lockStates: Record<string, any> }> {
    logInfo(`Getting lock states for itinerary ${itineraryId}`);
    return this.request<{ success: boolean; lockStates: Record<string, any> }>(`/itineraries/${itineraryId}/lock-states`);
  }

  // Chat endpoints
  async sendChatMessage(itineraryId: string, request: ChatRequest): Promise<ChatResponse> {
    return this.request<ChatResponse>(`/itineraries/${itineraryId}/chat`, {
      method: 'POST',
      body: JSON.stringify(request),
    });
  }

  async getChatHistory(itineraryId: string): Promise<ChatResponse[]> {
    return this.request<ChatResponse[]>(`/itineraries/${itineraryId}/chat/history`);
  }

  async addChatHistoryMessage(itineraryId: string, message: any): Promise<any> {
    return this.request<any>(`/itineraries/${itineraryId}/chat/history`, {
      method: 'POST',
      body: JSON.stringify(message),
    });
  }

  async clearChatHistory(itineraryId: string): Promise<void> {
    return this.request<void>(`/itineraries/${itineraryId}/chat/history`, {
      method: 'DELETE',
    });
  }

  // Agent execution endpoints
  async executeAgent(itineraryId: string, agentType: string, parameters: any): Promise<any> {
    return this.request(`/itineraries/${itineraryId}/agents/${agentType}/execute`, {
      method: 'POST',
      body: JSON.stringify(parameters),
    });
  }

  async getAgentStatus(itineraryId: string, agentType: string): Promise<any> {
    return this.request(`/itineraries/${itineraryId}/agents/${agentType}/status`);
  }

  async cancelAgentExecution(itineraryId: string, agentType: string): Promise<void> {
    return this.request(`/itineraries/${itineraryId}/agents/${agentType}/cancel`, {
      method: 'POST',
    });
  }

  // Revision management endpoints
  async getRevisions(itineraryId: string): Promise<any[]> {
    return this.request(`/itineraries/${itineraryId}/revisions`);
  }

  async rollbackToRevision(itineraryId: string, revisionId: string): Promise<TripData> {
    try {
      const normalized = await this.request<NormalizedItinerary>(`/itineraries/${itineraryId}/revisions/${revisionId}/rollback`, {
        method: 'POST',
      });
      return convertNormalizedToTripData(normalized);
    } catch (error) {
      console.error('Error rolling back to revision:', error);
      throw error;
    }
  }

  async getRevision(itineraryId: string, revisionId: string): Promise<TripData> {
    try {
      const normalized = await this.request<NormalizedItinerary>(`/itineraries/${itineraryId}/revisions/${revisionId}`);
      return convertNormalizedToTripData(normalized);
    } catch (error) {
      console.error('Error fetching revision:', error);
      throw error;
    }
  }

  // Booking endpoints
  async searchHotels(request: any): Promise<any> {
    return this.request<any>('/booking/hotels/search', {
      method: 'POST',
      body: JSON.stringify(request),
    });
  }

  async searchFlights(request: any): Promise<any> {
    return this.request<any>('/booking/flights/search', {
      method: 'POST',
      body: JSON.stringify(request),
    });
  }

  async searchActivities(request: any): Promise<any> {
    return this.request<any>('/booking/activities/search', {
      method: 'POST',
      body: JSON.stringify(request),
    });
  }

  async confirmBooking(request: any): Promise<any> {
    return this.request<any>('/booking/confirm', {
      method: 'POST',
      body: JSON.stringify(request),
    });
  }

  // Payment endpoints
  async processPayment(request: any): Promise<any> {
    return this.request<any>('/payments/process', {
      method: 'POST',
      body: JSON.stringify(request),
    });
  }

  async refundPayment(paymentId: string, amount: number): Promise<any> {
    return this.request<any>(`/payments/${paymentId}/refund`, {
      method: 'POST',
      body: JSON.stringify({ amount }),
    });
  }

  // Workflow management endpoints
  async updateWorkflow(itineraryId: string, workflow: any): Promise<TripData> {
    try {
      const normalized = await this.request<NormalizedItinerary>(`/itineraries/${itineraryId}/workflow`, {
        method: 'PUT',
        body: JSON.stringify(workflow),
      });
      return convertNormalizedToTripData(normalized);
    } catch (error) {
      console.error('Error updating workflow:', error);
      throw error;
    }
  }

  async getWorkflow(itineraryId: string): Promise<any> {
    return this.request(`/itineraries/${itineraryId}/workflow`);
  }

  // Health check
  async healthCheck(): Promise<{ status: string; timestamp: string }> {
    return this.request<{ status: string; timestamp: string }>('/health');
  }

  // Generic HTTP methods for convenience
  async get<T>(endpoint: string): Promise<T> {
    return this.request<T>(endpoint, { method: 'GET' });
  }

  async post<T>(endpoint: string, data?: any): Promise<T> {
    return this.request<T>(endpoint, {
      method: 'POST',
      body: data ? JSON.stringify(data) : undefined,
    });
  }

  async put<T>(endpoint: string, data?: any): Promise<T> {
    return this.request<T>(endpoint, {
      method: 'PUT',
      body: data ? JSON.stringify(data) : undefined,
    });
  }

  async delete<T>(endpoint: string): Promise<T> {
    return this.request<T>(endpoint, { method: 'DELETE' });
  }
}

export const itineraryApi = new ApiService();

// Export for backward compatibility with existing components
export const api = itineraryApi;

// Export endpoints configuration
export const endpoints = {
  createItinerary: '/itineraries',
  getItinerary: (id: string) => `/itineraries/${id}/json`,
  getMetadata: (id: string) => `/itineraries/${id}/metadata`,
  getAllItineraries: '/itineraries',
  deleteItinerary: (id: string) => `/itineraries/${id}`,
  websocketUrl: import.meta.env.VITE_WS_BASE_URL || 'ws://localhost:8080/ws',
};