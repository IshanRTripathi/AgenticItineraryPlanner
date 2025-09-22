/**
 * Chat service for communicating with the backend chat API
 * Handles chat requests, responses, and error management
 */

import { 
  ChatRequest, 
  ChatResponse, 
  ChatOperationResult,
  DisambiguationResult,
  NodeCandidate 
} from '../types/ChatTypes';
import { safeJsonEncode, normalizeText } from '../utils/encodingUtils';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/v1';

export class ChatService {
  private static instance: ChatService;
  
  private constructor() {}
  
  public static getInstance(): ChatService {
    if (!ChatService.instance) {
      ChatService.instance = new ChatService();
    }
    return ChatService.instance;
  }

  /**
   * Send a chat message to the backend
   */
  async sendMessage(request: ChatRequest): Promise<ChatResponse> {
    try {
      // Normalize the request text to handle special characters
      const normalizedRequest = {
        ...request,
        text: normalizeText(request.text)
      };
      
      const response = await fetch(`${API_BASE_URL}/chat/route`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json; charset=UTF-8',
        },
        body: safeJsonEncode(normalizedRequest),
        mode: 'cors',
        credentials: 'omit',
      });

      if (!response.ok) {
        const errorData = await response.json().catch(() => ({}));
        throw new Error(
          `Chat API error: ${response.status} ${response.statusText} - ${
            errorData.message || 'Unknown error'
          }`
        );
      }

      const chatResponse: ChatResponse = await response.json();
      
      // Debug: Log the actual response received
      console.log('=== CHAT RESPONSE DEBUG ===');
      console.log('Response status:', response.status);
      console.log('Response headers:', Object.fromEntries(response.headers.entries()));
      console.log('Raw response:', chatResponse);
      console.log('Message field:', chatResponse.message);
      console.log('Intent field:', chatResponse.intent);
      console.log('============================');
      
      return chatResponse;
    } catch (error) {
      console.error('Chat service error:', error);
      throw error;
    }
  }

  /**
   * Process a user message and return the chat response
   */
  async processMessage(
    itineraryId: string,
    text: string,
    options: {
      scope?: 'trip' | 'day';
      day?: number;
      selectedNodeId?: string;
      autoApply?: boolean;
    } = {}
  ): Promise<ChatResponse> {
    const request: ChatRequest = {
      itineraryId,
      scope: options.scope || 'trip',
      day: options.day,
      selectedNodeId: options.selectedNodeId,
      text,
      autoApply: options.autoApply || false,
    };

    return this.sendMessage(request);
  }

  /**
   * Handle disambiguation by selecting a specific node candidate
   */
  async handleDisambiguation(
    itineraryId: string,
    originalText: string,
    selectedCandidate: NodeCandidate,
    options: {
      scope?: 'trip' | 'day';
      day?: number;
      autoApply?: boolean;
    } = {}
  ): Promise<ChatResponse> {
    // Create a new request with the selected node ID
    const request: ChatRequest = {
      itineraryId,
      scope: options.scope || 'trip',
      day: options.day,
      selectedNodeId: selectedCandidate.id,
      text: originalText,
      autoApply: options.autoApply || false,
    };

    return this.sendMessage(request);
  }

  /**
   * Apply changes from a change set
   */
  async applyChanges(
    itineraryId: string,
    changeSet: any, // ChangeSet type from backend
    options: {
      scope?: 'trip' | 'day';
      day?: number;
    } = {}
  ): Promise<ChatOperationResult> {
    try {
      const response = await fetch(`${API_BASE_URL}/itineraries/${itineraryId}:apply`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          changeSet,
          scope: options.scope || 'trip',
          day: options.day,
        }),
        mode: 'cors',
        credentials: 'omit',
      });

      if (!response.ok) {
        const errorData = await response.json().catch(() => ({}));
        throw new Error(
          `Apply changes error: ${response.status} ${response.statusText} - ${
            errorData.message || 'Unknown error'
          }`
        );
      }

      const result = await response.json();
      return {
        success: true,
        message: 'Changes applied successfully',
        changeSet: result.changeSet,
        diff: result.diff,
        warnings: result.warnings || [],
      };
    } catch (error) {
      console.error('Apply changes error:', error);
      return {
        success: false,
        message: error instanceof Error ? error.message : 'Failed to apply changes',
      };
    }
  }

  /**
   * Propose changes without applying them
   */
  async proposeChanges(
    itineraryId: string,
    changeSet: any, // ChangeSet type from backend
    options: {
      scope?: 'trip' | 'day';
      day?: number;
    } = {}
  ): Promise<ChatOperationResult> {
    try {
      const response = await fetch(`${API_BASE_URL}/itineraries/${itineraryId}:propose`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          changeSet,
          scope: options.scope || 'trip',
          day: options.day,
        }),
        mode: 'cors',
        credentials: 'omit',
      });

      if (!response.ok) {
        const errorData = await response.json().catch(() => ({}));
        throw new Error(
          `Propose changes error: ${response.status} ${response.statusText} - ${
            errorData.message || 'Unknown error'
          }`
        );
      }

      const result = await response.json();
      return {
        success: true,
        message: 'Changes proposed successfully',
        changeSet: result.changeSet,
        diff: result.diff,
        warnings: result.warnings || [],
      };
    } catch (error) {
      console.error('Propose changes error:', error);
      return {
        success: false,
        message: error instanceof Error ? error.message : 'Failed to propose changes',
      };
    }
  }

  /**
   * Undo changes to a specific version
   */
  async undoChanges(
    itineraryId: string,
    toVersion: number
  ): Promise<ChatOperationResult> {
    try {
      const response = await fetch(`${API_BASE_URL}/itineraries/${itineraryId}:undo`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          toVersion,
        }),
        mode: 'cors',
        credentials: 'omit',
      });

      if (!response.ok) {
        const errorData = await response.json().catch(() => ({}));
        throw new Error(
          `Undo changes error: ${response.status} ${response.statusText} - ${
            errorData.message || 'Unknown error'
          }`
        );
      }

      const result = await response.json();
      return {
        success: true,
        message: 'Changes undone successfully',
        diff: result.diff,
        warnings: result.warnings || [],
      };
    } catch (error) {
      console.error('Undo changes error:', error);
      return {
        success: false,
        message: error instanceof Error ? error.message : 'Failed to undo changes',
      };
    }
  }

  /**
   * Get the current itinerary JSON
   */
  async getItineraryJson(itineraryId: string): Promise<any> {
    try {
      const response = await fetch(`${API_BASE_URL}/itineraries/${itineraryId}/json`, {
        mode: 'cors',
        credentials: 'omit',
      });

      if (!response.ok) {
        const errorData = await response.json().catch(() => ({}));
        throw new Error(
          `Get itinerary error: ${response.status} ${response.statusText} - ${
            errorData.message || 'Unknown error'
          }`
        );
      }

      return await response.json();
    } catch (error) {
      console.error('Get itinerary error:', error);
      throw error;
    }
  }

  /**
   * Validate a chat request before sending
   */
  validateRequest(request: ChatRequest): { valid: boolean; errors: string[] } {
    const errors: string[] = [];

    if (!request.itineraryId || request.itineraryId.trim() === '') {
      errors.push('Itinerary ID is required');
    }

    if (!request.text || request.text.trim() === '') {
      errors.push('Message text is required');
    }

    if (request.scope === 'day' && !request.day) {
      errors.push('Day number is required when scope is "day"');
    }

    if (request.text && request.text.length > 1000) {
      errors.push('Message text is too long (max 1000 characters)');
    }

    return {
      valid: errors.length === 0,
      errors,
    };
  }

  /**
   * Parse intent from response for better user experience
   */
  parseIntent(intent: string): {
    type: string;
    description: string;
    icon: string;
  } {
    const intentMap: Record<string, { description: string; icon: string }> = {
      'REPLAN_TODAY': { description: 'Replanning your day', icon: '🔄' },
      'MOVE_TIME': { description: 'Moving activity time', icon: '⏰' },
      'INSERT_PLACE': { description: 'Adding new place', icon: '➕' },
      'DELETE_NODE': { description: 'Removing activity', icon: '🗑️' },
      'REPLACE_NODE': { description: 'Replacing activity', icon: '🔄' },
      'BOOK_NODE': { description: 'Booking activity', icon: '📅' },
      'UNDO': { description: 'Undoing changes', icon: '↩️' },
      'EXPLAIN': { description: 'Explaining itinerary', icon: '💬' },
      'DISAMBIGUATION': { description: 'Need clarification', icon: '❓' },
      'UNKNOWN': { description: 'Unknown request', icon: '❓' },
      'ERROR': { description: 'Error occurred', icon: '⚠️' },
      'WELCOME': { description: 'Welcome message', icon: '👋' },
      'Changes previewed successfully': { description: 'Changes previewed', icon: '👀' },
      'Changes applied successfully!': { description: 'Changes applied', icon: '✅' },
      'APPLY_SUCCESS': { description: 'Changes applied', icon: '✅' },
      'CANCEL': { description: 'Changes cancelled', icon: '❌' },
    };

    const intentInfo = intentMap[intent] || intentMap['UNKNOWN'];
    return {
      type: intent,
      description: intentInfo.description,
      icon: intentInfo.icon,
    };
  }
}

// Export singleton instance
export const chatService = ChatService.getInstance();
