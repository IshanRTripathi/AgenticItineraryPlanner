import { useState, useEffect, useCallback } from 'react';

/**
 * Custom hook for debouncing function calls
 * @param callback - The function to debounce
 * @param delay - The delay in milliseconds
 * @returns The debounced function
 */
export function useDebounce<T extends (...args: any[]) => any>(
  callback: T,
  delay: number
): T {
  const [timeoutId, setTimeoutId] = useState<NodeJS.Timeout | null>(null);

  const debouncedCallback = useCallback(
    (...args: Parameters<T>) => {
      // Clear existing timeout
      if (timeoutId) {
        clearTimeout(timeoutId);
      }

      // Set new timeout
      const newTimeoutId = setTimeout(() => {
        callback(...args);
      }, delay);

      setTimeoutId(newTimeoutId);
    },
    [callback, delay, timeoutId]
  ) as T;

  // Cleanup timeout on unmount
  useEffect(() => {
    return () => {
      if (timeoutId) {
        clearTimeout(timeoutId);
      }
    };
  }, [timeoutId]);

  return debouncedCallback;
}

/**
 * Custom hook for preventing multiple rapid function calls (throttling)
 * @param callback - The function to throttle
 * @param delay - The minimum delay between calls in milliseconds
 * @returns The throttled function and loading state
 */
export function useThrottle<T extends (...args: any[]) => any>(
  callback: T,
  delay: number
): [T, boolean] {
  const [isThrottled, setIsThrottled] = useState(false);
  const [lastCallTime, setLastCallTime] = useState(0);

  const throttledCallback = useCallback(
    (...args: Parameters<T>) => {
      const now = Date.now();
      
      if (now - lastCallTime >= delay) {
        setLastCallTime(now);
        setIsThrottled(true);
        
        // Execute the callback
        const result = callback(...args);
        
        // Reset throttling state after delay
        setTimeout(() => {
          setIsThrottled(false);
        }, delay);
        
        return result;
      }
    },
    [callback, delay, lastCallTime]
  ) as T;

  return [throttledCallback, isThrottled];
}