/**
 * usePullToRefresh Hook
 * Implements pull-to-refresh gesture for mobile
 */

import { useEffect, useRef, useState } from 'react';

interface UsePullToRefreshOptions {
  onRefresh: () => Promise<void>;
  threshold?: number;
  disabled?: boolean;
}

export function usePullToRefresh({
  onRefresh,
  threshold = 80,
  disabled = false,
}: UsePullToRefreshOptions) {
  const [isPulling, setIsPulling] = useState(false);
  const [pullDistance, setPullDistance] = useState(0);
  const [isRefreshing, setIsRefreshing] = useState(false);
  const startY = useRef(0);
  const currentY = useRef(0);

  useEffect(() => {
    if (disabled) return;

    let touchStartY = 0;
    let isTouchActive = false;

    const handleTouchStart = (e: TouchEvent) => {
      // Only activate if scrolled to top
      if (window.scrollY === 0) {
        touchStartY = e.touches[0].clientY;
        startY.current = touchStartY;
        isTouchActive = true;
      }
    };

    const handleTouchMove = (e: TouchEvent) => {
      if (!isTouchActive || window.scrollY > 0) return;

      currentY.current = e.touches[0].clientY;
      const distance = Math.max(0, currentY.current - touchStartY);

      if (distance > 0) {
        // Prevent default scroll behavior
        e.preventDefault();
        
        // Apply resistance curve (diminishing returns)
        const resistedDistance = Math.min(
          distance * 0.5,
          threshold * 1.5
        );
        
        setPullDistance(resistedDistance);
        setIsPulling(resistedDistance > threshold);
      }
    };

    const handleTouchEnd = async () => {
      if (!isTouchActive) return;
      
      isTouchActive = false;

      if (pullDistance > threshold && !isRefreshing) {
        setIsRefreshing(true);
        try {
          await onRefresh();
        } catch (error) {
          console.error('Refresh failed:', error);
        } finally {
          setIsRefreshing(false);
        }
      }

      setPullDistance(0);
      setIsPulling(false);
    };

    // Add event listeners
    document.addEventListener('touchstart', handleTouchStart, { passive: true });
    document.addEventListener('touchmove', handleTouchMove, { passive: false });
    document.addEventListener('touchend', handleTouchEnd);
    document.addEventListener('touchcancel', handleTouchEnd);

    return () => {
      document.removeEventListener('touchstart', handleTouchStart);
      document.removeEventListener('touchmove', handleTouchMove);
      document.removeEventListener('touchend', handleTouchEnd);
      document.removeEventListener('touchcancel', handleTouchEnd);
    };
  }, [onRefresh, pullDistance, threshold, disabled, isRefreshing]);

  return {
    isPulling,
    pullDistance,
    isRefreshing,
  };
}
