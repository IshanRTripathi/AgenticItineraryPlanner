/**
 * Chat Message Component
 * Displays individual chat messages with high-end change previews
 * Integrated with ItineraryChangesDisplay for premium UX
 */

import { memo } from 'react';
import { Button } from '@/components/ui/button';
import { AlertCircle, CheckCircle2, Loader2 } from 'lucide-react';
import { ChatMessage as ChatMessageType } from '@/contexts/UnifiedItineraryTypes';
import { PlaceSuggestion } from '@/types/ChatTypes';
import { ItineraryChangesDisplay } from './ItineraryChangesDisplay';
import { CostImpactDisplay, type CostImpact } from './CostImpactDisplay';
import PlaceSuggestionCard from './PlaceSuggestionCard';
import type { ItineraryDiff } from '@/types/ItineraryChanges';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';

interface ChatMessageProps {
  message: ChatMessageType;
  messageId: string;
  isDetailed: boolean;
  isApplying: boolean;
  onToggleDetail: (messageId: string) => void;
  onApplyChanges: (messageId: string, changeSet: any) => void;
  onSelectCandidate: (text: string) => void;
  progress?: { message: string; progress: number } | null;
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
  progress,
}) => {
  const isUser = m.sender === 'user';
  const showPreview = !!m.changeSet && !m.applied;
  
  // Get cost impact from message
  const costImpact = (m as any).costImpact as CostImpact | undefined;

  // Convert changeSet/diff to ItineraryDiff format with comprehensive null safety
  const getItineraryDiff = (): ItineraryDiff | null => {
    try {
      // Check if we have a diff object (new format from backend)
      if (m.diff && typeof m.diff === 'object') {
        return {
          added: Array.isArray(m.diff.added) ? m.diff.added : [],
          removed: Array.isArray(m.diff.removed) ? m.diff.removed : [],
          updated: Array.isArray(m.diff.updated) ? m.diff.updated : [],
        };
      }

      // Fallback to changeSet (old format or chat response format)
      if (m.changeSet && typeof m.changeSet === 'object') {
        return {
          added: Array.isArray(m.changeSet.added) ? m.changeSet.added : [],
          removed: Array.isArray(m.changeSet.removed) ? m.changeSet.removed : [],
          updated: Array.isArray(m.changeSet.modified) 
            ? m.changeSet.modified 
            : Array.isArray(m.changeSet.updated) 
              ? m.changeSet.updated 
              : [],
        };
      }

      return null;
    } catch (error) {
      console.error('Error parsing itinerary diff:', error);
      return null;
    }
  };

  const diff = getItineraryDiff();
  
  // Check if diff has any actual changes
  const hasChanges = diff && (
    (diff.added && diff.added.length > 0) ||
    (diff.updated && diff.updated.length > 0) ||
    (diff.removed && diff.removed.length > 0)
  );

  return (
    <div className={`flex ${isUser ? 'justify-end' : 'justify-start'} mb-3 sm:mb-4`}>
      <div className={`max-w-[90%] sm:max-w-[85%] rounded-lg sm:rounded-xl px-3 py-2 sm:px-4 sm:py-3 shadow-sm transition-all ${isUser
        ? 'bg-gradient-to-br from-primary/10 to-primary/5 text-gray-900 border border-primary/20'
        : 'bg-white text-gray-900 border border-gray-200 shadow-md'
        }`}>
        <div className="flex items-center justify-between gap-2 mb-1.5 sm:mb-2">
          <span className="text-xs font-semibold text-gray-700">
            {isUser ? 'You' : '✨ AI Assistant'}
          </span>
          <span className="text-xs text-gray-500">
            {new Date(m.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
          </span>
        </div>

        {/* Applied Changes - Use Premium Display */}
        {m.applied && hasChanges ? (
          <div className="mt-2" key={`applied-${messageId}`}>
            <ItineraryChangesDisplay
              diff={diff!}
              message={m.text || "Changes applied successfully"}
              compact={false}
              onUndo={undefined}
              onViewItinerary={undefined}
            />
          </div>
        ) : (
          <>
            <div className="prose prose-sm max-w-none text-xs sm:text-sm leading-relaxed mb-1.5 sm:mb-2">
              <ReactMarkdown
                remarkPlugins={[remarkGfm]}
                components={{
                  // Customize markdown rendering
                  p: ({ children }) => <p className="mb-2 last:mb-0">{children}</p>,
                  ul: ({ children }) => <ul className="list-disc ml-4 mb-2 space-y-1">{children}</ul>,
                  ol: ({ children }) => <ol className="list-decimal ml-4 mb-2 space-y-1">{children}</ol>,
                  li: ({ children }) => <li className="leading-relaxed">{children}</li>,
                  strong: ({ children }) => <strong className="font-semibold text-gray-900">{children}</strong>,
                  em: ({ children }) => <em className="italic">{children}</em>,
                  code: ({ children }) => <code className="bg-gray-100 px-1 py-0.5 rounded text-xs font-mono">{children}</code>,
                  h1: ({ children }) => <h1 className="text-base font-bold mb-2 mt-3 first:mt-0">{children}</h1>,
                  h2: ({ children }) => <h2 className="text-sm font-bold mb-2 mt-3 first:mt-0">{children}</h2>,
                  h3: ({ children }) => <h3 className="text-sm font-semibold mb-1 mt-2 first:mt-0">{children}</h3>,
                  blockquote: ({ children }) => <blockquote className="border-l-2 border-gray-300 pl-3 italic text-gray-700">{children}</blockquote>,
                  a: ({ href, children }) => <a href={href} className="text-blue-600 hover:underline" target="_blank" rel="noopener noreferrer">{children}</a>,
                }}
              >
                {m.text}
              </ReactMarkdown>
            </div>
            
            {/* Progress Indicator (for AI messages being processed) */}
            {!isUser && progress && (
              <div className="mt-2 flex items-center gap-2 text-xs text-gray-600 bg-gray-50 rounded px-2 py-1.5">
                <Loader2 className="h-3 w-3 animate-spin" />
                <span>{progress.message}</span>
                <div className="flex-1 bg-gray-200 rounded-full h-1.5 ml-2">
                  <div 
                    className="bg-primary h-1.5 rounded-full transition-all duration-300"
                    style={{ width: `${progress.progress}%` }}
                  />
                </div>
                <span className="text-xs font-medium">{progress.progress}%</span>
              </div>
            )}
          </>
        )}

        {Array.isArray(m.warnings) && m.warnings.length > 0 && (
          <div className="mt-2 text-xs bg-yellow-50 text-yellow-800 rounded px-2 py-1.5 sm:px-3 sm:py-2 border border-yellow-200 flex items-start gap-1.5 sm:gap-2">
            <AlertCircle className="h-3 w-3 mt-0.5 flex-shrink-0 text-yellow-600" />
            <div className="flex-1 min-w-0">
              <span className="font-medium">Warnings</span>
              <ul className="list-disc ml-3 sm:ml-4 mt-1 space-y-1">{m.warnings.map((w, i) => <li key={i}>{w}</li>)}</ul>
            </div>
          </div>
        )}

        {/* Simple success message if no changeSet */}
        {m.applied && !hasChanges && (
          <div className="mt-2 sm:mt-3">
            <div className="inline-flex items-center gap-1.5 sm:gap-2 text-xs sm:text-sm bg-green-50 text-green-700 rounded px-2 py-1.5 sm:px-3 sm:py-2 border border-green-200">
              <CheckCircle2 className="h-3 w-3 sm:h-4 sm:w-4 text-green-600" />
              <span className="font-medium">Changes Applied Successfully</span>
            </div>
          </div>
        )}

        {!isUser && Array.isArray(m.candidates) && m.candidates.length > 0 && (
          <div className="mt-2 sm:mt-3 border rounded-lg bg-white overflow-hidden">
            <div className="px-2 py-1.5 sm:px-3 sm:py-2 border-b bg-gray-50 text-xs font-medium">
              Did you mean one of these?
            </div>
            <div className="p-2 sm:p-3 space-y-1.5 sm:space-y-2 max-h-40 sm:max-h-48 overflow-y-auto">
              {m.candidates.slice(0, 5).map((c: any, idx: number) => (
                <div
                  key={c.id || idx}
                  className="flex items-center justify-between gap-2 text-xs sm:text-sm hover:bg-gray-50 p-1.5 sm:p-2 rounded transition-colors"
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
                    className="flex-shrink-0 h-7 text-xs px-2"
                    onClick={() => onSelectCandidate(`Use "${c.title}" from day ${c.day}`)}
                  >
                    Select
                  </Button>
                </div>
              ))}
            </div>
          </div>
        )}
        
        {/* Place Suggestions - Always show 3 vertically stacked */}
        {!isUser && (m as any).placeSuggestions && Array.isArray((m as any).placeSuggestions) && (m as any).placeSuggestions.length > 0 && (
          <div className="mt-4 space-y-3">
            <div className="text-sm font-medium text-gray-700 mb-2">
              {(m as any).placeSuggestions.length === 1 
                ? '1 suggestion found:' 
                : `${(m as any).placeSuggestions.length} suggestions found:`}
            </div>
            
            {/* Render exactly 3 suggestions (or less if not available) */}
            {(m as any).placeSuggestions.slice(0, 3).map((suggestion: any, idx: number) => (
              <PlaceSuggestionCard
                key={suggestion.placeId}
                suggestion={suggestion}
                index={idx + 1}
                onSelect={(selected) => {
                  // Build detailed selection message with place info
                  const parts: string[] = [];
                  parts.push(`Add "${selected.name}"`);
                  
                  // Add location context if available
                  if (selected.address) {
                    const shortAddress = selected.address.split(',')[0];
                    parts.push(`(${shortAddress})`);
                  }
                  
                  // Use day from suggestion object (already 1-indexed from backend)
                  // Fallback to parsing from message text if not available
                  const dayNumber = selected.day || (() => {
                    const dayMatch = m.text.match(/day\s+(\d+)/i);
                    return dayMatch ? parseInt(dayMatch[1]) : null;
                  })();
                  
                  if (dayNumber) {
                    parts.push(`to day ${dayNumber}`);
                  } else {
                    parts.push(`to the itinerary`);
                  }
                  
                  // Add distance context if available
                  if (selected.distanceKm !== undefined) {
                    const distText = selected.distanceKm < 1 
                      ? `${(selected.distanceKm * 1000).toFixed(0)}m away`
                      : `${selected.distanceKm.toFixed(1)}km away`;
                    parts.push(`[${distText}]`);
                  }
                  
                  onSelectCandidate(parts.join(' '));
                }}
              />
            ))}
            
            {(m as any).placeSuggestions.length === 0 && (
              <div className="text-sm text-gray-500 italic">
                No suggestions found. Try a different search.
              </div>
            )}
          </div>
        )}

        {/* Proposed Changes - Use Premium Display with Apply Action */}
        {showPreview && hasChanges && (
          <div className="mt-2 sm:mt-3 space-y-2">
            <ItineraryChangesDisplay
              diff={diff!}
              message="Proposed changes to your itinerary"
              compact={false} // Always show expanded for better UX
              onUndo={undefined} // No undo for proposed changes
              onViewItinerary={undefined}
            />
            
            {/* Cost Impact Preview */}
            {costImpact && (
              <CostImpactDisplay costImpact={costImpact} />
            )}
            
            {/* Apply Actions */}
            <div className="mt-2 flex justify-end gap-1.5 sm:gap-2">
              <Button
                variant="outline"
                size="sm"
                onClick={() => onToggleDetail(messageId)}
                className="h-7 sm:h-8 text-xs px-2 sm:px-3"
              >
                {isDetailed ? 'Collapse' : 'Expand'}
              </Button>
              <Button
                size="sm"
                disabled={isApplying}
                onClick={() => onApplyChanges(messageId, m.changeSet)}
                className="bg-primary hover:bg-primary-hover h-7 sm:h-8 text-xs px-2 sm:px-3"
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
    prevProps.message.applied === nextProps.message.applied &&
    prevProps.message.diff === nextProps.message.diff &&
    prevProps.message.changeSet === nextProps.message.changeSet
  );
});

ChatMessageComponent.displayName = 'ChatMessage';
