import { useState, useEffect, useRef, useCallback, RefObject } from 'react';

interface UseVirtualScrollOptions {
  itemHeight: number;
  containerHeight: number;
  overscan?: number;
}

interface VirtualItem {
  index: number;
  start: number;
  size: number;
}

interface UseVirtualScrollReturn {
  virtualItems: VirtualItem[];
  totalHeight: number;
  scrollToIndex: (index: number) => void;
  containerRef: RefObject<HTMLDivElement>;
}

export const useVirtualScroll = (
  itemCount: number,
  options: UseVirtualScrollOptions
): UseVirtualScrollReturn => {
  const { itemHeight, containerHeight, overscan = 3 } = options;
  const containerRef = useRef<HTMLDivElement>(null);
  const [scrollTop, setScrollTop] = useState(0);

  const totalHeight = itemCount * itemHeight;

  const startIndex = Math.max(0, Math.floor(scrollTop / itemHeight) - overscan);
  const endIndex = Math.min(
    itemCount - 1,
    Math.ceil((scrollTop + containerHeight) / itemHeight) + overscan
  );

  const virtualItems: VirtualItem[] = [];
  for (let i = startIndex; i <= endIndex; i++) {
    virtualItems.push({
      index: i,
      start: i * itemHeight,
      size: itemHeight
    });
  }

  const handleScroll = useCallback(() => {
    if (containerRef.current) {
      setScrollTop(containerRef.current.scrollTop);
    }
  }, []);

  useEffect(() => {
    const container = containerRef.current;
    if (!container) return;

    container.addEventListener('scroll', handleScroll);
    return () => container.removeEventListener('scroll', handleScroll);
  }, [handleScroll]);

  const scrollToIndex = useCallback((index: number) => {
    if (containerRef.current) {
      containerRef.current.scrollTop = index * itemHeight;
    }
  }, [itemHeight]);

  return {
    virtualItems,
    totalHeight,
    scrollToIndex,
    containerRef
  };
};
