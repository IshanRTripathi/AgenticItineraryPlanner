import { describe, it, expect, beforeEach, vi } from 'vitest';
import { apiClient } from '../apiClient';
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
} from '../../types/NormalizedItinerary';

// Mock fetch globally
global.fetch = vi.fn();

describe('ApiClient', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('Normalized JSON Endpoints', () => {
    test('getItineraryJson should return normalized itinerary', async () => {
      // Arrange
      const mockItinerary: NormalizedItinerary = {
        itineraryId: 'test-itinerary',
        version: 1,
        summary: 'Test itinerary',
        currency: 'EUR',
        themes: ['culture', 'food'],
        days: [],
        settings: {
          autoApply: false,
          defaultScope: 'day'
        },
        agents: {
          planner: { status: 'idle' },
          enrichment: { status: 'idle' }
        }
      };

      (fetch as vi.MockedFunction<typeof fetch>).mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: async () => mockItinerary
      });

      // Act
      const result = await apiClient.getItineraryJson('test-itinerary');

      // Assert
      expect(fetch).toHaveBeenCalledWith(
        'http://localhost:8080/api/v1/itineraries/test-itinerary/json',
        expect.objectContaining({
          mode: 'cors',
          credentials: 'omit',
          headers: expect.objectContaining({
            'Content-Type': 'application/json'
          })
        })
      );
      expect(result).toEqual(mockItinerary);
    });

    test('getItineraryJson should handle 404 error', async () => {
      // Arrange
      (fetch as vi.MockedFunction<typeof fetch>).mockResolvedValueOnce({
        ok: false,
        status: 404,
        statusText: 'Not Found',
        text: async () => 'Not Found'
      });

      // Act & Assert
      await expect(apiClient.getItineraryJson('non-existent')).rejects.toThrow('API Error: HTTP 404: Not Found (404)');
    });

    test('proposeChanges should return propose response', async () => {
      // Arrange
      const changeSet: ChangeSet = {
        scope: 'day',
        day: 1,
        ops: [{
          op: 'move',
          id: 'n_attraction_1',
          startTime: '2025-06-01T10:00:00',
          endTime: '2025-06-01T12:00:00'
        }],
        preferences: {
          userFirst: true,
          autoApply: false,
          respectLocks: true
        }
      };

      const mockResponse: ProposeResponse = {
        proposed: {
          itineraryId: 'test-itinerary',
          version: 1,
          summary: 'Test itinerary',
          currency: 'EUR',
          themes: ['culture'],
          days: [],
          settings: { autoApply: false, defaultScope: 'day' },
          agents: { planner: { status: 'idle' } }
        },
        diff: { added: [], removed: [], updated: [] },
        previewVersion: 1
      };

      (fetch as vi.MockedFunction<typeof fetch>).mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: async () => mockResponse
      });

      // Act
      const result = await apiClient.proposeChanges('test-itinerary', changeSet);

      // Assert
      expect(fetch).toHaveBeenCalledWith(
        'http://localhost:8080/api/v1/itineraries/test-itinerary:propose',
        expect.objectContaining({
          method: 'POST',
          mode: 'cors',
          credentials: 'omit',
          body: JSON.stringify(changeSet),
          headers: expect.objectContaining({
            'Content-Type': 'application/json'
          })
        })
      );
      expect(result).toEqual(mockResponse);
    });

    test('applyChanges should return apply response', async () => {
      // Arrange
      const applyRequest: ApplyRequest = {
        changeSet: {
          scope: 'day',
          day: 1,
          ops: [{
            op: 'move',
            id: 'n_attraction_1',
            startTime: '2025-06-01T10:00:00',
            endTime: '2025-06-01T12:00:00'
          }],
          preferences: {
            userFirst: true,
            autoApply: false,
            respectLocks: true
          }
        }
      };

      const mockResponse: ApplyResponse = {
        toVersion: 2,
        diff: { added: [], removed: [], updated: [] }
      };

      (fetch as vi.MockedFunction<typeof fetch>).mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: async () => mockResponse
      });

      // Act
      const result = await apiClient.applyChanges('test-itinerary', applyRequest);

      // Assert
      expect(fetch).toHaveBeenCalledWith(
        'http://localhost:8080/api/v1/itineraries/test-itinerary:apply',
        expect.objectContaining({
          method: 'POST',
          mode: 'cors',
          credentials: 'omit',
          body: JSON.stringify(applyRequest),
          headers: expect.objectContaining({
            'Content-Type': 'application/json'
          })
        })
      );
      expect(result).toEqual(mockResponse);
    });

    test('undoChanges should return undo response', async () => {
      // Arrange
      const undoRequest: UndoRequest = {
        toVersion: 1
      };

      const mockResponse: UndoResponse = {
        toVersion: 1,
        diff: { added: [], removed: [], updated: [] }
      };

      (fetch as vi.MockedFunction<typeof fetch>).mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: async () => mockResponse
      });

      // Act
      const result = await apiClient.undoChanges('test-itinerary', undoRequest);

      // Assert
      expect(fetch).toHaveBeenCalledWith(
        'http://localhost:8080/api/v1/itineraries/test-itinerary:undo',
        expect.objectContaining({
          method: 'POST',
          mode: 'cors',
          credentials: 'omit',
          body: JSON.stringify(undoRequest),
          headers: expect.objectContaining({
            'Content-Type': 'application/json'
          })
        })
      );
      expect(result).toEqual(mockResponse);
    });
  });

  describe('Agent Endpoints', () => {
    test('processUserRequest should return process response', async () => {
      // Arrange
      const request: ProcessRequestRequest = {
        itineraryId: 'test-itinerary',
        request: 'Add a museum visit'
      };

      const mockResponse: ProcessRequestResponse = {
        itineraryId: 'test-itinerary',
        status: 'completed',
        message: 'Request processed successfully'
      };

      (fetch as vi.MockedFunction<typeof fetch>).mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: async () => mockResponse
      });

      // Act
      const result = await apiClient.processUserRequest(request);

      // Assert
      expect(fetch).toHaveBeenCalledWith(
        'http://localhost:8080/api/v1/agents/process-request',
        expect.objectContaining({
          method: 'POST',
          mode: 'cors',
          credentials: 'omit',
          body: JSON.stringify(request),
          headers: expect.objectContaining({
            'Content-Type': 'application/json'
          })
        })
      );
      expect(result).toEqual(mockResponse);
    });

    test('applyWithEnrichment should return enrichment response', async () => {
      // Arrange
      const request: ApplyWithEnrichmentRequest = {
        itineraryId: 'test-itinerary',
        changeSet: {
          scope: 'day',
          day: 1,
          ops: [{
            op: 'move',
            id: 'n_attraction_1',
            startTime: '2025-06-01T10:00:00',
            endTime: '2025-06-01T12:00:00'
          }],
          preferences: {
            userFirst: true,
            autoApply: false,
            respectLocks: true
          }
        }
      };

      const mockResponse: ApplyWithEnrichmentResponse = {
        itineraryId: 'test-itinerary',
        status: 'completed',
        message: 'Changes applied with enrichment'
      };

      (fetch as vi.MockedFunction<typeof fetch>).mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: async () => mockResponse
      });

      // Act
      const result = await apiClient.applyWithEnrichment(request);

      // Assert
      expect(fetch).toHaveBeenCalledWith(
        'http://localhost:8080/api/v1/agents/apply-with-enrichment',
        expect.objectContaining({
          method: 'POST',
          mode: 'cors',
          credentials: 'omit',
          body: JSON.stringify(request),
          headers: expect.objectContaining({
            'Content-Type': 'application/json'
          })
        })
      );
      expect(result).toEqual(mockResponse);
    });
  });

  describe('Booking Endpoints', () => {
    test('mockBook should return booking response', async () => {
      // Arrange
      const request: MockBookingRequest = {
        itineraryId: 'test-itinerary',
        nodeId: 'n_attraction_1'
      };

      const mockResponse: MockBookingResponse = {
        itineraryId: 'test-itinerary',
        nodeId: 'n_attraction_1',
        bookingRef: 'BK123456',
        locked: true,
        message: 'Booking completed successfully'
      };

      (fetch as vi.MockedFunction<typeof fetch>).mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: async () => mockResponse
      });

      // Act
      const result = await apiClient.mockBook(request);

      // Assert
      expect(fetch).toHaveBeenCalledWith(
        'http://localhost:8080/api/v1/book',
        expect.objectContaining({
          method: 'POST',
          mode: 'cors',
          credentials: 'omit',
          body: JSON.stringify(request),
          headers: expect.objectContaining({
            'Content-Type': 'application/json'
          })
        })
      );
      expect(result).toEqual(mockResponse);
    });
  });

  describe('SSE Endpoints', () => {
    test('createPatchesEventStream should create EventSource', () => {
      // Arrange
      const itineraryId = 'test-itinerary';
      const mockEventSource = {
        onerror: vi.fn(),
        close: vi.fn()
      };

      // Mock EventSource
      global.EventSource = vi.fn().mockImplementation(() => mockEventSource);

      // Act
      const eventSource = apiClient.createPatchesEventStream(itineraryId);

      // Assert
      expect(EventSource).toHaveBeenCalledWith(
        'http://localhost:8080/api/v1/itineraries/patches?itineraryId=test-itinerary'
      );
      expect(eventSource).toBe(mockEventSource);
    });
  });

  describe('Error Handling', () => {
    test('should handle network errors', async () => {
      // Arrange
      (fetch as vi.MockedFunction<typeof fetch>).mockRejectedValueOnce(new Error('Network error'));

      // Act & Assert
      await expect(apiClient.getItineraryJson('test-itinerary')).rejects.toThrow('Network error');
    });

    test('should handle JSON parsing errors', async () => {
      // Arrange
      (fetch as vi.MockedFunction<typeof fetch>).mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: async () => { throw new Error('Invalid JSON'); }
      });

      // Act & Assert
      await expect(apiClient.getItineraryJson('test-itinerary')).rejects.toThrow('Invalid JSON');
    });

    test('should handle 500 server errors', async () => {
      // Arrange
      (fetch as vi.MockedFunction<typeof fetch>).mockResolvedValueOnce({
        ok: false,
        status: 500,
        statusText: 'Internal Server Error',
        text: async () => 'Internal Server Error'
      });

      // Act & Assert
      await expect(apiClient.getItineraryJson('test-itinerary')).rejects.toThrow('API Error: HTTP 500: Internal Server Error (500)');
    });

    test('should handle empty responses', async () => {
      // Arrange
      (fetch as vi.MockedFunction<typeof fetch>).mockResolvedValueOnce({
        ok: true,
        status: 204,
        json: async () => null
      });

      // Act
      const result = await apiClient.getItineraryJson('test-itinerary');

      // Assert
      expect(result).toEqual({});
    });
  });

  describe('Authentication', () => {
    test('should set auth token', () => {
      // Act
      apiClient.setAuthToken('test-token');

      // Assert - This would need to be tested with actual requests
      // For now, we just verify the method exists and doesn't throw
      expect(() => apiClient.setAuthToken('test-token')).not.toThrow();
    });

    test('should clear auth token', () => {
      // Act & Assert
      expect(() => apiClient.clearAuthToken()).not.toThrow();
    });
  });
});
