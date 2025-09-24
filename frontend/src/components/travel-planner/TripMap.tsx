import React, { useEffect, useRef } from 'react'
import { useGoogleMaps } from '../../hooks/useGoogleMaps'
import type { TripMapProps } from '../../types/MapTypes'

export function TripMap({
  itineraryId,
  mapBounds,
  countryCentroid,
  nodes,
  onMapReady,
  className,
}: TripMapProps) {
  const { isLoading, error, api } = useGoogleMaps()
  const mapRef = useRef<HTMLDivElement | null>(null)
  const mapInstanceRef = useRef<google.maps.Map | null>(null)

  useEffect(() => {
    if (!api || !mapRef.current || mapInstanceRef.current) return
    const map = new api.maps.Map(mapRef.current, {
      center: { lat: 0, lng: 0 },
      zoom: 2,
      mapTypeId: api.maps.MapTypeId.ROADMAP,
      gestureHandling: 'greedy',
      fullscreenControl: true,
      streetViewControl: false,
      mapTypeControl: false,
      zoomControl: true,
    })
    mapInstanceRef.current = map
    onMapReady?.(map)
  }, [api, onMapReady])

  return (
    <div className={className} style={{ width: '100%', height: '100%' }}>
      {error ? (
        <div className="p-4 text-sm">Map unavailable. {error.message}</div>
      ) : (
        <div ref={mapRef} style={{ width: '100%', height: '100%' }} />
      )}
    </div>
  )
}


