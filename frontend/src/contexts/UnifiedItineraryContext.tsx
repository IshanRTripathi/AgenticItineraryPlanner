import React, { createContext, useContext, useReducer, useEffect, useCallback, useMemo } from 'react';
import { NormalizedItinerary, NormalizedDay, NormalizedNode } from '../types/NormalizedItinerary';
import { TripData } from '../types/TripData';
import { ChatRequest, ChatResponse } from '../types/ChatTypes';
import { itineraryApi } from '../services/api';
import { webSocketService } from '../services/websocket';
import { logger, logInfo, logError, logWarn, logDebug, logStateChange, logUserAction, startTimer } from '../utils/logger';
import { 
  normalizedItineraryToTripData, 
  tripDataToNormalizedItinerary,
  isNormalizedItinerary,
  isTripData 
} from '../utils/dataTransformers';

// Additional types for unified context
export interface ChatMessage {
  id: string;
  text: string;
  sender: 'user' | 'assistant';
  timestamp: Date;
  selectedNodeId?: string;
  data?: any;
  type?: 'text' | 'itinerary_update' | 'booking_confirmation' | 'error';
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
interface UnifiedItineraryState {
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
}

/**
 * Action types for the unified reducer
 */
type UnifiedItineraryAction =
  // Itinerary actions
  | { type: 'SET_LOADING'; payload: boolean }
  | { type: 'SET_ERROR'; payload: string | null }
  | { type: 'SET_ITINERARY'; payload: TripData }
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
 * Initial state for the unified context
 */
const initialState: UnifiedItineraryState = {
  // Core itinerary data
  itinerary: null,
  loading: false,
  error: null,
  
  // Chat system
  chatMessages: [],
  chatLoading: false,
  chatError: null,
  
  // Workflow system
  workflowNodes: [],
  workflowEdges: [],
  workflowSettings: {
    autoLayout: true,
    showMinimap: false,
    snapToGrid: true,
    gridSize: 20,
    theme: 'light'
  },
  selectedNodeId: null,
  
  // Agent system
  agentData: {},
  activeAgents: [],
  agentProgress: {},
  
  // Revision system
  revisions: [],
  currentRevision: null,
  pendingChanges: [],
  
  // UI state
  selectedDay: 0,
  selectedNodeIds: [],
  viewMode: 'day-by-day',
  sidebarOpen: true,
  
  // Real-time sync
  isConnected: false,
  lastSyncTime: null,
  syncStatus: 'idle'
};

/**
 * Unified reducer for managing all itinerary state
 */
function unifiedItineraryReducer(state: UnifiedItineraryState, action: UnifiedItineraryAction): UnifiedItineraryState {
  switch (action.type) {
    // Itinerary actions
    case 'SET_LOADING':
      return { ...state, loading: action.payload };
    
    case 'SET_ERROR':
      return { ...state, error: action.payload, loading: false };
    
    case 'SET_ITINERARY':
      return {
        ...state,
        itinerary: action.payload,
        agentData: {}, // Will be populated from backend data
        workflowNodes: [], // Will be populated from backend data
        workflowEdges: [], // Will be populated from backend data
        loading: false,
        error: null
      };
    
    case 'UPDATE_DAY':
      if (!state.itinerary?.itinerary) return state;
      const updatedDays = [...state.itinerary.itinerary.days];
      // For now, just update the day directly (type compatibility will be handled by data transformers)
      updatedDays[action.payload.dayIndex] = action.payload.day as any;
      return {
        ...state,
        itinerary: { 
          ...state.itinerary, 
          itinerary: { ...state.itinerary.itinerary, days: updatedDays }
        }
      };
    
    case 'UPDATE_NODE':
      if (!state.itinerary?.itinerary) return state;
      const daysCopy = [...state.itinerary.itinerary.days];
      const componentsCopy = [...daysCopy[action.payload.dayIndex].components];
      // Convert NormalizedNode back to TripComponent format
      const updatedComponent = {
        ...componentsCopy[action.payload.nodeIndex],
        name: action.payload.node.title,
        locked: action.payload.node.locked
      };
      componentsCopy[action.payload.nodeIndex] = updatedComponent;
      daysCopy[action.payload.dayIndex] = { ...daysCopy[action.payload.dayIndex], components: componentsCopy };
      return {
        ...state,
        itinerary: { 
          ...state.itinerary, 
          itinerary: { ...state.itinerary.itinerary, days: daysCopy }
        }
      };
    
    case 'ADD_NODE':
      if (!state.itinerary?.itinerary) return state;
      const daysForAdd = [...state.itinerary.itinerary.days];
      const componentsForAdd = [...daysForAdd[action.payload.dayIndex].components];
      const position = action.payload.position ?? componentsForAdd.length;
      // Convert NormalizedNode to component format (minimal implementation)
      const newComponent = {
        id: action.payload.node.id,
        name: action.payload.node.title,
        type: action.payload.node.type,
        locked: action.payload.node.locked,
        // Add required TripComponent properties with defaults
        description: (action.payload.node as any).description || '',
        location: action.payload.node.location || { name: '', address: '', coordinates: { lat: null, lng: null } },
        timing: { startTime: new Date().toISOString(), endTime: new Date().toISOString(), duration: 60, suggestedDuration: 60 },
        cost: { pricePerPerson: 0, currency: 'EUR', priceRange: 'mid-range' as const, includesWhat: [], additionalCosts: [] },
        travel: { distanceFromPrevious: 0, travelTimeFromPrevious: 0, transportMode: 'walking' as const, transportCost: 0 },
        details: { rating: 0, reviewCount: 0, category: '', tags: [], openingHours: {}, contact: {}, accessibility: { wheelchairAccessible: false, elevatorAccess: false, restrooms: false, parking: false }, amenities: [] },
        booking: { required: false, notes: '' },
        media: { images: [], videos: [], virtualTour: undefined },
        tips: { bestTimeToVisit: '', whatToBring: [], insider: [], warnings: [] },
        priority: 'recommended' as const
      } as any;
      componentsForAdd.splice(position, 0, newComponent);
      daysForAdd[action.payload.dayIndex] = { ...daysForAdd[action.payload.dayIndex], components: componentsForAdd };
      return {
        ...state,
        itinerary: { 
          ...state.itinerary, 
          itinerary: { ...state.itinerary.itinerary, days: daysForAdd }
        }
      };
    
    case 'REMOVE_NODE':
      if (!state.itinerary?.itinerary) return state;
      
      // Simplified to only handle TripData structure
      const itineraryForRemove = { ...state.itinerary.itinerary };
      const daysForRemove = [...itineraryForRemove.days];
      const dayForRemove = daysForRemove[action.payload.dayIndex];
      
      if (dayForRemove.components) {
        const componentsForRemove = [...dayForRemove.components];
        componentsForRemove.splice(action.payload.nodeIndex, 1);
        daysForRemove[action.payload.dayIndex] = { ...dayForRemove, components: componentsForRemove };
      }
      
      return {
        ...state,
        itinerary: { 
          ...state.itinerary, 
          itinerary: { ...itineraryForRemove, days: daysForRemove }
        }
      };
    
    case 'MOVE_NODE':
      if (!state.itinerary?.itinerary) return state;
      const daysForMove = [...state.itinerary.itinerary.days];
      const sourceComponents = [...daysForMove[action.payload.fromDay].components];
      const targetComponents = action.payload.fromDay === action.payload.toDay ? sourceComponents : [...daysForMove[action.payload.toDay].components];
      
      const [movedComponent] = sourceComponents.splice(action.payload.fromIndex, 1);
      targetComponents.splice(action.payload.toIndex, 0, movedComponent);
      
      daysForMove[action.payload.fromDay] = { ...daysForMove[action.payload.fromDay], components: sourceComponents };
      if (action.payload.fromDay !== action.payload.toDay) {
        daysForMove[action.payload.toDay] = { ...daysForMove[action.payload.toDay], components: targetComponents };
      }
      
      return {
        ...state,
        itinerary: { 
          ...state.itinerary, 
          itinerary: { ...state.itinerary.itinerary, days: daysForMove }
        }
      };
    
    // Chat actions
    case 'ADD_CHAT_MESSAGE':
      return {
        ...state,
        chatMessages: [...state.chatMessages, action.payload]
      };
    
    case 'SET_CHAT_LOADING':
      return { ...state, chatLoading: action.payload };
    
    case 'SET_CHAT_ERROR':
      return { ...state, chatError: action.payload, chatLoading: false };
    
    case 'CLEAR_CHAT_MESSAGES':
      return { ...state, chatMessages: [] };
    
    // Workflow actions
    case 'SET_WORKFLOW_NODES':
      return { ...state, workflowNodes: action.payload };
    
    case 'SET_WORKFLOW_EDGES':
      return { ...state, workflowEdges: action.payload };
    
    case 'UPDATE_WORKFLOW_SETTINGS':
      return {
        ...state,
        workflowSettings: { ...state.workflowSettings, ...action.payload }
      };
    
    case 'SELECT_WORKFLOW_NODE':
      return { ...state, selectedNodeId: action.payload };
    
    // Agent actions
    case 'SET_AGENT_DATA':
      return { ...state, agentData: action.payload };
    
    case 'UPDATE_AGENT_PROGRESS':
      return {
        ...state,
        agentProgress: {
          ...state.agentProgress,
          [action.payload.agentId]: action.payload.progress
        }
      };
    
    case 'SET_ACTIVE_AGENTS':
      return { ...state, activeAgents: action.payload };
    
    // Revision actions
    case 'SET_REVISIONS':
      return { ...state, revisions: action.payload };
    
    case 'SET_CURRENT_REVISION':
      return { ...state, currentRevision: action.payload };
    
    case 'ADD_PENDING_CHANGE':
      return {
        ...state,
        pendingChanges: [...state.pendingChanges, action.payload]
      };
    
    case 'CLEAR_PENDING_CHANGES':
      return { ...state, pendingChanges: [] };
    
    // UI actions
    case 'SET_SELECTED_DAY':
      return { ...state, selectedDay: action.payload };
    
    case 'SET_SELECTED_NODES':
      return { ...state, selectedNodeIds: action.payload };
    
    case 'SET_VIEW_MODE':
      return { ...state, viewMode: action.payload };
    
    case 'SET_SIDEBAR_OPEN':
      return { ...state, sidebarOpen: action.payload };
    
    // Sync actions
    case 'SET_CONNECTION_STATUS':
      return { ...state, isConnected: action.payload };
    
    case 'SET_SYNC_STATUS':
      return { ...state, syncStatus: action.payload };
    
    case 'SET_LAST_SYNC_TIME':
      return { ...state, lastSyncTime: action.payload };
    
    default:
      return state;
  }
}

/**
 * Context interface for the unified itinerary system
 */
interface UnifiedItineraryContextType {
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

/**
 * Create the unified context
 */
const UnifiedItineraryContext = createContext<UnifiedItineraryContextType | null>(null);

/**
 * Props for the UnifiedItineraryProvider
 */
interface UnifiedItineraryProviderProps {
  children: React.ReactNode;
  itineraryId: string;
}

/**
 * Unified Itinerary Provider component
 */
export function UnifiedItineraryProvider({ children, itineraryId }: UnifiedItineraryProviderProps) {
  const [state, dispatch] = useReducer(unifiedItineraryReducer, initialState);
  
  // Enhanced dispatch with logging
  const loggedDispatch = useCallback((action: any) => {
    const oldState = state;
    logDebug(`Dispatching action: ${action.type}`, {
      component: 'UnifiedItineraryProvider',
      action: 'dispatch',
      itineraryId,
      actionType: action.type
    }, { action, currentState: oldState });
    
    dispatch(action);
  }, [state, itineraryId]);
  
  // Load itinerary on mount or when itineraryId changes
  useEffect(() => {
    if (itineraryId) {
      loadItinerary(itineraryId);
    }
  }, [itineraryId]);

  // Load chat history on mount when itineraryId changes
  useEffect(() => {
    if (!itineraryId) return;
    (async () => {
      try {
        const history = await itineraryApi.getChatHistory(itineraryId);
        // Map backend history to ChatMessage[]
        const mapped = (history || []).map((h: any, idx: number) => ({
          id: h.id || `hist_${idx}_${Date.now()}`,
          text: h.message || h.text || '',
          sender: h.sender === 'user' ? 'user' : 'assistant',
          timestamp: typeof h.timestamp === 'number' || typeof h.timestamp === 'string' ? new Date(h.timestamp) : (h.timestamp || new Date()),
          intent: h.intent,
          changeSet: h.changeSet,
          diff: h.diff,
          warnings: h.warnings,
          applied: h.applied,
          candidates: h.candidates,
        } as ChatMessage));
        mapped.forEach(m => loggedDispatch({ type: 'ADD_CHAT_MESSAGE', payload: m }));
      } catch (e) {
        logWarn('Failed to load chat history', { component: 'UnifiedItineraryProvider', action: 'chat_history_load', itineraryId }, e as any);
      }
    })();
  }, [itineraryId]);
  
  // Set up WebSocket connection with proper connection management
  useEffect(() => {
    if (!itineraryId) return;

    logInfo('Setting up WebSocket connection', {
      component: 'UnifiedItineraryProvider',
      action: 'websocket_setup',
      itineraryId
    });

    let isActive = true;
    let connectionTimeout: NodeJS.Timeout;

    // Delay connection to prevent immediate reconnection loops
    connectionTimeout = setTimeout(() => {
      if (!isActive) return;

      webSocketService.connect(itineraryId).then(() => {
        if (!isActive) return;
        
        logInfo('WebSocket connected successfully', {
          component: 'UnifiedItineraryProvider',
          action: 'websocket_connected',
          itineraryId
        });
      }).catch((error) => {
        if (!isActive) return;
        
        logError('Failed to connect to WebSocket', {
          component: 'UnifiedItineraryProvider',
          action: 'websocket_connection_failed',
          itineraryId,
          error: error.message
        }, error);
      });
    }, 1000); // 1 second delay to prevent connection storms

      // Listen for real-time updates
      const handleMessage = (message: any) => {
        if (!isActive) return;
        
        logInfo(`WebSocket message received: ${message.type}`, {
          component: 'UnifiedItineraryProvider',
          action: 'websocket_message',
          itineraryId,
          messageType: message.type
        }, message);
        
        switch (message.type) {
          case 'itinerary_updated':
            logInfo('Processing itinerary update from WebSocket', {
              component: 'UnifiedItineraryProvider',
              action: 'itinerary_update',
              itineraryId
            });
            loggedDispatch({ type: 'SET_ITINERARY', payload: message.data });
            loggedDispatch({ type: 'SET_LAST_SYNC_TIME', payload: new Date() });
            break;
          case 'agent_progress':
            logInfo(`Agent progress update: ${message.agentId} - ${message.progress}%`, {
              component: 'UnifiedItineraryProvider',
              action: 'agent_progress',
              itineraryId,
              agentId: message.agentId,
              progress: message.progress
            });
            loggedDispatch({ 
              type: 'UPDATE_AGENT_PROGRESS', 
              payload: { agentId: message.agentId!, progress: message.progress! }
            });
            break;
          case 'chat_response':
            logInfo('Chat response received via WebSocket', {
              component: 'UnifiedItineraryProvider',
              action: 'chat_response',
              itineraryId
            });
            
            // Create ChatMessage from WebSocket response
            const chatMessage: ChatMessage = {
              id: message.data.id || `msg_${Date.now()}_ws`,
              text: message.data.text,
              sender: message.data.sender || 'assistant',
              timestamp: new Date(message.data.timestamp || Date.now()),
              data: message.data.data
            };
            
            loggedDispatch({ type: 'ADD_CHAT_MESSAGE', payload: chatMessage });
            // Persist assistant message to history (best-effort, non-blocking)
            itineraryApi.addChatHistoryMessage(itineraryId, {
              message: chatMessage.text,
              sender: chatMessage.sender,
              timestamp: Date.now(),
              intent: message.data.data?.intent,
              changeSet: message.data.data?.changeSet,
              diff: message.data.data?.diff,
              warnings: message.data.data?.warnings,
              applied: message.data.data?.applied,
              candidates: message.data.data?.candidates,
            }).catch((persistErr) => {
              logWarn('Failed to persist assistant chat message (WebSocket)', { component: 'UnifiedItineraryProvider', action: 'chat_history_persist_assistant_ws', itineraryId }, persistErr as any);
            });
            
            // If changes were applied, reload the itinerary
            if (message.data.data?.applied && message.data.data?.changeSet) {
              logInfo('WebSocket chat response applied changes, reloading itinerary', {
                component: 'UnifiedItineraryProvider',
                action: 'websocket_chat_changes_applied',
                itineraryId
              });
              
              loadItinerary(itineraryId);
            }
            break;
          case 'chat_update':
            logInfo('Chat update received via WebSocket', {
              component: 'UnifiedItineraryProvider',
              action: 'chat_update',
              itineraryId
            });
            
            // Reload itinerary when chat updates are received
            if (message.data?.changes) {
              logInfo('Chat update contains changes, reloading itinerary', {
                component: 'UnifiedItineraryProvider',
                action: 'chat_update_reload',
                itineraryId
              });
              
              loadItinerary(itineraryId);
            }
            break;
          default:
            logWarn(`Unknown WebSocket message type: ${message.type}`, {
              component: 'UnifiedItineraryProvider',
              action: 'websocket_unknown_message',
              itineraryId,
              messageType: message.type
            }, message);
        }
      };
      
      const handleConnectionChange = (connected: boolean) => {
        if (!isActive) return;
        
        logInfo(`WebSocket connection status changed: ${connected ? 'connected' : 'disconnected'}`, {
          component: 'UnifiedItineraryProvider',
          action: 'websocket_connection_change',
          itineraryId,
          connected
        });
        loggedDispatch({ type: 'SET_CONNECTION_STATUS', payload: connected });
      };
      
      webSocketService.on('message', handleMessage);
      webSocketService.onConnectionChange(handleConnectionChange);

    return () => {
      isActive = false;
      clearTimeout(connectionTimeout);
      
      logInfo('Cleaning up WebSocket connection', {
        component: 'UnifiedItineraryProvider',
        action: 'websocket_cleanup',
        itineraryId
      });
      
      webSocketService.off('message', handleMessage);
      webSocketService.offConnectionChange(handleConnectionChange);
      webSocketService.disconnect();
    };
    }, [itineraryId]); // Remove loggedDispatch from dependencies to prevent reconnection loops
  
  // Core itinerary actions
  const loadItinerary = useCallback(async (id: string) => {
    const timer = startTimer(`Load itinerary ${id}`, {
      component: 'UnifiedItineraryProvider',
      action: 'load_itinerary',
      itineraryId: id
    });
    
    logInfo(`Loading itinerary: ${id}`, {
      component: 'UnifiedItineraryProvider',
      action: 'load_itinerary_start',
      itineraryId: id
    });
    
    loggedDispatch({ type: 'SET_LOADING', payload: true });
    
    try {
      const itinerary = await itineraryApi.getItinerary(id);
      
      logInfo(`Successfully loaded itinerary: ${id}`, {
        component: 'UnifiedItineraryProvider',
        action: 'load_itinerary_success',
        itineraryId: id,
        daysCount: itinerary.itinerary?.days?.length || 0,
        totalComponents: itinerary.itinerary?.days?.reduce((total, day) => total + (day.components?.length || 0), 0) || 0
      }, {
        itinerarySummary: {
          id: itinerary.id,
          summary: itinerary.summary,
          daysCount: itinerary.itinerary?.days?.length,
          destination: itinerary.destination
        }
      });
      
      loggedDispatch({ type: 'SET_ITINERARY', payload: itinerary });
      timer();
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : 'Failed to load itinerary';
      
      logError(`Failed to load itinerary: ${id}`, {
        component: 'UnifiedItineraryProvider',
        action: 'load_itinerary_error',
        itineraryId: id,
        errorType: error instanceof Error ? error.constructor.name : 'Unknown',
        errorMessage
      }, error);
      
      loggedDispatch({ type: 'SET_ERROR', payload: errorMessage });
      timer();
    }
  }, [loggedDispatch]);
  
  const saveItinerary = useCallback(async () => {
    if (!state.itinerary) {
      logWarn('Attempted to save itinerary but no itinerary loaded', {
        component: 'UnifiedItineraryProvider',
        action: 'save_itinerary_no_data',
        itineraryId
      });
      return;
    }
    
    const timer = startTimer(`Save itinerary ${state.itinerary.id}`, {
      component: 'UnifiedItineraryProvider',
      action: 'save_itinerary',
      itineraryId: state.itinerary.id
    });
    
    logInfo(`Saving itinerary: ${state.itinerary.id}`, {
      component: 'UnifiedItineraryProvider',
      action: 'save_itinerary_start',
      itineraryId: state.itinerary.id,
      hasUnsavedChanges: state.hasUnsavedChanges
    });
    
    loggedDispatch({ type: 'SET_SYNC_STATUS', payload: 'syncing' });
    
    try {
      await itineraryApi.updateItinerary(state.itinerary.id, state.itinerary);
      
      logInfo(`Successfully saved itinerary: ${state.itinerary.id}`, {
        component: 'UnifiedItineraryProvider',
        action: 'save_itinerary_success',
        itineraryId: state.itinerary.id
      });
      
      loggedDispatch({ type: 'SET_SYNC_STATUS', payload: 'idle' });
      loggedDispatch({ type: 'SET_LAST_SYNC_TIME', payload: new Date() });
      loggedDispatch({ type: 'CLEAR_PENDING_CHANGES' });
      timer();
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : 'Failed to save itinerary';
      
      logError(`Failed to save itinerary: ${state.itinerary.id}`, {
        component: 'UnifiedItineraryProvider',
        action: 'save_itinerary_error',
        itineraryId: state.itinerary.id,
        errorType: error instanceof Error ? error.constructor.name : 'Unknown',
        errorMessage
      }, error);
      
      loggedDispatch({ type: 'SET_SYNC_STATUS', payload: 'error' });
      timer();
      throw error;
    }
  }, [state.itinerary, state.hasUnsavedChanges, itineraryId, loggedDispatch]);
  
  const updateDay = useCallback((dayIndex: number, day: NormalizedDay) => {
    dispatch({ type: 'UPDATE_DAY', payload: { dayIndex, day } });
    dispatch({ type: 'ADD_PENDING_CHANGE', payload: {
      id: `day_${dayIndex}_${Date.now()}`,
      type: 'update',
      scope: 'day',
      target: `day_${dayIndex}`,
      description: `Updated day ${dayIndex + 1}`,
      timestamp: new Date(),
      data: day
    }});
  }, []);
  
  const updateNode = useCallback((dayIndex: number, nodeIndex: number, node: NormalizedNode) => {
    dispatch({ type: 'UPDATE_NODE', payload: { dayIndex, nodeIndex, node } });
    dispatch({ type: 'ADD_PENDING_CHANGE', payload: {
      id: `node_${dayIndex}_${nodeIndex}_${Date.now()}`,
      type: 'update',
      scope: 'node',
      target: node.id,
      description: `Updated ${node.title}`,
      timestamp: new Date(),
      data: node
    }});
  }, []);
  
  const addNode = useCallback((dayIndex: number, node: NormalizedNode, position?: number) => {
    dispatch({ type: 'ADD_NODE', payload: { dayIndex, node, position } });
    dispatch({ type: 'ADD_PENDING_CHANGE', payload: {
      id: `add_node_${dayIndex}_${Date.now()}`,
      type: 'insert',
      scope: 'node',
      target: node.id,
      description: `Added ${node.title}`,
      timestamp: new Date(),
      data: node
    }});
  }, []);
  
  const removeNode = useCallback((dayIndex: number, nodeIndex: number) => {
    console.log('🔄 UnifiedItineraryContext.removeNode called:', { dayIndex, nodeIndex });
    
    if (!state.itinerary?.itinerary?.days) {
      console.error('❌ No itinerary days data found');
      return;
    }
    
    const day = state.itinerary.itinerary.days[dayIndex];
    if (!day) {
      console.error('❌ Day not found at index:', dayIndex);
      return;
    }
    
    const component = day.components[nodeIndex];
    if (component) {
      console.log('✅ Component found, dispatching REMOVE_NODE action');
      dispatch({ type: 'REMOVE_NODE', payload: { dayIndex, nodeIndex } });
      dispatch({ type: 'ADD_PENDING_CHANGE', payload: {
        id: `remove_node_${dayIndex}_${nodeIndex}_${Date.now()}`,
        type: 'delete',
        scope: 'node',
        target: component.id,
        description: `Removed ${component.name}`,
        timestamp: new Date(),
        data: component
      }});
      console.log('✅ REMOVE_NODE action dispatched successfully');
    } else {
      console.error('❌ Component not found at dayIndex:', dayIndex, 'nodeIndex:', nodeIndex);
    }
  }, [state.itinerary]);
  
  const moveNode = useCallback((fromDay: number, fromIndex: number, toDay: number, toIndex: number) => {
    const component = state.itinerary?.itinerary?.days[fromDay]?.components[fromIndex];
    if (component) {
      dispatch({ type: 'MOVE_NODE', payload: { fromDay, fromIndex, toDay, toIndex } });
      dispatch({ type: 'ADD_PENDING_CHANGE', payload: {
        id: `move_node_${fromDay}_${fromIndex}_${Date.now()}`,
        type: 'move',
        scope: 'node',
        target: component.id,
        description: `Moved ${component.name} from day ${fromDay + 1} to day ${toDay + 1}`,
        timestamp: new Date(),
        data: { fromDay, fromIndex, toDay, toIndex }
      }});
    }
  }, [state.itinerary]);
  
  // Chat actions
  const sendChatMessage = useCallback(async (message: string, selectedNodeId?: string) => {
    if (!state.itinerary) {
      logError('Cannot send chat message: no itinerary loaded', {
        component: 'UnifiedItineraryProvider',
        action: 'send_chat_message_error'
      });
      return;
    }

    const timer = startTimer(`Send chat message`, {
      component: 'UnifiedItineraryProvider',
      action: 'send_chat_message',
      itineraryId
    });
    
    logInfo(`Sending chat message`, {
      component: 'UnifiedItineraryProvider',
      action: 'send_chat_message_start',
      itineraryId,
      messageLength: message.length,
      hasSelectedNode: !!selectedNodeId,
      selectedNodeId,
      selectedDay: state.selectedDay
    }, { messagePreview: message.substring(0, 100) + (message.length > 100 ? '...' : '') });
    
    const userMessage: ChatMessage = {
      id: `msg_${Date.now()}`,
      text: message,
      sender: 'user',
      timestamp: new Date(),
      selectedNodeId
    };
    
    loggedDispatch({ type: 'ADD_CHAT_MESSAGE', payload: userMessage });
    // Persist user message to history (best-effort, non-blocking)
    try {
      await itineraryApi.addChatHistoryMessage(itineraryId, {
        message,
        sender: 'user',
        timestamp: Date.now(),
        selectedNodeId,
        day: state.selectedDay ?? undefined,
      });
    } catch (persistErr) {
      logWarn('Failed to persist user chat message', { component: 'UnifiedItineraryProvider', action: 'chat_history_persist_user', itineraryId }, persistErr as any);
    }
    loggedDispatch({ type: 'SET_CHAT_LOADING', payload: true });
    
    try {
      // Create ChatRequest for the backend
      const chatRequest: ChatRequest = {
        itineraryId: state.itinerary.id,
        scope: state.selectedDay !== null ? 'day' : 'trip',
        day: state.selectedDay || undefined,
        selectedNodeId,
        text: message,
        autoApply: false
      };

      // Send via WebSocket for real-time response
      logInfo('Sending chat message via WebSocket', {
        component: 'UnifiedItineraryProvider',
        action: 'chat_websocket_send',
        itineraryId
      });
      
      webSocketService.sendChatMessage(message, {
        selectedNodeId,
        selectedDay: state.selectedDay,
        scope: chatRequest.scope,
        autoApply: false
      });
      
      // Also send via REST API as fallback and for reliability
      logInfo('Sending chat message via REST API', {
        component: 'UnifiedItineraryProvider',
        action: 'chat_rest_send',
        itineraryId
      });
      
      const response = await itineraryApi.sendChatMessage(state.itinerary.id, chatRequest);
      
      // Add assistant response to chat
      const assistantMessage: ChatMessage = {
        id: `msg_${Date.now()}_response`,
        text: response.message,
        sender: 'assistant',
        timestamp: new Date(),
        data: {
          changeSet: response.changeSet,
          diff: response.diff,
          applied: response.applied
        }
      };
      
      loggedDispatch({ type: 'ADD_CHAT_MESSAGE', payload: assistantMessage });

      // Persist assistant message to history (best-effort)
      try {
        await itineraryApi.addChatHistoryMessage(itineraryId, {
          message: assistantMessage.text,
          sender: 'assistant',
          timestamp: Date.now(),
          intent: response.intent,
          changeSet: response.changeSet,
          diff: response.diff,
          warnings: response.warnings,
          applied: response.applied,
          candidates: response.candidates,
        });
      } catch (persistErr) {
        logWarn('Failed to persist assistant chat message', { component: 'UnifiedItineraryProvider', action: 'chat_history_persist_assistant', itineraryId }, persistErr as any);
      }
      
      // If changes were applied, reload the itinerary
      if (response.applied && response.changeSet) {
        logInfo('Chat response applied changes, reloading itinerary', {
          component: 'UnifiedItineraryProvider',
          action: 'chat_changes_applied',
          itineraryId
        });
        
        await loadItinerary(itineraryId);
      }
      
      logInfo('Chat message processed successfully', {
        component: 'UnifiedItineraryProvider',
        action: 'chat_message_success',
        itineraryId,
        hasChanges: !!response.changeSet,
        applied: response.applied
      });
      
      timer();
      
    } catch (error) {
      logError('Failed to send chat message', {
        component: 'UnifiedItineraryProvider',
        action: 'send_chat_message_error',
        itineraryId,
        errorType: error instanceof Error ? error.constructor.name : 'Unknown'
      }, error);
      
      const errorMessage: ChatMessage = {
        id: `msg_${Date.now()}_error`,
        text: 'Sorry, I encountered an error processing your request. Please try again.',
        sender: 'assistant',
        timestamp: new Date(),
        data: {
          error: error instanceof Error ? error.message : 'Unknown error'
        }
      };
      
      loggedDispatch({ type: 'ADD_CHAT_MESSAGE', payload: errorMessage });
      loggedDispatch({ type: 'SET_CHAT_ERROR', payload: error instanceof Error ? error.message : 'Failed to send message' });
      
      timer();
    } finally {
      dispatch({ type: 'SET_CHAT_LOADING', payload: false });
    }
  }, [itineraryId, state.selectedDay]);
  
  const clearChatMessages = useCallback(() => {
    dispatch({ type: 'CLEAR_CHAT_MESSAGES' });
  }, []);

  // Clear chat history (backend + local state)
  const clearChatHistory = useCallback(async () => {
    if (!itineraryId) return;
    try {
      await itineraryApi.clearChatHistory(itineraryId);
      dispatch({ type: 'CLEAR_CHAT_MESSAGES' });
      logInfo('Chat history cleared', { component: 'UnifiedItineraryProvider', action: 'chat_history_cleared', itineraryId });
    } catch (e) {
      logWarn('Failed to clear chat history', { component: 'UnifiedItineraryProvider', action: 'chat_history_clear_failed', itineraryId }, e as any);
      throw e;
    }
  }, [itineraryId]);
  
  // Workflow actions
  const updateWorkflowSettings = useCallback((settings: Partial<WorkflowSettings>) => {
    dispatch({ type: 'UPDATE_WORKFLOW_SETTINGS', payload: settings });
  }, []);
  
  const selectWorkflowNode = useCallback((nodeId: string | null) => {
    dispatch({ type: 'SELECT_WORKFLOW_NODE', payload: nodeId });
  }, []);
  
  // Agent actions
  const executeAgent = useCallback(async (agentType: string, data: any) => {
    dispatch({ type: 'SET_ACTIVE_AGENTS', payload: [...state.activeAgents, agentType] });
    dispatch({ type: 'UPDATE_AGENT_PROGRESS', payload: { agentId: agentType, progress: 0 } });
    
    try {
      // TODO: Implement agent execution API when backend supports it
      const result = await Promise.resolve({
        success: true,
        message: `${agentType} agent executed successfully`,
        data: data
      });
      
      // Update agent data with results (if available)
      if (result.data) {
        dispatch({ type: 'SET_AGENT_DATA', payload: {
          ...state.agentData,
          [agentType]: result.data
        }});
      }
      
      // If the agent updated the itinerary, refresh it
      if (result.success && result.data?.changes) {
        await loadItinerary(itineraryId);
      }
      
    } catch (error) {
      console.error(`Agent ${agentType} execution failed:`, error);
    } finally {
      dispatch({ type: 'SET_ACTIVE_AGENTS', payload: state.activeAgents.filter(id => id !== agentType) });
      dispatch({ type: 'UPDATE_AGENT_PROGRESS', payload: { agentId: agentType, progress: 100 } });
    }
  }, [itineraryId, state.activeAgents, state.agentData, loadItinerary]);

  // Enhanced agent processing with multiple agents
  const processWithAgents = useCallback(async (nodeId: string, agentIds: string[]) => {
    if (agentIds.length === 0) return;
    
    dispatch({ type: 'SET_SYNC_STATUS', payload: 'syncing' });
    dispatch({ type: 'SET_ACTIVE_AGENTS', payload: [...state.activeAgents, ...agentIds] });
    
    // Initialize progress for all agents
    agentIds.forEach(agentId => {
      dispatch({ type: 'UPDATE_AGENT_PROGRESS', payload: { agentId, progress: 0 } });
    });
    
    try {
      // Execute agents in sequence for now (could be parallel in future)
      for (const agentId of agentIds) {
        await executeAgent(agentId, { nodeId, selectedDay: state.selectedDay });
      }
      
      dispatch({ type: 'SET_SYNC_STATUS', payload: 'idle' });
      dispatch({ type: 'SET_LAST_SYNC_TIME', payload: new Date() });
      
    } catch (error) {
      console.error('Agent processing failed:', error);
      dispatch({ type: 'SET_SYNC_STATUS', payload: 'error' });
      throw error;
    } finally {
      // Clean up active agents
      dispatch({ type: 'SET_ACTIVE_AGENTS', payload: 
        state.activeAgents.filter(id => !agentIds.includes(id)) 
      });
    }
  }, [state.activeAgents, state.selectedDay, executeAgent]);
  
  // Revision actions
  const loadRevisions = useCallback(async () => {
    try {
      // TODO: Implement revisions API when backend supports it
      const revisions = await Promise.resolve([]);
      dispatch({ type: 'SET_REVISIONS', payload: revisions });
    } catch (error) {
      console.error('Failed to load revisions:', error);
    }
  }, [itineraryId]);
  
  const switchToRevision = useCallback(async (revisionId: string) => {
    try {
      // TODO: Implement revision retrieval API when backend supports it
      const itinerary = await itineraryApi.getItinerary(itineraryId);
      dispatch({ type: 'SET_ITINERARY', payload: itinerary });
      dispatch({ type: 'SET_CURRENT_REVISION', payload: revisionId });
    } catch (error) {
      console.error('Failed to switch to revision:', error);
    }
  }, [itineraryId]);
  
  const createRevision = useCallback(async (description: string) => {
    try {
      // TODO: Implement revision creation API when backend supports it
      const revision = await Promise.resolve({
        id: `rev_${Date.now()}`,
        itineraryId,
        description,
        createdAt: new Date(),
        createdBy: 'current-user',
        version: '1.0',
        changes: []
      });
      dispatch({ type: 'SET_REVISIONS', payload: [...state.revisions, revision] });
      dispatch({ type: 'SET_CURRENT_REVISION', payload: revision.id });
    } catch (error) {
      console.error('Failed to create revision:', error);
    }
  }, [itineraryId, state.revisions]);

  // Enhanced rollback functionality
  const rollbackToRevision = useCallback(async (revisionId: string) => {
    dispatch({ type: 'SET_SYNC_STATUS', payload: 'syncing' });
    
    try {
      // Store current state for potential recovery
      const previousState = state.itinerary;
      
      // Load the revision
      // TODO: Implement revision retrieval API when backend supports it
      const restoredItinerary = await itineraryApi.getItinerary(itineraryId);
      
      // Update local state
      dispatch({ type: 'SET_ITINERARY', payload: restoredItinerary });
      dispatch({ type: 'SET_CURRENT_REVISION', payload: revisionId });
      dispatch({ type: 'CLEAR_PENDING_CHANGES' });
      
      // Save the rollback to backend
      // TODO: Implement itinerary update API when backend supports it
      // await apiClient.updateItinerary(itineraryId, restoredItinerary);
      
      dispatch({ type: 'SET_SYNC_STATUS', payload: 'idle' });
      dispatch({ type: 'SET_LAST_SYNC_TIME', payload: new Date() });
      
      console.log(`Successfully rolled back to revision: ${revisionId}`);
      
    } catch (error) {
      console.error('Rollback failed:', error);
      dispatch({ type: 'SET_SYNC_STATUS', payload: 'error' });
      
      // Optionally revert to previous state on failure
      // This would require storing previousState in a ref or state
      throw new Error(`Failed to rollback to revision: ${error instanceof Error ? error.message : 'Unknown error'}`);
    }
  }, [itineraryId, state.itinerary]);
  
  // UI actions
  const setSelectedDay = useCallback((day: number) => {
    dispatch({ type: 'SET_SELECTED_DAY', payload: day });
  }, []);
  
  const setSelectedNodes = useCallback((nodeIds: string[]) => {
    dispatch({ type: 'SET_SELECTED_NODES', payload: nodeIds });
  }, []);
  
  const setViewMode = useCallback((mode: 'day-by-day' | 'workflow' | 'timeline') => {
    dispatch({ type: 'SET_VIEW_MODE', payload: mode });
  }, []);
  
  const setSidebarOpen = useCallback((open: boolean) => {
    dispatch({ type: 'SET_SIDEBAR_OPEN', payload: open });
  }, []);
  
  // Utility functions
  const getSelectedNodes = useCallback((): NormalizedNode[] => {
    if (!state.itinerary || state.selectedNodeIds.length === 0) return [];
    
    const nodes: NormalizedNode[] = [];
    state.itinerary.itinerary?.days.forEach(day => {
      day.components?.forEach(component => {
        if (state.selectedNodeIds.includes(component.id)) {
          // Convert component to NormalizedNode format
          const node: NormalizedNode = {
            id: component.id,
            title: component.name,
            type: component.type as any,
            locked: component.locked
          };
          nodes.push(node);
        }
      });
    });
    return nodes;
  }, [state.itinerary, state.selectedNodeIds]);
  
  const getCurrentDay = useCallback((): any | null => {
    if (!state.itinerary?.itinerary || state.selectedDay < 0 || state.selectedDay >= state.itinerary.itinerary.days.length) {
      return null;
    }
    return state.itinerary.itinerary.days[state.selectedDay];
  }, [state.itinerary, state.selectedDay]);
  
  const hasUnsavedChanges = useCallback((): boolean => {
    return state.pendingChanges.length > 0;
  }, [state.pendingChanges]);
  
  // Memoize the context value to prevent unnecessary re-renders
  const contextValue = useMemo(() => ({
    state,
    loadItinerary,
    saveItinerary,
    updateDay,
    updateNode,
    addNode,
    removeNode,
    moveNode,
    sendChatMessage,
    clearChatMessages,
    clearChatHistory,
    updateWorkflowSettings,
    selectWorkflowNode,
    executeAgent,
    processWithAgents,
    loadRevisions,
    switchToRevision,
    createRevision,
    rollbackToRevision,
    setSelectedDay,
    setSelectedNodes,
    setViewMode,
    setSidebarOpen,
    getSelectedNodes,
    getCurrentDay,
    hasUnsavedChanges
  }), [
    state,
    loadItinerary,
    saveItinerary,
    updateDay,
    updateNode,
    addNode,
    removeNode,
    moveNode,
    sendChatMessage,
    clearChatMessages,
    updateWorkflowSettings,
    selectWorkflowNode,
    executeAgent,
    processWithAgents,
    loadRevisions,
    switchToRevision,
    createRevision,
    rollbackToRevision,
    setSelectedDay,
    setSelectedNodes,
    setViewMode,
    setSidebarOpen,
    getSelectedNodes,
    getCurrentDay,
    hasUnsavedChanges
  ]);
  
  return (
    <UnifiedItineraryContext.Provider value={contextValue}>
      {children}
    </UnifiedItineraryContext.Provider>
  );
}

/**
 * Hook to use the unified itinerary context
 */
export function useUnifiedItinerary(): UnifiedItineraryContextType {
  const context = useContext(UnifiedItineraryContext);
  if (!context) {
    throw new Error('useUnifiedItinerary must be used within a UnifiedItineraryProvider');
  }
  return context;
}

/**
 * Hook to get specific parts of the state (for performance optimization)
 */
export function useUnifiedItinerarySelector<T>(selector: (state: UnifiedItineraryState) => T): T {
  const { state } = useUnifiedItinerary();
  return useMemo(() => selector(state), [selector, state]);
}

export default UnifiedItineraryContext;