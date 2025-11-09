/**
 * Chat Tab Component
 * AI-powered chat interface for itinerary modifications
 * Week 11: Task 24 - Chat Interface
 */

import React, { useRef, useEffect, useState } from 'react';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Loader2, MessageSquare, Download, ChevronDown } from 'lucide-react';
import { useUnifiedItinerary } from '@/contexts/UnifiedItineraryContext';
import { ChatMessageComponent } from '@/components/chat/ChatMessage';
import { useScrollDetection } from '@/hooks/useScrollDetection';
import { Textarea } from '@/components/ui/textarea';
import { useTranslation } from '@/i18n';

const INITIAL_DISPLAY_COUNT = 10;
const LOAD_MORE_COUNT = 10;

export function ChatTab() {
  const { t } = useTranslation();
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
    <div className="space-y-3 sm:space-y-4 pb-32 relative">
      {/* Header - Smaller on mobile */}
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-xl sm:text-2xl md:text-3xl font-bold text-foreground">{t('components.chatTab.title')}</h2>
          <p className="text-xs sm:text-sm text-muted-foreground mt-0.5 sm:mt-1">
            {t('components.chatTab.subtitle')}
          </p>
        </div>
      </div>

      {/* Floating Action Buttons - Positioned to avoid hamburger menu */}
      <div className="fixed top-3 sm:top-20 right-3 sm:right-6 z-30 flex items-center gap-1.5 sm:gap-2">
        {isConnected && (
          <Badge variant="outline" className="text-green-600 border-green-600 bg-white shadow-md text-xs px-2 py-0.5">
            {t('components.chatTab.status.live')}
          </Badge>
        )}
        {chatMessages.length > 0 && (
          <Button 
            size="sm" 
            variant="outline"
            onClick={handleExportHistory}
            title="Export chat history"
            className="bg-white shadow-md hover:shadow-lg h-7 w-7 sm:h-8 sm:w-8 p-0"
          >
            <Download className="h-3 w-3 sm:h-4 sm:w-4" />
          </Button>
        )}
      </div>

      {/* Messages Container - Smaller padding on mobile */}
      <div className="bg-white rounded-lg sm:rounded-xl shadow-sm border border-gray-200 min-h-[calc(100vh-18rem)] sm:min-h-[calc(100vh-20rem)]">
        <div ref={messagesContainerRef} className="overflow-y-auto p-3 sm:p-4 md:p-6 max-h-[calc(100vh-18rem)] sm:max-h-[calc(100vh-20rem)]">
          {/* Load More Button - Smaller on mobile */}
          {hasMoreMessages && isNearTop && (
            <div className="sticky top-0 z-10 flex justify-center mb-3 sm:mb-4">
              <Button
                size="sm"
                variant="outline"
                onClick={handleLoadMore}
                className="bg-white shadow-md hover:shadow-lg transition-shadow text-xs sm:text-sm h-8 sm:h-9"
              >
                <ChevronDown className="h-3 w-3 sm:h-4 sm:w-4 mr-1 sm:mr-2 rotate-180" />
                {t('components.chatTab.loadMore', { count: Math.min(LOAD_MORE_COUNT, chatMessages.length - displayCount) })}
              </Button>
            </div>
          )}

          {chatMessages.length === 0 && (
            <div className="text-center text-gray-600 py-8 sm:py-12">
              <MessageSquare className="h-12 w-12 sm:h-16 sm:w-16 mx-auto mb-3 sm:mb-4 text-gray-300" />
              <p className="text-base sm:text-lg font-medium mb-1 sm:mb-2">{t('components.chatTab.empty.title')}</p>
              <p className="text-xs sm:text-sm text-gray-500 mb-4 sm:mb-6">{t('components.chatTab.empty.subtitle')}</p>
              <div className="max-w-2xl mx-auto">
                <p className="text-xs text-gray-400 mb-2 sm:mb-3">{t('components.chatTab.empty.examples')}</p>
                <div className="flex flex-wrap gap-1.5 sm:gap-2 justify-center">
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
                      className="px-2.5 py-1.5 sm:px-4 sm:py-2 rounded-full bg-gray-50 border border-gray-200 hover:bg-primary/5 hover:border-primary text-xs sm:text-sm transition-all shadow-sm hover:shadow touch-manipulation active:scale-95"
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
            <div className="text-center text-xs text-gray-500 mb-3 sm:mb-4 py-1.5 sm:py-2">
              {t('components.chatTab.showing', { current: displayedMessages.length, total: chatMessages.length })}
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
      </div>

      {/* Floating Input - Smaller on mobile */}
      <div className="fixed bottom-20 md:bottom-6 left-0 right-0 px-2 sm:px-3 md:px-6 z-20">
        <div className="max-w-4xl mx-auto">
          <div className="bg-white rounded-lg sm:rounded-xl shadow-lg border border-gray-200 p-2 sm:p-3 md:p-4">
            <div className="flex gap-1.5 sm:gap-2">
              <Textarea
                value={input}
                onChange={(e) => setInput(e.target.value)}
                onKeyDown={handleKeyDown}
                placeholder={t('components.chatTab.input.placeholder')}
                disabled={isSending}
                className="flex-1 min-h-[48px] sm:min-h-[60px] max-h-[150px] sm:max-h-[200px] resize-none border-gray-200 focus:border-primary text-sm"
                maxLength={1000}
              />
              <Button
                onClick={handleSend}
                disabled={!input.trim() || isSending}
                className="self-end min-h-[44px] sm:min-h-[48px] px-3 sm:px-6 text-sm touch-manipulation active:scale-95"
              >
                {isSending ? (
                  <>
                    <Loader2 className="h-3 w-3 sm:h-4 sm:w-4 sm:mr-2 animate-spin" />
                    <span className="hidden sm:inline">{t('components.chatTab.input.sending')}</span>
                  </>
                ) : (
                  t('components.chatTab.input.send')
                )}
              </Button>
            </div>
            <div className="mt-1.5 sm:mt-2 text-xs text-gray-500 flex justify-between">
              <span>{input.length}/1000</span>
              <span className="hidden sm:inline">{t('components.chatTab.input.hint')}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
