import { useState, useEffect, useCallback } from 'react';
import { apiClient } from '../services/apiClient';
import { logger } from '../utils/logger';

export interface GenerationStatusOptions {
  pollingInterval?: number; // milliseconds
  onComplete?: () => void;
  onError?: (error: Error) => void;
  enabled?: boolean;
}

/**
 * Hook to poll generation status and detect completion after page refresh.
 * 
 * This solves the problem where:
 * 1. User starts generation
 * 2. User refreshes page (SSE connection lost)
 * 3. Generation continues in backend
 * 4. Frontend doesn't know when it's done
 * 
 * This hook polls the status every N seconds when status is "generating"
 * and automatically detects completion.
 */
export function useGenerationStatus(
  itineraryId: string | null,
  initialStatus: string,
  options: GenerationStatusOptions = {}
) {
  const {
    pollingInterval = 5000, // 5 seconds default
    onComplete,
    onError,
    enabled = true
  } = options;

  const [status, setStatus] = useState(initialStatus);
  const [isPolling, setIsPolling] = useState(false);
  const [pollCount, setPollCount] = useState(0);

  const checkStatus = useCallback(async () => {
    if (!itineraryId || !enabled) return;

    try {
      setIsPolling(true);
      
      logger.debug('Polling generation status', {
        component: 'useGenerationStatus',
        action: 'poll',
        itineraryId,
        currentStatus: status,
        pollCount
      });

      const itinerary = await apiClient.getItinerary(itineraryId);
      // Status might be in different places depending on response format
      const newStatus = (itinerary as any).status || 'completed';

      if (newStatus !== status) {
        logger.info('Generation status changed', {
          component: 'useGenerationStatus',
          action: 'status_changed',
          itineraryId,
          oldStatus: status,
          newStatus,
          pollCount
        });

        setStatus(newStatus);

        // If generation completed, trigger callback
        if (newStatus !== 'generating' && newStatus !== 'planning') {
          logger.info('Generation completed', {
            component: 'useGenerationStatus',
            action: 'completed',
            itineraryId,
            finalStatus: newStatus,
            totalPolls: pollCount
          });
          
          onComplete?.();
        }
      }

      setPollCount(prev => prev + 1);
    } catch (error) {
      logger.error('Status polling failed', {
        component: 'useGenerationStatus',
        action: 'poll_error',
        itineraryId,
        pollCount
      }, error);
      
      onError?.(error as Error);
    } finally {
      setIsPolling(false);
    }
  }, [itineraryId, status, pollCount, enabled, onComplete, onError]);

  // Start polling when status is "generating" or "planning"
  useEffect(() => {
    if (!itineraryId || !enabled) return;
    
    const shouldPoll = status === 'generating' || status === 'planning';
    
    if (!shouldPoll) {
      logger.debug('Polling not needed', {
        component: 'useGenerationStatus',
        action: 'skip_polling',
        status
      });
      return;
    }

    logger.info('Starting status polling', {
      component: 'useGenerationStatus',
      action: 'start_polling',
      itineraryId,
      interval: pollingInterval
    });

    // Initial check
    checkStatus();

    // Set up interval
    const interval = setInterval(checkStatus, pollingInterval);

    return () => {
      logger.debug('Stopping status polling', {
        component: 'useGenerationStatus',
        action: 'stop_polling',
        itineraryId,
        totalPolls: pollCount
      });
      clearInterval(interval);
    };
  }, [itineraryId, status, pollingInterval, enabled, checkStatus, pollCount]);

  // Update status when initialStatus changes (e.g., from SSE)
  useEffect(() => {
    if (initialStatus !== status) {
      logger.debug('Status updated from external source', {
        component: 'useGenerationStatus',
        action: 'external_update',
        oldStatus: status,
        newStatus: initialStatus
      });
      setStatus(initialStatus);
    }
  }, [initialStatus, status]);

  return {
    status,
    isPolling,
    pollCount,
    checkStatus
  };
}
