/**
 * Individual chat message component
 */

import React from 'react';
import { ChatMessage, ChatIntent } from '../types/ChatTypes';
import { chatService } from '../services/chatService';
import './ChatMessageItem.css';

interface ChatMessageItemProps {
  message: ChatMessage;
}

export const ChatMessageItem: React.FC<ChatMessageItemProps> = ({ message }) => {
  const isUser = message.sender === 'user';
  const intentInfo = message.intent ? chatService.parseIntent(message.intent) : null;

  const formatTime = (date: Date) => {
    return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  };

  const renderIntentBadge = () => {
    if (!intentInfo) return null;

    return (
      <div className={`intent-badge intent-${message.intent?.toLowerCase()}`}>
        <span className="intent-icon">{intentInfo.icon}</span>
        <span className="intent-description">{intentInfo.description}</span>
      </div>
    );
  };

  const renderWarnings = () => {
    if (!message.warnings || message.warnings.length === 0) return null;

    return (
      <div className="message-warnings">
        {message.warnings.map((warning, index) => (
          <div key={index} className="warning-item">
            ⚠️ {warning}
          </div>
        ))}
      </div>
    );
  };

  const renderAppliedBadge = () => {
    if (message.applied) {
      return (
        <div className="applied-badge">
          ✅ Applied
        </div>
      );
    }
    return null;
  };

  return (
    <div className={`chat-message ${isUser ? 'user-message' : 'assistant-message'}`}>
      <div className="message-header">
        <div className="message-sender">
          {isUser ? '👤 You' : '🤖 Assistant'}
        </div>
        <div className="message-time">
          {formatTime(message.timestamp)}
        </div>
      </div>
      
      <div className="message-content">
        <div className="message-text">
          {message.text}
        </div>
        
        {renderIntentBadge()}
        {renderWarnings()}
        {renderAppliedBadge()}
      </div>
    </div>
  );
};
