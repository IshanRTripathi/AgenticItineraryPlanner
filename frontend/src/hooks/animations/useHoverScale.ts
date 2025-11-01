/**
 * useHoverScale Hook
 * Provides hover scale animation state and handlers
 */

import { useState } from 'react';

export function useHoverScale(scale = 1.02) {
  const [isHovered, setIsHovered] = useState(false);

  return {
    onMouseEnter: () => setIsHovered(true),
    onMouseLeave: () => setIsHovered(false),
    animate: { scale: isHovered ? scale : 1 },
    transition: { duration: 0.2, ease: 'easeOut' },
  };
}
