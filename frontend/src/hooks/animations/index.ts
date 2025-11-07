/**
 * Animation Hooks - Centralized Export
 * 
 * Usage examples:
 * ```typescript
 * import { useFadeIn, useHoverScale } from '@/hooks/animations';
 * 
 * function MyComponent() {
 *   const fadeInProps = useFadeIn(0.2);
 *   const hoverProps = useHoverScale(1.05);
 *   
 *   return (
 *     <motion.div {...fadeInProps} {...hoverProps}>
 *       Content
 *     </motion.div>
 *   );
 * }
 * ```
 */

export { useFadeIn } from './useFadeIn';
export { useHoverScale } from './useHoverScale';
export { useStaggerChildren } from './useStaggerChildren';
