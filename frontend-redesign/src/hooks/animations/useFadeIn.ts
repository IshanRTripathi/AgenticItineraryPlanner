/**
 * useFadeIn Hook
 * Composable hook for fade-in animations on scroll
 */

import { useScrollAnimation } from './useScrollAnimation';
import { fadeInUp } from '@/lib/animations/variants';

export function useFadeIn(delay = 0) {
  const { ref, controls } = useScrollAnimation();
  
  return {
    ref,
    initial: 'initial',
    animate: controls,
    variants: fadeInUp,
    transition: { delay },
  };
}
