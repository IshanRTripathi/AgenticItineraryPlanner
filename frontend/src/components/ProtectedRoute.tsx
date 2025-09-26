/**
 * Protected Route Component
 * Guards routes based on authentication status
 */

import React, { ReactNode } from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { LoadingSpinner } from './travel-planner/shared/LoadingSpinner';

interface ProtectedRouteProps {
  children: ReactNode;
  requireAuth?: boolean; // If true, requires authentication; if false, redirects if authenticated
  redirectTo?: string;
}

export const ProtectedRoute: React.FC<ProtectedRouteProps> = ({
  children,
  requireAuth = true,
  redirectTo
}) => {
  const { user, loading } = useAuth();
  const location = useLocation();

  // Show loading spinner while checking authentication
  if (loading) {
    return <LoadingSpinner message="Checking authentication..." fullScreen />;
  }

  // If authentication is required but user is not authenticated
  if (requireAuth && !user) {
    // Redirect to login page, preserving the attempted location
    const loginPath = redirectTo || '/login';
    return <Navigate to={loginPath} state={{ from: location }} replace />;
  }

  // If authentication is not required but user is authenticated (e.g., login page)
  if (!requireAuth && user) {
    // Redirect to home or dashboard
    const homePath = redirectTo || '/dashboard';
    return <Navigate to={homePath} replace />;
  }

  // User is authenticated and auth is required, or user is not authenticated and auth is not required
  return <>{children}</>;
};

/**
 * Higher-order component for protecting routes
 */
export const withAuth = <P extends object>(
  Component: React.ComponentType<P>,
  requireAuth: boolean = true,
  redirectTo?: string
) => {
  return (props: P) => (
    <ProtectedRoute requireAuth={requireAuth} redirectTo={redirectTo}>
      <Component {...props} />
    </ProtectedRoute>
  );
};

export default ProtectedRoute;
