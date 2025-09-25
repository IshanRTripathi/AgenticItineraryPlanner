import React, { useEffect, useRef } from 'react'
import { useGoogleMaps } from '../../hooks/useGoogleMaps'
import type { TripMapProps } from '../../types/MapTypes'

export function TripMap({
  itineraryId,
  mapBounds,
  countryCentroid,
  nodes,
  days,
  onAddPlace,
  onPlaceSelected,
  onMapReady,
  className,
}: TripMapProps) {
  const { isLoading, error, api } = useGoogleMaps()
  const mapRef = useRef<HTMLDivElement | null>(null)
  const mapInstanceRef = useRef<any | null>(null)
  const pendingClickRef = useRef<{ lat: number; lng: number; name?: string; address?: string } | null>(null)
  const infoDivRef = useRef<HTMLDivElement | null>(null)

  // Initialize map
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
    // Attach click listener to capture lat/lng and perform reverse geocoding
    map.addListener('click', async (ev: any) => {
      try {
        const lat = ev?.latLng?.lat?.();
        const lng = ev?.latLng?.lng?.();
        if (typeof lat !== 'number' || typeof lng !== 'number') return;
        pendingClickRef.current = { lat, lng };
        let name: string | undefined;
        let address: string | undefined;
        if (api.maps.places) {
          const geocoder = new api.maps.Geocoder();
          const res = await geocoder.geocode({ location: { lat, lng } });
          const first = res.results?.[0];
          address = first?.formatted_address;
          name = first?.address_components?.[0]?.long_name || address;
        }
        pendingClickRef.current = { lat, lng, name, address };
        // Prefer lifting to parent to render a modal/portal
        if (onPlaceSelected) {
          onPlaceSelected({ lat, lng, name, address })
        } else if (onAddPlace && days && days.length > 0) {
          // Backward-compatible overlay
          showOverlayPrompt({ lat, lng, name, address });
        }
      } catch (e) {
        // ignore
      }
    });
    onMapReady?.(map)
  }, [api, onMapReady])

  // Fit map to bounds/centroid/nodes when available
  useEffect(() => {
    const map = mapInstanceRef.current
    if (!api || !map) return

    try {
      if (mapBounds &&
          typeof mapBounds.south === 'number' && typeof mapBounds.west === 'number' &&
          typeof mapBounds.north === 'number' && typeof mapBounds.east === 'number') {
        const bounds = new api.maps.LatLngBounds(
          { lat: mapBounds.south, lng: mapBounds.west },
          { lat: mapBounds.north, lng: mapBounds.east }
        )
        map.fitBounds(bounds, { padding: 48 })
        return
      }

      if (countryCentroid && typeof countryCentroid.lat === 'number' && typeof countryCentroid.lng === 'number') {
        map.setCenter(countryCentroid)
        map.setZoom(8)
        return
      }

      if (nodes && nodes.length > 0) {
        const bounds = new api.maps.LatLngBounds()
        let validCount = 0
        nodes.forEach(n => {
          if (n?.position && typeof n.position.lat === 'number' && typeof n.position.lng === 'number') {
            bounds.extend(n.position)
            validCount += 1
          }
        })
        if (validCount === 1) {
          map.setCenter(bounds.getCenter())
          map.setZoom(12)
        } else if (validCount > 1) {
          map.fitBounds(bounds, { padding: 48 })
        }
        return
      }

      // Default world view
      map.setCenter({ lat: 0, lng: 0 })
      map.setZoom(2)
    } catch (e) {
      // Ignore fit errors
    }
  }, [api, mapBounds, countryCentroid, JSON.stringify(nodes)])

  // Simple overlay UI for adding place to a day
  const showOverlayPrompt = (place: { lat: number; lng: number; name?: string; address?: string }) => {
    if (!infoDivRef.current) {
      infoDivRef.current = document.createElement('div')
      infoDivRef.current.style.position = 'absolute'
      infoDivRef.current.style.top = '16px'
      infoDivRef.current.style.right = '16px'
      infoDivRef.current.style.zIndex = '30'
      infoDivRef.current.style.background = 'white'
      infoDivRef.current.style.border = '1px solid #e5e7eb'
      infoDivRef.current.style.borderRadius = '8px'
      infoDivRef.current.style.boxShadow = '0 10px 15px -3px rgba(0,0,0,0.1), 0 4px 6px -4px rgba(0,0,0,0.1)'
      infoDivRef.current.style.padding = '12px'
      infoDivRef.current.style.maxWidth = '320px'
      infoDivRef.current.style.fontSize = '14px'
      if (mapRef.current?.parentElement) {
        mapRef.current.parentElement.appendChild(infoDivRef.current)
      }
    }
    const title = place.name || 'Selected location'
    const addr = place.address || `${place.lat.toFixed(5)}, ${place.lng.toFixed(5)}`
    const options = (days || []).map(d => `<option value="${d.id}">Day ${d.dayNumber}${d.date ? ` • ${d.date}` : ''}</option>`).join('')
    infoDivRef.current!.innerHTML = `
      <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:8px;">
        <div style="font-weight:600;">Add to itinerary</div>
        <button id="map-overlay-close" style="border:none;background:transparent;font-size:16px;cursor:pointer">×</button>
      </div>
      <div style="margin-bottom:8px;">
        <div style="font-weight:500;">${title}</div>
        <div style="color:#6b7280;">${addr}</div>
      </div>
      <div style="display:flex;gap:8px;align-items:center;">
        <label for="map-overlay-day" style="font-size:12px;color:#6b7280;">Day</label>
        <select id="map-overlay-day" style="flex:1;border:1px solid #e5e7eb;border-radius:6px;padding:6px;">${options}</select>
        <button id="map-overlay-add" style="background:#2563eb;color:white;border:none;border-radius:6px;padding:6px 10px;cursor:pointer;">Add</button>
      </div>
    `
    const closeBtn = infoDivRef.current!.querySelector('#map-overlay-close') as HTMLButtonElement
    const addBtn = infoDivRef.current!.querySelector('#map-overlay-add') as HTMLButtonElement
    const daySel = infoDivRef.current!.querySelector('#map-overlay-day') as HTMLSelectElement
    closeBtn.onclick = () => {
      infoDivRef.current?.remove()
      infoDivRef.current = null
    }
    addBtn.onclick = () => {
      if (!onAddPlace || !daySel?.value || !pendingClickRef.current) return
      const d = (days || []).find(x => x.id === daySel.value)
      if (!d) return
      onAddPlace({
        dayId: d.id,
        dayNumber: d.dayNumber,
        place: {
          name: pendingClickRef.current.name || 'Selected location',
          lat: pendingClickRef.current.lat,
          lng: pendingClickRef.current.lng,
          address: pendingClickRef.current.address,
        }
      })
      infoDivRef.current?.remove()
      infoDivRef.current = null
    }
  }

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


