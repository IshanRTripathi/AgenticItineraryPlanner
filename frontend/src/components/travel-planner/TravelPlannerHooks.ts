import { useState, useEffect } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { TripData } from '../../types/TripData';
import { queryKeys } from '../../state/query/hooks';
import { TravelPlannerView, PlanTab, Destination, AgentStatus } from './shared/types';

/**
 * Custom hooks for TravelPlanner
 * Extracted from TravelPlanner.tsx for better maintainability
 */

interface UseTravelPlannerStateReturn {
  activeView: TravelPlannerView;
  setActiveView: (view: TravelPlannerView) => void;
  activeTab: PlanTab;
  setActiveTab: (tab: PlanTab) => void;
  currency: string;
  setCurrency: (currency: string) => void;
  showNotes: boolean;
  setShowNotes: (show: boolean) => void;
  showWorkflowBuilder: boolean;
  setShowWorkflowBuilder: (show: boolean) => void;
  showChatInterface: boolean;
  setShowChatInterface: (show: boolean) => void;
  leftPanelWidth: number;
  setLeftPanelWidth: (width: number) => void;
  isLeftPanelExpanded: boolean;
  setIsLeftPanelExpanded: (expanded: boolean) => void;
  destinations: Destination[];
  setDestinations: (destinations: Destination[] | ((prev: Destination[]) => Destination[])) => void;
  agentStatuses: AgentStatus[];
  setAgentStatuses: (statuses: AgentStatus[]) => void;
  selectedDay: { dayNumber: number; dayData: any } | null;
  setSelectedDay: (day: { dayNumber: number; dayData: any } | null) => void;
  showProgressModal: boolean;
  setShowProgressModal: (show: boolean) => void;
}

/**
 * Hook for managing TravelPlanner state
 */
export function useTravelPlannerState(): UseTravelPlannerStateReturn {
  const [activeView, setActiveView] = useState<TravelPlannerView>('plan');
  const [activeTab, setActiveTab] = useState<PlanTab>('destinations');
  const [currency, setCurrency] = useState('EUR');
  const [showNotes, setShowNotes] = useState(false);
  const [showWorkflowBuilder, setShowWorkflowBuilder] = useState(true);
  const [showChatInterface, setShowChatInterface] = useState(false);
  const [leftPanelWidth, setLeftPanelWidth] = useState(45);
  const [isLeftPanelExpanded, setIsLeftPanelExpanded] = useState(true);
  const [destinations, setDestinations] = useState<Destination[]>([]);
  const [agentStatuses, setAgentStatuses] = useState<AgentStatus[]>([]);
  const [selectedDay, setSelectedDay] = useState<{ dayNumber: number; dayData: any } | null>(null);
  const [showProgressModal, setShowProgressModal] = useState(false);

  return {
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
    setShowProgressModal,
  };
}

/**
 * Hook for syncing destinations from trip data
 */
export function useDestinationsSync(
  currentTripData: TripData,
  setDestinations: (destinations: Destination[]) => void
) {
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
  }, [JSON.stringify(currentTripData.itinerary?.days), setDestinations]);
}

/**
 * Hook for ensuring fresh itinerary data
 */
export function useFreshItineraryCheck(
  isLoading: boolean,
  error: any,
  currentTripData: TripData,
  tripId: string
) {
  const queryClient = useQueryClient();

  useEffect(() => {
    if (!isLoading && !error && (!currentTripData.itinerary?.days || currentTripData.itinerary.days.length === 0)) {
      queryClient.invalidateQueries({ queryKey: queryKeys.itinerary(tripId) }).catch(() => { });
    }
  }, [isLoading, error, currentTripData.itinerary?.days?.length, tripId, queryClient]);
}

/**
 * Hook for map view mode switching
 * DISABLED: Map functionality removed
 */
export function useMapViewModeSync(
  activeTab: PlanTab,
  showWorkflowBuilder: boolean,
  showChatInterface: boolean
) {
  // Map functionality disabled - no-op
  // const { switchToDestinations, switchToDayByDay, switchToWorkflow } = useMapViewMode();
  // useEffect(() => { ... }, [activeTab, showWorkflowBuilder, showChatInterface]);
}

/**
 * Hook for map center handling
 * DISABLED: Map functionality removed
 */
export function useMapCenterSync(
  viewMode: string,
  destinations: Destination[],
  currentTripData: TripData,
  centerOnFirstDestination: (destinations: Array<{ lat: number; lng: number }>) => void,
  centerOnDayComponent: (dayNumber: number, componentId: string, coordinates: { lat: number; lng: number }) => void
) {
  // Map functionality disabled - no-op
  // useEffect(() => { ... }, [viewMode, destinations, currentTripData, centerOnFirstDestination, centerOnDayComponent]);
}

/**
 * Hook for agent statuses sync
 */
export function useAgentStatusesSync(
  currentTripData: TripData,
  setAgentStatuses: (statuses: AgentStatus[]) => void
) {
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
      const fallbackStatuses: AgentStatus[] = [
        { id: '1', kind: 'planner', status: 'completed', progress: 100 },
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
  }, [currentTripData, setAgentStatuses]);
}
