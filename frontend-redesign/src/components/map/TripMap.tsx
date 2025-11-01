/**
 * Trip Map Component
 * Google Maps integration showing trip locations and routes
 * Uses smart coordinate resolution with fallback strategies
 */

import { useEffect, useRef, useState, useMemo } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { MapPin, Navigation, Loader2, AlertCircle } from 'lucide-react';
import type { NormalizedItinerary, Coordinates } from '@/types/dto';
import { useGoogleMaps } from '@/hooks/useGoogleMaps';
import { MarkerClusterer } from '@googlemaps/markerclusterer';
import { coordinateResolver } from '@/services/coordinateResolver';
import { DAY_COLORS } from '@/constants/dayColors';
import { cn } from '@/lib/utils';

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

// Day colors imported from shared constants

// Type-based icons and colors
const ACTIVITY_TYPES = {
  attraction: { emoji: '🎯', label: 'Attraction', color: '#3B82F6' },
  meal: { emoji: '🍽️', label: 'Meal', color: '#F59E0B' },
  accommodation: { emoji: '🏨', label: 'Hotel', color: '#8B5CF6' },
  transport: { emoji: '🚗', label: 'Transport', color: '#6B7280' },
  shopping: { emoji: '🛍️', label: 'Shopping', color: '#EC4899' },
  entertainment: { emoji: '🎭', label: 'Entertainment', color: '#EF4444' },
  nature: { emoji: '🌳', label: 'Nature', color: '#10B981' },
  culture: { emoji: '🏛️', label: 'Culture', color: '#14B8A6' },
} as const;

// Confidence colors (for marker border)
const CONFIDENCE_COLORS = {
  exact: '#10B981',      // Green - exact coordinates
  approximate: '#3B82F6', // Blue - geocoded
  city: '#F59E0B',       // Amber - city center
  fallback: '#6B7280',   // Gray - fallback
} as const;

// Helper to detect generic activities
function isGenericActivity(title: string): boolean {
  const genericPatterns = [
    /breakfast/i,
    /lunch/i,
    /dinner/i,
    /hotel/i,
    /check.?in/i,
    /check.?out/i,
    /free time/i,
    /rest/i,
    /leisure/i,
    /morning activity/i,
    /afternoon activity/i,
    /evening activity/i,
  ];
  return genericPatterns.some(pattern => pattern.test(title));
}

// Helper to create clean SVG marker with day color
function createMarkerIcon(dayColor: string, isHighlighted: boolean): string {
  const size = isHighlighted ? 44 : 36;
  const radius = isHighlighted ? 16 : 14;
  const strokeWidth = isHighlighted ? 4 : 3;

  const svg = `
    <svg width="${size}" height="${size}" xmlns="http://www.w3.org/2000/svg">
      <!-- Drop shadow -->
      <defs>
        <filter id="shadow" x="-50%" y="-50%" width="200%" height="200%">
          <feGaussianBlur in="SourceAlpha" stdDeviation="2"/>
          <feOffset dx="0" dy="2" result="offsetblur"/>
          <feComponentTransfer>
            <feFuncA type="linear" slope="0.3"/>
          </feComponentTransfer>
          <feMerge>
            <feMergeNode/>
            <feMergeNode in="SourceGraphic"/>
          </feMerge>
        </filter>
      </defs>
      <!-- Marker circle -->
      <circle 
        cx="${size / 2}" 
        cy="${size / 2}" 
        r="${radius}" 
        fill="${dayColor}" 
        stroke="white" 
        stroke-width="${strokeWidth}"
        filter="url(#shadow)"
      />
    </svg>
  `;
  return `data:image/svg+xml;charset=UTF-8,${encodeURIComponent(svg)}`;
}

export function TripMap({ itinerary }: TripMapProps) {
  const mapRef = useRef<HTMLDivElement>(null);
  const mapInstanceRef = useRef<any>(null);
  const polylinesRef = useRef<any[]>([]);
  const markersRef = useRef<any[]>([]);
  const clustererRef = useRef<any>(null);
  const { isLoading, error, api } = useGoogleMaps();

  const [nodes, setNodes] = useState<MapNode[]>([]);
  const [isResolving, setIsResolving] = useState(true);
  const [resolutionProgress, setResolutionProgress] = useState({ current: 0, total: 0 });
  const [resolutionStats, setResolutionStats] = useState({
    total: 0,
    exact: 0,
    approximate: 0,
    city: 0,
    fallback: 0,
    filtered: 0, // Generic activities filtered out
  });

  // Day filtering state
  const [selectedDays, setSelectedDays] = useState<Set<number>>(new Set());
  const [highlightedDay, setHighlightedDay] = useState<number | null>(null);
  const [showAllDays, setShowAllDays] = useState(true);

  // Extract destination city from itinerary
  // Extract just the city name, removing country suffix if present
  const extractCityName = (location: string): string => {
    if (!location) return 'Unknown';
    // Remove country suffix (e.g., "Tokyo, Japan" -> "Tokyo")
    const parts = location.split(',');
    return parts[0].trim();
  };

  // Safe access to days - memoized to prevent infinite loops
  // Handle nested structure: itinerary.itinerary.days or itinerary.days
  const days = useMemo(() => (itinerary as any)?.itinerary?.days || (itinerary as any)?.days || [], [(itinerary as any)?.itinerary?.days, (itinerary as any)?.days]);
  const destinationCity = useMemo(
    () => extractCityName(days[0]?.location || 'Unknown'),
    [days]
  );

  // Initialize selected days when days change
  useEffect(() => {
    const allDayNumbers = days.map((d: any) => d.dayNumber);
    setSelectedDays(new Set(allDayNumbers));
  }, [days]);

  // Resolve coordinates for all nodes with parallel batch processing
  useEffect(() => {
    async function resolveCoordinates() {
      setIsResolving(true);
      const startTime = performance.now();

      console.log('[TripMap] ========== MAP COORDINATE RESOLUTION START ==========');
      console.log('[TripMap] Itinerary ID:', (itinerary as any)?.id || itinerary?.itineraryId);
      console.log('[TripMap] Total days:', days.length);
      console.log('[TripMap] Destination city:', destinationCity);
      console.log('[TripMap] Using PARALLEL batch processing');

      const allNodes: MapNode[] = [];
      const stats = { total: 0, exact: 0, approximate: 0, city: 0, fallback: 0, filtered: 0 };

      // Collect all nodes to resolve
      const nodesToResolve: Array<{
        node: any;
        day: number;
        nodeTitle: string;
      }> = [];

      for (const day of days) {
        const nodes = day.nodes || day.components || [];
        for (const node of nodes) {
          stats.total++;
          nodesToResolve.push({
            node,
            day: day.dayNumber,
            nodeTitle: node.title || node.name || 'Unknown',
          });
        }
      }

      console.log(`[TripMap] Total nodes to resolve: ${nodesToResolve.length}`);
      setResolutionProgress({ current: 0, total: nodesToResolve.length });

      // Batch process in parallel (5 at a time to avoid rate limiting)
      const BATCH_SIZE = 5;
      let processed = 0;

      for (let i = 0; i < nodesToResolve.length; i += BATCH_SIZE) {
        const batch = nodesToResolve.slice(i, i + BATCH_SIZE);
        const batchStartTime = performance.now();

        // Resolve batch in parallel
        const results = await Promise.all(
          batch.map(({ node }) =>
            coordinateResolver.resolve(node.location, destinationCity)
          )
        );

        const batchTime = performance.now() - batchStartTime;
        console.log(`[TripMap] Batch ${Math.floor(i / BATCH_SIZE) + 1}: Resolved ${batch.length} nodes in ${batchTime.toFixed(0)}ms`);

        // Process results
        batch.forEach(({ node, day, nodeTitle }, index) => {
          const result = results[index];

          // Filter out generic activities with city center coordinates
          if (result.confidence === 'city' && isGenericActivity(nodeTitle)) {
            console.log(`[TripMap]   ⚠️ Filtered generic activity: ${nodeTitle}`);
            stats.filtered++;
            return; // Skip this node
          }

          // Track statistics
          stats[result.confidence]++;

          // Add to nodes array
          allNodes.push({
            id: node.id,
            position: result.coordinates,
            title: nodeTitle,
            type: node.type,
            status: 'planned',
            locked: node.locked || false,
            day,
            confidence: result.confidence,
          });
        });

        processed += batch.length;
        setResolutionProgress({ current: processed, total: nodesToResolve.length });

        // Small delay between batches to avoid rate limiting
        if (i + BATCH_SIZE < nodesToResolve.length) {
          await new Promise(resolve => setTimeout(resolve, 100));
        }
      }

      const totalTime = performance.now() - startTime;
      const avgTimePerNode = totalTime / nodesToResolve.length;

      setNodes(allNodes);
      setResolutionStats(stats);
      setIsResolving(false);

      console.log('[TripMap] ========== COORDINATE RESOLUTION COMPLETE ==========');
      console.log('[TripMap] Total nodes processed:', stats.total);
      console.log('[TripMap] Nodes added to map:', allNodes.length);
      console.log('[TripMap] Generic activities filtered:', stats.filtered);
      console.log('[TripMap] Resolution stats:', stats);
      console.log('[TripMap] Cache stats:', coordinateResolver.getCacheStats());
      console.log('[TripMap] Success rate:', `${((stats.exact + stats.approximate) / stats.total * 100).toFixed(1)}%`);
      console.log('[TripMap] ⚡ Performance:');
      console.log('[TripMap]   Total time: ${totalTime.toFixed(0)}ms');
      console.log('[TripMap]   Avg per node: ${avgTimePerNode.toFixed(0)}ms');
      console.log('[TripMap]   Batch size: ${BATCH_SIZE}');
      console.log('[TripMap]   Speedup: ~${(BATCH_SIZE * 0.7).toFixed(1)}x faster than sequential');
    }

    resolveCoordinates();
  }, [days, destinationCity]);

  // Filter nodes based on selected days
  const filteredNodes = useMemo(() => {
    if (showAllDays) return nodes;
    return nodes.filter(node => selectedDays.has(node.day));
  }, [nodes, selectedDays, showAllDays]);

  // Group nodes by day for route drawing
  const nodesByDay = useMemo(() => {
    const grouped = new Map<number, MapNode[]>();
    filteredNodes.forEach(node => {
      if (!grouped.has(node.day)) {
        grouped.set(node.day, []);
      }
      grouped.get(node.day)!.push(node);
    });
    // Sort nodes within each day by their original sequence
    grouped.forEach(dayNodes => {
      dayNodes.sort((a, b) => {
        const aIndex = nodes.findIndex(n => n.id === a.id);
        const bIndex = nodes.findIndex(n => n.id === b.id);
        return aIndex - bIndex;
      });
    });
    return grouped;
  }, [filteredNodes, nodes]);

  // Initialize map with resolved coordinates
  useEffect(() => {
    if (!api || !mapRef.current || nodes.length === 0 || isResolving) return;

    console.log('[TripMap] ========== MAP INITIALIZATION START ==========');
    console.log('[TripMap] Google Maps API loaded:', !!api);
    console.log('[TripMap] Map container ready:', !!mapRef.current);
    console.log('[TripMap] Total nodes:', nodes.length);
    console.log('[TripMap] Filtered nodes:', filteredNodes.length);
    console.log('[TripMap] Selected days:', Array.from(selectedDays).join(', '));
    console.log('[TripMap] Is resolving:', isResolving);

    // Calculate initial center from filtered nodes
    if (filteredNodes.length === 0) {
      console.log('[TripMap] No filtered nodes to display');
      return;
    }

    const sumLat = filteredNodes.reduce((sum, n) => sum + n.position.lat, 0);
    const sumLng = filteredNodes.reduce((sum, n) => sum + n.position.lng, 0);
    const initialCenter = {
      lat: sumLat / filteredNodes.length,
      lng: sumLng / filteredNodes.length,
    };

    // Initialize or reuse map
    if (!mapInstanceRef.current) {
      mapInstanceRef.current = new api.maps.Map(mapRef.current, {
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
    }

    const googleMap = mapInstanceRef.current;

    // Clear existing markers
    markersRef.current.forEach(marker => marker.setMap(null));
    markersRef.current = [];

    // Clear existing clusterer
    if (clustererRef.current) {
      clustererRef.current.clearMarkers();
      clustererRef.current = null;
    }

    // IMPORTANT: Create markers ONLY from filteredNodes
    const bounds = new api.maps.LatLngBounds();
    const markers = filteredNodes.map((node, index) => {
      // Get day color
      const dayColor = DAY_COLORS[(node.day - 1) % DAY_COLORS.length];
      const isHighlighted = highlightedDay === node.day;

      // Get type info for info window
      const typeInfo = ACTIVITY_TYPES[node.type as keyof typeof ACTIVITY_TYPES] || ACTIVITY_TYPES.attraction;

      // Create clean marker with day color
      const markerIcon = createMarkerIcon(dayColor.primary, isHighlighted);

      const marker = new api.maps.Marker({
        position: node.position,
        title: node.title,
        icon: {
          url: markerIcon,
          scaledSize: new api.maps.Size(isHighlighted ? 44 : 36, isHighlighted ? 44 : 36),
          anchor: new api.maps.Point(isHighlighted ? 22 : 18, isHighlighted ? 22 : 18),
        },
        label: {
          text: `${index + 1}`,
          color: 'white',
          fontSize: isHighlighted ? '12px' : '11px',
          fontWeight: 'bold',
        },
        zIndex: isHighlighted ? 1000 : node.day,
      });

      // Add info window
      const confidenceLabel = {
        exact: '📍 Exact location',
        approximate: '📌 Approximate location',
        city: '🏙️ City center',
        fallback: '⚠️ Fallback location',
      }[node.confidence];

      const infoWindow = new api.maps.InfoWindow({
        content: `
          <div style="padding: 14px; min-width: 240px; font-family: system-ui, -apple-system, sans-serif;">
            <!-- Header -->
            <div style="display: flex; align-items: start; gap: 12px; margin-bottom: 12px;">
              <div style="
                width: 36px;
                height: 36px;
                border-radius: 50%;
                background: ${dayColor.primary};
                display: flex;
                align-items: center;
                justify-content: center;
                font-size: 20px;
                flex-shrink: 0;
              ">
                ${typeInfo.emoji}
              </div>
              <div style="flex: 1; min-width: 0;">
                <h3 style="margin: 0 0 6px 0; font-size: 16px; font-weight: 600; color: #111827; line-height: 1.3;">
                  ${node.title}
                </h3>
                <div style="display: flex; align-items: center; gap: 8px; font-size: 13px;">
                  <span style="
                    display: inline-flex;
                    align-items: center;
                    padding: 3px 10px;
                    border-radius: 14px;
                    background: ${dayColor.light};
                    color: ${dayColor.primary};
                    font-weight: 600;
                    font-size: 12px;
                  ">
                    Day ${node.day}
                  </span>
                  <span style="color: #6b7280;">${typeInfo.label}</span>
                </div>
              </div>
            </div>
            <!-- Confidence indicator -->
            <div style="
              padding: 8px 12px;
              background: #f9fafb;
              border-radius: 8px;
              font-size: 12px;
              color: #6b7280;
              border-left: 3px solid ${dayColor.primary};
            ">
              ${confidenceLabel}
            </div>
          </div>
        `,
      });

      marker.addListener('click', () => {
        infoWindow.open(googleMap, marker);
      });

      bounds.extend(node.position);
      return marker;
    });

    console.log('[TripMap] Created', markers.length, 'markers for filtered nodes');

    // Store markers in ref
    markersRef.current = markers;

    // Add marker clustering for better performance
    clustererRef.current = new MarkerClusterer({ markers, map: googleMap });

    // Clear existing polylines
    polylinesRef.current.forEach(p => p.setMap(null));
    polylinesRef.current = [];

    // Draw day-based route polylines
    nodesByDay.forEach((dayNodes, dayNumber) => {
      // Draw route connecting ALL locations in the day (in sequence)
      // Include all confidence levels to avoid gaps in the route
      if (dayNodes.length < 2) return;

      const dayColor = DAY_COLORS[(dayNumber - 1) % DAY_COLORS.length];
      const isHighlighted = highlightedDay === dayNumber;
      const isFaded = highlightedDay !== null && highlightedDay !== dayNumber;

      const polyline = new api.maps.Polyline({
        path: dayNodes.map(n => n.position),
        geodesic: true,
        strokeColor: dayColor.primary,
        strokeOpacity: isFaded ? 0.2 : isHighlighted ? 1.0 : 0.7,
        strokeWeight: isHighlighted ? 5 : 3,
        map: googleMap,
        zIndex: isHighlighted ? 1000 : dayNumber,
      });

      // Add click handler to highlight day
      polyline.addListener('click', () => {
        setHighlightedDay(dayNumber);
      });

      polylinesRef.current.push(polyline);
    });

    // Fit bounds to show all markers
    if (bounds.isEmpty() === false) {
      googleMap.fitBounds(bounds);
    }

    console.log('[TripMap] Map initialized successfully');
    console.log('[TripMap] Drew', polylinesRef.current.length, 'route polylines');
    console.log('[TripMap] Showing markers for days:', Array.from(selectedDays).join(', '));
  }, [api, filteredNodes, nodesByDay, isResolving, highlightedDay, selectedDays, showAllDays]);

  if (isLoading || isResolving) {
    const progressPercentage = resolutionProgress.total > 0
      ? Math.round((resolutionProgress.current / resolutionProgress.total) * 100)
      : 0;

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
            <Loader2 className="w-12 h-12 mx-auto mb-4 animate-spin text-primary" />
            <p className="font-medium text-gray-900 mb-2">
              {isResolving ? 'Resolving coordinates...' : 'Loading map...'}
            </p>
            {isResolving && resolutionProgress.total > 0 && (
              <>
                <p className="text-sm mb-3">
                  {resolutionProgress.current} of {resolutionProgress.total} locations
                </p>
                {/* Progress bar */}
                <div className="max-w-xs mx-auto">
                  <div className="h-2 bg-gray-200 rounded-full overflow-hidden">
                    <div
                      className="h-full bg-primary transition-all duration-300 ease-out"
                      style={{ width: `${progressPercentage}%` }}
                    />
                  </div>
                  <p className="text-xs mt-2 text-gray-500">
                    {progressPercentage}% complete
                  </p>
                </div>
              </>
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
              Total days: {days.length},
              Total activities: {days.reduce((sum, d) => sum + (d.nodes?.length || 0), 0)}
            </p>
          </div>
        </CardContent>
      </Card>
    );
  }

  return (
    <Card>
      <CardHeader>
        <div className="flex items-center justify-between mb-4">
          <CardTitle className="flex items-center gap-2">
            <Navigation className="w-5 h-5" />
            Trip Map
          </CardTitle>
          <span className="text-sm font-normal text-muted-foreground">
            {filteredNodes.length} of {nodes.length} locations
          </span>
        </div>

        {/* Day Filter Controls */}
        {days.length > 1 && (
          <div className="flex flex-wrap gap-2">
            {/* All Days button - same style as day buttons */}
            <button
              onClick={() => {
                const allDaysSelected = selectedDays.size === days.length;
                if (allDaysSelected) {
                  // Deselect all
                  setSelectedDays(new Set());
                  setShowAllDays(false);
                } else {
                  // Select all
                  setShowAllDays(true);
                  setSelectedDays(new Set(days.map((d: any) => d.dayNumber)));
                }
                setHighlightedDay(null);
              }}
              className={cn(
                "px-3 py-2 rounded-lg text-sm font-medium transition-all",
                "border-2 flex items-center gap-2",
                selectedDays.size === days.length
                  ? "border-primary bg-primary text-white shadow-sm"
                  : "border-gray-300 bg-white hover:border-gray-400 hover:bg-gray-50 text-gray-700"
              )}
            >
              <div className="w-3 h-3 rounded-full bg-gradient-to-r from-red-500 via-blue-500 to-green-500 shadow-sm" />
              <span>All Days</span>
            </button>

            {/* Individual Day Buttons */}
            {days.map((day: any, index: number) => {
              const dayColor = DAY_COLORS[index % DAY_COLORS.length];
              const isSelected = selectedDays.has(day.dayNumber);
              const isHighlighted = highlightedDay === day.dayNumber;
              const dayNodeCount = nodesByDay.get(day.dayNumber)?.length || 0;

              return (
                <button
                  key={day.dayNumber}
                  onClick={() => {
                    setShowAllDays(false);
                    setSelectedDays(prev => {
                      const next = new Set(prev);
                      if (next.has(day.dayNumber)) {
                        next.delete(day.dayNumber);
                        if (highlightedDay === day.dayNumber) {
                          setHighlightedDay(null);
                        }
                      } else {
                        next.add(day.dayNumber);
                      }
                      return next;
                    });
                  }}
                  onMouseEnter={() => setHighlightedDay(day.dayNumber)}
                  onMouseLeave={() => setHighlightedDay(null)}
                  className={cn(
                    "px-3 py-2 rounded-lg text-sm font-medium transition-all",
                    "border-2 flex items-center gap-2",
                    isSelected
                      ? "border-transparent shadow-sm"
                      : "border-gray-200 bg-white hover:border-gray-300",
                    isHighlighted && "ring-2 ring-offset-1 scale-105"
                  )}
                  style={{
                    backgroundColor: isSelected ? dayColor.light : undefined,
                    color: isSelected ? dayColor.primary : undefined,
                    ...(isHighlighted && { '--tw-ring-color': dayColor.primary } as any),
                  }}
                >
                  {/* Color indicator */}
                  <div
                    className="w-3 h-3 rounded-full flex-shrink-0"
                    style={{ backgroundColor: dayColor.primary }}
                  />

                  {/* Day info */}
                  <span>Day {day.dayNumber}</span>
                  {dayNodeCount > 0 && (
                    <span className="text-xs px-1.5 py-0.5 rounded-full bg-white/60">
                      {dayNodeCount}
                    </span>
                  )}
                </button>
              );
            })}
          </div>
        )}
      </CardHeader>

      <CardContent className="p-6">
        {/* Map Container */}
        <div
          ref={mapRef}
          className="w-full h-[500px] rounded-xl overflow-hidden bg-muted shadow-sm border border-gray-200"
        />

        {/* Resolution statistics */}
        {resolutionStats.total > 0 && (
          <div className="mt-4 flex flex-wrap items-center gap-4 text-xs text-muted-foreground">
            <div className="flex items-center gap-1.5">
              <div className="w-3 h-3 rounded-full bg-green-500 shadow-sm" />
              <span>{resolutionStats.exact} exact</span>
            </div>
            <div className="flex items-center gap-1.5">
              <div className="w-3 h-3 rounded-full bg-blue-500 shadow-sm" />
              <span>{resolutionStats.approximate} approximate</span>
            </div>
            <div className="flex items-center gap-1.5">
              <div className="w-3 h-3 rounded-full bg-amber-500 shadow-sm" />
              <span>{resolutionStats.city} city center</span>
            </div>
            {resolutionStats.fallback > 0 && (
              <div className="flex items-center gap-1.5">
                <AlertCircle className="w-3 h-3 text-gray-500" />
                <span>{resolutionStats.fallback} fallback</span>
              </div>
            )}
            {resolutionStats.filtered > 0 && (
              <div className="flex items-center gap-1.5">
                <span className="text-gray-400">•</span>
                <span>{resolutionStats.filtered} generic filtered</span>
              </div>
            )}
          </div>
        )}
      </CardContent>
    </Card>
  );
}
