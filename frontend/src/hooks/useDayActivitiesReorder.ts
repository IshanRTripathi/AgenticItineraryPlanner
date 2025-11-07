/**
 * Hook for handling drag & drop reordering of activities within a day
 * Uses @dnd-kit for drag & drop and backend API for persistence
 */

import { useState, useCallback, useEffect } from 'react';
import { DragEndEvent } from '@dnd-kit/core';
import { arrayMove } from '@dnd-kit/sortable';
import { itineraryApi } from '@/services/api';
import { useToast } from '@/components/ui/use-toast';

interface Activity {
  id: string;
  title?: string;
  name?: string;
  [key: string]: any;
}

interface UseDayActivitiesReorderProps {
  itineraryId: string;
  dayNumber: number;
  initialActivities: Activity[];
  onReorderSuccess?: (newActivities: Activity[]) => void;
  onRefetchNeeded?: () => Promise<void> | void; // Callback to trigger refetch from parent
  autoSave?: boolean; // If false, requires manual save via saveReorder()
}

export function useDayActivitiesReorder({
  itineraryId,
  dayNumber,
  initialActivities,
  onReorderSuccess,
  onRefetchNeeded,
  autoSave = false, // Default to manual save mode
}: UseDayActivitiesReorderProps) {
  const [activities, setActivities] = useState<Activity[]>(initialActivities);
  const [isReordering, setIsReordering] = useState(false);
  const [hasUnsavedChanges, setHasUnsavedChanges] = useState(false);
  const { toast } = useToast();

  // Sync with initialActivities when they change (but only if no unsaved changes)
  // This handles external updates (e.g., from WebSocket, other users, etc.)
  useEffect(() => {
    if (!hasUnsavedChanges) {
      console.log('[useDayActivitiesReorder] Syncing activities from props:', {
        count: initialActivities.length,
        ids: initialActivities.map(a => a.id),
      });
      setActivities(initialActivities);
    } else {
      console.log('[useDayActivitiesReorder] Skipping sync - has unsaved changes');
    }
  }, [initialActivities, hasUnsavedChanges]);

  const handleDragEnd = useCallback(
    async (event: DragEndEvent) => {
      const { active, over } = event;

      if (!over || active.id === over.id) {
        return;
      }

      const oldIndex = activities.findIndex((a) => a.id === active.id);
      const newIndex = activities.findIndex((a) => a.id === over.id);

      if (oldIndex === -1 || newIndex === -1) {
        console.error('[useDayActivitiesReorder] Invalid drag indices', {
          oldIndex,
          newIndex,
          activeId: active.id,
          overId: over.id,
        });
        return;
      }

      // Prevent concurrent reorder operations
      if (isReordering) {
        console.warn('[useDayActivitiesReorder] Reorder already in progress, ignoring');
        return;
      }

      // Optimistic update
      const newActivities = arrayMove(activities, oldIndex, newIndex);
      setActivities(newActivities);
      setHasUnsavedChanges(true);

      // If autoSave is enabled, persist immediately
      if (!autoSave) {
        console.log('[useDayActivitiesReorder] Manual save mode - changes not persisted yet');
        return;
      }

      // Auto-save: Persist to backend
      setIsReordering(true);
      try {
        // Create ChangeSet with a single reorder operation
        // Send the complete ordered list of node IDs
        const changeSet = {
          scope: 'day',
          day: dayNumber,
          ops: [
            {
              op: 'reorder',
              nodeIds: newActivities.map(a => a.id),
            }
          ],
          preferences: {
            userFirst: true,
            autoApply: true,
            respectLocks: true,
          },
        };

        console.log('[useDayActivitiesReorder] Applying reorder', {
          itineraryId,
          dayNumber,
          changeSet,
        });

        // Apply changes via backend API
        await itineraryApi.post(`/itineraries/${itineraryId}:apply`, {
          changeSet,
        });

        console.log('[useDayActivitiesReorder] Reorder successful');

        // Notify parent component
        if (onReorderSuccess) {
          onReorderSuccess(newActivities);
        }

        // Trigger refetch to get the latest data from backend
        if (onRefetchNeeded) {
          console.log('[useDayActivitiesReorder] Triggering refetch...');
          await onRefetchNeeded();
          console.log('[useDayActivitiesReorder] Refetch completed');
        }

        // Show success toast after refetch completes
        setHasUnsavedChanges(false);
        
        toast({
          title: 'Activities reordered',
          description: 'Your changes have been saved',
        });
      } catch (error) {
        console.error('[useDayActivitiesReorder] Failed to reorder', error);

        // Rollback optimistic update
        setActivities(initialActivities);
        setHasUnsavedChanges(false);

        toast({
          title: 'Failed to reorder',
          description: 'Could not save your changes. Please try again.',
          variant: 'destructive',
        });
      } finally {
        setIsReordering(false);
      }
    },
    [activities, itineraryId, dayNumber, onReorderSuccess, onRefetchNeeded, toast, isReordering, autoSave, initialActivities]
  );

  // Manual save function for when autoSave is false
  const saveReorder = useCallback(async () => {
    if (!hasUnsavedChanges) {
      console.log('[useDayActivitiesReorder] No changes to save');
      return;
    }

    setIsReordering(true);
    try {
      const changeSet = {
        scope: 'day',
        day: dayNumber,
        ops: [
          {
            op: 'reorder',
            nodeIds: activities.map(a => a.id),
          }
        ],
        preferences: {
          userFirst: true,
          autoApply: true,
          respectLocks: true,
        },
      };

      console.log('[useDayActivitiesReorder] Saving reorder', {
        itineraryId,
        dayNumber,
        changeSet,
      });

      await itineraryApi.post(`/itineraries/${itineraryId}:apply`, {
        changeSet,
      });

      console.log('[useDayActivitiesReorder] Save successful');
      
      if (onReorderSuccess) {
        onReorderSuccess(activities);
      }

      // Refetch to get the latest data from backend
      if (onRefetchNeeded) {
        console.log('[useDayActivitiesReorder] Triggering refetch...');
        await onRefetchNeeded();
        console.log('[useDayActivitiesReorder] Refetch completed');
      }
      
      // Clear unsaved changes AFTER refetch completes
      setHasUnsavedChanges(false);

      toast({
        title: 'Changes saved',
        description: 'Activity order has been updated',
      });
    } catch (error) {
      console.error('[useDayActivitiesReorder] Failed to save', error);

      toast({
        title: 'Failed to save',
        description: 'Could not save your changes. Please try again.',
        variant: 'destructive',
      });
    } finally {
      setIsReordering(false);
    }
  }, [hasUnsavedChanges, activities, itineraryId, dayNumber, onReorderSuccess, onRefetchNeeded, toast]);

  // Discard changes and reset to initial state
  const discardChanges = useCallback(() => {
    setActivities(initialActivities);
    setHasUnsavedChanges(false);
    console.log('[useDayActivitiesReorder] Changes discarded');
  }, [initialActivities]);

  return {
    activities,
    isReordering,
    hasUnsavedChanges,
    handleDragEnd,
    saveReorder,
    discardChanges,
  };
}

