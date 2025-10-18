import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { SseManager } from '../../services/sseManager';
import { apiClient } from '../../services/apiClient';

// Mock the apiClient
vi.mock('../../services/apiClient', () => ({
  apiClient: {
    createAgentEventStream: vi.fn(),
    createPatchesEventStream: vi.fn(),
  },
}));

// Mock EventSource
class MockEventSource {
  public onopen: ((event: Event) => void) | null = null;
  public onmessage: ((event: MessageEvent) => void) | null = null;
  public onerror: ((event: Event) => void) | null = null;
  public readyState: number = 0;
  public url: string;

  constructor(url: string) {
    this.url = url;
    // Simulate connection opening
    setTimeout(() => {
      this.readyState = 1;
      if (this.onopen) {
        this.onopen(new Event('open'));
      }
    }, 10);
  }

  close() {
    this.readyState = 2;
  }

  // Helper method to simulate receiving a message
  simulateMessage(data: any) {
    if (this.onmessage) {
      const event = new MessageEvent('message', { data: JSON.stringify(data) });
      this.onmessage(event);
    }
  }

  // Helper method to simulate an error
  simulateError() {
    if (this.onerror) {
      this.onerror(new Event('error'));
    }
  }
}

// Replace global EventSource with our mock
global.EventSource = MockEventSource as any;

describe('SSE Connection Tests', () => {
  let sseManager: SseManager;
  const mockItineraryId = 'it_test_123';
  const mockExecutionId = 'exec_test_456';

  beforeEach(() => {
    vi.clearAllMocks();
    sseManager = new SseManager({
      onConnect: vi.fn(),
      onDisconnect: vi.fn(),
      onAgentEvent: vi.fn(),
      onPatchEvent: vi.fn(),
      onError: vi.fn(),
      executionId: mockExecutionId,
    });
  });

  afterEach(() => {
    sseManager.disconnect();
  });

  describe('Agent Event Stream', () => {
    it('should create agent event stream with correct URL', () => {
      // Given
      const mockEventSource = new MockEventSource('mock-url');
      (apiClient.createAgentEventStream as any).mockReturnValue(mockEventSource);

      // When
      sseManager.connect(mockItineraryId);

      // Then
      expect(apiClient.createAgentEventStream).toHaveBeenCalledWith(mockItineraryId);
    });

    it('should handle agent event stream connection', () => {
      // Given
      const mockEventSource = new MockEventSource('mock-url');
      (apiClient.createAgentEventStream as any).mockReturnValue(mockEventSource);
      const onConnect = vi.fn();
      sseManager = new SseManager({
        onConnect,
        onDisconnect: vi.fn(),
        onAgentEvent: vi.fn(),
        onPatchEvent: vi.fn(),
        onError: vi.fn(),
        executionId: mockExecutionId,
      });

      // When
      sseManager.connect(mockItineraryId);

      // Then
      expect(onConnect).toHaveBeenCalled();
    });

    it('should handle agent event messages', () => {
      // Given
      const mockEventSource = new MockEventSource('mock-url');
      (apiClient.createAgentEventStream as any).mockReturnValue(mockEventSource);
      const onAgentEvent = vi.fn();
      sseManager = new SseManager({
        onConnect: vi.fn(),
        onDisconnect: vi.fn(),
        onAgentEvent,
        onPatchEvent: vi.fn(),
        onError: vi.fn(),
        executionId: mockExecutionId,
      });

      sseManager.connect(mockItineraryId);

      // When
      const testEvent = { type: 'agent_event', data: 'test data' };
      mockEventSource.simulateMessage(testEvent);

      // Then
      expect(onAgentEvent).toHaveBeenCalledWith(testEvent);
    });

    it('should handle agent event stream errors', () => {
      // Given
      const mockEventSource = new MockEventSource('mock-url');
      (apiClient.createAgentEventStream as any).mockReturnValue(mockEventSource);
      const onError = vi.fn();
      sseManager = new SseManager({
        onConnect: vi.fn(),
        onDisconnect: vi.fn(),
        onAgentEvent: vi.fn(),
        onPatchEvent: vi.fn(),
        onError,
        executionId: mockExecutionId,
      });

      sseManager.connect(mockItineraryId);

      // When
      mockEventSource.simulateError();

      // Then
      expect(onError).toHaveBeenCalled();
    });
  });

  describe('Patches Event Stream', () => {
    it('should create patches event stream with execution ID', () => {
      // Given
      const mockEventSource = new MockEventSource('mock-url');
      (apiClient.createPatchesEventStream as any).mockReturnValue(mockEventSource);

      // When
      sseManager.connect(mockItineraryId);

      // Then
      expect(apiClient.createPatchesEventStream).toHaveBeenCalledWith(mockItineraryId, mockExecutionId);
    });

    it('should create patches event stream without execution ID when not provided', () => {
      // Given
      const mockEventSource = new MockEventSource('mock-url');
      (apiClient.createPatchesEventStream as any).mockReturnValue(mockEventSource);
      sseManager = new SseManager({
        onConnect: vi.fn(),
        onDisconnect: vi.fn(),
        onAgentEvent: vi.fn(),
        onPatchEvent: vi.fn(),
        onError: vi.fn(),
        // No executionId provided
      });

      // When
      sseManager.connect(mockItineraryId);

      // Then
      expect(apiClient.createPatchesEventStream).toHaveBeenCalledWith(mockItineraryId, undefined);
    });

    it('should handle patch event messages', () => {
      // Given
      const mockEventSource = new MockEventSource('mock-url');
      (apiClient.createPatchesEventStream as any).mockReturnValue(mockEventSource);
      const onPatchEvent = vi.fn();
      sseManager = new SseManager({
        onConnect: vi.fn(),
        onDisconnect: vi.fn(),
        onAgentEvent: vi.fn(),
        onPatchEvent,
        onError: vi.fn(),
        executionId: mockExecutionId,
      });

      sseManager.connect(mockItineraryId);

      // When
      const testPatch = { type: 'patch', data: 'patch data' };
      mockEventSource.simulateMessage(testPatch);

      // Then
      expect(onPatchEvent).toHaveBeenCalledWith(testPatch);
    });
  });

  describe('Connection Management', () => {
    it('should disconnect all streams when disconnect is called', () => {
      // Given
      const mockEventSource = new MockEventSource('mock-url');
      (apiClient.createAgentEventStream as any).mockReturnValue(mockEventSource);
      (apiClient.createPatchesEventStream as any).mockReturnValue(mockEventSource);
      const onDisconnect = vi.fn();
      sseManager = new SseManager({
        onConnect: vi.fn(),
        onDisconnect,
        onAgentEvent: vi.fn(),
        onPatchEvent: vi.fn(),
        onError: vi.fn(),
        executionId: mockExecutionId,
      });

      sseManager.connect(mockItineraryId);

      // When
      sseManager.disconnect();

      // Then
      expect(onDisconnect).toHaveBeenCalled();
    });

    it('should handle multiple connections to the same itinerary', () => {
      // Given
      const mockEventSource = new MockEventSource('mock-url');
      (apiClient.createAgentEventStream as any).mockReturnValue(mockEventSource);
      (apiClient.createPatchesEventStream as any).mockReturnValue(mockEventSource);

      // When
      sseManager.connect(mockItineraryId);
      sseManager.connect(mockItineraryId); // Second connection

      // Then
      expect(apiClient.createAgentEventStream).toHaveBeenCalledTimes(2);
      expect(apiClient.createPatchesEventStream).toHaveBeenCalledTimes(2);
    });

    it('should handle connection to different itineraries', () => {
      // Given
      const mockEventSource = new MockEventSource('mock-url');
      (apiClient.createAgentEventStream as any).mockReturnValue(mockEventSource);
      (apiClient.createPatchesEventStream as any).mockReturnValue(mockEventSource);
      const otherItineraryId = 'it_test_456';

      // When
      sseManager.connect(mockItineraryId);
      sseManager.connect(otherItineraryId);

      // Then
      expect(apiClient.createAgentEventStream).toHaveBeenCalledWith(mockItineraryId);
      expect(apiClient.createAgentEventStream).toHaveBeenCalledWith(otherItineraryId);
      expect(apiClient.createPatchesEventStream).toHaveBeenCalledWith(mockItineraryId, mockExecutionId);
      expect(apiClient.createPatchesEventStream).toHaveBeenCalledWith(otherItineraryId, mockExecutionId);
    });
  });

  describe('Error Handling', () => {
    it('should handle malformed JSON in event messages', () => {
      // Given
      const mockEventSource = new MockEventSource('mock-url');
      (apiClient.createAgentEventStream as any).mockReturnValue(mockEventSource);
      const onError = vi.fn();
      sseManager = new SseManager({
        onConnect: vi.fn(),
        onDisconnect: vi.fn(),
        onAgentEvent: vi.fn(),
        onPatchEvent: vi.fn(),
        onError,
        executionId: mockExecutionId,
      });

      sseManager.connect(mockItineraryId);

      // When
      const malformedEvent = new MessageEvent('message', { data: 'invalid json{' });
      mockEventSource.onmessage!(malformedEvent);

      // Then
      expect(onError).toHaveBeenCalled();
    });

    it('should handle empty event messages', () => {
      // Given
      const mockEventSource = new MockEventSource('mock-url');
      (apiClient.createAgentEventStream as any).mockReturnValue(mockEventSource);
      const onAgentEvent = vi.fn();
      sseManager = new SseManager({
        onConnect: vi.fn(),
        onDisconnect: vi.fn(),
        onAgentEvent,
        onPatchEvent: vi.fn(),
        onError: vi.fn(),
        executionId: mockExecutionId,
      });

      sseManager.connect(mockItineraryId);

      // When
      const emptyEvent = new MessageEvent('message', { data: '' });
      mockEventSource.onmessage!(emptyEvent);

      // Then
      expect(onAgentEvent).toHaveBeenCalledWith(null);
    });
  });
});


