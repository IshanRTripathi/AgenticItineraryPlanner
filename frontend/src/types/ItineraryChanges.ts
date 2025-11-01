/**
 * Itinerary Changes Types
 * Type definitions for displaying itinerary changes in chat interface
 */

export interface ItineraryDiff {
  added: DiffItem[];
  removed: DiffItem[];
  updated: DiffItem[];
}

export interface DiffItem {
  nodeId: string;
  day: number | null;
  fields?: string[];
  title?: string;
}

export type ChangeType = 'added' | 'modified' | 'removed';

export interface ChangeDisplayItem {
  type: ChangeType;
  nodeId: string;
  day: number | null;
  title: string;
  fields?: string[];
  oldValue?: string;
  newValue?: string;
}

export interface ItineraryChangeEvent {
  type: 'itinerary_change';
  itineraryId: string;
  diff: ItineraryDiff;
  message?: string;
  timestamp: string;
  canUndo?: boolean;
}
