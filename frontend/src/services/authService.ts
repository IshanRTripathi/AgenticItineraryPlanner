/**
 * Authentication Service
 * Handles Firebase auth, token refresh, and session management
 */

import { auth } from '@/config/firebase';
import { 
  signInWithEmailAndPassword, 
  createUserWithEmailAndPassword,
  signInWithPopup,
  GoogleAuthProvider,
  signOut as firebaseSignOut,
  onAuthStateChanged as firebaseOnAuthStateChanged,
  User
} from 'firebase/auth';
import { apiClient } from './apiClient';

export interface AuthUser {
  uid: string;
  email: string | null;
  displayName: string | null;
  photoURL: string | null;
  emailVerified?: boolean;
  metadata?: {
    creationTime?: string;
    lastSignInTime?: string;
  };
}

class AuthService {
  private tokenRefreshInterval: NodeJS.Timeout | null = null;
  private readonly TOKEN_REFRESH_INTERVAL = 50 * 60 * 1000; // 50 minutes
  private googleProvider: GoogleAuthProvider;

  constructor() {
    this.googleProvider = new GoogleAuthProvider();
  }

  /**
   * Initialize auth service and set up token refresh
   */
  initialize() {
    firebaseOnAuthStateChanged(auth, async (user) => {
      if (user) {
        await this.setupTokenRefresh(user);
      } else {
        this.clearTokenRefresh();
      }
    });
  }

  /**
   * Listen to auth state changes
   */
  onAuthStateChanged(callback: (user: AuthUser | null) => void | Promise<void>) {
    return firebaseOnAuthStateChanged(auth, async (firebaseUser) => {
      if (firebaseUser) {
        const authUser: AuthUser = {
          uid: firebaseUser.uid,
          email: firebaseUser.email,
          displayName: firebaseUser.displayName,
          photoURL: firebaseUser.photoURL,
          emailVerified: firebaseUser.emailVerified,
          metadata: {
            creationTime: firebaseUser.metadata.creationTime,
            lastSignInTime: firebaseUser.metadata.lastSignInTime,
          },
        };
        await callback(authUser);
      } else {
        await callback(null);
      }
    });
  }

  /**
   * Set up automatic token refresh
   */
  private async setupTokenRefresh(user: User) {
    // Clear any existing interval
    this.clearTokenRefresh();

    // Get initial token
    const token = await user.getIdToken();
    this.updateApiClientToken(token);

    // Set up refresh interval
    this.tokenRefreshInterval = setInterval(async () => {
      try {
        const freshToken = await user.getIdToken(true); // Force refresh
        this.updateApiClientToken(freshToken);
        console.log('[Auth] Token refreshed successfully');
      } catch (error) {
        console.error('[Auth] Token refresh failed:', error);
        // If refresh fails, redirect to login
        this.handleAuthError();
      }
    }, this.TOKEN_REFRESH_INTERVAL);
  }

  /**
   * Clear token refresh interval
   */
  private clearTokenRefresh() {
    if (this.tokenRefreshInterval) {
      clearInterval(this.tokenRefreshInterval);
      this.tokenRefreshInterval = null;
    }
  }

  /**
   * Update API client with new token
   */
  private updateApiClientToken(token: string) {
    // Update axios interceptor with new token
    apiClient.interceptors.request.use(
      async (config: any) => {
        config.headers.Authorization = `Bearer ${token}`;
        return config;
      },
      (error: any) => Promise.reject(error)
    );
  }

  /**
   * Handle authentication errors (401, token expiry)
   */
  private handleAuthError() {
    this.clearTokenRefresh();
    // Redirect to login
    window.location.href = '/login?expired=true';
  }

  /**
   * Sign in with Google
   */
  async signInWithGoogle() {
    try {
      const result = await signInWithPopup(auth, this.googleProvider);
      const token = await result.user.getIdToken();
      this.updateApiClientToken(token);
      await this.setupTokenRefresh(result.user);
      return { success: true, user: result.user };
    } catch (error: any) {
      console.error('[Auth] Google sign in failed:', error);
      throw error;
    }
  }

  /**
   * Sign in with email and password
   */
  async signIn(email: string, password: string) {
    try {
      const userCredential = await signInWithEmailAndPassword(auth, email, password);
      const token = await userCredential.user.getIdToken();
      this.updateApiClientToken(token);
      await this.setupTokenRefresh(userCredential.user);
      return { success: true, user: userCredential.user };
    } catch (error: any) {
      console.error('[Auth] Sign in failed:', error);
      return { success: false, error: error.message };
    }
  }

  /**
   * Sign up with email and password
   */
  async signUp(email: string, password: string) {
    try {
      const userCredential = await createUserWithEmailAndPassword(auth, email, password);
      const token = await userCredential.user.getIdToken();
      this.updateApiClientToken(token);
      await this.setupTokenRefresh(userCredential.user);
      return { success: true, user: userCredential.user };
    } catch (error: any) {
      console.error('[Auth] Sign up failed:', error);
      return { success: false, error: error.message };
    }
  }

  /**
   * Sign out
   */
  async signOut() {
    try {
      this.clearTokenRefresh();
      await firebaseSignOut(auth);
      this.updateApiClientToken('');
      return { success: true };
    } catch (error: any) {
      console.error('[Auth] Sign out failed:', error);
      return { success: false, error: error.message };
    }
  }

  /**
   * Get current user
   */
  getCurrentUser() {
    return auth.currentUser;
  }

  /**
   * Get current auth token
   */
  async getCurrentToken() {
    const user = auth.currentUser;
    if (!user) return null;
    return await user.getIdToken();
  }

  /**
   * Get ID token (alias for getCurrentToken)
   */
  async getIdToken() {
    return await this.getCurrentToken();
  }

  /**
   * Force refresh ID token
   */
  async getIdTokenForceRefresh() {
    const user = auth.currentUser;
    if (!user) return null;
    return await user.getIdToken(true);
  }

  /**
   * Check if user is authenticated
   */
  isAuthenticated() {
    return !!auth.currentUser;
  }
}

export const authService = new AuthService();

// Initialize on module load
authService.initialize();
