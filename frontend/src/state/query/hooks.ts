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

export function useItinerary(id: string) {
  return useQuery({
    queryKey: queryKeys.itinerary(id),
    queryFn: () => apiClient.getItinerary(id),
    enabled: !!id
  });
}

export function useItineraries() {
  return useQuery({
    queryKey: queryKeys.itineraries,
    queryFn: () => apiClient.getAllItineraries(),
  });
}

export function useCreateItinerary() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: CreateItineraryRequest) => apiClient.createItinerary(payload),
    onSuccess: (data) => {
      qc.invalidateQueries({ queryKey: queryKeys.itinerary(data.id) });
    }
  });
}


