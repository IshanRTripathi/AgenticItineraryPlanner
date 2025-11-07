import type { Coordinates } from '../types/dto';

// Map-specific types
export interface MapBounds {
  north: number;
  south: number;
  east: number;
  west: number;
}

export interface MapMarker {
  id: string;
  position: Coordinates;
  type: 'attraction' | 'meal' | 'accommodation' | 'transport';
  status: 'planned' | 'in_progress' | 'completed' | 'skipped' | 'cancelled';
  locked?: boolean;
}

// Google Maps type declarations
declare global {
  interface Window {
    google: any;
  }
}

/**
 * Calculate optimal zoom level for given bounds and viewport
 */
export function calculateOptimalZoom(
  bounds: MapBounds,
  viewport: { width: number; height: number }
): number {
  const latDiff = bounds.north - bounds.south;
  const lngDiff = bounds.east - bounds.west;
  
  const latZoom = Math.log2(360 / latDiff);
  const lngZoom = Math.log2(360 / lngDiff);
  
  // Use the smaller zoom to ensure both dimensions fit
  const zoom = Math.min(latZoom, lngZoom);
  
  // Clamp between min and max zoom levels
  return Math.max(1, Math.min(20, Math.floor(zoom)));
}

/**
 * Animate map to bounds with smooth transition
 */
export function animateToBounds(
  map: any,
  bounds: MapBounds,
  options: { duration?: number; padding?: number } = {}
): Promise<void> {
  return new Promise((resolve) => {
    const { duration = 1000, padding = 48 } = options;
    
    const google = (window as any).google;
    const latLngBounds = new google.maps.LatLngBounds(
      { lat: bounds.south, lng: bounds.west },
      { lat: bounds.north, lng: bounds.east }
    );
    
    map.fitBounds(latLngBounds, { padding });
    
    // Resolve after animation duration
    setTimeout(resolve, duration);
  });
}

/**
 * Create marker icon based on type and status
 */
export function createMarkerIcon(
  type: MapMarker['type'],
  status: MapMarker['status'],
  locked: boolean = false
): string {
  const baseSize = 32;
  const colors = {
    planned: '#3B82F6',      // Blue
    in_progress: '#F59E0B',  // Amber
    completed: '#10B981',    // Green
    skipped: '#6B7280',      // Gray
    cancelled: '#EF4444',    // Red
  };

  const icons: Record<string, string> = {
    attraction: '🎯',
    meal: '🍽️',
    accommodation: '🏨',
    transport: '🚗',
  };

  const color = colors[status] || colors.planned;
  const icon = icons[type] || '📍';

  // Create SVG icon
  return `data:image/svg+xml;charset=UTF-8,${encodeURIComponent(`
    <svg width="${baseSize}" height="${baseSize}" viewBox="0 0 32 32" xmlns="http://www.w3.org/2000/svg">
      <circle cx="16" cy="16" r="14" fill="${color}" stroke="white" stroke-width="2"/>
      <text x="16" y="20" text-anchor="middle" font-size="16" fill="white">${icon}</text>
      ${locked ? '<circle cx="24" cy="8" r="6" fill="#EF4444" stroke="white" stroke-width="1"/><text x="24" y="11" text-anchor="middle" font-size="8" fill="white">🔒</text>' : ''}
    </svg>
  `)}`;
}

/**
 * Validate coordinates
 */
export function validateCoordinates(coords: Coordinates): boolean {
  return (
    typeof coords.lat === 'number' &&
    typeof coords.lng === 'number' &&
    coords.lat >= -90 &&
    coords.lat <= 90 &&
    coords.lng >= -180 &&
    coords.lng <= 180 &&
    !isNaN(coords.lat) &&
    !isNaN(coords.lng)
  );
}

/**
 * Calculate distance between two coordinates (in kilometers)
 */
export function calculateDistance(
  coord1: Coordinates,
  coord2: Coordinates
): number {
  const R = 6371; // Earth's radius in kilometers
  const dLat = toRadians(coord2.lat - coord1.lat);
  const dLng = toRadians(coord2.lng - coord1.lng);
  
  const a =
    Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos(toRadians(coord1.lat)) *
      Math.cos(toRadians(coord2.lat)) *
      Math.sin(dLng / 2) *
      Math.sin(dLng / 2);
  
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  return R * c;
}

/**
 * Convert degrees to radians
 */
function toRadians(degrees: number): number {
  return degrees * (Math.PI / 180);
}

/**
 * Calculate bounds from array of coordinates
 */
export function calculateBoundsFromCoordinates(coords: Coordinates[]): MapBounds | null {
  if (coords.length === 0) return null;
  
  let minLat = coords[0].lat;
  let maxLat = coords[0].lat;
  let minLng = coords[0].lng;
  let maxLng = coords[0].lng;
  
  coords.forEach(coord => {
    minLat = Math.min(minLat, coord.lat);
    maxLat = Math.max(maxLat, coord.lat);
    minLng = Math.min(minLng, coord.lng);
    maxLng = Math.max(maxLng, coord.lng);
  });
  
  return {
    south: minLat,
    west: minLng,
    north: maxLat,
    east: maxLng,
  };
}

/**
 * Calculate centroid from array of coordinates
 */
export function calculateCentroidFromCoordinates(coords: Coordinates[]): Coordinates | null {
  if (coords.length === 0) return null;
  
  const sum = coords.reduce(
    (acc, coord) => ({
      lat: acc.lat + coord.lat,
      lng: acc.lng + coord.lng,
    }),
    { lat: 0, lng: 0 }
  );
  
  return {
    lat: sum.lat / coords.length,
    lng: sum.lng / coords.length,
  };
}

/**
 * Check if a coordinate is within bounds
 */
export function isCoordinateInBounds(
  coord: Coordinates,
  bounds: MapBounds
): boolean {
  return (
    coord.lat >= bounds.south &&
    coord.lat <= bounds.north &&
    coord.lng >= bounds.west &&
    coord.lng <= bounds.east
  );
}

/**
 * Expand bounds by a given factor
 */
export function expandBounds(
  bounds: MapBounds,
  factor: number = 1.1
): MapBounds {
  const latCenter = (bounds.north + bounds.south) / 2;
  const lngCenter = (bounds.east + bounds.west) / 2;
  const latDiff = (bounds.north - bounds.south) * factor;
  const lngDiff = (bounds.east - bounds.west) * factor;
  
  return {
    south: latCenter - latDiff / 2,
    west: lngCenter - lngDiff / 2,
    north: latCenter + latDiff / 2,
    east: lngCenter + lngDiff / 2,
  };
}
