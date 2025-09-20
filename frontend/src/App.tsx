import React from 'react';
import { LandingPage } from './components/LandingPage';
import { SimplifiedTripWizard } from './components/trip-wizard/SimplifiedTripWizard';
import { AgentProgressModal } from './components/agents/AgentProgressModal';
import { TravelPlanner } from './components/TravelPlanner';
import { CostAndCart } from './components/booking/CostAndCart';
import { Checkout } from './components/booking/Checkout';
import { BookingConfirmation } from './components/booking/BookingConfirmation';
import { ShareView } from './components/trip-management/ShareView';
import { TripDashboard } from './components/trip-management/TripDashboard';
import { TripData } from './types/TripData';
import { useAppStore } from './state/hooks';
import { useItinerary } from './state/query/hooks';
import { Routes, Route, useNavigate, useParams, Outlet, Navigate } from 'react-router-dom';
// Data transformation will be handled by the backend

export type AppScreen = 
  | 'landing'
  | 'wizard'
  | 'generating'
  | 'planner'
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
            onComplete={() => navigateToScreen('/planner')}
          />
        } />
        <Route path="/planner" element={
          <TravelPlanner
            tripData={currentTrip as TripData}
            onSave={(updatedTrip) => {
              updateCurrentTrip(updatedTrip);
              navigateToScreen('/dashboard');
            }}
            onBack={() => navigateToScreen('/dashboard')}
            onShare={() => navigateToScreen('/share')}
            onExportPDF={() => {
              // Handle PDF export
              alert('PDF export functionality would be implemented here');
            }}
          />
        } />
        <Route path="/cost" element={
          <CostAndCart
            tripData={currentTrip as TripData}
            onCheckout={() => navigateToScreen('/checkout')}
            onBack={() => navigateToScreen('/planner')}
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
            navigateToScreen('/planner');
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
      navigate('/planner', { replace: true });
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
          navigate('/planner', { replace: true });
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