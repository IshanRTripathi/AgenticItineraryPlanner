import { useState } from 'react';
import { TravelPlannerView, PlanTab, Destination, AgentStatus } from './shared/types';

/**
 * Custom hook for TravelPlanner state management
 * Extracted from TravelPlanner.tsx for better maintainability
 */
export function useTravelPlannerState() {
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
  const [showProgressModal, setShowProgressModal] = useState(false);

  return {
    // State values
    activeView,
    activeTab,
    currency,
    showNotes,
    showWorkflowBuilder,
    showChatInterface,
    leftPanelWidth,
    isLeftPanelExpanded,
    destinations,
    agentStatuses,
    selectedDay,
    showProgressModal,
    
    // State setters
    setActiveView,
    setActiveTab,
    setCurrency,
    setShowNotes,
    setShowWorkflowBuilder,
    setShowChatInterface,
    setLeftPanelWidth,
    setIsLeftPanelExpanded,
    setDestinations,
    setAgentStatuses,
    setSelectedDay,
    setShowProgressModal,
  };
}
