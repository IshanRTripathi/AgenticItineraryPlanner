import React from 'react';
import { TripData } from '../../../types/TripData';

// Core interfaces for component communication
export interface TravelPlannerProps {
  tripData: TripData;
  onSave: (updatedTrip: TripData) => void;
  onBack: () => void;
  onShare: () => void;
  onExportPDF: () => void;
}

export interface ViewComponentProps {
  tripData: TripData;
  onUpdate?: (updates: Partial<TripData>) => void;
  onDaySelect?: (dayNumber: number, dayData: any) => void;
  isCollapsed?: boolean;
  onRefresh?: () => void;
}

// Destination management interfaces
export interface TransportDetails {
  id: string;
  mode: 'walk' | 'cab' | 'flight' | 'other';
  distance: string;
  distanceUnit: 'km' | 'mi';
  duration?: string;
  cost?: string;
  notes?: string;
}

export interface Destination {
  id: string;
  name: string;
  nights: number;
  sleeping: boolean;
  transports?: TransportDetails[];
  notes: string;
  lat?: number;
  lng?: number;
}

export interface DestinationManagerProps {
  destinations: Destination[];
  currency: string;
  showNotes: boolean;
  onUpdate: (id: string, updates: Partial<Destination>) => void;
  onAdd: (destination: Omit<Destination, 'id'>) => void;
  onRemove: (id: string) => void;
  onCurrencyChange: (currency: string) => void;
  onToggleNotes: () => void;
  onUpdateTransport: (fromId: string, toId: string, transports: TransportDetails[]) => void;
}

// Agent status interfaces
export interface AgentStatus {
  id: string;
  kind: 'planner' | 'places' | 'route' | 'hotels' | 'flights' | 'activities' | 'bus' | 'train' | 'pt' | 'food' | 'photo' | 'packing' | 'cost';
  status: 'queued' | 'running' | 'completed' | 'failed';
  progress: number;
  message?: string;
  step?: string;
}

// Packing list interfaces
export interface PackingCategory {
  name: string;
  items: string[];
  completed: number;
  total: number;
}

export interface PackingListProps {
  tripData: TripData;
  onUpdate: (updates: Partial<TripData>) => void;
}

// Transport planner interfaces
export interface TransportInfo {
  mode: 'drive' | 'flights' | 'train' | 'bus' | 'ferry';
  distance?: string;
  duration?: string;
  cost?: number;
  currency?: string;
}

export interface TransportPlannerProps {
  destination: Destination;
  tripData: TripData;
  onUpdate: (destinationId: string, transport: TransportInfo) => void;
}


// Layout component interfaces
export interface NavigationSidebarProps {
  activeView: string;
  onViewChange: (view: string) => void;
}

export interface TopNavigationProps {
  tripData: TripData;
  onShare: () => void;
  onExportPDF: () => void;
  onBack: () => void;
}

export interface ResizablePanelProps {
  leftPanelWidth: number;
  onWidthChange: (width: number) => void;
  leftContent: React.ReactNode;
  rightContent: React.ReactNode;
}

// Constants
export const CURRENCIES = ['EUR', 'USD', 'GBP', 'CAD', 'AUD', 'NZD', 'HKD', 'CHF', 'JPY', 'CNY', 'SGD', 'ZAR'];

// Re-export shared components for convenience
export { ErrorBoundary, withErrorBoundary } from './ErrorBoundary';
export { LoadingSpinner, SkeletonCard, SkeletonTable } from './LoadingSpinner';

// View types
export type TravelPlannerView = 'view' | 'plan' | 'budget' | 'packing' | 'collection' | 'docs';
export type PlanTab = 'destinations' | 'day-by-day';
