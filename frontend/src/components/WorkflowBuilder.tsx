import React, { useState, useCallback, useRef, useEffect } from 'react';
import ReactFlow, {
  Node,
  Edge,
  addEdge,
  useNodesState,
  useEdgesState,
  Connection,
  ReactFlowProvider,
  Controls,
  Background,
  MiniMap,
  useReactFlow,
} from 'reactflow';
import 'reactflow/dist/style.css';
import { 
  validateWorkflowNode, 
  createNewNode,
  formatDuration,
  formatCurrency 
} from './workflow/WorkflowUtils';
import { Button } from './ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from './ui/card';
import { Input } from './ui/input';
import { Label } from './ui/label';
import { Badge } from './ui/badge';
import { Tabs, TabsContent, TabsList, TabsTrigger } from './ui/tabs';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from './ui/select';
import { Textarea } from './ui/textarea';
import { ScrollArea } from './ui/scroll-area';
import { Separator } from './ui/separator';
import { Alert, AlertDescription } from './ui/alert';
import {
  ArrowLeft,
  Plus,
  Trash2,
  Undo,
  Redo,
  RotateCcw,
  Save,
  MapPin,
  Coffee,
  Camera,
  Car,
  Bed,
  TreePine,
  Building,
  Utensils,
  Clock,
  DollarSign,
  AlertTriangle,
  CheckCircle,
  Info,
} from 'lucide-react';
import { WorkflowNode } from './workflow/WorkflowNode';
import { TripData } from '../types/TripData';

// Calculate optimal zoom based on number of nodes
const calculateOptimalZoom = (nodeCount: number): number => {
  if (nodeCount <= 3) return 0.8;
  if (nodeCount <= 6) return 0.6;
  if (nodeCount <= 9) return 0.5;
  if (nodeCount <= 12) return 0.4;
  return 0.3; // For more than 12 nodes
};

// Calculate vertical separation based on node count to avoid overlap
const calculateVerticalSeparation = (nodeCount: number): number => {
  const baseSeparation = 250; // Base separation
  const additionalSeparation = Math.max(0, (nodeCount - 3) * 20); // Extra space for more nodes
  return baseSeparation + additionalSeparation;
};

// Ladder positioning function - top-left to bottom-right diagonal
const getLadderPosition = (index: number, nodeId?: string, savedPositions?: Record<string, { x: number; y: number }>, totalNodes?: number): { x: number; y: number } => {
  // Check if we have a saved position for this node (but only if it's not at origin and reasonable)
  if (nodeId && savedPositions && savedPositions[nodeId] && 
      (savedPositions[nodeId].x !== 0 || savedPositions[nodeId].y !== 0) &&
      savedPositions[nodeId].x > 50 && savedPositions[nodeId].y > 50) {
    console.log(`📍 SAVED POSITION for ${nodeId}:`, {
      position: savedPositions[nodeId],
      index,
      totalNodes
    });
    return savedPositions[nodeId];
  }
  
  const baseX = 400; // Start from center-left
  const baseY = 300; // Start from center-top
  const stepX = 250; // Move right
  const stepY = 100; // Move down
  const verticalSeparation = totalNodes ? calculateVerticalSeparation(totalNodes) : 250;
  
  let position;
  let ladderType;
  
  if (index < 3) {
    // First ladder: cards 0, 1, 2 (top-left to bottom-right)
    position = {
      x: baseX + index * stepX,
      y: baseY + index * stepY
    };
    ladderType = "FIRST LADDER";
  } else {
    // Second ladder: cards 3, 4, 5 (below corresponding first ladder cards)
    const secondLadderIndex = index - 3;
    
    // Calculate the position of the corresponding card in the first ladder
    const correspondingFirstLadderIndex = secondLadderIndex;
    const firstLadderCardX = baseX + correspondingFirstLadderIndex * stepX;
    const firstLadderCardY = baseY + correspondingFirstLadderIndex * stepY;
    
    // Position the second ladder card directly below the corresponding first ladder card
    position = {
      x: firstLadderCardX,
      y: firstLadderCardY + verticalSeparation
    };
    ladderType = "SECOND LADDER";
  }
  
  console.log(`🎯 CALCULATED POSITION for ${nodeId || 'unknown'}:`, {
    nodeId,
    index,
    ladderType,
    position,
    totalNodes,
    verticalSeparation,
    baseCoordinates: { baseX, baseY },
    stepValues: { stepX, stepY }
  });
  
  return position;
};

// Register custom node types (moved outside component to prevent React Flow warning)
const nodeTypes = {
  workflow: WorkflowNode,
};

export interface WorkflowNodeData {
  id: string;
  type: 'Attraction' | 'Meal' | 'Transit' | 'Hotel' | 'FreeTime' | 'Decision';
  title: string;
  tags: string[];
  start: string;
  durationMin: number;
  costINR: number;
  meta: {
    rating: number;
    open: string;
    close: string;
    address: string;
    distanceKm?: number;
  };
  validation?: {
    status: 'valid' | 'warning' | 'error';
    message?: string;
  };
}

interface WorkflowBuilderProps {
  tripData: TripData;
  onSave: (updatedItinerary: any) => void;
  onCancel: () => void;
  embedded?: boolean; // When true, removes full-screen layout and header
}

interface WorkflowDay {
  day: number;
  date: string;
  nodes: Node<WorkflowNodeData>[];
  edges: Edge[];
}

// Convert API trip data to WorkflowDay format
const createWorkflowDaysFromTripData = (tripData: TripData, savedPositions?: Record<string, { x: number; y: number }>): WorkflowDay[] => {
  if (!tripData.itinerary?.days || tripData.itinerary.days.length === 0) {
    return [];
  }

  return tripData.itinerary.days.map((day, dayIndex) => {
    const nodes: Node<WorkflowNodeData>[] = day.components.map((component, componentIndex) => {
      // Map TripComponent type to WorkflowNodeData type
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

      // Parse time strings to get start time
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

      // Calculate duration in minutes
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

      return {
        id: component.id,
        type: 'workflow',
        position: getLadderPosition(componentIndex, component.id, savedPositions, day.components.length),
        data: {
          id: component.id,
          type: mapComponentType(component.type),
          title: component.name,
          tags: component.details?.tags || [component.type],
          start: parseStartTime(component.timing?.startTime || '09:00'),
          durationMin: parseDuration(component.timing?.duration || '2h'),
          costINR: component.cost?.pricePerPerson || 0,
          meta: {
            rating: component.details?.rating || 4.0,
            open: '09:00',
            close: '18:00',
            address: component.location?.address || component.location?.name || 'Unknown',
            distanceKm: component.travel?.distanceFromPrevious || 0,
          },
        },
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

// Mock seed data for 3 days (fallback)
const createSeedData = (): WorkflowDay[] => [
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

const WorkflowBuilderContent: React.FC<WorkflowBuilderProps> = ({ tripData, onSave, onCancel, embedded = false }) => {
  const [workflowDays, setWorkflowDays] = useState<WorkflowDay[]>(() => {
    const savedPositions = (() => {
      try {
        const saved = localStorage.getItem(`workflow-positions-${tripData.id}`);
        return saved ? JSON.parse(saved) : {};
      } catch {
        return {};
      }
    })();
    
    return createWorkflowDaysFromTripData(tripData, savedPositions).length > 0 
      ? createWorkflowDaysFromTripData(tripData, savedPositions) 
      : createSeedData();
  });
  const [activeDay, setActiveDay] = useState(0);
  const [selectedNode, setSelectedNode] = useState<Node<WorkflowNodeData> | null>(null);
  const [undoStack, setUndoStack] = useState<WorkflowDay[][]>([]);
  const [redoStack, setRedoStack] = useState<WorkflowDay[][]>([]);
  const [savedPositions, setSavedPositions] = useState<Record<string, { x: number; y: number }>>(() => {
    // Load saved positions from localStorage
    try {
      const saved = localStorage.getItem(`workflow-positions-${tripData.id}`);
      return saved ? JSON.parse(saved) : {};
    } catch {
      return {};
    }
  });

  const currentDay = workflowDays[activeDay];
  const [nodes, setNodes, onNodesChangeBase] = useNodesState(currentDay?.nodes || []);
  const [edges, setEdges, onEdgesChange] = useEdgesState(currentDay?.edges || []);

  const reactFlowWrapper = useRef<HTMLDivElement>(null);
  const { project, getViewport, setViewport } = useReactFlow();

  // Save positions to localStorage
  const savePositions = useCallback((positions: Record<string, { x: number; y: number }>) => {
    try {
      console.log(`💾 SAVING POSITIONS:`, {
        tripId: tripData.id,
        positionCount: Object.keys(positions).length,
        positions: Object.entries(positions).map(([nodeId, pos]) => ({
          nodeId,
          position: pos
        }))
      });
      localStorage.setItem(`workflow-positions-${tripData.id}`, JSON.stringify(positions));
      setSavedPositions(positions);
    } catch (error) {
      console.error('Failed to save positions:', error);
    }
  }, [tripData.id]);

  // Custom onNodesChange that saves positions when nodes are moved
  const onNodesChange = useCallback((changes: any[]) => {
    onNodesChangeBase(changes);
    
    // Check if any changes are position changes
    const positionChanges = changes.filter(change => change.type === 'position' && change.position);
    if (positionChanges.length > 0) {
      const newPositions = { ...savedPositions };
      positionChanges.forEach(change => {
        if (change.position) {
          newPositions[change.id] = change.position;
        }
      });
      savePositions(newPositions);
    }
  }, [onNodesChangeBase, savedPositions, savePositions]);

  // Clear bad saved positions (positions at origin or top-left)
  const clearBadPositions = useCallback(() => {
    const cleanedPositions = { ...savedPositions };
    Object.keys(cleanedPositions).forEach(nodeId => {
      const pos = cleanedPositions[nodeId];
      if (pos.x === 0 && pos.y === 0) {
        delete cleanedPositions[nodeId];
      }
    });
    savePositions(cleanedPositions);
  }, [savedPositions, savePositions]);

  // Reset positions to ladder pattern
  const resetToLadderPattern = useCallback(() => {
    if (!currentDay) return;
    
    // Clear any bad positions first
    clearBadPositions();
    
    const newPositions = { ...savedPositions };
    currentDay.nodes.forEach((node, index) => {
      newPositions[node.id] = getLadderPosition(index, node.id, {}, currentDay.nodes.length);
    });
    
    // Update nodes with new positions
    const updatedNodes = currentDay.nodes.map((node, index) => ({
      ...node,
      position: newPositions[node.id]
    }));
    
    setNodes(updatedNodes);
    savePositions(newPositions);
  }, [currentDay, savedPositions, setNodes, savePositions, clearBadPositions]);

  // Update workflow days when trip data changes (only on initial load)
  useEffect(() => {
    const newWorkflowDays = createWorkflowDaysFromTripData(tripData, savedPositions);
    if (newWorkflowDays.length > 0 && workflowDays.length === 0) {
      setWorkflowDays(newWorkflowDays);
      setActiveDay(0);
    }
  }, [tripData]);

  // Update workflow days with saved positions when savedPositions changes
  useEffect(() => {
    if (workflowDays.length > 0) {
      const updatedWorkflowDays = createWorkflowDaysFromTripData(tripData, savedPositions);
      setWorkflowDays(updatedWorkflowDays);
    }
  }, [savedPositions]);


  // Update React Flow when active day changes
  useEffect(() => {
    if (currentDay) {
      console.log(`📅 SWITCHING TO DAY ${activeDay + 1}:`, {
        dayNumber: currentDay.day,
        nodeCount: currentDay.nodes.length,
        nodePositions: currentDay.nodes.map(n => ({
          id: n.id,
          position: n.position,
          title: n.data.title
        }))
      });
      setNodes(currentDay.nodes);
      setEdges(currentDay.edges);
    }
  }, [activeDay, setNodes, setEdges]);

  // Update viewport zoom when nodes change
  useEffect(() => {
    if (nodes.length > 0) {
      const optimalZoom = calculateOptimalZoom(nodes.length);
      const currentViewport = getViewport();
      setViewport({
        x: currentViewport.x,
        y: currentViewport.y,
        zoom: optimalZoom
      });
    }
  }, [nodes.length, setViewport, getViewport]);

  // Update workflowDays when nodes/edges change
  useEffect(() => {
    if (currentDay) {
      const updatedDays = [...workflowDays];
      updatedDays[activeDay] = {
        ...currentDay,
        nodes,
        edges,
      };
      setWorkflowDays(updatedDays);
    }
  }, [nodes, edges]);

  const validateNodes = useCallback((dayNodes: Node<WorkflowNodeData>[]) => {
    return dayNodes.map(node => {
      const validation = validateWorkflowNode(node, dayNodes);
      return {
        ...node,
        data: { ...node.data, validation },
      };
    });
  }, []);

  const onConnect = useCallback((params: Connection | Edge) => {
    setEdges((eds) => addEdge(params, eds));
  }, [setEdges]);

  const onNodeClick = useCallback((event: React.MouseEvent, node: Node<WorkflowNodeData>) => {
    setSelectedNode(node);
  }, []);

  const onPaneClick = useCallback(() => {
    setSelectedNode(null);
  }, []);

  const addNewNode = useCallback((nodeType: WorkflowNodeData['type']) => {
    const newNode = createNewNode(nodeType, activeDay, {
      x: 200 + Math.random() * 300,
      y: 200 + Math.random() * 300,
    });

    setNodes((nds) => [...nds, newNode]);
  }, [activeDay, setNodes]);

  const deleteNode = useCallback((nodeId: string) => {
    setNodes((nds) => nds.filter((node) => node.id !== nodeId));
    setEdges((eds) => eds.filter((edge) => edge.source !== nodeId && edge.target !== nodeId));
    if (selectedNode?.id === nodeId) {
      setSelectedNode(null);
    }
  }, [setNodes, setEdges, selectedNode]);

  const updateSelectedNode = useCallback((updates: Partial<WorkflowNodeData>) => {
    if (!selectedNode) return;
    
    setNodes((nds) =>
      nds.map((node) =>
        node.id === selectedNode.id
          ? { ...node, data: { ...node.data, ...updates } }
          : node
      )
    );
    
    setSelectedNode(prev => prev ? { 
      ...prev, 
      data: { ...prev.data, ...updates } 
    } : null);
  }, [selectedNode, setNodes]);


  const undo = useCallback(() => {
    if (undoStack.length > 0) {
      const previousState = undoStack[undoStack.length - 1];
      setRedoStack(prev => [...prev, workflowDays]);
      setUndoStack(prev => prev.slice(0, -1));
      setWorkflowDays(previousState);
    }
  }, [undoStack, workflowDays]);

  const redo = useCallback(() => {
    if (redoStack.length > 0) {
      const nextState = redoStack[redoStack.length - 1];
      setUndoStack(prev => [...prev, workflowDays]);
      setRedoStack(prev => prev.slice(0, -1));
      setWorkflowDays(nextState);
    }
  }, [redoStack, workflowDays]);

  const resetDemo = useCallback(() => {
    const tripDataWorkflow = createWorkflowDaysFromTripData(tripData);
    const workflowData = tripDataWorkflow.length > 0 ? tripDataWorkflow : createSeedData();
    setWorkflowDays(workflowData);
    setActiveDay(0);
    setSelectedNode(null);
    setUndoStack([]);
    setRedoStack([]);
  }, [tripData]);

  const applyToItinerary = useCallback(() => {
    const generatedItinerary = workflowDays.map(day => ({
      day: day.day,
      date: day.date,
      activities: day.nodes
        .sort((a, b) => a.data.start.localeCompare(b.data.start))
        .map(node => ({
          id: node.data.id,
          title: node.data.title,
          type: node.data.type.toLowerCase(),
          time: node.data.start,
          duration: node.data.durationMin,
          cost: node.data.costINR,
          description: `${node.data.type} activity with ${node.data.tags.join(', ')} features`,
          location: {
            lat: 28.6139 + Math.random() * 0.1,
            lng: 77.2090 + Math.random() * 0.1,
          },
          rating: node.data.meta.rating,
          openingHours: `${node.data.meta.open} - ${node.data.meta.close}`,
          address: node.data.meta.address,
          tags: node.data.tags,
        })),
    }));

    onSave({ itinerary: { days: generatedItinerary } });
  }, [workflowDays, onSave]);


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
    <div className={embedded ? "h-full flex flex-col bg-gray-50" : "h-screen flex flex-col bg-gray-50"}>

      <div className="flex-1 flex">
        {/* Main Canvas Area */}
        <div className="flex-1 flex flex-col">

          {/* Section 1: Main Navigation (Map/Workflow/AI Assistant) - Only show when not embedded */}
          {!embedded && (
            <div className="bg-white border-b px-6 py-4 mb-2">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-4">
                  <Button variant="ghost" onClick={onCancel}>
                    <ArrowLeft className="h-4 w-4 mr-2" />
                    Back
                  </Button>
                  <div>
                    <h1 className="text-2xl">Workflow Builder</h1>
                    <p className="text-gray-600">{tripData.destination}</p>
                  </div>
                </div>
                <Button onClick={applyToItinerary}>
                  <Save className="h-4 w-4 mr-2" />
                  Apply to Itinerary
                </Button>
              </div>
            </div>
          )}


          {/* Section 2: Day Navigation */}
          <div className="bg-white border-b px-4 py-4 mb-2">
            <div className="flex items-center justify-center">
              <Tabs value={activeDay.toString()} onValueChange={(value) => setActiveDay(parseInt(value))}>
                <TabsList className="h-10 gap-2">
                  {workflowDays.map((day, index) => (
                    <TabsTrigger key={index} value={index.toString()} className="text-sm px-4 py-2 mx-1">
                      Day {day.day}
                    </TabsTrigger>
                  ))}
                </TabsList>
              </Tabs>
            </div>
          </div>

          {/* Section 3: Add Node Toolbar */}
          <div className="bg-white border-b px-4 py-4 mb-2">
            <div className="flex items-center justify-center gap-2">
              <span className="text-sm font-medium mr-4">Add Node:</span>
              {(['Attraction', 'Meal', 'Transit', 'Hotel', 'FreeTime'] as const).map((type) => {
                const Icon = getNodeIcon(type);
                return (
                  <Button
                    key={type}
                    variant="outline"
                    size="sm"
                    onClick={() => addNewNode(type)}
                  >
                    <Icon className="h-4 w-4 mr-2" />
                    {type}
                  </Button>
                );
              })}
            </div>
          </div>

          {/* Canvas Container */}
          <div className="flex-1 flex relative">
            {/* React Flow Canvas */}
            <div className="flex-1" ref={reactFlowWrapper}>
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
                onClick={resetToLadderPattern}
                className="w-10 h-10 p-0 shadow-lg"
                title="Reset to Ladder"
              >
                <RotateCcw className="h-4 w-4" />
              </Button>
            </div>

          </div>
        </div>

        {/* Side Panel - Node Inspector */}
        {selectedNode && (
          <div className="w-80 bg-white border-l">
            <div className="p-4 border-b">
              <div className="flex items-center justify-between">
                <h3 className="font-medium">Node Inspector</h3>
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={() => deleteNode(selectedNode.id)}
                >
                  <Trash2 className="h-4 w-4" />
                </Button>
              </div>
            </div>
            
            <ScrollArea className="h-full">
              <div className="p-4 space-y-4">
                <div>
                  <Label htmlFor="node-title">Title</Label>
                  <Input
                    id="node-title"
                    value={selectedNode.data.title}
                    onChange={(e) => updateSelectedNode({ title: e.target.value })}
                    className="mt-1"
                  />
                </div>

                <div>
                  <Label htmlFor="node-type">Category</Label>
                  <Select
                    value={selectedNode.data.type}
                    onValueChange={(value: WorkflowNodeData['type']) => updateSelectedNode({ type: value })}
                  >
                    <SelectTrigger className="mt-1">
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="Attraction">Attraction</SelectItem>
                      <SelectItem value="Meal">Meal</SelectItem>
                      <SelectItem value="Transit">Transit</SelectItem>
                      <SelectItem value="Hotel">Hotel</SelectItem>
                      <SelectItem value="FreeTime">Free Time</SelectItem>
                    </SelectContent>
                  </Select>
                </div>

                <div className="grid grid-cols-2 gap-2">
                  <div>
                    <Label htmlFor="node-start">Start Time</Label>
                    <Input
                      id="node-start"
                      type="time"
                      value={selectedNode.data.start}
                      onChange={(e) => updateSelectedNode({ start: e.target.value })}
                      className="mt-1"
                    />
                  </div>
                  <div>
                    <Label htmlFor="node-duration">Duration (min)</Label>
                    <Input
                      id="node-duration"
                      type="number"
                      value={selectedNode.data.durationMin}
                      onChange={(e) => updateSelectedNode({ durationMin: parseInt(e.target.value) || 0 })}
                      className="mt-1"
                    />
                  </div>
                </div>

                <div>
                  <Label htmlFor="node-cost">Cost (₹)</Label>
                  <Input
                    id="node-cost"
                    type="number"
                    value={selectedNode.data.costINR}
                    onChange={(e) => updateSelectedNode({ costINR: parseInt(e.target.value) || 0 })}
                    className="mt-1"
                  />
                </div>

                <div>
                  <Label htmlFor="node-tags">Tags</Label>
                  <Input
                    id="node-tags"
                    value={selectedNode.data.tags.join(', ')}
                    onChange={(e) => updateSelectedNode({ tags: e.target.value.split(', ').filter(Boolean) })}
                    placeholder="veg, family, heritage"
                    className="mt-1"
                  />
                </div>

                <Separator />

                <div>
                  <h4 className="font-medium mb-2">Read-only Meta</h4>
                  <div className="space-y-2 text-sm">
                    <div className="flex justify-between">
                      <span className="text-gray-600">Rating:</span>
                      <span>{selectedNode.data.meta.rating}/5</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-gray-600">Hours:</span>
                      <span>{selectedNode.data.meta.open} - {selectedNode.data.meta.close}</span>
                    </div>
                    <div className="text-gray-600">
                      <span>Address:</span>
                      <p className="mt-1 text-gray-900">{selectedNode.data.meta.address}</p>
                    </div>
                    {selectedNode.data.meta.distanceKm && (
                      <div className="flex justify-between">
                        <span className="text-gray-600">Distance:</span>
                        <span>{selectedNode.data.meta.distanceKm}km</span>
                      </div>
                    )}
                  </div>
                </div>

                {selectedNode.data.validation && selectedNode.data.validation.status !== 'valid' && (
                  <Alert className={`${selectedNode.data.validation.status === 'error' ? 'border-red-200 bg-red-50' : 'border-amber-200 bg-amber-50'}`}>
                    <AlertTriangle className="h-4 w-4" />
                    <AlertDescription>
                      {selectedNode.data.validation.message}
                    </AlertDescription>
                  </Alert>
                )}
              </div>
            </ScrollArea>
          </div>
        )}
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