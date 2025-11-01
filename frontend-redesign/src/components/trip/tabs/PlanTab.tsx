/**
 * Plan Tab - Destinations & Day-by-Day View
 * Task 26: Enhanced with destinations list, collapsible day cards, and place photos
 */

import { useState } from 'react';
import { Tabs, TabsList, TabsTrigger, TabsContent } from '@/components/ui/tabs';
import { Button } from '@/components/ui/button';
import { DayCard } from '@/components/trip/DayCard';
import { TripMap } from '@/components/map/TripMap';
import { EmptyState } from '@/components/common/EmptyState';
import { useUnifiedItinerary } from '@/contexts/UnifiedItineraryContext';
import { Calendar, Plus, MapPin } from 'lucide-react';
import { cn } from '@/lib/utils';
import { motion } from 'framer-motion';
import { staggerChildren, slideUp } from '@/utils/animations';
import { getDayColor } from '@/constants/dayColors';

interface PlanTabProps {
  itinerary: any; // NormalizedItinerary type
}

export function PlanTab({ itinerary }: PlanTabProps) {
  const { loadItinerary, state } = useUnifiedItinerary();
  const itineraryId = itinerary?.id || itinerary?.itineraryId;
  const isGenerating = itinerary?.status === 'generating' || itinerary?.status === 'planning';
  
  const [subTab, setSubTab] = useState('day-by-day');
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


  


  return (
    <div className="space-y-6">
      {/* Tab Navigation */}
      <Tabs value={subTab} onValueChange={setSubTab}>
        <div className="flex justify-center mb-6">
          <div className="inline-flex gap-3">
            <button
              onClick={() => setSubTab('day-by-day')}
              className={cn(
                "px-6 py-3 rounded-xl text-sm font-medium transition-all duration-200",
                "border flex items-center gap-3",
                subTab === 'day-by-day'
                  ? "border-primary bg-primary text-white shadow-lg shadow-primary/25"
                  : "border-gray-200 bg-white hover:border-primary/50 hover:bg-primary/5 text-gray-700"
              )}
            >
              <Calendar className="w-5 h-5" />
              <span className="font-semibold">Day by Day</span>
            </button>
            <button
              onClick={() => setSubTab('map')}
              className={cn(
                "px-6 py-3 rounded-xl text-sm font-medium transition-all duration-200",
                "border flex items-center gap-3",
                subTab === 'map'
                  ? "border-primary bg-primary text-white shadow-lg shadow-primary/25"
                  : "border-gray-200 bg-white hover:border-primary/50 hover:bg-primary/5 text-gray-700"
              )}
            >
              <MapPin className="w-5 h-5" />
              <span className="font-semibold">Map View</span>
            </button>
          </div>
        </div>

        {/* Day by Day View */}
        <TabsContent value="day-by-day">
          <div className="space-y-6">
            {/* Timeline Header with Overview */}
            <div className="flex items-center justify-between">
              <div>
                <h2 className="text-2xl font-bold">Your Itinerary</h2>
                <p className="text-sm text-muted-foreground mt-1">
                  {mappedDays.length} days • {mappedDays.reduce((sum: number, d: any) => sum + (d.nodes?.length || 0), 0)} activities
                </p>
              </div>
              
              {/* Quick Actions */}
              <div className="flex items-center gap-2">
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => setExpandedDay(expandedDay === null ? 0 : null)}
                >
                  {expandedDay === null ? 'Expand All' : 'Collapse All'}
                </Button>
                <Button
                  variant="outline"
                  size="sm"
                  disabled={isGenerating}
                >
                  <Plus className="w-4 h-4 mr-2" />
                  Add Day
                </Button>
              </div>
            </div>

            {/* Progress Indicator */}
            {isRefetching && (
              <motion.div
                initial={{ opacity: 0, y: -10 }}
                animate={{ opacity: 1, y: 0 }}
                className="p-3 bg-blue-50 border border-blue-200 rounded-lg flex items-center gap-2 text-sm text-blue-700"
              >
                <div className="w-4 h-4 border-2 border-blue-600 border-t-transparent rounded-full animate-spin" />
                <span>Updating itinerary...</span>
              </motion.div>
            )}

            {/* Timeline View */}
            <div className="relative">
              {/* Vertical Timeline Line */}
              <div className="absolute left-6 top-0 bottom-0 w-0.5 bg-gradient-to-b from-primary via-primary/50 to-transparent hidden md:block" />
              
              {/* Day Cards */}
              <motion.div 
                className="space-y-6"
                variants={staggerChildren}
                initial="initial"
                animate="animate"
              >
                {mappedDays.map((day: any, dayIndex: number) => {
                  const dayColor = getDayColor(day.dayNumber);
                  return (
                    <motion.div 
                      key={dayIndex} 
                      variants={slideUp}
                      className="relative"
                    >
                      {/* Timeline Dot - Colored to match day */}
                      <div 
                        className="absolute left-6 top-8 w-3 h-3 rounded-full border-4 border-background shadow-lg hidden md:block z-10" 
                        style={{ backgroundColor: dayColor.primary }}
                      />
                      
                      {/* Enhanced Day Card */}
                      <div className="md:ml-16">
                        <DayCard
                          day={day}
                          isExpanded={expandedDay === dayIndex}
                          onToggle={() => setExpandedDay(expandedDay === dayIndex ? null : dayIndex)}
                          itineraryId={itineraryId}
                          enableDragDrop={!isGenerating && !isRefetching}
                          onRefetchNeeded={handleRefetchNeeded}
                          isGenerating={isGenerating}
                        />
                      </div>
                    </motion.div>
                  );
                })}
              </motion.div>
            </div>

            {/* Empty State */}
            {mappedDays.length === 0 && !isGenerating && (
              <EmptyState
                icon={Calendar}
                title="No days planned yet"
                description="Start building your itinerary by adding your first day"
                actionLabel="Add First Day"
                onAction={() => {/* TODO: Add day handler */}}
              />
            )}
          </div>
        </TabsContent>

        {/* Map View - Full viewport */}
        <TabsContent value="map" className="p-0">
          <div className="h-[calc(100vh-16rem)] min-h-[600px]">
            <TripMap itinerary={itinerary} />
          </div>
        </TabsContent>
      </Tabs>
    </div>
  );
}
