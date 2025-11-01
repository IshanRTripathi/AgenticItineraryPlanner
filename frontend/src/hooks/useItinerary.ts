/**
 * React Query Hook for Itinerary Data
 * Fetches and caches itinerary data from backend
 */

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { apiClient } from '@/services/apiClient';
import { NormalizedItinerary } from '@/types/dto';

// Query keys for cache management
export const queryKeys = {
  itinerary: (id: string) => ['itinerary', id] as const,
  itineraries: ['itineraries'] as const,
  bookings: (itineraryId: string) => ['bookings', itineraryId] as const,
};

/**
 * Hook to fetch a single itinerary by ID
 * Uses React Query for caching and automatic refetching
 */
export function useItinerary(id: string | undefined) {
  return useQuery({
    queryKey: queryKeys.itinerary(id || ''),
    queryFn: async () => {
      if (!id) throw new Error('Itinerary ID is required');
      
      const response = await apiClient.get<NormalizedItinerary>(
        `/itineraries/${id}/json`
      );
      
      return response.data;
    },
    enabled: !!id, // Only run query if ID exists
  });
}

/**
 * Hook to fetch all itineraries for the current user
 */
export function useItineraries() {
  return useQuery({
    queryKey: queryKeys.itineraries,
    queryFn: async () => {
      const response = await apiClient.get('/itineraries');
      return response.data;
    },
  });
}

/**
 * Hook to fetch bookings for an itinerary
 */
export function useBookings(itineraryId: string | undefined) {
  return useQuery({
    queryKey: queryKeys.bookings(itineraryId || ''),
    queryFn: async () => {
      if (!itineraryId) throw new Error('Itinerary ID is required');
      
      const response = await apiClient.get(`/bookings/itinerary/${itineraryId}`);
      return response.data;
    },
    enabled: !!itineraryId,
  });
}

/**
 * Hook to update itinerary (apply changes)
 */
export function useUpdateItinerary(itineraryId: string) {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: async (changeSet: any) => {
      const response = await apiClient.post(
        `/itineraries/${itineraryId}:apply`,
        { changeSet }
      );
      return response.data;
    },
    onSuccess: () => {
      // Invalidate and refetch itinerary data
      queryClient.invalidateQueries({ queryKey: queryKeys.itinerary(itineraryId) });
    },
  });
}
