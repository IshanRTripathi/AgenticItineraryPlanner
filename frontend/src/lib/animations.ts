/**
 * Framer Motion Animation Configurations
 * Material 3 Motion System
 * 
 * All animations target 60fps with GPU acceleration
 */

import { Variants, Transition } from 'framer-motion';

/* ========================================
   EASING FUNCTIONS (Material 3)
   ======================================== */

export const easings = {
  standard: [0.4, 0, 0.2, 1] as const,
  decelerate: [0, 0, 0.2, 1] as const,
  accelerate: [0.4, 0, 1, 1] as const,
  sharp: [0.4, 0, 0.6, 1] as const,
} as const;

/* ========================================
   DURATIONS
   ======================================== */

export const durations = {
  instant: 0.1,
  fast: 0.2,
  normal: 0.3,
  slow: 0.5,
} as const;

/* ========================================
   TRANSITIONS
   ======================================== */

export const transitions = {
  standard: {
    duration: durations.normal,
    ease: easings.standard,
  } as Transition,
  
  fast: {
    duration: durations.fast,
    ease: easings.standard,
  } as Transition,
  
  slow: {
    duration: durations.slow,
    ease: easings.standard,
  } as Transition,
  
  spring: {
    type: 'spring',
    stiffness: 300,
    damping: 30,
  } as Transition,
  
  springBouncy: {
    type: 'spring',
    stiffness: 400,
    damping: 25,
  } as Transition,
} as const;

/* ========================================
   PAGE TRANSITIONS
   ======================================== */

export const pageTransition: Variants = {
  initial: {
    opacity: 0,
  },
  animate: {
    opacity: 1,
    transition: {
      duration: durations.normal,
      ease: easings.standard,
    },
  },
  exit: {
    opacity: 0,
    transition: {
      duration: durations.fast,
      ease: easings.standard,
    },
  },
};

export const pageSlideTransition: Variants = {
  initial: {
    opacity: 0,
    x: -20,
  },
  animate: {
    opacity: 1,
    x: 0,
    transition: {
      duration: durations.normal,
      ease: easings.standard,
    },
  },
  exit: {
    opacity: 0,
    x: 20,
    transition: {
      duration: durations.fast,
      ease: easings.standard,
    },
  },
};

/* ========================================
   MODAL ANIMATIONS
   ======================================== */

export const modalBackdrop: Variants = {
  hidden: {
    opacity: 0,
  },
  visible: {
    opacity: 1,
    transition: {
      duration: durations.fast,
      ease: easings.standard,
    },
  },
  exit: {
    opacity: 0,
    transition: {
      duration: durations.fast,
      ease: easings.standard,
    },
  },
};

export const modalContent: Variants = {
  hidden: {
    opacity: 0,
    scale: 0.95,
  },
  visible: {
    opacity: 1,
    scale: 1,
    transition: {
      duration: durations.normal,
      ease: easings.standard,
    },
  },
  exit: {
    opacity: 0,
    scale: 0.95,
    transition: {
      duration: durations.fast,
      ease: easings.standard,
    },
  },
};

export const modalSlideUp: Variants = {
  hidden: {
    opacity: 0,
    y: 100,
  },
  visible: {
    opacity: 1,
    y: 0,
    transition: {
      duration: durations.normal,
      ease: easings.standard,
    },
  },
  exit: {
    opacity: 0,
    y: 100,
    transition: {
      duration: durations.fast,
      ease: easings.standard,
    },
  },
};

/* ========================================
   LIST ANIMATIONS (Stagger)
   ======================================== */

export const listContainer: Variants = {
  hidden: {
    opacity: 0,
  },
  visible: {
    opacity: 1,
    transition: {
      staggerChildren: 0.05, // 50ms delay between items
      delayChildren: 0.1,
    },
  },
};

export const listItem: Variants = {
  hidden: {
    opacity: 0,
    y: 20,
  },
  visible: {
    opacity: 1,
    y: 0,
    transition: {
      duration: durations.normal,
      ease: easings.standard,
    },
  },
};

export const listItemFade: Variants = {
  hidden: {
    opacity: 0,
  },
  visible: {
    opacity: 1,
    transition: {
      duration: durations.fast,
      ease: easings.standard,
    },
  },
};

/* ========================================
   CARD ANIMATIONS
   ======================================== */

export const cardHover = {
  scale: 1.02,
  y: -4,
  transition: {
    duration: durations.normal,
    ease: easings.standard,
  },
};

export const cardTap = {
  scale: 0.98,
  transition: {
    duration: durations.instant,
    ease: easings.standard,
  },
};

/* ========================================
   BUTTON ANIMATIONS
   ======================================== */

export const buttonHover = {
  scale: 1.02,
  y: -2,
  transition: {
    duration: durations.normal,
    ease: easings.standard,
  },
};

export const buttonTap = {
  scale: 0.98,
  transition: {
    duration: durations.instant,
    ease: easings.standard,
  },
};

/* ========================================
   ICON ANIMATIONS
   ======================================== */

export const iconRotate: Variants = {
  initial: {
    rotate: 0,
  },
  animate: {
    rotate: 360,
    transition: {
      duration: 0.5,
      ease: 'linear',
      repeat: Infinity,
    },
  },
};

export const iconPulse: Variants = {
  initial: {
    scale: 1,
  },
  animate: {
    scale: [1, 1.2, 1],
    transition: {
      duration: 2,
      ease: easings.standard,
      repeat: Infinity,
    },
  },
};

/* ========================================
   LOADING ANIMATIONS
   ======================================== */

export const skeletonShimmer: Variants = {
  initial: {
    backgroundPosition: '-1000px 0',
  },
  animate: {
    backgroundPosition: '1000px 0',
    transition: {
      duration: 1.5,
      ease: 'linear',
      repeat: Infinity,
    },
  },
};

export const spinnerRotate: Variants = {
  animate: {
    rotate: 360,
    transition: {
      duration: 1,
      ease: 'linear',
      repeat: Infinity,
    },
  },
};

/* ========================================
   SUCCESS ANIMATIONS
   ======================================== */

export const successCheckmark: Variants = {
  hidden: {
    pathLength: 0,
    opacity: 0,
  },
  visible: {
    pathLength: 1,
    opacity: 1,
    transition: {
      duration: 0.5,
      ease: easings.standard,
    },
  },
};

export const successBounce: Variants = {
  hidden: {
    scale: 0,
    opacity: 0,
  },
  visible: {
    scale: 1,
    opacity: 1,
    transition: {
      type: 'spring',
      stiffness: 400,
      damping: 20,
    },
  },
};

/* ========================================
   SCROLL ANIMATIONS
   ======================================== */

export const fadeInUp: Variants = {
  hidden: {
    opacity: 0,
    y: 40,
  },
  visible: {
    opacity: 1,
    y: 0,
    transition: {
      duration: durations.normal,
      ease: easings.standard,
    },
  },
};

export const fadeInDown: Variants = {
  hidden: {
    opacity: 0,
    y: -40,
  },
  visible: {
    opacity: 1,
    y: 0,
    transition: {
      duration: durations.normal,
      ease: easings.standard,
    },
  },
};

export const fadeInLeft: Variants = {
  hidden: {
    opacity: 0,
    x: -40,
  },
  visible: {
    opacity: 1,
    x: 0,
    transition: {
      duration: durations.normal,
      ease: easings.standard,
    },
  },
};

export const fadeInRight: Variants = {
  hidden: {
    opacity: 0,
    x: 40,
  },
  visible: {
    opacity: 1,
    x: 0,
    transition: {
      duration: durations.normal,
      ease: easings.standard,
    },
  },
};

/* ========================================
   UTILITY FUNCTIONS
   ======================================== */

/**
 * Create a stagger container with custom delay
 */
export const createStaggerContainer = (staggerDelay: number = 0.05): Variants => ({
  hidden: { opacity: 0 },
  visible: {
    opacity: 1,
    transition: {
      staggerChildren: staggerDelay,
      delayChildren: 0.1,
    },
  },
});

/**
 * Create a fade-in animation with custom duration
 */
export const createFadeIn = (duration: number = durations.normal): Variants => ({
  hidden: { opacity: 0 },
  visible: {
    opacity: 1,
    transition: {
      duration,
      ease: easings.standard,
    },
  },
});

/**
 * Create a scale animation with custom values
 */
export const createScale = (from: number = 0.95, to: number = 1): Variants => ({
  hidden: {
    scale: from,
    opacity: 0,
  },
  visible: {
    scale: to,
    opacity: 1,
    transition: {
      duration: durations.normal,
      ease: easings.standard,
    },
  },
});
