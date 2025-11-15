import React, { createContext, useReducer, useEffect, useCallback, useMemo } from 'react';
import { itineraryApi } from '../services/api';
import { webSocketService } from '../services/websocket';
import { logInfo, logError, logWarn, logDebug } from '../utils/logger';
import { UnifiedItineraryContextType, ChatMessage } from './UnifiedItineraryTypes';
import { unifiedItineraryReducer, initialState } from './UnifiedItineraryReducer';
import {
  createLoadItinerary,
  createSaveItinerary,
  createUpdateDay,
  createUpdateNode,
  createAddNode,
  createRemoveNode,
  createMoveNode,
  createSendChatMessage,
  createClearChatMessages,
  createClearChatHistory,
  createUpdateWorkflowSettings,
  createSelectWorkflowNode,
  createExecuteAgent,
  createProcessWithAgents,
  createLoadRevisions,
  createSwitchToRevision,
  createCreateRevision,
  createRollbackToRevision,
  createSetSelectedDay,
  createSetSelectedNodes,
  createSetViewMode,
  createSetSidebarOpen
} from './UnifiedItineraryActions';
import {
  createGetSelectedNodes,
  createGetCurrentDay,
  createHasUnsavedChanges,
  setUnifiedItineraryContext
} from './UnifiedItineraryHooks';

/**
 * Create the unified context
 */
const UnifiedItineraryContext = createContext<UnifiedItineraryContextType | null>(null);

// Set the context for hooks to use
setUnifiedItineraryContext(UnifiedItineraryContext);

/**
 * Props for the UnifiedItineraryProvider
 */
interface UnifiedItineraryProviderProps {
  children: React.ReactNode;
  itineraryId: string;
}

/**
 * Unified Itinerary Provider component
 * Refactored to use extracted actions, reducer, and hooks
 */
export function UnifiedItineraryProvider({ children, itineraryId }: UnifiedItineraryProviderProps) {
  const [state, dispatch] = useReducer(unifiedItineraryReducer, initialState);

  // Enhanced dispatch with logging (STABLE - no state dependency)
  const loggedDispatch = useCallback((action: any) => {
    logDebug(`Dispatching action: ${action.type}`, {
      component: 'UnifiedItineraryProvider',
      action: 'dispatch',
      itineraryId,
      actionType: action.type
    });

    dispatch(action);
  }, [itineraryId]); // REMOVED state dependency to make it stable

  // Create action creators
  const loadItinerary = useCallback(createLoadItinerary(loggedDispatch), [loggedDispatch]);
  const saveItinerary = useCallback(createSaveItinerary(state, loggedDispatch, itineraryId), [state, loggedDispatch, itineraryId]);
  const updateDay = useCallback(createUpdateDay(dispatch), []);
  const updateNode = useCallback(createUpdateNode(dispatch), []);
  const addNode = useCallback(createAddNode(dispatch), []);
  const removeNode = useCallback(createRemoveNode(state, dispatch), [state]);
  const moveNode = useCallback(createMoveNode(state, dispatch), [state]);

  const sendChatMessage = useCallback(
    createSendChatMessage(state, dispatch, itineraryId, loadItinerary),
    [state, itineraryId, loadItinerary]
  );
  const clearChatMessages = useCallback(createClearChatMessages(dispatch), []);
  const clearChatHistory = useCallback(createClearChatHistory(dispatch, itineraryId), [itineraryId]);

  const updateWorkflowSettings = useCallback(createUpdateWorkflowSettings(dispatch), []);
  const selectWorkflowNode = useCallback(createSelectWorkflowNode(dispatch), []);

  const executeAgent = useCallback(
    createExecuteAgent(state, dispatch, itineraryId, loadItinerary),
    [state, itineraryId, loadItinerary]
  );
  const processWithAgents = useCallback(
    createProcessWithAgents(state, dispatch, executeAgent),
    [state, executeAgent]
  );

  const loadRevisions = useCallback(createLoadRevisions(dispatch, itineraryId), [itineraryId]);
  const switchToRevision = useCallback(createSwitchToRevision(dispatch, itineraryId), [itineraryId]);
  const createRevision = useCallback(createCreateRevision(state, dispatch, itineraryId), [state, itineraryId]);
  const rollbackToRevision = useCallback(createRollbackToRevision(state, dispatch, itineraryId), [state, itineraryId]);

  const setSelectedDay = useCallback(createSetSelectedDay(dispatch), []);
  const setSelectedNodes = useCallback(createSetSelectedNodes(dispatch), []);
  const setViewMode = useCallback(createSetViewMode(dispatch), []);
  const setSidebarOpen = useCallback(createSetSidebarOpen(dispatch), []);

  // Utility functions
  const getSelectedNodes = useCallback(createGetSelectedNodes(state), [state]);
  const getCurrentDay = useCallback(createGetCurrentDay(state), [state]);
  const hasUnsavedChanges = useCallback(createHasUnsavedChanges(state), [state]);

  // Load itinerary on mount or when itineraryId changes
  useEffect(() => {
    if (itineraryId) {
      loadItinerary(itineraryId);
    }
  }, [itineraryId]);

  // Load chat history on mount when itineraryId changes
  useEffect(() => {
    if (!itineraryId) return;
    
    let isActive = true;
    
    (async () => {
      try {
        // Clear existing messages before loading history to prevent duplicates
        loggedDispatch({ type: 'CLEAR_CHAT_MESSAGES' });
        
        const history = await itineraryApi.getChatHistory(itineraryId);
        
        if (!isActive) return; // Component unmounted
        
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
        if (!isActive) return;
        logWarn('Failed to load chat history', { component: 'UnifiedItineraryProvider', action: 'chat_history_load', itineraryId }, e as any);
      }
    })();
    
    return () => {
      isActive = false;
    };
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
    let connectionTimeout: ReturnType<typeof setTimeout>;
    let reloadTimeout: ReturnType<typeof setTimeout> | null = null;

    // Debounced reload function to prevent too many API calls
    const scheduleItineraryReload = () => {
      if (reloadTimeout) {
        clearTimeout(reloadTimeout);
      }
      reloadTimeout = setTimeout(() => {
        if (isActive) {
          loadItinerary(itineraryId);
        }
      }, 500); // Wait 500ms after last event before reloading (reduced from 2000ms for better responsiveness)
    };

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

      // Backend now sends 'type' field consistently
      const messageType = message.type;
      
      logInfo(`WebSocket message received: ${messageType}`, {
        component: 'UnifiedItineraryProvider',
        action: 'websocket_message',
        itineraryId,
        messageType: messageType
      });

      switch (messageType) {
        case 'itinerary_updated':
          console.log('[UnifiedItineraryProvider] Processing itinerary update:', message.data);
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
        case 'day_completed':
          logInfo(`Day ${message.data?.dayNumber} completed`, {
            component: 'UnifiedItineraryProvider',
            action: 'day_completed',
            itineraryId,
            dayNumber: message.data?.dayNumber
          });
          // Schedule reload with debounce
          scheduleItineraryReload();
          break;
        case 'phase_transition':
          logInfo(`Phase transition: ${message.data?.fromPhase} → ${message.data?.toPhase}`, {
            component: 'UnifiedItineraryProvider',
            action: 'phase_transition',
            itineraryId
          });
          // Update current phase in state
          if (message.data?.toPhase) {
            loggedDispatch({ 
              type: 'SET_CURRENT_PHASE', 
              payload: message.data.toPhase.toLowerCase() 
            });
          }
          // Schedule reload with debounce
          scheduleItineraryReload();
          break;
        case 'agent_complete':
          logInfo(`Agent completed: ${message.data?.agentName} (${message.data?.itemsProcessed} items)`, {
            component: 'UnifiedItineraryProvider',
            action: 'agent_complete',
            itineraryId,
            agentName: message.data?.agentName
          });
          // Schedule reload with debounce
          scheduleItineraryReload();
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
          loggedDispatch({ type: 'SET_CHAT_LOADING', payload: false });
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

          // If the update includes the full itinerary, use it directly (optimization)
          if (message.data?.itinerary) {
            logInfo('Chat update includes full itinerary, updating state directly', {
              component: 'UnifiedItineraryProvider',
              action: 'chat_update_direct',
              itineraryId
            });
            
            loggedDispatch({ type: 'SET_ITINERARY', payload: message.data.itinerary });
            loggedDispatch({ type: 'SET_LAST_SYNC_TIME', payload: new Date() });
          } 
          // Fallback: Reload itinerary when chat updates are received (for backward compatibility)
          else if (message.data?.changes) {
            logInfo('Chat update contains changes, reloading itinerary via API', {
              component: 'UnifiedItineraryProvider',
              action: 'chat_update_reload',
              itineraryId
            });

            loadItinerary(itineraryId);
          }
          break;
        default:
          logWarn(`Unknown WebSocket message type: ${messageType}`, {
            component: 'UnifiedItineraryProvider',
            action: 'websocket_unknown_message',
            itineraryId,
            messageType: messageType
          });
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
      if (reloadTimeout) {
        clearTimeout(reloadTimeout);
      }

      logInfo('Cleaning up WebSocket connection', {
        component: 'UnifiedItineraryProvider',
        action: 'websocket_cleanup',
        itineraryId
      });

      webSocketService.off('message', handleMessage);
      webSocketService.offConnectionChange(handleConnectionChange);
      webSocketService.disconnect();
    };
  }, [itineraryId]); // REMOVED loadItinerary - it's captured in closure

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
  ]);

  return (
    <UnifiedItineraryContext.Provider value={contextValue}>
      {children}
    </UnifiedItineraryContext.Provider>
  );
}

export default UnifiedItineraryContext;

// Re-export types and hooks for backward compatibility
export type {
  ChatMessage,
  WorkflowNode,
  WorkflowEdge,
  WorkflowSettings,
  AgentDataSection,
  ChangeDetail,
  RevisionInfo,
  UnifiedItineraryState,
  UnifiedItineraryAction,
  UnifiedItineraryContextType
} from './UnifiedItineraryTypes';

export { useUnifiedItinerary, useUnifiedItinerarySelector } from './UnifiedItineraryHooks';
