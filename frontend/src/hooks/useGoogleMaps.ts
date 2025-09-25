import { useEffect, useState } from 'react'
import { loadGoogleMaps } from '../utils/googleMapsLoader'

export function useGoogleMaps() {
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<Error | null>(null)
  const [api, setApi] = useState<typeof google | null>(null)

  useEffect(() => {
    const key = ((import.meta as any).env?.VITE_GOOGLE_MAPS_BROWSER_KEY as string | undefined)?.trim() || ''
    setIsLoading(true)
    setError(null)
    loadGoogleMaps(key)
      .then(g => {
        setApi(g)
      })
      .catch(err => setError(err))
      .finally(() => setIsLoading(false))
  }, [])

  return { isLoading, error, api }
}

