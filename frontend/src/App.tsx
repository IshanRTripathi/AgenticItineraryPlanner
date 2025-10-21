import React from 'react';
import { LandingPage } from './components/LandingPage';
import { SimplifiedTripWizard } from './components/trip-wizard/SimplifiedTripWizard';
import { SimplifiedAgentProgress } from './components/agents/SimplifiedAgentProgress';
import { TravelPlanner } from './components/TravelPlanner';
import { CostAndCart } from './components/booking/CostAndCart';
import { Checkout } from './components/booking/Checkout';
import { BookingConfirmation } from './components/booking/BookingConfirmation';
import { ShareView } from './components/trip-management/ShareView';
import { TripDashboard } from './components/trip-management/TripDashboard';
import { ItineraryWithChat } from './components/ItineraryWithChat';
import { TripData } from './types/TripData';
import { useAppStore } from './state/hooks';
import { useItinerary } from './state/query/hooks';
import { apiClient } from './services/apiClient';
import { Routes, Route, useNavigate, useParams, Outlet, Navigate, useLocation } from 'react-router-dom';
import { AuthProvider } from './contexts/AuthContext';
import { MapProvider } from './contexts/MapContext';
import { ProtectedRoute } from './components/ProtectedRoute';
import { GoogleSignIn } from './components/GoogleSignIn';
import { LoginPage } from './components/LoginPage';
import { GlobalErrorBoundary } from './components/shared/GlobalErrorBoundary';
import { KeyboardShortcuts } from './components/shared/KeyboardShortcuts';
import { TripViewLoader } from './components/TripViewLoader';
import { AppProviders } from './contexts/AppProviders';
import './i18n'; // Initialize i18n
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
  const location = useLocation();
  const { currentTrip: currentTripForHydrate } = useAppStore();
  
  // Only fetch itinerary data for routes that need it
  const shouldFetchItinerary = location.pathname.includes('/generating') ||
                              location.pathname.includes('/planner') ||
                              location.pathname.includes('/cost') ||
                              location.pathname.includes('/checkout') ||
                              location.pathname.includes('/confirmation') ||
                              location.pathname.includes('/share');
  
  const { refetch } = useItinerary(shouldFetchItinerary ? (currentTripForHydrate?.id || '') : '');

  React.useEffect(() => {
    (async () => {
      if (currentTripForHydrate?.id && shouldFetchItinerary) {
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
  }, [currentTripForHydrate?.id, shouldFetchItinerary, currentTrip?.itinerary, setCurrentTrip]);

  const navigateToScreen = (path: string, tripData?: TripData) => {
    if (tripData) setCurrentTrip(tripData);
    navigate(path);
  };

  const authenticate = () => setAuthenticated(true);

  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/" element={
        <ProtectedRoute requireAuth={false}>
          <LandingPage
            isAuthenticated={isAuthenticated}
            onAuthenticate={authenticate}
            onStartTrip={() => navigateToScreen('/wizard')}
            onViewTrips={() => navigateToScreen('/dashboard')}
            trips={trips}
          />
        </ProtectedRoute>
      } />
      <Route path="/wizard" element={
        <ProtectedRoute>
          <SimplifiedTripWizard
            onComplete={(tripData) => {
              addTrip(tripData);
              setCurrentTrip(tripData);
              navigateToScreen('/generating');
            }}
            onBack={() => navigateToScreen('/')}
          />
        </ProtectedRoute>
      } />
      {/* Guarded routes requiring a current trip */}
      <Route element={<RequireTrip tripExists={!!currentTrip} /> }>
        <Route path="/generating" element={
          <SimplifiedAgentProgress
            tripData={currentTrip as TripData}
            onComplete={async () => {
              try {
                // Fetch the completed itinerary from ItineraryJsonService using the API client
                const completedItinerary = await apiClient.getItinerary(currentTrip?.id || '');
                updateCurrentTrip(completedItinerary);
              } catch (error) {
                console.error('Failed to fetch completed itinerary:', error);
              }
              navigateToScreen('/planner');
            }}
            onCancel={() => navigateToScreen('/dashboard')}
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
        <ProtectedRoute>
          <TripDashboard
            trips={trips}
            onCreateTrip={() => navigateToScreen('/wizard')}
            onViewTrip={(trip) => {
              // Ensure trip has itinerary structure
              const tripWithItinerary = {
                ...trip,
                itinerary: trip.itinerary || { days: [] }
              } as any;
              setCurrentTrip(tripWithItinerary);
              navigate('/planner');
            }}
            onBack={() => navigateToScreen('/')}
          />
        </ProtectedRoute>
      } />
      <Route path="/trip/:id" element={<TripRouteLoader />} />
      <Route path="/planner" element={<PlannerRouteLoader />} />
      <Route path="/itinerary/:id" element={<ItineraryRouteLoader />} />
      <Route path="/itinerary/:id/chat" element={<ItineraryChatRouteLoader />} />
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
      navigate('/dashboard', { replace: true });
    }
  }, [id]);
  return null;
}

function PlannerRouteLoader() {
  const { currentTrip, setCurrentTrip } = useAppStore();
  const navigate = useNavigate();
  
  // If no current trip is set, redirect to dashboard
  if (!currentTrip) {
    navigate('/dashboard', { replace: true });
    return null;
  }
  
  return (
    <TripViewLoader
      itineraryId={currentTrip.id}
      onSave={(updatedTrip) => {
        // Update the trip in the store
        setCurrentTrip(updatedTrip);
        console.log('Trip saved:', updatedTrip);
      }}
      onBack={() => navigate('/dashboard')}
      onShare={() => navigate('/share')}
      onExportPDF={() => {
        // Handle PDF export
        alert('PDF export functionality would be implemented here');
      }}
    />
  );
}

function ItineraryRouteLoader() {
  const { id } = useParams();
  const { setCurrentTrip } = useAppStore();
  const navigate = useNavigate();
  const { refetch } = useItinerary(id || '');
  React.useEffect(() => {
    (async () => {
      if (!id) {
        navigate('/dashboard', { replace: true });
        return;
      }
      try {
        const r = await refetch();
        if (r.data) {
          // Use the response data directly
          const data: TripData = r.data as TripData;
          setCurrentTrip(data);
          navigate('/planner', { replace: true });
        } else {
          navigate('/dashboard', { replace: true });
        }
      } catch {
        navigate('/dashboard', { replace: true });
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

function ItineraryChatRouteLoader() {
  const { id } = useParams<{ id: string }>();
  return <ItineraryWithChat itineraryId={id || "it_barcelona_comprehensive"} />;
}

export default function App() {
  const errorHandler = (error: Error, errorInfo: React.ErrorInfo) => {
    console.error('Global error:', error, errorInfo);
  };

  return (
    <GlobalErrorBoundary onError={errorHandler}>
      <AuthProvider>
        <AppProviders>
          <MapProvider>
            <KeyboardShortcuts>
              <div className="size-full min-h-screen bg-background">
                <RoutedApp />
              </div>
            </KeyboardShortcuts>
          </MapProvider>
        </AppProviders>
      </AuthProvider>
    </GlobalErrorBoundary>
  );
}