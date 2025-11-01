/**
 * Day Color Palette
 * Shared colors for day visualization across map markers and day cards
 * Ensures visual consistency throughout the app
 */

export interface DayColor {
  primary: string;
  light: string;
  name: string;
}

/**
 * Day colors for route visualization and day cards
 * Colors cycle through for trips longer than 7 days
 */
export const DAY_COLORS: DayColor[] = [
  { primary: '#EF4444', light: '#FCA5A5', name: 'Red' },      // Day 1
  { primary: '#F59E0B', light: '#FCD34D', name: 'Amber' },    // Day 2
  { primary: '#10B981', light: '#6EE7B7', name: 'Green' },    // Day 3
  { primary: '#3B82F6', light: '#93C5FD', name: 'Blue' },     // Day 4
  { primary: '#8B5CF6', light: '#C4B5FD', name: 'Purple' },   // Day 5
  { primary: '#EC4899', light: '#F9A8D4', name: 'Pink' },     // Day 6
  { primary: '#14B8A6', light: '#5EEAD4', name: 'Teal' },     // Day 7
];

/**
 * Get color for a specific day number
 * @param dayNumber - 1-indexed day number
 * @returns DayColor object with primary, light, and name
 */
export function getDayColor(dayNumber: number): DayColor {
  return DAY_COLORS[(dayNumber - 1) % DAY_COLORS.length];
}

/**
 * Get all day colors for a trip
 * @param totalDays - Total number of days in the trip
 * @returns Array of DayColor objects
 */
export function getTripColors(totalDays: number): DayColor[] {
  return Array.from({ length: totalDays }, (_, i) => getDayColor(i + 1));
}
