import React, { useState } from 'react';
import { Button } from '../../ui/button';
import { ArrowLeft } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { TripMap } from '../TripMap';
import { WorkflowBuilder } from '../../WorkflowBuilder';
import { ChatInterface } from '../../ChatInterface';
import { TripData } from '../../../types/TripData';
import type { MapMarker } from '../../../types/MapTypes';
import MapErrorBoundary from '../MapErrorBoundary';

interface MobileMapDetailViewProps {
  tripData: TripData;
  mapMarkers: MapMarker[];
  onAddPlace: (data: { dayId: string; dayNumber: number; place: any }) => void;
  onBack: () => void;
}

export function MobileMapDetailView({
  tripData,
  mapMarkers,
  onAddPlace,
  onBack,
}: MobileMapDetailViewProps) {
  const { t } = useTranslation();
  const [activeView, setActiveView] = useState<'map' | 'workflow'>('map');

  const handleSwitch = () => {
    setActiveView(activeView === 'map' ? 'workflow' : 'map');
  };

  return (
    <div className="h-full flex flex-col bg-gray-50">
      {/* Header */}
      <div className="bg-white border-b border-gray-200 px-4 py-3 flex items-center justify-between">
        <Button
          variant="ghost"
          size="sm"
          onClick={onBack}
          className="flex items-center space-x-2 min-h-[44px]"
        >
          <ArrowLeft className="w-4 h-4" />
          <span>Back</span>
        </Button>
        <h1 className="text-lg font-semibold">Map</h1>
        <div className="w-16" /> {/* Spacer for centering */}
      </div>

      {/* Content */}
      <div className="flex-1 overflow-hidden">
        {activeView === 'map' ? (
          <div className="h-full relative">
            {/* Map Controls Overlay */}
            <div className="absolute top-4 left-4 z-20">
              <div className="flex space-x-2">
                <Button size="sm" variant="outline" className="min-h-[44px]">
                  Layers
                </Button>
                <Button size="sm" variant="outline" className="min-h-[44px]">
                  Search
                </Button>
              </div>
            </div>
            
            <div className="absolute top-4 right-4 z-20">
              <Button size="sm" variant="outline" className="min-h-[44px]">
                Share
              </Button>
            </div>

            {/* Map */}
            {Boolean((import.meta as any).env?.VITE_GOOGLE_MAPS_BROWSER_KEY) ? (
              <MapErrorBoundary
                onError={(error) => {
                  console.error('Map error:', error);
                }}
              >
                <TripMap
                  itineraryId={tripData.id}
                  mapBounds={tripData.itinerary?.mapBounds}
                  countryCentroid={tripData.itinerary?.countryCentroid}
                  nodes={mapMarkers}
                  days={(tripData.itinerary?.days || []).map((d: any, idx: number) => ({ 
                    id: d.id || `day-${idx+1}`, 
                    dayNumber: d.dayNumber || (idx+1), 
                    date: d.date, 
                    location: d.location 
                  }))}
                  onAddPlace={onAddPlace}
                  className="w-full h-full"
                />
              </MapErrorBoundary>
            ) : (
              <div className="h-full flex items-center justify-center text-gray-500">
                <div className="text-center">
                  <div className="w-16 h-16 bg-gray-100 rounded-full flex items-center justify-center mx-auto mb-4">
                    <span className="text-2xl">🗺️</span>
                  </div>
                  <p className="text-lg font-medium">Interactive Map</p>
                  <p className="text-sm">Set VITE_GOOGLE_MAPS_BROWSER_KEY to enable the map</p>
                </div>
              </div>
            )}
          </div>
        ) : (
          <div className="h-full overflow-hidden relative">
            <WorkflowBuilder
              tripData={tripData}
              embedded={true}
              onSave={(updatedItinerary) => {
                console.log('Workflow saved:', updatedItinerary);
                // TODO: Implement save functionality
              }}
              onCancel={() => {
                console.log('Workflow cancelled');
                setActiveView('map');
              }}
            />
          </div>
        )}
      </div>

      {/* Switch Button */}
      <div className="bg-white border-t border-gray-200 p-4">
        <Button
          onClick={handleSwitch}
          className="w-full min-h-[48px] text-base font-medium"
          variant="outline"
        >
          Switch to {activeView === 'map' ? 'Workflow' : 'Map'}
        </Button>
      </div>
    </div>
  );
}
