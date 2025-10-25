import { useState, useCallback, useRef } from 'react';
import { logger } from '../utils/logger';

interface UseFormSubmissionOptions {
  debounceMs?: number;
  onSuccess?: () => void;
  onError?: (error: Error) => void;
}

interface UseFormSubmissionReturn<T> {
  isSubmitting: boolean;
  submit: (submitFn: () => Promise<T>) => Promise<T | null>;
  reset: () => void;
  error: Error | null;
}

/**
 * Custom hook for handling form submissions with debouncing and loading state
 * Prevents multiple rapid submissions and provides loading state management
 */
export function useFormSubmission<T = any>({
  debounceMs = 1000,
  onSuccess,
  onError
}: UseFormSubmissionOptions = {}): UseFormSubmissionReturn<T> {
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<Error | null>(null);
  const lastSubmissionTime = useRef<number>(0);
  const submissionTimeout = useRef<NodeJS.Timeout | null>(null);

  const submit = useCallback(async (submitFn: () => Promise<T>): Promise<T | null> => {
    const now = Date.now();
    
    // Prevent multiple submissions within the debounce period
    if (isSubmitting || (now - lastSubmissionTime.current < debounceMs)) {
      logger.debug('Form submission blocked - too soon or already submitting', { 
        component: 'useFormSubmission' 
      });
      return null;
    }

    // Clear any existing timeout
    if (submissionTimeout.current) {
      clearTimeout(submissionTimeout.current);
    }

    setIsSubmitting(true);
    setError(null);
    lastSubmissionTime.current = now;

    try {
      const result = await submitFn();
      onSuccess?.();
      return result;
    } catch (err) {
      const error = err instanceof Error ? err : new Error('Submission failed');
      setError(error);
      onError?.(error);
      throw error;
    } finally {
      // Reset submitting state after a minimum delay to prevent rapid clicking
      submissionTimeout.current = setTimeout(() => {
        setIsSubmitting(false);
      }, Math.min(debounceMs, 500)); // At least 500ms or the debounce time, whichever is smaller
    }
  }, [isSubmitting, debounceMs, onSuccess, onError]);

  const reset = useCallback(() => {
    setIsSubmitting(false);
    setError(null);
    lastSubmissionTime.current = 0;
    if (submissionTimeout.current) {
      clearTimeout(submissionTimeout.current);
      submissionTimeout.current = null;
    }
  }, []);

  return {
    isSubmitting,
    submit,
    reset,
    error
  };
}
