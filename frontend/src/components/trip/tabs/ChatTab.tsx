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
import { useSpeechRecognition } from '@/hooks/useSpeechRecognition';
import { Textarea } from '@/components/ui/textarea';
import { useTranslation } from '@/i18n';
import { analytics } from '@/services/analytics';

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

  // Voice Input
  const {
    isListening,
    transcript,
    interimTranscript,
    startListening,
    stopListening,
    isSupported: isSpeechSupported
  } = useSpeechRecognition();

  const startInputRef = useRef('');

  // Sync voice input with text area
  useEffect(() => {
    if (isListening) {
      const currentTranscript = transcript + (interimTranscript ? (transcript ? ' ' : '') + interimTranscript : '');
      if (currentTranscript) {
        const prefix = startInputRef.current ? startInputRef.current + ' ' : '';
        setInput(prefix + currentTranscript);
      }
    }
  }, [transcript, interimTranscript, isListening]);

  const toggleListening = () => {
    if (isListening) {
      stopListening();
    } else {
      startInputRef.current = input;
      startListening();
    }
  };

  // Check if we're waiting for a response (last message is from user)
  const isWaitingForResponse = chatMessages.length > 0 && chatMessages[chatMessages.length - 1].sender === 'user';

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
    if (!text || isWaitingForResponse) return;

    setInput('');

    const startTime = Date.now();
    const messageLength = text.length;
    const wordCount = text.split(/\s+/).length;

    // Track chat message sent
    analytics.track('chat_message_sent', {
      messageLength,
      wordCount,
      itineraryId: itinerary?.itineraryId,
      timestamp: startTime
    });

    try {
      await sendChatMessage(text);

      const duration = Date.now() - startTime;

      // Track successful response
      analytics.track('chat_response_received', {
        messageLength,
        wordCount,
        duration,
        itineraryId: itinerary?.itineraryId
      });
    } catch (error) {
      const duration = Date.now() - startTime;

      // Classify error for better tracking
      const errorMessage = error instanceof Error ? error.message : 'Unknown error';
      const statusCode = (error as any)?.response?.status || (error as any)?.status;
      const errorType =
        statusCode === 429 ? 'rate_limit' :
          statusCode >= 500 ? 'server_error' :
            statusCode >= 400 ? 'client_error' :
              error instanceof Error && error.message.includes('timeout') ? 'timeout' :
                error instanceof Error && error.message.includes('network') ? 'network' :
                  'unknown';
      const retryable = errorType === 'rate_limit' || errorType === 'server_error' || errorType === 'timeout' || errorType === 'network';

      // Track failed response with classification
      analytics.track('chat_response_failed', {
        messageLength,
        wordCount,
        duration,
        error: errorMessage,
        errorType,
        statusCode: statusCode || null,
        retryable,
        itineraryId: itinerary?.itineraryId
      });

      console.error('Failed to send message:', error);
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
    const startTime = Date.now();

    // Track apply changes initiated
    analytics.track('chat_changes_apply_initiated', {
      messageId,
      changeCount: changeSet?.ops?.length || 0,
      itineraryId: itinerary?.itineraryId
    });

    try {
      // Apply changes through context
      // The UnifiedItineraryContext will handle the API call
      console.log('Applying changes:', changeSet);
      // TODO: Implement apply changes in context

      const duration = Date.now() - startTime;

      // Track successful application
      analytics.track('chat_changes_applied', {
        messageId,
        changeCount: changeSet?.ops?.length || 0,
        duration,
        itineraryId: itinerary?.itineraryId
      });
    } catch (error) {
      const duration = Date.now() - startTime;

      // Track failed application
      analytics.track('chat_changes_apply_failed', {
        messageId,
        changeCount: changeSet?.ops?.length || 0,
        duration,
        error: error instanceof Error ? error.message : 'Unknown error',
        itineraryId: itinerary?.itineraryId
      });

      console.error('Failed to apply changes:', error);
    } finally {
      setApplyingMessageId(undefined);
    }
  };

  const handleExportHistory = () => {
    const dataStr = JSON.stringify(chatMessages, null, 2);
    const dataUri = 'data:application/json;charset=utf-8,' + encodeURIComponent(dataStr);
    const exportFileDefaultName = `chat-history-${itinerary?.itineraryId}-${Date.now()}.json`;
    const linkElement = document.createElement('a');
    linkElement.setAttribute('href', dataUri);
    linkElement.setAttribute('download', exportFileDefaultName);
    linkElement.click();
  };

  return (
    <div className="relative h-[calc(100vh-12rem)] flex flex-col">
      {/* Floating Action Buttons - Top right */}
      <div className="absolute top-2 right-2 z-30 flex items-center gap-2">
        {isConnected && (
          <Badge variant="outline" className="text-green-600 border-green-600 bg-white/90 backdrop-blur-sm shadow-sm text-xs px-2 py-0.5">
            {t('components.chatTab.status.live')}
          </Badge>
        )}
        {chatMessages.length > 0 && (
          <Button
            size="sm"
            variant="outline"
            onClick={handleExportHistory}
            title="Export chat history"
            className="bg-white/90 backdrop-blur-sm shadow-sm hover:shadow-md h-9 w-9 p-0"
          >
            <Download className="h-4 w-4" />
          </Button>
        )}
      </div>

      {/* Messages Container - Full height, clean design */}
      <div className="flex-1 overflow-hidden">
        <div ref={messagesContainerRef} className="h-full overflow-y-auto px-3 sm:px-4 py-4 sm:py-6 pb-24 sm:pb-6">
          {/* Load More Button - Larger on mobile */}
          {hasMoreMessages && isNearTop && (
            <div className="sticky top-0 z-10 flex justify-center mb-4">
              <Button
                size="sm"
                variant="outline"
                onClick={handleLoadMore}
                className="bg-white shadow-md hover:shadow-lg transition-shadow text-sm h-10 px-4 rounded-full"
              >
                <ChevronDown className="h-4 w-4 mr-2 rotate-180" />
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

      {/* Simplified Light Theme Chat Input Bar - Mobile Optimized */}
      <div className="flex-shrink-0 pb-[env(safe-area-inset-bottom)] pt-2 bg-gradient-to-t from-white via-white to-transparent">
        <div className="w-full px-3 sm:px-4 md:px-6 pb-2 sm:pb-4">
          <div className="max-w-4xl mx-auto">
            {/* Single unified background container - fully rounded pill */}
            <div
              className="relative flex items-center gap-2 sm:gap-3 px-3 sm:px-5 py-2 sm:py-3 transition-all"
              style={{
                backgroundColor: '#FFFFFF',
                borderRadius: '28px',
                border: '1px solid #E5E5E5',
                boxShadow: '0 2px 8px rgba(0,0,0,0.08)',
                width: '100%',
                minHeight: '56px'
              }}
            >
              {/* Left: Plus icon - Larger touch target */}
              <button
                type="button"
                className="flex-shrink-0 flex items-center justify-center hover:opacity-60 transition-opacity active:scale-95"
                style={{
                  background: 'transparent',
                  border: 'none',
                  cursor: 'pointer',
                  color: '#414141ff',
                  padding: 0,
                  width: '44px',
                  height: '44px'
                }}
                title="Add attachment"
                aria-label="Add files"
              >
                <svg
                  width="24"
                  height="24"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  strokeWidth="1.8"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                >
                  <path d="M12 5v14m7-7H5" />
                </svg>
              </button>

              {/* Center: Text input - no border, seamless, 16px font for mobile */}
              <div className="flex-1 min-w-0">
                <Textarea
                  value={input}
                  onChange={(e) => setInput(e.target.value)}
                  onKeyDown={handleKeyDown}
                  placeholder="Ask anything..."
                  disabled={isSending}
                  className="w-full resize-none bg-transparent px-0 py-2 text-[16px] leading-[1.5] placeholder:text-[#9ca3af] focus-visible:outline-none"
                  style={{
                    color: '#0f1724',
                    minHeight: '24px',
                    caretColor: '#0f1724',
                    border: 'none',
                    outline: 'none',
                    boxShadow: 'none'
                  }}
                  rows={1}
                  maxLength={1000}
                  onInput={(e) => {
                    const target = e.target as HTMLTextAreaElement;
                    target.style.height = 'auto';
                    target.style.height = Math.min(target.scrollHeight, 150) + 'px';
                  }}
                  onFocus={(e) => {
                    e.target.style.outline = 'none';
                    e.target.style.border = 'none';
                    e.target.style.boxShadow = 'none';
                  }}
                />
              </div>

              {/* Right: Action buttons - overlay style */}
              <div className="flex items-center">
                {input.length === 0 && !isWaitingForResponse ? (
                  <>
                    {/* Mic button - Larger touch target */}
                    {isSpeechSupported && (
                      <button
                        type="button"
                        onClick={toggleListening}
                        className={`flex-shrink-0 flex items-center justify-center transition-all active:scale-95 ${isListening
                          ? 'text-red-500 hover:text-red-600'
                          : 'text-[#414141ff] hover:opacity-60'
                          }`}
                        style={{
                          background: 'transparent',
                          border: 'none',
                          cursor: 'pointer',
                          padding: 0,
                          width: '44px',
                          height: '44px'
                        }}
                        title={isListening ? "Stop recording" : "Voice input"}
                        aria-label={isListening ? "Stop recording" : "Start dictation"}
                      >
                        {isListening ? (
                          <div className="relative flex items-center justify-center w-full h-full">
                            <span className="animate-ping absolute inline-flex h-8 w-8 rounded-full bg-red-400 opacity-40"></span>
                            <span className="relative inline-flex rounded-full h-3 w-3 bg-red-500"></span>
                          </div>
                        ) : (
                          <svg
                            width="24"
                            height="24"
                            viewBox="0 0 24 24"
                            fill="none"
                            stroke="currentColor"
                            strokeWidth="1.5"
                            strokeLinecap="round"
                            strokeLinejoin="round"
                          >
                            <path d="M12 2a3 3 0 0 1 3 3v6a3 3 0 0 1-6 0V5a3 3 0 0 1 3-3z" />
                            <path d="M19 11a7 7 0 0 1-14 0" />
                            <path d="M12 18v4" />
                          </svg>
                        )}
                      </button>
                    )}
                  </>
                ) : (
                  /* Send/Stop button - Larger touch target */
                  <button
                    onClick={handleSend}
                    disabled={!input.trim() && !isWaitingForResponse}
                    type="button"
                    className="flex-shrink-0 flex items-center justify-center hover:opacity-60 transition-opacity active:scale-95"
                    style={{
                      background: 'transparent',
                      border: 'none',
                      cursor: isWaitingForResponse || input.trim() ? 'pointer' : 'not-allowed',
                      color: '#414141ff',
                      padding: 0,
                      width: '44px',
                      height: '44px',
                      opacity: !input.trim() && !isWaitingForResponse ? 0.5 : 1
                    }}
                    title={isWaitingForResponse ? 'Stop' : 'Send'}
                    aria-label={isWaitingForResponse ? 'Stop' : 'Send'}
                  >
                    {isWaitingForResponse ? (
                      <svg
                        xmlns="http://www.w3.org/2000/svg"
                        viewBox="0 0 20 20"
                        fill="currentColor"
                        width="24"
                        height="24"
                      >
                        <rect x="5" y="5" width="10" height="10" rx="2" />
                      </svg>
                    ) : (
                      <svg
                        xmlns="http://www.w3.org/2000/svg"
                        viewBox="0 0 20 20"
                        fill="currentColor"
                        width="24"
                        height="24"
                      >
                        <path fillRule="evenodd" d="M10 17a.75.75 0 0 1-.75-.75V5.612L5.29 9.77a.75.75 0 0 1-1.08-1.04l5.25-5.5a.75.75 0 0 1 1.08 0l5.25 5.5a.75.75 0 1 1-1.08 1.04l-3.96-4.158V16.25A.75.75 0 0 1 10 17Z" clipRule="evenodd" />
                      </svg>
                    )}
                  </button>
                )}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
