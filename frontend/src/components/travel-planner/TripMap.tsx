import React, { useEffect, useRef } from 'react'
import { useGoogleMaps } from '../../hooks/useGoogleMaps'
import type { TripMapProps } from '../../types/MapTypes'
import { createRoot, Root } from 'react-dom/client'
import { useMapContext } from '../../contexts/MapContext'
import { PlaceInfoCard } from './cards/PlaceInfoCard'

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
  const { 
    viewMode, 
    center, 
    zoom, 
    highlightedMarkers, 
    selectedNodeId,
    setCenter,
    setZoom,
    setSelectedNode,
    addHighlightedMarker,
    clearHighlightedMarkers
  } = useMapContext()
  
  const mapRef = useRef<HTMLDivElement | null>(null)
  const mapInstanceRef = useRef<any | null>(null)
  const pendingClickRef = useRef<{ lat: number; lng: number; name?: string; address?: string } | null>(null)
  const infoDivRef = useRef<HTMLDivElement | null>(null)
  const infoWindowRef = useRef<any | null>(null)
  const infoRootRef = useRef<Root | null>(null)

  // Initialize map
  useEffect(() => {
    if (!api || !mapRef.current || mapInstanceRef.current) return
    
    // Use context center and zoom if available, otherwise use defaults
    const initialCenter = center || { lat: 0, lng: 0 }
    const initialZoom = center ? zoom : 2
    
    const map = new api.maps.Map(mapRef.current, {
      center: initialCenter,
      zoom: initialZoom,
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
        console.info('[Maps] Click at', { lat, lng })
        let name: string | undefined;
        let address: string | undefined;
        let placeTypes: string[] | undefined;
        let placeRating: number | undefined;
        let placeUserRatingCount: number | undefined;
        let placePhoneNumber: string | undefined;
        let placeMapsLink: string | undefined;
        let placeDetails: any | undefined;
        if (api.maps?.importLibrary) {
          try {
            // Use the NEW Places API (searchNearby + fetchFields)
            const placesLib: any = await (api.maps as any).importLibrary('places')
            const Place = placesLib.Place
            const SearchNearbyRankPreference = placesLib.SearchNearbyRankPreference
            const request = {
              locationRestriction: { center: { lat, lng }, radius: 40 },
              includedPrimaryTypes: undefined, // generic; let API choose
              maxResultCount: 5,
              rankPreference: SearchNearbyRankPreference.POPULARITY,
              fields: ['id', 'displayName', 'formattedAddress', 'location', 'types'],
            }
            const nearby: any = await Place.searchNearby(request)
            const top: any | undefined = nearby?.places?.[0]
            if (top) {
              // Fetch richer fields for the top place
              const fetched = await top.fetchFields({
                fields: [
                  'id',
                  'displayName',
                  'formattedAddress',
                  'location',
                  'types',
                  'rating',
                  'userRatingCount',
                  'internationalPhoneNumber',
                ],
              })
              placeDetails = fetched || top
              name = placeDetails?.displayName || top?.displayName || undefined
              address = placeDetails?.formattedAddress || top?.formattedAddress || undefined
              placeTypes = placeDetails?.types || top?.types || undefined
              placeRating = placeDetails?.rating || top?.rating || undefined
              placeUserRatingCount = placeDetails?.userRatingCount || top?.userRatingCount || undefined
              placePhoneNumber = placeDetails?.internationalPhoneNumber || top?.internationalPhoneNumber || undefined
              placeMapsLink = `https://www.google.com/maps/search/?api=1&query=${lat},${lng}&query_place_id=${placeDetails?.id || top?.id}`
              console.info('[Maps] Places (new) result', {
                name,
                address,
                types: placeTypes,
                rating: placeRating,
                userRatingCount: placeUserRatingCount,
                phoneNumber: placePhoneNumber,
                mapsLink: placeMapsLink,
              })
            }
          } catch (e) {
            console.warn('[Maps] Places (new) failed, falling back to Geocoder', e)
          }
        }
        if (!name && api.maps) {
          const geocoder = new api.maps.Geocoder();
          const res = await geocoder.geocode({ location: { lat, lng } });
          const first = res.results?.[0];
          address = address || first?.formatted_address;
          name = name || (first?.address_components?.find((c: any) => c.types?.includes('point_of_interest'))?.long_name)
              || first?.address_components?.find((c: any) => c.types?.includes('establishment'))?.long_name
              || first?.address_components?.find((c: any) => c.types?.includes('premise'))?.long_name
              || first?.address_components?.find((c: any) => c.types?.includes('route'))?.long_name
              || first?.address_components?.[0]?.long_name
              || address;
          placeTypes = placeTypes || first?.types || undefined;
          placeMapsLink = placeMapsLink || `https://www.google.com/maps/search/?api=1&query=${lat},${lng}`;
          console.info('[Maps] Reverse geocode result (fallback)', {
            formattedAddress: address,
            chosenName: name,
            types: placeTypes,
            resultTypes: res.results?.[0]?.types,
          })
        }
        pendingClickRef.current = { lat, lng, name, address, types: placeTypes, rating: placeRating, userRatingCount: placeUserRatingCount, phoneNumber: placePhoneNumber, mapsLink: placeMapsLink };
        // Prefer lifting to parent to render a modal/portal
        if (onPlaceSelected) {
          onPlaceSelected({ lat, lng, name, address })
        } else if (onAddPlace && days && days.length > 0) {
          // Render React card inside a Google Maps InfoWindow at clicked LatLng
          try {
            if (!infoDivRef.current) {
              infoDivRef.current = document.createElement('div')
            } else {
              infoRootRef.current?.unmount()
              infoDivRef.current.innerHTML = ''
            }
            infoRootRef.current = createRoot(infoDivRef.current)
            infoRootRef.current.render(
              <PlaceInfoCard
                place={{ lat, lng, name, address, types: placeTypes, rating: placeRating, userRatingCount: placeUserRatingCount, phoneNumber: placePhoneNumber, mapsLink: placeMapsLink }}
                days={days}
                onAdd={({ dayId, dayNumber }) => {
                  onAddPlace?.({ 
                    dayId, 
                    dayNumber, 
                    place: { 
                      name: name || 'Selected location', 
                      address, 
                      lat, 
                      lng,
                      types: placeTypes,
                      rating: placeRating,
                      userRatingCount: placeUserRatingCount,
                      phoneNumber: placePhoneNumber,
                      mapsLink: placeMapsLink
                    } 
                  })
                  infoWindowRef.current?.close()
                }}
                onClose={() => infoWindowRef.current?.close()}
              />
            )
            if (!infoWindowRef.current) {
              infoWindowRef.current = new api.maps.InfoWindow({ 
                maxWidth: 420,
                pixelOffset: new api.maps.Size(0, -10)
              })
            }
            infoWindowRef.current.setContent(infoDivRef.current)
            infoWindowRef.current.setPosition({ lat, lng })
            infoWindowRef.current.open({ map })
            // Pan into view if needed
            map.panTo({ lat, lng })
          } catch {}
        }
      } catch (e) {
        // ignore
      }
    });
    onMapReady?.(map)
  }, [api, onMapReady])

  // Update map center and zoom when context changes
  useEffect(() => {
    const map = mapInstanceRef.current
    if (!api || !map || !center) return

    console.log('=== MAP CENTER UPDATE ===')
    console.log('New Center:', center)
    console.log('New Zoom:', zoom)
    console.log('View Mode:', viewMode)

    try {
      map.setCenter(center)
      map.setZoom(zoom)
      console.log('Map center and zoom updated successfully')
    } catch (error) {
      console.error('Failed to update map center/zoom:', error)
    }
  }, [api, center, zoom, viewMode])

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

  // Render markers with highlighting
  useEffect(() => {
    const map = mapInstanceRef.current
    if (!api || !map || !nodes || nodes.length === 0) return

    console.log('=== RENDERING MAP MARKERS ===');
    console.log('Nodes:', nodes);
    console.log('Highlighted Markers:', highlightedMarkers);
    console.log('Selected Node ID:', selectedNodeId);

    // Clear existing markers
    const existingMarkers = map.markers || [];
    existingMarkers.forEach((marker: any) => marker.setMap(null));
    map.markers = [];

    // Create new markers
    nodes.forEach((node) => {
      if (!node.position || typeof node.position.lat !== 'number' || typeof node.position.lng !== 'number') {
        return;
      }

      const isHighlighted = highlightedMarkers.includes(node.id);
      const isSelected = selectedNodeId === node.id;

      console.log(`Creating marker for ${node.id}:`, {
        position: node.position,
        isHighlighted,
        isSelected,
        title: node.title
      });

      // Create marker with different styles based on state
      const marker = new api.maps.Marker({
        position: node.position,
        map: map,
        title: node.title,
        icon: {
          url: isHighlighted || isSelected 
            ? 'data:image/svg+xml;charset=UTF-8,' + encodeURIComponent(`
                <svg width="32" height="32" viewBox="0 0 32 32" xmlns="http://www.w3.org/2000/svg">
                  <circle cx="16" cy="16" r="12" fill="${isSelected ? '#ef4444' : '#3b82f6'}" stroke="white" stroke-width="3"/>
                  <circle cx="16" cy="16" r="6" fill="white"/>
                </svg>
              `)
            : 'data:image/svg+xml;charset=UTF-8,' + encodeURIComponent(`
                <svg width="24" height="24" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                  <circle cx="12" cy="12" r="8" fill="#6b7280" stroke="white" stroke-width="2"/>
                </svg>
              `),
          scaledSize: new api.maps.Size(isHighlighted || isSelected ? 32 : 24, isHighlighted || isSelected ? 32 : 24),
          anchor: new api.maps.Point(16, 16),
        },
        animation: isHighlighted || isSelected ? api.maps.Animation.BOUNCE : null,
      });

      // Add click listener
      marker.addListener('click', () => {
        console.log('=== MAP MARKER CLICK ===');
        console.log('Marker clicked:', node.id);
        console.log('Node:', node);
        console.log('View Mode:', viewMode);
        
        // Update map context
        setCenter(node.position);
        setZoom(15);
        
        // Update selection for bidirectional sync
        setSelectedNode(node.id);
        clearHighlightedMarkers();
        addHighlightedMarker(node.id);
        
        // Notify parent component
        onPlaceSelected?.({
          name: node.title,
          address: node.title,
          lat: node.position.lat,
          lng: node.position.lng,
        });
      });

      // Store marker reference
      map.markers = map.markers || [];
      map.markers.push(marker);
    });

    console.log('Markers rendered:', map.markers.length);
  }, [api, nodes, highlightedMarkers, selectedNodeId, setCenter, setZoom, setSelectedNode, addHighlightedMarker, clearHighlightedMarkers, onPlaceSelected]);

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


