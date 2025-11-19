/**
 * Simplified Analytics Service
 * Sends events to backend Pub/Sub → BigQuery pipeline
 * 
 * This is intentionally minimal - no complex logic, just event tracking.
 */

interface AnalyticsEvent {
  eventName: string;
  timestamp: number;
  userId: string | null;
  sessionId: string;
  platform: string;
  properties?: Record<string, any>;
}

class AnalyticsService {
  private userId: string | null = null;
  private sessionId: string;
  private enabled: boolean;
  private apiUrl: string;

  constructor() {
    this.sessionId = this.generateSessionId();
    this.userId = this.restoreUserId();
    this.enabled = import.meta.env.VITE_ENABLE_ANALYTICS !== 'false';
    this.apiUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/v1';
    
    if (this.enabled) {
      console.log('[Analytics] Initialized - Session:', this.sessionId, 'User:', this.userId || 'anonymous');
    }
  }

  /**
   * Track an analytics event.
   * This is the main method - all other methods call this.
   */
  track(eventName: string, properties?: Record<string, any>) {
    if (!this.enabled) return;

    const event: AnalyticsEvent = {
      eventName,
      timestamp: Date.now(),
      userId: this.userId,
      sessionId: this.sessionId,
      platform: 'web',
      properties: properties || {}
    };

    // Send to backend (async, non-blocking)
    this.sendEvent(event);
  }

  /**
   * Track page view.
   */
  page(path: string, title?: string) {
    this.track('page_view', {
      path,
      title: title || document.title,
      referrer: document.referrer
    });
  }

  /**
   * Identify user.
   */
  identify(userId: string) {
    this.userId = userId;
    localStorage.setItem('analytics_user_id', userId);
    console.log('[Analytics] User identified:', userId);
  }

  /**
   * Clear user identity (on logout).
   */
  clearIdentity() {
    this.userId = null;
    localStorage.removeItem('analytics_user_id');
    console.log('[Analytics] User identity cleared');
  }

  /**
   * Restore userId from localStorage.
   */
  private restoreUserId(): string | null {
    return localStorage.getItem('analytics_user_id');
  }

  /**
   * Track booking events.
   */
  trackBooking(type: 'initiated' | 'completed' | 'failed', data: {
    provider: string;
    category: string;
    itineraryId?: string;
    amount?: number;
    currency?: string;
    error?: string;
  }) {
    this.track(`booking_${type}`, data);
  }

  /**
   * Track signup events.
   */
  trackSignup(status: 'started' | 'completed' | 'failed', data?: {
    method?: string;
    error?: string;
  }) {
    this.track(`user_signup_${status}`, data);
  }

  /**
   * Track trip wizard and creation events.
   */
  trackTripWizard(action: 'started' | 'initiated' | 'completed' | 'failed', data?: {
    destination?: string;
    origin?: string;
    itineraryId?: string;
    error?: string;
  }) {
    const eventName = action === 'started' ? 'trip_wizard_started' : `trip_creation_${action}`;
    this.track(eventName, data);
  }

  /**
   * Track payment events.
   */
  trackPayment(status: 'initiated' | 'completed' | 'failed', data: {
    amount?: number;
    currency?: string;
    itineraryId?: string;
    provider?: string;
    error?: string;
  }) {
    this.track(`payment_${status}`, data);
  }

  /**
   * Track user interaction events.
   */
  trackInteraction(action: 'activity_viewed' | 'day_expanded' | 'chat_message_sent' | 'search_initiated', data?: {
    activityId?: string;
    activityName?: string;
    dayNumber?: number;
    messageLength?: number;
    query?: string;
    queryLength?: number;
    resultCount?: number;
    itineraryId?: string;
  }) {
    this.track(action, data);
  }

  /**
   * Track public link creation.
   */
  trackPublicLink(itineraryId: string, data?: {
    linkId?: string;
    expiresAt?: string;
  }) {
    this.track('public_link_created', { itineraryId, ...data });
  }

  /**
   * Send event to backend.
   * Uses keepalive to ensure delivery even if page unloads.
   */
  private sendEvent(event: AnalyticsEvent) {
    try {
      fetch(`${this.apiUrl}/analytics/events`, {
        method: 'POST',
        keepalive: true, // Ensures delivery even if page closes
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(event)
      })
      .then(response => {
        if (!response.ok) {
          console.warn(`[Analytics] Event failed: ${event.eventName} (${response.status})`);
        }
      })
      .catch((error) => {
        console.warn(`[Analytics] Network error for event: ${event.eventName}`, error);
      });
    } catch (error) {
      console.warn(`[Analytics] Failed to send event: ${event.eventName}`, error);
    }
  }

  /**
   * Generate a unique session ID.
   */
  private generateSessionId(): string {
    // Check if session ID exists in sessionStorage
    const stored = sessionStorage.getItem('analytics_session_id');
    if (stored) return stored;

    // Generate new session ID
    const sessionId = `session_${Date.now()}_${Math.random().toString(36).substring(2, 11)}`;
    sessionStorage.setItem('analytics_session_id', sessionId);
    return sessionId;
  }
}

export const analytics = new AnalyticsService();
