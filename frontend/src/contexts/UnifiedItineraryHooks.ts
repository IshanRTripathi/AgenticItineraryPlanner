import React, { useContext, useMemo } from 'react';
import { NormalizedNode } from '../types/NormalizedItinerary';
import { UnifiedItineraryState, UnifiedItineraryContextType } from './UnifiedItineraryTypes';

// Forward declaration to avoid circular dependency
// The actual context will be imported at runtime
let UnifiedItineraryContext: React.Context<UnifiedItineraryContextType | null>;

export function setUnifiedItineraryContext(context: React.Context<UnifiedItineraryContextType | null>) {
  UnifiedItineraryContext = context;
}

/**
 * Custom hooks for the Unified Itinerary Context
 * Extracted from UnifiedItineraryContext.tsx for better maintainability
 */

/**
 * Hook to use the unified itinerary context
 */
export function useUnifiedItinerary(): UnifiedItineraryContextType {
  const context = useContext(UnifiedItineraryContext);
  if (!context) {
    throw new Error('useUnifiedItinerary must be used within a UnifiedItineraryProvider');
  }
  return context;
}

/**
 * Hook to get specific parts of the state (for performance optimization)
 */
export function useUnifiedItinerarySelector<T>(selector: (state: UnifiedItineraryState) => T): T {
  const { state } = useUnifiedItinerary();
  return useMemo(() => selector(state), [selector, state]);
}

/**
 * Utility function creators
 */
export const createGetSelectedNodes = (state: UnifiedItineraryState) => {
  return (): NormalizedNode[] => {
    if (!state.itinerary || state.selectedNodeIds.length === 0) return [];
    
    const nodes: NormalizedNode[] = [];
    state.itinerary.itinerary?.days.forEach(day => {
      day.nodes?.forEach(node => {
        if (state.selectedNodeIds.includes(node.id)) {
          nodes.push(node);
        }
      });
    });
    return nodes;
  };
};

export const createGetCurrentDay = (state: UnifiedItineraryState) => {
  return (): any | null => {
    if (!state.itinerary?.itinerary || state.selectedDay < 0 || state.selectedDay >= state.itinerary.itinerary.days.length) {
      return null;
    }
    return state.itinerary.itinerary.days[state.selectedDay];
  };
};

export const createHasUnsavedChanges = (state: UnifiedItineraryState) => {
  return (): boolean => {
    return state.pendingChanges.length > 0;
  };
};
