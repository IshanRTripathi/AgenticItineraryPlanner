import { Node, Edge } from 'reactflow';
import { TripData } from '../../types/TripData';

/**
 * Workflow node data structure
 */
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
  // User change tracking
  userModified?: boolean;
  changeType?: 'added' | 'modified' | 'moved' | 'deleted';
  changeTimestamp?: string;
  originalData?: Partial<WorkflowNodeData>;
  // Lock state
  isLocked?: boolean;
  itineraryId?: string;
}

/**
 * Workflow day structure containing nodes and edges
 */
export interface WorkflowDay {
  day: number;
  date: string;
  nodes: Node<WorkflowNodeData>[];
  edges: Edge[];
}

/**
 * Props for WorkflowBuilder component
 */
export interface WorkflowBuilderProps {
  tripData: TripData;
  onSave: (updatedItinerary: any) => void;
  onCancel: () => void;
  embedded?: boolean; // When true, removes full-screen layout and header
}
