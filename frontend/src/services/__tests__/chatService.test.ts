import { describe, it, expect, vi, beforeEach } from 'vitest';
import { chatService } from '../chatService';
import type { ChatRequest, ChatResponse } from '../../types/ChatTypes';

// Mock fetch
global.fetch = vi.fn();

describe('chatService', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('sendMessage', () => {
    it('should send a chat message and return response', async () => {
      const mockRequest: ChatRequest = {
        text: 'Hello, can you help me plan my trip?',
        itineraryId: 'test-itinerary',
        scope: 'trip'
      };

      const mockResponse: ChatResponse = {
        message: 'I\'d be happy to help you plan your trip!',
        intent: {
          type: 'general',
          confidence: 0.9,
          entities: {}
        },
        suggestions: ['What would you like to do today?', 'Would you like to see some attractions?'],
        timestamp: new Date().toISOString()
      };

      (fetch as any).mockResolvedValueOnce({
        ok: true,
        json: async () => mockResponse
      });

      const result = await chatService.sendMessage(mockRequest);

      expect(fetch).toHaveBeenCalledWith('/api/v1/chat/route', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(mockRequest),
        mode: 'cors',
        credentials: 'omit'
      });

      expect(result).toEqual(mockResponse);
    });

    it('should handle API errors', async () => {
      const mockRequest: ChatRequest = {
        message: 'Test message',
        itineraryId: 'test-itinerary'
      };

      (fetch as any).mockResolvedValueOnce({
        ok: false,
        status: 500,
        statusText: 'Internal Server Error',
        json: async () => ({ error: 'Server error' })
      });

      await expect(chatService.sendMessage(mockRequest)).rejects.toThrow('Chat API error: 500 Internal Server Error - Server error');
    });

    it('should handle network errors', async () => {
      const mockRequest: ChatRequest = {
        text: 'Test message',
        itineraryId: 'test-itinerary',
        scope: 'trip'
      };

      (fetch as any).mockRejectedValueOnce(new Error('Network error'));

      await expect(chatService.sendMessage(mockRequest)).rejects.toThrow('Network error');
    });

    it('should handle intent classification response', async () => {
      const mockRequest: ChatRequest = {
        text: 'Move the museum visit to tomorrow',
        itineraryId: 'test-itinerary',
        scope: 'trip'
      };

      const mockResponse: ChatResponse = {
        message: 'I can help you move the museum visit to tomorrow.',
        intent: {
          type: 'move_node',
          confidence: 0.95,
          entities: {
            nodeId: 'museum-node',
            targetDay: 2
          }
        },
        suggestions: ['Would you like me to suggest a new time?', 'Should I update your itinerary?'],
        timestamp: new Date().toISOString()
      };

      (fetch as any).mockResolvedValueOnce({
        ok: true,
        json: async () => mockResponse
      });

      const result = await chatService.sendMessage(mockRequest);

      expect(result.intent.type).toBe('move_node');
      expect(result.intent.entities.nodeId).toBe('museum-node');
      expect(result.intent.entities.targetDay).toBe(2);
    });

    it('should handle disambiguation response', async () => {
      const mockRequest: ChatRequest = {
        text: 'Show me the restaurant',
        itineraryId: 'test-itinerary',
        scope: 'trip'
      };

      const mockResponse: ChatResponse = {
        message: 'I found multiple restaurants. Which one did you mean?',
        intent: {
          type: 'disambiguation',
          confidence: 0.8,
          entities: {
            candidates: [
              {
                id: 'restaurant-1',
                title: 'La Boqueria Restaurant',
                type: 'restaurant',
                day: 1,
                time: '12:00'
              },
              {
                id: 'restaurant-2',
                title: 'El Nacional',
                type: 'restaurant',
                day: 2,
                time: '19:00'
              }
            ]
          }
        },
        suggestions: ['La Boqueria Restaurant (Day 1, 12:00)', 'El Nacional (Day 2, 19:00)'],
        timestamp: new Date().toISOString()
      };

      (fetch as any).mockResolvedValueOnce({
        ok: true,
        json: async () => mockResponse
      });

      const result = await chatService.sendMessage(mockRequest);

      expect(result.intent.type).toBe('disambiguation');
      expect(result.intent.entities.candidates).toHaveLength(2);
      expect(result.intent.entities.candidates[0].title).toBe('La Boqueria Restaurant');
    });
  });
});
