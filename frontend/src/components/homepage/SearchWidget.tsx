/**
 * Search Widget with Multi-Tab Interface
 * Glass morphism search with 5 travel verticals
 */

import { useState } from 'react';
import { Tabs, TabsList, TabsTrigger, TabsContent } from '@/components/ui/tabs';
import { Card } from '@/components/ui/card';
import { Plane, Hotel, Palmtree, Train, Bus } from 'lucide-react';
import { FlightSearchForm } from './forms/FlightSearchForm';
import { HotelSearchForm } from './forms/HotelSearchForm';

export function SearchWidget() {
  const [activeTab, setActiveTab] = useState('flights');

  return (
    <Card className="glass max-w-4xl mx-auto -mt-20 relative z-20 p-6">
      <Tabs value={activeTab} onValueChange={setActiveTab}>
        <TabsList className="grid grid-cols-5 w-full h-14 bg-white/10 backdrop-blur-sm">
          <TabsTrigger value="flights" className="flex items-center gap-2">
            <Plane className="h-4 w-4" />
            <span className="hidden sm:inline">Flights</span>
          </TabsTrigger>
          <TabsTrigger value="hotels" className="flex items-center gap-2">
            <Hotel className="h-4 w-4" />
            <span className="hidden sm:inline">Hotels</span>
          </TabsTrigger>
          <TabsTrigger value="holidays" className="flex items-center gap-2">
            <Palmtree className="h-4 w-4" />
            <span className="hidden sm:inline">Holidays</span>
          </TabsTrigger>
          <TabsTrigger value="trains" className="flex items-center gap-2">
            <Train className="h-4 w-4" />
            <span className="hidden sm:inline">Trains</span>
          </TabsTrigger>
          <TabsTrigger value="bus" className="flex items-center gap-2">
            <Bus className="h-4 w-4" />
            <span className="hidden sm:inline">Bus</span>
          </TabsTrigger>
        </TabsList>

        <TabsContent value="flights" className="mt-6">
          <FlightSearchForm />
        </TabsContent>

        <TabsContent value="hotels" className="mt-6">
          <HotelSearchForm />
        </TabsContent>

        <TabsContent value="holidays" className="mt-6">
          <div className="text-center py-8 text-muted-foreground">
            Holiday search form coming soon...
          </div>
        </TabsContent>

        <TabsContent value="trains" className="mt-6">
          <div className="text-center py-8 text-muted-foreground">
            Train search form coming soon...
          </div>
        </TabsContent>

        <TabsContent value="bus" className="mt-6">
          <div className="text-center py-8 text-muted-foreground">
            Bus search form coming soon...
          </div>
        </TabsContent>
      </Tabs>
    </Card>
  );
}
