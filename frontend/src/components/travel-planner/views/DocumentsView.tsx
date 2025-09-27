import React from 'react';
import { Button } from '../../ui/button';
import { Card } from '../../ui/card';
import { ViewComponentProps } from '../shared/types';

export function DocumentsView({ tripData }: ViewComponentProps) {
  return (
    <div className="p-4 md:p-6 overflow-y-auto h-full">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between mb-6 gap-4">
        <h2 className="text-xl md:text-2xl font-semibold">Documents & Info</h2>
        <Button className="min-h-[44px]">Add Document</Button>
      </div>
      
      <div className="space-y-4">
        <Card className="p-4">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
            <div className="flex-1 min-w-0">
              <h3 className="font-semibold">Travel Insurance</h3>
              <p className="text-sm text-gray-600">Policy: {tripData.id}-TI</p>
            </div>
            <Button variant="outline" size="sm" className="min-h-[44px] flex-shrink-0">View</Button>
          </div>
        </Card>
        
        <Card className="p-4">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
            <div className="flex-1 min-w-0">
              <h3 className="font-semibold">Flight Tickets</h3>
              <p className="text-sm text-gray-600">Booking: {tripData.id}-FL</p>
            </div>
            <Button variant="outline" size="sm" className="min-h-[44px] flex-shrink-0">View</Button>
          </div>
        </Card>
        
        <Card className="p-4">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
            <div className="flex-1 min-w-0">
              <h3 className="font-semibold">Hotel Reservations</h3>
              <p className="text-sm text-gray-600">{tripData.itinerary?.days?.length || 0} reservations</p>
            </div>
            <Button variant="outline" size="sm" className="min-h-[44px] flex-shrink-0">View</Button>
          </div>
        </Card>
      </div>
    </div>
  );
}
