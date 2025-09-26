/**
 * Login Page Component
 * Simple login page with Google Sign-in
 */

import React from 'react';
import { GoogleSignIn } from './GoogleSignIn';
import { useAuth } from '../contexts/AuthContext';
import { Navigate } from 'react-router-dom';

export const LoginPage: React.FC = () => {
  const { isAuthenticated, loading } = useAuth();

  // Redirect to dashboard if already authenticated
  if (isAuthenticated) {
    return <Navigate to="/dashboard" replace />;
  }

  // Show loading state
  if (loading) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-teal-600 mx-auto mb-4"></div>
          <p className="text-gray-600">Loading...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 flex items-center justify-center py-12 px-4 sm:px-6 lg:px-8">
      <div className="max-w-md w-full space-y-8">
        <GoogleSignIn />
      </div>
    </div>
  );
};

export default LoginPage;
