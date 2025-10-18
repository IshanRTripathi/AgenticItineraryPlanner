import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, fireEvent, waitFor, cleanup } from '@testing-library/react';
import { NewChat } from '../NewChat';
import * as chatApi from '../../../services/chatApi';

// Mock the context with a factory function for better isolation
const createMockContext = (overrides = {}) => ({
  state: {
    itinerary: { id: 'test-itin-123' },
    isConnected: true,
    selectedDay: 1,
    selectedNodeId: null,
    ...overrides,
  },
  loadItinerary: vi.fn(),
});

vi.mock('../../../contexts/UnifiedItineraryContext', () => ({
  UnifiedItineraryProvider: ({ children }: any) => <div>{children}</div>,
  useUnifiedItinerary: () => createMockContext(),
}));

// Mock chatApi
vi.mock('../../../services/chatApi');

describe('NewChat - Isolated Integration Tests', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    // Default mocks
    vi.mocked(chatApi.chatApi.history).mockResolvedValue([]);
    vi.mocked(chatApi.chatApi.persist).mockResolvedValue(undefined);
  });

  afterEach(() => {
    cleanup();
  });

  describe('User Journey 1: Simple Activity Modification', () => {
    it('should handle complete flow from input to preview', async () => {
      // Arrange: Setup backend response
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

      // Act: Render and interact
      render(<NewChat />);

      // Wait for initial load
      await waitFor(() => {
        expect(screen.getByPlaceholderText(/Ask me about your trip/i)).toBeInTheDocument();
      });

      // Type message
      const textarea = screen.getByPlaceholderText(/Ask me about your trip/i);
      fireEvent.change(textarea, { target: { value: 'Move lunch to 2pm' } });

      // Click send
      const sendButton = screen.getByRole('button', { name: /send/i });
      fireEvent.click(sendButton);

      // Assert: Verify API call
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

      // Assert: Verify UI updates
      await waitFor(() => {
        expect(screen.getByText('modify_activity')).toBeInTheDocument();
        expect(screen.getByText('I\'ll move lunch to 2:00 PM for you.')).toBeInTheDocument();
        expect(screen.getByText('Apply Changes')).toBeInTheDocument();
      });
    });
  });

  describe('User Journey 2: Disambiguation Flow', () => {
    it('should display candidates and handle selection', async () => {
      const backendResponse = {
        intent: 'modify_activity',
        message: 'I found multiple restaurants. Which one did you mean?',
        needsDisambiguation: true,
        candidates: [
          { id: 'rest-1', title: 'Italian Restaurant', day: 1, location: 'Downtown' },
          { id: 'rest-2', title: 'French Restaurant', day: 2, location: 'Uptown' }
        ],
        changeSet: null,
        applied: false,
        warnings: [],
        errors: []
      };

      vi.mocked(chatApi.chatApi.send).mockResolvedValue(backendResponse);

      render(<NewChat />);

      await waitFor(() => {
        expect(screen.getByPlaceholderText(/Ask me about your trip/i)).toBeInTheDocument();
      });

      const textarea = screen.getByPlaceholderText(/Ask me about your trip/i);
      fireEvent.change(textarea, { target: { value: 'Remove the restaurant' } });

      const sendButton = screen.getByRole('button', { name: /send/i });
      fireEvent.click(sendButton);

      // Wait for disambiguation UI
      await waitFor(() => {
        expect(screen.getByText('Did you mean one of these?')).toBeInTheDocument();
      });

      // Verify candidates
      expect(screen.getByText('Italian Restaurant')).toBeInTheDocument();
      expect(screen.getByText('French Restaurant')).toBeInTheDocument();
      expect(screen.getByText(/Downtown/i)).toBeInTheDocument();

      // Click select button
      const selectButtons = screen.getAllByText('Select');
      fireEvent.click(selectButtons[0]);

      // Verify input prefilled
      await waitFor(() => {
        const input = screen.getByPlaceholderText(/Ask me about your trip/i) as HTMLTextAreaElement;
        expect(input.value).toContain('Italian Restaurant');
      });
    });
  });

  describe('User Journey 3: Warning Display', () => {
    it('should show warnings from backend', async () => {
      const backendResponse = {
        intent: 'add_activity',
        message: 'I\'ve added the museum visit, but there are some concerns.',
        changeSet: {
          operations: [{ type: 'add', path: '/days/1/activities/-', value: { title: 'Museum' } }]
        },
        diff: null,
        applied: false,
        needsDisambiguation: false,
        warnings: [
          'This activity may conflict with dinner reservations',
          'Museum closes at 4pm on Sundays'
        ],
        errors: []
      };

      vi.mocked(chatApi.chatApi.send).mockResolvedValue(backendResponse);

      render(<NewChat />);

      await waitFor(() => {
        expect(screen.getByPlaceholderText(/Ask me about your trip/i)).toBeInTheDocument();
      });

      const textarea = screen.getByPlaceholderText(/Ask me about your trip/i);
      fireEvent.change(textarea, { target: { value: 'Add museum at 3pm' } });

      const sendButton = screen.getByRole('button', { name: /send/i });
      fireEvent.click(sendButton);

      await waitFor(() => {
        expect(screen.getByText(/conflict with dinner reservations/i)).toBeInTheDocument();
        expect(screen.getByText(/Museum closes at 4pm/i)).toBeInTheDocument();
      });
    });
  });

  describe('User Journey 4: Error Handling', () => {
    it('should display errors gracefully', async () => {
      const backendResponse = {
        intent: 'unknown',
        message: 'I couldn\'t process your request.',
        changeSet: null,
        diff: null,
        applied: false,
        needsDisambiguation: false,
        warnings: [],
        errors: ['Invalid time format', 'Activity not found']
      };

      vi.mocked(chatApi.chatApi.send).mockResolvedValue(backendResponse);

      render(<NewChat />);

      await waitFor(() => {
        expect(screen.getByPlaceholderText(/Ask me about your trip/i)).toBeInTheDocument();
      });

      const textarea = screen.getByPlaceholderText(/Ask me about your trip/i);
      fireEvent.change(textarea, { target: { value: 'Invalid request' } });

      const sendButton = screen.getByRole('button', { name: /send/i });
      fireEvent.click(sendButton);

      await waitFor(() => {
        expect(screen.getByText('Invalid time format')).toBeInTheDocument();
        expect(screen.getByText('Activity not found')).toBeInTheDocument();
      });
    });
  });

  describe('User Journey 5: Apply Changes Complete Flow', () => {
    it('should apply changes and reload itinerary', async () => {
      const chatResponse = {
        intent: 'modify_activity',
        message: 'Updated successfully',
        changeSet: {
          operations: [{ type: 'update', path: '/days/0/activities/1/time', value: '16:00' }]
        },
        diff: {
          sections: [
            {
              type: 'modified',
              path: 'Day 1 / Coffee',
              changes: [
                { type: 'removed', line: 'Time: 3:00 PM' },
                { type: 'added', line: 'Time: 4:00 PM' }
              ]
            }
          ]
        },
        applied: false,
        needsDisambiguation: false,
        warnings: [],
        errors: []
      };

      const applyResponse = { toVersion: 2, diff: chatResponse.diff };
      const mockLoadItinerary = vi.fn();

      vi.mocked(chatApi.chatApi.send).mockResolvedValue(chatResponse);
      vi.mocked(chatApi.chatApi.applyChangeSet).mockResolvedValue(applyResponse);

      // Override context mock for this test
      vi.mocked(require('../../../contexts/UnifiedItineraryContext').useUnifiedItinerary)
        .mockReturnValue({
          state: { itinerary: { id: 'test-itin-123' }, isConnected: true },
          loadItinerary: mockLoadItinerary,
        });

      render(<NewChat />);

      await waitFor(() => {
        expect(screen.getByPlaceholderText(/Ask me about your trip/i)).toBeInTheDocument();
      });

      // Send message
      const textarea = screen.getByPlaceholderText(/Ask me about your trip/i);
      fireEvent.change(textarea, { target: { value: 'Move coffee to 4pm' } });
      fireEvent.click(screen.getByRole('button', { name: /send/i }));

      // Wait for apply button
      await waitFor(() => {
        expect(screen.getByText('Apply Changes')).toBeInTheDocument();
      });

      // Click apply
      const applyButton = screen.getByText('Apply Changes');
      fireEvent.click(applyButton);

      // Verify apply call
      await waitFor(() => {
        expect(chatApi.chatApi.applyChangeSet).toHaveBeenCalledWith(
          'test-itin-123',
          { changeSet: chatResponse.changeSet }
        );
      });

      // Verify itinerary reload
      expect(mockLoadItinerary).toHaveBeenCalledWith('test-itin-123');

      // Verify applied badge
      await waitFor(() => {
        expect(screen.getByText('✓ Applied')).toBeInTheDocument();
      });
    });
  });

  describe('User Journey 6: Chat History', () => {
    it('should load and display history on mount', async () => {
      const history = [
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

      vi.mocked(chatApi.chatApi.history).mockResolvedValue(history);

      render(<NewChat />);

      await waitFor(() => {
        expect(chatApi.chatApi.history).toHaveBeenCalledWith('test-itin-123');
      });

      await waitFor(() => {
        expect(screen.getByText('Previous question')).toBeInTheDocument();
        expect(screen.getByText('Previous answer')).toBeInTheDocument();
      });
    });
  });

  describe('User Journey 7: Clear History', () => {
    it('should clear history with confirmation', async () => {
      const confirmSpy = vi.spyOn(global, 'confirm').mockReturnValue(true);
      
      const history = [
        { sender: 'user', message: 'Old message', timestamp: Date.now() - 10000 }
      ];

      vi.mocked(chatApi.chatApi.history).mockResolvedValue(history);
      vi.mocked(chatApi.chatApi.clear).mockResolvedValue(undefined);

      render(<NewChat />);

      await waitFor(() => {
        expect(screen.getByText('Old message')).toBeInTheDocument();
      });

      // Find clear button by aria-label or title
      const clearButton = screen.getAllByTitle('Clear chat history')[0];
      fireEvent.click(clearButton);

      expect(confirmSpy).toHaveBeenCalled();

      await waitFor(() => {
        expect(chatApi.chatApi.clear).toHaveBeenCalledWith('test-itin-123');
      });

      await waitFor(() => {
        expect(screen.queryByText('Old message')).not.toBeInTheDocument();
      });

      confirmSpy.mockRestore();
    });
  });

  describe('User Journey 8: Detailed Diff Toggle', () => {
    it('should toggle between simple and detailed preview', async () => {
      const response = {
        intent: 'modify_activity',
        message: 'Updated',
        changeSet: {
          operations: [{ type: 'update', path: '/days/0/activities/0/title', value: 'New Title' }]
        },
        diff: {
          sections: [
            {
              type: 'modified',
              path: 'Day 1 / Activity',
              changes: [
                { type: 'removed', line: 'Old Title' },
                { type: 'added', line: 'New Title' }
              ]
            }
          ]
        },
        applied: false,
        needsDisambiguation: false,
        warnings: [],
        errors: []
      };

      vi.mocked(chatApi.chatApi.send).mockResolvedValue(response);

      render(<NewChat />);

      await waitFor(() => {
        expect(screen.getByPlaceholderText(/Ask me about your trip/i)).toBeInTheDocument();
      });

      const textarea = screen.getByPlaceholderText(/Ask me about your trip/i);
      fireEvent.change(textarea, { target: { value: 'Update activity' } });
      fireEvent.click(screen.getByRole('button', { name: /send/i }));

      await waitFor(() => {
        expect(screen.getByText(/proposed changes/i)).toBeInTheDocument();
      });

      // Click show details
      const showDetails = screen.getByText('Show Details');
      fireEvent.click(showDetails);

      await waitFor(() => {
        expect(screen.getByText('Hide Details')).toBeInTheDocument();
      });

      // Click hide details
      fireEvent.click(screen.getByText('Hide Details'));

      await waitFor(() => {
        expect(screen.getByText('Show Details')).toBeInTheDocument();
      });
    });
  });
});






