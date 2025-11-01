/**
 * Animation Variants Library
 * Reusable Framer Motion variants for consistent animations
 */

import { Variants } from 'framer-motion';
import { motion } from '../design-tokens';
import { shadows } from '../design-tokens/shadows';

export const fadeIn: Variants = {
  initial: { opacity: 0 },
  animate: { opacity: 1 },
  exit: { opacity: 0 },
};

export const fadeInUp: Variants = {
  initial: { opacity: 0, y: 20 },
  animate: { 
    opacity: 1, 
    y: 0,
    transition: { duration: motion.duration.normal / 1000, ease: [0.0, 0.0, 0.2, 1] }
  },
  exit: { 
    opacity: 0, 
    y: -20,
    transition: { duration: motion.duration.fast / 1000, ease: [0.4, 0.0, 1, 1] }
  },
};

export const scaleIn: Variants = {
  initial: { opacity: 0, scale: 0.95 },
  animate: { 
    opacity: 1, 
    scale: 1,
    transition: { duration: motion.duration.normal / 1000, ease: [0.0, 0.0, 0.2, 1] }
  },
  exit: { 
    opacity: 0, 
    scale: 0.95,
    transition: { duration: motion.duration.fast / 1000, ease: [0.4, 0.0, 1, 1] }
  },
};

export const slideInRight: Variants = {
  initial: { x: 100, opacity: 0 },
  animate: { 
    x: 0, 
    opacity: 1,
    transition: { duration: motion.duration.slow / 1000, ease: [0.0, 0.0, 0.2, 1] }
  },
  exit: { 
    x: -100, 
    opacity: 0,
    transition: { duration: motion.duration.slow / 1000, ease: [0.4, 0.0, 1, 1] }
  },
};

export const slideInLeft: Variants = {
  initial: { x: -100, opacity: 0 },
  animate: { 
    x: 0, 
    opacity: 1,
    transition: { duration: motion.duration.slow / 1000, ease: [0.0, 0.0, 0.2, 1] }
  },
  exit: { 
    x: 100, 
    opacity: 0,
    transition: { duration: motion.duration.slow / 1000, ease: [0.4, 0.0, 1, 1] }
  },
};

export const staggerContainer: Variants = {
  animate: {
    transition: {
      staggerChildren: 0.1,
    },
  },
};

export const hoverScale: Variants = {
  rest: { scale: 1 },
  hover: { 
    scale: 1.02,
    transition: { duration: motion.duration.fast / 1000, ease: [0.0, 0.0, 0.2, 1] }
  },
  tap: { 
    scale: 0.98,
    transition: { duration: motion.duration.instant / 1000 }
  },
};

export const hoverLift: Variants = {
  rest: { y: 0, boxShadow: shadows.md },
  hover: { 
    y: -4,
    boxShadow: shadows.xl,
    transition: { duration: motion.duration.normal / 1000, ease: [0.0, 0.0, 0.2, 1] }
  },
};

// Shimmer loading animation
export const shimmer: Variants = {
  initial: { backgroundPosition: '-200% 0' },
  animate: {
    backgroundPosition: '200% 0',
    transition: {
      duration: 1.5,
      ease: 'linear',
      repeat: Infinity,
    },
  },
};

// Page transition variants
export const pageTransition: Variants = {
  initial: { opacity: 0, x: 20 },
  animate: { 
    opacity: 1, 
    x: 0,
    transition: { duration: motion.duration.slow / 1000, ease: [0.0, 0.0, 0.2, 1] }
  },
  exit: { 
    opacity: 0, 
    x: -20,
    transition: { duration: motion.duration.slow / 1000, ease: [0.4, 0.0, 1, 1] }
  },
};

// Modal variants
export const modalBackdrop: Variants = {
  initial: { opacity: 0 },
  animate: { opacity: 1 },
  exit: { opacity: 0 },
};

export const modalContent: Variants = {
  initial: { opacity: 0, scale: 0.95, y: 20 },
  animate: { 
    opacity: 1, 
    scale: 1, 
    y: 0,
    transition: { duration: motion.duration.normal / 1000, ease: [0.0, 0.0, 0.2, 1] }
  },
  exit: { 
    opacity: 0, 
    scale: 0.95, 
    y: 20,
    transition: { duration: motion.duration.fast / 1000, ease: [0.4, 0.0, 1, 1] }
  },
};

// Dropdown variants
export const dropdown: Variants = {
  initial: { opacity: 0, y: -10, scale: 0.95 },
  animate: { 
    opacity: 1, 
    y: 0, 
    scale: 1,
    transition: { duration: motion.duration.fast / 1000, ease: [0.0, 0.0, 0.2, 1] }
  },
  exit: { 
    opacity: 0, 
    y: -10, 
    scale: 0.95,
    transition: { duration: motion.duration.fast / 1000, ease: [0.4, 0.0, 1, 1] }
  },
};
