import { useEffect, useState, RefObject } from 'react';

interface UseScrollDetectionOptions {
  threshold?: number; // Distance from top to trigger (in pixels)
  enabled?: boolean;
}

/**
 * Hook to detect when user has scrolled near the top of a container
 * Useful for implementing "load more" functionality
 */
export const useScrollDetection = (
  containerRef: RefObject<HTMLElement>,
  options: UseScrollDetectionOptions = {}
) => {
  const { threshold = 100, enabled = true } = options;
  const [isNearTop, setIsNearTop] = useState(false);
  const [isAtBottom, setIsAtBottom] = useState(true);

  useEffect(() => {
    if (!enabled || !containerRef.current) return;

    const container = containerRef.current;

    const handleScroll = () => {
      const { scrollTop, scrollHeight, clientHeight } = container;
      
      // Check if near top
      const nearTop = scrollTop < threshold;
      setIsNearTop(nearTop);
      
      // Check if at bottom (within 50px)
      const atBottom = scrollHeight - scrollTop - clientHeight < 50;
      setIsAtBottom(atBottom);
    };

    // Initial check
    handleScroll();

    container.addEventListener('scroll', handleScroll, { passive: true });
    return () => container.removeEventListener('scroll', handleScroll);
  }, [containerRef, threshold, enabled]);

  return { isNearTop, isAtBottom };
};
