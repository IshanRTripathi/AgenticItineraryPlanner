import React from 'react';
import { TripData } from '../../types/TripData';
import { AgentProgressModal } from './AgentProgressModal';

interface GeneratingPlanProps {
  tripData: TripData;
  onComplete: (itinerary: any) => void;
  onCancel: () => void;
}

export function GeneratingPlan({ tripData, onComplete }: GeneratingPlanProps) {
  return (
    <div className="min-h-screen bg-gray-50 p-4">
      <AgentProgressModal
        tripData={tripData}
        onComplete={() => onComplete(null)}
      />
    </div>
  );
}



