/**
 * Enhanced SSE Manager for Change Events
 * Handles real-time updates for itinerary changes with optimistic UI updates
 */

import { apiClient } from './apiClient';

export type ChangeEventType = 
  | 'patch_applied'
  | 'patch_rejected'
  | 'version_updated'
  | 'node_locked'
  | 'node_unlocked'
  | 'agent_started'
  | 'agent_progress'
  | 'agent_completed'
  | 'agent_failed';

export interface ChangeEvent {
  type: ChangeEventType;
  itineraryId: string;
  timestamp: string;
  data: any;
  version?: number;
  userId?: string;
}

export interface SseManagerOptions {
  onChangeEvent?: (event: ChangeEvent) => void;
  onAgentEvent?: (event: any) => void;
  onError?: (error: Error) => void;
  onConnect?: () => void;
  onDisconnect?: () => void;
  autoReconnect?: boolean;
  reconnectDelay?: number;
  executionId?: string;
}

export class SseManager {
  private patchesEventSource: EventSource | null = null;
  private agentEventSource: EventSource | null = null;
  private itineraryId: string | null = null;
  private options: SseManagerOptions;
  private reconnectAttempts = 0;
  private maxReconnectAttempts = 5;
  private isConnected = false;

  constructor(options: SseManagerOptions = {}) {
    this.options = {
      autoReconnect: true,
      reconnectDelay: 3000,
      ...options,
    };
  }

  /**
   * Connect to SSE streams for an itinerary
   */
  connect(itineraryId: string, executionId?: string): void {
    if (this.itineraryId === itineraryId && this.isConnected) {
      console.log('[SSE] Already connected to itinerary:', itineraryId);
      return;
    }

    // Disconnect existing connections
    this.disconnect();

    this.itineraryId = itineraryId;
    
    // Store executionId if provided
    if (executionId) {
      this.options.executionId = executionId;
    }
    
    console.log('[SSE] Connecting to itinerary:', itineraryId, 'executionId:', executionId);

    // Connect to patches stream
    this.connectPatchesStream(itineraryId);

    // Connect to agent stream
    this.connectAgentStream(itineraryId);

    this.isConnected = true;
    this.reconnectAttempts = 0;
    this.options.onConnect?.();
  }

  /**
   * Connect to patches event stream
   */
  private connectPatchesStream(itineraryId: string): void {
    try {
      this.patchesEventSource = apiClient.createPatchesEventStream(itineraryId, this.options.executionId);

      // Handle connection errors
      this.patchesEventSource.onerror = (error) => {
        console.warn('[SSE] Patches stream error:', error);
        this.handleConnectionError();
      };

      this.patchesEventSource.onopen = () => {
        console.log('[SSE] Patches stream connected');
        this.reconnectAttempts = 0;
      };

      // Handle patch_applied events
      this.patchesEventSource.addEventListener('patch_applied', (event: MessageEvent) => {
        try {
          const data = JSON.parse(event.data);
          const changeEvent: ChangeEvent = {
            type: 'patch_applied',
            itineraryId,
            timestamp: new Date().toISOString(),
            data,
            version: data.version,
            userId: data.userId,
          };
          
          console.log('[SSE] Patch applied:', changeEvent);
          this.options.onChangeEvent?.(changeEvent);
        } catch (error) {
          console.error('[SSE] Error parsing patch_applied event:', error);
        }
      });

      // Handle version_updated events
      this.patchesEventSource.addEventListener('version_updated', (event: MessageEvent) => {
        try {
          const data = JSON.parse(event.data);
          const changeEvent: ChangeEvent = {
            type: 'version_updated',
            itineraryId,
            timestamp: new Date().toISOString(),
            data,
            version: data.version,
          };
          
          console.log('[SSE] Version updated:', changeEvent);
          this.options.onChangeEvent?.(changeEvent);
        } catch (error) {
          console.error('[SSE] Error parsing version_updated event:', error);
        }
      });

      // Handle progress update events (from agent events)
      this.patchesEventSource.addEventListener('progress_update', (event: MessageEvent) => {
        try {
          const data = JSON.parse(event.data);
          console.log('[SSE] Progress update:', data);
          this.options.onAgentEvent?.(data);

          // Emit as change event for progress updates
          const changeEvent: ChangeEvent = {
            type: 'agent_progress',
            itineraryId,
            timestamp: new Date().toISOString(),
            data,
          };
          this.options.onChangeEvent?.(changeEvent);
        } catch (error) {
          console.error('[SSE] Error parsing progress update:', error);
        }
      });

      // Handle generation complete events (from agent events)
      this.patchesEventSource.addEventListener('generation_complete', (event: MessageEvent) => {
        try {
          const data = JSON.parse(event.data);
          console.log('[SSE] Generation complete:', data);
          this.options.onAgentEvent?.(data);

          // Emit as change event for completion
          const changeEvent: ChangeEvent = {
            type: 'agent_completed',
            itineraryId,
            timestamp: new Date().toISOString(),
            data,
          };
          this.options.onChangeEvent?.(changeEvent);
        } catch (error) {
          console.error('[SSE] Error parsing generation complete:', error);
        }
      });

      // Handle agent started events (from agent events)
      this.patchesEventSource.addEventListener('agent_started', (event: MessageEvent) => {
        try {
          const data = JSON.parse(event.data);
          console.log('[SSE] Agent started:', data);
          this.options.onAgentEvent?.(data);

          // Emit as change event for agent started
          const changeEvent: ChangeEvent = {
            type: 'agent_started',
            itineraryId,
            timestamp: new Date().toISOString(),
            data,
          };
          this.options.onChangeEvent?.(changeEvent);
        } catch (error) {
          console.error('[SSE] Error parsing agent started:', error);
        }
      });

      // Handle agent completed events (from agent events)
      this.patchesEventSource.addEventListener('agent_completed', (event: MessageEvent) => {
        try {
          const data = JSON.parse(event.data);
          console.log('[SSE] Agent completed:', data);
          this.options.onAgentEvent?.(data);

          // Emit as change event for agent completed
          const changeEvent: ChangeEvent = {
            type: 'agent_completed',
            itineraryId,
            timestamp: new Date().toISOString(),
            data,
          };
          this.options.onChangeEvent?.(changeEvent);
        } catch (error) {
          console.error('[SSE] Error parsing agent completed:', error);
        }
      });

      // Handle agent failed events (from agent events)
      this.patchesEventSource.addEventListener('agent_failed', (event: MessageEvent) => {
        try {
          const data = JSON.parse(event.data);
          console.log('[SSE] Agent failed:', data);
          this.options.onAgentEvent?.(data);

          // Emit as change event for agent failed
          const changeEvent: ChangeEvent = {
            type: 'agent_failed',
            itineraryId,
            timestamp: new Date().toISOString(),
            data,
          };
          this.options.onChangeEvent?.(changeEvent);
        } catch (error) {
          console.error('[SSE] Error parsing agent failed:', error);
        }
      });

      // Handle day completed events (from agent events)
      this.patchesEventSource.addEventListener('day_completed', (event: MessageEvent) => {
        try {
          const data = JSON.parse(event.data);
          console.log('[SSE] Day completed:', data);
          this.options.onAgentEvent?.(data);

          // Emit as change event for day completed
          const changeEvent: ChangeEvent = {
            type: 'agent_progress',
            itineraryId,
            timestamp: new Date().toISOString(),
            data,
          };
          this.options.onChangeEvent?.(changeEvent);
        } catch (error) {
          console.error('[SSE] Error parsing day completed:', error);
        }
      });

      // Handle node enhanced events (from agent events)
      this.patchesEventSource.addEventListener('node_enhanced', (event: MessageEvent) => {
        try {
          const data = JSON.parse(event.data);
          console.log('[SSE] Node enhanced:', data);
          this.options.onAgentEvent?.(data);

          // Emit as change event for node enhanced
          const changeEvent: ChangeEvent = {
            type: 'agent_progress',
            itineraryId,
            timestamp: new Date().toISOString(),
            data,
          };
          this.options.onChangeEvent?.(changeEvent);
        } catch (error) {
          console.error('[SSE] Error parsing node enhanced:', error);
        }
      });

      // Handle node lock events
      this.patchesEventSource.addEventListener('node_locked', (event: MessageEvent) => {
        try {
          const data = JSON.parse(event.data);
          const changeEvent: ChangeEvent = {
            type: 'node_locked',
            itineraryId,
            timestamp: new Date().toISOString(),
            data,
          };
          
          console.log('[SSE] Node locked:', changeEvent);
          this.options.onChangeEvent?.(changeEvent);
        } catch (error) {
          console.error('[SSE] Error parsing node_locked event:', error);
        }
      });

      this.patchesEventSource.addEventListener('node_unlocked', (event: MessageEvent) => {
        try {
          const data = JSON.parse(event.data);
          const changeEvent: ChangeEvent = {
            type: 'node_unlocked',
            itineraryId,
            timestamp: new Date().toISOString(),
            data,
          };
          
          console.log('[SSE] Node unlocked:', changeEvent);
          this.options.onChangeEvent?.(changeEvent);
        } catch (error) {
          console.error('[SSE] Error parsing node_unlocked event:', error);
        }
      });

      // Handle errors
      this.patchesEventSource.onerror = (error) => {
        console.error('[SSE] Patches stream error:', error);
        this.handleError(new Error('Patches SSE connection error'));
      };

      console.log('[SSE] Patches stream connected');
    } catch (error) {
      console.error('[SSE] Failed to connect patches stream:', error);
      this.handleError(error as Error);
    }
  }

  /**
   * Connect to agent event stream
   */
  private connectAgentStream(itineraryId: string): void {
    try {
      this.agentEventSource = apiClient.createAgentEventStream(itineraryId);

      // Handle connection errors
      this.agentEventSource.onerror = (error) => {
        console.warn('[SSE] Agent stream error:', error);
        this.handleConnectionError();
      };

      this.agentEventSource.onopen = () => {
        console.log('[SSE] Agent stream connected');
        this.reconnectAttempts = 0;
      };

      // Handle progress update events
      this.agentEventSource.addEventListener('progress_update', (event: MessageEvent) => {
        try {
          const data = JSON.parse(event.data);
          console.log('[SSE] Progress update:', data);
          this.options.onAgentEvent?.(data);

          // Emit as change event for progress updates
          const changeEvent: ChangeEvent = {
            type: 'agent_progress',
            itineraryId,
            timestamp: new Date().toISOString(),
            data,
          };
          this.options.onChangeEvent?.(changeEvent);
        } catch (error) {
          console.error('[SSE] Error parsing progress update:', error);
        }
      });

      // Handle generation complete events
      this.agentEventSource.addEventListener('generation_complete', (event: MessageEvent) => {
        try {
          const data = JSON.parse(event.data);
          console.log('[SSE] Generation complete:', data);
          this.options.onAgentEvent?.(data);

          // Emit as change event for completion
          const changeEvent: ChangeEvent = {
            type: 'agent_completed',
            itineraryId,
            timestamp: new Date().toISOString(),
            data,
          };
          this.options.onChangeEvent?.(changeEvent);
        } catch (error) {
          console.error('[SSE] Error parsing generation complete:', error);
        }
      });

      // Handle agent started events
      this.agentEventSource.addEventListener('agent_started', (event: MessageEvent) => {
        try {
          const data = JSON.parse(event.data);
          console.log('[SSE] Agent started:', data);
          this.options.onAgentEvent?.(data);

          // Emit as change event for agent started
          const changeEvent: ChangeEvent = {
            type: 'agent_started',
            itineraryId,
            timestamp: new Date().toISOString(),
            data,
          };
          this.options.onChangeEvent?.(changeEvent);
        } catch (error) {
          console.error('[SSE] Error parsing agent started:', error);
        }
      });

      // Handle agent completed events
      this.agentEventSource.addEventListener('agent_completed', (event: MessageEvent) => {
        try {
          const data = JSON.parse(event.data);
          console.log('[SSE] Agent completed:', data);
          this.options.onAgentEvent?.(data);

          // Emit as change event for agent completed
          const changeEvent: ChangeEvent = {
            type: 'agent_completed',
            itineraryId,
            timestamp: new Date().toISOString(),
            data,
          };
          this.options.onChangeEvent?.(changeEvent);
        } catch (error) {
          console.error('[SSE] Error parsing agent completed:', error);
        }
      });

      // Handle agent failed events
      this.agentEventSource.addEventListener('agent_failed', (event: MessageEvent) => {
        try {
          const data = JSON.parse(event.data);
          console.log('[SSE] Agent failed:', data);
          this.options.onAgentEvent?.(data);

          // Emit as change event for agent failed
          const changeEvent: ChangeEvent = {
            type: 'agent_failed',
            itineraryId,
            timestamp: new Date().toISOString(),
            data,
          };
          this.options.onChangeEvent?.(changeEvent);
        } catch (error) {
          console.error('[SSE] Error parsing agent failed:', error);
        }
      });

      // Handle day completed events
      this.agentEventSource.addEventListener('day_completed', (event: MessageEvent) => {
        try {
          const data = JSON.parse(event.data);
          console.log('[SSE] Day completed:', data);
          this.options.onAgentEvent?.(data);

          // Emit as change event for day completed
          const changeEvent: ChangeEvent = {
            type: 'agent_progress',
            itineraryId,
            timestamp: new Date().toISOString(),
            data,
          };
          this.options.onChangeEvent?.(changeEvent);
        } catch (error) {
          console.error('[SSE] Error parsing day completed:', error);
        }
      });

      // Handle node enhanced events
      this.agentEventSource.addEventListener('node_enhanced', (event: MessageEvent) => {
        try {
          const data = JSON.parse(event.data);
          console.log('[SSE] Node enhanced:', data);
          this.options.onAgentEvent?.(data);

          // Emit as change event for node enhanced
          const changeEvent: ChangeEvent = {
            type: 'agent_progress',
            itineraryId,
            timestamp: new Date().toISOString(),
            data,
          };
          this.options.onChangeEvent?.(changeEvent);
        } catch (error) {
          console.error('[SSE] Error parsing node enhanced:', error);
        }
      });

      // Handle errors
      this.agentEventSource.onerror = (error) => {
        console.error('[SSE] Agent stream error:', error);
        this.handleError(new Error('Agent SSE connection error'));
      };

      console.log('[SSE] Agent stream connected');
    } catch (error) {
      console.error('[SSE] Failed to connect agent stream:', error);
      this.handleError(error as Error);
    }
  }

  /**
   * Handle SSE errors with auto-reconnect
   */
  private handleError(error: Error): void {
    this.options.onError?.(error);

    if (this.options.autoReconnect && this.reconnectAttempts < this.maxReconnectAttempts) {
      this.reconnectAttempts++;
      const delay = this.options.reconnectDelay! * this.reconnectAttempts;
      
      console.log(`[SSE] Reconnecting in ${delay}ms (attempt ${this.reconnectAttempts}/${this.maxReconnectAttempts})`);
      
      setTimeout(() => {
        if (this.itineraryId) {
          this.connect(this.itineraryId);
        }
      }, delay);
    } else {
      console.error('[SSE] Max reconnect attempts reached or auto-reconnect disabled');
      this.isConnected = false;
    }
  }

  /**
   * Handle connection errors with automatic reconnection
   */
  private async handleConnectionError(): Promise<void> {
    console.warn('[SSE] Connection error occurred');
    this.options.onError?.(new Error('SSE connection error'));
    
    if (this.options.autoReconnect && this.reconnectAttempts < this.maxReconnectAttempts) {
      this.reconnectAttempts++;
      const delay = this.options.reconnectDelay || 1000 * this.reconnectAttempts;
      
      console.log(`[SSE] Attempting to reconnect in ${delay}ms (attempt ${this.reconnectAttempts}/${this.maxReconnectAttempts})`);
      
      // Try to refresh the auth token before reconnecting
      try {
        const { authService } = await import('./authService');
        const newToken = await authService.getIdTokenForceRefresh();
        if (newToken) {
          console.log('[SSE] Token refreshed before reconnection');
          apiClient.setAuthToken(newToken);
        }
      } catch (error) {
        console.error('[SSE] Failed to refresh token before reconnection:', error);
      }
      
      setTimeout(() => {
        if (this.itineraryId) {
          this.connect(this.itineraryId);
        }
      }, delay);
    } else {
      console.error('[SSE] Max reconnection attempts reached, giving up');
      this.disconnect();
    }
  }

  /**
   * Handle connection close events
   */
  private handleConnectionClose(): void {
    console.log('[SSE] Connection closed');
    if (this.options.autoReconnect && this.reconnectAttempts < this.maxReconnectAttempts) {
      this.handleConnectionError();
    }
  }

  /**
   * Disconnect from all SSE streams
   */
  disconnect(): void {
    console.log('[SSE] Disconnecting...');

    if (this.patchesEventSource) {
      this.patchesEventSource.close();
      this.patchesEventSource = null;
    }

    if (this.agentEventSource) {
      this.agentEventSource.close();
      this.agentEventSource = null;
    }

    this.isConnected = false;
    this.itineraryId = null;
    this.reconnectAttempts = 0;
    this.options.onDisconnect?.();
  }

  /**
   * Check if connected
   */
  getIsConnected(): boolean {
    return this.isConnected;
  }

  /**
   * Get current itinerary ID
   */
  getCurrentItineraryId(): string | null {
    return this.itineraryId;
  }
}

// Create singleton instance
export const sseManager = new SseManager();
