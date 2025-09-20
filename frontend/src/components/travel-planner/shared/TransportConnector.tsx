import React, { useState } from 'react';
import { Button } from '../../ui/button';
import { Plus, Navigation, Car, Plane, Train } from 'lucide-react';
import { TransportPopup } from './TransportPopup';

interface TransportConnectorProps {
  fromDestination: string;
  toDestination: string;
  transports?: TransportDetails[];
  onUpdate: (transports: TransportDetails[]) => void;
}

interface TransportDetails {
  id: string;
  mode: 'walk' | 'cab' | 'flight' | 'other';
  distance: string;
  distanceUnit: 'km' | 'mi';
  duration?: string;
  cost?: string;
  notes?: string;
}

const modeIcons = {
  walk: Navigation,
  cab: Car,
  flight: Plane,
  other: Train,
};

const modeColors = {
  walk: 'text-green-600 bg-green-50 border-green-200',
  cab: 'text-yellow-600 bg-yellow-50 border-yellow-200',
  flight: 'text-blue-600 bg-blue-50 border-blue-200',
  other: 'text-gray-600 bg-gray-50 border-gray-200',
};

export function TransportConnector({ 
  fromDestination, 
  toDestination, 
  transports = [], 
  onUpdate 
}: TransportConnectorProps) {
  const [isPopupOpen, setIsPopupOpen] = useState(false);

  const handleSave = (newTransports: TransportDetails[]) => {
    onUpdate(newTransports);
  };

  if (!transports || transports.length === 0) {
    return (
      <>
        <div className="flex justify-center py-6">
          <Button
            variant="outline"
            size="sm"
            onClick={() => setIsPopupOpen(true)}
            className="h-12 px-6 text-sm border-dashed border-gray-300 hover:border-gray-400 hover:bg-gray-50"
          >
            <Plus className="w-4 h-4 mr-2" />
            Add Transport
          </Button>
        </div>
        <TransportPopup
          isOpen={isPopupOpen}
          onClose={() => setIsPopupOpen(false)}
          fromDestination={fromDestination}
          toDestination={toDestination}
          onSave={handleSave}
        />
      </>
    );
  }

  return (
    <>
      <div className="flex justify-center py-6">
        <div className="flex flex-wrap items-center justify-center gap-2">
          {transports.map((transport, index) => {
            const IconComponent = modeIcons[transport.mode];
            const colorClass = modeColors[transport.mode];
            
            return (
              <Button
                key={transport.id}
                variant="outline"
                size="sm"
                onClick={() => setIsPopupOpen(true)}
                className={`h-12 px-4 text-sm ${colorClass} hover:opacity-80`}
              >
                <IconComponent className="w-4 h-4 mr-2" />
                {transport.distance} {transport.distanceUnit}
                {transport.duration && ` • ${transport.duration}`}
              </Button>
            );
          })}
          <Button
            variant="ghost"
            size="sm"
            onClick={() => setIsPopupOpen(true)}
            className="h-12 w-12 p-0 text-gray-400 hover:text-gray-600 hover:bg-gray-100"
            title="Add another transport mode"
          >
            <Plus className="w-4 h-4" />
          </Button>
        </div>
      </div>
      <TransportPopup
        isOpen={isPopupOpen}
        onClose={() => setIsPopupOpen(false)}
        fromDestination={fromDestination}
        toDestination={toDestination}
        onSave={handleSave}
        existingTransports={transports}
      />
    </>
  );
}
