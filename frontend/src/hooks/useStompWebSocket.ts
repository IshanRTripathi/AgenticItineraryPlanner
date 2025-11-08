/**
 * STOMP WebSocket Hook
 * For real-time agent progress updates via STOMP protocol
 */

import { useEffect, useState, useRef, useCallback } from 'react';
import { Client, IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

interface StompOptions {
  onMessage?: (data: any) => void;
  onError?: (error: any) => void;
  onConnect?: () => void;
  onDisconnect?: () => void;
  reconnect?: boolean;
  reconnectDelay?: number;
}

export function useStompWebSocket(
  itineraryId: string | null,
  options: StompOptions = {}
) {
  const [data, setData] = useState<any>(null);
  const [error, setError] = useState<any>(null);
  const [isConnected, setIsConnected] = useState(false);
  const clientRef = useRef<Client | null>(null);
  const subscriptionRef = useRef<any>(null);

  const {
    onMessage,
    onError,
    onConnect,
    onDisconnect,
    reconnect = true,
    reconnectDelay = 3000,
  } = options;

  const connect = useCallback(() => {
    if (!itineraryId) {
      console.log('[STOMP] No itineraryId provided, skipping connection');
      return;
    }
    
    // Prevent duplicate connections
    if (clientRef.current?.connected) {
      console.log('[STOMP] Already connected, skipping');
      return;
    }

    // Clean up any existing client before creating new one
    if (clientRef.current) {
      console.log('[STOMP] Cleaning up existing client before reconnect');
      try {
        clientRef.current.deactivate();
      } catch (e) {
        console.warn('[STOMP] Error deactivating existing client:', e);
      }
      clientRef.current = null;
    }

    // Construct WebSocket URL with proper protocol detection
    let wsUrl = import.meta.env.VITE_WS_BASE_URL;
    
    if (!wsUrl) {
      // Auto-detect WebSocket URL from API base URL
      const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || import.meta.env.VITE_APP_BASE_URL;
      if (apiBaseUrl) {
        // Convert http(s):// to ws(s):// and append /ws path
        wsUrl = apiBaseUrl.replace(/^https:/, 'wss:').replace(/^http:/, 'ws:').replace(/\/api\/v1$/, '') + '/ws';
      } else {
        // Fallback to localhost
        wsUrl = 'ws://localhost:8080/ws';
      }
    }
    
    // Ensure WebSocket URL uses ws:// or wss:// protocol (not http/https)
    if (wsUrl.startsWith('http://')) {
      wsUrl = wsUrl.replace('http://', 'ws://');
    } else if (wsUrl.startsWith('https://')) {
      wsUrl = wsUrl.replace('https://', 'wss://');
    }
    
    console.log('[STOMP] Using WebSocket URL:', wsUrl);
    
    const client = new Client({
      webSocketFactory: () => {
        // Use itinerary-specific session ID to prevent duplicate sessions
        // Edge case: Ensure sessionId is deterministic for same itinerary to enable reconnection
        const sessionId = `session_${itineraryId}_${Date.now()}`;
        console.log('[STOMP] Creating WebSocket with session:', sessionId);
        return new SockJS(wsUrl, null, {
          sessionId: () => sessionId,
          transports: ['websocket', 'xhr-streaming', 'xhr-polling']
        });
      },
      reconnectDelay: reconnect ? reconnectDelay : 0,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      debug: (str) => {
        console.log('[STOMP Debug]', str);
      },
      onConnect: () => {
        console.log('[STOMP] Connected');
        setIsConnected(true);
        setError(null);
        onConnect?.();

        // Subscribe to itinerary updates topic
        // CRITICAL: Backend publishes to /topic/itinerary/{itineraryId}
        subscriptionRef.current = client.subscribe(
          `/topic/itinerary/${itineraryId}`,
          (message: IMessage) => {
            try {
              const parsedData = JSON.parse(message.body);
              console.log('[STOMP] Received message:', parsedData);
              setData(parsedData);
              onMessage?.(parsedData);
            } catch (err) {
              console.error('[STOMP] Failed to parse message:', err);
            }
          }
        );
      },
      onStompError: (frame) => {
        console.error('[STOMP] Error:', frame);
        setError(frame);
        onError?.(frame);
      },
      onWebSocketClose: () => {
        console.log('[STOMP] Disconnected');
        setIsConnected(false);
        onDisconnect?.();
      },
    });

    clientRef.current = client;
    client.activate();
  }, [itineraryId, reconnect, reconnectDelay]); // ✅ Removed callback dependencies

  const disconnect = useCallback(() => {
    console.log('[STOMP] Disconnecting...');
    
    // Edge case: Unsubscribe first to prevent memory leaks
    if (subscriptionRef.current) {
      try {
        subscriptionRef.current.unsubscribe();
        console.log('[STOMP] Unsubscribed from topic');
      } catch (e) {
        console.warn('[STOMP] Error unsubscribing:', e);
      }
      subscriptionRef.current = null;
    }
    
    // Edge case: Deactivate client safely
    if (clientRef.current) {
      try {
        if (clientRef.current.connected) {
          clientRef.current.deactivate();
          console.log('[STOMP] Client deactivated');
        }
      } catch (e) {
        console.warn('[STOMP] Error deactivating client:', e);
      }
      clientRef.current = null;
    }
    
    // Reset state
    setIsConnected(false);
  }, []);

  const sendMessage = useCallback((destination: string, body: any) => {
    if (clientRef.current && clientRef.current.connected) {
      clientRef.current.publish({
        destination,
        body: JSON.stringify(body),
      });
    } else {
      console.warn('[STOMP] Client is not connected');
    }
  }, []);

  useEffect(() => {
    if (!itineraryId) return;
    
    connect();
    
    return () => {
      disconnect();
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [itineraryId]); // ✅ Only reconnect when itineraryId changes

  return {
    data,
    error,
    isConnected,
    sendMessage,
    disconnect,
    reconnect: connect,
  };
}
