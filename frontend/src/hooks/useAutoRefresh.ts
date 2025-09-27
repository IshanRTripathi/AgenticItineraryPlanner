import { useState, useEffect, useCallback } from 'react';

interface UseAutoRefreshOptions {
  interval?: number; // in seconds
  onRefresh: () => void;
  enabled?: boolean;
}

interface UseAutoRefreshReturn {
  countdown: number;
  isRefreshing: boolean;
  startRefresh: () => void;
  stopRefresh: () => void;
}

export const useAutoRefresh = ({ 
  interval = 3, 
  onRefresh, 
  enabled = true 
}: UseAutoRefreshOptions): UseAutoRefreshReturn => {
  const [countdown, setCountdown] = useState(interval);
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [isActive, setIsActive] = useState(false);

  const startRefresh = useCallback(() => {
    setIsActive(true);
    setCountdown(interval);
    setIsRefreshing(false);
  }, [interval]);

  const stopRefresh = useCallback(() => {
    setIsActive(false);
    setCountdown(interval);
    setIsRefreshing(false);
  }, [interval]);

  useEffect(() => {
    if (!enabled || !isActive) {
      return;
    }

    const timer = setInterval(() => {
      setCountdown((prev) => {
        if (prev <= 1) {
          setIsRefreshing(true);
          onRefresh();
          return interval;
        }
        return prev - 1;
      });
    }, 1000);

    return () => clearInterval(timer);
  }, [enabled, isActive, interval, onRefresh]);

  // Auto-start when enabled
  useEffect(() => {
    if (enabled && !isActive) {
      startRefresh();
    }
  }, [enabled, isActive, startRefresh]);

  return {
    countdown,
    isRefreshing,
    startRefresh,
    stopRefresh
  };
};
