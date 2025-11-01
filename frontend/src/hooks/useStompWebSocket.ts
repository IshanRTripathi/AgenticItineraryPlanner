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
  executionId: string | null,
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
    if (!executionId) return;

    const wsUrl = import.meta.env.VITE_WS_BASE_URL || 'http://localhost:8080/ws';
    
    const client = new Client({
      webSocketFactory: () => new SockJS(wsUrl),
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

        // Subscribe to agent progress topic
        subscriptionRef.current = client.subscribe(
          `/topic/itinerary/${executionId}`,
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
  }, [executionId, onMessage, onError, onConnect, onDisconnect, reconnect, reconnectDelay]);

  const disconnect = useCallback(() => {
    if (subscriptionRef.current) {
      subscriptionRef.current.unsubscribe();
      subscriptionRef.current = null;
    }
    if (clientRef.current) {
      clientRef.current.deactivate();
      clientRef.current = null;
    }
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
    connect();
    return () => {
      disconnect();
    };
  }, [connect, disconnect]);

  return {
    data,
    error,
    isConnected,
    sendMessage,
    disconnect,
    reconnect: connect,
  };
}
