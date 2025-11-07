import { lazy, Suspense, useEffect, useState } from 'react';
import { Routes, Route, useLocation } from 'react-router-dom';
import { ErrorBoundary } from './components/common/ErrorBoundary';
import { ProtectedRoute } from './components/auth/ProtectedRoute';
import { BottomNav } from './components/layout/BottomNav';
import { FloatingHamburger } from './components/layout/FloatingHamburger';
import { HomePage } from './pages/HomePage';
import { Loader2 } from 'lucide-react';
import { analytics } from './services/analytics';
import './index.css';

// Lazy load heavy pages for better performance
const TripWizardPage = lazy(() => import('./pages/TripWizardPage'));
const AgentProgressPage = lazy(() => import('./pages/AgentProgressPage'));
const DashboardPage = lazy(() => import('./pages/DashboardPage'));
const TripDetailPage = lazy(() => import('./pages/TripDetailPage'));
const SearchPage = lazy(() => import('./pages/SearchPage'));
const LoginPage = lazy(() => import('./pages/LoginPage'));
const ProfilePage = lazy(() => import('./pages/ProfilePage').then(m => ({ default: m.ProfilePage })));

// Loading fallback component
function PageLoader() {
  return (
    <div className="min-h-screen flex items-center justify-center">
      <div className="text-center">
        <Loader2 className="w-12 h-12 animate-spin text-primary mx-auto mb-4" />
        <p className="text-muted-foreground">Loading...</p>
      </div>
    </div>
  );
}

function App() {
  const location = useLocation();
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  
  // Track page views
  useEffect(() => {
    analytics.page(location.pathname, document.title);
  }, [location.pathname]);
  
  // Close mobile menu on route change
  useEffect(() => {
    setMobileMenuOpen(false);
  }, [location.pathname]);
  
  // Hide bottom nav on login/signup pages
  const hideBottomNav = ['/login', '/signup'].includes(location.pathname);

  return (
    <ErrorBoundary>
      {/* Skip to main content link for accessibility */}
      <a
        href="#main-content"
        className="sr-only focus:not-sr-only focus:absolute focus:top-4 focus:left-4 focus:z-50 focus:px-4 focus:py-2 focus:bg-primary focus:text-primary-foreground focus:rounded-md focus:outline-none focus:ring-2 focus:ring-primary focus:ring-offset-2"
      >
        Skip to main content
      </a>
      
      <Suspense fallback={<PageLoader />}>
        <Routes>
          <Route path="/" element={<HomePage />} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/search" element={<SearchPage />} />
          
          {/* Protected Routes */}
          <Route path="/planner" element={<ProtectedRoute><TripWizardPage /></ProtectedRoute>} />
          <Route path="/planner-progress" element={<ProtectedRoute><AgentProgressPage /></ProtectedRoute>} />
          <Route path="/dashboard" element={<ProtectedRoute><DashboardPage /></ProtectedRoute>} />
          <Route path="/trip/:id" element={<ProtectedRoute><TripDetailPage /></ProtectedRoute>} />
          <Route path="/profile" element={<ProtectedRoute><ProfilePage /></ProtectedRoute>} />
        </Routes>
      </Suspense>
      
      {/* Floating Hamburger Button */}
      {!hideBottomNav && (
        <FloatingHamburger
          isOpen={mobileMenuOpen}
          onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
        />
      )}
      
      {/* Mobile Bottom Navigation - Hidden when menu is open */}
      {!hideBottomNav && <BottomNav hide={mobileMenuOpen} />}
    </ErrorBoundary>
  );
}

export default App;
