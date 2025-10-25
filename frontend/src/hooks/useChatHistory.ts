import { useState, useEffect, useCallback } from 'react';
import { apiClient } from '../services/apiClient';
import { logger } from '../utils/logger';

interface ChatMessage {
  id: string;
  sender: 'user' | 'assistant';
  text: string;
  timestamp: string;
  data?: any;
}

interface UseChatHistoryReturn {
  messages: ChatMessage[];
  loading: boolean;
  error: string | null;
  loadHistory: () => Promise<void>;
  clearHistory: () => Promise<void>;
  searchMessages: (query: string) => ChatMessage[];
}

export const useChatHistory = (itineraryId: string): UseChatHistoryReturn => {
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const loadHistory = useCallback(async () => {
    if (!itineraryId) return;

    try {
      setLoading(true);
      setError(null);
      // Placeholder - API method needs to be implemented
      const history: ChatMessage[] = [];
      setMessages(history);
    } catch (err: any) {
      setError(err.message || 'Failed to load chat history');
      console.error('Failed to load chat history:', err);
    } finally {
      setLoading(false);
    }
  }, [itineraryId]);

  const clearHistory = useCallback(async () => {
    if (!itineraryId) return;

    try {
      setLoading(true);
      setError(null);
      // Placeholder - API method needs to be implemented
      logger.info('Clear chat history', { 
        component: 'useChatHistory',
        itineraryId 
      });
      setMessages([]);
    } catch (err: any) {
      setError(err.message || 'Failed to clear chat history');
      console.error('Failed to clear chat history:', err);
    } finally {
      setLoading(false);
    }
  }, [itineraryId]);

  const searchMessages = useCallback((query: string): ChatMessage[] => {
    if (!query.trim()) return messages;
    
    const lowerQuery = query.toLowerCase();
    return messages.filter(msg =>
      msg.text.toLowerCase().includes(lowerQuery) ||
      msg.sender.toLowerCase().includes(lowerQuery)
    );
  }, [messages]);

  useEffect(() => {
    loadHistory();
  }, [loadHistory]);

  return {
    messages,
    loading,
    error,
    loadHistory,
    clearHistory,
    searchMessages
  };
};
