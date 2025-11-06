/**
 * Trip Map Component
 * Google Maps integration showing trip locations and routes
 * Uses smart coordinate resolution with fallback strategies
 * Task 10: Mobile-optimized map with responsive layout and touch controls
 */

import { useEffect, useRef, useState, useMemo } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { MapPin, Navigation, Loader2, AlertCircle, SlidersHorizontal } from 'lucide-react';
import type { NormalizedItinerary, Coordinates } from '@/types/dto';
import { useGoogleMaps } from '@/hooks/useGoogleMaps';
import { MarkerClusterer } from '@googlemaps/markerclusterer';
import { coordinateResolver } from '@/services/coordinateResolver';
import { DAY_COLORS } from '@/constants/dayColors';
import { ResponsiveModal } from '@/components/ui/responsive-modal';
import { useMediaQuery } from '@/hooks/useMediaQuery';
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
  const isMobile = useMediaQuery('(max-width: 768px)');
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
    failed: 0, // Failed to resolve
  });

  const [failedNodes, setFailedNodes] = useState<Array<{ title: string; day: number }>>([]);

  // Day filtering state
  const [selectedDays, setSelectedDays] = useState<Set<number>>(new Set());
  const [highlightedDay, setHighlightedDay] = useState<number | null>(null);
  const [showAllDays, setShowAllDays] = useState(true);
  const [isFiltersOpen, setIsFiltersOpen] = useState(false);

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
  const itineraryId = (itinerary as any)?.id || (itinerary as any)?.itineraryId;
  const days = useMemo(() => {
    const result = (itinerary as any)?.itinerary?.days || (itinerary as any)?.days || [];
    console.log('[TripMap] Days extracted:', result.length, 'days');
    return result;
  }, [itinerary]);
  
  // Create stable string representation of days to prevent unnecessary re-renders
  const daysKey = useMemo(() => {
    return days.map((d: any) => `${d.dayNumber}-${d.nodes?.length || 0}`).join('|');
  }, [days]);
  
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
  // Only re-run when daysKey changes (not on every itinerary object change)
  useEffect(() => {
    // Skip if no days or already resolved for this key
    if (days.length === 0) {
      setNodes([]);
      setIsResolving(false);
      return;
    }
    
    async function resolveCoordinates() {
      setIsResolving(true);
      const startTime = performance.now();

      console.log('[TripMap] ========== MAP COORDINATE RESOLUTION START ==========');
      console.log('[TripMap] Itinerary ID:', itineraryId);
      console.log('[TripMap] Total days:', days.length);
      console.log('[TripMap] Destination city:', destinationCity);
      console.log('[TripMap] Days key:', daysKey);
      console.log('[TripMap] Using PARALLEL batch processing');

      const allNodes: MapNode[] = [];
      const stats = { total: 0, exact: 0, approximate: 0, city: 0, fallback: 0, filtered: 0, failed: 0 };
      const failed: Array<{ title: string; day: number }> = [];

      // Collect all nodes to resolve
      const nodesToResolve: Array<{
        node: any;
        day: number;
        nodeTitle: string;
      }> = [];

      for (const day of days) {
        const nodes = day.nodes || day.components || [];
        for (const node of nodes) {
          // Skip transport/transit nodes entirely from map
          if (node.type === 'transport' || node.type === 'transit') {
            continue;
          }

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

          // Skip if resolution failed (coordinates are 0,0 or invalid)
          if (!result || !result.coordinates ||
            (Math.abs(result.coordinates.lat) < 0.0001 && Math.abs(result.coordinates.lng) < 0.0001)) {
            console.log(`[TripMap]   ❌ Failed to resolve: ${nodeTitle}`);
            stats.failed++;
            failed.push({ title: nodeTitle, day });
            return; // Skip this node from map
          }

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
      setFailedNodes(failed);
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
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [daysKey, destinationCity, itineraryId]); // Use stable daysKey instead of days array

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
  // Only re-run when necessary dependencies change
  useEffect(() => {
    if (!api || !mapRef.current || nodes.length === 0 || isResolving) {
      console.log('[TripMap] Skipping map initialization:', {
        hasApi: !!api,
        hasMapRef: !!mapRef.current,
        nodesCount: nodes.length,
        isResolving
      });
      return;
    }

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
        fullscreenControl: !isMobile, // Hide fullscreen on mobile
        zoomControl: true,
        zoomControlOptions: {
          position: isMobile ? api.maps.ControlPosition.RIGHT_BOTTOM : api.maps.ControlPosition.RIGHT_CENTER,
        },
        gestureHandling: 'greedy', // Better touch handling on mobile
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
          <div style="padding: 12px; max-width: 280px; min-width: 200px; font-family: system-ui, -apple-system, sans-serif;">
            <!-- Header -->
            <div style="display: flex; align-items: start; gap: 10px; margin-bottom: 10px;">
              <div style="
                width: 32px;
                height: 32px;
                border-radius: 50%;
                background: ${dayColor.primary};
                display: flex;
                align-items: center;
                justify-content: center;
                font-size: 18px;
                flex-shrink: 0;
              ">
                ${typeInfo.emoji}
              </div>
              <div style="flex: 1; min-width: 0;">
                <h3 style="margin: 0 0 6px 0; font-size: 14px; font-weight: 600; color: #111827; line-height: 1.3; word-wrap: break-word;">
                  ${node.title}
                </h3>
                <div style="display: flex; align-items: center; gap: 6px; font-size: 12px; flex-wrap: wrap;">
                  <span style="
                    display: inline-flex;
                    align-items: center;
                    padding: 2px 8px;
                    border-radius: 12px;
                    background: ${dayColor.light};
                    color: ${dayColor.primary};
                    font-weight: 600;
                    font-size: 11px;
                  ">
                    Day ${node.day}
                  </span>
                  <span style="color: #6b7280; font-size: 11px;">${typeInfo.label}</span>
                </div>
              </div>
            </div>
            <!-- Confidence indicator -->
            <div style="
              padding: 6px 10px;
              background: #f9fafb;
              border-radius: 6px;
              font-size: 11px;
              color: #6b7280;
              border-left: 3px solid ${dayColor.primary};
            ">
              ${confidenceLabel}
            </div>
          </div>
        `,
        maxWidth: 280,
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

  if (nodes.length === 0 && !isResolving) {
    return (
      <Card>
        <CardContent>
          <div className="text-center py-12 text-muted-foreground">
            <MapPin className="w-12 h-12 mx-auto mb-4 opacity-50" />
            <p>No locations to display on map</p>
            <p className="text-xs mt-2">
              {days.length > 0 ? (
                <>
                  Total days: {days.length},
                  Total activities: {days.reduce((sum: number, d: any) => sum + (d.nodes?.length || 0), 0)}
                </>
              ) : (
                'Waiting for itinerary data...'
              )}
            </p>
          </div>
        </CardContent>
      </Card>
    );
  }

  // Day filter content component (shared between mobile and desktop)
  const DayFiltersContent = () => (
    <div className="space-y-3">
      {!isMobile && (
        <div className="mb-4">
          <h3 className="text-sm font-semibold text-gray-900 mb-1">Filter by Day</h3>
          <p className="text-xs text-gray-600">
            {filteredNodes.length} of {nodes.length} locations
          </p>
        </div>
      )}

      {/* All Days button */}
      <button
        onClick={() => {
          const allDaysSelected = selectedDays.size === days.length;
          if (allDaysSelected) {
            setSelectedDays(new Set());
            setShowAllDays(false);
          } else {
            setShowAllDays(true);
            setSelectedDays(new Set(days.map((d: any) => d.dayNumber)));
          }
          setHighlightedDay(null);
          if (isMobile) setIsFiltersOpen(false);
        }}
        className={cn(
          "w-full px-4 py-3 min-h-[48px] rounded-lg text-sm font-medium transition-all touch-manipulation active:scale-95",
          "border-2 flex items-center gap-3",
          selectedDays.size === days.length
            ? "border-primary bg-primary text-white shadow-md"
            : "border-gray-300 bg-white hover:border-gray-400 hover:bg-gray-50 text-gray-700"
        )}
      >
        <div className="w-4 h-4 rounded-full bg-gradient-to-r from-red-500 via-blue-500 to-green-500 shadow-sm flex-shrink-0" />
        <span>All Days</span>
      </button>

      {/* Individual Day Buttons - Vertical Stack */}
      <div className="space-y-2">
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
              onMouseEnter={() => !isMobile && setHighlightedDay(day.dayNumber)}
              onMouseLeave={() => !isMobile && setHighlightedDay(null)}
              className={cn(
                "w-full px-4 py-3 min-h-[48px] rounded-lg text-sm font-medium transition-all touch-manipulation active:scale-95",
                "border-2 flex items-center justify-between gap-3",
                isSelected
                  ? "border-transparent shadow-md"
                  : "border-gray-200 bg-white hover:border-gray-300 hover:shadow-sm",
                isHighlighted && "ring-2 ring-offset-1 scale-[1.02]"
              )}
              style={{
                backgroundColor: isSelected ? dayColor.light : undefined,
                color: isSelected ? dayColor.primary : undefined,
                ...(isHighlighted && { '--tw-ring-color': dayColor.primary } as any),
              }}
            >
              <div className="flex items-center gap-3 flex-1 min-w-0">
                {/* Color indicator */}
                <div
                  className="w-4 h-4 rounded-full flex-shrink-0 shadow-sm"
                  style={{ backgroundColor: dayColor.primary }}
                />

                <span className="truncate">Day {day.dayNumber}</span>
              </div>

              {/* Activity count badge */}
              {dayNodeCount > 0 && (
                <span className="text-xs px-2 py-1 rounded-full bg-white/80 font-semibold flex-shrink-0">
                  {dayNodeCount}
                </span>
              )}
            </button>
          );
        })}
      </div>
    </div>
  );

  return (
    <div className="h-full flex flex-col md:flex-row gap-3 md:gap-4">
      {/* Mobile: Filter Button */}
      {isMobile && days.length > 1 && (
        <div className="flex-shrink-0">
          <Button
            onClick={() => setIsFiltersOpen(true)}
            variant="outline"
            className="w-full min-h-[48px] touch-manipulation active:scale-95 transition-transform"
          >
            <SlidersHorizontal className="w-4 h-4 mr-2" />
            Day Filters
            {selectedDays.size < days.length && (
              <Badge variant="secondary" className="ml-2">
                {selectedDays.size}/{days.length}
              </Badge>
            )}
          </Button>

          <ResponsiveModal
            open={isFiltersOpen}
            onOpenChange={setIsFiltersOpen}
            title="Filter by Day"
            description={`${filteredNodes.length} of ${nodes.length} locations`}
          >
            <DayFiltersContent />
          </ResponsiveModal>
        </div>
      )}

      {/* Desktop: Left Sidebar - Day Filters */}
      {!isMobile && days.length > 1 && (
        <div className="w-64 flex-shrink-0">
          <DayFiltersContent />
        </div>
      )}

      {/* Map Container */}
      <Card className="flex-1 flex flex-col">
        <CardContent className="p-2 sm:p-3 flex-1 flex flex-col">
          {/* Map */}
          <div
            ref={mapRef}
            className="w-full flex-1 h-64 sm:h-80 md:h-96 lg:h-[500px] rounded-lg md:rounded-xl overflow-hidden bg-muted shadow-sm border border-gray-200 touch-manipulation"
          />

          {/* Resolution statistics */}
          {resolutionStats.total > 0 && (
            <div className="mt-3 md:mt-4 space-y-3">
              <div className="flex flex-wrap items-center gap-2 sm:gap-4 text-xs text-muted-foreground">
                <div className="flex items-center gap-1.5">
                  <div className="w-3 h-3 rounded-full bg-green-500 shadow-sm" />
                  <span className="text-xs sm:text-sm">{resolutionStats.exact} exact</span>
                </div>
                <div className="flex items-center gap-1.5">
                  <div className="w-3 h-3 rounded-full bg-blue-500 shadow-sm" />
                  <span className="text-xs sm:text-sm">{resolutionStats.approximate} approx</span>
                </div>
                <div className="flex items-center gap-1.5">
                  <div className="w-3 h-3 rounded-full bg-amber-500 shadow-sm" />
                  <span className="text-xs sm:text-sm">{resolutionStats.city} city</span>
                </div>
                {resolutionStats.fallback > 0 && (
                  <div className="flex items-center gap-1.5">
                    <AlertCircle className="w-3 h-3 text-gray-500" />
                    <span className="text-xs sm:text-sm">{resolutionStats.fallback} fallback</span>
                  </div>
                )}
                {resolutionStats.filtered > 0 && (
                  <div className="flex items-center gap-1.5">
                    <span className="text-gray-400">•</span>
                    <span className="text-xs sm:text-sm">{resolutionStats.filtered} filtered</span>
                  </div>
                )}
              </div>

              {/* Failed nodes section */}
              {failedNodes.length > 0 && (
                <div className="p-2 sm:p-3 bg-amber-50 border border-amber-200 rounded-lg">
                  <div className="flex items-start gap-2">
                    <AlertCircle className="w-4 h-4 text-amber-600 flex-shrink-0 mt-0.5" />
                    <div className="flex-1 min-w-0">
                      <p className="text-xs sm:text-sm font-semibold text-amber-900 mb-1">
                        Locations not shown ({failedNodes.length})
                      </p>
                      <div className="space-y-1 max-h-32 overflow-y-auto">
                        {failedNodes.map((node, idx) => (
                          <div key={idx} className="text-xs text-amber-700">
                            <span className="font-medium">Day {node.day}:</span> {node.title}
                          </div>
                        ))}
                      </div>
                      <p className="text-xs text-amber-600 mt-2 hidden sm:block">
                        These locations couldn't be resolved to map coordinates.
                      </p>
                    </div>
                  </div>
                </div>
              )}
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
