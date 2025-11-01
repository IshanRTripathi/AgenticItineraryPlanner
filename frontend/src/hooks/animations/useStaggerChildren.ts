/**
 * useStaggerChildren Hook
 * Creates stagger animation variants for child elements
 */

export function useStaggerChildren(staggerDelay = 0.1) {
  return {
    variants: {
      animate: {
        transition: {
          staggerChildren: staggerDelay,
        },
      },
    },
  };
}
