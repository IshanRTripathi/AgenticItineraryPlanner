import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { ChatInput } from '../ChatInput';

describe('ChatInput', () => {
  const mockOnSendMessage = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should render input and send button', () => {
    render(<ChatInput onSendMessage={mockOnSendMessage} disabled={false} />);
    
    expect(screen.getByTestId('chat-input')).toBeInTheDocument();
    expect(screen.getByTestId('message-input')).toBeInTheDocument();
    expect(screen.getByTestId('send-button')).toBeInTheDocument();
  });

  it('should call onSendMessage when send button is clicked', () => {
    render(<ChatInput onSendMessage={mockOnSendMessage} disabled={false} />);
    
    const input = screen.getByTestId('message-input');
    const sendButton = screen.getByTestId('send-button');
    
    fireEvent.change(input, { target: { value: 'Hello world' } });
    fireEvent.click(sendButton);
    
    expect(mockOnSendMessage).toHaveBeenCalledWith('Hello world');
  });

  it('should call onSendMessage when Enter key is pressed', () => {
    render(<ChatInput onSendMessage={mockOnSendMessage} disabled={false} />);
    
    const input = screen.getByTestId('message-input');
    
    fireEvent.change(input, { target: { value: 'Hello world' } });
    fireEvent.keyDown(input, { key: 'Enter', code: 'Enter' });
    
    expect(mockOnSendMessage).toHaveBeenCalledWith('Hello world');
  });

  it('should not call onSendMessage when Shift+Enter is pressed', () => {
    render(<ChatInput onSendMessage={mockOnSendMessage} disabled={false} />);
    
    const input = screen.getByTestId('message-input');
    
    fireEvent.change(input, { target: { value: 'Hello world' } });
    fireEvent.keyDown(input, { key: 'Enter', code: 'Enter', shiftKey: true });
    
    expect(mockOnSendMessage).not.toHaveBeenCalled();
  });

  it('should clear input after sending message', () => {
    render(<ChatInput onSendMessage={mockOnSendMessage} disabled={false} />);
    
    const input = screen.getByTestId('message-input') as HTMLInputElement;
    const sendButton = screen.getByTestId('send-button');
    
    fireEvent.change(input, { target: { value: 'Hello world' } });
    fireEvent.click(sendButton);
    
    expect(input.value).toBe('');
  });

  it('should not send empty messages', () => {
    render(<ChatInput onSendMessage={mockOnSendMessage} disabled={false} />);
    
    const input = screen.getByTestId('message-input');
    const sendButton = screen.getByTestId('send-button');
    
    fireEvent.change(input, { target: { value: '   ' } });
    fireEvent.click(sendButton);
    
    expect(mockOnSendMessage).not.toHaveBeenCalled();
  });

  it('should not send messages with only whitespace', () => {
    render(<ChatInput onSendMessage={mockOnSendMessage} disabled={false} />);
    
    const input = screen.getByTestId('message-input');
    const sendButton = screen.getByTestId('send-button');
    
    fireEvent.change(input, { target: { value: '\n\t  ' } });
    fireEvent.click(sendButton);
    
    expect(mockOnSendMessage).not.toHaveBeenCalled();
  });

  it('should be disabled when disabled prop is true', () => {
    render(<ChatInput onSendMessage={mockOnSendMessage} disabled={true} />);
    
    const input = screen.getByTestId('message-input');
    const sendButton = screen.getByTestId('send-button');
    
    expect(input).toBeDisabled();
    expect(sendButton).toBeDisabled();
  });

  it('should not call onSendMessage when disabled', () => {
    render(<ChatInput onSendMessage={mockOnSendMessage} disabled={true} />);
    
    const input = screen.getByTestId('message-input');
    const sendButton = screen.getByTestId('send-button');
    
    fireEvent.change(input, { target: { value: 'Hello world' } });
    fireEvent.click(sendButton);
    
    expect(mockOnSendMessage).not.toHaveBeenCalled();
  });

  it('should show placeholder text', () => {
    render(<ChatInput onSendMessage={mockOnSendMessage} disabled={false} />);
    
    const input = screen.getByTestId('message-input');
    expect(input).toHaveAttribute('placeholder', 'Type your message...');
  });

  it('should handle multiline input', () => {
    render(<ChatInput onSendMessage={mockOnSendMessage} disabled={false} />);
    
    const input = screen.getByTestId('message-input');
    
    fireEvent.change(input, { target: { value: 'Line 1\nLine 2' } });
    fireEvent.keyDown(input, { key: 'Enter', code: 'Enter' });
    
    expect(mockOnSendMessage).toHaveBeenCalledWith('Line 1\nLine 2');
  });

  it('should trim whitespace from messages', () => {
    render(<ChatInput onSendMessage={mockOnSendMessage} disabled={false} />);
    
    const input = screen.getByTestId('message-input');
    const sendButton = screen.getByTestId('send-button');
    
    fireEvent.change(input, { target: { value: '  Hello world  ' } });
    fireEvent.click(sendButton);
    
    expect(mockOnSendMessage).toHaveBeenCalledWith('Hello world');
  });
});
