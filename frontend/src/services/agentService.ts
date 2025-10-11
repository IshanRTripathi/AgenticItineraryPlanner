/**
 * Agent Service
 * Handles agent execution with lock enforcement
 */

import { apiClient } from './apiClient';

export interface AgentExecutionRequest {
  itineraryId: string;
  agentType: string;
  parameters?: Record<string, any>;
  respectLocks?: boolean;
}

export interface AgentExecutionResponse {
  success: boolean;
  message?: string;
  lockedNodes?: string[];
  changes?: any;
}

class AgentService {
  /**
   * Execute agent with lock checking
   */
  async executeAgent(request: AgentExecutionRequest): Promise<AgentExecutionResponse> {
    const { itineraryId, agentType, parameters, respectLocks = true } = request;

    try {
      // Get lock states if respect locks is enabled
      if (respectLocks) {
        const lockStates = await apiClient.getLockStates(itineraryId);
        const lockedNodes = Object.entries(lockStates)
          .filter(([_, isLocked]) => isLocked)
          .map(([nodeId]) => nodeId);

        if (lockedNodes.length > 0) {
          // Warn about locked nodes
          console.warn(`Agent execution will skip ${lockedNodes.length} locked nodes:`, lockedNodes);
          
          // Return warning response
          return {
            success: false,
            message: `Cannot execute agent: ${lockedNodes.length} nodes are locked`,
            lockedNodes,
          };
        }
      }

      // Execute agent via API
      const response = await fetch(
        `${apiClient['baseUrl']}/itineraries/${itineraryId}/agents/${agentType}/execute`,
        {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            ...(apiClient['authToken'] ? { Authorization: `Bearer ${apiClient['authToken']}` } : {}),
          },
          body: JSON.stringify(parameters || {}),
        }
      );

      if (!response.ok) {
        throw new Error(`Agent execution failed: ${response.statusText}`);
      }

      const result = await response.json();
      return {
        success: true,
        changes: result,
      };
    } catch (error) {
      console.error('Agent execution error:', error);
      return {
        success: false,
        message: error instanceof Error ? error.message : 'Unknown error',
      };
    }
  }

  /**
   * Execute agent with override (ignore locks)
   */
  async executeAgentWithOverride(
    itineraryId: string,
    agentType: string,
    parameters?: Record<string, any>
  ): Promise<AgentExecutionResponse> {
    return this.executeAgent({
      itineraryId,
      agentType,
      parameters,
      respectLocks: false,
    });
  }

  /**
   * Check if agent can execute (no locked nodes)
   */
  async canExecuteAgent(itineraryId: string): Promise<{ canExecute: boolean; lockedNodes: string[] }> {
    try {
      const lockStates = await apiClient.getLockStates(itineraryId);
      const lockedNodes = Object.entries(lockStates)
        .filter(([_, isLocked]) => isLocked)
        .map(([nodeId]) => nodeId);

      return {
        canExecute: lockedNodes.length === 0,
        lockedNodes,
      };
    } catch (error) {
      console.error('Failed to check agent execution status:', error);
      return {
        canExecute: false,
        lockedNodes: [],
      };
    }
  }
}

export const agentService = new AgentService();
