import React, { useState } from 'react';
import { Button } from '../../ui/button';
import { Sheet, SheetContent, SheetHeader, SheetTitle, SheetDescription, SheetTrigger } from '../../ui/sheet';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '../../ui/tabs';
import { Plus, ExternalLink, Plane } from 'lucide-react';
import { TransportPlannerProps } from '../shared/types';

export function TransportPlanner({ destination, tripData, onUpdate }: TransportPlannerProps) {
  const [transportMode, setTransportMode] = useState<'drive' | 'flights' | 'train' | 'bus' | 'ferry'>('drive');

  if (!destination.transport) {
    return null;
  }

  return (
    <Sheet>
      <SheetTrigger asChild>
        <Button variant="outline" size="sm" className="bg-pink-100 border-pink-300 text-pink-700 hover:bg-pink-200">
          {destination.transport.distance}
          <Plus className="w-3 h-3 ml-1" />
        </Button>
      </SheetTrigger>
      <SheetContent side="left" className="w-[400px]">
        <SheetHeader>
          <SheetTitle>
            {destination.name} — Transportation Details
            <div className="text-sm text-gray-500 font-normal">
              {tripData.dates?.start ? new Date(tripData.dates.start).toLocaleDateString('en-US', { day: 'numeric', month: 'long', year: 'numeric' }) : 'TBD'}
            </div>
          </SheetTitle>
          <SheetDescription>
            Plan your transportation and route details for this destination.
          </SheetDescription>
        </SheetHeader>
        <div className="mt-6">
          <Tabs value={transportMode} onValueChange={(value) => setTransportMode(value as any)}>
            <TabsList className="grid grid-cols-5 w-full">
              <TabsTrigger value="drive">Drive</TabsTrigger>
              <TabsTrigger value="flights">Flights</TabsTrigger>
              <TabsTrigger value="train">Train</TabsTrigger>
              <TabsTrigger value="bus">Bus</TabsTrigger>
              <TabsTrigger value="ferry">Ferry</TabsTrigger>
            </TabsList>
            
            <TabsContent value="drive" className="mt-6">
              <div className="space-y-4">
                <Button variant="outline" className="w-full justify-start">
                  <ExternalLink className="w-4 h-4 mr-2" />
                  Rent your car on rentalcars.com
                </Button>
                <div className="space-y-2">
                  <div className="flex items-center justify-between p-3 border rounded-lg">
                    <span>2h 40m — 161 km</span>
                    <Button size="sm">Add to trip</Button>
                  </div>
                  <div className="flex items-center justify-between p-3 border rounded-lg">
                    <span>2h 45m — 171 km</span>
                    <Button size="sm">Add to trip</Button>
                  </div>
                </div>
              </div>
            </TabsContent>
            
            <TabsContent value="flights" className="mt-6">
              <div className="space-y-4">
                <div className="flex items-center justify-between p-3 border rounded-lg">
                  <div className="flex items-center space-x-2">
                    <Plus className="w-4 h-4" />
                    <span>Add your flight</span>
                  </div>
                </div>
                <Button variant="outline" className="w-full justify-start">
                  <ExternalLink className="w-4 h-4 mr-2" />
                  Find flights on Skyscanner
                </Button>
                <div className="text-center py-8 text-gray-500">
                  <Plane className="w-12 h-12 mx-auto mb-2 text-gray-300" />
                  <p className="font-medium">No Plane tickets found</p>
                  <p className="text-sm">Use the + above to add transport to your trip</p>
                </div>
              </div>
            </TabsContent>
          </Tabs>
        </div>
      </SheetContent>
    </Sheet>
  );
}
