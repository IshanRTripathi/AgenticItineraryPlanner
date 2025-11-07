/**
 * useFadeIn Hook
 * Composable hook for fade-in animations on scroll
 */

import { useScrollAnimation } from '../useScrollAnimation';
import { fadeInUp } from '@/lib/animations/variants';

export function useFadeIn(delay = 0) {
  const { ref, isVisible } = useScrollAnimation();
  
  return {
    ref,
    initial: 'initial',
    animate: isVisible ? 'animate' : 'initial',
    variants: fadeInUp,
    transition: { delay },
  };
}
