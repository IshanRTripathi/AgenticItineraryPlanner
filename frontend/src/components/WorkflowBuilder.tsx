import React, { useCallback } from 'react';
import ReactFlow, {
  addEdge,
  useNodesState,
  useEdgesState,
  Connection,
  Edge,
  ReactFlowProvider,
  Controls,
  Background,
  MiniMap,
} from 'reactflow';
import 'reactflow/dist/style.css';
import { useMapState } from '../hooks/useMapState';
import { validateWorkflowNode } from './workflow/WorkflowUtils';
import { Button } from './ui/button';
import { Badge } from './ui/badge';
import { Tabs, TabsContent, TabsList, TabsTrigger } from './ui/tabs';
import {
  ArrowLeft,
  Undo,
  Redo,
  RotateCcw,
  Save,
  MapPin,
  Utensils,
  Car,
  Bed,
  Clock,
  Info,
  Edit3,
} from 'lucide-react';
import { WorkflowNode } from './workflow/WorkflowNode';
import { NodeInspectorModal } from './workflow/NodeInspectorModal';
import { userChangeTracker } from '../services/userChangeTracker';

// Extracted modules
import { WorkflowBuilderProps, WorkflowNodeData } from './workflow-builder/WorkflowBuilderTypes';
import { useWorkflowBuilderState } from './workflow-builder/WorkflowBuilderState';
import {
  useUserChangesSubscription,
  useTripDataSync,
  useSavedPositionsSync,
  useActiveDaySync,
  useViewportZoomSync,
  useWorkflowDaysSync,
  useMapViewModeSync,
  useNodePositionTracking,
  useClearBadPositions,
  useResetToGridPattern,
} from './workflow-builder/WorkflowBuilderHooks';
import {
  calculateOptimalZoom,
  createNodeHandlers,
  createWorkflowActionHandlers,
} from './workflow-builder/WorkflowBuilderHelpers';

// Register custom node types (moved outside component to prevent React Flow warning)
const nodeTypes = {
  workflow: WorkflowNode,
};

const WorkflowBuilderContent: React.FC<WorkflowBuilderProps> = ({
  tripData,
  onSave,
  onCancel,
  embedded = false,
}) => {
  // Map state for highlighting associated nodes
  const mapState = useMapState();
  const {
    viewMode,
    selectedNodeId,
    addHighlightedMarker,
    clearHighlightedMarkers,
    setSelectedNode: setMapSelectedNode,
  } = mapState;

  // State management
  const {
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
    savePositions,
    userChanges,
    setUserChanges,
  } = useWorkflowBuilderState(tripData);

  const currentDay = workflowDays[activeDay];
  const [nodes, setNodes, onNodesChangeBase] = useNodesState(currentDay?.nodes || []);
  const [edges, setEdges, onEdgesChange] = useEdgesState(currentDay?.edges || []);

  // Manual sync function to update workflowDays with current nodes/edges
  // Called only when switching days or saving
  const syncCurrentDayToWorkflowDays = useCallback(() => {
    if (currentDay) {
      setWorkflowDays((prev) => {
        const updated = [...prev];
        updated[activeDay] = {
          ...currentDay,
          nodes,
          edges,
        };
        return updated;
      });
    }
  }, [currentDay, activeDay, nodes, edges, setWorkflowDays]);

  // Custom hooks for side effects
  // Note: Hook order matters - some hooks depend on state set by previous hooks
  useUserChangesSubscription(setUserChanges);
  useTripDataSync(tripData, savedPositions, workflowDays, setWorkflowDays, setActiveDay);
  useSavedPositionsSync(workflowDays, tripData, savedPositions, setWorkflowDays); // Disabled - causes circular dependency
  useActiveDaySync(currentDay, activeDay, setNodes, setEdges);
  useViewportZoomSync(nodes.length);
  useWorkflowDaysSync(currentDay, activeDay, nodes, edges, workflowDays, setWorkflowDays); // Disabled - causes circular dependency
  useMapViewModeSync(viewMode, selectedNodeId, currentDay, setSelectedNode);

  // Position tracking
  const onNodesChange = useNodePositionTracking(onNodesChangeBase, savedPositions, savePositions);
  const clearBadPositions = useClearBadPositions(savedPositions, savePositions);
  const resetToGridPattern = useResetToGridPattern(
    currentDay,
    savedPositions,
    setNodes,
    savePositions,
    clearBadPositions
  );

  // Node validation
  const validateNodes = useCallback(
    (dayNodes: typeof nodes) => {
      return dayNodes.map((node) => {
        if (!node.id) {
          
          return {
            ...node,
            data: {
              ...node.data,
              validation: { status: 'error', message: 'Node missing ID' },
              userModified: false,
            },
          };
        }

        const validation = validateWorkflowNode(node, dayNodes);
        const latestChange = userChangeTracker.getLatestChange(node.id);

        return {
          ...node,
          data: {
            ...node.data,
            validation,
            userModified: userChanges.has(node.id),
            changeType: latestChange?.changeType,
            changeTimestamp: latestChange?.timestamp,
          },
        };
      });
    },
    [userChanges]
  );

  // Event handlers
  const onConnect = useCallback(
    (params: Connection | Edge) => {
      setEdges((eds) => addEdge(params, eds));
    },
    [setEdges]
  );

  const onNodeClick = useCallback(
    (event: React.MouseEvent, node: typeof nodes[0]) => {
      
      
      

      setSelectedNode(node);
      setIsNodeInspectorOpen(true);

      // Sync with map - highlight the corresponding marker
      setMapSelectedNode(node.id);
      addHighlightedMarker(node.id);

      
    },
    [setSelectedNode, setIsNodeInspectorOpen, setMapSelectedNode, addHighlightedMarker]
  );

  const onPaneClick = useCallback(() => {
    
    setSelectedNode(null);
    setIsNodeInspectorOpen(false);

    // Clear map selection
    setMapSelectedNode(null);
    clearHighlightedMarkers();

    
  }, [setSelectedNode, setIsNodeInspectorOpen, setMapSelectedNode, clearHighlightedMarkers]);

  // Node handlers
  const { addNewNode, deleteNode, updateSelectedNode } = createNodeHandlers(
    setNodes,
    setEdges,
    selectedNode,
    setSelectedNode,
    setIsNodeInspectorOpen,
    activeDay,
    nodes,
    userChangeTracker
  );

  // Workflow action handlers
  const workflowHandlers = createWorkflowActionHandlers(
    workflowDays,
    setWorkflowDays,
    undoStack,
    setUndoStack,
    redoStack,
    setRedoStack,
    setActiveDay,
    setSelectedNode,
    tripData,
    onSave
  );

  // Wrap applyToItinerary to sync before saving
  const applyToItinerary = useCallback(() => {
    syncCurrentDayToWorkflowDays();
    // Use setTimeout to ensure state is updated before calling the handler
    setTimeout(() => {
      workflowHandlers.applyToItinerary();
    }, 0);
  }, [syncCurrentDayToWorkflowDays, workflowHandlers]);

  const { undo, redo, resetDemo } = workflowHandlers;

  const getNodeIcon = (type: WorkflowNodeData['type']) => {
    const iconMap = {
      Attraction: MapPin,
      Meal: Utensils,
      Transit: Car,
      Hotel: Bed,
      FreeTime: Clock,
      Decision: Info,
    };
    return iconMap[type];
  };

  return (
    <div className={embedded ? 'h-full flex flex-col bg-gray-50' : 'h-screen flex flex-col bg-gray-50'}>
      <div className="flex-1 flex flex-col">
        {/* Main Canvas Area */}
        <div className="flex-1 flex flex-col">
          {/* Section 1: Main Navigation - Only show when not embedded */}
          {!embedded && (
            <div className="bg-white border-b px-4 md:px-6 py-4 mb-2">
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
                <div className="flex items-center gap-4">
                  <Button variant="ghost" onClick={onCancel} className="min-h-[44px]">
                    <ArrowLeft className="h-4 w-4 mr-2" />
                    Back
                  </Button>
                  <div>
                    <h1 className="text-xl md:text-2xl">Workflow Builder</h1>
                    <p className="text-gray-600 text-sm md:text-base">{tripData.destination}</p>
                  </div>
                </div>
                <Button onClick={applyToItinerary} className="min-h-[44px]">
                  <Save className="h-4 w-4 mr-2" />
                  <span className="hidden sm:inline">Apply to Itinerary</span>
                  <span className="sm:hidden">Apply</span>
                </Button>
              </div>
            </div>
          )}

          {/* Section 2: Day Navigation */}
          <div className="bg-white border-b px-4 py-4 mb-2">
            <div className="flex items-center justify-center overflow-x-auto">
              <Tabs
                value={activeDay.toString()}
                onValueChange={(value) => {
                  // Sync current day before switching
                  syncCurrentDayToWorkflowDays();
                  setActiveDay(parseInt(value));
                }}
              >
                <TabsList className="h-10 gap-2 min-w-max">
                  {workflowDays.map((day, index) => (
                    <TabsTrigger
                      key={index}
                      value={index.toString()}
                      className="text-sm px-3 md:px-4 py-2 mx-1 min-h-[44px]"
                    >
                      Day {day.day}
                    </TabsTrigger>
                  ))}
                </TabsList>
              </Tabs>
            </div>
          </div>

          {/* Section 3: Add Node Toolbar */}
          <div className="bg-white border-b px-4 py-4 mb-2">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
              <div className="flex flex-col sm:flex-row sm:items-center justify-center gap-2">
                <span className="text-sm font-medium mr-4 hidden sm:inline">Add Node:</span>
                <div className="flex flex-wrap justify-center gap-2">
                  {(['Attraction', 'Meal', 'Transit', 'Hotel', 'FreeTime'] as const).map((type) => {
                    const Icon = getNodeIcon(type);
                    return (
                      <Button
                        key={type}
                        variant="outline"
                        size="sm"
                        onClick={() => addNewNode(type)}
                        className="min-h-[44px]"
                      >
                        <Icon className="h-4 w-4 mr-2" />
                        <span className="hidden sm:inline">{type}</span>
                      </Button>
                    );
                  })}
                </div>
              </div>

              {/* User Changes Summary */}
              {userChanges.size > 0 && (
                <div className="flex items-center gap-2">
                  <Badge variant="outline" className="bg-blue-50 text-blue-700 border-blue-200">
                    <Edit3 className="h-3 w-3 mr-1" />
                    {userChanges.size} modified
                  </Badge>
                  <Button
                    variant="ghost"
                    size="sm"
                    onClick={() => userChangeTracker.clearAllChanges()}
                    className="text-xs text-gray-500 hover:text-gray-700"
                    title="Clear all change indicators"
                  >
                    Clear
                  </Button>
                </div>
              )}
            </div>
          </div>

          {/* Canvas Container */}
          <div className="flex-1 relative">
            {/* React Flow Canvas */}
            <div className="w-full h-full">
              <ReactFlow
                nodes={validateNodes(nodes)}
                edges={edges}
                onNodesChange={onNodesChange}
                onEdgesChange={onEdgesChange}
                onConnect={onConnect}
                onNodeClick={onNodeClick}
                onPaneClick={onPaneClick}
                nodeTypes={nodeTypes}
                fitView
                fitViewOptions={{ padding: 0.3, minZoom: 0.1, maxZoom: 1.5 }}
                defaultViewport={{ x: 0, y: 0, zoom: calculateOptimalZoom(nodes.length) }}
                snapToGrid
                snapGrid={[20, 20]}
              >
                <Controls />
                <MiniMap />
                <Background variant="dots" gap={20} size={1} />
              </ReactFlow>
            </div>

            {/* Floating Action Buttons */}
            <div className="absolute top-4 right-4 flex flex-col gap-2 z-10">
              <Button
                variant="outline"
                size="sm"
                onClick={undo}
                disabled={undoStack.length === 0}
                className="w-10 h-10 p-0 shadow-lg"
                title="Undo"
              >
                <Undo className="h-4 w-4" />
              </Button>
              <Button
                variant="outline"
                size="sm"
                onClick={redo}
                disabled={redoStack.length === 0}
                className="w-10 h-10 p-0 shadow-lg"
                title="Redo"
              >
                <Redo className="h-4 w-4" />
              </Button>
              <Button
                variant="outline"
                size="sm"
                onClick={resetToGridPattern}
                className="w-10 h-10 p-0 shadow-lg"
                title="Reset to Grid"
              >
                <RotateCcw className="h-4 w-4" />
              </Button>
            </div>
          </div>
        </div>

        {/* Node Inspector Modal */}
        <NodeInspectorModal
          isOpen={isNodeInspectorOpen}
          onClose={() => setIsNodeInspectorOpen(false)}
          selectedNode={selectedNode}
          onUpdateNode={updateSelectedNode}
          onDeleteNode={deleteNode}
        />
      </div>
    </div>
  );
};

export function WorkflowBuilder(props: WorkflowBuilderProps) {
  return (
    <ReactFlowProvider>
      <WorkflowBuilderContent {...props} />
    </ReactFlowProvider>
  );
}

// Re-export types for backward compatibility
export type { WorkflowNodeData, WorkflowDay } from './workflow-builder/WorkflowBuilderTypes';

