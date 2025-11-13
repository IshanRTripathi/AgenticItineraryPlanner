/**
 * Authentication Context
 * Provides global user authentication state across the application
 */

import React, { createContext, useContext, useEffect, useState, ReactNode } from 'react';
import { authService, AuthUser } from '../services/authService';
import { apiClient } from '../services/apiClient';
import { itineraryApi } from '../services/api';
import { logger } from '../utils/logger';

interface AuthContextType {
  user: AuthUser | null;
  loading: boolean;
  isGuest: boolean;
  signInWithGoogle: () => Promise<void>;
  signOut: () => Promise<void>;
  isAuthenticated: boolean;
  getIdToken: () => Promise<string | null>;
  migrateGuestData: () => Promise<{ success: boolean; migratedCount?: number; error?: string }>;
  enableGuestMode: () => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

interface AuthProviderProps {
  children: ReactNode;
}

export const AuthProvider: React.FC<AuthProviderProps> = ({ children }) => {
  const [user, setUser] = useState<AuthUser | null>(null);
  const [loading, setLoading] = useState(true);
  const [isGuest, setIsGuest] = useState<boolean>(false);

  // Check guest mode on mount
  useEffect(() => {
    const guestMode = localStorage.getItem('isGuestMode') === 'true';
    setIsGuest(guestMode && !user);
  }, [user]);

  useEffect(() => {
    // Listen to authentication state changes
    const unsubscribe = authService.onAuthStateChanged(async (user) => {
      setUser(user);
      setLoading(false);
      logger.info('Auth state changed', {
        component: 'AuthContext',
        hasUser: !!user,
        userEmail: user?.email
      });

      // Set or clear auth token in API clients
      if (user) {
        try {
          const token = await authService.getIdToken();
          if (token) {
            (apiClient as any).setAuthToken(token);
            itineraryApi.setAuthToken(token);
            logger.info('Auth token set for API requests', {
              component: 'AuthContext'
            });
          }
        } catch (error) {
          logger.error('Failed to get ID token', {
            component: 'AuthContext'
          }, error as Error);
          (apiClient as any).clearAuthToken();
          itineraryApi.setAuthToken(null);
        }
      } else {
        (apiClient as any).clearAuthToken();
        itineraryApi.setAuthToken(null);
        logger.info('Auth token cleared', {
          component: 'AuthContext'
        });
      }
    });

    // Cleanup subscription on unmount
    return () => unsubscribe();
  }, []);

  // Periodic token refresh to prevent expiration
  useEffect(() => {
    if (!user) return;

    // Refresh token every 50 minutes (tokens expire after 1 hour)
    // This gives us a 10-minute buffer before expiration
    const refreshInterval = setInterval(async () => {
      try {
        logger.info('Proactively refreshing token', {
          component: 'AuthContext'
        });
        const newToken = await authService.getIdTokenForceRefresh();
        if (newToken) {
          (apiClient as any).setAuthToken(newToken);
          itineraryApi.setAuthToken(newToken);
          logger.info('Token refreshed proactively at ' + new Date().toISOString(), {
            component: 'AuthContext'
          });
        } else {
          logger.error('Failed to get refreshed token', {
            component: 'AuthContext'
          });
        }
      } catch (error) {
        logger.error('Failed to refresh token proactively', {
          component: 'AuthContext'
        }, error as Error);
      }
    }, 24 * 60 * 60 * 1000); // 1 day

    // Also do an immediate refresh on mount to ensure we have a fresh token
    const refreshImmediately = async () => {
      try {
        const newToken = await authService.getIdTokenForceRefresh();
        if (newToken) {
          (apiClient as any).setAuthToken(newToken);
          itineraryApi.setAuthToken(newToken);
          logger.info('Token refreshed on mount', {
            component: 'AuthContext'
          });
        }
      } catch (error) {
        logger.error('Failed to refresh token on mount', {
          component: 'AuthContext'
        }, error as Error);
      }
    };
    refreshImmediately();

    return () => clearInterval(refreshInterval);
  }, [user]);

  const signInWithGoogle = async (): Promise<void> => {
    try {
      setLoading(true);
      await authService.signInWithGoogle();
      
      // Migrate guest data after successful sign-in
      try {
        const migrationResult = await authService.migrateGuestData();
        if (migrationResult.success && migrationResult.migratedCount && migrationResult.migratedCount > 0) {
          logger.info('Guest data migrated successfully', {
            component: 'AuthContext',
            migratedCount: migrationResult.migratedCount
          });
        }
      } catch (migrationError) {
        // Don't fail sign-in if migration fails
        logger.error('Guest data migration failed', {
          component: 'AuthContext'
        }, migrationError as Error);
      }
      
      // User state will be updated via onAuthStateChanged
    } catch (error) {
      setLoading(false);
      throw error;
    }
  };

  const migrateGuestData = async () => {
    return await authService.migrateGuestData();
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

  const enableGuestMode = () => {
    authService.enableGuestMode();
    setIsGuest(true);
  };

  const value: AuthContextType = {
    user,
    loading,
    isGuest,
    signInWithGoogle,
    signOut,
    isAuthenticated,
    getIdToken,
    migrateGuestData,
    enableGuestMode
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
