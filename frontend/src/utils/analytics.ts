/**
 * Analytics Tracking Utilities
 * Tracks feature usage, errors, and performance
 */

interface AnalyticsEvent {
  category: string;
  action: string;
  label?: string;
  value?: number;
  timestamp: Date;
}

interface PerformanceMetric {
  name: string;
  value: number;
  unit: 'ms' | 'bytes' | 'count';
  timestamp: Date;
}

class AnalyticsManager {
  private events: AnalyticsEvent[] = [];
  private metrics: PerformanceMetric[] = [];
  private errorCount: Map<string, number> = new Map();

  /**
   * Track feature usage
   */
  trackFeatureUsage(feature: string, action: string, label?: string) {
    this.trackEvent('feature', `${feature}:${action}`, label);
    console.log(`[Analytics] Feature: ${feature} - ${action}`, label);
  }

  /**
   * Track error
   */
  trackError(errorCode: string, errorMessage: string, context?: Record<string, any>) {
    const count = (this.errorCount.get(errorCode) || 0) + 1;
    this.errorCount.set(errorCode, count);
    
    this.trackEvent('error', errorCode, errorMessage, count);
    console.error(`[Analytics] Error: ${errorCode}`, { message: errorMessage, count, context });
  }

  /**
   * Track performance metric
   */
  trackPerformance(name: string, value: number, unit: 'ms' | 'bytes' | 'count' = 'ms') {
    const metric: PerformanceMetric = {
      name,
      value,
      unit,
      timestamp: new Date(),
    };
    
    this.metrics.push(metric);
    console.log(`[Analytics] Performance: ${name} = ${value}${unit}`);
    
    // Keep only last 100 metrics
    if (this.metrics.length > 100) {
      this.metrics.shift();
    }
  }

  /**
   * Track user flow
   */
  trackUserFlow(flow: string, step: string, metadata?: Record<string, any>) {
    this.trackEvent('user_flow', `${flow}:${step}`, JSON.stringify(metadata));
    console.log(`[Analytics] User Flow: ${flow} - ${step}`, metadata);
  }

  /**
   * Generic event tracking
   */
  private trackEvent(category: string, action: string, label?: string, value?: number) {
    const event: AnalyticsEvent = {
      category,
      action,
      label,
      value,
      timestamp: new Date(),
    };
    
    this.events.push(event);
    
    // Keep only last 1000 events
    if (this.events.length > 1000) {
      this.events.shift();
    }
  }

  /**
   * Get error statistics
   */
  getErrorStats() {
    return {
      totalErrors: Array.from(this.errorCount.values()).reduce((a, b) => a + b, 0),
      uniqueErrors: this.errorCount.size,
      topErrors: Array.from(this.errorCount.entries())
        .sort((a, b) => b[1] - a[1])
        .slice(0, 10)
        .map(([code, count]) => ({ code, count })),
    };
  }

  /**
   * Get performance statistics
   */
  getPerformanceStats() {
    const metricsByName = new Map<string, number[]>();
    
    this.metrics.forEach(metric => {
      if (!metricsByName.has(metric.name)) {
        metricsByName.set(metric.name, []);
      }
      metricsByName.get(metric.name)!.push(metric.value);
    });
    
    const stats: Record<string, { avg: number; min: number; max: number; count: number }> = {};
    
    metricsByName.forEach((values, name) => {
      stats[name] = {
        avg: values.reduce((a, b) => a + b, 0) / values.length,
        min: Math.min(...values),
        max: Math.max(...values),
        count: values.length,
      };
    });
    
    return stats;
  }

  /**
   * Get feature usage statistics
   */
  getFeatureUsageStats() {
    const featureEvents = this.events.filter(e => e.category === 'feature');
    const usageByFeature = new Map<string, number>();
    
    featureEvents.forEach(event => {
      const feature = event.action.split(':')[0];
      usageByFeature.set(feature, (usageByFeature.get(feature) || 0) + 1);
    });
    
    return Array.from(usageByFeature.entries())
      .sort((a, b) => b[1] - a[1])
      .map(([feature, count]) => ({ feature, count }));
  }

  /**
   * Export analytics data
   */
  exportData() {
    return {
      events: this.events,
      metrics: this.metrics,
      errorStats: this.getErrorStats(),
      performanceStats: this.getPerformanceStats(),
      featureUsage: this.getFeatureUsageStats(),
      timestamp: new Date(),
    };
  }

  /**
   * Clear all analytics data
   */
  clear() {
    this.events = [];
    this.metrics = [];
    this.errorCount.clear();
  }
}

export const analytics = new AnalyticsManager();

// Helper functions for common tracking scenarios
export const trackPageView = (page: string) => {
  analytics.trackFeatureUsage('navigation', 'page_view', page);
};

export const trackButtonClick = (button: string, context?: string) => {
  analytics.trackFeatureUsage('interaction', 'button_click', `${button}${context ? `:${context}` : ''}`);
};

export const trackApiCall = (endpoint: string, duration: number, success: boolean) => {
  analytics.trackPerformance(`api:${endpoint}`, duration, 'ms');
  if (!success) {
    analytics.trackError('API_ERROR', `Failed: ${endpoint}`);
  }
};
