/**
 * Enhanced SSE Manager for Change Events
 * Handles real-time updates for itinerary changes with optimistic UI updates
 */

import { apiClient } from './apiClient';
import { logger } from '../utils/logger';

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
      logger.debug('Already connected to itinerary', {
        component: 'SseManager',
        action: 'connect_skip',
        itineraryId
      });
      return;
    }

    // Disconnect existing connections
    this.disconnect();

    this.itineraryId = itineraryId;
    
    // Store executionId if provided
    if (executionId) {
      this.options.executionId = executionId;
    }
    
    logger.info('Connecting to SSE streams', {
      component: 'SseManager',
      action: 'connect_start',
      itineraryId,
      executionId
    });

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
        logger.warn('Patches stream error', {
          component: 'SseManager',
          action: 'patches_stream_error',
          itineraryId
        }, error);
        this.handleConnectionError();
      };

      this.patchesEventSource.onopen = () => {
        logger.info('Patches stream connected', {
          component: 'SseManager',
          action: 'patches_stream_connected',
          itineraryId
        });
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
          
          logger.debug('Patch applied', {
            component: 'SseManager',
            action: 'patch_applied',
            itineraryId,
            version: data.version
          });
          this.options.onChangeEvent?.(changeEvent);
        } catch (error) {
          logger.error('Error parsing patch_applied event', {
            component: 'SseManager',
            action: 'parse_error',
            eventType: 'patch_applied'
          }, error);
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
          
          logger.debug('Version updated', {
            component: 'SseManager',
            action: 'version_updated',
            itineraryId,
            version: data.version
          });
          this.options.onChangeEvent?.(changeEvent);
        } catch (error) {
          logger.error('Error parsing version_updated event', {
            component: 'SseManager',
            action: 'parse_error',
            eventType: 'version_updated'
          }, error);
        }
      });

      // Handle progress update events (from agent events)
      this.patchesEventSource.addEventListener('progress_update', (event: MessageEvent) => {
        try {
          const data = JSON.parse(event.data);
          logger.debug('Progress update (patches stream)', {
            component: 'SseManager',
            action: 'progress_update_patches',
            itineraryId
          });
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
          logger.error('Error parsing progress update (patches)', {
            component: 'SseManager',
            action: 'parse_error',
            eventType: 'progress_update_patches'
          }, error);
        }
      });

      // Handle generation complete events (from agent events)
      this.patchesEventSource.addEventListener('generation_complete', (event: MessageEvent) => {
        try {
          const data = JSON.parse(event.data);
          logger.info('Generation complete (patches stream)', {
            component: 'SseManager',
            action: 'generation_complete_patches',
            itineraryId
          });
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
          logger.error('Error parsing generation complete (patches)', {
            component: 'SseManager',
            action: 'parse_error',
            eventType: 'generation_complete_patches'
          }, error);
        }
      });

      // Handle agent started events (from agent events)
      this.patchesEventSource.addEventListener('agent_started', (event: MessageEvent) => {
        try {
          const data = JSON.parse(event.data);
          logger.info('Agent started (patches stream)', {
            component: 'SseManager',
            action: 'agent_started_patches',
            itineraryId
          });
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
          logger.error('Error parsing agent started (patches)', {
            component: 'SseManager',
            action: 'parse_error',
            eventType: 'agent_started_patches'
          }, error);
        }
      });

      // Handle agent completed events (from agent events)
      this.patchesEventSource.addEventListener('agent_completed', (event: MessageEvent) => {
        try {
          const data = JSON.parse(event.data);
          logger.info('Agent completed (patches stream)', {
            component: 'SseManager',
            action: 'agent_completed_patches',
            itineraryId
          });
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
          logger.error('Error parsing agent completed (patches)', {
            component: 'SseManager',
            action: 'parse_error',
            eventType: 'agent_completed_patches'
          }, error);
        }
      });

      // Handle agent failed events (from agent events)
      this.patchesEventSource.addEventListener('agent_failed', (event: MessageEvent) => {
        try {
          const data = JSON.parse(event.data);
          logger.warn('Agent failed (patches stream)', {
            component: 'SseManager',
            action: 'agent_failed_patches',
            itineraryId
          });
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
          logger.error('Error parsing agent failed (patches)', {
            component: 'SseManager',
            action: 'parse_error',
            eventType: 'agent_failed_patches'
          }, error);
        }
      });

      // Handle day completed events (from agent events)
      this.patchesEventSource.addEventListener('day_completed', (event: MessageEvent) => {
        try {
          const data = JSON.parse(event.data);
          logger.debug('Day completed (patches stream)', {
            component: 'SseManager',
            action: 'day_completed_patches',
            itineraryId
          });
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
          logger.error('Error parsing day completed (patches)', {
            component: 'SseManager',
            action: 'parse_error',
            eventType: 'day_completed_patches'
          }, error);
        }
      });

      // Handle node enhanced events (from agent events)
      this.patchesEventSource.addEventListener('node_enhanced', (event: MessageEvent) => {
        try {
          const data = JSON.parse(event.data);
          logger.debug('Node enhanced (patches stream)', {
            component: 'SseManager',
            action: 'node_enhanced_patches',
            itineraryId
          });
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
          logger.error('Error parsing node enhanced (patches)', {
            component: 'SseManager',
            action: 'parse_error',
            eventType: 'node_enhanced_patches'
          }, error);
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
          
          logger.debug('Node locked', {
            component: 'SseManager',
            action: 'node_locked',
            itineraryId
          });
          this.options.onChangeEvent?.(changeEvent);
        } catch (error) {
          logger.error('Error parsing node_locked event', {
            component: 'SseManager',
            action: 'parse_error',
            eventType: 'node_locked'
          }, error);
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
          
          logger.debug('Node unlocked', {
            component: 'SseManager',
            action: 'node_unlocked',
            itineraryId
          });
          this.options.onChangeEvent?.(changeEvent);
        } catch (error) {
          logger.error('Error parsing node_unlocked event', {
            component: 'SseManager',
            action: 'parse_error',
            eventType: 'node_unlocked'
          }, error);
        }
      });

      // Handle errors
      this.patchesEventSource.onerror = (error) => {
        logger.error('Patches stream error', {
          component: 'SseManager',
          action: 'patches_stream_error',
          itineraryId
        }, error);
        this.handleError(new Error('Patches SSE connection error'));
      };

      logger.info('Patches stream connected', {
        component: 'SseManager',
        action: 'patches_stream_ready',
        itineraryId
      });
    } catch (error) {
      logger.error('Failed to connect patches stream', {
        component: 'SseManager',
        action: 'patches_connect_failed',
        itineraryId
      }, error);
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
        logger.warn('Agent stream error', {
          component: 'SseManager',
          action: 'agent_stream_error',
          itineraryId
        }, error);
        this.handleConnectionError();
      };

      this.agentEventSource.onopen = () => {
        logger.info('Agent stream connected', {
          component: 'SseManager',
          action: 'agent_stream_connected',
          itineraryId
        });
        this.reconnectAttempts = 0;
      };

      // Handle progress update events
      this.agentEventSource.addEventListener('progress_update', (event: MessageEvent) => {
        try {
          const data = JSON.parse(event.data);
          logger.debug('Progress update (agent stream)', {
            component: 'SseManager',
            action: 'progress_update_agent',
            itineraryId
          });
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
          logger.error('Error parsing progress update (agent)', {
            component: 'SseManager',
            action: 'parse_error',
            eventType: 'progress_update_agent'
          }, error);
        }
      });

      // Handle generation complete events
      this.agentEventSource.addEventListener('generation_complete', (event: MessageEvent) => {
        try {
          const data = JSON.parse(event.data);
          logger.info('Generation complete (agent stream)', {
            component: 'SseManager',
            action: 'generation_complete_agent',
            itineraryId
          });
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
          logger.error('Error parsing generation complete (agent)', {
            component: 'SseManager',
            action: 'parse_error',
            eventType: 'generation_complete_agent'
          }, error);
        }
      });

      // Handle agent started events
      this.agentEventSource.addEventListener('agent_started', (event: MessageEvent) => {
        try {
          const data = JSON.parse(event.data);
          logger.info('Agent started (agent stream)', {
            component: 'SseManager',
            action: 'agent_started_agent',
            itineraryId
          });
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
          logger.error('Error parsing agent started (agent)', {
            component: 'SseManager',
            action: 'parse_error',
            eventType: 'agent_started_agent'
          }, error);
        }
      });

      // Handle agent completed events
      this.agentEventSource.addEventListener('agent_completed', (event: MessageEvent) => {
        try {
          const data = JSON.parse(event.data);
          logger.info('Agent completed (agent stream)', {
            component: 'SseManager',
            action: 'agent_completed_agent',
            itineraryId
          });
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
          logger.error('Error parsing agent completed (agent)', {
            component: 'SseManager',
            action: 'parse_error',
            eventType: 'agent_completed_agent'
          }, error);
        }
      });

      // Handle agent failed events
      this.agentEventSource.addEventListener('agent_failed', (event: MessageEvent) => {
        try {
          const data = JSON.parse(event.data);
          logger.warn('Agent failed (agent stream)', {
            component: 'SseManager',
            action: 'agent_failed_agent',
            itineraryId
          });
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
          logger.error('Error parsing agent failed (agent)', {
            component: 'SseManager',
            action: 'parse_error',
            eventType: 'agent_failed_agent'
          }, error);
        }
      });

      // Handle day completed events
      this.agentEventSource.addEventListener('day_completed', (event: MessageEvent) => {
        try {
          const data = JSON.parse(event.data);
          logger.debug('Day completed (agent stream)', {
            component: 'SseManager',
            action: 'day_completed_agent',
            itineraryId
          });
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
          logger.error('Error parsing day completed (agent)', {
            component: 'SseManager',
            action: 'parse_error',
            eventType: 'day_completed_agent'
          }, error);
        }
      });

      // Handle node enhanced events
      this.agentEventSource.addEventListener('node_enhanced', (event: MessageEvent) => {
        try {
          const data = JSON.parse(event.data);
          logger.debug('Node enhanced (agent stream)', {
            component: 'SseManager',
            action: 'node_enhanced_agent',
            itineraryId
          });
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
          logger.error('Error parsing node enhanced (agent)', {
            component: 'SseManager',
            action: 'parse_error',
            eventType: 'node_enhanced_agent'
          }, error);
        }
      });

      // Handle errors
      this.agentEventSource.onerror = (error) => {
        logger.error('Agent stream error', {
          component: 'SseManager',
          action: 'agent_stream_error',
          itineraryId
        }, error);
        this.handleError(new Error('Agent SSE connection error'));
      };

      logger.info('Agent stream connected', {
        component: 'SseManager',
        action: 'agent_stream_ready',
        itineraryId
      });
    } catch (error) {
      logger.error('Failed to connect agent stream', {
        component: 'SseManager',
        action: 'agent_connect_failed',
        itineraryId
      }, error);
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
      
      logger.info('Reconnecting SSE', {
        component: 'SseManager',
        action: 'reconnect',
        delay,
        attempt: this.reconnectAttempts,
        maxAttempts: this.maxReconnectAttempts
      });
      
      setTimeout(() => {
        if (this.itineraryId) {
          this.connect(this.itineraryId);
        }
      }, delay);
    } else {
      logger.error('Max reconnect attempts reached', {
        component: 'SseManager',
        action: 'reconnect_failed',
        attempts: this.reconnectAttempts
      });
      this.isConnected = false;
    }
  }

  /**
   * Handle connection errors with automatic reconnection
   */
  private async handleConnectionError(): Promise<void> {
    logger.warn('SSE connection error occurred', {
      component: 'SseManager',
      action: 'connection_error'
    });
    this.options.onError?.(new Error('SSE connection error'));
    
    if (this.options.autoReconnect && this.reconnectAttempts < this.maxReconnectAttempts) {
      this.reconnectAttempts++;
      const delay = this.options.reconnectDelay || 1000 * this.reconnectAttempts;
      
      logger.info('Attempting to reconnect SSE', {
        component: 'SseManager',
        action: 'reconnect_attempt',
        delay,
        attempt: this.reconnectAttempts,
        maxAttempts: this.maxReconnectAttempts
      });
      
      // Try to refresh the auth token before reconnecting
      try {
        const { authService } = await import('./authService');
        const newToken = await authService.getIdTokenForceRefresh();
        if (newToken) {
          logger.info('Token refreshed before reconnection', {
            component: 'SseManager',
            action: 'token_refresh_success'
          });
          apiClient.setAuthToken(newToken);
        }
      } catch (error) {
        logger.error('Failed to refresh token before reconnection', {
          component: 'SseManager',
          action: 'token_refresh_failed'
        }, error);
      }
      
      setTimeout(() => {
        if (this.itineraryId) {
          this.connect(this.itineraryId);
        }
      }, delay);
    } else {
      logger.error('Max reconnection attempts reached', {
        component: 'SseManager',
        action: 'reconnect_give_up',
        attempts: this.reconnectAttempts
      });
      this.disconnect();
    }
  }

  /**
   * Handle connection close events
   */
  private handleConnectionClose(): void {
    logger.info('SSE connection closed', {
      component: 'SseManager',
      action: 'connection_closed'
    });
    if (this.options.autoReconnect && this.reconnectAttempts < this.maxReconnectAttempts) {
      this.handleConnectionError();
    }
  }

  /**
   * Disconnect from all SSE streams
   */
  disconnect(): void {
    logger.info('Disconnecting SSE streams', {
      component: 'SseManager',
      action: 'disconnect',
      itineraryId: this.itineraryId
    });

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
