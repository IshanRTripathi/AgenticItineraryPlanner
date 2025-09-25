import { useEffect, useState } from 'react'
import { loadGoogleMaps } from '../utils/googleMapsLoader'

export function useGoogleMaps() {
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<Error | null>(null)
  const [api, setApi] = useState<any | null>(null)

  useEffect(() => {
    const key = ((import.meta as any).env?.VITE_GOOGLE_MAPS_BROWSER_KEY as string | undefined)?.trim() || ''
    setIsLoading(true)
    setError(null)
    try {
      const masked = key ? (key.length > 8 ? `${key.slice(0, 4)}****${key.slice(-4)}` : '****') : 'missing';
      console.info('[Maps] useGoogleMaps init', {
        hasKey: Boolean(key),
        keyLen: key.length,
        keyMasked: masked,
        envHasKey: Boolean((import.meta as any).env?.VITE_GOOGLE_MAPS_BROWSER_KEY),
        location: typeof window !== 'undefined' ? window.location.origin : 'n/a',
      })
    } catch {}
    loadGoogleMaps(key)
      .then(g => {
        setApi(g)
        try {
          console.info('[Maps] useGoogleMaps ready', {
            version: (g as any)?.maps?.version,
          })
        } catch {}
      })
      .catch(err => setError(err))
      .finally(() => setIsLoading(false))
  }, [])

  return { isLoading, error, api }
}

