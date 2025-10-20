import { QueryClient } from '@tanstack/react-query';
import { logger } from '../../utils/logger';
import { ErrorHandler } from '../../utils/errorHandler';
import { toast } from 'sonner';

/**
 * Centralized React Query client with integrated error handling
 * 
 * Features:
 * - Automatic retry with exponential backoff for retryable errors
 * - Centralized error logging
 * - User-friendly error messages via toast notifications
 */
export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      refetchOnWindowFocus: false,
      retry: (failureCount, error) => {
        // Use centralized error handler to determine if should retry
        const errorKey = `query_${failureCount}`;
        const shouldRetry = ErrorHandler.shouldRetry(error, errorKey);
        
        if (shouldRetry) {
          ErrorHandler.incrementRetryCount(errorKey);
          logger.warn('Retrying query', {
            component: 'QueryClient',
            action: 'query_retry',
            failureCount,
            errorType: ErrorHandler.classify(error)
          });
        } else {
          ErrorHandler.resetRetryCount(errorKey);
          
          // Log final query error
          ErrorHandler.handle(error, {
            component: 'QueryClient',
            action: 'query_error_final',
            failureCount
          });
        }
        
        return shouldRetry;
      },
      retryDelay: (attemptIndex) => {
        // Exponential backoff: 1s, 2s, 4s, 8s...
        return ErrorHandler.getRetryDelay(attemptIndex);
      }
    },
    mutations: {
      retry: (failureCount, error) => {
        // Use centralized error handler to determine if should retry
        const errorKey = `mutation_${failureCount}`;
        const shouldRetry = ErrorHandler.shouldRetry(error, errorKey);
        
        if (shouldRetry) {
          ErrorHandler.incrementRetryCount(errorKey);
          logger.warn('Retrying mutation', {
            component: 'QueryClient',
            action: 'mutation_retry',
            failureCount,
            errorType: ErrorHandler.classify(error)
          });
        } else {
          ErrorHandler.resetRetryCount(errorKey);
          
          // Log final mutation error
          const appError = ErrorHandler.handle(error, {
            component: 'QueryClient',
            action: 'mutation_error_final',
            failureCount
          });
          
          // Show toast for user-facing errors (not auth errors, those are handled separately)
          if (appError.type !== 'AUTH_FAILED') {
            toast.error(ErrorHandler.getUserMessage(error));
          }
        }
        
        return shouldRetry;
      },
      retryDelay: (attemptIndex) => {
        // Exponential backoff: 1s, 2s, 4s, 8s...
        return ErrorHandler.getRetryDelay(attemptIndex);
      }
    }
  }
});

/**
 * Helper to handle query errors in components
 * Use this in onError callbacks for individual queries/mutations
 */
export function handleQueryError(error: unknown, context?: { component?: string; action?: string }) {
  const appError = ErrorHandler.handle(error as Error, {
    component: context?.component || 'Query',
    action: context?.action || 'error'
  });
  
  // Show toast for user-facing errors
  if (appError.type !== 'AUTH_FAILED') {
    toast.error(ErrorHandler.getUserMessage(error));
  }
  
  return appError;
}



