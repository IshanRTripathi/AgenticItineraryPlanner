import { useEffect } from 'react';
import { useLocation } from 'react-router-dom';
import { analytics } from '@/services/analytics';

/**
 * Hook to automatically track page views on route changes.
 * Add this to your App component to track all page navigations.
 */
export function usePageTracking() {
  const location = useLocation();

  useEffect(() => {
    // Track page view whenever location changes
    analytics.page(location.pathname, document.title);
  }, [location.pathname]);
}
