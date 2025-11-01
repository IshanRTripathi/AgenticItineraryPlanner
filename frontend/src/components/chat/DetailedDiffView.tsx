/**
 * Detailed Diff View Component
 * Expandable table view showing old vs new values for itinerary changes
 */

import { useState, useEffect } from 'react';
import { createPortal } from 'react-dom';
import { Card } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { X, ArrowRight } from 'lucide-react';
import { cn } from '@/lib/utils';
import type { ItineraryDiff, ChangeDisplayItem, ChangeType } from '@/types/ItineraryChanges';

interface DetailedDiffViewProps {
  diff: ItineraryDiff;
  onClose: () => void;
  nodeDetails?: Map<string, { oldValue?: any; newValue?: any }>;
}

export function DetailedDiffView({ diff, onClose, nodeDetails }: DetailedDiffViewProps) {
  // Handle escape key
  useEffect(() => {
    const handleEscape = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        onClose();
      }
    };
    
    document.addEventListener('keydown', handleEscape);
    return () => document.removeEventListener('keydown', handleEscape);
  }, [onClose]);
  // Convert diff to display items with details (with null safety)
  const changes: ChangeDisplayItem[] = [
    ...(diff.added || []).map(item => ({
      type: 'added' as ChangeType,
      nodeId: item.nodeId,
      day: item.day,
      title: item.title || item.nodeId,
      fields: item.fields,
      newValue: nodeDetails?.get(item.nodeId)?.newValue,
    })),
    ...(diff.updated || []).map(item => ({
      type: 'modified' as ChangeType,
      nodeId: item.nodeId,
      day: item.day,
      title: item.title || item.nodeId,
      fields: item.fields,
      oldValue: nodeDetails?.get(item.nodeId)?.oldValue,
      newValue: nodeDetails?.get(item.nodeId)?.newValue,
    })),
    ...(diff.removed || []).map(item => ({
      type: 'removed' as ChangeType,
      nodeId: item.nodeId,
      day: item.day,
      title: item.title || item.nodeId,
      fields: item.fields,
      oldValue: nodeDetails?.get(item.nodeId)?.oldValue,
    })),
  ];

  const modalContent = (
    <div 
      className="fixed inset-0 bg-black/50 flex items-center justify-center p-4 animate-fade-in"
      style={{ zIndex: 9999 }}
      onClick={(e) => {
        // Close on backdrop click
        if (e.target === e.currentTarget) {
          onClose();
        }
      }}
    >
      <Card className="max-w-4xl w-full max-h-[80vh] overflow-hidden flex flex-col animate-scale-in">
        {/* Header */}
        <div className="flex items-center justify-between p-4 border-b border-border">
          <h2 className="text-lg font-semibold">Detailed Changes</h2>
          <Button
            variant="ghost"
            size="sm"
            onClick={onClose}
            className="h-8 w-8 p-0"
            aria-label="Close"
          >
            <X className="w-4 h-4" />
          </Button>
        </div>

        {/* Table */}
        <div className="flex-1 overflow-auto p-4">
          <table className="w-full border-collapse">
            <thead>
              <tr className="border-b border-border">
                <th className="text-left p-3 font-semibold text-sm">Item</th>
                <th className="text-left p-3 font-semibold text-sm">Old Value</th>
                <th className="text-center p-3 w-12"></th>
                <th className="text-left p-3 font-semibold text-sm">New Value</th>
                <th className="text-center p-3 font-semibold text-sm w-24">Type</th>
              </tr>
            </thead>
            <tbody>
              {changes.map((change, index) => (
                <DiffRow key={`${change.nodeId}-${index}`} change={change} />
              ))}
            </tbody>
          </table>
        </div>

        {/* Footer */}
        <div className="p-4 border-t border-border flex justify-end">
          <Button onClick={onClose}>Close</Button>
        </div>
      </Card>
    </div>
  );

  // Render modal in a portal at document.body level
  if (typeof document === 'undefined') {
    return null;
  }
  
  return createPortal(modalContent, document.body);
}

interface DiffRowProps {
  change: ChangeDisplayItem;
}

function DiffRow({ change }: DiffRowProps) {
  const [isHovered, setIsHovered] = useState(false);

  const getTypeColor = (type: ChangeType) => {
    switch (type) {
      case 'added':
        return 'bg-green-100 text-green-700 dark:bg-green-950 dark:text-green-400';
      case 'modified':
        return 'bg-yellow-100 text-yellow-700 dark:bg-yellow-950 dark:text-yellow-400';
      case 'removed':
        return 'bg-gray-100 text-gray-700 dark:bg-gray-800 dark:text-gray-400';
    }
  };

  const getTypeLabel = (type: ChangeType) => {
    switch (type) {
      case 'added':
        return 'Added';
      case 'modified':
        return 'Modified';
      case 'removed':
        return 'Removed';
    }
  };

  const formatValue = (value: any) => {
    if (value === undefined || value === null) {
      return <span className="text-muted-foreground italic">—</span>;
    }
    if (typeof value === 'object') {
      return <pre className="text-xs">{JSON.stringify(value, null, 2)}</pre>;
    }
    return <span>{String(value)}</span>;
  };

  return (
    <tr
      className={cn(
        'border-b border-border/50 transition-colors',
        isHovered && 'bg-muted/30'
      )}
      onMouseEnter={() => setIsHovered(true)}
      onMouseLeave={() => setIsHovered(false)}
    >
      {/* Item */}
      <td className="p-3">
        <div>
          <div className="font-medium">{change.title}</div>
          {change.day && (
            <div className="text-xs text-muted-foreground">Day {change.day}</div>
          )}
          {change.fields && change.fields.length > 0 && (
            <div className="text-xs text-muted-foreground mt-1">
              Fields: {change.fields.join(', ')}
            </div>
          )}
        </div>
      </td>

      {/* Old Value */}
      <td className="p-3">
        {change.type !== 'added' && (
          <div className={cn(
            'text-sm',
            change.type === 'removed' && 'line-through opacity-60'
          )}>
            {formatValue(change.oldValue || change.title)}
          </div>
        )}
      </td>

      {/* Arrow */}
      <td className="p-3 text-center">
        {change.type === 'modified' && (
          <ArrowRight className="w-4 h-4 text-muted-foreground mx-auto" />
        )}
      </td>

      {/* New Value */}
      <td className="p-3">
        {change.type !== 'removed' && (
          <div className={cn(
            'text-sm font-medium',
            change.type === 'added' && 'text-green-600 dark:text-green-400'
          )}>
            {formatValue(change.newValue || change.title)}
          </div>
        )}
      </td>

      {/* Type Badge */}
      <td className="p-3 text-center">
        <span className={cn(
          'inline-block px-2 py-1 rounded-full text-xs font-medium',
          getTypeColor(change.type)
        )}>
          {getTypeLabel(change.type)}
        </span>
      </td>
    </tr>
  );
}
