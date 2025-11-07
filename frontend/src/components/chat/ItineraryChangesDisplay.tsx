/**
 * Itinerary Changes Display Component
 * High-end display of itinerary changes in chat interface
 */

import { useState } from 'react';
import { Card } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Plus, Minus, ChevronDown, ChevronUp, Undo2 } from 'lucide-react';
import { cn } from '@/lib/utils';
import type { ItineraryDiff, ChangeDisplayItem, ChangeType } from '@/types/ItineraryChanges';

interface ItineraryChangesDisplayProps {
    diff: ItineraryDiff;
    message?: string;
    onUndo?: (nodeId: string, changeType: ChangeType) => void;
    onViewItinerary?: () => void;
    compact?: boolean;
}

export function ItineraryChangesDisplay({
    diff,
    message,
    onUndo,
    onViewItinerary,
    compact = false,
}: ItineraryChangesDisplayProps) {
    const [isExpanded, setIsExpanded] = useState(!compact);

    // Convert diff to display items
    const changes: ChangeDisplayItem[] = [
        ...(diff.added || []).map(item => ({
            type: 'added' as ChangeType,
            nodeId: item.nodeId,
            day: item.day,
            title: item.title || item.nodeId || 'Unnamed item',
            fields: item.fields,
        })),
        ...(diff.updated || []).map(item => ({
            type: 'modified' as ChangeType,
            nodeId: item.nodeId,
            day: item.day,
            title: item.title || item.nodeId || 'Unnamed item',
            fields: item.fields,
        })),
        ...(diff.removed || []).map(item => ({
            type: 'removed' as ChangeType,
            nodeId: item.nodeId,
            day: item.day,
            title: item.title || item.nodeId || 'Unnamed item',
            fields: item.fields,
        })),
    ];

    const totalChanges = changes.length;

    if (totalChanges === 0) {
        return null;
    }

    return (
        <Card className="p-2.5 sm:p-4 shadow-sm border border-gray-200 bg-white my-2 sm:my-3">
            {/* Header */}
            <div className="flex items-center justify-between mb-2 sm:mb-3">
                <div className="flex items-center gap-1.5 sm:gap-2 min-w-0 flex-1">
                    <div className="w-6 h-6 sm:w-8 sm:h-8 rounded-full bg-primary/10 flex items-center justify-center flex-shrink-0">
                        <span className="text-sm sm:text-lg">✨</span>
                    </div>
                    <div className="min-w-0 flex-1">
                        <h3 className="font-semibold text-gray-900 text-xs sm:text-sm truncate">
                            {message || 'Your itinerary has been updated'}
                        </h3>
                        <p className="text-xs sm:text-sm text-gray-600">
                            {totalChanges} {totalChanges === 1 ? 'change' : 'changes'}
                        </p>
                    </div>
                </div>

                {compact && (
                    <Button
                        variant="ghost"
                        size="sm"
                        onClick={() => setIsExpanded(!isExpanded)}
                        className="text-gray-600 h-7 sm:h-8 px-2 flex-shrink-0"
                    >
                        {isExpanded ? (
                            <>
                                <ChevronUp className="w-3 h-3 sm:w-4 sm:h-4 sm:mr-1" />
                                <span className="hidden sm:inline">Hide</span>
                            </>
                        ) : (
                            <>
                                <ChevronDown className="w-3 h-3 sm:w-4 sm:h-4 sm:mr-1" />
                                <span className="hidden sm:inline">View details</span>
                            </>
                        )}
                    </Button>
                )}
            </div>

            {/* Changes List */}
            {isExpanded && (
                <div className="space-y-1.5 sm:space-y-2 mb-2 sm:mb-3" role="list">
                    {changes.map((change, index) => (
                        <ChangeItem
                            key={`${change.nodeId}-${index}`}
                            change={change}
                            onUndo={onUndo}
                        />
                    ))}
                </div>
            )}

            {/* Actions */}
            {onViewItinerary && (
                <div className="pt-2 sm:pt-3 border-t border-gray-100">
                    <Button
                        variant="outline"
                        size="sm"
                        onClick={onViewItinerary}
                        className="w-full h-8 sm:h-9 text-xs sm:text-sm"
                    >
                        View full itinerary
                    </Button>
                </div>
            )}
        </Card>
    );
}

interface ChangeItemProps {
    change: ChangeDisplayItem;
    onUndo?: (nodeId: string, changeType: ChangeType) => void;
}

function ChangeItem({ change, onUndo }: ChangeItemProps) {
    const [isUndoing, setIsUndoing] = useState(false);

    const handleUndo = () => {
        if (!onUndo) return;
        setIsUndoing(true);
        try {
            onUndo(change.nodeId, change.type);
            setIsUndoing(false);
        } catch (error) {
            console.error('Failed to undo change:', error);
            setIsUndoing(false);
        }
    };

    const colorClass = getChangeColor(change.type);
    const icon = getChangeIcon(change.type);
    const label = getChangeLabel(change.type);

    return (
        <div
            className={cn(
                'flex items-center justify-between p-2 sm:p-3 rounded-lg border transition-all duration-300',
                colorClass,
                change.type === 'added' && 'animate-slide-in-from-top',
                change.type === 'removed' && 'opacity-60 line-through',
                change.type === 'modified' && 'animate-highlight-flash'
            )}
            role="listitem"
            aria-label={`${label} ${change.title}`}
        >
            <div className="flex items-center gap-2 sm:gap-3 flex-1 min-w-0">
                {/* Icon */}
                <div className="flex-shrink-0">
                    {icon}
                </div>

                {/* Content */}
                <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-1.5 sm:gap-2 flex-wrap">
                        <span className="font-medium truncate text-xs sm:text-sm">{change.title}</span>
                        <span className="text-xs px-1.5 py-0.5 rounded-full bg-white/50 flex-shrink-0">
                            {label}
                        </span>
                        {change.day && (
                            <span className="text-xs text-gray-600 flex-shrink-0">
                                Day {change.day}
                            </span>
                        )}
                    </div>

                    {change.fields && change.fields.length > 0 && (
                        <p className="text-xs text-gray-600 mt-0.5 sm:mt-1 truncate">
                            Updated: {change.fields.join(', ')}
                        </p>
                    )}
                </div>

                {/* Undo Button */}
                {onUndo && (
                    <Button
                        variant="ghost"
                        size="sm"
                        onClick={handleUndo}
                        disabled={isUndoing}
                        className="flex-shrink-0 h-7 sm:h-8 px-1.5 sm:px-2 text-xs"
                        aria-label={`Undo ${label} ${change.title}`}
                    >
                        <Undo2 className="w-3 h-3 sm:mr-1" />
                        <span className="hidden sm:inline">Undo</span>
                    </Button>
                )}
            </div>
        </div>
    );
}

// Helper functions
function getChangeIcon(type: ChangeType) {
    switch (type) {
        case 'added':
            return <Plus className="w-3 h-3 sm:w-4 sm:h-4" aria-label="Added" />;
        case 'modified':
            return <span className="text-sm sm:text-base font-bold" aria-label="Modified">~</span>;
        case 'removed':
            return <Minus className="w-3 h-3 sm:w-4 sm:h-4" aria-label="Removed" />;
    }
}

function getChangeColor(type: ChangeType) {
    switch (type) {
        case 'added':
            return 'text-green-700 bg-green-50 border-green-200 dark:text-green-400 dark:bg-green-950/30 dark:border-green-800';
        case 'modified':
            return 'text-yellow-700 bg-yellow-50 border-yellow-200 dark:text-yellow-400 dark:bg-yellow-950/30 dark:border-yellow-800';
        case 'removed':
            return 'text-gray-700 bg-gray-50 border-gray-200 dark:text-gray-400 dark:bg-gray-900/30 dark:border-gray-700';
    }
}

function getChangeLabel(type: ChangeType) {
    switch (type) {
        case 'added':
            return 'added';
        case 'modified':
            return 'modified';
        case 'removed':
            return 'removed';
    }
}
