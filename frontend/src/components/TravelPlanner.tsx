import React, { useState, useEffect } from 'react';
import { Tabs, TabsContent, TabsList, TabsTrigger } from './ui/tabs';
import { Button } from './ui/button';
import { Card } from './ui/card';
import { Input } from './ui/input';
import { Search, Globe, Workflow, Video, Share2, MessageSquare } from 'lucide-react';
import { useItinerary, queryKeys } from '../state/query/hooks';
import { useQueryClient } from '@tanstack/react-query';
import { TripData } from '../types/TripData';
import { useTranslation } from 'react-i18next';
import { LanguageSelector } from './shared/LanguageSelector';
import { useDeviceDetection } from '../hooks/useDeviceDetection';
import { MobileLayout } from './travel-planner/mobile/MobileLayout';

// Import extracted components
import { NavigationSidebar } from './travel-planner/layout/NavigationSidebar';
import { TopNavigation } from './travel-planner/layout/TopNavigation';
import { ResizablePanel } from './travel-planner/layout/ResizablePanel';
import { PackingListView } from './travel-planner/views/PackingListView';
import { BudgetView } from './travel-planner/views/BudgetView';
import { CollectionView } from './travel-planner/views/CollectionView';
import { DocumentsView } from './travel-planner/views/DocumentsView';
import { TripOverviewView } from './travel-planner/views/TripOverviewView';
import { DayByDayView } from './travel-planner/views/DayByDayView';
import { DestinationsManager } from './travel-planner/views/DestinationsManager';
import { WorkflowBuilder } from './WorkflowBuilder';
import { ChatInterface } from './ChatInterface';
import { TripMap } from './travel-planner/TripMap';
// Removed modal-based add flow; use on-map InfoWindow card instead
import type { MapMarker } from '../types/MapTypes';
import MapErrorBoundary from './travel-planner/MapErrorBoundary';
import { addPlaceToItineraryDay } from '../utils/addPlaceToItinerary';
import { createWorkflowNodeFromPlace } from '../utils/placeToWorkflowNode';

// Import error handling and loading components
import { ErrorBoundary, withErrorBoundary } from './travel-planner/shared/ErrorBoundary';
import { LoadingSpinner } from './travel-planner/shared/LoadingSpinner';
import { ErrorDisplay } from './shared/ErrorDisplay';

// Import types
import { 
  TravelPlannerView, 
  PlanTab, 
  Destination, 
  AgentStatus
} from './travel-planner/shared/types';

interface TravelPlannerProps {
  tripData: TripData;
  onSave: (updatedTrip: TripData) => void;
  onBack: () => void;
  onShare: () => void;
  onExportPDF: () => void;
}

function TravelPlannerComponent({ tripData, onSave, onBack, onShare, onExportPDF }: TravelPlannerProps) {
  console.log('=== TRAVEL PLANNER COMPONENT RENDER ===');
  console.log('Trip Data Props:', tripData);
  console.log('=======================================');
  
  // Device detection
  const { isMobile, isTablet } = useDeviceDetection();
  
  // Main state
  const [activeView, setActiveView] = useState<TravelPlannerView>('plan');
  const [activeTab, setActiveTab] = useState<PlanTab>('destinations');
  const [currency, setCurrency] = useState('EUR');
  const [showNotes, setShowNotes] = useState(false);
  const [showWorkflowBuilder, setShowWorkflowBuilder] = useState(true);
  const [showChatInterface, setShowChatInterface] = useState(false);
  const [leftPanelWidth, setLeftPanelWidth] = useState(45); // Start with expanded view
  const [isLeftPanelExpanded, setIsLeftPanelExpanded] = useState(true); // Start expanded
  const [destinations, setDestinations] = useState<Destination[]>([]);
  const [agentStatuses, setAgentStatuses] = useState<AgentStatus[]>([]);
  const [selectedDay, setSelectedDay] = useState<{ dayNumber: number; dayData: any } | null>(null);


  // Fetch fresh data from API instead of using cached props
  const { data: freshTripData, isLoading, error } = useItinerary(tripData.id);
  const queryClient = useQueryClient();
  
  // Use fresh data if available, fallback to props
  const currentTripData = freshTripData || tripData;

  // Log data fetching status
  console.log('===  TRAVEL PLANNER DATA FETCH ===');
  console.log('Trip ID:', tripData.id);
  console.log('Is Loading:', isLoading);
  console.log('Error:', error);
  console.log('Fresh Trip Data:', freshTripData);
  console.log('Current Trip Data:', currentTripData);
  console.log('Has Itinerary:', !!currentTripData.itinerary);
  console.log('Days Count:', currentTripData.itinerary?.days?.length || 0);
  console.log('================================');

  // Update destinations when fresh data arrives
  useEffect(() => {
    console.log('=== DESTINATIONS UPDATE EFFECT ===');
    console.log('Current Trip Data:', currentTripData);
    console.log('Has Itinerary:', !!currentTripData.itinerary);
    console.log('Days:', currentTripData.itinerary?.days);
    console.log('Days Length:', currentTripData.itinerary?.days?.length);
    
    if (currentTripData.itinerary?.days && currentTripData.itinerary.days.length > 0) {
      console.log('Processing itinerary days for destinations...');
      const newDestinations = currentTripData.itinerary.days.map((day, index) => {
        console.log(`Processing day ${index}:`, day);
        return {
          id: day.id || index.toString(),
          name: day.location,
          nights: 1,
          sleeping: !!day.accommodation,
          transport: day.components?.find(c => c.type === 'transport') ? {
            distance: `${day.totalDistance || 0} km`,
            duration: `${Math.round((day.totalDuration || 0) / 60)}h ${(day.totalDuration || 0) % 60}m`
          } : undefined,
          notes: day.notes || '',
          lat: day.components?.[0]?.location?.coordinates?.lat || 0,
          lng: day.components?.[0]?.location?.coordinates?.lng || 0
        };
      });
      console.log('New Destinations:', newDestinations);
      setDestinations(newDestinations);
    } else {
      console.log('No itinerary days found; awaiting fresh data');
      setDestinations([]);
    }
    console.log('==================================');
  }, [JSON.stringify(currentTripData.itinerary?.days)]);

  // Ensure fresh itinerary when days are empty (avoids stale state after generation)
  useEffect(() => {
    if (!isLoading && !error && (!currentTripData.itinerary?.days || currentTripData.itinerary.days.length === 0)) {
      queryClient.invalidateQueries({ queryKey: queryKeys.itinerary(tripData.id) }).catch(() => {});
    }
  }, [isLoading, error, currentTripData.itinerary?.days?.length, tripData.id]);


  // Update agent statuses when fresh data arrives
  useEffect(() => {
    console.log('=== AGENT STATUSES UPDATE EFFECT ===');
    console.log('Current Trip Data Agent Progress:', currentTripData.agentProgress);
    
    if (currentTripData.agentProgress) {
      console.log('Processing agent progress data...');
      const newAgentStatuses = Object.entries(currentTripData.agentProgress).map(([agentId, progress]) => {
        console.log(`Processing agent ${agentId}:`, progress);
        return {
          id: agentId,
          kind: agentId as any,
          status: progress.status as any,
          progress: progress.progress,
          message: progress.message
        };
      });
      console.log('New Agent Statuses:', newAgentStatuses);
      setAgentStatuses(newAgentStatuses);
    } else {
      console.log('No agent progress data found, using fallback statuses');
      // Fallback to basic status if no agent data available
      const fallbackStatuses = [
        { id: '1', kind: 'planner', status: 'succeeded', progress: 100 },
        { id: '2', kind: 'places', status: 'completed', progress: 100 },
        { id: '3', kind: 'route', status: 'completed', progress: 100 },
        { id: '4', kind: 'hotels', status: 'completed', progress: 100 },
        { id: '5', kind: 'food', status: 'completed', progress: 100 },
        { id: '6', kind: 'photo', status: 'completed', progress: 100 },
      ];
      console.log('Fallback Agent Statuses:', fallbackStatuses);
      setAgentStatuses(fallbackStatuses);
    }
    console.log('====================================');
  }, [currentTripData]);

  // Destination management functions
  const updateDestination = (id: string, updates: Partial<Destination>) => {
    setDestinations(prev => prev.map(dest => 
      dest.id === id ? { ...dest, ...updates } : dest
    ));
  };

  const addDestination = (destination: Omit<Destination, 'id'>) => {
    const newDest: Destination = {
      id: Date.now().toString(),
      ...destination
    };
    setDestinations(prev => [...prev, newDest]);
  };

  const removeDestination = (id: string) => {
    setDestinations(prev => prev.filter(dest => dest.id !== id));
  };

  // Day selection handler
  const handleDaySelect = (dayNumber: number, dayData: any) => {
    console.log('Day selected:', dayNumber, dayData);
    setSelectedDay({ dayNumber, dayData });
  };

  // Handle itinerary updates from chat
  const handleItineraryUpdateFromChat = async (updatedItinerary: any) => {
    console.log('Itinerary updated from chat:', updatedItinerary);
    
    // Invalidate and refetch the itinerary data to reflect changes
    try {
      await queryClient.invalidateQueries({ 
        queryKey: queryKeys.itinerary(tripData.id) 
      });
      console.log('Itinerary data refreshed successfully');
    } catch (error) {
      console.error('Failed to refresh itinerary data:', error);
    }
  };

  // Show loading state while fetching fresh data
  if (isLoading) {
    return <LoadingSpinner message="Loading planner..." fullScreen />;
  }

  // Show error state if API call failed
  if (error) {
    return (
      <ErrorDisplay
        error={error}
        onRetry={() => window.location.reload()}
        onGoBack={onBack}
      />
    );
  }


  // Render plan view with resizable panels
  const renderPlanView = () => {
    console.log('=== RENDER PLAN VIEW ===');
    console.log('Destinations Length:', destinations.length);
    console.log('Destinations:', destinations);
    console.log('Current Trip Data Itinerary:', currentTripData.itinerary);
    console.log('Current Trip Data Days:', currentTripData.itinerary?.days);
    console.log('Has Itinerary Data:', !!(currentTripData.itinerary?.days && currentTripData.itinerary.days.length > 0));
    console.log('========================');
    
    // Check if we have actual itinerary data, not just destinations array
    const hasItineraryData = currentTripData.itinerary?.days && currentTripData.itinerary.days.length > 0;
    
    // Show loading state while data is being fetched
    if (isLoading) {
      return (
        <div className="p-4 md:p-6 space-y-4 md:space-y-6">
          <Card className="p-4 md:p-6">
            <div className="flex items-center space-x-3 mb-4">
              <div className="animate-spin rounded-full h-5 w-5 border-b-2 border-blue-600"></div>
              <span className="text-gray-600">Loading your itinerary...</span>
            </div>
          </Card>
        </div>
      );
    }
    
    if (!hasItineraryData) {
      return (
        <div className="p-4 md:p-6 space-y-4 md:space-y-6">
          <Card className="p-4 md:p-6">
            <div className="flex items-center space-x-3 mb-4">
              <Search className="w-5 h-5 text-gray-400" />
              <Input 
                placeholder="Search any place in the world.." 
                className="flex-1 border-none text-lg"
              />
            </div>
          </Card>
          
          <Card className="p-4 md:p-6">
            <h3 className="font-semibold mb-2">No itinerary data available yet</h3>
            <p className="text-gray-600 text-sm mb-4">
              Your personalized itinerary will appear here once planning is complete. In the meantime, you can collect your research links below.
            </p>
            <Button>Add a link</Button>
          </Card>
        </div>
      );
    }

    const leftContent = (
      <Tabs value={activeTab} onValueChange={setActiveTab} className="flex flex-col h-full">
        <div className="border-b border-gray-200 px-4 md:px-6 flex-shrink-0">
          <TabsList className="h-10 md:h-12">
            <TabsTrigger value="destinations" className="text-sm md:text-base min-h-[44px]">Destinations</TabsTrigger>
            <TabsTrigger value="day-by-day" className="text-sm md:text-base min-h-[44px]">Day by day</TabsTrigger>
          </TabsList>
        </div>

        <div className="flex-1 min-h-0 flex flex-col">
          <TabsContent value="destinations" className="m-0 flex-1 overflow-y-auto">
            <DestinationsManager
              destinations={destinations}
              currency={currency}
              showNotes={showNotes}
              onUpdate={updateDestination}
              onAdd={addDestination}
              onRemove={removeDestination}
              onCurrencyChange={setCurrency}
              onToggleNotes={() => setShowNotes(!showNotes)}
              onUpdateTransport={(fromId, toId, transports) => {
                console.log(`Transport from ${fromId} to ${toId}:`, transports);
                // TODO: Implement transport update logic
              }}
            />
          </TabsContent>

          <TabsContent value="day-by-day" className="m-0 flex-1 overflow-y-auto">
            <DayByDayView 
              tripData={currentTripData} 
              onDaySelect={handleDaySelect}
              isCollapsed={!isLeftPanelExpanded}
            />
          </TabsContent>
        </div>
      </Tabs>
    );

    // Build map markers from itinerary days/components with error handling
    const mapMarkers: MapMarker[] = (() => {
      const markers: MapMarker[] = [];
      const days = currentTripData.itinerary?.days || [];
      
      try {
        days.forEach((day, dayIdx) => {
          const comps = day.components || [];
          comps.forEach((c: any, compIdx: number) => {
            try {
              const lat = c?.location?.coordinates?.lat;
              const lng = c?.location?.coordinates?.lng;
              
              console.log(`[Maps] Processing component ${c.id}:`, {
                name: c.name,
                lat: lat,
                lng: lng,
                latType: typeof lat,
                lngType: typeof lng,
                hasValidCoords: lat !== null && lng !== null && lat !== undefined && lng !== undefined
              });
              
              // Validate coordinates - must be valid numbers and not null/undefined
              if (lat !== null && lng !== null && lat !== undefined && lng !== undefined &&
                  typeof lat === 'number' && typeof lng === 'number' && 
                  !isNaN(lat) && !isNaN(lng) && 
                  lat >= -90 && lat <= 90 && lng >= -180 && lng <= 180) {
                
                markers.push({
                  id: c.id || `${dayIdx}-${compIdx}`,
                  position: { lat, lng },
                  title: c.name || c.type || `Place ${compIdx + 1}`,
                  type: (c.type === 'restaurant' ? 'meal' :
                        c.type === 'hotel' ? 'accommodation' :
                        c.type === 'transport' ? 'transport' : 'attraction'),
                  status: 'planned',
                  locked: false,
                  rating: c.rating || 0,
                  googleMapsUri: c.googleMapsUri || '',
                });
                
                console.log(`[Maps] Added marker for ${c.name} at (${lat}, ${lng})`);
              } else {
                console.warn('[Maps] Skipping component with invalid coordinates:', {
                  id: c.id,
                  name: c.name,
                  lat: lat,
                  lng: lng,
                  reason: lat === null || lng === null ? 'null coordinates' : 
                          lat === undefined || lng === undefined ? 'undefined coordinates' :
                          typeof lat !== 'number' || typeof lng !== 'number' ? 'non-numeric coordinates' :
                          isNaN(lat) || isNaN(lng) ? 'NaN coordinates' :
                          'out of range coordinates'
                });
              }
            } catch (error) {
              console.error('[Maps] Error processing component:', c, error);
            }
          });
        });
      } catch (error) {
        console.error('[Maps] Error building markers:', error);
      }
      
      console.log('[Maps] Built markers from itinerary:', markers.length, 'valid markers');
      return markers;
    })();

    const rightContent = (
      <>
        <div className="absolute top-4 left-4 z-20">
          <div className="flex space-x-2">
            <Button 
              size="sm" 
              variant={!showWorkflowBuilder && !showChatInterface ? "default" : "outline"}
              onClick={() => {
                setShowWorkflowBuilder(false);
                setShowChatInterface(false);
              }}
              className="min-h-[44px]"
            >
              <Globe className="w-4 h-4 mr-2" />
              <span className="hidden sm:inline">Map</span>
            </Button>
            <Button 
              size="sm" 
              variant={showWorkflowBuilder && !showChatInterface ? "default" : "outline"}
              onClick={() => {
                setShowWorkflowBuilder(true);
                setShowChatInterface(false);
              }}
              className="min-h-[44px]"
            >
              <Workflow className="w-4 h-4 mr-2" />
              <span className="hidden sm:inline">Workflow</span>
            </Button>
            <Button 
              size="sm" 
              variant={showChatInterface ? "default" : "outline"}
              onClick={() => {
                setShowWorkflowBuilder(false);
                setShowChatInterface(true);
              }}
              className="min-h-[44px]"
            >
              <MessageSquare className="w-4 h-4 mr-2" />
              <span className="hidden sm:inline">AI Assistant</span>
            </Button>
          </div>
        </div>
        
        <div className="absolute top-4 right-4 space-y-2 z-20">
          {!showWorkflowBuilder && !showChatInterface && (
            <Button size="sm" onClick={onShare} className="min-h-[44px]">Share trip</Button>
          )}
        </div>
        
        <div className="flex-1 overflow-hidden">
          {showChatInterface ? (
            <ChatInterface 
              itineraryId={currentTripData.id}
              onItineraryUpdate={handleItineraryUpdateFromChat}
            />
          ) : !showWorkflowBuilder ? (
            <div className="h-full overflow-hidden relative">
              {/* Feature flag: simplest gated render for map MVP */}
              {Boolean((import.meta as any).env?.VITE_GOOGLE_MAPS_BROWSER_KEY) ? (
                <div className="h-full">
                  {/* TripMap integration with error boundary */}
                  <MapErrorBoundary
                    onError={(error) => {
                      console.error('Map error:', error);
                    }}
                  >
                    <TripMap
                      itineraryId={currentTripData.id}
                      mapBounds={currentTripData.itinerary?.mapBounds}
                      countryCentroid={currentTripData.itinerary?.countryCentroid}
                      nodes={mapMarkers}
                      days={(currentTripData.itinerary?.days || []).map((d: any, idx: number) => ({ id: d.id || `day-${idx+1}`, dayNumber: d.dayNumber || (idx+1), date: d.date, location: d.location }))}
                      onAddPlace={({ dayId, dayNumber, place }) => {
                        console.log('[Maps] Add place to itinerary (InfoWindow)', { dayId, dayNumber, place })
                        console.log('[Maps] Place types:', place.types)
                        console.log('[Maps] Place name:', place.name)
                        
                        try {
                          // 1. Add to day-by-day view
                          const updatedTripData = addPlaceToItineraryDay(currentTripData, {
                            dayId,
                            dayNumber,
                            place,
                          });
                          
                          // Update the trip data via query client
                          queryClient.setQueryData(
                            queryKeys.itinerary(currentTripData.id),
                            updatedTripData
                          );
                          
                          // 2. Create workflow node if workflow builder is available
                          if (showWorkflowBuilder) {
                            const dayIndex = dayNumber - 1; // Convert to 0-based index
                            const workflowNode = createWorkflowNodeFromPlace(
                              place,
                              dayIndex,
                              { x: 200 + Math.random() * 300, y: 200 + Math.random() * 300 }
                            );
                            
                            console.log('[Maps] Created workflow node:', workflowNode);
                            
                            // TODO: Add the workflow node to the workflow builder
                            // This would require access to the workflow builder's state management
                            // For now, we'll just log it
                          }
                          
                          // 3. TODO: Persist via backend mutation; then refresh itinerary
                          // This would involve calling the backend API to save the changes
                          
                          console.log('[Maps] Successfully added place to itinerary');
                        } catch (error) {
                          console.error('[Maps] Failed to add place to itinerary:', error);
                        }
                      }}
                      className="w-full h-full"
                    />
                  </MapErrorBoundary>
                </div>
              ) : (
                <div className="h-full flex items-center justify-center text-gray-500">
                  <div className="text-center">
                    <Globe className="w-16 h-16 mx-auto mb-4 text-gray-300" />
                    <p>Interactive Map</p>
                    <p className="text-sm">Set VITE_GOOGLE_MAPS_BROWSER_KEY to enable the map</p>
                  </div>
                </div>
              )}
            </div>
          ) : (
             <div className="h-full overflow-hidden relative">
               <WorkflowBuilder 
                 tripData={currentTripData}
                 embedded={true}
                 onSave={(updatedItinerary) => {
                   console.log('Workflow saved:', updatedItinerary);
                   // TODO: Implement save functionality
                 }}
                 onCancel={() => {
                   console.log('Workflow cancelled');
                   setShowWorkflowBuilder(false);
                 }}
               />
             </div>
          )}
        </div>
      </>
    );

    return (
      <ResizablePanel
        leftPanelWidth={leftPanelWidth}
        onWidthChange={(width) => {
          setLeftPanelWidth(width);
          setIsLeftPanelExpanded(width > 30); // Consistent with ResizablePanel logic
        }}
        leftContent={leftContent}
        rightContent={rightContent}
      />
    );
  };

  // Render current view
  const renderCurrentView = () => {
    switch (activeView) {
      case 'view':
        return (
          <TripOverviewView
            tripData={currentTripData}
            agentStatuses={agentStatuses}
            onShare={onShare}
            onExportPDF={onExportPDF}
          />
        );
      case 'plan':
        return renderPlanView();
      case 'packing':
        return <PackingListView tripData={currentTripData} onUpdate={() => {}} />;
      case 'budget':
        return (
          <BudgetView
            tripData={currentTripData}
            currency={currency}
            onCurrencyChange={setCurrency}
          />
        );
      case 'collection':
        return <CollectionView tripData={currentTripData} />;
      case 'docs':
        return <DocumentsView tripData={currentTripData} />;
      default:
        return renderPlanView();
    }
  };

  // Build map markers for mobile layout
  const mapMarkers: MapMarker[] = (() => {
    const markers: MapMarker[] = [];
    const days = currentTripData.itinerary?.days || [];
    
    try {
      days.forEach((day, dayIdx) => {
        const comps = day.components || [];
        comps.forEach((c: any, compIdx: number) => {
          try {
            const lat = c?.location?.coordinates?.lat;
            const lng = c?.location?.coordinates?.lng;
            
            // Validate coordinates - must be valid numbers and not null/undefined
            if (lat !== null && lng !== null && lat !== undefined && lng !== undefined &&
                typeof lat === 'number' && typeof lng === 'number' && 
                !isNaN(lat) && !isNaN(lng) && 
                lat >= -90 && lat <= 90 && lng >= -180 && lng <= 180) {
              
              markers.push({
                id: c.id || `${dayIdx}-${compIdx}`,
                position: { lat, lng },
                title: c.name || c.type || `Place ${compIdx + 1}`,
                type: (c.type === 'restaurant' ? 'meal' :
                      c.type === 'hotel' ? 'accommodation' :
                      c.type === 'transport' ? 'transport' : 'attraction'),
                status: 'planned',
                locked: false,
                rating: c.rating || 0,
                googleMapsUri: c.googleMapsUri || '',
              });
            }
          } catch (error) {
            console.error('[Maps] Error processing component:', c, error);
          }
        });
      });
    } catch (error) {
      console.error('[Maps] Error building markers:', error);
    }
    
    return markers;
  })();

  // Handle place addition for mobile
  const handleAddPlace = ({ dayId, dayNumber, place }: { dayId: string; dayNumber: number; place: any }) => {
    console.log('[Mobile] Add place to itinerary:', { dayId, dayNumber, place });
    
    try {
      // Add to day-by-day view
      const updatedTripData = addPlaceToItineraryDay(currentTripData, {
        dayId,
        dayNumber,
        place,
      });
      
      // Update the trip data via query client
      queryClient.setQueryData(
        queryKeys.itinerary(currentTripData.id),
        updatedTripData
      );
      
      console.log('[Mobile] Successfully added place to itinerary');
    } catch (error) {
      console.error('[Mobile] Failed to add place to itinerary:', error);
    }
  };

  // Main component return
  return (
    <div className="h-screen flex flex-col bg-gray-50">
      {/* Only show TopNavigation when not in mobile plan view */}
      {!((isMobile || isTablet) && activeView === 'plan') && (
        <TopNavigation
          tripData={currentTripData}
          onShare={onShare}
          onExportPDF={onExportPDF}
          onBack={onBack}
        />
      )}
      
      {/* Main content area */}
      <div className="flex flex-1 min-h-0 overflow-hidden">
        {/* Sidebar - Desktop: part of layout, Mobile: overlay - only show when not in mobile layout */}
        {!((isMobile || isTablet) && activeView === 'plan') && (
          <NavigationSidebar
            activeView={activeView}
            onViewChange={(view) => {
              console.log('TravelPlanner: onViewChange called with', view);
              setActiveView(view);
            }}
          />
        )}
        
        {/* Content area */}
        <div className="flex-1 min-h-0">
          {/* Show mobile layout for plan tab on mobile/tablet, otherwise show normal view */}
          {(() => {
            const shouldShowMobile = (isMobile || isTablet) && activeView === 'plan';
            console.log('TravelPlanner render decision:', { 
              isMobile, 
              isTablet, 
              activeView, 
              shouldShowMobile 
            });
            
            return shouldShowMobile ? (
              <MobileLayout
                key={activeView} // Force re-render when activeView changes to reset currentCard
                tripData={currentTripData}
                destinations={destinations}
                mapMarkers={mapMarkers}
                currency={currency}
                showNotes={showNotes}
                onUpdateDestination={updateDestination}
                onAddDestination={addDestination}
                onRemoveDestination={removeDestination}
                onCurrencyChange={setCurrency}
                onToggleNotes={() => setShowNotes(!showNotes)}
                onUpdateTransport={(fromId, toId, transports) => {
                  console.log(`Transport from ${fromId} to ${toId}:`, transports);
                  // TODO: Implement transport update logic
                }}
                onDaySelect={handleDaySelect}
                onAddPlace={handleAddPlace}
                onItineraryUpdate={handleItineraryUpdateFromChat}
                onViewChange={(view) => {
                  console.log('TravelPlanner: MobileLayout onViewChange called with', view);
                  setActiveView(view);
                }}
                activeView={activeView}
              />
            ) : (
              renderCurrentView()
            );
          })()}
        </div>
      </div>
    </div>
  );
}

// Export with error boundary
export const TravelPlanner = withErrorBoundary(TravelPlannerComponent);