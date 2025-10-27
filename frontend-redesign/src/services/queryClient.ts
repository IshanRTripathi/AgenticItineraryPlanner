/**
 * React Query Client Configuration
 * Single source of truth for all server data
 */

import { QueryClient } from '@tanstack/react-query';

export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      // Consider data fresh for 5 minutes
      staleTime: 5 * 60 * 1000,
      // Keep in cache for 10 minutes
      gcTime: 10 * 60 * 1000,
      // Don't refetch on window focus (prevents unnecessary API calls)
      refetchOnWindowFocus: false,
      // Don't refetch on reconnect
      refetchOnReconnect: false,
      // Retry logic
      retry: (failureCount, error: any) => {
        // Don't retry on 4xx errors (except 408, 429)
        if (error?.response?.status) {
          const status = error.response.status;
          if (status === 401 || status === 403 || status === 404) {
            return false;
          }
        }
        // Retry up to 3 times for other errors
        return failureCount < 3;
      },
      // Exponential backoff with max 30s delay
      retryDelay: (attemptIndex) => Math.min(1000 * 2 ** attemptIndex, 30000),
    },
    mutations: {
      // Less aggressive retry for mutations
      retry: (failureCount, error: any) => {
        if (error?.response?.status) {
          const status = error.response.status;
          if (status === 401 || status === 403 || status === 404) {
            return false;
          }
        }
        return failureCount < 2;
      },
      retryDelay: (attemptIndex) => Math.min(1000 * 2 ** attemptIndex, 30000),
    },
  },
});
