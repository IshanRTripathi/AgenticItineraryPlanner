import { useState, useCallback } from 'react';
import { Node } from 'reactflow';
import { WorkflowDay, WorkflowNodeData } from './WorkflowBuilderTypes';
import { TripData } from '../../types/TripData';
import { createWorkflowDaysFromTripData, createSeedData } from './WorkflowBuilderHelpers';

/**
 * Custom hook for managing WorkflowBuilder state
 */
export const useWorkflowBuilderState = (tripData: TripData) => {
  // Load saved positions from localStorage
  const loadSavedPositions = useCallback(() => {
    try {
      const saved = localStorage.getItem(`workflow-positions-${tripData.id}`);
      return saved ? JSON.parse(saved) : {};
    } catch {
      return {};
    }
  }, [tripData.id]);

  const [workflowDays, setWorkflowDays] = useState<WorkflowDay[]>(() => {
    const savedPositions = loadSavedPositions();
    const days = createWorkflowDaysFromTripData(tripData, savedPositions);
    return days.length > 0 ? days : createSeedData();
  });

  const [activeDay, setActiveDay] = useState(0);
  const [selectedNode, setSelectedNode] = useState<Node<WorkflowNodeData> | null>(null);
  const [isNodeInspectorOpen, setIsNodeInspectorOpen] = useState(false);
  const [undoStack, setUndoStack] = useState<WorkflowDay[][]>([]);
  const [redoStack, setRedoStack] = useState<WorkflowDay[][]>([]);
  const [savedPositions, setSavedPositions] = useState<Record<string, { x: number; y: number }>>(
    loadSavedPositions
  );
  const [userChanges, setUserChanges] = useState<Set<string>>(new Set());

  // Save positions to localStorage
  const savePositions = useCallback(
    (positions: Record<string, { x: number; y: number }>) => {
      try {
        console.log(`💾 SAVING POSITIONS:`, {
          tripId: tripData.id,
          positionCount: Object.keys(positions).length,
          positions: Object.entries(positions).map(([nodeId, pos]) => ({
            nodeId,
            position: pos,
          })),
        });
        localStorage.setItem(`workflow-positions-${tripData.id}`, JSON.stringify(positions));
        setSavedPositions(positions);
      } catch (error) {
        console.error('Failed to save positions:', error);
      }
    },
    [tripData.id]
  );

  return {
    workflowDays,
    setWorkflowDays,
    activeDay,
    setActiveDay,
    selectedNode,
    setSelectedNode,
    isNodeInspectorOpen,
    setIsNodeInspectorOpen,
    undoStack,
    setUndoStack,
    redoStack,
    setRedoStack,
    savedPositions,
    setSavedPositions,
    savePositions,
    userChanges,
    setUserChanges,
  };
};
