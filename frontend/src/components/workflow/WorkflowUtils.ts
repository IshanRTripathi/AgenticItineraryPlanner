import { Node, Edge } from 'reactflow';
import { WorkflowNodeData } from '../WorkflowBuilder';

export interface ValidationResult {
  status: 'valid' | 'warning' | 'error';
  message?: string;
}

export const validateWorkflowNode = (
  node: Node<WorkflowNodeData>,
  allNodes: Node<WorkflowNodeData>[]
): ValidationResult => {
  const data = node.data;
  
  // Check if activity is outside opening hours
  if (data.meta.open !== '24/7') {
    const activityStart = new Date(`2000-01-01T${data.start}`);
    const openTime = new Date(`2000-01-01T${data.meta.open}`);
    const closeTime = new Date(`2000-01-01T${data.meta.close}`);
    
    if (activityStart < openTime || activityStart > closeTime) {
      return {
        status: 'warning',
        message: `Activity outside opening hours (${data.meta.open} - ${data.meta.close})`,
      };
    }
  }
  
  // Check for excessive distance
  if (data.meta.distanceKm && data.meta.distanceKm > 10) {
    return {
      status: 'error',
      message: `Distance too far (${data.meta.distanceKm}km)`,
    };
  }
  
  // Check for total day duration (>10 hours)
  const dayNodes = allNodes.filter(n => n.id && node.id && n.id.includes(node.id.split('-')[0]));
  const totalDuration = dayNodes.reduce((sum, n) => sum + (n.data.durationMin || 0), 0);
  
  if (totalDuration > 600) { // 10 hours
    return {
      status: 'warning',
      message: `Day is overbooked (${Math.round(totalDuration / 60)}h total)`,
    };
  }
  
  return { status: 'valid' };
};

export const calculateNodePosition = (
  index: number,
  totalNodes: number,
  canvasWidth: number = 800,
  canvasHeight: number = 600
): { x: number; y: number } => {
  const cols = Math.ceil(Math.sqrt(totalNodes));
  const rows = Math.ceil(totalNodes / cols);
  
  const col = index % cols;
  const row = Math.floor(index / cols);
  
  const xSpacing = canvasWidth / (cols + 1);
  const ySpacing = canvasHeight / (rows + 1);
  
  return {
    x: xSpacing * (col + 1),
    y: ySpacing * (row + 1),
  };
};

export const autoArrangeNodes = (
  nodes: Node<WorkflowNodeData>[]
): Node<WorkflowNodeData>[] => {
  // Sort nodes by start time
  const sortedNodes = [...nodes].sort((a, b) => 
    a.data.start.localeCompare(b.data.start)
  );
  
  return sortedNodes.map((node, index) => ({
    ...node,
    position: {
      x: 150 + index * 280,
      y: 200 + (index % 2) * 180,
    },
  }));
};

export const generateTimelineFromNodes = (
  nodes: Node<WorkflowNodeData>[]
): Array<{
  time: string;
  title: string;
  type: string;
  duration: number;
  cost: number;
  validation?: ValidationResult;
}> => {
  return nodes
    .sort((a, b) => a.data.start.localeCompare(b.data.start))
    .map(node => ({
      time: node.data.start,
      title: node.data.title,
      type: node.data.type,
      duration: node.data.durationMin,
      cost: node.data.costINR,
      validation: node.data.validation,
    }));
};

export const formatDuration = (minutes: number): string => {
  const hours = Math.floor(minutes / 60);
  const mins = minutes % 60;
  
  if (hours > 0) {
    return mins > 0 ? `${hours}h ${mins}m` : `${hours}h`;
  }
  return `${mins}m`;
};

export const formatCurrency = (amount: number): string => {
  return `₹${amount.toLocaleString('en-IN')}`;
};

export const getNodeTypeColor = (type: WorkflowNodeData['type']): string => {
  const colorMap = {
    Attraction: 'blue',
    Meal: 'green',
    Transit: 'yellow',
    Hotel: 'purple',
    FreeTime: 'gray',
    Decision: 'orange',
  };
  return colorMap[type];
};

export const createNewNode = (
  type: WorkflowNodeData['type'],
  dayIndex: number,
  position: { x: number; y: number }
): Node<WorkflowNodeData> => {
  const id = `day${dayIndex + 1}-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
  
  const defaultData: Record<WorkflowNodeData['type'], Partial<WorkflowNodeData>> = {
    Attraction: {
      title: 'New Attraction',
      tags: ['sightseeing', 'culture'],
      durationMin: 120,
      costINR: 500,
    },
    Meal: {
      title: 'New Restaurant',
      tags: ['dining', 'local'],
      durationMin: 60,
      costINR: 800,
    },
    Transit: {
      title: 'Transportation',
      tags: ['transport'],
      durationMin: 30,
      costINR: 200,
    },
    Hotel: {
      title: 'Hotel Stay',
      tags: ['accommodation'],
      durationMin: 480, // 8 hours
      costINR: 0,
    },
    FreeTime: {
      title: 'Free Time',
      tags: ['rest', 'flexible'],
      durationMin: 60,
      costINR: 0,
    },
    Decision: {
      title: 'Decision Point',
      tags: ['choice', 'branching'],
      durationMin: 0,
      costINR: 0,
    },
  };

  return {
    id,
    type: 'workflow',
    position,
    data: {
      id,
      type,
      title: defaultData[type].title || 'New Activity',
      tags: defaultData[type].tags || [],
      start: '12:00',
      durationMin: defaultData[type].durationMin || 60,
      costINR: defaultData[type].costINR || 0,
      meta: {
        rating: 4.0,
        open: '09:00',
        close: '18:00',
        address: 'New Location',
      },
      ...defaultData[type],
    },
  };
};