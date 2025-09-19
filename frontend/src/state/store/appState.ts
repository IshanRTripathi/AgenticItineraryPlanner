// Minimal app state shape mirrored from App.tsx to allow gradual migration
export type AppScreen = 
  | 'landing'
  | 'wizard'
  | 'generating'
  | 'overview'
  | 'planner'
  | 'edit'
  | 'workflow'
  | 'cost'
  | 'checkout'
  | 'confirmation'
  | 'share'
  | 'dashboard';

export interface AppStateShape<TTrip> {
  currentScreen: AppScreen;
  isAuthenticated: boolean;
  currentTrip: TTrip | null;
  trips: TTrip[];
}

export const createInitialAppState = <TTrip>(initial?: Partial<AppStateShape<TTrip>>): AppStateShape<TTrip> => ({
  currentScreen: 'landing',
  isAuthenticated: false,
  currentTrip: null,
  trips: [],
  ...initial
});



