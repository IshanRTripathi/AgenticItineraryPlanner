import { useEffect, useRef, useCallback } from 'react';

interface MobileScrollOptions {
  onScroll?: (scrollY: number) => void;
  onScrollToTop?: () => void;
  onScrollToBottom?: () => void;
  threshold?: number;
  debounceMs?: number;
}

export function useMobileScroll(options: MobileScrollOptions = {}) {
  const {
    onScroll,
    onScrollToTop,
    onScrollToBottom,
    threshold = 100,
    debounceMs = 16, // ~60fps
  } = options;

  const lastScrollY = useRef(0);
  const ticking = useRef(false);
  const debounceTimeout = useRef<NodeJS.Timeout>();

  const handleScroll = useCallback(() => {
    if (!ticking.current) {
      requestAnimationFrame(() => {
        const scrollY = window.scrollY;
        const scrollHeight = document.documentElement.scrollHeight;
        const clientHeight = window.innerHeight;

        // Call scroll callback
        onScroll?.(scrollY);

        // Check if scrolled to top
        if (scrollY <= threshold) {
          onScrollToTop?.();
        }

        // Check if scrolled to bottom
        if (scrollY + clientHeight >= scrollHeight - threshold) {
          onScrollToBottom?.();
        }

        lastScrollY.current = scrollY;
        ticking.current = false;
      });
      ticking.current = true;
    }
  }, [onScroll, onScrollToTop, onScrollToBottom, threshold]);

  const debouncedHandleScroll = useCallback(() => {
    if (debounceTimeout.current) {
      clearTimeout(debounceTimeout.current);
    }
    
    debounceTimeout.current = setTimeout(handleScroll, debounceMs);
  }, [handleScroll, debounceMs]);

  useEffect(() => {
    // Add passive event listener for better performance
    window.addEventListener('scroll', debouncedHandleScroll, { passive: true });
    
    return () => {
      window.removeEventListener('scroll', debouncedHandleScroll);
      if (debounceTimeout.current) {
        clearTimeout(debounceTimeout.current);
      }
    };
  }, [debouncedHandleScroll]);

  const scrollToTop = useCallback((smooth = true) => {
    window.scrollTo({
      top: 0,
      behavior: smooth ? 'smooth' : 'instant',
    });
  }, []);

  const scrollToBottom = useCallback((smooth = true) => {
    window.scrollTo({
      top: document.documentElement.scrollHeight,
      behavior: smooth ? 'smooth' : 'instant',
    });
  }, []);

  return {
    scrollToTop,
    scrollToBottom,
    scrollY: lastScrollY.current,
  };
}
