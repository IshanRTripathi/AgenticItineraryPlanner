/**
 * Authentication Context
 * Provides global user authentication state across the application
 */

import React, { createContext, useContext, useEffect, useState, ReactNode } from 'react';
import { authService, AuthUser } from '../services/authService';
import { apiClient } from '../services/apiClient';
import { itineraryApi } from '../services/api';
import { useQueryClient } from '@tanstack/react-query';
import { queryKeys } from '../state/query/hooks';

interface AuthContextType {
  user: AuthUser | null;
  loading: boolean;
  signInWithGoogle: () => Promise<void>;
  signOut: () => Promise<void>;
  isAuthenticated: boolean;
  getIdToken: () => Promise<string | null>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

interface AuthProviderProps {
  children: ReactNode;
}

export const AuthProvider: React.FC<AuthProviderProps> = ({ children }) => {
  const [user, setUser] = useState<AuthUser | null>(null);
  const [loading, setLoading] = useState(true);
  const queryClient = useQueryClient();

  useEffect(() => {
    // Listen to authentication state changes
    const unsubscribe = authService.onAuthStateChanged(async (user) => {
      setUser(user);
      setLoading(false);
      console.log('[AuthContext] Auth state changed:', user ? `User: ${user.email}` : 'No user');
      
      // Set or clear auth token in API clients
      if (user) {
        try {
          const token = await authService.getIdToken();
          if (token) {
            apiClient.setAuthToken(token);
            itineraryApi.setAuthToken(token);
            console.log('[AuthContext] Auth token set for API requests');
            
            // Queries will be enabled automatically when components re-render with user state
          }
        } catch (error) {
          console.error('[AuthContext] Failed to get ID token:', error);
          apiClient.clearAuthToken();
          itineraryApi.setAuthToken(null);
        }
      } else {
        apiClient.clearAuthToken();
        itineraryApi.setAuthToken(null);
        console.log('[AuthContext] Auth token cleared');
        
        // Clear authenticated data
        queryClient.removeQueries({ queryKey: queryKeys.itineraries });
      }
    });

    // Cleanup subscription on unmount
    return () => unsubscribe();
  }, []);

  // Periodic token refresh to prevent expiration
  useEffect(() => {
    if (!user) return;

    // Refresh token every 45 minutes (tokens expire after 1 hour)
    const refreshInterval = setInterval(async () => {
      try {
        const newToken = await authService.getIdTokenForceRefresh();
        if (newToken) {
          apiClient.setAuthToken(newToken);
          itineraryApi.setAuthToken(newToken);
          console.log('[AuthContext] Token refreshed proactively');
        }
      } catch (error) {
        console.error('[AuthContext] Failed to refresh token proactively:', error);
      }
    }, 45 * 60 * 1000); // 45 minutes

    return () => clearInterval(refreshInterval);
  }, [user]);

  const signInWithGoogle = async (): Promise<void> => {
    try {
      setLoading(true);
      await authService.signInWithGoogle();
      // User state will be updated via onAuthStateChanged
    } catch (error) {
      setLoading(false);
      throw error;
    }
  };

  const signOut = async (): Promise<void> => {
    try {
      setLoading(true);
      await authService.signOut();
      // User state will be updated via onAuthStateChanged
    } catch (error) {
      setLoading(false);
      throw error;
    }
  };

  // Removed resetPassword - using Google Sign-in only

  const isAuthenticated = user !== null;

  const getIdToken = async (): Promise<string | null> => {
    return await authService.getIdToken();
  };

  const value: AuthContextType = {
    user,
    loading,
    signInWithGoogle,
    signOut,
    isAuthenticated,
    getIdToken
  };

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
};

/**
 * Hook to use authentication context
 */
export const useAuth = (): AuthContextType => {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};

export default AuthContext;
