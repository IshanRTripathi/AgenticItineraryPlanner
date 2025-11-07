/**
 * Test Utilities
 * Helper functions for testing responsive components
 */

/**
 * Mock window.matchMedia for testing
 */
export function mockMatchMedia(query: string, matches: boolean) {
  Object.defineProperty(window, 'matchMedia', {
    writable: true,
    value: (q: string) => ({
      matches: q === query ? matches : false,
      media: q,
      onchange: null,
      addListener: () => {},
      removeListener: () => {},
      addEventListener: () => {},
      removeEventListener: () => {},
      dispatchEvent: () => true,
    }),
  });
}

/**
 * Set viewport size for testing
 */
export function setViewport(width: number, height: number) {
  Object.defineProperty(window, 'innerWidth', {
    writable: true,
    configurable: true,
    value: width,
  });
  
  Object.defineProperty(window, 'innerHeight', {
    writable: true,
    configurable: true,
    value: height,
  });
  
  // Trigger resize event
  window.dispatchEvent(new Event('resize'));
}

/**
 * Common viewport sizes for testing
 */
export const VIEWPORTS = {
  mobile: {
    iPhoneSE: { width: 375, height: 667, name: 'iPhone SE' },
    iPhone12: { width: 390, height: 844, name: 'iPhone 12/13' },
    iPhone14ProMax: { width: 430, height: 932, name: 'iPhone 14 Pro Max' },
    galaxyS21: { width: 360, height: 800, name: 'Samsung Galaxy S21' },
  },
  tablet: {
    iPadMini: { width: 768, height: 1024, name: 'iPad Mini' },
    iPadPro: { width: 1024, height: 1366, name: 'iPad Pro' },
  },
  desktop: {
    laptop: { width: 1280, height: 800, name: 'Laptop' },
    desktop: { width: 1920, height: 1080, name: 'Desktop' },
  },
};

/**
 * Mock touch device
 */
export function mockTouchDevice(isTouch: boolean) {
  if (isTouch) {
    Object.defineProperty(window, 'ontouchstart', {
      writable: true,
      configurable: true,
      value: () => {},
    });
    
    Object.defineProperty(navigator, 'maxTouchPoints', {
      writable: true,
      configurable: true,
      value: 5,
    });
  } else {
    delete (window as any).ontouchstart;
    
    Object.defineProperty(navigator, 'maxTouchPoints', {
      writable: true,
      configurable: true,
      value: 0,
    });
  }
}

/**
 * Wait for async updates in tests
 */
export const waitFor = (ms: number) => new Promise(resolve => setTimeout(resolve, ms));
