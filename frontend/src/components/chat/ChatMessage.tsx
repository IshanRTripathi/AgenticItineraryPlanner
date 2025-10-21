import React, { memo } from 'react';
import { Button } from '../ui/button';
import { AlertCircle, CheckCircle2, Loader2 } from 'lucide-react';
import { ChatMessageDTO } from '../../services/chatApi';
import { DiffViewer } from '../diff/DiffViewer';
import { convertItineraryDiffToSections, createChangeSetDiff } from '../../utils/diffUtils';

interface ChatMessageProps {
  message: ChatMessageDTO;
  messageId: string;
  isDetailed: boolean;
  isApplying: boolean;
  onToggleDetail: (messageId: string) => void;
  onApplyChanges: (messageId: string, changeSet: any) => void;
  onSelectCandidate: (text: string) => void;
}

/**
 * Memoized chat message component for better performance with large message lists
 */
export const ChatMessage = memo<ChatMessageProps>(({
  message: m,
  messageId,
  isDetailed,
  isApplying,
  onToggleDetail,
  onApplyChanges,
  onSelectCandidate,
}) => {
  const isUser = m.sender === 'user';
  const showPreview = !!m.changeSet && !m.applied;
  
  return (
    <div className={`flex ${isUser ? 'justify-end' : 'justify-start'} mb-3`}>
      <div className={`max-w-[85%] rounded-xl px-3 py-2 shadow-sm transition-all ${
        isUser 
          ? 'bg-gray-100 text-gray-900 border border-gray-200' 
          : 'bg-white text-gray-900 border border-gray-200'
      }`}>
        <div className="flex items-center justify-between gap-2 mb-2">
          <span className="text-xs font-medium text-gray-600">
            {isUser ? 'You' : 'AI Assistant'}
          </span>
          <span className="text-xs text-gray-500">
            {new Date(m.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
          </span>
        </div>
        
        <p className="whitespace-pre-wrap text-sm leading-relaxed">{m.message}</p>

        {m.intent && (
          <div className="mt-2 text-xs text-gray-600 bg-gray-50 rounded-lg px-3 py-2 border border-gray-200">
            <span className="font-medium text-gray-700">Intent:</span> {m.intent}
          </div>
        )}

        {Array.isArray(m.warnings) && m.warnings.length > 0 && (
          <div className="mt-2 text-xs bg-yellow-50 text-yellow-800 rounded-lg px-3 py-2 border border-yellow-200 flex items-start gap-2">
            <AlertCircle className="h-3 w-3 mt-0.5 flex-shrink-0 text-yellow-600" />
            <div className="flex-1">
              <span className="font-medium">Warnings</span>
              <ul className="list-disc ml-4 mt-1 space-y-1">{m.warnings.map((w, i) => <li key={i}>{w}</li>)}</ul>
            </div>
          </div>
        )}

        {Array.isArray(m.errors) && m.errors.length > 0 && (
          <div className="mt-2 text-xs bg-red-50 text-red-800 rounded-lg px-3 py-2 border border-red-200 flex items-start gap-2">
            <AlertCircle className="h-3 w-3 mt-0.5 flex-shrink-0 text-red-600" />
            <div className="flex-1">
              <span className="font-medium">Errors</span>
              <ul className="list-disc ml-4 mt-1 space-y-1">{m.errors.map((e, i) => <li key={i}>{e}</li>)}</ul>
            </div>
          </div>
        )}

        {m.applied && (
          <div className="mt-3">
            <div className="inline-flex items-center gap-2 text-sm bg-green-50 text-green-700 rounded-lg px-3 py-2 border border-green-200">
              <CheckCircle2 className="h-4 w-4 text-green-600" />
              <span className="font-medium">Changes Applied Successfully</span>
            </div>
            
            {(m.diff || m.changeSet) && (
              <details className="mt-2">
                <summary className="text-xs text-gray-600 cursor-pointer hover:text-gray-800 transition-colors">
                  View Details
                </summary>
                <div className="mt-2 border rounded-lg overflow-hidden bg-gray-50">
                  <div className="overflow-auto" style={{ maxHeight: '200px' }}>
                    <DiffViewer
                      sections={m.diff ? convertItineraryDiffToSections(m.diff) : createChangeSetDiff(m.changeSet)}
                      viewMode={'unified'}
                      showUnchanged={false}
                    />
                  </div>
                </div>
              </details>
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
                    onClick={() => onSelectCandidate(`Use "${c.title}" from day ${c.day}`)}
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
                onClick={() => onToggleDetail(messageId)}
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
                    onClick={() => onToggleDetail(messageId)}
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
}, (prevProps, nextProps) => {
  // Custom comparison for memo - only re-render if these props change
  return (
    prevProps.messageId === nextProps.messageId &&
    prevProps.isDetailed === nextProps.isDetailed &&
    prevProps.isApplying === nextProps.isApplying &&
    prevProps.message.applied === nextProps.message.applied
  );
});

ChatMessage.displayName = 'ChatMessage';
