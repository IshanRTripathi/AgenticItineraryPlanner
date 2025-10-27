/**
 * Motion Design Tokens
 * Based on Material Design 3 and Apple's Human Interface Guidelines
 */

export const motion = {
  // Duration (in milliseconds)
  duration: {
    instant: 100,      // Micro-interactions (hover, focus)
    fast: 150,         // Button clicks, toggles
    normal: 250,       // Card animations, dropdowns
    slow: 400,         // Page transitions, modals
    slower: 600,       // Complex animations
    slowest: 800,      // Success animations, celebrations
  },
  
  // Easing Functions
  easing: {
    // Entrances (ease-out)
    easeOut: 'cubic-bezier(0.0, 0.0, 0.2, 1)',
    easeOutQuart: 'cubic-bezier(0.165, 0.84, 0.44, 1)',
    
    // Exits (ease-in)
    easeIn: 'cubic-bezier(0.4, 0.0, 1, 1)',
    easeInQuart: 'cubic-bezier(0.895, 0.03, 0.685, 0.22)',
    
    // Movements (ease-in-out)
    easeInOut: 'cubic-bezier(0.4, 0.0, 0.2, 1)',
    easeInOutQuart: 'cubic-bezier(0.77, 0, 0.175, 1)',
    
    // Spring (natural motion)
    spring: 'cubic-bezier(0.68, -0.55, 0.265, 1.55)',
  },
  
  // Spring Configurations (for Framer Motion)
  spring: {
    gentle: { type: 'spring' as const, stiffness: 120, damping: 14 },
    snappy: { type: 'spring' as const, stiffness: 300, damping: 25 },
    bouncy: { type: 'spring' as const, stiffness: 400, damping: 10 },
  },
} as const;

export type MotionTokens = typeof motion;
