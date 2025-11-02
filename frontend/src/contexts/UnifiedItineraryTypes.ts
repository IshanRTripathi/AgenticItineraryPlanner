import { NormalizedDay, NormalizedNode } from '../types/NormalizedItinerary';
import { TripData } from '../types/TripData';

/**
 * Type definitions for the Unified Itinerary Context
 * Extracted from UnifiedItineraryContext.tsx for better maintainability
 */

export interface ChatMessage {
  id: string;
  text: string;
  sender: 'user' | 'assistant';
  timestamp: Date;
  selectedNodeId?: string;
  data?: any;
  type?: 'text' | 'itinerary_update' | 'booking_confirmation' | 'error';
  intent?: string;
  changeSet?: any;
  diff?: any;
  warnings?: string[];
  errors?: string[];
  applied?: boolean;
  candidates?: any[];
}

export interface WorkflowNode {
  id: string;
  type: 'day' | 'activity' | 'transport' | 'accommodation' | 'dining' | 'custom';
  position: { x: number; y: number };
  data: any;
}

export interface WorkflowEdge {
  id: string;
  source: string;
  target: string;
  type?: string;
}

export interface WorkflowSettings {
  autoLayout: boolean;
  showMinimap: boolean;
  snapToGrid: boolean;
  gridSize: number;
  theme: 'light' | 'dark';
  nodePositions?: Record<string, { x: number; y: number }>;
}

export interface AgentDataSection {
  location?: any;
  activities?: any;
  dining?: any;
  transport?: any;
  booking?: any;
  photos?: any;
}

export interface ChangeDetail {
  id: string;
  type: 'insert' | 'update' | 'delete' | 'move' | 'replace';
  scope: 'trip' | 'day' | 'node' | 'metadata';
  target: string;
  description: string;
  timestamp: Date;
  data?: any;
  oldValue?: any;
  newValue?: any;
}

export interface RevisionInfo {
  id: string;
  itineraryId: string;
  version: string;
  description: string;
  createdAt: Date;
  createdBy: string;
  changes: ChangeDetail[];
}

/**
 * Unified state interface for the entire itinerary system
 * Now uses TripData as the primary format for frontend compatibility
 */
export interface UnifiedItineraryState {
  // Core itinerary data - using TripData for frontend compatibility
  itinerary: TripData | null;
  loading: boolean;
  error: string | null;
  
  // Chat system
  chatMessages: ChatMessage[];
  chatLoading: boolean;
  chatError: string | null;
  
  // Workflow system
  workflowNodes: WorkflowNode[];
  workflowEdges: WorkflowEdge[];
  workflowSettings: WorkflowSettings;
  selectedNodeId: string | null;
  
  // Agent system
  agentData: Record<string, AgentDataSection>;
  activeAgents: string[];
  agentProgress: Record<string, number>;
  
  // Revision system
  revisions: RevisionInfo[];
  currentRevision: string | null;
  pendingChanges: ChangeDetail[];
  
  // UI state
  selectedDay: number;
  selectedNodeIds: string[];
  viewMode: 'day-by-day' | 'workflow' | 'timeline';
  sidebarOpen: boolean;
  
  // Real-time sync
  isConnected: boolean;
  lastSyncTime: Date | null;
  syncStatus: 'idle' | 'syncing' | 'error';
  
  // Generation tracking
  currentPhase: string | null;
}

/**
 * Action types for the unified reducer
 */
export type UnifiedItineraryAction =
  // Itinerary actions
  | { type: 'SET_LOADING'; payload: boolean }
  | { type: 'SET_ERROR'; payload: string | null }
  | { type: 'SET_ITINERARY'; payload: TripData }
  | { type: 'SET_CURRENT_PHASE'; payload: string | null }
  | { type: 'UPDATE_DAY'; payload: { dayIndex: number; day: NormalizedDay } }
  | { type: 'UPDATE_NODE'; payload: { dayIndex: number; nodeIndex: number; node: NormalizedNode } }
  | { type: 'ADD_NODE'; payload: { dayIndex: number; node: NormalizedNode; position?: number } }
  | { type: 'REMOVE_NODE'; payload: { dayIndex: number; nodeIndex: number } }
  | { type: 'MOVE_NODE'; payload: { fromDay: number; fromIndex: number; toDay: number; toIndex: number } }
  
  // Chat actions
  | { type: 'ADD_CHAT_MESSAGE'; payload: ChatMessage }
  | { type: 'SET_CHAT_LOADING'; payload: boolean }
  | { type: 'SET_CHAT_ERROR'; payload: string | null }
  | { type: 'CLEAR_CHAT_MESSAGES' }
  
  // Workflow actions
  | { type: 'SET_WORKFLOW_NODES'; payload: WorkflowNode[] }
  | { type: 'SET_WORKFLOW_EDGES'; payload: WorkflowEdge[] }
  | { type: 'UPDATE_WORKFLOW_SETTINGS'; payload: Partial<WorkflowSettings> }
  | { type: 'SELECT_WORKFLOW_NODE'; payload: string | null }
  
  // Agent actions
  | { type: 'SET_AGENT_DATA'; payload: Record<string, AgentDataSection> }
  | { type: 'UPDATE_AGENT_PROGRESS'; payload: { agentId: string; progress: number } }
  | { type: 'SET_ACTIVE_AGENTS'; payload: string[] }
  
  // Revision actions
  | { type: 'SET_REVISIONS'; payload: RevisionInfo[] }
  | { type: 'SET_CURRENT_REVISION'; payload: string }
  | { type: 'ADD_PENDING_CHANGE'; payload: ChangeDetail }
  | { type: 'CLEAR_PENDING_CHANGES' }
  
  // UI actions
  | { type: 'SET_SELECTED_DAY'; payload: number }
  | { type: 'SET_SELECTED_NODES'; payload: string[] }
  | { type: 'SET_VIEW_MODE'; payload: 'day-by-day' | 'workflow' | 'timeline' }
  | { type: 'SET_SIDEBAR_OPEN'; payload: boolean }
  
  // Sync actions
  | { type: 'SET_CONNECTION_STATUS'; payload: boolean }
  | { type: 'SET_SYNC_STATUS'; payload: 'idle' | 'syncing' | 'error' }
  | { type: 'SET_LAST_SYNC_TIME'; payload: Date };

/**
 * Context interface for the unified itinerary system
 */
export interface UnifiedItineraryContextType {
  // State
  state: UnifiedItineraryState;
  
  // Core itinerary actions
  loadItinerary: (itineraryId: string) => Promise<void>;
  saveItinerary: () => Promise<void>;
  updateDay: (dayIndex: number, day: NormalizedDay) => void;
  updateNode: (dayIndex: number, nodeIndex: number, node: NormalizedNode) => void;
  addNode: (dayIndex: number, node: NormalizedNode, position?: number) => void;
  removeNode: (dayIndex: number, nodeIndex: number) => void;
  moveNode: (fromDay: number, fromIndex: number, toDay: number, toIndex: number) => void;
  
  // Chat actions
  sendChatMessage: (message: string, selectedNodeId?: string) => Promise<void>;
  clearChatMessages: () => void;
  clearChatHistory: () => Promise<void>;
  
  // Workflow actions
  updateWorkflowSettings: (settings: Partial<WorkflowSettings>) => void;
  selectWorkflowNode: (nodeId: string | null) => void;
  
  // Agent actions
  executeAgent: (agentType: string, data: any) => Promise<void>;
  processWithAgents: (nodeId: string, agentIds: string[]) => Promise<void>;
  
  // Revision actions
  loadRevisions: () => Promise<void>;
  switchToRevision: (revisionId: string) => Promise<void>;
  createRevision: (description: string) => Promise<void>;
  rollbackToRevision: (revisionId: string) => Promise<void>;
  
  // UI actions
  setSelectedDay: (day: number) => void;
  setSelectedNodes: (nodeIds: string[]) => void;
  setViewMode: (mode: 'day-by-day' | 'workflow' | 'timeline') => void;
  setSidebarOpen: (open: boolean) => void;
  
  // Utility functions
  getSelectedNodes: () => NormalizedNode[];
  getCurrentDay: () => NormalizedDay | null;
  hasUnsavedChanges: () => boolean;
}
