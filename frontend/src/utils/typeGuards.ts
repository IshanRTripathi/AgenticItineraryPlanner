/**
 * Type Guard Utilities
 * 
 * Runtime type checking for itinerary data formats.
 * Helps distinguish between NormalizedItinerary and TripData during migration.
 * 
 * @module typeGuards
 */

import { NormalizedItinerary, NormalizedDay, NormalizedNode } from '../types/NormalizedItinerary';
import { TripData, DayPlan, TripComponent } from '../types/TripData';

/**
 * Type guard to check if data is NormalizedItinerary format
 * 
 * @param data - Data to check
 * @returns True if data is NormalizedItinerary
 * 
 * @example
 * if (isNormalizedItinerary(data)) {
 *   // TypeScript knows data is NormalizedItinerary
 *   console.log(data.itineraryId);
 * }
 */
export function isNormalizedItinerary(data: any): data is NormalizedItinerary {
  return (
    data &&
    typeof data === 'object' &&
    'itineraryId' in data &&
    'days' in data &&
    Array.isArray(data.days) &&
    'currency' in data &&
    'themes' in data &&
    Array.isArray(data.themes)
  );
}

/**
 * Type guard to check if data is TripData format (legacy)
 * 
 * @param data - Data to check
 * @returns True if data is TripData
 * 
 * @example
 * if (isTripData(data)) {
 *   // TypeScript knows data is TripData
 *   console.log(data.id);
 * }
 */
export function isTripData(data: any): data is TripData {
  return (
    data &&
    typeof data === 'object' &&
    'id' in data &&
    'itinerary' in data &&
    typeof data.itinerary === 'object' &&
    'budget' in data &&
    'preferences' in data
  );
}

/**
 * Type guard to check if data is NormalizedDay
 * 
 * @param data - Data to check
 * @returns True if data is NormalizedDay
 */
export function isNormalizedDay(data: any): data is NormalizedDay {
  return (
    data &&
    typeof data === 'object' &&
    'dayNumber' in data &&
    'date' in data &&
    'nodes' in data &&
    Array.isArray(data.nodes)
  );
}

/**
 * Type guard to check if data is DayPlan (legacy)
 * 
 * @param data - Data to check
 * @returns True if data is DayPlan
 */
export function isDayPlan(data: any): data is DayPlan {
  return (
    data &&
    typeof data === 'object' &&
    'dayNumber' in data &&
    'date' in data &&
    'components' in data &&
    Array.isArray(data.components)
  );
}

/**
 * Type guard to check if data is NormalizedNode
 * 
 * @param data - Data to check
 * @returns True if data is NormalizedNode
 */
export function isNormalizedNode(data: any): data is NormalizedNode {
  return (
    data &&
    typeof data === 'object' &&
    'id' in data &&
    'type' in data &&
    'title' in data &&
    ['attraction', 'meal', 'hotel', 'accommodation', 'transit', 'transport'].includes(data.type)
  );
}

/**
 * Type guard to check if data is TripComponent (legacy)
 * 
 * @param data - Data to check
 * @returns True if data is TripComponent
 */
export function isTripComponent(data: any): data is TripComponent {
  return (
    data &&
    typeof data === 'object' &&
    'id' in data &&
    'type' in data &&
    'name' in data &&
    ['attraction', 'hotel', 'restaurant', 'activity', 'transport', 'shopping', 'entertainment'].includes(data.type)
  );
}

/**
 * Assert that data is NormalizedItinerary, throw error if not
 * 
 * @param data - Data to assert
 * @param message - Custom error message
 * @throws Error if data is not NormalizedItinerary
 */
export function assertNormalizedItinerary(data: any, message?: string): asserts data is NormalizedItinerary {
  if (!isNormalizedItinerary(data)) {
    throw new Error(message || 'Expected NormalizedItinerary but got different format');
  }
}

/**
 * Assert that data is TripData, throw error if not
 * 
 * @param data - Data to assert
 * @param message - Custom error message
 * @throws Error if data is not TripData
 */
export function assertTripData(data: any, message?: string): asserts data is TripData {
  if (!isTripData(data)) {
    throw new Error(message || 'Expected TripData but got different format');
  }
}

/**
 * Get the format type of itinerary data
 * 
 * @param data - Data to check
 * @returns Format type or 'unknown'
 * 
 * @example
 * const format = getItineraryFormat(data);
 * if (format === 'normalized') {
 *   // Handle NormalizedItinerary
 * } else if (format === 'legacy') {
 *   // Handle TripData
 * }
 */
export function getItineraryFormat(data: any): 'normalized' | 'legacy' | 'unknown' {
  if (isNormalizedItinerary(data)) return 'normalized';
  if (isTripData(data)) return 'legacy';
  return 'unknown';
}

/**
 * Check if data is in any valid itinerary format
 * 
 * @param data - Data to check
 * @returns True if data is either NormalizedItinerary or TripData
 */
export function isValidItineraryFormat(data: any): data is NormalizedItinerary | TripData {
  return isNormalizedItinerary(data) || isTripData(data);
}
