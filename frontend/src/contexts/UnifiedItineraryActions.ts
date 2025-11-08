import { Dispatch, useCallback } from 'react';
import { NormalizedDay, NormalizedNode } from '../types/NormalizedItinerary';
import { ChatRequest } from '../types/ChatTypes';
import { itineraryApi } from '../services/api';
import { webSocketService } from '../services/websocket';
import { logger, logInfo, logError, logWarn, logDebug, startTimer } from '../utils/logger';
import { 
  UnifiedItineraryState,
  UnifiedItineraryAction,
  ChatMessage,
  WorkflowSettings
} from './UnifiedItineraryTypes';

/**
 * Action creators for the Unified Itinerary Context
 * Extracted from UnifiedItineraryContext.tsx for better maintainability
 */

/**
 * Core itinerary actions
 */
export const createLoadItinerary = (
  dispatch: Dispatch<UnifiedItineraryAction>
) => {
  return async (id: string) => {
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
    
    dispatch({ type: 'SET_LOADING', payload: true });
    
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
      
      dispatch({ type: 'SET_ITINERARY', payload: itinerary });
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
      
      dispatch({ type: 'SET_ERROR', payload: errorMessage });
      timer();
    }
  };
};

export const createSaveItinerary = (
  state: UnifiedItineraryState,
  dispatch: Dispatch<UnifiedItineraryAction>,
  itineraryId: string
) => {
  return async () => {
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
      hasUnsavedChanges: state.pendingChanges.length > 0
    });
    
    dispatch({ type: 'SET_SYNC_STATUS', payload: 'syncing' });
    
    try {
      await itineraryApi.updateItinerary(state.itinerary.id, state.itinerary);
      
      logInfo(`Successfully saved itinerary: ${state.itinerary.id}`, {
        component: 'UnifiedItineraryProvider',
        action: 'save_itinerary_success',
        itineraryId: state.itinerary.id
      });
      
      dispatch({ type: 'SET_SYNC_STATUS', payload: 'idle' });
      dispatch({ type: 'SET_LAST_SYNC_TIME', payload: new Date() });
      dispatch({ type: 'CLEAR_PENDING_CHANGES' });
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
      
      dispatch({ type: 'SET_SYNC_STATUS', payload: 'error' });
      timer();
      throw error;
    }
  };
};

export const createUpdateDay = (dispatch: Dispatch<UnifiedItineraryAction>) => {
  return (dayIndex: number, day: NormalizedDay) => {
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
  };
};

export const createUpdateNode = (dispatch: Dispatch<UnifiedItineraryAction>) => {
  return (dayIndex: number, nodeIndex: number, node: NormalizedNode) => {
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
  };
};

export const createAddNode = (dispatch: Dispatch<UnifiedItineraryAction>) => {
  return (dayIndex: number, node: NormalizedNode, position?: number) => {
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
  };
};

export const createRemoveNode = (
  state: UnifiedItineraryState,
  dispatch: Dispatch<UnifiedItineraryAction>
) => {
  return (dayIndex: number, nodeIndex: number) => {
    logger.debug('removeNode called', { 
      component: 'UnifiedItineraryContext',
      dayIndex, 
      nodeIndex 
    });
    
    if (!state.itinerary?.itinerary?.days) {
      logger.error('No itinerary days data found', { 
        component: 'UnifiedItineraryContext' 
      });
      return;
    }
    
    const day = state.itinerary.itinerary.days[dayIndex];
    if (!day) {
      logger.error('Day not found at index', { 
        component: 'UnifiedItineraryContext',
        dayIndex 
      });
      return;
    }
    
    const component = day.components[nodeIndex];
    if (component) {
      logger.debug('Component found, dispatching REMOVE_NODE action', { 
        component: 'UnifiedItineraryContext',
        componentId: component.id 
      });
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
      logger.debug('REMOVE_NODE action dispatched successfully', { 
        component: 'UnifiedItineraryContext' 
      });
    } else {
      logger.error('Component not found', { 
        component: 'UnifiedItineraryContext',
        dayIndex, 
        nodeIndex 
      });
    }
  };
};

export const createMoveNode = (
  state: UnifiedItineraryState,
  dispatch: Dispatch<UnifiedItineraryAction>
) => {
  return (fromDay: number, fromIndex: number, toDay: number, toIndex: number) => {
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
  };
};

/**
 * Chat actions
 */
export const createSendChatMessage = (
  state: UnifiedItineraryState,
  dispatch: Dispatch<UnifiedItineraryAction>,
  itineraryId: string,
  loadItinerary: (id: string) => Promise<void>
) => {
  return async (message: string, selectedNodeId?: string) => {
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
    
    dispatch({ type: 'ADD_CHAT_MESSAGE', payload: userMessage });
    
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
    
    dispatch({ type: 'SET_CHAT_LOADING', payload: true });
    
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

      // Send via WebSocket for real-time response if connected, otherwise use REST API
      if (state.isConnected) {
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
        
        // WebSocket will handle the response via the message handler
        // No need to wait for REST API response
        dispatch({ type: 'SET_CHAT_LOADING', payload: false });
        timer();
        return;
      }
      
      // Fallback to REST API if WebSocket not connected
      logInfo('Sending chat message via REST API (WebSocket not connected)', {
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
      
      dispatch({ type: 'ADD_CHAT_MESSAGE', payload: assistantMessage });

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
      
      dispatch({ type: 'ADD_CHAT_MESSAGE', payload: errorMessage });
      dispatch({ type: 'SET_CHAT_ERROR', payload: error instanceof Error ? error.message : 'Failed to send message' });
      
      timer();
    } finally {
      dispatch({ type: 'SET_CHAT_LOADING', payload: false });
    }
  };
};

export const createClearChatMessages = (dispatch: Dispatch<UnifiedItineraryAction>) => {
  return () => {
    dispatch({ type: 'CLEAR_CHAT_MESSAGES' });
  };
};

export const createClearChatHistory = (
  dispatch: Dispatch<UnifiedItineraryAction>,
  itineraryId: string
) => {
  return async () => {
    if (!itineraryId) return;
    try {
      await itineraryApi.clearChatHistory(itineraryId);
      dispatch({ type: 'CLEAR_CHAT_MESSAGES' });
      logInfo('Chat history cleared', { component: 'UnifiedItineraryProvider', action: 'chat_history_cleared', itineraryId });
    } catch (e) {
      logWarn('Failed to clear chat history', { component: 'UnifiedItineraryProvider', action: 'chat_history_clear_failed', itineraryId }, e as any);
      throw e;
    }
  };
};

/**
 * Workflow actions
 */
export const createUpdateWorkflowSettings = (dispatch: Dispatch<UnifiedItineraryAction>) => {
  return (settings: Partial<WorkflowSettings>) => {
    dispatch({ type: 'UPDATE_WORKFLOW_SETTINGS', payload: settings });
  };
};

export const createSelectWorkflowNode = (dispatch: Dispatch<UnifiedItineraryAction>) => {
  return (nodeId: string | null) => {
    dispatch({ type: 'SELECT_WORKFLOW_NODE', payload: nodeId });
  };
};

/**
 * Agent actions
 */
export const createExecuteAgent = (
  state: UnifiedItineraryState,
  dispatch: Dispatch<UnifiedItineraryAction>,
  itineraryId: string,
  loadItinerary: (id: string) => Promise<void>
) => {
  return async (agentType: string, data: any) => {
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
  };
};

export const createProcessWithAgents = (
  state: UnifiedItineraryState,
  dispatch: Dispatch<UnifiedItineraryAction>,
  executeAgent: (agentType: string, data: any) => Promise<void>
) => {
  return async (nodeId: string, agentIds: string[]) => {
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
  };
};

/**
 * Revision actions
 */
export const createLoadRevisions = (
  dispatch: Dispatch<UnifiedItineraryAction>,
  itineraryId: string
) => {
  return async () => {
    try {
      // TODO: Implement revisions API when backend supports it
      const revisions = await Promise.resolve([]);
      dispatch({ type: 'SET_REVISIONS', payload: revisions });
    } catch (error) {
      logError('Failed to load revisions', {
        component: 'UnifiedItineraryContext',
        action: 'load_revisions_failed',
        itineraryId
      }, error);
    }
  };
};

export const createSwitchToRevision = (
  dispatch: Dispatch<UnifiedItineraryAction>,
  itineraryId: string
) => {
  return async (revisionId: string) => {
    try {
      // TODO: Implement revision retrieval API when backend supports it
      const itinerary = await itineraryApi.getItinerary(itineraryId);
      dispatch({ type: 'SET_ITINERARY', payload: itinerary });
      dispatch({ type: 'SET_CURRENT_REVISION', payload: revisionId });
    } catch (error) {
      logError('Failed to switch to revision', {
        component: 'UnifiedItineraryContext',
        action: 'switch_revision_failed',
        revisionId,
        itineraryId
      }, error);
    }
  };
};

export const createCreateRevision = (
  state: UnifiedItineraryState,
  dispatch: Dispatch<UnifiedItineraryAction>,
  itineraryId: string
) => {
  return async (description: string) => {
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
      logError('Failed to create revision', {
        component: 'UnifiedItineraryContext',
        action: 'create_revision_failed',
        description,
        itineraryId
      }, error);
    }
  };
};

export const createRollbackToRevision = (
  state: UnifiedItineraryState,
  dispatch: Dispatch<UnifiedItineraryAction>,
  itineraryId: string
) => {
  return async (revisionId: string) => {
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
      
      logInfo('Successfully rolled back to revision', {
        component: 'UnifiedItineraryContext',
        action: 'rollback_success',
        revisionId,
        itineraryId
      });
      
    } catch (error) {
      logError('Rollback failed', {
        component: 'UnifiedItineraryContext',
        action: 'rollback_failed',
        revisionId,
        itineraryId
      }, error);
      dispatch({ type: 'SET_SYNC_STATUS', payload: 'error' });
      
      // Optionally revert to previous state on failure
      // This would require storing previousState in a ref or state
      throw new Error(`Failed to rollback to revision: ${error instanceof Error ? error.message : 'Unknown error'}`);
    }
  };
};

/**
 * UI actions
 */
export const createSetSelectedDay = (dispatch: Dispatch<UnifiedItineraryAction>) => {
  return (day: number) => {
    dispatch({ type: 'SET_SELECTED_DAY', payload: day });
  };
};

export const createSetSelectedNodes = (dispatch: Dispatch<UnifiedItineraryAction>) => {
  return (nodeIds: string[]) => {
    dispatch({ type: 'SET_SELECTED_NODES', payload: nodeIds });
  };
};

export const createSetViewMode = (dispatch: Dispatch<UnifiedItineraryAction>) => {
  return (mode: 'day-by-day' | 'workflow' | 'timeline') => {
    dispatch({ type: 'SET_VIEW_MODE', payload: mode });
  };
};

export const createSetSidebarOpen = (dispatch: Dispatch<UnifiedItineraryAction>) => {
  return (open: boolean) => {
    dispatch({ type: 'SET_SIDEBAR_OPEN', payload: open });
  };
};
