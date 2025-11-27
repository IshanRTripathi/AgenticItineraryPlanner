/**
 * Chat Tab Component
 * AI-powered chat interface for itinerary modifications
 * Week 11: Task 24 - Chat Interface
 */

import React, { useRef, useEffect, useState } from 'react';
import { Button } from '@/components/ui/button';
import { MessageSquare, ChevronDown, Settings } from 'lucide-react';
import { useUnifiedItinerary } from '@/contexts/UnifiedItineraryContext';
import { ChatMessageComponent } from '@/components/chat/ChatMessage';
import { useScrollDetection } from '@/hooks/useScrollDetection';
import { useSpeechRecognition } from '@/hooks/useSpeechRecognition';
import { Textarea } from '@/components/ui/textarea';
import { useTranslation } from '@/i18n';
import { analytics } from '@/services/analytics';
import { PreferencesPanel } from '@/components/memory/PreferencesPanel';

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
  const [preferencesPanelOpen, setPreferencesPanelOpen] = useState(false);
  const [hideBottomNav, setHideBottomNav] = useState(false);

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
      // After stopping, the transcribed text remains in input
      // and send button will be shown
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

  // Hide bottom navbar on scroll down in chat
  useEffect(() => {
    const container = messagesContainerRef.current;
    if (!container) return;

    let lastScrollTop = 0;
    let ticking = false;

    const handleScroll = () => {
      if (!ticking) {
        window.requestAnimationFrame(() => {
          const scrollTop = container.scrollTop;

          // At very top - always show
          if (scrollTop < 10) {
            setHideBottomNav(false);
            document.body.classList.remove('hide-bottom-nav');
          }
          // Scrolling down - hide
          else if (scrollTop > lastScrollTop && scrollTop > 80) {
            setHideBottomNav(true);
            document.body.classList.add('hide-bottom-nav');
          }
          // Scrolling up - show
          else if (scrollTop < lastScrollTop) {
            setHideBottomNav(false);
            document.body.classList.remove('hide-bottom-nav');
          }

          lastScrollTop = scrollTop;
          ticking = false;
        });

        ticking = true;
      }
    };

    container.addEventListener('scroll', handleScroll, { passive: true });
    
    // Cleanup: remove class when component unmounts
    return () => {
      container.removeEventListener('scroll', handleScroll);
      document.body.classList.remove('hide-bottom-nav');
    };
  }, []);

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

    // Clear input immediately
    setInput('');
    
    // Reset textarea height
    const textarea = document.querySelector('textarea');
    if (textarea) {
      textarea.style.height = 'auto';
    }

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
    <div className="fixed inset-0 md:relative md:h-[calc(100vh-12rem)] flex flex-col bg-white">
      {/* Floating Action Button - Top right - Memory Preferences */}
      <div className="absolute top-2 right-2 z-30">
        <Button
          size="sm"
          variant="outline"
          onClick={() => setPreferencesPanelOpen(true)}
          title="Your Travel Preferences"
          className="bg-white/90 backdrop-blur-sm shadow-sm hover:shadow-md h-9 px-3 gap-2"
        >
          <Settings className="h-4 w-4" />
          <span className="hidden sm:inline text-xs">Preferences</span>
        </Button>
      </div>

      {/* Messages Container - Full viewport on mobile, constrained on desktop */}
      <div className="flex-1 overflow-hidden">
        <div ref={messagesContainerRef} className="h-full overflow-y-auto px-3 sm:px-4 pt-12 pb-32 sm:pt-4 sm:pb-6">
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

      {/* Fixed Chat Input Bar - Overlays bottom nav on mobile */}
      <div className="fixed md:relative bottom-20 md:bottom-0 left-0 right-0 z-[60] flex-shrink-0 pb-[env(safe-area-inset-bottom)] pt-2 bg-gradient-to-t from-white via-white to-transparent transition-transform duration-300"
        style={{
          transform: hideBottomNav ? 'translateY(80px)' : 'translateY(0)'
        }}
      >
        <div className="w-full px-2 sm:px-4 md:px-6 pb-2">
          <div className="max-w-4xl mx-auto">
            {/* Single unified background container - fully rounded pill */}
            <div
              className="relative flex items-center gap-2 sm:gap-3 px-3 sm:px-4 py-1.5 sm:py-2 transition-all"
              style={{
                backgroundColor: '#FFFFFF',
                borderRadius: '24px',
                border: '1px solid #E5E5E5',
                boxShadow: '0 2px 8px rgba(0,0,0,0.08)',
                width: '100%',
                minHeight: '48px'
              }}
            >
              {/* Center: Text input - no border, seamless, auto-resize */}
              <div className="flex-1 min-w-0">
                <Textarea
                  value={input}
                  onChange={(e) => setInput(e.target.value)}
                  onKeyDown={handleKeyDown}
                  placeholder="Ask anything..."
                  disabled={isSending}
                  className="w-full resize-none bg-transparent px-0 py-1 text-[15px] sm:text-[16px] leading-[1.4] placeholder:text-[#9ca3af] focus-visible:outline-none"
                  style={{
                    color: '#0f1724',
                    minHeight: '24px',
                    maxHeight: '120px',
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
                    target.style.height = Math.min(target.scrollHeight, 120) + 'px';
                  }}
                  onFocus={(e) => {
                    e.target.style.outline = 'none';
                    e.target.style.border = 'none';
                    e.target.style.boxShadow = 'none';
                  }}
                />
              </div>

              {/* Right: Single action button - clean logic */}
              <div className="flex items-center flex-shrink-0">
                {input.trim().length === 0 ? (
                  /* Mic button when empty */
                  isSpeechSupported && (
                    <button
                      type="button"
                      onClick={toggleListening}
                      className="flex items-center justify-center transition-all active:scale-95 text-[#414141ff] hover:opacity-60"
                      style={{
                        background: 'transparent',
                        border: 'none',
                        cursor: 'pointer',
                        padding: 0,
                        width: '40px',
                        height: '40px'
                      }}
                      title="Voice input"
                      aria-label="Start voice input"
                    >
                      <svg
                        width="22"
                        height="22"
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
                    </button>
                  )
                ) : isListening ? (
                  /* Stop recording button when recording */
                  <button
                    type="button"
                    onClick={toggleListening}
                    className="flex items-center justify-center transition-all active:scale-95 text-red-500 hover:text-red-600"
                    style={{
                      background: 'transparent',
                      border: 'none',
                      cursor: 'pointer',
                      padding: 0,
                      width: '40px',
                      height: '40px'
                    }}
                    title="Stop recording"
                    aria-label="Stop recording"
                  >
                    <div className="relative flex items-center justify-center w-full h-full">
                      <span className="animate-ping absolute inline-flex h-7 w-7 rounded-full bg-red-400 opacity-40"></span>
                      <span className="relative inline-flex rounded-full h-3 w-3 bg-red-500"></span>
                    </div>
                  </button>
                ) : (
                  /* Send button when has text and not recording */
                  <button
                    onClick={handleSend}
                    disabled={!input.trim()}
                    type="button"
                    className="flex items-center justify-center transition-all active:scale-95 disabled:opacity-40 disabled:cursor-not-allowed text-[#414141ff] hover:opacity-60"
                    style={{
                      background: 'transparent',
                      border: 'none',
                      cursor: input.trim() ? 'pointer' : 'not-allowed',
                      padding: 0,
                      width: '40px',
                      height: '40px'
                    }}
                    title="Send message"
                    aria-label="Send message"
                  >
                    <svg
                      xmlns="http://www.w3.org/2000/svg"
                      viewBox="0 0 20 20"
                      fill="currentColor"
                      width="22"
                      height="22"
                    >
                      <path fillRule="evenodd" d="M10 17a.75.75 0 0 1-.75-.75V5.612L5.29 9.77a.75.75 0 0 1-1.08-1.04l5.25-5.5a.75.75 0 0 1 1.08 0l5.25 5.5a.75.75 0 1 1-1.08 1.04l-3.96-4.158V16.25A.75.75 0 0 1 10 17Z" clipRule="evenodd" />
                    </svg>
                  </button>
                )}
              </div>
            </div>
          </div>
        </div>
      </div>
      
      {/* Preferences Panel */}
      <PreferencesPanel
        itineraryId={itinerary?.itineraryId || ''}
        isOpen={preferencesPanelOpen}
        onClose={() => setPreferencesPanelOpen(false)}
      />
      
      {/* Mobile optimizations for chat tab */}
      <style>{`
        @media (max-width: 768px) {
          /* Hide mobile bottom navigation when scrolling in chat */
          body.hide-bottom-nav .menu {
            transform: translateX(-50%) translateY(120px) !important;
            opacity: 0 !important;
            pointer-events: none !important;
          }
        }
        
        /* Smooth transitions */
        textarea {
          transition: height 0.1s ease-out;
        }
        
        .menu {
          transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1), opacity 0.3s cubic-bezier(0.4, 0, 0.2, 1) !important;
        }
      `}</style>
    </div>
  );
}
