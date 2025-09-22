import React from 'react';
import { Button } from '../../ui/button';
import { Card } from '../../ui/card';
import { ViewComponentProps } from '../shared/types';

export function DocumentsView({ tripData }: ViewComponentProps) {
  return (
    <div className="p-6 overflow-y-auto h-full">
      <div className="flex items-center justify-between mb-6">
        <h2 className="text-2xl font-semibold">Documents & Info</h2>
        <Button>Add Document</Button>
      </div>
      
      <div className="space-y-4">
        <Card className="p-4">
          <div className="flex items-center justify-between">
            <div>
              <h3 className="font-semibold">Travel Insurance</h3>
              <p className="text-sm text-gray-600">Policy: {tripData.id}-TI</p>
            </div>
            <Button variant="outline" size="sm">View</Button>
          </div>
        </Card>
        
        <Card className="p-4">
          <div className="flex items-center justify-between">
            <div>
              <h3 className="font-semibold">Flight Tickets</h3>
              <p className="text-sm text-gray-600">Booking: {tripData.id}-FL</p>
            </div>
            <Button variant="outline" size="sm">View</Button>
          </div>
        </Card>
        
        <Card className="p-4">
          <div className="flex items-center justify-between">
            <div>
              <h3 className="font-semibold">Hotel Reservations</h3>
              <p className="text-sm text-gray-600">{tripData.itinerary?.days?.length || 0} reservations</p>
            </div>
            <Button variant="outline" size="sm">View</Button>
          </div>
        </Card>
      </div>
    </div>
  );
}
