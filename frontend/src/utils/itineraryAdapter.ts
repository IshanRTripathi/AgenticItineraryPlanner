/**
 * Itinerary Adapter Utilities
 * 
 * Minimal adapter utilities for working with NormalizedItinerary.
 * Provides convenient accessors and helper functions without transformation overhead.
 * 
 * @module itineraryAdapter
 */

import { 
  NormalizedItinerary, 
  NormalizedDay, 
  NormalizedNode 
} from '../types/NormalizedItinerary';

/**
 * Adapter class providing utility methods for NormalizedItinerary
 */
export class ItineraryAdapter {
  
  // ============================================================================
  // ID Accessors
  // ============================================================================
  
  /**
   * Get the itinerary ID
   */
  static getId(itinerary: NormalizedItinerary): string {
    return itinerary.itineraryId;
  }
  
  // ============================================================================
  // Date Accessors
  // ============================================================================
  
  /**
   * Get the date range from itinerary
   * Extracts from days array if startDate/endDate not present
   */
  static getDateRange(itinerary: NormalizedItinerary): { start: string; end: string } {
    // Use explicit dates if available
    if (itinerary.startDate && itinerary.endDate) {
      return {
        start: itinerary.startDate,
        end: itinerary.endDate
      };
    }
    
    // Extract from days
    if (itinerary.days.length === 0) {
      const today = new Date().toISOString().split('T')[0];
      return { start: today, end: today };
    }
    
    const sorted = [...itinerary.days].sort((a, b) => a.dayNumber - b.dayNumber);
    return {
      start: sorted[0].date,
      end: sorted[sorted.length - 1].date
    };
  }
  
  /**
   * Get the duration in days
   */
  static getDuration(itinerary: NormalizedItinerary): number {
    return itinerary.days.length;
  }
  
  // ============================================================================
  // Cost Accessors
  // ============================================================================
  
  /**
   * Calculate total cost from all days
   */
  static getTotalCost(itinerary: NormalizedItinerary): number {
    return itinerary.days.reduce((total, day) => total + (day.totals?.cost || 0), 0);
  }
  
  /**
   * Get cost for a specific day
   */
  static getDayCost(day: NormalizedDay): number {
    return day.totals?.cost || 0;
  }
  
  // ============================================================================
  // Distance Accessors
  // ============================================================================
  
  /**
   * Calculate total distance from all days
   */
  static getTotalDistance(itinerary: NormalizedItinerary): number {
    return itinerary.days.reduce((total, day) => total + (day.totals?.distanceKm || 0), 0);
  }
  
  /**
   * Get distance for a specific day
   */
  static getDayDistance(day: NormalizedDay): number {
    return day.totals?.distanceKm || 0;
  }
  
  // ============================================================================
  // Day Accessors
  // ============================================================================
  
  /**
   * Get a day by its ID (using dayNumber as ID)
   * Note: NormalizedDay doesn't have an id field, so we use dayNumber
   */
  static getDayById(itinerary: NormalizedItinerary, dayId: string | number): NormalizedDay | undefined {
    const dayNum = typeof dayId === 'string' ? parseInt(dayId.replace(/\D/g, ''), 10) : dayId;
    return itinerary.days.find(d => d.dayNumber === dayNum);
  }
  
  /**
   * Get a day by its number (1-indexed)
   */
  static getDayByNumber(itinerary: NormalizedItinerary, dayNumber: number): NormalizedDay | undefined {
    return itinerary.days.find(d => d.dayNumber === dayNumber);
  }
  
  /**
   * Get all days sorted by day number
   */
  static getSortedDays(itinerary: NormalizedItinerary): NormalizedDay[] {
    return [...itinerary.days].sort((a, b) => a.dayNumber - b.dayNumber);
  }
  
  // ============================================================================
  // Node Accessors
  // ============================================================================
  
  /**
   * Get a node by its ID from a specific day
   */
  static getNodeById(day: NormalizedDay, nodeId: string): NormalizedNode | undefined {
    return day.nodes.find(n => n.id === nodeId);
  }
  
  /**
   * Get all nodes of a specific type from a day
   */
  static getNodesByType(day: NormalizedDay, type: NormalizedNode['type']): NormalizedNode[] {
    return day.nodes.filter(n => n.type === type);
  }
  
  /**
   * Get all meal nodes from a day
   */
  static getMealNodes(day: NormalizedDay): NormalizedNode[] {
    return day.nodes.filter(n => n.type === 'meal');
  }
  
  /**
   * Get the accommodation node from a day (first one found)
   */
  static getAccommodationNode(day: NormalizedDay): NormalizedNode | undefined {
    return day.nodes.find(n => n.type === 'accommodation' || n.type === 'hotel');
  }
  
  /**
   * Get all attraction nodes from a day
   */
  static getAttractionNodes(day: NormalizedDay): NormalizedNode[] {
    return day.nodes.filter(n => n.type === 'attraction');
  }
  
  /**
   * Get all locked nodes from a day
   */
  static getLockedNodes(day: NormalizedDay): NormalizedNode[] {
    return day.nodes.filter(n => n.locked === true);
  }
  
  // ============================================================================
  // Type Conversions
  // ============================================================================
  
  /**
   * Convert NormalizedNode type to legacy component type
   * Used for backward compatibility during migration
   */
  static getComponentType(nodeType: NormalizedNode['type']): string {
    const typeMap: Record<NormalizedNode['type'], string> = {
      'attraction': 'attraction',
      'meal': 'restaurant',
      'accommodation': 'hotel',
      'hotel': 'hotel',
      'transit': 'transport',
      'transport': 'transport'
    };
    return typeMap[nodeType] || 'activity';
  }
  
  /**
   * Get price range category from cost amount
   */
  static getPriceRange(amount: number): 'budget' | 'mid-range' | 'luxury' {
    if (amount === 0) return 'budget';
    if (amount < 50) return 'budget';
    if (amount < 150) return 'mid-range';
    return 'luxury';
  }
  
  // ============================================================================
  // Time Conversions
  // ============================================================================
  
  /**
   * Convert milliseconds since epoch to HH:MM time string
   */
  static millisecondsToTimeString(ms?: number): string | null {
    if (!ms) return null;
    
    try {
      const date = new Date(ms);
      const hours = date.getHours().toString().padStart(2, '0');
      const minutes = date.getMinutes().toString().padStart(2, '0');
      return `${hours}:${minutes}`;
    } catch (error) {
      console.warn('Failed to convert milliseconds to time string:', ms);
      return null;
    }
  }
  
  /**
   * Convert HH:MM time string to milliseconds since epoch (today's date)
   */
  static timeStringToMilliseconds(timeString: string): number | null {
    if (!timeString) return null;
    
    try {
      const [hours, minutes] = timeString.split(':').map(Number);
      const date = new Date();
      date.setHours(hours, minutes, 0, 0);
      return date.getTime();
    } catch (error) {
      console.warn('Failed to convert time string to milliseconds:', timeString);
      return null;
    }
  }
  
  // ============================================================================
  // Validation Helpers
  // ============================================================================
  
  /**
   * Check if itinerary has any days
   */
  static hasDays(itinerary: NormalizedItinerary): boolean {
    return itinerary.days.length > 0;
  }
  
  /**
   * Check if day has any nodes
   */
  static hasNodes(day: NormalizedDay): boolean {
    return day.nodes.length > 0;
  }
  
  /**
   * Check if node has valid coordinates
   */
  static hasValidCoordinates(node: NormalizedNode): boolean {
    return !!(
      node.location?.coordinates &&
      typeof node.location.coordinates.lat === 'number' &&
      typeof node.location.coordinates.lng === 'number' &&
      !isNaN(node.location.coordinates.lat) &&
      !isNaN(node.location.coordinates.lng)
    );
  }
  
  // ============================================================================
  // Search & Filter Helpers
  // ============================================================================
  
  /**
   * Find a node across all days by its ID
   */
  static findNodeById(itinerary: NormalizedItinerary, nodeId: string): { day: NormalizedDay; node: NormalizedNode } | null {
    for (const day of itinerary.days) {
      const node = day.nodes.find(n => n.id === nodeId);
      if (node) {
        return { day, node };
      }
    }
    return null;
  }
  
  /**
   * Get all nodes across all days
   */
  static getAllNodes(itinerary: NormalizedItinerary): NormalizedNode[] {
    return itinerary.days.flatMap(day => day.nodes);
  }
  
  /**
   * Get all nodes of a specific type across all days
   */
  static getAllNodesByType(itinerary: NormalizedItinerary, type: NormalizedNode['type']): NormalizedNode[] {
    return this.getAllNodes(itinerary).filter(n => n.type === type);
  }
  
  /**
   * Extract unique locations from itinerary
   */
  static getUniqueLocations(itinerary: NormalizedItinerary): string[] {
    const locations = new Set<string>();
    
    // Add destination if available
    if (itinerary.destination) {
      locations.add(itinerary.destination);
    }
    
    // Add day locations
    itinerary.days.forEach(day => {
      if (day.location) {
        locations.add(day.location);
      }
    });
    
    return Array.from(locations);
  }
  
  // ============================================================================
  // Statistics Helpers
  // ============================================================================
  
  /**
   * Get statistics for the itinerary
   */
  static getStatistics(itinerary: NormalizedItinerary) {
    const allNodes = this.getAllNodes(itinerary);
    
    return {
      totalDays: itinerary.days.length,
      totalNodes: allNodes.length,
      totalCost: this.getTotalCost(itinerary),
      totalDistance: this.getTotalDistance(itinerary),
      nodesByType: {
        attractions: allNodes.filter(n => n.type === 'attraction').length,
        meals: allNodes.filter(n => n.type === 'meal').length,
        accommodations: allNodes.filter(n => n.type === 'accommodation' || n.type === 'hotel').length,
        transport: allNodes.filter(n => n.type === 'transit' || n.type === 'transport').length,
      },
      lockedNodes: allNodes.filter(n => n.locked === true).length,
    };
  }
}

/**
 * Export convenience functions for direct use
 */
export const {
  getId,
  getDateRange,
  getDuration,
  getTotalCost,
  getDayCost,
  getTotalDistance,
  getDayDistance,
  getDayById,
  getDayByNumber,
  getSortedDays,
  getNodeById,
  getNodesByType,
  getMealNodes,
  getAccommodationNode,
  getAttractionNodes,
  getLockedNodes,
  getComponentType,
  getPriceRange,
  millisecondsToTimeString,
  timeStringToMilliseconds,
  hasDays,
  hasNodes,
  hasValidCoordinates,
  findNodeById,
  getAllNodes,
  getAllNodesByType,
  getUniqueLocations,
  getStatistics,
} = ItineraryAdapter;
