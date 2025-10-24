/**
 * React Query Hooks - Single Source of Truth for Server Data
 * 
 * IMPORTANT: React Query is the single source of truth for all server data.
 * - Do NOT persist server data (trips, itineraries) to LocalStorage
 * - Use React Query cache for data persistence
 * - Zustand store should only contain UI state
 * 
 * State Management Strategy:
 * - Server Data: React Query (this file)
 * - UI State: Zustand (useAppStore)
 * - Real-time Updates: SSE/WebSocket → React Query cache invalidation
 */

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { apiClient, CreateItineraryRequest } from '../../services/apiClient';
// SSE manager removed - using WebSocket only
// import { sseManager } from '../../services/sseManager';
import { logger } from '../../utils/logger';

export const queryKeys = {
  user: ['user'] as const,
  itinerary: (id: string) => ['itinerary', id] as const,
  itineraries: ['itineraries'] as const,
  bookings: ['bookings'] as const,
};

export function useUser() {
  return useQuery({
    queryKey: queryKeys.user,
    queryFn: () => apiClient.getCurrentUser(),
    enabled: false // enable when auth is wired
  });
}

export function useItinerary(id: string, retryOptions?: { maxRetries?: number; retryDelay?: number }, initialDelay?: number) {
  return useQuery({
    queryKey: queryKeys.itinerary(id),
    queryFn: () => apiClient.getItinerary(id, retryOptions),
    enabled: !!id,
    retry: (failureCount, error) => {
      // Don't retry on 4xx errors
      if (error.message.includes('401') || error.message.includes('403') || error.message.includes('404')) {
        return false;
      }
      // Retry up to 3 times for other errors
      return failureCount < 3;
    },
    retryDelay: (attemptIndex) => Math.min(1000 * 2 ** attemptIndex, 30000), // Exponential backoff, max 30s
    // Cache configuration to reduce unnecessary requests
    staleTime: 5 * 60 * 1000, // Consider data fresh for 5 minutes
    gcTime: 10 * 60 * 1000, // Keep in cache for 10 minutes (renamed from cacheTime in v5)
    refetchOnWindowFocus: false, // Don't refetch when window regains focus
    refetchOnReconnect: false, // Don't refetch when network reconnects
    // Add initial delay to prevent immediate API calls after creation
    ...(initialDelay && { 
      refetchOnMount: false,
    }),
  });
}

export function useItineraries(enabled: boolean = true, retryOptions?: { maxRetries?: number; retryDelay?: number }) {
  return useQuery({
    queryKey: queryKeys.itineraries,
    queryFn: () => apiClient.getAllItineraries(retryOptions),
    enabled: enabled,
    // Cache configuration to prevent multiple calls
    staleTime: 5 * 60 * 1000, // Consider data fresh for 5 minutes
    gcTime: 10 * 60 * 1000, // Keep in cache for 10 minutes (formerly cacheTime)
    retry: (failureCount, error) => {
      // Don't retry on 4xx errors except 408, 429
      if (error.message.includes('401') || error.message.includes('403') || error.message.includes('404')) {
        return false;
      }
      // Retry up to 3 times for other errors
      return failureCount < 3;
    },
    retryDelay: (attemptIndex) => Math.min(1000 * 2 ** attemptIndex, 30000), // Exponential backoff, max 30s
  });
}

export function useCreateItinerary(retryOptions?: { maxRetries?: number; retryDelay?: number }) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: CreateItineraryRequest) => apiClient.createItinerary(payload, retryOptions),
    onSuccess: (response) => {
      logger.info('Itinerary created successfully', {
        component: 'useCreateItinerary',
        action: 'create_success',
        itineraryId: response.itinerary.id
      });
      
      // Update React Query cache with new itinerary
      qc.setQueryData(queryKeys.itinerary(response.itinerary.id), response.itinerary);
      
      // Invalidate itineraries list to include new item
      qc.invalidateQueries({ queryKey: queryKeys.itineraries });
      
      // Establish SSE connection immediately after creation with executionId
      if (response.itinerary.id) {
        logger.info('Real-time updates via WebSocket', {
          component: 'useCreateItinerary',
          action: 'websocket_active',
          itineraryId: response.itinerary.id,
          executionId: response.executionId
        });
        // SSE removed - WebSocket handles real-time updates via UnifiedItineraryContext
      }
    },
    onError: (error) => {
      logger.error('Failed to create itinerary', {
        component: 'useCreateItinerary',
        action: 'create_error'
      }, error);
    },
    retry: (failureCount, error) => {
      // Don't retry on 4xx errors except 408, 429
      if (error.message.includes('401') || error.message.includes('403') || error.message.includes('404')) {
        return false;
      }
      // Retry up to 2 times for mutations (less aggressive than queries)
      return failureCount < 2;
    },
    retryDelay: (attemptIndex) => Math.min(1000 * 2 ** attemptIndex, 30000), // Exponential backoff, max 30s
  });
}


