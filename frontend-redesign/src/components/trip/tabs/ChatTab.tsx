/**
 * Chat Tab Component
 * AI-powered chat interface for itinerary modifications
 * Week 11: Task 24 - Chat Interface
 */

import React, { useRef, useEffect, useState } from 'react';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Loader2, MessageSquare, Download, Trash2, ChevronDown } from 'lucide-react';
import { useUnifiedItinerary } from '@/contexts/UnifiedItineraryContext';
import { ChatMessageComponent } from '@/components/chat/ChatMessage';
import { useScrollDetection } from '@/hooks/useScrollDetection';
import { Textarea } from '@/components/ui/textarea';

const INITIAL_DISPLAY_COUNT = 10;
const LOAD_MORE_COUNT = 10;

export function ChatTab() {
  const { state, sendChatMessage, clearChatHistory } = useUnifiedItinerary();
  const { chatMessages, isConnected, itinerary } = state;

  const [input, setInput] = useState('');
  const [isSending, setIsSending] = useState(false);
  const [detailedViewMessageId, setDetailedViewMessageId] = useState<string>();
  const [applyingMessageId, setApplyingMessageId] = useState<string>();
  const [displayCount, setDisplayCount] = useState(INITIAL_DISPLAY_COUNT);

  const messagesContainerRef = useRef<HTMLDivElement>(null);
  const endRef = useRef<HTMLDivElement>(null);

  const scrollToBottom = () => endRef.current?.scrollIntoView({ behavior: 'smooth' });

  const { isNearTop } = useScrollDetection(messagesContainerRef, {
    threshold: 50,
    enabled: chatMessages.length > 0 && !!messagesContainerRef.current
  });

  // Auto-scroll to bottom for new messages
  useEffect(() => {
    scrollToBottom();
  }, [chatMessages.length]);

  const displayedMessages = chatMessages.slice(-displayCount);
  const hasMoreMessages = chatMessages.length > displayCount;

  const handleLoadMore = () => {
    const newDisplayCount = Math.min(
      displayCount + LOAD_MORE_COUNT,
      chatMessages.length
    );
    setDisplayCount(newDisplayCount);
  };

  const handleSend = async () => {
    const text = input.trim();
    if (!text || isSending) return;

    setIsSending(true);
    setInput('');

    try {
      await sendChatMessage(text);
    } catch (error) {
      console.error('Failed to send message:', error);
    } finally {
      setIsSending(false);
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  const handleApplyChanges = async (messageId: string, changeSet: any) => {
    setApplyingMessageId(messageId);
    try {
      // Apply changes through context
      // The UnifiedItineraryContext will handle the API call
      console.log('Applying changes:', changeSet);
      // TODO: Implement apply changes in context
    } catch (error) {
      console.error('Failed to apply changes:', error);
    } finally {
      setApplyingMessageId(undefined);
    }
  };

  const handleClearHistory = async () => {
    const confirmed = window.confirm('Clear all chat history? This cannot be undone.');
    if (!confirmed) return;

    try {
      await clearChatHistory();
    } catch (error) {
      console.error('Failed to clear history:', error);
    }
  };

  const handleExportHistory = () => {
    const dataStr = JSON.stringify(chatMessages, null, 2);
    const dataUri = 'data:application/json;charset=utf-8,' + encodeURIComponent(dataStr);
    const exportFileDefaultName = `chat-history-${itinerary?.id}-${Date.now()}.json`;
    const linkElement = document.createElement('a');
    linkElement.setAttribute('href', dataUri);
    linkElement.setAttribute('download', exportFileDefaultName);
    linkElement.click();
  };

  return (
    <div className="h-full flex flex-col bg-gray-50 rounded-lg overflow-hidden">
      {/* Header */}
      <div className="p-4 border-b bg-white flex items-center justify-between shadow-sm">
        <div className="flex items-center gap-3">
          <MessageSquare className="h-5 w-5 text-blue-600" />
          <span className="text-lg font-semibold">AI Travel Assistant</span>
          {isConnected && (
            <Badge variant="outline" className="text-green-600 border-green-600">
              Live
            </Badge>
          )}
          {isSending && (
            <Badge variant="outline" className="text-blue-600 border-blue-600">
              Processing
            </Badge>
          )}
        </div>
        
        <div className="flex items-center gap-2">
          {chatMessages.length > 0 && (
            <>
              <Button 
                size="sm" 
                variant="outline"
                onClick={handleExportHistory}
                title="Export chat history"
              >
                <Download className="h-4 w-4" />
              </Button>
              <Button 
                size="sm" 
                variant="outline"
                onClick={handleClearHistory}
                title="Clear chat history"
              >
                <Trash2 className="h-4 w-4" />
              </Button>
            </>
          )}
        </div>
      </div>

      {/* Messages */}
      <div ref={messagesContainerRef} className="flex-1 overflow-y-auto p-4 relative">
        {/* Load More Button */}
        {hasMoreMessages && isNearTop && (
          <div className="sticky top-0 z-10 flex justify-center mb-4">
            <Button
              size="sm"
              variant="outline"
              onClick={handleLoadMore}
              className="bg-white shadow-md hover:shadow-lg transition-shadow"
            >
              <ChevronDown className="h-4 w-4 mr-2 rotate-180" />
              Load {Math.min(LOAD_MORE_COUNT, chatMessages.length - displayCount)} More Messages
            </Button>
          </div>
        )}

        {chatMessages.length === 0 && (
          <div className="text-center text-gray-600 mt-12">
            <MessageSquare className="h-16 w-16 mx-auto mb-4 text-gray-300" />
            <p className="text-lg font-medium mb-2">Start a conversation</p>
            <p className="text-sm text-gray-500 mb-6">Ask me to modify your itinerary!</p>
            <div className="max-w-2xl mx-auto">
              <p className="text-xs text-gray-400 mb-3">Try these examples:</p>
              <div className="flex flex-wrap gap-2 justify-center">
                {[
                  'Move lunch to 2pm',
                  'Add a museum visit on day 2',
                  'Remove the beach activity',
                  "What's my plan for today?",
                  'Find a romantic restaurant',
                  'Add more outdoor activities'
                ].map(s => (
                  <button 
                    key={s} 
                    className="px-4 py-2 rounded-full bg-white border border-gray-200 hover:bg-blue-50 hover:border-blue-300 text-sm transition-all shadow-sm hover:shadow"
                    onClick={() => setInput(s)}
                  >
                    "{s}"
                  </button>
                ))}
              </div>
            </div>
          </div>
        )}

        {hasMoreMessages && displayedMessages.length > 0 && (
          <div className="text-center text-xs text-gray-500 mb-4 py-2">
            Showing {displayedMessages.length} of {chatMessages.length} messages
          </div>
        )}
        
        {displayedMessages.map((message, index) => {
          const messageId = message.id || `msg-${index}`;
          // Create unique key combining id, timestamp, and index to prevent duplicates
          const uniqueKey = `${messageId}-${message.timestamp?.getTime() || index}-${index}`;
          return (
            <ChatMessageComponent
              key={uniqueKey}
              message={message}
              messageId={messageId}
              isDetailed={detailedViewMessageId === messageId}
              isApplying={applyingMessageId === messageId}
              onToggleDetail={setDetailedViewMessageId}
              onApplyChanges={handleApplyChanges}
              onSelectCandidate={setInput}
            />
          );
        })}
        <div ref={endRef} />
      </div>

      {/* Input */}
      <div className="p-4 border-t bg-white shadow-lg">
        <div className="flex gap-2">
          <Textarea
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onKeyDown={handleKeyDown}
            placeholder="Ask me about your trip..."
            disabled={isSending}
            className="flex-1 min-h-[60px] max-h-[200px] resize-none"
            maxLength={1000}
          />
          <Button
            onClick={handleSend}
            disabled={!input.trim() || isSending}
            className="self-end"
          >
            {isSending ? (
              <>
                <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                Sending
              </>
            ) : (
              'Send'
            )}
          </Button>
        </div>
        <div className="mt-2 text-xs text-gray-500 flex justify-between">
          <span>{input.length}/1000</span>
          <span>Press Enter to send, Shift+Enter for new line</span>
        </div>
      </div>
    </div>
  );
}
