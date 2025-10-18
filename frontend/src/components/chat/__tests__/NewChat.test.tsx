import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { NewChat } from '../NewChat';
import { UnifiedItineraryProvider } from '../../../contexts/UnifiedItineraryContext';
import * as chatApi from '../../../services/chatApi';

// Mock the chat API
vi.mock('../../../services/chatApi', () => ({
  chatApi: {
    send: vi.fn(),
    history: vi.fn(),
    persist: vi.fn(),
    clear: vi.fn(),
    applyChangeSet: vi.fn(),
  },
}));

// Mock UnifiedItineraryContext
vi.mock('../../../contexts/UnifiedItineraryContext', () => {
  return {
    UnifiedItineraryProvider: ({ children }: any) => <div>{children}</div>,
    useUnifiedItinerary: () => ({
      state: {
        itinerary: { id: 'test-itinerary-123' },
        isConnected: true,
        selectedDay: null,
        selectedNodeId: null,
      },
      loadItinerary: vi.fn(),
    }),
  };
});

describe('NewChat Component', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    (chatApi.chatApi.history as any).mockResolvedValue([]);
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('should render the chat interface', async () => {
    render(<NewChat />);
    
    await waitFor(() => {
      expect(screen.getByText('AI Travel Assistant')).toBeInTheDocument();
    });
  });

  it('should show empty state with suggestion chips', async () => {
    render(<NewChat />);
    
    await waitFor(() => {
      expect(screen.getByText('Start a conversation')).toBeInTheDocument();
      expect(screen.getByText('"Move lunch to 2pm"')).toBeInTheDocument();
      expect(screen.getByText('"Add a museum visit on day 2"')).toBeInTheDocument();
    });
  });

  it('should prefill input when clicking a suggestion chip', async () => {
    render(<NewChat />);
    
    await waitFor(() => {
      const chip = screen.getByText('"Move lunch to 2pm"');
      fireEvent.click(chip);
    });
    
    const textarea = screen.getByPlaceholderText('Ask me about your trip…');
    expect(textarea).toHaveValue('Move lunch to 2pm');
  });

  it('should load chat history on mount', async () => {
    const mockHistory = [
      {
        id: 'msg1',
        message: 'Hello',
        sender: 'user' as const,
        timestamp: Date.now(),
      },
      {
        id: 'msg2',
        message: 'Hi there!',
        sender: 'assistant' as const,
        timestamp: Date.now(),
      },
    ];
    
    (chatApi.chatApi.history as any).mockResolvedValue(mockHistory);
    
    render(<NewChat />);
    
    await waitFor(() => {
      expect(chatApi.chatApi.history).toHaveBeenCalledWith('test-itinerary-123');
      expect(screen.getByText('Hello')).toBeInTheDocument();
      expect(screen.getByText('Hi there!')).toBeInTheDocument();
    });
  });

  it('should send a message when user clicks send', async () => {
    const mockResponse = {
      id: 'msg-response',
      message: 'Message received!',
      sender: 'assistant' as const,
      timestamp: Date.now(),
    };
    
    (chatApi.chatApi.send as any).mockResolvedValue(mockResponse);
    (chatApi.chatApi.persist as any).mockResolvedValue(undefined);
    
    render(<NewChat />);
    
    await waitFor(() => {
      expect(screen.getByPlaceholderText('Ask me about your trip…')).toBeInTheDocument();
    });
    
    const textarea = screen.getByPlaceholderText('Ask me about your trip…');
    const sendButton = screen.getByRole('button', { name: /send/i });
    
    fireEvent.change(textarea, { target: { value: 'Test message' } });
    fireEvent.click(sendButton);
    
    await waitFor(() => {
      expect(chatApi.chatApi.persist).toHaveBeenCalledWith(
        'test-itinerary-123',
        expect.objectContaining({
          message: 'Test message',
          sender: 'user',
        })
      );
      
      expect(chatApi.chatApi.send).toHaveBeenCalledWith(
        'test-itinerary-123',
        expect.objectContaining({
          text: 'Test message',
          itineraryId: 'test-itinerary-123',
        })
      );
    });
  });

  it('should show error message when send fails', async () => {
    (chatApi.chatApi.send as any).mockRejectedValue(new Error('Failed to send message (404): Not Found'));
    (chatApi.chatApi.persist as any).mockResolvedValue(undefined);
    
    render(<NewChat />);
    
    await waitFor(() => {
      expect(screen.getByPlaceholderText('Ask me about your trip…')).toBeInTheDocument();
    });
    
    const textarea = screen.getByPlaceholderText('Ask me about your trip…');
    const sendButton = screen.getByRole('button', { name: /send/i });
    
    fireEvent.change(textarea, { target: { value: 'Test message' } });
    fireEvent.click(sendButton);
    
    await waitFor(() => {
      expect(screen.getByText(/Failed to send message \(404\): Not Found/i)).toBeInTheDocument();
    });
  });

  it('should display intent badge when message has intent', async () => {
    const mockHistory = [
      {
        id: 'msg1',
        message: 'Move lunch to 2pm',
        sender: 'assistant' as const,
        timestamp: Date.now(),
        intent: 'MODIFY_ACTIVITY',
      },
    ];
    
    (chatApi.chatApi.history as any).mockResolvedValue(mockHistory);
    
    render(<NewChat />);
    
    await waitFor(() => {
      expect(screen.getByText('Intent:')).toBeInTheDocument();
      expect(screen.getByText('MODIFY_ACTIVITY')).toBeInTheDocument();
    });
  });

  it('should display warnings when present', async () => {
    const mockHistory = [
      {
        id: 'msg1',
        message: 'Activity moved',
        sender: 'assistant' as const,
        timestamp: Date.now(),
        warnings: ['This may conflict with dinner', 'Restaurant may be fully booked'],
      },
    ];
    
    (chatApi.chatApi.history as any).mockResolvedValue(mockHistory);
    
    render(<NewChat />);
    
    await waitFor(() => {
      expect(screen.getByText('Warnings')).toBeInTheDocument();
      expect(screen.getByText('This may conflict with dinner')).toBeInTheDocument();
      expect(screen.getByText('Restaurant may be fully booked')).toBeInTheDocument();
    });
  });

  it('should display errors when present', async () => {
    const mockHistory = [
      {
        id: 'msg1',
        message: 'Failed to process',
        sender: 'assistant' as const,
        timestamp: Date.now(),
        errors: ['Activity not found on day 2'],
      },
    ];
    
    (chatApi.chatApi.history as any).mockResolvedValue(mockHistory);
    
    render(<NewChat />);
    
    await waitFor(() => {
      expect(screen.getByText('Errors')).toBeInTheDocument();
      expect(screen.getByText('Activity not found on day 2')).toBeInTheDocument();
    });
  });

  it('should show applied badge when changes are applied', async () => {
    const mockHistory = [
      {
        id: 'msg1',
        message: 'Lunch moved to 2pm',
        sender: 'assistant' as const,
        timestamp: Date.now(),
        applied: true,
      },
    ];
    
    (chatApi.chatApi.history as any).mockResolvedValue(mockHistory);
    
    render(<NewChat />);
    
    await waitFor(() => {
      expect(screen.getByText('Changes Applied')).toBeInTheDocument();
    });
  });

  it('should display disambiguation candidates', async () => {
    const mockHistory = [
      {
        id: 'msg1',
        message: 'Which one did you mean?',
        sender: 'assistant' as const,
        timestamp: Date.now(),
        candidates: [
          { id: '1', title: 'Lunch at Bistro', day: 2, location: 'Downtown' },
          { id: '2', title: 'Lunch at Café', day: 2, location: 'Old Town' },
        ],
      },
    ];
    
    (chatApi.chatApi.history as any).mockResolvedValue(mockHistory);
    
    render(<NewChat />);
    
    await waitFor(() => {
      expect(screen.getByText('Did you mean one of these?')).toBeInTheDocument();
      expect(screen.getByText('Lunch at Bistro')).toBeInTheDocument();
      expect(screen.getByText('Lunch at Café')).toBeInTheDocument();
    });
  });

  it('should prefill input when selecting a candidate', async () => {
    const mockHistory = [
      {
        id: 'msg1',
        message: 'Which one?',
        sender: 'assistant' as const,
        timestamp: Date.now(),
        candidates: [
          { id: '1', title: 'Lunch at Bistro', day: 2 },
        ],
      },
    ];
    
    (chatApi.chatApi.history as any).mockResolvedValue(mockHistory);
    
    render(<NewChat />);
    
    await waitFor(() => {
      const selectButton = screen.getAllByText('Select')[0];
      fireEvent.click(selectButton);
    });
    
    const textarea = screen.getByPlaceholderText('Ask me about your trip…');
    expect(textarea).toHaveValue('Use "Lunch at Bistro" from day 2');
  });

  it('should show change preview when changeSet is present', async () => {
    const mockHistory = [
      {
        id: 'msg1',
        message: "I'll move your lunch",
        sender: 'assistant' as const,
        timestamp: Date.now(),
        changeSet: { operation: 'modify', activityId: '123' },
        applied: false,
      },
    ];
    
    (chatApi.chatApi.history as any).mockResolvedValue(mockHistory);
    
    render(<NewChat />);
    
    await waitFor(() => {
      expect(screen.getByText('Preview Changes')).toBeInTheDocument();
      expect(screen.getByText('Apply Changes')).toBeInTheDocument();
    });
  });

  it('should apply changes when clicking apply button', async () => {
    const mockHistory = [
      {
        id: 'msg1',
        message: "I'll move your lunch",
        sender: 'assistant' as const,
        timestamp: Date.now(),
        changeSet: { operation: 'modify', activityId: '123' },
        applied: false,
      },
    ];
    
    (chatApi.chatApi.history as any).mockResolvedValue(mockHistory);
    (chatApi.chatApi.applyChangeSet as any).mockResolvedValue({ toVersion: 2, diff: {} });
    
    render(<NewChat />);
    
    await waitFor(() => {
      const applyButton = screen.getByText('Apply Changes');
      fireEvent.click(applyButton);
    });
    
    await waitFor(() => {
      expect(chatApi.chatApi.applyChangeSet).toHaveBeenCalledWith(
        'test-itinerary-123',
        expect.objectContaining({
          changeSet: { operation: 'modify', activityId: '123' },
        })
      );
    });
  });

  it('should clear chat history when clicking clear button', async () => {
    const mockHistory = [
      {
        id: 'msg1',
        message: 'Test',
        sender: 'user' as const,
        timestamp: Date.now(),
      },
    ];
    
    (chatApi.chatApi.history as any).mockResolvedValue(mockHistory);
    (chatApi.chatApi.clear as any).mockResolvedValue(undefined);
    
    // Mock window.confirm
    global.confirm = vi.fn(() => true);
    
    render(<NewChat />);
    
    await waitFor(() => {
      expect(screen.getByText('Test')).toBeInTheDocument();
    });
    
    const clearButton = screen.getByTitle('Clear chat history');
    fireEvent.click(clearButton);
    
    await waitFor(() => {
      expect(chatApi.chatApi.clear).toHaveBeenCalledWith('test-itinerary-123');
    });
  });

  // Removed: This test was trying to override the mock incorrectly
  // The integration tests cover real user interaction scenarios more accurately
});

