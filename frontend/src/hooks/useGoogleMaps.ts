/**
 * Google Maps Hook
 * Manages Google Maps API loading and initialization
 */

import { useState, useEffect } from 'react';
import { loadGoogleMaps } from '@/utils/googleMapsLoader';

interface GoogleMapsAPI {
  maps: any;
}

interface UseGoogleMapsResult {
  isLoading: boolean;
  error: Error | null;
  api: GoogleMapsAPI | null;
}

export function useGoogleMaps(): UseGoogleMapsResult {
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<Error | null>(null);
  const [api, setApi] = useState<GoogleMapsAPI | null>(null);

  useEffect(() => {
    const apiKey = import.meta.env.VITE_GOOGLE_MAPS_BROWSER_KEY;

    if (!apiKey) {
      setError(new Error('Google Maps API key is not configured'));
      setIsLoading(false);
      return;
    }

    loadGoogleMaps(apiKey, ['places', 'geometry'])
      .then((google) => {
        setApi({ maps: google.maps });
        setIsLoading(false);
        console.log('[useGoogleMaps] Google Maps API loaded successfully');
      })
      .catch((err) => {
        console.error('[useGoogleMaps] Failed to load Google Maps:', err);
        setError(err);
        setIsLoading(false);
      });
  }, []);

  return { isLoading, error, api };
}
