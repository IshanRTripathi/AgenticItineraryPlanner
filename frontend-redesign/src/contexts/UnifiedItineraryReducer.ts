import { UnifiedItineraryState, UnifiedItineraryAction } from './UnifiedItineraryTypes';

/**
 * Initial state for the unified context
 */
export const initialState: UnifiedItineraryState = {
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
