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
import { useUnifiedItinerary } from '@/contexts/UnifiedItineraryContext';
import {
  MapPin,
  Map as MapIcon,
} from 'lucide-react';
import { cn } from '@/lib/utils';
import { motion } from 'framer-motion';
import { staggerChildren, slideUp } from '@/utils/animations';

interface PlanTabProps {
  itinerary: any; // NormalizedItinerary type
}

export function PlanTab({ itinerary }: PlanTabProps) {
  const { loadItinerary, state } = useUnifiedItinerary();
  const itineraryId = itinerary?.id || itinerary?.itineraryId;
  const isGenerating = itinerary?.status === 'generating' || itinerary?.status === 'planning';
  
  const [selectedDestination, setSelectedDestination] = useState(0);
  const [subTab, setSubTab] = useState('destinations');
  const [expandedDay, setExpandedDay] = useState<number | null>(null);
  const [isRefetching, setIsRefetching] = useState(false);
  
  // Callback to refetch itinerary after reordering
  const handleRefetchNeeded = async () => {
    console.log('[PlanTab] Refetch requested for itinerary:', itineraryId);
    if (itineraryId) {
      setIsRefetching(true);
      try {
        await loadItinerary(itineraryId);
        console.log('[PlanTab] Refetch completed, UI state preserved');
      } finally {
        setIsRefetching(false);
      }
    }
  };

  // Use the itinerary from state (which gets updated by loadItinerary)
  // This ensures we always have the latest data
  const currentItinerary = state.itinerary || itinerary;
  const days = currentItinerary?.itinerary?.days || [];
  
  console.log('[PlanTab] Render - Days count:', days.length);
  console.log('[PlanTab] Render - State itinerary:', state.itinerary?.id);
  console.log('[PlanTab] Render - Prop itinerary:', itinerary?.id);
  console.log('[PlanTab] Render - Using:', currentItinerary?.id);
  
  // Debug: Check data structure
  if (days.length > 0) {
    console.log('[PlanTab] First day structure:', {
      hasNodes: !!days[0].nodes,
      hasComponents: !!days[0].components,
      nodesCount: days[0].nodes?.length || 0,
      componentsCount: days[0].components?.length || 0,
      firstNodeId: days[0].nodes?.[0]?.id,
      firstComponentId: days[0].components?.[0]?.id
    });
  }
  
  // Map to format expected by DayCard
  // Backend returns 'nodes', ensure we use them directly
  const mappedDays = days.map((day: any) => ({
    dayNumber: day.dayNumber,
    date: day.date,
    location: day.location || day.theme,
    // Use nodes directly from backend (already in correct format)
    // Fallback to components for backward compatibility
    nodes: day.nodes || (day.components || []).map((component: any) => ({
      ...component,
      title: component.name || component.title,
      id: component.id,
      type: component.type
    }))
  }));
  
  console.log('[PlanTab] Mapped days:', mappedDays);

  // Group nodes by location
  const destinations = mappedDays.reduce((acc: any[], day: any) => {
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
  
  console.log('[PlanTab] Destinations:', destinations);

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
            <motion.div 
              className="lg:col-span-1 space-y-3"
              variants={staggerChildren}
              initial="initial"
              animate="animate"
            >
              <h3 className="text-lg font-semibold mb-4">Destinations</h3>
              {destinations.map((dest: any, index: number) => (
                <motion.div key={index} variants={slideUp}>
                <Card
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
                </motion.div>
              ))}
            </motion.div>

            {/* Trip Map */}
            <div className="lg:col-span-2">
              <TripMap itinerary={itinerary} />
            </div>
          </div>
        </TabsContent>

        {/* Day by Day View */}
        <TabsContent value="day-by-day" className="mt-6">
          {isRefetching && (
            <div className="mb-4 p-3 bg-blue-50 border border-blue-200 rounded-lg flex items-center gap-2 text-sm text-blue-700">
              <div className="w-4 h-4 border-2 border-blue-600 border-t-transparent rounded-full animate-spin" />
              <span>Updating itinerary...</span>
            </div>
          )}
          <motion.div 
            className="space-y-4"
            variants={staggerChildren}
            initial="initial"
            animate="animate"
          >
            {mappedDays.map((day: any, dayIndex: number) => (
              <motion.div key={dayIndex} variants={slideUp}>
                <DayCard
                  day={day}
                  isExpanded={expandedDay === dayIndex}
                  onToggle={() => setExpandedDay(expandedDay === dayIndex ? null : dayIndex)}
                  itineraryId={itineraryId}
                  enableDragDrop={!isGenerating && !isRefetching}
                  onRefetchNeeded={handleRefetchNeeded}
                  isGenerating={isGenerating}
                />
              </motion.div>
            ))}
          </motion.div>
        </TabsContent>
      </Tabs>
    </div>
  );
}
