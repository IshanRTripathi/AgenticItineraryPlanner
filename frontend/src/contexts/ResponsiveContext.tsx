/**
 * Responsive Context
 * Provides responsive state to all components
 */

import React, { createContext, useContext, ReactNode } from 'react';
import { useMediaQuery } from '@/hooks/useMediaQuery';
import { useViewport } from '@/hooks/useViewport';
import { useOrientation } from '@/hooks/useOrientation';
import { useTouchDevice } from '@/hooks/useTouchDevice';

interface ResponsiveState {
  isMobile: boolean;
  isTablet: boolean;
  isDesktop: boolean;
  orientation: 'portrait' | 'landscape';
  touchEnabled: boolean;
  viewportWidth: number;
  viewportHeight: number;
}

const ResponsiveContext = createContext<ResponsiveState | null>(null);

interface ResponsiveProviderProps {
  children: ReactNode;
}

export function ResponsiveProvider({ children }: ResponsiveProviderProps) {
  const isMobile = useMediaQuery('(max-width: 768px)');
  const isTablet = useMediaQuery('(min-width: 768px) and (max-width: 1024px)');
  const isDesktop = useMediaQuery('(min-width: 1024px)');
  const orientation = useOrientation();
  const touchEnabled = useTouchDevice();
  const { width: viewportWidth, height: viewportHeight } = useViewport();

  const value: ResponsiveState = {
    isMobile,
    isTablet,
    isDesktop,
    orientation,
    touchEnabled,
    viewportWidth,
    viewportHeight,
  };

  return (
    <ResponsiveContext.Provider value={value}>
      {children}
    </ResponsiveContext.Provider>
  );
}

export function useResponsive(): ResponsiveState {
  const context = useContext(ResponsiveContext);
  
  if (!context) {
    throw new Error('useResponsive must be used within ResponsiveProvider');
  }
  
  return context;
}
