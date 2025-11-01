/**
 * useThrottle Hook
 * Throttles a value to limit update frequency
 */

import { useEffect, useRef, useState } from 'react';

export function useThrottle<T>(value: T, interval: number = 100): T {
  const [throttledValue, setThrottledValue] = useState<T>(value);
  const lastExecuted = useRef<number>(Date.now());

  useEffect(() => {
    const now = Date.now();
    const timeSinceLastExecution = now - lastExecuted.current;

    if (timeSinceLastExecution >= interval) {
      lastExecuted.current = now;
      setThrottledValue(value);
    } else {
      const timeoutId = setTimeout(() => {
        lastExecuted.current = Date.now();
        setThrottledValue(value);
      }, interval - timeSinceLastExecution);

      return () => clearTimeout(timeoutId);
    }
  }, [value, interval]);

  return throttledValue;
}

/**
 * useThrottleCallback Hook
 * Throttles a callback function
 */
export function useThrottleCallback<T extends (...args: any[]) => any>(
  callback: T,
  interval: number = 100
): T {
  const lastRan = useRef<number>(Date.now());
  const timeoutId = useRef<NodeJS.Timeout | null>(null);

  const throttledCallback = ((...args: Parameters<T>) => {
    const now = Date.now();
    const timeSinceLastRan = now - lastRan.current;

    if (timeSinceLastRan >= interval) {
      callback(...args);
      lastRan.current = now;
    } else {
      if (timeoutId.current) {
        clearTimeout(timeoutId.current);
      }

      timeoutId.current = setTimeout(() => {
        callback(...args);
        lastRan.current = Date.now();
      }, interval - timeSinceLastRan);
    }
  }) as T;

  useEffect(() => {
    return () => {
      if (timeoutId.current) {
        clearTimeout(timeoutId.current);
      }
    };
  }, []);

  return throttledCallback;
}
