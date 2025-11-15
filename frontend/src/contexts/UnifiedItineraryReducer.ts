import { UnifiedItineraryState, UnifiedItineraryAction } from './UnifiedItineraryTypes';

/**
 * Initial state for the unified context
 */
export const initialState: UnifiedItineraryState = {
  // Core itinerary data
  itinerary: null,
  currentPhase: null,
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
export function unifiedItineraryReducer(
  state: UnifiedItineraryState,
  action: UnifiedItineraryAction
): UnifiedItineraryState {
  switch (action.type) {
    // Itinerary actions
    case 'SET_LOADING':
      return { ...state, loading: action.payload };
    
    case 'SET_ERROR':
      return { ...state, error: action.payload, loading: false };
    
    case 'SET_CURRENT_PHASE':
      return { ...state, currentPhase: action.payload };
    
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
      if (!state.itinerary?.days) return state;
      const updatedDays = [...state.itinerary.days];
      updatedDays[action.payload.dayIndex] = action.payload.day;
      return {
        ...state,
        itinerary: { 
          ...state.itinerary, 
          days: updatedDays
        }
      };
    
    case 'UPDATE_NODE':
      if (!state.itinerary?.days) return state;
      const daysCopy = [...state.itinerary.days];
      const nodesCopy = [...daysCopy[action.payload.dayIndex].nodes];
      nodesCopy[action.payload.nodeIndex] = action.payload.node;
      daysCopy[action.payload.dayIndex] = { ...daysCopy[action.payload.dayIndex], nodes: nodesCopy };
      return {
        ...state,
        itinerary: { 
          ...state.itinerary, 
          days: daysCopy
        }
      };
    
    case 'ADD_NODE':
      if (!state.itinerary?.days) return state;
      const daysForAdd = [...state.itinerary.days];
      const nodesForAdd = [...daysForAdd[action.payload.dayIndex].nodes];
      const position = action.payload.position ?? nodesForAdd.length;
      nodesForAdd.splice(position, 0, action.payload.node);
      daysForAdd[action.payload.dayIndex] = { ...daysForAdd[action.payload.dayIndex], nodes: nodesForAdd };
      return {
        ...state,
        itinerary: { 
          ...state.itinerary, 
          days: daysForAdd
        }
      };
    
    case 'REMOVE_NODE':
      if (!state.itinerary?.days) return state;
      const daysForRemove = [...state.itinerary.days];
      const nodesForRemove = [...daysForRemove[action.payload.dayIndex].nodes];
      nodesForRemove.splice(action.payload.nodeIndex, 1);
      daysForRemove[action.payload.dayIndex] = { ...daysForRemove[action.payload.dayIndex], nodes: nodesForRemove };
      return {
        ...state,
        itinerary: { 
          ...state.itinerary, 
          days: daysForRemove
        }
      };
    
    case 'MOVE_NODE':
      if (!state.itinerary?.days) return state;
      const daysForMove = [...state.itinerary.days];
      const sourceNodes = [...daysForMove[action.payload.fromDay].nodes];
      const targetNodes = action.payload.fromDay === action.payload.toDay ? sourceNodes : [...daysForMove[action.payload.toDay].nodes];
      
      const [movedNode] = sourceNodes.splice(action.payload.fromIndex, 1);
      targetNodes.splice(action.payload.toIndex, 0, movedNode);
      
      daysForMove[action.payload.fromDay] = { ...daysForMove[action.payload.fromDay], nodes: sourceNodes };
      if (action.payload.fromDay !== action.payload.toDay) {
        daysForMove[action.payload.toDay] = { ...daysForMove[action.payload.toDay], nodes: targetNodes };
      }
      
      return {
        ...state,
        itinerary: { 
          ...state.itinerary, 
          days: daysForMove
        }
      };
    
    // Chat actions
    case 'ADD_CHAT_MESSAGE':
      // Prevent duplicate messages by checking if message ID already exists
      const messageExists = state.chatMessages.some(m => m.id === action.payload.id);
      if (messageExists) {
        return state;
      }
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
