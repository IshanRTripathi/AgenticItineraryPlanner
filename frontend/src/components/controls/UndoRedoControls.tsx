/**
 * Undo/Redo Controls Component
 * Provides undo/redo functionality with keyboard shortcuts
 */

import React, { useEffect, useCallback, useState } from 'react';
import { Button } from '../ui/button';
import { Undo2, Redo2, History, Info } from 'lucide-react';
import { apiClient } from '../../services/apiClient';
import { useUnifiedItinerary } from '../../contexts/UnifiedItineraryContext';
import { Tooltip, TooltipContent, TooltipProvider, TooltipTrigger } from '../ui/tooltip';

interface UndoRedoControlsProps {
  className?: string;
  showVersionInfo?: boolean;
  showStackDepth?: boolean;
  onUndoSuccess?: () => void;
  onRedoSuccess?: () => void;
  onError?: (error: Error) => void;
}

export function UndoRedoControls({
  className = '',
  showVersionInfo = true,
  showStackDepth = false,
  onUndoSuccess,
  onRedoSuccess,
  onError,
}: UndoRedoControlsProps) {
  const { state } = useUnifiedItinerary();
  const [isUndoing, setIsUndoing] = useState(false);
  const [isRedoing, setIsRedoing] = useState(false);
  const [canUndo, setCanUndo] = useState(false);
  const [canRedo, setCanRedo] = useState(false);
  const [currentVersion, setCurrentVersion] = useState<number>(1);
  const [undoStackDepth, setUndoStackDepth] = useState<number>(0);
  const [redoStackDepth, setRedoStackDepth] = useState<number>(0);

  // Fetch revision history to determine undo/redo availability
  const fetchRevisionInfo = useCallback(async () => {
    if (!state.itinerary?.id) return;

    try {
      const response = await apiClient.getRevisions(state.itinerary.id);
      
      if (response.revisions && response.revisions.length > 0) {
        const currentRev = response.revisions.find((r: any) => r.isCurrent);
        const currentIndex = currentRev ? response.revisions.indexOf(currentRev) : response.revisions.length - 1;
        
        setCurrentVersion(currentRev?.version || response.revisions.length);
        setCanUndo(currentIndex > 0);
        setCanRedo(currentIndex < response.revisions.length - 1);
        setUndoStackDepth(currentIndex);
        setRedoStackDepth(response.revisions.length - 1 - currentIndex);
      } else {
        setCurrentVersion(1);
        setCanUndo(false);
        setCanRedo(false);
        setUndoStackDepth(0);
        setRedoStackDepth(0);
      }
    } catch (error) {
      
    }
  }, [state.itinerary?.id]);

  // Fetch revision info on mount and when itinerary changes
  useEffect(() => {
    fetchRevisionInfo();
  }, [fetchRevisionInfo, state.itinerary?.id]);

  // Handle undo
  const handleUndo = useCallback(async () => {
    if (!state.itinerary?.id || !canUndo || isUndoing) return;

    setIsUndoing(true);
    try {
      await apiClient.undoChanges(state.itinerary.id, {});
      await fetchRevisionInfo();
      onUndoSuccess?.();
      
      // Reload to show changes
      setTimeout(() => window.location.reload(), 500);
    } catch (error) {
      
      onError?.(error as Error);
    } finally {
      setIsUndoing(false);
    }
  }, [state.itinerary?.id, canUndo, isUndoing, fetchRevisionInfo, onUndoSuccess, onError]);

  // Handle redo
  const handleRedo = useCallback(async () => {
    if (!state.itinerary?.id || !canRedo || isRedoing) return;

    setIsRedoing(true);
    try {
      await apiClient.redoChanges(state.itinerary.id);
      await fetchRevisionInfo();
      onRedoSuccess?.();
      
      // Reload to show changes
      setTimeout(() => window.location.reload(), 500);
    } catch (error) {
      
      onError?.(error as Error);
    } finally {
      setIsRedoing(false);
    }
  }, [state.itinerary?.id, canRedo, isRedoing, fetchRevisionInfo, onRedoSuccess, onError]);

  // Keyboard shortcuts
  useEffect(() => {
    const handleKeyDown = (event: KeyboardEvent) => {
      // Ctrl+Z or Cmd+Z for undo
      if ((event.ctrlKey || event.metaKey) && event.key === 'z' && !event.shiftKey) {
        event.preventDefault();
        handleUndo();
      }
      
      // Ctrl+Y or Cmd+Shift+Z for redo
      if (
        ((event.ctrlKey || event.metaKey) && event.key === 'y') ||
        ((event.ctrlKey || event.metaKey) && event.shiftKey && event.key === 'z')
      ) {
        event.preventDefault();
        handleRedo();
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [handleUndo, handleRedo]);

  if (!state.itinerary?.id) {
    return null;
  }

  return (
    <div className={`flex items-center gap-2 ${className}`}>
      <TooltipProvider>
        {/* Undo Button */}
        <Tooltip>
          <TooltipTrigger asChild>
            <Button
              variant="outline"
              size="sm"
              onClick={handleUndo}
              disabled={!canUndo || isUndoing}
              className="flex items-center gap-1"
            >
              <Undo2 className="w-4 h-4" />
              {isUndoing ? 'Undoing...' : 'Undo'}
            </Button>
          </TooltipTrigger>
          <TooltipContent>
            <p>Undo last change (Ctrl+Z)</p>
            {showStackDepth && undoStackDepth > 0 && (
              <p className="text-xs text-gray-400">{undoStackDepth} changes available</p>
            )}
          </TooltipContent>
        </Tooltip>

        {/* Redo Button */}
        <Tooltip>
          <TooltipTrigger asChild>
            <Button
              variant="outline"
              size="sm"
              onClick={handleRedo}
              disabled={!canRedo || isRedoing}
              className="flex items-center gap-1"
            >
              <Redo2 className="w-4 h-4" />
              {isRedoing ? 'Redoing...' : 'Redo'}
            </Button>
          </TooltipTrigger>
          <TooltipContent>
            <p>Redo last undone change (Ctrl+Y)</p>
            {showStackDepth && redoStackDepth > 0 && (
              <p className="text-xs text-gray-400">{redoStackDepth} changes available</p>
            )}
          </TooltipContent>
        </Tooltip>

        {/* Version Info */}
        {showVersionInfo && (
          <Tooltip>
            <TooltipTrigger asChild>
              <div className="flex items-center gap-1 px-2 py-1 text-sm text-gray-600 bg-gray-100 rounded">
                <History className="w-3 h-3" />
                <span>v{currentVersion}</span>
              </div>
            </TooltipTrigger>
            <TooltipContent>
              <p>Current version: {currentVersion}</p>
              {showStackDepth && (
                <>
                  <p className="text-xs text-gray-400">Undo: {undoStackDepth} available</p>
                  <p className="text-xs text-gray-400">Redo: {redoStackDepth} available</p>
                </>
              )}
            </TooltipContent>
          </Tooltip>
        )}

        {/* Info Icon */}
        {!showVersionInfo && (
          <Tooltip>
            <TooltipTrigger asChild>
              <Button variant="ghost" size="sm" className="p-1">
                <Info className="w-4 h-4 text-gray-400" />
              </Button>
            </TooltipTrigger>
            <TooltipContent>
              <p>Version: {currentVersion}</p>
              <p className="text-xs text-gray-400 mt-1">
                Use Ctrl+Z to undo, Ctrl+Y to redo
              </p>
            </TooltipContent>
          </Tooltip>
        )}
      </TooltipProvider>
    </div>
  );
}

