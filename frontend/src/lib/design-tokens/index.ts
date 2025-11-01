/**
 * Design Tokens - Centralized Export
 * 
 * Usage examples:
 * ```typescript
 * import { colors, motion, typography } from '@/lib/design-tokens';
 * 
 * // Use in components
 * const buttonStyle = {
 *   backgroundColor: colors.primary[500],
 *   transition: `all ${motion.duration.fast}ms ${motion.easing.easeOut}`,
 *   fontSize: typography.fontSize.base,
 * };
 * ```
 */

export { colors, type ColorPalette } from './colors';
export { motion, type MotionTokens } from './motion';
export { typography, type TypographyTokens } from './typography';
export { spacing, type SpacingTokens } from './spacing';
export { shadows, type ShadowTokens } from './shadows';
