import React, { useEffect, useRef } from 'react'
import { useGoogleMaps } from '../../hooks/useGoogleMaps'
import type { TripMapProps } from '../../types/MapTypes'
import { createRoot, Root } from 'react-dom/client'
import { useMapState } from '../../hooks/useMapState'
import { PlaceInfoCard } from './cards/PlaceInfoCard'
import { logger } from '../../utils/logger'

interface ExtendedTripMapProps extends TripMapProps {
  mapState?: ReturnType<typeof useMapState>;
}

export function TripMap({
  nodes,
  days,
  onAddPlace,
  onPlaceSelected,
  onMapReady,
  className,
  mapState,
}: ExtendedTripMapProps) {
  const { error, api } = useGoogleMaps()
  const {
    center,
    zoom,
    highlightedMarkers,
    selectedNodeId,
    setCenter,
    setZoom,
    setSelectedNode,
    addHighlightedMarker,
    clearHighlightedMarkers
  } = mapState || {
    center: null,
    zoom: 12,
    highlightedMarkers: [],
    selectedNodeId: null,
    setCenter: () => {},
    setZoom: () => {},
    setSelectedNode: () => {},
    addHighlightedMarker: () => {},
    clearHighlightedMarkers: () => {}
  }

  const mapRef = useRef<HTMLDivElement | null>(null)
  const mapInstanceRef = useRef<any | null>(null)
  const pendingClickRef = useRef<{ lat: number; lng: number; name?: string; address?: string } | null>(null)
  const infoDivRef = useRef<HTMLDivElement | null>(null)
  const infoWindowRef = useRef<any | null>(null)
  const infoRootRef = useRef<Root | null>(null)

  // Initialize map
  useEffect(() => {
    if (!api || !mapRef.current || mapInstanceRef.current) return

    // Calculate initial center and zoom based on nodes
    let initialCenter = center
    let initialZoom = zoom

    if (!initialCenter && nodes.length > 0) {
      // Calculate bounds from all nodes
      const validNodes = nodes.filter(n => n.position &&
        typeof n.position.lat === 'number' &&
        typeof n.position.lng === 'number')

      if (validNodes.length > 0) {
        // Calculate center as average of all positions
        const sumLat = validNodes.reduce((sum, n) => sum + n.position.lat, 0)
        const sumLng = validNodes.reduce((sum, n) => sum + n.position.lng, 0)
        initialCenter = {
          lat: sumLat / validNodes.length,
          lng: sumLng / validNodes.length
        }

        // Calculate appropriate zoom based on spread of markers
        const lats = validNodes.map(n => n.position.lat)
        const lngs = validNodes.map(n => n.position.lng)
        const latSpread = Math.max(...lats) - Math.min(...lats)
        const lngSpread = Math.max(...lngs) - Math.min(...lngs)
        const maxSpread = Math.max(latSpread, lngSpread)

        // Zoom levels: 0.001 = ~13, 0.01 = ~11, 0.1 = ~9, 1 = ~7
        if (maxSpread < 0.01) initialZoom = 13
        else if (maxSpread < 0.05) initialZoom = 11
        else if (maxSpread < 0.2) initialZoom = 9
        else if (maxSpread < 1) initialZoom = 7
        else initialZoom = 5

        logger.debug('Calculated initial view', { 
          component: 'TripMap',
          initialCenter, 
          initialZoom, 
          nodeCount: validNodes.length, 
          maxSpread 
        });
      }
    }

    // Fallback to world view if still no center
    if (!initialCenter) {
      initialCenter = { lat: 0, lng: 0 }
      initialZoom = 2
    }

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
            console.warn('[Maps] Places API failed, falling back to geocoding', e)
          }
        }

        // Fallback to reverse geocoding if Places API didn't return results
        if (!name || !address) {
          try {
            const geocoder = new api.maps.Geocoder()
            const res = await geocoder.geocode({ location: { lat, lng } })
            const first = res.results?.[0]
            address = address || first?.formatted_address
            name = name || (first?.address_components?.find((c: any) => c.types?.includes('point_of_interest'))?.long_name)
              || first?.address_components?.find((c: any) => c.types?.includes('establishment'))?.long_name
              || first?.address_components?.find((c: any) => c.types?.includes('premise'))?.long_name
              || first?.address_components?.find((c: any) => c.types?.includes('route'))?.long_name
              || first?.address_components?.[0]?.long_name
              || address
            placeTypes = placeTypes || first?.types || undefined
            placeMapsLink = placeMapsLink || `https://www.google.com/maps/search/?api=1&query=${lat},${lng}`
            console.info('[Maps] Reverse geocode result (fallback)', {
              formattedAddress: address,
              chosenName: name,
              types: placeTypes,
              resultTypes: res.results?.[0]?.types,
            })
          } catch (geocodeError) {
            console.warn('[Maps] Geocoding failed', geocodeError)
          }
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
          } catch { }
        }
      } catch (e) {
        // ignore
      }
    });
    onMapReady?.(map)
  }, [api, center, zoom, onMapReady])

  // Update map center and zoom when context changes
  useEffect(() => {
    const map = mapInstanceRef.current
    if (!api || !map || !center) return

    // Update map view
    map.setCenter(center)
    map.setZoom(zoom)
  }, [api, center, zoom])

  // Update markers when nodes or highlight state changes
  useEffect(() => {
    const map = mapInstanceRef.current
    if (!api || !map) return

    logger.debug('Updating markers', {
      component: 'TripMap',
      totalNodes: nodes.length,
      highlightedMarkers,
      selectedNodeId
    });

    // Clear existing markers
    const existingMarkers = map.markers || []
    existingMarkers.forEach((marker: any) => marker.setMap(null))
    map.markers = []

    // Create new markers
    const createdMarkers: any[] = []
    let skippedNodes = 0

    nodes.forEach((node) => {
      // Validate node has position with valid coordinates
      if (!node.position ||
        typeof node.position.lat !== 'number' ||
        typeof node.position.lng !== 'number' ||
        isNaN(node.position.lat) ||
        isNaN(node.position.lng)) {
        skippedNodes++
        logger.debug('Skipping node without valid position', {
          component: 'TripMap',
          nodeId: node.id,
          nodeTitle: node.title,
          position: node.position
        });
        return
      }

      const isHighlighted = highlightedMarkers.includes(node.id)
      const isSelected = selectedNodeId === node.id

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
      })

      // Add click listener
      marker.addListener('click', () => {
        logger.debug('Marker clicked', { component: 'TripMap', nodeId: node.id });

        // Update map context
        setCenter(node.position)
        setZoom(15)

        // Update selection for bidirectional sync
        setSelectedNode(node.id)
        clearHighlightedMarkers()
        addHighlightedMarker(node.id)

        // Notify parent component
        onPlaceSelected?.({
          name: node.title,
          address: node.title,
          lat: node.position.lat,
          lng: node.position.lng,
        })
      })

      createdMarkers.push(marker)
    })

    // Store marker references
    map.markers = createdMarkers

    logger.debug('Markers created', {
      component: 'TripMap',
      created: createdMarkers.length,
      skipped: skippedNodes,
      total: nodes.length
    });

    // Fit bounds to show all markers if we have multiple markers and no specific center set
    if (createdMarkers.length > 1 && !center) {
      const bounds = new api.maps.LatLngBounds()
      createdMarkers.forEach((marker: any) => {
        bounds.extend(marker.getPosition())
      })
      map.fitBounds(bounds)

      // Add padding to bounds
      const padding = { top: 50, right: 50, bottom: 50, left: 50 }
      map.fitBounds(bounds, padding)

      logger.debug('Fitted bounds to show all markers', { 
        component: 'TripMap',
        markerCount: createdMarkers.length 
      });
    } else if (createdMarkers.length === 0) {
      logger.warn('No markers to display. Locations may need enrichment.', { 
        component: 'TripMap' 
      });
    }
  }, [api, nodes, highlightedMarkers, selectedNodeId, center, setCenter, setZoom, setSelectedNode, addHighlightedMarker, clearHighlightedMarkers, onPlaceSelected])

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



