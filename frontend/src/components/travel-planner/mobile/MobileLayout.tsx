import React, { useState, useEffect } from 'react';
import { MobilePlanView } from './MobilePlanView';
import { MobilePlanDetailView } from './MobilePlanDetailView';
import { MobileMapDetailView } from './MobileMapDetailView';
import { MobileChatDetailView } from './MobileChatDetailView';
import { TripData } from '../../../types/TripData';
import { Destination } from '../shared/types';
import type { MapMarker } from '../../../types/MapTypes';

interface MobileLayoutProps {
  tripData: TripData;
  destinations: Destination[];
  mapMarkers: MapMarker[];
  currency: string;
  showNotes: boolean;
  onUpdateDestination: (id: string, updates: Partial<Destination>) => void;
  onAddDestination: (destination: Omit<Destination, 'id'>) => void;
  onRemoveDestination: (id: string) => void;
  onCurrencyChange: (currency: string) => void;
  onToggleNotes: () => void;
  onUpdateTransport: (fromId: string, toId: string, transports: any[]) => void;
  onDaySelect: (dayNumber: number, dayData: any) => void;
  onAddPlace: (data: { dayId: string; dayNumber: number; place: any }) => void;
  onItineraryUpdate: (updatedItinerary: any) => void;
  onViewChange: (view: string) => void;
  activeView: string;
}

export function MobileLayout({
  tripData,
  destinations,
  mapMarkers,
  currency,
  showNotes,
  onUpdateDestination,
  onAddDestination,
  onRemoveDestination,
  onCurrencyChange,
  onToggleNotes,
  onUpdateTransport,
  onDaySelect,
  onAddPlace,
  onItineraryUpdate,
  onViewChange,
  activeView,
}: MobileLayoutProps) {
  const [currentCard, setCurrentCard] = useState<'plan' | 'map' | 'chat' | null>(null);

  // Reset currentCard when activeView changes
  useEffect(() => {
    console.log('MobileLayout: activeView changed to', activeView);
    setCurrentCard(null);
  }, [activeView]);

  // Handle card selection from the 3-card view
  const handleCardSelect = (cardType: 'plan' | 'map' | 'chat') => {
    console.log('MobileLayout: card selected', cardType);
    setCurrentCard(cardType);
  };

  // Handle back navigation
  const handleBack = () => {
    console.log('MobileLayout: back navigation');
    setCurrentCard(null);
  };

  console.log('MobileLayout render:', { activeView, currentCard });

  // Show 3-card selection view when no card is selected
  if (activeView === 'plan' && !currentCard) {
    return (
      <div className="h-full">
        <MobilePlanView
          onCardSelect={handleCardSelect}
          onBack={() => onViewChange('view')}
        />
      </div>
    );
  }

  // Show detail views when a card is selected
  if (activeView === 'plan' && currentCard) {
    return (
      <div className="h-full flex flex-col">
        {currentCard === 'plan' && (
          <MobilePlanDetailView
            tripData={tripData}
            destinations={destinations}
            onUpdateDestination={onUpdateDestination}
            onAddDestination={onAddDestination}
            onRemoveDestination={onRemoveDestination}
            onCurrencyChange={onCurrencyChange}
            onToggleNotes={onToggleNotes}
            onUpdateTransport={onUpdateTransport}
            onDaySelect={onDaySelect}
            onBack={handleBack}
            currency={currency}
            showNotes={showNotes}
          />
        )}
        
        {currentCard === 'map' && (
          <MobileMapDetailView
            tripData={tripData}
            mapMarkers={mapMarkers}
            onAddPlace={onAddPlace}
            onBack={handleBack}
          />
        )}
        
        {currentCard === 'chat' && (
          <MobileChatDetailView
            itineraryId={tripData.id}
            onItineraryUpdate={onItineraryUpdate}
            onBack={handleBack}
          />
        )}
      </div>
    );
  }

  // This should not be reached since MobileLayout is only used for plan tab
  return null;
}
