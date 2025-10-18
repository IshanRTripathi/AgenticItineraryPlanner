/**
 * WebSocket service for real-time updates using STOMP protocol
 */

import { Client, IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

export interface WebSocketMessage {
  type: 'itinerary_updated' | 'agent_progress' | 'chat_response' | 'error' | 'connection_status';
  data?: any;
  agentId?: string;
  progress?: number;
  timestamp: string;
}

export interface WebSocketConfig {
  url?: string;
  reconnectInterval?: number;
  maxReconnectAttempts?: number;
  heartbeatInterval?: number;
}

export type ConnectionState = 'connecting' | 'connected' | 'disconnected' | 'error';

class WebSocketService {
  private client: Client | null = null;
  private config: Required<WebSocketConfig>;
  private reconnectAttempts = 0;
  private eventListeners: Map<string, Function[]> = new Map();
  private connectionHandlers: Set<(connected: boolean) => void> = new Set();
  private connectionState: ConnectionState = 'disconnected';
  private currentItineraryId: string | null = null;
  private subscriptions: Map<string, any> = new Map();

  // Connection management
  private connectionPromise: Promise<void> | null = null;
  private isConnecting = false;
  private lastConnectionAttempt = 0;
  private minConnectionInterval = 5000; // Minimum 5 seconds between connection attempts
  private connectionTimeout: NodeJS.Timeout | null = null;
  private maxConcurrentConnections = 1; // Prevent multiple connections
  private connectionDeduplicationMap = new Map<string, Promise<void>>();

  constructor(config: WebSocketConfig = {}) {
    this.config = {
      url: config.url || this.getWebSocketUrl(),
      reconnectInterval: config.reconnectInterval || 3000,
      maxReconnectAttempts: config.maxReconnectAttempts || 10,
      heartbeatInterval: config.heartbeatInterval || 30000,
    };

    console.log('[WebSocketService] Initialized with config:', this.config);
  }

  private getWebSocketUrl(): string {
    // SockJS expects HTTP/HTTPS URLs, not WebSocket URLs
    const protocol = window.location.protocol === 'https:' ? 'https:' : 'http:';
    const hostname = window.location.hostname;
    const baseUrl = import.meta.env.VITE_WS_BASE_URL || `${protocol}//${hostname}:8080`;

    return `${baseUrl}/ws`;
  }

  /**
   * Connect to WebSocket for a specific itinerary
   */
  connect(itineraryId: string): Promise<void> {
    // Check if already connected to the same itinerary
    if (this.client && this.client.connected && this.currentItineraryId === itineraryId) {
      console.log('[WebSocket] Already connected to itinerary:', itineraryId);
      return Promise.resolve();
    }

    // Connection deduplication - prevent multiple connections to same itinerary
    const existingConnection = this.connectionDeduplicationMap.get(itineraryId);
    if (existingConnection) {
      console.log('[WebSocket] Reusing existing connection promise for:', itineraryId);
      return existingConnection;
    }

    // Prevent multiple simultaneous connection attempts
    if (this.isConnecting) {
      console.log('[WebSocket] Connection already in progress, returning existing promise');
      return this.connectionPromise || Promise.resolve();
    }

    // Throttle connection attempts
    const now = Date.now();
    if (now - this.lastConnectionAttempt < this.minConnectionInterval) {
      const waitTime = this.minConnectionInterval - (now - this.lastConnectionAttempt);
      console.log(`[WebSocket] Throttling connection attempt, waiting ${waitTime}ms`);
      return new Promise((resolve) => {
        setTimeout(() => {
          this.connect(itineraryId).then(resolve);
        }, waitTime);
      });
    }

    this.lastConnectionAttempt = now;

    // Set connection state
    this.isConnecting = true;
    this.currentItineraryId = itineraryId;
    this.connectionState = 'connecting';
    this.notifyConnectionHandlers(false);

    // Disconnect existing client if any
    if (this.client) {
      try {
        this.client.deactivate();
      } catch (error) {
        console.warn('[WebSocket] Error deactivating existing client:', error);
      }
      this.client = null;
    }

    // Clear any existing connection timeout
    if (this.connectionTimeout) {
      clearTimeout(this.connectionTimeout);
      this.connectionTimeout = null;
    }

    // Create connection promise and add to deduplication map
    this.connectionPromise = new Promise<void>((resolve, reject) => {
      // Set overall connection timeout
      this.connectionTimeout = setTimeout(() => {
        console.error('[WebSocket] Connection timeout after 15 seconds');
        this.isConnecting = false;
        this.connectionState = 'error';
        this.notifyConnectionHandlers(false);
        reject(new Error('Connection timeout'));
      }, 15000);

      // Create new STOMP client
      console.log('[WebSocket] Creating STOMP client with URL:', this.config.url);
      this.client = new Client({
        webSocketFactory: () => {
          console.log('[WebSocket] Creating SockJS connection to:', this.config.url);
          return new SockJS(this.config.url, null, {
            transports: ['websocket', 'xhr-polling'],
            timeout: 10000
          });
        },
        connectHeaders: {},
        debug: (str) => {
          // Only log important STOMP events to reduce noise
          if (str.includes('Opening Web Socket') ||
            str.includes('CONNECT') ||
            str.includes('CONNECTED') ||
            str.includes('ERROR') ||
            str.includes('DISCONNECT')) {
            console.log('[STOMP Debug]', str);
          }
        },
        reconnectDelay: 0, // Disable automatic reconnection - we'll handle it manually
        heartbeatIncoming: this.config.heartbeatInterval,
        heartbeatOutgoing: this.config.heartbeatInterval,
        connectionTimeout: 10000,
      });

      // Set up event handlers
      this.client.onConnect = (frame) => {
        console.log('[WebSocket] Connected successfully:', frame);
        this.connectionState = 'connected';
        this.reconnectAttempts = 0;
        this.isConnecting = false;

        // Clear connection timeout
        if (this.connectionTimeout) {
          clearTimeout(this.connectionTimeout);
          this.connectionTimeout = null;
        }

        this.notifyConnectionHandlers(true);
        this.subscribeToTopics();
        this.emit('connected', frame);
        resolve(); // Resolve the connection promise
      };

      this.client.onDisconnect = (frame) => {
        console.log('[WebSocket] Disconnected:', frame);
        this.connectionState = 'disconnected';
        this.isConnecting = false;
        this.notifyConnectionHandlers(false);
        this.emit('disconnected', frame);
      };

      this.client.onStompError = (frame) => {
        console.error('[WebSocket] STOMP error:', frame);
        this.connectionState = 'error';
        this.isConnecting = false;

        // Clear connection timeout
        if (this.connectionTimeout) {
          clearTimeout(this.connectionTimeout);
          this.connectionTimeout = null;
        }

        this.notifyConnectionHandlers(false);
        this.emit('error', frame);
        reject(new Error(`STOMP error: ${frame.headers?.message || 'Unknown error'}`));
      };

      this.client.onWebSocketError = (event) => {
        console.error('[WebSocket] WebSocket error:', event);
        this.connectionState = 'error';
        this.isConnecting = false;

        // Clear connection timeout
        if (this.connectionTimeout) {
          clearTimeout(this.connectionTimeout);
          this.connectionTimeout = null;
        }

        this.notifyConnectionHandlers(false);
        this.emit('error', event);
        reject(new Error(`WebSocket error: ${event}`));
      };

      this.client.onWebSocketClose = (event) => {
        console.log('[WebSocket] WebSocket closed:', event);
        this.connectionState = 'disconnected';
        this.isConnecting = false;
        this.notifyConnectionHandlers(false);
      };

      // Activate the client
      try {
        this.client.activate();
      } catch (error) {
        console.error('[WebSocket] Error activating client:', error);
        this.isConnecting = false;
        this.connectionState = 'error';
        this.connectionDeduplicationMap.delete(itineraryId);
        reject(error);
      }
    });

    // Add to deduplication map
    this.connectionDeduplicationMap.set(itineraryId, this.connectionPromise);

    // Clean up deduplication map on completion
    this.connectionPromise.finally(() => {
      this.connectionDeduplicationMap.delete(itineraryId);
    });

    return this.connectionPromise;

  }

  /**
   * Subscribe to relevant topics for the current itinerary
   */
  private subscribeToTopics(): void {
    if (!this.client || !this.client.connected || !this.currentItineraryId) {
      return;
    }

    // Clear existing subscriptions
    this.subscriptions.forEach((subscription) => {
      subscription.unsubscribe();
    });
    this.subscriptions.clear();

    // Subscribe to itinerary-specific updates
    const itineraryTopic = `/topic/itinerary/${this.currentItineraryId}`;
    const itinerarySubscription = this.client.subscribe(itineraryTopic, (message: IMessage) => {
      this.handleMessage(message);
    });
    this.subscriptions.set('itinerary', itinerarySubscription);

    // Subscribe to agent progress updates
    const agentTopic = `/topic/agent/${this.currentItineraryId}`;
    const agentSubscription = this.client.subscribe(agentTopic, (message: IMessage) => {
      this.handleMessage(message);
    });
    this.subscriptions.set('agent', agentSubscription);

    // Subscribe to chat responses
    const chatTopic = `/topic/chat/${this.currentItineraryId}`;
    const chatSubscription = this.client.subscribe(chatTopic, (message: IMessage) => {
      this.handleMessage(message);
    });
    this.subscriptions.set('chat', chatSubscription);

    console.log('[WebSocket] Subscribed to topics for itinerary:', this.currentItineraryId);
  }

  /**
   * Handle incoming STOMP messages
   */
  private handleMessage(message: IMessage): void {
    try {
      const data = JSON.parse(message.body);
      console.log('[WebSocket] Message received:', data);

      const wsMessage: WebSocketMessage = {
        type: data.type || 'connection_status',
        data: data.data,
        agentId: data.agentId,
        progress: data.progress,
        timestamp: data.timestamp || new Date().toISOString()
      };

      this.emit('message', wsMessage);

      // Emit specific event types
      if (wsMessage.type) {
        this.emit(wsMessage.type, wsMessage);
      }
    } catch (error) {
      console.error('[WebSocket] Failed to parse message:', error, message.body);
      this.emit('error', error);
    }
  }

  /**
   * Send a message to the server
   */
  sendMessage(destination: string, body: any, headers: any = {}): void {
    if (!this.client || !this.client.connected) {
      console.warn('[WebSocket] Cannot send message - not connected');
      return;
    }

    try {
      this.client.publish({
        destination,
        body: JSON.stringify(body),
        headers
      });
      console.log('[WebSocket] Message sent:', { destination, body });
    } catch (error) {
      console.error('[WebSocket] Failed to send message:', error);
      this.emit('error', error);
    }
  }

  /**
   * Send a chat message
   */
  sendChatMessage(message: string, context?: any): void {
    if (!this.currentItineraryId) {
      console.warn('[WebSocket] Cannot send chat message - no itinerary ID');
      return;
    }

    this.sendMessage('/app/chat', {
      itineraryId: this.currentItineraryId,
      message,
      context,
      timestamp: new Date().toISOString()
    });
  }

  /**
   * Disconnect from WebSocket
   */
  disconnect(): void {
    console.log('[WebSocket] Disconnecting...');

    if (this.client) {
      // Unsubscribe from all topics
      this.subscriptions.forEach((subscription) => {
        try {
          subscription.unsubscribe();
        } catch (error) {
          console.warn('[WebSocket] Error unsubscribing:', error);
        }
      });
      this.subscriptions.clear();

      // Deactivate the client
      try {
        this.client.deactivate();
      } catch (error) {
        console.warn('[WebSocket] Error deactivating client:', error);
      }
      this.client = null;
    }

    this.connectionState = 'disconnected';
    this.currentItineraryId = null;
    this.reconnectAttempts = 0;
    this.notifyConnectionHandlers(false);
    console.log('[WebSocket] Disconnected');
  }

  /**
   * Force disconnect and reset connection state
   */
  forceDisconnect(): void {
    console.log('[WebSocket] Force disconnecting...');
    this.disconnect();

    // Clear any pending reconnection attempts
    if (this.client) {
      this.client.forceDisconnect();
    }
  }

  /**
   * Get current connection state
   */
  getConnectionState(): ConnectionState {
    return this.connectionState;
  }

  /**
   * Check if connected
   */
  isConnected(): boolean {
    return this.client?.connected || false;
  }

  /**
   * Add event listener
   */
  on(event: string, listener: Function): void {
    if (!this.eventListeners.has(event)) {
      this.eventListeners.set(event, []);
    }
    this.eventListeners.get(event)!.push(listener);
  }

  /**
   * Remove event listener
   */
  off(event: string, listener: Function): void {
    const listeners = this.eventListeners.get(event);
    if (listeners) {
      const index = listeners.indexOf(listener);
      if (index > -1) {
        listeners.splice(index, 1);
      }
    }
  }

  /**
   * Emit event to listeners
   */
  private emit(event: string, data?: any): void {
    const listeners = this.eventListeners.get(event);
    if (listeners) {
      listeners.forEach(listener => {
        try {
          listener(data);
        } catch (error) {
          console.error('[WebSocket] Error in event listener:', error);
        }
      });
    }
  }

  /**
   * Add connection state handler
   */
  onConnectionChange(handler: (connected: boolean) => void): void {
    this.connectionHandlers.add(handler);
  }

  /**
   * Remove connection state handler
   */
  offConnectionChange(handler: (connected: boolean) => void): void {
    this.connectionHandlers.delete(handler);
  }

  /**
   * Notify connection handlers
   */
  private notifyConnectionHandlers(connected: boolean): void {
    this.connectionHandlers.forEach(handler => {
      try {
        handler(connected);
      } catch (error) {
        console.error('[WebSocket] Error in connection handler:', error);
      }
    });
  }
}

// Export singleton instance
export const webSocketService = new WebSocketService();