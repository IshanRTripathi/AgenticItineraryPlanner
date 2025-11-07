/**
 * Analytics Service
 * Tracks user events to Google Analytics 4 and backend
 */

interface AnalyticsEvent {
  event: string;
  properties?: Record<string, any>;
}

interface UserProperties {
  userId?: string;
  email?: string;
  [key: string]: any;
}

class AnalyticsService {
  private isInitialized = false;
  private userId: string | null = null;

  /**
   * Initialize analytics
   */
  initialize(trackingId?: string) {
    if (this.isInitialized) return;

    // Initialize Google Analytics 4 if tracking ID provided
    if (trackingId && typeof window !== 'undefined') {
      // Load gtag script
      const script = document.createElement('script');
      script.async = true;
      script.src = `https://www.googletagmanager.com/gtag/js?id=${trackingId}`;
      document.head.appendChild(script);

      // Initialize gtag
      window.dataLayer = window.dataLayer || [];
      function gtag(...args: any[]) {
        window.dataLayer.push(arguments);
      }
      gtag('js', new Date());
      gtag('config', trackingId);

      this.isInitialized = true;
      console.log('[Analytics] Initialized with tracking ID:', trackingId);
    }
  }

  /**
   * Track an event
   */
  track(event: string, properties?: Record<string, any>) {
    // Send to Google Analytics
    if (this.isInitialized && typeof window !== 'undefined' && window.gtag) {
      window.gtag('event', event, properties);
    }

    // Send to backend analytics endpoint
    this.sendToBackend({ event, properties });

    console.log('[Analytics] Event tracked:', event, properties);
  }

  /**
   * Track page view
   */
  page(path: string, title?: string, referrer?: string) {
    this.track('page_view', {
      page_path: path,
      page_title: title || document.title,
      page_referrer: referrer || document.referrer,
    });
  }

  /**
   * Identify user
   */
  identify(userId: string, properties?: UserProperties) {
    this.userId = userId;

    if (this.isInitialized && typeof window !== 'undefined' && window.gtag) {
      window.gtag('set', 'user_properties', {
        user_id: userId,
        ...properties,
      });
    }

    console.log('[Analytics] User identified:', userId);
  }

  /**
   * Track booking events
   */
  trackBooking(type: 'initiated' | 'iframe_loaded' | 'confirmed', data: {
    provider: string;
    vertical: string;
    itineraryId?: string;
    amount?: number;
    currency?: string;
  }) {
    const eventName = `booking_${type}`;
    this.track(eventName, {
      provider: data.provider,
      vertical: data.vertical,
      itinerary_id: data.itineraryId,
      amount: data.amount,
      currency: data.currency,
      timestamp: new Date().toISOString(),
    });
  }

  /**
   * Track search events
   */
  trackSearch(data: {
    searchType: string;
    origin?: string;
    destination?: string;
    startDate?: string;
    endDate?: string;
    travelers?: number;
  }) {
    this.track('search_performed', {
      search_type: data.searchType,
      origin: data.origin,
      destination: data.destination,
      start_date: data.startDate,
      end_date: data.endDate,
      travelers: data.travelers,
      timestamp: new Date().toISOString(),
    });
  }

  /**
   * Track AI trip creation
   */
  trackAITrip(data: {
    destination: string;
    duration: number;
    travelers: number;
    budget?: string;
    executionId?: string;
  }) {
    this.track('ai_trip_created', {
      destination: data.destination,
      duration: data.duration,
      travelers: data.travelers,
      budget: data.budget,
      execution_id: data.executionId,
      timestamp: new Date().toISOString(),
    });
  }

  /**
   * Track agent progress
   */
  trackAgentProgress(data: {
    executionId: string;
    progress: number;
    currentStep?: string;
    status: string;
  }) {
    this.track('agent_progress', {
      execution_id: data.executionId,
      progress: data.progress,
      current_step: data.currentStep,
      status: data.status,
      timestamp: new Date().toISOString(),
    });
  }

  /**
   * Track feature usage
   */
  trackFeature(featureName: string, context?: Record<string, any>) {
    this.track('feature_used', {
      feature_name: featureName,
      ...context,
      timestamp: new Date().toISOString(),
    });
  }

  /**
   * Send event to backend analytics endpoint
   */
  private async sendToBackend(event: AnalyticsEvent) {
    try {
      const apiUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/v1';
      await fetch(`${apiUrl}/analytics/events`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          ...event,
          userId: this.userId,
          timestamp: new Date().toISOString(),
          userAgent: navigator.userAgent,
          url: window.location.href,
        }),
      });
    } catch (error) {
      // Silently fail - don't break app if analytics fails
      console.warn('[Analytics] Failed to send to backend:', error);
    }
  }
}

// Extend Window interface for gtag
declare global {
  interface Window {
    dataLayer: any[];
    gtag: (...args: any[]) => void;
  }
}

export const analytics = new AnalyticsService();

// Initialize with tracking ID from environment
if (import.meta.env.VITE_GA_TRACKING_ID) {
  analytics.initialize(import.meta.env.VITE_GA_TRACKING_ID);
}
