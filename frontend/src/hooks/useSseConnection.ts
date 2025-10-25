/**
 * React hook for SSE connection management
 * Provides easy integration with components
 */

import { useEffect, useCallback, useState } from 'react';
import { sseManager, ChangeEvent, SseManagerOptions } from '../services/sseManager';
import { logger } from '../utils/logger';

interface UseSseConnectionOptions extends Omit<SseManagerOptions, 'onConnect' | 'onDisconnect'> {
  enabled?: boolean;
  executionId?: string;
}

interface UseSseConnectionReturn {
  isConnected: boolean;
  connect: (itineraryId: string) => void;
  disconnect: () => void;
  reconnect: () => void;
}

export function useSseConnection(
  itineraryId: string | null,
  options: UseSseConnectionOptions = {}
): UseSseConnectionReturn {
  const [isConnected, setIsConnected] = useState(false);
  const { enabled = true, executionId, ...sseOptions } = options;

  // Connect to SSE
  const connect = useCallback((id: string) => {
    if (!enabled) return;

    const manager = new (sseManager.constructor as any)({
      ...sseOptions,
      executionId,
      onConnect: () => {
        setIsConnected(true);
        logger.info('SSE Connected', { component: 'useSseConnection' });
      },
      onDisconnect: () => {
        setIsConnected(false);
        logger.info('SSE Disconnected', { component: 'useSseConnection' });
      },
    });

    manager.connect(id);
    
    // Store manager instance for cleanup
    (window as any).__sseManager = manager;
  }, [enabled, executionId, sseOptions]);

  // Disconnect from SSE
  const disconnect = useCallback(() => {
    const manager = (window as any).__sseManager;
    if (manager) {
      manager.disconnect();
      (window as any).__sseManager = null;
    }
    setIsConnected(false);
  }, []);

  // Reconnect
  const reconnect = useCallback(() => {
    if (itineraryId) {
      disconnect();
      setTimeout(() => connect(itineraryId), 100);
    }
  }, [itineraryId, connect, disconnect]);

  // Auto-connect when itineraryId changes
  useEffect(() => {
    if (itineraryId && enabled) {
      connect(itineraryId);
    }

    return () => {
      disconnect();
    };
  }, [itineraryId, enabled, executionId, connect, disconnect]);

  return {
    isConnected,
    connect,
    disconnect,
    reconnect,
  };
}

/**
 * Hook for listening to specific change events
 */
export function useChangeEvents(
  itineraryId: string | null,
  onChangeEvent: (event: ChangeEvent) => void,
  enabled = true
): UseSseConnectionReturn {
  return useSseConnection(itineraryId, {
    enabled,
    onChangeEvent,
  });
}

/**
 * Hook for optimistic UI updates
 */
export function useOptimisticUpdates(
  itineraryId: string | null,
  onUpdate: (data: any) => void,
  enabled = true
): UseSseConnectionReturn {
  const handleChangeEvent = useCallback((event: ChangeEvent) => {
    if (event.type === 'patch_applied' || event.type === 'version_updated') {
      logger.debug('Applying update', { 
        component: 'useOptimisticUpdates',
        eventType: event.type 
      });
      onUpdate(event.data);
    }
  }, [onUpdate]);

  return useSseConnection(itineraryId, {
    enabled,
    onChangeEvent: handleChangeEvent,
  });
}
