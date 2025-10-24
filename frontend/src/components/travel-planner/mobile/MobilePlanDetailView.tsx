import React, { useState } from 'react';
import { Button } from '../../ui/button';
import { ArrowLeft, RefreshCw } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { DestinationsManager } from '../views/DestinationsManager';
import { DayByDayView } from '../views/DayByDayView';
import { UnifiedItineraryProvider } from '../../../contexts/UnifiedItineraryContext';
import { useMapState } from '../../../hooks/useMapState';
import { TripData } from '../../../types/TripData';
import { Destination } from '../shared/types';

interface MobilePlanDetailViewProps {
  tripData: TripData;
  destinations: Destination[];
  onUpdateDestination: (id: string, updates: Partial<Destination>) => void;
  onAddDestination: (destination: Omit<Destination, 'id'>) => void;
  onRemoveDestination: (id: string) => void;
  onCurrencyChange: (currency: string) => void;
  onToggleNotes: () => void;
  onUpdateTransport: (fromId: string, toId: string, transports: any[]) => void;
  onDaySelect: (dayNumber: number, dayData: any) => void;
  onBack: () => void;
  currency: string;
  showNotes: boolean;
}

export function MobilePlanDetailView({
  tripData,
  destinations,
  onUpdateDestination,
  onAddDestination,
  onRemoveDestination,
  onCurrencyChange,
  onToggleNotes,
  onUpdateTransport,
  onDaySelect,
  onBack,
  currency,
  showNotes,
}: MobilePlanDetailViewProps) {
  const { t } = useTranslation();
  const [activeView, setActiveView] = useState<'destinations' | 'day-by-day'>('destinations');
  const mapState = useMapState();

  const handleSwitch = () => {
    setActiveView(activeView === 'destinations' ? 'day-by-day' : 'destinations');
  };

  const handleRefresh = () => {
    window.location.reload();
  };

  // Show empty state if no destinations
  if (destinations.length === 0) {
    return (
      <div className="h-full flex flex-col bg-gray-50">
        {/* Header */}
        <div className="bg-white border-b border-gray-200 px-4 py-3 flex items-center justify-between">
          <Button
            variant="ghost"
            size="sm"
            onClick={(e) => {
              e.stopPropagation();
              
              onBack();
            }}
            className="flex items-center space-x-2"
          >
            <ArrowLeft className="w-4 h-4" />
            <span>Back</span>
          </Button>
          <h1 className="text-lg font-semibold">Plan</h1>
          <div className="w-16" /> {/* Spacer for centering */}
        </div>

        {/* Empty State */}
        <div className="flex-1 flex items-center justify-center p-4">
          <div className="text-center">
            <div className="w-16 h-16 bg-gray-100 rounded-full flex items-center justify-center mx-auto mb-4">
              <RefreshCw className="w-8 h-8 text-gray-400" />
            </div>
            <h3 className="text-lg font-semibold text-gray-900 mb-2">No destinations yet</h3>
            <p className="text-gray-600">Your itinerary will appear here once planning is complete.</p>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="h-full flex flex-col bg-gray-50">
      {/* Header */}
      <div className="bg-white border-b border-gray-200 px-4 py-3 flex items-center justify-between">
        <Button
          variant="ghost"
          size="sm"
          onClick={(e) => {
            e.stopPropagation();
            
            onBack();
          }}
          className="flex items-center space-x-2"
        >
          <ArrowLeft className="w-4 h-4" />
          <span>Back</span>
        </Button>
        <h1 className="text-lg font-semibold">Plan</h1>
        <div className="w-16" /> {/* Spacer for centering */}
      </div>

      {/* Content */}
      <div className="flex-1 overflow-hidden flex flex-col">
        {activeView === 'destinations' ? (
          <DestinationsManager
            destinations={destinations}
            currency={currency}
            showNotes={showNotes}
            onUpdate={onUpdateDestination}
            onAdd={onAddDestination}
            onRemove={onRemoveDestination}
            onCurrencyChange={onCurrencyChange}
            onToggleNotes={onToggleNotes}
            onUpdateTransport={onUpdateTransport}
          />
        ) : (
          <UnifiedItineraryProvider itineraryId={tripData.id}>
            <DayByDayView
              tripData={tripData}
              onDaySelect={onDaySelect}
              isCollapsed={false}
              mapState={mapState}
            />
          </UnifiedItineraryProvider>
        )}
      </div>

      {/* Switch Button */}
      <div className="bg-white border-t border-gray-200 p-4">
        <Button
          onClick={handleSwitch}
          className="w-full min-h-[48px] text-base font-medium"
          variant="outline"
        >
          Switch to {activeView === 'destinations' ? 'Day-by-Day' : 'Destinations'}
        </Button>
      </div>
    </div>
  );
}

