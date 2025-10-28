/**
 * Trip Map Component
 * Google Maps integration showing trip locations and routes
 * Uses smart coordinate resolution with fallback strategies
 */

import { useEffect, useRef, useState } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { MapPin, Navigation, Loader2, AlertCircle } from 'lucide-react';
import type { NormalizedItinerary, Coordinates } from '@/types/dto';
import { useGoogleMaps } from '@/hooks/useGoogleMaps';
import { MarkerClusterer } from '@googlemaps/markerclusterer';
import { coordinateResolver } from '@/services/coordinateResolver';

interface TripMapProps {
  itinerary: NormalizedItinerary;
}

interface MapNode {
  id: string;
  position: Coordinates;
  title: string;
  type: string;
  status: 'planned' | 'in_progress' | 'completed';
  locked: boolean;
  day: number;
  confidence: 'exact' | 'approximate' | 'city' | 'fallback';
}

export function TripMap({ itinerary }: TripMapProps) {
  const mapRef = useRef<HTMLDivElement>(null);
  const { isLoading, error, api } = useGoogleMaps();
  const [nodes, setNodes] = useState<MapNode[]>([]);
  const [isResolving, setIsResolving] = useState(true);
  const [resolutionStats, setResolutionStats] = useState({
    total: 0,
    exact: 0,
    approximate: 0,
    city: 0,
    fallback: 0,
  });

  // Extract destination city from itinerary
  // Extract just the city name, removing country suffix if present
  const extractCityName = (location: string): string => {
    if (!location) return 'Unknown';
    // Remove country suffix (e.g., "Tokyo, Japan" -> "Tokyo")
    const parts = location.split(',');
    return parts[0].trim();
  };
  
  const destinationCity = extractCityName(itinerary.days[0]?.location || 'Unknown');

  // Resolve coordinates for all nodes
  useEffect(() => {
    async function resolveCoordinates() {
      setIsResolving(true);
      console.log('[TripMap] ========== MAP COORDINATE RESOLUTION START ==========');
      console.log('[TripMap] Itinerary ID:', itinerary.itineraryId);
      console.log('[TripMap] Total days:', itinerary.days.length);
      console.log('[TripMap] Destination city:', destinationCity);

      const allNodes: MapNode[] = [];
      const stats = { total: 0, exact: 0, approximate: 0, city: 0, fallback: 0 };

      for (const day of itinerary.days) {
        console.log(`[TripMap] Processing Day ${day.dayNumber} with ${day.nodes.length} nodes`);
        
        for (const node of day.nodes) {
          stats.total++;
          
          console.log(`[TripMap] Resolving node: ${node.id} - ${node.title}`);
          console.log(`[TripMap]   Location data:`, node.location);
          
          // Resolve coordinates using smart resolver
          const result = await coordinateResolver.resolve(
            node.location,
            destinationCity
          );

          console.log(`[TripMap]   Resolved: (${result.coordinates.lat}, ${result.coordinates.lng})`);
          console.log(`[TripMap]   Confidence: ${result.confidence}`);
          console.log(`[TripMap]   Source: ${result.source}`);

          // Track statistics
          stats[result.confidence]++;

          // Add to nodes array
          allNodes.push({
            id: node.id,
            position: result.coordinates,
            title: node.title,
            type: node.type,
            status: 'planned',
            locked: node.locked || false,
            day: day.dayNumber,
            confidence: result.confidence,
          });

          console.log(`[TripMap] Resolved ${node.title}:`, {
            confidence: result.confidence,
            source: result.source,
            cached: result.cached,
            coordinates: result.coordinates,
          });
        }
      }

      setNodes(allNodes);
      setResolutionStats(stats);
      setIsResolving(false);

      console.log('[TripMap] ========== COORDINATE RESOLUTION COMPLETE ==========');
      console.log('[TripMap] Total nodes resolved:', allNodes.length);
      console.log('[TripMap] Resolution stats:', stats);
      console.log('[TripMap] Cache stats:', coordinateResolver.getCacheStats());
      console.log('[TripMap] Success rate:', `${((stats.exact + stats.approximate) / stats.total * 100).toFixed(1)}%`);
    }

    resolveCoordinates();
  }, [itinerary, destinationCity]);

  // Initialize map with resolved coordinates
  useEffect(() => {
    if (!api || !mapRef.current || nodes.length === 0 || isResolving) return;

    console.log('[TripMap] ========== MAP INITIALIZATION START ==========');
    console.log('[TripMap] Google Maps API loaded:', !!api);
    console.log('[TripMap] Map container ready:', !!mapRef.current);
    console.log('[TripMap] Nodes to display:', nodes.length);
    console.log('[TripMap] Is resolving:', isResolving);

    // Calculate initial center from nodes
    const sumLat = nodes.reduce((sum, n) => sum + n.position.lat, 0);
    const sumLng = nodes.reduce((sum, n) => sum + n.position.lng, 0);
    const initialCenter = {
      lat: sumLat / nodes.length,
      lng: sumLng / nodes.length,
    };

    // Initialize map
    const googleMap = new api.maps.Map(mapRef.current, {
      zoom: 12,
      center: initialCenter,
      styles: [
        {
          featureType: 'poi',
          elementType: 'labels',
          stylers: [{ visibility: 'off' }],
        },
      ],
      mapTypeControl: false,
      streetViewControl: false,
      fullscreenControl: true,
    });

    // Add markers with clustering
    const bounds = new api.maps.LatLngBounds();
    const markers = nodes.map((node, index) => {
      // Color based on confidence
      const markerColor = {
        exact: '#10B981',      // Green - exact coordinates
        approximate: '#3b82f6', // Blue - geocoded
        city: '#F59E0B',       // Amber - city center
        fallback: '#6B7280',   // Gray - fallback
      }[node.confidence];

      const marker = new api.maps.Marker({
        position: node.position,
        title: node.title,
        label: {
          text: `${index + 1}`,
          color: 'white',
          fontSize: '12px',
          fontWeight: 'bold',
        },
        icon: {
          path: api.maps.SymbolPath.CIRCLE,
          scale: 20,
          fillColor: markerColor,
          fillOpacity: 1,
          strokeColor: 'white',
          strokeWeight: 2,
        },
      });

      // Add info window with confidence indicator
      const confidenceLabel = {
        exact: '📍 Exact location',
        approximate: '📌 Approximate location',
        city: '🏙️ City center',
        fallback: '⚠️ Fallback location',
      }[node.confidence];

      const infoWindow = new api.maps.InfoWindow({
        content: `
          <div style="padding: 8px; min-width: 200px;">
            <h3 style="font-weight: bold; margin-bottom: 4px;">${node.title}</h3>
            <p style="color: #666; font-size: 12px; margin-bottom: 4px;">Day ${node.day}</p>
            <p style="color: #999; font-size: 11px;">${confidenceLabel}</p>
          </div>
        `,
      });

      marker.addListener('click', () => {
        infoWindow.open(googleMap, marker);
      });

      bounds.extend(node.position);
      return marker;
    });

    console.log('[TripMap] Created', markers.length, 'markers');

    // Add marker clustering for better performance
    new MarkerClusterer({ markers, map: googleMap });

    // Draw route polyline (only for exact/approximate coordinates)
    const routeNodes = nodes.filter(n => n.confidence === 'exact' || n.confidence === 'approximate');
    if (routeNodes.length > 1) {
      const path = routeNodes.map((node) => node.position);
      new api.maps.Polyline({
        path,
        geodesic: true,
        strokeColor: '#3b82f6',
        strokeOpacity: 0.8,
        strokeWeight: 3,
        map: googleMap,
      });
    }

    // Fit bounds to show all markers
    googleMap.fitBounds(bounds);
    
    console.log('[TripMap] Map initialized successfully');
  }, [api, nodes, isResolving]);

  if (isLoading || isResolving) {
    return (
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <MapPin className="w-5 h-5" />
            Trip Map
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div className="text-center py-12 text-muted-foreground">
            <Loader2 className="w-12 h-12 mx-auto mb-4 animate-spin" />
            <p>{isResolving ? 'Resolving coordinates...' : 'Loading map...'}</p>
            {isResolving && resolutionStats.total > 0 && (
              <p className="text-xs mt-2">
                Processed {resolutionStats.exact + resolutionStats.approximate + resolutionStats.city + resolutionStats.fallback} of {resolutionStats.total} locations
              </p>
            )}
          </div>
        </CardContent>
      </Card>
    );
  }

  if (error) {
    return (
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <MapPin className="w-5 h-5" />
            Trip Map
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div className="text-center py-12 text-muted-foreground">
            <MapPin className="w-12 h-12 mx-auto mb-4 opacity-50" />
            <p className="text-destructive">Failed to load Google Maps</p>
            <p className="text-sm mt-2">{error.message}</p>
          </div>
        </CardContent>
      </Card>
    );
  }

  if (nodes.length === 0) {
    return (
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <MapPin className="w-5 h-5" />
            Trip Map
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div className="text-center py-12 text-muted-foreground">
            <MapPin className="w-12 h-12 mx-auto mb-4 opacity-50" />
            <p>No locations to display</p>
            <p className="text-xs mt-2">
              Total days: {itinerary.days.length}, 
              Total activities: {itinerary.days.reduce((sum, d) => sum + d.nodes.length, 0)}
            </p>
          </div>
        </CardContent>
      </Card>
    );
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <Navigation className="w-5 h-5" />
          Trip Map
          <span className="ml-auto text-sm font-normal text-muted-foreground">
            {nodes.length} locations
          </span>
        </CardTitle>
      </CardHeader>
      <CardContent>
        <div
          ref={mapRef}
          className="w-full h-[500px] rounded-lg overflow-hidden bg-muted"
        />
        
        {/* Resolution statistics */}
        {resolutionStats.total > 0 && (
          <div className="mt-4 flex items-center gap-4 text-xs text-muted-foreground">
            <div className="flex items-center gap-1">
              <div className="w-3 h-3 rounded-full bg-green-500" />
              <span>{resolutionStats.exact} exact</span>
            </div>
            <div className="flex items-center gap-1">
              <div className="w-3 h-3 rounded-full bg-blue-500" />
              <span>{resolutionStats.approximate} approximate</span>
            </div>
            <div className="flex items-center gap-1">
              <div className="w-3 h-3 rounded-full bg-amber-500" />
              <span>{resolutionStats.city} city center</span>
            </div>
            {resolutionStats.fallback > 0 && (
              <div className="flex items-center gap-1">
                <AlertCircle className="w-3 h-3 text-gray-500" />
                <span>{resolutionStats.fallback} fallback</span>
              </div>
            )}
          </div>
        )}
      </CardContent>
    </Card>
  );
}
