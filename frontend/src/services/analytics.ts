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
    this.enabled = import.meta.env.VITE_ENABLE_ANALYTICS !== 'false';
    this.apiUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/v1';
    
    if (this.enabled) {
      console.log('[Analytics] Initialized - Session:', this.sessionId);
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
    console.log('[Analytics] User identified:', userId);
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
  }) {
    this.track(`booking_${type}`, data);
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
      }).catch(() => {
        // Silent failure - analytics should never break the app
      });
    } catch (error) {
      // Silent failure
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
