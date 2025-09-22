/**
 * Main chat interface component for itinerary modifications
 * Provides a conversational interface for users to modify their itineraries
 */

import React, { useState, useRef, useEffect } from 'react';
import { 
  ChatMessage, 
  ChatState, 
  NodeCandidate, 
  ChatResponse,
  ChatIntent 
} from '../types/ChatTypes';
import { chatService } from '../services/chatService';
import { ChatMessageList } from './ChatMessageList';
import { ChatInput } from './ChatInput';
import { DisambiguationPanel } from './DisambiguationPanel';
import { ChangePreview } from './ChangePreview';
import './ChatInterface.css';

interface ChatInterfaceProps {
  itineraryId: string;
  selectedNodeId?: string;
  selectedDay?: number;
  scope?: 'trip' | 'day';
  onItineraryUpdate?: (itineraryId: string) => void;
  className?: string;
}

export const ChatInterface: React.FC<ChatInterfaceProps> = ({
  itineraryId,
  selectedNodeId,
  selectedDay,
  scope = 'trip',
  onItineraryUpdate,
  className = '',
}) => {
  const [chatState, setChatState] = useState<ChatState>({
    messages: [],
    isLoading: false,
    currentItineraryId: itineraryId,
    selectedNodeId,
    selectedDay,
    scope,
  });

  const [pendingDisambiguation, setPendingDisambiguation] = useState<{
    candidates: NodeCandidate[];
    originalText: string;
  } | null>(null);

  const [previewChanges, setPreviewChanges] = useState<{
    changeSet: any;
    diff: any;
  } | null>(null);

  const messagesEndRef = useRef<HTMLDivElement>(null);

  // Auto-scroll to bottom when new messages arrive
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [chatState.messages]);

  // Update chat state when props change
  useEffect(() => {
    setChatState(prev => ({
      ...prev,
      currentItineraryId: itineraryId,
      selectedNodeId,
      selectedDay,
      scope,
    }));
  }, [itineraryId, selectedNodeId, selectedDay, scope]);

  // Add welcome message when chat first loads
  useEffect(() => {
    if (chatState.messages.length === 0) {
      addMessage({
        text: `Hi! I'm your AI travel assistant. I can help you modify your itinerary by:\n\n• Adding or removing places\n• Changing the order of activities\n• Replacing attractions with alternatives\n• Adjusting timing and schedules\n• Answering questions about your trip\n\nJust tell me what you'd like to change!`,
        sender: 'assistant',
        intent: 'WELCOME'
      });
    }
  }, []);

  const addMessage = (message: Omit<ChatMessage, 'id' | 'timestamp'>) => {
    const newMessage: ChatMessage = {
      ...message,
      id: `msg_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`,
      timestamp: new Date(),
    };

    setChatState(prev => ({
      ...prev,
      messages: [...prev.messages, newMessage],
    }));
  };

  const handleSendMessage = async (text: string) => {
    if (!text.trim() || chatState.isLoading) return;

    // Add user message
    addMessage({
      text: text.trim(),
      sender: 'user',
    });

    setChatState(prev => ({ ...prev, isLoading: true }));

    try {
      const response: ChatResponse = await chatService.processMessage(
        itineraryId,
        text.trim(),
        {
          scope: chatState.scope,
          day: chatState.selectedDay,
          selectedNodeId: chatState.selectedNodeId,
          autoApply: false, // Always propose first
        }
      );

      // Add assistant response
      addMessage({
        text: response.message || 'No response received',
        sender: 'assistant',
        intent: response.intent,
        changeSet: response.changeSet,
        diff: response.diff,
        applied: response.applied,
        warnings: response.warnings,
        needsDisambiguation: response.needsDisambiguation,
        candidates: response.candidates,
      });

      // Handle disambiguation
      if (response.needsDisambiguation && response.candidates) {
        setPendingDisambiguation({
          candidates: response.candidates,
          originalText: text.trim(),
        });
      }

      // Handle change preview
      if (response.changeSet && response.diff && !response.applied) {
        setPreviewChanges({
          changeSet: response.changeSet,
          diff: response.diff,
        });
      }

      // Handle applied changes
      if (response.applied && onItineraryUpdate) {
        onItineraryUpdate(itineraryId);
      }

    } catch (error) {
      console.error('Chat error:', error);
      addMessage({
        text: `Sorry, I encountered an error: ${error instanceof Error ? error.message : 'Unknown error'}`,
        sender: 'assistant',
        intent: 'ERROR',
      });
    } finally {
      setChatState(prev => ({ ...prev, isLoading: false }));
    }
  };

  const handleDisambiguationSelect = async (candidate: NodeCandidate) => {
    if (!pendingDisambiguation) return;

    setChatState(prev => ({ ...prev, isLoading: true }));

    try {
      const response: ChatResponse = await chatService.handleDisambiguation(
        itineraryId,
        pendingDisambiguation.originalText,
        candidate,
        {
          scope: chatState.scope,
          day: chatState.selectedDay,
          autoApply: false,
        }
      );

      // Add assistant response
      addMessage({
        text: response.message,
        sender: 'assistant',
        intent: response.intent,
        changeSet: response.changeSet,
        diff: response.diff,
        applied: response.applied,
        warnings: response.warnings,
      });

      // Handle change preview
      if (response.changeSet && response.diff && !response.applied) {
        setPreviewChanges({
          changeSet: response.changeSet,
          diff: response.diff,
        });
      }

      // Handle applied changes
      if (response.applied && onItineraryUpdate) {
        onItineraryUpdate(itineraryId);
      }

    } catch (error) {
      console.error('Disambiguation error:', error);
      addMessage({
        text: `Sorry, I encountered an error: ${error instanceof Error ? error.message : 'Unknown error'}`,
        sender: 'assistant',
        intent: 'ERROR',
      });
    } finally {
      setChatState(prev => ({ ...prev, isLoading: false }));
      setPendingDisambiguation(null);
    }
  };

  const handleApplyChanges = async () => {
    if (!previewChanges) return;

    setChatState(prev => ({ ...prev, isLoading: true }));

    try {
      const result = await chatService.applyChanges(
        itineraryId,
        previewChanges.changeSet,
        {
          scope: chatState.scope,
          day: chatState.selectedDay,
        }
      );

      if (result.success) {
        addMessage({
          text: 'Changes applied successfully!',
          sender: 'assistant',
          intent: 'APPLY_SUCCESS',
        });

        if (onItineraryUpdate) {
          onItineraryUpdate(itineraryId);
        }
      } else {
        addMessage({
          text: `Failed to apply changes: ${result.message}`,
          sender: 'assistant',
          intent: 'ERROR',
        });
      }

    } catch (error) {
      console.error('Apply changes error:', error);
      addMessage({
        text: `Sorry, I encountered an error: ${error instanceof Error ? error.message : 'Unknown error'}`,
        sender: 'assistant',
        intent: 'ERROR',
      });
    } finally {
      setChatState(prev => ({ ...prev, isLoading: false }));
      setPreviewChanges(null);
    }
  };

  const handleCancelChanges = () => {
    setPreviewChanges(null);
    addMessage({
      text: 'Changes cancelled.',
      sender: 'assistant',
      intent: 'CANCEL',
    });
  };

  const clearChat = () => {
    setChatState(prev => ({
      ...prev,
      messages: [],
    }));
    setPendingDisambiguation(null);
    setPreviewChanges(null);
  };

  return (
    <div className={`chat-interface ${className}`}>
      <div className="chat-header">
        <div className="chat-context">
          <h3>AI Assistant</h3>
          <span className="scope-badge">{scope}</span>
          {selectedDay && <span className="day-badge">Day {selectedDay}</span>}
          {selectedNodeId && <span className="node-badge">Node Selected</span>}
        </div>
        <button 
          className="clear-chat-btn"
          onClick={clearChat}
          title="Clear chat history"
        >
          🗑️
        </button>
      </div>

      <div className="chat-messages-container">
        <ChatMessageList 
          messages={chatState.messages}
          isLoading={chatState.isLoading}
        />
        <div ref={messagesEndRef} />
      </div>

      {pendingDisambiguation && (
        <DisambiguationPanel
          candidates={pendingDisambiguation.candidates}
          onSelect={handleDisambiguationSelect}
          onCancel={() => setPendingDisambiguation(null)}
        />
      )}

      {previewChanges && (
        <ChangePreview
          changeSet={previewChanges.changeSet}
          diff={previewChanges.diff}
          onApply={handleApplyChanges}
          onCancel={handleCancelChanges}
        />
      )}

      <ChatInput
        onSendMessage={handleSendMessage}
        disabled={chatState.isLoading}
        placeholder={
          chatState.scope === 'day' 
            ? `Ask me to modify Day ${chatState.selectedDay || '?'}...`
            : "Ask me to modify your itinerary..."
        }
      />
    </div>
  );
};
