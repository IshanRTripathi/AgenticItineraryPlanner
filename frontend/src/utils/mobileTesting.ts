// Mobile testing utilities and helpers

export const MOBILE_BREAKPOINTS = {
  xs: 0,
  sm: 640,
  md: 768,
  lg: 1024,
  xl: 1280,
  '2xl': 1536,
} as const;

export type Breakpoint = keyof typeof MOBILE_BREAKPOINTS;

// Check if current viewport is mobile
export function isMobile(): boolean {
  if (typeof window === 'undefined') return false;
  return window.innerWidth < MOBILE_BREAKPOINTS.md;
}

// Check if current viewport is tablet
export function isTablet(): boolean {
  if (typeof window === 'undefined') return false;
  return window.innerWidth >= MOBILE_BREAKPOINTS.md && window.innerWidth < MOBILE_BREAKPOINTS.lg;
}

// Check if current viewport is desktop
export function isDesktop(): boolean {
  if (typeof window === 'undefined') return false;
  return window.innerWidth >= MOBILE_BREAKPOINTS.lg;
}

// Get current breakpoint
export function getCurrentBreakpoint(): Breakpoint {
  if (typeof window === 'undefined') return 'lg';
  
  const width = window.innerWidth;
  
  if (width < MOBILE_BREAKPOINTS.sm) return 'xs';
  if (width < MOBILE_BREAKPOINTS.md) return 'sm';
  if (width < MOBILE_BREAKPOINTS.lg) return 'md';
  if (width < MOBILE_BREAKPOINTS.xl) return 'lg';
  if (width < MOBILE_BREAKPOINTS['2xl']) return 'xl';
  return '2xl';
}

// Check if touch is supported
export function isTouchDevice(): boolean {
  if (typeof window === 'undefined') return false;
  return 'ontouchstart' in window || navigator.maxTouchPoints > 0;
}

// Get device pixel ratio
export function getDevicePixelRatio(): number {
  if (typeof window === 'undefined') return 1;
  return window.devicePixelRatio || 1;
}

// Check if device is high DPI
export function isHighDPI(): boolean {
  return getDevicePixelRatio() > 1;
}

// Mobile performance testing
export function measurePerformance(): {
  connection: string;
  memory: number | null;
  timing: PerformanceNavigationTiming | null;
} {
  if (typeof window === 'undefined') {
    return { connection: 'unknown', memory: null, timing: null };
  }

  const connection = (navigator as any).connection || (navigator as any).mozConnection || (navigator as any).webkitConnection;
  const memory = (performance as any).memory;
  const timing = performance.getEntriesByType('navigation')[0] as PerformanceNavigationTiming;

  return {
    connection: connection?.effectiveType || 'unknown',
    memory: memory?.usedJSHeapSize || null,
    timing,
  };
}

// Test touch target size (should be at least 44px)
export function validateTouchTarget(element: HTMLElement): boolean {
  const rect = element.getBoundingClientRect();
  const minSize = 44; // WCAG minimum touch target size
  
  return rect.width >= minSize && rect.height >= minSize;
}

// Test if element is visible in viewport
export function isElementInViewport(element: HTMLElement): boolean {
  const rect = element.getBoundingClientRect();
  return (
    rect.top >= 0 &&
    rect.left >= 0 &&
    rect.bottom <= (window.innerHeight || document.documentElement.clientHeight) &&
    rect.right <= (window.innerWidth || document.documentElement.clientWidth)
  );
}

// Mobile-specific CSS class utilities
export function getMobileClasses(baseClasses: string, mobileClasses?: string): string {
  if (!mobileClasses) return baseClasses;
  
  const isMobileDevice = isMobile();
  return isMobileDevice ? `${baseClasses} ${mobileClasses}` : baseClasses;
}

// Responsive image src generation
export function generateResponsiveImageSrc(
  baseSrc: string,
  width: number,
  quality: number = 75
): string {
  // This would typically integrate with your image optimization service
  return `${baseSrc}?w=${width}&q=${quality}`;
}

// Mobile viewport meta tag validation
export function validateViewportMeta(): boolean {
  if (typeof document === 'undefined') return false;
  
  const viewportMeta = document.querySelector('meta[name="viewport"]');
  if (!viewportMeta) return false;
  
  const content = viewportMeta.getAttribute('content');
  return content?.includes('width=device-width') && content?.includes('initial-scale=1');
}

// Test mobile navigation accessibility
export function testMobileNavigation(): {
  hasHamburgerMenu: boolean;
  hasSkipLinks: boolean;
  hasProperFocus: boolean;
} {
  if (typeof document === 'undefined') {
    return { hasHamburgerMenu: false, hasSkipLinks: false, hasProperFocus: false };
  }

  const hamburgerMenu = document.querySelector('[data-testid="hamburger-menu"], .hamburger, [aria-label*="menu"]');
  const skipLinks = document.querySelectorAll('a[href^="#"], [data-skip-link]');
  const focusableElements = document.querySelectorAll('button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])');

  return {
    hasHamburgerMenu: !!hamburgerMenu,
    hasSkipLinks: skipLinks.length > 0,
    hasProperFocus: focusableElements.length > 0,
  };
}
