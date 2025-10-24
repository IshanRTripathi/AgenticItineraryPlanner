import { useState, useCallback } from 'react';

export interface MapCoordinates {
  lat: number;
  lng: number;
}

export function useMapState() {
  const [center, setCenter] = useState<MapCoordinates | null>(null);
  const [zoom, setZoom] = useState(12);
  const [highlightedMarkers, setHighlightedMarkers] = useState<string[]>([]);
  const [selectedNodeId, setSelectedNode] = useState<string | null>(null);
  const [hoveredCard, setHoveredCard] = useState<{ dayNumber: number; componentId: string } | null>(null);

  const addHighlightedMarker = useCallback((nodeId: string) => {
    setHighlightedMarkers(prev => [...prev, nodeId]);
  }, []);

  const clearHighlightedMarkers = useCallback(() => {
    setHighlightedMarkers([]);
  }, []);

  const centerOnDayComponent = useCallback((
    dayNumber: number,
    componentId: string,
    coordinates: MapCoordinates
  ) => {
    console.log('[useMapState] Centering on component', { dayNumber, componentId, coordinates });
    setCenter(coordinates);
    setZoom(15);
    setSelectedNode(componentId);
  }, []);

  return {
    center,
    zoom,
    highlightedMarkers,
    selectedNodeId,
    hoveredCard,
    viewMode: 'destinations' as const,
    setCenter,
    setZoom,
    setSelectedNode,
    addHighlightedMarker,
    clearHighlightedMarkers,
    centerOnDayComponent,
    setHoveredCard
  };
}
