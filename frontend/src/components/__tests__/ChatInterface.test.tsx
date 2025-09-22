import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { ChatInterface } from '../ChatInterface';
import * as chatService from '../../services/chatService';
import type { ChatResponse } from '../../types/ChatTypes';

// Mock the chat service
vi.mock('../../services/chatService', () => ({
  sendMessage: vi.fn()
}));

// Mock the other chat components
vi.mock('../ChatMessageList', () => ({
  ChatMessageList: ({ messages }: { messages: any[] }) => (
    <div data-testid="chat-message-list">
      {messages.map((msg, index) => (
        <div key={index} data-testid={`message-${index}`}>
          {msg.content}
        </div>
      ))}
    </div>
  )
}));

vi.mock('../ChatInput', () => ({
  ChatInput: ({ onSendMessage, disabled }: { onSendMessage: (message: string) => void; disabled: boolean }) => (
    <div data-testid="chat-input">
      <input 
        data-testid="message-input" 
        disabled={disabled}
        onChange={(e) => {
          // Store the value for testing
          (e.target as any).testValue = e.target.value;
        }}
      />
      <button 
        data-testid="send-button" 
        onClick={() => {
          const input = document.querySelector('[data-testid="message-input"]') as HTMLInputElement;
          onSendMessage(input.testValue || input.value);
        }}
        disabled={disabled}
      >
        Send
      </button>
    </div>
  )
}));

vi.mock('../DisambiguationPanel', () => ({
  DisambiguationPanel: ({ candidates, onSelect }: { candidates: any[]; onSelect: (id: string) => void }) => (
    <div data-testid="disambiguation-panel">
      {candidates.map((candidate) => (
        <button 
          key={candidate.id} 
          data-testid={`candidate-${candidate.id}`}
          onClick={() => onSelect(candidate.id)}
        >
          {candidate.title}
        </button>
      ))}
    </div>
  )
}));

vi.mock('../ChangePreview', () => ({
  ChangePreview: ({ changeSet, onApply, onReject }: { changeSet: any; onApply: () => void; onReject: () => void }) => (
    <div data-testid="change-preview">
      <button data-testid="apply-button" onClick={onApply}>Apply</button>
      <button data-testid="reject-button" onClick={onReject}>Reject</button>
    </div>
  )
}));

describe('ChatInterface', () => {
  const mockProps = {
    itineraryId: 'test-itinerary',
    onItineraryUpdate: vi.fn()
  };

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should render chat interface with initial state', () => {
    render(<ChatInterface {...mockProps} />);
    
    expect(screen.getByTestId('chat-interface')).toBeInTheDocument();
    expect(screen.getByTestId('chat-message-list')).toBeInTheDocument();
    expect(screen.getByTestId('chat-input')).toBeInTheDocument();
  });

  it('should send a message and display response', async () => {
    const mockResponse: ChatResponse = {
      message: 'Hello! How can I help you?',
      intent: {
        type: 'general',
        confidence: 0.9,
        entities: {}
      },
      suggestions: ['What would you like to do?'],
      timestamp: new Date().toISOString()
    };

    (chatService.sendMessage as any).mockResolvedValueOnce(mockResponse);

    render(<ChatInterface {...mockProps} />);
    
    const input = screen.getByTestId('message-input');
    const sendButton = screen.getByTestId('send-button');
    
    fireEvent.change(input, { target: { value: 'Hello' } });
    fireEvent.click(sendButton);

    await waitFor(() => {
      expect(chatService.sendMessage).toHaveBeenCalledWith({
        message: 'Hello',
        itineraryId: 'test-itinerary',
        context: undefined
      });
    });

    await waitFor(() => {
      expect(screen.getByText('Hello! How can I help you?')).toBeInTheDocument();
    });
  });

  it('should show disambiguation panel when needed', async () => {
    const mockResponse: ChatResponse = {
      message: 'Which restaurant did you mean?',
      intent: {
        type: 'disambiguation',
        confidence: 0.8,
        entities: {
          candidates: [
            {
              id: 'restaurant-1',
              title: 'La Boqueria',
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
      suggestions: [],
      timestamp: new Date().toISOString()
    };

    (chatService.sendMessage as any).mockResolvedValueOnce(mockResponse);

    render(<ChatInterface {...mockProps} />);
    
    const input = screen.getByTestId('message-input');
    const sendButton = screen.getByTestId('send-button');
    
    fireEvent.change(input, { target: { value: 'Show me the restaurant' } });
    fireEvent.click(sendButton);

    await waitFor(() => {
      expect(screen.getByTestId('disambiguation-panel')).toBeInTheDocument();
      expect(screen.getByTestId('candidate-restaurant-1')).toBeInTheDocument();
      expect(screen.getByTestId('candidate-restaurant-2')).toBeInTheDocument();
    });
  });

  it('should show change preview when changes are proposed', async () => {
    const mockResponse: ChatResponse = {
      message: 'I can move the museum visit to tomorrow. Would you like me to apply this change?',
      intent: {
        type: 'move_node',
        confidence: 0.95,
        entities: {
          nodeId: 'museum-node',
          targetDay: 2
        }
      },
      suggestions: [],
      timestamp: new Date().toISOString(),
      changeSet: {
        operations: [
          {
            type: 'move',
            nodeId: 'museum-node',
            fromDay: 1,
            toDay: 2
          }
        ]
      }
    };

    (chatService.sendMessage as any).mockResolvedValueOnce(mockResponse);

    render(<ChatInterface {...mockProps} />);
    
    const input = screen.getByTestId('message-input');
    const sendButton = screen.getByTestId('send-button');
    
    fireEvent.change(input, { target: { value: 'Move museum to tomorrow' } });
    fireEvent.click(sendButton);

    await waitFor(() => {
      expect(screen.getByTestId('change-preview')).toBeInTheDocument();
      expect(screen.getByTestId('apply-button')).toBeInTheDocument();
      expect(screen.getByTestId('reject-button')).toBeInTheDocument();
    });
  });

  it('should handle disambiguation selection', async () => {
    const mockResponse: ChatResponse = {
      message: 'Which restaurant did you mean?',
      intent: {
        type: 'disambiguation',
        confidence: 0.8,
        entities: {
          candidates: [
            {
              id: 'restaurant-1',
              title: 'La Boqueria',
              type: 'restaurant',
              day: 1,
              time: '12:00'
            }
          ]
        }
      },
      suggestions: [],
      timestamp: new Date().toISOString()
    };

    const mockFollowUpResponse: ChatResponse = {
      message: 'Here are the details for La Boqueria',
      intent: {
        type: 'show_details',
        confidence: 0.9,
        entities: { nodeId: 'restaurant-1' }
      },
      suggestions: [],
      timestamp: new Date().toISOString()
    };

    (chatService.sendMessage as any)
      .mockResolvedValueOnce(mockResponse)
      .mockResolvedValueOnce(mockFollowUpResponse);

    render(<ChatInterface {...mockProps} />);
    
    const input = screen.getByTestId('message-input');
    const sendButton = screen.getByTestId('send-button');
    
    fireEvent.change(input, { target: { value: 'Show me the restaurant' } });
    fireEvent.click(sendButton);

    await waitFor(() => {
      expect(screen.getByTestId('disambiguation-panel')).toBeInTheDocument();
    });

    const candidateButton = screen.getByTestId('candidate-restaurant-1');
    fireEvent.click(candidateButton);

    await waitFor(() => {
      expect(chatService.sendMessage).toHaveBeenCalledWith({
        message: 'restaurant-1',
        itineraryId: 'test-itinerary',
        context: undefined
      });
    });
  });

  it('should handle loading state', async () => {
    // Mock a delayed response
    (chatService.sendMessage as any).mockImplementation(() => 
      new Promise(resolve => setTimeout(() => resolve({
        message: 'Response',
        intent: { type: 'general', confidence: 0.9, entities: {} },
        suggestions: [],
        timestamp: new Date().toISOString()
      }), 100))
    );

    render(<ChatInterface {...mockProps} />);
    
    const input = screen.getByTestId('message-input');
    const sendButton = screen.getByTestId('send-button');
    
    fireEvent.change(input, { target: { value: 'Test message' } });
    fireEvent.click(sendButton);

    // Check that input is disabled during loading
    expect(input).toBeDisabled();
    expect(sendButton).toBeDisabled();
  });

  it('should handle errors gracefully', async () => {
    (chatService.sendMessage as any).mockRejectedValueOnce(new Error('Network error'));

    render(<ChatInterface {...mockProps} />);
    
    const input = screen.getByTestId('message-input');
    const sendButton = screen.getByTestId('send-button');
    
    fireEvent.change(input, { target: { value: 'Test message' } });
    fireEvent.click(sendButton);

    await waitFor(() => {
      expect(screen.getByText(/error/i)).toBeInTheDocument();
    });
  });
});
