/**
 * useTouchDevice Hook
 * Detect if the device supports touch input
 */

import { useState, useEffect } from 'react';

export function useTouchDevice(): boolean {
  const [isTouchDevice, setIsTouchDevice] = useState(false);

  useEffect(() => {
    // Check multiple indicators for touch support
    const hasTouchScreen = 
      'ontouchstart' in window ||
      navigator.maxTouchPoints > 0 ||
      // @ts-ignore - for older browsers
      navigator.msMaxTouchPoints > 0;

    setIsTouchDevice(hasTouchScreen);
  }, []);

  return isTouchDevice;
}
