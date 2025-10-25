import React, { useEffect, useCallback } from 'react';
import { Node, Edge, useReactFlow } from 'reactflow';
import { WorkflowDay, WorkflowNodeData } from './WorkflowBuilderTypes';
import { TripData } from '../../types/TripData';
import { createWorkflowDaysFromTripData, calculateOptimalZoom, getGridPosition } from './WorkflowBuilderHelpers';
import { userChangeTracker } from '../../services/userChangeTracker';
import { logger } from '../../utils/logger';

/**
 * Hook to subscribe to user changes
 */
export const useUserChangesSubscription = (
  setUserChanges: React.Dispatch<React.SetStateAction<Set<string>>>
) => {
  useEffect(() => {
    const unsubscribe = userChangeTracker.subscribe((changes) => {
      const modifiedNodeIds = new Set(changes.map((change) => change.nodeId));
      setUserChanges(modifiedNodeIds);
    });

    // Load existing changes
    const modifiedNodeIds = new Set(userChangeTracker.getModifiedNodeIds());
    setUserChanges(modifiedNodeIds);

    return unsubscribe;
  }, [setUserChanges]);
};

/**
 * Hook to update workflow days when trip data changes (only on initial load)
 * Uses a ref to track if initial load has happened
 */
export const useTripDataSync = (
  tripData: TripData,
  savedPositions: Record<string, { x: number; y: number }>,
  workflowDays: WorkflowDay[],
  setWorkflowDays: React.Dispatch<React.SetStateAction<WorkflowDay[]>>,
  setActiveDay: React.Dispatch<React.SetStateAction<number>>
) => {
  const initializedRef = React.useRef(false);
  
  useEffect(() => {
    // Only run once on initial load
    if (!initializedRef.current && workflowDays.length === 0) {
      const newWorkflowDays = createWorkflowDaysFromTripData(tripData, savedPositions);
      if (newWorkflowDays.length > 0) {
        setWorkflowDays(newWorkflowDays);
        setActiveDay(0);
        initializedRef.current = true;
      }
    }
  }, [tripData, savedPositions, workflowDays.length, setWorkflowDays, setActiveDay]);
};

/**
 * Hook to update workflow days with saved positions when savedPositions changes
 * DISABLED: This was causing infinite re-render loops
 * Position updates are now handled by useNodePositionTracking
 */
export const useSavedPositionsSync = (
  workflowDays: WorkflowDay[],
  tripData: TripData,
  savedPositions: Record<string, { x: number; y: number }>,
  setWorkflowDays: React.Dispatch<React.SetStateAction<WorkflowDay[]>>
) => {
  // DISABLED: Causes circular dependency
  // Positions are updated through node changes, not workflow days
};

/**
 * Hook to update React Flow when active day changes
 */
export const useActiveDaySync = (
  currentDay: WorkflowDay | undefined,
  activeDay: number,
  setNodes: React.Dispatch<React.SetStateAction<Node<WorkflowNodeData>[]>>,
  setEdges: React.Dispatch<React.SetStateAction<Edge[]>>
) => {
  useEffect(() => {
    if (currentDay) {
      logger.debug(`Switching to day ${activeDay + 1}`, {
        component: 'WorkflowBuilderHooks',
        dayNumber: currentDay.day,
        nodeCount: currentDay.nodes.length,
        nodePositions: currentDay.nodes.map((n) => ({
          id: n.id,
          position: n.position,
          title: n.data.title,
        })),
      });
      setNodes(currentDay.nodes);
      setEdges(currentDay.edges);
    }
  }, [activeDay, currentDay, setNodes, setEdges]);
};

/**
 * Hook to update viewport zoom when nodes change
 */
export const useViewportZoomSync = (nodesLength: number) => {
  const { getViewport, setViewport } = useReactFlow();

  useEffect(() => {
    if (nodesLength > 0) {
      const optimalZoom = calculateOptimalZoom(nodesLength);
      const currentViewport = getViewport();
      setViewport({
        x: currentViewport.x,
        y: currentViewport.y,
        zoom: optimalZoom,
      });
    }
  }, [nodesLength, setViewport, getViewport]);
};

/**
 * Hook to update workflowDays when nodes/edges change
 * DISABLED: This was causing infinite re-render loops with useActiveDaySync
 * The circular dependency: useActiveDaySync sets nodes → useWorkflowDaysSync updates workflowDays → currentDay changes → useActiveDaySync runs again
 * Solution: workflowDays is now only updated through explicit actions (undo/redo/reset), not from node changes
 */
export const useWorkflowDaysSync = (
  currentDay: WorkflowDay | undefined,
  activeDay: number,
  nodes: Node<WorkflowNodeData>[],
  edges: Edge[],
  workflowDays: WorkflowDay[],
  setWorkflowDays: React.Dispatch<React.SetStateAction<WorkflowDay[]>>
) => {
  // DISABLED: Causes circular dependency with useActiveDaySync
  // workflowDays should be the source of truth, not updated from node changes
};

/**
 * Hook to sync with map context when view mode changes to workflow
 */
export const useMapViewModeSync = (
  viewMode: string,
  selectedNodeId: string | null,
  currentDay: WorkflowDay | undefined,
  setSelectedNode: React.Dispatch<React.SetStateAction<Node<WorkflowNodeData> | null>>
) => {
  useEffect(() => {
    if (viewMode === 'workflow' && selectedNodeId) {
      logger.debug('Workflow view mode sync', {
        component: 'WorkflowBuilderHooks',
        selectedNodeId
      });

      // Find the node in the current day's nodes
      const node = currentDay?.nodes.find((n) => n.id === selectedNodeId);
      if (node) {
        logger.debug('Found node in workflow, selecting it', {
          component: 'WorkflowBuilderHooks',
          nodeId: node.id
        });
        setSelectedNode(node);
      } else {
        logger.debug('Node not found in current day, clearing selection', {
          component: 'WorkflowBuilderHooks'
        });
        setSelectedNode(null);
      }
    }
  }, [viewMode, selectedNodeId, currentDay, setSelectedNode]);
};

/**
 * Hook to create custom onNodesChange that saves positions when nodes are moved
 */
export const useNodePositionTracking = (
  onNodesChangeBase: (changes: any[]) => void,
  savedPositions: Record<string, { x: number; y: number }>,
  savePositions: (positions: Record<string, { x: number; y: number }>) => void
) => {
  const onNodesChange = useCallback(
    (changes: any[]) => {
      onNodesChangeBase(changes);

      // Check if any changes are position changes
      const positionChanges = changes.filter((change) => change.type === 'position' && change.position);
      if (positionChanges.length > 0) {
        const newPositions = { ...savedPositions };
        positionChanges.forEach((change) => {
          if (change.position) {
            newPositions[change.id] = change.position;
          }
        });
        savePositions(newPositions);
      }
    },
    [onNodesChangeBase, savedPositions, savePositions]
  );

  return onNodesChange;
};

/**
 * Hook to clear bad saved positions (positions at origin or top-left)
 */
export const useClearBadPositions = (
  savedPositions: Record<string, { x: number; y: number }>,
  savePositions: (positions: Record<string, { x: number; y: number }>) => void
) => {
  const clearBadPositions = useCallback(() => {
    const cleanedPositions = { ...savedPositions };
    Object.keys(cleanedPositions).forEach((nodeId) => {
      const pos = cleanedPositions[nodeId];
      if (pos.x === 0 && pos.y === 0) {
        delete cleanedPositions[nodeId];
      }
    });
    savePositions(cleanedPositions);
  }, [savedPositions, savePositions]);

  return clearBadPositions;
};

/**
 * Hook to reset positions to grid pattern
 */
export const useResetToGridPattern = (
  currentDay: WorkflowDay | undefined,
  savedPositions: Record<string, { x: number; y: number }>,
  setNodes: React.Dispatch<React.SetStateAction<Node<WorkflowNodeData>[]>>,
  savePositions: (positions: Record<string, { x: number; y: number }>) => void,
  clearBadPositions: () => void
) => {
  const resetToGridPattern = useCallback(() => {
    if (!currentDay) return;

    // Clear any bad positions first
    clearBadPositions();

    const newPositions = { ...savedPositions };
    currentDay.nodes.forEach((node, index) => {
      newPositions[node.id] = getGridPosition(index, node.id, {}, currentDay.nodes.length);
    });

    // Update nodes with new positions
    const updatedNodes = currentDay.nodes.map((node, index) => ({
      ...node,
      position: newPositions[node.id],
    }));

    setNodes(updatedNodes);
    savePositions(newPositions);
  }, [currentDay, savedPositions, setNodes, savePositions, clearBadPositions]);

  return resetToGridPattern;
};
