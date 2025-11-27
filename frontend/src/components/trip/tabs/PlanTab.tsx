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
import { Calendar, Plus, MapPin, Hotel, CheckCircle } from 'lucide-react';
import { cn } from '@/lib/utils';
import { motion } from 'framer-motion';
import { staggerChildren, slideUp } from '@/utils/animations';
import { getDayColor } from '@/constants/dayColors';
import { useTranslation } from '@/i18n';
import { useSearchParams } from 'react-router-dom';
import { ValidationAdvice } from '@/components/validation/ValidationAdvice';
import { validationApi, ValidationLevel, ValidationAdvice as ValidationAdviceType } from '@/services/validationApi';

interface PlanTabProps {
  itinerary: any; // NormalizedItinerary type
}

export function PlanTab({ itinerary }: PlanTabProps) {
  const { t } = useTranslation();
  const { loadItinerary, state } = useUnifiedItinerary();
  const [searchParams, setSearchParams] = useSearchParams();
  const itineraryId = itinerary?.id || itinerary?.itineraryId;
  const isGenerating = itinerary?.status === 'generating' || itinerary?.status === 'planning';
  
  const [subTab, setSubTab] = useState('day-by-day');
  const [expandedDay, setExpandedDay] = useState<number | null>(null);
  const [isRefetching, setIsRefetching] = useState(false);
  const [validationAdvice, setValidationAdvice] = useState<ValidationAdviceType | null>(null);
  const [isValidating, setIsValidating] = useState(false);
  
  // Refs for each day card to enable scrolling
  const dayRefs = useState(() => new Map<number, HTMLDivElement>())[0];
  
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
  
  // Validate itinerary
  const handleValidate = async () => {
    if (!itineraryId) return;
    
    setIsValidating(true);
    try {
      const advice = await validationApi.validate(itineraryId, ValidationLevel.STANDARD);
      setValidationAdvice(advice);
    } catch (error) {
      console.error('Validation failed:', error);
    } finally {
      setIsValidating(false);
    }
  };

  // Use the itinerary from state (which gets updated by loadItinerary)
  // This ensures we always have the latest data
  const currentItinerary = state.itinerary || itinerary;
  
  // DEBUG: Log the entire itinerary structure to understand the data
  console.log('[PlanTab] 🔍 FULL ITINERARY OBJECT:', currentItinerary);
  console.log('[PlanTab] 🔍 ITINERARY KEYS:', currentItinerary ? Object.keys(currentItinerary) : 'null');
  
  // Backend returns days at top level, not nested under itinerary.itinerary
  // Try multiple paths for backward compatibility
  const days = currentItinerary?.days || currentItinerary?.itinerary?.days || [];
  
  console.log('[PlanTab] ✅ Days count:', days.length);
  console.log('[PlanTab] ✅ State itinerary:', state.itinerary?.itineraryId);
  console.log('[PlanTab] ✅ Prop itinerary:', itinerary?.id);
  console.log('[PlanTab] ✅ Using:', currentItinerary?.id);
  console.log('[PlanTab] ✅ Days path check:', {
    hasDaysAtRoot: !!currentItinerary?.days,
    hasDaysNested: !!currentItinerary?.itinerary?.days,
    daysLength: days.length,
    firstDay: days[0]
  });
  
  // Debug: Check data structure
  if (days.length > 0) {
    console.log('[PlanTab] 📊 First day structure:', {
      hasNodes: !!days[0].nodes,
      hasComponents: !!days[0].components,
      nodesCount: days[0].nodes?.length || 0,
      componentsCount: days[0].components?.length || 0,
      firstNodeId: days[0].nodes?.[0]?.id,
      firstComponentId: days[0].components?.[0]?.id,
      firstNodeTitle: days[0].nodes?.[0]?.title,
      dayKeys: Object.keys(days[0])
    });
    console.log('[PlanTab] 📊 First day FULL:', days[0]);
  } else {
    console.log('[PlanTab] ⚠️ NO DAYS FOUND - days array is empty!');
  }
  
  // Map to format expected by DayCard
  // Backend returns 'nodes', use them directly
  const mappedDays = days.map((day: any) => ({
    dayNumber: day.dayNumber,
    date: day.date,
    location: day.location || day.theme,
    nodes: day.nodes || []
  }));
  
  console.log('[PlanTab] 🎯 Mapped days (what DayCard receives):', mappedDays);
  console.log('[PlanTab] 🎯 Mapped days count:', mappedDays.length);
  if (mappedDays.length > 0) {
    console.log('[PlanTab] 🎯 First mapped day:', mappedDays[0]);
    console.log('[PlanTab] 🎯 First mapped day nodes count:', mappedDays[0].nodes?.length);
    
    // *** ENRICHMENT DATA VERIFICATION ***
    if (mappedDays[0].nodes && mappedDays[0].nodes.length > 0) {
      const firstNode = mappedDays[0].nodes[0];
      console.log('[PlanTab] 🔍 ENRICHMENT CHECK - First node:', {
        id: firstNode.id,
        title: firstNode.title,
        hasLocation: !!firstNode.location,
        locationKeys: firstNode.location ? Object.keys(firstNode.location) : [],
        photos: firstNode.location?.photos,
        photosCount: firstNode.location?.photos?.length || 0,
        rating: firstNode.location?.rating,
        userRatingsTotal: firstNode.location?.userRatingsTotal,
        priceLevel: firstNode.location?.priceLevel,
        placeId: firstNode.location?.placeId
      });
    }
  }


  


  return (
    <div className="space-y-4 sm:space-y-6">
      {/* Header - Centered Title and Summary - Only for Day by Day view */}
      {subTab === 'day-by-day' && (
        <div className="text-center space-y-2 pb-4 border-b">
          <h2 className="text-xl sm:text-2xl font-bold">{t('components.planTab.yourItinerary')}</h2>
          <p className="text-sm sm:text-base text-muted-foreground">
            {t('components.planTab.summary', { 
              days: mappedDays.length, 
              activities: mappedDays.reduce((sum: number, d: any) => sum + (d.nodes?.length || 0), 0) 
            })}
          </p>
        </div>
      )}

      {/* Tab Navigation - Below Header */}
      <Tabs value={subTab} onValueChange={setSubTab}>
        <div className="flex justify-center mb-4 sm:mb-6">
          <div className="inline-flex gap-2 sm:gap-3">
            <button
              onClick={() => setSubTab('day-by-day')}
              className={cn(
                "px-3 py-2 sm:px-6 sm:py-3 rounded-lg sm:rounded-xl text-xs sm:text-sm font-medium transition-all duration-200",
                "border flex items-center gap-2 sm:gap-3 min-h-[40px] touch-manipulation active:scale-95",
                subTab === 'day-by-day'
                  ? "border-primary bg-primary text-white shadow-lg shadow-primary/25"
                  : "border-gray-200 bg-white hover:border-primary/50 hover:bg-primary/5 text-gray-700"
              )}
            >
              <Calendar className="w-4 h-4 sm:w-5 sm:h-5" />
              <span className="font-semibold">{t('components.planTab.dayByDay')}</span>
            </button>
            <button
              onClick={() => setSubTab('map')}
              className={cn(
                "px-3 py-2 sm:px-6 sm:py-3 rounded-lg sm:rounded-xl text-xs sm:text-sm font-medium transition-all duration-200",
                "border flex items-center gap-2 sm:gap-3 min-h-[40px] touch-manipulation active:scale-95",
                subTab === 'map'
                  ? "border-primary bg-primary text-white shadow-lg shadow-primary/25"
                  : "border-gray-200 bg-white hover:border-primary/50 hover:bg-primary/5 text-gray-700"
              )}
            >
              <MapPin className="w-4 h-4 sm:w-5 sm:h-5" />
              <span className="font-semibold">{t('components.planTab.mapView')}</span>
            </button>
          </div>
        </div>

        {/* Day by Day View */}
        <TabsContent value="day-by-day">
          <div className="space-y-4 sm:space-y-6">
            {/* Quick Actions */}
            <div className="flex items-center gap-2 justify-end flex-wrap">
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => setExpandedDay(expandedDay === null ? 0 : null)}
                  className="flex-1 sm:flex-initial min-h-[36px] text-xs sm:text-sm px-2 sm:px-3 touch-manipulation active:scale-95"
                >
                  {expandedDay === null ? t('components.planTab.expandAll') : t('components.planTab.collapseAll')}
                </Button>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={handleValidate}
                  disabled={isValidating || isGenerating}
                  className="flex-1 sm:flex-initial min-h-[36px] text-xs sm:text-sm px-2 sm:px-3 touch-manipulation active:scale-95 bg-green-50 hover:bg-green-100 border-green-200"
                >
                  <CheckCircle className="w-3 h-3 sm:w-4 sm:h-4 mr-1 sm:mr-2" />
                  {isValidating ? 'Validating...' : 'Validate'}
                </Button>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => setSearchParams({ tab: 'bookings' })}
                  className="flex-1 sm:flex-initial min-h-[36px] text-xs sm:text-sm px-2 sm:px-3 touch-manipulation active:scale-95 bg-primary/5 hover:bg-primary/10 border-primary/20"
                >
                  <Hotel className="w-3 h-3 sm:w-4 sm:h-4 mr-1 sm:mr-2" />
                  Book Hotels
                </Button>
                <Button
                  variant="outline"
                  size="sm"
                  disabled={isGenerating}
                  className="flex-1 sm:flex-initial min-h-[36px] text-xs sm:text-sm px-2 sm:px-3 touch-manipulation active:scale-95"
                >
                  <Plus className="w-3 h-3 sm:w-4 sm:h-4 mr-1 sm:mr-2" />
                  {t('components.planTab.addDay')}
                </Button>
              </div>
            
            {/* Validation Advice */}
            {validationAdvice && (
              <ValidationAdvice advice={validationAdvice} />
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
                {mappedDays.length === 0 ? (
                  <div className="p-8 text-center border-2 border-dashed border-gray-300 rounded-lg">
                    <p className="text-gray-500">No days to display. Check console for debugging info.</p>
                  </div>
                ) : (
                  mappedDays.map((day: any, dayIndex: number) => {
                    console.log(`[PlanTab] 🎨 Rendering DayCard ${dayIndex + 1}:`, {
                      dayNumber: day.dayNumber,
                      date: day.date,
                      location: day.location,
                      nodesCount: day.nodes?.length,
                      isExpanded: expandedDay === dayIndex
                    });
                    
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
                        <div 
                          className="md:ml-16"
                          ref={(el) => {
                            if (el) dayRefs.set(dayIndex, el);
                          }}
                        >
                          <DayCard
                            day={day}
                            isExpanded={expandedDay === dayIndex}
                            onToggle={() => {
                              const newExpandedDay = expandedDay === dayIndex ? null : dayIndex;
                              setExpandedDay(newExpandedDay);
                              
                              // Scroll to the card when expanding, with offset for sticky headers
                              if (newExpandedDay !== null) {
                                setTimeout(() => {
                                  const element = dayRefs.get(dayIndex);
                                  if (element) {
                                    // Find the scrollable container (main element on desktop, window on mobile)
                                    const scrollContainer = element.closest('main') || window;
                                    const isWindow = scrollContainer === window;
                                    
                                    // Calculate position relative to scroll container
                                    const elementRect = element.getBoundingClientRect();
                                    const containerTop = isWindow ? 0 : (scrollContainer as Element).getBoundingClientRect().top;
                                    const currentScroll = isWindow ? window.pageYOffset : (scrollContainer as Element).scrollTop;
                                    
                                    // Responsive offset for sticky headers/tabs
                                    const offset = window.innerWidth >= 768 ? 100 : 80;
                                    const targetPosition = currentScroll + (elementRect.top - containerTop) - offset;
                                    
                                    // Scroll the appropriate container
                                    if (isWindow) {
                                      window.scrollTo({
                                        top: targetPosition,
                                        behavior: 'smooth'
                                      });
                                    } else {
                                      (scrollContainer as Element).scrollTo({
                                        top: targetPosition,
                                        behavior: 'smooth'
                                      });
                                    }
                                  }
                                }, 100); // Small delay to let the expansion animation start
                              }
                            }}
                            itineraryId={itineraryId}
                            enableDragDrop={true}
                            onRefetchNeeded={handleRefetchNeeded}
                            isGenerating={isGenerating}
                          />
                        </div>
                      </motion.div>
                    );
                  })
                )}
              </motion.div>
            </div>

            {/* Empty State */}
            {mappedDays.length === 0 && !isGenerating && (
              <EmptyState
                icon={Calendar}
                title={t('components.planTab.empty.title')}
                description={t('components.planTab.empty.description')}
                actionLabel={t('components.planTab.empty.action')}
                onAction={() => {/* TODO: Add day handler */}}
              />
            )}
          </div>
        </TabsContent>

        {/* Map View - Maximum height */}
        <TabsContent value="map" className="p-0">
          <div 
            className="h-[calc(100vh-12rem)] overflow-hidden"
            style={{ touchAction: 'none' }}
          >
            <TripMap itinerary={itinerary} />
          </div>
        </TabsContent>
      </Tabs>
    </div>
  );
}
