import React from 'react';
import { Node, Edge } from 'reactflow';
import { WorkflowNodeData, WorkflowDay } from './WorkflowBuilderTypes';
import { TripData } from '../../types/TripData';
import { createNewNode } from '../workflow/WorkflowUtils';

/**
 * Calculate optimal zoom based on number of nodes
 */
export const calculateOptimalZoom = (nodeCount: number): number => {
  if (nodeCount <= 3) return 0.8;
  if (nodeCount <= 6) return 0.6;
  if (nodeCount <= 9) return 0.5;
  if (nodeCount <= 12) return 0.4;
  return 0.3; // For more than 12 nodes
};

/**
 * Calculate vertical separation based on node count to avoid overlap
 */
export const calculateVerticalSeparation = (nodeCount: number): number => {
  const baseSeparation = 250; // Base separation
  const additionalSeparation = Math.max(0, (nodeCount - 3) * 20); // Extra space for more nodes
  return baseSeparation + additionalSeparation;
};

/**
 * Simplified grid positioning function
 */
export const getGridPosition = (
  index: number,
  nodeId?: string,
  savedPositions?: Record<string, { x: number; y: number }>,
  totalNodes?: number
): { x: number; y: number } => {
  // Check if we have a valid saved position for this node
  if (
    nodeId &&
    savedPositions &&
    savedPositions[nodeId] &&
    savedPositions[nodeId].x > 50 &&
    savedPositions[nodeId].y > 50 &&
    savedPositions[nodeId].x < 2000 &&
    savedPositions[nodeId].y < 2000
  ) {
    return savedPositions[nodeId];
  }

  // Simple grid layout: 3 columns, rows as needed
  const colsPerRow = 3;
  const colWidth = 300;
  const rowHeight = 200;
  const startX = 200;
  const startY = 200;

  const row = Math.floor(index / colsPerRow);
  const col = index % colsPerRow;

  return {
    x: startX + col * colWidth,
    y: startY + row * rowHeight,
  };
};

/**
 * Map TripComponent type to WorkflowNodeData type
 */
const mapComponentType = (type: string): WorkflowNodeData['type'] => {
  switch (type.toLowerCase()) {
    case 'attraction':
    case 'activity':
      return 'Attraction';
    case 'restaurant':
    case 'meal':
      return 'Meal';
    case 'hotel':
    case 'accommodation':
      return 'Hotel';
    case 'transport':
    case 'transportation':
      return 'Transit';
    default:
      return 'Attraction';
  }
};

/**
 * Parse time strings to get start time
 */
const parseStartTime = (timeStr: string): string => {
  if (!timeStr) return '09:00';

  // If it's already in HH:MM format
  if (/^\d{1,2}:\d{2}$/.test(timeStr)) {
    return timeStr;
  }

  try {
    const date = new Date(timeStr);
    if (!isNaN(date.getTime())) {
      return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    }
  } catch {
    // Fall through to default
  }

  return '09:00';
};

/**
 * Calculate duration in minutes
 */
const parseDuration = (durationStr: string | number): number => {
  if (typeof durationStr === 'number') return durationStr;
  if (!durationStr) return 120; // Default 2 hours

  const str = durationStr.toString().toLowerCase();

  // Handle different duration formats
  if (str.includes('hour') || str.includes('hr')) {
    const match = str.match(/(\d+)/);
    return match ? parseInt(match[1]) * 60 : 120;
  } else if (str.includes('min')) {
    const match = str.match(/(\d+)/);
    return match ? parseInt(match[1]) : 120;
  } else {
    // Try to extract number and assume hours
    const match = str.match(/(\d+)/);
    return match ? parseInt(match[1]) * 60 : 120;
  }
};

/**
 * Convert API trip data to WorkflowDay format
 */
export const createWorkflowDaysFromTripData = (
  tripData: TripData,
  savedPositions?: Record<string, { x: number; y: number }>
): WorkflowDay[] => {
  if (!tripData.itinerary?.days || tripData.itinerary.days.length === 0) {
    return [];
  }

  return tripData.itinerary.days.map((day, dayIndex) => {
    const dayComponents = (day as any).nodes || day.components || [];
    
    const nodes: Node<WorkflowNodeData>[] = dayComponents.map((component, componentIndex) => {
      const nodeData = {
        id: component.id,
        type: mapComponentType(component.type),
        // Support both backend formats: NormalizedNode (title) and TripComponent (name)
        title: component.title || component.name || 'Untitled',
        tags: component.details?.tags || [component.type],
        start: parseStartTime(component.timing?.startTime || '09:00'),
        durationMin: parseDuration(component.timing?.duration || component.timing?.durationMin || '2h'),
        costINR: component.cost?.pricePerPerson || component.cost?.amountPerPerson || 0,
        meta: {
          rating: component.details?.rating || 4.0,
          open: '09:00',
          close: '18:00',
          address: component.location?.address || component.location?.name || 'Unknown',
          distanceKm: component.travel?.distanceFromPrevious || 0,
        },
      };
      
      return {
        id: component.id,
        type: 'workflow',
        position: getGridPosition(componentIndex, component.id, savedPositions, dayComponents.length),
        data: nodeData,
      };
    });

    // Create edges between consecutive components
    const edges: Edge[] = [];
    for (let i = 0; i < nodes.length - 1; i++) {
      edges.push({
        id: `e${dayIndex}-${i}`,
        source: nodes[i].id,
        target: nodes[i + 1].id,
      });
    }

    return {
      day: dayIndex + 1, // Use 1-based day number for display
      date: day.date,
      nodes,
      edges,
    };
  });
};

/**
 * Mock seed data for 3 days (fallback)
 */
export const createSeedData = (): WorkflowDay[] => [
  {
    day: 1,
    date: '2024-03-15',
    nodes: [
      {
        id: 'day1-airport',
        type: 'workflow',
        position: { x: 100, y: 100 },
        data: {
          id: 'day1-airport',
          type: 'Transit',
          title: 'Airport Arrival',
          tags: ['transport', 'arrival'],
          start: '08:00',
          durationMin: 60,
          costINR: 0,
          meta: {
            rating: 4.0,
            open: '24/7',
            close: '24/7',
            address: 'Indira Gandhi International Airport',
          },
        },
      },
      {
        id: 'day1-hotel',
        type: 'workflow',
        position: { x: 350, y: 100 },
        data: {
          id: 'day1-hotel',
          type: 'Hotel',
          title: 'Hotel Check-in',
          tags: ['accommodation', 'checkin'],
          start: '10:00',
          durationMin: 30,
          costINR: 0,
          meta: {
            rating: 4.2,
            open: '24/7',
            close: '24/7',
            address: 'Connaught Place, New Delhi',
            distanceKm: 15,
          },
        },
      },
      {
        id: 'day1-cafe',
        type: 'workflow',
        position: { x: 600, y: 100 },
        data: {
          id: 'day1-cafe',
          type: 'Meal',
          title: 'Breakfast Cafe',
          tags: ['cafe', 'veg', 'breakfast'],
          start: '11:00',
          durationMin: 45,
          costINR: 600,
          meta: {
            rating: 4.3,
            open: '07:00',
            close: '22:00',
            address: 'Khan Market, New Delhi',
            distanceKm: 2,
          },
        },
      },
      {
        id: 'day1-museum',
        type: 'workflow',
        position: { x: 350, y: 300 },
        data: {
          id: 'day1-museum',
          type: 'Attraction',
          title: 'National Museum',
          tags: ['museum', 'heritage', 'culture'],
          start: '13:00',
          durationMin: 120,
          costINR: 250,
          meta: {
            rating: 4.4,
            open: '10:00',
            close: '18:00',
            address: 'Janpath, New Delhi',
            distanceKm: 3,
          },
        },
      },
      {
        id: 'day1-market',
        type: 'workflow',
        position: { x: 600, y: 300 },
        data: {
          id: 'day1-market',
          type: 'Attraction',
          title: 'Night Market',
          tags: ['market', 'shopping', 'local'],
          start: '18:00',
          durationMin: 180,
          costINR: 1000,
          meta: {
            rating: 4.1,
            open: '17:00',
            close: '23:00',
            address: 'Palika Bazaar, New Delhi',
            distanceKm: 1,
          },
        },
      },
    ],
    edges: [
      { id: 'e1-1', source: 'day1-airport', target: 'day1-hotel' },
      { id: 'e1-2', source: 'day1-hotel', target: 'day1-cafe' },
      { id: 'e1-3', source: 'day1-cafe', target: 'day1-museum' },
      { id: 'e1-4', source: 'day1-museum', target: 'day1-market' },
    ],
  },
  {
    day: 2,
    date: '2024-03-16',
    nodes: [
      {
        id: 'day2-breakfast',
        type: 'workflow',
        position: { x: 100, y: 100 },
        data: {
          id: 'day2-breakfast',
          type: 'Meal',
          title: 'Breakfast Cafe',
          tags: ['cafe', 'veg', 'breakfast'],
          start: '08:30',
          durationMin: 45,
          costINR: 500,
          meta: {
            rating: 4.5,
            open: '07:00',
            close: '22:00',
            address: 'Hotel Restaurant',
          },
        },
      },
      {
        id: 'day2-walk',
        type: 'workflow',
        position: { x: 350, y: 100 },
        data: {
          id: 'day2-walk',
          type: 'Attraction',
          title: 'Heritage Walk',
          tags: ['walking', 'heritage', 'guided'],
          start: '10:00',
          durationMin: 180,
          costINR: 800,
          meta: {
            rating: 4.6,
            open: '09:00',
            close: '17:00',
            address: 'Old Delhi',
            distanceKm: 8,
          },
        },
      },
      {
        id: 'day2-gallery',
        type: 'workflow',
        position: { x: 600, y: 100 },
        data: {
          id: 'day2-gallery',
          type: 'Attraction',
          title: 'Art Gallery',
          tags: ['art', 'culture', 'indoor'],
          start: '14:00',
          durationMin: 90,
          costINR: 300,
          meta: {
            rating: 4.2,
            open: '10:00',
            close: '19:00',
            address: 'India Gate Area',
            distanceKm: 5,
          },
        },
      },
      {
        id: 'day2-dinner',
        type: 'workflow',
        position: { x: 350, y: 300 },
        data: {
          id: 'day2-dinner',
          type: 'Meal',
          title: 'Rooftop Dinner',
          tags: ['dinner', 'rooftop', 'fine-dining'],
          start: '19:00',
          durationMin: 120,
          costINR: 2500,
          meta: {
            rating: 4.7,
            open: '18:00',
            close: '23:00',
            address: 'Connaught Place',
            distanceKm: 3,
          },
        },
      },
    ],
    edges: [
      { id: 'e2-1', source: 'day2-breakfast', target: 'day2-walk' },
      { id: 'e2-2', source: 'day2-walk', target: 'day2-gallery' },
      { id: 'e2-3', source: 'day2-gallery', target: 'day2-dinner' },
    ],
  },
  {
    day: 3,
    date: '2024-03-17',
    nodes: [
      {
        id: 'day3-transit',
        type: 'workflow',
        position: { x: 100, y: 100 },
        data: {
          id: 'day3-transit',
          type: 'Transit',
          title: 'Early Transit',
          tags: ['transport', 'early'],
          start: '07:00',
          durationMin: 90,
          costINR: 800,
          meta: {
            rating: 4.0,
            open: '24/7',
            close: '24/7',
            address: 'To Hill Station',
            distanceKm: 45,
          },
        },
      },
      {
        id: 'day3-trek',
        type: 'workflow',
        position: { x: 350, y: 100 },
        data: {
          id: 'day3-trek',
          type: 'Attraction',
          title: 'Hill Trek',
          tags: ['trekking', 'nature', 'adventure'],
          start: '09:00',
          durationMin: 240,
          costINR: 1200,
          meta: {
            rating: 4.8,
            open: '06:00',
            close: '18:00',
            address: 'Hill Station Base',
          },
        },
      },
      {
        id: 'day3-lunch',
        type: 'workflow',
        position: { x: 600, y: 100 },
        data: {
          id: 'day3-lunch',
          type: 'Meal',
          title: 'Local Lunch',
          tags: ['local', 'veg', 'authentic'],
          start: '13:30',
          durationMin: 60,
          costINR: 400,
          meta: {
            rating: 4.3,
            open: '11:00',
            close: '22:00',
            address: 'Hill Station Market',
          },
        },
      },
      {
        id: 'day3-park',
        type: 'workflow',
        position: { x: 350, y: 300 },
        data: {
          id: 'day3-park',
          type: 'Attraction',
          title: 'Lakeside Park',
          tags: ['nature', 'relaxation', 'scenic'],
          start: '15:00',
          durationMin: 120,
          costINR: 200,
          meta: {
            rating: 4.5,
            open: '06:00',
            close: '20:00',
            address: 'Lake View Point',
            distanceKm: 2,
          },
        },
      },
      {
        id: 'day3-return',
        type: 'workflow',
        position: { x: 600, y: 300 },
        data: {
          id: 'day3-return',
          type: 'Transit',
          title: 'Return to Hotel',
          tags: ['transport', 'return'],
          start: '17:30',
          durationMin: 90,
          costINR: 800,
          meta: {
            rating: 4.0,
            open: '24/7',
            close: '24/7',
            address: 'Back to Delhi',
            distanceKm: 45,
          },
        },
      },
    ],
    edges: [
      { id: 'e3-1', source: 'day3-transit', target: 'day3-trek' },
      { id: 'e3-2', source: 'day3-trek', target: 'day3-lunch' },
      { id: 'e3-3', source: 'day3-lunch', target: 'day3-park' },
      { id: 'e3-4', source: 'day3-park', target: 'day3-return' },
    ],
  },
];

/**
 * Factory function to create node event handlers
 */
export const createNodeHandlers = (
  setNodes: React.Dispatch<React.SetStateAction<Node<WorkflowNodeData>[]>>,
  setEdges: React.Dispatch<React.SetStateAction<Edge[]>>,
  selectedNode: Node<WorkflowNodeData> | null,
  setSelectedNode: React.Dispatch<React.SetStateAction<Node<WorkflowNodeData> | null>>,
  setIsNodeInspectorOpen: React.Dispatch<React.SetStateAction<boolean>>,
  activeDay: number,
  nodes: Node<WorkflowNodeData>[],
  userChangeTracker: any
) => {
  const addNewNode = (nodeType: WorkflowNodeData['type']) => {
    const newNode = createNewNode(nodeType, activeDay, {
      x: 200 + Math.random() * 300,
      y: 200 + Math.random() * 300,
    });

    // Track the addition
    userChangeTracker.processWorkflowChange(newNode.id, 'added', undefined, newNode.data);

    setNodes((nds) => [...nds, newNode]);
  };

  const deleteNode = (nodeId: string) => {
    // Track the deletion
    const nodeToDelete = nodes.find((n) => n.id === nodeId);
    if (nodeToDelete) {
      userChangeTracker.processWorkflowChange(nodeId, 'deleted', nodeToDelete.data, undefined);
    }

    setNodes((nds) => nds.filter((node) => node.id !== nodeId));
    setEdges((eds) => eds.filter((edge) => edge.source !== nodeId && edge.target !== nodeId));
    if (selectedNode?.id === nodeId) {
      setSelectedNode(null);
      setIsNodeInspectorOpen(false);
    }
  };

  const updateSelectedNode = (updates: Partial<WorkflowNodeData>) => {
    if (!selectedNode) return;

    // Track the change
    userChangeTracker.processWorkflowChange(
      selectedNode.id,
      'modified',
      selectedNode.data,
      { ...selectedNode.data, ...updates }
    );

    setNodes((nds) =>
      nds.map((node) =>
        node.id === selectedNode.id ? { ...node, data: { ...node.data, ...updates } } : node
      )
    );

    setSelectedNode((prev) =>
      prev
        ? {
            ...prev,
            data: { ...prev.data, ...updates },
          }
        : null
    );
  };

  return {
    addNewNode,
    deleteNode,
    updateSelectedNode,
  };
};

/**
 * Factory function to create workflow action handlers
 */
export const createWorkflowActionHandlers = (
  workflowDays: WorkflowDay[],
  setWorkflowDays: React.Dispatch<React.SetStateAction<WorkflowDay[]>>,
  undoStack: WorkflowDay[][],
  setUndoStack: React.Dispatch<React.SetStateAction<WorkflowDay[][]>>,
  redoStack: WorkflowDay[][],
  setRedoStack: React.Dispatch<React.SetStateAction<WorkflowDay[][]>>,
  setActiveDay: React.Dispatch<React.SetStateAction<number>>,
  setSelectedNode: React.Dispatch<React.SetStateAction<Node<WorkflowNodeData> | null>>,
  tripData: TripData,
  onSave: (updatedItinerary: any) => void
) => {
  const undo = () => {
    if (undoStack.length > 0) {
      const previousState = undoStack[undoStack.length - 1];
      setRedoStack((prev) => [...prev, workflowDays]);
      setUndoStack((prev) => prev.slice(0, -1));
      setWorkflowDays(previousState);
    }
  };

  const redo = () => {
    if (redoStack.length > 0) {
      const nextState = redoStack[redoStack.length - 1];
      setUndoStack((prev) => [...prev, workflowDays]);
      setRedoStack((prev) => prev.slice(0, -1));
      setWorkflowDays(nextState);
    }
  };

  const resetDemo = () => {
    const tripDataWorkflow = createWorkflowDaysFromTripData(tripData);
    const workflowData = tripDataWorkflow.length > 0 ? tripDataWorkflow : createSeedData();
    setWorkflowDays(workflowData);
    setActiveDay(0);
    setSelectedNode(null);
    setUndoStack([]);
    setRedoStack([]);
  };

  const applyToItinerary = () => {
    const generatedItinerary = workflowDays.map((day) => ({
      day: day.day,
      date: day.date,
      activities: day.nodes
        .sort((a, b) => a.data.start.localeCompare(b.data.start))
        .map((node) => ({
          id: node.data.id,
          title: node.data.title,
          type: node.data.type.toLowerCase(),
          time: node.data.start,
          duration: node.data.durationMin,
          cost: node.data.costINR,
          description: `${node.data.type} activity with ${node.data.tags.join(', ')} features`,
          location: {
            lat: 28.6139 + Math.random() * 0.1,
            lng: 77.209 + Math.random() * 0.1,
          },
          rating: node.data.meta.rating,
          openingHours: `${node.data.meta.open} - ${node.data.meta.close}`,
          address: node.data.meta.address,
          tags: node.data.tags,
        })),
    }));

    onSave({ itinerary: { days: generatedItinerary } });
  };

  return {
    undo,
    redo,
    resetDemo,
    applyToItinerary,
  };
};
