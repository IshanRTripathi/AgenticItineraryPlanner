import type { TripData } from '../../types/TripData';
import type { AppScreen } from '../store/appState';

export interface AuthSlice {
  isAuthenticated: boolean;
  authToken?: string;
  setAuthenticated: (value: boolean) => void;
  setAuthToken: (token?: string) => void;
  clearAuth: () => void;
}

export interface TripSlice {
  currentTrip: TripData | null;
  trips: TripData[];
  setCurrentTrip: (trip: TripData | null) => void;
  addTrip: (trip: TripData) => void;
  updateCurrentTrip: (updates: Partial<TripData>) => void;
}

export type AgentStatus = 'queued' | 'running' | 'completed' | 'failed';

export interface SseSlice {
  agentProgress: Record<string, { status: AgentStatus; progress: number; message?: string }>;
  overallProgress: number;
  connectSse: (tripId: string) => void;
  disconnectSse: () => void;
  clearSse: () => void;
}

export interface UiSlice {
  currentScreen: AppScreen;
  setScreen: (screen: AppScreen) => void;
}



