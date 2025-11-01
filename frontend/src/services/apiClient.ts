/**
 * Axios-based API Client
 * HTTP client with auth token management and 401 handling
 */

import axios, { AxiosInstance } from 'axios';
import { auth } from '@/config/firebase';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/v1';

// Extend AxiosInstance with custom methods
interface ExtendedAxiosInstance extends AxiosInstance {
  setAuthToken: (token: string | null) => void;
  clearAuthToken: () => void;
}

// Create axios instance
const instance = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 30000, // 30 seconds
}) as ExtendedAxiosInstance;

// Add helper methods for compatibility with AuthContext
instance.setAuthToken = (token: string | null) => {
  // Token is already handled by interceptors, but we keep this for compatibility
  console.log('[API Client] setAuthToken called (handled by interceptors)');
};

instance.clearAuthToken = () => {
  // Token is already handled by interceptors, but we keep this for compatibility
  console.log('[API Client] clearAuthToken called (handled by interceptors)');
};

export const apiClient = instance;

// Track if we're currently refreshing the token
let isRefreshing = false;
let failedQueue: any[] = [];

const processQueue = (error: any, token: string | null = null) => {
  failedQueue.forEach((prom) => {
    if (error) {
      prom.reject(error);
    } else {
      prom.resolve(token);
    }
  });
  failedQueue = [];
};

// Request interceptor to add auth token
apiClient.interceptors.request.use(
  async (config: any) => {
    try {
      const user = auth.currentUser;
      if (user) {
        const token = await user.getIdToken();
        config.headers.Authorization = `Bearer ${token}`;
      }
    } catch (error) {
      console.error('[API Client] Error getting auth token:', error);
    }
    return config;
  },
  (error: any) => {
    return Promise.reject(error);
  }
);

// Response interceptor for error handling and token refresh
apiClient.interceptors.response.use(
  (response: any) => response,
  async (error: any) => {
    const originalRequest = error.config;

    // Handle 401 errors with token refresh
    if (error.response?.status === 401 && !originalRequest._retry) {
      if (isRefreshing) {
        // Queue the request while token is being refreshed
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject });
        })
          .then((token) => {
            originalRequest.headers.Authorization = `Bearer ${token}`;
            return apiClient(originalRequest);
          })
          .catch((err) => {
            return Promise.reject(err);
          });
      }

      originalRequest._retry = true;
      isRefreshing = true;

      try {
        const user = auth.currentUser;
        if (user) {
          // Force token refresh
          const newToken = await user.getIdToken(true);
          console.log('[API Client] Token refreshed successfully');
          
          // Update the failed request with new token
          originalRequest.headers.Authorization = `Bearer ${newToken}`;
          
          // Process queued requests
          processQueue(null, newToken);
          
          isRefreshing = false;
          
          // Retry the original request
          return apiClient(originalRequest);
        } else {
          throw new Error('No authenticated user');
        }
      } catch (refreshError) {
        console.error('[API Client] Token refresh failed:', refreshError);
        processQueue(refreshError, null);
        isRefreshing = false;
        
        // Redirect to login with expired flag
        const currentPath = window.location.pathname;
        window.location.href = `/login?expired=true&redirect=${encodeURIComponent(currentPath)}`;
        
        return Promise.reject(refreshError);
      }
    }

    // Handle other errors
    if (error.response?.status === 403) {
      console.error('[API Client] Access forbidden');
    } else if (error.response?.status >= 500) {
      console.error('[API Client] Server error:', error.response.status);
    }

    return Promise.reject(error);
  }
);
