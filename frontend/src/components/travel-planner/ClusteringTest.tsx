import React, { useState, useMemo } from 'react';
import { TripMap } from './TripMap';
import type { MapMarker } from '../../types/MapTypes';

// Test component to verify marker clustering works
export const ClusteringTest: React.FC = () => {
  const [markerCount, setMarkerCount] = useState(15);

  // Generate test markers around Gorakhpur, Uttar Pradesh
  const testMarkers = useMemo((): MapMarker[] => {
    const baseLat = 26.7619;
    const baseLng = 83.4050;
    const markers: MapMarker[] = [];

    for (let i = 0; i < markerCount; i++) {
      // Generate random coordinates within a small radius
      const lat = baseLat + (Math.random() - 0.5) * 0.1; // ±0.05 degrees
      const lng = baseLng + (Math.random() - 0.5) * 0.1; // ±0.05 degrees

      const types: MapMarker['type'][] = ['attraction', 'meal', 'accommodation', 'transport'];
      const statuses: MapMarker['status'][] = ['planned', 'in_progress', 'completed', 'skipped'];

      markers.push({
        id: `test-marker-${i}`,
        position: { lat, lng },
        title: `Test Location ${i + 1}`,
        type: types[i % types.length],
        status: statuses[i % statuses.length],
        locked: i % 5 === 0, // Every 5th marker is locked
        rating: Math.random() * 5,
        googleMapsUri: `https://maps.google.com/?q=${lat},${lng}`,
      });
    }

    return markers;
  }, [markerCount]);

  const handleMarkerClick = (nodeId: string) => {
    console.log('Marker clicked:', nodeId);
  };

  return (
    <div className="h-screen w-full flex flex-col">
      {/* Controls */}
      <div className="p-4 bg-gray-100 border-b">
        <div className="flex items-center gap-4">
          <label className="text-sm font-medium">
            Number of Markers:
            <input
              type="number"
              value={markerCount}
              onChange={(e) => setMarkerCount(parseInt(e.target.value) || 0)}
              className="ml-2 px-2 py-1 border rounded w-20"
              min="1"
              max="100"
            />
          </label>
          <div className="text-sm text-gray-600">
            {markerCount >= 10 ? 'Clustering: ON' : 'Clustering: OFF'}
          </div>
        </div>
      </div>

      {/* Map */}
      <div className="flex-1">
        <TripMap
          itineraryId="clustering-test"
          mapBounds={{
            south: 26.7119,
            west: 83.3550,
            north: 26.8119,
            east: 83.4550,
          }}
          countryCentroid={{ lat: 26.7619, lng: 83.4050 }}
          nodes={testMarkers}
          onMarkerClick={handleMarkerClick}
          className="w-full h-full"
        />
      </div>
    </div>
  );
};

export default ClusteringTest;
