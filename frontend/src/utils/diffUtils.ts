/**
 * Utility functions for converting itinerary diffs to DiffViewer format
 */

import { ItineraryDiff, DiffItem } from '../types/ChatTypes';
import { DiffSection, DiffChange } from '../components/diff/DiffViewer';

/**
 * Convert ItineraryDiff to DiffViewer sections
 */
export function convertItineraryDiffToSections(diff: ItineraryDiff): DiffSection[] {
  const sections: DiffSection[] = [];

  // Group changes by day
  const changesByDay = new Map<number, { added: DiffItem[], removed: DiffItem[], updated: DiffItem[] }>();

  // Process added items
  diff.added.forEach(item => {
    if (!changesByDay.has(item.day)) {
      changesByDay.set(item.day, { added: [], removed: [], updated: [] });
    }
    changesByDay.get(item.day)!.added.push(item);
  });

  // Process removed items
  diff.removed.forEach(item => {
    if (!changesByDay.has(item.day)) {
      changesByDay.set(item.day, { added: [], removed: [], updated: [] });
    }
    changesByDay.get(item.day)!.removed.push(item);
  });

  // Process updated items
  diff.updated.forEach(item => {
    if (!changesByDay.has(item.day)) {
      changesByDay.set(item.day, { added: [], removed: [], updated: [] });
    }
    changesByDay.get(item.day)!.updated.push(item);
  });

  // Create sections for each day
  const sortedDays = Array.from(changesByDay.keys()).sort((a, b) => a - b);
  
  sortedDays.forEach(day => {
    const dayChanges = changesByDay.get(day)!;
    const changes: DiffChange[] = [];

    // Add added items
    dayChanges.added.forEach(item => {
      const nodeTitle = item.title || item.nodeId || 'New Node';
      changes.push({
        type: 'added',
        path: item.nodeId || 'new-node',
        label: `${nodeTitle} (Added)`,
        newValue: formatDiffItem(item),
      });
    });

    // Add removed items
    dayChanges.removed.forEach(item => {
      const nodeTitle = item.title || item.nodeId || 'Unknown Node';
      changes.push({
        type: 'removed',
        path: item.nodeId || 'unknown-node',
        label: `${nodeTitle} (Removed)`,
        oldValue: formatDiffItem(item),
      });
    });

    // Add updated items
    dayChanges.updated.forEach(item => {
      const nodeTitle = item.title || item.nodeId || 'Unknown Node';
      if (item.fields && item.fields.length > 0) {
        // Create a change for each modified field
        item.fields.forEach(field => {
          changes.push({
            type: 'modified',
            path: `${item.nodeId || 'unknown-node'}.${field}`,
            label: `${nodeTitle} - ${field}`,
            oldValue: '(previous value)',
            newValue: '(new value)',
          });
        });
      } else {
        // Generic update
        changes.push({
          type: 'modified',
          path: item.nodeId || 'unknown-node',
          label: `${nodeTitle} (Modified)`,
          oldValue: formatDiffItem(item),
          newValue: formatDiffItem(item),
        });
      }
    });

    if (changes.length > 0) {
      sections.push({
        title: `Day ${day}`,
        changes,
      });
    }
  });

  return sections;
}

/**
 * Format a DiffItem for display
 */
function formatDiffItem(item: DiffItem): string {
  const parts: string[] = [];
  
  // Show meaningful information instead of just "undefined"
  const nodeId = item.nodeId || 'New Node';
  const nodeTitle = item.title || 'Untitled';
  
  parts.push(`Node ID: ${nodeId}`);
  parts.push(`Title: ${nodeTitle}`);
  parts.push(`Day: ${item.day}`);
  
  if (item.fields && item.fields.length > 0) {
    parts.push(`Modified fields: ${item.fields.join(', ')}`);
  }
  
  return parts.join('\n');
}

/**
 * Create a simple diff section from before/after objects
 */
export function createObjectDiff(
  title: string,
  before: any,
  after: any
): DiffSection {
  const changes: DiffChange[] = [];

  // Get all unique keys
  const allKeys = new Set([
    ...Object.keys(before || {}),
    ...Object.keys(after || {}),
  ]);

  allKeys.forEach(key => {
    const oldValue = before?.[key];
    const newValue = after?.[key];

    if (oldValue === undefined && newValue !== undefined) {
      // Added
      changes.push({
        type: 'added',
        path: key,
        label: key,
        newValue,
      });
    } else if (oldValue !== undefined && newValue === undefined) {
      // Removed
      changes.push({
        type: 'removed',
        path: key,
        label: key,
        oldValue,
      });
    } else if (JSON.stringify(oldValue) !== JSON.stringify(newValue)) {
      // Modified
      changes.push({
        type: 'modified',
        path: key,
        label: key,
        oldValue,
        newValue,
      });
    } else {
      // Unchanged (only include if showUnchanged is true)
      changes.push({
        type: 'unchanged',
        path: key,
        label: key,
        oldValue,
        newValue,
      });
    }
  });

  return {
    title,
    changes,
  };
}

/**
 * Create diff sections from a ChangeSet
 */
export function createChangeSetDiff(changeSet: any): DiffSection[] {
  const sections: DiffSection[] = [];

  if (!changeSet || !changeSet.ops) {
    return sections;
  }

  // Group operations by type
  const opsByType = new Map<string, any[]>();
  
  changeSet.ops.forEach((op: any) => {
    const type = op.op || 'unknown';
    if (!opsByType.has(type)) {
      opsByType.set(type, []);
    }
    opsByType.get(type)!.push(op);
  });

  // Create sections for each operation type
  opsByType.forEach((ops, type) => {
    const changes: DiffChange[] = ops.map((op, index) => {
      const changeType = type === 'add' ? 'added' : type === 'remove' ? 'removed' : 'modified';
      
      return {
        type: changeType,
        path: op.path || `operation-${index}`,
        label: `${type.toUpperCase()}: ${op.path || 'unknown'}`,
        oldValue: op.oldValue,
        newValue: op.value || op.newValue,
      };
    });

    sections.push({
      title: `${type.toUpperCase()} Operations (${ops.length})`,
      changes,
    });
  });

  return sections;
}
