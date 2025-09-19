import React from 'react';
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
import { useAppStore } from './state/hooks';
import { useItinerary } from './state/query/hooks';
import { Routes, Route, useNavigate, useParams, Outlet, Navigate } from 'react-router-dom';
// Data transformation will be handled by the backend

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

function RoutedApp() {
  const {
    isAuthenticated,
    currentTrip,
    trips,
    setScreen,
    setCurrentTrip,
    addTrip,
    updateCurrentTrip,
    setAuthenticated
  } = useAppStore();
  const navigate = useNavigate();
  const { currentTrip: currentTripForHydrate } = useAppStore();
  const { refetch } = useItinerary(currentTripForHydrate?.id || '');

  React.useEffect(() => {
    (async () => {
      if (currentTripForHydrate?.id) {
        try {
          const r = await refetch();
          if (r.data) {
            // Avoid overwrite if already present
            if (!currentTrip?.itinerary) {
              const responseData = r.data as TripData;
              
              // Use the response data directly
              setCurrentTrip(responseData);
            }
          }
        } catch {}
      }
    })();
  }, []);

  const navigateToScreen = (path: string, tripData?: TripData) => {
    if (tripData) setCurrentTrip(tripData);
    navigate(path);
  };

  const authenticate = () => setAuthenticated(true);

  return (
    <Routes>
      <Route path="/" element={
        <LandingPage
          isAuthenticated={isAuthenticated}
          onAuthenticate={authenticate}
          onStartTrip={() => navigateToScreen('/wizard')}
          onViewTrips={() => navigateToScreen('/dashboard')}
          trips={trips}
        />
      } />
      <Route path="/wizard" element={
        <SimplifiedTripWizard
          onComplete={(tripData) => {
            addTrip(tripData);
            setCurrentTrip(tripData);
            navigateToScreen('/generating');
          }}
          onBack={() => navigateToScreen('/')}
        />
      } />
      {/* Guarded routes requiring a current trip */}
      <Route element={<RequireTrip tripExists={!!currentTrip} /> }>
        <Route path="/generating" element={
          <AgentProgressModal
            tripData={currentTrip as TripData}
            onComplete={() => navigateToScreen('/overview')}
          />
        } />
        <Route path="/overview" element={
          <ItineraryOverview
            tripData={currentTrip as TripData}
            onEdit={() => navigateToScreen('/edit')}
            onWorkflowEdit={() => navigateToScreen('/workflow')}
            onProceedToCost={() => navigateToScreen('/cost')}
            onBack={() => navigateToScreen('/wizard')}
            onOpenPlanner={() => navigateToScreen('/planner')}
          />
        } />
        <Route path="/planner" element={
          <StipplPlanner
            tripData={currentTrip as TripData}
            onSave={(updatedTrip) => {
              updateCurrentTrip(updatedTrip);
              navigateToScreen('/overview');
            }}
            onBack={() => navigateToScreen('/overview')}
            onShare={() => navigateToScreen('/share')}
            onExportPDF={() => {
              // Handle PDF export
              alert('PDF export functionality would be implemented here');
            }}
          />
        } />
        <Route path="/edit" element={
          <EditMode
            tripData={currentTrip as TripData}
            onSave={(updatedTrip) => {
              updateCurrentTrip(updatedTrip);
              navigateToScreen('/overview');
            }}
            onCancel={() => navigateToScreen('/overview')}
          />
        } />
        <Route path="/workflow" element={
          <WorkflowBuilder
            tripData={currentTrip as TripData}
            onSave={(updatedTrip) => {
              updateCurrentTrip(updatedTrip);
              navigateToScreen('/overview');
            }}
            onCancel={() => navigateToScreen('/overview')}
          />
        } />
        <Route path="/cost" element={
          <CostAndCart
            tripData={currentTrip as TripData}
            onCheckout={() => navigateToScreen('/checkout')}
            onBack={() => navigateToScreen('/overview')}
          />
        } />
        <Route path="/checkout" element={
          <Checkout
            tripData={currentTrip as TripData}
            onSuccess={(bookingData) => {
              updateCurrentTrip({ bookingData });
              navigateToScreen('/confirmation');
            }}
            onBack={() => navigateToScreen('/cost')}
          />
        } />
        <Route path="/confirmation" element={
          <BookingConfirmation
            tripData={currentTrip as TripData}
            onShare={() => navigateToScreen('/share')}
            onDashboard={() => navigateToScreen('/dashboard')}
          />
        } />
        <Route path="/share" element={
          <ShareView
            tripData={currentTrip as TripData}
            onBack={() => navigateToScreen('/confirmation')}
          />
        } />
      </Route>
      <Route path="/dashboard" element={
        <TripDashboard
          trips={trips}
          onCreateTrip={() => navigateToScreen('/wizard')}
          onViewTrip={(trip) => {
            setCurrentTrip(trip);
            navigateToScreen('/overview');
          }}
          onBack={() => navigateToScreen('/')}
        />
      } />
      <Route path="/trip/:id" element={<TripRouteLoader />} />
      <Route path="/itinerary/:id" element={<ItineraryRouteLoader />} />
      <Route path="*" element={<div>Not found</div>} />
    </Routes>
  );
}

function TripRouteLoader() {
  const { id } = useParams();
  const { trips, setCurrentTrip } = useAppStore();
  const navigate = useNavigate();
  React.useEffect(() => {
    const trip = trips.find(t => t.id === id);
    if (trip) {
      setCurrentTrip(trip);
      navigate('/overview', { replace: true });
    } else {
      navigate('/', { replace: true });
    }
  }, [id]);
  return null;
}

function ItineraryRouteLoader() {
  const { id } = useParams();
  const { setCurrentTrip } = useAppStore();
  const navigate = useNavigate();
  const { refetch } = useItinerary(id || '');
  React.useEffect(() => {
    (async () => {
      if (!id) {
        navigate('/', { replace: true });
        return;
      }
      try {
        const r = await refetch();
        if (r.data) {
          // Use the response data directly
          const data: TripData = r.data;
          setCurrentTrip(data);
          navigate('/overview', { replace: true });
        } else {
          navigate('/', { replace: true });
        }
      } catch {
        navigate('/', { replace: true });
      }
    })();
  }, [id]);
  return null;
}

function RequireTrip({ tripExists }: { tripExists: boolean }) {
  if (!tripExists) {
    return <Navigate to="/" replace />;
  }
  return <Outlet />;
}

export default function App() {
  return (
    <div className="size-full min-h-screen bg-background">
      <RoutedApp />
    </div>
  );
}