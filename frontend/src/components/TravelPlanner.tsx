import React from 'react';
import { Tabs, TabsContent, TabsList, TabsTrigger } from './ui/tabs';
import { Button } from './ui/button';
import { Card } from './ui/card';
import { Input } from './ui/input';
import { Search, Globe, Workflow, Video, Share2, MessageSquare } from 'lucide-react';
import { useItinerary, queryKeys } from '../state/query/hooks';
import { useQueryClient } from '@tanstack/react-query';
import { TripData } from '../types/TripData';
import { apiClient } from '../services/apiClient';
import { useTranslation } from 'react-i18next';
import { SimplifiedAgentProgress } from './agents/SimplifiedAgentProgress';
import { AutoRefreshEmptyState } from './shared/AutoRefreshEmptyState';
import { LanguageSelector } from './shared/LanguageSelector';
import { useDeviceDetection } from '../hooks/useDeviceDetection';
import { MobileLayout } from './travel-planner/mobile/MobileLayout';
import { useMapContext, useMapViewMode, useMapSelection } from '../contexts/MapContext';

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
import { NewChat } from './chat/NewChat';
import { UnifiedItineraryProvider } from '../contexts/UnifiedItineraryContext';
import { TripMap } from './travel-planner/TripMap';
import type { MapMarker } from '../types/MapTypes';
import MapErrorBoundary from './travel-planner/MapErrorBoundary';
import { addPlaceToItineraryDay } from '../utils/addPlaceToItinerary';
import { createWorkflowNodeFromPlace } from '../utils/placeToWorkflowNode';

// Import error handling and loading components
import { ErrorBoundary, withErrorBoundary } from './travel-planner/shared/ErrorBoundary';
import { LoadingState } from './shared/LoadingState';
import { DayCardSkeleton } from './loading/SkeletonLoader';
import { ErrorDisplay } from './shared/ErrorDisplay';

// Import extracted hooks and helpers
import {
  useTravelPlannerState,
  useDestinationsSync,
  useFreshItineraryCheck,
  useMapViewModeSync,
  useMapCenterSync,
  useAgentStatusesSync
} from './travel-planner/TravelPlannerHooks';
import {
  createDestinationHandlers,
  createDaySelectHandler,
  createItineraryUpdateHandler,
  buildMapMarkers,
  createAddPlaceHandler
} from './travel-planner/TravelPlannerHelpers';

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

  // Map context
  const {
    viewMode,
    setViewMode,
    center,
    setCenter,
    centerOnFirstDestination,
    centerOnDayComponent,
    highlightedMarkers,
    clearHighlightedMarkers
  } = useMapContext();

  const { selectNode, clearSelection } = useMapSelection();

  // Use extracted state management hook
  const state = useTravelPlannerState();
  const {
    activeView,
    setActiveView,
    activeTab,
    setActiveTab,
    currency,
    setCurrency,
    showNotes,
    setShowNotes,
    showWorkflowBuilder,
    setShowWorkflowBuilder,
    showChatInterface,
    setShowChatInterface,
    leftPanelWidth,
    setLeftPanelWidth,
    isLeftPanelExpanded,
    setIsLeftPanelExpanded,
    destinations,
    setDestinations,
    agentStatuses,
    setAgentStatuses,
    selectedDay,
    setSelectedDay,
    showProgressModal,
    setShowProgressModal
  } = state;

  // Fetch fresh data from API
  const { data: freshTripData, isLoading, error, refetch } = useItinerary(tripData.id);
  const queryClient = useQueryClient();

  // Use fresh data if available, fallback to props
  const currentTripData = (freshTripData as TripData) || tripData;

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

  // Use extracted hooks for side effects
  useDestinationsSync(currentTripData, setDestinations);
  useFreshItineraryCheck(isLoading, error, currentTripData, tripData.id);
  useMapViewModeSync(activeTab, showWorkflowBuilder, showChatInterface);
  useMapCenterSync(viewMode, destinations, currentTripData, centerOnFirstDestination, centerOnDayComponent);
  useAgentStatusesSync(currentTripData, setAgentStatuses);

  // Use extracted handlers
  const { updateDestination, addDestination, removeDestination } = createDestinationHandlers(setDestinations);
  const handleDaySelect = createDaySelectHandler(setSelectedDay);
  const handleItineraryUpdateFromChat = createItineraryUpdateHandler(queryClient, tripData.id);

  // Show loading state while fetching fresh data
  if (isLoading) {
    return <LoadingState variant="fullPage" message="Loading your travel planner..." size="lg" />;
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

  // Show progress modal if itinerary is still being generated
  if (currentTripData.status === 'planning' && (!currentTripData.itinerary || !currentTripData.itinerary.days || currentTripData.itinerary.days.length === 0)) {
    return (
      <SimplifiedAgentProgress
        tripData={currentTripData}
        onComplete={async () => {
          console.log('Itinerary generation completed!');
          setShowProgressModal(false);
          // Fetch the completed itinerary from ItineraryJsonService
          try {
            const completedItinerary = await apiClient.getItinerary(currentTripData.id);
            // Update the trip data via query client
            queryClient.setQueryData(
              queryKeys.itinerary(currentTripData.id),
              completedItinerary
            );
          } catch (error) {
            console.error('Failed to fetch completed itinerary:', error);
            refetch(); // Fallback to refetch
          }
        }}
        onCancel={() => {
          setShowProgressModal(false);
          onBack(); // Go back to dashboard
        }}
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
          <LoadingState
            variant="inline"
            message="Loading your itinerary..."
            size="md"
          />
          {/* Show skeleton loaders for better UX */}
          <div className="space-y-4">
            <DayCardSkeleton />
            <DayCardSkeleton />
            <DayCardSkeleton />
          </div>
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

          <AutoRefreshEmptyState
            title="No itinerary data available yet"
            description="Your personalized itinerary will appear here once planning is complete. In the meantime, you can collect your research links below."
            onRefresh={() => {
              console.log('TravelPlanner: Manual refresh triggered');
              refetch();
            }}
            showRefreshButton={true}
            icon={<Search className="w-16 h-16 mx-auto mb-4 text-gray-300" />}
          />
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
            <UnifiedItineraryProvider itineraryId={currentTripData.id}>
              <DayByDayView
                tripData={currentTripData}
                onDaySelect={handleDaySelect}
                isCollapsed={!isLeftPanelExpanded}
                onRefresh={() => {
                  console.log('TravelPlanner: DayByDayView refresh triggered');
                  refetch();
                }}
              />
            </UnifiedItineraryProvider>
          </TabsContent>
        </div>
      </Tabs>
    );

    // Build map markers using helper function
    const mapMarkers = buildMapMarkers(currentTripData);

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
            <UnifiedItineraryProvider itineraryId={currentTripData.id}>
              <NewChat />
            </UnifiedItineraryProvider>
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
                      days={(currentTripData.itinerary?.days || []).map((d: any, idx: number) => ({ id: d.id || `day-${idx + 1}`, dayNumber: d.dayNumber || (idx + 1), date: d.date, location: d.location }))}
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
              <UnifiedItineraryProvider itineraryId={currentTripData.id}>
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
              </UnifiedItineraryProvider>
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
            destinations={destinations}
          />
        );
      case 'plan':
        return renderPlanView();
      case 'packing':
        return <PackingListView tripData={currentTripData} onUpdate={() => { }} />;
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

  // Build map markers and handle place addition using helper functions
  const mapMarkers = buildMapMarkers(currentTripData);
  const handleAddPlace = createAddPlaceHandler(currentTripData, queryClient);

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
              setActiveView(view as TravelPlannerView);
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
                  setActiveView(view as TravelPlannerView);
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