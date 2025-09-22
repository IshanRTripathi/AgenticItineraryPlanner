import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { ChatMessageList } from '../ChatMessageList';
import type { ChatMessage } from '../../types/ChatTypes';

// Mock the ChatMessageItem component
vi.mock('../ChatMessageItem', () => ({
  ChatMessageItem: ({ message }: { message: ChatMessage }) => (
    <div data-testid={`message-${message.id}`}>
      <div data-testid="message-content">{message.content}</div>
      <div data-testid="message-timestamp">{message.timestamp}</div>
      <div data-testid="message-sender">{message.sender}</div>
    </div>
  )
}));

describe('ChatMessageList', () => {
  const mockMessages: ChatMessage[] = [
    {
      id: '1',
      content: 'Hello! How can I help you?',
      sender: 'assistant',
      timestamp: '2024-01-01T10:00:00Z',
      type: 'text'
    },
    {
      id: '2',
      content: 'I need help with my itinerary',
      sender: 'user',
      timestamp: '2024-01-01T10:01:00Z',
      type: 'text'
    },
    {
      id: '3',
      content: 'I can help you with that! What would you like to do?',
      sender: 'assistant',
      timestamp: '2024-01-01T10:02:00Z',
      type: 'text'
    }
  ];

  it('should render all messages', () => {
    render(<ChatMessageList messages={mockMessages} />);
    
    expect(screen.getByTestId('chat-message-list')).toBeInTheDocument();
    expect(screen.getByTestId('message-1')).toBeInTheDocument();
    expect(screen.getByTestId('message-2')).toBeInTheDocument();
    expect(screen.getByTestId('message-3')).toBeInTheDocument();
  });

  it('should render empty state when no messages', () => {
    render(<ChatMessageList messages={[]} />);
    
    expect(screen.getByTestId('chat-message-list')).toBeInTheDocument();
    expect(screen.getByText('No messages yet. Start a conversation!')).toBeInTheDocument();
  });

  it('should display message content correctly', () => {
    render(<ChatMessageList messages={mockMessages} />);
    
    expect(screen.getByText('Hello! How can I help you?')).toBeInTheDocument();
    expect(screen.getByText('I need help with my itinerary')).toBeInTheDocument();
    expect(screen.getByText('I can help you with that! What would you like to do?')).toBeInTheDocument();
  });

  it('should display message timestamps', () => {
    render(<ChatMessageList messages={mockMessages} />);
    
    expect(screen.getByText('2024-01-01T10:00:00Z')).toBeInTheDocument();
    expect(screen.getByText('2024-01-01T10:01:00Z')).toBeInTheDocument();
    expect(screen.getByText('2024-01-01T10:02:00Z')).toBeInTheDocument();
  });

  it('should display message senders', () => {
    render(<ChatMessageList messages={mockMessages} />);
    
    expect(screen.getAllByText('assistant')).toHaveLength(2);
    expect(screen.getByText('user')).toBeInTheDocument();
  });

  it('should handle messages with suggestions', () => {
    const messagesWithSuggestions: ChatMessage[] = [
      {
        id: '1',
        content: 'Here are some suggestions:',
        sender: 'assistant',
        timestamp: '2024-01-01T10:00:00Z',
        type: 'text',
        suggestions: ['Visit the museum', 'Go to the park', 'Try the restaurant']
      }
    ];

    render(<ChatMessageList messages={messagesWithSuggestions} />);
    
    expect(screen.getByText('Here are some suggestions:')).toBeInTheDocument();
    expect(screen.getByText('Visit the museum')).toBeInTheDocument();
    expect(screen.getByText('Go to the park')).toBeInTheDocument();
    expect(screen.getByText('Try the restaurant')).toBeInTheDocument();
  });

  it('should handle messages with change sets', () => {
    const messagesWithChanges: ChatMessage[] = [
      {
        id: '1',
        content: 'I can move the museum visit to tomorrow. Would you like me to apply this change?',
        sender: 'assistant',
        timestamp: '2024-01-01T10:00:00Z',
        type: 'text',
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
      }
    ];

    render(<ChatMessageList messages={messagesWithChanges} />);
    
    expect(screen.getByText('I can move the museum visit to tomorrow. Would you like me to apply this change?')).toBeInTheDocument();
  });

  it('should handle messages with disambiguation', () => {
    const messagesWithDisambiguation: ChatMessage[] = [
      {
        id: '1',
        content: 'Which restaurant did you mean?',
        sender: 'assistant',
        timestamp: '2024-01-01T10:00:00Z',
        type: 'text',
        disambiguation: {
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
      }
    ];

    render(<ChatMessageList messages={messagesWithDisambiguation} />);
    
    expect(screen.getByText('Which restaurant did you mean?')).toBeInTheDocument();
    expect(screen.getByText('La Boqueria')).toBeInTheDocument();
    expect(screen.getByText('El Nacional')).toBeInTheDocument();
  });

  it('should handle mixed message types', () => {
    const mixedMessages: ChatMessage[] = [
      {
        id: '1',
        content: 'Hello!',
        sender: 'user',
        timestamp: '2024-01-01T10:00:00Z',
        type: 'text'
      },
      {
        id: '2',
        content: 'Hi there!',
        sender: 'assistant',
        timestamp: '2024-01-01T10:01:00Z',
        type: 'text',
        suggestions: ['How can I help?']
      },
      {
        id: '3',
        content: 'I can help you with that.',
        sender: 'assistant',
        timestamp: '2024-01-01T10:02:00Z',
        type: 'text',
        changeSet: {
          operations: [
            {
              type: 'add',
              nodeId: 'new-node',
              day: 1
            }
          ]
        }
      }
    ];

    render(<ChatMessageList messages={mixedMessages} />);
    
    expect(screen.getByText('Hello!')).toBeInTheDocument();
    expect(screen.getByText('Hi there!')).toBeInTheDocument();
    expect(screen.getByText('How can I help?')).toBeInTheDocument();
    expect(screen.getByText('I can help you with that.')).toBeInTheDocument();
  });
});
