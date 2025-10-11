/**
 * useChangePreview Hook
 * Provides easy access to change preview functionality
 */

import { useState, useCallback } from 'react';
import { apiClient } from '../services/apiClient';
import { PreviewCache } from '../components/preview/PreviewCache';
import { ItineraryDiff } from '../types/ChatTypes';

interface UseChangePreviewOptions {
  itineraryId: string;
  onSuccess?: () => void;
  onError?: (error: Error) => void;
}

export function useChangePreview({ itineraryId, onSuccess, onError }: UseChangePreviewOptions) {
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<Error | null>(null);

  /**
   * Propose changes and get preview
   */
  const proposeChanges = useCallback(
    async (changeSet: any): Promise<{ changeSet: any; diff: ItineraryDiff } | null> => {
      // Check cache first
      const cached = PreviewCache.get(changeSet);
      if (cached) {
        console.log('Using cached preview');
        return cached;
      }

      setIsLoading(true);
      setError(null);

      try {
        const response = await apiClient.proposeChanges(itineraryId, changeSet);
        
        // Cache the result
        PreviewCache.set(changeSet, response.diff);
        
        return {
          changeSet,
          diff: response.diff,
        };
      } catch (err) {
        const error = err instanceof Error ? err : new Error('Failed to propose changes');
        setError(error);
        onError?.(error);
        return null;
      } finally {
        setIsLoading(false);
      }
    },
    [itineraryId, onError]
  );

  /**
   * Apply changes directly without preview
   */
  const applyChanges = useCallback(
    async (changeSet: any): Promise<boolean> => {
      setIsLoading(true);
      setError(null);

      try {
        await apiClient.applyChanges(itineraryId, changeSet);
        onSuccess?.();
        return true;
      } catch (err) {
        const error = err instanceof Error ? err : new Error('Failed to apply changes');
        setError(error);
        onError?.(error);
        return false;
      } finally {
        setIsLoading(false);
      }
    },
    [itineraryId, onSuccess, onError]
  );

  /**
   * Clear preview cache
   */
  const clearCache = useCallback(() => {
    PreviewCache.clear();
  }, []);

  return {
    proposeChanges,
    applyChanges,
    clearCache,
    isLoading,
    error,
  };
}
