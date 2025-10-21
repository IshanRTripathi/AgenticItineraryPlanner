import React, { useEffect, useRef, useReducer } from 'react';
import { Button } from '../ui/button';
import { Badge } from '../ui/badge';
import { Loader2, MessageSquare, Download, Trash2, AlertCircle, ChevronDown } from 'lucide-react';
import { chatApi, ChatRequest, ChatMessageDTO } from '../../services/chatApi';
import { useUnifiedItinerary } from '../../contexts/UnifiedItineraryContext';
import { userChangeTracker } from '../../services/userChangeTracker';
import { PromptBox } from '../ui/chatgpt-prompt-input';
import { useScrollDetection } from '../../hooks/useScrollDetection';
import { ChatMessage } from './ChatMessage';

type ChatState = {
  itineraryId: string;
  allMessages: ChatMessageDTO[]; // All messages loaded so far
  displayedMessages: ChatMessageDTO[]; // Messages currently displayed
  status: 'idle' | 'loading_history' | 'sending' | 'error' | 'applying' | 'loading_more';
  error?: string;
  input: string;
  detailedViewMessageId?: string;
  applyingMessageId?: string;
  hasMoreMessages: boolean;
  displayCount: number; // Number of messages to display
};

type Action =
  | { type: 'INIT'; itineraryId: string }
  | { type: 'LOAD_START' }
  | { type: 'LOAD_SUCCESS'; messages: ChatMessageDTO[] }
  | { type: 'LOAD_ERROR'; error: string }
  | { type: 'INPUT_SET'; value: string }
  | { type: 'SEND_START' }
  | { type: 'SEND_SUCCESS'; message: ChatMessageDTO; assistant: ChatMessageDTO }
  | { type: 'SEND_ERROR'; error: string }
  | { type: 'TOGGLE_DETAIL'; messageId: string }
  | { type: 'APPLY_START'; messageId: string }
  | { type: 'APPLY_SUCCESS'; messageId: string }
  | { type: 'APPLY_ERROR'; error: string }
  | { type: 'CLEAR_HISTORY' }
  | { type: 'APPEND_WS_MESSAGE'; message: ChatMessageDTO }
  | { type: 'LOAD_MORE_START' }
  | { type: 'LOAD_MORE_SUCCESS' };

const INITIAL_DISPLAY_COUNT = 5;
const LOAD_MORE_COUNT = 5;

function reducer(state: ChatState, action: Action): ChatState {
  switch (action.type) {
    case 'INIT': return { ...state, itineraryId: action.itineraryId };
    case 'LOAD_START': return { ...state, status: 'loading_history', error: undefined };
    case 'LOAD_SUCCESS': {
      const allMessages = action.messages;
      const displayCount = Math.min(INITIAL_DISPLAY_COUNT, allMessages.length);
      const displayedMessages = allMessages.slice(-displayCount); // Show latest messages
      return { 
        ...state, 
        status: 'idle', 
        allMessages,
        displayedMessages,
        displayCount,
        hasMoreMessages: allMessages.length > displayCount
      };
    }
    case 'LOAD_ERROR': return { ...state, status: 'error', error: action.error };
    case 'INPUT_SET': return { ...state, input: action.value };
    case 'SEND_START': return { ...state, status: 'sending', error: undefined };
    case 'SEND_SUCCESS': {
      const newAllMessages = [...state.allMessages, action.message, action.assistant];
      const newDisplayCount = state.displayCount + 2;
      const newDisplayedMessages = newAllMessages.slice(-newDisplayCount);
      return { 
        ...state, 
        status: 'idle', 
        input: '', 
        allMessages: newAllMessages,
        displayedMessages: newDisplayedMessages,
        displayCount: newDisplayCount
      };
    }
    case 'SEND_ERROR': return { ...state, status: 'error', error: action.error };
    case 'TOGGLE_DETAIL':
      return { 
        ...state, 
        detailedViewMessageId: state.detailedViewMessageId === action.messageId ? undefined : action.messageId 
      };
    case 'APPLY_START': return { ...state, status: 'applying', applyingMessageId: action.messageId, error: undefined };
    case 'APPLY_SUCCESS':
      return {
        ...state,
        status: 'idle',
        applyingMessageId: undefined,
        allMessages: state.allMessages.map((m, idx) => {
          const msgId = m.id || (m.timestamp ? `${m.timestamp}` : `msg-${idx}`);
          return msgId === action.messageId ? { ...m, applied: true } : m;
        }),
        displayedMessages: state.displayedMessages.map((m, idx) => {
          const msgId = m.id || (m.timestamp ? `${m.timestamp}` : `msg-${idx}`);
          return msgId === action.messageId ? { ...m, applied: true } : m;
        }),
      };
    case 'APPLY_ERROR': return { ...state, status: 'error', error: action.error, applyingMessageId: undefined };
    case 'CLEAR_HISTORY': return { 
      ...state, 
      allMessages: [], 
      displayedMessages: [], 
      displayCount: 0, 
      hasMoreMessages: false 
    };
    case 'APPEND_WS_MESSAGE': {
      const newAllMessages = [...state.allMessages, action.message];
      const newDisplayCount = state.displayCount + 1;
      const newDisplayedMessages = newAllMessages.slice(-newDisplayCount);
      return { 
        ...state, 
        allMessages: newAllMessages,
        displayedMessages: newDisplayedMessages,
        displayCount: newDisplayCount
      };
    }
    case 'LOAD_MORE_START': return { ...state, status: 'loading_more' };
    case 'LOAD_MORE_SUCCESS': {
      const newDisplayCount = Math.min(
        state.displayCount + LOAD_MORE_COUNT,
        state.allMessages.length
      );
      const newDisplayedMessages = state.allMessages.slice(-newDisplayCount);
      return {
        ...state,
        status: 'idle',
        displayedMessages: newDisplayedMessages,
        displayCount: newDisplayCount,
        hasMoreMessages: newDisplayCount < state.allMessages.length
      };
    }
    default:
      return state;
  }
}

interface NewChatProps {
  itineraryId?: string;
  onItineraryUpdate?: (updatedItinerary: any) => void;
}

export const NewChat: React.FC<NewChatProps> = ({ 
  itineraryId: propItineraryId, 
  onItineraryUpdate: propOnItineraryUpdate 
}) => {
  // Try to use context, but fallback to props if context is not available
  let contextItineraryId: string | undefined;
  let loadItinerary: any;
  let contextState: any = null;
  
  try {
    const { state: uix, loadItinerary: contextLoadItinerary } = useUnifiedItinerary() as any;
    contextItineraryId = uix.itinerary?.id;
    loadItinerary = contextLoadItinerary;
    contextState = uix;
  } catch (error) {
    // Context not available, use props instead
    contextItineraryId = undefined;
    loadItinerary = null;
    contextState = null;
  }
  
  const itineraryId = propItineraryId || contextItineraryId;
  const [state, dispatch] = useReducer(reducer, {
    itineraryId: itineraryId ?? '',
    allMessages: [],
    displayedMessages: [],
    status: 'idle',
    input: '',
    hasMoreMessages: false,
    displayCount: 0,
  });

  const messagesContainerRef = useRef<HTMLDivElement>(null);
  const endRef = useRef<HTMLDivElement>(null);
  const previousScrollHeightRef = useRef<number>(0);
  
  const scrollToBottom = () => endRef.current?.scrollIntoView({ behavior: 'smooth' });
  
  // Detect scroll position - only enable after messages are loaded
  const { isNearTop, isAtBottom } = useScrollDetection(messagesContainerRef, {
    threshold: 50, // Only show button when within 50px of top
    enabled: state.displayedMessages.length > 0 && !!messagesContainerRef.current
  });

  useEffect(() => {
    if (!itineraryId) return;
    dispatch({ type: 'INIT', itineraryId });
    dispatch({ type: 'LOAD_START' });
    chatApi.history(itineraryId)
      .then(messages => {
        dispatch({ type: 'LOAD_SUCCESS', messages });
      })
      .catch(e => {
        console.error('[NewChat] Failed to load chat history:', e);
        dispatch({ type: 'LOAD_ERROR', error: e.message });
      });
  }, [itineraryId]);

  // Auto-scroll to bottom only for new messages (not when loading more)
  useEffect(() => { 
    if (state.status !== 'loading_more') {
      scrollToBottom(); 
    }
  }, [state.displayedMessages.length, state.status]);
  
  // Load more messages handler
  const handleLoadMore = () => {
    if (state.status === 'loading_more' || !state.hasMoreMessages) return;
    
    // Store current scroll height before loading more
    if (messagesContainerRef.current) {
      previousScrollHeightRef.current = messagesContainerRef.current.scrollHeight;
    }
    
    dispatch({ type: 'LOAD_MORE_START' });
    
    // Simulate async operation (in case we need to fetch from server in future)
    setTimeout(() => {
      dispatch({ type: 'LOAD_MORE_SUCCESS' });
      
      // Restore scroll position after new messages are rendered
      requestAnimationFrame(() => {
        if (messagesContainerRef.current) {
          const newScrollHeight = messagesContainerRef.current.scrollHeight;
          const scrollDiff = newScrollHeight - previousScrollHeightRef.current;
          messagesContainerRef.current.scrollTop += scrollDiff;
        }
      });
    }, 100);
  };

  const onSend = async () => {
    const text = state.input.trim();
    if (!text || !itineraryId) return;
    dispatch({ type: 'SEND_START' });
    const userMsg: ChatMessageDTO = { message: text, sender: 'user', timestamp: Date.now() };
    try {
      await chatApi.persist(itineraryId, userMsg);
      const req: ChatRequest = {
        itineraryId,
        scope: contextState?.selectedDay ? 'day' : 'trip',
        day: contextState?.selectedDay || undefined,
        selectedNodeId: contextState?.selectedNodeId || undefined,
        text,
        autoApply: false,
      };
      const res = await chatApi.send(itineraryId, req);
      await chatApi.persist(itineraryId, { ...res, sender: 'assistant', message: res.message, timestamp: Date.now() });
      dispatch({ type: 'SEND_SUCCESS', message: { ...userMsg }, assistant: { ...res, sender: 'assistant' } });
    } catch (e: any) {
      console.error('[NewChat] Send message failed:', e);
      dispatch({ type: 'SEND_ERROR', error: e.message || 'Failed to send message' });
    }
  };

  const onApplyChanges = async (messageId: string, changeSet: any) => {
    if (!itineraryId) return;
    dispatch({ type: 'APPLY_START', messageId });
    try {
      await chatApi.applyChangeSet(itineraryId, { changeSet });
      dispatch({ type: 'APPLY_SUCCESS', messageId });
      
      // Track user changes for workflow highlighting
      const message = state.allMessages.find(m => {
        const msgId = m.id || (m.timestamp ? `${m.timestamp}` : '');
        return msgId === messageId;
      });
      
      if (message) {
        userChangeTracker.processChatChanges(message);
      }
      
      // Reload itinerary to reflect changes
      if (loadItinerary) {
        await loadItinerary(itineraryId);
      } else if (propOnItineraryUpdate) {
        // If no context loadItinerary, use the prop callback
        // Note: This is a simplified approach - in a real app you might want to fetch the updated itinerary
        propOnItineraryUpdate({ id: itineraryId });
      }
    } catch (e: any) {
      dispatch({ type: 'APPLY_ERROR', error: e.message || 'Failed to apply changes' });
    }
  };

  const onClearHistory = async () => {
    if (!itineraryId) return;
    const confirmed = window.confirm('Clear all chat history? This cannot be undone.');
    if (!confirmed) return;
    try {
      await chatApi.clear(itineraryId);
      dispatch({ type: 'CLEAR_HISTORY' });
    } catch (e: any) {
      dispatch({ type: 'SEND_ERROR', error: e.message || 'Failed to clear history' });
    }
  };

  const onExportHistory = () => {
    const dataStr = JSON.stringify(state.allMessages, null, 2);
    const dataUri = 'data:application/json;charset=utf-8,' + encodeURIComponent(dataStr);
    const exportFileDefaultName = `chat-history-${itineraryId}-${Date.now()}.json`;
    const linkElement = document.createElement('a');
    linkElement.setAttribute('href', dataUri);
    linkElement.setAttribute('download', exportFileDefaultName);
    linkElement.click();
  };

  const renderMessage = (m: ChatMessageDTO, index: number) => {
    // Generate unique key: use id if available, fallback to timestamp, then index
    const messageId = m.id || (m.timestamp ? `${m.timestamp}` : `msg-${index}`);
    const isDetailed = state.detailedViewMessageId === messageId;
    const isApplying = state.applyingMessageId === messageId;
    
    return (
      <ChatMessage
        key={messageId}
        message={m}
        messageId={messageId}
        isDetailed={isDetailed}
        isApplying={isApplying}
        onToggleDetail={(id) => dispatch({ type: 'TOGGLE_DETAIL', messageId: id })}
        onApplyChanges={onApplyChanges}
        onSelectCandidate={(text) => dispatch({ type: 'INPUT_SET', value: text })}
      />
    );
  };

  return (
    <div className="h-full flex flex-col new-chat bg-gray-50">
      {/* Header */}
      <div className="p-4 border-b bg-white flex items-center justify-between shadow-sm">
        <div className="flex items-center gap-3">
          <MessageSquare className="h-5 w-5 text-blue-600" />
          <span className="text-lg font-semibold">AI Travel Assistant</span>
          <div style={{ width: 56, height: 22 }} className="inline-flex items-center">
            {contextState?.isConnected ? (
              <Badge variant="outline" className="text-green-600 border-green-600">
                Live
              </Badge>
            ) : (
              <span style={{ visibility: 'hidden' }}>Live</span>
            )}
          </div>
          <div style={{ width: 92, height: 22 }} className="inline-flex items-center">
            {state.status === 'sending' || state.status === 'applying' ? (
              <Badge variant="outline" className="text-blue-600 border-blue-600">
                {state.status === 'applying' ? 'Applying' : 'Processing'}
              </Badge>
            ) : (
              <span style={{ visibility: 'hidden' }}>Processing</span>
            )}
          </div>
        </div>
        
        <div className="flex items-center gap-2">
          {state.allMessages.length > 0 && (
            <>
              <Button 
                size="sm" 
                variant="outline"
                onClick={onExportHistory}
                title="Export chat history"
              >
                <Download className="h-4 w-4" />
              </Button>
              <Button 
                size="sm" 
                variant="outline"
                onClick={onClearHistory}
                title="Clear chat history"
              >
                <Trash2 className="h-4 w-4" />
              </Button>
            </>
          )}
        </div>
      </div>

      {/* Error Display */}
      {state.error && (
        <div className="mx-4 mt-4 p-3 bg-red-50 border border-red-200 rounded-lg flex items-start gap-2">
          <AlertCircle className="h-4 w-4 text-red-600 mt-0.5 flex-shrink-0" />
          <div className="flex-1">
            <p className="text-sm text-red-900 font-medium">Error</p>
            <p className="text-sm text-red-800">{state.error}</p>
          </div>
        </div>
      )}

      {/* Messages */}
      <div ref={messagesContainerRef} className="flex-1 overflow-y-auto p-4 relative">
        {/* Load More Button - Sticky at top - only show when near top */}
        {state.hasMoreMessages && isNearTop && (
          <div className="sticky top-0 z-10 flex justify-center mb-4">
            <Button
              size="sm"
              variant="outline"
              onClick={handleLoadMore}
              disabled={state.status === 'loading_more'}
              className="bg-white shadow-md hover:shadow-lg transition-shadow"
            >
              {state.status === 'loading_more' ? (
                <>
                  <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                  Loading...
                </>
              ) : (
                <>
                  <ChevronDown className="h-4 w-4 mr-2 rotate-180" />
                  Load {Math.min(LOAD_MORE_COUNT, state.allMessages.length - state.displayCount)} More Messages
                </>
              )}
            </Button>
          </div>
        )}
        
        {state.status === 'loading_history' && (
          <div className="flex items-center justify-center py-8">
            <Loader2 className="h-6 w-6 animate-spin text-blue-600" />
            <span className="ml-2 text-sm text-gray-600">Loading chat history...</span>
          </div>
        )}

        {state.displayedMessages.length === 0 && state.status !== 'loading_history' && (
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
                    onClick={() => dispatch({ type: 'INPUT_SET', value: s })}
                  >
                    "{s}"
                  </button>
                ))}
              </div>
            </div>
          </div>
        )}

        {/* Show message count indicator if there are hidden messages */}
        {state.hasMoreMessages && state.displayedMessages.length > 0 && (
          <div className="text-center text-xs text-gray-500 mb-4 py-2">
            Showing {state.displayedMessages.length} of {state.allMessages.length} messages
          </div>
        )}
        
        {state.displayedMessages.map(renderMessage)}
        <div ref={endRef} />
      </div>

      {/* Input */}
      <div className="p-4 border-t bg-white shadow-lg">
        {!itineraryId && (
          <div className="mb-3 p-2 bg-amber-50 border border-amber-200 rounded text-sm text-amber-900">
            Loading itinerary... Please wait before sending a message.
          </div>
        )}
        <PromptBox
          value={state.input}
          onChange={(value) => dispatch({ type: 'INPUT_SET', value })}
          onSubmit={onSend}
          disabled={!itineraryId || state.status === 'sending' || state.status === 'applying'}
          placeholder={itineraryId ? "Ask me about your trip…" : "Loading..."}
          maxLength={1000}
          showCharacterCount={true}
          showEncodingWarning={true}
        />
      </div>
    </div>
  );
};


