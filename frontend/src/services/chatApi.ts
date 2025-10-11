export interface ChatRequest {
  itineraryId: string;
  scope: 'trip' | 'day';
  day?: number;
  selectedNodeId?: string;
  text: string;
  autoApply: boolean;
  userId?: string;
}

export interface ChatMessageDTO {
  id?: string;
  message: string;
  sender: 'user' | 'assistant';
  timestamp: number | string;
  intent?: string;
  changeSet?: any;
  diff?: any;
  applied?: boolean;
  warnings?: string[];
  errors?: string[];
  candidates?: any[];
}

export interface ApplyChangeSetRequest {
  changeSet: any;
}

export interface ApplyChangeSetResponse {
  toVersion: number;
  diff: any;
}

// Import authService to get Firebase ID token
import { authService } from './authService';

// Use the same API base URL as the main apiClient
const API_BASE_URL = (import.meta as any).env?.VITE_API_BASE_URL || 'http://localhost:8080/api/v1';

// Helper function to get authentication headers
async function getAuthHeaders(): Promise<HeadersInit> {
  const token = await authService.getIdToken();
  const headers: HeadersInit = {
    'Content-Type': 'application/json',
  };
  
  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }
  
  return headers;
}

async function safeJsonParse<T>(response: Response, allowEmpty: boolean = false): Promise<T> {
  const text = await response.text();
  
  // Handle empty responses
  if (!text || text.trim() === '') {
    if (allowEmpty) {
      return (Array.isArray([]) ? [] : {}) as T;
    }
    throw new Error('Empty response from server');
  }
  
  try {
    return JSON.parse(text) as T;
  } catch (e) {
    // Response is not JSON (likely HTML error page)
    if (text.includes('<!DOCTYPE') || text.includes('<html')) {
      const error = new Error(`Server returned HTML instead of JSON (Status: ${response.status}). The backend endpoint may not exist or returned an error page.`);
      (error as any).isHtmlResponse = true;
      throw error;
    }
    throw new Error(`Invalid JSON response: ${text.substring(0, 100)}...`);
  }
}

export const chatApi = {
  async send(itineraryId: string, req: ChatRequest): Promise<ChatMessageDTO> {
    try {
      const headers = await getAuthHeaders();
      const res = await fetch(`${API_BASE_URL}/itineraries/${itineraryId}/chat`, {
        method: 'POST',
        headers,
        credentials: 'include',
        body: JSON.stringify(req),
      });
      
      if (!res.ok) {
        const errorText = await res.text();
        if (errorText.includes('<!DOCTYPE') || errorText.includes('<html')) {
          throw new Error(`Chat endpoint not available (${res.status}). Please ensure the backend is running.`);
        }
        throw new Error(`Failed to send message (${res.status}): ${errorText || res.statusText}`);
      }
      
      return await safeJsonParse<ChatMessageDTO>(res);
    } catch (error: any) {
      console.error('[chatApi.send] Error:', error);
      throw error;
    }
  },

  async history(itineraryId: string): Promise<ChatMessageDTO[]> {
    try {
      const headers = await getAuthHeaders();
      const res = await fetch(`${API_BASE_URL}/itineraries/${itineraryId}/chat/history`, { 
        headers,
        credentials: 'include' 
      });
      
      if (!res.ok) {
        if (res.status === 404) return []; // No history yet
        const errorText = await res.text();
        if (errorText.includes('<!DOCTYPE') || errorText.includes('<html')) {
          console.warn('[chatApi.history] Endpoint returned HTML, assuming no history exists');
          return [];
        }
        throw new Error(`Failed to load history (${res.status}): ${res.statusText}`);
      }
      
      // Check content type before parsing
      const contentType = res.headers.get('content-type');
      if (contentType && !contentType.includes('application/json')) {
        console.warn('[chatApi.history] Response is not JSON (content-type:', contentType, '), assuming empty history');
        return [];
      }
      
      return await safeJsonParse<ChatMessageDTO[]>(res, true);
    } catch (error: any) {
      // Gracefully handle HTML responses (backend endpoint may not be implemented yet)
      if (error.isHtmlResponse || error.message?.includes('HTML instead of JSON')) {
        console.warn('[chatApi.history] Chat history endpoint returned HTML, assuming empty history. Backend may not have this endpoint implemented.');
        return [];
      }
      
      console.error('[chatApi.history] Error loading chat history:', error);
      throw error;
    }
  },

  async persist(itineraryId: string, msg: ChatMessageDTO): Promise<void> {
    try {
      const headers = await getAuthHeaders();
      const res = await fetch(`${API_BASE_URL}/itineraries/${itineraryId}/chat/history`, {
        method: 'POST',
        headers,
        credentials: 'include',
        body: JSON.stringify(msg),
      });
      
      if (!res.ok) {
        const errorText = await res.text();
        console.warn('[chatApi.persist] Failed to persist message:', res.status, errorText);
      }
    } catch (error: any) {
      console.error('[chatApi.persist] Error:', error);
      // Don't throw - persistence is best-effort
    }
  },

  async clear(itineraryId: string): Promise<void> {
    try {
      const headers = await getAuthHeaders();
      const res = await fetch(`${API_BASE_URL}/itineraries/${itineraryId}/chat/history`, {
        method: 'DELETE',
        headers,
        credentials: 'include',
      });
      
      if (!res.ok) {
        const errorText = await res.text();
        if (errorText.includes('<!DOCTYPE') || errorText.includes('<html')) {
          throw new Error(`Clear history endpoint not available (${res.status})`);
        }
        throw new Error(`Failed to clear history (${res.status}): ${res.statusText}`);
      }
    } catch (error: any) {
      console.error('[chatApi.clear] Error:', error);
      throw error;
    }
  },

  async applyChangeSet(itineraryId: string, request: ApplyChangeSetRequest): Promise<ApplyChangeSetResponse> {
    try {
      const headers = await getAuthHeaders();
      const res = await fetch(`${API_BASE_URL}/itineraries/${itineraryId}:apply`, {
        method: 'POST',
        headers,
        credentials: 'include',
        body: JSON.stringify(request),
      });
      
      if (!res.ok) {
        const errorText = await res.text();
        if (errorText.includes('<!DOCTYPE') || errorText.includes('<html')) {
          throw new Error(`Apply endpoint not available (${res.status}). Please ensure the backend is running.`);
        }
        throw new Error(`Failed to apply changes (${res.status}): ${errorText || res.statusText}`);
      }
      
      return await safeJsonParse<ApplyChangeSetResponse>(res);
    } catch (error: any) {
      console.error('[chatApi.applyChangeSet] Error:', error);
      throw error;
    }
  },
};


