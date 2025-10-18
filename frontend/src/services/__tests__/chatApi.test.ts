import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { chatApi } from '../chatApi';

describe('chatApi', () => {
  const mockItineraryId = 'test-itinerary-123';
  
  beforeEach(() => {
    global.fetch = vi.fn();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  describe('send', () => {
    it('should send a chat message successfully', async () => {
      const mockRequest = {
        itineraryId: mockItineraryId,
        scope: 'trip' as const,
        text: 'Move lunch to 2pm',
        autoApply: false,
      };

      const mockResponse = {
        id: 'msg-123',
        message: "I'll move your lunch",
        sender: 'assistant' as const,
        timestamp: Date.now(),
        intent: 'MODIFY_ACTIVITY',
      };

      (global.fetch as any).mockResolvedValue({
        ok: true,
        text: async () => JSON.stringify(mockResponse),
        json: async () => mockResponse,
      });

      const result = await chatApi.send(mockItineraryId, mockRequest);

      expect(result).toEqual(mockResponse);
      expect(global.fetch).toHaveBeenCalledWith(
        `/api/v1/itineraries/${mockItineraryId}/chat`,
        expect.objectContaining({
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          credentials: 'include',
        })
      );
    });

    it('should throw error on 404', async () => {
      (global.fetch as any).mockResolvedValue({
        ok: false,
        status: 404,
        statusText: 'Not Found',
        text: async () => 'Not Found',
      });

      await expect(
        chatApi.send(mockItineraryId, { text: 'test' } as any)
      ).rejects.toThrow('Failed to send message (404): Not Found');
    });

    it('should handle HTML error responses', async () => {
      (global.fetch as any).mockResolvedValue({
        ok: false,
        status: 404,
        text: async () => '<!DOCTYPE html><html>...</html>',
      });

      await expect(
        chatApi.send(mockItineraryId, { text: 'test' } as any)
      ).rejects.toThrow('Chat endpoint not available');
    });

    it('should include request body in POST', async () => {
      const mockRequest = {
        itineraryId: mockItineraryId,
        scope: 'day' as const,
        day: 2,
        text: 'Add museum',
        autoApply: true,
      };

      (global.fetch as any).mockResolvedValue({
        ok: true,
        text: async () => JSON.stringify({ message: 'Done' }),
      });

      await chatApi.send(mockItineraryId, mockRequest);

      expect(global.fetch).toHaveBeenCalledWith(
        expect.any(String),
        expect.objectContaining({
          body: JSON.stringify(mockRequest),
        })
      );
    });
  });

  describe('history', () => {
    it('should retrieve chat history successfully', async () => {
      const mockHistory = [
        {
          message: 'Hello',
          sender: 'user' as const,
          timestamp: Date.now(),
        },
        {
          message: 'Hi!',
          sender: 'assistant' as const,
          timestamp: Date.now(),
        },
      ];

      (global.fetch as any).mockResolvedValue({
        ok: true,
        headers: {
          get: (name: string) => (name === 'content-type' ? 'application/json' : null),
        },
        text: async () => JSON.stringify(mockHistory),
      });

      const result = await chatApi.history(mockItineraryId);

      expect(result).toEqual(mockHistory);
      expect(global.fetch).toHaveBeenCalledWith(
        `/api/v1/itineraries/${mockItineraryId}/chat/history`,
        { credentials: 'include' }
      );
    });

    it('should return empty array on 404', async () => {
      (global.fetch as any).mockResolvedValue({
        ok: false,
        status: 404,
        text: async () => 'Not Found',
      });

      const result = await chatApi.history(mockItineraryId);

      expect(result).toEqual([]);
    });

    it('should return empty array for HTML responses', async () => {
      (global.fetch as any).mockResolvedValue({
        ok: true,
        headers: {
          get: (name: string) => (name === 'content-type' ? 'text/html' : null),
        },
        text: async () => '<!DOCTYPE html>',
      });

      const result = await chatApi.history(mockItineraryId);

      expect(result).toEqual([]);
    });

    it('should handle empty JSON array', async () => {
      (global.fetch as any).mockResolvedValue({
        ok: true,
        headers: {
          get: (name: string) => (name === 'content-type' ? 'application/json' : null),
        },
        text: async () => '[]',
      });

      const result = await chatApi.history(mockItineraryId);

      expect(result).toEqual([]);
    });
  });

  describe('persist', () => {
    it('should persist a message successfully', async () => {
      const mockMessage = {
        message: 'Test',
        sender: 'user' as const,
        timestamp: Date.now(),
      };

      (global.fetch as any).mockResolvedValue({
        ok: true,
        text: async () => '',
      });

      await chatApi.persist(mockItineraryId, mockMessage);

      expect(global.fetch).toHaveBeenCalledWith(
        `/api/v1/itineraries/${mockItineraryId}/chat/history`,
        expect.objectContaining({
          method: 'POST',
          body: JSON.stringify(mockMessage),
        })
      );
    });

    it('should not throw error on failure (best-effort)', async () => {
      (global.fetch as any).mockResolvedValue({
        ok: false,
        status: 500,
        text: async () => 'Server Error',
      });

      await expect(
        chatApi.persist(mockItineraryId, { message: 'test' } as any)
      ).resolves.not.toThrow();
    });

    it('should handle network errors gracefully', async () => {
      (global.fetch as any).mockRejectedValue(new Error('Network error'));

      await expect(
        chatApi.persist(mockItineraryId, { message: 'test' } as any)
      ).resolves.not.toThrow();
    });
  });

  describe('clear', () => {
    it('should clear chat history successfully', async () => {
      (global.fetch as any).mockResolvedValue({
        ok: true,
        text: async () => '',
      });

      await chatApi.clear(mockItineraryId);

      expect(global.fetch).toHaveBeenCalledWith(
        `/api/v1/itineraries/${mockItineraryId}/chat/history`,
        expect.objectContaining({
          method: 'DELETE',
        })
      );
    });

    it('should throw error on failure', async () => {
      (global.fetch as any).mockResolvedValue({
        ok: false,
        status: 500,
        statusText: 'Server Error',
        text: async () => 'Error',
      });

      await expect(chatApi.clear(mockItineraryId)).rejects.toThrow(
        'Failed to clear history'
      );
    });

    it('should handle HTML error page', async () => {
      (global.fetch as any).mockResolvedValue({
        ok: false,
        status: 404,
        text: async () => '<!DOCTYPE html>',
      });

      await expect(chatApi.clear(mockItineraryId)).rejects.toThrow(
        'Clear history endpoint not available'
      );
    });
  });

  describe('applyChangeSet', () => {
    it('should apply changes successfully', async () => {
      const mockRequest = {
        changeSet: { operation: 'modify', activityId: '123' },
      };

      const mockResponse = {
        toVersion: 2,
        diff: { changes: [] },
      };

      (global.fetch as any).mockResolvedValue({
        ok: true,
        text: async () => JSON.stringify(mockResponse),
        json: async () => mockResponse,
      });

      const result = await chatApi.applyChangeSet(mockItineraryId, mockRequest);

      expect(result).toEqual(mockResponse);
      expect(global.fetch).toHaveBeenCalledWith(
        `/api/v1/itineraries/${mockItineraryId}:apply`,
        expect.objectContaining({
          method: 'POST',
          body: JSON.stringify(mockRequest),
        })
      );
    });

    it('should throw error on failure', async () => {
      (global.fetch as any).mockResolvedValue({
        ok: false,
        status: 500,
        text: async () => 'Server Error',
      });

      await expect(
        chatApi.applyChangeSet(mockItineraryId, { changeSet: {} })
      ).rejects.toThrow('Failed to apply changes');
    });

    it('should handle 404 with helpful message', async () => {
      (global.fetch as any).mockResolvedValue({
        ok: false,
        status: 404,
        text: async () => '<!DOCTYPE html>',
      });

      await expect(
        chatApi.applyChangeSet(mockItineraryId, { changeSet: {} })
      ).rejects.toThrow('Apply endpoint not available');
    });

    it('should include credentials in request', async () => {
      (global.fetch as any).mockResolvedValue({
        ok: true,
        text: async () => JSON.stringify({ toVersion: 1, diff: {} }),
      });

      await chatApi.applyChangeSet(mockItineraryId, { changeSet: {} });

      expect(global.fetch).toHaveBeenCalledWith(
        expect.any(String),
        expect.objectContaining({
          credentials: 'include',
        })
      );
    });
  });

  describe('safeJsonParse', () => {
    it('should handle empty responses when allowed', async () => {
      (global.fetch as any).mockResolvedValue({
        ok: true,
        headers: {
          get: () => 'application/json',
        },
        text: async () => '',
      });

      const result = await chatApi.history(mockItineraryId);
      expect(result).toEqual([]);
    });

    it('should detect HTML responses', async () => {
      (global.fetch as any).mockResolvedValue({
        ok: true,
        headers: {
          get: () => 'application/json',
        },
        text: async () => '<!DOCTYPE html><html></html>',
      });

      const result = await chatApi.history(mockItineraryId);
      expect(result).toEqual([]);
    });

    it('should handle partial HTML in response', async () => {
      (global.fetch as any).mockResolvedValue({
        ok: true,
        headers: {
          get: () => 'application/json',
        },
        text: async () => '<html><body>Error</body></html>',
      });

      const result = await chatApi.history(mockItineraryId);
      expect(result).toEqual([]);
    });
  });

  describe('edge cases', () => {
    it('should handle network timeout', async () => {
      (global.fetch as any).mockRejectedValue(new Error('Network request failed'));

      await expect(
        chatApi.send(mockItineraryId, { text: 'test' } as any)
      ).rejects.toThrow();
    });

    it('should handle malformed JSON response', async () => {
      (global.fetch as any).mockResolvedValue({
        ok: true,
        headers: {
          get: () => 'application/json',
        },
        text: async () => '{invalid json}',
      });

      await expect(chatApi.history(mockItineraryId)).rejects.toThrow();
    });

    it('should handle very large messages', async () => {
      const largeMessage = 'a'.repeat(10000);
      (global.fetch as any).mockResolvedValue({
        ok: true,
        text: async () => '',
      });

      await expect(
        chatApi.persist(mockItineraryId, { message: largeMessage } as any)
      ).resolves.not.toThrow();
    });

    it('should handle special characters in messages', async () => {
      const specialMessage = '你好 🎉 <script>alert("xss")</script>';
      (global.fetch as any).mockResolvedValue({
        ok: true,
        text: async () => JSON.stringify({ message: specialMessage }),
      });

      await chatApi.send(mockItineraryId, { text: specialMessage } as any);

      expect(global.fetch).toHaveBeenCalledWith(
        `/api/v1/itineraries/${mockItineraryId}/chat`,
        expect.objectContaining({
          method: 'POST',
          credentials: 'include',
          headers: { 'Content-Type': 'application/json' },
          body: expect.stringContaining(specialMessage),
        })
      );
    });
  });
});
