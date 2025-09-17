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
  generateTimelineFromNodes,
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
import { TripData } from '../App';

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
  onSave: (updatedItinerary: any) => void;
  onCancel: () => void;
}

interface WorkflowDay {
  day: number;
  date: string;
  nodes: Node<WorkflowNodeData>[];
  edges: Edge[];
}

// Mock seed data for 3 days
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

const WorkflowBuilderContent: React.FC<WorkflowBuilderProps> = ({ tripData, onSave, onCancel }) => {
  const [workflowDays, setWorkflowDays] = useState<WorkflowDay[]>(createSeedData);
  const [activeDay, setActiveDay] = useState(0);
  const [selectedNode, setSelectedNode] = useState<Node<WorkflowNodeData> | null>(null);
  const [undoStack, setUndoStack] = useState<WorkflowDay[][]>([]);
  const [redoStack, setRedoStack] = useState<WorkflowDay[][]>([]);
  const [showTimeline, setShowTimeline] = useState(true);

  const currentDay = workflowDays[activeDay];
  const [nodes, setNodes, onNodesChange] = useNodesState(currentDay?.nodes || []);
  const [edges, setEdges, onEdgesChange] = useEdgesState(currentDay?.edges || []);

  const reactFlowWrapper = useRef<HTMLDivElement>(null);
  const { project, getViewport } = useReactFlow();

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
    const seedData = createSeedData();
    setWorkflowDays(seedData);
    setActiveDay(0);
    setSelectedNode(null);
    setUndoStack([]);
    setRedoStack([]);
  }, []);

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

  const generateTimeline = useCallback(() => {
    if (!currentDay) return [];
    return generateTimelineFromNodes(currentDay.nodes);
  }, [currentDay]);

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
    <div className="h-screen flex flex-col bg-gray-50">
      {/* Header */}
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
              Reset Demo
            </Button>
            <Button onClick={applyToItinerary}>
              <Save className="h-4 w-4 mr-2" />
              Apply to Itinerary
            </Button>
          </div>
        </div>
      </div>

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

          {/* Toolbar */}
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
              <Separator orientation="vertical" className="mx-4 h-6" />
              <Button
                variant="outline"
                size="sm"
                onClick={() => setShowTimeline(!showTimeline)}
              >
                {showTimeline ? 'Hide Timeline' : 'Show Timeline'}
              </Button>
            </div>
          </div>

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

            {/* Timeline Preview */}
            {showTimeline && (
              <div className="w-80 bg-white border-l">
                <div className="p-4 border-b">
                  <h3 className="font-medium">Day {currentDay?.day} Timeline</h3>
                  <p className="text-sm text-gray-600">Generated from workflow</p>
                </div>
                <ScrollArea className="h-full">
                  <div className="p-4 space-y-3">
                    {generateTimeline().map((item, index) => (
                      <div key={index} className="p-3 border rounded-lg">
                        <div className="flex items-start justify-between mb-2">
                          <div className="flex items-center gap-2">
                            <Badge variant="outline" className="text-xs">
                              {item.time}
                            </Badge>
                            {item.validation?.status === 'warning' && (
                              <AlertTriangle className="h-4 w-4 text-amber-500" />
                            )}
                            {item.validation?.status === 'error' && (
                              <AlertTriangle className="h-4 w-4 text-red-500" />
                            )}
                            {item.validation?.status === 'valid' && (
                              <CheckCircle className="h-4 w-4 text-green-500" />
                            )}
                          </div>
                        </div>
                        <h4 className="font-medium text-sm">{item.title}</h4>
                        <div className="flex items-center gap-2 mt-1">
                          <Badge variant="secondary" className="text-xs">
                            {item.type}
                          </Badge>
                          <span className="text-xs text-gray-500">{item.duration}min</span>
                          <span className="text-xs text-gray-500">₹{item.cost}</span>
                        </div>
                        {item.validation?.message && (
                          <p className="text-xs text-amber-600 mt-1">{item.validation.message}</p>
                        )}
                      </div>
                    ))}
                  </div>
                </ScrollArea>
              </div>
            )}
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