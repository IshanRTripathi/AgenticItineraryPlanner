/**
 * useNormalizedItinerary Hook
 * 
 * Custom hook for working with NormalizedItinerary with convenient accessors.
 * Provides memoized computed values and helper methods.
 * 
 * @module useNormalizedItinerary
 */

import { useMemo } from 'react';
import { NormalizedItinerary, NormalizedDay, NormalizedNode } from '../types/NormalizedItinerary';
import { ItineraryAdapter } from '../utils/itineraryAdapter';

/**
 * Hook return type with convenient accessors
 */
export interface UseNormalizedItineraryReturn {
  // Raw data
  raw: NormalizedItinerary;
  
  // Basic info
  id: string;
  summary: string;
  currency: string;
  themes: string[];
  destination?: string;
  
  // Days
  days: NormalizedDay[];
  sortedDays: NormalizedDay[];
  dayCount: number;
  
  // Computed values
  dateRange: { start: string; end: string };
  totalCost: number;
  totalDistance: number;
  
  // Statistics
  statistics: ReturnType<typeof ItineraryAdapter.getStatistics>;
  uniqueLocations: string[];
  
  // Helper methods
  getDayByNumber: (dayNumber: number) => NormalizedDay | undefined;
  getDayById: (dayId: string) => NormalizedDay | undefined;
  findNodeById: (nodeId: string) => { day: NormalizedDay; node: NormalizedNode } | null;
  getAllNodes: () => NormalizedNode[];
  getAllNodesByType: (type: NormalizedNode['type']) => NormalizedNode[];
  
  // Validation
  hasDays: boolean;
  hasNodes: boolean;
}

/**
 * Custom hook to work with NormalizedItinerary
 * 
 * Provides convenient accessors and computed values for NormalizedItinerary data.
 * All computed values are memoized for performance.
 * 
 * @param itinerary - NormalizedItinerary data (can be null during loading)
 * @returns Object with convenient accessors and helper methods, or null if itinerary is null
 * 
 * @example
 * ```tsx
 * function MyComponent({ itineraryId }: { itineraryId: string }) {
 *   const { data: itinerary } = useItinerary(itineraryId);
 *   const normalized = useNormalizedItinerary(itinerary);
 *   
 *   if (!normalized) return <LoadingState />;
 *   
 *   return (
 *     <div>
 *       <h1>{normalized.summary}</h1>
 *       <p>Total Cost: {normalized.totalCost} {normalized.currency}</p>
 *       <p>Days: {normalized.dayCount}</p>
 *       <p>Locations: {normalized.uniqueLocations.join(', ')}</p>
 *     </div>
 *   );
 * }
 * ```
 */
export function useNormalizedItinerary(
  itinerary: NormalizedItinerary | null
): UseNormalizedItineraryReturn | null {
  return useMemo(() => {
    if (!itinerary) return null;
    
    // Compute all values once
    const sortedDays = ItineraryAdapter.getSortedDays(itinerary);
    const dateRange = ItineraryAdapter.getDateRange(itinerary);
    const totalCost = ItineraryAdapter.getTotalCost(itinerary);
    const totalDistance = ItineraryAdapter.getTotalDistance(itinerary);
    const statistics = ItineraryAdapter.getStatistics(itinerary);
    const uniqueLocations = ItineraryAdapter.getUniqueLocations(itinerary);
    
    return {
      // Raw data
      raw: itinerary,
      
      // Basic info
      id: itinerary.itineraryId,
      summary: itinerary.summary,
      currency: itinerary.currency,
      themes: itinerary.themes,
      destination: itinerary.destination,
      
      // Days
      days: itinerary.days,
      sortedDays,
      dayCount: itinerary.days.length,
      
      // Computed values
      dateRange,
      totalCost,
      totalDistance,
      
      // Statistics
      statistics,
      uniqueLocations,
      
      // Helper methods (bound to current itinerary)
      getDayByNumber: (dayNumber: number) => 
        ItineraryAdapter.getDayByNumber(itinerary, dayNumber),
      getDayById: (dayId: string) => 
        ItineraryAdapter.getDayById(itinerary, dayId),
      findNodeById: (nodeId: string) => 
        ItineraryAdapter.findNodeById(itinerary, nodeId),
      getAllNodes: () => 
        ItineraryAdapter.getAllNodes(itinerary),
      getAllNodesByType: (type: NormalizedNode['type']) => 
        ItineraryAdapter.getAllNodesByType(itinerary, type),
      
      // Validation
      hasDays: itinerary.days.length > 0,
      hasNodes: ItineraryAdapter.getAllNodes(itinerary).length > 0,
    };
  }, [itinerary]);
}

/**
 * Hook to work with a specific day from NormalizedItinerary
 * 
 * @param itinerary - NormalizedItinerary data
 * @param dayNumber - Day number (1-indexed)
 * @returns Day data with helper methods, or null if not found
 * 
 * @example
 * ```tsx
 * function DayView({ itinerary, dayNumber }: Props) {
 *   const day = useNormalizedDay(itinerary, dayNumber);
 *   
 *   if (!day) return <div>Day not found</div>;
 *   
 *   return (
 *     <div>
 *       <h2>Day {day.dayNumber}: {day.location}</h2>
 *       <p>Nodes: {day.nodeCount}</p>
 *       <p>Cost: {day.cost}</p>
 *     </div>
 *   );
 * }
 * ```
 */
export function useNormalizedDay(
  itinerary: NormalizedItinerary | null,
  dayNumber: number
) {
  return useMemo(() => {
    if (!itinerary) return null;
    
    const day = ItineraryAdapter.getDayByNumber(itinerary, dayNumber);
    if (!day) return null;
    
    return {
      // Raw data
      raw: day,
      
      // Basic info
      id: `day-${day.dayNumber}`, // Generate ID from dayNumber
      dayNumber: day.dayNumber,
      date: day.date,
      location: day.location,
      notes: day.notes,
      
      // Nodes
      nodes: day.nodes,
      nodeCount: day.nodes.length,
      
      // Computed values
      cost: ItineraryAdapter.getDayCost(day),
      distance: ItineraryAdapter.getDayDistance(day),
      timeWindow: day.timeWindow,
      
      // Filtered nodes
      mealNodes: ItineraryAdapter.getMealNodes(day),
      attractionNodes: ItineraryAdapter.getAttractionNodes(day),
      accommodationNode: ItineraryAdapter.getAccommodationNode(day),
      lockedNodes: ItineraryAdapter.getLockedNodes(day),
      
      // Helper methods
      getNodeById: (nodeId: string) => 
        ItineraryAdapter.getNodeById(day, nodeId),
      getNodesByType: (type: NormalizedNode['type']) => 
        ItineraryAdapter.getNodesByType(day, type),
      
      // Validation
      hasNodes: day.nodes.length > 0,
    };
  }, [itinerary, dayNumber]);
}

/**
 * Hook to get statistics for an itinerary
 * 
 * @param itinerary - NormalizedItinerary data
 * @returns Statistics object
 * 
 * @example
 * ```tsx
 * function StatsPanel({ itinerary }: Props) {
 *   const stats = useItineraryStatistics(itinerary);
 *   
 *   if (!stats) return null;
 *   
 *   return (
 *     <div>
 *       <p>Total Days: {stats.totalDays}</p>
 *       <p>Total Activities: {stats.totalNodes}</p>
 *       <p>Attractions: {stats.nodesByType.attractions}</p>
 *       <p>Meals: {stats.nodesByType.meals}</p>
 *     </div>
 *   );
 * }
 * ```
 */
export function useItineraryStatistics(itinerary: NormalizedItinerary | null) {
  return useMemo(() => {
    if (!itinerary) return null;
    return ItineraryAdapter.getStatistics(itinerary);
  }, [itinerary]);
}

/**
 * Hook to get all nodes of a specific type across all days
 * 
 * @param itinerary - NormalizedItinerary data
 * @param type - Node type to filter
 * @returns Array of nodes of the specified type
 * 
 * @example
 * ```tsx
 * function AttractionsList({ itinerary }: Props) {
 *   const attractions = useNodesByType(itinerary, 'attraction');
 *   
 *   return (
 *     <ul>
 *       {attractions.map(node => (
 *         <li key={node.id}>{node.title}</li>
 *       ))}
 *     </ul>
 *   );
 * }
 * ```
 */
export function useNodesByType(
  itinerary: NormalizedItinerary | null,
  type: NormalizedNode['type']
) {
  return useMemo(() => {
    if (!itinerary) return [];
    return ItineraryAdapter.getAllNodesByType(itinerary, type);
  }, [itinerary, type]);
}
