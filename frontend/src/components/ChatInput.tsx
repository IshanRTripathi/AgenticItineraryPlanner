/**
 * Chat input component for sending messages
 */

import React, { useState, useRef, useEffect } from 'react';
import { hasSpecialCharacters, getDisplayText } from '../utils/encodingUtils';
import './ChatInput.css';

interface ChatInputProps {
  onSendMessage: (message: string) => void;
  disabled?: boolean;
  placeholder?: string;
}

export const ChatInput: React.FC<ChatInputProps> = ({
  onSendMessage,
  disabled = false,
  placeholder = "Type your message...",
}) => {
  const [message, setMessage] = useState('');
  const [showEncodingWarning, setShowEncodingWarning] = useState(false);
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (message.trim() && !disabled) {
      onSendMessage(message);
      setMessage('');
      setShowEncodingWarning(false);
    }
  };

  const handleMessageChange = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
    const newMessage = e.target.value;
    setMessage(newMessage);
    setShowEncodingWarning(hasSpecialCharacters(newMessage));
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSubmit(e);
    }
  };

  // Auto-resize textarea
  useEffect(() => {
    if (textareaRef.current) {
      textareaRef.current.style.height = 'auto';
      textareaRef.current.style.height = `${textareaRef.current.scrollHeight}px`;
    }
  }, [message]);

  const isDisabled = disabled || !message.trim();

  return (
    <form className="chat-input-form" onSubmit={handleSubmit}>
      <div className="chat-input-container">
        <textarea
          ref={textareaRef}
          value={message}
          onChange={handleMessageChange}
          onKeyDown={handleKeyDown}
          placeholder={placeholder}
          disabled={disabled}
          className="chat-input-textarea"
          rows={1}
          maxLength={1000}
        />
        <button
          type="submit"
          disabled={isDisabled}
          className={`send-button ${isDisabled ? 'disabled' : ''}`}
          title="Send message (Enter)"
        >
          {disabled ? (
            <span className="loading-spinner">⏳</span>
          ) : (
            <span>📤</span>
          )}
        </button>
      </div>
      {showEncodingWarning && (
        <div className="encoding-warning">
          <span className="warning-icon">⚠️</span>
          <span className="warning-text">
            Special characters detected. They will be converted to standard characters for better compatibility.
          </span>
        </div>
      )}
      <div className="input-footer">
        <span className="character-count">
          {message.length}/1000
        </span>
        <span className="input-hint">
          Press Enter to send, Shift+Enter for new line
        </span>
      </div>
    </form>
  );
};
