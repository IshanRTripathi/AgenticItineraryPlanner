/**
 * Tests for API service with retry logic and error handling
 */

import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { itineraryApi } from '../api';

// Mock fetch globally
const mockFetch = vi.fn();
global.fetch = mockFetch;

// Mock logger
vi.mock('../../utils/logger', () => ({
  logger: {
    logApiCall: vi.fn(() => ({
      success: vi.fn(),
      error: vi.fn()
    }))
  },
  logInfo: vi.fn(),
  logError: vi.fn(),
  logWarn: vi.fn()
}));

// Mock data transformers
vi.mock('../../utils/dataTransformers', () => ({
  normalizedItineraryToTripData: vi.fn((data) => ({ ...data, transformed: true })),
  tripDataToNormalizedItinerary: vi.fn((data) => ({ ...data, normalized: true })),
  isNormalizedItinerary: vi.fn(() => true)
}));

describe('API Service with Retry Logic', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  describe('Exponential Backoff Retry', () => {
    it('should retry on 404 errors with exponential backoff', async () => {
      // First two calls return 404, third succeeds
      mockFetch
        .mockResolvedValueOnce({
          ok: false,
          status: 404,
          statusText: 'Not Found',
          json: () => Promise.resolve({ message: 'Itinerary not found' })
        })
        .mockResolvedValueOnce({
          ok: false,
          status: 404,
          statusText: 'Not Found',
          json: () => Promise.resolve({ message: 'Itinerary not found' })
        })
        .mockResolvedValueOnce({
          ok: true,
          status: 200,
          headers: new Map([['content-type', 'application/json']]),
          json: () => Promise.resolve({ 
            itineraryId: 'test-123',
            days: [{ dayNumber: 1, nodes: [] }]
          })
        });

      const promise = itineraryApi.getItinerary('test-123');

      // Fast-forward through the retry delays
      await vi.advanceTimersByTimeAsync(1000); // First retry after 1s
      await vi.advanceTimersByTimeAsync(2000); // Second retry after 2s

      const result = await promise;

      expect(mockFetch).toHaveBeenCalledTimes(3);
      expect(result).toEqual({
        itineraryId: 'test-123',
        days: [{ dayNumber: 1, nodes: [] }],
        transformed: true
      });
    });

    it('should fail after max retries exceeded', async () => {
      // All calls return 404
      mockFetch.mockResolvedValue({
        ok: false,
        status: 404,
        statusText: 'Not Found',
        json: () => Promise.resolve({ message: 'Itinerary not found' })
      });

      const promise = itineraryApi.getItinerary('test-123');

      // Fast-forward through all retry attempts
      await vi.advanceTimersByTimeAsync(1000); // First retry
      await vi.advanceTimersByTimeAsync(2000); // Second retry
      await vi.advanceTimersByTimeAsync(4000); // Third retry

      await expect(promise).rejects.toThrow();
      expect(mockFetch).toHaveBeenCalledTimes(4); // Initial + 3 retries
    });

    it('should retry on network errors', async () => {
      // First call fails with network error, second succeeds
      mockFetch
        .mockRejectedValueOnce(new TypeError('Failed to fetch'))
        .mockResolvedValueOnce({
          ok: true,
          status: 200,
          headers: new Map([['content-type', 'application/json']]),
          json: () => Promise.resolve({ 
            itineraryId: 'test-123',
            days: []
          })
        });

      const promise = itineraryApi.getItinerary('test-123');

      // Fast-forward through the retry delay
      await vi.advanceTimersByTimeAsync(1000);

      const result = await promise;

      expect(mockFetch).toHaveBeenCalledTimes(2);
      expect(result).toEqual({
        itineraryId: 'test-123',
        days: [],
        transformed: true
      });
    });

    it('should not retry on 400 errors', async () => {
      mockFetch.mockResolvedValue({
        ok: false,
        status: 400,
        statusText: 'Bad Request',
        json: () => Promise.resolve({ message: 'Invalid request' })
      });

      await expect(itineraryApi.getItinerary('test-123')).rejects.toThrow();
      expect(mockFetch).toHaveBeenCalledTimes(1); // No retries
    });
  });

  describe('User-Friendly Error Messages', () => {
    it('should provide user-friendly message for 404 errors', async () => {
      mockFetch.mockResolvedValue({
        ok: false,
        status: 404,
        statusText: 'Not Found',
        json: () => Promise.resolve({ message: 'Not found' })
      });

      try {
        await itineraryApi.getItinerary('test-123');
      } catch (error: any) {
        expect(error.userMessage).toBe('Your itinerary is being created by our AI agents. This usually takes 30-60 seconds.');
        expect(error.isRetryable).toBe(true);
        expect(error.suggestedAction).toBe('Wait 30-60 seconds and refresh the page');
      }
    });

    it('should provide user-friendly message for network errors', async () => {
      mockFetch.mockRejectedValue(new TypeError('Failed to fetch'));

      try {
        await itineraryApi.getItinerary('test-123');
      } catch (error: any) {
        expect(error.userMessage).toBe('Unable to connect to the server. Please check your internet connection and try again.');
        expect(error.isRetryable).toBe(true);
        expect(error.suggestedAction).toBe('Check your internet connection and refresh the page');
      }
    });

    it('should provide user-friendly message for 500 errors', async () => {
      mockFetch.mockResolvedValue({
        ok: false,
        status: 500,
        statusText: 'Internal Server Error',
        json: () => Promise.resolve({ message: 'Server error' })
      });

      try {
        await itineraryApi.getItinerary('test-123');
      } catch (error: any) {
        expect(error.userMessage).toBe('Our servers are experiencing issues. Please try again in a few moments.');
        expect(error.isRetryable).toBe(true);
        expect(error.suggestedAction).toBe('Try again in a few minutes');
      }
    });

    it('should provide user-friendly message for 403 errors', async () => {
      mockFetch.mockResolvedValue({
        ok: false,
        status: 403,
        statusText: 'Forbidden',
        json: () => Promise.resolve({ message: 'Access denied' })
      });

      try {
        await itineraryApi.getItinerary('test-123');
      } catch (error: any) {
        expect(error.userMessage).toBe('You don\'t have permission to access this itinerary.');
        expect(error.isRetryable).toBe(false);
        expect(error.suggestedAction).toBe('Make sure you have access to this itinerary');
      }
    });
  });

  describe('Retry Configuration', () => {
    it('should use custom retry configuration', async () => {
      const customConfig = {
        maxRetries: 1,
        baseDelay: 500,
        maxDelay: 1000,
        backoffMultiplier: 1.5,
        retryableStatusCodes: [404]
      };

      mockFetch
        .mockResolvedValueOnce({
          ok: false,
          status: 404,
          statusText: 'Not Found',
          json: () => Promise.resolve({ message: 'Not found' })
        })
        .mockResolvedValueOnce({
          ok: true,
          status: 200,
          headers: new Map([['content-type', 'application/json']]),
          json: () => Promise.resolve({ itineraryId: 'test-123' })
        });

      const promise = itineraryApi.getItinerary('test-123', customConfig);

      // Fast-forward through custom delay
      await vi.advanceTimersByTimeAsync(500);

      const result = await promise;

      expect(mockFetch).toHaveBeenCalledTimes(2);
      expect(result).toEqual({
        itineraryId: 'test-123',
        transformed: true
      });
    });
  });

  describe('Progress Indicators', () => {
    it('should log retry attempts with progress information', async () => {
      const { logWarn } = await import('../../utils/logger');

      mockFetch
        .mockResolvedValueOnce({
          ok: false,
          status: 404,
          statusText: 'Not Found',
          json: () => Promise.resolve({ message: 'Not found' })
        })
        .mockResolvedValueOnce({
          ok: true,
          status: 200,
          headers: new Map([['content-type', 'application/json']]),
          json: () => Promise.resolve({ itineraryId: 'test-123' })
        });

      const promise = itineraryApi.getItinerary('test-123');
      await vi.advanceTimersByTimeAsync(1000);
      await promise;

      expect(logWarn).toHaveBeenCalledWith(
        expect.stringContaining('Itinerary not ready yet, retrying'),
        expect.objectContaining({
          action: 'itinerary_generation_retry',
          attempt: 1,
          delay: 1000,
          retryReason: 'itinerary_generation_in_progress'
        })
      );
    });
  });
});