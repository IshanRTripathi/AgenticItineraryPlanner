/**
 * Component for displaying chat messages
 */

import React from 'react';
import { ChatMessage } from '../types/ChatTypes';
import { ChatMessageItem } from './ChatMessageItem';
import './ChatMessageList.css';

interface ChatMessageListProps {
  messages: ChatMessage[];
  isLoading: boolean;
}

export const ChatMessageList: React.FC<ChatMessageListProps> = ({
  messages,
  isLoading,
}) => {
  return (
    <div className="chat-message-list">
      {messages.length === 0 && !isLoading && (
        <div className="empty-state">
          <div className="empty-state-icon">💬</div>
          <h4>Start a conversation</h4>
          <p>Ask me to modify your itinerary!</p>
          <div className="suggestion-chips">
            <span className="suggestion-chip">"Move lunch to 2pm"</span>
            <span className="suggestion-chip">"Add a museum visit"</span>
            <span className="suggestion-chip">"Remove the beach activity"</span>
            <span className="suggestion-chip">"What's my plan for today?"</span>
          </div>
        </div>
      )}
      
      {messages.map((message) => (
        <ChatMessageItem key={message.id} message={message} />
      ))}
      
      {isLoading && (
        <div className="loading-message">
          <div className="typing-indicator">
            <span></span>
            <span></span>
            <span></span>
          </div>
          <span className="loading-text">Thinking...</span>
        </div>
      )}
    </div>
  );
};
