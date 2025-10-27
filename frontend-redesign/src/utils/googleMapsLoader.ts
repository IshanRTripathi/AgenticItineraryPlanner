let mapsScriptPromise: Promise<any> | null = null;

export function loadGoogleMaps(apiKey: string, libraries: string[] = ['places']): Promise<any> {
  if (mapsScriptPromise) return mapsScriptPromise;

  mapsScriptPromise = new Promise((resolve, reject) => {
    const key = (apiKey || '').trim();
    if (!key) {
      reject(new Error('Google Maps API key is missing. Ensure VITE_GOOGLE_MAPS_BROWSER_KEY is set at build time.'));
      return;
    }

    try {
      const masked = key.length > 8 ? `${key.slice(0, 4)}****${key.slice(-4)}` : '****';
      console.info('[Maps] Starting loader', {
        hostname: typeof window !== 'undefined' ? window.location.hostname : 'n/a',
        keyLen: key.length,
        keyMasked: masked,
        libs: libraries,
      });
    } catch {}

    // If already loaded
    if (typeof window !== 'undefined' && (window as any).google?.maps) {
      resolve((window as any).google);
      return;
    }

    const callbackName = '__onGoogleMapsLoaded';
    (window as any)[callbackName] = () => {
      try {
        const version = (window as any).google?.maps?.version;
        console.info('[Maps] API loaded successfully', { version });
      } catch {}
      resolve((window as any).google);
    };

    // Capture auth failures
    (window as any).gm_authFailure = () => {
      console.error('[Maps] Authentication failed: gm_authFailure');
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
    script.onerror = () => {
      reject(new Error('Failed to load Google Maps script. Verify network access and script URL.'));
    };
    script.onload = () => {
      console.info('[Maps] Script tag loaded');
    };
    try {
      console.info('[Maps] Injecting script', { src: script.src });
    } catch {}
    document.head.appendChild(script);
  });

  return mapsScriptPromise;
}

export function resetGoogleMapsLoaderForTests() {
  mapsScriptPromise = null;
}
