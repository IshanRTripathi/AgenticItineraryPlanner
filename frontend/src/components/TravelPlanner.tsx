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

// Import error handling and loading components
import { ErrorBoundary } from './travel-planner/shared/ErrorBoundary';
import { LoadingSpinner } from './travel-planner/shared/LoadingSpinner';

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

export function TravelPlanner({ tripData, onSave, onBack, onShare, onExportPDF }: TravelPlannerProps) {
  console.log('=== TRAVEL PLANNER COMPONENT RENDER ===');
  console.log('Trip Data Props:', tripData);
  console.log('=======================================');
  
  // Main state
  const [activeView, setActiveView] = useState<TravelPlannerView>('plan');
  const [activeTab, setActiveTab] = useState<PlanTab>('destinations');
  const [currency, setCurrency] = useState('EUR');
  const [showNotes, setShowNotes] = useState(false);
  const [showWorkflowBuilder, setShowWorkflowBuilder] = useState(true);
  const [showChatInterface, setShowChatInterface] = useState(false);
  const [leftPanelWidth, setLeftPanelWidth] = useState(25);
  const [isLeftPanelExpanded, setIsLeftPanelExpanded] = useState(false);
  const [destinations, setDestinations] = useState<Destination[]>([]);
  const [agentStatuses, setAgentStatuses] = useState<AgentStatus[]>([]);
  const [selectedDay, setSelectedDay] = useState<{ dayNumber: number; dayData: any } | null>(null);
  // Modal flow removed in favor of on-map InfoWindow


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
          discover: day.components && day.components.length > 0,
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
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <Card className="p-8 max-w-md mx-auto">
          <div className="text-center">
            <p className="text-red-600 mb-4">Failed to load planner data</p>
            <Button onClick={onBack}>Go Back</Button>
          </div>
        </Card>
      </div>
    );
  }


  // Render plan view with resizable panels
  const renderPlanView = () => {
    console.log('=== RENDER PLAN VIEW ===');
    console.log('Destinations Length:', destinations.length);
    console.log('Destinations:', destinations);
    console.log('Current Trip Data Itinerary:', currentTripData.itinerary);
    console.log('Current Trip Data Days:', currentTripData.itinerary?.days);
    console.log('========================');
    
    if (destinations.length === 0) {
      return (
        <div className="p-6 space-y-6">
          <Card className="p-6">
            <div className="flex items-center space-x-3 mb-4">
              <Search className="w-5 h-5 text-gray-400" />
              <Input 
                placeholder="Search any place in the world.." 
                className="flex-1 border-none text-lg"
              />
            </div>
          </Card>
          
          <Card className="p-6">
            <h3 className="font-semibold mb-2">Collect all your online research in one place</h3>
            <p className="text-gray-600 text-sm mb-4">
              Save your links from Instagram, Pinterest, blogs and more to your trip for easy reference.
            </p>
            <Button>Add a link</Button>
          </Card>
        </div>
      );
    }

    const leftContent = (
      <Tabs value={activeTab} onValueChange={setActiveTab} className="flex flex-col h-full">
        <div className="border-b border-gray-200 px-6 flex-shrink-0">
          <TabsList className="h-12">
            <TabsTrigger value="destinations">Destinations</TabsTrigger>
            <TabsTrigger value="day-by-day">Day by day</TabsTrigger>
          </TabsList>
        </div>
        
        <div className="flex-1 overflow-hidden">
          <TabsContent value="destinations" className="m-0 h-full overflow-y-auto">
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
          
          <TabsContent value="day-by-day" className="m-0 h-full overflow-y-auto">
            <DayByDayView 
              tripData={currentTripData} 
              onDaySelect={handleDaySelect}
              isCollapsed={!isLeftPanelExpanded}
            />
          </TabsContent>
        </div>
      </Tabs>
    );

    // Build map markers from itinerary days/components
    const mapMarkers: MapMarker[] = (() => {
      const markers: MapMarker[] = [];
      const days = currentTripData.itinerary?.days || [];
      days.forEach((day, dayIdx) => {
        const comps = day.components || [];
        comps.forEach((c: any, compIdx: number) => {
          const lat = c?.location?.coordinates?.lat;
          const lng = c?.location?.coordinates?.lng;
          if (typeof lat === 'number' && typeof lng === 'number') {
            markers.push({
              id: c.id || `${dayIdx}-${compIdx}`,
              position: { lat, lng },
              title: c.name || c.type || `Place ${compIdx + 1}`,
              type: (c.type === 'restaurant' ? 'meal' :
                    c.type === 'hotel' ? 'accommodation' :
                    c.type === 'transport' ? 'transport' : 'attraction'),
              status: 'planned',
              locked: false,
              rating: c.rating,
              googleMapsUri: c.googleMapsUri,
            });
          }
        });
      });
      console.log('[Maps] Built markers from itinerary:', markers);
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
            >
              <Globe className="w-4 h-4 mr-2" />
              Map
            </Button>
            <Button 
              size="sm" 
              variant={showWorkflowBuilder && !showChatInterface ? "default" : "outline"}
              onClick={() => {
                setShowWorkflowBuilder(true);
                setShowChatInterface(false);
              }}
            >
              <Workflow className="w-4 h-4 mr-2" />
              Workflow
            </Button>
            <Button 
              size="sm" 
              variant={showChatInterface ? "default" : "outline"}
              onClick={() => {
                setShowWorkflowBuilder(false);
                setShowChatInterface(true);
              }}
            >
              <MessageSquare className="w-4 h-4 mr-2" />
              AI Assistant
            </Button>
          </div>
        </div>
        
        <div className="absolute top-4 right-4 space-y-2 z-20">
          {!showWorkflowBuilder && !showChatInterface && (
            <Button size="sm" onClick={onShare}>Share trip</Button>
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
                        // TODO: Persist via backend mutation; then refresh itinerary
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
               <div className="pt-16 h-full">
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
          setIsLeftPanelExpanded(width > 25);
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

  // Main component return
  return (
    <>
      <div className="h-screen flex flex-col bg-gray-50">
        <TopNavigation
          tripData={currentTripData}
          onShare={onShare}
          onExportPDF={onExportPDF}
          onBack={onBack}
        />
        <div className="flex flex-1 overflow-hidden">
          <NavigationSidebar
            activeView={activeView}
            onViewChange={setActiveView}
          />
          {renderCurrentView()}
        </div>
      </div>
    </>
  );
}
