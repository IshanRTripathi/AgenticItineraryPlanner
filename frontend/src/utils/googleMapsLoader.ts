let mapsScriptPromise: Promise<typeof google> | null = null;

export function loadGoogleMaps(apiKey: string, libraries: string[] = ['places']): Promise<typeof google> {
  if (mapsScriptPromise) return mapsScriptPromise;

  mapsScriptPromise = new Promise((resolve, reject) => {
    if (!apiKey) {
      reject(new Error('Missing Google Maps API key'));
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

    const script = document.createElement('script');
    const params = new URLSearchParams({
      key: apiKey,
      libraries: libraries.join(','),
      loading: 'async',
      callback: callbackName,
    });
    script.src = `https://maps.googleapis.com/maps/api/js?${params.toString()}`;
    script.async = true;
    script.defer = true;
    script.onerror = (err) => {
      reject(new Error('Failed to load Google Maps script'));
    };
    document.head.appendChild(script);
  });

  return mapsScriptPromise;
}

export function resetGoogleMapsLoaderForTests() {
  mapsScriptPromise = null;
}

