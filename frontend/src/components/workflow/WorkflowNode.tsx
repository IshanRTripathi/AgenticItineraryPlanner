import React from 'react';
import { Handle, Position, NodeProps } from 'reactflow';
import { Badge } from '../ui/badge';
import { 
  MapPin, 
  Utensils, 
  Car, 
  Bed, 
  Clock, 
  Info,
  AlertTriangle,
  CheckCircle,
  DollarSign,
  Star,
  Lock,
  Edit3,
  Plus,
  Move,
  Trash2
} from 'lucide-react';
import { WorkflowNodeData } from '../WorkflowBuilder';
import { NodeLockToggle } from '../locks/NodeLockToggle';

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

const getNodeColor = (type: WorkflowNodeData['type']) => {
  const colorMap = {
    Attraction: 'bg-blue-500',
    Meal: 'bg-green-500',
    Transit: 'bg-yellow-500',
    Hotel: 'bg-purple-500',
    FreeTime: 'bg-gray-500',
    Decision: 'bg-orange-500',
  };
  return colorMap[type];
};

const getValidationIcon = (status?: 'valid' | 'warning' | 'error') => {
  switch (status) {
    case 'valid':
      return <CheckCircle className="h-4 w-4 text-green-500" />;
    case 'warning':
      return <AlertTriangle className="h-4 w-4 text-amber-500" />;
    case 'error':
      return <AlertTriangle className="h-4 w-4 text-red-500" />;
    default:
      return null;
  }
};

const getChangeIcon = (changeType?: 'added' | 'modified' | 'moved' | 'deleted') => {
  switch (changeType) {
    case 'added':
      return <Plus className="h-3 w-3" />;
    case 'modified':
      return <Edit3 className="h-3 w-3" />;
    case 'moved':
      return <Move className="h-3 w-3" />;
    case 'deleted':
      return <Trash2 className="h-3 w-3" />;
    default:
      return <Edit3 className="h-3 w-3" />;
  }
};

const getChangeColor = (changeType?: 'added' | 'modified' | 'moved' | 'deleted') => {
  switch (changeType) {
    case 'added':
      return 'bg-green-500 text-white';
    case 'modified':
      return 'bg-blue-500 text-white';
    case 'moved':
      return 'bg-orange-500 text-white';
    case 'deleted':
      return 'bg-red-500 text-white';
    default:
      return 'bg-blue-500 text-white';
  }
};

export function WorkflowNode({ data, selected }: NodeProps<WorkflowNodeData>) {
  const Icon = getNodeIcon(data.type);
  const nodeColor = getNodeColor(data.type);
  const validationIcon = getValidationIcon(data.validation?.status);
  const changeIcon = getChangeIcon(data.changeType);
  const changeColor = getChangeColor(data.changeType);

  const formatTime = (minutes: number) => {
    const hours = Math.floor(minutes / 60);
    const mins = minutes % 60;
    if (hours > 0) {
      return `${hours}h ${mins}m`;
    }
    return `${mins}m`;
  };

  return (
    <div 
      className={`
        relative bg-white rounded-lg shadow-lg border-2 transition-all duration-200 min-w-[200px] max-w-[250px]
        ${selected ? 'border-blue-500 shadow-xl' : 'border-gray-200 hover:border-gray-300'}
        ${data.validation?.status === 'error' ? 'border-red-300' : ''}
        ${data.validation?.status === 'warning' ? 'border-amber-300' : ''}
        ${data.isLocked ? 'border-red-400 border-4' : ''}
        ${data.userModified ? 'ring-2 ring-blue-400 ring-opacity-50' : ''}
      `}
    >
      {/* Input Handle */}
      <Handle
        type="target"
        position={Position.Left}
        className="w-3 h-3 !bg-gray-400 border-2 border-white"
      />
      
      {/* Header with Icon and Type */}
      <div className={`${nodeColor} text-white px-3 py-2 rounded-t-lg flex items-center justify-between`}>
        <div className="flex items-center gap-2">
          <Icon className="h-4 w-4" />
          <span className="text-sm font-medium">{data.type}</span>
        </div>
        <div className="flex items-center gap-1">
          {validationIcon}
          {data.userModified && (
            <div className={`${changeColor} rounded-full p-1`} title={`User ${data.changeType || 'modified'}`}>
              {changeIcon}
            </div>
          )}
        </div>
      </div>
      
      {/* Content */}
      <div className="p-3">
        <h3 className="font-semibold text-sm mb-2 line-clamp-2">{data.title}</h3>
        
        {/* Time and Duration */}
        <div className="flex items-center gap-2 mb-2">
          <Clock className="h-3 w-3 text-gray-500" />
          <span className="text-xs text-gray-600">{data.start}</span>
          <Badge variant="outline" className="text-xs">
            {formatTime(data.durationMin)}
          </Badge>
        </div>
        
        {/* Cost */}
        <div className="flex items-center gap-2 mb-2">
          <DollarSign className="h-3 w-3 text-gray-500" />
          <span className="text-xs text-gray-600">₹{data.costINR.toLocaleString()}</span>
          {data.meta.rating && (
            <div className="flex items-center gap-1 ml-auto">
              <Star className="h-3 w-3 text-yellow-500 fill-current" />
              <span className="text-xs text-gray-600">{data.meta.rating}</span>
            </div>
          )}
        </div>
        
        {/* Tags */}
        <div className="flex flex-wrap gap-1">
          {data.tags.slice(0, 3).map((tag, index) => (
            <Badge 
              key={index} 
              variant="secondary" 
              className="text-xs px-1 py-0 h-5"
            >
              {tag}
            </Badge>
          ))}
          {data.tags.length > 3 && (
            <Badge variant="secondary" className="text-xs px-1 py-0 h-5">
              +{data.tags.length - 3}
            </Badge>
          )}
        </div>
        
        {/* User Change Indicator */}
        {data.userModified && (
          <div className="mt-2 p-2 rounded text-xs bg-blue-50 border border-blue-200">
            <div className="flex items-center gap-1 text-blue-700">
              {changeIcon}
              <span className="font-medium">User {data.changeType || 'modified'}</span>
              {data.changeTimestamp && (
                <span className="text-blue-500 ml-auto">
                  {new Date(data.changeTimestamp).toLocaleTimeString()}
                </span>
              )}
            </div>
          </div>
        )}
        
        {/* Validation Message */}
        {data.validation?.message && data.validation.status !== 'valid' && (
          <div className="mt-2 p-2 rounded text-xs bg-gray-50 border">
            <div className={`${
              data.validation.status === 'error' ? 'text-red-600' : 'text-amber-600'
            }`}>
              {data.validation.message}
            </div>
          </div>
        )}
      </div>
      
      {/* Lock Toggle */}
      {data.itineraryId && (
        <div className="absolute -top-2 -right-2 z-10">
          <NodeLockToggle
            nodeId={data.id}
            itineraryId={data.itineraryId}
            isLocked={data.isLocked || false}
            size="sm"
            variant="icon"
          />
        </div>
      )}
      
      {/* Locked Indicator */}
      {data.isLocked && (
        <div className="absolute top-2 left-2 bg-red-500 text-white p-1 rounded-full">
          <Lock className="w-3 h-3" />
        </div>
      )}
      
      {/* Output Handle */}
      <Handle
        type="source"
        position={Position.Right}
        className="w-3 h-3 !bg-gray-400 border-2 border-white"
      />
      
      {/* Selection Indicator */}
      {selected && (
        <div className="absolute -top-1 -right-1 w-3 h-3 bg-blue-500 rounded-full border-2 border-white" />
      )}
    </div>
  );
}