import { useState, useEffect, useCallback, useRef } from 'react';
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
  const pollCountRef = useRef(0); // Use ref instead of state to avoid dependency issues
  const metricsRef = useRef({
    startTime: Date.now(),
    statusChanges: [] as Array<{from: string, to: string, timestamp: number}>
  });

  const checkStatus = useCallback(async () => {
    if (!itineraryId || !enabled) return;

    try {
      setIsPolling(true);
      pollCountRef.current += 1; // Increment ref
      
      logger.debug('Polling generation status', {
        component: 'useGenerationStatus',
        action: 'poll',
        itineraryId,
        currentStatus: status,
        pollCount: pollCountRef.current
      });

      const itinerary = await apiClient.getItinerary(itineraryId);
      // Extract status from response without fallback
      const newStatus = (itinerary as any).status;

      // Debug logging for received response
      const hasDays = (itinerary as any).days?.length > 0;
      const hasContent = hasDays && (itinerary as any).days.some(
        (day: any) => day.nodes && day.nodes.length > 0
      );
      
      logger.debug('Received itinerary response', {
        component: 'useGenerationStatus',
        action: 'response',
        itineraryId,
        hasStatus: !!newStatus,
        status: newStatus,
        hasDays,
        daysCount: (itinerary as any).days?.length || 0,
        hasContent
      });

      // If no status field, don't assume completion - log warning and continue polling
      if (!newStatus) {
        logger.warn('No status field in itinerary response, continuing to poll', {
          component: 'useGenerationStatus',
          action: 'missing_status',
          itineraryId,
          response: itinerary
        });
        return; // Don't update status, keep polling
      }

      if (newStatus !== status) {
        // Track status change in metrics
        metricsRef.current.statusChanges.push({
          from: status,
          to: newStatus,
          timestamp: Date.now()
        });
        
        // Log metrics
        logger.info('Generation metrics', {
          component: 'useGenerationStatus',
          action: 'metrics',
          totalDuration: Date.now() - metricsRef.current.startTime,
          pollCount: pollCountRef.current,
          statusChanges: metricsRef.current.statusChanges
        });
        logger.info('Generation status changed', {
          component: 'useGenerationStatus',
          action: 'status_changed',
          itineraryId,
          oldStatus: status,
          newStatus,
          pollCount: pollCountRef.current
        });

        setStatus(newStatus);

        // Only mark as complete if status is 'completed' AND itinerary has actual content
        if (newStatus === 'completed') {
          if (hasContent) {
            logger.info('Generation completed with content', {
              component: 'useGenerationStatus',
              action: 'completed',
              itineraryId,
              finalStatus: newStatus,
              totalPolls: pollCountRef.current,
              daysCount: (itinerary as any).days.length
            });
            
            onComplete?.();
          } else {
            logger.warn('Status is completed but no content found, continuing to poll', {
              component: 'useGenerationStatus',
              action: 'completed_no_content',
              itineraryId,
              hasDays,
              daysCount: (itinerary as any).days?.length || 0
            });
            // Don't call onComplete, keep polling
          }
        } else if (newStatus === 'failed') {
          logger.error('Generation failed', {
            component: 'useGenerationStatus',
            action: 'failed',
            itineraryId,
            finalStatus: newStatus
          });
          
          onError?.(new Error('Generation failed'));
        }
      }

    } catch (error) {
      logger.error('Status polling failed', {
        component: 'useGenerationStatus',
        action: 'poll_error',
        itineraryId,
        pollCount: pollCountRef.current
      }, error);
      
      onError?.(error as Error);
    } finally {
      setIsPolling(false);
    }
  }, [itineraryId, status, enabled, onComplete, onError]); // Removed pollCount from dependencies

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
        itineraryId
      });
      clearInterval(interval);
    };
  }, [itineraryId, status, pollingInterval, enabled, checkStatus]); // Removed pollCount

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
    checkStatus
  };
}
