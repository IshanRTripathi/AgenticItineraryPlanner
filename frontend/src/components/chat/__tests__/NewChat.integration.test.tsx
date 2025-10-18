import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { NewChat } from '../NewChat';
import * as chatApi from '../../../services/chatApi';

// Mock the context
vi.mock('../../../contexts/UnifiedItineraryContext', () => ({
  UnifiedItineraryProvider: ({ children }: any) => <div>{children}</div>,
  useUnifiedItinerary: () => ({
    state: {
      itinerary: { id: 'test-itin-123' },
      isConnected: true,
      selectedDay: 1,
      selectedNodeId: null,
    },
    loadItinerary: vi.fn(),
  }),
}));

// Mock chatApi
vi.mock('../../../services/chatApi');

describe('NewChat - User Interaction Integration Tests', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    // Mock history to return empty by default
    vi.mocked(chatApi.chatApi.history).mockResolvedValue([]);
    vi.mocked(chatApi.chatApi.persist).mockResolvedValue(undefined);
  });

  describe('Scenario 1: Simple modification request', () => {
    it('should handle "Move lunch to 2pm" request with changeSet response', async () => {
      // Mock backend response based on OrchestratorService
      const backendResponse = {
        intent: 'modify_activity',
        message: 'I\'ll move lunch to 2:00 PM for you.',
        changeSet: {
          operations: [
            {
              type: 'update',
              path: '/days/0/activities/2/time',
              value: '14:00',
              oldValue: '12:00'
            }
          ]
        },
        diff: {
          sections: [
            {
              type: 'modified',
              path: 'Day 1 / Lunch',
              changes: [
                { type: 'removed', line: '🕐 Time: 12:00 PM' },
                { type: 'added', line: '🕐 Time: 2:00 PM' }
              ]
            }
          ]
        },
        applied: false,
        needsDisambiguation: false,
        warnings: [],
        errors: []
      };

      vi.mocked(chatApi.chatApi.send).mockResolvedValue(backendResponse);

      render(<NewChat />);

      // Wait for component to load
      await waitFor(() => {
        expect(screen.getByPlaceholderText('Ask me about your trip…')).toBeInTheDocument();
      });

      // User types message
      const textarea = screen.getByPlaceholderText('Ask me about your trip…');
      fireEvent.change(textarea, { target: { value: 'Move lunch to 2pm' } });

      // User clicks send
      const sendButton = screen.getByRole('button', { name: /send/i });
      fireEvent.click(sendButton);

      // Wait for response
      await waitFor(() => {
        expect(chatApi.chatApi.send).toHaveBeenCalledWith(
          'test-itin-123',
          expect.objectContaining({
            text: 'Move lunch to 2pm',
            scope: 'day',
            day: 1,
            autoApply: false,
          })
        );
      });

      // Verify UI shows intent badge
      await waitFor(() => {
        expect(screen.getByText('modify_activity')).toBeInTheDocument();
      });

      // Verify message is displayed
      expect(screen.getByText('I\'ll move lunch to 2:00 PM for you.')).toBeInTheDocument();

      // Verify preview button is shown
      expect(screen.getByText('Apply Changes')).toBeInTheDocument();
    });
  });

  describe('Scenario 2: Disambiguation needed', () => {
    it('should show candidates when disambiguation is required', async () => {
      const backendResponse = {
        intent: 'modify_activity',
        message: 'I found multiple restaurants. Which one did you mean?',
        needsDisambiguation: true,
        candidates: [
          { id: 'rest-1', title: 'Italian Restaurant', day: 1, location: 'Downtown' },
          { id: 'rest-2', title: 'French Restaurant', day: 2, location: 'Uptown' },
          { id: 'rest-3', title: 'Asian Restaurant', day: 1, location: 'Midtown' }
        ],
        changeSet: null,
        applied: false,
        warnings: [],
        errors: []
      };

      vi.mocked(chatApi.chatApi.send).mockResolvedValue(backendResponse);

      render(<NewChat />);

      await waitFor(() => {
        expect(screen.getByPlaceholderText('Ask me about your trip…')).toBeInTheDocument();
      });

      const textarea = screen.getByPlaceholderText('Ask me about your trip…');
      fireEvent.change(textarea, { target: { value: 'Remove the restaurant' } });

      const sendButton = screen.getByRole('button', { name: /send/i });
      fireEvent.click(sendButton);

      // Wait for disambiguation UI
      await waitFor(() => {
        expect(screen.getByText('Did you mean one of these?')).toBeInTheDocument();
      });

      // Verify all candidates are shown
      expect(screen.getByText('Italian Restaurant')).toBeInTheDocument();
      expect(screen.getByText('French Restaurant')).toBeInTheDocument();
      expect(screen.getByText('Asian Restaurant')).toBeInTheDocument();

      // Click on a candidate to select it
      const selectButton = screen.getAllByText('Select')[0];
      fireEvent.click(selectButton);

      // Verify input is prefilled
      await waitFor(() => {
        const input = screen.getByPlaceholderText('Ask me about your trip…') as HTMLTextAreaElement;
        expect(input.value).toContain('Italian Restaurant');
      });
    });
  });

  describe('Scenario 3: Request with warnings', () => {
    it('should display warnings from backend', async () => {
      const backendResponse = {
        intent: 'add_activity',
        message: 'I\'ve added the museum visit, but there are some concerns.',
        changeSet: {
          operations: [
            {
              type: 'add',
              path: '/days/1/activities/-',
              value: { title: 'Museum Visit', time: '15:00' }
            }
          ]
        },
        diff: null,
        applied: false,
        needsDisambiguation: false,
        warnings: [
          'This activity may conflict with dinner reservations at 5pm',
          'Museum closes at 4pm on Sundays'
        ],
        errors: []
      };

      vi.mocked(chatApi.chatApi.send).mockResolvedValue(backendResponse);

      render(<NewChat />);

      await waitFor(() => {
        expect(screen.getByPlaceholderText('Ask me about your trip…')).toBeInTheDocument();
      });

      const textarea = screen.getByPlaceholderText('Ask me about your trip…');
      fireEvent.change(textarea, { target: { value: 'Add museum visit at 3pm' } });

      const sendButton = screen.getByRole('button', { name: /send/i });
      fireEvent.click(sendButton);

      // Wait for warnings to appear
      await waitFor(() => {
        expect(screen.getByText(/conflict with dinner reservations/i)).toBeInTheDocument();
      });

      expect(screen.getByText(/Museum closes at 4pm/i)).toBeInTheDocument();
    });
  });

  describe('Scenario 4: Error handling', () => {
    it('should display errors from backend', async () => {
      const backendResponse = {
        intent: 'unknown',
        message: 'I couldn\'t process your request.',
        changeSet: null,
        diff: null,
        applied: false,
        needsDisambiguation: false,
        warnings: [],
        errors: [
          'Invalid time format provided',
          'Activity not found in itinerary'
        ]
      };

      vi.mocked(chatApi.chatApi.send).mockResolvedValue(backendResponse);

      render(<NewChat />);

      await waitFor(() => {
        expect(screen.getByPlaceholderText('Ask me about your trip…')).toBeInTheDocument();
      });

      const textarea = screen.getByPlaceholderText('Ask me about your trip…');
      fireEvent.change(textarea, { target: { value: 'Move dinner to invalid time' } });

      const sendButton = screen.getByRole('button', { name: /send/i });
      fireEvent.click(sendButton);

      // Wait for errors to appear
      await waitFor(() => {
        expect(screen.getByText('Invalid time format provided')).toBeInTheDocument();
      });

      expect(screen.getByText('Activity not found in itinerary')).toBeInTheDocument();
    });
  });

  describe('Scenario 5: Apply changes flow', () => {
    it('should apply changes and reload itinerary', async () => {
      const backendResponse = {
        intent: 'modify_activity',
        message: 'I\'ll update the activity time for you.',
        changeSet: {
          operations: [
            {
              type: 'update',
              path: '/days/0/activities/1/time',
              value: '16:00'
            }
          ]
        },
        diff: {
          sections: [
            {
              type: 'modified',
              path: 'Day 1 / Coffee Break',
              changes: [
                { type: 'removed', line: '🕐 Time: 3:00 PM' },
                { type: 'added', line: '🕐 Time: 4:00 PM' }
              ]
            }
          ]
        },
        applied: false,
        needsDisambiguation: false,
        warnings: [],
        errors: []
      };

      const applyResponse = {
        toVersion: 2,
        diff: backendResponse.diff
      };

      vi.mocked(chatApi.chatApi.send).mockResolvedValue(backendResponse);
      vi.mocked(chatApi.chatApi.applyChangeSet).mockResolvedValue(applyResponse);

      const { useUnifiedItinerary } = await import('../../../contexts/UnifiedItineraryContext');
      const mockLoadItinerary = vi.fn();
      vi.mocked(useUnifiedItinerary).mockReturnValue({
        state: {
          itinerary: { id: 'test-itin-123' },
          isConnected: true,
          selectedDay: 1,
          selectedNodeId: null,
        },
        loadItinerary: mockLoadItinerary,
      } as any);

      render(<NewChat />);

      await waitFor(() => {
        expect(screen.getByPlaceholderText('Ask me about your trip…')).toBeInTheDocument();
      });

      const textarea = screen.getByPlaceholderText('Ask me about your trip…');
      fireEvent.change(textarea, { target: { value: 'Move coffee to 4pm' } });

      const sendButton = screen.getByRole('button', { name: /send/i });
      fireEvent.click(sendButton);

      // Wait for preview to appear
      await waitFor(() => {
        expect(screen.getByText('Apply Changes')).toBeInTheDocument();
      });

      // Click apply button
      const applyButton = screen.getByText('Apply Changes');
      fireEvent.click(applyButton);

      // Verify applyChangeSet was called
      await waitFor(() => {
        expect(chatApi.chatApi.applyChangeSet).toHaveBeenCalledWith(
          'test-itin-123',
          { changeSet: backendResponse.changeSet }
        );
      });

      // Verify itinerary reload was triggered
      expect(mockLoadItinerary).toHaveBeenCalledWith('test-itin-123');

      // Verify applied badge appears
      await waitFor(() => {
        expect(screen.getByText('✓ Applied')).toBeInTheDocument();
      });
    });
  });

  describe('Scenario 6: Chat history persistence', () => {
    it('should persist messages and load history on mount', async () => {
      const historicalMessages = [
        {
          id: 'msg-1',
          sender: 'user',
          message: 'Previous question',
          timestamp: Date.now() - 10000
        },
        {
          id: 'msg-2',
          sender: 'assistant',
          message: 'Previous answer',
          timestamp: Date.now() - 9000,
          intent: 'query'
        }
      ];

      vi.mocked(chatApi.chatApi.history).mockResolvedValue(historicalMessages);

      render(<NewChat />);

      // Wait for history to load
      await waitFor(() => {
        expect(chatApi.chatApi.history).toHaveBeenCalledWith('test-itin-123');
      });

      // Verify historical messages are displayed
      await waitFor(() => {
        expect(screen.getByText('Previous question')).toBeInTheDocument();
      });
      expect(screen.getByText('Previous answer')).toBeInTheDocument();
    });
  });

  describe('Scenario 7: Clear history', () => {
    it('should clear history after confirmation', async () => {
      // Mock confirm dialog
      global.confirm = vi.fn(() => true);

      const historicalMessages = [
        {
          sender: 'user',
          message: 'Old message',
          timestamp: Date.now() - 10000
        }
      ];

      vi.mocked(chatApi.chatApi.history).mockResolvedValue(historicalMessages);
      vi.mocked(chatApi.chatApi.clear).mockResolvedValue(undefined);

      render(<NewChat />);

      // Wait for history to load
      await waitFor(() => {
        expect(screen.getByText('Old message')).toBeInTheDocument();
      });

      // Click clear button
      const clearButton = screen.getByTitle('Clear chat history');
      fireEvent.click(clearButton);

      // Verify confirm was called
      expect(global.confirm).toHaveBeenCalled();

      // Verify API was called
      await waitFor(() => {
        expect(chatApi.chatApi.clear).toHaveBeenCalledWith('test-itin-123');
      });

      // Verify messages are cleared from UI
      await waitFor(() => {
        expect(screen.queryByText('Old message')).not.toBeInTheDocument();
      });
    });
  });

  describe('Scenario 8: Detailed diff view', () => {
    it('should toggle detailed diff view', async () => {
      const backendResponse = {
        intent: 'modify_activity',
        message: 'I\'ll update the activity.',
        changeSet: {
          operations: [
            { type: 'update', path: '/days/0/activities/0/title', value: 'Updated Title' }
          ]
        },
        diff: {
          sections: [
            {
              type: 'modified',
              path: 'Day 1 / Morning Activity',
              changes: [
                { type: 'removed', line: 'Old Title' },
                { type: 'added', line: 'Updated Title' }
              ]
            }
          ]
        },
        applied: false,
        needsDisambiguation: false,
        warnings: [],
        errors: []
      };

      vi.mocked(chatApi.chatApi.send).mockResolvedValue(backendResponse);

      render(<NewChat />);

      await waitFor(() => {
        expect(screen.getByPlaceholderText('Ask me about your trip…')).toBeInTheDocument();
      });

      const textarea = screen.getByPlaceholderText('Ask me about your trip…');
      fireEvent.change(textarea, { target: { value: 'Update morning activity' } });

      const sendButton = screen.getByRole('button', { name: /send/i });
      fireEvent.click(sendButton);

      // Wait for preview
      await waitFor(() => {
        expect(screen.getByText('Preview Changes')).toBeInTheDocument();
      });

      // Initially shows simple preview
      expect(screen.getByText('This message contains proposed changes to your itinerary')).toBeInTheDocument();

      // Click "Show Details"
      const showDetailsButton = screen.getByText('Show Details');
      fireEvent.click(showDetailsButton);

      // Verify detailed view is shown
      await waitFor(() => {
        expect(screen.getByText('Hide Details')).toBeInTheDocument();
      });
    });
  });
});






