/**
 * Plan Tab - Destinations & Day-by-Day View
 * Task 26: Enhanced with destinations list, collapsible day cards, and place photos
 */

import { useState } from 'react';
import { Tabs, TabsList, TabsTrigger, TabsContent } from '@/components/ui/tabs';
import { Card, CardContent } from '@/components/ui/card';
import { DayCard } from '@/components/trip/DayCard';
import { PlacePhotos } from '@/components/places/PlacePhotos';
import { TripMap } from '@/components/map/TripMap';
import {
  MapPin,
  Map as MapIcon,
} from 'lucide-react';
import { cn } from '@/lib/utils';

interface PlanTabProps {
  itinerary: any; // NormalizedItinerary type
}

export function PlanTab({ itinerary }: PlanTabProps) {
  const [selectedDestination, setSelectedDestination] = useState(0);
  const [subTab, setSubTab] = useState('destinations');
  const [expandedDay, setExpandedDay] = useState<number | null>(null);

  // Group nodes by location
  const destinations = itinerary.days.reduce((acc: any[], day: any) => {
    const location = day.location;
    const existing = acc.find(d => d.name === location);
    
    if (existing) {
      existing.days.push(day);
    } else {
      acc.push({
        name: location,
        days: [day],
        image: 'https://images.unsplash.com/photo-1502602898657-3e91760cbb34?w=400',
      });
    }
    
    return acc;
  }, []);

  return (
    <div className="space-y-6">
      <Tabs value={subTab} onValueChange={setSubTab}>
        <TabsList>
          <TabsTrigger value="destinations">Destinations</TabsTrigger>
          <TabsTrigger value="day-by-day">Day by Day</TabsTrigger>
        </TabsList>

        {/* Destinations View */}
        <TabsContent value="destinations" className="mt-6">
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
            {/* Destination List */}
            <div className="lg:col-span-1 space-y-3">
              <h3 className="text-lg font-semibold mb-4">Destinations</h3>
              {destinations.map((dest: any, index: number) => (
                <Card
                  key={index}
                  className={cn(
                    'cursor-pointer transition-all hover:shadow-md',
                    selectedDestination === index && 'ring-2 ring-primary'
                  )}
                  onClick={() => setSelectedDestination(index)}
                >
                  <CardContent className="p-4">
                    <div className="flex gap-3">
                      <img
                        src={dest.image}
                        alt={dest.name}
                        className="w-16 h-16 rounded-lg object-cover"
                      />
                      <div className="flex-1">
                        <h4 className="font-semibold">{dest.name}</h4>
                        <p className="text-sm text-muted-foreground">
                          {dest.days.length} {dest.days.length === 1 ? 'day' : 'days'}
                        </p>
                      </div>
                    </div>
                  </CardContent>
                </Card>
              ))}
            </div>

            {/* Trip Map */}
            <div className="lg:col-span-2">
              <TripMap itinerary={itinerary} />
            </div>
          </div>
        </TabsContent>

        {/* Day by Day View */}
        <TabsContent value="day-by-day" className="mt-6">
          <div className="space-y-4">
            {itinerary.days.map((day: any, dayIndex: number) => (
              <DayCard
                key={dayIndex}
                day={day}
                isExpanded={expandedDay === dayIndex}
                onToggle={() => setExpandedDay(expandedDay === dayIndex ? null : dayIndex)}
              />
            ))}
          </div>
        </TabsContent>
      </Tabs>
    </div>
  );
}
