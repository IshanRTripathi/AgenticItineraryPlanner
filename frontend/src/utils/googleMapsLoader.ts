let mapsScriptPromise: Promise<typeof google> | null = null;

export function loadGoogleMaps(apiKey: string, libraries: string[] = ['places']): Promise<typeof google> {
  if (mapsScriptPromise) return mapsScriptPromise;

  mapsScriptPromise = new Promise((resolve, reject) => {
    const key = (apiKey || '').trim();
    if (!key) {
      reject(new Error('Google Maps API key is missing. Ensure VITE_GOOGLE_MAPS_BROWSER_KEY is set at build time.'));
      return;
    }

    // If already loaded
    if (typeof window !== 'undefined' && (window as any).google?.maps) {
      resolve((window as any).google);
      return;
    }

    const callbackName = '__onGoogleMapsLoaded';
    (window as any)[callbackName] = () => {
      resolve((window as any).google);
    };

    // Capture auth failures like InvalidKeyMapError / RefererNotAllowedMapError
    (window as any).gm_authFailure = () => {
      reject(new Error('Google Maps authentication failed. Check API key, referrer restrictions, and API enablement.'));
    };

    const script = document.createElement('script');
    const params = new URLSearchParams({
      key,
      libraries: libraries.join(','),
      loading: 'async',
      callback: callbackName,
    });
    script.src = `https://maps.googleapis.com/maps/api/js?${params.toString()}`;
    script.async = true;
    script.defer = true;
    script.onerror = (err) => {
      reject(new Error('Failed to load Google Maps script. Verify network access and script URL.'));
    };
    document.head.appendChild(script);
  });

  return mapsScriptPromise;
}

export function resetGoogleMapsLoaderForTests() {
  mapsScriptPromise = null;
}

