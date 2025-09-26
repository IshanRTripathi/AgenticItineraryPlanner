/**
 * Authentication Service
 * Handles user authentication using Firebase Auth
 */

import {
  signInWithPopup,
  signOut,
  onAuthStateChanged,
  User,
  UserCredential,
  GoogleAuthProvider
} from 'firebase/auth';
import { auth, googleProvider } from '../config/firebase';

export interface AuthUser {
  uid: string;
  email: string | null;
  displayName: string | null;
  photoURL: string | null;
  emailVerified: boolean;
}

// Removed email/password interfaces - using Google Sign-in only

class AuthService {
  /**
   * Sign in with Google
   */
  async signInWithGoogle(): Promise<UserCredential> {
    try {
      const result = await signInWithPopup(auth, googleProvider);
      console.log('[Auth] Google sign-in successful:', result.user.email);
      return result;
    } catch (error) {
      console.error('[Auth] Google sign-in failed:', error);
      throw error;
    }
  }

  /**
   * Sign out current user
   */
  async signOut(): Promise<void> {
    try {
      await signOut(auth);
      console.log('[Auth] Sign-out successful');
    } catch (error) {
      console.error('[Auth] Sign-out failed:', error);
      throw error;
    }
  }

  // Removed password reset - using Google Sign-in only

  /**
   * Get current user
   */
  getCurrentUser(): User | null {
    return auth.currentUser;
  }

  /**
   * Convert Firebase User to AuthUser interface
   */
  convertToAuthUser(user: User): AuthUser {
    return {
      uid: user.uid,
      email: user.email,
      displayName: user.displayName,
      photoURL: user.photoURL,
      emailVerified: user.emailVerified
    };
  }

  /**
   * Listen to authentication state changes
   */
  onAuthStateChanged(callback: (user: AuthUser | null) => void): () => void {
    return onAuthStateChanged(auth, (user) => {
      if (user) {
        callback(this.convertToAuthUser(user));
      } else {
        callback(null);
      }
    });
  }

  /**
   * Check if user is authenticated
   */
  isAuthenticated(): boolean {
    return auth.currentUser !== null;
  }

  /**
   * Get user ID token for backend authentication
   */
  async getIdToken(): Promise<string | null> {
    const user = auth.currentUser;
    if (user) {
      try {
        return await user.getIdToken();
      } catch (error) {
        console.error('[Auth] Failed to get ID token:', error);
        return null;
      }
    }
    return null;
  }

  /**
   * Get user ID token with force refresh
   */
  async getIdTokenForceRefresh(): Promise<string | null> {
    const user = auth.currentUser;
    if (user) {
      try {
        return await user.getIdToken(true);
      } catch (error) {
        console.error('[Auth] Failed to get refreshed ID token:', error);
        return null;
      }
    }
    return null;
  }
}

// Export singleton instance
export const authService = new AuthService();
export default authService;
