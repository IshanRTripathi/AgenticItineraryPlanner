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
  autoArrangeNodes, 
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

// Register custom node types
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
  selectedDay?: { dayNumber: number; dayData: any } | null;
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
const createWorkflowDaysFromTripData = (tripData: TripData): WorkflowDay[] => {
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
        position: { 
          x: 100 + (componentIndex % 3) * 200, 
          y: 100 + Math.floor(componentIndex / 3) * 150 
        },
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
      day: day.dayNumber,
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

const WorkflowBuilderContent: React.FC<WorkflowBuilderProps> = ({ tripData, selectedDay, onSave, onCancel, embedded = false }) => {
  const [workflowDays, setWorkflowDays] = useState<WorkflowDay[]>(() => 
    createWorkflowDaysFromTripData(tripData).length > 0 
      ? createWorkflowDaysFromTripData(tripData) 
      : createSeedData()
  );
  const [activeDay, setActiveDay] = useState(0);
  const [selectedNode, setSelectedNode] = useState<Node<WorkflowNodeData> | null>(null);
  const [undoStack, setUndoStack] = useState<WorkflowDay[][]>([]);
  const [redoStack, setRedoStack] = useState<WorkflowDay[][]>([]);

  const currentDay = workflowDays[activeDay];
  const [nodes, setNodes, onNodesChange] = useNodesState(currentDay?.nodes || []);
  const [edges, setEdges, onEdgesChange] = useEdgesState(currentDay?.edges || []);

  const reactFlowWrapper = useRef<HTMLDivElement>(null);
  const { project, getViewport } = useReactFlow();

  // Update workflow days when trip data changes
  useEffect(() => {
    const newWorkflowDays = createWorkflowDaysFromTripData(tripData);
    if (newWorkflowDays.length > 0) {
      setWorkflowDays(newWorkflowDays);
      setActiveDay(0); // Reset to first day
    }
  }, [tripData]);

  // Update active day when selectedDay changes
  useEffect(() => {
    if (selectedDay) {
      const dayIndex = workflowDays.findIndex(day => day.day === selectedDay.dayNumber);
      if (dayIndex !== -1) {
        setActiveDay(dayIndex);
      }
    }
  }, [selectedDay, workflowDays]);

  // Update React Flow when active day changes
  useEffect(() => {
    if (currentDay) {
      setNodes(currentDay.nodes);
      setEdges(currentDay.edges);
    }
  }, [activeDay, setNodes, setEdges]);

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

  const autoArrange = useCallback(() => {
    const arrangedNodes = autoArrangeNodes(nodes);
    setNodes(arrangedNodes);
  }, [nodes, setNodes]);

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
      {/* Header - Only show when not embedded */}
      {!embedded && (
        <div className="bg-white border-b px-6 py-4">
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
            
            <div className="flex gap-2">
              <Button variant="outline" onClick={undo} disabled={undoStack.length === 0}>
                <Undo className="h-4 w-4" />
              </Button>
              <Button variant="outline" onClick={redo} disabled={redoStack.length === 0}>
                <Redo className="h-4 w-4" />
              </Button>
              <Button variant="outline" onClick={autoArrange}>
                <RotateCcw className="h-4 w-4 mr-2" />
                Auto Arrange
              </Button>
              <Button variant="outline" onClick={resetDemo}>
                Reset to Original
              </Button>
              <Button onClick={applyToItinerary}>
                <Save className="h-4 w-4 mr-2" />
                Apply to Itinerary
              </Button>
            </div>
          </div>
        </div>
      )}

      <div className="flex-1 flex">
        {/* Main Canvas Area */}
        <div className="flex-1 flex flex-col">
          {/* Day Tabs */}
          <div className="bg-white border-b px-6 py-2">
            <Tabs value={activeDay.toString()} onValueChange={(value) => setActiveDay(parseInt(value))}>
              <TabsList>
                {workflowDays.map((day, index) => (
                  <TabsTrigger key={index} value={index.toString()}>
                    Day {day.day}
                  </TabsTrigger>
                ))}
              </TabsList>
            </Tabs>
          </div>

          {/* Compact Toolbar for Embedded Mode */}
          {embedded && (
            <div className="bg-white border-b px-4 py-2">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <Button variant="outline" size="sm" onClick={undo} disabled={undoStack.length === 0}>
                    <Undo className="h-3 w-3" />
                  </Button>
                  <Button variant="outline" size="sm" onClick={redo} disabled={redoStack.length === 0}>
                    <Redo className="h-3 w-3" />
                  </Button>
                  <Button variant="outline" size="sm" onClick={autoArrange}>
                    <RotateCcw className="h-3 w-3 mr-1" />
                    Arrange
                  </Button>
                </div>
                <div className="flex items-center gap-2">
                  <Button variant="outline" size="sm" onClick={resetDemo}>
                    Reset
                  </Button>
                  <Button size="sm" onClick={applyToItinerary}>
                    <Save className="h-3 w-3 mr-1" />
                    Apply
                  </Button>
                </div>
              </div>
            </div>
          )}

          {/* Toolbar - Only show when not embedded */}
          {!embedded && (
            <div className="bg-white border-b px-6 py-3">
            <div className="flex items-center gap-2">
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
          )}

          {/* Canvas Container */}
          <div className="flex-1 flex">
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
                snapToGrid
                snapGrid={[20, 20]}
              >
                <Controls />
                <MiniMap />
                <Background variant="dots" gap={20} size={1} />
              </ReactFlow>
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