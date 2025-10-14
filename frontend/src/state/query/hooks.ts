import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { apiClient } from '../../services/apiClient';

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
    cacheTime: 10 * 60 * 1000, // Keep in cache for 10 minutes
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
      // Invalidate queries for the created itinerary
      qc.invalidateQueries({ queryKey: queryKeys.itinerary(response.itinerary.id) });
      
      // Establish SSE connection immediately after creation
      if (response.itinerary.id) {
        console.log('[useCreateItinerary] Establishing SSE connection for:', response.itinerary.id);
        const { sseManager } = require('../../services/sseManager');
        sseManager.connect(response.itinerary.id);
      }
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


