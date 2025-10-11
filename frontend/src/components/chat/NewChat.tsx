import React, { useEffect, useRef, useReducer, useState } from 'react';
import { Button } from '../ui/button';
import { Badge } from '../ui/badge';
import { Send, Loader2, MessageSquare, Download, Trash2, AlertCircle, CheckCircle2, Info } from 'lucide-react';
import { chatApi, ChatRequest, ChatMessageDTO } from '../../services/chatApi';
import { DiffViewer } from '../diff/DiffViewer';
import { convertItineraryDiffToSections, createChangeSetDiff } from '../../utils/diffUtils';
import { useUnifiedItinerary } from '../../contexts/UnifiedItineraryContext';
import { userChangeTracker } from '../../services/userChangeTracker';

type ChatState = {
  itineraryId: string;
  messages: ChatMessageDTO[];
  status: 'idle' | 'loading_history' | 'sending' | 'error' | 'applying';
  error?: string;
  input: string;
  detailedViewMessageId?: string;
  applyingMessageId?: string;
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
  | { type: 'APPEND_WS_MESSAGE'; message: ChatMessageDTO };

function reducer(state: ChatState, action: Action): ChatState {
  switch (action.type) {
    case 'INIT': return { ...state, itineraryId: action.itineraryId };
    case 'LOAD_START': return { ...state, status: 'loading_history', error: undefined };
    case 'LOAD_SUCCESS': return { ...state, status: 'idle', messages: action.messages };
    case 'LOAD_ERROR': return { ...state, status: 'error', error: action.error };
    case 'INPUT_SET': return { ...state, input: action.value };
    case 'SEND_START': return { ...state, status: 'sending', error: undefined };
    case 'SEND_SUCCESS':
      return { ...state, status: 'idle', input: '', messages: [...state.messages, action.message, action.assistant] };
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
        messages: state.messages.map((m, idx) => {
          const msgId = m.id || (m.timestamp ? `${m.timestamp}` : `msg-${idx}`);
          return msgId === action.messageId ? { ...m, applied: true } : m;
        }),
      };
    case 'APPLY_ERROR': return { ...state, status: 'error', error: action.error, applyingMessageId: undefined };
    case 'CLEAR_HISTORY': return { ...state, messages: [] };
    case 'APPEND_WS_MESSAGE': return { ...state, messages: [...state.messages, action.message] };
  }
}

export const NewChat: React.FC = () => {
  const { state: uix, loadItinerary } = useUnifiedItinerary() as any;
  const itineraryId = uix.itinerary?.id;
  const [state, dispatch] = useReducer(reducer, {
    itineraryId: itineraryId ?? '',
    messages: [],
    status: 'idle',
    input: '',
  });

  const endRef = useRef<HTMLDivElement>(null);
  const scrollToBottom = () => endRef.current?.scrollIntoView({ behavior: 'smooth' });

  useEffect(() => {
    if (!itineraryId) return;
    console.log('[NewChat] Loading chat history for itinerary:', itineraryId);
    dispatch({ type: 'INIT', itineraryId });
    dispatch({ type: 'LOAD_START' });
    chatApi.history(itineraryId)
      .then(messages => {
        console.log('[NewChat] Chat history loaded:', messages.length, 'messages');
        dispatch({ type: 'LOAD_SUCCESS', messages });
      })
      .catch(e => {
        console.error('[NewChat] Failed to load chat history:', e);
        dispatch({ type: 'LOAD_ERROR', error: e.message });
      });
  }, [itineraryId]);

  useEffect(() => { scrollToBottom(); }, [state.messages]);

  const onSend = async () => {
    const text = state.input.trim();
    if (!text || !itineraryId) return;
    dispatch({ type: 'SEND_START' });
    const userMsg: ChatMessageDTO = { message: text, sender: 'user', timestamp: Date.now() };
    try {
      await chatApi.persist(itineraryId, userMsg);
      const req: ChatRequest = {
        itineraryId,
        scope: uix.selectedDay ? 'day' : 'trip',
        day: uix.selectedDay || undefined,
        selectedNodeId: uix.selectedNodeId || undefined,
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
      const message = state.messages.find(m => {
        const msgId = m.id || (m.timestamp ? `${m.timestamp}` : '');
        return msgId === messageId;
      });
      
      if (message) {
        userChangeTracker.processChatChanges(message);
      }
      
      // Reload itinerary to reflect changes
      if (loadItinerary) {
        await loadItinerary(itineraryId);
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
    const dataStr = JSON.stringify(state.messages, null, 2);
    const dataUri = 'data:application/json;charset=utf-8,' + encodeURIComponent(dataStr);
    const exportFileDefaultName = `chat-history-${itineraryId}-${Date.now()}.json`;
    const linkElement = document.createElement('a');
    linkElement.setAttribute('href', dataUri);
    linkElement.setAttribute('download', exportFileDefaultName);
    linkElement.click();
  };

  const renderMessage = (m: ChatMessageDTO, index: number) => {
    const isUser = m.sender === 'user';
    // Generate unique key: use id if available, fallback to timestamp, then index
    const messageId = m.id || (m.timestamp ? `${m.timestamp}` : `msg-${index}`);
    const showPreview = !!m.changeSet && !m.applied;
    const isDetailed = state.detailedViewMessageId === messageId;
    const isApplying = state.applyingMessageId === messageId;
    
    return (
      <div key={messageId} className={`flex ${isUser ? 'justify-end' : 'justify-start'} mb-3`}>
        <div className={`max-w-[85%] rounded-xl px-4 py-3 shadow-sm transition-all ${
          isUser 
            ? 'bg-blue-600 text-white' 
            : 'bg-white text-gray-900 border border-gray-200'
        }`}>
          <div className="flex items-center justify-between gap-2 mb-1">
            <span className="text-xs font-medium opacity-80">
              {isUser ? '👤 You' : '🤖 Assistant'}
            </span>
            <span className="text-[10px] opacity-70">
              {new Date(m.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
            </span>
          </div>
          
          <p className="whitespace-pre-wrap text-sm leading-relaxed">{m.message}</p>

          {m.intent && (
            <div className={`mt-2 text-xs rounded px-2 py-1 border ${
              isUser ? 'bg-blue-500/30 border-blue-400' : 'bg-indigo-50 text-indigo-800 border-indigo-100'
            }`}>
              <span className="font-semibold">Intent:</span> {m.intent}
            </div>
          )}

          {Array.isArray(m.warnings) && m.warnings.length > 0 && (
            <div className={`mt-2 text-xs rounded px-2 py-1 border flex items-start gap-1 ${
              isUser ? 'bg-yellow-500/20 border-yellow-400' : 'bg-yellow-50 text-yellow-900 border-yellow-200'
            }`}>
              <AlertCircle className="h-3 w-3 mt-0.5 flex-shrink-0" />
              <div className="flex-1">
                <span className="font-semibold">Warnings</span>
                <ul className="list-disc ml-4 mt-1">{m.warnings.map((w, i) => <li key={i}>{w}</li>)}</ul>
              </div>
            </div>
          )}

          {Array.isArray(m.errors) && m.errors.length > 0 && (
            <div className={`mt-2 text-xs rounded px-2 py-1 border flex items-start gap-1 ${
              isUser ? 'bg-red-500/20 border-red-400' : 'bg-red-50 text-red-900 border-red-200'
            }`}>
              <AlertCircle className="h-3 w-3 mt-0.5 flex-shrink-0" />
              <div className="flex-1">
                <span className="font-semibold">Errors</span>
                <ul className="list-disc ml-4 mt-1">{m.errors.map((e, i) => <li key={i}>{e}</li>)}</ul>
              </div>
            </div>
          )}

          {m.applied && (
            <div className="mt-2">
              <div className="inline-flex items-center gap-1 text-xs bg-green-50 text-green-800 rounded px-2 py-1 border border-green-200 mb-2">
                <CheckCircle2 className="h-3 w-3" />
                <span className="font-semibold">Changes Applied</span>
              </div>
              
              {/* Show applied changes details */}
              {m.diff && (
                <div className="border rounded-lg overflow-hidden bg-green-50/50">
                  <div className="px-3 py-2 border-b bg-green-100 text-xs font-medium text-green-800">
                    Applied Changes
                  </div>
                  <div className="overflow-auto" style={{ maxHeight: '300px' }}>
                    <DiffViewer
                      sections={convertItineraryDiffToSections(m.diff)}
                      viewMode={'unified'}
                      showUnchanged={false}
                    />
                  </div>
                </div>
              )}
              
              {/* Show changeSet details if no diff available */}
              {!m.diff && m.changeSet && (
                <div className="border rounded-lg overflow-hidden bg-green-50/50">
                  <div className="px-3 py-2 border-b bg-green-100 text-xs font-medium text-green-800">
                    Applied Changes
                  </div>
                  <div className="overflow-auto" style={{ maxHeight: '300px' }}>
                    <DiffViewer
                      sections={createChangeSetDiff(m.changeSet)}
                      viewMode={'unified'}
                      showUnchanged={false}
                    />
                  </div>
                </div>
              )}
            </div>
          )}

          {!isUser && Array.isArray(m.candidates) && m.candidates.length > 0 && (
            <div className="mt-3 border rounded-lg bg-white overflow-hidden">
              <div className="px-3 py-2 border-b bg-gray-50 text-xs font-medium">
                Did you mean one of these?
              </div>
              <div className="p-3 space-y-2 max-h-48 overflow-y-auto">
                {m.candidates.slice(0, 5).map((c: any, idx: number) => (
                  <div 
                    key={c.id || idx} 
                    className="flex items-center justify-between gap-2 text-sm hover:bg-gray-50 p-2 rounded transition-colors"
                  >
                    <div className="min-w-0 flex-1">
                      <div className="font-medium truncate text-gray-900">{c.title}</div>
                      <div className="text-xs text-gray-500 truncate">
                        Day {c.day}{c.location ? ` • ${c.location}` : ''}
                      </div>
                    </div>
                    <Button 
                      size="sm" 
                      variant="outline"
                      className="flex-shrink-0"
                      onClick={() => dispatch({ type: 'INPUT_SET', value: `Use "${c.title}" from day ${c.day}` })}
                    >
                      Select
                    </Button>
                  </div>
                ))}
              </div>
            </div>
          )}

          {showPreview && (
            <div className="mt-3">
              <div className="mb-2 flex justify-between items-center">
                <span className="text-xs font-medium opacity-80">Preview Changes</span>
                <button 
                  className="text-xs text-blue-600 hover:text-blue-800 underline transition-colors"
                  onClick={() => dispatch({ type: 'TOGGLE_DETAIL', messageId })}
                >
                  {isDetailed ? 'Hide Details' : 'Show Details'}
                </button>
              </div>
              
              {isDetailed ? (
                <div className="border rounded-lg overflow-hidden bg-white">
                  <div className="overflow-auto" style={{ maxHeight: '400px' }}>
                    <DiffViewer
                      sections={m.diff ? convertItineraryDiffToSections(m.diff) : createChangeSetDiff(m.changeSet)}
                      viewMode={'unified'}
                      showUnchanged={false}
                    />
                  </div>
                  <div className="p-3 bg-gray-50 border-t flex justify-end gap-2">
                    <Button 
                      variant="outline" 
                      size="sm"
                      onClick={() => dispatch({ type: 'TOGGLE_DETAIL', messageId })}
                    >
                      Cancel
                    </Button>
                    <Button 
                      size="sm"
                      disabled={isApplying}
                      onClick={() => onApplyChanges(messageId, m.changeSet)}
                    >
                      {isApplying ? (
                        <>
                          <Loader2 className="h-3 w-3 mr-1 animate-spin" />
                          Applying...
                        </>
                      ) : (
                        'Apply Changes'
                      )}
                    </Button>
                  </div>
                </div>
              ) : (
                <div className="border rounded-lg p-3 bg-gradient-to-br from-blue-50 to-indigo-50">
                  <div className="text-xs text-gray-600 mb-2">
                    This message contains proposed changes to your itinerary
                  </div>
                  <Button 
                    size="sm"
                    disabled={isApplying}
                    onClick={() => onApplyChanges(messageId, m.changeSet)}
                    className="w-full"
                  >
                    {isApplying ? (
                      <>
                        <Loader2 className="h-3 w-3 mr-1 animate-spin" />
                        Applying Changes...
                      </>
                    ) : (
                      'Apply Changes'
                    )}
                  </Button>
                </div>
              )}
            </div>
          )}
        </div>
      </div>
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
            {uix.isConnected ? (
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
          {state.messages.length > 0 && (
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
      <div className="flex-1 overflow-y-auto p-4">
        {state.status === 'loading_history' && (
          <div className="flex items-center justify-center py-8">
            <Loader2 className="h-6 w-6 animate-spin text-blue-600" />
            <span className="ml-2 text-sm text-gray-600">Loading chat history...</span>
          </div>
        )}

        {state.messages.length === 0 && state.status !== 'loading_history' && (
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

        {state.messages.map(renderMessage)}
        <div ref={endRef} />
      </div>

      {/* Input */}
      <div className="p-4 border-t bg-white shadow-lg">
        {!itineraryId && (
          <div className="mb-3 p-2 bg-amber-50 border border-amber-200 rounded text-sm text-amber-900">
            Loading itinerary... Please wait before sending a message.
          </div>
        )}
        <div className="flex gap-2">
          <textarea
            className="flex-1 min-h-[44px] max-h-32 resize-none border border-gray-300 rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all"
            placeholder={itineraryId ? "Ask me about your trip…" : "Loading..."}
            value={state.input}
            onChange={(e) => dispatch({ type: 'INPUT_SET', value: e.target.value })}
            onKeyDown={(e) => { 
              if (e.key === 'Enter' && !e.shiftKey) { 
                e.preventDefault(); 
                void onSend(); 
              } 
            }}
            disabled={!itineraryId || state.status === 'sending' || state.status === 'applying'}
          />
          <Button 
            onClick={onSend} 
            disabled={!state.input.trim() || !itineraryId || state.status === 'sending' || state.status === 'applying'} 
            className="min-h-[44px] px-6"
            aria-label="Send"
            title="Send"
          >
            {state.status === 'sending' ? (
              <Loader2 className="h-4 w-4 animate-spin" />
            ) : (
              <Send className="h-4 w-4" />
            )}
          </Button>
        </div>
      </div>
    </div>
  );
};


