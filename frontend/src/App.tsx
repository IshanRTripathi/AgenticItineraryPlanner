import React, { useState } from 'react';
import { LandingPage } from './components/LandingPage';
import { SimplifiedTripWizard } from './components/SimplifiedTripWizard';
import { AgentProgressModal } from './components/AgentProgressModal';
import { ItineraryOverview } from './components/ItineraryOverview';
import { StipplPlanner } from './components/StipplPlanner';
import { EditMode } from './components/EditMode';
import { WorkflowBuilder } from './components/WorkflowBuilder';
import { CostAndCart } from './components/CostAndCart';
import { Checkout } from './components/Checkout';
import { BookingConfirmation } from './components/BookingConfirmation';
import { ShareView } from './components/ShareView';
import { TripDashboard } from './components/TripDashboard';
import { TripData } from './types/TripData';

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

export interface AppState {
  currentScreen: AppScreen;
  isAuthenticated: boolean;
  currentTrip: TripData | null;
  trips: TripData[];
}

export default function App() {
  const [appState, setAppState] = useState<AppState>({
    currentScreen: 'landing',
    isAuthenticated: false,
    currentTrip: null,
    trips: []
  });

  const navigateToScreen = (screen: AppScreen, tripData?: TripData) => {
    setAppState(prev => ({
      ...prev,
      currentScreen: screen,
      ...(tripData && { currentTrip: tripData })
    }));
  };

  const updateCurrentTrip = (updates: Partial<TripData>) => {
    setAppState(prev => ({
      ...prev,
      currentTrip: prev.currentTrip ? { ...prev.currentTrip, ...updates } : null
    }));
  };

  const authenticate = () => {
    setAppState(prev => ({ ...prev, isAuthenticated: true }));
  };

  const renderScreen = () => {
    switch (appState.currentScreen) {
      case 'landing':
        return (
          <LandingPage
            isAuthenticated={appState.isAuthenticated}
            onAuthenticate={authenticate}
            onStartTrip={() => navigateToScreen('wizard')}
            onViewTrips={() => navigateToScreen('dashboard')}
            trips={appState.trips}
          />
        );
      case 'wizard':
        return (
          <SimplifiedTripWizard
            onComplete={(tripData) => {
              const newTrip = { ...tripData, id: Date.now().toString() };
              setAppState(prev => ({
                ...prev,
                trips: [...prev.trips, newTrip],
                currentTrip: newTrip
              }));
              navigateToScreen('generating');
            }}
            onBack={() => navigateToScreen('landing')}
          />
        );
      case 'generating':
        return (
          <AgentProgressModal
            tripData={appState.currentTrip!}
            onComplete={() => navigateToScreen('planner')}
          />
        );
      case 'overview':
        return (
          <ItineraryOverview
            tripData={appState.currentTrip!}
            onEdit={() => navigateToScreen('edit')}
            onWorkflowEdit={() => navigateToScreen('workflow')}
            onProceedToCost={() => navigateToScreen('cost')}
            onBack={() => navigateToScreen('wizard')}
            onOpenPlanner={() => navigateToScreen('planner')}
          />
        );
      case 'planner':
        return (
          <StipplPlanner
            tripData={appState.currentTrip!}
            onSave={(updatedTrip) => {
              updateCurrentTrip(updatedTrip);
              navigateToScreen('overview');
            }}
            onBack={() => navigateToScreen('overview')}
            onShare={() => navigateToScreen('share')}
            onExportPDF={() => {
              // Handle PDF export
              alert('PDF export functionality would be implemented here');
            }}
          />
        );
      case 'edit':
        return (
          <EditMode
            tripData={appState.currentTrip!}
            onSave={(updatedTrip) => {
              updateCurrentTrip(updatedTrip);
              navigateToScreen('overview');
            }}
            onCancel={() => navigateToScreen('overview')}
          />
        );
      case 'workflow':
        return (
          <WorkflowBuilder
            tripData={appState.currentTrip!}
            onSave={(updatedTrip) => {
              updateCurrentTrip(updatedTrip);
              navigateToScreen('overview');
            }}
            onCancel={() => navigateToScreen('overview')}
          />
        );
      case 'cost':
        return (
          <CostAndCart
            tripData={appState.currentTrip!}
            onCheckout={() => navigateToScreen('checkout')}
            onBack={() => navigateToScreen('overview')}
          />
        );
      case 'checkout':
        return (
          <Checkout
            tripData={appState.currentTrip!}
            onSuccess={(bookingData) => {
              updateCurrentTrip({ bookingData });
              navigateToScreen('confirmation');
            }}
            onBack={() => navigateToScreen('cost')}
          />
        );
      case 'confirmation':
        return (
          <BookingConfirmation
            tripData={appState.currentTrip!}
            onShare={() => navigateToScreen('share')}
            onDashboard={() => navigateToScreen('dashboard')}
          />
        );
      case 'share':
        return (
          <ShareView
            tripData={appState.currentTrip!}
            onBack={() => navigateToScreen('confirmation')}
          />
        );
      case 'dashboard':
        return (
          <TripDashboard
            trips={appState.trips}
            onCreateTrip={() => navigateToScreen('wizard')}
            onViewTrip={(trip) => {
              setAppState(prev => ({ ...prev, currentTrip: trip }));
              navigateToScreen('overview');
            }}
            onBack={() => navigateToScreen('landing')}
          />
        );
      default:
        return <div>Screen not found</div>;
    }
  };

  return (
    <div className="size-full min-h-screen bg-background">
      {renderScreen()}
    </div>
  );
}