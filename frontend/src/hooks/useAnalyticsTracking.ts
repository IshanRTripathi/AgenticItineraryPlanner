/**
 * Custom hook for analytics tracking
 * Provides easy-to-use methods for tracking user interactions
 */

import { useCallback } from 'react';
import { analytics } from '@/services/analytics';

export function useAnalyticsTracking() {
  // Track activity view
  const trackActivityView = useCallback((activityId: string, activityName: string, itineraryId?: string) => {
    analytics.trackInteraction('activity_viewed', {
      activityId,
      activityName,
      itineraryId
    });
  }, []);

  // Track day expansion
  const trackDayExpansion = useCallback((dayNumber: number, itineraryId?: string) => {
    analytics.trackInteraction('day_expanded', {
      dayNumber,
      itineraryId
    });
  }, []);

  // Track chat message
  const trackChatMessage = useCallback((messageLength: number, itineraryId?: string) => {
    analytics.trackInteraction('chat_message_sent', {
      messageLength,
      itineraryId
    });
  }, []);

  // Track search
  const trackSearch = useCallback((query: string, resultCount?: number, itineraryId?: string) => {
    analytics.trackInteraction('search_initiated', {
      query,
      queryLength: query.length,
      resultCount,
      itineraryId
    });
  }, []);

  // Track public link creation
  const trackPublicLinkCreation = useCallback((itineraryId: string, linkId?: string) => {
    analytics.trackPublicLink(itineraryId, { linkId });
  }, []);

  // Track payment
  const trackPaymentInitiated = useCallback((amount: number, currency: string, itineraryId?: string) => {
    analytics.trackPayment('initiated', {
      amount,
      currency,
      itineraryId
    });
  }, []);

  const trackPaymentCompleted = useCallback((amount: number, currency: string, itineraryId?: string, provider?: string) => {
    analytics.trackPayment('completed', {
      amount,
      currency,
      itineraryId,
      provider
    });
  }, []);

  const trackPaymentFailed = useCallback((amount: number, currency: string, error: string, itineraryId?: string) => {
    analytics.trackPayment('failed', {
      amount,
      currency,
      error,
      itineraryId
    });
  }, []);

  // Track booking completion
  const trackBookingCompleted = useCallback((provider: string, category: string, itineraryId?: string, amount?: number) => {
    analytics.trackBooking('completed', {
      provider,
      category,
      itineraryId,
      amount,
      currency: 'USD'
    });
  }, []);

  const trackBookingFailed = useCallback((provider: string, category: string, error: string, itineraryId?: string) => {
    analytics.trackBooking('failed', {
      provider,
      category,
      error,
      itineraryId
    });
  }, []);

  return {
    trackActivityView,
    trackDayExpansion,
    trackChatMessage,
    trackSearch,
    trackPublicLinkCreation,
    trackPaymentInitiated,
    trackPaymentCompleted,
    trackPaymentFailed,
    trackBookingCompleted,
    trackBookingFailed
  };
}
