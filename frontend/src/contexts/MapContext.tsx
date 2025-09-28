import React, { createContext, useContext, useState, useCallback, ReactNode } from 'react';

export type MapViewMode = 'destinations' | 'day-by-day' | 'workflow';

export interface MapCoordinates {
  lat: number;
  lng: number;
}

export interface MapContextType {
  // View state
  viewMode: MapViewMode;
  setViewMode: (mode: MapViewMode) => void;
  
  // Map center and zoom
  center: MapCoordinates | null;
  setCenter: (center: MapCoordinates | null) => void;
  zoom: number;
  setZoom: (zoom: number) => void;
  
  // Selection state
  selectedNodeId: string | null;
  setSelectedNode: (nodeId: string | null) => void;
  selectedDay: number | null;
  setSelectedDay: (day: number | null) => void;
  
  // Highlighting
  highlightedMarkers: string[];
  setHighlightedMarkers: (markers: string[]) => void;
  addHighlightedMarker: (markerId: string) => void;
  removeHighlightedMarker: (markerId: string) => void;
  clearHighlightedMarkers: () => void;
  
  // Hover state
  hoveredCard: { dayNumber: number; componentId: string } | null;
  setHoveredCard: (card: { dayNumber: number; componentId: string } | null) => void;
  
  // Map bounds
  bounds: { north: number; south: number; east: number; west: number } | null;
  setBounds: (bounds: { north: number; south: number; east: number; west: number } | null) => void;
  
  // Utility functions
  resetMapState: () => void;
  centerOnCoordinates: (coordinates: MapCoordinates, zoom?: number) => void;
  centerOnFirstDestination: (destinations: Array<{ lat: number; lng: number }>) => void;
  centerOnDayComponent: (dayNumber: number, componentId: string, coordinates: MapCoordinates) => void;
}

const MapContext = createContext<MapContextType | undefined>(undefined);

interface MapProviderProps {
  children: ReactNode;
}

export const MapProvider: React.FC<MapProviderProps> = ({ children }) => {
  // View state
  const [viewMode, setViewMode] = useState<MapViewMode>('destinations');
  
  // Map center and zoom
  const [center, setCenter] = useState<MapCoordinates | null>(null);
  const [zoom, setZoom] = useState<number>(10);
  
  // Selection state
  const [selectedNodeId, setSelectedNode] = useState<string | null>(null);
  const [selectedDay, setSelectedDay] = useState<number | null>(null);
  
  // Highlighting
  const [highlightedMarkers, setHighlightedMarkers] = useState<string[]>([]);
  
  // Hover state
  const [hoveredCard, setHoveredCard] = useState<{ dayNumber: number; componentId: string } | null>(null);
  
  // Map bounds
  const [bounds, setBounds] = useState<{ north: number; south: number; east: number; west: number } | null>(null);

  // Utility functions
  const addHighlightedMarker = useCallback((markerId: string) => {
    setHighlightedMarkers(prev => 
      prev.includes(markerId) ? prev : [...prev, markerId]
    );
  }, []);

  const removeHighlightedMarker = useCallback((markerId: string) => {
    setHighlightedMarkers(prev => prev.filter(id => id !== markerId));
  }, []);

  const clearHighlightedMarkers = useCallback(() => {
    setHighlightedMarkers([]);
  }, []);

  const resetMapState = useCallback(() => {
    setViewMode('destinations');
    setCenter(null);
    setZoom(10);
    setSelectedNode(null);
    setSelectedDay(null);
    setHighlightedMarkers([]);
    setHoveredCard(null);
    setBounds(null);
  }, []);

  const centerOnCoordinates = useCallback((coordinates: MapCoordinates, zoomLevel?: number) => {
    setCenter(coordinates);
    if (zoomLevel !== undefined) {
      setZoom(zoomLevel);
    }
  }, []);

  const centerOnFirstDestination = useCallback((destinations: Array<{ lat: number; lng: number }>) => {
    if (destinations.length > 0) {
      const firstDestination = destinations[0];
      setCenter({ lat: firstDestination.lat, lng: firstDestination.lng });
      setZoom(12); // Zoom in on city level
    }
  }, []);

  const centerOnDayComponent = useCallback((dayNumber: number, componentId: string, coordinates: MapCoordinates) => {
    setCenter(coordinates);
    setSelectedDay(dayNumber);
    setSelectedNode(componentId);
    setViewMode('day-by-day');
    setZoom(15); // Zoom in on specific location
  }, []);

  const contextValue: MapContextType = {
    // View state
    viewMode,
    setViewMode,
    
    // Map center and zoom
    center,
    setCenter,
    zoom,
    setZoom,
    
    // Selection state
    selectedNodeId,
    setSelectedNode,
    selectedDay,
    setSelectedDay,
    
    // Highlighting
    highlightedMarkers,
    setHighlightedMarkers,
    addHighlightedMarker,
    removeHighlightedMarker,
    clearHighlightedMarkers,
    
    // Hover state
    hoveredCard,
    setHoveredCard,
    
    // Map bounds
    bounds,
    setBounds,
    
    // Utility functions
    resetMapState,
    centerOnCoordinates,
    centerOnFirstDestination,
    centerOnDayComponent,
  };

  return (
    <MapContext.Provider value={contextValue}>
      {children}
    </MapContext.Provider>
  );
};

export const useMapContext = (): MapContextType => {
  const context = useContext(MapContext);
  if (context === undefined) {
    throw new Error('useMapContext must be used within a MapProvider');
  }
  return context;
};

// Hook for map view mode management
export const useMapViewMode = () => {
  const { viewMode, setViewMode } = useMapContext();
  
  const switchToDestinations = useCallback(() => {
    setViewMode('destinations');
  }, [setViewMode]);
  
  const switchToDayByDay = useCallback(() => {
    setViewMode('day-by-day');
  }, [setViewMode]);
  
  const switchToWorkflow = useCallback(() => {
    setViewMode('workflow');
  }, [setViewMode]);
  
  return {
    viewMode,
    setViewMode,
    switchToDestinations,
    switchToDayByDay,
    switchToWorkflow,
  };
};

// Hook for map selection management
export const useMapSelection = () => {
  const { 
    selectedNodeId, 
    setSelectedNode, 
    selectedDay, 
    setSelectedDay,
    highlightedMarkers,
    addHighlightedMarker,
    removeHighlightedMarker,
    clearHighlightedMarkers
  } = useMapContext();
  
  const selectNode = useCallback((nodeId: string) => {
    setSelectedNode(nodeId);
    addHighlightedMarker(nodeId);
  }, [setSelectedNode, addHighlightedMarker]);
  
  const deselectNode = useCallback(() => {
    if (selectedNodeId) {
      removeHighlightedMarker(selectedNodeId);
    }
    setSelectedNode(null);
  }, [selectedNodeId, removeHighlightedMarker, setSelectedNode]);
  
  const selectDay = useCallback((day: number) => {
    setSelectedDay(day);
  }, [setSelectedDay]);
  
  const clearSelection = useCallback(() => {
    setSelectedNode(null);
    setSelectedDay(null);
    clearHighlightedMarkers();
  }, [setSelectedNode, setSelectedDay, clearHighlightedMarkers]);
  
  return {
    selectedNodeId,
    selectedDay,
    highlightedMarkers,
    selectNode,
    deselectNode,
    selectDay,
    clearSelection,
    addHighlightedMarker,
    removeHighlightedMarker,
    clearHighlightedMarkers,
  };
};
