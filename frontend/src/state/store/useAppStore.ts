import { create } from 'zustand';
import { persist, createJSONStorage } from 'zustand/middleware';
import type { TripData } from '../../types/TripData';
import type { AppScreen } from './appState';
import type { AuthSlice, TripSlice, UiSlice, SseSlice } from '../slices/types';
import { apiClient } from '../../services/apiClient';

const ENABLE_SSE_IN_STORE = true; // centralized SSE enabled

type AppStore = AuthSlice & TripSlice & UiSlice & SseSlice;

export const useAppStore = create<AppStore>()(
  persist((set, get) => ({
    // Auth
    isAuthenticated: false,
    authToken: undefined,
    setAuthenticated: (value: boolean) => set({ isAuthenticated: value }),
    setAuthToken: (token?: string) => set({ authToken: token }),
    clearAuth: () => set({ isAuthenticated: false, authToken: undefined }),

    // Trip - NOTE: Server data should come from React Query, not persisted here
    // These are kept for backward compatibility but not persisted to LocalStorage
    currentTrip: null,
    trips: [],
    setCurrentTrip: (trip: TripData | null) => set({ currentTrip: trip }),
    addTrip: (trip: TripData) => set(state => ({ trips: [...state.trips, trip] })),
    updateCurrentTrip: (updates: Partial<TripData>) => set(state => (
      state.currentTrip ? { currentTrip: { ...state.currentTrip, ...updates } } : {}
    )),

    // UI
    currentScreen: 'landing' as AppScreen,
    setScreen: (screen: AppScreen) => set({ currentScreen: screen }),

    // SSE
    agentProgress: {},
    overallProgress: 0,
    connectSse: (tripId: string) => {
      if (!ENABLE_SSE_IN_STORE) return;
      try {
        const es = apiClient.createAgentEventStream(tripId);
        es.onmessage = (event) => {
          // handled by component for now
          console.debug('SSE(message) in store (ignored)', event.data);
        };
        es.addEventListener('agent-event', (event: MessageEvent) => {
          try {
            const data = JSON.parse(event.data);
            const kind = data.kind as string;
            const progress = Number(data.progress ?? 0);
            const status = (data.status ?? 'running') as 'queued' | 'running' | 'succeeded' | 'failed';
            set(state => ({
              agentProgress: {
                ...state.agentProgress,
                [kind]: { status, progress, message: data.message }
              }
            }));
            const values = Object.values(get().agentProgress);
            const avg = values.length ? values.reduce((a, b) => a + (b.progress || 0), 0) / values.length : 0;
            set({ overallProgress: Math.round(avg) });
          } catch (e) {
            // Error parsing SSE event - should be handled by error boundary
          }
        });
        (window as any).__appSse = es;
      } catch (e) {
        // SSE connection error - should be handled by error boundary
      }
    },
    disconnectSse: () => {
      try {
        const es = (window as any).__appSse as EventSource | undefined;
        if (es && es.close) es.close();
        (window as any).__appSse = undefined;
      } catch {}
    },
    clearSse: () => set({ agentProgress: {}, overallProgress: 0 }),
    
    // Debug function to clear store
    clearStore: () => {
      set({ 
        isAuthenticated: false, 
        authToken: undefined, 
        currentTrip: null, 
        trips: [], 
        currentScreen: 'landing',
        agentProgress: {},
        overallProgress: 0
      });
    },
    
    // Debug function to manually save to localStorage
    debugSaveToStorage: () => {
      const state = get();
      const dataToSave = {
        isAuthenticated: state.isAuthenticated,
        authToken: state.authToken,
        currentScreen: state.currentScreen,
        // Note: currentTrip and trips are no longer persisted
      };
      localStorage.setItem('app-store', JSON.stringify(dataToSave));
    }
  }), {
    name: 'app-store',
    storage: createJSONStorage(() => localStorage),
    // Only persist UI state, not server data
    // Server data should come from React Query cache
    partialize: (state) => ({
      isAuthenticated: state.isAuthenticated,
      authToken: state.authToken,
      currentScreen: state.currentScreen,
      // Removed: currentTrip and trips (server data)
    })
  })
);


