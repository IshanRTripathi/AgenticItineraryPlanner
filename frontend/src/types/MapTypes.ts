// Map-specific types for Google Maps integration

export interface MapConfig {
  center: Coordinates;
  zoom: number;
  mapTypeId: MapTypeId;
  gestureHandling: 'greedy' | 'cooperative' | 'none' | 'auto';
  fullscreenControl: boolean;
  streetViewControl: boolean;
  mapTypeControl: boolean;
  zoomControl: boolean;
}

export interface Coordinates {
  lat: number;
  lng: number;
}

export type MapTypeId = 'roadmap' | 'satellite' | 'hybrid' | 'terrain';

export interface MapMarker {
  id: string;
  position: Coordinates;
  title: string;
  type: 'attraction' | 'meal' | 'accommodation' | 'transport';
  status: 'planned' | 'in_progress' | 'completed' | 'skipped' | 'cancelled';
  locked: boolean;
  rating?: number;
  googleMapsUri?: string;
}

export interface MapBounds {
  south: number;
  west: number;
  north: number;
  east: number;
}

export interface MapCameraOptions {
  center?: Coordinates;
  zoom?: number;
  bounds?: MapBounds;
  padding?: number;
}

export interface MapAnimationOptions {
  duration?: number;
  easing?: 'linear' | 'ease-in' | 'ease-out' | 'ease-in-out';
}

export interface TerrainControlProps {
  currentMapType: MapTypeId;
  onMapTypeChange: (mapType: MapTypeId) => void;
  disabled?: boolean;
}

export interface TripMapProps {
  itineraryId: string;
  mapBounds?: MapBounds;
  countryCentroid?: Coordinates;
  nodes: MapMarker[];
  selectedNodeId?: string;
  selectedDay?: number;
  onMarkerClick?: (nodeId: string) => void;
  onMapReady?: (map: any) => void;
  days?: Array<{ id: string; dayNumber: number; date?: string; location?: string }>;
  onAddPlace?: (args: { 
    dayId: string; 
    dayNumber: number; 
    place: { 
      name: string; 
      lat: number; 
      lng: number; 
      address?: string;
      types?: string[];
      rating?: number;
      userRatingCount?: number;
      phoneNumber?: string;
      mapsLink?: string;
    } 
  }) => void;
  onPlaceSelected?: (place: { name?: string; address?: string; lat: number; lng: number }) => void;
  className?: string;
}

export interface MarkerManagerProps {
  map: any;
  markers: MapMarker[];
  selectedNodeId?: string;
  onMarkerClick?: (nodeId: string) => void;
}

export interface MarkerInfoWindowProps {
  marker: MapMarker;
  onClose?: () => void;
}

export interface MapError {
  code: string;
  message: string;
  details?: string;
}

export interface MapLoadingState {
  isLoading: boolean;
  error?: MapError;
  progress?: number;
}

// Google Maps API types (extending the global google.maps namespace)
// Note: avoid augmenting google.maps types here to prevent TS conflicts in environments without the global types

// Utility types for map operations
export type MapEventType = 
  | 'click'
  | 'dblclick'
  | 'rightclick'
  | 'mousemove'
  | 'mouseover'
  | 'mouseout'
  | 'dragstart'
  | 'drag'
  | 'dragend'
  | 'zoom_changed'
  | 'center_changed'
  | 'bounds_changed'
  | 'idle';

export interface MapEventHandlers {
  onClick?: (event: any) => void;
  onDoubleClick?: (event: any) => void;
  onRightClick?: (event: any) => void;
  onMouseMove?: (event: any) => void;
  onMouseOver?: (event: any) => void;
  onMouseOut?: (event: any) => void;
  onDragStart?: (event: any) => void;
  onDrag?: (event: any) => void;
  onDragEnd?: (event: any) => void;
  onZoomChanged?: () => void;
  onCenterChanged?: () => void;
  onBoundsChanged?: () => void;
  onIdle?: () => void;
}

// Constants for map configuration
export const MAP_CONSTANTS = {
  DEFAULT_ZOOM: 2,
  DEFAULT_CENTER: { lat: 0, lng: 0 } as Coordinates,
  WORLD_BOUNDS: {
    south: -90,
    west: -180,
    north: 90,
    east: 180
  } as MapBounds,
  WORLD_CENTROID: { lat: 0, lng: 0 } as Coordinates,
  MIN_ZOOM: 1,
  MAX_ZOOM: 20,
  ANIMATION_DURATION: 1000,
  MARKER_CLUSTER_SIZE: 200,
  BOUNDS_PADDING: 48,
  CLUSTERING_THRESHOLD: 10, // Use clustering for 10+ markers
  CLUSTER_GRID_SIZE: 60,
  CLUSTER_MAX_ZOOM: 15
} as const;

// Map type options for terrain control
export const MAP_TYPE_OPTIONS: Array<{ value: MapTypeId; label: string; icon: string }> = [
  { value: 'roadmap', label: 'Roadmap', icon: '🗺️' },
  { value: 'terrain', label: 'Terrain', icon: '🏔️' },
  { value: 'satellite', label: 'Satellite', icon: '🛰️' },
  { value: 'hybrid', label: 'Hybrid', icon: '🛰️🗺️' }
] as const;
