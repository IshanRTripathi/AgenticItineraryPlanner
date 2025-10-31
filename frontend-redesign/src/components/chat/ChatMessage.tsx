/**
 * Chat Message Component
 * Displays individual chat messages with change previews
 */

import { memo } from 'react';
import { Button } from '@/components/ui/button';
import { AlertCircle, CheckCircle2, Loader2 } from 'lucide-react';
import { ChatMessage as ChatMessageType } from '@/contexts/UnifiedItineraryTypes';

interface ChatMessageProps {
  message: ChatMessageType;
  messageId: string;
  isDetailed: boolean;
  isApplying: boolean;
  onToggleDetail: (messageId: string) => void;
  onApplyChanges: (messageId: string, changeSet: any) => void;
  onSelectCandidate: (text: string) => void;
}

/**
 * Get a human-readable summary of changes
 */
const getChangeSummary = (changeSet: any): string => {
  if (!changeSet) return 'Changes to your itinerary';

  const changes: string[] = [];
  if (changeSet.added?.length) changes.push(`${changeSet.added.length} added`);
  if (changeSet.modified?.length) changes.push(`${changeSet.modified.length} modified`);
  if (changeSet.removed?.length) changes.push(`${changeSet.removed.length} removed`);

  return changes.length > 0 ? changes.join(', ') : 'Updates to your itinerary';
};

/**
 * Simple change visualization
 */
const renderChangeSet = (changeSet: any) => {
  if (!changeSet) return <div className="text-sm text-gray-500">No changes</div>;

  const renderChange = (change: any, type: 'added' | 'modified' | 'removed') => {
    const config = {
      added: { label: 'Add', color: 'text-green-700', bg: 'bg-green-50' },
      modified: { label: 'Change', color: 'text-blue-700', bg: 'bg-blue-50' },
      removed: { label: 'Remove', color: 'text-red-700', bg: 'bg-red-50' },
    };

    const { label, color, bg } = config[type];

    return (
      <div key={change.id || change.title} className={`p-2 rounded ${bg}`}>
        <div className="flex items-start gap-2 text-sm">
          <span className={`font-medium ${color} min-w-[60px]`}>{label}:</span>
          <span className="text-gray-900">{change.title || change.name || 'Item'}</span>
          {change.day && <span className="text-gray-500 text-xs">(Day {change.day})</span>}
        </div>
      </div>
    );
  };

  return (
    <div className="space-y-1.5">
      {changeSet.added?.map((change: any) => renderChange(change, 'added'))}
      {changeSet.modified?.map((change: any) => renderChange(change, 'modified'))}
      {changeSet.removed?.map((change: any) => renderChange(change, 'removed'))}
    </div>
  );
};

/**
 * Memoized chat message component for better performance
 */
export const ChatMessageComponent = memo<ChatMessageProps>(({
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
      <div className={`max-w-[85%] rounded-xl px-3 py-2 shadow-sm transition-all ${isUser
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

        <p className="whitespace-pre-wrap text-sm leading-relaxed">{m.text}</p>

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

        {m.applied && m.changeSet && (
          <div className="mt-3 border rounded-lg overflow-hidden bg-white">
            {/* Success Header */}
            <div className="px-3 py-2 bg-green-50 border-b border-green-200 flex items-center justify-between">
              <div className="flex items-center gap-2">
                <CheckCircle2 className="h-4 w-4 text-green-600" />
                <span className="text-sm font-medium text-green-700">Changes Applied Successfully</span>
              </div>
              <button
                className="text-xs text-green-600 hover:underline"
                onClick={() => onToggleDetail(messageId)}
              >
                {isDetailed ? 'Hide' : 'Show what changed'}
              </button>
            </div>

            {/* Applied Changes Details */}
            {isDetailed && (
              <div className="p-3 max-h-80 overflow-y-auto">
                {renderChangeSet(m.changeSet)}
              </div>
            )}

            {/* Summary when collapsed */}
            {!isDetailed && (
              <div className="px-3 py-2 text-sm text-gray-600">
                {getChangeSummary(m.changeSet)}
              </div>
            )}
          </div>
        )}

        {/* Simple success message if no changeSet */}
        {m.applied && !m.changeSet && (
          <div className="mt-3">
            <div className="inline-flex items-center gap-2 text-sm bg-green-50 text-green-700 rounded-lg px-3 py-2 border border-green-200">
              <CheckCircle2 className="h-4 w-4 text-green-600" />
              <span className="font-medium">Changes Applied Successfully</span>
            </div>
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
          <div className="mt-3 border rounded-lg overflow-hidden bg-white">
            {/* Header */}
            <div className="px-3 py-2 bg-gray-50 border-b flex items-center justify-between">
              <span className="text-sm font-medium text-gray-700">Proposed Changes</span>
              <button
                className="text-xs text-blue-600 hover:underline"
                onClick={() => onToggleDetail(messageId)}
              >
                {isDetailed ? 'Hide' : 'Show details'}
              </button>
            </div>

            {/* Changes */}
            {isDetailed && (
              <div className="p-3 max-h-80 overflow-y-auto">
                {renderChangeSet(m.changeSet)}
              </div>
            )}

            {/* Summary when collapsed */}
            {!isDetailed && (
              <div className="px-3 py-2 text-sm text-gray-600">
                {getChangeSummary(m.changeSet)}
              </div>
            )}

            {/* Actions */}
            <div className="px-3 py-2 bg-gray-50 border-t flex justify-end gap-2">
              <Button
                variant="outline"
                size="sm"
                onClick={() => onToggleDetail(messageId)}
              >
                {isDetailed ? 'Close' : 'View'}
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
                  'Apply'
                )}
              </Button>
            </div>
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

ChatMessageComponent.displayName = 'ChatMessage';
