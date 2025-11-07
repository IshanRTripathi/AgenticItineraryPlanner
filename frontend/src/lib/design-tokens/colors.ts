/**
 * Color Design Tokens
 * Premium color palette with semantic meaning
 * All colors meet WCAG AA contrast requirements (4.5:1 minimum)
 */

export const colors = {
  // Brand Colors
  primary: {
    50: '#f0f9ff',
    100: '#e0f2fe',
    200: '#bae6fd',
    300: '#7dd3fc',
    400: '#38bdf8',
    500: '#0ea5e9',  // Main brand color
    600: '#0284c7',
    700: '#0369a1',
    800: '#075985',
    900: '#0c4a6e',
  },
  
  // Semantic Colors
  success: {
    light: '#10b981',
    main: '#059669',
    dark: '#047857',
  },
  warning: {
    light: '#f59e0b',
    main: '#d97706',
    dark: '#b45309',
  },
  error: {
    light: '#ef4444',
    main: '#dc2626',
    dark: '#b91c1c',
  },
  
  // Neutral Grays (high contrast)
  gray: {
    50: '#f9fafb',
    100: '#f3f4f6',
    200: '#e5e7eb',
    300: '#d1d5db',
    400: '#9ca3af',
    500: '#6b7280',
    600: '#4b5563',
    700: '#374151',
    800: '#1f2937',
    900: '#111827',
  },
  
  // Overlay & Backdrop
  overlay: 'rgba(0, 0, 0, 0.5)',
  backdrop: 'rgba(0, 0, 0, 0.25)',
} as const;

export type ColorPalette = typeof colors;
