import React, { useState, useCallback } from 'react';
import { Card } from './ui/card';
import { Button } from './ui/button';
import { Badge } from './ui/badge';
import { ScrollArea } from './ui/scroll-area';
import { Separator } from './ui/separator';
import { 
  MapPin, 
  Clock, 
  DollarSign, 
  Car, 
  Utensils, 
  Camera, 
  ShoppingBag,
  Plane,
  Building,
  Palette,
  TreePine,
  Coffee,
  Plus,
  Trash2,
  Edit3,
  Move3D,
  ZoomIn,
  ZoomOut,
  RotateCcw,
  Save
} from 'lucide-react';
import { TripComponent, DayPlan } from '../types/TripData';

interface WorkflowBuilderPanelProps {
  dayPlans: DayPlan[];
  selectedDay: number;
  onDaySelect: (day: number) => void;
  onComponentUpdate: (dayIndex: number, componentId: string, updates: Partial<TripComponent>) => void;
  onComponentDelete: (dayIndex: number, componentId: string) => void;
  onComponentAdd: (dayIndex: number, component: TripComponent) => void;
  onSave: () => void;
}

interface WorkflowNode {
  id: string;
  component: TripComponent;
  position: { x: number; y: number };
  connections: string[];
}

const getTypeIcon = (type: TripComponent['type']) => {
  switch (type) {
    case 'attraction': return <MapPin className="w-4 h-4" />;
    case 'hotel': return <Building className="w-4 h-4" />;
    case 'restaurant': return <Utensils className="w-4 h-4" />;
    case 'activity': return <Palette className="w-4 h-4" />;
    case 'transport': return <Car className="w-4 h-4" />;
    case 'shopping': return <ShoppingBag className="w-4 h-4" />;
    case 'entertainment': return <Camera className="w-4 h-4" />;
    default: return <MapPin className="w-4 h-4" />;
  }
};

const getTypeColor = (type: TripComponent['type']) => {
  switch (type) {
    case 'attraction': return 'bg-blue-100 border-blue-300 text-blue-700';
    case 'hotel': return 'bg-purple-100 border-purple-300 text-purple-700';
    case 'restaurant': return 'bg-orange-100 border-orange-300 text-orange-700';
    case 'activity': return 'bg-green-100 border-green-300 text-green-700';
    case 'transport': return 'bg-gray-100 border-gray-300 text-gray-700';
    case 'shopping': return 'bg-pink-100 border-pink-300 text-pink-700';
    case 'entertainment': return 'bg-red-100 border-red-300 text-red-700';
    default: return 'bg-blue-100 border-blue-300 text-blue-700';
  }
};

export function WorkflowBuilderPanel({ 
  dayPlans, 
  selectedDay, 
  onDaySelect, 
  onComponentUpdate,
  onComponentDelete,
  onComponentAdd,
  onSave 
}: WorkflowBuilderPanelProps) {
  const [zoom, setZoom] = useState(100);
  const [selectedNode, setSelectedNode] = useState<string | null>(null);
  const [dragOffset, setDragOffset] = useState<{ x: number; y: number } | null>(null);
  
  const currentDay = dayPlans[selectedDay] || null;
  
  const [nodes, setNodes] = useState<WorkflowNode[]>(
    currentDay?.components?.map((comp, index) => ({
      id: comp.id,
      component: comp,
      position: { 
        x: 100 + (index % 3) * 200, 
        y: 100 + Math.floor(index / 3) * 150 
      },
      connections: index < (currentDay?.components?.length || 0) - 1 ? [currentDay?.components?.[index + 1]?.id || ''] : []
    })) || []
  );

  const handleNodeDrag = useCallback((nodeId: string, newPosition: { x: number; y: number }) => {
    setNodes(prev => prev.map(node => 
      node.id === nodeId 
        ? { ...node, position: newPosition }
        : node
    ));
  }, []);

  const handleNodeClick = (nodeId: string) => {
    setSelectedNode(selectedNode === nodeId ? null : nodeId);
  };

  const handleDeleteNode = (nodeId: string) => {
    const nodeIndex = nodes.findIndex(n => n.id === nodeId);
    if (nodeIndex !== -1) {
      onComponentDelete(selectedDay, nodeId);
      setNodes(prev => prev.filter(n => n.id !== nodeId));
    }
  };

  const handleZoomIn = () => setZoom(prev => Math.min(prev + 25, 200));
  const handleZoomOut = () => setZoom(prev => Math.max(prev - 25, 50));
  const handleReset = () => {
    setZoom(100);
    setSelectedNode(null);
  };

  const formatTime = (timeStr: string) => {
    try {
      const date = new Date(timeStr);
      return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    } catch {
      return timeStr;
    }
  };

  const formatDuration = (minutes: number) => {
    const hours = Math.floor(minutes / 60);
    const mins = minutes % 60;
    return hours > 0 ? `${hours}h ${mins}m` : `${mins}m`;
  };

  return (
    <div className="h-full flex flex-col">
      {/* Header */}
      <div className="p-4 border-b">
        <div className="flex items-center justify-between mb-3">
          <h2 className="text-lg font-semibold">Workflow Builder</h2>
          <div className="flex items-center gap-2">
            <Button variant="outline" size="sm" onClick={handleZoomOut}>
              <ZoomOut className="w-4 h-4" />
            </Button>
            <span className="text-sm font-medium min-w-[4rem] text-center">{zoom}%</span>
            <Button variant="outline" size="sm" onClick={handleZoomIn}>
              <ZoomIn className="w-4 h-4" />
            </Button>
            <Button variant="outline" size="sm" onClick={handleReset}>
              <RotateCcw className="w-4 h-4" />
            </Button>
            <Button size="sm" onClick={onSave}>
              <Save className="w-4 h-4 mr-1" />
              Save
            </Button>
          </div>
        </div>
        
        {/* Day Tabs */}
        <div className="flex gap-1 overflow-x-auto">
          {dayPlans.map((day, index) => (
            <Button
              key={day.id}
              variant={selectedDay === index ? "default" : "outline"}
              size="sm"
              onClick={() => onDaySelect(index)}
              className="whitespace-nowrap"
            >
              Day {day.dayNumber}
            </Button>
          ))}
        </div>
      </div>

      {/* Canvas */}
      <div className="flex-1 flex">
        <div className="flex-1 relative overflow-hidden bg-gradient-to-br from-slate-50 to-slate-100">
          <ScrollArea className="h-full">
            <div 
              className="relative min-h-full"
              style={{ 
                transform: `scale(${zoom / 100})`,
                transformOrigin: 'top left',
                minWidth: `${100 * (200 / zoom)}%`,
                minHeight: `${100 * (200 / zoom)}%`
              }}
            >
              {/* Grid Background */}
              <div 
                className="absolute inset-0 opacity-20"
                style={{
                  backgroundImage: `
                    linear-gradient(to right, #e2e8f0 1px, transparent 1px),
                    linear-gradient(to bottom, #e2e8f0 1px, transparent 1px)
                  `,
                  backgroundSize: '20px 20px'
                }}
              />

              {/* Connection Lines */}
              {nodes.map(node => 
                node.connections.map(targetId => {
                  const targetNode = nodes.find(n => n.id === targetId);
                  if (!targetNode) return null;
                  
                  return (
                    <svg
                      key={`${node.id}-${targetId}`}
                      className="absolute inset-0 pointer-events-none"
                      style={{ zIndex: 1 }}
                    >
                      <line
                        x1={node.position.x + 100}
                        y1={node.position.y + 50}
                        x2={targetNode.position.x + 100}
                        y2={targetNode.position.y + 50}
                        stroke="#3b82f6"
                        strokeWidth="2"
                        strokeDasharray="5,5"
                        markerEnd="url(#arrowhead)"
                      />
                      <defs>
                        <marker
                          id="arrowhead"
                          markerWidth="10"
                          markerHeight="7"
                          refX="9"
                          refY="3.5"
                          orient="auto"
                        >
                          <polygon
                            points="0 0, 10 3.5, 0 7"
                            fill="#3b82f6"
                          />
                        </marker>
                      </defs>
                    </svg>
                  );
                })
              )}

              {/* Workflow Nodes */}
              {nodes.map(node => (
                <div
                  key={node.id}
                  className={`absolute w-48 cursor-move transition-all ${
                    selectedNode === node.id ? 'ring-2 ring-blue-500 shadow-lg' : 'shadow-md'
                  }`}
                  style={{
                    left: node.position.x,
                    top: node.position.y,
                    zIndex: selectedNode === node.id ? 10 : 2
                  }}
                  onMouseDown={(e) => {
                    const rect = e.currentTarget.getBoundingClientRect();
                    setDragOffset({
                      x: e.clientX - rect.left,
                      y: e.clientY - rect.top
                    });
                    handleNodeClick(node.id);
                  }}
                  onMouseMove={(e) => {
                    if (dragOffset && selectedNode === node.id) {
                      const canvas = e.currentTarget.parentElement;
                      if (canvas) {
                        const canvasRect = canvas.getBoundingClientRect();
                        const newX = (e.clientX - canvasRect.left - dragOffset.x) * (100 / zoom);
                        const newY = (e.clientY - canvasRect.top - dragOffset.y) * (100 / zoom);
                        handleNodeDrag(node.id, { x: Math.max(0, newX), y: Math.max(0, newY) });
                      }
                    }
                  }}
                  onMouseUp={() => setDragOffset(null)}
                >
                  <Card className={`p-3 ${getTypeColor(node.component.type)} hover:shadow-lg transition-shadow`}>
                    <div className="flex items-start justify-between mb-2">
                      <div className="flex items-center gap-2">
                        {getTypeIcon(node.component.type)}
                        <Badge variant="secondary" className="text-xs">
                          {node.component.type}
                        </Badge>
                      </div>
                      {selectedNode === node.id && (
                        <Button
                          variant="destructive"
                          size="sm"
                          className="h-6 w-6 p-0"
                          onClick={(e) => {
                            e.stopPropagation();
                            handleDeleteNode(node.id);
                          }}
                        >
                          <Trash2 className="w-3 h-3" />
                        </Button>
                      )}
                    </div>
                    
                    <h4 className="font-medium text-sm mb-1 line-clamp-2">
                      {node.component.name}
                    </h4>
                    
                    <div className="space-y-1 text-xs">
                      <div className="flex items-center gap-1">
                        <Clock className="w-3 h-3" />
                        <span>
                          {formatTime(node.component.timing.startTime)} - {formatTime(node.component.timing.endTime)}
                        </span>
                      </div>
                      
                      <div className="flex items-center gap-1">
                        <DollarSign className="w-3 h-3" />
                        <span>{node.component.cost?.currency || 'USD'} {node.component.cost?.pricePerPerson || 0}/person</span>
                      </div>
                      
                      {(node.component.travel?.distanceFromPrevious || 0) > 0 && (
                        <div className="flex items-center gap-1">
                          <Car className="w-3 h-3" />
                          <span>{node.component.travel?.distanceFromPrevious || 0}km • {formatDuration(node.component.travel?.travelTimeFromPrevious || 0)}</span>
                        </div>
                      )}
                    </div>

                    {node.component.userNotes && (
                      <div className="mt-2 p-2 bg-yellow-50 rounded text-xs">
                        <strong>Note:</strong> {node.component.userNotes}
                      </div>
                    )}
                  </Card>
                </div>
              ))}

              {/* Add Node Button */}
              <div
                className="absolute w-48 h-24 border-2 border-dashed border-gray-300 rounded-lg flex items-center justify-center cursor-pointer hover:border-gray-400 hover:bg-gray-50 transition-colors"
                style={{
                  left: 100 + (nodes.length % 3) * 200,
                  top: 100 + Math.floor(nodes.length / 3) * 150,
                  zIndex: 1
                }}
                onClick={() => {
                  // This would open a component picker dialog
                  console.log('Add new component');
                }}
              >
                <div className="text-center">
                  <Plus className="w-6 h-6 mx-auto mb-1 text-gray-400" />
                  <span className="text-sm text-gray-500">Add Component</span>
                </div>
              </div>
            </div>
          </ScrollArea>
        </div>

        {/* Side Panel */}
        {selectedNode && (
          <div className="w-80 border-l bg-white">
            <div className="p-4">
              <div className="flex items-center justify-between mb-4">
                <h3 className="font-semibold">Edit Component</h3>
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={() => setSelectedNode(null)}
                >
                  <Edit3 className="w-4 h-4" />
                </Button>
              </div>

              {(() => {
                const node = nodes.find(n => n.id === selectedNode);
                if (!node) return null;

                return (
                  <ScrollArea className="h-[calc(100vh-12rem)]">
                    <div className="space-y-4">
                      <div>
                        <label className="text-sm font-medium">Name</label>
                        <input 
                          type="text"
                          className="w-full mt-1 p-2 border rounded"
                          value={node.component.name}
                          onChange={(e) => {
                            const updatedNodes = nodes.map(n => 
                              n.id === selectedNode 
                                ? { ...n, component: { ...n.component, name: e.target.value } }
                                : n
                            );
                            setNodes(updatedNodes);
                          }}
                        />
                      </div>

                      <div>
                        <label className="text-sm font-medium">Type</label>
                        <select 
                          className="w-full mt-1 p-2 border rounded"
                          value={node.component.type}
                          onChange={(e) => {
                            const updatedNodes = nodes.map(n => 
                              n.id === selectedNode 
                                ? { ...n, component: { ...n.component, type: e.target.value as TripComponent['type'] } }
                                : n
                            );
                            setNodes(updatedNodes);
                          }}
                        >
                          <option value="attraction">Attraction</option>
                          <option value="hotel">Hotel</option>
                          <option value="restaurant">Restaurant</option>
                          <option value="activity">Activity</option>
                          <option value="transport">Transport</option>
                          <option value="shopping">Shopping</option>
                          <option value="entertainment">Entertainment</option>
                        </select>
                      </div>

                      <div className="grid grid-cols-2 gap-2">
                        <div>
                          <label className="text-sm font-medium">Start Time</label>
                          <input 
                            type="time"
                            className="w-full mt-1 p-2 border rounded text-sm"
                            value={formatTime(node.component.timing.startTime)}
                            onChange={(e) => {
                              // Handle time update
                            }}
                          />
                        </div>
                        <div>
                          <label className="text-sm font-medium">End Time</label>
                          <input 
                            type="time"
                            className="w-full mt-1 p-2 border rounded text-sm"
                            value={formatTime(node.component.timing.endTime)}
                            onChange={(e) => {
                              // Handle time update
                            }}
                          />
                        </div>
                      </div>

                      <div className="grid grid-cols-2 gap-2">
                        <div>
                          <label className="text-sm font-medium">Price per Person</label>
                          <input 
                            type="number"
                            className="w-full mt-1 p-2 border rounded"
                            value={node.component.cost?.pricePerPerson || 0}
                            onChange={(e) => {
                              const updatedNodes = nodes.map(n => 
                                n.id === selectedNode 
                                  ? { 
                                      ...n, 
                                      component: { 
                                        ...n.component, 
                                        cost: { 
                                          ...n.component.cost, 
                                          pricePerPerson: Number(e.target.value) 
                                        } 
                                      } 
                                    }
                                  : n
                              );
                              setNodes(updatedNodes);
                            }}
                          />
                        </div>
                        <div>
                          <label className="text-sm font-medium">Currency</label>
                          <select 
                            className="w-full mt-1 p-2 border rounded"
                            value={node.component.cost?.currency || 'USD'}
                          >
                            <option value="USD">USD</option>
                            <option value="EUR">EUR</option>
                            <option value="GBP">GBP</option>
                          </select>
                        </div>
                      </div>

                      <div>
                        <label className="text-sm font-medium">Notes</label>
                        <textarea 
                          className="w-full mt-1 p-2 border rounded text-sm"
                          rows={3}
                          value={node.component.userNotes || ''}
                          onChange={(e) => {
                            const updatedNodes = nodes.map(n => 
                              n.id === selectedNode 
                                ? { ...n, component: { ...n.component, userNotes: e.target.value } }
                                : n
                            );
                            setNodes(updatedNodes);
                          }}
                          placeholder="Add any special notes or instructions..."
                        />
                      </div>

                      <Separator />

                      <div className="space-y-2">
                        <h4 className="font-medium text-sm">Location Details</h4>
                        <p className="text-sm text-gray-600">{node.component.location.address}</p>
                        <div className="flex items-center gap-2 text-sm">
                          <MapPin className="w-3 h-3" />
                          <span>{node.component.location.coordinates.lat.toFixed(4)}, {node.component.location.coordinates.lng.toFixed(4)}</span>
                        </div>
                      </div>

                      <div className="space-y-2">
                        <h4 className="font-medium text-sm">Travel Info</h4>
                        <div className="text-sm space-y-1">
                          <p>Distance from previous: {node.component.travel?.distanceFromPrevious || 0}km</p>
                          <p>Travel time: {formatDuration(node.component.travel?.travelTimeFromPrevious || 0)}</p>
                          <p>Transport: {node.component.travel?.transportMode || 'walking'}</p>
                          <p>Transport cost: {node.component.cost?.currency || 'USD'} {node.component.travel?.transportCost || 0}</p>
                        </div>
                      </div>
                    </div>
                  </ScrollArea>
                );
              })()}
            </div>
          </div>
        )}
      </div>

      {/* Footer */}
      <div className="p-4 border-t bg-gray-50">
        <div className="flex items-center justify-between text-sm">
          <div className="flex items-center gap-4">
            <span>Day {currentDay?.dayNumber} - {currentDay?.location}</span>
            <span>{currentDay?.components.length || 0} components</span>
            <span>Total: €{currentDay?.totalCost || 0}</span>
          </div>
          <div className="text-gray-500">
            Drag components to reorder • Click to edit • Delete with trash icon
          </div>
        </div>
      </div>
    </div>
  );
}